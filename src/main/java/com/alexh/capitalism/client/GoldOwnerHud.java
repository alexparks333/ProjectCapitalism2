package com.alexh.capitalism.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

// Looking at gold someone dropped (deathtax/GoldCollector names it "Dev's gold", name tag hidden) shows whose it is in
// small text under the crosshair. Item entities can't be targeted by the crosshair, so this traces the look ray itself.
public final class GoldOwnerHud {

    private static final double REACH = 6.0;

    private GoldOwnerHud() {
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        Vec3 eye = mc.player.getEyePosition();
        Vec3 end = eye.add(mc.player.getViewVector(1.0F).scale(REACH));
        // a block in the way hides it
        double limit = REACH * REACH;
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) limit = mc.hitResult.getLocation().distanceToSqr(eye);

        Component name = null;
        double best = limit;
        for (ItemEntity item : mc.level.getEntitiesOfClass(ItemEntity.class, new AABB(eye, end).inflate(1.0))) {
            Component custom = item.getCustomName();
            if (custom == null || !custom.getString().endsWith("'s gold")) continue;
            var hit = item.getBoundingBox().inflate(0.2).clip(eye, end);
            if (hit.isEmpty()) continue;
            double d = hit.get().distanceToSqr(eye);
            if (d < best) {
                best = d;
                name = custom;
            }
        }
        if (name == null) return;

        String text = name.getString();
        int w = mc.font.width(text);
        int cx = mc.getWindow().getGuiScaledWidth() / 2;
        int y = mc.getWindow().getGuiScaledHeight() / 2 + 10;
        graphics.fill(cx - w / 2 - 3, y - 2, cx + w / 2 + 3, y + 10, 0x80000000);
        graphics.centeredText(mc.font, text, cx, y, 0xFFFFC040);
    }
}
