package com.alexh.capitalism.event;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

// The Crimson Revenant (Events.md > The bosses): one of the Blood Moon's three bosses (BloodMoonBoss). A huge armoured
// undead knight with a greatsword, glowing red through the cracks in its armour. It fights:
// - with its greatsword up close (a heavy hit);
// - GROUND SLAM: when players crowd it, it raises its sword for a second (red sparks gather) and smashes the ground:
//   everyone within 5.5 blocks is hurt and thrown back and up;
// - SWEEP: it pulls its sword back and spins, a full circle: everyone within 4.8 blocks is hurt and flung aside;
// - BLOOD LINE: at middle range it drives its sword into the ground and a line of eruptions races out at you (red
//   sparks warn of each one first): step out of the line;
// - LEAP: whoever keeps their distance (or shoots it from far off) gets leapt at: it lands with a smaller slam;
// - THRALLS: at half health, and again at a quarter, five Level II zombies rise round it; every thrall that dies near it
//   heals it a little (BloodMoonBoss.onThrallDeath).
public class CrimsonRevenant extends BloodMoonBoss {

    public static final int SLAM_WINDUP = 1;
    public static final int SLAM = 2;
    public static final int LEAP = 3;
    public static final int SUMMON = 4;
    public static final int SWEEP_WINDUP = 5;
    public static final int SWEEP = 6;
    public static final int BLOOD_LINE = 7;
    // in water (it or its target): it drives its sword into the water and geysers burst up under everyone near
    public static final int GEYSER = 8;

    static final double SLAM_RANGE = 5.5;
    static final int SLAM_WINDUP_TICKS = 22;
    public static final int SWEEP_WINDUP_TICKS = 12;

    private int slamCooldown = 60;
    private int sweepCooldown = 40;
    private int lineCooldown = 80;
    private int leapCooldown = 40;
    private int geyserCooldown = 40;
    private int airTicks;
    // where a leap will come down (its gold marker)
    private @org.jspecify.annotations.Nullable Vec3 leapMark;
    // each attack its own colour (BossFx): slam ember orange, sweep steel white, blood line crimson, leap gold
    private static final int EMBER = 0xFF7A20, EMBER_HOT = 0xFFD080, STEEL = 0xE8F0FF, STEEL_DARK = 0x9AA8C0,
        CRIMSON = 0xFF1830, GOLD = 0xFFC040, GOLD_HOT = 0xFFF0A0, SOUL_BLUE = 0x40E0FF, AQUA = 0x30C8FF, FOAM = 0xE0FFFF;
    private static final double[][] SLAM_WAVE = {{0.1, 0xC03808}, {0.4, EMBER}, {0.7, EMBER_HOT}};
    private static final double[][] LEAP_WAVE = {{0.1, 0xA07010}, {0.4, GOLD}, {0.7, GOLD_HOT}};

    public CrimsonRevenant(EntityType<? extends CrimsonRevenant> type, Level level) {
        super(type, level, "Crimson Revenant", 0xFF3030);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 360.0)
            .add(Attributes.ATTACK_DAMAGE, 14.0)
            .add(Attributes.ATTACK_KNOCKBACK, 1.5)
            .add(Attributes.ARMOR, 12.0)
            .add(Attributes.ARMOR_TOUGHNESS, 4.0)
            .add(Attributes.MOVEMENT_SPEED, 0.27)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.55)
            .add(Attributes.FOLLOW_RANGE, 48.0)
            .add(Attributes.STEP_HEIGHT, 1.5)
            .add(Attributes.SCALE, 1.75);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true) {
            @Override
            public boolean canUse() {
                return CrimsonRevenant.this.action() == IDLE && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return CrimsonRevenant.this.action() == IDLE && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected net.minecraft.world.level.ItemLike crestPiece() {
        return com.alexh.capitalism.core.ModItems.REVENANT_CREST.get();
    }

    @Override
    protected int mistColor() {
        return 0xB01010;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.slamCooldown > 0) this.slamCooldown--;
        if (this.sweepCooldown > 0) this.sweepCooldown--;
        if (this.lineCooldown > 0) this.lineCooldown--;
        if (this.leapCooldown > 0) this.leapCooldown--;
        if (this.geyserCooldown > 0) this.geyserCooldown--;
        LivingEntity target = this.getTarget();
        int t = this.actionTicks();
        switch (this.action()) {
            case SLAM_WINDUP -> {
                this.getNavigation().stop();
                if (target != null) this.getLookControl().setLookAt(target);
                // the danger circle (as far as it hits) in ember orange, a ring closing in to him as the countdown, embers
                // drawn up into the raised sword
                BossFx.ring(level, this.position(), SLAM_RANGE, 40, EMBER, 2, 0.08);
                BossFx.ring(level, this.position(), Math.max(0.3, SLAM_RANGE * (1.0 - t / (double) SLAM_WINDUP_TICKS)), 24, EMBER_HOT, 2, 0.08);
                for (int i = 0; i < 3; i++) {
                    double a = this.random.nextDouble() * Math.PI * 2, r = 1.5 + this.random.nextDouble() * 2.5;
                    BossFx.streak(level, this.position().add(Math.cos(a) * r, 0.1, Math.sin(a) * r), this.position().add(0, 4.2, 0), i == 0 ? EMBER_HOT : EMBER, 8);
                }
                if (t >= SLAM_WINDUP_TICKS) {
                    this.slam(level, SLAM_RANGE, 12.0F, 1.4, SLAM_WAVE, EMBER);
                    this.setAction(SLAM);
                }
                return;
            }
            case SLAM -> {
                this.getNavigation().stop();
                if (t >= 12) this.setAction(IDLE);
                return;
            }
            case SWEEP_WINDUP -> {
                this.getNavigation().stop();
                if (target != null) this.getLookControl().setLookAt(target);
                // steel white: the reach of the swing on the ground, and glints drawn into the blade
                BossFx.ring(level, this.position(), 4.8, 40, STEEL, 2, 0.08);
                if (t % 2 == 0) BossFx.burst(level, this.random, this.position().add(0, 1.8, 0), 3, 1.2, STEEL, 4);
                if (t >= SWEEP_WINDUP_TICKS) {
                    this.setAction(SWEEP);
                    this.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.0F, 0.5F);
                }
                return;
            }
            case SWEEP -> {
                this.getNavigation().stop();
                if (t >= 3 && t <= 6) {
                    // the blade's arc: steel streaks whipping round and out in a spiral, over a few ticks
                    double turn = (t - 3) * 0.9;
                    for (int i = 0; i < 48; i++) {
                        double a = i * Math.PI * 2 / 48 + turn;
                        for (double h : new double[]{1.2, 1.6, 2.0}) {
                            Vec3 from = this.position().add(Math.cos(a) * 1.2, h, Math.sin(a) * 1.2);
                            Vec3 to = this.position().add(Math.cos(a + 0.6) * 4.8, h - 0.2, Math.sin(a + 0.6) * 4.8);
                            BossFx.streak(level, from, to, h == 1.6 ? STEEL : STEEL_DARK, 6);
                        }
                    }
                }
                if (t == 3) {
                    this.hurtAround(level, this.position(), 4.8, 10.0F, 1.3, 0.35, null);
                    // and the sweep marks
                    for (int i = 0; i < 12; i++) {
                        double a = i * Math.PI * 2 / 12;
                        level.sendParticles(ParticleTypes.SWEEP_ATTACK, this.getX() + Math.cos(a) * 3.2, this.getY() + 1.6, this.getZ() + Math.sin(a) * 3.2, 1, 0, 0, 0, 0);
                    }
                }
                if (t >= 12) this.setAction(IDLE);
                return;
            }
            case BLOOD_LINE -> {
                this.getNavigation().stop();
                if (t < 12 && target != null) this.getLookControl().setLookAt(target);
                // crimson gathering at its feet
                if (t < 12) BossFx.groundBurst(level, this.random, this.position(), 3, 1.6, CRIMSON, 6);
                if (t == 12) this.bloodLine(level, target);
                if (t >= 30) this.setAction(IDLE);
                return;
            }
            case LEAP -> {
                if (!this.landed()) this.airTicks++;
                // the gold marker where it's coming down, and a gold streak behind it in the air
                if (this.leapMark != null) {
                    BossFx.ring(level, this.leapMark, 3.5, 32, GOLD, 2, 0.08);
                    BossFx.ring(level, this.leapMark, 1.2 + 0.4 * Math.sin(t * 0.6), 14, GOLD_HOT, 2, 0.08);
                }
                if (!this.landed()) {
                    Vec3 behind = this.position().subtract(this.getDeltaMovement().scale(3.0));
                    for (int i = 0; i < 4; i++) BossFx.streak(level, this.position().add(0, 1.0 + i * 0.6, 0), behind.add(0, 1.0 + i * 0.6, 0), i % 2 == 0 ? GOLD : GOLD_HOT, 5);
                }
                if (this.airTicks > 3 && this.landed() || t > 60) {
                    this.slam(level, 3.5, 8.0F, 0.9, LEAP_WAVE, GOLD);
                    // walled in or up out of reach: it comes down through your roof (never below your feet)
                    if (this.breaching()) this.smashBlocks(level, this.position().add(0, 1.0, 0), 3.0, target != null ? target.getBlockY() : this.getBlockY(), 60);
                    this.leapMark = null;
                    this.airTicks = 0;
                    this.setAction(IDLE);
                }
                return;
            }
            case GEYSER -> {
                // AQUA (its own colour): the sword driven down, bubbles and spray boiling up round it, then geysers burst
                // up under everyone near (BloodMoonBoss eruptions: an aqua circle and countdown ring first)
                this.getNavigation().stop();
                if (target != null) this.getLookControl().setLookAt(target);
                if (t < 16) {
                    BossFx.ring(level, this.position(), 2.5, 24, AQUA, 2, 0.1);
                    BossFx.burst(level, this.random, this.position().add(0, 0.3, 0), 3, 2.0, t % 2 == 0 ? FOAM : AQUA, 6);
                    level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, this.getX(), this.getY() + 0.3, this.getZ(), 6, 1.2, 0.2, 1.2, 0.1);
                }
                if (t == 16) this.geysers(level, target);
                if (t >= 30) this.setAction(IDLE);
                return;
            }
            case SUMMON -> {
                this.getNavigation().stop();
                if (t % 2 == 0) level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 0.2, this.getZ(), 6, 2.5, 0.1, 2.5, 0.02);
                BossFx.ring(level, this.position(), 4.0, 32, SOUL_BLUE, 2, 0.08);
                if (t == 30) {
                    this.sound(SoundEvents.EVOKER_CAST_SPELL, 2.0F, 0.5F);
                    this.callThralls(level, EntityTypes.ZOMBIE, 5, "Thrall", null);
                }
                if (t >= 40) this.setAction(IDLE);
                return;
            }
            default -> {
            }
        }
        // what next
        if (this.timeToCall()) {
            this.setAction(SUMMON);
            this.sound(SoundEvents.EVOKER_PREPARE_SUMMON, 2.0F, 0.6F);
            return;
        }
        // walled in: it smashes the wall it walks into
        if (this.breaching() && this.horizontalCollision && this.tickCount % 10 == 0) {
            Vec3 look = Vec3.directionFromRotation(0, this.getYRot());
            this.smashBlocks(level, this.position().add(look.x * 1.4, 1.5, look.z * 1.4), 1.8, this.getBlockY(), 20);
        }
        if (target == null || !target.isAlive() || !this.landed()) return;
        double dist = this.distanceTo(target);
        if (this.geyserCooldown <= 0 && (this.isInWater() || target.isInWater()) && dist < 24.0) {
            this.setAction(GEYSER);
            this.geyserCooldown = 120 + this.random.nextInt(60);
            this.sound(SoundEvents.ELDER_GUARDIAN_CURSE, 1.4F, 0.6F);
            return;
        }
        if (this.slamCooldown <= 0 && (this.playersWithin(SLAM_RANGE) >= 2 || dist < 4.5 && this.sweepCooldown > 0)) {
            this.setAction(SLAM_WINDUP);
            this.slamCooldown = 120 + this.random.nextInt(60);
            this.sound(SoundEvents.RAVAGER_ROAR, 1.6F, 0.6F);
            return;
        }
        if (this.sweepCooldown <= 0 && dist < 4.5) {
            this.setAction(SWEEP_WINDUP);
            this.sweepCooldown = 90 + this.random.nextInt(50);
            this.sound(SoundEvents.WITHER_SKELETON_AMBIENT, 2.0F, 0.5F);
            return;
        }
        if (this.lineCooldown <= 0 && dist > 6.0 && dist < 18.0 && this.hasLineOfSight(target)) {
            this.setAction(BLOOD_LINE);
            this.lineCooldown = 140 + this.random.nextInt(60);
            this.sound(SoundEvents.EVOKER_PREPARE_ATTACK, 2.0F, 0.5F);
            return;
        }
        if ((this.leapCooldown <= 0 && dist > 9.0 || this.breaching() && this.leapCooldown <= 60) && dist < 30.0) {
            this.jumpAt(target, 2.2, 0.75);
            int my = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target.getBlockX(), target.getBlockZ());
            this.leapMark = new Vec3(target.getX(), my, target.getZ());
            this.setAction(LEAP);
            this.airTicks = 0;
            this.leapCooldown = 100 + this.random.nextInt(60);
            this.sound(SoundEvents.RAVAGER_ATTACK, 1.6F, 0.6F);
        }
    }

    // geysers: one under everyone near (and its target), and a ring of them round it
    private void geysers(ServerLevel level, @org.jspecify.annotations.Nullable LivingEntity target) {
        this.sound(SoundEvents.GENERIC_SPLASH, 2.0F, 0.5F);
        this.sound(SoundEvents.ANVIL_LAND, 1.2F, 0.5F);
        java.util.List<LivingEntity> marked = new java.util.ArrayList<>(level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(24.0), this::isFoe));
        if (target != null && !marked.contains(target)) marked.add(target);
        int k = 0;
        for (LivingEntity e : marked) {
            if (k++ >= 5) break;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, e.getBlockX(), e.getBlockZ());
            this.erupt(new Vec3(e.getX(), Math.min(y, e.getY() + 1.0), e.getZ()), 22, 2.0, 10.0F, AQUA);
        }
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3;
            int x = (int) Math.floor(this.getX() + Math.cos(a) * 4.5), z = (int) Math.floor(this.getZ() + Math.sin(a) * 4.5);
            this.erupt(new Vec3(this.getX() + Math.cos(a) * 4.5, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), this.getZ() + Math.sin(a) * 4.5), 14 + i * 2, 1.6, 8.0F, AQUA);
        }
    }

    // the sword driven into the ground: eruptions race out toward the target, one after another
    private void bloodLine(ServerLevel level, LivingEntity target) {
        Vec3 dir = target != null ? target.position().subtract(this.position()).multiply(1, 0, 1)
            : Vec3.directionFromRotation(0, this.getYRot());
        if (dir.lengthSqr() < 0.01) return;
        dir = dir.normalize();
        this.sound(SoundEvents.ANVIL_LAND, 1.4F, 0.5F);
        // a glowing crack in the ground along the whole line, then the spikes along it one after another
        BossFx.line(level, this.position().add(dir.x * 2, 0.06, dir.z * 2), this.position().add(dir.x * 17, 0.06, dir.z * 17), 40, 0x8A0010, 40);
        BossFx.groundBurst(level, this.random, this.position().add(dir.x * 2, 0, dir.z * 2), 20, 1.5, CRIMSON, 6);
        for (int i = 0; i < 10; i++) {
            double d = 2.5 + i * 1.6;
            int x = (int) Math.floor(this.getX() + dir.x * d), z = (int) Math.floor(this.getZ() + dir.z * d);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (Math.abs(y - this.getY()) > 6) continue;
            this.erupt(new Vec3(this.getX() + dir.x * d, y, this.getZ() + dir.z * d), 8 + i * 3, 1.4, 9.0F, CRIMSON);
        }
    }

    // the sword smashes the ground: everyone near is hurt and thrown back
    private void slam(ServerLevel level, double range, float damage, double push, double[][] wave, int color) {
        var ground = level.getBlockState(this.blockPosition().below());
        if (!ground.isAir()) {
            level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, ground), this.getX(), this.getY() + 0.1, this.getZ(), 80, range * 0.4, 0.1, range * 0.4, 0.3);
        }
        level.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 0.3, this.getZ(), 3, range * 0.3, 0.1, range * 0.3, 0.0);
        // a shockwave rolling out to its reach, debris flung along the ground
        BossFx.wave(level, this.position(), 0.5, range, 6, 90, wave);
        BossFx.groundBurst(level, this.random, this.position(), 40, range, color, 7);
        this.sound(SoundEvents.GENERIC_EXPLODE.value(), 1.4F, 0.6F);
        this.sound(SoundEvents.ANVIL_LAND, 1.2F, 0.5F);
        this.hurtAround(level, this.position(), range, damage, push, 0.55, null);
    }

    // shot from far off: it comes for you
    @Override
    protected void hitFromAfar(LivingEntity attacker) {
        super.hitFromAfar(attacker);
        this.leapCooldown = Math.min(this.leapCooldown, 10);
    }

    // its greatsword sends people flying
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit) this.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.4F, 0.6F);
        return hit;
    }

    // ── the client: glowing embers off it, sparks as it winds up ──

    @Override
    protected void clientEffects() {
        if (this.random.nextInt(3) == 0) {
            this.level().addParticle(ParticleTypes.CRIMSON_SPORE, this.getRandomX(0.8), this.getRandomY(), this.getRandomZ(0.8), 0, 0.02, 0);
        }
        if ((this.action() == SLAM_WINDUP || this.action() == BLOOD_LINE && this.actionTicks() < 12) && this.random.nextBoolean()) {
            this.level().addParticle(ParticleTypes.FLAME, this.getRandomX(0.6), this.getY() + 3.0 + this.random.nextDouble(), this.getRandomZ(0.6), 0, 0.05, 0);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITHER_SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.55F;
    }
}
