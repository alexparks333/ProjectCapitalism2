# Draws the Bounty Clerk's skin (textures/entity/bounty_clerk.png, 128x64) for client/BountyClerkModel: a lean, clean-
# shaven face with a long nose and a thin, prim mouth, grey-blue eyes behind round gold-rimmed spectacles, neat side-parted
# chestnut hair; a white quill (a white vane, a dark tip); a bottle-green frock coat with lapels, gold-brass buttons, a
# breast pocket with a gold watch chain and tails to his knees; under it a buff waistcoat with small buttons, a white
# shirt and a white cravat; green sleeves with white cuffs and pale hands; grey striped trousers and polished black
# shoes; a thick red-leather ledger with gold corners and a ribbon; the notice board: cork in a dark oak frame, a little
# shingled roof, oak posts, and the bounty papers pinned to it (cream paper, scribbled lines, a red wax seal, a skull on
# one, a fish on another, a pickaxe on a third).
# The boxes below match BountyClerkModel's texOffs and sizes (u, v, w, h, d).
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Imaging;

public static class ClerkTex
{
    static Bitmap img;
    static Random rng = new Random(61);
    static int Cl(double v) { return v < 0 ? 0 : v > 255 ? 255 : (int)v; }
    static void P(int x, int y, int r, int g, int b) { if (x >= 0 && x < 128 && y >= 0 && y < 64) img.SetPixel(x, y, Color.FromArgb(255, Cl(r), Cl(g), Cl(b))); }
    static void P(int x, int y, int[] c) { P(x, y, c[0], c[1], c[2]); }
    static void Clear(int x, int y) { if (x >= 0 && x < 128 && y >= 0 && y < 64) img.SetPixel(x, y, Color.FromArgb(0, 0, 0, 0)); }
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
        int[] skin = { 226, 186, 152 }, skinShade = { 196, 152, 118 }, hair = { 110, 66, 36 }, hairDark = { 78, 44, 22 };
        int[] green = { 36, 82, 58 }, greenDark = { 22, 56, 38 }, greenLight = { 56, 110, 80 }, brass = { 222, 180, 80 };
        int[] buff = { 206, 176, 116 }, white = { 240, 238, 230 }, grey = { 104, 106, 112 }, shoe = { 26, 24, 28 };
        int[] red = { 150, 34, 34 }, redDark = { 104, 20, 22 }, gold = { 232, 190, 70 }, oak = { 92, 62, 36 }, oakDark = { 62, 40, 22 };
        int[] cork = { 176, 128, 78 }, paper = { 236, 224, 190 }, ink = { 60, 50, 60 };

        // ── head 8x8x8 at 0,0 ──
        Box(0, 0, 8, 8, 8, skin, 4);
        // the face (8..15, 8..15): brows, grey-blue eyes, a long nose, a thin prim mouth, a hint of a chin line
        for (int x = 9; x <= 10; x++) P(x, 10, hairDark); for (int x = 13; x <= 14; x++) P(x, 10, hairDark);
        P(9, 11, 250, 250, 250); P(10, 11, 70, 96, 130); P(13, 11, 70, 96, 130); P(14, 11, 250, 250, 250);
        P(11, 12, skinShade); P(12, 12, skinShade); P(11, 13, skinShade); P(12, 13, 206, 160, 128);
        for (int x = 10; x <= 13; x++) P(x, 14, 168, 104, 96);
        P(10, 15, skinShade); P(13, 15, skinShade);
        // ears
        P(1, 11, skinShade); P(1, 12, skinShade); P(22, 11, skinShade); P(22, 12, skinShade);
        // ── hair 8x8x8 (proud) at 32,0: on top, the back, the sides above the ears; a side parting; clear over the face ──
        Box(32, 0, 8, 8, 8, hair, 6);
        for (int x = 40; x < 48; x++) for (int y = 8; y < 16; y++) Clear(x, y);           // its front: the face shows
        for (int y = 8; y < 16; y++) for (int x = 32; x < 40; x++) if (y > 10) Clear(x, y); // its sides: only above the ears
        for (int y = 8; y < 16; y++) for (int x = 48; x < 56; x++) if (y > 10) Clear(x, y);
        for (int x = 40; x < 48; x++) P(x, 8, hairDark);                                   // a fringe line over the brow
        for (int y = 0; y < 8; y++) P(43, y, hairDark);                                     // the parting on top
        // ── spectacles: two lenses (3x2 flat, at 64,0 and 64,3) gold rims, clear glass; the bridge ──
        foreach (int v in new[] { 0, 3 })
        {
            for (int u = 64; u < 70; u++) for (int y = v; y < v + 2; y++) P(u, y, gold);
            Clear(65, v + 1); Clear(68, v + 1);
        }
        Rect(72, 0, 4, 2, gold, 6);
        // ── the quill: a dark stem (76,0), a white vane with a grey edge (80,0 side faces at 80..86 x 3..8) ──
        Rect(76, 0, 4, 8, new[] { 60, 50, 40 }, 4);
        Rect(80, 0, 8, 8, white, 6);
        for (int y = 3; y < 8; y++) { P(80, y, 190, 190, 196); P(85, y, 190, 190, 196); }
        P(82, 7, 40, 36, 40); P(83, 7, 40, 36, 40);
        // ── body 8x12x4 at 0,16: buff waistcoat with small buttons over a white shirt, a white cravat ──
        Box(0, 16, 8, 12, 4, buff, 5);
        Rect(4, 20, 8, 3, white, 3);                          // shirt and cravat at the neck
        P(7, 23, white); P(8, 23, white); P(7, 24, 226, 224, 216); P(8, 24, 226, 224, 216);
        for (int y = 25; y < 32; y += 2) P(8, y, 150, 120, 60); // waistcoat buttons
        // ── coat 9x12x5 at 24,16: bottle green, open over the waistcoat in front (a cut-away middle), lapels, brass
        //    buttons, a breast pocket and its gold watch chain ──
        Box(24, 16, 9, 12, 5, green, 6);
        for (int y = 21; y < 33; y++) for (int x = 31; x < 36; x++) Clear(x, y);   // open in front
        for (int y = 21; y < 27; y++) { P(29, y, greenLight); P(30, y, greenLight); P(36, y, greenLight); P(37, y, greenLight); }  // lapels
        for (int y = 22; y < 33; y += 3) { P(30, y, brass); P(36, y, brass); }
        Rect(26, 27, 3, 1, greenDark, 2);                                           // pocket
        P(29, 28, gold); P(30, 29, gold);                                           // watch chain
        for (int y = 21; y < 33; y++) P(24, y, greenDark);
        // ── coat tails 9x7x2 at 52,16 ──
        Box(52, 16, 9, 7, 2, green, 6);
        for (int y = 18; y < 25; y++) P(58, y, greenDark);                          // the vent between the tails
        P(56, 18, brass); P(60, 18, brass);
        // ── sleeves 4x12x4 at 84,16 and 100,16: green, white cuffs, pale hands ──
        foreach (int u in new[] { 84, 100 })
        {
            Box(u, 16, 4, 12, 4, green, 6);
            Rect(u, 28, 16, 1, white, 3);
            Rect(u, 29, 16, 3, skin, 4);
            Rect(u + 8, 16, 4, 4, skin, 4);
            P(u + 5, 27, brass);
        }
        // ── legs 4x12x4 at 0,36 and 16,36: grey striped trousers, black shoes with a shine ──
        foreach (int u in new[] { 0, 16 })
        {
            Box(u, 36, 4, 12, 4, grey, 4);
            for (int x = u; x < u + 16; x += 3) for (int y = 40; y < 48; y++) P(x, y, 88, 90, 96);
            Rect(u, 48, 16, 4, shoe, 3);
            Rect(u + 8, 36, 4, 4, shoe, 2);
            P(u + 5, 49, 90, 90, 100);
        }
        // ── the ledger 6x8x2 at 64,36: red leather, gold corners, cream page edges, a ribbon ──
        Box(64, 36, 6, 8, 2, red, 5);
        Rect(64, 36, 2, 2, paper, 3); Rect(72, 36, 6, 2, paper, 3);                 // page edges (top faces)
        foreach (int[] c in new[] { new[] { 66, 38 }, new[] { 71, 38 }, new[] { 66, 45 }, new[] { 71, 45 } }) P(c[0], c[1], gold);
        for (int y = 38; y < 46; y++) P(68, y, redDark);                             // the spine line
        P(69, 46, 200, 40, 40); P(69, 45, 200, 40, 40);                              // a ribbon hanging out
        // ── the notice board 14x11x1 at 32,36: cork in an oak frame ──
        Box(32, 36, 14, 11, 1, cork, 12);
        for (int x = 33; x < 47; x++) { P(x, 37, oakDark); P(x, 47, oakDark); }
        for (int y = 37; y < 48; y++) { P(33, y, oakDark); P(46, y, oakDark); }
        for (int i = 0; i < 30; i++) P(34 + rng.Next(12), 38 + rng.Next(9), 140, 96, 56); // cork pits
        Rect(48, 37, 14, 11, oak, 6);                                                 // its back: plain oak boards
        // ── the roof 16x1x3 at 32,52: dark shingles ──
        Box(32, 52, 16, 1, 3, oakDark, 8);
        for (int x = 35; x < 51; x += 2) P(x, 52, 46, 28, 16);
        // ── posts 1x16x1 at 104,36 and 108,36 ──
        Box(104, 36, 1, 16, 1, oak, 6); Box(108, 36, 1, 16, 1, oak, 6);
        // ── the papers (flat, front faces): cream, scribbled lines, a red seal; little pictures ──
        int[][] papers = { new[] { 64, 48, 4, 5 }, new[] { 72, 48, 4, 4 }, new[] { 80, 48, 3, 5 }, new[] { 88, 48, 5, 4 }, new[] { 64, 54, 4, 4 } };
        foreach (int[] p in papers)
        {
            Rect(p[0], p[1], p[2] * 2, p[3], paper, 6);
            for (int y = p[1] + 1; y < p[1] + p[3] - 1; y += 2) for (int x = p[0]; x < p[0] + p[2] - 1; x++) if (rng.Next(3) > 0) P(x, y, ink);
            P(p[0] + p[2] - 1, p[1] + p[3] - 1, 190, 30, 30);                         // a wax seal
            P(p[0] + p[2] / 2, p[1], 150, 150, 160);                                   // the pin
        }
        // a skull (kill), a fish (catch), a pickaxe (mine) drawn on three of them
        P(65, 49, 40, 40, 40); P(66, 49, 40, 40, 40); P(65, 50, 30, 30, 30); P(66, 50, 30, 30, 30);
        P(73, 49, 60, 110, 160); P(74, 49, 60, 110, 160); P(75, 50, 60, 110, 160);
        P(89, 49, 120, 120, 128); P(90, 49, 120, 120, 128); P(91, 49, 120, 120, 128); P(90, 50, 110, 70, 40); P(90, 51, 110, 70, 40);
        img.Save(dir + "\\bounty_clerk.png", ImageFormat.Png);
    }
}
'@
[ClerkTex]::Make((Resolve-Path (Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures\entity')).Path)
'saved'
