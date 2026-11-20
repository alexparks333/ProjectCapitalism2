// Paints the Blood Moon decorations (client/BloodDecorModels, Decorations.md > Blood Moon decorations): the second
// half of BoatTex (same layout of zones as client/BoatKit, compiled with tools/BoatTex.cs by tools/make_boats.ps1).
// Each: textures/entity/<id>.png, <id>_glow.png (only what glows), textures/item/<id>.png (32 x 32).
// The palette: crimson velvet, black iron, gold leaf, bone, dark gothic stone, ebony, and glowing blood red.
using System;
using System.Collections.Generic;
using System.Drawing;

public static partial class BoatTex
{
    static readonly string[] BloodNames = { "blood_moon_lantern", "crimson_candelabra", "crimson_throne", "vampire_coffin", "blood_fountain",
        "blood_moon_banner", "crimson_rug", "revenant_greatsword_statue", "gorefang_skull_trophy", "sanguine_staff_pedestal", "bat_gargoyle",
        "grandfather_clock", "crimson_chandelier", "blood_pipe_organ", "secret_bookshelf_door", "haunted_portrait", "iron_maiden", "blood_moon_altar",
        "bat_roost", "crimson_canopy_bed", "blood_crystal_fireplace",
        // the Halloween ones (the Blood Merchant's Spooky tab; models in client/SpookyDecorModels)
        "grinning_jack_o_lantern", "haunted_tombstone", "possessed_scarecrow" };

    static bool IsBlood(string name) { return Array.IndexOf(BloodNames, name) >= 0 || name == "bottle_of_blood"; }

    // just the Blood Moon decorations (quicker than MakeAll while working on them)
    public static string MakeBlood(string entityDir, string itemDir)
    {
        foreach (string name in BloodNames)
        {
            img = new Color[N, N];
            glow = new bool[N, N];
            glowColor = new Color[N, N];
            Style st = Make(name);
            Common(st);
            Pictures(name);
            Extra(name);
            Save(img, N, N, System.IO.Path.Combine(entityDir, name + ".png"), (x, y) => true);
            Color[,] lit = new Color[N, N];
            for (int y = 0; y < N; y++) for (int x = 0; x < N; x++) lit[x, y] = glowColor[x, y].A > 0 ? glowColor[x, y] : img[x, y];
            Save(lit, N, N, System.IO.Path.Combine(entityDir, name + "_glow.png"), (x, y) => glow[x, y]);
            Icon(name);
            Save(icon, 32, 32, System.IO.Path.Combine(itemDir, name + ".png"), (x, y) => true);
        }
        // the Bottle of Blood (an item only)
        Icon("bottle_of_blood");
        Save(icon, 32, 32, System.IO.Path.Combine(itemDir, "bottle_of_blood.png"), (x, y) => true);
        return "saved";
    }

    // ── the palette ──
    // an orange pumpkin skin with deep ribs (the Halloween jack-o'-lantern)
    static Color Pumpkin(double u, double v, int seed)
    {
        Color c = Mix(Rgb(206, 96, 18), Rgb(246, 146, 40), Fbm(u * 0.4, v * 0.4, seed));
        c = Mul(c, 0.72 + 0.28 * Math.Abs(Math.Sin(u * 1.3)));
        if (Hash((int)(u * S), (int)(v * S), seed + 1) < 0.03) c = Mul(c, 0.85);
        return c;
    }
    static Color BlackIron(double u, double v)
    {
        Color c = Mix(Rgb(20, 19, 23), Rgb(64, 60, 68), Fbm(u * 0.6, v * 0.6, 910));
        c = Mul(c, 0.82 + 0.32 * Smooth(u * 1.3, v * 1.3, 911));
        if (Fbm(u * 1.1, v * 1.1, 912) > 0.73) c = Mix(c, Rgb(88, 36, 26), 0.35);
        if (Hash((int)(u * S), (int)(v * S), 913) < 0.02) c = Mix(c, Rgb(160, 156, 166), 0.5);
        return c;
    }
    static Color GoldLeaf(double u, double v, int seed)
    {
        double sheen = 0.5 + 0.5 * Math.Sin(v * 2.4 + u * 0.6 + Fbm(u * 0.4, v * 0.4, 915 + seed) * 2);
        Color c = Mix(Rgb(138, 88, 22), Rgb(255, 218, 116), sheen);
        c = Mul(c, 0.9 + 0.16 * Fbm(u * 1.5, v * 1.5, 916 + seed));
        if (Hash((int)(u * S), (int)(v * S), 917 + seed) < 0.015) c = Rgb(255, 248, 210);
        return c;
    }
    static Color Ruby(double u, double v)
    {
        Color c = Mix(Rgb(110, 4, 14), Rgb(232, 40, 52), 0.5 + 0.5 * Math.Sin(u * 1.3 + v * 2.1));
        if (Hash((int)(u * S), (int)(v * S), 918) < 0.04) c = Rgb(255, 200, 200);
        return c;
    }
    static Color Velvet(double u, double v, bool tufted)
    {
        double fold = 0.8 + 0.2 * Math.Sin(u * 0.7 + Fbm(u * 0.2, v * 0.2, 921) * 3);
        Color c = Mul(Mix(Rgb(78, 6, 16), Rgb(162, 18, 34), Fbm(u * 0.5, v * 0.5, 920)), fold);
        if (Hash((int)(u * S), (int)(v * S), 922) < 0.05) c = Mul(c, 1.18);   // the nap catching the light
        if (!tufted) return c;
        // button tufting: diamonds 3 px across, creases between them, a gold button where they meet, plump in the middle
        double a = (u + v) / 3.0, b = (u - v + 300) / 3.0;
        double fa = a - Math.Floor(a), fb = b - Math.Floor(b);
        double da = Math.Min(fa, 1 - fa), db = Math.Min(fb, 1 - fb);
        double crease = Math.Min(da, db);
        c = Mul(c, 0.6 + 0.6 * Math.Min(1, crease * 3.0) + 0.12 * (da * db * 4));
        if (da < 0.1 && db < 0.1) c = Mix(Rgb(200, 150, 60), Rgb(255, 226, 140), 1 - (da + db) * 5);
        return c;
    }
    static Color GripLeather(double u, double v)
    {
        double w = ((u * 0.8 + v) % 1.4) / 1.4;
        Color c = Mix(Rgb(40, 16, 14), Rgb(88, 36, 28), Fbm(u * 0.8, v * 0.8, 925));
        c = Mul(c, 0.7 + 0.45 * Math.Sin(w * Math.PI));
        if (w < 0.12) c = Mul(c, 0.5);
        return c;
    }
    // dark gothic ashlar: blocks in courses, dark mortar, pits, old stains
    static Color StoneBlocks(double u, double v)
    {
        double row = Math.Floor(v / 2.6);
        double uu = u + (row % 2) * 2.0;
        double col = Math.Floor(uu / 4.2);
        double fu = uu / 4.2 - col, fv = v / 2.6 - row;
        double edge = Math.Min(Math.Min(fu, 1 - fu) * 4.2, Math.Min(fv, 1 - fv) * 2.6);
        if (edge < 0.17) return Mix(Rgb(28, 26, 30), Rgb(44, 40, 44), Fbm(u, v, 931));
        double pick = Hash((int)col, (int)row, 932);
        Color s = Mix(Rgb(60, 56, 62), Rgb(98, 94, 98), pick * 0.6 + 0.4 * Fbm(u * 0.8, v * 0.8, 933));
        s = Mul(s, 0.82 + 0.22 * Math.Min(1, edge / 0.7));
        if (Fbm(u * 1.6, v * 1.6, 934) > 0.74) s = Mul(s, 0.72);
        if (Fbm(u * 0.35, v * 0.35, 935) > 0.7) s = Mix(s, Rgb(76, 36, 38), 0.3);
        return s;
    }
    // smoother carved stone, a little paler, faint fluting
    static Color CarvedStone(double u, double v)
    {
        Color c = Mix(Rgb(92, 88, 96), Rgb(136, 132, 138), Fbm(u * 0.5, v * 0.5, 936));
        c = Mul(c, 0.86 + 0.16 * Math.Abs(Math.Sin(u * 1.6)));
        if (Hash((int)(u * S), (int)(v * S), 937) < 0.05) c = Mul(c, 0.8);
        if (Fbm(u * 0.3, v * 0.3, 938) > 0.72) c = Mix(c, Rgb(70, 76, 60), 0.25);
        return c;
    }
    // big rough blocks, cracked
    static Color CrackedStone(double u, double v)
    {
        Color c = Mix(Rgb(62, 58, 64), Rgb(104, 100, 104), Fbm(u * 0.4, v * 0.4, 940));
        c = Mul(c, 0.85 + 0.2 * Smooth(u * 1.2, v * 1.2, 941));
        if (Math.Abs(Fbm(u * 0.45, v * 0.45, 942) - 0.5) < 0.022) c = Rgb(26, 22, 26);
        if (Math.Abs(Fbm(u * 0.8, v * 0.8, 943) - 0.5) < 0.012) c = Mul(c, 0.55);
        return c;
    }
    static Color Flagstones(double u, double v)
    {
        double cu = Math.Floor(u / 5.5 + 0.3 * Smooth(v * 0.3, 0, 944)), cv = Math.Floor(v / 5.0);
        double fu = u / 5.5 - Math.Floor(u / 5.5), fv = v / 5.0 - cv;
        if (Math.Min(Math.Min(fu, 1 - fu) * 5.5, Math.Min(fv, 1 - fv) * 5.0) < 0.2) return Rgb(30, 28, 30);
        return Mul(Mix(Rgb(58, 54, 60), Rgb(90, 86, 90), Hash((int)cu, (int)cv, 945)), 0.85 + 0.25 * Fbm(u * 0.6, v * 0.6, 946));
    }
    static Color Ebony(double u, double v, int seed)
    {
        Color c = Planks(Rgb(54, 32, 30), u, v, 2.0, seed, 0.62);
        return Mul(c, 0.92 + 0.14 * Math.Max(0, Math.Sin(v * 0.7 + u * 0.12)));
    }
    static Color Lacquer(double u, double v)
    {
        Color c = Mix(Rgb(16, 10, 12), Rgb(52, 22, 24), Fbm(u * 0.3, v * 0.9, 947));
        c = Mul(c, 0.9 + 0.2 * (Fbm(u * 0.25, v * 5.0, 948) - 0.5));
        if (((u * 0.5 + v) % 7.0) < 0.45) c = Mix(c, Rgb(130, 86, 86), 0.3);   // a gloss streak
        return c;
    }
    static Color BoneColour(double u, double v)
    {
        Color c = Mul(Mix(Rgb(194, 182, 152), Rgb(238, 230, 208), Fbm(u * 0.5, v * 0.5, 950)), 0.84 + 0.16 * Math.Abs(Math.Sin(v * 1.9)));
        if (Math.Abs(Fbm(u * 0.6, v * 0.6, 951) - 0.5) < 0.018) c = Mul(c, 0.55);
        if (Fbm(u * 0.4, v * 0.4, 952) > 0.72) c = Mix(c, Rgb(120, 84, 56), 0.3);
        return c;
    }
    // ridged ram's horn: rings along it, dark toward the tips
    static Color Horn(double u, double v)
    {
        double ring = Math.Abs(Math.Sin(v * 2.6 + Fbm(u, v, 955) * 0.8));
        Color c = Mix(Rgb(112, 92, 70), Rgb(214, 198, 164), Fbm(u * 0.3, v * 0.15, 956));
        return Mul(c, 0.66 + 0.34 * ring);
    }
    static Color Wax(double u, double v)
    {
        Color c = Mix(Rgb(118, 6, 20), Rgb(200, 26, 42), Fbm(u * 0.6, v * 0.4, 960));
        c = Mul(c, 0.86 + 0.22 * Math.Sin(u * 1.2));
        if (Fbm(u * 1.4, v * 0.5, 961) > 0.7) c = Mix(c, Rgb(236, 90, 96), 0.3);   // where it's thin and the light comes through
        return c;
    }
    static Color DarkSteel(double u, double v)
    {
        Color c = Mix(Rgb(30, 30, 36), Rgb(72, 72, 82), Fbm(u * 0.25, v * 2.5, 970));
        if (Hash((int)(u * 3), (int)(v * 0.6), 971) < 0.025) c = Mix(c, Rgb(170, 170, 182), 0.6);
        if (Math.Abs(Math.Sin(u * 0.4 + v * 0.05)) > 0.97) c = Mix(c, Rgb(150, 30, 30), 0.25);
        return c;
    }
    static Color BloodGlow(double u, double v)
    {
        Color c = Mix(Rgb(205, 12, 26), Rgb(255, 120, 108), Fbm(u * 0.7, v * 0.7, 975));
        if (Hash((int)(u * S), (int)(v * S), 976) < 0.02) c = Rgb(255, 222, 210);
        return c;
    }
    static Color MoonOrb(double u, double v)
    {
        Color c = Mix(Rgb(176, 14, 20), Rgb(255, 92, 66), Fbm(u * 0.5, v * 0.5, 985));
        double cr = Smooth(u * 0.9, v * 0.9, 986);
        if (cr > 0.72) c = Mul(c, 0.72);
        else if (cr > 0.66) c = Mix(c, Rgb(255, 170, 140), 0.4);   // crater rims catching the light
        return c;
    }
    static Color Satin(double u, double v)
    {
        double p = Math.Sin(u * 2.2 + 0.3 * Math.Sin(v * 0.4));
        Color c = Mix(Rgb(104, 6, 18), Rgb(214, 44, 58), 0.5 + 0.5 * p);
        c = Mix(c, Rgb(255, 170, 170), Math.Pow(Math.Max(0, p), 8) * 0.45);
        return c;
    }

    // a blood crystal: facets of deep and bright red, bright edges, glints
    static Color CrystalFacets(double u, double v)
    {
        double fa = u / 1.6 + v / 2.6, fb = v / 1.6 - u / 2.6;
        double pick = Hash((int)Math.Floor(fa), (int)Math.Floor(fb), 990);
        Color c = Mix(Rgb(140, 0, 22), Rgb(255, 72, 96), 0.25 + 0.75 * pick);
        double ea = fa - Math.Floor(fa), eb = fb - Math.Floor(fb);
        if (Math.Min(Math.Min(ea, 1 - ea), Math.Min(eb, 1 - eb)) < 0.07) c = Rgb(255, 190, 200);
        if (Hash((int)(u * S), (int)(v * S), 991) < 0.015) c = Rgb(255, 255, 255);
        return c;
    }

    static void BloodDecor(string name)
    {
        if (!IsBlood(name)) return;
        bool tufted = name == "crimson_throne" || name == "vampire_coffin";
        Zone(168, 44, 40, 40, BlackIron, false);                                                                  // ENGINE
        Zone(128, 44, 40, 40, (u, v) => GoldLeaf(u, v, 1), false);                                                // BRASS
        Zone(224, 32, 32, 12, (u, v) => GoldLeaf(u, v, 2), false);                                                // GOLD
        Zone(232, 20, 24, 12, (u, v) => Mul(Rgb(14, 8, 10), 0.85 + 0.3 * Hash((int)(u * S), (int)(v * S), 901)), false);   // BLACK
        Zone(192, 32, 32, 12, Ruby, false);                                                                       // RED
        Zone(0, 84, 64, 32, (u, v) => Velvet(u, v, tufted), false);                                               // CANVAS
        Zone(128, 20, 40, 24, GripLeather, false);                                                                // LEATHER
        Zone(208, 44, 48, 40, StoneBlocks, false);                                                                // GREY
        Zone(64, 84, 64, 32, CarvedStone, false);                                                                 // WHITE
        Zone(0, 148, 64, 32, CrackedStone, false);                                                                // MOSS
        Zone(0, 116, 128, 32, Flagstones, false);                                                                 // DECK
        Zone(192, 84, 64, 32, (u, v) => Ebony(u, v, 903), false);                                                 // DARKWOOD
        Zone(128, 84, 64, 32, (u, v) => Ebony(u, v, 904), false);                                                 // WOOD
        Zone(128, 148, 64, 32, Lacquer, false);                                                                   // OLDWOOD
        Zone(192, 148, 32, 32, BoneColour, false);                                                                // BONE
        Zone(224, 148, 32, 32, Horn, false);                                                                      // SCALE
        Zone(96, 148, 32, 32, Wax, false);                                                                        // BAMBOO
        Zone(64, 148, 32, 32, (u, v) => Mix(Rgb(20, 50, 24), Rgb(52, 92, 42), Fbm(u, v, 905)), false);            // VINE
        Zone(48, 68, 16, 16, (u, v) => (((u + v) * 2.6) % 1.0) < 0.4 ? Rgb(150, 100, 30) : Rgb(238, 192, 92), false);   // ROPE
        Zone(168, 20, 24, 24, (u, v) => Mix(Rgb(120, 118, 128), Rgb(226, 228, 238), 0.5 + 0.5 * Math.Sin(v * 1.7 + u * 2.1)), false);   // CHROME
        Zone(192, 116, 64, 32, DarkSteel, false);                                                                 // STEEL
        Zone(128, 116, 64, 32, BloodGlow, true);                                                                  // ICE (glowing blood)
        Zone(64, 60, 16, 8, (u, v) => Mix(Rgb(232, 40, 12), Rgb(255, 172, 84), Fbm(u * 1.2, v * 1.2, 980)), true);    // FIRE
        Zone(64, 52, 8, 8, (u, v) => Mix(Rgb(255, 34, 30), Rgb(255, 214, 186), Math.Max(0, 1 - Math.Abs(v - 4) / 3.5)), true);   // NAV_RED
        Zone(96, 52, 32, 16, MoonOrb, true);                                                                      // WARM
        switch (name)
        {
            case "vampire_coffin":
                Zone(64, 84, 64, 32, Satin, false);                                                               // the lining
                Zone(0, 116, 128, 32, (u, v) => Ebony(u * 0.5, v, 906), false);
                break;
            case "crimson_candelabra":
                Zone(64, 60, 16, 8, (u, v) => Mix(Rgb(255, 110, 16), Rgb(255, 236, 150), Fbm(u * 1.4, v * 1.0, 981)), true);
                Zone(96, 52, 32, 16, (u, v) => Mix(Rgb(255, 250, 226), Rgb(255, 220, 140), Fbm(u, v, 982)), true);
                break;
            case "grandfather_clock":
                Zone(0, 84, 64, 32, (u, v) => Velvet(u, v, false), false);
                Zone(128, 148, 64, 32, (u, v) => Mul(Lacquer(u, v), 1.25), false);
                break;
            case "crimson_chandelier":
            case "blood_moon_altar":
                Zone(128, 116, 64, 32, CrystalFacets, true);
                break;
            case "blood_crystal_fireplace":
                Zone(128, 116, 64, 32, CrystalFacets, true);
                // the crimson fire: deep red licking up into scarlet, a hot pink-white heart
                Zone(64, 60, 16, 8, (u, v) => Mix(Rgb(150, 0, 16), Rgb(255, 60, 50), Math.Min(1, Fbm(u * 1.2, v * 1.2, 1060) * 1.3)), true);
                Zone(96, 52, 32, 16, (u, v) => Mix(Rgb(255, 120, 120), Rgb(255, 220, 210), Fbm(u, v, 1061)), true);
                break;
            case "blood_pipe_organ":
                // the pipes: dark silver, a bright streak down each, darker toward the edges
                Zone(192, 116, 64, 32, (u, v) =>
                {
                    double band = Math.Abs(Math.Sin(u * 0.9));
                    Color c = Mix(Rgb(54, 54, 62), Rgb(170, 170, 182), Math.Pow(band, 3));
                    return Mul(c, 0.9 + 0.15 * Fbm(u * 0.3, v * 2.0, 1062));
                }, false);
                Zone(64, 84, 64, 32, (u, v) =>
                {
                    Color c = Mix(Rgb(214, 198, 160), Rgb(236, 224, 192), Fbm(u * 0.6, v * 0.6, 1063));
                    return Hash((int)(u * S), (int)(v * S), 1064) < 0.03 ? Mul(c, 0.8) : c;
                }, false);
                Zone(0, 84, 64, 32, (u, v) => Velvet(u, v, false), false);
                break;
            case "secret_bookshelf_door":
            {
                // the books' leather: crimson, green, brown, plum, black; gold bands and a title across each spine
                Func<Color, Color, int, Painter> book = (dark, lite, seed) => (u, v) =>
                {
                    Color c = Mix(dark, lite, Fbm(u * 0.8, v * 0.8, seed));
                    double band = (v + 100) % 3.6;
                    if (band < 0.3 || (band > 0.6 && band < 0.75)) return GoldLeaf(u * 2, v * 2, seed);
                    if (band > 1.4 && band < 2.0 && Hash((int)(u * S / 2), (int)(v * S), seed + 1) < 0.5) return Mix(c, Rgb(220, 180, 90), 0.6);
                    return Mul(c, 0.85 + 0.2 * Math.Abs(Math.Sin(u * 1.4)));
                };
                Zone(192, 32, 32, 12, book(Rgb(90, 8, 16), Rgb(160, 24, 32), 1070), false);
                Zone(64, 68, 32, 16, book(Rgb(16, 46, 30), Rgb(40, 84, 52), 1071), false);
                Zone(128, 20, 40, 24, book(Rgb(66, 40, 24), Rgb(112, 72, 44), 1072), false);
                Zone(96, 68, 32, 16, book(Rgb(44, 16, 48), Rgb(90, 36, 92), 1073), false);
                Zone(232, 20, 24, 12, book(Rgb(14, 12, 14), Rgb(42, 38, 40), 1074), false);
                break;
            }
            case "haunted_portrait":
                Zone(128, 148, 64, 32, (u, v) => Velvet(u, v, false), false);
                break;
            case "grinning_jack_o_lantern":
                // STEEL: a ribbed orange pumpkin skin; DARKWOOD: its green-brown stem; FIRE: the candlelit face, gold and orange
                Zone(192, 116, 64, 32, (u, v) => Pumpkin(u, v, 1201), false);
                Zone(192, 84, 64, 32, (u, v) => Mix(Rgb(70, 78, 36), Rgb(120, 112, 58), Fbm(u * 2.0, v * 0.3, 1202)), false);
                Zone(64, 60, 16, 8, (u, v) => Mix(Rgb(255, 150, 20), Rgb(255, 232, 110), Fbm(u * 1.2, v * 1.2, 1203)), true);
                break;
            case "haunted_tombstone":
                // WHITE: weathered grey headstone; MOSS: real moss; GHOST: the glowing wisp, pale blue-green
                Zone(64, 84, 64, 32, (u, v) =>
                {
                    Color c = Mix(Rgb(104, 106, 108), Rgb(150, 152, 150), Fbm(u * 0.5, v * 0.5, 1204));
                    if (Fbm(u * 1.6, v * 1.6, 1205) > 0.7) c = Mul(c, 0.82);
                    if (Hash((int)(u * S), (int)(v * S), 1206) < 0.05) c = Mul(c, 0.75);
                    return c;
                }, false);
                Zone(0, 148, 64, 32, (u, v) => Mix(Rgb(40, 70, 28), Rgb(96, 132, 52), Fbm(u * 0.9, v * 0.9, 1207)), false);
                Zone(80, 60, 16, 8, (u, v) => Mix(Rgb(150, 255, 230), Rgb(230, 255, 250), Fbm(u * 1.5, v * 1.5, 1208)), true);
                break;
            case "possessed_scarecrow":
                // CANVAS: a ragged charcoal coat; OLDWOOD: grey weathered wood; LEATHER: burlap (the sack head, belt, patch); BAMBOO: straw; FIRE: sickly green eyes
                Zone(0, 84, 64, 32, (u, v) =>
                {
                    Color c = Mix(Rgb(30, 26, 34), Rgb(62, 52, 66), Fbm(u * 0.4, v * 0.4, 1209));
                    if (Hash((int)(u / 4), (int)(v / 4), 1210) < 0.2) c = Mix(c, Rgb(70, 40, 76), 0.5);
                    if ((u % 4) < 0.25 && ((int)v % 2 == 0)) c = Mul(c, 0.6);
                    return c;
                }, false);
                Zone(128, 148, 64, 32, (u, v) =>
                {
                    Color c = Mix(Rgb(70, 64, 58), Rgb(122, 112, 100), Fbm(u * 1.4, v * 0.3, 1211));
                    if (Math.Abs(Math.Sin(u * 2.6 + Fbm(u * 0.4, v * 0.2, 1212) * 4)) < 0.12) c = Mul(c, 0.6);
                    return c;
                }, false);
                Zone(128, 20, 40, 24, (u, v) =>
                {
                    Color c = Mix(Rgb(140, 110, 70), Rgb(186, 156, 104), Fbm(u * 0.8, v * 0.8, 1213));
                    if (((int)(u * S) + (int)(v * S)) % 3 == 0) c = Mul(c, 0.84);
                    return c;
                }, false);
                Zone(96, 148, 32, 32, (u, v) => Mix(Rgb(196, 160, 70), Rgb(240, 214, 120), Fbm(u * 2.5, v * 0.3, 1214)), false);
                Zone(64, 60, 16, 8, (u, v) => Mix(Rgb(120, 255, 60), Rgb(220, 255, 170), Fbm(u * 1.2, v * 1.2, 1215)), true);
                break;
            case "iron_maiden":
                Zone(0, 84, 64, 32, (u, v) => Mul(Velvet(u, v, false), 0.6), false);
                Zone(208, 44, 48, 40, (u, v) => Mix(Rgb(70, 66, 70), Rgb(120, 116, 118), Fbm(u * 0.8, v * 0.8, 1075)), false);
                break;
            case "bat_roost":
                // gnarled dead wood: grey-brown bark, deep twisting cracks
                Zone(128, 148, 64, 32, (u, v) =>
                {
                    Color c = Mix(Rgb(52, 44, 40), Rgb(108, 96, 86), Fbm(u * 1.4, v * 0.3, 1076));
                    if (Math.Abs(Math.Sin(u * 2.6 + Fbm(u * 0.4, v * 0.2, 1077) * 4)) < 0.15) c = Mul(c, 0.5);
                    return c;
                }, false);
                Zone(96, 68, 32, 16, (u, v) =>
                {
                    Color c = Mix(Rgb(30, 20, 22), Rgb(62, 40, 40), Fbm(u * 0.6, v * 0.6, 1078));
                    if (Math.Abs(Math.Sin(u * 1.2 + v * 0.4)) < 0.08) c = Mul(c, 0.6);
                    return c;
                }, false);
                break;
            case "crimson_canopy_bed":
                Zone(128, 20, 40, 24, Satin, false);
                Zone(128, 116, 64, 32, Satin, false);
                Zone(0, 116, 128, 32, (u, v) => Mix(Rgb(226, 222, 212), Rgb(250, 248, 242), Fbm(u * 0.5, v * 0.5, 1079)), false);
                Zone(0, 0, 128, 20, (u, v) => Velvet(u, v, false), false);
                Zone(64, 84, 64, 32, (u, v) => Mix(Rgb(226, 222, 212), Rgb(250, 248, 242), Fbm(u * 0.5, v * 0.5, 1079)), false);
                Zone(0, 84, 64, 32, (u, v) => Mul(Velvet(u, v, false), 0.9 + 0.2 * Math.Abs(Math.Sin(u * 1.6))), false);
                break;
            case "sanguine_staff_pedestal":
                // the crystal: facets of deep and bright blood red, glints
                Zone(128, 116, 64, 32, (u, v) =>
                {
                    double fa = u / 1.6 + v / 2.6, fb = v / 1.6 - u / 2.6;
                    double pick = Hash((int)Math.Floor(fa), (int)Math.Floor(fb), 990);
                    Color c = Mix(Rgb(140, 0, 22), Rgb(255, 72, 96), 0.25 + 0.75 * pick);
                    double ea = fa - Math.Floor(fa), eb = fb - Math.Floor(fb);
                    if (Math.Min(Math.Min(ea, 1 - ea), Math.Min(eb, 1 - eb)) < 0.07) c = Rgb(255, 190, 200);
                    if (Hash((int)(u * S), (int)(v * S), 991) < 0.015) c = Rgb(255, 255, 255);
                    return c;
                }, true);
                break;
            case "gorefang_skull_trophy":
                Zone(128, 148, 64, 32, (u, v) => Velvet(u, v, false), false);                                    // the plaque's velvet
                break;
            case "bat_gargoyle":
                // the gargoyle's own stone: weathered grey, pitted, streaked darker where rain runs, lichen
                Zone(64, 84, 64, 32, (u, v) =>
                {
                    Color c = Mix(Rgb(88, 86, 90), Rgb(132, 130, 134), Fbm(u * 0.5, v * 0.5, 993));
                    c = Mul(c, 0.86 + 0.18 * Smooth(u * 1.5, v * 1.5, 994));
                    if (Fbm(u * 2.0, v * 0.3, 995) > 0.7) c = Mul(c, 0.78);
                    if (Hash((int)(u * S), (int)(v * S), 996) < 0.06) c = Mul(c, 0.7);
                    if (Fbm(u * 0.4, v * 0.4, 997) > 0.74) c = Mix(c, Rgb(120, 130, 80), 0.3);
                    return c;
                }, false);
                Zone(0, 148, 64, 32, (u, v) => Mix(Rgb(44, 72, 30), Rgb(104, 140, 56), Fbm(u * 0.9, v * 0.9, 998)), false);   // real moss
                Zone(192, 148, 32, 32, (u, v) => Mix(Rgb(150, 148, 146), Rgb(190, 188, 184), Fbm(u, v, 999)), false);       // pale stone fangs
                break;
        }
        BloodPictures(name);
    }

    // ── pictures (below y 180) ──
    static void Paint(int U, int V, double w, double h, Func<double, double, Color> paint, Func<double, double, double> glowAlpha)
    {
        for (int y = V * S; y < (int)Math.Ceiling((V + h) * S); y++)
            for (int x = U * S; x < (int)Math.Ceiling((U + w) * S); x++)
            {
                double c = (x + 0.5) / S - U, s = (y + 0.5) / S - V;
                Color col = paint(c, s);
                img[x, y] = col;
                double a = glowAlpha == null ? 0 : glowAlpha(c, s);
                glow[x, y] = a > 0 && col.A > 0;
                glowColor[x, y] = a > 0 ? Argb(255 * Math.Min(1, a), col.R, col.G, col.B) : Clear;
            }
    }

    static double SegDist(double px, double py, double ax, double ay, double bx, double by, out double t)
    {
        double dx = bx - ax, dy = by - ay;
        t = Math.Max(0, Math.Min(1, ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy)));
        double qx = ax + dx * t - px, qy = ay + dy * t - py;
        return Math.Sqrt(qx * qx + qy * qy);
    }

    static bool InPoly(double x, double y, List<double[]> p)
    {
        bool inside = false;
        for (int i = 0, j = p.Count - 1; i < p.Count; j = i++)
        {
            if ((p[i][1] > y) != (p[j][1] > y) && x < (p[j][0] - p[i][0]) * (y - p[i][1]) / (p[j][1] - p[i][1]) + p[i][0]) inside = !inside;
        }
        return inside;
    }

    // a bat's wing: its root at the left edge (from rootTop down to rootBot), the arm up to the wrist, four fingers out to
    // their tips, the membrane between them cut in scallops
    class BatWingShape
    {
        public double RootTop, RootBot;
        public double[] Wrist;
        public double[][] Tips;
        public bool In(double c, double s)
        {
            var poly = new List<double[]> { new[] { 0.0, RootTop }, Wrist };
            poly.AddRange(Tips);
            poly.Add(new[] { 0.0, RootBot });
            if (!InPoly(c, s, poly)) return false;
            for (int i = 0; i < Tips.Length; i++)
            {
                double[] a = Tips[i], b = i + 1 < Tips.Length ? Tips[i + 1] : new[] { 0.0, RootBot };
                double mx = (a[0] + b[0]) / 2, my = (a[1] + b[1]) / 2, dx = b[0] - a[0], dy = b[1] - a[1];
                double len = Math.Sqrt(dx * dx + dy * dy), nx = dy / len, ny = -dx / len;
                if ((mx - Wrist[0]) * nx + (my - Wrist[1]) * ny < 0) { nx = -nx; ny = -ny; }
                double k = i + 1 < Tips.Length ? 0.6 : 1.1;
                double cx = mx + nx * k * len, cy = my + ny * k * len, r2 = len * len / 4 + k * k * len * len;
                if ((c - cx) * (c - cx) + (s - cy) * (s - cy) < r2) return false;
            }
            return true;
        }
        // how near a bone it is (0 on one; > 1 clear of them)
        public double Bone(double c, double s)
        {
            double t;
            double best = SegDist(c, s, 0, (RootTop + RootBot) / 2, Wrist[0], Wrist[1], out t) / 0.65;
            foreach (double[] tip in Tips)
            {
                double d = SegDist(c, s, Wrist[0], Wrist[1], tip[0], tip[1], out t);
                best = Math.Min(best, d / (0.45 * (1 - 0.55 * t)));
            }
            return best;
        }
        public bool Edge(double c, double s, double e)
        {
            return !In(c - e, s) || !In(c + e, s) || !In(c, s - e) || !In(c, s + e);
        }
    }

    // the wing picture w wide, both its sides: the second mirrored (it's the same wing seen from behind)
    static void WingPicture(int U, int V, double w, double h, BatWingShape shape, bool stone)
    {
        Paint(U, V, 2 * w, h, (cc, s) =>
        {
            double c = cc < w ? cc : 2 * w - cc;
            if (!shape.In(c, s)) return Clear;
            double bone = shape.Bone(c, s);
            bool edge = shape.Edge(c, s, 0.3);
            double fromWrist = Math.Sqrt((c - shape.Wrist[0]) * (c - shape.Wrist[0]) + (s - shape.Wrist[1]) * (s - shape.Wrist[1]));
            if (stone)
            {
                if (bone < 1) return Mul(Mix(Rgb(132, 128, 132), Rgb(166, 162, 164), Fbm(c, s, 1001)), 0.9 + 0.2 * (1 - bone));
                Color m = Mix(Rgb(78, 76, 82), Rgb(114, 110, 116), Fbm(c * 0.6, s * 0.6, 1002));
                m = Mul(m, 0.86 + 0.14 * Math.Sin(fromWrist * 3.2));                                  // wrinkles
                if (Hash((int)(c * S), (int)(s * S), 1003) < 0.06) m = Mul(m, 0.75);
                if (edge) m = Mul(m, 0.62);
                return m;
            }
            // carved black wood, gilded bones and edges, a hint of crimson in the hollows
            if (bone < 1 || edge) return GoldLeaf(c * 2, s * 2, 7);
            Color mem = Mix(Rgb(18, 10, 12), Rgb(62, 16, 22), Fbm(c * 0.5, s * 0.5, 1004));
            mem = Mul(mem, 0.8 + 0.3 * Math.Abs(Math.Sin(fromWrist * 2.0)));
            if (bone < 1.6) mem = Mul(mem, 0.6);
            return mem;
        }, null);
    }

    // a bat seen from below, `size` across its wings (dx, dy from its middle, in units of size)
    static bool Bat(double dx, double dy)
    {
        double ax = Math.Abs(dx);
        if (ax < 0.12 && dy > -0.3 && dy < 0.28) return true;                                  // the body
        if (InEllipse(dx, dy, 0, -0.34, 0.11, 0.1)) return true;                                // the head
        if (ax < 0.1 && ax > 0.03 && dy > -0.5 && dy < -0.38) return true;                      // ears
        if (ax > 1.0 || ax < 0.1) return false;
        double top = -0.14 - 0.3 * ax + 0.12 * ax * ax;
        double bottom = 0.2 - 0.12 * ax - 0.16 * Math.Abs(Math.Sin(ax * Math.PI * 2.4));
        return dy > top && dy < bottom;
    }

    // the Blood Moon on the banner and the rug: crimson, darker craters, a bright rim; 0 to 1 how far out
    static Color MoonFace(double dx, double dy, double r)
    {
        double d = Math.Sqrt(dx * dx + dy * dy) / r;
        Color c = Mix(Rgb(255, 96, 70), Rgb(190, 16, 22), Math.Pow(d, 1.6));
        if (Smooth(dx * 1.4 / r * 5 + 40, dy * 1.4 / r * 5 + 40, 1010) > 0.7) c = Mul(c, 0.74);
        if (d > 0.9) c = Mix(c, Rgb(255, 170, 140), (d - 0.9) * 6);
        return c;
    }

    static void BloodPictures(string name)
    {
        const int PV = 180;
        if (name == "crimson_throne")
        {
            var wing = new BatWingShape { RootTop = 8.4, RootBot = 11.6, Wrist = new[] { 5.6, 3.2 },
                Tips = new[] { new[] { 15.8, 0.3 }, new[] { 15.6, 6.4 }, new[] { 12.2, 11.4 }, new[] { 7.0, 14.6 } } };
            WingPicture(0, PV, 16, 15, wing, false);
        }
        if (name == "bat_gargoyle")
        {
            var wing = new BatWingShape { RootTop = 8.6, RootBot = 12.0, Wrist = new[] { 5.2, 2.8 },
                Tips = new[] { new[] { 13.8, 0.3 }, new[] { 13.6, 5.8 }, new[] { 10.6, 10.4 }, new[] { 6.2, 12.8 } } };
            WingPicture(0, PV, 14, 13, wing, true);
        }
        if (name == "blood_pipe_organ")
        {
            var wing = new BatWingShape { RootTop = 8.4, RootBot = 11.6, Wrist = new[] { 5.6, 3.2 },
                Tips = new[] { new[] { 15.8, 0.3 }, new[] { 15.6, 6.4 }, new[] { 12.2, 11.4 }, new[] { 7.0, 14.6 } } };
            WingPicture(0, PV, 16, 15, wing, false);
        }
        if (name == "grandfather_clock")
        {
            // the dial: a crimson blood moon in the middle, a gold ring, a black chapter ring with gold hour marks
            Paint(80, PV + 10, 9, 9, (c, s) =>
            {
                double dx = c - 4.5, dy = s - 4.5, r = Math.Sqrt(dx * dx + dy * dy);
                if (r < 3.0) return MoonFace(dx, dy, 3.0);
                if (r < 3.3) return GoldLeaf(c * 3, s * 3, 31);
                if (r < 4.35)
                {
                    double a = Math.Atan2(dy, dx) / (Math.PI / 6);
                    double off = Math.Abs(a - Math.Round(a));
                    bool quarter = ((int)Math.Round(a) % 3 + 3) % 3 == 0;
                    if (r > 3.45 && r < 4.2 && off < (quarter ? 0.16 : 0.09)) return GoldLeaf(c * 3, s * 3, 32);
                    return Mix(Rgb(12, 6, 8), Rgb(30, 12, 14), Fbm(c, s, 1080));
                }
                if (r < 4.6) return GoldLeaf(c * 3, s * 3, 33);
                return Mul(Lacquer(c * 2, s * 2), 1.3);
            }, (c, s) => { double dx = c - 4.5, dy = s - 4.5; return dx * dx + dy * dy < 9.0 ? 0.45 : 0; });
        }
        if (name == "iron_maiden")
        {
            // her face: stern, cast iron, eyes shut, a tear of blood from one of them
            Paint(40, PV + 10, 6, 8, (c, s) =>
            {
                double dx = (c - 3.0) / 2.6, dy = (s - 4.2) / 3.8;
                double d = dx * dx + dy * dy;
                Color iron = Mix(Rgb(66, 64, 70), Rgb(150, 146, 150), Math.Max(0, 1 - d) * 0.8 + 0.2 * Fbm(c * 2, s * 2, 1081));
                if (d > 1) return Mix(Rgb(30, 28, 32), Rgb(60, 56, 60), Fbm(c * 2, s * 2, 1082));
                if (Math.Abs(s - 3.2) < 0.2 && Math.Abs(Math.Abs(c - 3.0) - 1.1) < 0.6) return Rgb(24, 20, 24);
                if (Math.Abs(s - 2.5) < 0.18 && Math.Abs(Math.Abs(c - 3.0) - 1.1) < 0.7) return Rgb(40, 36, 40);
                if (Math.Abs(c - 3.0) < 0.3 && s > 3.4 && s < 5.0) return Mul(iron, 1.15);
                if (Math.Abs(s - 6.0) < 0.17 && Math.Abs(c - 3.0) < 0.8) return Rgb(28, 22, 24);
                if (Math.Abs(c - 4.15) < 0.16 && s > 3.4 && s < 5.6) return Rgb(140, 10, 16);
                return iron;
            }, null);
        }
        if (name == "haunted_portrait")
        {
            // the Blood Merchant in oils: hooded, pale, red-lipped, fangs, a high crimson collar with a gold clasp, against
            // a dark red ground; a vignette and fine cracks in the varnish. (His pupils are separate: they move.)
            Paint(20, PV + 20, 11, 26, (c, s) =>
            {
                double vx = (c - 5.5) / 5.5, vy = (s - 10.0) / 13.0;
                double vig = Math.Max(0, 1 - 0.55 * (vx * vx + vy * vy));
                Color col = Mul(Mix(Rgb(36, 6, 10), Rgb(104, 22, 24), Fbm(c * 0.5, s * 0.3, 1090)), 0.55 + 0.45 * vig);
                col = Mul(col, 0.9 + 0.12 * Math.Sin(c * 1.7 + s * 0.6 + Fbm(c, s, 1091) * 3));
                double hx = c - 5.5, hy = s - 8.4;
                bool hood = (hx * hx) / (4.3 * 4.3) + (hy * hy) / (5.6 * 5.6) < 1 && s < 12.5;
                double half = 3.2 + Math.Max(0, s - 11.5) * 0.4;
                bool cloak = s >= 11.0 && Math.Abs(hx) < Math.Min(5.4, half);
                if (hood || cloak)
                {
                    col = Mix(Rgb(12, 5, 8), Rgb(54, 12, 18), Fbm(c * 0.7, s * 0.5, 1092));
                    col = Mul(col, 0.8 + 0.3 * Math.Abs(Math.Sin(hx * 1.4 + s * 0.2)));
                    // the crimson lining showing at the collar
                    if (s > 11.4 && s < 15.5 && Math.Abs(hx) < 2.6 - (s - 11.4) * 0.5) col = Mix(Rgb(130, 10, 24), Rgb(200, 30, 40), Fbm(c, s, 1093));
                    if (Math.Abs(hx) < 0.55 && Math.Abs(s - 13.8) < 0.5) col = GoldLeaf(c * 3, s * 3, 41);
                    if (Math.Abs(hx) < 0.25 && Math.Abs(s - 13.8) < 0.22) col = Rgb(210, 20, 30);
                }
                double fx = (c - 5.5) / 2.15, fy = (s - 8.7) / 2.9;
                if (fx * fx + fy * fy < 1)
                {
                    double shade = Math.Max(0, 1 - (fx * fx + fy * fy));
                    col = Mix(Rgb(120, 116, 128), Rgb(222, 216, 222), 0.35 + 0.65 * shade);
                    if (s < 7.0) col = Mul(col, 0.55 + 0.45 * (s - 5.8) / 1.2);   // the hood's shadow
                    for (int e = -1; e <= 1; e += 2)
                    {
                        double ex = c - (5.5 + e * 1.25), ey = s - 7.3;
                        if ((ex * ex) / 0.36 + (ey * ey) / 0.1 < 1) col = Rgb(214, 196, 188);
                        if (Math.Abs(ey + 0.55) < 0.14 && Math.Abs(ex) < 0.65) col = Rgb(30, 14, 18);
                    }
                    if (Math.Abs(c - 5.5) < 0.18 && s > 7.8 && s < 9.3) col = Mul(col, 0.8);
                    if (Math.Abs(s - 10.3) < 0.18 && Math.Abs(c - 5.5) < 0.75) col = Rgb(150, 12, 24);
                    if (s > 10.4 && s < 10.85 && (Math.Abs(c - 5.05) < 0.12 || Math.Abs(c - 5.95) < 0.12)) col = Rgb(248, 246, 240);
                }
                if (Math.Abs(Fbm(c * 1.6, s * 1.6, 1094) - 0.5) < 0.01) col = Mul(col, 0.7);   // the varnish's cracks
                return col;
            }, null);
        }
        if (name == "blood_fountain")
        {
            // the blood: deep red, rippling, bright on the crests (glowing softly)
            Paint(0, PV, 128, 40, (u, v) =>
            {
                double ripple = 0.5 + 0.5 * Math.Sin(Math.Sqrt((u - 30) * (u - 30) + (v - 20) * (v - 20)) * 1.3 - Fbm(u * 0.2, v * 0.2, 1020) * 4);
                Color c = Mix(Rgb(110, 4, 14), Rgb(214, 28, 40), Fbm(u * 0.5, v * 0.5, 1021) * 0.7 + 0.3 * ripple);
                c = Mix(c, Rgb(255, 120, 116), Math.Pow(ripple, 8) * 0.55);
                if (Hash((int)(u * S), (int)(v * S), 1022) < 0.01) c = Rgb(255, 210, 200);
                return c;
            }, (u, v) => 0.62);
        }
        if (name == "blood_moon_banner")
        {
            // the banner: its back (seen from behind, so mirrored) in columns 0 to 16, its front in 16 to 32
            Paint(0, PV, 32, 34, (x, s) =>
            {
                double c = x < 16 ? 16 - x : x - 16;
                // tattered: the swallow tail, a frayed hem and sides, holes low down
                double rag = Fbm(c * 1.3, s * 0.4, 1030);
                if (s > 26.5 + 7.0 * Math.Abs(c - 8) / 8 - rag * 1.2) return Clear;
                if ((c < 0.2 + rag * 0.5 || c > 15.8 - rag * 0.5) && s > 14) return Clear;
                if (s > 18 && Fbm(c * 0.6 + 9, s * 0.45, 1031) > 0.76) return Clear;
                if (c < -0.5 || c > 16.5) return Clear;
                double fold = 0.78 + 0.22 * Math.Sin(c * 1.1 + Fbm(c * 0.3, s * 0.1, 1032) * 2);
                Color col = Mul(Mix(Rgb(84, 6, 16), Rgb(156, 16, 30), Fbm(c * 0.5, s * 0.3, 1033)), fold);
                if (s > 20) col = Mul(col, 1 - (s - 20) / 40.0);   // darker, more worn toward the hem
                // the gold border down the sides and across the top, a black line inside it
                double side = Math.Min(c, 16 - c);
                if (s < 1.0 || (side < 0.9 && s < 26)) return GoldLeaf(c * 3, s * 3, 11);
                if (s < 1.4 || (side < 1.3 && s < 26)) return Rgb(18, 6, 8);
                // a gold bar under the top with points hanging from it
                if (s > 2.2 && s < 2.8) return GoldLeaf(c * 3, s * 3, 12);
                if (s >= 2.8 && s < 4.0 && Math.Abs(((c + 0.5) % 2.0) - 1.0) < (4.0 - s) * 0.5) return GoldLeaf(c * 3, s * 3, 13);
                // the moon, its halo; bats across it; blood dripping from it
                double mx = c - 8, my = s - 12.5, md = Math.Sqrt(mx * mx + my * my);
                if (Bat((c - 6.3) / 3.6, (s - 11.0) / 3.6) || Bat((c - 10.6) / 2.4, (s - 14.6) / 2.4) || Bat((c - 12.4) / 1.8, (s - 5.6) / 1.8)
                    || Bat((c - 3.4) / 1.5, (s - 19.4) / 1.5)) return Rgb(10, 4, 6);
                if (md < 5.4) return MoonFace(mx, my, 5.4);
                if (md < 6.4) return Mix(col, Rgb(255, 70, 60), (6.4 - md) * 0.5);
                double[] drips = { 6.0, 8.3, 9.7 };
                double[] lens = { 21.0, 23.5, 20.5 };
                for (int i = 0; i < 3; i++)
                {
                    if (Math.Abs(c - drips[i]) < 0.32 && s > 16 && s < lens[i]) return Rgb(120, 6, 14);
                    if (InEllipse(c, s, drips[i], lens[i], 0.5, 0.6)) return Rgb(132, 8, 16);
                }
                // a small gold crescent under it all
                double cx = c - 8, cy = s - 22.6, cd = Math.Sqrt(cx * cx + cy * cy);
                if (cd < 1.5 && cd > 0.85 && cy > -0.4) return GoldLeaf(c * 3, s * 3, 14);
                return col;
            }, (x, s) =>
            {
                double c = x < 16 ? 16 - x : x - 16, mx = c - 8, my = s - 12.5, md = Math.Sqrt(mx * mx + my * my);
                if (Bat((c - 6.3) / 3.6, (s - 11.0) / 3.6) || Bat((c - 10.6) / 2.4, (s - 14.6) / 2.4)) return 0;
                return md < 5.4 ? 0.85 : md < 6.4 ? (6.4 - md) * 0.4 : 0;
            });
        }
        if (name == "crimson_rug")
        {
            // the rug: the top of its box is 48 x 32 at u 32; the rest (its edges) plain crimson. The same design mirrored
            // both ways from the middle, so it reads the same whichever way it's laid.
            Paint(0, PV, 160, 33, (u, v) =>
            {
                if (u < 32 || u >= 80 || v >= 32) return Mul(Rgb(110, 10, 20), 0.9 + 0.2 * Fbm(u, v, 1040));
                double x = u - 32, z = v;
                double ax = Math.Abs(x - 24), az = Math.Abs(z - 16);
                double fromEdge = Math.Min(24 - ax, 16 - az);
                Color field = Mul(Mix(Rgb(96, 8, 18), Rgb(150, 18, 32), Fbm(x * 0.35, z * 0.35, 1041)), 0.9 + 0.12 * Fbm(x * 2, z * 2, 1042));
                // a faint damask lattice in the field
                double dam = Math.Abs(Math.Sin(ax * 0.9) * Math.Sin(az * 0.9));
                if (dam > 0.82) field = Mul(field, 0.82);
                if (fromEdge < 0.6) return Rgb(54, 4, 10);                                                   // the binding
                if (fromEdge < 3.4)
                {
                    // the gold border, a running chain of diamonds along it
                    double along = 24 - ax < 16 - az ? z : x;
                    double across = fromEdge - 0.6;
                    double dm = Math.Abs(((along + 100) % 2.4) - 1.2) + Math.Abs(across - 1.4);
                    if (across < 0.25 || across > 2.55) return Rgb(30, 6, 10);
                    if (dm < 0.7) return Mix(Rgb(120, 8, 20), Rgb(200, 30, 44), 1 - dm);
                    if (dm < 0.95) return Rgb(30, 6, 10);
                    return GoldLeaf(x * 2, z * 2, 21);
                }
                if (fromEdge < 3.9) return Rgb(14, 6, 8);
                if (Math.Abs(fromEdge - 6.0) < 0.2) return GoldLeaf(x * 2, z * 2, 22);                     // an inner gold line
                // crescent moons in the corners
                double kx = ax - 17.5, kz = az - 9.5, kd = Math.Sqrt(kx * kx + kz * kz), kd2 = Math.Sqrt((kx + 0.9) * (kx + 0.9) + (kz + 0.9) * (kz + 0.9));
                if (kd < 2.3 && kd2 > 2.0) return GoldLeaf(x * 2, z * 2, 23);
                // the medallion: a gold-rimmed black ring, the Blood Moon inside, a bat over it, gold studs round it
                double mx = x - 24, mz = z - 16, md = Math.Sqrt(mx * mx * 0.8 + mz * mz);
                for (int i = 0; i < 8; i++)
                {
                    double a = i * Math.PI / 4;
                    if (InEllipse(mx, mz, Math.Cos(a) * 10.4 / Math.Sqrt(0.8), Math.Sin(a) * 10.4, 0.55, 0.55)) return GoldLeaf(x * 2, z * 2, 24);
                }
                if (md < 9.4 && md > 9.0) return GoldLeaf(x * 2, z * 2, 25);
                if (md <= 9.0 && md > 7.6) return Mix(Rgb(14, 6, 8), Rgb(40, 10, 14), Fbm(x, z, 1043));
                if (md <= 7.6 && md > 7.2) return GoldLeaf(x * 2, z * 2, 26);
                if (md <= 7.2)
                {
                    if (Bat(mx / 8.5, (mz - 0.4) / 8.5)) return Rgb(10, 4, 6);
                    return MoonFace(mx, mz, 7.2);
                }
                return field;
            }, (u, v) =>
            {
                if (u < 32 || u >= 80 || v >= 32) return 0;
                double mx = u - 32 - 24, mz = v - 16, md = Math.Sqrt(mx * mx * 0.8 + mz * mz);
                return md <= 7.2 && !Bat(mx / 8.5, (mz - 0.4) / 8.5) ? 0.35 : 0;
            });
        }
    }

    // ── the item icons (32 x 32) ──
    static void IEll(double cx, double cy, double rx, double ry, Color c)
    {
        for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++) if (InEllipse(x + 0.5, y + 0.5, cx, cy, rx, ry)) IP(x, y, c);
    }
    static void ILine(double x0, double y0, double x1, double y1, Color c)
    {
        int n = (int)Math.Ceiling(Math.Max(Math.Abs(x1 - x0), Math.Abs(y1 - y0)) * 2) + 1;
        for (int i = 0; i <= n; i++) IP((int)Math.Floor(x0 + (x1 - x0) * i / n + 0.5), (int)Math.Floor(y0 + (y1 - y0) * i / n + 0.5), c);
    }

    static bool BloodIcon(string name)
    {
        if (!IsBlood(name)) return false;
        Color iron = Rgb(46, 42, 52), ironL = Rgb(92, 88, 100), ironD = Rgb(20, 18, 22);
        Color gold = Rgb(238, 188, 72), goldD = Rgb(158, 108, 30), goldL = Rgb(255, 234, 150);
        Color crim = Rgb(152, 14, 30), crimD = Rgb(88, 6, 16), crimL = Rgb(214, 42, 54);
        Color glowR = Rgb(255, 64, 54), glowL = Rgb(255, 196, 176), glowD = Rgb(170, 16, 20);
        Color bone = Rgb(232, 222, 196), boneD = Rgb(168, 154, 122);
        Color stone = Rgb(112, 108, 114), stoneD = Rgb(70, 66, 72), stoneL = Rgb(152, 148, 152);
        Color wood = Rgb(58, 34, 30), woodL = Rgb(94, 60, 48), woodD = Rgb(30, 18, 16), black = Rgb(14, 8, 10);
        switch (name)
        {
            case "grinning_jack_o_lantern":
            {
                Color pum = Rgb(232, 118, 26), pumL = Rgb(250, 160, 50), pumD = Rgb(170, 70, 12), lit = Rgb(255, 226, 90), litD = Rgb(255, 150, 30);
                IEll(15.5, 18.5, 13, 10.5, pumD); IEll(15.5, 18.0, 12, 10, pum); IEll(11, 15, 4, 5, pumL);
                for (int x = 9; x <= 22; x += 6) ILine(x, 9.5, x, 27.5, pumD);
                IRect(14, 4, 17, 8, Rgb(80, 86, 40)); IP(18, 4, Rgb(80, 86, 40)); IP(19, 3, Rgb(80, 86, 40)); IRect(19, 6, 22, 7, Rgb(60, 120, 40));
                for (int i = 0; i < 3; i++) { IRect(9 - i, 13 + i, 11 + i, 13 + i, lit); IRect(20 - i, 13 + i, 22 + i, 13 + i, lit); }
                IRect(15, 17, 16, 18, litD);
                IRect(8, 21, 23, 23, lit); IRect(8, 21, 23, 21, litD); IRect(11, 24, 12, 24, lit); IRect(19, 24, 20, 24, lit); IRect(14, 21, 15, 22, pumD); IRect(18, 23, 19, 23, pumD);
                return true;
            }
            case "haunted_tombstone":
            {
                Color st = Rgb(128, 130, 132), stL = Rgb(168, 170, 168), stD = Rgb(80, 82, 86), moss = Rgb(70, 110, 40), wisp = Rgb(190, 255, 240);
                IRect(4, 27, 27, 30, stD); IRect(5, 26, 26, 26, st);
                IRect(7, 12, 24, 25, st); IEll(15.5, 12, 8.5, 6, st); IRect(7, 12, 8, 25, stL); IRect(23, 12, 24, 25, stD);
                IRect(13, 9, 18, 12, Rgb(222, 214, 190)); IP(14, 10, stD); IP(17, 10, stD);
                IRect(10, 15, 21, 15, stD); IRect(11, 17, 20, 17, stD); IRect(12, 19, 19, 19, stD); IRect(9, 22, 22, 22, stD);
                IRect(7, 23, 10, 25, moss); IRect(21, 24, 24, 25, moss); ILine(19, 6.5, 21, 11, stD);
                IEll(15.5, 2.5, 2, 2, wisp); IP(15, 5, wisp); IP(16, 6, wisp);
                return true;
            }
            case "possessed_scarecrow":
            {
                Color post = Rgb(96, 88, 78), coat = Rgb(44, 38, 50), coatL = Rgb(70, 58, 78), straw = Rgb(232, 200, 110), sack = Rgb(170, 140, 92), eye = Rgb(140, 255, 80);
                IRect(15, 18, 16, 31, post); IRect(3, 12, 28, 13, post);
                IRect(11, 12, 20, 22, coat); IRect(11, 12, 12, 22, coatL); for (int x = 11; x <= 20; x += 2) IRect(x, 23, x, 24 + (x % 3), coat);
                IRect(5, 11, 10, 14, coat); IRect(21, 11, 26, 14, coat); IRect(2, 11, 4, 14, straw); IRect(27, 11, 29, 14, straw);
                IRect(12, 4, 19, 11, sack); IRect(13, 7, 14, 7, eye); IRect(17, 7, 18, 7, eye); IRect(13, 9, 18, 9, Rgb(40, 30, 24)); for (int x = 13; x <= 18; x += 2) IP(x, 10, Rgb(40, 30, 24));
                IRect(10, 3, 21, 3, coat); IRect(13, 0, 18, 2, coat); IP(19, 1, coat); IP(20, 2, coat);
                IRect(24, 9, 26, 11, Rgb(20, 18, 22)); IP(27, 10, Rgb(230, 180, 50)); IP(25, 9, eye);
                return true;
            }
            case "blood_moon_lantern":
            {
                // crescent on top, the stepped roof, the cage with the glowing moon, the foot
                IP(13, 0, gold); IP(18, 0, gold); IP(13, 1, gold); IP(18, 1, gold); IRect(14, 2, 17, 2, gold); IRect(15, 3, 16, 4, goldD);
                for (int y = 5; y <= 9; y++) { int half = (y - 5) + 2; IRect(16 - half, y, 15 + half, y, iron); IP(16 - half, y, ironL); IP(15 + half, y, ironD); }
                IRect(8, 10, 23, 10, gold); IP(8, 9, gold); IP(23, 9, gold);
                IRect(9, 11, 22, 23, ironD);
                for (int y = 12; y <= 22; y++) for (int x = 11; x <= 20; x++)
                {
                    double r = Math.Sqrt((x - 15.5) * (x - 15.5) + (y - 16.5) * (y - 16.5));
                    IP(x, y, r < 1.6 ? glowL : r < 3.6 ? Mix(glowR, glowD, (r - 1.6) / 2.0) : Mix(Rgb(120, 22, 20), Rgb(60, 10, 12), (r - 3.6) / 3.0));
                }
                IP(14, 15, Rgb(200, 40, 30)); IP(17, 18, Rgb(200, 40, 30));
                IRect(9, 11, 10, 23, iron); IRect(21, 11, 22, 23, iron); IRect(9, 11, 9, 23, ironL);
                IRect(13, 12, 13, 22, iron); IRect(18, 12, 18, 22, iron);
                ILine(11, 15, 15.5, 11.5, gold); ILine(20, 15, 15.5, 11.5, gold);
                IRect(9, 20, 22, 20, goldD);
                IRect(8, 24, 23, 24, gold); IRect(8, 25, 23, 26, iron); IRect(7, 27, 24, 28, ironD); IRect(7, 27, 24, 27, iron);
                IP(7, 29, goldD); IP(24, 29, goldD); IP(8, 29, gold); IP(23, 29, gold);
                return true;
            }
            case "crimson_candelabra":
            {
                // five candles on S-curled arms, flames, the stem and three feet
                int[][] candles = { new[] { 15, 3, 10 }, new[] { 10, 6, 12 }, new[] { 21, 6, 12 }, new[] { 5, 9, 14 }, new[] { 26, 9, 14 } };
                IRect(15, 11, 16, 26, iron); IRect(15, 11, 15, 26, ironL);
                IRect(14, 16, 17, 17, gold); IRect(14, 21, 17, 22, gold); IRect(13, 12, 18, 13, iron);
                ILine(15, 14, 10, 15, iron); ILine(10, 15, 10.5, 13, iron); ILine(16, 14, 21, 15, iron); ILine(21, 15, 21.5, 13, iron);
                ILine(15, 15, 7, 17, iron); ILine(7, 17, 5.5, 15, iron); ILine(16, 15, 24, 17, iron); ILine(24, 17, 26.5, 15, iron);
                IP(10, 15, gold); IP(21, 15, gold); IP(7, 17, gold); IP(24, 17, gold);
                foreach (int[] cd in candles)
                {
                    int x = cd[0], top = cd[1], bottom = cd[2];
                    IRect(x - 1, bottom + 1, x + 2, bottom + 1, gold);
                    IRect(x, top + 2, x + 1, bottom, crim); IP(x, top + 2, crimL); IP(x + 1, top + 4 + (x % 3), crimL); IP(x - 1, top + 4, crimD);
                    IP(x, top + 1, black);
                    IP(x, top, Rgb(255, 200, 80)); IP(x + 1, top, Rgb(255, 150, 40)); IP(x, top - 1, Rgb(255, 240, 180)); IP(x, top - 2, Rgb(255, 170, 60));
                    IP(x + 1, bottom + 2, crimL);
                }
                IRect(12, 26, 19, 27, iron); ILine(12, 27, 8, 30, iron); ILine(19, 27, 23, 30, iron); IRect(15, 27, 16, 30, iron);
                IP(8, 30, gold); IP(23, 30, gold); IP(15, 31, gold); IP(16, 31, gold);
                return true;
            }
            case "crimson_throne":
            {
                // carved bat wings spreading from the top of the back
                for (int s = -1; s <= 1; s += 2)
                {
                    for (int i = 0; i < 9; i++)
                    {
                        int x = s < 0 ? 9 - i : 22 + i;
                        int top = 6 - i / 2 - (i > 5 ? 1 : 0), bot = 9 - (i % 3 == 2 ? 1 : 0) + (i < 3 ? 1 : 0) - i / 4;
                        IRect(x, Math.Min(top, bot), x, Math.Max(top, bot), black);
                        IP(x, top, gold);
                    }
                }
                // the back: frame with a pointed arch, tufted velvet in it, gold tracing, a ruby at the peak
                for (int y = 2; y <= 19; y++)
                {
                    int half = y < 8 ? (y - 2) + 1 : 7;
                    IRect(16 - half - 1, y, 15 + half + 1, y, wood);
                    if (y >= 4) { int ih = y < 8 ? (y - 4) + 1 : 5; IRect(16 - ih, y, 15 + ih, y, ((y + 1) % 3 == 0) ? crimD : crim); }
                }
                for (int y = 9; y <= 18; y += 3) for (int x = 12; x <= 19; x += 3) IP(x, y, gold);
                IP(15, 1, glowR); IP(16, 1, glowR); IP(15, 0, gold); IP(16, 0, gold);
                IRect(8, 3, 8, 19, wood); IRect(23, 3, 23, 19, wood); IP(8, 2, gold); IP(23, 2, gold);
                // arms, the cushion, the gold-trimmed apron, legs with gold feet
                IRect(5, 15, 10, 16, wood); IRect(21, 15, 26, 16, wood); IP(5, 15, gold); IP(26, 15, gold); IRect(5, 17, 5, 22, wood); IRect(26, 17, 26, 22, wood);
                IRect(7, 19, 24, 21, crim); IRect(7, 19, 24, 19, crimL);
                IRect(7, 22, 24, 23, wood); IRect(7, 22, 24, 22, gold); IP(15, 23, glowR); IP(16, 23, glowR);
                IRect(8, 24, 9, 28, woodD); IRect(22, 24, 23, 28, woodD); IRect(7, 29, 10, 29, gold); IRect(21, 29, 24, 29, gold);
                return true;
            }
            case "vampire_coffin":
            {
                // the lid from above, head up: six-sided, ebony, a raised panel, the gold cross, the ruby
                for (int y = 1; y <= 30; y++)
                {
                    double half = y <= 8 ? 5 + (y - 1) * 3.4 / 7 : 8.4 - (y - 8) * 4.4 / 22;
                    int x0 = (int)Math.Round(15.5 - half), x1 = (int)Math.Round(15.5 + half);
                    IRect(x0, y, x1, y, wood); IP(x0, y, woodL); IP(x1, y, woodD);
                    if (y == 1 || y == 30) IRect(x0, y, x1, y, woodD);
                    if (y >= 3 && y <= 28) { int ih = (int)Math.Round(half - 2); IRect(16 - ih, y, 15 + ih, y, Mix(wood, woodL, 0.35)); }
                }
                IRect(15, 4, 16, 26, gold); IRect(15, 4, 15, 26, goldL); IRect(10, 10, 21, 11, gold); IRect(10, 10, 21, 10, goldL);
                IRect(14, 3, 17, 3, goldD); IRect(14, 27, 17, 27, goldD); IRect(9, 9, 9, 12, goldD); IRect(22, 9, 22, 12, goldD);
                IRect(14, 9, 17, 12, gold); IRect(15, 10, 16, 11, crimL); IP(15, 10, glowL);
                IP(7, 8, gold); IP(24, 8, gold); IP(11, 1, gold); IP(20, 1, gold); IP(12, 30, gold); IP(19, 30, gold);
                return true;
            }
            case "blood_fountain":
            {
                // the basin with its glowing blood, the pillar, the bowl, streams, the jet
                for (int y = 20; y <= 29; y++)
                {
                    double half = 13.5 - Math.Max(0, y - 26) * 0.8;
                    IRect((int)Math.Round(15.5 - half), y, (int)Math.Round(15.5 + half), y, (y % 3 == 0) ? stoneD : stone);
                }
                IRect(2, 19, 29, 20, stoneL);
                IEll(15.5, 20.5, 12, 1.6, glowD); IEll(15.5, 20.3, 10, 1.1, glowR); IP(10, 20, glowL); IP(20, 21, glowL);
                IRect(14, 10, 17, 19, stone); IRect(14, 10, 14, 19, stoneL); IRect(14, 14, 17, 14, gold);
                IRect(8, 9, 23, 10, stoneL); IRect(9, 11, 22, 11, stone); IEll(15.5, 9.3, 7, 0.9, glowR);
                IRect(8, 11, 8, 19, glowR); IRect(23, 11, 23, 19, glowR); IP(8, 14, glowL); IP(23, 17, glowL);
                IRect(15, 5, 16, 8, stone); IRect(13, 4, 18, 5, stoneL); IRect(15, 1, 16, 3, glowR); IP(15, 0, glowL);
                IP(13, 6, glowR); IP(18, 7, glowR);
                IP(3, 17, stoneL); IP(28, 17, stoneL); IP(3, 16, gold); IP(28, 16, gold);
                return true;
            }
            case "blood_moon_banner":
            {
                IRect(6, 2, 7, 29, iron); IRect(6, 2, 6, 29, ironL);
                IP(6, 0, gold); IP(7, 0, gold); IP(5, 1, gold); IP(8, 1, gold); IP(6, 1, glowR); IP(7, 1, glowR);
                IRect(6, 3, 26, 3, iron); IP(27, 3, gold); IP(28, 3, gold);
                IRect(3, 29, 10, 30, iron); IP(3, 30, gold); IP(10, 30, gold);
                for (int y = 4; y <= 27; y++)
                {
                    for (int x = 9; x <= 25; x++)
                    {
                        double c = x - 9, s = y - 4;
                        if (s > 17 + 6 * Math.Abs(c - 8) / 8) continue;
                        Color col = (x == 9 || x == 25 || y == 4) ? gold : ((x + y) % 5 == 0 ? crimD : crim);
                        IP(x, y, col);
                    }
                }
                IEll(17.5, 11.5, 4.4, 4.4, glowR); IEll(17.0, 11.0, 2.4, 2.4, glowL); IP(19, 13, glowD); IP(16, 9, Rgb(220, 50, 40));
                IP(15, 11, black); IP(16, 11, black); IP(14, 10, black); IP(17, 10, black); IP(13, 9, black); IP(18, 9, black); IP(15, 12, black);
                IRect(16, 16, 16, 18, crimD); IRect(19, 16, 19, 17, crimD);
                return true;
            }
            case "crimson_rug":
            {
                // laid flat, seen at a slant: a crimson field, gold border, the moon medallion, tassels at its ends
                for (int y = 9; y <= 24; y++)
                {
                    double t = (y - 9) / 15.0;
                    int x0 = (int)Math.Round(7 - 5 * t), x1 = (int)Math.Round(24 + 5 * t);
                    for (int x = x0; x <= x1; x++)
                    {
                        bool border = y == 9 || y == 24 || x == x0 || x == x1;
                        bool goldLine = y == 10 || y == 23 || x == x0 + 1 || x == x1 - 1;
                        IP(x, y, border ? crimD : goldLine ? gold : ((x * 3 + y * 5) % 11 == 0 ? crimD : crim));
                    }
                    IP(x0 - 1, y, (y % 2 == 0) ? gold : goldD); IP(x1 + 1, y, (y % 2 == 0) ? gold : goldD);
                }
                IEll(15.5, 16.5, 5.5, 3.6, black); IEll(15.5, 16.5, 4.4, 2.7, glowR); IEll(15.0, 16.0, 2.0, 1.2, glowL);
                IP(10, 16, gold); IP(21, 16, gold); IP(15, 13, gold); IP(16, 20, gold);
                return true;
            }
            case "revenant_greatsword_statue":
            {
                IRect(6, 23, 25, 30, stone); IRect(5, 22, 26, 23, stoneL); IRect(6, 30, 25, 30, stoneD);
                ILine(11, 23, 9, 27, glowR); ILine(9, 27, 10, 30, glowR); ILine(20, 23, 23, 26, glowR); ILine(23, 26, 22, 29, glowR);
                IP(4, 30, stoneD); IP(27, 30, stone); IP(28, 29, stoneD);
                IRect(14, 7, 17, 22, Rgb(52, 52, 60)); IRect(14, 7, 14, 22, Rgb(150, 150, 162)); IRect(17, 7, 17, 22, Rgb(90, 90, 100));
                IRect(15, 8, 16, 21, glowR); IRect(15, 9, 15, 20, glowL);
                IRect(9, 5, 22, 6, gold); IRect(9, 5, 22, 5, goldL); IP(8, 4, gold); IP(23, 4, gold); IP(8, 3, goldD); IP(23, 3, goldD);
                IRect(14, 4, 17, 7, gold); IP(15, 5, glowR); IP(16, 5, glowR);
                IRect(15, 1, 16, 3, Rgb(90, 40, 30)); IP(15, 2, Rgb(140, 70, 50));
                IRect(14, 0, 17, 0, gold);
                return true;
            }
            case "gorefang_skull_trophy":
            {
                // the shield plaque, edged in gold
                for (int y = 2; y <= 29; y++)
                {
                    int half = y < 22 ? 12 : 12 - (y - 21) * 3 / 2;
                    if (half < 1) half = 1;
                    IRect(16 - half, y, 15 + half, y, (y % 4 == 0) ? woodD : wood);
                    IP(16 - half, y, gold); IP(15 + half, y, gold);
                    if (y == 2) IRect(16 - half, y, 15 + half, y, gold);
                }
                // horns curling round each side
                for (int s = -1; s <= 1; s += 2)
                {
                    for (int i = 0; i < 18; i++)
                    {
                        double th = i / 17.0 * 1.8 * Math.PI, r = 5.2 * (1 - 0.5 * i / 17.0);
                        double x = 15.5 + s * (4.5 + r * Math.Sin(th)), y = 11 - r * Math.Cos(th);
                        Color hc = Mix(bone, boneD, i / 17.0);
                        IP((int)Math.Round(x), (int)Math.Round(y), hc); IP((int)Math.Round(x + s * 0.6), (int)Math.Round(y + 0.5), hc);
                    }
                }
                // the skull: cranium, muzzle, eye holes burning, fangs
                IRect(11, 6, 20, 14, bone); IRect(11, 6, 20, 6, boneD); IRect(12, 15, 19, 21, bone); IRect(13, 22, 18, 23, bone);
                IRect(11, 14, 11, 15, boneD); IRect(20, 14, 20, 15, boneD);
                IRect(12, 10, 14, 12, black); IRect(17, 10, 19, 12, black); IP(13, 11, glowR); IP(18, 11, glowR); IP(13, 10, glowL); IP(18, 10, glowL);
                IRect(15, 21, 16, 22, black);
                IRect(13, 24, 13, 26, bone); IRect(18, 24, 18, 26, bone); IRect(14, 24, 17, 24, boneD);
                return true;
            }
            case "sanguine_staff_pedestal":
            {
                IRect(9, 27, 22, 30, stone); IRect(9, 27, 22, 27, stoneL); IRect(11, 20, 20, 26, stone); IRect(11, 20, 11, 26, stoneL); IRect(20, 20, 20, 26, stoneD);
                IRect(11, 23, 20, 23, gold); IP(15, 23, glowR); IP(16, 23, glowR);
                IRect(10, 19, 21, 19, stoneL); IRect(14, 18, 17, 18, gold);
                IRect(15, 9, 16, 17, bone); IRect(16, 9, 16, 17, boneD); IP(15, 11, boneD); IP(15, 14, boneD);
                ILine(17, 12, 18, 15, crim); ILine(14, 13, 13, 17, crim);
                IRect(14, 8, 17, 8, bone); IP(12, 6, bone); IP(13, 7, bone); IP(19, 6, bone); IP(18, 7, bone); IP(12, 4, bone); IP(19, 4, bone); IP(13, 2, bone); IP(18, 2, bone);
                for (int y = 0; y <= 7; y++) { int half = y < 3 ? y : y < 6 ? 2 : 7 - y; IRect(15 - half, y, 16 + half, y, Mix(glowL, glowR, Math.Abs(y - 3) / 4.0)); }
                IP(15, 2, Rgb(255, 255, 255)); IP(10, 3, glowR); IP(21, 5, glowR); IP(20, 1, glowL);
                return true;
            }
            case "bottle_of_blood":
            {
                // a round-bellied glass bottle full of dark glowing blood, a cork, a glint on the glass
                Color glassE = Rgb(170, 200, 210), glassD = Rgb(110, 140, 156), cork = Rgb(150, 104, 62), corkD = Rgb(104, 70, 40);
                IRect(14, 3, 17, 6, cork); IRect(14, 3, 14, 6, corkD); IRect(13, 7, 18, 8, glassE); IRect(14, 9, 17, 12, glassD);
                IEll(15.5, 21.0, 8.4, 8.0, glassD);
                IEll(15.5, 21.5, 7.4, 6.9, crimD);
                for (int y = 15; y < 29; y++) for (int x = 8; x < 24; x++)
                {
                    if (!InEllipse(x + 0.5, y + 0.5, 15.5, 21.5, 7.4, 6.9)) continue;
                    double t = (y - 15) / 14.0;
                    IP(x, y, y < 17 ? crimL : Mix(crim, crimD, t * 0.9));
                }
                IRect(10, 17, 21, 17, glowR); IP(12, 22, glowR); IP(19, 25, glowR); IP(16, 20, crimL);
                IRect(10, 19, 10, 23, glassE); IP(11, 18, Rgb(240, 250, 255)); IP(10, 24, glassE); IP(15, 10, Rgb(230, 245, 250));
                return true;
            }
            case "grandfather_clock":
            {
                IRect(10, 27, 21, 30, wood); IRect(10, 27, 21, 27, woodL); IP(10, 31, gold); IP(21, 31, gold);
                IRect(11, 12, 20, 26, wood); IRect(11, 12, 11, 26, woodL);
                IRect(13, 14, 18, 25, Rgb(70, 10, 18)); IRect(13, 14, 13, 25, gold); IRect(18, 14, 18, 25, gold); IRect(13, 14, 18, 14, gold);
                ILine(15.5, 15, 15.5, 22, gold); IEll(15.5, 22.5, 1.3, 1.3, gold); IP(15, 22, glowR);
                IRect(9, 2, 22, 11, wood); IRect(9, 11, 22, 11, gold);
                IEll(15.5, 6.5, 4.0, 4.0, gold); IEll(15.5, 6.5, 3.3, 3.3, black); IEll(15.5, 6.5, 2.4, 2.4, glowR); IEll(15.0, 6.0, 1.0, 1.0, glowL);
                ILine(15.5, 6.5, 15.5, 4.0, gold); ILine(15.5, 6.5, 17.3, 6.5, gold);
                for (int x = 9; x <= 22; x++) { int h = 2 - Math.Abs(x - 15) / 3; if (h > 0) IRect(x, 2 - h, x, 1, wood); }
                IP(15, 0, gold); IP(16, 0, gold); IP(15, 1, glowR); IP(16, 1, glowR);
                IRect(8, 3, 8, 11, gold); IRect(23, 3, 23, 11, gold);
                return true;
            }
            case "crimson_chandelier":
            {
                IRect(15, 0, 16, 1, gold); for (int y = 2; y <= 7; y++) IP(15 + (y % 2), y, iron);
                IRect(14, 8, 17, 9, gold); IRect(15, 10, 16, 15, iron); IRect(14, 12, 17, 12, gold);
                IEll(15.5, 17.5, 13, 2.6, iron); IEll(15.5, 17.5, 11.4, 1.6, Rgb(0, 0, 0));
                for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++) if (icon[x, y].A > 0 && icon[x, y].R == 0 && icon[x, y].G == 0 && icon[x, y].B == 0) icon[x, y] = Clear;
                IRect(4, 17, 27, 17, iron); IRect(4, 18, 27, 18, goldD);
                int[] cx = { 4, 9, 15, 21, 27 };
                foreach (int x in cx)
                {
                    int top = x == 15 ? 11 : 13;
                    IRect(x - 1, top + 3, x + 1, top + 3, gold); IRect(x, top, x, top + 2, crim); IP(x, top - 1, Rgb(255, 200, 80)); IP(x, top - 2, Rgb(255, 240, 180));
                }
                for (int i = 0; i < 6; i++) { int x = 6 + i * 4; IRect(x, 19, x, 21 + (i % 2), Mix(glowR, glowD, 0.3)); IP(x, 22 + (i % 2), glowL); }
                IRect(15, 16, 16, 24, glowR); IP(15, 25, glowL);
                return true;
            }
            case "blood_pipe_organ":
            {
                for (int i = 0; i < 9; i++)
                {
                    int x = 5 + i * 3; double f = Math.Abs(i - 4) / 4.0;
                    int top = 4 + (int)Math.Round(8 * Math.Sin(f * Math.PI / 2)) - (f > 0.8 ? 4 : 0);
                    IRect(x, top, x + 1, 19, Rgb(150, 150, 162)); IRect(x, top, x, 19, Rgb(200, 200, 212)); IP(x, top - 1, Rgb(110, 110, 120));
                    IRect(x, 16, x + 1, 16, gold); IP(x + 1, 14, black);
                }
                IEll(15.5, 4, 2.6, 2.6, glowR); IEll(15.2, 3.6, 1, 1, glowL);
                for (int x = 1; x <= 8; x++) IP(x + 3, 3 - x / 3, black); for (int x = 1; x <= 8; x++) IP(27 - x, 3 - x / 3, black);
                IRect(3, 20, 28, 29, wood); IRect(3, 20, 28, 20, woodL); IRect(5, 22, 26, 23, bone); for (int x = 6; x < 26; x += 2) IP(x, 22, black);
                IRect(3, 29, 28, 29, gold); IRect(4, 17, 5, 19, crim); IP(4, 16, Rgb(255, 200, 80)); IRect(26, 17, 27, 19, crim); IP(27, 16, Rgb(255, 200, 80));
                return true;
            }
            case "secret_bookshelf_door":
            {
                IRect(7, 1, 24, 30, wood); IRect(7, 1, 7, 30, woodL); IRect(24, 1, 24, 30, woodD); IRect(7, 1, 24, 2, woodL);
                Color[] covers = { crim, Rgb(30, 70, 44), Rgb(96, 60, 36), Rgb(70, 30, 74), Rgb(30, 26, 30) };
                int[] shelfY = { 9, 17, 25 };
                for (int sh = 0; sh < 3; sh++)
                {
                    IRect(8, shelfY[sh] + 1, 23, shelfY[sh] + 1, woodD);
                    for (int x = 9; x <= 22; x++)
                    {
                        int h = 4 + ((x * 7 + sh) % 3);
                        IRect(x, shelfY[sh] - h + 1, x, shelfY[sh], covers[(x + sh * 2) % covers.Length]);
                        if (x % 2 == 0) IP(x, shelfY[sh] - 2, gold);
                    }
                }
                IRect(14, 10, 17, 12, crimL); IRect(15, 11, 18, 11, crim); IP(18, 10, gold);
                IRect(8, 26, 23, 29, wood); IP(15, 3, glowR); IP(16, 3, glowR);
                return true;
            }
            case "haunted_portrait":
            {
                IRect(8, 1, 23, 30, gold); IRect(8, 1, 23, 1, goldL); IRect(8, 30, 23, 30, goldD); IRect(9, 2, 22, 29, goldD);
                IRect(10, 3, 21, 28, Rgb(70, 14, 18));
                IEll(15.5, 11, 5, 6.5, Rgb(20, 8, 12)); IRect(10, 16, 21, 28, Rgb(20, 8, 12));
                IEll(15.5, 11.5, 2.6, 3.4, Rgb(214, 206, 214));
                IRect(13, 10, 14, 10, glowR); IRect(17, 10, 18, 10, glowR);
                IRect(14, 14, 17, 14, crim); IP(14, 15, Rgb(250, 250, 250)); IP(17, 15, Rgb(250, 250, 250));
                for (int y = 17; y < 21; y++) IRect(16 - (21 - y), y, 15 + (21 - y), y, crim);
                IP(15, 19, gold); IP(16, 19, gold);
                IP(8, 1, glowR); IP(23, 1, glowR); IP(8, 30, glowR); IP(23, 30, glowR);
                return true;
            }
            case "iron_maiden":
            {
                IRect(8, 28, 23, 30, stone); IRect(8, 28, 23, 28, stoneL);
                for (int y = 3; y < 28; y++)
                {
                    double t = (y - 3) / 25.0;
                    int half = y < 7 ? 3 + (y - 3) / 2 : (int)Math.Round(6 - 1.5 * Math.Sin(t * Math.PI * 1.2));
                    IRect(16 - half, y, 15 + half, y, (y % 6 == 0) ? ironL : iron);
                    IP(16 - half, y, ironL);
                }
                IRect(13, 5, 18, 10, Rgb(130, 126, 134)); IRect(14, 7, 14, 7, black); IRect(17, 7, 17, 7, black); IRect(15, 9, 16, 9, black); IP(17, 8, glowR);
                for (int x = 12; x <= 19; x += 2) IP(x, 2, gold); IRect(12, 3, 19, 3, gold);
                for (int y = 12; y < 26; y += 4) { IRect(11, y, 20, y, Rgb(100, 96, 104)); IP(13, y, gold); IP(18, y, gold); }
                return true;
            }
            case "blood_moon_altar":
            {
                IRect(3, 27, 28, 30, stone); IRect(3, 27, 28, 27, stoneL); IRect(5, 18, 26, 26, stone); IRect(5, 18, 5, 26, stoneL); IRect(26, 18, 26, 26, stoneD);
                IRect(4, 17, 27, 17, stoneL); IRect(9, 17, 22, 22, crim); for (int x = 9; x <= 22; x += 2) IP(x, 23, gold);
                IRect(13, 15, 18, 16, gold); IRect(14, 15, 17, 15, glowR);
                IRect(5, 11, 6, 16, crim); IP(5, 10, Rgb(255, 200, 80)); IRect(25, 11, 26, 16, crim); IP(26, 10, Rgb(255, 200, 80));
                for (int y = 1; y <= 10; y++) { int half = y < 5 ? (y + 1) / 2 : (11 - y) / 2; IRect(15 - half, y, 16 + half, y, Mix(glowL, glowR, Math.Abs(y - 4) / 5.0)); }
                IP(11, 4, glowR); IP(20, 7, glowR); IP(15, 3, Rgb(255, 255, 255));
                return true;
            }
            case "bat_roost":
            {
                IEll(15.5, 29.5, 6, 1.8, stone); IRect(15, 22, 16, 28, iron);
                Color bark = Rgb(84, 72, 64);
                ILine(15.5, 22, 14, 14, bark); ILine(14, 14, 16, 7, bark); ILine(16, 7, 15, 2, bark); ILine(15, 15, 16, 15, bark);
                ILine(15, 12, 23, 9, bark); ILine(23, 9, 27, 10, bark); ILine(15, 16, 7, 13, bark); ILine(7, 13, 4, 14, bark); ILine(16, 7, 21, 4, bark);
                int[][] bats = { new[] { 22, 10 }, new[] { 8, 14 }, new[] { 25, 11 } };
                foreach (int[] b in bats) { IRect(b[0], b[1], b[0] + 1, b[1] + 3, black); IP(b[0], b[1] + 4, black); IP(b[0] + 1, b[1] + 4, black); }
                // two flying
                IP(5, 4, black); IP(6, 5, black); IP(7, 4, black); IP(4, 3, black); IP(8, 3, black); IP(6, 4, glowR);
                IP(25, 18, black); IP(26, 19, black); IP(27, 18, black); IP(24, 17, black); IP(28, 17, black);
                return true;
            }
            case "crimson_canopy_bed":
            {
                IRect(3, 3, 4, 29, wood); IRect(27, 3, 28, 29, wood); IP(3, 2, gold); IP(4, 2, gold); IP(27, 2, gold); IP(28, 2, gold);
                IRect(3, 4, 28, 7, crim); IRect(3, 4, 28, 4, crimL); for (int x = 4; x <= 27; x += 3) IP(x, 8, crim); IRect(3, 7, 28, 7, gold);
                IRect(5, 8, 7, 26, crimD); IRect(24, 8, 26, 26, crimD); IRect(5, 16, 7, 16, gold); IRect(24, 16, 26, 16, gold);
                IRect(8, 12, 23, 20, wood); IRect(9, 13, 22, 19, crim); for (int y = 14; y < 19; y += 2) for (int x = 10; x < 22; x += 3) IP(x, y, gold);
                IRect(6, 20, 25, 23, Rgb(236, 232, 224)); IRect(8, 21, 12, 22, Rgb(250, 248, 242)); IRect(19, 21, 23, 22, Rgb(250, 248, 242));
                IRect(5, 23, 26, 26, crim); IRect(5, 23, 26, 23, gold); IRect(5, 27, 26, 28, wood);
                return true;
            }
            case "blood_crystal_fireplace":
            {
                IRect(1, 28, 30, 30, stone); IRect(1, 28, 30, 28, stoneL);
                IRect(2, 9, 29, 27, stone); IRect(2, 9, 2, 27, stoneL); IRect(29, 9, 29, 27, stoneD);
                IRect(0, 7, 31, 8, Rgb(196, 190, 196)); IRect(0, 8, 31, 8, gold);
                IRect(7, 1, 24, 6, stone); IRect(13, 2, 18, 4, stoneL); IP(14, 3, glowR); IP(17, 3, glowR); ILine(13, 3, 9, 1, stoneL); ILine(18, 3, 22, 1, stoneL);
                IRect(8, 13, 23, 27, black);
                for (int y = 11; y < 13; y++) IRect(10 + (12 - y) * 0, y, 21, y, black);
                IRect(9, 26, 22, 27, iron);
                for (int x = 9; x <= 22; x++) { int h = 5 + (int)(4 * Math.Abs(Math.Sin(x * 1.3))); IRect(x, 26 - h, x, 25, x % 3 == 0 ? glowL : glowR); }
                IRect(13, 21, 14, 25, Rgb(255, 120, 130)); IRect(17, 22, 18, 25, Rgb(255, 120, 130)); IP(13, 20, glowL); IP(18, 21, glowL);
                IRect(3, 4, 3, 6, crim); IP(3, 3, Rgb(255, 200, 80)); IRect(28, 4, 28, 6, crim); IP(28, 3, Rgb(255, 200, 80));
                return true;
            }
            case "bat_gargoyle":
            {
                // wings half spread behind it
                for (int s = -1; s <= 1; s += 2)
                {
                    for (int i = 0; i < 11; i++)
                    {
                        int x = s < 0 ? 11 - i : 20 + i;
                        int top = 9 - (i * 6) / 10, bot = 15 - (i % 3 == 2 ? 2 : 0) - i / 3;
                        IRect(x, top, x, bot, (i % 3 == 0) ? stoneL : stoneD);
                        IP(x, top, stoneL);
                    }
                }
                IRect(6, 24, 25, 30, stone); IRect(5, 23, 26, 23, stoneL); IRect(6, 30, 25, 30, stoneD); IP(8, 26, Rgb(90, 120, 60)); IP(22, 28, Rgb(90, 120, 60));
                IRect(11, 13, 20, 22, stone); IRect(11, 13, 11, 22, stoneL);
                IRect(9, 18, 11, 22, stoneD); IRect(20, 18, 22, 22, stoneD);
                IRect(12, 21, 14, 22, stoneL); IRect(17, 21, 19, 22, stoneL);
                IRect(12, 6, 19, 12, stone); IRect(12, 6, 19, 6, stoneL);
                IRect(12, 2, 13, 5, stone); IP(12, 1, stone); IRect(18, 2, 19, 5, stone); IP(19, 1, stone);
                IP(13, 8, glowR); IP(14, 8, glowR); IP(17, 8, glowR); IP(18, 8, glowR);
                IRect(14, 10, 17, 11, black); IP(14, 11, bone); IP(17, 11, bone); IP(15, 10, stoneD);
                return true;
            }
        }
        return false;
    }
}
