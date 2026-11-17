package com.alexh.capitalism.pets;

import com.alexh.capitalism.core.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

// A Water Bowl's window (client/WaterBowlScreen): how much water is in it, in mL. No slots: water goes in with a bucket
// or bottle on the bowl itself.
public class WaterBowlMenu extends AbstractContainerMenu {

    private final ContainerData data;
    private final @Nullable BlockPos pos;

    // Client side: the numbers come from the server
    public WaterBowlMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainerData(2), null);
    }

    public WaterBowlMenu(int containerId, Inventory inventory, ContainerData data, @Nullable BlockPos pos) {
        super(ModMenus.WATER_BOWL.get(), containerId);
        checkContainerDataCount(data, 2);
        this.data = data;
        this.pos = pos;
        this.addDataSlots(data);
    }

    public int water() {
        return this.data.get(0);
    }

    public int capacity() {
        return Math.max(1, this.data.get(1));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.pos == null || (player.level().getBlockEntity(this.pos) instanceof WaterBowlBlockEntity
            && player.distanceToSqr(this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5) < 64);
    }
}
