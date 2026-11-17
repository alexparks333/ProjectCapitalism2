package com.alexh.capitalism.town;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import org.jspecify.annotations.Nullable;

// The Mayoral Chest (Town.md > The Town Hall): a grand chest of dark mahogany banded with gold, the town's crest on its
// front (client/MayoralChestRenderer). 54 slots; only the Mayor and the Vice Mayor open it (whoever holds the office
// now; an op in creative too); anyone else hears it locked. Not craftable: an op in creative puts it down; nothing breaks
// it in survival (not even its officials), only an op in creative, and then what's inside drops. Explosions and pistons
// don't touch it; hoppers can't reach inside (MayoralChestBlockEntity). (It carries the chest's TYPE, always single, so
// the chest's lid and sounds work.)
@EventBusSubscriber(modid = Capitalism.MODID)
public class MayoralChestBlock extends BaseEntityBlock {

    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 14, 15);

    public MayoralChestBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.SINGLE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ChestBlock.FACING, ChestBlock.TYPE);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(ChestBlock.FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MayoralChestBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? createTickerHelper(type, ModBlockEntities.MAYORAL_CHEST.get(), ChestBlockEntity::lidAnimateTick) : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof MayoralChestBlockEntity chest)) return InteractionResult.PASS;
        if (!MayoralChestBlockEntity.mayOpen(serverPlayer)) {
            serverPlayer.sendOverlayMessage(MayoralChestBlockEntity.deniedMessage());
            level.playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1.0F, 0.8F);
            return InteractionResult.SUCCESS;
        }
        serverPlayer.openMenu(chest);
        // a regal creak under the lid's own sound
        level.playSound(null, pos, SoundEvents.WOODEN_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.6F, 0.55F);
        level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.4F, 0.7F);
        return InteractionResult.SUCCESS;
    }

    // nobody breaks it but an op in creative (survival can't anyway: it's as hard as bedrock)
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBreak(BreakBlockEvent event) {
        if (!(event.getState().getBlock() instanceof MayoralChestBlock)) return;
        Player player = event.getPlayer();
        if (player != null && player.isCreative() && player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) return;
        event.setCanceled(true);
        if (player instanceof ServerPlayer sp) sp.sendOverlayMessage(net.minecraft.network.chat.Component.literal("The Mayoral Chest can't be broken."));
    }
}
