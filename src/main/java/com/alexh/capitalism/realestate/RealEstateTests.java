package com.alexh.capitalism.realestate;

import com.alexh.capitalism.core.ModAttachments;
import com.alexh.capitalism.economy.GoldMoney;
import com.alexh.capitalism.gametest.GameTests;
import com.alexh.capitalism.occupation.Job;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;
import java.util.UUID;

// Real estate (RealEstate.md), on a fresh set of lots (not the world's): making lots (names, no overlaps), buying off
// the city (agents only, at the City Chest: what it wants and the gold), who can build where, selling, leasing, the rent each day (paid to the agent, a
// tenant who can't pay loses the lease), ending a lease.
public final class RealEstateTests {

    static {
        GameTests.register("real_estate", RealEstateTests::realEstate);
    }

    private RealEstateTests() {
    }

    public static void init() {
    }

    private static void realEstate(GameTests.Checks c) {
        Properties lots = new Properties();
        c.check(lots.create("Main_Street_1", new BlockPos(0, 64, 0), new BlockPos(9, 64, 14)) == null, "a lot is made from two corners");
        Property lot = lots.byName("main_street_1");
        c.check(lot != null && lot.width() == 10 && lot.length() == 15, "its size, and its name doesn't care about capitals");
        c.check(lots.create("Overlap", new BlockPos(5, 64, 5), new BlockPos(20, 64, 20)) != null, "lots can't overlap");
        c.check(lots.create("two words", new BlockPos(50, 64, 50), new BlockPos(60, 64, 60)) != null, "a lot's name is one word");
        c.check(lot.contains(new BlockPos(9, 200, 14)) && !lot.contains(new BlockPos(10, 64, 0)), "a lot is its whole column, corners included");

        ServerPlayer agent = c.player();
        ServerPlayer buyer = c.player();
        ServerPlayer stranger = c.player();
        c.check(lots.claimFromCity(agent, lot) != null && lot.cityOwned(), "only a Real Estate Agent can buy city land");
        agent.getData(ModAttachments.JOBS.get()).add(Job.REAL_ESTATE_AGENT);
        GoldMoney.give(agent, GoldMoney.fromGold(10));
        c.check(lots.claimFromCity(agent, lot) == null && lot.ownedBy(agent), "an agent who paid the City Chest owns the lot");
        c.check(GoldMoney.carried(agent) == GoldMoney.fromGold(10), "buying never takes gold from their pockets");
        c.check(lot.canBuild(agent) && !lot.canBuild(stranger), "the owner can build on it, nobody else");

        // leasing
        c.check(lots.lease(agent, lot, buyer, 0) != null, "no lease until there's a rent");
        lots.setRent(lot, GoldMoney.fromGold(5));
        c.check(lots.lease(stranger, lot, buyer, 0) != null, "only its owner can lease it");
        c.check(lots.lease(agent, lot, buyer, 0) == null && lot.canBuild(buyer) && lot.ownedBy(agent), "leased: the tenant can build, the agent still owns it");
        c.check(lots.sell(agent, lot, buyer) != null, "a leased lot can't be sold until the lease ends");
        Map<UUID, ServerPlayer> online = Map.of(agent.getUUID(), agent, buyer.getUUID(), buyer);
        GoldMoney.give(buyer, GoldMoney.fromGold(7));
        lots.collectRent(online::get, 1);
        c.check(GoldMoney.carried(buyer) == GoldMoney.fromGold(2) && GoldMoney.carried(agent) == GoldMoney.fromGold(15),
            "a day later the rent goes from the tenant to the agent");
        lots.collectRent(online::get, 1);
        c.check(GoldMoney.carried(buyer) == GoldMoney.fromGold(2), "only once a day");
        lots.collectRent(online::get, 2);
        c.check(!lot.leased() && !lot.canBuild(buyer), "a tenant who can't pay loses the lease");

        // lease again and end it
        lots.lease(agent, lot, buyer, 2);
        c.check(lots.evict(stranger, lot) != null && lot.leased(), "only the owner ends a lease");
        c.check(lots.evict(agent, lot) == null && !lot.leased(), "the owner ends the lease");

        // selling
        c.check(lots.sell(stranger, lot, buyer) != null, "only its owner can sell it");
        c.check(lots.sell(agent, lot, buyer) == null && lot.ownedBy(buyer) && !lot.canBuild(agent), "sold: it's the buyer's, the agent can't build on it");
        c.check(lots.lease(buyer, lot, stranger, 3) != null, "a player who isn't an agent can't lease it out");
        c.check(lots.delete("Main_Street_1") && lots.byName("Main_Street_1") == null, "an op can delete a lot");

        // the City Chest: each wanted thing, and the slot right under it
        ItemStack none = ItemStack.EMPTY;
        java.util.List<ItemStack> wanted = java.util.List.of(new ItemStack(Items.OAK_LOG, 32), new ItemStack(Items.GOLD_INGOT, 5), none);
        c.check(CityChestBlockEntity.fits(wanted.get(0), new ItemStack(Items.OAK_LOG)) && !CityChestBlockEntity.fits(wanted.get(0), new ItemStack(Items.STONE)),
            "a slot only takes the thing above it");
        c.check(CityChestBlockEntity.fits(wanted.get(1), new ItemStack(Items.GOLD_NUGGET)) && CityChestBlockEntity.fits(wanted.get(1), new ItemStack(Items.GOLD_BLOCK))
            && !CityChestBlockEntity.fits(wanted.get(2), new ItemStack(Items.OAK_LOG)), "a gold slot takes any gold; an empty one takes nothing");
        c.check(CityChestBlockEntity.leftover(wanted, java.util.List.of(new ItemStack(Items.OAK_LOG, 32), none, none)) == null,
            "the chest waits until every slot is filled");
        c.check(CityChestBlockEntity.leftover(wanted, java.util.List.of(new ItemStack(Items.OAK_LOG, 32), new ItemStack(Items.GOLD_NUGGET, 44), none)) == null,
            "...gold included");
        java.util.List<ItemStack> paid = CityChestBlockEntity.leftover(wanted, java.util.List.of(new ItemStack(Items.OAK_LOG, 32), new ItemStack(Items.GOLD_NUGGET, 45), none));
        c.check(paid != null && paid.isEmpty(), "paid in nuggets just as well, nothing back");
        java.util.List<ItemStack> back = CityChestBlockEntity.leftover(wanted, java.util.List.of(new ItemStack(Items.OAK_LOG, 40), new ItemStack(Items.GOLD_BLOCK, 1), none));
        long change = back == null ? -1 : back.stream().mapToLong(GoldMoney::valueOf).sum();
        int logsBack = back == null ? -1 : back.stream().filter(s -> s.is(Items.OAK_LOG)).mapToInt(ItemStack::getCount).sum();
        c.check(change == GoldMoney.fromGold(4) && logsBack == 8, "the extra logs and the change come back (" + change + ", " + logsBack + ")");
        c.check(CityChestBlockEntity.leftover(java.util.List.of(none, none), java.util.List.of(none, none)) == null, "an empty chest sells nothing");
        // the city's price can never leave the chest
        CityChestBlockEntity chest = new CityChestBlockEntity(BlockPos.ZERO, com.alexh.capitalism.core.ModBlocks.CITY_CHEST.get().defaultBlockState());
        chest.setItem(0, new ItemStack(Items.GOLD_INGOT, 50));
        c.check(!chest.canTakeItem(new net.minecraft.world.SimpleContainer(1), 0, chest.getItem(0)) && !chest.canPlaceItem(0, new ItemStack(Items.GOLD_INGOT)),
            "no hopper can take from or put into the City Chest");
        java.util.List<ItemStack> noGoldBack = CityChestBlockEntity.leftover(java.util.List.of(new ItemStack(Items.GOLD_INGOT, 50)), java.util.List.of(new ItemStack(Items.GOLD_INGOT, 50)));
        c.check(noGoldBack != null && noGoldBack.isEmpty(), "paying exactly gives nothing back: the price's gold is never handed out");
        var state = com.alexh.capitalism.core.ModBlocks.CITY_CHEST.get().defaultBlockState();
        c.check(state.is(net.minecraft.tags.BlockTags.WITHER_IMMUNE) && state.is(net.minecraft.tags.BlockTags.DRAGON_IMMUNE) && state.getBlock().getExplosionResistance() > 1000,
            "withers, the dragon and explosions can't break it");
        lots.create("Chest_Lot", new BlockPos(100, 64, 100), new BlockPos(110, 64, 110));
        Property chestLot = lots.byName("Chest_Lot");
        c.check(lots.claimFromCity(stranger, chestLot) != null && chestLot.cityOwned(), "only an agent can unlock a lot");
        c.check(lots.claimFromCity(agent, chestLot) == null && chestLot.ownedBy(agent), "an agent who paid the chest owns the lot");
    }
}
