package com.alexh.capitalism.farmer;

import com.alexh.capitalism.block.RiceBlock;
import com.alexh.capitalism.core.ModBlocks;
import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.occupation.Job;
import com.alexh.capitalism.occupation.JobEvents;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

// What a crop gives when it's harvested (Farmer.md > Harvests). Only a Farmer gets the full (vanilla) harvest; anyone
// else, and anything that isn't a player (water, pistons, villagers, explosions), gets one crop and one seed back (two of
// the crop when it is its own seed: carrots, potatoes, nether wart, cocoa beans, sweet berries), one melon slice, one
// berry when picking a bush. Unripe crops are left alone (they just give the seed back anyway). A Farmer harvesting on
// Yield-fertilized farmland (FertilizedSoil) gets 50% to 90% more of the crop, and uses up one of its harvests. Any
// harvest of a ripe crop on Fast Fertilizer uses up one of its harvests.
// Turned on by data/capitalism/loot_modifiers/farm_harvest.json; the codec is registered in core/ModLootModifiers.
public class FarmHarvestModifier extends LootModifier {

    public static final MapCodec<FarmHarvestModifier> CODEC = RecordCodecBuilder.mapCodec(
        inst -> codecStart(inst).apply(inst, FarmHarvestModifier::new));

    // extra crops from fertilized farmland: 50% to 90% more
    public static final float BONUS_MIN = 0.5F;
    public static final float BONUS_MAX = 0.9F;
    // Super Yield Fertilizer's bonus: more than Yield's
    public static final float SUPER_BONUS_MIN = 0.6F;
    public static final float SUPER_BONUS_MAX = 1.15F;

    private static final Identifier PICK_BERRIES = Identifier.withDefaultNamespace("harvest/sweet_berry_bush");

    public FarmHarvestModifier(Optional<Holder<LootItemCondition>> condition, int priority) {
        super(condition, priority);
    }

    // A ripe crop: what it grows, and its seed (the same item when it plants itself)
    private record Crop(Item produce, Item seed) {
        boolean plantsItself() {
            return this.produce == this.seed;
        }
    }

    private static @Nullable Crop ripe(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop && crop.isMaxAge(state)) {
            if (state.is(Blocks.WHEAT)) return new Crop(Items.WHEAT, Items.WHEAT_SEEDS);
            if (state.is(Blocks.BEETROOTS)) return new Crop(Items.BEETROOT, Items.BEETROOT_SEEDS);
            if (state.is(Blocks.CARROTS)) return new Crop(Items.CARROT, Items.CARROT);
            if (state.is(Blocks.POTATOES)) return new Crop(Items.POTATO, Items.POTATO);
            return null;
        }
        if (state.is(ModBlocks.RICE.get()) && state.getValue(RiceBlock.AGE) == RiceBlock.MAX_AGE) return new Crop(ModItems.RICE.get(), ModItems.RICE_SEEDS.get());
        if (state.is(Blocks.NETHER_WART) && state.getValue(NetherWartBlock.AGE) == NetherWartBlock.MAX_AGE) return new Crop(Items.NETHER_WART, Items.NETHER_WART);
        if (state.is(Blocks.COCOA) && state.getValue(CocoaBlock.AGE) == CocoaBlock.MAX_AGE) return new Crop(Items.COCOA_BEANS, Items.COCOA_BEANS);
        if (state.is(Blocks.SWEET_BERRY_BUSH) && state.getValue(SweetBerryBushBlock.AGE) > 1) return new Crop(Items.SWEET_BERRIES, Items.SWEET_BERRIES);
        if (state.is(Blocks.MELON)) return new Crop(Items.MELON_SLICE, Items.AIR);
        if (state.is(Blocks.PUMPKIN)) return new Crop(Items.PUMPKIN, Items.AIR);
        return null;
    }

    public static boolean isRipeCrop(BlockState state) {
        return ripe(state) != null;
    }

    public static boolean isFarmer(@Nullable Entity entity) {
        return entity instanceof Player player && JobEvents.has(player, Job.FARMER);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        BlockState state = context.getOptional(LootContextParams.BLOCK_STATE);
        Vec3 origin = context.getOptional(LootContextParams.ORIGIN);
        if (state == null || origin == null) return loot;
        Crop crop = ripe(state);
        if (crop == null) return loot;
        boolean picking = PICK_BERRIES.equals(context.getQueriedLootTableId());
        Entity harvester = picking ? context.getOptional(LootContextParams.INTERACTING_ENTITY) : context.getOptional(LootContextParams.THIS_ENTITY);
        BlockPos soil = soil(context.getLevel(), BlockPos.containing(origin), state);
        // Fast Fertilizer: any harvest of a ripe crop on it uses one up (it already made the crop grow faster)
        String fast = null;
        if (soil != null && FertilizedSoil.fastLeft(context.getLevel(), soil) > 0) {
            int left = FertilizedSoil.useFast(context.getLevel(), soil);
            fast = left > 0 ? "Fast: " + harvests(left) + " left" : "The Fast Fertilizer is used up";
        }
        if (!isFarmer(harvester)) {
            tell(harvester, fast);
            return smallHarvest(crop, picking);
        }
        if (soil == null || FertilizedSoil.harvestsLeft(context.getLevel(), soil) <= 0) {
            tell(harvester, fast);
            return loot;
        }
        fertilized(loot, crop, context.getRandom(), FertilizedSoil.isSuper(context.getLevel(), soil));
        int left = FertilizedSoil.use(context.getLevel(), soil);
        context.getLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER, origin.x, origin.y, origin.z, 6, 0.3, 0.3, 0.3, 0.0);
        String yield = left > 0 ? "Fertilized: " + harvests(left) + " left" : "The Yield Fertilizer is used up";
        tell(harvester, fast == null ? yield : yield + "  |  " + fast);
        return loot;
    }

    private static String harvests(int count) {
        return count + (count == 1 ? " harvest" : " harvests");
    }

    private static void tell(@Nullable Entity harvester, @Nullable String message) {
        if (message != null && harvester instanceof ServerPlayer player) {
            player.sendOverlayMessage(Component.literal(message).withStyle(ChatFormatting.GREEN));
        }
    }

    // Anyone but a Farmer: one crop and one seed back (two of the crop when it plants itself), one slice, one berry
    private static ObjectArrayList<ItemStack> smallHarvest(Crop crop, boolean picking) {
        ObjectArrayList<ItemStack> out = new ObjectArrayList<>();
        if (picking || crop.seed() == Items.AIR) {
            out.add(new ItemStack(crop.produce()));
        } else if (crop.plantsItself()) {
            out.add(new ItemStack(crop.produce(), 2));
        } else {
            out.add(new ItemStack(crop.produce()));
            out.add(new ItemStack(crop.seed()));
        }
        return out;
    }

    // Fertilized: 50% to 90% more of the crop (Super Yield: 60% to 115%; not of a separate seed); a part crop is a
    // chance of one more
    private static void fertilized(ObjectArrayList<ItemStack> loot, Crop crop, RandomSource random, boolean superYield) {
        int count = 0;
        for (ItemStack stack : loot) {
            if (stack.is(crop.produce())) count += stack.getCount();
        }
        if (count == 0) return;
        float min = superYield ? SUPER_BONUS_MIN : BONUS_MIN;
        float max = superYield ? SUPER_BONUS_MAX : BONUS_MAX;
        float extra = count * (min + random.nextFloat() * (max - min));
        int more = (int) extra + (random.nextFloat() < extra - (int) extra ? 1 : 0);
        while (more > 0) {
            int part = Math.min(more, crop.produce().getDefaultMaxStackSize());
            loot.add(new ItemStack(crop.produce(), part));
            more -= part;
        }
    }

    // The farmland the crop grows on: under it, or for a melon or pumpkin, under the stem it grew from (by the time the
    // fruit drops, its stem has already let go of it, so any ripe stem beside it)
    public static @Nullable BlockPos soil(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN)) {
            for (Direction side : Direction.Plane.HORIZONTAL) {
                BlockState beside = level.getBlockState(pos.relative(side));
                if (beside.is(Blocks.MELON_STEM) || beside.is(Blocks.ATTACHED_MELON_STEM) || beside.is(Blocks.PUMPKIN_STEM) || beside.is(Blocks.ATTACHED_PUMPKIN_STEM)) {
                    BlockPos under = pos.relative(side).below();
                    if (FertilizedSoil.isFarmland(level.getBlockState(under))) return under;
                }
            }
            return null;
        }
        BlockPos under = pos.below();
        return FertilizedSoil.isFarmland(level.getBlockState(under)) ? under : null;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
