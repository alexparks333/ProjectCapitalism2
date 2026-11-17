# Project Capitalism 2 — Dev Setup

**Versions:** Minecraft **26.3**, NeoForge **26.3.0.10-beta**, Java **25** (Temurin). Mod id `capitalism`, version `0.1.0`. (All in `gradle.properties`.)

## Prerequisites
1. **Java 25** (Temurin, from adoptium.net). `gradle.properties` points Gradle at `C:/Program Files/Eclipse Adoptium/jdk-25.0.4.101-hotspot`; change `org.gradle.java.home` if yours is somewhere else.
2. **IntelliJ IDEA Community** (optional; everything also works from a terminal).
3. The Gradle wrapper is already in the project (`gradle/wrapper/`), nothing else to download by hand.

## Building
- `.\gradlew.bat compileJava processResources` compiles the code and copies the assets/data (quick check).
- `.\gradlew.bat build` makes the mod jar: `build/libs/capitalism-0.1.0.jar`.
- The first build downloads Minecraft + NeoForge (a few minutes).

## Running
- `.\gradlew.bat runClient` starts a dev client (files in `runs/client`).
- `.\gradlew.bat runServer` starts a dev server (files in `runs/server`; config in `runs/server/config/capitalism-server.toml`).
- After changing code, **restart both** the server and the client: an old server still makes the old decisions (e.g. what you're allowed to eat) even with a new client.

## Starting a fresh world
- The dev server's world is the folder named by `level-name=` in `runs/server/server.properties` (now `world_capitalism`, seed `GoldRush`; the first world is kept in `runs/server/world`). To start over (new map, nothing built), **stop the server**, then rename or delete that folder; the next start makes a brand new world. The mod and its config (`runs/server/config/`) are untouched.
- Players' jobs, discoveries, diet etc. are saved in the world, so they start fresh too (players pick them again with `/pick`).
- Want a particular map? Put its seed in `level-seed=` in `runs/server/server.properties` before starting (empty = random).

## Mixins
- The mod uses a few small mixins (`src/main/resources/capitalism.mixins.json`, classes in `com.alexh.capitalism.mixin`) where NeoForge has no event: telling job recipes who's crafting, and blocking Netherite weapons at the Smithing Table for non-Weaponsmiths.

## Running the real server (for friends)
- Put `build/libs/capitalism-0.1.0.jar` in a **NeoForge 26.3.0.10-beta** dedicated server's `mods/` folder.
- A fresh world is best: wild rice only generates in newly explored land.

## For friends to play
1. Install **Prism Launcher** (prismlauncher.org, free).
2. Create an instance: NeoForge, Minecraft **26.3**, NeoForge **26.3.0.10-beta**.
3. Drop `capitalism-0.1.0.jar` into the instance's `mods` folder.
4. Connect to the server IP.

## Notes files
- `Wellness.md`: thirst, water purity, diet, body heat (temperature), wellbeing hearts, the Wellness screen.
- `Jobs.md`: the job list, getting a job, job-only recipes, the server-wide food debuff.
- `Cook.md`: everything about the Cook: kitchen, sushi, Oven meals, plates, rice, Gourmet food, the Cook's Guide.
- `Weaponsmith.md`: the Weaponsmith: Compacted Diamonds, sharp / dull swords, the Whetstone, the Weaponsmith Table, the Weaponsmith's Guide.
- `Bartending.md`: Juicer, Fermenter, Bartending Table, drinks, golden bars.
- `Economy.md`: gold, the Banker, locks, the Shopfront, the leaderboard.
