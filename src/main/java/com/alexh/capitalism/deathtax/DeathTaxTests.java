package com.alexh.capitalism.deathtax;

import com.alexh.capitalism.bounty.Standings;
import com.alexh.capitalism.calendar.Calendar;
import com.alexh.capitalism.core.ModAttachments;
import com.alexh.capitalism.economy.GoldMoney;
import com.alexh.capitalism.event.BloodMoon;
import com.alexh.capitalism.event.Purge;
import com.alexh.capitalism.gametest.GameTests;
import com.alexh.capitalism.town.Town;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

// The death tax, Purgatory, /pay death, Not-A-Scam Insurance and The Purge (Insurance.md, Events.md > The Purge).
public final class DeathTaxTests {

    static {
        register("death_tax", DeathTaxTests::deathTax);
        register("purgatory", DeathTaxTests::purgatory);
        register("pay_death", DeathTaxTests::payDeath);
        register("insurance", DeathTaxTests::insurance);
        register("blood_moon_insurance", DeathTaxTests::bloodMoonInsurance);
        register("purge", DeathTaxTests::purge);
        register("death_tax_collect", DeathTaxTests::collect);
        register("double_chest_gold", DeathTaxTests::doubleChest);
    }

    // Respawned test players are plain players with a pretend connection, which NeoForge's config sync won't accept
    // on the next tick: they log out when the test is done (before that tick).
    private static final List<ServerPlayer> RESPAWNED = new java.util.ArrayList<>();

    private static void register(String name, java.util.function.Consumer<GameTests.Checks> body) {
        GameTests.register(name, c -> {
            try {
                body.accept(c);
            } finally {
                for (ServerPlayer p : RESPAWNED) c.level().getServer().getPlayerList().remove(p);
                RESPAWNED.clear();
            }
        });
    }

    private DeathTaxTests() {
    }

    public static void init() {
    }

    // ── helpers ──

    // exactly this much gold on them (nuggets), nothing in the Ender Chest unless asked
    private static void setGold(ServerPlayer p, long carried, long ender) {
        p.getInventory().clearContent();
        p.getEnderChestInventory().clearContent();
        GoldMoney.give(p, carried);
        int slot = 0;
        for (ItemStack s : GoldMoney.toStacks(ender)) p.getEnderChestInventory().setItem(slot++, s);
    }

    private static void kill(ServerPlayer p, DamageSource source) {
        p.hurtServer(p.level(), source, 100000.0F);
    }

    private static ServerPlayer respawn(ServerPlayer p) {
        ServerPlayer back = p.level().getServer().getPlayerList().respawn(p, false, Entity.RemovalReason.KILLED);
        // (what the client's respawn button would clear: the server holds a dead player's connection "waiting for
        // respawn", invulnerable, until then)
        try {
            java.lang.reflect.Field waiting = net.minecraft.server.network.ServerGamePacketListenerImpl.class.getDeclaredField("waitingForRespawn");
            waiting.setAccessible(true);
            waiting.setBoolean(back.connection, false);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("can't respawn a test player", e);
        }
        // (like a fresh one: the server keeps them invulnerable until their client says it has loaded, and while it
        // thinks they're still arriving from the respawn)
        back.connection.markClientLoaded();
        back.hasChangedDimension();
        RESPAWNED.add(back);
        return back;
    }

    private static void owe(ServerPlayer p, java.util.@org.jspecify.annotations.Nullable UUID creditor, String name, long nuggets) {
        DeathTaxData d = DeathTax.data(p);
        d.add(creditor, name, nuggets);
        DeathTax.save(p, d);
    }

    private static void clearDebt(ServerPlayer p) {
        DeathTaxData d = DeathTax.data(p);
        d.debts.clear();
        DeathTax.save(p, d);
    }

    private static int run(ServerPlayer p, String command) {
        p.level().getServer().getCommands().performPrefixedCommand(p.createCommandSourceStack(), command);
        return 0;
    }

    private static void clearDrops(ServerLevel level, ServerPlayer p) {
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(8))) e.discard();
    }

    private static long dropsGold(ServerLevel level, Vec3 at) {
        long total = 0;
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(at, at).inflate(8))) total += GoldMoney.valueOf(e.getItem());
        return total;
    }

    private static void day(ServerLevel level, long day) {
        Calendar.setClock(level, day * Calendar.DAY_TICKS + 1000);
    }

    // ── the tax: 25% of carried + Ender Chest, rounded down; kept; stacking; nothing when broke ──

    private static void deathTax(GameTests.Checks c) {
        ServerLevel level = c.level();
        c.check(DeathTax.tax(1, 25) == 0 && DeathTax.tax(4, 25) == 1 && DeathTax.tax(7, 25) == 1 && DeathTax.tax(441, 25) == 110
            && DeathTax.tax(100, 25) == 25 && DeathTax.tax(0, 25) == 0 && DeathTax.tax(500, 0) == 0, "25% rounds down to whole nuggets (441 -> 110, 7 -> 1, 1 -> 0)");

        ServerPlayer p = c.player();
        setGold(p, GoldMoney.fromGold(40), GoldMoney.fromGold(9));
        // gold in a chest doesn't count
        BlockPos chest = BlockPos.containing(c.at(4, 2, 4));
        level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 3);
        if (level.getBlockEntity(chest) instanceof net.minecraft.world.Container box) box.setItem(0, new ItemStack(Items.GOLD_BLOCK, 10));
        c.check(DeathTax.taxableGold(p) == 441, "the taxed gold: carried + Ender Chest (a chest that isn't theirs doesn't count): " + DeathTax.taxableGold(p));
        Vec3 at = p.position();
        kill(p, level.damageSources().fellOutOfWorld());
        DeathTaxData d = DeathTax.data(p);
        c.check(p.isDeadOrDying() && d.owed() == 110 && d.lastRate() == 25 && d.lastGold() == 441, "dying with 49 gold owes 110 nuggets (25%): " + d.owed());
        boolean keep = level.getGameRules().get(GameRules.KEEP_INVENTORY);
        c.check(keep || dropsGold(level, at) == GoldMoney.fromGold(40), "the carried gold drops as normal, all of it (the tax takes nothing): " + dropsGold(level, at));
        c.check(GoldMoney.count(p.getEnderChestInventory()) == GoldMoney.fromGold(9), "the Ender Chest keeps its gold");
        clearDrops(level, p);

        ServerPlayer back = respawn(p);
        c.check(DeathTax.owed(back) == 110 && back.getData(ModAttachments.IN_PURGATORY.get()), "the debt and Purgatory come back with them after respawning");
        c.check(back.getDisplayName().getString().startsWith("☠"), "a skull by their name (" + back.getDisplayName().getString() + ")");

        // dying again in debt adds 25% of what they have then
        setGold(back, GoldMoney.fromGold(10), GoldMoney.fromGold(9));
        kill(back, level.damageSources().fellOutOfWorld());
        c.check(DeathTax.owed(back) == 110 + 42, "dying again adds 25% of their gold then (171 -> 42): " + DeathTax.owed(back));
        clearDrops(level, back);
        ServerPlayer again = respawn(back);
        c.check(DeathTax.owed(again) == 152, "...and it's all still owed after the second respawn");

        // broke: nothing at all
        ServerPlayer broke = c.player();
        setGold(broke, 0, 0);
        kill(broke, level.damageSources().fellOutOfWorld());
        c.check(DeathTax.owed(broke) == 0 && !broke.getData(ModAttachments.IN_PURGATORY.get()), "no gold, no tax, no Purgatory");
        ServerPlayer brokeBack = respawn(broke);
        c.check(!DeathTax.restricted(brokeBack), "...and they come back free");

        // keepInventory: their gold stays with them, still taxed once
        level.getGameRules().set(GameRules.KEEP_INVENTORY, true, level.getServer());
        ServerPlayer keeper = c.player();
        setGold(keeper, GoldMoney.fromGold(18), 0);
        kill(keeper, level.damageSources().fellOutOfWorld());
        ServerPlayer keeperBack = respawn(keeper);
        level.getGameRules().set(GameRules.KEEP_INVENTORY, keep, level.getServer());
        c.check(GoldMoney.carried(keeperBack) == GoldMoney.fromGold(18) && DeathTax.owed(keeperBack) == 40,
            "with keepInventory they keep their 18 gold and owe 40 nuggets: " + GoldMoney.carried(keeperBack) + ", " + DeathTax.owed(keeperBack));
        level.setBlock(chest, Blocks.AIR.defaultBlockState(), 3);
        clearDebt(again);
        clearDebt(keeperBack);
    }

    // ── Purgatory: no mining or hurting; chests, eating, being hurt still work ──

    private static void purgatory(GameTests.Checks c) {
        ServerLevel level = c.level();
        ServerPlayer p = c.player();
        owe(p, null, "Death", 50);
        c.check(DeathTax.restricted(p) && p.getData(ModAttachments.IN_PURGATORY.get()), "owing anything puts them In Purgatory");

        BlockPos stone = BlockPos.containing(c.at(3, 2, 2));
        level.setBlock(stone, Blocks.STONE.defaultBlockState(), 3);
        c.check(!p.gameMode.destroyBlock(stone) && level.getBlockState(stone).is(Blocks.STONE), "they can't break a block");

        Zombie zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
        if (zombie == null) {
            c.fail("no zombie");
            return;
        }
        Vec3 zat = c.at(2.5, 2, 4.5);
        zombie.snapTo(zat.x, zat.y, zat.z, 0, 0);
        zombie.setNoAi(true);
        level.addFreshEntity(zombie);
        float hp = zombie.getHealth();
        p.attack(zombie);
        boolean melee = zombie.hurtServer(level, level.damageSources().playerAttack(p), 5.0F);
        boolean thrown = zombie.hurtServer(level, level.damageSources().thrown(p, p), 5.0F);
        boolean potion = zombie.hurtServer(level, level.damageSources().indirectMagic(p, p), 5.0F);
        c.check(!melee && !thrown && !potion && zombie.getHealth() == hp, "they can't hurt anything: fists, thrown things, potions (" + zombie.getHealth() + " / " + hp + ")");
        var wolf = EntityTypes.WOLF.create(level, EntitySpawnReason.COMMAND);
        if (wolf != null) {
            wolf.snapTo(zat.x + 1, zat.y, zat.z, 0, 0);
            level.addFreshEntity(wolf);
            wolf.tame(p);
            c.check(!zombie.hurtServer(level, level.damageSources().mobAttack(wolf), 4.0F) && zombie.getHealth() == hp, "...nor their pet");
            wolf.discard();
        }
        float php = p.getHealth();
        p.hurtServer(level, level.damageSources().mobAttack(zombie), 2.0F);
        c.check(p.getHealth() < php, "monsters can still hurt them");

        // chests open, food is eaten
        BlockPos chest = BlockPos.containing(c.at(1, 2, 3));
        level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 3);
        p.gameMode.useItemOn(p, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(chest), Direction.UP, chest, false));
        c.check(p.containerMenu instanceof ChestMenu, "they can open a chest");
        p.closeContainer();
        p.getFoodData().setFoodLevel(8);
        c.consume(p, new ItemStack(Items.BREAD));
        c.check(p.getFoodData().getFoodLevel() > 8, "they can eat");

        // paid: free
        setGold(p, GoldMoney.fromGold(10), 0);
        run(p, "pay death");
        c.check(!DeathTax.restricted(p) && p.gameMode.destroyBlock(stone), "paid up, they can mine again");
        c.check(zombie.hurtServer(level, level.damageSources().playerAttack(p), 1.0F), "...and fight again");
        zombie.discard();
        level.setBlock(chest, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(stone, Blocks.AIR.defaultBlockState(), 3);
    }

    // ── /pay death: all, part, too poor, sharing between creditors, a player named Death ──

    private static void payDeath(GameTests.Checks c) {
        ServerPlayer p = c.player();
        owe(p, null, "Death", 110);
        setGold(p, GoldMoney.fromGold(5), 0);
        run(p, "pay death");
        c.check(DeathTax.owed(p) == 65 && GoldMoney.carried(p) == 0, "short of it all: Death takes what it can find (45 of 110)");
        run(p, "pay death");
        c.check(DeathTax.owed(p) == 65, "nothing left to find: nothing paid");
        GoldMoney.give(p, GoldMoney.fromGold(3));
        run(p, "pay death 2");
        c.check(DeathTax.owed(p) == 47 && GoldMoney.carried(p) == 9, "/pay death 2 pays 2 gold of it: " + DeathTax.owed(p) + " left");
        GoldMoney.give(p, GoldMoney.fromGold(20));
        run(p, "pay death");
        c.check(DeathTax.owed(p) == 0 && GoldMoney.carried(p) == 9 + 180 - 47 && !p.getData(ModAttachments.IN_PURGATORY.get()),
            "/pay death pays the rest exactly (change given) and frees them: " + GoldMoney.carried(p));
        run(p, "pay death");
        c.check(GoldMoney.carried(p) == 142, "owing nothing, /pay death takes nothing");

        // a player named Death: "/pay Death" pays them, "/pay death" pays the tax
        ServerPlayer death = c.player("Death", UUID.randomUUID());
        setGold(death, 0, 0);
        ServerPlayer q = c.player();
        owe(q, null, "Death", 20);
        setGold(q, GoldMoney.fromGold(10), 0);
        run(q, "pay Death 3");
        c.check(GoldMoney.carried(death) == 27 && DeathTax.owed(q) == 20, "/pay Death 3 pays the player named Death; the tax is untouched");
        run(q, "pay death");
        c.check(DeathTax.owed(q) == 0 && GoldMoney.carried(death) == 27 && GoldMoney.carried(q) == 90 - 27 - 20, "/pay death still pays the tax, not the player");

        // two creditors: a part payment is shared by size, all of it goes where it's owed
        long[] shares = DeathTax.shares(List.of(new DeathTaxData.Debt(null, "Death", 10), new DeathTaxData.Debt(null, "x", 10), new DeathTaxData.Debt(null, "y", 10)), 10);
        c.check(shares[0] == 4 && shares[1] == 3 && shares[2] == 3, "shares add up exactly (10 over 10/10/10 = 4/3/3)");
        ServerPlayer killer = c.player("Reaper", UUID.randomUUID());
        setGold(killer, 0, 0);
        ServerPlayer r = c.player();
        owe(r, null, "Death", 30);
        owe(r, killer.getUUID(), "Reaper", 60);
        setGold(r, GoldMoney.fromGold(3), 0);
        run(r, "pay death 3");
        DeathTaxData d = DeathTax.data(r);
        c.check(GoldMoney.carried(killer) == 18 && d.owed() == 63 && d.debts().get(0).nuggets() == 21 && d.debts().get(1).nuggets() == 42,
            "3 gold over Death 30 / Reaper 60: 9 to the bank, 18 to Reaper");
        GoldMoney.give(r, GoldMoney.fromGold(7));
        run(r, "pay death");
        c.check(GoldMoney.carried(killer) == 60 && DeathTax.owed(r) == 0, "the rest: Reaper has all 60 they were owed, no more");
    }

    // ── insurance: next-day start, premiums, switching, cancelling, lapsing ──

    private static void insurance(GameTests.Checks c) {
        ServerLevel level = c.level();
        long d0 = Calendar.date(level).index();
        ServerPlayer p = c.player();
        setGold(p, GoldMoney.fromGold(50), 0);
        Insurance.process(p);
        Insurance.buy(p, "Silver");
        c.check(Insurance.rateFor(p).percent() == 25 && GoldMoney.carried(p) == GoldMoney.fromGold(50) && DeathTax.data(p).pending().equals("silver"),
            "bought today: not covered until tomorrow, nothing charged");
        day(level, d0 + 1);
        Insurance.process(p);
        c.check(Insurance.rateFor(p).percent() == 15 && GoldMoney.carried(p) == GoldMoney.fromGold(47), "the next morning: Silver, 3 gold charged, 15%");
        Insurance.process(p);
        c.check(GoldMoney.carried(p) == GoldMoney.fromGold(47), "charged once a day");
        // a death with Silver
        setGold(p, GoldMoney.fromGold(40), 0);
        kill(p, level.damageSources().fellOutOfWorld());
        c.check(DeathTax.owed(p) == 54 && DeathTax.data(p).lastRate() == 15, "dying with Silver: 15% of 40 gold = 54 nuggets: " + DeathTax.owed(p));
        clearDrops(level, p);
        ServerPlayer s = respawn(p);
        clearDebt(s);
        c.check(DeathTax.data(s).tier().equals("silver") && Insurance.rateFor(s).percent() == 15, "the plan survives death");

        // switching: tomorrow, the old plan covers today
        setGold(s, GoldMoney.fromGold(50), 0);
        Insurance.buy(s, "gold");
        c.check(Insurance.rateFor(s).percent() == 15, "switching to Gold: still Silver today");
        day(level, d0 + 2);
        Insurance.process(s);
        c.check(Insurance.rateFor(s).percent() == 10 && GoldMoney.carried(s) == GoldMoney.fromGold(43), "the next morning: Gold, 7 gold charged, 10%");

        // cancelling: covered until tomorrow
        Insurance.cancel(s);
        c.check(Insurance.rateFor(s).percent() == 10 && DeathTax.data(s).pending().equals("cancel"), "cancelled: still covered today");
        day(level, d0 + 3);
        Insurance.process(s);
        c.check(Insurance.rateFor(s).percent() == 25 && GoldMoney.carried(s) == GoldMoney.fromGold(43) && DeathTax.data(s).tier().isEmpty(),
            "the next morning: no plan, nothing charged");

        // lapsing: can't pay, no cover, no debt
        Insurance.buy(s, "bronze");
        setGold(s, 5, 0);
        day(level, d0 + 4);
        Insurance.process(s);
        c.check(Insurance.rateFor(s).percent() == 25 && DeathTax.data(s).tier().isEmpty() && DeathTax.owed(s) == 0 && GoldMoney.carried(s) == 5,
            "can't pay the premium: the plan lapses, no cover, no debt, nothing taken");

        // a plan that hasn't started can be cancelled for nothing
        setGold(s, GoldMoney.fromGold(30), 0);
        Insurance.buy(s, "premium");
        Insurance.cancel(s);
        day(level, d0 + 5);
        Insurance.process(s);
        c.check(DeathTax.data(s).tier().isEmpty() && GoldMoney.carried(s) == GoldMoney.fromGold(30), "cancelling before it starts: it never starts, nothing charged");
        c.check(Insurance.buy(s, "platinum") == 0, "no such plan: refused");
        // the quotes come from the config
        c.check(Insurance.Tier.BRONZE.price() == 1 && Insurance.Tier.BRONZE.rate() == 20 && Insurance.Tier.SILVER.price() == 3 && Insurance.Tier.SILVER.rate() == 15
            && Insurance.Tier.GOLD.price() == 7 && Insurance.Tier.GOLD.rate() == 10 && Insurance.Tier.PREMIUM.price() == 20 && Insurance.Tier.PREMIUM.rate() == 5
            && Insurance.bloodMoonPrice() == 30 && Insurance.baseRate() == 25, "the default quotes: 1/20%, 3/15%, 7/10%, 20/5%, Blood Moon 30, uninsured 25%");
    }

    // ── Blood Moons: plans don't cover; Blood Moon Insurance covers the next one to start ──

    private static void bloodMoonInsurance(GameTests.Checks c) {
        ServerLevel level = c.level();
        MinecraftServer server = level.getServer();
        BloodMoon.testSetActive(server, false);
        long d0 = Calendar.date(level).index();
        ServerPlayer p = c.player();
        setGold(p, GoldMoney.fromGold(100), 0);
        Insurance.buy(p, "premium");
        day(level, d0 + 1);
        Insurance.process(p);
        c.check(Insurance.rateFor(p).percent() == 5, "Premium: 5%");
        BloodMoon.testSetActive(server, true);
        c.check(Insurance.rateFor(p).percent() == 25, "a Blood Moon: nobody is insured, Premium or not: 25%");
        // bought during a Blood Moon: it's for the next one
        Insurance.buy(p, "bloodmooninsurance");
        long carried = GoldMoney.carried(p);
        c.check(carried == GoldMoney.fromGold(100 - 20 - 30) && Insurance.rateFor(p).percent() == 25, "bought during a Blood Moon (30 gold): this one still costs 25%");
        Insurance.buy(p, "BloodMoonInsurance");
        c.check(GoldMoney.carried(p) == carried, "one at a time: a second is refused, nothing taken");
        BloodMoon.testSetActive(server, false);
        c.check(Insurance.rateFor(p).percent() == 5, "after it: Premium again");
        BloodMoon.testSetActive(server, true);
        c.check(Insurance.rateFor(p).percent() == 0, "the next Blood Moon: covered, 0%");
        setGold(p, GoldMoney.fromGold(60), 0);
        kill(p, level.damageSources().fellOutOfWorld());
        c.check(DeathTax.owed(p) == 0, "dying in it costs nothing");
        clearDrops(level, p);
        ServerPlayer back = respawn(p);
        c.check(Insurance.rateFor(back).percent() == 0, "...every death that night");
        BloodMoon.testSetActive(server, false);
        BloodMoon.testSetActive(server, true);
        c.check(Insurance.rateFor(back).percent() == 25, "used up when that Blood Moon ended: the one after costs 25%");
        BloodMoon.testSetActive(server, false);
        // bought before one: covers it
        setGold(back, GoldMoney.fromGold(40), 0);
        Insurance.buy(back, "BloodMoonInsurance");
        c.check(GoldMoney.carried(back) == GoldMoney.fromGold(10), "a new one can be bought once the old is used");
        BloodMoon.testSetActive(server, true);
        c.check(Insurance.rateFor(back).percent() == 0, "bought before a Blood Moon: covers it");
        BloodMoon.testSetActive(server, false);
        clearDebt(back);
    }

    // ── all your gold: the base counts owned chests; collecting goes inventory -> chests -> ground -> raw gold ──

    // a chest here owned by this player (as placing it would: the owner on the block, the ledger told), with these items
    private static BlockPos ownedChest(GameTests.Checks c, double x, double z, UUID owner, ItemStack... items) {
        ServerLevel level = c.level();
        BlockPos pos = BlockPos.containing(c.at(x, 2, z));
        level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 3);
        return own(level, pos, owner, items);
    }

    private static BlockPos own(ServerLevel level, BlockPos pos, UUID owner, ItemStack... items) {
        var be = level.getBlockEntity(pos);
        if (be instanceof net.minecraft.world.Container box) {
            box.clearContent();
            for (int i = 0; i < items.length; i++) box.setItem(i, items[i]);
            be.setData(ModAttachments.CONTAINER_OWNER.get(), new com.alexh.capitalism.economy.ContainerOwner(owner, "mod-test", true));
            com.alexh.capitalism.economy.GoldLedger.get(level.getServer()).setContainer(net.minecraft.core.GlobalPos.of(level.dimension(), pos), owner,
                GoldMoney.deepCount(box));
        }
        return pos;
    }

    // a double chest counts each half once: 32 ingots in each half is 64 gold, never 128 (an old bug doubled it)
    private static void doubleChest(GameTests.Checks c) {
        ServerLevel level = c.level();
        var ledger = com.alexh.capitalism.economy.GoldLedger.get(level.getServer());
        ServerPlayer p = c.player("Doubled", UUID.randomUUID());
        setGold(p, 0, 0);
        BlockPos left = BlockPos.containing(c.at(6, 2, 6));
        BlockPos right = left.east();
        var base = Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.FACING, net.minecraft.core.Direction.NORTH);
        level.setBlock(left, base.setValue(net.minecraft.world.level.block.ChestBlock.TYPE, net.minecraft.world.level.block.state.properties.ChestType.LEFT), 2);
        level.setBlock(right, base.setValue(net.minecraft.world.level.block.ChestBlock.TYPE, net.minecraft.world.level.block.state.properties.ChestType.RIGHT), 2);
        own(level, left, p.getUUID(), new ItemStack(Items.GOLD_INGOT, 32));
        own(level, right, p.getUUID(), new ItemStack(Items.GOLD_INGOT, 32));
        net.minecraft.world.Container both = net.minecraft.world.level.block.ChestBlock.getContainer(
            (net.minecraft.world.level.block.ChestBlock) Blocks.CHEST, level.getBlockState(left), level, left, true);
        c.check(both != null && both.getContainerSize() == 54 && GoldMoney.count(both) == GoldMoney.fromGold(64),
            "it really is one double chest holding 64 gold: " + (both == null ? "none" : both.getContainerSize() + " slots"));
        com.alexh.capitalism.economy.GoldLeaderboard.recount(level.getServer(), ledger);
        c.check(ledger.netWorth(p.getUUID()) == GoldMoney.fromGold(64), "net worth counts it once: 64 gold, not 128: " + GoldMoney.format(ledger.netWorth(p.getUUID())));
        c.check(DeathTax.taxableGold(p) == GoldMoney.fromGold(64), "the death tax base is 64 gold too");
        // and counting again never adds it twice
        com.alexh.capitalism.economy.GoldLeaderboard.recount(level.getServer(), ledger);
        c.check(ledger.netWorth(p.getUUID()) == GoldMoney.fromGold(64), "a second count still says 64 gold");
        for (BlockPos pos : List.of(left, right)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            ledger.removeContainer(net.minecraft.core.GlobalPos.of(level.dimension(), pos));
        }
    }

    private static long goldIn(ServerLevel level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof net.minecraft.world.Container box ? GoldMoney.count(box) : -1;
    }

    private static ItemEntity onGround(GameTests.Checks c, double x, double z, ItemStack stack, @org.jspecify.annotations.Nullable UUID owner) {
        Vec3 at = c.at(x, 2, z);
        ItemEntity item = new ItemEntity(c.level(), at.x, at.y, at.z, stack);
        item.setDeltaMovement(Vec3.ZERO);
        c.level().addFreshEntity(item);
        if (owner != null) GoldCollector.lock(item, owner, "mod-test");
        return item;
    }

    private static void collect(GameTests.Checks c) {
        ServerLevel level = c.level();
        MinecraftServer server = level.getServer();
        var ledger = com.alexh.capitalism.economy.GoldLedger.get(server);

        // 32 gold on them and 32 in their own chest: 64 gold, 16 owed
        ServerPlayer p = c.player("Taxed", UUID.randomUUID());
        setGold(p, GoldMoney.fromGold(32), 0);
        BlockPos chest = ownedChest(c, 4, 4, p.getUUID(), new ItemStack(Items.GOLD_INGOT, 32));
        c.check(DeathTax.taxableGold(p) == GoldMoney.fromGold(64), "all their gold: 32 on them + 32 in their chest = 64 gold");
        Vec3 died = p.position();
        kill(p, level.damageSources().fellOutOfWorld());
        c.check(DeathTax.owed(p) == GoldMoney.fromGold(16), "dying owes 16 gold (25% of 64): " + GoldMoney.format(DeathTax.owed(p)));
        boolean tagged = !level.getEntitiesOfClass(ItemEntity.class, new AABB(died, died).inflate(6), e -> GoldCollector.isGold(e.getItem())).isEmpty()
            && level.getEntitiesOfClass(ItemEntity.class, new AABB(died, died).inflate(6), e -> GoldCollector.isGold(e.getItem()))
            .stream().allMatch(e -> GoldCollector.droppedBy(e, p.getUUID()));
        c.check(tagged || level.getGameRules().get(GameRules.KEEP_INVENTORY), "their dropped gold on the ground is tagged as theirs");
        clearDrops(level, p);
        level.setBlock(chest, Blocks.AIR.defaultBlockState(), 3);
        ledger.removeContainer(net.minecraft.core.GlobalPos.of(level.dimension(), chest));
        ServerPlayer pb = respawn(p);
        clearDebt(pb);

        // the order: inventory, then their chests, then their gold on the ground, then raw gold
        UUID otherId = UUID.randomUUID();
        ServerPlayer q = c.player("Collected", UUID.randomUUID());
        setGold(q, GoldMoney.fromGold(2), 0);
        q.getInventory().add(new ItemStack(Items.RAW_GOLD, 5));
        BlockPos mine = ownedChest(c, 1, 5, q.getUUID(), new ItemStack(Items.GOLD_INGOT, 3));
        BlockPos theirs = ownedChest(c, 3, 5, otherId, new ItemStack(Items.GOLD_INGOT, 10));
        BlockPos city = BlockPos.containing(c.at(5, 2, 1));
        level.setBlock(city, com.alexh.capitalism.core.ModBlocks.CITY_CHEST.get().defaultBlockState(), 3);
        own(level, city, q.getUUID(), new ItemStack(Items.GOLD_INGOT, 10));
        ItemEntity myDrop = onGround(c, 1.5, 1.5, new ItemStack(Items.GOLD_INGOT, 2), q.getUUID());
        ItemEntity otherDrop = onGround(c, 3.5, 1.5, new ItemStack(Items.GOLD_INGOT, 5), otherId);
        c.check(DeathTax.taxableGold(q) == GoldMoney.fromGold(10), "their gold: 2 carried + 5 Raw Gold (1 gold each) + 3 in their chest; a City Chest (the town's) isn't in it, nor another player's chest");
        owe(q, null, "Death", GoldMoney.fromGold(3));
        run(q, "pay death");
        c.check(DeathTax.owed(q) == 0 && GoldMoney.carried(q) == 0 && goldIn(level, mine) == GoldMoney.fromGold(2) && myDrop.isAlive(),
            "3 gold: their 2 on them first, then 1 from their chest (the ground untouched)");
        owe(q, null, "Death", GoldMoney.fromGold(6));
        run(q, "pay death");
        int raw = q.getInventory().countItem(Items.RAW_GOLD);
        c.check(DeathTax.owed(q) == GoldMoney.fromGold(2) && goldIn(level, mine) == 0 && !myDrop.isAlive() && raw == 5,
            "6 more: the chest's 2, their 2 on the ground, and 2 still owed: Raw Gold never pays (all 5 kept, to smelt): " + raw);
        for (int i = 0, left = 2; i < q.getInventory().getContainerSize() && left > 0; i++) {
            ItemStack s = q.getInventory().getItem(i);
            if (s.is(Items.RAW_GOLD)) { int n = Math.min(left, s.getCount()); s.shrink(n); left -= n; }
        }
        q.getInventory().add(new ItemStack(Items.GOLD_INGOT, 2));
        run(q, "pay death");
        c.check(DeathTax.owed(q) == 0, "smelted 2 Raw Gold into ingots, they pay the rest");
        c.check(goldIn(level, theirs) == GoldMoney.fromGold(10) && goldIn(level, city) == GoldMoney.fromGold(10) && otherDrop.isAlive() && otherDrop.getItem().getCount() == 5,
            "never another player's chest, the town's City Chest, or another player's gold on the ground");

        // change: a gold block for 21 nuggets
        ServerPlayer r = c.player();
        setGold(r, 0, 0);
        r.getInventory().add(new ItemStack(Items.GOLD_BLOCK));
        owe(r, null, "Death", 21);
        run(r, "pay death");
        c.check(DeathTax.owed(r) == 0 && GoldMoney.carried(r) == 60 && r.getInventory().countItem(Items.GOLD_BLOCK) == 0, "a block broken for 21 nuggets: 60 back in change");

        // an unloaded chest: counted (its last count) but out of reach: a part payment
        ServerPlayer u = c.player();
        setGold(u, GoldMoney.fromGold(2), 0);
        var far = net.minecraft.core.GlobalPos.of(level.dimension(), new BlockPos(29_000_000, 64, 29_000_000));
        ledger.setContainer(far, u.getUUID(), GoldMoney.fromGold(9));
        c.check(!level.isLoaded(far.pos()) && DeathTax.taxableGold(u) == GoldMoney.fromGold(11), "a chest in an unloaded chunk counts at its last count (11 gold)");
        owe(u, null, "Death", GoldMoney.fromGold(5));
        GoldCollector.Result got = GoldCollector.collect(u, GoldMoney.fromGold(5));
        c.check(got.taken() == GoldMoney.fromGold(2) && got.unloaded(), "paying 5 takes the 2 in reach, says some gold is in unloaded chests");
        ledger.removeContainer(far);
        clearDebt(u);

        // dropped gold is its owner's alone
        ServerPlayer owner = c.player("Owner", UUID.randomUUID());
        setGold(owner, 0, 0);
        ItemEntity tossed = owner.drop(new ItemStack(Items.GOLD_INGOT, 10), false, net.minecraft.util.Prediction.SERVER_ONLY);
        c.check(tossed != null && GoldCollector.droppedBy(tossed, owner.getUUID()) && owner.getUUID().equals(tossed.getTarget())
            && tossed.getCustomName() != null && tossed.getCustomName().getString().equals("Owner's gold"), "tossed gold is locked to its owner, named \"Owner's gold\"");
        if (tossed == null) return;
        ServerPlayer finder = c.player();
        setGold(finder, 0, 0);
        tossed.setNoPickUpDelay();
        tossed.playerTouch(finder);
        c.check(tossed.isAlive() && tossed.getItem().getCount() == 10 && GoldMoney.carried(finder) == 0, "nobody else can pick it up");
        c.check(tossed.getAge() != -32768, "it despawns like any dropped item (5 minutes)");
        BlockPos hopperPos = BlockPos.containing(tossed.position()).below();
        level.setBlock(hopperPos, Blocks.HOPPER.defaultBlockState(), 3);
        var hopper = (net.minecraft.world.level.block.entity.HopperBlockEntity) level.getBlockEntity(hopperPos);
        boolean sucked = hopper != null && net.minecraft.world.level.block.entity.HopperBlockEntity.suckInItems(level, hopper);
        c.check(!sucked && tossed.isAlive() && hopper != null && hopper.isEmpty(), "a hopper can't take it");
        ItemEntity loose = onGround(c, 2.5, 3.5, new ItemStack(Items.GOLD_NUGGET, 3), null);
        loose.snapTo(tossed.getX(), tossed.getY(), tossed.getZ());
        boolean suckedLoose = hopper != null && net.minecraft.world.level.block.entity.HopperBlockEntity.suckInItems(level, hopper);
        c.check(suckedLoose, "...though it takes gold nobody dropped");
        level.setBlock(hopperPos, Blocks.AIR.defaultBlockState(), 3);
        loose.discard();
        tossed.playerTouch(owner);
        c.check(!tossed.isAlive() && GoldMoney.carried(owner) == GoldMoney.fromGold(10), "its owner picks it up");
        // a shulker box with gold in it is locked too; one without isn't
        ItemStack box = new ItemStack(Items.SHULKER_BOX);
        box.set(net.minecraft.core.component.DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.fromItems(List.of(new ItemStack(Items.GOLD_INGOT, 4))));
        ItemEntity boxDrop = owner.drop(box, false, net.minecraft.util.Prediction.SERVER_ONLY);
        ItemEntity dirtBox = owner.drop(new ItemStack(Items.SHULKER_BOX), false, net.minecraft.util.Prediction.SERVER_ONLY);
        c.check(boxDrop != null && GoldCollector.isLocked(boxDrop) && dirtBox != null && !GoldCollector.isLocked(dirtBox), "a shulker box holding gold is locked; an empty one isn't");
        if (boxDrop != null) boxDrop.discard();
        if (dirtBox != null) dirtBox.discard();
        otherDrop.discard();
        for (BlockPos pos : List.of(mine, theirs, city)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            ledger.removeContainer(net.minecraft.core.GlobalPos.of(level.dimension(), pos));
        }
    }

    // ── The Purge: PvP outside town, the safe zone, the killer's share, mob deaths, standings, PvP put back ──

    private static void purge(GameTests.Checks c) {
        ServerLevel level = c.level();
        MinecraftServer server = level.getServer();
        BloodMoon.testSetActive(server, false);
        if (Purge.isActive()) Purge.end(server, false);
        Purge.unschedule(server);
        boolean pvpBefore = level.getGameRules().get(GameRules.PVP);
        level.getGameRules().set(GameRules.PVP, false, server);

        BloodMoon.testSetActive(server, true);
        c.check(Purge.schedule(server), "a Blood Moon doesn't stop The Purge being announced (they can share a night)");
        Purge.unschedule(server);
        BloodMoon.testSetActive(server, false);
        c.check(Purge.schedule(server) && Purge.data(server).night() == Calendar.date(level).index() + 1, "announced for tomorrow night");
        day(level, Calendar.date(level).index() + 1);
        c.check(Purge.claimsTonight(server), "on the day, tonight is the Purge's");
        Purge.unschedule(server);

        // plain PvP (no Purge): a player's kill brings no death tax and no Purgatory
        ServerPlayer duelist = c.player("Duelist", UUID.randomUUID());
        ServerPlayer loser = c.player();
        setGold(loser, GoldMoney.fromGold(40), 0);
        DeathTax.died(loser, level.damageSources().playerAttack(duelist));
        c.check(!Purge.isActive() && DeathTax.owed(loser) == 0 && !DeathTax.restricted(loser), "killed by a player outside The Purge: no death tax, no Purgatory");

        c.check(Purge.start(server) && Purge.isActive() && level.getGameRules().get(GameRules.PVP), "it begins: PvP on (it was off)");
        long tod = Math.floorMod(Calendar.clock(level), Calendar.DAY_TICKS);
        c.check(tod >= 12500 && tod < 23000, "it's night");

        UUID killerId = UUID.randomUUID();
        ServerPlayer killer = c.player("Purger", killerId);
        setGold(killer, 0, 0);
        ServerPlayer victim = c.player();
        setGold(victim, GoldMoney.fromGold(40), 0);
        c.check(!Purge.inSafeZone(victim), "out past the town");
        int killsBefore = DeathTaxLedger.get(server).purgeKills(killerId);
        kill(victim, level.damageSources().playerAttack(killer));
        DeathTaxData vd = DeathTax.data(victim);
        c.check(victim.isDeadOrDying() && vd.owed() == 90 && killerId.equals(vd.debts().get(0).creditor()),
            "killed by a player: their 90 nuggets of tax are owed to the killer");
        c.check(DeathTaxLedger.get(server).purgeKills(killerId) == killsBefore + 1
            && Standings.get(server).ranking(Standings.Category.PURGE_KILLS).stream().anyMatch(e -> e.id().equals(killerId) && e.score() == 1), "a Purge kill on the standings");
        // the killer can't take the gold they dropped: only the tax, once it's paid
        List<ItemEntity> loot = level.getEntitiesOfClass(ItemEntity.class, victim.getBoundingBox().inflate(6), e -> GoldCollector.holdsGold(e.getItem()));
        for (ItemEntity e : loot) {
            e.setNoPickUpDelay();
            e.playerTouch(killer);
        }
        c.check(!loot.isEmpty() && loot.stream().allMatch(ItemEntity::isAlive) && GoldMoney.carried(killer) == 0, "the killer can't pick up the victim's dropped gold");
        clearDrops(level, victim);

        // the safe zone
        BlockPos town = Town.protectedArea().center(120);
        ServerPlayer safe = c.player();
        safe.snapTo(town.getX() + 0.5, town.getY(), town.getZ() + 0.5, 0, 0);
        float hp = safe.getHealth();
        boolean hurt = safe.hurtServer(level, level.damageSources().playerAttack(killer), 4.0F);
        c.check(Purge.inSafeZone(safe) && !hurt && safe.getHealth() == hp, "in the town nobody can hurt them");
        ServerPlayer outside = c.player();
        killer.snapTo(town.getX() + 0.5, town.getY(), town.getZ() + 0.5, 0, 0);
        hp = outside.getHealth();
        c.check(!outside.hurtServer(level, level.damageSources().playerAttack(killer), 4.0F) && outside.getHealth() == hp, "...nor can anyone in the town hurt others");
        Vec3 back = c.at(2.5, 2, 2.5);
        killer.snapTo(back.x, back.y, back.z, 0, 0);
        c.check(Purge.killerFor(safe, level.damageSources().playerAttack(killer)) == null, "a death in the town is the bank's");

        // 10 gold or less: Death takes pity, no tax
        ServerPlayer poor = c.player();
        setGold(poor, GoldMoney.fromGold(10), 0);
        DeathTax.died(poor, level.damageSources().generic());
        c.check(DeathTax.owed(poor) == 0, "dying with 10 gold: no death tax (pity)");
        ServerPlayer richer = c.player();
        setGold(richer, GoldMoney.fromGold(11), 0);
        DeathTax.died(richer, level.damageSources().generic());
        c.check(DeathTax.owed(richer) > 0, "with 11 gold: the death tax as usual");

        // a monster's kill, and their own: the bank's
        ServerPlayer mobbed = c.player();
        setGold(mobbed, GoldMoney.fromGold(16), 0);
        Zombie zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
        if (zombie != null) {
            zombie.snapTo(back.x, back.y, back.z + 2, 0, 0);
            level.addFreshEntity(zombie);
            kill(mobbed, level.damageSources().mobAttack(zombie));
            c.check(DeathTax.owed(mobbed) == 36 && DeathTax.data(mobbed).debts().get(0).creditor() == null, "killed by a monster in the Purge: owed to the bank");
            zombie.discard();
            clearDrops(level, mobbed);
        }
        ServerPlayer self = c.player();
        c.check(Purge.killerFor(self, level.damageSources().playerAttack(self)) == null, "killing yourself owes nobody but the bank (no gold made)");

        // paying: the killer gets it, online
        ServerPlayer payer = respawn(victim);
        setGold(payer, GoldMoney.fromGold(20), 0);
        run(payer, "pay death");
        c.check(DeathTax.owed(payer) == 0 && GoldMoney.carried(killer) == 90 && GoldMoney.carried(payer) == 180 - 90,
            "paid: the killer gets exactly the 90 nuggets, no more, no less");
        c.check(DeathTaxLedger.get(server).goldTaken(killerId) == 90
            && Standings.get(server).ranking(Standings.Category.GOLD_TAKEN).stream().anyMatch(e -> e.id().equals(killerId) && e.score() == 10), "gold taken on the standings (10 gold)");

        // offline: kept for their next login
        UUID awayId = UUID.randomUUID();
        ServerPlayer away = c.player("Away", awayId);
        setGold(away, 0, 0);
        ServerPlayer victim2 = c.player();
        setGold(victim2, GoldMoney.fromGold(32), 0);
        kill(victim2, level.damageSources().playerAttack(away));
        c.check(DeathTax.owed(victim2) == 72, "a second Purge kill: 72 nuggets owed to Away");
        clearDrops(level, victim2);
        server.getPlayerList().remove(away);
        ServerPlayer victim2b = respawn(victim2);
        setGold(victim2b, GoldMoney.fromGold(10), 0);
        run(victim2b, "pay death");
        c.check(DeathTax.owed(victim2b) == 0 && DeathTaxLedger.get(server).owedTo(awayId) == 72, "the killer is away: their 72 nuggets wait for them");
        ServerPlayer awayBack = c.player("Away", awayId);
        c.check(DeathTaxLedger.get(server).owedTo(awayId) == 0 && GoldMoney.carried(awayBack) >= 72, "...and are handed over when they log in: " + GoldMoney.carried(awayBack));

        // dawn: PvP put back; kills after it are the bank's
        Purge.end(server, true);
        c.check(!Purge.isActive() && !level.getGameRules().get(GameRules.PVP), "it ends: PvP back to what it was (off)");
        c.check(Purge.killerFor(self, level.damageSources().playerAttack(killer)) == null, "after the Purge, a player kill is the bank's");
        // no PvP in the town, ever (not just in The Purge): with PvP on and no Purge, a player in town can't be hurt
        level.getGameRules().set(GameRules.PVP, true, server);
        ServerPlayer townie = c.player();
        townie.snapTo(town.getX() + 0.5, town.getY(), town.getZ() + 0.5, 0, 0);
        float townHp = townie.getHealth();
        boolean townHurt = townie.hurtServer(level, level.damageSources().playerAttack(killer), 4.0F);
        ServerPlayer outsider = c.player();
        float outHp = outsider.getHealth();
        outsider.hurtServer(level, level.damageSources().playerAttack(killer), 4.0F);
        c.check(!townHurt && townie.getHealth() == townHp && outsider.getHealth() < outHp,
            "no Purge: still no PvP in the town (outside it, PvP is up to the game rule)");
        level.getGameRules().set(GameRules.PVP, pvpBefore, server);
    }
}
