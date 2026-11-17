package com.alexh.capitalism.decor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

// The Secret Bookshelf Door (decor/BloodDecorKind.door, Decorations.md): a gothic bookshelf two blocks tall, placed like
// a door (the lower half where you click, the upper above it). Right-click it (the red book, or anywhere on it) and the
// whole shelf swings inward on a hinge at its side with a grinding of stone, letting you through (nothing to bump into
// while it's open); it swings shut by itself after about 5 seconds, or when you right-click it again. Only the lower half
// has a block entity: client/BloodDecorRenderer draws the whole shelf from it.
public class BloodDoorBlock extends BaseEntityBlock implements BloodDecorKind.Holder {

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final BooleanProperty OPEN = BloodDecorBlock.OPEN;
    // how long it stays open (ticks)
    public static final int OPEN_TICKS = 100;

    // shut: the shelf panel at the front of its block; open: swung against its hinge side (facing west: the model's
    // front -x in the world)
    private static final VoxelShape SHUT = Block.box(0, 0, 0, 5, 16, 16);
    private static final VoxelShape SWUNG = Block.box(5, 0, 0, 16, 16, 5);

    private final BloodDecorKind kind;
    private final Map<Direction, VoxelShape> shut = new EnumMap<>(Direction.class);
    private final Map<Direction, VoxelShape> swung = new EnumMap<>(Direction.class);

    public BloodDoorBlock(BloodDecorKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            this.shut.put(d, BloodDecorBlock.turn(SHUT, d));
            this.swung.put(d, BloodDecorBlock.turn(SWUNG, d));
        }
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER).setValue(OPEN, false));
    }

    @Override
    public BloodDecorKind kind() {
        return this.kind;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, OPEN);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() >= level.getMaxY() || !level.getBlockState(pos.above()).canBeReplaced(context)) return null;
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
        // it remembers who placed it: theirs to open, unless it stands on someone's lot
        if (by instanceof Player p && level.getBlockEntity(pos) instanceof BloodDecorBlockEntity be) be.setOwner(p.getUUID());
    }

    // Who may open it: the owner of the property lot it stands on (realestate/Properties), or if it's on no lot (or an
    // unowned one), whoever placed it. Nobody else, not even trusted players; operators in Creative always can.
    public static boolean mayOpen(Level level, BlockPos lower, Player player) {
        if (player.isCreative() && player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)) return true;
        java.util.UUID owner = null;
        var lot = com.alexh.capitalism.realestate.Properties.at(level, lower);
        if (lot != null) owner = lot.owner();
        if (owner == null && level.getBlockEntity(lower) instanceof BloodDecorBlockEntity be) owner = be.owner();
        return owner == null || owner.equals(player.getUUID());
    }

    // the two halves keep together: one goes, so does the other; the upper one follows the lower one open and shut
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        DoubleBlockHalf half = state.getValue(HALF);
        if (direction.getAxis() == Direction.Axis.Y && (half == DoubleBlockHalf.LOWER) == (direction == Direction.UP)) {
            if (!neighbourState.is(this) || neighbourState.getValue(HALF) == half) return Blocks.AIR.defaultBlockState();
            return half == DoubleBlockHalf.UPPER ? state.setValue(OPEN, neighbourState.getValue(OPEN)) : state;
        }
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    // breaking the upper half in Creative doesn't drop the lower one's item
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.preventsBlockDrops() && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockPos below = pos.below();
            BlockState lower = level.getBlockState(below);
            if (lower.is(this)) {
                level.setBlock(below, Blocks.AIR.defaultBlockState(), 35);
                level.levelEvent(player, 2001, below, Block.getId(lower));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server) {
            BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
            BlockState l = level.getBlockState(lower);
            if (!l.is(this)) return InteractionResult.SUCCESS;
            if (!mayOpen(level, lower, player)) {
                player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("It won't budge.").withStyle(net.minecraft.ChatFormatting.GRAY));
                server.playSound(null, lower, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1.0F, 0.6F);
                return InteractionResult.SUCCESS;
            }
            swing(server, lower, l, !l.getValue(OPEN));
        }
        return InteractionResult.SUCCESS;
    }

    // swings it open (it shuts again by itself after OPEN_TICKS) or shut
    public static void swing(ServerLevel level, BlockPos lower, BlockState state, boolean open) {
        level.setBlock(lower, state.setValue(OPEN, open), 3);
        level.playSound(null, lower, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.8F, 0.45F);
        level.playSound(null, lower, open ? SoundEvents.PISTON_EXTEND : SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.5F, 0.5F);
        if (open) level.scheduleTick(lower, state.getBlock(), OPEN_TICKS);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER && state.getValue(OPEN)) swing(level, pos, state, false);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (state.getValue(OPEN) ? this.swung : this.shut).get(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(OPEN) ? Shapes.empty() : this.shut.get(state.getValue(FACING));
    }

    @Override
    protected boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) {
        return state.getValue(OPEN);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new BloodDecorBlockEntity(pos, state) : null;
    }
}
