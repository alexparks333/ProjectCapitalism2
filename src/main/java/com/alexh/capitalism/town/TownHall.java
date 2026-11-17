package com.alexh.capitalism.town;

import com.alexh.capitalism.Capitalism;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// The Town Hall's offices (Town.md > The Town Hall): a Mayor, a Vice Mayor and a Treasurer. The town votes for its
// Mayor and Vice Mayor and they appoint a Treasurer, all roleplayed; the server's admin makes it official with
// /townhall appoint. An official's title shows before their name in chat and the player list. What they do with it
// (hearing people out, changing the server's laws) is roleplay. Saved with the world.
//  - /townhall: who holds each office (anyone)
//  - /townhall appoint <mayor|vice_mayor|treasurer> <player>, /townhall remove <office> (operators)
//  - /townhall meeting [what about]: an official (or an operator) calls everyone to a Town Hall meeting
//  - /townannounce <message>: the Mayor (or an operator) announces something, bottom middle of every screen
@EventBusSubscriber(modid = Capitalism.MODID)
public class TownHall extends SavedData {

    public enum Office {
        MAYOR("Mayor", 0xF0C040),
        VICE_MAYOR("Vice Mayor", 0x7CC8F0),
        TREASURER("Treasurer", 0x7CE07C);

        public final String title;
        public final int color;

        Office(String title, int color) {
            this.title = title;
            this.color = color;
        }

        public String id() {
            return this.name().toLowerCase();
        }

        public static Office byId(String id) {
            for (Office o : values()) if (o.id().equalsIgnoreCase(id)) return o;
            return null;
        }
    }

    // who holds an office: their id, and their name when appointed (shown while they're offline)
    public record Holder(UUID id, String name) {
        static final Codec<Holder> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(Holder::id),
            Codec.STRING.fieldOf("name").forGetter(Holder::name)).apply(i, Holder::new));
    }

    private static final Codec<TownHall> CODEC = Codec.unboundedMap(Codec.STRING, Holder.CODEC).xmap(map -> {
        TownHall hall = new TownHall();
        map.forEach((id, holder) -> {
            Office office = Office.byId(id);
            if (office != null) hall.holders.put(office, holder);
        });
        return hall;
    }, hall -> {
        Map<String, Holder> map = new HashMap<>();
        hall.holders.forEach((office, holder) -> map.put(office.id(), holder));
        return map;
    });

    public static final SavedDataType<TownHall> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(Capitalism.MODID, "town_hall"), TownHall::new, CODEC);

    private final Map<Office, Holder> holders = new EnumMap<>(Office.class);

    public TownHall() {
    }

    public static TownHall get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public Holder holder(Office office) {
        return this.holders.get(office);
    }

    // the office this player holds, or null
    public Office officeOf(UUID player) {
        for (Map.Entry<Office, Holder> e : this.holders.entrySet()) if (e.getValue().id().equals(player)) return e.getKey();
        return null;
    }

    // gives the office to this player (one office each: anything else they held is left empty); returns who had it
    public Holder appoint(Office office, ServerPlayer player) {
        this.holders.values().removeIf(h -> h.id().equals(player.getUUID()));
        Holder before = this.holders.put(office, new Holder(player.getUUID(), player.getGameProfile().name()));
        this.setDirty();
        return before;
    }

    public Holder remove(Office office) {
        Holder before = this.holders.remove(office);
        if (before != null) this.setDirty();
        return before;
    }

    // ââ titles before officials' names ââ

    @SubscribeEvent
    public static void onNameFormat(PlayerEvent.NameFormat event) {
        Office office = officeOf(event.getEntity());
        if (office != null) event.setDisplayname(title(office).append(event.getDisplayname()));
    }

    @SubscribeEvent
    public static void onTabListName(PlayerEvent.TabListNameFormat event) {
        Office office = officeOf(event.getEntity());
        if (office == null) return;
        Component name = event.getDisplayName() != null ? event.getDisplayName() : event.getEntity().getName();
        event.setDisplayName(title(office).append(name));
    }

    private static Office officeOf(Player player) {
        if (!(player instanceof ServerPlayer sp)) return null;
        return get(sp.level().getServer()).officeOf(player.getUUID());
    }

    // "[Mayor] " in the office's colour, bold, then whatever follows plain (it's a sibling, so it doesn't inherit the bold)
    public static MutableComponent title(Office office) {
        return Component.empty().append(Component.literal("[" + office.title + "] ")
            .withStyle(s -> s.withColor(TextColor.fromRgb(office.color)).withBold(true)));
    }

    private static void refreshName(MinecraftServer server, UUID id) {
        if (id == null) return;
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        if (player == null) return;
        player.refreshDisplayName();
        player.refreshTabListName();
    }

    // ââ commands ââ

    private static final PermissionCheck OPS = new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER);

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        var office = Commands.argument("office", StringArgumentType.word())
            .suggests((c, b) -> {
                for (Office o : Office.values()) b.suggest(o.id());
                return b.buildFuture();
            });
        event.getDispatcher().register(Commands.literal("townhall")
            .executes(c -> list(c.getSource()))
            .then(Commands.literal("appoint").requires(Commands.hasPermission(OPS))
                .then(office.then(Commands.argument("player", EntityArgument.player())
                    .executes(c -> appoint(c.getSource(), StringArgumentType.getString(c, "office"), EntityArgument.getPlayer(c, "player"))))))
            .then(Commands.literal("remove").requires(Commands.hasPermission(OPS))
                .then(Commands.argument("office", StringArgumentType.word())
                    .suggests((c, b) -> {
                        for (Office o : Office.values()) b.suggest(o.id());
                        return b.buildFuture();
                    })
                    .executes(c -> remove(c.getSource(), StringArgumentType.getString(c, "office")))))
            .then(Commands.literal("meeting")
                .executes(c -> meeting(c.getSource(), ""))
                .then(Commands.argument("about", StringArgumentType.greedyString())
                    .executes(c -> meeting(c.getSource(), StringArgumentType.getString(c, "about"))))));
        // (dev) /tp town: back to the middle of town. Added under "teleport" because vanilla's "tp" only redirects there.
        event.getDispatcher().register(Commands.literal("teleport")
            .then(Commands.literal("town").requires(Commands.hasPermission(OPS)).executes(c -> {
                ServerPlayer player = c.getSource().getPlayerOrException();
                var middle = Town.protectedArea().center(0);
                if (!Town.teleportOnTop(player, middle.getX(), middle.getZ())) {
                    c.getSource().sendFailure(Component.literal("Only from the overworld."));
                    return 0;
                }
                c.getSource().sendSuccess(() -> Component.literal("Back to town."), false);
                return 1;
            })));
        // /townannounce <message>: the Mayor (or an operator) tells the whole town, bottom middle of every screen
        event.getDispatcher().register(Commands.literal("townannounce")
            .then(Commands.argument("message", StringArgumentType.greedyString())
                .executes(c -> announce(c.getSource(), StringArgumentType.getString(c, "message")))));
    }

    private static MutableComponent tag() {
        return Component.literal("[Town Hall] ").withStyle(ChatFormatting.GOLD);
    }

    private static int list(CommandSourceStack source) {
        TownHall hall = get(source.getServer());
        source.sendSuccess(() -> tag().append(Component.literal("The town's officials:").withStyle(ChatFormatting.YELLOW)), false);
        for (Office o : Office.values()) {
            Holder h = hall.holder(o);
            boolean online = h != null && source.getServer().getPlayerList().getPlayer(h.id()) != null;
            source.sendSuccess(() -> Component.literal("  ").append(title(o))
                .append(h == null ? Component.literal("(vacant)").withStyle(ChatFormatting.GRAY)
                    : Component.literal(h.name()).withStyle(ChatFormatting.WHITE)
                        .append(Component.literal(online ? "" : " (offline)").withStyle(ChatFormatting.DARK_GRAY))), false);
        }
        return 1;
    }

    private static int appoint(CommandSourceStack source, String officeId, ServerPlayer player) {
        Office office = Office.byId(officeId);
        if (office == null) {
            source.sendFailure(Component.literal("No such office. Use mayor, vice_mayor or treasurer."));
            return 0;
        }
        MinecraftServer server = source.getServer();
        TownHall hall = get(server);
        Office had = hall.officeOf(player.getUUID());
        Holder before = hall.appoint(office, player);
        refreshName(server, player.getUUID());
        if (before != null) refreshName(server, before.id());
        // the whole town hears it
        MutableComponent news = tag().append(Component.literal(player.getGameProfile().name()).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
            .append(Component.literal(" is now the town's ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(office.title).withStyle(s -> s.withColor(TextColor.fromRgb(office.color)).withBold(true)))
            .append(Component.literal("!").withStyle(ChatFormatting.YELLOW));
        if (before != null && !before.id().equals(player.getUUID())) {
            news.append(Component.literal(" (taking over from " + before.name() + ")").withStyle(ChatFormatting.GRAY));
        }
        if (had != null && had != office) news.append(Component.literal(" They leave the office of " + had.title + ".").withStyle(ChatFormatting.GRAY));
        server.getPlayerList().broadcastSystemMessage(news, false);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.2F);
        }
        return 1;
    }

    private static int remove(CommandSourceStack source, String officeId) {
        Office office = Office.byId(officeId);
        if (office == null) {
            source.sendFailure(Component.literal("No such office. Use mayor, vice_mayor or treasurer."));
            return 0;
        }
        MinecraftServer server = source.getServer();
        Holder before = get(server).remove(office);
        if (before == null) {
            source.sendFailure(Component.literal("Nobody is the " + office.title + "."));
            return 0;
        }
        refreshName(server, before.id());
        server.getPlayerList().broadcastSystemMessage(tag().append(Component.literal(before.name() + " is no longer the town's " + office.title + ".")
            .withStyle(ChatFormatting.YELLOW)), false);
        return 1;
    }

    // the Mayor's announcement: shown bottom middle of everyone's screen (just above the hotbar), and kept in chat so
    // nobody misses it. Quotes round the message are dropped.
    private static int announce(CommandSourceStack source, String message) {
        MinecraftServer server = source.getServer();
        ServerPlayer caller = source.getPlayer();
        boolean mayor = caller != null && get(server).officeOf(caller.getUUID()) == Office.MAYOR;
        if (!mayor && !source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            source.sendFailure(Component.literal("Only the Mayor can make a town announcement."));
            return 0;
        }
        String text = message.trim();
        if (text.length() >= 2 && text.startsWith("\"") && text.endsWith("\"")) text = text.substring(1, text.length() - 1).trim();
        if (text.isEmpty()) return 0;
        MutableComponent shown = Component.literal("\u2726 ").append(Component.literal(text).withStyle(s -> s.withColor(TextColor.fromRgb(Office.MAYOR.color)).withBold(true)));
        MutableComponent from = mayor ? title(Office.MAYOR).append(Component.literal(caller.getGameProfile().name()).withStyle(ChatFormatting.WHITE))
            : Component.literal("The Town Hall").withStyle(ChatFormatting.WHITE);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.sendOverlayMessage(shown);
            p.sendSystemMessage(tag().append(from).append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(text).withStyle(ChatFormatting.YELLOW)));
            p.level().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.8F, 1.0F);
        }
        return 1;
    }

    // an official (or an operator) calls a Town Hall meeting: everyone hears it, with a bell
    private static int meeting(CommandSourceStack source, String about) {
        MinecraftServer server = source.getServer();
        ServerPlayer caller = source.getPlayer();
        Office office = caller == null ? null : get(server).officeOf(caller.getUUID());
        boolean op = source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        if (office == null && !op) {
            source.sendFailure(Component.literal("Only the Mayor, Vice Mayor or Treasurer can call a Town Hall meeting."));
            return 0;
        }
        MutableComponent who = office != null ? title(office).append(Component.literal(caller.getGameProfile().name()).withStyle(ChatFormatting.WHITE))
            : Component.literal(caller != null ? caller.getGameProfile().name() : "The server").withStyle(ChatFormatting.WHITE);
        MutableComponent call = tag().append(who).append(Component.literal(" calls a Town Hall meeting!").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        server.getPlayerList().broadcastSystemMessage(call, false);
        if (!about.isBlank()) {
            server.getPlayerList().broadcastSystemMessage(Component.literal("  \"" + about + "\"").withStyle(ChatFormatting.ITALIC, ChatFormatting.WHITE), false);
        }
        server.getPlayerList().broadcastSystemMessage(Component.literal("  Come to the Town Hall.").withStyle(ChatFormatting.GRAY), false);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.level().playSound(null, p.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return 1;
    }
}
