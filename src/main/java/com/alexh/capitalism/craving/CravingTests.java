package com.alexh.capitalism.craving;

import com.alexh.capitalism.bounty.Standings;
import com.alexh.capitalism.calendar.Calendar;
import com.alexh.capitalism.core.ModAttachments;
import com.alexh.capitalism.core.ModComponents;
import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.farmer.MeatCut;
import com.alexh.capitalism.farmer.Sear;
import com.alexh.capitalism.gametest.GameTests;
import com.alexh.capitalism.player.ThirstData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashSet;
import java.util.Set;

// Cravings (Cravings.md): every craving is a real item; a pair per player per day; today's food fills the food bar and
// every golden drumstick, today's drink the water bar and every golden drop, and the item's own effects still apply;
// a seared cut must be at the exact doneness; once per craving per day; the standings count them.
public final class CravingTests {

    static {
        GameTests.register("cravings", CravingTests::cravings);
    }

    private CravingTests() {
    }

    public static void init() {
    }

    private static void cravings(GameTests.Checks c) {
        ServerLevel level = c.level();
        ServerPlayer alex = c.player(), sam = c.player();

        // ── the pools: real items, mostly job-made, a plain way out in each ──
        boolean real = Cravings.foods().stream().allMatch(f -> BuiltInRegistries.ITEM.getValue(f.item()) != Items.AIR)
            && Cravings.drinks().stream().allMatch(d -> BuiltInRegistries.ITEM.getValue(d.item()) != Items.AIR);
        long jobFoods = Cravings.foods().stream().filter(f -> !f.plain()).count(), jobDrinks = Cravings.drinks().stream().filter(d -> !d.plain()).count();
        c.check(real && jobFoods > 30 && jobDrinks > 15 && Cravings.foods().stream().anyMatch(Cravings.Craving::plain)
            && Cravings.drinks().stream().anyMatch(d -> d.item().equals(BuiltInRegistries.ITEM.getKey(Items.MILK_BUCKET))),
            "every craving is a real item: " + jobFoods + " job foods and " + jobDrinks + " job drinks, plus plain ones (milk...)");
        c.check(Cravings.foods().stream().noneMatch(f -> f.doneness() == Sear.Doneness.RAW.ordinal()), "never a raw steak (nor underdone chicken)");

        // ── a pair per player per day ──
        // (test players start with no cravings, GameTests.player: roll theirs)
        Cravings.of(alex).day = -1;
        Cravings.of(sam).day = -1;
        Cravings.ensure(alex);
        CravingData first = Cravings.of(alex);
        String food = first.food(), drink = first.drink();
        Cravings.ensure(alex);
        c.check(Cravings.of(alex).food().equals(food) && Cravings.of(alex).drink().equals(drink), "today's pair stays the same all day");
        Set<String> alexDays = new HashSet<>(), samDays = new HashSet<>();
        boolean differ = false;
        long start = Calendar.clock(level);
        for (int d = 0; d < 8; d++) {
            Calendar.setClock(level, start + (long) d * Calendar.DAY_TICKS);
            Cravings.ensure(alex);
            Cravings.ensure(sam);
            alexDays.add(Cravings.of(alex).food() + Cravings.of(alex).foodDone() + Cravings.of(alex).drink());
            samDays.add(Cravings.of(sam).food() + Cravings.of(sam).drink());
            differ |= !(Cravings.of(alex).food() + Cravings.of(alex).drink()).equals(Cravings.of(sam).food() + Cravings.of(sam).drink());
        }
        Calendar.setClock(level, start);
        c.check(alexDays.size() > 4 && differ, "a new pair each morning, and each player their own (" + alexDays.size() + " different in 8 days)");

        // ── the food craving fills the food bar and all the gold; once a day ──
        Cravings.set(alex, BuiltInRegistries.ITEM.getKey(Items.BREAD).toString(), -1, BuiltInRegistries.ITEM.getKey(ModItems.MINERS_MOONSHINE.get()).toString());
        alex.getFoodData().setFoodLevel(0);
        ThirstData thirst = alex.getData(ModAttachments.THIRST.get());
        c.consume(alex, new ItemStack(Items.BREAD));
        c.check(alex.getFoodData().getFoodLevel() == thirst.foodCap && thirst.goldFood() == thirst.goldFoodMax() && Cravings.of(alex).ate(),
            "the craved food fills the food bar and every golden drumstick, from empty");
        int total = Cravings.of(alex).total();
        alex.getFoodData().setFoodLevel(2);
        c.consume(alex, new ItemStack(Items.BREAD));
        c.check(Cravings.of(alex).total() == total && alex.getFoodData().getFoodLevel() < thirst.foodCap, "a second one the same day is just bread (no point, no full bar)");

        // ── the drink craving fills the water bar and all the gold drops, and its own effects still apply ──
        thirst.drain(40.0F);
        c.consume(alex, new ItemStack(ModItems.MINERS_MOONSHINE.get()));
        c.check(thirst.getThirstF() >= thirst.thirstCap && thirst.goldThirst() >= thirst.goldThirstMax() && Cravings.of(alex).drank(),
            "the craved drink fills the water bar and every golden drop");
        c.check(alex.hasEffect(MobEffects.HASTE), "and its usual effects still apply (Miner's Moonshine: Haste)");
        c.check(Cravings.of(alex).total() == total + 1, "1 point per craving, once each per day (" + Cravings.of(alex).total() + ")");

        // ── a seared cut must be at the exact doneness ──
        String rib = BuiltInRegistries.ITEM.getKey(ModItems.SEARED_CUTS.get(MeatCut.BEEF_RIB).get()).toString();
        Cravings.set(sam, rib, Sear.Doneness.MEDIUM_WELL.ordinal(), "minecraft:milk_bucket");
        ItemStack mediumRare = new ItemStack(ModItems.SEARED_CUTS.get(MeatCut.BEEF_RIB).get());
        mediumRare.set(ModComponents.SEAR.get(), new Sear(Sear.Doneness.MEDIUM_RARE.ordinal(), 0));
        ItemStack mediumWell = mediumRare.copy();
        mediumWell.set(ModComponents.SEAR.get(), new Sear(Sear.Doneness.MEDIUM_WELL.ordinal(), 0));
        c.check(!Cravings.matchesFood(Cravings.of(sam), mediumRare) && Cravings.matchesFood(Cravings.of(sam), mediumWell),
            "a Medium Well Seared Beef Rib craving takes exactly Medium Well (not Medium Rare)");
        c.check(Cravings.name(rib, Sear.Doneness.MEDIUM_WELL.ordinal()).getString().startsWith("Medium Well "), "its name says the doneness");

        // ── the standings ──
        var ranked = Standings.get(level.getServer()).ranking(Standings.Category.CRAVINGS);
        c.check(ranked.stream().anyMatch(e -> e.id().equals(alex.getUUID()) && e.score() == Cravings.of(alex).total()), "the \"Cravings satisfied\" standings count them");
    }
}
