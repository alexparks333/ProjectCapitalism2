package com.alexh.capitalism.enchanter;

import com.alexh.capitalism.core.ModBlocks;
import com.alexh.capitalism.core.ModMenus;
import com.alexh.capitalism.core.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

import java.util.List;

// The Enchanter's Table (EnchantersTableBlock, Enchanter.md): a big slot, a small slot and a result slot, plus the list
// of the big item's enchantments (client/EnchantersTableScreen). Book + Blaze Rod makes an Ench-Removal; Bamboo + a
// Potion of Poison makes an Apprentice Wand (the bottle comes back). An enchanted
// item + an Ench-Removal: click an enchantment to pick it (menu button = its index) and the result is the item without
// it; taking it costs EnchantRemoval.COST XP levels and the Ench-Removal. No fuel: XP only.
// Whatever is left in the slots goes back to you when you close it.
public class EnchantersTableMenu extends AbstractContainerMenu {

    public static final int BIG = 0;
    public static final int SMALL = 1;
    public static final int RESULT = 2;
    private static final int INV_START = 3;
    private static final int INV_END = INV_START + 36;

    private final ContainerLevelAccess access;
    private final SimpleContainer inputs = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
            EnchantersTableMenu.this.slotsChanged(this);
        }
    };
    private final ResultContainer result = new ResultContainer();
    // which enchantment is picked (-1 = none); synced so the screen can light it up
    private final DataSlot selected = DataSlot.standalone();
    private ItemStack lastBig = ItemStack.EMPTY;

    public EnchantersTableMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public EnchantersTableMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(ModMenus.ENCHANTERS_TABLE.get(), containerId);
        this.access = access;
        this.selected.set(-1);
        this.addSlot(new Slot(this.inputs, BIG, 22, 26) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.BOOK) || stack.is(Items.BAMBOO) || !EnchantRemoval.entries(stack).isEmpty();
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        this.addSlot(new Slot(this.inputs, SMALL, 62, 26) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return EnchantRemoval.isRemover(stack) || stack.is(Items.BLAZE_ROD) || isPoison(stack);
            }
        });
        this.addSlot(new Slot(this.result, 0, 136, 26) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player taker) {
                return EnchantersTableMenu.this.canTake(taker);
            }

            @Override
            public void onTake(Player taker, ItemStack carried) {
                EnchantersTableMenu.this.take(taker);
                super.onTake(taker, carried);
            }
        });
        this.addStandardInventorySlots(inventory, 8, 140);
        this.addDataSlot(this.selected);
    }

    // Client and server: the enchantments of the item in the big slot
    public List<EnchantRemoval.Entry> entries() {
        return EnchantRemoval.entries(this.slots.get(BIG).getItem());
    }

    public int selected() {
        return this.selected.get();
    }

    public boolean hasRemover() {
        return EnchantRemoval.isRemover(this.slots.get(SMALL).getItem());
    }

    // Book + Blaze Rod in the slots: making an Ench-Removal
    private boolean crafting() {
        return EnchantRemoval.isRecipe(this.inputs.getItem(BIG), this.inputs.getItem(SMALL)) || this.makingWand();
    }

    // Bamboo + a Potion of Poison (any strength or length): making an Apprentice Wand (ApprenticeWand)
    private boolean makingWand() {
        return this.inputs.getItem(BIG).is(Items.BAMBOO) && isPoison(this.inputs.getItem(SMALL));
    }

    public static boolean isPoison(ItemStack stack) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return stack.is(Items.POTION) && contents != null
            && (contents.is(Potions.POISON) || contents.is(Potions.LONG_POISON) || contents.is(Potions.STRONG_POISON));
    }

    private EnchantRemoval.@org.jspecify.annotations.Nullable Entry picked() {
        List<EnchantRemoval.Entry> entries = this.entries();
        int index = this.selected.get();
        return index >= 0 && index < entries.size() ? entries.get(index) : null;
    }

    private boolean canTake(Player taker) {
        if (this.crafting()) return true;
        EnchantRemoval.Entry entry = this.picked();
        return entry != null && EnchantRemoval.canAfford(taker, entry);
    }

    @Override
    public boolean clickMenuButton(Player clicker, int id) {
        if (id < 0 || id >= this.entries().size()) return false;
        this.selected.set(id);
        this.slotsChanged(this.inputs);
        return true;
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        ItemStack big = this.inputs.getItem(BIG);
        // a different item: nothing picked any more
        if (!ItemStack.matches(big, this.lastBig)) {
            this.selected.set(-1);
            this.lastBig = big.copy();
        }
        if (this.crafting()) {
            this.result.setItem(0, new ItemStack(this.makingWand() ? ModItems.APPRENTICE_WAND.get() : ModItems.ENCH_REMOVAL.get()));
            return;
        }
        EnchantRemoval.Entry entry = this.picked();
        boolean ready = entry != null && EnchantRemoval.isRemover(this.inputs.getItem(SMALL));
        this.result.setItem(0, ready ? EnchantRemoval.without(big, entry) : ItemStack.EMPTY);
    }

    // The result was taken: an Ench-Removal made, or an enchantment burnt off (pay the levels, use up the Ench-Removal)
    private void take(Player taker) {
        if (this.makingWand()) {
            this.inputs.removeItem(BIG, 1);
            this.inputs.removeItem(SMALL, 1);
            // the empty bottle comes back
            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
            if (!taker.getInventory().add(bottle)) taker.drop(bottle, false, net.minecraft.util.Prediction.SERVER_ONLY);
            com.alexh.capitalism.guide.Discoveries.discover(taker, new ItemStack(ModItems.APPRENTICE_WAND.get()));
            this.access.execute((level, pos) -> {
                level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.7F, 1.2F);
                level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            });
            return;
        }
        if (this.crafting()) {
            this.inputs.removeItem(BIG, 1);
            this.inputs.removeItem(SMALL, 1);
            com.alexh.capitalism.guide.Discoveries.discover(taker, new ItemStack(ModItems.ENCH_REMOVAL.get()));
            this.access.execute((level, pos) -> {
                level.playSound(null, pos, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 0.6F, 1.2F);
                level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            });
            return;
        }
        EnchantRemoval.Entry entry = this.picked();
        if (entry == null) return;
        if (!taker.isCreative()) taker.giveExperienceLevels(-entry.cost());
        this.inputs.removeItem(BIG, 1);
        this.inputs.removeItem(SMALL, 1);
        this.access.execute((level, pos) -> {
            level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.7F, 1.0F);
            level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 0.7F);
        });
        if (taker instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendOverlayMessage(Component.literal("Burnt off ").append(entry.name()).append(" for "
                + entry.cost() + (entry.cost() == 1 ? " level." : " levels.")));
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> this.clearContainer(player, this.inputs));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.ENCHANTERS_TABLE.get());
    }

    // Shift-click: the result and the inputs go to your inventory; from the inventory, an Ench-Removal or a Blaze Rod
    // goes in the small slot, anything else (an enchanted item, a Book) in the big one
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack clicked = stack.copy();
        if (slotIndex < INV_START) {
            if (slotIndex == RESULT && !slot.mayPickup(player)) return ItemStack.EMPTY;
            if (!this.moveItemStackTo(stack, INV_START, INV_END, true)) return ItemStack.EMPTY;
            if (slotIndex == RESULT) slot.onQuickCraft(stack, clicked);
        } else if (EnchantRemoval.isRemover(stack) || stack.is(Items.BLAZE_ROD) || isPoison(stack)) {
            if (!this.moveItemStackTo(stack, SMALL, SMALL + 1, false)) return ItemStack.EMPTY;
        } else if (!this.moveItemStackTo(stack, BIG, BIG + 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == clicked.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return clicked;
    }
}
