package com.alexh.capitalism.mail;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

// The Mailbox (Mailbox.md): a painted metal mailbox on a wooden post, its door toward whoever put it down. It's theirs:
// they open it like a chest and take their mail out; anyone else gets the mail slot and can only put things in. The
// little red flag goes up while there's mail in it, and the owner sees how many letters are waiting when they walk up
// (client/MailboxRenderer). Only its owner (or someone in creative) can break it; what's in it drops.
public class MailboxBlock extends Block implements EntityBlock {

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    // the post, and the box on top of it (long the way the door faces): more than a block tall, like a fence
    private static final VoxelShape POST = Block.box(6.5, 0, 6.5, 9.5, 13, 9.5);
    private static final VoxelShape ALONG_X = Shapes.or(POST, Block.box(1.6, 13, 4.5, 14.4, 21.5, 11.5));
    private static final VoxelShape ALONG_Z = Shapes.or(POST, Block.box(4.5, 13, 1.6, 11.5, 21.5, 14.4));

    public MailboxBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        // the Town Hall Mailbox: an op in creative puts it in front of the Town Hall
        if (this == com.alexh.capitalism.core.ModBlocks.TOWN_MAILBOX.get() && context.getPlayer() instanceof ServerPlayer player
                && !(com.alexh.capitalism.town.TownProtection.canBuild(player) && player.isCreative())) {
            player.sendOverlayMessage(Component.literal("Only an op in creative can put down the Town Hall Mailbox."));
            return null;
        }
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
        if (this == com.alexh.capitalism.core.ModBlocks.TOWN_MAILBOX.get()) return;
        if (by instanceof Player player && level.getBlockEntity(pos) instanceof MailboxBlockEntity mailbox) {
            mailbox.setOwner(player.getUUID(), player.getGameProfile().name());
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.X ? ALONG_X : ALONG_Z;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    // someone else's mailbox won't break (they'd get its mail)
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof MailboxBlockEntity mailbox && !mailbox.isOwner(player) && !player.isCreative()) return 0.0F;
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof MailboxBlockEntity mailbox) {
            if (!mailbox.hasOwner()) {
                // (one put down by a command or structure) the first to open it gets it
                mailbox.setOwner(player.getUUID(), player.getGameProfile().name());
                serverPlayer.sendOverlayMessage(Component.literal("This mailbox is yours now."));
            }
            mailbox.open(serverPlayer);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MailboxBlockEntity(pos, state);
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
