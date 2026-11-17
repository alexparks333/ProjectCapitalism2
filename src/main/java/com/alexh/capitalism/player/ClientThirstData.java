package com.alexh.capitalism.player;

// Client-side store for the thirst and golden bar values received from the server.
// Only used for HUD rendering and the Wellness screen — the server owns the real values.
public class ClientThirstData {
    public static int thirst = 20;
    public static int goldThirst = 0;
    public static int goldFood = 0;
    public static float purity = 70;
    // Highest the thirst / food bars can go right now (below 20 = drops/drumsticks lost to Low purity / a Poor diet).
    public static int thirstCap = 20;
    public static int foodCap = 20;
}
