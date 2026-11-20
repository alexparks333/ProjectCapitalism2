# Draws the Investment Chest's texture (textures/entity/investment_chest.png, 128x128) for client/InvestmentChestModel:
# polished gold plating in riveted panels, dark iron straps and corner caps, a silver combination dial with its ticks and
# knob, a black coin slot, a heap of gold coins inside (the base's top, seen when the lid's up) and green felt under the
# lid; dark wooden posts and crossbar, grey chains, the deep green sign with a gold border and the cream tag (lettered by the renderer).
# The boxes below match InvestmentChestModel's texOffs and sizes. A box's faces at (u, v) with w, h, d: the world top at
# (u+d, v), the underside at (u+d+w, v), then along v+d: the sides in a row, 2(w+d) wide, h tall.
# Run: powershell -File tools\make_investment_chest.ps1
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Imaging;

public static class InvestTex
{
    static Bitmap img;
    static Random rng = new Random(1929);
    static int Cl(double v) { return v < 0 ? 0 : v > 255 ? 255 : (int)v; }
    static void P(int x, int y, int r, int g, int b) { if (x >= 0 && x < 128 && y >= 0 && y < 128) img.SetPixel(x, y, Color.FromArgb(255, Cl(r), Cl(g), Cl(b))); }
    static void P(int x, int y, int[] c) { P(x, y, c[0], c[1], c[2]); }

    static void Fill(int x, int y, int w, int h, int[] c, int noise)
    {
        for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) { int n = rng.Next(-noise, noise + 1); P(x + i, y + j, c[0] + n, c[1] + n, c[2] + n); }
    }
    // polished gold plate: a bright top-left edge, a shadowed bottom-right, a soft diagonal shine
    static void Gold(int x, int y, int w, int h, int panel)
    {
        for (int j = 0; j < h; j++) for (int i = 0; i < w; i++)
        {
            int n = rng.Next(-5, 6);
            int shine = ((i + j) % 11 < 2) ? 14 : 0;
            int edge = 0;
            if (panel > 0)
            {
                int pi = i % panel, pj = j % panel;
                if (pi == 0 || pj == 0) edge = 22; else if (pi == panel - 1 || pj == panel - 1) edge = -34;
            }
            P(x + i, y + j, 232 + n + shine + edge, 184 + n + shine + edge, 64 + n / 2 + edge / 2);
        }
    }
    static void Iron(int x, int y, int w, int h)
    {
        for (int j = 0; j < h; j++) for (int i = 0; i < w; i++)
        {
            int n = rng.Next(-5, 6);
            bool rivet = (i % 5 == 2) && (j % 4 == 1);
            if (rivet) P(x + i, y + j, 150, 150, 156); else P(x + i, y + j, 52 + n, 52 + n, 60 + n);
        }
    }
    static void Wood(int x, int y, int w, int h)
    {
        for (int j = 0; j < h; j++) for (int i = 0; i < w; i++)
        {
            int n = rng.Next(-5, 6) + (j % 3 == 0 ? -6 : 0);
            P(x + i, y + j, 70 + n, 44 + n, 26 + n / 2);
        }
    }
    static void Coins(int x, int y, int w, int h)
    {
        for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) { int n = rng.Next(-10, 6); P(x + i, y + j, 196 + n, 146 + n, 36 + n / 2); }
        for (int k = 0; k < w * h / 5; k++)
        {
            int cx = x + rng.Next(w), cy = y + rng.Next(h);
            P(cx, cy, 255, 226, 110); P(cx + 1, cy, 246, 200, 70); P(cx, cy + 1, 170, 120, 30);
        }
    }
    static void Felt(int x, int y, int w, int h)
    {
        for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) { int n = rng.Next(-6, 7); P(x + i, y + j, 26 + n, 78 + n, 42 + n); }
    }
    static void Cream(int x, int y, int w, int h)
    {
        for (int j = 0; j < h; j++) for (int i = 0; i < w; i++)
        {
            int n = rng.Next(-5, 6) + (j % 2 == 0 ? 0 : -3);
            P(x + i, y + j, 242 + n, 228 + n, 190 + n);
        }
    }

    public static void Make(string path)
    {
        img = new Bitmap(128, 128, PixelFormat.Format32bppArgb);

        // the base (0,0; 14 x 10 x 14): coins on its top (inside), gold plated sides in panels, dark iron underneath
        Coins(14, 0, 14, 14);
        Fill(28, 0, 14, 14, new[] { 40, 34, 20 }, 4);
        Gold(0, 14, 56, 10, 7);
        for (int i = 0; i < 56; i++) { P(i, 14, 255, 232, 140); P(i, 23, 120, 84, 20); }
        // "$" stamped either side of the dial on the front (u 28..42, v 14..24)
        foreach (int sx in new[] { 30, 38 })
        {
            int[] d = { 150, 104, 26 };
            P(sx + 1, 16, d); P(sx, 17, d); P(sx + 1, 17, d); P(sx + 2, 17, d); P(sx, 18, d);
            P(sx + 1, 19, d); P(sx + 2, 20, d); P(sx, 21, d); P(sx + 1, 21, d); P(sx + 2, 21, d); P(sx + 1, 22, d);
        }

        // the lid (0,24; 14 x 4 x 14): plated top, green felt beneath, plated rim
        Gold(14, 24, 14, 14, 7);
        Felt(28, 24, 14, 14);
        Gold(0, 38, 56, 4, 0);
        for (int i = 0; i < 56; i++) { P(i, 41, 120, 84, 20); P(i, 38, 255, 232, 140); }

        // iron straps (60,0 base; 60,24 lid) and corner caps (96,0)
        Iron(60, 0, 33, 13);
        Iron(60, 24, 33, 7);
        Iron(96, 0, 10, 6);

        // the dial (96,8; 0.6 x 4 x 4): brushed silver, a dark rim, tick marks round it
        for (int j = 0; j < 8; j++) for (int i = 0; i < 10; i++)
        {
            int n = rng.Next(-6, 7) + (i % 2 == 0 ? 6 : 0);
            P(96 + i, 8 + j, 196 + n, 200 + n, 208 + n);
        }
        for (int i = 0; i < 10; i++) { P(96 + i, 8, 90, 92, 100); P(96 + i, 15, 90, 92, 100); }
        for (int j = 8; j < 16; j++) { P(96, j, 90, 92, 100); P(105, j, 90, 92, 100); }
        P(97, 12, 40, 40, 46); P(98, 12, 40, 40, 46); P(101, 12, 40, 40, 46); P(102, 12, 40, 40, 46); P(104, 12, 40, 40, 46);
        // the knob (96,26) bright silver; the coin slot (96,16) black; the latch (96,32) gold with a keyhole
        Fill(96, 26, 4, 3, new[] { 230, 232, 238 }, 6);
        Fill(96, 16, 12, 6, new[] { 14, 12, 10 }, 3);
        Gold(96, 32, 8, 7, 0);
        P(100, 35, 30, 20, 10); P(100, 36, 30, 20, 10); P(101, 35, 30, 20, 10);

        // posts (0,44) and the crossbar (8,44): dark wood; chains (50,44): grey links
        Wood(0, 44, 4, 34);
        Wood(8, 44, 41, 19);
        for (int j = 44; j < 47; j++) for (int i = 50; i < 52; i++) P(i, j, (i + j) % 2 == 0 ? 120 : 80, (i + j) % 2 == 0 ? 120 : 80, (i + j) % 2 == 0 ? 128 : 88);

        // the sign (0,80; 0.8 x 6 x 18) and the tag (40,80; 0.6 x 2.6 x 7.2): cream paint, a brown painted border
        Felt(0, 80, 38, 24);   // the sign: deep money green (darker than the felt), so the gold letters pop
        for (int j = 80; j < 104; j++) for (int i = 0; i < 38; i++) { var c0 = img.GetPixel(i, j); P(i, j, c0.R * 6 / 10, c0.G * 6 / 10, c0.B * 6 / 10); }
        int[] brown = { 214, 168, 52 };   // (a gold painted border)
        foreach (int u in new[] { 0, 18 })   // its two big faces, 18 x 6 each, along v 98..104
        {
            int fu = u == 0 ? 0 : 19;
            for (int i = 0; i < 18; i++) { P(fu + i, 98, brown); P(fu + i, 103, brown); }
            for (int j = 98; j < 104; j++) { P(fu, j, brown); P(fu + 17, j, brown); }
        }
        Cream(40, 80, 18, 12);
        int[] red = { 150, 40, 30 };
        foreach (int fu in new[] { 40, 49 })
        {
            for (int i = 0; i < 8; i++) { P(fu + i, 88, red); P(fu + i, 91, red); }
        }
        P(41, 89, 40, 30, 20);   // the nail hole

        img.Save(path, ImageFormat.Png);
    }
}
'@
$root = Split-Path -Parent $PSScriptRoot
[InvestTex]::Make((Join-Path $root 'src\main\resources\assets\capitalism\textures\entity\investment_chest.png'))
Write-Output 'investment_chest.png written'
