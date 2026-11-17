package com.alexh.capitalism.farmer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

// The Scythes (Farmer.md > Scythe): break a ripe crop with one and every ripe crop in a patch comes down with it: the
// Scythe's is 5 wide and 3 deep (the one you hit and the two beyond it, the way you face, and two either side of
// each), the Diamond Scythe's the same, lasting four times as long. Unripe crops are left standing. The harvest is the same as by hand
// (FarmHarvestModifier: full for a Farmer). Only Farmers can make one; anyone can use it. Each swing that cuts crops
// wears it by one.
public class ScytheItem extends Item {

    public static final int USES = 400;
    public static final int DIAMOND_USES = 1600;

    // how far it reaches to each side of the crop you hit, and how many rows deep
    private final int reach;
    private final int depth;

    public ScytheItem(Properties properties, int reach, int depth) {
        super(properties);
        this.reach = reach;
        this.depth = depth;
    }

    public static ScytheItem iron(Properties properties) {
        return new ScytheItem(properties, 2, 3);
    }

    public static ScytheItem diamond(Properties properties) {
        return new ScytheItem(properties, 2, 3);
    }

    // The patch it cuts: forward from the crop you hit (the way you face), `depth` deep, and `reach` to the left and right
    public Iterable<BlockPos> patch(BlockPos hit, Direction facing) {
        Direction side = facing.getClockWise();
        java.util.List<BlockPos> out = new java.util.ArrayList<>();
        for (int deep = 0; deep < this.depth; deep++) {
            for (int across = -this.reach; across <= this.reach; across++) {
                out.add(hit.relative(facing, deep).relative(side, across));
            }
        }
        return out;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        if (!(level instanceof ServerLevel) || !FarmHarvestModifier.isRipeCrop(state)) return false;
        for (BlockPos spot : this.patch(pos, owner.getDirection())) {
            if (!spot.equals(pos) && FarmHarvestModifier.isRipeCrop(level.getBlockState(spot))) level.destroyBlock(spot, true, owner);
        }
        stack.hurtAndBreak(1, owner, EquipmentSlot.MAINHAND);
        return true;
    }
}
