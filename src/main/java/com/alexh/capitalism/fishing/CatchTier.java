package com.alexh.capitalism.fishing;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.Util;

// How rare a catch is (Fishing.md > Tiers): its name and the tier line in its tooltip are coloured by it, and its
// plaque gets grander with it (client/FishPlaqueRenderer: walnut and brass; then dark wood and silver; ebony and gold;
// obsidian, gold and glowing runes). Mythic shimmers between blood red and ember orange, in bold.
public enum CatchTier {
    UNCOMMON("Uncommon", 0x55FF55),
    RARE("Rare", 0x5599FF),
    EPIC("Epic", 0xC060FF),
    LEGENDARY("Legendary", 0xFFAA00),
    MYTHIC("Mythic", 0xFF3030);

    public final String title;
    public final int color;

    CatchTier(String title, int color) {
        this.title = title;
        this.color = color;
    }

    // its colour now (Mythic pulses)
    public int colorNow() {
        if (this != MYTHIC) return this.color;
        double t = 0.5 + 0.5 * Math.sin(Util.getMillis() / 260.0);
        int g = (int) (0x18 + t * 0x70);
        return 0xFF0000 | (g << 8) | 0x10;
    }

    // a name or line in this tier's colour (bold for Mythic)
    public MutableComponent paint(MutableComponent text) {
        return text.withStyle(s -> s.withColor(TextColor.fromRgb(this.colorNow())).withBold(this == MYTHIC ? Boolean.TRUE : null));
    }

    // the tooltip's tier line: "◆ Legendary"
    public Component line() {
        return this.paint(Component.literal("◆ " + this.title));
    }
}
