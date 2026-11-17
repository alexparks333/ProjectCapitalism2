package com.alexh.capitalism.farmer;

import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

// Fertilized Farmland (Farmer.md > Fertilizer): farmland a Farmer has fertilized, so it can be seen. Everything about it
// is farmland's (it gets wet, dries out, gets trampled, holds crops: it's in the same tags), but its top shows what's
// on it (FERTILIZER: speckled green for Yield, silver glints for Super Yield, yellow and blue for Fast). FertilizedSoil turns
// farmland into it when fertilized and back once it's used up. When it turns to dirt (trampled, dried out, covered) its fertilizer is lost: hoeing it again gives plain farmland.
public class FertilizedFarmlandBlock extends FarmlandBlock {

    public enum Look implements StringRepresentable {
        YIELD("yield"),
        SUPER_YIELD("super_yield"),
        FAST("fast");

        private final String name;

        Look(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public static final EnumProperty<Look> FERTILIZER = EnumProperty.create("fertilizer", Look.class);

    public FertilizedFarmlandBlock(Properties properties) {
        super(Blocks.DIRT, properties);
        this.registerDefaultState(this.defaultBlockState().setValue(FERTILIZER, Look.YIELD));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FERTILIZER);
    }

    // Back to dirt: the fertilizer goes with it
    @Override
    public void turnToBaseBlock(@Nullable Entity sourceEntity, BlockState state, Level level, BlockPos pos) {
        FertilizedSoil.clear(level, pos);
        super.turnToBaseBlock(sourceEntity, state, level, pos);
    }
}
