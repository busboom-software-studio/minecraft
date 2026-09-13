# Nolan's Mod

A Fabric mod for Minecraft 26.2.

## Play it

```bash
./gradlew runClient
```

That launches Minecraft with the mod loaded. No launcher or login needed. The
world and settings live in `run/` and persist between launches.

## Requirements

- Java 25 (`brew install openjdk@25`)
- Everything else is downloaded by the Gradle wrapper on first run.

## Iterating while playing

| Change | How to see it in-game |
| --- | --- |
| Textures, models, lang files (`src/main/resources/assets/`) | Press **F3+T** |
| Recipes, loot tables, tags (`src/main/resources/data/`) | Run `/reload` |
| Java code | Quit and run `./gradlew runClient` again |

See [CLAUDE.md](CLAUDE.md) for the project layout and conventions.
