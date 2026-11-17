package com.alexh.capitalism.event;

import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.core.TougherMobs;
import com.alexh.capitalism.economy.GoldMoney;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

// What every Blood Moon boss shares (Events.md > The bosses): BloodMoon picks one of them each Blood Moon and hides it
// out past the town. This class: the synced ACTION (what move it's making, its model poses from it) and how long it's
// been at it; its health bar (players within 48 blocks); "found" when someone near sees it; only tonight's boss stays
// (any other sinks away); a burst of blood and a little shove when it's hit; ground eruptions it can call up (warned
// by red sparks first); minions ("thralls") that heal it when they die near it; its drop (15 gold, 20 Blood
// Shards and its piece of the Blood Moon Crest); it never despawns, takes no fall damage, can't drown, can't be leashed or go through portals.
public abstract class BloodMoonBoss extends Monster {

    public static final int IDLE = 0;
    public static final String THRALL_TAG = "capitalism_revenant_thrall";

    private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(BloodMoonBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ACTION_TICKS = SynchedEntityData.defineId(BloodMoonBoss.class, EntityDataSerializers.INT);

    protected final ServerBossEvent bossBar;
    protected boolean calledAtHalf;
    protected boolean calledAtQuarter;
    private final List<Eruption> eruptions = new ArrayList<>();
    // (dev: client/PhotoShoot) run its moves though it has no AI (held still for the camera)
    public boolean photoAi;
    private final String name;
    private final int color;
    // anti-cheese (Events.md > The bosses > Hiding doesn't work): the last tick it hurt a player or was hit up close, the last
    // tick it was hurt at all, how long it's been unable to reach its target, how long it's been healing while out of reach
    private int lastReach;
    private int lastHurt = -1000;
    private int stuckTicks;
    private int hideHealTicks;
    // arrows and other shots do this much of their damage
    static final float RANGED_DAMAGE = 0.75F;
    // out of reach this long (nobody hit up close, nobody hurt by it) and it starts healing
    static final int HIDE_HEAL_AFTER = 300;
    // unable to reach its target this long and it starts tearing through blocks
    static final int BREACH_AFTER = 80;

    // a spot on the ground that bursts after a warning
    private record Eruption(Vec3 at, int start, int when, double radius, float damage, int color) {
    }

    protected BloodMoonBoss(EntityType<? extends BloodMoonBoss> type, Level level, String name, int color) {
        super(type, level);
        this.xpReward = 120;
        this.setPersistenceRequired();
        this.bossBar = new ServerBossEvent(java.util.UUID.randomUUID(),
            Component.literal(name).withStyle(s -> s.withColor(TextColor.fromRgb(color)).withBold(true)),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_12);
        this.name = name;
        this.color = color;
        // its hitbox: the entity was sized before it had its SCALE attribute (Entity's constructor runs first), and
        // nothing resizes it after, so it would keep the small unscaled box. Size it now.
        this.refreshDimensions();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(ACTION, IDLE);
        entityData.define(ACTION_TICKS, 0);
    }

    public int action() {
        return this.entityData.get(ACTION);
    }

    // ticks since the action began (the client poses it from this)
    public int actionTicks() {
        return this.entityData.get(ACTION_TICKS);
    }

    public void setAction(int action) {
        this.entityData.set(ACTION, action);
        this.entityData.set(ACTION_TICKS, 0);
    }

    // its colour of mist and sparks
    protected abstract int mistColor();

    // the client side each tick (embers, sparks...)
    protected abstract void clientEffects();

    // ── each tick ──

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.clientEffects();
            return;
        }
        ServerLevel level = (ServerLevel) this.level();
        // only on a Blood Moon night, and only tonight's: otherwise it sinks away
        if (!BloodMoon.isTheRevenant(level.getServer(), this)) {
            this.vanish();
            return;
        }
        BloodMoon.revenantAt(level.getServer(), this);
        // (dev: client/PhotoShoot) held still, but still making its moves
        if (this.photoAi) this.customServerAiStep(level);
        this.entityData.set(ACTION_TICKS, this.actionTicks() + 1);
        if (this.tickCount % 10 == 0) {
            this.bossBar.setProgress(this.getHealth() / this.getMaxHealth());
            for (ServerPlayer player : level.players()) {
                boolean near = player.distanceToSqr(this) < 48 * 48;
                if (near) this.bossBar.addPlayer(player);
                else this.bossBar.removePlayer(player);
                if (near && player.distanceToSqr(this) < 24 * 24 && this.hasLineOfSight(player) && !player.isSpectator()) BloodMoon.found(level.getServer(), player);
            }
        }
        if (this.tickCount % 4 == 0) {
            level.sendParticles(new DustParticleOptions(this.mistColor(), 1.6F), this.getX(), this.getY() + this.getBbHeight() * 0.4, this.getZ(),
                3, this.getBbWidth() * 0.6, this.getBbHeight() * 0.3, this.getBbWidth() * 0.6, 0.0);
        }
        if (this.tickCount % 10 == 0) this.checkStuck(level);
        this.tickHideHeal(level);
        this.tickSwim();
        this.tickEruptions(level);
    }

    // ── in water: it swims hard at its target, keeps its head up, and heaves itself out onto the bank ──

    // blocks a tick (6 a second)
    static final double SWIM_SPEED = 0.3;

    // the top of the water it's in (its feet when it isn't)
    protected double surfaceY() {
        return this.getY() + (this.isInWater() ? this.getFluidHeight(net.minecraft.tags.FluidTags.WATER) : 0.0);
    }

    // on its feet, or in water (its moves work in water too)
    protected boolean landed() {
        return this.onGround() || this.isInWater();
    }

    // false: it doesn't swim at its target (the witch keeps her distance), it just stays up on the water
    protected boolean swimsAtTarget() {
        return true;
    }

    private void tickSwim() {
        if (!this.isInWater() || this.isNoGravity()) return;
        Vec3 v = this.getDeltaMovement();
        double up = v.y;
        // afloat, head and shoulders out of the water
        if (this.getFluidHeight(net.minecraft.tags.FluidTags.WATER) > this.getBbHeight() * (this.swimsAtTarget() ? 0.45 : 0.1)) up = Math.max(up, 0.09);
        LivingEntity target = this.getTarget();
        if (this.swimsAtTarget() && target != null && target.isAlive() && this.action() == IDLE) {
            Vec3 to = target.position().subtract(this.position()).multiply(1, 0, 1);
            if (to.lengthSqr() > 4.0) {
                to = to.normalize();
                v = new Vec3(v.x + (to.x * SWIM_SPEED - v.x) * 0.3, v.y, v.z + (to.z * SWIM_SPEED - v.z) * 0.3);
                float yaw = (float) (Math.atan2(to.z, to.x) * 180 / Math.PI) - 90.0F;
                this.setYRot(yaw);
                this.yBodyRot = yaw;
                // up out onto the bank
                if (this.horizontalCollision && v.y < 0.2) up = 0.55;
            }
        }
        this.setDeltaMovement(v.x, up, v.z);
    }

    // ── hiding doesn't work: boxed in, shooting from a hut, it heals and it tears its way in ──

    // it hurt a player, or a player hit it up close: the fight is reaching it
    protected void reached() {
        this.lastReach = this.tickCount;
        this.hideHealTicks = 0;
    }

    protected boolean inCombat() {
        return this.getTarget() != null && this.getTarget().isAlive() || this.tickCount - this.lastHurt < 200;
    }

    // it can't get at its target (no way to walk there: walled in, up a pillar) and hasn't reached anyone for a while
    // (see BREACH_AFTER): its moves tear through blocks (each boss its own way)
    protected boolean breaching() {
        return this.stuckTicks >= BREACH_AFTER;
    }

    private void checkStuck(ServerLevel level) {
        LivingEntity target = this.getTarget();
        boolean stuck = false;
        if (target != null && target.isAlive() && this.distanceTo(target) < 40.0 && this.tickCount - this.lastReach > 60) {
            var path = this.getNavigation().createPath(target, 1);
            stuck = path == null || !path.canReach();
        }
        this.stuckTicks = stuck ? this.stuckTicks + 10 : 0;
    }

    // heals while nobody can get to it (or it to them): it isn't killed from a hiding place
    protected boolean healsItselfAnyway() {
        return false;
    }

    private void tickHideHeal(ServerLevel level) {
        if (this.healsItselfAnyway() || !this.inCombat() || this.tickCount - this.lastReach < HIDE_HEAL_AFTER
            || this.getHealth() >= this.getMaxHealth() || this.isDeadOrDying()) {
            return;
        }
        this.hideHealTicks++;
        if (this.hideHealTicks % 10 != 0) return;
        // 3 a second, rising to 6 the longer it goes on
        this.heal(1.5F + Math.min(1.5F, this.hideHealTicks / 200.0F * 1.5F));
        // its own look: dark blood drawn in out of the night from all round into its heart, and a heart over it
        Vec3 heart = this.position().add(0, this.getBbHeight() * 0.6, 0);
        for (int i = 0; i < 16; i++) {
            Vec3 dir = new Vec3(this.random.nextGaussian(), this.random.nextGaussian() * 0.5, this.random.nextGaussian());
            if (dir.lengthSqr() < 1.0E-4) continue;
            BossFx.streak(level, heart.add(dir.normalize().scale(3.5 + this.random.nextDouble())), heart, i % 3 == 0 ? 0xFF4050 : 0x700818, 8);
        }
        level.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + this.getBbHeight() + 0.4, this.getZ(), 1, 0.3, 0.1, 0.3, 0);
        if (this.hideHealTicks % 40 == 0) this.sound(SoundEvents.WARDEN_HEARTBEAT, 1.6F, 0.8F);
    }

    // tears through the blocks round `centre` (within `radius`, none below `minY`): never in the protected town or on a
    // lot, never anything with a block entity (chests...), nothing as tough as obsidian. They drop as if mined.
    protected int smashBlocks(ServerLevel level, Vec3 centre, double radius, int minY, int max) {
        int broken = 0;
        int r = (int) Math.ceil(radius);
        BlockPos mid = BlockPos.containing(centre);
        for (int dy = r; dy >= -r && broken < max; dy--) {
            for (int dx = -r; dx <= r && broken < max; dx++) {
                for (int dz = -r; dz <= r && broken < max; dz++) {
                    BlockPos pos = mid.offset(dx, dy, dz);
                    if (pos.getY() < minY || Vec3.atCenterOf(pos).distanceTo(centre) > radius + 0.5) continue;
                    if (!this.canSmash(level, pos)) continue;
                    if (level.destroyBlock(pos, true, this)) broken++;
                }
            }
        }
        if (broken > 0) {
            this.sound(SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 1.6F, 0.6F);
            level.sendParticles(ParticleTypes.EXPLOSION, centre.x, centre.y, centre.z, 1, 0, 0, 0, 0);
        }
        return broken;
    }

    private boolean canSmash(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || !state.getFluidState().isEmpty() || state.hasBlockEntity()) return false;
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0 || hardness >= 50.0F) return false;
        return !com.alexh.capitalism.town.Town.isProtected(level, pos) && com.alexh.capitalism.realestate.Properties.at(level, pos) == null;
    }


    // on the monsters' side: any monster, its thralls, another boss
    public static boolean isMonsterSide(LivingEntity e) {
        return e instanceof net.minecraft.world.entity.monster.Enemy || e.entityTags().contains(THRALL_TAG);
    }

    // ── eruptions: red sparks on the ground, then it bursts ──

    protected void erupt(Vec3 at, int delay, double radius, float damage, int color) {
        this.eruptions.add(new Eruption(at, this.tickCount, this.tickCount + delay, radius, damage, color));
    }

    private void tickEruptions(ServerLevel level) {
        for (Iterator<Eruption> it = this.eruptions.iterator(); it.hasNext(); ) {
            Eruption e = it.next();
            if (this.tickCount < e.when) {
                // the warning: a glowing circle where it will burst, and a ring closing in to its middle as the countdown
                double progress = (this.tickCount - e.start) / (double) Math.max(1, e.when - e.start);
                BossFx.ring(level, e.at, e.radius, 18, e.color, 2, 0.08);
                BossFx.ring(level, e.at, Math.max(0.1, e.radius * (1.0 - progress)), 10, 0xFFD0D0, 2, 0.08);
                continue;
            }
            it.remove();
            // the burst: spikes of blood shooting up out of the ground, the ground thrown up
            BlockState ground = level.getBlockState(BlockPos.containing(e.at.x, e.at.y - 0.5, e.at.z));
            if (!ground.isAir()) level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), e.at.x, e.at.y + 0.2, e.at.z, 24, e.radius * 0.4, 0.1, e.radius * 0.4, 0.2);
            BossFx.spikes(level, this.random, e.at, e.radius * 0.8, 26, 2.8, e.color, 0xFFB0B0);
            // on water: a geyser, a column of spray and bubbles
            if (!level.getFluidState(BlockPos.containing(e.at.x, e.at.y - 0.5, e.at.z)).isEmpty()) {
                // a tall white column of spray shooting up
                BossFx.spikes(level, this.random, e.at, e.radius * 0.5, 40, 5.5, 0xE8FFFF, 0xFFFFFF);
                level.sendParticles(ParticleTypes.SPLASH, e.at.x, e.at.y + 1.5, e.at.z, 80, e.radius * 0.4, 1.5, e.radius * 0.4, 0.3);
                level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, e.at.x, e.at.y + 0.5, e.at.z, 30, e.radius * 0.4, 0.5, e.radius * 0.4, 0.2);
                level.playSound(null, e.at.x, e.at.y, e.at.z, SoundEvents.GENERIC_SPLASH, SoundSource.HOSTILE, 1.6F, 0.6F);
            }
            level.playSound(null, e.at.x, e.at.y, e.at.z, SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.HOSTILE, 1.2F, 0.6F);
            this.hurtAround(level, e.at, e.radius, e.damage, 0.15, 0.85, null);
        }
    }

    // ── hurting what's round it ──

    // everyone (not its thralls, not bosses, not creative players) within range of the spot: hurt, thrown back and up
    protected List<LivingEntity> hurtAround(ServerLevel level, Vec3 at, double range, float damage, double push, double up,
                                            Consumer<LivingEntity> also) {
        List<LivingEntity> hit = level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(range + 1, 2.5, range + 1),
            e -> this.isFoe(e) && e.position().distanceTo(at) <= range + e.getBbWidth() * 0.5);
        for (LivingEntity e : hit) {
            if (e.hurtServer(level, this.damageSources().mobAttack(this), damage) && e instanceof Player) this.reached();
            Vec3 away = e.position().subtract(at).multiply(1, 0, 1);
            if (away.lengthSqr() < 0.01) away = new Vec3(this.random.nextDouble() - 0.5, 0, this.random.nextDouble() - 0.5);
            away = away.normalize().scale(push);
            e.push(away.x, up, away.z);
            e.needsSync = true;
            if (also != null) also.accept(e);
        }
        return hit;
    }

    protected boolean isFoe(LivingEntity e) {
        if (e == this || !e.isAlive() || e instanceof BloodMoonBoss || e instanceof net.minecraft.world.entity.monster.Enemy || e.entityTags().contains(THRALL_TAG)) return false;
        return !(e instanceof Player p) || !p.isCreative() && !p.isSpectator();
    }

    protected int playersWithin(double range) {
        int n = 0;
        for (Player p : this.level().players()) if (!p.isSpectator() && !p.isCreative() && p.distanceTo(this) <= range) n++;
        return n;
    }

    protected void sound(SoundEvent sound, float volume, float pitch) {
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), sound, SoundSource.HOSTILE, volume, pitch);
    }

    // a jump at the target (lands wherever it lands; the subclass checks onGround)
    protected void jumpAt(LivingEntity target, double maxSpeed, double lift) {
        Vec3 to = target.position().subtract(this.position());
        double flat = Math.max(0.1, Math.sqrt(to.x * to.x + to.z * to.z));
        double speed = Math.min(maxSpeed, flat * 0.11);
        this.setDeltaMovement(to.x / flat * speed, lift + Math.min(0.4, flat * 0.012), to.z / flat * speed);
        this.needsSync = true;
    }

    // ── minions ──

    // half health, then a quarter: time to call its thralls (true once each)
    protected boolean timeToCall() {
        float health = this.getHealth() / this.getMaxHealth();
        if (!this.calledAtHalf && health <= 0.5F || !this.calledAtQuarter && health <= 0.25F) {
            if (health <= 0.25F) this.calledAtQuarter = true;
            this.calledAtHalf = true;
            return true;
        }
        return false;
    }

    // n thralls rise round it (Level II), after its target
    protected void callThralls(ServerLevel level, EntityType<? extends Mob> type, int n, String name, Consumer<Mob> setup) {
        for (int i = 0; i < n; i++) {
            double angle = i * Math.PI * 2 / n + this.random.nextDouble() * 0.4;
            double r = 3.0 + this.random.nextDouble() * 2.0;
            BlockPos at = BlockPos.containing(this.getX() + Math.cos(angle) * r, this.getY() + 1, this.getZ() + Math.sin(angle) * r);
            for (int dy = 0; dy < 4 && !level.getBlockState(at.below()).isSolid(); dy++) at = at.below();
            Mob thrall = type.create(level, EntitySpawnReason.MOB_SUMMONED);
            if (thrall == null) continue;
            thrall.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, this.random.nextFloat() * 360.0F, 0.0F);
            // (gives a skeleton its bow)
            thrall.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.MOB_SUMMONED, null);
            thrall.addTag(THRALL_TAG);
            thrall.setCustomName(Component.literal(name).withStyle(s -> s.withColor(TextColor.fromRgb(0xC04040))));
            level.addFreshEntity(thrall);
            TougherMobs.setLevel(thrall, TougherMobs.Level.TWO);
            if (this.getTarget() != null) thrall.setTarget(this.getTarget());
            if (setup != null) setup.accept(thrall);
            level.sendParticles(ParticleTypes.SOUL, thrall.getX(), thrall.getY() + 0.5, thrall.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
        }
    }

    // a thrall died near its boss: the boss drinks the blood and heals
    public static void onThrallDeath(LivingEntity thrall) {
        if (!(thrall.level() instanceof ServerLevel level) || !thrall.entityTags().contains(THRALL_TAG)) return;
        for (BloodMoonBoss boss : level.getEntitiesOfClass(BloodMoonBoss.class, thrall.getBoundingBox().inflate(24.0))) {
            boss.heal(12.0F);
            Vec3 from = thrall.position().add(0, 1, 0), to = boss.position().add(0, boss.getBbHeight() * 0.6, 0);
            for (int i = 0; i <= 12; i++) {
                Vec3 p = from.lerp(to, i / 12.0);
                level.sendParticles(new DustParticleOptions(0xE01818, 1.4F), p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.0);
            }
            boss.sound(SoundEvents.WARDEN_HEARTBEAT, 1.5F, 1.2F);
        }
    }

    // ── being hit: a burst of blood (and a little shove: its knockback resistance is under 1) ──

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        // the other monsters are on its side: a skeleton's stray arrow, a zombie, its own thralls don't hurt it
        if (source.getEntity() instanceof LivingEntity from && isMonsterSide(from)) return false;
        if (source.getEntity() instanceof LivingEntity attacker && attacker.distanceTo(this) > 12.0) this.hitFromAfar(attacker);
        // arrows and other shots do less (melee is the way to fight it); a player's hit up close counts as reaching it
        if (isRanged(source)) damage *= RANGED_DAMAGE;
        else if (isMelee(source)) this.reached();
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt) this.lastHurt = this.tickCount;
        if (hurt) {
            Vec3 at = source.getEntity() != null ? this.hitPoint(source.getEntity().getEyePosition()) : this.position().add(0, this.getBbHeight() * 0.6, 0);
            level.sendParticles(new DustParticleOptions(0x9A0E0E, 1.5F), at.x, at.y, at.z, 14, 0.25, 0.3, 0.25, 0.0);
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, at.x, at.y, at.z, 3, 0.2, 0.2, 0.2, 0.1);
        }
        return hurt;
    }

    // an arrow, a trident, a snowball... (anything shot or thrown)
    protected static boolean isRanged(DamageSource source) {
        return source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE);
    }

    // a player hitting it with their own hand or weapon
    protected static boolean isMelee(DamageSource source) {
        return source.getEntity() instanceof Player && source.getDirectEntity() == source.getEntity() && !isRanged(source);
    }

    // its own hits count as reaching someone
    @Override
    public boolean doHurtTarget(ServerLevel level, net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof Player) this.reached();
        return hit;
    }

    // the spot on its body facing whoever hit it
    private Vec3 hitPoint(Vec3 from) {
        var box = this.getBoundingBox();
        Vec3 mid = box.getCenter();
        Vec3 toward = from.subtract(mid);
        double y = Math.max(box.minY + 0.3, Math.min(box.maxY - 0.3, from.y - 0.3));
        Vec3 flat = new Vec3(toward.x, 0, toward.z);
        if (flat.lengthSqr() > 0.01) flat = flat.normalize().scale(this.getBbWidth() * 0.5);
        return new Vec3(mid.x + flat.x, y, mid.z + flat.z);
    }

    // hit by someone more than 12 blocks off (an arrow...)
    protected void hitFromAfar(LivingEntity attacker) {
        if (attacker instanceof Player) this.setTarget(attacker);
    }

    // dawn (or it's not tonight's): it sinks into the ground
    public void vanish() {
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 1.5, this.getZ(), 40, 0.8, 1.5, 0.8, 0.02);
            level.sendParticles(new DustParticleOptions(0x801010, 2.0F), this.getX(), this.getY() + 1, this.getZ(), 40, 0.8, 1.5, 0.8, 0.0);
            this.sound(SoundEvents.WARDEN_DIG, 2.0F, 0.6F);
        }
        this.bossBar.removeAllPlayers();
        this.discard();
    }

    // ── what it drops: 15 gold, 20 Blood Shards and its piece of the Blood Moon Crest ──

    // its third of the Blood Moon Crest (ModItems: Revenant upper left, Sanguine upper right, Gorefang the bottom)
    protected abstract net.minecraft.world.level.ItemLike crestPiece();

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        for (ItemStack gold : GoldMoney.toStacks(15L * GoldMoney.INGOT)) this.spawnAtLocation(level, gold);
        this.spawnAtLocation(level, new ItemStack(ModItems.BLOOD_SHARD.get(), 20));
        // and its third of the Blood Moon Crest
        this.spawnAtLocation(level, new ItemStack(this.crestPiece()));
        level.sendParticles(new DustParticleOptions(0xFF2020, 3.0F), this.getX(), this.getY() + 2, this.getZ(), 120, 1.2, 2.0, 1.2, 0.0);
        level.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 2, this.getZ(), 40, 1.0, 1.5, 1.0, 0.05);
    }

    // ── what it ignores ──

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
        return false;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean canUsePortal(boolean ignorePassenger) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossBar.removePlayer(player);
    }

    @Override
    public void remove(RemovalReason reason) {
        this.bossBar.removeAllPlayers();
        super.remove(reason);
    }

    @Override
    protected float getSoundVolume() {
        return 2.0F;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("called_half", this.calledAtHalf);
        output.putBoolean("called_quarter", this.calledAtQuarter);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.calledAtHalf = input.getBooleanOr("called_half", false);
        this.calledAtQuarter = input.getBooleanOr("called_quarter", false);
    }
}
