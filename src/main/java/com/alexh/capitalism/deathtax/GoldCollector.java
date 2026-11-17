package com.alexh.capitalism.deathtax;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.ModAttachments;
import com.alexh.capitalism.designer.Bag;
import com.alexh.capitalism.designer.BagSlot;
import com.alexh.capitalism.economy.ContainerOwner;
import com.alexh.capitalism.economy.GoldLeaderboard;
import com.alexh.capitalism.economy.GoldLedger;
import com.alexh.capitalism.economy.GoldMoney;
import com.alexh.capitalism.economy.ShopfrontBlockEntity;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

// Collecting the death tax (/pay death, Insurance.md > Paying): gold is taken from everything that's the player's and
// within reach, in this order:
//  1. their inventory (the item on their cursor, shulker boxes in it, the bag on their back),
//  2. their Ender Chest, then every container they own (GoldLeaderboard.ledgerOwner: never someone else's, never the
//     town's City Chests, Mayoral Chest or Town Hall Mailbox) in a loaded chunk, then their Shopfronts' earnings,
//  3. their gold lying on the ground (loaded item entities tagged as theirs: their death drops and what they threw),
//  Raw Gold never pays: it counts toward their gold (the tax), but they smelt it into ingots first (furnaces work in
//     Purgatory); if it's all they have left, /pay death tells them so.
// Within each place: ingots, then nuggets, then blocks (like /pay). Breaking a block (or the last ingot or raw gold)
// overshoots: the change goes back to their inventory (dropped at their feet if it's full). Chunks are never loaded:
// containers in unloaded chunks are skipped (and reported). Everything is on the server thread, each stack changed
// once in place, an emptied item entity discarded straight away: nothing can be taken twice.
@EventBusSubscriber(modid = Capitalism.MODID)
public final class GoldCollector {

    // The tag on a gold item entity on the ground: whose it is (and the stack's size when it was tagged; only their
    // own items ever merge into it, so all of it is theirs)
    public record Drop(UUID owner, int count) {
        public static final Drop NONE = new Drop(new UUID(0, 0), 0);
        public static final Codec<Drop> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.STRING_CODEC.fieldOf("owner").forGetter(Drop::owner),
            Codec.INT.fieldOf("count").forGetter(Drop::count)
        ).apply(i, Drop::new));
    }

    // what was collected (nuggets, change already given back), and whether some of their containers were out of reach
    // rawLeft: they still have Raw Gold within reach (it can't pay: smelt it into ingots first)
    public record Result(long taken, boolean unloaded, boolean rawLeft) {
    }

    private GoldCollector() {
    }

    static boolean isGold(ItemStack stack) {
        return stack.is(Items.GOLD_NUGGET) || stack.is(Items.GOLD_INGOT) || stack.is(Items.GOLD_BLOCK);
    }

    static boolean isRawGold(ItemStack stack) {
        return stack.is(Items.RAW_GOLD) || stack.is(Items.RAW_GOLD_BLOCK);
    }

    // ── a player's gold on the ground is theirs alone ──
    // Any gold a player drops (nuggets, ingots, blocks, Raw Gold, or anything holding gold: a shulker box, a bundle...) is
    // locked to them (Economy.md > Gold belongs to its owner): only they can pick it up (vanilla's pickup target, and the
    // pickup event); no hopper, hopper minecart or mob takes it (mixin/HopperItemsMixin, mixin/MobPickUpMixin); it only
    // merges with their own (vanilla merges only items with the same target); it despawns like any dropped item (5 minutes, as death drops do;
    // lava, fire and the void destroy it too); it wears a name tag ("Dev's gold"). /pay death can collect it (the ground step below).

    // anything with gold in it
    public static boolean holdsGold(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (isGold(stack) || isRawGold(stack) || GoldMoney.deepValueOf(stack) > 0) return true;
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents != null && contents.nonEmptyItemCopyStream().anyMatch(GoldCollector::holdsGold)) return true;
        var bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
        return bundle != null && bundle.itemCopies().anyMatch(GoldCollector::holdsGold);
    }

    // Their death drops. A player's inventory goes straight into the world as they die (not through the drops list), so
    // every item that appears while they're dying is theirs: from the start of their death (first of all its handlers:
    // a worn bag spills then too) until its drops are done (or something cancelled the death).
    private static @org.jspecify.annotations.Nullable ServerPlayer dying;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDying(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) dying = player;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onDeathDone(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (event.isCanceled() && event.getEntity() == dying) dying = null;
    }

    @SubscribeEvent
    public static void onJoin(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (dying != null && !event.getLevel().isClientSide() && event.getEntity() instanceof ItemEntity item) lock(item, dying);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!event.isCanceled()) for (ItemEntity item : event.getDrops()) lock(item, player);
        if (player == dying) dying = null;
    }

    @SubscribeEvent
    public static void onTickEnd(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        dying = null;
    }

    // whatever they throw or drop (Q, out of a menu, change that didn't fit: every player drop fires this)
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onToss(ItemTossEvent event) {
        if (!event.isCanceled() && event.getPlayer() instanceof ServerPlayer player) lock(event.getEntity(), player);
    }

    // locks it to this player, if it holds gold
    public static void lock(ItemEntity item, ServerPlayer owner) {
        lock(item, owner.getUUID(), owner.getGameProfile().name());
    }

    static void lock(ItemEntity item, UUID owner, String name) {
        ItemStack stack = item.getItem();
        if (!holdsGold(stack)) return;
        item.setData(ModAttachments.GOLD_DROPPED_BY.get(), new Drop(owner, stack.getCount()));
        item.setTarget(owner);
        item.setCustomName(net.minecraft.network.chat.Component.literal(name + "'s gold").withStyle(net.minecraft.ChatFormatting.GOLD));
        item.setCustomNameVisible(false); // shown small when looked at (client/GoldOwnerHud), not as a big floating tag
    }

    public static boolean isLocked(ItemEntity item) {
        Drop drop = item.getExistingDataOrNull(ModAttachments.GOLD_DROPPED_BY.get());
        return drop != null && drop.count() > 0;
    }

    // nobody but the owner picks it up (whatever else says yes)
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        ItemEntity item = event.getItemEntity();
        Drop drop = item.getExistingDataOrNull(ModAttachments.GOLD_DROPPED_BY.get());
        if (drop == null || drop.count() <= 0 || event.getPlayer().getUUID().equals(drop.owner())) return;
        event.setCanPickup(net.minecraft.util.TriState.FALSE);
    }

    public static boolean droppedBy(ItemEntity item, UUID owner) {
        Drop drop = item.getExistingDataOrNull(ModAttachments.GOLD_DROPPED_BY.get());
        return drop != null && drop.count() > 0 && drop.owner().equals(owner);
    }
    // ── where gold can be taken from ──

    // one place holding a stack: read it, put the changed stack back
    interface Spot {
        ItemStack get();

        void set(ItemStack stack);
    }

    private static Spot spot(Supplier<ItemStack> get, Consumer<ItemStack> set) {
        return new Spot() {
            @Override
            public ItemStack get() {
                return get.get();
            }

            @Override
            public void set(ItemStack stack) {
                set.accept(stack);
            }
        };
    }

    // a container's slots, and the slots of any shulker box (or other item holding items) in them
    private static void containerSpots(List<Spot> out, Container container, Runnable changed) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            int slot = i;
            Spot s = spot(() -> container.getItem(slot), stack -> {
                container.setItem(slot, stack);
                changed.run();
            });
            out.add(s);
            nested(out, s);
        }
    }

    private static final int NESTED = 64;

    // the items inside the item in this spot (a shulker box's contents), written back into it. Not a Designer's bag: a
    // bag off your back is always empty, and the worn one is reached through its own slots.
    private static void nested(List<Spot> out, Spot parent) {
        ItemStack holder = parent.get();
        if (holder.isEmpty() || holder.getCount() != 1 || Bag.of(holder) != null) return;
        ItemContainerContents contents = holder.get(DataComponents.CONTAINER);
        if (contents == null || contents.nonEmptyItemCopyStream().findAny().isEmpty()) return;
        for (int i = 0; i < NESTED; i++) {
            int slot = i;
            out.add(spot(() -> inside(parent.get()).get(slot), stack -> {
                ItemStack box = parent.get().copy();
                NonNullList<ItemStack> items = inside(box);
                items.set(slot, stack);
                box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
                parent.set(box);
            }));
        }
    }

    private static NonNullList<ItemStack> inside(ItemStack holder) {
        NonNullList<ItemStack> items = NonNullList.withSize(NESTED, ItemStack.EMPTY);
        ItemContainerContents contents = holder.get(DataComponents.CONTAINER);
        if (contents != null) contents.copyInto(items);
        return items;
    }

    // 1. the inventory, the cursor, the bag on their back
    static List<Spot> inventory(ServerPlayer player) {
        List<Spot> out = new ArrayList<>();
        Inventory inventory = player.getInventory();
        containerSpots(out, inventory, inventory::setChanged);
        out.add(spot(() -> player.containerMenu.getCarried(), stack -> player.containerMenu.setCarried(stack)));
        for (Slot slot : player.inventoryMenu.slots) {
            if (slot instanceof BagSlot bag) out.add(spot(bag::getItem, bag::set));
        }
        return out;
    }

    // 2. the Ender Chest and their own containers in loaded chunks (and the Shopfronts among them, for their earnings)
    static List<Spot> storage(ServerPlayer player, List<ShopfrontBlockEntity> shops, boolean[] unloaded) {
        List<Spot> out = new ArrayList<>();
        Container ender = player.getEnderChestInventory();
        containerSpots(out, ender, () -> {
        });
        MinecraftServer server = player.level().getServer();
        GoldLedger ledger = GoldLedger.get(server);
        for (GlobalPos pos : new ArrayList<>(ledger.containerPositions())) {
            GoldLedger.ContainerEntry entry = ledger.container(pos);
            if (entry == null || !entry.owner().equals(player.getUUID())) continue;
            ServerLevel level = server.getLevel(pos.dimension());
            if (level == null) continue;
            if (!level.isLoaded(pos.pos())) {
                if (entry.nuggets() > 0) unloaded[0] = true;
                continue;
            }
            BlockEntity be = level.getBlockEntity(pos.pos());
            ContainerOwner owner = GoldLeaderboard.ledgerOwner(be);
            if (owner == null || !owner.owner().equals(player.getUUID()) || !(be instanceof Container container)) continue;
            containerSpots(out, container, be::setChanged);
            if (be instanceof ShopfrontBlockEntity shop) shops.add(shop);
        }
        return out;
    }

    // 3. their gold on the ground (loaded only): as much of each stack as is theirs
    static List<Spot> ground(ServerPlayer player) {
        List<Spot> out = new ArrayList<>();
        UUID id = player.getUUID();
        for (ServerLevel level : player.level().getServer().getAllLevels()) {
            for (ItemEntity item : level.getEntities(EntityTypeTest.forClass(ItemEntity.class), e -> e.isAlive() && droppedBy(e, id))) {
                out.add(spot(() -> {
                    if (!item.isAlive()) return ItemStack.EMPTY;
                    Drop drop = item.getExistingDataOrNull(ModAttachments.GOLD_DROPPED_BY.get());
                    if (drop == null || !drop.owner().equals(id)) return ItemStack.EMPTY;
                    return item.getItem().copy();
                }, stack -> {
                    Drop drop = item.getExistingDataOrNull(ModAttachments.GOLD_DROPPED_BY.get());
                    if (drop == null) return;
                    int taken = item.getItem().getCount() - stack.getCount();
                    if (taken <= 0) return;
                    ItemStack left = item.getItem().copy();
                    left.shrink(taken);
                    if (left.isEmpty()) item.discard();
                    else {
                        item.setItem(left);
                        item.setData(ModAttachments.GOLD_DROPPED_BY.get(), new Drop(id, left.getCount()));
                    }
                }));
            }
        }
        return out;
    }

    // ── taking ──

    private record Piece(Item item, int value) {
    }

    private static final List<Piece> GOLD = List.of(new Piece(Items.GOLD_INGOT, GoldMoney.INGOT), new Piece(Items.GOLD_NUGGET, GoldMoney.NUGGET),
        new Piece(Items.GOLD_BLOCK, GoldMoney.BLOCK));
    // 1 Raw Gold = 1 gold (it smelts into an ingot); a Block of Raw Gold = 9
    private static final List<Piece> RAW = List.of(new Piece(Items.RAW_GOLD, GoldMoney.INGOT), new Piece(Items.RAW_GOLD_BLOCK, GoldMoney.BLOCK));

    // takes pieces from these spots until `remaining` is covered (it can go below 0: change owed back)
    private static boolean has(List<Spot> spots, List<Piece> pieces) {
        for (Spot spot : spots) for (Piece piece : pieces) if (spot.get().is(piece.item)) return true;
        return false;
    }

    private static long take(List<Spot> spots, List<Piece> pieces, long remaining) {
        for (Piece piece : pieces) {
            for (Spot spot : spots) {
                if (remaining <= 0) return remaining;
                ItemStack stack = spot.get();
                if (stack.isEmpty() || !stack.is(piece.item)) continue;
                long need = (remaining + piece.value - 1) / piece.value;
                int n = (int) Math.min(stack.getCount(), need);
                ItemStack left = stack.copy();
                left.shrink(n);
                spot.set(left);
                remaining -= (long) n * piece.value;
            }
        }
        return remaining;
    }

    // Takes up to `wanted` nuggets of the player's gold, in the order above. What was taken (never more than wanted:
    // any overshoot is given back as change).
    public static Result collect(ServerPlayer player, long wanted) {
        if (wanted <= 0) return new Result(0, false, false);
        boolean[] unloaded = {false};
        List<ShopfrontBlockEntity> shops = new ArrayList<>();
        List<Spot> inventory = inventory(player);
        List<Spot> storage = storage(player, shops, unloaded);
        long remaining = wanted;
        remaining = take(inventory, GOLD, remaining);
        remaining = take(storage, GOLD, remaining);
        for (ShopfrontBlockEntity shop : shops) {
            if (remaining > 0) remaining -= shop.takeEarnings(remaining);
        }
        if (remaining > 0) remaining = take(ground(player), GOLD, remaining);
        // Raw Gold counts toward their gold (the tax) but never pays: they smelt it into ingots first (a furnace works fine
        // in Purgatory). Just note whether they have some, to tell them.
        boolean rawLeft = remaining > 0 && (has(inventory, RAW) || has(storage, RAW) || has(ground(player), RAW));
        player.getInventory().setChanged();
        if (remaining < 0) {
            GoldMoney.give(player, -remaining);
            remaining = 0;
        }
        return new Result(wanted - remaining, unloaded[0], rawLeft);
    }
}
