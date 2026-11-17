package com.alexh.capitalism.block;

import com.alexh.capitalism.core.ModBlockEntities;
import com.alexh.capitalism.core.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

// The Advanced Osmosis Machine (Bartending.md > Spring Water): Bartenders only (to craft and to use, occupation/JobEvents).
// No menu, just right-clicks:
//  - with an Osmosis Filter (2 buckets) or an Advanced Osmosis Filter (4): it goes in (one at a time);
//  - with a Purified Water Bucket, a filter in: the water goes in and is pressed through the filter for BREW_TICKS (a
//    minute); the filter wears one bucket (and crumbles when it's used up);
//  - when it's done, with an empty bucket or an empty hand: a Spring Water Bucket comes out (pour it into 6 bottles at
//    the Bartending Table);
//  - empty-handed otherwise: what it's doing (filter uses left, time left), and shift + empty hand takes the filter out.
// While it works it hums and bubbles (lit).
public class OsmosisMachineBlock extends BaseEntityBlock {

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final int BREW_TICKS = 60 * 20;

    public OsmosisMachineBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Machine(pos, state);
    }

    private static void say(Player player, String text) {
        if (player instanceof ServerPlayer sp) sp.sendOverlayMessage(Component.literal(text).withStyle(ChatFormatting.AQUA));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server) || !(level.getBlockEntity(pos) instanceof Machine machine)) return InteractionResult.SUCCESS;
        if (stack.is(ModItems.OSMOSIS_FILTER.get()) || stack.is(ModItems.ADVANCED_OSMOSIS_FILTER.get())) {
            if (!machine.filter.isEmpty()) {
                say(player, "It already has a filter (shift + right-click empty-handed to take it out).");
                return InteractionResult.FAIL;
            }
            machine.filter = stack.split(1);
            machine.setChanged();
            server.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 0.8F);
            say(player, "Filter in: " + machine.usesLeft() + " bucket" + (machine.usesLeft() == 1 ? "" : "s") + " left in it.");
            return InteractionResult.SUCCESS;
        }
        if (stack.is(ModItems.PURIFIED_WATER_BUCKET.get())) {
            if (machine.ready || machine.progress > 0) {
                say(player, machine.ready ? "Take the Spring Water out first." : "It's already pressing a bucket.");
                return InteractionResult.FAIL;
            }
            if (machine.filter.isEmpty()) {
                say(player, "It needs an Osmosis Filter first.");
                return InteractionResult.FAIL;
            }
            if (!player.isCreative()) player.setItemInHand(hand, new ItemStack(net.minecraft.world.item.Items.BUCKET));
            machine.progress = BREW_TICKS;
            machine.setChanged();
            level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
            server.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            say(player, "The water starts through the filter: Spring Water in a minute.");
            return InteractionResult.SUCCESS;
        }
        if (stack.is(net.minecraft.world.item.Items.BUCKET) && machine.ready) {
            stack.shrink(1);
            give(player, new ItemStack(ModItems.SPRING_WATER_BUCKET.get()));
            machine.ready = false;
            machine.setChanged();
            server.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.2F);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server) || !(level.getBlockEntity(pos) instanceof Machine machine)) return InteractionResult.SUCCESS;
        if (machine.ready) {
            give(player, new ItemStack(ModItems.SPRING_WATER_BUCKET.get()));
            machine.ready = false;
            machine.setChanged();
            server.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.2F);
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown() && !machine.filter.isEmpty() && machine.progress <= 0) {
            give(player, machine.filter);
            machine.filter = ItemStack.EMPTY;
            machine.setChanged();
            return InteractionResult.SUCCESS;
        }
        if (machine.progress > 0) say(player, "Pressing water through the filter: " + (machine.progress / 20 + 1) + " s left.");
        else if (machine.filter.isEmpty()) say(player, "Put in an Osmosis Filter, then a Purified Water Bucket.");
        else say(player, "Filter: " + machine.usesLeft() + " bucket" + (machine.usesLeft() == 1 ? "" : "s") + " left. Put in a Purified Water Bucket.");
        return InteractionResult.SUCCESS;
    }

    private static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof Machine machine) {
            if (!machine.filter.isEmpty()) Block.popResource(level, pos, machine.filter);
            if (machine.ready) Block.popResource(level, pos, new ItemStack(ModItems.SPRING_WATER_BUCKET.get()));
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;
        level.addParticle(ParticleTypes.BUBBLE_POP, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 1.02, pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.02, 0);
        if (random.nextInt(4) == 0) level.addParticle(ParticleTypes.DRIPPING_WATER, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 0, 0, 0);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.OSMOSIS_MACHINE.get(), (l, p, s, m) -> m.tick(l, p, s));
    }

    public static class Machine extends BlockEntity {
        ItemStack filter = ItemStack.EMPTY;
        int progress;
        boolean ready;

        public Machine(BlockPos pos, BlockState state) {
            super(ModBlockEntities.OSMOSIS_MACHINE.get(), pos, state);
        }

        public int usesLeft() {
            return this.filter.isEmpty() ? 0 : this.filter.getMaxDamage() - this.filter.getDamageValue();
        }

        public boolean ready() {
            return this.ready;
        }

        public ItemStack filter() {
            return this.filter;
        }

        public void tick(Level level, BlockPos pos, BlockState state) {
            if (this.progress <= 0) return;
            if (--this.progress > 0) {
                if (this.progress % 40 == 0) level.playSound(null, pos, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.BLOCKS, 0.5F, 1.2F);
                return;
            }
            // done: Spring Water, and the filter wears a bucket
            this.ready = true;
            this.filter.setDamageValue(this.filter.getDamageValue() + 1);
            if (this.filter.getDamageValue() >= this.filter.getMaxDamage()) {
                this.filter = ItemStack.EMPTY;
                level.playSound(null, pos, SoundEvents.ITEM_BREAK.value(), SoundSource.BLOCKS, 0.8F, 1.0F);
            }
            level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 1.4F);
            this.setChanged();
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            this.filter = input.read("filter", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
            this.progress = input.getIntOr("progress", 0);
            this.ready = input.getBooleanOr("ready", false);
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            if (!this.filter.isEmpty()) output.store("filter", ItemStack.OPTIONAL_CODEC, this.filter);
            output.putInt("progress", this.progress);
            output.putBoolean("ready", this.ready);
        }
    }
}
