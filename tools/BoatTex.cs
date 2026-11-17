// Paints the Premium boats built with client/BoatKit (Speedboat, Yacht, Longship, Paddlewheeler, Swamp Raft...): for each, textures/entity/<boat>.png (1024 x 1024: 4x the 256 x 256 layout), <boat>_glow.png (only what
// glows) and textures/item/<boat>.png (32 x 32). The ZONES here must match BoatKit's. Used by tools/make_boats.ps1.
using System;
using System.Drawing;
using System.Drawing.Imaging;

public static partial class BoatTex
{
    const int S = 4;
    const int N = 256 * S;
    static Color[,] img;
    static bool[,] glow;
    // glowing colours that differ from the texture (a lamp's beam: clear in the texture, soft light in the glow)
    static Color[,] glowColor;

    // â”€â”€ noise and colour helpers â”€â”€
    static double Hash(int x, int y, int seed)
    {
        unchecked
        {
            int h = x * 374761393 + y * 668265263 + seed * 1442695041;
            h = (h ^ (h >> 13)) * 1274126177;
            h ^= h >> 16;
            return (h & 0x7fffffff) / (double)0x7fffffff;
        }
    }
    static double Smooth(double x, double y, int seed)
    {
        int xi = (int)Math.Floor(x), yi = (int)Math.Floor(y);
        double fx = x - xi, fy = y - yi;
        fx = fx * fx * (3 - 2 * fx); fy = fy * fy * (3 - 2 * fy);
        double a = Hash(xi, yi, seed), b = Hash(xi + 1, yi, seed), c = Hash(xi, yi + 1, seed), d = Hash(xi + 1, yi + 1, seed);
        return (a + (b - a) * fx) + ((c + (d - c) * fx) - (a + (b - a) * fx)) * fy;
    }
    static double Fbm(double x, double y, int seed)
    {
        return 0.55 * Smooth(x, y, seed) + 0.3 * Smooth(x * 2.1, y * 2.1, seed + 7) + 0.15 * Smooth(x * 4.3, y * 4.3, seed + 13);
    }
    static int C(double v) { return v < 0 ? 0 : v > 255 ? 255 : (int)v; }
    static Color Rgb(double r, double g, double b) { return Color.FromArgb(255, C(r), C(g), C(b)); }
    static Color Argb(double a, double r, double g, double b) { return Color.FromArgb(C(a), C(r), C(g), C(b)); }
    static Color Mul(Color c, double k) { return Color.FromArgb(c.A, C(c.R * k), C(c.G * k), C(c.B * k)); }
    static Color Mix(Color a, Color b, double t) { return Color.FromArgb(C(a.A + (b.A - a.A) * t), C(a.R + (b.R - a.R) * t), C(a.G + (b.G - a.G) * t), C(a.B + (b.B - a.B) * t)); }
    static readonly Color Clear = Color.FromArgb(0, 0, 0, 0);
    static bool InEllipse(double x, double y, double cx, double cy, double rx, double ry) { double a = (x - cx) / rx, b = (y - cy) / ry; return a * a + b * b <= 1; }

    delegate Color Painter(double u, double v);
    static void Zone(int zu, int zv, int zw, int zh, Painter f, bool glows)
    {
        for (int y = zv * S; y < (zv + zh) * S; y++)
            for (int x = zu * S; x < (zu + zw) * S; x++)
            {
                img[x, y] = f((x - zu * S + 0.5) / S, (y - zv * S + 0.5) / S);
                glow[x, y] = glows;
            }
    }

    // horizontal boards `size` tall: tint per board, butt joints, seams, grain
    static Color Planks(Color baseC, double u, double v, double size, int seed, double seam)
    {
        int board = (int)Math.Floor(v / size);
        double inBoard = v / size - board;
        double tint = 0.88 + 0.24 * Hash(board, 0, seed);
        int len = 10 + (int)(Hash(board, 1, seed) * 7);
        double off = Hash(board, 2, seed) * len;
        double along = (u + off) % len;
        int piece = (int)Math.Floor((u + off) / len);
        tint *= 0.93 + 0.14 * Hash(board, piece + 3, seed);
        double grain = Fbm(u * 0.35, v * 7.0, seed + board) - 0.5;
        Color c = Mul(baseC, tint + grain * 0.22);
        if (inBoard < 0.5 / S * size * 1.0 / size * size || inBoard > 1 - 0.3 / size) c = Mul(c, seam);
        if (along < 0.25) c = Mul(c, seam + 0.15);
        return c;
    }
    static Color Glossy(Color baseC, double u, double v, int seed)
    {
        double sheen = 0.95 + 0.04 * Math.Sin(v * 0.9 + u * 0.15) + 0.03 * Fbm(u * 0.2, v * 0.2, seed);
        return Mul(baseC, sheen);
    }
    static Color Metal(Color baseC, double u, double v, int seed, double streak)
    {
        double s = Fbm(u * 0.3, v * 5, seed);
        return Mul(baseC, 0.85 + 0.25 * s + streak * Math.Max(0, Math.Sin(v * 3.1 + u * 0.7)));
    }

    // â”€â”€ the boats' styles â”€â”€
    class Style
    {
        public string Name;
        public Color Fiber, Bottom, Leather, Lining, Accent, Navy, Wood, DarkWood, Canvas, Steel;
        public int Deck;           // 0 teak, 1 spruce, 2 varnished dark, 3 weathered grey, 4 steel plate
        public bool TintGlows;     // windows that glow warm at night
        public Func<double, double, Color> Side;   // the hull-side picture at (x, y); glowing pixels marked by SideGlow
        public Func<double, double, bool> SideGlow;
    }

    static void Common(Style st)
    {
        for (int y = 0; y < N; y++) for (int x = 0; x < N; x++) { img[x, y] = Rgb(70, 50, 34); glow[x, y] = false; }
        // the hull sides: port column c is x = 56 - c, starboard x = c - 48; row r is y = r - 18
        Zone(0, 0, 128, 20, (u, v) => st.Side(56 - u, v - 18), false);
        Zone(128, 0, 128, 20, (u, v) => st.Side(u - 48, v - 18), false);
        for (int y = 0; y < 20 * S; y++) for (int x = 0; x < 256 * S; x++)
        {
            double u = (x % (128 * S) + 0.5) / S, v = (y + 0.5) / S;
            double hx = x < 128 * S ? 56 - u : u - 48;
            glow[x, y] = st.SideGlow != null && st.SideGlow(hx, v - 18);
        }
        Zone(0, 20, 64, 32, (u, v) => Glossy(st.Fiber, u, v, 1), false);
        Zone(64, 20, 64, 32, (u, v) => Mul(st.Bottom, 0.85 + 0.25 * Fbm(u * 0.5, v * 0.5, 2)), false);
        Zone(128, 20, 40, 24, (u, v) =>
        {
            double fu = u % 4, fv = v % 4;
            Color c = Mul(st.Leather, 0.9 + 0.1 * Fbm(u, v, 3));
            if (fu < 0.18 || fv < 0.18) c = Mul(st.Leather, 0.75);                       // stitched panels
            if (v < 0.4 || v > 23.6) c = Rgb(170, 30, 30);                              // piping
            return c;
        }, false);
        Zone(168, 20, 24, 24, (u, v) => Mix(Rgb(120, 126, 136), Rgb(245, 248, 255), 0.5 + 0.5 * Math.Sin(v * 2.6 + u * 0.8)), false);
        Zone(192, 20, 40, 12, (u, v) => Mul(st.Lining, 0.92 + 0.12 * Fbm(u * 0.8, v * 0.8, 4)), false);
        Zone(232, 20, 24, 12, (u, v) => Mul(Rgb(24, 24, 28), 0.85 + 0.3 * Hash((int)(u * S), (int)(v * S), 5)), false);
        Zone(192, 32, 32, 12, (u, v) => Glossy(Rgb(180, 28, 30), u, v, 6), false);
        Zone(224, 32, 32, 12, (u, v) => Mix(Rgb(170, 120, 30), Rgb(255, 220, 100), 0.5 + 0.5 * Math.Sin(v * 2.9 + u * 0.4)), false);
        // tinted glass: dark teal with a reflection streak
        Zone(0, 52, 48, 32, (u, v) =>
        {
            Color c = Rgb(26, 46, 56);
            double streak = ((u * 0.6 + v) % 6.0);
            if (streak < 0.6) c = Mix(c, Rgb(150, 190, 205), 0.5);
            return Mul(c, 0.9 + 0.2 * (1 - v / 32));
        }, st.TintGlows);
        Zone(48, 52, 16, 8, (u, v) =>
        {
            double fu = (u % 4) - 2, fv = (v % 4) - 2;
            double r = Math.Sqrt(fu * fu + fv * fv);
            if (r > 1.8) return Rgb(10, 10, 14);
            if (r > 1.45) return Rgb(80, 220, 255);
            if (Math.Abs(fu - fv * 0.6) < 0.18 && r < 1.3) return Rgb(255, 120, 60);  // the needle
            return Rgb(14, 40, 60);
        }, true);
        Zone(64, 52, 8, 8, (u, v) => Rgb(255, 40, 30), true);
        Zone(72, 52, 8, 8, (u, v) => Rgb(40, 255, 80), true);
        Zone(80, 52, 8, 8, (u, v) => Rgb(255, 250, 225), true);
        Zone(88, 52, 8, 8, (u, v) => Rgb(60, 170, 255), true);
        Zone(96, 52, 32, 16, (u, v) => Mix(Rgb(240, 160, 60), Rgb(255, 236, 160), 0.5 + 0.5 * Math.Sin(u * 0.7)), true);
        Zone(48, 60, 16, 8, (u, v) => Mix(Rgb(30, 190, 230), Rgb(190, 250, 255), Fbm(u * 0.8, v * 0.8, 7)), true);
        Zone(64, 60, 16, 8, (u, v) => Mix(Rgb(255, 80, 10), Rgb(255, 230, 90), Fbm(u * 1.4, v * 1.2, 8)), true);
        Zone(80, 60, 16, 8, (u, v) => Mix(Rgb(90, 255, 220), Rgb(220, 255, 255), Fbm(u, v, 9)), true);
        Zone(48, 68, 16, 16, (u, v) => (((u + v) * 2.6) % 1.0) < 0.4 ? Rgb(150, 122, 80) : Rgb(198, 170, 120), false);
        Zone(64, 68, 32, 16, (u, v) => Glossy(st.Navy, u, v, 10), false);
        Zone(96, 68, 32, 16, (u, v) => Glossy(st.Accent, u, v, 11), false);
        Zone(128, 44, 40, 40, (u, v) =>
        {
            Color c = Mix(Rgb(150, 100, 40), Rgb(250, 200, 110), 0.5 + 0.5 * Math.Sin(v * 2.2 + u * 0.5));
            if (((int)(u * 1.0) + (int)(v * 1.0)) % 5 == 0 && (u % 1) < 0.35 && (v % 1) < 0.35) c = Rgb(110, 70, 30);  // rivets
            return c;
        }, false);
        Zone(168, 44, 40, 40, (u, v) =>
        {
            Color c = Glossy(Rgb(22, 22, 26), u, v, 12);
            if (v % 8 > 6.6 && v % 8 < 7.4) c = Rgb(170, 26, 26);                       // a red stripe
            if (u % 10 < 1.2 && v % 8 < 1.2) c = Rgb(220, 224, 232);                    // chrome badges
            return c;
        }, false);
        Zone(208, 44, 48, 40, (u, v) => Metal(Rgb(132, 136, 144), u, v, 13, 0.1), false);
        Zone(0, 84, 64, 32, (u, v) =>
        {
            Color c = Mul(st.Canvas, 0.9 + 0.15 * Fbm(u * 0.7, v * 0.7, 14));
            if ((u % 6) < 0.15 || (v % 6) < 0.15) c = Mul(c, 0.8);                    // quilting
            return c;
        }, false);
        Zone(64, 84, 64, 32, (u, v) => Glossy(Rgb(240, 242, 246), u, v, 15), false);
        Zone(128, 84, 64, 32, (u, v) => Planks(st.Wood, u, v, 2.0, 16, 0.55), false);
        Zone(192, 84, 64, 32, (u, v) => Planks(st.DarkWood, u, v, 2.0, 17, 0.55), false);
        // decking: rows are z = 11 - row, columns x = col - 56
        Zone(0, 116, 128, 32, (u, v) => Deck(st, u, v), false);
        Zone(128, 116, 64, 32, (u, v) =>
        {
            Color c = Mix(Rgb(170, 210, 235), Rgb(235, 248, 255), Fbm(u * 0.5, v * 0.5, 18));
            if (Smooth(u * 1.5, v * 1.5, 19) > 0.72) c = Rgb(250, 254, 255);           // frost
            return c;
        }, false);
        Zone(192, 116, 64, 32, (u, v) => Metal(st.Steel, u, v, 20, 0.05), false);
        Zone(0, 148, 64, 32, (u, v) =>
        {
            double m = Fbm(u * 0.9, v * 0.9, 21);
            Color c = Mix(Rgb(46, 78, 30), Rgb(110, 150, 50), m);
            if (Hash((int)(u * S), (int)(v * S), 22) < 0.04) c = Rgb(150, 190, 70);
            return c;
        }, false);
        Zone(64, 148, 32, 32, (u, v) =>
        {
            double leaf = Smooth(u * 2.2, v * 1.1, 23);
            Color c = Mix(Rgb(28, 56, 22), Rgb(70, 120, 40), leaf);
            if (leaf < 0.18) return Argb(0, 0, 0, 0);                                    // gaps between strands
            return c;
        }, false);
        Zone(96, 148, 32, 32, (u, v) =>
        {
            Color c = Mix(Rgb(150, 160, 70), Rgb(200, 200, 110), Fbm(u * 3, v * 0.3, 24));
            if (v % 4 < 0.3) c = Rgb(110, 110, 50);                                     // nodes
            return c;
        }, false);
        Zone(128, 148, 64, 32, (u, v) =>
        {
            Color c = Planks(Rgb(96, 88, 70), u, v, 2.0, 25, 0.5);
            double rot = Fbm(u * 0.4, v * 0.6, 26);
            c = Mix(c, Rgb(60, 70, 50), rot * 0.6);
            if (rot > 0.68) c = Mix(c, Rgb(60, 100, 40), 0.6);                         // moss creeping in
            return c;
        }, false);
        Zone(192, 148, 32, 32, (u, v) => Mul(Rgb(232, 224, 200), 0.85 + 0.2 * Fbm(u, v, 27)), false);
        // carved dragon scales: rows of overlapping arcs, dark wood with gilt edges
        Zone(224, 148, 32, 32, (u, v) =>
        {
            double row = Math.Floor(v / 1.2);
            double su = u / 1.4 + (row % 2) * 0.5;
            double fu = su - Math.Floor(su) - 0.5, fv = v / 1.2 - row;
            double d = Math.Sqrt(fu * fu * 1.6 + fv * fv);
            Color c = Mul(Rgb(70, 46, 28), 0.7 + 0.5 * fv);
            if (d > 0.42 && d < 0.55) c = Rgb(200, 150, 60);
            return c;
        }, false);
    }

    static Color Deck(Style st, double u, double v)
    {
        switch (st.Deck)
        {
            case 0: // teak with black caulking
            {
                Color c = Planks(Rgb(170, 112, 62), u, v, 1.0, 30, 0.95);
                double fv = v - Math.Floor(v);
                if (fv < 0.14) c = Rgb(28, 22, 18);
                return c;
            }
            case 1: return Planks(Rgb(150, 104, 62), u, v, 2.0, 31, 0.5);    // spruce
            case 2: return Planks(Rgb(96, 56, 34), u, v, 2.0, 32, 0.5);      // varnished dark
            case 3: return Mix(Planks(Rgb(110, 104, 92), u, v, 2.0, 33, 0.5), Rgb(70, 80, 60), 0.3 * Fbm(u * 0.3, v * 0.3, 34));
            case 5: return Planks(Rgb(66, 44, 26), u, v, 2.0, 36, 0.5);    // dark oak
            case 6: return Mix(Planks(Rgb(226, 206, 198), u, v, 1.0, 37, 0.88), Rgb(240, 214, 230), 0.25 * Fbm(u * 0.2, v * 0.2, 38));   // blush birch
            default: // steel diamond plate
            {
                Color c = Metal(Rgb(110, 116, 112), u, v, 35, 0.0);
                double du = (u * 1.4) % 1.0, dv = (v * 1.4) % 1.0;
                bool odd = ((int)(u * 1.4) + (int)(v * 1.4)) % 2 == 0;
                double t = odd ? Math.Abs(du - dv) : Math.Abs(du + dv - 1);
                if (t < 0.12) c = Mul(c, 1.3);
                return c;
            }
        }
    }

    // â”€â”€ each boat's own pictures â”€â”€
    static void Pictures(string boat)
    {
        const int PV = 180;
        if (boat == "steamboat") SteamPictures();
        if (boat == "fairy_boat") FairyPictures();
        if (boat == "yacht")
        {
            // the flag: navy, a gold anchor, 13 columns (its back shows 1 to 12)
            for (int y = PV * S; y < (PV + 6) * S; y++) for (int x = 0; x < 13 * S; x++)
            {
                double c = (x + 0.5) / S, s = (y - PV * S + 0.5) / S;
                Color col = Mul(Rgb(20, 34, 84), 0.9 + 0.15 * Fbm(c, s, 40));
                double ax = c - 6.5, ay = s - 3.0;
                bool anchor = (Math.Abs(ax) < 0.25 && ay > -2.0 && ay < 1.9) || (Math.Abs(ay + 1.2) < 0.2 && Math.Abs(ax) < 1.2)
                    || (InEllipse(ax, ay, 0, -2.2, 0.5, 0.5) && !InEllipse(ax, ay, 0, -2.2, 0.25, 0.25))
                    || (ay > 0.7 && ay < 2.0 && Math.Abs(Math.Sqrt(ax * ax + (ay - 0.4) * (ay - 0.4)) - 1.5) < 0.22);
                if (anchor) col = Rgb(240, 200, 90);
                img[x, y] = col;
            }
        }
        if (boat == "longship")
        {
            // four round shields 4 x 4 from u 4: painted, an iron rim, a boss; round (transparent corners)
            for (int d = 0; d < 4; d++)
                for (int y = PV * S; y < (PV + 4) * S; y++) for (int x = (4 + d * 4) * S; x < (8 + d * 4) * S; x++)
                {
                    double px = (x - (4 + d * 4) * S + 0.5) / S - 2, py = (y - PV * S + 0.5) / S - 2;
                    double r = Math.Sqrt(px * px + py * py);
                    Color col;
                    if (r > 2.0) col = Clear;
                    else if (r > 1.75) col = Rgb(70, 70, 76);
                    else if (r < 0.45) col = Rgb(110, 112, 118);
                    else
                    {
                        Color a = d == 0 ? Rgb(180, 30, 28) : d == 1 ? Rgb(30, 60, 140) : d == 2 ? Rgb(220, 180, 40) : Rgb(40, 110, 50);
                        Color b = d == 0 ? Rgb(236, 230, 214) : d == 1 ? Rgb(230, 190, 50) : d == 2 ? Rgb(20, 20, 22) : Rgb(236, 230, 214);
                        bool alt = d == 0 ? (px * py > 0) : d == 1 ? (Math.Abs(px) < 0.35 || Math.Abs(py) < 0.35)
                            : d == 2 ? Math.Sin(Math.Atan2(py, px) * 3 + r * 3) > 0 : r > 1.35;
                        col = Mul(alt ? b : a, 0.85 + 0.2 * Fbm(px * 2 + d * 9, py * 2, 41));
                    }
                    img[x, y] = col;
                }
            // the sail: 33 wide (thirds of 11), 16 tall, red and white stripes running down it
            Sail(0, PV + 6, 33, 16, (z, s) =>
            {
                double band = Math.Floor((z + 16.5) / 3.3);
                Color c = band % 2 == 0 ? Rgb(178, 30, 28) : Rgb(234, 226, 206);
                c = Mul(c, 0.86 + 0.18 * Fbm(z * 0.6, s * 0.6, 42));
                if (s < 0.4 || s > 15.6 || Math.Abs(z) > 16.1) c = Mul(c, 0.7);         // the boltrope
                if (((s + 100) % 4.0) < 0.1) c = Mul(c, 0.85);                          // seams across
                return c;
            });
        }
    }

    // The River Queen's flat pictures (client/SteamboatModel: RAIL_*, FRET_*, FAN_*, FLAG_*, CREST_*, STAR_*: keep the
    // places the same). Each panel shows its first `length` columns on one side and the next `length` on the other.
    static void SteamPictures()
    {
        Color white = Rgb(246, 246, 248), gold1 = Rgb(170, 120, 30), gold2 = Rgb(255, 222, 110);
        // turned balusters every 0.8 between a top and bottom rail: 2.4 tall at (0,180), 1.6 tall at (0,183)
        Func<double, double, double, Color> rail = (c, s, h) =>
        {
            if (s < 0.32 || s > h - 0.26) return Mul(white, s < 0.32 ? 1.0 : 0.86);
            double f = (c % 0.8) - 0.4;
            double t = (s - 0.32) / (h - 0.58);                                     // 0 top to 1 bottom of the baluster
            double w = 0.1 + 0.1 * Math.Pow(Math.Sin(Math.PI * Math.Min(1, t * 1.15)), 2);   // the vase
            if (Math.Abs(t - 0.12) < 0.05 || Math.Abs(t - 0.9) < 0.05) w = 0.19;   // the beads
            if (Math.Abs(f) > w) return Clear;
            return Mul(white, 0.78 + 0.22 * (1 - Math.Abs(f) / w));
        };
        for (int y = 180 * S; y < 183 * S; y++) for (int x = 0; x < 64 * S; x++) img[x, y] = rail((x + 0.5) / S, (y - 180 * S + 0.5) / S, 2.4);
        for (int y = 183 * S; y < 185 * S; y++) for (int x = 0; x < 64 * S; x++) img[x, y] = rail((x + 0.5) / S, (y - 183 * S + 0.5) / S, 1.6);
        // gingerbread fretwork, 2 tall at (0,185): a top band with a row of holes, under it an arch every 4, the
        // corners filled with lace (round holes), a drop hanging in the middle of each arch
        for (int y = 185 * S; y < 187 * S; y++) for (int x = 0; x < 64 * S; x++)
        {
            double c = (x + 0.5) / S, s = (y - 185 * S + 0.5) / S;
            double p = (c % 4.0) - 2.0;
            double ap = Math.Abs(p);
            Color col = Clear;
            double open = Math.Pow(p / 1.75, 2) + Math.Pow((s - 0.45) / 1.35, 2);           // inside the arch: < 1
            if (s < 0.45) col = (s > 0.15 && s < 0.33 && (c % 0.5) < 0.25) ? Clear : white;    // the band and its holes
            else if (open >= 1 && s < 1.9)
            {
                double hx = ap - 1.5, hy = s - 0.85;
                bool hole = hx * hx + hy * hy < 0.07;                                          // a round hole in the lace
                col = hole ? Clear : (open < 1.18 ? Mul(white, 0.8) : white);                    // a shaded edge round the arch
            }
            if (ap < 0.11 && s < 1.0) col = white;                                              // the drop
            if (ap < 0.24 && s >= 1.0 && s < 1.36) col = Rgb(236, 190, 80);
            img[x, y] = col;
        }
        // a bunting fan, 3.2 x 1.6 at (64,180), twice: a half round of pleats hung from its straight top edge: a blue
        // middle with white stars, then white, red, white, red, the edge scalloped by the pleats
        for (int y = 180 * S; y < (int)(181.6 * S); y++) for (int x = 64 * S; x < (int)(70.4 * S); x++)
        {
            double c = (x + 0.5) / S - 64, s = (y - 180 * S + 0.5) / S;
            double px = (c % 3.2) - 1.6, py = s;
            double r = Math.Sqrt(px * px + py * py) / 1.6, a = Math.Atan2(py, px);
            if (r > 0.93 + 0.07 * Math.Abs(Math.Sin(a * 7))) { img[x, y] = Clear; continue; }
            Color col = r < 0.34 ? Rgb(28, 44, 120) : r < 0.52 ? white : r < 0.69 ? Rgb(196, 28, 36) : r < 0.85 ? white : Rgb(196, 28, 36);
            if (r < 0.34 && Hash((int)(c * S * 0.5), (int)(s * S * 0.5), 99) < 0.18) col = white;
            img[x, y] = Mul(col, 0.84 + 0.16 * Math.Cos(a * 14));
        }
        // the Stars and Stripes, 13 columns x 7 at (72,180): thirteen stripes, the blue canton at the pole
        for (int y = 180 * S; y < 187 * S; y++) for (int x = 72 * S; x < 85 * S; x++)
        {
            double c = (x + 0.5) / S - 72, s = (y - 180 * S + 0.5) / S;
            int stripe = (int)Math.Floor(s / 7.0 * 13);
            Color col = stripe % 2 == 0 ? Rgb(190, 30, 40) : white;
            if (c < 5.6 && s < 7.0 * 7 / 13)
            {
                col = Rgb(30, 42, 110);
                double sx = (c % 0.95) - 0.47, sy = (s % 0.75) - 0.37;
                if (sx * sx + sy * sy < 0.03) col = white;
            }
            img[x, y] = Mul(col, 0.88 + 0.14 * Fbm(c * 0.7, s * 0.7, 100));
        }
        // the pilothouse crest, 6 x 3.5 at (88,180), twice: a gold medallion (a red heart in it) between curling
        // scrolls, a star above, on a gold base bar
        for (int y = 180 * S; y < (int)(183.5 * S); y++) for (int x = 88 * S; x < 100 * S; x++)
        {
            double c = (x + 0.5) / S - 88, s = (y - 180 * S + 0.5) / S;
            double px = (c % 6.0) - 3.0, py = s;
            double ax = Math.Abs(px);
            Color col = Clear;
            double gold = 0.5 + 0.5 * Math.Sin(py * 3.1 + px * 0.9);
            bool bar = py > 3.0;
            bool medal = InEllipse(px, py, 0, 1.75, 1.05, 1.25);
            bool medalRim = medal && !InEllipse(px, py, 0, 1.75, 0.8, 1.0);
            double sr = Math.Sqrt((ax - 2.1) * (ax - 2.1) + (py - 2.2) * (py - 2.2));
            bool scroll = (sr > 0.45 && sr < 0.72 && !(px * (ax - 2.1) < 0 && py < 2.2)) || (ax > 1.0 && ax < 2.6 && Math.Abs(py - 2.85 + 0.2 * (ax - 1.0)) < 0.13);
            double tr = Math.Sqrt(px * px + (py - 0.25) * (py - 0.25)), ta = Math.Atan2(py - 0.25, px);
            bool star = tr < 0.42 * (0.5 + 0.5 * Math.Pow(Math.Abs(Math.Cos(2.5 * ta + 0.3)), 2));
            if (bar || medalRim || scroll || star) col = Mix(gold1, gold2, gold);
            else if (medal) col = InEllipse(Math.Abs(px) - 0.22, py, 0, 1.6, 0.32, 0.3) || (py > 1.6 && Math.Abs(px) < 0.55 - (py - 1.6) * 0.75) ? Rgb(190, 30, 40) : white;
            img[x, y] = col;
        }
        // the star in a ring between the stacks, 4 x 4 at (100,180), twice
        for (int y = 180 * S; y < 184 * S; y++) for (int x = 100 * S; x < 108 * S; x++)
        {
            double c = (x + 0.5) / S - 100, s = (y - 180 * S + 0.5) / S;
            double px = (c % 4.0) - 2.0, py = s - 2.0;
            double r = Math.Sqrt(px * px + py * py), a = Math.Atan2(py, px);
            bool ring = r > 1.55 && r < 1.95;
            bool star = r < 1.35 * (0.42 + 0.58 * Math.Pow(Math.Abs(Math.Cos(2.5 * a + Math.PI / 4)), 3));
            img[x, y] = ring || star ? Mix(gold1, gold2, 0.5 + 0.5 * Math.Sin(px * 1.4 - py * 1.1)) : Clear;
        }
    }

    // ── the Fairy Blossom (client/FairyBoatModel: WING_*, PETAL_*, INNER_U, LEAF_U, BIG_LEAF_U: keep the places) ──

    // half a petal's (or leaf's) width at p (0 its tip, 1 its base): pointed tip, widest past the middle, a narrow foot
    static double PetalHalf(double p, double w)
    {
        double hw = w / 2 * Math.Pow(Math.Sin(Math.PI * Math.Min(1, p * 0.85)), 0.7);
        if (p > 0.85) hw *= 1 - (p - 0.85) / 0.15 * 0.5;
        return hw;
    }

    // a flat picture w x h at (U, V), twice side by side (its two sides)
    static void Twice(int U, int V, double w, double h, Func<double, double, Color> paint)
    {
        for (int y = V * S; y < (int)Math.Ceiling((V + h) * S); y++)
            for (int x = U * S; x < (int)Math.Ceiling((U + 2 * w) * S); x++)
            {
                double c = (x + 0.5) / S - U, s = (y + 0.5) / S - V;
                img[x, y] = paint((c % w) - w / 2, s);
            }
    }

    // a wing, chord x span: a long rounded blade widest toward its tip, its root narrow. In the texture only its veins
    // and golden edge (the membrane is clear); in the glow, the membrane faint and iridescent (pink, lilac, aqua),
    // brighter toward its edge, a few sparkles, the veins glowing pale gold
    static void Wing(int U, int V, double chord, double span)
    {
        for (int y = V * S; y < (int)Math.Ceiling((V + span) * S); y++)
            for (int x = U * S; x < (int)Math.Ceiling((U + 2 * chord) * S); x++)
            {
                double cc = (x + 0.5) / S - U, s = (y + 0.5) / S - V;
                double c = (cc % chord) - chord / 2;
                double sn = s / span;
                double e = sn < 0.6 ? (sn - 0.6) / 0.6 : (sn - 0.6) / 0.4;
                double hw = chord / 2 * Math.Sqrt(Math.Max(0, 1 - e * e));
                if (sn < 0.12) hw = Math.Max(hw, chord * 0.1);
                img[x, y] = Clear;
                glow[x, y] = true;
                glowColor[x, y] = Clear;
                if (Math.Abs(c) >= hw) continue;
                double rel = Math.Abs(c) / hw;
                // veins: a few flowing ones fanning out from the root (curving toward the tip), and arcs round the root
                // every few px making long, organic cells
                double a = Math.Atan2(c, s + 1.5) * (1 - 0.35 * sn);
                double vr = Math.Abs(((a / 0.34) % 1.0 + 1.0) % 1.0 - 0.5) * 0.34 * (s + 1.5);
                double arc = Math.Sqrt(c * c + (s + 1.5) * (s + 1.5));
                bool vein = Math.Abs(c) < 0.1 + 0.05 * (1 - sn) || (vr < 0.065 && s > 1.2) || (s > 3 && Math.Abs(arc % (span / 3.2) - span / 6.4) > span / 6.4 - 0.07);
                bool edge = hw - Math.Abs(c) < 0.28 || (sn > 0.97);
                if (edge)
                {
                    img[x, y] = Rgb(240, 206, 120);
                    glowColor[x, y] = Argb(235, 255, 232, 170);
                }
                else if (vein)
                {
                    img[x, y] = Rgb(250, 236, 200);
                    glowColor[x, y] = Argb(200, 255, 246, 225);
                }
                else
                {
                    Color pink = Rgb(255, 160, 214), lilac = Rgb(196, 160, 255), aqua = Rgb(150, 236, 255);
                    Color col = Mix(Mix(pink, lilac, sn), aqua, 0.5 + 0.5 * Math.Sin(sn * 5.0 + c * 0.9));
                    double alpha = 70 + 70 * rel * rel + 25 * Fbm(c * 0.8, s * 0.5, 101);
                    if (Hash((int)(cc * S), y, 102) < 0.012) { col = Rgb(255, 255, 255); alpha = 230; }
                    glowColor[x, y] = Argb(alpha * 0.6, col.R, col.G, col.B);
                    img[x, y] = Argb(40 + alpha * 0.55, col.R, col.G, col.B);   // (tinted by day too: the boat is drawn see-through)
                }
            }
    }

    static void FairyPictures()
    {
        Wing(0, 180, 9, 22);
        Wing(18, 180, 7, 16);
        // the lotus petals: rose at the foot to blush white at the tip, a paler middle, fine veins, a soft edge
        Func<Color, Color, double, double, Func<double, double, Color>> petal = (foot, tip, w, h) => (c, s) =>
        {
            double p = s / h;
            double hw = PetalHalf(p, w);
            if (Math.Abs(c) >= hw) return Clear;
            double rel = Math.Abs(c) / hw;
            Color col = Mix(tip, foot, Math.Pow(p, 1.3));
            col = Mix(col, Rgb(255, 250, 252), 0.25 * (1 - rel));
            if (Math.Abs(Math.Sin(Math.Atan2(c, h - s + 0.5) * 9)) < 0.12) col = Mul(col, 0.94);
            if (rel > 0.86) col = Mul(col, 0.9);
            return col;
        };
        Twice(32, 180, 4, 7, petal(Rgb(232, 96, 156), Rgb(255, 238, 246), 4, 7));
        Twice(40, 180, 3, 5, petal(Rgb(212, 60, 130), Rgb(255, 200, 226), 3, 5));
        // leaves: green, a pale midrib and side veins
        Func<double, double, Func<double, double, Color>> leaf = (w, h) => (c, s) =>
        {
            double p = s / h;
            double hw = PetalHalf(p, w);
            if (Math.Abs(c) >= hw) return Clear;
            Color col = Mix(Rgb(150, 206, 116), Rgb(70, 138, 72), Math.Pow(p, 0.8));
            if (Math.Abs(c) < 0.1 + 0.04 * p) col = Rgb(196, 230, 160);
            else if (Math.Abs(((s - Math.Abs(c) * 0.9) % 1.1) - 0.55) > 0.48) col = Mul(col, 1.12);
            if (Math.Abs(c) / hw > 0.88) col = Mul(col, 0.85);
            return col;
        };
        Twice(46, 180, 3, 5, leaf(3, 5));
        Twice(52, 180, 5, 8, leaf(5, 8));
    }

    // the Fairy Blossom's sides: pearl at the top blushing pink, lilac at the waterline, a gold filigree vine with
    // curling tendrils and leaves, little painted flowers
    static Color FairySide(double x, double y)
    {
        if (y >= 0.6) return Mul(Rgb(150, 130, 200), 0.9 + 0.15 * Fbm(x * 0.5, y * 0.5, 103));
        Color c = y < -5 ? Rgb(252, 248, 252) : y < -2.5 ? Mix(Rgb(252, 248, 252), Rgb(250, 212, 228), (y + 5) / 2.5)
            : Mix(Rgb(250, 212, 228), Rgb(208, 190, 238), (y + 2.5) / 3.1);
        c = Mul(c, 0.96 + 0.06 * Fbm(x * 0.3, y * 0.3, 104));
        double yv = -2.6 + 0.7 * Math.Sin(x * 0.45);
        Color gold = Rgb(226, 184, 92);
        if (Math.Abs(y - yv) < 0.13) return gold;
        // a tendril curling off the vine every 6 px, above and below in turn
        double k = Math.Floor((x + 300) / 6.0), cx = k * 6 - 300 + 1.5;
        double side = ((int)k % 2 == 0) ? -1 : 1;
        double cy = -2.6 + 0.7 * Math.Sin(cx * 0.45) + side * 0.85;
        double r = Math.Sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy));
        if (r > 0.42 && r < 0.56 && !((x - cx) > 0 && (y - cy) * side > 0)) return gold;
        // a leaf beside it, and a little five-petalled flower
        double lx = k * 6 - 300 + 4.2, ly = -2.6 + 0.7 * Math.Sin(lx * 0.45) - side * 0.55;
        if (InEllipse(x, y, lx, ly, 0.55, 0.22)) return Rgb(214, 172, 80);
        double fx = k * 6 - 300 + 3.0, fy = -2.6 + 0.7 * Math.Sin(fx * 0.45) + side * 1.1;
        double fr = Math.Sqrt((x - fx) * (x - fx) + (y - fy) * (y - fy)), fa = Math.Atan2(y - fy, x - fx);
        if (fr < 0.14) return gold;
        if (fr < 0.38 * (0.6 + 0.4 * Math.Abs(Math.Cos(fa * 2.5)))) return Rgb(246, 140, 182);
        return c;
    }

    // ── the seasonal decorations (client/DecorModels: the zones each repaints) ──
    static void DecorExtra(string name)
    {
        BloodDecor(name);   // the Blood Moon decorations (tools/BloodDecorTex.cs)
        Func<Color, double, double, int, Color> ribbed = (baseC, u, v, seed) =>
            Mul(baseC, 0.72 + 0.28 * Math.Abs(Math.Sin(u * 1.3)) + 0.06 * Fbm(u, v, seed));
        if (name == "wishing_well")
        {
            // GREY: fieldstone, rounded stones of mixed greys (a few warm, a few blue) in pale mortar
            Zone(208, 44, 48, 40, (u, v) =>
            {
                double row = Math.Floor(v / 3.5);
                double uu = u + (row % 2) * 2.4;
                double col = Math.Floor(uu / 4.8);
                double fu = uu / 4.8 - col, fv = v / 3.5 - row;
                double edge = Math.Min(Math.Min(fu, 1 - fu) * 4.8, Math.Min(fv, 1 - fv) * 3.5);
                if (edge < 0.45) return Mix(Rgb(176, 170, 156), Rgb(140, 134, 122), Fbm(u * 0.9, v * 0.9, 401));
                double pick = Hash((int)col, (int)row, 402);
                Color stone = pick < 0.15 ? Rgb(146, 132, 114) : pick < 0.3 ? Rgb(112, 120, 132) : Rgb(126 + pick * 30, 126 + pick * 30, 124 + pick * 28);
                return Mul(stone, 0.82 + 0.16 * Math.Min(1, edge / 1.4) + 0.1 * (Fbm(u * 0.7, v * 0.7, 403) - 0.5));
            }, false);
            // MOSS: soft green moss, darker in the hollows
            Zone(0, 148, 64, 32, (u, v) => Mix(Rgb(70, 110, 46), Rgb(122, 160, 70), Fbm(u * 0.8, v * 0.8, 404)), false);
            // DECK: red clay shingles in overlapping rows, each tile its own shade, a dark line under each row
            Zone(0, 116, 128, 32, (u, v) =>
            {
                double row = Math.Floor(v / 3.0);
                double uu = u + (row % 2) * 2.0;
                double tile = Math.Floor(uu / 4.0);
                double fv = v / 3.0 - row;
                double fu = uu / 4.0 - tile;
                if (fv > 0.82 || fu < 0.07) return Rgb(74, 34, 26);
                double shade = 0.82 + 0.25 * Hash((int)tile, (int)row, 405);
                return Mul(Mix(Rgb(178, 72, 52), Rgb(150, 58, 44), fv), shade + 0.08 * (Fbm(u, v, 406) - 0.5));
            }, false);
            // WATER: the water in the bucket, clear blue-green, a soft highlight
            Zone(48, 60, 16, 8, (u, v) =>
            {
                double ripple = 0.5 + 0.5 * Math.Sin(u * 1.4 + Fbm(u * 0.5, v * 0.5, 407) * 3);
                return Mix(Rgb(34, 84, 104), Rgb(70, 140, 160), ripple * 0.6);
            }, false);
            // OLDWOOD: the bucket's oak staves, grain running up and down, darker at the edges
            Zone(128, 148, 64, 32, (u, v) =>
            {
                double grain = Fbm(u * 2.6, v * 0.22, 411);
                Color c = Mix(Rgb(112, 76, 44), Rgb(156, 112, 68), grain);
                if (Fbm(u * 5.0, v * 0.5, 412) > 0.72) c = Mul(c, 0.82);
                return c;
            }, false);
            // STEEL: the hoops and handle, dark iron with a little rust
            Zone(192, 116, 64, 32, (u, v) =>
            {
                Color c = Mix(Rgb(58, 58, 62), Rgb(92, 92, 98), Fbm(u * 0.9, v * 0.9, 413));
                if (Fbm(u * 0.6, v * 0.6, 414) > 0.68) c = Mix(c, Rgb(120, 70, 40), 0.5);
                return c;
            }, false);
            // NAVY: the well's floor, wet pebbles of mixed dark greys and browns
            Zone(64, 68, 32, 16, (u, v) =>
            {
                double cu = Math.Floor(u / 1.4), cv = Math.Floor(v / 1.4);
                double fu = u / 1.4 - cu - 0.5, fv = v / 1.4 - cv - 0.5;
                double pick = Hash((int)cu, (int)cv, 415);
                Color pebble = pick < 0.3 ? Rgb(84, 74, 60) : pick < 0.6 ? Rgb(70, 74, 76) : Rgb(96, 96, 92);
                double round = 1 - Math.Min(1, Math.Sqrt(fu * fu + fv * fv) * 2.2);
                return Mul(Mix(Rgb(30, 31, 31), pebble, Math.Min(1, round * 2.5)), 0.55 + 0.2 * round);
            }, false);
            // the pool (client/WishingWellModel.Water, see-through): one picture of the whole round surface, centre
            // at (84, 196): deep blue-green, ripples spreading from the middle, darker and deeper looking toward the
            // stones, light catching the ripple crests; a faint shimmer of glow on the crests
            for (int y = 180 * S; y < 220 * S; y++)
                for (int x = 64 * S; x < 112 * S; x++)
                {
                    double u = (x + 0.5) / S - 84, v = (y + 0.5) / S - 196;
                    double r = Math.Sqrt(u * u + v * v);
                    double wobble = Fbm(u * 0.18 + 30, v * 0.18 + 30, 416) * 3.0;
                    double ripple = 0.5 + 0.5 * Math.Sin(r * 1.25 - wobble);
                    double crest = Math.Pow(ripple, 6);
                    double edge = Math.Min(1, Math.Max(0, (r - 9) / 8));
                    Color c = Mix(Rgb(30, 90, 112), Rgb(12, 38, 56), edge * 0.85);
                    c = Mix(c, Rgb(130, 196, 214), crest * 0.4 * (1 - edge * 0.6));
                    bool glint = Hash(x, y, 417) < 0.012 && crest > 0.3;
                    if (glint) c = Rgb(235, 250, 255);
                    img[x, y] = Argb(glint ? 230 : 185 + 55 * edge, c.R, c.G, c.B);
                    glow[x, y] = true;
                    glowColor[x, y] = glint ? Argb(200, 220, 245, 255) : Argb(30 + 70 * crest, 70, 170, 200);
                }
        }
        if (name == "mailbox")
        {
            // STEEL: the box, painted navy metal, ribs running along it, a little worn at the edges
            Zone(192, 116, 64, 32, (u, v) =>
            {
                double rib = Math.Abs(Math.Sin(v * 1.6));
                Color c = Mul(Rgb(48, 74, 124), 0.82 + 0.18 * rib + 0.08 * (Fbm(u * 0.6, v * 0.6, 501) - 0.5));
                if (Fbm(u * 1.4, v * 1.4, 502) > 0.8) c = Mix(c, Rgb(150, 150, 156), 0.5);
                return c;
            }, false);
            // RED: the flag, glossy red paint
            Zone(192, 32, 32, 12, (u, v) => Mul(Rgb(206, 40, 40), 0.85 + 0.2 * Fbm(u * 0.8, v * 0.8, 503)), false);
            // WOOD: the post, weathered pine, grain up and down
            Zone(128, 84, 64, 32, (u, v) => Mix(Rgb(116, 84, 52), Rgb(152, 114, 72), Fbm(u * 2.2, v * 0.2, 504)), false);
        }
        if (name == "town_mailbox")
        {
            // STEEL: glossy white enamel, faint ribs along it, a soft sheen
            Zone(192, 116, 64, 32, (u, v) =>
            {
                double rib = Math.Abs(Math.Sin(v * 1.6));
                return Mul(Rgb(240, 238, 232), 0.9 + 0.08 * rib + 0.04 * (Fbm(u * 0.5, v * 0.5, 511) - 0.5));
            }, false);
            // WOOD: the post, painted white and fluted (grooves running up it)
            Zone(128, 84, 64, 32, (u, v) =>
            {
                double flute = Math.Abs(Math.Sin(u * 2.4));
                return Mul(Rgb(236, 234, 228), 0.78 + 0.2 * flute);
            }, false);
            // DARKWOOD: the plinth, capital and cross board: white marble with soft grey veins
            Zone(192, 84, 64, 32, (u, v) =>
            {
                double vein = Math.Abs(Math.Sin(u * 0.5 + v * 0.3 + Fbm(u * 0.3, v * 0.3, 512) * 6));
                Color c = Rgb(232, 230, 226);
                if (vein < 0.08) c = Mix(c, Rgb(150, 150, 158), 0.6);
                return Mul(c, 0.92 + 0.08 * Fbm(u, v, 513));
            }, false);
            // GOLD: polished gold, bright along the edges
            Zone(224, 32, 32, 12, (u, v) => Mix(Rgb(196, 146, 40), Rgb(255, 222, 110), Fbm(u * 0.9, v * 0.9, 514)), false);
            // RED: the flag, deep crimson
            Zone(192, 32, 32, 12, (u, v) => Mul(Rgb(160, 24, 40), 0.85 + 0.2 * Fbm(u * 0.8, v * 0.8, 515)), false);
        }
        if (name == "crimson_revenant")
        {
            // STEEL: blackened iron plate, hammered, rust and dried blood at the edges, a few bright scratches
            Zone(192, 116, 64, 32, (u, v) =>
            {
                Color c = Mix(Rgb(34, 32, 36), Rgb(64, 60, 66), Fbm(u * 0.5, v * 0.5, 601));
                if (Fbm(u * 0.9, v * 0.9, 602) > 0.7) c = Mix(c, Rgb(96, 34, 26), 0.6);
                if (Hash((int)(u * 2), (int)(v * 0.5), 603) < 0.03) c = Mix(c, Rgb(130, 126, 130), 0.6);
                double plate = (v % 4.0) / 4.0;
                if (plate > 0.88) c = Mul(c, 0.6);
                else if (plate < 0.1) c = Mul(c, 1.25);
                return c;
            }, false);
            // ENGINE: the dark body under the plate, rotten black-red
            Zone(168, 44, 40, 40, (u, v) => Mix(Rgb(26, 14, 16), Rgb(52, 22, 24), Fbm(u * 0.6, v * 0.6, 604)), false);
            // CANVAS: the tattered crimson cape, dark folds, frayed holes
            Zone(0, 84, 64, 32, (u, v) =>
            {
                double fold = 0.7 + 0.3 * Math.Abs(Math.Sin(u * 0.9 + Fbm(u * 0.2, v * 0.2, 605) * 2));
                Color c = Mul(Mix(Rgb(110, 12, 18), Rgb(150, 20, 26), Fbm(u * 0.4, v * 0.4, 606)), fold);
                if (Fbm(u * 0.8, v * 0.8, 607) > 0.74) c = Mul(c, 0.45);
                return c;
            }, false);
            // BONE: old yellowed horn, ringed
            Zone(192, 148, 32, 32, (u, v) => Mul(Mix(Rgb(176, 160, 128), Rgb(220, 206, 172), Fbm(u * 0.5, v * 0.5, 608)), 0.82 + 0.18 * Math.Abs(Math.Sin(v * 2.2))), false);
            // GREY: the blade, dark steel with a bright edge
            Zone(208, 44, 48, 40, (u, v) =>
            {
                Color c = Mix(Rgb(70, 70, 78), Rgb(118, 118, 128), Fbm(u * 0.3, v * 1.2, 609));
                if (Hash((int)(u * 3), (int)(v * 3), 610) < 0.02) c = Rgb(170, 170, 180);
                return c;
            }, false);
            // LEATHER: worn dark leather
            Zone(128, 20, 40, 24, (u, v) => Mix(Rgb(44, 28, 20), Rgb(74, 46, 30), Fbm(u * 0.8, v * 0.8, 611)), false);
            // GOLD: tarnished old gold
            Zone(224, 32, 32, 12, (u, v) => Mix(Rgb(120, 86, 30), Rgb(196, 150, 60), Fbm(u * 0.9, v * 0.9, 612)), false);
            // FIRE (glowing): molten red light, white-hot in the middle
            Zone(64, 60, 16, 8, (u, v) => Mix(Rgb(220, 20, 10), Rgb(255, 140, 90), Fbm(u * 1.2, v * 1.2, 613)), true);
            // NAV_RED (glowing): the eyes, burning
            Zone(64, 52, 8, 8, (u, v) => Mix(Rgb(255, 40, 30), Rgb(255, 200, 160), Math.Max(0, 1 - Math.Abs(v - 4) / 4.0)), true);
            // BLACK: the visor slit
            Zone(232, 20, 24, 12, (u, v) => Rgb(10, 6, 8), false);
        }
        if (name == "gorefang")
        {
            // CANVAS: its hide: deep blood-red fur shading to black, coarse strokes running back, crimson highlights
            // on the strands, matted dark patches
            Zone(0, 84, 64, 32, (u, v) =>
            {
                double stroke = Math.Abs(Math.Sin(u * 2.1 + Fbm(u * 0.3, v * 0.3, 621) * 5));
                Color c = Mix(Rgb(46, 8, 12), Rgb(128, 20, 24), Fbm(u * 0.45, v * 0.8, 622));
                c = Mul(c, 0.62 + 0.45 * stroke);
                if (stroke > 0.94) c = Mix(c, Rgb(196, 46, 38), 0.55);
                if (Fbm(u * 0.8, v * 0.8, 623) > 0.74) c = Mix(c, Rgb(20, 4, 6), 0.6);
                return c;
            }, false);
            // WHITE: the mane, legs and tail brush: long black fur with blood-red tips catching the light
            Zone(64, 84, 64, 32, (u, v) =>
            {
                double stroke = Math.Abs(Math.Sin(u * 1.6 + v * 0.5 + Fbm(u * 0.25, v * 0.25, 624) * 6));
                Color c = Mix(Rgb(14, 6, 8), Rgb(44, 12, 14), Fbm(u * 0.4, v * 0.6, 625));
                c = Mul(c, 0.75 + 0.45 * stroke);
                if (stroke > 0.9) c = Mix(c, Rgb(150, 26, 26), 0.6 * (stroke - 0.9) / 0.1);
                return c;
            }, false);
            // GREY: the collar and chain: black iron, rusted, worn bright on the edges
            Zone(208, 44, 48, 40, (u, v) =>
            {
                Color c = Mix(Rgb(36, 34, 36), Rgb(70, 66, 68), Fbm(u * 0.6, v * 0.6, 631));
                if (Fbm(u * 1.1, v * 1.1, 632) > 0.68) c = Mix(c, Rgb(104, 46, 24), 0.6);
                if (Hash((int)(u * 2), (int)(v * 2), 633) < 0.04) c = Rgb(130, 126, 124);
                return c;
            }, false);
            // ENGINE: its legs and jaw, near-black fur
            Zone(168, 44, 40, 40, (u, v) => Mul(Mix(Rgb(30, 10, 12), Rgb(62, 18, 20), Fbm(u * 0.6, v * 0.9, 626)), 0.8 + 0.25 * Math.Abs(Math.Sin(u * 2.1))), false);
            // BONE: spikes, fangs, claws and its bone mask: old ivory, cracked, stained red at the tips
            Zone(192, 148, 32, 32, (u, v) =>
            {
                Color c = Mul(Mix(Rgb(196, 182, 150), Rgb(232, 222, 196), Fbm(u * 0.5, v * 0.5, 627)), 0.85 + 0.15 * Math.Abs(Math.Sin(v * 2.4)));
                if (Hash((int)(u * 2), (int)(v * 2), 628) < 0.05) c = Mul(c, 0.6);
                return c;
            }, false);
            // LEATHER: the pads of its paws, dark
            Zone(128, 20, 40, 24, (u, v) => Mix(Rgb(28, 18, 18), Rgb(48, 30, 28), Fbm(u * 0.8, v * 0.8, 629)), false);
            // BLACK: its nose and the inside of its mouth
            Zone(232, 20, 24, 12, (u, v) => Rgb(16, 6, 8), false);
            // FIRE (glowing): the furnace glow of its throat
            Zone(64, 60, 16, 8, (u, v) => Mix(Rgb(220, 20, 10), Rgb(255, 120, 70), Fbm(u * 1.2, v * 1.2, 630)), true);
            // NAV_RED (glowing): burning eyes
            Zone(64, 52, 8, 8, (u, v) => Mix(Rgb(255, 50, 30), Rgb(255, 220, 170), Math.Max(0, 1 - Math.Abs(v - 4) / 4.0)), true);
        }
        if (name == "sanguine_witch")
        {
            // CANVAS: her robe, deep crimson silk, long folds falling, worn and stained darker toward the hem
            Zone(0, 84, 64, 32, (u, v) =>
            {
                double fold = 0.7 + 0.3 * Math.Abs(Math.Sin(u * 1.1 + Fbm(u * 0.2, v * 0.2, 641) * 2));
                Color c = Mul(Mix(Rgb(120, 10, 30), Rgb(170, 22, 44), Fbm(u * 0.4, v * 0.4, 642)), fold);
                if (v > 22) c = Mul(c, 1 - (v - 22) / 20.0);
                if (Fbm(u * 0.9, v * 0.9, 643) > 0.78) c = Mul(c, 0.55);
                return c;
            }, false);
            // DARKWOOD: the hood and over-robe, near-black wine red
            Zone(192, 84, 64, 32, (u, v) => Mul(Mix(Rgb(36, 6, 14), Rgb(66, 12, 24), Fbm(u * 0.5, v * 0.5, 644)), 0.8 + 0.2 * Math.Abs(Math.Sin(u * 1.4))), false);
            // WHITE: her skin, corpse-pale, grey-blue shadows, faint veins
            Zone(64, 84, 64, 32, (u, v) =>
            {
                Color c = Mix(Rgb(188, 186, 192), Rgb(222, 218, 222), Fbm(u * 0.4, v * 0.4, 645));
                if (Math.Abs(Math.Sin(u * 0.8 + Fbm(u * 0.3, v * 0.3, 646) * 6)) < 0.05) c = Mix(c, Rgb(120, 60, 90), 0.5);
                return c;
            }, false);
            // BONE: her crown and claws, polished old bone
            Zone(192, 148, 32, 32, (u, v) => Mul(Mix(Rgb(200, 188, 160), Rgb(238, 230, 208), Fbm(u * 0.5, v * 0.5, 647)), 0.85 + 0.15 * Math.Abs(Math.Sin(v * 2.0))), false);
            // GOLD: tarnished gold trim
            Zone(224, 32, 32, 12, (u, v) => Mix(Rgb(130, 90, 30), Rgb(206, 160, 70), Fbm(u * 0.9, v * 0.9, 648)), false);
            // BLACK: her mouth, her empty sleeves' shadow
            Zone(232, 20, 24, 12, (u, v) => Rgb(12, 4, 8), false);
            // FIRE (glowing): the orbs of blood and her bloody tears, bright and wet
            Zone(64, 60, 16, 8, (u, v) => Mix(Rgb(200, 10, 30), Rgb(255, 110, 120), Fbm(u * 1.3, v * 1.3, 649)), true);
            // NAV_RED (glowing): her eyes
            Zone(64, 52, 8, 8, (u, v) => Mix(Rgb(255, 40, 70), Rgb(255, 210, 220), Math.Max(0, 1 - Math.Abs(v - 4) / 4.0)), true);
            // ACCENT: the membrane of her wings: thin blood-red skin, darker veins branching through it, lighter
            // where it's stretched thinnest
            Zone(96, 68, 32, 16, (u, v) =>
            {
                Color c = Mix(Rgb(96, 8, 22), Rgb(168, 24, 40), Fbm(u * 0.35, v * 0.35, 650));
                double vein = Math.Abs(Math.Sin(u * 0.9 + v * 0.35 + Fbm(u * 0.25, v * 0.25, 651) * 5));
                if (vein < 0.09) c = Mix(c, Rgb(40, 2, 10), 0.75);
                return c;
            }, false);
        }
        if (name == "christmas_tree")
        {
            // the fir needles (DecorModels.ChristmasTree.FIR, below y 180): deep green, strokes of needles, lighter
            // tips, a dusting of snow
            Zone(0, 180, 256, 76, (u, v) =>
            {
                double n = Fbm(u * 0.25, v * 0.25, 120);
                Color c = Mix(Rgb(18, 62, 34), Rgb(44, 112, 56), n);
                double stroke = ((u * 1.7 + v * 2.3) % 1.0);
                if (stroke < 0.22) c = Mix(c, Rgb(70, 140, 70), 0.6);
                if (((u * 1.7 - v * 2.3) % 1.0 + 1.0) % 1.0 < 0.12) c = Mul(c, 0.7);
                if (Hash((int)(u * S), (int)(v * S), 121) < 0.025) c = Rgb(236, 244, 250);
                return c;
            }, false);
            // DECK: the red velvet tree skirt, a gold band round its edge
            Zone(0, 116, 128, 32, (u, v) =>
            {
                Color c = Mul(Rgb(170, 22, 30), 0.85 + 0.2 * Fbm(u * 0.4, v * 0.4, 122));
                if (v < 1.2 || v > 30.8 || u < 1.2 || u > 126.8) c = Rgb(236, 190, 80);
                return c;
            }, false);
            // baubles: glossy, a bright highlight
            Func<Color, Painter> bauble = baseC => (u, v) =>
            {
                Color c = Mul(baseC, 0.8 + 0.3 * Math.Sin(v * 1.4 + u * 0.6));
                if (((u + v) % 4.0) < 0.5) c = Mix(c, Rgb(255, 255, 255), 0.6);
                return c;
            };
            Zone(192, 32, 32, 12, bauble(Rgb(210, 20, 30)), false);
            Zone(128, 44, 40, 40, bauble(Rgb(236, 180, 50)), false);
            Zone(64, 68, 32, 16, bauble(Rgb(40, 80, 210)), false);
            Zone(64, 84, 64, 32, bauble(Rgb(210, 214, 224)), false);
            Zone(96, 68, 32, 16, bauble(Rgb(150, 60, 200)), false);
        }
        if (name == "spooky_scarecrow")
        {
            // CANVAS: a patched, faded purple-brown coat, crude stitching
            Zone(0, 84, 64, 32, (u, v) =>
            {
                Color c = Mix(Rgb(78, 52, 88), Rgb(108, 78, 56), Fbm(u * 0.3, v * 0.3, 130));
                if (Hash((int)(u / 4), (int)(v / 4), 131) < 0.22) c = Mix(c, Rgb(120, 96, 50), 0.5);
                if ((u % 4) < 0.25 && ((int)v % 2 == 0)) c = Mul(c, 0.6);
                return Mul(c, 0.9 + 0.15 * Fbm(u * 1.5, v * 1.5, 132));
            }, false);
            Zone(192, 116, 64, 32, (u, v) => ribbed(Rgb(232, 118, 26), u, v, 133), false);
            Zone(96, 148, 32, 32, (u, v) => Mix(Rgb(200, 168, 80), Rgb(244, 218, 130), Fbm(u * 2.5, v * 0.3, 134)), false);
            Zone(64, 60, 16, 8, (u, v) => Mix(Rgb(255, 150, 20), Rgb(255, 230, 90), Fbm(u * 1.2, v * 1.2, 135)), true);
        }
        if (name == "witchs_cauldron")
        {
            Zone(208, 44, 48, 40, (u, v) => Metal(Rgb(56, 56, 64), u, v, 140, 0.08), false);
            // ICE: the glowing green brew, bubbles in it
            Zone(128, 116, 64, 32, (u, v) =>
            {
                Color c = Mix(Rgb(50, 200, 60), Rgb(170, 255, 110), Fbm(u * 0.6, v * 0.6, 141));
                double fu = (u % 3) - 1.5, fv = (v % 3) - 1.5;
                if (Math.Abs(Math.Sqrt(fu * fu + fv * fv) - 0.8) < 0.15 && Hash((int)(u / 3), (int)(v / 3), 142) < 0.5) c = Rgb(220, 255, 200);
                return c;
            }, true);
            Zone(96, 148, 32, 32, (u, v) => Mix(Rgb(170, 130, 60), Rgb(220, 190, 110), Fbm(u * 2.5, v * 0.3, 143)), false);
            Zone(64, 84, 64, 32, (u, v) => Mix(Rgb(236, 226, 200), Rgb(250, 244, 226), Fbm(u * 0.5, v * 0.5, 144)), false);
        }
        if (name == "pumpkin_pile")
        {
            // DECK: hay, straws running along
            Zone(0, 116, 128, 32, (u, v) =>
            {
                Color c = Mix(Rgb(196, 160, 70), Rgb(236, 206, 112), Fbm(u * 0.15, v * 2.0, 150));
                if (((v * 3.1 + Math.Sin(u * 0.3)) % 1.0) < 0.18) c = Mul(c, 0.82);
                return c;
            }, false);
            Zone(192, 116, 64, 32, (u, v) => ribbed(Rgb(234, 120, 28), u, v, 151), false);
            Zone(64, 84, 64, 32, (u, v) => ribbed(Rgb(240, 234, 216), u, v, 152), false);
            Zone(0, 148, 64, 32, (u, v) => ((int)(u * 0.9) % 2 == 0) ? Rgb(60, 110, 50) : Rgb(170, 190, 90), false);
            Zone(128, 44, 40, 40, (u, v) => ribbed(Rgb(236, 190, 50), u, v, 153), false);
            Zone(128, 84, 64, 32, (u, v) => Mix(Rgb(176, 150, 90), Rgb(214, 190, 124), Fbm(u * 2.5, v * 0.2, 154)), false);
            Zone(192, 32, 32, 12, (u, v) => Rgb(180, 44, 24), false);
            Zone(96, 68, 32, 16, (u, v) => Rgb(226, 110, 30), false);
        }
        Func<Color, double, double, int, Color> ribbedD = (baseC, u, v, seed) => Mul(baseC, 0.75 + 0.25 * Math.Abs(Math.Sin(u * 1.3)) + 0.06 * Fbm(u, v, seed));
        if (name == "autumn_maple" || name == "autumn_birch")
        {
            bool maple = name == "autumn_maple";
            // the leaves (DecorModels.FOLIAGE): clumps of leaf shapes, red to gold on the maple, gold on the birch
            Color[] leaves = maple ? new[] { Rgb(196, 40, 24), Rgb(226, 90, 26), Rgb(240, 150, 30), Rgb(168, 30, 30), Rgb(244, 186, 52) }
                : new[] { Rgb(240, 196, 48), Rgb(232, 170, 30), Rgb(250, 214, 80), Rgb(214, 150, 30) };
            Zone(0, 180, 256, 76, (u, v) =>
            {
                int cu = (int)Math.Floor(u / 1.4 + 0.4 * Math.Sin(v * 0.9)), cv = (int)Math.Floor(v / 1.2);
                Color c = leaves[(int)(Hash(cu, cv, 240) * leaves.Length) % leaves.Length];
                double fu = (u / 1.4) % 1.0, fv = (v / 1.2) % 1.0;
                if (Math.Abs(fu - 0.5) < 0.07 || (fu - fv) * (fu - fv) < 0.004) c = Mul(c, 0.78);
                return Mul(c, 0.82 + 0.3 * Fbm(u * 0.3, v * 0.3, 241));
            }, false);
            // bark: dark furrowed for the maple; the birch white with black marks
            Zone(192, 84, 64, 32, (u, v) => Mul(Rgb(78, 58, 44), 0.75 + 0.35 * Math.Abs(Math.Sin(u * 2.1 + Fbm(u, v * 0.2, 242) * 4))), false);
            Zone(128, 84, 64, 32, (u, v) =>
            {
                Color c = Mul(Rgb(236, 232, 222), 0.92 + 0.1 * Fbm(u * 0.3, v * 2.0, 243));
                if (Hash((int)(u * 1.3), (int)(v * 2.5), 244) < 0.13 && (v * 2.5 % 1.0) < 0.35) c = Rgb(52, 48, 44);
                return c;
            }, false);
            Zone(192, 32, 32, 12, (u, v) => Rgb(190, 44, 24), false);
            Zone(96, 68, 32, 16, (u, v) => Rgb(226, 112, 30), false);
            Zone(128, 44, 40, 40, (u, v) => Rgb(240, 186, 46), false);
        }
        if (name.StartsWith("gravestone") || name == "bone_pile")
        {
            // weathered grey stone, speckled, a little lichen
            Zone(208, 44, 48, 40, (u, v) =>
            {
                Color c = Mul(Rgb(128, 128, 126), 0.82 + 0.3 * Fbm(u * 0.5, v * 0.5, 250));
                if (Hash((int)(u * S), (int)(v * S), 251) < 0.06) c = Mul(c, 0.7);
                if (Fbm(u * 0.25, v * 0.25, 252) > 0.72) c = Mix(c, Rgb(150, 160, 110), 0.4);
                return c;
            }, false);
            // DECK: the grave's earth, grass speckles
            Zone(0, 116, 128, 32, (u, v) => Hash((int)(u * S), (int)(v * S), 253) < 0.18 ? Rgb(78, 118, 52)
                : Mul(Rgb(96, 70, 48), 0.8 + 0.3 * Fbm(u * 0.5, v * 0.5, 254)), false);
            Zone(64, 84, 64, 32, (u, v) => Rgb(236, 228, 206), false);
            Zone(192, 148, 32, 32, (u, v) => Mul(Rgb(222, 212, 184), 0.82 + 0.25 * Fbm(u, v, 255)), false);
        }
        if (name == "beach_towel")
        {
            Zone(0, 116, 128, 32, (u, v) =>
            {
                int band = (int)Math.Floor(v / 3.0);
                Color c = band % 3 == 0 ? Rgb(40, 190, 200) : band % 3 == 1 ? Rgb(250, 248, 240) : Rgb(250, 110, 90);
                if (((u + v) % 1.0) < 0.12) c = Mul(c, 0.92);
                return c;
            }, false);
        }
        if (name == "beach_ball")
        {
            // one solid glossy colour per slice: ICE red, STEEL blue, MOSS yellow, VINE green, WHITE white
            Func<Color, Painter> gloss = c => (u, v) => Mul(c, 0.95 + 0.05 * Fbm(u * 0.5, v * 0.5, 264));
            Zone(128, 116, 64, 32, gloss(Rgb(232, 44, 44)), false);
            Zone(192, 116, 64, 32, gloss(Rgb(44, 116, 232)), false);
            Zone(0, 148, 64, 32, gloss(Rgb(252, 212, 44)), false);
            Zone(64, 148, 32, 32, gloss(Rgb(56, 192, 84)), false);
            Zone(64, 84, 64, 32, gloss(Rgb(250, 250, 248)), false);
        }
        if (name == "beach_umbrella")
        {
            Zone(0, 116, 128, 32, (u, v) => Mul(Rgb(220, 40, 44), 0.9 + 0.12 * Fbm(u * 0.4, v * 0.4, 260)), false);
            Zone(64, 84, 64, 32, (u, v) => Mul(Rgb(250, 248, 242), 0.94 + 0.06 * Fbm(u * 0.4, v * 0.4, 261)), false);
            Zone(96, 148, 32, 32, (u, v) => Mix(Rgb(220, 196, 140), Rgb(246, 226, 176), Fbm(u * 0.8, v * 0.8, 262)), false);
        }
        if (name == "beach_chair")
        {
            Zone(0, 84, 64, 32, (u, v) => (int)Math.Floor(u / 1.6) % 2 == 0 ? Rgb(40, 90, 200) : Rgb(248, 248, 244), false);
            Zone(128, 84, 64, 32, (u, v) => Planks(Rgb(196, 150, 96), u, v, 2.0, 263, 0.7), false);
            Zone(128, 44, 40, 40, (u, v) => Rgb(250, 160, 40), false);
        }
        if (name == "egg_basket" || name == "giant_egg" || name == "spring_bunny" || name == "tulip_bed")
        {
            // pastel eggs and petals: pink with dots, baby blue with stripes, mint with zigzags, butter yellow, lavender
            Func<double, double, double, bool> dot = (u, v, sz) => { double fu = (u % sz) - sz / 2, fv = (v % sz) - sz / 2; return fu * fu + fv * fv < sz * sz * 0.07; };
            bool flowers = name == "tulip_bed";
            Zone(192, 32, 32, 12, (u, v) => flowers ? Rgb(220, 30, 40) : dot(u, v, 2.4) ? Rgb(255, 255, 255) : Rgb(248, 168, 200), false);
            Zone(64, 68, 32, 16, (u, v) => flowers ? Rgb(246, 140, 190) : (v % 2.0) < 0.6 ? Rgb(255, 255, 255) : Rgb(150, 200, 246), false);
            Zone(96, 68, 32, 16, (u, v) => flowers ? Rgb(70, 160, 60) : Math.Abs(((u * 0.8 + 100) % 2.0) - 1.0) * 2.4 > (v % 2.4) ? Rgb(160, 230, 200) : Rgb(255, 255, 255), false);
            Zone(128, 44, 40, 40, (u, v) => flowers ? Rgb(250, 210, 40) : dot(u, v, 2.0) ? Rgb(250, 160, 120) : Rgb(252, 230, 140), false);
            Zone(192, 116, 64, 32, (u, v) => flowers ? Rgb(150, 70, 200) : (u % 2.2) < 0.7 ? Rgb(255, 255, 255) : Rgb(200, 170, 240), false);
            // WOOD: wicker
            Zone(128, 84, 64, 32, (u, v) => (((int)(u / 1.2) + (int)(v / 1.0)) % 2 == 0) ? Mul(Rgb(206, 160, 96), 0.9 + 0.2 * Math.Sin(v * 3)) : Mul(Rgb(176, 128, 72), 0.9 + 0.2 * Math.Sin(u * 3)), false);
            // WHITE: soft cream fur (the bunny) / white petals
            Zone(64, 84, 64, 32, (u, v) => Mul(Rgb(250, 246, 238), 0.9 + 0.12 * Fbm(u * 1.4, v * 1.4, 270)), false);
            if (name == "giant_egg")
            {
                // DECK: the big egg's paint: pastel bands, a zigzag band, dots
                Zone(0, 116, 128, 32, (u, v) =>
                {
                    double band = v % 8.0;
                    Color c = band < 2 ? Rgb(248, 168, 200) : band < 4 ? Rgb(252, 230, 140) : band < 6 ? Rgb(150, 200, 246) : Rgb(160, 230, 200);
                    if (Math.Abs(band - 4.0 - Math.Abs(((u + 100) % 2.0) - 1.0)) < 0.25) c = Rgb(255, 255, 255);
                    if (dot(u + (int)(v / 2) * 1.2, v, 2.4) && band < 2) c = Rgb(255, 255, 255);
                    return c;
                }, false);
            }
            if (name == "tulip_bed")
            {
                Zone(0, 116, 128, 32, (u, v) => (u < 2 || u > 126 || v < 2 || v > 30 || Hash((int)(u * S), (int)(v * S), 271) < 0.05)
                    ? Rgb(90, 150, 60) : Mul(Rgb(80, 56, 38), 0.8 + 0.3 * Fbm(u * 0.6, v * 0.6, 272)), false);
            }
        }
        if (name == "candy_cane_pole" || name == "candy_canes")
        {
            // WHITE: candy stripes, red and white running round on a slant, glossy, a thin pale-red edge to each red band
            Zone(64, 84, 64, 32, (u, v) =>
            {
                double s = ((u * 0.5 + v) % 4.0 + 4.0) % 4.0;
                Color c = s < 1.5 ? Rgb(214, 20, 34) : s < 1.75 ? Rgb(240, 140, 150) : Rgb(250, 248, 246);
                return Mul(c, 0.9 + 0.12 * Math.Sin(u * 1.6));
            }, false);
            // ICE: snow, white with a blue shade and glints
            Zone(128, 116, 64, 32, (u, v) => Hash((int)(u * S), (int)(v * S), 190) < 0.04 ? Rgb(255, 255, 255)
                : Mix(Rgb(220, 232, 244), Rgb(250, 252, 255), Fbm(u * 0.6, v * 0.6, 191)), false);
            Zone(192, 32, 32, 12, (u, v) => Mul(Rgb(210, 24, 34), 0.85 + 0.25 * Math.Sin(u * 1.5)), false);
            Zone(0, 148, 64, 32, (u, v) => Mix(Rgb(20, 90, 40), Rgb(50, 140, 60), Fbm(u * 0.8, v * 0.8, 192)), false);
        }
        if (name == "present_pile")
        {
            // wrapping papers (DecorModels.PresentPile.PAPERS): red with white spots, blue with gold stars, green candy
            // stripes, white with blue snowflakes, gold foil, purple with silver dots, a red-and-green tartan
            Func<double, double, double, bool> dots = (u, v, size) => { double fu = (u % size) - size / 2, fv = (v % size) - size / 2; return fu * fu + fv * fv < size * size * 0.06; };
            Zone(0, 116, 128, 32, (u, v) => dots(u + ((int)(v / 3) % 2) * 1.5, v, 3) ? Rgb(250, 246, 240) : Mul(Rgb(200, 24, 34), 0.92 + 0.1 * Fbm(u, v, 180)), false);
            Zone(192, 116, 64, 32, (u, v) =>
            {
                double fu = (u % 4) - 2, fv = (v % 4) - 2;
                double r = Math.Sqrt(fu * fu + fv * fv), a = Math.Atan2(fv, fu);
                bool star = r < 0.9 * (0.45 + 0.55 * Math.Pow(Math.Abs(Math.Cos(2.5 * a)), 3)) && ((int)(u / 4) + (int)(v / 4)) % 2 == 0;
                return star ? Rgb(246, 200, 70) : Mul(Rgb(30, 60, 170), 0.92 + 0.1 * Fbm(u, v, 181));
            }, false);
            Zone(0, 84, 64, 32, (u, v) => ((u + v) % 3.0) < 1.2 ? Rgb(200, 30, 36) : (((u + v) % 3.0) < 1.5 ? Rgb(250, 246, 240) : Rgb(30, 140, 60)), false);
            Zone(64, 84, 64, 32, (u, v) =>
            {
                double fu = (u % 5) - 2.5, fv = (v % 5) - 2.5;
                bool flake = (Math.Abs(fu) < 0.18 || Math.Abs(fv) < 0.18 || Math.Abs(fu - fv) < 0.22 || Math.Abs(fu + fv) < 0.22) && fu * fu + fv * fv < 2.0;
                return flake ? Rgb(90, 150, 230) : Rgb(246, 248, 252);
            }, false);
            Zone(128, 84, 64, 32, (u, v) => Mix(Rgb(200, 150, 40), Rgb(255, 226, 110), 0.5 + 0.5 * Math.Sin(v * 1.9 + u * 0.7)), false);
            Zone(128, 148, 64, 32, (u, v) => dots(u, v, 2.5) ? Rgb(220, 224, 236) : Mul(Rgb(120, 50, 170), 0.92 + 0.1 * Fbm(u, v, 182)), false);
            Zone(0, 148, 64, 32, (u, v) =>
            {
                bool a = (int)(u / 3) % 2 == 0, b = (int)(v / 3) % 2 == 0;
                Color c = a && b ? Rgb(170, 20, 30) : a || b ? Rgb(120, 30, 34) : Rgb(30, 90, 50);
                if ((u % 3) < 0.3 || (v % 3) < 0.3) c = Rgb(236, 200, 80);
                return c;
            }, false);
            // ribbons: gold (default), red, silver satin
            Zone(192, 32, 32, 12, (u, v) => Mul(Rgb(210, 26, 36), 0.85 + 0.25 * Math.Sin(u * 1.5)), false);
            Zone(128, 116, 64, 32, (u, v) => Mix(Rgb(190, 196, 210), Rgb(250, 252, 255), 0.5 + 0.5 * Math.Sin(u * 1.5 + v * 0.4)), false);
        }
        if (name == "leaf_pile")
        {
            // DECK: a heap of leaves, each a patch of red, orange, gold or brown with a darker vein
            Zone(0, 116, 128, 32, (u, v) =>
            {
                Color[] leaves = { Rgb(196, 52, 26), Rgb(232, 120, 30), Rgb(238, 182, 40), Rgb(140, 78, 30), Rgb(214, 86, 24) };
                int cu = (int)Math.Floor(u / 1.6 + 0.5 * Math.Sin(v * 0.7)), cv = (int)Math.Floor(v / 1.3);
                Color c = leaves[(int)(Hash(cu, cv, 160) * leaves.Length) % leaves.Length];
                double fu = (u / 1.6) % 1.0, fv = (v / 1.3) % 1.0;
                if (Math.Abs(fu - fv) < 0.08) c = Mul(c, 0.7);
                return Mul(c, 0.85 + 0.25 * Fbm(u * 0.5, v * 0.5, 161));
            }, false);
            Zone(192, 32, 32, 12, (u, v) => Mul(Rgb(196, 48, 24), 0.9 + 0.1 * Fbm(u, v, 162)), false);
            Zone(96, 68, 32, 16, (u, v) => Mul(Rgb(232, 118, 30), 0.9 + 0.1 * Fbm(u, v, 163)), false);
            Zone(128, 44, 40, 40, (u, v) => Mul(Rgb(236, 180, 40), 0.9 + 0.1 * Fbm(u, v, 164)), false);
            Zone(192, 84, 64, 32, (u, v) => Mul(Rgb(130, 76, 34), 0.9 + 0.1 * Fbm(u, v, 165)), false);
        }
    }

    // a square sail's picture block (the same scheme as the pirate ship's: PirateShipModel.sail)
    static void Sail(int U, int V, int D, int H, Func<double, double, Color> design)
    {
        double L = D / 3.0;
        for (int y = V * S; y < (V + H) * S; y++)
            for (int x = U * S; x < (U + 2 * D) * S; x++)
            {
                double c = (x - U * S + 0.5) / S, s = (y - V * S + 0.5) / S;
                int k = (int)Math.Floor(c / (2 * L));
                double within = c - 2 * L * k;
                bool back = within < L;
                double zmin = k == 0 ? 0 : k == 1 ? -L / 2 : -L, zmax = zmin + L, off = k == 0 ? L / 2 : k == 1 ? 0 : -L / 2;
                double local = back ? zmax - within : zmin + (within - L);
                img[x, y] = design(local + off, s);
            }
    }

    // â”€â”€ the hull-side pictures â”€â”€
    static Color SpeedSide(double x, double y)
    {
        if (y >= 0.3) return Mul(Rgb(170, 30, 28), 0.9 + 0.1 * Fbm(x, y, 50));
        if (y >= -0.2) return Rgb(20, 20, 24);
        Color c = Glossy(Rgb(246, 248, 252), x, y, 51);
        double t = Math.Max(0, Math.Min(1, (x + 18) / 40.0));
        double yc = -1.6 - 3.0 * Math.Pow(t, 1.4);
        if (Math.Abs(y - yc) < 0.6) c = Rgb(196, 24, 30);
        if (Math.Abs(y - (yc - 0.85)) < 0.13) c = Rgb(236, 190, 70);
        if (Math.Abs(y - (yc + 0.85)) < 0.13) c = Rgb(20, 20, 24);
        if (x > -16 && x < -8 && y > -4.6 && y < -2.4 && ((x - y * 0.8 + 100) % 2.2) < 0.7) c = Rgb(196, 24, 30);   // speed slashes
        // a gold lightning bolt near the bow
        double bx = x - 11, by = y + 3.6;
        bool bolt = (by > -1.6 && by < 0 && Math.Abs(bx + by * 0.9 - 0.6) < 0.35) || (by >= 0 && by < 1.6 && Math.Abs(bx + by * 0.9 + 0.2) < 0.35)
            || (Math.Abs(by) < 0.2 && bx > -0.5 && bx < 0.9);
        if (bolt) c = Rgb(250, 205, 70);
        return c;
    }
    static Color YachtSide(double x, double y)
    {
        if (y >= 0.8) return Mul(Rgb(30, 30, 60), 0.9 + 0.1 * Fbm(x, y, 52));
        if (y >= 0.5) return Rgb(240, 240, 244);
        if (y >= -2.95) return Glossy(Rgb(20, 32, 78), x, y, 53);
        if (y >= -3.2) return Rgb(236, 190, 80);
        Color c = Glossy(Rgb(246, 248, 252), x, y, 54);
        if (YachtPort(x, y)) c = Rgb(30, 44, 56);
        if (y > -5.3 && y < -4.4 && x > -12 && x < 4) c = Rgb(26, 40, 52);             // the owner's suite windows
        if (Math.Abs(y + 6.0) < 0.62 && ((x + 300) % 3.0) < 1.2 && x > -26 && x < 22 && !YachtPort(x, y)) c = Rgb(200, 204, 214);
        return c;
    }
    static bool YachtPort(double x, double y) { double fx = ((x + 300) % 3.0) - 0.6; return x > -26 && x < 22 && InEllipse(fx, y + 6.0, 0, 0, 0.42, 0.42); }
    static bool YachtGlow(double x, double y) { return YachtPort(x, y) || (y > -5.3 && y < -4.4 && x > -12 && x < 4); }
    static Color LongSide(double x, double y)
    {
        if (y >= 0.6) return Mul(Rgb(30, 24, 20), 0.85 + 0.25 * Fbm(x * 0.5, y, 55));    // tarred
        double strake = Math.Floor((y + 20) / 1.5);
        double inS = (y + 20) / 1.5 - strake;
        Color c = Mul(Rgb(150, 104, 62), 0.85 + 0.2 * Hash((int)strake, (int)Math.Floor((x + 100) / 13), 56) + 0.1 * (Fbm(x * 0.3, y * 5, 57) - 0.5));
        if (inS > 0.82) c = Mul(c, 0.55);                                             // the overlap's shadow
        if (inS < 0.12) c = Mul(c, 1.15);
        if (inS > 0.6 && inS < 0.78 && ((x + 300) % 2.0) < 0.3) c = Rgb(60, 60, 64);   // rivets
        if (y < -3.2 && y > -4.0) c = ((Math.Floor(x / 1.5) % 2) == 0) ? Rgb(176, 32, 28) : Rgb(220, 180, 50);   // a painted band
        return c;
    }
    static Color PaddleSide(double x, double y)
    {
        if (y >= 0.4) return Mul(Rgb(26, 22, 22), 0.85 + 0.2 * Fbm(x, y, 58));
        Color c = Mul(Rgb(112, 26, 24), 0.9 + 0.15 * Fbm(x * 0.4, y * 0.4, 59));
        if (y > -4.6 && y < -3.9)
        {
            c = Mix(Rgb(170, 120, 40), Rgb(250, 210, 120), 0.5 + 0.5 * Math.Sin(y * 6));
            if (((x + 300) % 1.2) < 0.3) c = Rgb(110, 70, 30);
        }
        if (PaddlePort(x, y)) c = Rgb(255, 200, 110);
        else if (PaddleRing(x, y)) c = Rgb(220, 170, 70);
        if (((x + 300) % 6.0) < 0.12) c = Mul(c, 0.7);                                 // plate seams
        return c;
    }
    static bool PaddlePort(double x, double y) { double fx = ((x + 300) % 4.0) - 2; return Math.Abs(x) > 10 && x > -24 && x < 24 && InEllipse(fx, y + 2.4, 0, 0, 0.5, 0.5); }
    static bool PaddleRing(double x, double y) { double fx = ((x + 300) % 4.0) - 2; return Math.Abs(x) > 10 && x > -24 && x < 24 && InEllipse(fx, y + 2.4, 0, 0, 0.78, 0.78); }
    static Color RaftSide(double x, double y) { return Mix(Rgb(90, 84, 66), Rgb(50, 80, 40), 0.4 * Fbm(x * 0.3, y * 0.3, 60)); }
    // â”€â”€ item icons (32 x 32) â”€â”€
    static Color[,] icon = new Color[32, 32];
    static void IP(int x, int y, Color c) { if (x >= 0 && x < 32 && y >= 0 && y < 32) icon[x, y] = c; }
    static void IRect(int x0, int y0, int x1, int y1, Color c) { for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) IP(x, y, c); }
    static void Hull(int x0, int x1, int top, int bottom, Func<int, int, Color> col)
    {
        for (int x = x0; x <= x1; x++)
        {
            double t = (x - x0) / (double)(x1 - x0);
            int b = (int)Math.Round(bottom - 2 * Math.Pow(Math.Abs(t - 0.4) / 0.6, 2));
            int tp = top - (t > 0.85 ? (int)((t - 0.85) * 12) : 0);
            if (x >= x1 - 2) b = Math.Max(tp + 1, b - (x - (x1 - 2)) * 2);
            for (int y = tp; y <= b; y++) IP(x, y, col(x, y - tp));
            IP(x, b + 1, Rgb(12, 10, 10));
        }
    }
    static void Outline()
    {
        Color[,] copy = (Color[,])icon.Clone();
        for (int y = 1; y < 31; y++) for (int x = 1; x < 31; x++)
        {
            if (copy[x, y].A != 0) continue;
            if (copy[x - 1, y].A > 200 || copy[x + 1, y].A > 200 || copy[x, y - 1].A > 200 || copy[x, y + 1].A > 200) icon[x, y] = Color.FromArgb(150, 12, 12, 14);
        }
    }
    static void Icon(string boat)
    {
        for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++) icon[x, y] = Clear;
        Color white = Rgb(244, 246, 250), red = Rgb(190, 28, 30), navy = Rgb(24, 36, 84), gold = Rgb(240, 196, 70), chrome = Rgb(200, 206, 214);
        Color glass = Rgb(40, 70, 86), wood = Rgb(150, 104, 62), dark = Rgb(80, 52, 30), black = Rgb(24, 24, 28), brass = Rgb(220, 170, 70);
        if (BloodIcon(boat)) { Outline(); return; }
        switch (boat)
        {
            case "speedboat":
                Hull(3, 29, 18, 24, (x, d) => d >= 5 ? red : (d == 2 && x > 6) ? red : white);
                for (int x = 8; x < 26; x++) IP(x, 19 - (x - 8) / 9, red);
                IRect(18, 14, 21, 17, glass); IP(18, 13, chrome); IP(19, 13, chrome); IP(20, 13, chrome);
                IRect(9, 15, 12, 17, white); IRect(9, 13, 9, 15, white);
                IRect(1, 13, 3, 22, black); IRect(1, 22, 2, 25, black); IP(2, 12, chrome);
                IP(24, 17, gold); IP(25, 18, gold); IP(24, 19, gold);
                break;
            case "yacht":
                Hull(1, 30, 17, 25, (x, d) => d >= 6 ? navy : d == 3 ? gold : (d == 2 && x % 3 == 0 && x > 4 && x < 26) ? glass : white);
                IRect(7, 12, 22, 16, white); for (int x = 8; x < 22; x += 2) IP(x, 14, glass);
                IRect(10, 8, 19, 11, white); for (int x = 11; x < 19; x += 2) IP(x, 9, glass);
                IRect(12, 5, 16, 7, white); IRect(13, 3, 13, 5, chrome); IRect(14, 2, 15, 2, Rgb(60, 60, 60));
                IRect(23, 15, 26, 16, Rgb(80, 220, 240));
                IP(2, 12, chrome); IRect(1, 11, 3, 12, navy);
                break;
            case "longship":
                Hull(4, 27, 19, 24, (x, d) => d >= 4 ? Rgb(40, 30, 24) : (d % 2 == 0 ? wood : Mul(wood, 0.7)));
                for (int i = 0; i < 6; i++) { IP(28 + i / 2, 18 - i * 2, dark); IP(28 + i / 2, 19 - i * 2, dark); }
                IRect(30, 6, 32, 8, dark); IP(31, 7, Rgb(255, 120, 20));
                for (int i = 0; i < 5; i++) IP(3 - i / 2, 18 - i * 2, dark);
                IP(2, 9, dark); IP(3, 8, dark); IP(4, 9, dark);
                for (int x = 6; x < 26; x += 3) { IRect(x, 18, x + 1, 19, x % 2 == 0 ? red : gold); }
                IRect(15, 4, 15, 18, dark);
                for (int x = 9; x <= 21; x++) for (int y = 5; y <= 14; y++) IP(x, y, ((x - 9) / 2) % 2 == 0 ? red : white);
                break;
            case "paddlewheeler":
                Hull(3, 29, 19, 25, (x, d) => d >= 5 ? black : d == 1 ? brass : Rgb(112, 26, 24));
                for (int a = 0; a < 16; a++) { double ang = a * Math.PI / 8; IP(14 + (int)Math.Round(Math.Cos(ang) * 5), 20 + (int)Math.Round(Math.Sin(ang) * 5), a % 2 == 0 ? brass : dark); }
                IP(14, 20, brass);
                IRect(5, 12, 11, 18, dark); IP(7, 14, Rgb(255, 200, 110)); IP(9, 14, Rgb(255, 200, 110));
                IRect(6, 9, 10, 11, Rgb(255, 210, 120)); IRect(5, 8, 11, 8, dark);
                IRect(20, 4, 21, 18, black); IRect(23, 6, 24, 18, black); IRect(19, 3, 22, 3, brass); IRect(22, 5, 25, 5, brass);
                IP(20, 1, Rgb(200, 200, 200)); IP(21, 0, Rgb(220, 220, 220)); IP(24, 3, Rgb(200, 200, 200));
                break;
            case "swamp_raft":
                for (int r = 0; r < 4; r++) IRect(3, 20 + r * 2, 28, 21 + r * 2, r % 2 == 0 ? Rgb(96, 88, 70) : Rgb(80, 76, 58));
                for (int x = 6; x < 28; x += 9) IRect(x, 19, x + 1, 27, Rgb(60, 90, 40));
                IRect(4, 10, 4, 19, dark); IRect(14, 7, 14, 19, dark);
                for (int x = 3; x <= 16; x++) IP(x, 10 - (x - 3) / 4, Rgb(150, 140, 100));
                for (int x = 3; x <= 16; x++) IP(x, 11 - (x - 3) / 4, Rgb(120, 110, 80));
                IP(6, 12, Rgb(60, 100, 40)); IP(6, 13, Rgb(60, 100, 40)); IP(11, 11, Rgb(60, 100, 40)); IP(11, 12, Rgb(60, 100, 40)); IP(11, 13, Rgb(60, 100, 40));
                IRect(7, 16, 10, 19, wood); IP(12, 15, Rgb(255, 210, 120));
                IRect(25, 5, 25, 19, dark); IRect(24, 3, 27, 5, Rgb(232, 224, 200)); IP(23, 3, Rgb(232, 224, 200)); IP(28, 3, Rgb(232, 224, 200));
                IP(26, 9, Rgb(90, 255, 120)); IP(26, 10, Rgb(90, 255, 120));
                IP(2, 28, Rgb(80, 140, 50)); IP(29, 28, Rgb(80, 140, 50)); IP(30, 27, Rgb(80, 140, 50));
                break;
            case "steamboat":
                Hull(1, 30, 21, 26, (x, d) => d >= 4 ? Rgb(30, 26, 26) : d == 1 ? red : white);
                IRect(4, 16, 27, 20, white); for (int x = 5; x < 27; x += 2) IP(x, 18, Rgb(255, 210, 120));
                IRect(3, 15, 28, 15, white); IRect(6, 11, 25, 14, white); for (int x = 7; x < 25; x += 2) IP(x, 12, Rgb(255, 210, 120));
                IRect(5, 10, 26, 10, white); IRect(10, 7, 18, 9, white); IRect(12, 4, 16, 6, white); IP(14, 3, gold);
                IRect(20, 1, 21, 10, black); IRect(23, 2, 24, 10, black); IRect(19, 0, 22, 0, gold); IRect(22, 1, 25, 1, gold);
                for (int a = 0; a < 12; a++) { double ang = a * Math.PI / 6; IP(3 + (int)Math.Round(Math.Cos(ang) * 4), 23 + (int)Math.Round(Math.Sin(ang) * 4), red); }
                IP(3, 23, red);
                break;
            case "leviathan":
                Hull(4, 27, 21, 25, (x, d) => d >= 3 ? Rgb(30, 26, 24) : Rgb(62, 42, 26));
                for (int x = 6; x <= 24; x += 3) { for (int y = 10; y <= 21; y++) IP(x + (y < 14 ? (14 - y) / 3 : 0), y, Rgb(226, 218, 190)); }
                IRect(26, 17, 31, 21, Rgb(226, 218, 190)); IP(28, 18, Rgb(170, 60, 255)); IP(30, 21, Rgb(240, 240, 230));
                for (int i = 0; i < 4; i++) IP(3 - i, 20 - i, Rgb(226, 218, 190));
                IRect(15, 4, 15, 20, Rgb(226, 218, 190));
                for (int i = 0; i < 6; i++) { IP(15 - i, 9 - i / 2, Rgb(170, 110, 240)); IP(15 + i, 9 - i / 2, Rgb(170, 110, 240)); IP(15 - i / 2, 6 - i / 2, Rgb(200, 150, 255)); IP(15 + i / 2, 6 - i / 2, Rgb(200, 150, 255)); }
                IRect(17, 18, 18, 19, Rgb(120, 50, 200));
                IP(8, 9, Rgb(60, 170, 255)); IP(20, 9, Rgb(60, 170, 255));
                break;
            case "teapot":
                IRect(2, 26, 29, 27, Rgb(214, 200, 160)); IRect(1, 25, 30, 25, gold);
                for (int y = 12; y <= 24; y++) { int r = (int)Math.Round(8 * Math.Sin(Math.PI * (0.15 + 0.75 * (y - 12) / 12.0))) + 3; IRect(15 - r, y, 15 + r, y, white); }
                for (int x = 6; x < 25; x += 4) IP(x, 18, Rgb(52, 84, 170));
                IRect(4, 17, 26, 17, gold); IRect(9, 11, 21, 11, gold);
                for (int i = 0; i < 6; i++) IP(25 + i, 19 - i, white);
                for (int i = 0; i < 4; i++) { IP(3, 15 + i, white); IP(4, 14 + (i == 0 ? 0 : 5), white); }
                IRect(7, 6, 13, 7, white); IP(10, 5, gold);
                IRect(27, 6, 29, 9, Rgb(236, 238, 244)); IRect(25, 3, 28, 5, Rgb(236, 238, 244)); IRect(22, 1, 25, 3, Rgb(236, 238, 244));
                break;
            case "submersible":
                Hull(4, 26, 15, 24, (x, d) => d >= 7 ? Rgb(52, 120, 100) : Rgb(80, 170, 140));
                for (int x = 7; x < 22; x += 4) IP(x, 18, Rgb(255, 60, 40));
                IRect(10, 9, 16, 14, Rgb(80, 170, 140)); IRect(10, 8, 16, 8, gold);
                IRect(13, 2, 13, 8, brass); IRect(13, 2, 15, 3, brass);
                for (int y = 14; y <= 24; y++) for (int x = 23; x <= 30; x++) if ((x - 25) * (x - 25) + (y - 19) * (y - 19) < 26) IP(x, y, ((x + y) % 3 == 0) ? Rgb(170, 230, 230) : Rgb(60, 110, 120));
                IP(26, 20, Rgb(255, 60, 40)); IP(27, 19, Rgb(255, 60, 40));
                IRect(1, 15, 2, 23, wood); IRect(0, 18, 3, 19, wood);
                IP(5, 25, Rgb(170, 220, 255)); IP(4, 27, Rgb(170, 220, 255)); IP(6, 28, Rgb(170, 220, 255));
                break;
            case "castaway_tub":
                for (int y = 17; y <= 27; y++) { int r = 9 + (y > 19 && y < 26 ? 1 : 0); IRect(15 - r, y, 15 + r, y, ((y + 15 - r) % 3 == 0) ? Rgb(150, 100, 60) : Rgb(176, 120, 76)); }
                IRect(5, 19, 25, 19, Rgb(110, 90, 80)); IRect(5, 25, 25, 25, Rgb(110, 90, 80));
                IRect(7, 17, 23, 17, Rgb(60, 110, 130));
                IRect(12, 13, 17, 17, Rgb(120, 96, 60)); IRect(12, 13, 17, 13, Rgb(80, 110, 50));
                IRect(9, 3, 9, 16, Rgb(170, 180, 90)); IP(10, 8, Rgb(170, 180, 90));
                IRect(4, 4, 8, 8, Rgb(236, 234, 228)); IP(4, 9, Rgb(236, 234, 228)); IP(6, 9, Rgb(236, 234, 228));
                for (int i = 0; i < 6; i++) IP(23 + i, 17 + i, Rgb(150, 104, 62)); IRect(28, 22, 30, 24, Rgb(110, 76, 44));
                break;
            case "christmas_tree":
            {
                Color fir = Rgb(34, 100, 48), firD = Rgb(20, 66, 32);
                for (int y = 4; y < 27; y++)
                {
                    int half = (y - 3) * 11 / 23 + 1 - ((y - 4) % 6 < 2 ? 1 : 0);
                    for (int x = 16 - half; x <= 15 + half; x++) IP(x, y, (x + y) % 3 == 0 ? firD : fir);
                }
                IRect(14, 27, 17, 29, Rgb(90, 60, 36));
                IRect(10, 29, 21, 30, Rgb(170, 22, 30));
                int[][] dots = { new[] { 15, 9 }, new[] { 12, 14 }, new[] { 18, 13 }, new[] { 10, 20 }, new[] { 16, 19 }, new[] { 21, 22 }, new[] { 13, 24 }, new[] { 19, 25 } };
                Color[] dc = { red, gold, Rgb(60, 100, 230), Rgb(150, 60, 200), white, red, gold, Rgb(60, 220, 90) };
                for (int i = 0; i < dots.Length; i++) IP(dots[i][0], dots[i][1], dc[i]);
                for (int i = 0; i < 9; i++) IP(11 + i, 17 + (i % 3) - 1 + i / 3, gold);
                IP(15, 1, Rgb(255, 230, 90)); IRect(14, 2, 17, 3, Rgb(255, 210, 60)); IP(16, 1, Rgb(255, 230, 90)); IP(13, 2, Rgb(255, 230, 90)); IP(18, 2, Rgb(255, 230, 90));
                break;
            }
            case "present_pile":
            {
                IRect(2, 18, 15, 30, Rgb(200, 24, 34)); IRect(8, 18, 9, 30, gold); IRect(2, 23, 15, 24, gold);
                IRect(16, 20, 29, 30, Rgb(30, 60, 170)); IRect(22, 20, 23, 30, Rgb(220, 224, 236)); IRect(16, 25, 29, 26, Rgb(220, 224, 236));
                IRect(7, 8, 20, 17, Rgb(30, 140, 60)); IRect(13, 8, 14, 17, red); IRect(7, 12, 20, 13, red);
                IP(12, 7, red); IP(11, 6, red); IP(15, 7, red); IP(16, 6, red); IP(10, 5, red); IP(17, 5, red);
                IP(7, 17, gold); IP(6, 16, gold); IP(10, 16, gold);
                break;
            }
            case "candy_cane_pole":
            case "candy_canes":
            {
                Color cr = Rgb(214, 20, 34), cw = Rgb(250, 248, 246);
                int[][] canes = boat == "candy_cane_pole" ? new[] { new[] { 14, 6, 30 } } : new[] { new[] { 9, 8, 30 }, new[] { 15, 12, 30 }, new[] { 21, 15, 30 } };
                foreach (int[] cn in canes)
                {
                    int x = cn[0], top = cn[1], bottom = cn[2];
                    for (int y = top; y <= bottom; y++) { IP(x, y, ((y + x) / 2) % 2 == 0 ? cr : cw); IP(x + 1, y, ((y + x + 1) / 2) % 2 == 0 ? cr : cw); }
                    IP(x + 1, top - 1, cr); IP(x + 2, top - 2, cw); IP(x + 3, top - 2, cr); IP(x + 4, top - 1, cw); IP(x + 4, top, cr); IP(x + 4, top + 1, cw);
                }
                if (boat == "candy_canes") { IRect(8, 20, 23, 21, cr); IP(24, 19, cr); IP(25, 18, cr); IP(24, 22, cr); }
                else { IRect(13, 12, 16, 13, Rgb(30, 140, 60)); IP(17, 11, Rgb(30, 140, 60)); IP(18, 10, Rgb(30, 140, 60)); }
                IRect(6, 30, 25, 31, Rgb(236, 242, 250));
                break;
            }
            case "autumn_maple":
            case "autumn_birch":
            {
                bool m = boat == "autumn_maple";
                Color tr = m ? Rgb(78, 58, 44) : Rgb(236, 232, 222);
                IRect(14, 18, 17, 30, tr); if (!m) { IP(15, 21, black); IP(16, 25, black); IP(14, 28, black); }
                Color[] lc = m ? new[] { Rgb(196, 40, 24), Rgb(226, 90, 26), Rgb(240, 150, 30) } : new[] { Rgb(240, 196, 48), Rgb(232, 170, 30), Rgb(250, 214, 80) };
                for (int y = 1; y < 22; y++) for (int x = 2; x < 30; x++)
                    if (InEllipse(x, y, 16, 11, m ? 13 : 9, m ? 9.5 : 10.5)) IP(x, y, lc[(int)(Hash(x / 2, y / 2, 280) * 3) % 3]);
                IP(6, 30, lc[0]); IP(24, 29, lc[1]); IP(27, 31, lc[2]);
                break;
            }
            case "gravestone_round":
            case "gravestone_cross":
            case "gravestone_tomb":
            {
                Color st = Rgb(128, 128, 126), sd = Rgb(90, 90, 90);
                if (boat == "gravestone_round") { for (int y = 6; y < 27; y++) for (int x = 8; x < 24; x++) if (y > 12 || InEllipse(x, y, 15.5, 12, 7.5, 6.5)) IP(x, y, st);
                    IRect(11, 14, 11, 19, sd); IRect(12, 14, 13, 14, sd); IRect(12, 16, 13, 16, sd); IP(13, 15, sd); IP(13, 18, sd); IP(13, 19, sd); IRect(15, 14, 15, 19, sd); IRect(18, 14, 18, 19, sd); IRect(19, 14, 20, 14, sd); IRect(19, 16, 20, 16, sd); IP(20, 15, sd); }
                else if (boat == "gravestone_cross") { IRect(14, 3, 17, 26, st); IRect(8, 9, 23, 12, st); for (int a = 0; a < 16; a++) { double an = a * Math.PI / 8; IP(15 + (int)Math.Round(Math.Cos(an) * 5), 10 + (int)Math.Round(Math.Sin(an) * 5), st); } }
                else { for (int y = 4; y < 27; y++) { int half = y < 10 ? (y - 3) : 7; IRect(16 - half, y, 15 + half, y, st); } IRect(13, 12, 18, 15, Rgb(222, 212, 184)); IP(14, 13, black); IP(17, 13, black); IRect(24, 23, 25, 26, Rgb(236, 228, 206)); IP(24, 22, Rgb(255, 170, 40)); }
                IRect(6, 27, 25, 29, Rgb(96, 70, 48)); IP(8, 27, Rgb(78, 118, 52)); IP(19, 27, Rgb(78, 118, 52));
                break;
            }
            case "bone_pile":
            {
                Color b = Rgb(232, 224, 200);
                for (int i = 0; i < 8; i++) { int x0 = 4 + (i * 7) % 20, y0 = 18 + (i * 5) % 10; for (int t = 0; t < 7; t++) IP(x0 + t, y0 - ((i % 2 == 0) ? t / 2 : -t / 3), b); }
                IRect(12, 9, 19, 15, b); IRect(14, 16, 17, 17, b); IP(13, 11, black); IP(14, 11, black); IP(17, 11, black); IP(18, 11, black); IP(15, 14, black); IP(16, 14, black);
                break;
            }
            case "wishing_well":
            {
                Color stone = Rgb(140, 136, 126), mortar = Rgb(96, 92, 86);
                IRect(5, 21, 26, 29, stone);
                for (int x = 5; x <= 26; x += 4) IRect(x, 21, x, 29, mortar);
                IRect(5, 25, 26, 25, mortar);
                IRect(4, 19, 27, 20, Rgb(170, 166, 156));
                IRect(7, 20, 24, 21, Rgb(60, 130, 210));
                IRect(6, 6, 7, 20, Rgb(80, 52, 32)); IRect(24, 6, 25, 20, Rgb(80, 52, 32));
                IRect(8, 11, 23, 11, Rgb(150, 110, 70));
                IRect(15, 12, 15, 15, Rgb(190, 160, 110)); IRect(13, 15, 17, 18, Rgb(150, 110, 70));
                for (int y = 1; y < 8; y++) IRect(16 - (y * 2) - 1, y + 1, 15 + (y * 2) + 1, y + 1, (y % 2 == 0) ? Rgb(178, 72, 52) : Rgb(150, 58, 44));
                IRect(15, 1, 16, 1, Rgb(80, 52, 32));
                break;
            }
            case "mailbox":
            {
                Color mbPaint = Rgb(46, 72, 120), mbDark = Rgb(26, 40, 72), mbLight = Rgb(84, 112, 164);
                Color mbWood = Rgb(120, 84, 50), mbWoodDark = Rgb(80, 54, 32), mbRed = Rgb(204, 46, 46);
                IRect(14, 19, 17, 30, mbWood); IRect(14, 19, 14, 30, mbWoodDark); IRect(11, 29, 20, 30, mbWoodDark);
                IRect(8, 18, 23, 19, mbWoodDark);
                // the box, side on: round top, ribbed, the door at the right
                IRect(4, 10, 27, 17, mbPaint);
                IRect(5, 8, 26, 9, mbPaint); IRect(7, 7, 24, 7, mbPaint);
                IRect(7, 7, 24, 7, mbDark); IRect(5, 8, 6, 8, mbDark); IRect(25, 8, 26, 8, mbDark); IRect(4, 9, 4, 17, mbDark); IRect(4, 17, 27, 17, mbDark);
                for (int x = 8; x < 24; x += 4) IRect(x, 9, x, 16, mbLight);
                IRect(26, 8, 27, 16, mbDark); IRect(28, 11, 28, 12, Rgb(220, 180, 70));
                IRect(12, 12, 17, 13, Rgb(220, 180, 70));
                // the flag, up
                IRect(9, 2, 9, 11, mbRed); IRect(10, 2, 13, 4, mbRed); IRect(9, 11, 10, 11, mbDark);
                break;
            }
            case "town_mailbox":
            {
                Color tWhite = Rgb(238, 236, 230), tShade = Rgb(186, 184, 178), tGold = Rgb(232, 186, 70), tGoldDark = Rgb(160, 116, 30);
                IRect(14, 19, 17, 28, tWhite); IRect(14, 19, 14, 28, tShade); IRect(14, 22, 17, 22, tGold); IRect(14, 26, 17, 26, tGold);
                IRect(10, 28, 21, 30, tWhite); IRect(10, 30, 21, 30, tShade); IRect(12, 18, 19, 19, tShade);
                IRect(4, 10, 27, 17, tWhite);
                IRect(5, 8, 26, 9, tWhite); IRect(7, 7, 24, 7, tWhite);
                IRect(7, 7, 24, 7, tShade); IRect(4, 17, 27, 17, tShade); IRect(4, 9, 4, 17, tShade);
                IRect(6, 7, 7, 17, tGold); IRect(24, 7, 25, 17, tGold); IRect(9, 6, 22, 6, tGold);
                IRect(15, 3, 16, 5, tGold); IRect(14, 4, 17, 4, tGoldDark);
                IRect(11, 12, 20, 14, tGold); IRect(11, 14, 20, 14, tGoldDark);
                IRect(28, 11, 28, 12, tGold);
                break;
            }
            case "crimson_revenant":
            case "gorefang":
            case "sanguine_witch":
            case "blood_shard":
            {
                // a jagged crystal bsShard of blood: bsDark at its edges, bright red at its heart, a white glint
                Color bsDark = Rgb(90, 6, 12), bsMid = Rgb(176, 16, 26), bsLite = Rgb(240, 70, 70), bsShine = Rgb(255, 214, 214);
                int[][] bsShard = {
                    new[] {18, 3, 20, 3}, new[] {17, 4, 21, 4}, new[] {16, 5, 21, 5}, new[] {15, 6, 22, 6}, new[] {14, 7, 22, 7},
                    new[] {13, 8, 22, 8}, new[] {12, 9, 22, 9}, new[] {12, 10, 21, 10}, new[] {11, 11, 21, 11}, new[] {11, 12, 20, 12},
                    new[] {10, 13, 20, 13}, new[] {10, 14, 19, 14}, new[] {9, 15, 19, 15}, new[] {9, 16, 18, 16}, new[] {9, 17, 18, 17},
                    new[] {9, 18, 17, 18}, new[] {9, 19, 17, 19}, new[] {10, 20, 16, 20}, new[] {10, 21, 16, 21}, new[] {11, 22, 15, 22},
                    new[] {11, 23, 14, 23}, new[] {12, 24, 14, 24}, new[] {12, 25, 13, 25}, new[] {12, 26, 12, 26} };
                foreach (int[] bsRow in bsShard)
                {
                    IRect(bsRow[0], bsRow[1], bsRow[2], bsRow[3], bsMid);
                    IP(bsRow[0], bsRow[1], bsDark); IP(bsRow[2], bsRow[3], bsDark);
                    for (int x = bsRow[0] + 1; x < bsRow[0] + 1 + (bsRow[2] - bsRow[0]) / 3; x++) IP(x, bsRow[1], bsLite);
                }
                IRect(13, 9, 14, 13, bsShine); IP(15, 8, bsShine); IP(12, 15, bsLite); IP(11, 17, bsLite);
                IP(24, 6, bsShine); IP(25, 5, bsLite); IP(23, 5, bsLite); IP(24, 4, bsLite); IP(24, 7, bsLite);
                break;
            }
            case "fun_wishing_star":
            case "essential_wishing_star":
            case "changing_star":
            {
                Color inner = boat == "fun_wishing_star" ? Rgb(170, 230, 255) : boat == "essential_wishing_star" ? Rgb(255, 236, 140) : Rgb(236, 170, 255);
                Color outer = boat == "fun_wishing_star" ? Rgb(60, 150, 230) : boat == "essential_wishing_star" ? Rgb(230, 150, 30) : Rgb(150, 60, 210);
                Color edge = boat == "fun_wishing_star" ? Rgb(30, 80, 160) : boat == "essential_wishing_star" ? Rgb(150, 80, 10) : Rgb(80, 20, 130);
                // a five-pointed star
                for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++)
                {
                    double dx = x - 15.5, dy = y - 16.5;
                    double r = Math.Sqrt(dx * dx + dy * dy);
                    double a = Math.Atan2(dy, dx) + Math.PI / 2;
                    double k = Math.Abs(((a / (2 * Math.PI / 5)) % 1 + 1) % 1 - 0.5) * 2;   // 0 at a point, 1 between
                    double reach = 13.5 - k * 7.5;
                    if (r <= reach) IP(x, y, r > reach - 1.3 ? edge : Mix(inner, outer, Math.Min(1, r / reach)));
                }
                IRect(14, 13, 15, 14, Rgb(255, 255, 255));
                Color spark = boat == "fun_wishing_star" ? Rgb(255, 170, 220) : boat == "essential_wishing_star" ? Rgb(255, 255, 230) : Rgb(120, 230, 255);
                IP(3, 4, spark); IP(4, 3, spark); IP(4, 4, spark); IP(5, 4, spark); IP(4, 5, spark);
                IP(27, 26, spark); IP(28, 27, spark); IP(26, 27, spark); IP(27, 28, spark); IP(27, 27, spark);
                if (boat == "changing_star")
                {
                    // a swirl round it: change
                    for (int t = 0; t < 40; t++) { double a = t * 0.16; double r = 14.0 - t * 0.05; IP(16 + (int)(Math.Cos(a) * r), 16 + (int)(Math.Sin(a) * r), Rgb(255, 120, 210)); }
                }
                break;
            }
            case "fallen_leaves":
            {
                Color[] lc = { Rgb(196, 46, 26), Rgb(226, 96, 30), Rgb(238, 150, 34), Rgb(222, 184, 56), Rgb(150, 82, 40) };
                for (int i = 0; i < 14; i++)
                {
                    int cx = 5 + (int)(Hash(i, 1, 340) * 22), cy = 9 + (int)(Hash(i, 2, 340) * 16);
                    Color c = lc[i % lc.Length];
                    IRect(cx - 2, cy, cx + 2, cy, c); IRect(cx - 1, cy - 1, cx + 1, cy + 1, c); IP(cx, cy - 2, c); IP(cx, cy + 2, c);
                    IP(cx, cy, Mul(c, 0.7));
                }
                break;
            }
            case "ground_fog":
                for (int y = 10; y < 28; y++) for (int x = 1; x < 31; x++)
                {
                    double n = Fbm(x * 0.2, y * 0.35, 290);
                    if (n > 0.42) IP(x, y, Mix(Rgb(200, 206, 214), Rgb(240, 244, 248), (n - 0.42) * 2.5));
                }
                break;
            case "beach_towel":
                for (int y = 8; y < 26; y++) for (int x = 3; x < 29; x++) { int band = ((y - 8) / 3) % 3; IP(x, y, band == 0 ? Rgb(40, 190, 200) : band == 1 ? Rgb(250, 248, 240) : Rgb(250, 110, 90)); }
                IRect(3, 8, 6, 25, Rgb(30, 160, 170)); IRect(18, 14, 20, 15, black); IRect(22, 14, 24, 15, black); IP(21, 14, black);
                break;
            case "beach_ball":
            {
                Color[] sg = { Rgb(230, 40, 40), Rgb(250, 250, 250), Rgb(40, 110, 230), Rgb(250, 210, 40), Rgb(250, 250, 250), Rgb(50, 190, 80) };
                for (int y = 4; y < 28; y++) for (int x = 4; x < 28; x++) if (InEllipse(x, y, 15.5, 15.5, 11.5, 11.5)) { double an = Math.Atan2(y - 15.5, x - 15.5); IP(x, y, sg[(int)((an + Math.PI) / (2 * Math.PI) * 6) % 6]); }
                IRect(14, 14, 17, 17, white);
                break;
            }
            case "beach_umbrella":
                IRect(15, 9, 16, 30, white);
                for (int y = 2; y < 11; y++) for (int x = 2; x < 30; x++) if (InEllipse(x, y, 16, 11, 14, 9) && y < 11) IP(x, y, ((x - 2) / 4) % 2 == 0 ? Rgb(220, 40, 44) : white);
                IRect(15, 1, 16, 2, gold); IRect(11, 29, 20, 31, Rgb(236, 214, 160));
                break;
            case "beach_chair":
                for (int i = 0; i < 12; i++) { IP(8 + i / 2, 6 + i, Rgb(196, 150, 96)); IP(10 + i, 18 + i / 4, Rgb(196, 150, 96)); }
                for (int y = 7; y < 18; y++) for (int x = 10; x < 15; x++) IP(x + (y - 7) / 3, y, (x % 2 == 0) ? Rgb(40, 90, 200) : white);
                for (int x = 12; x < 23; x++) for (int y = 17; y < 20; y++) IP(x, y, (x % 2 == 0) ? Rgb(40, 90, 200) : white);
                IP(10, 27, Rgb(196, 150, 96)); IP(22, 27, Rgb(196, 150, 96)); IRect(10, 24, 10, 27, Rgb(196, 150, 96)); IRect(22, 21, 22, 27, Rgb(196, 150, 96));
                break;
            case "egg_basket":
            {
                Color wk = Rgb(206, 160, 96);
                IRect(5, 17, 26, 27, wk); for (int x = 5; x <= 26; x += 3) IRect(x, 17, x, 27, Rgb(176, 128, 72));
                for (int a = 0; a <= 20; a++) { double an = Math.PI * a / 20; IP(16 + (int)Math.Round(Math.Cos(an) * 10), 17 - (int)Math.Round(Math.Sin(an) * 12), wk); }
                Color[] ec = { Rgb(248, 168, 200), Rgb(150, 200, 246), Rgb(160, 230, 200), Rgb(252, 230, 140) };
                for (int i = 0; i < 4; i++) IRect(8 + i * 4, 13, 10 + i * 4, 16, ec[i]);
                IRect(5, 16, 26, 16, Rgb(90, 160, 60));
                break;
            }
            case "giant_egg":
            {
                for (int y = 3; y < 28; y++) for (int x = 6; x < 26; x++)
                    if (InEllipse(x, y, 15.5, 16, 9, y < 16 ? 13 : 11)) { int band = ((y - 3) / 3) % 4; IP(x, y, band == 0 ? Rgb(248, 168, 200) : band == 1 ? Rgb(252, 230, 140) : band == 2 ? Rgb(150, 200, 246) : Rgb(160, 230, 200)); }
                IRect(4, 26, 27, 29, Rgb(220, 190, 110));
                break;
            }
            case "spring_bunny":
            {
                Color fur = Rgb(250, 246, 238), pink = Rgb(248, 168, 200);
                for (int y = 16; y < 29; y++) for (int x = 7; x < 22; x++) if (InEllipse(x, y, 14, 22.5, 7, 6.5)) IP(x, y, fur);
                for (int y = 8; y < 18; y++) for (int x = 15; x < 26; x++) if (InEllipse(x, y, 20, 13, 5, 4.6)) IP(x, y, fur);
                IRect(17, 1, 18, 9, fur); IRect(21, 1, 22, 9, fur); IRect(17, 2, 17, 7, pink); IRect(21, 2, 21, 7, pink);
                IP(22, 12, black); IP(24, 14, pink); IRect(6, 21, 7, 23, fur); IP(25, 19, Rgb(250, 210, 40)); IP(24, 20, Rgb(70, 160, 60));
                break;
            }
            case "tulip_bed":
            {
                IRect(2, 24, 29, 29, Rgb(80, 56, 38)); IRect(2, 24, 29, 24, Rgb(90, 150, 60));
                Color[] tc = { Rgb(220, 30, 40), Rgb(250, 210, 40), Rgb(150, 70, 200), Rgb(246, 140, 190), white };
                for (int i = 0; i < 7; i++) { int x = 4 + i * 4, top = 9 + (i * 5) % 6; IRect(x, top + 3, x, 23, Rgb(70, 160, 60)); IRect(x - 1, top, x + 1, top + 2, tc[i % 5]); IP(x - 1, top - 1, tc[i % 5]); IP(x + 1, top - 1, tc[i % 5]); }
                break;
            }
            case "christmas_frost":
            {
                for (int y = 4; y < 28; y++) for (int x = 2; x < 30; x++)
                {
                    double n = Fbm(x * 0.25, y * 0.25, 230);
                    if (n > 0.48 || Hash(x, y, 231) < 0.05) IP(x, y, Hash(x, y, 232) < 0.12 ? Rgb(255, 255, 255) : Mix(Rgb(190, 220, 246), Rgb(246, 250, 255), (n - 0.4) * 2));
                }
                IP(9, 9, Rgb(255, 255, 255)); IP(8, 9, Rgb(210, 236, 255)); IP(10, 9, Rgb(210, 236, 255)); IP(9, 8, Rgb(210, 236, 255)); IP(9, 10, Rgb(210, 236, 255));
                IP(22, 19, Rgb(255, 255, 255)); IP(21, 19, Rgb(210, 236, 255)); IP(23, 19, Rgb(210, 236, 255)); IP(22, 18, Rgb(210, 236, 255)); IP(22, 20, Rgb(210, 236, 255));
                break;
            }
            case "icicles":
            {
                IRect(2, 3, 29, 6, Rgb(240, 248, 255));
                int[][] ic = { new[] { 4, 14 }, new[] { 8, 22 }, new[] { 12, 11 }, new[] { 16, 27 }, new[] { 20, 16 }, new[] { 24, 21 }, new[] { 27, 12 } };
                foreach (int[] t in ic)
                    for (int y = 7; y < t[1]; y++)
                    {
                        int w = y < t[1] - (t[1] - 7) / 2 ? 1 : 0;
                        IP(t[0], y, Rgb(200, 230, 250)); if (w == 1) IP(t[0] + 1, y, Rgb(160, 200, 236));
                    }
                break;
            }
            case "christmas_present":
            {
                Color paper = Rgb(200, 24, 34), paperD = Rgb(160, 16, 26), dot = Rgb(250, 246, 240);
                IRect(5, 12, 26, 28, paper);
                for (int y = 12; y <= 28; y++) for (int x = 5; x <= 26; x++) if (((x + (y / 4) * 2) % 4 == 0) && y % 4 == 1) IP(x, y, dot);
                IRect(4, 10, 27, 13, paperD);
                IRect(14, 10, 17, 28, gold); IRect(4, 18, 27, 20, gold);
                IRect(9, 4, 14, 9, gold); IRect(17, 4, 22, 9, gold); IRect(11, 6, 12, 8, paper); IRect(19, 6, 20, 8, paper);
                IRect(14, 7, 17, 10, Rgb(255, 220, 100));
                break;
            }
            case "string_light_hook":
                IRect(14, 6, 17, 9, Rgb(50, 50, 56)); IRect(15, 10, 16, 18, Rgb(60, 60, 66));
                for (int i = 0; i < 5; i++) IP(16 + i / 2, 19 + i, Rgb(70, 70, 76));
                IP(18, 24, Rgb(70, 70, 76)); IP(17, 25, Rgb(70, 70, 76)); IP(15, 24, Rgb(70, 70, 76));
                break;
            case "string_lights":
            {
                Color wire = Rgb(28, 60, 28);
                Color[] bulb = { Rgb(255, 60, 40), Rgb(255, 200, 50), Rgb(60, 230, 90), Rgb(60, 140, 255), Rgb(255, 110, 210) };
                for (int x = 3; x < 29; x++)
                {
                    int y = 10 + (int)Math.Round(6 * Math.Sin((x - 3) / 26.0 * Math.PI));
                    IP(x, y, wire);
                    if ((x - 3) % 4 == 2) { IRect(x - 1, y + 1, x, y + 3, bulb[((x - 3) / 4) % 5]); }
                }
                for (int x = 6; x < 26; x++) { int y = 20 + (int)Math.Round(4 * Math.Sin((x - 6) / 20.0 * Math.PI)); IP(x, y, wire); if ((x - 6) % 4 == 1) IRect(x, y + 1, x + 1, y + 3, bulb[((x - 6) / 4 + 2) % 5]); }
                break;
            }
            case "spooky_scarecrow":
            {
                IRect(15, 12, 16, 30, Rgb(110, 76, 44)); IRect(4, 14, 27, 15, Rgb(110, 76, 44));
                IRect(10, 13, 21, 22, Rgb(84, 56, 92)); for (int x = 10; x <= 21; x += 2) IRect(x, 23, x, 24 + (x % 3), Rgb(84, 56, 92));
                IRect(3, 13, 5, 16, Rgb(230, 200, 110)); IRect(26, 13, 28, 16, Rgb(230, 200, 110));
                IRect(11, 5, 20, 12, Rgb(232, 118, 26)); IP(13, 7, Rgb(255, 230, 90)); IP(18, 7, Rgb(255, 230, 90)); IRect(13, 10, 18, 10, Rgb(255, 230, 90));
                IRect(9, 4, 22, 4, Rgb(24, 24, 28)); IRect(12, 1, 19, 3, Rgb(24, 24, 28)); IRect(14, 0, 16, 0, Rgb(24, 24, 28));
                IRect(23, 11, 25, 13, Rgb(20, 20, 24)); IP(26, 12, Rgb(230, 180, 50));
                break;
            }
            case "witchs_cauldron":
            {
                Color iron = Rgb(56, 56, 64), ironL = Rgb(90, 90, 100);
                for (int y = 12; y < 26; y++) { int half = (int)(11 * Math.Sqrt(1 - Math.Pow((y - 18.5) / 9.0, 2))); IRect(16 - half, y, 15 + half, y, y == 12 ? ironL : iron); }
                IRect(5, 11, 26, 12, ironL);
                IRect(7, 9, 24, 11, Rgb(90, 230, 80)); IP(11, 8, Rgb(200, 255, 170)); IP(19, 7, Rgb(200, 255, 170)); IP(15, 6, Rgb(170, 255, 140));
                IRect(9, 26, 22, 28, Rgb(255, 140, 30)); IP(12, 25, Rgb(255, 220, 80)); IP(19, 25, Rgb(255, 220, 80));
                for (int i = 0; i < 10; i++) IP(26 - i / 2, 2 + i * 2 / 1, Rgb(150, 100, 60)); IRect(25, 22, 28, 27, Rgb(210, 180, 100));
                break;
            }
            case "pumpkin_pile":
            {
                IRect(2, 14, 20, 22, Rgb(220, 186, 96));
                Color o = Rgb(234, 120, 28), od = Rgb(190, 90, 20);
                for (int y = 18; y < 30; y++) for (int x = 10; x < 28; x++) if (InEllipse(x, y, 19, 24, 9, 6)) IP(x, y, x % 3 == 0 ? od : o);
                for (int y = 10; y < 18; y++) for (int x = 4; x < 16; x++) if (InEllipse(x, y, 10, 14, 6, 4)) IP(x, y, x % 3 == 1 ? od : o);
                for (int y = 22; y < 30; y++) for (int x = 2; x < 11; x++) if (InEllipse(x, y, 6, 26, 4, 3.5)) IP(x, y, Rgb(240, 234, 216));
                IRect(18, 16, 19, 18, Rgb(90, 60, 30)); IRect(9, 8, 10, 10, Rgb(90, 60, 30));
                IP(28, 26, Rgb(60, 110, 50)); IRect(28, 27, 30, 29, Rgb(60, 110, 50));
                break;
            }
            case "leaf_pile":
            {
                Color[] lc = { Rgb(196, 52, 26), Rgb(232, 120, 30), Rgb(238, 182, 40), Rgb(140, 78, 30) };
                for (int y = 12; y < 28; y++) for (int x = 2; x < 30; x++)
                    if (InEllipse(x, y, 16, 27, 14, 14) && y < 28) IP(x, y, lc[(int)(Hash(x / 2, y / 2, 170) * 4) % 4]);
                IP(4, 29, lc[0]); IP(27, 29, lc[2]); IP(30, 27, lc[1]); IP(1, 26, lc[3]);
                for (int i = 0; i < 12; i++) IP(24 + i / 4, 3 + i * 2 / 1, Rgb(150, 104, 62));
                break;
            }
            case "fairy_boat":
            {
                Color pearl = Rgb(252, 246, 250), blush = Rgb(248, 196, 220), lilac = Rgb(186, 160, 236), green = Rgb(110, 170, 100);
                Color rose = Rgb(236, 96, 150), lav = Rgb(170, 130, 215), wingA = Rgb(214, 190, 255), wingB = Rgb(170, 230, 255);
                // the wings, spread up behind it
                // two wings each side, tilted up and out like a butterfly's, edged in gold
                double[][] wingsAt = { new double[] { 9.5, 8.5, 6.0, 3.6, -0.5 }, new double[] { 10.5, 14.5, 4.4, 2.6, 0.45 } };
                for (int y = 1; y < 20; y++) for (int x = 1; x < 31; x++)
                {
                    foreach (double[] wg in wingsAt)
                        for (int m = 0; m < 2; m++)
                        {
                            double cx = m == 0 ? wg[0] : 31 - wg[0], tilt = m == 0 ? wg[4] : -wg[4];
                            double dx = x - cx, dy = y - wg[1];
                            double rx = dx * Math.Cos(tilt) + dy * Math.Sin(tilt), ry = -dx * Math.Sin(tilt) + dy * Math.Cos(tilt);
                            double q = (rx / wg[2]) * (rx / wg[2]) + (ry / wg[3]) * (ry / wg[3]);
                            if (q > 1) continue;
                            IP(x, y, q > 0.62 ? Rgb(236, 200, 120) : Mix(Mix(Rgb(255, 180, 222), wingA, (y - 2) / 12.0), wingB, Math.Abs(rx) / wg[2] * 0.7));
                        }
                }
                // the hull, a leaf with its ends swept up
                for (int x = 3; x <= 28; x++)
                {
                    double t = (x - 3) / 25.0;
                    int top = 20 - (int)Math.Round(3 * Math.Pow(2 * t - 1, 2)), bot = 23 + (int)Math.Round(2 * Math.Sin(Math.PI * t));
                    for (int y = top; y <= bot; y++) IP(x, y, y - top < 1 ? Rgb(230, 190, 100) : y > bot - 1 ? lilac : (y - top < 2 ? pearl : blush));
                }
                // the bow curl and the bell flower, the lotus on its stem, flowers along the gunwale
                IP(29, 16, green); IP(30, 15, green); IP(30, 14, green); IP(29, 13, green); IP(28, 13, green); IP(28, 14, green);
                IP(30, 17, Rgb(220, 210, 255)); IP(30, 18, Rgb(250, 246, 255));
                IRect(18, 9, 18, 19, green);
                IP(16, 7, rose); IP(17, 6, Rgb(255, 220, 236)); IP(18, 5, rose); IP(19, 6, Rgb(255, 220, 236)); IP(20, 7, rose);
                IRect(17, 7, 19, 8, Rgb(255, 210, 120));
                for (int x = 5; x < 27; x += 2) IP(x, 18 + (x % 4 == 1 ? 1 : 0) - (int)Math.Round(3 * Math.Pow(2 * (x - 3) / 25.0 - 1, 2)), x % 3 == 0 ? rose : x % 3 == 1 ? lav : pearl);
                break;
            }
        }
        Outline();
    }

    static void Save(Color[,] px, int w, int h, string path, Func<int, int, bool> keep)
    {
        using (var bmp = new Bitmap(w, h, PixelFormat.Format32bppArgb))
        {
            var data = bmp.LockBits(new Rectangle(0, 0, w, h), ImageLockMode.WriteOnly, PixelFormat.Format32bppArgb);
            int[] buf = new int[w * h];
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) buf[y * w + x] = keep(x, y) ? px[x, y].ToArgb() : 0;
            System.Runtime.InteropServices.Marshal.Copy(buf, 0, data.Scan0, buf.Length);
            bmp.UnlockBits(data);
            bmp.Save(path, ImageFormat.Png);
        }
    }

    static Style Make(string name)
    {
        var s = new Style { Name = name, Leather = Rgb(242, 238, 228), Lining = Rgb(236, 226, 200), Accent = Rgb(240, 120, 30),
            Navy = Rgb(24, 36, 84), Wood = Rgb(150, 104, 62), DarkWood = Rgb(84, 52, 30), Canvas = Rgb(232, 222, 196),
            Steel = Rgb(80, 92, 90), Fiber = Rgb(246, 248, 252), Bottom = Rgb(150, 30, 28) };
        switch (name)
        {
            case "speedboat": s.Deck = 0; s.Accent = Rgb(196, 24, 30); s.Side = SpeedSide; break;
            case "yacht": s.Deck = 0; s.TintGlows = true; s.Bottom = Rgb(30, 30, 60); s.Side = YachtSide; s.SideGlow = YachtGlow; break;
            case "longship": s.Deck = 1; s.Fiber = Rgb(150, 104, 62); s.Bottom = Rgb(34, 26, 20); s.Side = LongSide; s.Canvas = Rgb(200, 180, 140); break;
            case "paddlewheeler": s.Deck = 2; s.TintGlows = true; s.Fiber = Rgb(112, 26, 24); s.Bottom = Rgb(26, 22, 22); s.DarkWood = Rgb(70, 40, 26);
                s.Side = PaddleSide; s.SideGlow = (x, y) => PaddlePort(x, y); break;
            case "swamp_raft": s.Deck = 3; s.Fiber = Rgb(96, 88, 70); s.Bottom = Rgb(40, 50, 30); s.Wood = Rgb(110, 90, 62); s.DarkWood = Rgb(62, 50, 36);
                s.Canvas = Rgb(150, 140, 100); s.Side = RaftSide; break;
            case "steamboat": s.Deck = 2; s.TintGlows = true; s.Bottom = Rgb(36, 28, 28); s.Accent = Rgb(186, 30, 32); s.DarkWood = Rgb(80, 50, 30);
                s.Side = SteamSide; break;
            case "leviathan": s.Deck = 5; s.Fiber = Rgb(60, 40, 24); s.Bottom = Rgb(30, 26, 24); s.DarkWood = Rgb(56, 36, 22); s.Side = LevSide; break;
            case "teapot": s.Deck = 0; s.Wood = Rgb(214, 200, 160); s.Leather = Rgb(176, 60, 80); s.Canvas = Rgb(246, 246, 250); s.Side = RaftSide; break;
            case "submersible": s.Deck = 4; s.Fiber = Rgb(70, 160, 130); s.Bottom = Rgb(52, 120, 100); s.Leather = Rgb(110, 66, 40); s.Wood = Rgb(150, 104, 62);
                s.Side = SubSide; s.SideGlow = (x, y) => SubPort(x, y); break;
            case "christmas_tree": s.Navy = Rgb(40, 70, 190); s.Accent = Rgb(150, 60, 200); s.DarkWood = Rgb(90, 60, 36); s.Side = RaftSide; break;
            case "spooky_scarecrow": s.Navy = Rgb(110, 50, 150); s.Side = RaftSide; break;
            case "witchs_cauldron": s.Side = RaftSide; break;
            case "pumpkin_pile": s.Side = RaftSide; break;
            case "leaf_pile": s.Side = RaftSide; break;
            case "present_pile": s.Side = RaftSide; break;
            case "candy_cane_pole": s.Accent = Rgb(30, 140, 60); s.Side = RaftSide; break;
            case "candy_canes": s.Accent = Rgb(30, 140, 60); s.Side = RaftSide; break;
            case "fairy_boat": s.Deck = 6; s.Fiber = Rgb(250, 246, 250); s.Bottom = Rgb(150, 130, 200); s.Leather = Rgb(236, 150, 180);
                s.Wood = Rgb(232, 226, 214); s.DarkWood = Rgb(196, 150, 160); s.Accent = Rgb(120, 175, 105); s.Navy = Rgb(170, 130, 215); s.Side = FairySide; break;
            case "castaway_tub": s.Deck = 1; s.Wood = Rgb(176, 120, 76); s.Canvas = Rgb(236, 234, 228); s.Side = RaftSide; break;
        }
        if (s.Side == null) s.Side = RaftSide;   // (decorations: no hull sides)
        return s;
    }

    // â”€â”€ each new boat's own materials, painted over the shared zones â”€â”€
    static void Extra(string name)
    {
        DecorExtra(name);
        if (name == "fairy_boat")
        {
            // ACCENT: soft sage stems and vines, fine streaks along them
            Zone(96, 68, 32, 16, (u, v) => Mul(Mix(Rgb(96, 150, 90), Rgb(150, 198, 122), Fbm(u * 0.4, v * 1.2, 105)), 0.92 + 0.1 * Math.Sin(u * 3.0)), false);
            // RED: rose petals; NAVY: wisteria, deep purple at the top of the zone to pale lilac at the bottom, florets
            Zone(192, 32, 32, 12, (u, v) => Mix(Rgb(232, 104, 150), Rgb(255, 196, 218), Fbm(u * 0.9, v * 0.9, 106)), false);
            Zone(64, 68, 32, 16, (u, v) =>
            {
                Color c = Mix(Rgb(112, 66, 168), Rgb(222, 200, 250), v / 16.0);
                double f = Smooth(u * 2.2, v * 2.2, 107);
                return f < 0.3 ? Mul(c, 0.82) : f > 0.75 ? Mix(c, Rgb(255, 255, 255), 0.25) : c;
            }, false);
            // BRASS: apricot petals; STEEL: periwinkle petals
            Zone(128, 44, 40, 40, (u, v) => Mix(Rgb(250, 168, 118), Rgb(255, 222, 184), Fbm(u * 0.9, v * 0.9, 108)), false);
            Zone(192, 116, 64, 32, (u, v) => Mix(Rgb(128, 140, 232), Rgb(196, 204, 255), Fbm(u * 0.9, v * 0.9, 109)), false);
            // ICE: glowing petals (the bell flower, the wing jewels): luminous lilac-white
            Zone(128, 116, 64, 32, (u, v) => Mix(Rgb(196, 178, 255), Rgb(250, 244, 255), Fbm(u * 0.8, v * 0.8, 110)), true);
            // fairy lights: soft pink, mint
            Zone(64, 52, 8, 8, (u, v) => Rgb(255, 150, 206), true);
            Zone(72, 52, 8, 8, (u, v) => Rgb(160, 255, 210), true);
            // FIBER: pearl with a faint pink and blue sheen
            Zone(0, 20, 64, 32, (u, v) => Mix(Rgb(252, 248, 252), Fbm(u * 0.2, v * 0.2, 111) > 0.5 ? Rgb(250, 226, 238) : Rgb(226, 232, 252), 0.3 * Fbm(u * 0.3, v * 0.3, 112)), false);
            // WOOD: silver birch, its dark marks
            Zone(128, 84, 64, 32, (u, v) =>
            {
                Color c = Mul(Rgb(234, 228, 216), 0.92 + 0.1 * Fbm(u * 0.3, v * 2.0, 113));
                if (Hash((int)(u * 1.3), (int)(v * 3.0), 114) < 0.12 && (v * 3.0 % 1.0) < 0.35) c = Rgb(70, 64, 60);
                return c;
            }, false);
        }
        if (name == "steamboat")
        {
            // ENGINE: the stacks and boilers, plain glossy black iron, faint seams round them (no stripes)
            Zone(168, 44, 40, 40, (u, v) =>
            {
                Color c = Glossy(Rgb(24, 24, 27), u, v, 12);
                if ((v % 6) < 0.18) c = Rgb(44, 44, 48);
                return c;
            }, false);
        }
        if (name == "leviathan")
        {
            // ACCENT: amethyst crystal (its bright facets glow a little); NAVY: crying obsidian, its tears glowing
            Zone(96, 68, 32, 16, (u, v) =>
            {
                double facet = Math.Abs(Math.Sin(u * 1.7 + v * 0.9)) * Math.Abs(Math.Cos(u * 0.6 - v * 1.3));
                return Mix(Rgb(90, 50, 150), Rgb(220, 170, 255), facet);
            }, false);
            for (int y = 68 * S; y < 84 * S; y++) for (int x = 96 * S; x < 128 * S; x++) glow[x, y] = img[x, y].R > 175;
            Zone(64, 68, 32, 16, (u, v) =>
            {
                Color c = Mul(Rgb(22, 10, 34), 0.8 + 0.4 * Fbm(u, v, 80));
                if (Fbm(u * 1.4, v * 1.4, 81) > 0.66) c = Rgb(170, 70, 255);
                return c;
            }, false);
            for (int y = 68 * S; y < 84 * S; y++) for (int x = 64 * S; x < 96 * S; x++) glow[x, y] = img[x, y].B > 200;
            // TINT: purple stained glass; GHOST: the skull's eyes, purple
            Zone(0, 52, 48, 32, (u, v) => (((u * 0.6 + v) % 6.0) < 0.6) ? Rgb(200, 150, 240) : Mul(Rgb(110, 60, 160), 0.85 + 0.2 * Fbm(u, v, 82)), false);
            for (int y = 52 * S; y < 84 * S; y++) for (int x = 0; x < 48 * S; x++) glow[x, y] = (x + y) % 9 == 0;
            Zone(80, 60, 16, 8, (u, v) => Mix(Rgb(170, 60, 255), Rgb(240, 200, 255), Fbm(u, v, 83)), true);
        }
        if (name == "teapot")
        {
            // FIBER: glazed porcelain, white with blue flowers; VINE: cobwebs (white strands); CANVAS: soft steam
            Zone(0, 20, 64, 32, (u, v) =>
            {
                Color c = Mix(Rgb(236, 238, 244), Rgb(255, 255, 255), 0.5 + 0.5 * Math.Sin(v * 0.8));
                double fx = (u % 8) - 4, fy = (v % 8) - 4;
                double r = Math.Sqrt(fx * fx + fy * fy);
                double petals = Math.Abs(Math.Cos(Math.Atan2(fy, fx) * 2.5));
                if (r < 1.6 * petals + 0.3 && r > 0.4) c = Rgb(52, 84, 170);
                if (r < 0.4) c = Rgb(236, 190, 80);
                if (Math.Abs(fy - Math.Sin(u * 0.8) * 1.2 - 3.0) < 0.12) c = Rgb(70, 110, 190);    // a twining vine
                return c;
            }, false);
            Zone(64, 148, 32, 32, (u, v) =>
            {
                double a = Math.Abs(Math.Sin((u + v) * 1.3)) < 0.08 || Math.Abs(Math.Sin((u - v) * 1.3)) < 0.08 ? 1 : 0;
                double ring = Math.Abs(Math.Sin(Math.Sqrt((u - 16) * (u - 16) + (v - 16) * (v - 16)) * 1.4)) < 0.1 ? 1 : 0;
                return (a + ring) > 0 ? Rgb(244, 246, 250) : Clear;
            }, false);
            Zone(0, 84, 64, 32, (u, v) =>
            {
                double d = Fbm(u * 0.3, v * 0.3, 84);
                return d < 0.36 ? Clear : Argb(255, 236 + 14 * d, 238 + 12 * d, 244 + 8 * d);
            }, false);
        }
        if (name == "submersible")
        {
            // The flashlight's beam (client/SubmersibleModel: BEAM_*): nothing in the texture, faint see-through light in
            // the glow. A panel of the cone's shell at (0,180), 2 x 112 long, 18 wide: a wedge from a point at the lamp to
            // its full width at the far end (so the panels meet edge to edge round the cone), brightest near the lamp
            // and fading to nothing, soft at its sides, a little murky.
            const double L = 112, W = 18;
            Color beamC = Rgb(255, 246, 222);
            for (int y = 180 * S; y < (int)((180 + W) * S); y++) for (int x = 0; x < (int)(2 * L) * S; x++)
            {
                double c = (x + 0.5) / S, s = (y - 180 * S + 0.5) / S;
                double a = (c % L) / L;
                double hw = W / 2 * a + 0.15;
                double t = Math.Abs(s - W / 2) / hw;
                img[x, y] = Clear;
                glow[x, y] = true;
                if (t >= 1) { glowColor[x, y] = Clear; continue; }
                double fade = Math.Pow(1 - a, 1.3) * Math.Min(1, Math.Pow(a * 3.5, 1.5));   // (faint by the lamp, where the panels meet)
                double soft = 1 - Math.Pow(t, 4);
                double murk = 0.7 + 0.6 * Fbm(c * 0.12, s * 0.35, 97);
                glowColor[x, y] = Argb(Math.Min(255, 255 * 0.3 * fade * soft * murk), beamC.R, beamC.G, beamC.B);
            }
            // the haze its rider sees down the beam (client/SubmersibleModel: HAZE): soft round glows from (0,200), each
            // twice side by side, faint in the middle and nothing at the edge
            double[][] hazes = { new double[] { 20, 0 }, new double[] { 36, 40 }, new double[] { 52, 112 } };
            foreach (double[] hz in hazes)
            {
                double w = hz[0], u0 = hz[1];
                for (int y = 200 * S; y < (int)((200 + w) * S); y++) for (int x = (int)(u0 * S); x < (int)((u0 + 2 * w) * S); x++)
                {
                    double c = (x + 0.5) / S - u0, s = (y - 200 * S + 0.5) / S;
                    double px = (c % w) - w / 2, py = s - w / 2;
                    double r = Math.Sqrt(px * px + py * py) / (w / 2);
                    img[x, y] = Clear;
                    glow[x, y] = true;
                    glowColor[x, y] = r >= 1 ? Clear : Argb(255 * 0.09 * Math.Pow(1 - r * r, 2.2) * (0.75 + 0.5 * Fbm(px * 0.3, py * 0.3, 98)), beamC.R, beamC.G, beamC.B);
                }
            }
            // TINT: the dome's clear glass: see-through, a thin pale edge and a streak or two
            Zone(0, 52, 48, 32, (u, v) =>
            {
                double fu = u % 2.0, fv = v % 2.0;
                if (fu < 0.12 || fv < 0.08) return Rgb(170, 230, 230);
                if (((u + v * 0.7) % 7.0) < 0.25) return Rgb(220, 250, 250);
                return Clear;
            }, false);
            // FIBER: waxed oxidized copper, verdigris with patches of old copper and rivets
            Zone(0, 20, 64, 32, (u, v) =>
            {
                Color c = Mix(Rgb(60, 150, 120), Rgb(110, 196, 166), Fbm(u * 0.4, v * 0.4, 85));
                if (Fbm(u * 0.7, v * 0.7, 86) > 0.7) c = Mix(c, Rgb(170, 100, 60), 0.6);
                if ((u % 4) < 0.35 && (v % 4) < 0.35) c = Rgb(40, 90, 76);
                return c;
            }, false);
        }
        if (name == "castaway_tub")
        {
            // ICE: murky bilge water; GREY: rusty iron hoops
            Zone(128, 116, 64, 32, (u, v) =>
            {
                Color c = Mix(Rgb(40, 90, 110), Rgb(90, 140, 140), Fbm(u * 0.5, v * 0.5, 87));
                if (Math.Abs(Math.Sin(u * 0.9 + Math.Sin(v * 0.7) * 2)) < 0.05) c = Rgb(170, 210, 210);
                return c;
            }, false);
            Zone(208, 44, 48, 40, (u, v) => Mix(Rgb(90, 80, 74), Rgb(150, 76, 36), Fbm(u * 0.8, v * 0.8, 88)), false);
            // the banner: a little tattered at the bottom
            Zone(0, 84, 64, 32, (u, v) => v > 5.4 + 0.5 * Hash((int)(u * 2), 3, 89) ? Clear : Mul(Rgb(236, 234, 228), 0.9 + 0.1 * Fbm(u, v, 90)), false);
        }
    }

    // â”€â”€ the new hull-side pictures â”€â”€
    static Color SteamSide(double x, double y)
    {
        if (y >= 0.5) return Rgb(30, 26, 26);
        Color c = Glossy(Rgb(246, 246, 248), x, y, 91);
        if (y > -3.0 && y < -1.9) c = Rgb(186, 30, 32);
        if (Math.Abs(y + 3.25) < 0.12) c = Rgb(236, 190, 80);
        if (((x + 300) % 3.0) < 0.1 && y < -1.9) c = Mul(c, 0.9);            // plank lines
        return c;
    }
    static Color LevSide(double x, double y)
    {
        if (y >= 0.6) return Mul(Rgb(30, 26, 24), 0.85 + 0.2 * Fbm(x, y, 92));
        Color c = Planks(Rgb(62, 42, 26), x + 100, y + 20, 1.5, 93, 0.55);
        if (((x + 300) % 6.0) < 0.8) c = Mul(Rgb(226, 218, 190), 0.85 + 0.2 * Fbm(x, y, 94));   // bone lashings
        if (y > -0.5 && Hash((int)(x * 3), (int)(y * 3), 95) < 0.08) c = Rgb(170, 170, 150);    // barnacles
        return c;
    }
    static Color SubSide(double x, double y)
    {
        Color c = Mix(Rgb(60, 150, 120), Rgb(110, 196, 166), Fbm(x * 0.4, y * 0.4, 96));
        if (((x + 300) % 5.0) < 0.15 || ((y + 300) % 3.0) < 0.1) c = Mul(c, 0.7);                // plate seams
        if (((x + 300) % 5.0) < 0.6 && ((y + 300) % 1.0) < 0.25) c = Rgb(40, 96, 80);            // rivets
        if (SubPort(x, y)) c = Rgb(255, 60, 40);
        else if (SubRing(x, y)) c = Rgb(220, 170, 70);
        return c;
    }
    static bool SubPort(double x, double y) { double fx = ((x + 300) % 5.0) - 2.5; return x > -20 && x < 2 && InEllipse(fx, y + 2.0, 0, 0, 0.6, 0.6); }
    static bool SubRing(double x, double y) { double fx = ((x + 300) % 5.0) - 2.5; return x > -20 && x < 2 && InEllipse(fx, y + 2.0, 0, 0, 0.95, 0.95); }

    public static string MakeAll(string entityDir, string itemDir)
    {
        foreach (string name in new string[] { "speedboat", "yacht", "longship", "paddlewheeler", "swamp_raft", "steamboat", "leviathan", "teapot",
            "submersible", "castaway_tub", "fairy_boat", "christmas_tree", "spooky_scarecrow", "witchs_cauldron", "pumpkin_pile", "leaf_pile", "present_pile", "candy_cane_pole", "candy_canes",
            "autumn_maple", "autumn_birch", "gravestone_round", "gravestone_cross", "gravestone_tomb", "bone_pile", "beach_towel", "beach_ball",
            "beach_umbrella", "beach_chair", "egg_basket", "giant_egg", "spring_bunny", "tulip_bed", "wishing_well", "mailbox", "town_mailbox", "crimson_revenant", "gorefang", "sanguine_witch",
            "blood_moon_lantern", "crimson_candelabra", "crimson_throne", "vampire_coffin", "blood_fountain", "blood_moon_banner", "crimson_rug",
            "revenant_greatsword_statue", "gorefang_skull_trophy", "sanguine_staff_pedestal", "bat_gargoyle",
            "grandfather_clock", "crimson_chandelier", "blood_pipe_organ", "secret_bookshelf_door", "haunted_portrait", "iron_maiden", "blood_moon_altar",
            "bat_roost", "crimson_canopy_bed", "blood_crystal_fireplace" })
        {
            img = new Color[N, N];
            glow = new bool[N, N];
            glowColor = new Color[N, N];
            Style st = Make(name);
            Common(st);
            Pictures(name);
            Extra(name);
            if (name == "swamp_raft")
            {
                // patch the tarp: browner squares, stitching
                for (int y = 84 * S; y < 116 * S; y++) for (int x = 0; x < 64 * S; x++)
                {
                    double u = (x + 0.5) / S, v = (y - 84 * S + 0.5) / S;
                    if (Hash((int)(u / 5), (int)(v / 5), 70) < 0.35) img[x, y] = Mix(img[x, y], Rgb(110, 84, 50), 0.55);
                    if (Hash((int)(u / 5), (int)(v / 5), 71) < 0.12) img[x, y] = Mix(img[x, y], Rgb(60, 80, 90), 0.5);
                }
            }
            Save(img, N, N, System.IO.Path.Combine(entityDir, name + ".png"), (x, y) => true);
            Color[,] lit = new Color[N, N];
            for (int y = 0; y < N; y++) for (int x = 0; x < N; x++) lit[x, y] = glowColor[x, y].A > 0 ? glowColor[x, y] : img[x, y];
            Save(lit, N, N, System.IO.Path.Combine(entityDir, name + "_glow.png"), (x, y) => glow[x, y]);
            Icon(name);
            Save(icon, 32, 32, System.IO.Path.Combine(itemDir, name + ".png"), (x, y) => true);
        }
        // Christmas Frost and Icicles (decor/, block models): their block textures (textures/block)
        string blockDir = System.IO.Path.Combine(System.IO.Directory.GetParent(entityDir).FullName, "block");
        for (int variant = 0; variant < 2; variant++)
        {
            // sparse frost: patches of white and icy blue with darker crystal veins and glints, clear gaps between
            Color[,] frost = new Color[32, 32];
            int seed = 200 + variant * 7;
            for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++)
            {
                double u = x / 2.0, v = y / 2.0;
                double n = Fbm(u * 0.35, v * 0.35, seed) * 0.75 + 0.25 * Fbm(u * 1.1, v * 1.1, seed + 1);
                bool speck = Hash(x, y, seed + 2) < 0.05;
                if (n < 0.5 && !speck) { frost[x, y] = Clear; continue; }
                Color c = Mix(Rgb(196, 222, 246), Rgb(246, 250, 255), Math.Min(1, (n - 0.5) * 3.5));
                if (Math.Abs(((u * 0.9 + v * 0.6 + 40) % 2.2) - 1.1) < 0.12 && n > 0.55) c = Rgb(150, 192, 232);   // crystal veins
                if (Hash(x, y, seed + 3) < 0.06) c = Rgb(255, 255, 255);                                          // glints
                frost[x, y] = c;
            }
            Save(frost, 32, 32, System.IO.Path.Combine(blockDir, variant == 0 ? "christmas_frost.png" : "christmas_frost_b.png"), (x, y) => true);
        }
        // the sparkle layer just above: a few tiny star crystals
        Color[,] top = new Color[32, 32];
        for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++) top[x, y] = Clear;
        for (int i = 0; i < 26; i++)
        {
            int cx = (int)(Hash(i, 1, 210) * 30) + 1, cy = (int)(Hash(i, 2, 210) * 30) + 1;
            Color c = i % 3 == 0 ? Rgb(200, 236, 255) : Rgb(255, 255, 255);
            top[cx, cy] = c;
            if (i % 2 == 0) { top[cx - 1, cy] = Rgb(220, 238, 252); top[cx + 1, cy] = Rgb(220, 238, 252); top[cx, cy - 1] = Rgb(220, 238, 252); top[cx, cy + 1] = Rgb(220, 238, 252); }
        }
        Save(top, 32, 32, System.IO.Path.Combine(blockDir, "christmas_frost_top.png"), (x, y) => true);
        // Fallen Leaves (decor/, a block model like the frost): a carpet of fallen autumn leaves, each a little pointed
        // leaf with a darker midrib, red, orange, gold, brown, the ground showing through here and there; and a sparser
        // layer of loose leaves lying just above it so the carpet has some depth
        Color[] autumn = { Rgb(196, 46, 26), Rgb(226, 96, 30), Rgb(238, 150, 34), Rgb(222, 184, 56), Rgb(150, 82, 40), Rgb(176, 36, 30), Rgb(208, 122, 40) };
        Action<Color[,], int, int, int> leaves = (img, count, seed, size) =>
        {
            for (int i = 0; i < count; i++)
            {
                double cx = Hash(i, 1, seed) * 32, cy = Hash(i, 2, seed) * 32, an = Hash(i, 3, seed) * Math.PI;
                double len = size * (0.8 + 0.5 * Hash(i, 4, seed)), wid = len * 0.45;
                Color c = Mul(autumn[(int)(Hash(i, 5, seed) * autumn.Length) % autumn.Length], 0.85 + 0.25 * Hash(i, 6, seed));
                double ca = Math.Cos(an), sa = Math.Sin(an);
                for (int dy = -6; dy <= 6; dy++) for (int dx = -6; dx <= 6; dx++)
                {
                    double a = dx * ca + dy * sa, b = -dx * sa + dy * ca;
                    double t = a / len;                                  // -1 .. 1 along the leaf
                    if (Math.Abs(t) > 1) continue;
                    if (Math.Abs(b) > wid * (1 - t * t) + 0.2) continue;  // pointed at both ends
                    int px = ((int)Math.Floor(cx + dx) % 32 + 32) % 32, py = ((int)Math.Floor(cy + dy) % 32 + 32) % 32;   // tiles
                    Color p = Math.Abs(b) < 0.5 && t > -0.9 ? Mul(c, 0.72) : Mul(c, 0.95 + 0.1 * Hash(px, py, seed + 9));
                    img[px, py] = p;
                }
                // a little stalk
                int sx = ((int)Math.Floor(cx - ca * (len + 1)) % 32 + 32) % 32, sy = ((int)Math.Floor(cy - sa * (len + 1)) % 32 + 32) % 32;
                img[sx, sy] = Rgb(100, 64, 36);
            }
        };
        for (int variant = 0; variant < 2; variant++)
        {
            Color[,] carpet = new Color[32, 32];
            for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++) carpet[x, y] = Clear;
            leaves(carpet, 85, 300 + variant * 11, 3);
            Save(carpet, 32, 32, System.IO.Path.Combine(blockDir, variant == 0 ? "fallen_leaves.png" : "fallen_leaves_b.png"), (x, y) => true);
        }
        Color[,] loose = new Color[32, 32];
        for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++) loose[x, y] = Clear;
        leaves(loose, 14, 330, 3);
        Save(loose, 32, 32, System.IO.Path.Combine(blockDir, "fallen_leaves_top.png"), (x, y) => true);
        // ice: see-through pale blue, brighter streaks running down, a darker edge
        Color[,] ice = new Color[16, 16];
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++)
        {
            double streak = Math.Abs(Math.Sin(x * 1.3 + Fbm(x * 0.3, y * 0.15, 220) * 3));
            Color c = Mix(Rgb(170, 214, 245), Rgb(236, 248, 255), streak * 0.7 + y / 40.0);
            double alpha = 150 + 70 * streak;
            if (x % 4 == 0) { c = Mul(c, 0.85); alpha = 210; }
            if (Hash(x, y, 221) < 0.08) { c = Rgb(255, 255, 255); alpha = 235; }
            ice[x, y] = Argb(alpha, c.R, c.G, c.B);
        }
        Save(ice, 16, 16, System.IO.Path.Combine(blockDir, "icicle.png"), (x, y) => true);
        // the soft round glow round each string-light bulb (client/DecorRenderer): white, fading out to nothing
        Color[,] halo = new Color[64, 64];
        for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++)
        {
            double r = Math.Sqrt((x - 31.5) * (x - 31.5) + (y - 31.5) * (y - 31.5)) / 32.0;
            halo[x, y] = r >= 1 ? Clear : Argb(255 * Math.Pow(1 - r, 2.2), 255, 255, 255);
        }
        Save(halo, 64, 64, System.IO.Path.Combine(entityDir, "light_halo.png"), (x, y) => true);
        // a puff of dry ice fog (the Ground Fog): a soft white cloud, thick in the middle, wispy and billowy at its edge
        Color[,] puff = new Color[64, 64];
        for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++)
        {
            double dx = (x - 31.5) / 32.0, dy = (y - 31.5) / 32.0;
            double r = Math.Sqrt(dx * dx + dy * dy);
            double billow = Fbm(x * 0.09, y * 0.09, 350) - 0.5;
            double edge = r + billow * 0.45;
            double a = edge >= 1 ? 0 : Math.Pow(1 - edge, 1.6);
            a *= 0.8 + 0.4 * Fbm(x * 0.16, y * 0.16, 351);
            int shade = (int)(238 + 17 * Fbm(x * 0.12, y * 0.12, 352));
            puff[x, y] = Argb(Math.Min(255, 255 * a), Math.Min(255, shade), Math.Min(255, shade + 2), 255);
        }
        Save(puff, 64, 64, System.IO.Path.Combine(entityDir, "fog_puff.png"), (x, y) => true);
        foreach (string name in new string[] { "string_light_hook", "string_lights", "christmas_present", "christmas_frost", "icicles", "ground_fog", "fallen_leaves",
            "fun_wishing_star", "essential_wishing_star", "changing_star", "blood_shard", "bottle_of_blood" })
        {
            Icon(name);
            Save(icon, 32, 32, System.IO.Path.Combine(itemDir, name + ".png"), (x, y) => true);
        }
        return "saved";
    }
}
