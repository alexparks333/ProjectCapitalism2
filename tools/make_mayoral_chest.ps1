# Draws the Mayoral Chest's texture (textures/entity/mayoral_chest.png, 128x64) for client/MayoralChestModel: dark
# mahogany planks (a fine grain, darker seams), crimson velvet inside (the base's top, the lid's underside), polished gold
# bands, corner caps and lock plate (a dark keyhole), and the town crest on the front (white enamel in a gold rim, a
# crimson shield on it, like the Town Hall Mailbox's).
# The boxes below match MayoralChestModel's texOffs and sizes. A box's faces at (u, v) with w, h, d: the world top at
# (u+d, v), the world underside at (u+d+w, v), then along v+d: back (u), side (u+d), front (u+d+w), side (u+2d+w).
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Imaging;

public static class MayoralTex
{
    static Bitmap img;
    static Random rng = new Random(77);
    static int Cl(double v) { return v < 0 ? 0 : v > 255 ? 255 : (int)v; }
    static void P(int x, int y, int r, int g, int b) { if (x >= 0 && x < 128 && y >= 0 && y < 64) img.SetPixel(x, y, Color.FromArgb(255, Cl(r), Cl(g), Cl(b))); }
    static void P(int x, int y, int[] c) { P(x, y, c[0], c[1], c[2]); }
    static int[] Add(int[] c, int k) { return new[] { c[0] + k, c[1] + k, c[2] + k }; }
    static void Rect(int x, int y, int w, int h, int[] c, int noise)
    {
        for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) { int n = rng.Next(-noise, noise + 1); P(x + i, y + j, c[0] + n, c[1] + n, c[2] + n); }
    }
    // mahogany: planks running across (seams every 4 pixels down the sides, every 5 across the top), a fine grain
    static void Wood(int x, int y, int w, int h, bool top)
    {
        int[] wood = { 92, 40, 28 };
        for (int j = 0; j < h; j++) for (int i = 0; i < w; i++)
        {
            int n = rng.Next(-5, 6) + ((top ? i : j) % 2 == 0 ? 0 : -3) + (rng.Next(9) == 0 ? -8 : 0);
            bool seam = top ? (i % 5 == 4) : (j % 4 == 3);
            int[] c = seam ? new[] { 54, 22, 16 } : wood;
            P(x + i, y + j, c[0] + n, c[1] + n / 2, c[2] + n / 2);
        }
    }
    static void Velvet(int x, int y, int w, int h)
    {
        for (int j = 0; j < h; j++) for (int i = 0; i < w; i++)
        {
            int n = rng.Next(-8, 9) + (((i + j) % 3 == 0) ? 6 : 0);
            P(x + i, y + j, 136 + n, 18 + n / 3, 34 + n / 3);
        }
    }
    static void Gold(int x, int y, int w, int h)
    {
        for (int j = 0; j < h; j++) for (int i = 0; i < w; i++)
        {
            int shine = (j == 0 || i == 0) ? 22 : (j == h - 1 || i == w - 1) ? -30 : 0;
            int n = rng.Next(-6, 7) + shine;
            P(x + i, y + j, 226 + n, 182 + n, 72 + n / 2);
        }
    }

    public static void Make(string path)
    {
        img = new Bitmap(128, 64, PixelFormat.Format32bppArgb);
        int[] gold = { 232, 190, 72 }, goldDark = { 150, 112, 34 }, enamel = { 244, 240, 230 }, crimson = { 150, 20, 36 };

        // the base (0,0; 14 x 10 x 14): mahogany outside, velvet on its top (seen when the lid's up)
        Wood(28, 0, 14, 14, true);          // its underside
        Velvet(14, 0, 14, 14);              // its top: the velvet lining
        for (int i = 0; i < 14; i++) { P(14 + i, 0, 70, 30, 22); P(14 + i, 13, 70, 30, 22); P(14, i, 70, 30, 22); P(27, i, 70, 30, 22); }
        Wood(0, 14, 56, 10, false);          // its four sides
        for (int i = 0; i < 56; i++) P(i, 14, 120, 58, 40);  // a lighter lip along the top edge

        // the lid (0,24; 14 x 4 x 14): mahogany on top, velvet beneath
        Wood(14, 24, 14, 14, true);
        Velvet(28, 24, 14, 14);
        Wood(0, 38, 56, 4, false);
        for (int i = 0; i < 56; i++) P(i, 41, 60, 26, 18);   // a dark edge at its rim

        // the gold bands (60,0 base, 60,24 lid) and the corner caps (96,0)
        Gold(60, 0, 33, 13);
        Gold(60, 24, 33, 7);
        Gold(96, 0, 10, 6);
        for (int y = 0; y < 13; y += 4) { P(61, y + 2, 255, 236, 150); P(76, y + 2, 255, 236, 150); P(90, y + 2, 255, 236, 150); }  // rivets

        // the lock plate (96,8; 0.6 x 3.2 x 3): gold, its front with a dark keyhole
        Gold(96, 8, 8, 7);
        P(100, 11, 30, 18, 12); P(101, 11, 30, 18, 12); P(100, 12, 30, 18, 12); P(101, 12, 30, 18, 12); P(100, 13, 30, 18, 12);

        // the crest (96,16; 0.5 x 5 x 5): its front (101..106, 21..26) white enamel in a gold rim, a crimson shield
        Gold(96, 16, 12, 11);
        for (int j = 0; j < 6; j++) for (int i = 0; i < 6; i++)
        {
            int x = 101 + i, y = 21 + j;
            bool rim = i == 0 || j == 0 || i == 5 || j == 5;
            P(x, y, rim ? goldDark : enamel);
        }
        P(103, 22, crimson); P(104, 22, crimson); P(103, 23, crimson); P(104, 23, crimson); P(102, 22, crimson); P(105, 22, crimson);
        P(103, 24, crimson); P(104, 24, crimson); P(103, 25, 110, 14, 26); P(104, 25, 110, 14, 26);
        P(103, 23, gold); // a gold boss on the shield

        img.Save(path, ImageFormat.Png);
    }
}
'@
$root = Split-Path -Parent $PSScriptRoot
[MayoralTex]::Make((Join-Path $root 'src\main\resources\assets\capitalism\textures\entity\mayoral_chest.png'))
Write-Output 'mayoral_chest.png written'
