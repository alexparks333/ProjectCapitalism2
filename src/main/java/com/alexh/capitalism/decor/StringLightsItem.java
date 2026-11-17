package com.alexh.capitalism.decor;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

// String Lights (Decorations.md): a coil of Christmas lights to string between String Light Hooks. Click a hook, then
// another holder (a hook or a Candy Cane Pole, up to SeasonalDecorBlockEntity.MAX_STRAND blocks away): a strand of twinkling
// bulbs hangs between them, sagging
// a little. Keep going hook to hook to run them across the town. Ctrl-click (or sneak-click) a holder to take down every
// strand on it and start a new strand from it. The
// strands are up and down with their hooks (Christmas).
public class StringLightsItem extends Item {

    private static final String START = "capitalism_lights_from";

    public StringLightsItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (!(level.getBlockState(pos).getBlock() instanceof SeasonalDecorBlock block) || !block.kind().isHook()) {
            if (player != null && !level.isClientSide()) {
                player.sendOverlayMessage(Component.literal("Click a String Light Hook or Candy Cane Pole to start a strand.").withStyle(ChatFormatting.GRAY));
            }
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof SeasonalDecorBlockEntity hook)) return InteractionResult.FAIL;
        // Ctrl-click (the sprint key) or sneak-click: take down every strand on this holder (its own and those strung to it)
        // and start a new strand from it
        boolean ctrl = player instanceof net.minecraft.server.level.ServerPlayer sp && sp.getLastClientInput().sprint();
        if (player != null && (ctrl || player.isSecondaryUseActive())) {
            int removed = SeasonalDecorBlockEntity.clearAllAt((net.minecraft.server.level.ServerLevel) level, pos);
            setStart(stack, pos);
            player.sendOverlayMessage(Component.literal((removed > 0 ? "Took down " + removed + (removed == 1 ? " strand" : " strands") + ". " : "")
                + "Starting from here: click another holder to string the lights to it.").withStyle(ChatFormatting.GOLD));
            return InteractionResult.SUCCESS;
        }
        BlockPos from = start(stack);
        if (from == null || from.equals(pos)) {
            setStart(stack, pos);
            if (player != null) player.sendOverlayMessage(Component.literal("Now click another hook to string the lights to it.").withStyle(ChatFormatting.GOLD));
            return InteractionResult.SUCCESS;
        }
        double distance = Math.sqrt(from.distSqr(pos));
        if (distance > SeasonalDecorBlockEntity.MAX_STRAND) {
            if (player != null) player.sendOverlayMessage(Component.literal("Too far: a strand reaches " + SeasonalDecorBlockEntity.MAX_STRAND
                + " blocks. Starting from this hook instead.").withStyle(ChatFormatting.RED));
            setStart(stack, pos);
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(from) instanceof SeasonalDecorBlockEntity start)) {
            setStart(stack, pos);
            if (player != null) player.sendOverlayMessage(Component.literal("The first hook is gone. Starting from this one.").withStyle(ChatFormatting.GRAY));
            return InteractionResult.SUCCESS;
        }
        boolean added = start.addStrand(pos);
        // carry on from here: the next click strings on from this hook
        setStart(stack, pos);
        if (player != null) player.sendOverlayMessage(Component.literal(added ? "✨ Strung! Click another holder to carry on, or Ctrl-click a holder to clear it and start from there."
            : "Those hooks are already strung.").withStyle(added ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        return InteractionResult.SUCCESS;
    }

    private static BlockPos start(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        CompoundTag tag = data.copyTag();
        return tag.contains(START) ? BlockPos.of(tag.getLongOr(START, 0L)) : null;
    }

    private static void setStart(ItemStack stack, BlockPos pos) {
        CompoundTag tag = new CompoundTag();
        tag.putLong(START, pos.asLong());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(DataComponents.CUSTOM_DATA);
    }
}
