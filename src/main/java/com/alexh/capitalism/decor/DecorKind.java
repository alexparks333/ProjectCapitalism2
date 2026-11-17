package com.alexh.capitalism.decor;

import com.alexh.capitalism.calendar.Calendar;
import com.alexh.capitalism.calendar.Calendar.Season;

// The seasonal decorations (Decorations.md) and when each is up: a holiday's decorations go up by themselves on the
// first day of their stretch of the calendar and are packed away after the last (decor/SeasonalDecorBlock), unless an
// operator has put every decoration up or down by hand (/decorations).
public enum DecorKind {

    // Christmas: Winter 5 to 28
    CHRISTMAS_TREE("Grand Christmas Tree", Holiday.CHRISTMAS),
    STRING_LIGHTS("String Light Hook", Holiday.CHRISTMAS),
    PRESENT_PILE("Present Pile", Holiday.CHRISTMAS),
    CANDY_CANE_POLE("Candy Cane Pole", Holiday.CHRISTMAS),
    CANDY_CANES("Candy Canes", Holiday.CHRISTMAS),
    CHRISTMAS_FROST("Christmas Frost", Holiday.CHRISTMAS),
    ICICLES("Icicles", Holiday.CHRISTMAS),
    // Halloween: the second half of fall, up to Spooky Night (Fall 28)
    SCARECROW("Spooky Scarecrow", Holiday.HALLOWEEN),
    WITCHS_CAULDRON("Witch's Cauldron", Holiday.HALLOWEEN),
    GRAVESTONE_ROUND("Rounded Gravestone", Holiday.HALLOWEEN),
    GRAVESTONE_CROSS("Cross Gravestone", Holiday.HALLOWEEN),
    GRAVESTONE_TOMB("Old Tombstone", Holiday.HALLOWEEN),
    BONE_PILE("Pile of Bones", Holiday.HALLOWEEN),
    GROUND_FOG("Ground Fog", Holiday.HALLOWEEN),
    // the harvest: all fall
    PUMPKIN_PILE("Big Pumpkin Pile", Holiday.HARVEST),
    LEAF_PILE("Leaf Pile", Holiday.HARVEST),
    AUTUMN_MAPLE("Autumn Maple", Holiday.HARVEST),
    AUTUMN_BIRCH("Autumn Birch", Holiday.HARVEST),
    FALLEN_LEAVES("Fallen Leaves", Holiday.HARVEST),
    // summer: all summer
    BEACH_TOWEL("Beach Towel", Holiday.SUMMER),
    BEACH_BALL("Beach Ball", Holiday.SUMMER),
    BEACH_UMBRELLA("Beach Umbrella", Holiday.SUMMER),
    BEACH_CHAIR("Beach Chair", Holiday.SUMMER),
    // spring: all spring
    EGG_BASKET("Egg Basket", Holiday.SPRING),
    GIANT_EGG("Giant Painted Egg", Holiday.SPRING),
    SPRING_BUNNY("Spring Bunny", Holiday.SPRING),
    TULIP_BED("Tulip Bed", Holiday.SPRING);

    public final String title;
    public final Holiday holiday;

    DecorKind(String title, Holiday holiday) {
        this.title = title;
        this.holiday = holiday;
    }

    // String lights can hang from it (a String Light Hook, a Candy Cane Pole), and how high their strands hang from on its
    // block (blocks above its foot); -1 if not
    public float hookHeight() {
        return switch (this) {
            case STRING_LIGHTS -> 0.47F;
            case CANDY_CANE_POLE -> 3.85F;
            default -> -1.0F;
        };
    }

    public boolean isHook() {
        return this.hookHeight() >= 0;
    }

    // drawn as an ordinary block model (its blockstate: nothing when packed away), not by client/DecorRenderer: the
    // small things you place lots of. Nothing to bump into even when up.
    // nothing to bump into even when it's up (the fog)
    public boolean passable() {
        return this.blockModel() || this == GROUND_FOG || this == BEACH_TOWEL || this == TULIP_BED;
    }

    public boolean blockModel() {
        return this == CHRISTMAS_FROST || this == ICICLES || this == FALLEN_LEAVES;
    }

    public enum Holiday {
        CHRISTMAS("Christmas", Season.WINTER, 5, 28),
        HALLOWEEN("Halloween", Season.FALL, 18, 28),
        HARVEST("the harvest", Season.FALL, 1, 28),
        SUMMER("summer", Season.SUMMER, 1, 28),
        SPRING("spring", Season.SPRING, 1, 28);

        public final String title;
        public final Season season;
        public final int firstDay;
        public final int lastDay;

        Holiday(String title, Season season, int firstDay, int lastDay) {
            this.title = title;
            this.season = season;
            this.firstDay = firstDay;
            this.lastDay = lastDay;
        }

        public boolean on(Calendar.Date date) {
            return date.season() == this.season && date.day() >= this.firstDay && date.day() <= this.lastDay;
        }

        // "Winter 1 to 28"
        public String window() {
            return this.season.title + " " + this.firstDay + " to " + this.lastDay;
        }
    }
}
