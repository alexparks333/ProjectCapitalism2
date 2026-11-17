package com.alexh.capitalism.boats;

import com.alexh.capitalism.core.ModEntities;
import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.economy.GoldMoney;
import com.alexh.capitalism.economy.NpcPricing;
import com.alexh.capitalism.gametest.GameTests;
import com.alexh.capitalism.npc.BoatMaster;
import com.alexh.capitalism.npc.NpcShops;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.List;

// The Boat Master and the Pirate Ship (Boats.md): his shop (cheap boats, the Premium tab, prices through NpcPricing,
// nothing to eat or drink), the Pirate Ship's item (one to a stack, doesn't burn, places the ship) and its 10% speed.
public final class BoatTests {

    static {
        GameTests.register("boats", BoatTests::boats);
        GameTests.register("sub_flashlight", BoatTests::flashlight);
    }

    private BoatTests() {
    }

    public static void init() {
    }

    private static void boats(GameTests.Checks c) {
        ServerPlayer player = c.player();
        // the shop
        List<NpcShops.Section> sections = NpcShops.sections(NpcShops.BOAT_MASTER);
        c.check(sections.size() == 5 && sections.get(2).title().equals("Premium") && sections.get(3).title().equals("Legendary"),
            "the Boat Master has Boats, More Boats, Premium, Legendary and Legendary II tabs");
        // every Premium boat at its price (NPC price, gold)
        Object[][] prices = {{ModItems.CASTAWAY_TUB, 50}, {ModItems.SPEEDBOAT, 100}, {ModItems.SWAMP_RAFT, 150}, {ModItems.YACHT, 300},
            {ModItems.LONGSHIP, 400}, {ModItems.PADDLEWHEELER, 450}, {ModItems.PIRATE_SHIP, 500}, {ModItems.TEAPOT, 550},
            {ModItems.STEAMBOAT, 350}, {ModItems.GHOST_SHIP, 666}, {ModItems.SUBMERSIBLE, 750}, {ModItems.FAIRY_BOAT, 800}, {ModItems.LEVIATHAN, 900}};
        for (Object[] p : prices) {
            net.minecraft.world.item.Item boatItem = ((java.util.function.Supplier<? extends net.minecraft.world.item.Item>) p[0]).get();
            NpcShops.Offer offer = NpcShops.all(NpcShops.BOAT_MASTER).stream().filter(o -> o.item().get() == boatItem).findFirst().orElse(null);
            long price = offer == null ? -1 : NpcPricing.npcPrice(offer.fairNuggets(), player);
            c.check(price == GoldMoney.fromGold((Integer) p[1]), boatItem + " costs " + p[1] + " gold at the NPC (" + GoldMoney.format(price) + ")");
        }
        List<NpcShops.Offer> all = NpcShops.all(NpcShops.BOAT_MASTER);
        NpcShops.Offer oak = all.stream().filter(o -> o.item().get() == Items.OAK_BOAT).findFirst().orElse(null);
        c.check(oak != null && NpcPricing.npcPrice(oak.fairNuggets(), player) == 3 && oak.maxStock() == 0,
            "an oak boat is cheap (3 nuggets at the NPC) and never runs out");
        NpcShops.Offer ship = all.stream().filter(o -> o.item().get() == ModItems.PIRATE_SHIP.get()).findFirst().orElse(null);
        long shipPrice = ship == null ? -1 : NpcPricing.npcPrice(ship.fairNuggets(), player);
        c.check(shipPrice == GoldMoney.fromGold(500), "the Pirate Ship costs 500 gold at the NPC (" + GoldMoney.format(shipPrice) + ")");
        c.check(ship != null && ship.maxStock() == 2 && ship.forever(), "two Pirate Ships ever, never restocking");
        // the Legendary boats: 2 of each ever, shared by everyone, the restock doesn't bring more; Premium ones restock
        com.alexh.capitalism.npc.NpcStock stock = new com.alexh.capitalism.npc.NpcStock();
        int shipAt = all.indexOf(ship);
        NpcShops.Offer yacht = all.stream().filter(o -> o.item().get() == ModItems.YACHT.get()).findFirst().orElse(null);
        int yachtAt = all.indexOf(yacht);
        c.check(stock.take(NpcShops.BOAT_MASTER, shipAt, 3) == 2 && stock.left(NpcShops.BOAT_MASTER, shipAt) == 0, "only two Pirate Ships can be bought");
        c.check(stock.take(NpcShops.BOAT_MASTER, yachtAt, 2) == 2, "two Yachts bought");
        stock.restockNow();
        c.check(stock.left(NpcShops.BOAT_MASTER, shipAt) == 0 && stock.soldForever(NpcShops.BOAT_MASTER, ship) == 2, "...and after a restock there are still none");
        c.check(stock.left(NpcShops.BOAT_MASTER, yachtAt) == 2, "...but the Yachts (Premium) are back");
        c.check(sections.get(3).offers().stream().allMatch(NpcShops.Offer::forever) && sections.get(4).offers().stream().allMatch(NpcShops.Offer::forever)
            && sections.get(2).offers().stream().noneMatch(NpcShops.Offer::forever), "every Legendary boat never restocks; Premium ones do");
        c.check(all.stream().noneMatch(o -> o.stack().has(DataComponents.FOOD) || o.stack().has(DataComponents.CONSUMABLE)),
            "nothing he sells is food or drink (so nothing with effects)");
        c.check(sections.stream().allMatch(s -> s.offers().size() <= 7), "every tab fits the shop screen (7 rows)");
        // the item
        ItemStack item = new ItemStack(ModItems.PIRATE_SHIP.get());
        c.check(item.getMaxStackSize() == 1 && item.has(DataComponents.DAMAGE_RESISTANT), "one to a stack, and it doesn't burn");
        PirateShip entity = ModEntities.PIRATE_SHIP.get().create(c.level(), EntitySpawnReason.COMMAND);
        c.check(entity != null && entity.getPickResult().is(ModItems.PIRATE_SHIP.get()), "the ship is the Pirate Ship (it drops its item)");
        // 10% faster: its extra push each tick is a tenth of a boat's, straight ahead
        Vec3 push = PirateShip.extraPush(0.0F);
        c.near(push.z, 0.004, 1e-6, "facing south its extra push is 0.004 south (a boat's is 0.04)");
        c.near(push.x, 0.0, 1e-6, "...and nothing sideways");
        Vec3 east = PirateShip.extraPush(-90.0F);
        c.near(east.x, 0.004, 1e-6, "facing east it pushes east");
        // a boat's top speed on still water (motion x0.9 each tick, +0.04): the ship's is 10% higher
        double boat = 0;
        double fast = 0;
        for (int i = 0; i < 400; i++) {
            boat = boat * 0.9 + 0.04;
            fast = fast * 0.9 + 0.04 + push.z;
        }
        c.near(fast / boat, 1.10, 1e-6, "its top speed is 10% higher than a boat's");
        // the Boat Master
        BoatMaster master = ModEntities.BOAT_MASTER.get().create(c.level(), EntitySpawnReason.COMMAND);
        if (master != null) master.setUp();
        c.check(master != null && master.skin().equals(NpcShops.BOAT_MASTER) && master.job() == null
            && master.getMainHandItem().is(ModItems.PIRATE_SHIP.get()), "the Boat Master: his own skin, no job, a little Pirate Ship in his arms");
        c.check(master != null && !master.stillWanted(), "without a spot (/npc setspot boat_master) he doesn't stay");
        // the lock: whoever places it owns it; nobody else gets in until it's unlocked
        ServerPlayer owner = c.player();
        ServerPlayer other = c.player();
        PirateShip locked = ModEntities.PIRATE_SHIP.get().create(c.level(), EntitySpawnReason.COMMAND);
        if (locked != null) locked.setOwner(owner);
        c.check(locked != null && locked.allows(owner) && !locked.allows(other) && locked.isLocked(), "a placed boat is locked to its owner");
        c.check(locked != null && !locked.canBeLeashed(), "...and nobody can tow it away on a lead");
        // the Submersible's flashlight lights the water ahead (waterlogged light), and the water comes back after
        Submersible sub = ModEntities.SUBMERSIBLE.get().create(c.level(), EntitySpawnReason.COMMAND);
        if (sub != null) {
            net.minecraft.core.BlockPos base = net.minecraft.core.BlockPos.containing(c.at(2, 3, 8));
            // a pool with a stone wall at its far end, for the beam to land on
            for (int dx = 0; dx < 9; dx++) for (int dy = -3; dy <= 0; dy++) for (int dz = -2; dz <= 2; dz++)
                c.level().setBlock(base.offset(dx, dy, dz), dx == 8 ? net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()
                    : net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), 3);
            sub.snapTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, -90.0F, 0.0F);
            c.level().addFreshEntity(sub);
            sub.toggleFlashlight();
            sub.refreshLights();
            java.util.List<net.minecraft.core.BlockPos> lamp = sub.placedLights().stream()
                .filter(p -> c.level().getBlockState(p).is(net.minecraft.world.level.block.Blocks.LIGHT)
                    && c.level().getBlockState(p).getValue(net.minecraft.world.level.block.LightBlock.WATERLOGGED)).toList();
            c.check(!lamp.isEmpty(), "the Submersible's flashlight lights the water where its beam lands (" + lamp.size() + ")");
            c.check(lamp.stream().allMatch(p -> p.getX() - base.getX() >= 2), "...ahead of it, not round the boat (" + lamp + ")");
            sub.toggleFlashlight();
            sub.refreshLights();
            c.check(lamp.stream().allMatch(p -> c.level().getBlockState(p).is(net.minecraft.world.level.block.Blocks.WATER)),
                "...and turned off, the water is just water again");
            sub.discard();
        }
        // the Spectral Galleon floats on lava (its water checks count lava); a Speedboat there doesn't; it doesn't burn
        net.minecraft.core.BlockPos pool = net.minecraft.core.BlockPos.containing(c.at(12, 2, 3));
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++)
            c.level().setBlock(pool.offset(dx, 0, dz), net.minecraft.world.level.block.Blocks.LAVA.defaultBlockState(), 3);
        GhostShip ghost = ModEntities.GHOST_SHIP.get().create(c.level(), EntitySpawnReason.COMMAND);
        Speedboat plain = ModEntities.SPEEDBOAT.get().create(c.level(), EntitySpawnReason.COMMAND);
        if (ghost != null && plain != null) {
            ghost.snapTo(pool.getX() + 0.5, pool.getY() + 0.5, pool.getZ() + 0.5, 0.0F, 0.0F);
            plain.snapTo(pool.getX() + 0.5, pool.getY() + 0.5, pool.getZ() + 0.5, 0.0F, 0.0F);
            float lavaTop = pool.getY() + 0.9F;
            c.check(ghost.getWaterLevelAbove() >= lavaTop, "the Spectral Galleon floats on lava (its surface at " + ghost.getWaterLevelAbove() + ")");
            net.minecraft.world.level.material.FluidState lava = net.minecraft.world.level.material.Fluids.LAVA.defaultFluidState();
            c.check(ghost.canBoatInFluid(lava) && !plain.canBoatInFluid(lava), "...a Speedboat doesn't float on lava");
            c.check(ghost.fireImmune() && ModEntities.LEVIATHAN.get().fireImmune(), "the lava boats don't burn");
            // ...nor does anyone riding one: no flames, no fire or lava damage
            c.level().addFreshEntity(ghost);
            ServerPlayer rider = c.player();
            rider.startRiding(ghost, true, false);
            rider.igniteForSeconds(15.0F);
            BoatEvents.douse(rider);
            float before = rider.getHealth();
            rider.hurtServer(c.level(), c.level().damageSources().lava(), 4.0F);
            rider.hurtServer(c.level(), c.level().damageSources().onFire(), 1.0F);
            c.check(rider.getVehicle() == ghost && rider.getRemainingFireTicks() <= 0 && rider.getHealth() == before,
                "a lava boat's rider never catches fire nor takes lava damage (health " + before + " -> " + rider.getHealth() + ")");
            rider.stopRiding();
            ghost.discard();
        }
        // the Fairy Blossom flutters: up to its height off the water, holds it two seconds, drifts back down
        FairyBoat fairy = ModEntities.FAIRY_BOAT.get().create(c.level(), EntitySpawnReason.COMMAND);
        if (fairy != null) {
            net.minecraft.core.BlockPos pond = net.minecraft.core.BlockPos.containing(c.at(3, 2, 16));
            for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) for (int dy = -2; dy <= 5; dy++)
                c.level().setBlock(pond.offset(dx, dy, dz), dy <= 0 ? net.minecraft.world.level.block.Blocks.WATER.defaultBlockState()
                    : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            fairy.snapTo(pond.getX() + 0.5, pond.getY() + 0.5, pond.getZ() + 0.5, 0.0F, 0.0F);
            c.level().addFreshEntity(fairy);
            for (int i = 0; i < 30; i++) fairy.tick();
            double water = fairy.getY();
            c.check(fairy.flutter(), "the Fairy Blossom flutters up off the water");
            c.check(!fairy.flutter(), "...not again while it's already up");
            for (int i = 0; i < 20; i++) fairy.tick();
            c.near(fairy.getY() - water, FairyBoat.FLIGHT_HEIGHT, 0.2, "...rises " + FairyBoat.FLIGHT_HEIGHT + " blocks");
            for (int i = 0; i < 25; i++) fairy.tick();
            c.near(fairy.getY() - water, FairyBoat.FLIGHT_HEIGHT, 0.2, "...and holds that height");
            c.check(fairy.wingSpread(1.0F) > 0.9F, "...its wings spread (" + fairy.wingSpread(1.0F) + ")");
            for (int i = 0; i < 90; i++) fairy.tick();
            c.check(!fairy.fluttering() && Math.abs(fairy.getY() - water) < 0.35, "...then drifts back down onto the water ("
                + (fairy.getY() - water) + ")");
            for (int i = 0; i < 40; i++) fairy.tick();
            c.check(fairy.wingSpread(1.0F) < 0.1F, "...and folds its wings (" + fairy.wingSpread(1.0F) + ")");
            fairy.discard();
        }
        // its light: invisible light blocks in the air round it, gone with it
        Longship lit = ModEntities.LONGSHIP.get().create(c.level(), EntitySpawnReason.COMMAND);
        if (lit != null) {
            net.minecraft.core.BlockPos where = net.minecraft.core.BlockPos.containing(c.at(8, 2, 8));
            lit.snapTo(where.getX() + 0.5, where.getY(), where.getZ() + 0.5, 0.0F, 0.0F);
            c.level().addFreshEntity(lit);
            lit.refreshLights();
            java.util.List<net.minecraft.core.BlockPos> lights = lit.placedLights();
            c.check(!lights.isEmpty() && c.level().getBlockState(lights.get(0)).is(net.minecraft.world.level.block.Blocks.LIGHT),
                "a Premium boat's fire lights up the air round it (" + lights + ")");
            lit.discard();
            c.check(lights.stream().noneMatch(p -> c.level().getBlockState(p).is(net.minecraft.world.level.block.Blocks.LIGHT)),
                "...and its light goes with it");
        }
        // the model builds and animates (client code; in the dev test server it's there, elsewhere it's skipped)
        try {
            Class<?> check = Class.forName("com.alexh.capitalism.client.PirateShipModelCheck");
            String result = (String) check.getMethod("run").invoke(null);
            c.check(result.startsWith("ok"), "every Premium boat's model builds and animates, no faces fighting: " + result);
            try {
                for (Object e : (java.util.List<?>) check.getField("EXAMPLES").get(null)) com.mojang.logging.LogUtils.getLogger().info("[fights] {}", e);
            } catch (ReflectiveOperationException ignored) {
            }
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            c.check(true, "the Pirate Ship's model check skipped (no client classes)");
        } catch (ReflectiveOperationException e) {
            c.fail("the Pirate Ship's model failed: " + (e.getCause() == null ? e : e.getCause()));
        }
    }

    // The Submersible's flashlight, for real use: an underwater room 19 long, 13 wide, 9 deep (stone floor and far
    // wall), the sub at one end with its lamp aimed down the room. The light it casts is measured as the game spreads
    // it (each light block's level, one less per block away; nothing in the way under water): where the cone lands it's
    // bright, beside the cone and round the boat it stays dark; aimed elsewhere the light goes with it; seagrass on the
    // floor doesn't stop it; off, every light block is water again; and it doesn't use too many light blocks.
    private static void flashlight(GameTests.Checks c) {
        net.minecraft.server.level.ServerLevel level = c.level();
        net.minecraft.core.BlockPos base = net.minecraft.core.BlockPos.containing(c.at(0, 30, 0));
        net.minecraft.world.level.block.state.BlockState water = net.minecraft.world.level.block.Blocks.WATER.defaultBlockState();
        net.minecraft.world.level.block.state.BlockState stone = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
        for (int x = -2; x <= 22; x++) for (int y = -1; y <= 12; y++) for (int z = -11; z <= 11; z++) {
            net.minecraft.world.level.block.state.BlockState s = y == -1 || x == 22 || Math.abs(z) == 11 ? stone : y <= 8 ? water : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
            level.setBlock(base.offset(x, y, z), s, 2);
        }
        Submersible sub = ModEntities.SUBMERSIBLE.get().create(level, EntitySpawnReason.COMMAND);
        if (sub == null) {
            c.fail("no Submersible");
            return;
        }
        sub.snapTo(base.getX() + 0.5, base.getY() + 8.45, base.getZ() + 0.5, -90.0F, 0.0F);   // facing east (+x), down the room
        level.addFreshEntity(sub);
        sub.toggleFlashlight();
        sub.aimLamp(0.0F, 30.0F);
        sub.refreshLights();
        java.util.List<net.minecraft.core.BlockPos> lights = sub.placedLights();
        java.util.function.ToIntFunction<net.minecraft.core.BlockPos> light = p -> {
            int best = 0;
            for (net.minecraft.core.BlockPos l : sub.placedLights()) {
                net.minecraft.world.level.block.state.BlockState s = level.getBlockState(l);
                if (!s.is(net.minecraft.world.level.block.Blocks.LIGHT)) continue;
                best = Math.max(best, s.getValue(net.minecraft.world.level.block.LightBlock.LEVEL) - p.distManhattan(l));
            }
            return best;
        };
        // where the middle of the beam lands on the floor (the lamp is about 1.7 east of the boat, 0.6 below it)
        net.minecraft.world.phys.Vec3 from = sub.lampFrom();
        net.minecraft.world.phys.Vec3 dir = sub.lampDirection(1.0F);
        double t = (from.y - (base.getY())) / -dir.y;
        net.minecraft.core.BlockPos centre = net.minecraft.core.BlockPos.containing(from.add(dir.scale(t)).add(0, 0.5, 0));
        com.mojang.logging.LogUtils.getLogger().info("[flashlight] {} lights, beam lands at {} (rel {}), lights {}", lights.size(), centre,
            centre.subtract(base), lights.stream().map(p -> p.subtract(base).toShortString()).toList());
        c.check(lights.size() >= 12 && lights.size() <= 60, "the flashlight uses a sensible number of light blocks (" + lights.size() + ")");
        c.check(light.applyAsInt(centre) >= 12, "where the beam lands on the floor it's bright (" + light.applyAsInt(centre) + ")");
        // across the cone at that distance: about 20 degrees either side lands ~tan(20)*distance to the side
        double dist = from.distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(centre));
        int halfWidth = (int) Math.floor(Math.tan(Math.toRadians(16)) * dist);
        int edgeL = light.applyAsInt(centre.offset(0, 0, halfWidth));
        int edgeR = light.applyAsInt(centre.offset(0, 0, -halfWidth));
        c.check(edgeL >= 7 && edgeR >= 7, "...and across the cone, " + halfWidth + " blocks to either side (" + edgeL + ", " + edgeR + ")");
        int mid = light.applyAsInt(centre);
        int outL = light.applyAsInt(centre.offset(-4, 0, 10));
        int outR = light.applyAsInt(centre.offset(-4, 0, -10));
        c.check(outL <= 6 && outR <= 6 && outL <= mid - 6 && outR <= mid - 6, "...but 45 degrees off to the side it's dim (" + outL + ", " + outR + ")");
        net.minecraft.core.BlockPos besideBoat = base.offset(2, 4, 5);
        c.check(light.applyAsInt(besideBoat) <= 3, "...and round the boat itself it's dark (" + light.applyAsInt(besideBoat) + ")");
        int wall = light.applyAsInt(base.offset(21, 7, 0));
        // the far wall, lit when aimed straight down the room
        sub.aimLamp(0.0F, 0.0F);
        sub.refreshLights();
        int wallAhead = light.applyAsInt(base.offset(21, 7, 0));
        c.check(wallAhead >= 10, "aimed straight ahead it lights the far wall 20 blocks off (" + wallAhead + ", was " + wall + ")");
        // aimed off to the left (port, +z here... facing east, left is north: -z)
        sub.aimLamp(-35.0F, 30.0F);
        sub.refreshLights();
        int oldSpot = light.applyAsInt(centre);
        net.minecraft.world.phys.Vec3 dl = sub.lampDirection(1.0F);
        double tl = (from.y - base.getY()) / -dl.y;
        net.minecraft.core.BlockPos left = net.minecraft.core.BlockPos.containing(from.add(dl.scale(tl)).add(0, 0.5, 0));
        c.check(left.getZ() < base.getZ() && light.applyAsInt(left) >= 11 && oldSpot <= light.applyAsInt(left) - 4,
            "looking left, the light moves with it (" + left.subtract(base).toShortString() + ": " + light.applyAsInt(left) + ", old spot now " + oldSpot + ")");
        // steady: the boat bobbing a little and the rider's head twitching don't move the light (no flickering)
        sub.aimLamp(0.0F, 30.0F);
        sub.refreshLights();
        java.util.Set<net.minecraft.core.BlockPos> settled = new java.util.HashSet<>(sub.placedLights());   // (reassigned below)
        int moved = 0;
        for (int i = 0; i < 6; i++) {
            sub.setPos(sub.getX(), base.getY() + 8.45 + (i % 2 == 0 ? 0.06 : -0.05), sub.getZ());
            sub.aimLamp((i % 3 - 1) * 1.4F, 30.0F + (i % 2 == 0 ? 1.2F : -1.1F));
            sub.refreshLights();
            java.util.Set<net.minecraft.core.BlockPos> now = new java.util.HashSet<>(sub.placedLights());
            if (!now.equals(settled)) {
                moved++;
                java.util.Set<net.minecraft.core.BlockPos> gone = new java.util.HashSet<>(settled);
                gone.removeAll(now);
                java.util.Set<net.minecraft.core.BlockPos> added = new java.util.HashSet<>(now);
                added.removeAll(settled);
                com.mojang.logging.LogUtils.getLogger().info("[flicker] {}: gone {} added {}", i, gone.stream().map(p -> p.subtract(base).toShortString()).toList(),
                    added.stream().map(p -> p.subtract(base).toShortString()).toList());
                settled = now;
            }
        }
        sub.setPos(sub.getX(), base.getY() + 8.45, sub.getZ());
        c.check(moved == 0, "bobbing and small head movements don't move its light blocks (no flicker: " + moved + " of 6 moved)");
        // seagrass all over the floor: still lights it, just above
        for (int x = 2; x <= 21; x++) for (int z = -10; z <= 10; z++)
            level.setBlock(base.offset(x, 0, z), net.minecraft.world.level.block.Blocks.SEAGRASS.defaultBlockState(), 2);
        sub.aimLamp(0.0F, 30.0F);
        sub.refreshLights();
        int grassy = light.applyAsInt(centre);
        c.check(grassy >= 10, "through seagrass the floor is still lit (" + grassy + ")");
        // off: every light block is water again
        java.util.List<net.minecraft.core.BlockPos> before = sub.placedLights();
        sub.toggleFlashlight();
        sub.refreshLights();
        long leftover = before.stream().filter(p -> level.getBlockState(p).is(net.minecraft.world.level.block.Blocks.LIGHT)
            && !sub.placedLights().contains(p)).count();
        c.check(leftover == 0 && before.stream().filter(p -> !sub.placedLights().contains(p)).allMatch(p -> level.getBlockState(p).is(net.minecraft.world.level.block.Blocks.WATER)
                || level.getBlockState(p).is(net.minecraft.world.level.block.Blocks.SEAGRASS)),   // (the floor the test covered in seagrass)
            "turned off, the light blocks are water again (" + leftover + " left; not water: " + before.stream().filter(p -> !sub.placedLights().contains(p) && !level.getBlockState(p).is(net.minecraft.world.level.block.Blocks.WATER)).map(p -> p.subtract(base).toShortString() + "=" + level.getBlockState(p).getBlock().getDescriptionId()).toList() + ")");
        sub.discard();
    }
}
