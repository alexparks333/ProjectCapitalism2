package com.alexh.capitalism.designer;

import com.alexh.capitalism.core.ModMenus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.alexh.capitalism.designer.DesignerTableBlockEntity.LEATHER;
import static com.alexh.capitalism.designer.DesignerTableBlockEntity.NEEDLE;
import static com.alexh.capitalism.designer.DesignerTableBlockEntity.OUTPUT;
import static com.alexh.capitalism.designer.DesignerTableBlockEntity.PATCHES;
import static com.alexh.capitalism.designer.DesignerTableBlockEntity.SIZE;

// The Designer Table's menu (client/DesignerTableScreen): the bags to choose from along the top, the row of 6 patch
// slots on the cutting mat (ghosts of the chosen bag's patches), Leather and the needle bottom left, the bag bottom
// right. Buttons: a bag (0..), MAKE.
public class DesignerTableMenu extends AbstractContainerMenu {

    public static final int MAKE = 50;

    public static final int PATCH_X = 27;
    public static final int PATCH_Y = 62;
    public static final int PATCH_STEP = 20;
    public static final int LEATHER_X = 10;
    public static final int LEATHER_Y = 98;
    public static final int NEEDLE_X = 32;
    public static final int NEEDLE_Y = 98;
    public static final int OUTPUT_X = 150;
    public static final int OUTPUT_Y = 98;
    private static final int INV_START = SIZE;
    private static final int INV_END = INV_START + 36;

    private final Container container;
    private final ContainerData data;
    private final @Nullable DesignerTableBlockEntity table;

    public DesignerTableMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(SIZE), new SimpleContainerData(1), null);
    }

    public DesignerTableMenu(int containerId, Inventory inventory, Container container, ContainerData data, @Nullable DesignerTableBlockEntity table) {
        super(ModMenus.DESIGNER_TABLE.get(), containerId);
        checkContainerSize(container, SIZE);
        checkContainerDataCount(data, 1);
        this.container = container;
        this.data = data;
        this.table = table;
        this.addSlot(new Slot(container, LEATHER, LEATHER_X, LEATHER_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.LEATHER);
            }
        });
        this.addSlot(new Slot(container, NEEDLE, NEEDLE_X, NEEDLE_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return DesignerTableBlockEntity.isNeedle(stack);
            }
        });
        for (int i = 0; i < Bag.MAX_PATCHES; i++) {
            this.addSlot(new Slot(container, PATCHES + i, PATCH_X + i * PATCH_STEP, PATCH_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return DesignerTableBlockEntity.isPatch(stack);
                }
            });
        }
        this.addSlot(new Slot(container, OUTPUT, OUTPUT_X, OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player taker, ItemStack stack) {
                com.alexh.capitalism.guide.Discoveries.discover(taker, stack);
                super.onTake(taker, stack);
            }
        });
        this.addStandardInventorySlots(inventory, 8, 140);
        this.addDataSlots(data);
    }

    public Design design() {
        return Design.byIndex(this.data.get(0));
    }

    public List<ItemStack> patches() {
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < Bag.MAX_PATCHES; i++) list.add(this.slots.get(PATCHES + i).getItem());
        return list;
    }

    public @Nullable String problem() {
        return DesignerTableBlockEntity.problem(this.design(), this.slots.get(LEATHER).getItem(), this.slots.get(NEEDLE).getItem(),
            this.patches(), this.slots.get(OUTPUT).getItem());
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.table == null || !(player instanceof ServerPlayer serverPlayer)) return true;
        if (id == MAKE) this.table.make(serverPlayer);
        else this.table.choose(id);
        this.broadcastChanges();
        return true;
    }

    // Shift-click: out of the table into the inventory; from the inventory Leather, the needle and Patches to their
    // slots (a Patch to the first empty slot that wants it)
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack clicked = stack.copy();
        if (slotIndex < INV_START) {
            if (!this.moveItemStackTo(stack, INV_START, INV_END, true)) return ItemStack.EMPTY;
        } else if (stack.is(Items.LEATHER)) {
            if (!this.moveItemStackTo(stack, LEATHER, LEATHER + 1, false)) return ItemStack.EMPTY;
        } else if (DesignerTableBlockEntity.isNeedle(stack)) {
            if (!this.moveItemStackTo(stack, NEEDLE, NEEDLE + 1, false)) return ItemStack.EMPTY;
        } else if (DesignerTableBlockEntity.isPatch(stack)) {
            if (!this.movePatch(stack)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == clicked.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return clicked;
    }

    // One patch into each slot of the row that wants that patch and is empty
    private boolean movePatch(ItemStack stack) {
        List<net.minecraft.world.item.Item> wanted = this.design().patches();
        boolean moved = false;
        for (int i = 0; i < wanted.size() && !stack.isEmpty(); i++) {
            Slot target = this.slots.get(PATCHES + i);
            if (!target.hasItem() && stack.is(wanted.get(i))) {
                target.setByPlayer(stack.split(1));
                moved = true;
            }
        }
        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }
}
