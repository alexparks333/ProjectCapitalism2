package com.alexh.capitalism.decor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

// A shop sign for over a double door (Town Hall, Bank, Boat Shop: Town.md > Shop signs): a painted board two blocks
// wide and one tall, flat on a wall. Put it on the wall above the LEFT door (as you face it) and it fills the block to
// the right too; break either half and both go. The pictures are tools/make_shop_signs.ps1 (each half its own texture).
// You walk through it (no collision), like a sign.
public class ShopSignBlock extends Block {

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");

    // flat against the wall behind it (facing north: the board is at the south edge)
    private static final VoxelShape NORTH = Block.box(0, 0, 14.5, 16, 16, 16);
    private static final VoxelShape SOUTH = Block.box(0, 0, 0, 16, 16, 1.5);
    private static final VoxelShape EAST = Block.box(0, 0, 0, 1.5, 16, 16);
    private static final VoxelShape WEST = Block.box(14.5, 0, 0, 16, 16, 16);

    public ShopSignBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(RIGHT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, RIGHT);
    }

    // the other half is to the right of the left one, as you face the sign
    private static Direction toRight(Direction facing) {
        return facing.getCounterClockWise();
    }

    private static BlockPos otherHalf(BlockPos pos, BlockState state) {
        Direction right = toRight(state.getValue(FACING));
        return state.getValue(RIGHT) ? pos.relative(right.getOpposite()) : pos.relative(right);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        // on a wall only; if clicked on top of something, face the player
        Direction facing = face.getAxis().isHorizontal() ? face : context.getHorizontalDirection().getOpposite();
        BlockPos right = context.getClickedPos().relative(toRight(facing));
        if (!context.getLevel().getBlockState(right).canBeReplaced(context)) return null;
        return this.defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
        level.setBlock(pos.relative(toRight(state.getValue(FACING))), state.setValue(RIGHT, true), Block.UPDATE_ALL);
    }

    // one half gone: the other goes too
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
        if (neighbourPos.equals(otherHalf(pos, state)) && !(neighbour.is(this) && neighbour.getValue(RIGHT) != state.getValue(RIGHT))) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    // breaking the right half in survival: drop it once (from the left half)
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && state.getValue(RIGHT) && !player.isCreative()) {
            BlockPos left = otherHalf(pos, state);
            BlockState leftState = level.getBlockState(left);
            if (leftState.is(this)) level.destroyBlock(left, true, player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            default -> NORTH;
        };
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
