package com.alexh.capitalism.weaponsmith;

import com.alexh.capitalism.core.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// Repairing at a smith table: the "dent hunting" game. Weapons at the Weaponsmith Table, tools at the Toolsmith Table,
// armor at the Armorsmith Table (SmithTable.repairs). A damaged one in the big slot + 1 of its
// repair material (whatever an anvil takes: Iron Ingot, Diamond, planks...) in the small slot lights up Forge.
// Glowing dents pop up on the weapon (client/SmithTableScreen); hit them with the hammer before they cool.
// The more damaged the weapon, the harder it is: more dents (4 to 14), coming faster and cooling quicker (dents(),
// spawnEvery(), dentLife()). Hitting them all gives back a full anvil repair (25% of max durability per material),
// each dent hit 1/dents of that.
// Compacted Iron / Compacted Diamond work too, as 2 Iron Ingots / Diamonds: double the repair (50%), double the XP.
// Costs: 1 material, REPAIR_LAVA lava and xpCost(material) levels (Netherite 5, Diamond 2, Iron and everything else 1),
// even if you miss them all. The lava cost is still a placeholder.
public final class DentForging {

    // Barely scratched -> nearly broken
    public static final int MIN_DENTS = 4;
    public static final int MAX_DENTS = 14;
    private static final long SLOW_SPAWN = 600;
    private static final long FAST_SPAWN = 350;
    private static final long SLOW_LIFE = 1250;
    private static final long FAST_LIFE = 720;
    public static final float FULL_REPAIR_SHARE = 0.25F;
    public static final int REPAIR_LAVA = 100;

    // Why the Forge button can or can't be used (synced to the client as its ordinal)
    public enum State {
        NONE,       // not a repair: no button
        READY,
        NEEDS_LAVA,
        NEEDS_XP
    }

    private DentForging() {
    }

    public static boolean isRepair(SmithTable kind, ItemStack weapon, ItemStack material) {
        return kind.repairs(weapon) && weapon.isDamaged()
            && !material.isEmpty() && weapon.isValidRepairItem(plainMaterial(material));
    }

    public static State state(SmithTable kind, ItemStack weapon, ItemStack material, int lava, Player player) {
        if (!isRepair(kind, weapon, material)) return State.NONE;
        if (lava < REPAIR_LAVA) return State.NEEDS_LAVA;
        if (!player.isCreative() && player.experienceLevel < xpCost(material)) return State.NEEDS_XP;
        return State.READY;
    }

    // XP levels a repair costs, by its repair material: Netherite 5, Diamond 2, Iron (and anything else) 1;
    // Compacted Diamond 4, Compacted Iron 2
    // Compacted Iron / Diamond count as 2 Iron Ingots / Diamonds: double the repair, double the XP
    public static int strength(ItemStack material) {
        return material.is(ModItems.COMPACTED_IRON.get()) || material.is(ModItems.COMPACTED_DIAMOND.get()) ? 2 : 1;
    }

    // What the material counts as for "does this repair it?": Compacted Iron = Iron Ingot, Compacted Diamond = Diamond
    private static ItemStack plainMaterial(ItemStack material) {
        if (material.is(ModItems.COMPACTED_IRON.get())) return new ItemStack(Items.IRON_INGOT);
        if (material.is(ModItems.COMPACTED_DIAMOND.get())) return new ItemStack(Items.DIAMOND);
        return material;
    }

    public static int xpCost(ItemStack material) {
        ItemStack plain = plainMaterial(material);
        int levels = plain.is(Items.NETHERITE_INGOT) ? 5 : plain.is(Items.DIAMOND) ? 2 : 1;
        return levels * strength(material);
    }

    // How broken it is: 0 = like new, 1 = about to break
    public static float damageShare(ItemStack weapon) {
        return weapon.getMaxDamage() <= 0 ? 0 : Math.clamp((float) weapon.getDamageValue() / weapon.getMaxDamage(), 0, 1);
    }

    public static int dents(ItemStack weapon) {
        return Math.round(MIN_DENTS + (MAX_DENTS - MIN_DENTS) * damageShare(weapon));
    }

    // Milliseconds between dents popping up
    public static long spawnEvery(ItemStack weapon) {
        return Math.round(SLOW_SPAWN + (FAST_SPAWN - SLOW_SPAWN) * damageShare(weapon));
    }

    // Milliseconds a dent stays hot (clickable)
    public static long dentLife(ItemStack weapon) {
        return Math.round(SLOW_LIFE + (FAST_LIFE - SLOW_LIFE) * damageShare(weapon));
    }

    // Repairs the weapon for this many dents hit, with this material; returns the durability given back
    public static int repair(ItemStack weapon, int hits, ItemStack material) {
        int full = Math.max(1, Math.round(weapon.getMaxDamage() * FULL_REPAIR_SHARE * strength(material)));
        int dents = dents(weapon);
        int fixed = Math.min(weapon.getDamageValue(), Math.round((float) full * Math.clamp(hits, 0, dents) / dents));
        weapon.setDamageValue(weapon.getDamageValue() - fixed);
        return fixed;
    }
}
