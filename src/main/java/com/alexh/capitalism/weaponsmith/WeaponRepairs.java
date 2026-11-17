package com.alexh.capitalism.weaponsmith;

import com.alexh.capitalism.Capitalism;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.GrindstoneEvent;

// Repairs only happen at the smith tables (the dent game, DentForging): weapons at the Weaponsmith Table, tools at the
// Toolsmith Table, armor at the Armorsmith Table. Every other way to get durability back is off, for everyone:
//  - Anvil: no repairs with a material. Combining two items still merges their enchantments (that's for the
//    Enchanter, enchanter/AnvilEnchanting: only they can), but the result keeps the left item's durability; if combining would only repair, it's blocked.
//    Enchanted books and renaming work as normal.
//  - Grindstone: combining two items (which repairs them) is off. Removing enchantments from one item still works.
//  - Crafting grid (two damaged items together): off (JobRepairItemRecipe).
@EventBusSubscriber(modid = Capitalism.MODID)
public final class WeaponRepairs {

    private static final String NO_REPAIRS = "Repairs are done at a smith table.";

    private WeaponRepairs() {
    }

    @SubscribeEvent
    public static void onAnvil(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        ItemStack out = event.getOutput();
        if (!left.isDamageableItem() || out.isEmpty() || out.getDamageValue() >= left.getDamageValue()) return;
        // the anvil would give durability back
        if (!right.isEmpty() && left.isValidRepairItem(right)) {
            block(event);
            return;
        }
        // two of the same item: keep the enchantment merge, drop the repair
        out.setDamageValue(left.getDamageValue());
        if (ItemStack.isSameItemSameComponents(out, left)) block(event);
    }

    private static void block(AnvilUpdateEvent event) {
        event.setCanceled(true);
        if (event.getPlayer() instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendOverlayMessage(Component.literal(NO_REPAIRS));
        }
    }

    @SubscribeEvent
    public static void onGrindstone(GrindstoneEvent.OnPlaceItem event) {
        if (!event.getTopItem().isEmpty() && !event.getBottomItem().isEmpty()) event.setCanceled(true);
    }
}
