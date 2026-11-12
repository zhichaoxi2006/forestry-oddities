# Forestry Oddities

A Minecraft **1.21.1 / NeoForge** mod development environment for add-ons and tweaks to
[Forestry: Community Edition](https://github.com/thedarkcolour/ForestryCE) (ForestryCE).

The project is a working, buildable skeleton: it compiles against the Forestry API, declares
Forestry as a required mod dependency, and puts Forestry on the development runtime so `runClient` /
`runServer` load it. [Create](https://github.com/Creators-of-Create/Create) (机械动力) is wired in as
an **optional** dependency for the attribute filter integration.

## Versions

| Component | Version |
|---|---|
| Forestry Oddities | 0.2.0 (`mod_version`; see [CHANGELOG.md](CHANGELOG.md)) |
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
* `ForestryOdditiesPlugin` — implements `forestry.api.plugin.IForestryPlugin` and is registered via
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
* `ForestryOddities` — checks `ModList.get().isLoaded("create")` and only then calls
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
| `forestry_oddities:bee_species` | "is of species Imperial" | bees whose active species is that species (any of the four life stages) |
| `forestry_oddities:bee_species_recessive` | "carries Forest recessively" | bees whose recessive species allele is that species, i.e. what their offspring can turn into |
| `forestry_oddities:bee_life_stage` | "is a Queen" | drone, princess, queen or larvae |
| `forestry_oddities:bee_stock` | "is pristine stock" / "is ignoble stock" | whether the bee descends from naturally spawned (pristine) or bred (ignoble) stock |
| `forestry_oddities:bee_activity` | "has Diurnal activity" | the bee's activity allele: diurnal, nocturnal, crepuscular, metaturnal, ... |
| `forestry_oddities:bee_activity_recessive` | "has Diurnal activity recessively" | the activity allele the bee carries recessively |
| `forestry_oddities:bee_flower_type` | "has flower type Cacti" | the flowers the bee needs (Flowers, Cacti, Jungle, Nether, ...) |
| `forestry_oddities:bee_flower_type_recessive` | "has flower type Cacti recessively" | the flower type allele the bee carries recessively |
| `forestry_oddities:bee_effect` | "has the Creeper effect" | the bee's effect allele (Creeper, Radioactive, Flammable, ...) |
| `forestry_oddities:bee_effect_recessive` | "has the Creeper effect recessively" | the effect allele the bee carries recessively |
| `forestry_oddities:bee_temperature_tolerance` | "has Up 3 temperature tolerance" | how far the bee's ideal temperature may deviate |
| `forestry_oddities:bee_temperature_tolerance_recessive` | "has Up 3 temperature tolerance recessively" | the temperature tolerance allele the bee carries recessively |
| `forestry_oddities:bee_humidity_tolerance` | "has Both 2 humidity tolerance" | how far the bee's ideal humidity may deviate |
| `forestry_oddities:bee_humidity_tolerance_recessive` | "has Both 2 humidity tolerance recessively" | the humidity tolerance allele the bee carries recessively |
| `forestry_oddities:bee_speed` | "has Fastest speed" | the production speed allele |
| `forestry_oddities:bee_speed_recessive` | "has Fastest speed recessively" | the speed allele the bee carries recessively |
| `forestry_oddities:bee_lifespan` | "has a Longest lifespan" | the queen's lifespan allele |
| `forestry_oddities:bee_lifespan_recessive` | "has a Longest lifespan recessively" | the lifespan allele the bee carries recessively |
| `forestry_oddities:bee_cave_dwelling` | "can work underground" | bees with the `CAVE_DWELLING` gene |
| `forestry_oddities:bee_cave_dwelling_recessive` | "carries the underground gene recessively" | bees whose recessive `CAVE_DWELLING` allele is set |
| `forestry_oddities:bee_tolerates_rain` | "works in the rain" | bees with the `TOLERATES_RAIN` gene |
| `forestry_oddities:bee_tolerates_rain_recessive` | "carries the rain-tolerance gene recessively" | bees whose recessive `TOLERATES_RAIN` allele is set |
| `forestry_oddities:bee_analyzed` | "is analyzed" | bee items that have been scanned by an analyzer |
| `forestry_oddities:bee_mated` | "is mated" | princesses and queens that already carry a mate genome |
| `forestry_oddities:bee_product` | "produces Honey Comb" / "has the specialty X" | items in the species' product list, and its jubilant-state specialties |

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
* The strings live in `assets/forestry_oddities/lang/{en_us,zh_cn}.json`; Create renders an attribute as
  `create.item_attributes.<key>` and appends `.inverted` for the inverted variant, and the stage names
  come from `forestry_oddities.item_attributes.bee_stage.*`.

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

`rainMode` selects the rule (`com.zhichaoxi.forestryoddities.tweak.RainChecks`):

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

Configuration (`config/forestry_oddities-common.toml`):

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

Configuration (`config/forestry_oddities-common.toml`):

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

Configuration (`config/forestry_oddities-common.toml`):

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

### Backpack quick collect (背包快速收集)

Vanilla's bundle (`minecraft:bundle`, "收纳袋") can be fed from inside an inventory GUI: before its own
"take one / swap / place" logic, the container calls `Item#overrideStackedOnOther` and
`Item#overrideOtherStackedOnMe` on the two stacks being clicked together. Forestry's `BackpackItem`
implements neither, so right-clicking an item onto a backpack in the player inventory swaps the item
into the backpack's own slot (the backpack jumps to the cursor), and the only ways to put something
in are the backpack's screen and shift-right-clicking a container while the backpack is in `RECEIVE`
mode (see the note on modes at the end of this section).

This tweak adds the bundle's gesture to **every** backpack built on `BackpackItem`: the three
naturalist backpacks (`forestry:apiarists_backpack` plus its arborist and lepidopterist siblings),
the seven normal backpacks, their seven woven variants, and backpacks other mods create through
Forestry's backpack API. NeoForge turns the two item hooks into one cancellable
`net.neoforged.neoforge.event.ItemStackedOnOtherEvent`, fired on both sides, so
`tweak.BackpackQuickCollect` is a single game-bus listener and no mixin is involved.

| Gesture | Result |
|---|---|
| An item on the cursor, right-click the backpack | the stack goes in, as much as fits |
| The backpack on the cursor, right-click a stack | that stack goes in, as much as fits |
| The backpack on the cursor, right-click an empty slot | unchanged: the backpack is put down |
| Left-click, any stack | unchanged |

What goes in is exactly what the backpack would have accepted anyway, because the stow itself goes
through `BackpackItem#tryStowing`: the definition's item filter (a beekeeper's backpack only takes
bee items, a miner's backpack what its item tags allow), the `BackpackMode` the backpack reports, the
15/45/128 slots of the three sizes, and Forestry's own
`forestry.api.core.backpacks.BackpackStowEvent`, which other mods can still cancel.

Three behaviours are deliberate:

* **A right-click the backpack refuses is not consumed.** Right-clicking dirt onto a beekeeper's
  backpack still does what it did before (the two stacks swap), so a backpack can still be put into
  a chest by right-clicking it onto a stack the backpack has no interest in.
* **A backpack that wants the stack but is full consumes the click.** Nothing happens; the backpack
  is not swapped out of its slot. The bundle behaves the same way, and it turns the common accident
  ("the bag is full, now the item replaced it") into a no-op.
* **Nothing is ever taken *out* of a backpack by clicking.** The bundle's other half — right-clicking
  an empty slot with the bundle on the cursor to pull an item out — would take over the gesture that
  *places* a backpack into that slot, and a backpack is moved around far more often than one bee is
  pulled out of it. The backpack's screen keeps that job.

The stow runs on the client as well as on the server, so the GUI shows the result immediately instead
of waiting for the server's slot update; a backpack's contents live in the `minecraft:container`
component of the item stack, which the client has, so both sides compute the same result. The
bundle's insert sound is played on the client only — the server would echo every click back to the
player who made it.

Configuration (`config/forestry_oddities-common.toml`):

```toml
[backpacks]
    # false = backpacks behave exactly as in Forestry again.
    quickCollect = true
```

`quickCollect` is read on every click, so an edit applies live (each side reads its own copy).

Verification: a temporary development-server self-test (removed again afterwards) drove 29 checks
through real inventory clicks (`AbstractContainerMenu#clicked` on a fake player) and all of them
passed:

* a bee stowed into an apiarist's backpack from the cursor, and a second one on top of it, with the
  clicked slot and the backpack's mode component left untouched;
* dirt refused by the naturalist filter: nothing stowed, and the vanilla swap still ran;
* a left-click stowing nothing, and a backpack whose damage value was set to `LOCKED` stowing only
  when Forestry's `BackpackItem#getMode` actually reports `LOCKED` — the check confirmed the tweak
  asks the mode accessor instead of reading the damage value itself (see the note below: the accessor
  answers `NEUTRAL`, so nothing is refused for that reason today);
* a bee stowed in the other direction (backpack on the cursor, bee in the clicked slot), the emptied
  slot left empty and the backpack on the cursor;
* a refused stack left untouched by the slot direction, with the dirt surviving the vanilla swap;
* a full 128-slot naturalist backpack and a full 45-slot woven backpack stowing nothing, the offered
  stack going back into its slot and the backpack staying on the cursor, with no item created or lost;
* an empty slot still accepting the backpack on a right-click;
* the arborist's backpack refusing a bee (its naturalist filter is a different species type), the
  miner's backpack taking iron and refusing a bee, and the woven miner's backpack taking iron;
* 128 stows in a row into one naturalist backpack;
* `quickCollect` reporting its default of `true`.

Found while verifying, and worth knowing (upstream, not changed here): a Forestry backpack's mode
never leaves `NEUTRAL` with the alpha9 jar on NeoForge 21.1. `BackpackItem` stores its mode in the
stack's damage value, but NeoForge's `ItemStack#getDamageValue`/`setDamageValue` go through its
`IItemExtension#getDamage`/`#setDamage`, which clamp to `ItemStack#getMaxDamage()` — and a Forestry
backpack has no `MAX_DAMAGE` component, so a probe read `maxDamage=0 damage=0` after
`setDamageValue(1)` and `BackpackItem#getMode` answered `NEUTRAL`. Shift-right-clicking still cycles
the damage component, but `LOCKED`, `RECEIVE` and `RESUPPLY` have no observable effect, so `RECEIVE`
mode does not drain a chest either. The quick collect asks the same `BackpackItem#getMode`, so it
behaves exactly like Forestry's own stow path: a backpack Forestry reports as `LOCKED` takes nothing,
and in the current state the item filter alone decides.


### Mixin setup

* `src/main/resources/forestry_oddities.mixins.json` — the mixin config. It has no `refmap`: NeoForge runs
  on Mojang's official names, so its mixins do not need remapping.
* `META-INF/neoforge.mods.toml` — `[[mixins]] config = "forestry_oddities.mixins.json"` with
  `requiredMods = ["forestry"]`.
* NeoForge puts Mixin (`sponge-mixin`) on the compile classpath, so `build.gradle` needs no extra
  dependency or annotation processor.

## Frames (框架)

Two apiary frames. Both implement `forestry.api.apiculture.hives.IHiveFrame`, so they go into the
apiary's three frame slots and Forestry wears them through exactly the same call as its own frames:
`BeeHousingBeekeepingLogic#doProduction` → `IBeeListener#wearOutEquipment(1)` →
`ApiaryInventory#wearOutFrames` → `IHiveFrame#frameUsed`. That call happens once per **work cycle**,
which is `IBeekeepingLogic.DEFAULT_WORK_THROTTLE` = 550 ticks. The apiary is the only housing
Forestry: CE 3.0.0-alpha9 gives a frame slot to; the alveary gets one through the
[frame housing part](#alveary-frame-housing-蜂房组框架外壳) below, and the bee house still has none.

Both items are in the `forestry:apiculture` creative tab, right after Forestry's own frames, and both
are registered with stack size 1 because the state they carry (charge, potion, durability) is per
stack.

### 柔光框架 / Soft Light Frame

| Recipe | | |
|:---:|:---:|:---:|
| | 红石灯 | |
| 铜锭 | 标定框架 | 铜锭 |
| | 铁锭 | |

```text
 L
CFC
 I
```

`L` = `minecraft:redstone_lamp`, `C` = `minecraft:copper_ingot`, `I` = `minecraft:iron_ingot`,
`F` = `forestry:proven_frame` (标定框架).

The frame is a sunlight simulation. Forestry refuses to let a queen work when the housing cannot see
the sky unless the bee has the `CAVE_DWELLING` gene or a modifier answers `true` from
`IBeeModifier#isSunlightSimulated()`; this frame does the latter, so an apiary underground, in a cave
or under a solid roof works without breeding cave-dwelling into the line. It is the only modifier the
frame contributes - production speed, territory, aging and so on are untouched, so the apiary's own
`0.1x` production modifier still applies.

It is not free:

* The frame holds up to **1 000 000 FE (1 MFE)**, and **every work cycle costs 80 FE**. A full buffer
  is 12 500 cycles, i.e. about 95 hours of an apiary that never idles.
* **Newly crafted frames are empty.** The buffer is exposed as a NeoForge item energy storage
  (`Capabilities.EnergyStorage.ITEM`, receive only, `canExtract() == false`), so any FE item charger
  fills it; the frame spends the energy itself and nothing can pull it back out.
* The modifier reports `true` while the stack holds at least 1 FE. The cycle that drains the last
  charge still counts as simulated, after that the frame is inert until it is charged again.
* The frame has no durability at all: it never wears out and is never destroyed. Running dry only
  switches the sunlight simulation off.

State is visible without a tooltip:

* The icon is a **标定框架 with a redstone lamp set into its opening**: the proven frame's silhouette
  is copied pixel for pixel from `assets/forestry/textures/item/proven_frame.png` (every solid pixel
  is identical), and the 8x6 window the frame leaves empty - including the cream mesh strip in the
  middle of that window - is filled with vanilla's lamp block texture, so the lamp's lattice shows
  through the frame instead of the frame being replaced by a lamp icon. The lamp texture is sampled
  at half scale (an 8x8 crop of it, its middle six rows) because a 1:1 crop of a 16x16 block face
  would show one corner of the lattice blown up; at half scale the whole lamp pattern stays
  recognisable. Two textures ship, `item/soft_light_frame.png` (built from
  `minecraft:block/redstone_lamp`, the charge is empty) and `item/soft_light_frame_on.png` (built
  from `minecraft:block/redstone_lamp_on`, charged); they differ only inside the window.
  `models/item/soft_light_frame.json` has one override on the mod's own `forestry_oddities:charged` item
  model property (registered client side, `1.0` while the charge is above zero) that switches to
  `soft_light_frame_on`, which is the same frame with the lit lamp.
* The item bar is the charge, 13 segments wide, in amber.
* The tooltip prints `energy / capacity FE`, whether sunlight is currently simulated, the per-cycle
  cost and - when the frame is empty - a reminder that it needs charging.

The charge lives in the `forestry_oddities:soft_light_frame_energy` data component (persistent and
network-synchronised), so it survives a save/load and a container sync; the client needs it to pick
the model.

### 灌注框架 / Infusion Frame

| Recipe | | |
|:---:|:---:|:---:|
| 木棍 | 木棍 | 木棍 |
| 烈焰粉 | 药水 | 烈焰粉 |
| 木棍 | 木棍 | 木棍 |

```text
SSS
BPB
SSS
```

`S` = `minecraft:stick`, `B` = `minecraft:blaze_powder`, `P` = any potion
(`forestry_oddities:potions`: `minecraft:potion`, `minecraft:splash_potion`, `minecraft:lingering_potion`).

The potion is stored on the frame as vanilla's `minecraft:potion_contents` component, which gives the
frame everything it needs:

* **Durability = the potion's duration in whole seconds.** A 3:00 potion gives a 180-use frame, 8:00
  gives 480, and 1:30 gives 90. The frame wears one point per apiary work cycle and breaks like any
  other frame. The maximum damage depends on the potion, so it is written onto the crafted stack
  (`DataComponents.MAX_DAMAGE`) instead of being a property of the item type - that is why the item
  is registered with stack size 1 and why the item cannot be crafted without a potion.
* **Modifiers = the potion's effects.** The mapping is read from the stored `PotionContents` every
  time Forestry asks for the frame's `IBeeModifier`, so a frame from an old save keeps working when
  the table below changes:

| Potion | Modifier |
|---|---|
| 速度 Speed | production speed `x (2 x level)` (Speed I `x2`, Speed II `x4`) |
| 中毒 Poison | aging `x 10^level`, i.e. the queen loses that many times more lifespan per work cycle |
| 再生 Regeneration | cancels aging entirely (`modifyAging` returns `0`) |
| 虚弱 Weakness | mutation chance `x 10^level` |
| 水下呼吸 Water Breathing | sealed housing: bees work in the rain without `TOLERATES_RAIN` (`isSealed()`) |

Details that follow from that:

* "Level" is the level the potion tooltip shows, `amplifier + 1`.
* A potion with several of the effects (custom or splash potions) gets all of them; multipliers of
  the same effect stack. Regeneration wins over Poison, so poison + regeneration does not age the queen.
* Exponents are capped at `10^6` and the mutation chance at `100` so a hacked amplifier cannot
  overflow the float or the percentage scale. Weakness II is therefore already a guaranteed mutation.
* Speed is a multiplier, not an additive bonus: the apiary's own `0.1x` modifier is first in
  `ApiaryBlockEntity#getBeeModifiers`, then the frames are applied, so a bee whose `SPEED` allele is
  `1` produces at `0.1 x 2 = 0.2` with a Speed I frame (`0.1 x 4 = 0.4` with Speed II). A potion that
  carries none of the table's effects leaves every value at its neutral `x1`, so the frame never
  zeroes anything out: `of()` starts the multipliers at `1.0`, and the tooltip and `isEmpty()` ask
  `> 1.0` / `<= 1.0` for the same reason.
* **Colour = the potion's colour.** The icon is the same 标定框架 silhouette as the soft light
  frame, with its opening filled by liquid and nothing else: no bottle, no cork, the whole 8x6
  window is liquid. The liquid is vanilla's still-water texture sampled exactly like the lamp (half
  scale, middle rows), with its grey range `[165, 255]` stretched to `[197, 255]` - `0xC5` is the
  midtone of `minecraft:item/potion_overlay` - so the tinted window is as bright as the liquid
  inside a vanilla potion instead of darker than the potion it is named after. The icon is split
  over two layers so only the liquid takes the tint: `item/infusion_frame.png` holds the frame with
  an empty opening, and `item/infusion_frame_overlay.png` holds the 48 liquid pixels of that
  opening in pure grey. An item colour handler registered on the mod bus tints `layer1` (the
  liquid) with `PotionContents#getColor()`, the same colour vanilla uses for the potion item, so the
  frame visually carries its potion; `layer0` returns `-1` and stays untinted. Generated item
  models number their tint indices by layer (`ItemModelGenerator` passes the layer index as the
  face's `tintindex`), so "layer1" really is tint index 1. A frame without potion data shows
  vanilla's default potion colour (the water blue).
* The tooltip is the kind Forestry's own frames have, not the kind a potion item has. Copying what
  `forestry.apiculture.apiary.HiveFrameItem` does (disassembled from 3.0.0-alpha9): one unstyled line
  per modifier, then `item.forestry.durability` with the maximum uses - and only while the frame is
  undamaged, because a worn frame shows the vanilla durability bar instead. A proven frame therefore
  reads `Production: 2.0x / Genetic Decay: 0.3x / Durability: 720` and an infusion frame reads
  `Work speed: x2 / Durability: 180`, the durability line in Forestry's own words. The potion's
  effect list (vanilla's `addPotionTooltip`) is **not** repeated: on an apiary those effects *are* the
  modifier lines, and the tinted window already shows which potion it was. A frame whose potion
  contributes none of the table's effects (the empty creative-tab frame, a water bottle) gets one
  grey line saying so.
* A frame without potion data (the plain creative-tab entry, `/give`) has no durability and no
  modifiers, and `frameUsed` leaves it alone instead of dividing by a zero maximum damage.

Recipe mechanics: a vanilla `crafting_shaped` recipe cannot do this, because the potion's data has to
travel from the ingredient to the result. `content/InfusionFrameRecipe` is therefore a proper shaped
recipe with its own serializer (`forestry_oddities:crafting_infusion_frame`) and the same JSON layout as
the vanilla shaped recipe; `assemble` finds the potion in the grid, copies its `PotionContents` onto
the result and writes the matching maximum damage. Because it is an ordinary `CraftingRecipe` with a
pattern and ingredients, JEI shows it like any other shaped recipe. Both recipes also ship recipe-book
unlock advancements (`data/forestry_oddities/advancement/recipes/misc/`), so the recipe book lists them as
soon as the player has held a 标定框架 (`forestry:proven_frame`) or blaze powder, respectively.

Verification: on a dedicated development server a temporary self-test (100 checks, removed again
afterwards) covered: both items and the data component registered under the expected ids; the
`forestry_oddities:potions` tag resolving to the three potion items; both recipes loading from their
JSON and the infusion recipe being found by `RecipeType.CRAFTING`; matching a normal and a splash
potion grid and rejecting a non-potion and an empty grid; `assemble` producing an infusion frame with
the potion component, `MAX_DAMAGE` 180 and damage 0 for a 3:00 swiftness potion; the recipe surviving a
`RegistryFriendlyByteBuf` round trip; every potion row of the table (`swiftness` `x2`, strong
swiftness `x4`, poison `x10` aging, strong poison `x100`, regeneration aging `0`, weakness `x10`
mutation and its `100` clamp, water breathing `isSealed`); durability 180/45/90/180 for
swiftness/poison/weakness/water breathing and 1 for a potion with no effects; a 180-cycle frame
breaking exactly on its 180th use; a bare frame being left untouched; the soft light frame starting
empty, its capability accepting and clamping energy, refusing extraction while simulation stored
nothing, 80 FE per work cycle draining 100 FE to 20 and then to 0, the component being removed at 0,
and the modifier switching off when it runs dry; and both stacks surviving NBT and network round trips
with their component intact.

The tooltip got its own temporary self-test on the same server (44 checks, removed again): a bare
frame's tooltip lines are just the grey "no potion" line - no durability line, because it is not
damageable; a swiftness frame's lines are `Work speed: x2` and then `item.forestry.durability` with
`180`; a frame at damage 5 loses the durability line and keeps the modifier; each potion row of the
table yields exactly one modifier line of its own and no vanilla potion effect line; a custom potion
with speed + poison + weakness + water breathing yields the four lines in the table's order and the
durability line after them, all of them unstyled; and the reference item `forestry:proven_frame` emits
the same `item.forestry.durability` key unstyled, so both the key and the styling really are
Forestry's. The line 0 of every tooltip (the display name) was excluded from the line comparisons.

The icons were checked pixel by pixel against the vanilla sources they are built from (a one-off
script, 55 assertions, all passing): every solid pixel of the three textures is identical to
`forestry:item/proven_frame`, nothing is drawn outside that silhouette, all 48 window pixels of both
soft light frames are the (half-scale) lamp texture and the two variants differ only inside the
window with the lit one measurably brighter; the infusion frame's `layer0` leaves those same 48
pixels empty while its overlay fills exactly them - every one of them pure grey in `[197, 255]` and
equal to the remapped still-water sampling - so the overlay never overlaps the frame and the liquid
covers the window without a gap; multiplying the overlay by `swiftness`, `poison`, `regeneration`
and `water_breathing` keeps the potion's dominant colour channel and a visible brightness; and every
texture the three models reference exists inside the built jar.

## Alveary frame housing (蜂房组框架外壳)

Forestry's alveary has **no frame slots**: only the apiary's three slots are worn out, which is why the
Forester's Manual says "I still have not managed to stuff frames into a bee house or an alveary". This
part adds the alveary's missing slot: a wall part that holds **one** frame.

| Recipe | | |
|:---:|:---:|:---:|
| 木板 | 木棍 | 木板 |
| 木棍 | 蜂房组 | 木棍 |
| 木板 | 木棍 | 木板 |

```text
PSP
SXS
PSP
```

`P` = any `minecraft:planks`, `S` = `minecraft:stick`, `X` = `forestry:alveary_block`. The part sits in
the `forestry:apiculture` creative tab directly after Forestry's own alveary parts, it is in
`#minecraft:mineable/axe` like the rest of the structure, and the recipe ships a recipe-book unlock
that fires once the player has held an alveary block.

How it works:

* Build it into an alveary like any other part: the exterior cells of the 3×3×3 box, not the top layer
  and not the interior (those stay plain blocks). While the structure is incomplete a right-click
  prints the pattern error, exactly like Forestry's own parts.
* Right-click the part once the alveary is assembled to open a one-slot GUI. Only
  `forestry.api.apiculture.hives.IHiveFrame` items fit - Forestry's untreated, impregnated, proven and
  creative frames as well as this mod's 柔光框架 and 灌注框架. Everything else is refused by the slot
  filter.
* While a frame is in the slot its `IBeeModifier` is folded into the alveary's modifiers: production
  speed, mutation chance, aging, territory, pollination, genetic decay, sealed, always active,
  sunlight simulation, hellish, flowers and climate tolerance are all forwarded. The modifier reads the
  slot on every query, so pulling the frame out stops its effect immediately and swapping frames needs
  no reassembly.
* Frames are worn by the alveary's work cycles, not by a timer:
  `BeeHousingBeekeepingLogic#doProduction` calls `IBeeListener#wearOutEquipment(1)` once per cycle and
  the part hands that point to `IHiveFrame#frameUsed` with the working queen - one point per cycle,
  exactly what the apiary does. A frame that breaks on its last point leaves the slot empty; the mod's
  soft light frame instead pays its 80 FE per cycle and stays in place.
* The alveary has no production penalty of its own (the apiary multiplies by `0.1`), so a proven frame
  (×2 production) doubles the alveary's production speed, while the same frame in an apiary takes its
  `0.1` base to `0.2`.
* The slot is persistent and drops with the part: `IMultiblockComponent.HasInventory` makes Forestry
  save the frame in the part's NBT, and `BlockStructure` drops it when the part is broken. A NeoForge
  item handler capability (`Capabilities.ItemHandler.BLOCK`) exposes the slot to automation, so pipes
  and hoppers can insert and pull frames; the slot filter still applies.
* The GUI is Forestry's `ContainerTile` + `GuiForestryTitled` pair, so it uses the same layout as the
  other alveary part screens and shows the alveary's error ledger (no queen, no sky, ...). Its texture
  is `assets/forestry_oddities/textures/gui/alveary_frame_housing.png`: Forestry's standard
  player-inventory slice with a recessed 34×34 display that holds the one slot. The file is **256×256**
  with the 176×166 GUI in its top-left corner, because
  `GuiGraphics#blit(ResourceLocation, int, int, int, int, int, int)` always samples with a hardcoded
  256×256 texture size - a 176×166 file gets stretched by 256/176 and every slot ends up next to its
  drawn background instead of inside it.

How the part joins the multiblock: it is an `AbstractAlvearyBlockEntity` (the "for addons"
constructor) implementing `IAlvearyComponent.BeeModifier`, `IAlvearyComponent.BeeListener` and
`IMultiblockComponent.HasInventory`. The declarative alveary pattern accepts every component whose
pattern type id starts with `alveary_`, which the base class supplies (`alveary_part`), and
`AlvearyController#bucketComponents` collects the modifier and listener by `instanceof` when the
structure assembles, so no registration with Forestry is needed beyond the block and its block entity.

Verification: a development self-test on a dedicated server (removed again afterwards), 30 checks, all
passing:

* the block, item, block entity type and menu register under `forestry_oddities:alveary_frame_housing`,
  the block entity type accepts the block, and the menu type is the registered one;
* a 3×3×3 alveary of plain blocks with a wooden slab cap assembles with the part in an exterior cell,
  and the part's modifier and listener end up in the controller's sets;
* the part's own inventory is the frame slot (one slot), it accepts a hive frame and rejects a stick;
* no frame means no production bonus, a proven frame doubles production speed, and pulling it out
  removes the bonus immediately;
* one work cycle wears the frame by one point, no queen means no wear, and a frame that breaks on its
  last point leaves the slot empty;
* the frame survives a `saveAdditional`/`loadAdditional` round trip;
* breaking the part drops the frame (checked on the world's item entities, in a ticking chunk);
* the shaped recipe loads, matches its pattern and produces the part.

The screen was also opened in the development client and screenshotted (that check is the reason the GUI
texture is 256×256): the frame slot's 16×16 interior lands exactly on the menu's `(80,38)` slot inside
the recessed display, the player inventory (rows at y=87/105/123, hotbar at 145) lines up with the drawn
slot backgrounds, and the title is centred in the top row like Forestry's own part screens.

## Alveary electrical parts (蜂房组电路巢础 / 蜂房组配电 / 蜂房组进刻器)

Two more alveary wall parts, ported from Binnie's Mod - Extra Bees' **Electrical Stimulator**
(电刺激器) and **Alveary Transmission** (蜂房组配电) - through that mod's NeoForge 1.21.1 port. The pair
is one feature in two blocks: the circuit foundation turns FE into bee modifiers by running a
**Forestry circuit board**, and the transmission is the part that feeds it from a single power line, so
an alveary full of circuit foundations needs one cable instead of one per block face.

The section's third part, the **ticker** (蜂房组进刻器), is this mod's own rather than a port. It
spends the same FE on changing *when* the bees work instead of what they do: every tick it advances
the housing's work-cycle counter, up to one whole cycle per game tick.

**Why it is called the circuit foundation.** Binnie's Mod and this mod would otherwise ship two
blocks called *Alveary Stimulator* (电刺激器) and two soldering-iron layouts called *Electrical
Stimulator* (电子增产器): the same two entries in the alveary's part list and the same two in the
iron's layout list, with no way to tell them apart. The port therefore takes a name of its own - the
rest of the machine is unchanged, down to the layout's programs and the art it draws. Its layout is
*Circuit Foundation* (电路巢础).

Both are ordinary alveary parts: build them into the exterior of the assembled structure, like
Forestry's own heater or sieve. They cannot go into the top layer or the interior, and they are in
`#minecraft:mineable/axe` like the rest of the structure.

### Recipes

| 蜂房组电路巢础 | | |
|:---:|:---:|:---:|
| 先进电路板 | 金电子管 | 先进电路板 |
| 红石粉 | 蜂房组 | 红石粉 |
| 铜锭 | 铜锭 | 铜锭 |

```text
KTK
RXR
CCC
```

| 蜂房组配电 | | |
|:---:|:---:|:---:|
| 铜齿轮 | 红石粉 | 铜齿轮 |
| 红石粉 | 蜂房组 | 红石粉 |
| 铜齿轮 | 红石粉 | 铜齿轮 |

```text
GRG
RXR
GRG
```

`K` = `forestry:refined_circuit_board`, `T` = `forestry:golden_electron_tube`,
`R` = `c:dusts/redstone`, `C` = `c:ingots/copper`, `G` = `c:gears/copper`,
`X` = `forestry:alveary_block`. The recipes are this mod's own, **not** the reference
implementation's, and that is deliberate: Binnie's Mods ships the reference's two grids verbatim -
`kik` / `iai` / ` t ` with gold nuggets, and ` t ` / `tat` / ` t ` with tin gears - so while this mod
kept them, exactly one of the two parts could ever be crafted whenever both mods were installed.
Vanilla's `RecipeManager#getRecipeFor` answers with the *first* matching recipe
(`byType(type).stream().filter(matches).findFirst()`), not with all of them, so the loser is simply
unobtainable while JEI and the recipe book keep listing both. The machine is still the reference's -
layout, fourteen programs, 500 FE offers, GUI and block art are untouched - only the grid says
something of its own: copper and redstone wiring hugging the hive block, the socket's two circuit
boards and the golden tube on top, and four copper gear ports wired by redstone on the transmission.
The ingredients are common tags NeoForge and Forestry already fill (`c:dusts/redstone`,
`c:ingots/copper`, and `c:gears/copper` holds `forestry:copper_gear`, which is four vanilla copper
ingots and an iron nugget), so neither part needs a tin provider to stay craftable.
`tools/check_recipes.py` re-checks every shaped recipe in the pack for the "same grid, different
result" collision this rewrite exists to avoid. Each ships a recipe-book unlock that fires once the
player has held an alveary block.

### 蜂房组电路巢础 / Alveary Circuit Foundation

The part is a **circuit socket fed by power**. Its one socket takes a circuit board programmed in this
mod's *Circuit Foundation* layout, and while the part can pay the board's total draw, the
board's programs are folded into the alveary's modifiers.

**How a board is programmed.** Take a Forestry **Soldering Iron**, put an empty circuit board and
electron tubes into its slots, and use the `<` / `>` buttons to select the *Circuit Foundation* layout.
The iron lists every registered layout, so the circuit foundation's layout appears next to Forestry's
own farm/machine/engine layouts, and with it selected each of the fourteen tubes means one program. The
board type decides how many programs fit, exactly as for a machine upgrade board: a basic board carries
one tube, an intricate board four.

| Electron tube | Circuit | Effect | FE / work cycle |
|---|---|---|---|
| 铁电子管 `iron_electron_tube` | Production Up I | production ×1.5 | 10 |
| 钻石电子管 `diamantine_electron_tube` | Production Up II | production ×2.5 | 20 |
| 青金石电子管 `lapis_electron_tube` | Flowering Up I | pollination ×1.5 | 10 |
| 黑曜石电子管 `obsidian_electron_tube` | Lifespan Down I | aging ÷0.8, i.e. the queen lives 20% shorter | 10 |
| 磷灰石电子管 `apatine_electron_tube` | Lifespan Up I | aging ÷1.5, i.e. the queen lives 50% longer | 10 |
| 烈焰电子管 `blazing_electron_tube` | Hell | simulates a hellish climate (Nether bees work anywhere) | 15 |
| 金电子管 `golden_electron_tube` | Mutation Up I | mutation chance ×1.5 | 15 |
| 锡电子管 `tin_electron_tube` | Production Down I | territory ×0.4 and production ×0.9 | 10 |
| 青铜电子管 `bronze_electron_tube` | Territory Up I | territory ×1.5 | 10 |
| 铜电子管 `copper_electron_tube` | Rainproof | the bees work in the rain, like an alveary rain shield | 10 |
| 硅电子管 `silicon_electron_tube` | Sunlight | simulates daylight, so a hive underground works like one in the open | 15 |
| 末影电子管 `ender_electron_tube` | Night Shift | the bees work day and night | 15 |
| 琥珀电子管 `amber_electron_tube` | Climate Control | ignores temperature and humidity (heater + fan + hygroregulator) | 25 |
| 绿宝石电子管 `emerald_electron_tube` | Flower Source | provides flowers without a flower bed | 35 |

The first nine are the reference implementation's, one program per tube. The last five are this mod's:
they take the tubes Forestry's own circuits never use and give them the other half of `IBeeModifier` -
the boolean questions about whether a place is *workable* at all, which the 1.12 machine never touched.
That is also why they are priced higher than the numeric programs: a rain shield, a lighting block, a
heater, a fan and a flower bed are things a player would otherwise build, so these tubes stand in for
structure blocks instead of multiplying a number. Because they answer yes/no questions, two tubes that
answer the same one do not stack, while two numeric programs do.

Two rules apply to every program:

* **Genetic decay ×1.5 per program.** A board with four programs raises the decay to `1.5^4 = 5.06x`,
  so a circuit foundation is never free even when its programs look harmless. The circuit tooltips say so.
* **The whole board is paid at once.** One work cycle (550 ticks, `IBeekeepingLogic.DEFAULT_WORK_THROTTLE`)
  drains the *sum* of the fitted programs' draws, so a cheap program cannot ride along for free next to
  an expensive one. The programs only apply while the buffer can pay that sum: at 59 FE a 60 FE board
  does nothing, at 60 FE it runs, and after it has been drained it switches off until it is charged
  again.

The buffer is **1 000 FE**, accepts **500 FE** per offer and is **receive-only**: any FE source (cables,
chargers, machine item slots) can fill it, but nothing can pull the energy back out - the part spends
it itself. It is exposed as a NeoForge block energy capability, so no mod needs to know about the
circuit foundation to charge it.

The modifiers compose with the rest of the alveary in the usual way: Forestry folds every part's
`IBeeModifier` into the housing's combined modifier, in sequence, so two Production Up boards multiply
twice and a Lifespan Up board cancels part of a Lifespan Down board. The modifiers are re-read from the
socket on every query (through a one-entry cache keyed on the socketed stack, because Forestry asks
several times per bee tick), so a new board, a removed board and an empty buffer all take effect
immediately.

**The board is a Forestry socket, not an inventory slot** - the same widget a Forestry machine uses for
its chipset. With a programmed board on the cursor, click the socket; it goes in and the cursor stack
shrinks by one. To take it back out, click the socket with a **Soldering Iron** in hand: the board goes
to the player inventory and the iron loses one point of durability. A board that is not programmed in
the circuit foundation layout is refused (Forestry's own machine upgrade boards included), and no pipe or hopper
can reach the socket in either direction. Breaking the part drops the socketed board.

The board is not consumed; only the FE is spent - there was no board wear in 1.12 either.

A socket is not a menu slot, so nothing syncs it for free: the part writes the socket into its chunk
update tag (`getUpdateTag`, because the vanilla default is an *empty* tag and
`MultiblockTileEntityBase` only adds the multiblock payload), and Forestry's `PacketSocketUpdate`
carries every later change. Without that first half a reloaded chunk hands the client a part whose
socket looks empty - and a board that is already inside then refuses a second click, because the
server's socket is not empty at all. Forestry's own machines do not need it because their board is a
slot.

The GUI is the reference window itself: Binnie's 1.12 `guistimulator.png` - a 256×256 sheet whose
top-left 176×144 is the GUI - copied into
`assets/forestry_oddities/textures/gui/alveary_circuit_foundation.png` and drawn the way that mod's 1.21.1 port
draws it.

* **Window.** The port keeps the legacy art and puts a 40-pixel header strip above it, so the window is
  176×179 instead of Forestry's 176×166: the sheet's top border is blitted once, the strip's side columns
  are stretched down it, the strip between them is plain panel, and the legacy body - from texture row 5,
  so its border is not drawn twice - starts 35 pixels lower. Everything baked into the art moves with it:
  the player inventory frames at the legacy rows 63/81/99/121 are drawn at **98/116/134/156**, which is
  where the menu puts its slots. That header is also why the screen extends `GuiForestry` rather than
  `GuiForestryTitled`: the port's title sits on the strip at **y=28**, not on Forestry's y=6 title row.
* **Socket.** At `(44,72)` - legacy `(44,37)` - drawn by the window's own art, which is shipped exactly
  as Binnie has it: that sheet already carries a socket cell there (a little lower and clipped compared
  with the 1.12 original's 18×18 recess at `(43,36)`, whose 16×16 plate starts at the item origin), and
  this mod does not retouch it. Forestry's `SocketWidget` paints the board over that cell; its tooltip is
  the board's program list plus Forestry's *Removable with a Soldering Iron* line, or *Empty socket* when
  it is empty, and its clicks are handled by Forestry's `ContainerSocketedHelper` on the server, so this
  mod only validates what may be socketed.
* **Energy gauge.** At `(70,72)` - legacy `(70,37)` - 72×16, exactly over the recess the art already has.
  It is Binnie's `ControlEnergyBar` layering, ported in `GuiEnergyBar`: the dark frame, then the charge
  tinted from red at empty through yellow at half to green at full, then the glass highlight, all cut as a
  one-pixel nine-slice from Binnie's `craftgui-controls.png` (also copied verbatim, as
  `assets/forestry_oddities/textures/gui/craftgui-controls.png`). Hovering it shows `Energy: x / 1000 FE`
  and `Usage: n FE per work cycle`; like Forestry's own widget tooltips it stays hidden while the player
  carries a stack.
* **Ledger.** The error ledger belongs to the part, not to the alveary, and it carries the port's two
  conditions in the port's order: an empty socket reports Forestry's `NO_CIRCUIT_BOARD`, and a board the
  buffer cannot pay for reports `NO_POWER` - both with Forestry's own sprites, titles and help text. There
  is no status line: the gauge shows the charge and the ledger shows what is missing, as in the original.
* Both sheets are **256×256**, which is what `GuiGraphics#blit(ResourceLocation, int, int, int, int, int,
  int)` samples with regardless of the file's size.

Block art: both parts use **Binnie's Mod's own textures** - `stimulator_side.png` and
`transmission_side.png` from that mod's 1.21.1 port, copied verbatim (byte-identical, SHA-256 checked) as
`assets/forestry_oddities/textures/block/alveary_circuit_foundation.png` and `.../alveary_transmission.png`. Each
file is a complete 16×16 cube face - creviced wooden alveary side with the machine face worked into it,
the glowing coil and the transmission's grid - so there is no separate overlay and no plate
to place. Binnie's part models point their `end` faces at a `*_top` file that is pixel-identical to the
`*_side` one, so this mod uses the single file on all six faces (`minecraft:block/cube_all`), which
renders exactly what Binnie's two files do. Binnie's Mods and Forestry Oddities are both LGPL-3.0, so the
art is redistributed under the same terms as the code.

### 蜂房组配电 / Alveary Transmission

A power distributor and nothing else: it **never touches the bees**, so powering it changes no
production, territory or lifespan by itself. It buffers up to **1 000 FE** (500 FE per offer, receive
only) and hands what it holds to the other parts of the assembled alveary **once per tick**, through
the NeoForge block energy capability, so any addon part with an FE buffer can be fed, not just this
mod's circuit foundation.

* Each sweep splits the output limit (500 FE) evenly over the parts that can receive, and stops when
  the buffer is empty, so one transmission can run several circuit foundations at once.
* A transmission **never feeds another transmission**: both buffers are receive-only, so two of them
  would push the same energy back and forth forever. This is the one behavioural addition to the
  reference implementation, which had the same infinite-bounce hole.
* It only walks the multiblock while it actually holds energy, like the original, so an idle
  transmission costs nothing.
* It has **no GUI**: right-clicking it opens the alveary's own screen, exactly like Forestry's parts,
  because there is nothing to show or configure.
* Block art: `textures/block/alveary_transmission.png` is Binnie's own transmission face (see the
  circuit foundation's note above): the same creviced alveary side with the power grid worked into it.

### 蜂房组进刻器 / Alveary Ticker

| 蜂房组进刻器 | | |
|:---:|:---:|:---:|
| | 精制电路板 | |
| 时钟 | 蜂房组 | 时钟 |
| | 精制电路板 | |

```text
 K
CXC
 K
```

`K` = `forestry:refined_circuit_board`, `C` = `minecraft:clock`, `X` = `forestry:alveary_block`. It is
the only part in this section that is not a port: it is this mod's own answer to the alveary's fixed
27.5-second work cycle, and it spends the FE the other two parts deliver.

Forestry runs a queen's work cycle on a **550-tick throttle**: `BeeHousingBeekeepingLogic` adds one
to a private counter every game tick and works once the counter reaches `maxWorkThrottle`. The ticker
buys extra counter ticks with FE:

* It is an `IAlvearyComponent.Active`, so the alveary calls `updateServer` once per server tick,
  before the bee logic works. Every tick it adds up to `speed - 1` ticks to the counter and pays
  **10 FE for each one**. The natural +1 per game tick still happens, so speed **x1** is exactly the
  unaccelerated alveary and speed **x`maxWorkThrottle`** is exactly **one work cycle per game tick** -
  a higher speed could not buy anything, because the counter is checked once per tick.
* The maximum is the housing's *current* `maxWorkThrottle`, re-read every tick. Another mod can call
  `IBeekeepingLogic#setWorkThrottle`; the ticker's slider and clamp follow it, and a throttle that is
  lowered also lowers the saved speed.
* Only whole ticks are bought: with `n` FE in the buffer the part adds
  `min(speed - 1, n / 10)` ticks and spends exactly that many times 10 FE. A half-empty buffer slows
  the acceleration down instead of giving away progress for free. An empty queen slot (or a princess
  that is still breeding) spends nothing, because there is no work cycle to advance - the breeding
  counter is not the throttled one.
* The buffer is **20 000 FE**, accepts **10 000 FE** per offer and is **receive-only**, exposed as a
  NeoForge block energy capability like the other powered parts. 20 kFE is about 3.6 seconds at full
  speed, and a 1 kFE buffer would last a fifth of a tick there - the transmission (500 FE per tick)
  can feed a lower setting from one cable, while a direct line covers the top end.
* Right-clicking the assembled part opens its GUI: a **speed slider** from x1 to the housing's work
  throttle, and the clock face. The 176x166 window is laid out as a small instrument panel: the speed
  row across the top (caption, brass-gauge slider, and a brass plate with the current multiplier), a
  **32x32 vanilla clock** in a brass bezel below it, and a readout panel beside the clock. The panel
  lists the FE per tick and the seconds per work cycle and draws the buffer with the same Binnie
  `ControlEnergyBar` gauge the circuit foundation uses. Hovering the clock repeats the numbers as a
  tooltip, hovering the gauge shows the charge. A click jumps the knob to the pointer and holding it
  drags the speed live - a container screen drops mouse drags before any widget sees them, so the
  screen forwards them to the slider itself - and every change is sent as a menu-button packet, so no
  custom network packet is needed; the block entity clamps it, saves it and sends the block update the
  clock needs.
* The block texture is Forestry's alveary side with **Minecraft's own clock item sprite** (the
  `clock_00` frame) worked into it, on all four sides (top and bottom keep `alveary_bottom`); the item
  icon uses the same texture, so the inventory icon is the same vanilla clock. The in-world animation
  draws the vanilla clock's 64 frames (`textures/item/clock_00.png` ... `clock_63.png`) as a quad just
  in front of each exposed side, and the frame rate is the setting - one full turn of the dial every
  1280 ticks at x1, down to 64 ticks (3.2 s) at the throttle. A side that would not render its block
  face is skipped with `Block#shouldRenderFace`, so a ticker in the middle of the wall never paints a
  clock onto its neighbour, while the GUI's 32x32 copy uses the same frames. Each quad takes its
  light from the air cell in front of the face, not from the part's own position: an alveary part is
  a full solid block and stores no light, so the position's light is 0 and would draw the clock
  almost black. The clock sprite is Mojang's vanilla art: it is composited only into the static
  block/item texture, while the animated frames are loaded from the vanilla assets at runtime.
* The one thing Forestry does not expose is the counter: `workThrottleCounter` and
  `maxWorkThrottle` are private, and `setWorkThrottle` changes the cycle length rather than
  advancing the current one. `mixin/BeeHousingBeekeepingLogicMixin` therefore adds
  `WorkThrottleAccess` to `BeeHousingBeekeepingLogic` - a getter for the throttle and an add method
  that clamps at it - and the part casts the controller's logic to that interface. If another mod
  replaced the beekeeping logic, the part falls back to `IBeekeepingLogic.DEFAULT_WORK_THROTTLE` and
  simply does nothing. Everything else uses the public alveary and beekeeping API.

Verification: a temporary development-server self-test (removed again afterwards), **33 checks**, all
passing:

* the block, item, block entity type and menu register under `forestry_oddities:alveary_ticker`, and
  the block entity type accepts the block;
* a 3x3x3 alveary of plain blocks with a wooden slab cap assembles with the ticker in an exterior
  cell, and the ticker ends up in the controller's component set;
* the mixin applies to the controller's `BeeHousingBeekeepingLogic` (`instanceof WorkThrottleAccess`)
  and its throttle reads the expected 550;
* the speed clamps to [1, maxWorkThrottle]: 0 becomes 1 and 99 999 becomes 550;
* the buffer takes 10 000 FE per offer, fills to 20 000 on a second offer and refuses extraction;
* at speed 10 one tick adds 9 to the counter and spends 90 FE, a second tick doubles both, and adding
  1 000 ticks at once clamps the counter at the throttle;
* an empty queen slot changes neither the counter nor the buffer, and a buffer with 5 FE buys no tick;
* speed, throttle and buffer survive a `saveAdditional`/`loadAdditional` round trip, and
  `getUpdateTag` carries `Speed`, `MaxSpeed` and `Power` - Forestry's multiblock base already returns
  that tag from `getUpdatePacket`, so the block update sent when the slider moves reaches the client
  without this mod adding a packet of its own;
* the shaped recipe loads and crafts the part.

The client-side half - the screen, the slider drawing, the vanilla clock frames, the model and the
block texture - is client-only. It was compiled, and a temporary client probe (removed afterwards)
then loaded a development world, placed the part on a lit stage and screenshotted the block and its
screen: the clock renders on all four sides with its frames advancing at the setting, the gauge slider
tracks the setting, the clock fills its bezel and the buffer gauge follows the charge. A second probe
drove the screen's mouse entry points the way the mouse handler does - click on the groove, drag to
the right end, drag back, release, then drag again - and logged 1, 550, 205, 205, 205: the knob follows
the held pointer across the whole range and a release stops it, which is the check that caught
vanilla's container screen swallowing drags in the first place. That in-game check also caught a real bug: the quad
was taking the light at the part's own position, which a full solid block stores as 0, so the clock
was drawn almost black. The renderer now reads each face's light from the air cell in front of it
(`LevelRenderer#getLightColor` on the neighbour), and the screenshot shows the clock as bright as the
wall it is on.

### How the parts join the multiblock

All four are `AbstractAlvearyBlockEntity` subclasses in `content/alveary`:

* `AlvearyPartBlock` is the shared block plumbing: Forestry's `BlockStructure` (right-click opens the
  part's menu once assembled, the placer becomes the owner, a part's inventory drops when it breaks)
  plus the anchor-only multiblock ticker and the neighbour check that re-validates the structure.
* The alveary discovers parts by interface while it collects its components: the circuit foundation is
  `IAlvearyComponent.BeeModifier` + `IAlvearyComponent.BeeListener`, and the transmission and the
  ticker are `IAlvearyComponent.Active` (the hook that gives an alveary part a server tick). The circuit foundation's board
  is a Forestry `ISocketable` socket - the interface Forestry's own machines implement - so the board is
  neither a part inventory nor a menu slot: it is saved in the part's own NBT and dropped by
  `AlvearyCircuitFoundationBlock#onRemove`, which is the same thing `BlockBase` does for a Forestry machine.
  No registration with Forestry is needed beyond the block, its block entity and - for the circuit foundation -
  its menu.
* The fourteen circuits and the layout are registered in `integration/ForestryOdditiesPlugin#registerCircuits`,
  which is Forestry's own extension point; the soldering iron therefore knows the layout without this
  mod patching anything.

### Verification

Temporary development-server self-tests (removed again afterwards), all passing. The first run checked
`148` assertions for the whole port - registration, layout and circuits, the programs' numbers, the
buffers, persistence, a real 3×3×3 alveary and the recipes. When the board moved from a menu slot into a
Forestry socket, a second run re-checked everything that touches the board (`46` assertions), and a third
`11`-check run repeated the socket, refusal, buffer, work-cycle, NBT and drop checks against the build
that ships:

* the layout is registered with `forestry_oddities:circuit_foundation` as its socket type, and a board soldered in
  that layout parses back carrying the same socket type - which is exactly the value the circuit foundation
  accepts and no Forestry machine does;
* the part has one socket, it starts empty, its socket type is `CircuitFoundationCircuit.SOCKET_TYPE`, a board
  clicks in and stays a single item, and its programs are read in circuit order (two tubes -> two
  programs, a 30 FE draw);
* a Forestry machine upgrade board and a plain item are both refused and leave the socketed board
  untouched;
* the socket is **not** an item capability, so no pipe or hopper can reach it, while the energy
  capability still takes 500 FE per offer, fills the 1 000 FE buffer, accepts nothing when full and
  refuses extraction;
* one work cycle drains the whole board's draw, a buffer that cannot pay stops at what it could not pay
  and switches the programs off, and the next cycle empties it;
* replacing the board changes the programs and the draw (15 FE for a Hell board), and the modifier
  reports hellish only while 15 FE is in the buffer - so the socketed programs reach the alveary;
* board and charge survive a `saveAdditional`/`loadAdditional` round trip;
* the menu holds only the 36 player slots (the board is no longer a slot), `hasBoard()` follows the
  socket, and charge, capacity and draw still read correctly;
* breaking the part drops exactly one board as an item entity.

The first run's multiblock checks are unaffected by the socket change: the part still joins as
`IAlvearyComponent.BeeModifier` + `BeeListener`, and assembly, modifier chaining, the transmission's
per-tick distribution and the recipes are untouched code paths.

The socket's trip to the client got its own small run, because a socket is not a menu slot and 1.21's
`BlockEntity#getUpdateTag` returns an empty tag - the board was in the save while the client showed an
empty socket, and a second board was then refused because the server's socket was full. `5` assertions on
the shipping build: the board goes into the socket, the chunk update tag carries `Sockets` and `Power`, and
a freshly constructed part that runs `handleUpdateTag` on that tag - the client's own path - sees the board
and reports `hasBoard()`.

One detail the drop check has to account for: item entities only exist in a chunk that is *entity
ticking*, and the test server has no players, so the world part of the test force-loads the test chunk
and checks on tick 20. Without that, `Block.popResource` is never reached with an addable entity and the
check sees an empty world - for a frame housing part, a vanilla dirt drop and a manual `addFreshEntity`
alike, which is how the behaviour was pinned down.

The GUI was verified geometrically and against a rendered mock rather than by screenshot
(`build/preview_gui.py`, a development script, `20/20` assertions): both sheets are byte-identical to
Binnie's files - the window sheet is that mod's own `guistimulator.png`, untouched, so the socket draws
exactly the cell the port has at `(44,38)`-`(59,51)`, and the screen's own constants still mirror the 1.12
original's recess at `(43,36)` with its 16×16 plate at the item origin `(44,37)`; the bar recess is
exactly 72×16 at `(70,37)` with its dark top-left and light bottom-right bevel; the inventory frames start
at `x=8+18k` on the legacy rows 63/81/99/121; and every geometry constant in the Java sources
(`HEADER_HEIGHT=40`, `BORDER=5`, `CONTENT_OFFSET=35`, `IMAGE_HEIGHT=179`, the menu's
`PLAYER_INVENTORY_Y=98`) agrees with those pixels. The script then renders the expanded window with the
gauge at 0 %, 25 %, 60 % and 100 % from the same arithmetic `GuiEnergyBar` uses, so the layout cannot
drift away from the art. Both block textures were verified as exact copies of Binnie's files (SHA-256 of
each shipped PNG equals the source), and both block models resolve to `minecraft:block/cube_all` against
them.

The program table is checked against Forestry's own data instead of being trusted (`build/check_jar.py`):
the fourteen electron tubes the thermionic fabricator can make and the fourteen programs match one for
one, and no program shares an id or a tube - so the *Circuit Foundation* layout owns every tube
Forestry ships. The two names Forestry's language file still carries without an item
(`electron_tube_rubber`, `electron_tube_orchid`) are deliberately left alone. `build/check_lang.mjs`
reads the program ids off the enum and derives the runtime-built tooltip keys from the same file, so a
new program that forgets a translation fails the check instead of printing a raw key in game.

## The Forester's manual (林业员手册条目)

This mod does not ship a book. It adds a chapter to Forestry's own **Forester's Manual**
(`forestry:foresters_manual`) instead: a root category `forestry:oddities` titled *Oddities*
(林业异闻), holding six entries - the two frames and the four alveary parts.

How the pages get into someone else's book:

* Patchouli 1.20 removed the `extend` flag that used to bolt one book onto another, and in
  exchange the content of a book is no longer read from a single namespace: it is collected
  from `assets/<namespace>/patchouli_books/<book>/en_us/{categories,entries}/<id>.json`
  across **every** pack the resource manager knows
  (`BookContentResourceListenerLoader`, keyed on the resource location, so the namespace of
  the path is the namespace of the page id). Shipping
  `assets/forestry/patchouli_books/foresters_manual/...` is therefore the supported way for
  an addon to write into Forestry's book: the manual loads these pages next to Forestry's
  own, none of Forestry's files is replaced, and no second book appears in the item list.
* Entries name their own `category` and `BookEntry#initCategory` adds the entry to the
  category it names, so the category file does not have to list it. That is what makes a new
  chapter possible without touching Forestry's `categories/beekeeping.json`.
* `book.json` sets `"i18n": true`, and Patchouli then reads every `name` and every page text
  as a **translation key** (`Component.translatable` for the names, `I18n.get` for the text
  of a page). The JSON of this chapter therefore holds keys only
  (`forestry_oddities.patchouli.*`), and the prose lives in this mod's own
  `assets/forestry_oddities/lang/en_us.json` and `zh_cn.json`. Other languages fall back to
  English through the vanilla language manager, so no raw key can reach a page.
* Only the `en_us` files are enumerated (`BookContentsBuilder.findFiles`); a client tries the
  selected language first and falls back to `en_us` per file. Since all of the text is in the
  language files, there is no `zh_cn` copy of the chapter JSON at all - one set of pages, two
  languages.

The chapter, in `sortnum` order (the order they are meant to be discovered in):

| Entry | Id | Pages |
|---|---|---|
| Soft Light Frame (柔光框架) | `forestry:oddities/soft_light_frame` | spotlight, recipe, the charge, where it fits |
| Infusion Frame (灌注框架) | `forestry:oddities/infusion_frame` | spotlight, recipe, the five potion rows, the wear |
| Alveary Frame Housing (蜂房组框架外壳) | `forestry:oddities/alveary_frame_housing` | spotlight, recipe, building it in, what the slot does |
| Circuit Foundation (电路巢础) | `forestry:oddities/alveary_circuit_foundation` | spotlight, recipe, how to solder a board, the tubes that give numbers (two pages), the five that answer questions, the rules |
| Alveary Transmission (蜂房组配电) | `forestry:oddities/alveary_transmission` | spotlight, recipe, how it distributes |
| Alveary Ticker (蜂房组进刻器) | `forestry:oddities/alveary_ticker` | spotlight, recipe, the slider, the buffer |

Crafting pages point at the recipe ids the mod ships (`forestry_oddities:<name>`) and are
drawn from the recipe manager, so they cannot drift away from `data/forestry_oddities/recipe/`.
`patchouli:crafting` keeps a recipe only when `Recipe#getType() == RecipeType.CRAFTING`, which
the infusion frame's recipe answers through `CraftingRecipe` even though its serializer is the
mod's own.

The prose is written the way the rest of the manual is written, which was checked against
Forestry's own pages first: the first person of the almanac ("For years this book has said...",
"I never liked breeding that in"), short paragraphs separated by `$(br2)`, numbers spelled into
the sentence ("half again as fast for ten FE a cycle"), `$(item)` and `$(thing)` on names, and
`$(l:...)...$(/l)` links into Forestry's own pages (apiary, soldering iron, circuit board) as
well as into this chapter. **No bullet lists**: `$(li)` appears nowhere in Forestry's language
files (checked in all eleven of them), so a list would have been the one thing in the chapter
that looked foreign, and the fourteen programs are grouped into prose families instead of a
table.

Verification: `tools/check_manual.py`, no problems. The script exists because Patchouli fails
*softly* in every way that matters here - a missing key prints the key, a wrong link target
stops being a link, a wrong recipe id draws an empty page, and a broken JSON or an unclosed
`$(/l)` leaves the **whole manual** at "Error loading and compiling book" - and none of that
fails a build. It re-derives everything Patchouli derives from the file paths and checks:

* the layout (namespace `forestry`, book `foresters_manual`, language `en_us`) that decides
  which page ids the chapter gets, and that no page of ours sits on an id Forestry already
  ships (that would silently replace Forestry's page);
* entry `category`, category `parent` and all `16` `$(l:...)` targets, against Forestry's own
  entries and categories read out of a sibling `ForestryCE` checkout or a jar;
* every `$(...)` command against Patchouli's command set, and that no `$(/l)` outlives its
  `$(l:...)`;
* all `34` translation keys in both languages (a missing `en_us` key is an error, a missing
  `zh_cn` one a warning, since the game falls back to English), and that no manual key is
  left unused;
* every `recipe` / `recipe2` id against the recipe that exists and that it is a crafting
  recipe;
* every icon and spotlight item this mod names against its item model.

The negative cases are the interesting half, so they were tested by mutation: a mis-spelled
link target, an unknown `$(items)` command, a `recipe` id that does not exist, a page whose
key was renamed away, and a page type Patchouli does not know were each injected into the
chapter, each was reported (the last one as a warning, because a future Patchouli may add page
types), and each was reverted. The built jar was inspected as well: `.\gradlew.bat jar` puts
the category and the six entry files under
`assets/forestry/patchouli_books/foresters_manual/en_us/`, unchanged. What none of this checks
is how the pages *look*: they were not opened in a running client here, so the chapter is
verified exactly as far as the files go - every key, link, recipe and id resolved, and nothing
about the drawing assumed.

## Long energy (long 能量适配)

Ported from Career Bees (`CareerBees-CE`), so that a part that needs to move more than an `int` of FE
in one call does not have to invent the plumbing first. The API and the adapters are in place and the
insert side is in use: the Voltaic Bee's effect offers each tick's discharge through `LongEnergy`. No
block of this mod offers a long *source* yet.

### Why

NeoForge's `IEnergyStorage` moves at most `Integer.MAX_VALUE` FE per call, while Modern
Industrialization (EU), Mekanism (Joules), Flux Networks (FE) and GrandPower all carry energy as a
`long` and routinely store more than the int range. `api.energy` is the long-valued view of a block:

| type | role |
|---|---|
| `LongEnergyStorage` | sink: `long receiveEnergy(long maxReceive, boolean simulate)` |
| `LongEnergySource` | source: `extractEnergy` / `getEnergyStored` / `getMaxEnergyStored` - the extraction side, for exposing a buffer to other mods |
| `LongEnergyProvider` | `(Level, BlockPos, Direction) -> LongEnergyStorage`, for energy systems that have no NeoForge capability |
| `LongEnergy` | facade: the `forestry_oddities:long_energy` block capability (`LongEnergy.BLOCK`), the provider registry and `find(...)` |
| `NeoForgeEnergyStorage` | package-private adapter from the int-valued `IEnergyStorage`, used by the FE fallback |

`LongEnergy.find(level, pos, side)` resolves one block in three steps: the
`forestry_oddities:long_energy` capability, then the registered providers in registration order, then
the plain FE capability, wrapped so that a larger amount is offered in int-sized pieces (up to 64 calls
per block and tick, so a storage beyond the int range still takes more than 2.1e9 FE per tick). A
provider that throws is disabled permanently and logged once, so one broken optional integration cannot
break a transfer tick.

### The adapters

`compat/energy/EnergyIntegrations` registers one provider per supported mod, but only when that mod is
installed: each integration class is loaded inside its `ModList.isLoaded` branch, so the mod runs with
none of them, and `libs/compat`'s jars are `compileOnly`. There is no reflection in this path.

| mod id | lookup | conversion |
|---|---|---|
| `modern_industrialization` | `EnergyApi.SIDED` (native EU storage), skipped when the block already exposes FE | FE / `forgeEnergyPerEu` (default 10) |
| `grandpower` (or `modern_industrialization`, which jar-in-jars it) | `ILongEnergyStorage.BLOCK`; wrappers of plain FE storages are skipped so the FE path keeps handling them | 1:1 FE |
| `fluxnetworks` | `FluxCapabilities.BLOCK` (`IFNEnergyStorage`) | 1:1 FE |
| `mekanism` | `Capabilities.STRICT_ENERGY.block()` (`IStrictEnergyHandler`), also retried with a null side because Mekanism registers its machines without one | FE ↔ J via `EnergyUnit.FORGE_ENERGY` |
| `appflux` | the Flux Accessor block entity, or any `PartFluxAccessor` on an AE2 `IPartHost` (AE2 only exposes a part's capability on the cable side it sits on) | 1:1 FE |

The reverse direction is `EnergyIntegrations.registerSourceCapabilities(event, type, sourceOf)`: it
registers a block entity type's `LongEnergySource` as the energy storage of every supported mod, so
their cables can drain it in one long call. Career Bees hard-wired that side to its bee housings; here
it takes a source callback, with a `BiFunction` overload for a per-side answer. (Career Bees refused
the unsided lookup so the display mods would not draw an energy bar on a housing - whether a block
should answer that lookup is the caller's decision now.)

### Wiring

* `ForestryOddities`' constructor calls `EnergyIntegrations.install()` once, after mod discovery.
* `build.gradle` adds the seven API jars in `libs/compat` as `compileOnly` - the same jars Career Bees'
  integrations compile against. They are not on the development runtime, and players need none of
  them. Those jars are third-party binaries, so they are not in the repository (`libs/compat/*.jar` is
  git-ignored): `libs/compat/README.md` lists the seven files, the versions and the classes each bridge
  touches, and a fresh clone has to drop them in before `compileJava` can resolve.
* No block entity registers a `LongEnergySource` yet; the call that would do it is noted in
  `content/ModCapabilities`. The *insert* side is in use: the Voltaic Bee's effect (see the next
  section) offers every tick's discharge through `LongEnergy.find`.

### Verification

`.\gradlew.bat --offline compileJava` and `--offline build` succeed with the port, so every integration
class really compiles against those seven API jars. The development server boots with the mod and no
new errors or warnings (the world-gen `Unprimed heightmap` errors in that log are older than this
change and are in the previous session's log too), and its log has none of the
`Enabled long energy transfer for ...` lines - expected, since none of the five mods is installed on
this machine's development runtime. Real transfers through an MI / Mekanism / Flux Networks cable are
untested: no block of this mod owns a long source yet.

## Voltaic Bee (伏打蜂)

A bee that turns a hive's spare work speed into Forge Energy. The effect and the transfer follow
Career Bees' energy bee (`careerbees:power`); the species, the numbers and the mutation are this
mod's. Design document: `docs/voltaic-bee-design.md`.
Implementation: `content/apiculture/VoltaicBeeEffect`.

### The species

`data/forestry_oddities/bee_species/voltaicus.json` - id `forestry_oddities:voltaicus`, genus
`electrapis` (琥珀 / Electrum), species `voltaicus` (伏打 / Voltaic). Genus data:
`data/forestry_oddities/taxon/electrapis.json`.

| field | value | why |
|---|---|---|
| speed | 0.8, dominant | the discharge is squared in it, so this is the balance knob |
| lifespan / fertility | 40 / 2 | it earns by working, not by living long |
| tolerances | none / none | deliberately fussy: an alveary or a circuit foundation program has to fix it |
| activity / cave / rain | diurnal, no, no | night, a roof and rain all stop the discharge |
| effect | `forestry_oddities:voltaic`, recessive | a hybrid can carry it without working |
| flowers | `forestry_oddities:flower_type_redstone` | it eats redstone: ore, deepslate ore or a block; the emerald program skips the check |
| products | `forestry:honey_comb` 0.30 | it is not a product bee |
| specialties | `minecraft:redstone` 0.20, `minecraft:copper_ingot` 0.08 | static charge, enamelled wire |
| colours | body `0xD97C1E`, stripes `0x5FD8FF`, outline `0x2B1608` | matte copper, arc-blue band |
| territory / pollination | 9/6/9, 15 | the box for pollination and FX only |

No texture, model or client registration: Forestry tints its default bee sprite through the three
colour numbers. `docs/previews/voltaic-bee-tint.png` is the offline preview that picked the stripe
colour (`#9BE8FF` blurred into the fixed cream abdomen band, `#5FD8FF` reads as a separate colour).

### Acquisition

`forestry:anachrone` × `forestry:glacial`, chance **0.04**, gated on one condition in
`data/forestry_oddities/recipe/bee_mutation/voltaic_1.json`: the level has to be in a thunderstorm
(`forestry_oddities:thunderstorm`, a condition type this mod registers because Forestry's seven types
do not cover weather). The condition is the level's weather, so it never passes in the Nether or the
End - and rain stops bees, so the housing has to be rain-proof too: a `tolerates_rain` queen, the
alveary's `SEALED` circuit, or a roof (with the default `rainMode = BIOME`, a housing whose sky light
is below `biomeModeSkyLight` does not count as rained on).

The design's second gate - a lightning rod under the housing, which `forestry:requires_resource`
would have checked by walking down from the housing position and skipping bee housings - was dropped
before shipping: waiting for the storm is the gate, and the rod stays an image in the flavour text.

The flavour: a bee revived out of amber, standing in a polar thunderstorm, with a rod on the floor to
catch what the ice in the clouds rub together. The route is deliberately late: the Anachrone's
parents are the relic bee (sniffer-dug amber fossils, 25% per centrifuge run, drones only) and the
Steadfast bee (dungeon loot, drones only), and the Glacial is depth 7, so both lines have to be worked
before this mutation can even be attempted.

### The effect

`forestry_oddities:voltaic` discharges
`ceil(192 * speed^2 * voltaicBeePowerMultiplier)` FE per tick, where `speed` is the bee's speed allele
times the housing's production modifier. One effect tick is one game tick: Forestry calls the effect
from `BeeHousingBeekeepingLogic` every tick while the housing has no error, so the effect does not
throttle, and it does not check whether the bee may work either. It keeps no state at all.

| effective speed | FE/tick | FE/s |
|---|---|---|
| 0.3 | 18 | 360 |
| 0.6 | 70 | 1 400 |
| **0.8 (own allele)** | **123** | **2 460** |
| 1.0 | 192 | 3 840 |
| 1.6 (one production frame) | 492 | 9 840 |
| 1.7 (fastest Forestry allele) | 555 | 11 100 |
| 3.2 (two frames) | 1 967 | 39 340 |
| 6.4 (three frames) | 7 865 | 157 300 |

The `+1` ceiling quirk is deliberate: `0.8^2` is a hair above `0.64` in binary, so the product is
122.88... and `ceil` gives **123**, not 122. Career Bees' energy bee has the same 400-based formula
and the same quirk; this one is 48 % of its rate at the same speed, and the bee is far cheaper.

### Where the energy goes

1. Every block touching the **outside of the hive's shell**, in flood-fill order, offered the whole
   remaining budget in one call through `api.energy.LongEnergy`, so a sink that carries more than an
   `int` (Modern Industrialization, Mekanism, Flux Networks, ...) gets it through its own long API.
   The shell is the whole multiblock, not the one block the bee sits in: six faces for an apiary, 54
   for a 3x3x3 alveary (9 below, 9 above, 36 around the sides). Each face is reported with the side
   that really touches the shell, so a machine that only takes energy from one face is fed correctly.

There is no second step: the remainder is dropped, so a housing never becomes a battery - run a
cable to the hive, do not come back later for a stored charge.

The alveary's **own parts are neighbours too**. The shell is the set of bee housings that share an
inventory, so a transmission or a circuit foundation standing next to one is just another block with
an energy capability, served in the same pass as a cable. That is what keeps the loop the design
document describes: an alveary with one Voltaic Bee, a transmission and a circuit foundation fills the
transmission's 1 000 FE buffer (500 FE per offer) as if it were a cable, the part hands it to the
foundations once per tick, and 67 650 FE per 550-tick work cycle against a five-program draw of about
100 leaves the rest to whatever else touches the shell.

An electric spark burst follows the rate, one burst every four ticks, from one spark at 123 FE/tick up
to six. No sound: a burst every four ticks is noise rather than information.

### In an apiary versus in an alveary

Forestry's apiary has its own `IBeeModifier` that multiplies the work speed by **0.1**
(`ApiaryBlockEntity.ApiaryBeeModifier`); an alveary does not. The discharge follows the work speed, so
the same bee gives **2 FE/tick** in an apiary and **123 FE/tick** in an alveary. That is Career Bees'
behaviour too, and it is left alone on purpose: the alveary is the housing with the wiring, and the
frames and circuit foundation programs that stack the speed only make sense next to a transmission. Frames
still help in an apiary (three production frames take the effective speed to 0.64 and the discharge to
79 FE/tick), they are just an order of magnitude below the same three frames in an alveary.

### Configuration

`config/forestry_oddities-common.toml`, `[apiculture]`: `voltaicBeePowerMultiplier`, default `1.0`,
range `0.0` - `10.0`, read on every effect tick. `0` switches the generation off without removing the
bee; `2.08` makes it exactly Career Bees' energy bee.

### Verification

A temporary development-server self-test (deleted again after the run) checked `72` assertions on the
build that ships - species data, the effect registration, the mutation, the rate maths, a real apiary
with a real FE sink next to it, and a real assembled 3x3x3 alveary with one of this mod's transmissions
in its wall:

* the development server logs `Loaded 70 bee species` (69 from Forestry plus this one), with no
  `Skipping bee_species` line and no datapack error - so the species file, the taxon file and the
  reference to `forestry_oddities:voltaic` all resolve;
* the species round-trips to the designed genome: speed 0.8 (dominant), lifespan 40, fertility 2, both
  tolerances `NONE`, diurnal, not cave dwelling, no rain tolerance, the mod's own redstone flower type,
  pollination 15, territory 9/6/9, recessive species and recessive effect;
* the three colour numbers and the two products are what the JSON says, the two specialties are
  `minecraft:redstone` and `minecraft:copper_ingot`;
* the effect is registered, recessive and not combinable;
* exactly one mutation leads into the species, `anachrone` × `glacial`, chance 0.04, with one
  condition (`forestry_oddities:thunderstorm`), and the condition type is present in Forestry's
  `forestry:mutation_condition_type` registry;
* the rate maths of the table above: 0.3/0.6/0.8/1.0/1.2/1.4/1.7/1.6 and a multiplier of 2.0 all give
  exactly the listed number, a multiplier of 0, a speed of 0, a negative speed and `NaN` give 0, and an
  infinite speed saturates at `Long.MAX_VALUE` instead of throwing;
* a lone apiary reports six shell faces, and a transmission placed east of it is reported with `west` -
  the face of the transmission that really touches the hive; the apiary's modifier really is 0.1, and
  two effect ticks put exactly 2 and then 4 FE into that transmission's buffer;
* blocking the neighbour makes the discharge go nowhere without throwing, and `push` hands the whole
  budget back untouched;
* a real assembled 3x3x3 alveary with a transmission in its wall reports **54** shell faces, gives the
  bee its own 0.8 speed (123 FE/tick), and one effect tick lands 123 FE in the alveary's own
  transmission even though nothing outside accepts energy - so the self-supply loop works. The
  transmission is reached as one of those 54 neighbours, not by a special case;
* the part's buffer reads back its capacity, and a full buffer refuses the excess, which is then
  dropped like any other unaccepted energy.

Those assertions were run against the build that still had **backflow**, a second settlement step that
poured the remainder into the alveary transmission after the shell walk. Backflow has since been
removed: the transmission is one of the shell's neighbours, so the same 123 FE/tick still lands in it,
but there is no second step, and the "4 123 FE out of 5 000 with 123 FE already stored" case no longer
exists to test.

The mutation route and the flower type were re-verified afterwards (a second temporary self-test,
`25` assertions, same dev server, removed again): one mutation into the species with the `anachrone` ×
`glacial` parents, chance 0.04, exactly one condition - `forestry_oddities:thunderstorm` - that
condition type present in Forestry's `forestry:mutation_condition_type` registry, its description key
in both lang files, the gate returning `0` after `setWeatherParameters(10, 0, false, false)` while
returning the full chance ten ticks into `setWeatherParameters(0, 12000, true, true)`, plus the flower
type: the species names `forestry_oddities:flower_type_redstone`, the manager resolves it, and its tag
accepts a redstone block, redstone ore and deepslate redstone ore while rejecting a poppy. The weather
steps need `doWeatherCycle` switched on and a few ticks for the levels to ramp, which is what the
self-test does; a plain boot logs `Loaded 70 bee species` with no datapack error.

`.\gradlew.bat --offline build` passes, and a plain development-server boot of the shipping jar logs
`Loaded 70 bee species`, `Done`, and no new error or warning: the only remaining log noise is the
world-gen `Unprimed heightmap` error and a missing JourneyMap mixin target, both older than this
change.

## Reaper Bee (死神蜂)

A bee whose special effect is a weapon: every couple of seconds it kills the queen of every bee
housing in its territory. Each death settles the way the game settles any death - a successor princess
and drones, the owner's breeding tracker entry - so what the Reaper takes is a queen and her current
work, not the line. The only queens it spares are its own kind. The species, the numbers, the flower
and the mutation are this mod's. Design document: `docs/reaper-bee-design.md`.
Implementation: `content/apiculture/ReapingBeeEffect`.

### The species

`data/forestry_oddities/bee_species/reaper.json` - id `forestry_oddities:reaper`, genus `styxapis`
(黄泉 / Styx), species `reaper` (死神 / Reaper). Genus data:
`data/forestry_oddities/taxon/styxapis.json`.

| field | value | why |
|---|---|---|
| speed | 1.0, dominant | the reaping period is fixed, so speed only moves its products |
| lifespan / fertility | 30 / 2 | short-lived, mildly fertile: a line you have to keep |
| tolerances | none / none | fussy on purpose: an alveary or a circuit foundation has to fix it |
| activity / cave / rain | nocturnal, yes, no | it reaps by night, underground or not, and a downpour stops it |
| effect | `forestry_oddities:reaping`, recessive | a hybrid can carry the reaping without doing any of it |
| flowers | `forestry_oddities:flower_type_death` | wither rose, soul torch, soul wall torch |
| products | `forestry:mysterious_comb` 0.25 | the ghost bees' comb; it is not a product bee |
| specialties | `minecraft:bone` 0.20, `minecraft:wither_rose` 0.05 | what a harvest leaves behind |
| colours | body `0x6B5876`, stripes `0x4FBF7A`, outline `0x140B1E`, glint on | grey-purple, soulfire green, near-black edge |
| territory / pollination | 15/9/15, 5 | the territory **is** the reaping radius |

No texture, model or client registration: Forestry tints its default bee sprite through the three
colour numbers.

### Acquisition

`forestry:phantasmal` (幻影) × `forestry:demonic` (魔鬼), chance **0.03**, gated on two conditions in
`data/forestry_oddities/recipe/bee_mutation/reaper_1.json`:

* **night** - Forestry's own `forestry:daytime` condition with `"day": false`;
* **a wither rose under the housing** - `forestry:requires_resource` with
  `minecraft:wither_rose`. The condition walks down from the housing position, skips bee housings,
  and compares the first block, so an apiary sits directly on the rose while an alveary needs the rose
  under the whole multiblock (the walk passes through the parts below the alveary's centre).

Both are Forestry's condition types, so no new type had to be registered for this bee. The parents
are as deep as they come - the Phantasmal is `spectral` × `ended` at 0.02, the Demonic is
`sinister` × `fiendish` inside the Nether - and their ideal climates (cold, hellish) do not match, so
the pairing itself is a build problem: an alveary with the climate program, or accept that one of the
two is unhappy. The flower, the mutation and the flavour are one gesture: the Reaper is born standing
on its own food.

### The effect

`forestry_oddities:reaping` runs once every `reaperReapIntervalTicks` (default **40 ticks**, two
seconds). Forestry calls the effect every game tick while the housing reports no error, so the
period is the only throttle the effect adds - and it is read off `Level#getGameTime()` rather than
kept in the effect data, which keeps the effect stateless: there is no counter in the save to go
stale, and every Reaper in a level reaps on the same tick. The bee's own genes do the rest of the
gating: she is **nocturnal**, she is not rain-tolerant, she has no climate tolerance and she wants
wither roses, so a Reaper that is not looked after simply does nothing.

**The area** is the bee's territory, run through the housing's modifiers exactly like
`Bee#getAreaIterator` lays it out for pollination: `[15, 9, 15]`, i.e. 7 blocks to each side and 4 up
and down. Because it is the territory and not a special radius, this mod's own circuit foundation
programs tune it: the **inhibitor** (tin tube) shrinks the reaping to 6x3x6, the **territory**
program (bronze tube) grows it to 22x13x22. Only loaded chunks are searched (chunks are taken with
`getChunkNow`, which never loads one) and the chunk's own block entity map is walked and clipped to
the box, so an empty area costs a few chunk lookups rather than two thousand position lookups.

**Whom it takes**, and whom it does not:

* **any queen** in any bee housing of any mod inside the box. The queen slot also holds the
  *princess* while a pairing is breeding - princesses and drones are not touched, only crowned
  queens;
* **not its own housing**. The inventory of the housing the Reaper lives in is put into the
  already-seen set before the scan starts, which also covers an alveary, whose every part reports the
  same shared inventory;
* **not its own kind**. A queen is kindred when her *active species* is `forestry_oddities:reaper` -
  pure Reapers are immune - or when her *active effect* is `forestry_oddities:reaping`, which also
  protects hybrids that ended up with the Reaper's gift but another species; without that second
  clause, such a hybrid would reap its own species. A bee that merely carries the recessive allele
  without expressing it is ordinary prey;
* **not wild hives** (`IHiveTile`). A wild hive has no queen slot; its inventory fabricates a
  "queen" from the wild bee for display, so writing it back would only replace a cached stack. The
  effect skips them instead of pretending to kill them.

**How it takes them**: her health is set to zero and the housing's own beekeeping logic is asked to
settle the death at once, so Forestry's normal path runs - the successor princess and drones are
spawned into the hive's product slots (queued in the hive if there is no room), the owner's breeding
tracker records them, and the queen slot ends up empty. The chunk is marked changed (an apiary that
has just lost its queen stops working in the check that follows and would otherwise never mark
itself), and the only thing the Reaper leaves behind is twelve soul particles and a wisp of
`SOUL_ESCAPE`. Losing the queen still costs the hive her work cycle and the products she had in
progress; it does not cost it the species.

### Configuration

`config/forestry_oddities-common.toml`, `[apiculture]`, both read on every effect tick:

* `reaperReapIntervalTicks` - default `40`, range `0` - `1200`. `0` switches the reaping off without
  removing the bee or its allele: the server-side kill switch.
* `reaperSparesSameOwner` - default `false`. `false` is the literal bee: every queen in range except
  its own kind, **including the ones of the player who owns the Reaper**. `true` skips a housing whose
  owner is the same non-null player, which turns the bee into a weapon against other players' bees on
  a server and into a harmless pet in singleplayer. Housings with no owner (village apiaries) are
  never "the same owner" and are still reaped.

### Using it (and the warning)

By default a Reaper placed in a bee base **eats that base**: the queens of your own apiaries are in
range and are not kindred. That is intended - it is the strongest bee in the mod, and position is its
only balance - but there are three ways to have one safely: solder the circuit foundation's inhibitor
program (range ×0.4), set `reaperSparesSameOwner = true`, or keep it in its own room. The other
readings of the bee are culling (put an unwanted queen in range and let her die) and PvP (cost an
opponent queens and their current work cycles; the successor princess still rules).

The Reaper also closes the mod's energy loop from the other side: it generates nothing, but its four
dislikes are exactly what the circuit foundation's boolean programs (Night Shift 15, Sealed 10,
Climate 25, Flower Source 35 FE per work cycle) fix, and those programs need a powered alveary. A
Voltaic Bee next door pays for it - 123 FE/tick covers hundreds of Reaper alvearies.

### Verification

The content is data plus one effect class, so it is reviewed by reading and by playing, not by a
test suite:

* **Loads clean.** Start the game with only Forestry: CE and this mod; a datapack error on the
  species, the taxon, the death flower type or either mutation condition prints at load, and the
  species is listed under the Styx genus in the bee book with its own flower type and the reaping
  allele. The designed genome is speed 1.0 (dominant), lifespan 30, fertility 2, both tolerances
  `NONE`, nocturnal, cave dwelling, no rain tolerance, the mod's own death flower type, the reaping
  effect, pollination 5, territory 15/9/15, recessive species, glint on.
* **Exactly one way in.** `data/forestry_oddities/recipe/bee_mutation/reaper_1.json` is the only
  mutation into the species: `phantasmal` x `demonic`, chance 0.03, both conditions on the same
  housing - `MutationConditionDaytime` (night) and `MutationConditionRequiresResource` (a wither rose
  as the foundation under it).
* **The reaping.** Put a Reaper in a working apiary and ring it with apiaries of ordinary bees, one
  directly adjacent, one at x+8 (outside the 7-block half-width), one at y+5 (outside the 4-block
  half-height), plus a princess, a second Reaper and a hybrid whose active effect is the reaping.
  After a work cycle the three outer ones and the kindred ones still hold their bees, the Reaper's
  own housing is untouched, and the in-range queens are gone **and settled the normal way**: the
  product slots hold the offspring princess (drones when she has company), exactly as if the queen
  had died of old age, so the apiary keeps running on the successor. A wild hive left inside the box
  keeps its wild bee, because `IHiveTile` is skipped.
* **The gate.** `reaperReapIntervalTicks` throttles the effect by game time, `0` disables reaping
  while leaving the bee and its allele intact, and `reaperSparesSameOwner` spares only housings whose
  owner is the same non-null player.
* **Not verified yet:** an alveary (the shared inventory should be deduped to one queen), and
  saving/reloading a housing whose queen was taken in the same tick - the design document lists both.

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
python tools/check_manual.py                   # the Forester's manual chapter: ids, links, keys, recipes
python tools/check_recipes.py                  # the same-grid-different-result check
```

`runServer` writes its world, config and logs to `run/`; accept the EULA once with
`echo eula=true > run/eula.txt` (already done here).

## Layout

```
gradle.properties                  versions, mod metadata, Forestry + Create coordinates
build.gradle                       NeoForge/ModDevGradle setup, Forestry + Create dependencies, runs
settings.gradle                    pluginManagement repositories + project name
CHANGELOG.md                       the released versions, newest first (Keep a Changelog)
LICENSE                            the LGPL-3.0, the license this mod is released under
LICENSE.GPL-3.0                    the GNU GPL v3.0, which the LGPL incorporates by reference
src/main/java/com/zhichaoxi/forestryoddities/
    ForestryOddities.java            mod entry point (@Mod): registers config, content and Create gate
    config/ForestryOdditiesConfig.java        common config (rainMode, biomeModeSkyLight,
                                            ignobleDecayStartGeneration, voltaicBeePowerMultiplier,
                                            reaperReapIntervalTicks, reaperSparesSameOwner,
                                            beekeeper.*, backpacks.quickCollect)
    config/RainMode.java           VANILLA / POSITION / BIOME
    tweak/RainChecks.java          the rain rule shared by both mixins
    tweak/IgnobleDecay.java        configurable ignoble ("卑贱") decay threshold
    tweak/BackpackQuickCollect.java  bundle-style right-click stow for every Forestry backpack
    apiculture/BeekeeperEvents.java         attaches the beekeeper goal to villagers (game bus)
    apiculture/BeekeeperGoal.java           the smarter apiary AI (a MoveToBlockGoal)
    apiculture/BeekeeperTending.java        what a beekeeper does inside a housing
    mixin/AbstractBeeHousingBlockEntityMixin.java  apiary/bee house rain check
    mixin/AlvearyControllerMixin.java              alveary rain check
    mixin/BeeHousingBeekeepingLogicMixin.java      exposes the work-cycle counter to the ticker
    mixin/BeeMixin.java                            ignoble decay threshold
    mixin/EventHandlerApicultureMixin.java         keeps Forestry's beekeeper AI out
    content/ModItems.java             the two frames + their creative tab entries
    content/ModDataComponents.java    soft_light_frame_energy (persistent + synced)
    content/ModRecipes.java           the infusion frame recipe serializer
    content/ModCapabilities.java      FE item capability of the soft light frame; frame slot of
                                      the alveary frame housing; FE buffers of the circuit foundation,
                                      the transmission and the ticker (the circuit foundation's board is
                                      a socket, not an inventory, so it has no item capability)
    content/ModBlocks.java            the four alveary part blocks + their block items
    content/ModBlockEntities.java     their block entity types
    content/ModMenus.java             the frame housing, circuit foundation and ticker menu types
    content/SoftLightFrameItem.java   柔光框架: sunlight simulation, 1 MFE buffer, 80 FE/cycle
    content/InfusionFrameItem.java    灌注框架: potion component, durability, tooltip
    content/InfusionModifiers.java    potion effect -> IBeeModifier mapping
    content/InfusionFrameRecipe.java  shaped recipe that copies the potion into the result
    content/alveary/AlvearyPartBlock.java                     shared part plumbing: ticker + neighbour check
    content/alveary/AlvearyFrameHousingBlock.java             蜂房组框架外壳: block, GUI opening, drops
    content/alveary/AlvearyFrameHousingBlockEntity.java       the part: one frame slot, modifier, wear
    content/alveary/FrameHousingModifier.java                 forwards the frame's IBeeModifier
    content/alveary/AlvearyFrameHousingMenu.java              one-slot menu above the player inventory
    content/alveary/AlvearyCircuitFoundationBlock.java        蜂房组电路巢础: block, socket drop, tooltip
    content/alveary/AlvearyCircuitFoundationBlockEntity.java  the part: board socket, 1 kFE buffer, per-cycle draw
    content/alveary/CircuitFoundationModifier.java            folds the board's programs into the alveary
    content/alveary/AlvearyCircuitFoundationMenu.java         socket + FE readouts for the screen
    content/alveary/AlvearyTransmissionBlock.java             蜂房组配电: block, tooltip
    content/alveary/AlvearyTransmissionBlockEntity.java       the part: buffer + per-tick distribution
    content/alveary/AlvearyTickerBlock.java                   蜂房组进刻器: block, tooltip
    content/alveary/AlvearyTickerBlockEntity.java             the part: speed, work-cycle progress, 20 kFE buffer
    content/alveary/AlvearyTickerMenu.java                    the ticker's speed slider menu
    content/alveary/WorkThrottleAccess.java                   the counter bridge the mixin adds to Forestry's logic
    content/alveary/AlvearyPartEnergy.java                    the receive-only FE buffer the powered parts use
    content/alveary/circuit/CircuitFoundationProgram.java   the fourteen programs (each an IBeeModifier)
    content/alveary/circuit/CircuitFoundationCircuit.java   one program as a Forestry circuit + the layout
    content/alveary/circuit/CircuitFoundationCircuits.java  circuit board -> mounted programs
    content/apiculture/VoltaicBeeEffect.java  伏打放电: the Voltaic Bee's effect (rate, shell
                                              push, sparks)
    content/apiculture/ReapingBeeEffect.java  收魂: the Reaper Bee's effect (period, territory scan,
                                              kindred check, queen removal)
    client/ForestryOdditiesClient.java          charged item model property, potion tint, ticker screen and
                                                renderer (client only)
    client/AlvearyFrameHousingScreen.java       the frame housing GUI (client only)
    client/AlvearyCircuitFoundationScreen.java  the circuit foundation GUI: socket widget, charge bar, status line
                                                (client only)
    client/AlvearyTickerScreen.java             the ticker GUI: brass clock housing, speed row, readout (client only)
    client/AlvearyTickerRenderer.java           the running vanilla clock on the four exposed sides (client only)
    client/TickerSpeedSlider.java               the speed gauge widget: brass knob on a grey groove (client only)
    client/TickerClock.java                     the vanilla clock frame rate shared by the screen and renderer (client only)
    integration/ForestryOdditiesPlugin.java  Forestry plugin (ServiceLoader): the circuit foundation's circuit
                                             layout + circuits, and the two bee effects
    integration/create/CreateIntegration.java      Create hook-up (RegisterEvent)
    integration/create/BeeItemAttributes.java      attribute type ids + the unit-attribute factory
    integration/create/BeeSpeciesAttribute.java    "is of species X" / "carries X recessively"
    integration/create/BeeLifeStageAttribute.java  "is a Queen" and friends
    integration/create/BeeStockAttribute.java      "is pristine/ignoble stock"
    integration/create/BeeGenomeAttribute.java     one gene per type: activity, flower type, effect,
                                                   tolerances, speed, lifespan, each expressed and
                                                   recessive
    integration/create/BeeProductAttribute.java    "produces X" / "has the specialty X"
    integration/create/Bees.java                   bee/stack helpers over the Forestry API
    api/energy/LongEnergy.java                      long 能量 facade: capability, provider registry, find()
    api/energy/LongEnergyStorage.java               the long sink interface
    api/energy/LongEnergySource.java                the long source interface (the extraction side)
    api/energy/LongEnergyProvider.java              provider hook for energy APIs without a capability
    api/energy/NeoForgeEnergyStorage.java           int-valued FE storage as a long sink (the FE fallback)
    compat/energy/EnergyIntegrations.java           per-mod providers + source capability registration
    compat/energy/EnergyMath.java                   saturating multiply and clamping
    compat/energy/ModernIndustrializationIntegration.java  MI EU provider (FE / forgeEnergyPerEu)
    compat/energy/ModernIndustrializationSourceStorage.java a LongEnergySource as an MI EU storage
    compat/energy/GrandPowerIntegration.java        GrandPower long FE provider
    compat/energy/GrandPowerSourceStorage.java      a LongEnergySource as a GrandPower long storage
    compat/energy/FluxNetworksIntegration.java      Flux Networks long FE provider
    compat/energy/FluxNetworksSourceStorage.java    a LongEnergySource as a Flux Networks storage
    compat/energy/MekanismIntegration.java          Mekanism Joules provider (FE <-> J)
    compat/energy/MekanismSourceStorage.java        a LongEnergySource as a strict energy handler
    compat/energy/AppliedFluxIntegration.java       the Applied Flux Flux Accessor (block or AE2 part)
src/main/resources/
    forestry_oddities.mixins.json     mixin config (listed in neoforge.mods.toml)
    META-INF/neoforge.mods.toml    mod metadata + dependencies + mixin declarations
    META-INF/services/forestry.api.plugin.IForestryPlugin
    assets/forestry_oddities/lang/en_us.json, zh_cn.json   filter + item + block strings, and the
                                                          chapter's prose (forestry_oddities.patchouli.*)
    assets/forestry/patchouli_books/foresters_manual/en_us/categories/oddities.json  the chapter this
                                                                                    mod adds to Forestry's manual
    assets/forestry/patchouli_books/foresters_manual/en_us/entries/oddities/*.json   its six entries
                                                                                    (two frames, four parts)
    assets/forestry_oddities/models/item/soft_light_frame.json       unlit lamp in the frame, override on charge
    assets/forestry_oddities/models/item/soft_light_frame_on.json    lit lamp in the frame
    assets/forestry_oddities/models/item/infusion_frame.json         frame + liquid, liquid on layer1 (tinted)
    assets/forestry_oddities/textures/item/soft_light_frame.png      proven frame + unlit lamp window
    assets/forestry_oddities/textures/item/soft_light_frame_on.png   proven frame + lit lamp window
    assets/forestry_oddities/textures/item/infusion_frame.png        proven frame, opening left empty
    assets/forestry_oddities/textures/item/infusion_frame_overlay.png  the liquid that fills the opening, tinted by the potion
    assets/forestry_oddities/blockstates/alveary_frame_housing.json  the part's block state
    assets/forestry_oddities/models/block/alveary_frame_housing.json the part's block model
    assets/forestry_oddities/models/item/alveary_frame_housing.json  the part's item model
    assets/forestry_oddities/textures/block/alveary_frame_housing.png the part's side texture
    assets/forestry_oddities/textures/gui/alveary_frame_housing.png   the part's GUI background
    assets/forestry_oddities/blockstates/alveary_ticker.json         the ticker's block state
    assets/forestry_oddities/models/block/alveary_ticker.json        the ticker's block model
    assets/forestry_oddities/models/item/alveary_ticker.json         the ticker's item model
    assets/forestry_oddities/textures/block/alveary_ticker.png       the alveary face with the vanilla clock sprite
    assets/forestry_oddities/textures/gui/alveary_ticker.png         the ticker's GUI background
    data/forestry_oddities/recipe/soft_light_frame.json              the soft light frame recipe
    data/forestry_oddities/recipe/infusion_frame.json                infusion recipe (custom type)
    data/forestry_oddities/recipe/alveary_frame_housing.json         the frame housing recipe
    data/forestry_oddities/recipe/alveary_ticker.json                the ticker recipe
    data/forestry_oddities/advancement/recipes/misc/*.json           recipe-book unlocks (frames)
    data/forestry_oddities/advancement/recipes/building_blocks/alveary_frame_housing.json
    data/forestry_oddities/advancement/recipes/building_blocks/alveary_ticker.json
    data/forestry_oddities/loot_table/blocks/alveary_frame_housing.json   the part's block drop
    data/forestry_oddities/loot_table/blocks/alveary_ticker.json          the ticker's block drop
    data/forestry_oddities/tags/item/potions.json                     potion tag (normal/splash/lingering)
    data/forestry_oddities/taxon/electrapis.json                     the Voltaic Bee's genus (琥珀 / Electrum)
    data/forestry_oddities/bee_species/voltaicus.json                the Voltaic Bee (伏打蜂)
    data/forestry_oddities/flower_type/flower_type_redstone.json     its datapack flower type (红石)
    data/forestry_oddities/tags/block/flowers/redstone.json          what counts as that flower
    data/forestry_oddities/recipe/bee_mutation/voltaic_1.json        anachrone x glacial + thunderstorm
    data/forestry_oddities/taxon/styxapis.json                       the Reaper Bee's genus (黄泉 / Styx)
    data/forestry_oddities/bee_species/reaper.json                   the Reaper Bee (死神蜂)
    data/forestry_oddities/flower_type/flower_type_death.json        its datapack flower type (死亡)
    data/forestry_oddities/tags/block/flowers/death.json             what counts as that flower
    data/forestry_oddities/recipe/bee_mutation/reaper_1.json         phantasmal x demonic + night + wither rose
    data/minecraft/tags/block/mineable/axe.json                    axes mine the part faster
    pack.mcmeta
tools/
    check_recipes.py               shaped-recipe grid collisions across the pack
    check_manual.py                the Forester's manual chapter: page ids, links, keys, recipes
libs/compat/                       compileOnly API jars, not in the repository (see its README.md)
```

## Mod properties

`mod_id=forestry_oddities`, `mod_name=Forestry Oddities`, group `com.zhichaoxi.forestryoddities`, license
LGPL-3.0 (the same license as ForestryCE, see [License](#license)). Rename these in `gradle.properties`
and the package names if you want different identifiers.

## License

Forestry Oddities is released under the **GNU Lesser General Public License, version 3.0** (LGPL-3.0,
SPDX `LGPL-3.0`). That is the same license Forestry: Community Edition uses, and the one the code it
builds on comes with: this mod compiles and runs against Forestry's API, and it redistributes
material derived from Binnie's Mods / Extra Bees (nine of the circuit foundation's programs and its GUI
art, see the credits in `docs/curseforge-description.md`), which is LGPL-3.0 as well.

| File | Contents |
|---|---|
| `LICENSE` | The full text of the LGPL-3.0, unmodified. |
| `LICENSE.GPL-3.0` | The full text of the GNU GPL v3.0, unmodified. The LGPL-3.0 is a set of additional permissions on top of the GPL v3.0 (§0), and it requires a copy of *both* documents to accompany the work (§3b, §4b), so both are shipped. |

Both files are also packaged into `META-INF/` of the mod jar and of the sources jar
(`build.gradle`, "License files"), so a jar taken out of a modpack carries its own license.
`gradle.properties`' `mod_license=LGPL-3.0` is the same statement for
`META-INF/neoforge.mods.toml`, i.e. for the in-game mod list:

```toml
# META-INF/neoforge.mods.toml, from gradle.properties' mod_license
license = "LGPL-3.0"
```

What that means in practice:

* You may use, study, modify and redistribute this mod, including in modpacks and monetised videos,
  as long as the LGPL is kept (source available, notices intact, modifications marked).
* Bundling it into a modpack is conveying an unmodified copy, which the license allows without
  asking. A link back is appreciated, not required.
* No warranty: the mod comes `AS IS`, as §15/§16 of the GPL say.
