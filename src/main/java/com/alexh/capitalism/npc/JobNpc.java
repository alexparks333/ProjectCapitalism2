package com.alexh.capitalism.npc;

import com.alexh.capitalism.occupation.Job;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

// A job's stand-in shopkeeper (see NPCs.md, NpcManager): looks like a villager in that job's outfit, holding something
// from the job (a sword for the Weaponsmith...), with the job's name over its head. Stands still at its spot and watches
// players; completely silent; can't be killed, hurt, pushed or moved by anything; never despawns.
// NpcManager spawns and removes it; it also removes itself if its spot is gone or another NPC took its place.
public class JobNpc extends PathfinderMob {

    private static final EntityDataAccessor<String> JOB = SynchedEntityData.defineId(JobNpc.class, EntityDataSerializers.STRING);

    public JobNpc(EntityType<? extends JobNpc> type, Level level) {
        super(type, level);
        this.setSilent(true);
        this.setPermanentlyInvulnerable(true);
        this.setPersistenceRequired();
        // floats if the floor under it is removed: it can never fall into lava or the void
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.0).add(Attributes.MAX_HEALTH, 20.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(JOB, "");
    }

    public @Nullable Job job() {
        return Job.byId(this.entityData.get(JOB));
    }

    // Who this NPC stands in for: its name, and what it holds
    public void setJob(Job job) {
        this.entityData.set(JOB, job.id());
        this.setCustomName(Component.literal(job.title).withStyle(ChatFormatting.GOLD));
        this.setCustomNameVisible(true);
        this.setItemSlot(EquipmentSlot.MAINHAND, heldItem(job));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    private static ItemStack heldItem(Job job) {
        return new ItemStack(switch (job) {
            case COOK -> Items.MUSHROOM_STEW;
            case BARTENDER -> Items.POTION;
            case WEAPONSMITH -> Items.IRON_SWORD;
            case TOOLSMITH -> Items.IRON_PICKAXE;
            case ARMORSMITH -> Items.SHIELD;
            case ENCHANTER -> Items.ENCHANTED_BOOK;
            case LIBRARIAN -> Items.BOOK;
            case PET_TRAINER -> Items.LEAD;
            case DESIGNER -> Items.LEATHER;
            case FARMER -> Items.WHEAT;
            case REAL_ESTATE_AGENT -> Items.MAP;
        });
    }

    // Right-click: its shop (NpcShops), or, for a job without stock yet, a word about it
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        Job job = this.job();
        if (job != null && player instanceof ServerPlayer serverPlayer && hand == InteractionHand.MAIN_HAND) {
            // the Enchanter: an Enchanting Table you pay in gold (enchanter/NpcEnchanting)
            if (job == Job.ENCHANTER) {
                com.alexh.capitalism.enchanter.NpcEnchanting.open(serverPlayer, this);
                return InteractionResult.SUCCESS;
            }
            if (!NpcShops.sections(job).isEmpty()) {
                JobNpc self = this;
                serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, who) -> new NpcShopMenu(id, inventory, job.id(), self),
                    Component.literal(job.title + "'s Shop")), buf -> buf.writeUtf(job.id()));
                return InteractionResult.SUCCESS;
            }
            serverPlayer.sendSystemMessage(Component.literal("[" + job.title + "] ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal("I'm filling in while no " + job.title + " is around. My shop opens soon!")
                    .withStyle(ChatFormatting.WHITE)));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void tick() {
        super.tick();
        // Every 30 seconds: am I still the NPC for my job's spot? If not, go (NpcManager spawns the right one)
        if (this.level() instanceof ServerLevel && this.tickCount % 600 == 0 && !this.stillWanted()) {
            this.leave();
        }
    }

    // Should it still be standing here? (its job's NPC: NpcManager says; the Boat Master overrides this)
    public boolean stillWanted() {
        return NpcManager.isCurrent(this);
    }

    // Its skin: textures/entity/npc/<this>.png (client/JobNpcRenderer); "" for the plain villager
    public String skin() {
        Job job = this.job();
        return job == null ? "" : job.id();
    }

    // Vanishes in a little puff of smoke (silently, like everything it does)
    public void leave() {
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 1.0, this.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
        }
        this.discard();
    }

    // ── Can't be killed, hurt or moved, by anything (Creative players and /kill included). The only ways it goes:
    // /npc removespot, or NpcManager taking it away when a player with its job is online. ──

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    public void kill(ServerLevel level) {
    }

    // Can't be hit at all (no swing, no knockback), and mobs never go after it
    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean canBeSeenAsEnemy() {
        return false;
    }

    // No potions (Harming, Poison, Wither...), no fire, no explosions
    @Override
    public boolean isAffectedByPotions() {
        return false;
    }

    @Override
    public void setRemainingFireTicks(int ticks) {
        super.setRemainingFireTicks(0);
    }

    @Override
    public boolean ignoreExplosion(Explosion explosion) {
        return true;
    }

    // Can't be leashed or put in a boat / minecart to drag it away
    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean startRiding(net.minecraft.world.entity.Entity vehicle, boolean force, boolean sendEventAndTriggers) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entity) {
    }

    @Override
    public boolean removeWhenFarAway(double distanceSqr) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("job", this.entityData.get(JOB));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        Job job = Job.byId(input.getStringOr("job", ""));
        if (job != null) this.setJob(job);
    }
}
