package com.alexh.capitalism.farmer;

import com.alexh.capitalism.core.ModBlockEntities;
import com.alexh.capitalism.occupation.Job;
import com.alexh.capitalism.occupation.JobEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

// The Decomposter (Farmer.md > Decomposter): a slatted wooden bin. Farmers rot Bone Meal, Rotten Flesh and Dirt down into
// Fertilizer in it (DecomposterBlockEntity). It stands empty until ingredients go in, then fills with dark compost
// (FULL); while it works the compost bubbles and green specks rise out of it (WORKING). Only Farmers can use it.
// (Breaking it drops what's in it.)
public class DecomposterBlock extends BaseEntityBlock {

    public static final BooleanProperty WORKING = BooleanProperty.create("working");
    public static final BooleanProperty FULL = BooleanProperty.create("full");

    public DecomposterBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(WORKING, false).setValue(FULL, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WORKING, FULL);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof DecomposterBlockEntity decomposter) {
            if (!player.isCreative() && !JobEvents.has(player, Job.FARMER)) {
                serverPlayer.sendOverlayMessage(Component.literal("Only a Farmer can use the Decomposter."));
                return InteractionResult.SUCCESS;
            }
            player.openMenu(decomposter);
        }
        return InteractionResult.SUCCESS;
    }

    // While it works: a bubble now and then, and green specks drifting up off the compost
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(WORKING)) return;
        double x = pos.getX() + 0.2 + random.nextDouble() * 0.6;
        double z = pos.getZ() + 0.2 + random.nextDouble() * 0.6;
        level.addParticle(ParticleTypes.COMPOSTER, x, pos.getY() + 0.9, z, 0.0, 0.03, 0.0);
        if (random.nextInt(4) == 0) level.addParticle(ParticleTypes.BUBBLE_POP, x, pos.getY() + 0.85, z, 0.0, 0.02, 0.0);
        if (random.nextInt(12) == 0) level.playLocalSound(pos, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.BLOCKS, 0.4F, 0.6F + random.nextFloat() * 0.3F, false);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DecomposterBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level instanceof ServerLevel serverLevel
            ? createTickerHelper(type, ModBlockEntities.DECOMPOSTER.get(),
                (innerLevel, pos, innerState, entity) -> DecomposterBlockEntity.serverTick(serverLevel, pos, innerState, entity))
            : null;
    }
}
