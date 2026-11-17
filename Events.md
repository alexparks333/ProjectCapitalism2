# World events

Big nights that admins start. `/event <type> start | end | delay <time>` (ops; `event/BloodMoon`):
- `start`: now. A Blood Moon is a night, so if it's day the sun goes down. `start <boss>` picks the boss.
- `end`: now (or calls off one that's coming).
- `delay <time>`: starts in that long (`30s`, `10m` isn't valid: use `600s`; `1d` = one Minecraft day = 20 minutes;
  plain numbers are ticks). Again while one is coming: pushes it back that much more.
- `/event bloodmoon` alone says what's on or coming.
- (dev) `/event bloodmoon tp`: teleports you 6 blocks from tonight's boss, facing it (careful: getting that close counts as finding it). `/tp town` takes you back to the middle of town. Both work from the overworld only.
- (dev) `/dev kit` (`core/DevCommands`): a kit to practise on the bosses: a Power II + Infinity bow and an arrow, a Sharpness III diamond sword, Protection I diamond armour (put on if the slot is empty), 32 steak and 16 Purified Water.

Events: `bloodmoon`, `purge` (`/event purge [start | schedule | end]`, below).

## The Purge (`event/Purge`)
A rare night with no laws. It runs **dusk to dawn of one night** (13000 to 23000 on the clock).
- **When:**
  - **Once every 10-12 days** (random, `event/EventSchedule`): announced the morning before for the **next** night. A
    natural Purge never lands on a natural Blood Moon's night (one moves on a day); the Mayor's switches CAN stack them.
  - The announcement: sirens (raid horns, three wails), the bottom-right card and chat: "The Purge begins at dusk
    tomorrow. No laws until dawn."
  - Reminders that evening, the next morning ("...at dusk tonight") and a minute before dusk (with sirens).
  - Players logging in are told.
- **It CAN share a night with a Blood Moon:** a Blood Moon on or coming doesn't stop The Purge being announced or starting, the Blood Moon Altar takes offerings on a Purge day, and neither waits for the other. Both at once: monsters at Blood Moon strength and no laws.
  - If a Blood Moon is somehow on at dusk, the Purge is called off.
- **It begins:** sirens, the card ("The Purge / No laws until dawn / The town is a safe zone") and chat. A red boss bar,
  "THE PURGE: no laws until dawn", counts the night down. Nobody can sleep through it.
- **PvP is on everywhere** except the town.
  - Normally PvP on this server is only the vanilla `pvp` game rule (on by default; the mod has no PvP rules of its
    own).
  - At dusk the Purge saves the rule's value and sets it on. At dawn it puts the saved value back exactly. The saved
    value is kept in the Purge's saved data, so a restart mid-Purge still restores it.
  - An admin changing the rule during the night is overwritten at dawn.
- **The town is a safe zone** (always, not just tonight: `Town.md`): inside the protected town (`Town.protectedArea`, the whole column, overworld), no player
  or player's pet can hurt a player, and nobody inside can hurt anyone outside.
- **The death tax goes to the killer** (`Insurance.md`): killed by another player outside the town, a victim's death
  tax (after their insurance) is owed to the killer (only the tax: the gold the victim drops stays theirs, locked to
  them), and paid to them when the victim pays (at their next login if
  they're away).
  - Killed by a monster, by themselves or in the town, it goes to the bank.
  - A kill after dawn is ordinary: if the Purge ends mid-fight, kills from then on are the bank's.
  - Killing an alt only moves the alt's own gold: no gold is created.
- **It ends at dawn** (or `/event purge end`): PvP is put back, a bell rings, and the night's top 3 killers are shown.
- **Standings** on the Bounty Clerk's Ledger: **Purge kills** and **Gold taken** (`Bounties.md`).
- **Admin:** `/event purge` (what's on or coming), `start` (now; if it's day the sun goes down), `schedule` (announce
  it for tomorrow night), `end` (now, or call off an announced one).
- **Tests:** `deathtax/DeathTaxTests` ("purge"):
  - A Blood Moon doesn't stop it (they can share a night).
  - Announced for tomorrow, and on the day it claims the night.
  - It starts at night with PvP on (it was off).
  - A player kill outside town: the tax is owed to the killer, and the kill is on the standings.
  - The killer can't pick up the victim's dropped gold (it's locked to the victim, `Economy.md`): only the tax.
  - In the town nobody is hurt, from inside or out, and a death there is the bank's.
  - Monster kills and self-kills go to the bank.
  - Paying hands the killer exactly their share, and "Gold taken" counts it.
  - An offline killer's share waits and is delivered at login.
  - Dawn puts PvP back (off), and a kill after it is the bank's.

## The Blood Moon (`event/BloodMoon`)
- **When:** naturally **once every 6-8 nights** (random, `event/EventSchedule`), rising at dusk; never on a natural Purge's night (or a night The Purge already has: a day later). Also from the Blood Moon Altar and the Mayor's Blood Moon Switch (which can share a night with The Purge).
- **It begins:** three tolls of a bell, the bottom-right card ("Blood Moon / The moon rises red tonight... / Hunt
  <the boss>"), a chat message. The moon turns full, big and blood red, the night sky and fog red, the stars
  fade (`client/BloodMoonClient`, `mixin/SkyRendererMixin`). A red bar across the top shows how much night is left.
- **Monsters** that spawn in the overworld during it: **20% Level I, 60% Level II, 20% Boss** (instead of the usual
  60/30/10, `core/TougherMobs`). **Every monster gives double XP** (on top of the Level II / Boss bonus).
- **Nobody can sleep** through it. **Dying is dying:** nothing is softened, it's a risk you take. Insurance hides too: every death pays the full death tax unless you hold Blood Moon Insurance (`Insurance.md`).
- **Points** for every monster you kill: **Level I 1, Level II 2, Boss 4, the Blood Moon boss 35**, shown on the
  sidebar ("Blood Moon Kills", in place of the Gold Leaderboard for the night) and as "+2" over your hotbar.
- **Blood Shards** (`capitalism:blood_shard`): monsters you kill drop them: Level I 15%, Level II 35%, a Boss 1-2,
  a Blood Moon boss 20. They buy the Blood Merchant's decorations and pay to mend the Blood Moon Crest (`Town.md` >
  The Blood Merchant).
- **It ends at dawn** (or `/event bloodmoon end`): the results in chat and on the card (top 3), and the **top hunter
  gets 25 gold** (given at their next login if they're off).
- **Calling one down:** players can feed **Blood Moon Altars** Blood Shards by day; when the server-wide pool reaches
  the week's price (150, then 300, 450... back to 150 on Monday) a Blood Moon rises at dusk that night
  (`BloodMoon.scheduleTonight`, `decor/BloodAltar`, `Decorations.md` > Blood Moon decorations). Short by dusk, the
  shards go back.
- **The decorations notice:** every Grandfather Clock tolls 13 times as it begins, Crimson Chandeliers gutter out till
  it's over, the Blood Fountain runs red (Bottles of Blood: Strength and Speed till dawn, taken away when it ends), and
  the Vampire Coffin can't be slept in (`Decorations.md`).

## The switches (`event/EventSwitchBlock`)
- **Purge Switch** and **Blood Moon Switch**: big levers on heavy plates (black with a hazard border and a red knob; crimson with gold and a blood-red crystal knob). **Nobody can craft them** (an admin hands them out).
- **Only the Mayor** (`town/TownHall`, whoever holds the office now) can throw one. The Purge Switch announces The Purge for **dusk tomorrow** (`Purge.schedule`, the usual warning); the Blood Moon Switch brings a **Blood Moon at dusk tonight** (`BloodMoon.scheduleTonight`).
- Thrown, it stays down until that event is over, then springs back up, ready again (checks every 5 s): no spamming. If another event is on or coming it says so and stays up.

## The bosses (`event/BloodMoonBoss` and one class each; models in `client/`)
Each Blood Moon one of three is picked at random (`/event bloodmoon start <revenant|gorefang|witch>` picks it).
What they all share (`BloodMoonBoss`):
- **Hidden:** it appears as the Blood Moon starts, **150 to 400 blocks out from the town's middle** in a random
  direction, on dry ground. Clues: a **red pillar of light** into the sky over it (seen within 100 blocks), and its
  **heartbeat**, louder as you get close (within ~56 blocks). Whoever gets within 24 blocks and sees it: "**Sam has
  found ...!**" to everyone (and the top bar says so).
- **Its bar** (health) shows to everyone within 48 blocks. Doesn't burn, drown or take fall damage.
- **Being hit:** a burst of blood where you hit it, it flashes red and reels back, and it's shoved back a little
  (knockback resistance under 1). Its hitbox covers its whole body (sized after its SCALE: `BloodMoonBoss` constructor).
- **The monsters are on its side:** none of them targets or hurts it (a skeleton's stray arrow does nothing), and its
  slams and eruptions don't hurt them.
- **Eruptions** some moves call up: a glowing circle marks the spot and a ring closes in to its middle as the
  countdown, then spikes burst up out of the ground. Move!
- **Every attack has its own look and colour** (`event/BossFx`: glowing trail particles, which keep crisp rings, lines
  and waves; dust smears), so you can tell what's coming. Each boss's colours are listed with its moves.
- **Hiding doesn't work** (`BloodMoonBoss`: anti-cheese):
  - **Arrows and other shots do 25% less** to a boss; melee is the way to fight it.
  - **Out of reach, it heals:** if for 15 seconds nobody has hit it up close and it hasn't hurt anyone, while it's in
    a fight, it heals 3 a second (rising to 6): dark blood drawn into it from all round, a heart over it.
  - **Walled in, it breaks in:** if it can't find a way to walk to its target (shut in a hut, up a pillar) and hasn't
    reached anyone for 4 seconds, it tears through blocks: the Revenant leaps onto you and comes down through the roof
    (and smashes walls it walks into), Gorefang charges without needing to see you and plows through walls instead of
    being dazed, the witch's spears smash down through the roof to your feet (and come sooner) and her bolts blast holes
    in walls (she fires them without seeing you). It never breaks blocks in the protected town or on a lot, anything
    with a block entity (chests, furnaces...), anything as tough as obsidian, or below your feet; broken blocks drop.
- **In water** (`BloodMoonBoss.tickSwim`): it stays afloat, head out, swims hard at its target (6 blocks a second)
  and heaves itself up out onto the bank; its moves all work in water (the witch just stays up on the water and keeps
  her distance). Each has a **water move** for when it or its target is in water (see each boss). Eruptions that burst
  on water are geysers: a tall white column of spray.
- **Thralls** at half health and again at a quarter: every one that dies within 24 blocks of it **heals it 12**.
- **Killed:** 35 points to whoever lands the killing blow, and it drops **15 gold**, **20 Blood Shards** and **its piece of the Blood Moon
  Crest** (see Trophies). **Not
  killed by dawn:** it sinks into the earth and escapes. Only tonight's boss stays (a `/summon`ed one sinks away).

### The Crimson Revenant (`CrimsonRevenant`)
A huge undead knight (near 4 blocks tall): blackened iron plate with red-hot cracks, a horned helm with burning eyes,
spiked pauldrons, a tattered crimson cape, a greatsword with a glowing line down its blade. 360 health, armour 12, hits
for 14.
- **Ground slam (ember orange):** when players crowd it, it raises its sword for a second and smashes the ground: 12
  within 5.5 blocks, thrown back and up. An orange danger circle shows its reach, a ring closing in counts down, embers
  rise into the sword; then an orange shockwave rolls out.
- **Sweep (steel white):** up close it draws its sword back and spins a full circle: 10 within 4.8 blocks, flung
  aside. A white circle shows its reach; the swing whips out a spiral of white streaks.
- **Blood line (crimson):** at middle range it drives its sword into the ground: a glowing crack runs out at you and
  crimson eruptions burst along it one after another: step out of the line.
- **Leap (gold):** anyone who keeps away (9 to 30 blocks), or shoots it from more than 12 blocks off, gets leapt at; a
  gold circle marks where it comes down, a gold streak behind it, a smaller gold shockwave slam on landing.
- **Geysers (aqua, water move):** when it or its target is in water it drives its sword down, bubbles and spray boiling
  round it, then geysers burst up under everyone near (aqua circle and countdown first, 10, thrown up) and in a ring
  round it (8). Photos `bm_geyser`, `bm_geyser_burst`.
- **Thralls (soul blue):** five Level II zombies, a ring of blue soul fire round it as they rise.

### Gorefang, the Blood Wolf (`Gorefang`)
A huge dire wolf (over 3 blocks tall): a hunched shoulder hump under a spiky black mane, a mask of bone with ram's horns
sweeping back and burning eye holes, ribs showing through its flanks with fire glowing behind them, a ridge of bone
spikes, a spiked iron collar with broken chain, veins of fire down its legs, a smouldering tail tip. 320 health, fast.
- **Bite (dark red):** up close; three claw slashes across your view, and you bleed (Wither for 3 seconds).
- **Pounce (blood red):** it crouches low for a moment, then springs: 10 and slowness to everyone within 3.2 blocks of
  where it lands. A red ring with claw marks follows you while it crouches (that's where it lands); a red streak behind
  it in the air; a red shockwave on landing.
- **Charge (amber):** it lowers its head and scrapes the ground, then thunders straight ahead, ramming and flinging anything in
  its way (11). If it hits a wall or a tree it's **dazed for 2 seconds**: hit it then! Its lane is painted on the ground in
  **amber** first (as far as it will run, or up to the wall it will hit), white wind streams off it as it runs, and
  **gold stars** circle its head while it's dazed.
- **Howl (ghost blue):** it rears up and howls: three pale blue rings of sound roll out 20 blocks; Darkness and
  slowness on players near, every monster near gets faster and stronger.
- **Riptide (sea green and white foam, water move):** when it or its target is in water (3 to 18 blocks) a churning ring
  of water spins round it, then it surges straight at you through the water like a shark, a white wake behind it:
  10, slowness, and **dragged under** if you're in water. It bursts up out of the water at the end. Photos
  `bm_wolf_riptide`, `bm_wolf_riptide_surge`.
- **Thralls:** its howl at half and a quarter health calls four **Blood Hounds** (angry Level II wolves).
- **Frenzy (fire):** under a third of its health it bursts into embers (and keeps smouldering), faster, its moves
  come round quicker.

### The Sanguine Witch (`SanguineWitch`)
A tall blood sorceress floating off the ground: great bat-like wings of blood-red membrane on bone, crimson robes
fraying into wisps, a deep hood, long black hair, a pale face with burning eyes and tears of blood, a tall crown of bone,
a bone staff crowned with a glowing blood crystal, three orbs of blood circling her. 260 health. She keeps a little distance (backs off if you're right on her).
- **Blood bolts (red):** a fan of five slow orbs of blood that curve after you a little (5 each, no knockback); under half health, two volleys.
- **Blood rain (storm cloud, pale pink circles):** a dark storm cloud gathers over everyone near (and round her
  target), a pale pink circle under each with a ring closing in as the countdown; drips start, then a downpour of blood
  streaks hits it with a splash (10). Step out of the circle.
- **Spear storm (gold):** gold marks with a cross round everyone near, a thin pillar of gold light up into the sky
  over each, then golden spears fall down the pillars (8 each).
- **Blood nova (crimson wave):** she rises into the air, wings spread wide, and sends out **waves of blood rolling along the
  ground** (one; two, one behind the other, under half health): **jump over them** or take 9 and be thrown back.
  Each wave is a knee-high wall of glowing blood with spray off its crest, rolling out 9 blocks a second to about 20
  blocks (trail particles launched once that glide out in
  exactly the wave's time, so it stays a crisp band; photo `bm_witch_ring`).
- **Drain (violet):** a spinning violet ring locks round your feet and a tether reaches out from her hand to you (a
  second's warning); then violet streams flow from you into her with souls torn out of you, 2 every half second,
  while she can see you: break line of sight or get away. Each pulse that lands **heals her a little (2)**, shown by a
  lilac glow swelling over her and a heart (at most 14 a drain).
- **Exhausted:** after a nova she sinks to the ground for 2 seconds, worn out: your chance to get close and hit her.
  Strike now!
- **Blood bubble (glassy dark blood sphere):** shot **5 times** (arrows, tridents...) with nobody hitting her up close
  in between, she shuts herself in a big bubble of blood: no attacks, she heals 4 a second, and **any shot that hits
  the bubble heals her** by its damage (it bounces off; nothing else gets through). **A melee hit pops it** (that hit
  lands) and she's stunned (Exhausted) for 2 seconds. With nobody within 32 blocks and her health full she lets it go.
  Photo `bm_witch_bubble`.
- **Maelstrom (sea blue and white, water move):** when her target is in water a whirlpool opens round them: a second's
  warning as its outline and spiral arms form, then for 2 seconds it pulls everyone within 5 blocks round, in and under;
  in its dark eye it hurts (3 every half second). Swim out hard. Photo `bm_witch_maelstrom`.
- **Blink (black ink):** crowd her or keep hitting her up close and she vanishes in a puff of black ink and smoke and
  appears nearby.
- **Thralls (soul blue):** four Level II skeletons ("Bone Thrall").

## Trophies: the Blood Moon Crest (`ModItems`, textures by `tools/make_blood_crest.ps1`)
Each boss drops its broken third of a round crest (dark iron rim with gold studs, a blood moon in the middle, a dark
blood-red field), its break glowing red-hot:
- **Upper Left Revenant Crest** (the Crimson Revenant): the Revenant's greatsword, a red line down its blade, a gold guard.
- **Upper Right Sanguine Crest** (the Sanguine Witch): a crimson bat wing on bone fingers.
- **Lower Gorefang Crest** (Gorefang): two great white fangs over a red gum.

The **Blood Merchant** (`Town.md`) mends all three into the **Blood Moon Crest** (glinting, gold seams where they join)
for 50 Blood Shards, and trades an **Essential Wishing Star** for a whole crest (once per player). A piece is also part
of the price of that boss's trophy decoration at his shop. There's no crafting recipe: only he can mend it.

Textures: "crimson_revenant", "gorefang", "sanguine_witch" in `tools/BoatTex.cs` (glow parts in `<id>_glow.png`).
Photos: `gradlew runPhoto -Pphoto=bloodmoon` (the attack shots set `photoAi`, so a boss held still for the camera still
makes its moves, at a villager standing in for a player).
