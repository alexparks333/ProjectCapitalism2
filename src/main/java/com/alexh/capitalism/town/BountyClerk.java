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

// The Bounty Clerk (Town.md > The Bounty Clerk, Bounties.md): a neat town official in a tidy bottle-green frock coat with
// brass buttons, a waistcoat and cravat, round spectacles on his nose, a quill behind his ear, a thick ledger under his
// arm, standing at a notice board pinned with bounty papers (client/BountyClerkModel). An admin puts him in town
// (/npc bountyclerk: exactly where they stand, facing their way) and he stays on that spot (home), day and night.
// Right-click him (BountyMenu): today's bounty task, your open tasks, the ledger. Now and then a page of his ledger
// flutters. He can't be hurt or pushed and never despawns.
public class BountyClerk extends PathfinderMob {

    // his name's colour: ledger gold
    public static final int COLOR = 0xD8B060;

    // where he stands (exactly) and which way he faces
    private @Nullable Vec3 home;
    private float homeYaw;
    // who he's talking to: he faces them until they close his menu
    private @Nullable Player customer;

    public BountyClerk(EntityType<? extends BountyClerk> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setCustomName(Component.literal("The Bounty Clerk").withStyle(s -> s.withColor(TextColor.fromRgb(COLOR))));
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
            this.pageFlutter();
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
                && this.customer.containerMenu instanceof BountyMenu menu && menu.clerk() == this;
            if (talking) this.getLookControl().setLookAt(this.customer, 30.0F, 30.0F);
            else this.customer = null;
        }
    }

    // (client) now and then a page of his ledger flutters (a little white glint by his side)
    private void pageFlutter() {
        if (this.random.nextInt(120) != 0) return;
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(yaw), fz = Mth.cos(yaw);
        double x = this.getX() + fx * 0.3 - fz * 0.35, z = this.getZ() + fz * 0.3 + fx * 0.35;
        this.level().addParticle(ParticleTypes.ENCHANT, x, this.getY() + 1.1, z, 0.0, 0.3, 0.0);
    }
    // (/npc bountyclerk) stand exactly here, facing this way
    public void placeAt(Vec3 at, float yaw) {
        this.home = at;
        this.homeYaw = yaw;
        this.snapTo(at.x, at.y, at.z, yaw, 0.0F);
        this.setYHeadRot(yaw);
        this.yBodyRot = yaw;
    }

    // (/npc bountyclerk remove) off he goes, closing his ledger
    public void leave() {
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.ENCHANT, this.getX(), this.getY() + 1.0, this.getZ(), 30, 0.4, 0.6, 0.4, 0.4);
            level.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 1.0, this.getZ(), 12, 0.3, 0.6, 0.3, 0.02);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BOOK_PUT, SoundSource.NEUTRAL, 1.0F, 0.8F);
        }
        this.discard();
    }

    // ── talking to him ──

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer customer) {
            BountyClerk self = this;
            this.setCustomer(customer);
            com.alexh.capitalism.bounty.Bounties.syncBoard(customer);   // the posted task and the winners, sent before the screen opens
            customer.openMenu(new SimpleMenuProvider(
                (id, inventory, who) -> new BountyMenu(id, inventory, self),
                Component.literal("The Bounty Clerk")));
            this.playSound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F);
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

    // a clerk's murmurs, and now and then the scratch of his quill (a villager cartographer's work)
    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.random.nextInt(3) == 0 ? SoundEvents.VILLAGER_WORK_CARTOGRAPHER : SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    public float getVoicePitch() {
        return 0.95F + this.random.nextFloat() * 0.08F;
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
