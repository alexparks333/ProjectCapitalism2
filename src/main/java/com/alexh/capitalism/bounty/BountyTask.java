package com.alexh.capitalism.bounty;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

// Every bounty the Bounty Clerk hands out (Bounties.md > The tasks). Each has a range of amounts (rolled when it's
// offered) and a range of gold to match (the more to do, the more it pays, in proportion), a tier, how it counts
// (Kind, BountyEvents), what it's about (entity, block, item ids or "#tag" ids) and, for the town's own tasks, when it
// can be offered at all (a pet to feed, a Cook's shopfront to buy from...). A delivery always pays more than the Banker
// would for its items (Bounties.reward). Ids are saved on players: keep them; add new ones anywhere.
public final class BountyTask {

    public enum Tier {
        EASY("Easy", 0x55FF55, 40), MEDIUM("Medium", 0x5599FF, 30), HARD("Hard", 0xC060FF, 20), HEROIC("Heroic", 0xFFAA00, 10);

        public final String title;
        public final int color;
        // how often it's offered (out of 100)
        public final int weight;

        Tier(String title, int color, int weight) {
            this.title = title;
            this.color = color;
            this.weight = weight;
        }
    }

    // how a task counts (BountyEvents)
    public enum Kind {
        KILL, KILL_AT_NIGHT, MINE, HARVEST, PLANT, BREED, SHEAR, MILK, CATCH, CATCH_ITEM, CATCH_SET, TREASURE, DELIVER,
        WALK, BOAT, HORSE, TRAVEL, REACH_LOW, REACH_HIGH, BIOMES, MAIL, SHOP_BUY, SHOP_SELL, COOK_BUY, PET_FEED, TRADE,
        SMELT, BREW, ENCHANT, REPAIR, TAME, BLOOD_MONSTER, RARE_FISH, EPIC_FISH, UNCORK, CRACK_CHEST, LOOT_CHEST, VAULT,
        OMINOUS_VAULT, NETHER_RETURN, HELP_KILL, RAID_WIN, BLOOD_SURVIVE, BLOOD_KILLS, BLOOD_TOP
    }

    public final String id;
    private final String title;
    public final Tier tier;
    public final Kind kind;
    public final int min, max;
    private final int goldMin, goldMax;
    public final List<String> targets;
    private Predicate<ServerPlayer> available = p -> true;
    private boolean conditional;

    private BountyTask(String id, String title, Tier tier, Kind kind, int min, int max, int goldMin, int goldMax, String... targets) {
        this.id = id;
        this.title = title;
        this.tier = tier;
        this.kind = kind;
        this.min = min;
        this.max = max;
        this.goldMin = goldMin;
        this.goldMax = goldMax;
        this.targets = List.of(targets);
    }

    // "Kill %s Zombies" -> "Kill 6 Zombies"
    public String title(int amount) {
        return this.title.contains("%s") ? String.format(this.title, String.format(Locale.ROOT, "%,d", amount)) : this.title;
    }

    public int rollAmount(RandomSource random) {
        return this.min >= this.max ? this.min : this.min + random.nextInt(this.max - this.min + 1);
    }

    // its gold for this amount (nuggets): in proportion between its two ends, plus its tier's bonus
    public long goldFor(int amount) {
        double t = this.max <= this.min ? 0 : (amount - this.min) / (double) (this.max - this.min);
        return Math.round((this.goldMin + (this.goldMax - this.goldMin) * t + TIER_BONUS[this.tier.ordinal()]) * 9.0);
    }

    // gold added to every task's pay by tier (Easy, Medium, Hard, Heroic), so bounties are a fair way to earn a little
    public static final int[] TIER_BONUS = {2, 3, 4, 5};

    public boolean availableTo(ServerPlayer player) {
        return this.available.test(player);
    }

    // a town task: only for those who can do it (a pet, a Shopfront...)
    public boolean conditional() {
        return this.conditional;
    }

    BountyTask needs(Predicate<ServerPlayer> when) {
        this.available = when;
        this.conditional = true;
        return this;
    }

    public boolean matchesEntity(EntityType<?> type) {
        return this.targets.contains(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
    }

    public boolean matchesItem(Item item) {
        return this.targets.contains(BuiltInRegistries.ITEM.getKey(item).toString());
    }

    // the item a delivery wants
    public Item item() {
        if (this.targets.isEmpty()) return Items.AIR;
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(this.targets.get(0)));
    }

    // ── the catalogue ──

    private static final List<BountyTask> ALL = new ArrayList<>();

    public static List<BountyTask> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static BountyTask byId(String id) {
        for (BountyTask t : ALL) if (t.id.equals(id)) return t;
        return null;
    }

    private static BountyTask t(String id, String title, Tier tier, Kind kind, int min, int max, int goldMin, int goldMax, String... targets) {
        BountyTask task = new BountyTask(id, title, tier, kind, min, max, goldMin, goldMax, targets);
        ALL.add(task);
        return task;
    }

    private static void kill(Tier tier, String id, String what, int min, int max, int gMin, int gMax, String... entities) {
        t(id, "Kill %s " + what, tier, Kind.KILL, min, max, gMin, gMax, entities);
    }

    private static void mine(Tier tier, String id, String what, int min, int max, int gMin, int gMax, String... blocks) {
        t(id, "Mine %s " + what, tier, Kind.MINE, min, max, gMin, gMax, blocks);
    }

    // "Chop %s Oak logs"
    private static void chop(Tier tier, String id, String wood, int min, int max, int gMin, int gMax, String log) {
        t(id, "Chop %s " + wood + " logs", tier, Kind.MINE, min, max, gMin, gMax, log);
    }

    private static void deliver(Tier tier, String id, String what, int min, int max, int gMin, int gMax, String item) {
        t(id, "Deliver %s " + what, tier, Kind.DELIVER, min, max, gMin, gMax, item);
    }

    // Every task is about fighting, mining, wood, meat, fish, the Nether or the town's trade. (Niche ones were cut on
    // 2026-11-19: taming, milking, shearing, breeding, planting, sand / gravel / dirt, mail, villager trades, odd
    // deliveries; a player still holding one keeps it until their next task.) The gold numbers below are the base:
    // goldFor() adds the tier's bonus on top (TIER_BONUS).
    static {
        Tier E = Tier.EASY, M = Tier.MEDIUM, H = Tier.HARD, X = Tier.HEROIC;
        // ── Easy: 3-5 gold base (5-7 paid) ──
        kill(E, "kill_zombies", "Zombies", 4, 7, 3, 5, "minecraft:zombie");
        kill(E, "kill_skeletons", "Skeletons", 4, 7, 3, 5, "minecraft:skeleton");
        kill(E, "kill_spiders", "Spiders", 4, 7, 3, 5, "minecraft:spider");
        kill(E, "kill_creepers_few", "Creepers", 2, 4, 3, 5, "minecraft:creeper");
        kill(E, "kill_drowned", "Drowned", 4, 7, 3, 5, "minecraft:drowned");
        kill(E, "kill_husks", "Husks", 4, 7, 3, 5, "minecraft:husk");
        kill(E, "kill_strays", "Strays", 4, 7, 3, 5, "minecraft:stray");
        kill(E, "kill_slimes", "Slimes", 3, 6, 3, 5, "minecraft:slime");
        kill(E, "kill_cave_spiders", "Cave Spiders", 4, 7, 3, 5, "minecraft:cave_spider");
        t("kill_skeletons_night", "Kill %s Skeletons at night", E, Kind.KILL_AT_NIGHT, 5, 8, 3, 5, "minecraft:skeleton");
        mine(E, "mine_coal", "Coal Ore", 16, 24, 3, 4, "minecraft:coal_ore", "minecraft:deepslate_coal_ore");
        mine(E, "mine_copper", "Copper Ore", 12, 20, 3, 4, "minecraft:copper_ore", "minecraft:deepslate_copper_ore");
        mine(E, "mine_iron_few", "Iron Ore", 3, 5, 3, 5, "minecraft:iron_ore", "minecraft:deepslate_iron_ore");
        mine(E, "mine_stone", "Stone", 48, 80, 3, 4, "minecraft:stone");
        // wood: any logs, or one kind
        t("chop_logs", "Chop %s logs", E, Kind.MINE, 24, 40, 3, 4, "#minecraft:logs");
        chop(E, "chop_oak", "Oak", 24, 40, 3, 4, "minecraft:oak_log");
        chop(E, "chop_birch", "Birch", 24, 40, 3, 4, "minecraft:birch_log");
        chop(E, "chop_spruce", "Spruce", 24, 40, 3, 4, "minecraft:spruce_log");
        // meat: raw, any way you get it (a Farmer gets the full drops; everyone else only 1 a kill, or buys it)
        deliver(E, "deliver_beef", "Raw Beef", 4, 6, 3, 5, "minecraft:beef");
        deliver(E, "deliver_porkchops", "Raw Porkchops", 4, 6, 3, 5, "minecraft:porkchop");
        deliver(E, "deliver_chicken", "Raw Chicken", 4, 6, 3, 5, "minecraft:chicken");
        deliver(E, "deliver_mutton", "Raw Mutton", 4, 6, 3, 5, "minecraft:mutton");
        // mob drops
        deliver(E, "deliver_bones", "Bones", 12, 24, 3, 4, "minecraft:bone");
        deliver(E, "deliver_string", "String", 24, 40, 3, 4, "minecraft:string");
        deliver(E, "deliver_leather", "Leather", 6, 10, 3, 5, "minecraft:leather");
        // farming and fishing
        t("harvest_wheat", "Harvest %s Wheat", E, Kind.HARVEST, 24, 40, 3, 4, "minecraft:wheat");
        t("harvest_carrots", "Harvest %s Carrots", E, Kind.HARVEST, 24, 40, 3, 4, "minecraft:carrots");
        t("harvest_potatoes", "Harvest %s Potatoes", E, Kind.HARVEST, 24, 40, 3, 4, "minecraft:potatoes");
        t("catch_fish", "Catch %s fish", E, Kind.CATCH, 8, 12, 3, 5);
        t("catch_salmon", "Catch %s Salmon", E, Kind.CATCH_ITEM, 4, 6, 3, 5, "minecraft:salmon");
        t("catch_cod", "Catch %s Cod", E, Kind.CATCH_ITEM, 6, 10, 3, 5, "minecraft:cod");
        // the town's trade
        t("shop_buy", "Buy anything from a player's Shopfront", E, Kind.SHOP_BUY, 1, 1, 3, 3).needs(p -> ShopIndex.anyOther(p));

        // ── Medium: 6-12 gold base (9-15 paid) ──
        kill(M, "kill_creepers", "Creepers", 8, 12, 7, 10, "minecraft:creeper");
        t("kill_spiders_night", "Kill %s Spiders at night", M, Kind.KILL_AT_NIGHT, 6, 10, 6, 9, "minecraft:spider", "minecraft:cave_spider");
        t("kill_creepers_night", "Kill %s Creepers at night", M, Kind.KILL_AT_NIGHT, 5, 8, 7, 10, "minecraft:creeper");
        kill(M, "kill_zombies_many", "Zombies", 12, 18, 6, 9, "minecraft:zombie", "minecraft:husk", "minecraft:drowned");
        kill(M, "kill_skeletons_many", "Skeletons", 12, 18, 6, 9, "minecraft:skeleton", "minecraft:stray", "minecraft:bogged");
        kill(M, "kill_drowned_many", "Drowned", 8, 12, 6, 8, "minecraft:drowned");
        kill(M, "kill_witches", "Witches", 2, 4, 8, 12, "minecraft:witch");
        kill(M, "kill_pillagers", "Pillagers", 4, 6, 8, 11, "minecraft:pillager");
        kill(M, "kill_phantoms", "Phantoms", 2, 4, 8, 11, "minecraft:phantom");
        kill(M, "kill_guardians", "Guardians", 4, 6, 8, 11, "minecraft:guardian");
        kill(M, "kill_endermen_few", "Endermen", 2, 4, 7, 10, "minecraft:enderman");
        // the Nether
        t("nether_return", "Visit the Nether and come back alive", M, Kind.NETHER_RETURN, 1, 1, 9, 9);
        kill(M, "kill_magma_cubes", "Magma Cubes", 4, 7, 7, 10, "minecraft:magma_cube");
        kill(M, "kill_zombified_piglins", "Zombified Piglins", 4, 7, 6, 8, "minecraft:zombified_piglin");
        kill(M, "kill_hoglins", "Hoglins", 3, 5, 8, 11, "minecraft:hoglin");
        kill(M, "kill_piglins", "Piglins", 4, 7, 7, 10, "minecraft:piglin");
        mine(M, "mine_quartz", "Nether Quartz Ore", 20, 32, 6, 9, "minecraft:nether_quartz_ore");
        mine(M, "mine_glowstone", "Glowstone", 16, 32, 7, 10, "minecraft:glowstone");
        mine(M, "mine_nether_gold", "Nether Gold Ore", 12, 20, 6, 8, "minecraft:nether_gold_ore");
        t("chop_crimson", "Chop %s Crimson Stems", M, Kind.MINE, 16, 32, 7, 10, "minecraft:crimson_stem");
        t("chop_warped", "Chop %s Warped Stems", M, Kind.MINE, 16, 32, 7, 10, "minecraft:warped_stem");
        // mining
        mine(M, "mine_iron", "Iron Ore", 8, 12, 6, 9, "minecraft:iron_ore", "minecraft:deepslate_iron_ore");
        mine(M, "mine_gold", "Gold Ore", 6, 10, 8, 11, "minecraft:gold_ore", "minecraft:deepslate_gold_ore");
        mine(M, "mine_lapis", "Lapis Ore", 10, 14, 6, 8, "minecraft:lapis_ore", "minecraft:deepslate_lapis_ore");
        mine(M, "mine_redstone", "Redstone Ore", 12, 20, 6, 8, "minecraft:redstone_ore", "minecraft:deepslate_redstone_ore");
        mine(M, "mine_copper_many", "Copper Ore", 32, 48, 6, 8, "minecraft:copper_ore", "minecraft:deepslate_copper_ore");
        mine(M, "mine_obsidian", "Obsidian", 8, 12, 8, 11, "minecraft:obsidian");
        t("reach_depths", "Reach Y -50", M, Kind.REACH_LOW, 1, 1, 7, 7);
        t("smelt_iron", "Smelt %s Iron Ingots", M, Kind.SMELT, 24, 40, 7, 10, "minecraft:iron_ingot");
        t("smelt_gold", "Smelt %s Gold Ingots", M, Kind.SMELT, 8, 16, 7, 10, "minecraft:gold_ingot");
        // wood
        chop(M, "chop_dark_oak", "Dark Oak", 24, 40, 6, 9, "minecraft:dark_oak_log");
        chop(M, "chop_jungle", "Jungle", 24, 40, 6, 9, "minecraft:jungle_log");
        chop(M, "chop_acacia", "Acacia", 24, 40, 6, 9, "minecraft:acacia_log");
        chop(M, "chop_mangrove", "Mangrove", 24, 40, 7, 10, "minecraft:mangrove_log");
        chop(M, "chop_cherry", "Cherry", 24, 40, 7, 10, "minecraft:cherry_log");
        // meat
        deliver(M, "deliver_beef_many", "Raw Beef", 10, 16, 7, 10, "minecraft:beef");
        deliver(M, "deliver_porkchops_many", "Raw Porkchops", 10, 16, 7, 10, "minecraft:porkchop");
        deliver(M, "deliver_rabbit", "Raw Rabbit", 4, 6, 7, 10, "minecraft:rabbit");
        t("cook_meat", "Cook %s meat", M, Kind.SMELT, 12, 20, 6, 8, "minecraft:cooked_beef", "minecraft:cooked_porkchop", "minecraft:cooked_chicken",
            "minecraft:cooked_mutton", "minecraft:cooked_rabbit");
        t("cook_fish", "Cook %s fish", M, Kind.SMELT, 12, 20, 6, 8, "minecraft:cooked_cod", "minecraft:cooked_salmon");
        // mob drops
        deliver(M, "deliver_gunpowder", "Gunpowder", 12, 20, 7, 10, "minecraft:gunpowder");
        deliver(M, "deliver_pearls_few", "Ender Pearls", 3, 5, 7, 10, "minecraft:ender_pearl");
        deliver(M, "deliver_arrows", "Arrows", 32, 64, 6, 8, "minecraft:arrow");
        // fishing, travel, the town's trade
        t("catch_fish_many", "Catch %s fish", M, Kind.CATCH, 20, 30, 7, 10);
        t("fish_treasure", "Fish up %s treasures", M, Kind.TREASURE, 2, 4, 8, 12, "minecraft:enchanted_book", "minecraft:name_tag", "minecraft:saddle",
            "minecraft:nautilus_shell", "minecraft:bow", "minecraft:fishing_rod");
        t("travel", "Travel %s blocks", M, Kind.TRAVEL, 1500, 2500, 6, 9);
        t("enchant", "Enchant any item", M, Kind.ENCHANT, 1, 1, 7, 7);
        t("shop_sell", "Sell something from your Shopfront", M, Kind.SHOP_SELL, 1, 1, 7, 7).needs(ShopIndex::owns);
        t("cook_buy", "Buy a meal from a Cook's Shopfront", M, Kind.COOK_BUY, 1, 1, 7, 7).needs(ShopIndex::anyCook);

        // ── Hard: 10-20 gold base (14-24 paid) ──
        kill(H, "kill_endermen", "Endermen", 12, 18, 14, 19, "minecraft:enderman");
        kill(H, "kill_witches_many", "Witches", 5, 8, 12, 16, "minecraft:witch");
        kill(H, "kill_guardians_many", "Guardians", 10, 15, 12, 16, "minecraft:guardian");
        kill(H, "kill_phantoms_many", "Phantoms", 6, 10, 12, 16, "minecraft:phantom");
        kill(H, "kill_pillagers_many", "Pillagers", 10, 15, 12, 16, "minecraft:pillager");
        kill(H, "kill_creepers_many", "Creepers", 20, 30, 12, 16, "minecraft:creeper");
        kill(H, "kill_vindicators", "Vindicators", 2, 4, 12, 16, "minecraft:vindicator");
        t("kill_ravager", "Kill a Ravager", H, Kind.KILL, 1, 1, 16, 16, "minecraft:ravager");
        t("kill_evoker", "Kill an Evoker", H, Kind.KILL, 1, 1, 16, 16, "minecraft:evoker");
        kill(H, "kill_breezes", "Breezes", 2, 4, 12, 16, "minecraft:breeze");
        kill(H, "kill_shulkers", "Shulkers", 2, 4, 13, 17, "minecraft:shulker");
        t("blood_monster", "Kill a Blood Moon Level II monster", H, Kind.BLOOD_MONSTER, 1, 1, 12, 12);
        t("blood_survive", "Survive a whole Blood Moon without dying", H, Kind.BLOOD_SURVIVE, 1, 1, 15, 15);
        t("blood_kills", "Get %s kills in one Blood Moon", H, Kind.BLOOD_KILLS, 20, 30, 14, 19);
        // the Nether
        kill(H, "kill_blazes", "Blazes", 4, 6, 12, 16, "minecraft:blaze");
        kill(H, "kill_wither_skeletons", "Wither Skeletons", 4, 6, 13, 17, "minecraft:wither_skeleton");
        kill(H, "kill_ghasts", "Ghasts", 2, 4, 12, 17, "minecraft:ghast");
        kill(H, "kill_brutes", "Piglin Brutes", 3, 5, 14, 19, "minecraft:piglin_brute");
        mine(H, "mine_debris", "Ancient Debris", 2, 3, 16, 20, "minecraft:ancient_debris");
        t("loot_fortress", "Loot a Nether Fortress chest", H, Kind.LOOT_CHEST, 1, 1, 14, 14, "chests/nether_bridge");
        t("loot_bastion", "Loot a Bastion chest", H, Kind.LOOT_CHEST, 1, 1, 18, 18, "chests/bastion");
        deliver(H, "deliver_blaze_rods", "Blaze Rods", 6, 10, 12, 16, "minecraft:blaze_rod");
        deliver(H, "deliver_tears", "Ghast Tears", 2, 4, 14, 20, "minecraft:ghast_tear");
        // mining
        mine(H, "mine_diamonds", "Diamond Ore", 4, 6, 14, 19, "minecraft:diamond_ore", "minecraft:deepslate_diamond_ore");
        mine(H, "mine_emeralds", "Emerald Ore", 2, 4, 13, 18, "minecraft:emerald_ore", "minecraft:deepslate_emerald_ore");
        mine(H, "mine_gold_many", "Gold Ore", 16, 24, 12, 16, "minecraft:gold_ore", "minecraft:deepslate_gold_ore");
        deliver(H, "deliver_diamonds", "Diamonds", 2, 3, 14, 20, "minecraft:diamond");
        deliver(H, "deliver_emeralds", "Emeralds", 2, 3, 12, 18, "minecraft:emerald");
        // meat and wood in bulk
        deliver(H, "deliver_beef_bulk", "Raw Beef", 24, 32, 12, 16, "minecraft:beef");
        deliver(H, "deliver_mutton_bulk", "Raw Mutton", 24, 32, 12, 16, "minecraft:mutton");
        t("chop_logs_bulk", "Chop %s logs", H, Kind.MINE, 128, 192, 12, 16, "#minecraft:logs");
        // structures, rare fish
        t("loot_shipwreck", "Loot a Shipwreck chest", H, Kind.LOOT_CHEST, 1, 1, 12, 12, "chests/shipwreck");
        t("loot_temple", "Loot a Desert Temple chest", H, Kind.LOOT_CHEST, 1, 1, 14, 14, "chests/desert_pyramid");
        t("loot_ruin", "Loot an Ocean Ruin chest", H, Kind.LOOT_CHEST, 1, 1, 12, 12, "chests/underwater_ruin");
        t("loot_stronghold", "Loot a Stronghold chest", H, Kind.LOOT_CHEST, 1, 1, 16, 16, "chests/stronghold");
        t("loot_jungle", "Loot a Jungle Temple chest", H, Kind.LOOT_CHEST, 1, 1, 14, 14, "chests/jungle_temple");
        t("loot_mansion", "Loot a Woodland Mansion chest", H, Kind.LOOT_CHEST, 1, 1, 18, 18, "chests/woodland_mansion");
        t("loot_ancient_city", "Loot an Ancient City chest", H, Kind.LOOT_CHEST, 1, 1, 20, 20, "chests/ancient_city");
        t("trial_vault", "Open a Trial Chamber Vault", H, Kind.VAULT, 1, 1, 15, 15);
        t("rare_fish", "Catch any rare fish", H, Kind.RARE_FISH, 1, 1, 14, 14);

        // ── Heroic: 30-50 gold base (40-55 paid), rare offers ──
        t("slay_revenant", "Help slay the Crimson Revenant", X, Kind.HELP_KILL, 1, 1, 45, 45, "capitalism:crimson_revenant");
        t("slay_witch", "Help slay the Sanguine Witch", X, Kind.HELP_KILL, 1, 1, 45, 45, "capitalism:sanguine_witch");
        t("slay_gorefang", "Help slay Gorefang", X, Kind.HELP_KILL, 1, 1, 45, 45, "capitalism:gorefang");
        t("slay_wither", "Help slay the Wither", X, Kind.HELP_KILL, 1, 1, 50, 50, "minecraft:wither");
        t("slay_dragon", "Help slay the Ender Dragon", X, Kind.HELP_KILL, 1, 1, 50, 50, "minecraft:ender_dragon");
        t("slay_warden", "Slay a Warden", X, Kind.HELP_KILL, 1, 1, 50, 50, "minecraft:warden");
        t("slay_elder", "Slay an Elder Guardian", X, Kind.HELP_KILL, 1, 1, 35, 35, "minecraft:elder_guardian");
        t("win_raid", "Win a Raid (Hero of the Village)", X, Kind.RAID_WIN, 1, 1, 40, 40);
        t("blood_top", "Top a Blood Moon's kill board", X, Kind.BLOOD_TOP, 1, 1, 40, 40);
        kill(X, "kill_wither_skeleton_army", "Wither Skeletons", 15, 20, 35, 45, "minecraft:wither_skeleton");
        kill(X, "kill_enderman_army", "Endermen", 40, 50, 35, 45, "minecraft:enderman");
        t("kill_ravagers", "Kill 3 Ravagers", X, Kind.KILL, 3, 3, 40, 40, "minecraft:ravager");
        t("kill_evokers", "Kill 3 Evokers", X, Kind.KILL, 3, 3, 40, 40, "minecraft:evoker");
        mine(X, "mine_debris_many", "Ancient Debris", 6, 8, 40, 50, "minecraft:ancient_debris");
        t("ominous_vault", "Open an Ominous Vault", X, Kind.OMINOUS_VAULT, 1, 1, 35, 35);
        t("epic_fish", "Catch an Epic or rarer fish", X, Kind.EPIC_FISH, 1, 1, 35, 35);
        t("loot_end_city", "Loot an End City chest", X, Kind.LOOT_CHEST, 1, 1, 40, 40, "chests/end_city_treasure");
        deliver(X, "deliver_skulls", "Wither Skeleton Skulls", 3, 3, 45, 45, "minecraft:wither_skeleton_skull");
        t("deliver_trident", "Deliver a Trident", X, Kind.DELIVER, 1, 1, 35, 35, "minecraft:trident");
        t("deliver_totem", "Deliver a Totem of Undying", X, Kind.DELIVER, 1, 1, 40, 40, "minecraft:totem_of_undying");
        t("deliver_heart_of_sea", "Deliver a Heart of the Sea", X, Kind.DELIVER, 1, 1, 40, 40, "minecraft:heart_of_the_sea");
        deliver(X, "deliver_shulker_shells", "Shulker Shells", 4, 4, 40, 40, "minecraft:shulker_shell");
        deliver(X, "deliver_echo_shards", "Echo Shards", 4, 6, 35, 45, "minecraft:echo_shard");
    }
}
