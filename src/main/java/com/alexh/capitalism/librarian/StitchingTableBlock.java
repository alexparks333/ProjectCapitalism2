package com.alexh.capitalism.librarian;

import com.alexh.capitalism.core.ModBlockEntities;
import com.alexh.capitalism.occupation.Job;
import com.alexh.capitalism.occupation.JobEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

// The Stitching Table (Librarian.md): a wooden worktable with a leather mat and a spool of thread (the needle and
// what's on the mat are drawn by client/StitchingTableRenderer: the needle sews round the book while it works). Only
// Librarians can craft and use it. It slowly sews Paper and Leather / Rabbit Hide into Rough Books (Iron Ingot thread)
// or Delicate Books (Diamond thread): StitchingTableBlockEntity. SEWING while it works (bits of thread fly).
public class StitchingTableBlock extends BaseEntityBlock {

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty SEWING = BooleanProperty.create("sewing");
    private static final VoxelShape SHAPE = Shapes.or(box(0, 0, 0, 16, 13, 16), box(9, 13, 3, 13, 17, 7));

    public StitchingTableBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SEWING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SEWING);
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
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StitchingTableBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof StitchingTableBlockEntity table) {
            if (!player.isCreative() && !JobEvents.has(player, Job.LIBRARIAN)) {
                serverPlayer.sendOverlayMessage(Component.literal("Only a Librarian can use the Stitching Table."));
                return InteractionResult.SUCCESS;
            }
            player.openMenu(table);
        }
        return InteractionResult.SUCCESS;
    }

    // While sewing: wisps of thread and a sparkle now and then off the book (the needle itself is drawn by
    // client/StitchingTableRenderer)
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(SEWING)) return;
        double x = pos.getX() + 0.3 + random.nextDouble() * 0.25;
        double z = pos.getZ() + 0.45 + random.nextDouble() * 0.25;
        if (random.nextInt(2) == 0) level.addParticle(ParticleTypes.WHITE_ASH, x, pos.getY() + 0.9, z, 0, 0.02, 0);
        if (random.nextInt(6) == 0) level.addParticle(ParticleTypes.ENCHANT, x, pos.getY() + 1.1, z, 0, 0.1, 0);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level instanceof ServerLevel serverLevel
            ? createTickerHelper(type, ModBlockEntities.STITCHING_TABLE.get(),
                (innerLevel, pos, innerState, entity) -> StitchingTableBlockEntity.serverTick(serverLevel, pos, innerState, entity))
            : null;
    }
}
