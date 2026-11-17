package com.alexh.capitalism.farmer;

import com.alexh.capitalism.occupation.Job;
import com.alexh.capitalism.occupation.JobEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

// The Grill (Cook.md > The Grill): a black iron grill box with a grate over glowing coals. Only Cooks can use it: sear
// a fine cut to the doneness a customer ordered (GrillMenu, client/SearGame). LIT while a steak is on (smoke and
// sizzle). Breaking it drops what's in it.
public class GrillBlock extends BaseEntityBlock {

    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public GrillBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof GrillBlockEntity grill) {
            if (!player.isCreative() && !JobEvents.has(player, Job.COOK)) {
                serverPlayer.sendOverlayMessage(Component.literal("Only a Cook can use the Grill."));
                return InteractionResult.SUCCESS;
            }
            player.openMenu(grill);
        }
        return InteractionResult.SUCCESS;
    }

    // A steak on: smoke curling up, a flicker of flame and the odd sizzle
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;
        double x = pos.getX() + 0.25 + random.nextDouble() * 0.5;
        double z = pos.getZ() + 0.25 + random.nextDouble() * 0.5;
        level.addParticle(ParticleTypes.SMOKE, x, pos.getY() + 1.02, z, 0.0, 0.04, 0.0);
        if (random.nextInt(3) == 0) level.addParticle(ParticleTypes.SMALL_FLAME, x, pos.getY() + 0.95, z, 0.0, 0.01, 0.0);
        if (random.nextInt(5) == 0) level.playLocalSound(pos, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.6F, 1.4F, false);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GrillBlockEntity(pos, state);
    }
}
