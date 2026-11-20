package com.alexh.capitalism.npc;

import com.alexh.capitalism.core.ModMenus;
import com.alexh.capitalism.economy.GoldMoney;
import com.alexh.capitalism.economy.NpcPricing;
import com.alexh.capitalism.occupation.Job;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.List;

// An NPC's shop (NpcShops, by shop key: a job's id or a Shopkeeper's): no slots, just the offers (and for smiths, repairs of what you carry: repair()). The client asks to buy with a menu button
// (offer index * 2, +1 = buy up to BULK at once); the server checks the NPC is still there and close and that there's
// stock left (NpcStock, shared by everyone), takes the gold (GoldMoney.take makes change) and hands over the items.
// Synced to the client: how many of each offer are left, then the seconds until the restock.
public class NpcShopMenu extends AbstractContainerMenu {

    public static final int BULK = 4;
    // Repair tab buttons: REPAIR_BUTTON + the inventory slot of what to repair (NpcRepairs)
    public static final int REPAIR_BUTTON = 1000;

    private final String shop;
    // the job, for a job NPC's shop (null for a Shopkeeper: the Boat Master, the Butcher)
    private final @Nullable Job job;
    private final @Nullable JobNpc npc;
    private final ContainerData data;

    public NpcShopMenu(int containerId, Inventory inventory, String shop, @Nullable JobNpc npc) {
        super(ModMenus.NPC_SHOP.get(), containerId);
        this.shop = shop;
        this.job = Job.byId(shop);
        this.npc = npc;
        int offers = NpcShops.all(shop).size();
        MinecraftServer server = npc == null ? null : npc.level().getServer();
        this.data = server == null ? new SimpleContainerData(offers + 1) : new ContainerData() {
            @Override
            public int get(int index) {
                NpcStock stock = NpcStock.get(server);
                return index < offers ? stock.left(shop, index) : (int) (stock.ticksUntilRestock(server) / 20);
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return offers + 1;
            }
        };
        this.addDataSlots(this.data);
    }

    public String shop() {
        return this.shop;
    }

    public @Nullable Job job() {
        return this.job;
    }

    public List<NpcShops.Offer> offers() {
        return NpcShops.all(this.shop);
    }

    // Client: how many of this offer are left (NpcStock.UNLIMITED = no limit)
    public int left(int index) {
        return this.data.get(index);
    }

    public int secondsUntilRestock() {
        return this.data.get(this.offers().size());
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= REPAIR_BUTTON) return this.repair(player, id - REPAIR_BUTTON);
        List<NpcShops.Offer> offers = this.offers();
        int index = id / 2;
        if (index < 0 || index >= offers.size()) return false;
        if (!(player instanceof ServerPlayer buyer) || buyer.level().getServer() == null) return true;
        MinecraftServer server = buyer.level().getServer();
        NpcStock stock = NpcStock.get(server);
        NpcShops.Offer offer = offers.get(index);
        int left = stock.left(this.shop, index);
        if (left <= 0) {
            buyer.sendOverlayMessage(Component.literal(offer.forever() ? "Sold out for good! Only " + offer.maxStock() + " were ever made."
                : "Sold out! More in " + clock((int) (stock.ticksUntilRestock(server) / 20)) + "."));
            return false;
        }
        int count = Math.min(id % 2 == 1 ? BULK : 1, left);
        long price = NpcPricing.npcPrice(offer.fairNuggets(), buyer) * count;
        ItemStack bought = offer.stack();
        bought.setCount(count * offer.per());
        if (!GoldMoney.take(buyer, price)) {
            buyer.sendOverlayMessage(Component.literal("You need " + GoldMoney.format(price) + " for that ("
                + GoldMoney.format(GoldMoney.carried(buyer)) + " on you)."));
            return false;
        }
        stock.take(this.shop, index, count);
        buyer.getInventory().placeItemBackInInventory(bought, Prediction.SERVER_ONLY);
        buyer.inventoryMenu.broadcastChanges();
        buyer.level().playSound(null, buyer.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6F, 1.4F);
        buyer.sendOverlayMessage(Component.literal("Bought " + bought.getCount() + "x " + bought.getHoverName().getString()
            + " for " + GoldMoney.format(price) + "."));
        return true;
    }

    // One repair (NpcRepairs.REPAIR_SHARE of its durability) of the item in this inventory slot, for gold
    private boolean repair(Player player, int slot) {
        if (!(player instanceof ServerPlayer buyer)) return true;
        if (slot < 0 || slot >= buyer.getInventory().getContainerSize()) return false;
        ItemStack stack = buyer.getInventory().getItem(slot);
        if (this.job == null || !NpcRepairs.canRepair(this.job, stack)) return false;
        long price = NpcPricing.npcPrice(NpcRepairs.fairNuggets(stack), buyer);
        if (!GoldMoney.take(buyer, price)) {
            buyer.sendOverlayMessage(Component.literal("You need " + GoldMoney.format(price) + " for that ("
                + GoldMoney.format(GoldMoney.carried(buyer)) + " on you)."));
            return false;
        }
        int amount = NpcRepairs.amount(stack);
        stack.setDamageValue(stack.getDamageValue() - amount);
        // this menu has no slots, so the inventory (and the gold paid) is synced by the player's own menu
        buyer.inventoryMenu.broadcastChanges();
        com.alexh.capitalism.bounty.BountyEvents.onNpcRepair(buyer);
        buyer.level().playSound(null, buyer.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.6F, 1.1F);
        buyer.sendOverlayMessage(Component.literal("Repaired " + stack.getHoverName().getString() + ": +" + amount
            + " durability for " + GoldMoney.format(price) + "."));
        return true;
    }

    // 754 -> "12:34"
    public static String clock(int seconds) {
        return seconds / 60 + ":" + String.format(java.util.Locale.ROOT, "%02d", seconds % 60);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    // Only while the NPC is still standing there (and still the one for its job), and you're close to it
    @Override
    public boolean stillValid(Player player) {
        if (this.npc == null) return true;
        return this.npc.isAlive() && player.distanceToSqr(this.npc) <= 64.0 && this.npc.stillWanted();
    }
}
