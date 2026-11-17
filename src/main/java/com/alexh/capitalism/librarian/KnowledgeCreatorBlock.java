package com.alexh.capitalism.librarian;

import com.alexh.capitalism.core.ModBlockEntities;
import com.alexh.capitalism.occupation.Job;
import com.alexh.capitalism.occupation.JobEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

// The Knowledge Creator (Librarian.md): a dark deepslate pedestal with bookshelf sides and amethyst on its corners, and a
// glass bell jar on top. Only Librarians can craft and use it. Put an Enchanted Book in and in 30 seconds it's a
// Knowledge Piece (KnowledgeCreatorBlockEntity); the book floats in the jar while it works (client/KnowledgeCreatorRenderer).
// WORKING while it makes one: it glows a little, and glyphs swirl down into the jar.
public class KnowledgeCreatorBlock extends BaseEntityBlock {

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty WORKING = BooleanProperty.create("working");
    private static final VoxelShape SHAPE = Shapes.or(box(0, 0, 0, 16, 9, 16), box(3, 9, 3, 13, 16, 13));

    public KnowledgeCreatorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WORKING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WORKING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KnowledgeCreatorBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof KnowledgeCreatorBlockEntity creator) {
            if (!player.isCreative() && !JobEvents.has(player, Job.LIBRARIAN)) {
                serverPlayer.sendOverlayMessage(Component.literal("Only a Librarian can use the Knowledge Creator."));
                return InteractionResult.SUCCESS;
            }
            player.openMenu(creator);
        }
        return InteractionResult.SUCCESS;
    }

    // While it works: enchanting glyphs swirl in from around the jar and fall into it, and a faint glow rises
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(WORKING)) return;
        for (int i = 0; i < 2; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double x = pos.getX() + 0.5 + Math.cos(angle) * 0.9;
            double z = pos.getZ() + 0.5 + Math.sin(angle) * 0.9;
            level.addParticle(ParticleTypes.ENCHANT, x, pos.getY() + 1.4 + random.nextDouble() * 0.4, z,
                (pos.getX() + 0.5 - x) * 1.2, -0.6, (pos.getZ() + 0.5 - z) * 1.2);
        }
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.3, pos.getY() + 0.65,
                pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.3, 0, 0.01, 0);
        }
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level instanceof ServerLevel serverLevel
            ? createTickerHelper(type, ModBlockEntities.KNOWLEDGE_CREATOR.get(),
                (innerLevel, pos, innerState, entity) -> KnowledgeCreatorBlockEntity.serverTick(serverLevel, pos, innerState, entity))
            : null;
    }
}
