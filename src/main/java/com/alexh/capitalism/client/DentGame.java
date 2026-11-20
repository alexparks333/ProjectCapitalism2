package com.alexh.capitalism.client;

import com.alexh.capitalism.weaponsmith.DentForging;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

// The Weaponsmith Table's repair game, "dent hunting": the weapon is drawn big in the middle of the screen and glowing
// dents pop up on it one after another. Click (hammer) each one before it cools down. The more damaged the weapon,
// the more dents and the faster they come (DentForging). Dents only land on the weapon's own pixels (read from its
// item texture) and never right next to another one. Played on the client; when it's over the number of hits goes to
// the server (SmithTableMenu.BUTTON_FORGE + hits), which does the actual repair.
final class DentGame {

    // Timing, in milliseconds (the rest comes from DentForging)
    private static final long WARM_UP = 900;
    private static final long FLASH = 180;
    private static final long END_PAUSE = 700;
    // The weapon is drawn this many times its 16x16 icon size (smaller on a short window)
    private static final int MAX_SCALE = 11;
    // A new dent stays at least this many icon pixels away from every earlier one (and further from the last few,
    // which may still be glowing)
    private static final int MIN_GAP = 2;
    private static final int RECENT_GAP = 4;
    private static final int RECENT = 3;

    private static final int OVERLAY = 0xE8181416;
    private static final int TITLE = 0xFFF2D9B8;
    private static final int TEXT = 0xFFD8C0A0;

    private static final class Dent {
        final int px;
        final int py;
        final long born;
        long hitAt = -1;

        Dent(int px, int py, long born) {
            this.px = px;
            this.py = py;
            this.born = born;
        }
    }

    private final ItemStack weapon;
    private final long start;
    private final int dentCount;
    private final long dentLife;
    private final List<Dent> dents = new ArrayList<>();
    private int hits;

    DentGame(ItemStack weapon) {
        this.weapon = weapon.copy();
        this.start = Util.getMillis();
        this.dentCount = DentForging.dents(weapon);
        // Steady Forge (Forgefire Old Fashioned): dents stay hot 50% longer
        var me = net.minecraft.client.Minecraft.getInstance().player;
        boolean steady = me != null && me.hasEffect(com.alexh.capitalism.core.ModEffects.STEADY_FORGE);
        this.dentLife = DentForging.dentLife(weapon) * (steady ? 3 : 2) / 2;
        long spawnEvery = DentForging.spawnEvery(weapon);
        RandomSource random = RandomSource.create();
        List<int[]> spots = ItemSilhouette.pixels(weapon.getItem());
        for (int i = 0; i < this.dentCount; i++) {
            int[] spot = pickSpot(spots, random);
            this.dents.add(new Dent(spot[0], spot[1], this.start + WARM_UP + i * spawnEvery));
        }
    }

    // A solid pixel far enough from the other dents (relaxing the rule if the weapon is too small for it)
    private int[] pickSpot(List<int[]> spots, RandomSource random) {
        for (int relax = 0; relax <= RECENT_GAP; relax++) {
            for (int attempt = 0; attempt < 40; attempt++) {
                int[] spot = spots.get(random.nextInt(spots.size()));
                if (farEnough(spot, Math.max(0, MIN_GAP - relax), Math.max(0, RECENT_GAP - relax))) return spot;
            }
        }
        return spots.get(random.nextInt(spots.size()));
    }

    private boolean farEnough(int[] spot, int gap, int recentGap) {
        for (int i = 0; i < this.dents.size(); i++) {
            Dent dent = this.dents.get(i);
            int distance = Math.max(Math.abs(dent.px - spot[0]), Math.abs(dent.py - spot[1]));
            boolean recent = i >= this.dents.size() - RECENT;
            if (distance < (recent ? recentGap : gap)) return false;
        }
        return true;
    }

    int hits() {
        return this.hits;
    }

    boolean isOver() {
        Dent last = this.dents.getLast();
        return Util.getMillis() > last.born + this.dentLife + END_PAUSE;
    }

    // The game uses the whole screen: the weapon as big as fits, centered, with text above and below
    private static int scale(int screenHeight) {
        return Mth.clamp((screenHeight - 60) / 16, 4, MAX_SCALE);
    }

    private static int weaponX(int screenWidth, int scale) {
        return (screenWidth - 16 * scale) / 2;
    }

    private static int weaponY(int screenHeight, int scale) {
        return (screenHeight - 16 * scale) / 2;
    }

    // A hammer blow: hits a live dent under the cursor
    void click(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        long now = Util.getMillis();
        int scale = scale(screenHeight);
        int wx = weaponX(screenWidth, scale);
        int wy = weaponY(screenHeight, scale);
        double reach = scale * 0.85;
        for (Dent dent : this.dents) {
            if (dent.hitAt >= 0 || now < dent.born || now > dent.born + this.dentLife) continue;
            double cx = wx + dent.px * scale + scale / 2.0;
            double cy = wy + dent.py * scale + scale / 2.0;
            if (Math.abs(mouseX - cx) <= reach && Math.abs(mouseY - cy) <= reach) {
                dent.hitAt = now;
                this.hits++;
                Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.ANVIL_LAND, 1.3F + this.hits * 0.04F, 0.35F));
                return;
            }
        }
    }

    void draw(GuiGraphicsExtractor graphics, Font font, int screenWidth, int screenHeight) {
        long now = Util.getMillis();
        int scale = scale(screenHeight);
        int wx = weaponX(screenWidth, scale);
        int wy = weaponY(screenHeight, scale);
        graphics.fill(0, 0, screenWidth, screenHeight, OVERLAY);
        graphics.centeredText(font, "Hammer the glowing dents!", screenWidth / 2, wy - 24, TITLE);
        graphics.centeredText(font, "Hits: " + this.hits + " / " + this.dentCount, screenWidth / 2, wy - 12, TITLE);
        graphics.pose().pushMatrix();
        graphics.pose().translate(wx, wy);
        graphics.pose().scale(scale, scale);
        graphics.item(this.weapon, 0, 0);
        graphics.pose().popMatrix();
        graphics.nextStratum();
        int radius = Math.max(3, Math.round(scale * 0.7F));
        for (Dent dent : this.dents) {
            if (now < dent.born) continue;
            int cx = wx + dent.px * scale + scale / 2;
            int cy = wy + dent.py * scale + scale / 2;
            if (dent.hitAt >= 0) {
                // a white spark where the hammer landed, fading fast
                if (now - dent.hitAt < FLASH) {
                    int alpha = (int) (255 * (1 - (now - dent.hitAt) / (float) FLASH));
                    drawDent(graphics, cx, cy, radius + 1, (alpha << 24) | 0xFFFFFF);
                }
                continue;
            }
            float age = (now - dent.born) / (float) this.dentLife;
            if (age > 1) continue;
            drawDent(graphics, cx, cy, radius, heat(age));
        }
        graphics.centeredText(font, "Esc to stop (nothing is used)", screenWidth / 2, wy + 16 * scale + 6, TEXT);
    }

    // White-hot -> yellow -> orange -> dull red as the dent cools, fading out at the end
    private static int heat(float age) {
        int r = 255;
        int g = (int) Mth.lerp(Math.min(1, age * 1.4F), 240, 40);
        int b = (int) Mth.lerp(Math.min(1, age * 3F), 200, 0);
        if (age > 0.7F) r = (int) Mth.lerp((age - 0.7F) / 0.3F, 255, 120);
        int alpha = age > 0.85F ? (int) Mth.lerp((age - 0.85F) / 0.15F, 255, 60) : 255;
        return (alpha << 24) | (r << 16) | (g << 8) | b;
    }

    // A round-ish glowing spot with a soft halo
    private static void drawDent(GuiGraphicsExtractor graphics, int cx, int cy, int r, int color) {
        int halo = ((color >>> 24) / 3 << 24) | (color & 0xFFFFFF);
        int corner = Math.max(1, r / 3);
        graphics.fill(cx - r - 2, cy - r + corner, cx + r + 2, cy + r - corner, halo);
        graphics.fill(cx - r + corner, cy - r - 2, cx + r - corner, cy + r + 2, halo);
        graphics.fill(cx - r, cy - r + corner, cx + r, cy + r - corner, color);
        graphics.fill(cx - r + corner, cy - r, cx + r - corner, cy + r, color);
    }
}
