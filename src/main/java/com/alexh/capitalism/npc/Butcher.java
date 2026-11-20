package com.alexh.capitalism.npc;

import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.farmer.MeatCut;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// The Butcher (NPCs.md > The Butcher): a town Shopkeeper an op puts with /npc setspot butcher. He comes to town on
// NpcManager.DAILY_CHANCE of days (35%, like a job's NPC; NpcManager.shopkeeperInTown) and sells 4 random raw fine cuts
// that day (todaysCuts; farmer/MeatCut, NpcShops.BUTCHER), so a Cook can sometimes make the meals that need one when no
// Farmer is around (a Cook can't be a Farmer). Dearer than a Farmer, little stock. Raw cuts give no effects, so the NPC
// rule holds. They start rotting when bought (farmer/Freshness stamps them). Looks like a villager in the butcher's
// apron and a deep red cap, holding a Beef Rib.
public class Butcher extends Shopkeeper {

    public static final String TITLE = "Butcher";

    public Butcher(EntityType<? extends JobNpc> type, Level level) {
        super(type, level);
    }

    @Override
    public String shopKey() {
        return NpcShops.BUTCHER;
    }

    // Today's NpcShops.CUTS_A_DAY random cuts, as a mask (bit i = MeatCut i): the same all day, new ones tomorrow
    public static int todaysCuts(net.minecraft.server.MinecraftServer server) {
        return cutsFor(NpcManager.dailySeed(server, "butcher_cuts"));
    }

    public static int cutsFor(long seed) {
        java.util.List<MeatCut> cuts = new java.util.ArrayList<>(java.util.List.of(MeatCut.values()));
        java.util.Collections.shuffle(cuts, new java.util.Random(seed));
        int mask = 0;
        for (int i = 0; i < NpcShops.CUTS_A_DAY; i++) mask |= 1 << cuts.get(i).ordinal();
        return mask;
    }

    @Override
    protected String menuKey(net.minecraft.server.MinecraftServer server) {
        return NpcShops.butcherKey(todaysCuts(server));
    }

    @Override
    public String title() {
        return TITLE;
    }

    @Override
    protected String shopName() {
        return "The Butcher's Counter";
    }

    @Override
    protected ChatFormatting nameColor() {
        return ChatFormatting.RED;
    }

    @Override
    protected ItemStack held() {
        return new ItemStack(ModItems.MEAT_CUTS.get(MeatCut.BEEF_RIB).get());
    }
}
