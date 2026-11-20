package com.alexh.capitalism.calendar;

import com.alexh.capitalism.calendar.Calendar.Season;
import com.alexh.capitalism.calendar.Calendar.Weekday;
import com.alexh.capitalism.economy.NpcPricing;
import com.alexh.capitalism.gametest.GameTests;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

// The calendar (Calendar.md): the date and time from the clock, seasons and years rolling over, /calendar set, the
// festivals (on their days, the next one, sales through NpcPricing, more XP, the gift once a day).
public final class CalendarTests {

    static {
        GameTests.register("calendar", CalendarTests::calendar);
    }

    private CalendarTests() {
    }

    public static void init() {
    }

    private static void calendar(GameTests.Checks c) {
        // the date
        Calendar.Date first = Calendar.of(0);
        c.check(first.year() == 1 && first.season() == Season.SPRING && first.day() == 1 && first.weekday() == Weekday.MONDAY,
            "day 0 is Monday, Spring 1, Year 1 (" + first.longText() + ")");
        Calendar.Date d27 = Calendar.of(27);
        c.check(d27.season() == Season.SPRING && d27.day() == 28 && d27.weekday() == Weekday.SUNDAY, "day 27 is Sunday, Spring 28 (" + d27.longText() + ")");
        Calendar.Date d28 = Calendar.of(28);
        c.check(d28.season() == Season.SUMMER && d28.day() == 1 && d28.weekday() == Weekday.MONDAY, "day 28 is Monday, Summer 1 (" + d28.longText() + ")");
        Calendar.Date d111 = Calendar.of(111);
        c.check(d111.season() == Season.WINTER && d111.day() == 28 && d111.year() == 1, "day 111 is Winter 28, Year 1 (" + d111.longText() + ")");
        Calendar.Date d112 = Calendar.of(112);
        c.check(d112.season() == Season.SPRING && d112.day() == 1 && d112.year() == 2, "day 112 is Spring 1, Year 2 (" + d112.longText() + ")");
        c.check(Calendar.of(16).shortText().equals("Wed. 17"), "a short date reads like \"Wed. 17\" (" + Calendar.of(16).shortText() + ")");
        // the time
        c.check(Calendar.timeText(0).equals("6:00 am") && Calendar.timeText(6000).equals("12:00 pm")
            && Calendar.timeText(18000).equals("12:00 am") && Calendar.timeText(1170).equals("7:10 am")
            && Calendar.timeText(13000).equals("7:00 pm"), "the clock reads 6:00 am at dawn, 12:00 pm at noon, 7:10 am, 7:00 pm, 12:00 am at midnight ("
            + Calendar.timeText(1170) + ", " + Calendar.timeText(13000) + ")");
        c.check(Calendar.isDay(3000) && !Calendar.isDay(15000), "day and night");
        // /calendar set: to a date, keeping the time of day
        ServerLevel level = c.level();
        Calendar.setClock(level, Calendar.DAY_TICKS * 3L + 7000);
        Calendar.setDate(level, 2, Season.FALL, 15);
        Calendar.Date set = Calendar.date(level);
        long timeOfDay = Math.floorMod(Calendar.clock(level), Calendar.DAY_TICKS);
        c.check(set.year() == 2 && set.season() == Season.FALL && set.day() == 15 && timeOfDay == 7000,
            "the date can be set (Fall 15, Year 2), keeping the time of day (" + set.longText() + ", " + timeOfDay + ")");
        c.check(Festival.today(level) == Festival.HARVEST_FESTIVAL, "Fall 15 is the Harvest Festival");
        // festivals: the next one
        Festival.Upcoming next = Festival.next(Calendar.of(1));
        c.check(next != null && next.festival() == Festival.FOUNDERS_DAY && next.inDays() == 5,
            "from Spring 2 the next festival is Founders' Day, in 5 days (" + next + ")");
        // more XP on the Harvest Festival
        ServerPlayer player = c.player();
        int before = player.totalExperience;
        player.giveExperiencePoints(10);
        c.check(player.totalExperience - before == 15, "on the Harvest Festival XP is worth 50% more (10 -> " + (player.totalExperience - before) + ")");
        // a sale: Founders' Day, NPC prices 20% off (through NpcPricing)
        Calendar.setDate(level, 1, Season.SPRING, 2);
        long normal = NpcPricing.npcPrice(900, player);
        Calendar.setDate(level, 1, Season.SPRING, 7);
        long sale = NpcPricing.npcPrice(900, player);
        c.check(normal == 1350 && sale == 1080, "on Founders' Day NPC prices are 20% off (1350 -> " + sale + ")");
        // Spooky Night's treat: once that day
        Calendar.setDate(level, 1, Season.FALL, 28);
        Calendar.Date spooky = Calendar.date(level);
        int pies = player.getInventory().countItem(Items.PUMPKIN_PIE);
        CalendarEvents.giveGift(player, Festival.SPOOKY_NIGHT, spooky);
        CalendarEvents.giveGift(player, Festival.SPOOKY_NIGHT, spooky);
        c.check(player.getInventory().countItem(Items.PUMPKIN_PIE) - pies == 1, "Spooky Night's treat comes once that day");
        // the Christmas tree: a present on Christmas, once; on other days only a hint
        net.minecraft.core.BlockPos treePos = net.minecraft.core.BlockPos.containing(c.at(2, 2, 2));
        level.setBlock(treePos, com.alexh.capitalism.core.ModBlocks.CHRISTMAS_TREE.get().defaultBlockState()
            .setValue(com.alexh.capitalism.decor.SeasonalDecorBlock.UP, true), 3);
        var presentItem = com.alexh.capitalism.core.ModItems.CHRISTMAS_PRESENT.get();
        int presents = player.getInventory().countItem(presentItem);
        Calendar.setDate(level, 1, Season.WINTER, 3);
        boolean early = com.alexh.capitalism.decor.SeasonalDecorBlock.treePresent(level, treePos, player);
        Calendar.setDate(level, 1, Season.WINTER, 8);
        boolean onChristmas = com.alexh.capitalism.decor.SeasonalDecorBlock.treePresent(level, treePos, player);
        boolean again = com.alexh.capitalism.decor.SeasonalDecorBlock.treePresent(level, treePos, player);
        c.check(!early && onChristmas && !again && player.getInventory().countItem(presentItem) - presents == 1,
            "the Christmas tree gives a present on Christmas, once (not before)");
        c.check(!com.alexh.capitalism.decor.ChristmasPresentItem.contents(level.getRandom()).isEmpty(), "...and a present has things in it");
        level.setBlock(treePos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        // each festival does what it should and no more
        c.check(Festival.NEW_YEAR.fireworks && Festival.NEW_YEAR.gift().isEmpty() && Festival.NEW_YEAR.season == Calendar.Season.WINTER && Festival.NEW_YEAR.day == 28
            && Festival.on(Calendar.date(Calendar.startOf(1, Calendar.Season.SPRING, 1))) == null,
            "New Year's Eve (Winter 28): fireworks over the town at midnight, no gift; nothing on Spring 1 any more");
        c.check(!Festival.HARVEST_FESTIVAL.cropBoost && Festival.HARVEST_FESTIVAL.xp == 1.5 && Festival.BLOSSOM_FESTIVAL.cropBoost,
            "the Harvest Festival is +50% XP; the Blossom Festival grows crops faster");
        c.check(Festival.CHRISTMAS.npcDiscount == 0 && Festival.CHRISTMAS.day == 8 && Festival.WINTER_FEST.day == 25,
            "Christmas (Winter 8) and Winter Fest (Winter 25)");
        Festival b = Festival.BEACH_FESTIVAL;
        c.check(b.npcDiscount == 0 && !b.cropBoost && b.luck == 0 && b.xp == 1.0 && !b.fireworks && !b.peacefulNight && !b.pumpkins && b.gift().isEmpty(),
            "the Beach Festival doesn't do anything yet");
        // every festival on its own day, within its season
        java.util.Set<Long> days = new java.util.HashSet<>();
        boolean ok = true;
        for (Festival f : Festival.values()) {
            ok &= f.day >= 1 && f.day <= Calendar.DAYS_PER_SEASON && days.add((long) f.season.ordinal() * 100 + f.day);
        }
        c.check(ok, "every festival has its own day within its season");
        Calendar.setClock(level, Calendar.DAY_TICKS + 6000L);
    }
}
