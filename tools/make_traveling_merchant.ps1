# Draws the Traveling Merchant and his wagon (textures/entity/traveling_merchant.png, 256x256) and what glows
# (traveling_merchant_glow.png: the lantern flames, the pipe's ember) for client/TravelingMerchantModel.
#  Him: a weathered tan face with deep wrinkles, bushy white brows, a big red nose, a white moustache and a long grey
#  beard; grey hair; a battered brown felt hat with a leather band and a red feather; a red striped scarf; a long
#  patched brown coat with brass buttons and a belt; an olive canvas pack with leather straps and a red-and-grey bedroll;
#  dark trousers and worn boots; a gnarled staff; an iron lantern; a clay pipe.
#  The wagon: a plank bed with iron straps, a dusty patched white canvas bonnet over hoops (the open ends show the dark
#  inside), round spoked wheels with iron rims (transparent outside the rim), axles, the tongue, an iron-hooped barrel,
#  plank crates with an X brace.
# The boxes below match TravelingMerchantModel's texOffs and sizes (u, v, w, h, d). Run: powershell -File tools\make_traveling_merchant.ps1
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Imaging;

public static class TravelerTex
{
    static Bitmap img, glow;
    static Random rng = new Random(57);
    const int S = 256;
    static int Cl(double v) { return v < 0 ? 0 : v > 255 ? 255 : (int)v; }
    static void P(int x, int y, int r, int g, int b) { if (x >= 0 && x < S && y >= 0 && y < S) img.SetPixel(x, y, Color.FromArgb(255, Cl(r), Cl(g), Cl(b))); }
    static void P(int x, int y, int[] c) { P(x, y, c[0], c[1], c[2]); }
    static void Clear(int x, int y) { if (x >= 0 && x < S && y >= 0 && y < S) img.SetPixel(x, y, Color.FromArgb(0, 0, 0, 0)); }
    static void G(int x, int y, int r, int g, int b) { glow.SetPixel(x, y, Color.FromArgb(255, r, g, b)); }
    static void Rect(int x, int y, int w, int h, int[] c, int noise)
    {
        for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) { int n = rng.Next(-noise, noise + 1); P(x + i, y + j, c[0] + n, c[1] + n, c[2] + n); }
    }
    static int[] Add(int[] c, int k) { return new[] { c[0] + k, c[1] + k, c[2] + k }; }
    static int[] C(int r, int g, int b) { return new[] { r, g, b }; }
    // a box's six faces: top lighter, bottom darker
    static void Box(int u, int v, int w, int h, int d, int[] c, int noise)
    {
        Rect(u + d, v, w, d, Add(c, 14), noise);
        Rect(u + d + w, v, w, d, Add(c, -14), noise);
        Rect(u, v + d, d, h, Add(c, -6), noise);
        Rect(u + d, v + d, w, h, c, noise);
        Rect(u + d + w, v + d, d, h, Add(c, -8), noise);
        Rect(u + 2 * d + w, v + d, w, h, Add(c, -4), noise);
    }
    // horizontal plank seams across a face
    static void Planks(int x, int y, int w, int h, int every, int[] c)
    {
        for (int j = every - 1; j < h; j += every) for (int i = 0; i < w; i++) P(x + i, y + j, Add(c, -34));
        for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) if (rng.Next(40) == 0) P(x + i, y + j, Add(c, -22));
    }

    public static void Make(string dir)
    {
        img = new Bitmap(S, S, PixelFormat.Format32bppArgb);
        glow = new Bitmap(S, S, PixelFormat.Format32bppArgb);
        int[] skin = C(196, 150, 112), hair = C(172, 172, 168), beard = C(222, 222, 216);
        int[] felt = C(110, 78, 48), coat = C(120, 84, 50), coatDark = C(92, 62, 36), red = C(156, 42, 32);
        int[] canvasPack = C(112, 106, 70), leather = C(74, 48, 28), trousers = C(66, 66, 78), boot = C(58, 40, 26);
        int[] wood = C(140, 100, 60), darkWood = C(96, 66, 38), iron = C(70, 70, 76), canvas = C(234, 228, 208);

        // ── head 8x8x8 at 0,0: grey hair round the sides and back, the face in front (8..15, 8..15)
        Box(0, 0, 8, 8, 8, hair, 6);
        Rect(8, 8, 8, 8, skin, 6);
        // wrinkled forehead, bushy white brows
        for (int i = 9; i < 15; i++) if (i % 2 == 0) P(i, 9, Add(skin, -24));
        P(9, 10, beard); P(10, 10, beard); P(11, 10, Add(beard, -20)); P(12, 10, Add(beard, -20)); P(13, 10, beard); P(14, 10, beard);
        // small tired eyes, crow's feet
        P(9, 11, 230, 226, 214); P(10, 11, 40, 34, 30); P(13, 11, 40, 34, 30); P(14, 11, 230, 226, 214);
        P(8, 11, Add(skin, -30)); P(15, 11, Add(skin, -30));
        // the big red nose
        P(11, 11, 200, 120, 100); P(12, 11, 200, 120, 100); P(11, 12, 214, 112, 96); P(12, 12, 190, 96, 84);
        // cheeks, then the white moustache and beard over the mouth and chin
        P(9, 12, 206, 132, 108); P(14, 12, 206, 132, 108);
        Rect(9, 13, 6, 1, beard, 6); P(8, 13, Add(beard, -16)); P(15, 13, Add(beard, -16));
        Rect(8, 14, 8, 2, Add(beard, -6), 8);
        // sideburns into the beard
        for (int y = 11; y < 16; y++) { P(8, y, Add(hair, -6)); P(15, y, Add(hair, -6)); }
        // the beard 6x7x1 at 88,16: front 89..94, 17..23, long and streaky
        Box(88, 16, 6, 7, 1, beard, 8);
        for (int y = 17; y < 24; y++) for (int x = 89; x < 95; x++) if ((x + y) % 3 == 0) P(x, y, Add(beard, -26));
        Clear(89, 23); Clear(94, 23); Clear(89, 22);

        // ── the hat: brim 14x1x14 at 0,16, crown 8x4x8 at 56,16
        Box(0, 16, 14, 1, 14, felt, 8);
        for (int k = 0; k < 30; k++) P(14 + rng.Next(14), 16 + rng.Next(14), Add(felt, -24));   // worn patches on top
        for (int k = 0; k < 20; k++) P(28 + rng.Next(14), 16 + rng.Next(14), Add(felt, -10));
        Box(56, 16, 8, 4, 8, Add(felt, -10), 8);
        // a leather band round the crown's bottom, and a red feather on its side
        for (int x = 56; x < 88; x++) P(x, 27, leather);
        P(57, 25, red); P(58, 24, red); P(59, 25, 200, 70, 50); P(58, 26, red);
        // a dent in the top
        P(67, 19, Add(felt, -34)); P(68, 20, Add(felt, -34)); P(67, 20, Add(felt, -26));

        // ── the scarf 9x2x5 at 102,16: red with dark stripes
        Box(102, 16, 9, 2, 5, red, 8);
        for (int x = 102; x < 130; x += 3) for (int y = 16; y < 23; y++) P(x, y, Add(red, -40));

        // ── the body 8x11x4 at 0,32 (front 4..11, 36..46): the coat, a shirt down the middle, brass buttons, a belt
        Box(0, 32, 8, 11, 4, coat, 8);
        Rect(7, 36, 2, 9, C(196, 180, 140), 6);
        P(6, 38, 210, 170, 70); P(6, 41, 210, 170, 70); P(9, 38, 210, 170, 70); P(9, 41, 210, 170, 70);
        Rect(4, 45, 8, 1, leather, 4); P(7, 45, 200, 170, 80); P(8, 45, 200, 170, 80);
        // patches on the coat: a square of other cloth, stitched
        Rect(16, 38, 3, 3, C(88, 96, 70), 4); P(15, 38, 40, 30, 20); P(19, 40, 40, 30, 20);
        Rect(5, 42, 2, 2, C(140, 110, 70), 4);
        // ── the coat's skirt 9x7x5 at 24,32: to the knees, frayed at the hem, a patch
        Box(24, 32, 9, 7, 5, coat, 8);
        for (int x = 24; x < 52; x++) { if (rng.Next(3) == 0) P(x, 43, Add(coat, -36)); }
        Rect(30, 39, 3, 2, C(100, 92, 60), 4);
        // ── arms 3x12x3 at 52,32 and 64,32: sleeves, cuffs, weathered hands
        foreach (int u in new[] { 52, 64 })
        {
            Box(u, 32, 3, 12, 3, coat, 8);
            for (int x = u; x < u + 12; x++) P(x, 43, Add(coatDark, -10));
            Rect(u, 44, 12, 3, skin, 8);
            Rect(u + 3, 32, 3, 3, Add(skin, 4), 6);   // the bottom of the arm box: the palm
        }
        // ── the pack 6x8x4 at 76,32: olive canvas, leather straps, a buckle
        Box(76, 32, 6, 8, 4, canvasPack, 8);
        for (int y = 36; y < 44; y++) { P(81, y, leather); P(84, y, leather); P(91, y, leather); P(94, y, leather); }
        Rect(80, 38, 6, 1, leather, 2); P(82, 38, 200, 170, 80); P(83, 38, 200, 170, 80);
        // ── the bedroll 8x3x3 at 96,32: red and grey stripes, the ends rolled up
        Box(96, 32, 8, 3, 3, C(150, 60, 50), 6);
        for (int x = 96; x < 118; x += 2) for (int y = 35; y < 38; y++) P(x, y, 140, 138, 130);
        P(97, 36, 110, 40, 34); P(96, 36, 90, 30, 26);
        // ── legs 3x11x3 at 0,48 and 16,48: dark trousers, worn boots
        foreach (int u in new[] { 0, 16 })
        {
            Box(u, 48, 3, 11, 3, trousers, 6);
            Rect(u, 59, 12, 3, boot, 6);
            Rect(u + 6, 48, 3, 3, Add(boot, -10), 4);  // the soles
            for (int x = u; x < u + 12; x++) if (rng.Next(3) == 0) P(x, 58, Add(trousers, -20));
        }
        // ── the staff 1x26x1 at 120,0: gnarled, knots, a darker crook at the top
        Box(120, 0, 1, 26, 1, darkWood, 10);
        for (int y = 1; y < 27; y += 5) for (int x = 120; x < 124; x++) P(x, y, Add(darkWood, -30));
        // ── the lantern 2x3x2 at 128,0: an iron frame round glowing glass
        Box(128, 0, 2, 3, 2, iron, 4);
        foreach (int x in new[] { 130, 134 }) { P(x, 3, 250, 196, 90); P(x, 4, 240, 160, 60); G(x, 3, 255, 210, 110); G(x, 4, 255, 170, 70); }
        foreach (int x in new[] { 128, 132 }) { P(x + 1, 3, 250, 196, 90); G(x + 1, 3, 255, 210, 110); }
        // ── the pipe: a stem 1x1x3 at 136,0 and the bowl 2x2x2 at 136,4, an ember on top
        Box(136, 0, 1, 1, 3, C(120, 90, 60), 4);
        Box(136, 4, 2, 2, 2, C(90, 60, 36), 4);
        P(138, 4, 255, 120, 40); P(139, 4, 230, 90, 30); G(138, 4, 255, 140, 50); G(139, 4, 255, 110, 40);

        // ── the wagon bed 24x8x48 at 0,64: planks with iron straps (sides 0..47 / 72..119, front 48..71, back 120..143, rows 112..119)
        Box(0, 64, 24, 8, 48, wood, 8);
        Planks(0, 112, 48, 8, 4, wood); Planks(72, 112, 48, 8, 4, wood); Planks(48, 112, 24, 8, 4, wood); Planks(120, 112, 24, 8, 4, wood);
        foreach (int x in new[] { 5, 23, 41, 77, 95, 113 }) for (int y = 112; y < 120; y++) P(x, y, iron);
        Planks(48, 64, 24, 48, 5, Add(wood, -8));
        // ── the canvas bonnet: lower 26x13x44 at 0,120 (sides 0..43 / 70..113, ends 44..69 / 114..139, rows 164..176),
        //    top 18x7x44 at 0,177 (sides 0..43 / 62..105, ends 44..61 / 106..123, rows 221..227)
        Box(0, 120, 26, 13, 44, canvas, 6);
        Box(0, 177, 18, 7, 44, canvas, 6);
        foreach (int x0 in new[] { 0, 70 })
        {
            for (int k = 5; k < 44; k += 9) for (int y = 164; y < 177; y++) P(x0 + k, y, Add(canvas, -30));   // the hoops
            for (int x = x0; x < x0 + 44; x++) { P(x, 176, Add(canvas, -52)); if (rng.Next(2) == 0) P(x, 175, Add(canvas, -38)); if (rng.Next(3) == 0) P(x, 174, Add(canvas, -22)); }
        }
        Rect(10, 167, 5, 4, C(200, 186, 150), 4); Rect(88, 168, 4, 3, C(190, 176, 140), 4); Rect(30, 166, 3, 3, C(206, 194, 160), 3);
        foreach (int x0 in new[] { 0, 62 }) for (int k = 5; k < 44; k += 9) for (int y = 221; y < 228; y++) P(x0 + k, y, Add(canvas, -30));
        for (int k = 5; k < 44; k += 9) for (int x = 44; x < 62; x++) P(x, 177 + k, Add(canvas, -26));     // hoops over the top
        // the open ends: dark inside, a canvas rim, a drawstring, sacks and crates inside
        foreach (int x0 in new[] { 44, 114 })
        {
            Rect(x0 + 3, 166, 20, 11, C(40, 30, 22), 3);
            for (int y = 166; y < 177; y++) { P(x0 + 2, y, Add(canvas, -40)); P(x0 + 23, y, Add(canvas, -40)); }
            for (int x = x0 + 2; x < x0 + 24; x += 2) P(x, 165, Add(canvas, -46));
            Rect(x0 + 6, 171, 4, 4, C(160, 120, 70), 4); Rect(x0 + 13, 172, 5, 4, C(130, 96, 56), 4); Rect(x0 + 19, 170, 2, 5, C(110, 110, 120), 3);
        }
        foreach (int x0 in new[] { 44, 106 }) Rect(x0 + 2, 222, 14, 6, C(40, 30, 22), 3);
        // ── a wheel 1x16x16 at 144,64: its round faces at 144..159 and 161..176, rows 80..95
        Box(144, 64, 1, 16, 16, iron, 4);
        foreach (int x0 in new[] { 144, 161 })
        {
            for (int j = 0; j < 16; j++) for (int i = 0; i < 16; i++)
            {
                double dx = i - 7.5, dy = j - 7.5, r = Math.Sqrt(dx * dx + dy * dy);
                int x = x0 + i, y = 80 + j;
                if (r > 8.0) Clear(x, y);
                else if (r > 6.9) P(x, y, iron);
                else if (r > 5.6) P(x, y, wood[0] - 20 + rng.Next(8), wood[1] - 20, wood[2] - 16);
                else if (r < 1.8) P(x, y, darkWood);
                else if (Math.Abs(dx) < 0.7 || Math.Abs(dy) < 0.7 || Math.Abs(Math.Abs(dx) - Math.Abs(dy)) < 0.9) P(x, y, wood);
                else Clear(x, y);
            }
        }
        // ── axle 26x1x1 at 144,100, tongue 2x2x20 at 180,64
        Box(144, 100, 26, 1, 1, Add(darkWood, -10), 6);
        Box(180, 64, 2, 2, 20, darkWood, 8);
        // ── barrel 6x8x6 at 144,104 (sides rows 110..117): oak staves, two iron hoops, a lid
        Box(144, 104, 6, 8, 6, C(132, 92, 52), 8);
        for (int x = 144; x < 168; x++) { P(x, 111, iron); P(x, 116, iron); }
        for (int x = 144; x < 168; x += 2) for (int y = 110; y < 118; y++) if (y != 111 && y != 116) P(x, y, 112, 76, 42);
        // ── crate 7x7x7 at 170,104: light planks, a dark frame, an X brace on each side
        Box(170, 104, 7, 7, 7, C(176, 140, 90), 8);
        int[][] faces = { new[] { 170, 111 }, new[] { 177, 111 }, new[] { 184, 111 }, new[] { 191, 111 }, new[] { 177, 104 }, new[] { 184, 104 } };
        foreach (int[] f in faces)
        {
            for (int i = 0; i < 7; i++) { P(f[0] + i, f[1], 110, 80, 48); P(f[0] + i, f[1] + 6, 110, 80, 48); P(f[0], f[1] + i, 110, 80, 48); P(f[0] + 6, f[1] + i, 110, 80, 48); }
            for (int i = 1; i < 6; i++) { P(f[0] + i, f[1] + i, 130, 96, 58); P(f[0] + 6 - i, f[1] + i, 130, 96, 58); }
        }

        img.Save(System.IO.Path.Combine(dir, "traveling_merchant.png"), ImageFormat.Png);
        glow.Save(System.IO.Path.Combine(dir, "traveling_merchant_glow.png"), ImageFormat.Png);
    }
}
'@
$dir = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures\entity'
[TravelerTex]::Make((Resolve-Path $dir).Path)
'saved'
