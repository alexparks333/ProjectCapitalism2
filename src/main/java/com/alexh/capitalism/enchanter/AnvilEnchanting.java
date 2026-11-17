package com.alexh.capitalism.enchanter;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.occupation.Job;
import com.alexh.capitalism.occupation.JobEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

// Merging enchantments at an anvil is split between two jobs: an Enchanted Book on an Enchanted Book is the
// Librarian's (Librarian.md), anything else enchanted in the right slot (a book on an item, an enchanted item on
// another) is the Enchanter's (Enchanter.md). Anyone else gets nothing. Renaming still works for everyone; repairs
// don't work at all (weaponsmith/WeaponRepairs). Creative isn't limited.
@EventBusSubscriber(modid = Capitalism.MODID)
public final class AnvilEnchanting {

    private AnvilEnchanting() {
    }

    @SubscribeEvent
    public static void onAnvil(AnvilUpdateEvent event) {
        ItemStack right = event.getRight();
        if (right.isEmpty() || EnchantmentHelper.getEnchantmentsForCrafting(right).isEmpty()) return;
        Player player = event.getPlayer();
        boolean books = event.getLeft().is(Items.ENCHANTED_BOOK) && right.is(Items.ENCHANTED_BOOK);
        Job job = books ? Job.LIBRARIAN : Job.ENCHANTER;
        if (player == null || player.isCreative() || JobEvents.has(player, job)) return;
        event.setCanceled(true);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendOverlayMessage(Component.literal(books ? "Only a Librarian can combine Enchanted Books."
                : "Only an Enchanter can put enchantments on gear."));
        }
    }
}
