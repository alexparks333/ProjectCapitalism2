package com.alexh.capitalism.pets;

import com.alexh.capitalism.core.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.jspecify.annotations.Nullable;

// The Animal Bjorn (PetTrainer.md > Animal Bjorn; Pet Trainers make it, anyone can use it): a baby carrier for an animal.
// Right-click an animal with an empty one and it climbs in (it's taken out of the world, everything about it kept in
// CARRIED_ANIMAL); right-click the ground with a full one and it climbs out there. Wear it in the Back slot
// (back/BackSlot) and the animal rides on your chest for everyone to see (client/BjornLayer); with it on your back, sneak
// + right-click an animal with an empty hand to put it in, or sneak + right-click the ground to let it out (back/Back).
// Not someone else's animal, and not big ones (wider than a Polar Bear). Pet Trainers can't put animals in one (they'd
// scoop up wild animals and carry them back to the shop); they can make and sell them, and let an animal out. A pet still gets hungry and thirsty while it's
// carried: when it comes out it's caught up on the time it was in while its carrier was online (Pets.timePassed).
public class AnimalBjornItem extends Item {

    public static final float MAX_WIDTH = 1.4F;

    public AnimalBjornItem(Properties properties) {
        super(properties);
    }

    public static @Nullable CarriedAnimal carried(ItemStack stack) {
        return stack.get(ModComponents.CARRIED_ANIMAL.get());
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Animal animal)) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer)) return InteractionResult.SUCCESS;
        pickUp(stack, player, animal);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (carried(context.getItemInHand()) == null) return InteractionResult.PASS;
        if (!(context.getLevel() instanceof ServerLevel level) || context.getPlayer() == null) return InteractionResult.SUCCESS;
        letOut(context.getItemInHand(), context.getPlayer(), level, context.getClickedPos().relative(context.getClickedFace()));
        return InteractionResult.SUCCESS;
    }

    // Put this animal in the bjorn (true if it went in; the player is told why not)
    public static boolean pickUp(ItemStack bjorn, Player player, Animal animal) {
        String problem = null;
        if (!player.isCreative() && com.alexh.capitalism.occupation.JobEvents.has(player, com.alexh.capitalism.occupation.Job.PET_TRAINER)) {
            // they'd just scoop up wild animals and carry them back to the shop
            problem = "Pet Trainers can't carry animals in an Animal Bjorn.";
        } else if (carried(bjorn) != null) problem = "The Animal Bjorn already has an animal in it.";
        else if (Pets.claimed(animal) && !Pets.isOwner(animal, player)) problem = "This animal belongs to " + Pets.data(animal).ownerName() + ".";
        // only your own pet: no carrying off wild or unclaimed animals
        else if (!player.isCreative() && !Pets.isOwner(animal, player)) problem = "You can only carry your own pet in an Animal Bjorn.";
        else if (animal.getBbWidth() > MAX_WIDTH || !Pets.trainable(animal)) problem = "That animal is too big for the Animal Bjorn.";
        else if (animal.isVehicle() || animal.isPassenger()) problem = "It can't climb in right now.";
        if (problem != null) {
            if (player instanceof ServerPlayer serverPlayer) serverPlayer.sendOverlayMessage(Component.literal(problem).withStyle(ChatFormatting.RED));
            return false;
        }
        if (animal.isLeashed()) animal.dropLeash();
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, animal.registryAccess());
        animal.saveWithoutId(out);
        bjorn.set(ModComponents.CARRIED_ANIMAL.get(), new CarriedAnimal(BuiltInRegistries.ENTITY_TYPE.getKey(animal.getType()), out.buildResult(),
            Pets.name(animal).getString(), player.level().getGameTime(), Pets.playedTicks(player)));
        animal.discard();
        player.level().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 1.0F, 1.2F);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendOverlayMessage(Component.literal(Pets.name(animal).getString() + " climbed into the Animal Bjorn.").withStyle(ChatFormatting.GREEN));
        }
        return true;
    }

    // Let the animal out at this spot (true if it came out)
    public static boolean letOut(ItemStack bjorn, Player player, ServerLevel level, BlockPos at) {
        CarriedAnimal carried = carried(bjorn);
        if (carried == null) return false;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(carried.type());
        Entity entity = EntityType.create(type, TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), carried.data()), level,
            EntitySpawnReason.BUCKET).orElse(null);
        if (entity == null) return false;
        entity.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, player.getYRot() + 180, 0);
        entity.setDeltaMovement(0, 0, 0);
        entity.resetFallDistance();
        // (a bjorn filled before this was kept has no time: nothing passed)
        // only time its carrier was online counts (their played time; never more than the time that really passed)
        if (entity instanceof Animal pet && Pets.isPet(pet) && carried.since() != 0) {
            long ticks = Math.min(level.getGameTime() - carried.since(), Pets.playedTicks(player) - carried.played());
            Pets.timePassed(pet, Math.max(0, ticks) / 20);
        }
        if (!level.addFreshEntity(entity)) return false;
        bjorn.remove(ModComponents.CARRIED_ANIMAL.get());
        level.playSound(null, at, SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 1.0F, 0.9F);
        return true;
    }

    @Override
    public Component getName(ItemStack stack) {
        CarriedAnimal carried = carried(stack);
        return carried == null ? super.getName(stack) : Component.literal("Animal Bjorn (" + carried.name() + ")");
    }
}
