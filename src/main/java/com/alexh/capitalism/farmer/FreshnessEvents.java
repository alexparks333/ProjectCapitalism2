package com.alexh.capitalism.farmer;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.ModComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

// Keeps meat's timers (Freshness) up to date: every second, everything in a player's inventory, what they hold on the
// cursor and the container they have open (a chest, say) gets its timer if it had none, or turns to Rotten Meat if its
// time is up; the same for meat lying on the ground; and the moment a container is opened (so a chest nobody looked
// at for a while shows what rotted in it).
@EventBusSubscriber(modid = Capitalism.MODID)
public final class FreshnessEvents {

    private FreshnessEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 20 == 0) update(player);
    }

    @SubscribeEvent
    public static void onOpen(PlayerContainerEvent.Open event) {
        if (event.getEntity() instanceof ServerPlayer player) update(player);
    }

    @SubscribeEvent
    public static void onItemTick(EntityTickEvent.Post event) {
        var entity = event.getEntity();
        if (entity.tickCount % 20 != 0 || entity.level().isClientSide()) return;
        long time = entity.level().getGameTime();
        // meat in an item frame
        if (entity instanceof net.minecraft.world.entity.decoration.ItemFrame frame) {
            ItemStack shown = frame.getItem();
            ItemStack updated = Freshness.update(shown, time, frame.getRandom());
            if (updated != null && updated != shown) frame.setItem(updated);
            return;
        }
        // meat held or worn by anything but a player (armor stands, allays, foxes...: players are done above)
        if (entity instanceof net.minecraft.world.entity.LivingEntity living && !(living instanceof net.minecraft.world.entity.player.Player)) {
            for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
                ItemStack held = living.getItemBySlot(slot);
                if (held.isEmpty()) continue;
                ItemStack updated = Freshness.update(held, time, living.getRandom());
                if (updated != null && updated != held) living.setItemSlot(slot, updated);
            }
            return;
        }
        if (!(entity instanceof ItemEntity item)) return;
        ItemStack held = item.getItem();
        if (held.has(ModComponents.CHILL.get())) {
            held = held.copy();
            held.remove(ModComponents.CHILL.get());
            item.setItem(held);
        }
        ItemStack now = Freshness.update(held, item.level().getGameTime(), item.getRandom());
        if (now != null) item.setItem(now == held ? now.copy() : now);
    }

    // Meat in a cooling bag: its time runs down `slower` times slower from when it went in (Chill: since, left), so it's
    // exact however long it's been (the player offline too). Each update moves its timer on to match.
    private static boolean chillInBag(ItemStack stack, int slower, long now) {
        Chill chill = stack.get(ModComponents.CHILL.get());
        long left = chill != null && chill.slower() == slower ? chill.leftAtNormalRate(now) : Freshness.rotsAt(stack) - now;
        stack.set(ModComponents.ROTS_AT.get(), now + left);
        stack.set(ModComponents.CHILL.get(), new Chill(slower, now, left));
        return true;
    }

    // The player's inventory, the cursor, the worn bag, and the open container
    public static void update(ServerPlayer player) {
        long now = player.level().getGameTime();
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            stack.remove(ModComponents.CHILL.get());
            ItemStack updated = Freshness.update(stack, now, player.getRandom());
            if (updated != null && updated != stack) inventory.setItem(i, updated);
        }
        // the worn bag (its slots are on the inventory menu); in the Freezer Sling meat keeps 6x as long, no fuel
        com.alexh.capitalism.designer.Bag bag = com.alexh.capitalism.designer.Bags.worn(player);
        if (bag != null) {
            for (Slot slot : player.inventoryMenu.slots) {
                if (!(slot instanceof com.alexh.capitalism.designer.BagSlot) || !slot.isActive()) continue;
                ItemStack stack = slot.getItem();
                if (stack.isEmpty() || !Freshness.rots(stack)) continue;
                boolean changed = Freshness.stamp(stack, now, player.getRandom());
                if (bag.slower() > 1 && Freshness.rotsAt(stack) >= 0) changed |= chillInBag(stack, bag.slower(), now);
                if (Freshness.isRotten(stack, now)) slot.set(Freshness.rotted(stack));
                else if (changed) slot.setChanged();
            }
        }
        AbstractContainerMenu menu = player.containerMenu;
        ItemStack carried = menu.getCarried();
        carried.remove(ModComponents.CHILL.get());
        ItemStack updated = Freshness.update(carried, now, player.getRandom());
        if (updated != null && updated != carried) menu.setCarried(updated);
        if (menu != player.inventoryMenu) {
            for (Slot slot : menu.slots) {
                if (slot.container == inventory) continue;
                ItemStack stack = slot.getItem();
                if (!(slot.container instanceof ColdStorageBlockEntity) && stack.has(ModComponents.CHILL.get())) {
                    stack.remove(ModComponents.CHILL.get());
                    slot.setChanged();
                }
                ItemStack fresh = Freshness.update(stack, now, player.getRandom());
                if (fresh != null && fresh != stack) slot.set(fresh);
                else if (fresh != null) slot.setChanged();
            }
        }
    }
}
