package com.alexh.capitalism.designer;

import com.alexh.capitalism.core.ModBlockEntities;
import com.alexh.capitalism.core.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.List;

// The Designer Table (Designer.md > Designer Table): pick a bag or a pet hat (Design, Bag, Hat), lay its Patches in the row of 6 (ghosts show
// which goes where), put Leather and Sewing Needles in (needleFor: Iron or Diamond), and press Stitch: the patches,
// one Leather and NEEDLES_USED needles are sewn into the bag (so even a small bag costs something).
public class DesignerTableBlockEntity extends DesignerBlockEntity {

    public static final int LEATHER = 0;
    public static final int NEEDLE = 1;
    public static final int PATCHES = 2;
    public static final int OUTPUT = PATCHES + Bag.MAX_PATCHES;
    public static final int SIZE = OUTPUT + 1;
    // Sewing Needles used up by every bag or hat stitched
    public static final int NEEDLES_USED = 2;

    private int bag;
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return DesignerTableBlockEntity.this.bag;
        }

        @Override
        public void set(int index, int value) {
            DesignerTableBlockEntity.this.bag = value;
        }

        @Override
        public int getCount() {
            return 1;
        }
    };

    public DesignerTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DESIGNER_TABLE.get(), pos, state, "Designer Table", SIZE);
    }

    public Design design() {
        return Design.byIndex(this.bag);
    }

    public void choose(int index) {
        if (index < 0 || index >= Design.all().size()) return;
        this.bag = index;
        this.setChanged();
    }

    // Why it can't make the bag right now, or null if it can (the screen shows it; the server checks again)
    public static @Nullable String problem(Design bag, ItemStack leather, ItemStack needle, List<ItemStack> patches, ItemStack output) {
        List<Item> wanted = bag.patches();
        for (int i = 0; i < Bag.MAX_PATCHES; i++) {
            ItemStack patch = patches.get(i);
            if (i < wanted.size() ? !patch.is(wanted.get(i)) : !patch.isEmpty()) {
                return i < wanted.size() ? "Patch " + (i + 1) + ": " + new ItemStack(wanted.get(i)).getHoverName().getString() : "Patch " + (i + 1) + " isn't needed";
            }
        }
        if (!leather.is(Items.LEATHER)) return "Needs Leather";
        Item kind = needleFor(bag);
        if (!needle.is(kind) || needle.getCount() < NEEDLES_USED) return "Needs " + NEEDLES_USED + " " + (kind == ModItems.IRON_SEWING_NEEDLE.get() ? "Iron" : "Diamond") + " Needles";
        if (!output.isEmpty()) return "Take the bag out";
        return null;
    }

    public @Nullable String problem() {
        return problem(this.design(), this.items.get(LEATHER), this.items.get(NEEDLE), this.items.subList(PATCHES, PATCHES + Bag.MAX_PATCHES), this.items.get(OUTPUT));
    }

    // Stitch it together: one of each Patch, one Leather and NEEDLES_USED needles become the bag
    public void make(ServerPlayer player) {
        if (this.problem() != null || !(this.level instanceof ServerLevel level)) return;
        Design bag = this.design();
        for (int i = 0; i < bag.patches().size(); i++) this.items.get(PATCHES + i).shrink(1);
        this.items.get(LEATHER).shrink(1);
        this.items.get(NEEDLE).shrink(NEEDLES_USED);
        this.items.set(OUTPUT, new ItemStack(bag.item()));
        level.playSound(null, this.getBlockPos(), SoundEvents.UI_LOOM_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 0.8F);
        level.playSound(null, this.getBlockPos(), SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.5F, 1.6F);
        player.sendOverlayMessage(Component.literal(bag.title() + " made!").withStyle(ChatFormatting.GOLD));
        this.setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == LEATHER) return stack.is(Items.LEATHER);
        if (slot == NEEDLE) return isNeedle(stack);
        if (slot == OUTPUT) return false;
        return isPatch(stack);
    }

    public static boolean isNeedle(ItemStack stack) {
        return SewingMachineBlockEntity.isNeedle(stack);
    }

    // The needles a design uses up: Iron for Standard bags, Diamond for Fancy and Luxury bags and the hats
    public static Item needleFor(Design design) {
        return design.line() == Design.Line.STANDARD ? ModItems.IRON_SEWING_NEEDLE.get() : ModItems.DIAMOND_SEWING_NEEDLE.get();
    }

    public static boolean isPatch(ItemStack stack) {
        for (Pattern pattern : Pattern.values()) {
            if (stack.is(pattern.patch())) return true;
        }
        return false;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new DesignerTableMenu(containerId, inventory, this, this.data, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.bag = input.getIntOr("bag", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("bag", this.bag);
    }
}
