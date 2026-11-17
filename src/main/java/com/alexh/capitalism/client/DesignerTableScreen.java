package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.designer.Bag;
import com.alexh.capitalism.designer.Design;
import com.alexh.capitalism.designer.Hat;
import com.alexh.capitalism.designer.DesignerTableBlockEntity;
import com.alexh.capitalism.designer.DesignerTableMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

// The Designer Table (textures/gui/container/designer_table.png, tools/make_designer_textures.ps1): plum velvet with
// gold trim. Tabs above it (Standard / Fancy / Luxury Bags, Hats); along the top that tab's designs (the chosen one in
// gold) and what it holds or does; across the green cutting
// mat the row of 6 patch slots, with faint ghosts of the patches the chosen bag needs (spare slots stitched shut, a red
// edge on a wrong patch); Leather and the needle bottom left, the bag bottom right. "Stitch" runs a needle along the
// row, sewing the patches together with gold thread, then the bag pops out with a sparkle.
public class DesignerTableScreen extends AbstractContainerScreen<DesignerTableMenu> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/gui/container/designer_table.png");
    private static final int TITLE = 0xFFFFE6B8;
    private static final int LABEL = 0xFFE6CFA8;
    private static final int GOLD = 0xFFE8C062;
    private static final int GOLD_DARK = 0xFF9A7428;
    private static final int SLOT_EDGE = 0xFF1A0A14;
    private static final int SLOT_LIP = 0xFF7A4A66;
    private static final int SLOT_INSIDE = 0xFF3A1A2E;
    private static final int MAT_INSIDE = 0xFF1E3A2A;
    private static final int TILE_X = 8;
    private static final int TILE_Y = 19;
    private static final int TILE = 22;
    private static final int TILE_STEP = 24;
    // the tabs sticking up above the table
    private static final int TAB_X = 8;
    private static final int TAB_W = 28;
    private static final int TAB_H = 22;
    private static final int TAB_STEP = 31;
    private static final long STITCH_MS = 1600;
    private static final long POP_MS = 700;

    private @Nullable Button stitchButton;
    // the Stitch animation (0: not running) and when the bag popped out
    private long stitchingSince;
    private long poppedAt;
    private boolean hadOutput;
    private Design.Line tab = Design.Line.STANDARD;

    public DesignerTableScreen(DesignerTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 222);
        this.inventoryLabelY = 129;
    }

    @Override
    protected void init() {
        super.init();
        this.stitchButton = this.addRenderableWidget(Button.builder(Component.literal("Stitch"), b -> this.startStitching())
            .bounds(this.leftPos + 60, this.topPos + 99, 72, 16).build());
        this.hadOutput = this.menu.getSlot(DesignerTableBlockEntity.OUTPUT).hasItem();
        this.tab = this.menu.design().line();
        this.updateButton();
    }

    private void press(int id) {
        if (this.minecraft.gameMode != null) this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
    }

    private void startStitching() {
        if (this.menu.problem() != null || this.stitchingSince != 0) return;
        this.stitchingSince = Util.getMillis();
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_LOOM_SELECT_PATTERN, 0.8F));
    }

    private void updateButton() {
        if (this.stitchButton == null) return;
        String problem = this.menu.problem();
        this.stitchButton.active = problem == null && this.stitchingSince == 0;
        this.stitchButton.setTooltip(problem == null ? Tooltip.create(Component.literal("Sew the patches and a Leather into a " + this.menu.design().title() + ".")) : null);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        long now = Util.getMillis();
        if (this.stitchingSince != 0) {
            // a click of the needle every few steps along the row
            long t = now - this.stitchingSince;
            if ((t / 110) % 2 == 0) this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.9F, 0.06F));
            if (t >= STITCH_MS) {
                this.press(DesignerTableMenu.MAKE);
                this.stitchingSince = 0;
            }
        }
        boolean hasOutput = this.menu.getSlot(DesignerTableBlockEntity.OUTPUT).hasItem();
        if (hasOutput && !this.hadOutput) this.poppedAt = now;
        this.hadOutput = hasOutput;
        this.updateButton();
    }

    // ── drawing ──

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int xo = this.leftPos;
        int yo = this.topPos;
        long now = Util.getMillis();
        this.drawTabs(graphics, xo, yo, mouseX, mouseY, false);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        this.drawTabs(graphics, xo, yo, mouseX, mouseY, true);
        Design chosen = this.menu.design();

        // this tab's designs
        List<Design> shown = Design.inLine(this.tab);
        for (int i = 0; i < shown.size(); i++) {
            Design bag = shown.get(i);
            int x = xo + TILE_X + i * TILE_STEP;
            int y = yo + TILE_Y;
            boolean hover = mouseX >= x && mouseX < x + TILE && mouseY >= y && mouseY < y + TILE;
            DesignerDraw.slot(graphics, x, y, TILE, SLOT_EDGE, SLOT_LIP, bag == chosen ? 0xFF4A2A1E : hover ? 0xFF4A2440 : SLOT_INSIDE);
            graphics.item(new ItemStack(bag.item()), x + 3, y + 3);
            if (bag == chosen) DesignerDraw.goldFrame(graphics, x, y, TILE, TILE);
        }
        // what the chosen design is (squeezed to fit)
        int infoX = xo + TILE_X + 4 * TILE_STEP + 2;
        int room = xo + this.imageWidth - 8 - infoX;
        this.fitText(graphics, chosen.title(), infoX, yo + TILE_Y + 1, room, GOLD);
        if (chosen instanceof Bag bag) {
            this.fitText(graphics, "+" + bag.slots + " slots", infoX, yo + TILE_Y + 11, room, 0xFFFFF0D0);
            this.fitText(graphics, bag.groups.size() == 1 ? bag.holdsText() : "Mixed (hover)", infoX, yo + TILE_Y + 21, room, 0xFFB8F0A8);
        } else if (chosen instanceof Hat hat) {
            this.fitText(graphics, "A hat for a pet", infoX, yo + TILE_Y + 11, room, 0xFFFFF0D0);
            this.fitText(graphics, hat.does, infoX, yo + TILE_Y + 21, room, 0xFFB8F0A8);
        }

        this.drawPatches(graphics, xo, yo, now, chosen);

        // Leather, the needle, and the bag
        this.slotWithGhost(graphics, xo + DesignerTableMenu.LEATHER_X, yo + DesignerTableMenu.LEATHER_Y, DesignerTableBlockEntity.LEATHER, new ItemStack(Items.LEATHER));
        this.slotWithGhost(graphics, xo + DesignerTableMenu.NEEDLE_X, yo + DesignerTableMenu.NEEDLE_Y, DesignerTableBlockEntity.NEEDLE, new ItemStack(DesignerTableBlockEntity.needleFor(this.menu.design())));
        int ox = xo + DesignerTableMenu.OUTPUT_X;
        int oy = yo + DesignerTableMenu.OUTPUT_Y;
        DesignerDraw.slot(graphics, ox - 5, oy - 5, 26, SLOT_EDGE, GOLD_DARK, SLOT_INSIDE);
        if (!this.menu.getSlot(DesignerTableBlockEntity.OUTPUT).hasItem()) DesignerDraw.ghost(graphics, new ItemStack(chosen.item()), ox, oy, SLOT_INSIDE);
        this.drawPop(graphics, now, ox + 8, oy + 8);
    }

    private void slotWithGhost(GuiGraphicsExtractor graphics, int x, int y, int slot, ItemStack ghost) {
        DesignerDraw.slot(graphics, x - 1, y - 1, 18, SLOT_EDGE, SLOT_LIP, SLOT_INSIDE);
        if (!this.menu.getSlot(slot).hasItem()) DesignerDraw.ghost(graphics, ghost, x, y, SLOT_INSIDE);
    }

    // The row on the cutting mat: ghosts of the patches the bag needs, spare slots stitched shut, a red edge round a
    // wrong patch; while stitching, a needle runs along the row leaving gold thread across the patches
    private void drawPatches(GuiGraphicsExtractor graphics, int xo, int yo, long now, Design bag) {
        List<Item> wanted = bag.patches();
        for (int i = 0; i < Bag.MAX_PATCHES; i++) {
            int x = xo + DesignerTableMenu.PATCH_X + i * DesignerTableMenu.PATCH_STEP;
            int y = yo + DesignerTableMenu.PATCH_Y;
            ItemStack in = this.menu.getSlot(DesignerTableBlockEntity.PATCHES + i).getItem();
            boolean needed = i < wanted.size();
            boolean wrong = !in.isEmpty() && (!needed || !in.is(wanted.get(i)));
            DesignerDraw.slot(graphics, x - 1, y - 1, 18, SLOT_EDGE, wrong ? 0xFFE04848 : needed ? 0xFF6A9A7A : 0xFF2A4A36, needed ? MAT_INSIDE : 0xFF16281E);
            if (!needed && in.isEmpty()) {
                DesignerDraw.thread(graphics, x + 3, y + 3, x + 12, y + 12, 0x60F0E6C8, 0);
                DesignerDraw.thread(graphics, x + 12, y + 3, x + 3, y + 12, 0x60F0E6C8, 0);
            } else if (needed && in.isEmpty()) {
                DesignerDraw.ghost(graphics, new ItemStack(wanted.get(i)), x, y, MAT_INSIDE);
            }
        }
        if (this.stitchingSince == 0) return;
        // the needle running along the row, gold thread behind it
        float t = Mth.clamp((now - this.stitchingSince) / (float) STITCH_MS, 0, 1);
        int x0 = xo + DesignerTableMenu.PATCH_X - 2;
        int x1 = xo + DesignerTableMenu.PATCH_X + (wanted.size() - 1) * DesignerTableMenu.PATCH_STEP + 18;
        int y = yo + DesignerTableMenu.PATCH_Y + 8;
        int nx = x0 + Math.round((x1 - x0) * t);
        graphics.nextStratum();
        for (int x = x0; x < nx; x++) {
            int wave = (x / 3) % 2 == 0 ? -3 : 3;
            if (x % 6 < 4) graphics.fill(x, y + wave, x + 1, y + wave + 1, GOLD);
        }
        int bob = (Math.sin(now / 20.0) > 0) ? 2 : -1;
        graphics.fill(nx, y - 12 + bob, nx + 1, y + 2 + bob, 0xFFF4F8FF);
        graphics.fill(nx - 1, y - 12 + bob, nx + 2, y - 10 + bob, 0xFF8E949C);
        graphics.fill(nx, y - 20, nx + 1, y - 12 + bob, 0xB0E8C062);
    }

    // Text squeezed smaller to fit in a width (with a shadow)
    private void fitText(GuiGraphicsExtractor graphics, String text, int x, int y, int width, int color) {
        float scale = Math.min(1.0F, width / (float) Math.max(1, this.font.width(text)));
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y + (1 - scale) * 4);
        graphics.pose().scale(scale, scale);
        graphics.text(this.font, text, 0, 0, color, true);
        graphics.pose().popMatrix();
    }

    // The tabs above the table: velvet with a gold edge, each with its first design on it; the open one joins the table
    private void drawTabs(GuiGraphicsExtractor graphics, int xo, int yo, int mouseX, int mouseY, boolean open) {
        for (Design.Line line : Design.Line.values()) {
            if ((line == this.tab) != open) continue;
            int x = xo + TAB_X + line.ordinal() * TAB_STEP;
            int y = yo - TAB_H + (open ? 0 : 3);
            int h = TAB_H + (open ? 3 : 0);
            boolean hover = mouseX >= x && mouseX < x + TAB_W && mouseY >= y && mouseY < yo;
            graphics.fill(x, y, x + TAB_W, y + h, 0xFF1A0A14);
            graphics.fill(x + 1, y + 1, x + TAB_W - 1, y + h, open ? 0xFF4E2240 : hover ? 0xFF3E1A32 : 0xFF2C1224);
            graphics.fill(x + 1, y + 1, x + TAB_W - 1, y + 2, open ? GOLD : GOLD_DARK);
            graphics.item(new ItemStack(Design.inLine(line).getFirst().item()), x + 6, y + 4);
        }
    }

    // The bag popping out: a ring of gold sparkles flying out and fading
    private void drawPop(GuiGraphicsExtractor graphics, long now, int cx, int cy) {
        float t = (now - this.poppedAt) / (float) POP_MS;
        if (this.poppedAt == 0 || t > 1) return;
        graphics.nextStratum();
        int alpha = Math.round(255 * (1 - t));
        for (int i = 0; i < 10; i++) {
            float angle = i * Mth.TWO_PI / 10 + t;
            float r = 8 + 16 * t;
            int px = cx + Math.round(Mth.cos(angle) * r);
            int py = cy + Math.round(Mth.sin(angle) * r);
            int color = (alpha << 24) | (i % 2 == 0 ? 0xFFE08A : 0xFFFFFF);
            graphics.fill(px - 1, py, px + 2, py + 1, color);
            graphics.fill(px, py - 1, px + 1, py + 2, color);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, 8, 7, TITLE, true);
        graphics.text(this.font, "Patches", DesignerTableMenu.PATCH_X, DesignerTableMenu.PATCH_Y - 11, LABEL, false);
        DesignerDraw.status(graphics, this.font, this.playerInventoryTitle, this.menu.problem(), "Ready to stitch", this.imageWidth, this.inventoryLabelY, LABEL);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.stitchingSince == 0) {
            for (Design.Line line : Design.Line.values()) {
                int x = this.leftPos + TAB_X + line.ordinal() * TAB_STEP;
                if (event.x() >= x && event.x() < x + TAB_W && event.y() >= this.topPos - TAB_H && event.y() < this.topPos) {
                    // a new tab picks its first design
                    if (line != this.tab) this.press(Design.all().indexOf(Design.inLine(line).getFirst()));
                    this.tab = line;
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.2F));
                    return true;
                }
            }
            List<Design> shown = Design.inLine(this.tab);
            for (int i = 0; i < shown.size(); i++) {
                int x = this.leftPos + TILE_X + i * TILE_STEP;
                int y = this.topPos + TILE_Y;
                if (event.x() >= x && event.x() < x + TILE && event.y() >= y && event.y() < y + TILE) {
                    this.press(Design.all().indexOf(shown.get(i)));
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_LOOM_SELECT_PATTERN, 1.2F));
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    // Hovering a bag: its name, its slots and the patches it takes
    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        // an empty slot: exactly what goes in it, by name
        int slot = this.hoveredSlot == null ? -1 : this.menu.slots.indexOf(this.hoveredSlot);
        if (slot >= 0 && slot < DesignerTableBlockEntity.SIZE && !this.hoveredSlot.hasItem()) {
            List<Item> wanted = this.menu.design().patches();
            int patch = slot - DesignerTableBlockEntity.PATCHES;
            String need = slot == DesignerTableBlockEntity.LEATHER ? "Needs: Leather"
                : slot == DesignerTableBlockEntity.NEEDLE ? "Needs: " + new ItemStack(DesignerTableBlockEntity.needleFor(this.menu.design())).getHoverName().getString() + "s (uses " + DesignerTableBlockEntity.NEEDLES_USED + ")"
                : slot == DesignerTableBlockEntity.OUTPUT ? "Your " + this.menu.design().title() + " appears here"
                : patch < wanted.size() ? "Needs: " + new ItemStack(wanted.get(patch)).getHoverName().getString() : "Not needed for this one";
            graphics.setTooltipForNextFrame(this.font, Component.literal(need), mouseX, mouseY);
        }
        for (Design.Line line : Design.Line.values()) {
            int x = this.leftPos + TAB_X + line.ordinal() * TAB_STEP;
            if (mouseX >= x && mouseX < x + TAB_W && mouseY >= this.topPos - TAB_H && mouseY < this.topPos) {
                graphics.setTooltipForNextFrame(this.font, Component.literal(line.title), mouseX, mouseY);
            }
        }
        List<Design> shown = Design.inLine(this.tab);
        int infoLeft = this.leftPos + TILE_X + 4 * TILE_STEP;
        for (int i = -1; i < shown.size(); i++) {
            // -1: the chosen design's info text, the rest: the tiles
            Design bag = i < 0 ? this.menu.design() : shown.get(i);
            int x = i < 0 ? infoLeft : this.leftPos + TILE_X + i * TILE_STEP;
            int w = i < 0 ? this.leftPos + this.imageWidth - infoLeft : TILE;
            int y = this.topPos + TILE_Y;
            if (mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + TILE) {
                List<Component> lines = new ArrayList<>();
                lines.add(Component.literal(bag.title()).withStyle(ChatFormatting.GOLD));
                if (bag instanceof Bag b) {
                    lines.add(Component.literal(b.slots + " slots:").withStyle(ChatFormatting.GRAY));
                    for (Bag.Group group : b.groups) lines.add(Component.literal("  " + group.count() + " " + group.holds().title.toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.GRAY));
                } else if (bag instanceof Hat hat) {
                    lines.add(Component.literal("A hat for a pet: " + hat.does.toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.GRAY));
                }
                lines.add(Component.literal("Patches:").withStyle(ChatFormatting.DARK_GRAY));
                for (Item patch : bag.patches()) lines.add(Component.literal("- " + new ItemStack(patch).getHoverName().getString()).withStyle(ChatFormatting.GRAY));
                graphics.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
            }
        }
    }
}
