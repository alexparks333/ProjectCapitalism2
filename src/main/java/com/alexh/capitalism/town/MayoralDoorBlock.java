package com.alexh.capitalism.town;

import com.alexh.capitalism.event.EventSwitchBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

// The Mayoral Door (Town.md > The Town Hall): a grand door that only the MAYOR can open or close (town/TownHall: whoever
// holds the office now). It's a real door: two side by side hinge as a double door, and opening one swings its partner too.
// Nothing else opens it: no redstone (buttons, plates, levers do nothing), no villagers, no zombies breaking it down (it's
// a metal door, like iron). Anyone else gets "Only the Mayor can open this door."
public class MayoralDoorBlock extends DoorBlock {

    public MayoralDoorBlock(Properties properties) {
        super(BlockSetType.GOLD, properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        if (!EventSwitchBlock.isMayor(sp)) {
            sp.sendOverlayMessage(Component.literal("Only the Mayor can open this door.").withStyle(ChatFormatting.RED));
            level.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.6F, 1.6F);
            return InteractionResult.FAIL;
        }
        boolean open = !state.getValue(OPEN);
        this.setOpen(player, level, state, pos, open);
        // its partner of a double door: beside it, same facing, the other hinge, the same half
        Direction facing = state.getValue(FACING);
        for (Direction side : new Direction[]{facing.getClockWise(), facing.getCounterClockWise()}) {
            BlockPos next = pos.relative(side);
            BlockState other = level.getBlockState(next);
            if (other.is(this) && other.getValue(FACING) == facing && other.getValue(HINGE) != state.getValue(HINGE)
                && other.getValue(HALF) == state.getValue(HALF)) {
                this.setOpen(player, level, other, next, open);
            }
        }
        return InteractionResult.SUCCESS;
    }

    // no redstone: a button, plate or lever never opens it
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
    }
}
