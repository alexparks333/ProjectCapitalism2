package com.alexh.capitalism.weaponsmith;

import com.alexh.capitalism.core.ModBlockEntities;
import com.alexh.capitalism.core.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// A smith table's lava tank (Weaponsmith Table, Toolsmith Table): its fuel. Lava Buckets poured in through the menu's fuel slot fill it;
// every recipe burns some (WeaponsmithTableRecipes). Kept when the menu closes (it's in the block), lost if it's broken.
public class SmithTableBlockEntity extends BlockEntity {

    public static final int BUCKET = 1000;
    public static final int CAPACITY = 4 * BUCKET;

    private int lava;

    public SmithTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WEAPONSMITH_TABLE.get(), pos, state);
    }

    public int lava() {
        return this.lava;
    }

    // Fuel poured in: a Lava Bucket is 1 bucket, a Refined Bucket of Lava (Lava Creator) is 2
    public static int fuelValue(ItemStack stack) {
        if (stack.is(Items.LAVA_BUCKET)) return BUCKET;
        if (stack.is(ModItems.REFINED_LAVA_BUCKET.get())) return 2 * BUCKET;
        return 0;
    }

    // True if all of it fits (and was poured in)
    public boolean pour(int amount) {
        if (amount <= 0 || this.lava + amount > CAPACITY) return false;
        this.lava += amount;
        this.setChanged();
        return true;
    }

    public void burn(int amount) {
        this.lava = Math.max(0, this.lava - amount);
        this.setChanged();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.lava = Math.clamp(input.getIntOr("lava", 0), 0, CAPACITY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("lava", this.lava);
    }
}
