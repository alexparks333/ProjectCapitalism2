package com.alexh.capitalism.town;

import com.alexh.capitalism.core.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;

// The Mayoral Chest open (client/MayoralChestScreen): a six-row chest menu of its own type, so it gets the town's
// styling.
public class MayoralChestMenu extends ChestMenu {

    public MayoralChestMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(MayoralChestBlockEntity.SIZE));
    }

    public MayoralChestMenu(int id, Inventory inventory, Container chest) {
        super(ModMenus.MAYORAL_CHEST.get(), id, inventory, chest, 6);
    }
}
