# Draws the Blood Merchant's skin (textures/entity/blood_merchant.png, 128x64) and what glows on him
# (blood_merchant_glow.png: his eyes, the gem at his throat) for client/BloodMerchantModel: a deep black hood whose front
# rim is a frame round a pale, hollow-cheeked face (black hair in a widow's peak, red burning eyes in dark sockets, a
# thin dark mouth, two fangs), a high collar black outside and crimson inside with a gold edge, a burgundy doublet with
# a pale lace jabot, gold buttons and a belt, a long black cape with folds and a ragged hem lined in crimson, black
# sleeves ending in pale long-fingered hands with dark nails, black trousers and boots, a gold-set blood-red gem.
# The boxes below match BloodMerchantModel's texOffs and sizes (u, v, w, h, d).
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Imaging;

public static class BloodMerchantTex
{
    static Bitmap img, glow;
    static Random rng = new Random(31);
    static int Cl(double v) { return v < 0 ? 0 : v > 255 ? 255 : (int)v; }
    static void P(int x, int y, int r, int g, int b) { if (x >= 0 && x < 128 && y >= 0 && y < 64) img.SetPixel(x, y, Color.FromArgb(255, Cl(r), Cl(g), Cl(b))); }
    static void Clear(int x, int y) { img.SetPixel(x, y, Color.FromArgb(0, 0, 0, 0)); }
    static void G(int x, int y, int r, int g, int b) { glow.SetPixel(x, y, Color.FromArgb(255, r, g, b)); }
    static void Rect(int x, int y, int w, int h, int[] c, int noise)
    {
        for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) { int n = rng.Next(-noise, noise + 1); P(x + i, y + j, c[0] + n, c[1] + n, c[2] + n); }
    }
    static int[] Add(int[] c, int k) { return new[] { c[0] + k, c[1] + k, c[2] + k }; }
    // a box's six faces: top lighter, bottom darker
    static void Box(int u, int v, int w, int h, int d, int[] c, int noise)
    {
        Rect(u + d, v, w, d, Add(c, 14), noise);
        Rect(u + d + w, v, w, d, Add(c, -12), noise);
        Rect(u, v + d, d, h, Add(c, -6), noise);
        Rect(u + d, v + d, w, h, c, noise);
        Rect(u + d + w, v + d, d, h, Add(c, -8), noise);
        Rect(u + 2 * d + w, v + d, w, h, Add(c, -4), noise);
    }

    public static void Make(string dir)
    {
        img = new Bitmap(128, 64, PixelFormat.Format32bppArgb);
        glow = new Bitmap(128, 64, PixelFormat.Format32bppArgb);
        int[] black = { 22, 18, 24 }, hair = { 14, 10, 16 }, skin = { 234, 232, 238 }, skinShade = { 196, 190, 206 };
        int[] crimson = { 140, 14, 30 }, burgundy = { 70, 12, 26 }, gold = { 214, 168, 64 };

        // head 8x8x8 at 0,0: black hair all round, the face in front (x 8-15, y 8-15)
        Box(0, 0, 8, 8, 8, hair, 3);
        Rect(8, 8, 8, 8, skin, 4);
        // hair: a widow's peak over the forehead
        Rect(8, 8, 8, 1, hair, 2); P(11, 9, hair[0], hair[1], hair[2]); P(12, 9, hair[0], hair[1], hair[2]); P(8, 9, hair[0], hair[1], hair[2]); P(15, 9, hair[0], hair[1], hair[2]);
        // brows, low and angry
        P(9, 10, 40, 30, 40); P(10, 10, 60, 46, 58); P(13, 10, 60, 46, 58); P(14, 10, 40, 30, 40);
        // eyes: dark sockets round burning red
        P(8, 11, 120, 110, 132); P(9, 11, 230, 30, 36); P(10, 11, 255, 90, 70); P(11, 11, 150, 140, 160);
        P(12, 11, 150, 140, 160); P(13, 11, 255, 90, 70); P(14, 11, 230, 30, 36); P(15, 11, 120, 110, 132);
        G(9, 11, 255, 40, 40); G(10, 11, 255, 120, 90); G(13, 11, 255, 120, 90); G(14, 11, 255, 40, 40);
        // hollow cheeks, the shadow of a sharp nose
        Rect(8, 12, 2, 2, skinShade, 3); Rect(14, 12, 2, 2, skinShade, 3); P(11, 12, 176, 174, 190); P(12, 12, 150, 146, 166);
        // a thin dark mouth, fangs below it, a pointed chin
        P(10, 13, 140, 60, 76); Rect(11, 13, 2, 1, new[] { 90, 20, 34 }, 2); P(13, 13, 140, 60, 76);
        P(10, 14, 246, 244, 240); P(13, 14, 246, 244, 240); P(11, 14, skin[0], skin[1], skin[2]); P(12, 14, skin[0], skin[1], skin[2]);
        Rect(9, 15, 6, 1, skinShade, 3);
        // the hood 9x10x9 at 32,0: black cloth with folds, its front rim a frame (crimson edge) open in the middle
        Box(32, 0, 9, 10, 9, black, 4);
        for (int y = 9; y < 19; y++) { P(36 + (y % 3), y, 14, 10, 16); P(62 + (y % 2), y, 14, 10, 16); }
        for (int y = 11; y < 19; y++) for (int x = 42; x < 49; x++) Clear(x, y);
        for (int x = 41; x < 50; x++) { P(x, 10, 80, 12, 24); }
        for (int y = 10; y < 19; y++) { P(41, y, 80, 12, 24); P(49, y, 80, 12, 24); }
        // the hood's peak 4x3x5 at 70,0
        Box(70, 0, 4, 3, 5, black, 3);
        // fangs 1x1x1 at 88,12
        Box(88, 12, 1, 1, 1, new[] { 246, 244, 238 }, 2);
        // the gem at his throat 2x2x1 at 88,8: blood red in gold
        Box(88, 8, 2, 2, 1, gold, 6);
        P(89, 9, 220, 20, 40); P(90, 9, 160, 10, 26); P(89, 10, 160, 10, 26); P(90, 10, 255, 70, 80);
        G(89, 9, 255, 40, 60); G(90, 10, 255, 110, 120);
        // the collar 6x7x1 at 90,0 and 106,0: crimson inside (its front faces the head), black outside, a gold edge on top
        foreach (int u in new[] { 90, 106 })
        {
            Box(u, 0, 6, 7, 1, black, 3);
            Rect(u + 1, 1, 6, 7, crimson, 6);
            for (int y = 2; y < 8; y++) P(u + 1 + (y % 3) * 2, y, 110, 10, 24);
            Rect(u + 1, 1, 6, 1, gold, 8);
            Rect(u + 8, 1, 6, 1, gold, 8);
        }
        // the doublet 8x13x4 at 0,20: burgundy; the front (4..11, 24..36): lace jabot, black lapels, gold buttons, belt
        Box(0, 20, 8, 13, 4, burgundy, 5);
        for (int y = 24; y < 37; y++) { P(4, y, 24, 14, 22); P(5, y, 30, 18, 28); P(10, y, 30, 18, 28); P(11, y, 24, 14, 22); }
        Rect(6, 24, 4, 4, new[] { 222, 216, 214 }, 8);
        P(6, 27, 180, 172, 176); P(9, 27, 180, 172, 176); P(7, 28, 200, 194, 196); P(8, 28, 200, 194, 196);
        foreach (int y in new[] { 30, 33 }) { P(7, y, gold[0], gold[1], gold[2]); P(8, y, 255, 220, 120); }
        Rect(4, 35, 8, 1, new[] { 18, 12, 14 }, 2); P(7, 35, gold[0], gold[1], gold[2]); P(8, 35, gold[0], gold[1], gold[2]);
        // the cape 12x24x1 at 26,20: black with folds and a ragged hem outside (back face: 40..51), crimson inside (27..38)
        Box(26, 20, 12, 24, 1, black, 4);
        for (int y = 21; y < 45; y++) for (int x = 40; x < 52; x++) if ((x - 40) % 3 == 0) P(x, y, 12, 8, 14);
        Rect(27, 21, 12, 24, crimson, 7);
        for (int y = 21; y < 45; y++) for (int x = 27; x < 39; x++) if ((x - 27) % 4 == 1) P(x, y, 100, 10, 22);
        for (int x = 26; x < 52; x++) { if (rng.Next(3) == 0) Clear(x, 44); if (rng.Next(6) == 0) Clear(x, 43); }
        // sleeves 3x13x3 at 56,20 and 70,20: black, pale hands at the ends (the last 3 rows), dark nails
        foreach (int u in new[] { 56, 70 })
        {
            Box(u, 20, 3, 13, 3, new[] { 30, 16, 26 }, 4);
            Rect(u, 33, 12, 3, skin, 5);
            Rect(u + 3, 23, 3, 1, new[] { 120, 16, 30 }, 3);
            for (int x = u; x < u + 12; x++) P(x, 35, 40, 20, 34);
            Rect(u + 6, 20, 3, 3, skin, 4);
            P(u + 6, 22, 40, 20, 34); P(u + 8, 22, 40, 20, 34);
        }
        // legs 3x11x3 at 0,46 and 14,46: black trousers, boots with a sheen
        foreach (int u in new[] { 0, 14 })
        {
            Box(u, 46, 3, 11, 3, new[] { 26, 22, 28 }, 3);
            Rect(u, 54, 12, 6, new[] { 14, 10, 12 }, 2);
            P(u + 4, 55, 70, 64, 72); P(u + 4, 56, 50, 46, 54);
        }
        // the coat's skirt 9x6x5 at 28,46: black, a burgundy hem
        Box(28, 46, 9, 6, 5, black, 4);
        Rect(28, 56, 28, 1, burgundy, 4);
        img.Save(dir + "\\blood_merchant.png", ImageFormat.Png);
        glow.Save(dir + "\\blood_merchant_glow.png", ImageFormat.Png);
    }
}
'@
[BloodMerchantTex]::Make((Resolve-Path (Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures\entity')).Path)
'saved'
