package com.alexh.capitalism.designer;

import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

// Something the Designer Table makes (Designer.md > Designer Table): a bag (Bag) or a pet hat (Hat), with its tab (Line)
// and the row of Patches it takes (plus one Leather).
public interface Design {

    // The Designer Table's tabs
    enum Line {
        STANDARD("Standard Bags"),
        FANCY("Fancy Bags"),
        LUXURY("Luxury Bags"),
        HATS("Hats"),
        UTILITY("Utility Bags");

        public final String title;

        Line(String title) {
            this.title = title;
        }
    }

    String title();

    Line line();

    Item item();

    List<Item> patches();

    // Every design in the order the table's choice is saved and sent in: all the bags, then all the hats
    static List<Design> all() {
        // (the Traveler's Bag isn't a design: only the Traveling Merchant sells it)
        List<Design> all = new ArrayList<>(java.util.Arrays.stream(Bag.values()).filter(b -> b != Bag.TRAVELERS_BAG).toList());
        all.addAll(java.util.Arrays.stream(Hat.values()).filter(h -> h != Hat.FEEDING_HAT).toList());
        return all;
    }

    static Design byIndex(int index) {
        List<Design> all = all();
        return all.get(Math.floorMod(index, all.size()));
    }

    static List<Design> inLine(Line line) {
        return all().stream().filter(d -> d.line() == line).toList();
    }
}
