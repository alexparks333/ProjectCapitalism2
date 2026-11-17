package com.alexh.capitalism.realestate;

import com.alexh.capitalism.core.ModMenus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

// The City Chest's menu (client/CityChestScreen): the WANTED row on top (an op setting it up in creative fills it; for
// an agent it's only to look at, drawn ghosted), right under it the DEPOSIT row (each slot takes only the wanted thing
// above it: CityChestBlockEntity.fits), then the inventory. While an agent has it open the server checks every tick
// whether everything's in (CityChestBlockEntity.tryClaim). Data: 0 setting up (1) or paying (0).
public class CityChestMenu extends AbstractContainerMenu {

    public static final int WANTED_Y = 30;
    public static final int DEPOSIT_Y = 52;
    public static final int INV_Y = 86;
    public static final int HEIGHT = 168;
    private static final int INV_START = CityChestBlockEntity.SIZE;
    private static final int INV_END = INV_START + 36;

    private final Container container;
    private final ContainerData data;
    private final Player player;

    public CityChestMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(CityChestBlockEntity.SIZE), new SimpleContainerData(1));
    }

    public CityChestMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(ModMenus.CITY_CHEST.get(), containerId);
        checkContainerSize(container, CityChestBlockEntity.SIZE);
        this.container = container;
        this.data = data;
        this.player = inventory.player;
        for (int i = 0; i < CityChestBlockEntity.WANTED; i++) {
            this.addSlot(new Slot(container, i, 8 + i * 18, WANTED_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return CityChestMenu.this.setup();
                }

                @Override
                public boolean mayPickup(Player player) {
                    return CityChestMenu.this.setup();
                }
            });
        }
        for (int i = 0; i < CityChestBlockEntity.DEPOSIT; i++) {
            int wantedSlot = i;
            this.addSlot(new Slot(container, CityChestBlockEntity.WANTED + i, 8 + i * 18, DEPOSIT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return CityChestBlockEntity.fits(container.getItem(wantedSlot), stack);
                }
            });
        }
        this.addStandardInventorySlots(inventory, 8, INV_Y);
        this.addDataSlots(data);
    }

    // Double-click gathering never pulls from the wanted row (only someone setting it up can touch it)
    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot slot) {
        return (slot.index >= CityChestBlockEntity.WANTED || this.setup()) && super.canTakeItemForPickAll(carried, slot);
    }

    public boolean setup() {
        return this.data.get(0) == 1;
    }

    // An agent paying: is everything in now?
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (this.setup() || !(this.player instanceof ServerPlayer agent) || !(this.container instanceof CityChestBlockEntity chest)) return;
        if (chest.tryClaim(agent)) agent.closeContainer();
    }

    // Shift-click: out into the inventory; from the inventory into the slot that wants it (setting up: the wanted row)
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack clicked = stack.copy();
        if (slotIndex < INV_START) {
            if (slotIndex < CityChestBlockEntity.WANTED && !this.setup()) return ItemStack.EMPTY;
            if (!this.moveItemStackTo(stack, INV_START, INV_END, true)) return ItemStack.EMPTY;
        } else {
            int from = this.setup() ? 0 : CityChestBlockEntity.WANTED;
            int to = this.setup() ? CityChestBlockEntity.WANTED : CityChestBlockEntity.SIZE;
            if (!this.moveItemStackTo(stack, from, to, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == clicked.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return clicked;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }
}
