package com.alexh.capitalism.enchanter;

import com.alexh.capitalism.economy.GoldMoney;
import com.alexh.capitalism.economy.NpcPricing;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

// The Enchanter NPC's enchanting (Enchanter.md > The Enchanter NPC): while no Enchanter is online, the stand-in
// Enchanter is an Enchanting Table you pay. Right-click him: the real enchanting screen, as if 15 bookshelves stood
// round it (options up to level 30), no level-10 cap. No lapis needed (the lapis slot shows some you can't take); an
// option costs its levels as at a table, plus gold by how high it is: under 15 10 gold, 15 to 29 20 gold, 30 50 gold
// (NPC prices, through NpcPricing like every NPC shop, so Emerald armor and sale days take some off). The menu hooks are
// in mixin/EnchantmentMenuMixin; this is the rest.
public final class NpcEnchanting {

    // its screen's title, also how the client tells it from a real table (mixin/EnchantmentScreenMixin dresses it as a shop)
    public static final String TITLE = "Enchanter's Shop";

    // the fair (player) prices, in nuggets; the NPC charges 1.5x: 10, 20, 50 gold
    public static final long FAIR_LOW = 60;
    public static final long FAIR_MID = 120;
    public static final long FAIR_TOP = 300;

    // what a mixed-in EnchantmentMenu knows about being the NPC's
    public interface Menu {
        void capitalism$setNpc(Entity npc);
    }

    private NpcEnchanting() {
    }

    public static void open(ServerPlayer player, Entity npc) {
        player.openMenu(new SimpleMenuProvider((id, inventory, who) -> {
            EnchantmentMenu menu = new EnchantmentMenu(id, inventory, ContainerLevelAccess.create(npc.level(), npc.blockPosition()));
            ((Menu) menu).capitalism$setNpc(npc);
            return menu;
        }, Component.literal(TITLE)));
    }

    // gold (NPC price, nuggets) for an option of this level
    public static long price(int optionLevel, Player player) {
        long fair = optionLevel >= 30 ? FAIR_TOP : optionLevel >= 15 ? FAIR_MID : FAIR_LOW;
        return NpcPricing.npcPrice(fair, player);
    }

    // when each player was last told they're short (so clicking again and again doesn't fill chat)
    private static final java.util.Map<java.util.UUID, Long> LAST_NAG = new java.util.HashMap<>();

    public static boolean canPay(Player player, long price) {
        if (GoldMoney.carried(player) >= price) return true;
        long now = player.level().getGameTime();
        Long last = LAST_NAG.get(player.getUUID());
        if (last != null && now - last >= 0 && now - last < 100) return false;
        LAST_NAG.put(player.getUUID(), now);
        player.sendSystemMessage(Component.literal("[Enchanter] ").withStyle(ChatFormatting.GOLD)
            .append(Component.literal("That one's " + GoldMoney.format(price) + ". Come back with the gold.").withStyle(ChatFormatting.WHITE)));
        return false;
    }

    public static void paid(Player player, long price) {
        GoldMoney.take(player, price);
        player.sendSystemMessage(Component.literal("[Enchanter] ").withStyle(ChatFormatting.GOLD)
            .append(Component.literal("Enchanted, for " + GoldMoney.format(price) + ".").withStyle(ChatFormatting.WHITE)));
    }

    // the lapis slot: shows lapis, but you can't take it or put anything in
    public static final class LockedSlot extends Slot {
        public LockedSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }
}
