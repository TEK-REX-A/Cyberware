# DEBT-CLEANUP-A.md —— 技术债 A 路：ProjectileMixin 去留核实 + GHOST-PERF.md 行号修正

> 任务 t21（opticsdev，attempt `d967ebca-2929-42c8-a7c7-36299cf55310`）
> ⚠ **状态：第 ① 项（删除 ProjectileMixin）已按 captain 指示暂停 —— 本次没有删除任何文件。**
> 第 ② 项（行号修正）已完成。
> ⚠ **未真机验证**（见 §E）。

---

## 0. 一句话结论（先给最要紧的）

**投掷物时间减缓在 0.3.12 的生效路径里完全没有 `ProjectileMixin`。**
它由 `ClientProjectileDilation`（客户端，`CyberwareClient.java:44` 注册）与
`TimeDilationHandler.onEntityTickPre`（服务端，`Cyberware.java:52` 注册）两条腿实现，
两条腿都经 `DilationTickGate.shouldSkip(...)` 做**整刻跳过**。
`ProjectileMixin` 走的是被废弃的「tick 后按倍率缩回位移」方案，且**从未登记进任何 mixin 配置**。

---

## A. 核实报告（captain 要求的三节）

### a) 到底有没有登记？项目里是否还有别的 mixins.json？

**a-1. 仓库里那份配置的内容（全文）**

`src/main/resources/cyberware.mixins.json`：

```json
{
  "required": true,
  "minVersion": "0.8.5",
  "package": "com.dsh.cyberware.mixin",
  "compatibilityLevel": "JAVA_21",
  "client": [
    "client.PostChainAccessor",
    "client.PostPassAccessor",
    "client.GuiMixin",
    "client.SoundEngineMixin",
    "client.ParticleMixin",
    "client.WeatherEffectRendererMixin",
    "client.GameRendererMixin",
    "client.LivingEntityRendererAccessor",
    "client.EquipmentLayerRendererMixin",
    "client.CapeLayerMixin",
    "client.EntityRendererOutlineMixin"
  ],
  "injectors": {
    "defaultRequire": 1
  }
}
```

```
$ grep -n "ProjectileMixin" src/main/resources/cyberware.mixins.json
（无输出，exit=1）
```

11 项 client 里没有 `client.ProjectileMixin`。

**a-2. 全仓只有这一份 mixin 配置，没有第二个**

```
$ find . -name "*mixins*.json" -not -path "./.git/*"
./src/main/resources/cyberware.mixins.json
./build/resources/main/cyberware.mixins.json        ← 上者的构建产物副本
```

声明入口也只有一处 —— `src/main/resources/META-INF/neoforge.mods.toml`：

```toml
[[mixins]]
config = "cyberware.mixins.json"
```

（该 toml 里 `[[mixins]]` 只出现 1 次。）

**a-3. 打包后的 jar 里同样只有一份，且它没列 ProjectileMixin**

```
$ jar tf build/libs/cyberware-0.3.12-Beta.jar | grep -iE "mixins.*json"
cyberware.mixins.json
$ jar xf <jar> cyberware.mixins.json && grep -c ProjectileMixin cyberware.mixins.json
0
```

同一个 jar 里 **两个类都在**，但只有后者是活的：

```
com/dsh/cyberware/mixin/client/ProjectileMixin.class     ← 编译进去了，但没登记
com/dsh/cyberware/client/ClientProjectileDilation.class  ← 真正在跑的那个
```

**a-4. git 全历史：它从来没有在任何 json 里注册过**

```
$ git log --all -S "ProjectileMixin" -- '*.json'
（无输出）
```

`cyberware.mixins.json` 一共 4 个历史版本，逐版本核对：

| commit | 提交 | 该版本 json 里 ProjectileMixin 命中数 |
|---|---|---|
| 660c81e | cyberware 0.3.6-Beta | **0** |
| 54cd558 | 0.3.9-Beta | **0** |
| b0aabee | 0.3.12-Beta（定义表 + Attachment） | **0** |
| 30e9646 | 0.3.12-Beta（当前 HEAD） | **0** |

> 注意措辞：`VERIFY-REPORT.md` §11 写的是「**自 0.3.6 起**是死代码」。
> 更准确的表述是 **「从 0.3.6 它被加进来的那一刻起就没登记过，一次都没有」**。

**a-5. 顺带：设备上正在玩的那份 jar 也是同样的结论**

```
$ ls -la /sdcard/FCL/.minecraft/versions/26.1.2-NeoForge/mods/cyberware-0.3.12-Beta.jar
-rw-rw----. 10000 1023 664674  2026-09-29 16:20
$ jar tf <那个 jar> | grep -i projectile
com/dsh/cyberware/client/ClientProjectileDilation.class
com/dsh/cyberware/mixin/client/ProjectileMixin.class     ← 在，但…
$ jar xf <那个 jar> cyberware.mixins.json && grep -c ProjectileMixin cyberware.mixins.json
0                                                        ← …配置里没有它
```

**即：主人正在玩的那个 jar，ProjectileMixin.class 躺在里面，但配置里没有它 → Mixin 不会处理它。**

### b) 投掷物减速的实际生效路径（调用链）

**两条腿，都做整刻跳过（不是位移回拉）：**

**服务端（权威侧）**

| 步骤 | 位置 | 代码 |
|---|---|---|
| 注册 | `Cyberware.java:52` | `NeoForge.EVENT_BUS.addListener(TimeDilationHandler::onEntityTickPre);` |
| 入口 | `TimeDilationHandler.java:83` | `public static void onEntityTickPre(EntityTickEvent.Pre event)` |
| 只处理服务端 | `:85-87` | `if (entity.level().isClientSide()) return;` |
| 只看投射物 | `:88-90` | `if (!(entity instanceof Projectile projectile)) return;` |
| 自己射的豁免 | `:91-94` | `if (projectile.getOwner() instanceof Player) return;` |
| **查时间倍率** | **`:95`** | **`double timeScale = TimeDilationManager.timeScaleFor(entity);`** |
| 判定 | `:96` | `if (DilationTickGate.shouldSkip(entity, timeScale)) {` |
| 执行 | `:97` | `event.setCanceled(true);` ← 整个 tick 不执行 |

> captain 问的「服务端 `TimeDilationManager.timeScaleFor` 在哪一步被查」——**就是 `TimeDilationHandler.java:95`**
> （同一个方法在 `:120` 还有一处用于另一个 handler）。

**客户端**

| 步骤 | 位置 | 代码 |
|---|---|---|
| 注册 | `CyberwareClient.java:44` | `NeoForge.EVENT_BUS.addListener(ClientProjectileDilation::onEntityTickPre);` |
| 入口 | `ClientProjectileDilation.java:27` | `public static void onEntityTickPre(EntityTickEvent.Pre event)` |
| 只看投射物 | `:29` | `if (!(entity instanceof Projectile projectile)) return;` |
| 自己射的豁免 | `:32-34` | `if (projectile.getOwner() instanceof Player) return;` |
| 查客户端倍率 | `:36-37` | `double timeScale = ClientTimeDilation.timeScaleAt(entity.getX(), entity.getY(), entity.getZ());` |
| 判定 | `:38` | `if (DilationTickGate.shouldSkip(entity, timeScale)) {` |
| 执行 | `:39` | `event.setCanceled(true);` |

**两端为什么跳的是同一批刻** —— `core/DilationTickGate.java:29-41`：

```java
public static boolean shouldSkip(Entity entity, double timeScale) {
    ...
    int period = (int) Math.round(1.0D / clamped);          // timeScale 0.2 → period 5
    long gameTime = entity.level().getGameTime();
    return Math.floorMod(gameTime + entity.getId(), period) != 0;   // :40
}
```

相位 = `gameTime + entity.getId()`，两端同一公式、同一 `gameTime`、同一 id → 永远一致。

**结论（b）**：投掷物减速 = `ClientProjectileDilation`（客户端）+ `TimeDilationHandler.onEntityTickPre`（服务端），
两者都经 `DilationTickGate`。**`ProjectileMixin` 不在这条链上**，因为它没有被应用；
它实现的是「tick 跑完后把这一 tick 的位移乘 timeScale 缩回去」的旧方案，
而 `TimeDilationHandler.java:73-82` 的类注释本身就把那个方案判为废弃：

> 「为什么投射物不能用位置回拉（主人实测的两个现象就是证据）：… 回拉只改了**位置**，
> 改不掉 tick 内部已经走完的那 3 格 … **跳 tick 才是物理自洽的**」

### c) 它是否通过别的方式被应用？

逐条排查，结论是**没有**：

| 可能的旁路 | 核查 | 结果 |
|---|---|---|
| 第二个 mixin 配置 | `find . -name "*mixins*.json"`（a-2）；`neoforge.mods.toml` 只有一个 `[[mixins]]` | **不存在** |
| 被别的类引用（`@Mixin` 目标、注册、import） | `grep -rn "ProjectileMixin" --include='*.java' src/` → 只有它自己的类声明行 `ProjectileMixin.java:25` | **零引用** |
| 名字写错（例如 `CyberwareProjectile`） | `grep -rn "CyberwareProjectile" .` → 无输出 | **不存在该名字** |
| 通过 json 注册但被嵌套/条件包裹 | 该 json 是纯字符串数组，无嵌套 | **不可能** |

**因此：先前「未登记 → 不生效」的判断成立，`ProjectileMixin` 没有被任何机制应用。**

**那主人为什么记得「跟 ProjectileMixin 有关系」？** —— 有历史解释，而且这个印象不算错：

```
$ git log --oneline --follow -- .../mixin/client/ProjectileMixin.java
30e9646 0.3.12-Beta：…义眼敌我识别描边        ← 只有 require=0 的小改（t11）
660c81e cyberware 0.3.6-Beta：位置回拉 + 洋葱皮残影   ← 文件诞生

$ git log --oneline --follow -- .../client/ClientProjectileDilation.java
32a5351 0.3.8-Beta：残影独立 state（定格姿态）+ 箭矢改回跳 tick（一顿一顿）  ← 换成跳 tick
660c81e cyberware 0.3.6-Beta：位置回拉 + 洋葱皮残影                          ← 与 mixin 同一个提交诞生
```

- `ProjectileMixin.java` 与 `ClientProjectileDilation.java` 是**同一个提交 660c81e（0.3.6）**加进来的；
- 0.3.8 的 `32a5351`（提交信息直接写着「箭矢改回跳 tick（一顿一顿）」）把这条路改成跳 tick；
- 而那条 mixin **从落地起就没写进配置**。

**所以：主人记的「投掷物减速跟 ProjectileMixin 有关系」在「功能史」上是对的
（它是同一个功能的**第一版实现**），在「0.3.12 的实际运行路径」上不成立。**

### 真机日志旁证（额外）

日志：`/sdcard/FCL/.minecraft/versions/26.1.2-NeoForge/logs/latest.log`（46,663 B，2026-09-30 00:14）

**该会话确实加载了本模组：**

```
:22   Found mod file "cyberware-0.3.12-Beta.jar" [locator: {mods folder locator …}]
:78    - cyberware (jar(mods/cyberware-0.3.12-Beta.jar))
:118  [cyberware] 义体系统已初始化（MC 26.1.2 / NeoForge 26.1.2.109）
```

**且 mixin 确实在运行**（这些是 Mixin 注入后生成的转发方法名）：

```
:497  [net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer:redirect$zbf000$cyberware$ghostArmorColor:596]
:498  […:redirect$zbf000$cyberware$ghostArmorRenderType:570]
:499  […:modify$zbf000$cyberware$ghostTrimColor:646]
```

**而 `ProjectileMixin` 的影子一条都没有：**

```
$ grep -c "ProjectileMixin" latest.log          → 0
$ grep -E "Mixin apply|InvalidInjectionException|Invalid name:" latest.log
（只有字体加载失败与 sodium 的无关行，没有任何注入失败）
```

> **诚实边界**：日志里没有 `ProjectileMixin` 字样，这件事**本身不足以单独证明「未应用」**
> —— Mixin 不会为每个被应用的 mixin 类打 INFO 日志。
> 真正的证明是 a) 的配置 + 历史与 c) 的零引用；日志只是**与结论一致**的旁证。

**顺带一个对 t19 有用的发现**：设备上部署的那份 jar（`mods/cyberware-0.3.12-Beta.jar`，
mtime 2026-09-29 16:20）里的 `AfterimageRenderer.class` 含有 t19 新增的「残影保留」字符串，
日志 `:437` 打出了：

```
[cyberware] 残影图层: collector=true count=10 -> [HumanoidArmorLayer, PlayerItemInHandLayer,
ArrowLayer, Deadmau5EarsLayer, CapeLayer, CustomHeadLayer, WingsLayer, ParrotOnShoulderLayer,
SpinAttackEffectLayer, BeeStingerLayer] | 残影保留=[HumanoidArmorLayer, CapeLayer]
```

→ **t19 的裁剪在真机上确实生效、日志项已获真机确认**（帧率与观感仍**未**确认）。

---

## B. 第 ① 项的状态：**未执行删除**

- 收到 captain 指示时，我**还没有**删除任何文件 —— 当时只做了两件无副作用的事：
  确认 `ProjectileMixin.java` 仍在、验证 trash 目录可写（写入测试文件已删除）。
- **`src/main/java/com/dsh/cyberware/mixin/client/ProjectileMixin.java` 原样保留**
  （mtime `2026-09-29 06:13`，即 t11 给它补 `require = 0` 那次），
  `git status --porcelain src/main/java/com/dsh/cyberware/mixin/client/` **无输出**。
- trash 目录 `/sdcard/DSH/trash/cyberware-repo/` 已验证存在且可写
  （`mkdir -p` + 写入测试成功，测试文件已清理）。若主人之后决定删除，
  正确做法是 `cp <file> /sdcard/DSH/trash/cyberware-repo/` 之后再 `rm`。
- 本次**未改** `cyberware.mixins.json` —— 它本来就没登记这个类，不需要改。

---

## C. 第 ② 项：GHOST-PERF.md 行号修正清单

### C-1 改了什么

| # | 位置 | 改前 | 改后 | 依据 |
|---|---|---|---|---|
| 1 | `GHOST-PERF.md` §4「一句话」段（原 L152） | `accessor.cyberware$layers()` 在本文件里只被**读**过一次（`:149`） | 同句，改成 `（AfterimageRenderer.java:151）` | `sed -n '151p' AfterimageRenderer.java` = `List<RenderLayer<…>> layers = accessor.cyberware$layers();` |
| 2 | `GHOST-PERF.md` §1.2（L35） | 真机日志（`AfterimageRenderer:191`，本次改动前的那行） | 真机日志（改动前那行在 `AfterimageRenderer.java:191`，改动后在 `:193`；两份日志引用的都是同一句「残影图层」打印） | t19 新增 2 行 import → 该打印行从 191 下移到 193；设备日志（部署的是 t19 之后的构建）里显示的正是 `:193` |
| 3 | `GHOST-PERF.md` §4 第 4 条（非行号，**额外披露**） | 「普通**火体**渲染时」 | 「普通**本体**渲染时」 | 错别字 |

第 3 条严格说不是行号，但它是同一段的明显错字，一并改了并在此列明 —— **没有改任何结论**。

### C-2 关于 inspector 说的「§4 有两处 :149」

我逐字符核对了修正前的全文：当时 `:149` 一共出现 **2 次**，性质不同：

- **L54（§1.4）**：`RenderType renderType = RenderTypes.entityTranslucentCullItemTarget(texture);`
  —— 这一处 **是对的**，该行确实在 `AfterimageRenderer.java:149`；
- **L152（§4）**：指向 accessor 读行的 `:149` —— 这一处 **是错的**，实际在 `:151`（已改）。

另外 §1.1 里**曾经**也写过 `:149` 指 accessor，那一处在 t19 收尾的行号自审里已经改成 `:151`
（inspector 看到的「两处」应是 L54 + L152，或是包含那处已被我改掉的 §1.1）。
**现在全文指向 accessor 读行的引用只有 `:151`，保留的 `:149` 只有 L54 且它正确。**

### C-3 全文行号引用复核清单（逐个核过）

| 被引用的行 | 引用位置（GHOST-PERF.md） | 核对结果 |
|---|---|---|
| `AfterimageRenderer.java:12-13` | §3 | ✅ 两行 import（CapeLayer / HumanoidArmorLayer） |
| `AfterimageRenderer.java:62` | §1.1 / §7 | ✅ `MAX_GHOSTS = 8` |
| `AfterimageRenderer.java:67` | §1.1 / §7 | ✅ `GHOST_STEP = 1` |
| `AfterimageRenderer.java:98-109` | §1.1 | ✅ `onRenderLiving` 方法体范围 |
| `AfterimageRenderer.java:100` | §4 第 2 条 | ✅ `renderAfterimages(event);` |
| `AfterimageRenderer.java:107` | §1.1 / §4 | ✅ 外层 `finally` 的 `ghostAlpha = 0.0F` |
| `AfterimageRenderer.java:135` | §1.1 | ✅ `int wanted = Math.min(...)` |
| `AfterimageRenderer.java:149` | §1.4 | ✅ 半透明 RenderType 赋值（**这处是对的**） |
| `AfterimageRenderer.java:151` | §1.1 / §4 | ✅ accessor 读 `layers`（**本次修正点**） |
| `AfterimageRenderer.java:154` | §1.1 | ✅ 主循环 `for (int g = 0; ...)` |
| `AfterimageRenderer.java:175` | §4 | ✅ `ghostAlpha = alpha;` |
| `AfterimageRenderer.java:189` | §1.1 / §3 / §5 | ✅ 主模型 `submitModel` |
| `AfterimageRenderer.java:191 / 193` | §1.2 | ✅ 改动前 / 后的「残影图层」打印行（**本次澄清**） |
| `AfterimageRenderer.java:192-198` | §3 | ✅ 诊断日志语句块 |
| `AfterimageRenderer.java:197` | §4 第 1 条 | ✅ `.filter(AfterimageRenderer::isGhostLayer)` |
| `AfterimageRenderer.java:202` / `202-206` / `202-208` | §1.1 / §3 / §5 | ✅ 图层 for 循环 |
| `AfterimageRenderer.java:203` | §3 / §4 | ✅ `if (!isGhostLayer(layer)) {` |
| `AfterimageRenderer.java:208` | §1.1 | ✅ `layer.submit(...)` |
| `AfterimageRenderer.java:214-216` | §4 | ✅ 内层 `finally { ghostAlpha = 0.0F; }` |
| `AfterimageRenderer.java:245` | §2 / §3 | ✅ `isGhostLayer` 方法 |
| `AfterimageHistory.java:28` | §7 | ✅ `CAPACITY = 48` |
| `AfterimageHistory.java:42` | §7 | ✅ `INTERVAL = 2` |
| `EquipmentLayerRendererMixin.java:53 / :81 / :111 / :132` | §2 | ✅ 4 个注入点（`@Redirect`×3 + `@ModifyArg`×1） |
| `EquipmentLayerRendererMixin.java:60 / :90 / :118 / :141` | §4 第 4 条 | ✅ 4 处 `ghostAlpha` 判定 |
| `EquipmentLayerRendererMixin.java:68` | §1.4 | ✅ `RenderTypes.armorTranslucent(texture)` |
| `CapeLayerMixin.java:38` | §2 | ✅ `@Redirect(` |
| `CapeLayerMixin.java:55` | §4 第 4 条 | ✅ `float alpha = AfterimageRenderer.ghostAlpha();` |

**结论：除上表标明的 1 处真错（已改）与 1 处需澄清（已澄清）外，其余行号引用全部准确。**

---

## D. 验证

```
cd /root/mod26/cyberware
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-arm64 \
PATH=$JAVA_HOME/bin:/opt/gradle-8.10.2/bin:$PATH \
gradle compileJava --no-daemon
```

```
> Task :createMinecraftArtifacts UP-TO-DATE
> Task :compileJava UP-TO-DATE
BUILD SUCCESSFUL in 1m 25s
2 actionable tasks: 2 up-to-date
PIPE_EXIT=0
```

（`compileJava` UP-TO-DATE：本次只改了 `.md`，Java 源码零改动，符合预期。
只跑 `compileJava`，未跑完整 build；未 commit。）

### 改动文件

| 文件 | 动作 |
|---|---|
| `GHOST-PERF.md` | 改 3 处（2 处行号 + 1 处错字），见 §C-1 |
| `DEBT-CLEANUP-A.md` | 新增（本文件） |

`src/main/java/com/dsh/cyberware/mixin/client/ProjectileMixin.java` —— **未删除、未修改**。
本次没有改 `core/**`、`resources/**`、`client/AfterimageRenderer.java`。

---

## E. ⚠ 未真机验证 + 「零影响」的证据

### E-1 未真机验证（明确声明）

**我没有启动过游戏。** 上文 §A 引用的 `latest.log` 是**主人自己跑出来的既有日志**，
我只是读取它作为旁证 —— 这**不算**我做过真机验证。

具体地，以下事情我**没有**验证、也无法用静态证据证明：

- 投掷物减速在真机上的**观感**是否正常（是否真的是「一顿一顿」而不是抖动/回拉）；
- 若将来删除 `ProjectileMixin`，主人的实际游戏体验是否有任何变化（理论上是零，见 E-2）；
- t19 的图层裁剪在真机上的**帧率收益**与**视觉损失**（日志只证明了裁剪生效，没证明够不够快）。

### E-2 「对本版运行行为零影响」的证据

**首先**：本次**没有删除任何文件**，所以这句话在这一轮是字面成立的。

**其次**，即使删了，运行时行为同样为零 —— 两条 grep 证据：

1. **配置层面**：`grep -c ProjectileMixin src/main/resources/cyberware.mixins.json` → **0**；
   而全仓只有这一份 mixin 配置（`find . -name "*mixins*.json"` 只有它 + 构建副本，
   `neoforge.mods.toml:24-25` 只声明它这一份）→ **Mixin 框架永远不会处理这个类**。
2. **代码层面**：`grep -rn "ProjectileMixin" --include='*.java' src/` →
   只有 `ProjectileMixin.java:25` 自己的类声明那一行 →
   **没有任何代码路径能触达它**（没有引用、没有实例化、没有注册）。

> 说明：仓库里的 `.md` 文档（`VERIFY-REPORT.md`、`M2-FIX.md`、`FINAL-HANDOFF.md`、
> `ROUND2-HANDOFF.md`、本文件）确实还提到 `ProjectileMixin` 这个名字，
> 但那是**历史报告对事实的记录**，不是代码引用；这些文件也不在本任务的范围内，我没有改。
