package com.alexh.capitalism.craving;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.block.KitchenRecipes;
import com.alexh.capitalism.bounty.Standings;
import com.alexh.capitalism.calendar.Calendar;
import com.alexh.capitalism.core.ModAttachments;
import com.alexh.capitalism.core.ModComponents;
import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.farmer.MeatCut;
import com.alexh.capitalism.farmer.Sear;
import com.alexh.capitalism.network.ThirstSyncPacket;
import com.alexh.capitalism.player.ThirstData;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// Cravings (Cravings.md): every player gets their own pair each morning (the Calendar day), one FOOD and one DRINK.
//  - Food: a Cook's meal (sushi, Cooking Table and Oven dishes) or a Farmer's seared cut at an exact doneness ("Medium
//    Well Seared Beef Rib"), rarely a plain food anyone can make; drink: a Bartender's cocktail, shot or glass of
//    wine, rarely plain milk or honey. Mostly job-made on purpose (you buy them from Cooks, Farmers, Bartenders);
//    the plain ones mean it's never impossible.
//  - Eating today's food (by hand or off a plate) fills the food bar AND every golden drumstick; drinking today's drink
//    fills the water bar and every golden drop. Its normal effects still apply. Once per craving per day: 1 point each
//    (the "Cravings satisfied" standings, kept by the town). A second one the same day is just food.
//  - Told in the morning and at login; shown on the Wellness screen's Tasks page.
//  - Admins: /cravings <player> [reroll]. Nothing resets except the daily roll.
@EventBusSubscriber(modid = Capitalism.MODID)
public final class Cravings {

    private static final PermissionCheck OPS = new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER);

    // one possible craving: an item, a doneness for a seared cut (-1: any), how likely, and whether anyone can make it
    public record Craving(Identifier item, int doneness, int weight, boolean plain) {
        public ItemStack icon() {
            return new ItemStack(BuiltInRegistries.ITEM.getValue(this.item));
        }
    }

    private static List<Craving> foods, drinks;

    private Cravings() {
    }

    private static Identifier id(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }

    // the food pool: every Cook meal (not the uncooked dishes that go in the Oven), every seared cut at each safe
    // doneness, a few plain foods
    public static List<Craving> foods() {
        if (foods != null) return foods;
        List<Craving> out = new ArrayList<>();
        Set<Item> meals = new LinkedHashSet<>();
        Set<Item> ovenInputs = new LinkedHashSet<>();
        for (KitchenRecipes.Baking b : KitchenRecipes.OVEN) {
            ovenInputs.add(b.input().get());
            meals.add(b.result().get());
        }
        for (KitchenRecipes.Assembly a : KitchenRecipes.SUSHI) meals.add(a.result().get());
        for (KitchenRecipes.Assembly a : KitchenRecipes.COOKING) if (!ovenInputs.contains(a.result().get())) meals.add(a.result().get());
        for (Item meal : meals) out.add(new Craving(id(meal), -1, 3, false));
        for (MeatCut cut : MeatCut.values()) {
            Item seared = ModItems.SEARED_CUTS.get(cut).get();
            for (Sear.Doneness d : Sear.Doneness.values()) {
                if (d == Sear.Doneness.RAW) continue;
                if (cut.animal == MeatCut.Animal.CHICKEN && d != Sear.Doneness.WELL_DONE) continue;   // only safe chicken
                out.add(new Craving(id(seared), d.ordinal(), 1, false));
            }
        }
        for (Item plain : new Item[]{Items.BREAD, Items.BAKED_POTATO, Items.PUMPKIN_PIE, Items.COOKED_SALMON}) out.add(new Craving(id(plain), -1, 1, true));  // plain: rare on purpose, so Cooks get the business
        foods = List.copyOf(out);
        return foods;
    }

    // the drink pool: every Bartender cocktail, every liquor shot, the glasses of wine, plain milk and honey
    public static List<Craving> drinks() {
        if (drinks != null) return drinks;
        List<Craving> out = new ArrayList<>();
        for (var c : ModItems.COCKTAILS) out.add(new Craving(id(c.get()), -1, 3, false));
        out.add(new Craving(id(ModItems.BEET_NIGHTCAP.get()), -1, 3, false));
        for (var s : ModItems.SHOTS.values()) out.add(new Craving(id(s.get()), -1, 2, false));
        out.add(new Craving(id(ModItems.GLASS_OF_RED_WINE.get()), -1, 3, false));
        out.add(new Craving(id(ModItems.GLASS_OF_WHITE_WINE.get()), -1, 3, false));
        for (Item plain : new Item[]{Items.MILK_BUCKET, Items.HONEY_BOTTLE}) out.add(new Craving(id(plain), -1, 1, true));  // plain: rare on purpose, so Bartenders get the business
        drinks = List.copyOf(out);
        return drinks;
    }

    private static Craving pick(List<Craving> pool, RandomSource random) {
        int total = pool.stream().mapToInt(Craving::weight).sum();
        int roll = random.nextInt(total);
        for (Craving c : pool) {
            roll -= c.weight();
            if (roll < 0) return c;
        }
        return pool.get(0);
    }

    public static CravingData of(Player player) {
        return player.getData(ModAttachments.CRAVINGS.get());
    }

    static void save(ServerPlayer player, CravingData data) {
        player.setData(ModAttachments.CRAVINGS.get(), data);   // saves it and sends it to them
    }

    public static long today(Level level) {
        return Calendar.date(level).index();
    }

    // today's pair for this player: the same all day (from their id, the day and the reroll salt)
    static void roll(ServerPlayer player, CravingData data, long day) {
        long seed = player.getUUID().getMostSignificantBits() * 31 ^ player.getUUID().getLeastSignificantBits() ^ (day * 0x9E3779B97F4A7C15L) ^ (data.salt * 0x632BE59BD9B4E019L);
        RandomSource random = RandomSource.create(seed);
        Craving food = pick(foods(), random), drink = pick(drinks(), random);
        data.day = day;
        data.food = food.item().toString();
        data.foodDone = food.doneness();
        data.drink = drink.item().toString();
        data.ate = false;
        data.drank = false;
    }

    // make sure today's are rolled; true if they're new
    public static boolean ensure(ServerPlayer player) {
        CravingData data = of(player);
        long day = today(player.level());
        if (data.day == day && !data.food.isEmpty()) return false;
        roll(player, data, day);
        save(player, data);
        return true;
    }

    // "Medium Well Seared Beef Rib", "Classic Old Fashioned"
    public static MutableComponent name(String item, int doneness) {
        Item it = BuiltInRegistries.ITEM.getValue(Identifier.parse(item));
        MutableComponent name = it.getName(new ItemStack(it)).copy();
        if (doneness >= 0 && doneness < Sear.Doneness.values().length) name = Component.literal(Sear.Doneness.values()[doneness].title + " ").append(name);
        return name;
    }

    public static boolean matchesFood(CravingData data, ItemStack stack) {
        if (data.food.isEmpty() || !BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(data.food)) return false;
        if (data.foodDone < 0) return true;
        Sear sear = stack.get(ModComponents.SEAR.get());
        return sear != null && sear.done().ordinal() == data.foodDone;
    }

    public static boolean matchesDrink(CravingData data, ItemStack stack) {
        return !data.drink.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(data.drink);
    }

    // (eating or drinking finished, off a plate too) the craving satisfied, if it's today's and not yet
    public static void consumed(ServerPlayer player, ItemStack stack) {
        ensure(player);
        CravingData data = of(player);
        boolean food = !data.ate && matchesFood(data, stack);
        boolean drink = !food && !data.drank && matchesDrink(data, stack);
        if (!food && !drink) return;
        ThirstData thirst = player.getData(ModAttachments.THIRST.get());
        if (food) {
            data.ate = true;
            thirst.fillFoodCompletely(player.getFoodData());
        } else {
            data.drank = true;
            thirst.fillCompletely();
        }
        data.total++;
        save(player, data);
        PacketDistributor.sendToPlayer(player, ThirstSyncPacket.of(thirst, player));
        var server = player.level().getServer();
        Standings.get(server).set(Standings.Category.CRAVINGS, player.getUUID(), player.getGameProfile().name(), data.total, 0, server.overworld().getGameTime());
        var level = player.level();
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.4F);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.2F);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.2, player.getZ(), 16, 0.4, 0.5, 0.4, 0.05);
        level.sendParticles(food ? ParticleTypes.WAX_ON : ParticleTypes.GLOW, player.getX(), player.getY() + 1.4, player.getZ(), 14, 0.4, 0.5, 0.4, 0.1);
        player.sendSystemMessage(Component.literal("✔ Craving satisfied! ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
            .append(Component.literal(food ? "Your food bar is full to gold." : "Your water bar is full to gold.").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(" (" + data.total + " in all)").withStyle(ChatFormatting.GRAY)));
    }

    // after the normal eating (gold, diet...), so the craving's full bars are what's left
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player) consumed(player, event.getItem());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ensure(player);
        var server = player.level().getServer();
        Standings.get(server).set(Standings.Category.CRAVINGS, player.getUUID(), player.getGameProfile().name(), of(player).total, 0, server.overworld().getGameTime());
    }

    // a new morning: everyone on gets their new pair (quietly)
    private static long lastDay = Long.MIN_VALUE;

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 0) return;
        long day = today(server.overworld());
        if (day == lastDay) return;
        lastDay = day;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            ensure(p);   // (no chat message: the cravings show on the Wellness screen)
        }
    }

    // ── /cravings <player> [reroll] (ops) ──

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("cravings").requires(Commands.hasPermission(OPS))
            .then(Commands.argument("player", EntityArgument.player()).executes(Cravings::show)
                .then(Commands.literal("reroll").executes(Cravings::reroll))));
    }

    private static int show(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        ensure(target);
        CravingData d = of(target);
        Component text = Component.literal(target.getGameProfile().name() + "'s cravings today: ").withStyle(ChatFormatting.GOLD)
            .append(name(d.food, d.foodDone)).append(d.ate ? " ✔" : "").append(", ").append(name(d.drink, -1)).append(d.drank ? " ✔" : "")
            .append(Component.literal(" (" + d.total + " satisfied in all)").withStyle(ChatFormatting.GRAY));
        context.getSource().sendSuccess(() -> text, false);
        return d.total;
    }

    private static int reroll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        CravingData d = of(target);
        d.salt++;
        roll(target, d, today(target.level()));
        save(target, d);
        context.getSource().sendSuccess(() -> Component.literal("Rerolled " + target.getGameProfile().name() + "'s cravings.").withStyle(ChatFormatting.YELLOW), true);
        return 1;
    }

    // (game tests) set today's pair
    public static void set(ServerPlayer player, String food, int doneness, String drink) {
        CravingData d = of(player);
        d.day = today(player.level());
        d.food = food;
        d.foodDone = doneness;
        d.drink = drink;
        d.ate = false;
        d.drank = false;
        save(player, d);
    }
}
