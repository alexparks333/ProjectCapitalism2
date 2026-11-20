package com.alexh.capitalism.town;

import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.core.ModMenus;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

// Talking to the Blood Merchant (client/BloodMerchantScreen), two tabs:
//  - Curios: his decorations (BloodMerchantShop.OFFERS) for Blood Shards (the bosses' trophies also take that boss's
//    crest piece), and the Essential Wishing Star for a Blood Moon Crest, once per player. Menu button = which offer.
//  - Mend the Crest: three slots set in a broken crest, each taking only its piece (upper left the Revenant's, upper
//    right the Sanguine Witch's, the bottom Gorefang's); with all three in, MEND joins them into a Blood Moon Crest for
//    BloodMerchantShop.MEND_SHARDS Blood Shards. Pieces left in the slots go back to you when you close it.
// Data: whether you've had your star (1/0). Your inventory is along the bottom.
public class BloodMerchantMenu extends AbstractContainerMenu {

    public static final int MEND = 100;
    // a building block (1 shard each) bought by the stack: BULK + which offer
    public static final int BULK = 1000;
    // a Spooky offer (BloodMerchantShop.SPOOKY): SPOOKY + which one
    public static final int SPOOKY = 2000;
    // where the piece slots sit (in the screen's broken crest: BloodMerchantScreen.CREST_X/Y + each piece's middle)
    public static final int[][] PIECE_SLOTS = {{41, 69}, {77, 69}, {59, 100}};
    public static final int INV_X = 44;
    public static final int INV_Y = 172;

    private final Container pieces = new SimpleContainer(3);
    private final ContainerData data;
    private final @Nullable BloodMerchant merchant;
    // (client) the tab showing: the piece slots only show on Mend the Crest
    public int tab;

    public BloodMerchantMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainerData(2), null);
        this.tab = 0;
    }

    public BloodMerchantMenu(int containerId, Inventory inventory, ContainerData data, @Nullable BloodMerchant merchant) {
        super(ModMenus.BLOOD_MERCHANT.get(), containerId);
        checkContainerDataCount(data, 2);
        this.data = data;
        this.merchant = merchant;
        this.tab = 1;
        for (int i = 0; i < 3; i++) {
            Supplier<Item> piece = piece(i);
            this.addSlot(new Slot(this.pieces, i, PIECE_SLOTS[i][0], PIECE_SLOTS[i][1]) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.is(piece.get());
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public boolean isActive() {
                    return BloodMerchantMenu.this.tab == 1;
                }
            });
        }
        this.addStandardInventorySlots(inventory, INV_X, INV_Y);
        this.addDataSlots(data);
    }

    public static Supplier<Item> piece(int i) {
        return switch (i) {
            case 0 -> ModItems.REVENANT_CREST::get;
            case 1 -> ModItems.SANGUINE_CREST::get;
            default -> ModItems.GOREFANG_CREST::get;
        };
    }

    public static ContainerData liveData(ServerPlayer player) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                if (index == 1) return BloodMerchantShop.spookyOpen(player.level()) ? 1 : 0;
                return BloodMerchantShop.StarBuyers.get(player.level().getServer()).has(player.getUUID()) ? 1 : 0;
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
    }

    public boolean hadStar() {
        return this.data.get(0) == 1;
    }

    // the Spooky tab is open (the Halloween weeks)
    public boolean spookyOpen() {
        return this.data.get(1) == 1;
    }

    public boolean piecePlaced(int i) {
        return !this.slots.get(i).getItem().isEmpty();
    }

    public boolean allPlaced() {
        return this.piecePlaced(0) && this.piecePlaced(1) && this.piecePlaced(2);
    }

    public @Nullable BloodMerchant merchant() {
        return this.merchant;
    }

    private static void say(ServerPlayer player, String line) {
        player.sendOverlayMessage(Component.literal(line).withStyle(s -> s.withColor(TextColor.fromRgb(0xE04050)).withItalic(true)));
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false, Prediction.SERVER_ONLY);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer buyer)) return true;
        if (id == MEND) return this.mend(buyer);
        if (id >= SPOOKY) return this.buySpooky(buyer, id - SPOOKY);
        if (id >= BULK) return this.buyStack(buyer, id - BULK);
        if (id < 0 || id >= BloodMerchantShop.OFFERS.size()) return false;
        BloodMerchantShop.Offer offer = BloodMerchantShop.OFFERS.get(id);
        if (!offer.exists()) return false;
        boolean free = buyer.isCreative();
        if (offer.star()) {
            var buyers = BloodMerchantShop.StarBuyers.get(buyer.level().getServer());
            if (buyers.has(buyer.getUUID())) {
                say(buyer, "\"You've had your wish from me. One is all a soul may buy.\"");
                return false;
            }
            if (!free && BloodMerchantShop.count(buyer, ModItems.BLOOD_MOON_CREST.get()) < 1) {
                say(buyer, "\"Bring me the whole Blood Moon Crest, and the star is yours.\"");
                return false;
            }
            if (!free) BloodMerchantShop.take(buyer, ModItems.BLOOD_MOON_CREST.get(), 1);
            buyers.add(buyer.getUUID());
            give(buyer, new ItemStack(offer.item()));
            buyer.level().playSound(null, buyer.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.0F, 0.6F);
            buyer.level().playSound(null, buyer.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.NEUTRAL, 1.0F, 0.6F);
            say(buyer, "\"A wish, for a crest... a fair trade. Spend it wisely.\"");
            this.broadcastChanges();
            return true;
        }
        Item piece = offer.piece() == null ? null : offer.piece().get();
        if (!free && BloodMerchantShop.shards(buyer) < offer.shards()) {
            say(buyer, "\"That one is " + offer.shards() + " Blood Shards.\"");
            return false;
        }
        if (!free && piece != null && BloodMerchantShop.count(buyer, piece) < 1) {
            say(buyer, "\"That one needs its piece of the crest as well.\"");
            return false;
        }
        if (!free) {
            BloodMerchantShop.take(buyer, ModItems.BLOOD_SHARD.get(), offer.shards());
            if (piece != null) BloodMerchantShop.take(buyer, piece, 1);
        }
        give(buyer, new ItemStack(offer.item()));
        buyer.level().playSound(null, buyer.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.NEUTRAL, 0.9F, 0.6F);
        say(buyer, "\"Exquisite, isn't it? There is no other like it.\"");
        return true;
    }

    // something from his Halloween stall, while it's open
    private boolean buySpooky(ServerPlayer buyer, int index) {
        if (index < 0 || index >= BloodMerchantShop.SPOOKY.size()) return false;
        if (!BloodMerchantShop.spookyOpen(buyer.level())) {
            say(buyer, "\"Those are only for the Halloween nights.\"");
            return false;
        }
        BloodMerchantShop.Offer offer = BloodMerchantShop.SPOOKY.get(index);
        if (!offer.exists()) return false;
        boolean free = buyer.isCreative();
        if (!free && BloodMerchantShop.shards(buyer) < offer.shards()) {
            say(buyer, "\"That one is " + offer.shards() + " Blood Shards.\"");
            return false;
        }
        if (!free) BloodMerchantShop.take(buyer, ModItems.BLOOD_SHARD.get(), offer.shards());
        give(buyer, new ItemStack(offer.item()));
        buyer.level().playSound(null, buyer.blockPosition(), SoundEvents.WITCH_CELEBRATE, SoundSource.NEUTRAL, 0.7F, 0.8F);
        say(buyer, "\"A little something for the season... boo.\"");
        return true;
    }

    // shift-click on a building block: as many as you can pay for, up to a stack of 64
    private boolean buyStack(ServerPlayer buyer, int index) {
        if (index < 0 || index >= BloodMerchantShop.OFFERS.size()) return false;
        BloodMerchantShop.Offer offer = BloodMerchantShop.OFFERS.get(index);
        if (!offer.exists() || offer.star() || offer.piece() != null || offer.shards() != 1) return false;
        int n = buyer.isCreative() ? 64 : Math.min(64, BloodMerchantShop.shards(buyer));
        if (n <= 0) {
            say(buyer, "\"One Blood Shard a block.\"");
            return false;
        }
        if (!buyer.isCreative()) BloodMerchantShop.take(buyer, ModItems.BLOOD_SHARD.get(), n);
        give(buyer, new ItemStack(offer.item(), n));
        buyer.level().playSound(null, buyer.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.NEUTRAL, 0.9F, 0.8F);
        return true;
    }

    // the three pieces and MEND_SHARDS Blood Shards: a whole Blood Moon Crest
    private boolean mend(ServerPlayer buyer) {
        if (!this.allPlaced()) {
            say(buyer, "\"All three pieces, in their places.\"");
            return false;
        }
        if (!buyer.isCreative() && BloodMerchantShop.shards(buyer) < BloodMerchantShop.MEND_SHARDS) {
            say(buyer, "\"Mending it costs " + BloodMerchantShop.MEND_SHARDS + " Blood Shards.\"");
            return false;
        }
        if (!buyer.isCreative()) BloodMerchantShop.take(buyer, ModItems.BLOOD_SHARD.get(), BloodMerchantShop.MEND_SHARDS);
        for (int i = 0; i < 3; i++) this.pieces.setItem(i, ItemStack.EMPTY);
        give(buyer, new ItemStack(ModItems.BLOOD_MOON_CREST.get()));
        buyer.level().playSound(null, buyer.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.NEUTRAL, 0.8F, 0.6F);
        buyer.level().playSound(null, buyer.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.NEUTRAL, 1.0F, 0.8F);
        say(buyer, "\"Whole again... the Blood Moon Crest.\"");
        this.broadcastChanges();
        return true;
    }

    // shift-click: a piece from your inventory into its slot; out of a slot back to your inventory
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < 3) {
            if (!this.moveItemStackTo(stack, 3, this.slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            for (int i = 0; i < 3 && !moved; i++) {
                if (stack.is(piece(i).get()) && !this.slots.get(i).hasItem()) moved = this.moveItemStackTo(stack, i, i + 1, false);
            }
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.clearContainer(player, this.pieces);
        if (this.merchant != null && this.merchant.customer() == player) this.merchant.setCustomer(null);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.merchant == null || (this.merchant.isAlive() && this.merchant.out() && this.merchant.distanceToSqr(player) < 8 * 8);
    }
}
