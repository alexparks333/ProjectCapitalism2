package com.alexh.capitalism.librarian;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.gametest.GameTests;
import com.alexh.capitalism.core.ModAttachments;
import com.alexh.capitalism.core.ModEffects;
import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.economy.NpcPricing;
import com.alexh.capitalism.occupation.Job;
import com.alexh.capitalism.player.BodyTemperature;
import com.alexh.capitalism.player.WellbeingData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.inventory.ContainerLevelAccess;
import com.alexh.capitalism.weaponsmith.Socketing;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// Automated checks for every skill book (LibrarianBooks.md), run by the game test server (gametest/GameTests,
// `gradlew runGameTestServer`). Each test uses fresh survival test players and checks what a book really does in the
// game: attribute values at level I / II, mining speed on logs and rock, NPC prices, thirst, mobs targeting you, arrow
// damage, meat drops, XP, healing, horse speed, powder snow and ice, sprint hunger, the fishing lure, reading books, and
// the odds of the rolls.
public final class SkillBookTests {

    static {
        GameTests.register("skill_attributes", SkillBookTests::attributes);
        GameTests.register("skill_mining", SkillBookTests::mining);
        GameTests.register("skill_thirst_and_prices", SkillBookTests::thirstAndPrices);
        GameTests.register("skill_winter_survival", SkillBookTests::winterSurvival);
        GameTests.register("body_temperature", SkillBookTests::bodyTemperature);
        GameTests.register("temperature_gear", SkillBookTests::temperatureGear);
        GameTests.register("skill_mobs", SkillBookTests::mobs);
        GameTests.register("skill_combat", SkillBookTests::combat);
        GameTests.register("skill_drops_and_xp", SkillBookTests::dropsAndXp);
        GameTests.register("skill_ticking", SkillBookTests::ticking);
        GameTests.register("skill_movement", SkillBookTests::movement);
        GameTests.register("skill_fishing", SkillBookTests::fishing);
        GameTests.register("skill_reading", SkillBookTests::reading);
        GameTests.register("skill_rolls", SkillBookTests::rolls);
        GameTests.register("skill_lucky_librarian", SkillBookTests::luckyLibrarian);
        GameTests.register("skill_stitching", SkillBookTests::stitching);
    }

    private SkillBookTests() {
    }

    // Loaded by GameTests.init (runs the static block above)
    public static void init() {
    }

    private static void lesson(ServerPlayer player, Skill skill, int level) {
        player.addEffect(new MobEffectInstance(ModEffects.BOOK_LESSONS.get(skill), 2000, level - 1, false, false, false));
    }

    private static void give(ServerPlayer player, Skill skill, int level) {
        if (skill.trait) Traits.learn(player, skill);
        else lesson(player, skill, level);
    }

    // ── The tests ──

    // Every attribute book: the attribute goes up by its amount (twice at level II), and back down when the lesson ends
    private static void attributes(GameTests.Checks c) {
        for (Skill skill : Skill.values()) {
            if (skill.attribute == null) continue;
            int levels = skill.trait ? 1 : 2;
            for (int level = 1; level <= levels; level++) {
                ServerPlayer player = c.player();
                AttributeInstance attribute = player.getAttribute(skill.attribute);
                if (attribute == null) {
                    c.fail(skill.title + ": player has no " + skill.attribute.getRegisteredName());
                    continue;
                }
                double before = attribute.getValue();
                double step = skill.operation == AttributeModifier.Operation.ADD_VALUE ? skill.amount : attribute.getBaseValue() * skill.amount;
                give(player, skill, level);
                c.near(attribute.getValue(), before + step * level, 1e-6, skill.title + (level == 2 ? " II" : "") + ": " + skill.attribute.getRegisteredName());
                if (!skill.trait) {
                    player.removeEffect(ModEffects.BOOK_LESSONS.get(skill));
                    c.near(attribute.getValue(), before, 1e-6, skill.title + ": back to normal when it wears off");
                } else {
                    Traits.apply(player);
                    c.near(attribute.getValue(), before + step, 1e-6, skill.title + ": applying again doesn't stack");
                    c.check(Traits.of(player).has(skill), skill.title + ": remembered as learned");
                    // dying forgets it (the traits aren't copied on death): its bonus comes off at respawn
                    player.setData(com.alexh.capitalism.core.ModAttachments.TRAITS.get(), new TraitData());
                    Traits.apply(player);
                    c.near(attribute.getValue(), before, 1e-6, skill.title + ": gone once it's forgotten (dying)");
                }
            }
        }
        // Iron Constitution is gone: an old +1 heart is taken back off
        ServerPlayer player = c.player();
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        double normal = health.getValue();
        health.addPermanentModifier(new AttributeModifier(Identifier.fromNamespaceAndPath(Capitalism.MODID, "trait_iron_constitution"), 2.0, AttributeModifier.Operation.ADD_VALUE));
        Traits.apply(player);
        c.near(health.getValue(), normal, 1e-6, "Iron Constitution's old heart is removed");

        // Milk (or a Totem: anything that clears every effect) takes potions away but not the lessons you've read
        ServerPlayer milky = c.player();
        lesson(milky, Skill.RUNNING_CLUB, 2);
        lesson(milky, Skill.FIRST_AID_BASICS, 1);
        milky.addEffect(new MobEffectInstance(MobEffects.SPEED, 2000));
        new ItemStack(Items.MILK_BUCKET).finishUsingItem(c.level(), milky);
        c.check(!milky.hasEffect(MobEffects.SPEED), "milk clears a potion");
        c.check(Skills.level(milky, Skill.RUNNING_CLUB) == 2 && Skills.level(milky, Skill.FIRST_AID_BASICS) == 1, "milk leaves lessons alone");
        milky.removeAllEffects();
        c.check(Skills.has(milky, Skill.RUNNING_CLUB), "clearing all effects leaves lessons alone");
        milky.removeEffect(ModEffects.BOOK_LESSONS.get(Skill.RUNNING_CLUB));
        c.check(!Skills.has(milky, Skill.RUNNING_CLUB), "a lesson can still be removed on its own");
    }

    // Lumberjack's Log on logs, Stonemason's Guide on rock (not on dirt), Mining Monthly / Born Miner on everything
    private static void mining(GameTests.Checks c) {
        BlockState log = Blocks.OAK_LOG.defaultBlockState();
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState deepslate = Blocks.DEEPSLATE.defaultBlockState();
        BlockState cobble = Blocks.COBBLESTONE.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        for (int level = 1; level <= 2; level++) {
            ServerPlayer plain = c.player();
            ServerPlayer reader = c.player();
            lesson(reader, Skill.LUMBERJACKS_LOG, level);
            c.near(reader.getDestroySpeed(log, null) / plain.getDestroySpeed(log, null), 1 + 0.25 * level, 1e-4, "Lumberjack's Log " + level + " on logs");
            c.near(reader.getDestroySpeed(stone, null) / plain.getDestroySpeed(stone, null), 1, 1e-4, "Lumberjack's Log " + level + " doesn't touch stone");
            ServerPlayer mason = c.player();
            lesson(mason, Skill.STONEMASONS_GUIDE, level);
            for (BlockState rock : List.of(stone, deepslate, cobble)) {
                c.near(mason.getDestroySpeed(rock, null) / plain.getDestroySpeed(rock, null), 1 + 0.25 * level, 1e-4,
                    "Stonemason's Guide " + level + " on " + rock.getBlock().getName().getString());
            }
            c.near(mason.getDestroySpeed(dirt, null) / plain.getDestroySpeed(dirt, null), 1, 1e-4, "Stonemason's Guide " + level + " doesn't touch dirt");
            ServerPlayer monthly = c.player();
            lesson(monthly, Skill.MINING_MONTHLY, level);
            c.near(monthly.getDestroySpeed(dirt, null) / plain.getDestroySpeed(dirt, null), 1 + 0.15 * level, 1e-4, "Mining Monthly " + level + " on dirt");
        }
        ServerPlayer plain = c.player();
        ServerPlayer miner = c.player();
        Traits.learn(miner, Skill.BORN_MINER);
        c.near(miner.getDestroySpeed(stone, null) / plain.getDestroySpeed(stone, null), 1.10, 1e-4, "Born Miner on stone");
    }

    // Water Rationing / Camel slow thirst; Merchant's Ledger takes 7.5% / 15% off NPC prices
    private static void thirstAndPrices(GameTests.Checks c) {
        ServerPlayer plain = c.player();
        c.near(Skills.thirstDrainMultiplier(plain), 1.0, 1e-6, "no book: thirst drains normally");
        ServerPlayer one = c.player();
        lesson(one, Skill.WATER_RATIONING, 1);
        c.near(Skills.thirstDrainMultiplier(one), 0.75, 1e-6, "Water Rationing I");
        ServerPlayer two = c.player();
        lesson(two, Skill.WATER_RATIONING, 2);
        c.near(Skills.thirstDrainMultiplier(two), 0.5, 1e-6, "Water Rationing II");
        ServerPlayer camel = c.player();
        Traits.learn(camel, Skill.CAMEL);
        c.near(Skills.thirstDrainMultiplier(camel), 0.75, 1e-6, "Camel");
        lesson(camel, Skill.WATER_RATIONING, 1);
        c.near(Skills.thirstDrainMultiplier(camel), 0.5625, 1e-6, "Camel + Water Rationing I");

        c.check(NpcPricing.npcPrice(100, plain) == 150, "no book: 100 nuggets costs 150 at an NPC (got " + NpcPricing.npcPrice(100, plain) + ")");
        ServerPlayer ledger = c.player();
        lesson(ledger, Skill.MERCHANTS_LEDGER, 1);
        c.near(NpcPricing.discount(ledger), 0.075, 1e-9, "Merchant's Ledger I discount");
        c.check(NpcPricing.npcPrice(100, ledger) == 139, "Merchant's Ledger I: 139 (got " + NpcPricing.npcPrice(100, ledger) + ")");
        ServerPlayer ledger2 = c.player();
        lesson(ledger2, Skill.MERCHANTS_LEDGER, 2);
        c.near(NpcPricing.discount(ledger2), 0.15, 1e-9, "Merchant's Ledger II discount");
        c.check(NpcPricing.npcPrice(100, ledger2) == 128, "Merchant's Ledger II: 128 (got " + NpcPricing.npcPrice(100, ledger2) + ")");
    }

    // Winter Survival: the cold can't touch you (stuck in powder snow: the target stays at 70%, not 0%)
    private static void winterSurvival(GameTests.Checks c) {
        ServerPlayer cold = c.player();
        cold.setTicksFrozen(20);
        BodyTemperature.Climate freezing = BodyTemperature.climate(cold);
        c.check(freezing.target() == 0f && freezing.fast(), "powder snow without a book: body heat heads for 0%, fast (got " + freezing.target() + ")");
        ServerPlayer warm = c.player();
        warm.setTicksFrozen(20);
        Traits.learn(warm, Skill.WINTER_SURVIVAL);
        BodyTemperature.Climate safe = BodyTemperature.climate(warm);
        c.check(safe.target() >= BodyTemperature.NORMAL && !safe.fast() && (safe.reasons() & BodyTemperature.R_WINTER_SURVIVAL) != 0,
            "Winter Survival: powder snow can't chill you (target " + safe.target() + ", not 0%)");
    }

    // Leather, wet leather, the Ice / Blaze sockets, Hot Pepper, Used To Hell, Ice In The Pants and A Fresh Start
    private static void temperatureGear(GameTests.Checks c) {
        float rain = BodyTemperature.RAIN_TARGET;
        // leather: each piece keeps a quarter of the cold off, all 4 keep it all off; wet leather does nothing
        c.check(BodyTemperature.leatherCold(rain, 0, false, false) == rain, "no leather: rain pulls to 25%");
        c.near(BodyTemperature.leatherCold(rain, 1, false, false), rain + (70 - rain) / 4f, 1e-4, "1 leather piece: a quarter of the cold off");
        c.near(BodyTemperature.leatherCold(rain, 2, false, false), rain + (70 - rain) / 2f, 1e-4, "2 leather pieces: half the cold off");
        c.check(BodyTemperature.leatherCold(rain, 4, false, false) == 70f, "full leather: no cold gets through");
        c.check(BodyTemperature.leatherCold(BodyTemperature.COLD_WATER_TARGET, 4, true, false) == BodyTemperature.COLD_WATER_TARGET, "wet leather: no help");
        c.check(BodyTemperature.leatherCold(0f, 4, false, true) == 20f, "powder snow: 5% per leather piece");
        // leather somewhere hot: each piece a quarter of the way to burning
        float sun = BodyTemperature.DESERT_SUN_TARGET;
        c.check(BodyTemperature.leatherHot(sun, 4) == 100f, "full leather in the desert sun: burning (100%)");
        c.near(BodyTemperature.leatherHot(sun, 2), sun + (100 - sun) / 2f, 1e-4, "2 leather pieces in the sun: halfway to burning");

        // powder snow: Hot Pepper keeps it off, so does a Blaze socket; an Ice socket doesn't
        ServerPlayer pepper = c.player();
        pepper.setTicksFrozen(20);
        lesson(pepper, Skill.HOT_PEPPER, 1);
        BodyTemperature.Climate peppered = BodyTemperature.climate(pepper);
        c.check(peppered.target() >= 70f && (peppered.reasons() & BodyTemperature.R_HOT_PEPPER) != 0, "Hot Pepper: powder snow can't chill you (target " + peppered.target() + ")");
        ServerPlayer chili = c.player();
        chili.setTicksFrozen(20);
        chili.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.alexh.capitalism.core.ModEffects.WARMED_UP, 6000));
        BodyTemperature.Climate chilied = BodyTemperature.climate(chili);
        c.check(chilied.target() >= 70f && (chilied.reasons() & BodyTemperature.R_WARMED_UP) != 0, "Campfire Chili (Warmed Up): powder snow can't chill you (target " + chilied.target() + ")");
        ServerPlayer blaze = c.player();
        blaze.setTicksFrozen(20);
        blaze.setItemSlot(EquipmentSlot.CHEST, socketed(Items.IRON_CHESTPLATE, Socketing.Gem.BLAZE));
        BodyTemperature.Climate blazing = BodyTemperature.climate(blaze);
        c.check(blazing.target() >= 70f && (blazing.reasons() & BodyTemperature.R_BLAZE) != 0, "Blaze socket: powder snow can't chill you (target " + blazing.target() + ")");
        ServerPlayer ice = c.player();
        ice.setTicksFrozen(20);
        ice.setItemSlot(EquipmentSlot.HEAD, socketed(Items.IRON_HELMET, Socketing.Gem.ICE));
        c.check(BodyTemperature.climate(ice).target() <= 0f, "Ice socket: powder snow still freezes you");
        float icyNight = BodyTemperature.NORMAL;
        int icyMinutes = 0;
        for (; icyMinutes < 3600 && icyNight > BodyTemperature.MIN_HEAT; icyMinutes++) icyNight = BodyTemperature.step(icyNight, BodyTemperature.SNOWY_WATER_TARGET, false, false, BodyTemperature.SNOWY_WATER_CHILL);
        c.check(icyNight == -5f && icyMinutes < 300, "snowy water: all the way down to -5 degrees (in " + icyMinutes + "s)");
        c.check(Socketing.Gem.ICE.fits(new ItemStack(Items.LEATHER_BOOTS)) && !Socketing.Gem.ICE.fits(new ItemStack(Items.IRON_PICKAXE))
            && Socketing.Gem.BLAZE.fits(new ItemStack(Items.DIAMOND_HELMET)) && !Socketing.Gem.BLAZE.fits(new ItemStack(Items.IRON_SHOVEL)),
            "Ice and Blaze Powder fit any armor piece (and some tools, SocketTests)");

        // somewhere hot (the test world is a hot biome in the daytime, when it isn't raining)
        ServerPlayer plain = c.player();
        BodyTemperature.Climate sunny = BodyTemperature.climate(plain);
        if ((sunny.reasons() & BodyTemperature.R_SUN) != 0) {
            c.check(sunny.target() == sun, "desert sun: 88% (got " + sunny.target() + ")");
            ServerPlayer hell = c.player();
            Traits.learn(hell, Skill.USED_TO_HELL);
            c.check(BodyTemperature.climate(hell).target() == 70f, "Used To Hell: never above 70%");
            ServerPlayer pants = c.player();
            lesson(pants, Skill.ICE_IN_THE_PANTS, 1);
            c.check(BodyTemperature.climate(pants).target() == 70f, "Ice In The Pants: never above 70%");
            ServerPlayer sorbet = c.player();
            sorbet.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.alexh.capitalism.core.ModEffects.COOLED_OFF, 6000));
            c.check(BodyTemperature.climate(sorbet).target() == 70f, "Frosty Melon Sorbet (Cooled Off): never above 70");
            ServerPlayer cool = c.player();
            cool.setItemSlot(EquipmentSlot.FEET, socketed(Items.IRON_BOOTS, Socketing.Gem.ICE));
            c.check(BodyTemperature.climate(cool).target() == 70f, "Ice socket: you can't get hot");
            ServerPlayer hot = c.player();
            hot.setItemSlot(EquipmentSlot.LEGS, socketed(Items.IRON_LEGGINGS, Socketing.Gem.BLAZE));
            c.check(BodyTemperature.climate(hot).target() == 100f, "Blaze socket in the sun: burning (100%)");
            ServerPlayer leathered = c.player();
            leathered.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
            leathered.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
            leathered.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
            leathered.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
            c.check(BodyTemperature.climate(leathered).target() == 100f, "full leather in the sun: burning (100%)");
            ServerPlayer soaked = c.player();
            soaked.getData(ModAttachments.WELLBEING.get()).wet = true;
            BodyTemperature.Climate dripping = BodyTemperature.climate(soaked);
            c.check(dripping.target() == BodyTemperature.WATER_COOLED_TARGET && (dripping.reasons() & BodyTemperature.R_WET) != 0,
                "wet in the sun: cooled like in the water (got " + dripping.target() + ")");
        }

        // Neglect: Low water / freezing / burning drag the wellbeing meter down; at -5 minutes you lose a heart
        ServerPlayer neglected = c.player();
        var nw = neglected.getData(ModAttachments.WELLBEING.get());
        neglected.getData(ModAttachments.THIRST.get()).setPurity(5f);
        nw.bodyHeat = 5f;
        int n = com.alexh.capitalism.player.WellnessEvents.neglect(neglected, nw);
        c.check((n & com.alexh.capitalism.player.WellnessEvents.N_LOW_WATER) != 0 && (n & com.alexh.capitalism.player.WellnessEvents.N_FREEZING) != 0,
            "neglect: Low water and freezing (under 10) both count (got " + n + ")");
        nw.bodyHeat = 97f;
        c.check((com.alexh.capitalism.player.WellnessEvents.neglect(neglected, nw) & com.alexh.capitalism.player.WellnessEvents.N_BURNING) != 0, "neglect: burning (over 95) counts");
        var meter = new com.alexh.capitalism.player.WellbeingData();
        int seconds = 0;
        while (com.alexh.capitalism.player.WellbeingData.effect(meter.score(), 600, -300, 1) != com.alexh.capitalism.player.WellbeingData.Effect.PENALTY && seconds < 2000) {
            meter.tickScore(true, 1, -600, 900);
            seconds++;
        }
        c.check(seconds == 300, "one kind of neglect: -1 heart after 5 minutes, even while eating well (took " + seconds + "s)");
        var both = new com.alexh.capitalism.player.WellbeingData();
        for (int i = 0; i < 150; i++) both.tickScore(false, 2, -600, 900);
        c.check(com.alexh.capitalism.player.WellbeingData.effect(both.score(), 600, -300, 0) == com.alexh.capitalism.player.WellbeingData.Effect.PENALTY,
            "two kinds of neglect (bad water + freezing): twice as fast, 2.5 minutes");
        for (int i = 0; i < 900; i++) both.tickScore(false, 0, -600, 900);
        c.check(both.score() == 0, "no neglect, not thriving: drifts back to neutral");
        // a drink of alcohol: Warmed Up for 2 minutes
        ServerPlayer drinker = c.player();
        c.consume(drinker, new ItemStack(ModItems.GLASS_OF_RED_WINE.get()));
        var warm = drinker.getEffect(com.alexh.capitalism.core.ModEffects.WARMED_UP);
        c.check(warm != null && warm.getDuration() == com.alexh.capitalism.player.WellnessEvents.ALCOHOL_WARMTH_TICKS, "a glass of wine: Warmed Up for 2 minutes");

        // A Fresh Start: diet back to the balanced head start, water full, body heat 70% and dry
        ServerPlayer fresh = c.player();
        var diet = fresh.getData(ModAttachments.DIET.get());
        diet.eat(new float[]{0, 0, 0, 1}, 200, 130);
        var wellbeing = fresh.getData(ModAttachments.WELLBEING.get());
        wellbeing.bodyHeat = 25f;
        wellbeing.wet = true;
        fresh.getData(ModAttachments.THIRST.get()).drain(10);
        com.alexh.capitalism.player.WellnessEvents.freshStart(fresh);
        c.check(com.alexh.capitalism.player.DietData.tier(diet.values()) == com.alexh.capitalism.player.DietData.Tier.BALANCED, "A Fresh Start: diet balanced again");
        c.check(wellbeing.bodyHeat == 70f && !wellbeing.wet, "A Fresh Start: body heat 70%, dry");
        c.check(fresh.getData(ModAttachments.THIRST.get()).getThirstF() >= 20f, "A Fresh Start: water full");
    }

    private static ItemStack socketed(net.minecraft.world.item.Item item, Socketing.Gem gem) {
        ItemStack stack = new ItemStack(item);
        stack.set(com.alexh.capitalism.core.ModComponents.SOCKET.get(), gem.id);
        return stack;
    }

    // Body heat (player/BodyTemperature): how fast it moves, step by step, against the design (Wellness.md > Temperature)
    private static void bodyTemperature(GameTests.Checks c) {
        // into icy water / rain from 70%: Cold (under 50%) in about 15 seconds...
        float heat = BodyTemperature.NORMAL;
        int seconds = 0;
        while (BodyTemperature.zone(heat) != BodyTemperature.ZONE_COLD && seconds < 1000) {
            heat = BodyTemperature.step(heat, BodyTemperature.COLD_WATER_TARGET, false, false);
            seconds++;
        }
        c.check(seconds >= 13 && seconds <= 17, "70% to Cold in about 15s (took " + seconds + "s)");
        // ... then about 2 more minutes of Cold to Freezing (under 32%)
        int coldFor = 0;
        while (BodyTemperature.zone(heat) != BodyTemperature.ZONE_FREEZING && coldFor < 1000) {
            heat = BodyTemperature.step(heat, BodyTemperature.COLD_WATER_TARGET, false, false);
            coldFor++;
        }
        c.check(coldFor >= 110 && coldFor <= 130, "Cold to Freezing in about 2 minutes (took " + coldFor + "s)");
        // water in a snowy place: twice as fast (Cold in ~7.5s, Freezing ~1 minute after)
        float icy = BodyTemperature.NORMAL;
        int icyToCold = 0, icyToFreezing = 0;
        while (BodyTemperature.zone(icy) != BodyTemperature.ZONE_COLD && icyToCold < 1000) {
            icy = BodyTemperature.step(icy, BodyTemperature.COLD_WATER_TARGET, false, false, BodyTemperature.SNOWY_WATER_CHILL);
            icyToCold++;
        }
        while (BodyTemperature.zone(icy) != BodyTemperature.ZONE_FREEZING && icyToFreezing < 1000) {
            icy = BodyTemperature.step(icy, BodyTemperature.COLD_WATER_TARGET, false, false, BodyTemperature.SNOWY_WATER_CHILL);
            icyToFreezing++;
        }
        c.check(icyToCold >= 6 && icyToCold <= 9 && icyToFreezing >= 55 && icyToFreezing <= 65,
            "snowy water chills twice as fast (Cold in " + icyToCold + "s, Freezing " + icyToFreezing + "s later)");
        // it bottoms out at the water's 20%, never lower
        for (int i = 0; i < 600; i++) heat = BodyTemperature.step(heat, BodyTemperature.COLD_WATER_TARGET, false, false);
        c.check(heat == BodyTemperature.COLD_WATER_TARGET, "icy water stops at 20% (got " + heat + ")");
        // a campfire: semi-fast out of Freezing, through Cold, to Normal, and never past 70%
        int toCold = 0;
        while (BodyTemperature.zone(heat) == BodyTemperature.ZONE_FREEZING && toCold < 1000) {
            heat = BodyTemperature.step(heat, BodyTemperature.NORMAL, true, false);
            toCold++;
        }
        c.check(toCold <= 8, "a fire gets you out of Freezing in a few seconds (took " + toCold + "s)");
        int toNormal = 0;
        while (BodyTemperature.zone(heat) != BodyTemperature.ZONE_NORMAL && toNormal < 1000) {
            heat = BodyTemperature.step(heat, BodyTemperature.NORMAL, true, false);
            toNormal++;
        }
        c.check(toNormal <= 12, "then out of Cold quickly too (took " + toNormal + "s)");
        for (int i = 0; i < 600; i++) heat = BodyTemperature.step(heat, BodyTemperature.NORMAL, true, false);
        c.check(heat == BodyTemperature.NORMAL, "a fire alone never makes you hot: it settles at 70% (got " + heat + ")");
        // the desert sun: Hot (over 80%) in about 15 seconds, and a fire there pushes you up into burning
        float desert = BodyTemperature.NORMAL;
        int toHot = 0;
        while (BodyTemperature.zone(desert) != BodyTemperature.ZONE_HOT && toHot < 1000) {
            desert = BodyTemperature.step(desert, BodyTemperature.DESERT_SUN_TARGET, false, false);
            toHot++;
        }
        c.check(toHot >= 13 && toHot <= 17, "70% to Hot in the desert sun in about 15s (took " + toHot + "s)");
        for (int i = 0; i < 600; i++) desert = BodyTemperature.step(desert, BodyTemperature.BURNING_TARGET, true, false);
        c.check(desert == BodyTemperature.BURNING_TARGET, "a fire in the desert: burning, 100% (got " + desert + ")");
        // out of Hot in a few seconds once you cool off
        int cool = 0;
        while (BodyTemperature.zone(desert) == BodyTemperature.ZONE_HOT && cool < 1000) {
            desert = BodyTemperature.step(desert, BodyTemperature.NORMAL, false, false);
            cool++;
        }
        c.check(cool <= 8, "out of burning in a few seconds (took " + cool + "s)");
        // powder snow drops you fast
        float snow = BodyTemperature.NORMAL;
        int snowFreeze = 0;
        while (BodyTemperature.zone(snow) != BodyTemperature.ZONE_FREEZING && snowFreeze < 1000) {
            snow = BodyTemperature.step(snow, 0f, false, true);
            snowFreeze++;
        }
        c.check(snowFreeze <= 30, "powder snow freezes you in under half a minute (took " + snowFreeze + "s)");
        // deeper is worse: hearts go faster the colder you are, thirst faster the hotter
        c.check(BodyTemperature.frostInterval(31f) == 15 && BodyTemperature.frostInterval(0f) == 3 && BodyTemperature.frostInterval(16f) < 15,
            "Freezing: half a heart every 15s at the edge, every 3s at 0%");
        c.check(BodyTemperature.thirstMultiplier(70f) == 1f && BodyTemperature.thirstMultiplier(100f) == 5f && BodyTemperature.thirstMultiplier(80.01f) > 2.99f
            && BodyTemperature.thirstMultiplier(90f) == 4f, "Hot: thirst x3 past 80, x4 at 90, x5 at 100");
        c.check(BodyTemperature.zone(31.9f) == BodyTemperature.ZONE_FREEZING && BodyTemperature.zone(32f) == BodyTemperature.ZONE_COLD
            && BodyTemperature.zone(50f) == BodyTemperature.ZONE_NORMAL && BodyTemperature.zone(80f) == BodyTemperature.ZONE_NORMAL
            && BodyTemperature.zone(80.1f) == BodyTemperature.ZONE_HOT, "zones: Freezing <32, Cold <50, Normal 50-80, Hot >80");
        // the real thing: a player in the open with nothing around is just right, and a player frozen stiff takes frost damage
        ServerPlayer p = c.player();
        WellbeingData data = p.getData(ModAttachments.WELLBEING.get());
        data.bodyHeat = 0f;
        p.setHealth(20f);
        data.frostSeconds = 0;
        for (int i = 0; i < 5; i++) BodyTemperature.tick(p, data);
        c.check(p.getHealth() < 20f, "Freezing near 0%: you lose health within 5 seconds (health " + p.getHealth() + ")");
        p.setHealth(1.5f);
        data.bodyHeat = 0f;
        for (int i = 0; i < 10; i++) BodyTemperature.tick(p, data);
        c.check(p.getHealth() == 1.5f, "Freezing never takes you below 1 heart");
    }

    // Night Owl (phantoms), Beekeeper's Notes (bees), Quiet Steps (how well mobs see you), Iron Stomach (no Hunger)
    private static void mobs(GameTests.Checks c) {
        Vec3 at = c.helper.absoluteVec(new Vec3(2.5, 2, 2.5));
        ServerPlayer plain = c.player();
        ServerPlayer owl = c.player();
        Traits.learn(owl, Skill.NIGHT_OWL);
        Mob phantom = EntityTypes.PHANTOM.create(c.level(), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        phantom.snapTo(at.x, at.y + 1, at.z, 0, 0);
        c.level().addFreshEntity(phantom);
        phantom.setTarget(plain);
        c.check(phantom.getTarget() == plain, "a phantom goes for a player without Night Owl");
        phantom.setTarget(owl);
        c.check(phantom.getTarget() != owl, "a phantom won't go for a Night Owl");

        ServerPlayer keeper = c.player();
        lesson(keeper, Skill.BEEKEEPERS_NOTES, 1);
        Mob bee = EntityTypes.BEE.create(c.level(), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        bee.snapTo(at.x, at.y + 1, at.z, 0, 0);
        c.level().addFreshEntity(bee);
        bee.setTarget(plain);
        c.check(bee.getTarget() == plain, "a bee goes for a player without Beekeeper's Notes");
        bee.setTarget(null);
        bee.setTarget(keeper);
        c.check(bee.getTarget() != keeper, "a bee won't go for a Beekeeper");

        Mob zombie = EntityTypes.ZOMBIE.create(c.level(), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        zombie.snapTo(at.x + 1, at.y, at.z, 0, 0);
        double seen = plain.getVisibilityPercent(c.level(), zombie);
        for (int level = 1; level <= 2; level++) {
            ServerPlayer quiet = c.player();
            lesson(quiet, Skill.QUIET_STEPS, level);
            c.near(quiet.getVisibilityPercent(c.level(), zombie) / seen, 1 - 0.2 * level, 1e-6, "Quiet Steps " + level + ": mobs see you less");
        }

        plain.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200));
        c.check(plain.hasEffect(MobEffects.HUNGER), "Hunger sticks without Iron Stomach");
        ServerPlayer stomach = c.player();
        Traits.learn(stomach, Skill.IRON_STOMACH);
        stomach.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200));
        c.check(!stomach.hasEffect(MobEffects.HUNGER), "Iron Stomach: no Hunger");
    }

    // Archery Drills (arrow damage) and Steady Aim (arrows fly where you look)
    private static void combat(GameTests.Checks c) {
        Vec3 at = c.helper.absoluteVec(new Vec3(2.5, 2, 2.5));
        for (int level = 0; level <= 2; level++) {
            ServerPlayer archer = c.player();
            if (level > 0) lesson(archer, Skill.ARCHERY_DRILLS, level);
            LivingEntity pig = EntityTypes.PIG.create(c.level(), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
            pig.snapTo(at.x, at.y, at.z, 0, 0);
            c.level().addFreshEntity(pig);
            float before = pig.getHealth();
            Arrow arrow = new Arrow(c.level(), archer, new ItemStack(Items.ARROW), null);
            pig.hurtServer(c.level(), c.level().damageSources().arrow(arrow, archer), 3.0F);
            c.near(before - pig.getHealth(), 3.0 * (1 + 0.15 * level), 1e-3, "Archery Drills " + level + ": a 3-damage arrow");
        }
        // a melee hit isn't an arrow
        ServerPlayer drills = c.player();
        lesson(drills, Skill.ARCHERY_DRILLS, 2);
        LivingEntity pig = EntityTypes.PIG.create(c.level(), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        pig.snapTo(at.x, at.y, at.z, 0, 0);
        c.level().addFreshEntity(pig);
        float before = pig.getHealth();
        pig.hurtServer(c.level(), c.level().damageSources().playerAttack(drills), 3.0F);
        c.near(before - pig.getHealth(), 3.0, 1e-3, "Archery Drills doesn't touch melee");
        // nor a thrown trident (an "arrow" to the game)
        LivingEntity target = EntityTypes.PIG.create(c.level(), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        target.snapTo(at.x, at.y, at.z, 0, 0);
        c.level().addFreshEntity(target);
        float full = target.getHealth();
        var trident = new net.minecraft.world.entity.projectile.arrow.ThrownTrident(c.level(), drills, new ItemStack(Items.TRIDENT));
        target.hurtServer(c.level(), c.level().damageSources().trident(trident, drills), 3.0F);
        c.near(full - target.getHealth(), 3.0, 1e-3, "Archery Drills doesn't touch tridents");

        for (boolean steady : new boolean[]{false, true}) {
            ServerPlayer shooter = c.player();
            if (steady) Traits.learn(shooter, Skill.STEADY_AIM);
            shooter.setYRot(30);
            shooter.setXRot(-10);
            Vec3 look = shooter.getViewVector(1.0F).normalize();
            Vec3 off = look.add(look.cross(new Vec3(0, 1, 0)).normalize().scale(Math.tan(Math.toRadians(2)))).normalize();
            Arrow arrow = new Arrow(c.level(), shooter, new ItemStack(Items.ARROW), null);
            arrow.setDeltaMovement(off.scale(3.0));
            c.level().addFreshEntity(arrow);
            double degrees = Math.toDegrees(Math.acos(Math.min(1, arrow.getDeltaMovement().normalize().dot(look))));
            if (steady) c.near(degrees, 0, 0.01, "Steady Aim: a slightly-off arrow flies dead straight");
            else c.near(degrees, 2, 0.05, "without Steady Aim the arrow keeps its spread");
            c.near(arrow.getDeltaMovement().length(), 3.0, 1e-6, "arrow keeps its speed");
        }
        ServerPlayer shooter = c.player();
        Traits.learn(shooter, Skill.STEADY_AIM);
        Vec3 look = shooter.getViewVector(1.0F).normalize();
        Vec3 side = look.add(look.cross(new Vec3(0, 1, 0)).normalize().scale(Math.tan(Math.toRadians(10)))).normalize();
        Arrow multishot = new Arrow(c.level(), shooter, new ItemStack(Items.ARROW), null);
        multishot.setDeltaMovement(side.scale(3.0));
        c.level().addFreshEntity(multishot);
        c.near(Math.toDegrees(Math.acos(Math.min(1, multishot.getDeltaMovement().normalize().dot(look)))), 10, 0.05,
            "Steady Aim leaves a Multishot side arrow (10 degrees) alone");
    }

    // Butchery Basics (extra meat from animals only), Fast Learner (25% more XP)
    private static void dropsAndXp(GameTests.Checks c) {
        Vec3 at = c.helper.absoluteVec(new Vec3(2.5, 2, 2.5));
        Animal cow = (Animal) EntityTypes.COW.create(c.level(), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        cow.snapTo(at.x, at.y, at.z, 0, 0);
        LivingEntity zombie = EntityTypes.ZOMBIE.create(c.level(), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        zombie.snapTo(at.x, at.y, at.z, 0, 0);
        int tries = 4000;
        for (int level = 0; level <= 2; level++) {
            ServerPlayer butcher = c.player();
            if (level > 0) lesson(butcher, Skill.BUTCHERY_BASICS, level);
            int extra = 0;
            int leatherExtra = 0;
            for (int i = 0; i < tries; i++) {
                ItemEntity beef = new ItemEntity(c.level(), at.x, at.y, at.z, new ItemStack(Items.BEEF, 1));
                ItemEntity leather = new ItemEntity(c.level(), at.x, at.y, at.z, new ItemStack(Items.LEATHER, 1));
                List<ItemEntity> drops = new ArrayList<>(List.of(leather, beef));
                SkillEffects.onDrops(new LivingDropsEvent(cow, c.level().damageSources().playerAttack(butcher), drops, true));
                extra += beef.getItem().getCount() - 1;
                leatherExtra += leather.getItem().getCount() - 1;
            }
            c.near(extra / (double) tries, 0.25 * level, 0.03, "Butchery Basics " + level + ": share of cows with an extra beef");
            c.check(leatherExtra == 0, "Butchery Basics " + level + ": leather isn't doubled");
            if (level > 0) {
                ItemEntity flesh = new ItemEntity(c.level(), at.x, at.y, at.z, new ItemStack(Items.ROTTEN_FLESH, 1));
                int zombieExtra = 0;
                for (int i = 0; i < 400; i++) {
                    SkillEffects.onDrops(new LivingDropsEvent(zombie, c.level().damageSources().playerAttack(butcher), new ArrayList<>(List.of(flesh)), true));
                }
                zombieExtra = flesh.getItem().getCount() - 1;
                c.check(zombieExtra == 0, "Butchery Basics " + level + ": nothing extra from a zombie");
            }
        }

        for (boolean learner : new boolean[]{false, true}) {
            ServerPlayer player = c.player();
            if (learner) Traits.learn(player, Skill.FAST_LEARNER);
            int before = player.totalExperience;
            for (int i = 0; i < 100; i++) {
                ExperienceOrb orb = new ExperienceOrb(c.level(), at.x, at.y, at.z, 100);
                SkillEffects.onPickupXp(new PlayerXpEvent.PickupXp(player, orb));
            }
            c.near((player.totalExperience - before) / 100.0, learner ? 25 : 0, 1e-6, (learner ? "Fast Learner" : "no book") + ": extra XP per 100-XP orb");
        }
    }

    // First Aid Basics (more natural healing), Horse Whispering (the horse you ride)
    private static void ticking(GameTests.Checks c) {
        // natural healing heals 25% / 50% more: the slow kind (food 18+, no saturation: every 80 ticks) and the fast
        // kind (a full bar with saturation: every 10 ticks)
        for (int level = 0; level <= 2; level++) {
            for (boolean saturated : new boolean[]{false, true}) {
                ServerPlayer player = c.player();
                if (level > 0) lesson(player, Skill.FIRST_AID_BASICS, level);
                player.setHealth(10);
                player.getFoodData().setFoodLevel(saturated ? 20 : 18);
                player.getFoodData().setSaturation(saturated ? 6 : 0);
                for (int i = 0; i < (saturated ? 10 : 80); i++) player.getFoodData().tick(player);
                c.near(player.getHealth(), 11 + 0.25 * level, 1e-4, "First Aid Basics " + level + (saturated ? " (saturated)" : " (slow)") + ": one natural heal");
            }
        }
        ServerPlayer hungry = c.player();
        lesson(hungry, Skill.FIRST_AID_BASICS, 2);
        hungry.setHealth(10);
        hungry.getFoodData().setFoodLevel(10);
        hungry.getFoodData().setSaturation(0);
        for (int i = 0; i < 200; i++) hungry.getFoodData().tick(hungry);
        c.near(hungry.getHealth(), 10, 1e-6, "First Aid Basics needs you well fed");

        Vec3 at = c.helper.absoluteVec(new Vec3(2.5, 2, 2.5));
        for (int level = 1; level <= 2; level++) {
            ServerPlayer rider = c.player();
            lesson(rider, Skill.HORSE_WHISPERING, level);
            LivingEntity horse = EntityTypes.HORSE.create(c.level(), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
            horse.snapTo(at.x, at.y, at.z, 0, 0);
            c.level().addFreshEntity(horse);
            AttributeInstance speed = horse.getAttribute(Attributes.MOVEMENT_SPEED);
            double normal = speed.getValue();
            rider.startRiding(horse, true, true);
            rider.tickCount = 20;
            SkillEffects.onPlayerTick(new PlayerTickEvent.Post(rider));
            c.near(speed.getValue(), normal + speed.getBaseValue() * 0.15 * level, 1e-6, "Horse Whispering " + level + ": the horse is faster");
            rider.stopRiding();
            c.near(speed.getValue(), normal, 1e-6, "Horse Whispering " + level + ": back to normal when you get off");
        }
    }

    // Sure Footed (powder snow, ice), Trail Rations (sprint hunger)
    private static void movement(GameTests.Checks c) {
        ServerPlayer plain = c.player();
        ServerPlayer sure = c.player();
        Traits.learn(sure, Skill.SURE_FOOTED);
        c.check(!PowderSnowBlock.canEntityWalkOnPowderSnow(plain), "no book: you sink in Powder Snow");
        c.check(PowderSnowBlock.canEntityWalkOnPowderSnow(sure), "Sure Footed: you walk on Powder Snow");

        c.helper.setBlock(new BlockPos(1, 1, 1), Blocks.ICE);
        c.helper.setBlock(new BlockPos(3, 1, 3), Blocks.STONE);
        Vec3 ice = c.helper.absoluteVec(new Vec3(1.5, 2, 1.5));
        Vec3 stone = c.helper.absoluteVec(new Vec3(3.5, 2, 3.5));
        AttributeInstance speed = sure.getAttribute(Attributes.MOVEMENT_SPEED);
        double normal = speed.getValue();
        sure.snapTo(ice.x, ice.y, ice.z, 0, 0);
        sure.setOnGround(true);
        SkillEffects.onPlayerTick(new PlayerTickEvent.Post(sure));
        c.near(speed.getValue(), normal * 1.2, 1e-6, "Sure Footed: 20% faster on ice");
        sure.snapTo(stone.x, stone.y, stone.z, 0, 0);
        sure.setOnGround(true);
        SkillEffects.onPlayerTick(new PlayerTickEvent.Post(sure));
        c.near(speed.getValue(), normal, 1e-6, "Sure Footed: normal speed off the ice");

        for (int level = 0; level <= 2; level++) {
            for (boolean sprinting : new boolean[]{false, true}) {
                ServerPlayer player = c.player();
                if (level > 0) lesson(player, Skill.TRAIL_RATIONS, level);
                player.setSprinting(sprinting);
                float before = exhaustion(player);
                player.causeFoodExhaustion(1.0F);
                double expected = sprinting ? 1 - 0.3 * level : 1;
                c.near(exhaustion(player) - before, expected, 1e-5, "Trail Rations " + level + (sprinting ? " sprinting" : " walking"));
            }
        }
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

    // Fisherman's Patience: the bobber's lure (ticks taken off the wait), 100 per level like Lure
    private static void fishing(GameTests.Checks c) {
        for (int level = 0; level <= 2; level++) {
            ServerPlayer fisher = c.player();
            if (level > 0) lesson(fisher, Skill.FISHERMANS_PATIENCE, level);
            fisher.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FISHING_ROD));
            fisher.getMainHandItem().use(c.level(), fisher, InteractionHand.MAIN_HAND);
            FishingHook hook = fisher.fishing;
            if (hook == null) {
                c.fail("Fisherman's Patience " + level + ": no bobber was cast");
                continue;
            }
            try {
                Field lure = FishingHook.class.getDeclaredField("lureSpeed");
                lure.setAccessible(true);
                c.near(lure.getInt(hook), 100 * level, 1e-6, "Fisherman's Patience " + level + ": lure");
            } catch (ReflectiveOperationException e) {
                c.fail("couldn't read the bobber's lure: " + e);
            }
            hook.discard();
        }
    }

    // Reading books: lessons give their (hidden) effect for the right time, Bookworm makes it longer; traits are learned
    // once and the book is used up; a known trait's book stays
    private static void reading(GameTests.Checks c) {
        for (boolean delicate : new boolean[]{false, true}) {
            for (boolean bookworm : new boolean[]{false, true}) {
                ServerPlayer reader = c.player();
                if (bookworm) Traits.learn(reader, Skill.BOOKWORM);
                ItemStack book = SkillBookItem.write(new ItemStack(ModItems.SKILL_BOOK.get()), Skill.RUNNING_CLUB, delicate);
                ItemStack left = book.getItem().finishUsingItem(book, c.level(), reader);
                MobEffectInstance effect = reader.getEffect(ModEffects.BOOK_LESSONS.get(Skill.RUNNING_CLUB));
                String what = (delicate ? "Delicate" : "Rough") + (bookworm ? " + Bookworm" : "");
                if (effect == null) {
                    c.fail(what + ": reading gave no effect");
                    continue;
                }
                int ticks = (delicate ? Skill.DELICATE_TICKS : Skill.ROUGH_TICKS) * (bookworm ? 3 : 2) / 2;
                c.check(effect.getDuration() == ticks, what + ": lasts " + ticks + " ticks (got " + effect.getDuration() + ")");
                c.check(effect.getAmplifier() == (delicate ? 1 : 0), what + ": level " + (delicate ? "II" : "I"));
                c.check(!effect.isVisible() && !effect.showIcon(), what + ": no particles, no vanilla icon");
                c.check(left.isEmpty(), what + ": the book is used up");
            }
        }
        ServerPlayer reader = c.player();
        ItemStack trait = SkillBookItem.write(new ItemStack(ModItems.SKILL_BOOK.get()), Skill.NIMBLE, false);
        trait.getItem().finishUsingItem(trait, c.level(), reader);
        c.check(Traits.of(reader).has(Skill.NIMBLE), "reading a trait book teaches it");
        c.check(trait.isEmpty(), "the trait book is used up");
        ItemStack again = SkillBookItem.write(new ItemStack(ModItems.SKILL_BOOK.get()), Skill.NIMBLE, false);
        again.getItem().finishUsingItem(again, c.level(), reader);
        c.check(again.getCount() == 1, "a trait you already know: the book stays");

        // every book's text names just its own level's number
        for (Skill skill : Skill.values()) {
            for (int level = 1; level <= 2; level++) {
                String text = skill.description(level);
                c.check(!text.contains("{") && !text.contains("|") && !text.contains("}") && !text.contains("II:"), skill.title + " level " + level + " reads: " + text);
            }
        }
        c.check(Skill.WATER_RATIONING.description(2).equals("Thirst drains 50% slower."), "a Delicate Water Rationing says 50%");
        c.check(SkillBookItem.readTicks(Skill.NIMBLE) == 500 && SkillBookItem.readTicks(Skill.RUNNING_CLUB) == 200,
            "reading takes 25 s for a trait, 10 s for a lesson");
    }

    // The odds: lesson / trait, rare vs common (Skill.roll), against LibrarianBooks.md
    private static void rolls(GameTests.Checks c) {
        RandomSource random = RandomSource.create(42);
        int n = 400000;
        for (boolean delicate : new boolean[]{false, true}) {
            for (float odds : new float[]{1.0F, 2.0F}) {
                Map<Skill, Integer> counts = new EnumMap<>(Skill.class);
                for (int i = 0; i < n; i++) counts.merge(Skill.roll(random, delicate, odds), 1, Integer::sum);
                double traits = 0;
                for (var entry : counts.entrySet()) if (entry.getKey().trait) traits += entry.getValue();
                double expected = (delicate ? Skill.DELICATE_TRAIT_CHANCE : Skill.ROUGH_TRAIT_CHANCE) * odds;
                String what = (delicate ? "Delicate" : "Rough") + (odds > 1 ? " + Lucky Librarian" : "");
                c.near(traits / n, expected, expected * 0.08, what + ": share of traits");
                // each lesson (and instant book): common = 10 / (all their weights) of the draw, rare = 4 / (all their weights)
                double lessons = n - traits;
                double common = 0, rare = 0;
                int commons = 0, rares = 0;
                for (Skill skill : Skill.values()) {
                    if (skill.trait) continue;
                    int count = counts.getOrDefault(skill, 0);
                    if (skill.rare) { rare += count; rares++; } else { common += count; commons++; }
                }
                c.near((rare / rares) / (common / commons), 0.4, 0.03, what + ": a rare lesson is 0.4x as likely as a common one");
                int lessonWeight = 0;
                for (Skill skill : Skill.values()) if (!skill.trait) lessonWeight += skill.weight();
                c.near(common / commons / lessons, (double) Skill.COMMON_WEIGHT / lessonWeight, 0.003, what + ": each common lesson's share of lessons");
            }
        }
        // within traits: rare 4/104, common 10/104
        Map<Skill, Integer> counts = new EnumMap<>(Skill.class);
        int traitsSeen = 0;
        for (int i = 0; i < 3000000 && traitsSeen < 40000; i++) {
            Skill skill = Skill.roll(random, true, 2.0F);
            if (!skill.trait) continue;
            traitsSeen++;
            counts.merge(skill, 1, Integer::sum);
        }
        double common = 0, rare = 0;
        int commons = 0, rares = 0;
        for (Skill skill : Skill.values()) {
            if (!skill.trait) continue;
            int count = counts.getOrDefault(skill, 0);
            if (skill.rare) { rare += count; rares++; } else { common += count; commons++; }
        }
        c.check(commons == 8 && rares == 6, "8 common traits, 6 rare (got " + commons + ", " + rares + ")");
        c.near(rare / rares / traitsSeen, 4.0 / 104, 0.006, "each rare trait's share of traits");
        c.near(common / commons / traitsSeen, 10.0 / 104, 0.008, "each common trait's share of traits");
    }

    // Lucky Librarian at the Librarian Table: doubles trait books, but only for Librarians
    private static void luckyLibrarian(GameTests.Checks c) {
        for (int setup = 0; setup < 3; setup++) {
            ServerPlayer writer = c.player();
            if (setup >= 1) lesson(writer, Skill.LUCKY_LIBRARIAN, 1);
            if (setup == 2) writer.getData(ModAttachments.JOBS.get()).add(Job.LIBRARIAN);
            LibrarianTableMenu menu = new LibrarianTableMenu(0, writer.getInventory(), ContainerLevelAccess.NULL);
            int writes = 6000;
            int traits = 0;
            for (int i = 0; i < writes; i++) {
                menu.getSlot(LibrarianTableMenu.BIG).set(new ItemStack(ModItems.DELICATE_BOOK.get()));
                menu.getSlot(LibrarianTableMenu.SMALL).set(new ItemStack(ModItems.KNOWLEDGE_PIECE.get()));
                ItemStack result = menu.getSlot(LibrarianTableMenu.RESULT).getItem().copy();
                if (result.isEmpty()) {
                    c.fail("the table shows no result for a Delicate Book + Knowledge Piece");
                    return;
                }
                menu.getSlot(LibrarianTableMenu.RESULT).onTake(writer, result);
                Skill skill = SkillBookItem.skill(result);
                if (skill != null && skill.trait) traits++;
            }
            double expected = Skill.DELICATE_TRAIT_CHANCE * (setup == 2 ? 2 : 1);
            String what = setup == 0 ? "no book" : setup == 1 ? "Lucky Librarian, not a Librarian" : "Lucky Librarian, a Librarian";
            c.near(traits / (double) writes, expected, 0.02, what + ": share of trait books written");
            c.check(menu.getSlot(LibrarianTableMenu.BIG).getItem().isEmpty(), what + ": taking the book uses the blank book");
        }
    }

    // The Stitching Table: needs Paper, a cover in every ring slot and thread; after 15 minutes one book, using 1 Paper,
    // 1 from EACH of the 16 ring slots and 1 thread; Iron makes a Rough Book, a Diamond a Delicate one
    private static void stitching(GameTests.Checks c) {
        net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.containing(c.at(2, 1, 2));
        for (boolean diamond : new boolean[]{false, true}) {
            // a fresh, empty table each time
            c.level().setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            c.level().setBlockAndUpdate(pos, com.alexh.capitalism.core.ModBlocks.STITCHING_TABLE.get().defaultBlockState());
            StitchingTableBlockEntity table = (StitchingTableBlockEntity) c.level().getBlockEntity(pos);
            table.setItem(StitchingTableBlockEntity.PAPER, new ItemStack(Items.PAPER, 3));
            table.setItem(StitchingTableBlockEntity.THREAD, new ItemStack(diamond ? Items.DIAMOND : Items.IRON_INGOT, 3));
            for (int i = 0; i < StitchingTableBlockEntity.COVERS - 1; i++) {
                table.setItem(i, new ItemStack(i % 2 == 0 ? Items.LEATHER : Items.RABBIT_HIDE, 2));
            }
            String what = diamond ? "Diamond" : "Iron";
            for (int t = 0; t < 200; t++) StitchingTableBlockEntity.serverTick(c.level(), pos, c.level().getBlockState(pos), table);
            c.check(!c.level().getBlockState(pos).getValue(StitchingTableBlock.SEWING) && table.output().isEmpty(),
                what + ": with one ring slot empty it doesn't sew");
            table.setItem(StitchingTableBlockEntity.COVERS - 1, new ItemStack(Items.LEATHER, 2));
            for (int t = 0; t < StitchingTableBlockEntity.SEW_TICKS - 1; t++) {
                StitchingTableBlockEntity.serverTick(c.level(), pos, c.level().getBlockState(pos), table);
            }
            c.check(c.level().getBlockState(pos).getValue(StitchingTableBlock.SEWING) && table.output().isEmpty(), what + ": still sewing a tick before 15 minutes");
            StitchingTableBlockEntity.serverTick(c.level(), pos, c.level().getBlockState(pos), table);
            var book = diamond ? com.alexh.capitalism.core.ModItems.DELICATE_BOOK.get() : com.alexh.capitalism.core.ModItems.ROUGH_BOOK.get();
            c.check(table.output().is(book) && table.output().getCount() == 1, what + ": one " + (diamond ? "Delicate" : "Rough") + " Book after 15 minutes");
            boolean everySlot = true;
            for (int i = 0; i < StitchingTableBlockEntity.COVERS; i++) everySlot &= table.getItem(i).getCount() == 1;
            c.check(everySlot, what + ": one cover used from every one of the " + StitchingTableBlockEntity.COVERS + " ring slots");
            c.check(table.paper().getCount() == 2 && table.thread().getCount() == 2, what + ": 1 Paper and 1 thread used");
        }
    }
}
