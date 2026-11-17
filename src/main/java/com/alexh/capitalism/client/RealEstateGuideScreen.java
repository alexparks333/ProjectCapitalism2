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

// The Real Estate Agent's Guide (agents only, JobGuideScreen's layout in deed green and brass):
//  - Workshop: Items (the For Sale and For Lease signs).
//  - Information: Lots (buying at the City Chest, building), Selling and Leasing (the commands, the rent), Player Debuffs.
// Keep every line short, and no numbers: players find those out. Hand-written: keep it matching RealEstate.md.
public class RealEstateGuideScreen extends JobGuideScreen {

    private static final Theme THEME = new Theme(0xFF1E3A2A, 0xFF10241A, 0xFFE0C070, 0xFF5A4A20, 0xFF0A160E, 0xFF4A6A50,
        0xFFF4DE8E, 0xFFF0F4E8, 0xFFB8C8B0, 0xFFE8D48A);

    private static final Tab ITEMS = new Tab("Workshop", "Signs", new ItemStack(ModItems.FOR_SALE_SIGN.get()));
    private static final Tab LOTS = new Tab("Information", "Lots", new ItemStack(ModItems.CITY_SALE_SIGN.get()));
    private static final Tab DEALS = new Tab("Information", "Selling & Leasing", new ItemStack(Items.GOLD_INGOT));
    private static final Tab DEBUFFS = new Tab("Information", "Player Debuffs", new ItemStack(Items.BARRIER));

    public RealEstateGuideScreen(@Nullable Screen parent) {
        super("Real Estate Agent's Guide", THEME, List.of(ITEMS, LOTS, DEALS, DEBUFFS), entries(), parent);
    }

    private static List<Entry> entries() {
        ItemStack e = ItemStack.EMPTY;
        ItemStack sign = new ItemStack(Items.OAK_SIGN);
        ItemStack forSale = new ItemStack(ModItems.FOR_SALE_SIGN.get());
        ItemStack forLease = new ItemStack(ModItems.FOR_LEASE_SIGN.get());
        ItemStack city = new ItemStack(ModItems.CITY_SALE_SIGN.get());
        ItemStack gold = new ItemStack(Items.GOLD_INGOT);
        List<Entry> list = new ArrayList<>();

        // ── Workshop: the signs ──
        list.add(made(ITEMS, forSale, "For Sale Sign",
            HowToMake.crafting(e, new ItemStack(Items.DYE.red()), e, e, sign, e, e, e, e),
            "Put it up on a lot you own.", "It shows your asking price."));
        list.add(made(ITEMS, forLease, "For Lease Sign",
            HowToMake.crafting(e, new ItemStack(Items.DYE.blue()), e, e, sign, e, e, e, e),
            "Put it up on a lot you own.", "It shows the rent a day."));

        // ── Information: Lots ──
        list.add(info(LOTS, city, "City Land", List.of(city),
            "The town is split into lots.", "These signs show which ones", "the city sells. Only you can buy."));
        ItemStack chest = new ItemStack(ModItems.CITY_CHEST.get());
        list.add(info(LOTS, chest, "Buying a Lot", List.of(chest, gold),
            "Open the lot's City Chest. Put each", "thing in the slot under it until", "they're all green. Any gold coins", "do. Then the lot is yours."));
        list.add(info(LOTS, new ItemStack(Items.BRICKS), "Building", List.of(new ItemStack(Items.BRICKS)),
            "Nobody can build in the town", "center but on their own lot,", "or one they lease."));
        list.add(info(LOTS, new ItemStack(Items.MAP), "Your Lots", List.of(),
            "/properties list shows every lot.", "/properties and a name shows one."));

        // ── Information: Selling & Leasing ──
        list.add(info(DEALS, forSale, "Pricing", List.of(forSale, forLease),
            "/properties <lot> price sets your", "asking price, /properties <lot> rent", "the rent a day. Signs show them."));
        list.add(info(DEALS, forSale, "Selling", List.of(forSale),
            "Settle the price with the buyer,", "then /properties <lot> sell <player>.", "The lot is theirs to build on."));
        list.add(info(DEALS, forLease, "Leasing", List.of(forLease),
            "/properties <lot> lease <player> lets", "them build. The lot stays yours."));
        list.add(info(DEALS, gold, "Rent", List.of(gold),
            "Your tenant pays the rent every day,", "straight to you. If they can't pay,", "the lease ends."));
        list.add(info(DEALS, new ItemStack(Items.BARRIER), "Ending a Lease", List.of(),
            "/properties <lot> evict ends it.", "Sell a leased lot only after."));

        // ── Information: Player Debuffs ──
        list.add(info(DEBUFFS, city, "No City Land", List.of(),
            "Others can't buy from the city."));
        list.add(info(DEBUFFS, forSale, "No Signs", List.of(),
            "Others can't make or put up", "For Sale and For Lease signs."));
        return list;
    }
}
