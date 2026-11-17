# Draws the Fisherman's skin (textures/entity/fisherman.png, 128x64) and what glows on him (fisherman_glow.png: the
# ember in his pipe) for client/FishermanModel: a weathered, tanned face with bushy grey brows, squinting eyes and a
# ruddy nose, grey hair under the hat; a big bushy grey beard with a white moustache; a bright yellow sou'wester with
# stitched seams; a bright yellow oilskin coat hanging open (its front middle is cut away) with black buckles down its
# edges and a yellow skirt to the thighs, open too; under it dark green rubber waders up to the chest (a bib with brass
# buttons and braces) over a cream knitted jumper; yellow sleeves with buckled cuffs and tanned hands; the waders' legs
# and dark boots; a briar pipe with a glowing ember.
# The boxes below match FishermanModel's texOffs and sizes (u, v, w, h, d).
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Imaging;

public static class FishermanTex
{
    static Bitmap img, glow;
    static Random rng = new Random(47);
    static int Cl(double v) { return v < 0 ? 0 : v > 255 ? 255 : (int)v; }
    static void P(int x, int y, int r, int g, int b) { if (x >= 0 && x < 128 && y >= 0 && y < 64) img.SetPixel(x, y, Color.FromArgb(255, Cl(r), Cl(g), Cl(b))); }
    static void P(int x, int y, int[] c) { P(x, y, c[0], c[1], c[2]); }
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
    // a black clasp buckle: a dark strap and a silver glint
    static void Buckle(int x, int y)
    {
        P(x, y, 30, 30, 34); P(x + 1, y, 44, 44, 50); P(x, y + 1, 150, 150, 158);
    }

    public static void Make(string dir)
    {
        img = new Bitmap(128, 64, PixelFormat.Format32bppArgb);
        glow = new Bitmap(128, 64, PixelFormat.Format32bppArgb);
        int[] yellow = { 240, 198, 34 }, yellowDark = { 196, 150, 18 }, seam = { 214, 170, 24 };
        int[] skin = { 198, 140, 100 }, skinShade = { 168, 112, 78 }, grey = { 186, 184, 178 }, greyLight = { 222, 220, 214 }, greyDark = { 140, 138, 134 };
        int[] green = { 42, 76, 46 }, greenDark = { 28, 52, 32 }, boot = { 22, 30, 24 };
        int[] jumper = { 214, 204, 180 }, brass = { 210, 168, 70 }, briar = { 96, 54, 26 }, stem = { 50, 30, 18 };

        // head 8x8x8 at 0,0: tanned face in front (8..15, 8..15), grey hair round the sides and back
        Box(0, 0, 8, 8, 8, skin, 4);
        // grey hair: the back (24..31) and the lower half of the sides, under the hat's brim
        Rect(24, 8, 8, 8, grey, 8);
        Rect(0, 11, 8, 5, grey, 8); Rect(16, 11, 8, 5, grey, 8);
        Rect(8, 8, 8, 1, grey, 6);
        // ears: tan, a little ruddy
        P(3, 12, 186, 120, 90); P(4, 12, 176, 110, 82); P(19, 12, 176, 110, 82); P(20, 12, 186, 120, 90);
        // weathered forehead lines
        P(9, 9, skinShade); P(10, 9, skinShade); P(13, 9, skinShade); P(14, 9, skinShade);
        // bushy grey brows
        P(9, 10, greyLight); P(10, 10, grey); P(13, 10, grey); P(14, 10, greyLight);
        // squinting eyes, crow's feet
        P(8, 11, skinShade); P(9, 11, skin); P(10, 11, 34, 44, 64); P(11, 11, skinShade);
        P(12, 11, skinShade); P(13, 11, 34, 44, 64); P(14, 11, skin); P(15, 11, skinShade);
        // a ruddy nose and cheeks
        P(11, 12, 214, 124, 98); P(12, 12, 204, 114, 90); P(9, 12, 206, 128, 100); P(14, 12, 206, 128, 100);
        // the beard over the rest (the beard box covers it in front)
        Rect(8, 13, 8, 3, grey, 8);
        // beard 8x5x2 at 108,0: big, bushy, grey with a white moustache, ragged at the bottom
        Box(108, 0, 8, 5, 2, grey, 10);
        Rect(110, 2, 8, 1, greyLight, 6);
        P(110, 3, greyLight); P(117, 3, greyLight);
        P(113, 3, 70, 40, 36); P(114, 3, 90, 50, 44);
        for (int y = 3; y < 7; y++) for (int x = 110; x < 118; x++) if (rng.Next(4) == 0) P(x, y, greyDark);
        Clear(110, 6); Clear(117, 6); if (rng.Next(2) == 0) Clear(112, 6); Clear(115, 6);
        // the hat's crown 9x4x9 at 32,0: yellow with stitched seams
        Box(32, 0, 9, 4, 9, yellow, 6);
        for (int x = 41; x < 50; x++) P(x, 4, seam);
        for (int y = 0; y < 9; y++) P(45, y, seam);
        for (int x = 32; x < 68; x++) P(x, 12, yellowDark);
        for (int x = 32; x < 68; x += 3) P(x, 11, seam);
        // the brims: front 10x1x3 at 70,0, back 11x1x8 at 70,6 (yellow, a little darker underneath, stitched round the edge)
        Box(70, 0, 10, 1, 3, yellow, 5);
        for (int x = 73; x < 83; x++) P(x, 0, seam);
        Box(70, 6, 11, 1, 8, yellow, 5);
        for (int x = 78; x < 89; x++) P(x, 13, seam);
        for (int y = 6; y < 14; y++) { P(78, y, seam); P(88, y, seam); }
        // pipe: the stem 1x1x3 at 108,10, the bowl 2x3x2 at 118,10 (briar with a dark band, a charred top, the ember)
        Box(108, 10, 1, 1, 3, stem, 3);
        Box(118, 10, 2, 3, 2, briar, 8);
        Rect(118, 13, 8, 1, new[] { 70, 38, 18 }, 3);
        P(120, 10, 40, 28, 22); P(121, 10, 255, 120, 30); P(120, 11, 255, 90, 20); P(121, 11, 60, 30, 20);
        G(121, 10, 255, 150, 50); G(120, 11, 255, 110, 30);
        // body 8x12x4 at 0,16: the waders' bib (green) over a cream jumper; the front (4..11, 20..31)
        Box(0, 16, 8, 12, 4, jumper, 6);
        for (int y = 20; y < 32; y++) for (int x = 0; x < 24; x++) if ((x + y) % 2 == 0 && rng.Next(3) == 0) P(x, y, 196, 186, 160);
        Rect(4, 23, 8, 9, green, 5);
        Rect(4, 27, 8, 5, green, 5);
        for (int y = 20; y < 23; y++) { P(6, y, greenDark); P(9, y, greenDark); }
        P(6, 23, brass); P(9, 23, brass);
        Rect(6, 26, 4, 3, greenDark, 3);
        // the braces over the shoulders and down the back
        for (int y = 20; y < 32; y++) { P(17, y, greenDark); P(22, y, greenDark); }
        Rect(16, 28, 8, 4, green, 5);
        // the coat 9x12x5 at 24,16: yellow oilskin, its front (29..37, 21..32) open in the middle, black buckles down the edges
        Box(24, 16, 9, 12, 5, yellow, 6);
        for (int y = 21; y < 33; y++) for (int x = 31; x < 36; x++) Clear(x, y);
        for (int y = 21; y < 33; y++) { P(30, y, yellowDark); P(36, y, yellowDark); }
        foreach (int y in new[] { 23, 27, 31 }) { Buckle(29, y); Buckle(36, y); }
        // a collar round the neck (the top face's edge), a seam down the back and the sides
        for (int x = 29; x < 38; x++) P(x, 20, yellowDark);
        for (int y = 21; y < 33; y++) { P(47, y, seam); P(26, y, seam); P(40, y, seam); }
        // the coat's skirt 10x6x6 at 52,16: yellow, open in front (58..67, 22..27), a dark hem
        Box(52, 16, 10, 6, 6, yellow, 6);
        for (int y = 22; y < 28; y++) for (int x = 61; x < 65; x++) Clear(x, y);
        for (int x = 52; x < 84; x++) P(x, 27, yellowDark);
        for (int y = 22; y < 27; y++) { P(60, y, yellowDark); P(65, y, yellowDark); }
        Buckle(59, 23); Buckle(65, 23);
        for (int x = 58; x < 78; x++) for (int y = 16; y < 22; y++) Clear(x, y);
        // sleeves 4x12x4 at 84,16 and 100,16: yellow, a buckled cuff, tanned hands at the ends (and underneath)
        foreach (int u in new[] { 84, 100 })
        {
            Box(u, 16, 4, 12, 4, yellow, 6);
            for (int y = 20; y < 28; y += 3) P(u + 1, y, seam);
            Rect(u, 28, 16, 1, yellowDark, 4);
            Buckle(u + 5, 27);
            Rect(u, 29, 16, 3, skin, 5);
            Rect(u + 8, 16, 4, 4, skin, 5);
            for (int x = u; x < u + 16; x++) P(x, 31, skinShade);
        }
        // legs 4x12x4 at 0,36 and 16,36: the waders' dark green rubber, a sheen, a knee patch, dark boots
        foreach (int u in new[] { 0, 16 })
        {
            Box(u, 36, 4, 12, 4, green, 4);
            for (int y = 40; y < 48; y++) P(u + 5, y, 66, 104, 70);
            Rect(u + 4, 44, 4, 2, greenDark, 3);
            Rect(u, 48, 16, 4, boot, 3);
            Rect(u + 8, 36, 4, 4, boot, 2);
            for (int x = u; x < u + 16; x++) P(x, 51, 46, 50, 46);
        }
        img.Save(dir + "\\fisherman.png", ImageFormat.Png);
        glow.Save(dir + "\\fisherman_glow.png", ImageFormat.Png);
    }
}
'@
[FishermanTex]::Make((Resolve-Path (Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures\entity')).Path)
'saved'
