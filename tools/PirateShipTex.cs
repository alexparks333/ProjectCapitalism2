// Paints the Pirate Ship's textures (see client/PirateShipModel: the ZONES there and here must match).
//   entity/pirate_ship.png       1024 x 1024 (4 texels a model pixel, for the model's 256 x 256 layout)
//   entity/pirate_ship_glow.png  the same, only the glowing bits (lantern glass, lit windows)
//   item/pirate_ship.png         32 x 32 icon
// Used by tools/make_boats.ps1 (it also paints the Spectral Galleon: MakeGhost).
using System;
using System.Drawing;
using System.Drawing.Imaging;

public static class PirateShipTex
{
    const int S = 4;          // texels per model pixel
    const int N = 256 * S;
    static Color[,] img = new Color[N, N];
    static bool[,] glow = new bool[N, N];

    // ── hash noise ──
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
    static Color Mul(Color c, double k) { return Rgb(c.R * k, c.G * k, c.B * k); }
    static Color Mix(Color a, Color b, double t) { return Color.FromArgb(C(a.A + (b.A - a.A) * t), C(a.R + (b.R - a.R) * t), C(a.G + (b.G - a.G) * t), C(a.B + (b.B - a.B) * t)); }
    static readonly Color Clear = Color.FromArgb(0, 0, 0, 0);

    // run f(u, v) for every texel of a zone (u, v in model pixels, relative to the zone)
    delegate Color Painter(double u, double v);
    static void Zone(int zu, int zv, int zw, int zh, Painter f)
    {
        for (int y = zv * S; y < (zv + zh) * S; y++)
            for (int x = zu * S; x < (zu + zw) * S; x++)
                img[x, y] = f((x - zu * S + 0.5) / S, (y - zv * S + 0.5) / S);
    }

    // planks: horizontal boards `size` tall, rows lined up with `row0`; per-board tint, dark seams, grain
    static Color Planks(Color baseC, double u, double v, double size, int seed, double seamDark)
    {
        int board = (int)Math.Floor(v / size);
        double inBoard = v / size - board;
        double tint = 0.88 + 0.24 * Hash(board, 0, seed);
        // butt joints: each board in lengths of 10 to 16
        int len = 10 + (int)(Hash(board, 1, seed) * 7);
        double off = Hash(board, 2, seed) * len;
        double along = (u + off) % len;
        int piece = (int)Math.Floor((u + off) / len);
        tint *= 0.93 + 0.14 * Hash(board, piece + 3, seed);
        double grain = Fbm(u * 0.35, v * 7.0, seed + board) - 0.5;
        Color c = Mul(baseC, tint + grain * 0.22);
        if (inBoard < 0.5 / S * size || inBoard > 1 - 0.3 / S * size) c = Mul(c, seamDark);
        if (along < 0.25) c = Mul(c, seamDark + 0.15);
        // a knot now and then
        double kx = Hash(board, piece + 50, seed) * len;
        if (Hash(board, piece + 60, seed) < 0.25 && Math.Abs(along - kx) < 0.45 && Math.Abs(inBoard - 0.5) < 0.22) c = Mul(c, 0.7);
        return c;
    }

    static void Paint()
    {
        for (int y = 0; y < N; y++) for (int x = 0; x < N; x++) img[x, y] = Rgb(60, 40, 26);

        // DARK (0,0,96,40): black hull planks; rows: y = row - 24; the waterline band (y -1..2) is dark red
        Zone(0, 0, 96, 40, (u, v) =>
        {
            double y = v - 24;
            bool red = y >= -1 && y < 2;
            Color b = red ? Rgb(118, 26, 22) : Rgb(34, 31, 34);
            Color c = Planks(b, u, v, 2.0, red ? 11 : 10, 0.45);
            // salt and wear: a few lighter scuffs on the black
            if (!red && Fbm(u * 0.6, v * 2.5, 99) > 0.72) c = Mix(c, Rgb(78, 74, 76), 0.35);
            return c;
        });
        // WOOD (96,0,96,40): brown planks; rows: y = row - 20 (below y 6 the weed and barnacles start)
        Zone(96, 0, 96, 40, (u, v) =>
        {
            double y = v - 20;
            Color c = Planks(Rgb(104, 68, 40), u, v, 2.0, 20, 0.5);
            if (y > 5.5)
            {
                double t = Math.Min(1, (y - 5.5) / 3.0) * (0.45 + 0.4 * Fbm(u * 0.8, v * 0.8, 21));
                c = Mix(c, Rgb(58, 82, 44), t);
                if (Hash((int)(u * S), (int)(v * S), 22) < 0.03 * t) c = Rgb(190, 186, 170);
            }
            return c;
        });
        // GOLD (192,0,64,24): polished gold with a sheen and engraved lines
        Zone(192, 0, 64, 24, (u, v) =>
        {
            double sheen = 0.5 + 0.5 * Math.Sin(v * 2.9 + u * 0.35);
            Color c = Mix(Rgb(150, 104, 28), Rgb(255, 214, 92), 0.25 + 0.75 * sheen);
            c = Mul(c, 0.9 + 0.2 * Fbm(u * 1.5, v * 1.5, 30));
            double fv = v - Math.Floor(v);
            if (fv < 0.12) c = Mul(c, 0.72);
            if (Hash((int)(u * S), (int)(v * S), 31) < 0.02) c = Rgb(255, 244, 190);
            return c;
        });
        // RED (192,24,64,16): red planks (inside the bulwarks), lids, pennants
        Zone(192, 24, 64, 16, (u, v) => Planks(Rgb(150, 30, 26), u, v, 1.0, 40, 0.6));
        // DECK (0,40,128,40): planks 2 wide running along the ship (rows: z = 12 - row), joints and nails
        Zone(0, 40, 128, 40, (u, v) =>
        {
            Color c = Planks(Rgb(172, 128, 84), u, v, 2.0, 50, 0.38);
            // nails near the joints
            int board = (int)Math.Floor(v / 2.0);
            int len = 10 + (int)(Hash(board, 1, 50) * 7);
            double off = Hash(board, 2, 50) * len;
            double along = (u + off) % len;
            double inBoard = v / 2.0 - board;
            if ((along > 0.35 && along < 0.65) && (Math.Abs(inBoard - 0.3) < 0.07 || Math.Abs(inBoard - 0.7) < 0.07)) c = Rgb(52, 40, 30);
            // sun-bleached and scuffed in places
            c = Mix(c, Rgb(196, 170, 130), 0.25 * Fbm(u * 0.15, v * 0.3, 52));
            return c;
        });
        // MAST (128,40,32,80): upright grain
        Zone(128, 40, 32, 80, (u, v) =>
        {
            double g = Fbm(u * 5.0, v * 0.25, 60);
            Color c = Mul(Rgb(122, 82, 48), 0.82 + 0.36 * g);
            if (Smooth(u * 9.0, v * 0.1, 61) > 0.78) c = Mul(c, 0.75);
            return c;
        });
        // SPAR (160,40,84,36): lengthwise grain
        Zone(160, 40, 84, 36, (u, v) =>
        {
            double g = Fbm(u * 0.25, v * 5.0, 70);
            Color c = Mul(Rgb(100, 66, 38), 0.8 + 0.4 * g);
            if (Smooth(u * 0.1, v * 9.0, 71) > 0.8) c = Mul(c, 0.72);
            return c;
        });
        // ROPE (244,40,12,84): twisted hemp
        Zone(244, 40, 12, 84, (u, v) =>
        {
            double s = ((u + v) * 2.6) % 1.0;
            Color c = s < 0.4 ? Rgb(150, 122, 80) : Rgb(198, 170, 120);
            return Mul(c, 0.9 + 0.2 * Hash((int)(u * S), (int)(v * S), 80));
        });
        // IRON (160,76,28,24): dark iron, a little rust
        Zone(160, 76, 28, 24, (u, v) =>
        {
            Color c = Mul(Rgb(56, 56, 62), 0.8 + 0.4 * Fbm(u * 1.2, v * 1.2, 90));
            if (Fbm(u * 2, v * 2, 91) > 0.74) c = Mix(c, Rgb(120, 62, 30), 0.6);
            double fv = v - Math.Floor(v);
            if (fv < 0.15) c = Mul(c, 1.35);
            return c;
        });
        // BLACK (188,76,28,24)
        Zone(188, 76, 28, 24, (u, v) => Mul(Rgb(16, 13, 13), 0.8 + 0.4 * Hash((int)(u * S), (int)(v * S), 95)));
        // GLASS (216,76,28,24): warm lamplight (glows)
        Zone(216, 76, 28, 24, (u, v) =>
        {
            double fu = u - Math.Floor(u), fv = v - Math.Floor(v);
            double center = 1 - 1.4 * Math.Max(Math.Abs(fu - 0.5), Math.Abs(fv - 0.5));
            Color c = Mix(Rgb(232, 140, 44), Rgb(255, 232, 150), Math.Max(0, center));
            return c;
        });
        for (int y = 76 * S; y < 100 * S; y++) for (int x = 216 * S; x < 244 * S; x++) glow[x, y] = true;
        // BONE (160,100,28,20)
        Zone(160, 100, 28, 20, (u, v) => Mul(Rgb(230, 222, 194), 0.88 + 0.16 * Fbm(u, v, 100)));
        // GRATE (188,100,56,20): a hatch grating, bars and holes a pixel apart
        Zone(188, 100, 56, 20, (u, v) =>
        {
            double fu = u - Math.Floor(u), fv = v - Math.Floor(v);
            bool hole = fu > 0.5 && fv > 0.5;
            return hole ? Rgb(22, 16, 12) : Mul(Rgb(128, 90, 54), 0.85 + 0.25 * Hash((int)u, (int)v, 110));
        });
        // CLOTH (0,80,128,40): black sail cloth with seams and patches (the jibs and the gaff sail)
        Zone(0, 80, 128, 40, (u, v) => Cloth(u * 0.9, v, 120, 99));

        // ── the square sails ──
        Sail(0, 30, 14, 1);    // main course: skull and crossbones
        Sail(60, 24, 12, 2);   // fore course: crossed cutlasses
        Sail(108, 24, 14, 0);  // main topsail
        Sail(156, 18, 10, 0);  // fore topsail
        Flag(192, 124);
        Stern(0, 160);
    }

    // charcoal sail cloth: seams every 3 (running down the sail), weave, weathering, patches
    static Color Cloth(double z, double s, int seed, double height)
    {
        Color c = Mul(Rgb(40, 40, 46), 0.86 + 0.24 * Fbm(z * 0.7, s * 0.7, seed));
        double weave = Hash((int)(z * S * 2), (int)(s * S * 2), seed + 1);
        c = Mul(c, 0.94 + 0.1 * weave);
        double fz = (z + 300) / 3.0; fz -= Math.Floor(fz);
        if (fz < 0.06) c = Mix(c, Rgb(70, 70, 76), 0.8);
        // patches (a rectangle of a browner cloth, its border stitched)
        for (int p = 0; p < 6; p++)
        {
            double pz = (Hash(p, 0, seed + 5) - 0.5) * 26, ps = Hash(p, 1, seed + 5) * Math.Max(1, height - 3);
            double pw = 1.5 + Hash(p, 2, seed + 5) * 2.5, ph = 1.2 + Hash(p, 3, seed + 5) * 2;
            if (z >= pz && z <= pz + pw && s >= ps && s <= ps + ph)
            {
                c = Mix(c, Rgb(66, 56, 50), 0.75);
                double edge = Math.Min(Math.Min(z - pz, pz + pw - z), Math.Min(s - ps, ps + ph - s));
                if (edge < 0.25 && ((int)((z + s) * S * 1.5)) % 2 == 0) c = Rgb(120, 112, 96);
            }
        }
        return c;
    }

    // ── emblems: 1 = bone, 2 = dark detail, 0 = none ──
    static double SegDist(double px, double py, double ax, double ay, double bx, double by)
    {
        double dx = bx - ax, dy = by - ay;
        double t = Math.Max(0, Math.Min(1, ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy)));
        double qx = ax + t * dx - px, qy = ay + t * dy - py;
        return Math.Sqrt(qx * qx + qy * qy);
    }
    static bool InEllipse(double x, double y, double cx, double cy, double rx, double ry)
    {
        double a = (x - cx) / rx, b = (y - cy) / ry;
        return a * a + b * b <= 1;
    }
    // the skull and crossbones, centred at (cz, cs), `k` its size (1 = the main course's)
    static int SkullBones(double z, double s, double cz, double cs, double k)
    {
        double x = (z - cz) / k, y = (s - cs) / k;
        int result = 0;
        // crossbones behind the skull
        for (int sign = -1; sign <= 1; sign += 2)
        {
            double ax = -5.6 * sign, ay = -4.3, bx = 5.6 * sign, by = 4.6;
            if (SegDist(x, y, ax, ay, bx, by) < 0.55) result = 1;
            double nx = -(by - ay), ny = bx - ax, nl = Math.Sqrt(nx * nx + ny * ny); nx /= nl; ny /= nl;
            foreach (double e in new double[] { 0, 1 })
            {
                double ex = ax + (bx - ax) * e, ey = ay + (by - ay) * e;
                if (InEllipse(x, y, ex + nx * 0.55, ey + ny * 0.55, 0.75, 0.75) || InEllipse(x, y, ex - nx * 0.55, ey - ny * 0.55, 0.75, 0.75)) result = 1;
            }
        }
        // the skull
        bool cranium = InEllipse(x, y, 0, -1.3, 3.1, 2.9);
        bool jaw = Math.Abs(x) < 1.9 && y > 0.5 && y < 2.9 && !(y > 2.3 && Math.Abs(x) > 1.4);
        bool cheek = InEllipse(x, y, 0, 0.6, 2.6, 1.3);
        if (cranium || jaw || cheek)
        {
            result = 1;
            if (InEllipse(x, y, -1.25, -0.4, 0.95, 1.0) || InEllipse(x, y, 1.25, -0.4, 0.95, 1.0)) result = 2;     // eyes
            if (y > 0.6 && y < 1.6 && Math.Abs(x) < 0.55 * (1.6 - y)) result = 2;                                // nose
            if (y > 2.0 && y < 2.75 && Math.Abs(x) < 1.6)
            {
                double tooth = (x + 1.6) / 0.64; tooth -= Math.Floor(tooth);
                if (tooth < 0.18 || Math.Abs(y - 2.0) < 0.12) result = 2;                                       // teeth
            }
            if (Math.Abs(y + 3.0) < 0.12 && Math.Abs(x) < 1.0 && x > -0.2) result = 2;                           // a crack
        }
        return result;
    }
    // two crossed cutlasses, curved, hilts at the bottom
    static int Cutlasses(double z, double s, double cz, double cs)
    {
        int result = 0;
        for (int sign = -1; sign <= 1; sign += 2)
        {
            // blade: a curve from the hilt (bottom) to the point (top), widening toward the point
            double best = 99, bestT = 0;
            for (int i = 0; i <= 48; i++)
            {
                double t = i / 48.0;
                double bx = sign * (-5.6 + 11.0 * t) + sign * 1.4 * Math.Sin(t * Math.PI);
                double by = 4.2 - 8.4 * t;
                double d = Math.Sqrt((z - cz - bx) * (z - cz - bx) + (s - cs - by) * (s - cs - by));
                if (d < best) { best = d; bestT = t; }
            }
            double width = 0.28 + 0.35 * bestT;
            if (bestT > 0.12 && bestT < 0.99 && best < width) result = 1;
            // the guard, grip and pommel at the hilt end
            double hx = sign * -5.6, hy = 4.2;
            double gx = sign * -4.6, gy = 3.1;
            if (SegDist(z - cz, s - cs, gx - 1.0, gy - 1.0 * sign * sign * -1, gx + 1.0, gy + 1.0) < 0.3) result = 1;
            if (SegDist(z - cz, s - cs, hx, hy, gx, gy) < 0.35) result = 2;
            if (InEllipse(z - cz, s - cs, hx - sign * 0.4, hy + 0.4, 0.5, 0.5)) result = 1;
        }
        return result;
    }

    // A square sail's picture block at (U, SAIL_V): 6 thirds wide (see PirateShipModel.sail): third k's back face in
    // [2Lk, 2Lk + L), its front face in [2Lk + L, 2Lk + 2L). Each texel is worked back to its spot on the sail (z across,
    // s down), so the emblem, the patches and the tattered hem line up across the pieces from both sides.
    static void Sail(int U, int D, int H, int emblem)
    {
        const int V = 124;
        double L = D / 3.0;
        int seed = 200 + U;
        for (int y = V * S; y < (V + H) * S; y++)
        {
            for (int x = U * S; x < (U + 2 * D) * S; x++)
            {
                double c = (x - U * S + 0.5) / S, s = (y - V * S + 0.5) / S;
                int k = (int)Math.Floor(c / (2 * L));
                double within = c - 2 * L * k;
                bool back = within < L;
                double zmin = k == 0 ? 0 : k == 1 ? -L / 2 : -L, zmax = zmin + L, off = k == 0 ? L / 2 : k == 1 ? 0 : -L / 2;
                double local = back ? zmax - within : zmin + (within - L);
                double z = local + off;
                // the tattered hem and a few holes (shot through)
                double hem = 0.25 + 1.25 * Math.Abs(Smooth(z * 1.6, 0.5, seed) - 0.5) * 2 + 0.3 * Hash((int)(z * S), 1, seed);
                bool hole = s > H - hem;
                for (int h = 0; h < 4 && !hole; h++)
                {
                    double hz = (Hash(h, 0, seed + 3) - 0.5) * (D - 4), hs = 1 + Hash(h, 1, seed + 3) * (H - 3);
                    double r = 0.22 + 0.2 * Hash(h, 2, seed + 3);
                    if (InEllipse(z, s, hz, hs, r, r)) hole = true;
                }
                if (hole) { img[x, y] = Clear; continue; }
                Color col = Cloth(z, s, seed, H);
                int e = emblem == 1 ? SkullBones(z, s, 0, H / 2.0 - 0.2, H / 14.0) : emblem == 2 ? Cutlasses(z, s, 0, H / 2.0) : 0;
                if (e != 0)
                {
                    Color mark = e == 1 ? Mul(Rgb(236, 230, 208), 0.9 + 0.12 * Fbm(z * 1.3, s * 1.3, seed + 9)) : Rgb(26, 24, 26);
                    col = back ? mark : Mix(col, e == 1 ? Rgb(150, 146, 136) : col, 0.35);
                }
                // a darker band along the hem
                if (s > H - hem - 0.4) col = Mul(col, 0.8);
                img[x, y] = col;
            }
        }
    }

    // The Jolly Roger at (FU, FV): 18 columns (its front shows 0-16, its back 1-17), 10 rows; column 0 at the pole
    static void Flag(int FU, int FV)
    {
        for (int y = FV * S; y < (FV + 10) * S; y++)
        {
            for (int x = FU * S; x < (FU + 18) * S; x++)
            {
                double c = (x - FU * S + 0.5) / S, s = (y - FV * S + 0.5) / S;
                // a frayed fly end
                double fray = 15.6 - 0.9 * Hash((int)(s * S / 2), 7, 300);
                if (c > fray) { img[x, y] = Clear; continue; }
                Color col = Mul(Rgb(18, 18, 20), 0.85 + 0.3 * Fbm(c, s, 301));
                if (c < 0.6) col = Rgb(220, 214, 196);   // the white hoist tape at the pole
                int e = SkullBones(c, s, 8.5, 5.0, 0.95);
                if (e == 1) col = Mul(Rgb(240, 236, 220), 0.92 + 0.1 * Fbm(c * 2, s * 2, 302));
                else if (e == 2) col = Rgb(18, 18, 20);
                img[x, y] = col;
            }
        }
    }

    // The stern gallery at (SU, SV): 18 wide (column c is z = 9 - c) by 15 tall (row r is y = -13 + r): gold carving,
    // three arched windows glowing, pilasters between them, a gold name rail, the red waterline band at the bottom
    static void Stern(int SU, int SV)
    {
        for (int y = SV * S; y < (SV + 15) * S; y++)
        {
            for (int x = SU * S; x < (SU + 18) * S; x++)
            {
                double c = (x - SU * S + 0.5) / S, r = (y - SV * S + 0.5) / S;
                double z = 9 - c;
                Color col = Planks(Rgb(70, 42, 26), c, r * 0.5 + 40, 1.0, 400, 0.6);
                Color gold = Mix(Rgb(160, 112, 30), Rgb(255, 214, 92), 0.5 + 0.5 * Math.Sin(r * 3.0 + c * 0.4));
                bool isGlow = false;
                if (r >= 12) col = Planks(Rgb(118, 26, 22), c, r, 1.0, 401, 0.6);         // the waterline band
                if (r >= 9.8 && r < 11.6)                                                 // the name rail
                {
                    col = gold;
                    if (r > 10.2 && r < 11.2 && Math.Abs(z) < 6.5 && Hash((int)(c * S / 1.5), (int)(r * S / 1.5), 402) < 0.35) col = Rgb(60, 36, 14);
                }
                if (r < 2.2)                                                              // carving along the top
                {
                    double wave = 0.9 + 0.5 * Math.Sin(z * 1.6);
                    if (r < wave || r < 0.5) col = gold;
                }
                if (r >= 8.4 && r < 9.0) col = gold;                                      // sill
                if (c < 0.45 || c > 17.55) col = gold;                                    // edges
                // three arched windows
                foreach (double wz in new double[] { -5.5, 0, 5.5 })
                {
                    double dx = z - wz, half = 1.8, top = 3.4, bottom = 8.2;
                    bool inArch = Math.Abs(dx) <= half && r <= bottom && (r >= top || InEllipse(dx, r, 0, top, half, 1.4));
                    bool inFrame = Math.Abs(dx) <= half + 0.35 && r <= bottom + 0.3 && (r >= top || InEllipse(dx, r, 0, top, half + 0.35, 1.75));
                    if (inFrame && !inArch) col = gold;
                    if (inArch)
                    {
                        bool bar = Math.Abs(dx) < 0.1 || Math.Abs(r - 5.0) < 0.1 || Math.Abs(r - 6.6) < 0.1;
                        if (bar) col = Rgb(40, 26, 14);
                        else { col = Mix(Rgb(236, 150, 52), Rgb(255, 230, 150), Math.Max(0, 1 - Math.Abs(dx) / half) * 0.8); isGlow = true; }
                    }
                }
                // gold pilasters between the windows
                if ((Math.Abs(Math.Abs(z) - 2.75) < 0.3) && r > 2.4 && r < 8.4) col = gold;
                img[x, y] = col;
                glow[x, y] = isGlow;
            }
        }
    }

    // ── the item icon: a little galleon, black sails, the skull on its main course, the Jolly Roger ──
    static Color[,] icon = new Color[32, 32];
    static void IP(int x, int y, Color c) { if (x >= 0 && x < 32 && y >= 0 && y < 32) icon[x, y] = c; }
    static void IRect(int x0, int y0, int x1, int y1, Color c) { for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) IP(x, y, c); }
    static void Icon()
    {
        for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++) icon[x, y] = Clear;
        Color black = Rgb(30, 28, 32), outline = Rgb(10, 8, 10), gold = Rgb(236, 186, 62), red = Rgb(140, 30, 26);
        Color wood = Rgb(110, 72, 42), sail = Rgb(46, 46, 52), sailLight = Rgb(70, 70, 78), bone = Rgb(236, 230, 208), mast = Rgb(96, 62, 36);
        // hull: rows 18..28, stern high on the left, bow rising to the right
        for (int x = 3; x <= 29; x++)
        {
            int top = x <= 9 ? 15 : x >= 25 ? 19 - (x - 24) : 19;
            double t = (x - 3) / 26.0;
            int bottom = (int)Math.Round(27 - 2.5 * Math.Pow(Math.Abs(t - 0.45) / 0.55, 2.2));
            if (x >= 27) bottom = Math.Max(top + 2, bottom - (x - 26) * 2);
            for (int y = top; y <= bottom; y++)
            {
                Color c = black;
                if (y == top) c = gold;
                else if (y == top + 2 && x > 4 && x < 27) c = gold;
                else if (y >= bottom - 2 && y < bottom) c = red;
                else if (y == bottom) c = wood;
                IP(x, y, c);
            }
            IP(x, bottom + 1, outline);
        }
        // gunports and stern windows
        foreach (int gx in new int[] { 12, 16, 20 }) IP(gx, 21, Rgb(8, 6, 6));
        IP(5, 17, Rgb(255, 210, 110)); IP(7, 17, Rgb(255, 210, 110));
        // masts
        IRect(15, 3, 15, 18, mast); IRect(22, 6, 22, 18, mast); IRect(8, 6, 8, 14, mast);
        // bowsprit
        for (int i = 0; i < 5; i++) IP(28 + i, 17 - i, mast);
        // sails: main course with the skull, main topsail, fore course and topsail, the gaff sail
        IRect(11, 10, 19, 16, sail); IRect(12, 4, 18, 8, sail); IRect(19, 11, 25, 16, sail); IRect(19, 7, 24, 9, sail);
        for (int y = 8; y <= 14; y++) for (int x = 4; x <= 7; x++) if (x - 4 >= (14 - y) / 2) IP(x, y, sail);
        IRect(11, 10, 19, 10, sailLight); IRect(12, 4, 18, 4, sailLight); IRect(19, 11, 25, 11, sailLight);
        // the skull (3 wide, two dark eyes, teeth) over crossbones
        IP(12, 11, bone); IP(13, 12, bone); IP(17, 12, bone); IP(18, 11, bone);
        IP(12, 15, bone); IP(13, 14, bone); IP(17, 14, bone); IP(18, 15, bone);
        IRect(14, 11, 16, 13, bone); IP(14, 12, Rgb(20, 20, 20)); IP(16, 12, Rgb(20, 20, 20));
        IP(14, 14, bone); IP(16, 14, bone);
        // tattered hems
        IP(12, 16, Clear); IP(17, 16, Clear); IP(21, 16, Clear); IP(14, 8, Clear);
        // the jib to the bowsprit
        for (int y = 10; y <= 16; y++) for (int x = 24; x <= 30; x++) if (x - 24 <= (y - 9) && x >= 26) IP(x, y, sailLight);
        // the Jolly Roger and a pennant
        IRect(9, 1, 14, 3, black); IP(11, 2, bone); IP(12, 2, bone); IP(15, 2, mast); IP(15, 1, gold);
        IP(23, 5, red); IP(24, 5, red); IP(25, 5, red);
        // dark outline round the sails' edges
        for (int y = 1; y < 31; y++) for (int x = 1; x < 31; x++)
        {
            if (icon[x, y].A != 0) continue;
            bool near = icon[x - 1, y].A != 0 || icon[x + 1, y].A != 0 || icon[x, y - 1].A != 0 || icon[x, y + 1].A != 0;
            if (near && (icon[x - 1, y] == sail || icon[x + 1, y] == sail || icon[x, y - 1] == sail || icon[x, y + 1] == sail)) icon[x, y] = Color.FromArgb(160, 10, 10, 12);
        }
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

    // The Spectral Galleon (client/GhostShipModel): the same ship, washed pale grey-teal and half see-through, rotted
    // through in places, its sails torn more; its emblems, rails and lanterns glow spectral cyan, faint wisps glow over
    // the hull; and the ghost (SPECTRE at (0,180) 64 x 40, its glowing eyes and lantern at (64,180) 16 x 16).
    public static string MakeGhost(string entityDir, string itemDir)
    {
        img = new Color[N, N];
        glow = new bool[N, N];
        Paint();
        Color[,] lit = new Color[N, N];
        for (int y = 0; y < N; y++) for (int x = 0; x < N; x++)
        {
            Color c = img[x, y];
            if (c.A == 0) continue;
            double u = x / (double)S, v = y / (double)S;
            double l = (0.3 * c.R + 0.59 * c.G + 0.11 * c.B) / 255.0;
            bool sail = v >= 124 && v < 138 && u < 210;
            bool hull = v < 80 || (v >= 160 && v < 176);
            Color pale = Mix(Rgb(40, 70, 72), Rgb(190, 240, 236), Math.Min(1, l * 1.25 + 0.1));
            int alpha = sail ? 150 : 200;
            // rot holes through the planks, more holes through the sails
            double rot = Fbm(u * 0.35, v * 0.35, 900);
            if (hull && rot > 0.74) { img[x, y] = Clear; continue; }
            if (sail && Fbm(u * 0.6, v * 0.6, 901) > 0.66) { img[x, y] = Clear; continue; }
            img[x, y] = Color.FromArgb(alpha, pale.R, pale.G, pale.B);
            // what glows: emblems (bright bits of the sails, the flag), the gold, lantern glass, wisps
            bool bright = l > 0.7 && (sail || (v >= 124 && v < 134 && u >= 192));
            bool gold = u >= 192 && v < 24;
            bool glass = u >= 216 && u < 244 && v >= 76 && v < 100;
            bool wisp = hull && Fbm(u * 0.25, v * 1.2, 902) > 0.72;
            if (bright || gold || glass || wisp || glow[x, y])
            {
                glow[x, y] = true;
                lit[x, y] = glass ? Rgb(90, 230, 255) : wisp ? Rgb(40, 120, 120) : Rgb(120, 255, 235);
            }
            else glow[x, y] = false;
        }
        // the ghost: a pale robe fading downward, a dark face in the hood
        for (int y = 180 * S; y < 220 * S; y++) for (int x = 0; x < 64 * S; x++)
        {
            double u = (x + 0.5) / S, v = (y - 180 * S + 0.5) / S;
            double fade = 1 - v / 50.0;
            Color robe = Mix(Rgb(150, 230, 225), Rgb(230, 255, 252), Fbm(u * 0.8, v * 0.5, 903));
            if (u >= 14 && u < 30 && v < 8) robe = Rgb(10, 20, 24);
            img[x, y] = Color.FromArgb(C(170 * fade + 40), robe.R, robe.G, robe.B);
            glow[x, y] = Fbm(u * 0.5, v * 0.5, 904) > 0.6;
            lit[x, y] = Rgb(60, 160, 150);
        }
        for (int y = 180 * S; y < 196 * S; y++) for (int x = 64 * S; x < 80 * S; x++)
        {
            img[x, y] = Rgb(120, 255, 240);
            glow[x, y] = true;
            lit[x, y] = Rgb(140, 255, 240);
        }
        Save(img, N, N, System.IO.Path.Combine(entityDir, "ghost_ship.png"), (x, y) => true);
        Color[,] glowImg = lit;
        Save(glowImg, N, N, System.IO.Path.Combine(entityDir, "ghost_ship_glow.png"), (x, y) => glow[x, y]);
        // the icon: the pirate ship's, gone spectral
        Icon();
        for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++)
        {
            Color c = icon[x, y];
            if (c.A == 0) continue;
            double l = (0.3 * c.R + 0.59 * c.G + 0.11 * c.B) / 255.0;
            Color p = Mix(Rgb(40, 90, 96), Rgb(200, 255, 250), Math.Min(1, l * 1.4 + 0.15));
            icon[x, y] = Color.FromArgb(Math.Min((int)c.A, 215), p.R, p.G, p.B);
        }
        Save(icon, 32, 32, System.IO.Path.Combine(itemDir, "ghost_ship.png"), (x, y) => true);
        return "saved ghost";
    }

    public static string Make(string entityDir, string itemDir)
    {
        Paint();
        Save(img, N, N, System.IO.Path.Combine(entityDir, "pirate_ship.png"), (x, y) => true);
        Save(img, N, N, System.IO.Path.Combine(entityDir, "pirate_ship_glow.png"), (x, y) => glow[x, y]);
        Icon();
        Save(icon, 32, 32, System.IO.Path.Combine(itemDir, "pirate_ship.png"), (x, y) => true);
        return "saved";
    }
}
