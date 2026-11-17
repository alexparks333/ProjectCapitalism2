package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.block.AssemblyTableBlock;
import com.alexh.capitalism.menu.AssemblyTableMenu;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;

// Sushi Table (bamboo mat with 4 slots in a row) and Cooking Table (5 slots around a plate).
// The Sushi Table has a ROLL button under the mat, a little timing game: click and HOLD it, the bar fills up
// (0.85-1.5 s, a different speed each time) and the bamboo mat rolls over the ingredients, and a green zone sits somewhere in the back half
// (a new spot and a new width, 8-20% of the bar, each time).
// Let go IN the green = sushi. Too early OR too late (or holding it all the way) = squashed: the ingredients
// are gone and you get Squashed Sushi. The bar's speed changes every attempt too (0.85-1.5 s).
public class AssemblyTableScreen extends AbstractContainerScreen<AssemblyTableMenu> {

    private static final Identifier SUSHI = Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/gui/container/sushi_table.png");
    private static final Identifier COOKING = Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/gui/container/cooking_table.png");

    private static final int TITLE_COLOR = 0xFFFFF1D6;
    private static final int LABEL_COLOR = 0xFFF0E0C0;

    // ROLL button and the mat it rolls up (positions inside the panel; match sushi_table.png)
    private static final int BTN_X0 = 26;
    private static final int BTN_Y0 = 52;
    private static final int BTN_X1 = 82;
    private static final int BTN_Y1 = 66;
    private static final int MAT_X0 = 12;
    private static final int MAT_Y0 = 18;
    private static final int MAT_X1 = 95;
    private static final int MAT_Y1 = 50;
    // how long the bar takes to fill this attempt: somewhere from quick (0.85 s) to slow (1.5 s)
    private long rollMs = 1150;

    private boolean holding;
    private long holdStart;
    // where this attempt's green zone starts (0..1 along the bar)
    private float zoneStart = 0.7F;
    // and how wide it is this time (sometimes generous, sometimes tiny)
    private float zoneWidth = 0.14F;
    // a short message on the button after letting go ("Perfect!", "Too early", "Squashed!")
    private String flash = "";
    private int flashColor;
    private long flashUntil;

    public AssemblyTableScreen(AssemblyTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    private boolean isSushi() {
        return this.menu.kind() == AssemblyTableBlock.Kind.SUSHI;
    }

    @Override
    protected void init() {
        super.init();
        // The Cooking Table's plate sits under the middle of the top edge, so its title goes on the right.
        this.titleLabelX = this.menu.kind() == AssemblyTableBlock.Kind.COOKING
            ? 96 + (this.imageWidth - 96 - this.font.width(this.title)) / 2
            : (this.imageWidth - this.font.width(this.title)) / 2;
    }

    private float rollProgress() {
        if (!this.holding) return 0;
        return Math.min(1f, (Util.getMillis() - this.holdStart) / (float) this.rollMs);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        Identifier texture = isSushi() ? SUSHI : COOKING;
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        if (isSushi()) drawRollButton(graphics, mouseX, mouseY);
    }

    private void drawRollButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // Holding: give up if the ingredients stop making anything; holding all the way to the end squashes it
        if (this.holding && !this.menu.canRoll()) this.holding = false;
        if (this.holding && rollProgress() >= 1f) finishRoll(1f);
        float progress = rollProgress();

        int x0 = this.leftPos + BTN_X0;
        int y0 = this.topPos + BTN_Y0;
        int x1 = this.leftPos + BTN_X1;
        int y1 = this.topPos + BTN_Y1;
        boolean enabled = this.menu.canRoll();
        boolean hover = mouseX >= x0 && mouseX < x1 && mouseY >= y0 && mouseY < y1;
        int edge = enabled ? 0xFF5A1612 : 0xFF3A3430;
        int face = !enabled ? 0xFF7A7068 : hover || this.holding ? 0xFFD8483E : 0xFFB8342C;
        graphics.fill(x0, y0, x1, y1, edge);
        graphics.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, face);
        graphics.fill(x0 + 1, y0 + 1, x1 - 1, y0 + 2, 0x40FFFFFF);

        int inner = x1 - x0 - 2;
        String label = "ROLL";
        int labelColor = enabled ? 0xFFFFF4E6 : 0xFFB8B0A8;
        if (this.holding) {
            // the bar fills left to right, with the green zone to let go in
            int zx0 = x0 + 1 + Math.round(inner * this.zoneStart);
            int zx1 = x0 + 1 + Math.round(inner * Math.min(1f, this.zoneStart + this.zoneWidth));
            int fillTo = x0 + 1 + Math.round(inner * progress);
            graphics.fill(x0 + 1, y0 + 1, fillTo, y1 - 1, 0xFFF2B36A);
            graphics.fill(zx0, y0 + 1, zx1, y1 - 1, 0xC040D060);
            graphics.fill(fillTo - 1, y0, fillTo + 1, y1, 0xFFFFFFFF);
            boolean inZone = progress >= this.zoneStart && progress <= this.zoneStart + this.zoneWidth;
            label = inZone ? "NOW!" : "";
            labelColor = 0xFFFFFFFF;
        } else if (Util.getMillis() < this.flashUntil) {
            label = this.flash;
            labelColor = this.flashColor;
        }
        if (!label.isEmpty()) graphics.centeredText(this.font, label, (x0 + x1) / 2, y0 + 3, labelColor);
    }

    // Let go (or held to the very end): judge where the bar was.
    private void finishRoll(float progress) {
        this.holding = false;
        // only inside the green counts: too early or too late both squash it
        boolean perfect = progress >= this.zoneStart && progress <= this.zoneStart + this.zoneWidth;
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId,
            perfect ? AssemblyTableMenu.BUTTON_ROLL : AssemblyTableMenu.BUTTON_SQUASH);
        if (perfect) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BAMBOO_PLACE, 1.3F));
            showFlash("Perfect!", 0xFF9CFFA8);
        } else {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.SLIME_SQUISH, 0.8F));
            showFlash(progress < this.zoneStart ? "Too early!" : "Too late!", 0xFFFFB0A0);
        }
    }

    private void showFlash(String text, int color) {
        this.flash = text;
        this.flashColor = color;
        this.flashUntil = Util.getMillis() + 1200;
    }


    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, TITLE_COLOR, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, LABEL_COLOR, false);
        if (isSushi()) drawRollingMat(graphics);
    }

    // The bamboo mat rolling up from the left over the ingredients while ROLL is held.
    private void drawRollingMat(GuiGraphicsExtractor graphics) {
        float progress = rollProgress();
        if (progress <= 0) return;
        int rollX = MAT_X0 + Math.round((MAT_X1 - MAT_X0 - 8) * progress);
        // rolled-up part: the mat folded over everything behind the roll
        for (int x = MAT_X0; x < rollX; x++) {
            int slat = ((x - MAT_X0) / 3) % 2 == 0 ? 0xFFC4AA68 : 0xFFB69C5C;
            if ((x - MAT_X0) % 3 == 2) slat = 0xFF78623A;
            graphics.fill(x, MAT_Y0, x + 1, MAT_Y1, slat);
        }
        graphics.fill(MAT_X0, MAT_Y0 + 6, rollX, MAT_Y0 + 7, 0xFF465E2C);
        graphics.fill(MAT_X0, MAT_Y1 - 7, rollX, MAT_Y1 - 6, 0xFF465E2C);
        // the roll itself: a fat white cylinder of rice, with a soft shadow and a few grains
        graphics.fill(rollX, MAT_Y0 - 2, rollX + 8, MAT_Y1 + 2, 0xFFB8B4A8);
        graphics.fill(rollX + 1, MAT_Y0 - 1, rollX + 7, MAT_Y1 + 1, 0xFFF8F8F0);
        graphics.fill(rollX + 5, MAT_Y0 - 1, rollX + 7, MAT_Y1 + 1, 0xFFDEDCD0);
        graphics.fill(rollX + 2, MAT_Y0 - 1, rollX + 3, MAT_Y1 + 1, 0xFFFFFFFF);
        for (int y = MAT_Y0 + 1; y < MAT_Y1; y += 4) {
            graphics.fill(rollX + 3 + (y % 3), y, rollX + 4 + (y % 3), y + 1, 0xFFD0CEC2);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (isSushi() && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            double mx = event.x() - this.leftPos;
            double my = event.y() - this.topPos;
            if (mx >= BTN_X0 && mx < BTN_X1 && my >= BTN_Y0 && my < BTN_Y1) {
                if (this.menu.canRoll()) {
                    this.holding = true;
                    this.holdStart = Util.getMillis();
                    // a new green zone every attempt: 8-20% of the bar wide, somewhere in the back half
                    this.rollMs = 850 + (long) (Math.random() * 650);
                    this.zoneWidth = 0.08F + (float) Math.random() * 0.12F;
                    this.zoneStart = 0.5F + (float) Math.random() * (0.45F - this.zoneWidth);
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.holding && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            finishRoll(rollProgress());
            return true;
        }
        return super.mouseReleased(event);
    }
}
