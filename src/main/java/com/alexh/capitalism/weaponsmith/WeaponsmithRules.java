package com.alexh.capitalism.weaponsmith;

import com.alexh.capitalism.occupation.Job;
import com.alexh.capitalism.occupation.JobEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// Weaponsmith-only things that aren't recipes:
//  - Smithing Table (called from mixin/SmithingMenuMixin): Netherite gear is job-only. Netherite Sword: Weaponsmith.
//    Netherite Pickaxe / Shovel / Hoe: Toolsmith. Netherite Axe: either. (Netherite armor: anyone, for now.)
// The crafting ones (Crossbow, iron/diamond swords and axes from plain materials, 4 arrows) are "capitalism:job_shaped" recipes
// that replace the vanilla ones in data/minecraft/recipe/.
public final class WeaponsmithRules {

    private WeaponsmithRules() {
    }

    public static void checkSmithing(Player player, ResultContainer result) {
        ItemStack made = result.getItem(0);
        String message;
        boolean allowed;
        if (made.is(Items.NETHERITE_SWORD)) {
            allowed = has(player, Job.WEAPONSMITH);
            message = "Only Weaponsmiths can make Netherite Swords.";
        } else if (made.is(Items.NETHERITE_AXE)) {
            allowed = has(player, Job.WEAPONSMITH) || has(player, Job.TOOLSMITH);
            message = "Only Weaponsmiths and Toolsmiths can make Netherite Axes.";
        } else if (made.is(Items.NETHERITE_PICKAXE) || made.is(Items.NETHERITE_SHOVEL) || made.is(Items.NETHERITE_HOE)) {
            allowed = has(player, Job.TOOLSMITH);
            message = "Only Toolsmiths can make Netherite Tools.";
        } else {
            return;
        }
        if (allowed) return;
        result.setItem(0, ItemStack.EMPTY);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendOverlayMessage(Component.literal(message));
        }
    }

    private static boolean has(Player player, Job job) {
        return player.isCreative() || JobEvents.has(player, job);
    }
}
