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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

// The Sewing Machine (Designer.md > Sewing Machine): pick a pattern (Pattern), put its materials in the slots under it
// (they show as ghosts) and Sewing Needles in (Pattern.needle: Iron for Generic, Diamond for Fancy and Luxury), then sew the
// materials on ONE AT A TIME, in order, each with the needle game (client/SewingGame). Touching the edge of the seam
// ends the try: a needle breaks, the material is kept and that spot isn't done (sew it again). When every material is on, the Patch is done
// (into the output slot) and the machine is ready to sew the same pattern again. The pattern can only be changed before
// anything is sewn, or after unpicking (what was sewn on is lost).
public class SewingMachineBlockEntity extends DesignerBlockEntity {

    public static final int MATERIALS = 0;
    public static final int NEEDLE = Pattern.MAX_STITCHES;
    public static final int OUTPUT = NEEDLE + 1;
    public static final int SIZE = OUTPUT + 1;

    // -1: no pattern chosen
    private int pattern = -1;
    // which of the pattern's materials are sewn on (bit i = stitch i)
    private int sewn;
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? SewingMachineBlockEntity.this.pattern : SewingMachineBlockEntity.this.sewn;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) SewingMachineBlockEntity.this.pattern = value;
            else SewingMachineBlockEntity.this.sewn = value;
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public SewingMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SEWING_MACHINE.get(), pos, state, "Sewing Machine", SIZE);
    }

    public @Nullable Pattern pattern() {
        return this.pattern < 0 ? null : Pattern.byIndex(this.pattern);
    }

    public int sewn() {
        return this.sewn;
    }

    // Choose what to sew (only while nothing is sewn on yet)
    public void choose(int index) {
        if (this.sewn != 0 || index < 0 || index >= Pattern.values().length) return;
        this.pattern = index;
        this.setChanged();
    }

    // Unpick the patch being sewn: start again (what was sewn on is lost)
    public void unpick() {
        this.sewn = 0;
        this.setChanged();
    }

    // The material to sew next (-1 if there's no pattern or it's all on)
    public int next() {
        Pattern pattern = this.pattern();
        if (pattern == null) return -1;
        for (int i = 0; i < pattern.stitches.size(); i++) {
            if ((this.sewn & (1 << i)) == 0) return i;
        }
        return -1;
    }

    // Why it can't sew right now, or null if it can (the screen shows it; the server checks it again)
    public static @Nullable String problem(@Nullable Pattern pattern, int next, ItemStack material, ItemStack needle, ItemStack output) {
        if (pattern == null) return "Pick a pattern";
        if (next < 0) return "Take the patch out";
        if (!pattern.takesNeedle(needle)) return pattern.tier == Pattern.Tier.GENERIC ? "Needs Iron needles" : "Needs Diamond needles";
        if (!pattern.stitches.get(next).matches(material)) return "Needs " + pattern.stitches.get(next).name();
        // only the last material finishes the patch, so only it needs room in the output
        boolean last = next == pattern.stitches.size() - 1;
        if (last && !output.isEmpty() && (!output.is(pattern.patch()) || output.getCount() >= output.getMaxStackSize())) {
            return "Take the " + output.getHoverName().getString() + " out";
        }
        return null;
    }

    public @Nullable String problem() {
        int next = this.next();
        return problem(this.pattern(), next, next < 0 ? ItemStack.EMPTY : this.items.get(MATERIALS + next), this.items.get(NEEDLE), this.items.get(OUTPUT));
    }

    // The needle game for the next material is over. A clean seam sews it on (the material is used; the last one finishes
    // the Patch). A slip (the game stops at the first one) breaks one needle, even in creative; the material isn't used
    // and that spot still needs sewing.
    public void sewn(ServerPlayer player, int mistakes) {
        if (this.problem() != null || !(this.level instanceof ServerLevel level)) return;
        Pattern pattern = this.pattern();
        int next = this.next();
        ItemStack material = this.items.get(MATERIALS + next);
        BlockPos pos = this.getBlockPos();
        if (mistakes > 0) {
            // a slip: the needle breaks (even in creative), the material stays, and that spot still needs sewing
            this.items.get(NEEDLE).shrink(1);
            player.sendOverlayMessage(Component.literal("A needle broke! Sew the " + pattern.stitches.get(next).name() + " again.").withStyle(ChatFormatting.RED));
            level.playSound(null, pos, SoundEvents.ITEM_BREAK.value(), SoundSource.BLOCKS, 0.7F, 1.3F);
        } else {
            material.shrink(1);
            this.sewn |= 1 << next;
            level.playSound(null, pos, SoundEvents.UI_LOOM_SELECT_PATTERN, SoundSource.BLOCKS, 0.9F, 1.2F);
            if (this.next() < 0) {
                ItemStack out = this.items.get(OUTPUT);
                if (out.isEmpty()) this.items.set(OUTPUT, new ItemStack(pattern.patch()));
                else out.grow(1);
                this.sewn = 0;
                level.playSound(null, pos, SoundEvents.UI_LOOM_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 1.0F);
                player.sendOverlayMessage(Component.literal(new ItemStack(pattern.patch()).getHoverName().getString() + " finished!").withStyle(ChatFormatting.GOLD));
            }
        }
        this.setChanged();
    }

    // how many needles are in the machine
    public int getNeedles() {
        return this.items.get(NEEDLE).getCount();
    }

    public static boolean isNeedle(ItemStack stack) {
        return stack.is(ModItems.IRON_SEWING_NEEDLE.get()) || stack.is(ModItems.DIAMOND_SEWING_NEEDLE.get());
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == NEEDLE) return isNeedle(stack);
        if (slot == OUTPUT) return false;
        Pattern pattern = this.pattern();
        return pattern != null && slot - MATERIALS < pattern.stitches.size() && pattern.stitches.get(slot - MATERIALS).matches(stack);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new SewingMachineMenu(containerId, inventory, this, this.data, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.pattern = input.getIntOr("pattern", -1);
        this.sewn = input.getIntOr("sewn", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("pattern", this.pattern);
        output.putInt("sewn", this.sewn);
    }
}
