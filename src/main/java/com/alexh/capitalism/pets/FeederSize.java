package com.alexh.capitalism.pets;

import java.util.Locale;

// The three sizes of Food Trough and Water Bowl (PetTrainer.md): Small (copper), Medium (iron), Large (Compacted Iron).
// Small is enough for one pet for a while, Medium for two or three, Large for about ten.
public enum FeederSize {
    SMALL("Small", 1, 1000),
    MEDIUM("Medium", 2, 3000),
    LARGE("Large", 5, 10000);

    public final String title;
    // Food Trough: slots of food
    public final int slots;
    // Water Bowl: how much water it holds, in mL (a Water Bucket pours in 1000, a Water Bottle 250)
    public final int waterMl;

    FeederSize(String title, int slots, int waterMl) {
        this.title = title;
        this.slots = slots;
        this.waterMl = waterMl;
    }

    public String id() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
