package com.alexh.capitalism.pets;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

// Training Feed (PetTrainer.md > Training Feed): Cooks make it, only Pet Trainers can use it. Right-click an animal on the pet
// list (PetTraits) with it: one feed is used and the animal follows you for a few seconds (pathfinding round things in
// the way, ahead of its own wandering), to help bring it home.
// Not someone else's animal (claimed, or a tamed wolf / cat...). Nothing is saved: a follow is only a few seconds.
@EventBusSubscriber(modid = Capitalism.MODID)
public final class TrainingFeed {

    public static final int FOLLOW_TICKS = 10 * 20;
    // how fast it walks after you, and how close it comes
    private static final double SPEED = 1.25;
    private static final double CLOSE_ENOUGH = 2.5;
    private static final double TOO_FAR = 24.0;

    private record Follow(UUID player, long until) {
    }

    private static final Map<Animal, Follow> FOLLOWING = new WeakHashMap<>();

    private TrainingFeed() {
    }

    // Can this animal be fed Training Feed by this player?
    public static boolean feedable(Entity target, Player player) {
        if (!(target instanceof Animal animal) || !PetTraits.listed().contains(animal.getType())) return false;
        if (!Pets.mayUse(animal, player)) return false;
        return !(animal instanceof TamableAnimal tame && tame.isTame() && !tame.isOwnedBy(player));
    }

    public static boolean following(Animal animal, Player player) {
        Follow follow = FOLLOWING.get(animal);
        return follow != null && follow.player().equals(player.getUUID()) && animal.level().getGameTime() < follow.until();
    }

    // Before the pet rules (Pets.onInteract), so a fed pet doesn't count as an ordinary click
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        ItemStack stack = event.getItemStack();
        Player player = event.getEntity();
        if (!stack.is(ModItems.TRAINING_FEED.get()) || !feedable(event.getTarget(), player)) return;
        Animal animal = (Animal) event.getTarget();
        // only Pet Trainers know how to use it (Cooks make it, but can't use it themselves)
        if (!player.isCreative() && !com.alexh.capitalism.occupation.JobEvents.has(player, com.alexh.capitalism.occupation.Job.PET_TRAINER)) {
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                serverPlayer.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Only Pet Trainers can use Training Feed.")
                    .withStyle(net.minecraft.ChatFormatting.RED));
            }
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }
        if (animal.level() instanceof ServerLevel level) {
            if (!player.isCreative()) stack.shrink(1);
            FOLLOWING.put(animal, new Follow(player.getUUID(), level.getGameTime() + FOLLOW_TICKS));
            if (!brainDriven(animal) && animal.goalSelector.getAvailableGoals().stream().noneMatch(g -> g.getGoal() instanceof FollowFeederGoal)) {
                animal.goalSelector.addGoal(0, new FollowFeederGoal(animal));
            }
            animal.playSound(SoundEvents.GENERIC_EAT.value(), 0.8F, 1.2F);
            level.sendParticles(ParticleTypes.HEART, animal.getX(), animal.getY() + animal.getBbHeight(), animal.getZ(), 3, 0.3, 0.2, 0.3, 0);
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    // Who a fed animal is following right now (null: nobody, or the time is up / they're too far away)
    private static @Nullable Player feeder(Animal animal) {
        Follow follow = FOLLOWING.get(animal);
        if (follow == null) return null;
        Player player = animal.level().getPlayerByUUID(follow.player());
        if (player == null || !player.isAlive() || animal.level().getGameTime() >= follow.until() || animal.distanceTo(player) > TOO_FAR || !animal.isAlive()) {
            FOLLOWING.remove(animal);
            return null;
        }
        return player;
    }

    // Newer animals (goats, axolotls, frogs, camels, sniffers, armadillos, hoglins...) are run by a "brain" that walks
    // wherever its WALK_TARGET memory says, not by goals
    private static boolean brainDriven(Animal animal) {
        return animal.getBrain().checkMemory(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED);
    }

    // Brain animals: every tick, their walk target is the feeder (their own wandering only picks a spot when there's
    // none), and they look at them. Pathfinding takes them round whatever is in the way. (Goal animals:
    // FollowFeederGoal.)
    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (FOLLOWING.isEmpty() || !(event.getEntity() instanceof Animal animal) || animal.level().isClientSide()) return;
        if (!FOLLOWING.containsKey(animal)) return;
        Player player = feeder(animal);
        if (!brainDriven(animal)) return;
        if (player == null) {
            animal.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            return;
        }
        animal.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(player, true));
        if (animal.distanceTo(player) > CLOSE_ENOUGH) {
            animal.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(player, (float) SPEED, (int) CLOSE_ENOUGH - 1));
        } else {
            animal.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        }
    }

    // Goal animals (pigs, cows, wolves, foxes, pandas...): the first thing they do while fed, ahead of wandering,
    // panicking or anything else that moves them. Like a tamed wolf following its owner: a new path to the feeder every
    // half second (round whatever is in the way), stopping close by.
    private static final class FollowFeederGoal extends Goal {
        private final Animal animal;
        private int repath;

        FollowFeederGoal(Animal animal) {
            this.animal = animal;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return FOLLOWING.containsKey(this.animal) && feeder(this.animal) != null;
        }

        @Override
        public void start() {
            this.repath = 0;
        }

        @Override
        public void stop() {
            this.animal.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Player player = feeder(this.animal);
            if (player == null) return;
            this.animal.getLookControl().setLookAt(player, 30.0F, this.animal.getMaxHeadXRot());
            if (this.animal.distanceTo(player) <= CLOSE_ENOUGH) {
                this.animal.getNavigation().stop();
                return;
            }
            if (--this.repath <= 0 || this.animal.getNavigation().isDone()) {
                this.repath = 10;
                this.animal.getNavigation().moveTo(player, SPEED);
            }
        }
    }
}
