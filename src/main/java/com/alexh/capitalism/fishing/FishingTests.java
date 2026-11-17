package com.alexh.capitalism.fishing;

import com.alexh.capitalism.core.ModBlocks;
import com.alexh.capitalism.core.ModComponents;
import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.decor.BloodPartBlock;
import com.alexh.capitalism.economy.GoldMoney;
import com.alexh.capitalism.economy.NpcPricing;
import com.alexh.capitalism.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

// Fishing's logic (Fishing.md):
//  - where and when each rare catch bites; rarest first; Luck makes them likelier (at most twice);
//  - a catch is weighed within its range and carries who caught it and when; the bottle and the chest aren't weighed;
//  - the Fisherman's prices: heavier pays more, ordinary fish and unweighed finds aren't bought, a Soggy Note pays what's
//    written on it; the plaque's price goes through NpcPricing;
//  - the Message in a Bottle (a glass bottle back and a note for 1 to 5 gold); the pirate chest's loot;
//  - a plaque as wide as its catch, needing its wall free, breaking whole into one plaque that keeps the story.
public final class FishingTests {

    static {
        GameTests.register("fishing", FishingTests::fishing);
        GameTests.register("fishing_collection", FishingTests::collection);
        GameTests.register("fishwatch", FishingTests::fishWatch);
    }

    private FishingTests() {
    }

    public static void init() {
    }

    // a random whose nextDouble is always `v` (to make roll() deterministic)
    private static RandomSource fixed(double v) {
        return new SingleThreadedRandomSource(1L) {
            @Override
            public double nextDouble() {
                return v;
            }
        };
    }

    private static Holder<Biome> biome(ServerLevel level, ResourceKey<Biome> key) {
        return level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(key);
    }

    private static void fishing(GameTests.Checks c) {
        ServerLevel level = c.level();
        ServerPlayer player = c.player();
        Holder<Biome> deep = biome(level, Biomes.DEEP_OCEAN), warm = biome(level, Biomes.WARM_OCEAN), river = biome(level, Biomes.RIVER),
            plains = biome(level, Biomes.PLAINS), swamp = biome(level, Biomes.SWAMP);

        // ── where and when ──
        RareCatch.Where deepNight = new RareCatch.Where(deep, false, false, false, false);
        RareCatch.Where deepDay = new RareCatch.Where(deep, true, false, false, false);
        RareCatch.Where plainsDay = new RareCatch.Where(plains, true, false, false, false);
        c.check(RareCatch.OARFISH.livesIn(deepNight) && !RareCatch.OARFISH.livesIn(deepDay) && RareCatch.ANGLERFISH.livesIn(deepNight),
            "the Oarfish and the Anglerfish bite in the deep ocean at night only");
        c.check(RareCatch.KRAKEN_TENTACLE.livesIn(new RareCatch.Where(deep, true, true, true, false)) && !RareCatch.KRAKEN_TENTACLE.livesIn(deepDay),
            "the Kraken Tentacle needs a thunderstorm over the deep ocean");
        c.check(RareCatch.ELECTRIC_EEL.livesIn(new RareCatch.Where(river, true, true, true, false)) && !RareCatch.ELECTRIC_EEL.livesIn(new RareCatch.Where(river, true, false, false, false)),
            "the Electric Eel bites in a river in a thunderstorm");
        c.check(RareCatch.GIANT_CATFISH.livesIn(new RareCatch.Where(swamp, false, false, false, false)) && !RareCatch.GIANT_CATFISH.livesIn(new RareCatch.Where(swamp, true, false, false, false)),
            "the Giant Catfish bites in a swamp at night");
        c.check(RareCatch.PUFFERFISH_KING.livesIn(new RareCatch.Where(warm, true, false, false, false)) && !RareCatch.PUFFERFISH_KING.livesIn(deepDay),
            "the Pufferfish King lives in warm oceans");
        c.check(RareCatch.OCEAN_SUNFISH.livesIn(deepDay) && !RareCatch.OCEAN_SUNFISH.livesIn(new RareCatch.Where(deep, true, true, false, false)),
            "the Ocean Sunfish only basks on a clear day");
        c.check(RareCatch.GHOST_KOI.livesIn(new RareCatch.Where(plains, false, false, false, true)) && !RareCatch.GHOST_KOI.livesIn(plainsDay),
            "the Ghost Koi bites under a Blood Moon only");
        c.check(RareCatch.GOLDEN_LEVIATHAN_SCALE.livesIn(plainsDay) && !RareCatch.MAKO_SHARK.livesIn(plainsDay) && !RareCatch.MESSAGE_IN_A_BOTTLE.livesIn(plainsDay),
            "the Leviathan Scale can come up anywhere; the shark and the bottle need the sea");

        // ── the roll: rarest first, Luck ──
        c.check(RareCatchModifier.roll(deepNight, 0, 0, fixed(0.0)) == null && RareCatchModifier.roll(deepNight, 0, 10, fixed(0.0)) == null,
            "a rod without Luck of the Sea never lands a rare catch, however lucky the roll or the angler");
        c.check(RareCatchModifier.roll(deepNight, 1, 0, fixed(0.00015)) == RareCatch.OARFISH, "Luck of the Sea I, a 1-in-6,700 roll at night in the deep: the Oarfish (rarest that bites)");
        c.check(RareCatchModifier.roll(deepDay, 1, 0, fixed(0.00015)) == RareCatch.MAKO_SHARK, "the same roll by day: the Mako Shark");
        c.check(RareCatchModifier.roll(deepNight, 3, 0, fixed(0.00015)) == RareCatch.GOLDEN_LEVIATHAN_SCALE, "with Luck of the Sea III the same roll lands the Leviathan Scale");
        c.check(RareCatchModifier.roll(deepNight, 1, 0, fixed(0.5)) == null, "a high roll: no rare catch (the ordinary loot stays)");
        c.near(RareCatch.MAKO_SHARK.chanceFor(0, 0), 0, 1e-12, "no Luck of the Sea: no chance at all");
        c.near(RareCatch.MAKO_SHARK.chanceFor(1, 0), 1.0 / 500, 1e-9, "the Mako Shark with Luck of the Sea I: 1 in 500");
        c.near(RareCatch.MAKO_SHARK.chanceFor(2, 0), 1.5 / 500, 1e-9, "Luck of the Sea II: x1.5");
        c.near(RareCatch.MAKO_SHARK.chanceFor(3, 0), 2.0 / 500, 1e-9, "Luck of the Sea III: x2");
        c.near(RareCatch.MAKO_SHARK.chanceFor(1, 2), 1.3 / 500, 1e-9, "other Luck adds 15% a point on top (2 points: x1.3)");
        c.near(RareCatch.MAKO_SHARK.chanceFor(3, 20), 3.0 / 500, 1e-9, "the total is capped at x3");
        ItemStack plainRod = new ItemStack(Items.FISHING_ROD), seaRod = new ItemStack(Items.FISHING_ROD);
        seaRod.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LUCK_OF_THE_SEA), 2);
        c.check(RareCatchModifier.luckOfTheSea(level, plainRod) == 0 && RareCatchModifier.luckOfTheSea(level, seaRod) == 2, "the rod's Luck of the Sea level is read off it (none: 0; II: 2)");

        // ── tiers and the chance line ──
        c.check(RareCatch.GOLDEN_KOI.tier() == CatchTier.UNCOMMON && RareCatch.SUNKEN_PIRATE_CHEST.tier() == CatchTier.RARE
            && RareCatch.MAKO_SHARK.tier() == CatchTier.EPIC && RareCatch.GHOST_KOI.tier() == CatchTier.LEGENDARY
            && RareCatch.GOLDEN_LEVIATHAN_SCALE.tier() == CatchTier.MYTHIC, "each catch has its tier (koi uncommon ... scale mythic)");
        c.check(RareCatch.OARFISH.chanceLines().get(1).equals("I 1 in 3,000 · II 1 in 2,000 · III 1 in 1,500") && RareCatch.GIANT_CATFISH.oddsAt(1).equals("1 in 60"),
            "the tooltip's chance line reads \"I 1 in 3,000 · II 1 in 2,000 · III 1 in 1,500\"");
        c.check(net.minecraft.network.chat.TextColor.fromRgb(CatchTier.LEGENDARY.color).equals(
                new ItemStack(ModItems.RARE_CATCHES.get(RareCatch.OARFISH).get()).getHoverName().getStyle().getColor()),
            "an Oarfish's name is in the Legendary colour");

        // ── weighed, named, dated ──
        boolean inRange = true;
        RandomSource random = RandomSource.create(42L);
        for (int i = 0; i < 40; i++) {
            ItemStack shark = RareCatchModifier.make(RareCatch.MAKO_SHARK, "Alex", "Summer 9, Year 2", random);
            CatchInfo info = shark.get(ModComponents.CATCH_INFO.get());
            inRange &= shark.is(ModItems.RARE_CATCHES.get(RareCatch.MAKO_SHARK).get()) && info != null
                && info.weight() >= 150 && info.weight() <= 400 && info.weight() == Math.round(info.weight());
        }
        c.check(inRange, "a Mako Shark weighs 150 to 400 lb, in whole pounds");
        ItemStack koi = RareCatchModifier.make(RareCatch.GOLDEN_KOI, "Alex", "Summer 9, Year 2", random);
        CatchInfo koiInfo = koi.get(ModComponents.CATCH_INFO.get());
        c.check(koiInfo != null && koiInfo.weight() >= 4 && koiInfo.weight() <= 12 && koiInfo.line().contains("lb · caught by Alex · Summer 9, Year 2"),
            "a koi has its weight, catcher and date: " + (koiInfo == null ? "none" : koiInfo.line()));
        c.check(new CatchInfo(312, "Alex", "Summer 9, Year 2").line().equals("312 lb · caught by Alex · Summer 9, Year 2"),
            "the tooltip line reads \"312 lb · caught by Alex · Summer 9, Year 2\"");
        ItemStack bottle = RareCatchModifier.make(RareCatch.MESSAGE_IN_A_BOTTLE, "Alex", "x", random);
        c.check(!bottle.has(ModComponents.CATCH_INFO.get()), "the bottle isn't weighed (so bottles stack)");

        // ── the Fisherman's prices ──
        ItemStack light = shark(1.0F, 150), heavy = shark(1.0F, 400), mid = shark(1.0F, 275);
        long lightPay = FishMarket.valueOf(light), midPay = FishMarket.valueOf(mid), heavyPay = FishMarket.valueOf(heavy);
        c.check(lightPay < midPay && midPay < heavyPay, "a heavier shark sells for more (" + lightPay + " < " + midPay + " < " + heavyPay + " nuggets)");
        c.check(midPay == 15L * GoldMoney.INGOT, "a middling Mako Shark sells for 15 gold");
        c.check(heavyPay == Math.round(15 * 9 * 1.4) && lightPay == Math.round(15 * 9 * 0.6), "the heaviest pays 140%, the lightest 60%");
        c.check(!FishMarket.buys(new ItemStack(Items.COD)) && !FishMarket.buys(new ItemStack(ModItems.RARE_CATCHES.get(RareCatch.SUNKEN_PIRATE_CHEST).get())),
            "he doesn't buy cod, nor an unopened chest");
        ItemStack note = RareFindItems.MessageInABottle.note(random);
        Integer noteGold = note.get(ModComponents.NOTE_GOLD.get());
        c.check(noteGold != null && FishMarket.valueOf(note) == noteGold && noteGold >= 9 && noteGold <= 5 * 9 && noteGold % 9 == 0,
            "a Soggy Note is worth the 1 to 5 gold written on it (" + noteGold + " nuggets)");
        long fair = FishMarket.plaqueFair(mid);
        c.check(fair == 3L * 9 * 2 + midPay / 10, "a Mako plaque's fair price: 3 gold a block of its 2 + a tenth of the shark");
        c.check(FishMarket.plaquePrice(mid, player) == NpcPricing.npcPrice(fair, player) && FishMarket.plaquePrice(mid, player) > fair,
            "the plaque's price goes through NpcPricing (with his markup)");
        c.check(!FishMarket.mountable(note) && !FishMarket.mountable(bottle) && FishMarket.mountable(koi), "notes and bottles can't be mounted; a koi can");

        // ── the Message in a Bottle ──
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.RARE_CATCHES.get(RareCatch.MESSAGE_IN_A_BOTTLE).get(), 2));
        player.getMainHandItem().getItem().use(level, player, InteractionHand.MAIN_HAND);
        int bottles = player.getInventory().countItem(Items.GLASS_BOTTLE), notes = player.getInventory().countItem(ModItems.SOGGY_NOTE.get());
        c.check(player.getMainHandItem().getCount() == 1 && bottles == 1 && notes == 1, "uncorking one: a glass bottle back and a Soggy Note");

        // ── the pirate chest ──
        boolean goldOk = true, sawHat = false, sawGem = false;
        RandomSource chestRandom = RandomSource.create(7L);
        for (int i = 0; i < 200; i++) {
            List<ItemStack> loot = RareFindItems.PirateChest.loot(chestRandom);
            long gold = 0;
            for (ItemStack s : loot) {
                gold += GoldMoney.valueOf(s);
                sawHat |= s.is(ModItems.PIRATE_HAT.get());
                sawGem |= s.is(Items.EMERALD) || s.is(Items.DIAMOND);
            }
            goldOk &= gold >= 2L * 9 && gold < 7L * 9;
        }
        c.check(goldOk, "every pirate chest holds 2 to 6 gold");
        c.check(sawHat && sawGem, "chests sometimes hold the Pirate Hat, and gems");
        c.check(new ItemStack(ModItems.PIRATE_HAT.get()).get(net.minecraft.core.component.DataComponents.EQUIPPABLE) != null
                && new ItemStack(ModItems.PIRATE_HAT.get()).get(net.minecraft.core.component.DataComponents.EQUIPPABLE).slot() == net.minecraft.world.entity.EquipmentSlot.HEAD,
            "the Pirate Hat is worn on the head");

        // ── plaques ──
        // a stone wall along z 0; plaques hang on its south face
        for (int x = 0; x < 5; x++) for (int y = 1; y < 4; y++) level.setBlock(BlockPos.containing(c.at(x, y, 0)), Blocks.STONE.defaultBlockState(), 3);
        Vec3 stand = c.at(2.5, 1, 3.5);
        player.snapTo(stand.x, stand.y, stand.z, 180.0F, 0.0F);
        ItemStack oarfish = RareCatchModifier.make(RareCatch.OARFISH, "Alex", "Winter 3, Year 1", random);
        ItemStack plaque = FishMarket.plaqueFor(oarfish);
        c.check(plaque.is(ModItems.FISH_PLAQUE.get()) && FishPlaqueBlock.widthOf(plaque) == 3
            && oarfish.get(ModComponents.CATCH_INFO.get()).equals(plaque.get(ModComponents.CATCH_INFO.get())), "an Oarfish plaque is 3 wide and keeps its story");
        BlockPos wallAt = BlockPos.containing(c.at(2, 2, 0));
        BlockPos main = wallAt.south();
        // something in the way: no plaque
        BlockPos inTheWay = FishPlaqueBlock.cell(main, Direction.SOUTH, -1);
        level.setBlock(inTheWay, Blocks.OAK_FENCE.defaultBlockState(), 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, plaque.copy());
        place(player, wallAt);
        c.check(level.getBlockState(main).isAir() && player.getMainHandItem().getCount() == 1, "it won't go up without 3 blocks of wall free");
        level.setBlock(inTheWay, Blocks.AIR.defaultBlockState(), 3);
        place(player, wallAt);
        boolean parts = true;
        for (int i = -1; i <= 1; i += 2) {
            BlockPos p = FishPlaqueBlock.cell(main, Direction.SOUTH, i);
            parts &= level.getBlockState(p).getBlock() instanceof BloodPartBlock && BloodPartBlock.mainOf(level.getBlockState(p), p).equals(main);
        }
        c.check(level.getBlockState(main).is(ModBlocks.FISH_PLAQUE.get()) && parts && player.getMainHandItem().isEmpty(),
            "with room it hangs, 3 blocks wide, centred where it was put (the plaque used up)");
        c.check(level.getBlockEntity(main) instanceof FishPlaqueBlockEntity be && be.kind() == RareCatch.OARFISH && be.info() != null
            && be.info().catcher().equals("Alex"), "the placed plaque knows its catch and catcher");
        level.destroyBlock(FishPlaqueBlock.cell(main, Direction.SOUTH, 1), true);
        int dropped = 0;
        boolean kept = false;
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(main).inflate(4))) {
            if (e.getItem().is(ModItems.FISH_PLAQUE.get())) {
                dropped += e.getItem().getCount();
                kept = "oarfish".equals(e.getItem().get(ModComponents.PLAQUE_CATCH.get()))
                    && oarfish.get(ModComponents.CATCH_INFO.get()).equals(e.getItem().get(ModComponents.CATCH_INFO.get()));
            }
            e.discard();
        }
        boolean gone = level.getBlockState(main).isAir();
        for (int i = -1; i <= 1; i += 2) gone &= level.getBlockState(FishPlaqueBlock.cell(main, Direction.SOUTH, i)).isAir();
        c.check(gone && dropped == 1 && kept, "breaking its end takes the whole plaque down: one plaque, its story kept");
        for (int x = 0; x < 5; x++) for (int y = 1; y < 4; y++) level.setBlock(BlockPos.containing(c.at(x, y, 0)), Blocks.AIR.defaultBlockState(), 3);
    }

    // the Fisherman's Collection: only catches count; the prize once, only when complete
    private static void collection(GameTests.Checks c) {
        ServerLevel level = c.level();
        ServerPlayer angler = c.player();
        RandomSource random = RandomSource.create(9L);
        c.check(CatchCollection.of(angler).count() == 0, "a new player's collection is empty");
        RareCatchModifier.land(level, angler, RareCatch.GOLDEN_KOI, random);
        RareCatchModifier.land(level, angler, RareCatch.GOLDEN_KOI, random);
        var koi = CatchCollection.of(angler).get(RareCatch.GOLDEN_KOI);
        c.check(koi != null && koi.count() == 2 && koi.heaviest() >= 4 && koi.heaviest() <= 12, "landing a koi twice records it: caught 2, its heaviest weight");
        angler.getInventory().add(RareCatchModifier.make(RareCatch.MAKO_SHARK, "someone else", "x", random));
        angler.getInventory().add(new ItemStack(ModItems.RARE_CATCHES.get(RareCatch.OARFISH).get()));
        c.check(!CatchCollection.of(angler).has(RareCatch.MAKO_SHARK) && !CatchCollection.of(angler).has(RareCatch.OARFISH),
            "being given a catch (or spawning one) doesn't put it in your collection");
        angler.getInventory().clearContent();
        c.check(!CatchCollection.claim(angler) && angler.getInventory().countItem(ModItems.ESSENTIAL_WISHING_STAR.get()) == 0,
            "the prize can't be claimed with the collection incomplete");
        for (RareCatch kind : RareCatch.values()) if (!CatchCollection.of(angler).has(kind)) CatchCollection.record(angler, kind, 1.0F);
        c.check(CatchCollection.of(angler).complete() && CatchCollection.of(angler).count() == 15, "all 15 caught: complete");
        boolean first = CatchCollection.claim(angler);
        boolean second = CatchCollection.claim(angler);
        c.check(first && !second && angler.getInventory().countItem(ModItems.ESSENTIAL_WISHING_STAR.get()) == 1 && CatchCollection.of(angler).claimed(),
            "the prize (one Essential Wishing Star) can be claimed once, and only once");
    }

    // FishWatch: an inhuman pattern is flagged, a human one isn't; flagging never restricts; restrict stops rare rolls
    private static void fishWatch(GameTests.Checks c) {
        ServerPlayer bot = c.player(), human = c.player();
        FishWatch.clear(bot);
        FishWatch.clear(human);
        Vec3 spot = c.at(2.5, 1.0, 4.5);
        long t = 100000;
        // the bot: never moves, casts every 300 ticks exactly, the bite after 200, reeled in 3 ticks later, same spot
        FishWatch.moved(bot, t);
        FishWatch.talked(bot, t);
        for (int i = 0; i < 30; i++) {
            long at = t + i * 300L;
            FishWatch.cast(bot, at);
            FishWatch.bite(bot, at + 200);
            FishWatch.caught(bot, at + 203, spot, false);
        }
        FishWatch.Stats b = FishWatch.stats(bot);
        c.check(FishWatch.flagged(b) && b.score >= FishWatch.FLAG, "an inhuman pattern is flagged (score " + b.score + ": " + String.join(", ", b.reasons) + ")");
        c.check(!FishWatch.restricted(bot), "being flagged never restricts anyone by itself");
        // the human: reacts in 4 to 20 ticks, casts at uneven times, looks around, the bobber lands all over
        RandomSource r = RandomSource.create(3L);
        long at = t;
        FishWatch.moved(human, at);
        for (int i = 0; i < 30; i++) {
            at += 260 + r.nextInt(200);
            FishWatch.cast(human, at);
            FishWatch.moved(human, at + 40);
            FishWatch.bite(human, at + 150);
            FishWatch.caught(human, at + 150 + 4 + r.nextInt(17), spot.add(r.nextDouble() * 2 - 1, 0, r.nextDouble() * 2 - 1), false);
        }
        FishWatch.Stats h = FishWatch.stats(human);
        c.check(!FishWatch.flagged(h) && h.score < FishWatch.SUSPICIOUS, "a human-like pattern isn't flagged (score " + h.score + ")");
        // restrict: no rare rolls (even a roll that would land the rarest); unrestrict: back
        RareCatch.Where anywhere = new RareCatch.Where(c.level().registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.DEEP_OCEAN), false, false, false, false);
        c.check(RareCatchModifier.rollFor(bot, anywhere, 1, 0, fixed(0.0)) != null, "unrestricted, a lucky roll lands a rare catch");
        FishWatch.restrict(bot, true);
        c.check(FishWatch.restricted(bot) && RareCatchModifier.rollFor(bot, anywhere, 1, 0, fixed(0.0)) == null, "restricted: no rare catches, only ordinary fish");
        FishWatch.restrict(bot, false);
        c.check(RareCatchModifier.rollFor(bot, anywhere, 1, 0, fixed(0.0)) != null, "unrestricted again: rare catches are back");
        FishWatch.clear(bot);
        c.check(FishWatch.stats(bot).score == 0 && FishWatch.stats(bot).catches == 0, "clearing wipes their suspicion stats");
    }

    private static ItemStack shark(float unused, float weight) {
        ItemStack s = new ItemStack(ModItems.RARE_CATCHES.get(RareCatch.MAKO_SHARK).get());
        s.set(ModComponents.CATCH_INFO.get(), new CatchInfo(weight, "Alex", "Summer 9, Year 2"));
        return s;
    }

    // right-click the south face of `wall` with what's in the hand
    private static void place(ServerPlayer player, BlockPos wall) {
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(wall).add(0, 0, 0.5), Direction.SOUTH, wall, false);
        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }
}
