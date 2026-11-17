# Project Capitalism 2

NeoForge mod (Minecraft 26.3, NeoForge 26.3 beta). Source in `src/main/java/com/alexh/capitalism`, design notes in the
`*.md` files at the root (`Smiths.md` = overview of the three smith jobs, `Jobs.md`, `Weaponsmith.md`, `Toolsmith.md`, `Armorsmith.md`, `Enchanter.md`, `Librarian.md`, `LibrarianBooks.md`, `PetTrainer.md`, `PetAnimals.md`, `Designer.md`, `Farmer.md`, `RealEstate.md`, `Boats.md`, `Back.md`, `Town.md`, `Sockets.md`, `LavaCreator.md`, `Economy.md`, `Calendar.md` (days, seasons, the clock, festivals), `Decorations.md`, `Mailbox.md`, `Events.md` (Blood Moon and its three bosses), `Fishing.md` (rare catches, the Fisherman, plaques), `Insurance.md` (the death tax, Purgatory, `/pay death`, Not-A-Scam Insurance; The Purge is in `Events.md`), `Bounties.md` (the Bounty Clerk's daily tasks), `Cravings.md` (daily food and drink cravings)
(seasonal decorations)...).
Keep those notes matching the code when things change.

## Rules to keep
- **NPC shops: every price a player pays when BUYING from an NPC must go through
  `economy/NpcPricing.npcPrice(fairNuggets, player)`.** It adds the NPC markup (+50%) and Emerald-socketed armor's discount
  (7.5% off per piece worn, see `Sockets.md`) and festival sales (`Calendar.md`). Shop stock lists the FAIR (player) price. Don't compute NPC prices
  anywhere else. Player-run Shopfronts are not NPCs. (The Whisperer, `Town.md`, is different on purpose: he sells
  Knowledge Pieces for emeralds. So is the Blood Merchant: Blood Moon decorations and a Wishing Star for Blood Shards
  and crest pieces. The Traveling Merchant (`Town.md`) is an NPC: his prices DO go through `npcPrice`. Not-A-Scam Insurance's quotes are flat company prices from the server config, not NPC prices
  (`Insurance.md`), like the Bounty fee. The Fisherman only BUYS (`Fishing.md`); his plaque mounting does go through `npcPrice`. The Bounty Clerk's 2-nugget task fee is a flat fee, not a shop price (`Bounties.md`).)
- **NPC shops never sell anything with effects** (meals or drinks that give an effect when eaten / drunk, like Miner's
  Casserole or any Specialty drink). Only real Cooks and Bartenders make those, so players still need each other.
- Overrides of vanilla recipes in `data/minecraft/recipe/` only work because `neoforge.mods.toml` loads this mod AFTER
  NeoForge (`ordering="AFTER"`). Don't remove it: in dev, NeoForge's data pack carries copies of the vanilla recipes.
- Job-only crafting uses the recipe type `capitalism:job_shaped` (`"job"` can be one job or a list).
- New jobs go at the END of `occupation/Job.java` (players' jobs are synced by position). Every job is Essential or Fun
  (`Job.Category`); a player starts with one slot of each, picked for good in `/pick`; Wishing Stars at the Wishing
  Well add a slot, a Changing Star lets them change (`JobData`, `Jobs.md`).
- Textures are drawn with PowerShell (System.Drawing); the smith table menu is drawn by `tools/make_smith_gui.ps1`.
- Game tests: `gradlew runGameTestServer` (headless, own world in runs/gameTestServer) runs the mod's tests
  (`gametest/GameTests` registers them: `librarian/SkillBookTests`, `player/FoodDrinkTests`, `town/TownTests` (incl. town hall, job picking), `town/FishermanTests`, `town/MayoralChestTests`, `deathtax/DeathTaxTests` (death tax, Purgatory, `/pay death`, insurance, the Purge), `pets/PetTests`, `designer/DesignerTests`, `farmer/FarmerTests`, `realestate/RealEstateTests`, `boats/BoatTests`, `calendar/CalendarTests`, `decor/DecorTests`, `decor/BloodDecorTests`, `fishing/FishingTests`, `weaponsmith/SocketTests` (Molten Socket gems), `bounty/BountyTests`, `craving/CravingTests`; see `Librarian.md` > Testing). Run it
  after changing skill books, meals or drinks; add a check when you add one. `/devpanel` shows debug info in game.
- Seeing how things LOOK: `gradlew runPhoto` (DEV ONLY, `client/PhotoShoot`, runs/photo, its own fresh flat world)
  builds a scene, takes scripted screenshots (runs/photo/screenshots) and quits. Add shots there for new visuals.
  `-Pphoto=mobs|well|mail|signs|bloodmoon|merchant|traveler|blooddecor|blooditems|bloodbanner|fish|bounty|insurance|guide|perf` runs just those shots; `perf` is a frame-time benchmark with a Flight Recorder profile
  (runs/photo/perf.jfr: `jfr view hot-methods`, `jfr view gc-pauses`).
