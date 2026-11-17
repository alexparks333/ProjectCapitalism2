package com.alexh.capitalism.decor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

// A seasonal decoration (Decorations.md: the Grand Christmas Tree, String Light Hooks, the Spooky Scarecrow, the
// Witch's Cauldron, a Big Pumpkin Pile, a Leaf Pile). Placed once, it stays where it is all year: UP in its holiday's
// days (DecorKind.Holiday, or /decorations up), PACKED AWAY the rest of the time.
//  - Up, it's drawn by client/DecorRenderer (its block model is empty), has its own solid footprint and may light the
//    area round it.
//  - Packed away, there's nothing there: nothing drawn, nothing to bump into or click, no light. Holding any
//    decoration you see a small marker where each packed-away one is, to find it (and break it to move it).
//  - Going up or coming down happens a moment after it's due, each at its own moment (its spot decides), so they
//    go up one by one round the town, with a sparkle (snow for Christmas, leaves for the harvest, purple for Halloween)
//    and a chime, or a puff and a rustle when packed away (SeasonalDecorBlockEntity.serverTick).
public class SeasonalDecorBlock extends BaseEntityBlock {

    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape MARKER = Block.box(5, 0, 5, 11, 6, 11);
    private static final VoxelShape HOOK_MARKER = Block.box(6, 6, 6, 10, 10, 10);

    private final DecorKind kind;
    private final VoxelShape upShape;
    private final int light;

    public SeasonalDecorBlock(DecorKind kind, VoxelShape upShape, int light, Properties properties) {
        super(properties);
        this.kind = kind;
        this.upShape = upShape;
        this.light = light;
        this.registerDefaultState(this.stateDefinition.any().setValue(UP, false).setValue(FACING, Direction.NORTH));
    }

    public DecorKind kind() {
        return this.kind;
    }

    // how much light it gives while it's up
    public int lightWhenUp() {
        return this.light;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UP, FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean up = context.getLevel() instanceof ServerLevel level && Decorations.shouldBeUp(level, this.kind);
        if (!this.canSurvive(this.defaultBlockState(), context.getLevel(), context.getClickedPos())) return null;
        // placed over a packed-away decoration of another holiday: that one is kept here (SeasonalDecorBlockEntity)
        if (context.getLevel() instanceof ServerLevel server && server.getBlockState(context.getClickedPos()).getBlock() instanceof SeasonalDecorBlock) {
            SeasonalDecorBlockEntity.keepFor(server, context.getClickedPos(), this.kind);
        }
        // placed in its season it's up at once; out of season it's packed away (a marker while you hold one)
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(UP, up || context.getLevel().isClientSide());
    }

    // A packed-away decoration's spot takes a decoration of another holiday: the new one goes in and keeps this one there
    // (one spot, one decoration per holiday; whichever is in season shows)
    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        if (state.getValue(UP)) return false;
        return context.getItemInHand().getItem() instanceof net.minecraft.world.item.BlockItem item
            && item.getBlock() instanceof SeasonalDecorBlock other && other.kind.holiday != this.kind.holiday;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.@Nullable LivingEntity by, net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, by, stack);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SeasonalDecorBlockEntity decor) decor.takeKept();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return this.kind.blockModel() ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }

    // Christmas Frost lies on the top of a solid block; Icicles hang from the underside of one
    @Override
    protected boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        if (this.kind == DecorKind.CHRISTMAS_FROST || this.kind == DecorKind.FALLEN_LEAVES) return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
        if (this.kind == DecorKind.ICICLES) return level.getBlockState(pos.above()).isFaceSturdy(level, pos.above(), Direction.DOWN);
        return true;
    }

    @Override
    protected BlockState updateShape(BlockState state, net.minecraft.world.level.LevelReader level, net.minecraft.world.level.ScheduledTickAccess ticks,
                                     BlockPos pos, Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return !state.canSurvive(level, pos) ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
            : super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    // up: its footprint; packed away: nothing, unless you're holding a decoration (then a marker, to find and break it)
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(UP)) return this.upShape;
        if (context instanceof EntityCollisionContext entityContext && entityContext.getEntity() instanceof Player player
            && (player.getMainHandItem().getItem() instanceof StringLightsItem
            || player.getMainHandItem().getItem() instanceof net.minecraft.world.item.BlockItem bi && bi.getBlock() instanceof SeasonalDecorBlock)) {
            return this.kind == DecorKind.STRING_LIGHTS ? HOOK_MARKER : MARKER;
        }
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(UP) && !this.kind.passable() ? this.upShape : Shapes.empty();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SeasonalDecorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel)) return null;
        return createTickerHelper(type, com.alexh.capitalism.core.ModBlockEntities.SEASONAL_DECOR.get(),
            (l, pos, s, decor) -> SeasonalDecorBlockEntity.serverTick((ServerLevel) l, pos, s, decor));
    }

    // a little life while it's up: snowflakes round the tree, leaves falling off a leaf pile, the cauldron bubbling
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(UP)) return;
        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;
        switch (this.kind) {
            case CHRISTMAS_TREE -> {
                if (random.nextFloat() < 0.5F) {
                    level.addParticle(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE, x + (random.nextDouble() - 0.5) * 6,
                        y + 2 + random.nextDouble() * 7, z + (random.nextDouble() - 0.5) * 6, 0.0, -0.02, 0.0);
                }
            }
            case LEAF_PILE -> {
                int[] colors = {0xD8401A, 0xE87A20, 0xF0B020, 0xA02818, 0xC86018};
                level.addParticle(net.minecraft.core.particles.ColorParticleOption.create(net.minecraft.core.particles.ParticleTypes.TINTED_LEAVES,
                    colors[random.nextInt(colors.length)]), x + (random.nextDouble() - 0.5) * 2.2, y + 0.6 + random.nextDouble() * 0.6,
                    z + (random.nextDouble() - 0.5) * 2.2, 0.0, 0.0, 0.0);
            }
            case AUTUMN_MAPLE, AUTUMN_BIRCH -> {
                int[] colors = this.kind == DecorKind.AUTUMN_MAPLE ? new int[]{0xD8401A, 0xE87A20, 0xF0B020, 0xA02818} : new int[]{0xF0C030, 0xE8A020, 0xF8D850};
                level.addParticle(net.minecraft.core.particles.ColorParticleOption.create(net.minecraft.core.particles.ParticleTypes.TINTED_LEAVES,
                    colors[random.nextInt(colors.length)]), x + (random.nextDouble() - 0.5) * 4.0, y + 3.5 + random.nextDouble() * 2.0,
                    z + (random.nextDouble() - 0.5) * 4.0, 0.0, 0.0, 0.0);
            }
            case PUMPKIN_PILE -> {
                if (random.nextFloat() < 0.3F) {
                    level.addParticle(net.minecraft.core.particles.ColorParticleOption.create(net.minecraft.core.particles.ParticleTypes.TINTED_LEAVES, 0xE87A20),
                        x + (random.nextDouble() - 0.5) * 2.4, y + 1.2, z + (random.nextDouble() - 0.5) * 2.4, 0.0, 0.0, 0.0);
                }
            }
            case WITCHS_CAULDRON -> {
                level.addParticle(net.minecraft.core.particles.ParticleTypes.WITCH, x + (random.nextDouble() - 0.5) * 0.7, y + 1.0,
                    z + (random.nextDouble() - 0.5) * 0.7, 0.0, 0.03, 0.0);
                if (random.nextFloat() < 0.6F) {
                    level.addParticle(net.minecraft.core.particles.ColorParticleOption.create(net.minecraft.core.particles.ParticleTypes.ENTITY_EFFECT, 0x60FF60),
                        x + (random.nextDouble() - 0.5) * 0.6, y + 0.95, z + (random.nextDouble() - 0.5) * 0.6, 0.0, 0.0, 0.0);
                }
                if (random.nextFloat() < 0.15F) {
                    level.playLocalSound(x, y + 0.9, z, net.minecraft.sounds.SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, net.minecraft.sounds.SoundSource.BLOCKS, 0.3F, 0.7F + random.nextFloat() * 0.3F, false);
                }
            }
            case ICICLES -> {
                if (random.nextFloat() < 0.06F) {
                    level.addParticle(net.minecraft.core.particles.ParticleTypes.DRIPPING_DRIPSTONE_WATER, pos.getX() + 0.2 + random.nextDouble() * 0.6,
                        pos.getY() + 0.2, pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0.0, 0.0, 0.0);
                }
            }
            case CHRISTMAS_FROST -> {
                if (random.nextFloat() < 0.03F) {
                    level.addParticle(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE, pos.getX() + random.nextDouble(), pos.getY() + 0.15,
                        pos.getZ() + random.nextDouble(), 0.0, 0.01, 0.0);
                }
            }
            case SCARECROW -> {
                if (random.nextFloat() < 0.05F) {
                    level.playLocalSound(x, y + 2, z, net.minecraft.sounds.SoundEvents.PARROT_IMITATE_PHANTOM, net.minecraft.sounds.SoundSource.BLOCKS, 0.3F, 0.6F, false);
                }
            }
            default -> {
            }
        }
    }

    // On Christmas (calendar/Festival.CHRISTMAS), right-clicking the Grand Christmas Tree gives you a Christmas Present,
    // once that day; on other days while it's up, a hint
    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                                   net.minecraft.world.phys.BlockHitResult hit) {
        if (this.kind != DecorKind.CHRISTMAS_TREE || !state.getValue(UP)) return net.minecraft.world.InteractionResult.PASS;
        if (level instanceof ServerLevel server) treePresent(server, pos, player);
        return net.minecraft.world.InteractionResult.SUCCESS;
    }

    private static final String TREE_GIFT = "capitalism_tree_present";

    // the tree's present for this player (true if they got one now)
    public static boolean treePresent(ServerLevel level, BlockPos pos, Player player) {
        com.alexh.capitalism.calendar.Calendar.Date date = com.alexh.capitalism.calendar.Calendar.date(level);
        if (com.alexh.capitalism.calendar.Festival.on(date) != com.alexh.capitalism.calendar.Festival.CHRISTMAS) {
            player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Come back on Christmas ("
                + com.alexh.capitalism.calendar.Festival.CHRISTMAS.season.title + " " + com.alexh.capitalism.calendar.Festival.CHRISTMAS.day
                + ") for a present!").withStyle(net.minecraft.ChatFormatting.GREEN));
            return false;
        }
        var data = player.getPersistentData();
        if (data.getLongOr(TREE_GIFT, -1L) == date.index()) {
            player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("You've had your present from the tree this Christmas!")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
            return false;
        }
        data.putLong(TREE_GIFT, date.index());
        net.minecraft.world.item.ItemStack present = new net.minecraft.world.item.ItemStack(com.alexh.capitalism.core.ModItems.CHRISTMAS_PRESENT.get());
        if (!player.getInventory().add(present)) player.drop(present, false, net.minecraft.util.Prediction.SERVER_ONLY);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE, pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5, 40, 1.5, 1.5, 1.5, 0.05);
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.5F);
        player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("🎄 A present from the tree! Right-click it to unwrap it.")
            .withStyle(net.minecraft.ChatFormatting.RED));
        return true;
    }

    // is it something you can pass through (so it doesn't suffocate or block mobs)?
    @Override
    protected boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) {
        return !state.getValue(UP) || this.kind.passable();
    }
}
