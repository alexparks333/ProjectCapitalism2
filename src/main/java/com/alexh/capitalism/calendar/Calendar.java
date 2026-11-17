package com.alexh.capitalism.calendar;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.level.Level;

// The calendar (Calendar.md): days of the week, four seasons of 28 days, years, and the time of day, all read from the
// overworld's clock (Minecraft 26's world clock: one Minecraft day is one calendar day, 20 minutes). Nothing is saved
// or sent: every side works the date out from the same clock, sleeping moves it to the next morning, and the Nether
// and the End show the overworld's date.
//  - Day 0 of a world (clock 0 to 23999) is Monday, Spring 1, Year 1.
//  - The clock's day starts at 6:00 am (clock 0), noon is 6000, 6:00 pm 12000, midnight 18000.
//  - /calendar set moves the clock to a date (keeping the time of day): Calendar.setDate.
public final class Calendar {

    public static final int DAY_TICKS = 24000;
    public static final int DAYS_PER_SEASON = 28;
    public static final int DAYS_PER_WEEK = 7;
    public static final int DAYS_PER_YEAR = DAYS_PER_SEASON * 4;

    private Calendar() {
    }

    public enum Season {
        SPRING("Spring", 0xFFF59AC8),
        SUMMER("Summer", 0xFFFFC23A),
        FALL("Fall", 0xFFE8742A),
        WINTER("Winter", 0xFF9ADCFF);

        public final String title;
        public final int color;

        Season(String title, int color) {
            this.title = title;
            this.color = color;
        }

        public Season next() {
            return values()[(this.ordinal() + 1) % 4];
        }

        public static Season byName(String name) {
            for (Season s : values()) if (s.name().equalsIgnoreCase(name) || s.title.equalsIgnoreCase(name)) return s;
            if (name.equalsIgnoreCase("autumn")) return FALL;
            return null;
        }
    }

    public enum Weekday {
        MONDAY("Mon"), TUESDAY("Tue"), WEDNESDAY("Wed"), THURSDAY("Thu"), FRIDAY("Fri"), SATURDAY("Sat"), SUNDAY("Sun");

        public final String shortName;

        Weekday(String shortName) {
            this.shortName = shortName;
        }

        public String title() {
            return this.name().charAt(0) + this.name().substring(1).toLowerCase();
        }
    }

    // A date: which day since the world began (0 = Spring 1, Year 1), the year (from 1), the season, the day of the
    // season (1 to 28) and the day of the week
    public record Date(long index, int year, Season season, int day, Weekday weekday) {

        public boolean is(Season season, int day) {
            return this.season == season && this.day == day;
        }

        // "Mon. 14"
        public String shortText() {
            return this.weekday.shortName + ". " + this.day;
        }

        // "Monday, Spring 14, Year 1"
        public String longText() {
            return this.weekday.title() + ", " + this.season.title + " " + this.day + ", Year " + this.year;
        }

        public Date plusDays(int days) {
            return of(this.index + days);
        }
    }

    public static Date of(long index) {
        int dayOfYear = (int) Math.floorMod(index, DAYS_PER_YEAR);
        int year = (int) Math.floorDiv(index, DAYS_PER_YEAR) + 1;
        Season season = Season.values()[dayOfYear / DAYS_PER_SEASON];
        int day = dayOfYear % DAYS_PER_SEASON + 1;
        Weekday weekday = Weekday.values()[(int) Math.floorMod(index, DAYS_PER_WEEK)];
        return new Date(index, year, season, day, weekday);
    }

    // which day it is at this clock time
    public static Date date(long clock) {
        return of(Math.floorDiv(clock, DAY_TICKS));
    }

    public static Date date(Level level) {
        return date(clock(level));
    }

    // the overworld's clock (every dimension shows the overworld's date)
    public static long clock(Level level) {
        Holder<WorldClock> overworld = overworldClock(level);
        return overworld == null ? 0L : level.clockManager().getInstance(overworld).totalTicks();
    }

    private static Holder<WorldClock> overworldClock(Level level) {
        return level.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK).get(WorldClocks.OVERWORLD).orElse(null);
    }

    // the clock time for the start of a day (6:00 am) of a season and year
    public static long startOf(int year, Season season, int day) {
        long index = (long) (year - 1) * DAYS_PER_YEAR + (long) season.ordinal() * DAYS_PER_SEASON + (day - 1);
        return index * DAY_TICKS;
    }

    // Moves the overworld's clock to that date, keeping the time of day (/calendar set)
    public static void setDate(ServerLevel level, int year, Season season, int day) {
        long timeOfDay = Math.floorMod(clock(level), DAY_TICKS);
        setClock(level, startOf(year, season, day) + timeOfDay);
    }

    public static void setClock(ServerLevel level, long clock) {
        Holder<WorldClock> overworld = overworldClock(level);
        if (overworld != null) level.getServer().clockManager().setTotalTicks(overworld, clock);
    }

    // ── the time of day ──

    // minutes since midnight (0 to 1439): clock 0 is 6:00 am
    public static int minuteOfDay(long clock) {
        long t = Math.floorMod(clock, DAY_TICKS);
        return (int) ((t * 1440 / DAY_TICKS + 360) % 1440);
    }

    // "9:40 am", in 10-minute steps
    public static String timeText(long clock) {
        int minutes = minuteOfDay(clock) / 10 * 10;
        int hour = minutes / 60;
        int minute = minutes % 60;
        int h12 = hour % 12 == 0 ? 12 : hour % 12;
        return h12 + ":" + (minute < 10 ? "0" : "") + minute + (hour < 12 ? " am" : " pm");
    }

    // how far through the daylight it is, 0 at 6:00 am to 1 at 6:00 pm (outside that, how far through the night,
    // 0 at 6:00 pm to 1 at 6:00 am), and whether it's day
    public static float arc(long clock) {
        long t = Math.floorMod(clock, DAY_TICKS);
        return t < DAY_TICKS / 2 ? t / (DAY_TICKS / 2.0F) : (t - DAY_TICKS / 2) / (DAY_TICKS / 2.0F);
    }

    public static boolean isDay(long clock) {
        return Math.floorMod(clock, DAY_TICKS) < DAY_TICKS / 2;
    }
}
