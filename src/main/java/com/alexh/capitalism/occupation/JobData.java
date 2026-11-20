package com.alexh.capitalism.occupation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

// The jobs a player holds (Jobs.md > Picking jobs; saved by id, kept through death). Everyone starts with no jobs and
// one slot of each category (Job.Category): /pick fills them, and what's picked is for good. A Wishing Star wished on
// the Wishing Well (occupation/WishingWellBlock) adds a slot of its kind, up to MAX_SLOTS each. A Changing Star (handed
// out by an admin) lets them drop jobs in /pick and pick again, until their slots are full once more.
public class JobData {

    // slots a player can reach in each category (1 to start, +1 from a Wishing Star)
    public static final int MAX_SLOTS = 2;

    public static final MapCodec<JobData> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
        Codec.STRING.listOf().optionalFieldOf("jobs", List.of()).forGetter(JobData::ids),
        Codec.INT.optionalFieldOf("essential_slots", 1).forGetter(d -> d.essentialSlots),
        Codec.INT.optionalFieldOf("fun_slots", 1).forGetter(d -> d.funSlots),
        Codec.BOOL.optionalFieldOf("changing", false).forGetter(d -> d.changing),
        Codec.INT.optionalFieldOf("released", -1).forGetter(d -> d.released),
        Codec.BOOL.optionalFieldOf("free_pick", false).forGetter(d -> d.freePick)
    ).apply(inst, JobData::new));

    private final Set<Job> jobs = EnumSet.noneOf(Job.class);
    private int essentialSlots = 1;
    private int funSlots = 1;
    // a Changing Star was used: jobs can be dropped and picked again until every slot is full
    private boolean changing;
    // while changing: the kind of job let go of (Job.Category ordinal), -1 before one is; only ONE job changes per star
    private int released = -1;
    // an admin reset their jobs (/admin jobs reset): they pick again freely, no job draft in the way, until every slot is full
    private boolean freePick;

    public JobData() {
    }

    private JobData(List<String> ids, int essentialSlots, int funSlots, boolean changing, int released, boolean freePick) {
        this.freePick = freePick;
        this.released = released;
        this.essentialSlots = Math.max(1, Math.min(MAX_SLOTS, essentialSlots));
        this.funSlots = Math.max(1, Math.min(MAX_SLOTS, funSlots));
        this.changing = changing;
        for (String id : ids) {
            Job job = Job.byId(id);
            if (job != null && this.count(job.category) < this.slots(job.category)) this.jobs.add(job);
        }
    }

    private List<String> ids() {
        List<String> ids = new ArrayList<>();
        for (Job job : this.jobs) ids.add(job.id());
        return ids;
    }

    public boolean has(Job job) {
        return this.jobs.contains(job);
    }

    public Set<Job> jobs() {
        return EnumSet.copyOf(this.jobs);
    }

    // a job they hold in this category (the first), or null
    public @Nullable Job holder(Job.Category category) {
        for (Job job : this.jobs) {
            if (job.category == category) return job;
        }
        return null;
    }

    public int count(Job.Category category) {
        int n = 0;
        for (Job job : this.jobs) if (job.category == category) n++;
        return n;
    }

    public int slots(Job.Category category) {
        return category == Job.Category.ESSENTIAL ? this.essentialSlots : this.funSlots;
    }

    public void setSlots(Job.Category category, int slots) {
        int s = Math.max(1, Math.min(MAX_SLOTS, slots));
        if (category == Job.Category.ESSENTIAL) this.essentialSlots = s;
        else this.funSlots = s;
    }

    // one more slot of this kind (a Wishing Star); false if they already have the most
    public boolean addSlot(Job.Category category) {
        if (this.slots(category) >= MAX_SLOTS) return false;
        this.setSlots(category, this.slots(category) + 1);
        return true;
    }

    public boolean hasFreeSlot(Job.Category category) {
        return this.count(category) < this.slots(category);
    }

    // every slot filled
    public boolean full() {
        for (Job.Category c : Job.Category.values()) if (this.hasFreeSlot(c)) return false;
        return true;
    }

    // the kind of job let go of while changing, or null
    public Job.@org.jspecify.annotations.Nullable Category released() {
        return this.released < 0 || this.released >= Job.Category.values().length ? null : Job.Category.values()[this.released];
    }

    public void setReleased(Job.@org.jspecify.annotations.Nullable Category category) {
        this.released = category == null ? -1 : category.ordinal();
    }

    public boolean freePick() {
        return this.freePick;
    }

    public void setFreePick(boolean freePick) {
        this.freePick = freePick;
    }

    public boolean changing() {
        return this.changing;
    }

    public void setChanging(boolean changing) {
        this.changing = changing;
        if (!changing) this.released = -1;
    }

    // False if they already have it or have no free slot of its category.
    public boolean add(Job job) {
        if (this.jobs.contains(job) || !this.hasFreeSlot(job.category)) return false;
        return this.jobs.add(job);
    }

    public boolean remove(Job job) {
        return this.jobs.remove(job);
    }

    public int mask() {
        int mask = 0;
        for (Job job : this.jobs) mask |= job.bit();
        return mask;
    }
}
