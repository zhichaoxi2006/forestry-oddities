# 伏打蜂 / Voltaic Bee — 设计稿

> **状态：已实现。** 代码在 `content/apiculture/VoltaicBeeEffect`，数据在
> `data/forestry_oddities/{taxon,bee_species,recipe/bee_mutation}`，配置项、语言与文档均已落地。
> 实现与本稿的差异记在 §17，验证结果见 README 的 “Voltaic Bee (伏打蜂)” 一节。
> 基线：Minecraft 1.21.1 / NeoForge 21.1.230 / Forestry: CE `3.0.0-alpha9`。

---

## 1. 定位

一句话：**一只把蜂房"多余的精力"换成 FE 的蜂，给本模组的电力部件和外部电缆喂电。**

* 特殊效果沿用 CareerBees 能量蜂（`careerbees:power`）的思路：每个 effect tick 把一笔 FE 推给与蜂房相邻、且能接收的方块；推不出去的余量**直接丢弃**（§4.4），蜂房不留任何缓冲。
* **其余全部重新设计**：分类学、基因组、产物、突变路线、外观、语言、配置，以及"电从哪来、给谁用"的玩法闭环（§11）。

## 2. 与能量蜂的异同

| | CareerBees 能量蜂 | 本设计 伏打蜂 |
|---|---|---|
| 效果 id | `careerbees:power` | `forestry_oddities:voltaic` |
| 速率 | `ceil(400 × speed²)` FE/tick | `ceil(192 × speed²)` FE/tick |
| 余量去向 | 蜂房 1 tick 缓存，暴露成能量能力 | 直接丢弃：配电与电路巢础只是"外壳邻居"之一，和电缆走同一条推送（§4.4）；蜂房缓存见 §15 v2 |
| 速度 / 寿命 | 0.8 / 50 | 0.8 / 40 |
| 气候 | 无耐受、日行 | 无耐受、日行、怕雨、不见洞穴（**故意的**，见 §5） |
| 产物 | 蜂蜜蜂窝 0.4 | 蜂蜜蜂窝 0.3 + 红石 0.2 + 铜锭 0.08 |
| 获取 | 修理蜂 × 时钟蜂，0.05 | 错时蜂 × 冰河蜂，0.04，且必须是**雷雨天**（§7） |
| 位宽 | long / BigInteger 预算 | 同 —— 复用刚移植的 `api.energy` 与 `compat.energy` |

## 3. 标识与命名

| 内容 | id / key | 中文 | English |
|---|---|---|---|
| 属（taxon） | `electrapis` | 琥珀 | Electrum |
| 种 | `forestry_oddities:voltaicus` | 伏打 | Voltaic |
| 效果 | `forestry_oddities:voltaic` | 伏打放电 | Voltaic Discharge |
| 属名 key | `for.genus.bees.electrapis` | 琥珀 | Electrum |
| 种名 key | `allele.forestry.bee_species.forestry_oddities.voltaicus` (+ `.desc`) | 伏打 | Voltaic |
| 效果名 key | `allele.forestry.bee_effect.forestry_oddities.voltaic` (+ `.desc`) | 伏打放电 | Voltaic Discharge |

**翻译键规则（容易踩的坑，FCE `GeneticTranslationKeys`）**：等位基因名不是按"自己的命名空间"拼的，而是**染色体在前、贡献者命名空间在后**：

* 同命名空间：`allele.<染色体ns>.<染色体path>.<对象path>`（如 `allele.forestry.bee_species.imperial`）；
* 跨命名空间（本模组就是这种）：`allele.<染色体ns>.<染色体path>.<对象ns>.<对象path>`，即 **`allele.forestry.bee_species.forestry_oddities.voltaicus`**。

CareerBees 的 `allele.forestry.bee_species.careerbees.acceleration` 就是这个形状，可作对照。顺带这也解释了效果 id 为什么取 `forestry_oddities:voltaic` 而不加 `bee_effect_` 前缀：翻译键里已经有染色体名 `bee_effect`，再加就成了 `…forestry_oddities.bee_effect_voltaic`，平白重复。

命名理由：

* `electron` / 电 一词来自希腊语 **ἤλεκτρον**（琥珀），`electrum` 即琥珀金 —— 属名走"材料/现象"路线，与 FCE 的 `monapis` / `rustapis` / `spongiforma` 一致。
* 种名走"形容词"路线（`imperial` / `industrious` / `glacial`），"伏打"取自伏打电堆（Voltaic pile），是本模组电力部件的共同历史隐喻。
* 物品名由 FCE 的 grammar 自动拼出，不需要额外交互：**伏打蜂后 / 伏打雄蜂 / 伏打蜂**（`for.bees.grammar.*`），英文 Voltaic Queen / Drone / Princess。
* 林业自带的效果 id 都带 `bee_effect_` 前缀（`forestry:bee_effect_glacial` …）；本模组的效果 id **不带**，因为翻译键本身已经把染色体名放进去了（见上表），前缀只会重复。`/forestry dump` 里显示为 `forestry_oddities:voltaic`。

## 4. 特殊效果

### 4.1 触发时机

FCE 的 `BeeHousingBeekeepingLogic#queenWorkTick` **每 tick** 调 `queen.doEffect(...)`，并且只在 `errorLogic` 无错误时调用（没蜂后、没花、下雨、气候超限、睡觉……都不触发）。因此：

* 效果每 tick 结算一次，**不额外加 throttle**（和能量蜂一致）；
* 不需要自己判断"能不能干活"，FCE 已经挡住了；
* **不写 NBT**：`validateStorage` 原样返回 `storedData`，效果是无状态的。这让它天然免疫"存档里残留旧计数器"的问题。

### 4.2 速率

```
speed = genome.getActiveValue(BeeChromosomes.SPEED)
      × 蜂房修正.modifyProductionSpeed(genome, 1.0F)      // 框架、电路巢础程序、蜂房类型
rate  = ceil(192 × speed² × config.voltaicBeePowerMultiplier)   // FE / tick
```

* 取整照抄能量蜂的写法 —— `new BigDecimal(double).setScale(0, RoundingMode.CEILING).toBigIntegerExact()` —— 保证 speed 0.8 得到 **123** 而不是 122（`0.8²` 的二进制值是 0.64000000000000001…）。
* 生产速度修正为 0 时按 1 处理（避免"框架关掉就无穷大"的除零写法），与能量蜂一致。

| speed | 来源示例 | rate (FE/tick) | 折合 FE/s |
|---|---|---|---|
| 0.3 | 慢速等位基因 | 18 | 360 |
| 0.6 | 较慢 | 70 | 1 400 |
| **0.8** | **本种默认** | **123** | **2 460** |
| 1.0 | 正常 | 192 | 3 840 |
| 1.2 | 快速 | 277 | 5 540 |
| 1.4 | 更快 | 377 | 7 540 |
| 1.7 | 最快等位基因 | 555 | 11 100 |
| 1.6 | 0.8 × 2（一张生产框架） | 492 | 9 840 |
| 3.2 | 0.8 × 4（两张框架） | 1 967 | 39 340 |
| 6.4 | 0.8 × 8（三张框架） | 7 865 | 157 300 |

**上表的“速度”是有效速度**：`蜂房修正.modifyProductionSpeed` 也是折算进去的，而 Forestry: CE 的蜂房自带一个
**×0.1** 的修正（`ApiaryBlockEntity.ApiaryBeeModifier`），蜂房组没有。所以上表是**蜂房组**里的数值；同一只蜂
在蜂房里只有 2 FE/tick。这与 CareerBees 一致，故意不修正：蜂房组才是接配电、装电路巢础的家（§11），
三张生产框架在蜂房里也只到 79 FE/tick。

**为什么是 192**：同速度下约为能量蜂（400）的 48%。理由不是"获取便宜"——路线后来改成了大后期——而是 §11 的自给闭环让它几乎不需要外部电力，它的价值在"接上就不用管"，不在峰值；如果想要能量蜂同款强度，配置调到 `2.08`。

### 4.3 推送算法

与能量蜂相同：把邻近方块按顺序逐个尝试，预算收一笔就用掉一笔。

```
remaining = rate
for pos in adjacentPositions(housing):          // §4.3.1
    if remaining <= 0: break
    side   = Direction.getNearest(蜂房pos - pos)
    sink   = LongEnergy.find(level, pos, side)  // 自有能力 → 各 mod 适配器 → 原生 FE
    if sink != null:
        accepted = sink.receiveEnergy(remaining, false)
        remaining -= clamp(accepted, 0, remaining)
// 剩下的直接丢弃（§4.4）：没有回灌，蜂房也不留缓冲
```

#### 4.3.1 "邻近方块"是多方块外壳

不是蜂房方块自己的 6 个面，而是**整座蜂房组外壳外侧一圈**：从蜂房方块出发，洪水填充所有共享同一个 `getBeeInventory()` 的 `IBeeHousing`（蜂房组的每个部件 + 普通蜂房组方块都是 `IBeeHousing`），把它们**不是**蜂房的邻居收集起来。能量蜂 1.12 就是这个语义，移植版也是，这里保持一致。

* 结果按 housing 缓存在 `WeakHashMap<IBeeHousing, List<BlockPos>>`（能量蜂同款），另加一个 400 tick 全局清理钩子（`ServerTickEvent.Post`，能量蜂用 400 tick 清一次缓存）。
* `side` 永远取六向之一（从蜂房指向目标），不会给接收端传 null。
* 顺序不去抖：同一个蜂房的相邻表在一生之内稳定，玩家看到的是"先接上的先吃电"。

### 4.4 余量：丢弃（回灌已删除）

预算走完邻近方块后还剩 `left`，**到此为止**：剩下的直接丢掉，蜂房不留缓冲，`push()` 把余量原样返回，`doEffect` 不读它。

早期稿在 `push()` 之后还有一段**余量回灌**：在已组装的蜂房组里找第一个 `AlvearyTransmissionBlockEntity`，把余量灌进它的接收缓冲，好让"不下电缆也能自转"。这段代码已经删除，两条理由：

1. **它和外壳推送重叠。** 蜂房组的部件（配电、电路巢础、框架外壳）不是 `IBeeHousing`；按 §4.3.1 的规则，外壳是共享同一 `getBeeInventory()` 的蜂房方块集合，所以这些部件本来就是"外壳外侧的邻居"。蜜蜂推开电缆的那套逻辑，同样会把电推开配电的缓冲区——回灌等于把同一件事再做一遍。
2. **它多一条不可控的路径。** 回灌排在推送之后，实际语义是"电缆先吃、蜂房组吃剩的"，却披着"内部结算"的外衣；删掉之后只剩一条线：谁在外壳上、谁先被遍历到，谁先吃。

所以 §11 的"蜂房组自己转起来"不需要特例代码：配电作为邻居拿到电，再按它自己的每 tick 派发规则分给组内其它部件（`AlvearyPartEnergy#distribute`）。**代价**是配电不再有优先权——外壳上若接了能一次吃光的电缆，蜂房组的电路巢础确实会被抢在前面，这是保留 `push` 单一通道后接受的行为。

### 4.5 粒子

每 4 tick 一次（`level.random.nextInt(4) == 0`）：

```
count = min(6, 1 + rate / 128)
serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, fx.x, fx.y, fx.z, count, 0.5, 0.5, 0.5, 0.02)
```

* 位置取 `housing.getBeeFXCoordinates()`（FCE 给的蜂房特效坐标）。
* 客户端不额外实现 `doFX`（用 FCE 默认的蜂房粒子）。
* **不播音效**：每 4 tick 一次会吵；如果以后要，放在"蓄满一个工作周期"这类稀疏事件上。

### 4.6 组合性与显性

| 钩子 | 取值 | 理由 |
|---|---|---|
| `isDominant()` | `false` | 效果等位基因隐性（与能量蜂一致）：杂交后代可能把它藏起来，想要稳定发电就得纯血 |
| `isCombinable()` | 接口默认 `false` | 一只蜂一个主效果，避免与其它 mod 的效果叠成"复合发电厂" |
| `codec()` | 不实现 | 纯代码注册（FCE 自家 14 个 bespoke 效果同款）。若以后想让数据包调参，再加两个字段：`rate` / `period` |
| `validateStorage` | 默认 | 无状态 |
| `doFX` | 默认 | 见 §4.5 |

### 4.7 代码契约

```java
package com.zhichaoxi.forestryoddities.content.apiculture;

public final class VoltaicBeeEffect implements IBeeEffect {
    public static final ResourceLocation ID =
        ResourceLocation.fromNamespaceAndPath(ForestryOddities.MOD_ID, "voltaic");

    private static final int BASE_VOLTAGE = 192;      // FE/tick at speed 1.0
    private static final int PARTICLE_PERIOD = 4;     // ticks between spark bursts

    @Override public boolean isDominant() { return false; }

    @Override
    public IEffectData doEffect(IGenome genome, IEffectData data, IBeeHousing housing) { ... }

    /** 纯函数，方便单测：ceil(192 × speed² × multiplier)。 */
    static long discharge(float speed, double multiplier) { ... }

    /** 洪水填充，返回外壳外侧一圈（位置 + 真正贴着外壳的那个面）。 */
    static List<ShellFace> adjacentPositions(IBeeHousing housing) { ... }

    /** 逐个邻居推送，返回没人要的余量（调用方丢弃）。 */
    static long push(Level level, IBeeHousing housing, long budget) { ... }
}
```

注册（`integration/ForestryOdditiesPlugin`，新增一个重写）：

```java
@Override
public void registerApiculture(IApicultureRegistration apiculture) {
    apiculture.registerBeeEffect(VoltaicBeeEffect.ID, new VoltaicBeeEffect());
}
```

### 4.8 为什么用 long 通道（诚实说明）

本种实际速率永远在 int 内（§4.2 最大 7 865/tick），所以 long **不是**为了"一次推超过 21 亿"。它买的是三件事：

1. **接收端的量程**：MI 的 EU、Mekanism 的焦耳、Flux Networks / 长整型 FE 的存储都可能远超 int。`LongEnergy.find` 会把整笔交给它们的**原生** `long` API，而不是走"拆成 int 片段"的兜底（拆片会被某些 mod 的单次插入上限吃掉余量，也会多打十几次方法调用）。
2. **v2 蜂房电容**（§15）：容量 `Long.MAX_VALUE`，五大 mod 的电缆都能一次取走。
3. **代码路径统一**：本模组的产电/输电全部走同一套 `LongEnergy`，不给蜜蜂开特例。

## 5. 基因组

下面是 species JSON 的 `genome` 字段内容（片段；完整文件见 §12）：

```json
"genome": {
  "forestry:speed":                 { "value": 0.8,                              "dominant": true  },
  "forestry:lifespan":              { "value": 40,                                 "dominant": false },
  "forestry:fertility":             { "value": 2,                                  "dominant": true  },
  "forestry:temperature_tolerance": { "value": "NONE",                             "dominant": false },
  "forestry:humidity_tolerance":    { "value": "NONE",                             "dominant": false },
  "forestry:activity":              { "value": "forestry:activity_diurnal",        "dominant": false },
  "forestry:cave_dwelling":         { "value": false,                              "dominant": true  },
  "forestry:tolerates_rain":        { "value": false,                              "dominant": true  },
  "forestry:flower_type":           { "value": "forestry_oddities:flower_type_redstone", "dominant": false },
  "forestry:bee_effect":            { "value": "forestry_oddities:voltaic",         "dominant": false },
  "forestry:pollination":           { "value": 15,                                 "dominant": false },
  "forestry:territory":             { "value": [9, 6, 9],                          "dominant": false }
}
```

| 染色体 | 值 | 显性 | 设计理由 |
|---|---|---|---|
| `speed` | 0.8（较慢档） | 是 | 速率∝speed²，0.8 是"中等偏快"的手感；显性让杂交后代立刻有用 |
| `lifespan` | 40（正常档） | 否 | 发电靠"现在在干活"，不靠长寿；寿命短也让玩家必须维持繁育线 |
| `fertility` | 2 | 是 | 常见档，方便量产与做突变 |
| `temperature_tolerance` | `NONE` | 否 | **故意娇气**：要么用蜂房组加热/降温，要么用电路巢础的琥珀程序 |
| `humidity_tolerance` | `NONE` | 否 | 同上 |
| `activity` | `activity_diurnal` | 否 | 日行 —— 夜里不发电，逼玩家用末影程序或接受夜间停机 |
| `cave_dwelling` | `false` | 是 | 必须见天（或用硅程序模拟日照） |
| `tolerates_rain` | `false` | 是 | 见雨停机（或用铜程序防雨） |
| `flower_type` | `forestry_oddities:flower_type_redstone`（本模组的数据包花型） | 否 | 它吃红石：红石矿 / 深层红石矿 / 红石块都算花。口粮就是它的产物，主题上一笔到底；嫌麻烦可以上绿宝石程序（`providesFlowers()` 会直接跳过花检） |
| `bee_effect` | `voltaic` | 否 | 核心 |
| `pollination` | 15（较慢档） | 否 | 它不是在传粉，产物线也刻意压低 |
| `territory` | `[9, 6, 9]` | 否 | 只影响授粉/粒子范围，不影响放电（放电只看相邻面） |

> **设计意图**：这只蜂的四项"娇气"（怕雨、需日照、日行、无耐受）**正好**是本模组电路巢础五件套要解决的问题（铜 10 / 硅 15 / 末影 15 / 琥珀 25 / 绿宝石 35 FE 每工作周期）。也就是说：玩家的第一反应是"这不就是给我家电路巢础找活干吗" —— 这就是 §11 的闭环入口。若你希望它更"野蜂"一点，把四项中的一个改成耐受即可，其余不变。

## 6. 物种元数据

| 字段 | 值 | 说明 |
|---|---|---|
| `genus` | `electrapis` | 见 §3 |
| `species` | `voltaicus` | 文件路径 `data/forestry_oddities/bee_species/voltaicus.json` → id `forestry_oddities:voltaicus` |
| `dominant` | `false` | 物种等位基因隐性（与大多数后期种一致） |
| `glint` | `false` | 电弧靠粒子表现，不靠附魔光；与能量蜂（`glint: true`）刻意区分 |
| `secret` | `false` | 不藏 |
| `authority` | `zhichaoxi2006` | 图鉴署名 |
| `temperature` / `humidity` | `normal` / `normal` | 理想气候普通，但耐受为 NONE，所以照样娇气 |
| `body` | `0xD97C1E`（14253086）铜 | 身体主色 |
| `stripes` | `0x5FD8FF`（6281471）电弧蓝 | 条纹（**已按 §8.1 的实物预览加深**：`#9BE8FF` 会和固定的奶油色环带糊在一起） |
| `outline` | `0x2B1608`（2823688）焦棕 | 描边 |
| `products` | `forestry:honey_comb` 0.30 | 它不是产蜡蜂，只给基本产出 |
| `specialties` | `minecraft:redstone` 0.20、`minecraft:copper_ingot` 0.08 | 静电结晶 / 漆包线，主题上"铜 + 红石"就是电 |
| `jubilance` | 省略（默认 `forestry:default`） | 不额外加条件 |

配色说明（十进制值已算好，json 里直接写数字）：

* `0xD97C1E` = `217*65536 + 124*256 + 30` = **14253086**
* `0x5FD8FF` = `95*65536 + 216*256 + 255` = **6281471**
* `0x2B1608` = `43*65536 + 22*256 + 8` = **2823688**

## 7. 突变

`data/forestry_oddities/recipe/bee_mutation/voltaic_1.json`：

```json
{
  "type": "forestry:bee_mutation",
  "chance": 0.04,
  "conditions": [
    { "type": "forestry_oddities:thunderstorm" }
  ],
  "first": "forestry:anachrone",
  "id": "forestry_oddities:bee_mutation/voltaic_1",
  "result": "forestry_oddities:voltaicus",
  "second": "forestry:glacial"
}
```

| 项 | 值 | 理由 |
|---|---|---|
| 父本 | `forestry:anachrone`（错时）× `forestry:glacial`（冰河） | 叙事见 §7.1；错时把"琥珀里的古蜂"这一半补上，冰河补上"云里的电" |
| 几率 | 0.04 | 深度 8（错时 1 + 冰河 7）；期望 25 次配对，90% 在 57 次内 |
| 条件 | 雷雨天（`forestry_oddities:thunderstorm`） | 电从天上来；把"什么时候配"也变成条件，而不只是"在哪配" |
| 条件实现 | 本模组注册的 `forestry_oddities:thunderstorm` | 天气不在 FCE 的七个条件类型里，见 §7.3 |

**原方案（落地时未采用，见 §17）**：草稿里还有第二个条件——蜂房**正下方**是避雷针
（`forestry:requires_resource`），"引雷入巢"这一笔既是灵感来源，也把突变和"电"绑在一起。实现时核对了它的语义：
`requires_resource` 从 `housing.getBlockPos()` 开始向下走、跳过 `IBeeHousing`
方块后比较第一个方块。蜂房（apiary / bee house）就是蜂房正下方；**蜂房组上报的是多方块的锚点，也就是最低的那个角**
（`Bee#getChance` → `housing.getBlockPos()`，锚点是"lowest-(x,y,z) member"），所以避雷针要放在那个角的下面。
入口最后只留了天气，避雷针退回成叙事里的意象。

### 7.1 为什么是错时 × 冰河

这只蜂身上有三个已经写死、不能动的符号：

| 符号 | 出处 |
|---|---|
| 属名 **琥珀 `electrapis` / `Electrum`** | electricity 这个词来自希腊语"琥珀"（ēlektron）；琥珀摩擦毛皮是人类记录的第一次静电 |
| **铜翼 + 铜锭产物** | 伏打电堆（1800）的铜极 |
| **蜂房底下那根避雷针** | 富兰克林 1752，把天上的电引下来（落地时只作叙事意象，不做突变条件，见 §17） |

所以父本只需要补两件事：**古老**和**天上的电**。

* **错时蜂补"古老"。** 它自己写的是"依然顽强地保有自身的远古能力……窃取其他生物的生命力来延长自己的生命"，
  而它的前置**遗古蜂**是"从琥珀化石里挖出来、带回温暖的实验室之后……它好像动了起来"的那只蜂。伏打蜂的属名就是琥珀，
  所以它的父本本来就应该从琥珀里挖出来，而不是随便挑一个远亲。
* **冰河蜂补"天上的电"。** 雷云的电荷分离来自**冰晶与霰在云中碰撞摩擦**（非感应起电），而严寒蜂的描述是
  "饲育该蜂种将会使得蜂巢内部温度下降到冰凌凝结"——冰凌就在巢里。于是这只古蜂有了第二个摩擦起电的源头：不是毛皮，是冰。
* **避雷针在两个符号之间收口**：两个"摩擦生电"的源头在一只蜂身上合并，再由一根避雷针接住。

对照两个落选的：魔鬼蜂（火山闪电同样是摩擦起电，但它自己的身份是火与点燃，和"电"抢戏）、敬业蜂（"伏打电堆是工业
革命的产物"这条故事成立，但避雷针就只剩装饰作用）。**冰河是唯一让避雷针在叙事上"必须存在"的选择**，而避雷针是
已经写进 README 验证清单的东西。

### 7.2 为什么这条突变只在大后期

错时蜂本身极浅（深度 1），它的价格全在**入口**，而且全是 FCE 里"加温器 / 风扇 / 电路巢础都伪造不了"的成本：

| 环节 | 事实 |
|---|---|
| 遗古蜂 | 只在嗅探兽挖出的琥珀化石里：FCE 把 `forestry:amber_drone_fossil` 以权重 1 加进 `minecraft:gameplay/sniffer_digging` 主池；离心机 9 秒 **25%** 出一只"纯净"遗古蜂**雄蜂**，另 80% 出琥珀 |
| 坚定蜂 | 只在战利品箱里、且**只有雄蜂**：地牢 / 矿坑 / 沙漠神殿 / 要塞 / 冰屋 / 下界桥，权重 1 对"空" 9（约 10%/箱） |
| 雄蜂 → 公主 | FCE 3.0 **没有**培养箱 / 分离器 / 复制机（jar 里只剩 `textures/item/imprinter.png` 一张遗留贴图），只能配对纯化，两份都省不掉 |
| 冰河蜂 | 深度 7：凛冬（野生根）× 敬业 0.12 → 严寒，严寒 × 凛冬 0.08 → 冰河（两段都要求温度 `icy..cold`） |
| 雷雨窗口 | 天气循环随机给，一场雷雨只有几分钟；玩家要么等，要么在冰原上守着一座能挡雨、能控温的蜂房组 |

于是"贵"落在三件不同的事上：**考古（化石 + 开箱）**、**极地育种（两段低温突变）**、**等天气**。没有一项能靠
虚弱药水或金电子管抹掉——这也正是把它从"中期发电机"改成"大后期内容"的办法：不动几率，动进入条件。

### 7.3 突变条件的技术细节

* **天气是维度级的**：条件读 `level.isThundering()`，所以下界 / 末地永远不成立（那两个维度没有天气）。这正好——
  冰河是冰原蜂，本来就该在冰原等雨。
* **雷雨 = 下雨，而蜜蜂默认怕雨**：想让突变有机会掷出来，蜂房必须"不淋雨"——耐雨基因、蜂房组的 `SEALED`
  （铜电子管，10 FE）、或者盖顶（默认 `rainMode = BIOME` 下，蜂房上方天空光低于 `biomeModeSkyLight`（默认 10）
  即视为不淋雨）。这三条任意一条即可，也是"先造蜂房组、再造这只蜂"的又一笔成本。
* **原方案的方块条件（未采用，见 §17）**：`MutationConditionRequiresResource` 用
  `acceptedBlockStates.contains(blockState)`，一个条件里的多个方块"任一即可"；而两条 `requires_resource` 又都
  只看同一格（蜂房正下方），所以同一个条件里不能同时要求避雷针和另一种方块——"复古蜂窝块垫底"之类更重的门槛只能二选一。
  条件之间则是相乘的：各自把 0.04 乘成 0 或原样返回，顺序无关。

### 7.4 实现位置

| 路径 | 内容 |
|---|---|
| `content/apiculture/ThunderstormMutationCondition.java` | `IMutationCondition` 实现，`MapCodec.unit` + `StreamCodec.unit`（无字段的 JSON 形式） |
| `content/ModMutationConditionTypes.java` | `DeferredRegister` 注册进 Forestry 的 `forestry:mutation_condition_type` 注册表，id `forestry_oddities:thunderstorm` |
| `assets/forestry_oddities/lang/{zh_cn,en_us}.json` | `for.mutation.condition.thunderstorm`，JEI 里显示的那句 |

`equals` / `hashCode` 是必需的：unit 流编解码器会校验被编码的值，而突变配方要同步到客户端。

### 7.5 变体（记录，不采用）

* **去掉天气条件**：`anachrone × glacial`，0.04 + 避雷针——少一个"等雨"的环节，成本只剩两条蜂线。
* **原来的中期方案**：`industrious × unweary`，0.06 + 避雷针——电力部件的叙事还在，但"大后期"这个定位就没了。
* **更重的门槛**：把避雷针换成"复古蜂窝块"（只有遗物三兄弟产，2×2 四个复古蜂窝）——与避雷针互斥，见 §7.3。
* 早期稿里写过"更后期：`heroic` × `demonic`，0.04" ——**这是错的**：英雄蜂本身是 `steadfast × valiant`（两只都是
  野生根），深度只有 1，那个组合其实是深度 6，比现在的路线更便宜。

## 8. 外观与渲染

* FCE 的 `ForestryBeeItem#getColorFromItemStack` 按 tint 索引上色：**0 = outline、1 = body、2 = stripes**，其余索引返回白色。所以三种颜色写进 species JSON 就够了 —— **不需要任何材质、模型或客户端注册**。
* `glint: false` 让它在物品栏里是"哑光铜"而不是"附魔闪"，身份靠游戏内的电弧粒子表达。
* 若以后想要更强的辨识度（v2）：模仿 CareerBees 的做法，给默认蜂模型加一层自定义 overlay（`assets/forestry_oddities/models/item/bees/*.json` + 一张 16×16 的 PNG），通过 `IClientRegistration#setCustomBeeModel` 注册四种生活史模型。**本设计不要求**，属于加分项。
* 花粉/蜂巢 UI 中显示的物种描述来自 `allele.…voltaicus.desc`（§9）。

### 8.1 配色校准（实物预览之后）

tint 是**乘法**（屏幕色 = 贴图色 × 配色），FCE 的身体层贴图本身是米褐色（`#EDE2D7` / `#CEB39B`），所以 `#D97C1E` 渲染出来是"哑光铜"而不是"亮橙"——**这正是想要的**，不用改。

预览（`docs/previews/voltaic-bee-tint.png`，脚本同目录可复现）看了五个变体，结论：

| 变体 | 结果 |
|---|---|
| **V0 稿 v1**（`#D97C1E` / `#9BE8FF` / `#2B1608`） | 颜色本身没坏（不像零点蜂原稿会塌成黑），但**淡青条纹和固定的奶油色环带糊在一起**，腹部只剩"橙 + 近白"两色 |
| V1 铜更浓（`#C16A1C`） | 更像氧化铜，也更容易和深色木头背景糊，放弃 |
| V2 更亮橙（`#F0912A`） | 像金不像铜，与"伏打电堆"的叙事不合，放弃 |
| **V3 电弧更青**（条纹 `#5FD8FF`） | **采用**：腹部一眼看到"铜 / 电弧蓝 / 奶油"三色，`stripes` 才真的读成电 |
| V4 描边近黑（`#120A04`） | 与 V0 几乎无差别（身体本来就深），不必改 |

所以只改一个数字：`stripes` `#9BE8FF` → **`#5FD8FF`**（10217727 → **6281471**）。`body` / `outline` 保持原稿。

3 条通用事实（记在这里省得来回翻）：

1. 四层贴图里**只有两层能上色**：腹部底色（`body`）与腹部环带（`stripes`）；翅膀、头、腿、王冠是固定的奶油白 + 深褐，任何配色都改不了。
2. 描边层压在最上面，`outline` 决定 16×16 下的轮廓清晰度。
3. 因为固定的奶油色环带占了腹部一条，`stripes` **必须和奶油白拉开**才数得清颜色——这就是 V0 唯一的问题。

## 9. 语言

`assets/forestry_oddities/lang/en_us.json`：

```json
{
  "for.genus.bees.electrapis": "Electrum",
  "allele.forestry.bee_species.forestry_oddities.voltaicus": "Voltaic",
  "allele.forestry.bee_species.forestry_oddities.voltaicus.desc": "Copper wings, and a hum you can feel in your teeth. It does not make honey; it makes current.|Beekeeping 101",
  "allele.forestry.bee_effect.forestry_oddities.voltaic": "Voltaic Discharge",
  "allele.forestry.bee_effect.forestry_oddities.voltaic.desc": "Its fire only goes outward. Whoever will take it takes the whole of it; what nobody takes is scattered on the wind and never comes back.",
  "forestry_oddities.voltaic.rate": "Discharge: %1$s FE / tick"
}
```

`assets/forestry_oddities/lang/zh_cn.json`：

```json
{
  "for.genus.bees.electrapis": "琥珀",
  "allele.forestry.bee_species.forestry_oddities.voltaicus": "伏打",
  "allele.forestry.bee_species.forestry_oddities.voltaicus.desc": "铜翼，和一种能从牙根里听见的嗡鸣。它不酿蜜，它发电。|《养蜂入门指南》",
  "allele.forestry.bee_effect.forestry_oddities.voltaic": "伏打放电",
  "allele.forestry.bee_effect.forestry_oddities.voltaic.desc": "火只往壳外走。有人接，就整份递出去；没人接的散在风里，一滴也不回头。",
  "forestry_oddities.voltaic.rate": "放电：每 tick %1$s FE"
}
```

* 键的拼法见 §3（跨命名空间要带上 `forestry_oddities` 这一段）；林业自带的对照组是 `allele.forestry.bee_effect.bee_effect_glacial` —— 同命名空间，所以少一段。
* 最后一条 `forestry_oddities.voltaic.rate` 是**可选**的：如果分析仪页面里要显示实时速率（需要自定义 analyzer plugin，不在 v1 范围），先留 key 也行。
* **效果描述写成谜语**：`…voltaic.desc` 只作为分析仪里的悬停提示出现（`BeeAnalyzerPlugin#addHoverDescription`），是一行小字、没有 `|` 签名行，所以它既不分段也不报数字。正文用"火 / 壳外 / 风"换掉"FE / 外壳邻居 / 丢弃"，三件事却都在：只往外送、门外谁接就一次拿走整份余量、没人要的部分当场散掉（蜂房从来不是电池）。回灌删掉之后，**"一滴也不回头"就是新增的信息点**——玩家只能靠这句区分它和"带缓冲的蜂房"。种描述（`…voltaicus.desc`）反过来保持直白，因为那条是图鉴正文，要负责"它不酿蜜，它发电"这句说明书。
  长度上有硬约束：`drawTooltip` 不传 `maxWidth`，FCE 用 `renderTooltip(…, Optional.empty(), x, y)` 直接画，**不会自动折行**，所以两行以内、长度不超过旧版（中文约 40 字、英文约 140 字符）才不会被屏幕边缘切掉。

## 10. 配置

`config/forestry_oddities-common.toml`，`[apiculture]` 段追加一项：

```java
VOLTAIC_BEE_POWER_MULTIPLIER = builder
    .comment(
        "Output multiplier of the Voltaic Bee's discharge, applied on top of ceil(192 * speed^2).",
        "1.0 keeps the designed balance (123 FE/tick at speed 0.8). 0 disables generation without",
        "removing the bee. Values are read on every effect tick, so edits apply live.")
    .defineInRange("voltaicBeePowerMultiplier", 1.0D, 0.0D, 10.0D);
```

* 类型选 **COMMON**（与现有配置一致，`ForestryOdditiesConfig` 整体是 COMMON，值每次读取）。
* 双击调节范围 0–10：0 是"关掉发电但不删种"，10 是"作弊模式"。
* 回灌删掉之后只有一个行为参数，所以不需要"回灌开关"：配置项就这一个。

## 11. 玩法闭环

推荐配置（本模组三件套 + 一只伏打蜂）：

```
        ┌──────────────── 蜂房组 ────────────────┐
        │  伏打蜂 ──推电──▶ 外壳一圈的邻居：       │
        │                   电缆 / 机器 / 配电 /   │
        │                   电路巢础（谁会接给谁）  │
        │                        │                 │
        │        配电 ──每 tick 派发──▶ 组内其它部件 │
        │                              （铜/硅/末影/   │
        │                               琥珀/绿宝石）  │
        └─────────────────────────────────────────┘
                                  ▲
                        四件套让蜂房组"哪都能养蜂"
```

数值核算（速度 0.8，一个工作周期 = 550 tick）：

| 项目 | FE / 工作周期 |
|---|---|
| 伏打蜂产出 | 123 × 550 = **67 650** |
| 电路巢础五件套（铜10 + 硅15 + 末影15 + 琥珀25 + 绿宝石35） | **100** |
| 再加上铁(10) + 钻石(20) 两张生产电路（速度×1.5×2.5 → 3.0） | **130**，产出变 1728 × 550 = 950 400 |
| 配电缓存容量 | 1 000 |
| 电路巢础缓存容量 | 1 000 |

结论：产出是消耗的 **约 520 倍**（装生产电路后约 7 300 倍）。所以：

* **自给自足**是设计的核心卖点：这只蜂自己付得起"让自己能干活"的全部费用，玩家不需要先铺电网；
* 真正的"收益"是把**电送到外部**：电缆接在蜂房组外壳上，和配电、电路巢础一起排队，谁先被遍历到谁先吃，一次取走整笔（走 §4.8 的 long 通道）；
* 反过来说，这也意味着玩家不该指望"用伏打蜂的高压输出去喂大型工业线" —— 它是**稳定的、免维护的基载电**，不是终局发电厂。拿到它的门槛很后期，但拿到之后它干的活仍然只是"一直在那里发电"。

**平衡阀门**：速率∝speed²，而框架/程序乘的是 speed，所以叠加是二次的（§4.2 表最后一行的 7 865/tick 已经是"三张生产框架"的极端值）。如果实测太强，把 `BASE_VOLTAGE` 降到 128（默认速度 82/tick），或者把配置默认值调成 0.6。

## 12. 文件清单

**新增代码（4 个）**

| 路径 | 内容 |
|---|---|
| `src/main/java/com/zhichaoxi/forestryoddities/content/apiculture/VoltaicBeeEffect.java` | §4.7 的效果类 |
| `src/main/java/com/zhichaoxi/forestryoddities/content/apiculture/ThunderstormMutationCondition.java` | §7 的雷雨条件（v1 之后追加） |
| `src/main/java/com/zhichaoxi/forestryoddities/content/ModMutationConditionTypes.java` | 把上面那个条件类型注册进 `forestry:mutation_condition_type`（v1 之后追加） |
| （可选）`…/content/apiculture/VoltaicBeeEffectTest.java` 之类的开发期监听器 | 只在验证阶段存在，验完删除（本仓库惯例） |

**修改代码（3 个）**

| 路径 | 改动 |
|---|---|
| `integration/ForestryOdditiesPlugin.java` | 新增 `registerApiculture` 重写 |
| `config/ForestryOdditiesConfig.java` | 新增 `voltaicBeePowerMultiplier` |
| `ForestryOddities.java` | 追加 `ModMutationConditionTypes.register(modBus)`（v1 之后） |

**新增数据（5 个）**

```
data/forestry_oddities/taxon/electrapis.json
data/forestry_oddities/bee_species/voltaicus.json
data/forestry_oddities/recipe/bee_mutation/voltaic_1.json
data/forestry_oddities/flower_type/flower_type_redstone.json
data/forestry_oddities/tags/block/flowers/redstone.json
```

花型是**数据包**定义的（`forestry:tag_flower_type`），不需要 Java：

```json
{ "type": "forestry:tag_flower_type", "dominant": false, "flowers": "forestry_oddities:flowers/redstone" }
```

```json
{ "values": [ "minecraft:redstone_block", "minecraft:redstone_ore", "minecraft:deepslate_redstone_ore" ] }
```

`electrapis.json`：

```json
{ "parent": "apidae", "name": "electrapis", "rank": "genus" }
```

`voltaicus.json`（完整）：

```json
{
  "authority": "zhichaoxi2006",
  "body": 14253086,
  "stripes": 6281471,
  "outline": 2823688,
  "dominant": false,
  "genus": "electrapis",
  "glint": false,
  "secret": false,
  "species": "voltaicus",
  "temperature": "normal",
  "humidity": "normal",
  "products": [
    { "chance": 0.30, "item": "forestry:honey_comb" }
  ],
  "specialties": [
    { "chance": 0.20, "item": "minecraft:redstone" },
    { "chance": 0.08, "item": "minecraft:copper_ingot" }
  ],
  "genome": {
    "forestry:speed":                 { "value": 0.8,  "dominant": true  },
    "forestry:lifespan":              { "value": 40,   "dominant": false },
    "forestry:fertility":             { "value": 2,    "dominant": true  },
    "forestry:temperature_tolerance": { "value": "NONE", "dominant": false },
    "forestry:humidity_tolerance":    { "value": "NONE", "dominant": false },
    "forestry:activity":              { "value": "forestry:activity_diurnal", "dominant": false },
    "forestry:cave_dwelling":         { "value": false, "dominant": true  },
    "forestry:tolerates_rain":        { "value": false, "dominant": true  },
    "forestry:flower_type":           { "value": "forestry_oddities:flower_type_redstone", "dominant": false },
    "forestry:bee_effect":            { "value": "forestry_oddities:voltaic",         "dominant": false },
    "forestry:pollination":           { "value": 15,   "dominant": false },
    "forestry:territory":             { "value": [9, 6, 9], "dominant": false }
  }
}
```

**语言（2 个文件各加 6 行）**：见 §9。

**文档（2 个）**：`README.md` 新增 "Voltaic Bee (伏打蜂)" 一节（做法同现有各节：机制 + 数值 + 验证结果）；`TODO.md` 把本稿的"状态"从设计改为已实现。

## 13. 实现步骤

1. `ForestryOdditiesConfig` 加 `voltaicBeePowerMultiplier`（先加配置，后面代码直接读）。
2. `VoltaicBeeEffect` 写到"速率 + 推送 + 粒子"能跑。
3. `ForestryOdditiesPlugin#registerApiculture` 注册效果。
4. 三个 JSON 数据文件 + 两份语言。
5. §14 的 1–4 项（载入 + 速率 + 网格 + 粒子）。
6. §14 第 5 项（外壳推送：邻居吃电、余量丢弃）。
7. 突变（§14 第 6 项）。
8. README / TODO 文档化，删掉所有临时监听器。

## 14. 验证清单

沿用本仓库的做法（开发服务器 + 临时 listener，验完删除）：

1. **载入**：`/forestry dump bee_species` 输出里出现 `forestry_oddities:voltaicus`；日志里没有 `Skipping bee_species …`；`/forestry dump mutations` 里出现 `forestry_oddities:bee_mutation/voltaic_1`。
2. **速率**：临时 listener 在一个 speed 0.8 的蜂房里数 sink 收到多少 FE，断言每 tick 123 ± 1；把速度染色体改成 1.7 断言 555 ± 1；配置改 2.0 断言 246 ± 1。
3. **条件网格**：夜里（日行）、下雨（怕雨）、气候超限（无耐受）、蜂房组里换气不足 —— 四种情况分别断言"不产电"，防止把 FCE 的错误判断写两遍还写错。
4. **粒子**：每 4 tick 一批，数量随速率从 1 涨到 6（用 `sendParticles` 的临时包装断言，或直接数客户端）。
5. **外壳推送**：把外壳六个面全堵死（放满石头），断言没有任何方块收下电、余量被丢掉且不抛异常；再把一个配电贴到外壳上，断言它作为普通邻居每 tick 拿到当 tick 的预算、涨到 1 000 FE 封顶，然后由它自己的每 tick 派发喂起电路巢础。
6. **突变**：把天气切成雷雨 → 把 chance 临时改成 1.0，一次繁殖出伏打蜂；让天气放晴 → 繁殖 100 次都不出（`chance` 恢复 0.04 再跑一次正常概率）。
   路线与花型改完之后已按这条清单跑过一次开发服务器（临时 listener `dev/FlowerAndRouteSelfTest.java`，`25/25` 通过，验完删除）：
   物种数 70、进伏打蜂的突变只有一条、父本是 `anachrone` + `glacial`、几率 0.04、只有一个条件
   `forestry_oddities:thunderstorm`、该条件类型在 `forestry:mutation_condition_type` 注册表里、
   条件描述键 `for.mutation.condition.thunderstorm` 在两个语言的 lang 文件里都有、晴天返回 0 而雷雨返回原几率；
   另外物种携带的花型就是 `forestry_oddities:flower_type_redstone`、管理器能解析它、它的标签接受红石块 / 红石矿 /
   深层红石矿而拒绝虞美人、花型的 lang 键在两个语言文件里都有。
   顺带一个**实测才发现的坑**：开发世界的天气写在 `level.dat` 里，而且 `isThundering()` 要等 `rainLevel`/`thunderLevel`
   平滑上来（~100 tick）；自测里必须先把 `doWeatherCycle` 打开、切天气、再等若干 tick 才能观察到雷雨状态。
7. **闭环**：蜂房组（配电 + 电路巢础五件套）+ 若干伏打蜂，跨越 5 个工作周期，断言电路巢础从未因断电停机。
8. **回归**：`.\gradlew.bat --offline build` 通过；开发服务器启动无新错误；现有 14 个电路巢础程序、蜂房组框架外壳、配电的行为不变。

## 15. 变体与后续

**v2：蜂房电容（long 能力的第一次实战）**

* 效果把"邻近方块没人要的余量"写进一个**按蜂房/蜂房组控制器键**的 `WeakHashMap` 缓存（CareerBees 的 `HousingEnergy.Buffer` 语义：本 tick 与下一 tick 可读，之后过期，下一次效果覆盖而不会累积）。
* 用刚移植的 `LongEnergySource` 实现它，并在 `ModCapabilities` 里对**本模组自己的蜂房组部件**（框架外壳 / 电路巢础 / 蜂房组配电）调用
  `EnergyIntegrations.registerSourceCapabilities(event, type, (part, side) -> side == null ? null : part.energySource())`。
  apiary、bee house 和普通蜂房组方块属于 Forestry，本模组无法给它们注册能力 —— 这也是为什么 v2 的电容挂在自有部件上。
* 这样 MI / GrandPower / Flux Networks / Mekanism 的电缆能从蜂房组的自有部件上把余量**一次抽走**，而不是只能"被推"。这是 `api.energy` + `compat.energy` 那套代码真正的用武之地。
* 注意"只回应有侧查询"（`side == null` 返回 null）：Jade / The One Probe 用无侧查询决定要不要画电量条，而这个缓存是 1 tick 的余量，不是电池 —— 别给玩家画一条假电量条。

**其它可选**：

* **静电积累**：`storedData` 里累加"连续工作 tick 数"，速率按 `1 + 0.25 × 连续/200` 爬升，下雨/停机清零。会让"维持舒适环境"更有回报，但需要效果有状态（v1 刻意保持无状态）。
* **自定义蜂窝**（如"铜蜂窝"）：需要新物品 + 材质 + 蜂房配方 + 标签，属于一次完整的内容扩展，不建议和本蜂一起做。
* **数据包可调**：给效果实现 `codec()`（`rate` / `period`），让整合包能做"高压伏打蜂""低压伏打蜂"两个变体 —— 与 FCE "数据驱动优先"的方向一致。

## 16. 风险与平衡

| 风险 | 说明 | 应对 |
|---|---|---|
| **二次放大** | 速率∝speed²，而框架、电路巢础程序乘的是 speed；三张生产框架 + 高压程序能到 7 865 FE/tick | 默认 192 已比能量蜂低一半；配置项是最终阀门；README 里给出表格 |
| **FCE 框架怪癖** | `HiveFrameItem` 的生产修正是 `currentSpeed < 10 ? ×2 : 1`，堆到第 4 张会把速度打回 1 | 文档里写清楚；别在 effect 里"修正"它 |
| **突变条件的位置** | （原方案）`requires_resource` 只认蜂房正下方，蜂房组里读不到；该条件最终未采用，见 §17 | 若将来加位置类条件：种描述里提示"在蜂房里引雷"，或改成 `temperature: WARM` 之类不需要位置的条​件 |
| **雷雨窗口 + 怕雨** | 雷雨天气本身就让蜜蜂停工，玩家会以为"这条突变根本不可能" | §7.3 写明三条解法（耐雨 / `SEALED` 电路 / 盖顶）；JEI 会显示这条条件 |
| **自定义条件类型** | 天气条件是我们自己注册的类型，FCE 若日后自带 weather 条件，两套会并存 | 迁移成本只有一个类 + 一条 JSON；类型 id 已带命名空间，不会撞名 |
| **名字长度** | 中文 2 字「伏打」拼出来是"伏打蜂后"，与 FCE 的"帝皇蜂后"同构，不会溢出 | — |
| **与能量蜂同场** | 两个模组都在时，玩家可能叠加两种发电蜂 | 无冲突（效果互不影响），功率叠加只是玩家选择 |
| **无材质/无新物品** | 是设计的一部分：不需要做模型、配方、进度 | 也意味着"内容量"看起来少，用 §11 的闭环叙事补足 |

## 17. 实现记录（与本稿的差异）

实现与验证已经完成，下面是落地时与本稿不一致的地方，以及四个待定项的结论。

**四个待定项全部按本稿执行**：名字 `伏打 / Voltaic` + `琥珀 / Electrum` 不变；`BASE_VOLTAGE = 192`
（默认速度 123 FE/tick）；余量回灌进 v1（**后来整段删除**，见下面第 9 条）。

**突变路线后来改过一次**（v1 落地之后）：从 `industrious × unweary`、0.06 + 避雷针，改成
`anachrone × glacial`、0.04 + 雷雨天，理由见 §7.1/§7.2——这只蜂被定位成"大后期发力"，
而几率不是成本，进入条件才是。为此新增了一个突变条件类型 `forestry_oddities:thunderstorm`
（FCE 的七个条件类型里没有天气），实现位置见 §7.4。草稿里同时保留的避雷针条件最后没有进配方，
见下面第 8 条。

差异清单：

1. **不缓存相邻方块的列表**。本稿 §4.3.1 抄了能量蜂的 `WeakHashMap` 缓存 + 400 tick 全局清理；实现改为
   **每个 tick 重新洪水填充**，并且删掉了那个全局 tick 钩子。理由：一次填充是几十次方块实体查询
   （3×3×3 蜂房组 54 个外壳面），而缓存的失效时机（多方块重组、部件增删）比填充本身更容易出错；
   没有缓存，蜜蜂的相邻表永远是当前结构。
2. **面的方向按“真正贴着外壳的那一面”给出**。本稿 §4.3 与能量蜂都用
   `Direction.getNearest(蜂房pos − 目标pos)`，对蜂房组外壳的对角位置会就近取整、可能给错面；实现改成在
   洪水填充时记住是从哪个部件走出来的，因此拿到的永远是轴对齐的正确面。
3. **`discharge` 多了非正速度的显式保护**。速率里有平方，负速度会被平方成正数；实现先判
   `speed > 0 && multiplier > 0`，再算 `ceil`。速度 0 / 负速度 / `NaN` / 倍率 0 一律返回 0，无限速度封顶
   `Long.MAX_VALUE` 而不是抛异常。
4. **蜂房与蜂房组差 60 倍**（实现时发现，本稿没有写到）：蜂房自带的产量修正是 ×0.1，详见 §4.2 的补充说明。
   这不是 bug，也不修正，但 README 与 TODO 都写明了。
5. **验证方式**：用临时开发服务器自测（验完删除）而不是 `/forestry dump`：开发服务器日志里
   `Loaded 70 bee species`（69 + 本种）已经证明物种文件、taxon 与效果引用都解析成功，其余断言
   覆盖速率数学、物种数据、突变、真蜂房 + 真 FE 接收端、真 3×3×3 蜂房组 + 真配电被当邻居喂电。
   （那一次跑的是仍带回灌的版本，回灌删除后同一批断言里只有"余量交给配电"这一条换了路径，见第 9 条。）
6. **突变路线改成后期**（v1 之后）：父本换 `anachrone × glacial`、几率 0.04、并新增
   `forestry_oddities:thunderstorm` 条件类型。自测断言随之变成：突变的两个父本、几率 0.04、
   一个条件 `forestry_oddities:thunderstorm`，以及该条件类型在 `forestry:mutation_condition_type`
   注册表里确实存在。
7. **花型改成红石**（v1 之后）：`flower_type` 从 `forestry:flower_type_vanilla` 换成数据包花型
   `forestry_oddities:flower_type_redstone`。没有写 Java——`forestry:tag_flower_type` 加一个方块标签
   （红石块 / 红石矿 / 深层红石矿）就够了，刷新时由 `FlowerTypeManager` 和物种一起解析。
   两个注意点：花型的 lang 键是 `allele.forestry.flower_type.forestry_oddities.flower_type_redstone`
   （跨命名空间的格式，见 `GeneticTranslationKeys`）；绿宝石程序（`providesFlowers()`）会直接跳过花检，
   所以它依旧是"花源"问题的通用答案。
8. **避雷针条件没有进发布配方**。本稿 §7 原计划用 `forestry:requires_resource` 要求蜂房正下方是避雷针，
   再叠一个雷雨天；发布的 `voltaic_1.json` 只保留了 `forestry_oddities:thunderstorm`，避雷针退回成叙事里的意象
   （§7 的 JSON、条件表、§7.3 与 §14 的清单都已按实际文件更新）。`requires_resource` 的语义——从蜂房上报的位置
   开始向下走、跳过蜂房方块，蜂房组读的是多方块锚点（最低的那个角）——留在 §7 的技术说明里，将来要用位置类
   条件时仍然有效。
9. **余量回灌删除**（最后一步，v1 之后）：`doEffect` 里 `backflow(housing, push(...))` 那一行，连同 `backflow()`、
   `findTransmission()`、`controllerOf()`、`BACKFLOW_CALLS` 与它们用到的多方块 import 一起删掉；现在 `doEffect`
   只有 `push()` + `spawnSparks()` 两步，余量直接丢弃。理由见 §4.4：蜂房组的部件本来就落在外壳邻居里，回灌只是
   把同一件事再做一遍，还额外制造了一条"电缆先吃、蜂房组吃剩"的隐藏顺序。连带影响：效果描述（§9）里"回灌进
   自己的血管"那句改成了"没人接的散在风里，一滴也不回头"。

---

### 待你拍板的四件事（已全部按本稿实现）

1. **名字**：`伏打 / Voltaic` + 属名 `琥珀 / Electrum`（id `voltaicus` / `electrapis`）要不要换？
2. **`BASE_VOLTAGE = 192`**（默认速度 123 FE/tick）是否合适？还是直接对齐能量蜂的 400？
3. **~~`§4.4` 余量回灌配电**进 v1~~（已定：**删掉**，见 §4.4 与 §17 第 9 条；配电改为普通的外壳邻居）。
4. **突变父本**（已定：`anachrone × glacial`，0.04 + 雷雨天，见 §7）。
