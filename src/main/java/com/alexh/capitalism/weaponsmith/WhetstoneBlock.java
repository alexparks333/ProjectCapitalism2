package com.alexh.capitalism.weaponsmith;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

// The Whetstones: a sharpening stone on a wooden stand, one per kind (Kind): the Sword Whetstone (Weaponsmiths make it),
// the Tool Whetstone (Toolsmiths) and the Armor Whetstone (Armorsmiths). Anyone can place and use them, so smiths
// can sell them. Right-click one holding its kind of item, with a Water Bucket in your off hand or inventory: the bucket
// is poured out (you keep the empty bucket), the item is like new again (Sharpness / Wear) and the Whetstone is worn
// out: it breaks (no drop), so every sharpening takes a new one. Creative keeps it.
public class WhetstoneBlock extends Block {

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    public enum Kind {
        SWORD("sword", "This sword is already razor sharp.", "Right-click with a sword to sharpen it. One use."),
        PICKAXE("tool", "This is already sharp.", "Right-click with a pickaxe, shovel, axe or Butcher Knife to sharpen it. One use."),
        ARMOR("piece of armor", "This armor has no dents.", "Right-click with a piece of armor to hammer out its dents. One use.");

        final String what;
        final String alreadyFine;
        final String hint;

        Kind(String what, String alreadyFine, String hint) {
            this.what = what;
            this.alreadyFine = alreadyFine;
            this.hint = hint;
        }

        boolean accepts(ItemStack stack) {
            return switch (this) {
                case SWORD -> Sharpness.isSword(stack);
                case PICKAXE -> Wear.wears(stack) || com.alexh.capitalism.farmer.ButcherKnifeItem.isKnife(stack);
                case ARMOR -> Wear.isArmor(stack);
            };
        }

        boolean isFine(ItemStack stack) {
            return switch (this) {
                case SWORD -> Sharpness.hits(stack) == 0;
                case PICKAXE -> com.alexh.capitalism.farmer.ButcherKnifeItem.isKnife(stack)
                    ? com.alexh.capitalism.farmer.ButcherKnifeItem.used(stack) == 0 : Wear.pickaxeUsed(stack) == 0;
                case ARMOR -> Wear.armorDents(stack) == 0;
            };
        }

        // Fixes it and says how long it lasts now
        String restore(ItemStack stack) {
            return switch (this) {
                case SWORD -> {
                    Sharpness.sharpen(stack);
                    yield "Sharpened! " + Sharpness.maxHits(stack) + " hits until it's dull again.";
                }
                case PICKAXE -> {
                    if (com.alexh.capitalism.farmer.ButcherKnifeItem.isKnife(stack)) {
                        com.alexh.capitalism.farmer.ButcherKnifeItem.sharpen(stack);
                        yield "Sharpened! " + com.alexh.capitalism.farmer.ButcherKnifeItem.maxCuts(stack) + " slabs until it's spent.";
                    }
                    Wear.restore(stack);
                    yield "Sharpened! " + Wear.pickaxeMax(stack) + " blocks until it's dull again.";
                }
                case ARMOR -> {
                    Wear.restore(stack);
                    yield "Dents hammered out! " + Wear.armorMax(stack) + " hits until it's dented again.";
                }
            };
        }
    }

    private final Kind kind;

    // The stone runs left to right as you look at it
    private static final VoxelShape SHAPE_NS = Block.box(1, 0, 3, 15, 6, 13);
    private static final VoxelShape SHAPE_EW = Block.box(3, 0, 1, 13, 6, 15);

    public WhetstoneBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    // Only Weaponsmiths can build it.
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
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? SHAPE_NS : SHAPE_EW;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!this.kind.accepts(stack)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            if (this.kind.isFine(stack)) {
                serverPlayer.sendOverlayMessage(Component.literal(this.kind.alreadyFine));
                return InteractionResult.SUCCESS;
            }
            // The stone has to be wet: every sharpening pours out one Water Bucket (you keep the empty bucket).
            if (!player.isCreative() && !useWaterBucket(player)) {
                serverPlayer.sendOverlayMessage(Component.literal("You need a Water Bucket to wet the stone."));
                return InteractionResult.SUCCESS;
            }
            String done = this.kind.restore(stack);
            serverLevel.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 16, 0.3, 0.05, 0.2, 0.1);
            serverLevel.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.6F, 1.1F);
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.45;
            double z = pos.getZ() + 0.5;
            serverLevel.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.8F, 1.2F);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 14, 0.25, 0.05, 0.25, 0.25);
            serverLevel.sendParticles(ParticleTypes.CRIT, x, y, z, 8, 0.2, 0.05, 0.2, 0.2);
            serverPlayer.sendOverlayMessage(Component.literal(done + " The Whetstone is worn out."));
            // One use: the stone breaks (no drop)
            if (!player.isCreative()) {
                serverLevel.levelEvent(2001, pos, Block.getId(state));
                serverLevel.removeBlock(pos, false);
            }
        }
        return InteractionResult.SUCCESS;
    }

    // Off hand first, then the rest of the inventory. Turns it into an empty Bucket.
    private static boolean useWaterBucket(Player player) {
        if (player.getOffhandItem().is(Items.WATER_BUCKET)) {
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.BUCKET));
            return true;
        }
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(Items.WATER_BUCKET)) {
                inventory.setItem(i, new ItemStack(Items.BUCKET));
                return true;
            }
        }
        return false;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendOverlayMessage(Component.literal(this.kind.hint));
        }
        return InteractionResult.SUCCESS;
    }
}
