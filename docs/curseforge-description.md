# Forestry Oddities

**Oddities and quality-of-life adjustments for Forestry: Community Edition.**

Forestry: CE gives you bees. It does not give the alveary a frame slot, a way to power an alveary, a way to sort bees on an automated factory floor, or a bee that turns a queen's working speed into energy. Forestry Oddities adds exactly those missing pieces — small, self-contained, and written the way Forestry would have written them itself.

Nothing is taken away: Forestry's own blocks, frames, circuits and bees keep working exactly as before. Every change is either **new content** (two frames, three alveary parts, two bees) or a **config-gated tweak**, implemented with a handful of Mixins instead of a patched Forestry build.

## Requirements

|  |  |
|---|---|
| **Minecraft** | 1.21.1 |
| **Loader** | NeoForge 21.1.230 or newer |
| **Required** | Forestry: Community Edition 3.0.0-alpha8 or newer (tested against 3.0.0-alpha9) |
| **Required** | Patchouli (Forestry itself requires it, so it must be installed) |
| **Optional** | Create 6.0.4+ — unlocks 25 bee attributes in the Attribute Filter |
| **Optional** | JEI — recipe lookups |
| **Side** | Client & server |
| **License** | LGPL-3.0 |

## Contents

* Create Attribute Filter support for bees — 25 attributes
* Three tweaks — rain detection, ignoble decay, beekeeper villagers
* Two apiary frames — Soft Light Frame / 柔光框架, Infusion Frame / 灌注框架
* Three alveary parts — Frame Housing / 蜂房组框架外壳, Circuit Foundation / 蜂房组电路巢础, Transmission / 蜂房组配电
* Two bees — the Voltaic Bee / 伏打蜂, which discharges FE, and its opposite the Reaper Bee / 死神蜂, which takes the queens of every housing in its territory

---

## Create Attribute Filter support

Create's **Attribute Filter** matches items by their *properties* instead of their item ID — which is exactly what a bee sorter needs, because all bee species share the same four items (`drone_bee`, `princess_bee`, `queen_bee`, `larvae_bee`) and differ only inside the genome data component.

This mod registers **25 attributes** for bees:

| Group | What you can filter on |
|---|---|
| Species | active species, and the **recessive** species (what its offspring can still turn into) |
| Life stage | drone, princess, queen, larvae |
| Stock | pristine / ignoble |
| Activity | diurnal, nocturnal, crepuscular, metaturnal, and each recessively |
| Flower type | Flowers, Cacti, Jungle, Nether, Redstone, … and each recessively |
| Effect | Creeper, Radioactive, Flammable, Voltaic, … and each recessively |
| Tolerances | temperature (Up/Down/Both N) and humidity, plus their recessive alleles |
| Speed / lifespan | the allele, plus its recessive allele |
| Genes | “can work underground” (cave dwelling) and “works in the rain” |
| State | analyzed / not analyzed, mated / not mated |
| Products | “produces Honey Comb”, “has the specialty Redstone” |

Usage is Create's: right-click an Attribute Filter, drop a bee into the reference slot, pick an attribute from the dropdown, add it (or its inverted variant), and the filter works in every Create filter slot — brass and andesite funnels, smart chutes, brass tunnels, deployers and so on.

**Examples**

* *“is of species Imperial”* in a brass funnel sorts an apiary's output by species.
* *“is a Queen”* accepts queens of every species while rejecting drones and princesses.
* *“is ignoble stock”* on a funnel into a lava-filled chute culls ignoble lines and keeps the pristine ones.
* *“has Nocturnal activity” + “is a Queen”* sends the night shift to separate housing.
* *“carries Forest recessively”* finds the princesses that look Imperial but still throw Forest offspring — clean a line up before it mutates back.
* *“is not analyzed”* routes fresh bees to the analyzer before they are filed.

Good to know: everything is read from the stack itself, so unanalyzed bees work (analysis only affects the tooltip), and the client offers exactly what the server matches on. A bee with no genome — a villager trade wildcard — resolves to the default species. The `_recessive` attributes match every bee that *carries* the allele, expressed or not (a homozygous bee matches both), so combine one with its inverted sibling when a filter must exclude a value on both alleles.

---

## Tweaks

### Rain is weather, not a dimension-wide flag

Forestry 2.10+ decides that a housing is “raining” from the **whole dimension's** weather plus a sky light check. The result: an apiary under a roof, built underground, or standing in a desert still counts as rained on, and bees without `TOLERATES_RAIN` refuse to work there.

This mod makes the check look at the housing's position again. `apiculture.rainMode` selects the rule:

| Mode | Rule | Roofed housing | Desert / snow | Cave |
|---|---|---|---|---|
| `BIOME` *(default)* | dimension weather **and** a rain-producing biome **and** sky light ≥ `biomeModeSkyLight` | raining | dry | dry |
| `POSITION` | `Level#isRainingAt(pos)` — the test wild beehives use | dry | dry | dry |
| `VANILLA` | upstream behaviour | raining | raining | dry |

`BIOME` keeps upstream's intent (a housing under a canopy still counts as rained on) while removing the part that was simply wrong: deserts, snowy biomes and caves are no longer “raining”. The separate “cannot see the sky” check is untouched — those housings still need a cave-dwelling bee or a sunlight-simulating frame. Values are re-read on every check, so an edit applies without a restart.

### A configurable ignoble decay

An ignoble queen never becomes pristine again, and Forestry eventually kills the line off: past a hard-coded generation, a queen can die out instead of leaving a princess. `apiculture.ignobleDecayStartGeneration` replaces that base value — the random 0–10 generation jitter and the 2% chance stay exactly as Forestry defines them:

| Value | Effect |
|---|---|
| `96` *(default)* | Forestry's behaviour |
| `N` | decay can start at generation `N+1` … `N+11` |
| `0` | decay can start right away |
| `-1` | ignoble queens never die out |

Pristine bees are unaffected, and the setting also applies to queens that already carry a generation counter.

### Beekeeper villagers that actually work

Forestry's beekeeper AI assumes the apiary sits *north of and one block above* the block the villager walks to, only recognises bee houses (never apiaries), only targets housings whose inventory is *not* empty — so the empty apiary that needs stocking is the one it ignores — pairs any princess with any drone (creating hybrid queens), hovers for minutes, and will happily empty a player's own apiary.

This mod replaces it with a goal that:

* finds a housing on **any** side of the villager, apiary or bee house;
* treats an **empty** housing as the normal case and stocks it with a princess and a matching drone;
* requires the pair to be of the **same species** (configurable) and prefers the housing's own product bees;
* only walks over when there is real work, and respects the villager's sleeping and meeting schedule;
* leaves a player-placed housing alone (configurable), and leaves a living queen untouched;
* never targets an alveary — its inventory belongs to a multiblock controller.

Options: `beekeeper.enabled`, `beekeeper.tendPlayerOwned`, `beekeeper.requireSameSpecies`, `beekeeper.searchRadius` (4–32, Forestry uses 16).

---

## Frames

Both are ordinary `IHiveFrame`s: they sit in the apiary's three frame slots, wear out per **work cycle** (550 ticks) exactly like Forestry's frames, and appear in the `forestry:apiculture` creative tab right after Forestry's own frames. In an alveary, put them in the Frame Housing below.

### Soft Light Frame / 柔光框架

```
  L
 CFC
  I
```

`L` redstone lamp · `C` copper ingot · `I` iron ingot · `F` proven frame

A sunlight simulation. Forestry refuses to let a queen work when the housing cannot see the sky, unless the bee has `CAVE_DWELLING` or a modifier reports that sunlight is simulated. This frame answers yes, so an apiary underground, in a cave or under a solid roof works **without breeding cave-dwelling into the line**. It contributes no other modifier — production, territory and aging stay exactly as they are.

It is not free:

* Holds up to **1 000 000 FE**, and every work cycle costs **80 FE** (about 12 500 cycles, i.e. roughly 95 hours of a never-idling apiary).
* Newly crafted frames are **empty**; the buffer is exposed as a NeoForge item energy storage that accepts charge from any FE charger (receive only — nothing can pull it back out).
* **No durability at all**: it never wears out and is never destroyed. Running dry only switches the sunlight simulation off.
* The icon is a proven frame with a redstone lamp set into its opening and lights up while charged; the item bar is the charge, and the tooltip prints `FE / capacity`, whether sunlight is simulated, and the per-cycle cost.

### Infusion Frame / 灌注框架

```
 SSS
 BPB
 SSS
```

`S` stick · `B` blaze powder · `P` any potion (regular, splash or lingering)

The frame carries its potion, and the potion *is* the frame — vanilla's `potion_contents` component gives it both numbers:

* **Durability = the potion's duration in seconds.** A 3:00 potion gives a 180-use frame, 8:00 gives 480.
* **Modifiers = the potion's effects**, read live from the stored potion — see the table below.

| Potion | Modifier |
|---|---|
| Speed | production speed ×2 per level |
| Poison | aging ×10 per level (the queen loses lifespan that much faster) |
| Regeneration | cancels aging entirely |
| Weakness | mutation chance ×10 per level (Weakness II is already guaranteed) |
| Water Breathing | sealed housing: bees work in the rain without `TOLERATES_RAIN` |

Multi-effect potions (custom or splash) get every row, multipliers of the same effect stack, and Regeneration wins over Poison. A potion with none of those effects leaves everything neutral — the frame never zeroes a number out.

The icon is the same proven-frame silhouette with its window filled by liquid, tinted with the potion's colour exactly like a vanilla potion item, so you can see at a glance what is inside. The tooltip lists the modifier lines and the remaining durability in Forestry's own words.

---

## Alveary parts

Forestry's alveary has **no frame slots** — the Forester's Manual says as much — and no electricity. These three wall parts fix both. Build them into the exterior of an assembled 3×3×3 alveary like Forestry's own heater or sieve (not the top layer, not the interior), and break them to get their contents back.

### Alveary Frame Housing / 蜂房组框架外壳

```
 PSP
 SXS
 PSP
```

`P` any planks · `S` stick · `X` alveary block

Gives the alveary the frame slot Forestry never gave it. Right-click the assembled part for a one-slot GUI; only hive frames fit — Forestry's untreated, impregnated, proven and creative frames as well as this mod's two. The frame's modifiers are folded into the **whole** alveary (production, mutation, aging, territory, pollination, genetic decay, sealed, always active, sunlight, hellish, flowers, climate), it wears out per work cycle like in an apiary, and a NeoForge item handler exposes the slot to pipes and hoppers.

Because the alveary has none of the apiary's `0.1x` production penalty, a proven frame doubles alveary production where the same frame takes an apiary from `0.1x` to `0.2x`.

### Alveary Circuit Foundation / 蜂房组电路巢础

```
 KIK
 IXI
  T
```

`K` refined circuit board · `I` gold nugget · `X` alveary block · `T` golden electron tube

Turns **FE** into bee modifiers by running a programmed circuit board. Take a Forestry **Soldering Iron**, select the *Circuit Foundation* circuit layout, and each electron tube means one program:

| Tube | Program | Effect | FE / cycle |
|---|---|---|---|
| Iron | Production Up I | production ×1.5 | 10 |
| Diamantine | Production Up II | production ×2.5 | 20 |
| Lapis | Flowering Up I | pollination ×1.5 | 10 |
| Bronze | Territory Up I | territory ×1.5 | 10 |
| Tin | Production Down I | territory ×0.4, production ×0.9 | 10 |
| Golden | Mutation Up I | mutation ×1.5 | 15 |
| Apatine | Lifespan Up I | aging ÷1.5 | 10 |
| Obsidian | Lifespan Down I | aging ÷0.8 | 10 |
| Blazing | Hell | simulates a hellish climate | 15 |
| Copper | Rainproof | bees work in the rain | 10 |
| Silicon | Sunlight | simulates daylight | 15 |
| Ender | Night Shift | works day and night | 15 |
| Amber | Climate Control | ignores temperature and humidity | 25 |
| Emerald | Flower Source | provides flowers without a flower bed | 35 |

* **1 000 FE buffer**, receive only, charged through a NeoForge block energy capability (any cable or charger works). The **whole board is paid at once** once per work cycle — a cheap program cannot ride along free next to an expensive one — and the programs only apply while the buffer can pay the sum.
* **Genetic decay ×1.5 per program**, so a four-program board raises decay to about 5×. A circuit foundation is never free even when its programs look harmless.
* The board goes into a Forestry **socket**: click it in with the board on your cursor, take it back out with a soldering iron (which loses one durability point). Only boards programmed in the circuit foundation layout fit, no pipe or hopper can reach the socket, and breaking the part drops the board.

### Alveary Transmission / 蜂房组配电

```
  G
 GXG
  G
```

`G` tin gear · `X` alveary block

A power distributor — it **never touches the bees**. It buffers 1 000 FE and hands it to the other parts of the assembled alveary **once per tick** through the energy capability, splitting the 500 FE-per-tick output evenly over the parts that can receive. So an alveary full of circuit foundations needs **one cable** instead of one per block face. A transmission never feeds another transmission (the one behavioural fix over the original implementation, which had that infinite-bounce hole), it only walks the multiblock while it actually holds energy, and it has no GUI — right-clicking opens the alveary's own screen.

---

## The Voltaic Bee / 伏打蜂

`for.genus.bees.electrapis` (Electrum) · `forestry_oddities:voltaicus`

*Copper wings and a hum you can feel in your teeth. It feeds on redstone; it does not make honey, it makes current. — Beekeeping 101*

A bee that turns a hive's spare work speed into **Forge Energy**, using this mod's own alveary wiring to get the power out.

**Getting one.** Anachrone × Glacial, chance **4%**, and only in a **thunderstorm**. The housing has to be rain-proof too (a rain-tolerant queen, sealed housing, or — with the default rain mode — a roof), and both parent lines are late-game: the Anachrone comes from relic bees and the Glacial is depth 7.

**What it makes.** A honey comb (30%), with redstone (20%) and copper ingots (8%) as specialities.

**The effect.** Voltaic Discharge yields `ceil(400 × speed²)` FE per tick, times `apiculture.voltaicBeePowerMultiplier`, where speed is the bee's speed allele times the housing's production modifier — and it is squared, so frames are what actually move the number:

| Where | Speed | FE/tick | FE/s |
|---|---|---|---|
| Apiary | 0.08 (`0.8 × 0.1` apiary penalty) | 3 | 60 |
| Alveary | 0.8 (own allele) | 257 | 5 140 |
| Alveary + one Production Up I program (`× 1.5`) | 1.2 | 577 | 11 540 |
| Alveary + one production frame (`× 2`) | 1.6 | 1 025 | 20 500 |
| Alveary + three production frames (`× 8`) | 6.4 | 16 385 | 327 700 |

The figures are the game's own arithmetic, not rounded for the table: `0.8f × 0.8f` lands a hair above 0.64, which is why the plain alveary row reads 257 and not 256.

**Where the energy goes.** Every block touching the *outside of the hive's shell*, in flood-fill order — six faces for an apiary, 54 for a 3×3×3 alveary — offered through the long-energy layer so sinks bigger than an `int` get it in one call. That is the whole list: the remainder is dropped, and a housing is not a battery, so run a cable to it. An alveary's own parts are neighbours too — the Transmission or a Circuit Foundation sitting in the wall is fed by the same pass as a cable would be.

That closes the loop the design calls for: one Voltaic Bee, a Transmission and a Circuit Foundation in the same alveary fills the Transmission's buffer, and the part hands it on to the programs that keep the bee working (about 141 350 FE per work cycle against a five-program draw of roughly 100), with everything above that leaving the hive as free power.

The species is deliberately fussy — no temperature or humidity tolerance, diurnal, no cave dwelling, no rain tolerance, redstone for flowers — so an alveary and its programs are what make it good. Config: `apiculture.voltaicBeePowerMultiplier` (0–10, default 1.0; `0` switches generation off without removing the bee).

---

## The Reaper Bee / 死神蜂

`for.genus.bees.styxapis` · `forestry_oddities:reaper`

*It gathers no nectar; it collects debts. Within its reach, no other queen sees a second dusk. — Beekeeping 101*

A bee whose product is absence: every couple of seconds it kills the queen of every bee housing in its territory.

**Getting one.** Phantasmal × Demonic, chance **3%**, only at **night**, and only with a **wither rose under the housing**. Both parents are deep lines (the Phantasmal is Spectral × Ended at 2%, the Demonic is bred in the Nether) and their climates disagree, so the pairing is a build problem in itself.

**What she makes.** A mysterious comb (25%), with bone (20%) and wither roses (5%) as specialities.

**The effect — Soul Reaping.** The area is the bee's own territory (15 × 9 × 15 by default: 7 blocks to each side, 4 up and down), run through the housing's modifiers — an alveary's territory parts and this mod's own circuit foundation programs, including the inhibitor that shrinks it to 6 × 4 × 6. Every queen in that box dies, and each death settles the normal way: her health is set to zero and Forestry's own beekeeping logic produces the successor princess and drones. So a Reaper costs a queen and her current work cycle, not the line. Princesses and drones are untouched, and a Reaper never touches its own housing, wild hives, or its own kind (Reaper queens, and hybrids whose active effect is the reaping). There is no defence other than position.

**The cost.** She reaps only while she can work, and she is deliberately high-maintenance: nocturnal, not rain-tolerant, no climate tolerance, and her flowers are wither roses and soul torches. The four circuit foundation programs that fix exactly those four things (Night Shift 15, Sealed 10, Climate 25, Flower Source 35 FE per work cycle) turn a Reaper alveary into a machine that has to be fed — which is what the Voltaic Bee next door is for.

**Careful.** By default the Reaper also eats *your* base: every queen in range except her own kind. Three ways to keep one safely: the inhibitor program, the configuration option below, or a room of her own.

Config: `apiculture.reaperReapIntervalTicks` (default 40 ticks, 0–1200, `0` disables reaping without removing the bee) and `apiculture.reaperSparesSameOwner` (default off; on = housings of the same player are skipped).

---

## Energy compatibility

The Voltaic Bee's discharge goes through a long-valued energy layer (`forestry_oddities:long_energy`), which falls back to plain FE when nothing else answers. If any of these mods is installed, their own energy API is used automatically — no configuration, no reflection, no required dependency:

Modern Industrialization (EU) · Mekanism (Joules) · Flux Networks · GrandPower · Applied Flux / AE2

---

## Configuration

`config/forestry_oddities-common.toml`, all under `[apiculture]` except the beekeeper block. The `[apiculture]` values are re-read on use, so an edit applies without restarting (client and server each read their own copy); the `[beekeeper]` settings `enabled` and `searchRadius` are read when a villager's goal is created, so those reach existing beekeepers only after they reload.

| Key | Default | Meaning |
|---|---|---|
| `apiculture.rainMode` | `BIOME` | `BIOME`, `POSITION` or `VANILLA` rain detection |
| `apiculture.biomeModeSkyLight` | `10` | sky light needed for `BIOME` to count as rained on (0–15) |
| `apiculture.ignobleDecayStartGeneration` | `96` | base generation of the ignoble decay test, `-1` disables it |
| `apiculture.voltaicBeePowerMultiplier` | `1.0` | output multiplier of the Voltaic Bee (0–10) |
| `apiculture.reaperReapIntervalTicks` | `40` | how often a Reaper reaps, in ticks (0–1200); `0` disables reaping |
| `apiculture.reaperSparesSameOwner` | `false` | leave the housings of the Reaper's own owner alone |
| `beekeeper.enabled` | `true` | replace Forestry's beekeeper AI |
| `beekeeper.tendPlayerOwned` | as generated | whether beekeepers may tend player-placed housings |
| `beekeeper.requireSameSpecies` | `true` | only pair a princess with a drone of the same species |
| `beekeeper.searchRadius` | `16` | horizontal search radius in blocks (4–32) |

---

## Notes and limits

* Built and tested against **Forestry: Community Edition 3.0.0-alpha9**; it also loads with `alpha8`. Forestry is still in alpha, so expect upstream changes to need an update here.
* **No Create needed** — without it the mod loads and works, only the attribute filters are gone. Same for JEI. Everything happens behind a mod-loaded check, so no Create class is ever touched when Create is absent.
* The alveary parts must go into an **assembled** alveary's exterior; they cannot go into the top layer or the interior. While the structure is incomplete, right-clicking prints the pattern error like Forestry's own parts.
* The Frame Housing holds **one** frame. The bee house still has no frame slot, in Forestry or here.
* Forestry's sources are not patched: the tweaks are Mixins on Forestry's own methods plus this mod's own goal, so a locally modified Forestry build is never involved.
* Recipes: all five new recipes (two frames, three alveary parts) ship recipe-book unlock advancements. Both bee mutations are ordinary Forestry mutations and show up in JEI like any other.

## Credits

* **Forestry: Community Edition** by thedarkcolour and contributors — the mod this is built on.
* **Binnie's Mods / Extra Bees** — the Electrical Stimulator and Alveary Transmission are ports of Binnie's parts: nine of the fourteen circuit programs, the 500 FE-per-offer buffers and the per-tick distribution are Binnie's. Binnie's own GUI art comes with them — `guistimulator.png` (the circuit foundation window) and `craftgui-controls.png` (the energy bar) are redistributed verbatim under the same LGPL-3.0 terms. The three block textures are this mod's own, not Binnie's.
* **Career Bees** — the long-energy layer and the energy-bee effect this mod's Voltaic Bee follows.
* **Create** — the Attribute Filter API the bee sorting is built on.

## Links

* Source and issue tracker: [github.com/zhichaoxi2006/Forestry-Oddities](https://github.com/zhichaoxi2006/Forestry-Oddities)
* License: **GNU Lesser General Public License v3.0 (LGPL-3.0)** — the full text is the repository's `LICENSE`, and it also ships inside the jar at `META-INF/LICENSE`, together with the GNU GPL v3.0 it incorporates by reference (`LICENSE.GPL-3.0` / `META-INF/LICENSE.GPL-3.0`).
* Permission is granted to use this mod in modpacks, videos and streams, including monetised ones. A link back is appreciated, not required.

---

# 中文介绍

**林业社区版（Forestry: Community Edition）的异想补遗。**

林业社区版给了你蜜蜂，却没有给蜂房组框架槽、没有给蜂房组供电的方式、没有让自动化工厂按基因分拣蜜蜂的手段，也没有一只把蜂后工作速度变成电能的蜜蜂。本模组补的就是这几块——每一样都自成一体，并且按林业自己的做法去实现。

**没有拆掉任何东西**：林业原有的方块、框架、电路与蜜蜂行为全部保持原样。所有改动要么是**新内容**（两个框架、三个蜂房组部件、两只新蜂），要么是**可配置的微调**（用少量 Mixin 实现，而不是修改林业源码）。

## 需求

|  |  |
|---|---|
| **Minecraft** | 1.21.1 |
| **加载器** | NeoForge 21.1.230 或更高 |
| **必需** | 林业社区版 3.0.0-alpha8 或更高（在 3.0.0-alpha9 上测试） |
| **必需** | Patchouli（林业自身依赖它，必须安装） |
| **可选** | Create 机械动力 6.0.4+ —— 解锁属性过滤器里的 25 种蜜蜂属性 |
| **可选** | JEI —— 配方查询 |
| **端** | 客户端与服务端 |
| **许可** | LGPL-3.0 |

## 内容一览

* Create 属性过滤器支持 —— 25 种蜜蜂属性
* 三项微调 —— 雨天判定、卑贱种衰退、养蜂人村民
* 两个蜂房框架 —— 柔光框架、灌注框架
* 三个蜂房组部件 —— 框架外壳、蜂房组电路巢础、蜂房组配电
* 两只新蜂 —— 伏打蜂，放电产出 FE；死神蜂，摘掉活动范围内所有蜂房的蜂后

---

## Create 属性过滤器支持

Create 的**属性过滤器**按物品的*属性*而不是物品 ID 匹配，这正是蜜蜂分拣需要的——所有蜂种共用四个物品（雄蜂、公主蜂、蜂后、幼虫），区别只在基因组数据组件里。

本模组注册了 **25 种**蜜蜂属性：

| 类别 | 可按此筛选 |
|---|---|
| 品种 | 显性品种，以及**隐性**品种（后代还能变成什么） |
| 生命阶段 | 雄蜂、公主蜂、蜂后、幼虫 |
| 血统 | 始祖种 / 卑贱种 |
| 作息 | 昼行、夜行、晨昏、全时，以及各自的隐性版本 |
| 采蜜对象 | 花朵、仙人掌、丛林、下界、红石…… 以及各自的隐性版本 |
| 特殊效果 | 苦力怕、辐射、易燃、伏打…… 以及各自的隐性版本 |
| 耐受 | 温度（上/下/双向 N）、湿度，以及隐性等位基因 |
| 速度与寿命 | 对应等位基因，以及隐性版本 |
| 基因 | “可在地下工作”（穴居性）、“可在雨天工作”（耐雨飞行性） |
| 状态 | 已鉴定 / 未鉴定、已交配 / 未交配 |
| 产物 | “生产蜂蜜蜂窝”、“特产为红石” |

用法就是 Create 的用法：右键属性过滤器，把一只蜜蜂放进左侧参考槽，从下拉菜单中选一项属性并添加（或添加其反转版本），之后这个过滤器可用于所有 Create 过滤槽位——黄铜漏斗、安山漏斗、智能溜槽、黄铜隧道、机械手等等。

**实用例子**

* 黄铜漏斗上加“是帝国品种”，蜂房产物自动按品种分箱。
* “是蜂后”让漏斗接受所有品种的蜂后，同时拒绝雄蜂和公主蜂。
* 通往灌满岩浆的溜槽的漏斗加“是卑贱种”，清理卑贱血统、保留始祖种。
* “作息为夜行” + “是蜂后”，把夜班单独安置。
* “隐性品种为森林”，找出看起来是帝国、后代却还能出森林的公主蜂。
* “未鉴定”，让新蜂先进分析仪再归档。

须知：所有属性都从物品堆本身读取，所以**未鉴定**的蜜蜂同样有效（鉴定只影响提示文本），客户端列出的就是服务端匹配的。没有基因组的蜂（村民交换的未知蜂）按默认品种处理。`_recessive` 系列匹配所有**携带**该等位基因的蜜蜂（纯合子两边都匹配），要同时排除两个等位基因上的某个值时，把它和反转版本一起用。

---

## 微调

### 雨天判定：看位置，而不是整个维度

林业 2.10 之后用**整个维度**的天气加天光亮度来判定蜂房“是否下雨”。结果是有顶棚的、建在地下的、种在沙漠里的蜂房都算被雨淋，没有耐雨基因的蜜蜂就拒绝工作。

本模组让判定重新看蜂房所在位置。`apiculture.rainMode` 选择规则：

| 模式 | 规则 | 有顶棚 | 沙漠 / 雪原 | 洞穴 |
|---|---|---|---|---|
| `BIOME` *（默认）* | 维度天气 **且** 生物群系会下雨 **且** 天光 ≥ `biomeModeSkyLight` | 算下雨 | 干燥 | 干燥 |
| `POSITION` | `Level#isRainingAt(pos)`，野外蜂巢用的判定 | 干燥 | 干燥 | 干燥 |
| `VANILLA` | 上游行为 | 算下雨 | 算下雨 | 干燥 |

`BIOME` 保留了上游的本意（树冠下的蜂房仍算被雨淋），同时去掉了显然错误的部分：沙漠、雪原和洞穴不再“下雨”。“看不到天空”那套独立判定完全不动——这类蜂房依旧需要穴居基因或模拟阳光的框架。数值每次判定都重新读取，改完无需重启。

### 可配置的卑贱种衰退

卑贱种蜂后永远无法变回始祖种，林业最终会让这条血统断绝：世代数超过硬编码阈值后，蜂后可能不再产出公主蜂而直接死亡。`apiculture.ignobleDecayStartGeneration` 替换这个基准值，而 0–10 的随机偏移和 2% 的概率保持林业原样：

| 取值 | 效果 |
|---|---|
| `96` *（默认）* | 林业原行为 |
| `N` | 衰退可从第 `N+1` … `N+11` 代开始 |
| `0` | 立刻可能开始衰退 |
| `-1` | 卑贱种蜂后永不断绝 |

始祖种不受影响，该设置对已经带有世代计数的蜂后同样生效。

### 真正会干活的养蜂人村民

林业的养蜂人 AI 假设蜂房在村民所走向方块的*北面一格上方*，只认简易蜂房（完全不认蜂房），只对*非空*的容器行动，于是最需要补充蜂的**空蜂房**反而被忽略；它会把任意公主蜂和任意雄蜂配成杂交蜂后，每次会在那里磨蹭几分钟，还会把玩家自己的蜂房搬空。

本模组用一个新的目标替换它，它会：

* 从村民**任意一侧**寻找蜂房或简易蜂房；
* 把**空蜂房**当作正常情况，放入一只公主蜂和一只同品种雄蜂；
* 要求配对**同品种**（可配置），并优先使用蜂房自己产出的蜂；
* 只在**确实有活可干**时走过去，并遵守村民的睡眠与集会时间表；
* 不动玩家放置的蜂房（可配置），也不打扰正在产出的蜂后；
* 不把蜂房组当目标——它的容器属于多方块控制器。

选项：`beekeeper.enabled`、`beekeeper.tendPlayerOwned`、`beekeeper.requireSameSpecies`、`beekeeper.searchRadius`（4–32，林业用 16）。

---

## 框架

两者都是普通框架物品：能放进蜂房的三个框架槽，按**工作周期**磨损（550 tick），与林业框架完全一致，并出现在 `forestry:apiculture`（养蜂）创造模式页林业框架之后。在蜂房组里，放进下面的框架外壳即可。

### 柔光框架

```
  红石灯
铜锭 标定框架 铜锭
  铁锭
```

模拟阳光。林业不允许看不到天空的蜂房产出，除非蜜蜂带穴居基因或有修正声明“已模拟阳光”——这个框架属于后者，所以地下、洞穴或实心屋顶下的蜂房**不必再往血统里配穴居基因**。它不提供其他任何修正，产量、活动范围、老化都保持原样。

但并非免费：

* 容量 **1 000 000 FE**，每个工作周期消耗 **80 FE**（约 12 500 个周期，不空闲的蜂房约 95 小时）。
* 新合成的框架是**空的**；能量缓冲以 NeoForge 物品能量能力暴露，任何 FE 充电器都能充（只进不出）。
* **完全没有耐久**：不会磨损，也不会损坏。耗尽只是关闭阳光模拟。
* 图标是嵌了红石灯的标定框架，充能时会亮；物品耐久条显示电量，提示框显示 `能量 / 容量`、是否在模拟阳光，以及每周期消耗。

### 灌注框架

```
木棍 木棍 木棍
烈焰粉 药水 烈焰粉
木棍 木棍 木棍
```

框架携带药水，药水**就是**框架本身——原版 `potion_contents` 组件同时给出了两个数值：

* **耐久 = 药水的持续秒数**。3:00 的药水给出 180 次使用的框架，8:00 给出 480 次。
* **修正 = 药水的效果**，每次查询都从保存的药水实时读取——见下表。

| 药水 | 修正 |
|---|---|
| 速度 | 工作速度 ×2/等级 |
| 中毒 | 老化 ×10/等级，蜂后寿命消耗成倍加快 |
| 再生 | 完全取消老化 |
| 虚弱 | 突变概率 ×10/等级（虚弱 II 已是必定突变） |
| 水下呼吸 | 密封蜂房：无耐雨基因也能在雨天工作 |

多效果药水（自定义或喷溅）会拿到全部对应行，同种效果叠加，再生优先于中毒。表里一个效果都不含的药水则保持全部中性——框架从不会把数值清零。

图标沿用标定框架轮廓，窗口里灌满液体，并按药水颜色染色（和原版药水物品一致），一眼就能看出里面装了什么。提示框按林业自己的措辞列出修正行和剩余耐久。

---

## 蜂房组部件

林业的蜂房组**没有框架槽**——林业手册里也这么写——也没有电。下面三个壁面部件把这些补齐。像林业自己的蜂房组加热器或筛网那样，放进已组装好的 3×3×3 蜂房组的**外壳**（不是顶层，也不是内部），破坏即可取回内容物。

### 蜂房组框架外壳

```
木板 木棍 木板
木棍 蜂房组 木棍
木板 木棍 木板
```

给蜂房组补上林业从未给过的框架槽。组装完成后右键打开单槽界面，只接受框架——林业的未处理、浸渍、标定、创造框架，以及本模组的两个框架。框架的修正会作用于**整个**蜂房组（产量、突变、老化、活动范围、授粉、基因衰退、密封、昼夜工作、模拟日照、地狱、花源、气候），按工作周期磨损，并可通过 NeoForge 物品能力接入管道与漏斗。

蜂房组本身没有蜂房那种 `0.1x` 产量惩罚，所以“标定框架”让蜂房组产量翻倍，而同一个框架放在蜂房里只是把 `0.1x` 变成 `0.2x`。

### 蜂房组电路巢础

```
先进电路板 金粒 先进电路板
金粒 蜂房组 金粒
     金电子管
```

消耗 **FE**，通过运行已编程电路板把电力变成蜜蜂修正。拿一把林业**电烙铁**，选择 *电路巢础* 电路布局，之后每种电子管就代表一个程序：

| 电子管 | 程序 | 效果 | FE / 周期 |
|---|---|---|---|
| 铁 | 产量提高 I | 产量 ×1.5 | 10 |
| 钻石 | 产量提高 II | 产量 ×2.5 | 20 |
| 青金石 | 花朵催生 I | 授粉 ×1.5 | 10 |
| 青铜 | 范围扩大 I | 活动范围 ×1.5 | 10 |
| 锡 | 产量减少 I | 活动范围 ×0.4，产量 ×0.9 | 10 |
| 金 | 增加突变 I | 突变 ×1.5 | 15 |
| 磷灰石 | 延长寿命 I | 老化 ÷1.5 | 10 |
| 黑曜石 | 降低寿命 I | 老化 ÷0.8 | 10 |
| 烈焰 | 地狱 | 模拟地狱气候 | 15 |
| 铜 | 防雨 | 雨天照常工作 | 10 |
| 硅 | 模拟日照 | 模拟日光 | 15 |
| 末影 | 昼夜工作 | 昼夜不停 | 15 |
| 琥珀 | 气候控制 | 无视温度与湿度 | 25 |
| 绿宝石 | 花源 | 无需花圃即提供花朵 | 35 |

* **1 000 FE 缓冲**，只进不出，通过 NeoForge 方块能量能力充电（任何线缆或充电器都行）。**整块电路板一次性结算**，每工作周期一次——便宜的电路不能搭着贵的一起白嫖——缓冲不够支付总和时，程序全部不生效。
* **每个程序基因衰退 ×1.5**，四程序的电路板约 5×。电路巢础从不会真正“免费”，即使程序看起来无害。
* 电路板插在林业**插座**里：手上拿着电路板点击装入，拿**电烙铁**点击取回（消耗 1 点耐久）。只接受在电路巢础布局下编程的电路板，管道和漏斗都无法接触插座，破坏方块会掉出电路板。

### 蜂房组配电

```
     锡齿轮
锡齿轮 蜂房组 锡齿轮
     锡齿轮
```

纯粹的电力分配器，**完全不碰蜜蜂**。它缓冲 1 000 FE，每 tick 通过能量能力输送给已组装蜂房组的其他部件，把每 tick 500 FE 的输出均分给能接收的部件——所以一整组蜂房组只需要**一根线**，而不是每个面一根。配电永远不会给另一个配电供电（相对原始实现的唯一行为修正，原实现存在无限互推的漏洞），只在确实有能量时才遍历多方块，也没有界面——右键打开的是蜂房组自身的界面。

---

## 伏打蜂

琥珀属（`for.genus.bees.electrapis`）· `forestry_oddities:voltaicus`

*铜翼，和一种能从牙根里听见的嗡鸣。它以红石为口粮，不酿蜜，只发电。——《养蜂入门指南》*

把蜂后剩余的工作速度变成**能量（FE）**的蜜蜂，并用本模组自己的蜂房组线路把电送出去。

**获得方式**：`错时蜂 × 冰河蜂`，概率 **4%**，并且只在**雷雨天**发生。蜂房还必须是防雨的（耐雨蜂后、密封蜂房，或默认雨天模式下有天棚），两条亲本血统都属于后期内容。

**产物**：30% 蜂蜜蜜脾，特产为 20% 红石与 8% 铜锭。

**效果**：`伏打放电` 每 tick 产出 `ceil(400 × 工作速度²)` FE，再乘以 `apiculture.voltaicBeePowerMultiplier`；工作速度是蜂的速度等位基因乘以蜂房的产量修正——而且是平方关系，所以真正把数字拉上去的是框架与程序：

| 位置 | 工作速度 | FE/tick | FE/秒 |
|---|---|---|---|
| 蜂房 | 0.08（`0.8 × 0.1` 蜂房惩罚） | 3 | 60 |
| 蜂房组 | 0.8（自身等位基因） | 257 | 5 140 |
| 蜂房组 + 一个产量提高 I 程序（`× 1.5`） | 1.2 | 577 | 11 540 |
| 蜂房组 + 一个产量 ×2 的框架（`× 2`） | 1.6 | 1 025 | 20 500 |
| 蜂房组 + 三个产量 ×2 的框架（`× 8`） | 6.4 | 16 385 | 327 700 |

表里的数字是游戏自己的算法结果，没有为表格取整：`0.8f × 0.8f` 略大于 0.64，所以纯蜂房组一行是 257 而不是 256。

**电能去向**：先按洪泛顺序喂给贴着**蜂房外壳外侧**的每一个方块——蜂房 6 个面，3×3×3 蜂房组 54 个面——通过 long 能量层以单次调用交给任何可以接收的储能（超出 int 范围的储能也能一次吃饱）；就这些，剩下的直接丢弃：蜂房不是电池，请把线接过去。蜂房组自己的部件同样是“邻居”：墙里的配电、电路巢础和外面接的电缆走同一次推送。

这就闭合了设计想要的循环：同一组蜂房组里放一只伏打蜂、一个配电和一个电路巢础，配电作为邻居拿到电、再由它派发给维持这只蜂的程序（约 141 350 FE/工作周期，对五程序约 100 FE 的消耗），其余全部作为外送电力。

这个蜂种被刻意设计得挑剔——没有温度与湿度耐受、昼行、不穴居、不耐雨、以红石为采蜜对象——所以蜂房组和它的程序才是让它变强的关键。配置项：`apiculture.voltaicBeePowerMultiplier`（0–10，默认 1.0；设为 `0` 可关闭发电而不删除蜜蜂）。

---

## 死神蜂

黄泉属（`for.genus.bees.styxapis`）· `forestry_oddities:reaper`

*它不采蜜，只收账：方圆之内，别的蜂后活不过第二个黄昏。——《养蜂入门指南》*

一只“产物就是缺席”的蜜蜂：每隔两秒，杀掉它活动范围内每一座蜂房的蜂后。

**获得方式**：`幻影蜂 × 魔鬼蜂`，概率 **3%**，只在**夜晚**、且**蜂房正下方有一朵凋灵玫瑰**时发生。两条亲本血统都很深（幻影蜂是幽灵蜂 × 末影蜂的 2%，魔鬼蜂在下界育成），而且两者的理想气候并不一致，所以配对本身就是一道建造题。

**产物**：25% 神秘蜜脾，特产为 20% 骨头与 5% 凋灵玫瑰。

**效果——收魂**：范围就是蜂自己的领地（默认 15×9×15：左右各 7、上下各 4），会经过蜂房的修正——蜂房组的领地部件、本模组电路巢础的领地/抑制剂程序（抑制剂把范围压到 6×4×6）。范围内**每一只蜂后**都会死亡，而且是**正常死亡结算**：生命值归零后由林业自己的养蜂逻辑产出继承的公主与雄蜂。所以死神消耗的是“一任蜂后 + 她当前的工作周期”，不是血脉。公主与雄蜂本身不受影响；死神不碰自己的蜂房、不碰野巢，也不碰同类（死神蜂后，以及活跃效果为收魂的杂交蜂后）。除了“别放在范围内”，没有别的防御手段。

**代价**：只在能干活时收魂，而且刻意难伺候——夜行、不耐雨、无气候耐受、吃凋灵玫瑰与灵魂火把。恰好解决这四件事的电路巢础程序（昼夜 15、密封 10、气候 25、花源 35 FE/工作周期）会让死神蜂房组变成一台要供能的机器——这就是隔壁伏打蜂的用处。

**小心**：默认配置下死神**也吃自己主人的蜂场**——范围里除了同类，谁的蜂后都摘。想安全养有三条路：抑制剂程序、下面的配置项，或者单独一间房。

配置项：`apiculture.reaperReapIntervalTicks`（默认 40 tick，0–1200，`0` 关闭收魂但不删种）与 `apiculture.reaperSparesSameOwner`（默认关；打开后同一玩家的蜂房会被跳过）。

---

## 能量兼容

伏打蜂的放电走一层 long 值能量接口（`forestry_oddities:long_energy`），无人应答时回退到普通 FE。安装下列任意模组时会自动使用它们自己的能量 API——无需配置、不使用反射、也不是必需依赖：

现代工业化 Modern Industrialization（EU）· 通用机械 Mekanism（J）· 通量网络 Flux Networks · GrandPower · 应用通量 Applied Flux / AE2

---

## 配置

文件 `config/forestry_oddities-common.toml`，除养蜂人段落外都在 `[apiculture]` 下。`[apiculture]` 的数值在使用时读取，改完无需重启（客户端与服务端各读自己的一份）；`[beekeeper]` 里的 `enabled` 与 `searchRadius` 只在村民生成行为目标时读取，改完要等村民重新加载才生效。

| 键 | 默认 | 含义 |
|---|---|---|
| `apiculture.rainMode` | `BIOME` | `BIOME` / `POSITION` / `VANILLA` 雨天判定 |
| `apiculture.biomeModeSkyLight` | `10` | `BIOME` 模式判定为被雨淋所需的天光（0–15） |
| `apiculture.ignobleDecayStartGeneration` | `96` | 卑贱种衰退测试基准代，`-1` 关闭 |
| `apiculture.voltaicBeePowerMultiplier` | `1.0` | 伏打蜂输出倍率（0–10） |
| `apiculture.reaperReapIntervalTicks` | `40` | 死神蜂收魂间隔（tick，0–1200，`0` 关闭） |
| `apiculture.reaperSparesSameOwner` | `false` | 跳过与死神蜂同一主人的蜂房 |
| `beekeeper.enabled` | `true` | 替换林业养蜂人 AI |
| `beekeeper.tendPlayerOwned` | 以生成文件为准 | 养蜂人是否可照料玩家放置的蜂房 |
| `beekeeper.requireSameSpecies` | `true` | 只配同品种的公主蜂与雄蜂 |
| `beekeeper.searchRadius` | `16` | 水平搜索半径（4–32） |

---

## 注意事项

* 针对**林业社区版 3.0.0-alpha9** 构建与测试，`alpha8` 亦可运行。林业仍处于 alpha，上游改动可能需要本模组跟进。
* **不需要 Create**：未安装时模组正常加载与运行，只是没有属性过滤器；JEI 同理。所有相关代码都在“模组已加载”判断之后，未安装时不会触碰任何 Create 类。
* 蜂房组部件必须放进**已组装**蜂房组的外壳，不能放顶层或内部。结构不完整时右键会像林业自己的部件一样提示图案错误。
* 框架外壳只容纳**一个**框架。简易蜂房至今仍没有框架槽（林业如此，这里也如此）。
* 不修改林业源码：微调是打在林业自身方法上的 Mixin 加本模组自己的目标，不涉及任何本地修改版林业。
* 全部五个新配方（两个框架、三个蜂房组部件）都带配方书解锁进度；两种蜜蜂突变都是标准林业突变，会在 JEI 中像其他突变一样显示。

## 致谢

* **林业社区版 Forestry: Community Edition** —— 作者 thedarkcolour 与所有贡献者，本模组的基础。
* **Binnie's Mods / Extra Bees** —— 电刺激器与蜂房组配电移植自 Binnie 的部件：十四个电路程序中的九个、每次 500 FE 的进料上限与每 tick 的分发逻辑都出自 Binnie。Binnie 自己的界面素材一并沿用——`guistimulator.png`（电路巢础窗口）与 `craftgui-controls.png`（能量条）按同样的 LGPL-3.0 条款原样再分发；三个方块贴图为本模组自有素材，并非 Binnie 的贴图。
* **Career Bees** —— long 能量层，以及伏打蜂所参照的能量蜂效果。
* **Create 机械动力** —— 蜜蜂分拣所依赖的属性过滤器 API。

## 链接

* 源码与问题反馈：[github.com/zhichaoxi2006/Forestry-Oddities](https://github.com/zhichaoxi2006/Forestry-Oddities)
* 许可：**GNU Lesser General Public License v3.0（LGPL-3.0）** —— 完整条文见仓库根目录的 `LICENSE`，jar 内 `META-INF/LICENSE` 也带一份；同时附上其所引用的 GNU GPL v3.0 全文（`LICENSE.GPL-3.0` / `META-INF/LICENSE.GPL-3.0`）。
* 允许在整合包、视频与直播（包括营利内容）中使用本模组；标注来源是加分项，不是义务。
