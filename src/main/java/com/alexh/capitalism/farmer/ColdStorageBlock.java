package com.alexh.capitalism.farmer;

import com.alexh.capitalism.core.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import org.jspecify.annotations.Nullable;

// The Fridge (an upright white fridge, a green light on while it runs) and the Freezer (a white chest freezer, a blue
// light): ColdStorageBlockEntity keeps meat in them fresh for longer while they have fuel. Anyone can open one.
// Breaking it drops what's in it.
public class ColdStorageBlock extends BaseEntityBlock {

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public final ColdStorageBlockEntity.Kind kind;

    public ColdStorageBlock(ColdStorageBlockEntity.Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ColdStorageBlockEntity storage) {
            player.openMenu(storage);
            level.playSound(null, pos, this.kind == ColdStorageBlockEntity.Kind.FREEZER ? SoundEvents.CHEST_OPEN : SoundEvents.IRON_DOOR_OPEN,
                SoundSource.BLOCKS, 0.5F, 1.3F);
        }
        return InteractionResult.SUCCESS;
    }

    // While it runs: a wisp of cold now and then
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT) || random.nextInt(3) != 0) return;
        Direction front = state.getValue(FACING);
        double x = pos.getX() + 0.5 + front.getStepX() * 0.55 + (random.nextDouble() - 0.5) * 0.6;
        double z = pos.getZ() + 0.5 + front.getStepZ() * 0.55 + (random.nextDouble() - 0.5) * 0.6;
        double y = pos.getY() + 0.2 + random.nextDouble() * 0.6;
        level.addParticle(this.kind == ColdStorageBlockEntity.Kind.FREEZER ? ParticleTypes.SNOWFLAKE : ParticleTypes.WHITE_ASH, x, y, z, 0.0, -0.01, 0.0);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ColdStorageBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        BlockEntityType<ColdStorageBlockEntity> mine = this.kind == ColdStorageBlockEntity.Kind.FREEZER
            ? ModBlockEntities.FREEZER.get() : ModBlockEntities.FRIDGE.get();
        return createTickerHelper(type, mine, (innerLevel, pos, innerState, entity) -> ColdStorageBlockEntity.serverTick(serverLevel, pos, innerState, entity));
    }
}
