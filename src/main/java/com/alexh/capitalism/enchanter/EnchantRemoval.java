package com.alexh.capitalism.enchanter;

import com.alexh.capitalism.core.ModItems;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.ArrayList;
import java.util.List;

// The Enchanter's Table (EnchantersTableMenu, Enchanter.md) does two things:
//  - Book (big slot) + Blaze Rod (small slot) -> an Ench-Removal (a flaming book).
//  - An enchanted item (big slot) + an Ench-Removal (small slot): pick one of its enchantments and it's burnt off for
//    good (curses too), for COST XP levels and the Ench-Removal. An Enchanted Book with 2+ enchantments works the same.
public final class EnchantRemoval {

    public static final int COST = 5;

    public record Entry(Holder<Enchantment> enchantment, int level) {
        public Component name() {
            return Enchantment.getFullname(this.enchantment, this.level);
        }

        public boolean curse() {
            return this.enchantment.is(EnchantmentTags.CURSE);
        }

        // XP levels to burn it off (the same for every enchantment)
        public int cost() {
            return COST;
        }
    }

    private EnchantRemoval() {
    }

    // What can be burnt off this item (in its own order), or nothing
    public static List<Entry> entries(ItemStack stack) {
        List<Entry> entries = new ArrayList<>();
        if (stack.isEmpty()) return entries;
        for (Object2IntMap.Entry<Holder<Enchantment>> e : EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet()) {
            entries.add(new Entry(e.getKey(), e.getIntValue()));
        }
        // a book with just one enchantment would just be a plain Book again: not that way
        if (stack.is(Items.ENCHANTED_BOOK) && entries.size() < 2) entries.clear();
        return entries;
    }

    public static boolean isRemover(ItemStack stack) {
        return stack.is(ModItems.ENCH_REMOVAL.get());
    }

    // Making an Ench-Removal: a Book in the big slot, a Blaze Rod in the small one
    public static boolean isRecipe(ItemStack big, ItemStack small) {
        return big.is(Items.BOOK) && small.is(Items.BLAZE_ROD);
    }

    public static boolean canAfford(Player player, Entry entry) {
        return player.isCreative() || player.experienceLevel >= entry.cost();
    }

    // The item without that enchantment
    public static ItemStack without(ItemStack stack, Entry entry) {
        ItemStack out = stack.copyWithCount(1);
        EnchantmentHelper.updateEnchantments(out, enchantments -> enchantments.removeIf(e -> e.equals(entry.enchantment())));
        return out;
    }
}
