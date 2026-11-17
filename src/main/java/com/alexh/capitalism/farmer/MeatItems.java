package com.alexh.capitalism.farmer;

import com.alexh.capitalism.core.ModComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

// The meat that rots (Freshness): the Meat Slab items and the fine cuts. Neither stacks (each piece has its own
// timer). Under each one a bar like durability shows how fresh it is: full when fresh, emptying as it rots, green to
// yellow to red, and icy blue while it's chilled in a Fridge or Freezer (Chill).
public final class MeatItems {

    private MeatItems() {
    }

    // The fine cuts
    public static class Cut extends Item {
        public Cut(Properties properties) {
            super(properties.stacksTo(1));
        }

        @Override
        public boolean isBarVisible(ItemStack stack) {
            return barVisible(stack);
        }

        @Override
        public int getBarWidth(ItemStack stack) {
            return barWidth(stack);
        }

        @Override
        public int getBarColor(ItemStack stack) {
            return barColor(stack);
        }
    }

    // The Meat Slabs (they place as MeatSlabBlock)
    public static class Slab extends BlockItem {
        public Slab(Block block, Properties properties) {
            super(block, properties.stacksTo(1));
        }

        @Override
        public boolean isBarVisible(ItemStack stack) {
            return barVisible(stack);
        }

        @Override
        public int getBarWidth(ItemStack stack) {
            return barWidth(stack);
        }

        @Override
        public int getBarColor(ItemStack stack) {
            return barColor(stack);
        }
    }

    // ── the bar (drawn on the client: Freshness.clientClock) ──

    static boolean barVisible(ItemStack stack) {
        return stack.has(ModComponents.ROTS_AT.get());
    }

    // How much of its time it has left, 0-1
    static float freshShare(ItemStack stack) {
        long now = Freshness.clientClock.getAsLong();
        if (now < 0) return 1;
        Chill chill = stack.get(ModComponents.CHILL.get());
        long left = chill != null && chill.current(now) ? chill.leftAtNormalRate(now) : Freshness.ticksLeft(stack, now);
        return Mth.clamp(left / (float) Freshness.fullLife(stack), 0, 1);
    }

    static int barWidth(ItemStack stack) {
        return Math.round(13 * freshShare(stack));
    }

    static int barColor(ItemStack stack) {
        long now = Freshness.clientClock.getAsLong();
        Chill chill = stack.get(ModComponents.CHILL.get());
        if (chill != null && now >= 0 && chill.current(now)) return 0x7AD8FF;
        // green (fresh) through yellow to red (about to rot)
        return Mth.hsvToRgb(freshShare(stack) / 3.0F, 0.9F, 1.0F);
    }
}
