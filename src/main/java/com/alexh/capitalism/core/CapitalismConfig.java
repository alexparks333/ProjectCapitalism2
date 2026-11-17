package com.alexh.capitalism.core;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class CapitalismConfig {

    // ── Thirst ──────────────────────────────────────────────────────────────
    public static ModConfigSpec.IntValue THIRST_MAX;
    public static ModConfigSpec.DoubleValue THIRST_DRAIN_PER_TICK;
    public static ModConfigSpec.DoubleValue THIRST_DRAIN_SPRINT_MULTIPLIER;
    public static ModConfigSpec.DoubleValue THIRST_RAW_WATER_RESTORE;
    public static ModConfigSpec.DoubleValue THIRST_PURIFIED_WATER_RESTORE;
    public static ModConfigSpec.DoubleValue THIRST_MAX_HYDRATION;
    public static ModConfigSpec.IntValue PURIFICATION_TICKS;
    public static ModConfigSpec.IntValue PURIFICATION_USES_PER_FUEL;
    public static ModConfigSpec.IntValue JUICER_TICKS;
    public static ModConfigSpec.IntValue FERMENTER_TICKS;
    public static ModConfigSpec.IntValue FERMENTER_SUGAR_TICKS;

    // ── Chef ─────────────────────────────────────────────────────────────────
    public static ModConfigSpec.DoubleValue CHEF_GOURMET_HUNGER_MULTIPLIER;
    public static ModConfigSpec.DoubleValue CHEF_GOURMET_SPEED_MULTIPLIER;
    public static ModConfigSpec.DoubleValue CHEF_NERF_HUNGER_MULTIPLIER;
    public static ModConfigSpec.DoubleValue CHEF_NERF_SPEED_MULTIPLIER;

    // ── Economy ──────────────────────────────────────────────────────────────
    public static ModConfigSpec.IntValue STARTING_BALANCE;
    public static ModConfigSpec.ConfigValue<List<? extends String>> BANK_PRICES;
    public static ModConfigSpec.BooleanValue LEADERBOARD_ENABLED;
    public static ModConfigSpec.IntValue LEADERBOARD_INTERVAL_TICKS;

    // ── Wellness ─────────────────────────────────────────────────────────────
    public static ModConfigSpec.DoubleValue DIET_KEEP_PER_MINUTE;
    public static ModConfigSpec.DoubleValue DIET_MIN_POINTS;
    public static ModConfigSpec.DoubleValue DIET_BALANCED_SCORE;
    public static ModConfigSpec.DoubleValue DIET_POOR_SCORE;
    public static ModConfigSpec.IntValue DIET_BALANCED_FOOD_BONUS;
    public static ModConfigSpec.DoubleValue DIET_START_POINTS;
    public static ModConfigSpec.DoubleValue DIET_WINDOW_POINTS;
    public static ModConfigSpec.IntValue DIET_MEALS_BEFORE_EFFECTS;
    public static ModConfigSpec.DoubleValue WATER_SIP_RESTORE;
    public static ModConfigSpec.DoubleValue RAW_WATER_SICK_CHANCE;
    public static ModConfigSpec.IntValue RAW_WATER_SICK_TICKS;
    public static ModConfigSpec.DoubleValue PURITY_START;
    public static ModConfigSpec.DoubleValue PURITY_SHIFT;
    public static ModConfigSpec.IntValue PURITY_LOW_WATER_LOSS;
    public static ModConfigSpec.IntValue PURITY_EXCELLENT_WATER_BONUS;
    public static ModConfigSpec.DoubleValue WELLBEING_HEALTH;
    public static ModConfigSpec.IntValue WELLBEING_BUFF_MINUTES;
    public static ModConfigSpec.IntValue WELLBEING_GRACE_MINUTES;
    public static ModConfigSpec.IntValue WELLBEING_DEBUFF_MINUTES;
    public static ModConfigSpec.IntValue WELLNESS_GRACE_SECONDS;
    public static ModConfigSpec.IntValue WELLNESS_FADE_SECONDS;
    public static ModConfigSpec.IntValue DIET_POOR_FOOD_LOSS;

    // ── Occupations ──────────────────────────────────────────────────────────
    public static ModConfigSpec.IntValue SWORD_HITS_BEFORE_DULL;
    public static ModConfigSpec.DoubleValue DULL_DAMAGE_MULTIPLIER;
    public static ModConfigSpec.IntValue PICKAXE_BLOCKS_BEFORE_DULL;
    public static ModConfigSpec.IntValue ARMOR_HITS_BEFORE_DENTED;
    public static ModConfigSpec.DoubleValue HOSTILE_MOB_HEALTH_MULTIPLIER;
    public static ModConfigSpec.DoubleValue LEVEL2_MOB_CHANCE;
    public static ModConfigSpec.DoubleValue BOSS_MOB_CHANCE;
    public static ModConfigSpec.DoubleValue GOURMET_GOLD_HAND;
    public static ModConfigSpec.DoubleValue GOURMET_GOLD_SILVER_PLATE;
    public static ModConfigSpec.DoubleValue GOURMET_GOLD_GOLDEN_PLATE;
    public static ModConfigSpec.IntValue JOB_CHANGE_COST;
    public static ModConfigSpec.IntValue JOB_CHANGE_COOLDOWN_DAYS;
    public static ModConfigSpec.IntValue NEW_PLAYER_GRACE_MINUTES;

    // ── Events ───────────────────────────────────────────────────────────────

    // Not-A-Scam Insurance (deathtax/Insurance, Insurance.md): flat company prices (not NPC prices), whole gold a day, and the death tax rate each tier leaves
    public static ModConfigSpec.IntValue DEATH_TAX_PERCENT;
    public static ModConfigSpec.IntValue INSURANCE_BRONZE_PRICE, INSURANCE_BRONZE_RATE, INSURANCE_SILVER_PRICE, INSURANCE_SILVER_RATE;
    public static ModConfigSpec.IntValue INSURANCE_GOLD_PRICE, INSURANCE_GOLD_RATE, INSURANCE_PREMIUM_PRICE, INSURANCE_PREMIUM_RATE;
    public static ModConfigSpec.IntValue BLOOD_MOON_INSURANCE_PRICE;

    // Banker prices (Economy.md), in gold NUGGETS (9 = 1 gold): "N" a piece, or "N/M" for N nuggets per M pieces.
    // Priced by the time it takes to get one and how farmable it is, aiming at about 25 to 60 gold an hour for steady
    // work (chopping, mining, farming, fighting), more for rare or risky things (diamonds, the Nether, the End), less
    // for things automatic farms pour out (bamboo, sugar cane, melons, iron). For scale: Steak is 3 nuggets at the Cook,
    // a feast 8 gold, the River Queen 350 gold. No money loops: anything craftable sells for less than what it's made
    // from (a Golden Apple costs 8 gold, Raw Gold smelts into a whole ingot, 4 Rabbit Hide make a Leather, Netherite = 4
    // scrap + 4 gold), Arrows aren't bought (4 from a flint, a feather and a stick), and nothing pays more than an NPC
    // shop could sell it for (BankerEvents caps that).
    private static final List<String> DEFAULT_BANK_PRICES = List.of(
        // Wood: a log a nugget (a tree is 4 to 7 logs; 25-35 gold an hour chopping). Bamboo grows like mad
        "minecraft:oak_log=1", "minecraft:spruce_log=1", "minecraft:birch_log=1", "minecraft:jungle_log=1",
        "minecraft:acacia_log=1", "minecraft:dark_oak_log=1", "minecraft:mangrove_log=1", "minecraft:cherry_log=1",
        "minecraft:bamboo_block=1/3",
        // Stone & mining: flint from gravel, clay 4 to a block, quartz all over the Nether
        "minecraft:flint=1/2", "minecraft:clay_ball=1/2", "minecraft:quartz=1",
        // Ores & metals, by what one ore brings (with Fortune III): coal ~2 nuggets, copper ~3 (huge veins), lapis ~5
        // (4-9 a block, ~14 with Fortune), redstone ~2, iron 2-3, gold ~13, a diamond ore ~13 gold. Emerald 5 gold, Diamond
        // 6, Netherite Scrap 14, so a Netherite Ingot (4 scrap + 4 gold) 60
        "minecraft:coal=1", "minecraft:raw_copper=1/3", "minecraft:copper_ingot=1/3", "minecraft:raw_iron=2",
        "minecraft:iron_ingot=3", "minecraft:lapis_lazuli=1/3", "minecraft:redstone=1/3", "minecraft:raw_gold=6",
        "minecraft:diamond=54", "minecraft:emerald=45", "minecraft:amethyst_shard=1/2",
        "minecraft:netherite_scrap=126", "minecraft:netherite_ingot=540",
        // Mob drops: common drops by the pair, the dangerous ones (creepers, the Nether, the End) worth the fight
        "minecraft:bone=1/2", "minecraft:string=1/2", "minecraft:spider_eye=1", "minecraft:gunpowder=3",
        "minecraft:feather=1/2", "minecraft:leather=2", "minecraft:rabbit_hide=1/2",
        "minecraft:ender_pearl=9", "minecraft:blaze_rod=6", "minecraft:ghast_tear=18", "minecraft:slime_ball=2",
        "minecraft:magma_cream=4", "minecraft:nether_wart=1/3", "minecraft:shulker_shell=36",
        "minecraft:phantom_membrane=6", "minecraft:turtle_scute=6", "minecraft:prismarine_shard=1",
        "minecraft:prismarine_crystals=2",
        // Food & farming: every crop 1 nugget for 2 (apples and pumpkins, one a tree or a vine, 1 each). A Farmer
        // can grow a mountain of it, so the bank pays little on
        // purpose: crops are worth more sold to a Cook (or another player). Cooked food is the Cook's to sell to players
        // (the bank pays little, never above the Cook NPC)
        "minecraft:wheat=1/2", "minecraft:beetroot=1/2", "minecraft:carrot=1/2", "minecraft:potato=1/2",
        "minecraft:melon_slice=1/2", "minecraft:pumpkin=1", "minecraft:sugar_cane=1/2", "minecraft:apple=1",
        "minecraft:bread=1", "minecraft:cooked_beef=1", "minecraft:cooked_porkchop=1", "minecraft:cooked_chicken=1/2",
        "minecraft:cooked_mutton=1/2", "minecraft:cooked_rabbit=1/2", "minecraft:cooked_salmon=1/2",
        "minecraft:cooked_cod=1/2", "minecraft:golden_apple=54", "minecraft:enchanted_golden_apple=180",
        // (V5) almost everything else you pick up, cheaply: raw meat and fish below cooked (the Cook's), more plants
        "minecraft:cod=1/3", "minecraft:salmon=1/3", "minecraft:tropical_fish=1", "minecraft:pufferfish=1",
        "minecraft:beef=1/2", "minecraft:porkchop=1/2", "minecraft:chicken=1/3", "minecraft:mutton=1/3", "minecraft:rabbit=1/3",
        "minecraft:rotten_flesh=1/8", "minecraft:egg=1/3", "minecraft:sweet_berries=1/4", "minecraft:glow_berries=1/2",
        "minecraft:cocoa_beans=1/2", "minecraft:cactus=1/2", "minecraft:kelp=1/4", "minecraft:dried_kelp=1/4",
        "minecraft:brown_mushroom=1/2", "minecraft:red_mushroom=1/2", "minecraft:chorus_fruit=1/2", "minecraft:honeycomb=1",
        "minecraft:honey_bottle=2", "minecraft:wheat_seeds=1/8", "minecraft:beetroot_seeds=1/8", "minecraft:pumpkin_seeds=1/8",
        "minecraft:melon_seeds=1/8", "minecraft:vine=1/8", "minecraft:lily_pad=1/4", "minecraft:sea_pickle=1/2",
        // more mob drops (Blaze Powder under half a rod: no loop), the sea's treasures
        "minecraft:blaze_powder=2", "minecraft:ink_sac=1/2", "minecraft:glow_ink_sac=1", "minecraft:white_wool=1/2",
        "minecraft:breeze_rod=6", "minecraft:armadillo_scute=2", "minecraft:nautilus_shell=9", "minecraft:echo_shard=18",
        "minecraft:totem_of_undying=45", "minecraft:sponge=9", "minecraft:wet_sponge=9", "minecraft:name_tag=9", "minecraft:saddle=9",
        // more wood: the Nether's stems, pale oak, planks four to a log, saplings
        "minecraft:pale_oak_log=1", "minecraft:crimson_stem=1", "minecraft:warped_stem=1", "minecraft:oak_planks=1/8",
        "minecraft:spruce_planks=1/8", "minecraft:birch_planks=1/8", "minecraft:oak_sapling=1/4", "minecraft:spruce_sapling=1/4",
        "minecraft:birch_sapling=1/4", "minecraft:jungle_sapling=1/4", "minecraft:acacia_sapling=1/4", "minecraft:dark_oak_sapling=1/4",
        // stone, earth and the like: a mountain of it is barely worth carrying (so it never beats real work)
        "minecraft:cobblestone=1/32", "minecraft:cobbled_deepslate=1/32", "minecraft:stone=1/16", "minecraft:dirt=1/32",
        "minecraft:sand=1/16", "minecraft:red_sand=1/16", "minecraft:gravel=1/16", "minecraft:netherrack=1/32",
        "minecraft:end_stone=1/8", "minecraft:obsidian=1", "minecraft:crying_obsidian=2", "minecraft:glowstone_dust=1/2",
        "minecraft:snowball=1/8", "minecraft:ice=1/4", "minecraft:packed_ice=2", "minecraft:blue_ice=18",
        "minecraft:soul_sand=1/8", "minecraft:basalt=1/16", "minecraft:blackstone=1/16", "minecraft:calcite=1/8",
        "minecraft:tuff=1/16", "minecraft:mud=1/16", "minecraft:moss_block=1/4", "minecraft:copper_ore=1/2", "minecraft:iron_ore=2",
        "minecraft:coal_ore=1", "minecraft:gold_ore=6", "minecraft:nether_quartz_ore=1"
    );

    // The town (town/Town): where the Whisperer walks, and the protected town center (no breaking blocks)
    public static ModConfigSpec.IntValue TOWN_WALK_MIN_X;
    public static ModConfigSpec.IntValue TOWN_WALK_MAX_X;
    public static ModConfigSpec.IntValue TOWN_WALK_MIN_Z;
    public static ModConfigSpec.IntValue TOWN_WALK_MAX_Z;
    public static ModConfigSpec.IntValue TOWN_PROTECT_MIN_X;
    public static ModConfigSpec.IntValue TOWN_PROTECT_MAX_X;
    public static ModConfigSpec.IntValue TOWN_PROTECT_MIN_Z;
    public static ModConfigSpec.IntValue TOWN_PROTECT_MAX_Z;
    public static ModConfigSpec.IntValue TOWN_PROTECT_MIN_Y;

    private static final ModConfigSpec SPEC;

    static {
        var builder = new ModConfigSpec.Builder();

        builder.comment("Thirst system settings").push("thirst");
        THIRST_MAX = builder.comment("Max thirst points (like hunger's 20)").defineInRange("maxThirst", 20, 1, 100);
        THIRST_DRAIN_PER_TICK = builder.comment("Thirst drained per game tick (1/20s)").defineInRange("drainPerTick", 0.0008, 0.0, 1.0);
        THIRST_DRAIN_SPRINT_MULTIPLIER = builder.comment("Extra drain multiplier while sprinting").defineInRange("sprintMultiplier", 2.0, 1.0, 10.0);
        THIRST_RAW_WATER_RESTORE = builder.comment("Thirst restored by raw/unpurified water").defineInRange("rawWaterRestore", 6.0, 0.0, 20.0);
        THIRST_PURIFIED_WATER_RESTORE = builder.comment("Thirst restored by purified water").defineInRange("purifiedWaterRestore", 14.0, 0.0, 20.0);
        THIRST_MAX_HYDRATION = builder.comment("Max hidden hydration from drinking past full (like hunger saturation). Drains before the bar does. 8 = 4 drops").defineInRange("maxHydration", 8.0, 0.0, 20.0);
        WATER_SIP_RESTORE = builder.comment("Thirst restored per sip when drinking straight from fresh water with an empty hand (2 = 1 drop)").defineInRange("waterSipRestore", 4.0, 0.0, 20.0);
        RAW_WATER_SICK_CHANCE = builder.comment("Chance that unpurified water (sips or plain water bottles) gives you Hunger (0-1)").defineInRange("rawWaterSickChance", 0.5, 0.0, 1.0);
        RAW_WATER_SICK_TICKS = builder.comment("How long that Hunger lasts (20 ticks = 1 second)").defineInRange("rawWaterSickTicks", 200, 1, 72000);
        PURITY_START = builder.comment("Water Purity everyone starts at (0-100). Low 0-24, Medium 25-54, Good 55-89, Excellent 90-100").defineInRange("purityStart", 70.0, 0.0, 100.0);
        PURITY_SHIFT = builder.comment("How hard a full bar (20 points) of one kind of water pulls the purity meter toward that water's target (0-1). Smaller drinks pull less").defineInRange("purityShift", 0.3, 0.0, 1.0);
        PURITY_LOW_WATER_LOSS = builder.comment("Max thirst lost while Water Purity is Low (2 = 1 drop)").defineInRange("purityLowWaterLoss", 2, 0, 18);
        PURITY_EXCELLENT_WATER_BONUS = builder.comment("Extra thirst bar room while Water Purity is Excellent (2 = 1 drop)").defineInRange("purityExcellentWaterBonus", 2, 0, 2);
        WELLBEING_HEALTH = builder.comment("Max health gained for keeping a Balanced diet and Good water a while (2 = 1 heart)").defineInRange("wellbeingHealth", 2.0, 0.0, 20.0);
        WELLBEING_BUFF_MINUTES = builder.comment("Real minutes of a Balanced diet and Good water (starting from neutral) to earn the bonus heart").defineInRange("wellbeingBuffMinutes", 10, 1, 1440);
        WELLBEING_DEBUFF_MINUTES = builder.comment("Real minutes of neglect (a food group at 0%, Low water, or body heat under 10 / over 95) starting from neutral before you lose a heart").defineInRange("wellbeingDebuffMinutes", 5, 1, 1440);
        WELLBEING_GRACE_MINUTES = builder.comment("Real minutes a fully topped-up player keeps the bonus heart after the diet or water slips").defineInRange("wellbeingGraceMinutes", 5, 0, 1440);
        WELLNESS_GRACE_SECONDS = builder.comment("Grace period: diet (food), purity (water) and wellbeing (hearts) bonuses/penalties only switch on or off after the new state has held this many seconds in a row").defineInRange("wellnessGraceSeconds", 180, 0, 3600);
        WELLNESS_FADE_SECONDS = builder.comment("Seconds a removed bonus heart / drumstick / water drop takes to fade away").defineInRange("wellnessFadeSeconds", 4, 1, 600);
        PURIFICATION_TICKS = builder.comment("Ticks for the Purification Table to purify one water bucket (20 ticks = 1 second)").defineInRange("purificationTicks", 400, 1, 72000);
        PURIFICATION_USES_PER_FUEL = builder.comment("Water buckets purified per piece of coal or charcoal").defineInRange("purificationUsesPerFuel", 2, 1, 64);
        JUICER_TICKS = builder.comment("Ticks for the Juicer to make one bucket of juice (20 ticks = 1 second)").defineInRange("juicerTicks", 600, 1, 72000);
        FERMENTER_TICKS = builder.comment("Ticks for the Fermenter to make one bucket of liquor (12000 = half a Minecraft day = 10 real minutes)").defineInRange("fermenterTicks", 12000, 1, 240000);
        FERMENTER_SUGAR_TICKS = builder.comment("Ticks of fermenting one sugar lasts (1800 = 1 minute 30 seconds)").defineInRange("fermenterSugarTicks", 1800, 1, 240000);
        builder.pop();

        builder.comment("Weaponsmith settings").push("weaponsmith");
        SWORD_HITS_BEFORE_DULL = builder.comment("Edge (hits before dull) of a sword that somehow has no rolled stats yet. Crafted, looted and traded swords roll their own edge (weaponsmith/SwordForging); anyone sharpens them at a Sword Whetstone").defineInRange("swordHitsBeforeDull", 200, 1, 100000);
        DULL_DAMAGE_MULTIPLIER = builder.comment("Damage a dull sword does, as a share of normal (0.5 = half)").defineInRange("dullDamageMultiplier", 0.5, 0.0, 1.0);
        PICKAXE_BLOCKS_BEFORE_DULL = builder.comment("Blocks an IRON pickaxe mines before it goes dull (half mining speed until a Pickaxe Whetstone sharpens it). Other materials scale from this: wood x0.25, gold x0.35, stone x0.5, copper x0.6, diamond x1.5, netherite x2. Obsidian counts as 100 blocks").defineInRange("pickaxeBlocksBeforeDull", 600, 1, 1000000);
        ARMOR_HITS_BEFORE_DENTED = builder.comment("Hits an IRON armor piece takes before it's dented (half protection until an Armor Whetstone fixes it). Other materials scale from this: leather x0.5, gold x0.6, chainmail and copper x0.75, diamond x1.5, netherite x2").defineInRange("armorHitsBeforeDented", 120, 1, 1000000);
        builder.pop();

        builder.comment("Difficulty settings").push("difficulty");
        HOSTILE_MOB_HEALTH_MULTIPLIER = builder.comment("Max health of Level I hostile mobs (most of them) and the real bosses (Wither, Ender Dragon, Warden, Elder Guardian), as a share of normal (1.2 = 20% more; 1.0 = vanilla). Mobs keep the value they spawned with").defineInRange("hostileMobHealthMultiplier", 1.2, 0.1, 100.0);
        LEVEL2_MOB_CHANCE = builder.comment("Chance a hostile mob spawns as Level II: 30% more health, hits 1.25x as hard, 1.4x the XP (0.3 = 30%)").defineInRange("level2MobChance", 0.30, 0.0, 1.0);
        BOSS_MOB_CHANCE = builder.comment("Chance a hostile mob spawns as a Boss: twice the health, hits 1.75x as hard, double loot and XP; a Boss skeleton shoots 1.5x as fast, a zombie walks 1.5x as fast, a spider poisons (0.1 = 10%)").defineInRange("bossMobChance", 0.10, 0.0, 1.0);
        builder.pop();

        builder.comment("Chef occupation settings").push("chef");
        GOURMET_GOLD_HAND = builder.comment("Gourmet food: share of the drumsticks that don't fit your bar that become GOLD drumsticks when eaten by hand").defineInRange("gourmetGoldHand", 0.5, 0.0, 5.0);
        GOURMET_GOLD_SILVER_PLATE = builder.comment("... when eaten off a Silver Plate").defineInRange("gourmetGoldSilverPlate", 1.0, 0.0, 5.0);
        GOURMET_GOLD_GOLDEN_PLATE = builder.comment("... when eaten off a Golden Plate").defineInRange("gourmetGoldGoldenPlate", 1.5, 0.0, 5.0);
        CHEF_GOURMET_HUNGER_MULTIPLIER = builder.comment("Hunger multiplier for Gourmet food (1.25 = 25% more)").defineInRange("gourmetHungerMult", 1.25, 1.0, 5.0);
        CHEF_GOURMET_SPEED_MULTIPLIER = builder.comment("Eat-speed multiplier for Gourmet food (1.25 = 25% faster)").defineInRange("gourmetSpeedMult", 1.25, 1.0, 5.0);
        CHEF_NERF_HUNGER_MULTIPLIER = builder.comment("Hunger multiplier for non-chef food when a chef is online (0.5 = half)").defineInRange("nerfHungerMult", 0.5, 0.1, 1.0);
        CHEF_NERF_SPEED_MULTIPLIER = builder.comment("Eat-speed multiplier for non-chef food when a chef is online (0.5 = twice as slow)").defineInRange("nerfSpeedMult", 0.5, 0.1, 1.0);
        builder.pop();

        builder.comment("Economy settings").push("economy");
        STARTING_BALANCE = builder.comment("Starting money for new players").defineInRange("startingBalance", 100, 0, Integer.MAX_VALUE);
        BANK_PRICES = builder.comment(
                "What the Banker pays, in gold NUGGETS (9 nuggets = 1 ingot, 81 = 1 block). \"item_id=N\" (N a piece) or \"item_id=N/M\" (N for M).",
                "Items not listed (or priced 0) can't be sold. Payouts come as blocks, then ingots, then nuggets.")
            .defineListAllowEmpty("bankPricesV5", DEFAULT_BANK_PRICES, () -> "minecraft:stone=0",
                o -> o instanceof String s && s.contains("="));
        LEADERBOARD_ENABLED = builder.comment("Show the Gold Leaderboard on the sidebar (gold in inventory + ender chest)").define("leaderboardEnabled", true);
        LEADERBOARD_INTERVAL_TICKS = builder.comment("How often the Gold Leaderboard updates (200 ticks = 10 seconds)").defineInRange("leaderboardIntervalTicks", 200, 20, 72000);
        builder.pop();

        builder.comment("Wellness settings (diet)").push("wellness");
        DIET_KEEP_PER_MINUTE = builder.comment("How much of each food group is kept every minute (1.0 = never fades; the diet window already keeps it recent. 0.97 = fades 3% a minute)").defineInRange("dietKeepPerMinute", 1.0, 0.5, 1.0);
        DIET_MIN_POINTS = builder.comment("Diet points needed before a bonus or penalty kicks in (1 hunger point eaten = 1 diet point)").defineInRange("dietMinPoints", 12.0, 0.0, 1000.0);
        DIET_BALANCED_SCORE = builder.comment("Balance at or above this = Balanced diet bonus (0-1, 1 = exactly the ideal 10% veg / 10% fruit / 65% protein / 15% carbs)").defineInRange("dietBalancedScore", 0.86, 0.0, 1.0);
        DIET_POOR_SCORE = builder.comment("Balance at or below this = Poor diet penalty (0-1)").defineInRange("dietPoorScore", 0.44, 0.0, 1.0);
        DIET_BALANCED_FOOD_BONUS = builder.comment("Extra food bar room with a Balanced diet (2 = 1 drumstick)").defineInRange("dietBalancedFoodBonus", 2, 0, 2);
        DIET_START_POINTS = builder.comment("New players start with this many diet points, split into the ideal mix (a balanced head start, so a couple of apples do not wreck the chart)").defineInRange("dietStartPoints", 130.0, 0.0, 1000.0);
        DIET_WINDOW_POINTS = builder.comment("The diet remembers the last this-many diet points (1 hunger point eaten = 1 diet point). Bigger = each food moves the chart less (a 2-point melon slice is about 1.5% of 130)").defineInRange("dietWindowPoints", 130.0, 10.0, 10000.0);
        DIET_MEALS_BEFORE_EFFECTS = builder.comment("Meals a new player must eat before diet buffs/debuffs start (the head start alone gives nothing)").defineInRange("dietMealsBeforeEffects", 20, 0, 10000);
        DIET_POOR_FOOD_LOSS = builder.comment("Max food lost with a Poor diet (2 = 1 drumstick)").defineInRange("dietPoorFoodLoss", 2, 0, 18);
        builder.pop();

        builder.comment("Occupation settings").push("occupations");
        JOB_CHANGE_COST = builder.comment("Money cost to change jobs").defineInRange("jobChangeCost", 500, 0, Integer.MAX_VALUE);
        JOB_CHANGE_COOLDOWN_DAYS = builder.comment("Real-life days before a player can change jobs again").defineInRange("jobChangeCooldownDays", 7, 0, 365);
        NEW_PLAYER_GRACE_MINUTES = builder.comment("Minutes after first join during which global nerfs don't apply").defineInRange("newPlayerGraceMinutes", 120, 0, 1440);
        builder.pop();

        builder.comment("The town (Overworld). Corners are inclusive; either order works.").push("town");
        TOWN_WALK_MIN_X = builder.comment("Where the Whisperer wanders: one corner x").defineInRange("walkX1", 557828, -30000000, 30000000);
        TOWN_WALK_MIN_Z = builder.comment("Where the Whisperer wanders: one corner z").defineInRange("walkZ1", -849933, -30000000, 30000000);
        TOWN_WALK_MAX_X = builder.comment("Where the Whisperer wanders: the other corner x").defineInRange("walkX2", 557917, -30000000, 30000000);
        TOWN_WALK_MAX_Z = builder.comment("Where the Whisperer wanders: the other corner z").defineInRange("walkZ2", -849826, -30000000, 30000000);
        TOWN_PROTECT_MIN_X = builder.comment("Protected town center (nobody but ops breaks blocks): one corner x").defineInRange("protectX1", 557796, -30000000, 30000000);
        TOWN_PROTECT_MIN_Z = builder.comment("Protected town center: one corner z").defineInRange("protectZ1", -849948, -30000000, 30000000);
        TOWN_PROTECT_MAX_X = builder.comment("Protected town center: the other corner x").defineInRange("protectX2", 557966, -30000000, 30000000);
        TOWN_PROTECT_MAX_Z = builder.comment("Protected town center: the other corner z").defineInRange("protectZ2", -849791, -30000000, 30000000);
        TOWN_PROTECT_MIN_Y = builder.comment("Protected from this height up to the sky (the town is at about y 63, so 53 = 10 blocks under the surface)").defineInRange("protectMinY", 53, -64, 320);
        builder.pop();

        builder.comment("World event settings").push("events");
        builder.pop();

        builder.comment("The death tax and Not-A-Scam Insurance (Insurance.md). Prices are whole gold; rates are the percent of a player's gold owed when they die").push("insurance");
        DEATH_TAX_PERCENT = builder.comment("Death tax without insurance (and during a Blood Moon without Blood Moon Insurance), percent").defineInRange("deathTaxPercent", 25, 0, 100);
        INSURANCE_BRONZE_PRICE = builder.comment("Bronze plan: gold a day").defineInRange("bronzePrice", 1, 0, 100000);
        INSURANCE_BRONZE_RATE = builder.comment("Bronze plan: death tax percent").defineInRange("bronzeRate", 20, 0, 100);
        INSURANCE_SILVER_PRICE = builder.comment("Silver plan: gold a day").defineInRange("silverPrice", 3, 0, 100000);
        INSURANCE_SILVER_RATE = builder.comment("Silver plan: death tax percent").defineInRange("silverRate", 15, 0, 100);
        INSURANCE_GOLD_PRICE = builder.comment("Gold plan: gold a day").defineInRange("goldPrice", 7, 0, 100000);
        INSURANCE_GOLD_RATE = builder.comment("Gold plan: death tax percent").defineInRange("goldRate", 10, 0, 100);
        INSURANCE_PREMIUM_PRICE = builder.comment("Premium plan: gold a day").defineInRange("premiumPrice", 20, 0, 100000);
        INSURANCE_PREMIUM_RATE = builder.comment("Premium plan: death tax percent").defineInRange("premiumRate", 5, 0, 100);
        BLOOD_MOON_INSURANCE_PRICE = builder.comment("Blood Moon Insurance: one-time gold, covers every death during the next Blood Moon that starts after buying it").defineInRange("bloodMoonInsurancePrice", 30, 0, 100000);
        builder.pop();

        SPEC = builder.build();
    }

    public static void register(ModContainer container) {
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER, SPEC, "capitalism-server.toml");
    }
}
