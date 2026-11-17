package com.alexh.capitalism.town;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// The Fisherman (Town.md > The Fisherman, Fishing.md): an old salt in a bright yellow oilskin coat and sou'wester, a
// bushy grey beard, a pipe in his mouth and dark green waders to his chest (client/FishermanModel). An admin puts him on
// the docks (/npc fisherman: exactly where they stand, facing their way) and he stays on that spot (home), day and night.
// Right-click him (FishermanMenu): he buys rare catches and Soggy Notes (fishing/FishMarket) and mounts a catch on a wall
// plaque for gold. A puff of pipe smoke now and then. He can't be hurt or pushed and never despawns.
public class Fisherman extends PathfinderMob {

    // his name's colour: sea blue
    public static final int COLOR = 0x2FA8B8;

    // where he stands (exactly) and which way he faces
    private @Nullable Vec3 home;
    private float homeYaw;
    // who he's talking to: he faces them until they close his menu
    private @Nullable Player customer;

    public Fisherman(EntityType<? extends Fisherman> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setCustomName(Component.literal("The Fisherman").withStyle(s -> s.withColor(TextColor.fromRgb(COLOR))));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.0).add(Attributes.MAX_HEALTH, 20.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F, 0.05F));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    public @Nullable Player customer() {
        return this.customer;
    }

    public void setCustomer(@Nullable Player customer) {
        this.customer = customer;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.pipeSmoke();
            return;
        }
        if (this.home == null) {
            this.home = this.position();
            this.homeYaw = this.getYRot();
        }
        if (this.position().distanceToSqr(this.home) > 0.04) this.teleportTo(this.home.x, this.home.y, this.home.z);
        // his body keeps facing the way he was put; his head follows you
        this.setYRot(this.homeYaw);
        this.yBodyRot = this.homeYaw;
        if (this.customer != null) {
            boolean talking = this.customer.isAlive() && this.distanceToSqr(this.customer) < 8 * 8
                && this.customer.containerMenu instanceof FishermanMenu menu && menu.fisherman() == this;
            if (talking) this.getLookControl().setLookAt(this.customer, 30.0F, 30.0F);
            else this.customer = null;
        }
    }

    // (client) a little puff of smoke from the bowl of his pipe every few seconds, drifting up
    private void pipeSmoke() {
        if (this.random.nextInt(50) != 0) return;
        // the pipe hangs off the right side of his mouth, its bowl a little forward of his chin
        float yaw = this.yHeadRot * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(yaw), fz = Mth.cos(yaw);
        double sx = -fz, sz = fx;
        double x = this.getX() + fx * 0.42 - sx * 0.12;
        double z = this.getZ() + fz * 0.42 - sz * 0.12;
        double y = this.getEyeY() - 0.12;
        int puffs = 1 + this.random.nextInt(2);
        for (int i = 0; i < puffs; i++) {
            this.level().addParticle(ParticleTypes.SMOKE, x, y + i * 0.06, z, 0.0, 0.025, 0.0);
        }
    }

    // (/npc fisherman) stand exactly here, facing this way
    public void placeAt(Vec3 at, float yaw) {
        this.home = at;
        this.homeYaw = yaw;
        this.snapTo(at.x, at.y, at.z, yaw, 0.0F);
        this.setYHeadRot(yaw);
        this.yBodyRot = yaw;
    }

    // (/npc fisherman remove) off he goes, with a splash
    public void leave() {
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SPLASH, this.getX(), this.getY() + 0.4, this.getZ(), 30, 0.4, 0.3, 0.4, 0.1);
            level.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 1.0, this.getZ(), 12, 0.3, 0.6, 0.3, 0.02);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 1.0F, 0.8F);
        }
        this.discard();
    }

    // ── talking to him ──

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer customer) {
            Fisherman self = this;
            this.setCustomer(customer);
            customer.openMenu(new SimpleMenuProvider(
                (id, inventory, who) -> new FishermanMenu(id, inventory, self),
                Component.literal("The Fisherman")));
            this.playSound(SoundEvents.VILLAGER_TRADE, 0.8F, 0.7F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurtServer(level, source, damage);
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    // an old sailor's grumbles, and now and then the creak of his work (a villager fisherman's)
    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.random.nextInt(3) == 0 ? SoundEvents.VILLAGER_WORK_FISHERMAN : SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    public float getVoicePitch() {
        return 0.7F + this.random.nextFloat() * 0.08F;
    }

    @Override
    protected float getSoundVolume() {
        return 0.5F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 400;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (this.home != null) {
            output.putDouble("home_x", this.home.x);
            output.putDouble("home_y", this.home.y);
            output.putDouble("home_z", this.home.z);
            output.putFloat("home_yaw", this.homeYaw);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        double y = input.getDoubleOr("home_y", Double.NaN);
        if (!Double.isNaN(y)) {
            this.home = new Vec3(input.getDoubleOr("home_x", 0), y, input.getDoubleOr("home_z", 0));
            this.homeYaw = input.getFloatOr("home_yaw", 0.0F);
        }
    }
}
