package com.alexh.capitalism.block;

import com.alexh.capitalism.core.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

// A kitchen sink. It does one thing: right-click it with a Dirty Plate and you get the clean plate back.
public class SinkBlock extends Block {

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    public SinkBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
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
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    // The clean plate for a dirty one, or null if it isn't a dirty plate.
    private static @Nullable Item cleaned(ItemStack stack) {
        if (stack.is(ModItems.DIRTY_SILVER_PLATE.get())) return ModItems.SILVER_PLATE.get();
        if (stack.is(ModItems.DIRTY_GOLDEN_PLATE.get())) return ModItems.GOLDEN_PLATE.get();
        return null;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        Item clean = cleaned(stack);
        if (clean == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level instanceof ServerLevel serverLevel) {
            stack.shrink(1);
            // into the inventory the normal way, so it stacks with clean plates you already have
            ItemStack washed = new ItemStack(clean);
            if (!player.getInventory().add(washed)) {
                player.drop(washed, false, Prediction.SERVER_ONLY);
            }
            serverLevel.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.6F, 1.3F);
            serverLevel.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 0.95, pos.getZ() + 0.5, 12, 0.2, 0.05, 0.2, 0.1);
        }
        return InteractionResult.SUCCESS;
    }
}
