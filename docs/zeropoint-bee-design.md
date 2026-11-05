# 零点蜂 / Zero-Point Bee — 设计稿（科幻方向）

> **状态：设计稿（未实现）。** 数据文件可直接照抄，代码给出契约与伪代码，验证清单见 §15。
> 与 `docs/voltaic-bee-design.md`（伏打蜂，工业/蒸汽朋克方向）是**同一格的替代方案**：两者只留一只，或做成前后两级（§8 末尾）。
> 基线：Minecraft 1.21.1 / NeoForge 21.1.230 / Forestry: CE `3.0.0-alpha9`。

---

## 0. "科幻"落在哪里：三个方向

科幻不是一个滤镜。要让它真的科幻，得把科幻放进**机制**里。三只蜂把科幻放在三个不同的位置：

| # | 名字 | 科幻落在 | 核心机制 | 成本 | 结论 |
|---|---|---|---|---|---|
| **A** | **零点蜂 / Zero-Point** | **能量的来源与存在形式** | 抽运真空零点涨落；每 0.5 s 放**一束脉冲**而不是细水长流；必须住在**暗室**（光、雨、热都让它退相干） | 低：1 个 effect 类 + 3 个 JSON | **本稿主推** |
| B | 广播蜂 / Broadcast | 输运方式 | 不贴邻送电，而是把电**广播**给半径内的端点（无线共振输电） | 中：要扫描并缓存目标，要处理性能 | 要另出稿 |
| C | 奇点蜂 / Singularity | 能量的**时间形态** | 一只会走路的电容：蓄满 20 s 才吐一次，吐不出去就憋着（`IEffectData` 用两个 int 存一个 long） | 中：效果变成有状态 | 要另出稿 |

**为什么主推 A**：这只蜂最科幻的地方不是"它能发电"，而是**它对环境的要求**——量子退相干这个设定，和 FCE 现成的基因（光照偏好、怕雨、温湿度耐受）**逐条对得上**。一对上，玩家就会觉得"这玩意儿是个仪器，不是一头牲口"，而这一点**不需要任何新系统**就成立了（§6.1）。

## 1. 定位

一句话：**一只把真空"整流"成 FE 的蜂，给本模组的电力部件和外部电缆喂电；它是仪器，所以必须住在实验室里。**

* 特殊效果与能量蜂同类（`careerbees:power`）：向蜂房外可接收的方块推电，一个字都不改的那部分只有"推电"这个大方向。
* **其余全部新设计**：速率公式（线性而非平方）、结算节奏（脉冲而非逐 tick）、输送顺序（先自持后外送）、苛刻条件（暗室）、分类学、基因组、产物、突变、外观、语言、配置。

## 2. 与能量蜂 / 伏打蜂的三角对比

| | CareerBees 能量蜂 | 伏打蜂（工业稿） | **本设计 零点蜂** |
|---|---|---|---|
| 效果 id | `careerbees:power` | `forestry_oddities:voltaic` | `forestry_oddities:zeropoint` |
| 速率 | `ceil(400 × speed²)` | `ceil(192 × speed²)` | **`ceil(320 × speed)`（线性）** |
| 结算节奏 | 每 tick 推一笔 | 每 tick 推一笔 | **每 10 tick 推一束（10 倍大）** |
| 余量去向 | 蜂房 1 tick 缓存 → 能量能力 | 先送外壳，**余量**回灌配电 | **先灌配电**（自持），余量送外壳，再余丢弃 |
| 运行条件 | 无耐受、日行、怕雨 | 无耐受、日行、怕雨、不见洞穴 | 无耐受、**夜行 + 必须够黑**、怕雨 |
| 获取 | 修理 × 时钟，0.05 | 错时 × 冰河，0.04 + 避雷针 + 雷雨天 | **恶魔 × 英雄，0.04** + 紫水晶块 |
| 定位 | 中期发电机 | 后期免维护基载电源（路线在 v1 之后改过） | **后期精密仪器** |

## 3. 设定（一行一句，全部对应到机制）

* 真空不空。每个频率上都有场在抖；只要你肯把它整流，它就会给你一套不欠账的电。量子电动力学管这叫**零点涨落**，工程学管这叫"还没人肯付费的那部分"。
* 零点蜂不发电，它**抽运**：翅腔是一对卡西米尔腔，被代谢锁相；两个腔之间的相位差被整流成电势差。所以它没有燃料、不看花、也不看你是谁——**它只在乎相干**。
* 而相干极脆：**光**是一次测量（§6.1 的 `NOT_GLOOMY`），**雨**是一次介电常数跳变（`tolerates_rain: false`），**热**是一把把声子（耐受 `NONE`）。它住的地方因此不是蜂房，是**暗室**。
* 它的产率是"理论上允许"与"工程上划算"之间的那点可怜差额：一辈子挣的电够点亮一座工厂，但**不许在它自己的房间里点灯**——这是它最像人类实验室的地方。

## 4. 标识与命名

| 内容 | id / key | 中文 | English |
|---|---|---|---|
| 属（taxon） | `nullapis` | 虚量 | Nullapis |
| 种 | `forestry_oddities:zero_point` | 零点 | Zero-Point |
| 效果 | `forestry_oddities:zeropoint` | 零点抽取 | Zero-Point Tap |
| 属名 key | `for.genus.bees.nullapis` | 虚量 | Nullapis |
| 种名 key | `allele.forestry.bee_species.forestry_oddities.zero_point` (+ `.desc`) | 零点 | Zero-Point |
| 效果名 key | `allele.forestry.bee_effect.forestry_oddities.zeropoint` (+ `.desc`) | 零点抽取 | Zero-Point Tap |

**翻译键规则（坑，见 `GeneticTranslationKeys`）**：等位基因名是"染色体在前、贡献者命名空间在后"，跨命名空间要四段：

`allele.<染色体ns>.<染色体path>.<对象ns>.<对象path>` → **`allele.forestry.bee_species.forestry_oddities.zero_point`**。

对照证据：CareerBees 的 `allele.forestry.bee_species.careerbees.quantum_charm`（它的文件是 `quantum_charm.json`，而 `species` 字段写的是带空格的 `"quantum charm"`——**键取的是注册 id 的 path，不是 `species` 字符串**）。伏打蜂稿 §3 有完整推导，这里不重复。

命名理由：

* 属名 `nullapis` = `nulla`（无）+ `apis`（蜂），"虚量"取"真空的量"；和 FCE 的 `monapis` / `rustapis` / `spongiforma` 同一路数（描述一个特征，不描述一种动物）。
* 种名走"形容词/概念"路线（`imperial` / `glacial` / `industrious`）。用 `zero_point` 而不是 `nullii`：**游戏里的 id 要能被玩家在命令里拼对**，`/forestry dump bee_species` 和 `/give` 之类都看得懂才有用。
* 物品名由 FCE 的 grammar 拼出：**零点蜂后 / 零点雄蜂 / 零点蜂**（`for.bees.grammar.*`），英文 Zero-Point Queen / Drone / Princess。
* 效果 id 不带 `bee_effect_` 前缀：翻译键里已经有染色体名 `bee_effect`，前缀只会重复（林业自家是 `forestry:bee_effect_glacial`，同命名空间少一段）。

## 5. 特殊效果 `forestry_oddities:zeropoint`

### 5.1 触发时机

`BeeHousingBeekeepingLogic#queenWorkTick` 的注释写得很直白（`BeeHousingBeekeepingLogic.java:262-263`）：

```java
// Effects only fire when queen can work.
this.effectData = queen.doEffect(this.effectData, this.housing);
```

* **每 tick 调用一次**，位置在 550 tick 的"工作周期"节流**之前**（`265-268`），所以效果和"工作周期"无关，不要按周期算账；
* 只有能干活时才调 —— 下雨、夜里（对夜行种是白天）、太亮、没花、气候超限，FCE 已经挡住了，**effect 里不要再判断一遍**；
* `effectData` 会被持久化（`compoundNBT` 里的 `Throttle` 旁边就存着它），但**本设计不用它**（§5.3）：零点蜂是无状态的。

### 5.2 速率：线性，不是平方

```
speed = genome.getActiveValue(BeeChromosomes.SPEED)
      × 蜂房修正.modifyProductionSpeed(genome, 1.0F)        // 框架、电路巢础程序
rate  = ceil(320 × speed × config.zeropointBeePowerMultiplier)   // FE / tick
```

* 取整仍用 `new BigDecimal(double).setScale(0, RoundingMode.CEILING).toBigIntegerExact()`，理由同能量蜂：`0.8²` 的二进制值是 0.64000…01，不走精确值就会差 1。
* 生产速度修正为 0 时按 1 处理（能量蜂同款保险）。
* **为什么线性**：能量蜂是 `∝speed²`，而框架和电路巢础程序乘的是 **speed**，于是"框架 × 速率"是二次的——三张生产框架能把能量蜂推到 16 384 FE/tick。零点蜂线性，所以同样的堆叠只到 2 048：

| speed | 来源 | rate (FE/tick) | 单束（×10） | 折合 FE/s |
|---|---|---|---|---|
| 0.3 | 慢速等位基因 | 96 | 960 | 1 920 |
| 0.6 | 较慢 | 192 | 1 920 | 3 840 |
| **0.8** | **本种默认** | **256** | **2 560** | **5 120** |
| 1.0 | 正常 | 320 | 3 200 | 6 400 |
| 1.2 | 快速 | 384 | 3 840 | 7 680 |
| 1.4 | 更快 | 448 | 4 480 | 8 960 |
| 1.7 | 最快等位基因 | 544 | 5 440 | 10 880 |
| 1.2 | 0.8 × 1.5（低压程序） | 384 | 3 840 | 7 680 |
| 2.0 | 0.8 × 2.5（高压程序） | 640 | 6 400 | 12 800 |
| 3.0 | 0.8 × 1.5 × 2.5 | 960 | 9 600 | 19 200 |
| 6.4 | 0.8 × 2³（三张生产框架） | 2 048 | 20 480 | 40 960 |

* 0.8 速时与能量蜂**完全同功率**（256/t），之后差距随速度线性拉开——这是"等价交换"：零点蜂换来的是**不失控的曲线**，付出的是**暗室 + 夜班 + 怕雨**（§6）。
* 对照伏打蜂（`∝speed²`，三张框架 7 865/t）：零点蜂的上限约是它的 1/4，但它在**任何**堆叠下都不会把服务器电成负数。

### 5.3 脉冲结算：为什么要"憋一炮"

不是每 tick 推 `rate`，而是**每 10 tick 推 `rate × 10`**：

```
budget = rate × PULSE_TICKS        // 默认 10 → 2 560 FE（0.8 速）
```

四个理由：

1. **它是电容，不是水龙头。** 设定上它先充电再击穿；机制上这就是让"一束"变成可感知的事件（粒子闪一下、声音响一下），而不是背景噪音。
2. **对下游是道真实的工程题。** 一次 2 560 FE 的转账要求接收端"这一 tick 吃得下"：MI 的电流上限、Mekanism 的线缆吞吐、Flux Networks 的接口速率都会在这一 tick 被问到。玩家想让零点蜂出力，得给它配一条**吃得下整束**的线——这是它区别于能量蜂的玩法钩子。
3. **长整型通道才真的有用**（§5.8）：一束是一笔交付，不该被 int 切片。
4. **不需要状态**。用 `gameTime` 取模，不计数、不写 NBT、不写 `effectData`：存档重载、区块卸载都不会留下半个电容，也不会有"读档时电荷翻倍"这类经典 bug。

```java
/** 每座蜂房按自己的相位放电：相位由方块位置决定，不是全局同步。 */
static boolean isDischargeTick(Level level, IBeeHousing housing) {
    return Math.floorMod(level.getGameTime() + housing.getBlockPos().asLong(), (long) PULSE_TICKS) == 0L;
}
```

* **相位为什么要掺位置**：如果所有蜂房都按 `gameTime % 10` 放假，几千只零点蜂会在同一 tick 集体推矿——TPS 的经典尖刺。掺进 `BlockPos.asLong()` 后，每座蜂房落在 10 个 tick 之一，天然错峰，且**相位稳定**（同一个位置永远是同一个 tick），玩家看到的节奏是恒定的。
* **代价（故意留着）**：如果停机的正是下一束应该结算的那 10 tick 里的某几 tick（下雨、气候超限、光照超了），这一段窗口的电就**没了**——不像能量蜂那样"少推一点"。脉冲式就是这样，最多损失一束；写在 tooltip 里比让玩家自己去猜好。

### 5.4 输送顺序：先自持，后外送

一束的预算 `budget` 按固定顺序花掉：

```
1) selfSupply: 蜂房组配电（AlvearyTransmissionBlockEntity.getEnergyHandler()）
2) export:     蜂房组外壳外一圈相邻方块，逐个 LongEnergy.find
3) 丢弃
```

与伏打蜂**顺序相反**（伏打蜂是"先外送、余量回灌"），这不是随意：

* 配电是蜂房组里**唯一**的电源总线，电路巢础靠它吃饭（§12 的闭环）。零点蜂的设定是"先稳住自己的谐振腔"，机制上就是**先把自己的实验室付费**；
* 配电缓存只有 1 000（远小于一束 2 560），所以顺序几乎不影响"外部能拿多少"——满则溢出到外壳；
* 结果是：**不下电缆也永远能自转**，接了电缆则电缆拿大头。这比"靠余量"更可预测。

细节：

* 外壳 = 整座蜂房组外侧一圈（洪水填充所有共享 `getBeeInventory()` 的 `IBeeHousing`，取它们不是蜂房的邻居）。能量蜂与伏打蜂同一实现，`WeakHashMap<IBeeHousing, List<BlockPos>>` 缓存 + 400 tick 清理；
* `side` 永远取六向之一（从蜂房指向目标），不给接收端传 `null`；
* 顺序不去抖：同一座蜂房一生之内相邻表稳定，玩家看到"先接上的先吃电"。

### 5.5 粒子与音效

```java
Vec3 fx = housing.getBeeFXCoordinates();
// 常态：慢速上飘的紫白火星，每束 4 颗
serverLevel.sendParticles(ParticleTypes.END_ROD, fx.x, fx.y, fx.z, 4, 0.4D, 0.4D, 0.4D, 0.01D);
// 击穿：一发白闪
serverLevel.sendParticles(ParticleTypes.FLASH, fx.x, fx.y, fx.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
// 每 4 束一声充电音（40 tick 一次，不会吵）
serverLevel.playSound(null, fx.x, fx.y, fx.z, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 0.35F, 1.9F);
```

* `END_ROD`（紫白）而不是 `ELECTRIC_SPARK`（黄白）：后者是"电"，前者是"场"。这也是与伏打蜂最快的视觉区分。
* `FLASH` 只在击穿那一 tick 出现，所以"节奏感"来自粒子本身，不需要额外的 HUD。
* 音效周期 `PULSE_TICKS × 4 = 40 tick`（约 2 s 一次），音量 0.35：够听见，不会成音乐。想更安静就把这一行删掉，不影响逻辑。

### 5.6 显性、组合性、codec

| 钩子 | 取值 | 理由 |
|---|---|---|
| `isDominant()` | `false` | 效果隐性：杂交后代可能把它藏起来，想要稳定产出就养纯血 |
| `isCombinable()` | 默认 `false` | 一只蜂一个主效果，不与其它 mod 的效果叠成"复合发电厂" |
| `codec()` | 不实现 | 纯代码注册。以后要让整合包调参，加 `rate` / `pulse` / `order` 三个字段即可 |
| `validateStorage` | 默认 | 无状态 |
| `doFX` | 默认（FCE 的蜂房粒子） | 自定义粒子走 `doEffect` 的服务端 `sendParticles`（§5.5） |

### 5.7 代码契约

```java
package com.zhichaoxi.forestryoddities.content.apiculture;

public final class ZeroPointBeeEffect implements IBeeEffect {
    public static final ResourceLocation ID =
        ResourceLocation.fromNamespaceAndPath(ForestryOddities.MOD_ID, "zeropoint");

    /** FE/tick at speed 1.0. Linear in speed on purpose - see §5.2. */
    private static final int BASE_INDUCTION = 320;
    /** Ticks per discharge. 10 = two bursts per second; sustained output is unchanged. */
    static final int PULSE_TICKS = 10;
    /** One charging tone every four bursts. */
    private static final int SOUND_TICKS = PULSE_TICKS * 4;

    @Override public boolean isDominant() { return false; }

    @Override
    public IEffectData doEffect(IGenome genome, IEffectData data, IBeeHousing housing) {
        if (!(housing.getLevel() instanceof ServerLevel level)) return data;
        if (!isDischargeTick(level, housing)) return data;       // §5.3
        long budget = induction(genome, housing) * PULSE_TICKS;  // §5.2
        budget = selfSupply(housing, budget);                    // §5.4 step 1
        budget = export(level, housing, budget);                 // §5.4 step 2
        dischargeFX(level, housing);                             // §5.5
        return data;                                             // stateless: nothing to persist
    }

    /** Each housing fires on its own phase so a base full of these bees does not spike one tick. */
    static boolean isDischargeTick(Level level, IBeeHousing housing) { ... }

    /** ceil(320 * speed * multiplier); speed already carries the housing's production modifiers. */
    static long induction(IGenome genome, IBeeHousing housing) { ... }

    /** Step 1: top the group's own wiring up first, return the budget that is still unspent. */
    static long selfSupply(IBeeHousing housing, long budget) { ... }

    /** Step 2: the multiblock shell outwards, exactly like the energy bee. */
    static long export(Level level, IBeeHousing housing, long budget) { ... }

    static void dischargeFX(ServerLevel level, IBeeHousing housing) { ... }
}
```

注册（`integration/ForestryOdditiesPlugin`）：

```java
@Override
public void registerApiculture(IApicultureRegistration apiculture) {
    apiculture.registerBeeEffect(ZeroPointBeeEffect.ID, new ZeroPointBeeEffect());
}
```

`speed` 的取法与能量蜂一致（`CareerEffect#getSpeed`）：`genome.getActiveValue(SPEED) × 修正的 modifyProductionSpeed(genome, 1.0F)`，修正为 0 时按 1 处理。本模组侧用 `IForestryApi.INSTANCE.getHiveManager().createBeeHousingModifier(housing)` 拿聚合修正。

### 5.8 为什么用 long 通道（这次的实话）

单束默认 **2 560 FE**，高速/双程序可到 **9 600**，三张生产框架 20 480。仍然远小于 int 上限。long 买的是三件事：

1. **一笔交付不该被切片**：`LongEnergy.find` 把整束交给 Mekanism（J）、Modern Industrialization（EU）、Flux Networks / GrandPower 的**原生 long** API；走 int 兜底会被某些实现的单次插入上限吃掉零头，MI 的 EU 换算更是会在小数上丢电。
2. **脉冲天然是大额**：让能量通道从第一天就按"单笔"设计，而不是"每次一小口"。
3. **v2 蜂房电容**（§16）：容量 `Long.MAX_VALUE`，五大 mod 的电缆一次取走。

## 6. 基因组

| 染色体 | 值 | 显性 | 设计理由 |
|---|---|---|---|
| `speed` | 0.8 | **是** | 线性公式下速度不再失控；0.8 时正好与能量蜂同功率（256/t），杂交后代立刻有用 |
| `lifespan` | 70（最高档） | 否 | "场不会衰减"：它是仪器，不该像工蜂那样短命；隐性使它难以白拿 |
| `fertility` | 2 | 是 | 常见档，方便量产与做突变 |
| `temperature_tolerance` | `NONE` | 否 | **声子退相干**：要么蜂房组控温，要么电路巢础琥珀程序（25 FE/周期） |
| `humidity_tolerance` | `NONE` | 否 | 同上（湿度也会让腔长漂） |
| `activity` | `forestry:activity_nocturnal` | 否 | **夜行 + 必须够黑**，见 §6.1 —— 本设计最有辨识度的一条 |
| `cave_dwelling` | `true` | 是 | 它**不要求**天光：暗室可以完全封顶（§12）。顺带省掉硅程序（15 FE/周期） |
| `tolerates_rain` | `false` | 是 | 雨水是介电常数跳变 = 一次退相干；铜程序（10 FE/周期）解决 |
| `flower_type` | `flower_type_vanilla` | 否 | 普通花，便宜；设定上花蜜是**谐振腔的冷却剂**，所以还是得种花 |
| `bee_effect` | `forestry_oddities:zeropoint` | 否 | 核心 |
| `pollination` | 5（最低档 5） | 否 | 它是仪器，不巡游、不授粉；产物线也刻意压低 |
| `territory` | `[9, 6, 9]` | 否 | 只影响授粉与粒子范围，不影响放电（放电只看外壳相邻面） |

### 6.1 "必须够黑"到底是怎么算的（照源码说）

这不是设定，是三条现成机制：

1. `forestry:activity_nocturnal` 注册时带 `LightPreference.DARK`（`ApicultureForestryPlugin.java:204`）；
2. `Bee#getCanWork` 里，只要蜂房所在位置的**方块光照 > 11**（`Constants.APIARY_MIN_LEVEL_LIGHT`），夜行种就拿到 `NOT_GLOOMY`，**当 tick 不干活**（`Bee.java:179-182`）；
3. 时间检查：夜行种只在 `dayTime ∈ [12000, 24000)` 工作，白天报 `NOT_NIGHT`（`SingleActivityType`）。

所以默认状态下它要求：**夜里 + 方块光照 ≤ 11 + 有屋顶 + 恒温 + 有花**。一支火把（光照 14）放在三格内就能让它停机——这不是 bug，是"光 = 测量 = 退相干"的机制化，也是初学者最容易踩的坑（§16）。

**官方后门**：`ALWAYS_ACTIVE`（末影程序，15 FE/周期）在 `Bee.java:170` 处**整段跳过**时间与光照检查——因为"一直醒着"就不需要判断醒没醒。买了它，零点蜂立刻能在白天、在灯下工作，但气候（琥珀 25）和雨（铜 10）还得自己解决。这个设计意图很明确：**暗室的代价可以用 FE 赎买**，而赎回的钱正是它自己挣的（§12）。

### 6.2 为什么把"娇气"放在这里

四项"娇气"（怕光、怕雨、无耐受、夜行）分别对应本模组电路巢础的三条程序（末影 15 / 铜 10 / 琥珀 25 = **50 FE/周期**）。也就是说：**它的缺点，正好是本模组另一件物品的功能清单**。这不是巧合，是 §12 闭环的入口。

## 7. 物种元数据

| 字段 | 值 | 说明 |
|---|---|---|
| `genus` / `species` | `nullapis` / `zero_point` | 文件 `data/forestry_oddities/bee_species/zero_point.json` → id `forestry_oddities:zero_point` |
| `dominant` | `false` | 物种等位基因隐性（与大多数后期种一致） |
| `glint` | `false` | 它不发光，它吸光；身份靠 `END_ROD` 粒子 |
| `secret` | `false` | 不藏 |
| `complexity` | `2` | 图鉴研究/分析成本档位（后期种，按 FCE 默认 0 往上抬一档） |
| `authority` | `zhichaoxi2006` | 图鉴署名 |
| `escritoire_color` | `8015359`（`0x7A4DFF`） | 写字桌条目色，紫 |
| `temperature` / `humidity` | `normal` / `normal` | 理想气候普通，但耐受 `NONE`，所以照样娇气 |
| `body` | `0x8A4BE0` = **9063392** 紫罗兰 | 腹部底色（**已按 §9.1 的实物预览校准**；原稿的 `#2E1B5E` 在 16×16 下读成黑色） |
| `stripes` | `0xE8FBFF` = **15268863** 冷白 | 腹部环带（"电势差"那一侧） |
| `outline` | `0x1A0F2E` = **1707822** 深紫黑 | 描边（轮廓线，压住 16×16 的边缘） |
| `products` | `forestry:honey_comb` 0.15 | 它不太会酿蜜 |
| `specialties` | `minecraft:amethyst_shard` 0.20、`minecraft:nether_quartz` 0.08 | 谐振晶体 / 压电石英 |
| `jubilance` | 省略（默认 `forestry:default`） | 不加额外条件 |

十进制换算（json 里直接写数字，不用 `0x`）：

* `0x8A4BE0` = `138×65536 + 75×256 + 224` = **9063392**
* `0xE8FBFF` = `232×65536 + 251×256 + 255` = **15268863**
* `0x1A0F2E` = `26×65536 + 15×256 + 46` = **1707822**

## 8. 突变

`data/forestry_oddities/recipe/bee_mutation/zeropoint_1.json`：

```json
{
  "type": "forestry:bee_mutation",
  "chance": 0.04,
  "conditions": [
    { "type": "forestry:requires_resource", "blocks": [ { "Name": "minecraft:amethyst_block" } ] }
  ],
  "first": "forestry:demonic",
  "id": "forestry_oddities:bee_mutation/zeropoint_1",
  "result": "forestry_oddities:zero_point",
  "second": "forestry:heroic"
}
```

| 项 | 值 | 理由 |
|---|---|---|
| 父本 | `forestry:demonic` × `forestry:heroic` | 两个极端（地狱的炽热 + 英雄的"圣"）叠在一起，中间的差就是"真空"——**设定上的冷幽默，机制上的"后期门票"**（两族都要先养出来） |
| 几率 | 0.04 | 后期低概率档；`demonic`/`heroic` 本身就在帝国/地狱两条长线上 |
| 条件 | 蜂房**正下方**是紫水晶块 | 晶振/谐振腔的隐喻，成本不高（紫水晶洞），但它把"这只蜂需要一块晶体"变成看得见的要求 |
| 条件实现 | FCE 的 `forestry:requires_resource`（`MutationConditionRequiresResource`） | 现成条件，不需要写代码 |

**注意**：`requires_resource` 检查的是"从蜂房向下走，跳过蜂房方块之后的第一个方块"。**蜂房组控制器在多层结构里，下面仍然是蜂房组方块**，所以第一次繁殖要在普通蜂房（apiary / bee house）里做——写进 `.desc` 提示（§10）。

**变体**：

* 若伏打蜂（`docs/voltaic-bee-design.md`）也实现了，可做成二级线：`forestry_oddities:voltaicus` × `forestry:demonic`，0.06，同样要紫水晶块 —— 从"工业电"进化到"真空电"，两条稿子就串成一条进化链。
* 若嫌 0.04 太狠：`forestry:demonic` × `forestry:industrious`，0.05（中期提前拿到，但叙事上就没有"两极端相消"了）。

## 9. 外观与渲染

* FCE 的 `ForestryBeeItem#getColorFromItemStack` 按 tint 索引上色：**0 = outline、1 = body、2 = stripes**，其余索引返回白色。三种颜色写进 species JSON 就够，**不需要任何材质、模型或客户端注册**。
* `glint: false`：附魔光会把它读成"魔法"，而这只蜂是物理。身份交给 `END_ROD` 的紫白火星（§5.5）。
* 可选 v2：自定义蜂模型 overlay（`IClientRegistration#setCustomBeeModel` + 一张 16×16 PNG），本设计不要求。

### 9.1 配色校准（看过实物预览之后改的）

tint 不是"覆盖"，是**乘法**：屏幕上看到的是 `贴图颜色 × 配色`。FCE 的身体层贴图本身是米褐色（`#EDE2D7` / `#CEB39B`），乘完亮度还要再掉 30–40%。三条结论：

1. **深色配色会直接读成黑色。** 本稿最初写的 `#2E1B5E`（深空紫）在 16×16 下就是一只黑蜂，紫的辨识度为零 —— 这是看图才知道的坑，光看十六进制发现不了。校准后取 `#8A4BE0`：乘完米褐底仍是明确的紫罗兰，白环带读作"电弧"，也不和奶油色翅膀打架。
2. **四层贴图里只有两层能上色**：腹部底色（`body`）与腹部环带（`stripes`）。翅膀、头、腿、王冠都在细节层里，是固定的奶油白 + 深褐 —— 任何配色都改不掉。所以"深空紫 + 冷白电弧"里那块奶油色翅膀**永远在**，配色只能跟它共存；真要连翅膀一起换，只能走 §9 末尾的自定义模型。
3. **描边层压在最上面**（叠加时它盖住了身体边缘），所以 `outline` 决定 16×16 下的边缘是否清晰。紫色身体配 `#1A0F2E` 刚好把轮廓勾出来；原来的 `#0B0714` 和暗身体糊成一团。

预览图在 `docs/previews/`（渲染脚本同目录，可复现）：

| 图 | 内容 |
|---|---|
| `zeropoint-bee-tint.png` | 三种生活史（蜂后 / 雄蜂 / 公主）× 四套配色的对照表，含 1:1 实际大小 |
| `bee-layer-map.png` | 四层分别涂成红 / 绿 / 蓝 / 白，看清"哪一层画了哪块" |
| `zeropoint-bee-candidates.png` | 五个腹部底色候选（P0 稿 v1 → P4 靛紫） |

`body` / `stripes` / `outline` 这三个数字就是**全部美术工作量**：换配色只改这三个数，不用碰任何 PNG。

## 10. 语言

`assets/forestry_oddities/lang/en_us.json`：

```json
{
  "for.genus.bees.nullapis": "Nullapis",
  "allele.forestry.bee_species.forestry_oddities.zero_point": "Zero-Point",
  "allele.forestry.bee_species.forestry_oddities.zero_point.desc": "It does not make honey. It rectifies the vacuum, and pays for the privilege in coherence. Keep it dark.|Field Notes, vol. 3",
  "allele.forestry.bee_effect.forestry_oddities.zeropoint": "Zero-Point Tap",
  "allele.forestry.bee_effect.forestry_oddities.zeropoint.desc": "One pulse every half second: the hive's own wiring first, then anything touching the shell. Light, rain and heat break the resonance.",
  "forestry_oddities.zeropoint.rate": "Induction: %1$s FE / tick"
}
```

`assets/forestry_oddities/lang/zh_cn.json`：

```json
{
  "for.genus.bees.nullapis": "虚量",
  "allele.forestry.bee_species.forestry_oddities.zero_point": "零点",
  "allele.forestry.bee_species.forestry_oddities.zero_point.desc": "它不酿蜜。它把真空整流，代价是相干性。请把它养在暗处。|《野外记录》第三卷",
  "allele.forestry.bee_effect.forestry_oddities.zeropoint": "零点抽取",
  "allele.forestry.bee_effect.forestry_oddities.zeropoint.desc": "每半秒放电一次：先喂蜂房组自己的线路，再喂贴着外壳的方块。光、雨与热都会打断谐振。",
  "forestry_oddities.zeropoint.rate": "感应：每 tick %1$s FE"
}
```

* 最后一条是可选的（分析仪页面要显示实时速率才需要自定义 analyzer plugin，不在 v1 范围）。
* `|` 之后是图鉴里的署名行（伏打蜂稿同款写法）。

## 11. 配置

`config/forestry_oddities-common.toml`，`[apiculture]` 段追加两项：

```java
ZEROPOINT_BEE_POWER_MULTIPLIER = builder
    .comment(
        "Output multiplier of the Zero-Point Bee, applied on top of ceil(320 * speed).",
        "1.0 keeps the designed balance (256 FE/tick at speed 0.8, the same as the energy bee",
        "at that speed). 0 disables generation without removing the bee. Read every effect tick.")
    .defineInRange("zeropointBeePowerMultiplier", 1.0D, 0.0D, 10.0D);

ZEROPOINT_PULSE_TICKS = builder
    .comment(
        "Ticks between discharges. Sustained output is unchanged: the budget is scaled by this",
        "value, so 1 is a trickle (one push per tick, like the energy bee) and 40 is one big",
        "burst every two seconds. Lower values are gentler on downstream throughput limits,",
        "higher values look and sound better and stress the long-energy path.")
    .defineInRange("zeropointPulseTicks", 10, 1, 40);
```

* 两项都是 COMMON（与本模组现有配置一致），每次读值，改完立即生效。
* `pulseTicks` 的意义：**它不改变总产出，只改变交付的颗粒度**。整合包作者调它是为了手感/性能，不是为了平衡。

## 12. 玩法闭环与暗室

```
        ┌──────────── 暗室：封顶、无灯、无雨、恒温 ────────────┐
        │                                                     │
        │   ╔═════════════ 蜂房组 ═════════════╗               │
        │   ║  零点蜂 ──束──▶ 蜂房组配电 ──────╫──▶ 电路巢础 │
        │   ║               └───余量──▶ 外壳外电缆 ──▶ 外部机器│
        │   ╚══════════════════════════════════╝               │
        │        方块光照 ≤ 11 · 只在夜里开工（除非买末影程序）│
        └─────────────────────────────────────────────────────┘
```

数值核算（速度 0.8，一个工作周期 = 550 tick）：

| 项目 | FE / 工作周期 |
|---|---|
| 零点蜂产出 | 256 × 550 = **140 800** |
| 电路巢础：末影 15 + 铜 10 + 琥珀 25 | **50** |
| 其中一束的大小 | 2 560 |
| 配电缓存 / 电路巢础缓存 | 1 000 / 1 000 |

结论：

* 产出是"让自己能干活"的成本的 **2 816 倍**。和伏打蜂一样，这不是"发电厂"，是**免维护的基载电源**：接上电缆，它就一直往你的总线里滴电；
* 真正的收益在**余量**：配电满（1 000）之后，一束里剩下的 1 500+ 全都溢出到外壳上的电缆；
* 若玩家不买末影程序，它只在夜里出力：有效产出减半到约 70 400 FE/周期，仍然远远够用。**"要不要为它建暗室"因此是一个真正的选择题，而不是收费关卡。**

**暗室最低配**（v1，不碰新方块）：

1. 蜂房组四面 + 顶部用任意不透光方块封住（`cave_dwelling: true` 让它不需要天光）；
2. 室内不放光源，或用红石灯 + 拉杆（夜里关掉）——更省事的办法是**末影程序**；
3. 顶上必须有实体方块挡雨（`tolerates_rain: false`）；或铜程序；
4. 温湿度靠蜂房组自带的加热/风扇，或琥珀程序；
5. 花：普通花床（`flower_type_vanilla`）；怕麻烦就绿宝石程序（35 FE/周期）。

## 13. 文件清单

**新增代码（1 个）**

| 路径 | 内容 |
|---|---|
| `src/main/java/com/zhichaoxi/forestryoddities/content/apiculture/ZeroPointBeeEffect.java` | §5.7 的效果类 |

**修改代码（2 个）**

| 路径 | 改动 |
|---|---|
| `integration/ForestryOdditiesPlugin.java` | 新增 `registerApiculture` 重写 |
| `config/ForestryOdditiesConfig.java` | 新增 `zeropointBeePowerMultiplier` / `zeropointPulseTicks` |

**新增数据（3 个）**

```
data/forestry_oddities/taxon/nullapis.json
data/forestry_oddities/bee_species/zero_point.json
data/forestry_oddities/recipe/bee_mutation/zeropoint_1.json
```

`nullapis.json`：

```json
{ "parent": "apidae", "name": "nullapis", "rank": "genus" }
```

`zero_point.json`（完整）：

```json
{
  "authority": "zhichaoxi2006",
  "body": 9063392,
  "stripes": 15268863,
  "outline": 1707822,
  "complexity": 2,
  "dominant": false,
  "escritoire_color": 8015359,
  "genus": "nullapis",
  "glint": false,
  "secret": false,
  "species": "zero_point",
  "temperature": "normal",
  "humidity": "normal",
  "products": [
    { "chance": 0.15, "item": "forestry:honey_comb" }
  ],
  "specialties": [
    { "chance": 0.20, "item": "minecraft:amethyst_shard" },
    { "chance": 0.08, "item": "minecraft:nether_quartz" }
  ],
  "genome": {
    "forestry:speed":                 { "value": 0.8,  "dominant": true  },
    "forestry:lifespan":              { "value": 70,   "dominant": false },
    "forestry:fertility":             { "value": 2,    "dominant": true  },
    "forestry:temperature_tolerance": { "value": "NONE", "dominant": false },
    "forestry:humidity_tolerance":    { "value": "NONE", "dominant": false },
    "forestry:activity":              { "value": "forestry:activity_nocturnal", "dominant": false },
    "forestry:cave_dwelling":         { "value": true, "dominant": true  },
    "forestry:tolerates_rain":        { "value": false, "dominant": true  },
    "forestry:flower_type":           { "value": "forestry:flower_type_vanilla", "dominant": false },
    "forestry:bee_effect":            { "value": "forestry_oddities:zeropoint",  "dominant": false },
    "forestry:pollination":           { "value": 5,    "dominant": false },
    "forestry:territory":             { "value": [9, 6, 9], "dominant": false }
  }
}
```

**语言（2 个文件各加 6 行）**：见 §10。
**文档（2 个）**：`README.md` 新增本节（机制 + 数值 + 验证结果）；`TODO.md` 状态改为已实现。

## 14. 实现步骤

1. `ForestryOdditiesConfig` 加两项配置（先加，后面代码直接读）。
2. `ZeroPointBeeEffect`：速率 + `isDischargeTick` + 外壳推送能跑（`selfSupply` 先返回 budget 原值）。
3. `ForestryOdditiesPlugin#registerApiculture` 注册。
4. 三个 JSON + 两份语言。
5. §15 第 1–4 项（载入 / 速率 / 条件网格 / 脉冲时序）。
6. 补 `selfSupply`（§5.4）+ §15 第 5 项。
7. 突变（§15 第 6 项）。
8. README / TODO 文档化；删掉所有临时监听器（本仓库惯例）。

## 15. 验证清单

沿用本仓库做法：开发服务器 + 临时 listener，验完删除。

1. **载入**：`/forestry dump bee_species` 出现 `forestry_oddities:zero_point`；日志无 `Skipping bee_species …`；`/forestry dump mutations` 出现 `forestry_oddities:bee_mutation/zeropoint_1`。
2. **速率**：speed 0.8 断言 sink 平均每 tick 收到 256 ± 0.2（按 10 tick 一束合计 2 560）；speed 1.7 断言 544；配置 `2.0` 断言 512。
3. **脉冲与相位**：断言"同一蜂房每 10 tick 恰好推一束、束内金额 = rate × 10、其余 9 tick 为 0"；两座相邻蜂房**不同 tick** 放电（相位由位置决定）。
4. **条件网格**：白天（夜行）、火把照到方块光照 ≥ 12（`NOT_GLOOMY`）、下雨（怕雨）、气候超限（无耐受）——四种情况分别断言"零产出"，防止把 FCE 的错误判断写两遍还写错。
5. **自持顺序**：外壳六个面全堵死 + 配电缓存从 0 涨到 1 000 并停住；电路巢础断电后断言它自己重新工作。再接一条 sink 到外壳，断言"配电先满、余量才出外壳"。
6. **突变**：紫水晶块放蜂房正下方 + `chance` 临时改 1.0 → 一次出零点蜂；拿掉紫水晶块 → 繁殖 100 次都不出（再恢复 0.04 跑一次正常概率）。
7. **闭环**：蜂房组（配电 + 三件套程序 + 零点蜂）跨 5 个工作周期，断言电路巢础从未因断电停机；关掉末影程序后断言"只在夜里产电"。
8. **回归**：`.\gradlew.bat --offline build` 通过；开发服务器启动无新错误；现有 14 个电路巢础程序、蜂房组框架外壳、配电行为不变。

## 16. 风险与平衡

| 风险 | 说明 | 应对 |
|---|---|---|
| **光照陷阱** | 方块光照 > 11 就停机，一支火把也会踩雷 | `.desc` 明写"Keep it dark"；README 的"暗室"一节；末影程序是官方出路 |
| **玩家嫌麻烦** | 四项条件全开时，它不如能量蜂省心 | 这是设计目标（后期仪器 ≠ 中期发电机）；三条程序的赎买成本合计 50 FE/周期，相对 140 800 的产出可忽略 |
| **脉冲丢电** | 停机窗口恰好覆盖结算 tick 时，那一束的电没了 | 最多损失一束（10 tick）；tooltip 说明；`pulseTicks` 可调小以减少颗粒度 |
| **相位实现错误** | 若忘了掺位置，全服同 tick 集体脉冲 | §5.3 的 `isDischargeTick` 就是为此存在；§15 第 3 项专门测它 |
| **线性公式被嫌弱** | 三张生产框架只有 2 048 FE/t（能量蜂 16 384） | 默认值与能量蜂同功率（256/t）；要狂暴就调 `zeropointBeePowerMultiplier`，别改公式 |
| **突变成本** | 恶魔 × 英雄 + 0.04 是真正的前期投入 | 提供中期变体（§8）；若两条稿子都实现，可改成"伏打 → 零点"的二级线 |
| **与能量蜂同场** | 两个模组都在时玩家会叠加发电蜂 | 无冲突，功率叠加是玩家选择 |

## 17. 待你拍板

1. **方向**：就选 A（零点蜂），还是想先看 B（无线广播）/ C（蓄能奇点）的稿子？
2. **名字**：`零点 / Zero-Point` + 属名 `虚量 / Nullapis`（id `zero_point` / `nullapis`）要不要换？
3. **`BASE_INDUCTION = 320`**（默认速度 256 FE/tick，与能量蜂同功率）合不合适？还是要更弱（256）或更强（400）？
4. **突变**：恶魔 × 英雄 0.04 + 紫水晶块（后期），还是改成中期（恶魔 × 敬业 0.05）／二级线（伏打 × 恶魔 0.06）？
