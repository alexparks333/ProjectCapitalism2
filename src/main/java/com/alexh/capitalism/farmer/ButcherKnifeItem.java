package com.alexh.capitalism.farmer;

import com.alexh.capitalism.core.ModComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// The Butcher Knives (Farmer.md > Butchering): what cuts a Meat Slab up at the Butcher's Table. It goes in the table's
// knife slot, and each slab cut up uses up one cut of its sharpness (ModComponents.KNIFE_CUTS): an Iron Butcher Knife
// has 4, a Diamond one 10. Sharpness, not durability: the Tool (Pickaxe) Whetstone sharpens it back to full
// (weaponsmith/WhetstoneBlock). Use up the last cut and the knife is spent: it breaks on that slab. The bar under it
// shows the sharpness left. Farmers and Toolsmiths make them.
public class ButcherKnifeItem extends Item {

    public static final int IRON_CUTS = 4;
    public static final int DIAMOND_CUTS = 10;

    private final int maxCuts;

    public ButcherKnifeItem(Properties properties, int maxCuts) {
        super(properties.stacksTo(1));
        this.maxCuts = maxCuts;
    }

    public static ButcherKnifeItem iron(Properties properties) {
        return new ButcherKnifeItem(properties, IRON_CUTS);
    }

    public static ButcherKnifeItem diamond(Properties properties) {
        return new ButcherKnifeItem(properties, DIAMOND_CUTS);
    }

    public static boolean isKnife(ItemStack stack) {
        return stack.getItem() instanceof ButcherKnifeItem;
    }

    public static int maxCuts(ItemStack stack) {
        return stack.getItem() instanceof ButcherKnifeItem knife ? knife.maxCuts : 0;
    }

    public static int used(ItemStack stack) {
        return stack.getOrDefault(ModComponents.KNIFE_CUTS.get(), 0);
    }

    public static int cutsLeft(ItemStack stack) {
        return Math.max(0, maxCuts(stack) - used(stack));
    }

    // One slab cut up: true if that was its last cut (the knife is spent and breaks)
    public static boolean useCut(ItemStack stack) {
        int used = used(stack) + 1;
        if (used >= maxCuts(stack)) return true;
        stack.set(ModComponents.KNIFE_CUTS.get(), used);
        return false;
    }

    public static void sharpen(ItemStack stack) {
        stack.remove(ModComponents.KNIFE_CUTS.get());
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return used(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * cutsLeft(stack) / Math.max(1, this.maxCuts));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        // steel blue when sharp, going red as it dulls
        float left = cutsLeft(stack) / (float) Math.max(1, this.maxCuts);
        return Mth.hsvToRgb(0.55F * left, 0.7F, 1.0F);
    }
}
