package com.alexh.capitalism.farmer;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.occupation.Job;
import com.alexh.capitalism.occupation.JobEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.List;

// Meat Slabs (Farmer.md > Butchering). A Farmer who kills a grown animal with a diamond or netherite sword or axe
// (#capitalism:butchering_blades) has a SLAB_CHANCE chance of it dropping a Meat Slab too, on top of its usual drops.
// Which slab: a second roll on the animal's slabs by weight (the bigger the cut, the rarer). Luck (the Angler's Almanac,
// Lucky Star, a Luck potion...) raises both: LUCK_CHANCE more chance per point, and the rarer slabs come up more
// (LUCK_RARE per point per step of rarity). A Bone set in the sword's Molten Socket (weaponsmith/Socketing) adds
// BONE_CHANCE. Butchery Basics doesn't change it.
// A Farmer cuts a slab up at the Butcher's Table (ButchersTableMenu, the butchery game): done well it gives YIELD of
// the cut itself (Raw Beef Rib...: for Cooks' recipes), botched it gives YIELD of the plain raw meat.
public enum MeatCut {
    BEEF_CHUCK(Animal.COW, "beef_chuck", "Beef Chuck", 35, 0, 3, Bones.BLADE, 3),
    BEEF_BRISKET(Animal.COW, "beef_brisket", "Beef Brisket", 35, 0, 3, Bones.SMALL, 3),
    BEEF_ROUND(Animal.COW, "beef_round", "Beef Round", 20, 1, 4, Bones.KNUCKLE, 4),
    BEEF_RIB(Animal.COW, "beef_rib", "Beef Rib", 10, 2, 5, Bones.RIBS, 5),
    PORK_SHOULDER(Animal.PIG, "pork_shoulder", "Pork Shoulder", 35, 0, 3, Bones.BLADE, 3),
    PORK_BELLY(Animal.PIG, "pork_belly", "Pork Belly", 35, 0, 3, Bones.SMALL, 3),
    PORK_HAM(Animal.PIG, "pork_ham", "Pork Ham", 20, 1, 4, Bones.KNUCKLE, 4),
    PORK_LOIN(Animal.PIG, "pork_loin", "Pork Loin", 10, 2, 5, Bones.SPINE, 5),
    MUTTON_SHOULDER(Animal.SHEEP, "mutton_shoulder", "Mutton Shoulder", 50, 0, 2, Bones.BLADE, 3),
    MUTTON_LEG(Animal.SHEEP, "mutton_leg", "Mutton Leg", 35, 1, 3, Bones.KNUCKLE, 4),
    MUTTON_RACK(Animal.SHEEP, "mutton_rack", "Mutton Rack", 15, 2, 4, Bones.RIBS, 5),
    CHICKEN_LEG_QUARTER(Animal.CHICKEN, "chicken_leg_quarter", "Chicken Leg Quarter", 65, 0, 2, Bones.KNUCKLE, 2),
    CHICKEN_BREAST(Animal.CHICKEN, "chicken_breast", "Chicken Breast", 35, 1, 3, Bones.SPINE, 3),
    RABBIT_HIND_LEGS(Animal.RABBIT, "rabbit_hind_legs", "Rabbit Hind Legs", 65, 0, 2, Bones.KNUCKLE, 2),
    RABBIT_SADDLE(Animal.RABBIT, "rabbit_saddle", "Rabbit Saddle", 35, 1, 3, Bones.SPINE, 3);

    public static final float SLAB_CHANCE = 0.15F;
    // a Bone in the sword's socket: this much more chance of a slab
    public static final float BONE_CHANCE = 0.15F;
    // each point of Luck: this much more chance of a slab...
    public static final float LUCK_CHANCE = 0.05F;
    public static final float MAX_CHANCE = 0.6F;
    // ...and the rarer slabs' weights go up by this share per point, per step of rarity
    public static final float LUCK_RARE = 0.25F;

    public static final TagKey<Item> BUTCHERING_BLADES = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Capitalism.MODID, "butchering_blades"));

    // The animals that drop slabs and the plain meat their slabs give
    public enum Animal {
        COW(Items.BEEF, List.of(EntityTypes.COW, EntityTypes.MOOSHROOM)),
        PIG(Items.PORKCHOP, List.of(EntityTypes.PIG, EntityTypes.HOGLIN)),
        SHEEP(Items.MUTTON, List.of(EntityTypes.SHEEP)),
        CHICKEN(Items.CHICKEN, List.of(EntityTypes.CHICKEN)),
        RABBIT(Items.RABBIT, List.of(EntityTypes.RABBIT));

        public final Item meat;
        private final List<EntityType<?>> types;

        Animal(Item meat, List<EntityType<?>> types) {
            this.meat = meat;
            this.types = types;
        }

        public static @Nullable Animal of(EntityType<?> type) {
            for (Animal animal : values()) {
                if (animal.types.contains(type)) return animal;
            }
            return null;
        }
    }

    // What the bones inside look like in the butchery game
    public enum Bones {
        // a shoulder blade: a fan of flat bones from one point
        BLADE,
        // a few small bones and chips (brisket, belly)
        SMALL,
        // one big leg bone with knobbly ends
        KNUCKLE,
        // long ribs running top to bottom: cut between them
        RIBS,
        // a row of vertebrae (or a keel bone): cut between them
        SPINE
    }

    public final Animal animal;
    public final String id;
    public final String title;
    public final int weight;
    // 0 the most common slab of its animal, up to 2 the rarest
    public final int rarity;
    public final int yield;
    public final Bones bones;
    // how many bones are in it (the butchery game)
    public final int boneCount;

    MeatCut(Animal animal, String id, String title, int weight, int rarity, int yield, Bones bones, int boneCount) {
        this.animal = animal;
        this.id = id;
        this.title = title;
        this.weight = weight;
        this.rarity = rarity;
        this.yield = yield;
        this.bones = bones;
        this.boneCount = boneCount;
    }

    public Item slab() {
        return ModItems.MEAT_SLABS.get(this).get();
    }

    public Item cut() {
        return ModItems.MEAT_CUTS.get(this).get();
    }

    // What cutting it gives: the cut itself if it went well, plain raw meat if not
    public ItemStack result(boolean success) {
        return new ItemStack(success ? this.cut() : this.animal.meat, this.yield);
    }

    public static @Nullable MeatCut ofSlab(ItemStack stack) {
        for (MeatCut cut : values()) {
            if (stack.is(cut.slab())) return cut;
        }
        return null;
    }

    public static boolean isCut(ItemStack stack) {
        for (MeatCut cut : values()) {
            if (stack.is(cut.cut())) return true;
        }
        return false;
    }

    // The chance of a slab at this much Luck
    public static float chance(double luck) {
        return chance(luck, false);
    }

    // ... with a Bone in the sword's socket or not
    public static float chance(double luck, boolean bone) {
        return Math.min(MAX_CHANCE, SLAB_CHANCE + (bone ? BONE_CHANCE : 0) + LUCK_CHANCE * (float) Math.max(0, luck));
    }

    // This slab's weight at this much Luck
    public float weight(double luck) {
        return this.weight * (1 + LUCK_RARE * (float) Math.max(0, luck) * this.rarity);
    }

    // Which of the animal's slabs drops
    public static MeatCut pick(Animal animal, double luck, RandomSource random) {
        float total = 0;
        for (MeatCut cut : values()) {
            if (cut.animal == animal) total += cut.weight(luck);
        }
        float roll = random.nextFloat() * total;
        MeatCut last = null;
        for (MeatCut cut : values()) {
            if (cut.animal != animal) continue;
            last = cut;
            roll -= cut.weight(luck);
            if (roll < 0) return cut;
        }
        return last;
    }

    // The slab this kill drops, if any: a Farmer, a grown animal on the list, a butchering blade in hand, and the roll
    public static @Nullable ItemStack slabDrop(@Nullable Player killer, LivingEntity mob, RandomSource random) {
        if (killer == null || mob.isBaby() || !JobEvents.has(killer, Job.FARMER)) return null;
        Animal animal = Animal.of(mob.getType());
        if (animal == null || !killer.getMainHandItem().is(BUTCHERING_BLADES)) return null;
        double luck = killer.getAttributeValue(Attributes.LUCK);
        boolean bone = com.alexh.capitalism.weaponsmith.Socketing.gem(killer.getMainHandItem()) == com.alexh.capitalism.weaponsmith.Socketing.Gem.BONE;
        if (random.nextFloat() >= chance(luck, bone)) return null;
        return Freshness.newSlab(pick(animal, luck, random), mob.level().getGameTime(), random);
    }
}
