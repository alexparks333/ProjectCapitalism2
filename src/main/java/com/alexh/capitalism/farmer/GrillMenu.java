package com.alexh.capitalism.farmer;

import com.alexh.capitalism.core.ModComponents;
import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.core.ModMenus;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

// The Grill's menu (client/GrillScreen): three cut slots and the fuel slot. Buttons: SEAR puts every cut in the slots
// on the grill (one sear of heat each, as far as the heat goes; the client works it out the same way, onGrill) and
// starts the searing game (client/SearGame); as each steak comes off the game sends RESULT + its slot x 256 + its final
// inside temperature, and the Seared cut (or Burnt Food, past Well Done) goes into the inventory. Closing the menu
// mid-sear burns whatever's still on. Data: 0 charges left.
public class GrillMenu extends AbstractContainerMenu {

    public static final int SEAR = 100;
    public static final int RESULT = 1000;
    public static final int CUT_X = 17;
    public static final int CUT_STEP = 22;
    public static final int CUT_Y = 27;
    public static final int FUEL_X = 39;
    public static final int FUEL_Y = 54;
    private static final int INV_START = GrillBlockEntity.SIZE;
    private static final int INV_END = INV_START + 36;

    private final Container container;
    private final ContainerData data;
    private final ContainerLevelAccess access;
    // the cuts on the grill right now (by slot), and how aged each was
    private final @Nullable MeatCut[] searing = new MeatCut[GrillBlockEntity.CUTS];
    private final int[] aged = new int[GrillBlockEntity.CUTS];

    public GrillMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(GrillBlockEntity.SIZE), new SimpleContainerData(1), ContainerLevelAccess.NULL);
    }

    public GrillMenu(int containerId, Inventory inventory, Container container, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.GRILL.get(), containerId);
        checkContainerSize(container, GrillBlockEntity.SIZE);
        this.container = container;
        this.data = data;
        this.access = access;
        for (int i = 0; i < GrillBlockEntity.CUTS; i++) {
            this.addSlot(new Slot(container, GrillBlockEntity.CUT + i, CUT_X + i * CUT_STEP, CUT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return GrillBlockEntity.searable(stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        this.addSlot(new Slot(container, GrillBlockEntity.FUEL, FUEL_X, FUEL_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return GrillBlockEntity.fuel(stack) > 0;
            }
        });
        this.addStandardInventorySlots(inventory, 8, 84);
        this.addDataSlots(data);
    }

    public ItemStack cut(int slot) {
        return this.slots.get(GrillBlockEntity.CUT + slot).getItem();
    }

    public ItemStack fuel() {
        return this.slots.get(GrillBlockEntity.FUEL).getItem();
    }

    // Which slots' cuts go on the grill with this much heat: the ones with a cut, in order, while the heat lasts
    public boolean[] onGrill() {
        boolean[] on = new boolean[GrillBlockEntity.CUTS];
        int charges = this.container instanceof GrillBlockEntity grill ? grill.charges() : this.charges();
        int heat = GrillBlockEntity.heat(charges, this.fuel());
        for (int i = 0; i < GrillBlockEntity.CUTS && heat > 0; i++) {
            if (cutOf(this.cut(i)) != null) {
                on[i] = true;
                heat--;
            }
        }
        return on;
    }

    public int charges() {
        return this.data.get(0);
    }

    // The cut in the slot's MeatCut, or null
    public static @Nullable MeatCut cutOf(ItemStack stack) {
        for (MeatCut cut : MeatCut.values()) {
            if (stack.is(cut.cut())) return cut;
        }
        return null;
    }

    // The cut a Seared item was, or null
    public static @Nullable MeatCut searedCutOf(ItemStack stack) {
        for (MeatCut cut : MeatCut.values()) {
            if (stack.is(ModItems.SEARED_CUTS.get(cut).get())) return cut;
        }
        return null;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer serverPlayer)) return true;
        if (id == SEAR) {
            for (MeatCut on : this.searing) if (on != null) return false;
            boolean[] on = this.onGrill();
            boolean any = false;
            for (int i = 0; i < GrillBlockEntity.CUTS; i++) {
                if (!on[i]) continue;
                ItemStack stack = this.cut(i);
                if (!(this.container instanceof GrillBlockEntity grill ? grill.useHeat() : true)) break;
                this.searing[i] = cutOf(stack);
                this.aged[i] = Freshness.agedLevel(stack);
                stack.shrink(1);
                any = true;
            }
            if (!any) return false;
            this.container.setChanged();
            this.access.execute((level, pos) -> {
                level.setBlock(pos, level.getBlockState(pos).setValue(GrillBlock.LIT, true), 3);
                level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.6F, 1.2F);
            });
        } else if (id >= RESULT) {
            int slot = (id - RESULT) / 256;
            if (slot >= GrillBlockEntity.CUTS || this.searing[slot] == null) return false;
            this.finish(serverPlayer, slot, (id - RESULT) % 256);
        }
        this.broadcastChanges();
        return true;
    }

    // The steak comes off the grill: Seared (with its doneness, order, crust and aging), or Burnt Food past Well Done
    private void finish(ServerPlayer player, int slot, int temperature) {
        MeatCut cut = this.searing[slot];
        if (cut == null) return;
        this.searing[slot] = null;
        ItemStack result = seared(cut, temperature, this.aged[slot]);
        boolean stillOn = false;
        for (MeatCut on : this.searing) stillOn |= on != null;
        boolean lit = stillOn;
        if (player.isAlive() && !player.hasDisconnected()) player.getInventory().placeItemBackInInventory(result, net.minecraft.util.Prediction.SERVER_ONLY);
        else player.drop(result, false, net.minecraft.util.Prediction.SERVER_ONLY);
        this.access.execute((level, pos) -> {
            level.setBlock(pos, level.getBlockState(pos).setValue(GrillBlock.LIT, lit), 3);
            level.playSound(null, pos, result.is(ModItems.BURNT_FOOD.get()) ? SoundEvents.FIRE_EXTINGUISH : SoundEvents.PLAYER_BURP,
                SoundSource.BLOCKS, 0.7F, 1.0F);
        });
    }

    // What a sear makes (public for the tests)
    public static ItemStack seared(MeatCut cut, int temperature, int aged) {
        if (temperature >= Sear.Doneness.BURNT) return new ItemStack(ModItems.BURNT_FOOD.get());
        Sear sear = new Sear(Sear.Doneness.of(temperature).ordinal(), aged);
        ItemStack steak = new ItemStack(ModItems.SEARED_CUTS.get(cut).get());
        steak.set(ModComponents.SEAR.get(), sear);
        steak.set(DataComponents.FOOD, sear.food(cut));
        steak.set(DataComponents.CONSUMABLE, sear.consumable(cut));
        steak.set(ModComponents.COOK_MADE.get(), true);
        return steak;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // walked away from the grill: it burns
        if (player instanceof ServerPlayer serverPlayer) {
            for (int i = 0; i < GrillBlockEntity.CUTS; i++) this.finish(serverPlayer, i, Sear.Doneness.BURNT);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack clicked = stack.copy();
        if (slotIndex < INV_START) {
            if (!this.moveItemStackTo(stack, INV_START, INV_END, true)) return ItemStack.EMPTY;
        } else if (GrillBlockEntity.searable(stack)) {
            if (!this.moveItemStackTo(stack, GrillBlockEntity.CUT, GrillBlockEntity.CUT + GrillBlockEntity.CUTS, false)) return ItemStack.EMPTY;
        } else if (GrillBlockEntity.fuel(stack) > 0) {
            if (!this.moveItemStackTo(stack, GrillBlockEntity.FUEL, GrillBlockEntity.FUEL + 1, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == clicked.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return clicked;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }
}
