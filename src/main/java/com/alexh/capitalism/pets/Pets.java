package com.alexh.capitalism.pets;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.ModAttachments;
import com.alexh.capitalism.core.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jspecify.annotations.Nullable;

// The Pet Trainer's animals (PetTrainer.md), in this order:
//  1. Trained into a pet (the Training Stick, TrainingStickItem; Pet Trainers only, 25 levels): any animal nobody owns
//     (not riding animals: horses, donkeys, mules) gets a name ("Pet Pig"), hunger and thirst, and never
//     despawns. Otherwise it acts just like before.
//  2. Claimed (an Ownership Tag, OwnershipTagItem; only works on a pet): the pet is yours. Other players can't hurt it or
//     do anything with it (lead it, ride it, milk it, name it...). They can still feed and water it.
//  - A pet gets hungry and thirsty over time. Feed it its usual food (what it'd breed with) by hand; give it a Water
//    Bucket or Water Bottle, or let it stand in water. Well fed and watered, it slowly heals; with either bar empty it
//    slowly gets hurt, down to one heart; with both bars empty, or one empty for over 5 minutes, it can starve to death.
//  - A Food Trough or Water Bowl within 5 blocks: a hungry / thirsty pet walks over and eats its own food / drinks
//    till it's full (FoodTroughBlock; WaterBowlBlockEntity: it drinks only the mL it needs).
//  - The owner can pet it (right-click with an empty hand: its happy sound; the first time each Minecraft day they get its
//    buff, PetTraits, and little patting hands float up) and Ctrl + right-click opens its window
//    (client/PetScreen: Status, and Actions like Stay Still, sent back as a PetActionPacket).
// client/PetHud shows a pet's name, owner (or "No owner yet"), hearts, hunger and thirst when you look at it.
@EventBusSubscriber(modid = Capitalism.MODID)
public final class Pets {

    public static final int TRAINING_LEVELS = 25;
    // A pet loses one hunger point every 300 seconds (full to empty in 100 minutes), one thirst point every 200 seconds
    // (about 67 minutes)
    public static final float HUNGER_PER_SECOND = 1f / 300f;
    public static final float THIRST_PER_SECOND = 1f / 200f;
    // standing in water, it drinks this much a second
    public static final float DRINK_PER_SECOND = 1f;
    // well fed and watered it heals half a heart every HEAL_SECONDS; with a bar empty it's hurt half a heart every
    // HURT_SECONDS
    public static final int HEAL_SECONDS = 10;
    public static final int HURT_SECONDS = 20;
    // a hungry or thirsty pet never goes below one heart, unless BOTH bars are empty, or one has been empty longer than
    // this: then it can starve to death
    public static final int STARVE_SECONDS = 300;
    // longest name the owner can give it (the pet window)
    public static final int MAX_NAME = 24;
    // both bars at least this full to heal (a share of its bars: they're as big as the pet, barSize)
    public static final float WELL_FED = 0.75f;
    public static final float WATER_BOTTLE = 8f;
    // Food Troughs and Water Bowls this close (blocks, 2 up or down) are found; a pet eats / drinks from one when it's
    // standing next to it, and walks over once it's this hungry / thirsty
    public static final int BOWL_RANGE = 5;
    public static final double REACH = 1.9;
    public static final float WALK_TO_FOOD_AT = 0.8f;
    // it eats / drinks only with this much room (so a full pet doesn't nibble every second as the bars tick down)
    public static final float ROOM_TO_EAT = 0.1f;
    public static final float ROOM_TO_DRINK = 0.2f;
    // a pet drinks what it needs from a Water Bowl: this many mL per thirst point (a whole bar is 500 mL)
    public static final float ML_PER_THIRST = 25f;
    // Pet window actions (PetActionPacket)
    public static final int ACTION_STAY = 0;

    private Pets() {
    }

    public static PetData data(Entity animal) {
        return animal.getData(ModAttachments.PET.get());
    }

    public static boolean claimed(Entity entity) {
        return entity instanceof Animal && entity.hasData(ModAttachments.PET.get()) && data(entity).claimed();
    }

    public static boolean isPet(Entity entity) {
        return entity instanceof Animal && entity.hasData(ModAttachments.PET.get()) && data(entity).pet();
    }

    // Is this player its owner (or in creative)? Only for a claimed pet: petting it, its window
    public static boolean isOwner(Entity animal, Player player) {
        return claimed(animal) && (player.isCreative() || data(animal).isOwner(player.getUUID()));
    }

    // Can this player do things with it (the owner, or anyone in creative)? Unclaimed animals are anyone's.
    public static boolean mayUse(Entity animal, Player player) {
        return !claimed(animal) || player.isCreative() || data(animal).isOwner(player.getUUID());
    }

    // Saved changes go out to everyone watching
    private static void save(Entity animal, PetData data) {
        animal.setData(ModAttachments.PET.get(), data);
    }

    // ── Claiming (Ownership Tag) ──
    // Null when it worked, or why not
    public static @Nullable String claim(Animal animal, Player player) {
        if (claimed(animal)) return "This animal already belongs to " + data(animal).ownerName() + ".";
        if (!isPet(animal)) return "Only a trained pet can be claimed. A Pet Trainer has to train it first.";
        if (animal instanceof TamableAnimal tame && tame.isTame() && !tame.isOwnedBy(player)) return "Someone else tamed this animal.";
        PetData data = data(animal);
        data.claim(player.getUUID(), player.getGameProfile().name());
        save(animal, data);
        animal.setPersistenceRequired();
        return null;
    }

    // ── Training (Training Stick) ──
    // Not riding animals: horses, donkeys and mules (skeleton and zombie horses too). Llamas and camels are fine.
    private static final java.util.Set<net.minecraft.world.entity.EntityType<?>> RIDING = java.util.Set.of(
        net.minecraft.world.entity.EntityTypes.HORSE, net.minecraft.world.entity.EntityTypes.DONKEY, net.minecraft.world.entity.EntityTypes.MULE,
        net.minecraft.world.entity.EntityTypes.SKELETON_HORSE, net.minecraft.world.entity.EntityTypes.ZOMBIE_HORSE);

    public static boolean trainable(Animal animal) {
        return !RIDING.contains(animal.getType());
    }

    public static @Nullable String train(Animal animal, Player trainer) {
        if (!trainable(animal)) return "Riding animals can't be trained into pets.";
        if (isPet(animal)) return "It's already a pet.";
        if (!trainer.isCreative() && trainer.experienceLevel < TRAINING_LEVELS) return "Training takes " + TRAINING_LEVELS + " levels.";
        if (!trainer.isCreative()) trainer.giveExperienceLevels(-TRAINING_LEVELS);
        PetData data = data(animal);
        data.setSize(barSize(animal));
        data.train();
        save(animal, data);
        if (!animal.hasCustomName()) {
            animal.setCustomName(Component.literal("Pet ").append(animal.getType().getDescription()));
        }
        animal.setCustomNameVisible(true);
        animal.setPersistenceRequired();
        return null;
    }

    // ── Others keep their hands off; anyone can feed or water a pet ──
    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        Entity target = event.getTarget();
        if (!isPet(target)) return;
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (care((Animal) target, player, stack, event.getHand())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }
        // the owner pets it with an empty hand (unless it's saddled: then it's for riding)
        if (event.getHand() == InteractionHand.MAIN_HAND && stack.isEmpty() && isOwner(target, player)
            && ((Animal) target).getItemBySlot(net.minecraft.world.entity.EquipmentSlot.SADDLE).isEmpty()) {
            if (target.level() instanceof ServerLevel level) {
                ((Animal) target).playAmbientSound();
                petBuff((Animal) target, player);
            }
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }
        // a tag on an owned pet says whose it is
        if (stack.is(ModItems.TRAINING_STICK.get()) || stack.is(ModItems.OWNERSHIP_TAG.get())) return;
        if (!claimed(target)) return;
        if (!mayUse(target, player)) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendOverlayMessage(Component.literal("This animal belongs to " + data(target).ownerName() + ".").withStyle(ChatFormatting.RED));
            }
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    // Feeding (its own food, while it's hungry) and watering (while it's thirsty). True if it was used.
    private static boolean care(Animal pet, Player player, ItemStack stack, InteractionHand hand) {
        PetData data = data(pet);
        boolean water = stack.is(Items.WATER_BUCKET) || isWaterBottle(stack);
        boolean food = !water && PetTraits.eats(pet, stack);
        if (water ? data.thirst() >= data.max() : !food || data.hunger() >= data.max()) return false;
        if (pet.level().isClientSide()) return true;
        if (water) {
            data.setThirst(stack.is(Items.WATER_BUCKET) ? data.max() : data.thirst() + WATER_BOTTLE);
            if (!player.isCreative()) {
                ItemStack empty = new ItemStack(stack.is(Items.WATER_BUCKET) ? Items.BUCKET : Items.GLASS_BOTTLE);
                stack.shrink(1);
                if (stack.isEmpty()) player.setItemInHand(hand, empty);
                else if (!player.getInventory().add(empty)) player.drop(empty, false, net.minecraft.util.Prediction.SERVER_ONLY);
            }
            pet.playSound(SoundEvents.GENERIC_DRINK.value(), 0.8F, 1.0F);
        } else {
            data.setHunger(data.hunger() + foodPoints(stack));
            if (player instanceof net.minecraft.server.level.ServerPlayer owner && isOwner(pet, player)) com.alexh.capitalism.bounty.BountyEvents.petFed(owner);
            if (!player.isCreative()) stack.shrink(1);
            pet.playSound(SoundEvents.GENERIC_EAT.value(), 0.8F, 1.0F);
        }
        save(pet, data);
        ((ServerLevel) pet.level()).sendParticles(ParticleTypes.HEART, pet.getX(), pet.getY() + pet.getBbHeight(), pet.getZ(), 2, 0.3, 0.2, 0.3, 0);
        return true;
    }

    public static boolean isWaterBottle(ItemStack stack) {
        PotionContents potion = stack.get(DataComponents.POTION_CONTENTS);
        return stack.is(Items.POTION) && potion != null && potion.is(Potions.WATER);
    }

    // How much a food fills a pet: what it'd give a player (wheat, seeds and the like: 4)
    public static float foodPoints(ItemStack stack) {
        FoodProperties food = stack.get(DataComponents.FOOD);
        return food != null ? Math.max(2, food.nutrition()) : 4f;
    }

    // ── Other players can't hurt a claimed animal ──
    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (!claimed(event.getEntity())) return;
        if (event.getSource().getEntity() instanceof Player attacker && !mayUse(event.getEntity(), attacker)) event.setCanceled(true);
    }

    // ── Pets are docile: they never pick a target (polar bears, hoglins, wolves, bees, llamas...) and never hurt anyone ──
    @SubscribeEvent
    public static void onTarget(net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() != null && isPet(event.getEntity())) event.setNewAboutToBeSetTarget(null);
    }

    @SubscribeEvent
    public static void onPetAttack(LivingIncomingDamageEvent event) {
        if (isPet(event.getSource().getEntity()) || isPet(event.getSource().getDirectEntity())) event.setCanceled(true);
    }

    // ── Once a second: a pet gets hungry and thirsty, drinks in water, heals or hurts ──
    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity.tickCount % 20 != 0 || entity.level().isClientSide() || !(entity instanceof Animal pet) || !isPet(pet)) return;
        second(pet);
    }

    public static void second(Animal pet) {
        PetData data = data(pet);
        // a pet Hoglin stays a Hoglin in the Overworld (it doesn't turn into a Zoglin)
        if (pet instanceof net.minecraft.world.entity.monster.hoglin.Hoglin hoglin) hoglin.setImmuneToZombification(true);
        data.setSize(barSize(pet));
        // an owned pet waits for its owner: nothing changes while they're offline (it can't starve while they're away)
        if (data.claimed() && !ownerOnline(pet, data)) return;
        data.setHunger(data.hunger() - hungerPerSecond(data));
        data.setThirst(pet.isInWater() ? data.thirst() + DRINK_PER_SECOND : data.thirst() - thirstPerSecond(data));
        // the Feeding Hat (only from the Traveling Merchant): this pet is always full and never thirsty
        if (data.hat() == com.alexh.capitalism.designer.Hat.FEEDING_HAT) {
            data.setHunger(data.max());
            data.setThirst(data.max());
        }
        if (!data.stay()) troughAndBowl(pet, data);
        boolean empty = data.hunger() <= 0 || data.thirst() <= 0;
        data.setEmptySeconds(empty ? data.emptySeconds() + 1 : 0);
        if (empty) {
            if (pet.tickCount % (20 * hurtSeconds(data)) == 0 && (pet.getHealth() > 2.0F || mayStarve(data))) {
                pet.hurtServer((ServerLevel) pet.level(), pet.damageSources().starve(), 1.0F);
            }
        } else if (pet.tickCount % (20 * HEAL_SECONDS) == 0 && data.hunger() >= data.max() * WELL_FED && data.thirst() >= data.max() * WELL_FED && pet.getHealth() < pet.getMaxHealth()) {
            pet.heal(1.0F);
        }
        save(pet, data);
    }

    // A Food Trough with its food / a Water Bowl with water nearby: eat or drink from it when next to it (till full), walk
    // over when hungry or thirsty enough. Thirst first when it's the emptier one.
    private static void troughAndBowl(Animal pet, PetData data) {
        ServerLevel level = (ServerLevel) pet.level();
        boolean thirstFirst = data.thirst() <= data.hunger();
        for (int i = 0; i < 2; i++) {
            boolean water = (i == 0) == thirstFirst;
            float have = water ? data.thirst() : data.hunger();
            if (have > data.max() * (1 - (water ? ROOM_TO_DRINK : ROOM_TO_EAT))) continue;
            BlockPos spot = water ? nearestBowl(level, pet) : nearestTrough(level, pet);
            if (spot == null) continue;
            if (pet.position().distanceTo(net.minecraft.world.phys.Vec3.atBottomCenterOf(spot)) <= REACH) {
                if (water) {
                    if (level.getBlockEntity(spot) instanceof WaterBowlBlockEntity bowl) {
                        int taken = bowl.take(Math.round((data.max() - data.thirst()) * ML_PER_THIRST));
                        if (taken > 0) {
                            data.setThirst(data.thirst() + taken / ML_PER_THIRST);
                            pet.playSound(SoundEvents.GENERIC_DRINK.value(), 0.6F, 1.0F);
                        }
                    }
                } else if (level.getBlockEntity(spot) instanceof FoodTroughBlockEntity trough) {
                    ItemStack food = trough.takeFoodFor(pet);
                    if (!food.isEmpty()) {
                        data.setHunger(data.hunger() + foodPoints(food));
                        pet.playSound(SoundEvents.GENERIC_EAT.value(), 0.6F, 1.0F);
                        level.sendParticles(new net.minecraft.core.particles.ItemParticleOption(ParticleTypes.ITEM, food.getItem()),
                            pet.getX(), pet.getY() + pet.getBbHeight() * 0.6, pet.getZ(), 5, 0.2, 0.1, 0.2, 0.05);
                    }
                }
                pet.getNavigation().stop();
                pet.getLookControl().setLookAt(spot.getX() + 0.5, spot.getY() + 0.3, spot.getZ() + 0.5);
                return;
            }
            if (have <= data.max() * WALK_TO_FOOD_AT) {
                pet.getNavigation().moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 1.0);
                return;
            }
        }
    }

    private static @Nullable BlockPos nearestTrough(ServerLevel level, Animal pet) {
        return nearest(level, pet, pos -> level.getBlockEntity(pos) instanceof FoodTroughBlockEntity trough && trough.hasFoodFor(pet));
    }

    private static @Nullable BlockPos nearestBowl(ServerLevel level, Animal pet) {
        return nearest(level, pet, pos -> {
            return level.getBlockEntity(pos) instanceof WaterBowlBlockEntity bowl && bowl.water() > 0;
        });
    }

    private static @Nullable BlockPos nearest(ServerLevel level, Animal pet, java.util.function.Predicate<BlockPos> wanted) {
        BlockPos center = pet.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-BOWL_RANGE, -2, -BOWL_RANGE), center.offset(BOWL_RANGE, 2, BOWL_RANGE))) {
            if (!wanted.test(pos)) continue;
            double distance = pos.distSqr(center);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = pos.immutable();
            }
        }
        return best;
    }

    // ── The pet window's actions (PetActionPacket): only the owner, close by ──
    public static void action(ServerPlayer player, int entityId, int action) {
        Entity entity = player.level().getEntity(entityId);
        if (!(entity instanceof Animal pet) || !isPet(pet) || !isOwner(pet, player) || pet.distanceTo(player) > 16) return;
        if (action == ACTION_STAY) setStay(pet, !data(pet).stay());
    }

    // The first pet of each Minecraft day: its owner gets its buff (PetTraits). True if they got it.
    public static boolean petBuff(Animal pet, Player owner) {
        PetData data = data(pet);
        long day = day(pet.level());
        if (data.pettedDay() == day) return false;
        data.setPettedDay(day);
        save(pet, data);
        // one pet buff at a time: this one takes the last one's place (only the pet's part of it)
        PetBuffs.give(owner, PetTraits.of(pet.getType()).buffs().stream().map(PetTraits.Buff::instance).toList());
        pet.level().playSound(null, pet.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 0.8F, 1.4F);
        // little patting hands float up (only for this first pet of the day)
        if (pet.level() instanceof ServerLevel level) {
            level.sendParticles(com.alexh.capitalism.core.ModParticles.PETTING.get(), pet.getX(), pet.getY() + pet.getBbHeight(), pet.getZ(), 4, 0.3, 0.2, 0.3, 0);
        }
        return true;
    }

    // Which Minecraft day it is (sleeping through the night moves it on)
    public static long day(net.minecraft.world.level.Level level) {
        return level.getOverworldClockTime() / 24000L;
    }

    // A new name from the pet window (RenamePetPacket): only the owner, close by. Empty: back to "Pet <Animal>".
    public static void rename(ServerPlayer player, int entityId, String name) {
        Entity entity = player.level().getEntity(entityId);
        if (!(entity instanceof Animal pet) || !isPet(pet) || !isOwner(pet, player) || pet.distanceTo(player) > 16) return;
        String clean = net.minecraft.util.StringUtil.filterText(name).trim();
        if (clean.length() > MAX_NAME) clean = clean.substring(0, MAX_NAME);
        pet.setCustomName(clean.isEmpty() ? Component.literal("Pet ").append(pet.getType().getDescription()) : Component.literal(clean));
        pet.setCustomNameVisible(true);
    }

    // ── Hats (designer/Hat): the Top Hat slows hunger, the Santa Hat thirst, the Baseball Cap starving, the Fedora adds
    // hearts and room on both bars ──

    // How fast the bars drop: the same points per second for every pet, whatever its bar's size (a chicken's small bar
    // empties sooner, a polar bear's big one lasts longer)
    public static float hungerPerSecond(PetData data) {
        if (data.hat() == com.alexh.capitalism.designer.Hat.FEEDING_HAT) return 0F;
        return data.hat() == com.alexh.capitalism.designer.Hat.GUCCI_TOP_HAT ? HUNGER_PER_SECOND * com.alexh.capitalism.designer.Hat.SLOWER : HUNGER_PER_SECOND;
    }

    public static float thirstPerSecond(PetData data) {
        if (data.hat() == com.alexh.capitalism.designer.Hat.FEEDING_HAT) return 0F;
        return data.hat() == com.alexh.capitalism.designer.Hat.BURBERRY_SANTA_HAT ? THIRST_PER_SECOND * com.alexh.capitalism.designer.Hat.SLOWER : THIRST_PER_SECOND;
    }

    // How big a pet's hunger and thirst bars are, like its hearts: 2 points (one drumstick / drop) for every heart of its
    // own health (without a hat), at least 3 and at most 15 of each. A chicken 3, a pig 5, a polar bear 15.
    public static float barSize(Animal pet) {
        net.minecraft.world.entity.ai.attributes.AttributeInstance health = pet.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        double base = health == null ? PetData.MAX : health.getBaseValue();
        return net.minecraft.util.Mth.clamp(2 * net.minecraft.util.Mth.ceil(base / 2), 6, 30);
    }

    // how often starving hurts it (twice as long with a Baseball Cap)
    public static int hurtSeconds(PetData data) {
        return data.hat() == com.alexh.capitalism.designer.Hat.LOUIS_VUITTON_CAP ? HURT_SECONDS * 2 : HURT_SECONDS;
    }

    private static final net.minecraft.resources.Identifier FEDORA = net.minecraft.resources.Identifier.fromNamespaceAndPath(Capitalism.MODID, "prada_fedora");

    // Put a hat on (hat: the one to take from the player's inventory) or take it off (null), from the pet window
    // (PetHatPacket): only the owner, close by. The old hat goes back to them.
    public static void wearHat(ServerPlayer player, int entityId, com.alexh.capitalism.designer.@Nullable Hat hat) {
        Entity entity = player.level().getEntity(entityId);
        if (!(entity instanceof Animal pet) || !isPet(pet) || !isOwner(pet, player) || pet.distanceTo(player) > 16) return;
        PetData data = data(pet);
        if (hat != null) {
            int slot = -1;
            for (int i = 0; i < player.getInventory().getContainerSize() && slot < 0; i++) {
                if (player.getInventory().getItem(i).is(hat.item())) slot = i;
            }
            if (slot < 0) return;
            player.getInventory().getItem(slot).shrink(1);
        }
        com.alexh.capitalism.designer.Hat old = data.hat();
        if (old != null) player.getInventory().placeItemBackInInventory(new ItemStack(old.item()), net.minecraft.util.Prediction.SERVER_ONLY);
        setHat(pet, data, hat);
        pet.level().playSound(null, pet.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.NEUTRAL, 0.8F, 1.2F);
    }

    // Sets the hat and what comes with it (the Fedora's extra hearts)
    public static void setHat(Animal pet, PetData data, com.alexh.capitalism.designer.@Nullable Hat hat) {
        data.setHat(hat);
        save(pet, data);
        net.minecraft.world.entity.ai.attributes.AttributeInstance health = pet.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        if (health == null) return;
        health.removeModifier(FEDORA);
        if (hat == com.alexh.capitalism.designer.Hat.PRADA_FEDORA) {
            health.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(FEDORA, com.alexh.capitalism.designer.Hat.FEDORA_HEALTH,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
        } else if (pet.getHealth() > pet.getMaxHealth()) {
            pet.setHealth(pet.getMaxHealth());
        }
    }

    // A pet that dies drops its hat
    @SubscribeEvent
    public static void onPetDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Animal pet) || !isPet(pet) || pet.level().isClientSide()) return;
        com.alexh.capitalism.designer.Hat hat = data(pet).hat();
        if (hat != null) pet.spawnAtLocation((ServerLevel) pet.level(), new ItemStack(hat.item()));
    }

    // Stay Still: no AI at all (doesn't walk, turn or wander) until it's let go
    public static void setStay(Animal pet, boolean stay) {
        PetData data = data(pet);
        data.setStay(stay);
        save(pet, data);
        pet.setNoAi(stay);
        if (stay) pet.getNavigation().stop();
    }

    public static boolean ownerOnline(Animal pet, PetData data) {
        return data.owner() != null && pet.level().getServer() != null && pet.level().getServer().getPlayerList().getPlayer(data.owner()) != null;
    }

    // How long this player has played (ticks online; the Animal Bjorn counts carried time with it)
    public static long playedTicks(Player player) {
        return player instanceof ServerPlayer serverPlayer ? serverPlayer.getStats().getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.PLAY_TIME)) : 0;
    }

    // Can a starving pet die now? Only with both bars empty, or one empty longer than STARVE_SECONDS (otherwise it stops at
    // one heart)
    public static boolean mayStarve(PetData data) {
        return (data.hunger() <= 0 && data.thirst() <= 0) || data.emptySeconds() > STARVE_SECONDS;
    }

    // A pet that's been away (carried in an Animal Bjorn) for this many seconds: as hungry and thirsty as it would be
    // now, and hurt half a heart for every HURT_SECONDS a bar was empty (down to one heart, or to death if it may starve,
    // mayStarve). No troughs or healing.
    public static void timePassed(Animal pet, long seconds) {
        if (seconds <= 0) return;
        PetData data = data(pet);
        float hunger = data.hunger();
        float thirst = data.thirst();
        // when the first bar ran dry, if it did
        double dry = Math.min(hunger / hungerPerSecond(data), thirst / thirstPerSecond(data));
        data.setHunger(hunger - seconds * hungerPerSecond(data));
        data.setThirst(thirst - seconds * thirstPerSecond(data));
        if (data.hat() == com.alexh.capitalism.designer.Hat.FEEDING_HAT) {
            data.setHunger(data.max());
            data.setThirst(data.max());
        }
        double emptyFor = Math.max(0, seconds - dry);
        data.setEmptySeconds(data.hunger() <= 0 || data.thirst() <= 0 ? data.emptySeconds() + (int) emptyFor : 0);
        save(pet, data);
        int hurt = (int) (emptyFor / hurtSeconds(data));
        if (hurt <= 0) return;
        if (mayStarve(data) && pet.level() instanceof ServerLevel level) pet.hurtServer(level, pet.damageSources().starve(), hurt);
        else if (pet.getHealth() > 2.0F) pet.setHealth(Math.max(2.0F, pet.getHealth() - hurt));
    }

    // The animal's name for messages: its name tag, or its kind
    public static Component name(Entity animal) {
        return animal.hasCustomName() ? animal.getCustomName() : animal.getType().getDescription();
    }

    public static void sparkle(Animal animal) {
        if (animal.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, animal.getX(), animal.getY() + animal.getBbHeight() * 0.6, animal.getZ(), 12, 0.4, 0.4, 0.4, 0);
            level.playSound(null, animal.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.6F, 1.4F);
        }
    }
}
