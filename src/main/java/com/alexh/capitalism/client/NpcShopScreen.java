package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.economy.GoldMoney;
import com.alexh.capitalism.economy.NpcPricing;
import com.alexh.capitalism.npc.NpcRepairs;
import com.alexh.capitalism.npc.NpcShopMenu;
import com.alexh.capitalism.npc.NpcShops;
import com.alexh.capitalism.npc.NpcStock;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

// A job NPC's shop, as a market stall (textures/gui/container/npc_shop.png, drawn by tools/make_npc_shop_gui.ps1): a tab per
// section (e.g. Gourmet Meals / Gourmet Bites), up to 7 offers each with its icon, name, NPC price, how many are left
// and a Buy button (shift-click buys up to NpcShopMenu.BULK), and the restock timer (plus any Emerald discount) at the
// bottom. Buying is a menu button; the server
// does the paying (NpcShopMenu.clickMenuButton).
public class NpcShopScreen extends AbstractContainerScreen<NpcShopMenu> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/gui/container/npc_shop.png");

    private static final int ROWS = 7;
    private static final int ROW_Y = 52;
    private static final int ROW_STEP = 20;
    private static final int TITLE = 0xFFF5DFA8;
    private static final int NAME = 0xFFFFF4DE;
    private static final int PRICE = 0xFFFFD34A;
    private static final int TOO_DEAR = 0xFFFF6A5A;
    private static final int FOOTER = 0xFFE8D3A8;
    private static final int STOCK = 0xFFB8E0A0;

    private int section;
    private int scroll;
    private final List<Button> tabButtons = new ArrayList<>();
    private final List<Button> buyButtons = new ArrayList<>();

    public NpcShopScreen(NpcShopMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 200, 222);
    }

    @Override
    protected void init() {
        super.init();
        this.tabButtons.clear();
        this.buyButtons.clear();
        List<NpcShops.Section> sections = NpcShops.sections(this.menu.shop());
        int tabWidth = sections.isEmpty() ? 0 : (180 - 4 * (sections.size() - 1)) / sections.size();
        for (int i = 0; i < sections.size(); i++) {
            int tab = i;
            this.tabButtons.add(this.addRenderableWidget(Button.builder(Component.literal(sections.get(i).title()), b -> { this.section = tab; this.scroll = 0; })
                .bounds(this.leftPos + 10 + i * (tabWidth + 4), this.topPos + 35, tabWidth, 14).build()));
        }
        for (int row = 0; row < ROWS; row++) {
            int r = row;
            this.buyButtons.add(this.addRenderableWidget(Button.builder(Component.literal("Buy"), b -> this.buy(r))
                .bounds(this.leftPos + 154, this.topPos + ROW_Y + row * ROW_STEP + 1, 34, 16).build()));
        }
        this.refresh();
    }

    // The offers on the open tab
    private List<NpcShops.Offer> shown() {
        List<NpcShops.Section> sections = NpcShops.sections(this.menu.shop());
        return sections.isEmpty() ? List.of() : sections.get(Math.min(this.section, sections.size() - 1)).offers();
    }

    // Its index in the whole shop (what the server's buy button ids count in)
    private int indexOf(int row) {
        List<NpcShops.Section> sections = NpcShops.sections(this.menu.shop());
        int index = 0;
        for (int i = 0; i < this.section; i++) index += sections.get(i).offers().size();
        return index + row;
    }

    private boolean repairTab() {
        List<NpcShops.Section> sections = NpcShops.sections(this.menu.shop());
        return !sections.isEmpty() && sections.get(Math.min(this.section, sections.size() - 1)).repairs();
    }

    // Repair tab: the inventory slots of what you carry that this NPC can fix (NpcRepairs)
    private List<Integer> repairSlots() {
        Player player = this.minecraft.player;
        return player == null || this.menu.job() == null ? List.of() : NpcRepairs.slots(player, this.menu.job());
    }

    private void buy(int row) {
        if (this.minecraft.gameMode == null) return;
        if (this.repairTab()) {
            List<Integer> slots = this.repairSlots();
            if (this.scroll + row < slots.size()) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId,
                    NpcShopMenu.REPAIR_BUTTON + slots.get(this.scroll + row));
            }
            return;
        }
        if (row >= this.shown().size()) return;
        boolean bulk = this.minecraft.hasShiftDown();
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, this.indexOf(row) * 2 + (bulk ? 1 : 0));
    }

    private long price(NpcShops.Offer offer) {
        Player player = this.minecraft.player;
        return player == null ? 0 : NpcPricing.npcPrice(offer.fairNuggets(), player);
    }

    private void refresh() {
        List<NpcShops.Offer> offers = this.shown();
        Player player = this.minecraft.player;
        long gold = player == null ? 0 : GoldMoney.carried(player);
        for (int i = 0; i < this.tabButtons.size(); i++) this.tabButtons.get(i).active = i != this.section;
        if (this.repairTab()) {
            List<Integer> slots = this.repairSlots();
            this.scroll = Math.max(0, Math.min(this.scroll, slots.size() - ROWS));
            for (int row = 0; row < ROWS; row++) {
                Button button = this.buyButtons.get(row);
                button.setMessage(Component.literal("Fix"));
                button.visible = this.scroll + row < slots.size();
                if (!button.visible || player == null) continue;
                ItemStack stack = player.getInventory().getItem(slots.get(this.scroll + row));
                button.active = gold >= NpcPricing.npcPrice(NpcRepairs.fairNuggets(stack), player);
                button.setTooltip(Tooltip.create(Component.literal("+" + NpcRepairs.amount(stack)
                    + " durability (doesn't sharpen)")));
            }
            return;
        }
        for (int row = 0; row < ROWS; row++) {
            this.buyButtons.get(row).setMessage(Component.literal("Buy"));
            Button button = this.buyButtons.get(row);
            button.visible = row < offers.size();
            if (!button.visible) continue;
            long price = this.price(offers.get(row));
            int left = this.menu.left(this.indexOf(row));
            button.active = left > 0 && gold >= price;
            int bulk = Math.min(NpcShopMenu.BULK, left);
            boolean forever = offers.get(row).forever();
            button.setTooltip(left <= 0 ? Tooltip.create(Component.literal(forever ? "Sold out for good: it never restocks" : "Sold out until the restock"))
                : forever ? Tooltip.create(Component.literal("Only " + offers.get(row).maxStock() + " ever, never restocks. " + left + " left."))
                : Tooltip.create(Component.literal("Shift-click: buy " + bulk + " for " + GoldMoney.format(price * bulk))));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.refresh();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int xo = this.leftPos;
        int yo = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        Player player = this.minecraft.player;
        long gold = player == null ? 0 : GoldMoney.carried(player);
        if (this.repairTab()) {
            this.extractRepairs(graphics, mouseX, mouseY, gold);
        }
        List<NpcShops.Offer> offers = this.shown();
        for (int row = 0; row < Math.min(ROWS, offers.size()); row++) {
            NpcShops.Offer offer = offers.get(row);
            int y = yo + ROW_Y + row * ROW_STEP;
            var stack = offer.stack();
            graphics.item(stack, xo + 11, y + 1);
            graphics.itemDecorations(this.font, stack, xo + 11, y + 1);
            // name on top; price and how many are left under it
            String name = this.font.plainSubstrByWidth(stack.getHoverName().getString(), 118);
            graphics.text(this.font, name, xo + 32, y + 1, NAME, true);
            long price = this.price(offer);
            int left = this.menu.left(this.indexOf(row));
            String text = GoldMoney.format(price);
            graphics.text(this.font, text, xo + 32, y + 10, gold >= price ? PRICE : TOO_DEAR, true);
            if (left < NpcStock.UNLIMITED) {
                String stockText = left <= 0 ? "SOLD OUT" : left + " left";
                graphics.text(this.font, stockText, xo + 36 + this.font.width(text), y + 10, left <= 0 ? TOO_DEAR : STOCK, true);
            }
            // the food's tooltip when hovering its icon
            if (mouseX >= xo + 10 && mouseX < xo + 28 && mouseY >= y && mouseY < y + 18) {
                graphics.setTooltipForNextFrame(this.font, stack, mouseX, mouseY);
            }
        }
        // footer: when the stock refills (and an Emerald discount, only if you have one)
        double discount = player == null ? 0 : NpcPricing.discount(player);
        String restock = "Restocks in " + NpcShopMenu.clock(this.menu.secondsUntilRestock());
        graphics.text(this.font, restock, xo + (this.imageWidth - this.font.width(restock)) / 2, yo + (discount > 0 ? 198 : 203), FOOTER, true);
        if (discount > 0) {
            String note = "Emerald discount: " + Math.round(discount * 1000) / 10.0 + "% off";
            graphics.text(this.font, note, xo + (this.imageWidth - this.font.width(note)) / 2, yo + 209, 0xFF7CE08A, true);
        }
    }

    // Repair tab rows: what you carry that this NPC fixes, with its durability, the repair's price and how much it gives back
    private void extractRepairs(GuiGraphicsExtractor graphics, int mouseX, int mouseY, long gold) {
        Player player = this.minecraft.player;
        if (player == null) return;
        int xo = this.leftPos;
        int yo = this.topPos;
        List<Integer> slots = this.repairSlots();
        if (slots.isEmpty()) {
            String none = "Nothing you carry needs repairing.";
            graphics.text(this.font, none, xo + (this.imageWidth - this.font.width(none)) / 2, yo + ROW_Y + 5, NAME, true);
            return;
        }
        for (int row = 0; row < ROWS && this.scroll + row < slots.size(); row++) {
            ItemStack stack = player.getInventory().getItem(slots.get(this.scroll + row));
            int y = yo + ROW_Y + row * ROW_STEP;
            graphics.item(stack, xo + 11, y + 1);
            graphics.itemDecorations(this.font, stack, xo + 11, y + 1);
            String name = this.font.plainSubstrByWidth(stack.getHoverName().getString(), 118);
            graphics.text(this.font, name, xo + 32, y + 1, NAME, true);
            long price = NpcPricing.npcPrice(NpcRepairs.fairNuggets(stack), player);
            String text = GoldMoney.format(price);
            graphics.text(this.font, text, xo + 32, y + 10, gold >= price ? PRICE : TOO_DEAR, true);
            String fix = "+" + NpcRepairs.amount(stack) + " (" + (stack.getMaxDamage() - stack.getDamageValue()) + "/"
                + stack.getMaxDamage() + ")";
            graphics.text(this.font, fix, xo + 36 + this.font.width(text), y + 10, STOCK, true);
            if (mouseX >= xo + 10 && mouseX < xo + 28 && mouseY >= y && mouseY < y + 18) {
                graphics.setTooltipForNextFrame(this.font, stack, mouseX, mouseY);
            }
        }
    }

    // Scroll the Repair tab when you carry more than ROWS things to fix
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.repairTab() && scrollY != 0) {
            this.scroll -= (int) Math.signum(scrollY);
            this.refresh();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, (this.imageWidth - this.font.width(this.title)) / 2, 22, TITLE, true);
    }
}
