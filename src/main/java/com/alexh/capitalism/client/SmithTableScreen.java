package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.weaponsmith.SmithTableBlockEntity;
import com.alexh.capitalism.weaponsmith.DentForging;
import com.alexh.capitalism.weaponsmith.Salvaging;
import com.alexh.capitalism.weaponsmith.Socketing;
import com.alexh.capitalism.weaponsmith.SmithTableMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.List;

// A smith table (Weaponsmith / Toolsmith Table): a dark iron plate with rivets, a big slot and a small one joined by "+",
// a forged arrow and the result slot, with a fuel slot and a lava gauge (the glowing strip) under it. The arrow glows
// orange when the items make something; "Needs Lava" shows when they would, but the tank is too low. Hover the gauge
// to see how much lava is left. A damaged weapon + its material shows the Forge button: it starts the dent game
// (DentGame), drawn over the whole window; when it ends, the hits go to the server as a menu button.
public class SmithTableScreen extends AbstractContainerScreen<SmithTableMenu> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/gui/container/weaponsmith_table.png");

    private static final int TITLE_COLOR = 0xFFF2D9B8;
    private static final int LABEL_COLOR = 0xFFD8C0A0;
    // the arrow (in the texture at 102,36, 24x16)
    private static final int ARROW_X = 102;
    private static final int ARROW_Y = 36;
    private static final int EMBER = 0xFFFF8A2A;
    private static final int EMBER_LIGHT = 0xFFFFC870;
    // the lava gauge: the glowing strip in the texture (100,70 to 167,75), drained from the right
    private static final int GAUGE_X = 100;
    private static final int GAUGE_Y = 70;
    private static final int GAUGE_W = 68;
    private static final int GAUGE_H = 6;
    private static final int GAUGE_EMPTY = 0xFF140D0F;
    private static final int NEEDS_LAVA = 0xFFFF5A3A;
    // the faded emblem behind the forge: which table this is (48x48, centered, on a little stand)
    private static final int EMBLEM_SCALE = 3;
    private static final int EMBLEM_X = 64;
    private static final int EMBLEM_Y = 16;
    private static final int EMBLEM = 0x16E0D8D8;
    private static final int EMBLEM_STAND = 0x10E0D8D8;

    // the Forge button, right under the result box (129,29 to 155,55), and its XP cost to its left like an anvil's
    private static final int FORGE_X = 121;
    private static final int FORGE_Y = 57;
    private static final int FORGE_W = 42;
    private static final int FORGE_H = 12;
    private static final int XP_OK = 0xFF80FF20;
    private static final int XP_SHORT = 0xFFFF6060;
    private @Nullable Button forgeButton;
    private @Nullable Button salvageButton;
    private @Nullable DentGame game;

    public SmithTableScreen(SmithTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.forgeButton = this.addRenderableWidget(Button.builder(Component.literal("Forge"), b -> this.startForging())
            .bounds(this.leftPos + FORGE_X, this.topPos + FORGE_Y, FORGE_W, FORGE_H).build());
        // the Salvage button sits in the same spot (gear + Salvage Hammer; the two never show at once)
        this.salvageButton = this.addRenderableWidget(Button.builder(Component.literal("Salvage"), b -> this.press(SmithTableMenu.BUTTON_SALVAGE))
            .bounds(this.leftPos + FORGE_X, this.topPos + FORGE_Y, FORGE_W, FORGE_H).build());
        this.updateForgeButton();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int xo = this.leftPos;
        int yo = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        this.drawEmblem(graphics, xo, yo);
        // lava gauge
        int lava = this.menu.lava();
        int filled = Math.round((float) GAUGE_W * lava / SmithTableBlockEntity.CAPACITY);
        if (filled < GAUGE_W) {
            graphics.fill(xo + GAUGE_X + filled, yo + GAUGE_Y, xo + GAUGE_X + GAUGE_W, yo + GAUGE_Y + GAUGE_H, GAUGE_EMPTY);
        }
        // embers drifting up off the lava (only where there is lava), fading as they rise
        if (filled > 0) {
            long now = Util.getMillis();
            for (int i = 0; i < 7; i++) {
                long period = 1600 + i * 330L;
                float rise = ((now + i * 977L) % period) / (float) period;
                int ex = xo + GAUGE_X + (i * 29 + 7) % filled;
                int ey = yo + GAUGE_Y - 1 - Math.round(rise * 24);
                int alpha = Math.round(230 * (1 - rise));
                graphics.fill(ex, ey, ex + 1, ey + 1, (alpha << 24) | (rise < 0.4F ? 0xFFC060 : 0xFF7A28));
            }
        }
        if (mouseX >= xo + GAUGE_X && mouseX < xo + GAUGE_X + GAUGE_W && mouseY >= yo + GAUGE_Y && mouseY < yo + GAUGE_Y + GAUGE_H) {
            graphics.setComponentTooltipForNextFrame(this.font, List.of(
                Component.literal("Lava: " + buckets(lava) + " / " + buckets(SmithTableBlockEntity.CAPACITY) + " buckets"),
                Component.literal("Lava Bucket = 1, Refined Bucket of Lava = 2").withStyle(ChatFormatting.GRAY)), mouseX, mouseY, ItemStack.EMPTY);
        }
        // empty fuel slot: say what goes there
        Slot fuel = this.menu.getSlot(SmithTableMenu.FUEL);
        if (!fuel.hasItem() && this.carried().isEmpty() && mouseX >= xo + fuel.x - 1 && mouseX < xo + fuel.x + 17
            && mouseY >= yo + fuel.y - 1 && mouseY < yo + fuel.y + 17) {
            graphics.setComponentTooltipForNextFrame(this.font, List.of(Component.literal("Fuel: Lava Bucket or Refined Bucket of Lava")), mouseX, mouseY, ItemStack.EMPTY);
        }
        // the repair's XP cost, next to the Forge button (green, or red if you don't have the levels)
        DentForging.State forge = this.menu.forge();
        if (this.game == null && forge != DentForging.State.NONE) {
            String cost = this.xpCost() + " XP";
            graphics.text(this.font, cost, xo + FORGE_X - 3 - this.font.width(cost), yo + FORGE_Y + 2,
                forge == DentForging.State.NEEDS_XP ? XP_SHORT : XP_OK, true);
        } else if (this.game == null && this.menu.socket() == SmithTableMenu.GEM_BREAK) {
            // breaking a gem out: free, but the gem is lost
            String text = "Breaks the gem";
            graphics.text(this.font, text, xo + 142 - this.font.width(text) / 2, yo + FORGE_Y + 2, XP_SHORT, true);
        } else if (this.game == null && this.menu.socket() != SmithTableMenu.SOCKET_NONE) {
            // setting a Molten Socket or a gem: its XP cost, under the result box
            boolean gem = this.menu.socket() == SmithTableMenu.GEM_READY || this.menu.socket() == SmithTableMenu.GEM_NEEDS_XP;
            String cost = (gem ? Socketing.GEM_XP : Socketing.SOCKET_XP) + " XP";
            boolean short_ = this.menu.socket() == SmithTableMenu.SOCKET_NEEDS_XP || this.menu.socket() == SmithTableMenu.GEM_NEEDS_XP;
            graphics.text(this.font, cost, xo + 142 - this.font.width(cost) / 2, yo + FORGE_Y + 2, short_ ? XP_SHORT : XP_OK, true);
        }
        if (this.menu.needsLava()) {
            String text = "Needs Lava";
            graphics.text(this.font, text, xo + ARROW_X + 12 - this.font.width(text) / 2, yo + 58, NEEDS_LAVA, false);
        }
        // glowing arrow when there's a result
        if (this.menu.getSlot(SmithTableMenu.RESULT).hasItem()) {
            for (int r = 0; r < 16; r++) {
                int end = 23 - (int) Math.ceil(Math.abs(r - 7.5));
                int start = r >= 5 && r <= 10 ? 0 : 15;
                if (end > start) {
                    graphics.fill(xo + ARROW_X + start + 1, yo + ARROW_Y + r, xo + ARROW_X + end, yo + ARROW_Y + r + 1, r == 6 ? EMBER_LIGHT : EMBER);
                }
            }
        }
    }

    private void startForging() {
        if (this.menu.forge() != DentForging.State.READY) return;
        this.game = new DentGame(this.menu.getSlot(SmithTableMenu.BIG).getItem());
    }

    private void press(int buttonId) {
        if (this.minecraft.gameMode != null) this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
    }

    private void updateSalvageButton() {
        if (this.salvageButton == null) return;
        Salvaging.State state = this.menu.salvage();
        this.salvageButton.visible = this.game == null && state != Salvaging.State.NONE;
        this.salvageButton.active = state == Salvaging.State.READY;
        String back = Salvaging.describe(this.menu.getSlot(SmithTableMenu.BIG).getItem());
        this.salvageButton.setTooltip(switch (state) {
            case READY -> Tooltip.create(Component.literal("Breaks it down into " + back + ". Uses 1 hammer use and a little lava."));
            case NEEDS_LAVA -> Tooltip.create(Component.literal("Needs more lava in the tank."));
            case NONE -> null;
        });
    }

    // XP levels this repair costs (by the material in the small slot)
    private int xpCost() {
        return DentForging.xpCost(this.menu.getSlot(SmithTableMenu.SMALL).getItem());
    }

    private static String levels(int count) {
        return count == 1 ? "level" : "levels";
    }

    private void updateForgeButton() {
        if (this.forgeButton == null) return;
        this.updateSalvageButton();
        DentForging.State state = this.menu.forge();
        this.forgeButton.visible = this.game == null && state != DentForging.State.NONE;
        this.forgeButton.active = state == DentForging.State.READY;
        this.forgeButton.setTooltip(switch (state) {
            case READY -> Tooltip.create(Component.literal("Hammer out the dents to repair it. Uses 1 material, some lava and "
                + this.xpCost() + " XP " + levels(this.xpCost()) + "."));
            case NEEDS_LAVA -> Tooltip.create(Component.literal("Needs more lava in the tank."));
            case NEEDS_XP -> Tooltip.create(Component.literal("Needs " + this.xpCost() + " XP " + levels(this.xpCost()) + "."));
            case NONE -> null;
        });
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.game != null && this.game.isOver()) {
            if (this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, SmithTableMenu.BUTTON_FORGE + this.game.hits());
            }
            this.game = null;
        }
        this.updateForgeButton();
    }

    // While forging, the game is drawn over everything and takes every click
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (this.game == null) {
            super.extractRenderState(graphics, mouseX, mouseY, a);
            return;
        }
        this.extractContents(graphics, mouseX, mouseY, a);
        graphics.nextStratum();
        this.game.draw(graphics, this.font, this.width, this.height);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.game == null) return super.mouseClicked(event, doubleClick);
        this.game.click(event.x(), event.y(), this.width, this.height);
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

    // Esc stops the game (nothing is used); every other key is ignored while forging
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.game == null) return super.keyPressed(event);
        if (event.isEscape()) this.game = null;
        return true;
    }

    // Which table this is: a big, very faded grey sword / pickaxe / chestplate on a little stand behind the forge
    private void drawEmblem(GuiGraphicsExtractor graphics, int xo, int yo) {
        Item emblem = switch (this.menu.kind()) {
            case WEAPONSMITH -> Items.IRON_SWORD;
            case TOOLSMITH -> Items.IRON_PICKAXE;
            case ARMORSMITH -> Items.IRON_CHESTPLATE;
        };
        // in texture coordinates; drawn pixel by pixel so it can skip everything that sits in front of it
        for (int[] pixel : ItemSilhouette.pixels(emblem)) {
            this.emblemArea(graphics, xo, yo, EMBLEM_X + pixel[0] * EMBLEM_SCALE, EMBLEM_Y + pixel[1] * EMBLEM_SCALE,
                EMBLEM_SCALE, EMBLEM_SCALE, EMBLEM);
        }
        // the stand: a top plate, a short neck and a wide foot
        int mid = EMBLEM_X + 8 * EMBLEM_SCALE;
        int base = EMBLEM_Y + 16 * EMBLEM_SCALE + 1;
        this.emblemArea(graphics, xo, yo, mid - 16, base, 32, 2, EMBLEM_STAND);
        this.emblemArea(graphics, xo, yo, mid - 4, base + 2, 8, 5, EMBLEM_STAND);
        this.emblemArea(graphics, xo, yo, mid - 18, base + 7, 36, 2, EMBLEM_STAND);
    }

    // Fills a w x h area of the emblem, leaving out the pixels covered by slots, the arrow, the gauge... so it looks
    // like it's behind them
    private void emblemArea(GuiGraphicsExtractor graphics, int xo, int yo, int x, int y, int w, int h, int color) {
        for (int dy = 0; dy < h; dy++) {
            for (int dx = 0; dx < w; dx++) {
                if (inFront(x + dx, y + dy)) continue;
                graphics.fill(xo + x + dx, yo + y + dy, xo + x + dx + 1, yo + y + dy + 1, color);
            }
        }
    }

    // The parts of the texture in front of the emblem: {x0, y0, x1, y1}, inclusive
    private static final int[][] IN_FRONT = {
        {13, 28, 41, 56},    // big slot (with its glowing corners)
        {60, 34, 77, 51},    // small slot
        {45, 39, 53, 47},    // the "+"
        {102, 36, 125, 51},  // the arrow
        {128, 28, 156, 56},  // result slot
        {79, 57, 96, 74},    // fuel slot
        {70, 61, 75, 70},    // the flame
        {99, 69, 168, 76},   // the lava gauge
    };

    private static boolean inFront(int x, int y) {
        for (int[] r : IN_FRONT) {
            if (x >= r[0] && x <= r[2] && y >= r[1] && y <= r[3]) return true;
        }
        return false;
    }

    // 2500 -> "2.5"
    private static String buckets(int millibuckets) {
        float buckets = millibuckets / (float) SmithTableBlockEntity.BUCKET;
        return buckets == Math.floor(buckets) ? String.valueOf((int) buckets) : String.format(java.util.Locale.ROOT, "%.2f", buckets).replaceAll("0+$", "");
    }

    private ItemStack carried() {
        return this.menu.getCarried();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, TITLE_COLOR, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, LABEL_COLOR, false);
    }
}
