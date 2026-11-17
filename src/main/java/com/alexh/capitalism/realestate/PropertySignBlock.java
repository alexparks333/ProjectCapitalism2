package com.alexh.capitalism.realestate;

import com.alexh.capitalism.economy.GoldMoney;
import com.alexh.capitalism.town.TownProtection;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

// The real estate signs (RealEstate.md): a board on a post, put up on a lot (the lot is wherever the sign stands).
//  - For Sale by the City (CITY): only ops put them up, to say a lot is for sale; it's bought at its City Chest
//    (CityChestBlockEntity: a Real Estate Agent puts in what the city wants and the gold).
//  - For Sale (SALE) and For Lease (LEASE): agents make them and put them up on lots they own; they show the asking
//    price / the rent a day (/properties <name> price | rent). The sale or lease itself is a command.
// Anyone right-clicking one is told about the lot.
public class PropertySignBlock extends Block {

    public enum Kind {
        CITY("For Sale by the City"),
        SALE("For Sale"),
        LEASE("For Lease");

        public final String title;

        Kind(String title) {
            this.title = title;
        }
    }

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);

    public final Kind kind;

    public PropertySignBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    // Who may put one up: city signs ops only; For Sale and For Lease the agent who owns the lot it's on
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Player player = context.getPlayer();
        if (player instanceof ServerPlayer serverPlayer) {
            String wrong = this.cantPlace(serverPlayer, context.getLevel(), context.getClickedPos());
            if (wrong != null) {
                serverPlayer.sendOverlayMessage(Component.literal(wrong));
                return null;
            }
        }
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private @Nullable String cantPlace(ServerPlayer player, Level level, BlockPos pos) {
        if (this.kind == Kind.CITY) return TownProtection.canBuild(player) ? null : "Only the city (an op) puts up these signs.";
        if (!com.alexh.capitalism.realestate.Properties.isAgent(player)) return "Only a Real Estate Agent can put up this sign.";
        Property p = com.alexh.capitalism.realestate.Properties.at(level, pos);
        if (p == null || !p.ownedBy(player)) return "Put it up on a lot you own.";
        return null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;
        var data = com.alexh.capitalism.realestate.Properties.get(serverLevel.getServer());
        Property p = level.dimension() == Level.OVERWORLD ? data.at(pos) : null;
        if (p == null) {
            serverPlayer.sendSystemMessage(Component.literal("[Real Estate] This sign isn't on a lot.").withStyle(ChatFormatting.GRAY));
            return InteractionResult.SUCCESS;
        }
        serverPlayer.sendSystemMessage(Component.literal("[Real Estate] ").withStyle(ChatFormatting.DARK_GREEN)
            .append(Component.literal(p.name + " (" + p.width() + " x " + p.length() + "): ").withStyle(ChatFormatting.GOLD))
            .append(Component.literal(this.describe(p)).withStyle(ChatFormatting.WHITE)));
        return InteractionResult.SUCCESS;
    }

    private String describe(Property p) {
        return switch (this.kind) {
            case CITY -> p.cityOwned()
                ? "For sale by the city. A Real Estate Agent buys it at its City Chest: what the city wants and the gold."
                : com.alexh.capitalism.realestate.Properties.status(p) + ".";
            case SALE -> p.salePrice() >= 0
                ? "For sale: " + GoldMoney.format(p.salePrice()) + ". Talk to " + p.ownerName() + "."
                : "For sale: ask " + p.ownerName() + ".";
            case LEASE -> p.leased() ? "Leased to " + p.tenantName() + "."
                : p.rent() >= 0 ? "For lease: " + GoldMoney.format(p.rent()) + " a day. Talk to " + p.ownerName() + "."
                : "For lease: ask " + p.ownerName() + ".";
        };
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
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
