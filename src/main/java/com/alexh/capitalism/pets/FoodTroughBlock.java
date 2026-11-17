package com.alexh.capitalism.pets;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

// The Food Trough (PetTrainer.md): a low metal trough, Small (copper), Medium (iron) or Large (Compacted Iron): 1, 2 or 5
// slots of food (FeederSize). Anyone can open it (hoppers work too) and put food in; pets within 5 blocks that are
// hungry walk over and eat their own food from it till they're full (Pets). FILLED shows a heap of food in it. Pet
// Trainers craft it.
public class FoodTroughBlock extends BaseEntityBlock {

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty FILLED = BooleanProperty.create("filled");
    public final FeederSize size;
    private final VoxelShape shape;

    public FoodTroughBlock(Properties properties, FeederSize size) {
        super(properties);
        this.size = size;
        this.shape = switch (size) {
            case SMALL -> box(2, 0, 2, 14, 5, 14);
            case MEDIUM -> box(1, 0, 1, 15, 6, 15);
            case LARGE -> box(0, 0, 0, 16, 8, 16);
        };
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FILLED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FILLED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
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
        return this.shape;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FoodTroughBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof FoodTroughBlockEntity trough) {
            serverPlayer.openMenu(trough, buf -> buf.writeVarInt(trough.getContainerSize()));
        }
        return InteractionResult.SUCCESS;
    }
}
