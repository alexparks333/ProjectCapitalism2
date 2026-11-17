package com.alexh.capitalism.economy;

import com.alexh.capitalism.core.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

// A player shop (market stall). Lives in the bottom half of the 2-block-tall Shopfront.
//  - Storage: 18 slots of real stock (this is the block's Container: it counts for net worth and drops when broken).
//  - 4 listings. Each listing box is only a picture: a copy of the item being sold (enchantments and all).
//    A purchase takes matching items out of Storage; none left = "Sold out".
//  - Each listing has an amount per purchase and a price made of gold nuggets, ingots and blocks.
//  - Earnings: gold paid by customers (all of it: there is no tax), stored in the block until the owner collects it.
// Hoppers can't touch it. Only the owner can mine it or change it (see ContainerLocks). All checks happen here,
// on the server.
public class ShopfrontBlockEntity extends BlockEntity implements WorldlyContainer {

    public static final int LISTINGS = 4;
    public static final int STORAGE_SLOTS = 18;
    private static final int[] NO_SLOTS = {};

    // Per-listing number fields, in the order the menu shows and syncs them
    public static final int FIELD_AMOUNT = 0;
    public static final int FIELD_NUGGETS = 1;
    public static final int FIELD_INGOTS = 2;
    public static final int FIELD_BLOCKS = 3;
    public static final int FIELDS = 4;
    private static final int[] FIELD_MIN = {1, 0, 0, 0};
    private static final int[] FIELD_MAX = {64, 99, 99, 999};

    private final NonNullList<ItemStack> stock = NonNullList.withSize(STORAGE_SLOTS, ItemStack.EMPTY);
    // What each listing sells: a 1-item copy set by the owner. Purely a picture; the real items are in Storage.
    private final NonNullList<ItemStack> templates = NonNullList.withSize(LISTINGS, ItemStack.EMPTY);
    private final int[][] fields = new int[LISTINGS][FIELDS];
    // Bumped whenever an offer changes, so a customer can never buy an offer that changed after they saw it.
    private final int[] versions = new int[LISTINGS];
    private long earnings;

    // The listing pictures, shown in both menus (read-only; the owner changes them with a button).
    private final Container displayView = new Container() {
        @Override public int getContainerSize() { return LISTINGS; }
        @Override public boolean isEmpty() { return templates.stream().allMatch(ItemStack::isEmpty); }
        @Override public ItemStack getItem(int slot) { return templates.get(slot); }
        @Override public ItemStack removeItem(int slot, int count) { return ItemStack.EMPTY; }
        @Override public ItemStack removeItemNoUpdate(int slot) { return ItemStack.EMPTY; }
        @Override public void setItem(int slot, ItemStack stack) { }
        @Override public void setChanged() { }
        @Override public boolean stillValid(Player player) { return ShopfrontBlockEntity.this.stillValid(player); }
        @Override public void clearContent() { }
    };

    // Numbers shown in the menus. Menus sync numbers as shorts (max 32767), so each value is split over two.
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return ShopfrontMenu.packHalf(value(ShopfrontMenu.valueIndex(index)), index);
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return ShopfrontMenu.DATA_COUNT;
        }
    };

    public ShopfrontBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SHOPFRONT.get(), pos, state);
        for (int[] f : this.fields) {
            f[FIELD_AMOUNT] = 1;
        }
    }

    // Value layout (see ShopfrontMenu): per listing [amount, nuggets, ingots, blocks, sales, version], then earnings.
    private int value(int valueIndex) {
        int perListing = ShopfrontMenu.VALUES_PER_LISTING;
        if (valueIndex < LISTINGS * perListing) {
            int listing = valueIndex / perListing;
            int field = valueIndex % perListing;
            if (field < FIELDS) return this.fields[listing][field];
            if (field == FIELDS) return salesInStock(listing);
            return this.versions[listing];
        }
        return (int) Math.min(this.earnings, (1L << 30) - 1);
    }

    public Container displayView() {
        return this.displayView;
    }

    public ContainerData dataAccess() {
        return this.dataAccess;
    }

    // takes up to this many nuggets of the uncollected earnings (the death tax collecting: deathtax/GoldCollector); how many
    public long takeEarnings(long max) {
        long taken = Math.max(0, Math.min(max, this.earnings));
        if (taken > 0) {
            this.earnings -= taken;
            this.setChanged();
        }
        return taken;
    }

    public long earnings() {
        return this.earnings;
    }

    public @Nullable UUID ownerId() {
        ContainerOwner owner = ContainerLocks.ownerOf(this);
        return owner == null ? null : owner.owner();
    }

    // Price of listing i in nuggets.
    public long priceNuggets(int i) {
        return this.fields[i][FIELD_NUGGETS] * (long) GoldMoney.NUGGET
            + this.fields[i][FIELD_INGOTS] * (long) GoldMoney.INGOT
            + this.fields[i][FIELD_BLOCKS] * (long) GoldMoney.BLOCK;
    }

    // How many purchases of listing i the Storage covers right now.
    public int salesInStock(int i) {
        ItemStack template = this.templates.get(i);
        if (template.isEmpty()) return 0;
        return countMatching(template) / this.fields[i][FIELD_AMOUNT];
    }

    private int countMatching(ItemStack template) {
        return ContainerHelper.clearOrCountMatchingItems(this, stack -> ItemStack.isSameItemSameComponents(stack, template), Integer.MAX_VALUE, true);
    }

    private void bumpVersion(int i) {
        this.versions[i] = (this.versions[i] + 1) & 0x3FFF;
    }

    // ── Owner actions ──────────────────────────────────────────────────────

    // Sets listing i's picture to a copy of what the owner holds on their cursor (empty cursor clears it).
    public void setTemplate(int i, ItemStack fromCursor) {
        this.templates.set(i, fromCursor.isEmpty() ? ItemStack.EMPTY : fromCursor.copyWithCount(1));
        this.bumpVersion(i);
        this.setChanged();
    }

    public void adjustField(int i, int field, int delta) {
        this.fields[i][field] = Math.max(FIELD_MIN[field], Math.min(FIELD_MAX[field], this.fields[i][field] + delta));
        this.bumpVersion(i);
        this.setChanged();
    }

    public void collect(ServerPlayer owner) {
        if (this.earnings <= 0) {
            owner.sendSystemMessage(EconomyCommands.tag("Shop").append(EconomyCommands.text("No earnings to collect yet.")));
            return;
        }
        long amount = this.earnings;
        this.earnings = 0;
        this.setChanged();
        GoldMoney.give(owner, amount);
        owner.sendSystemMessage(EconomyCommands.tag("Shop").append(EconomyCommands.text("Collected "))
            .append(EconomyCommands.gold(GoldMoney.format(amount))).append(EconomyCommands.text(".")));
    }

    // ── Customer: buying ───────────────────────────────────────────────────

    // `seenVersion` is the listing version the customer's screen showed; if the owner changed it since, refuse.
    public void buy(ServerPlayer buyer, int i, int seenVersion) {
        ItemStack template = this.templates.get(i);
        long price = priceNuggets(i);
        int amount = this.fields[i][FIELD_AMOUNT];
        if (seenVersion != this.versions[i]) {
            fail(buyer, "This offer just changed. Check it and try again.");
            return;
        }
        if (template.isEmpty() || price <= 0) {
            fail(buyer, "That isn't for sale.");
            return;
        }
        if (buyer.getUUID().equals(ownerId())) {
            fail(buyer, "You can't buy from your own shop.");
            return;
        }
        if (countMatching(template) < amount) {
            fail(buyer, "Sold out!");
            return;
        }
        if (!GoldMoney.take(buyer, price)) {
            fail(buyer, "You need " + GoldMoney.format(price) + ". You have " + GoldMoney.format(GoldMoney.carried(buyer)) + ".");
            return;
        }

        // Take the goods out of Storage and hand them over (whatever doesn't fit drops at their feet).
        ContainerHelper.clearOrCountMatchingItems(this, stack -> ItemStack.isSameItemSameComponents(stack, template), amount, false);
        String itemName = template.getHoverName().getString();
        int remaining = amount;
        while (remaining > 0) {
            ItemStack goods = template.copyWithCount(Math.min(remaining, template.getMaxStackSize()));
            remaining -= goods.getCount();
            if (!buyer.getInventory().add(goods) || !goods.isEmpty()) {
                buyer.drop(goods, false, Prediction.SERVER_ONLY);
            }
        }

        UUID owner = ownerId();
        ServerPlayer ownerPlayer = owner == null ? null : buyer.level().getServer().getPlayerList().getPlayer(owner);
        this.earnings += price;
        this.setChanged();
        // bounties (bounty/BountyEvents): the buyer bought, the owner sold; and the town's index of Shopfronts
        if (this.level instanceof net.minecraft.server.level.ServerLevel serverLevel && owner != null) {
            boolean cook = ownerPlayer != null ? ownerPlayer.getData(com.alexh.capitalism.core.ModAttachments.JOBS.get()).has(com.alexh.capitalism.occupation.Job.COOK)
                : com.alexh.capitalism.bounty.ShopIndex.cookAt(serverLevel, this.worldPosition);
            com.alexh.capitalism.bounty.ShopIndex.note(serverLevel, this.worldPosition, owner, cook);
            com.alexh.capitalism.bounty.BountyEvents.shopSale(buyer, ownerPlayer, template, cook);
        }

        buyer.sendSystemMessage(EconomyCommands.tag("Shop").append(EconomyCommands.text("Bought "))
            .append(EconomyCommands.gold(amount + "x " + itemName))
            .append(EconomyCommands.text(" for ")).append(EconomyCommands.gold(GoldMoney.format(price))).append(EconomyCommands.text("!")));
        if (this.level != null) {
            this.level.playSound(null, this.worldPosition, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.6F, 1.6F);
        }
    }

    private static void fail(ServerPlayer player, String text) {
        player.sendSystemMessage(EconomyCommands.tag("Shop").append(Component.literal(text).withStyle(ChatFormatting.RED)));
    }

    // ── Breaking: stock drops normally (it's a Container); earnings drop as gold ──

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level != null && this.earnings > 0) {
            for (ItemStack gold : GoldMoney.toStacks(this.earnings)) {
                Containers.dropItemStack(this.level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, gold);
            }
            this.earnings = 0;
        }
    }

    // ── Saving ─────────────────────────────────────────────────────────────

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.stock.clear();
        ContainerHelper.loadAllItems(input, this.stock);
        this.templates.clear();
        ContainerHelper.loadAllItems(input.childOrEmpty("listings"), this.templates);
        int[] saved = input.getIntArray("fields").orElse(new int[0]);
        for (int i = 0; i < LISTINGS; i++) {
            for (int f = 0; f < FIELDS; f++) {
                int index = i * FIELDS + f;
                this.fields[i][f] = index < saved.length ? saved[index] : FIELD_MIN[f];
            }
        }
        this.earnings = input.getLongOr("earnings", 0L);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.stock);
        ContainerHelper.saveAllItems(output.child("listings"), this.templates);
        int[] flat = new int[LISTINGS * FIELDS];
        for (int i = 0; i < LISTINGS; i++) {
            System.arraycopy(this.fields[i], 0, flat, i * FIELDS, FIELDS);
        }
        output.putIntArray("fields", flat);
        output.putLong("earnings", this.earnings);
    }

    // ── Container: the Storage ─────────────────────────────────────────────

    @Override
    public int getContainerSize() {
        return STORAGE_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return this.stock.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.stock.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack removed = ContainerHelper.removeItem(this.stock, slot, count);
        if (!removed.isEmpty()) this.setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.stock, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.stock.set(slot, stack);
        stack.limitSize(this.getMaxStackSize(stack));
        this.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.stock.clear();
    }

    // Hoppers can't put things in or take things out.
    @Override
    public int[] getSlotsForFace(Direction direction) {
        return NO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return false;
    }
}
