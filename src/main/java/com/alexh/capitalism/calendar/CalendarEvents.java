package com.alexh.capitalism.calendar;

import com.alexh.capitalism.Capitalism;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

// What the calendar does on the server (Calendar.md):
//  - Each new morning, everyone is told the date ("Good morning! Tuesday, Spring 2, Year 1.") in chat and on a small card
//    bottom right (a new season names itself there), a festival day says what it brings, the day before gives a heads-up.
//  - Festivals (calendar/Festival): sales (economy/NpcPricing reads them), crops growing twice as fast (a crop's
//    growth check always passes), Luck all day, +50% XP, fireworks after dark (8 pm to midnight, round each player in
//    the overworld), no monsters spawning after dark, monsters in pumpkin masks, and a gift for everyone once that day.
//  - /calendar: the date, the time, today's festival and the next one; /calendar festivals: the year's festivals;
//    /calendar set <season> <day> [year] (operators): moves the clock to that date, keeping the time of day.
@EventBusSubscriber(modid = Capitalism.MODID)
public final class CalendarEvents {

    // the day last announced (-1: not yet, this run)
    private static long lastDay = -1;
    private static final String GIFT_KEY = "capitalism_festival_gift";
    // in winter, the chance each minute of clear weather that it starts snowing
    static final float WINTER_SNOW_CHANCE = 0.15F;

    private CalendarEvents() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        lastDay = -1;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel overworld = server.overworld();
        if (server.getTickCount() % 20 != 0) return;
        long clock = Calendar.clock(overworld);
        Calendar.Date date = Calendar.date(clock);
        if (lastDay < 0) {
            lastDay = date.index();
        } else if (date.index() != lastDay) {
            boolean newSeason = Calendar.date(lastDay * Calendar.DAY_TICKS).season() != date.season() || date.index() < lastDay;
            lastDay = date.index();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) morning(player, date, newSeason);
        }
        // winter: snow comes often (a chance a minute to start snowing for 3 to 8 minutes, when the weather is left to run)
        if (date.season() == Calendar.Season.WINTER && server.getTickCount() % 1200 == 0 && !overworld.isRaining()
            && overworld.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.ADVANCE_WEATHER) && overworld.getRandom().nextFloat() < WINTER_SNOW_CHANCE) {
            server.setWeatherParameters(0, 3600 + overworld.getRandom().nextInt(6000), true, false);
        }
        Festival festival = Festival.on(date);
        if (festival == null) return;
        long timeOfDay = Math.floorMod(clock, Calendar.DAY_TICKS);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            giveGift(player, festival, date);
            if (festival.luck > 0) {
                player.addEffect(new MobEffectInstance(MobEffects.LUCK, 60, festival.luck - 1, true, false, true));
            }
        }
        // a celebration over the town: fireworks all over it, 8 pm to midnight (while someone's there to see them)
        if (festival.fireworks && timeOfDay >= 14000 && timeOfDay < 18000) {
            int rockets = 3 + overworld.getRandom().nextInt(4);
            for (int i = 0; i < rockets; i++) launchOverTown(overworld);
        }
    }

    // the morning card (a new season, a festival's news, tomorrow's heads-up): only the card, nothing in chat
    static void morning(ServerPlayer player, Calendar.Date date, boolean newSeason) {
        Festival today = Festival.on(date);
        Festival tomorrow = Festival.on(date.plusDays(1));
        // the little card, bottom right (client/DayNoticeHud): a new season gets its name as the heading
        String head = newSeason ? date.season().title + ", Year " + date.year() : "Good morning!";
        int headColor = newSeason ? date.season().color & 0xFFFFFF : 0xE0A030;
        Festival shown = today != null ? today : tomorrow;
        String festival = shown == null ? "" : (today != null ? "Today: " : "Tomorrow: ") + shown.title;
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new com.alexh.capitalism.network.DayNoticePacket(
            head, headColor, date.longText(), festival, shown == null ? 0 : shown.color & 0xFFFFFF));
    }

    private static MutableComponent festivalLine(String lead, Festival f) {
        return Component.literal("✦ " + lead).withStyle(ChatFormatting.GRAY)
            .append(Component.literal(f.title).withStyle(s -> s.withColor(TextColor.fromRgb(f.color & 0xFFFFFF)).withBold(true)))
            .append(Component.literal(": " + f.description).withStyle(ChatFormatting.GRAY));
    }

    // the festival's gift, once each player that day
    static void giveGift(ServerPlayer player, Festival festival, Calendar.Date date) {
        List<ItemStack> gift = festival.gift();
        if (gift.isEmpty()) return;
        var data = player.getPersistentData();
        if (data.getLongOr(GIFT_KEY, -1L) == date.index()) return;
        data.putLong(GIFT_KEY, date.index());
        for (ItemStack stack : gift) {
            if (!player.getInventory().add(stack.copy())) player.drop(stack.copy(), false, net.minecraft.util.Prediction.SERVER_ONLY);
        }
        player.sendSystemMessage(Component.literal("🎁 A " + festival.title + " gift for you!").withStyle(s -> s.withColor(TextColor.fromRgb(festival.color & 0xFFFFFF))));
    }

    // a firework from somewhere in the town (com.alexh.capitalism.town.Town: the protected town center), if that spot is
    // loaded; true if it went up
    static boolean launchOverTown(ServerLevel level) {
        var area = com.alexh.capitalism.town.Town.protectedArea();
        var random = level.getRandom();
        int x = area.minX() + random.nextInt(area.maxX() - area.minX() + 1);
        int z = area.minZ() + random.nextInt(area.maxZ() - area.minZ() + 1);
        if (!level.isLoaded(new net.minecraft.core.BlockPos(x, 64, z))) return false;
        launchFirework(level, x + 0.5, z + 0.5);
        return true;
    }

    // a firework from the ground (the highest block) at x, z
    static void launchFirework(ServerLevel level, double x, double z) {
        var random = level.getRandom();
        int[] palette = {0xFF3030, 0xFFD030, 0x30C0FF, 0x60FF60, 0xFF60D0, 0xFFFFFF};
        FireworkExplosion.Shape[] shapes = {FireworkExplosion.Shape.LARGE_BALL, FireworkExplosion.Shape.STAR, FireworkExplosion.Shape.BURST, FireworkExplosion.Shape.SMALL_BALL};
        it.unimi.dsi.fastutil.ints.IntList colors = it.unimi.dsi.fastutil.ints.IntList.of(palette[random.nextInt(palette.length)], palette[random.nextInt(palette.length)]);
        FireworkExplosion explosion = new FireworkExplosion(shapes[random.nextInt(shapes.length)], colors,
            it.unimi.dsi.fastutil.ints.IntList.of(0xFFFFFF), random.nextBoolean(), random.nextBoolean());
        ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
        rocket.set(DataComponents.FIREWORKS, new Fireworks(1 + random.nextInt(2), List.of(explosion)));
        double y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z) + 1;
        level.addFreshEntity(new FireworkRocketEntity(level, x, y, z, rocket));
    }

    // ── festival effects ──

    @SubscribeEvent
    public static void onCropGrow(CropGrowEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Festival f = Festival.today(level);
        if (f != null && f.cropBoost && event.getResult() == CropGrowEvent.Pre.Result.DEFAULT && level.getRandom().nextBoolean()) {
            event.setResult(CropGrowEvent.Pre.Result.GROW);
        }
    }

    @SubscribeEvent
    public static void onXp(PlayerXpEvent.XpChange event) {
        if (event.getAmount() <= 0) return;
        Festival f = Festival.today(event.getEntity().level());
        if (f != null && f.xp > 1.0) event.setAmount((int) Math.round(event.getAmount() * f.xp));
    }

    @SubscribeEvent
    public static void onSpawn(FinalizeSpawnEvent event) {
        if (!(event.getEntity() instanceof Monster monster) || !(event.getLevel() instanceof ServerLevel level)) return;
        Festival f = Festival.today(level);
        if (f == null) return;
        boolean natural = event.getSpawnType() == EntitySpawnReason.NATURAL;
        long timeOfDay = Math.floorMod(Calendar.clock(level), Calendar.DAY_TICKS);
        if (f.peacefulNight && natural && timeOfDay >= 12000) {
            event.setSpawnCancelled(true);
            return;
        }
        if (f.pumpkins && monster.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            monster.setItemSlot(EquipmentSlot.HEAD, new ItemStack(level.getRandom().nextInt(3) == 0 ? Items.JACK_O_LANTERN : Items.CARVED_PUMPKIN));
            monster.setDropChance(EquipmentSlot.HEAD, 0.0F);
        }
    }

    // ── /calendar ──

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("calendar")
            .executes(c -> show(c.getSource()))
            .then(Commands.literal("festivals").executes(c -> festivals(c.getSource())))
            // testing: jump to a festival's morning, the next morning, or tonight's 8 pm
            .then(Commands.literal("goto").requires(Commands.hasPermission(new net.minecraft.server.permissions.PermissionCheck.Require(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)))
                .then(Commands.argument("festival", StringArgumentType.word())
                    .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(java.util.Arrays.stream(Festival.values()).map(f -> f.name().toLowerCase()), b))
                    .executes(c -> gotoFestival(c.getSource(), StringArgumentType.getString(c, "festival")))))
            .then(Commands.literal("next").requires(Commands.hasPermission(new net.minecraft.server.permissions.PermissionCheck.Require(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)))
                .executes(c -> jump(c.getSource(), Calendar.date(source(c)).index() + 1, 1000)))
            .then(Commands.literal("night").requires(Commands.hasPermission(new net.minecraft.server.permissions.PermissionCheck.Require(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)))
                .executes(c -> jump(c.getSource(), Calendar.date(source(c)).index(), 14000)))
            .then(Commands.literal("set").requires(Commands.hasPermission(new net.minecraft.server.permissions.PermissionCheck.Require(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)))
                .then(Commands.argument("season", StringArgumentType.word())
                    .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(List.of("spring", "summer", "fall", "winter"), b))
                    .then(Commands.argument("day", IntegerArgumentType.integer(1, Calendar.DAYS_PER_SEASON))
                        .executes(c -> set(c.getSource(), StringArgumentType.getString(c, "season"), IntegerArgumentType.getInteger(c, "day"), -1))
                        .then(Commands.argument("year", IntegerArgumentType.integer(1, 9999))
                            .executes(c -> set(c.getSource(), StringArgumentType.getString(c, "season"), IntegerArgumentType.getInteger(c, "day"),
                                IntegerArgumentType.getInteger(c, "year"))))))));
    }

    private static int show(CommandSourceStack source) {
        long clock = Calendar.clock(source.getServer().overworld());
        Calendar.Date date = Calendar.date(clock);
        source.sendSuccess(() -> Component.literal(date.longText() + ", " + Calendar.timeText(clock) + ".")
            .withStyle(s -> s.withColor(TextColor.fromRgb(date.season().color & 0xFFFFFF))), false);
        Festival.Upcoming next = Festival.next(date);
        if (next != null) {
            String when = next.inDays() == 0 ? "Today is " : next.inDays() == 1 ? "Tomorrow is " : "In " + next.inDays() + " days: ";
            source.sendSuccess(() -> festivalLine(when, next.festival()), false);
        }
        return 1;
    }

    private static ServerLevel source(com.mojang.brigadier.context.CommandContext<CommandSourceStack> c) {
        return c.getSource().getServer().overworld();
    }

    // to a day (its index), at a time of day
    private static int jump(CommandSourceStack source, long day, long timeOfDay) {
        ServerLevel overworld = source.getServer().overworld();
        Calendar.setClock(overworld, day * Calendar.DAY_TICKS + timeOfDay);
        long clock = Calendar.clock(overworld);
        source.sendSuccess(() -> Component.literal("It's now " + Calendar.date(clock).longText() + ", " + Calendar.timeText(clock) + ".").withStyle(ChatFormatting.GOLD), true);
        return 1;
    }

    // to a festival's morning (7 am) this year
    private static int gotoFestival(CommandSourceStack source, String name) {
        Festival festival = null;
        for (Festival f : Festival.values()) if (f.name().equalsIgnoreCase(name)) festival = f;
        if (festival == null) {
            source.sendFailure(Component.literal("No festival called " + name + "."));
            return 0;
        }
        int year = Calendar.date(source.getServer().overworld()).year();
        return jump(source, Calendar.startOf(year, festival.season, festival.day) / Calendar.DAY_TICKS, 1000);
    }

    private static int festivals(CommandSourceStack source) {
        for (Festival f : Festival.values()) {
            source.sendSuccess(() -> Component.literal(f.season.title + " " + f.day + ": ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(f.title).withStyle(s -> s.withColor(TextColor.fromRgb(f.color & 0xFFFFFF))))
                .append(Component.literal(" - " + f.description).withStyle(ChatFormatting.DARK_GRAY)), false);
        }
        return Festival.values().length;
    }

    private static int set(CommandSourceStack source, String seasonName, int day, int year) {
        Calendar.Season season = Calendar.Season.byName(seasonName);
        if (season == null) {
            source.sendFailure(Component.literal("No season called " + seasonName + " (spring, summer, fall, winter)."));
            return 0;
        }
        ServerLevel overworld = source.getServer().overworld();
        int y = year > 0 ? year : Calendar.date(overworld).year();
        Calendar.setDate(overworld, y, season, day);
        Calendar.Date now = Calendar.date(overworld);
        source.sendSuccess(() -> Component.literal("The date is now " + now.longText() + ".").withStyle(ChatFormatting.GOLD), true);
        return 1;
    }
}
