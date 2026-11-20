package com.alexh.capitalism.core;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.occupation.JobData;
import com.alexh.capitalism.occupation.JobEvents;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

// /admin ... : server admin tools (ops only). More get added here over time.
//  - /admin panel: the live admin panel (core/AdminPanel, client/AdminPanelScreen).
//  - /admin jobs reset <player>: back to the very start: no jobs, one Essential and one Fun slot (extra slots from
//    Wishing Stars are gone; if they have any it asks for "/admin jobs reset <player> confirm" first). The job screen
//    opens and they pick again.
//  - /admin jobs clear <player>: their jobs are cleared so they pick again in /pick, but their job slots are kept.
//  Both let them pick straight away, whatever job draft is running.
//  - /admin stock refill <player>: they can buy the Blood Merchant's Essential Wishing Star again (one per player);
//    /admin stock drain <player>: they can't (counted as having had it).
@EventBusSubscriber(modid = Capitalism.MODID)
public final class AdminCommands {

    private static final PermissionCheck OPS = new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER);

    private AdminCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("admin").requires(Commands.hasPermission(OPS))
            // the live admin panel (core/AdminPanel): settings, the Banker's prices, every admin command
            .then(Commands.literal("panel").executes(context -> {
                AdminPanel.open(context.getSource().getPlayerOrException(), true);
                return 1;
            }))
            .then(Commands.literal("jobs")
                .then(Commands.literal("reset")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> resetJobs(context, false))
                        .then(Commands.literal("confirm").executes(context -> resetJobs(context, true)))))
                .then(Commands.literal("clear")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(AdminCommands::clearJobs))))
            // the Blood Merchant's Essential Wishing Star, one per player (town/BloodMerchantShop.StarBuyers):
            // refill = they can buy it again; drain = they can't (as if they'd had it)
            .then(Commands.literal("stock")
                .then(Commands.literal("refill").then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> starStock(context, true))))
                .then(Commands.literal("drain").then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> starStock(context, false)))))
            // the job drafts (occupation/JobDraft, Jobs.md > The job draft): /admin draft essential|fun ...
            .then(draftCommands("essential", com.alexh.capitalism.occupation.Job.Category.ESSENTIAL))
            .then(draftCommands("fun", com.alexh.capitalism.occupation.Job.Category.FUN)));
    }

    // add <player> / remove <name> / start / skip / stop / status, for one kind of job
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> draftCommands(String word,
            com.alexh.capitalism.occupation.Job.Category category) {
        return Commands.literal("draft").then(Commands.literal(word)
            .then(Commands.literal("add")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> result(context, com.alexh.capitalism.occupation.JobDraft.add(
                        context.getSource().getServer(), category, EntityArgument.getPlayer(context, "player"))))))
            .then(Commands.literal("remove")
                .then(Commands.argument("name", com.mojang.brigadier.arguments.StringArgumentType.word())
                    .suggests((context, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                        com.alexh.capitalism.occupation.JobDraft.orderNames(category), builder))
                    .executes(context -> result(context, com.alexh.capitalism.occupation.JobDraft.remove(
                        context.getSource().getServer(), category, com.mojang.brigadier.arguments.StringArgumentType.getString(context, "name"))))))
            .then(Commands.literal("start")
                .executes(context -> result(context, com.alexh.capitalism.occupation.JobDraft.start(context.getSource().getServer(), category))))
            .then(Commands.literal("skip")
                .executes(context -> result(context, com.alexh.capitalism.occupation.JobDraft.skip(context.getSource().getServer(), category))))
            .then(Commands.literal("stop").executes(context -> {
                if (!com.alexh.capitalism.occupation.JobDraft.active(category)) {
                    return result(context, "No " + category.title + " job draft is running.");
                }
                com.alexh.capitalism.occupation.JobDraft.stop(context.getSource().getServer(), category);
                return 1;
            }))
            .then(Commands.literal("status").executes(context -> {
                java.util.List<String> names = com.alexh.capitalism.occupation.JobDraft.orderNames(category);
                if (names.isEmpty()) return result(context, "Nobody is on the " + category.title + " draft.");
                boolean started = com.alexh.capitalism.occupation.JobDraft.started(category);
                StringBuilder line = new StringBuilder(category.title + " draft (" + (started ? "running" : "not started") + "): ");
                for (int i = 0; i < names.size(); i++) {
                    if (i > 0) line.append(", ");
                    line.append(started && i == com.alexh.capitalism.occupation.JobDraft.position(category) ? "[" + names.get(i) + "]" : names.get(i));
                }
                context.getSource().sendSuccess(() -> Component.literal(line.toString()).withStyle(ChatFormatting.AQUA), false);
                return 1;
            })));
    }

    // null = it worked; else the error
    private static int result(CommandContext<CommandSourceStack> context, String error) {
        if (error == null) return 1;
        context.getSource().sendFailure(Component.literal(error));
        return 0;
    }

    private static int starStock(CommandContext<CommandSourceStack> context, boolean refill) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        String name = target.getGameProfile().name();
        var buyers = com.alexh.capitalism.town.BloodMerchantShop.StarBuyers.get(context.getSource().getServer());
        boolean had = buyers.has(target.getUUID());
        if (refill) buyers.remove(target.getUUID());
        else buyers.add(target.getUUID());
        String msg = refill
            ? (had ? name + " can buy the Blood Merchant's Essential Wishing Star again." : name + " could already buy the Essential Wishing Star.")
            : (had ? name + " already couldn't buy the Essential Wishing Star." : name + " can no longer buy the Blood Merchant's Essential Wishing Star.");
        context.getSource().sendSuccess(() -> Component.literal(msg).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int clearJobs(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        clearJobs(target);
        context.getSource().sendSuccess(() -> Component.literal(target.getGameProfile().name()
            + "'s jobs were cleared: they pick again in /pick (their job slots are kept).").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    // /admin jobs reset: back to the very start. With extra slots (Wishing Stars) to lose, it asks for "confirm" first.
    private static int resetJobs(CommandContext<CommandSourceStack> context, boolean confirmed) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        String name = target.getGameProfile().name();
        JobData data = target.getData(ModAttachments.JOBS.get());
        int extra = 0;
        for (com.alexh.capitalism.occupation.Job.Category category : com.alexh.capitalism.occupation.Job.Category.values()) {
            extra += data.slots(category) - 1;
        }
        if (extra > 0 && !confirmed) {
            int lost = extra;
            context.getSource().sendSuccess(() -> Component.literal(name + " has " + lost + " extra job slot" + (lost == 1 ? "" : "s")
                + " from Wishing Stars. A full reset takes " + (lost == 1 ? "it" : "them") + " away for good (the stars aren't given back). Run /admin jobs reset "
                + name + " confirm to go ahead, or /admin jobs clear " + name + " to keep the slots.").withStyle(ChatFormatting.YELLOW), false);
            return 0;
        }
        resetJobs(target);
        context.getSource().sendSuccess(() -> Component.literal(name
            + "'s jobs were reset to the start: one Essential and one Fun job, picked again in /pick.").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    // /admin jobs clear: no jobs, not mid-change, the same number of slots; the job screen opens for them
    public static void clearJobs(ServerPlayer target) {
        wipeJobs(target, true);
    }

    // /admin jobs reset: as if they'd just joined: no jobs, one Essential and one Fun slot (Wishing Star slots gone)
    public static void resetJobs(ServerPlayer target) {
        wipeJobs(target, false);
    }

    private static void wipeJobs(ServerPlayer target, boolean keepSlots) {
        JobData old = target.getData(ModAttachments.JOBS.get());
        JobData fresh = new JobData();
        if (keepSlots) {
            for (com.alexh.capitalism.occupation.Job.Category category : com.alexh.capitalism.occupation.Job.Category.values()) {
                fresh.setSlots(category, old.slots(category));
            }
        }
        // they pick both again straight away, whatever job draft is going on (until every slot is full)
        fresh.setFreePick(true);
        target.setData(ModAttachments.JOBS.get(), fresh);
        JobEvents.sync(target);
        com.alexh.capitalism.occupation.JobDraft.sync(target);
        target.sendSystemMessage(Component.literal(keepSlots ? "An admin cleared your jobs: pick your jobs again."
            : "An admin reset your jobs to the start: pick one Essential and one Fun job again.").withStyle(ChatFormatting.GOLD));
        PacketDistributor.sendToPlayer(target, new com.alexh.capitalism.network.OpenJobPickPacket());
    }
}
