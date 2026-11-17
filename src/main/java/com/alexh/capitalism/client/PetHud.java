package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.pets.PetData;
import com.alexh.capitalism.pets.Pets;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;

// Look at a pet (pets/Pets) and a little card under the crosshair shows its name, whose it is (or "No owner yet"),
// its hearts, hunger and thirst (PetData, synced to everyone who can see it).
public final class PetHud {

    private static final Identifier HEART_CONTAINER = Identifier.withDefaultNamespace("hud/heart/container");
    private static final Identifier HEART_FULL = Identifier.withDefaultNamespace("hud/heart/full");
    private static final Identifier HEART_HALF = Identifier.withDefaultNamespace("hud/heart/half");
    private static final Identifier FOOD_EMPTY = Identifier.withDefaultNamespace("hud/food_empty");
    private static final Identifier FOOD_HALF = Identifier.withDefaultNamespace("hud/food_half");
    private static final Identifier FOOD_FULL = Identifier.withDefaultNamespace("hud/food_full");
    private static final Identifier THIRST_EMPTY = Identifier.fromNamespaceAndPath(Capitalism.MODID, "hud/thirst_empty");
    private static final Identifier THIRST_HALF = Identifier.fromNamespaceAndPath(Capitalism.MODID, "hud/thirst_half");
    private static final Identifier THIRST_FULL = Identifier.fromNamespaceAndPath(Capitalism.MODID, "hud/thirst_full");

    // at most this many hearts in a row (a big horse has 15)
    private static final int MAX_HEARTS = 15;
    // how big the card under the crosshair is drawn
    private static final float CARD_SCALE = 0.6F;

    private PetHud() {
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Entity target = mc.crosshairPickEntity;
        if (mc.player == null || !(target instanceof Animal animal) || !Pets.isPet(animal)) return;
        PetData data = Pets.data(animal);
        Font font = mc.font;
        int cx = mc.getWindow().getGuiScaledWidth() / 2;
        int y = mc.getWindow().getGuiScaledHeight() / 2 + 12;
        // the whole card drawn small, shrunk around its top middle
        graphics.pose().pushMatrix();
        graphics.pose().translate(cx, y);
        graphics.pose().scale(CARD_SCALE, CARD_SCALE);
        graphics.pose().translate(-cx, -y);
        card(graphics, font, animal, data, cx, y);
        graphics.pose().popMatrix();
    }

    private static void card(GuiGraphicsExtractor graphics, Font font, Animal animal, PetData data, int cx, int y) {
        Component name = Pets.name(animal);
        String owner = data.claimed() ? "Owner: " + data.ownerName() : "No owner yet";
        int hearts = heartCount(animal);
        // its hat (designer/Hat), at the bottom: the hat and what it does
        com.alexh.capitalism.designer.Hat hat = data.pet() ? data.hat() : null;
        String hatText = hat == null ? "" : hat.title + ": " + hat.does.toLowerCase(java.util.Locale.ROOT);
        int width = Math.max(Math.max(font.width(owner), hat == null ? 0 : font.width(hatText) + 20),
            data.pet() ? Math.max(font.width(name), Math.max(hearts, Mth.ceil(data.max() / 2)) * 8) : 0) + 10;
        int height = data.pet() ? (hat == null ? 54 : 72) : 14;
        graphics.fill(cx - width / 2, y - 3, cx + width / 2, y + height, 0x90000000);

        if (data.pet()) {
            graphics.centeredText(font, name, cx, y, 0xFFFFD24A);
            y += 10;
        }
        graphics.centeredText(font, owner, cx, y, 0xFFC8C8C8);
        if (!data.pet()) return;
        y += 11;

        hearts(graphics, cx - hearts * 8 / 2, y, animal);
        y += 10;
        int bars = Mth.ceil(data.max() / 2) * 8 / 2;
        hunger(graphics, cx - bars, y, data.hunger(), data.max());
        y += 10;
        thirst(graphics, cx - bars, y, data.thirst(), data.max());
        if (hat == null) return;
        y += 12;
        int hx = cx - (font.width(hatText) + 18) / 2;
        graphics.item(new net.minecraft.world.item.ItemStack(hat.item()), hx, y - 4);
        graphics.text(font, hatText, hx + 18, y, 0xFF9CF08A, true);
    }

    // The animal's hearts (its own health, up to 15 hearts) from x = left (also the pet window, PetScreen)
    static void hearts(GuiGraphicsExtractor graphics, int left, int y, Animal animal) {
        int hearts = heartCount(animal);
        float health = Math.min(animal.getHealth(), hearts * 2f);
        for (int i = 0; i < hearts; i++) {
            int x = left + i * 8;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_CONTAINER, x, y, 9, 9);
            float here = health - i * 2;
            if (here >= 2) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_FULL, x, y, 9, 9);
            else if (here >= 1) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_HALF, x, y, 9, 9);
        }
    }

    static int heartCount(Animal animal) {
        return Math.min(MAX_HEARTS, Mth.ceil(animal.getMaxHealth() / 2f));
    }

    static void hunger(GuiGraphicsExtractor graphics, int left, int y, float hunger, float max) {
        row(graphics, left, y, hunger, max, FOOD_EMPTY, FOOD_HALF, FOOD_FULL, 9);
    }

    static void thirst(GuiGraphicsExtractor graphics, int left, int y, float thirst, float max) {
        row(graphics, left, y, thirst, max, THIRST_EMPTY, THIRST_HALF, THIRST_FULL, 7);
    }

    // An icon per 2 points of the bar (10 for 20; more with a Prada Fedora), filled from the right (it empties left to
    // right, like the hunger bar)
    private static void row(GuiGraphicsExtractor graphics, int left, int y, float value, float max, Identifier empty, Identifier half, Identifier full, int width) {
        int points = Mth.ceil(value);
        int icons = Mth.ceil(max / 2);
        for (int i = 0; i < icons; i++) {
            // like the player's own food bar: it fills from the right, so it empties from the left
            int x = left + (icons - 1 - i) * 8;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, empty, x, y, width, 9);
            int here = points - i * 2;
            if (here >= 2) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, full, x, y, width, 9);
            else if (here == 1) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, half, x, y, width, 9);
        }
    }
}
