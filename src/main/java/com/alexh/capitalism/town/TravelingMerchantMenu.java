package com.alexh.capitalism.town;

import com.alexh.capitalism.calendar.Calendar;
import com.alexh.capitalism.core.ModMenus;
import com.alexh.capitalism.economy.GoldMoney;
import com.alexh.capitalism.economy.NpcPricing;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

// Talking to the Traveling Merchant (client/TravelingMerchantScreen): today's wares (TravelingMerchantShop), one of
// each, shared by everyone. Opening it sends the wares and YOUR prices (NpcPricing: his markup and your discounts);
// menu button = which ware to buy. Data: which are sold (a bitmask) and the seconds until he leaves at dawn.
// Your inventory isn't shown: nothing to drag, just buying.
public class TravelingMerchantMenu extends AbstractContainerMenu {

    public record Ware(ItemStack stack, long price) {
    }

    // data: 0 sold-out bitmask, 1 seconds until he leaves, 2.. how many of each ware are left
    public static final int DATA = 2 + TravelingMerchantShop.MAX_OFFERS;

    private final List<Ware> wares;
    private final ContainerData data;
    private final @Nullable TravelingMerchant merchant;

    // client: from the buffer the server wrote in open()
    public TravelingMerchantMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, read(buf), new SimpleContainerData(DATA), null);
    }

    public TravelingMerchantMenu(int containerId, List<Ware> wares, ContainerData data, @Nullable TravelingMerchant merchant) {
        super(ModMenus.TRAVELING_MERCHANT.get(), containerId);
        checkContainerDataCount(data, DATA);
        this.wares = wares;
        this.data = data;
        this.merchant = merchant;
        this.addDataSlots(data);
    }

    private static List<Ware> read(RegistryFriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<Ware> wares = new ArrayList<>();
        for (int i = 0; i < n; i++) wares.add(new Ware(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf), buf.readVarLong()));
        return wares;
    }

    // (server) the wares at this buyer's prices, and the menu
    public static void open(ServerPlayer buyer, TravelingMerchant merchant) {
        MinecraftServer server = buyer.level().getServer();
        List<Ware> wares = new ArrayList<>();
        for (String key : TravelingMerchantShop.stock(server)) {
            wares.add(new Ware(TravelingMerchantShop.stackFor(key), NpcPricing.npcPrice(TravelingMerchantShop.fairFor(key), buyer)));
        }
        ContainerData live = new ContainerData() {
            @Override
            public int get(int index) {
                if (index == 0) return TravelingMerchantShop.soldMask(server);
                if (index >= 2) return TravelingMerchantShop.left(server, index - 2);
                return (int) ((Calendar.DAY_TICKS - Math.floorMod(Calendar.clock(server.overworld()), Calendar.DAY_TICKS)) / 20);
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return DATA;
            }
        };
        buyer.openMenu(new SimpleMenuProvider((id, inventory, who) -> new TravelingMerchantMenu(id, wares, live, merchant),
            Component.literal("The Traveling Merchant")), buf -> {
            buf.writeVarInt(wares.size());
            for (Ware ware : wares) {
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, ware.stack());
                buf.writeVarLong(ware.price());
            }
        });
    }

    public List<Ware> wares() {
        return this.wares;
    }

    public boolean sold(int index) {
        return (this.data.get(0) & (1 << index)) != 0;
    }

    // how many of this ware he has left
    public int left(int index) {
        return index + 2 < DATA ? this.data.get(index + 2) : 0;
    }

    // seconds until he leaves (dawn)
    public int secondsLeft() {
        return this.data.get(1);
    }

    public @Nullable TravelingMerchant merchant() {
        return this.merchant;
    }

    private static void say(ServerPlayer player, String line) {
        player.sendOverlayMessage(Component.literal(line).withStyle(s -> s.withColor(TextColor.fromRgb(0xE8C890)).withItalic(true)));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer buyer) || this.merchant == null) return false;
        MinecraftServer server = buyer.level().getServer();
        List<String> stock = TravelingMerchantShop.stock(server);
        if (id < 0 || id >= stock.size() || id >= this.wares.size()) return false;
        if (TravelingMerchantShop.sold(server, id)) {
            say(buyer, "\"Ah, someone beat you to that one, friend.\"");
            return false;
        }
        String key = stock.get(id);
        long price = NpcPricing.npcPrice(TravelingMerchantShop.fairFor(key), buyer);
        if (!buyer.isCreative() && !GoldMoney.take(buyer, price)) {
            say(buyer, "\"That one's " + GoldMoney.format(price) + ". Come back when your purse is heavier.\"");
            return false;
        }
        TravelingMerchantShop.markSold(server, id);
        TravelingMerchantShop.give(buyer, TravelingMerchantShop.stackFor(key));
        buyer.level().playSound(null, buyer.blockPosition(), SoundEvents.VINDICATOR_CELEBRATE, SoundSource.NEUTRAL, 0.7F, 0.6F);
        buyer.level().playSound(null, buyer.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.NEUTRAL, 0.6F, 0.7F);
        say(buyer, "\"Carried that one a thousand miles. Treat it well.\"");
        this.broadcastChanges();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (this.merchant != null && this.merchant.customer() == player) this.merchant.setCustomer(null);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.merchant == null || (this.merchant.isAlive() && this.merchant.here() && this.merchant.distanceToSqr(player) < 8 * 8);
    }
}
