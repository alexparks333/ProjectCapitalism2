package com.alexh.capitalism.core;

import com.alexh.capitalism.Capitalism;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.resources.Identifier;
import com.alexh.capitalism.librarian.Skill;
import java.util.EnumMap;
import java.util.Map;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// Our own potion effects (shown with the vanilla ones, icon in textures/mob_effect/).
public final class ModEffects {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Capitalism.MODID);

    // Lava Swimmer (Fiery Mushroom Bowl): move 3x faster through lava. The movement itself is in player/MealEffects.
    public static final DeferredHolder<MobEffect, MobEffect> LAVA_SWIMMER = MOB_EFFECTS.register("lava_swimmer",
        () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xFF7A1E) {
        });

    // Ender Leap (Nether Dragon Pot Pie): sneak + hold jump to blink a few blocks forward, for a little hunger.
    // See player/MealEffects (server) and client/ClientMealEffects (the keys).
    public static final DeferredHolder<MobEffect, MobEffect> ENDER_LEAP = MOB_EFFECTS.register("ender_leap",
        () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x8E3BD6) {
        });

    // Green Thumb Aura (Hearty Harvest Loaf): crops and saplings around you grow like they're being bone-mealed.
    // See player/MealEffects.
    public static final DeferredHolder<MobEffect, MobEffect> GREEN_THUMB = MOB_EFFECTS.register("green_thumb",
        () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x59C93C) {
        });

    // Scholar's Focus (Scholar's Brain Stew): mobs you kill drop double experience. See player/MealEffects.
    public static final DeferredHolder<MobEffect, MobEffect> SCHOLARS_FOCUS = MOB_EFFECTS.register("scholars_focus",
        () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x7FD34E) {
        });

    // Bleeding (a Flint-socketed axe, weaponsmith/SocketEffects): 1 damage a second while it lasts.
    public static final DeferredHolder<MobEffect, MobEffect> BLEEDING = MOB_EFFECTS.register("bleeding",
        () -> new MobEffect(MobEffectCategory.HARMFUL, 0x8A0E0E) {
            @Override
            public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
                return duration % 20 == 0;
            }

            @Override
            public boolean applyEffectTick(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.LivingEntity mob, int amplification) {
                mob.hurtServer(level, level.damageSources().generic(), 1.0F);
                return true;
            }
        });

    // Warmed Up (Campfire Chili): the cold can't reach you. Cooled Off (Frosty Melon Sorbet): you can't get hot.
    // See player/BodyTemperature.
    public static final DeferredHolder<MobEffect, MobEffect> WARMED_UP = MOB_EFFECTS.register("warmed_up",
        () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xE85C1C) {
        });
    public static final DeferredHolder<MobEffect, MobEffect> COOLED_OFF = MOB_EFFECTS.register("cooled_off",
        () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x96D6FF) {
        });

    // ── Bartender cocktails (see player/DrinkEffects and client/TreasureSightClient) ──
    // Luminescent Aura (Glowberry Gold Rush): a bright light follows you around.
    public static final DeferredHolder<MobEffect, MobEffect> LUMINESCENT_AURA = MOB_EFFECTS.register("luminescent_aura",
        () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xFFE070) {
        });
    // Magma Trail (Nether Fireball Shot): you leave harmless flames behind that set hostile mobs on fire.
    public static final DeferredHolder<MobEffect, MobEffect> MAGMA_TRAIL = MOB_EFFECTS.register("magma_trail",
        () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xE8501C) {
        });
    // Treasure Sight (Pirate's High Seas Grog): unopened buried-treasure and shipwreck chests glow through walls.
    public static final DeferredHolder<MobEffect, MobEffect> TREASURE_SIGHT = MOB_EFFECTS.register("treasure_sight",
        () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x3CC8C8) {
        });
    // Fortune Boost (Miner's Moonshine): ores sometimes drop an extra item, on top of Fortune.
    public static final DeferredHolder<MobEffect, MobEffect> FORTUNE_BOOST = MOB_EFFECTS.register("fortune_boost",
        () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x4AE8D0) {
        });
    // Life Siphon (Vampiric Mulled Wine): melee hits on hostile mobs heal you.
    public static final DeferredHolder<MobEffect, MobEffect> LIFE_SIPHON = MOB_EFFECTS.register("life_siphon",
        () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x9C1028) {
        });
    // Phantom Camouflage (Phantom's Nightcap): phantoms can't target you.
    public static final DeferredHolder<MobEffect, MobEffect> PHANTOM_CAMOUFLAGE = MOB_EFFECTS.register("phantom_camouflage",
        () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x6E7CB4) {
        });
    // Iron Stomach (Zombie's Hangover Cure): no Hunger (food poisoning), and Cooks can eat raw food.
    public static final DeferredHolder<MobEffect, MobEffect> IRON_STOMACH = MOB_EFFECTS.register("iron_stomach",
        () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x8A9098) {
        });

    // Spatial Dislocation (Ender Twilight Fizz): a heavy or fatal hit teleports you a short way away (a fatal one is
    // cancelled). See player/DrinkEffects.
    public static final DeferredHolder<MobEffect, MobEffect> SPATIAL_DISLOCATION = MOB_EFFECTS.register("spatial_dislocation",
        () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xA45CE0) {
        });
    // Berserker's Rage (Drunken Brawler's Stout): you can't be knocked back. Comes with Strength II and Nausea (the
    // drink gives those).
    public static final DeferredHolder<MobEffect, MobEffect> BERSERKERS_RAGE = MOB_EFFECTS.register("berserkers_rage",
        () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xB0281C) {
        }.addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, Identifier.fromNamespaceAndPath(Capitalism.MODID, "berserkers_rage"),
            1.0, AttributeModifier.Operation.ADD_VALUE));

    // ── Pet buffs (pets/PetTraits, the Camel) ──
    // Refill Thirst Bar: instant, fills the thirst bar to the top (not past it).
    public static final DeferredHolder<MobEffect, MobEffect> REFILL_THIRST = MOB_EFFECTS.register("refill_thirst",
        () -> new net.minecraft.world.effect.InstantaneousMobEffect(MobEffectCategory.BENEFICIAL, 0x3C8CE6) {
            @Override
            public boolean applyEffectTick(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.LivingEntity mob, int amplification) {
                if (mob instanceof net.minecraft.world.entity.player.Player player) {
                    com.alexh.capitalism.player.ThirstData thirst = player.getData(ModAttachments.THIRST.get());
                    thirst.restore(Math.max(0f, thirst.thirstCap - thirst.getThirstF()));
                }
                return true;
            }
        });
    // No Water Draining: the thirst bar doesn't go down (core/ModServerEvents).
    public static final DeferredHolder<MobEffect, MobEffect> NO_WATER_DRAINING = MOB_EFFECTS.register("no_water_draining",
        () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xE8B83C) {
        });

    // ── Skill book lessons (librarian/Skill, not the traits): one effect each, "book_running_club" ... An attribute
    // lesson carries its modifier; the rest are done in librarian/SkillEffects. A Delicate Book gives level II (twice as
    // much). Hidden from the vanilla effect icons (client/ClientModEvents): they show in Wellness > Buffs & Debuffs. ──
    public static final Map<Skill, DeferredHolder<MobEffect, MobEffect>> BOOK_LESSONS = new EnumMap<>(Skill.class);

    static {
        for (Skill skill : Skill.values()) {
            if (skill.trait || skill.instant) continue;
            BOOK_LESSONS.put(skill, MOB_EFFECTS.register("book_" + skill.id(), () -> {
                MobEffect effect = new MobEffect(MobEffectCategory.BENEFICIAL, skill.color) {
                };
                return skill.attribute == null ? effect : effect.addAttributeModifier(skill.attribute, skill.modifierId(), skill.amount, skill.operation);
            }));
        }
    }

    private ModEffects() {
    }
}
