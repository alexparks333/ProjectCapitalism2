package com.alexh.capitalism.pets;

import com.alexh.capitalism.core.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// The water in a Water Bowl (WaterBowlBlock), in mL, up to its size's FeederSize.waterMl. Pets drink what they need
// (take); the block's LEVEL shows how full it is (0 empty, 1-3 a third, two thirds, full). Right-clicking it without
// water opens WaterBowlMenu, which shows the mL (data 0 = water, 1 = how much it holds).
public class WaterBowlBlockEntity extends BlockEntity implements MenuProvider {

    private int water;
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? WaterBowlBlockEntity.this.water : WaterBowlBlockEntity.this.capacity();
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public WaterBowlBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WATER_BOWL.get(), pos, state);
    }

    public int water() {
        return this.water;
    }

    public int capacity() {
        return this.getBlockState().getBlock() instanceof WaterBowlBlock bowl ? bowl.size.waterMl : FeederSize.SMALL.waterMl;
    }

    // Pour some in (capped at what it holds); how much went in
    public int pour(int ml) {
        int added = Math.min(ml, this.capacity() - this.water);
        if (added > 0) this.set(this.water + added);
        return added;
    }

    // A pet drinks up to this much; how much it got
    public int take(int ml) {
        int taken = Math.min(ml, this.water);
        if (taken > 0) this.set(this.water - taken);
        return taken;
    }

    public void set(int ml) {
        this.water = Math.clamp(ml, 0, this.capacity());
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            BlockState state = this.getBlockState();
            int shown = this.water <= 0 ? 0 : Math.max(1, Math.min(3, Math.round(3f * this.water / this.capacity())));
            if (state.hasProperty(WaterBowlBlock.LEVEL) && state.getValue(WaterBowlBlock.LEVEL) != shown) {
                this.level.setBlock(this.worldPosition, state.setValue(WaterBowlBlock.LEVEL, shown), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.literal((this.getBlockState().getBlock() instanceof WaterBowlBlock bowl ? bowl.size.title + " " : "") + "Water Bowl");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new WaterBowlMenu(containerId, inventory, this.data, this.worldPosition);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.water = input.getIntOr("water", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("water", this.water);
    }
}
