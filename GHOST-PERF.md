# GHOST-PERF.md —— 残影性能优化：裁剪渲染图层（t19）

> 交付：`client/AfterimageRenderer.java`（**唯一改动文件**）
> 未改动：`AfterimageHistory.java`、`EquipmentLayerRendererMixin.java`、`CapeLayerMixin.java`、
> `cyberware.mixins.json`、`core/**`、`network/**`、`resources/**`。未 commit。
> ⚠ **未真机验证**（见 §6）。

**本次只裁图层，段数与间距一个字节都没动**（实测见 §5）。

---

## 1. 瓶颈分析（带行号）

### 1.1 残影渲染的入口与主循环

`AfterimageRenderer.java`：

| 位置 | 内容 |
|---|---|
| `:62` | `MAX_GHOSTS = 8`（本次**未改**） |
| `:67` | `GHOST_STEP = 1`（本次**未改**） |
| `:98-109` | `onRenderLiving(RenderLivingEvent.Pre)` —— 唯一入口，整段 try/catch 保命，`finally`（`:107`）里 `ghostAlpha = 0` |
| `:135` | `int wanted = Math.min(MAX_GHOSTS, available / GHOST_STEP)` |
| `:154` | `for (int g = 0; g < wanted; g++)` —— 主循环，每个残影一次 |
| `:189` | `collector.submitModel(model, ghost, ...)` —— 每个残影提交一次**完整玩家模型** |
| `:202` | `for (RenderLayer<...> layer : layers)` —— **遍历全部图层**（本次的裁剪点） |
| `:208` | `layer.submit(...)` —— 每层一次回调 |

图层列表来源：`:151` `List<RenderLayer<...>> layers = accessor.cyberware$layers();`
—— 这是 `LivingEntityRendererAccessor`（`@Accessor`）对原版
`LivingEntityRenderer.layers` 字段的**只读**访问，本次**没有动它**。

### 1.2 图层有多少

真机日志（改动前那行在 `AfterimageRenderer.java:191`，改动后在 `:193`；两份日志引用的都是同一句
「残影图层」打印）实测玩家渲染器的图层列表共 **10** 个：

```
HumanoidArmorLayer, PlayerItemInHandLayer, ArrowLayer, Deadmau5EarsLayer, CapeLayer,
CustomHeadLayer, WingsLayer, ParrotOnShoulderLayer, SpinAttackEffectLayer, BeeStingerLayer
```

### 1.3 于是每帧的调用量

`wanted = 8`（稳态下历史攒够），每个残影：

- **1 次**主模型 `submitModel`（`:189`）
- **10 次**图层回调（`:202-208`）

一帧 = **8 次主模型 + 80 次图层回调**。第三人称开斯安威斯坦时，这就是
「9 个玩家模型 × 全套图层」里的「9」里的 8 个残影部分。

### 1.4 半透明渲染类型放大了代价

`:149` `RenderType renderType = RenderTypes.entityTranslucentCullItemTarget(texture);`
—— 主模型走的是**半透明**类型（要混合、要排序），比原版的不透明/cutout 类型贵。
护甲在残影里被 `EquipmentLayerRendererMixin` 换成 `armorTranslucent`
（`EquipmentLayerRendererMixin.java:68`），同样是混合类型。

### 1.5 目前零调控

`AfterimageRenderer.java` 里没有任何距离剔除、没有 LOD、没有帧率自适应；
`CyberwareConfig` 里也没有残影相关开关（本任务未核实配置文件以外的范围，
仅确认本文件内没有这些机制）。本次**不新增**这些（§7）。

---

## 2. 图层白名单与逐条理由

白名单只有两个（`AfterimageRenderer.java:245`）：

```java
private static boolean isGhostLayer(RenderLayer<?, ?> layer) {
    return layer instanceof HumanoidArmorLayer || layer instanceof CapeLayer;
}
```

判据是两条**可验证**的性质，不是感觉：

- **性质 A：半透明确实生效。** 该图层是否走「已经被我们改造成半透明」的通路
  —— `EquipmentLayerRenderer.renderLayers(...)`（被 `EquipmentLayerRendererMixin` 拦截，
  换成 `armorTranslucent` 并乘上 `ghostAlpha`）或 `CapeLayer`（被 `CapeLayerMixin` 拦截）。
- **性质 B：覆盖到绝大多数玩家。**

只有 A、B 同时成立的才保留。

| # | 图层 | 保留? | 性质 A（半透明是否生效） | 性质 B（普遍性） | 结论 |
|---|---|---|---|---|---|
| 1 | `HumanoidArmorLayer` | ✅ | **生效**：`renderArmorPiece` → `equipmentRenderer.renderLayers(...)`，被 `EquipmentLayerRendererMixin:53/:81/:111/:132` 四处注入覆盖（渲染类型 + 染色 + 纹饰） | **普遍**：穿甲就出现 | 保留。残影轮廓的视觉大头 |
| 2 | `CapeLayer` | ✅ | **生效**：`CapeLayerMixin:38` 专门为它做的（8 参 → 10 参 `submitModel` + 顶点 alpha） | 只有披风玩家有，但**没披风时它内部第一行就 return**，对其他人等于零成本 | 保留。已有专门注入，不保留会让那个 mixin 变成死代码 |
| 3 | `PlayerItemInHandLayer` | ❌ | **不生效**：走 `ItemInHandLayer.submitArmWithItem` → `item.submit(..., state.outlineColor)`，用的是**物品自己的渲染类型**，`ghostAlpha` 根本没传进去 → 残影里是**不透明的实心物品** | 普遍（手持物品） | 跳过。**视觉穿帮 + 成本不低**（见 §2.1） |
| 4 | `CustomHeadLayer` | ❌ | **不生效**：`SkullBlockRenderer.submitSkull(...)` / `state.headItem.submit(...)`，都不是我们的半透明通路 | 少数（南瓜/头颅） | 跳过 |
| 5 | `WingsLayer` | ❌ | **生效**（它走 `equipmentRenderer.renderLayers(WINGS, ...)`，同样被 mixin 覆盖） | **少数**：只有穿鞘翅的玩家 | 跳过（按「少数玩家才有」判据）。这是白名单里唯一的边界case，想恢复只需在 `:245` 加一个 `\|\|`，已在代码注释里写明 |
| 6 | `ArrowLayer` | ❌ | **不生效**：`StuckInBodyLayer.submit` 里循环提交**物品模型**（箭） | 少数/瞬时（身上插着箭） | 跳过 |
| 7 | `BeeStingerLayer` | ❌ | **不生效**：同 6（蜂刺） | 少数/瞬时 | 跳过 |
| 8 | `Deadmau5EarsLayer` | ❌ | **不生效**：`RenderTypes.entitySolid(...)`（不透明） | 极少数（deadmau5 账号的彩蛋） | 跳过 |
| 9 | `ParrotOnShoulderLayer` | ❌ | **不生效**：鹦鹉模型走自己的渲染类型 | 少数（驯了鹦鹉并让它站在肩上） | 跳过 |
| 10 | `SpinAttackEffectLayer` | ❌ | **不生效**：固定 `TEXTURE` + 自己的 `submitModel` | 瞬时（三叉戟激流旋转期间） | 跳过 |

### 2.1 为什么第 3 条（手持物品）值得单独说

- **成本**：`ItemInHandLayer.submit` 对**两只手各判一次** `!item.isEmpty()`，
  两只手都拿着东西时每个残影就是 **2 次物品模型提交**；8 段残影 = **一帧 16 次物品模型提交**。
  物品模型（尤其复杂 NBT 物品）的面数不低，这是被裁掉的图层里**最重**的一个。
- **视觉**：`item.submit(..., state.outlineColor)` 用的是物品自己的渲染类型，
  我们给残影算的 `color`（含 alpha）**没有**传进去 → 一个**完全不透明**的剑/方块会插在
  半透明的残影里。这既是穿帮，也让「残影」这个效果看起来像坏了。
- 因此它同时违反性质 A 和成本判据，跳过它没有争议。

### 2.2 captain 的初步判断 vs 本次结论

| captain 的分类 | 本次结论 | 是否推翻 |
|---|---|---|
| 保留候选：`HumanoidArmorLayer`、`CapeLayer` | 保留 | 一致 |
| 待判断：`PlayerItemInHandLayer` | **跳过**（性质 A 不成立 + 每帧 16 次物品提交） | 给出结论 |
| 待判断：`CustomHeadLayer` | **跳过**（性质 A 不成立 + 稀有） | 给出结论 |
| 跳过候选：`ArrowLayer`、`Deadmau5EarsLayer`、`ParrotOnShoulderLayer`、`SpinAttackEffectLayer`、`BeeStingerLayer` | 跳过 | 一致 |
| 跳过候选：`WingsLayer` | 跳过（**但注明它性质 A 是成立的**，只按稀有度跳过） | 保持一致，附证据 |

---

## 3. 实现方式

改动只有三处，全在 `AfterimageRenderer.java` 内：

1. 新增两个 import（`:12-13`）：`layers.CapeLayer`、`layers.HumanoidArmorLayer`。
2. 在残影循环的图层遍历里加一条 `continue`（`:202-206`）：

```java
for (RenderLayer<LivingEntityRenderState, ?> layer : layers) {
    if (!isGhostLayer(layer)) {
        // 非白名单图层不进残影 —— 白名单与逐条理由见 GHOST-PERF.md §2
        continue;
    }
    ((RenderLayer<...>) layer).submit(poseStack, layerCollector, ghost.lightCoords, ghost, ghost.yRot, ghost.xRot);
}
```

3. 新增白名单方法 `isGhostLayer`（`:245`），带完整理由注释与「以后怎么加回鞘翅」的指引；
   同时把原来那行只打一次的诊断日志（`:192-198`）扩展为**同时打印「全部图层」和「残影保留的图层」**
   —— 这样主人在真机日志里能直接看到裁剪是否生效（`残影保留=[HumanoidArmorLayer, CapeLayer]`）。

**没有做的事**：没改 `layers` 列表本身、没改 `accessor.cyberware$layers()`、
没改 `EquipmentLayerRendererMixin`、没改 `CapeLayerMixin`、没改 `EntityModel.setupAnim`
调用位置、没改主模型提交、没改任何渲染类型、没改段数/间距常量。

---

## 4. 「只影响残影」的保证说明

**一句话**：新的过滤只写在 `AfterimageRenderer` 的残影循环内部（`AfterimageRenderer.java:203`），
而普通实体渲染**根本不经过这个循环** —— 原版 `LivingEntityRenderer.submit` 遍历的是它自己的
`this.layers` 字段，`accessor.cyberware$layers()` 在本文件里只被**读**过一次（`AfterimageRenderer.java:151`）；
两个 mixin 也本来就被 `AfterimageRenderer.ghostAlpha() > 0` 门控，而 `ghostAlpha` 只在
残影循环里非零（`:175` 置值，`:214-216` 与 `:107` 两处 `finally` 归零）。

展开成可核对的四条：

1. `isGhostLayer` 只在 `:203` 一处被调用（另一处是 `:197` 的诊断日志），调用点在
   `renderAfterimages` 的 `for (int g = 0; g < wanted; g++)` 循环体内部。
2. `renderAfterimages` 只被 `onRenderLiving`（`:100` 调用）调用，而 `onRenderLiving` 只挂在
   `RenderLivingEvent.Pre` 上；它除了残影，不改任何状态（`finally` 里把 `ghostAlpha` 归零）。
3. `accessor.cyberware$layers()` 是 `@Accessor`，本文件只 **get**、没有 set；
   过滤是「遍历时跳过」，不是「从列表里删掉」—— 原版那份 `layers` 字段始终是完整的 10 个。
4. `EquipmentLayerRendererMixin`（`:60`、`:90`、`:118`、`:141`）与 `CapeLayerMixin`（`:55`）
   的所有分支都以 `AfterimageRenderer.ghostAlpha() > 0.0F` 为前提，普通本体渲染时 `ghostAlpha == 0`
   → 走原版分支，护甲渲染类型与染色**逐字节等于**没有本模组时的行为。

因此：**本体的护甲、披风、手持物品、箭、蜂刺、耳朵、鹦鹉、鞘翅、头顶方块、旋转特效一个都不会少**
—— 它们各自的图层在普通渲染路径上照旧全部执行。

---

## 5. 「改动前 / 改动后」的静态对比（从代码路径数出来的数）

### 5.1 每**一个**残影（`layerCollector != null` 时）

| 项目 | 改动前 | 改动后 | 依据 |
|---|---|---|---|
| 主模型 `submitModel` | 1 | 1 | `:189`（未改） |
| **图层回调**（`layer.submit`） | **10** | **2** | `:202` 循环 10 项 → `:203` 过滤掉 8 项 |
| 合计 submit 级调用 | 11 | 3 | 1 + 10 → 1 + 2 |

### 5.2 每**帧**（`wanted = 8`，即 `MAX_GHOSTS = 8` 在稳态下打满）

| 项目 | 改动前 | 改动后 | 差值 |
|---|---|---|---|
| 主模型 `submitModel` | 8 | 8 | 0 |
| **图层回调** | **80** | **16** | **−64（−80.0%）** |
| 合计 submit 级调用 | **88** | **24** | **−64（−72.7%）** |

### 5.3 被裁掉的 8 个图层「会做多少事」（按 26.1 源码里的门槛）

| 图层 | 门槛（源码） | 门槛通过时每个残影的实际提交 |
|---|---|---|
| `PlayerItemInHandLayer` | `!item.isEmpty()`（两只手各一次，`ItemInHandLayer.submitArmWithItem`） | 最多 **2** 次物品模型提交 |
| `ArrowLayer` | `numStuck(state) > 0` → `state.arrowCount` | 循环 `arrowCount` 次物品提交 |
| `BeeStingerLayer` | `state.stingerCount > 0` | 循环 `stingerCount` 次物品提交 |
| `CustomHeadLayer` | `!headItem.isEmpty() \|\| wornHeadType != null` | 1 次（头颅/方块） |
| `Deadmau5EarsLayer` | `state.showExtraEars && !state.isInvisible` | 1 次 `submitModel` |
| `WingsLayer` | 胸槽 `Equippable` 且 `assetId` 非空（鞘翅） | 1 次 `renderLayers` |
| `ParrotOnShoulderLayer` | 左右肩的 parrot variant 非空（各自判） | 最多 2 次 |
| `SpinAttackEffectLayer` | `state.isAutoSpinAttack` | 1 次 `submitModel` |

**必须说清的一点（避免把数看高）**：这 8 个图层在门槛不通过时只做一次字段判断就 return，
**几乎不花钱**。所以「80 → 16」是**确定的回调削减**，但**不等于**同比例的耗时削减。
其中**有把握的重活**是 `PlayerItemInHandLayer`：玩家手上几乎总拿着东西，
它每帧被砍掉的是最多 **16 次物品模型提交**（8 残影 × 2 手）。

**也必须说清另一点（避免把数看低）**：本次**没有减**主模型（8 次/帧）与护甲图层
（每个残影最多 4 次 `renderLayers`，8 残影最多 32 次/帧）。这两项才是剩下的开销大头 ——
而它们正是主人明确要求保留的部分（护甲是视觉大头，段数是特意调过的密度）。
所以这些静态数字只能证明「少做了多少事」，**证明不了帧率会回升多少**。

---

## 6. ⚠ 未真机验证 —— 明确声明

**本次没有在真机上启动过游戏。** `compileJava` 通过只证明能编译。

以下全部由主人在真机上判断，本文件给不出结论：

1. **帧率是否回升、回升多少** —— 静态数字只能说明图层回调从 80/帧降到 16/帧，
   说明不了这 64 次回调在主人的机器上值多少帧。
2. **视觉损失是否可接受** —— 具体是：手持物品不再出现在残影里（原本它以**不透明**方式出现）、
   鞘翅玩家的残影没有翅膀、头顶方块/箭/蜂刺/鹦鹉/deadmau5 耳朵/激流特效不再出现在残影里。
3. **护甲与披风在残影里是否仍旧正常**（半透明、不 z-fighting、纹饰颜色正确）——
   这两条通路没改动，但也没在真机确认过。
4. 诊断日志是否按预期打印出 `残影图层: ... | 残影保留=[HumanoidArmorLayer, CapeLayer]`。

验证方法：启动游戏 → 第三人称（F5）→ 开斯安威斯坦 → 看日志那一行 + 看帧率 + 看残影观感。
日志关键字：`[cyberware] 残影图层:`（只打一次）。

---

## 7. 未做项（明确列出）

按主人的本轮裁决，以下**都没做**：

1. **没有距离剔除** —— 残影不随距离淡出/消失。
2. **没有 LOD** —— 远近残影用同一模型精度。
3. **没有帧率自适应** —— 不做「掉帧就少画几段」。
4. **没有新增配置项**（`CyberwareConfig` 未改），也没有加开关。
5. **没有动段数与间距**：`MAX_GHOSTS = 8`（`:62`）、`GHOST_STEP = 1`（`:67`）、
   `AfterimageHistory.INTERVAL = 2`（`AfterimageHistory.java:42`）、`CAPACITY = 48`（`:28`）
   —— 全部**零改动**（`git diff` 里不出现这些常量）。
6. **没有动半透明渲染类型**、没有改主模型的提交方式。
7. **没有跑完整 gradle build、没有 commit**（按硬约束）。
8. **没有动** `EquipmentLayerRendererMixin.java` 与 `CapeLayerMixin.java` —— 本次只改
   `AfterimageRenderer.java` 一个文件，所以就「独占范围」而言也没有超出。

---

## 8. 验证

```
cd /root/mod26/cyberware
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-arm64 \
PATH=$JAVA_HOME/bin:/opt/gradle-8.10.2/bin:$PATH \
gradle compileJava --no-daemon
```

```
> Task :compileJava
BUILD SUCCESSFUL in 1m 41s
PIPE_EXIT=0
```

（只跑 `compileJava`，未跑完整 build，未 commit。）

`git diff --stat` 只有一个文件：

```
 src/main/java/com/dsh/cyberware/client/AfterimageRenderer.java | 39 ++++++++++++++++++++++
 1 file changed, 39 insertions(+)
```

（`git status --porcelain` 对 `client/` 与 `mixin/client/` 只列出这一个文件；
`EquipmentLayerRendererMixin.java`、`CapeLayerMixin.java`、`AfterimageHistory.java`
均**不在**改动列表里。）
