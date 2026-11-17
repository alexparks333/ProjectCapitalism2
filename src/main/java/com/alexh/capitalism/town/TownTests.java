package com.alexh.capitalism.town;

import com.alexh.capitalism.core.CapitalismConfig;
import com.alexh.capitalism.core.ModBlocks;
import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.gametest.GameTests;
import com.alexh.capitalism.librarian.KnowledgeCreatorBlockEntity;
import com.alexh.capitalism.librarian.KnowledgeQuality;
import com.alexh.capitalism.librarian.LibrarianTableMenu;
import com.alexh.capitalism.librarian.Skill;
import com.alexh.capitalism.librarian.SkillBookItem;
import com.alexh.capitalism.occupation.Job;
import com.alexh.capitalism.core.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.Map;

// Automated checks for the Knowledge Creator, Knowledge Piece quality, the Whisperer's stock and the protected town
// center (Town.md, Librarian.md), run by the game test server (gametest/GameTests, `gradlew runGameTestServer`).
public final class TownTests {

    static {
        GameTests.register("npc_town_days", TownTests::npcTownDays);
        GameTests.register("spring_water", TownTests::springWater);
        GameTests.register("event_switches", TownTests::eventSwitches);
        GameTests.register("traveling_merchant", TownTests::travelingMerchant);
        GameTests.register("traveler_goods", TownTests::travelerGoods);
        GameTests.register("knowledge_quality", TownTests::quality);
        GameTests.register("knowledge_creator", TownTests::creator);
        GameTests.register("knowledge_at_the_table", TownTests::atTheTable);
        GameTests.register("town_whisperer_stock", TownTests::whispererStock);
        GameTests.register("town_protection", TownTests::protection);
        GameTests.register("town_hall", TownTests::townHall);
        GameTests.register("job_picking", TownTests::jobPicking);
    }

    private TownTests() {
    }

    // The Traveling Merchant: 8-14 days away, 5-8 wares a visit, NPC prices (a Molten Socket 300 gold), one of each
    private static void travelingMerchant(GameTests.Checks c) {
        var random = c.level().getRandom();
        boolean daysOk = true, sizeOk = true, uniqueOk = true;
        for (int i = 0; i < 300; i++) {
            int d = TravelingMerchantShop.daysAway(random);
            if (d < 8 || d > 14) daysOk = false;
            var stock = TravelingMerchantShop.rollStock(random);
            if (stock.size() < 5 || stock.size() > 8) sizeOk = false;
            if (new java.util.HashSet<>(stock).size() != stock.size()) uniqueOk = false;
            for (String key : stock) if (TravelingMerchantShop.stackFor(key).isEmpty()) c.fail("ware " + key + " makes nothing");
        }
        c.check(daysOk, "he comes back every 8-14 days");
        c.check(sizeOk && uniqueOk, "each visit: 5-8 wares, one of each");
        net.minecraft.server.level.ServerPlayer buyer = c.player();
        c.check(com.alexh.capitalism.economy.NpcPricing.npcPrice(TravelingMerchantShop.fairFor("molten_socket"), buyer) == com.alexh.capitalism.economy.GoldMoney.fromGold(300),
            "a Molten Socket costs 300 gold from him (NPC price)");

        // a visit: in town today, buy the first ware, it's gone for everyone
        var server = c.level().getServer();
        TravelingMerchantShop.arrive(server);
        c.check(TravelingMerchantShop.inTown(server), "arrive: he's in town today");
        String first = TravelingMerchantShop.stock(server).get(0);
        long price = com.alexh.capitalism.economy.NpcPricing.npcPrice(TravelingMerchantShop.fairFor(first), buyer);
        com.alexh.capitalism.economy.GoldMoney.give(buyer, price);
        var merchant = new TravelingMerchant(com.alexh.capitalism.core.ModEntities.TRAVELING_MERCHANT.get(), c.level());
        var wares = java.util.List.of(new TravelingMerchantMenu.Ware(TravelingMerchantShop.stackFor(first), price));
        var menu = new TravelingMerchantMenu(0, wares, new net.minecraft.world.inventory.SimpleContainerData(TravelingMerchantMenu.DATA), merchant);
        c.check(menu.clickMenuButton(buyer, 0), "buying the first ware works");
        c.check(com.alexh.capitalism.economy.GoldMoney.carried(buyer) == 0 && TravelingMerchantShop.sold(server, 0), "the gold is paid and it's sold");
        com.alexh.capitalism.economy.GoldMoney.give(buyer, price);
        c.check(!menu.clickMenuButton(buyer, 0), "nobody can buy it again this visit");
        TravelingMerchantShop.leave(server);
        c.check(!TravelingMerchantShop.inTown(server), "leave: he's gone");
    }

    // The Traveling Merchant's own goods, and a ware he brings two of
    private static void travelerGoods(GameTests.Checks c) {
        var server = c.level().getServer();
        // two diamonds: one buyer can take both, then they're gone
        c.check(TravelingMerchantShop.stockFor("diamond") == 2 && TravelingMerchantShop.stockFor("molten_socket") == 1, "he brings 2 diamonds, 1 Molten Socket");
        c.check(com.alexh.capitalism.economy.NpcPricing.npcPrice(TravelingMerchantShop.fairFor("diamond"), c.player()) == com.alexh.capitalism.economy.GoldMoney.fromGold(5),
            "a diamond: 5 gold");
        c.check(!TravelingMerchantShop.stackFor("trim:silence").isEmpty() && TravelingMerchantShop.fairFor("trim:silence") > TravelingMerchantShop.fairFor("trim:coast"),
            "trim templates: a rare one costs more");
        c.check(!TravelingMerchantShop.stackFor("disc:minecraft:music_disc_cat").isEmpty(), "a music disc ware");

        // the Bottomless Waterskin: drinks like Purified Water, never used up
        ServerPlayer drinker = c.player();
        var thirst = drinker.getData(com.alexh.capitalism.core.ModAttachments.THIRST.get());
        thirst.drain(8);
        float before = thirst.getThirstF();
        ItemStack skin = new ItemStack(com.alexh.capitalism.core.ModItems.BOTTOMLESS_WATERSKIN.get());
        ItemStack after = c.consume(drinker, skin);
        c.check(thirst.getThirstF() > before && after.is(com.alexh.capitalism.core.ModItems.BOTTOMLESS_WATERSKIN.get()), "the waterskin quenches and is still there");

        // the Hand Warmer: in the offhand, the cold can't reach you, and it burns down
        ServerPlayer cold = c.player();
        cold.setTicksFrozen(20);
        cold.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, new ItemStack(com.alexh.capitalism.core.ModItems.HAND_WARMER.get()));
        var climate = com.alexh.capitalism.player.BodyTemperature.climate(cold);
        c.check(climate.target() >= 70f && (climate.reasons() & com.alexh.capitalism.player.BodyTemperature.R_HAND_WARMER) != 0, "Hand Warmer: powder snow can't chill you");
        com.alexh.capitalism.player.BodyTemperature.tick(cold, cold.getData(com.alexh.capitalism.core.ModAttachments.WELLBEING.get()));
        c.check(cold.getOffhandItem().getDamageValue() == 1, "it burns down a second at a time");

        // the Phoenix Feather: no death tax, and it's gone
        ServerPlayer phoenix = c.player();
        com.alexh.capitalism.economy.GoldMoney.give(phoenix, com.alexh.capitalism.economy.GoldMoney.fromGold(40));
        phoenix.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, new ItemStack(com.alexh.capitalism.core.ModItems.PHOENIX_FEATHER.get()));
        com.alexh.capitalism.deathtax.DeathTax.died(phoenix, c.level().damageSources().generic());
        c.check(com.alexh.capitalism.deathtax.DeathTax.owed(phoenix) == 0 && phoenix.getOffhandItem().isEmpty(), "Phoenix Feather: no death tax, and it burns away");
        ServerPlayer plain = c.player();
        com.alexh.capitalism.economy.GoldMoney.give(plain, com.alexh.capitalism.economy.GoldMoney.fromGold(40));
        com.alexh.capitalism.deathtax.DeathTax.died(plain, c.level().damageSources().generic());
        c.check(com.alexh.capitalism.deathtax.DeathTax.owed(plain) > 0, "without one: the death tax as usual");

        // a lost drumstick / drop (Poor diet, Low water): gold never fills it either
        var bars = new com.alexh.capitalism.player.ThirstData();
        bars.foodCap = 18;
        bars.thirstCap = 18;
        bars.addGoldFood(40);
        bars.restoreWithGold(60);
        c.check(bars.goldFood() == 18 && bars.goldThirst() <= 18f, "with a drumstick / drop lost, the gold bars stop at 9 too (food " + bars.goldFood() + ", water " + bars.goldThirst() + ")");

        // the Traveler's Bag: shrinking back near town, slots 7-12 burst out, the first 6 stay
        ServerPlayer traveler = c.player();
        ItemStack bag = new ItemStack(com.alexh.capitalism.core.ModItems.TRAVELERS_BAG.get());
        bag.set(com.alexh.capitalism.core.ModComponents.EXPANDED.get(), true);
        com.alexh.capitalism.back.Back.set(traveler, bag);
        java.util.List<com.alexh.capitalism.designer.BagSlot> bagSlots = new java.util.ArrayList<>();
        for (var slot : traveler.inventoryMenu.slots) if (slot instanceof com.alexh.capitalism.designer.BagSlot b) bagSlots.add(b);
        c.check(bagSlots.size() >= 12 && bagSlots.get(11).isActive() && !bagSlots.get(12).isActive(), "opened out: 12 slots");
        for (int i = 0; i < 12; i++) bagSlots.get(i).set(new ItemStack(net.minecraft.world.item.Items.COBBLESTONE, i + 1));
        var near = new net.minecraft.world.phys.AABB(traveler.blockPosition()).inflate(4);
        c.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, near).forEach(net.minecraft.world.entity.Entity::discard);
        com.alexh.capitalism.designer.TravelersBag.shrink(traveler, com.alexh.capitalism.back.Back.peek(traveler));
        boolean keptFirst = true, emptiedRest = true;
        for (int i = 0; i < 6; i++) keptFirst &= bagSlots.get(i).getItem().getCount() == i + 1;
        for (int i = 6; i < 12; i++) emptiedRest &= bagSlots.get(i).getItem().isEmpty();
        int burst = 0;
        for (var drop : c.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, near)) { burst += drop.getItem().getCount(); drop.discard(); }
        c.check(keptFirst && emptiedRest && !bagSlots.get(6).isActive(), "shrinking: the first 6 keep their things, 7-12 are empty and closed");
        c.check(burst == 7 + 8 + 9 + 10 + 11 + 12, "what was in 7-12 burst out, every item, no more (got " + burst + ")");
        var contents = com.alexh.capitalism.back.Back.peek(traveler).get(net.minecraft.core.component.DataComponents.CONTAINER);
        long saved = contents == null ? 0 : contents.nonEmptyItemCopyStream().mapToInt(ItemStack::getCount).sum();
        c.check(saved == 1 + 2 + 3 + 4 + 5 + 6, "the bag itself only holds the first 6 now (no copies left inside: " + saved + ")");

        // the Sunken Pirate Chest: fish often; very rarely the Old Captain's Rod or a rare fish
        boolean sawFish = false, sawRod = false, sawRare = false;
        var chestRandom = net.minecraft.util.RandomSource.create(3L);
        for (int i = 0; i < 400; i++) {
            for (ItemStack s : com.alexh.capitalism.fishing.RareFindItems.PirateChest.loot(chestRandom)) sawFish |= s.is(net.minecraft.world.item.Items.COD) || s.is(net.minecraft.world.item.Items.SALMON);
            for (ItemStack s : com.alexh.capitalism.fishing.RareFindItems.PirateChest.treasure(c.level(), traveler, chestRandom)) {
                sawRod |= s.is(net.minecraft.world.item.Items.FISHING_ROD) && s.isEnchanted();
                sawRare |= s.getItem() instanceof com.alexh.capitalism.fishing.RareCatchItem;
            }
        }
        c.check(sawFish && sawRod && sawRare, "pirate chests: cod and salmon, and (rarely) the Old Captain's Rod and rare fish");

        // the Purifying Waterskin loses a sip each drink
        c.check(after.getDamageValue() == 1, "the waterskin: one sip used of 16");

        // the Traveler's Lantern: lit when he's due tomorrow or here
        TravelingMerchantShop.arrive(server);
        c.check(TravelerBlocks.Lantern.shouldLight(c.level()), "the lantern is lit while he's in town");
        TravelingMerchantShop.leave(server);
        c.check(!TravelerBlocks.Lantern.shouldLight(c.level()), "and dark once he's gone (he's 8+ days away)");
    }

    // The Purge / Blood Moon Switches: only the Mayor throws them; thrown, they wait until the event is over
    private static void eventSwitches(GameTests.Checks c) {
        var level = c.level();
        var server = level.getServer();
        com.alexh.capitalism.event.BloodMoon.unschedule(server);
        net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.containing(c.at(1, 2, 1));
        level.setBlock(pos, com.alexh.capitalism.core.ModBlocks.BLOOD_MOON_SWITCH.get().defaultBlockState(), 3);
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false);
        ServerPlayer citizen = c.player();
        level.getBlockState(pos).useWithoutItem(level, citizen, hit);
        c.check(!level.getBlockState(pos).getValue(com.alexh.capitalism.event.EventSwitchBlock.ON) && com.alexh.capitalism.event.BloodMoon.ticksUntil(server) < 0,
            "someone who isn't the Mayor can't throw it");
        ServerPlayer mayor = c.player();
        TownHall.get(server).appoint(TownHall.Office.MAYOR, mayor);
        level.getBlockState(pos).useWithoutItem(level, mayor, hit);
        c.check(level.getBlockState(pos).getValue(com.alexh.capitalism.event.EventSwitchBlock.ON) && com.alexh.capitalism.event.BloodMoon.ticksUntil(server) >= 0,
            "the Mayor throws it: a Blood Moon is coming tonight");
        level.getBlockState(pos).useWithoutItem(level, mayor, hit);
        c.check(com.alexh.capitalism.event.BloodMoon.ticksUntil(server) >= 0, "thrown again: nothing more (no spamming)");
        // the natural schedule: The Purge every 10-12 days, a Blood Moon every 6-8, never the same night
        var sched = new com.alexh.capitalism.event.EventSchedule();
        var rng = net.minecraft.util.RandomSource.create(11L);
        boolean gapsOk = true, apartOk = true;
        long purgeAt = sched.pickPurge(0, rng), bloodAt = sched.pickBlood(0, rng);
        for (int i = 0; i < 300; i++) {
            int pg = com.alexh.capitalism.event.EventSchedule.gap(rng, 10, 12), bg = com.alexh.capitalism.event.EventSchedule.gap(rng, 6, 8);
            gapsOk &= pg >= 10 && pg <= 12 && bg >= 6 && bg <= 8;
        }
        for (int i = 0; i < 300; i++) {
            sched.set(-1, 20);
            apartOk &= sched.pickPurge(10, rng) != 20;   // 10-12 days from day 10 could be night 20: it moves on
            sched.set(14, -1);
            apartOk &= sched.pickBlood(8, rng) != 14;
        }
        c.check(gapsOk && purgeAt >= 10 && purgeAt <= 13 && bloodAt >= 6 && bloodAt <= 9, "natural events: The Purge every 10-12 days, a Blood Moon every 6-8");
        c.check(apartOk, "natural events never land on each other's night");

        // a Purge can be called with a Blood Moon coming: the two can share a night
        boolean purge = com.alexh.capitalism.event.Purge.schedule(server);
        c.check(purge, "The Purge can be announced while a Blood Moon is coming (both can happen together)");
        com.alexh.capitalism.event.Purge.unschedule(server);

        // the Mayoral Door: only the Mayor opens it, and its double-door partner swings with it; others can't
        net.minecraft.core.BlockPos left = net.minecraft.core.BlockPos.containing(c.at(3, 2, 1));
        net.minecraft.core.BlockPos right = left.east();
        var door = com.alexh.capitalism.core.ModBlocks.MAYORAL_DOOR.get().defaultBlockState()
            .setValue(net.minecraft.world.level.block.DoorBlock.FACING, net.minecraft.core.Direction.NORTH);
        var lower = net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER;
        var upper = net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER;
        var hingeL = net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT;
        var hingeR = net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT;
        level.setBlock(left, door.setValue(net.minecraft.world.level.block.DoorBlock.HINGE, hingeL).setValue(net.minecraft.world.level.block.DoorBlock.HALF, lower), 3);
        level.setBlock(left.above(), door.setValue(net.minecraft.world.level.block.DoorBlock.HINGE, hingeL).setValue(net.minecraft.world.level.block.DoorBlock.HALF, upper), 3);
        level.setBlock(right, door.setValue(net.minecraft.world.level.block.DoorBlock.HINGE, hingeR).setValue(net.minecraft.world.level.block.DoorBlock.HALF, lower), 3);
        level.setBlock(right.above(), door.setValue(net.minecraft.world.level.block.DoorBlock.HINGE, hingeR).setValue(net.minecraft.world.level.block.DoorBlock.HALF, upper), 3);
        var doorHit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(left), net.minecraft.core.Direction.NORTH, left, false);
        level.getBlockState(left).useWithoutItem(level, citizen, doorHit);
        c.check(!level.getBlockState(left).getValue(net.minecraft.world.level.block.DoorBlock.OPEN), "the Mayoral Door won't open for anyone but the Mayor");
        level.getBlockState(left).useWithoutItem(level, mayor, doorHit);
        c.check(level.getBlockState(left).getValue(net.minecraft.world.level.block.DoorBlock.OPEN) && level.getBlockState(right).getValue(net.minecraft.world.level.block.DoorBlock.OPEN)
            && level.getBlockState(right.above()).getValue(net.minecraft.world.level.block.DoorBlock.OPEN), "the Mayor opens it, and both halves of the double door swing open");
        level.getBlockState(left).useWithoutItem(level, mayor, doorHit);
        c.check(!level.getBlockState(right).getValue(net.minecraft.world.level.block.DoorBlock.OPEN), "...and closes them together");
        level.setBlock(left.below().south(), net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        level.setBlock(left.south(), net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        c.check(!level.getBlockState(left).getValue(net.minecraft.world.level.block.DoorBlock.OPEN), "redstone doesn't open it");
        for (var p : new net.minecraft.core.BlockPos[]{left.south(), left.below().south(), left.above(), right.above(), left, right}) {
            level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        }
        com.alexh.capitalism.event.BloodMoon.unschedule(server);
        level.getBlockState(pos).tick(level, pos, level.getRandom());
        c.check(!level.getBlockState(pos).getValue(com.alexh.capitalism.event.EventSwitchBlock.ON), "once it's over, the switch springs back, ready again");
        TownHall.get(server).remove(TownHall.Office.MAYOR);
        level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
    }

    // Spring Water (Bartending.md): the Advanced Osmosis Machine (Bartenders only), filters, bottling, cocktails, purity
    private static void springWater(GameTests.Checks c) {
        var level = c.level();
        ServerPlayer bartender = c.player();
        bartender.getData(com.alexh.capitalism.core.ModAttachments.JOBS.get()).add(com.alexh.capitalism.occupation.Job.BARTENDER);
        net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.containing(c.at(1, 2, 3));
        level.setBlock(pos, com.alexh.capitalism.core.ModBlocks.OSMOSIS_MACHINE.get().defaultBlockState(), 3);
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false);
        var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
        var machine = (com.alexh.capitalism.block.OsmosisMachineBlock.Machine) level.getBlockEntity(pos);
        // a bucket without a filter: nothing
        bartender.setItemInHand(hand, new ItemStack(com.alexh.capitalism.core.ModItems.PURIFIED_WATER_BUCKET.get()));
        level.getBlockState(pos).useItemOn(bartender.getItemInHand(hand), level, bartender, hand, hit);
        c.check(bartender.getItemInHand(hand).is(com.alexh.capitalism.core.ModItems.PURIFIED_WATER_BUCKET.get()), "no filter: the machine won't take the water");
        // an Osmosis Filter (2 buckets), then two buckets of purified water
        bartender.setItemInHand(hand, new ItemStack(com.alexh.capitalism.core.ModItems.OSMOSIS_FILTER.get()));
        level.getBlockState(pos).useItemOn(bartender.getItemInHand(hand), level, bartender, hand, hit);
        c.check(machine.usesLeft() == 2, "an Osmosis Filter goes in: 2 buckets");
        int made = 0;
        for (int round = 0; round < 3; round++) {
            bartender.setItemInHand(hand, new ItemStack(com.alexh.capitalism.core.ModItems.PURIFIED_WATER_BUCKET.get()));
            level.getBlockState(pos).useItemOn(bartender.getItemInHand(hand), level, bartender, hand, hit);
            for (int t = 0; t < com.alexh.capitalism.block.OsmosisMachineBlock.BREW_TICKS; t++) machine.tick(level, pos, level.getBlockState(pos));
            if (machine.ready()) {
                bartender.setItemInHand(hand, ItemStack.EMPTY);
                level.getBlockState(pos).useWithoutItem(level, bartender, hit);
                if (bartender.getItemInHand(hand).is(com.alexh.capitalism.core.ModItems.SPRING_WATER_BUCKET.get()) && !machine.ready()) made++;
            }
        }
        c.check(made == 2 && machine.filter().isEmpty(), "the Osmosis Filter lasts 2 buckets: 2 Spring Water Buckets, then it's used up (made " + made + ")");
        c.check(new ItemStack(com.alexh.capitalism.core.ModItems.ADVANCED_OSMOSIS_FILTER.get()).getMaxDamage() == 4, "an Advanced Osmosis Filter lasts 4 buckets");
        level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);

        // Spring Water pulls purity up to Excellent
        ServerPlayer drinker = c.player();
        var thirst = drinker.getData(com.alexh.capitalism.core.ModAttachments.THIRST.get());
        thirst.setPurity(80f);
        for (int i = 0; i < 4; i++) {
            thirst.drain(6);
            c.consume(drinker, new ItemStack(com.alexh.capitalism.core.ModItems.SPRING_WATER.get()));
        }
        c.check(com.alexh.capitalism.player.WaterPurity.level(thirst.getPurity()) == com.alexh.capitalism.player.WaterPurity.Level.EXCELLENT,
            "a few Spring Waters: Excellent water (" + Math.round(thirst.getPurity()) + "%)");

        // alcoholic drinks never touch Water Purity: thirst and effects only
        ServerPlayer sipper = c.player();
        var t2 = sipper.getData(com.alexh.capitalism.core.ModAttachments.THIRST.get());
        t2.setPurity(70f);
        t2.drain(6);
        c.consume(sipper, new ItemStack(com.alexh.capitalism.core.ModItems.OVERWORLD_MOJITO.get()));
        c.consume(sipper, new ItemStack(com.alexh.capitalism.core.ModItems.GLASS_OF_RED_WINE.get()));
        c.check(t2.getPurity() == 70f, "alcoholic drinks leave Water Purity alone (" + t2.getPurity() + ")");
        var bottling = BartenderRecipeCheck.bottling();
        c.check(bottling, "a Spring Water Bucket pours into 6 bottles at the Bartending Table");
    }

    private static final class BartenderRecipeCheck {
        static boolean bottling() {
            for (var r : com.alexh.capitalism.block.BartenderRecipe.RECIPES) {
                if (r.result().get() == com.alexh.capitalism.core.ModItems.SPRING_WATER.get() && r.count() == 6) return true;
            }
            return false;
        }
    }

    // Job NPCs come to town on their own 20% of days even with players of their job online (npc/NpcManager.inTownToday)
    private static void npcTownDays(GameTests.Checks c) {
        long seed = c.level().getSeed();
        int days = 2000, cook = 0, both = 0, farmer = 0;
        for (long day = 0; day < days; day++) {
            boolean a = com.alexh.capitalism.npc.NpcManager.inTownOn(seed, day, com.alexh.capitalism.occupation.Job.COOK);
            boolean b = com.alexh.capitalism.npc.NpcManager.inTownOn(seed, day, com.alexh.capitalism.occupation.Job.FARMER);
            if (a) cook++;
            if (b) farmer++;
            if (a && b) both++;
        }
        c.near(cook / (double) days, 0.2, 0.03, "the Cook NPC is in town about 20% of days");
        c.near(farmer / (double) days, 0.2, 0.03, "the Farmer NPC too");
        c.near(both / (double) days, 0.04, 0.02, "each job rolls on its own (both on the same day about 4%)");
        c.check(com.alexh.capitalism.npc.NpcManager.inTownOn(seed, 7, com.alexh.capitalism.occupation.Job.COOK)
            == com.alexh.capitalism.npc.NpcManager.inTownOn(seed, 7, com.alexh.capitalism.occupation.Job.COOK), "the same answer all day");
    }

    // Loaded by GameTests.init (runs the static block above)
    public static void init() {
    }

    // An Enchanted Book with these enchantments
    private static ItemStack book(GameTests.Checks c, Map<ResourceKey<Enchantment>, Integer> enchantments) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        var registry = c.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        enchantments.forEach((key, level) -> {
            Holder<Enchantment> holder = registry.getOrThrow(key);
            stored.set(holder, level);
        });
        book.set(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
        return book;
    }

    // Power = levels added up (+3 per treasure enchantment) -> Faded 1-2, Clear 3-5, Bright 6-9, Radiant 10+
    private static void quality(GameTests.Checks c) {
        record Case(Map<ResourceKey<Enchantment>, Integer> enchantments, int power, KnowledgeQuality quality, String name) {
        }
        for (Case test : new Case[]{
            new Case(Map.of(Enchantments.UNBREAKING, 2), 2, KnowledgeQuality.FADED, "Unbreaking II"),
            new Case(Map.of(Enchantments.UNBREAKING, 3), 3, KnowledgeQuality.CLEAR, "Unbreaking III"),
            new Case(Map.of(Enchantments.SHARPNESS, 5), 5, KnowledgeQuality.CLEAR, "Sharpness V"),
            new Case(Map.of(Enchantments.PROTECTION, 4, Enchantments.UNBREAKING, 3), 7, KnowledgeQuality.BRIGHT, "Protection IV + Unbreaking III"),
            new Case(Map.of(Enchantments.MENDING, 1), 4, KnowledgeQuality.CLEAR, "Mending (treasure)"),
            new Case(Map.of(Enchantments.MENDING, 1, Enchantments.EFFICIENCY, 5, Enchantments.UNBREAKING, 3), 12, KnowledgeQuality.RADIANT, "Mending + Efficiency V + Unbreaking III")}) {
            ItemStack book = book(c, test.enchantments());
            int power = KnowledgeQuality.power(book);
            c.check(power == test.power(), test.name() + ": power " + test.power() + " (got " + power + ")");
            c.check(KnowledgeQuality.byPower(power) == test.quality(), test.name() + " makes " + test.quality().title + " (got " + KnowledgeQuality.byPower(power).title + ")");
        }
        c.check(KnowledgeQuality.power(new ItemStack(Items.BOOK)) == 0, "a plain Book has no power (can't go in)");
        // pieces: each quality round-trips, Faded is the plain item, same qualities stack, different ones don't
        for (KnowledgeQuality q : KnowledgeQuality.values()) {
            c.check(KnowledgeQuality.of(q.stack(1)) == q, q.title + " piece reads back as " + q.title);
            c.check(ItemStack.isSameItemSameComponents(q.stack(1), q.stack(1)), q.title + " pieces stack together");
        }
        c.check(ItemStack.isSameItemSameComponents(KnowledgeQuality.FADED.stack(1), new ItemStack(ModItems.KNOWLEDGE_PIECE.get())), "a Faded piece is the plain Knowledge Piece");
        c.check(!ItemStack.isSameItemSameComponents(KnowledgeQuality.CLEAR.stack(1), KnowledgeQuality.BRIGHT.stack(1)), "Clear and Bright pieces don't stack");
        c.check(KnowledgeQuality.RADIANT.emeralds() == 4 && KnowledgeQuality.FADED.emeralds() == 1, "prices: Faded 1 emerald ... Radiant 4");
    }

    // The Knowledge Creator: 30 seconds, then a piece of the book's quality; it won't mix qualities in the output
    private static void creator(GameTests.Checks c) {
        BlockPos pos = BlockPos.containing(c.at(2, 1, 2));
        c.level().setBlockAndUpdate(pos, ModBlocks.KNOWLEDGE_CREATOR.get().defaultBlockState());
        KnowledgeCreatorBlockEntity creator = (KnowledgeCreatorBlockEntity) c.level().getBlockEntity(pos);
        c.check(!creator.canPlaceItem(KnowledgeCreatorBlockEntity.BOOK, new ItemStack(Items.BOOK)), "a plain Book can't go in");
        creator.setItem(KnowledgeCreatorBlockEntity.BOOK, book(c, Map.of(Enchantments.PROTECTION, 4, Enchantments.UNBREAKING, 3)));
        for (int t = 0; t < KnowledgeCreatorBlockEntity.MAKE_TICKS - 1; t++) {
            KnowledgeCreatorBlockEntity.serverTick(c.level(), pos, c.level().getBlockState(pos), creator);
        }
        c.check(creator.getItem(KnowledgeCreatorBlockEntity.OUTPUT).isEmpty(), "not done a tick before 30 seconds");
        KnowledgeCreatorBlockEntity.serverTick(c.level(), pos, c.level().getBlockState(pos), creator);
        ItemStack out = creator.getItem(KnowledgeCreatorBlockEntity.OUTPUT);
        c.check(out.is(ModItems.KNOWLEDGE_PIECE.get()) && KnowledgeQuality.of(out) == KnowledgeQuality.BRIGHT,
            "after 30 seconds: a Bright Knowledge Piece (got " + out.getHoverName().getString() + ")");
        c.check(creator.book().isEmpty(), "the Enchanted Book is used up");
        // a Radiant book with a Bright piece still in the output waits
        creator.setItem(KnowledgeCreatorBlockEntity.BOOK, book(c, Map.of(Enchantments.MENDING, 1, Enchantments.EFFICIENCY, 5, Enchantments.UNBREAKING, 3)));
        for (int t = 0; t < KnowledgeCreatorBlockEntity.MAKE_TICKS + 5; t++) {
            KnowledgeCreatorBlockEntity.serverTick(c.level(), pos, c.level().getBlockState(pos), creator);
        }
        c.check(creator.getItem(KnowledgeCreatorBlockEntity.OUTPUT).getCount() == 1 && !creator.book().isEmpty(),
            "a different quality waits until the output is taken");
        creator.removeItem(KnowledgeCreatorBlockEntity.OUTPUT, 64);
        for (int t = 0; t < KnowledgeCreatorBlockEntity.MAKE_TICKS; t++) {
            KnowledgeCreatorBlockEntity.serverTick(c.level(), pos, c.level().getBlockState(pos), creator);
        }
        c.check(KnowledgeQuality.of(creator.getItem(KnowledgeCreatorBlockEntity.OUTPUT)) == KnowledgeQuality.RADIANT, "then it makes the Radiant piece");
    }

    // At the Librarian Table, the piece's quality multiplies the Persistent chance (and Lucky Librarian doubles it)
    private static void atTheTable(GameTests.Checks c) {
        for (KnowledgeQuality quality : new KnowledgeQuality[]{KnowledgeQuality.FADED, KnowledgeQuality.RADIANT}) {
            for (boolean lucky : new boolean[]{false, true}) {
                ServerPlayer writer = c.player();
                if (lucky) {
                    writer.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.alexh.capitalism.core.ModEffects.BOOK_LESSONS.get(Skill.LUCKY_LIBRARIAN), 20000));
                    writer.getData(ModAttachments.JOBS.get()).add(Job.LIBRARIAN);
                }
                LibrarianTableMenu menu = new LibrarianTableMenu(0, writer.getInventory(), ContainerLevelAccess.NULL);
                int writes = 4000;
                int traits = 0;
                for (int i = 0; i < writes; i++) {
                    menu.getSlot(LibrarianTableMenu.BIG).set(new ItemStack(ModItems.DELICATE_BOOK.get()));
                    menu.getSlot(LibrarianTableMenu.SMALL).set(quality.stack(1));
                    ItemStack result = menu.getSlot(LibrarianTableMenu.RESULT).getItem().copy();
                    menu.getSlot(LibrarianTableMenu.RESULT).onTake(writer, result);
                    Skill skill = SkillBookItem.skill(result);
                    if (skill != null && skill.trait) traits++;
                }
                double expected = Skill.DELICATE_TRAIT_CHANCE * quality.multiplier * (lucky ? 2 : 1);
                c.near(traits / (double) writes, expected, 0.03, quality.title + " piece" + (lucky ? " + Lucky Librarian" : "") + ": Persistent books from Delicate Books");
            }
        }
    }

    // The Whisperer: SIZE pieces, shared by everyone, each sold once, paid in emeralds by quality; restocks
    private static void whispererStock(GameTests.Checks c) {
        WhispererStock stock = WhispererStock.get(c.level().getServer());
        stock.restock(RandomSource.create(7), c.level().getServer().overworld().getGameTime());
        // the mix over many restocks: every quality shows up, Faded most, Radiant least
        int[] seen = new int[4];
        RandomSource random = RandomSource.create(11);
        for (int i = 0; i < 20000; i++) seen[WhispererStock.rollQuality(random).ordinal()]++;
        c.check(seen[0] > seen[1] && seen[1] > seen[2] && seen[2] > seen[3] && seen[3] > 0,
            "random qualities: Faded most, Radiant least, all of them show up (" + seen[0] + "/" + seen[1] + "/" + seen[2] + "/" + seen[3] + ")");

        ServerPlayer rich = c.player();
        ServerPlayer other = c.player();
        rich.getInventory().add(new ItemStack(Items.EMERALD, 30));
        WhispererMenu richMenu = new WhispererMenu(1, rich.getInventory(), WhispererMenu.liveData(rich), null);
        WhispererMenu otherMenu = new WhispererMenu(2, other.getInventory(), WhispererMenu.liveData(other), null);
        KnowledgeQuality first = richMenu.offer(0);
        c.check(first != null, "a fresh stock has something to sell");
        int before = WhispererMenu.emeralds(rich);
        c.check(richMenu.clickMenuButton(rich, 0), "buying the first piece works");
        c.check(WhispererMenu.emeralds(rich) == before - first.emeralds(), "it cost " + first.emeralds() + " emeralds (" + first.title + ")");
        c.check(countPieces(rich.getInventory(), first) == 1, "you get a " + first.title + " Knowledge Piece");
        c.check(richMenu.offer(0) == null && otherMenu.offer(0) == null, "it's gone for everyone (shared stock)");
        c.check(!otherMenu.clickMenuButton(other, 0), "nobody can buy it again");
        ServerPlayer broke = c.player();
        WhispererMenu brokeMenu = new WhispererMenu(3, broke.getInventory(), WhispererMenu.liveData(broke), null);
        c.check(!brokeMenu.clickMenuButton(broke, 1) && brokeMenu.offer(1) != null, "without emeralds you can't buy, and it stays for sale");
        for (int i = 1; i < WhispererStock.SIZE; i++) richMenu.clickMenuButton(rich, i);
        boolean soldOut = true;
        for (int i = 0; i < WhispererStock.SIZE; i++) soldOut &= richMenu.offer(i) == null;
        c.check(soldOut, "all " + WhispererStock.SIZE + " can be bought out");
        stock.restock(RandomSource.create(3), c.level().getServer().overworld().getGameTime());
        boolean full = true;
        for (int i = 0; i < WhispererStock.SIZE; i++) full &= richMenu.offer(i) != null;
        c.check(full, "after the restock he has " + WhispererStock.SIZE + " again");
        c.check(WhispererStock.RESTOCK_TICKS == 72000, "he restocks every hour");
        c.check(richMenu.secondsUntilRestock() > 3500 && richMenu.secondsUntilRestock() <= 3600, "the countdown shows the seconds to his restock (" + richMenu.secondsUntilRestock() + ")");

        // a piece of advice: 1 gold, a tip from the list, a different one when you ask again
        ServerPlayer asker = c.player();
        asker.getInventory().add(new ItemStack(Items.GOLD_INGOT, 2));
        WhispererMenu askMenu = new WhispererMenu(4, asker.getInventory(), WhispererMenu.liveData(asker), null);
        c.check(askMenu.advice() == null, "no advice before you pay");
        c.check(askMenu.clickMenuButton(asker, WhispererMenu.ADVICE), "asking for advice works");
        String firstTip = askMenu.advice();
        c.check(firstTip != null && WhispererAdvice.TIPS.contains(firstTip), "he tells you a tip: " + firstTip);
        c.check(com.alexh.capitalism.economy.GoldMoney.carried(asker) == 9, "it cost 1 gold");
        askMenu.clickMenuButton(asker, WhispererMenu.ADVICE);
        c.check(askMenu.advice() != null && !askMenu.advice().equals(firstTip), "asking again gives a different tip");
        c.check(!askMenu.clickMenuButton(asker, WhispererMenu.ADVICE), "out of gold: no more advice");
    }

    private static int countPieces(Inventory inventory, KnowledgeQuality quality) {
        int count = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(ModItems.KNOWLEDGE_PIECE.get()) && KnowledgeQuality.of(stack) == quality) count += stack.getCount();
        }
        return count;
    }

    // The protected town center: inside (from protectMinY up) nobody but ops breaks blocks; below it, or outside, fine
    private static void protection(GameTests.Checks c) {
        Town.Area area = Town.protectedArea();
        int minY = CapitalismConfig.TOWN_PROTECT_MIN_Y.get();
        BlockPos inside = new BlockPos((area.minX() + area.maxX()) / 2, 70, (area.minZ() + area.maxZ()) / 2);
        BlockPos edge = new BlockPos(area.minX(), minY, area.maxZ());
        BlockPos under = new BlockPos(inside.getX(), minY - 1, inside.getZ());
        BlockPos outside = new BlockPos(area.maxX() + 1, 70, inside.getZ());
        Level level = c.level();
        c.check(Town.isProtected(level, inside), "the middle of town (y 70) is protected");
        c.check(Town.isProtected(level, edge), "the corner of the area at y " + minY + " is protected");
        c.check(!Town.isProtected(level, under), "y " + (minY - 1) + " (deep underground) isn't");
        c.check(!Town.isProtected(level, outside), "one block outside the area isn't");
        c.check(area.minX() == 557796 && area.maxX() == 557966 && area.minZ() == -849948 && area.maxZ() == -849791,
            "the area is the one from your screenshots");

        ServerPlayer player = c.player();
        BreakBlockEvent inTown = NeoForge.EVENT_BUS.post(new BreakBlockEvent(level, inside, Blocks.STONE.defaultBlockState(), player));
        c.check(inTown.isCanceled(), "a player can't break a block in the town center");
        BreakBlockEvent deep = NeoForge.EVENT_BUS.post(new BreakBlockEvent(level, under, Blocks.STONE.defaultBlockState(), player));
        c.check(!deep.isCanceled(), "a player can break blocks deep under the town");
        BreakBlockEvent away = NeoForge.EVENT_BUS.post(new BreakBlockEvent(level, outside, Blocks.STONE.defaultBlockState(), player));
        c.check(!away.isCanceled(), "a player can break blocks outside the town");

        Town.Area walk = Town.walkArea();
        c.check(walk.minX() == 557828 && walk.maxX() == 557917 && walk.minZ() == -849933 && walk.maxZ() == -849826,
            "the Whisperer's streets are the ones from your screenshots");
    }

    // the Town Hall's offices: appointing, one office each, removing, the title before the name
    private static void townHall(GameTests.Checks c) {
        TownHall hall = TownHall.get(c.level().getServer());
        ServerPlayer p = c.player();
        hall.remove(TownHall.Office.MAYOR);
        hall.remove(TownHall.Office.VICE_MAYOR);
        hall.remove(TownHall.Office.TREASURER);
        hall.appoint(TownHall.Office.MAYOR, p);
        c.check(hall.officeOf(p.getUUID()) == TownHall.Office.MAYOR && hall.holder(TownHall.Office.MAYOR).id().equals(p.getUUID()),
            "an appointed Mayor holds the office");
        p.refreshDisplayName();
        c.check(p.getDisplayName().getString().startsWith("[Mayor] "), "...and has the title before their name (" + p.getDisplayName().getString() + ")");
        hall.appoint(TownHall.Office.TREASURER, p);
        c.check(hall.officeOf(p.getUUID()) == TownHall.Office.TREASURER && hall.holder(TownHall.Office.MAYOR) == null,
            "one office each: made Treasurer, they leave the Mayor's office empty");
        hall.remove(TownHall.Office.TREASURER);
        p.refreshDisplayName();
        c.check(hall.officeOf(p.getUUID()) == null && !p.getDisplayName().getString().startsWith("["), "removed: no office, no title");
    }

    // picking jobs (Jobs.md): into a free slot only, for good; a Wishing Star adds a slot; a Changing Star lets them change
    private static void jobPicking(GameTests.Checks c) {
        net.minecraft.server.level.ServerPlayer p = c.player();
        var key = com.alexh.capitalism.core.ModAttachments.JOBS.get();
        p.setData(key, new com.alexh.capitalism.occupation.JobData());
        com.alexh.capitalism.occupation.Job cook = com.alexh.capitalism.occupation.Job.COOK;
        com.alexh.capitalism.occupation.Job smith = com.alexh.capitalism.occupation.Job.WEAPONSMITH;
        com.alexh.capitalism.occupation.Job farmer = com.alexh.capitalism.occupation.Job.FARMER;
        c.check(p.getData(key).jobs().isEmpty(), "a new player has no jobs");
        com.alexh.capitalism.occupation.JobCommands.togglePicked(p, cook.ordinal());
        com.alexh.capitalism.occupation.JobCommands.togglePicked(p, farmer.ordinal());
        c.check(p.getData(key).has(cook) && p.getData(key).has(farmer), "/pick: one Essential job and one Fun job");
        com.alexh.capitalism.occupation.JobCommands.togglePicked(p, smith.ordinal());
        c.check(!p.getData(key).has(smith), "...not a second Essential job (no free slot)");
        com.alexh.capitalism.occupation.JobCommands.togglePicked(p, cook.ordinal());
        c.check(p.getData(key).has(cook), "...and they're for good: clicking one you hold doesn't drop it");
        var data = p.getData(key);
        boolean first = data.addSlot(com.alexh.capitalism.occupation.Job.Category.ESSENTIAL);
        boolean second = data.addSlot(com.alexh.capitalism.occupation.Job.Category.ESSENTIAL);
        p.setData(key, data);
        com.alexh.capitalism.occupation.JobCommands.togglePicked(p, smith.ordinal());
        c.check(first && !second && p.getData(key).has(smith) && p.getData(key).has(cook),
            "an Essential Wishing Star gives a second Essential slot (only one more), and it can be filled");
        com.alexh.capitalism.occupation.ChangingStar.startChanging(p);
        com.alexh.capitalism.occupation.JobCommands.togglePicked(p, cook.ordinal());
        boolean dropped = !p.getData(key).has(cook);
        // only ONE job changes: no letting go of a second, no picking from the other kind
        com.alexh.capitalism.occupation.JobCommands.togglePicked(p, smith.ordinal());
        c.check(p.getData(key).has(smith), "a Changing Star changes only one job: a second can't be let go");
        com.alexh.capitalism.occupation.JobCommands.togglePicked(p, com.alexh.capitalism.occupation.Job.LIBRARIAN.ordinal());
        c.check(!p.getData(key).has(com.alexh.capitalism.occupation.Job.LIBRARIAN), "...and the new one must be the kind let go of (Essential here, not Fun)");
        com.alexh.capitalism.occupation.JobCommands.togglePicked(p, com.alexh.capitalism.occupation.Job.ENCHANTER.ordinal());
        c.check(dropped && p.getData(key).has(com.alexh.capitalism.occupation.Job.ENCHANTER) && !p.getData(key).changing(),
            "a Changing Star lets them drop a job and pick another; once every slot is full the change is over");
        // /admin jobs reset: no jobs, pick again; Wishing Star slots kept
        com.alexh.capitalism.core.AdminCommands.resetJobs(p);
        c.check(p.getData(key).jobs().isEmpty() && p.getData(key).slots(com.alexh.capitalism.occupation.Job.Category.ESSENTIAL) == 2 && !p.getData(key).changing(),
            "/admin jobs reset: their jobs are cleared to pick again, their extra slot kept");
        p.setData(key, new com.alexh.capitalism.occupation.JobData());
    }
}
