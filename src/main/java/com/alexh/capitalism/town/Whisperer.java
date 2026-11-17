package com.alexh.capitalism.town;

import com.alexh.capitalism.core.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

// The Whisperer (Town.md): a hooded, hunchbacked old figure with a cane (client/WhispererModel) who hobbles around the
// town's streets (Town.walkArea), mostly along the Stone Brick paths. Right-click him to buy Knowledge Pieces
// (WhispererMenu: a piece of advice, or Knowledge Pieces from the shared WhispererStock); while your menu is open he
// stands still and looks at you. He can't be hurt and never despawns; WhispererManager keeps exactly one
// of him in town. If he ever ends up outside the town, he's put back on a path.
public class Whisperer extends PathfinderMob {

    // who he's talking to (their Whisperer menu is open): he stands still and looks at them until they close it
    private @Nullable Player customer;

    public Whisperer(EntityType<? extends Whisperer> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setCustomName(Component.literal("The Whisperer").withStyle(ChatFormatting.DARK_PURPLE));
    }

    public @Nullable Player customer() {
        return this.customer;
    }

    public void setCustomer(@Nullable Player customer) {
        this.customer = customer;
        if (customer != null) this.getNavigation().stop();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.2).add(Attributes.MAX_HEALTH, 20.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 5.0F, 0.04F));
        this.goalSelector.addGoal(2, new StrollTheStreets(this));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    // ── Can't be hurt or pushed around ──

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

    // ── Talking to him ──

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer serverPlayer) {
            Whisperer self = this;
            this.setCustomer(serverPlayer);
            serverPlayer.openMenu(new SimpleMenuProvider(
                (id, inventory, who) -> new WhispererMenu(id, inventory, WhispererMenu.liveData(serverPlayer), self),
                Component.literal("The Whisperer")));
            this.playSound(SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, 0.4F, 0.5F);
        }
        return InteractionResult.SUCCESS;
    }

    // A low murmur now and then
    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;
    }

    @Override
    public float getVoicePitch() {
        return 0.45F + this.random.nextFloat() * 0.1F;
    }

    @Override
    protected float getSoundVolume() {
        return 0.35F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 400;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            if (this.random.nextInt(12) == 0) {
                this.level().addParticle(ParticleTypes.ENCHANT, this.getRandomX(0.6), this.getY() + 1.2 + this.random.nextDouble() * 0.4,
                    this.getRandomZ(0.6), 0, 0.2, 0);
            }
            return;
        }
        // talking to someone: stand still and face them, until they close his menu (or wander off / log out)
        if (this.customer != null) {
            boolean stillTalking = this.customer.isAlive() && !this.customer.isRemoved() && this.distanceToSqr(this.customer) < 8 * 8
                && this.customer.containerMenu instanceof WhispererMenu menu && menu.whisperer() == this;
            if (stillTalking) {
                this.getNavigation().stop();
                this.getLookControl().setLookAt(this.customer, 30.0F, 30.0F);
                return;
            }
            this.customer = null;
        }
        // strayed out of town (pushed, fell...): back onto a path
        if (this.tickCount % 40 == 0 && !Town.walkArea().contains(this.blockPosition())) {
            BlockPos spot = pathSpot((ServerLevel) this.level(), this.random, Town.walkArea().center(this.getBlockY()), 40);
            if (spot != null) this.teleportTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
        }
    }

    // ── Where he walks ──

    // Is this a street? Stone Bricks (and their mossy / cracked / chiseled kinds, slabs and stairs)
    static boolean isStreet(BlockState state) {
        return state.is(BlockTags.STONE_BRICKS) || state.is(Blocks.STONE_BRICK_SLAB) || state.is(Blocks.STONE_BRICK_STAIRS)
            || state.is(Blocks.MOSSY_STONE_BRICK_SLAB) || state.is(Blocks.MOSSY_STONE_BRICK_STAIRS);
    }

    // A spot on the ground near `around` (within `range`) inside the town: a Stone Brick street if one turns up,
    // otherwise any solid ground. Null if nothing's loaded there.
    static @Nullable BlockPos pathSpot(ServerLevel level, RandomSource random, BlockPos around, int range) {
        Town.Area area = Town.walkArea();
        BlockPos fallback = null;
        for (int i = 0; i < 30; i++) {
            int x = Math.clamp(around.getX() + random.nextInt(range * 2 + 1) - range, area.minX(), area.maxX());
            int z = Math.clamp(around.getZ() + random.nextInt(range * 2 + 1) - range, area.minZ(), area.maxZ());
            if (!level.isLoaded(new BlockPos(x, 0, z))) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos feet = new BlockPos(x, y, z);
            BlockState ground = level.getBlockState(feet.below());
            if (isStreet(ground)) return feet;
            if (fallback == null && ground.isFaceSturdy(level, feet.below(), net.minecraft.core.Direction.UP) && level.getFluidState(feet.below()).isEmpty()) fallback = feet;
        }
        return fallback;
    }

    // Hobble somewhere nearby in town every few seconds, nearly always along a Stone Brick street
    private static final class StrollTheStreets extends Goal {
        private final Whisperer whisperer;

        StrollTheStreets(Whisperer whisperer) {
            this.whisperer = whisperer;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.whisperer.customer != null || !this.whisperer.getNavigation().isDone() || this.whisperer.random.nextInt(80) != 0) return false;
            if (!(this.whisperer.level() instanceof ServerLevel level)) return false;
            BlockPos spot = pathSpot(level, this.whisperer.random, this.whisperer.blockPosition(), 12);
            // off the streets only now and then
            if (spot == null || (!isStreet(level.getBlockState(spot.below())) && this.whisperer.random.nextInt(5) != 0)) return false;
            return this.whisperer.getNavigation().moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 0.8);
        }

        @Override
        public boolean canContinueToUse() {
            return this.whisperer.customer == null && !this.whisperer.getNavigation().isDone();
        }
    }

    // Used by WhispererManager
    static Whisperer create(ServerLevel level, BlockPos at) {
        Whisperer whisperer = new Whisperer(ModEntities.WHISPERER.get(), level);
        whisperer.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360, 0);
        return whisperer;
    }
}
