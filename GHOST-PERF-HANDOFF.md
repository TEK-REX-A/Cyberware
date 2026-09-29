# GHOST-PERF-HANDOFF.md —— 残影图层裁剪收尾：独立复核 + 交付（t20 / inspector）

工作区：`/root/mod26/cyberware`　本任务 attempt_id：`96098a8b-f5e9-4537-b1a5-c41e886db5e5`
范围：独立复核 t19（opticsdev 只裁图层）→ 全量构建 → 静态自查 → commit → 部署 FCL → 真机验证清单。
本任务只新增本报告、执行授权范围内的 git 提交与 jar 部署；**未修改任何他人负责的源码逻辑**。

---

## 0. 结论

| 项 | 结果 |
|---|---|
| **铁律：本体渲染绝不能受影响** | ✅ **成立**。我用 6 条独立证据交叉验证（§1.1），其中最强的一条是：原版 `LivingEntityRenderer.submit` 自己遍历 `this.layers`，与我们的残影循环是**两个互不相干的循环**，且过滤是「遍历时跳过」—— 那份列表从头到尾没被写过 |
| 段数与间距未动 | ✅ `MAX_GHOSTS=8`、`GHOST_STEP=1`、`AfterimageHistory.INTERVAL=2` 全部未变；`AfterimageHistory.java` **零改动**（`git diff` 为空） |
| 白名单只留 2 个 | ✅ 编译产物里 `isGhostLayer` 就是 `instanceof HumanoidArmorLayer` + `instanceof CapeLayer` 两项 |
| 10 个图层的逐条理由 | ✅ 我按 26.1.2.109 的**源码**逐条复核，包括 opticsdev 推翻 captain 初步判断的两条（`PlayerItemInHandLayer` / `CustomHeadLayer`）—— **推翻站得住** |
| 静态量化 | ✅ 我按代码路径自己点了一遍，`11→3`（单残影）与 `88→24`（每帧）**认同**；但已补注该口径的含义与局限（§1.4） |
| 未引入距离剔除 / LOD / 帧率自适应 / 配置项 | ✅ 文件里 grep 不到任何相关标识符；diff 是**纯新增 39 行、0 删除** |
| 全量构建 | ✅ 普通 build exit 0；`--rerun-tasks --no-build-cache` **真编译 4 executed / 0 error** |
| 静态自查 | ✅ mixin 本轮零改动（12 类 / `require=0`×13 / 无冒号）；定义表 135/19/154；贴图 135×32×32；zh_cn 166；en_us 49 md5 未变 |
| commit | ✅ `a0d4d95bee4f5bb1c9dc549efaf63ef1a9d30838`，2 files, **+318**（未 push） |
| 部署 | ✅ mods 里 cyberware 只剩新 jar（664674 B）；round3 jar 进 trash |
| **总裁决** | **PASS** —— 本体渲染未受影响；仅 1 条 low 文档行号漂移（§1.5 F1） |

> ### ⚠ **静态数字只证明「少做了多少事」，证明不了「帧率回升多少」。**
> `80→16`、`88→24` 是**代码路径上的 submit 级调用计数**，不是 draw call 数、更不是帧时间。
> 被裁的 8 个图层在门槛不通过时只做一次字段判断（几乎不花钱），所以实际收益可能远小于 80%，
> 也可能因为砍掉了最重的 `PlayerItemInHandLayer`（每帧最多 16 次物品模型提交）而比 72.7% 更有感觉。
> **帧率回升多少、视觉损失能否接受 —— 只有主人在真机上判断。**

---

## 1. 独立复核

### 1.1 ⭐ 铁律核验：新的过滤有没有可能碰到本体渲染

任务把这条列为**风险最高**的一项（错了 = 所有生物的手持物/装饰消失）。我没有只复述 opticsdev 的四条，
而是从 **6 个互相独立的角度**各自验了一遍：

| # | 核验方式 | 命令 / 依据 | 结论 |
|---|---|---|---|
| 1 | 全仓搜 `isGhostLayer` | `grep -rn isGhostLayer src/` → 只有本类 3 处：定义 `:245`、诊断日志 `:197`、过滤 `:203`；且它是 `private static`（外部类根本调不到） | 过滤**逃不出**这个类 |
| 2 | 全仓搜 `cyberware$layers` | 只有 `AfterimageRenderer.java:151` 一处读取 + accessor 声明 | 只读一次 |
| 3 | accessor 是否有 setter | `javap -p` 该接口 → 只有 `cyberware$setupRotations` / `cyberware$scale` / **`cyberware$layers()`（返回 `java.util.List`，纯 getter）**，没有 setter | 没有写入口 |
| 4 | 字节码里有没有动这个 List | `javap -p -c AfterimageRenderer \| grep -E "List\.(remove\|add\|set\|clear\|sort)"` → **无任何 List 写操作**；`cyberware$layers:()Ljava/util/List;` 只出现 1 次（offset 165） | 「遍历时跳过」而非「删除」 |
| 5 | `ghostAlpha` 的生命周期 | 全仓 8 处引用全在本类：声明 `:82`、读取器 `:87-88`、**置值 `:175`（在残影循环体内、`isLocalPlayer` 早退之后）**、归零 `:107` 与 `:215`（两处 `finally`） | `ghostAlpha > 0` ⟺ 正处在残影循环中 |
| 6 | **原版源码**：本体走的是哪条循环 | `minecraft-patched-26.1.2.109-sources.jar` → `LivingEntityRenderer.submit` **L103-108**：`if (shouldRenderLayers && !this.layers.isEmpty()) { this.model.setupAnim(state); for (RenderLayer layer : this.layers) layer.submit(...); }` | 本体遍历的是**自己的 `this.layers`**，与我们的循环完全无关 |

**并且**：两个 mixin 的每个分支都以 `ghostAlpha > 0` 为前提，else 分支调用的是与「没装本模组」时**完全相同**的原版方法：

- `EquipmentLayerRendererMixin`：`:60`（护甲渲染类型）、`:90`（护甲染色）、`:118`（纹饰渲染类型）、`:141`（纹饰顶点色）—— 四处都是
  `if (ghostAlpha() > 0) { … } ` ，否则 `return RenderTypes.armorCutoutNoCull(texture)` /
  `return extensions.getArmorLayerTintColor(...)` / `return Sheets.armorTrimsSheet(decal)` / `return color`，即原版返回值。
- `CapeLayerMixin:55`：`if (alpha > 0.0F && capeTexture != null) {…半透明…return;}`，否则落到
  `collector.submitModel(model, state, poseStack, renderType, lightCoords, overlayCoords, outlineColor, crumblingOverlay)`
  —— 与原版同一重载、同样的参数（逐参数核对过）。

**结论：铁律成立**，本体渲染的护甲、披风、手持物、箭、蜂刺、耳朵、鹦鹉、鞘翅、头顶方块、旋转特效
**一个都不会少**。

**额外发现（顺带排除一个我本来担心的点）**：残影路径里唯一写入**共享对象**的地方是 `AfterimageRenderer:201`
的 `model.setupAnim(ghost)`（`model` = `renderer.getModel()`，本体也用同一个模型实例）。
但原版 `LivingEntityRenderer.submit` 在图层循环之前（**L104**）会再调一次 `this.model.setupAnim(state)` 把姿态刷回本体，
所以这个写入不会泄漏到本体 —— 而且它是 **t19 之前就有的旧行为**，本次 diff 未触碰该行（纯新增 39 行、0 删除）。

### 1.2 段数与间距：未动 ✅

```
AfterimageRenderer.java:62   private static final int MAX_GHOSTS = 8;
AfterimageRenderer.java:67   private static final int GHOST_STEP = 1;
AfterimageHistory.java:42    public static final int INTERVAL = 2;
$ git diff HEAD --stat -- src/main/java/com/dsh/cyberware/client/AfterimageHistory.java   →  空
$ git diff HEAD --stat                                                                    →  1 file changed, 39 insertions(+)
```

`AfterimageRenderer` 的改动是**纯新增**（39 行，0 删除）→ 残影本身的算法一行未改。

### 1.3 白名单与逐条理由（我按 26.1.2.109 源码复核）✅

`isGhostLayer` 编译产物（`javap -c`）：

```
405:       1: instanceof  #313  // class HumanoidArmorLayer
408:       8: instanceof  #315  // class CapeLayer
```

→ 恰好两项，用 `instanceof`（子类也覆盖）。

**保留的两个**（我核对过它们确实走「已被我们改造的半透明通路」）：

| 图层 | 源码证据 | 走我们的 mixin？ |
|---|---|---|
| `HumanoidArmorLayer` | `submit` → 4 次 `renderArmorPiece` → `equipmentRenderer.renderLayers(...)` | ✅ `EquipmentLayerRendererMixin` 拦的就是 `renderLayers` |
| `CapeLayer` | `submit` → `collector.submitModel(...)` | ✅ `CapeLayerMixin` 拦的就是这个调用点（我读过 round-1 的字节码） |

**跳过的 8 个**（逐条复核，全部成立）：

| 图层 | opticsdev 的理由 | 我的独立核验（26.1.2.109 源码） | 成立？ |
|---|---|---|---|
| `PlayerItemInHandLayer` | 走 `item.submit(..., state.outlineColor)`，没走 ghostAlpha 通路 → 残影里是不透明实心物品 | `ItemInHandLayer.submitArmWithItem` 末行**逐字**是 `item.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);` —— 用物品自己的 `ItemStackRenderState` 渲染类型，**既不经过 `EquipmentLayerRenderer.renderLayers`，也不经过 `CapeLayer`** → 我们的两个 mixin 够不着它 | ✅ |
| `CustomHeadLayer` | `SkullBlockRenderer.submitSkull` / `headItem.submit`，都不是半透明通路 | 源码确认：`resolveSkullRenderType(...)` 给的是头颅/皮肤渲染类型；`headItem.submit(...)` 同上。两者都不在我们的改造范围内 | ✅ |
| `ArrowLayer` | `StuckInBodyLayer` 循环提交物品模型，不透明 | 源码 `return state.arrowCount;` → 循环提交；渲染类型来自物品 | ✅ |
| `BeeStingerLayer` | 同上 | 源码 `return state.stingerCount;` | ✅ |
| `Deadmau5EarsLayer` | `entitySolid` 不透明 + 只有 deadmau5 | 源码 `if (state.showExtraEars && !state.isInvisible)` → `submitNodeCollector.submitModel(...)`（固定渲染类型） | ✅ |
| `ParrotOnShoulderLayer` | 鹦鹉走自己的渲染类型 + 少数玩家 | 源码左右肩各自 `!= null` 判定，最多 2 次 `submitModel` | ✅ |
| `SpinAttackEffectLayer` | 固定 TEXTURE + 仅激流旋转期间 | 源码 `if (state.isAutoSpinAttack) { submitNodeCollector.submitModel(this.model, state, poseStack, TEXTURE, …) }` | ✅ |
| `WingsLayer` | **边界 case**：它确实走 `renderLayers`、半透明**是生效的**，只按「少数玩家才有」跳过 | 源码确认 `this.equipmentRenderer.renderLayers(EquipmentClientInfo.LayerType.WINGS, …, playerElytraTexture, state.outlineColor, 0)` —— 与护甲同一条被改造过的通路 | ✅ **理由诚实** |

**关于 opticsdev 推翻 captain 初步判断的两条** —— **我判定推翻站得住**：
captain 原以为 `PlayerItemInHandLayer` / `CustomHeadLayer` 属「待定」，但按上面的源码，
这两者**根本不在我们改造过的两条半透明通路上**：它们的渲染类型由物品/头颅自己决定，
ghostAlpha 传不进去 → 在 45%~5% 不透明度的残影里会以**全不透明实心块**出现（穿帮），
同时白付渲染成本。所以「跳过」不是妥协，而是**正确取舍**。

同时我也认可 opticsdev 对 `WingsLayer` 的处理方式：它没有假装鞘翅「不透明」（那是错的），
而是明说「半透明生效、只因覆盖人数少而跳过」，并给出了恢复方法（在 `:245` 的 `return` 上加
`|| layer instanceof WingsLayer`，其它地方不用动）。**这条边界写清楚了，比含糊带过更好。**

### 1.4 静态量化：我自己点了一遍，认同（并补两条局限）

**按代码路径数**（`layerCollector != null` 时）：

| 层级 | 改动前 | 改动后 | 我复核的依据 |
|---|---|---|---|
| 每个残影：主模型 `submitModel` | 1 | 1 | `:189`，未改 |
| 每个残影：图层回调 `layer.submit` | 10 | 2 | `:202` 遍历 10 项 → `:203` 跳过 8 项 |
| 每个残影：submit 级合计 | 11 | 3 | 1+10 → 1+2 |
| **每帧（wanted=8）：图层回调** | **80** | **16** | 8×10 → 8×2，**−64（−80.0%）** |
| **每帧：submit 级合计** | **88** | **24** | 8+80 → 8+16，**−64（−72.7%）** |

算术核对：64/80 = 80.0% ✅；64/88 = 72.73% ✅ —— 与 opticsdev 给的数一致。

**10 个图层的清单本身也有真机证据**（不是我或他凭记忆写的）：

```
latest.log:437  [21:45:09] [Render thread/INFO]: […AfterimageRenderer:renderAfterimages:191]: [cyberware] 残影图层:
                collector=true count=10 -> [HumanoidArmorLayer, PlayerItemInHandLayer, ArrowLayer, Deadmau5EarsLayer,
                CapeLayer, CustomHeadLayer, WingsLayer, ParrotOnShoulderLayer, SpinAttackEffectLayer, BeeStingerLayer]
```

该 log 来自 `Sep 29 14:04` 那次真机启动（`latest.log:22` 显示加载的是 `cyberware-0.3.12-Beta.jar`）。
我另外把 10 个类名全部对照过 MC jar 的 `client/renderer/entity/layers/` 目录 —— **10/10 都存在**。

**我要补的两条局限（防止把数看高或看低）**：

1. **口径是「submit 级调用」不是「draw call」**。被裁的 8 个图层在门槛不通过时只做一次字段判断就 return，
   几乎不花钱；而保留下来的 `HumanoidArmorLayer` 每次 `submit` 内部还要跑最多 4 次 `renderLayers`，
   每个 `renderLayers` 内部又有主体/附魔/纹饰多次 `submitModel`。所以 **−64 次回调 ≠ 同比例耗时削减**。
2. **只有在 `layerCollector != null` 时这条优化才生效**（`:200` 的判断）；若为 null，图层循环两次都不跑
   —— 优化收益为 0，回归风险也为 0。真机日志里 `collector=true`，说明实际客户端走的正是会生效的那条路。
3. 剩下的开销大头（主模型 8 次/帧 + 护甲最多 32 次 `renderLayers`/帧）**本次没动**，而这两项正是主人要求保留的。

### 1.5 本轮未做 / 我的观察

- **未做（符合裁决）**：距离剔除、LOD、帧率自适应、配置项 —— 文件内 grep `distance|lod|fps|config|framerate|budget|adaptive` **无命中**；
  `build.gradle` 未改（无新依赖）。

| # | 级别 | 内容 | 我的判断 |
|---|---|---|---|
| F1 | low | `GHOST-PERF.md` §4 两处写 `accessor.cyberware$layers()` 在 `:149`，实际是 **`:151`**（t19 新增 2 个 import 后行号整体 +2；文档写这些行号时混用了改前/改后编号） | 只是**文档行号漂移**，结论本身没错。建议顺手改成 `:151`，不影响功能 |
| F2 | info | 白名单是「硬编码 2 个类」而非可配置 | 与「本轮不做配置项」的裁决一致；换白名单只需改 `:245` 一行（文档已写明），可接受 |

---

## 2. 全量构建

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-arm64
export PATH=$JAVA_HOME/bin:/opt/gradle-8.10.2/bin:$PATH
gradle build --no-daemon                                  # 任务书指定口径
gradle build --no-daemon --rerun-tasks --no-build-cache    # 补一次真编译，排除缓存假绿
```

| # | 命令 | 耗时 | 退出码 | compileJava |
|---|---|---|---|---|
| 1 | `gradle build --no-daemon` | 1m 19s | **0** | 真执行（t19 后首次） |
| 2 | `gradle build --no-daemon --rerun-tasks --no-build-cache` | 2m 15s | **0** | **真实执行（4 executed）** |

口径 2 尾部：`BUILD SUCCESSFUL in 2m 15s / 4 actionable tasks: 4 executed / EXIT=0`。

**最终产物（下文验证与部署都用它）**：

```
build/libs/cyberware-0.3.12-Beta.jar
  664674 字节   md5 b9a64f1fad17d610913a16f4d85de83f
```

日志：`/tmp/t20-build.log`、`/tmp/t20-forcebuild.log`。

**产物级复核（javap）**：`isGhostLayer` 存在且只含 2 个 `instanceof`；`cyberware$layers()` 只有 getter 形态；
`AfterimageRenderer` 内**无任何 List 写操作**（§1.1 表 4）。

---

## 3. 静态自查（沿用前几轮口径）

| 项 | 结果 |
|---|---|
| Mixin 冒号陷阱 | ✅ **本轮 mixin 目录零改动**（`git status --porcelain -- …/mixin/` 空）；对最终 jar 复跑：12 个 mixin 类、`require=0` **13** 处、非 0 值 0、8 个 `method=[...]` 去重后**无冒号** |
| 定义表 | ✅ 135 生效 / 19 注释 / 154 合计 |
| 贴图 | ✅ 物品贴图 135 张**全 32×32**；11 张特殊图全 16×16 |
| lang | ✅ zh_cn 166 条（md5 `b73796f8…`）；**en_us 49 条、md5 `db559925…` 未变** |
| 构建脚本 | ✅ `build.gradle` / `settings.gradle` 未改动 |

---

## 4. commit（本地，未 push）

显式清单（`/tmp/t20-add.txt` → `git add --pathspec-from-file`，**未用 `git add -A`**）：

```
src/main/java/com/dsh/cyberware/client/AfterimageRenderer.java
GHOST-PERF.md
```

混入检查：`.jar` / `build/` / `.gradle/` / `run/` / `LICENSE` **零命中**。

```
commit a0d4d95bee4f5bb1c9dc549efaf63ef1a9d30838
Message: 残影性能优化：裁剪渲染图层至护甲+披风（每帧图层回调 80→16，段数与间距不变）

 GHOST-PERF.md                                      | 279 +++++++++++++++++++++
 .../dsh/cyberware/client/AfterimageRenderer.java   |  39 +++
 2 files changed, 318 insertions(+)
```

提交后 `git status --porcelain` = **0 行**。**未执行 `git push`。**

---

## 5. 部署到 FCL

**部署前**：

```
<…/26.1.2-NeoForge/mods>
  -rw-rw---- 1 10000 1023 664396 Sep 29 14:57 cyberware-0.3.12-Beta.jar   md5 db16bd60396854aba9e77f377dbf2bbb  ← round3（P0 修复那版）
```

**执行**：

```bash
mv <mods>/cyberware-0.3.12-Beta.jar  /sdcard/DSH/trash/cyberware/cyberware-0.3.12-Beta-round3.jar
cp /root/mod26/cyberware/build/libs/cyberware-0.3.12-Beta.jar  <mods>/
sync
```

**部署后核对**：

```
=== mods 里 cyberware 条目（只剩一个）===
cyberware-0.3.12-Beta.jar   664674 字节   Sep 29 16:20

=== md5 两侧一致 ===
b9a64f1fad17d610913a16f4d85de83f  <mods>/cyberware-0.3.12-Beta.jar
b9a64f1fad17d610913a16f4d85de83f  /root/mod26/cyberware/build/libs/cyberware-0.3.12-Beta.jar

=== trash 里可回滚的历代 jar ===
cyberware-0.3.12-Beta-round1.jar   655477 B  （义体轮盘之前）
cyberware-0.3.12-Beta-round2.jar   663507 B  （R 键轮盘）
cyberware-0.3.12-Beta-round3.jar   664396 B  （P0 换世界修复）
cyberware-0.3.11-Beta-Hotfix-10.jar 296090 B （上一稳定版）
```

mods 目录其余 14 个第三方 jar 未动（目录共 15 项）。

---

## 6. 真机验证清单（主人操作）

### 6.1 主目标：帧率回升多少
1. 找一处**能稳定复现卡顿**的场景（大量生物 / 复杂地形 / 大型整合包加载中）。
2. 第三人称（F5）按住斯安威斯坦（或 `/cyberware dilate`）触发残影，记录 F3 的 fps。
3. 与**上一版**（trash 里的 `cyberware-0.3.12-Beta-round3.jar`）**同场景、同视角、同参数**再测一次。
4. 建议各测 2~3 次取观察值 —— 单次读数抖动可能比本次收益还大。
> 本报告**给不出**这个数字：静态计数只说明「图层回调 80→16」，不代表帧时间同比例下降。

### 6.2 视觉验收（判断损失能否接受）
- **仍应正常**：残影里的**护甲**（半透明轮廓的视觉大头）与**披风**。
- **应已退出残影**：手持物、鞘翅、身上的箭、蜂刺、deadmau5 耳朵、肩头鹦鹉、头顶方块、三叉戟激流特效。
  它们原本在残影里就是**不透明实心块**（穿帮），退出后观感应当**变干净**。
- 若你更看重「鞘翅也要有残影」：`WingsLayer` 的半透明是**生效的**，在 `AfterimageRenderer:245` 的 `return`
  上加 `|| layer instanceof WingsLayer` 即可（其余不用动）—— 这是唯一的边界 case。

### 6.3 ⚠ 回归（本轮最该盯的一项）
**本体（非残影）的护甲、披风、手持物、箭、蜂刺、耳朵、鹦鹉、鞘翅、头顶方块、旋转特效一个都不能少。**
建议：不开斯安威斯坦的普通第三人称下，逐一确认这些装饰都还在、且**是不透明的正常样子**（不是半透明）。
> 静态上我已按 §1.1 的 6 条证据判定本体不受影响，但**这是本轮风险最高的一条，必须实机确认**。

### 6.4 诊断日志
新 jar 首次渲染残影时会打印（`system.out` → `latest.log`）：

```
[cyberware] 残影图层: collector=true count=10 -> [ …10 个… ] | 残影保留=[HumanoidArmorLayer, CapeLayer]
```

- `count=10` 且 `残影保留=[HumanoidArmorLayer, CapeLayer]` → 白名单按预期生效。
- 若 `残影保留` 里出现别的名字 → 说明加载的不是本版 jar，或白名单被改过，请回报。

### 6.5 保留项（防止其他功能回归）
R 键义体轮盘（按住→指向→松开施放）/ V 键手持激活 / 操作台装卸 / 容量上限 100 /
义眼描边（只能验生效的 **6 件**）/ **P0 换世界修复**（退到标题→新建世界，HUD 从 0 起、怪物原速）。

### 6.6 日志关键字（注入/加载是否正常）
`/sdcard/FCL/.minecraft/versions/26.1.2-NeoForge/logs/latest.log` 搜：
`Mixin apply ... failed` / `InvalidInjectionException` / `Invalid name:` / `Scanned 0 target(s)`。

### 6.7 回滚
把 `/sdcard/DSH/trash/cyberware/` 里的 `cyberware-0.3.12-Beta-round3.jar`（P0 版，无本优化）拷回 mods 即成对照组；
**先把当前 jar 移走，不要两个 cyberware jar 并存。**

---

## 7. 一句话总结

t19 的实现**经得起复核**：过滤只存在于残影循环内部、列表只读不写、`ghostAlpha` 严格门控两个 mixin、
原版本体渲染走的是另一条循环 —— **本体不受影响**；白名单的 10 条理由我按 26.1.2.109 源码逐条验证，
包括推翻 captain 初步判断的那两条（`PlayerItemInHandLayer` / `CustomHeadLayer` 确实不走半透明通路）。
构建 exit 0、已提交（`a0d4d95`）并部署。

**但静态数字只证明「少做了多少事」，证明不了「帧率回升多少」** ——
帧率收益与视觉损失请主人在真机上按 §6.1/§6.2/§6.3 判断。
