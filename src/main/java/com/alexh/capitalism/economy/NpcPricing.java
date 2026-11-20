package com.alexh.capitalism.economy;

import com.alexh.capitalism.weaponsmith.Socketing;
import net.minecraft.world.entity.player.Player;

// Prices a player pays when BUYING from a town NPC shop (npc/NpcShops). Every NPC shop must get its prices from here,
// so the NPC markup and anything that changes prices (an Emerald in an armor socket: 7.5% off per piece, see
// weaponsmith/Socketing; the Merchant's Ledger skill book: 7.5% off, 15% at level II; a festival sale day, calendar/Festival:
// Founders' Day 20% off, Frost Fair 15%) always apply. Player-run Shopfronts are NOT NPCs: they keep the owner's prices.
public final class NpcPricing {

    // NPCs are a fallback for when no player with the job is online: they charge 50% more than a fair player price,
    // so buying from a real player is always the better deal
    public static final double NPC_MARKUP = 1.5;

    private NpcPricing() {
    }

    // Share of the price taken off for this player (0.075 per Emerald-socketed armor piece worn, so up to 0.30)
    public static double discount(Player buyer) {
        return Socketing.armorWith(buyer, Socketing.Gem.EMERALD) * Socketing.EMERALD_DISCOUNT_PER_PIECE
            + com.alexh.capitalism.librarian.Skills.level(buyer, com.alexh.capitalism.librarian.Skill.MERCHANTS_LEDGER) * 0.075
            + com.alexh.capitalism.calendar.Festival.npcDiscount(buyer.level())
            + (buyer.hasEffect(com.alexh.capitalism.core.ModEffects.SILVER_TONGUE) ? SILVER_TONGUE_DISCOUNT : 0);
    }

    // Silver Tongue (a Haggler's Highball): 10% off
    public static final double SILVER_TONGUE_DISCOUNT = 0.10;

    // What this player pays an NPC for something whose fair (player) price is fairNuggets: the markup, then their
    // discount. In gold nuggets (9 = 1 gold, see GoldMoney). Never below 1 nugget.
    public static long npcPrice(long fairNuggets, Player buyer) {
        if (fairNuggets <= 0) return 0;
        return Math.max(1, Math.round(fairNuggets * NPC_MARKUP * (1.0 - discount(buyer))));
    }

    // The biggest discount anyone could ever have: 4 Emerald-socketed armor pieces, the Merchant's Ledger at level II and
    // the best festival sale on the calendar
    public static double maxDiscount() {
        double festival = 0;
        for (com.alexh.capitalism.calendar.Festival f : com.alexh.capitalism.calendar.Festival.values()) festival = Math.max(festival, f.npcDiscount);
        return 4 * Socketing.EMERALD_DISCOUNT_PER_PIECE + 2 * 0.075 + festival + SILVER_TONGUE_DISCOUNT;
    }

    // The least anyone could ever pay an NPC for this (with every discount at once). The Banker never pays more than this
    // for anything an NPC sells (economy/BankerEvents), so buying from an NPC to sell to the bank can't make money.
    public static long lowestNpcPrice(long fairNuggets) {
        if (fairNuggets <= 0) return 0;
        return Math.max(1, Math.round(fairNuggets * NPC_MARKUP * (1.0 - maxDiscount())));
    }
}
