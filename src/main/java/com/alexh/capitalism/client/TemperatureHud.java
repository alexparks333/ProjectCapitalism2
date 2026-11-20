package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.player.BodyTemperature;
import com.alexh.capitalism.player.ClientWellnessData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.PlayerHeartTypeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.ArrayList;
import java.util.List;

// Body heat (player/BodyTemperature), a ball in the bottom right of the screen with the % in small text to its left
// (after Tough As Nails' gauge): a snowflake (Freezing), a blue drop (Cold), green (Comfortable), a flame (Hot).
// It shows in the inventory too, where hovering it says how you feel and what's doing it. While Freezing, your hearts
// turn frosty. The Wellness screen's Temperature card uses the same pieces.
@EventBusSubscriber(modid = Capitalism.MODID, value = net.neoforged.api.distmarker.Dist.CLIENT)
public final class TemperatureHud {

    private static final Identifier[] BALLS = {
        Identifier.fromNamespaceAndPath(Capitalism.MODID, "hud/temp_freezing"),
        Identifier.fromNamespaceAndPath(Capitalism.MODID, "hud/temp_cold"),
        Identifier.fromNamespaceAndPath(Capitalism.MODID, "hud/temp_normal"),
        Identifier.fromNamespaceAndPath(Capitalism.MODID, "hud/temp_hot")};
    public static final int SIZE = 16;
    private static final String[] NAMES = {"Freezing", "Cold", "Comfortable", "Hot"};
    private static final int[] COLORS = {0xFF9FD4FF, 0xFFB6E2F6, 0xFF9BEA8E, 0xFFFFB55A};
    private static final String[] ADVICE = {
        "You're losing health! Get to a fire, fast.",
        "Warm up before you freeze: a fire, shelter or leather armor.",
        "",
        "Your thirst drains faster. Find shade or water."};

    private TemperatureHud() {
    }

    public static String name(int zone) {
        return NAMES[zone];
    }

    public static int color(int zone) {
        return COLORS[zone];
    }

    // What's acting on you, one short line each
    public static List<String> reasons(int mask) {
        List<String> out = new ArrayList<>();
        boolean hotPlace = (mask & (BodyTemperature.R_NETHER | BodyTemperature.R_SUN)) != 0;
        if ((mask & BodyTemperature.R_POWDER_SNOW) != 0) out.add("You're stuck in powder snow!");
        if ((mask & BodyTemperature.R_COLD_WATER) != 0) out.add("The water is ice cold.");
        if ((mask & BodyTemperature.R_SNOWY) != 0) out.add("It's bitter cold out here.");
        if ((mask & BodyTemperature.R_ON_ICE) != 0) out.add("The ice under your feet is freezing.");
        if ((mask & BodyTemperature.R_CLOSE_HEAT) != 0) out.add("The flames right next to you are hot!");
        if ((mask & BodyTemperature.R_RAIN) != 0) out.add("The rain is soaking you.");
        if ((mask & BodyTemperature.R_NETHER) != 0) out.add("The Nether is scorching.");
        if ((mask & BodyTemperature.R_SUN) != 0) out.add("The sun is beating down.");
        if ((mask & BodyTemperature.R_WATER_COOLING) != 0) out.add("The water cools you off.");
        if ((mask & BodyTemperature.R_FIRE) != 0) out.add(hotPlace ? "The fire makes it even hotter." : "A fire is warming you.");
        if ((mask & BodyTemperature.R_FIREPLACE) != 0) out.add(hotPlace ? "The fireplace makes it even hotter." : "A fireplace is warming the room.");
        if ((mask & BodyTemperature.R_LEATHER) != 0) out.add("Your leather armor keeps the cold out.");
        if ((mask & BodyTemperature.R_WINTER_SURVIVAL) != 0) out.add("Winter Survival: the cold can't touch you.");
        if ((mask & BodyTemperature.R_HOT_PEPPER) != 0) out.add("Hot Pepper: the cold can't touch you.");
        if ((mask & BodyTemperature.R_NO_HEAT) != 0) out.add("The heat can't touch you.");
        if ((mask & BodyTemperature.R_COMFORTED) != 0) out.add("Comfort Soup: you're perfectly comfortable.");
        if ((mask & BodyTemperature.R_WARMED_UP) != 0) out.add("You're warmed up: the cold can't reach you.");
        if ((mask & BodyTemperature.R_COOLED_OFF) != 0) out.add("Frosty Melon Sorbet keeps you cool.");
        if ((mask & BodyTemperature.R_HAND_WARMER) != 0) out.add("Your Hand Warmer keeps the cold off.");
        if ((mask & BodyTemperature.R_WET) != 0) out.add("Your leather is soaked. Dry off by a fire.");
        if ((mask & BodyTemperature.R_LEATHER_HOT) != 0) out.add("Your leather armor is baking you.");
        if ((mask & BodyTemperature.R_ICE) != 0) out.add(hotPlace ? "Your Ice socket keeps you cool." : "Your Ice socket makes the cold bite harder.");
        if ((mask & BodyTemperature.R_BLAZE) != 0) out.add(hotPlace ? "Your Blaze socket makes you burn." : "Your Blaze socket keeps the cold out.");
        return out;
    }

    // The hover: "Temperature: Cold (43°)", what's doing it, and what to do
    public static List<Component> tooltip() {
        float heat = ClientWellnessData.bodyHeat;
        int zone = BodyTemperature.zone(heat);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("Temperature: " + NAMES[zone] + " (" + Math.round(heat) + "°)").withColor(COLORS[zone]));
        for (String reason : reasons(ClientWellnessData.heatReasons)) lines.add(Component.literal(reason).withStyle(ChatFormatting.GRAY));
        if (!ADVICE[zone].isEmpty()) lines.add(Component.literal(ADVICE[zone]).withStyle(ChatFormatting.WHITE));
        return lines;
    }

    public static void drawBall(GuiGraphicsExtractor graphics, int x, int y, int zone) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BALLS[zone], x, y, SIZE, SIZE);
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isCreative() || mc.player.isSpectator()) return;
        draw(graphics, mc);
    }

    // The ball (it shivers while Freezing) and the % in small text to its left
    private static void draw(GuiGraphicsExtractor graphics, Minecraft mc) {
        int zone = ClientWellnessData.temperatureZone();
        int x = mc.getWindow().getGuiScaledWidth() - SIZE - 4;
        int y = mc.getWindow().getGuiScaledHeight() - SIZE - 4;
        int shake = zone == BodyTemperature.ZONE_FREEZING && mc.player != null && (mc.player.tickCount / 2) % 4 == 0 ? 1 : 0;
        drawBall(graphics, x + shake, y, zone);
        String pct = Math.round(ClientWellnessData.bodyHeat) + "°";
        float scale = 0.75F;
        int textX = x - 2;
        int textY = y + SIZE / 2;
        graphics.pose().pushMatrix();
        graphics.pose().translate(textX, textY);
        graphics.pose().scale(scale, scale);
        graphics.text(mc.font, pct, -mc.font.width(pct), -4, COLORS[zone], true);
        graphics.pose().popMatrix();
    }

    // The inventory covers the HUD, so the ball is drawn on it too.
    @SubscribeEvent
    public static void onScreen(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isCreative() || mc.player.isSpectator()) return;
        draw(event.getGuiGraphics(), mc);
    }

    // The hover: set during Foreground, because Post comes after the screen has drawn its tooltips for the frame.
    @SubscribeEvent
    public static void onForeground(ScreenEvent.Render.Foreground event) {
        if (!(event.getScreen() instanceof InventoryScreen)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isCreative() || mc.player.isSpectator()) return;
        int x = mc.getWindow().getGuiScaledWidth() - SIZE - 4;
        int y = mc.getWindow().getGuiScaledHeight() - SIZE - 4;
        int mx = event.getMouseX();
        int my = event.getMouseY();
        // the ball and the % beside it
        if (mx < x - 24 || mx > x + SIZE || my < y - 1 || my > y + SIZE) return;
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (Component line : tooltip()) lines.add(line.getVisualOrderText());
        event.getGuiGraphics().setTooltipForNextFrame(mc.font, lines, mx, my);
    }

    // Freezing: frosty hearts (vanilla's frozen ones)
    @SubscribeEvent
    public static void onHeartType(PlayerHeartTypeEvent event) {
        if (event.getEntity() == Minecraft.getInstance().player && event.getType() == Hud.HeartType.NORMAL
            && ClientWellnessData.temperatureZone() == BodyTemperature.ZONE_FREEZING) {
            event.setType(Hud.HeartType.FROZEN);
        }
    }
}
