package com.alexh.capitalism.occupation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.neoforged.neoforge.common.CommonHooks;

import java.util.List;

// A normal shaped crafting recipe ("type": "capitalism:job_shaped") whose result depends on who's crafting:
//  - players with "job" (or in creative) get "result". "job" can also be a list: ["weaponsmith", "toolsmith"] = either;
//  - everyone else gets "others_count" of it, or nothing at all if others_count is 0 (left out) = job only.
// Example: arrows give a Weaponsmith 4 and everyone else 2. A Crafter block counts as "everyone else".
// The crafting player is known thanks to mixin/CraftingMenuMixin (and NeoForge, when the result is taken).
public class JobShapedRecipe extends ShapedRecipe {

    public static final MapCodec<JobShapedRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(
                Recipe.CommonInfo.MAP_CODEC.forGetter(o -> o.commonInfo),
                CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(o -> o.bookInfo),
                ShapedRecipePattern.MAP_CODEC.forGetter(o -> o.pattern),
                ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.jobResult),
                ExtraCodecs.compactListCodec(Codec.STRING).fieldOf("job").forGetter(o -> o.jobs),
                Codec.INT.optionalFieldOf("others_count", 0).forGetter(o -> o.othersCount)
            )
            .apply(i, JobShapedRecipe::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, JobShapedRecipe> STREAM_CODEC = StreamCodec.composite(
        Recipe.CommonInfo.STREAM_CODEC, o -> o.commonInfo,
        CraftingRecipe.CraftingBookInfo.STREAM_CODEC, o -> o.bookInfo,
        ShapedRecipePattern.STREAM_CODEC, o -> o.pattern,
        ItemStackTemplate.STREAM_CODEC, o -> o.jobResult,
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), o -> o.jobs,
        ByteBufCodecs.VAR_INT, o -> o.othersCount,
        JobShapedRecipe::new
    );
    public static final RecipeSerializer<JobShapedRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final ItemStackTemplate jobResult;
    private final List<String> jobs;
    private final int othersCount;

    public JobShapedRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, ShapedRecipePattern pattern,
                           ItemStackTemplate result, List<String> jobs, int othersCount) {
        super(commonInfo, bookInfo, pattern, result);
        this.jobResult = result;
        this.jobs = List.copyOf(jobs);
        this.othersCount = othersCount;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public RecipeSerializer<ShapedRecipe> getSerializer() {
        return (RecipeSerializer) SERIALIZER;
    }

    private boolean hasAnyJob(Player crafter) {
        for (String id : this.jobs) {
            Job job = Job.byId(id);
            if (job != null && JobEvents.has(crafter, job)) return true;
        }
        return false;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack made = super.assemble(input);
        Player crafter = CommonHooks.getCraftingPlayer();
        if (crafter != null && (crafter.isCreative() || hasAnyJob(crafter))) return made;
        if (this.othersCount <= 0) return ItemStack.EMPTY;
        made.setCount(this.othersCount);
        return made;
    }
}
