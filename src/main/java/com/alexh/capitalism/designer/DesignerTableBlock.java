package com.alexh.capitalism.designer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;

// The Designer Table (Designer.md > Designer Table): a dark walnut worktable with a green cutting mat, a bolt of cloth
// and a pin cushion. Designers put bags together on it from Leather and Patches (DesignerTableBlockEntity).
public class DesignerTableBlock extends DesignerBlock {

    public DesignerTableBlock(Properties properties) {
        super(properties, "Designer Table", Shapes.or(box(0, 12, 0, 16, 15, 16), box(1, 0, 1, 15, 12, 15)));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DesignerTableBlockEntity(pos, state);
    }
}
