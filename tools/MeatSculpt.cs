// The Meat Slab block models, sculpted (tools/make_meat_slab_blocks.ps1 compiles this with Add-Type). Each cut is a 3D
// shape (signed distance functions: rounded boxes, ellipsoids, tapered capsules, logs, blended together with a little
// lumpiness), filled in voxels of meat, lean, fat, skin, bone and marrow (fat caps, rinds, seams, ribs, knuckles,
// vertebrae). The voxels are merged into as few boxes as will do (greedy meshing, one material per box) and faces
// hidden against the solid are left out. Every face is mapped by its position in the block, so the material
// textures (tools/MeatTex.cs) flow across the little boxes. Resolution: half a pixel, a quarter for the small cuts.
using System;
using System.Collections.Generic;
using System.Globalization;
using System.Text;

public class SculptResult {
    public string Json;
    public double[] Bounds;
    public bool[] Used;
    public int Boxes;
}

public static class MeatSculpt {
    public const byte NONE = 0, MEAT = 1, LEAN = 2, FAT = 3, SKIN = 4, BONE = 5, MARROW = 6;
    public static readonly string[] Names = { "", "meat", "lean", "fat", "skin", "bone", "marrow" };

    // ── noise and shapes ──
    static double H3(int x, int y, int z, int s) {
        unchecked {
            uint h = (uint)(x * 374761393 + y * 668265263 + z * 1274126177 + s * 1442695041);
            h = (h ^ (h >> 13)) * 1274126177u;
            h ^= h >> 16;
            return (h & 0xFFFFFF) / 16777216.0;
        }
    }
    static double S(double t) { return t * t * (3 - 2 * t); }
    static double N3(double x, double y, double z, int s) {
        int ix = (int)Math.Floor(x), iy = (int)Math.Floor(y), iz = (int)Math.Floor(z);
        double tx = S(x - ix), ty = S(y - iy), tz = S(z - iz);
        double a = Lerp(Lerp(H3(ix, iy, iz, s), H3(ix + 1, iy, iz, s), tx), Lerp(H3(ix, iy + 1, iz, s), H3(ix + 1, iy + 1, iz, s), tx), ty);
        double b = Lerp(Lerp(H3(ix, iy, iz + 1, s), H3(ix + 1, iy, iz + 1, s), tx), Lerp(H3(ix, iy + 1, iz + 1, s), H3(ix + 1, iy + 1, iz + 1, s), tx), ty);
        return Lerp(a, b, tz);
    }
    static double Lerp(double a, double b, double t) { return a + (b - a) * t; }
    static double Lumpy(double x, double y, double z, int s, double amp) {
        return (N3(x * 0.35, y * 0.35, z * 0.35, s) * 0.65 + N3(x * 0.9, y * 0.9, z * 0.9, s + 5) * 0.35 - 0.5) * amp * 2;
    }
    static double Clamp(double v, double a, double b) { return v < a ? a : v > b ? b : v; }
    static double SmoothStep(double a, double b, double v) { return S(Clamp((v - a) / (b - a), 0, 1)); }
    static double Len(double a, double b) { return Math.Sqrt(a * a + b * b); }
    static double Len(double a, double b, double c) { return Math.Sqrt(a * a + b * b + c * c); }

    static double Ellipsoid(double x, double y, double z, double cx, double cy, double cz, double rx, double ry, double rz) {
        return (Len((x - cx) / rx, (y - cy) / ry, (z - cz) / rz) - 1) * Math.Min(rx, Math.Min(ry, rz));
    }
    static double RoundBox(double x, double y, double z, double cx, double cy, double cz, double hx, double hy, double hz, double r) {
        double qx = Math.Abs(x - cx) - hx + r, qy = Math.Abs(y - cy) - hy + r, qz = Math.Abs(z - cz) - hz + r;
        return Len(Math.Max(qx, 0), Math.Max(qy, 0), Math.Max(qz, 0)) + Math.Min(Math.Max(qx, Math.Max(qy, qz)), 0) - r;
    }
    // a capsule from a (radius ra) to b (radius rb)
    static double Capsule(double x, double y, double z, double ax, double ay, double az, double bx, double by, double bz, double ra, double rb) {
        double dx = bx - ax, dy = by - ay, dz = bz - az;
        double t = Clamp(((x - ax) * dx + (y - ay) * dy + (z - az) * dz) / (dx * dx + dy * dy + dz * dz), 0, 1);
        return Len(x - ax - dx * t, y - ay - dy * t, z - az - dz * t) - (ra + (rb - ra) * t);
    }
    static double Sphere(double x, double y, double z, double cx, double cy, double cz, double r) { return Len(x - cx, y - cy, z - cz) - r; }
    // a log along x, oval across, its ends rounded
    static double LogX(double x, double y, double z, double cx, double cy, double cz, double hl, double ry, double rz, double round) {
        double e = (Len((y - cy) / ry, (z - cz) / rz) - 1) * Math.Min(ry, rz);
        double dx = Math.Abs(x - cx) - hl;
        double a = e + round, b = dx + round;
        return Len(Math.Max(a, 0), Math.Max(b, 0)) + Math.Min(Math.Max(a, b), 0) - round;
    }
    static double SMin(double a, double b, double k) {
        double h = Clamp(0.5 + 0.5 * (b - a) / k, 0, 1);
        return b * (1 - h) + a * h - k * h * (1 - h);
    }

    // ── the cuts: the material at a point (16ths), NONE outside ──
    static byte Cut(string id, double x, double y, double z) {
        switch (id) {
            case "beef_chuck": {
                double body = RoundBox(x, y, z, 8, 4.2, 8, 6.2, 4.4, 5.0, 2.2) + Lumpy(x, y, z, 11, 0.7);
                if (Capsule(x, y, z, 1.4, 2.6, 6.6, 4.6, 6.2, 6.0, 0.55, 0.45) < 0) return BONE;
                if (body >= 0) return NONE;
                double depth = -body;
                // a fat edge down one long side, and odd patches of it on top
                if (z < 4.6 && depth < 0.9) return FAT;
                if (y > 6.5 && depth < 0.6 && N3(x * 0.4, 0, z * 0.4, 13) > 0.62) return FAT;
                if (depth < 2.5 && Math.Abs(z - 8 - 1.6 * Math.Sin(x * 0.45)) < 0.3) return FAT;
                if (depth < 2.5 && Math.Abs(y - 3.2 - 0.8 * Math.Sin(z * 0.6)) < 0.28) return FAT;
                return MEAT;
            }
            case "beef_brisket": {
                double top = 3.4 + 1.5 * SmoothStep(4, 13, x) + Lumpy(x, 0, z, 21, 0.35);
                double body = RoundBox(x, y, z, 8, 2.6, 8, 7.0, 3.2, 5.2, 1.4);
                body = Math.Max(body, y - top);
                if (body >= 0) return NONE;
                if (y > top - 1.0) return FAT;
                if (Math.Abs(y - (1.7 + 0.3 * Math.Sin(x * 0.5))) < 0.28) return FAT;
                return MEAT;
            }
            case "beef_round": {
                double body = LogX(x, y, z, 8, 4.0, 8, 5.8, 4.4, 4.8, 1.2) + Lumpy(x, y, z, 31, 0.5);
                double r = Len(y - 4.0, z - 8.4);
                if (r < 0.9 && Math.Abs(x - 8) < 6.2) return r < 0.45 ? MARROW : BONE;
                if (body >= 0) return NONE;
                if (-body < 0.5 && z > 12.6) return FAT;
                if (-body < 2.4 && Math.Abs(Len(y - 4.0, z - 8) - 2.6 - 0.3 * Math.Sin(x)) < 0.25) return FAT;
                return MEAT;
            }
            case "beef_rib": {
                double body = RoundBox(x, y, z, 8, 3.6, 6.4, 6.6, 3.8, 3.7, 1.6) + Lumpy(x, y, z, 41, 0.45);
                double[] ribs = { 3.0, 6.3, 9.6, 12.9 };
                double bone = 1e9;
                foreach (double xi in ribs) {
                    body = SMin(body, Capsule(x, y, z, xi, 2.4, 8.5, xi, 2.8, 11.4, 1.5, 1.2), 0.8);
                    bone = Math.Min(bone, Capsule(x, y, z, xi, 2.5, 8.0, xi, 3.0, 13.4, 0.85, 0.8));
                    bone = Math.Min(bone, Capsule(x, y, z, xi, 3.0, 13.4, xi, 8.6, 14.6, 0.8, 0.7));
                    bone = Math.Min(bone, Sphere(x, y, z, xi, 8.8, 14.7, 0.95));
                }
                if (bone < 0) return BONE;
                if (body >= 0) return NONE;
                double e = Len((y - 3.0) / 2.3, (z - 6.6) / 2.6);
                if (e < 1) return LEAN;
                if (e < 1.18) return FAT;
                if (-body < 1.25 && (y > 4.6 || z < 4.2)) return FAT;
                return MEAT;
            }
            case "pork_shoulder": {
                double body = RoundBox(x, y, z, 8, 4.1, 8, 6.0, 4.5, 5.4, 2.6) + Lumpy(x, y, z, 51, 1.0);
                if (Capsule(x, y, z, 1.6, 2.8, 7.0, 4.4, 6.2, 6.4, 0.55, 0.45) < 0) return BONE;
                if (body >= 0) return NONE;
                double depth = -body;
                if (x > 3.4 && y > 2.4) { if (depth < 0.5) return SKIN; if (depth < 1.4) return FAT; }
                if (depth < 2.4 && Math.Abs(y - 3.0 - 0.9 * Math.Sin(z * 0.5 + x * 0.2)) < 0.28) return FAT;
                return MEAT;
            }
            case "pork_belly": {
                double body = RoundBox(x, y, z, 8, 2.1, 8, 7.0, 2.5, 5.2, 0.9) + Lumpy(x, y, z, 61, 0.25);
                if (body >= 0) return NONE;
                double w = y + 0.28 * Math.Sin(x * 0.6 + z * 0.25) + 0.18 * Math.Sin(z * 0.9 + x * 0.15);
                if (w > 4.0) return SKIN;
                if (w > 3.1) return FAT;
                if (w > 2.1) return MEAT;
                if (w > 1.5) return FAT;
                if (w > 0.6) return MEAT;
                if (w > 0.1) return FAT;
                return MEAT;
            }
            case "pork_ham": {
                double big = Ellipsoid(x, y, z, 6.3, 4.0, 8, 5.2, 4.8, 5.2);
                double shank = Capsule(x, y, z, 9.5, 3.6, 8, 13.8, 3.0, 8, 3.4, 1.8);
                double body = SMin(big, shank, 1.5) + Lumpy(x, y, z, 71, 0.5);
                double r = Len(y - (4.2 - (x - 1.5) * 0.074), z - 8.1);
                if (Sphere(x, y, z, 15.1, 3.1, 8, 1.35) < 0) return BONE;
                if (r < 0.95 && x > 1.4 && x < 15.0) return x < 2.1 && r < 0.5 ? MARROW : BONE;
                if (body >= 0 || x < 1.6) return NONE;
                double rind = -body;
                if (x > 2.2) { if (rind < 0.5) return SKIN; if (rind < 1.4) return FAT; }
                else if (rind < 1.0) return FAT;
                if (rind < 2.6 && Math.Abs(Len(y - 4.0, z - 8) - 2.4 - 0.4 * Math.Sin(x * 0.8)) < 0.25) return FAT;
                return MEAT;
            }
            case "pork_loin": {
                double body = LogX(x, y, z, 8, 3.4, 8, 6.6, 3.5, 3.5, 0.8) + Lumpy(x, y, z, 81, 0.35);
                for (double vx = 2.4; vx <= 13.8; vx += 2.3) if (Sphere(x, y, z, vx, 1.1, 11.4, 0.95) < 0) return BONE;
                if (body >= 0) return NONE;
                double e = Len((y - 3.0) / 2.3, (z - 7.8) / 2.4);
                if (e < 1) return LEAN;
                if (e < 1.15) return FAT;
                if (-body < 0.9 && y > 4.4) return FAT;
                return MEAT;
            }
            case "mutton_shoulder": {
                double body = RoundBox(x, y, z, 8, 3.5, 8, 5.4, 3.8, 4.6, 2.2) + Lumpy(x, y, z, 91, 0.9);
                if (Capsule(x, y, z, 2.2, 2.4, 7.2, 4.8, 5.4, 6.6, 0.5, 0.4) < 0) return BONE;
                if (body >= 0) return NONE;
                double depth = -body;
                if (y > 4.4 && depth < 0.7 && N3(x * 0.4, y * 0.4, z * 0.4, 15) > 0.42) return FAT;
                if (depth < 2.2 && Math.Abs(z - 8 - 1.2 * Math.Sin(x * 0.5)) < 0.28) return FAT;
                return MEAT;
            }
            case "mutton_leg": {
                double big = Ellipsoid(x, y, z, 6.0, 3.0, 8, 4.4, 3.9, 4.2);
                double shank = Capsule(x, y, z, 8.8, 2.8, 8, 13.2, 2.4, 8, 2.6, 1.3);
                double body = SMin(big, shank, 1.3) + Lumpy(x, y, z, 101, 0.4);
                double r = Len(y - (3.2 - (x - 1.9) * 0.053), z - 8.0);
                if (Sphere(x, y, z, 15.2, 2.5, 8, 1.05) < 0) return BONE;
                if (r < 0.7 && x > 1.9 && x < 15.0) return x < 2.5 && r < 0.35 ? MARROW : BONE;
                if (body >= 0 || x < 2.0) return NONE;
                double rind = -body;
                if (x <= 2.6 && rind < 0.8) return FAT;
                if (rind < 0.6 && N3(x * 0.35, y * 0.35, z * 0.35, 17) > 0.45) return FAT;
                return MEAT;
            }
            case "mutton_rack": {
                double body = RoundBox(x, y, z, 8, 2.8, 6.6, 6.0, 3.0, 2.9, 1.3) + Lumpy(x, y, z, 111, 0.35);
                double bone = 1e9;
                for (int i = 0; i < 6; i++) {
                    double xi = 2.8 + i * 2.08;
                    body = SMin(body, Capsule(x, y, z, xi, 2.0, 8.4, xi, 2.3, 10.6, 1.0, 0.85), 0.6);
                    bone = Math.Min(bone, Capsule(x, y, z, xi, 2.0, 8.4, xi, 2.5, 12.2, 0.62, 0.6));
                    bone = Math.Min(bone, Capsule(x, y, z, xi, 2.5, 12.2, xi, 7.4, 13.4, 0.6, 0.55));
                    bone = Math.Min(bone, Sphere(x, y, z, xi, 7.55, 13.45, 0.72));
                }
                if (bone < 0) return BONE;
                if (body >= 0) return NONE;
                double e = Len((y - 2.4) / 1.7, (z - 7.0) / 1.9);
                if (e < 1) return LEAN;
                if (e < 1.2) return FAT;
                if (-body < 1.0 && (y > 3.4 || z < 4.4)) return FAT;
                return MEAT;
            }
            case "chicken_leg_quarter": {
                double thigh = Ellipsoid(x, y, z, 6.0, 1.7, 7.4, 3.6, 2.4, 3.0);
                double drum = Capsule(x, y, z, 8.6, 1.6, 8.2, 12.4, 1.3, 9.0, 1.9, 0.95);
                double body = SMin(thigh, drum, 1.0) + Lumpy(x, y, z, 121, 0.3);
                if (Capsule(x, y, z, 12.0, 1.3, 8.9, 13.8, 1.3, 9.3, 0.42, 0.42) < 0) return BONE;
                if (Sphere(x, y, z, 14.0, 1.3, 9.35, 0.62) < 0) return BONE;
                if (body >= 0) return NONE;
                return -body < 0.35 ? SKIN : MEAT;
            }
            case "chicken_breast": {
                double a = Capsule(x, y, z, 4.0, 0.6, 6.4, 10.4, 0.9, 6.3, 1.7, 2.7);
                double b = Capsule(x, y, z, 4.0, 0.6, 9.6, 10.4, 0.9, 9.7, 1.7, 2.7);
                double body = SMin(a, b, 0.6) + Lumpy(x, y, z, 131, 0.25);
                if (body >= 0) return NONE;
                if (-body < 0.3 && z > 8.2 && y > 1.0) return SKIN;
                return MEAT;
            }
            case "rabbit_hind_legs": {
                double a = Capsule(x, y, z, 4.0, 1.5, 6.6, 11.6, 0.95, 7.0, 1.5, 0.62);
                double b = Capsule(x, y, z, 4.2, 1.5, 9.6, 11.8, 0.95, 9.2, 1.5, 0.62);
                double pelvis = RoundBox(x, y, z, 3.6, 1.2, 8.1, 1.2, 1.1, 2.4, 0.8);
                double body = SMin(SMin(a, b, 0.5), pelvis, 0.6) + Lumpy(x, y, z, 141, 0.15);
                if (Capsule(x, y, z, 11.4, 0.95, 7.0, 13.0, 0.95, 7.1, 0.3, 0.3) < 0 || Sphere(x, y, z, 13.2, 0.95, 7.1, 0.45) < 0) return BONE;
                if (Capsule(x, y, z, 11.6, 0.95, 9.2, 13.2, 0.95, 9.1, 0.3, 0.3) < 0 || Sphere(x, y, z, 13.4, 0.95, 9.1, 0.45) < 0) return BONE;
                if (body >= 0) return NONE;
                return -body < 0.28 ? SKIN : MEAT;
            }
            case "rabbit_saddle": {
                double a = LogX(x, y, z, 8, 1.4, 7.0, 4.6, 1.5, 1.15, 0.5);
                double b = LogX(x, y, z, 8, 1.4, 9.0, 4.6, 1.5, 1.15, 0.5);
                double body = SMin(a, b, 0.4) + Lumpy(x, y, z, 151, 0.12);
                for (double vx = 3.9; vx <= 12.2; vx += 1.1) if (Sphere(x, y, z, vx, 0.55, 8.0, 0.55) < 0) return BONE;
                if (body >= 0) return NONE;
                if (-body < 0.28 && y > 1.1) return SKIN;
                return MEAT;
            }
        }
        return NONE;
    }

    // ── a quick look (not used in game): the voxels drawn as little cubes from above and the front right ──
    public static System.Drawing.Bitmap Preview(string id, double res, double[][] colors, int size) {
        int n = (int)Math.Round(16 / res);
        var bmp = new System.Drawing.Bitmap(size, size);
        var g = System.Drawing.Graphics.FromImage(bmp);
        g.Clear(System.Drawing.Color.FromArgb(139, 139, 139));
        double s = size / 30.0 * res;
        double cx = size / 2.0, cy = size * 0.62;
        byte[] v = new byte[n * n * n];
        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) for (int k = 0; k < n; k++) v[(i * n + j) * n + k] = Cut(id, (i + 0.5) * res, (j + 0.5) * res, (k + 0.5) * res);
        Func<int, int, int, bool> solid = (i, j, k) => i >= 0 && j >= 0 && k >= 0 && i < n && j < n && k < n && v[(i * n + j) * n + k] != NONE;
        Func<double, double, double, System.Drawing.PointF> P = (x, y, z) => new System.Drawing.PointF(
            (float)(cx + (x - z) * s * 0.866), (float)(cy - y * s + (x + z - n) * s * 0.5));
        for (int sum = 0; sum < 2 * n; sum++) for (int j = 0; j < n; j++) for (int i = 0; i < n; i++) {
            int k = sum - i;
            if (k < 0 || k >= n || !solid(i, j, k)) continue;
            double[] c = colors[v[(i * n + j) * n + k]];
            Action<System.Drawing.PointF[], double> quad = (pts, shade) => {
                using (var br = new System.Drawing.SolidBrush(System.Drawing.Color.FromArgb(255, (int)Math.Min(255, c[0] * shade), (int)Math.Min(255, c[1] * shade), (int)Math.Min(255, c[2] * shade))))
                    g.FillPolygon(br, pts);
            };
            if (!solid(i, j + 1, k)) quad(new[] { P(i, j + 1, k), P(i + 1, j + 1, k), P(i + 1, j + 1, k + 1), P(i, j + 1, k + 1) }, 1.0);
            if (!solid(i + 1, j, k)) quad(new[] { P(i + 1, j, k), P(i + 1, j + 1, k), P(i + 1, j + 1, k + 1), P(i + 1, j, k + 1) }, 0.8);
            if (!solid(i, j, k + 1)) quad(new[] { P(i, j, k + 1), P(i + 1, j, k + 1), P(i + 1, j + 1, k + 1), P(i, j + 1, k + 1) }, 0.62);
        }
        g.Dispose();
        return bmp;
    }

    // ── voxels to boxes ──
    static string F(double v) { return Math.Round(v, 4).ToString(CultureInfo.InvariantCulture); }

    public static SculptResult Build(string id, double res, string texPrefix) {
        int n = (int)Math.Round(16 / res);
        byte[] v = new byte[n * n * n];
        bool[] used = new bool[7];
        double[] bounds = { 16, 16, 16, 0, 0, 0 };
        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) for (int k = 0; k < n; k++) {
            byte m = Cut(id, (i + 0.5) * res, (j + 0.5) * res, (k + 0.5) * res);
            v[(i * n + j) * n + k] = m;
            if (m == NONE) continue;
            used[m] = true;
            bounds[0] = Math.Min(bounds[0], i * res); bounds[1] = Math.Min(bounds[1], j * res); bounds[2] = Math.Min(bounds[2], k * res);
            bounds[3] = Math.Max(bounds[3], (i + 1) * res); bounds[4] = Math.Max(bounds[4], (j + 1) * res); bounds[5] = Math.Max(bounds[5], (k + 1) * res);
        }
        Func<int, int, int, byte> at = (i, j, k) => i < 0 || j < 0 || k < 0 || i >= n || j >= n || k >= n ? NONE : v[(i * n + j) * n + k];
        bool[] done = new bool[v.Length];
        StringBuilder els = new StringBuilder();
        int boxes = 0;
        for (int j = 0; j < n; j++) for (int i = 0; i < n; i++) for (int k = 0; k < n; k++) {
            int idx = (i * n + j) * n + k;
            byte m = v[idx];
            if (m == NONE || done[idx]) continue;
            // grow along z, then x, then y, while every voxel is the same material and not yet taken
            int k1 = k + 1;
            while (k1 < n && v[(i * n + j) * n + k1] == m && !done[(i * n + j) * n + k1]) k1++;
            int i1 = i + 1;
            while (i1 < n) {
                bool ok = true;
                for (int kk = k; kk < k1 && ok; kk++) { int q = (i1 * n + j) * n + kk; ok = v[q] == m && !done[q]; }
                if (!ok) break;
                i1++;
            }
            int j1 = j + 1;
            while (j1 < n) {
                bool ok = true;
                for (int ii = i; ii < i1 && ok; ii++) for (int kk = k; kk < k1 && ok; kk++) { int q = (ii * n + j1) * n + kk; ok = v[q] == m && !done[q]; }
                if (!ok) break;
                j1++;
            }
            for (int ii = i; ii < i1; ii++) for (int jj = j; jj < j1; jj++) for (int kk = k; kk < k1; kk++) done[(ii * n + jj) * n + kk] = true;

            // which faces show: any with a gap next to it
            bool west = false, east = false, down = false, up = false, north = false, south = false;
            for (int jj = j; jj < j1; jj++) for (int kk = k; kk < k1; kk++) { if (at(i - 1, jj, kk) == NONE) west = true; if (at(i1, jj, kk) == NONE) east = true; }
            for (int ii = i; ii < i1; ii++) for (int kk = k; kk < k1; kk++) { if (at(ii, j - 1, kk) == NONE && j > 0) down = true; if (at(ii, j1, kk) == NONE) up = true; }
            for (int ii = i; ii < i1; ii++) for (int jj = j; jj < j1; jj++) { if (at(ii, jj, k - 1) == NONE) north = true; if (at(ii, jj, k1) == NONE) south = true; }
            if (j == 0) down = true;
            if (!(west || east || down || up || north || south)) continue;

            double x0 = i * res, y0 = j * res, z0 = k * res, x1 = i1 * res, y1 = j1 * res, z1 = k1 * res;
            string tex = "#" + Names[m];
            StringBuilder faces = new StringBuilder();
            Action<string, double, double, double, double, string> face = (dir, u0, v0, u1, v1, cull) => {
                if (faces.Length > 0) faces.Append(',');
                faces.Append("\"").Append(dir).Append("\":{\"texture\":\"").Append(tex).Append("\",\"uv\":[")
                    .Append(F(u0)).Append(',').Append(F(v0)).Append(',').Append(F(u1)).Append(',').Append(F(v1)).Append(']');
                if (cull != null) faces.Append(",\"cullface\":\"").Append(cull).Append('"');
                faces.Append('}');
            };
            if (north) face("north", 16 - x1, 16 - y1, 16 - x0, 16 - y0, null);
            if (south) face("south", x0, 16 - y1, x1, 16 - y0, null);
            if (west) face("west", z0, 16 - y1, z1, 16 - y0, null);
            if (east) face("east", 16 - z1, 16 - y1, 16 - z0, 16 - y0, null);
            if (up) face("up", x0, z0, x1, z1, null);
            if (down) face("down", x0, 16 - z1, x1, 16 - z0, j == 0 ? "down" : null);
            if (els.Length > 0) els.Append(",\n    ");
            els.Append("{\"from\":[").Append(F(x0)).Append(',').Append(F(y0)).Append(',').Append(F(z0))
                .Append("],\"to\":[").Append(F(x1)).Append(',').Append(F(y1)).Append(',').Append(F(z1))
                .Append("],\"faces\":{").Append(faces).Append("}}");
            boxes++;
        }

        StringBuilder json = new StringBuilder();
        // meat_slab_base: block/block plus a bigger "ground" size, so a dropped slab looks like a big cut of meat
        json.Append("{\n  \"parent\": \"capitalism:block/meat_slab_base\",\n  \"textures\": {\n");
        json.Append("    \"particle\": \"").Append(texPrefix).Append("meat\"");
        for (int m = 1; m < 7; m++) {
            if (!used[m]) continue;
            string name = m == BONE || m == MARROW ? "capitalism:block/meat_slab/" + Names[m] : texPrefix + Names[m];
            json.Append(",\n    \"").Append(Names[m]).Append("\": \"").Append(name).Append('"');
        }
        json.Append("\n  },\n  \"elements\": [\n    ").Append(els).Append("\n  ]\n}\n");
        return new SculptResult { Json = json.ToString(), Bounds = bounds, Used = used, Boxes = boxes };
    }
}
