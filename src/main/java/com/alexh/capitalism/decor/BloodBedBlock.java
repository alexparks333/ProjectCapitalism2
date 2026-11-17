package com.alexh.capitalism.decor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.OptionalDouble;

// A Blood Moon decoration you sleep in like a bed (decor/BloodDecorKind.bed, Decorations.md): the Vampire Coffin and the
// Four-Poster Crimson Bed. Two blocks long, placed like a bed (the foot where you click, the head beyond it); FACING
// points from the foot to the head. Only the head has a block entity: client/BloodDecorRenderer draws the whole thing
// from it, centred on the line between the two blocks. Sleeping is vanilla's (it skips the night, sets your spawn;
// no sleeping through a Blood Moon: event/BloodMoon).
//  - The coffin: right-click opens its lid; right-click it open to lie down in it; sneak + right-click (empty-handed)
//    closes it. You lie inside it (low down), not on top.
//  - The canopy bed: right-click to sleep; its curtains draw shut while someone sleeps in it (OCCUPIED).
public class BloodBedBlock extends AbstractBedBlock implements EntityBlock, BloodDecorKind.Holder {

    public static final BooleanProperty OPEN = BloodDecorBlock.OPEN;

    private final BloodDecorKind kind;

    public BloodBedBlock(BloodDecorKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, BedPart.FOOT)
            .setValue(OCCUPIED, false).setValue(OPEN, false));
    }

    @Override
    public BloodDecorKind kind() {
        return this.kind;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, OCCUPIED, OPEN);
    }

    @Override
    protected EnvironmentAttribute<BedRule> getBedEnvironmentAttribute() {
        return EnvironmentAttributes.BED_RULE;
    }

    // the coffin: you can lie down in it by day too (a vampire's bed; sleeping through the day takes you to dusk:
    // BloodDecorEvents), and it always sets your spawn; where beds blow up, so does it
    @Override
    public BedRule getBedRule(Level level, BlockPos pos) {
        BedRule rule = super.getBedRule(level, pos);
        if (this.kind != BloodDecorKind.VAMPIRE_COFFIN || rule.destroyOnUse()) return rule;
        return new BedRule(BedRule.Rule.ALWAYS, BedRule.Rule.ALWAYS, false, false, java.util.Optional.empty());
    }

    // where beds blow up (the Nether, the End), so does this
    @Override
    protected InteractionResult destroyOnUse(BlockState state, Level level, BlockPos pos, Player player) {
        level.removeBlock(pos, false);
        BlockPos other = pos.relative(state.getValue(FACING).getOpposite());
        if (level.getBlockState(other).is(this)) level.removeBlock(other, false);
        Vec3 boom = Vec3.atCenterOf(pos);
        level.explode(null, level.damageSources().badRespawnPointExplosion(boom), null, boom, 5.0F, true, Level.ExplosionInteraction.BLOCK);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected void destroyOnLeave(Level level, BlockPos pos) {
    }

    // you lie inside the coffin (only while it's open), on the canopy bed's mattress
    @Override
    public OptionalDouble getSleepHeight(BlockState state, Level level, BlockPos pos) {
        if (!state.is(this)) return OptionalDouble.empty();
        if (this.kind == BloodDecorKind.VAMPIRE_COFFIN) return state.getValue(OPEN) ? OptionalDouble.of(0.0) : OptionalDouble.empty();
        return OptionalDouble.of(0.5);
    }

    public static BlockPos headOf(BlockState state, BlockPos pos) {
        return state.getValue(PART) == BedPart.HEAD ? pos : pos.relative(state.getValue(FACING));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (this.kind != BloodDecorKind.VAMPIRE_COFFIN) return super.useWithoutItem(state, level, pos, player, hit);
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        BlockPos headPos = headOf(state, pos);
        BlockState head = level.getBlockState(headPos);
        if (!head.is(this)) return InteractionResult.CONSUME;
        boolean open = head.getValue(OPEN);
        if (player.isSecondaryUseActive()) {
            // sneak + right-click: shut it (not with someone in it)
            if (open && !head.getValue(OCCUPIED)) setOpen(level, headPos, head, false);
            return InteractionResult.SUCCESS_SERVER;
        }
        if (!open) {
            setOpen(level, headPos, head, true);
            player.sendOverlayMessage(Component.literal("Right-click again to lie down; sneak + right-click to close the lid."));
            return InteractionResult.SUCCESS_SERVER;
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    static void setOpen(Level level, BlockPos head, BlockState state, boolean open) {
        level.setBlock(head, state.setValue(OPEN, open), 3);
        level.playSound(null, head, open ? SoundEvents.WOODEN_TRAPDOOR_OPEN : SoundEvents.WOODEN_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.9F, 0.55F);
        if (open) level.playSound(null, head, SoundEvents.BAT_AMBIENT, SoundSource.BLOCKS, 0.25F, 0.8F);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        // the canopy bed needs the blocks over both halves free too
        if (state != null && this.kind.tall > 1 && !BloodDecorBlock.roomFor(this.kind, context.getLevel(), context.getClickedPos(),
            state.getValue(FACING).getOpposite(), context)) {
            if (context.getPlayer() != null && !context.getLevel().isClientSide()) {
                context.getPlayer().sendOverlayMessage(Component.literal("There isn't room for the " + this.kind.title + " here (it's 2 long and 2 tall).")
                    .withStyle(net.minecraft.ChatFormatting.RED));
            }
            return null;
        }
        return state == null ? null : state.setValue(OPEN, false);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        BlockState out = super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
        return out.is(this) && state.getValue(PART) == BedPart.FOOT && neighbourState.is(this) && neighbourPos.equals(pos.relative(state.getValue(FACING)))
            ? out.setValue(OPEN, neighbourState.getValue(OPEN)) : out;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    // placed: the head half (vanilla), then the canopy bed's upper blocks over both halves
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.@Nullable LivingEntity by, net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, by, stack);
        if (!level.isClientSide() && this.kind.tall > 1) BloodDecorBlock.placeParts(level, pos, state.getValue(FACING).getOpposite(), this.kind, pos);
    }

    // gone: the upper block over this half goes with it
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        BlockPos up = pos.above();
        BlockState there = level.getBlockState(up);
        if (there.getBlock() instanceof BloodPartBlock && BloodPartBlock.mainOf(there, up).equals(pos)) level.setBlock(up, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // its slice of the whole bed's shape (counted from its foot)
        Direction front = state.getValue(FACING).getOpposite();
        return this.kind.cellShape(state.getValue(PART) == BedPart.HEAD ? 1 : 0, 0, 0, front);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == BedPart.HEAD ? new BloodDecorBlockEntity(pos, state) : null;
    }
}
