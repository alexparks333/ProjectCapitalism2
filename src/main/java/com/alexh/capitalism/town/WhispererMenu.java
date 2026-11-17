package com.alexh.capitalism.town;

import com.alexh.capitalism.core.ModMenus;
import com.alexh.capitalism.librarian.KnowledgeQuality;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.util.Prediction;
import org.jspecify.annotations.Nullable;

// Talking to the Whisperer (client/WhispererScreen), two tabs:
//  - Piece of Advice: pay 1 gold (WhispererAdvice.PRICE_NUGGETS) for a tip about how the server works (menu button
//    ADVICE; which tip is synced in a data slot, and it's also said in chat so you keep it).
//  - Knowledge: his SIZE Knowledge Pieces (the shared WhispererStock), each with its quality and price in emeralds;
//    click one to buy it (menu button = which).
// Data: the 5 qualities (or SOLD), the seconds until he restocks, then the advice shown. No item slots: emeralds and
// gold come straight out of your inventory. While it's open he stands still and looks at you (Whisperer.customer).
public class WhispererMenu extends AbstractContainerMenu {

    public static final int SECONDS = WhispererStock.SIZE;
    public static final int ADVICE = 10;

    private final ContainerData data;
    private final @Nullable Whisperer whisperer;
    // which tip he last told this player (-1 = none yet)
    private final net.minecraft.world.inventory.DataSlot advice = net.minecraft.world.inventory.DataSlot.standalone();

    // Client side: everything comes from the server through the data
    public WhispererMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainerData(WhispererStock.SIZE + 1), null);
    }

    public WhispererMenu(int containerId, Inventory inventory, ContainerData data, @Nullable Whisperer whisperer) {
        super(ModMenus.WHISPERER.get(), containerId);
        checkContainerDataCount(data, WhispererStock.SIZE + 1);
        this.data = data;
        this.whisperer = whisperer;
        this.addDataSlots(data);
        this.advice.set(-1);
        this.addDataSlot(this.advice);
    }

    // The tip he told you, or null
    public @Nullable String advice() {
        int index = this.advice.get();
        return index >= 0 && index < WhispererAdvice.TIPS.size() ? WhispererAdvice.TIPS.get(index) : null;
    }

    public @Nullable Whisperer whisperer() {
        return this.whisperer;
    }

    // Server side: a live view of the shared stock
    public static ContainerData liveData(ServerPlayer player) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                WhispererStock stock = WhispererStock.get(player.level().getServer());
                if (index < WhispererStock.SIZE) return stock.get(index);
                return (int) ((stock.ticksUntilRestock(player.level().getServer()) + 19) / 20);
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return WhispererStock.SIZE + 1;
            }
        };
    }

    // This piece's quality, or null if it's sold
    public @Nullable KnowledgeQuality offer(int index) {
        int value = this.data.get(index);
        return value < 0 ? null : KnowledgeQuality.byOrdinal(value);
    }

    public int secondsUntilRestock() {
        return this.data.get(SECONDS);
    }

    public static int emeralds(Player player) {
        int count = 0;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(Items.EMERALD)) count += stack.getCount();
        }
        return count;
    }

    private static void takeEmeralds(Player player, int count) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize() && count > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.is(Items.EMERALD)) continue;
            int take = Math.min(count, stack.getCount());
            stack.shrink(take);
            count -= take;
        }
    }

    // Pay 1 gold for a piece of advice (a different one from last time)
    private boolean buyAdvice(ServerPlayer buyer) {
        if (!buyer.isCreative() && !com.alexh.capitalism.economy.GoldMoney.take(buyer, WhispererAdvice.PRICE_NUGGETS)) {
            buyer.sendOverlayMessage(Component.literal("Advice costs 1 gold."));
            return false;
        }
        int last = this.advice.get();
        int pick = buyer.getRandom().nextInt(WhispererAdvice.TIPS.size());
        if (pick == last) pick = (pick + 1) % WhispererAdvice.TIPS.size();
        this.advice.set(pick);
        buyer.sendSystemMessage(Component.literal("The Whisperer: ").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE)
            .append(Component.literal("\"" + WhispererAdvice.TIPS.get(pick) + "\"").withStyle(style -> style.withColor(0xC8B8E0).withItalic(true))));
        buyer.level().playSound(null, buyer.blockPosition(), SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.NEUTRAL, 0.6F, 0.5F);
        return true;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player instanceof ServerPlayer buyer && id == ADVICE) return this.buyAdvice(buyer);
        if (!(player instanceof ServerPlayer buyer) || id < 0 || id >= WhispererStock.SIZE) return false;
        WhispererStock stock = WhispererStock.get(buyer.level().getServer());
        int value = stock.get(id);
        if (value < 0) {
            buyer.sendOverlayMessage(Component.literal("Someone already bought that one."));
            return false;
        }
        KnowledgeQuality quality = KnowledgeQuality.byOrdinal(value);
        if (!buyer.isCreative() && emeralds(buyer) < quality.emeralds()) {
            buyer.sendOverlayMessage(Component.literal("That one's " + quality.emeralds() + (quality.emeralds() == 1 ? " emerald." : " emeralds.")));
            return false;
        }
        if (!buyer.isCreative()) takeEmeralds(buyer, quality.emeralds());
        stock.markSold(id);
        ItemStack piece = quality.stack(1);
        if (!buyer.getInventory().add(piece)) buyer.drop(piece, false, Prediction.SERVER_ONLY);
        buyer.level().playSound(null, buyer.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.NEUTRAL, 0.8F, 0.7F);
        buyer.sendOverlayMessage(Component.literal("\"Use it well...\"").withStyle(style -> style.withColor(0xB8A0D8).withItalic(true)));
        return true;
    }

    // Done talking: he can wander again
    @Override
    public void removed(Player player) {
        super.removed(player);
        if (this.whisperer != null && this.whisperer.customer() == player) this.whisperer.setCustomer(null);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.whisperer == null || (this.whisperer.isAlive() && this.whisperer.distanceToSqr(player) < 8 * 8);
    }
}
