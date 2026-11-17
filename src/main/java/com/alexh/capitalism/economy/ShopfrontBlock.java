package com.alexh.capitalism.economy;

import com.alexh.capitalism.core.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

// A 2-block-tall market stall shop: wooden counter below (holds the shop), striped awning and a crooked "Shop"
// sign above. Right-click either half: the owner gets the owner screen (shift + right-click for the customer
// view), everyone else gets the store. Only the owner can break it (ContainerLocks), and it's always locked.
public class ShopfrontBlock extends BaseEntityBlock {

    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    // Placeholder owner for the dev ownership toggle: a shop nobody can manage (admins can take it back).
    private static final ContainerOwner NOBODY = new ContainerOwner(new java.util.UUID(0L, 1L), "Nobody", true);

    public ShopfrontBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, FACING);
    }

    // Only place if there's room for the top half.
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() >= level.getMaxY() || !level.getBlockState(pos.above()).canBeReplaced(context)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        // noted for the Bounty Clerk's town tasks (bounty/ShopIndex)
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel && by instanceof net.minecraft.server.level.ServerPlayer placer) {
            com.alexh.capitalism.bounty.ShopIndex.note(serverLevel, pos, placer);
        }
    }

    // When one half disappears, the other goes too (the bottom half drops the shop's items and earnings).
    @Override
    protected BlockState updateShape(
        BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour,
        BlockPos neighbourPos, BlockState neighbourState, RandomSource random
    ) {
        DoubleBlockHalf half = state.getValue(HALF);
        boolean towardOtherHalf = directionToNeighbour == (half == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN);
        if (towardOtherHalf && !(neighbourState.is(this) && neighbourState.getValue(HALF) != half)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    // In creative, breaking the top half shouldn't drop the bottom half's item.
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.preventsBlockDrops() && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockPos below = pos.below();
            BlockState belowState = level.getBlockState(below);
            if (belowState.is(this) && belowState.getValue(HALF) == DoubleBlockHalf.LOWER) {
                level.setBlock(below, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                level.levelEvent(player, 2001, below, Block.getId(belowState));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    // Only the bottom half holds the shop.
    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new ShopfrontBlockEntity(pos, state) : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        if (!level.isClientSide() && level.getBlockEntity(lower) instanceof ShopfrontBlockEntity shop) {
            ContainerOwner owner = ContainerLocks.ownerOf(shop);

            // Dev/admin tool: shift + right-click with an empty hand flips ownership between you and "Nobody",
            // so one person can test both the owner and the customer side. Ops only.
            if (player.isShiftKeyDown() && player.getMainHandItem().isEmpty()
                && player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                boolean mine = owner != null && owner.isOwnedBy(player.getUUID());
                ContainerOwner next = mine ? NOBODY : new ContainerOwner(player.getUUID(), player.getGameProfile().name(), true);
                shop.setData(ModAttachments.CONTAINER_OWNER, next);
                shop.setChanged();
                player.sendOverlayMessage(Component.literal(mine ? "[Dev] Shop now owned by Nobody - you're a customer."
                    : "[Dev] You own this shop now.").withStyle(ChatFormatting.LIGHT_PURPLE));
                return InteractionResult.SUCCESS;
            }

            String ownerName = owner == null ? "" : owner.name();
            boolean isOwner = owner != null && owner.isOwnedBy(player.getUUID());
            if (isOwner && !player.isShiftKeyDown()) {
                player.openMenu(new SimpleMenuProvider((id, inventory, p) -> ShopfrontMenu.owner(id, inventory, shop),
                    Component.literal("Your Shop")));
            } else {
                player.openMenu(new SimpleMenuProvider((id, inventory, p) -> ShopfrontMenu.customer(id, inventory, shop),
                    Component.literal(ownerName.isEmpty() ? "Shop" : ownerName + "'s Shop")));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
