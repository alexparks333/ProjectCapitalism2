package com.alexh.capitalism.designer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;

// The Sewing Machine (Designer.md > Sewing Machine): an old black-and-gold machine on a wooden base, its arm reaching
// over to the needle. Designers sew Patches on it (SewingMachineBlockEntity, client/SewingGame).
public class SewingMachineBlock extends DesignerBlock {

    public SewingMachineBlock(Properties properties) {
        super(properties, "Sewing Machine", Shapes.or(box(1, 0, 3, 15, 2, 13), box(2, 2, 5, 15, 13, 12)));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SewingMachineBlockEntity(pos, state);
    }
}
