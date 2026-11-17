// Paints the fishing items (Fishing.md):
//  - the item icons (textures/item/<id>.png): 16 x 16 pixel art in the vanilla fish style (cod, salmon, pufferfish):
//    each one a hand-drawn pixel map (a grid of letters, one letter a pixel) with its own small palette; fish lie
//    diagonally, head bottom-left and tail top-right like vanilla, lit from the top left, a darker outline of their own
//    colour;
//  - the mounted trophies' textures (textures/entity/plaque/<id>.png, 64 x 64, one texel a model pixel): the zones of
//    client/FishTrophies (back, flank, belly, fins, eye, teeth, mouth, accents, glow), each a 3 to 5 tone ramp dithered
//    pixel by pixel, with the catch's own pattern (koi patches, catfish mottling, oarfish dashes...);
//  - the Pirate Hat's and the plaque board's textures (textures/entity/, the BoatKit layout).
// Part of BoatTex (compiled with tools/BoatTex.cs and tools/BloodDecorTex.cs; run MakeFish).
using System;
using System.Collections.Generic;
using System.Drawing;

public static partial class BoatTex
{
    // ── pixel maps ──

    static Color Hex(string h)
    {
        if (h.Length == 6) return Color.FromArgb(255, Convert.ToInt32(h.Substring(0, 2), 16), Convert.ToInt32(h.Substring(2, 2), 16), Convert.ToInt32(h.Substring(4, 2), 16));
        return Color.FromArgb(Convert.ToInt32(h.Substring(0, 2), 16), Convert.ToInt32(h.Substring(2, 2), 16), Convert.ToInt32(h.Substring(4, 2), 16), Convert.ToInt32(h.Substring(6, 2), 16));
    }

    // "a=RRGGBB b=AARRGGBB ..." -> palette ('.' is see-through)
    static Dictionary<char, Color> Palette(string spec)
    {
        var p = new Dictionary<char, Color>();
        foreach (string part in spec.Split(new[] { ' ' }, StringSplitOptions.RemoveEmptyEntries)) p[part[0]] = Hex(part.Substring(2));
        return p;
    }

    static void PixelIcon(string itemDir, string name, string palette, string[] rows)
    {
        var pal = Palette(palette);
        int h = rows.Length, w = rows[0].Length;
        var px = new Color[w, h];
        for (int y = 0; y < h; y++)
        {
            if (rows[y].Length != w) throw new Exception(name + ": row " + y + " is " + rows[y].Length + " wide, not " + w);
            for (int x = 0; x < w; x++)
            {
                char c = rows[y][x];
                if (c == '.') { px[x, y] = Clear; continue; }
                if (!pal.ContainsKey(c)) throw new Exception(name + ": no colour for '" + c + "' (row " + y + ")");
                px[x, y] = pal[c];
            }
        }
        Save(px, w, h, System.IO.Path.Combine(itemDir, name + ".png"), (x, y) => true);
    }

    // the koi's shape (the Golden Koi and the Ghost Koi share it)
    static readonly string[] KOI = {
        "............gg..",
        "...........gffg.",
        "..........gfgffg",
        "..........gffgg.",
        ".....gg.oogfg...",
        "....gffgopmOgg..",
        "....gfoppmdO....",
        "....olpmmdbO....",
        "...ollmmdbbO....",
        "...olmppdbO.....",
        "..ohlpqfbsO.....",
        "..oelmfgsO......",
        "..oppdbsO.......",
        ".goqbbOO........",
        "g..OOO..........",
        "................" };

    static void MakeFishIcons(string itemDir)
    {
        // Golden Koi: orange and white, a flowing tail, little barbels
        PixelIcon(itemDir, "golden_koi",
            "o=9A3E10 O=6C240A l=FFB35A m=F07A1E d=C8521A b=FFE6BE s=E9B07C f=FFCF8A g=D98A32 e=140C0C h=FFFFFF p=FBF7EE q=DCD2C2", KOI);
        // Ghost Koi: the koi in see-through blood red, pale ghostly patches
        PixelIcon(itemDir, "ghost_koi",
            "o=E0901830 O=E06A0A1E l=B4FF9AA6 m=B4F04A5E d=BCC42A40 b=B4FFD0D6 s=B4E8A0AA f=88FFB0B8 g=B0D25062 e=FF3A0410 h=FFFFFFFF p=AAFFE8EC q=AAF0C0C6", KOI);
        // Giant Catfish: olive-brown, a broad flat head, long whiskers
        PixelIcon(itemDir, "giant_catfish",
            "o=4A3E26 O=2A2214 l=948A5C m=727046 d=545034 b=DCD2A8 s=B4A880 f=625A3A g=3E3622 e=0E0E0A h=E8E0B0 w=3A3020", new[] {
            "................",
            "............gg..",
            "...........gffg.",
            "...........gfffg",
            "......g...ogffg.",
            ".....gfg.olmOgg.",
            ".....gfolmmdO...",
            "....oollmmdbO...",
            "...ollmmmdbbO...",
            "..ollmmmdbbsO...",
            ".ohllmmdfbsO....",
            ".oelmmdfgsO.....",
            "oommddbsO.......",
            "ommdbbsO........",
            "wOOOOOO.........",
            "w..w............" });
        // Pufferfish King: puffed up and furious, long spikes all round, red eyes under angry brows, a scowl, a scar, a
        // jagged gold crown with a ruby and a sapphire
        PixelIcon(itemDir, "pufferfish_king",
            "o=7A4410 O=4E2A06 l=FFE070 m=F2B428 d=C88418 b=FFEEC4 s=E2C288 k=FFFFF4 K=CFC4A0 c=FFD43A C=A86C08 r=E0283C g=4A7CFF B=3A1606 W=FFFFFF R=E01010 x=5A1408 S=F4D8C8", new[] {
            "....c..cc..c....",
            "....cc.cc.cc....",
            ".k..cCcrcCgc..k.",
            "..K.CCCCCCCC.K..",
            "k..KooooooooK..k",
            ".kKollllllmmdoKk",
            "kKolllllllmmddoK",
            ".kolBllllllBddok",
            "KKollBBllBBmddoK",
            ".kollWRllRWmddok",
            "kKolmmmmmmmmdsoK",
            ".koSmmxxxxmmdsok",
            "KKobSxbbbbxdssoK",
            ".kobbbbbbbbssoKk",
            "k.KoOssssssOOK.k",
            "...k.K.KK.K.k..." });
        // Blobfish: a sad pink blob, its droopy nose, a tear
        PixelIcon(itemDir, "blobfish",
            "o=A84E5C O=7C3442 l=FFDDE2 m=F6B6C0 d=E2929E b=FFEEF0 n=EE8C9C N=CC6878 e=241014 h=FFFFFF x=8A2E40 f=ECA2AE g=BE6E7C T=9AD8F0", new[] {
            "................",
            "................",
            "............gg..",
            "...........gffg.",
            "..........gfgg..",
            "........oooggg..",
            "......oolmmdO...",
            "....oolllmmddO..",
            "...ollllmmmmdO..",
            "..ollehlmmmmddO.",
            ".olllTlmmmmmdO..",
            ".olnnllmmmmddO..",
            "olnnNnlmmmddO...",
            "onNNNNxdddOO....",
            ".oNNoxxxOO......",
            "..oo.x.x........" });
        // Swordfish: steel blue over silver, a tall sail, a long bill to the corner, a crescent tail
        PixelIcon(itemDir, "swordfish",
            "o=223660 O=141E3A l=7898D0 m=4E70AE d=36548E b=E8EEF6 s=B4C2D4 f=3C548E g=243660 e=0A0A12 h=FFFFFF B=9AA4B6 K=5E6678", new[] {
            "...........g...g",
            "...........gf.gf",
            "............gfg.",
            "....g......ofg..",
            "....gg....olO...",
            "....gfg..olmO...",
            "....gffoolmdO...",
            ".....gfolmdbO...",
            "......olmdbsO...",
            ".....olmdbsO....",
            "....ohmdbsfg....",
            "...oelmbsOg.....",
            "..BKmbsO........",
            ".BK.OOO.........",
            "BK..............",
            "K..............." });
        // Ocean Sunfish: a tall grey disc, a fin above and below, its ruffled back edge
        PixelIcon(itemDir, "ocean_sunfish",
            "o=505A6A O=343C4A l=DDE2EA m=AEB8C6 d=828C9C b=F2F4F8 s=C8CED8 f=8E98A8 g=5E6878 e=0E1218 h=FFFFFF r=E4E8EE x=6A4848", new[] {
            ".........go.....",
            "........gfo.....",
            "........gfo.....",
            ".....ooogfoo....",
            "....olllmmmdo...",
            "...olllmmmmddgr.",
            "..ollmmmmmmddgr.",
            "..oehmmmmmmddsgr",
            ".xolmmmmmmddssgr",
            "..olmmmmmdddsgr.",
            "..obbmmmmddssgr.",
            "...obbbbddssgo..",
            "....oobssfgoo...",
            ".......gfo......",
            ".......gfo......",
            ".......go......." });
        // Electric Eel: long and dark, yellow bands, sparks crackling off it
        PixelIcon(itemDir, "electric_eel",
            "o=142024 O=0A1214 l=58787A m=3A5654 d=263A38 b=8E9E74 y=F4E244 e=101010 h=F8F27E z=B0F4FF Z=FFFFFF", new[] {
            "..............oz",
            "............oyO.",
            "...........olO..",
            "..........oydO..",
            "..........omO...",
            ".........oydO...",
            "........olmbO...",
            ".......oydbO....",
            "......olmdO.....",
            ".....oyybO......",
            "....olmdO.......",
            "..ooydbO........",
            ".oemmbO.z.......",
            ".ohydO..Zz......",
            "..ooO....z......",
            "................" });
        // Mako Shark: blue over white, a tall fin, gills, its jaws open with teeth
        PixelIcon(itemDir, "mako_shark",
            "o=1E3456 O=101C36 l=7EA0D0 m=5A7CB2 d=3C5C90 b=F4F6F8 s=C8D0DC f=44628E g=24385E e=000000 h=FFFFFF t=FFFFFF x=6E1A22 k=162438", new[] {
            "............g...",
            "...........gfg..",
            "...........gffg.",
            "..........ogfg..",
            "....g....olOg...",
            "....gg..olmdOg..",
            "....gfgolmmdOgg.",
            ".....ffolmdbO...",
            ".....olmmdbsO...",
            "....olmkdbsO....",
            "...olmkdbsfg....",
            "..ohmdbsOgg.....",
            ".oelmbsO........",
            "olmbxtO.........",
            "ootxtO..........",
            ".OO............." });
        // Anglerfish: a dark lumpy body, a jaw full of needle teeth, its lure glowing on a stalk
        PixelIcon(itemDir, "anglerfish",
            "o=241A1C O=100A0C l=6A5448 m=4E3C32 d=342820 b=846C5C x=1E0A0E t=F4F0DA s=7A6050 L=E4FFF4 G=50ECC4 e=E8DA8E h=FFFFFF", new[] {
            "....sss.........",
            "...s...s........",
            ".GLG....s.......",
            "..G.....s.......",
            "......ooooo.....",
            ".....ollllmoo...",
            "....olehmmmddo..",
            "...ollmmmmmddoo.",
            "..oxxxmmmmddsoo.",
            ".otxtxtmmmddso..",
            ".oxxxxxxmmdso...",
            ".otxtxtxdddso...",
            "..obbbbbbsso....",
            "...ooOOOOOO.....",
            "................",
            "................" });
        // Kraken Tentacle: a thick purple-red arm curling over at the top, pale suckers down its inside
        PixelIcon(itemDir, "kraken_tentacle",
            "o=46102C O=2C0818 l=CC5890 m=A23468 d=78204C s=F6CEDC S=C88AA4", new[] {
            "................",
            ".......oooo.....",
            ".....oolllmO....",
            "....ollmmSmdO...",
            "....olmSs.SmdO..",
            "...olms....sdO..",
            "...olmS....SdO..",
            "...olms....sO...",
            "..olmmd.SsSdO...",
            "..olmd...oO.....",
            ".olmms..........",
            ".olmdS..........",
            "olmms...........",
            "olmdS...........",
            "ommds...........",
            "OOdS............" });
        // Oarfish: a long silver ribbon, a red crest all down its back, red plumes at its head
        PixelIcon(itemDir, "oarfish",
            "o=6A7690 O=3E4A64 l=F0F4FA m=C6D0DE d=96A2B8 b=3A4A7A r=E02E42 R=A01428 e=101014 h=FFFFFF", new[] {
            "..............rR",
            "............rrlO",
            "...........rlmO.",
            "..........rlmdO.",
            ".........rlmdO..",
            "........rlmbO...",
            ".......rlmdO....",
            "......rlmbO.....",
            ".....rlmdO......",
            "....rlmbO.......",
            "....rlmdO.......",
            "...rlmbO........",
            "..RohmdO........",
            "RrRoedO.........",
            ".RrodO..........",
            "R..R............" });
        // Golden Leviathan Scale: a jagged shard of black-gold armour torn from something abyssal: a cruel point, hooked
        // barbs down both edges, a ragged torn base, and a molten slit down its middle like an eye opening
        PixelIcon(itemDir, "golden_leviathan_scale",
            "O=140C02 o=2E1E06 d=5A3A0A m=8C6412 l=B8901E L=E8C048 e=FF6A10 E=FFE070 r=8A1A06 x=2A0602", new[] {
            ".......OO.......",
            "......OLmO......",
            "......OLmO......",
            ".....OLlmdO.....",
            ".....OLrrdO.....",
            ".OO.OLlEEmdO.OO.",
            "..OOOLeExedOOO..",
            "...OLleExemdO...",
            "...OLleExemdO...",
            ".OOOLlmEEmmdOOO.",
            "..OOLlmrrmmdOO..",
            "...OLlmmmmdO....",
            "...OOdmmmddOO...",
            "....OOddddOO....",
            "....O.OOOO.O....",
            "................" });        // Message in a Bottle: green glass, a cork, a rolled note tied with red, a little sea inside
        PixelIcon(itemDir, "message_in_a_bottle",
            "a=D08A50 b=A0663A f=6E4224 c=E2F8E8 e=9ED2B0 g=4E8E6A h=C4EED6 p=F2E6C4 q=D2BC8C r=C8382A w=86CCDA W=5AA6BE", new[] {
            "................",
            "................",
            ".......aab......",
            "......aabbf.....",
            "......cebbe.....",
            ".......e.g......",
            ".......e.g......",
            "......c...g.....",
            ".....c.pqp.g....",
            "....c.pprqpg....",
            "....cepprqpg....",
            "....e.pprqhg....",
            "....gwwpqwhg....",
            ".....gWWWWg.....",
            "......ggggg.....",
            "................" });
        // Sunken Pirate Chest: wet planks, iron bands, a gold lock, barnacles, weed, gold heaped on the lid
        PixelIcon(itemDir, "sunken_pirate_chest",
            "o=24160A d=5A381C m=7A4E28 l=9A6A38 L=BC8A4E i=3C4046 I=7A8088 g=FFD84A G=C8941C y=FFF6B0 w=E6DECA W=9E968A s=4E9A4A S=2E6A30 k=140C06", new[] {
            "....y...........",
            "...gGg.g........",
            "..ogGgyGgGoo....",
            ".oLLLiLLLLLiLLo.",
            ".olllIllwllIllo.",
            ".ommmImmmmmImmo.",
            ".oiiiiiGGiiiiio.",
            ".ommmImGkmmImmo.",
            ".olllIlGGllIllo.",
            ".odddIddddwIddo.",
            ".ommmImmmmmIWmo.",
            ".olllIllllWIllo.",
            "SoddwIdddddIddoS",
            "sooooooooooooooS",
            "s..............s",
            "................" });
        // Soggy Note: yellowed wet paper (the vanilla paper's shape), smudged lines, a red X, a drip
        PixelIcon(itemDir, "soggy_note",
            "a=9E9478 b=D8CFB2 c=C4B894 d=EDE6CE e=6E6650 f=A89E80 i=5A5068 I=8A8098 x=B82820 w=C8BC98", new[] {
            "................",
            "................",
            ".........aa.....",
            "........abca....",
            "......aacdiba...",
            ".....abdiIddce..",
            "...aacdIiddwdae.",
            "..acbiIdwwdddcae",
            ".acbIiddxdxddbe.",
            "..ecbdiddxdbce..",
            "...ecbddxdxee...",
            "....ecbdbfe.....",
            ".....ecfee......",
            "......ee........",
            ".......w........",
            "................" });
        // Pirate Hat: a black tricorn, gold braid, a skull and crossbones, a red feather
        PixelIcon(itemDir, "pirate_hat",
            "o=0E0C10 k=1E1A22 K=2E2834 L=4A4252 g=EEC050 G=A87820 w=EEE6D2 W=B8AE98 e=0E0C10 r=D8303E R=8A1420", new[] {
            "..............r.",
            ".............rR.",
            "......oooo..rR..",
            "....ooLLKKoorR..",
            "...oLLKKKKKkoR..",
            "..oLKKKKKKKKko..",
            "gooLKKKKKKKKkoog",
            "GgoLKKwwwKKKkogG",
            "oGgoKwewewKKogGo",
            ".oGgoWwwwWKogGo.",
            "..oGgoWKWogGo...",
            "...oGgoKKogGo...",
            "....oGgKKgGo....",
            ".....oGggGo.....",
            "......oGGo......",
            "................" });
        // Fish Plaque: a walnut board, a fish mounted on it, a brass plate
        PixelIcon(itemDir, "fish_plaque",
            "o=2A160A d=5A3218 m=7C4824 l=9E6434 F=2E4A80 f=5276B4 w=E4ECF4 e=101010 p=ECC464 P=A87C30", new[] {
            "................",
            "................",
            "......pp........",
            ".oooooooooooooo.",
            ".ollllllllllldo.",
            ".olmmFFFFmmmFdo.",
            ".olFFffffFmFFdo.",
            ".oFeffffffFFmdo.",
            ".olFwwwwwwFFmdo.",
            ".olmFFFFFmmmFdo.",
            ".olmmmpppppmmdo.",
            ".olmmmPPPPPmmdo.",
            ".oddddddddddddo.",
            ".oooooooooooooo.",
            "................",
            "................" });
    }

    // ── the trophies' textures ──

    class TrophyStyle
    {
        public string Id;
        public Color[] Back, Side, Belly, Fin, Accent, Accent2, Glow, Band;
        public Color Eye = Color.FromArgb(255, 14, 12, 14), White = Color.FromArgb(255, 244, 242, 232), Mouth = Color.FromArgb(255, 40, 10, 14), Dark = Color.FromArgb(255, 20, 24, 34);
        public string Pattern = "";
        public int Alpha = 255, FinAlpha = 255;
    }

    static Color[] Ramp(params string[] hex)
    {
        var r = new Color[hex.Length];
        for (int i = 0; i < hex.Length; i++) r[i] = Hex(hex[i]);
        return r;
    }

    static Color WithAlpha(Color c, int a) { return Color.FromArgb(a, c.R, c.G, c.B); }

    // a zone dithered from a ramp (dark to light): mostly the middle tones, a lighter or darker pixel here and there,
    // scale rows; `pattern` can swap in another ramp or shift a tone
    static void RampZone(Color[,] px, int zx, int zy, int zw, int zh, Color[] ramp, int seed, int alpha, bool scales, Func<int, int, int> shift, Func<int, int, Color[]> swap)
    {
        for (int y = 0; y < zh; y++) for (int x = 0; x < zw; x++)
        {
            Color[] r = swap == null ? null : swap(x, y);
            if (r == null) r = ramp;
            int mid = r.Length / 2;
            double hsh = Hash(x, y, seed);
            int i = mid;
            if (hsh > 0.78) i = mid + 1;
            else if (hsh < 0.16) i = mid - 1;
            if (scales && ((x + ((y / 2) % 2) * 2) % 4 == 0) && (y % 2 == 1)) i -= 1;
            if (shift != null) i += shift(x, y);
            i = Math.Max(0, Math.Min(r.Length - 1, i));
            px[zx + x, zy + y] = WithAlpha(r[i], alpha);
        }
    }

    static void PaintTrophy(string dir, TrophyStyle s)
    {
        var px = new Color[64, 64];
        for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) px[x, y] = Clear;
        Func<int, int, Color[]> koi = null;
        Func<int, int, int> mottle = null;
        Func<int, int, Color[]> spots = null;
        if (s.Pattern == "koi") koi = (x, y) => Smooth(x * 0.22 + 3, y * 0.3 + 1, 31) > 0.66 ? s.Accent2 : null;
        if (s.Pattern == "mottle") mottle = (x, y) => Smooth(x * 0.35, y * 0.35, 41) > 0.6 ? -1 : 0;
        if (s.Pattern == "spots") spots = (x, y) => (x % 5 == 2 && y % 4 == 1) || (x % 5 == 3 && y % 4 == 1) ? new[] { s.Dark, s.Dark, s.Dark } : null;
        if (s.Pattern == "dash") spots = (x, y) => (y % 4 == 2 && (x / 2) % 3 == 0) ? new[] { s.Dark, s.Dark, s.Dark } : null;
        Func<int, int, Color[]> backSwap = koi ?? spots;
        RampZone(px, 0, 0, 32, 16, s.Back, 11, s.Alpha, true, mottle, backSwap);                  // BACK
        RampZone(px, 32, 0, 32, 16, s.Belly, 12, s.Alpha, false, null, null);                       // BELLY
        RampZone(px, 0, 16, 32, 16, s.Side, 13, s.Alpha, true, mottle, backSwap);                  // SIDE
        RampZone(px, 32, 16, 32, 16, s.Fin, 14, s.FinAlpha, false, (x, y) => x % 3 == 0 ? -1 : 0, null);   // FIN: rays
        for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++)
        {
            px[x, 32 + y] = (x + y) % 5 == 0 ? Mul(s.Eye, 1.6) : s.Eye;                               // EYE
            px[8 + x, 32 + y] = Hash(x, y, 15) > 0.8 ? Mul(s.White, 0.9) : s.White;                   // WHITE
            px[16 + x, 32 + y] = Hash(x, y, 16) > 0.75 ? Mul(s.Mouth, 1.3) : s.Mouth;                 // MOUTH
            px[24 + x, 32 + y] = Hash(x, y, 17) > 0.75 ? Mul(s.Dark, 1.4) : s.Dark;                   // DARK
        }
        RampZone(px, 32, 32, 32, 16, s.Accent, 18, 255, false, null, null);                         // ACCENT
        RampZone(px, 0, 48, 32, 16, s.Accent2, 19, s.Alpha, false, null, null);                     // ACCENT2
        RampZone(px, 32, 48, 32, 16, s.Glow, 20, 255, false, null, null);                           // GLOW
        RampZone(px, 0, 40, 32, 8, s.Band ?? s.Accent, 21, s.Alpha, false, null, null);             // BAND
        if (s.Pattern == "runes")
        {
            // engraved lines on the gold (ACCENT): a darker groove every few pixels
            for (int y = 0; y < 16; y++) for (int x = 0; x < 32; x++) if ((x + y * 2) % 7 == 0) px[32 + x, 32 + y] = s.Accent[0];
        }
        System.IO.Directory.CreateDirectory(dir);
        Save(px, 64, 64, System.IO.Path.Combine(dir, s.Id + ".png"), (x, y) => true);
    }

    static void MakeTrophies(string entityDir)
    {
        string dir = System.IO.Path.Combine(entityDir, "plaque");
        var styles = new List<TrophyStyle>();
        styles.Add(new TrophyStyle { Id = "giant_catfish", Pattern = "mottle",
            Back = Ramp("2E2818", "4A4428", "5E5836", "726C46"), Side = Ramp("4A4428", "625C3A", "7A744C", "8E8A5E"), Belly = Ramp("A89C74", "C4B890", "DCD2A8", "ECE4C4"),
            Fin = Ramp("2E2818", "443C26", "5A5234"), Accent = Ramp("2A2416", "3A3220", "4A4028"), Accent2 = Ramp("2A2416", "3A3220", "4A4028"), Glow = Ramp("FFFFFF", "FFFFFF", "FFFFFF"),
            Eye = Hex("D8C060"), Mouth = Hex("3A1E14") });
        styles.Add(new TrophyStyle { Id = "golden_koi", Pattern = "koi",
            Back = Ramp("B8460E", "D85A16", "F07A1E", "FF9C3A"), Side = Ramp("C8521A", "E86A1C", "F68A2C", "FFAE52"), Belly = Ramp("E8C8A0", "F6DEC0", "FFEEDA", "FFF8EE"),
            Fin = Ramp("E0904A", "F4AE6A", "FFC88C", "FFDCAC"), Accent = Ramp("C88A3A", "E0A858"), Accent2 = Ramp("D8D0C2", "ECE6DA", "FAF6EE", "FFFFFF"), Glow = Ramp("FFFFFF", "FFFFFF"),
            Mouth = Hex("7A2A10") });
        styles.Add(new TrophyStyle { Id = "ghost_koi", Pattern = "koi", Alpha = 190, FinAlpha = 140,
            Back = Ramp("901428", "B8203A", "D8344E", "F05068"), Side = Ramp("A01A30", "C82A44", "E84860", "FF7084"), Belly = Ramp("F0A0AC", "FFC0CA", "FFD8DE", "FFECEF"),
            Fin = Ramp("E05A70", "F48090", "FFA4B0", "FFC4CC"), Accent = Ramp("FF8090", "FFB0BA"), Accent2 = Ramp("F2C8CE", "FFDCE0", "FFECEE", "FFF6F7"), Glow = Ramp("FFD0D8", "FFE8EC"),
            Eye = Hex("3A0008"), Mouth = Hex("5A0A18") });
        styles.Add(new TrophyStyle { Id = "pufferfish_king", Pattern = "spots",
            Back = Ramp("C88A18", "E8A820", "F6C02E", "FFDA62"), Side = Ramp("D89C1E", "F2B82A", "FFD04A", "FFE27E"), Belly = Ramp("E8CE96", "F6E2B4", "FFF0CE", "FFF8E6"),
            Fin = Ramp("3A145A", "5A2484", "7A36AA", "9A52C8"), Accent = Ramp("8A5808", "C88A14", "FFC834", "FFE88A"), Accent2 = Ramp("C8BC98", "E8DEC2", "FFFAEC"), Glow = Ramp("FFFFFF", "FFFFFF"),
            Band = Ramp("1E3EA8", "3A62E8", "7A9CFF"), Eye = Hex("D01010"), White = Hex("FFFFF4"), Dark = Hex("D01830"), Mouth = Hex("3A1206") });
        styles.Add(new TrophyStyle { Id = "blobfish",
            Back = Ramp("D88894", "EAA2AE", "F6B8C2", "FFD0D6"), Side = Ramp("E296A2", "F2B0BA", "FCC6CE", "FFDCE0"), Belly = Ramp("F0C0C8", "FAD6DC", "FFE6EA", "FFF2F4"),
            Fin = Ramp("C87482", "DE909C", "EEAAB4"), Accent = Ramp("B85A6C", "D4788A", "E8909E"), Accent2 = Ramp("B85A6C", "D4788A"), Glow = Ramp("FFFFFF", "FFFFFF"),
            Dark = Hex("6A2232"), Mouth = Hex("6A2232") });
        styles.Add(new TrophyStyle { Id = "swordfish",
            Back = Ramp("1E2E58", "2C4276", "3A5690", "4C6CAA"), Side = Ramp("6A7E9E", "8496B4", "A0B0C8", "BCC8DA"), Belly = Ramp("C8D2DE", "DCE4EC", "EEF2F6", "FAFCFE"),
            Fin = Ramp("1C2A50", "2A3C6A", "3A5084"), Accent = Ramp("5A6276", "7A8498", "9AA4B6", "B8C0CE"), Accent2 = Ramp("2A3C6A", "3A5084"), Glow = Ramp("FFFFFF", "FFFFFF"),
            Eye = Hex("0A0A12") });
        styles.Add(new TrophyStyle { Id = "ocean_sunfish",
            Back = Ramp("5E6878", "7A8494", "949EAE", "AEB8C6"), Side = Ramp("848E9E", "A0AABA", "B8C2CE", "D0D6DE"), Belly = Ramp("C8CED8", "DCE0E6", "ECEEF2", "F8F8FA"),
            Fin = Ramp("5A6474", "748090", "8E98A8"), Accent = Ramp("8A94A4", "A8B0BE", "C4CAD4"), Accent2 = Ramp("8A94A4", "A8B0BE"), Glow = Ramp("FFFFFF", "FFFFFF"),
            Mouth = Hex("5A3A3A") });
        styles.Add(new TrophyStyle { Id = "electric_eel",
            Back = Ramp("101A1C", "1A282A", "263A3A", "324C4A"), Side = Ramp("1E2E30", "2C4242", "3A5654", "4A6A68"), Belly = Ramp("6A7A56", "7E8E66", "929E76", "A6B088"),
            Fin = Ramp("1A282A", "2A3E3E", "3A5252"), Accent = Ramp("C8A820", "E8CC30", "F6E244", "FFF484"), Accent2 = Ramp("1A282A", "2A3E3E"), Glow = Ramp("A8F0FF", "D8FAFF", "FFFFFF"),
            Band = Ramp("C8A820", "E8CC30", "F6E244", "FFF484"), Eye = Hex("E8D840") });
        styles.Add(new TrophyStyle { Id = "mako_shark",
            Back = Ramp("1E3458", "2C4878", "3C5C92", "4E72AC"), Side = Ramp("3C5C90", "5274AA", "6A8CC0", "84A4D2"), Belly = Ramp("CCD4DE", "E0E6EC", "F0F2F6", "FCFDFE"),
            Fin = Ramp("1E3256", "2C4470", "3C5888"), Accent = Ramp("2C4470", "3C5888"), Accent2 = Ramp("2C4470", "3C5888"), Glow = Ramp("FFFFFF", "FFFFFF"),
            Eye = Hex("050508"), Mouth = Hex("5A1018"), Dark = Hex("142036") });
        styles.Add(new TrophyStyle { Id = "anglerfish", Pattern = "mottle",
            Back = Ramp("2A2020", "3A2C28", "4E3C34", "64503E"), Side = Ramp("3A2C28", "4E3C32", "625044", "7A6450"), Belly = Ramp("62503E", "766250", "8A7462", "A08A74"),
            Fin = Ramp("1E1618", "2C2220", "3C2E28"), Accent = Ramp("3A2C24", "4E3C30"), Accent2 = Ramp("4A3828", "624A36", "7A6048"), Glow = Ramp("40E0B8", "90FFE0", "E8FFF6"),
            Eye = Hex("D8C878"), Mouth = Hex("1A080C"), White = Hex("F4F0DA") });
        styles.Add(new TrophyStyle { Id = "kraken_tentacle",
            Back = Ramp("5A1438", "7A1E4C", "9A2E64", "B8447C"), Side = Ramp("6A1A42", "8A2856", "AA3A70", "C8508A"), Belly = Ramp("C88AA4", "DCA6BC", "ECC2D2", "F8DCE6"),
            Fin = Ramp("5A1438", "7A1E4C"), Accent = Ramp("5A1438", "7A1E4C"), Accent2 = Ramp("5A1438", "7A1E4C"), Glow = Ramp("FFFFFF", "FFFFFF"),
            White = Hex("F4D2DE"), Mouth = Hex("A8607C") });
        styles.Add(new TrophyStyle { Id = "oarfish", Pattern = "dash",
            Back = Ramp("8A96AE", "A6B2C6", "C2CCDC", "DCE2EC"), Side = Ramp("A2AEC2", "BCC6D6", "D4DCE6", "ECF0F6"), Belly = Ramp("C8D0DC", "DCE2EA", "EEF2F6", "FAFCFE"),
            Fin = Ramp("A8182A", "C82234", "E43446"), Accent = Ramp("A01428", "C41E32", "E02E42", "F4505E"), Accent2 = Ramp("A01428", "C41E32"), Glow = Ramp("FFFFFF", "FFFFFF"),
            Dark = Hex("3A4A78") });
        styles.Add(new TrophyStyle { Id = "golden_leviathan_scale", Pattern = "runes",
            Back = Ramp("1A1004", "2E1E06", "4A3208"), Side = Ramp("1A1004", "2E1E06", "4A3208"), Belly = Ramp("1A1004", "2E1E06", "4A3208"),
            Fin = Ramp("1A1004", "2E1E06"), Accent = Ramp("2A1A04", "5A3C08", "8A6410", "C09022", "F0C040"), Accent2 = Ramp("0C0A0E", "18141C", "26202C", "3A3240"),
            Glow = Ramp("FF4A08", "FF9A20", "FFE080"), Band = Ramp("140C02", "3A2606", "6A4A0E"), Dark = Hex("8A1A06") });
        foreach (var s in styles) PaintTrophy(dir, s);
    }

    public static string MakeFish(string entityDir, string itemDir)
    {
        MakeFishIcons(itemDir);
        MakeTrophies(entityDir);
        // ── the textures: the Pirate Hat, the plaque's board ──
        foreach (string name in new[] { "pirate_hat", "fish_plaque", "fish_plaque_epic", "fish_plaque_legendary", "fish_plaque_blood", "fish_plaque_mythic" })
        {
            img = new Color[N, N];
            glow = new bool[N, N];
            glowColor = new Color[N, N];
            Common(Make(name));
            BloodDecor(name);
            if (name == "pirate_hat")
            {
                // BLACK: black felt, a faint nap; GOLD: gold braid; RED: the feather
                Zone(232, 20, 24, 12, (u, v) => Mul(Rgb(26, 24, 28), 0.85 + 0.3 * Fbm(u * 1.5, v * 1.5, 150)), false);
                Zone(224, 32, 32, 12, (u, v) => ((u * 2 + v) % 1.0) < 0.5 ? Rgb(232, 186, 70) : Rgb(180, 130, 40), false);
                Zone(192, 32, 32, 12, (u, v) => Mix(Rgb(170, 20, 30), Rgb(230, 60, 70), Math.Abs(Math.Sin(u * 3 + v))), false);
                Zone(192, 148, 32, 32, BoneColour, false);
                // DECK: the felt of the crown and brim; CANVAS: the gold braid (the big zones)
                Zone(0, 116, 128, 32, (u, v) => Mul(Rgb(28, 25, 30), 0.82 + 0.3 * Fbm(u * 1.2, v * 1.2, 154)), false);
                Zone(0, 84, 64, 32, (u, v) => ((u * 2 + v) % 1.0) < 0.5 ? Rgb(236, 190, 76) : Rgb(176, 126, 38), false);
            }
            else if (name == "fish_plaque_epic")
            {
                // dark polished wood (near black-brown, a deep gloss), a carved border, a brushed silver plate and trim
                Zone(192, 84, 64, 32, (u, v) => Mul(Planks(Rgb(62, 36, 24), u, v, 3.0, 161, 0.7), 0.9 + 0.25 * Math.Max(0, Math.Sin(u * 0.45 + v * 0.25))), false);
                Zone(128, 148, 64, 32, (u, v) => Mix(Rgb(26, 14, 10), Rgb(84, 50, 32), Math.Abs(Math.Sin(u * 1.6) * Math.Cos(v * 1.3)) * 0.8 + 0.2 * Fbm(u, v, 162)), false);
                Zone(128, 44, 40, 40, (u, v) => Mix(Rgb(150, 156, 168), Rgb(236, 240, 246), 0.5 + 0.5 * Math.Sin(u * 1.4 + Fbm(u * 0.2, v * 4, 163))), false);
                Zone(224, 32, 32, 12, (u, v) => Mix(Rgb(170, 176, 188), Rgb(250, 252, 255), 0.5 + 0.5 * Math.Sin(u * 2.2 + v)), false);
                Zone(168, 20, 24, 24, (u, v) => Mix(Rgb(160, 166, 178), Rgb(245, 248, 252), 0.5 + 0.5 * Math.Sin(v * 1.7 + u * 2.1)), false);
                Zone(168, 44, 40, 40, BlackIron, false);
            }
            else if (name == "fish_plaque_legendary" || name == "fish_plaque_blood")
            {
                bool blood = name == "fish_plaque_blood";
                // ebony: black with a faint warm grain and a high gloss; black lacquer rim; gold filigree (crimson-gold
                // for the Ghost Koi's Blood Moon plaque); the plate plain black lacquer under the gold lettering
                Zone(192, 84, 64, 32, (u, v) => Mul(Planks(Rgb(30, 22, 22), u, v, 3.0, 171, 0.5), 0.85 + 0.35 * Math.Max(0, Math.Sin(u * 0.4 + v * 0.3))), false);
                Zone(128, 148, 64, 32, (u, v) => Mix(Rgb(10, 8, 10), Rgb(36, 28, 30), Fbm(u * 0.6, v * 0.6, 172)), false);
                Zone(128, 44, 40, 40, (u, v) => blood ? Mix(Rgb(16, 6, 8), Rgb(30, 12, 14), 0.5 + 0.3 * Math.Sin(v * 0.5)) : Mix(Rgb(10, 9, 10), Rgb(22, 19, 20), 0.5 + 0.3 * Math.Sin(v * 0.5)), false);   // plain, under the lettering
                if (blood) Zone(224, 32, 32, 12, (u, v) => Mix(Rgb(120, 14, 24), Rgb(236, 90, 90), 0.5 + 0.5 * Math.Sin(u * 2.4 + v * 1.3)), false);
                else Zone(224, 32, 32, 12, (u, v) => GoldLeaf(u, v, 5), false);
                Zone(168, 20, 24, 24, (u, v) => GoldLeaf(u, v, 6), false);
                Zone(168, 44, 40, 40, BlackIron, false);
            }
            else if (name == "fish_plaque_mythic")
            {
                // obsidian: black glass with purple depths and sharp glints; an obsidian rim; a heavy gold frame; a black
                // plate for glowing letters; rubies; the runes ember-gold (drawn at full brightness)
                Zone(192, 84, 64, 32, (u, v) => Hash((int)(u * 4), (int)(v * 4), 181) > 0.985 ? Rgb(200, 180, 255) : Mix(Rgb(10, 6, 16), Rgb(46, 30, 64), Math.Pow(Fbm(u * 0.5, v * 0.5, 182), 2) + 0.2 * Math.Max(0, Math.Sin(u * 0.7 - v * 0.4))), false);
                Zone(128, 148, 64, 32, (u, v) => Mix(Rgb(6, 4, 10), Rgb(30, 20, 40), Fbm(u * 0.7, v * 0.7, 183)), false);
                Zone(128, 44, 40, 40, (u, v) => Mix(Rgb(8, 6, 10), Rgb(18, 13, 22), 0.5 + 0.3 * Math.Sin(v * 0.5)), false);
                Zone(224, 32, 32, 12, (u, v) => GoldLeaf(u, v, 7), false);
                Zone(192, 32, 32, 12, Ruby, false);
                Zone(128, 116, 64, 32, (u, v) => Mix(Rgb(255, 120, 20), Rgb(255, 230, 140), 0.5 + 0.5 * Math.Sin(u * 2 + v)), false);
                Zone(168, 44, 40, 40, (u, v) => Mix(Rgb(8, 6, 10), Rgb(40, 34, 44), Fbm(u * 0.8, v * 0.8, 185)), false);
            }
            else
            {
                // DARKWOOD: polished walnut, warm and glossy; OLDWOOD: the dark carved rim; BRASS: the plate, brushed
                Zone(192, 84, 64, 32, (u, v) => Mul(Planks(Rgb(124, 74, 44), u, v, 3.0, 151, 0.8), 0.95 + 0.12 * Math.Max(0, Math.Sin(u * 0.5 + v * 0.2))), false);
                Zone(128, 148, 64, 32, (u, v) => Mix(Rgb(40, 22, 14), Rgb(70, 40, 24), Fbm(u * 0.6, v * 0.6, 152)), false);
                Zone(128, 44, 40, 40, (u, v) => Mix(Rgb(176, 130, 60), Rgb(232, 196, 120), 0.5 + 0.5 * Math.Sin(u * 1.6 + Fbm(u * 0.2, v * 4, 153))), false);
                Zone(168, 44, 40, 40, BlackIron, false);
                Zone(224, 32, 32, 12, (u, v) => GoldLeaf(u, v, 2), false);
            }
            Save(img, N, N, System.IO.Path.Combine(entityDir, name + ".png"), (x, y) => true);
        }
        return "saved";
    }
}
