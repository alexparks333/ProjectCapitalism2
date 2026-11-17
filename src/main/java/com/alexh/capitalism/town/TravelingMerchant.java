package com.alexh.capitalism.town;

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

// The Traveling Merchant (Town.md > The Traveling Merchant): a weary old traveller, stooped, with a long grey beard, a
// battered wide-brimmed hat, a patched coat and a bedroll on his back, leaning on a staff with a lantern hanging from it,
// his big covered wagon (canvas bonnet, spoked wheels, crates and barrels lashed on) beside him (client/
// TravelingMerchantModel draws both). An admin puts him in town with /npc travelingmerchant (exactly where they stand,
// facing their way, the wagon to his left) and he never moves. He's only THERE on his visit days (TravelingMerchantShop:
// one day every 8-14): the rest of the time he and his wagon are gone (invisible, can't be touched). Right-click him for
// his wares (TravelingMerchantMenu). Can't be hurt or pushed, never despawns.
public class TravelingMerchant extends PathfinderMob {

    private @Nullable Vec3 home;
    private float homeYaw;
    private @Nullable Player customer;

    public TravelingMerchant(EntityType<? extends TravelingMerchant> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setCustomName(Component.literal("The Traveling Merchant").withStyle(s -> s.withColor(TextColor.fromRgb(0xE0B060))));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.0).add(Attributes.MAX_HEALTH, 20.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F, 0.05F));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    // in town (visit day) or away
    public boolean here() {
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
        // only his head turns: his body (and the wagon, drawn with it) always faces the way he was put
        this.yBodyRot = this.getYRot();
        this.yBodyRotO = this.getYRot();
        if (this.level().isClientSide()) {
            // a curl of pipe smoke now and then
            if (this.here() && this.random.nextInt(30) == 0) {
                Vec3 look = Vec3.directionFromRotation(0, this.yBodyRot);
                this.level().addParticle(ParticleTypes.SMOKE, this.getX() + look.x * 0.35, this.getY() + 1.55, this.getZ() + look.z * 0.35, 0, 0.02, 0);
            }
            return;
        }
        ServerLevel level = (ServerLevel) this.level();
        if (this.home == null) {
            this.home = this.position();
            this.homeYaw = this.getYRot();
        }
        if (this.position().distanceToSqr(this.home) > 0.04) this.teleportTo(this.home.x, this.home.y, this.home.z);
        this.setYRot(this.homeYaw);
        this.yBodyRot = this.homeYaw;
        if (this.tickCount % 20 == 0) {
            boolean visiting = TravelingMerchantShop.inTown(level.getServer());
            if (visiting != this.here()) {
                this.dust(level);
                this.setInvisible(!visiting);
                if (visiting) {
                    level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.HORSE_AMBIENT, SoundSource.NEUTRAL, 1.0F, 0.8F);
                    level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.CHAIN_PLACE, SoundSource.NEUTRAL, 0.8F, 0.7F);
                } else if (this.customer instanceof ServerPlayer p) {
                    p.closeContainer();
                }
            }
        }
        if (this.customer != null) {
            boolean talking = this.customer.isAlive() && this.distanceToSqr(this.customer) < 8 * 8
                && this.customer.containerMenu instanceof TravelingMerchantMenu menu && menu.merchant() == this;
            if (talking) this.getLookControl().setLookAt(this.customer, 30.0F, 30.0F);
            else this.customer = null;
        }
    }

    // (/npc travelingmerchant) stand exactly here, facing this way
    public void placeAt(Vec3 at, float yaw) {
        this.home = at;
        this.homeYaw = yaw;
        this.snapTo(at.x, at.y, at.z, yaw, 0.0F);
        this.setYHeadRot(yaw);
        this.yBodyRot = yaw;
    }

    public void leave() {
        if (this.level() instanceof ServerLevel level) this.dust(level);
        this.discard();
    }

    // road dust as the wagon comes or goes
    private void dust(ServerLevel level) {
        level.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.6, this.getZ(), 30, 1.2, 0.5, 1.2, 0.02);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX(), this.getY() + 0.4, this.getZ(), 6, 0.8, 0.2, 0.8, 0.01);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.here()) return InteractionResult.PASS;
        if (hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer buyer) {
            this.setCustomer(buyer);
            TravelingMerchantMenu.open(buyer, this);
            this.playSound(SoundEvents.VINDICATOR_AMBIENT, 0.8F, 0.65F);
        }
        return InteractionResult.SUCCESS;
    }

    // the body never turns to follow the head
    @Override
    protected void tickHeadTurn(float yBodyRot) {
    }

    @Override
    public boolean isPickable() {
        return this.here() && super.isPickable();
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
        // an old man's grumble: a Vindicator's mutter, pitched down (vanilla has no old-man voice)
        return this.here() ? SoundEvents.VINDICATOR_AMBIENT : null;
    }

    @Override
    public float getVoicePitch() {
        return 0.62F + this.random.nextFloat() * 0.06F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 600;
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
