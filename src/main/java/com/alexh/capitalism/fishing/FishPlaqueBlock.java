package com.alexh.capitalism.fishing;

import com.alexh.capitalism.core.ModBlocks;
import com.alexh.capitalism.core.ModComponents;
import com.alexh.capitalism.decor.BloodPartBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

// A Fish Plaque (Fishing.md > Plaques): a rare catch the Fisherman mounted, hung on the side of a solid block, facing out
// of it. It shows the catch on a polished wooden board with a brass nameplate (its name, weight, who caught it and when:
// client/FishPlaqueRenderer). The big ones run wider along the wall (RareCatch.plaque: the
// Mako, the Swordfish and the Kraken Tentacle 2 blocks, to the right as you face them; the Oarfish 3, centred), the extra blocks part blocks (decor/BloodPartBlock).
// Breaking any of it drops the plaque, catch and story kept (its loot table copies them from the block entity).
public class FishPlaqueBlock extends BaseEntityBlock implements BloodPartBlock.Owner {

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    // a board against the wall (facing west: its back at +x)
    private static final VoxelShape BOARD = Block.box(13, 2, 0, 16, 14, 16);
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        for (Direction d : Direction.Plane.HORIZONTAL) SHAPES.put(d, com.alexh.capitalism.decor.BloodDecorBlock.turn(BOARD, d));
    }

    public FishPlaqueBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    // how many blocks wide a plaque item makes
    public static int widthOf(ItemStack stack) {
        RareCatch kind = RareCatch.byId(stack.getOrDefault(ModComponents.PLAQUE_CATCH.get(), ""));
        return kind == null ? 1 : Math.max(1, kind.plaque);
    }

    public static int widthOf(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof FishPlaqueBlockEntity be && be.kind() != null ? Math.max(1, be.kind().plaque) : 1;
    }

    // where a plaque `width` wide starts, in blocks to the right of its main block: a 3-wide one is centred on it (part
    // blocks reach one block from their main one), a 2-wide one runs one block to the right
    public static int start(int width) {
        return width >= 3 ? -1 : 0;
    }

    // the block `i` along from the main one (to the right as you face it; negative: to the left)
    public static BlockPos cell(BlockPos main, Direction front, int i) {
        return main.relative(front.getCounterClockWise(), i);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        if (!face.getAxis().isHorizontal()) return null;
        BlockState state = this.defaultBlockState().setValue(FACING, face);
        Level level = context.getLevel();
        int width = widthOf(context.getItemInHand());
        for (int i = start(width); i < start(width) + width; i++) {
            BlockPos p = cell(context.getClickedPos(), face, i);
            BlockPos wall = p.relative(face.getOpposite());
            boolean free = i == 0 || level.getBlockState(p).canBeReplaced(context);
            if (!free || !level.getBlockState(wall).isFaceSturdy(level, wall, face)) {
                if (context.getPlayer() != null && !level.isClientSide()) {
                    context.getPlayer().sendOverlayMessage(Component.literal("This plaque needs " + width + " blocks of wall, free, side by side.")
                        .withStyle(ChatFormatting.RED));
                }
                return null;
            }
        }
        return state;
    }

    // placed: the catch goes onto the block entity (from the item's components, vanilla), the rest of its width fills
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
        super.setPlacedBy(level, pos, state, by, stack);
        if (level.isClientSide()) return;
        int width = widthOf(stack);
        BlockState part = ModBlocks.BLOOD_DECOR_PART.get().defaultBlockState();
        for (int i = start(width); i < start(width) + width; i++) {
            if (i == 0) continue;
            BlockPos p = cell(pos, state.getValue(FACING), i);
            level.setBlock(p, BloodPartBlock.pointingAt(part, p, pos), 3);
        }
    }

    // gone: its other blocks go too
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        for (int i = -1; i <= 1; i += 2) {
            BlockPos p = cell(pos, state.getValue(FACING), i);
            BlockState there = level.getBlockState(p);
            if (there.getBlock() instanceof BloodPartBlock && BloodPartBlock.mainOf(there, p).equals(pos)) level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    // it needs its wall
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction out = state.getValue(FACING);
        BlockPos wall = pos.relative(out.getOpposite());
        return level.getBlockState(wall).isFaceSturdy(level, wall, out);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState()
            : super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    public VoxelShape partShape(BlockState main, BlockGetter level, BlockPos mainPos, BlockPos partPos) {
        return main.hasProperty(FACING) ? SHAPES.get(main.getValue(FACING)) : Shapes.empty();
    }

    // (client) a Mythic catch's plaque gives off faint embers and glints, drifting up off its board
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) != 0) return;
        if (!(level.getBlockEntity(pos) instanceof FishPlaqueBlockEntity be) || be.kind() == null || be.kind().tier() != CatchTier.MYTHIC) return;
        Direction out = state.getValue(FACING);
        double x = pos.getX() + 0.5 - out.getStepX() * 0.3 + (random.nextDouble() - 0.5) * 0.9 * Math.abs(out.getStepZ());
        double z = pos.getZ() + 0.5 - out.getStepZ() * 0.3 + (random.nextDouble() - 0.5) * 0.9 * Math.abs(out.getStepX());
        double y = pos.getY() + 0.2 + random.nextDouble() * 0.8;
        level.addParticle(random.nextBoolean() ? net.minecraft.core.particles.ParticleTypes.SMALL_FLAME : net.minecraft.core.particles.ParticleTypes.WAX_ON,
            x, y, z, 0.0, 0.01 + random.nextDouble() * 0.02, 0.0);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FishPlaqueBlockEntity(pos, state);
    }
}
