package com.alexh.capitalism.librarian;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// A Knowledge Piece: "Faded / Clear / Bright / Radiant Knowledge Piece" in its quality's color (KnowledgeQuality).
public class KnowledgePieceItem extends Item {

    public KnowledgePieceItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        KnowledgeQuality quality = KnowledgeQuality.of(stack);
        return Component.literal(quality.title + " Knowledge Piece").withStyle(style -> style.withColor(quality.color));
    }
}
