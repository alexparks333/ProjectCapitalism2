package com.alexh.capitalism.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// Where a container screen's window is, and the slot under the mouse (for the Back slot's frame and tooltip,
// client/BackSlotFrame).
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {

    @Accessor("leftPos")
    int capitalism$leftPos();

    @Accessor("topPos")
    int capitalism$topPos();

    @Accessor("hoveredSlot")
    @Nullable Slot capitalism$hoveredSlot();
}
