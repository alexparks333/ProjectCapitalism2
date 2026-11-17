package com.alexh.capitalism.pets;

import com.alexh.capitalism.core.ModAttachments;
import com.alexh.capitalism.core.ModBlocks;
import com.alexh.capitalism.core.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import com.alexh.capitalism.gametest.GameTests;
import com.alexh.capitalism.occupation.Job;
import com.alexh.capitalism.npc.NpcShops;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;

// Automated checks for the Pet Trainer (PetTrainer.md), run by the game test server (gametest/GameTests,
// `gradlew runGameTestServer`): training with the Training Stick (job, levels, name, wear), claiming the pet with an
// Ownership Tag (only a trained one), other players kept off, a pet's hunger, thirst, feeding, watering, healing and never starving to death, eating
// and drinking from a Food Trough / Water Bowl, Stay Still and petting.
public final class PetTests {

    static {
        GameTests.register("pet_claiming", PetTests::claiming);
        GameTests.register("pet_training", PetTests::training);
        GameTests.register("pet_care", PetTests::care);
        GameTests.register("pet_trough_and_bowl", PetTests::troughAndBowl);
        GameTests.register("pet_actions", PetTests::actions);
        GameTests.register("pet_bjorn", PetTests::bjorn);
        GameTests.register("pet_one_buff", PetTests::oneBuff);
        GameTests.register("pet_training_feed", PetTests::trainingFeed);
        GameTests.register("pet_bar_size", PetTests::barSize);
    }

    private PetTests() {
    }

    // Loaded by GameTests.init (runs the static block above)
    public static void init() {
    }

    private static Animal pig(GameTests.Checks c) {
        Vec3 at = c.at(2.5, 2, 2.5);
        Animal pig = (Animal) EntityTypes.PIG.create(c.level(), EntitySpawnReason.MOB_SUMMONED);
        // a big pig (20 health): bars of the old full 20 points, so the numbers below stay simple (Pets.barSize)
        pig.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(20);
        pig.snapTo(at.x, at.y, at.z, 0, 0);
        c.level().addFreshEntity(pig);
        return pig;
    }

    private static ServerPlayer trainer(GameTests.Checks c, int levels) {
        ServerPlayer trainer = c.player();
        trainer.getData(ModAttachments.JOBS.get()).add(Job.PET_TRAINER);
        trainer.experienceLevel = levels;
        return trainer;
    }

    // Right-click the animal with this in hand, the way the game does (the interact event first, then the item)
    private static void use(ServerPlayer player, Animal animal, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        PlayerInteractEvent.EntityInteract event = new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, animal, animal.position());
        if (NeoForge.EVENT_BUS.post(event).isCanceled()) return;
        stack.getItem().interactLivingEntity(stack, player, animal, InteractionHand.MAIN_HAND);
    }

    // A pig a Pet Trainer has trained (nobody's yet)
    private static Animal trainedPig(GameTests.Checks c) {
        Animal pig = pig(c);
        use(trainer(c, 100), pig, new ItemStack(ModItems.TRAINING_STICK.get()));
        return pig;
    }

    // ... and then claimed with an Ownership Tag
    private static Animal claimedPig(GameTests.Checks c, ServerPlayer owner) {
        Animal pig = trainedPig(c);
        use(owner, pig, new ItemStack(ModItems.OWNERSHIP_TAG.get()));
        return pig;
    }

    private static void claiming(GameTests.Checks c) {
        ServerPlayer owner = c.player();

        // only a trained pet can be claimed
        Animal wild = pig(c);
        ItemStack tag = new ItemStack(ModItems.OWNERSHIP_TAG.get(), 2);
        use(owner, wild, tag);
        c.check(!Pets.claimed(wild) && tag.getCount() == 2, "an Ownership Tag does nothing on an untrained animal (and isn't used up)");

        Animal pig = trainedPig(c);
        c.check(Pets.isPet(pig) && !Pets.claimed(pig), "a trained pet is nobody's yet");
        use(owner, pig, tag);
        c.check(Pets.claimed(pig) && Pets.data(pig).isOwner(owner.getUUID()), "an Ownership Tag claims the pet");
        c.check(tag.getCount() == 1, "the tag is used up");

        ServerPlayer thief = c.player();
        ItemStack another = new ItemStack(ModItems.OWNERSHIP_TAG.get());
        use(thief, pig, another);
        c.check(Pets.data(pig).isOwner(owner.getUUID()) && another.getCount() == 1, "someone else can't claim it again (and keeps their tag)");

        float before = pig.getHealth();
        pig.hurtServer(c.level(), c.level().damageSources().playerAttack(thief), 2.0F);
        c.check(pig.getHealth() == before, "another player can't hurt it");
        pig.hurtServer(c.level(), c.level().damageSources().playerAttack(owner), 2.0F);
        c.check(pig.getHealth() < before, "the owner can");

        c.check(!Pets.mayUse(pig, thief) && Pets.mayUse(pig, owner), "only the owner may use it");
        c.check(!Pets.isOwner(pig, thief) && Pets.isOwner(pig, owner), "only the owner can pet it or open its window");
        ItemStack lead = new ItemStack(Items.LEAD);
        use(thief, pig, lead);
        c.check(!pig.isLeashed() && lead.getCount() == 1, "another player can't put a lead on it");
    }

    private static void training(GameTests.Checks c) {
        // not a Pet Trainer: nothing happens
        ServerPlayer someone = c.player();
        Animal pig = pig(c);
        someone.experienceLevel = 30;
        ItemStack stick = new ItemStack(ModItems.TRAINING_STICK.get());
        use(someone, pig, stick);
        c.check(!Pets.isPet(pig) && someone.experienceLevel == 30 && stick.getDamageValue() == 0, "only a Pet Trainer can use the stick");

        // a Pet Trainer without enough levels
        ServerPlayer poor = trainer(c, Pets.TRAINING_LEVELS - 1);
        use(poor, pig, stick);
        c.check(!Pets.isPet(pig) && poor.experienceLevel == Pets.TRAINING_LEVELS - 1, "training needs " + Pets.TRAINING_LEVELS + " levels");

        // a Pet Trainer trains a wild pig
        ServerPlayer trainer = trainer(c, 200);
        use(trainer, pig, stick);
        c.check(Pets.isPet(pig), "trained into a pet");
        c.check(trainer.experienceLevel == 200 - Pets.TRAINING_LEVELS, "it costs " + Pets.TRAINING_LEVELS + " levels (left: " + trainer.experienceLevel + ")");
        c.check(pig.hasCustomName() && pig.getCustomName().getString().equals("Pet Pig"), "it's called Pet Pig (got " + (pig.hasCustomName() ? pig.getCustomName().getString() : "nothing") + ")");
        c.check(!Pets.claimed(pig), "training doesn't make it anyone's");
        c.check(pig.isPersistenceRequired(), "a pet never despawns");
        c.check(Pets.data(pig).hunger() == PetData.MAX && Pets.data(pig).thirst() == PetData.MAX, "a new pet starts full");
        c.check(stick.getDamageValue() == 1, "the stick wears a little");
        use(trainer, pig, stick);
        c.check(trainer.experienceLevel == 200 - Pets.TRAINING_LEVELS && stick.getDamageValue() == 1, "a pet can't be trained twice");

        // riding animals can't be trained (llamas and camels can)
        for (net.minecraft.world.entity.EntityType<?> type : List.of(EntityTypes.HORSE, EntityTypes.DONKEY, EntityTypes.MULE)) {
            Animal mount = (Animal) type.create(c.level(), EntitySpawnReason.MOB_SUMMONED);
            Vec3 at = c.at(2.5, 2, 2.5);
            mount.snapTo(at.x, at.y, at.z, 0, 0);
            c.level().addFreshEntity(mount);
            use(trainer, mount, stick);
            c.check(!Pets.isPet(mount) && trainer.experienceLevel == 200 - Pets.TRAINING_LEVELS, type.getDescription().getString() + " can't be trained (a riding animal)");
            mount.discard();
        }

        for (net.minecraft.world.entity.EntityType<?> type : List.of(EntityTypes.LLAMA, EntityTypes.CAMEL)) {
            Animal animal = (Animal) type.create(c.level(), EntitySpawnReason.MOB_SUMMONED);
            Vec3 at = c.at(2.5, 2, 2.5);
            animal.snapTo(at.x, at.y, at.z, 0, 0);
            c.level().addFreshEntity(animal);
            use(trainer(c, 100), animal, new ItemStack(ModItems.TRAINING_STICK.get()));
            c.check(Pets.isPet(animal), type.getDescription().getString() + " can be trained");
            animal.discard();
        }

        // the stick breaks after its last use
        for (int i = 1; i < TrainingStickItem.USES; i++) use(trainer, pig(c), stick);
        c.check(stick.isEmpty(), "the stick breaks after " + TrainingStickItem.USES + " pets");
    }

    private static Animal pet(GameTests.Checks c) {
        return claimedPig(c, c.player());
    }

    private static void care(GameTests.Checks c) {
        Animal pig = pet(c);
        pig.tickCount = 1;
        for (int i = 0; i < 300; i++) Pets.second(pig);
        c.near(Pets.data(pig).hunger(), PetData.MAX - 300 * Pets.HUNGER_PER_SECOND, 1e-3, "hunger drops over time");
        c.near(Pets.data(pig).thirst(), PetData.MAX - 300 * Pets.THIRST_PER_SECOND, 1e-3, "thirst drops faster");

        // its owner logs off: nothing changes while they're away
        Animal waiting = pet(c);
        Pets.data(waiting).claim(java.util.UUID.randomUUID(), "Away");
        Pets.data(waiting).setHunger(1);
        Pets.data(waiting).setThirst(0);
        waiting.setHealth(5);
        waiting.tickCount = 20 * Pets.HURT_SECONDS;
        for (int i = 0; i < 600; i++) Pets.second(waiting);
        c.check(Pets.data(waiting).hunger() == 1 && Pets.data(waiting).thirst() == 0 && waiting.getHealth() == 5,
            "an owner who's offline: their pet doesn't get hungrier, thirstier or hurt");

        // anyone can feed and water it
        ServerPlayer stranger = c.player();
        Pets.data(pig).setHunger(10);
        float hunger = Pets.data(pig).hunger();
        ItemStack carrots = new ItemStack(Items.CARROT, 3);
        use(stranger, pig, carrots);
        c.near(Pets.data(pig).hunger(), hunger + Pets.foodPoints(new ItemStack(Items.CARROT)), 1e-3, "a carrot feeds a pet pig");
        c.check(carrots.getCount() == 2, "one carrot eaten");
        ItemStack bucket = new ItemStack(Items.WATER_BUCKET);
        use(stranger, pig, bucket);
        c.check(Pets.data(pig).thirst() == PetData.MAX, "a Water Bucket fills its thirst");
        c.check(stranger.getMainHandItem().is(Items.BUCKET), "you get the empty bucket back");
        ItemStack wheat = new ItemStack(Items.WHEAT);
        use(stranger, pig, wheat);
        c.check(wheat.getCount() == 1, "a pig won't eat what isn't its food");

        // empty: hurts now and then, never below one heart
        Animal starving = pet(c);
        Pets.data(starving).setHunger(0);
        starving.setHealth(6);
        starving.tickCount = 20 * Pets.HURT_SECONDS;
        Pets.second(starving);
        c.check(starving.getHealth() < 6, "a starving pet gets hurt");
        starving.setHealth(2);
        starving.setInvulnerableTime(0);
        Pets.second(starving);
        c.check(starving.getHealth() == 2 && starving.isAlive(), "one bar empty for a short while: it stops at one heart");
        // one bar empty for over STARVE_SECONDS: it can die
        Animal neglected = pet(c);
        Pets.data(neglected).setHunger(0);
        Pets.data(neglected).setEmptySeconds(Pets.STARVE_SECONDS);
        neglected.setHealth(1);
        neglected.tickCount = 20 * Pets.HURT_SECONDS;
        Pets.second(neglected);
        c.check(!neglected.isAlive(), "one bar empty for over 5 minutes: it starves to death");
        // both bars empty: it can die straight away
        Animal parched = pet(c);
        Pets.data(parched).setHunger(0);
        Pets.data(parched).setThirst(0);
        parched.setHealth(1);
        parched.tickCount = 20 * Pets.HURT_SECONDS;
        Pets.second(parched);
        c.check(!parched.isAlive(), "both bars empty: it starves to death");

        // docile: a pet never picks a target or hurts anyone
        Animal calm = pet(c);
        ServerPlayer victim = c.player();
        float before = victim.getHealth();
        victim.hurtServer(c.level(), c.level().damageSources().mobAttack(calm), 4.0F);
        c.check(victim.getHealth() == before, "a pet can't hurt anyone");
        if (calm instanceof net.minecraft.world.entity.Mob mob) {
            mob.setTarget(victim);
            c.check(mob.getTarget() == null, "a pet won't pick a target");
        }

        // well fed and watered: it heals
        Animal healing = pet(c);
        healing.setHealth(4);
        healing.tickCount = 20 * Pets.HEAL_SECONDS;
        Pets.second(healing);
        c.check(healing.getHealth() > 4, "a well-fed pet heals");
    }

    // A pig next to a Food Trough eats its own food from it till it's full; next to a Water Bowl it drinks
    private static void troughAndBowl(GameTests.Checks c) {
        BlockPos troughAt = c.helper.absolutePos(new BlockPos(3, 2, 2));
        BlockPos bowlAt = c.helper.absolutePos(new BlockPos(1, 2, 2));
        c.level().setBlockAndUpdate(troughAt, ModBlocks.LARGE_FOOD_TROUGH.get().defaultBlockState());
        c.level().setBlockAndUpdate(bowlAt, ModBlocks.MEDIUM_WATER_BOWL.get().defaultBlockState());
        FoodTroughBlockEntity trough = (FoodTroughBlockEntity) c.level().getBlockEntity(troughAt);
        WaterBowlBlockEntity bowl = (WaterBowlBlockEntity) c.level().getBlockEntity(bowlAt);

        // the bowl: a Water Bucket pours in 1000 mL, you keep the bucket
        ServerPlayer filler = c.player();
        pour(c, filler, bowlAt);
        c.check(bowl.water() == WaterBowlBlock.BUCKET_ML, "a Water Bucket pours in 1000 mL (got " + bowl.water() + ")");
        c.check(filler.getMainHandItem().is(Items.BUCKET), "you get the bucket back");
        c.check(c.level().getBlockState(bowlAt).getValue(WaterBowlBlock.LEVEL) == 1, "a third full shows a third of water");

        // only its own food: wheat isn't a pig's
        trough.setItem(0, new ItemStack(Items.WHEAT, 4));
        Animal pig = pet(c);
        pig.tickCount = 1;
        Pets.data(pig).setHunger(10);
        Pets.second(pig);
        c.check(trough.getItem(0).getCount() == 4 && Pets.data(pig).hunger() < 10, "a pig doesn't eat wheat from the trough");
        c.check(c.level().getBlockState(troughAt).getValue(FoodTroughBlock.FILLED), "a trough with something in it shows food");

        // its food: one a second till it's full
        trough.setItem(1, new ItemStack(Items.CARROT, 10));
        for (int i = 0; i < 10; i++) Pets.second(pig);
        c.check(Pets.data(pig).hunger() >= PetData.MAX - Pets.ROOM_TO_EAT, "it eats from the trough till it's full (hunger " + Pets.data(pig).hunger() + ")");
        int left = trough.getItem(1).getCount();
        // what it needed: carrots while there's room for a bite
        int needed = 0;
        for (float h = 10 - Pets.HUNGER_PER_SECOND; h <= PetData.MAX - Pets.ROOM_TO_EAT; h += Pets.foodPoints(new ItemStack(Items.CARROT))) needed++;
        c.check(10 - left == needed, "it took only what it needed (" + (10 - left) + " carrots, expected " + needed + ")");

        // thirsty: it drinks just what it needs (25 mL a point)
        Pets.data(pig).setThirst(4);
        Pets.second(pig);
        c.check(Pets.data(pig).thirst() >= PetData.MAX - 0.01, "a drink from the bowl fills its thirst (" + Pets.data(pig).thirst() + ")");
        int drunk = WaterBowlBlock.BUCKET_ML - bowl.water();
        c.check(drunk >= 395 && drunk <= 405, "it drank about 400 mL for 16 thirst (" + drunk + ")");
        // a nearly empty bowl: it gets what's left
        bowl.set(100);
        Pets.data(pig).setThirst(4);
        Pets.second(pig);
        c.check(bowl.water() == 0 && Pets.data(pig).thirst() > 7 && Pets.data(pig).thirst() < 9, "a nearly empty bowl gives what's left");
        c.check(c.level().getBlockState(bowlAt).getValue(WaterBowlBlock.LEVEL) == 0, "an empty bowl shows empty");

        // staying still: it doesn't go to the trough
        Pets.setStay(pig, true);
        Pets.data(pig).setHunger(5);
        Pets.second(pig);
        c.check(Pets.data(pig).hunger() < 5, "a pet told to stay doesn't eat from the trough");
        pig.discard();

        // empty the trough: the heap goes
        trough.clearContent();
        trough.setChanged();
        c.check(!c.level().getBlockState(troughAt).getValue(FoodTroughBlock.FILLED), "an empty trough shows empty");

        // the sizes: 1, 2, 5 slots of food; 1000, 3000, 10000 mL of water
        FeederSize[] sizes = FeederSize.values();
        net.minecraft.world.level.block.Block[] troughBlocks = {ModBlocks.SMALL_FOOD_TROUGH.get(), ModBlocks.MEDIUM_FOOD_TROUGH.get(), ModBlocks.LARGE_FOOD_TROUGH.get()};
        net.minecraft.world.level.block.Block[] bowlBlocks = {ModBlocks.SMALL_WATER_BOWL.get(), ModBlocks.MEDIUM_WATER_BOWL.get(), ModBlocks.LARGE_WATER_BOWL.get()};
        int[] slots = {1, 2, 5};
        for (int i = 0; i < 3; i++) {
            c.level().setBlockAndUpdate(troughAt, troughBlocks[i].defaultBlockState());
            c.check(c.level().getBlockEntity(troughAt) instanceof FoodTroughBlockEntity sized && sized.getContainerSize() == slots[i],
                sizes[i].title + " Food Trough has " + slots[i] + " slots of food");
            c.level().setBlockAndUpdate(bowlAt, bowlBlocks[i].defaultBlockState());
            for (int b = 0; b < 12; b++) pour(c, filler, bowlAt);
            WaterBowlBlockEntity sized = (WaterBowlBlockEntity) c.level().getBlockEntity(bowlAt);
            c.check(sized.water() == sizes[i].waterMl, sizes[i].title + " Water Bowl holds " + sizes[i].waterMl + " mL (got " + sized.water() + ")");
            c.check(c.level().getBlockState(bowlAt).getValue(WaterBowlBlock.LEVEL) == 3, sizes[i].title + " Water Bowl shows full");
        }
    }

    // Pour a Water Bucket on the bowl (as if right-clicked)
    private static void pour(GameTests.Checks c, ServerPlayer player, BlockPos bowlAt) {
        ItemStack bucket = new ItemStack(Items.WATER_BUCKET);
        player.setItemInHand(InteractionHand.MAIN_HAND, bucket);
        c.level().getBlockState(bowlAt).useItemOn(bucket, c.level(), player, InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atCenterOf(bowlAt), Direction.UP, bowlAt, false));
    }

    // Stay Still from the pet window (only the owner), and petting
    private static void actions(GameTests.Checks c) {
        ServerPlayer owner = c.player();
        Animal pig = claimedPig(c, owner);
        ServerPlayer stranger = c.player();
        Pets.action(stranger, pig.getId(), Pets.ACTION_STAY);
        c.check(!Pets.data(pig).stay() && !pig.isNoAi(), "someone else can't tell it to stay");
        Pets.action(owner, pig.getId(), Pets.ACTION_STAY);
        c.check(Pets.data(pig).stay() && pig.isNoAi(), "Stay Still: it stops moving");
        Pets.rename(stranger, pig.getId(), "Thief's Pig");
        c.check(pig.getCustomName().getString().equals("Pet Pig"), "someone else can't rename it");
        Pets.rename(owner, pig.getId(), "  Sir Oinksalot  ");
        c.check(pig.getCustomName().getString().equals("Sir Oinksalot"), "the owner renames it (got " + pig.getCustomName().getString() + ")");
        Pets.rename(owner, pig.getId(), "x".repeat(40));
        c.check(pig.getCustomName().getString().length() == Pets.MAX_NAME, "names are cut to " + Pets.MAX_NAME + " letters");
        Pets.rename(owner, pig.getId(), "");
        c.check(pig.getCustomName().getString().equals("Pet Pig"), "an empty name goes back to Pet Pig");
        Pets.action(owner, pig.getId(), Pets.ACTION_STAY);
        c.check(!Pets.data(pig).stay() && !pig.isNoAi(), "Stay Still off: it moves again");

        owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        PlayerInteractEvent.EntityInteract pat = new PlayerInteractEvent.EntityInteract(owner, InteractionHand.MAIN_HAND, pig, pig.position());
        c.check(NeoForge.EVENT_BUS.post(pat).isCanceled(), "the owner pets it with an empty hand");

        // the first pet of the day gives its buff (a pig: Absorption), once a day
        c.check(owner.hasEffect(net.minecraft.world.effect.MobEffects.ABSORPTION), "petting a pet pig gives Absorption");
        owner.removeAllEffects();
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(owner, InteractionHand.MAIN_HAND, pig, pig.position()));
        c.check(!owner.hasEffect(net.minecraft.world.effect.MobEffects.ABSORPTION), "only once a day");
        Pets.data(pig).setPettedDay(Pets.day(c.level()) - 1);
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(owner, InteractionHand.MAIN_HAND, pig, pig.position()));
        c.check(owner.hasEffect(net.minecraft.world.effect.MobEffects.ABSORPTION), "the next day it works again");
        stranger.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        Pets.data(pig).setPettedDay(-1);
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(stranger, InteractionHand.MAIN_HAND, pig, pig.position()));
        c.check(!stranger.hasEffect(net.minecraft.world.effect.MobEffects.ABSORPTION), "someone else gets no buff from petting it");

        // every listed animal is one that can be a pet, and has at least one buff
        for (net.minecraft.world.entity.EntityType<?> type : PetTraits.listed()) {
            net.minecraft.world.entity.Entity made = type.create(c.level(), EntitySpawnReason.MOB_SUMMONED);
            c.check(made instanceof Animal animal && Pets.trainable(animal) && !PetTraits.of(type).buffs().isEmpty()
                    && net.minecraft.core.registries.BuiltInRegistries.ITEM.stream().anyMatch(item -> PetTraits.eats(animal, new ItemStack(item))),
                type.getDescription().getString() + " can be a pet, has a buff and has something to eat");
            if (made != null) made.discard();
        }
    }

    // One pet buff at a time (PetBuffs): petting another pet swaps the pet buffs, and never touches the player's own
    // effects (potions, food, drinks), even ones of the same kind
    private static void oneBuff(GameTests.Checks c) {
        ServerPlayer owner = c.player();
        // the player's own effects: a long Strength potion, a short Speed I, Night Vision
        owner.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 9600, 0));
        owner.addEffect(new MobEffectInstance(MobEffects.SPEED, 1200, 0));
        owner.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 3600, 0));

        // the first pet: a cow (Regeneration I)
        Animal cow = (Animal) EntityTypes.COW.create(c.level(), EntitySpawnReason.MOB_SUMMONED);
        Pets.petBuff(cow, owner);
        c.check(owner.hasEffect(MobEffects.REGENERATION), "petting a cow gives Regeneration");

        // a second pet takes the cow's place: Strength I and Speed II
        PetBuffs.give(owner, List.of(new MobEffectInstance(MobEffects.STRENGTH, 6000, 0), new MobEffectInstance(MobEffects.SPEED, 9000, 1)));
        c.check(!owner.hasEffect(MobEffects.REGENERATION), "the next pet buff takes the last one's place");
        c.check(owner.getEffect(MobEffects.SPEED).getAmplifier() == 1, "the pet's Speed II beats your own Speed I");
        c.check(owner.getEffect(MobEffects.STRENGTH).getDuration() > 9000, "your longer Strength potion keeps its time");
        // a drink after petting: Speed I for longer than the first one
        owner.addEffect(new MobEffectInstance(MobEffects.SPEED, 3600, 0));

        // a third pet: Haste II. The Strength and Speed II from the pet go; everything of your own stays
        PetBuffs.give(owner, List.of(new MobEffectInstance(MobEffects.HASTE, 6000, 1)));
        MobEffectInstance strength = owner.getEffect(MobEffects.STRENGTH);
        MobEffectInstance speed = owner.getEffect(MobEffects.SPEED);
        c.check(strength != null && strength.getAmplifier() == 0 && strength.getDuration() > 9000, "your own Strength potion stays (its own time)");
        c.check(speed != null && speed.getAmplifier() == 0 && speed.getDuration() > 3000, "your own Speed I comes back (the drink's time)");
        c.check(owner.hasEffect(MobEffects.NIGHT_VISION) && owner.getEffect(MobEffects.NIGHT_VISION).getDuration() > 3000, "other effects aren't touched");
        c.check(owner.getEffect(MobEffects.HASTE).getAmplifier() == 1 && PetBuffs.current(owner).size() == 1, "the new pet buff is on");

        // milk clears everything: the pet buff is forgotten too
        owner.removeAllEffects();
        c.check(PetBuffs.current(owner).isEmpty(), "milk clears the pet buff");
        // a pet buff that runs out is forgotten, and the next one takes nothing of yours
        PetBuffs.give(owner, List.of(new MobEffectInstance(MobEffects.LUCK, 100, 0)));
        PetBuffs.tick(owner, 100);
        c.check(PetBuffs.current(owner).isEmpty(), "a pet buff that has run out is forgotten");
        owner.addEffect(new MobEffectInstance(MobEffects.LUCK, 2000, 0));
        PetBuffs.give(owner, List.of(new MobEffectInstance(MobEffects.ABSORPTION, 100, 0)));
        c.check(owner.hasEffect(MobEffects.LUCK), "a new pet buff doesn't take an old effect of yours");
        cow.discard();

        // the Camel: fills your thirst bar at once, then it doesn't go down for a while
        owner.removeAllEffects();
        com.alexh.capitalism.player.ThirstData thirst = owner.getData(ModAttachments.THIRST.get());
        thirst.drain(30);
        Animal camel = (Animal) EntityTypes.CAMEL.create(c.level(), EntitySpawnReason.MOB_SUMMONED);
        Pets.petBuff(camel, owner);
        c.check(owner.hasEffect(com.alexh.capitalism.core.ModEffects.REFILL_THIRST) && owner.hasEffect(com.alexh.capitalism.core.ModEffects.NO_WATER_DRAINING),
            "petting a camel gives Refill Thirst Bar and No Water Draining");
        com.alexh.capitalism.core.ModEffects.REFILL_THIRST.value().applyEffectTick(c.level(), owner, 0);
        c.check(thirst.getThirstF() >= thirst.thirstCap, "Refill Thirst Bar fills the thirst bar (" + thirst.getThirstF() + ")");
        float full = thirst.getThirstF();
        for (int i = 0; i < 200; i++) NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(owner));
        c.check(thirst.getThirstF() == full, "No Water Draining: the bar doesn't go down (" + thirst.getThirstF() + ")");
        owner.removeEffect(com.alexh.capitalism.core.ModEffects.NO_WATER_DRAINING);
        for (int i = 0; i < 200; i++) NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(owner));
        c.check(thirst.getThirstF() < full, "without it the bar goes down again");
        camel.discard();
    }

    // Training Feed (TrainingFeed): an animal on the pet list follows whoever feeds it, one feed each; not a riding
    // animal or someone else's pet. The NPC Cook sells it 5 for a gold ingot. And the fish eaters eat plain fish.
    private static void trainingFeed(GameTests.Checks c) {
        Animal wild = pig(c);
        ItemStack feed = new ItemStack(ModItems.TRAINING_FEED.get(), 3);
        ServerPlayer cook = c.player();
        cook.getData(ModAttachments.JOBS.get()).add(Job.COOK);
        use(cook, wild, feed);
        c.check(feed.getCount() == 3 && !TrainingFeed.following(wild, cook), "only Pet Trainers can use Training Feed (a Cook can't)");
        ServerPlayer feeder = trainer(c, 0);
        use(feeder, wild, feed);
        c.check(feed.getCount() == 2, "feeding an animal uses one Training Feed");
        c.check(TrainingFeed.following(wild, feeder), "the fed animal follows you");
        c.check(wild.goalSelector.getAvailableGoals().stream().anyMatch(g -> g.getPriority() == 0 && g.getGoal().getClass().getSimpleName().equals("FollowFeederGoal")),
            "a goal animal (pig) gets the follow goal first, ahead of its wandering");
        // a brain animal (goat): walks to the feeder (its walk target, pathfinding round things)
        Animal goat = (Animal) EntityTypes.GOAT.create(c.level(), EntitySpawnReason.MOB_SUMMONED);
        Vec3 far = feeder.position().add(8, 0, 0);
        goat.snapTo(far.x, far.y, far.z, 0, 0);
        c.level().addFreshEntity(goat);
        use(feeder, goat, feed);
        NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(goat));
        boolean walking = goat.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET)
            .map(target -> target.getTarget().currentPosition().distanceTo(feeder.position()) < 0.5).orElse(false);
        c.check(walking, "a brain animal (goat) walks to the feeder");
        feed.grow(1);
        goat.discard();

        Animal horse = (Animal) EntityTypes.HORSE.create(c.level(), EntitySpawnReason.MOB_SUMMONED);
        Vec3 at = c.at(4.5, 2, 4.5);
        horse.snapTo(at.x, at.y, at.z, 0, 0);
        c.level().addFreshEntity(horse);
        use(feeder, horse, feed);
        c.check(feed.getCount() == 2 && !TrainingFeed.following(horse, feeder), "a horse isn't on the pet list: no feed used");

        ServerPlayer owner = c.player();
        Animal owned = claimedPig(c, owner);
        use(feeder, owned, feed);
        c.check(feed.getCount() == 2 && !TrainingFeed.following(owned, feeder), "someone else's pet won't take your feed");

        NpcShops.Offer offer = NpcShops.all(Job.COOK).stream().filter(o -> o.item().get() == ModItems.TRAINING_FEED.get()).findFirst().orElse(null);
        c.check(offer != null && offer.stack().getCount() == 5, "the NPC Cook sells Training Feed in fives");
        c.check(offer != null && com.alexh.capitalism.economy.NpcPricing.npcPrice(offer.fairNuggets(), feeder) == 9, "five cost a gold ingot at the NPC");

        Animal axolotl = (Animal) EntityTypes.AXOLOTL.create(c.level(), EntitySpawnReason.MOB_SUMMONED);
        c.check(PetTraits.eats(axolotl, new ItemStack(Items.TROPICAL_FISH)) && !PetTraits.eats(axolotl, new ItemStack(Items.TROPICAL_FISH_BUCKET)),
            "an axolotl eats Tropical Fish, not the bucket");
        Animal nautilus = (Animal) EntityTypes.NAUTILUS.create(c.level(), EntitySpawnReason.MOB_SUMMONED);
        c.check(PetTraits.eats(nautilus, new ItemStack(Items.PUFFERFISH)) && !PetTraits.eats(nautilus, new ItemStack(Items.PUFFERFISH_BUCKET)),
            "a nautilus eats Pufferfish, not the bucket");
        wild.discard();
        horse.discard();
        owned.discard();
    }

    // Hunger and thirst bars as big as the animal (like its hearts), emptying in the same time whatever their size
    private static void barSize(GameTests.Checks c) {
        for (Object[] row : new Object[][]{{EntityTypes.CHICKEN, 6f}, {EntityTypes.PIG, 10f}, {EntityTypes.POLAR_BEAR, 30f}}) {
            Animal animal = (Animal) ((net.minecraft.world.entity.EntityType<?>) row[0]).create(c.level(), EntitySpawnReason.MOB_SUMMONED);
            Vec3 at = c.at(2.5, 2, 2.5);
            animal.snapTo(at.x, at.y, at.z, 0, 0);
            c.level().addFreshEntity(animal);
            use(trainer(c, 100), animal, new ItemStack(ModItems.TRAINING_STICK.get()));
            PetData data = Pets.data(animal);
            c.check(data.max() == (float) row[1] && data.hunger() == data.max() && data.thirst() == data.max(),
                animal.getType().getDescription().getString() + ": bars of " + row[1] + " (got " + data.max() + "), starting full");
            float before = data.hunger();
            for (int i = 0; i < 60; i++) Pets.second(animal);
            c.near(before - data.hunger(), 60 * Pets.HUNGER_PER_SECOND, 1e-4,
                animal.getType().getDescription().getString() + ": loses as many points a second as any other pet");
            animal.discard();
        }
    }

    // The Back slot and the Animal Bjorn: carrying a pet (it keeps everything), not someone else's, not big animals, the
    // worn shortcuts, and dropping it on death
    private static void bjorn(GameTests.Checks c) {
        ServerPlayer owner = c.player();
        c.check(owner.inventoryMenu.slots.size() > 46 && owner.inventoryMenu.slots.get(46) instanceof com.alexh.capitalism.back.BackSlot,
            "the inventory has a Back slot (slot 46)");
        net.minecraft.world.inventory.Slot back = owner.inventoryMenu.slots.get(46);
        c.check(back.mayPlace(new ItemStack(ModItems.ANIMAL_BJORN.get())) && !back.mayPlace(new ItemStack(Items.DIAMOND_CHESTPLATE)),
            "only things for your back go in the Back slot");

        // a pet climbs in: gone from the world, everything kept
        Animal pig = claimedPig(c, owner);
        Pets.rename(owner, pig.getId(), "Bacon");
        Pets.data(pig).setHunger(12);
        java.util.UUID id = pig.getUUID();
        ItemStack bjorn = new ItemStack(ModItems.ANIMAL_BJORN.get());
        use(owner, pig, bjorn);
        c.check(pig.isRemoved() && AnimalBjornItem.carried(bjorn) != null, "the pet climbs into the Animal Bjorn");
        c.check(bjorn.getHoverName().getString().equals("Animal Bjorn (Bacon)"), "the bjorn says who's in it (" + bjorn.getHoverName().getString() + ")");

        // ... and out again, the same pet
        BlockPos ground = c.helper.absolutePos(new BlockPos(2, 1, 2));
        c.check(AnimalBjornItem.letOut(bjorn, owner, c.level(), ground.above()), "it climbs out");
        net.minecraft.world.entity.Entity out = c.level().getEntity(id);
        c.check(out instanceof Animal back2 && Pets.isPet(back2) && Pets.data(back2).isOwner(owner.getUUID())
                && Pets.name(back2).getString().equals("Bacon") && Math.abs(Pets.data(back2).hunger() - 12) < 0.1,
            "out comes the same pet: its owner, name and hunger");
        c.check(AnimalBjornItem.carried(bjorn) == null, "the bjorn is empty again");

        // carried for 10 minutes: it comes out as hungry and thirsty as it would be
        if (out instanceof Animal pet) {
            Pets.data(pet).setHunger(PetData.MAX);
            Pets.data(pet).setThirst(PetData.MAX);
            AnimalBjornItem.pickUp(bjorn, owner, pet);
            CarriedAnimal in = AnimalBjornItem.carried(bjorn);
            bjorn.set(com.alexh.capitalism.core.ModComponents.CARRIED_ANIMAL.get(), new CarriedAnimal(in.type(), in.data(), in.name(), in.since() - 20 * 600, in.played() - 20 * 600));
            AnimalBjornItem.letOut(bjorn, owner, c.level(), ground.above());
            if (c.level().getEntity(id) instanceof Animal again) {
                c.near(Pets.data(again).hunger(), PetData.MAX - 600 * Pets.HUNGER_PER_SECOND, 0.01, "10 minutes in the bjorn: hungrier");
                c.near(Pets.data(again).thirst(), PetData.MAX - 600 * Pets.THIRST_PER_SECOND, 0.01, "10 minutes in the bjorn: thirstier");
                out = again;
            } else {
                c.fail("the pet came back out after 10 minutes");
            }
        }

        // a Pet Trainer can't put an animal in one
        ServerPlayer trainer = trainer(c, 0);
        Animal shopPig = pig(c);
        ItemStack trainers = new ItemStack(ModItems.ANIMAL_BJORN.get());
        use(trainer, shopPig, trainers);
        c.check(AnimalBjornItem.carried(trainers) == null && !shopPig.isRemoved(), "a Pet Trainer can't carry an animal in a bjorn");

        // not someone else's pet, not a big animal
        ServerPlayer stranger = c.player();
        ItemStack theirs = new ItemStack(ModItems.ANIMAL_BJORN.get());
        if (out instanceof Animal pet) use(stranger, pet, theirs);
        c.check(AnimalBjornItem.carried(theirs) == null, "someone else's pet won't go in your bjorn");
        Animal sniffer = (Animal) EntityTypes.SNIFFER.create(c.level(), EntitySpawnReason.MOB_SUMMONED);
        Vec3 at = c.at(2.5, 2, 2.5);
        sniffer.snapTo(at.x, at.y, at.z, 0, 0);
        c.level().addFreshEntity(sniffer);
        use(stranger, sniffer, theirs);
        c.check(AnimalBjornItem.carried(theirs) == null && !sniffer.isRemoved(), "a wild animal won't go in: only your own pet");
        use(trainer(c, 100), sniffer, new ItemStack(ModItems.TRAINING_STICK.get()));
        use(stranger, sniffer, new ItemStack(ModItems.OWNERSHIP_TAG.get()));
        use(stranger, sniffer, theirs);
        c.check(AnimalBjornItem.carried(theirs) == null && !sniffer.isRemoved(), "a Sniffer is too big for the bjorn");
        sniffer.discard();

        // worn on your back: sneak + right-click an animal with an empty hand puts it in, sneak + right-click the ground lets it out
        ServerPlayer wearer = c.player();
        com.alexh.capitalism.back.Back.set(wearer, new ItemStack(ModItems.ANIMAL_BJORN.get()));
        wearer.setShiftKeyDown(true);
        Animal chicken = (Animal) EntityTypes.CHICKEN.create(c.level(), EntitySpawnReason.MOB_SUMMONED);
        chicken.snapTo(at.x, at.y, at.z, 0, 0);
        c.level().addFreshEntity(chicken);
        wearer.setShiftKeyDown(false);
        use(trainer(c, 100), chicken, new ItemStack(ModItems.TRAINING_STICK.get()));
        use(wearer, chicken, new ItemStack(ModItems.OWNERSHIP_TAG.get()));
        wearer.setShiftKeyDown(true);
        wearer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(wearer, InteractionHand.MAIN_HAND, chicken, chicken.position()));
        c.check(chicken.isRemoved() && AnimalBjornItem.carried(com.alexh.capitalism.back.Back.get(wearer)) != null,
            "sneak + right-click: the chicken climbs into the bjorn on your back");
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(wearer, InteractionHand.MAIN_HAND, ground, new BlockHitResult(Vec3.atCenterOf(ground), Direction.UP, ground, false)));
        c.check(AnimalBjornItem.carried(com.alexh.capitalism.back.Back.get(wearer)) == null, "sneak + right-click the ground: it climbs out");

        // dying drops what's on your back
        com.alexh.capitalism.back.Back.set(wearer, new ItemStack(ModItems.ANIMAL_BJORN.get()));
        NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.living.LivingDeathEvent(wearer, c.level().damageSources().generic()));
        c.check(com.alexh.capitalism.back.Back.get(wearer).isEmpty(), "dying drops what's on your back");
    }
}
