package com.alexh.capitalism.bounty;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// A player's bounties (Bounties.md; ModAttachments.BOUNTIES: saved, kept through death, synced to that player): their one
// open task (its amount, its reward and how far along it is, counted only from when it was taken), the day they last
// took one (one a day), how many they've turned in, the gold that earned them. Plus a copy of the server's board for
// their screen (BountyBoard, refreshed by Bounties.syncBoard): the task posted now, the milestone winners in order, their
// own place (or -1) and whether they've claimed its star. Easy to read on the client: Bounties.of(player).
public class BountyData {

    // a task as taken (or offered): which, how many, what it pays (nuggets), how far along
    public record Open(String id, int amount, long reward, int progress) {
        static final Codec<Open> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(Open::id),
            Codec.INT.optionalFieldOf("amount", 1).forGetter(Open::amount),
            Codec.LONG.optionalFieldOf("reward", 0L).forGetter(Open::reward),
            Codec.INT.optionalFieldOf("progress", 0).forGetter(Open::progress)
        ).apply(i, Open::new));
        static final StreamCodec<ByteBuf, Open> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, Open::id, ByteBufCodecs.VAR_INT, Open::amount, ByteBufCodecs.VAR_LONG, Open::reward,
            ByteBufCodecs.VAR_INT, Open::progress, Open::new);

        public BountyTask task() {
            return BountyTask.byId(this.id);
        }

        public String title() {
            BountyTask task = this.task();
            return task == null ? this.id : task.title(this.amount);
        }

        Open withProgress(int p) {
            return new Open(this.id, this.amount, this.reward, p);
        }
    }

    // a line of a standings board, as shown (the board's copy): name, score, the extra number (bounties: gold earned,
    // nuggets), a milestone winner (★), is them
    public record Row(String name, long score, long extra, boolean star, boolean me) {
        static final StreamCodec<ByteBuf, Row> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, Row::name, ByteBufCodecs.VAR_LONG, Row::score, ByteBufCodecs.VAR_LONG, Row::extra,
            ByteBufCodecs.BOOL, Row::star, ByteBufCodecs.BOOL, Row::me, Row::new);
    }

    // one standings board, as shown: its category (Standings.Category id), its top 10, their rank (0: not on it) and score
    public record Board(String category, List<Row> top, int rank, long score) {
        static final StreamCodec<ByteBuf, Board> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, Board::category, Row.STREAM_CODEC.apply(ByteBufCodecs.list()), Board::top,
            ByteBufCodecs.VAR_INT, Board::rank, ByteBufCodecs.VAR_LONG, Board::score, Board::new);
    }

    public static final MapCodec<BountyData> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Open.CODEC.listOf().optionalFieldOf("open", List.of()).forGetter(d -> d.open),
        Open.CODEC.optionalFieldOf("offer").forGetter(d -> Optional.ofNullable(d.offer)),
        Codec.LONG.optionalFieldOf("offer_day", -1L).forGetter(d -> d.offerDay),
        Codec.STRING.listOf().optionalFieldOf("history", List.of()).forGetter(d -> d.history),
        Codec.LONG.optionalFieldOf("taken_day", -1L).forGetter(d -> d.takenDay),
        Codec.INT.optionalFieldOf("completed", 0).forGetter(d -> d.completed),
        Codec.LONG.optionalFieldOf("earned", 0L).forGetter(d -> d.earned),
        Codec.BOOL.optionalFieldOf("claimed", false).forGetter(d -> d.claimed)
    ).apply(i, (open, offer, offerDay, history, takenDay, completed, earned, claimed) ->
        new BountyData(open, offer.orElse(null), offerDay, history, takenDay, completed, earned, claimed)));
    // (the board's copy isn't saved here: it's refreshed from BountyBoard)

    public static final StreamCodec<ByteBuf, BountyData> STREAM_CODEC = StreamCodec.of(
        (buf, d) -> {
            Open.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, d.open);
            ByteBufCodecs.BOOL.encode(buf, d.offer != null);
            if (d.offer != null) Open.STREAM_CODEC.encode(buf, d.offer);
            ByteBufCodecs.VAR_LONG.encode(buf, d.offerDay);
            ByteBufCodecs.VAR_LONG.encode(buf, d.takenDay);
            ByteBufCodecs.VAR_INT.encode(buf, d.completed);
            ByteBufCodecs.VAR_LONG.encode(buf, d.earned);
            ByteBufCodecs.BOOL.encode(buf, d.claimed);
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).encode(buf, d.winners);
            ByteBufCodecs.VAR_INT.encode(buf, d.place + 1);
            Board.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, d.boards);
        },
        buf -> {
            List<Open> open = Open.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf);
            Open offer = ByteBufCodecs.BOOL.decode(buf) ? Open.STREAM_CODEC.decode(buf) : null;
            long offerDay = ByteBufCodecs.VAR_LONG.decode(buf);
            long takenDay = ByteBufCodecs.VAR_LONG.decode(buf);
            int completed = ByteBufCodecs.VAR_INT.decode(buf);
            long earned = ByteBufCodecs.VAR_LONG.decode(buf);
            boolean claimed = ByteBufCodecs.BOOL.decode(buf);
            BountyData d = new BountyData(open, offer, offerDay, List.of(), takenDay, completed, earned, claimed);
            d.winners.addAll(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).decode(buf));
            d.place = ByteBufCodecs.VAR_INT.decode(buf) - 1;
            d.boards.addAll(Board.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf));
            return d;
        });

    final List<Open> open = new ArrayList<>();
    Open offer;
    long offerDay = -1;
    final List<String> history = new ArrayList<>();
    long takenDay = -1;
    int completed;
    long earned;
    // claimed their milestone star
    boolean claimed;
    // (the board's copy) the milestone winners' names, in order; their own place (0..2) or -1
    final List<String> winners = new ArrayList<>();
    int place = -1;
    // (the board's copy) every standings board: its top 10 and their own rank
    final List<Board> boards = new ArrayList<>();

    public BountyData() {
    }

    private BountyData(List<Open> open, Open offer, long offerDay, List<String> history, long takenDay, int completed, long earned, boolean claimed) {
        this.open.addAll(open);
        this.offer = offer;
        this.offerDay = offerDay;
        this.history.addAll(history);
        this.takenDay = takenDay;
        this.completed = completed;
        this.earned = earned;
        this.claimed = claimed;
    }

    public List<Open> open() {
        return List.copyOf(this.open);
    }

    // the task posted on the board now (the board's copy)
    public Open offer() {
        return this.offer;
    }

    // their one open task, or null
    public Open task() {
        return this.open.isEmpty() ? null : this.open.get(0);
    }

    public List<String> winners() {
        return List.copyOf(this.winners);
    }

    public int place() {
        return this.place;
    }

    public List<Board> boards() {
        return List.copyOf(this.boards);
    }

    // one board (by Standings.Category id), or null
    public Board board(String category) {
        for (Board b : this.boards) if (b.category().equals(category)) return b;
        return null;
    }

    public long offerDay() {
        return this.offerDay;
    }

    public List<String> history() {
        return List.copyOf(this.history);
    }

    public long takenDay() {
        return this.takenDay;
    }

    public int completed() {
        return this.completed;
    }

    // gold nuggets earned from bounties, all told
    public long earned() {
        return this.earned;
    }

    public boolean claimed() {
        return this.claimed;
    }
}
