package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.ModAttachments;
import com.alexh.capitalism.deathtax.DeathTax;
import com.alexh.capitalism.deathtax.DeathTaxData;
import com.alexh.capitalism.economy.GoldMoney;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// The death tax board (deathtax/DeathTax, Insurance.md): a small black board in the style of the day boards (the clock,
// the day cards), as a slim strip at the top middle of the screen ("☠ Death Tax: 8 gold · /pay death"), sliding in after a respawn or
// join, staying while they owe and going when they've paid (no big card: it was too much in the way).
// Only drawn: it never takes the mouse or a key. Also keeps the skull by other players' names up to date: their In
// Purgatory flag arrives by sync, and a name is worked out once and kept, so it's worked out again when it changes.
@EventBusSubscriber(modid = Capitalism.MODID, value = Dist.CLIENT)
public final class PurgatoryHud {

    private static final long SHRINK_MS = 350;
    private static final int EDGE = 0xFF050505;
    private static final int RIM = 0xFF4A4A4A;
    private static final int LINE = 0xFF8A1A1A;
    private static final int BOARD = 0xF0121212;
    private static final int RED = 0xFFFF5050;
    private static final int CHALK = 0xFFEDE6DA;
    private static final int DIM = 0xFFA09890;
    private static final int GOLD = 0xFFE8B850;

    private static final Map<UUID, Boolean> SEEN = new HashMap<>();
    private static long bigAt = -SHRINK_MS * 2;
    private static Player lastPlayer;
    private static int bossBottom;
    private static long bossSeenAt;

    private PurgatoryHud() {
    }

    // (network/DeathTaxPopupPacket: they've just respawned owing) show it big for a few seconds
    public static void showBig() {
        bigAt = System.currentTimeMillis();
    }

    // where the boss bars end this frame (the board goes under them)
    @SubscribeEvent
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        bossBottom = event.getY() + event.getIncrement();
        bossSeenAt = System.currentTimeMillis();
    }

    private static int top() {
        return System.currentTimeMillis() - bossSeenAt < 200 ? bossBottom + 2 : 4;
    }

    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        DeathTaxData d = DeathTax.data(mc.player);
        long owed = d.owed();
        if (mc.player != lastPlayer) {
            lastPlayer = mc.player;
            if (owed > 0) showBig();
        }
        if (owed <= 0) return;
        Font font = mc.font;
        int sw = mc.getWindow().getGuiScaledWidth(), sh = mc.getWindow().getGuiScaledHeight();
        long age = System.currentTimeMillis() - bigAt;
        // a slim strip at the top middle (sliding in from above after a respawn or join)
        float in = Mth.clamp(age / (float) SHRINK_MS, 0.0F, 1.0F);
        String line = "☠ Death Tax: " + GoldMoney.format(owed) + " · /pay death";
        int w = font.width(line) + 12, h = 13;
        int x = (sw - w) / 2;
        int y = (int) (top() - (1 - in) * (h + 6));
        board(g, x, y, w, h);
        int lx = x + 6;
        g.text(font, "☠", lx, y + 3, RED, false);
        String rest = line.substring(1);
        g.text(font, rest, lx + font.width("☠"), y + 3, CHALK, false);
    }

    // a black board: a dark edge, a grey rim, a thin red line inside
    private static void board(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, EDGE);
        g.fill(x, y, x + w, y + h, RIM);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, LINE);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, BOARD);
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            SEEN.clear();
            return;
        }
        if (mc.level.getGameTime() % 10 != 0) return;
        for (Player player : mc.level.players()) {
            boolean in = player.getData(ModAttachments.IN_PURGATORY.get());
            Boolean was = SEEN.put(player.getUUID(), in);
            if (was == null ? in : was != in) player.refreshDisplayName();
        }
    }
}
