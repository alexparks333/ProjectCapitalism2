package com.alexh.capitalism.designer;

import com.alexh.capitalism.core.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

// The Designer's pet hats (Designer.md > Hats, PetTrainer.md > Hats): made at the Designer Table (the Hats tab), worn
// by a pet (the pet window's Actions tab, pets/Pets.wearHat). Each helps the pet in its own way (pets/Pets uses them):
//  - Gucci Top Hat: it gets hungry at 70% of the usual speed
//  - Burberry Santa Hat: it gets thirsty at 70% of the usual speed
//  - Louis Vuitton Baseball Cap: starving hurts it half as often (it lasts twice as long)
//  - Prada Fedora: +2 hearts, +2 drumsticks and +2 water drops on top of what it has
//  - Feeding Hat (only from the Traveling Merchant): the pet wearing it is always full and never thirsty
// New hats go at the END (pets save the hat they wear by its place here).
public enum Hat implements Design {
    GUCCI_TOP_HAT("Gucci Top Hat", "Gets hungry slower", ModItems.GUCCI_TOP_HAT,
        List.of(ModItems.GUCCI_PATCH, ModItems.GUCCI_PATCH, ModItems.SILK_PATCH, ModItems.VELVET_PATCH)),
    BURBERRY_SANTA_HAT("Burberry Santa Hat", "Gets thirsty slower", ModItems.BURBERRY_SANTA_HAT,
        List.of(ModItems.BURBERRY_PATCH, ModItems.BURBERRY_PATCH, ModItems.SILK_PATCH, ModItems.VELVET_PATCH)),
    LOUIS_VUITTON_CAP("Louis Vuitton Baseball Cap", "Starves twice as slowly", ModItems.LOUIS_VUITTON_CAP,
        List.of(ModItems.LOUIS_VUITTON_PATCH, ModItems.LOUIS_VUITTON_PATCH, ModItems.EMBROIDERED_PATCH, ModItems.STRAP_PATCH)),
    PRADA_FEDORA("Prada Fedora", "More hearts, food and water", ModItems.PRADA_FEDORA,
        List.of(ModItems.PRADA_PATCH, ModItems.PRADA_PATCH, ModItems.GOLD_TRIM_PATCH, ModItems.VELVET_PATCH)),
    // NOT a Designer's design: only the Traveling Merchant sells it (left out of Design.all). Always full, never thirsty.
    FEEDING_HAT("Feeding Hat", "Never hungry or thirsty", ModItems.FEEDING_HAT, List.of());

    // the Top Hat / Santa Hat: hunger / thirst drop at this share of the usual speed
    public static final float SLOWER = 0.7F;
    // the Fedora: this much more on each bar (2 drumsticks / drops) and this much more health (2 hearts)
    public static final float FEDORA_BARS = 4F;
    public static final double FEDORA_HEALTH = 4.0;

    public final String title;
    // what it does, in a few words (tooltips, the Designer Table)
    public final String does;
    private final Supplier<? extends Item> item;
    private final List<Supplier<? extends Item>> patches;

    Hat(String title, String does, Supplier<? extends Item> item, List<Supplier<? extends Item>> patches) {
        this.title = title;
        this.does = does;
        this.item = item;
        this.patches = patches;
    }

    @Override
    public String title() {
        return this.title;
    }

    @Override
    public Line line() {
        return Line.HATS;
    }

    @Override
    public Item item() {
        return this.item.get();
    }

    @Override
    public List<Item> patches() {
        return this.patches.stream().map(p -> (Item) p.get()).toList();
    }

    public static @Nullable Hat of(ItemStack stack) {
        for (Hat hat : values()) {
            if (stack.is(hat.item())) return hat;
        }
        return null;
    }

    public static @Nullable Hat byIndex(int index) {
        return index < 0 || index >= values().length ? null : values()[index];
    }
}
