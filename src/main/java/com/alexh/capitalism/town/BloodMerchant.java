package com.alexh.capitalism.town;

import com.alexh.capitalism.calendar.Calendar;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import org.jspecify.annotations.Nullable;

// The Blood Merchant (Town.md > The Blood Merchant): a tall, gaunt vampire in a black hooded cloak lined with crimson,
// a high collar, pale skin, burning red eyes and fangs (client/BloodMerchantModel). An admin puts him in his shop
// (/npc bloodmerchant: exactly where they stand, facing their way) and he stays on that spot (home). He only comes out at NIGHT (dusk to dawn): by
// day he's gone (invisible, can't be touched or talked to); at dusk he appears in a swirl of smoke and bats, at dawn he
// leaves the same way. Right-click him (BloodMerchantMenu): his curios (decorations, and the Essential Wishing Star for
// a Blood Moon Crest) and mending the crest. He can't be hurt or pushed and never despawns.
public class BloodMerchant extends PathfinderMob {

    // night: from dusk to dawn (the calendar's time of day)
    static final int DUSK = 13000;
    static final int DAWN = 23000;

    // where he stands (exactly) and which way he faces
    private net.minecraft.world.phys.@Nullable Vec3 home;
    private float homeYaw;
    // who he's talking to: he faces them until they close his menu
    private @Nullable Player customer;

    public BloodMerchant(EntityType<? extends BloodMerchant> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setCustomName(Component.literal("The Blood Merchant").withStyle(s -> s.withColor(TextColor.fromRgb(0xC01830))));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.0).add(Attributes.MAX_HEALTH, 20.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F, 0.05F));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    public static boolean isNight(Level level) {
        long time = Math.floorMod(Calendar.clock(level), Calendar.DAY_TICKS);
        return time >= DUSK && time < DAWN;
    }

    // out (night) or gone (day)
    public boolean out() {
        return !this.isInvisible();
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
            if (this.out() && this.random.nextInt(4) == 0) {
                // a faint red mist curling off his cloak, smoke at his feet
                this.level().addParticle(new DustParticleOptions(0x6A0814, 0.9F), this.getRandomX(0.7), this.getY() + this.random.nextDouble() * 1.6,
                    this.getRandomZ(0.7), 0, 0.01, 0);
                if (this.random.nextInt(3) == 0) this.level().addParticle(ParticleTypes.SMOKE, this.getRandomX(0.6), this.getY() + 0.05, this.getRandomZ(0.6), 0, 0.01, 0);
            }
            return;
        }
        ServerLevel level = (ServerLevel) this.level();
        if (this.home == null) {
            this.home = this.position();
            this.homeYaw = this.getYRot();
        }
        if (this.position().distanceToSqr(this.home) > 0.04) this.teleportTo(this.home.x, this.home.y, this.home.z);
        // his body keeps facing the way he was put; his head follows you
        this.setYRot(this.homeYaw);
        this.yBodyRot = this.homeYaw;
        // night: out; day: gone
        if (this.tickCount % 20 == 0) {
            boolean night = isNight(level);
            if (night != this.out()) {
                this.swirl(level);
                this.setInvisible(!night);
                this.playSound(night ? SoundEvents.EVOKER_PREPARE_SUMMON : SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0F, 0.6F);
                if (!night && this.customer instanceof ServerPlayer p) p.closeContainer();
            }
        }
        if (this.customer != null) {
            boolean talking = this.customer.isAlive() && this.distanceToSqr(this.customer) < 8 * 8
                && this.customer.containerMenu instanceof BloodMerchantMenu menu && menu.merchant() == this;
            if (talking) this.getLookControl().setLookAt(this.customer, 30.0F, 30.0F);
            else this.customer = null;
        }
    }

    // (/npc bloodmerchant) stand exactly here, facing this way
    public void placeAt(net.minecraft.world.phys.Vec3 at, float yaw) {
        this.home = at;
        this.homeYaw = yaw;
        this.snapTo(at.x, at.y, at.z, yaw, 0.0F);
        this.setYHeadRot(yaw);
        this.yBodyRot = yaw;
    }

    // (/npc bloodmerchant remove) gone in a swirl
    public void leave() {
        if (this.level() instanceof ServerLevel level) this.swirl(level);
        this.discard();
    }

    // a swirl of smoke, red mist and bats' wings as he comes or goes
    private void swirl(ServerLevel level) {
        level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 1.0, this.getZ(), 30, 0.4, 0.8, 0.4, 0.04);
        level.sendParticles(new DustParticleOptions(0x8A0A1A, 1.6F), this.getX(), this.getY() + 1.0, this.getZ(), 40, 0.5, 0.9, 0.5, 0.0);
        level.sendParticles(ParticleTypes.SQUID_INK, this.getX(), this.getY() + 1.4, this.getZ(), 16, 0.4, 0.6, 0.4, 0.08);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BAT_TAKEOFF, SoundSource.NEUTRAL, 1.0F, 0.7F);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BAT_AMBIENT, SoundSource.NEUTRAL, 0.8F, 0.6F);
    }

    // ── talking to him ──

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.out()) return InteractionResult.PASS;
        if (hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer buyer) {
            BloodMerchant self = this;
            this.setCustomer(buyer);
            buyer.openMenu(new SimpleMenuProvider(
                (id, inventory, who) -> new BloodMerchantMenu(id, inventory, BloodMerchantMenu.liveData(buyer), self),
                Component.literal("The Blood Merchant")));
            this.playSound(SoundEvents.EVOKER_AMBIENT, 0.7F, 0.5F);
        }
        return InteractionResult.SUCCESS;
    }

    // by day he isn't there to touch
    @Override
    public boolean isPickable() {
        return this.out() && super.isPickable();
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

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.out() ? SoundEvents.EVOKER_AMBIENT : null;
    }

    @Override
    public float getVoicePitch() {
        return 0.5F;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 480;
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
            this.home = new net.minecraft.world.phys.Vec3(input.getDoubleOr("home_x", 0), y, input.getDoubleOr("home_z", 0));
            this.homeYaw = input.getFloatOr("home_yaw", 0.0F);
        }
    }
}
