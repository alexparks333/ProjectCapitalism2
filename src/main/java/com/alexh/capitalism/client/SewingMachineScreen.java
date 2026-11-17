package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.designer.Pattern;
import com.alexh.capitalism.designer.SewingMachineMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.List;

// The Sewing Machine (textures/gui/container/sewing_machine.png, tools/make_designer_textures.ps1): black enamel with
// gold scrollwork. Down the left, the patterns to choose from; in the middle, the patch being made, big, on a round of
// chalked seams, one for each material, which fill in with gold thread as they're sewn on (the next one has the needle
// bobbing over it); under it the row of material slots, showing faint ghosts of what goes in each, with a gold arrow
// over the next one. On the right the needle and the finished patches. "Sew" starts the needle game (SewingGame) for
// the next material; "Unpick" starts the patch again.
public class SewingMachineScreen extends AbstractContainerScreen<SewingMachineMenu> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/gui/container/sewing_machine.png");
    private static final int TITLE = 0xFFF6E2B0;
    private static final int LABEL = 0xFFD8C69A;
    private static final int GOLD = 0xFFE2BC5C;
    private static final int CHALK = 0xFFE8E2D0;
    private static final int SLOT_EDGE = 0xFF0A0E0C;
    private static final int SLOT_LIP = 0xFF4A5A50;
    private static final int SLOT_INSIDE = 0xFF26302B;
    // the pattern tiles down the left
    private static final int TILE_X = 8;
    private static final int TILE_Y = 20;
    private static final int TILE = 20;
    private static final int TILE_STEP = 22;
    // the big patch in the middle, and the round of seams round it
    private static final int PATCH_CX = 88;
    private static final int PATCH_CY = 50;
    private static final int RING = 30;
    // the pattern tabs (Generic, Fancy, Luxury) sticking up above the machine
    private static final int TAB_X = 8;
    private static final int TAB_W = 28;
    private static final int TAB_H = 22;
    private static final int TAB_STEP = 31;

    private @Nullable Button sewButton;
    private @Nullable Button unpickButton;
    private @Nullable SewingGame game;
    // when the last material was sewn on (a little burst of gold round its seam)
    private int lastSewn;
    // the patterns tab showing (Generic, Fancy, Luxury)
    private Pattern.Tier tab = Pattern.Tier.GENERIC;
    // the pattern last seen (the tab follows it when it arrives from the machine or changes)
    private @Nullable Pattern shownPattern;
    private long sewnAt;

    public SewingMachineScreen(SewingMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 222);
        this.inventoryLabelY = 129;
    }

    @Override
    protected void init() {
        super.init();
        this.sewButton = this.addRenderableWidget(Button.builder(Component.literal("Sew"), b -> this.startSewing())
            .bounds(this.leftPos + 38, this.topPos + 113, 58, 14).build());
        this.unpickButton = this.addRenderableWidget(Button.builder(Component.literal("Unpick"), b -> this.press(SewingMachineMenu.UNPICK))
            .bounds(this.leftPos + 100, this.topPos + 113, 44, 14).build());
        this.unpickButton.setTooltip(Tooltip.create(Component.literal("Start this patch again. What's sewn on so far is lost.")));
        this.lastSewn = this.menu.sewn();
        if (this.menu.pattern() != null) this.tab = this.menu.pattern().tier;
        this.updateButtons();
    }

    private void press(int id) {
        if (this.minecraft.gameMode != null) this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
    }

    private void startSewing() {
        Pattern pattern = this.menu.pattern();
        int next = this.menu.next();
        if (this.menu.problem() != null || pattern == null || next < 0) return;
        this.game = new SewingGame(pattern.tier, pattern.stitches.get(next), this.menu.getSlot(next).getItem(), this.menu.needle());
    }

    private void updateButtons() {
        if (this.sewButton == null || this.unpickButton == null) return;
        String problem = this.menu.problem();
        this.sewButton.visible = this.game == null;
        this.unpickButton.visible = this.game == null;
        this.sewButton.active = problem == null;
        Pattern pattern = this.menu.pattern();
        int next = this.menu.next();
        this.sewButton.setTooltip(problem == null && pattern != null && next >= 0
            ? Tooltip.create(Component.literal("Sew on the " + pattern.stitches.get(next).stack().getHoverName().getString()
                + ". Keep the needle on the seam: slips wear the needle."))
            : null);
        this.unpickButton.active = this.menu.sewn() != 0;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.game != null && this.game.isOver()) {
            this.press(SewingMachineMenu.SEW + this.game.mistakes());
            this.game = null;
        }
        Pattern pattern = this.menu.pattern();
        if (pattern != this.shownPattern) {
            this.shownPattern = pattern;
            if (pattern != null) this.tab = pattern.tier;
        }
        int sewn = this.menu.sewn();
        if (sewn != this.lastSewn) {
            if (Integer.bitCount(sewn) > Integer.bitCount(this.lastSewn) || sewn == 0) this.sewnAt = Util.getMillis();
            this.lastSewn = sewn;
        }
        this.updateButtons();
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
        Pattern pattern = this.menu.pattern();

        this.drawPatterns(graphics, xo, yo, pattern, mouseX, mouseY);
        this.drawPatch(graphics, xo, yo, now, pattern);
        this.drawMaterials(graphics, xo, yo, now, pattern);

        // the needle and the finished patches
        DesignerDraw.slot(graphics, xo + SewingMachineMenu.NEEDLE_X - 1, yo + SewingMachineMenu.NEEDLE_Y - 1, 18, SLOT_EDGE, SLOT_LIP, SLOT_INSIDE);
        if (this.menu.needle().isEmpty()) {
            DesignerDraw.ghost(graphics, new ItemStack(pattern != null ? pattern.needle() : com.alexh.capitalism.core.ModItems.IRON_SEWING_NEEDLE.get()),
                xo + SewingMachineMenu.NEEDLE_X, yo + SewingMachineMenu.NEEDLE_Y, SLOT_INSIDE);
        }
        DesignerDraw.slot(graphics, xo + SewingMachineMenu.OUTPUT_X - 5, yo + SewingMachineMenu.OUTPUT_Y - 5, 26, SLOT_EDGE, GOLD, SLOT_INSIDE);
    }

    // The tabs above the machine: enamel with a gold edge, each with its tier's first patch on it; the open one joins the
    // machine (drawn after it, over its top edge), the others sit behind, darker
    private void drawTabs(GuiGraphicsExtractor graphics, int xo, int yo, int mouseX, int mouseY, boolean open) {
        for (Pattern.Tier tier : Pattern.Tier.values()) {
            if ((tier == this.tab) != open) continue;
            int x = xo + TAB_X + tier.ordinal() * TAB_STEP;
            int y = yo - TAB_H + (open ? 0 : 3);
            int h = TAB_H + (open ? 3 : 0);
            boolean hover = mouseX >= x && mouseX < x + TAB_W && mouseY >= y && mouseY < yo;
            int body = open ? 0xFF1E2823 : hover ? 0xFF1A231E : 0xFF121814;
            graphics.fill(x, y, x + TAB_W, y + h, 0xFF0A0E0C);
            graphics.fill(x + 1, y + 1, x + TAB_W - 1, y + h, body);
            graphics.fill(x + 1, y + 1, x + TAB_W - 1, y + 2, open ? GOLD : 0xFF8E6E26);
            graphics.item(new ItemStack(Pattern.inTier(tier).getFirst().patch()), x + 6, y + 4);
        }
    }

    // The patterns: a tile each with its patch on it; the chosen one in gold. While a patch is half sewn the others are
    // dimmed (unpick it to change).
    private void drawPatterns(GuiGraphicsExtractor graphics, int xo, int yo, @Nullable Pattern chosen, int mouseX, int mouseY) {
        boolean locked = this.menu.sewn() != 0;
        List<Pattern> shown = Pattern.inTier(this.tab);
        for (int i = 0; i < shown.size(); i++) {
            Pattern pattern = shown.get(i);
            int x = xo + TILE_X;
            int y = yo + TILE_Y + i * TILE_STEP;
            boolean hover = mouseX >= x && mouseX < x + TILE && mouseY >= y && mouseY < y + TILE;
            int inside = pattern == chosen ? 0xFF3A3220 : hover && !locked ? 0xFF34443A : SLOT_INSIDE;
            DesignerDraw.slot(graphics, x, y, TILE, SLOT_EDGE, SLOT_LIP, inside);
            graphics.item(new ItemStack(pattern.patch()), x + 2, y + 2);
            if (pattern == chosen) DesignerDraw.goldFrame(graphics, x, y, TILE, TILE);
            else if (locked) {
                graphics.nextStratum();
                graphics.fill(x + 1, y + 1, x + TILE - 1, y + TILE - 1, 0xA0101412);
            }
        }
    }

    // The patch being made, big in the middle, faint until it's done; round it one chalked seam per material, gold
    // thread over the ones sewn on, the needle bobbing over the next
    private void drawPatch(GuiGraphicsExtractor graphics, int xo, int yo, long now, @Nullable Pattern pattern) {
        int cx = xo + PATCH_CX;
        int cy = yo + PATCH_CY;
        if (pattern == null) {
            graphics.centeredText(this.font, "Pick a pattern", cx, cy - 4, 0xFF8A9A8E);
            return;
        }
        int count = pattern.stitches.size();
        int done = Integer.bitCount(this.menu.sewn());
        // the patch, 3 times its size: faint, and filling in from the bottom as materials go on
        graphics.pose().pushMatrix();
        graphics.pose().translate(cx - 24, cy - 24);
        graphics.pose().scale(3.0F, 3.0F);
        graphics.item(new ItemStack(pattern.patch()), 0, 0);
        graphics.pose().popMatrix();
        graphics.nextStratum();
        int veil = Math.round(48 * (1 - done / (float) count));
        if (veil > 0) graphics.fill(cx - 24, cy - 24, cx + 24, cy - 24 + veil, 0xC81A2420);
        // the seams round it: an arc each
        int next = this.menu.next();
        float burst = Mth.clamp(1 - (now - this.sewnAt) / 700.0F, 0, 1);
        for (int i = 0; i < count; i++) {
            float start = -Mth.HALF_PI + i * Mth.TWO_PI / count + 0.12F;
            float end = start + Mth.TWO_PI / count - 0.24F;
            boolean sewn = this.menu.isSewn(i);
            for (int d = 0; d <= 40; d++) {
                float t = start + (end - start) * d / 40.0F;
                int px = cx + Math.round(Mth.cos(t) * RING);
                int py = cy + Math.round(Mth.sin(t) * RING);
                if (sewn) {
                    graphics.fill(px, py, px + 1, py + 1, d % 4 == 3 ? 0xFF8E6E26 : GOLD);
                    if (burst > 0 && d % 8 == 0) {
                        int r = Math.round(burst * 3);
                        graphics.fill(px - r, py, px + r + 1, py + 1, (Math.round(burst * 200) << 24) | 0xFFF0B0);
                    }
                } else if (d % 3 != 2) {
                    graphics.fill(px, py, px + 1, py + 1, i == next ? CHALK : 0x80E8E2D0);
                }
            }
            // the needle over the next seam, bobbing
            if (i == next) {
                float t = (start + end) / 2;
                int px = cx + Math.round(Mth.cos(t) * RING);
                int py = cy + Math.round(Mth.sin(t) * RING);
                int bob = (int) Math.round(Math.sin(now / 140.0) * 2);
                graphics.fill(px, py - 9 + bob, px + 1, py + 1 + bob, 0xFFF4F8FF);
                graphics.fill(px - 1, py - 9 + bob, px + 2, py - 7 + bob, 0xFF8E949C);
            }
        }
    }

    // The row of material slots: ghosts of what goes in each, a gold arrow over the next one, and gold thread over the
    // ones already sewn on; the 4th slot is stitched shut on patterns with fewer materials
    private void drawMaterials(GuiGraphicsExtractor graphics, int xo, int yo, long now, @Nullable Pattern pattern) {
        int next = this.menu.next();
        for (int i = 0; i < Pattern.MAX_STITCHES; i++) {
            int x = xo + SewingMachineMenu.MATERIAL_X + i * SewingMachineMenu.MATERIAL_STEP;
            int y = yo + SewingMachineMenu.MATERIAL_Y;
            boolean used = pattern != null && i < pattern.stitches.size();
            DesignerDraw.slot(graphics, x - 1, y - 1, 18, SLOT_EDGE, used ? SLOT_LIP : 0xFF2A322E, used ? SLOT_INSIDE : 0xFF161C19);
            if (!used) {
                // stitched shut: a little cross of thread
                DesignerDraw.thread(graphics, x + 3, y + 3, x + 12, y + 12, 0x60E8E2D0, 0);
                DesignerDraw.thread(graphics, x + 12, y + 3, x + 3, y + 12, 0x60E8E2D0, 0);
                continue;
            }
            if (this.menu.getSlot(i).getItem().isEmpty()) DesignerDraw.ghost(graphics, pattern.stitches.get(i).stack(), x, y, SLOT_INSIDE);
            if (this.menu.isSewn(i)) {
                graphics.nextStratum();
                graphics.fill(x - 1, y + 17, x + 17, y + 19, GOLD);
                graphics.text(this.font, "✔", x + 10, y - 9, 0xFF9CE88A, true);
            }
            if (i == next) {
                int bob = (int) Math.round(Math.sin(now / 180.0) * 1.5);
                int ax = x + 7;
                int ay = y - 7 + bob;
                graphics.fill(ax - 2, ay, ax + 4, ay + 1, GOLD);
                graphics.fill(ax - 1, ay + 1, ax + 3, ay + 2, GOLD);
                graphics.fill(ax, ay + 2, ax + 2, ay + 3, GOLD);
            }
            // thread joining each material to the next
            if (i + 1 < pattern.stitches.size()) {
                DesignerDraw.thread(graphics, x + 17, y + 8, x + SewingMachineMenu.MATERIAL_STEP - 1, y + 8,
                    this.menu.isSewn(i) ? GOLD : 0x80E8E2D0, (int) (now / 120));
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, 8, 7, TITLE, true);
        graphics.text(this.font, "Needle", 148, 16, LABEL, false);
        String problem = this.menu.problem();
        Pattern pattern = this.menu.pattern();
        int next = this.menu.next();
        DesignerDraw.status(graphics, this.font, this.playerInventoryTitle, problem,
            pattern != null && next >= 0 ? "Next: " + pattern.stitches.get(next).name() : "", this.imageWidth, this.inventoryLabelY, LABEL);
    }

    // ── the needle game: drawn over everything, takes every click ──

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (this.game == null) {
            super.extractRenderState(graphics, mouseX, mouseY, a);
            return;
        }
        this.extractContents(graphics, mouseX, mouseY, a);
        graphics.nextStratum();
        this.game.draw(graphics, this.font, this.width, this.height, mouseX);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.game == null) {
            // a tab
            for (Pattern.Tier tier : Pattern.Tier.values()) {
                int x = this.leftPos + TAB_X + tier.ordinal() * TAB_STEP;
                if (event.x() >= x && event.x() < x + TAB_W && event.y() >= this.topPos - TAB_H && event.y() < this.topPos) {
                    if (tier == this.tab) return true;
                    // half way through a patch the tabs are locked: unpick it first
                    if (this.menu.sewn() != 0) {
                        this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            net.minecraft.sounds.SoundEvents.VILLAGER_NO, 1.4F));
                        return true;
                    }
                    // a new tab picks its first pattern
                    this.tab = tier;
                    this.press(Pattern.inTier(tier).getFirst().ordinal());
                    this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, 1.2F));
                    return true;
                }
            }
            // a pattern tile
            if (this.menu.sewn() == 0) {
                List<Pattern> shown = Pattern.inTier(this.tab);
                for (int i = 0; i < shown.size(); i++) {
                    Pattern pattern = shown.get(i);
                    int x = this.leftPos + TILE_X;
                    int y = this.topPos + TILE_Y + i * TILE_STEP;
                    if (event.x() >= x && event.x() < x + TILE && event.y() >= y && event.y() < y + TILE) {
                        this.press(pattern.ordinal());
                        this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            net.minecraft.sounds.SoundEvents.UI_LOOM_SELECT_PATTERN, 1.0F));
                        return true;
                    }
                }
            }
            return super.mouseClicked(event, doubleClick);
        }
        this.game.click();
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

    // Esc stops the game (nothing is used); every other key is ignored while sewing
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.game == null) return super.keyPressed(event);
        if (event.isEscape()) this.game = null;
        return true;
    }

    // Hovering a pattern tile: its name and what it's made of
    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (this.game != null) return;
        // an empty slot: exactly what goes in it, by name (the ghosts' colors alone aren't enough to tell wool apart)
        int slot = this.hoveredSlot == null ? -1 : this.menu.slots.indexOf(this.hoveredSlot);
        if (slot >= 0 && slot < com.alexh.capitalism.designer.SewingMachineBlockEntity.SIZE && !this.hoveredSlot.hasItem()) {
            Pattern pattern = this.menu.pattern();
            String need = null;
            if (slot == com.alexh.capitalism.designer.SewingMachineBlockEntity.NEEDLE) {
                need = pattern == null || pattern.tier == Pattern.Tier.GENERIC ? "Needs: Iron Sewing Needles" : "Needs: Diamond Sewing Needles";
            } else if (pattern != null && slot < pattern.stitches.size()) {
                need = (this.menu.isSewn(slot) ? "Sewn on: " : "Needs: ") + pattern.stitches.get(slot).name();
            }
            if (need != null) graphics.setTooltipForNextFrame(this.font, Component.literal(need), mouseX, mouseY);
        }
        for (Pattern.Tier tier : Pattern.Tier.values()) {
            int x = this.leftPos + TAB_X + tier.ordinal() * TAB_STEP;
            if (mouseX >= x && mouseX < x + TAB_W && mouseY >= this.topPos - TAB_H && mouseY < this.topPos) {
                List<Component> lines = new java.util.ArrayList<>();
                lines.add(Component.literal(tier.title + " Patches"));
                lines.add(Component.literal(tier == Pattern.Tier.LUXURY ? "Hard. Diamond needles." : tier == Pattern.Tier.FANCY ? "Medium. Diamond needles." : "Easy. Iron needles.")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
                if (this.menu.sewn() != 0 && tier != this.tab) {
                    lines.add(Component.literal("Unpick this patch to switch tabs").withStyle(net.minecraft.ChatFormatting.RED));
                }
                graphics.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
            }
        }
        List<Pattern> shown = Pattern.inTier(this.tab);
        for (int i = 0; i < shown.size(); i++) {
            Pattern pattern = shown.get(i);
            int x = this.leftPos + TILE_X;
            int y = this.topPos + TILE_Y + i * TILE_STEP;
            if (mouseX >= x && mouseX < x + TILE && mouseY >= y && mouseY < y + TILE) {
                java.util.List<Component> lines = new java.util.ArrayList<>();
                lines.add(new ItemStack(pattern.patch()).getHoverName());
                for (Pattern.Stitch stitch : pattern.stitches) lines.add(Component.literal("- " + stitch.name()).withStyle(net.minecraft.ChatFormatting.GRAY));
                graphics.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
            }
        }
    }
}
