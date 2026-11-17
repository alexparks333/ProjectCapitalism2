package com.alexh.capitalism.event;

import com.alexh.capitalism.town.TownHall;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

// The Purge Switch and the Blood Moon Switch (Events.md > The switches): nobody can craft them (an admin hands them out).
// A big lever on a heavy plate. Only the MAYOR can throw one (town/TownHall: whoever holds the office right now):
//  - the Purge Switch: The Purge is announced for dusk tomorrow, like a normal Purge (Purge.schedule);
//  - the Blood Moon Switch: a Blood Moon rises at dusk tonight (right away if it's already night; BloodMoon.scheduleTonight).
// Thrown, it stays down (ON) until that event is over, then springs back up, ready again: no spamming. It checks every
// few seconds. If that event is already on or coming, it says so and stays up. (The Purge and a Blood Moon CAN share a night.)
public class EventSwitchBlock extends Block {

    public enum Kind { PURGE, BLOOD_MOON }

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty ON = BlockStateProperties.POWERED;
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 10, 13);
    private static final int CHECK_TICKS = 100;

    private final Kind kind;

    public EventSwitchBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ON, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ON);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    public static boolean isMayor(ServerPlayer player) {
        return TownHall.get(player.level().getServer()).officeOf(player.getUUID()) == TownHall.Office.MAYOR;
    }

    // is this switch's event still on or coming?
    private boolean pending(ServerLevel level) {
        var server = level.getServer();
        return switch (this.kind) {
            case PURGE -> Purge.isActive() || Purge.data(server).night >= 0;
            case BLOOD_MOON -> BloodMoon.isActive() || BloodMoon.ticksUntil(server) >= 0;
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        String what = this.kind == Kind.PURGE ? "The Purge" : "a Blood Moon";
        if (!isMayor(sp)) {
            sp.sendOverlayMessage(Component.literal("Only the Mayor can throw this switch.").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        if (state.getValue(ON)) {
            sp.sendOverlayMessage(Component.literal("It's already thrown: it springs back once " + what + " is over.").withStyle(ChatFormatting.GRAY));
            return InteractionResult.FAIL;
        }
        boolean ok = this.kind == Kind.PURGE ? Purge.schedule(server.getServer()) : BloodMoon.scheduleTonight(server.getServer());
        if (!ok) {
            sp.sendOverlayMessage(Component.literal("Not now: " + what + " is already on or coming.").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        level.setBlock(pos, state.setValue(ON, true), Block.UPDATE_ALL);
        server.scheduleTick(pos, this, CHECK_TICKS);
        server.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 1.0F, 0.5F);
        server.playSound(null, pos, this.kind == Kind.PURGE ? SoundEvents.BELL_BLOCK : SoundEvents.WITHER_AMBIENT, SoundSource.BLOCKS, 1.0F, 0.5F);
        server.sendParticles(new DustParticleOptions(this.kind == Kind.PURGE ? 0xD01010 : 0x8A0A1A, 1.5F),
            pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 30, 0.3, 0.3, 0.3, 0.0);
        sp.sendSystemMessage(Component.literal(this.kind == Kind.PURGE ? "You threw the Purge Switch. The Purge begins at dusk tomorrow."
            : "You threw the Blood Moon Switch. A Blood Moon rises at dusk tonight.").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        return InteractionResult.SUCCESS;
    }

    // while thrown: back up once the event is over
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ON)) return;
        if (this.pending(level)) {
            level.scheduleTick(pos, this, CHECK_TICKS);
            return;
        }
        level.setBlock(pos, state.setValue(ON, false), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.8F, 0.8F);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (level instanceof ServerLevel server && state.getValue(ON)) server.scheduleTick(pos, this, CHECK_TICKS);
    }
}
