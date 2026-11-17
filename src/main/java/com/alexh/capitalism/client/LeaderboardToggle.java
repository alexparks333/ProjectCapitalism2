package com.alexh.capitalism.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// /toggle leaderboard (client command): hides the Gold Leaderboard sidebar on your own screen, or shows it again. Only the
// Gold Leaderboard (economy/GoldLeaderboard, objective "gold_lb"): a Blood Moon's kill board still shows. The choice is
// remembered between sessions in config/capitalism-client-leaderboard.txt.
public final class LeaderboardToggle {

    private static final String OBJECTIVE = "gold_lb";
    private static Boolean hidden;

    private LeaderboardToggle() {
    }

    private static Path file() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("capitalism-client-leaderboard.txt");
    }

    public static boolean hidden() {
        if (hidden == null) {
            try {
                hidden = Files.exists(file()) && Files.readString(file()).trim().equals("hidden");
            } catch (IOException e) {
                hidden = false;
            }
        }
        return hidden;
    }

    public static void toggle() {
        hidden = !hidden();
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), hidden ? "hidden" : "shown");
        } catch (IOException ignored) {
            // still toggled for this session
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.sendSystemMessage(Component.literal("[Gold Leaderboard] ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(hidden ? "Hidden. /toggle leaderboard brings it back." : "Shown.").withStyle(ChatFormatting.WHITE)));
        }
    }

    // true when the sidebar about to be drawn is the Gold Leaderboard and the player hid it
    public static boolean shouldHideSidebar() {
        if (!hidden()) return false;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return false;
        Objective objective = minecraft.level.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR);
        return objective != null && objective.getName().equals(OBJECTIVE);
    }
}
