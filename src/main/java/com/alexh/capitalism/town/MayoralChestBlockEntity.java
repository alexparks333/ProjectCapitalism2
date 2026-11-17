package com.alexh.capitalism.town;

import com.alexh.capitalism.core.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

// The Mayoral Chest's contents (Town.md > The Town Hall): 54 slots in one block, a chest's lid that opens while someone
// has it open (the chest's own openers counter and lid). Whose: the Mayor's and the Vice Mayor's, whoever holds the office
// NOW (TownHall.officeOf), like the Town Hall Mailbox; an op in creative too. No hopper, hopper minecart or dropper can
// take from it or put in (no slots on any face). Not locked to anyone and counted for no one (economy/ContainerLocks
// skips it, so GoldLedger never sees it).
public class MayoralChestBlockEntity extends ChestBlockEntity implements WorldlyContainer {

    public static final int SIZE = 54;
    private static final int[] NO_SLOTS = new int[0];

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    public MayoralChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MAYORAL_CHEST.get(), pos, state);
    }

    // may this player open it: the Mayor or the Vice Mayor right now, or an op in creative
    public static boolean mayOpen(Player player) {
        if (player.isCreative() && player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) return true;
        if (player.level().getServer() == null) return false;
        TownHall.Office office = TownHall.get(player.level().getServer()).officeOf(player.getUUID());
        return office == TownHall.Office.MAYOR || office == TownHall.Office.VICE_MAYOR;
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    protected Component getDefaultName() {
        return Component.literal("Mayoral Chest").withStyle(s -> s.withColor(0x8A1420).withBold(true));
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new MayoralChestMenu(id, inventory, this);
    }

    @Override
    public boolean stillValid(Player player) {
        return mayOpen(player) && super.stillValid(player);
    }

    // ── no hoppers, hopper minecarts or droppers ──

    @Override
    public int[] getSlotsForFace(Direction side) {
        return NO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    static Component deniedMessage() {
        return Component.literal("Only the Mayor and Vice Mayor can open the Mayoral Chest.").withStyle(ChatFormatting.RED);
    }
}
