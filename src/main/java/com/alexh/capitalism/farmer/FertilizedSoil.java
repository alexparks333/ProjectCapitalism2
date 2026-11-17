package com.alexh.capitalism.farmer;

import com.alexh.capitalism.core.ModAttachments;
import com.alexh.capitalism.core.ModBlocks;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// The fertilized farmland in a chunk (Farmer.md > Fertilizer), saved with the chunk; the farmland itself turns into
// Fertilized Farmland to show it (FertilizedFarmlandBlock, back to farmland once it's all used up). Each spot holds
// one fertilizer at most: Yield (how many Farmer harvests it has left: Yield 2, Super Yield 4, and whether it was
// Super) or Fast (how many harvests it has left: 2; FertilizerItem won't put one on another). Gone once used up, or once the spot isn't Fertilized Farmland any more (turned to dirt: the fertilizer is lost).
public class FertilizedSoil {

    private static final Codec<Map<String, Integer>> SPOTS = Codec.unboundedMap(Codec.STRING, Codec.INT);
    public static final MapCodec<FertilizedSoil> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
        SPOTS.fieldOf("soil").forGetter(soil -> saved(soil.harvests)),
        Codec.STRING.listOf().optionalFieldOf("super", List.of()).forGetter(soil -> soil.superYield.stream().map(pos -> Long.toString(pos)).toList()),
        SPOTS.optionalFieldOf("fast", Map.of()).forGetter(soil -> saved(soil.fast))
    ).apply(inst, FertilizedSoil::new));

    // Yield harvests left, the spots whose Yield is Super, Fast harvests left
    private final Map<Long, Integer> harvests = new HashMap<>();
    private final Set<Long> superYield = new HashSet<>();
    private final Map<Long, Integer> fast = new HashMap<>();

    public FertilizedSoil() {
    }

    private FertilizedSoil(Map<String, Integer> harvests, List<String> superYield, Map<String, Integer> fast) {
        harvests.forEach((pos, left) -> this.harvests.put(Long.parseLong(pos), left));
        superYield.forEach(pos -> this.superYield.add(Long.parseLong(pos)));
        fast.forEach((pos, left) -> this.fast.put(Long.parseLong(pos), left));
    }

    private static Map<String, Integer> saved(Map<Long, Integer> spots) {
        Map<String, Integer> out = new HashMap<>();
        spots.forEach((pos, left) -> out.put(Long.toString(pos), left));
        return out;
    }

    private static FertilizedSoil of(Level level, BlockPos pos) {
        return level.getChunkAt(pos).getData(ModAttachments.FERTILIZED_SOIL.get());
    }

    // Farmland of either kind (plain, or showing its fertilizer)
    public static boolean isFarmland(BlockState state) {
        return state.getBlock() instanceof FarmlandBlock;
    }

    // Only Fertilized Farmland holds fertilizer; anything left over on another block is stale (it was trampled to dirt
    // and hoed again, broken, ...)
    private static boolean fertilized(Level level, BlockPos soil) {
        return level.getBlockState(soil).is(ModBlocks.FERTILIZED_FARMLAND.get());
    }

    // How many Farmer harvests this farmland's Yield Fertilizer has left (0: none)
    public static int harvestsLeft(Level level, BlockPos soil) {
        if (!fertilized(level, soil)) return 0;
        return of(level, soil).harvests.getOrDefault(soil.asLong(), 0);
    }

    // Whether its Yield Fertilizer is Super Yield
    public static boolean isSuper(Level level, BlockPos soil) {
        return harvestsLeft(level, soil) > 0 && of(level, soil).superYield.contains(soil.asLong());
    }

    // How many harvests this farmland's Fast Fertilizer has left (0: none)
    public static int fastLeft(Level level, BlockPos soil) {
        if (!fertilized(level, soil)) return 0;
        return of(level, soil).fast.getOrDefault(soil.asLong(), 0);
    }

    // Yield Fertilizer (not Super)
    public static void set(Level level, BlockPos soil, int harvests) {
        set(level, soil, harvests, false);
    }

    public static void set(Level level, BlockPos soil, int harvests, boolean superYield) {
        FertilizedSoil data = fresh(level, soil);
        if (harvests <= 0) data.harvests.remove(soil.asLong());
        else data.harvests.put(soil.asLong(), harvests);
        if (harvests > 0 && superYield) data.superYield.add(soil.asLong());
        else data.superYield.remove(soil.asLong());
        show(level, soil, data);
    }

    public static void setFast(Level level, BlockPos soil, int harvests) {
        FertilizedSoil data = fresh(level, soil);
        if (harvests <= 0) data.fast.remove(soil.asLong());
        else data.fast.put(soil.asLong(), harvests);
        show(level, soil, data);
    }

    // The fertilizer on this spot is lost (it turned to dirt)
    public static void clear(Level level, BlockPos soil) {
        LevelChunk chunk = level.getChunkAt(soil);
        FertilizedSoil data = chunk.getData(ModAttachments.FERTILIZED_SOIL.get());
        data.harvests.remove(soil.asLong());
        data.superYield.remove(soil.asLong());
        data.fast.remove(soil.asLong());
        chunk.markUnsaved();
    }

    // This spot's data, with anything stale from before thrown away (plain farmland holds no fertilizer)
    private static FertilizedSoil fresh(Level level, BlockPos soil) {
        if (!fertilized(level, soil)) clear(level, soil);
        LevelChunk chunk = level.getChunkAt(soil);
        chunk.markUnsaved();
        return chunk.getData(ModAttachments.FERTILIZED_SOIL.get());
    }

    // Make the farmland show what's on it: Fertilized Farmland with the right look, or plain farmland once nothing is
    // (keeping how wet it is)
    private static void show(Level level, BlockPos soil, FertilizedSoil data) {
        BlockState state = level.getBlockState(soil);
        if (!isFarmland(state)) return;
        int moisture = state.getValue(FarmlandBlock.MOISTURE);
        boolean yield = data.harvests.getOrDefault(soil.asLong(), 0) > 0;
        boolean fast = data.fast.getOrDefault(soil.asLong(), 0) > 0;
        BlockState shown;
        if (!yield && !fast) {
            shown = Blocks.FARMLAND.defaultBlockState();
        } else {
            FertilizedFarmlandBlock.Look look = fast
                ? FertilizedFarmlandBlock.Look.FAST
                : (data.superYield.contains(soil.asLong()) ? FertilizedFarmlandBlock.Look.SUPER_YIELD : FertilizedFarmlandBlock.Look.YIELD);
            shown = ModBlocks.FERTILIZED_FARMLAND.get().defaultBlockState().setValue(FertilizedFarmlandBlock.FERTILIZER, look);
        }
        shown = shown.setValue(FarmlandBlock.MOISTURE, moisture);
        if (shown != state) level.setBlock(soil, shown, Block.UPDATE_ALL);
    }

    // A Farmer harvested a crop on it: one Yield harvest fewer. Returns how many are left.
    public static int use(Level level, BlockPos soil) {
        int left = harvestsLeft(level, soil) - 1;
        set(level, soil, left, isSuper(level, soil));
        return Math.max(0, left);
    }

    // A ripe crop on it was harvested (by anyone): one Fast harvest fewer. Returns how many are left.
    public static int useFast(Level level, BlockPos soil) {
        int left = fastLeft(level, soil) - 1;
        setFast(level, soil, left);
        return Math.max(0, left);
    }
}
