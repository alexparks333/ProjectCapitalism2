package com.alexh.capitalism.block;

import com.alexh.capitalism.core.ModAttachments;
import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.occupation.GourmetFood;
import com.alexh.capitalism.occupation.Job;
import com.alexh.capitalism.occupation.JobEvents;
import com.alexh.capitalism.player.ThirstData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

// A Silver or Golden Plate: a Cook sets it on top of any block (their restaurant table) and serves Gourmet food on it.
//  - Cooks only: place it (PlateItem), break it (can't even be mined by others), put food on it or take it back.
//  - Anyone: right-click to eat one serving off it. Off a plate, the gold drumsticks from Gourmet food are worth
//    more than eating it by hand (Silver x1, Golden x1.5, hand x0.5; see GourmetFood).
// Serving: a Cook right-clicks with Gourmet food to put ONE meal on it (a plate holds one meal at a time).
// Taking back: a Cook sneak-right-clicks with an empty hand.
// Once someone eats the meal, the plate is empty and DIRTY: no new food can go on it until
// a Cook picks it up (empty-hand right-click; it becomes a Dirty Plate item) and washes it in a Sink. A Cook can also
// pick up a clean, empty plate the same way.
public class PlateBlock extends BaseEntityBlock {

    // Someone has eaten off it: it has to be picked up and washed in a Sink before new food goes on it
    public static final BooleanProperty DIRTY = BooleanProperty.create("dirty");
    private static final VoxelShape SHAPE = Block.column(12, 0, 2);
    private static final int COOLDOWN_TICKS = 20;

    private final GourmetFood.Serving serving;

    public PlateBlock(GourmetFood.Serving serving, Properties properties) {
        super(properties);
        this.serving = serving;
        this.registerDefaultState(this.stateDefinition.any().setValue(DIRTY, false));
    }

    public GourmetFood.Serving serving() {
        return this.serving;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DIRTY);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    // Any block that can hold something in the middle of its top (full blocks, slabs, fence and wall posts...),
    // except a Sink (clicking a Sink with a clean plate would otherwise set it down there)
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (level.getBlockState(pos.below()).getBlock() instanceof SinkBlock) return false;
        return Block.canSupportCenter(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return direction == Direction.DOWN && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState()
            : super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    // Only Cooks can break plates.
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (!player.isCreative() && !JobEvents.has(player, Job.COOK)) return 0;
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlateBlockEntity(pos, state);
    }

    // A Cook holding Gourmet food serves it; anything else falls through to eating (below).
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!KitchenRecipes.isGourmet(stack) || !(player.isCreative() || JobEvents.has(player, Job.COOK))) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof PlateBlockEntity plate)) return InteractionResult.SUCCESS;

        if (state.getValue(DIRTY)) {
            ((ServerPlayer) player).sendOverlayMessage(Component.literal("This plate is dirty. Pick it up and wash it in a Sink."));
            return InteractionResult.SUCCESS;
        }
        ItemStack onPlate = plate.food();
        if (!onPlate.isEmpty() && !ItemStack.isSameItemSameComponents(onPlate, stack)) {
            ((ServerPlayer) player).sendOverlayMessage(Component.literal("This plate already has " + onPlate.getHoverName().getString() + " on it."));
            return InteractionResult.SUCCESS;
        }
        int room = PlateBlockEntity.MAX_SERVINGS - onPlate.getCount();
        int moving = Math.min(room, 1);
        if (moving <= 0) {
            ((ServerPlayer) player).sendOverlayMessage(Component.literal("There's already a meal on this plate."));
            return InteractionResult.SUCCESS;
        }
        ItemStack newFood = onPlate.isEmpty() ? stack.copyWithCount(moving) : onPlate.copyWithCount(onPlate.getCount() + moving);
        if (!player.isCreative()) stack.shrink(moving);
        plate.setFood(newFood);
        level.playSound(null, pos, SoundEvents.DECORATED_POT_INSERT, SoundSource.BLOCKS, 0.7F, 1.3F);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof PlateBlockEntity plate)) return InteractionResult.PASS;
        boolean cook = player.isCreative() || JobEvents.has(player, Job.COOK);
        boolean dirty = state.getValue(DIRTY);

        // A Cook picks the plate up with a truly empty hand (holding anything else does nothing): a dirty one (with any leftovers) to take to the Sink, or a
        // clean empty one to put away.
        if (cook && !player.isShiftKeyDown() && player.getMainHandItem().isEmpty() && (dirty || plate.food().isEmpty())) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            ItemStack leftovers = plate.food().copy();
            plate.setFood(ItemStack.EMPTY);
            boolean silver = this.serving == GourmetFood.Serving.SILVER_PLATE;
            ItemStack item = new ItemStack(dirty
                ? (silver ? ModItems.DIRTY_SILVER_PLATE.get() : ModItems.DIRTY_GOLDEN_PLATE.get())
                : (silver ? ModItems.SILVER_PLATE.get() : ModItems.GOLDEN_PLATE.get()));
            com.alexh.capitalism.guide.Discoveries.discover(player, item);
            level.removeBlock(pos, false);
            for (ItemStack give : new ItemStack[]{item, leftovers}) {
                if (!give.isEmpty() && !player.getInventory().add(give)) {
                    player.drop(give, false, net.minecraft.util.Prediction.SERVER_ONLY);
                }
            }
            level.playSound(null, pos, SoundEvents.DECORATED_POT_INSERT, SoundSource.BLOCKS, 0.6F, 1.6F);
            return InteractionResult.SUCCESS;
        }

        if (plate.food().isEmpty()) {
            if (dirty && !level.isClientSide()) {
                ((ServerPlayer) player).sendOverlayMessage(Component.literal("This plate is dirty. A Cook needs to wash it."));
            }
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        ServerPlayer serverPlayer = (ServerPlayer) player;

        // A Cook sneaking with an empty hand takes the food back
        if (player.isShiftKeyDown() && (player.isCreative() || JobEvents.has(player, Job.COOK))) {
            ItemStack food = plate.food().copy();
            plate.setFood(ItemStack.EMPTY);
            if (!player.getInventory().add(food)) player.drop(food, false, net.minecraft.util.Prediction.SERVER_ONLY);
            return InteractionResult.SUCCESS;
        }

        // Anyone else: eat one serving (a short pause between servings)
        ThirstData data = player.getData(ModAttachments.THIRST.get());
        long now = level.getGameTime();
        if (now - data.lastPlateMeal < COOLDOWN_TICKS && now >= data.lastPlateMeal) return InteractionResult.SUCCESS;
        data.lastPlateMeal = now;
        ItemStack serving = plate.food().copyWithCount(1);
        if (GourmetFood.eatFromPlate(serverPlayer, serving, this.serving)) {
            com.alexh.capitalism.craving.Cravings.consumed(serverPlayer, serving);   // today's craving, off a plate too
            ItemStack left = plate.food().copy();
            left.shrink(1);
            plate.setFood(left.isEmpty() ? ItemStack.EMPTY : left);
            if (!state.getValue(DIRTY)) level.setBlock(pos, state.setValue(DIRTY, true), Block.UPDATE_ALL);
        }
        return InteractionResult.SUCCESS;
    }
}
