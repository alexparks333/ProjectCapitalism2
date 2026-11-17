package com.alexh.capitalism.designer;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

// One of the bag slots in the inventory (Bags; added after the Back slot by mixin/InventoryMenuMixin): the panel to the
// right of the inventory (client/BagPanel), 2 columns. Only the first `slots` of them are there, for the bag on your
// back, and they only take what that bag holds (Bag.fits).
public class BagSlot extends Slot {

    public static final int COLUMNS = 2;
    // the panel's top-left corner, from the inventory's (it sits just right of the inventory picture)
    public static final int PANEL_X = 180;
    public static final int PANEL_Y = 4;
    public static final int PANEL_WIDTH = 68;
    public static final int FIRST_X = PANEL_X + (PANEL_WIDTH - COLUMNS * 18) / 2 + 1;
    public static final int FIRST_Y = PANEL_Y + 18;

    private final Player player;
    private final Bags.Holder holder;
    public final int index;

    public BagSlot(Player player, Bags.Holder holder, int index) {
        super(new net.minecraft.world.SimpleContainer(Bag.MAX_SLOTS), index, FIRST_X + (index % COLUMNS) * 18, FIRST_Y + (index / COLUMNS) * 18);
        this.player = player;
        this.holder = holder;
        this.index = index;
    }

    // the live container follows whatever bag is on the player's back right now
    @Override
    public ItemStack getItem() {
        return this.holder.container().getItem(this.index);
    }

    @Override
    public void set(ItemStack stack) {
        this.holder.container().setItem(this.index, stack);
        this.setChanged();
    }

    @Override
    public void setChanged() {
        this.holder.container().setChanged();
    }

    @Override
    public ItemStack remove(int amount) {
        return this.holder.container().removeItem(this.index, amount);
    }

    @Override
    public boolean hasItem() {
        return !this.getItem().isEmpty();
    }

    @Override
    public boolean isActive() {
        Bag bag = Bags.worn(this.player);
        return bag != null && this.index < bag.slots(com.alexh.capitalism.back.Back.peek(this.player));
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        Bag bag = Bags.worn(this.player);
        return bag != null && this.index < bag.slots(com.alexh.capitalism.back.Back.peek(this.player)) && bag.fits(this.index, stack);
    }

    @Override
    public boolean mayPickup(Player player) {
        return this.isActive();
    }
}
