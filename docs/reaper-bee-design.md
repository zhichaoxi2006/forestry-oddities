# 死神蜂 / Reaper Bee — 设计稿

> **状态：已实现（待审查）。** 代码在 `content/apiculture/ReapingBeeEffect`，数据在
> `data/forestry_oddities/{taxon,bee_species,flower_type,tags,recipe/bee_mutation}`，配置项与两份语言已落地。
> `.\gradlew.bat --offline build` 通过；开发服务器 `Loaded 71 bee species` 无 datapack 报错。
> 没有随包发布的自动化测试，§14 是给审查用的验证清单（读文件 + 游戏内自测）。
> 本稿是给审查用的：§17 列出由我自行决定的全部事项，哪一条要改，改完同步本节与 README 即可。
> 基线：Minecraft 1.21.1 / NeoForge 21.1.230 / Forestry: CE `3.0.0-alpha9`。
> 同族参考：`docs/voltaic-bee-design.md`（伏打蜂）。

---

## 1. 定位

一句话：**一只把"周围的蜂后"当作口粮的蜂 —— 它不生产任何东西，它只让别的蜂房停产。**

* 特殊效果「收魂」：每个结算周期，把活动范围内**每一座蜂房的蜂后杀掉**。死亡按**正常结算**走——蜂后生命值归零，由蜂房自己的 beekeeping logic 产出继承的公主与雄蜂、记入育种记录（仓满则先排队），所以死神拿走的是"这一任蜂后和她手头的工作"，不是这一族的血脉。
* 唯一的豁免是"同类"：死神蜂后本身，以及任何**正在表达**死神基因（收魂效果）的蜂后。
* 其余全部重新设计：分类学、基因组、产物、突变路线、花型、外观、语言、配置，以及"这东西到底给谁用、怎么用才不后悔"的玩法说明（§11）。

## 2. 与既有蜂种的对照

| | 伏打蜂（本模组） | 死神蜂（本设计） |
|---|---|---|
| 效果 id | `forestry_oddities:voltaic` | `forestry_oddities:reaping` |
| 效果内容 | 每 tick 把 FE 推给蜂房外壳邻居 | 每 40 tick 清掉范围内所有异种蜂后 |
| 判定范围 | 外壳一圈（不读 territory） | **territory 围出的立方体**（15×9×15） |
| 对别的蜂房 | 无影响 | 杀掉蜂后，但按正常死亡结算（公主 + 雄蜂照出） |
| 自身要求 | 日行、见天、怕雨、无耐受 | **夜行**、可不见天、怕雨、无耐受、吃凋灵玫瑰 |
| 产物 | 蜂窝 + 红石 + 铜 | 神秘蜂窝 + 骨头 + 凋灵玫瑰 |
| 获取 | 错时 × 冰河，0.04 + 雷雨 | 幻影 × 魔鬼，0.03 + **夜晚 + 蜂房下方凋灵玫瑰** |
| 显性 | 效果隐性 | 效果隐性（杂交通常带着收魂却不表达，稳定收魂要维持纯血） |

两只蜂在玩法上互为镜像：伏打蜂**供给**电力，死神蜂**消耗**（它自己一点电也不发，却要吃电路巢础的四五个程序）。§11 会把这条闭环写实。

## 3. 标识与命名

| 内容 | id / key | 中文 | English |
|---|---|---|---|
| 属（taxon） | `styxapis` | 黄泉 | Styx |
| 种 | `forestry_oddities:reaper` | 死神 | Reaper |
| 效果 | `forestry_oddities:reaping` | 收魂 | Soul Reaping |
| 属名 key | `for.genus.bees.styxapis` | 黄泉 | Styx |
| 种名 key | `allele.forestry.bee_species.forestry_oddities.reaper` (+ `.desc`) | 死神 | Reaper |
| 效果名 key | `allele.forestry.bee_effect.forestry_oddities.reaping` (+ `.desc`) | 收魂 | Soul Reaping |
| 花型 key | `allele.forestry.flower_type.forestry_oddities.flower_type_death` | 死亡 | Death |

**翻译键规则**照抄伏打蜂稿 §3：跨命名空间时是 `allele.<染色体ns>.<染色体path>.<对象ns>.<对象path>`，所以种名键里 `forestry_oddities.reaper` 两段都要有；效果 id 不带 `bee_effect_` 前缀（染色体名已经在键里）。

命名理由：

* 属名 `styxapis` = 希腊神话的冥河 **Styx** + `apis`（蜂）。既符合 FCE 的 `electrapis` / `finapis` / `punapis` 构词，也把"黄泉"这个地点意象留给属，把"死神"这个身份留给种。
* 种名 `reaper` 是林业惯用的英文形容词/名词路线（`imperial` / `industrial` / `demonic` / `heroic`），中文译"死神"；拼出来的物品名是**死神蜂后 / 死神雄蜂 / 死神蜂**，英文 Reaper Queen / Drone / Princess，不会超长。
* 效果名「收魂 / Soul Reaping」：它不收蜜、不收钱，收的是魂。选 `reaping` 而非 `reaper` 是为了让效果 id 与种 id 不撞脸（两者虽然不在同一注册表，但日志和图鉴里会并排出现）。

## 4. 特殊效果：收魂

### 4.1 触发时机

FCE 的 `BeeHousingBeekeepingLogic#queenWorkTick` **每 game tick** 调一次 `doEffect`，并且只在蜂房无错误时调（没蜂后、没花、下雨、气候超限、睡觉……都不进）。因此效果本身只需要：

```java
if (level.getGameTime() % period != 0) return storedData;   // period = reaperReapIntervalTicks
```

* `period` 默认 **40 tick（2 秒）**，配置范围 `0–1200`，**0 = 关闭收魂但不删种**（服务器开关）。
* 不写 `IEffectData`、不存 NBT：效果无状态，存档里不会残留计数器。副作用是同一维度的所有死神会在同一 tick 一起收魂——读起来像"收魂时刻"，而不是随机噪声。
* 触发与否完全交给 FCE 的错误判断：**夜行**（白天睡）、**怕雨**、**无气候耐受**、**要凋灵玫瑰**，一条都不会被效果重复实现。

### 4.2 判定范围 = territory

```
area  = housing 修正.modifyTerritory(genome, genome.getActiveValue(TERRITORY))
min   = housing.getBlockPos().offset(-x/2, -y/2, -z/2)
max   = min.offset(x-1, y-1, z-1)
```

* 与 `Bee#getAreaIterator` 完全同构：范围就是这只蜂"能授粉、能放特效"的那块地。
* 死神自己的 territory 是 **[15, 9, 15]** —— 左右各 7 格、上下各 4 格。这个数字是刻意选的：一座标准蜂房组（3×3×3）+ 两座相邻蜂房都在里面，而"隔一条路"的邻居基地刚好在边缘。
* `IBeeModifier#modifyTerritory` 会照常生效，所以本模组自己电路巢础的两个程序直接就是**收魂半径的调节器**：
  * 抑制剂（`INHIBITOR`，锡电子管，10 FE/周期）：范围 **×0.4** → 6×3×6，可以把死神养在别人基地旁边；
  * 领地（`TERRITORY`，青铜电子管，10 FE/周期）：范围 ×1.5 → 22×13×22。
  这是"自己的部件解决自己的问题"的又一次落地，不再额外加配置项。
* 实现只遍历**已加载区块**（`ChunkSource#getChunkNow`，绝不加载新区块），并且直接遍历区块的 block entity 表，再按立方体裁剪，所以空地上一次结算只是几个区块查表，不是 2 000 次 `getBlockEntity`。

### 4.3 杀谁，不杀谁

| 对象 | 处理 | 理由 |
|---|---|---|
| 范围内任何蜂房的**蜂后** | 杀 | 本蜂的全部功能 |
| 蜂房里的**公主**（含正在配对的那只） | 不杀 | 用户说的"蜂后"；公主还不是蜂后 |
| **雄蜂** | 不杀 | 同上；而且蜂房里雄蜂可以再放 |
| **同类**：`active species == forestry_oddities:reaper` | 不杀 | "不杀同类"的第一层：死神蜂后彼此免疫 |
| **同类**：`active effect == forestry_oddities:reaping` | 不杀 | 第二层：杂交出来的"别的种、却带着收魂"的蜂后也免疫；否则它会杀自己那一族 |
| **自己所在的蜂房** | 不杀 | 效果永远不会掀自己的桌子；蜂房组的共享库存整体排除（多部件只算一次） |
| **野生蜂巢**（`IHiveTile`） | 不杀 | 野巢没有蜂后槽，只有一只野蜂；它的 `getQueen()` 是临时拼出来的展示品，写回去也只是覆盖缓存。收魂不碰野巢 |
| 同一主人的蜂房 | **默认照杀**，可配置豁免 | 见 §4.5 与 §10：默认按字面执行"范围内所有蜂后"，服务器可以打开 `reaperSparesSameOwner` |

> 「不杀同类」的两种读法都实现了：按**种**（死神）、按**基因**（正在表达收魂）。只带隐性收魂等位基因、自己并不收魂的蜂，对死神来说就是普通蜂，照杀。

### 4.4 怎么杀（正常死亡结算）

```java
IIndividual individual = IIndividualHandlerItem.getIndividual(queen);
if (!(individual instanceof IIndividualLiving living)) skip;

living.setHealth(0);                       // 1. 生命值归零
individual.saveToStack(queen);             //    写回物品，蜂房下一次读取就是"已死"
target.getBeekeepingLogic().canWork();     // 2. 立刻让蜂房按正常死亡结算
level.blockEntityChanged(target.getBlockPos());
level.sendParticles(ParticleTypes.SOUL, ...); // 12 颗灵魂
level.playSound(null, pos, SoundEvents.SOUL_ESCAPE.value(), BLOCKS, 0.8F, 0.6F);
```

* **不自建死亡流程**：`setHealth(0)` 之后，FCE 自己的 `BeeHousingBeekeepingLogic#canWork` 会走它本来就走的分支——`queen.isAlive()` 为假 → `killQueen(queen, housing, listener)` → `spawnOffspring`：2% 概率两只公主、否则一只公主，加上 1–3 只雄蜂，全部 `addProduct` 进蜂房；仓满的留在蜂房自己的 `Offspring` 队列里（随方块存档），并注册进主人的 `IApiaristTracker`；`beeListener.onQueenDeath()` 照常触发（框架磨损等）。这与老死、被"框架老化"杀死的蜂后完全同一条路径。
* **为什么不直接 `setQueen(EMPTY)`**：那样是"删除"，没有后代、没有育种记录，也不触发监听器。用户要求"杀了要正常结算"，所以这里只负责"让她死"，结算交给游戏。顺带绕开了引用比较的坑：整槽替换成 `EMPTY` 才能让 beekeeping logic 发现变化，而生命值归零是由它自己发现 `!isAlive()` 的，两条路径都不会留下"槽里空着却以为有蜂后"的状态。
* **为什么要立刻调 `canWork()`**：若不调，目标蜂房要到下一个 game tick 才结算；而这一 tick 里若正好轮到它的工作周期结算，`queenWorkTick` 会把缓存里的旧生命值 `saveToStack` 写回去，把这次击杀覆盖掉。立刻结算把窗口关掉，也让"杀→产出公主/雄蜂"在观感上是同一瞬间。
* **要自己标脏**：蜂房掉了蜂后，下一次 `canWork()` 立刻报 `NO_QUEEN` 并返回 false，而 FCE 只在 `canWork()` 为真时 `blockEntityChanged`。不补这一下，重载后蜂后可能"复活"。
* **产物与粒子**：死神不碰蜂房的产品槽；它留下的只有灵魂粒子与音效。掉的是这一任蜂后、她的当前工作周期，以及她这一代的血脉优势。
* 蜂房组（alveary）的每个部件都上报同一个库存：用 `IdentityHashMap` 做集合去重，一座蜂房组只结算一次；玩家看到的是灵魂从这块多方块的一个部件飘出来。

### 4.5 玩法：这东西给谁用

* **清场**：把一整片（别人的、废弃的、野生村庄里的）蜂房的蜂后一次清掉，不用一个个点。注意野生蜂巢不在判定内，它清的是**玩家蜂房**。
* **耗王**：PvP/服务器里持续消耗对手的蜂后——每杀一只就少一任蜂后、少一个工作周期；但公主与雄蜂仍会照常产出，所以是"消耗"而不是"灭种"。
* **自伤警告**：默认配置下，死神**也吃自己主人的蜂房**。把它放进自己的蜂场 = 自己的蜂后被一只只杀掉。想安全使用有三条路：
  1. 电路巢础的**抑制剂**程序（范围 ×0.4）；
  2. 配置 `reaperSparesSameOwner = true`（只吃别人的/无主的，单人里等于无害）；
  3. 单独给它一间房，别和自己的蜂场贴在一起。
* **反向利用**：死神也能当"远程弃王"工具——把不想留的蜂后放在范围里，让她死一次，比手动掏槽省事（公主照出）。
* **与伏打蜂的闭环**：死神的四个"娇气"（夜行、怕雨、无气候耐受、吃凋灵玫瑰）正好是本模组电路巢础五个布尔程序要解决的问题（末影 15 / 铜 10 / 琥珀 25 / 绿宝石 35，共 85 FE/周期）。死神自己不发一度电，所以它的蜂房组要由隔壁的伏打蜂供电：伏打蜂把电推到它自己外壳外的邻居上，而死神蜂房组的配电就长在那圈外壳上。**伏打供电、死神收魂**，两只蜂在同一个基地里互为因果。

## 5. 基因组

| 染色体 | 值 | 显性 | 设计理由 |
|---|---|---|---|
| `speed` | 1.0 | 是 | 收魂周期固定，速度只影响产物；给 1.0 让它在"能干活"这件事上不拖后腿 |
| `lifespan` | 30（偏短档） | 否 | 死神不留太久；也让"维持一条纯血线"成为成本 |
| `fertility` | 2 | 是 | 常见档，方便量产与继续突变 |
| `temperature_tolerance` | `NONE` | 否 | 娇气：要蜂房组加热/降温，或琥珀程序 |
| `humidity_tolerance` | `NONE` | 否 | 同上 |
| `activity` | `forestry:activity_nocturnal` | 否 | **夜行**：白天不收魂。想 24 小时收魂就上末影程序（本模组电路巢础的"昼夜工作"） |
| `cave_dwelling` | `true` | 是 | 可以完全不见天；地下蜂场也能养 |
| `tolerates_rain` | `false` | 是 | 下雨停机（铜程序 / 屋顶 / 耐雨基因三选一） |
| `flower_type` | `forestry_oddities:flower_type_death` | 否 | 它吃凋灵玫瑰与灵魂火把（§6） |
| `bee_effect` | `forestry_oddities:reaping` | 否 | 核心；隐性（杂交可能携带而不表达，要稳定收魂就得维持纯血线） |
| `pollination` | 5 | 否 | 它不是在传粉 |
| `territory` | `[15, 9, 15]` | 否 | 收魂范围本身（§4.2） |

> **设计意图**：这是一只"难伺候但杀伤力直接"的蜂。它不靠概率、不靠数值，靠**位置**——放对了清场，放错了清自己。

## 6. 花型：死亡

数据包花型 `forestry_oddities:flower_type_death`，标签 `forestry_oddities:flowers/death`：

```json
{ "values": [ "minecraft:wither_rose", "minecraft:soul_torch", "minecraft:soul_wall_torch" ] }
```

* 凋灵玫瑰是"死之花"：需要凋灵（或让凋灵杀生物）才拿得到，正好是死神的口粮；
* 灵魂火把补一条较便宜的替代路线（灵魂沙/灵魂土 + 木棍），否则这只蜂在没打过凋灵的存档里根本不能工作。
* 与突变条件（§7）呼应：**蜂房正下方要有一朵凋灵玫瑰**才会掷出这只蜂——它出生时脚下就踩着口粮。

## 7. 突变

`data/forestry_oddities/recipe/bee_mutation/reaper_1.json`：

```json
{
  "type": "forestry:bee_mutation",
  "chance": 0.03,
  "conditions": [
    { "type": "forestry:daytime", "day": false },
    { "type": "forestry:requires_resource", "blocks": [ { "Name": "minecraft:wither_rose" } ] }
  ],
  "first": "forestry:phantasmal",
  "id": "forestry_oddities:bee_mutation/reaper_1",
  "result": "forestry_oddities:reaper",
  "second": "forestry:demonic"
}
```

| 项 | 值 | 理由 |
|---|---|---|
| 父本 | `forestry:phantasmal`（幻影）× `forestry:demonic`（魔鬼） | 一头是"死了又回来"的魂（复活效果），一头是地狱里点火的东西；死神是这两个概念的合流 |
| 几率 | 0.03 | 幻影本身是 `spectral × ended` 的 0.02，深度已经很高；0.03 让"凑齐父母"比"掷骰子"更贵 |
| 条件一 | 夜晚（`forestry:daytime` + `day: false`） | 死神在夜里诞生；也顺手和"夜行"这条基因对上 |
| 条件二 | 蜂房正下方是凋灵玫瑰（`forestry:requires_resource`） | 仪式感 + 与花型、产物呼应；`requires_resource` 从蜂房位置向下走、跳过蜂房方块后比较第一个方块，所以普通蜂房就放在玫瑰上面一格，蜂房组则要放在整个多方块下面（蜂房组上报的位置是多方块中心上方一格，向下走会穿过下面的部件，最终落在最底层下面那格） |

技术细节：

* `daytime` 条件是 FCE 自带的 `MutationConditionDaytime`，`day: false` 即 `level.isDay() == false`；不需要新条件类型。
* `requires_resource` 也是自带的，`blocks` 用 `BlockState.CODEC`，所以 JSON 是 `{ "Name": "minecraft:wither_rose" }`。
* 两个条件是相乘关系：任一为 0 则整条突变不成立，顺序无关。
* 父母一方是地狱蜂、一方是寒带蜂，气候都不普通；实际配对时要么把蜂房建在两者都能活的蜂房组里（本模组电路巢础的琥珀程序直接抹平气候），要么接受其中一只"心情不好"。`canWork()` 为假时不会配对，所以这确实是一道门槛。

## 8. 物种元数据与外观

| 字段 | 值 | 说明 |
|---|---|---|
| `genus` | `styxapis` | 见 §3 |
| `species` | `reaper` | 文件 `data/forestry_oddities/bee_species/reaper.json` |
| `dominant` | `false` | 种等位基因隐性（与后期种一致） |
| `glint` | `true` | 死亡主题的附魔光；与伏打蜂的哑光铜刻意相反 |
| `secret` | `false` | 不藏 |
| `authority` | `zhichaoxi2006` | 图鉴署名 |
| `temperature` / `humidity` | `normal` / `normal` | 理想气候普通；但耐受为 NONE，照样娇气 |
| `body` | `0x6B5876`（7034998） | 灰紫——寿衣的颜色 |
| `stripes` | `0x4FBF7A`（5226362） | 魂火绿；比奶油色环带暗一档，16×16 下也数得清 |
| `outline` | `0x140B1E`（1313566） | 近黑紫，压住轮廓 |
| `products` | `forestry:mysterious_comb` 0.25 | 神秘蜂窝——幽灵蜂的产物，死神的"收成" |
| `specialties` | `minecraft:bone` 0.20、`minecraft:wither_rose` 0.05 | 骨头是"收割"的痕迹；凋灵玫瑰让它偶尔自己补口粮 |
| `jubilance` | 省略（默认） | 不额外加条件 |

配色说明（tint 是乘法，身体贴图本身是米褐色，所以写进去的颜色会整体压暗）：

* `0x6B5876` = `107×65536 + 88×256 + 118` = **7034998**
* `0x4FBF7A` = `79×65536 + 191×256 + 122` = **5226362**
* `0x140B1E` = `20×65536 + 11×256 + 30` = **1313566**

不需要材质、模型或客户端注册：FCE 按 tint 索引 0/1/2 给 outline/body/stripes 上色。

## 9. 语言

`assets/forestry_oddities/lang/zh_cn.json`：

```json
{
  "for.genus.bees.styxapis": "黄泉",
  "allele.forestry.bee_species.forestry_oddities.reaper": "死神",
  "allele.forestry.bee_species.forestry_oddities.reaper.desc": "它不采蜜，只收账：方圆之内，别的蜂后活不过第二个黄昏。|《养蜂入门指南》",
  "allele.forestry.flower_type.forestry_oddities.flower_type_death": "死亡",
  "allele.forestry.bee_effect.forestry_oddities.reaping": "收魂",
  "allele.forestry.bee_effect.forestry_oddities.reaping.desc": "蜂后一顶一顶地熄灭，像是被谁记下了名字；只有同行的死神，它从不动手。"
}
```

`assets/forestry_oddities/lang/en_us.json`：

```json
{
  "for.genus.bees.styxapis": "Styx",
  "allele.forestry.bee_species.forestry_oddities.reaper": "Reaper",
  "allele.forestry.bee_species.forestry_oddities.reaper.desc": "It gathers no nectar; it collects debts. Within its reach, no other queen sees a second dusk.|Beekeeping 101",
  "allele.forestry.flower_type.forestry_oddities.flower_type_death": "Death",
  "allele.forestry.bee_effect.forestry_oddities.reaping": "Soul Reaping",
  "allele.forestry.bee_effect.forestry_oddities.reaping.desc": "Queens go out one by one, as if someone had written their names down; it never raises a hand against its own."
}
```

* 种描述是图鉴正文，直接说清"它杀蜂后"；效果描述是分析仪里的一行小字（`BeeAnalyzerPlugin#addHoverDescription`，**不自动折行**，长度须控制），所以写成谜语，但"同行的死神不动手"就是"不杀同类"的玩家可读版本。
* 花型键、属名键都按 §3 的跨命名空间规则拼。

## 10. 配置

`config/forestry_oddities-common.toml`，`[apiculture]` 段新增两项：

```java
REAPER_REAP_INTERVAL_TICKS = builder
    .comment(..., "0 disables reaping without removing the bee or its effect allele.")
    .defineInRange("reaperReapIntervalTicks", 40, 0, 1200);

REAPER_SPARES_SAME_OWNER = builder
    .comment(..., "Off (default): the Reaper takes every queen in its territory except its own kind.")
    .define("reaperSparesSameOwner", false);
```

* 两项都在**每次结算时读取**，改完即生效，不用重启。
* `reaperReapIntervalTicks = 0` 是服务器关掉这只蜂杀伤力的开关（蜂还在，图鉴还在，只是不收魂）。
* `reaperSparesSameOwner = true` 时只跳过**同一非空所有者**的蜂房；无主蜂房（村庄、结构）永远不算"同一主人"，避免"打开开关就什么都不杀了"。

## 11. 玩法闭环与平衡

```
        ┌────────── 伏打蜂的蜂房组 ──────────┐        ┌────────── 死神蜂的蜂房组 ──────────┐
        │ 伏打蜂 ──推电──▶ 外壳邻居           │        │ 死神蜂（夜行）──每 40 tick 收魂──▶ │
        │              （含隔壁的配电）──┐    │        │   半径 15×9×15 内所有异种蜂后      │
        └──────────────────────────────┼────┘        └───────────────────────────────────┘
                                       ▼                              ▲
                            死神蜂房组的配电 ──▶ 电路巢础（末影/铜/琥珀/绿宝石）
```

* **死神不发一度电**。它要的四五个程序（昼夜 15 + 防雨 10 + 气候 25 + 花源 35 = 85 FE/周期）全部由外部供电，而"外部"最自然的就是隔壁那只伏打蜂——伏打蜂 123 FE/tick、一个工作周期 67 650 FE，够养上百个死神蜂房组。
* **强度不在数值，在位置**：15×9×15 的立方体意味着"相邻两座蜂房组一定中招"。反制手段只有三条：把非死神蜂后挪出范围、用抑制剂程序把范围压到 6×3×6、或者服务器把配置调成 0 / 打开同主豁免。被杀的一任蜂后按正常死亡结算，公主与雄蜂照出，所以它消耗的是"蜂后 × 时间"，不是血脉。
* **自伤是设计的一部分**：默认配置下，自己的蜂后也会被一只只杀掉。§4.5 给了三条安全用法，玩家第一次踩坑就是教学。
* **不适合当"刷产物"的蜂**：神秘蜂窝 0.25、骨头 0.2、凋灵玫瑰 0.05，产出刻意压低，它的价值在"清场"。

## 12. 文件清单

**新增代码（1 个）**

| 路径 | 内容 |
|---|---|
| `content/apiculture/ReapingBeeEffect.java` | 收魂：周期、范围、同类判定、生命值归零 + 交给游戏正常结算、特效 |

**修改代码（2 个）**

| 路径 | 改动 |
|---|---|
| `integration/ForestryOdditiesPlugin.java` | `registerApiculture` 追加 `registerBeeEffect(ReapingBeeEffect.ID, ...)` |
| `config/ForestryOdditiesConfig.java` | 新增 `reaperReapIntervalTicks`、`reaperSparesSameOwner` 与两个 getter |

**新增数据（5 个）**

```
data/forestry_oddities/taxon/styxapis.json
data/forestry_oddities/bee_species/reaper.json
data/forestry_oddities/flower_type/flower_type_death.json
data/forestry_oddities/tags/block/flowers/death.json
data/forestry_oddities/recipe/bee_mutation/reaper_1.json
```

**语言（2 个文件各加 6 行）**：见 §9。

**文档（4 个）**：本稿；`README.md` 新增 "Reaper Bee (死神蜂)" 一节；`TODO.md` 增一条；`docs/curseforge-description.md` 的中英文内容一览与各一节。

## 13. 实现步骤

1. 写 `ReapingBeeEffect`（周期 → 范围 → 去重 → 同类判定 → 摘除 → 粒子音效）。
2. `ForestryOdditiesConfig` 加两项配置。
3. `ForestryOdditiesPlugin#registerApiculture` 注册效果。
4. 五个 JSON 数据文件。
5. 两份语言。
6. 编译（`.\gradlew.bat --offline build`），开发服务器确认 `Loaded 71 bee species` 且无 datapack 报错。
7. README / TODO / CurseForge 文档化。

## 14. 验证清单

这套内容是一条数据 + 一个效果类，没有自动化测试；下面是给审查用的清单，前四条读文件/读日志即可，后五条要在游戏里跑。

1. **载入**：启动日志出现 `Loaded 71 bee species`（70 + 本种），没有 `Skipping bee_species …`；蜂书里死神挂在黄泉属下，自带死亡花型与收魂等位基因。
2. **基因组**：`bee_species/reaper.json` 与设计一致——速度 1.0（显性）、寿命 30、生育 2、双耐受 `NONE`、夜行、可不见天、怕雨、授粉 5、领地 15/9/15、种隐性、glint 为真、三种颜色、一份产物 + 两份特产。
3. **唯一入口**：`recipe/bee_mutation/` 下只有 `reaper_1.json` 通往本种——父本 `phantasmal × demonic`、`chance` 0.03、两个条件同时挂在一座蜂房上（`MutationConditionDaytime` = 夜里、`MutationConditionRequiresResource` = 底座下压一朵凋灵玫瑰）。
4. **代码**：`ReapingBeeEffect` 里 `canReap` 只认 `BeeLifeStage.QUEEN`，`isKindred` 同时按「活跃种 = 死神」和「活跃效果 = 收魂」豁免，野巢按 `IHiveTile` 跳过，自己的蜂房由预置进 `seen` 的共享库存排除；`killQueen` 是「血量归零 → 存档 → `canWork()`」。
5. **范围**：在 (0,64,0) 放一只死神，四周摆蜂房——(7,68,7) 内的被清、(8,64,0) 与 (0,69,0) 的不受影响；换成抑制剂程序后 (4,0,0) 的蜂房逃过一劫。
6. **同类 / 自己家**：死神蜂房里塞一只纯血死神蜂后，不动；旁边放一只"别的种 + 隐性收魂"的杂交蜂后，被摘；公主 + 雄蜂的配对蜂房不被清；野生蜂巢（`IHiveTile`）不被碰。
7. **正常结算**：范围内异种蜂后被摘后，产物槽里出现后代公主（有雄蜂陪伴时还会有雄蜂），育种记录与 `onQueenDeath` 照常触发——即和寿终正寝完全同一条路径；第二遍收魂不再取任何人。
8. **持久化与蜂房组**：摘掉蜂后立即 `save` 并重载，蜂后不回来（验证 `blockEntityChanged`）；一座 3×3×3 蜂房组里放一只异种蜂后，只结算一次（灵魂粒子一批）。
9. **配置与回归**：`reaperReapIntervalTicks = 0` 什么都不杀；`reaperSparesSameOwner = true` 时同主蜂房不被清、无主蜂房照清；`.\gradlew.bat --offline build` 通过；伏打蜂与 14 个电路程序的既有行为不变。

> 之前的一轮临时 listener 自测（验完已删除，不随包发布）覆盖了 §1–§4 的静态事实和收魂的核心路径：范围内外、
> 同类豁免、公主/野巢、第二遍空跑。**这次改动后的正常结算路径与配置项没有跑过**，留给审查时在游戏里按 5–9 条
> 验；结果回填本节与 README 的 Verification。

## 15. 变体与后续

* **v2：只杀"有主"的蜂房**（现在的 `reaperSparesSameOwner` 是它的反向开关）。若你希望这只蜂在单人里也"有用"，可以让它只对**别人/无主**的蜂房生效，并把这个行为设为默认。
* **v2：留一只蜂后物品**。现在按正常死亡结算（公主 + 雄蜂），若想要更狠的"真死"，就得绕开 FCE 的死亡路径自建结算（无后代、无育种记录）——那是另一条分支。
* **v2：数据包可调**。给效果实现 `codec()`（`interval` / `territoryScale`），整合包可以做"高压死神/低压死神"。
* **自定义蜂窝**：像伏打蜂稿 §15 一样，不推荐和本蜂一起做。

## 16. 风险与平衡

| 风险 | 说明 | 应对 |
|---|---|---|
| **清自己家** | 默认配置下死神吃自己主人的蜂房 | §4.5 三条用法；配置项；README 首句警告 |
| **pvp 骚扰** | 对手每 2 秒掉一任蜂后与她当前的工作周期；公主与雄蜂仍出，所以是持续消耗而非灭种 | 配置项 `0` / 同主豁免；服务端可禁用；设计上这就是它的强度 |
| **跨模组蜂房** | 任何实现 `IBeeHousing` 的蜂房都会被清（含别的模组的蜂房） | 按接口判定是有意的：接口就是"这里有一只蜂后"的正式声明 |
| **野巢误伤** | 野巢也实现 `IBeeHousing` | 用 `IHiveTile` 显式跳过（§4.3）；野巢本来也没有蜂后槽 |
| **性能** | 每次结算扫一遍区块 block entity | 只读已加载区块、按区块遍历 + 立方体裁剪；默认 40 tick 一次，最坏 2000 格也只落在一个 3 区块范围里 |
| **同 tick 齐收** | 所有死神同一 tick 结算 | 有意为之；配置可拉开（改成不同 period 即可） |

## 17. 待你拍板（我自行决定的部分，逐条可改）

1. **名字**：种 `reaper / 死神`、属 `styxapis / 黄泉`、效果 `reaping / 收魂`。要换成别的（比如属名 `thanatapis`、种名 `mortis`）只需改 JSON、lang 与 `SPECIES_ID`。
2. **"不杀同类"的定义**：现在同时按「活跃种 = 死神」和「活跃效果 = 收魂」豁免。若你只想按种，把 `isKindred` 里的第二个条件删掉即可。
3. **杀法**：现在只把蜂后生命值归零，交给 FCE 自己的死亡结算（公主 + 雄蜂、育种记录、满仓排队），也就是"杀蜂后"而不是"删蜂后"。若要更狠的"无后代真死"，需要绕开游戏结算，属于另一条分支。
4. **周期**：默认 40 tick（2 秒）/ 可配 0–1200。
5. **范围**：领地 15×9×15（左右 7、上下 4）。这个数字直接决定"能祸害多远"，也可以改成 13/9/13 或 17/9/17。
6. **突变**：`phantasmal × demonic`、0.03、夜晚 + 蜂房下方凋灵玫瑰。父本/几率/条件都可以换（例如把凋灵玫瑰换成"地底"条件）。
7. **花型**：凋灵玫瑰 + 灵魂火把（含墙上的）。如果嫌"灵魂火把"太便宜，删掉它，只留凋灵玫瑰。
8. **气质**：夜行 + 可不见天 + 怕雨 + 无气候耐受（高维护、靠电路巢础伺候）。想更"野蜂"一点，把夜行换成 `metaturnal`（昼夜都干活）或把怕雨换成耐受。
9. **自伤与配置**：默认照杀同主蜂房（`reaperSparesSameOwner = false`）。若你更想让它在单人里也"安全"，把默认改成 `true`。
