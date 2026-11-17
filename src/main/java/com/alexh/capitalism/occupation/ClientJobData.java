package com.alexh.capitalism.occupation;

// Client-side copy of the player's jobs, their job slots and whether a Changing Star is in use. The server owns the
// real ones (JobData).
public class ClientJobData {
    public static int jobs;
    public static int essentialSlots = 1;
    public static int funSlots = 1;
    public static boolean changing;

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
