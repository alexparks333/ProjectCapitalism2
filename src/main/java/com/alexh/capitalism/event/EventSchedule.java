package com.alexh.capitalism.event;

import com.alexh.capitalism.Capitalism;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

// When the natural events come (Events.md > When): The Purge once every PURGE_MIN-MAX days (10-12), a Blood Moon once
// every BLOOD_MIN-MAX nights (6-8), each day picked at random. The natural ones NEVER land on the same night: a day that
// would clash (with the other's natural day, or with an event already on that night) moves on a day. The Purge is
// announced the morning before, as always (Purge.schedule); a Blood Moon rises at dusk on its day (BloodMoon.scheduleTonight).
// The Mayor's switches (EventSwitchBlock) and the Blood Moon Altar are outside this: the Mayor CAN put both on one night.
// The next days are saved with the world.
@EventBusSubscriber(modid = Capitalism.MODID)
public final class EventSchedule extends SavedData {

    public static final int PURGE_MIN = 10, PURGE_MAX = 12;
    public static final int BLOOD_MIN = 6, BLOOD_MAX = 8;

    private static final Codec<EventSchedule> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.LONG.optionalFieldOf("purge_night", -1L).forGetter(s -> s.purgeNight),
        Codec.LONG.optionalFieldOf("blood_night", -1L).forGetter(s -> s.bloodNight)
    ).apply(i, EventSchedule::new));
    public static final SavedDataType<EventSchedule> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(Capitalism.MODID, "event_schedule"), EventSchedule::new, CODEC);

    // the calendar day of the next natural Purge night / Blood Moon night (-1: not picked yet)
    long purgeNight = -1;
    long bloodNight = -1;

    public EventSchedule() {
    }

    private EventSchedule(long purgeNight, long bloodNight) {
        this.purgeNight = purgeNight;
        this.bloodNight = bloodNight;
    }

    public static EventSchedule get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    // (tests) set the next natural nights
    public void set(long purgeNight, long bloodNight) {
        this.purgeNight = purgeNight;
        this.bloodNight = bloodNight;
    }

    public long purgeNight() {
        return this.purgeNight;
    }

    public long bloodNight() {
        return this.bloodNight;
    }

    public static int gap(RandomSource random, int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    // The next natural Purge night after `from`, never on the natural Blood Moon night
    public long pickPurge(long from, RandomSource random) {
        long night = from + gap(random, PURGE_MIN, PURGE_MAX);
        while (night == this.bloodNight) night++;
        return night;
    }

    public long pickBlood(long from, RandomSource random) {
        long night = from + gap(random, BLOOD_MIN, BLOOD_MAX);
        while (night == this.purgeNight) night++;
        return night;
    }


    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        // (not on the headless game-test server: tests set the calendar themselves and natural events would get in the way)
        if (server.getTickCount() % 100 != 0 || server instanceof net.minecraft.gametest.framework.GameTestServer) return;
        long today = Purge.today(server);
        EventSchedule s = get(server);
        RandomSource random = server.overworld().getRandom();
        boolean changed = false;
        if (s.purgeNight < 0) {
            s.purgeNight = s.pickPurge(today, random);
            changed = true;
        }
        if (s.bloodNight < 0) {
            s.bloodNight = s.pickBlood(today, random);
            changed = true;
        }
        // nights that slipped by while the server was off: pick again from today
        if (s.purgeNight < today) {
            s.purgeNight = s.pickPurge(today, random);
            changed = true;
        }
        if (s.bloodNight < today) {
            s.bloodNight = s.pickBlood(today, random);
            changed = true;
        }
        // the morning before a natural Purge: announce it (unless a Blood Moon already has that night: then a day later)
        if (today == s.purgeNight - 1) {
            boolean bloodThatNight = s.bloodNight == s.purgeNight;
            if (bloodThatNight) {
                s.purgeNight++;
            } else {
                Purge.schedule(server);   // false if one is already on or announced: then this one's skipped
                s.purgeNight = s.pickPurge(s.purgeNight, random);
            }
            changed = true;
        }
        // a natural Blood Moon's day: it rises at dusk (a day later if The Purge has tonight)
        if (today == s.bloodNight) {
            if (Purge.claimsTonight(server)) {
                s.bloodNight++;
            } else {
                BloodMoon.scheduleTonight(server);   // false if one is already on or coming: then this one's skipped
                s.bloodNight = s.pickBlood(s.bloodNight, random);
            }
            changed = true;
        }
        if (changed) s.setDirty();
    }
}
