package com.alexh.capitalism.event;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
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
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// Gorefang, the Blood Wolf (Events.md > The bosses): one of the Blood Moon's three bosses (BloodMoonBoss). A huge dire
// wolf: spiky black mane, a bone mask with ram horns, ribs and fire showing through its flanks. Fast and savage. It fights:
// - BITE up close: a quick, hard bite that makes you bleed (Wither for a few seconds);
// - POUNCE: it crouches low (a moment's warning), then springs onto whoever it's after: a heavy landing that hurts and
//   slows everyone near;
// - CHARGE: it lowers its head, scrapes the ground, then thunders straight ahead, ramming and flinging anything in its
//   way. If it runs into a wall or a tree it's left DAZED for two seconds (stars round its head): hit it then!
// - HOWL: it rears back and howls: everyone near goes dark (Darkness) and slow for a moment, and every monster around
//   gets faster and stronger. At half health and at a quarter its howl calls four Blood Hounds (angry Level II wolves)
//   that heal it when they die near it;
// - FRENZY: under a third of its health, it's faster and its moves come round quicker.
public class Gorefang extends BloodMoonBoss {

    public static final int POUNCE_CROUCH = 1;
    public static final int POUNCE = 2;
    public static final int CHARGE_WINDUP = 3;
    public static final int CHARGE = 4;
    public static final int DAZED = 5;
    public static final int HOWL = 6;
    // in water (it or its target): it churns the water, then surges through it at you like a shark, dragging you under
    public static final int RIPTIDE = 7;
    static final int RIPTIDE_WINDUP = 12, RIPTIDE_TICKS = 16;

    public static final int CROUCH_TICKS = 14;
    public static final int CHARGE_WINDUP_TICKS = 20;
    static final int CHARGE_TICKS = 34;

    // the big wolf voice (vanilla wolves have sound sets)
    private static final net.minecraft.world.entity.animal.wolf.WolfSoundVariant.WolfSoundSet WOLF =
        SoundEvents.WOLF_SOUNDS.get(net.minecraft.world.entity.animal.wolf.WolfSoundVariants.SoundSet.BIG).adultSounds();

    private static SoundEvent growl() {
        return WOLF.growlSound().value();
    }

    private int pounceCooldown = 50;
    private int chargeCooldown = 100;
    private int howlCooldown = 160;
    private int riptideCooldown = 40;
    private int airTicks;
    private boolean callPack;
    private boolean frenzy;
    private Vec3 chargeDir = Vec3.ZERO;
    // where a pounce will land (its red mark)
    private @org.jspecify.annotations.Nullable Vec3 pounceMark;
    // each attack its own colour (BossFx): pounce blood red, charge amber lane + white wind, howl ghost blue, dazed
    // gold stars, frenzy fire
    private static final int POUNCE_RED = 0xFF2A1A, POUNCE_DARK = 0x8A1010, AMBER = 0xFFB020, AMBER_HOT = 0xFFE080,
        WIND = 0xF4F6FF, GHOST = 0xBFD8FF, GHOST_DEEP = 0x6F90E0, STAR = 0xFFE040, FIRE = 0xFF5010, FIRE_HOT = 0xFFC040, TIDE = 0x30F0D0, TIDE_DEEP = 0x108878, FOAM = 0xE8FFFF;
    static final double POUNCE_RADIUS = 3.2;
    private final Set<UUID> rammed = new HashSet<>();

    public Gorefang(EntityType<? extends Gorefang> type, Level level) {
        super(type, level, "Gorefang, the Blood Wolf", 0xFF4030);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 320.0)
            .add(Attributes.ATTACK_DAMAGE, 10.0)
            .add(Attributes.ATTACK_KNOCKBACK, 1.0)
            .add(Attributes.ARMOR, 8.0)
            .add(Attributes.ARMOR_TOUGHNESS, 2.0)
            .add(Attributes.MOVEMENT_SPEED, 0.33)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
            .add(Attributes.FOLLOW_RANGE, 48.0)
            .add(Attributes.STEP_HEIGHT, 1.5)
            .add(Attributes.SCALE, 1.9);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true) {
            @Override
            public boolean canUse() {
                return Gorefang.this.action() == IDLE && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return Gorefang.this.action() == IDLE && super.canContinueToUse();
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
        return com.alexh.capitalism.core.ModItems.GOREFANG_CREST.get();
    }

    @Override
    protected int mistColor() {
        return 0x8A1010;
    }

    // (the client sees it by its health)
    public boolean frenzied() {
        return this.frenzy || this.getHealth() < this.getMaxHealth() / 3;
    }

    private int cool(int ticks) {
        return this.frenzy ? ticks * 3 / 5 : ticks;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.pounceCooldown > 0) this.pounceCooldown--;
        if (this.chargeCooldown > 0) this.chargeCooldown--;
        if (this.howlCooldown > 0) this.howlCooldown--;
        if (this.riptideCooldown > 0) this.riptideCooldown--;
        // frenzy: under a third of its health
        if (!this.frenzy && this.getHealth() < this.getMaxHealth() / 3) {
            this.frenzy = true;
            var speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) speed.setBaseValue(0.42);
            this.sound(SoundEvents.RAVAGER_ROAR, 2.0F, 1.2F);
            BossFx.burst(level, this.random, this.position().add(0, 1.2, 0), 60, 3.0, FIRE, 8);
            BossFx.burst(level, this.random, this.position().add(0, 1.2, 0), 30, 2.0, FIRE_HOT, 6);
        }
        // frenzied: embers of fire rising off it
        if (this.frenzy && this.tickCount % 2 == 0) {
            Vec3 p = new Vec3(this.getRandomX(0.8), this.getY() + this.random.nextDouble() * 1.6, this.getRandomZ(0.8));
            BossFx.streak(level, p, p.add(0, 1.2, 0), this.random.nextBoolean() ? FIRE : FIRE_HOT, 8);
        }
        LivingEntity target = this.getTarget();
        int t = this.actionTicks();
        switch (this.action()) {
            case POUNCE_CROUCH -> {
                this.getNavigation().stop();
                if (target != null) {
                    this.getLookControl().setLookAt(target);
                    this.face(target.position().subtract(this.position()));
                }
                // the red mark where it will land (as far as the landing hurts), with three claw slashes, following the target
                if (target != null) {
                    int my = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target.getBlockX(), target.getBlockZ());
                    this.pounceMark = new Vec3(target.getX(), my, target.getZ());
                }
                if (this.pounceMark != null) this.drawPounceMark(level);
                if (t >= CROUCH_TICKS) {
                    if (target != null) this.jumpAt(target, 1.9, 0.6);
                    this.setAction(POUNCE);
                    this.airTicks = 0;
                    this.sound(growl(), 2.0F, 0.5F);
                }
                return;
            }
            case POUNCE -> {
                // facing where it's flying
                this.face(this.getDeltaMovement());
                if (!this.landed()) this.airTicks++;
                if (this.pounceMark != null) this.drawPounceMark(level);
                if (!this.landed()) {
                    // a streak of red behind it through the air
                    Vec3 behind = this.position().subtract(this.getDeltaMovement().scale(2.5));
                    for (int i = 0; i < 4; i++) BossFx.streak(level, this.position().add(0, 0.6 + i * 0.4, 0), behind.add(0, 0.6 + i * 0.4, 0), i % 2 == 0 ? POUNCE_RED : POUNCE_DARK, 5);
                }
                if (this.airTicks > 2 && this.landed() || t > 50) {
                    this.landing(level);
                    this.pounceMark = null;
                    this.setAction(IDLE);
                }
                return;
            }
            case CHARGE_WINDUP -> {
                this.getNavigation().stop();
                if (target != null) {
                    this.getLookControl().setLookAt(target);
                    Vec3 to = target.position().subtract(this.position()).multiply(1, 0, 1);
                    if (to.lengthSqr() > 0.01) this.chargeDir = to.normalize();
                    this.setYRot((float) (Math.atan2(this.chargeDir.z, this.chargeDir.x) * 180 / Math.PI) - 90.0F);
                    this.yBodyRot = this.getYRot();
                }
                if (t % 4 == 0) {
                    var ground = level.getBlockState(this.blockPosition().below());
                    if (!ground.isAir()) level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, ground),
                        this.getX(), this.getY() + 0.1, this.getZ(), 10, 0.6, 0.05, 0.6, 0.1);
                }
                // its lane painted on the ground in amber: how far it will run, a pulse racing down it
                this.drawLane(level, t);
                if (t >= CHARGE_WINDUP_TICKS) {
                    this.setAction(CHARGE);
                    this.rammed.clear();
                    this.sound(SoundEvents.RAVAGER_ROAR, 2.0F, 0.8F);
                }
                return;
            }
            case CHARGE -> {
                this.getNavigation().stop();
                double speed = this.frenzy ? 0.95 : 0.8;
                this.setDeltaMovement(this.chargeDir.x * speed, this.getDeltaMovement().y, this.chargeDir.z * speed);
                this.setYRot((float) (Math.atan2(this.chargeDir.z, this.chargeDir.x) * 180 / Math.PI) - 90.0F);
                this.yBodyRot = this.getYRot();
                this.yHeadRot = this.getYRot();
                if (t % 2 == 0) {
                    var ground = level.getBlockState(this.blockPosition().below());
                    if (!ground.isAir()) level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, ground),
                        this.getX(), this.getY() + 0.1, this.getZ(), 8, 0.6, 0.05, 0.6, 0.1);
                }
                // wind streaming off it, white, and amber sparks off its feet
                for (int i = 0; i < 6; i++) {
                    double side = (this.random.nextDouble() - 0.5) * 2.2, h = 0.3 + this.random.nextDouble() * 1.8;
                    Vec3 front = this.position().add(this.chargeDir.x * 1.6 - this.chargeDir.z * side, h, this.chargeDir.z * 1.6 + this.chargeDir.x * side);
                    BossFx.streak(level, front, front.subtract(this.chargeDir.scale(4.0)), WIND, 4);
                }
                BossFx.groundBurst(level, this.random, this.position(), 2, 1.2, AMBER, 5);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.6), this::isFoe)) {
                    if (!this.rammed.add(e.getUUID())) continue;
                    if (e.hurtServer(level, this.damageSources().mobAttack(this), 11.0F) && e instanceof Player) this.reached();
                    BossFx.burst(level, this.random, e.position().add(0, 1.0, 0), 20, 1.6, AMBER_HOT, 5);
                    e.push(this.chargeDir.x * 1.8, 0.6, this.chargeDir.z * 1.8);
                    e.needsSync = true;
                    this.sound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 2.0F, 0.6F);
                }
                // walled in (can't get at you): it plows straight through the wall instead of being dazed
                if (this.breaching()) this.smashBlocks(level, this.position().add(this.chargeDir.x * 1.6, 1.0, this.chargeDir.z * 1.6), 1.6, this.getBlockY(), 16);
                if (t > 4 && this.horizontalCollision && !this.breaching()) {
                    // into a wall: dazed
                    this.setDeltaMovement(this.chargeDir.x * -0.4, 0.3, this.chargeDir.z * -0.4);
                    this.setAction(DAZED);
                    this.sound(SoundEvents.ANVIL_LAND, 1.6F, 0.5F);
                    level.sendParticles(ParticleTypes.EXPLOSION, this.getX() + this.chargeDir.x * 1.5, this.getY() + 1, this.getZ() + this.chargeDir.z * 1.5, 2, 0.3, 0.3, 0.3, 0);
                } else if (t >= CHARGE_TICKS) {
                    this.setAction(IDLE);
                }
                return;
            }
            case RIPTIDE -> {
                // TIDE green-blue (its own colour): a churning ring of water round it and spray thrown up, then it surges
                // straight at you through the water, a white wake behind it; it rams (10), drags you under and slows you
                if (t < RIPTIDE_WINDUP) {
                    this.getNavigation().stop();
                    if (target != null) {
                        Vec3 to = target.position().subtract(this.position());
                        if (to.lengthSqr() > 0.01) this.chargeDir = to.normalize();
                        this.face(this.chargeDir);
                    }
                    double spin = t * 0.5;
                    Vec3 top = new Vec3(this.getX(), this.surfaceY(), this.getZ());
                    for (int i = 0; i < 48; i++) {
                        double a = i * Math.PI * 2 / 48 + spin;
                        Vec3 p = top.add(Math.cos(a) * 2.6, 0.1, Math.sin(a) * 2.6);
                        BossFx.streak(level, p, top.add(Math.cos(a + 0.5) * 1.6, 0.7, Math.sin(a + 0.5) * 1.6), i % 3 == 0 ? FOAM : TIDE, 3);
                    }
                    level.sendParticles(ParticleTypes.SPLASH, this.getX(), this.getY() + 0.8, this.getZ(), 10, 1.2, 0.3, 1.2, 0.2);
                    if (t == 0) this.sound(SoundEvents.DOLPHIN_SPLASH, 2.0F, 0.5F);
                    return;
                }
                if (t == RIPTIDE_WINDUP) {
                    this.rammed.clear();
                    this.sound(SoundEvents.TRIDENT_RIPTIDE_3.value(), 2.0F, 0.6F);
                }
                Vec3 dir = new Vec3(this.chargeDir.x, Math.max(-0.3, Math.min(0.3, this.chargeDir.y)), this.chargeDir.z);
                this.setDeltaMovement(dir.scale(1.0));
                this.face(this.chargeDir);
                // the wake: white foam and green-blue water streaming off it
                for (int i = 0; i < 14; i++) {
                    double side = (this.random.nextDouble() - 0.5) * 2.4, h = 0.1 + this.random.nextDouble() * 1.2;
                    Vec3 p = new Vec3(this.getX(), this.surfaceY(), this.getZ()).add(-this.chargeDir.z * side, h, this.chargeDir.x * side);
                    BossFx.streak(level, p, p.subtract(this.chargeDir.scale(4.0)).add(-this.chargeDir.z * side, 0.6, this.chargeDir.x * side), i % 2 == 0 ? FOAM : TIDE, 6);
                }
                level.sendParticles(ParticleTypes.BUBBLE, this.getX(), this.getY() + 0.5, this.getZ(), 8, 0.8, 0.5, 0.8, 0.1);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.8), this::isFoe)) {
                    if (!this.rammed.add(e.getUUID())) continue;
                    if (e.hurtServer(level, this.damageSources().mobAttack(this), 10.0F) && e instanceof Player) this.reached();
                    e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1), this);
                    // dragged under (in water), else thrown
                    e.push(this.chargeDir.x * 1.2, e.isInWater() ? -0.6 : 0.5, this.chargeDir.z * 1.2);
                    e.needsSync = true;
                    BossFx.burst(level, this.random, e.position().add(0, 0.8, 0), 24, 1.8, TIDE, 6);
                    level.sendParticles(ParticleTypes.SPLASH, e.getX(), e.getY() + 1.0, e.getZ(), 40, 0.6, 0.6, 0.6, 0.3);
                    this.sound(SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 2.0F, 0.6F);
                }
                if (t >= RIPTIDE_WINDUP + RIPTIDE_TICKS || this.horizontalCollision && t > RIPTIDE_WINDUP + 3) {
                    // it bursts up out of the water at the end
                    BossFx.burst(level, this.random, this.position().add(0, 1.0, 0), 40, 2.5, FOAM, 7);
                    level.sendParticles(ParticleTypes.SPLASH, this.getX(), this.getY() + 1.0, this.getZ(), 60, 1.2, 0.8, 1.2, 0.3);
                    this.setDeltaMovement(this.getDeltaMovement().multiply(0.2, 0, 0.2).add(0, this.isInWater() ? 0.5 : 0.2, 0));
                    this.setAction(IDLE);
                }
                return;
            }
            case DAZED -> {
                this.getNavigation().stop();
                // gold stars circling its head
                for (int i = 0; i < 5; i++) {
                    double a = i * Math.PI * 2 / 5 + t * 0.25;
                    BossFx.dot(level, this.position().add(Math.cos(a) * 1.0, this.getBbHeight() + 0.4 + 0.15 * Math.sin(a * 2 + t * 0.3), Math.sin(a) * 1.0), STAR, 2);
                }
                if (t >= 40) this.setAction(IDLE);
                return;
            }
            case HOWL -> {
                this.getNavigation().stop();
                // its breath rising before the howl, then three rings of ghost-blue sound rolling out
                if (t < 10) BossFx.burst(level, this.random, this.mouth().add(0, 0.3, 0), 2, 1.0, GHOST, 6);
                if (t == 10) this.howl(level);
                if (t == 10 || t == 16 || t == 22) BossFx.wave(level, this.position(), 1.0, 20.0, 24, 120,
                    new double[][]{{0.5, GHOST_DEEP}, {1.3, GHOST}, {2.1, GHOST_DEEP}});
                if (t >= 36) this.setAction(IDLE);
                return;
            }
            default -> {
            }
        }
        // what next
        if (this.timeToCall()) {
            this.callPack = true;
            this.setAction(HOWL);
            return;
        }
        if (target == null || !target.isAlive() || !this.landed()) return;
        double dist = this.distanceTo(target);
        if (this.riptideCooldown <= 0 && (this.isInWater() || target.isInWater()) && dist > 3.0 && dist < 18.0) {
            this.setAction(RIPTIDE);
            this.riptideCooldown = this.cool(100 + this.random.nextInt(50));
            this.sound(growl(), 2.0F, 0.5F);
            return;
        }
        if (this.howlCooldown <= 0 && dist < 16.0) {
            this.setAction(HOWL);
            this.howlCooldown = this.cool(320 + this.random.nextInt(100));
            return;
        }
        // walled in: it charges anyway, sooner and from closer, and plows through (CHARGE)
        boolean breach = this.breaching();
        if ((this.chargeCooldown <= 0 || breach && this.chargeCooldown <= 80) && dist > (breach ? 2.0 : 7.0) && dist < 24.0 && (breach || this.hasLineOfSight(target))) {
            this.setAction(CHARGE_WINDUP);
            this.chargeCooldown = this.cool(160 + this.random.nextInt(60));
            this.sound(growl(), 2.0F, 0.4F);
            return;
        }
        if (this.pounceCooldown <= 0 && dist > 4.0 && dist < 18.0) {
            this.setAction(POUNCE_CROUCH);
            this.pounceCooldown = this.cool(90 + this.random.nextInt(50));
            this.sound(growl(), 2.0F, 0.6F);
        }
    }

    // turn its whole body (not just its head) to face that way
    private void face(Vec3 dir) {
        if (dir.x * dir.x + dir.z * dir.z < 1.0E-4) return;
        float yaw = (float) (Math.atan2(dir.z, dir.x) * 180 / Math.PI) - 90.0F;
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
        this.yRotO = yaw;
        this.yBodyRotO = yaw;
    }

    // the pounce's mark: a red ring as big as the landing hurts, three claw slashes in it
    private void drawPounceMark(ServerLevel level) {
        Vec3 m = this.pounceMark;
        if (m == null) return;
        BossFx.ring(level, m, POUNCE_RADIUS, 32, POUNCE_RED, 2, 0.08);
        for (int k = -1; k <= 1; k++) {
            BossFx.line(level, m.add(-1.2 + k * 0.7, 0.08, -1.3), m.add(-0.4 + k * 0.7, 0.08, 1.3), 6, POUNCE_DARK, 2);
        }
    }

    // the charge's lane: two amber edges as far as it will run, and a pulse racing down it
    private void drawLane(ServerLevel level, int t) {
        Vec3 dir = this.chargeDir;
        if (dir.lengthSqr() < 1.0E-4) return;
        double length = (this.frenzy ? 0.95 : 0.8) * CHARGE_TICKS;
        // ...or up to the first wall in the way (where it will crash and be dazed)
        Vec3 eye = this.position().add(0, 1.0, 0);
        var wall = level.clip(new net.minecraft.world.level.ClipContext(eye, eye.add(dir.scale(length)), net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, this));
        if (wall.getType() != net.minecraft.world.phys.HitResult.Type.MISS) length = Math.max(1.0, wall.getLocation().distanceTo(eye));
        Vec3 side = new Vec3(-dir.z, 0, dir.x).scale(1.0);
        Vec3 start = this.position().add(0, 0.08, 0), end = start.add(dir.scale(length));
        BossFx.line(level, start.add(side), end.add(side), (int) (length / 0.6), AMBER, 2);
        BossFx.line(level, start.subtract(side), end.subtract(side), (int) (length / 0.6), AMBER, 2);
        double f = (t % 10) / 10.0;
        BossFx.line(level, start.add(dir.scale(length * f)).add(side), start.add(dir.scale(length * f)).subtract(side), 5, AMBER_HOT, 2);
    }

    // roughly where its mouth is
    private Vec3 mouth() {
        Vec3 look = Vec3.directionFromRotation(0, this.yBodyRot);
        return this.position().add(look.x * 2.0, 1.4, look.z * 2.0);
    }

    // it lands from a pounce: everyone near is hurt and slowed
    private void landing(ServerLevel level) {
        var ground = level.getBlockState(this.blockPosition().below());
        if (!ground.isAir()) level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, ground),
            this.getX(), this.getY() + 0.1, this.getZ(), 50, 1.4, 0.1, 1.4, 0.3);
        // a red shockwave out to its reach
        BossFx.wave(level, this.position(), 0.5, POUNCE_RADIUS, 4, 60, new double[][]{{0.1, POUNCE_DARK}, {0.4, POUNCE_RED}, {0.7, 0xFF8070}});
        this.sound(SoundEvents.RAVAGER_STEP, 2.0F, 0.5F);
        this.sound(SoundEvents.GENERIC_EXPLODE.value(), 0.8F, 0.8F);
        this.hurtAround(level, this.position(), POUNCE_RADIUS, 10.0F, 0.8, 0.4,
            e -> e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 1), this));
    }

    // the howl: darkness and slowness on players near, monsters round it faster and stronger; at half and a quarter,
    // the pack comes
    private void howl(ServerLevel level) {
        this.sound(WOLF.whineSound().value(), 3.0F, 0.4F);
        this.sound(growl(), 3.0F, 0.35F);
        this.sound(SoundEvents.WARDEN_ROAR, 1.2F, 1.4F);
        for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(20.0), this::isFoe)) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0), this);
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 0), this);
        }
        for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(24.0),
            e -> e instanceof Enemy && !(e instanceof BloodMoonBoss) || e.entityTags().contains(THRALL_TAG))) {
            mob.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 1), this);
            mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 200, 0), this);
        }
        if (this.callPack) {
            this.callPack = false;
            LivingEntity target = this.getTarget();
            this.callThralls(level, EntityTypes.WOLF, 4, "Blood Hound", mob -> {
                if (mob instanceof Wolf wolf && target != null) {
                    wolf.setPersistentAngerTarget(EntityReference.of(target));
                    wolf.startPersistentAngerTimer();
                }
            });
        }
    }

    // its bite: you bleed
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0), this);
            this.sound(growl(), 1.5F, 0.6F);
            // three claw slashes across them, just in front of them (between them and the wolf, so a bitten player sees
            // them across their view)
            Vec3 toWolf = this.position().subtract(target.position()).multiply(1, 0, 1);
            toWolf = toWolf.lengthSqr() < 1.0E-4 ? Vec3.ZERO : toWolf.normalize();
            Vec3 c = target.position().add(0, target.getBbHeight() * 0.75, 0).add(toWolf.scale(0.9));
            Vec3 side = new Vec3(-(target.getZ() - this.getZ()), 0, target.getX() - this.getX());
            side = side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize();
            for (int k = -1; k <= 1; k++) {
                Vec3 off = side.scale(k * 0.35);
                Vec3 from = c.add(side.scale(-1.0)).add(off).add(0, 0.9, 0), to = c.add(side.scale(1.0)).add(off).add(0, -0.9, 0);
                for (int i = 0; i < 12; i++) BossFx.streak(level, from.lerp(to, i / 20.0), from.lerp(to, 0.45 + i / 20.0), i % 3 == 0 ? 0xFF6060 : 0xC01020, 5);
            }
        }
        return hit;
    }

    // shot from far off: it pounces sooner
    @Override
    protected void hitFromAfar(LivingEntity attacker) {
        super.hitFromAfar(attacker);
        this.chargeCooldown = Math.min(this.chargeCooldown, 10);
    }

    @Override
    protected void clientEffects() {
        if (this.random.nextInt(4) == 0) {
            this.level().addParticle(new DustParticleOptions(0x700808, 0.9F), this.getRandomX(0.6), this.getY() + 0.1, this.getRandomZ(0.6), 0, 0, 0);
        }
        if (this.action() == DAZED && this.random.nextInt(3) == 0) {
            this.level().addParticle(ParticleTypes.CRIT, this.getRandomX(0.5), this.getY() + this.getBbHeight() + 0.2, this.getRandomZ(0.5), 0, 0.05, 0);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return growl();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return WOLF.hurtSound().value();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return WOLF.deathSound().value();
    }

    @Override
    public float getVoicePitch() {
        return 0.45F;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("frenzy", this.frenzy);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.frenzy = input.getBooleanOr("frenzy", false);
        if (this.frenzy) {
            var speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) speed.setBaseValue(0.42);
        }
    }
}
