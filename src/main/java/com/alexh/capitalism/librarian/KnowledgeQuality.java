package com.alexh.capitalism.librarian;

import com.alexh.capitalism.core.ModComponents;
import com.alexh.capitalism.core.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.List;
import java.util.Locale;

// How good a Knowledge Piece is (Librarian.md). Made in the Knowledge Creator from an Enchanted Book: the book's power
// (every enchantment's level added up, +3 for each treasure enchantment like Mending) picks the quality. At the
// Librarian Table the quality multiplies the chance of a Persistent (trait) book. The Whisperer sells all four (one
// emerald more per step up). Faded is a plain Knowledge Piece (no data, so it stacks with any other); the rest carry
// ModComponents.KNOWLEDGE_QUALITY and pick their own look by the CUSTOM_MODEL_DATA string (items/knowledge_piece.json).
public enum KnowledgeQuality {
    FADED("Faded", 1, 1.0F, 0xA89A80),
    CLEAR("Clear", 3, 1.5F, 0x9AD0F0),
    BRIGHT("Bright", 6, 2.0F, 0xB070F0),
    RADIANT("Radiant", 10, 3.0F, 0xF0C040);

    public static final int TREASURE_BONUS = 3;

    public final String title;
    // the lowest book power that makes this quality
    public final int minPower;
    // times the Persistent chance at the Librarian Table
    public final float multiplier;
    public final int color;

    KnowledgeQuality(String title, int minPower, float multiplier, int color) {
        this.title = title;
        this.minPower = minPower;
        this.multiplier = multiplier;
        this.color = color;
    }

    public String id() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    // The Whisperer's price: Faded 1 emerald, Clear 2, Bright 3, Radiant 4
    public int emeralds() {
        return this.ordinal() + 1;
    }

    public static KnowledgeQuality byPower(int power) {
        KnowledgeQuality best = FADED;
        for (KnowledgeQuality quality : values()) {
            if (power >= quality.minPower) best = quality;
        }
        return best;
    }

    public static KnowledgeQuality byOrdinal(int ordinal) {
        return values()[Math.max(0, Math.min(values().length - 1, ordinal))];
    }

    // An Enchanted Book's power: its enchantment levels added up, +3 for each treasure one. 0 = not an enchanted book.
    public static int power(ItemStack book) {
        if (!book.is(Items.ENCHANTED_BOOK)) return 0;
        ItemEnchantments enchantments = book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        int power = 0;
        for (var entry : enchantments.entrySet()) {
            Holder<Enchantment> enchantment = entry.getKey();
            power += entry.getIntValue();
            if (enchantment.is(EnchantmentTags.TREASURE)) power += TREASURE_BONUS;
        }
        return power;
    }

    // What quality this Knowledge Piece is (a plain one is Faded)
    public static KnowledgeQuality of(ItemStack piece) {
        Integer quality = piece.get(ModComponents.KNOWLEDGE_QUALITY.get());
        return quality == null ? FADED : byOrdinal(quality);
    }

    // A Knowledge Piece of this quality
    public ItemStack stack(int count) {
        ItemStack stack = new ItemStack(ModItems.KNOWLEDGE_PIECE.get(), count);
        if (this != FADED) {
            stack.set(ModComponents.KNOWLEDGE_QUALITY.get(), this.ordinal());
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(this.id()), List.of()));
            if (this == RADIANT) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
        return stack;
    }
}
