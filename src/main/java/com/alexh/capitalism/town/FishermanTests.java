package com.alexh.capitalism.town;

import com.alexh.capitalism.core.ModComponents;
import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.economy.GoldMoney;
import com.alexh.capitalism.economy.NpcPricing;
import com.alexh.capitalism.fishing.CatchInfo;
import com.alexh.capitalism.fishing.FishMarket;
import com.alexh.capitalism.fishing.RareCatch;
import com.alexh.capitalism.fishing.RareCatchModifier;
import com.alexh.capitalism.gametest.GameTests;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Objects;

// The Fisherman's shop (Town.md > The Fisherman, town/FishermanMenu), run by the game test server (gametest/GameTests):
//  - Sell pays what FishMarket.valueOf says and takes the catch;
//  - he won't take cod or salmon (they don't go in a sell slot);
//  - Mount takes the plaque's NpcPricing price and gives a plaque carrying the catch's kind and story;
//  - without enough gold, Mount does nothing.
public final class FishermanTests {

    static {
        GameTests.register("fisherman", FishermanTests::fisherman);
    }

    private FishermanTests() {
    }

    public static void init() {
    }

    private static ItemStack mako(GameTests.Checks c) {
        return RareCatchModifier.make(RareCatch.MAKO_SHARK, "Tester", "Summer 9, Year 1", c.level().getRandom());
    }

    private static int count(Inventory inventory, net.minecraft.world.item.Item item) {
        int n = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(item)) n += inventory.getItem(i).getCount();
        }
        return n;
    }

    private static ItemStack find(Inventory inventory, net.minecraft.world.item.Item item) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(item)) return inventory.getItem(i);
        }
        return ItemStack.EMPTY;
    }

    private static void fisherman(GameTests.Checks c) {
        // ── selling ──
        ServerPlayer seller = c.player();
        FishermanMenu sellMenu = new FishermanMenu(1, seller.getInventory(), null);
        ItemStack fish = mako(c);
        long value = FishMarket.valueOf(fish);
        c.check(value > 0 && sellMenu.getSlot(0).mayPlace(fish), "a Mako Shark goes in a sell slot (worth " + GoldMoney.format(value) + ")");
        c.check(!sellMenu.getSlot(0).mayPlace(new ItemStack(Items.COD)) && !sellMenu.getSlot(0).mayPlace(new ItemStack(Items.SALMON))
            && !FishMarket.buys(new ItemStack(Items.COD)), "he refuses cod and salmon: they don't go in a sell slot");
        sellMenu.getSlot(0).set(fish);
        c.check(sellMenu.offer() == value, "his offer is the catch's value");
        long before = GoldMoney.carried(seller);
        c.check(sellMenu.clickMenuButton(seller, FishermanMenu.SELL), "Sell works");
        c.check(GoldMoney.carried(seller) - before == value, "selling paid " + GoldMoney.format(value));
        c.check(sellMenu.getSlot(0).getItem().isEmpty() && sellMenu.offer() == 0, "the catch is gone from the slot");
        c.check(!sellMenu.clickMenuButton(seller, FishermanMenu.SELL), "an empty row sells nothing");

        // ── mounting ──
        ServerPlayer mounter = c.player();
        FishermanMenu mountMenu = new FishermanMenu(2, mounter.getInventory(), null);
        ItemStack catchToMount = mako(c);
        CatchInfo info = catchToMount.get(ModComponents.CATCH_INFO.get());
        long price = FishMarket.plaquePrice(catchToMount, mounter);
        c.check(price == NpcPricing.npcPrice(FishMarket.plaqueFair(catchToMount), mounter) && price > FishMarket.plaqueFair(catchToMount),
            "the plaque's price goes through NpcPricing (" + GoldMoney.format(price) + ")");
        c.check(mountMenu.getSlot(FishermanMenu.MOUNT_SLOT).mayPlace(catchToMount)
            && !mountMenu.getSlot(FishermanMenu.MOUNT_SLOT).mayPlace(new ItemStack(Items.COD)), "the mount slot takes the shark, not cod");
        GoldMoney.give(mounter, price + 20);
        mountMenu.getSlot(FishermanMenu.MOUNT_SLOT).set(catchToMount);
        long had = GoldMoney.carried(mounter);
        c.check(mountMenu.clickMenuButton(mounter, FishermanMenu.MOUNT), "Mount works");
        c.check(had - GoldMoney.carried(mounter) == price, "mounting took " + GoldMoney.format(price));
        ItemStack plaque = find(mounter.getInventory(), ModItems.FISH_PLAQUE.get());
        c.check(!plaque.isEmpty() && RareCatch.MAKO_SHARK.id.equals(plaque.get(ModComponents.PLAQUE_CATCH.get()))
            && Objects.equals(plaque.get(ModComponents.CATCH_INFO.get()), info), "you get a plaque with the catch's kind and story (" + (info == null ? "?" : info.line()) + ")");
        c.check(mountMenu.toMount().isEmpty(), "the catch is used up");

        // ── not enough gold ──
        ServerPlayer broke = c.player();
        FishermanMenu brokeMenu = new FishermanMenu(3, broke.getInventory(), null);
        ItemStack another = mako(c);
        brokeMenu.getSlot(FishermanMenu.MOUNT_SLOT).set(another);
        long cost = FishMarket.plaquePrice(another, broke);
        GoldMoney.give(broke, cost - 1);
        c.check(!brokeMenu.clickMenuButton(broke, FishermanMenu.MOUNT), "short of gold, Mount fails");
        c.check(GoldMoney.carried(broke) == cost - 1 && !brokeMenu.toMount().isEmpty() && count(broke.getInventory(), ModItems.FISH_PLAQUE.get()) == 0,
            "and nothing happens: the gold and the catch stay, no plaque");

        // ── leftovers go back ──
        ServerPlayer leaver = c.player();
        FishermanMenu leaveMenu = new FishermanMenu(4, leaver.getInventory(), null);
        leaveMenu.getSlot(3).set(mako(c));
        leaveMenu.removed(leaver);
        c.check(count(leaver.getInventory(), ModItems.RARE_CATCHES.get(RareCatch.MAKO_SHARK).get()) == 1, "a catch left in a slot goes back when you close it");
    }
}
