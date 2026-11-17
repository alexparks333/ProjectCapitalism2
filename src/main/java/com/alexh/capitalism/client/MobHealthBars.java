package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Health bars over mobs that get hurt (NPCs.md > Health bars). When a mob (or another player) loses health, a little
// bar floats over its head for a few seconds:
//  - a red heart, then the bar in a dark frame with rounded corners: green, yellow under half, red under a quarter, a
//    shine along its top (kept to a few rectangles: each one is sorted every frame, and many mobs add up);
//  - what a hit takes first flashes pale and then drains away behind the bar (like a fighting game), so you see the hit;
//  - the health in numbers after it ("14").
// Each hit pops out a number that springs up big, settles, arcs up and fades: red "-4.5", a gold "-9!" for a big hit (a
// third of its health or more), "☠ -6" for the killing blow; healing is a green "+2". Client only: every mob's health is
// already sent to everyone. Watched each tick (onTick); drawn as part of the world (onRender, after the mob), facing you.
@EventBusSubscriber(modid = Capitalism.MODID, value = Dist.CLIENT)
public final class MobHealthBars {

    private static final int SHOW_TICKS = 100;        // how long the bar stays after the last hit (5 s)
    private static final int FADE_TICKS = 15;
    private static final int NUMBER_TICKS = 26;       // how long a damage number floats
    private static final int TRAIL_DELAY = 8;         // ticks the drained part waits before it slides away
    private static final double RANGE = 32.0;
    private static final float BAR_W = 36.0F;         // in the bar's own units (1/40 of a block)
    private static final float BAR_H = 5.0F;

    // what we know about one mob: its health last tick, the trailing health behind the bar, when it last changed, its
    // floating numbers
    private static final class Watch {
        float health;
        float trail;
        long lastChange = Long.MIN_VALUE / 2;
        final List<Pop> pops = new ArrayList<>();
    }

    // a floating number: how much, when, which way it drifts, and what kind
    private record Pop(float amount, long born, float drift, boolean big, boolean kill) {
    }

    private static final Map<Integer, Watch> WATCHED = new HashMap<>();
    private static final ContextKey<Bar> BAR = new ContextKey<>(Identifier.fromNamespaceAndPath(Capitalism.MODID, "health_bar"));

    // what's drawn, on the mob's render state
    public record Bar(float health, float trail, float max, float alpha, List<Pop> pops, float now, float height) {
    }

    private MobHealthBars() {
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            WATCHED.clear();
            return;
        }
        long now = mc.level.getGameTime();
        Map<Integer, Boolean> seen = new HashMap<>();
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof LivingEntity living) || living == mc.player || living instanceof ArmorStand) continue;
            if (living.distanceToSqr(mc.player) > RANGE * RANGE) continue;
            seen.put(living.getId(), true);
            float health = Math.max(0.0F, living.getHealth());
            Watch w = WATCHED.get(living.getId());
            if (w == null) {
                w = new Watch();
                w.health = health;
                w.trail = health;
                WATCHED.put(living.getId(), w);
                continue;
            }
            float change = health - w.health;
            if (Math.abs(change) >= 0.05F) {
                w.lastChange = now;
                float max = Math.max(1.0F, living.getMaxHealth());
                boolean kill = health <= 0.0F;
                boolean big = change < 0 && -change >= max / 3.0F;
                w.pops.add(new Pop(change, now, (living.getRandom().nextFloat() - 0.5F) * 18.0F, big, kill));
                w.health = health;
            }
            // the drained part waits a moment, then slides down to the health; healing jumps straight up
            if (w.trail < w.health) w.trail = w.health;
            else if (w.trail > w.health && now - w.lastChange > TRAIL_DELAY) {
                w.trail = Math.max(w.health, w.trail - Math.max(0.25F, (w.trail - w.health) * 0.18F));
            }
            w.pops.removeIf(p -> now - p.born() > NUMBER_TICKS);
        }
        WATCHED.keySet().removeIf(id -> !seen.containsKey(id));
    }

    // with the mob's render state (ClientModEvents registers this for every living thing): a bar if it was hurt lately
    public static void extract(LivingEntity entity, LivingEntityRenderState state, float partialTick) {
        Watch w = WATCHED.get(entity.getId());
        Minecraft mc = Minecraft.getInstance();
        if (w == null || mc.level == null) return;
        float now = mc.level.getGameTime() + partialTick;
        float since = now - w.lastChange;
        if (since > SHOW_TICKS && w.pops.isEmpty()) return;
        float alpha = since <= SHOW_TICKS - FADE_TICKS ? 1.0F : Mth.clamp((SHOW_TICKS - since) / FADE_TICKS, 0.0F, 1.0F);
        if (entity.isDeadOrDying()) alpha = 0.0F;
        state.setRenderData(BAR, new Bar(Math.max(0.0F, entity.getHealth()), w.trail, entity.getMaxHealth(), alpha, List.copyOf(w.pops), now,
            state.boundingBoxHeight));
    }

    @SubscribeEvent
    public static void onRender(RenderLivingEvent.Post<?, ?, ?> event) {
        LivingEntityRenderState state = event.getRenderState();
        Bar bar = state.getRenderData(BAR);
        if (bar == null) return;
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        PoseStack pose = event.getPoseStack();
        SubmitNodeCollector out = event.getSubmitNodeCollector();
        // above the head, and above its name if it has one showing
        // (above the level mark too, when that shows: client/MobLevels)
        float lift = bar.height() + (state.nameTag != null ? 0.75F : 0.5F) + (state.getRenderData(MobLevels.LEVEL) != null ? 0.3F : 0.0F);
        pose.pushPose();
        pose.translate(0.0F, lift, 0.0F);
        pose.rotate(mc.gameRenderer.mainCamera().rotation());
        pose.scale(0.025F, -0.025F, 0.025F);
        if (bar.alpha() > 0.02F) drawBar(bar, font, pose, out);
        for (Pop p : bar.pops()) drawPop(p, bar.now(), font, pose, out);
        pose.popPose();
    }

    private static void drawBar(Bar bar, Font font, PoseStack pose, SubmitNodeCollector out) {
        float a = bar.alpha();
        float max = Math.max(0.001F, bar.max());
        float share = Mth.clamp(bar.health() / max, 0.0F, 1.0F);
        float trail = Mth.clamp(bar.trail() / max, share, 1.0F);
        int color = share > 0.5F ? 0x5BE05B : share > 0.25F ? 0xF0C83C : 0xE84848;
        String number = number(bar.health());
        // laid out round the middle: heart, bar, number
        float heartW = 7.0F;
        float numberW = font.width(number) * 0.7F;
        float total = heartW + 2 + BAR_W + 3 + numberW;
        float x0 = -total / 2 + heartW + 2;
        float x1 = x0 + BAR_W;
        // the frame: dark, its corners cut so it reads rounded
        rect(out, pose, x0, -1, x1, BAR_H + 1, ARGB.color(a * 0.9F, 0x0E0A0A));
        rect(out, pose, x0 - 1, 0, x1 + 1, BAR_H, ARGB.color(a * 0.9F, 0x0E0A0A));
        layer(pose);
        rect(out, pose, x0, 0, x1, BAR_H, ARGB.color(a * 0.9F, 0x3A1414));
        layer(pose);
        // what the last hits took, pale, draining behind
        if (trail > share) rect(out, pose, x0 + BAR_W * share, 0, x0 + BAR_W * trail, BAR_H, ARGB.color(a, 0xFFF2C8));
        layer(pose);
        if (share > 0) {
            float fx = x0 + BAR_W * share;
            rect(out, pose, x0, 0, fx, BAR_H, ARGB.color(a, color));
            layer(pose);
            rect(out, pose, x0, 0, fx, 1.2F, ARGB.color(a * 0.55F, 0xFFFFFF));                  // shine
        }
        layer(pose);
        // the heart, and the number
        text(out, pose, "❤", x0 - 2 - heartW, -1.5F, 0.85F, ARGB.color(a, 0xFF4A5A), ARGB.color(a, 0x2A0A0A));
        text(out, pose, number, x1 + 3, 0.0F, 0.7F, ARGB.color(a, 0xFFFFFF), ARGB.color(a, 0x101010));
    }

    // a hit's number: springs up big, settles, arcs up and fades
    private static void drawPop(Pop p, float now, Font font, PoseStack pose, SubmitNodeCollector out) {
        float t = (now - p.born()) / NUMBER_TICKS;
        if (t < 0 || t > 1) return;
        float fade = t < 0.6F ? 1.0F : 1.0F - (t - 0.6F) / 0.4F;
        float spring = t < 0.12F ? 1.7F - (t / 0.12F) * 0.7F : 1.0F + 0.08F * Mth.sin((t - 0.12F) * 18.0F) * (1.0F - t);
        float scale = (p.big() || p.kill() ? 1.25F : 1.0F) * spring;
        float rise = 1.0F - (1.0F - t) * (1.0F - t);
        String text;
        int color;
        if (p.amount() > 0) {
            text = "+" + number(p.amount());
            color = 0x70FF70;
        } else if (p.kill()) {
            text = "☠ -" + number(-p.amount());
            color = 0xFFFFFF;
        } else if (p.big()) {
            text = "-" + number(-p.amount()) + "!";
            color = 0xFFC830;
        } else {
            text = "-" + number(-p.amount());
            color = 0xFF5A5A;
        }
        pose.pushPose();
        pose.translate(p.drift() * (0.3F + rise), -9.0F - rise * 20.0F, 1.5F);
        pose.scale(scale, scale, scale);
        out.submitText(pose, -font.width(text) / 2.0F, -4.0F, Component.literal(text).getVisualOrderText(), false, Font.DisplayMode.NORMAL,
            0xF000F0, ARGB.color(Math.max(fade, 0.05F), color), 0, ARGB.color(Math.max(fade, 0.05F), 0x1A0505));
        pose.popPose();
    }

    private static void rect(SubmitNodeCollector out, PoseStack pose, float x0, float y0, float x1, float y1, int argb) {
        out.submitTextBackground(pose, x0, y0, x1, y1, argb, Font.DisplayMode.NORMAL, 0xF000F0);
    }

    // a hair toward the camera, so each layer sits on top of the last. Facing the camera, +z is toward it (the camera
    // looks down its -z); these used to step -z, behind the dark frame, so the bar looked empty.
    private static void layer(PoseStack pose) {
        pose.translate(0.0F, 0.0F, 0.12F);
    }

    // small outlined text, its top left at (x, y), scaled
    private static void text(SubmitNodeCollector out, PoseStack pose, String text, float x, float y, float scale, int color, int outline) {
        pose.pushPose();
        pose.translate(x, y, 0.0F);
        pose.scale(scale, scale, scale);
        out.submitText(pose, 0, 0, Component.literal(text).getVisualOrderText(), false, Font.DisplayMode.NORMAL, 0xF000F0, color, 0, outline);
        pose.popPose();
    }

    // "4", "4.5" (half hearts are halves)
    private static String number(float value) {
        float rounded = Math.round(value * 10.0F) / 10.0F;
        return rounded == (int) rounded ? Integer.toString((int) rounded) : Float.toString(rounded);
    }
}
