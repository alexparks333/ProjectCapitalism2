package com.alexh.capitalism.occupation;

// Client-side copy of the player's jobs, their job slots and whether a Changing Star is in use. The server owns the
// real ones (JobData).
public class ClientJobData {
    public static int jobs;
    public static int essentialSlots = 1;
    public static int funSlots = 1;
    public static boolean changing;
    // the job drafts (JobDraft), per Job.Category ordinal: the job screen's line ("" = no draft) and whether that
    // column is out of reach for us right now (not our turn)
    public static String[] draftLine = {"", ""};
    public static boolean[] draftLocked = {false, false};

    // the draft keeps this kind of job out of reach right now (a Changing Star's change isn't part of it)
    public static boolean draftLocks(Job.Category category) {
        return !changing && draftLocked[category.ordinal()];
    }

    // the line for the bottom of the job screen: our turn first, else any draft running ("" = none)
    public static String draftText() {
        if (changing) return "";
        for (Job.Category category : Job.Category.values()) {
            if (!draftLine[category.ordinal()].isEmpty() && !draftLocked[category.ordinal()]) return draftLine[category.ordinal()];
        }
        for (String line : draftLine) if (!line.isEmpty()) return line;
        return "";
    }

    public static boolean draftYourTurn() {
        for (Job.Category category : Job.Category.values()) {
            if (!draftLine[category.ordinal()].isEmpty() && !draftLocked[category.ordinal()]) return true;
        }
        return false;
    }

    public static boolean has(Job job) {
        return (jobs & job.bit()) != 0;
    }

    public static int slots(Job.Category category) {
        return category == Job.Category.ESSENTIAL ? essentialSlots : funSlots;
    }

    public static int count(Job.Category category) {
        int n = 0;
        for (Job job : Job.values()) if (job.category == category && has(job)) n++;
        return n;
    }
}
