package com.alexh.capitalism.player;

import com.alexh.capitalism.core.CapitalismConfig;
import com.alexh.capitalism.core.GoldenBonus;
import com.alexh.capitalism.core.ModAttachments;
import com.alexh.capitalism.core.ModEffects;
import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.block.LiquorType;
import com.alexh.capitalism.gametest.GameTests;
import com.alexh.capitalism.npc.NpcShops;
import com.alexh.capitalism.occupation.Job;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.UseRemainder;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

// Automated checks for the buffs from food and drinks (Cook.md, Bartending.md), run by the game test server
// (gametest/GameTests, `gradlew runGameTestServer`):
//  - every meal / drink with effects, eaten or drunk the real way: it gives exactly the effects it lists (level and
//    time) and hands back its bowl / bottle / stick
//  - gold bars: cocktails and shots overflow into gold water, Gourmet food into gold drumsticks
//  - the special effects: Scholar's Focus, Green Thumb Aura, Ender Leap, Luminescent Aura, Magma Trail, Phase Step,
//    Overworld Mojito, Phantom's Nightcap, Zombie's Hangover Cure (and Cooks eating raw food), Berserker's Rage,
//    Dolphin's Grace skimming, Life Siphon, Spatial Dislocation, Fortune Boost
//  - the rule that NPC shops never sell anything with effects
// Not testable here: Lava Swimmer (your own client moves you in lava) and Treasure Sight (needs a real shipwreck).
public final class FoodDrinkTests {

    static {
        GameTests.register("food_effects", FoodDrinkTests::itemEffects);
        GameTests.register("food_gold_bars", FoodDrinkTests::goldBars);
        GameTests.register("food_meal_specials", FoodDrinkTests::mealSpecials);
        GameTests.register("food_drink_specials", FoodDrinkTests::drinkSpecials);
        GameTests.register("food_drink_combat", FoodDrinkTests::drinkCombat);
        GameTests.register("food_npc_rule", FoodDrinkTests::npcRule);
    }

    private FoodDrinkTests() {
    }

    // Loaded by GameTests.init (runs the static block above)
    public static void init() {
    }

    private static List<MobEffectInstance> listedEffects(ItemStack stack) {
        List<MobEffectInstance> effects = new ArrayList<>();
        Consumable consumable = stack.get(DataComponents.CONSUMABLE);
        if (consumable == null) return effects;
        for (ConsumeEffect effect : consumable.onConsumeEffects()) {
            if (effect instanceof ApplyStatusEffectsConsumeEffect apply) effects.addAll(apply.effects());
        }
        return effects;
    }

    private static String name(ItemStack stack) {
        return stack.getHoverName().getString();
    }

    // Every one of our items with effects: eat / drink it, get exactly those effects, get the bowl / bottle back
    private static void itemEffects(GameTests.Checks c) {
        int tested = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            if (!BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(com.alexh.capitalism.Capitalism.MODID)) continue;
            ItemStack stack = new ItemStack(item);
            // (raw chicken cuts only have raw chicken's chance of Hunger: not a meal's effect)
            if (com.alexh.capitalism.farmer.MeatCut.isCut(stack)) continue;
            List<MobEffectInstance> listed = listedEffects(stack);
            if (listed.isEmpty()) continue;
            tested++;
            ServerPlayer player = c.player();
            player.getFoodData().setFoodLevel(10);
            UseRemainder remainder = stack.get(DataComponents.USE_REMAINDER);
            ItemStack left = c.consume(player, stack.copy());
            for (MobEffectInstance expected : listed) {
                MobEffectInstance got = player.getEffect(expected.getEffect());
                String what = name(stack) + " gives " + expected.getEffect().value().getDisplayName().getString()
                    + (expected.getAmplifier() > 0 ? " " + (expected.getAmplifier() + 1) : "");
                if (got == null) {
                    c.fail(what + ": not there after eating / drinking");
                    continue;
                }
                c.check(got.getAmplifier() == expected.getAmplifier(), what + ": level " + (expected.getAmplifier() + 1) + " (got " + (got.getAmplifier() + 1) + ")");
                c.check(got.getDuration() == expected.getDuration(), what + ": " + expected.getDuration() / 20 + " s (got " + got.getDuration() / 20 + " s)");
            }
            if (remainder != null) {
                Item back = remainder.convertInto().item().value();
                c.check(left.is(back), name(stack) + " hands back " + name(new ItemStack(back)) + " (got " + name(left) + ")");
            }
        }
        c.check(tested == 20, "found all 20 meals and drinks with effects (got " + tested + ")");
    }

    // Overflow: cocktails / shots into gold water, Gourmet food into gold drumsticks (by hand: half)
    private static void goldBars(GameTests.Checks c) {
        for (var drink : List.of(ModItems.GLOWBERRY_GOLD_RUSH.get(), ModItems.CLASSIC_OLD_FASHIONED.get(), ModItems.SHOTS.get(LiquorType.TEQUILA).get())) {
            ItemStack stack = new ItemStack(drink);
            int water = GoldenBonus.of(stack).water();
            ServerPlayer full = c.player();
            ThirstData data = full.getData(ModAttachments.THIRST.get());
            c.consume(full, stack.copy());
            c.check(data.getGoldThirstInt() == water, name(stack) + " at full thirst: " + water + " gold water (got " + data.getGoldThirstInt() + ")");
            ServerPlayer thirsty = c.player();
            ThirstData thirst = thirsty.getData(ModAttachments.THIRST.get());
            thirst.drain(6);
            c.consume(thirsty, stack.copy());
            int intoBar = Math.min(6, water);
            c.check(thirst.getThirstInt() == 14 + intoBar && thirst.getGoldThirstInt() == water - intoBar,
                name(stack) + " 6 short: bar " + (14 + intoBar) + ", gold " + (water - intoBar) + " (got " + thirst.getThirstInt() + ", " + thirst.getGoldThirstInt() + ")");
        }
        double hand = CapitalismConfig.GOURMET_GOLD_HAND.get();
        ItemStack steak = new ItemStack(ModItems.STEAK_ROLL.get());
        int hunger = steak.get(DataComponents.FOOD).nutrition();
        ServerPlayer full = c.player();
        full.getFoodData().setFoodLevel(20);
        c.consume(full, steak.copy());
        int gold = full.getData(ModAttachments.THIRST.get()).getGoldFood();
        c.check(gold == (int) Math.round(hunger * hand), "Steak Roll when full: " + Math.round(hunger * hand) + " gold drumstick points (got " + gold + ")");
        // gold rush (Cook.md): gold-only Gourmet food gives 1.5 s of fast healing, not the meal's saturation...
        ServerPlayer rush = c.player();
        ThirstData rushData = rush.getData(ModAttachments.THIRST.get());
        rush.getFoodData().setFoodLevel(20);
        rush.getFoodData().setSaturation(0f);
        c.consume(rush, steak.copy());
        c.check(rush.getFoodData().getSaturationLevel() == ThirstData.GOLD_RUSH_SATURATION && rushData.goldRushTicks == ThirstData.GOLD_RUSH_TICKS,
            "gold-only Steak Roll: a gold rush (saturation " + rush.getFoodData().getSaturationLevel() + ", " + rushData.goldRushTicks + " ticks)");
        for (int i = 0; i < ThirstData.GOLD_RUSH_TICKS; i++) rushData.tickGoldRush(rush.getFoodData());
        c.check(rush.getFoodData().getSaturationLevel() == 0f, "...and after 1.5 s its saturation is gone (got " + rush.getFoodData().getSaturationLevel() + ")");
        // ...and doesn't stack with saturation from a normal meal
        ServerPlayer fed = c.player();
        fed.getFoodData().setFoodLevel(20);
        fed.getFoodData().setSaturation(3f);
        c.consume(fed, steak.copy());
        c.check(fed.getFoodData().getSaturationLevel() == 3f && fed.getData(ModAttachments.THIRST.get()).goldRushTicks == 0,
            "gold-only Steak Roll with saturation left: no change, no rush (got " + fed.getFoodData().getSaturationLevel() + ")");
        ServerPlayer half = c.player();
        half.getFoodData().setFoodLevel(10);
        c.consume(half, steak.copy());
        int over = 10 + hunger - 20;
        c.check(half.getFoodData().getFoodLevel() == 20, "Steak Roll at 10: the bar fills (got " + half.getFoodData().getFoodLevel() + ")");
        c.check(half.getData(ModAttachments.THIRST.get()).getGoldFood() == (int) Math.round(over * hand),
            "Steak Roll at 10: the rest is gold (" + Math.round(over * hand) + ", got " + half.getData(ModAttachments.THIRST.get()).getGoldFood() + ")");
        ServerPlayer plain = c.player();
        plain.getFoodData().setFoodLevel(16);
        c.consume(plain, new ItemStack(Items.BREAD));
        c.check(plain.getData(ModAttachments.THIRST.get()).getGoldFood() == 0, "plain bread never makes gold");
    }

    // Scholar's Focus, Green Thumb Aura, Ender Leap
    private static void mealSpecials(GameTests.Checks c) {
        Mob zombie = EntityTypes.ZOMBIE.create(c.level(), EntitySpawnReason.MOB_SUMMONED);
        for (boolean focus : new boolean[]{false, true}) {
            ServerPlayer player = c.player();
            if (focus) player.addEffect(new MobEffectInstance(ModEffects.SCHOLARS_FOCUS, 200));
            LivingExperienceDropEvent event = new LivingExperienceDropEvent(zombie, player, 5);
            MealEffects.onExperienceDrop(event);
            c.check(event.getDroppedExperience() == (focus ? 10 : 5), (focus ? "Scholar's Focus" : "no effect") + ": a 5-XP kill drops " + event.getDroppedExperience());
        }

        // Green Thumb: wheat on farmland next to you grows; grass doesn't sprout
        BlockPos farm = BlockPos.containing(c.at(1, 1, 1));
        BlockPos grass = BlockPos.containing(c.at(3, 1, 3));
        for (boolean thumb : new boolean[]{false, true}) {
            c.level().setBlockAndUpdate(farm, Blocks.FARMLAND.defaultBlockState());
            c.level().setBlockAndUpdate(farm.above(), Blocks.WHEAT.defaultBlockState());
            c.level().setBlockAndUpdate(grass, Blocks.GRASS_BLOCK.defaultBlockState());
            c.level().setBlockAndUpdate(grass.above(), Blocks.SHORT_GRASS.defaultBlockState());
            ServerPlayer farmer = c.player();
            if (thumb) farmer.addEffect(new MobEffectInstance(ModEffects.GREEN_THUMB, 2000));
            for (int second = 1; second <= 40; second++) {
                farmer.tickCount = second * 20;
                MealEffects.onServerPlayerTick(new PlayerTickEvent.Post(farmer));
            }
            BlockState wheat = c.level().getBlockState(farm.above());
            int age = wheat.getBlock() instanceof CropBlock crop ? crop.getAge(wheat) : -1;
            if (thumb) c.check(age == 7, "Green Thumb Aura: wheat next to you is ripe in 40 seconds (age " + age + ")");
            else c.check(age == 0, "no Green Thumb: wheat doesn't grow on its own here (age " + age + ")");
            c.check(c.level().getBlockState(grass.above()).is(Blocks.SHORT_GRASS), (thumb ? "Green Thumb" : "no effect") + ": grass isn't bone-mealed");
        }

        // Ender Leap: blink forward, half a hunger point, once a second; not without food or the effect
        for (int setup = 0; setup < 3; setup++) {
            ServerPlayer leaper = c.player();
            if (setup > 0) leaper.addEffect(new MobEffectInstance(ModEffects.ENDER_LEAP, 2000));
            leaper.getFoodData().setFoodLevel(setup == 2 ? 0 : 20);
            leaper.setYRot(0);
            // up in open air, away from blocks other tests put down
            Vec3 sky = c.at(2.5, 40, 2.5);
            leaper.snapTo(sky.x, sky.y, sky.z, 0, 0);
            Vec3 before = leaper.position();
            float exhausted = exhaustion(leaper);
            MealEffects.enderLeap(leaper);
            double moved = leaper.position().distanceTo(before);
            if (setup == 1) {
                c.check(moved > 6, "Ender Leap: blinked forward " + String.format("%.1f", moved) + " blocks");
                c.near(exhaustion(leaper) - exhausted, 2.0, 1e-4, "Ender Leap costs half a hunger point");
                Vec3 landed = leaper.position();
                MealEffects.enderLeap(leaper);
                c.check(leaper.position().distanceTo(landed) < 1e-6, "Ender Leap: not again right away (cooldown)");
            } else {
                c.check(moved < 1e-6, (setup == 0 ? "no effect" : "no food") + ": no leap");
            }
        }
    }

    // Luminescent Aura, Magma Trail, Phase Step, Overworld Mojito, Phantom's Nightcap, Hangover Cure, Berserker's
    // Rage, Dolphin's Grace skimming
    private static void drinkSpecials(GameTests.Checks c) {
        // Luminescent Aura: a light block at your head, gone when the effect ends
        ServerPlayer glow = c.player();
        glow.addEffect(new MobEffectInstance(ModEffects.LUMINESCENT_AURA, 200));
        glow.tickCount = 2;
        DrinkEffects.onPlayerTick(new PlayerTickEvent.Post(glow));
        BlockPos head = BlockPos.containing(glow.getEyePosition());
        c.check(c.level().getBlockState(head).is(Blocks.LIGHT), "Luminescent Aura: a light at your head");
        glow.removeEffect(ModEffects.LUMINESCENT_AURA);
        glow.tickCount = 4;
        DrinkEffects.onPlayerTick(new PlayerTickEvent.Post(glow));
        c.check(c.level().getBlockState(head).isAir(), "Luminescent Aura: the light goes when the effect ends");

        // Magma Trail: flames where you walk burn a zombie and another player, not you
        ServerPlayer walker = c.player();
        walker.addEffect(new MobEffectInstance(ModEffects.MAGMA_TRAIL, 200));
        walker.setOnGround(true);
        Mob zombie = EntityTypes.ZOMBIE.create(c.level(), EntitySpawnReason.MOB_SUMMONED);
        zombie.snapTo(walker.getX(), walker.getY(), walker.getZ(), 0, 0);
        c.level().addFreshEntity(zombie);
        ServerPlayer other = c.player();
        float zombieHealth = zombie.getHealth();
        float otherHealth = other.getHealth();
        float walkerHealth = walker.getHealth();
        walker.tickCount = 4;
        DrinkEffects.onPlayerTick(new PlayerTickEvent.Post(walker));
        c.check(zombie.getHealth() < zombieHealth && zombie.isOnFire(), "Magma Trail: a zombie in your trail burns");
        c.check(other.getHealth() < otherHealth, "Magma Trail: another player in your trail is hurt");
        c.check(walker.getHealth() == walkerHealth && !walker.isOnFire(), "Magma Trail: your own trail doesn't burn you");
        zombie.discard();

        // Phase Step (Ender Tele-Tini): back to where you were 10 seconds ago, then Slow Falling
        ServerPlayer phaser = c.player();
        Vec3 start = phaser.position();
        phaser.tickCount = 20;
        DrinkEffects.onPlayerTick(new PlayerTickEvent.Post(phaser));
        Vec3 away = c.at(4.5, 2, 4.5);
        phaser.teleportTo(away.x, away.y, away.z);
        phaser.tickCount = 40;
        DrinkEffects.onPlayerTick(new PlayerTickEvent.Post(phaser));
        c.consume(phaser, new ItemStack(ModItems.ENDER_TELE_TINI.get()));
        c.check(phaser.position().distanceTo(start) < 0.01, "Ender Tele-Tini: back to where you stood before");

        // Overworld Mojito: every bad effect gone, good ones stay
        ServerPlayer sick = c.player();
        sick.addEffect(new MobEffectInstance(MobEffects.POISON, 400));
        sick.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 400));
        sick.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400));
        c.consume(sick, new ItemStack(ModItems.OVERWORLD_MOJITO.get()));
        c.check(!sick.hasEffect(MobEffects.POISON) && !sick.hasEffect(MobEffects.WEAKNESS), "Overworld Mojito: Poison and Weakness gone");
        c.check(sick.hasEffect(MobEffects.NIGHT_VISION), "Overworld Mojito: good effects stay (Night Vision)");

        // Phantom's Nightcap: rested (no phantoms coming, no Tired), and phantoms can't target you
        ServerPlayer tired = c.player();
        tired.getStats().setValue(tired, Stats.CUSTOM.get(Stats.TIME_SINCE_REST), 100000);
        c.consume(tired, new ItemStack(ModItems.PHANTOMS_NIGHTCAP.get()));
        c.check(tired.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST)) == 0, "Phantom's Nightcap: counts as rested");
        Mob phantom = EntityTypes.PHANTOM.create(c.level(), EntitySpawnReason.MOB_SUMMONED);
        phantom.snapTo(tired.getX(), tired.getY() + 3, tired.getZ(), 0, 0);
        c.level().addFreshEntity(phantom);
        phantom.setTarget(tired);
        c.check(phantom.getTarget() != tired, "Phantom's Nightcap: a phantom can't target you");
        phantom.discard();

        // Zombie's Hangover Cure: Hunger gone and can't come back; a Cook can eat raw meat while it lasts
        ServerPlayer hungover = c.player();
        hungover.addEffect(new MobEffectInstance(MobEffects.HUNGER, 400));
        hungover.getData(ModAttachments.JOBS.get()).add(Job.COOK);
        hungover.getFoodData().setFoodLevel(10);
        c.consume(hungover, new ItemStack(Items.BEEF));
        c.check(hungover.getFoodData().getFoodLevel() == 10, "a Cook can't eat raw beef");
        c.consume(hungover, new ItemStack(ModItems.ZOMBIES_HANGOVER_CURE.get()));
        c.check(!hungover.hasEffect(MobEffects.HUNGER), "Zombie's Hangover Cure: Hunger gone");
        hungover.addEffect(new MobEffectInstance(MobEffects.HUNGER, 400));
        c.check(!hungover.hasEffect(MobEffects.HUNGER), "Zombie's Hangover Cure: no new Hunger");
        int food = hungover.getFoodData().getFoodLevel();
        c.consume(hungover, new ItemStack(Items.BEEF));
        c.check(hungover.getFoodData().getFoodLevel() > food, "Zombie's Hangover Cure: a Cook can eat raw beef now");

        // Berserker's Rage: can't be knocked back
        for (boolean rage : new boolean[]{false, true}) {
            ServerPlayer brawler = c.player();
            if (rage) brawler.addEffect(new MobEffectInstance(ModEffects.BERSERKERS_RAGE, 200));
            c.near(brawler.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), rage ? 1.0 : 0.0, 1e-6, (rage ? "Berserker's Rage" : "no effect") + ": knockback resistance");
            brawler.setDeltaMovement(Vec3.ZERO);
            brawler.knockback(1.0, 1.0, 0.0, c.level().damageSources().generic(), 1.0F);
            c.check((brawler.getDeltaMovement().length() < 1e-6) == rage, (rage ? "Berserker's Rage: not knocked back" : "no effect: knocked back"));
        }

        // Dolphin's Grace over water: the server's "flying" counter is forgiven
        for (boolean grace : new boolean[]{false, true}) {
            ServerPlayer swimmer = c.player();
            BlockPos below = swimmer.blockPosition().below();
            c.level().setBlockAndUpdate(below, Blocks.WATER.defaultBlockState());
            if (grace) swimmer.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 200));
            try {
                Field ticks = net.minecraft.server.network.ServerGamePacketListenerImpl.class.getDeclaredField("aboveGroundTickCount");
                ticks.setAccessible(true);
                ticks.setInt(swimmer.connection, 60);
                swimmer.tickCount = 1;
                DrinkEffects.onPlayerTick(new PlayerTickEvent.Post(swimmer));
                c.check((ticks.getInt(swimmer.connection) == 0) == grace, (grace ? "Dolphin's Grace: skimming water isn't flying" : "no effect: the fly counter stays"));
            } catch (ReflectiveOperationException e) {
                c.fail("can't read the fly counter: " + e);
            }
            c.level().setBlockAndUpdate(below, Blocks.AIR.defaultBlockState());
        }
    }

    // Life Siphon, Spatial Dislocation, Fortune Boost
    private static void drinkCombat(GameTests.Checks c) {
        // Life Siphon: melee hits on hostile mobs heal you for a quarter of the damage; not on animals
        for (int setup = 0; setup < 3; setup++) {
            ServerPlayer vampire = c.player();
            if (setup > 0) vampire.addEffect(new MobEffectInstance(ModEffects.LIFE_SIPHON, 400));
            vampire.setHealth(10);
            LivingEntity target = (setup == 2 ? EntityTypes.PIG : EntityTypes.ZOMBIE).create(c.level(), EntitySpawnReason.MOB_SUMMONED);
            target.snapTo(vampire.getX() + 1, vampire.getY(), vampire.getZ(), 0, 0);
            c.level().addFreshEntity(target);
            float before = target.getHealth();
            target.hurtServer(c.level(), c.level().damageSources().playerAttack(vampire), 8.0F);
            float dealt = before - target.getHealth();
            double healed = vampire.getHealth() - 10;
            if (setup == 1) c.near(healed, dealt * 0.25, 1e-3, "Life Siphon: healed a quarter of " + dealt + " damage to a zombie");
            else c.near(healed, 0, 1e-6, setup == 0 ? "no effect: no healing" : "Life Siphon: no healing from hitting a pig");
            target.discard();
        }

        // Spatial Dislocation: a killing blow is cancelled and you're thrown away; a heavy hit throws you too; small
        // hits don't; not twice in 2 seconds
        BlockPos floor = BlockPos.containing(c.at(2, 0, 2));
        for (int dx = -10; dx <= 10; dx++) {
            for (int dz = -10; dz <= 10; dz++) c.level().setBlockAndUpdate(floor.offset(dx, 0, dz), Blocks.STONE.defaultBlockState());
        }
        Mob attacker = EntityTypes.ZOMBIE.create(c.level(), EntitySpawnReason.MOB_SUMMONED);
        for (boolean fizz : new boolean[]{false, true}) {
            ServerPlayer player = c.player();
            if (fizz) player.addEffect(new MobEffectInstance(ModEffects.SPATIAL_DISLOCATION, 400));
            player.setHealth(4);
            Vec3 before = player.position();
            player.hurtServer(c.level(), c.level().damageSources().mobAttack(attacker), 10.0F);
            if (fizz) {
                c.check(player.isAlive() && player.getHealth() == 4, "Ender Twilight Fizz: a killing blow is cancelled (health " + player.getHealth() + ")");
                c.check(player.position().distanceTo(before) > 0.5, "Ender Twilight Fizz: you're thrown somewhere else");
            } else {
                c.check(player.isDeadOrDying(), "no effect: a 10-damage hit at 4 health kills");
            }
        }
        ServerPlayer heavy = c.player();
        heavy.addEffect(new MobEffectInstance(ModEffects.SPATIAL_DISLOCATION, 400));
        Vec3 spot = heavy.position();
        heavy.hurtServer(c.level(), c.level().damageSources().mobAttack(attacker), 7.0F);
        c.check(heavy.position().distanceTo(spot) > 0.5 && heavy.getHealth() < 20, "Ender Twilight Fizz: a heavy hit hurts and throws you");
        Vec3 landed = heavy.position();
        heavy.setInvulnerableTime(0);
        heavy.hurtServer(c.level(), c.level().damageSources().mobAttack(attacker), 7.0F);
        c.check(heavy.position().distanceTo(landed) < 1e-6, "Ender Twilight Fizz: not again within 2 seconds");
        ServerPlayer light = c.player();
        light.addEffect(new MobEffectInstance(ModEffects.SPATIAL_DISLOCATION, 400));
        Vec3 stood = light.position();
        light.hurtServer(c.level(), c.level().damageSources().mobAttack(attacker), 2.0F);
        c.check(light.position().distanceTo(stood) < 1e-6, "Ender Twilight Fizz: a small hit doesn't throw you");

        // Fortune Boost (Miner's Moonshine): about 1 in 3 ore drops get one more; stone doesn't
        BlockPos ore = BlockPos.containing(c.at(1, 1, 1));
        for (boolean boost : new boolean[]{false, true}) {
            ServerPlayer miner = c.player();
            miner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_PICKAXE));
            if (boost) miner.addEffect(new MobEffectInstance(ModEffects.FORTUNE_BOOST, 20000));
            int breaks = 300;
            int raw = 0;
            for (int i = 0; i < breaks; i++) {
                c.level().setBlockAndUpdate(ore, Blocks.IRON_ORE.defaultBlockState());
                miner.gameMode.destroyBlock(ore);
                raw += collect(c, ore, Items.RAW_IRON);
            }
            c.near(raw / (double) breaks, boost ? 4.0 / 3 : 1.0, boost ? 0.1 : 1e-6, (boost ? "Miner's Moonshine" : "no effect") + ": raw iron per iron ore");
            int cobble = 0;
            for (int i = 0; i < 30; i++) {
                c.level().setBlockAndUpdate(ore, Blocks.STONE.defaultBlockState());
                miner.gameMode.destroyBlock(ore);
                cobble += collect(c, ore, Items.COBBLESTONE);
            }
            c.check(cobble == 30, (boost ? "Miner's Moonshine" : "no effect") + ": stone still gives one cobblestone (" + cobble + " from 30)");
        }
    }

    // Picks up (removes) the dropped items of this kind around a spot; how many there were
    private static int collect(GameTests.Checks c, BlockPos pos, Item item) {
        int count = 0;
        for (ItemEntity drop : c.level().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2))) {
            if (drop.getItem().is(item)) count += drop.getItem().getCount();
            drop.discard();
        }
        return count;
    }

    // NPC shops never sell anything with effects (CLAUDE.md)
    private static void npcRule(GameTests.Checks c) {
        int offers = 0;
        for (Job job : Job.values()) {
            for (NpcShops.Offer offer : NpcShops.all(job)) {
                offers++;
                ItemStack stack = new ItemStack(offer.item().get());
                c.check(listedEffects(stack).isEmpty(), job.title + " NPC sells " + name(stack) + ": no effects");
            }
        }
        c.check(offers > 0, "NPC shops have stock (" + offers + " offers)");
    }

    private static float exhaustion(ServerPlayer player) {
        try {
            Field field = net.minecraft.world.food.FoodData.class.getDeclaredField("exhaustionLevel");
            field.setAccessible(true);
            return field.getFloat(player.getFoodData());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
