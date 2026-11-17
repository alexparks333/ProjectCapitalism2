package com.alexh.capitalism.librarian;

import com.alexh.capitalism.Capitalism;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
import static net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE;

// What a skill book teaches (Librarian.md, the full chart with the odds is LibrarianBooks.md). Written at the Librarian
// Table (LibrarianTableMenu): a Rough or Delicate Book + a Knowledge Piece = one of these, at random. Read it
// (SkillBookItem) to learn it:
//  - a lesson: an effect for a while (ModEffects' "book_" effects, hidden from the vanilla effect icons: they show in
//    Wellness > Buffs & Debuffs). Delicate Books give level II (twice as strong) for twice as long. Gone on death.
//  - a trait (rare): for good, kept through death (Traits). Learned once.
//  - an instant book (instant): does its thing once, the moment it's read (A Fresh Start: diet, water and body
//    heat back to the start). Written like a lesson (in the lesson draw); Rough and Delicate are the same.
// A skill either changes one attribute (attribute != null: the effect / trait carries the modifier, level II doubles
// it) or is done by a hook in SkillEffects (and a few mixins), which asks Skills.level(player, skill).
// To add a book: add it here, give it a cover in tools/make_librarian_textures.ps1 (and lessons a lang line for the
// effect), and put it in LibrarianBooks.md.
public enum Skill {
    // ── Lessons (timed) ──
    RUNNING_CLUB("Running Club", 0x4FA3E0, false, false, "Walk {10%|20%} faster.", Attributes.MOVEMENT_SPEED, 0.10, ADD_MULTIPLIED_BASE),
    MINING_MONTHLY("Mining Monthly", 0x8A6A3A, false, true, "Mine everything {15%|30%} faster.", Attributes.BLOCK_BREAK_SPEED, 0.15, ADD_MULTIPLIED_BASE),
    SWIMMING_LESSONS("Swimming Lessons", 0x2F7FD0, false, false, "Swim {33%|66%} faster.", net.neoforged.neoforge.common.NeoForgeMod.SWIM_SPEED, 0.33, ADD_MULTIPLIED_BASE),
    SOFT_LANDINGS("Soft Landings", 0xE8E0C8, false, false, "{25%|50%} less fall damage.", Attributes.FALL_DAMAGE_MULTIPLIER, -0.25, ADD_VALUE),
    HOLDING_YOUR_BREATH("Holding Your Breath", 0x5FD0D0, false, false, "Air lasts about {twice|3 times} as long underwater.", Attributes.OXYGEN_BONUS, 1.0, ADD_VALUE),
    ANGLERS_ALMANAC("The Angler's Almanac", 0x2E9A6A, false, false, "{+1|+2} Luck: better fishing and loot.", Attributes.LUCK, 1.0, ADD_VALUE),
    WATER_RATIONING("Water Rationing", 0x3A8AE8, false, true, "Thirst drains {25%|50%} slower."),
    BUTCHERY_BASICS("Butchery Basics", 0xB8402E, false, false, "{25%|50%} chance of extra meat from animals you kill."),
    LUMBERJACKS_LOG("Lumberjack's Log", 0x6A4A26, false, false, "Chop logs {25%|50%} faster."),
    ARCHERY_DRILLS("Archery Drills", 0xC8A050, false, false, "Your arrows hit {15%|30%} harder."),
    HORSE_WHISPERING("Horse Whispering", 0x9A6A44, false, false, "Horses you ride run {15%|30%} faster."),
    QUIET_STEPS("Quiet Steps", 0x4A4A5A, false, false, "Mobs have to be {20%|40%} closer to notice you."),
    FISHERMANS_PATIENCE("Fisherman's Patience", 0x3070A0, false, false, "Fish bite faster, like {Lure I|Lure II}."),
    STONEMASONS_GUIDE("Stonemason's Guide", 0x8A8A8A, false, false, "Stone, deepslate and other rock break {25%|50%} faster."),
    FIRST_AID_BASICS("First Aid Basics", 0xE04040, false, true, "Heal {25%|50%} faster on your own."),
    TRAIL_RATIONS("Trail Rations", 0xC89A50, false, false, "Sprinting makes you {30%|60%} less hungry."),
    BEEKEEPERS_NOTES("Beekeeper's Notes", 0xE8C030, false, false, "Bees never get angry at you."),
    LUCKY_LIBRARIAN("Lucky Librarian", 0x50C878, false, true, "Librarians only: books you write are twice as likely to be traits."),
    MERCHANTS_LEDGER("Merchant's Ledger", 0xD4AF37, false, true, "NPC shop prices {7.5%|15%} cheaper."),
    BRAWLERS_HANDBOOK("The Brawler's Handbook", 0xB02A2A, false, true, "{+1|+2} attack damage.", Attributes.ATTACK_DAMAGE, 1.0, ADD_VALUE),
    REACHING_FURTHER("Reaching Further", 0x9A6AD0, false, true, "Reach {1 block|2 blocks} further when mining and building.", Attributes.BLOCK_INTERACTION_RANGE, 1.0, ADD_VALUE),
    ICE_IN_THE_PANTS("Ice In The Pants", 0x8FD8F0, false, false, "You never get hot."),
    HOT_PEPPER("Hot Pepper", 0xD8361E, false, false, "You never get cold."),
    // ── Instant (once, when read) ──
    FRESH_START("A Fresh Start", 0x7FD87F, false, "Your body starts over: diet, water and temperature back to how they began."),
    // ── Traits (permanent) ──
    IRON_STOMACH("Iron Stomach", 0x8A9098, true, false, "Raw food and Rotten Flesh never give you Hunger."),
    NIMBLE("Nimble", 0x70D0A0, true, false, "Falls of up to 5 blocks don't hurt.", Attributes.SAFE_FALL_DISTANCE, 2.0, ADD_VALUE),
    NIGHT_OWL("Night Owl", 0x3A3A7A, true, false, "Phantoms leave you alone."),
    BOOKWORM("Bookworm", 0x7A5A9A, true, false, "Lessons you read last 50% longer."),
    STEADY_AIM("Steady Aim", 0xA0522D, true, false, "Bows and crossbows shoot dead straight."),
    SURE_FOOTED("Sure Footed", 0x9AD0E8, true, false, "Walk on Powder Snow, and 20% faster on ice."),
    LIGHT_FOOTED("Light Footed", 0xF0D060, true, false, "Walk 5% faster.", Attributes.MOVEMENT_SPEED, 0.05, ADD_MULTIPLIED_BASE),
    DEEP_LUNGS("Deep Lungs", 0x3AB0E0, true, false, "Air lasts about 3 times as long underwater.", Attributes.OXYGEN_BONUS, 2.0, ADD_VALUE),
    BORN_MINER("Born Miner", 0xD08A3A, true, true, "Mine everything 10% faster.", Attributes.BLOCK_BREAK_SPEED, 0.10, ADD_MULTIPLIED_BASE),
    LUCKY_STAR("Lucky Star", 0x60E070, true, true, "+1 Luck: better fishing and loot.", Attributes.LUCK, 1.0, ADD_VALUE),
    FAST_LEARNER("Fast Learner", 0x80FF40, true, true, "25% more XP from XP orbs."),
    CAMEL("Camel", 0xD8B070, true, true, "Thirst drains 25% slower, for good."),
    USED_TO_HELL("Used To Hell", 0x8A1A10, true, true, "Your body never goes above 70°: the heat can't touch you."),
    WINTER_SURVIVAL("Winter Survival", 0xB8E0F0, true, true, "The cold can't touch you: rain, snow and icy water never chill you.");

    // How long a lesson lasts (1 Minecraft day = 24000 ticks), how likely a book is to be a trait, and the weights
    // that pick which lesson / trait (rare ones less often)
    public static final int ROUGH_TICKS = 24000;
    public static final int DELICATE_TICKS = 48000;
    public static final float ROUGH_TRAIT_CHANCE = 0.01F;
    public static final float DELICATE_TRAIT_CHANCE = 0.125F;
    public static final int COMMON_WEIGHT = 10;
    public static final int RARE_WEIGHT = 4;

    public final String title;
    public final int color;
    public final boolean trait;
    public final boolean rare;
    public final boolean instant;
    public final String description;
    public final @Nullable Holder<Attribute> attribute;
    public final double amount;
    public final AttributeModifier.Operation operation;

    Skill(String title, int color, boolean trait, boolean rare, String description) {
        this(title, color, trait, rare, description, null, 0, ADD_VALUE, false);
    }

    // An instant book (never a trait)
    Skill(String title, int color, boolean rare, String description) {
        this(title, color, false, rare, description, null, 0, ADD_VALUE, true);
    }

    Skill(String title, int color, boolean trait, boolean rare, String description, @Nullable Holder<Attribute> attribute,
          double amount, AttributeModifier.Operation operation) {
        this(title, color, trait, rare, description, attribute, amount, operation, false);
    }

    Skill(String title, int color, boolean trait, boolean rare, String description, @Nullable Holder<Attribute> attribute,
          double amount, AttributeModifier.Operation operation, boolean instant) {
        this.title = title;
        this.color = color;
        this.trait = trait;
        this.rare = rare;
        this.instant = instant;
        this.description = description;
        this.attribute = attribute;
        this.amount = amount;
        this.operation = operation;
    }

    // "running_club": saved on the book, its cover is item/skill_books/running_club, the effect "book_running_club"
    public String id() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public Identifier modifierId() {
        return Identifier.fromNamespaceAndPath(Capitalism.MODID, (this.trait ? "trait_" : "book_") + this.id());
    }

    public int weight() {
        return this.rare ? RARE_WEIGHT : COMMON_WEIGHT;
    }

    // What it does at this level (1 = a Rough Book's lesson or a trait, 2 = a Delicate Book's lesson): the description
    // with each {level I|level II} choice filled in, e.g. "Walk {10%|20%} faster." -> "Walk 20% faster."
    public String description(int level) {
        StringBuilder out = new StringBuilder();
        String text = this.description;
        int at = 0;
        while (true) {
            int open = text.indexOf('{', at);
            if (open < 0) break;
            int bar = text.indexOf('|', open);
            int close = text.indexOf('}', open);
            out.append(text, at, open).append(level >= 2 ? text.substring(bar + 1, close) : text.substring(open + 1, bar));
            at = close + 1;
        }
        return out.append(text.substring(at)).toString();
    }

    public static @Nullable Skill byId(@Nullable String id) {
        for (Skill skill : values()) {
            if (skill.id().equals(id)) return skill;
        }
        return null;
    }

    private static List<Skill> all(boolean trait) {
        List<Skill> list = new ArrayList<>();
        for (Skill skill : values()) {
            if (skill.trait == trait) list.add(skill);
        }
        return list;
    }

    // A random book: first lesson or trait (traitOdds times the usual trait chance: 2 with Lucky Librarian), then which
    // one by weight
    public static Skill roll(RandomSource random, boolean delicate, float traitOdds) {
        boolean trait = random.nextFloat() < (delicate ? DELICATE_TRAIT_CHANCE : ROUGH_TRAIT_CHANCE) * traitOdds;
        List<Skill> pool = all(trait);
        int total = 0;
        for (Skill skill : pool) total += skill.weight();
        int pick = random.nextInt(total);
        for (Skill skill : pool) {
            pick -= skill.weight();
            if (pick < 0) return skill;
        }
        return pool.getLast();
    }
}
