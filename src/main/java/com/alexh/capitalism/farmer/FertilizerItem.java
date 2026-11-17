package com.alexh.capitalism.farmer;

import com.alexh.capitalism.block.RiceBlock;
import com.alexh.capitalism.occupation.Job;
import com.alexh.capitalism.occupation.JobEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;

// The Fertilizers (Farmer.md > Fertilizer), made in the Decomposter. A Farmer right-clicks farmland (or a crop growing
// on it). Yield and Super Yield Fertilizer: its next HARVESTS Farmer harvests (2, or 4 for Super) give 50% to 90% more
// crops (FarmHarvestModifier). Fast Fertilizer: crops on it grow FAST_SPEED times as fast (grow) for its next
// FAST_HARVESTS harvests. One fertilizer on a piece of farmland at a time, of any kind, and no topping up.
// Only Farmers can use them.
public class FertilizerItem extends Item {

    public static final int HARVESTS = 2;
    public static final int SUPER_HARVESTS = 3;
    public static final int FAST_HARVESTS = 2;
    // how much faster crops grow on Fast Fertilizer: every time one grows a stage, 75% of the time it grows another
    public static final float FAST_SPEED = 1.75F;
    // A seed on Fast Fertilizer that hasn't sprouted yet gets this extra chance to sprout on each growth check (sprout)
    public static final float SPROUT_CHANCE = 0.1F;

    public enum Kind {
        YIELD("Yield Fertilizer"),
        SUPER_YIELD("Super Yield Fertilizer"),
        FAST("Fast Fertilizer");

        public final String title;

        Kind(String title) {
            this.title = title;
        }
    }

    private final Kind kind;

    public FertilizerItem(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
        return this.kind;
    }

    public int harvests() {
        return switch (this.kind) {
            case YIELD -> HARVESTS;
            case SUPER_YIELD -> SUPER_HARVESTS;
            case FAST -> FAST_HARVESTS;
        };
    }

    // Why it can't go on this farmland, or null if it can
    private @org.jspecify.annotations.Nullable String refusal(Level level, BlockPos soil) {
        int yield = FertilizedSoil.harvestsLeft(level, soil);
        int fast = FertilizedSoil.fastLeft(level, soil);
        if (yield > 0) return "Already fertilized: " + harvests(yield) + " left";
        if (fast > 0) return "Already has Fast Fertilizer: " + harvests(fast) + " left";
        return null;
    }

    private static String harvests(int count) {
        return count + (count == 1 ? " harvest" : " harvests");
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos soil = context.getClickedPos();
        if (!FertilizedSoil.isFarmland(level.getBlockState(soil))) soil = soil.below();
        if (!FertilizedSoil.isFarmland(level.getBlockState(soil))) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        ServerPlayer serverPlayer = player instanceof ServerPlayer p ? p : null;
        if (player != null && !player.isCreative() && !JobEvents.has(player, Job.FARMER)) {
            if (serverPlayer != null) serverPlayer.sendOverlayMessage(Component.literal("Only a Farmer can use Fertilizer."));
            return InteractionResult.FAIL;
        }
        String refusal = this.refusal(level, soil);
        if (refusal != null) {
            if (serverPlayer != null) serverPlayer.sendOverlayMessage(Component.literal(refusal));
            return InteractionResult.FAIL;
        }
        if (this.kind == Kind.FAST) FertilizedSoil.setFast(level, soil, this.harvests());
        else FertilizedSoil.set(level, soil, this.harvests(), this.kind == Kind.SUPER_YIELD);
        if (player == null || !player.isCreative()) context.getItemInHand().shrink(1);
        server.sendParticles(this.kind == Kind.FAST ? ParticleTypes.WAX_ON : ParticleTypes.HAPPY_VILLAGER,
            soil.getX() + 0.5, soil.getY() + 1.1, soil.getZ() + 0.5, 10, 0.3, 0.1, 0.3, 0.0);
        level.playSound(null, soil, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1.0F, this.kind == Kind.FAST ? 1.3F : 0.9F);
        if (serverPlayer != null) {
            String what = this.kind == Kind.FAST ? "Crops grow faster here for " : "Fertilized for ";
            serverPlayer.sendOverlayMessage(Component.literal(what + harvests(this.harvests())).withStyle(ChatFormatting.GREEN));
        }
        return InteractionResult.SUCCESS;
    }

    // A growth check on a crop (or stem, or rice) that hasn't sprouted yet: on Fast Fertilizer it sprouts SPROUT_CHANCE
    // of the time even if the normal roll failed, so seeds there don't sit bare for long. Called on CropGrowEvent.Pre.
    public static boolean sprout(ServerLevel level, BlockPos pos, RandomSource random) {
        if (FertilizedSoil.fastLeft(level, pos.below()) <= 0) return false;
        BlockState state = level.getBlockState(pos);
        boolean seed = state.getBlock() instanceof CropBlock crop ? crop.getAge(state) == 0
            : state.getBlock() instanceof StemBlock ? state.getValue(StemBlock.AGE) == 0
            : state.getBlock() instanceof RiceBlock && state.getValue(RiceBlock.AGE) == 0;
        return seed && random.nextFloat() < SPROUT_CHANCE;
    }

    // A crop (or melon / pumpkin stem, or rice) just grew a stage: on Fast Fertilizer, 75% of the time it grows one
    // more, so it ripens FAST_SPEED times as fast. Called from FarmerEvents on NeoForge's CropGrowEvent.Post.
    public static void grow(ServerLevel level, BlockPos pos, RandomSource random) {
        if (FertilizedSoil.fastLeft(level, pos.below()) <= 0 || random.nextFloat() >= FAST_SPEED - 1.0F) return;
        BlockState state = level.getBlockState(pos);
        BlockState grown = null;
        if (state.getBlock() instanceof CropBlock crop) {
            if (!crop.isMaxAge(state)) grown = crop.getStateForAge(crop.getAge(state) + 1);
        } else if (state.getBlock() instanceof StemBlock) {
            if (state.getValue(StemBlock.AGE) < StemBlock.MAX_AGE) grown = state.setValue(StemBlock.AGE, state.getValue(StemBlock.AGE) + 1);
        } else if (state.getBlock() instanceof RiceBlock) {
            if (state.getValue(RiceBlock.AGE) < RiceBlock.MAX_AGE) grown = state.setValue(RiceBlock.AGE, state.getValue(RiceBlock.AGE) + 1);
        }
        if (grown != null) level.setBlock(pos, grown, 2);
    }
}
