// The pixel work for tools/make_meat_slab_blocks.ps1 (compiled there with Add-Type): 64x64 textures for the Meat Slab
// blocks. Coordinates are in 16ths (block units) so the shapes line up with the model; 4 pixels per unit.
//  - meat: muscle bundles (Voronoi cells, each its own shade and fibre angle) split by thin seams of fat, fine fibres,
//    lacy marbling and flecks, a wet sheen; fat: creamy, blushed pink in places; skin: pig skin with pores and
//    creases, chicken skin with follicle bumps, rabbit's silverskin; bone; spongy red marrow.
//  - Material: one material's tile (meat, lean, fat, skin, marrow) for the sculpted models (tools/MeatSculpt.cs)
//  - Rot: a tile gone off (a placed slab whose time is up); Bone: the bone tile
using System;
using System.Collections.Generic;
using System.Drawing;

public class MeatCutSpec {
    public double[] M, D, L, F, S;
    public string Top;
    public double Marble;
    public bool Small;
    public int Seed;
}

public static class MeatTex {
    public const int N = 64;
    const double PX = N / 16.0;
    static readonly double[] BONE = { 236, 228, 206 };
    static readonly double[] BONE_EDGE = { 186, 170, 140 };
    static readonly double[] MARROW = { 168, 58, 52 };
    static readonly double[] WHITE = { 255, 246, 240 };

    // ── noise ──
    public static double Hash(int x, int y, int s) {
        unchecked {
            uint h = (uint)(x * 374761393 + y * 668265263 + s * 1442695041);
            h = (h ^ (h >> 13)) * 1274126177u;
            h ^= h >> 16;
            return (h & 0xFFFFFF) / 16777216.0;
        }
    }
    public static double Value(double x, double y, int s) {
        int ix = (int)Math.Floor(x), iy = (int)Math.Floor(y);
        double tx = x - ix, ty = y - iy;
        tx = tx * tx * (3 - 2 * tx); ty = ty * ty * (3 - 2 * ty);
        double a = Hash(ix, iy, s), b = Hash(ix + 1, iy, s), c = Hash(ix, iy + 1, s), d = Hash(ix + 1, iy + 1, s);
        return (a * (1 - tx) + b * tx) * (1 - ty) + (c * (1 - tx) + d * tx) * ty;
    }
    public static double Fbm(double x, double y, int s, int oct) {
        double sum = 0, amp = 0.5, f = 1, norm = 0;
        for (int i = 0; i < oct; i++) { sum += amp * Value(x * f, y * f, s + i * 17); norm += amp; amp *= 0.5; f *= 2; }
        return sum / norm;
    }
    static double Ridge(double x, double y, int s) { return 1 - Math.Abs(Fbm(x, y, s, 4) * 2 - 1); }

    // ── colour ──
    static double[] Mix(double[] a, double[] b, double t) {
        if (t < 0) t = 0; if (t > 1) t = 1;
        return new double[] { a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t };
    }
    static double[] Mul(double[] a, double f) { return new double[] { a[0] * f, a[1] * f, a[2] * f }; }
    static int C(double v) { return (int)Math.Max(0, Math.Min(255, Math.Round(v))); }
    static void Put(Bitmap b, int x, int y, double[] c) { b.SetPixel(x, y, Color.FromArgb(255, C(c[0]), C(c[1]), C(c[2]))); }

    // ── materials ──
    static double[] Meat(MeatCutSpec c, double u, double v, double ang, int seed, double tone) {
        double n = Fbm(u * 0.35, v * 0.35, seed, 4);
        double[] col = Mul(Mix(c.D, c.M, 0.3 + 0.7 * n), tone);
        // the fibres, wavy, along `ang`
        double along = u * Math.Cos(ang) + v * Math.Sin(ang), across = -u * Math.Sin(ang) + v * Math.Cos(ang);
        double warp = Fbm(along * 0.15, across * 0.5, seed + 5, 3) * 3.0;
        double f = Math.Sin((across * 5.0 + warp) * Math.PI);
        double broken = Value(along * 0.9, across * 5, seed + 9);
        if (f > 0.55 && broken > 0.4) col = Mix(col, c.L, 0.2 + 0.15 * broken);
        else if (f < -0.7) col = Mix(col, c.D, 0.28);
        // marbling: a lacy net of fat, and little flecks
        if (c.Marble > 0) {
            // a few soft veins, and irregular flecks that thicken with more marbling
            double r = Ridge(u * 0.3, v * 0.3, seed + 31);
            double thr = 0.99 - c.Marble * 0.025;
            if (r > thr) col = Mix(col, Mix(c.F, c.M, 0.3), 0.3 + 0.5 * (r - thr) / (1 - thr));
            double fl = Fbm(u * 0.9, v * 0.9, seed + 41, 3);
            double fthr = 0.74 - c.Marble * 0.09;
            if (fl > fthr) col = Mix(col, Mix(c.F, c.M, 0.15), Math.Min(0.85, (fl - fthr) * 9));
        }
        // a wet sheen
        double sh = Fbm(u * 0.45 + 3, v * 0.45, seed + 21, 3);
        if (sh > 0.66) col = Mix(col, WHITE, (sh - 0.66) * 1.3);
        // fine grain
        col = Mul(col, 0.95 + 0.1 * Hash((int)(u * PX), (int)(v * PX), seed + 51));
        return col;
    }

    static double[] Fat(MeatCutSpec c, double u, double v, int seed) {
        double n = Fbm(u * 0.5, v * 0.5, seed + 61, 4);
        double[] col = Mix(c.F, new double[] { 234, 206, 156 }, n * 0.3);
        double p = Fbm(u * 0.9, v * 0.9, seed + 67, 3);
        if (p > 0.62) col = Mix(col, Mix(c.F, c.M, 0.45), (p - 0.62) * 1.4);
        double sh = Fbm(u * 0.6, v * 0.6, seed + 69, 2);
        if (sh > 0.7) col = Mix(col, WHITE, (sh - 0.7) * 1.5);
        return Mul(col, 0.965 + 0.06 * Hash((int)(u * PX), (int)(v * PX), seed + 71));
    }

    // a jittered grid of dots: distance to this cell's dot, or 9 if the cell has none
    static double Dot(double gx, double gy, int seed, double keep) {
        int ix = (int)Math.Floor(gx), iy = (int)Math.Floor(gy);
        if (Hash(ix, iy, seed + 3) > keep) return 9;
        double px = ix + 0.5 + (Hash(ix, iy, seed) - 0.5) * 0.6, py = iy + 0.5 + (Hash(ix, iy, seed + 1) - 0.5) * 0.6;
        return Math.Sqrt((gx - px) * (gx - px) + (gy - py) * (gy - py));
    }

    static double[] Skin(MeatCutSpec c, string kind, double u, double v, int seed) {
        double n = Fbm(u * 0.4, v * 0.4, seed + 81, 4);
        double[] col = Mix(c.S, Mul(c.S, 0.86), n);
        if (kind == "pigskin") {
            double d = Dot(u * 1.5, v * 1.5, seed + 83, 0.75);
            if (d < 0.11) col = Mul(c.S, 0.7); else if (d < 0.2) col = Mul(col, 0.92);
            double crease = Math.Sin((v * 1.1 + Fbm(u * 0.25, v * 0.25, seed + 91, 3) * 5) * Math.PI);
            if (crease > 0.95) col = Mul(col, 0.9);
            double sh = Fbm(u * 0.5, v * 0.5, seed + 93, 3);
            if (sh > 0.64) col = Mix(col, WHITE, (sh - 0.64) * 1.2);
        } else if (kind == "chickenskin") {
            double d = Dot(u * 1.8, v * 1.8, seed + 85, 0.9);
            if (d < 0.1) col = Mul(col, 0.84); else if (d < 0.24) col = Mix(col, WHITE, 0.3);
            double t = Fbm(u * 0.35, v * 0.35, seed + 95, 3);
            if (t > 0.58) col = Mix(col, c.M, (t - 0.58) * 1.6);
        }
        return col;
    }

    // ── muscle bundles ──
    class Cells {
        public double[] X, Y; public int Count;
        public Cells(int count, double x0, double y0, double x1, double y1, int seed) {
            Count = count; X = new double[count]; Y = new double[count];
            for (int i = 0; i < count; i++) { X[i] = x0 + Hash(i, 1, seed) * (x1 - x0); Y[i] = y0 + Hash(i, 2, seed) * (y1 - y0); }
        }
        // nearest cell, and how far from the border with the next one (0 on it)
        public int Find(double u, double v, int seed, out double border) {
            double wu = u + (Fbm(u * 0.3, v * 0.3, seed + 111, 3) - 0.5) * 2.4;
            double wv = v + (Fbm(u * 0.3, v * 0.3, seed + 113, 3) - 0.5) * 2.4;
            double d1 = 1e9, d2 = 1e9; int best = 0;
            for (int i = 0; i < Count; i++) {
                double d = Math.Sqrt((wu - X[i]) * (wu - X[i]) + (wv - Y[i]) * (wv - Y[i]));
                if (d < d1) { d2 = d1; d1 = d; best = i; } else if (d < d2) d2 = d;
            }
            border = d2 - d1;
            return best;
        }
    }

    static double[] Bundled(MeatCutSpec c, Cells cells, double u, double v, int seed, double baseAng, double angSpread) {
        double border;
        int i = cells.Find(u, v, seed, out border);
        double tone = 0.88 + 0.22 * Hash(i, 7, seed);
        double ang = baseAng + (Hash(i, 9, seed) - 0.5) * angSpread;
        double[] col = Meat(c, u, v, ang, seed + i * 3, tone);
        double seam = c.Small ? 0.14 : 0.2;
        if (border < seam) col = Mix(col, Mix(c.F, c.M, 0.3), 0.85 * (1 - border / seam));
        return col;
    }

    // One material's tile for the sculpted models (tools/MeatSculpt.cs): meat (bundles and fibres), lean (the eye of a
    // loin or rib), fat, skin (pig, chicken, or rabbit's silverskin over meat), marrow
    public static Bitmap Material(MeatCutSpec c, string mat) {
        Bitmap b = new Bitmap(N, N);
        int seed = c.Seed + 600;
        Cells cells = new Cells(c.Small ? 6 : 9, 0, 0, 16, 16, seed + 700);
        MeatCutSpec lean = new MeatCutSpec { M = c.L, D = c.M, L = Mix(c.L, WHITE, 0.35), F = c.F, Marble = c.Marble * 0.9, Small = c.Small };
        for (int py = 0; py < N; py++) for (int px = 0; px < N; px++) {
            double u = (px + 0.5) / PX, v = (py + 0.5) / PX;
            double[] col;
            switch (mat) {
                case "meat": col = Bundled(c, cells, u, v, seed, 0.3, 1.4); break;
                case "lean": col = Meat(lean, u, v, 0.4, seed + 7, 0.95); break;
                case "fat": col = Fat(c, u, v, seed); break;
                case "skin":
                    if (c.Top == "silver") {
                        col = Meat(c, u, v, 0.1, seed, 1.0);
                        double s = Fbm(u * 0.22, v * 1.1, seed + 141, 4);
                        col = Mix(col, new double[] { 232, 226, 234 }, 0.35 + Math.Min(0.4, Math.Max(0, s - 0.45) * 2));
                    } else col = Skin(c, c.Top, u, v, seed);
                    break;
                default: {
                    double sp = Fbm(u * 2.4, v * 2.4, seed + 107, 3);
                    col = sp > 0.6 ? Mix(MARROW, BONE, 0.45) : sp < 0.35 ? Mul(MARROW, 0.75) : MARROW;
                    break;
                }
            }
            Put(b, px, py, col);
        }
        return b;
    }

    // The same tile gone off (a placed slab whose time is up): drained of colour, tinged grey-green, darker, with
    // furry spots of mould and darker bruises
    public static Bitmap Rot(Bitmap fresh, int seed) {
        Bitmap b = new Bitmap(N, N);
        double[] tint = { 112, 122, 72 };
        for (int py = 0; py < N; py++) for (int px = 0; px < N; px++) {
            Color c = fresh.GetPixel(px, py);
            double grey = c.R * 0.3 + c.G * 0.59 + c.B * 0.11;
            double[] col = Mix(new double[] { c.R, c.G, c.B }, new double[] { grey, grey, grey }, 0.55);
            col = Mul(Mix(col, tint, 0.35), 0.82);
            double u = (px + 0.5) / PX, v = (py + 0.5) / PX;
            double mould = Fbm(u * 0.6, v * 0.6, seed + 801, 4);
            if (mould > 0.68) col = Mix(col, new double[] { 206, 214, 178 }, Math.Min(0.8, (mould - 0.68) * 4));
            double bruise = Fbm(u * 0.4 + 7, v * 0.4, seed + 811, 3);
            if (bruise < 0.3) col = Mul(col, 0.7 + bruise);
            Put(b, px, py, col);
        }
        return b;
    }

    public static Bitmap Bone(int seed) {
        Bitmap b = new Bitmap(N, N);
        double[] meat = { 176, 70, 62 };
        for (int py = 0; py < N; py++) for (int px = 0; px < N; px++) {
            double u = (px + 0.5) / PX, v = (py + 0.5) / PX;
            double[] col = Mix(BONE, BONE_EDGE, Fbm(u * 0.4, v * 2.5, seed, 4) * 0.7);
            if (Math.Sin((u * 3 + Fbm(u, v, seed + 3, 2) * 4) * Math.PI) > 0.9) col = Mul(col, 0.94);
            double m = Fbm(u * 0.35, v * 0.35, seed + 7, 4);
            if (m > 0.66) col = Mix(col, meat, Math.Min(0.85, (m - 0.66) * 4));
            Put(b, px, py, Mul(col, 0.96 + 0.07 * Hash(px, py, seed + 9)));
        }
        return b;
    }
}
