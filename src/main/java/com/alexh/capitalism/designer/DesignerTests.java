package com.alexh.capitalism.designer;

import com.alexh.capitalism.back.Back;
import com.alexh.capitalism.core.ModBlocks;
import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.List;

// Automated checks for the Designer (Designer.md), run by the game test server (gametest/GameTests,
// `gradlew runGameTestServer`): the bag slots (how many, what fits), the rule that a bag only holds things while worn
// (taking it off, swapping it, shift-clicking it or dying spills them), shift-clicking into the bag, the Sewing
// Machine (sewing in order, slips wear the needle, the thread snapping, a finished Patch) and the Designer Table.
public final class DesignerTests {

    private static final int BACK = 46;
    private static final int FIRST_BAG = 47;

    static {
        GameTests.register("designer_bag_slots", DesignerTests::bagSlots);
        GameTests.register("designer_bag_spills", DesignerTests::bagSpills);
        GameTests.register("designer_sewing_machine", DesignerTests::sewingMachine);
        GameTests.register("designer_table", DesignerTests::designerTable);
        GameTests.register("designer_mixed_bags_and_hats", DesignerTests::mixedBagsAndHats);
        GameTests.register("designer_freezer_sling", DesignerTests::freezerSling);
    }

    private DesignerTests() {
    }

    // Loaded by GameTests.init (runs the static block above)
    public static void init() {
    }

    private static Slot bagSlot(ServerPlayer player, int i) {
        return player.inventoryMenu.slots.get(FIRST_BAG + i);
    }

    // Item entities lying around the player with this item
    private static int dropped(GameTests.Checks c, ServerPlayer player, Item item) {
        List<ItemEntity> items = c.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(4), entity -> entity.getItem().is(item));
        int count = items.stream().mapToInt(entity -> entity.getItem().getCount()).sum();
        items.forEach(ItemEntity::discard);
        return count;
    }

    // The Utility tier: the Frozen Patch (Diamond needle, its ice seam) and the Freezer Sling (10 slots of food or meat,
    // which keeps meat like a Freezer, no fuel)
    private static void freezerSling(GameTests.Checks c) {
        c.check(Pattern.FROZEN_PATCH.tier == Pattern.Tier.UTILITY && Pattern.FROZEN_PATCH.needle() == ModItems.DIAMOND_SEWING_NEEDLE.get(),
            "the Frozen Patch is a Utility patch, sewn with a Diamond needle");
        c.check(Pattern.Tier.UTILITY.difficulty > Pattern.Tier.FANCY.difficulty && Pattern.Tier.UTILITY.difficulty < Pattern.Tier.LUXURY.difficulty,
            "Utility patches are medium-hard");
        c.check(Pattern.Style.of(Items.ICE) == Pattern.Style.ICY && Pattern.Style.of(Items.SNOWBALL) == Pattern.Style.ICY, "ice and snowballs sew an icy seam");
        c.check(Bag.FREEZER_SLING.line == Design.Line.UTILITY && Bag.FREEZER_SLING.slots == 10
            && Bag.FREEZER_SLING.patches().equals(java.util.List.of(ModItems.SILK_PATCH.get(), ModItems.FROZEN_PATCH.get(), ModItems.FROZEN_PATCH.get(), ModItems.SILK_PATCH.get())),
            "the Freezer Sling: Utility Bags, 10 slots, Silk / Frozen / Frozen / Silk");
        boolean allWearable = true;
        for (Bag bag : Bag.values()) allWearable &= new ItemStack(bag.item()).is(Back.BACK_ITEMS);
        c.check(allWearable, "every bag (the Freezer Sling too) can be worn in the Back slot");
        ServerPlayer player = c.player();
        Back.set(player, new ItemStack(ModItems.FREEZER_SLING.get()));
        ItemStack slab = com.alexh.capitalism.farmer.Freshness.newSlab(com.alexh.capitalism.farmer.MeatCut.BEEF_RIB, c.level().getGameTime(), c.level().getRandom());
        c.check(bagSlot(player, 0).mayPlace(new ItemStack(Items.BREAD)) && bagSlot(player, 0).mayPlace(slab)
            && !bagSlot(player, 0).mayPlace(new ItemStack(Items.DIRT)), "it takes food and Meat Slabs, nothing else");
        long now = c.level().getGameTime();
        slab.set(com.alexh.capitalism.core.ModComponents.ROTS_AT.get(), now + 1000);
        bagSlot(player, 0).set(slab);
        com.alexh.capitalism.farmer.FreshnessEvents.update(player);
        com.alexh.capitalism.farmer.Chill chill = bagSlot(player, 0).getItem().get(com.alexh.capitalism.core.ModComponents.CHILL.get());
        c.check(chill != null && chill.slower() == 6 && chill.leftAtNormalRate(now + 600) == 900, "meat in it keeps 6x as long (600 ticks only take 100 off)");
        Back.set(player, ItemStack.EMPTY);
        dropped(c, player, com.alexh.capitalism.farmer.MeatCut.BEEF_RIB.slab());
    }

    private static void bagSlots(GameTests.Checks c) {
        ServerPlayer player = c.player();
        c.check(player.inventoryMenu.slots.size() == FIRST_BAG + Bag.MAX_SLOTS, "the inventory has the bag slots after the Back slot");
        c.check(!bagSlot(player, 0).isActive(), "no bag: no bag slots");
        Back.set(player, new ItemStack(ModItems.BACKPACK.get()));
        c.check(bagSlot(player, 1).isActive() && !bagSlot(player, 2).isActive(), "a Backpack has 2 slots");
        c.check(bagSlot(player, 0).mayPlace(new ItemStack(Items.DIRT)), "a Backpack takes anything");
        c.check(!bagSlot(player, 0).mayPlace(new ItemStack(ModItems.FOOD_SATCHEL.get())) && !bagSlot(player, 0).mayPlace(new ItemStack(Items.SHULKER_BOX)),
            "no bags or shulker boxes in a bag");
        Back.set(player, new ItemStack(ModItems.FOOD_SATCHEL.get()));
        c.check(bagSlot(player, 3).isActive() && !bagSlot(player, 4).isActive(), "a Food Satchel has 4 slots");
        c.check(bagSlot(player, 0).mayPlace(new ItemStack(Items.BREAD)) && !bagSlot(player, 0).mayPlace(new ItemStack(Items.DIRT)), "a Food Satchel only takes food");
        Back.set(player, new ItemStack(ModItems.SEED_SATCHEL.get()));
        c.check(bagSlot(player, 9).isActive() && !bagSlot(player, 10).isActive(), "a Seed Satchel has 10 slots");
        c.check(bagSlot(player, 0).mayPlace(new ItemStack(Items.WHEAT_SEEDS)) && bagSlot(player, 0).mayPlace(new ItemStack(ModItems.RICE_SEEDS.get()))
            && !bagSlot(player, 0).mayPlace(new ItemStack(Items.BREAD)), "a Seed Satchel only takes seeds");

        // what goes in is kept in the worn bag
        bagSlot(player, 0).set(new ItemStack(Items.WHEAT_SEEDS, 20));
        c.check(Bags.hasContents(Back.get(player)), "the seeds are kept in the worn satchel");
        // shift-clicking seeds from the inventory goes into the satchel
        player.inventoryMenu.slots.get(9).set(new ItemStack(Items.BEETROOT_SEEDS, 12));
        player.inventoryMenu.quickMoveStack(player, 9);
        c.check(player.inventoryMenu.slots.get(9).getItem().isEmpty() && bagSlot(player, 1).getItem().is(Items.BEETROOT_SEEDS),
            "shift-clicking seeds puts them in the satchel");
        Back.set(player, ItemStack.EMPTY);
        dropped(c, player, Items.WHEAT_SEEDS);
        dropped(c, player, Items.BEETROOT_SEEDS);
    }

    private static void bagSpills(GameTests.Checks c) {
        ServerPlayer player = c.player();
        // taking it out of the Back slot: the things spill, the bag comes off empty
        Back.set(player, new ItemStack(ModItems.BACKPACK.get()));
        bagSlot(player, 0).set(new ItemStack(Items.DIAMOND, 3));
        ItemStack taken = player.inventoryMenu.slots.get(BACK).remove(1);
        c.check(taken.is(ModItems.BACKPACK.get()) && !taken.has(DataComponents.CONTAINER), "taken off, the Backpack is empty");
        c.check(dropped(c, player, Items.DIAMOND) == 3, "what was in it drops on the ground");

        // swapping it for another bag spills the first one
        Back.set(player, new ItemStack(ModItems.BACKPACK.get()));
        bagSlot(player, 1).set(new ItemStack(Items.EMERALD, 5));
        ItemStack old = Back.get(player);
        Back.set(player, new ItemStack(ModItems.FOOD_SATCHEL.get()));
        c.check(!old.has(DataComponents.CONTAINER) && dropped(c, player, Items.EMERALD) == 5, "swapping bags spills the old one");
        c.check(bagSlot(player, 1).getItem().isEmpty(), "the new bag starts empty");

        // shift-clicking it off your back
        Back.set(player, new ItemStack(ModItems.BACKPACK.get()));
        bagSlot(player, 0).set(new ItemStack(Items.GOLD_INGOT, 2));
        player.inventoryMenu.quickMoveStack(player, BACK);
        boolean emptyInInventory = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.BACKPACK.get())) emptyInInventory = !stack.has(DataComponents.CONTAINER);
        }
        c.check(emptyInInventory && dropped(c, player, Items.GOLD_INGOT) == 2, "shift-clicked off, it lands in the inventory empty and its things drop");

        // dying: the things drop, the bag burns (it's gone)
        player.getInventory().clearContent();
        Back.set(player, new ItemStack(ModItems.SEED_SATCHEL.get()));
        bagSlot(player, 0).set(new ItemStack(Items.MELON_SEEDS, 7));
        Back.onDeath(new LivingDeathEvent(player, player.damageSources().generic()));
        List<ItemEntity> bags = c.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(4), entity -> entity.getItem().is(ModItems.SEED_SATCHEL.get()));
        c.check(dropped(c, player, Items.MELON_SEEDS) == 7, "dying drops what was in the bag");
        c.check(bags.isEmpty() && Back.get(player).isEmpty(), "and the bag itself is destroyed");
        bags.forEach(ItemEntity::discard);
    }

    private static void sewingMachine(GameTests.Checks c) {
        ServerPlayer player = c.player();
        BlockPos at = c.helper.absolutePos(new BlockPos(1, 2, 1));
        c.level().setBlockAndUpdate(at, ModBlocks.SEWING_MACHINE.get().defaultBlockState());
        SewingMachineBlockEntity machine = (SewingMachineBlockEntity) c.level().getBlockEntity(at);
        c.check(machine.problem() != null, "no pattern: nothing to sew");
        machine.choose(Pattern.LEATHER_PATCH.ordinal());
        machine.setItem(SewingMachineBlockEntity.MATERIALS, new ItemStack(Items.LEATHER, 2));
        machine.setItem(SewingMachineBlockEntity.MATERIALS + 1, new ItemStack(Items.LEATHER, 2));
        machine.setItem(SewingMachineBlockEntity.MATERIALS + 2, new ItemStack(Items.STRING));
        c.check(machine.problem() != null, "no needle: can't sew");
        machine.setItem(SewingMachineBlockEntity.NEEDLE, new ItemStack(ModItems.IRON_SEWING_NEEDLE.get(), 5));
        c.check(machine.problem() == null, "pattern, materials and Iron needles: ready");
        machine.setItem(SewingMachineBlockEntity.NEEDLE, new ItemStack(ModItems.DIAMOND_SEWING_NEEDLE.get(), 5));
        c.check(machine.problem() != null, "a Generic patch won't sew with Diamond needles");
        machine.setItem(SewingMachineBlockEntity.NEEDLE, new ItemStack(ModItems.IRON_SEWING_NEEDLE.get(), 5));

        machine.sewn(player, 0);
        c.check(machine.next() == 1 && machine.getItem(SewingMachineBlockEntity.MATERIALS).getCount() == 1, "a clean seam sews the first material on (one used)");
        machine.choose(Pattern.STRAP_PATCH.ordinal());
        c.check(machine.pattern() == Pattern.LEATHER_PATCH, "the pattern can't change half way");

        machine.sewn(player, 1);
        c.check(machine.next() == 1 && machine.getNeedles() == 4 && machine.getItem(SewingMachineBlockEntity.MATERIALS + 1).getCount() == 2,
            "a slip: one needle breaks, the material is kept and that spot isn't done");
        machine.setItem(SewingMachineBlockEntity.NEEDLE, ItemStack.EMPTY);
        c.check(machine.problem() != null, "no needles: can't sew");
        machine.setItem(SewingMachineBlockEntity.NEEDLE, new ItemStack(ModItems.IRON_SEWING_NEEDLE.get(), 2));
        machine.sewn(player, 0);
        machine.sewn(player, 0);
        ItemStack out = machine.getItem(SewingMachineBlockEntity.OUTPUT);
        c.check(out.is(ModItems.LEATHER_PATCH.get()) && machine.sewn() == 0 && machine.getNeedles() == 2, "the last material finishes the Leather Patch (a clean seam breaks none)");

        machine.setItem(SewingMachineBlockEntity.MATERIALS, new ItemStack(Items.LEATHER));
        machine.sewn(player, 0);
        machine.unpick();
        c.check(machine.sewn() == 0 && machine.next() == 0, "unpicking starts the patch again");
        machine.choose(Pattern.STRAP_PATCH.ordinal());
        c.check(machine.pattern() == Pattern.STRAP_PATCH, "then the pattern can change");
        // Fancy: any color of wool, Diamond needles; Luxury: Diamond needles
        machine.setItem(SewingMachineBlockEntity.OUTPUT, ItemStack.EMPTY);
        machine.choose(Pattern.VELVET_PATCH.ordinal());
        c.check(machine.canPlaceItem(SewingMachineBlockEntity.MATERIALS, new ItemStack(Items.WOOL.lime()))
            && machine.canPlaceItem(SewingMachineBlockEntity.MATERIALS, new ItemStack(Items.WOOL.purple())), "a Velvet Patch takes any color of wool");
        machine.setItem(SewingMachineBlockEntity.MATERIALS, new ItemStack(Items.WOOL.lime()));
        c.check(machine.problem() != null, "a Fancy patch won't sew with Iron needles");
        machine.setItem(SewingMachineBlockEntity.MATERIALS, ItemStack.EMPTY);
        machine.setItem(SewingMachineBlockEntity.OUTPUT, ItemStack.EMPTY);
        machine.choose(Pattern.GUCCI_PATCH.ordinal());
        machine.setItem(SewingMachineBlockEntity.MATERIALS, new ItemStack(Items.LEATHER));
        c.check(machine.problem() != null, "a Luxury patch won't sew with Iron needles");
        machine.setItem(SewingMachineBlockEntity.NEEDLE, new ItemStack(ModItems.DIAMOND_SEWING_NEEDLE.get()));
        c.check(machine.problem() == null, "it sews with Diamond needles");
        c.level().removeBlock(at, false);
    }

    private static void designerTable(GameTests.Checks c) {
        ServerPlayer player = c.player();
        BlockPos at = c.helper.absolutePos(new BlockPos(3, 2, 1));
        c.level().setBlockAndUpdate(at, ModBlocks.DESIGNER_TABLE.get().defaultBlockState());
        DesignerTableBlockEntity table = (DesignerTableBlockEntity) c.level().getBlockEntity(at);
        table.choose(Bag.BACKPACK.ordinal());
        table.setItem(DesignerTableBlockEntity.LEATHER, new ItemStack(Items.LEATHER, 2));
        table.setItem(DesignerTableBlockEntity.NEEDLE, new ItemStack(ModItems.IRON_SEWING_NEEDLE.get(), 5));
        List<Item> patches = Bag.BACKPACK.patches();
        // the wrong patch in a slot
        table.setItem(DesignerTableBlockEntity.PATCHES, new ItemStack(ModItems.STRAP_PATCH.get()));
        for (int i = 1; i < patches.size(); i++) table.setItem(DesignerTableBlockEntity.PATCHES + i, new ItemStack(patches.get(i)));
        c.check(table.problem() != null, "a wrong patch: can't stitch");
        table.setItem(DesignerTableBlockEntity.PATCHES, new ItemStack(patches.getFirst()));
        c.check(table.problem() == null, "the right patches, Leather and Iron needles: ready");
        table.setItem(DesignerTableBlockEntity.NEEDLE, new ItemStack(ModItems.IRON_SEWING_NEEDLE.get(), DesignerTableBlockEntity.NEEDLES_USED - 1));
        c.check(table.problem() != null, "too few needles: can't stitch");
        table.setItem(DesignerTableBlockEntity.NEEDLE, new ItemStack(ModItems.DIAMOND_SEWING_NEEDLE.get(), 5));
        c.check(table.problem() != null, "a Standard bag takes Iron needles, not Diamond");
        table.setItem(DesignerTableBlockEntity.NEEDLE, new ItemStack(ModItems.IRON_SEWING_NEEDLE.get(), 5));
        table.make(player);
        ItemStack bag = table.getItem(DesignerTableBlockEntity.OUTPUT);
        c.check(bag.is(ModItems.BACKPACK.get()), "it makes a Backpack");
        c.check(table.getItem(DesignerTableBlockEntity.PATCHES).isEmpty() && table.getItem(DesignerTableBlockEntity.LEATHER).getCount() == 1,
            "the patches and one Leather are used");
        c.check(table.getItem(DesignerTableBlockEntity.NEEDLE).getCount() == 5 - DesignerTableBlockEntity.NEEDLES_USED, "stitching a bag uses up needles");
        c.level().removeBlock(at, false);
    }

    // Bags with groups of slots (each takes its own kind of thing), and the pet hats: made at the Designer Table, worn
    // by a pet from the owner's inventory, and what each does
    private static void mixedBagsAndHats(GameTests.Checks c) {
        ServerPlayer player = c.player();
        Back.set(player, new ItemStack(ModItems.LOUIS_VUITTON_BAG.get()));
        c.check(bagSlot(player, 13).isActive() && !bagSlot(player, 14).isActive(), "a Louis Vuitton Bag has 14 slots");
        c.check(bagSlot(player, 0).mayPlace(new ItemStack(Items.BREAD)) && !bagSlot(player, 0).mayPlace(new ItemStack(Items.DIRT)), "its first 4 take food or water");
        c.check(bagSlot(player, 4).mayPlace(new ItemStack(Items.DIRT)), "the next 6 take anything");
        c.check(bagSlot(player, 10).mayPlace(new ItemStack(Items.IRON_PICKAXE)) && !bagSlot(player, 10).mayPlace(new ItemStack(Items.DIRT)), "the last 4 take weapons and tools");
        Back.set(player, new ItemStack(ModItems.PRADA_BAG.get()));
        c.check(bagSlot(player, 0).mayPlace(new ItemStack(Items.DIAMOND)) && bagSlot(player, 0).mayPlace(new ItemStack(Items.IRON_ORE))
            && !bagSlot(player, 0).mayPlace(new ItemStack(Items.BREAD)), "a Prada Bag's first 10 take gems and ores");
        Back.set(player, new ItemStack(ModItems.FANCY_SEED_SATCHEL.get()));
        c.check(bagSlot(player, 14).isActive() && bagSlot(player, 0).mayPlace(new ItemStack(Items.POPPY)), "a Fancy Seed Satchel has 15 slots and takes flowers");
        Back.set(player, ItemStack.EMPTY);

        // a hat made at the table
        BlockPos at = c.helper.absolutePos(new BlockPos(1, 2, 3));
        c.level().setBlockAndUpdate(at, ModBlocks.DESIGNER_TABLE.get().defaultBlockState());
        DesignerTableBlockEntity table = (DesignerTableBlockEntity) c.level().getBlockEntity(at);
        table.choose(Design.all().indexOf(Hat.GUCCI_TOP_HAT));
        table.setItem(DesignerTableBlockEntity.LEATHER, new ItemStack(Items.LEATHER));
        table.setItem(DesignerTableBlockEntity.NEEDLE, new ItemStack(ModItems.IRON_SEWING_NEEDLE.get(), DesignerTableBlockEntity.NEEDLES_USED));
        List<Item> patches = Hat.GUCCI_TOP_HAT.patches();
        for (int i = 0; i < patches.size(); i++) table.setItem(DesignerTableBlockEntity.PATCHES + i, new ItemStack(patches.get(i)));
        c.check(table.problem() != null, "a hat won't stitch with Iron needles");
        table.setItem(DesignerTableBlockEntity.NEEDLE, new ItemStack(ModItems.DIAMOND_SEWING_NEEDLE.get(), DesignerTableBlockEntity.NEEDLES_USED));
        table.make(player);
        c.check(table.getItem(DesignerTableBlockEntity.OUTPUT).is(ModItems.GUCCI_TOP_HAT.get()), "the Designer Table makes a Gucci Top Hat");
        c.level().removeBlock(at, false);

        // on a pet
        net.minecraft.world.entity.animal.Animal pig = (net.minecraft.world.entity.animal.Animal) net.minecraft.world.entity.EntityTypes.PIG.create(c.level(), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        net.minecraft.world.phys.Vec3 p = c.at(2.5, 2, 2.5);
        pig.snapTo(p.x, p.y, p.z, 0, 0);
        c.level().addFreshEntity(pig);
        ServerPlayer trainer = c.player();
        trainer.getData(com.alexh.capitalism.core.ModAttachments.JOBS.get()).add(com.alexh.capitalism.occupation.Job.PET_TRAINER);
        trainer.experienceLevel = 100;
        trainer.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.TRAINING_STICK.get()));
        ModItems.TRAINING_STICK.get().interactLivingEntity(trainer.getMainHandItem(), trainer, pig, net.minecraft.world.InteractionHand.MAIN_HAND);
        com.alexh.capitalism.pets.Pets.claim(pig, player);
        player.getInventory().add(new ItemStack(ModItems.GUCCI_TOP_HAT.get()));
        com.alexh.capitalism.pets.Pets.wearHat(player, pig.getId(), Hat.GUCCI_TOP_HAT);
        com.alexh.capitalism.pets.PetData data = com.alexh.capitalism.pets.Pets.data(pig);
        c.check(data.hat() == Hat.GUCCI_TOP_HAT && !player.getInventory().contains(new ItemStack(ModItems.GUCCI_TOP_HAT.get())), "the owner puts the hat on (it leaves their inventory)");
        c.check(com.alexh.capitalism.pets.Pets.hungerPerSecond(data) < com.alexh.capitalism.pets.Pets.HUNGER_PER_SECOND, "a Top Hat: hunger drops slower");
        com.alexh.capitalism.pets.Pets.setHat(pig, data, Hat.BURBERRY_SANTA_HAT);
        c.check(com.alexh.capitalism.pets.Pets.thirstPerSecond(data) < com.alexh.capitalism.pets.Pets.THIRST_PER_SECOND, "a Santa Hat: thirst drops slower");
        com.alexh.capitalism.pets.Pets.setHat(pig, data, Hat.LOUIS_VUITTON_CAP);
        c.check(com.alexh.capitalism.pets.Pets.hurtSeconds(data) == 2 * com.alexh.capitalism.pets.Pets.HURT_SECONDS, "a Baseball Cap: starving hurts half as often");
        float health = pig.getMaxHealth();
        com.alexh.capitalism.pets.Pets.setHat(pig, data, Hat.PRADA_FEDORA);
        c.check(pig.getMaxHealth() == health + 4 && data.max() == data.size() + 4, "a Fedora: 2 more hearts, drumsticks and water drops");
        // the Feeding Hat (only from the Traveling Merchant): always full, never thirsty; and not a Designer's design
        com.alexh.capitalism.pets.Pets.setHat(pig, data, Hat.FEEDING_HAT);
        data.setHunger(1);
        data.setThirst(1);
        com.alexh.capitalism.pets.Pets.second(pig);
        com.alexh.capitalism.pets.PetData fed = com.alexh.capitalism.pets.Pets.data(pig);
        c.check(fed.hunger() == fed.max() && fed.thirst() == fed.max(), "a Feeding Hat: the pet is always full and never thirsty");
        c.check(!Design.all().contains(Hat.FEEDING_HAT) && !Design.all().contains(Bag.TRAVELERS_BAG), "the Feeding Hat and the Traveler's Bag aren't Designer designs");
        com.alexh.capitalism.pets.Pets.setHat(pig, com.alexh.capitalism.pets.Pets.data(pig), Hat.PRADA_FEDORA);
        com.alexh.capitalism.pets.Pets.wearHat(player, pig.getId(), null);
        c.check(data.hat() == null && pig.getMaxHealth() == health && player.getInventory().contains(new ItemStack(ModItems.PRADA_FEDORA.get())),
            "taking it off gives it back and the extra hearts go");
        pig.discard();
    }
}
