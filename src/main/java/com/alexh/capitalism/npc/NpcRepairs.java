package com.alexh.capitalism.npc;

import com.alexh.capitalism.occupation.Job;
import com.alexh.capitalism.weaponsmith.SmithTable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

// The smith NPCs' Repair tab (NPCs.md): they fix what their smith table fixes (SmithTable.repairs: the Weaponsmith
// weapons, the Toolsmith tools, the Armorsmith armor), straight from your inventory (worn armor too). Each repair gives
// back REPAIR_SHARE of the item's max durability (never past full) and costs gold by its material. It doesn't sharpen
// or un-dent anything (that's the Whetstones), and doesn't touch rolled stats. No stock: as many repairs as you pay for.
public final class NpcRepairs {

    public static final float REPAIR_SHARE = 0.30F;
    private static final int GOLD = 9;

    private NpcRepairs() {
    }

    public static @Nullable SmithTable table(Job job) {
        return switch (job) {
            case WEAPONSMITH -> SmithTable.WEAPONSMITH;
            case TOOLSMITH -> SmithTable.TOOLSMITH;
            case ARMORSMITH -> SmithTable.ARMORSMITH;
            default -> null;
        };
    }

    public static boolean canRepair(Job job, ItemStack stack) {
        SmithTable table = table(job);
        return table != null && !stack.isEmpty() && stack.isDamaged() && table.repairs(stack);
    }

    // Durability one repair gives back
    public static int amount(ItemStack stack) {
        return Math.min(stack.getDamageValue(), Math.max(1, Math.round(stack.getMaxDamage() * REPAIR_SHARE)));
    }

    // FAIR price of one repair, in nuggets, by the item's material (the shop charges NpcPricing.npcPrice of it,
    // so +50%): NPC price wood / leather 2 gold, stone / copper 4, iron / gold / chainmail 10, diamond 20, netherite 50.
    // No material (bow, crossbow...): their own prices below.
    public static long fairNuggets(ItemStack stack) {
        int npcGold;
        if (stack.isValidRepairItem(new ItemStack(Items.NETHERITE_INGOT))) npcGold = 50;
        else if (stack.isValidRepairItem(new ItemStack(Items.DIAMOND))) npcGold = 20;
        else if (stack.isValidRepairItem(new ItemStack(Items.IRON_INGOT))
            || stack.isValidRepairItem(new ItemStack(Items.GOLD_INGOT))) npcGold = 10;
        else if (stack.isValidRepairItem(new ItemStack(Items.COPPER_INGOT))
            || stack.isValidRepairItem(new ItemStack(Items.COBBLESTONE))) npcGold = 4;
        else if (stack.isValidRepairItem(new ItemStack(Items.OAK_PLANKS))
            || stack.isValidRepairItem(new ItemStack(Items.LEATHER))) npcGold = 2;
        else if (stack.is(Items.BOW)) npcGold = 4;
        else if (stack.is(Items.TRIDENT) || stack.is(Items.MACE)) npcGold = 20;
        else npcGold = 10;                                          // crossbow, turtle shell...
        return npcGold * GOLD * 2 / 3;
    }

    // Inventory slots (Inventory.getItem index, worn armor and offhand included) holding something this NPC can repair
    public static List<Integer> slots(Player player, Job job) {
        Inventory inventory = player.getInventory();
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (canRepair(job, inventory.getItem(i))) slots.add(i);
        }
        return slots;
    }
}
