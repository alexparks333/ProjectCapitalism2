package com.alexh.capitalism.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

import java.util.List;

// The Cook's two "put it together" tables. Right-click (Cooks only, see JobEvents) to open the menu.
//  - SUSHI:   4 ingredient slots in a row; hold the ROLL button to roll them into sushi
//  - COOKING: 5 ingredient slots around a plate -> an uncooked dish for the Oven
public class AssemblyTableBlock extends BaseEntityBlock {

    public enum Kind {
        SUSHI("sushi_table", 4, true),
        COOKING("cooking_table", 5, false);

        public final String id;
        // ingredient slots
        public final int slots;
        // true = made by holding the ROLL button, into an output slot kept in the table (Sushi);
        // false = the dish appears in the result slot straight away, like a crafting table
        public final boolean rolled;

        Kind(String id, int slots, boolean rolled) {
            this.id = id;
            this.slots = slots;
            this.rolled = rolled;
        }

        // ingredients, plus the output slot for rolled dishes
        public int containerSize() {
            return this.slots + (this.rolled ? 1 : 0);
        }

        public List<KitchenRecipes.Assembly> recipes() {
            return this == SUSHI ? KitchenRecipes.SUSHI : KitchenRecipes.COOKING;
        }
    }

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    private final Kind kind;

    public AssemblyTableBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public Kind kind() {
        return this.kind;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AssemblyTableBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof AssemblyTableBlockEntity table) {
            player.openMenu(table);
        }
        return InteractionResult.SUCCESS;
    }
}
