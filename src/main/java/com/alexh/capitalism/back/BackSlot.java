package com.alexh.capitalism.back;

import com.alexh.capitalism.Capitalism;
import net.minecraft.resources.Identifier;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

// The Back slot in the inventory (Back), next to the offhand slot: one thing from capitalism:back_items. Reads and
// writes the player's ModAttachments.BACK (a one-slot stand-in container, since the attachment holds the real stack).
public class BackSlot extends Slot {

    public static final int X = 77;
    public static final int Y = 44;
    // the empty slot's faint backpack outline (textures/gui/sprites/container/slot/back.png)
    public static final Identifier EMPTY_ICON = Identifier.fromNamespaceAndPath(Capitalism.MODID, "container/slot/back");

    private final Player player;

    public BackSlot(Player player) {
        super(new SimpleContainer(1), 0, X, Y);
        this.player = player;
    }

    @Override
    public ItemStack getItem() {
        return Back.get(this.player);
    }

    @Override
    public boolean hasItem() {
        return !this.getItem().isEmpty();
    }

    @Override
    public void set(ItemStack stack) {
        Back.set(this.player, stack);
        this.setChanged();
    }

    @Override
    public void setByPlayer(ItemStack stack, ItemStack previous) {
        this.set(stack);
    }

    @Override
    public ItemStack remove(int amount) {
        ItemStack worn = this.getItem();
        if (worn.isEmpty() || amount <= 0) return ItemStack.EMPTY;
        // a bag comes off empty: what was in it spills on the ground (designer/Bags)
        com.alexh.capitalism.designer.Bags.spill(this.player, worn);
        ItemStack taken = worn.copy();
        Back.set(this.player, ItemStack.EMPTY);
        return taken;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return Back.fits(stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }

    @Override
    public @Nullable Identifier getNoItemIcon() {
        return EMPTY_ICON;
    }
}
