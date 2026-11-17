package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.player.ClientThirstData;
import com.alexh.capitalism.player.ClientWellnessData;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

public class ThirstHud {

    private static final Identifier THIRST_FULL     = Identifier.fromNamespaceAndPath(Capitalism.MODID, "hud/thirst_full");
    private static final Identifier THIRST_HALF     = Identifier.fromNamespaceAndPath(Capitalism.MODID, "hud/thirst_half");
    private static final Identifier THIRST_EMPTY    = Identifier.fromNamespaceAndPath(Capitalism.MODID, "hud/thirst_empty");
    private static final Identifier THIRST_FULL_LOW = Identifier.fromNamespaceAndPath(Capitalism.MODID, "hud/thirst_full_low");
    private static final Identifier THIRST_HALF_LOW = Identifier.fromNamespaceAndPath(Capitalism.MODID, "hud/thirst_half_low");
    private static final Identifier THIRST_FULL_GOLD = Identifier.fromNamespaceAndPath(Capitalism.MODID, "hud/thirst_full_gold");
    private static final Identifier THIRST_HALF_GOLD = Identifier.fromNamespaceAndPath(Capitalism.MODID, "hud/thirst_half_gold");
    private static final Identifier FOOD_FULL_GOLD   = Identifier.fromNamespaceAndPath(Capitalism.MODID, "hud/food_full_gold");
    private static final Identifier FOOD_HALF_GOLD   = Identifier.fromNamespaceAndPath(Capitalism.MODID, "hud/food_half_gold");

    // Below this the drops turn murky and shake, like hunger does when you're starving.
    private static final int LOW_THIRST = 3;
    // The empty slot of a bonus drop / drumstick (Excellent purity / Balanced diet) is tinted gold.
    public static final int BONUS_TINT = 0xFFFFD86A;
    private static final Identifier FOOD_EMPTY = Identifier.withDefaultNamespace("hud/food_empty");
    private static final Identifier FOOD_HALF = Identifier.withDefaultNamespace("hud/food_half");
    private static final Identifier FOOD_FULL = Identifier.withDefaultNamespace("hud/food_full");

    private static final RandomSource SHAKE_RANDOM = RandomSource.create();

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) return;
        if (!mc.gameMode.canHurtPlayer()) return;

        Player player = mc.player;
        if (player.isCreative() || player.isSpectator()) return;

        int screenWidth  = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int thirst = ClientThirstData.thirst;
        boolean low = thirst < LOW_THIRST;

        // Same trick as vanilla hunger: reseed per tick so the jitter is stable within a tick,
        // and only jitter on some ticks — the lower the thirst, the more often it shakes.
        SHAKE_RANDOM.setSeed(player.tickCount * 312871L);
        boolean shakeThisTick = low && player.tickCount % (thirst * 3 + 1) == 0;

        // Mirror hunger bar: right side, above food icons
        int xRight = screenWidth / 2 + 91;
        int baseY = screenHeight - 49;

        // 10 drops, plus an 11th on the left with Excellent purity
        // Hot (body heat over 80): the drops sweat, beads of water running off them (see sweat below)
        boolean hot = ClientWellnessData.temperatureZone() == com.alexh.capitalism.player.BodyTemperature.ZONE_HOT;
        int drops = Math.max(10, (ClientThirstData.thirstCap + 1) / 2);
        for (int i = 0; i < drops; i++) {
            int x = xRight - (i + 1) * 8;
            int y = baseY;
            if (shakeThisTick) {
                y += SHAKE_RANDOM.nextInt(3) - 1;
            }
            // sweating (Hot): the drops wobble a little, each in its own time
            if (hot && ((player.tickCount / 4 + i * 3) % 7) == 0) y -= 1;

            int dropThirst = thirst - (i * 2);

            Identifier sprite = dropThirst >= 2 ? (low ? THIRST_FULL_LOW : THIRST_FULL)
                              : dropThirst == 1 ? (low ? THIRST_HALF_LOW : THIRST_HALF)
                              : THIRST_EMPTY;

            if (i >= 10) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, THIRST_EMPTY, x, y, 7, 9, BONUS_TINT);
                if (sprite != THIRST_EMPTY) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, 7, 9);
                int goldBonus = ClientThirstData.goldThirst - (i * 2);
                if (goldBonus >= 2) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, THIRST_FULL_GOLD, x, y, 7, 9);
                else if (goldBonus == 1) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, THIRST_HALF_GOLD, x, y, 7, 9);
                continue;
            }
            // a drop lost to Low Water Purity isn't there at all (like a lost heart)
            if (i * 2 >= ClientThirstData.thirstCap) continue;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, 7, 9);

            // Golden drops cover the bar from the right, on top of the normal drops.
            int goldDrop = ClientThirstData.goldThirst - (i * 2);
            if (goldDrop >= 2) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, THIRST_FULL_GOLD, x, y, 7, 9);
            } else if (goldDrop == 1) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, THIRST_HALF_GOLD, x, y, 7, 9);
            }
        }
        if (hot) sweat(graphics, xRight, baseY, Math.min(drops, Math.max(1, (thirst + 1) / 2)), player.tickCount + delta.getGameTimeDeltaPartialTick(false));
    }

    // Sweat (Hot): every few drops a bead forms at the bottom of a drop and runs down, fading as it goes. Each drop has
    // its own timing, so they drip one after another. `filled` = how many drops still hold water.
    private static void sweat(GuiGraphicsExtractor graphics, int xRight, int baseY, int filled, float time) {
        for (int i = 0; i < filled; i++) {
            float cycle = ((time + i * 11.3f) % 26f) / 26f;   // 0..1, ~1.3 s per drip
            if (cycle > 0.8f) continue;                      // a short pause before the next bead
            float t = cycle / 0.8f;
            int x = xRight - (i + 1) * 8 + 3;
            int y = baseY + 8 + (int) (t * 7);
            int alpha = (int) (230 * (1 - t));
            int bead = (alpha << 24) | 0x9ED8FF;
            graphics.fill(x, y, x + 1, y + 2, bead);
            if (t < 0.35f) graphics.fill(x, y - 1, x + 1, y, (alpha / 2 << 24) | 0xDFF4FF);   // the shine on top
        }
    }

    // Golden drumsticks: drawn right after vanilla's food bar, on top of it, filling from the right.
    // Drumsticks lost to a Poor diet are crossed out (red X) on the left end; a Balanced diet adds an 11th on the left.
    public static void renderGoldFood(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) return;
        if (!mc.gameMode.canHurtPlayer()) return;
        if (mc.player.isCreative() || mc.player.isSpectator()) return;

        int gold = ClientThirstData.goldFood;
        int foodCap = ClientThirstData.foodCap;
        if (gold <= 0 && foodCap == 20) return;
        int foodLevel = mc.player.getFoodData().getFoodLevel();

        // Same spot vanilla draws the food bar
        int xRight = mc.getWindow().getGuiScaledWidth() / 2 + 91;
        int y = mc.getWindow().getGuiScaledHeight() - 39;
        int icons = Math.max(10, (foodCap + 1) / 2);
        for (int i = 0; i < icons; i++) {
            int x = xRight - i * 8 - 9;
            if (i >= 10) {
                // bonus drumstick (vanilla only draws 10)
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FOOD_EMPTY, x, y, 9, 9, BONUS_TINT);
                int left = foodLevel - i * 2;
                if (left >= 2) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FOOD_FULL, x, y, 9, 9);
                else if (left == 1) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FOOD_HALF, x, y, 9, 9);
                int goldBonus = gold - (i * 2);
                if (goldBonus >= 2) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FOOD_FULL_GOLD, x, y, 9, 9);
                else if (goldBonus == 1) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FOOD_HALF_GOLD, x, y, 9, 9);
                continue;
            }
            // lost to a Poor diet: not there at all (renderShortFood draws the shorter bar)
            if (i * 2 >= foodCap) continue;
            int goldPoints = gold - (i * 2);
            if (goldPoints >= 2) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FOOD_FULL_GOLD, x, y, 9, 9);
            } else if (goldPoints == 1) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FOOD_HALF_GOLD, x, y, 9, 9);
            }
        }
    }

    private static final Identifier FOOD_EMPTY_HUNGER = Identifier.withDefaultNamespace("hud/food_empty_hunger");
    private static final Identifier FOOD_HALF_HUNGER = Identifier.withDefaultNamespace("hud/food_half_hunger");
    private static final Identifier FOOD_FULL_HUNGER = Identifier.withDefaultNamespace("hud/food_full_hunger");

    // The food bar when a Poor diet took a drumstick away: vanilla's bar, but only as many drumsticks as the bar can hold
    // (the lost one simply isn't there). Like vanilla: the Hunger effect's green look, and the shake when saturation is
    // gone. Not drawn while riding something with its own hearts (vanilla shows those there instead).
    public static void renderShortFood(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null || !mc.gameMode.canHurtPlayer()) return;
        if (mc.player.isCreative() || mc.player.isSpectator()) return;
        if (mc.player.getVehicle() instanceof net.minecraft.world.entity.LivingEntity) return;
        var food = mc.player.getFoodData();
        int level = food.getFoodLevel();
        boolean hunger = mc.player.hasEffect(net.minecraft.world.effect.MobEffects.HUNGER);
        int icons = (ClientThirstData.foodCap + 1) / 2;
        int xRight = mc.getWindow().getGuiScaledWidth() / 2 + 91;
        int baseY = mc.getWindow().getGuiScaledHeight() - 39;
        SHAKE_RANDOM.setSeed(mc.player.tickCount * 312871L);
        boolean shake = food.getSaturationLevel() <= 0.0F && mc.player.tickCount % (level * 3 + 1) == 0;
        for (int i = 0; i < icons; i++) {
            int x = xRight - i * 8 - 9;
            int y = baseY + (shake ? SHAKE_RANDOM.nextInt(3) - 1 : 0);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, hunger ? FOOD_EMPTY_HUNGER : FOOD_EMPTY, x, y, 9, 9);
            int left = level - i * 2;
            if (left >= 2) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, hunger ? FOOD_FULL_HUNGER : FOOD_FULL, x, y, 9, 9);
            else if (left == 1) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, hunger ? FOOD_HALF_HUNGER : FOOD_HALF, x, y, 9, 9);
        }
    }

    // A red X over an icon slot `width` wide (9 for hearts and drumsticks, 7 for water drops): something you've lost.
    public static void drawX(GuiGraphicsExtractor graphics, int x, int y, int width) {
        int x0 = x + (width - 7) / 2;
        for (int i = 0; i < 7; i++) {
            int color = (i == 0 || i == 6) ? 0xFF8A1010 : 0xFFE0302A;
            graphics.fill(x0 + i, y + 1 + i, x0 + i + 1, y + 2 + i, color);
            graphics.fill(x0 + 6 - i, y + 1 + i, x0 + 7 - i, y + 2 + i, color);
        }
    }

}
