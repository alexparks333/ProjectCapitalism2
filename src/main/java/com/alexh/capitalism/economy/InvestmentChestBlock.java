package com.alexh.capitalism.economy;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import org.jspecify.annotations.Nullable;

// The Investment Chest (Economy.md > The Investment Chest): the bank's gold-plated vault chest, a crooked hanging sign
// on two posts above it ("INVESTMENT CHEST", up into the block above: client/InvestmentChestRenderer). Right-click: YOUR
// account (InvestmentBank), the same from every Investment Chest, nobody else's ever shown. Not craftable: an op in
// creative puts it in the bank; nothing breaks it in survival (it holds nothing itself, so breaking it loses nobody's
// gold either). Explosions and pistons don't touch it.
@EventBusSubscriber(modid = Capitalism.MODID)
public class InvestmentChestBlock extends BaseEntityBlock {

    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 14, 15);
    public static final Component TITLE = Component.literal("Investment Chest");

    public InvestmentChestBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HorizontalDirectionalBlock.FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, context.getHorizontalDirection().getOpposite());
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
        return new InvestmentChestBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? createTickerHelper(type, ModBlockEntities.INVESTMENT_CHEST.get(), InvestmentChestBlockEntity::lidAnimateTick) : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof InvestmentChestBlockEntity chest)) return InteractionResult.PASS;
        open(serverPlayer, chest);
        return InteractionResult.SUCCESS;
    }

    // their own account, nobody else's
    public static void open(ServerPlayer player, @Nullable InvestmentChestBlockEntity chest) {
        InvestmentBank.Account account = InvestmentBank.get(player.level().getServer()).account(player.getUUID());
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new InvestmentMenu(id, inventory, account,
            InvestmentMenu.liveData(account, player.level().getServer()), chest), TITLE));
    }

    // nobody breaks it but an op in creative
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBreak(BreakBlockEvent event) {
        if (!(event.getState().getBlock() instanceof InvestmentChestBlock)) return;
        Player player = event.getPlayer();
        if (player != null && player.isCreative() && player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) return;
        event.setCanceled(true);
        if (player instanceof ServerPlayer sp) sp.sendOverlayMessage(Component.literal("The bank would like its chest to stay where it is."));
    }
}
