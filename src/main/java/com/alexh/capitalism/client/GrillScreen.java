package com.alexh.capitalism.client;

import com.alexh.capitalism.farmer.Freshness;
import com.alexh.capitalism.farmer.GrillBlockEntity;
import com.alexh.capitalism.farmer.GrillMenu;
import com.alexh.capitalism.farmer.MeatCut;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

// The Grill (farmer/GrillMenu), drawn here: a kitchen's brick wall, the grill in front of it (three cut slots on the
// glowing grate, the fuel slot in the firebox door below, heat left beside it), a chalkboard of the doneness
// temperatures, and SEAR!, which takes the cut and starts the searing game (SearGame), drawn over everything until
// it's done. Under it all a steel counter with the inventory.
public class GrillScreen extends AbstractContainerScreen<GrillMenu> {

    private static final int STEEL = 0xFF3A3C42;
    private static final int STEEL_LIGHT = 0xFF6A6E76;
    private static final int CHALK = 0xFFE8E4D8;
    private static final int CHALK_DIM = 0xFFA8B0A0;

    private @Nullable Button searButton;
    private @Nullable SearGame game;

    public GrillScreen(GrillMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = 73;
    }

    @Override
    protected void init() {
        super.init();
        this.searButton = this.addRenderableWidget(Button.builder(Component.literal("Sear!"), b -> this.start())
            .bounds(this.leftPos + 96, this.topPos + 54, 72, 16).build());
        this.searButton.setTooltip(Tooltip.create(Component.literal(
            "Up to three at once. Flip each when it says FLIP!, and pull each off at Medium Rare.")));
        this.updateButton();
    }

    private void updateButton() {
        if (this.searButton == null) return;
        this.searButton.visible = this.game == null;
        boolean any = false;
        for (boolean on : this.menu.onGrill()) any |= on;
        this.searButton.active = any;
    }

    private void start() {
        if (this.game != null || this.minecraft.gameMode == null) return;
        boolean[] on = this.menu.onGrill();
        MeatCut[] cuts = new MeatCut[GrillBlockEntity.CUTS];
        boolean any = false;
        for (int i = 0; i < cuts.length; i++) {
            if (on[i]) {
                cuts[i] = GrillMenu.cutOf(this.menu.cut(i));
                any |= cuts[i] != null;
            }
        }
        if (!any) return;
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, GrillMenu.SEAR);
        this.game = new SearGame(cuts);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.game != null) {
            // each steak as it comes off
            if (this.minecraft.gameMode != null) {
                for (int code : this.game.takeResults()) this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, GrillMenu.RESULT + code);
            }
            if (this.game.isOver()) this.game = null;
        }
        this.updateButton();
    }

    // ── drawing ──

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int x0 = this.leftPos;
        int y0 = this.topPos;
        int w = this.imageWidth;
        int h = this.imageHeight;
        long now = Util.getMillis();
        graphics.fill(x0, y0, x0 + w, y0 + h, 0xFF120C08);

        // the brick wall
        for (int row = 0, y = y0 + 1; y < y0 + 78; row++, y += 6) {
            int shift = row % 2 == 0 ? 0 : 7;
            for (int x = x0 + 1 - shift; x < x0 + w - 1; x += 14) {
                int bx0 = Math.max(x0 + 1, x);
                int bx1 = Math.min(x0 + w - 1, x + 13);
                int tone = ((x * 7 + row * 13) & 3);
                int color = tone == 0 ? 0xFF6A3426 : tone == 1 ? 0xFF7A3C2A : tone == 2 ? 0xFF6E3A2C : 0xFF5E2E22;
                graphics.fill(bx0, y, bx1, Math.min(y0 + 78, y + 5), color);
            }
        }

        // the grill: a black iron box on legs, the grate glowing over the coals, the firebox door below
        int gx0 = x0 + 8;
        int gx1 = x0 + 88;
        graphics.fill(gx0 + 4, y0 + 74, gx0 + 8, y0 + 79, 0xFF0A0A0C);
        graphics.fill(gx1 - 8, y0 + 74, gx1 - 4, y0 + 79, 0xFF0A0A0C);
        graphics.fill(gx0 - 1, y0 + 17, gx1 + 1, y0 + 75, 0xFF050506);
        graphics.fill(gx0, y0 + 18, gx1, y0 + 74, 0xFF1C1C20);
        graphics.fill(gx0, y0 + 18, gx1, y0 + 19, 0xFF4A4A52);
        // the grate
        int ty0 = y0 + 21;
        int ty1 = y0 + 48;
        for (int cy = ty0; cy < ty1; cy += 3) {
            for (int cx = gx0 + 3; cx < gx1 - 3; cx += 3) {
                float glow = 0.5F + 0.5F * (float) Math.sin(now / 380.0 + cx * 0.45 + cy * 0.7);
                int r = (int) (110 + 120 * glow);
                int g = (int) (30 + 70 * glow * glow);
                graphics.fill(cx, cy, cx + 3, cy + 3, 0xFF000000 | (r << 16) | (g << 8) | 10);
            }
        }
        for (int bx = gx0 + 4; bx < gx1 - 3; bx += 6) {
            graphics.fill(bx, ty0, bx + 2, ty1, 0xFF34343A);
            graphics.fill(bx, ty0, bx + 1, ty1, 0xFF7A7A82);
        }
        // the firebox door, with a little window on the coals
        graphics.fill(gx0 + 6, y0 + 51, gx1 - 6, y0 + 72, 0xFF2A2A30);
        graphics.fill(gx0 + 6, y0 + 51, gx1 - 6, y0 + 52, 0xFF56565E);
        graphics.fill(gx1 - 16, y0 + 60, gx1 - 9, y0 + 62, 0xFFB8BCC4);
        // the slots: three cuts on the grate, the fuel in the door
        for (int i = 0; i < GrillBlockEntity.CUTS; i++) {
            int cx = x0 + GrillMenu.CUT_X + i * GrillMenu.CUT_STEP;
            int cy = y0 + GrillMenu.CUT_Y;
            DesignerDraw.slot(graphics, cx - 1, cy - 1, 18, 0xFF0A0A0C, 0xFFFFA04A, 0xFF3A2418);
            if (this.menu.cut(i).isEmpty()) DesignerDraw.ghost(graphics, new ItemStack(MeatCut.BEEF_RIB.cut()), cx, cy, 0xFF3A2418);
        }
        int fx = x0 + GrillMenu.FUEL_X;
        int fy = y0 + GrillMenu.FUEL_Y;
        DesignerDraw.slot(graphics, fx - 1, fy - 1, 18, 0xFF0A0A0C, 0xFF8A8A92, 0xFF1A1210);
        if (this.menu.fuel().isEmpty()) DesignerDraw.ghost(graphics, new ItemStack(Items.COAL), fx, fy, 0xFF1A1210);

        // the chalkboard
        int bx0 = x0 + 94;
        int bx1 = x0 + w - 6;
        graphics.fill(bx0, y0 + 4, bx1, y0 + 51, 0xFF6A4A2A);
        graphics.fill(bx0 + 2, y0 + 6, bx1 - 2, y0 + 49, 0xFF1E2A22);

        // the steel counter under it all
        graphics.fill(x0, y0 + 79, x0 + w, y0 + h, STEEL);
        graphics.fill(x0, y0 + 79, x0 + w, y0 + 80, STEEL_LIGHT);
        for (int i = 0; i < 36; i++) {
            int x = x0 + 7 + (i % 9) * 18;
            int y = y0 + (i < 27 ? 83 + (i / 9) * 18 : 141);
            DesignerDraw.slot(graphics, x, y, 18, 0xFF15161A, STEEL_LIGHT, 0xFF2A2C32);
        }
        graphics.fill(x0, y0, x0 + w, y0 + 1, 0xFF000000);
        graphics.fill(x0, y0 + h - 1, x0 + w, y0 + h, 0xFF000000);
        graphics.fill(x0, y0, x0 + 1, y0 + h, 0xFF000000);
        graphics.fill(x0 + w - 1, y0, x0 + w, y0 + h, 0xFF000000);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, 9, 6, 0xFFFFE6C0, true);
        // the chalkboard: the doneness temperatures
        graphics.pose().pushMatrix();
        graphics.pose().scale(0.75F, 0.75F);
        String[][] lines = {{"Rare", "120°"}, {"Med Rare", "130°"}, {"Medium", "140°"}, {"Med Well", "150°"}, {"Well Done", "160°"}, {"Burnt", "175°"}};
        int lx = Math.round(98 / 0.75F);
        int rx = Math.round(166 / 0.75F);
        int ly = Math.round(9 / 0.75F);
        for (int i = 0; i < lines.length; i++) {
            int y = ly + i * 10;
            graphics.text(this.font, lines[i][0], lx, y, i == lines.length - 1 ? 0xFFE08A7A : CHALK, false);
            graphics.text(this.font, lines[i][1], rx - this.font.width(lines[i][1]), y, CHALK_DIM, false);
        }
        graphics.pose().popMatrix();
        // heat left, and how aged the cut is
        int heat = GrillBlockEntity.heat(this.menu.charges(), this.menu.fuel());
        String heatText = heat > 0 ? heat + " steak" + (heat == 1 ? "" : "s") + " of heat" : "Add coal";
        graphics.text(this.font, heatText, 96, 73, heat > 0 ? 0xFFFFB070 : 0xFFFF6A50, false);
        for (int i = 0; i < GrillBlockEntity.CUTS; i++) {
            int aged = Freshness.agedLevel(this.menu.cut(i));
            if (aged > 0) graphics.text(this.font, aged == 2 ? "★★" : "★", GrillMenu.CUT_X + i * GrillMenu.CUT_STEP + 9, GrillMenu.CUT_Y - 9, 0xFFFFD86A, true);
        }
        graphics.text(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0xFFC8C8D0, false);
    }

    // ── the searing game: drawn over everything, takes every click ──

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (this.game == null) {
            super.extractRenderState(graphics, mouseX, mouseY, a);
            return;
        }
        this.extractContents(graphics, mouseX, mouseY, a);
        graphics.nextStratum();
        this.game.draw(graphics, this.font, this.width, this.height, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.game == null) return super.mouseClicked(event, doubleClick);
        this.game.press(event.x(), event.y(), event.button());
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return this.game != null || super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return this.game != null || super.mouseDragged(event, dx, dy);
    }

    // Esc pulls it off; Space flips, Enter pulls; every other key is ignored while searing
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.game == null) return super.keyPressed(event);
        if (event.isEscape()) this.game.giveUp();
        else this.game.key(event.key());
        return true;
    }
}
