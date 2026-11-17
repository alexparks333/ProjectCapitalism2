package com.alexh.capitalism.core;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.network.RulesPacket;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// The server's rules (the General Guide's Rules page, Economy.md > In-game: the General Guide): a plain text file the
// admin edits, config/capitalism-rules.txt, one rule per line (blank lines and lines starting with # are skipped; the
// guide numbers them). Made with the defaults below if it's missing. Read again every time someone logs in and sent to
// them, so an edit shows up at the next login without a restart.
@EventBusSubscriber(modid = Capitalism.MODID)
public final class ServerRules {

    public static final List<String> DEFAULTS = List.of(
        "No griefing or stealing. Take only what the game itself lets you take: locks, lots and shops are there for a reason.",
        "No hacked clients and NO X-ray mods (we can see if you have it).",
        "NO AFK farming or fishing (we can also tell if you're exploiting this).",
        "No creating mob farms!",
        "Found a bug? Don't exploit it: report it to an admin."
    );

    private ServerRules() {
    }

    public static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("capitalism-rules.txt");
    }

    public static List<String> load() {
        Path file = file();
        try {
            if (!Files.exists(file)) {
                List<String> lines = new ArrayList<>();
                lines.add("# The server's rules, shown in /guides > General Guide > Rules. One rule per line; the guide numbers them.");
                lines.add("# Lines starting with # are notes and aren't shown. Players see changes the next time they log in.");
                lines.addAll(DEFAULTS);
                Files.createDirectories(file.getParent());
                Files.write(file, lines, StandardCharsets.UTF_8);
            }
            List<String> rules = new ArrayList<>();
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String rule = line.strip();
                if (!rule.isEmpty() && !rule.startsWith("#")) rules.add(rule);
            }
            return rules;
        } catch (IOException e) {
            LogUtils.getLogger().warn("[Rules] couldn't read {}: {}", file, e.toString());
            return DEFAULTS;
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) PacketDistributor.sendToPlayer(player, new RulesPacket(load(), com.alexh.capitalism.deathtax.Insurance.bloodMoonPrice()));
    }
}