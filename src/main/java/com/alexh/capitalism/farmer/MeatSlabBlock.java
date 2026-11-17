package com.alexh.capitalism.farmer;

import com.alexh.capitalism.core.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

// A Meat Slab (MeatCut) set down in the world: a chunk of the animal on the counter. Its model is sculpted by
// tools/make_meat_slab_blocks.ps1 (tools/MeatSculpt.cs: the cut's real shape in half- or quarter-pixel boxes of meat,
// fat, skin and bone; 64x64 textures from tools/MeatTex.cs); its outline is the model's box (MeatSlabShapes, generated
// with it). It keeps rotting while it sits there (MeatSlabBlockEntity, Freshness): once its time is up it goes ROTTEN
// (grey-green, with flies), and breaking it gives Rotten Meat instead of the slab.
public class MeatSlabBlock extends BaseEntityBlock {

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty ROTTEN = BooleanProperty.create("rotten");

    public final MeatCut cut;
    private final Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);

    public MeatSlabBlock(MeatCut cut, Properties properties) {
        super(properties);
        this.cut = cut;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ROTTEN, false));
        double[] b = MeatSlabShapes.bounds(cut.id);
        // turned like the blockstate turns the model: clockwise from above
        this.shapes.put(Direction.NORTH, Block.box(b[0], b[1], b[2], b[3], b[4], b[5]));
        this.shapes.put(Direction.EAST, Block.box(16 - b[5], b[1], b[0], 16 - b[2], b[4], b[3]));
        this.shapes.put(Direction.SOUTH, Block.box(16 - b[3], b[1], 16 - b[5], 16 - b[0], b[4], 16 - b[2]));
        this.shapes.put(Direction.WEST, Block.box(b[2], b[1], 16 - b[3], b[5], b[4], 16 - b[0]));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ROTTEN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shapes.get(state.getValue(FACING));
    }

    // Rotten: a fly buzzing round it now and then
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ROTTEN) || random.nextInt(4) != 0) return;
        level.addParticle(ParticleTypes.MYCELIUM, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.4 + random.nextDouble() * 0.4,
            pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0.0, 0.0, 0.0);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MeatSlabBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level instanceof ServerLevel serverLevel && !state.getValue(ROTTEN)
            ? createTickerHelper(type, ModBlockEntities.MEAT_SLAB.get(),
                (innerLevel, pos, innerState, entity) -> MeatSlabBlockEntity.serverTick(serverLevel, pos, innerState, entity))
            : null;
    }
}
