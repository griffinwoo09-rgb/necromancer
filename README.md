# Necromancer (Fabric, Minecraft 26.3)

A hooded desert mini-boss (60 health, 6 armor) with five variants (wind, fire, wither, poison, lightning). It raises bone spikes out of the ground, Evoker-style: a line toward far targets and two rings around close ones. At 7-16 blocks it also lobs homing soul skulls (two on Hard), and anyone who gets within 3 blocks gets a wide staff sweep that knocks them back. It never summons Vexes. When a target gets close, it casts a ring of its element around itself (wind pushes out, fire burns, wither and poison apply their effects, lightning calls down a harmless-looking bolt that shocks for extra damage and stuns with Slowness; the bolt is cosmetic, so it never starts fires or converts mobs). Sometimes it raises a skeleton army instead: melee skeletons, mostly with swords and some with spears (spear users charge). Each summon has a 35% chance to include one spear + shield skeleton that holds its shield up and lowers it briefly to jab. Its shield carries a banner in its Necromancer's colours: wind is light blue with a white swirl, fire is red with orange-and-yellow flames, wither is black with a pale skull, poison is green with a lime orb, lightning is black with a yellow bolt. Their helmets vary: about half wear chainmail, some iron, and the rest go bare-headed and burn in daylight; none of their gear drops. After raising an army the Necromancer rests for 5 seconds before casting again, and every spell is followed by a short pause, so its attacks come in waves. Skeletons and other Necromancers are its allies, and summoned skeletons never fight each other, even between armies of different Necromancers. It moves and dies with skeleton sounds, but its spells keep the Evoker sounds. It never becomes a patrol captain and never joins raids, so it never drops an ominous banner. Variant names in commands ignore case. If a Necromancer dies, its fangs still in flight vanish with it. Its poses blend smoothly: it draws its staff back and thrusts it to cast fangs, raises both arms overhead to summon, and plants the staff in the ground for its aura; it breathes and sways while idle and flinches when hit. Standing still and calm, it now and then tosses its staff above its head, spins it and catches it.

Versions: Minecraft 26.3 · Fabric Loader 0.19.5 or newer · Fabric API 0.166.2+26.3 · Loom 1.19 · Java 25. Mojang names, no mappings.

## Where to tweak things
| What | Where |
|---|---|
| Health, armor, speed, cast timing, spike pattern, soul-skull and sweep numbers, aura size/duration, skeleton army chance/size, hover, effect durations, knockback, spawn weight and group size | `src/main/java/com/example/necromancer/NecromancerConfig.java` |
| Shared drops (totem, raw gold, netherite scrap; player kills only) | `src/main/resources/data/necromancer/loot_table/entities/necromancer.json` |
| Per-variant drops | `src/main/resources/data/necromancer/loot_table/entities/necromancer/<variant>.json` |
| Which biomes it spawns in (default `#c:is_desert`) | `src/main/resources/data/necromancer/tags/worldgen/biome/spawns_necromancer.json` |
| Particles, fang tint, cast sound per variant | `src/main/java/com/example/necromancer/entity/NecromancerVariant.java` |
| Placeholder textures | `python3 tools/make_textures.py` (Windows: `python tools\make_textures.py`) |

## Run
Needs Java 25. Windows (PowerShell):
```
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.5.10002-hotspot"   # wherever your JDK 25 is
.\gradlew build
```
macOS / Linux:
```
export JAVA_HOME=/opt/homebrew/opt/openjdk@25   # wherever your JDK 25 is
./gradlew build            # compile + jar in build/libs
./gradlew runClient        # play
./gradlew runGameTest      # 28 headless tests: mixed army gear, armies never infight, mini-boss stats, homing soul skull, sweep arc, lightning shock (no real bolt), variant banner shields, real drop rolls per variant (gold, scrap, no witch loot), army crumbles with its Necromancer (on death and when it disappears), fang effects, auras (spectators skipped), skeleton army + helmets, no banners or raids, orphaned fangs, spear attacks, gear never drops, case-insensitive variants (typos fall back to random), spawn eggs, loot table, desert-only spawning
./gradlew runClientGameTest  # opens the game, screenshots to build/run/clientGameTest/screenshots
```

## /summon examples
```
/summon necromancer:necromancer ~ ~ ~5 {Variant:"wind"}
/summon necromancer:necromancer ~ ~ ~5 {Variant:"fire"}
/summon necromancer:necromancer ~ ~ ~5 {Variant:"wither"}
/summon necromancer:necromancer ~ ~ ~5 {Variant:"poison"}
/summon necromancer:necromancer ~ ~ ~5 {Variant:"lightning"}
/summon necromancer:necromancer ~ ~ ~5            # random variant
/summon necromancer:necromancer_fangs ~ ~ ~2 {Variant:"wind",Warmup:10}   # visual only: fangs with no caster bite as plain magic
```

## Manual test checklist (`./gradlew runClient`, Survival mode, Normal difficulty)
1. Summon each variant with the commands above. Check the robe colour (gray-blue, red-orange, black, green, gold) and the glowing eyes.
2. Stand about 8 blocks away. The Necromancer stops, rises about 1 block into the air, raises its glowing staff with coloured sparks, and plays its own cast sound. It drops back down when the spell ends. A line of tinted fangs then runs toward you.
3. Walk up close. You get the two-ring pattern instead.
4. Get bitten by each variant. Wind launches you back and up, fire sets you on fire, wither gives Wither, poison gives Poison, lightning strikes a bolt (no fire), deals extra damage and slows you right down.
5. Kill some with and without a Looting sword. Every kill drops 1-3 emeralds and the variant item: wind 1-3 wind charges, fire 1-2 blaze powder, wither 1-3 bones, poison 1-2 spider eyes, lightning 1-3 glowstone dust (Looting adds up to 1 per level). Player kills can also drop: 1-3 bottles o' enchanting (30%), 1-3 raw gold (25%), a totem (12%), 1-2 netherite scrap (8%), an enchanted book (6%), a golden apple (4%), a diamond (3%), the variant rare (breeze rod, blaze rod, wither rose or fermented spider eye 10%, trident 2% from lightning). No witch drops, never an ominous banner.
5b. Walk within 5 blocks of one. It casts a ring of its element (radius 4) that pulses every second for 5 seconds.
5c. Keep fighting it. About 30% of its summon windows (every 15 s) raise 3 sword/spear skeletons, up to 6 nearby. Kill the Necromancer and its skeletons crumble to soul smoke.
6. Spawn eggs are in the Spawn Eggs creative tab, after the Evoker egg. Each egg spawns its own variant.
7. Desert spawning: `/locate biome minecraft:desert`, go there at night, and give it a few minutes. Plains should never get one.
8. Stand in daylight next to one. It doesn't burn.
