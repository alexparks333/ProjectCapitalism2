package com.alexh.capitalism.pets;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;

import java.util.List;
import java.util.Map;

// What each kind of pet gives its owner (PetTrainer.md > Traits): the first time you pet it each Minecraft day you get its
// buff (Pets.onInteract). Everyday farm animals give a small one; animals that are harder to find or tame give better
// ones (Panda, Goat, Polar Bear, Sniffer...). Shown on the pet window's Traits tab (client/PetScreen). An animal not in the
// list gets the default.
public final class PetTraits {

    public enum Tier {
        SMALL("Small buff", 0xFFB8AE98),
        GOOD("Good buff", 0xFF8AD06A),
        GREAT("Great buff", 0xFFE8C14A);

        public final String title;
        public final int color;

        Tier(String title, int color) {
            this.title = title;
            this.color = color;
        }
    }

    // One effect: which, its level (0 = I), for how many minutes (0: an instant one, like Refill Thirst Bar)
    public record Buff(Holder<MobEffect> effect, int amplifier, double minutes) {
        public MobEffectInstance instance() {
            return new MobEffectInstance(this.effect, Math.max(1, (int) (this.minutes * 60 * 20)), this.amplifier);
        }
    }

    public record Trait(Tier tier, List<Buff> buffs) {
    }

    private static Trait small(Holder<MobEffect> effect, int amplifier, double minutes) {
        return new Trait(Tier.SMALL, List.of(new Buff(effect, amplifier, minutes)));
    }

    private static Trait good(Holder<MobEffect> effect, int amplifier, double minutes) {
        return new Trait(Tier.GOOD, List.of(new Buff(effect, amplifier, minutes)));
    }

    private static Trait great(Buff... buffs) {
        return new Trait(Tier.GREAT, List.of(buffs));
    }

    public static final Trait DEFAULT = small(MobEffects.REGENERATION, 0, 1);

    private static final Map<EntityType<?>, Trait> TRAITS = Map.ofEntries(
        // everyday farm animals: small
        Map.entry(EntityTypes.PIG, small(MobEffects.ABSORPTION, 0, 5)),
        Map.entry(EntityTypes.COW, small(MobEffects.REGENERATION, 0, 1)),
        Map.entry(EntityTypes.SHEEP, small(MobEffects.RESISTANCE, 0, 3)),
        Map.entry(EntityTypes.CHICKEN, small(MobEffects.SLOW_FALLING, 0, 5)),
        Map.entry(EntityTypes.RABBIT, small(MobEffects.JUMP_BOOST, 0, 5)),
        // tamed or a bit harder to come by: good
        Map.entry(EntityTypes.WOLF, good(MobEffects.STRENGTH, 0, 2.5)),
        Map.entry(EntityTypes.CAT, good(MobEffects.NIGHT_VISION, 0, 2.5)),
        Map.entry(EntityTypes.OCELOT, good(MobEffects.SPEED, 0, 5)),
        Map.entry(EntityTypes.PARROT, good(MobEffects.LUCK, 0, 5)),
        Map.entry(EntityTypes.BEE, good(MobEffects.HASTE, 0, 5)),
        Map.entry(EntityTypes.FROG, good(MobEffects.JUMP_BOOST, 1, 5)),
        Map.entry(EntityTypes.TURTLE, good(MobEffects.WATER_BREATHING, 0, 5)),
        Map.entry(EntityTypes.LLAMA, good(MobEffects.RESISTANCE, 0, 3)),
        // the Camel: fills your thirst bar at once, then it doesn't go down for a while
        Map.entry(EntityTypes.CAMEL, new Trait(Tier.GOOD, List.of(new Buff(com.alexh.capitalism.core.ModEffects.REFILL_THIRST, 0, 0), new Buff(com.alexh.capitalism.core.ModEffects.NO_WATER_DRAINING, 0, 3)))),
        Map.entry(EntityTypes.STRIDER, good(MobEffects.FIRE_RESISTANCE, 0, 5)),
        Map.entry(EntityTypes.ARMADILLO, good(MobEffects.RESISTANCE, 1, 5)),
        Map.entry(EntityTypes.AXOLOTL, good(MobEffects.CONDUIT_POWER, 0, 5)),
        // hard to tame, rare, or far away: great
        Map.entry(EntityTypes.PANDA, great(new Buff(MobEffects.HEALTH_BOOST, 0, 3.5), new Buff(MobEffects.REGENERATION, 0, 4))),
        Map.entry(EntityTypes.GOAT, great(new Buff(MobEffects.STRENGTH, 0, 5), new Buff(MobEffects.JUMP_BOOST, 1, 7.5))),
        Map.entry(EntityTypes.POLAR_BEAR, great(new Buff(MobEffects.STRENGTH, 0, 7.5), new Buff(MobEffects.RESISTANCE, 0, 7.5))),
        Map.entry(EntityTypes.SNIFFER, great(new Buff(MobEffects.LUCK, 1, 7.5), new Buff(MobEffects.HASTE, 1, 7.5))),
        Map.entry(EntityTypes.FOX, great(new Buff(MobEffects.SPEED, 1, 4))),
        Map.entry(EntityTypes.MOOSHROOM, great(new Buff(MobEffects.HEALTH_BOOST, 0, 7.5), new Buff(MobEffects.SATURATION, 0, 0.05))),
        Map.entry(EntityTypes.HOGLIN, great(new Buff(MobEffects.STRENGTH, 1, 7.5))),
        Map.entry(EntityTypes.TRADER_LLAMA, great(new Buff(MobEffects.HERO_OF_THE_VILLAGE, 0, 7.5))),
        Map.entry(EntityTypes.HAPPY_GHAST, great(new Buff(MobEffects.SLOW_FALLING, 0, 7.5), new Buff(MobEffects.REGENERATION, 0, 2))),
        Map.entry(EntityTypes.NAUTILUS, great(new Buff(MobEffects.CONDUIT_POWER, 0, 7.5), new Buff(MobEffects.DOLPHINS_GRACE, 0, 5)))
    );

    private PetTraits() {
    }

    // Pets that vanilla gives no food (they can't be bred) eat these instead
    private static final Map<EntityType<?>, List<net.minecraft.world.item.Item>> EXTRA_FOOD = Map.of(
        EntityTypes.POLAR_BEAR, List.of(net.minecraft.world.item.Items.COD, net.minecraft.world.item.Items.SALMON),
        EntityTypes.PARROT, List.of(net.minecraft.world.item.Items.WHEAT_SEEDS, net.minecraft.world.item.Items.MELON_SEEDS,
            net.minecraft.world.item.Items.PUMPKIN_SEEDS, net.minecraft.world.item.Items.BEETROOT_SEEDS,
            net.minecraft.world.item.Items.TORCHFLOWER_SEEDS, net.minecraft.world.item.Items.PITCHER_POD));

    // Pets that vanilla feeds a bucket of fish eat the plain fish instead (only that)
    private static final Map<EntityType<?>, net.minecraft.world.item.Item> FISH_INSTEAD = Map.of(
        EntityTypes.AXOLOTL, net.minecraft.world.item.Items.TROPICAL_FISH,
        EntityTypes.NAUTILUS, net.minecraft.world.item.Items.PUFFERFISH);

    // Does this pet eat that? Its vanilla food (what it breeds with), or the extra food above
    public static boolean eats(net.minecraft.world.entity.animal.Animal animal, net.minecraft.world.item.ItemStack stack) {
        net.minecraft.world.item.Item fish = FISH_INSTEAD.get(animal.getType());
        if (fish != null) return stack.is(fish);
        return animal.isFood(stack) || EXTRA_FOOD.getOrDefault(animal.getType(), List.of()).contains(stack.getItem());
    }

    // every animal with its own trait (for the tests)
    public static java.util.Set<EntityType<?>> listed() {
        return TRAITS.keySet();
    }

    public static Trait of(EntityType<?> type) {
        return TRAITS.getOrDefault(type, DEFAULT);
    }
}
