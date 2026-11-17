package com.alexh.capitalism.deathtax;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// A player's death tax and insurance (Insurance.md; ModAttachments.DEATH_TAX: saved, kept through death, synced to that
// player). Their debts, oldest first: each owed to the bank (no creditor) or, from a Purge kill, to the player who killed
// them. Their last death's tax (for the popup and /deathtax). Their insurance plan (a tier id, "" = none), the change
// waiting for the next morning (a tier id, "cancel", or "" = none) and from which day, the last day processed (the plan
// switched and the premium charged), the last day they were covered (paid that morning), and the Blood Moon Insurance
// they hold (the number of the Blood Moon it covers, -1 = none; Insurance.moonsStarted counts them).
public class DeathTaxData {

    public static final class Debt {
        static final Codec<Debt> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.STRING_CODEC.optionalFieldOf("creditor").forGetter(d -> Optional.ofNullable(d.creditor)),
            Codec.STRING.optionalFieldOf("name", "Death").forGetter(d -> d.name),
            Codec.LONG.optionalFieldOf("nuggets", 0L).forGetter(d -> d.nuggets)
        ).apply(i, (c, n, g) -> new Debt(c.orElse(null), n, g)));

        // who's paid when it's paid: null = the bank (the gold leaves the economy), else the Purge killer
        final @Nullable UUID creditor;
        final String name;
        long nuggets;

        Debt(@Nullable UUID creditor, String name, long nuggets) {
            this.creditor = creditor;
            this.name = name;
            this.nuggets = nuggets;
        }

        public @Nullable UUID creditor() {
            return this.creditor;
        }

        public String name() {
            return this.name;
        }

        public long nuggets() {
            return this.nuggets;
        }
    }

    public static final MapCodec<DeathTaxData> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Debt.CODEC.listOf().optionalFieldOf("debts", List.of()).forGetter(d -> d.debts),
        Codec.INT.optionalFieldOf("last_rate", -1).forGetter(d -> d.lastRate),
        Codec.LONG.optionalFieldOf("last_gold", 0L).forGetter(d -> d.lastGold),
        Codec.LONG.optionalFieldOf("last_tax", 0L).forGetter(d -> d.lastTax),
        Codec.STRING.optionalFieldOf("last_why", "").forGetter(d -> d.lastWhy),
        Codec.STRING.optionalFieldOf("tier", "").forGetter(d -> d.tier),
        Codec.STRING.optionalFieldOf("pending", "").forGetter(d -> d.pending),
        Codec.LONG.optionalFieldOf("pending_day", -1L).forGetter(d -> d.pendingDay),
        Codec.LONG.optionalFieldOf("processed_day", Long.MIN_VALUE).forGetter(d -> d.processedDay),
        Codec.LONG.optionalFieldOf("covered_day", Long.MIN_VALUE).forGetter(d -> d.coveredDay),
        Codec.LONG.optionalFieldOf("blood_moon_cover", -1L).forGetter(d -> d.bloodMoonCover)
    ).apply(i, DeathTaxData::new));

    public static final StreamCodec<ByteBuf, DeathTaxData> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC.codec());

    final List<Debt> debts = new ArrayList<>();
    int lastRate = -1;
    long lastGold, lastTax;
    String lastWhy = "";
    String tier = "";
    String pending = "";
    long pendingDay = -1;
    long processedDay = Long.MIN_VALUE;
    long coveredDay = Long.MIN_VALUE;
    long bloodMoonCover = -1;

    public DeathTaxData() {
    }

    private DeathTaxData(List<Debt> debts, int lastRate, long lastGold, long lastTax, String lastWhy, String tier, String pending,
                         long pendingDay, long processedDay, long coveredDay, long bloodMoonCover) {
        for (Debt d : debts) this.debts.add(new Debt(d.creditor, d.name, d.nuggets));
        this.lastRate = lastRate;
        this.lastGold = lastGold;
        this.lastTax = lastTax;
        this.lastWhy = lastWhy;
        this.tier = tier;
        this.pending = pending;
        this.pendingDay = pendingDay;
        this.processedDay = processedDay;
        this.coveredDay = coveredDay;
        this.bloodMoonCover = bloodMoonCover;
    }

    // everything owed, in nuggets
    public long owed() {
        long total = 0;
        for (Debt d : this.debts) total += d.nuggets;
        return total;
    }

    public List<Debt> debts() {
        return List.copyOf(this.debts);
    }

    public int lastRate() {
        return this.lastRate;
    }

    public long lastGold() {
        return this.lastGold;
    }

    public long lastTax() {
        return this.lastTax;
    }

    public String lastWhy() {
        return this.lastWhy;
    }

    public String tier() {
        return this.tier;
    }

    public String pending() {
        return this.pending;
    }

    public long pendingDay() {
        return this.pendingDay;
    }

    public long coveredDay() {
        return this.coveredDay;
    }

    public long bloodMoonCover() {
        return this.bloodMoonCover;
    }

    // adds to what's owed this creditor (null = the bank), or a new debt at the end
    void add(@Nullable UUID creditor, String name, long nuggets) {
        if (nuggets <= 0) return;
        for (Debt d : this.debts) {
            if (java.util.Objects.equals(d.creditor, creditor)) {
                d.nuggets += nuggets;
                return;
            }
        }
        this.debts.add(new Debt(creditor, name, nuggets));
    }
}
