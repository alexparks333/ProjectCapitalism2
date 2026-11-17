package com.alexh.capitalism.core;

import com.alexh.capitalism.Capitalism;
import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.warden.Warden;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

// A harder world (NPCs.md > Mob levels): every hostile mob spawns at a level, rolled once and saved with it.
//  - Level I (most): 20% more max health (hostileMobHealthMultiplier), normal hits.
//  - Level II (level2MobChance, 30%): 30% more health, hits 1.25x as hard, 1.4x the XP.
//  - Boss (bossMobChance, 10%): twice the health, hits 1.75x as hard, double loot and XP; and by kind: a skeleton shoots
//    1.5x as fast, a zombie walks 1.5x as fast, a spider poisons (a creeper is just tougher).
// The real bosses (Wither, Ender Dragon, Warden, Elder Guardian) are always Level I. Animals, villagers and golems stay
// normal. The level IS the max-health modifier (each level its own id), a permanent attribute modifier saved with the
// mob and sent to every player's game with its attributes, so the client knows it too (client/MobLevels shows I, II
// or a skull). The harder hits are applied to whatever the mob does to anyone: a bite, an arrow, a creeper's blast.
@EventBusSubscriber(modid = Capitalism.MODID)
public final class TougherMobs {

    public enum Level {
        ONE(Identifier.fromNamespaceAndPath(Capitalism.MODID, "tougher_mobs"), 1.0F),
        TWO(Identifier.fromNamespaceAndPath(Capitalism.MODID, "mob_level_2"), 1.25F),
        BOSS(Identifier.fromNamespaceAndPath(Capitalism.MODID, "mob_level_boss"), 1.75F);

        public final Identifier modifier;
        public final float damage;

        Level(Identifier modifier, float damage) {
            this.modifier = modifier;
            this.damage = damage;
        }
    }

    private static final Identifier BOSS_SPEED = Identifier.fromNamespaceAndPath(Capitalism.MODID, "boss_zombie_speed");

    private TougherMobs() {
    }

    // this mob's level (by its max-health modifier), or null for anything without one; works on both sides
    public static Level levelOf(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return null;
        AttributeInstance maxHealth = living.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) return null;
        if (maxHealth.hasModifier(Level.BOSS.modifier)) return Level.BOSS;
        if (maxHealth.hasModifier(Level.TWO.modifier)) return Level.TWO;
        if (maxHealth.hasModifier(Level.ONE.modifier)) return Level.ONE;
        return null;
    }

    // A mob going through a portal is made again in the new dimension: remember its level and health on the way out, so a
    // Boss stays a Boss (and keeps its wounds) on the other side instead of rolling a fresh level at full health.
    private record Traveling(Level level, float health) {
    }

    private static final java.util.Map<java.util.UUID, Traveling> TRAVELING = new java.util.concurrent.ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onTravel(net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof Mob mob && levelOf(mob) != null) TRAVELING.put(mob.getUUID(), new Traveling(levelOf(mob), mob.getHealth()));
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob) || !(mob instanceof Enemy)) return;
        Traveling traveled = TRAVELING.remove(mob.getUUID());
        if (traveled != null) {
            setLevel(mob, traveled.level());
            mob.setHealth(Math.min(mob.getMaxHealth(), traveled.health()));
            return;
        }
        AttributeInstance maxHealth = mob.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null || levelOf(mob) != null) return;   // already has its level (loaded from the save)
        if (mob instanceof com.alexh.capitalism.event.BloodMoonBoss) return;   // a boss of its own, no level
        boolean realBoss = mob instanceof WitherBoss || mob instanceof EnderDragon || mob instanceof Warden || mob instanceof ElderGuardian;
        double roll = mob.getRandom().nextDouble();
        // a Blood Moon night (event/BloodMoon): 20% Level I, 60% Level II, 20% Boss
        boolean blood = com.alexh.capitalism.event.BloodMoon.isActive(mob.level());
        double boss = blood ? com.alexh.capitalism.event.BloodMoon.BOSS_CHANCE : chance(true);
        double two = blood ? com.alexh.capitalism.event.BloodMoon.LEVEL_TWO_CHANCE : chance(false);
        Level level = realBoss ? Level.ONE : roll < boss ? Level.BOSS : roll < boss + two ? Level.TWO : Level.ONE;
        setLevel(mob, level);
    }

    // gives a mob this level (its health scaled with it); for spawning and for tests
    public static void setLevel(Mob mob, Level level) {
        AttributeInstance maxHealth = mob.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) return;
        for (Level l : Level.values()) maxHealth.removeModifier(l.modifier);
        double extra = switch (level) {
            case ONE -> multiplier() - 1.0;
            case TWO -> 0.3;
            case BOSS -> 1.0;
        };
        float share = mob.getHealth() / mob.getMaxHealth();
        maxHealth.addPermanentModifier(new AttributeModifier(level.modifier, extra, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        // a fresh mob starts full: scale its current health up with the max
        mob.setHealth(mob.getMaxHealth() * share);
        // a Boss zombie (husks, drowned, zombie villagers and zombified piglins too) walks 1.5x as fast
        AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(BOSS_SPEED);
            if (level == Level.BOSS && mob instanceof net.minecraft.world.entity.monster.zombie.Zombie) {
                speed.addPermanentModifier(new AttributeModifier(BOSS_SPEED, 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }
        // a Boss skeleton shoots 1.5x as fast (mixin/AbstractSkeletonMixin, when it picks its weapon goal)
        if (mob instanceof net.minecraft.world.entity.monster.skeleton.AbstractSkeleton skeleton) skeleton.reassessWeaponGoal();
    }

    // Level II and Boss mobs hit harder: anything they do (a bite, their arrow, their blast) does more
    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity() != null ? source.getEntity() : source.getDirectEntity();
        if (!(attacker instanceof Enemy)) return;
        Level level = levelOf(attacker);
        if (level != null && level.damage != 1.0F) event.setAmount(event.getAmount() * level.damage);
    }

    // A Boss spider (cave spiders too) poisons what it bites: Poison for 7 seconds, on top of the hit
    @SubscribeEvent
    public static void onHurt(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
        if (event.getInflictedDamage() <= 0) return;
        Entity attacker = event.getSource().getDirectEntity();
        if (attacker instanceof net.minecraft.world.entity.monster.spider.Spider && levelOf(attacker) == Level.BOSS) {
            event.getEntity().addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 140, 0), attacker);
        }
    }

    // More XP: a Boss gives twice as much, a Level II 1.4x; on a Blood Moon night every monster's XP is doubled again
    @SubscribeEvent
    public static void onXp(net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent event) {
        if (!(event.getEntity() instanceof Enemy)) return;
        Level level = levelOf(event.getEntity());
        float times = level == Level.BOSS ? 2.0F : level == Level.TWO ? 1.4F : 1.0F;
        if (com.alexh.capitalism.event.BloodMoon.isActive(event.getEntity().level())) times *= 2.0F;
        if (times != 1.0F) event.setDroppedExperience(Math.round(event.getDroppedExperience() * times));
    }

    // A Boss drops double loot: every item it drops, twice over
    @SubscribeEvent
    public static void onDrops(net.neoforged.neoforge.event.entity.living.LivingDropsEvent event) {
        if (levelOf(event.getEntity()) != Level.BOSS || !(event.getEntity() instanceof Enemy)) return;
        java.util.List<net.minecraft.world.entity.item.ItemEntity> extra = new java.util.ArrayList<>();
        for (net.minecraft.world.entity.item.ItemEntity drop : event.getDrops()) {
            net.minecraft.world.entity.item.ItemEntity copy = new net.minecraft.world.entity.item.ItemEntity(drop.level(), drop.getX(), drop.getY(), drop.getZ(),
                drop.getItem().copy());
            copy.setDefaultPickUpDelay();
            extra.add(copy);
        }
        event.getDrops().addAll(extra);
    }

    private static double multiplier() {
        try {
            return CapitalismConfig.HOSTILE_MOB_HEALTH_MULTIPLIER.get();
        } catch (IllegalStateException notLoaded) {
            return 1.2;
        }
    }

    private static double chance(boolean boss) {
        try {
            return boss ? CapitalismConfig.BOSS_MOB_CHANCE.get() : CapitalismConfig.LEVEL2_MOB_CHANCE.get();
        } catch (IllegalStateException notLoaded) {
            return boss ? 0.10 : 0.30;
        }
    }
}
