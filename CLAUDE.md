# Nolan's Mod — Fabric mod for Minecraft 26.2

## Stack
- Minecraft 26.2, Fabric Loader 0.19.5, Fabric API 0.160.0+26.2, Loom 1.17 (see `gradle.properties`)
- Java 25 (Homebrew `openjdk@25`; Gradle finds it via `org.gradle.java.home` in `~/.gradle/gradle.properties`)
- Mappings: Mojang official (Loom default for 26.x). Class names match Mojang's, e.g. `net.minecraft.client.Minecraft`, `net.minecraft.resources.Identifier`.
- Mod id `nolanmod`, package `com.nolan.nolanmod`
- Source sets are split: `src/main` (common/server) and `src/client` (client only).

## Layout
- `src/main/java/com/nolan/nolanmod/NolanMod.java` — main entrypoint (`onInitialize`). Register items, blocks, commands, events here.
- `src/client/java/com/nolan/nolanmod/client/NolanModClient.java` — client entrypoint (rendering, keybinds, HUD).
- `src/main/resources/fabric.mod.json` — mod metadata and entrypoints.
- `src/main/resources/assets/nolanmod/` — textures, models, blockstates, `lang/en_us.json`.
- `src/main/resources/data/nolanmod/` — recipes, loot tables, tags (create as needed).
- `*.mixins.json` — mixin configs; `mixin/` packages hold the example mixins (safe to delete/replace).
- `run/` — the dev Minecraft instance (worlds, options, logs). Gitignored.

## Commands
- `./gradlew runClient` — launch a playable Minecraft client with the mod loaded (dev account, no login).
- `./gradlew runServer` — dedicated dev server.
- `./gradlew build` — compile and produce `build/libs/nolanmod-<version>.jar`.
- `./gradlew genSources` — decompile Minecraft for browsing in an IDE.

## Fast iteration loop (the whole point of this project)
Nolan plays while Claude edits. Keep the client from `runClient` open.
- **Resources** (textures, models, blockstates, lang, sounds): edit under `src/main/resources/assets/`, then in-game press **F3+T** to reload. No restart.
- **Data** (recipes, loot tables, tags, worldgen json): edit under `src/main/resources/data/`, then run `/reload` in-game. No restart.
- **Java code**: needs a client restart. Quit the game, run `./gradlew runClient` again (~30 s). The `run/` world persists so you land back where you were.
- Loom's dev run reads resources straight from the build dirs; if a reload does not pick up a change, run `./gradlew processResources` first.

## Conventions
- Tabs for indentation (matches the Fabric template).
- One feature per commit. Run `./gradlew build` before committing.
- New identifiers go through `NolanMod.id("name")`.

## Dev command bridge (run commands in the game from this session)
While the dev client is running, append a line to `run/claude-commands.txt` and the server runs
it as a command within half a second. Output shows in `run/logs/latest.log`.
- Player-targeted commands need `execute as @p at @p run ...` because the source is the server.
- Use `... run say <text>` to get a yes/no answer into the log (e.g. `execute at @p if block ~ ~-1 ~ minecraft:grass_block run say under: grass`).
- Player position: `execute as @p at @p run tp @s ~ ~ ~` logs "Teleported ... to x, y, z".
- Only active in the dev environment (`FabricLoader.isDevelopmentEnvironment()`).

## World types
Superflat worlds skip biome decoration, so worldgen features never run there. Cities handle
this with `FlatWorldCities` (chunk-generate event); any future worldgen feature needs the same.
Ground level in classic superflat is Y=-60.
