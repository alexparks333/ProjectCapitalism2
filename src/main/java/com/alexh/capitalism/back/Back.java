package com.alexh.capitalism.back;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.ModAttachments;
import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.pets.AnimalBjornItem;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

// The Back slot: one more place on your body, for things you wear on your back (the item tag capitalism:back_items: the
// Animal Bjorn and the Designer's bags, designer/Bag). It's in your inventory next to the offhand slot (BackSlot, added
// to the inventory by mixin/InventoryMenuMixin), saved with you (ModAttachments.BACK) and synced to everyone who can see
// you, so they see what's on your back (client/BjornLayer). Dropped when you die (unless keepInventory).
// With an Animal Bjorn on your back: sneak + right-click an animal with an empty hand to put it in, sneak + right-click
// the ground with an empty hand to let it out.
@EventBusSubscriber(modid = Capitalism.MODID)
public final class Back {

    public static final TagKey<Item> BACK_ITEMS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Capitalism.MODID, "back_items"));

    // What's on someone's back (the attachment's value)
    public record Worn(ItemStack stack) {
        public static final Codec<Worn> CODEC = ItemStack.OPTIONAL_CODEC.xmap(Worn::new, Worn::stack);
        public static final StreamCodec<RegistryFriendlyByteBuf, Worn> STREAM_CODEC = ItemStack.OPTIONAL_STREAM_CODEC.map(Worn::new, Worn::stack);

        public Worn() {
            this(ItemStack.EMPTY);
        }
    }

    private Back() {
    }

    public static ItemStack get(Player player) {
        return player.getData(ModAttachments.BACK.get()).stack();
    }

    // The same, without creating the attachment if the player has none yet (safe while a player is still being made: the
    // inventory's bag slots ask before the player is connected, and making it would sync it)
    public static ItemStack peek(Player player) {
        Worn worn = player.getExistingDataOrNull(ModAttachments.BACK.get());
        return worn == null ? ItemStack.EMPTY : worn.stack();
    }

    // Puts this on the player's back. A Designer's bag coming off spills what was in it (designer/Bags: a bag is only
    // full while it's worn)
    public static void set(Player player, ItemStack stack) {
        ItemStack old = get(player);
        if (old != stack) com.alexh.capitalism.designer.Bags.spill(player, old);
        player.setData(ModAttachments.BACK.get(), new Worn(stack));
    }

    public static boolean fits(ItemStack stack) {
        return stack.is(BACK_ITEMS);
    }

    // Dying: what's on your back drops like the rest of your things, except a bag, which is destroyed (its things drop)
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ServerLevel level = player.level();
        if (level.getGameRules().get(GameRules.KEEP_INVENTORY)) return;
        ItemStack worn = get(player);
        if (worn.isEmpty()) return;
        // a Designer's bag burns: what was in it drops, the bag itself is gone (so bags keep being bought). Anything else
        // (an Animal Bjorn) drops like the rest of your things.
        if (com.alexh.capitalism.designer.Bag.of(worn) != null) com.alexh.capitalism.designer.Bags.spill(player, worn);
        else player.drop(worn.copy(), true, net.minecraft.util.Prediction.SERVER_ONLY);
        set(player, ItemStack.EMPTY);
    }

    // The Animal Bjorn on your back: sneak + right-click an animal (empty hand) to carry it
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onInteractAnimal(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (event.getHand() != InteractionHand.MAIN_HAND || !player.isShiftKeyDown() || !player.getMainHandItem().isEmpty()) return;
        ItemStack worn = get(player);
        if (!worn.is(ModItems.ANIMAL_BJORN.get()) || !(event.getTarget() instanceof Animal animal)) return;
        if (AnimalBjornItem.carried(worn) == null && !player.level().isClientSide()) {
            ItemStack bjorn = worn.copy();
            if (AnimalBjornItem.pickUp(bjorn, player, animal)) set(player, bjorn);
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    // ... and sneak + right-click the ground (empty hand) to let it out
    @SubscribeEvent
    public static void onClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (event.getHand() != InteractionHand.MAIN_HAND || !player.isShiftKeyDown() || !player.getMainHandItem().isEmpty()) return;
        ItemStack worn = get(player);
        if (!worn.is(ModItems.ANIMAL_BJORN.get()) || AnimalBjornItem.carried(worn) == null) return;
        if (player.level() instanceof ServerLevel level) {
            ItemStack bjorn = worn.copy();
            if (AnimalBjornItem.letOut(bjorn, player, level, event.getPos().relative(event.getFace() == null ? net.minecraft.core.Direction.UP : event.getFace()))) set(player, bjorn);
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
