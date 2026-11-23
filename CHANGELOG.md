# Changelog

All notable changes to this mod. Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

Requirements for every release: Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API, Java 25.

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

