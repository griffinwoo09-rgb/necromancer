# Sorcerer (Fabric, Minecraft 26.3)

A hooded desert spellcaster with four variants (wind, fire, wither, poison). It casts Evoker-style fangs: a line toward far targets and two rings around close ones. It never summons Vexes. When a target gets close, it casts a ring of its element around itself (wind pushes out, fire burns, wither and poison apply their effects). Sometimes it raises a skeleton army instead: melee skeletons with swords or spears (spear users charge). Each summon has a 35% chance to include one spear + shield skeleton that holds its shield up and lowers it briefly to jab. All of them wear chainmail helmets so they survive daylight; none of their gear drops. Skeletons and other Sorcerers are its allies. It moves and dies with skeleton sounds, but its spells keep the Evoker sounds. It never becomes a patrol captain and never joins raids, so it never drops an ominous banner. Variant names in commands ignore case. If a Sorcerer dies, its fangs still in flight vanish with it. Standing still and calm, it now and then tosses its staff in the air and strikes a pose.

Versions: Minecraft 26.3 · Fabric Loader 0.19.5 or newer · Fabric API 0.166.2+26.3 · Loom 1.19 · Java 25. Mojang names, no mappings.

## Where to tweak things
| What | Where |
|---|---|
| Health, speed, cast timing, fang pattern, aura size/duration, skeleton army chance/size, hover, effect durations, knockback, spawn weight and group size | `src/main/java/com/example/sorcerer/SorcererConfig.java` |
| Shared drops (totem, raw gold, netherite scrap; player kills only) | `src/main/resources/data/sorcerer/loot_table/entities/sorcerer.json` |
| Per-variant drops | `src/main/resources/data/sorcerer/loot_table/entities/sorcerer/<variant>.json` |
| Which biomes it spawns in (default `#c:is_desert`) | `src/main/resources/data/sorcerer/tags/worldgen/biome/spawns_sorcerer.json` |
| Particles, fang tint, cast sound per variant | `src/main/java/com/example/sorcerer/entity/SorcererVariant.java` |
| Placeholder textures | `python3 tools/make_textures.py` |

## Run
```
export JAVA_HOME=/opt/homebrew/opt/openjdk@25
./gradlew build            # compile + jar in build/libs
./gradlew runClient        # play
./gradlew runGameTest      # 20 headless tests: real drop rolls per variant (gold, scrap, no witch loot), army crumbles with its Sorcerer, fang effects, auras (spectators skipped), skeleton army + helmets, no banners or raids, orphaned fangs, spear attacks, gear never drops, case-insensitive variants (typos fall back to random), spawn eggs, loot table, desert-only spawning
./gradlew runClientGameTest  # opens the game, screenshots to build/run/clientGameTest/screenshots
```

## /summon examples
```
/summon sorcerer:sorcerer ~ ~ ~5 {Variant:"wind"}
/summon sorcerer:sorcerer ~ ~ ~5 {Variant:"fire"}
/summon sorcerer:sorcerer ~ ~ ~5 {Variant:"wither"}
/summon sorcerer:sorcerer ~ ~ ~5 {Variant:"poison"}
/summon sorcerer:sorcerer ~ ~ ~5            # random variant
/summon sorcerer:sorcerer_fangs ~ ~ ~2 {Variant:"wind",Warmup:10}   # visual only: fangs with no caster bite as plain magic
```

## Manual test checklist (`./gradlew runClient`, Survival mode, Normal difficulty)
1. Summon each variant with the commands above. Check the robe colour (gray-blue, red-orange, black, green) and the glowing eyes.
2. Stand about 8 blocks away. The Sorcerer stops, rises about 1 block into the air, raises its glowing staff with coloured sparks, and plays its own cast sound. It drops back down when the spell ends. A line of tinted fangs then runs toward you.
3. Walk up close. You get the two-ring pattern instead.
4. Get bitten by each variant. Wind launches you back and up, fire sets you on fire, wither gives Wither, poison gives Poison.
5. Kill some with and without a Looting sword. Every kill drops the variant item: wind 1-3 wind charges, fire 1-2 blaze powder, wither 1-3 bones, poison 1-2 spider eyes (Looting adds up to 1 per level). Player kills can also drop a totem (12%), 1-3 raw gold (25%), 1-2 netherite scrap (8%), and a variant rare (10%): breeze rod, blaze rod, wither rose or fermented spider eye. No witch drops, never an ominous banner.
5b. Walk within 5 blocks of one. It casts a ring of its element (radius 4) that pulses every second for 5 seconds.
5c. Keep fighting it. About 30% of its summon windows (every 15 s) raise 3 sword/spear skeletons, up to 6 nearby. Kill the Sorcerer and its skeletons crumble to soul smoke.
6. Spawn eggs are in the Spawn Eggs creative tab, after the Evoker egg. Each egg spawns its own variant.
7. Desert spawning: `/locate biome minecraft:desert`, go there at night, and give it a few minutes. Plains should never get one.
8. Stand in daylight next to one. It doesn't burn.
