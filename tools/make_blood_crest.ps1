# Draws the Blood Moon Crest and its three broken thirds (textures/item/*.png, 32x32; Events.md > Trophies):
# a round crest with a dark iron rim, gold studs and a gold inner band, a dark blood-red field, a blood moon in the
# middle with a glow round it, and one boss's sign in each third: the Crimson Revenant's greatsword (upper left), the
# Sanguine Witch's bat wing (upper right), Gorefang's fangs (the bottom). The whole crest has thin gold seams where the
# thirds join; each third is cut out along a jagged crack (glowing red-hot at the break) and moved to the middle of its
# own picture. (C# inside: run with powershell -File tools/make_blood_crest.ps1)
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Imaging;

public static class BloodCrest
{
    const int N = 32;
    const double CX = 16.0;
    static Random rng = new Random(7);
    static readonly double[] Cuts = { 270.0, 30.0, 150.0 };

    static int Cl(double v) { return v < 0 ? 0 : v > 255 ? 255 : (int)v; }
    static double[] Rgb(double r, double g, double b) { return new[] { r, g, b }; }
    static double[] Mix(double[] a, double[] b, double t) { return new[] { a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t }; }
    static double Ang(double dx, double dy) { double a = Math.Atan2(dy, dx) * 180 / Math.PI; return a < 0 ? a + 360 : a; }
    static double ADiff(double a, double b) { double d = (a - b) % 360; if (d < -180) d += 360; if (d > 180) d -= 360; return d; }
    static double[] Polar(double deg, double r) { return new[] { CX + Math.Cos(deg * Math.PI / 180) * r, CX + Math.Sin(deg * Math.PI / 180) * r }; }
    static double SegDist(double px, double py, double[] a, double[] b)
    {
        double vx = b[0] - a[0], vy = b[1] - a[1];
        double t = ((px - a[0]) * vx + (py - a[1]) * vy) / (vx * vx + vy * vy);
        t = Math.Max(0, Math.Min(1, t));
        double qx = a[0] + vx * t - px, qy = a[1] + vy * t - py;
        return Math.Sqrt(qx * qx + qy * qy);
    }
    static bool InTri(double px, double py, double[] a, double[] b, double[] c)
    {
        double s1 = (b[0] - a[0]) * (py - a[1]) - (b[1] - a[1]) * (px - a[0]);
        double s2 = (c[0] - b[0]) * (py - b[1]) - (c[1] - b[1]) * (px - b[0]);
        double s3 = (a[0] - c[0]) * (py - c[1]) - (a[1] - c[1]) * (px - c[0]);
        return (s1 >= 0 && s2 >= 0 && s3 >= 0) || (s1 <= 0 && s2 <= 0 && s3 <= 0);
    }

    // the jagged crack k (0 up, 1 lower right, 2 lower left) at radius r
    static double Cut(int k, double r) { return Cuts[k] + 6.0 * Math.Sin(r * 1.45 + k * 2.1) * Math.Min(1.0, r / 4.0); }

    // which third (0 upper left, 1 upper right, 2 bottom) and how far from the nearest crack, in pixels
    static void Piece(double dx, double dy, out int piece, out double near)
    {
        double r = Math.Sqrt(dx * dx + dy * dy), a = Ang(dx, dy);
        double c0 = Cut(0, r), c1 = Cut(1, r), c2 = Cut(2, r);
        near = 99;
        foreach (double c in new[] { c0, c1, c2 })
        {
            double d = ADiff(a, c);
            if (Math.Abs(d) < 90) near = Math.Min(near, Math.Abs(Math.Sin(d * Math.PI / 180)) * r);
        }
        if (ADiff(a, c2) >= 0 && ADiff(a, c0) < 0) piece = 0;
        else if (ADiff(a, c0) >= 0 && ADiff(a, c1) < 0) piece = 1;
        else piece = 2;
    }

    // the whole crest's colour at a pixel (null outside)
    static double[] Crest(int x, int y)
    {
        double px = x + 0.5, py = y + 0.5, dx = px - CX, dy = py - CX, r = Math.Sqrt(dx * dx + dy * dy);
        if (r > 15.3) return null;
        double light = -(dx + dy) / Math.Max(1.0, r);
        double a = Ang(dx, dy);
        double[] c;
        if (r > 12.7)
        {
            if (r <= 13.5) return Mix(Rgb(200, 150, 50), Rgb(255, 228, 130), Math.Max(0, light) * 0.6);   // gold inner band
            c = Mix(Rgb(62, 50, 58), Rgb(124, 108, 116), Math.Max(0, light) * 0.6);                    // iron rim
            if (r > 14.6) c = Rgb(26, 18, 22);
            for (int i = 0; i < 8; i++)
            {
                double[] s = Polar(i * 45 + 22.5, 14.0);
                double d = Math.Sqrt((px - s[0]) * (px - s[0]) + (py - s[1]) * (py - s[1]));
                if (d < 1.15) c = (px < s[0] && py < s[1]) ? Rgb(255, 238, 150) : Rgb(226, 176, 64);
            }
            return c;
        }
        // the field: dark blood red, darker to the edge, a little grain
        c = Mix(Rgb(84, 14, 26), Rgb(26, 8, 12), Math.Min(1.0, r / 12.7));
        int n = rng.Next(-5, 6); c = Rgb(c[0] + n, c[1] + n * 0.3, c[2] + n * 0.3);
        if (r > 3.8 && r < 5.8) c = Mix(c, Rgb(220, 40, 50), (5.8 - r) / 2.0 * 0.6);   // the moon's glow
        if (r <= 3.8)
        {
            c = Mix(Rgb(250, 60, 64), Rgb(150, 10, 22), r / 3.8);
            if (dx + dy < -2.2) c = Mix(c, Rgb(255, 140, 140), 0.5);
            double[][] craters = { new[] { 1.2, 0.4, 0.9 }, new[] { -0.8, 1.5, 0.7 }, new[] { 0.3, -1.6, 0.6 } };
            foreach (var cr in craters) if (Math.Sqrt((dx - cr[0]) * (dx - cr[0]) + (dy - cr[1]) * (dy - cr[1])) < cr[2]) c = Mix(c, Rgb(110, 6, 16), 0.55);
            return c;
        }
        // upper left: the Revenant's greatsword, point out to the rim: dark edge, steel, a red line down it, gold guard
        double[] tip = Polar(212, 12.4), hilt = Polar(212, 6.6), pom = Polar(212, 4.3);
        double db = SegDist(px, py, tip, hilt);
        if (db < 1.75) c = Rgb(14, 8, 12);
        if (db < 1.2) { c = Rgb(206, 214, 230); if (dy < dx * 0.55 - 1.0) c = Rgb(236, 240, 250); if (db < 0.42) c = Rgb(255, 64, 56); }
        double[] g1 = Polar(122, 3.0), g2 = Polar(302, 3.0);
        double[] ga = { hilt[0] + g1[0] - CX, hilt[1] + g1[1] - CX }, gb = { hilt[0] + g2[0] - CX, hilt[1] + g2[1] - CX };
        double dg = SegDist(px, py, ga, gb);
        if (dg < 1.3 && db >= 1.2) c = Rgb(14, 8, 12);
        if (dg < 0.8) c = Rgb(244, 196, 76);
        if (SegDist(px, py, hilt, pom) < 0.7) c = Rgb(96, 52, 30);
        // upper right: the Witch's bat wing: bone fingers fanning from a joint, crimson membrane, scalloped trailing edge
        double[] joint = Polar(328, 4.8);
        double rel = Ang(px - joint[0], py - joint[1]);
        double rj = Math.Sqrt((px - joint[0]) * (px - joint[0]) + (py - joint[1]) * (py - joint[1]));
        double da = ADiff(rel, 328);
        if (da > -38 && da < 38 && rj > 0.6)
        {
            double edge = 7.6 - 1.6 * Math.Abs(Math.Sin((da + 38) / 76.0 * 3 * Math.PI));
            if (rj < edge + 0.7) c = Rgb(14, 6, 10);
            if (rj < edge) c = Mix(Rgb(60, 10, 22), Rgb(196, 30, 56), rj / edge);
        }
        foreach (double f in new[] { 292.0, 328.0, 364.0 })
        {
            double[] fe = { joint[0] + Math.Cos(f * Math.PI / 180) * 7.8, joint[1] + Math.Sin(f * Math.PI / 180) * 7.8 };
            if (SegDist(px, py, joint, fe) < 0.62) c = Rgb(240, 230, 210);
        }
        if (rj < 1.0) c = Rgb(252, 246, 232);
        // the bottom: Gorefang's two great fangs rising from a red gum, bone white with a shadowed side, dark outline
        foreach (double fa in new[] { 70.0, 110.0 })
        {
            double[] b1 = Polar(fa - 11, 12.5), b2 = Polar(fa + 11, 12.5), t = Polar(fa, 4.9);
            double[] o1 = Polar(fa - 13.5, 12.9), o2 = Polar(fa + 13.5, 12.9), ot = Polar(fa, 4.1);
            if (InTri(px, py, o1, o2, ot)) c = Rgb(14, 8, 12);
            if (InTri(px, py, b1, b2, t)) c = ADiff(a, fa) > 1.5 ? Rgb(200, 186, 164) : Rgb(244, 236, 218);
        }
        if (r > 11.5 && Math.Abs(ADiff(a, 90)) < 44) c = Rgb(176, 26, 42);
        return c;
    }

    static void Outline(Bitmap bmp)
    {
        bool[,] solid = new bool[N, N];
        for (int y = 0; y < N; y++) for (int x = 0; x < N; x++) solid[x, y] = bmp.GetPixel(x, y).A > 0;
        int[][] o = { new[] { 1, 0 }, new[] { -1, 0 }, new[] { 0, 1 }, new[] { 0, -1 } };
        for (int y = 0; y < N; y++) for (int x = 0; x < N; x++)
        {
            if (solid[x, y]) continue;
            foreach (var d in o)
            {
                int xx = x + d[0], yy = y + d[1];
                if (xx >= 0 && xx < N && yy >= 0 && yy < N && solid[xx, yy]) { bmp.SetPixel(x, y, Color.FromArgb(255, 18, 4, 8)); break; }
            }
        }
    }

    static void Px(Bitmap b, int x, int y, double[] c) { if (x >= 0 && x < N && y >= 0 && y < N) b.SetPixel(x, y, Color.FromArgb(255, Cl(c[0]), Cl(c[1]), Cl(c[2]))); }

    public static void Make(string dir)
    {
        var col = new double[N, N][];
        var part = new int[N, N];
        var near = new double[N, N];
        for (int y = 0; y < N; y++) for (int x = 0; x < N; x++)
        {
            col[x, y] = Crest(x, y);
            int p; double d; Piece(x + 0.5 - CX, y + 0.5 - CX, out p, out d); part[x, y] = p; near[x, y] = d;
        }
        // the whole crest, thin gold seams where the thirds join
        var whole = new Bitmap(N, N, PixelFormat.Format32bppArgb);
        for (int y = 0; y < N; y++) for (int x = 0; x < N; x++)
        {
            var c = col[x, y]; if (c == null) continue;
            double dx = x + 0.5 - CX, dy = y + 0.5 - CX, r = Math.Sqrt(dx * dx + dy * dy);
            if (near[x, y] < 0.5 && r > 3.9 && r < 12.7) c = Mix(c, Rgb(240, 192, 84), 0.5);
            Px(whole, x, y, c);
        }
        Outline(whole);
        whole.Save(dir + "\\blood_moon_crest.png", ImageFormat.Png);
        // each third: cut out along its cracks, red-hot at the break, moved to the middle of its picture
        string[] names = { "revenant_crest", "sanguine_crest", "gorefang_crest" };
        double[] mid = { 210, 330, 90 };
        for (int k = 0; k < 3; k++)
        {
            var bmp = new Bitmap(N, N, PixelFormat.Format32bppArgb);
            int sx = (int)Math.Round(-Math.Cos(mid[k] * Math.PI / 180) * 6.0), sy = (int)Math.Round(-Math.Sin(mid[k] * Math.PI / 180) * 6.0);
            for (int y = 0; y < N; y++) for (int x = 0; x < N; x++)
            {
                var c = col[x, y]; if (c == null || part[x, y] != k) continue;
                if (near[x, y] < 0.7) c = Mix(Rgb(255, 96, 40), Rgb(255, 210, 130), rng.NextDouble() * 0.4);
                else if (near[x, y] < 1.3) c = Mix(c, Rgb(255, 60, 40), 0.3);
                Px(bmp, x + sx, y + sy, c);
            }
            Outline(bmp);
            bmp.Save(dir + "\\" + names[k] + ".png", ImageFormat.Png);
        }
    }
}
'@
[BloodCrest]::Make((Resolve-Path (Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures\item')).Path)
'saved'
