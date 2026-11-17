package com.alexh.capitalism.farmer;

import com.alexh.capitalism.core.ModBlockEntities;
import com.alexh.capitalism.designer.DesignerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// The Grill (Cook.md > The Grill): three slots for fine cuts to sear at once (MeatItems.Cut) and a fuel slot. Every steak burns
// one charge of heat: a piece of coal or charcoal gives COAL_SEARS, a blaze rod BLAZE_SEARS (taken when the last
// charge is used up). The searing itself is the Grill's game (GrillMenu, client/SearGame).
public class GrillBlockEntity extends DesignerBlockEntity {

    public static final int CUT = 0;
    public static final int CUTS = 3;
    public static final int FUEL = CUTS;
    public static final int SIZE = CUTS + 1;
    public static final int COAL_SEARS = 4;
    public static final int BLAZE_SEARS = 6;

    private int charges;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return GrillBlockEntity.this.charges;
        }

        @Override
        public void set(int index, int value) {
            GrillBlockEntity.this.charges = value;
        }

        @Override
        public int getCount() {
            return 1;
        }
    };

    public GrillBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GRILL.get(), pos, state, "Grill", SIZE);
    }

    // How many sears one of this fuel gives (0: not fuel)
    public static int fuel(ItemStack stack) {
        return stack.is(Items.COAL) || stack.is(Items.CHARCOAL) ? COAL_SEARS : stack.is(Items.BLAZE_ROD) ? BLAZE_SEARS : 0;
    }

    public static boolean searable(ItemStack stack) {
        return stack.getItem() instanceof MeatItems.Cut;
    }

    public int charges() {
        return this.charges;
    }

    // There's heat for one more sear (lighting more fuel if it needs to)
    public boolean hasHeat() {
        return heat(this.charges, this.items.get(FUEL)) > 0;
    }

    // How many sears of heat: the charges left plus all the fuel waiting
    public static int heat(int charges, ItemStack fuel) {
        return charges + fuel(fuel) * fuel.getCount();
    }

    // One sear's worth of heat: true if there was any
    public boolean useHeat() {
        if (this.charges <= 0) {
            int sears = fuel(this.items.get(FUEL));
            if (sears <= 0) return false;
            this.items.get(FUEL).shrink(1);
            this.charges = sears;
        }
        this.charges--;
        this.setChanged();
        return true;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot < CUTS ? searable(stack) : fuel(stack) > 0;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new GrillMenu(containerId, inventory, this, this.data, net.minecraft.world.inventory.ContainerLevelAccess.create(this.level, this.worldPosition));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.charges = input.getIntOr("charges", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("charges", this.charges);
    }
}
