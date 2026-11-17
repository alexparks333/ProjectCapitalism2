package com.alexh.capitalism.client;

import com.alexh.capitalism.core.ModItems;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.alexh.capitalism.client.JobGuideScreen.Entry.info;
import static com.alexh.capitalism.client.JobGuideScreen.Entry.made;

// The Pet Trainer's Guide (Pet Trainers only, JobGuideScreen's layout in barn wood and meadow green):
//  - Workshop: Blocks (Small / Medium / Large Food Trough and Water Bowl), Items (Ownership Tag, Training Stick). Every recipe lives here.
//  - Information: Pets (training, claiming, looking after one), Perks (what only you do, and what you can't) and Player
//    Debuffs, as clickable info boxes.
// Keep every line short, and no numbers (the training cost, the stick's uses): players find those out. Hand-written: keep it matching PetTrainer.md
// and pets/Pets.
public class PetTrainersGuideScreen extends JobGuideScreen {

    private static final Theme THEME = new Theme(0xFF3A2E22, 0xFF221A12, 0xFF9AD070, 0xFF4A6A2A, 0xFF181410, 0xFF6E6250,
        0xFFC8E8A0, 0xFFF2EAD6, 0xFFB8AE98, 0xFFE8D08A);

    private static final Tab BLOCKS = new Tab("Workshop", "Blocks", new ItemStack(ModItems.MEDIUM_FOOD_TROUGH.get()));
    private static final Tab ITEMS = new Tab("Workshop", "Items", new ItemStack(ModItems.TRAINING_STICK.get()));
    private static final Tab PETS = new Tab("Information", "Pets", new ItemStack(Items.LEAD));
    private static final Tab PERKS = new Tab("Information", "Perks", new ItemStack(Items.NAME_TAG));
    private static final Tab DEBUFFS = new Tab("Information", "Player Debuffs", new ItemStack(Items.BARRIER));

    public PetTrainersGuideScreen(@Nullable Screen parent) {
        super("Pet Trainer's Guide", THEME, List.of(BLOCKS, ITEMS, PETS, PERKS, DEBUFFS), entries(), parent);
    }

    private static List<Entry> entries() {
        ItemStack e = ItemStack.EMPTY;
        ItemStack tag = new ItemStack(ModItems.OWNERSHIP_TAG.get());
        ItemStack stick = new ItemStack(ModItems.TRAINING_STICK.get());
        ItemStack diamond = new ItemStack(Items.DIAMOND);
        ItemStack sticks = new ItemStack(Items.STICK);
        ItemStack lead = new ItemStack(Items.LEAD);

        List<Entry> list = new ArrayList<>();

        // ── Workshop: Blocks ──
        // Small (copper), Medium (iron), Large (Compacted Iron): a trough is upside-down pants with a Hay Bale in the
        // middle, a bowl the same without the top corners, with an empty Bucket in the middle (used up)
        ItemStack hay = new ItemStack(Items.HAY_BLOCK);
        ItemStack bucket = new ItemStack(Items.BUCKET);
        List<ItemStack> metals = List.of(new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.IRON_INGOT), new ItemStack(ModItems.COMPACTED_IRON.get()));
        List<ItemStack> troughs = List.of(new ItemStack(ModItems.SMALL_FOOD_TROUGH.get()), new ItemStack(ModItems.MEDIUM_FOOD_TROUGH.get()), new ItemStack(ModItems.LARGE_FOOD_TROUGH.get()));
        List<ItemStack> bowls = List.of(new ItemStack(ModItems.SMALL_WATER_BOWL.get()), new ItemStack(ModItems.MEDIUM_WATER_BOWL.get()), new ItemStack(ModItems.LARGE_WATER_BOWL.get()));
        String[] sizes = {"Small", "Medium", "Large"};
        String[] holds = {"A little food.", "More food.", "Lots of food."};
        String[] drinks = {"A few drinks.", "More drinks.", "Lots of drinks."};
        for (int i = 0; i < 3; i++) {
            ItemStack m = metals.get(i);
            list.add(made(BLOCKS, troughs.get(i), sizes[i] + " Food Trough",
                HowToMake.crafting(m, e, m, m, hay, m, m, m, m),
                "Put food in it. Pets nearby eat from it.", holds[i]));
        }
        for (int i = 0; i < 3; i++) {
            ItemStack m = metals.get(i);
            list.add(made(BLOCKS, bowls.get(i), sizes[i] + " Water Bowl",
                HowToMake.crafting(m, bucket, m, m, m, m, e, e, e),
                "Fill it with a Water Bucket. Pets nearby drink from it.", drinks[i]));
        }

        // ── Workshop: Items ──
        list.add(made(ITEMS, tag, "Ownership Tag",
            HowToMake.crafting(new ItemStack(Items.NAME_TAG), diamond, e, e, e, e, e, e, e),
            "Anyone can use one to claim a trained pet.", "Only Pet Trainers can make it."));
        list.add(made(ITEMS, stick, "Training Stick",
            HowToMake.crafting(lead, new ItemStack(Items.FISHING_ROD), diamond, e, sticks, e, e, sticks, e),
            "Turns an animal into a pet.", "It wears out after a few pets."));

        ItemStack bjorn = new ItemStack(ModItems.ANIMAL_BJORN.get());
        ItemStack hide = new ItemStack(Items.RABBIT_HIDE);
        ItemStack string = new ItemStack(Items.STRING);
        list.add(made(ITEMS, bjorn, "Animal Bjorn",
            HowToMake.crafting(string, e, string, hide, new ItemStack(ModItems.BACKPACK.get()), hide, hide, hide, hide),
            "Carries an animal on your adventures.", "For your customers: you can't use it."));

        // ── Information: Pets ──
        list.add(info(PETS, stick, "1. Training", List.of(stick),
            "Bring an animal home the normal way,", "then right-click it with the Training Stick.", "It costs you a lot of levels.", "It becomes a pet with its own name.", "Riding animals like horses can't be trained."));
        list.add(info(PETS, tag, "2. Claiming", List.of(tag),
            "Right-click the pet with an Ownership Tag.", "Now it's yours. Tags only work on trained pets."));
        list.add(info(PETS, new ItemStack(Items.WHEAT), "Looking After Pets", List.of(new ItemStack(Items.WHEAT), new ItemStack(Items.WATER_BUCKET)),
            "Pets get hungry and thirsty.", "Feed them their usual food and give them water.", "Look at a pet to see how it's doing.",
            "Left empty too long, a pet can starve to death."));
        list.add(info(PETS, new ItemStack(ModItems.MEDIUM_FOOD_TROUGH.get()), "Troughs and Bowls", List.of(new ItemStack(ModItems.MEDIUM_FOOD_TROUGH.get()), new ItemStack(ModItems.MEDIUM_WATER_BOWL.get())),
            "Hungry or thirsty pets close by walk over", "and eat or drink till they're full.", "Plain water is fine."));
        list.add(info(PETS, new ItemStack(Items.POPPY), "Petting", List.of(),
            "Right-click your pet with an empty hand to pet it.", "The first pet each day gives you a buff.", "Rarer animals give better buffs.",
            "One pet buff at a time: a new one takes", "the last one's place. Potions and food stay."));
        list.add(info(PETS, new ItemStack(Items.SPYGLASS), "Pet Window", List.of(),
            "Ctrl + right-click your pet to open its window.", "See its buff, what it eats and how it's doing.", "Rename it, or tell it to stay still."));
        list.add(info(PETS, new ItemStack(Items.CLOCK), "Safe While Away", List.of(),
            "An owned pet doesn't change while its owner is offline.", "Pets never wander off for good."));
        list.add(info(PETS, new ItemStack(Items.POPPY), "Gentle", List.of(),
            "Pets never attack anyone,", "even ones that would in the wild."));
        list.add(info(PETS, bjorn, "Adventures", List.of(bjorn),
            "For your customers: right-click their own pet", "with an Animal Bjorn to carry it,", "right-click the ground to let it out.",
            "Worn in the Back slot, it rides on their chest.", "Sneak + right-click with an empty hand does the same."));
        list.add(info(PETS, bjorn, "Carried Pets", List.of(bjorn),
            "A pet in a bjorn still gets hungry and thirsty.", "It can't eat or drink in there: let it out now and then."));
        list.add(info(PETS, new ItemStack(ModItems.TRAINING_FEED.get()), "Training Feed", List.of(new ItemStack(ModItems.TRAINING_FEED.get())),
            "Cooks make it, only you can use it.", "Feed it to an animal and it follows you", "for a moment. Handy for bringing animals home."));
        list.add(info(PETS, new ItemStack(ModItems.GUCCI_TOP_HAT.get()), "Hats", List.of(new ItemStack(ModItems.GUCCI_TOP_HAT.get()),
                new ItemStack(ModItems.BURBERRY_SANTA_HAT.get()), new ItemStack(ModItems.LOUIS_VUITTON_CAP.get()), new ItemStack(ModItems.PRADA_FEDORA.get())),
            "Designers make hats that help pets:", "slower hunger, slower thirst, tougher", "when starving, or bigger bars.", "Put one on in the pet's window (Actions)."));
        list.add(info(PETS, lead, "Owned Animals", List.of(lead),
            "Only the owner can lead, ride or hurt an owned animal.", "Anyone can feed and water a pet."));

        // ── Information: Perks (what only Pet Trainers do, and their one limit) ──
        list.add(info(PERKS, tag, "Pet Trainer Only", List.of(stick, tag, new ItemStack(ModItems.MEDIUM_FOOD_TROUGH.get()), bjorn),
            "Only you can train pets and make Ownership Tags,", "Food Troughs, Water Bowls and Animal Bjorns."));
        list.add(info(PERKS, new ItemStack(Items.BARRIER), "No Carrying", List.of(),
            "You can't carry animals in an Animal Bjorn.", "Bring them home the normal way."));

        // ── Information: Player Debuffs (what everyone without the job can't do) ──
        list.add(info(DEBUFFS, tag, "No Tags", List.of(),
            "Others can't make Ownership Tags.", "Others can still use one."));
        list.add(info(DEBUFFS, stick, "No Training", List.of(),
            "Others can't make or use a Training Stick."));
        list.add(info(DEBUFFS, new ItemStack(ModItems.MEDIUM_FOOD_TROUGH.get()), "No Troughs", List.of(),
            "Others can't make Food Troughs or Water Bowls.", "Others can still fill and use them."));
        list.add(info(DEBUFFS, bjorn, "No Bjorns", List.of(),
            "Others can't make Animal Bjorns.", "Others can still use and wear them."));
        return list;
    }
}
