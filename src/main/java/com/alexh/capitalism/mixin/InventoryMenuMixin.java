package com.alexh.capitalism.mixin;

import com.alexh.capitalism.back.BackSlot;
import com.alexh.capitalism.designer.Bag;
import com.alexh.capitalism.designer.BagSlot;
import com.alexh.capitalism.designer.Bags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// The Back slot (back/Back): one more slot in the player's inventory, after all of vanilla's (index 46), next to the
// offhand slot; then the bag slots (designer/BagSlot, 47 on: what the Designer's bag on your back holds). Both sides add
// them, so they agree on the slots. (The creative inventory moves the Back slot and hides the bag slots,
// CreativeBackSlotMixin.) Shift-clicking a bag off your back spills what's in it first (designer/Bags).
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin extends AbstractCraftingMenu {

    @Unique
    private static final int capitalism$BACK = 46;

    private InventoryMenuMixin(MenuType<?> type, int containerId, int width, int height) {
        super(type, containerId, width, height);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void capitalism$backSlot(Inventory inventory, boolean active, Player owner, CallbackInfo ci) {
        this.addSlot(new BackSlot(owner));
        Bags.Holder holder = new Bags.Holder(owner);
        for (int i = 0; i < Bag.MAX_SLOTS; i++) this.addSlot(new BagSlot(owner, holder, i));
    }

    // Shift-click: a bag off your back spills first; from the main inventory or hotbar, what the worn bag holds goes into
    // the bag (the rest as usual)
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void capitalism$bagShiftClick(Player player, int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
        if (slotIndex == capitalism$BACK) {
            Bags.spill(player, this.slots.get(slotIndex).getItem());
            return;
        }
        Bag bag = Bags.worn(player);
        if (bag == null || slotIndex < 9 || slotIndex >= 45) return;
        net.minecraft.world.inventory.Slot slot = this.slots.get(slotIndex);
        ItemStack stack = slot.getItem();
        if (!bag.fitsAnywhere(stack)) return;
        ItemStack clicked = stack.copy();
        if (!this.moveItemStackTo(stack, capitalism$BACK + 1, capitalism$BACK + 1 + bag.slots, false)) return;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY, clicked);
        else slot.setChanged();
        slot.onTake(player, stack);
        cir.setReturnValue(stack.isEmpty() ? clicked : ItemStack.EMPTY);
    }
}
