package com.alexh.capitalism.decor;

import com.alexh.capitalism.calendar.Calendar;
import com.alexh.capitalism.core.ModBlocks;
import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.event.BloodMoon;
import com.alexh.capitalism.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.UUID;

// The Blood Moon decorations' logic (Decorations.md > Blood Moon decorations):
//  - big pieces: they need their whole footprint free, fill it with part blocks, and break whole from any part, dropping
//    one item;
//  - the iron maiden: a chest that keeps its items and drops them, its door open while someone has it open, and its trap;
//  - the fireplace: wood only (a log burns four times as long as a plank), out when it's burnt; it heals whoever stands
//    in front of it long enough, not beside or behind it;
//  - the altar: offerings by day, the cost ladder (150, then 300...), back to 150 on Monday, shards back at dusk (out of
//    the altar, into the inventory, or kept for an absent player);
//  - the fountain: bottles of blood only under a Blood Moon; gold nuggets tossed in are taken;
//  - the Bottle of Blood's strength taken away after its night, a potion's left alone;
//  - the coffin by day, the bookshelf door's owner, the chandelier's light.
public final class BloodDecorTests {

    static {
        GameTests.register("blood_decorations", BloodDecorTests::bloodDecorations);
    }

    private BloodDecorTests() {
    }

    public static void init() {
    }

    private static int countItems(ServerLevel level, BlockPos near, net.minecraft.world.item.Item item, boolean discard) {
        int n = 0;
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(near).inflate(3))) {
            if (e.getItem().is(item)) n += e.getItem().getCount();
            if (discard) e.discard();
        }
        return n;
    }

    private static void clear(ServerLevel level, BlockPos from, BlockPos to) {
        for (BlockPos p : BlockPos.betweenClosed(from, to)) level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
    }

    private static void bloodDecorations(GameTests.Checks c) {
        ServerLevel level = c.level();
        ServerPlayer player = c.player();

        // ── a big piece: the organ, 2 wide, 2 tall ──
        BlockPos organ = BlockPos.containing(c.at(1, 2, 1));
        Direction north = Direction.NORTH;
        BloodDecorKind organKind = BloodDecorKind.BLOOD_PIPE_ORGAN;
        BlockPos blocker = BloodDecorKind.cellPos(organ, north, 0, 1, 1);
        level.setBlock(blocker, Blocks.STONE.defaultBlockState(), 3);
        boolean blocked = !BloodDecorBlock.roomFor(organKind, level, organ, north, null);
        level.setBlock(blocker, Blocks.AIR.defaultBlockState(), 3);
        c.check(blocked && BloodDecorBlock.roomFor(organKind, level, organ, north, null), "a big piece needs its whole footprint free");
        level.setBlock(organ, ModBlocks.BLOOD_PIPE_ORGAN.get().defaultBlockState().setValue(BloodDecorBlock.FACING, north), 3);
        BloodDecorBlock.placeParts(level, organ, north, organKind, organ);
        boolean filled = true;
        for (int[] cell : organKind.otherCells()) {
            BlockPos p = BloodDecorKind.cellPos(organ, north, cell[0], cell[1], cell[2]);
            BlockState s = level.getBlockState(p);
            filled &= s.getBlock() instanceof BloodPartBlock && BloodPartBlock.mainOf(s, p).equals(organ)
                && !s.getCollisionShape(level, p, CollisionContext.empty()).isEmpty();
        }
        c.check(filled && organKind.otherCells().size() == 3, "the organ fills its 2 x 2 with solid part blocks pointing at it");
        BlockPos topRight = BloodDecorKind.cellPos(organ, north, 0, 1, 1);
        level.destroyBlock(topRight, true);
        boolean allGone = level.getBlockState(organ).isAir();
        for (int[] cell : organKind.otherCells()) allGone &= level.getBlockState(BloodDecorKind.cellPos(organ, north, cell[0], cell[1], cell[2])).isAir();
        c.check(allGone && countItems(level, organ, ModItems.BLOOD_PIPE_ORGAN.get(), true) == 1,
            "breaking one of its part blocks breaks the whole organ, which drops one organ");

        // ── the iron maiden: a chest, its door, its trap ──
        BlockPos maiden = BlockPos.containing(c.at(4, 2, 1));
        level.setBlock(maiden, ModBlocks.IRON_MAIDEN.get().defaultBlockState().setValue(BloodDecorBlock.FACING, north), 3);
        BloodDecorBlock.placeParts(level, maiden, north, BloodDecorKind.IRON_MAIDEN, maiden);
        if (level.getBlockEntity(maiden) instanceof BloodDecorBlockEntity be) {
            be.setItem(3, new ItemStack(Items.DIAMOND, 5));
            var tag = be.saveWithFullMetadata(level.registryAccess());
            BlockEntity again = BlockEntity.loadStatic(maiden, level.getBlockState(maiden), tag, level.registryAccess());
            c.check(again instanceof BloodDecorBlockEntity m && m.getItem(3).is(Items.DIAMOND) && m.getItem(3).getCount() == 5
                && m.getContainerSize() == 27, "the iron maiden keeps its items (27 slots, saved and loaded)");
            be.startOpen(player);
            boolean opened = level.getBlockState(maiden).getValue(BloodDecorBlock.OPEN);
            be.stopOpen(player);
            c.check(opened && !level.getBlockState(maiden).getValue(BloodDecorBlock.OPEN), "its door stands open while someone has its chest open");
            // armed, a mob stepping in gets the spikes; a player never does
            level.setBlock(maiden, level.getBlockState(maiden).setValue(BloodDecorBlock.LIT, true), 3);
            c.check(level.getBlockState(maiden).getCollisionShape(level, maiden, CollisionContext.empty()).max(Direction.Axis.X)
                > 0, "armed, it's hollow (only its walls are solid)");
            player.teleportTo(maiden.getX() + 0.5, maiden.getY(), maiden.getZ() + 0.5);
            BloodDecorBlockEntity.serverTick(level, maiden, level.getBlockState(maiden), be);
            boolean sparedPlayer = !level.getBlockState(maiden).getValue(BloodDecorBlock.TRIPPED);
            player.teleportTo(maiden.getX() + 6.5, maiden.getY(), maiden.getZ() + 0.5);
            Zombie zombie = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
            if (zombie != null) {
                zombie.snapTo(maiden.getX() + 0.5, maiden.getY(), maiden.getZ() + 0.5);
                level.addFreshEntity(zombie);
                float before = zombie.getHealth();
                BloodDecorBlockEntity.serverTick(level, maiden, level.getBlockState(maiden), be);
                c.check(sparedPlayer, "a player stepping into it is spared");
                c.check(level.getBlockState(maiden).getValue(BloodDecorBlock.TRIPPED) && zombie.getUUID().equals(be.victim()), "a mob stepping into it is slammed in and held");
                c.check(zombie.getHealth() <= before - 7.5F, "...the spikes do 8 damage (less its armour; health " + before + " -> " + zombie.getHealth() + ")");
                for (int i = 0; i <= BloodDecorBlockEntity.MAIDEN_HOLD; i++) BloodDecorBlockEntity.serverTick(level, maiden, level.getBlockState(maiden), be);
                c.check(!level.getBlockState(maiden).getValue(BloodDecorBlock.TRIPPED) && !level.getBlockState(maiden).getValue(BloodDecorBlock.LIT),
                    "after 3 seconds it lets go, disarmed");
                zombie.discard();
            } else c.fail("a zombie for the iron maiden");
            level.destroyBlock(maiden, true);
            c.check(countItems(level, maiden, Items.DIAMOND, false) == 5 && countItems(level, maiden, ModItems.IRON_MAIDEN.get(), true) == 1
                && level.getBlockState(maiden.above()).isAir(), "broken, it drops what was in it, and itself, and its upper block goes");
        } else c.fail("the iron maiden has its block entity");

        // ── the fireplace: wood only, burns down, heals whoever stands before it ──
        BlockPos fire = BlockPos.containing(c.at(1, 2, 5));
        level.setBlock(fire, ModBlocks.BLOOD_CRYSTAL_FIREPLACE.get().defaultBlockState().setValue(BloodDecorBlock.FACING, Direction.EAST), 3);
        BloodDecorBlock.placeParts(level, fire, Direction.EAST, BloodDecorKind.BLOOD_CRYSTAL_FIREPLACE, fire);
        BloodDecorBlockEntity fireplace = (BloodDecorBlockEntity) level.getBlockEntity(fire);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(fire), Direction.EAST, fire, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COAL, 4));
        level.getBlockState(fire).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
        boolean noCoal = fireplace.fuel() == 0 && player.getMainHandItem().getCount() == 4;
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS, 4));
        level.getBlockState(fire).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
        int plank = fireplace.fuel();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_LOG, 4));
        level.getBlockState(fire).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
        int log = fireplace.fuel() - plank;
        c.check(noCoal && plank == BloodDecorBlock.PLANK_FUEL && log == BloodDecorBlock.LOG_FUEL && log == 4 * plank
            && level.getBlockState(fire).getValue(BloodDecorBlock.LIT) && player.getMainHandItem().getCount() == 3,
            "the fireplace takes wood, not coal (a log burns 4 times as long as a plank) and is lit by it");
        c.check(level.getBlockState(fire).getLightEmission(level, fire) == 13, "lit, it gives light 13");
        Vec3 hearth = BloodDecorBlockEntity.centreOf(level, fire).add(0, 0.5, 0);
        player.teleportTo(hearth.x + 2.0, fire.getY(), hearth.z);
        ServerPlayer beside = c.player();
        beside.teleportTo(hearth.x - 0.2, fire.getY(), hearth.z + 2.5);
        for (int i = 0; i < 4; i++) fireplace.warmTick(level, fire, level.getBlockState(fire));
        boolean notYet = !player.hasEffect(MobEffects.REGENERATION);
        fireplace.warmTick(level, fire, level.getBlockState(fire));
        c.check(notYet && player.hasEffect(MobEffects.REGENERATION) && !beside.hasEffect(MobEffects.REGENERATION),
            "5 seconds in front of the fire heals you (Regeneration); standing beside it doesn't");
        player.removeEffect(MobEffects.REGENERATION);
        fireplace.addFuel(-fireplace.fuel() + 1);
        BloodDecorBlockEntity.serverTick(level, fire, level.getBlockState(fire), fireplace);
        c.check(!level.getBlockState(fire).getValue(BloodDecorBlock.LIT) && level.getBlockState(fire).getLightEmission(level, fire) == 0,
            "its wood burnt, it goes out");
        level.destroyBlock(fire, false);

        // ── the altar: offerings by day, the cost ladder, shards back at dusk ──
        BlockPos altar = BlockPos.containing(c.at(5, 2, 5));
        level.setBlock(altar, ModBlocks.BLOOD_MOON_ALTAR.get().defaultBlockState().setValue(BloodDecorBlock.FACING, north), 3);
        BloodAltar.Data data = BloodAltar.data(level.getServer());
        data.uses = 0;
        data.pool = 0;
        data.given.clear();
        data.where.clear();
        BloodMoon.unschedule(level.getServer());
        player.teleportTo(altar.getX() + 0.5, altar.getY(), altar.getZ() + 2.5);
        ItemStack shards = new ItemStack(ModItems.BLOOD_SHARD.get(), 100);
        int took = BloodAltar.offer(level, altar, player, shards);
        boolean partial = took == 100 && data.pool() == 100 && data.cost() == 150 && BloodMoon.ticksUntil(level.getServer()) < 0;
        ItemStack more = new ItemStack(ModItems.BLOOD_SHARD.get(), 64);
        took = BloodAltar.offer(level, altar, player, more);
        c.check(partial && took == 50 && more.getCount() == 14 && BloodMoon.ticksUntil(level.getServer()) >= 0 && data.pool() == 0,
            "the first Blood Moon of the week takes 150 shards (only what's still needed); then one rises tonight");
        c.check(data.cost() == 300 && BloodAltar.offer(level, altar, player, more) == 0, "the next costs 300; nothing more is taken while one is coming");
        BloodMoon.unschedule(level.getServer());
        Calendar.setClock(level, 7L * Calendar.DAY_TICKS + 6000L);   // a Monday morning
        BloodAltar.settle(level.getServer());
        c.check(data.cost() == 150, "on Monday morning it's back to 150");
        Calendar.setClock(level, 8L * Calendar.DAY_TICKS + 6000L);   // Tuesday noon
        // short by dusk: back out of the altar for a giver near it, into the inventory for one far off, kept for an absent one
        player.getInventory().clearContent();
        BloodAltar.offer(level, altar, player, new ItemStack(ModItems.BLOOD_SHARD.get(), 20));
        UUID absent = UUID.randomUUID();
        data.given.put(absent, 7);
        data.pool += 7;
        Calendar.setClock(level, 8L * Calendar.DAY_TICKS + 13100L);   // dusk
        BloodAltar.settle(level.getServer());
        c.check(countItems(level, altar, ModItems.BLOOD_SHARD.get(), true) == 20 && data.owedTo(absent) == 7 && data.pool() == 0,
            "short by dusk: the giver's shards come back out of the altar, an absent giver's are kept for them");
        Calendar.setClock(level, 9L * Calendar.DAY_TICKS + 6000L);
        BloodAltar.offer(level, altar, player, new ItemStack(ModItems.BLOOD_SHARD.get(), 12));
        player.teleportTo(altar.getX() + 60.5, altar.getY(), altar.getZ() + 0.5);
        Calendar.setClock(level, 9L * Calendar.DAY_TICKS + 13100L);
        BloodAltar.settle(level.getServer());
        c.check(player.getInventory().countItem(ModItems.BLOOD_SHARD.get()) == 12, "...a giver far off gets them in their inventory");
        Calendar.setClock(level, 9L * Calendar.DAY_TICKS + 14000L);
        c.check(BloodAltar.offer(level, altar, player, new ItemStack(ModItems.BLOOD_SHARD.get(), 5)) == 0, "no offerings by night");
        data.owed.remove(absent);
        data.uses = 0;
        data.setDirty();
        Calendar.setClock(level, Calendar.DAY_TICKS + 6000L);
        level.setBlock(altar, Blocks.AIR.defaultBlockState(), 3);

        // ── the fountain: bottles only under a Blood Moon; gold nuggets tossed in are taken ──
        BlockPos fountain = BlockPos.containing(c.at(1, 2, 8));
        level.setBlock(fountain, ModBlocks.BLOOD_FOUNTAIN.get().defaultBlockState().setValue(BloodDecorBlock.FACING, north), 3);
        BloodDecorBlock.placeParts(level, fountain, north, BloodDecorKind.BLOOD_FOUNTAIN, fountain);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE));
        boolean filled2 = BloodDecorBlock.fillBottle(level, fountain, player, InteractionHand.MAIN_HAND, player.getMainHandItem());
        c.check(!BloodMoon.isActive() && !filled2 && player.getMainHandItem().is(Items.GLASS_BOTTLE), "no Blood Moon: the fountain gives no blood");
        Vec3 basin = BloodDecorBlockEntity.centreOf(level, fountain);
        ItemEntity coins = new ItemEntity(level, basin.x, fountain.getY() + 0.5, basin.z, new ItemStack(Items.GOLD_NUGGET, 10));
        level.addFreshEntity(coins);
        BloodDecorBlockEntity.takeCoins(level, fountain);
        c.check(coins.isRemoved() && countItems(level, fountain, Items.GOLD_NUGGET, false) == 0, "gold nuggets tossed into the basin are taken");
        countItems(level, fountain, ModItems.BLOOD_SHARD.get(), true);
        level.destroyBlock(fountain, false);
        c.check(level.getBlockState(BloodDecorKind.cellPos(fountain, north, 1, 0, 1)).isAir(), "the fountain's 2 x 2 goes with it");

        // ── a Bottle of Blood's strength is taken after its night; a potion's isn't ──
        BottleOfBloodItem.grant(player, 2000);
        boolean strong = player.hasEffect(MobEffects.STRENGTH) && player.hasEffect(MobEffects.SPEED);
        BottleOfBloodItem.stripBlood(player);
        boolean stripped = !player.hasEffect(MobEffects.STRENGTH) && !player.hasEffect(MobEffects.SPEED);
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 3000, 1));
        player.getPersistentData().putBoolean(BottleOfBloodItem.DRUNK, true);
        BottleOfBloodItem.stripBlood(player);
        c.check(strong && stripped && player.hasEffect(MobEffects.STRENGTH), "the blood's Strength and Speed are taken away after its night; a potion's are left");
        player.removeEffect(MobEffects.STRENGTH);

        // ── the coffin by day, the canopy bed not ──
        BlockPos foot = BlockPos.containing(c.at(5, 2, 8));
        BlockState coffin = ModBlocks.VAMPIRE_COFFIN.get().defaultBlockState().setValue(BloodBedBlock.FACING, Direction.EAST);
        level.setBlock(foot, coffin, 3);
        level.setBlock(foot.east(), coffin.setValue(BloodBedBlock.PART, BedPart.HEAD), 3);
        BloodBedBlock bed = ModBlocks.VAMPIRE_COFFIN.get();
        boolean shut = bed.getSleepHeight(level.getBlockState(foot.east()), level, foot.east()).isEmpty();
        BloodBedBlock.setOpen(level, foot.east(), level.getBlockState(foot.east()), true);
        c.check(shut && bed.getSleepHeight(level.getBlockState(foot.east()), level, foot.east()).isPresent()
            && bed.getBedRule(level, foot.east()).canSleep(level) && !ModBlocks.CRIMSON_CANOPY_BED.get().getBedRule(level, foot.east()).canSleep(level),
            "the coffin can be slept in only open, and by day too (the canopy bed only by night)");
        level.setBlock(foot.east(), Blocks.AIR.defaultBlockState(), 3);
        c.check(level.getBlockState(foot).isAir(), "its two halves go together");

        // ── the bookshelf door: its owner's alone ──
        BlockPos lower = BlockPos.containing(c.at(8, 2, 1));
        BlockState door = ModBlocks.SECRET_BOOKSHELF_DOOR.get().defaultBlockState().setValue(BloodDoorBlock.FACING, north);
        level.setBlock(lower, door, 3);
        level.setBlock(lower.above(), door.setValue(BloodDoorBlock.HALF, DoubleBlockHalf.UPPER), 3);
        ((BloodDecorBlockEntity) level.getBlockEntity(lower)).setOwner(player.getUUID());
        ServerPlayer stranger = beside;
        c.check(BloodDoorBlock.mayOpen(level, lower, player) && !BloodDoorBlock.mayOpen(level, lower, stranger), "the bookshelf door opens for its owner, not for others");
        boolean solid = !level.getBlockState(lower).getCollisionShape(level, lower, CollisionContext.empty()).isEmpty();
        BloodDoorBlock.swing(level, lower, level.getBlockState(lower), true);
        boolean through = level.getBlockState(lower).getCollisionShape(level, lower, CollisionContext.empty()).isEmpty()
            && level.getBlockState(lower.above()).getValue(BloodDoorBlock.OPEN);
        BloodDoorBlock.swing(level, lower, level.getBlockState(lower), false);
        c.check(solid && through, "it's solid shut and walk-through open (both halves)");
        clear(level, lower, lower.above());

        // ── the chandelier: light only while lit; it needs its ceiling ──
        BlockPos ceiling = BlockPos.containing(c.at(8, 5, 5));
        level.setBlock(ceiling, Blocks.STONE.defaultBlockState(), 3);
        BlockPos chandelier = ceiling.below();
        level.setBlock(chandelier, ModBlocks.CRIMSON_CHANDELIER.get().defaultBlockState(), 3);
        boolean litAtFirst = level.getBlockState(chandelier).getLightEmission(level, chandelier) == 15;
        BloodDecorBlock.setLit(level, chandelier, level.getBlockState(chandelier), false);
        c.check(litAtFirst && level.getBlockState(chandelier).getLightEmission(level, chandelier) == 0, "the chandelier is lit when placed and dark when snuffed");
        level.setBlock(ceiling, Blocks.AIR.defaultBlockState(), 3);
        c.check(level.getBlockState(chandelier).isAir(), "with its ceiling gone, the chandelier comes down");
    }
}
