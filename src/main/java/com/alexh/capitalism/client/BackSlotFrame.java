package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.back.BackSlot;
import com.alexh.capitalism.mixin.AbstractContainerScreenAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.List;

// The Back slot (back/BackSlot) in the inventories: the vanilla pictures have no slot there, so its sunken frame is
// drawn on them (mixin/InventoryScreenMixin, mixin/CreativeBackSlotMixin), and hovering it empty says what it's for.
@EventBusSubscriber(modid = Capitalism.MODID, value = Dist.CLIENT)
public final class BackSlotFrame {

    private BackSlotFrame() {
    }

    // A vanilla slot frame: its top-left corner (one pixel up and left of the slot's item)
    public static void draw(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + 18, y + 18, 0xFFFFFFFF);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
    }

    @SubscribeEvent
    public static void onForeground(ScreenEvent.Render.Foreground event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        Slot hovered = ((AbstractContainerScreenAccessor) screen).capitalism$hoveredSlot();
        // an empty bag slot: which bag, and what goes in it
        if (hovered instanceof com.alexh.capitalism.designer.BagSlot && !hovered.hasItem() && screen.getMinecraft().player != null) {
            com.alexh.capitalism.designer.Bag bag = com.alexh.capitalism.designer.Bags.worn(screen.getMinecraft().player);
            com.alexh.capitalism.designer.Bag.Holds holds = bag == null ? null : bag.holds(((com.alexh.capitalism.designer.BagSlot) hovered).index);
            if (holds == null) return;
            event.getGuiGraphics().setTooltipForNextFrame(screen.getMinecraft().font, List.of(
                Component.literal(bag.title).withStyle(ChatFormatting.GOLD).getVisualOrderText(),
                Component.literal(holds.title).withStyle(ChatFormatting.GRAY).getVisualOrderText()),
                event.getMouseX(), event.getMouseY());
            return;
        }
        if (!(hovered instanceof BackSlot) || hovered.hasItem()) return;
        event.getGuiGraphics().setTooltipForNextFrame(screen.getMinecraft().font, List.of(
            Component.literal("Back").withStyle(ChatFormatting.GOLD).getVisualOrderText(),
            Component.literal("Wear a bag or an Animal Bjorn here").withStyle(ChatFormatting.GRAY).getVisualOrderText(),
            Component.literal("(take a bag off: what's in it spills)").withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText()),
            event.getMouseX(), event.getMouseY());
    }
}
