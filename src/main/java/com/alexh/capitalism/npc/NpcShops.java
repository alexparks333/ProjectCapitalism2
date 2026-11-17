package com.alexh.capitalism.npc;

import com.alexh.capitalism.block.LiquorType;
import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.occupation.CookMadeFood;
import com.alexh.capitalism.occupation.Job;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.function.Supplier;

// What each job's NPC sells (NPCs.md). Prices here are the FAIR price a real player with the job might ask, in gold
// nuggets (9 = 1 gold); the shop charges economy/NpcPricing.npcPrice of it (+50% NPC markup, minus Emerald discounts).
// Stock is limited (maxStock, shared by everyone) and refills every 20 minutes (NpcStock). Each shop has tabs (sections).
// RULE: NPCs never sell anything with effects (meals or drinks that give an effect when eaten / drunk): those are
// only made by real Cooks and Bartenders, so players still need them. Jobs without stock yet: their NPC just says so.
public final class NpcShops {

    // maxStock: how many there are per restock, shared by everyone (NpcStock); 0 = no limit.
    // cookMade: vanilla food sold as Cook-made (full strength, Gourmet; occupation/CookMadeFood).
    // per: how many come in one purchase (a bundle; fairNuggets and maxStock count bundles).
    // forever: maxStock is all there will ever be, shared by everyone; it never restocks (the Legendary boats).
    public record Offer(Supplier<? extends Item> item, long fairNuggets, int maxStock, boolean cookMade, int per, boolean forever) {
        public Offer(Supplier<? extends Item> item, long fairNuggets, int maxStock, boolean cookMade, int per) {
            this(item, fairNuggets, maxStock, cookMade, per, false);
        }

        public Offer(Supplier<? extends Item> item, long fairNuggets, int maxStock) {
            this(item, fairNuggets, maxStock, false, 1);
        }

        public Offer(Supplier<? extends Item> item, long fairNuggets, int maxStock, boolean cookMade) {
            this(item, fairNuggets, maxStock, cookMade, 1);
        }

        public ItemStack stack() {
            ItemStack stack = new ItemStack(this.item.get(), this.per);
            if (this.cookMade) CookMadeFood.makeCookMade(stack);
            return stack;
        }
    }

    // repairs = the smiths' Repair tab (NpcRepairs): no offers, it lists what you carry that the NPC can fix
    public record Section(String title, List<Offer> offers, boolean repairs) {
        public Section(String title, List<Offer> offers) {
            this(title, offers, false);
        }

        public static Section repairTab() {
            return new Section("Repair", List.of(), true);
        }
    }

    private static final int GOLD = 9;
    // A third of a gold ingot (3 nuggets): Cook prices are counted in these
    private static final int THIRD = GOLD / 3;
    // per restock: 1 of each big meal, 3 of each bite, so nobody can stock up and skip the real Cook for good
    private static final int MEAL_STOCK = 1;
    private static final int BITE_STOCK = 3;
    private static final int COOKED_STOCK = 30;
    // Training Feed: bundles of 5, 8 bundles per restock, 6 nuggets fair (1 gold at the NPC)
    private static final int FEED_PER = 5;
    private static final int FEED_STOCK = 8;
    private static final int FEED_FAIR = 6;
    // Wheat, Raw Beef, Raw Salmon and Seeds for animals: 16 bundles of each per restock
    private static final int ANIMAL_FOOD_STOCK = 16;
    // the Bartender: 3 of each drink, 75 bottles of Purified Water per restock
    private static final int DRINK_STOCK = 3;
    private static final int WATER_STOCK = 75;
    // the smiths: 8 Whetstones per restock, 3 gold each at the NPC
    private static final int WHETSTONE_STOCK = 8;
    private static final int WHETSTONE_FAIR = 2 * GOLD;

    private NpcShops() {
    }

    // The Boat Master's shop key (npc/BoatMaster; not a job)
    public static final String BOAT_MASTER = "boat_master";

    // A shop by its key: a job's id, or BOAT_MASTER
    public static List<Section> sections(String shop) {
        if (BOAT_MASTER.equals(shop)) return BOATS;
        Job job = Job.byId(shop);
        return job == null ? List.of() : sections(job);
    }

    public static List<Offer> all(String shop) {
        return sections(shop).stream().flatMap(section -> section.offers().stream()).toList();
    }

    public static List<Section> sections(Job job) {
        return switch (job) {
            case COOK -> COOK;
            case BARTENDER -> BARTENDER;
            case WEAPONSMITH -> smith(ModItems.WHETSTONE);
            case TOOLSMITH -> smith(ModItems.PICKAXE_WHETSTONE);
            case ARMORSMITH -> smith(ModItems.ARMOR_WHETSTONE);
            default -> List.of();
        };
    }

    // ── The Cook: big Oven meals for a lot of gold, smaller Gourmet bites and Cook-made cooked food for a little. ──
    private static final List<Section> COOK = List.of(
        new Section("Meals", List.of(
            new Offer(ModItems.GOLDEN_GLAZED_FEAST, 16 * THIRD, MEAL_STOCK), // 8 gold at the NPC
            new Offer(ModItems.STUFFED_PUMPKIN, 12 * THIRD, MEAL_STOCK),
            new Offer(ModItems.LAMB_CHOPS, 10 * THIRD, MEAL_STOCK),
            new Offer(ModItems.CHICKEN_POT_PIE, 10 * THIRD, MEAL_STOCK),
            new Offer(ModItems.SEAFOOD_CHOWDER, 8 * THIRD, MEAL_STOCK))),
        new Section("Bites", List.of(
            new Offer(ModItems.BEEF_SKEWER, 4 * THIRD, BITE_STOCK),
            new Offer(ModItems.CHICKEN_SKEWER, 3 * THIRD, BITE_STOCK),
            new Offer(ModItems.VEGETABLE_ROLL, 3 * THIRD, BITE_STOCK),
            new Offer(ModItems.FRUIT_SALAD, 3 * THIRD, BITE_STOCK),
            new Offer(ModItems.SALMON_NIGIRI, 2 * THIRD, BITE_STOCK),
            new Offer(ModItems.COD_NIGIRI, 2 * THIRD, BITE_STOCK),
            new Offer(ModItems.KELP_ROLL, 2 * THIRD, BITE_STOCK))),
        // ordinary food a Cook cooked: full strength, Gourmet (a real Cook's cooking beats everyone else's)
        // (NPC price after the +50%: 3 nuggets for Steak / Cooked Porkchop, 2 for the rest)
        new Section("Cooked", List.of(
            cooked(Items.COOKED_BEEF, 2),
            cooked(Items.COOKED_PORKCHOP, 2),
            cooked(Items.COOKED_CHICKEN, 1),
            cooked(Items.COOKED_MUTTON, 1),
            cooked(Items.COOKED_SALMON, 1),
            cooked(Items.COOKED_COD, 1),
            cooked(Items.BAKED_POTATO, 1))),
        // for Pet Trainers: Training Feed (5 a bundle: 1 gold at the NPC) and plain animal food (raw, not Cook-made, none
        // with effects; Wheat 6 nuggets for 4, Seeds 2 nuggets a bundle, Raw Beef and Salmon 3 nuggets each at the NPC)
        new Section("Pets", List.of(
            new Offer(ModItems.TRAINING_FEED, FEED_FAIR, FEED_STOCK, false, FEED_PER),
            new Offer(() -> Items.WHEAT, 4, ANIMAL_FOOD_STOCK, false, 4),    // 4 for 6 nuggets: never under the Banker's 1 per 2
            new Offer(() -> Items.BEEF, 2, ANIMAL_FOOD_STOCK, false, 1),     // raw, not Cook-made: 3 nuggets at the NPC
            new Offer(() -> Items.SALMON, 2, ANIMAL_FOOD_STOCK, false, 1),   // 3 nuggets
            new Offer(() -> Items.WHEAT_SEEDS, 1, ANIMAL_FOOD_STOCK, false, 8))));

    // ── The Bartender: ordinary cocktails, shots and wine (none with effects), and Purified Water by the bottle. NPC prices after the +50% in the comments. ──
    private static final List<Section> BARTENDER = List.of(
        new Section("Cocktails", List.of(
            new Offer(ModItems.CLASSIC_OLD_FASHIONED, 9, DRINK_STOCK),         // 1 gold 5 nuggets each
            new Offer(ModItems.WHISKEY_SOUR, 9, DRINK_STOCK),
            new Offer(ModItems.WATERMELON_BREEZE, 9, DRINK_STOCK),
            new Offer(ModItems.SANGRIA_PITCHER, 9, DRINK_STOCK),
            new Offer(ModItems.TROPICAL_KELP_COOLER, 9, DRINK_STOCK),
            new Offer(ModItems.WHITE_WINE_SPRITZER, 9, DRINK_STOCK),
            new Offer(ModItems.BEETROOT_VELVET_SOUR, 9, DRINK_STOCK))),
        new Section("Shots", List.of(
            new Offer(ModItems.SHOTS.get(LiquorType.TEQUILA), 6, DRINK_STOCK), // 1 gold each
            new Offer(ModItems.SHOTS.get(LiquorType.VODKA), 6, DRINK_STOCK),
            new Offer(ModItems.SHOTS.get(LiquorType.RUM), 6, DRINK_STOCK),
            new Offer(ModItems.SHOTS.get(LiquorType.WHISKEY), 6, DRINK_STOCK),
            new Offer(ModItems.GLASS_OF_RED_WINE, 6, DRINK_STOCK),
            new Offer(ModItems.GLASS_OF_WHITE_WINE, 6, DRINK_STOCK),
            new Offer(ModItems.BEET_NIGHTCAP, 9, DRINK_STOCK))),               // 1 gold 5 nuggets
        new Section("Water", List.of(
            new Offer(ModItems.PURIFIED_WATER, 1, WATER_STOCK))));             // 2 nuggets a bottle

    // ── The Boat Master (Boats.md): ordinary boats for next to nothing, and the Premium boats: luxuries for players
    // with gold to burn. Boats have no stock limit; each Premium boat has a few per restock. NPC prices in the
    // comments (after the +50%). ──
    private static final long BOAT_FAIR = 2;          // 3 nuggets at the NPC
    private static final long CHEST_BOAT_FAIR = 4;    // 6 nuggets
    private static final int PREMIUM_STOCK = 2;
    public static final int LEGENDARY_STOCK = 2;
    private static final List<Section> BOATS = List.of(
        new Section("Boats", List.of(
            boat(Items.OAK_BOAT), boat(Items.SPRUCE_BOAT), boat(Items.BIRCH_BOAT), boat(Items.JUNGLE_BOAT),
            boat(Items.ACACIA_BOAT), boat(Items.DARK_OAK_BOAT), boat(Items.MANGROVE_BOAT))),
        new Section("More Boats", List.of(
            boat(Items.CHERRY_BOAT), boat(Items.PALE_OAK_BOAT), boat(Items.BAMBOO_RAFT),
            new Offer(() -> Items.OAK_CHEST_BOAT, CHEST_BOAT_FAIR, 0),
            new Offer(() -> Items.SPRUCE_CHEST_BOAT, CHEST_BOAT_FAIR, 0),
            new Offer(() -> Items.BAMBOO_CHEST_RAFT, CHEST_BOAT_FAIR, 0))),
        new Section("Premium", List.of(
            premium(ModItems.CASTAWAY_TUB, 50),
            premium(ModItems.SPEEDBOAT, 100),
            premium(ModItems.SWAMP_RAFT, 150),
            premium(ModItems.YACHT, 300))),
        new Section("Legendary", List.of(
            legendary(ModItems.STEAMBOAT, 350),
            legendary(ModItems.LONGSHIP, 400),
            legendary(ModItems.PADDLEWHEELER, 450),
            legendary(ModItems.PIRATE_SHIP, 500),
            legendary(ModItems.TEAPOT, 550))),
        new Section("Legendary II", List.of(
            legendary(ModItems.GHOST_SHIP, 666),
            legendary(ModItems.SUBMERSIBLE, 750),
            legendary(ModItems.FAIRY_BOAT, 800),
            legendary(ModItems.LEVIATHAN, 900))));

    // a Premium boat that costs this many gold at the NPC (its fair price is two thirds of that: the NPC adds 50%)
    // a Legendary boat: LEGENDARY_STOCK of each, ever, shared by everyone on the server; they never restock
    private static Offer legendary(Supplier<? extends Item> boat, int npcGold) {
        return new Offer(boat, npcGold * 9L * 2 / 3, LEGENDARY_STOCK, false, 1, true);
    }

    private static Offer premium(Supplier<? extends Item> boat, int npcGold) {
        return new Offer(boat, npcGold * 9L * 2 / 3, PREMIUM_STOCK);
    }

    private static Offer boat(Item item) {
        return new Offer(() -> item, BOAT_FAIR, 0);
    }

    // ── The smiths: a Repair tab (NpcRepairs) and their Whetstone. ──
    private static List<Section> smith(Supplier<? extends Item> whetstone) {
        return List.of(Section.repairTab(),
            new Section("Shop", List.of(new Offer(whetstone, WHETSTONE_FAIR, WHETSTONE_STOCK))));
    }

    // A Cook-made vanilla food, fairNuggets each, COOKED_STOCK per restock
    private static Offer cooked(Item item, long fairNuggets) {
        return new Offer(() -> item, fairNuggets, COOKED_STOCK, true);
    }

    // Every offer of a job, in one list (the order the menu's buy buttons count in)
    public static List<Offer> all(Job job) {
        return sections(job).stream().flatMap(section -> section.offers().stream()).toList();
    }
}
