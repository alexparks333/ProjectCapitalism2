# Draws the Blood Moon building blocks (textures/block/*.png, 16x16; Town.md > The Blood Merchant):
#  - bloodstone_bricks: big chiselled bricks of polished black stone (a cloudy red-violet sheen, bevelled edges, red
#    flecks), blood glowing in the seams and in cracks running into the bricks (bloodstone_bricks_glow.png: drawn
#    over it at full brightness);
#  - blood_crystal_block: see-through blood-red crystal, darker rim, pale diagonal facets and a few sparkles.
# (C# inside: run with powershell -File tools/make_blood_blocks.ps1)
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Imaging;

public static class BloodBlocks
{
    static int Cl(double v) { return v < 0 ? 0 : v > 255 ? 255 : (int)v; }

    public static void Make(string dir)
    {
        var rng = new Random(11);
        // ── bloodstone bricks: big chiselled bricks of polished black stone, blood glowing in the seams and cracks ──
        var b = new Bitmap(16, 16, PixelFormat.Format32bppArgb);
        var glow = new Bitmap(16, 16, PixelFormat.Format32bppArgb);
        bool[,] seam = new bool[16, 16];
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++)
            {
                int row = y / 8, iy = y % 8;
                int ix = (x + (row == 1 ? 8 : 0)) % 16;
                seam[x, y] = iy == 7 || ix == 15;
                double r, g, bl;
                if (seam[x, y]) { r = 90; g = 8; bl = 18; }
                else
                {
                    // polished black stone, cloudy, a faint red-violet sheen
                    double cloud = Math.Sin(x * 0.9 + y * 0.4 + row * 2) * 0.5 + Math.Sin(x * 0.3 - y * 1.1) * 0.5;
                    r = 30 + cloud * 6; g = 22 + cloud * 4; bl = 30 + cloud * 6;
                    // chiselled bevel: lit top and left, shadowed bottom and right
                    if (iy == 0 || ix == 0) { r += 38; g += 30; bl += 38; }
                    else if (iy == 1 || ix == 1) { r += 14; g += 10; bl += 14; }
                    if (iy == 6 || ix == 14) { r -= 14; g -= 12; bl -= 14; }
                    int n = rng.Next(-4, 5); r += n; g += n; bl += n;
                    if (rng.Next(14) == 0) { r += 30; g += 4; bl += 10; }   // a fleck of red in the stone
                }
                b.SetPixel(x, y, Color.FromArgb(255, Cl(r), Cl(g), Cl(bl)));
            }
        // cracks running from the seams into the bricks
        int[][] cracks = {
            new[] { 3, 7, 3, 6, 4, 5, 4, 4, 5, 3 },
            new[] { 15, 4, 14, 4, 13, 3, 12, 3 },
            new[] { 10, 15, 10, 14, 11, 13, 11, 12, 12, 11, 12, 10 },
            new[] { 7, 9, 6, 10, 6, 11, 5, 12 },
            new[] { 0, 12, 1, 12, 2, 13 } };
        foreach (var c in cracks)
            for (int i = 0; i + 1 < c.Length; i += 2)
            {
                b.SetPixel(c[i], c[i + 1], Color.FromArgb(255, 70, 6, 14));
                int k = i / 2;
                glow.SetPixel(c[i], c[i + 1], k == 0 ? Color.FromArgb(255, 255, 70, 70) : Color.FromArgb(255, 200 - k * 18, 16, 34));
            }
        // the seams: blood glowing, brighter where they meet
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++)
            {
                if (!seam[x, y]) continue;
                bool cross = seam[(x + 1) % 16, y] && seam[x, (y + 1) % 16] || seam[(x + 15) % 16, y] && seam[x, (y + 15) % 16];
                int wobble = rng.Next(0, 30);
                glow.SetPixel(x, y, cross ? Color.FromArgb(255, 255, 150, 130) : Color.FromArgb(255, 220 + wobble, 30 + wobble / 2, 46));
            }
        b.Save(System.IO.Path.Combine(dir, "bloodstone_bricks.png"), ImageFormat.Png);
        glow.Save(System.IO.Path.Combine(dir, "bloodstone_bricks_glow.png"), ImageFormat.Png);

        // ── blood crystal ──
        var c2 = new Bitmap(16, 16, PixelFormat.Format32bppArgb);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++)
            {
                double r = 168, g = 18, bl = 40, a = 165;
                // facets: bands running corner to corner, lighter and more solid
                int d = (x + y) % 8, e = (x - y + 16) % 6;
                if (d == 0 || d == 1) { r = 220; g = 50; bl = 70; a = 190; }
                if (e == 0) { r = 196; g = 34; bl = 56; a = 180; }
                // darker toward the middle, like depth
                double cx = x - 7.5, cy = y - 7.5, dist = Math.Sqrt(cx * cx + cy * cy);
                if (dist < 4.0) { r *= 0.85; g *= 0.8; bl *= 0.85; }
                // the rim
                bool rim = x == 0 || y == 0 || x == 15 || y == 15;
                if (rim) { r = 110; g = 8; bl = 26; a = 235; }
                if ((x == 1 || y == 1) && !rim) { r = 255; g = 120; bl = 140; a = 215; }
                int n = rng.Next(-8, 9); r += n; g += n * 0.3; bl += n * 0.3;
                c2.SetPixel(x, y, Color.FromArgb(Cl(a), Cl(r), Cl(g), Cl(bl)));
            }
        foreach (var s in new[] { new[] { 4, 3 }, new[] { 11, 9 }, new[] { 6, 12 }, new[] { 12, 3 } })
            c2.SetPixel(s[0], s[1], Color.FromArgb(240, 255, 225, 230));
        c2.Save(dir + "\\blood_crystal_block.png", ImageFormat.Png);
    }
}
'@
[BloodBlocks]::Make((Resolve-Path (Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures\block')).Path)
'saved'
