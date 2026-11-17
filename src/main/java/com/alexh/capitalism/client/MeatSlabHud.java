package com.alexh.capitalism.client;

import com.alexh.capitalism.farmer.Freshness;
import com.alexh.capitalism.farmer.MeatSlabBlock;
import com.alexh.capitalism.farmer.MeatSlabBlockEntity;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;

// Looking at a placed Meat Slab (farmer/MeatSlabBlock): a small card under the crosshair with its name, how lean it is,
// and how long it keeps (MeatSlabBlockEntity, synced): "Fresh for 12:34", or on ice "❄ On ice: rotting 6x slower",
// "Keeps 1:14:00 here" and "(12:20 off the ice)" counting down smoothly, or "Rotten".
public final class MeatSlabHud {

    private static final float SCALE = 0.75F;

    private MeatSlabHud() {
    }

    private record Line(String text, int color) {
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos pos = blockHit.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        if (!(state.getBlock() instanceof MeatSlabBlock)) return;
        List<Line> lines = new ArrayList<>();
        lines.add(new Line(state.getBlock().getName().getString(), 0xFFFFE0B0));
        if (state.getValue(MeatSlabBlock.ROTTEN)) {
            lines.add(new Line("Rotten", 0xFF8AA060));
            lines.add(new Line("Break it for Rotten Meat", 0xFF9A9A9A));
        } else if (mc.level.getBlockEntity(pos) instanceof MeatSlabBlockEntity slab && slab.dated()) {
            int lean = slab.lean();
            int lost = Freshness.leanPenalty(lean);
            lines.add(new Line("Lean: " + lean + "%" + (lost > 0 ? " (-" + lost + " meat)" : ""), lost == 0 ? 0xFF7CE07C : lost == 1 ? 0xFFFFB040 : 0xFFFF6A5A));
            long now = mc.level.getGameTime();
            long left = slab.left(now);
            if (slab.onIce()) {
                lines.add(new Line("❄ On ice: rotting " + MeatSlabBlockEntity.ICE_SLOWER + "x slower", 0xFF7AD8FF));
                lines.add(new Line("Keeps " + Freshness.clock(left * MeatSlabBlockEntity.ICE_SLOWER) + " here", 0xFF7CE07C));
                lines.add(new Line("(" + Freshness.clock(left) + " off the ice)", 0xFF9A9A9A));
            } else {
                int color = left < 2 * Freshness.MINUTE ? 0xFFFF6A5A : left < 5 * Freshness.MINUTE ? 0xFFFFE050 : 0xFF7CE07C;
                lines.add(new Line("Fresh for " + Freshness.clock(left), color));
                lines.add(new Line("Set it on ice to keep it longer", 0xFF9A9A9A));
            }
        } else {
            lines.add(new Line("Fresh", 0xFF7CE07C));
        }
        Font font = mc.font;
        int width = 0;
        for (Line line : lines) width = Math.max(width, font.width(line.text()));
        int cx = mc.getWindow().getGuiScaledWidth() / 2;
        int y = mc.getWindow().getGuiScaledHeight() / 2 + 12;
        graphics.pose().pushMatrix();
        graphics.pose().translate(cx, y);
        graphics.pose().scale(SCALE, SCALE);
        graphics.pose().translate(-cx, -y);
        int h = lines.size() * 11 + 4;
        graphics.fill(cx - width / 2 - 5, y - 3, cx + width / 2 + 5, y + h - 2, 0x90000000);
        for (int i = 0; i < lines.size(); i++) {
            graphics.centeredText(font, lines.get(i).text(), cx, y + i * 11, lines.get(i).color());
        }
        graphics.pose().popMatrix();
    }
}
