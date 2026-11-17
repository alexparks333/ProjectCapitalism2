package com.alexh.capitalism.core;

import com.alexh.capitalism.Capitalism;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// Our own item data.
public class ModComponents {

    public static final DeferredRegister.DataComponents COMPONENTS =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Capitalism.MODID);

    // Hits a sword has landed since it was last sharpened (see weaponsmith/Sharpness). Missing = 0 = razor sharp.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SWORD_HITS =
        COMPONENTS.registerComponentType("sword_hits", builder -> builder
            .persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT));

    // A forged sword's damage, as a % of normal (rolled when it's crafted, see weaponsmith/SwordForging). Missing = 100.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SWORD_SHARPNESS =
        COMPONENTS.registerComponentType("sword_sharpness", builder -> builder
            .persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT));

    // Vanilla food made by a Cook (cooked in a Furnace / Smoker, or crafted): full strength, counts as Gourmet
    // (occupation/CookMadeFood). Missing = ordinary food.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> COOK_MADE =
        COMPONENTS.registerComponentType("cook_made", builder -> builder
            .persistent(Codec.BOOL)
            .networkSynchronized(ByteBufCodecs.BOOL));

    // A Molten Socket on a sword, pickaxe or armor piece (weaponsmith/Socketing): "empty", or the gem in it ("lapis",
    // "redstone"). Missing = no socket.
    // the Traveler's Bag (designer/Bag.TRAVELERS_BAG) is opened out to 12 slots (far from town)
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> EXPANDED =
        COMPONENTS.registerComponentType("expanded", builder -> builder
            .persistent(Codec.BOOL)
            .networkSynchronized(ByteBufCodecs.BOOL));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> SOCKET =
        COMPONENTS.registerComponentType("socket", builder -> builder
            .persistent(Codec.STRING)
            .networkSynchronized(ByteBufCodecs.STRING_UTF8));

    // Blocks a pickaxe has mined since it was last sharpened (see weaponsmith/Wear). Missing = 0 = sharp.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> PICKAXE_WEAR =
        COMPONENTS.registerComponentType("pickaxe_wear", builder -> builder
            .persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT));

    // Hits an armor piece has taken since it was last fixed at an Armor Whetstone (see weaponsmith/Wear). Missing = 0.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ARMOR_DENTS =
        COMPONENTS.registerComponentType("armor_dents", builder -> builder
            .persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT));

    // A forged tool's mining speed, as a % of normal (pickaxes, shovels, axes; see weaponsmith/ToolForging). Missing = 100.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> TOOL_EFFICIENCY =
        COMPONENTS.registerComponentType("tool_efficiency", builder -> builder
            .persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT));

    // Meat that rots (farmer/Freshness): the game tick it turns to Rotten Meat, and a Meat Slab's lean % (60-100)
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> ROTS_AT =
        COMPONENTS.registerComponentType("rots_at", builder -> builder
            .persistent(Codec.LONG)
            .networkSynchronized(ByteBufCodecs.VAR_LONG));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> LEAN =
        COMPONENTS.registerComponentType("lean", builder -> builder
            .persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT));

    // Meat being kept in a running Fridge / Freezer (farmer/Chill): only for its tooltip
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.alexh.capitalism.farmer.Chill>> CHILL =
        COMPONENTS.registerComponentType("chill", builder -> builder
            .persistent(com.alexh.capitalism.farmer.Chill.CODEC)
            .networkSynchronized(com.alexh.capitalism.farmer.Chill.STREAM_CODEC));

    // A fine cut's dry-aging (farmer/Freshness): ticks it's spent in a running Fridge
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> AGING =
        COMPONENTS.registerComponentType("aging", builder -> builder
            .persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT));
    // A steak seared on the Grill (farmer/Sear): doneness, what was ordered, crust, aging
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.alexh.capitalism.farmer.Sear>> SEAR =
        COMPONENTS.registerComponentType("sear", builder -> builder
            .persistent(com.alexh.capitalism.farmer.Sear.CODEC)
            .networkSynchronized(com.alexh.capitalism.farmer.Sear.STREAM_CODEC));

    // A Butcher Knife's cuts used since it was last sharpened (farmer/ButcherKnifeItem). Missing = razor sharp.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> KNIFE_CUTS =
        COMPONENTS.registerComponentType("knife_cuts", builder -> builder
            .persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT));

    // A forged sword's edge: hits it lands before it goes dull (rolled when it's crafted). Missing = the config default.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SWORD_EDGE =
        COMPONENTS.registerComponentType("sword_edge", builder -> builder
            .persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT));

    // An Apprentice item's armor set (enchanter/Apprentice.Kind name, e.g. "DIAMOND"). Missing = an empty Apprentice.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> APPRENTICE_KIND =
        COMPONENTS.registerComponentType("apprentice_kind", builder -> builder
            .persistent(Codec.STRING)
            .networkSynchronized(ByteBufCodecs.STRING_UTF8));

    // The XP points an Apprentice item is holding. Missing = 0.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> APPRENTICE_XP =
        COMPONENTS.registerComponentType("apprentice_xp", builder -> builder
            .persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT));

    // A written skill book: what it teaches and whether it's Delicate (librarian/SkillBookItem). Missing = not
    // written yet (the Librarian Table's preview).
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.alexh.capitalism.librarian.SkillBook>> SKILL_BOOK =
        COMPONENTS.registerComponentType("skill_book", builder -> builder
            .persistent(com.alexh.capitalism.librarian.SkillBook.CODEC)
            .networkSynchronized(com.alexh.capitalism.librarian.SkillBook.STREAM_CODEC));

    // A Knowledge Piece's quality (librarian/KnowledgeQuality ordinal: 1 Clear, 2 Bright, 3 Radiant). Missing = Faded.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> KNOWLEDGE_QUALITY =
        COMPONENTS.registerComponentType("knowledge_quality", builder -> builder
            .persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT));

    // The animal in an Animal Bjorn (pets/AnimalBjornItem, pets/CarriedAnimal)
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.alexh.capitalism.pets.CarriedAnimal>> CARRIED_ANIMAL =
        COMPONENTS.registerComponentType("carried_animal", builder -> builder
            .persistent(com.alexh.capitalism.pets.CarriedAnimal.CODEC)
            .networkSynchronized(com.alexh.capitalism.pets.CarriedAnimal.STREAM_CODEC));

    // A rare catch's weight, catcher and day (fishing/CatchInfo), on the catch and on its wall plaque
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.alexh.capitalism.fishing.CatchInfo>> CATCH_INFO =
        COMPONENTS.registerComponentType("catch_info", builder -> builder
            .persistent(com.alexh.capitalism.fishing.CatchInfo.CODEC)
            .networkSynchronized(com.alexh.capitalism.fishing.CatchInfo.STREAM_CODEC));

    // What a Fish Plaque holds (fishing/RareCatch id)
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> PLAQUE_CATCH =
        COMPONENTS.registerComponentType("plaque_catch", builder -> builder
            .persistent(Codec.STRING)
            .networkSynchronized(ByteBufCodecs.STRING_UTF8));

    // A Soggy Note's gold (nuggets), redeemed by the Fisherman (fishing/SoggyNoteItem)
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> NOTE_GOLD =
        COMPONENTS.registerComponentType("note_gold", builder -> builder
            .persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT));
}
