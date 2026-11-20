package com.alexh.capitalism.occupation;

import java.util.List;
import java.util.Locale;

// Every job a player can hold (one Essential and one Fun job at once, JobData). Each has pros and cons; what they actually do is
// wired up in JobEvents (and the machines/food code). New jobs go at the END: players' jobs are synced by position.
public enum Job {
    COOK("Cook", Category.ESSENTIAL,
        List.of("Makes Gourmet food (full-strength food)", "Only one who can make Training Feed (for Pet Trainers)"),
        List.of("Can't eat raw meat or fish", "Can't also be a Farmer")),
    BARTENDER("Bartender", Category.FUN,
        List.of("Only one who can use the Fermenter and Bartending Table", "Makes Excellent Purified Water"),
        List.of()),
    WEAPONSMITH("Weaponsmith", Category.ESSENTIAL,
        List.of("Only one who can make Bows, Crossbows and Netherite Swords (and Netherite Axes, like a Toolsmith)",
            "Makes Shields from a plain Iron Ingot (others need Compacted Iron; an Armorsmith can too)",
            "Only one who can repair weapons, and shields with the Armorsmith (bows, crossbows and tridents mend with another of the same)",
            "Makes Iron and Diamond Swords and Axes from plain materials (others need Compacted ones)",
            "Gets 4 arrows per craft (others get 2)",
            "Crafts sharper swords that stay sharp longer (with a rare Grail roll)",
            "Only one who can make Sword Whetstones (anyone can use them to sharpen swords)",
            "Only one who can use the Weaponsmith Table"),
        List.of()),
    ARMORSMITH("Armorsmith", Category.ESSENTIAL,
        List.of("Makes Iron and Diamond armor from plain materials (others need Compacted ones)",
            "Makes Shields from a plain Iron Ingot (others need Compacted Iron), and repairs shields like a Weaponsmith"),
        List.of()),
    TOOLSMITH("Toolsmith", Category.ESSENTIAL,
        List.of("Makes Iron and Diamond Pickaxes, Shovels, Hoes and Axes from plain materials (others need Compacted ones)",
            "Only one who can make Netherite Pickaxes, Shovels and Hoes (and Axes, like a Weaponsmith)",
            "Crafts faster-mining tools (with a rare Grail roll)",
            "Only one who can make Scythes and Butcher Knives (Farmers buy them), and repairs Scythes",
            "Only one who can use the Toolsmith Table"),
        List.of()),
    ENCHANTER("Enchanter", Category.ESSENTIAL,
        List.of("Only one who can enchant past level 10 at an Enchanting Table (up to 30, on anything but books)",
            "Only one who can put enchantments on gear at an anvil (books onto items, items onto items)",
            "Only one who can use the Enchanter's Table (burns unwanted enchantments off with an Ench-Removal)",
            "Only one who can make and work the Transfer Tube (moves XP from one player to another)",
            "Only one who can make the Lapis Compressor (turns Lapis into XP; anyone can use it)"),
        List.of("Can't enchant books (that's the Librarian)")),
    LIBRARIAN("Librarian", Category.FUN,
        List.of("Only one who can enchant books at an Enchanting Table (up to level 30)",
            "Only one who can combine Enchanted Books at an anvil",
            "Only one who can make and use the Stitching Table (sews Rough and Delicate Books)",
            "Only one who can make and use the Librarian Table (writes skill books anyone can read)",
            "Only one who can make and use the Knowledge Creator (turns Enchanted Books into Knowledge Pieces)"),
        List.of()),
    PET_TRAINER("Pet Trainer", Category.FUN,
        List.of("Only one who can craft Ownership Tags (anyone can use one to claim a trained pet)",
            "Only one who can make and use the Training Stick (25 levels: turns an animal into a pet with a name, hunger and thirst, ready to be claimed)",
            "Only one who can make Food Troughs and Water Bowls (pets nearby eat and drink from them)",
            "Only one who can make the Animal Bjorn (carries an animal; anyone else can use and wear one)",
            "Only one who can use Training Feed (made by Cooks: an animal fed it follows you for a moment)"),
        List.of("Can't carry animals in an Animal Bjorn (no scooping up wild animals for the shop)")),
    DESIGNER("Designer", Category.FUN,
        List.of("Only one who can sew Patches at the Sewing Machine (a needle minigame)",
            "Only one who can make bags at the Designer Table (Backpack, Food Satchel, Seed Satchel: worn on your back, extra slots)",
            "Only one who can make and use Diamond Sewing Needles"),
        List.of()),
    FARMER("Farmer", Category.FUN,
        List.of("Gets the full harvest from crops (others get just one crop and one seed back)",
            "Gets the full meat drops from animals (others get just one piece of meat)",
            "Only one who can make and use the Decomposter (makes Fertilizer and Super Fertilizer)",
            "Only one who can use Fertilizer (fertilized farmland gives 50% to 90% more crops)",
            "Animals killed with a diamond or netherite sword or axe may drop a Meat Slab (cut up at the Butcher's Table)"),
        List.of("Can't also be a Cook")),
    REAL_ESTATE_AGENT("Real Estate Agent", Category.FUN,
        List.of("Only one who can buy town land from the city (For Sale by the City signs)",
            "Builds on the lots they own (the rest of the town is protected)",
            "Only one who can make For Sale and For Lease signs",
            "Sells and leases lots to other players (/properties); collects the rent every day"),
        List.of());

    // Every player can hold one Essential job (what the server needs: food, gear, enchanting) and one Fun job (JobData)
    public enum Category {
        ESSENTIAL("Essential"),
        FUN("Fun");

        public final String title;

        Category(String title) {
            this.title = title;
        }
    }

    public final String title;
    public final Category category;
    public final List<String> pros;
    public final List<String> cons;

    Job(String title, Category category, List<String> pros, List<String> cons) {
        this.title = title;
        this.category = category;
        this.pros = pros;
        this.cons = cons;
    }

    // "cook", "bartender": used in commands and save data
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Job byId(String id) {
        for (Job job : values()) {
            if (job.id().equals(id)) return job;
        }
        return null;
    }

    public int bit() {
        return 1 << ordinal();
    }

    // Jobs one player can never hold together (Jobs.md > Picking jobs): a Cook can't be a Farmer and a Farmer can't be
    // a Cook, whatever slots they have. Null = no such job.
    public Job clashesWith() {
        return switch (this) {
            case COOK -> FARMER;
            case FARMER -> COOK;
            default -> null;
        };
    }
}
