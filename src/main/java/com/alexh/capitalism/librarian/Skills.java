package com.alexh.capitalism.librarian;

import com.alexh.capitalism.core.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

// What skill books a player has going right now (both sides: lessons are effects, traits a synced attachment).
public final class Skills {

    private Skills() {
    }

    // 0 = not at all, 1 = a lesson at level I or a trait, 2 = a lesson at level II (from a Delicate Book)
    public static int level(@Nullable Entity entity, Skill skill) {
        if (!(entity instanceof Player player)) return 0;
        if (skill.trait) return Traits.of(player).has(skill) ? 1 : 0;
        if (skill.instant) return 0;
        MobEffectInstance effect = player.getEffect(ModEffects.BOOK_LESSONS.get(skill));
        return effect == null ? 0 : effect.getAmplifier() + 1;
    }

    public static boolean has(@Nullable Entity entity, Skill skill) {
        return level(entity, skill) > 0;
    }

    // How fast thirst drains (core/ModServerEvents): Water Rationing takes 25% off per level, Camel another 25%
    public static float thirstDrainMultiplier(Player player) {
        float multiplier = 1.0F - 0.25F * level(player, Skill.WATER_RATIONING);
        if (has(player, Skill.CAMEL)) multiplier *= 0.75F;
        return multiplier;
    }

    // A lesson's lesson effect, for the Buffs & Debuffs tab: which skill an effect is, or null
    public static @Nullable Skill lessonOf(MobEffectInstance effect) {
        for (var entry : ModEffects.BOOK_LESSONS.entrySet()) {
            if (effect.is(entry.getValue())) return entry.getKey();
        }
        return null;
    }
}
