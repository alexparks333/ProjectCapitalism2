package com.alexh.capitalism.bounty;

import com.alexh.capitalism.Capitalism;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// The server's standings (the Bounty Clerk's Ledger shows them, Bounties.md > Standings), one board per Category, saved
// with the world: category -> player -> their score, an extra number some boards show (bounties: the gold earned), their
// name, and when they reached that score (on a tie, whoever got there first ranks higher). Offline players stay on.
// To add a board: add a Category and call set() when its score changes (and on login, from the player's own data).
public class Standings extends SavedData {

    public enum Category {
        BOUNTIES("bounties", "Bounties", "done", "gold"),
        // cravings satisfied (craving/Cravings): kept by the town
        CRAVINGS("cravings", "Cravings satisfied", "satisfied", ""),
        // players killed in The Purge, all told (event/Purge, deathtax/DeathTax)
        PURGE_KILLS("purge_kills", "Purge kills", "kills", ""),
        // whole gold paid to them from Purge victims' death tax, all told (deathtax/DeathTax)
        GOLD_TAKEN("gold_taken", "Gold taken", "gold", "");

        public final String id;
        public final String title;
        // what the score and the extra number are called in the column headings (extra: "" for none)
        public final String unit;
        public final String extra;

        Category(String id, String title, String unit, String extra) {
            this.id = id;
            this.title = title;
            this.unit = unit;
            this.extra = extra;
        }
    }

    public record Entry(UUID id, String name, long score, long extra, long reachedAt) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(Entry::id),
            Codec.STRING.fieldOf("name").forGetter(Entry::name),
            Codec.LONG.optionalFieldOf("score", 0L).forGetter(Entry::score),
            Codec.LONG.optionalFieldOf("extra", 0L).forGetter(Entry::extra),
            Codec.LONG.optionalFieldOf("reached_at", 0L).forGetter(Entry::reachedAt)
        ).apply(i, Entry::new));
    }

    static final Codec<Standings> CODEC = Codec.unboundedMap(Codec.STRING, Entry.CODEC.listOf()).xmap(Standings::new, Standings::lists);
    static final SavedDataType<Standings> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath(Capitalism.MODID, "standings"), Standings::new, CODEC);

    private final Map<String, Map<UUID, Entry>> boards = new HashMap<>();

    public Standings() {
    }

    private Standings(Map<String, List<Entry>> lists) {
        lists.forEach((cat, entries) -> {
            Map<UUID, Entry> m = new HashMap<>();
            entries.forEach(e -> m.put(e.id(), e));
            this.boards.put(cat, m);
        });
    }

    private Map<String, List<Entry>> lists() {
        Map<String, List<Entry>> out = new HashMap<>();
        this.boards.forEach((cat, m) -> out.put(cat, new ArrayList<>(m.values())));
        return out;
    }

    public static Standings get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    // a player's score on a board; `now` is when (for ties), only moving when the score does
    public void set(Category category, UUID id, String name, long score, long extra, long now) {
        Map<UUID, Entry> board = this.boards.computeIfAbsent(category.id, c -> new HashMap<>());
        Entry old = board.get(id);
        if (old != null && old.score() == score && old.extra() == extra && old.name().equals(name)) return;
        long reached = old != null && old.score() == score ? old.reachedAt() : now;
        board.put(id, new Entry(id, name, score, extra, reached));
        this.setDirty();
    }

    // everyone with a score, best first: highest score, then whoever reached it first, then by name
    public List<Entry> ranking(Category category) {
        List<Entry> out = new ArrayList<>(this.boards.getOrDefault(category.id, Map.of()).values());
        out.removeIf(e -> e.score() <= 0);
        out.sort(Comparator.comparingLong(Entry::score).reversed().thenComparingLong(Entry::reachedAt).thenComparing(Entry::name));
        return out;
    }

    // (tests) take a player off a board
    void remove(Category category, UUID id) {
        Map<UUID, Entry> board = this.boards.get(category.id);
        if (board != null && board.remove(id) != null) this.setDirty();
    }
}
