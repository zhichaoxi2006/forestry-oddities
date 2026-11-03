# Forestry Tweaks

A Minecraft **1.21.1 / NeoForge** mod development environment for add-ons and tweaks to
[Forestry: Community Edition](https://github.com/thedarkcolour/ForestryCE) (ForestryCE).

The project is a working, buildable skeleton: it compiles against the Forestry API, declares
Forestry as a required mod dependency, and puts Forestry on the development runtime so `runClient` /
`runServer` load it. [Create](https://github.com/Creators-of-Create/Create) (机械动力) is wired in as
an **optional** dependency for the attribute filter integration.

## Versions

| Component | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.230 |
| ModDevGradle | 2.0.140 |
| Gradle (wrapper) | 9.2.1 |
| Java | 21 (toolchain, required by 1.21.1) |
| Forestry: CE | 3.0.0-alpha9 (`thedarkcolour.forestry:forestry-1.21.1`) |
| Patchouli (required by Forestry) | 1.21.1-93-NEOFORGE |
| JEI (optional) | 19.27.0.340 |
| Create (optional) | 6.0.4-55 (`com.simibubi.create:create-1.21.1`) |
| Ponder (Create's library, bundles Catnip) | 1.0.46 |
| Flywheel (Create's renderer) | 1.0.4 (API 1.0.3) |
| Registrate (Create's registry library) | MC1.21-1.3.0+62 |

All of these are set in `gradle.properties`; change them there rather than in `build.gradle`.

## The Forestry dependency

Forestry 1.21.1 releases are **not** published to a public Maven repository, so the dependency is
resolved from this machine's **Maven Local**. The `3.0.0-alpha9` entries there are the released
Modrinth/CurseForge jars, copied into the Maven layout by hand, one directory per artifact:

```
~/.m2/repository/thedarkcolour/forestry/<artifact>/3.0.0-alpha9/
    <artifact>-3.0.0-alpha9.jar     forestry-1.21.1-3.0.0-alpha9.jar
                                    forestrymail-1.21.1-3.0.0-alpha9.jar        (optional)
                                    forestrybutterflies-1.21.1-3.0.0-alpha9.jar (optional)
    <artifact>-3.0.0-alpha9.pom     a minimal POM: groupId thedarkcolour.forestry, packaging jar
```

Older releases were produced with `cd ../ForestryCE && .\gradlew.bat publishToMavenLocal`. That no
longer works for alpha9 on Java 21: its build applies `net.darkhax.curseforgegradle` 1.3.33, which
requires a Java 25 daemon. The released jars are the same artifacts, so nothing in this mod changes.
There is no alpha9 `forestryfarms` jar yet, which is why `farms` is missing from
`forestry_optional_modules`.

How it is wired up:

* `build.gradle` — `compileOnly` (compile against the API, never bundle Forestry into the jar) and
  `runtimeOnly` (put it on the development runtime). The optional modules `mail,butterflies`
  are `runtimeOnly` too, so your mod keeps working for players who do not install them.
* `META-INF/neoforge.mods.toml` — declares `forestry` as `type = "required"`,
  `ordering = "AFTER"`. This is the in-game half of the dependency: NeoForge refuses to load this
  mod without Forestry. `patchouli` is required because Forestry itself requires it; `jei` is
  optional and client-side.
* `ForestryTweaksPlugin` — implements `forestry.api.plugin.IForestryPlugin` and is registered via
  `META-INF/services/forestry.api.plugin.IForestryPlugin`. That is the compile-time proof that the
  dependency works, and the entry point for registering Forestry content/tweaks.

Run `.\gradlew.bat verifyForestryDependency` to print exactly which Forestry jars the dev runtime
will load (it fails with a clear message if they are missing).

## The Create dependency (optional)

[Create](https://github.com/Creators-of-Create/Create) (机械动力) is an **optional** dependency: it is
only needed for the Create integration below, and nothing in the mod touches a Create class unless
Create is installed.

How it is wired up:

* `build.gradle` — `compileOnly` + `runtimeOnly`, exactly like Forestry: compile against Create, put
  it on the development runtime, but never bundle it. The artifact is Create's `slim` jar (the one
  without its libraries) with `transitive = false`, matching Create's own dev setup, so the libraries
  are declared explicitly:
  * `net.createmod.ponder:Ponder-NeoForge-1.21.1` — Ponder also carries Catnip (Create's GUI/util
    library). `transitive = false` because its POM points at `Ponder-Common` and
    `ForgeConfigAPIPort`, which the published jar does not reference.
  * `dev.engine-room.flywheel:flywheel-neoforge-api-1.21.1` (compileOnly) and
    `flywheel-neoforge-1.21.1` (runtimeOnly) — Flywheel's API stub lags the implementation by one
    patch in this release train (`1.0.3` vs `1.0.4`).
  * `com.tterrag.registrate:Registrate` — Create's registry library.
  * The repositories are `https://maven.createmod.net` (Create, Ponder, Flywheel) and
    `https://mvn.devos.one/snapshots` (Registrate), both without content filters so the artifacts are
    the same ones Create publishes (which also keeps `--offline` working against the Gradle cache).
* `META-INF/neoforge.mods.toml` — declares `create` as `type = "optional"` with
  `versionRange = "[6.0.4,7)"` and `ordering = "AFTER"`: 6.0.4 is the first Create with the attribute
  filter API this integration uses, and 7 would be a new, unchecked major version.
* `ForestryTweaks` — checks `ModList.get().isLoaded("create")` and only then calls
  `CreateIntegration.init(modBus)`. That guarded call is the only place the mod names a Create class,
  so the JVM never loads `integration/create/*` when Create is missing.
  `skip_create_runtime=true` (in `gradle.properties`, or `-Pskip_create_runtime=true`) keeps Create and
  its libraries off the development runtime, which is how the "Create is not installed" path is
  tested: `runServer` then logs `Create is not installed; the item attribute filter integration stays
  disabled` and starts normally.
* `CreateIntegration` — registers the item attribute types. Create keeps its attribute types in a
  NeoForge registry (`CreateBuiltInRegistries.ITEM_ATTRIBUTE_TYPE`, created from
  `CreateRegistries.ITEM_ATTRIBUTE_TYPE`), so `RegisterEvent` is enough; the call is a no-op for every
  other registry. Create dispatches on the registered **type instance itself**, which is why
  `BeeItemAttributes` stores the instances that were handed to the registry — an attribute must report
  exactly that instance from `getType()`.

### Attribute filter support for bees

Create's **Attribute Filter** (属性过滤器) matches items by properties rather than by item identity,
which is what a bee sorter needs: every bee species shares the four bee items
(`forestry:drone_bee`, `forestry:princess_bee`, `forestry:queen_bee`, `forestry:larvae_bee`) and
differs only in the genome data component. This mod registers these attribute types:

| Registry id | What the GUI shows | Matches |
|---|---|---|
| `forestrytweaks:bee_species` | "is of species Imperial" | bees whose active species is that species (any of the four life stages) |
| `forestrytweaks:bee_species_recessive` | "carries Forest recessively" | bees whose recessive species allele is that species, i.e. what their offspring can turn into |
| `forestrytweaks:bee_life_stage` | "is a Queen" | drone, princess, queen or larvae |
| `forestrytweaks:bee_stock` | "is pristine stock" / "is ignoble stock" | whether the bee descends from naturally spawned (pristine) or bred (ignoble) stock |
| `forestrytweaks:bee_activity` | "has Diurnal activity" | the bee's activity allele: diurnal, nocturnal, crepuscular, metaturnal, ... |
| `forestrytweaks:bee_activity_recessive` | "has Diurnal activity recessively" | the activity allele the bee carries recessively |
| `forestrytweaks:bee_flower_type` | "has flower type Cacti" | the flowers the bee needs (Flowers, Cacti, Jungle, Nether, ...) |
| `forestrytweaks:bee_flower_type_recessive` | "has flower type Cacti recessively" | the flower type allele the bee carries recessively |
| `forestrytweaks:bee_effect` | "has the Creeper effect" | the bee's effect allele (Creeper, Radioactive, Flammable, ...) |
| `forestrytweaks:bee_effect_recessive` | "has the Creeper effect recessively" | the effect allele the bee carries recessively |
| `forestrytweaks:bee_temperature_tolerance` | "has Up 3 temperature tolerance" | how far the bee's ideal temperature may deviate |
| `forestrytweaks:bee_temperature_tolerance_recessive` | "has Up 3 temperature tolerance recessively" | the temperature tolerance allele the bee carries recessively |
| `forestrytweaks:bee_humidity_tolerance` | "has Both 2 humidity tolerance" | how far the bee's ideal humidity may deviate |
| `forestrytweaks:bee_humidity_tolerance_recessive` | "has Both 2 humidity tolerance recessively" | the humidity tolerance allele the bee carries recessively |
| `forestrytweaks:bee_speed` | "has Fastest speed" | the production speed allele |
| `forestrytweaks:bee_speed_recessive` | "has Fastest speed recessively" | the speed allele the bee carries recessively |
| `forestrytweaks:bee_lifespan` | "has a Longest lifespan" | the queen's lifespan allele |
| `forestrytweaks:bee_lifespan_recessive` | "has a Longest lifespan recessively" | the lifespan allele the bee carries recessively |
| `forestrytweaks:bee_cave_dwelling` | "can work underground" | bees with the `CAVE_DWELLING` gene |
| `forestrytweaks:bee_cave_dwelling_recessive` | "carries the underground gene recessively" | bees whose recessive `CAVE_DWELLING` allele is set |
| `forestrytweaks:bee_tolerates_rain` | "works in the rain" | bees with the `TOLERATES_RAIN` gene |
| `forestrytweaks:bee_tolerates_rain_recessive` | "carries the rain-tolerance gene recessively" | bees whose recessive `TOLERATES_RAIN` allele is set |
| `forestrytweaks:bee_analyzed` | "is analyzed" | bee items that have been scanned by an analyzer |
| `forestrytweaks:bee_mated` | "is mated" | princesses and queens that already carry a mate genome |
| `forestrytweaks:bee_product` | "produces Honey Comb" / "has the specialty X" | items in the species' product list, and its jubilant-state specialties |

Usage: right-click an Attribute Filter, drop a bee into the reference slot on the left, pick one of
the bee attributes from the dropdown and add it (or its inverted variant). The filter then works in
any Create filter slot — brass and andesite funnels, smart chutes, brass tunnels, deployers, and so
on. For example:

* "is of species Imperial" in a brass funnel sorts an apiary's output by species.
* "is a Queen" lets a funnel accept queens of *all* species while rejecting drones and princesses.
* "is ignoble stock" on a funnel into a lava-filled chute gets rid of ignoble lines while keeping the
  pristine ones, which is the practical counterpart of the `ignobleDecayStartGeneration` tweak below.
* "has Nocturnal activity" + "is a Queen" sends the night shift to a separate housing, so a daytime
  player never idles them.
* "has the Creeper effect" or "has Explosive/Radioactive ..." keeps effect bees out of the base.
* "can work underground" and "works in the rain" match bees to underground or open-air housings
  without reading their tooltips.
* "carries Forest recessively" on an apiary's output finds the princesses that look imperial but
  still throw forest offspring, so a line can be cleaned up before it mutates back.
* "is not analyzed" routes fresh bees to the analyzer before they are filed.
* "produces X" / "has the specialty X" groups a species' production into dedicated output chests.

Notes on the semantics:

* Every attribute is read from the item stack: species, stock, the bee's state and the genes all come
  from the genome data component, and the product list comes from the species. The client therefore
  offers exactly what the server matches on, and unanalyzed bees work (analysis only affects the
  tooltip). A bee item without a genome — a villager trade wildcard bee — resolves to the default
  species, the same way Forestry's own item code does.
* Gene values are stored as the same values the Portable Analyzer shows (the allele id for activity,
  flower type and effect; the value itself for the tolerances, speed and lifespan), so the names in
  the filter are Forestry's own allele names (`chromosome.translationKey`), falling back to the raw
  value for a modded allele without a translation.
* The `_recessive` variants read the bee's *inactive* allele: the second value the Portable Analyzer
  shows, which the bee carries without expressing it. That is the allele a queen can pass on to its
  offspring, so these attributes answer "what can this bee still turn into" instead of "what is it".
* A homozygous bee carries the same allele on both slots, so it matches "has X" *and* "carries X
  recessively". The recessive attributes are therefore not a "hidden alleles only" filter — they
  match every bee that has the allele, expressed or not. Combine one with its inverted sibling when a
  filter must exclude a value on both alleles.
* The two `_recessive` boolean attributes are offered by Create's singleton mechanism, so the GUI
  only lists them for bees that actually carry the gene, exactly like "is placeable" only shows up
  for placeable items.
* Filters survive restarts, and a filter pointing at a species/allele from an uninstalled add-on
  shows the raw id instead of breaking.
* Inverted attributes follow Create's rule (the negation of the predicate, so they also match
  non-bee items). Combine them with "was added by Forestry" in *whitelist (AND)* mode if a filter must
  only ever accept bees.
* The strings live in `assets/forestrytweaks/lang/{en_us,zh_cn}.json`; Create renders an attribute as
  `create.item_attributes.<key>` and appends `.inverted` for the inverted variant, and the stage names
  come from `forestrytweaks.item_attributes.bee_stage.*`.

Verification: `.\gradlew.bat runServer` logs
`Registered Create attribute filter types for bees: [...]` (all twenty-five ids) while Create's
registries are being filled. During development the integration was additionally exercised on a
running server: 820 assertions covering attribute discovery from real bee stacks, NBT and network
round-trips of every codec (including Create's unit-attribute codec), `FilterItemStack#test` matching
and rejecting crafted bees per gene (activity, effect, flower type, both tolerances, speed, lifespan,
cave dwelling, rain, analyzed, mated, product), inverted species filters, and that saplings, pollen
and butterflies are never matched. The recessive attributes were verified by a second probe with 227
assertions that craft heterozygous genomes (imperial expressing, forest recessive) per chromosome and
check that the expressed attribute matches the expressed allele and never the recessive one, that the
recessive attribute matches only the carried allele, that a homozygous bee matches both, and that each
attribute still applies after an NBT and a network round trip.

## Tweaks

### Rain check based on position and biome

Forestry blocks bee work whenever `Level#isRaining()` is true and the sky light above the housing is
above 7 (`AbstractBeeHousingBlockEntity#isRaining`, `AlvearyController#isRaining`). `Level#isRaining()`
reports the weather of the **whole dimension**, so an apiary, bee house or alveary counts as "raining"
even when it is roofed over, built underground or placed in a biome where it never rains (desert,
savanna, badlands, ...). Bees without the `TOLERATES_RAIN` gene then refuse to work there.

#### Where that rule came from

It is deliberate, not an oversight. Commit `6fadbcf5e` ("More Bee Tweaks (#291)", EnderiumSmith,
2026-02-02, Forestry 2.10.0) moved both housing classes off `Level#isRainingAt` and onto the
global-weather plus sky-light test. The changelog entry for that PR is about the `NO_SKY` side:

> Relaxed skylight requirements so that bees don't have to see the sky directly above them, now only
> requiring hives to be in a skylight of 10 or greater.

Before that PR `canBlockSeeTheSky()` was `Level#canSeeSky` (sky light exactly 15); it became "sky
light >= 10". `isRainingAt` demands the full 15, nothing in the motion-blocking height map and a
rain-producing biome, so it would have contradicted the new, looser notion of "exposed to the sky" —
hence the rewrite. Wild hives were left alone (`HiveBlockEntity#isRaining` still calls `isRainingAt`),
so the relaxed rule only ever applied to player-built housing.

Before the 1.14 port, every housing used the position-based test: `World#isRainingAt` for apiaries and
bee houses (`TileBeeHousingBase`), alvearies (`AlvearyController`, top centre plus two), wild hives
(`TileHive`) and even the apiarist's chest minecart (`EntityMinecartBeeHousingBase`). In 1.12.2 the
`NO_SKY` side used `World#canBlockSeeSky` at the same position, and the rain tank (`TileRaintank`) used
exactly that pair as well. So 1.12.2 behaviour is closest to the `POSITION` mode; PR #291 replaced it
with the global heuristic.

This mod makes `isRaining()` look at the housing's position instead of the whole dimension. Two mixins
do the work; both keep the sample position Forestry already uses:

| Target | Sample position (unchanged from Forestry) |
|---|---|
| `AbstractBeeHousingBlockEntity#isRaining` (apiary, bee house) | the block above the housing |
| `AlvearyController#isRaining` (alveary) | top centre of the multiblock, two blocks up |

`rainMode` selects the rule (`com.forestrytweaks.tweak.RainChecks`):

| Mode | Rule | Roofed | Desert / snow | Cave |
|---|---|---|---|---|
| `BIOME` (default) | `isRaining()` **and** the biome's precipitation is rain **and** sky light >= `biomeModeSkyLight` | raining | dry | dry |
| `POSITION` | `Level#isRainingAt(pos)` — what wild beehives (`HiveBlockEntity`) use | dry | dry | dry |
| `VANILLA` | upstream: `isRaining()` and sky light > 7 | raining | raining | dry |

`BIOME` is the default because it keeps PR #291's intent — a housing under a canopy or a glass roof
still counts as rained on — while removing the part that was plainly wrong: rain in a biome where it
does not rain, and snowfall. `POSITION` is the strict "rain has to really fall here" reading; note that
it makes *any* block with a collision box above the housing (glass, slab, leaves, ...) a permanent
rain shield, which makes `TOLERATES_RAIN` pointless for roofed apiaries.

Consequences of the default:

* A housing in a biome without precipitation (desert, savanna, badlands, ...) or in a snowy biome is
  no longer "raining", so bees there do not need `TOLERATES_RAIN` while it pours elsewhere.
* A cave or a fully enclosed housing is no longer "raining" either. The separate `NO_SKY` check is
  untouched: a housing that cannot see the sky still needs `CAVE_DWELLING` or a frame that simulates
  sunlight.
* A roofed or shaded housing is still "raining", exactly as in Forestry — the sky light threshold
  (`biomeModeSkyLight`, default 10) matches the one `canBlockSeeTheSky()` uses for `NO_SKY`.
* When rain counts, the original rule still applies: bees need `TOLERATES_RAIN` (or a sealed housing
  such as one with a creative frame), and the apiary GUI still shows the "is raining" error.

Configuration (`config/forestrytweaks-common.toml`):

```toml
[apiculture]
    # BIOME (default), POSITION or VANILLA.
    rainMode = "BIOME"
    # Only used by BIOME: minimum sky light at the sampled position. 0-15.
    biomeModeSkyLight = 10
```

Values are re-read on every check, so an edit applies without a restart (each side reads its own copy:
the server decides whether bees work, the client only changes the GUI error it displays).

### Ignoble bee decay (绝代)

A princess taken from a wild hive can be **ignoble** ("卑贱") stock, and an ignoble queen never
becomes pristine again. Forestry eventually kills such a line: once the generation counter passes a
threshold, a queen can die out instead of leaving a princess, so the housing is left with drones only.

The test is `Bee#checkIgnobleDecay`, called from `Bee#spawnPrincess`:

```java
return (generation > 96 + rand.nextInt(6) + rand.nextInt(6)) && (rand.nextFloat() < 0.02f * modifier);
```

Consequences of that formula:

* Decay can start anywhere from generation **97 to 107** (96 plus a random 0-10).
* The chance is 2% per princess, scaled by the housing's `modifier` (`IBeeModifier#modifyGeneticDecay`).
  A bee house sets it to 0, so ignoble queens never die out there; frames can raise it.
* `BeeHousingBeekeepingLogic#spawnOffspring` rolls it once per princess, twice when
  `ForestryConfig.SERVER.hiveSecondPrincessChance` fires.
* Pristine bees are unaffected: the whole block is skipped for them.

`ignobleDecayStartGeneration` replaces the hard-coded `96`. The 0-10 generation jitter and the 2%
chance stay exactly as Forestry defines them, so the setting moves the *base* of the range, not a
single exact generation:

| Value | Effect |
|---|---|
| `96` (default) | Forestry's behaviour; the mixin returns `null` and the original code runs untouched |
| `N` | decay can start at generation `N+1` to `N+11` |
| `0` | decay can start right away (generations 1-11) |
| `-1` | decay disabled: an ignoble queen never dies out |

Configuration (`config/forestrytweaks-common.toml`):

```toml
[apiculture]
    # Base generation of Forestry's ignoble decay test. 96 = upstream behaviour, -1 = never die out.
    ignobleDecayStartGeneration = 96
```

The check runs in `BeeHousingBeekeepingLogic` (server side only, next to the other bee logic), and the
generation stored on a bee is not touched, so the setting also applies to queens that already have a
generation counter.

### Beekeeper villagers (村民养蜂)

Forestry's beekeeper villager (profession `forestry:beekeeper`, working at an escritoire) has an AI
that walks to an apiary or a bee house and tends it: `forestry.apiculture.apiarist.ApiaristAI`, added
by PR #2001 ("Add AI for Apiarist to Replace Bees in Village Bee Houses"). It puts a princess and a
drone into the housing and takes the bees the housing produced into its own inventory, which is what
keeps the apiaries of a village apiary house running.

That AI is the wrong shape for the job. This tweak replaces it: `apiculture.BeekeeperEvents` attaches
`apiculture.BeekeeperGoal` to every villager, and `mixin.EventHandlerApicultureMixin` cancels the goal
Forestry would have added.

| Forestry: CE 3.0.0-alpha9 | What this mod does instead |
|---|---|
| The housing is assumed to be `north().above()` of the block the villager walks to, so it is only found when the villager happens to stand on its south side, one block above the ground. | The four blocks next to the villager's feet are checked, so any side works. |
| The tile is cast to `BeeHouseBlockEntity`, so an apiary is never a target at all. | Any `IBeeHousing` whose inventory is a `BeeHousingInventory` is tended, so apiaries and bee houses both are. |
| A housing is only a target when its inventory is *not* empty, so the empty apiary that needs stocking is the one that is ignored. | An empty housing is the normal case: a princess and a drone are installed. |
| Whichever princess and drone come first are installed, even of different species, which produces a hybrid queen. | The pair has to match by species (`requireSameSpecies`), and the housing's own product bees are preferred over the villager's. |
| The villager tends every tick until the parent goal's give-up timer expires (minutes), and it also tends at night or during a village meeting. | Validity is "there is work", so the visitor leaves as soon as the work is done; and the goal requires the beekeeper profession, an awake villager and the `WORK`/`IDLE` activity. |
| Any housing within 16 blocks is tended, the player's own included. | A housing placed by a player is left alone (`tendPlayerOwned`). |

A visit (`apiculture.BeekeeperTending`), in order: install a princess that matches a drone already
waiting in the drone slot; otherwise install a princess and a drone of the same species; otherwise
install a drone for a princess that is waiting without one; never touch a living queen; finally collect
the bee items from the product slots into the villager's inventory - and only while the bag has room,
so a full villager does not keep walking to apiaries it cannot help. The alveary is deliberately not a
target: its inventory belongs to a multiblock controller a villager cannot reach.

Configuration (`config/forestrytweaks-common.toml`):

```toml
[beekeeper]
    # false = Forestry's own AI stays in charge.
    enabled = true
    # true lets beekeepers service housings a player placed, as Forestry's AI does.
    tendPlayerOwned = false
    # false pairs any princess with any drone, as Forestry's AI does.
    requireSameSpecies = true
    # Horizontal search radius in blocks; Forestry uses 16.
    searchRadius = 16
```

`enabled` and `searchRadius` are read when a villager joins the world (that is when its goal is
created), so a change needs a restart to reach villagers that already exist. `tendPlayerOwned` and
`requireSameSpecies` are read on every visit.

All of this lives in this mod: Forestry's sources are not patched. The mixin described under "Mixin
setup" takes Forestry's AI out of the `forestry-1.21.1` alpha9 artifact that the build resolves, and
this mod attaches its own goal instead, so no locally modified Forestry build is involved.

Verification: on a dedicated server a development self-test spawned five beekeeper villagers next to
apiaries and read the result after 60 ticks:

* a matching princess and drone installed into an empty apiary, with the matching drone chosen out of a
  bag that offered a drone of another species first, and that foreign drone left in the bag;
* a mismatched pair refused: nothing installed, bag untouched;
* a player-placed apiary, and its villager's bag, untouched;
* a living queen left alone, with the drone offered to it still in the bag;
* the villager's goal list `[BeekeeperGoal]` and no `ApiaristAI`, i.e. the mixin took Forestry's AI out;
* a direct call of the inventory logic on a sixth apiary installed the pair, and a second call was a
  no-op.

The same self-test was run again against the `3.0.0-alpha9` jars (dev server, dedicated, no players),
21 checks, all of them passing:

* `forestry:apiary` resolves, and both apiaries have a `BeeHousingInventory`;
* the spawned beekeeper's goal list contains this mod's `BeekeeperGoal` and no
  `forestry.apiculture.apiarist.ApiaristAI`, so the mixin still cancels Forestry's AI in alpha9;
* a matching pair is installed, the queen and drone slots hold a princess and a drone of that one
  species, and the bag is emptied;
* a mismatched pair is refused and installs nothing;
* a player-owned apiary is left alone, both slots still empty;
* a living queen's produce is collected into the bag while the queen itself is untouched;
* the goal, driven by the vanilla goal selector, tends the apiary the villager stands next to
  (5 ticks after the apiary was emptied).

### Mixin setup

* `src/main/resources/forestrytweaks.mixins.json` — the mixin config. It has no `refmap`: NeoForge runs
  on Mojang's official names, so its mixins do not need remapping.
* `META-INF/neoforge.mods.toml` — `[[mixins]] config = "forestrytweaks.mixins.json"` with
  `requiredMods = ["forestry"]`.
* NeoForge puts Mixin (`sponge-mixin`) on the compile classpath, so `build.gradle` needs no extra
  dependency or annotation processor.

## Offline / restricted network

This machine cannot reach `maven.neoforged.net`, so the build is set up to work without it:

* `neoforge_local_mirror=../ForestryCE/build/local-m2` in `gradle.properties` adds a local
  Maven-format mirror of the NeoForge artifacts as an extra repository (empty = disabled).
* Everything else resolves from the Gradle cache in `GRADLE_USER_HOME` (`D:\Gradle`) and Maven Local.

Use `--offline` when the network is unreachable:

```powershell
.\gradlew.bat --offline build
```

ModDevGradle is pinned to **2.0.140** on purpose: it is the newest version whose plugin marker and
implementation are both present in this machine's Gradle cache (the metadata Gradle 9 stores for
2.0.148 is missing, so `--offline` resolution fails there). With a working network you can bump it
in `build.gradle`.

The Create artifacts (`create-1.21.1`, Ponder, Flywheel, Registrate) resolve offline from the Gradle
cache as well; that is why the repository declarations above deliberately have no content filters —
Gradle keys cached module metadata by repository, so a filtered or renamed repository would miss the
cache.

## Common commands

```powershell
.\gradlew.bat --offline build                  # compile + jar into build/libs
.\gradlew.bat --offline compileJava            # compile only
.\gradlew.bat --offline runClient              # dev client
.\gradlew.bat --offline runServer              # dev server (game dir: run/, needs run/eula.txt)
.\gradlew.bat --offline runData                # datagen -> src/generated/resources
.\gradlew.bat --offline verifyForestryDependency
```

`runServer` writes its world, config and logs to `run/`; accept the EULA once with
`echo eula=true > run/eula.txt` (already done here).

## Layout

```
gradle.properties                  versions, mod metadata, Forestry + Create coordinates
build.gradle                       NeoForge/ModDevGradle setup, Forestry + Create dependencies, runs
settings.gradle                    pluginManagement repositories + project name
src/main/java/com/forestrytweaks/
    ForestryTweaks.java            mod entry point (@Mod), registers the config, gates Create
    config/ForestryTweaksConfig.java        common config (rainMode, biomeModeSkyLight,
                                            ignobleDecayStartGeneration, beekeeper.*)
    config/RainMode.java           VANILLA / POSITION / BIOME
    tweak/RainChecks.java          the rain rule shared by both mixins
    tweak/IgnobleDecay.java        configurable ignoble ("卑贱") decay threshold
    apiculture/BeekeeperEvents.java         attaches the beekeeper goal to villagers (game bus)
    apiculture/BeekeeperGoal.java           the smarter apiary AI (a MoveToBlockGoal)
    apiculture/BeekeeperTending.java        what a beekeeper does inside a housing
    mixin/AbstractBeeHousingBlockEntityMixin.java  apiary/bee house rain check
    mixin/AlvearyControllerMixin.java              alveary rain check
    mixin/BeeMixin.java                            ignoble decay threshold
    mixin/EventHandlerApicultureMixin.java         keeps Forestry's beekeeper AI out
    integration/ForestryTweaksPlugin.java   Forestry plugin (ServiceLoader)
    integration/create/CreateIntegration.java       Create hook-up (RegisterEvent)
    integration/create/BeeItemAttributes.java       attribute type ids + the unit-attribute factory
    integration/create/BeeSpeciesAttribute.java     "is of species X" / "carries X recessively"
    integration/create/BeeLifeStageAttribute.java   "is a Queen" and friends
    integration/create/BeeStockAttribute.java       "is pristine/ignoble stock"
    integration/create/BeeGenomeAttribute.java      one gene per type: activity, flower type, effect,
                                                    tolerances, speed, lifespan, each expressed and
                                                    recessive
    integration/create/BeeProductAttribute.java     "produces X" / "has the specialty X"
    integration/create/Bees.java                    bee/stack helpers over the Forestry API
src/main/resources/
    forestrytweaks.mixins.json     mixin config (listed in neoforge.mods.toml)
    META-INF/neoforge.mods.toml    mod metadata + dependencies + mixin declarations
    META-INF/services/forestry.api.plugin.IForestryPlugin
    assets/forestrytweaks/lang/en_us.json, zh_cn.json   Create attribute filter strings
    pack.mcmeta
```

## Mod properties

`mod_id=forestrytweaks`, `mod_name=Forestry Tweaks`, group `com.forestrytweaks`, license
LGPL-3.0 (the same license as ForestryCE). Rename these in `gradle.properties` and the package
names if you want different identifiers.
