package com.alexh.capitalism.pets;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

// The Water Bowl (PetTrainer.md): a round metal bowl with a lip, Small (copper), Medium (iron) or Large (Compacted Iron), holding 1000,
// 3000 or 10000 mL of plain water (WaterBowlBlockEntity, FeederSize). A Water Bucket pours in 1000 mL, a Water Bottle
// 250. Right-click it with anything else (or an empty hand) and its window shows how much is in it. Pets within 5 blocks
// that are thirsty walk over and drink what they need (Pets). LEVEL just shows how full it is. Pet Trainers craft it.
public class WaterBowlBlock extends BaseEntityBlock {

    public static final int BUCKET_ML = 1000;
    public static final int BOTTLE_ML = 250;
    // how full it looks: 0 empty, 1 a third, 2 two thirds, 3 full
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 3);

    public final FeederSize size;
    private final VoxelShape shape;

    public WaterBowlBlock(Properties properties, FeederSize size) {
        super(properties);
        this.size = size;
        this.shape = switch (size) {
            case SMALL -> box(4, 0, 4, 12, 4, 12);
            case MEDIUM -> box(3, 0, 3, 13, 5, 13);
            case LARGE -> box(1, 0, 1, 15, 6, 15);
        };
        this.registerDefaultState(this.stateDefinition.any().setValue(LEVEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shape;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WaterBowlBlockEntity(pos, state);
    }

    // A bucket or bottle of water pours in; anything else opens the window
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        boolean bucket = stack.is(Items.WATER_BUCKET);
        if (!bucket && !Pets.isWaterBottle(stack)) return this.useWithoutItem(state, level, pos, player, hit);
        if (!(level.getBlockEntity(pos) instanceof WaterBowlBlockEntity bowl)) return InteractionResult.PASS;
        if (bowl.water() >= bowl.capacity()) return this.useWithoutItem(state, level, pos, player, hit);
        if (!level.isClientSide()) {
            bowl.pour(bucket ? BUCKET_ML : BOTTLE_ML);
            if (!player.isCreative()) player.setItemInHand(hand, new ItemStack(bucket ? Items.BUCKET : Items.GLASS_BOTTLE));
            level.playSound(null, pos, bucket ? SoundEvents.BUCKET_EMPTY : SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof WaterBowlBlockEntity bowl) serverPlayer.openMenu(bowl);
        return InteractionResult.SUCCESS;
    }
}
