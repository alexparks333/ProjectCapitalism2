package com.alexh.capitalism.event;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// The Sanguine Witch (Events.md > The bosses): one of the Blood Moon's three bosses (BloodMoonBoss). A tall, gaunt
// sorceress with great bat-like wings of blood, in tattered crimson robes, floating off the ground, a crown of bone, long clawed hands, orbs of
// blood circling her. She keeps her distance and makes you come to her. She fights:
// - BLOOD BOLTS: a fan of five orbs of blood (two volleys under half health), flung at you, curving after you a little (dodge, or hide behind something);
// - BLOOD RAIN: dark storm clouds gather over everyone near (and round her target), a pale pink circle under each; it
//   closes in, drips start, and a second and a half later blood pours down: step out of the circle;
// - DRAIN: a violet soul siphon: a ring locks round your feet and a tether reaches out to you (a second's warning), then
//   violet streams flow from you into her (2 a pulse, healing her 2 with a lilac glow) while she can see you: break her line of sight
//   or get away;
// - SPEAR STORM: gold marks round everyone near, a pillar of light over each, then golden spears fall down them;
// - BLOOD NOVA: she rises, wings spread wide, and sends a wave of blood rolling out along the ground (two, one behind the
//   other, under half health): jump them;
// - after a nova she sinks to the ground EXHAUSTED for two seconds, open to attack;
// - BLOOD BUBBLE: shot 5 times with nobody hitting her up close, she shuts herself in a bubble of blood and heals (shots
//   heal her too) and attacks no more, until a melee hit pops it (and stuns her);
// - BLINK: crowd her or keep hitting her up close and she vanishes in a puff of black ink and smoke and appears somewhere else nearby;
// - THRALLS: at half health and at a quarter, four Level II skeletons ("Bone Thrall") climb up round her; they heal her
//   when they die near her.
public class SanguineWitch extends BloodMoonBoss {

    public static final int BOLTS = 1;
    public static final int RAIN = 2;
    public static final int DRAIN = 3;
    public static final int BLINK = 4;
    public static final int SUMMON = 5;
    public static final int NOVA = 6;
    public static final int SPEARS = 7;
    // after a nova: on the ground, worn out, open to attack
    public static final int EXHAUSTED = 8;
    // shot BUBBLE_ARROWS times without anyone hitting her up close: she shuts herself in a bubble of blood and heals
    // (arrows heal her too) until someone pops it with a melee hit
    public static final int BUBBLE = 9;
    static final int BUBBLE_ARROWS = 5;
    // her target is in water: a whirlpool opens round them, pulling them in and under
    public static final int MAELSTROM = 10;

    private int boltCooldown = 30;
    private int rainCooldown = 100;
    private int drainCooldown = 160;
    private int blinkCooldown = 60;
    private int novaCooldown = 140;
    private int spearCooldown = 90;
    private int maelstromCooldown = 40;
    private int crowdedTicks;
    private int closeHits;
    // shots that hit her since the last melee hit
    private int arrowHits;
    private @Nullable LivingEntity drained;
    private final List<Orb> orbs = new ArrayList<>();

    // a flying orb of blood (the server moves it, sends its particles)
    private static final class Orb {
        Vec3 pos;
        Vec3 vel;
        final @Nullable LivingEntity target;
        int life;

        Orb(Vec3 pos, Vec3 vel, @Nullable LivingEntity target) {
            this.pos = pos;
            this.vel = vel;
            this.target = target;
        }
    }

    public SanguineWitch(EntityType<? extends SanguineWitch> type, Level level) {
        super(type, level, "The Sanguine Witch", 0xFF4070);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 260.0)
            .add(Attributes.ATTACK_DAMAGE, 6.0)
            .add(Attributes.ARMOR, 4.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
            .add(Attributes.FOLLOW_RANGE, 48.0)
            .add(Attributes.STEP_HEIGHT, 1.5)
            .add(Attributes.SCALE, 1.5);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8) {
            @Override
            public boolean canUse() {
                return SanguineWitch.this.getTarget() == null && super.canUse();
            }
        });
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected net.minecraft.world.level.ItemLike crestPiece() {
        return com.alexh.capitalism.core.ModItems.SANGUINE_CREST.get();
    }

    @Override
    protected int mistColor() {
        return 0x9A1040;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel level) {
            this.tickOrbs(level);
            this.tickRings(level);
            this.tickSpears(level);
            this.tickStorms(level);
            this.tickWhirlpools(level);
            if (this.photoVictim != null) this.drainEffects(level, this.photoVictim, this.photoDrainTicks++);
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.boltCooldown > 0) this.boltCooldown--;
        if (this.rainCooldown > 0) this.rainCooldown--;
        if (this.drainCooldown > 0) this.drainCooldown--;
        if (this.blinkCooldown > 0) this.blinkCooldown--;
        if (this.novaCooldown > 0) this.novaCooldown--;
        if (this.spearCooldown > 0) this.spearCooldown--;
        if (this.maelstromCooldown > 0) this.maelstromCooldown--;
        if (this.tickCount % 100 == 0) this.closeHits = 0;
        LivingEntity target = this.getTarget();
        if (target != null) this.getLookControl().setLookAt(target);
        this.crowdedTicks = this.playersWithin(4.0) > 0 ? this.crowdedTicks + 1 : 0;
        int t = this.actionTicks();
        // shot again and again with nobody coming close: into her bubble (not mid-nova, mid-blink or calling thralls)
        int now = this.action();
        if (this.arrowHits >= BUBBLE_ARROWS && now != BUBBLE && now != NOVA && now != BLINK && now != SUMMON) {
            this.enterBubble(level);
            t = 0;
        }
        switch (this.action()) {
            case BOLTS -> {
                this.getNavigation().stop();
                if (t == 14 && target != null) this.fireBolts(level, target);
                // under half her health: a second volley
                if (t == 24 && target != null && this.getHealth() < this.getMaxHealth() / 2) this.fireBolts(level, target);
                if (t >= 30) this.setAction(IDLE);
                return;
            }
            case RAIN -> {
                this.getNavigation().stop();
                if (t == 12) this.bloodRain(level, target);
                if (t >= 26) this.setAction(IDLE);
                return;
            }
            case DRAIN -> {
                this.getNavigation().stop();
                LivingEntity victim = this.drained;
                if (victim == null || !victim.isAlive() || victim.distanceTo(this) > 14.0 || !this.hasLineOfSight(victim) || t > DRAIN_START + 60) {
                    this.drained = null;
                    this.setAction(IDLE);
                    return;
                }
                this.drainEffects(level, victim, t);
                // the siphon: from DRAIN_START (after the warning), every half second
                if (t >= DRAIN_START && (t - DRAIN_START) % 10 == 0) {
                    // heals her a little (2) for every pulse that lands, and you can see it: a lilac glow swells over her
                    if (victim.hurtServer(level, this.damageSources().magic(), 2.0F)) {
                        this.heal(2.0F);
                        if (victim instanceof Player) this.reached();
                        BossFx.burst(level, this.random, this.position().add(0, this.getBbHeight() * 0.55, 0), 14, 1.2, DRAIN_LILAC, 6);
                        level.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + this.getBbHeight() + 0.3, this.getZ(), 1, 0.3, 0.1, 0.3, 0);
                    }
                    this.sound(SoundEvents.WARDEN_HEARTBEAT, 1.2F, 1.6F);
                    level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 1.5F, 0.8F);
                }
                return;
            }
            case BLINK -> {
                this.getNavigation().stop();
                if (t == 6) this.blink(level, target);
                if (t >= 14) this.setAction(IDLE);
                return;
            }
            case SUMMON -> {
                this.getNavigation().stop();
                if (t % 2 == 0) level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 0.2, this.getZ(), 6, 2.5, 0.1, 2.5, 0.02);
                if (t == 30) {
                    this.sound(SoundEvents.EVOKER_CAST_SPELL, 2.0F, 0.6F);
                    this.callThralls(level, EntityTypes.SKELETON, 4, "Bone Thrall", null);
                }
                if (t >= 40) this.setAction(IDLE);
                return;
            }
            case NOVA -> {
                this.getNavigation().stop();
                // she rises, wings wide, then sends out two rings of blood along the ground: jump them
                this.setNoGravity(true);
                this.setDeltaMovement(0, t < 16 ? 0.14 : 0.0, 0);
                if (t < 26 && t % 2 == 0) {
                    level.sendParticles(new DustParticleOptions(0xFF1030, 1.6F), this.getX(), this.getY() + 2.0, this.getZ(), 12, 1.6, 1.6, 1.6, 0);
                }
                if (t == 6) this.sound(SoundEvents.WARDEN_SONIC_CHARGE, 2.5F, 0.7F);
                // one wave; under half her health a second right behind it (jump, then jump again)
                if (t == 26 || t == 40 && this.getHealth() < this.getMaxHealth() / 2) this.nova(level);
                if (t >= 52) {
                    // spent: she sinks to the ground, open to attack
                    this.setNoGravity(false);
                    this.setAction(EXHAUSTED);
                }
                return;
            }
            case BUBBLE -> {
                // no attacks: she floats in her bubble healing (4 a second) until a melee hit pops it (hurtServer)
                this.getNavigation().stop();
                this.drawBubble(level, t);
                if (t % 10 == 0) this.heal(2.0F);
                if (t % 30 == 0) this.sound(SoundEvents.WARDEN_HEARTBEAT, 1.4F, 0.9F);
                // nobody left to fight: she lets it go
                if (t > 100 && this.getHealth() >= this.getMaxHealth() && this.playersWithin(32.0) == 0) {
                    this.arrowHits = 0;
                    this.setAction(IDLE);
                }
                return;
            }
            case EXHAUSTED -> {
                this.getNavigation().stop();
                if (t % 4 == 0) level.sendParticles(new DustParticleOptions(0x600810, 1.2F), this.getX(), this.getY() + 1.0, this.getZ(), 4, 0.6, 0.6, 0.6, 0);
                if (t >= 40) this.setAction(IDLE);
                return;
            }
            case MAELSTROM -> {
                this.getNavigation().stop();
                if (t < 12) BossFx.burst(level, this.random, this.handPos(), 3, 1.0, WHIRL_FOAM, 5);
                if (t == 12 && target != null) this.whirlpool(level, target);
                if (t >= 24) this.setAction(IDLE);
                return;
            }
            case SPEARS -> {
                this.getNavigation().stop();
                if (t == 14) this.spearStorm(level, target);
                if (t >= 30) this.setAction(IDLE);
                return;
            }
            default -> {
                if (this.isNoGravity()) this.setNoGravity(false);
            }
        }
        // what next
        if (this.timeToCall()) {
            this.setAction(SUMMON);
            this.sound(SoundEvents.EVOKER_PREPARE_SUMMON, 2.0F, 0.8F);
            return;
        }
        if (this.blinkCooldown <= 0 && (this.crowdedTicks > 70 || this.closeHits >= 3)) {
            this.setAction(BLINK);
            this.blinkCooldown = 170 + this.random.nextInt(60);
            this.closeHits = 0;
            this.sound(SoundEvents.ILLUSIONER_PREPARE_MIRROR, 2.0F, 0.7F);
            return;
        }
        if (target == null || !target.isAlive()) return;
        double dist = this.distanceTo(target);
        // keep her distance: back off if they're close, come closer if they're far
        if (this.tickCount % 10 == 0) {
            if (dist < 4.0) {
                Vec3 away = this.position().subtract(target.position()).multiply(1, 0, 1);
                if (away.lengthSqr() < 0.01) away = new Vec3(1, 0, 0);
                away = away.normalize().scale(8);
                this.getNavigation().moveTo(this.getX() + away.x, this.getY(), this.getZ() + away.z, 1.0);
            } else if (dist > 15.0 || !this.hasLineOfSight(target)) {
                this.getNavigation().moveTo(target, 1.0);
            } else {
                this.getNavigation().stop();
            }
        }
        boolean sees = this.hasLineOfSight(target);
        if (this.novaCooldown <= 0 && dist < 12.0 && this.landed()) {
            this.setAction(NOVA);
            this.novaCooldown = 260 + this.random.nextInt(80);
            this.sound(SoundEvents.PHANTOM_FLAP, 2.5F, 0.5F);
            return;
        }
        if (this.maelstromCooldown <= 0 && target.isInWater() && dist < 24.0) {
            this.setAction(MAELSTROM);
            this.maelstromCooldown = 160 + this.random.nextInt(60);
            this.sound(SoundEvents.ELDER_GUARDIAN_AMBIENT, 2.0F, 0.7F);
            return;
        }
        if ((this.spearCooldown <= 0 || this.breaching() && this.spearCooldown <= 100) && dist < 24.0) {
            this.setAction(SPEARS);
            this.spearCooldown = 200 + this.random.nextInt(60);
            this.sound(SoundEvents.ILLUSIONER_PREPARE_MIRROR, 2.0F, 0.5F);
            return;
        }
        if (this.rainCooldown <= 0 && dist < 22.0) {
            this.setAction(RAIN);
            this.rainCooldown = 160 + this.random.nextInt(60);
            this.sound(SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, 2.0F, 0.6F);
            return;
        }
        if (this.drainCooldown <= 0 && sees && dist < 12.0) {
            this.drained = target;
            this.setAction(DRAIN);
            this.drainCooldown = 220 + this.random.nextInt(80);
            this.sound(SoundEvents.EVOKER_PREPARE_ATTACK, 2.0F, 1.2F);
            return;
        }
        if (this.boltCooldown <= 0 && (sees || this.breaching()) && dist < 26.0) {
            this.setAction(BOLTS);
            this.boltCooldown = 90 + this.random.nextInt(40);
            this.sound(SoundEvents.WITCH_AMBIENT, 2.0F, 0.6F);
        }
    }

    // roughly where her raised hand is
    private Vec3 handPos() {
        Vec3 look = Vec3.directionFromRotation(0, this.yBodyRot);
        return this.position().add(look.x * 0.6, this.getBbHeight() * 0.8, look.z * 0.6);
    }

    // ── blood nova: a ring of blood racing out along the ground; jump it or be hit ──

    private record Ring(Vec3 centre, int start, java.util.Set<java.util.UUID> hit) {
    }

    private final List<Ring> rings = new ArrayList<>();
    // blocks per tick: 9 a second
    static final double RING_SPEED = 0.45;
    // how long a ring runs: out to RING_MAX (just under 20 blocks)
    static final int RING_TICKS = 42;
    static final double RING_MAX = 1.0 + RING_SPEED * RING_TICKS;
    // its wall, foot to crest: (height, how far behind the front, colour); rows close together so it reads as solid
    private static final double[][] RING_ROWS = {
        {0.05, 0.0, 0x4A0410}, {0.20, 0.0, 0x6A0614}, {0.35, 0.0, 0x8A0A1C}, {0.50, 0.0, 0xAA0E24}, {0.65, 0.0, 0xCC1230}, {0.80, 0.0, 0xEE2040}, {0.92, 0.05, 0xFF5068},
        {0.10, 0.28, 0x500612}, {0.30, 0.28, 0x7A0A1A}, {0.50, 0.28, 0x9A0C20}, {0.70, 0.28, 0xC01030}};
    private static final int RING_POINTS = 400;

    private void nova(ServerLevel level) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, this.getBlockX(), this.getBlockZ());
        Vec3 centre = new Vec3(this.getX(), y, this.getZ());
        this.rings.add(new Ring(centre, this.tickCount, new java.util.HashSet<>()));
        this.sound(SoundEvents.WARDEN_SONIC_BOOM, 2.0F, 0.8F);
        level.sendParticles(new DustParticleOptions(0xFF2040, 3.0F), this.getX(), this.getY() + 1.5, this.getZ(), 60, 1.2, 1.2, 1.2, 0);
        level.sendParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 1.5, this.getZ(), 1, 0, 0, 0, 0);
        // the wave: a knee-high wall of blood (as high as it hits) launched once, every bit of it gliding straight out to
        // the edge in exactly the ring's time, so it travels as one crisp band (trail particles live just that long and
        // glow, unlike dust, which hangs about and would fill the whole circle)
        for (int i = 0; i < RING_POINTS; i++) {
            double a = i * Math.PI * 2 / RING_POINTS;
            double cos = Math.cos(a), sin = Math.sin(a);
            for (double[] row : RING_ROWS) {
                double from = 1.0 - row[1], to = RING_MAX - row[1];
                Vec3 target = new Vec3(centre.x + cos * to, y + row[0], centre.z + sin * to);
                level.sendParticles(new net.minecraft.core.particles.TrailParticleOption(target, (int) row[2], RING_TICKS),
                    centre.x + cos * from, y + row[0], centre.z + sin * from, 1, 0, 0, 0, 0);
            }
        }
    }

    // (dev: client/PhotoShoot) storms over these spots, landing in `delay` ticks, though she's held still
    public void photoRain(double x1, double z1, double x2, double z2, int delay) {
        if (!(this.level() instanceof ServerLevel level)) return;
        this.stormAt(level, x1, z1, delay);
        this.stormAt(level, x2, z2, delay + 8);
    }

    // (dev: client/PhotoShoot) the drain's look on this victim, though she's held still (null = stop)
    private @Nullable LivingEntity photoVictim;
    private int photoDrainTicks;

    public void photoDrain(@Nullable LivingEntity victim) {
        this.photoVictim = victim;
        this.photoDrainTicks = 0;
    }

    // (dev: client/PhotoShoot) send a ring out now, though she's held still
    public void photoNova() {
        if (this.level() instanceof ServerLevel level) this.nova(level);
    }

    private void tickRings(ServerLevel level) {
        for (Iterator<Ring> it = this.rings.iterator(); it.hasNext(); ) {
            Ring ring = it.next();
            int age = this.tickCount - ring.start;
            double r = age * RING_SPEED + 1.0;
            if (age > RING_TICKS) {
                it.remove();
                continue;
            }
            // spray flicking forward and up off the crest as it rolls
            for (int i = 0; i < 30; i++) {
                double a = (i + (age % 2) * 0.5) * Math.PI * 2 / 30;
                double cos = Math.cos(a), sin = Math.sin(a);
                double x = ring.centre.x + cos * (r - 0.3), z = ring.centre.z + sin * (r - 0.3);
                // keeps up with the wave (it moves 2.7 blocks in the spray's 6 ticks), thrown a little ahead
                Vec3 target = new Vec3(x + cos * 3.2, ring.centre.y + 1.4, z + sin * 3.2);
                level.sendParticles(new net.minecraft.core.particles.TrailParticleOption(target, 0xFF8090, 6), x, ring.centre.y + 0.85, z, 1, 0, 0, 0, 0);
            }
            if (age % 6 == 0) {
                level.playSound(null, ring.centre.x, ring.centre.y, ring.centre.z, SoundEvents.GENERIC_SPLASH, net.minecraft.sounds.SoundSource.HOSTILE, 1.4F, 0.5F);
            }
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(ring.centre, ring.centre).inflate(r + 1.5, 3.0, r + 1.5), this::isFoe)) {
                double d = Math.sqrt(Math.pow(e.getX() - ring.centre.x, 2) + Math.pow(e.getZ() - ring.centre.z, 2));
                if (Math.abs(d - r) > 0.9 || e.getY() - ring.centre.y > 0.75 || !ring.hit.add(e.getUUID())) continue;
                if (e.hurtServer(level, this.damageSources().indirectMagic(this, this), 9.0F) && e instanceof Player) this.reached();
                Vec3 away = new Vec3(e.getX() - ring.centre.x, 0, e.getZ() - ring.centre.z);
                if (away.lengthSqr() > 0.01) away = away.normalize();
                e.push(away.x * 1.1, 0.45, away.z * 1.1);
                e.needsSync = true;
            }
        }
    }

    // ── spear storm: golden spears fall out of the sky onto gold marks round everyone near ──

    // floorY: whoever it was aimed at was walled in (BloodMoonBoss.breaching): it smashes down through the roof to their feet
    private record Spear(Vec3 ground, int lands, int floorY) {
    }

    private final List<Spear> spears = new ArrayList<>();

    private void spearStorm(ServerLevel level, @Nullable LivingEntity target) {
        this.sound(SoundEvents.ILLUSIONER_CAST_SPELL, 2.0F, 0.5F);
        List<LivingEntity> marked = new ArrayList<>(level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(24.0), this::isFoe));
        if (target != null && !marked.contains(target)) marked.add(target);
        int k = 0;
        for (LivingEntity e : marked) {
            if (k++ >= 5) break;
            for (int i = 0; i < 4; i++) {
                double a = this.random.nextDouble() * Math.PI * 2, r = i == 0 ? 0.0 : 1.5 + this.random.nextDouble() * 2.5;
                double x = e.getX() + Math.cos(a) * r, z = e.getZ() + Math.sin(a) * r;
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
                this.spears.add(new Spear(new Vec3(x, y, z), this.tickCount + 22 + i * 6, this.breaching() && y - 1 > e.getBlockY() ? e.getBlockY() : Integer.MIN_VALUE));
            }
        }
    }

    private void tickSpears(ServerLevel level) {
        for (Iterator<Spear> it = this.spears.iterator(); it.hasNext(); ) {
            Spear s = it.next();
            int left = s.lands - this.tickCount;
            if (left > 0) {
                // GOLD (its own colour): a gold mark with a cross in it, a thin pillar of light up into the sky over it,
                // and for the last moments the spear itself falling down the pillar
                BossFx.ring(level, s.ground, 1.3, 16, SPEAR_GOLD, 2, 0.08);
                BossFx.line(level, s.ground.add(-0.9, 0.08, 0), s.ground.add(0.9, 0.08, 0), 6, SPEAR_GOLD, 2);
                BossFx.line(level, s.ground.add(0, 0.08, -0.9), s.ground.add(0, 0.08, 0.9), 6, SPEAR_GOLD, 2);
                if (this.tickCount % 2 == 0) BossFx.line(level, s.ground.add(0, 0.3, 0), s.ground.add(0, 14.0, 0), 40, SPEAR_LIGHT, 3);
                if (left == SPEAR_FALL) {
                    for (int i = 0; i < 8; i++) {
                        BossFx.streak(level, s.ground.add(0, 16.0 + i * 0.35, 0), s.ground.add(0, i * 0.35, 0), i == 0 ? SPEAR_TIP : SPEAR_SHAFT, SPEAR_FALL);
                    }
                }
                continue;
            }
            it.remove();
            BossFx.groundBurst(level, this.random, s.ground, 24, 1.8, SPEAR_GOLD, 6);
            BossFx.spikes(level, this.random, s.ground, 0.4, 8, 1.4, SPEAR_SHAFT, SPEAR_TIP);
            level.sendParticles(ParticleTypes.CRIT, s.ground.x, s.ground.y + 0.3, s.ground.z, 10, 0.4, 0.2, 0.4, 0.3);
            level.playSound(null, s.ground.x, s.ground.y, s.ground.z, SoundEvents.TRIDENT_HIT_GROUND, SoundSource.HOSTILE, 1.5F, 0.6F);
            level.playSound(null, s.ground.x, s.ground.y, s.ground.z, SoundEvents.POINTED_DRIPSTONE_LAND, SoundSource.HOSTILE, 1.2F, 0.6F);
            Vec3 hits = s.ground;
            if (s.floorY != Integer.MIN_VALUE) {
                // through the roof, down to their feet
                for (int y = (int) s.ground.y - 1; y >= s.floorY; y -= 2) this.smashBlocks(level, new Vec3(s.ground.x, y + 0.5, s.ground.z), 1.4, s.floorY, 20);
                hits = new Vec3(s.ground.x, s.floorY, s.ground.z);
                BossFx.spikes(level, this.random, hits, 0.6, 10, 1.6, SPEAR_SHAFT, SPEAR_TIP);
            }
            this.hurtAround(level, hits, 1.3, 8.0F, 0.2, 0.3, null);
        }
    }

    private static final int SPEAR_GOLD = 0xFFC830, SPEAR_LIGHT = 0xFFE8A0, SPEAR_TIP = 0xFFFFE0, SPEAR_SHAFT = 0xE0A020;
    private static final int SPEAR_FALL = 6;

    // ── blood bolts ──

    private void fireBolts(ServerLevel level, LivingEntity target) {
        this.sound(SoundEvents.BLAZE_SHOOT, 1.6F, 0.6F);
        Vec3 from = this.handPos();
        Vec3 aim = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(from).normalize();
        for (int i = -2; i <= 2; i++) {
            Vec3 dir = aim.yRot(i * 0.24F).add(0, 0.05, 0).normalize();
            this.orbs.add(new Orb(from, dir.scale(0.45), target));
        }
    }

    private void tickOrbs(ServerLevel level) {
        for (Iterator<Orb> it = this.orbs.iterator(); it.hasNext(); ) {
            Orb orb = it.next();
            orb.life++;
            // curving after its target, a little
            if (orb.target != null && orb.target.isAlive()) {
                Vec3 want = orb.target.position().add(0, orb.target.getBbHeight() * 0.5, 0).subtract(orb.pos).normalize().scale(0.45);
                orb.vel = orb.vel.lerp(want, 0.035).normalize().scale(0.45);
            }
            Vec3 next = orb.pos.add(orb.vel);
            HitResult wall = level.clip(new ClipContext(orb.pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            boolean done = wall.getType() != HitResult.Type.MISS || orb.life > 90;
            orb.pos = wall.getType() != HitResult.Type.MISS ? wall.getLocation() : next;
            // walled in (BloodMoonBoss.breaching): a bolt blasts a hole in the wall it hits
            if (wall.getType() != HitResult.Type.MISS && this.breaching()) {
                this.smashBlocks(level, orb.pos, 1.0, orb.target != null ? orb.target.getBlockY() : level.getMinY(), 4);
            }
            level.sendParticles(new DustParticleOptions(0xC00818, 1.8F), orb.pos.x, orb.pos.y, orb.pos.z, 3, 0.08, 0.08, 0.08, 0);
            level.sendParticles(ParticleTypes.CRIMSON_SPORE, orb.pos.x, orb.pos.y, orb.pos.z, 1, 0.05, 0.05, 0.05, 0);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(orb.pos, orb.pos).inflate(0.7), this::isFoe)) {
                if (e.hurtServer(level, this.damageSources().magic(), 5.0F) && e instanceof Player) this.reached();
                e.needsSync = true;
                done = true;
                break;
            }
            if (done) {
                level.sendParticles(new DustParticleOptions(0xE01020, 2.0F), orb.pos.x, orb.pos.y, orb.pos.z, 14, 0.3, 0.3, 0.3, 0);
                level.playSound(null, orb.pos.x, orb.pos.y, orb.pos.z, SoundEvents.SLIME_SQUISH, net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 0.6F);
                it.remove();
            }
        }
    }

    // ── drain: a VIOLET soul siphon (its own colour, not blood) ──
    // First a warning (DRAIN_START ticks): a violet circle locks round your feet and the tether reaches out from her hand to
    // you. Then streams of violet and lilac light flow from you INTO her, souls pulled out of you with every hurt.

    static final int DRAIN_START = 20;
    private static final int DRAIN_VIOLET = 0xA040FF, DRAIN_LILAC = 0xE6C8FF;

    private void drainEffects(ServerLevel level, LivingEntity victim, int t) {
        Vec3 from = victim.position().add(0, victim.getBbHeight() * 0.6, 0), hand = this.handPos();
        Vec3 feet = victim.position();
        // the lock-on ring round their feet, spinning
        for (int i = 0; i < 16; i++) {
            double a = i * Math.PI * 2 / 16 + t * 0.15;
            Vec3 p = new Vec3(feet.x + Math.cos(a) * 0.9, feet.y + 0.08, feet.z + Math.sin(a) * 0.9);
            level.sendParticles(new net.minecraft.core.particles.TrailParticleOption(p, i % 2 == 0 ? DRAIN_VIOLET : DRAIN_LILAC, 2), p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        if (t < DRAIN_START) {
            // the tether reaching out from her hand to them
            double reach = (t + 1) / (double) DRAIN_START;
            for (int i = 0; i <= 12; i++) {
                Vec3 p = hand.lerp(from, reach * i / 12.0);
                level.sendParticles(new net.minecraft.core.particles.TrailParticleOption(p, DRAIN_VIOLET, 2), p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
            if (t == 0) level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 1.2F, 0.5F);
            return;
        }
        // the stream: specks all along the line, each gliding the rest of the way into her hand, curling a little
        double length = from.distanceTo(hand);
        for (int i = 0; i < 18; i++) {
            double f = this.random.nextDouble();
            Vec3 p = from.lerp(hand, f).add((this.random.nextDouble() - 0.5) * 0.3, (this.random.nextDouble() - 0.5) * 0.3, (this.random.nextDouble() - 0.5) * 0.3);
            int ticks = Math.max(2, (int) ((1.0 - f) * length / 0.7));
            level.sendParticles(new net.minecraft.core.particles.TrailParticleOption(hand, i % 3 == 0 ? DRAIN_LILAC : DRAIN_VIOLET, ticks), p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        // purple sparkles round them, souls torn out as it bites, a glow gathering in her hand
        if (t % 2 == 0) level.sendParticles(ParticleTypes.WITCH, victim.getX(), victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(), 3, 0.4, 0.5, 0.4, 0);
        if ((t - DRAIN_START) % 10 == 0) level.sendParticles(ParticleTypes.SOUL, from.x, from.y, from.z, 4, 0.25, 0.25, 0.25, 0.03);
        level.sendParticles(new net.minecraft.core.particles.TrailParticleOption(hand, DRAIN_LILAC, 3), hand.x, hand.y + 0.15, hand.z, 1, 0.1, 0.1, 0.1, 0);
    }

    // ── blood rain: a storm cloud gathers over each spot, then blood pours down on it ──
    // Its own look: a dark cloud overhead, a PALE PINK circle on the ground marking exactly where it falls, a second
    // ring closing in to the middle as the countdown, drips at first, then a downpour of falling streaks and a splash.

    private record Storm(Vec3 ground, int start, int lands) {
    }

    private final List<Storm> storms = new ArrayList<>();
    static final double STORM_RADIUS = 2.0;
    static final double CLOUD_HEIGHT = 6.5;
    private static final int POUR_TICKS = 6;

    private void bloodRain(ServerLevel level, @Nullable LivingEntity target) {
        this.sound(SoundEvents.ILLUSIONER_CAST_SPELL, 2.0F, 0.6F);
        this.sound(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.6F, 1.6F);
        int clouds = 0;
        for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(22.0), this::isFoe)) {
            if (clouds++ >= 5) break;
            this.stormAt(level, player.getX(), player.getZ(), 30);
        }
        if (target != null) {
            for (int i = 0; i < 3; i++) {
                double a = this.random.nextDouble() * Math.PI * 2, r = 2.5 + this.random.nextDouble() * 3.0;
                this.stormAt(level, target.getX() + Math.cos(a) * r, target.getZ() + Math.sin(a) * r, 34 + i * 4);
            }
        }
    }

    private void stormAt(ServerLevel level, double x, double z, int delay) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
        this.storms.add(new Storm(new Vec3(x, y, z), this.tickCount, this.tickCount + delay));
    }

    // a streak of blood falling from the cloud to the ground, landing in `ticks`
    private void drop(ServerLevel level, Storm s, int ticks, int color) {
        double a = this.random.nextDouble() * Math.PI * 2, r = Math.sqrt(this.random.nextDouble()) * STORM_RADIUS;
        double x = s.ground.x + Math.cos(a) * r, z = s.ground.z + Math.sin(a) * r;
        level.sendParticles(new net.minecraft.core.particles.TrailParticleOption(new Vec3(x, s.ground.y + 0.05, z), color, ticks),
            x, s.ground.y + CLOUD_HEIGHT - 0.3, z, 1, 0, 0, 0, 0);
    }

    // a still, glowing ring on the ground
    private static void groundRing(ServerLevel level, Vec3 at, double radius, int points, int color) {
        for (int i = 0; i < points; i++) {
            double a = i * Math.PI * 2 / points;
            Vec3 p = new Vec3(at.x + Math.cos(a) * radius, at.y + 0.08, at.z + Math.sin(a) * radius);
            level.sendParticles(new net.minecraft.core.particles.TrailParticleOption(p, color, 2), p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    private void tickStorms(ServerLevel level) {
        for (Iterator<Storm> it = this.storms.iterator(); it.hasNext(); ) {
            Storm s = it.next();
            int now = this.tickCount;
            if (now < s.lands) {
                double progress = (now - s.start) / (double) (s.lands - s.start);
                // the cloud, gathering: dark, a little wider than the spot
                if (now % 2 == 0) {
                    level.sendParticles(new DustParticleOptions(0x1C0810, 3.5F), s.ground.x, s.ground.y + CLOUD_HEIGHT, s.ground.z,
                        (int) (4 + progress * 8), STORM_RADIUS * 0.7, 0.25, STORM_RADIUS * 0.7, 0);
                    level.sendParticles(ParticleTypes.LARGE_SMOKE, s.ground.x, s.ground.y + CLOUD_HEIGHT + 0.3, s.ground.z,
                        2, STORM_RADIUS * 0.6, 0.15, STORM_RADIUS * 0.6, 0.0);
                }
                // where it falls (pale pink), and the countdown ring closing in
                groundRing(level, s.ground, STORM_RADIUS, 32, 0xFFB8D0);
                groundRing(level, s.ground, Math.max(0.15, STORM_RADIUS * (1.0 - progress)), 20, 0xFF5A8A);
                // drips, getting heavier
                if (progress > 0.3) for (int i = 0; i < (int) (progress * 4); i++) this.drop(level, s, 8, 0x8A0818);
                // the downpour: lands just as it strikes
                if (now >= s.lands - POUR_TICKS) {
                    for (int i = 0; i < 22; i++) this.drop(level, s, POUR_TICKS, i % 2 == 0 ? 0xC00C24 : 0x700610);
                }
                if (now == s.lands - POUR_TICKS) {
                    level.playSound(null, s.ground.x, s.ground.y, s.ground.z, SoundEvents.WEATHER_RAIN_ABOVE, SoundSource.HOSTILE, 1.5F, 0.6F);
                }
                continue;
            }
            it.remove();
            // the splash: blood thrown up and out, a puddle, and the hurt
            for (int i = 0; i < 28; i++) {
                double a = this.random.nextDouble() * Math.PI * 2, r = this.random.nextDouble() * STORM_RADIUS;
                double x = s.ground.x + Math.cos(a) * r, z = s.ground.z + Math.sin(a) * r;
                Vec3 to = new Vec3(x + Math.cos(a) * 0.8, s.ground.y + 0.6 + this.random.nextDouble() * 0.8, z + Math.sin(a) * 0.8);
                level.sendParticles(new net.minecraft.core.particles.TrailParticleOption(to, 0xB00A20, 5), x, s.ground.y + 0.1, z, 1, 0, 0, 0, 0);
            }
            level.sendParticles(new DustParticleOptions(0x600612, 2.0F), s.ground.x, s.ground.y + 0.1, s.ground.z, 20, STORM_RADIUS * 0.45, 0.02, STORM_RADIUS * 0.45, 0);
            level.playSound(null, s.ground.x, s.ground.y, s.ground.z, SoundEvents.GENERIC_SPLASH, SoundSource.HOSTILE, 1.3F, 0.6F);
            this.hurtAround(level, s.ground, STORM_RADIUS, 10.0F, 0.1, 0.2, null);
        }
    }

    // ── blink: gone in a burst of blood, somewhere else near ──

    // blink's own look: a puff of black ink and smoke (not blood), shadows flung out
    private void shadowStep(ServerLevel level) {
        level.sendParticles(ParticleTypes.SQUID_INK, this.getX(), this.getY() + 1.4, this.getZ(), 40, 0.5, 1.0, 0.5, 0.05);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 1.2, this.getZ(), 20, 0.5, 0.9, 0.5, 0.02);
        BossFx.burst(level, this.random, this.position().add(0, 1.4, 0), 30, 2.2, 0x1A0610, 6);
    }

    private void blink(ServerLevel level, @Nullable LivingEntity target) {
        Vec3 centre = target != null ? target.position() : this.position();
        this.shadowStep(level);
        for (int attempt = 0; attempt < 12; attempt++) {
            double a = this.random.nextDouble() * Math.PI * 2, r = 9.0 + this.random.nextDouble() * 6.0;
            double x = centre.x + Math.cos(a) * r, z = centre.z + Math.sin(a) * r;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
            if (Math.abs(y - this.getY()) > 8) continue;
            if (this.randomTeleport(x, y, z, false, state -> !state.getFluidState().isEmpty())) break;
        }
        this.shadowStep(level);
        this.sound(SoundEvents.ENDERMAN_TELEPORT, 2.0F, 0.6F);
        this.getNavigation().stop();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.getEntity() instanceof LivingEntity from && isMonsterSide(from)) return false;
        boolean ranged = isRanged(source) && source.getEntity() instanceof Player, melee = isMelee(source);
        if (this.action() == BUBBLE) {
            if (melee) {
                // popped: the hit lands, and she's stunned (EXHAUSTED) for a moment
                this.popBubble(level);
            } else {
                if (ranged) {
                    // she drinks the shot: it heals her instead
                    this.heal(damage);
                    Vec3 at = source.getDirectEntity() != null ? source.getDirectEntity().position() : this.position().add(0, this.getBbHeight() * 0.6, 0);
                    BossFx.burst(level, this.random, at, 12, 1.0, BUBBLE_PINK, 5);
                    level.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + this.getBbHeight() + 0.4, this.getZ(), 1, 0.3, 0.1, 0.3, 0);
                    this.sound(SoundEvents.SLIME_SQUISH, 1.4F, 0.6F);
                }
                // nothing but a melee hit gets through (only /kill and the like)
                if (!source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
            }
        } else if (melee) {
            this.arrowHits = 0;
        } else if (ranged) {
            this.arrowHits++;
        }
        if (source.getEntity() instanceof LivingEntity attacker && attacker.distanceTo(this) < 4.5) this.closeHits++;
        return super.hurtServer(level, source, damage);
    }

    // ── maelstrom: her target's in the water, so she opens a whirlpool round them ──
    // Its own look: deep sea green-blue arms spiralling in to a dark eye, white foam on them. First a second's warning
    // (the outline and arms forming), then for two seconds it pulls everyone within WHIRL_RADIUS toward the middle and
    // under; in the eye it hurts (3 every half second).

    private record Whirlpool(Vec3 centre, int start) {
    }

    private final List<Whirlpool> whirlpools = new ArrayList<>();
    static final double WHIRL_RADIUS = 5.0;
    private static final int WHIRL_WARN = 20, WHIRL_TICKS = 60;
    private static final int WHIRL_SEA = 0x1890A0, WHIRL_DEEP = 0x0A3848, WHIRL_FOAM = 0xC8FFF4;

    private void whirlpool(ServerLevel level, LivingEntity target) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target.getBlockX(), target.getBlockZ());
        this.whirlpools.add(new Whirlpool(new Vec3(target.getX(), Math.min(y, target.getY() + 1.0), target.getZ()), this.tickCount));
        this.sound(SoundEvents.ILLUSIONER_CAST_SPELL, 2.0F, 0.5F);
    }

    private void tickWhirlpools(ServerLevel level) {
        for (Iterator<Whirlpool> it = this.whirlpools.iterator(); it.hasNext(); ) {
            Whirlpool w = it.next();
            int age = this.tickCount - w.start;
            if (age > WHIRL_TICKS) {
                it.remove();
                continue;
            }
            boolean live = age >= WHIRL_WARN;
            double spin = age * 0.18;
            // the outline, and four arms spiralling in to the eye (only half-drawn during the warning)
            BossFx.ring(level, w.centre, WHIRL_RADIUS, 40, live ? WHIRL_SEA : WHIRL_FOAM, 2, 0.15);
            if (age % 2 == 0) {
                int reach = live ? 24 : (int) (24 * age / (double) WHIRL_WARN);
                for (int arm = 0; arm < 4; arm++) {
                    for (int i = 0; i < reach; i++) {
                        double f = i / 24.0, r = WHIRL_RADIUS * (1.0 - f), a = arm * Math.PI / 2 + spin + f * 3.0;
                        Vec3 p = w.centre.add(Math.cos(a) * r, 0.12, Math.sin(a) * r);
                        // gliding inward along the arm
                        double r2 = Math.max(0.2, r - 0.6), a2 = a + 0.5;
                        BossFx.streak(level, p, w.centre.add(Math.cos(a2) * r2, 0.12 - (live ? 0.15 : 0), Math.sin(a2) * r2), i % 3 == 0 ? WHIRL_SEA : WHIRL_FOAM, 3);
                    }
                }
                BossFx.ring(level, w.centre, 0.8, 12, WHIRL_DEEP, 3, 0.15);
                BossFx.ring(level, w.centre, 1.2, 16, WHIRL_DEEP, 3, 0.15);
            }
            if (!live) continue;
            if (age % 8 == 0) level.playSound(null, w.centre.x, w.centre.y, w.centre.z, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundSource.HOSTILE, 1.6F, 0.6F);
            level.sendParticles(ParticleTypes.BUBBLE, w.centre.x, w.centre.y - 0.3, w.centre.z, 6, 1.0, 0.3, 1.0, 0.05);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(w.centre, w.centre).inflate(WHIRL_RADIUS, 3.0, WHIRL_RADIUS), this::isFoe)) {
                Vec3 in = w.centre.subtract(e.position()).multiply(1, 0, 1);
                double d = in.length();
                if (d > WHIRL_RADIUS) continue;
                // pulled round and in (and under, in water)
                Vec3 round = new Vec3(-in.z, 0, in.x).normalize().scale(0.06);
                Vec3 pull = d > 0.3 ? in.normalize().scale(0.07) : Vec3.ZERO;
                e.push(pull.x + round.x, e.isInWater() ? -0.03 : 0, pull.z + round.z);
                e.needsSync = true;
                if (d < 1.8 && age % 10 == 0 && e.hurtServer(level, this.damageSources().indirectMagic(this, this), 3.0F) && e instanceof Player) this.reached();
            }
        }
    }

    // she stays up on the water, she doesn't swim at you (BloodMoonBoss)
    @Override
    protected boolean swimsAtTarget() {
        return false;
    }

    // ── the blood bubble: shot again and again with nobody coming close, she shuts herself in and heals ──
    // Its own look: a big glassy sphere of dark blood round her, its rings slowly turning, pale pink glints on it.

    private static final int BUBBLE_DARK = 0x9A0A24, BUBBLE_PINK = 0xFF7090, BUBBLE_GLINT = 0xFFD0DC;

    private double bubbleRadius() {
        return this.getBbHeight() * 0.7 + 0.9;
    }

    private void enterBubble(ServerLevel level) {
        this.setAction(BUBBLE);
        this.setNoGravity(false);
        this.drained = null;
        this.getNavigation().stop();
        this.sound(SoundEvents.BEACON_ACTIVATE, 2.0F, 0.6F);
        this.sound(SoundEvents.WARDEN_HEARTBEAT, 2.0F, 0.8F);
        BossFx.burst(level, this.random, this.position().add(0, this.getBbHeight() * 0.5, 0), 40, this.bubbleRadius(), BUBBLE_PINK, 6);
    }

    private void popBubble(ServerLevel level) {
        this.arrowHits = 0;
        Vec3 mid = this.position().add(0, this.getBbHeight() * 0.5, 0);
        BossFx.burst(level, this.random, mid, 80, this.bubbleRadius() + 2.5, BUBBLE_PINK, 7);
        BossFx.burst(level, this.random, mid, 40, this.bubbleRadius() + 1.5, BUBBLE_DARK, 7);
        level.sendParticles(new DustParticleOptions(0xC01030, 2.0F), mid.x, mid.y, mid.z, 30, 1.0, 1.0, 1.0, 0);
        this.sound(SoundEvents.GLASS_BREAK, 2.0F, 0.6F);
        this.sound(SoundEvents.GENERIC_SPLASH, 1.6F, 0.6F);
        this.setAction(EXHAUSTED);
    }

    private void drawBubble(ServerLevel level, int t) {
        Vec3 mid = this.position().add(0, this.getBbHeight() * 0.5, 0);
        double r = this.bubbleRadius() * (t < 6 ? (t + 1) / 6.0 : 1.0 + 0.03 * Math.sin(t * 0.3));
        double spin = t * 0.05;
        // rings of latitude, and turning rings of longitude
        for (int lat = -3; lat <= 3; lat++) {
            double phi = lat * Math.PI / 8.0, rr = r * Math.cos(phi), y = r * Math.sin(phi);
            int n = Math.max(12, (int) (rr * Math.PI * 2 / 0.3));
            for (int i = 0; i < n; i++) {
                double a = i * Math.PI * 2 / n + spin;
                BossFx.dot(level, mid.add(Math.cos(a) * rr, y, Math.sin(a) * rr), BUBBLE_DARK, 3);
            }
        }
        for (int m = 0; m < 8; m++) {
            double a = m * Math.PI / 8.0 - spin;
            int n = (int) (r * Math.PI / 0.3);
            for (int i = 0; i <= n; i++) {
                double phi = -Math.PI / 2 + i * Math.PI / n;
                BossFx.dot(level, mid.add(Math.cos(a) * Math.cos(phi) * r, Math.sin(phi) * r, Math.sin(a) * Math.cos(phi) * r), m % 2 == 0 ? BUBBLE_PINK : BUBBLE_DARK, 3);
            }
        }
        // its skin: a shimmer of blood all over it, so it reads as one solid bubble
        for (int i = 0; i < 70; i++) {
            Vec3 d = new Vec3(this.random.nextGaussian(), this.random.nextGaussian(), this.random.nextGaussian());
            if (d.lengthSqr() < 1.0E-4) continue;
            BossFx.dot(level, mid.add(d.normalize().scale(r)), i % 4 == 0 ? BUBBLE_PINK : 0xC01838, 4);
        }
        // glints sliding over it
        for (int i = 0; i < 4; i++) {
            Vec3 d = new Vec3(this.random.nextGaussian(), this.random.nextGaussian(), this.random.nextGaussian());
            if (d.lengthSqr() < 1.0E-4) continue;
            d = d.normalize();
            BossFx.streak(level, mid.add(d.scale(r)), mid.add(d.add(0, 0.5, 0).normalize().scale(r)), BUBBLE_GLINT, 6);
        }
    }

    // in her bubble she heals by herself; the hiding heal (BloodMoonBoss) doesn't stack on it
    @Override
    protected boolean healsItselfAnyway() {
        return this.action() == BUBBLE;
    }

    @Override
    protected void clientEffects() {
        // drips of blood falling from her robe
        if (this.random.nextInt(3) == 0) {
            this.level().addParticle(ParticleTypes.DRIPPING_DRIPSTONE_LAVA, this.getRandomX(0.6), this.getY() + 0.3, this.getRandomZ(0.6), 0, 0, 0);
        }
        if ((this.action() == BOLTS || this.action() == RAIN) && this.random.nextBoolean()) {
            this.level().addParticle(new DustParticleOptions(0xFF2040, 1.0F), this.getRandomX(0.8), this.getY() + this.getBbHeight() * 0.85, this.getRandomZ(0.8), 0, 0.05, 0);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITCH_CELEBRATE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WITCH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ILLUSIONER_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.7F;
    }
}
