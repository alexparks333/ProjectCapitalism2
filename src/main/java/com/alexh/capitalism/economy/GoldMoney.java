package com.alexh.capitalism.economy;

import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Prediction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;

// Money is real gold, like Project Capitalism 1: Gold Nugget = 1/9, Gold Ingot = 1, Gold Block = 9.
// Everything is counted in nuggets internally so partial stacks never get rounded away,
// and change or payouts that don't fit in the inventory drop at the player's feet instead of vanishing.
public final class GoldMoney {

    public static final int NUGGET = 1;
    public static final int INGOT = 9;
    public static final int BLOCK = 81;

    private GoldMoney() {
    }

    // Value of one stack, in nuggets.
    public static long valueOf(ItemStack stack) {
        if (stack.is(Items.GOLD_NUGGET)) return (long) stack.getCount() * NUGGET;
        if (stack.is(Items.GOLD_INGOT)) return (long) stack.getCount() * INGOT;
        if (stack.is(Items.GOLD_BLOCK)) return (long) stack.getCount() * BLOCK;
        return 0;
    }

    // Total gold in any container (inventory, ender chest...), in nuggets.
    public static long count(Container container) {
        long total = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            total += valueOf(container.getItem(i));
        }
        return total;
    }

    // Gold the player is carrying, in nuggets.
    public static long carried(Player player) {
        return count(player.getInventory());
    }

    // ── Net worth (leaderboard): also counts gold packed inside shulker boxes and bundles ──

    // Value of a stack including any gold stored inside it (shulker box contents, bundles), in nuggets.
    public static long deepValueOf(ItemStack stack) {
        return deepValueOf(stack, 0);
    }

    // Raw Gold counts toward net worth (the leaderboard, the death tax) at its smelted value: 1 Raw Gold = 1 gold, a Block
    // of Raw Gold = 9. It isn't money for /pay (valueOf, count, take): smelt it first. (Nor for /pay death: they smelt it in Purgatory.)
    public static long rawValueOf(ItemStack stack) {
        if (stack.is(Items.RAW_GOLD)) return (long) stack.getCount() * INGOT;
        if (stack.is(Items.RAW_GOLD_BLOCK)) return (long) stack.getCount() * BLOCK;
        return 0;
    }

    private static long deepValueOf(ItemStack stack, int depth) {
        long total = valueOf(stack) + rawValueOf(stack);
        if (depth >= 4 || stack.isEmpty()) return total;
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents != null) {
            total += contents.nonEmptyItemCopyStream().mapToLong(inner -> deepValueOf(inner, depth + 1)).sum() * stack.getCount();
        }
        BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (bundle != null) {
            total += bundle.itemCopies().mapToLong(inner -> deepValueOf(inner, depth + 1)).sum() * stack.getCount();
        }
        // a pet carried in an Animal Bjorn: whatever gold it's holding (in its mouth, its paws...) is saved inside the item
        com.alexh.capitalism.pets.CarriedAnimal animal = stack.get(com.alexh.capitalism.core.ModComponents.CARRIED_ANIMAL.get());
        if (animal != null) total += goldInTag(animal.data(), 0) * stack.getCount();
        return total;
    }

    // gold items saved anywhere in an entity's data ({id: "minecraft:gold_ingot", count: 3}, at any depth), in nuggets
    private static long goldInTag(net.minecraft.nbt.Tag tag, int depth) {
        if (depth > 12) return 0;
        long total = 0;
        if (tag instanceof net.minecraft.nbt.CompoundTag compound) {
            String id = compound.getStringOr("id", "");
            int each = switch (id) {
                case "minecraft:gold_nugget" -> NUGGET;
                case "minecraft:gold_ingot", "minecraft:raw_gold" -> INGOT;
                case "minecraft:gold_block", "minecraft:raw_gold_block" -> BLOCK;
                default -> 0;
            };
            if (each > 0) total += (long) each * compound.getIntOr("count", 1);
            for (String key : compound.keySet()) total += goldInTag(compound.get(key), depth + 1);
        } else if (tag instanceof net.minecraft.nbt.ListTag list) {
            for (net.minecraft.nbt.Tag inner : list) total += goldInTag(inner, depth + 1);
        }
        return total;
    }

    // All gold in a container including gold inside shulker boxes / bundles stored there, in nuggets.
    public static long deepCount(Container container) {
        long total = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            total += deepValueOf(container.getItem(i));
        }
        return total;
    }

    // Everything a player personally holds for net worth: inventory, the item on their cursor, ender chest, and what's
    // on their back (a worn Backpack keeps its items inside it; an Animal Bjorn its pet: back/Back)
    public static long personalNetWorth(Player player) {
        return deepCount(player.getInventory())
            + deepValueOf(player.containerMenu.getCarried())
            + deepCount(player.getEnderChestInventory())
            + deepValueOf(com.alexh.capitalism.back.Back.peek(player));
    }

    // Takes exactly `nuggets` worth of gold from the player's inventory, making change as needed.
    // Uses ingots first, then nuggets, then breaks blocks. Returns false (and takes nothing) if they can't afford it.
    public static boolean take(Player player, long nuggets) {
        if (nuggets <= 0) return true;
        if (carried(player) < nuggets) return false;

        Inventory inventory = player.getInventory();
        long remaining = nuggets;
        remaining = takeItem(inventory, Items.GOLD_INGOT, INGOT, remaining);
        remaining = takeItem(inventory, Items.GOLD_NUGGET, NUGGET, remaining);
        remaining = takeItem(inventory, Items.GOLD_BLOCK, BLOCK, remaining);
        inventory.setChanged();

        // Took a little too much (broke an ingot or block): give the change back.
        if (remaining < 0) {
            give(player, -remaining);
        }
        return true;
    }

    // Removes pieces of one gold item until `remaining` is covered. Can overshoot (returns a negative number).
    private static long takeItem(Inventory inventory, Item item, int value, long remaining) {
        for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.is(item)) continue;
            long piecesNeeded = (remaining + value - 1) / value;
            int take = (int) Math.min(stack.getCount(), piecesNeeded);
            stack.shrink(take);
            remaining -= (long) take * value;
        }
        return remaining;
    }

    // Gives `nuggets` worth of gold as blocks, then ingots, then nuggets (so big payouts don't flood the inventory).
    // Whatever doesn't fit drops at the player's feet.
    public static void give(Player player, long nuggets) {
        if (nuggets <= 0) return;
        giveItem(player, Items.GOLD_BLOCK, nuggets / BLOCK);
        giveItem(player, Items.GOLD_INGOT, (nuggets % BLOCK) / INGOT);
        giveItem(player, Items.GOLD_NUGGET, nuggets % INGOT);
    }

    private static void giveItem(Player player, Item item, long amount) {
        int maxStack = new ItemStack(item).getMaxStackSize();
        while (amount > 0) {
            int size = (int) Math.min(amount, maxStack);
            ItemStack stack = new ItemStack(item, size);
            if (!player.getInventory().add(stack) || !stack.isEmpty()) {
                player.drop(stack, false, Prediction.SERVER_ONLY);
            }
            amount -= size;
        }
    }

    // `nuggets` worth of gold as item stacks (blocks, then ingots, then nuggets), e.g. for dropping on the ground.
    public static java.util.List<ItemStack> toStacks(long nuggets) {
        java.util.List<ItemStack> stacks = new java.util.ArrayList<>();
        addStacks(stacks, Items.GOLD_BLOCK, nuggets / BLOCK);
        addStacks(stacks, Items.GOLD_INGOT, (nuggets % BLOCK) / INGOT);
        addStacks(stacks, Items.GOLD_NUGGET, nuggets % INGOT);
        return stacks;
    }

    private static void addStacks(java.util.List<ItemStack> stacks, Item item, long amount) {
        while (amount > 0) {
            int size = (int) Math.min(amount, 64);
            stacks.add(new ItemStack(item, size));
            amount -= size;
        }
    }

    // Whole gold ingots, the unit players see and type.
    public static long toGold(long nuggets) {
        return nuggets / INGOT;
    }

    public static long fromGold(long gold) {
        return gold * INGOT;
    }

    // "12 gold", "12 gold 5 nuggets" or "5 nuggets"
    public static String format(long nuggets) {
        long gold = nuggets / INGOT;
        long rest = nuggets % INGOT;
        if (rest == 0) return gold + " gold";
        if (gold == 0) return rest + (rest == 1 ? " nugget" : " nuggets");
        return gold + " gold " + rest + (rest == 1 ? " nugget" : " nuggets");
    }
}
