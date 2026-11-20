# (palette letters are not case-sensitive: PowerShell hashtables ignore case)
# Effect icons (18x18) for the second round of Specialty drinks and meals (core/ModEffects). Each icon is a little
# pixel-art grid (one character per pixel, '.' = clear) drawn into the middle of the 18x18 icon.
# Run: powershell -ExecutionPolicy Bypass -File tools\make_effect_icons.ps1
Add-Type -AssemblyName System.Drawing
$out = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures\mob_effect'

function Icon($name, $palette, $rows) {
    $b = New-Object System.Drawing.Bitmap 18, 18
    $h = $rows.Count; $w = $rows[0].Length
    $ox = [int][Math]::Floor((18 - $w) / 2); $oy = [int][Math]::Floor((18 - $h) / 2)
    for ($y = 0; $y -lt $h; $y++) {
        for ($x = 0; $x -lt $w; $x++) {
            $ch = [string]$rows[$y][$x]
            if ($ch -eq '.') { continue }
            $b.SetPixel($ox + $x, $oy + $y, [System.Drawing.Color]::FromArgb([Convert]::ToInt32("FF" + $palette[$ch], 16)))
        }
    }
    $b.Save((Join-Path $out "$name.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $b.Dispose()
    Write-Output "$name.png"
}

# Steady Forge: an anvil with a spark over it
Icon 'steady_forge' @{ k = '2A2A30'; g = '6E7280'; l = 'A8ACB8'; o = 'FF8A2A'; y = 'FFD860' } @(
    '......y.....',
    '.....yoy....',
    '......y.....',
    '............',
    'kkkkkkkkkkk.',
    'klllllllllgk',
    '.kggggggggk.',
    '...kgggggk..',
    '....kgggk...',
    '...kgggggk..',
    '..kkkkkkkkk.')

# Lucky Line: a fishhook on its line, a star by it
Icon 'lucky_line' @{ k = '303844'; s = 'B8C4D0'; w = 'E8EEF4'; y = 'FFD860'; o = 'F0A030' } @(
    '.....w......',
    '.....w...y..',
    '.....w..yoy.',
    '.....w...y..',
    '.....s......',
    '.....s......',
    '.....s......',
    '.k...s......',
    '.ks..s......',
    '..ks.s......',
    '...kss......',
    '....k.......')

# Steady Hands: a cleaver
Icon 'steady_hands' @{ k = '2A2A30'; s = 'C8CCD4'; w = 'F0F2F6'; d = '8A8E98'; b = '6A3A1E'; r = '8A4E28' } @(
    'kkkkkkk.....',
    'kwwwwwsk....',
    'kwsssssk....',
    'kssssssk....',
    'kssssssk....',
    'kdddddddkk..',
    '.kkkkkkkbrk.',
    '.........brk',
    '..........bk',
    '...........k')

# Quick Study: an open book with a little lightning bolt
Icon 'quick_study' @{ k = '2A2040'; p = 'F4ECD8'; d = 'C8BCA0'; c = '6A4AC8'; y = 'FFD860' } @(
    '.....y......',
    '....yy......',
    '...yyyy.....',
    '.....y......',
    '....y.......',
    'kkkkk.kkkkk.',
    'kpppkckpppk.',
    'kpddkckddpk.',
    'kpppkckpppk.',
    'kpddkckddpk.',
    'kkkkkkkkkkk.')

# Silver Tongue: a gold coin with a price tag
Icon 'silver_tongue' @{ k = '6A4A10'; g = 'F0C030'; y = 'FFE880'; d = 'C89418'; s = 'D8D8E8'; t = '8A8AA0' } @(
    '...kkkk.....',
    '..kggggk....',
    '.kgyyggdk...',
    '.kgykkgdk...',
    '.kgkggkdk...',
    '.kggkgddk...',
    '..kddddk....',
    '...kkkk.t...',
    '.......tss..',
    '......tssss.',
    '.......tsst.',
    '........tt..')

# Quenched: a water drop with a shine
Icon 'quenched' @{ k = '1E4E7A'; b = '46A0E8'; l = '8CCCF8'; w = 'F4FAFF'; d = '2E78B8' } @(
    '.....k......',
    '.....k......',
    '....kbk.....',
    '....kbk.....',
    '...kbbbk....',
    '..kbwbbbk...',
    '..kwlbbbk...',
    '.kblbbbbdk..',
    '.kbbbbbbdk..',
    '..kbbbbdk...',
    '...kkkkk....')

# Moonstalker: a crescent moon crossed by a red slash
Icon 'moonstalker' @{ m = 'F4ECB0'; d = 'C8BC78'; r = 'D02040'; x = '8A1428' } @(
    '....mmm.....',
    '..mmm.......',
    '.mmd........',
    '.md.......r.',
    'mmd......rx.',
    'mmd.....rx..',
    'mmd....rx...',
    '.md...rx....',
    '.mmd.rx.....',
    '..mmrx......',
    '...rxmm.....',
    '....mm......')

# ══ The second round of Specialty meals ══

# Tempered: a chestplate with a cool steel sheen
Icon 'tempered' @{ k = '2A3040'; s = '8A9AAA'; l = 'C8D4E0'; d = '5A6A7A'; w = 'F0F6FA' } @(
    '.kkk...kkk..',
    'kssskkksssk.',
    'ksllsssslsk.',
    'kslwssssssk.',
    '.kslsssssk..',
    '.kslssssdk..',
    '.ksssssddk..',
    '.kssssdddk..',
    '.kddddddk...',
    '..kkkkkkk...')

# Timber: an axe over a log end
Icon 'timber' @{ k = '2A2A30'; s = 'C8CCD4'; w = 'F0F2F6'; b = '6A3A1E'; r = '8A6236'; y = 'C89A5A'; c = 'E8C88A' } @(
    '....kkk.....',
    '...kwssk....',
    '...ksssbk...',
    '....kkbk....',
    '.....bk.....',
    '....bk......',
    '..rrrrrr....',
    '.ryccccyr...',
    '.rcyyyycr...',
    '.rcyccycr...',
    '.ryccccyr...',
    '..rrrrrr....')

# Long Reach: a block with a long arrow reaching out from it
Icon 'long_reach' @{ k = '3A2A1A'; o = 'E8A040'; d = 'B87830'; w = 'FFFFFF'; a = 'F0F0F0' } @(
    '.......a....',
    '.......aa...',
    'aaaaaaaaaa..',
    '.......aa...',
    '.......a....',
    '............',
    'kkkkk.......',
    'koook.......',
    'kodok.......',
    'kooodk......',
    'kkkkk.......')

# Hearty: a big red heart with a plus
Icon 'hearty' @{ k = '5A0E14'; r = 'D83A3A'; l = 'F08080'; w = 'FFFFFF' } @(
    '.kkk..kkk...',
    'krrrkkrrrk..',
    'krlrrrrrrk..',
    'krlrrwrrrk..',
    'krrrwwwrrk..',
    '.krrrwrrk...',
    '..krrrrk....',
    '...krrk.....',
    '....kk......')

# Quick Bite: a fish with a spark at its mouth
Icon 'quick_bite' @{ k = '1E3A4A'; b = '4AA0C8'; l = '8CCCE8'; y = 'FFD860'; e = '102030' } @(
    'y...........',
    '.y..........',
    'yyy.........',
    '...kkkkk.kk.',
    '..kblllbkbk.',
    '.kebbbbbbbk.',
    '..kbbbbbkbk.',
    '...kkkkk.kk.')

# Comforted: a bowl of soup, steaming
Icon 'comforted' @{ k = '4A2A14'; b = '8A5A30'; y = 'F0C860'; w = 'F0F0F0' } @(
    '..w...w.....',
    '.w...w...w..',
    '..w...w.w...',
    '.w...w...w..',
    '............',
    'kkkkkkkkkkk.',
    'kyyyyyyyyyk.',
    'kbbbbbbbbbk.',
    '.kbbbbbbbk..',
    '..kkkkkkk...')

# Second Wind: swirls of wind
Icon 'second_wind' @{ w = 'E8F4FA'; b = '78C8E8'; d = '4A98B8' } @(
    '......bbb...',
    '.....b...b..',
    'wwwwwwww..b.',
    '.........b..',
    'bbbbbbbbbb..',
    '............',
    'dddddddd....',
    '........d...',
    '..wwwww..d..',
    '........d...',
    '...dddd.....')

# Night Watch: a half-closed eye under a little moon
Icon 'night_watch' @{ k = '10182A'; n = '2A3A6A'; w = 'D8E0F0'; p = '4A6AC8'; m = 'F4ECB0' } @(
    '.........mm.',
    '........m...',
    '........m...',
    '.........mm.',
    '..kkkkkkk...',
    '.knnnnnnnk..',
    'kwwwpppwwwk.',
    '.kwwpkpwwk..',
    '..kkkkkkk...')
