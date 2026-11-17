package com.alexh.capitalism.realestate;

import com.alexh.capitalism.town.TownProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

// The City Chest (CityChestBlockEntity): a navy strongbox banded with gold and stamped with the city's crest. Only an
// op in creative can put one down, and only on a lot; only a Real Estate Agent can open it (an op in creative opens it
// to set it up).
public class CityChestBlock extends BaseEntityBlock {

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    public CityChestBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        if (context.getPlayer() instanceof ServerPlayer player) {
            String wrong = !(TownProtection.canBuild(player) && player.isCreative()) ? "Only an op in creative can put down a City Chest."
                : com.alexh.capitalism.realestate.Properties.at(context.getLevel(), context.getClickedPos()) == null ? "Put the City Chest on a lot."
                : null;
            if (wrong != null) {
                player.sendOverlayMessage(Component.literal(wrong));
                return null;
            }
        }
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof CityChestBlockEntity chest) {
            boolean setup = TownProtection.canBuild(player) && player.isCreative();
            if (!setup && !com.alexh.capitalism.realestate.Properties.isAgent(player)) {
                serverPlayer.sendOverlayMessage(Component.literal("Only a Real Estate Agent can open the City Chest."));
                return InteractionResult.SUCCESS;
            }
            Property lot = chest.lot();
            if (lot == null || !lot.cityOwned()) {
                serverPlayer.sendOverlayMessage(Component.literal(lot == null ? "This City Chest isn't on a lot." : lot.name + " isn't city land any more."));
                if (!setup) return InteractionResult.SUCCESS;
            }
            player.openMenu(chest);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CityChestBlockEntity(pos, state);
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
