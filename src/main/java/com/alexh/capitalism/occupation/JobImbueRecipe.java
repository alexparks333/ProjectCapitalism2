package com.alexh.capitalism.occupation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.ImbueRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.common.CommonHooks;

// Vanilla's "imbue" recipe (8 Arrows around a Lingering Potion = 8 Tipped Arrows), except that a player with "job" gets
// twice as many ("type": "capitalism:job_imbue"; replaces data/minecraft/recipe/tipped_arrow.json). Like the 4-for-2
// plain and Spectral Arrows, a Weaponsmith makes double. A Crafter block counts as "everyone else".
public class JobImbueRecipe extends ImbueRecipe {

    public static final MapCodec<JobImbueRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(
                Recipe.CommonInfo.MAP_CODEC.forGetter(o -> o.info),
                CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(o -> o.book),
                Ingredient.CODEC.fieldOf("source").forGetter(o -> o.sourceIngredient),
                Ingredient.CODEC.fieldOf("material").forGetter(o -> o.materialIngredient),
                ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.resultTemplate),
                Codec.STRING.fieldOf("job").forGetter(o -> o.job)
            )
            .apply(i, JobImbueRecipe::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, JobImbueRecipe> STREAM_CODEC = StreamCodec.composite(
        Recipe.CommonInfo.STREAM_CODEC, o -> o.info,
        CraftingRecipe.CraftingBookInfo.STREAM_CODEC, o -> o.book,
        Ingredient.CONTENTS_STREAM_CODEC, o -> o.sourceIngredient,
        Ingredient.CONTENTS_STREAM_CODEC, o -> o.materialIngredient,
        ItemStackTemplate.STREAM_CODEC, o -> o.resultTemplate,
        ByteBufCodecs.STRING_UTF8, o -> o.job,
        JobImbueRecipe::new
    );
    public static final RecipeSerializer<JobImbueRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    // our own copies: the parent keeps its fields private
    private final Recipe.CommonInfo info;
    private final CraftingRecipe.CraftingBookInfo book;
    private final Ingredient sourceIngredient;
    private final Ingredient materialIngredient;
    private final ItemStackTemplate resultTemplate;
    private final String job;

    public JobImbueRecipe(Recipe.CommonInfo info, CraftingRecipe.CraftingBookInfo book, Ingredient source, Ingredient material,
                          ItemStackTemplate result, String job) {
        super(info, book, source, material, result);
        this.info = info;
        this.book = book;
        this.sourceIngredient = source;
        this.materialIngredient = material;
        this.resultTemplate = result;
        this.job = job;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public RecipeSerializer<ImbueRecipe> getSerializer() {
        return (RecipeSerializer) SERIALIZER;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack made = super.assemble(input);
        Player crafter = CommonHooks.getCraftingPlayer();
        Job needed = Job.byId(this.job);
        if (crafter != null && needed != null && (crafter.isCreative() || JobEvents.has(crafter, needed))) {
            made.setCount(made.getCount() * 2);
        }
        return made;
    }
}
