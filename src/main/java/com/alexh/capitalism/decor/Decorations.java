package com.alexh.capitalism.decor;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.calendar.Calendar;
import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

// Whether the seasonal decorations are up (Decorations.md): by the calendar (AUTO: each holiday's decorations are up
// on its days, DecorKind.Holiday), or all up / all packed away by hand (an operator's /decorations up|down, until
// /decorations auto). Saved with the world. The client is told the mode with each block's state (they only look at
// the block's UP), so it needn't know it.
@EventBusSubscriber(modid = Capitalism.MODID)
public class Decorations extends SavedData {

    public enum Mode { AUTO, UP, DOWN }

    private static final Codec<Decorations> CODEC = Codec.STRING.xmap(
        name -> {
            Decorations d = new Decorations();
            try {
                d.mode = Mode.valueOf(name);
            } catch (IllegalArgumentException e) {
                d.mode = Mode.AUTO;
            }
            return d;
        }, d -> d.mode.name());

    public static final SavedDataType<Decorations> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(Capitalism.MODID, "decorations"), Decorations::new, CODEC);

    private Mode mode = Mode.AUTO;

    public Decorations() {
    }

    public static Decorations get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public Mode mode() {
        return this.mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        this.setDirty();
    }

    // should decorations of this kind be up now? (server side)
    public static boolean shouldBeUp(Level level, DecorKind kind) {
        if (level.getServer() != null) {
            Mode mode = get(level.getServer()).mode;
            if (mode == Mode.UP) return true;
            if (mode == Mode.DOWN) return false;
        }
        return kind.holiday.on(Calendar.date(level));
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("decorations")
            .executes(c -> status(c.getSource()))
            .then(Commands.literal("auto").requires(Commands.hasPermission(OPS)).executes(c -> setMode(c.getSource(), Mode.AUTO)))
            .then(Commands.literal("up").requires(Commands.hasPermission(OPS)).executes(c -> setMode(c.getSource(), Mode.UP)))
            .then(Commands.literal("down").requires(Commands.hasPermission(OPS)).executes(c -> setMode(c.getSource(), Mode.DOWN))));
    }

    // Nobody breaks a decoration (they're unbreakable, like bedrock), except an operator in Creative (to move one)
    @SubscribeEvent
    public static void onBreak(net.neoforged.neoforge.event.level.block.BreakBlockEvent event) {
        if (!(event.getState().getBlock() instanceof SeasonalDecorBlock)) return;
        net.minecraft.world.entity.player.Player player = event.getPlayer();
        if (canRemove(player)) return;
        event.setCanceled(true);
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            sp.sendOverlayMessage(Component.literal("Decorations can't be broken.").withStyle(ChatFormatting.GRAY));
        }
    }

    // may this player take a decoration away? (an operator in Creative)
    public static boolean canRemove(net.minecraft.world.entity.player.Player player) {
        return player != null && player.isCreative()
            && player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
    }

    private static final net.minecraft.server.permissions.PermissionCheck OPS =
        new net.minecraft.server.permissions.PermissionCheck.Require(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);

    private static int status(CommandSourceStack source) {
        Mode mode = get(source.getServer()).mode;
        Calendar.Date date = Calendar.date(source.getServer().overworld());
        source.sendSuccess(() -> Component.literal(switch (mode) {
            case AUTO -> "Decorations follow the calendar (" + date.season().title + " " + date.day() + "):";
            case UP -> "Every decoration is up (/decorations auto to follow the calendar again).";
            case DOWN -> "Every decoration is packed away (/decorations auto to follow the calendar again).";
        }).withStyle(ChatFormatting.GOLD), false);
        if (mode == Mode.AUTO) {
            for (DecorKind.Holiday h : DecorKind.Holiday.values()) {
                boolean up = h.on(date);
                source.sendSuccess(() -> Component.literal("  " + h.title + " (" + h.window() + "): " + (up ? "up" : "packed away"))
                    .withStyle(up ? ChatFormatting.GREEN : ChatFormatting.GRAY), false);
            }
        }
        return 1;
    }

    private static int setMode(CommandSourceStack source, Mode mode) {
        get(source.getServer()).setMode(mode);
        source.sendSuccess(() -> Component.literal(switch (mode) {
            case AUTO -> "Decorations follow the calendar again: they'll go up and come down by themselves.";
            case UP -> "Putting every decoration up.";
            case DOWN -> "Packing every decoration away.";
        }).withStyle(ChatFormatting.GOLD), true);
        return 1;
    }
}
