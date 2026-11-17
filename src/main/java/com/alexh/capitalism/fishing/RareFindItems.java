package com.alexh.capitalism.fishing;

import com.alexh.capitalism.core.ModComponents;
import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.economy.GoldMoney;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// The rare catches you open (Fishing.md): the Message in a Bottle and the Sunken Pirate Chest; and what comes out of
// them: the Soggy Note (only the Fisherman redeems it, for the gold written on it) and the Pirate Hat (a cosmetic hat:
// client/PirateHatLayer draws it on your head).
public final class RareFindItems {

    private RareFindItems() {
    }

    static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
    }

    // ── the Message in a Bottle: uncork it ──

    public static class MessageInABottle extends RareCatchItem {
        public static final int MIN_GOLD = 1, MAX_GOLD = 5;

        public MessageInABottle(Properties properties) {
            super(RareCatch.MESSAGE_IN_A_BOTTLE, properties);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (level instanceof ServerLevel server) {
                ItemStack held = player.getItemInHand(hand);
                held.consume(1, player);
                give(player, new ItemStack(Items.GLASS_BOTTLE));
                give(player, note(server.getRandom()));
                if (player instanceof net.minecraft.server.level.ServerPlayer p) com.alexh.capitalism.bounty.BountyEvents.uncorked(p);
                server.playSound(null, player.blockPosition(), SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 1.0F, 1.2F);
                server.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 0.8F);
                player.sendOverlayMessage(Component.literal("📜 A soggy note, smudged but readable: a treasure! The Fisherman will know what it's worth.")
                    .withStyle(ChatFormatting.DARK_AQUA));
            }
            return InteractionResult.SUCCESS;
        }

        // a note for 1 to 5 gold
        public static ItemStack note(RandomSource random) {
            ItemStack note = new ItemStack(ModItems.SOGGY_NOTE.get());
            note.set(ModComponents.NOTE_GOLD.get(), (MIN_GOLD + random.nextInt(MAX_GOLD - MIN_GOLD + 1)) * GoldMoney.INGOT);
            return note;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
            super.appendHoverText(stack, context, display, builder, flag);
            builder.accept(Component.literal("Right-click to uncork it").withStyle(ChatFormatting.GRAY));
        }
    }

    // ── the Soggy Note ──

    public static class SoggyNote extends net.minecraft.world.item.Item {
        public SoggyNote(Properties properties) {
            super(properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
            Integer gold = stack.get(ModComponents.NOTE_GOLD.get());
            builder.accept(Component.literal("\"...buried " + (gold == null ? "?" : GoldMoney.toGold(gold)) + " gold where the old salt knows...\"")
                .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
            builder.accept(Component.literal("Only the Fisherman can redeem it").withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    // ── the Sunken Pirate Chest: crack it open ──

    public static class PirateChest extends RareCatchItem {
        // what's in one: gold (MIN to MAX gold), and by chance emeralds, a diamond, a spyglass, a nautilus shell, the Pirate Hat
        public static final int MIN_GOLD = 2, MAX_GOLD = 6;
        public static final float EMERALDS = 0.45F, DIAMOND = 0.12F, SPYGLASS = 0.10F, SHELL = 0.25F, HAT = 0.15F;
        // very rarely: the Old Captain's Rod (a fishing rod with Luck of the Sea III and Lure III), or a rare fish
        public static final float CAPTAINS_ROD = 0.03F, RARE_FISH = 0.04F;
        // a few raw cod / salmon that swam in
        public static final float FISH = 0.5F;

        public PirateChest(Properties properties) {
            super(RareCatch.SUNKEN_PIRATE_CHEST, properties);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (level instanceof ServerLevel server) {
                player.getItemInHand(hand).consume(1, player);
                List<ItemStack> loot = loot(server.getRandom());
                loot.addAll(treasure(server, player, server.getRandom()));
                for (ItemStack s : loot) give(player, s);
                if (player instanceof net.minecraft.server.level.ServerPlayer p) com.alexh.capitalism.bounty.BountyEvents.crackedChest(p);
                server.playSound(null, player.blockPosition(), SoundEvents.CHEST_OPEN, SoundSource.PLAYERS, 1.0F, 0.7F);
                server.playSound(null, player.blockPosition(), SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 0.5F, 1.2F);
                server.sendParticles(ParticleTypes.SPLASH, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.4, 0.3, 0.4, 0.1);
                server.sendParticles(ParticleTypes.BUBBLE_POP, player.getX(), player.getY() + 1.0, player.getZ(), 12, 0.3, 0.3, 0.3, 0.05);
                boolean hat = loot.stream().anyMatch(s -> s.is(ModItems.PIRATE_HAT.get()));
                boolean rod = loot.stream().anyMatch(s -> s.is(Items.FISHING_ROD));
                boolean fish = loot.stream().anyMatch(s -> s.getItem() instanceof RareCatchItem);
                player.sendOverlayMessage(Component.literal(rod ? "🏴‍☠️ The old captain's own rod, wrapped in oilcloth!" : fish ? "🏴‍☠️ Something rare flops out of the chest!"
                    : hat ? "🏴‍☠️ Arr! Gold, and a captain's hat!" : "🏴‍☠️ Pieces of gold, still glinting under the barnacles!")
                    .withStyle(ChatFormatting.GOLD));
            }
            return InteractionResult.SUCCESS;
        }

        // the very rare finds (they need the world: the rod's enchantments, the fish's date)
        public static List<ItemStack> treasure(ServerLevel level, Player player, RandomSource random) {
            List<ItemStack> out = new ArrayList<>();
            if (random.nextFloat() < CAPTAINS_ROD) out.add(captainsRod(level));
            if (random.nextFloat() < RARE_FISH) {
                List<RareCatch> fish = new ArrayList<>();
                for (RareCatch rare : RareCatch.values()) if (rare != RareCatch.SUNKEN_PIRATE_CHEST && rare != RareCatch.MESSAGE_IN_A_BOTTLE) fish.add(rare);
                RareCatch rare = fish.get(random.nextInt(fish.size()));
                out.add(RareCatchModifier.make(rare, player.getGameProfile().name(), CatchInfo.dateText(com.alexh.capitalism.calendar.Calendar.date(level)), random));
            }
            return out;
        }

        public static ItemStack captainsRod(ServerLevel level) {
            ItemStack rod = new ItemStack(Items.FISHING_ROD);
            var enchantments = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
            rod.enchant(enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LUCK_OF_THE_SEA), 3);
            rod.enchant(enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LURE), 3);
            rod.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("Old Captain's Rod").withStyle(ChatFormatting.GOLD).withStyle(s -> s.withItalic(false)));
            return rod;
        }

        public static List<ItemStack> loot(RandomSource random) {
            List<ItemStack> out = new ArrayList<>(GoldMoney.toStacks((MIN_GOLD + random.nextInt(MAX_GOLD - MIN_GOLD + 1)) * (long) GoldMoney.INGOT
                + random.nextInt(9)));
            if (random.nextFloat() < EMERALDS) out.add(new ItemStack(Items.EMERALD, 1 + random.nextInt(3)));
            if (random.nextFloat() < DIAMOND) out.add(new ItemStack(Items.DIAMOND));
            if (random.nextFloat() < SPYGLASS) out.add(new ItemStack(Items.SPYGLASS));
            if (random.nextFloat() < SHELL) out.add(new ItemStack(Items.NAUTILUS_SHELL));
            if (random.nextFloat() < HAT) out.add(new ItemStack(ModItems.PIRATE_HAT.get()));
            // often a few fish that swam in and got stuck
            if (random.nextFloat() < FISH) out.add(new ItemStack(random.nextBoolean() ? Items.COD : Items.SALMON, 1 + random.nextInt(3)));
            return out;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
            super.appendHoverText(stack, context, display, builder, flag);
            builder.accept(Component.literal("Right-click to crack it open").withStyle(ChatFormatting.GRAY));
        }
    }

    // ── the Pirate Hat ──

    public static class PirateHat extends net.minecraft.world.item.Item {
        public PirateHat(Properties properties) {
            super(properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
            builder.accept(Component.literal("A captain's black tricorn, gold-trimmed, a skull on its front. Wear it!").withStyle(ChatFormatting.GRAY));
        }
    }
}
