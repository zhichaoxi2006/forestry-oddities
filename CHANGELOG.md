# Changelog

All notable changes to **Forestry Oddities** (林业异闻) are recorded here, by hand, in the format of
[Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/); versions follow
[Semantic Versioning 2.0.0](https://semver.org/spec/v2.0.0.html).

The version a jar reports comes from `mod_version` in `gradle.properties`: it is what the file name
(`forestry_oddities-1.21.1-<version>.jar`) and the mods screen show. A release is therefore that line,
the tag, and the matching section here, in that order.

This is a Minecraft mod, so "API" means blocks, items, registry ids, config keys and the data a world
keeps - those are the things that break a world or a modpack. The names in parentheses are the ones a
player sees in `zh_cn` where they differ from the English; `en_us` and `zh_cn` are the two languages
that carry full texts.

## [Unreleased]

Nothing yet.

## [0.2.0] - 2026-11-12

The manual chapter: everything this mod adds is now described inside Forestry's own book, in game, with
the numbers the code actually uses.

### Added

- **A chapter in Forestry's Forester's Manual** (`forestry:foresters_manual`). The mod still ships no
  book of its own; it contributes a category and six entries to the book Forestry already gives every
  player, at `sortnum` 100, so *Oddities* (林业异闻) closes the manual instead of interrupting its order.

  | Entry | Id | Pages |
  |---|---|---|
  | Soft Light Frame (柔光框架) | `forestry:oddities/soft_light_frame` | spotlight, recipe, the charge, where it fits |
  | Infusion Frame (灌注框架) | `forestry:oddities/infusion_frame` | spotlight, recipe, the five potion effects, the wear |
  | Alveary Frame Housing (蜂房组框架外壳) | `forestry:oddities/alveary_frame_housing` | spotlight, recipe, building it in, what the slot does |
  | Circuit Foundation (电路巢础) | `forestry:oddities/alveary_circuit_foundation` | spotlight, recipe, soldering a board, the tubes that give numbers (two pages), the five that answer questions, the rules |
  | Alveary Transmission (蜂房组配电) | `forestry:oddities/alveary_transmission` | spotlight, recipe, how it distributes |
  | Alveary Ticker (蜂房组进刻器) | `forestry:oddities/alveary_ticker` | spotlight, recipe, the slider, the buffer |

  Twenty-six pages of text, written as the manual writes: the almanac's first person, short paragraphs,
  numbers spelled into the sentence, `$(item)` / `$(thing)` on names, and `$(l:...)` links - sixteen of
  them, into Forestry's apiary, soldering iron and circuit board pages as well as inside this chapter.
  No bullet lists, because Forestry's language files contain no `$(li)` anywhere.

- **The pages are shipped the way Patchouli 1.20 permits**: a book's content is collected from
  `assets/<namespace>/patchouli_books/<book>/<lang>/{categories,entries}/<id>.json` across *every* pack
  in the resource manager, and the namespace of the path is the namespace of the page id. So the files
  live under `assets/forestry/patchouli_books/foresters_manual/en_us/`; Forestry's book loads them next
  to its own pages, no file of Forestry's is replaced, and no second book appears in the item list.

- **One set of page files, two languages.** The book is `"i18n": true`, so every `name` and every page
  text is a translation key: the JSON holds `forestry_oddities.patchouli.*` and the prose lives in this
  mod's `lang/en_us.json` and `lang/zh_cn.json` (34 keys each). Other languages fall back to English
  through the vanilla language manager, so no raw key can reach a page; and because only the `en_us`
  files are enumerated, there is no `zh_cn` copy of the chapter JSON to keep in sync.

- **`tools/check_manual.py`**, a checker for exactly the failures the game does not report: a missing
  key prints the key, a wrong link target stops being a link, a wrong recipe id draws an empty page, and
  a broken JSON or an unclosed `$(/l)` leaves the *whole* manual at "Error loading and compiling book".
  None of that fails a Gradle build. The script re-derives the ids Patchouli derives from the paths and
  checks the layout, the category and all sixteen link targets against Forestry's own book (from a
  sibling `ForestryCE` checkout or a Forestry jar), every `$(...)` command, the 34 keys in both
  languages, the six recipe ids, and the item models of every icon and spotlight item. Colliding with an
  id Forestry already ships counts as an error, because that would replace Forestry's page.

### Changed

- The CurseForge description says the chapter exists, and counts four alveary wall parts rather than
  three (the ticker made it four).
- README: a section on the chapter, the two new files in the layout tree, and the checker in the command
  list. TODO.md records the Patchouli decisions and what was verified.

### Fixed

- Nothing in the mod's code: this release adds assets only.

### Notes

- Verified with `python tools/check_manual.py`: 7 page files, 6 entries in 1 category, 16 links (16
  resolvable), 34 translation keys in each language, 6 recipes, no id taken from Forestry - no problem
  reported. `python tools/check_recipes.py` still finds no shaped-recipe grid collisions (1771 recipes,
  1767 distinct grids). Every new or edited JSON file parses.
- How the pages *look* is not covered: they were not opened in a running client for this entry, and the
  jar was not rebuilt on this machine (the Gradle user home sits outside the checkout), so "the pages are
  in the jar" rests on `src/main/resources` being copied verbatim by `processResources`.

## [0.1.0] - 2026-11-12

The first version: the content and the quality-of-life work this chapter documents. Backfilled from the
history of `v0.1.0` (48 commits, 2026-11-03 to 2026-11-11), grouped by what it changes; this section was
written after the fact and is the only section here that was.

### Added

**Three tweaks to Forestry's own behaviour** (all configurable, all in
`config/forestry_oddities-common.toml`):

- The rain check can be decided from a housing's position and biome rather than from its birth biome
  (`rainMode`: `VANILLA`, `POSITION`, `BIOME`; `biomeModeSkyLight` for the sky-light half). A mixin on the
  apiary / bee house housing and one on the alveary controller share one rule in `tweak/RainChecks`.
- The ignoble (绝代) decay threshold is configurable, where Forestry hard-wired it.
- Beekeeper villagers (村民养蜂) tend apiaries and bee houses, and tend player-owned housings by default.

**Quality of life:**

- Bundle-style right-click stow for every Forestry backpack (背包快速收集), on the backpack's own
  `rightClickItem` path so a full inventory behaves like a bundle.
- A Create integration (optional): twenty-five item attribute types for the Attribute Filter, covering
  bee species, life stage, stock, activity, flower type, effect, both tolerances, speed, lifespan,
  cave dwelling, rain tolerance, analysed and mated flags, and the product a bee makes - each with a
  recessive counterpart, because a recessive allele is what a sorter needs to look at.

**Two apiary frames** (`IHiveFrame`, so Forestry wears them through its own call, once per 550-tick work
cycle):

- **Soft Light Frame (柔光框架)** - sunlight simulation, so a hive in a cellar or a mine works without
  breeding cave-dwelling into the line. 1,000,000 FE buffer, 80 FE per work cycle, receive-only item
  energy storage, and no durability at all: it goes inert, it never breaks. Its modifiers are nothing
  else - speed, territory and lifespan are untouched.
- **Infusion Frame (灌注框架)** - a potion becomes the frame: speed doubles the queen's work speed per
  level, poison ages her ten times as fast, regeneration stops the aging outright, weakness makes
  mutations ten times as likely, water breathing keeps the bees working in the rain; the potion's
  duration (in whole seconds) is the frame's durability, and the window is tinted with the potion's
  colour.

**Four alveary wall parts** (built into the outer shell like Forestry's own fan and sieve):

- **Frame Housing (蜂房组框架外壳)** - the alveary's first frame slot. Any hive frame goes in, the frame
  speaks for the whole alveary, and the slot is saved with the part and reachable by pipes and hoppers.
- **Circuit Foundation (电路巢础)** - a circuit socket fed by FE, ported from Binnie's electrical
  stimulator: fourteen electron-tube programs (production, flowering, territory, lifespan, mutations,
  nether heat, rain, sky, day/night, temperature and humidity, flowers), 1,000 FE buffer, 500 FE per
  offer, receive-only, paid per work cycle as the sum of the whole board, with every program also
  multiplying genetic decay by 1.5. Soldered in its own layout; the board is never worn out.
- **Alveary Transmission (蜂房组配电)** - 1,000 FE buffer that hands its energy to the other parts of the
  same alveary at up to 500 FE a tick, so one cable feeds a wall of circuit foundations. It refuses to
  feed another transmission.
- **Alveary Ticker (蜂房组进刻器)** - the only part that is not a port: it buys extra ticks for the
  housing's work-cycle counter at 10 FE a tick, from x1 up to the housing's current throttle, with a
  screen, a saved setting and a 20,000 FE buffer. An empty queen slot buys nothing.

**Two bees**, with their taxon, species, mutations, flower type and effects:

- **Voltaic Bee (伏打蜂)** - turns a hive's spare work speed into FE each tick's discharge offered
  through the long energy API; the effect and the transfer follow Career Bees' energy bee.
- **Reaper Bee (死神蜂)** - soul reaping (收魂): queens of the other housings in its territory go out one
  by one, never its own; with a death flower type and a phantasmal x demonic mutation under a
  thunderstorm on a wither rose.

**A long energy API** (`api.energy`), ported from Career Bees: `LongEnergyStorage`, `LongEnergySource`,
`LongEnergyProvider`, the `forestry_oddities:long_energy` block capability and `LongEnergy.find`, for
energy systems that carry more than an `int` of FE. Bridges for five mods, each loaded only when that mod
is installed and compiled against its API jar from `libs/compat` (not in the repository):
Modern Industrialization (EU), GrandPower, Flux Networks, Mekanism (Joules) and AppFlux.

**The project itself:** a NeoForge 1.21.1 / ModDevGradle skeleton that compiles against Forestry: CE
3.0.0-alpha9 (`required`, `AFTER`, `runtimeOnly` on the dev runtime, alpha8-compatible in what it uses),
Create as an optional dependency with a guarded integration, Patchouli and JEI declared, mixins in
`forestry_oddities.mixins.json`, `verifyForestryDependency`, `runData` datagen, a banner logo in the mods
screen, `LICENSE` (LGPL-3.0) with the `LICENSE.GPL-3.0` text it rests on (both packaged into
`META-INF/`), `tools/check_recipes.py`, and the README that documents all of it.

### Changed

- **The mod is renamed from Forestry Tweaks to Forestry Oddities** (mod id and namespace
  `forestrytweaks` -> `forestry_oddities`, package `com.zhichaoxi.forestryoddities`, jar
  `forestry-oddities` -> `forestry_oddities`). This breaks existing worlds: blocks and items saved under
  the old namespace are dropped on load.
- **The alveary stimulator is renamed to circuit foundation** (电路巢础), registry ids included. Binnie's
  Mod already calls its own part a stimulator, so two identical names ended up in the same part list and
  in the soldering iron's layout list. The mechanics, the fourteen programs and the textures are
  unchanged; the same release gives the circuit foundation and the transmission recipes of their own.
- The infusion frame's modifiers are *multipliers* scaled from the potion level (neutral at 1.0) rather
  than additions to a base of 1.0, and the tooltips read accordingly.
- The Voltaic Bee's effect is rebalanced, and its alveary backflow removed (it no longer feeds the
  alveary that hosts it).
- Ageing a living bee individual happens in one step, and the territory maths the Reaper uses is shared
  as `BeeTerritory`.
- The bee modifier and effect overrides are annotated `@NotNull`.

### Fixed

- The transmission and the stimulator no longer show a usage tooltip line, and the infusion frame's
  multiplier wording is right in both languages.
- Two stale soft light frame keys are gone from `zh_cn`.
- The alveary transmission has its own block face, the circuit foundation's face and ends were redrawn
  and its model textures renumbered.

### Removed

- The unused zeropoint bee design sketch, and the Corrupted Bee from the shared territory javadoc (that
  bee lives on another branch).

[Unreleased]: https://github.com/zhichaoxi2006/forestry-oddities/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/zhichaoxi2006/forestry-oddities/releases/tag/v0.2.0
[0.1.0]: https://github.com/zhichaoxi2006/forestry-oddities/releases/tag/v0.1.0
