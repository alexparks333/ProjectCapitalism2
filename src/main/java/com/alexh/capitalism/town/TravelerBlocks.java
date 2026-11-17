package com.alexh.capitalism.town;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

// Decorations only the Traveling Merchant sells (Town.md > The Traveling Merchant):
//  - the Traveler's Snow Globe: a glass dome on a wooden base round a tiny snowy cabin and pine. Right-click it and the
//    snow inside swirls up (snowflakes, a soft chime).
//  - the Traveler's Lantern: an old brass-and-iron road lantern with a little handle. It lights up when the Traveling
//    Merchant is about to come to town (the day before he's due) and stays lit while he's here; dark the rest of the time.
//    It checks every few seconds.
public final class TravelerBlocks {

    private TravelerBlocks() {
    }

    public static class SnowGlobe extends Block {
        public static final net.minecraft.world.level.block.state.properties.EnumProperty<net.minecraft.core.Direction> FACING = HorizontalDirectionalBlock.FACING;
        private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 14.5, 14);

        public SnowGlobe(Properties properties) {
            super(properties);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.SNOWFLAKE, pos.getX() + 0.5, pos.getY() + 0.55, pos.getZ() + 0.5, 30, 0.2, 0.2, 0.2, 0.02);
                server.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8F, 1.6F);
                server.playSound(null, pos, SoundEvents.POWDER_SNOW_STEP, SoundSource.BLOCKS, 0.5F, 1.4F);
            }
            return InteractionResult.SUCCESS;
        }
    }

    public static class Lantern extends Block {
        public static final BooleanProperty LIT = BlockStateProperties.LIT;
        private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11, 10, 11);
        private static final int CHECK_TICKS = 100;

        public Lantern(Properties properties) {
            super(properties);
            this.registerDefaultState(this.stateDefinition.any().setValue(LIT, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(LIT);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        // lit: he's due tomorrow (or today), or he's in town
        public static boolean shouldLight(ServerLevel level) {
            var server = level.getServer();
            long today = TravelingMerchantShop.today(server);
            return TravelingMerchantShop.inTown(server) || TravelingMerchantShop.nextDay(server) - today <= 1;
        }

        @Override
        protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
            if (level instanceof ServerLevel server) {
                if (state.getValue(LIT) != shouldLight(server)) level.setBlock(pos, state.setValue(LIT, shouldLight(server)), Block.UPDATE_ALL);
                server.scheduleTick(pos, this, CHECK_TICKS);
            }
        }

        @Override
        protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            boolean lit = shouldLight(level);
            if (lit != state.getValue(LIT)) {
                level.setBlock(pos, state.setValue(LIT, lit), Block.UPDATE_ALL);
                if (lit) level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.4F, 1.4F);
            }
            level.scheduleTick(pos, this, CHECK_TICKS);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (state.getValue(LIT) && random.nextInt(5) == 0) {
                level.addParticle(ParticleTypes.SMALL_FLAME, pos.getX() + 0.5, pos.getY() + 0.45, pos.getZ() + 0.5, 0, 0.005, 0);
            }
        }
    }
}
