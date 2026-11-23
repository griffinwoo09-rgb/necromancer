# Changelog

All notable changes to this mod. Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

Requirements for every release: Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API, Java 25.

## [1.2.0] - 2026-11-22 — The Storm update

A fifth Necromancer arrives, every pose got reworked, and the staff no longer cuts through the body.

### ⚠️ Upgrade notes
- No breaking changes. Existing Necromancers keep their variant, and the Lightning Necromancer joins the random desert spawns (each variant is now 1 in 5).
- Replace `necromancer-1.1.0-fabric-0.19.5.jar` with `necromancer-1.2.0-fabric-0.19.5.jar`.

### Added
- **Lightning Necromancer** (gold robe, electric-yellow glow, spark particles, trident-thunder cast sound). Its fangs and aura:
  - call down a **cosmetic** lightning bolt on the target: thunder and flash only, no fire, and no creeper, pig or villager conversions. At most one bolt per target every half second.
  - **shock** for 3 extra damage.
  - **stun** with Slowness III for 1.5 seconds.
- **Lightning drops:**
  - Every kill: 1–3 glowstone dust (+Looting).
  - Player kills: 2% trident (2.5% +0.5% per Looting level).
- **Lightning extras:** a Lightning Necromancer Spawn Egg, and a banner shield for its skeletons (yellow bolt slashing across black, in a yellow frame).
- **A distinct pose for each spell:**
  - Fangs: draws the staff back, then thrusts it at the target with the orb tipped forward.
  - Summon: both arms flung overhead with the staff held high, skull thrown back.
  - Aura: plants the staff in the ground in front, bows its head, spreads its free hand.
- **Idle life:**
  - Slow breathing and skull sway.
  - Cape and robe ripple out of step across a group.
  - Legs dangle while hovering.
  - A flinch when hit.
- New automated test for the lightning shock: damage and Slowness land, and the pig isn't set on fire, converted, or near any fire (22 tests total). The screenshot test now covers all 5 variants, every banner shield, each cast pose from both sides, and a lightning strike.

### Changed
- **Smooth animation blending.** Casting poses ease in and out over about a quarter second instead of snapping.
- **New staff:**
  - Thicker 2×2 ebony shaft with grain, a leather-wrapped grip and silver ferrule.
  - Silver collar under the orb.
  - The glowing halo is now a hollow ring.
  - The grip sits lower on the shaft, so less of it hangs below the hand.
- **Staff toss idle:** the staff is thrown above the head and spins about its middle before being caught.
- Docs screenshots retaken.

### Fixed
- **Staff clipping through the body.** Four causes:
  - It was held right against the robe and swung with the arm, so its foot cut into the robe and legs while walking. It's now held further out, tilted slightly away, and kept upright as the arm swings, like a walking stick.
  - The cast pose could push the orb into the hood.
  - The idle toss spun the staff around the hand, which swept it through the skull and crown.
  - The idle toss also lowered the staff when it was meant to lift it.

## [1.1.0] - 2026-11-22 — The Necromancer update

The Sorcerer is now the **Necromancer**, and its shield-bearing skeletons fight under their master's colours.

### ⚠️ Upgrade notes
- **This is a breaking rename.** The mod ID changed from `sorcerer` to `necromancer`. Any Sorcerers, Sorcerer spawn eggs and summoned skeletons already in a world will be removed when you update. New Necromancers spawn normally in deserts.
- Remove the old `sorcerer-*.jar` from your `mods` folder before adding `necromancer-1.1.0-fabric-0.19.5.jar`. Having both installed gives you two separate mobs.
- Commands, datapacks and loot table overrides need the new IDs (see "Changed").

### Added
- **Banner shields for shield skeletons.** A Necromancer's spear-and-shield skeleton now carries a shield painted to match its master's variant:
  - Wind: light blue, white swirl (Flow pattern), white border
  - Fire: red, orange gradient rising into yellow flames
  - Wither: black, pale gray skull, gray border
  - Poison: green, lime orb, lime curly border
- The shields still never drop.
- New automated test checking each variant's shield colour and patterns (21 tests total).

### Changed
- **Renamed Sorcerer → Necromancer everywhere:** in-game names, spawn eggs, README, code, files and the jar name.
  - Entities: `necromancer:necromancer`, `necromancer:necromancer_fangs`, `necromancer:necromancer_skeleton`
  - Spawn eggs: `necromancer:wind_necromancer_spawn_egg`, `fire_…`, `wither_…`, `poison_…`
  - Loot tables: `necromancer:entities/necromancer` (shared) and `necromancer:entities/necromancer/<variant>`
  - Spawn biome tag: `necromancer:spawns_necromancer`
  - Example: `/summon necromancer:necromancer ~ ~ ~5 {Variant:"fire"}`
- **New skull face.** The nose now runs into a dark mouth slit, with four square teeth along the chin, on all four variants. This replaces the 1.0.1 face, which still looked off.
- Screenshots in `docs/` retaken with the new names and face.

## [1.0.1] - 2026-11-22 — Loot and polish

### ⚠️ Upgrade notes
- Witch-style drops are gone (see "Removed"). Farms built around them will stop producing.
- Raw gold, netherite scrap and the totem now drop **only when a player gets the kill**.

### Added
- **Variant drops.** Every kill drops the variant's item:
  - Wind: 1–3 wind charges
  - Fire: 1–2 blaze powder
  - Wither: 1–3 bones
  - Poison: 1–2 spider eyes
- **Variant rare drops** (player kills, 10%): breeze rod (wind), blaze rod (fire), wither rose (wither), fermented spider eye (poison).
- **Skeleton armies crumble with their master.** When a Sorcerer dies, the skeletons it summoned fall apart in soul smoke and drop nothing. If the Sorcerer disappears any other way (despawned, /kill'd, unloaded), they crumble after about 5 seconds (`ORPHAN_CRUMBLE_SECONDS` in the config). Skeletons that weren't summoned are unaffected.
- New automated tests that kill hundreds of Sorcerers and check what actually lands on the ground: each variant's item drops, raw gold and scrap drop from player kills, no witch items, and no rare drops from non-player kills. Also a test that the army crumbles when its Sorcerer dies (20 tests total).

### Changed
- **Raw gold and netherite scrap drop much more often.** They were rare enough to look broken:
  - Raw gold: 8% → **25%**, 1–3 per drop (was 1–4, mostly 1)
  - Netherite scrap: 3% → **8%**, 1–2 per drop
  - Totem of Undying: stays at 12%
  - Looting raises all three chances, and adds raw gold.
- **Skull face redrawn.** Removed the stray pixels that made the mouth look like a lopsided smirk.
- Fabric Loader requirement lowered to 0.19.5 in the source too, so builds from source run on 0.19.5.
- README updated: correct drop list, skeleton armies are sword/spear (not bow), new test count.

### Removed
- **All witch drops:** glowstone dust, sugar, spider eyes (the general pool; poison keeps its own), glass bottles, gunpowder, sticks and redstone.

### Fixed
- Raw gold and netherite scrap seemed never to drop: the chances were too low (3% scrap) to show up in normal play. They're now raised, and the new tests prove they drop.

## [1.0.0-fabric-0.19.5] - 2026-11-22 — Fabric Loader 0.19.5 support

### Fixed
- The mod refused to load on Fabric Loader 0.19.5 ("requires version 0.20.1 or later of mod 'Fabric Loader'"). The minimum is now 0.19.5. Nothing else changed from 1.0.0.

[1.2.0]: ../../releases/tag/v1.2.0
[1.1.0]: ../../releases/tag/v1.1.0
[1.0.1]: ../../releases/tag/v1.0.1
[1.0.0-fabric-0.19.5]: ../../releases/tag/v1.0.0-fabric-0.19.5
