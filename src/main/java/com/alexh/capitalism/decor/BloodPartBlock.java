package com.alexh.capitalism.decor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

// One of the extra blocks a big Blood Moon decoration fills (decor/BloodDecorKind: the organ, the fountain, the clock...;
// the canopy bed's upper blocks). It draws nothing; it holds its slice of the piece's solid shape and points back at the
// main block (DX, DY, DZ: the main block is this one minus (DX - 1, DY, DZ - 1)). Using it uses the piece. Breaking it
// breaks the whole piece, which drops its one item (nothing in Creative); the main one going takes all its parts with it.
public class BloodPartBlock extends Block {

    public static final IntegerProperty DX = IntegerProperty.create("dx", 0, 2);
    public static final IntegerProperty DY = IntegerProperty.create("dy", 0, 1);
    public static final IntegerProperty DZ = IntegerProperty.create("dz", 0, 2);

    public BloodPartBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(DX, 1).setValue(DY, 1).setValue(DZ, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DX, DY, DZ);
    }

    // a part at `pos` whose main block is at `main`
    public static BlockState pointingAt(BlockState part, BlockPos pos, BlockPos main) {
        return part.setValue(DX, pos.getX() - main.getX() + 1).setValue(DY, pos.getY() - main.getY()).setValue(DZ, pos.getZ() - main.getZ() + 1);
    }

    public static BlockPos mainOf(BlockState state, BlockPos pos) {
        return pos.offset(-(state.getValue(DX) - 1), -state.getValue(DY), -(state.getValue(DZ) - 1));
    }

    // Another block that fills more than one block with these parts (the Fisherman's wall plaques): it gives each
    // part's shape itself
    public interface Owner {
        VoxelShape partShape(BlockState main, BlockGetter level, BlockPos mainPos, BlockPos partPos);
    }

    // is its main block still there (a piece, or another Owner)?
    static boolean hasMain(BlockGetter level, BlockPos main) {
        var block = level.getBlockState(main).getBlock();
        return block instanceof BloodDecorKind.Holder || block instanceof Owner;
    }

    // the piece this belongs to, or null (its main block gone)
    static @Nullable BloodDecorKind kindOf(BlockGetter level, BlockPos main) {
        return level.getBlockState(main).getBlock() instanceof BloodDecorKind.Holder h ? h.kind() : null;
    }

    // the way the piece faces (its front)
    static Direction frontOf(BlockState main) {
        if (main.getBlock() instanceof BloodBedBlock) return main.getValue(BloodBedBlock.FACING).getOpposite();
        return main.hasProperty(HorizontalDirectionalBlock.FACING) ? main.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
    }

    // where the piece's footprint is counted from: the bed's foot (its main one for parts is the block under them)
    static BlockPos originOf(BlockState main, BlockPos mainPos) {
        if (main.getBlock() instanceof BloodBedBlock && main.getValue(BloodBedBlock.PART) == net.minecraft.world.level.block.state.properties.BedPart.HEAD) {
            return mainPos.relative(main.getValue(BloodBedBlock.FACING).getOpposite());
        }
        return mainPos;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        BlockPos main = mainOf(state, pos);
        BlockState mainState = level.getBlockState(main);
        if (mainState.getBlock() instanceof Owner owner) return owner.partShape(mainState, level, main, pos);
        if (!(mainState.getBlock() instanceof BloodDecorKind.Holder h)) return Shapes.empty();
        Direction front = frontOf(mainState);
        int[] c = BloodDecorKind.cellOf(originOf(mainState, main), front, pos);
        if (mainState.getBlock() instanceof BloodDecorBlock block) return block.shapeAt(mainState, c[0], c[1], c[2], false);
        return h.kind().cellShape(c[0], c[1], c[2], front);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        BlockPos main = mainOf(state, pos);
        BlockState mainState = level.getBlockState(main);
        if (mainState.getBlock() instanceof BloodDecorBlock block) {
            int[] c = BloodDecorKind.cellOf(main, frontOf(mainState), pos);
            return block.shapeAt(mainState, c[0], c[1], c[2], true);
        }
        return this.getShape(state, level, pos, context);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    // using it uses the piece
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos main = mainOf(state, pos);
        BlockState mainState = level.getBlockState(main);
        if (!hasMain(level, main)) return InteractionResult.PASS;
        return mainState.useWithoutItem(level, player, hit.withPosition(main));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockPos main = mainOf(state, pos);
        BlockState mainState = level.getBlockState(main);
        if (!hasMain(level, main)) return InteractionResult.PASS;
        return mainState.useItemOn(stack, level, player, hand, hit.withPosition(main));
    }

    // broken in Creative: the piece goes without dropping anything
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.preventsBlockDrops()) {
            BlockPos main = mainOf(state, pos);
            if (hasMain(level, main)) {
                BlockState mainState = level.getBlockState(main);
                level.setBlock(main, Blocks.AIR.defaultBlockState(), 35);
                level.levelEvent(player, 2001, main, Block.getId(mainState));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    // gone (broken, blown up): the whole piece goes, dropping its item (from its main block)
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        BlockPos main = mainOf(state, pos);
        if (hasMain(level, main)) level.destroyBlock(main, true);
    }

    @Override
    protected boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) {
        return false;
    }
}
