package com.alexh.capitalism.calendar;

import com.alexh.capitalism.calendar.Calendar.Season;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Supplier;

// The festivals (Calendar.md): special days in the calendar, each with something that really happens all that day
// (calendar/CalendarEvents does it): NPC shops on sale (through economy/NpcPricing, as every NPC price must), crops
// growing twice as fast, more Luck for fishing, more XP, fireworks after dark, a night with no monsters, monsters in
// pumpkin masks, a treat for everyone, a present from the Christmas tree, snow over the town. Announced in the morning
// (and the day before), shown on the clock (client/ClockHud).
public enum Festival {

    NEW_YEAR("New Year's Day", Season.SPRING, 1, 0xFFFFE070, "A celebration in the town: fireworks all over it tonight.",
        0, false, 0, 1.0, true, false, false, List::of),
    FOUNDERS_DAY("Founders' Day", Season.SPRING, 7, 0xFF7CE07C, "Every town NPC's shop is 20% off today.",
        0.20, false, 0, 1.0, false, false, false, List::of),
    BLOSSOM_FESTIVAL("Blossom Festival", Season.SPRING, 14, 0xFFF59AC8, "Crops grow twice as fast today.",
        0, true, 0, 1.0, false, false, false, List::of),
    ANGLERS_DERBY("Angler's Derby", Season.SUMMER, 9, 0xFF6AC8FF, "Everyone has +3 Luck today: the best day to fish.",
        0, false, 3, 1.0, false, false, false, List::of),
    BEACH_FESTIVAL("Beach Festival", Season.SUMMER, 28, 0xFFFFB040, "A day at the beach.",
        0, false, 0, 1.0, false, false, false, List::of),
    HARVEST_FESTIVAL("Harvest Festival", Season.FALL, 15, 0xFFE8A030, "+50% XP from everything today.",
        0, false, 0, 1.5, false, false, false, List::of),
    SPOOKY_NIGHT("Spooky Night", Season.FALL, 28, 0xFFB070FF, "Monsters come out in pumpkin masks. A treat for everyone.",
        0, false, 0, 1.0, false, false, true, () -> List.of(new ItemStack(Items.PUMPKIN_PIE), new ItemStack(Items.COOKIE, 4))),
    CHRISTMAS("Christmas", Season.WINTER, 8, 0xFFFF5050, "Right-click the Christmas tree for a present!",
        0, false, 0, 1.0, false, false, false, List::of),
    WINTER_FEST("Winter Fest", Season.WINTER, 25, 0xFF9ADCFF, "It snows in the town all day.",
        0, false, 0, 1.0, false, false, false, List::of);

    public final String title;
    public final Season season;
    public final int day;
    public final int color;
    public final String description;
    // what it does: NPC discount (share off), crops twice as fast, Luck, XP multiplier, fireworks all over the town after
    // dark, no
    // monsters spawning tonight, monsters in pumpkin masks, the gift everyone gets once that day
    public final double npcDiscount;
    public final boolean cropBoost;
    public final int luck;
    public final double xp;
    public final boolean fireworks;
    public final boolean peacefulNight;
    public final boolean pumpkins;
    private final Supplier<List<ItemStack>> gift;

    Festival(String title, Season season, int day, int color, String description, double npcDiscount, boolean cropBoost, int luck,
             double xp, boolean fireworks, boolean peacefulNight, boolean pumpkins, Supplier<List<ItemStack>> gift) {
        this.title = title;
        this.season = season;
        this.day = day;
        this.color = color;
        this.description = description;
        this.npcDiscount = npcDiscount;
        this.cropBoost = cropBoost;
        this.luck = luck;
        this.xp = xp;
        this.fireworks = fireworks;
        this.peacefulNight = peacefulNight;
        this.pumpkins = pumpkins;
        this.gift = gift;
    }

    public List<ItemStack> gift() {
        return this.gift.get();
    }

    // the festival on this date, if any
    public static Festival on(Calendar.Date date) {
        for (Festival f : values()) if (date.is(f.season, f.day)) return f;
        return null;
    }

    public static Festival today(Level level) {
        return on(Calendar.date(level));
    }

    // the next festival from this date on (today counts), and how many days off it is
    public record Upcoming(Festival festival, int inDays) {
    }

    public static Upcoming next(Calendar.Date from) {
        for (int d = 0; d <= Calendar.DAYS_PER_YEAR; d++) {
            Festival f = on(from.plusDays(d));
            if (f != null) return new Upcoming(f, d);
        }
        return null;
    }

    // share off every NPC shop price today (economy/NpcPricing)
    public static double npcDiscount(Level level) {
        Festival f = today(level);
        return f == null ? 0 : f.npcDiscount;
    }
}
