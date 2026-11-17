package com.alexh.capitalism.enchanter;

import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.occupation.Job;
import com.alexh.capitalism.occupation.JobEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

// The Transfer Tube (Enchanter.md): 3 blocks wide, 2 high. A glass booth on each side for a player to stand in, and a
// console in the middle (a bookshelf with a stone button), with a wooden sign on top showing an arrow (click it to flip which way the XP goes), the bookshelf under it
// has the button that starts a transfer. Only Enchanters can make it, flip the arrow and press the button.
// The transfer happens TRANSFER_DELAY ticks (2 s) after the button, with whoever is standing in the booths by then
// (so the Enchanter can press it and run into a booth): all the XP of the player the arrow points away from goes to the
// other one (XpTransfer). Placed from the middle bottom; breaking any part breaks it all (one item back).
public class TransferTubeBlock extends Block {

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    // Which way the arrow points, as you look at the front: true = from the left booth to the right one
    public static final BooleanProperty TO_RIGHT = BooleanProperty.create("to_right");
    // The button was pressed and the transfer is coming (middle bottom only)
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public static final int TRANSFER_DELAY = 40;

    // column: -1 = left booth, 0 = the console, 1 = right booth (as you look at the front)
    public enum Part implements StringRepresentable {
        LEFT_BOTTOM(-1, 0), LEFT_TOP(-1, 1), MIDDLE_BOTTOM(0, 0), MIDDLE_TOP(0, 1), RIGHT_BOTTOM(1, 0), RIGHT_TOP(1, 1);

        public final int column;
        public final int row;

        Part(int column, int row) {
            this.column = column;
            this.row = row;
        }

        public boolean booth() {
            return this.column != 0;
        }

        static @Nullable Part at(int column, int row) {
            for (Part part : values()) {
                if (part.column == column && part.row == row) return part;
            }
            return null;
        }

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    // Shapes for a tube facing north (its front, the open side of the booths, toward the north), turned for the others
    private static final VoxelShape BOOTH_BOTTOM_N = Shapes.or(box(0, 0, 0, 16, 1, 16),
        box(0, 1, 15, 16, 16, 16), box(0, 1, 0, 1, 16, 15), box(15, 1, 0, 16, 16, 15));
    private static final VoxelShape BOOTH_TOP_N = Shapes.or(box(0, 15, 0, 16, 16, 16),
        box(0, 0, 15, 16, 15, 16), box(0, 0, 0, 1, 15, 15), box(15, 0, 0, 16, 15, 15));
    private static final VoxelShape CONSOLE_N = Shapes.or(box(1, 0, 2, 15, 16, 16), box(5, 7, 0, 11, 11, 2));
    private static final VoxelShape SCREEN_N = Shapes.or(box(6, 0, 6, 10, 3, 10), box(0, 3, 7, 16, 15, 10));
    private static final Map<Direction, VoxelShape[]> SHAPES = new EnumMap<>(Direction.class);

    static {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            SHAPES.put(facing, new VoxelShape[] {turn(BOOTH_BOTTOM_N, facing), turn(BOOTH_TOP_N, facing),
                turn(CONSOLE_N, facing), turn(SCREEN_N, facing)});
        }
    }

    public TransferTubeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH)
            .setValue(PART, Part.MIDDLE_BOTTOM).setValue(TO_RIGHT, true).setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, TO_RIGHT, ACTIVE);
    }

    // ── Where the parts are ──

    // To the right of the front, as you look at it
    private static Direction right(Direction facing) {
        return facing.getCounterClockWise();
    }

    public static BlockPos partPos(BlockPos middleBottom, Direction facing, Part part) {
        return middleBottom.relative(right(facing), part.column).above(part.row);
    }

    public static BlockPos middleBottom(BlockPos pos, BlockState state) {
        Part part = state.getValue(PART);
        return pos.relative(right(state.getValue(FACING)), -part.column).below(part.row);
    }

    // ── Placing and breaking ──

    // Placed at the middle bottom (the console), facing you; needs room for all 6 parts
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos origin = context.getClickedPos();
        Level level = context.getLevel();
        if (origin.getY() + 1 > level.getMaxY()) return null;
        for (Part part : Part.values()) {
            if (!level.getBlockState(partPos(origin, facing, part)).canBeReplaced(context)) return null;
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
        Direction facing = state.getValue(FACING);
        for (Part part : Part.values()) {
            if (part == Part.MIDDLE_BOTTOM) continue;
            level.setBlock(partPos(pos, facing, part), state.setValue(PART, part), Block.UPDATE_ALL);
        }
    }

    // A part next to this one went missing: this one goes too (so the whole tube comes down)
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState,
                                     RandomSource random) {
        Direction facing = state.getValue(FACING);
        Part part = state.getValue(PART);
        int column = part.column;
        int row = part.row;
        if (directionToNeighbour == Direction.UP) row++;
        else if (directionToNeighbour == Direction.DOWN) row--;
        else if (directionToNeighbour == right(facing)) column++;
        else if (directionToNeighbour == right(facing).getOpposite()) column--;
        else return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
        Part expected = Part.at(column, row);
        if (expected != null && !(neighbourState.is(this) && neighbourState.getValue(PART) == expected
            && neighbourState.getValue(FACING) == facing)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    // The item comes back from the middle bottom (loot table); breaking any other part drops it here instead,
    // since the rest of the tube just disappears
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && !player.preventsBlockDrops() && state.getValue(PART) != Part.MIDDLE_BOTTOM) {
            popResource(level, pos, new ItemStack(ModItems.TRANSFER_TUBE.get()));
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

    // ── Shapes: the booths are open at the front, so you can walk in ──

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Part part = state.getValue(PART);
        int index = part.booth() ? part.row : 2 + part.row;
        return SHAPES.get(state.getValue(FACING))[index];
    }

    private static VoxelShape turn(VoxelShape north, Direction facing) {
        VoxelShape[] out = {Shapes.empty()};
        north.forAllBoxes((x1, y1, z1, x2, y2, z2) -> out[0] = Shapes.or(out[0], switch (facing) {
            case EAST -> Shapes.box(1 - z2, y1, x1, 1 - z1, y2, x2);
            case SOUTH -> Shapes.box(1 - x2, y1, 1 - z2, 1 - x1, y2, 1 - z1);
            case WEST -> Shapes.box(z1, y1, 1 - x2, z2, y2, 1 - x1);
            default -> Shapes.box(x1, y1, z1, x2, y2, z2);
        }));
        return out[0];
    }

    // ── Working it (Enchanters only) ──

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        Part part = state.getValue(PART);
        if (part.booth()) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        if (!player.isCreative() && !JobEvents.has(player, Job.ENCHANTER)) {
            serverPlayer.sendOverlayMessage(Component.literal("Only an Enchanter can work the Transfer Tube."));
            return InteractionResult.SUCCESS;
        }
        BlockPos origin = middleBottom(pos, state);
        BlockState console = level.getBlockState(origin);
        if (!console.is(this)) return InteractionResult.SUCCESS;
        if (part == Part.MIDDLE_TOP) {
            // flip the arrow (both middle parts keep it)
            if (console.getValue(ACTIVE)) {
                serverPlayer.sendOverlayMessage(Component.literal("A transfer is already coming."));
                return InteractionResult.SUCCESS;
            }
            boolean toRight = !state.getValue(TO_RIGHT);
            level.setBlock(origin, console.setValue(TO_RIGHT, toRight), Block.UPDATE_ALL);
            level.setBlock(pos, state.setValue(TO_RIGHT, toRight), Block.UPDATE_ALL);
            serverLevel.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6F, toRight ? 0.7F : 0.6F);
            serverPlayer.sendOverlayMessage(Component.literal("XP goes from the " + (toRight ? "left" : "right")
                + " booth to the " + (toRight ? "right" : "left") + " booth."));
            return InteractionResult.SUCCESS;
        }
        // the button
        if (console.getValue(ACTIVE)) {
            serverPlayer.sendOverlayMessage(Component.literal("A transfer is already coming."));
            return InteractionResult.SUCCESS;
        }
        level.setBlock(origin, console.setValue(ACTIVE, true), Block.UPDATE_ALL);
        serverLevel.scheduleTick(origin, this, TRANSFER_DELAY);
        serverLevel.playSound(null, origin, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.8F, 0.6F);
        serverLevel.playSound(null, origin, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        XpTransfer.preview(serverLevel, origin, console.getValue(FACING), console.getValue(TO_RIGHT));
        return InteractionResult.SUCCESS;
    }

    // TRANSFER_DELAY after the button: move the XP
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(PART) != Part.MIDDLE_BOTTOM || !state.getValue(ACTIVE)) return;
        level.setBlock(pos, state.setValue(ACTIVE, false), Block.UPDATE_ALL);
        XpTransfer.run(level, pos, state.getValue(FACING), state.getValue(TO_RIGHT));
    }
}
