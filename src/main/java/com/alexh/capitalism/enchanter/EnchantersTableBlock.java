package com.alexh.capitalism.enchanter;

import com.alexh.capitalism.occupation.Job;
import com.alexh.capitalism.occupation.JobEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
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
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

// The Enchanter's Table (Enchanter.md): a dark oak desk with rune panels, a velvet top, an open spellbook and two
// purple candles. Only Enchanters can use it (EnchantersTableMenu: take an unwanted enchantment off an item, onto a
// Book, for XP). The candles flicker and glyphs drift up off the book (animateTick).
public class EnchantersTableBlock extends Block {

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape SHAPE = Shapes.or(box(0, 0, 0, 16, 12, 16), box(3, 12, 4, 13, 13.5, 12));

    public EnchantersTableBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (!player.isCreative() && !JobEvents.has(player, Job.ENCHANTER)) {
                serverPlayer.sendOverlayMessage(Component.literal("Only an Enchanter can use the Enchanter's Table."));
                return InteractionResult.SUCCESS;
            }
            player.openMenu(new SimpleMenuProvider(
                (id, inventory, who) -> new EnchantersTableMenu(id, inventory, ContainerLevelAccess.create(level, pos)),
                Component.literal("Enchanter's Table")));
        }
        return InteractionResult.SUCCESS;
    }

    // Candle flames (back corners, as the model has them for a north-facing table) and glyphs off the book
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Direction facing = state.getValue(FACING);
        for (double[] candle : new double[][] {{2.0, 13.0}, {14.0, 13.0}}) {
            Vec3 p = turn(candle[0] / 16.0, candle[1] / 16.0, facing);
            double x = pos.getX() + p.x;
            double y = pos.getY() + 19.5 / 16.0;   // the wick tip
            double z = pos.getZ() + p.z;
            if (random.nextInt(3) == 0) level.addParticle(ParticleTypes.SMALL_FLAME, x, y, z, 0, 0, 0);
            if (random.nextInt(12) == 0) level.addParticle(ParticleTypes.SMOKE, x, y + 0.05, z, 0, 0.01, 0);
        }
        if (random.nextInt(2) == 0) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.5;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.5;
            level.addParticle(ParticleTypes.ENCHANT, x, pos.getY() + 1.6, z, (random.nextDouble() - 0.5) * 0.3,
                -0.6 - random.nextDouble() * 0.3, (random.nextDouble() - 0.5) * 0.3);
        }
    }

    // A point on a north-facing table (x, z from 0 to 1), turned like the model is for this facing
    private static Vec3 turn(double x, double z, Direction facing) {
        return switch (facing) {
            case EAST -> new Vec3(1 - z, 0, x);
            case SOUTH -> new Vec3(1 - x, 0, 1 - z);
            case WEST -> new Vec3(z, 0, 1 - x);
            default -> new Vec3(x, 0, z);
        };
    }
}
