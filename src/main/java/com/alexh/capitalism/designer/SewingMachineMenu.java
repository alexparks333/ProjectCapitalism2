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
import org.jspecify.annotations.Nullable;

import static com.alexh.capitalism.designer.SewingMachineBlockEntity.MATERIALS;
import static com.alexh.capitalism.designer.SewingMachineBlockEntity.NEEDLE;
import static com.alexh.capitalism.designer.SewingMachineBlockEntity.OUTPUT;
import static com.alexh.capitalism.designer.SewingMachineBlockEntity.SIZE;

// The Sewing Machine's menu (client/SewingMachineScreen): the patterns down the left, the patch being sewn in the
// middle with the row of material slots under it (ghosts of what goes in), the needle and the finished patches on the
// right. Buttons: a pattern (0..), UNPICK, and the needle game's result (SEW + mistakes).
public class SewingMachineMenu extends AbstractContainerMenu {

    public static final int UNPICK = 40;
    public static final int SEW = 100;

    public static final int MATERIAL_X = 46;
    public static final int MATERIAL_Y = 92;
    public static final int MATERIAL_STEP = 22;
    public static final int NEEDLE_X = 152;
    public static final int NEEDLE_Y = 26;
    public static final int OUTPUT_X = 152;
    public static final int OUTPUT_Y = 92;
    private static final int INV_START = SIZE;
    private static final int INV_END = INV_START + 36;

    private final Container container;
    private final ContainerData data;
    private final @Nullable SewingMachineBlockEntity machine;

    public SewingMachineMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(SIZE), new SimpleContainerData(2), null);
    }

    public SewingMachineMenu(int containerId, Inventory inventory, Container container, ContainerData data, @Nullable SewingMachineBlockEntity machine) {
        super(ModMenus.SEWING_MACHINE.get(), containerId);
        checkContainerSize(container, SIZE);
        checkContainerDataCount(data, 2);
        this.container = container;
        this.data = data;
        this.machine = machine;
        for (int i = 0; i < Pattern.MAX_STITCHES; i++) {
            int stitch = i;
            this.addSlot(new Slot(container, MATERIALS + i, MATERIAL_X + i * MATERIAL_STEP, MATERIAL_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    Pattern pattern = SewingMachineMenu.this.pattern();
                    return pattern != null && stitch < pattern.stitches.size() && pattern.stitches.get(stitch).matches(stack);
                }

                @Override
                public boolean isActive() {
                    Pattern pattern = SewingMachineMenu.this.pattern();
                    return pattern != null && stitch < pattern.stitches.size();
                }
            });
        }
        this.addSlot(new Slot(container, NEEDLE, NEEDLE_X, NEEDLE_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return SewingMachineBlockEntity.isNeedle(stack);
            }
        });
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

    public @Nullable Pattern pattern() {
        int index = this.data.get(0);
        return index < 0 || index >= Pattern.values().length ? null : Pattern.byIndex(index);
    }

    public int sewn() {
        return this.data.get(1);
    }

    public boolean isSewn(int stitch) {
        return (this.sewn() & (1 << stitch)) != 0;
    }

    // The material to sew next (-1: none)
    public int next() {
        Pattern pattern = this.pattern();
        if (pattern == null) return -1;
        for (int i = 0; i < pattern.stitches.size(); i++) {
            if (!this.isSewn(i)) return i;
        }
        return -1;
    }

    // What's stopping the next material being sewn, or null
    public @Nullable String problem() {
        int next = this.next();
        return SewingMachineBlockEntity.problem(this.pattern(), next, next < 0 ? ItemStack.EMPTY : this.slots.get(MATERIALS + next).getItem(),
            this.slots.get(NEEDLE).getItem(), this.slots.get(OUTPUT).getItem());
    }

    public ItemStack needle() {
        return this.slots.get(NEEDLE).getItem();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.machine == null || !(player instanceof ServerPlayer serverPlayer)) return true;
        if (id >= SEW) {
            this.machine.sewn(serverPlayer, id - SEW);
        } else if (id == UNPICK) {
            this.machine.unpick();
        } else {
            this.machine.choose(id);
        }
        this.broadcastChanges();
        return true;
    }

    // Shift-click: out of the machine into the inventory; from the inventory a needle to its slot, the pattern's
    // materials to their slots
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack clicked = stack.copy();
        if (slotIndex < INV_START) {
            if (!this.moveItemStackTo(stack, INV_START, INV_END, true)) return ItemStack.EMPTY;
        } else if (SewingMachineBlockEntity.isNeedle(stack)) {
            if (!this.moveItemStackTo(stack, NEEDLE, NEEDLE + 1, false)) return ItemStack.EMPTY;
        } else if (!this.moveItemStackTo(stack, MATERIALS, MATERIALS + Pattern.MAX_STITCHES, false)) {
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
