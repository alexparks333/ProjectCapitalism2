package com.alexh.capitalism.player;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.block.LiquorType;
import com.alexh.capitalism.core.ModAttachments;
import com.alexh.capitalism.core.ModEffects;
import com.alexh.capitalism.core.ModItems;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

// Intoxication (Wellness.md > Intoxication): small and fun. Every alcoholic drink adds to a count for the day (in
// tenths of a "drink": a spirit shot is 1). It starts over every morning (the same day as Cravings).
//  - 3 drinks in a day: Tipsy for 2 minutes (the view sways a little).
//  - 5 drinks in a day: Drunk for 4 minutes (sways more, and you don't walk quite straight).
// Each drink past the line starts the timer again. The swaying is client/IntoxicationClient.
@EventBusSubscriber(modid = Capitalism.MODID)
public final class Intoxication {

    public static final int TIPSY_AT = 30;     // tenths
    public static final int DRUNK_AT = 50;
    public static final int TIPSY_TICKS = 2 * 60 * 20;
    public static final int DRUNK_TICKS = 4 * 60 * 20;

    // How strong each drink is, in tenths
    private static final Map<Supplier<? extends Item>, Integer> STRENGTH = new HashMap<>();

    static {
        for (LiquorType liquor : LiquorType.values()) {
            boolean wine = liquor == LiquorType.RED_WINE || liquor == LiquorType.WHITE_WINE;
            STRENGTH.put(ModItems.SHOTS.get(liquor), wine ? 5 : 10);
        }
        STRENGTH.put(ModItems.GLASS_OF_RED_WINE, 15);
        STRENGTH.put(ModItems.GLASS_OF_WHITE_WINE, 15);
        // cocktails: about a shot each
        for (Supplier<? extends Item> cocktail : java.util.List.<Supplier<? extends Item>>of(ModItems.BEET_NIGHTCAP, ModItems.GLOWBERRY_GOLD_RUSH,
                ModItems.NETHER_FIREBALL_SHOT, ModItems.ENDER_TELE_TINI, ModItems.PIRATES_HIGH_SEAS_GROG, ModItems.MINERS_MOONSHINE,
                ModItems.VAMPIRIC_MULLED_WINE, ModItems.OVERWORLD_MOJITO, ModItems.PHANTOMS_NIGHTCAP, ModItems.ZOMBIES_HANGOVER_CURE,
                ModItems.CLASSIC_OLD_FASHIONED, ModItems.WHISKEY_SOUR, ModItems.WATERMELON_BREEZE, ModItems.SANGRIA_PITCHER,
                ModItems.WHITE_WINE_SPRITZER, ModItems.TROPICAL_KELP_COOLER, ModItems.BEETROOT_VELVET_SOUR,
                ModItems.MARGARITA, ModItems.BERRY_DAIQUIRI, ModItems.COSMOPOLITAN, ModItems.TEQUILA_SUNRISE, ModItems.MOSCOW_MULE,
                ModItems.RUM_PUNCH, ModItems.ROSE_SPRITZ, ModItems.PUMPKIN_SPICE_FLIP,
                ModItems.FORGEFIRE_OLD_FASHIONED, ModItems.ANGLERS_SUNRISE, ModItems.BUTCHERS_BLOODY_MARY, ModItems.HOT_TODDY,
                ModItems.MIDNIGHT_OIL_MARTINI, ModItems.HAGGLERS_HIGHBALL, ModItems.DESERT_ROSE, ModItems.MOONSTALKER_SANGRIA)) {
            STRENGTH.put(cocktail, 10);
        }
        // the strong ones
        STRENGTH.put(ModItems.ENDER_TWILIGHT_FIZZ, 15);       // two shots in it
        STRENGTH.put(ModItems.DRUNKEN_BRAWLERS_STOUT, 25);    // three shots in it
    }

    private Intoxication() {
    }

    // tenths of a drink this adds (0 = not alcoholic)
    public static int strength(ItemStack stack) {
        for (Map.Entry<Supplier<? extends Item>, Integer> e : STRENGTH.entrySet()) {
            if (stack.is(e.getKey().get())) return e.getValue();
        }
        return 0;
    }

    // "2.5" (tenths shown as drinks)
    public static String drinks(int tenths) {
        return tenths % 10 == 0 ? String.valueOf(tenths / 10) : (tenths / 10) + "." + (tenths % 10);
    }

    @SubscribeEvent
    public static void onDrink(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        int strength = strength(event.getItem());
        if (strength > 0) drink(player, strength);
    }

    // Adds a drink to today's count and makes them Tipsy / Drunk past the lines.
    public static void drink(ServerPlayer player, int strength) {
        long today = com.alexh.capitalism.craving.Cravings.today(player.level());
        Data data = player.getData(ModAttachments.INTOXICATION.get());
        int before = data.day == today ? data.tenths : 0;
        int after = before + strength;
        player.setData(ModAttachments.INTOXICATION.get(), new Data(today, after));
        if (after >= DRUNK_AT) {
            player.removeEffect(ModEffects.TIPSY);
            player.addEffect(new MobEffectInstance(ModEffects.DRUNK, DRUNK_TICKS, 0, false, false, true));
            if (before < DRUNK_AT) player.sendOverlayMessage(Component.literal("You're drunk! The world won't hold still..."));
        } else if (after >= TIPSY_AT) {
            player.addEffect(new MobEffectInstance(ModEffects.TIPSY, TIPSY_TICKS, 0, false, false, true));
            if (before < TIPSY_AT) player.sendOverlayMessage(Component.literal("You feel a little tipsy."));
        }
    }

    // today's count (0 if the last drink was another day)
    public static int today(net.minecraft.world.entity.player.Player player) {
        Data data = player.getData(ModAttachments.INTOXICATION.get());
        return data.day == com.alexh.capitalism.craving.Cravings.today(player.level()) ? data.tenths : 0;
    }

    // A player's drinks today: the day (Cravings.today) and how many, in tenths. Saved, kept through death, synced to them.
    public record Data(long day, int tenths) {
        public static final MapCodec<Data> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.LONG.optionalFieldOf("day", -1L).forGetter(Data::day),
            Codec.INT.optionalFieldOf("tenths", 0).forGetter(Data::tenths)
        ).apply(i, Data::new));
        public static final StreamCodec<ByteBuf, Data> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, Data::day,
            ByteBufCodecs.VAR_INT, Data::tenths,
            Data::new);

        public Data() {
            this(-1L, 0);
        }
    }
}
