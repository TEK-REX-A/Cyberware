# ROUND2-HANDOFF.md —— 第二轮收尾：R 键轮盘 + 激活链路（t16 / inspector）

工作区：`/root/mod26/cyberware`　本任务 attempt_id：`63ca6671-7466-4471-af66-abe61aa5f604`
范围：验证 t13（协议 + 服务端 activateInstalled）/ t14（R 键中文名）/ t15（客户端轮盘 UI）→ 全量构建 → commit → 部署 FCL → 真机清单。
本任务只新增本报告、执行授权范围内的 git 提交与 jar 部署；**未修改任何他人负责的源码逻辑**。

---

## 0. 结论

| 项 | 结果 |
|---|---|
| 全量构建 | ✅ `gradle build --no-daemon` → **exit 0**，`BUILD SUCCESSFUL in 55s`；产物 **663507 字节** |
| Mixin 冒号陷阱 | ✅ 本轮 mixin 目录**零改动**（`git status` 空 + `git diff --stat` 空）；jar 内复跑仍 **13/13 `require=0`、8 个描述符无冒号** |
| 定义表 | ✅ 135 生效 / 19 注释 / 154 合计 / `cyberware:item/` 154，与上轮一致 |
| 贴图 | ✅ 135 张物品贴图**全部 32×32**；11 张特殊贴图（`cyberware_chip` + 10 `slot_*`）全部 16×16 |
| lang | ✅ zh_cn **166 条**（`key.cyberware.radial = 义体轮盘`）；en_us 仍 **49 条**、md5 未变 |
| 本轮新增项专项 | ✅ 6/6 通过（详见 §3），含**客户端/服务端判定口径交叉核对** |
| 第三方库 | ✅ build.gradle **完全没有 dependencies 块**，且未改动 |
| commit | ✅ `75126311400009cd4a6a21e6d375f1ffc20adebf`，9 files, **+992 / −10**（未 push） |
| 部署 | ✅ mods 里 cyberware **只剩新 `cyberware-0.3.12-Beta.jar`（663507 B）**；旧 jar 进 trash |
| ⚠ 待裁决 | **LICENSE 有一处无任务申报的改动**（GPL-2 文本 → GPL-3 文本），已**排除在本轮 commit 之外**，见 §7 F1 |

> ### ⚠ **编译通过不等于注入成功，真机日志才算数。**
> R 键轮盘、激活链路、描边都只是静态层面验证通过；是否真的弹轮盘、是否真的给效果，
> 必须由主人真机跑过、并在 `latest.log` 里看不到注入报错才算数。

---

## 1. 全量构建

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-arm64
export PATH=$JAVA_HOME/bin:/opt/gradle-8.10.2/bin:$PATH
gradle build --no-daemon
```

```
> Task :createMinecraftArtifacts UP-TO-DATE
> Task :compileJava UP-TO-DATE          ← t13/t14/t15 已真编译过；输入未变
> Task :processResources
> Task :classes / :jarJar NO-SOURCE / :jar / :assemble
> Task :compileTestJava NO-SOURCE / :test NO-SOURCE / :check UP-TO-DATE / :build
BUILD SUCCESSFUL in 55s
4 actionable tasks: 2 executed, 2 up-to-date
EXIT=0
```

产物：`build/libs/cyberware-0.3.12-Beta.jar`，**663507 字节**（上一轮 655477 → 本轮 +8030，新增轮盘 UI 与协议改动所致），
md5 `60401290cb49cc3a668cba3baf44f368`。jar 内 550 条目 / 71 个 class，含新增的
`client/CyberwareRadialScreen.class` 与 `client/CyberwareRadialScreen$Entry.class`。
日志：`/tmp/t16-build.log`。

---

## 2. 静态自查（沿用上一轮口径）

### 2.1 Mixin：本轮零改动，沿用上轮结论 + jar 内复跑

```
$ git status --porcelain -- src/main/java/com/dsh/cyberware/mixin/     →  （空）
$ git diff --stat HEAD -- src/main/java/com/dsh/cyberware/mixin/       →  （空）
```

因为没改，冒号结论不变。为了不空口复用，又对**本轮新 jar** 复跑了一次产物级检查：

```
mixin 类数 12   require=0 合计 13   非0值 无
method=[...] 去重 8   —— 全部 [ok]（方法名与 '(' 之间无冒号）
   [ok] calculatePitch / extractPlayerHealth / extractRenderState / render / tick
   [ok] extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V
   [ok] renderLayers(...Identifier;II)V
   [ok] submit(...PoseStack;L…SubmitNodeCollector;IL…AvatarRenderState;FF)V
```

### 2.2 定义表 / 贴图 / lang

| 指标 | 期望 | 实测 |
|---|---|---|
| 生效 register | 135 | **135** |
| 注释定义 | 19 | **19** |
| `new CyberwareDefinition(` 合计 | 154 | **154** |
| `cyberware:item/` | 154 | **154** |
| 物品贴图 32×32 | 135/135 | **135/135（非 32×32 列表为空）** |
| 特殊贴图 16×16 | chip + 10 slot_* | **11/11 全部 16×16** |
| zh_cn 条目 | 166（165 + radial） | **166**，item 135，`key.cyberware.radial = 义体轮盘`，md5 `b73796f8bcf464da9a6ecfc4ead0679a` |
| en_us 条目 | 49 且 md5 不变 | **49**，md5 `db559925b90bf73e22eda3b17053136b`（= 上轮值） |

---

## 3. 本轮新增项专项核验（逐项）

### 3.1 `ActivatePayload` 已带 `defId`，且空串语义唯一

`src/main/java/com/dsh/cyberware/network/ActivatePayload.java`：

```java
26: public record ActivatePayload(String defId) implements CustomPacketPayload {
29:     public static final String HELD = "";                     // 空串 = 手持激活（V 键）
34:     public static final StreamCodec<RegistryFriendlyByteBuf, ActivatePayload> STREAM_CODEC =
35:             StreamCodec.composite(
36:                     ByteBufCodecs.STRING_UTF8, ActivatePayload::defId,
37:                     ActivatePayload::new);
44:     public ActivatePayload {                                   // 紧凑构造器
45:         defId = defId == null ? HELD : defId;                  // null → 空串（退化成手持，不会变成「激活任意」）
46:     }
59:     public static ActivatePayload of(String defId) { … }       // 轮盘用
```

产物级（`javap -p -c -cp build/libs/cyberware-0.3.12-Beta.jar …ActivatePayload`）：

```
public final class …ActivatePayload extends java.lang.Record implements …CustomPacketPayload {
  private final java.lang.String defId;
  public static final java.lang.String HELD;
  public static final …StreamCodec<…> STREAM_CODEC;
  public com.dsh.cyberware.network.ActivatePayload(java.lang.String);
STRING_UTF8 出现: True
```

全仓 `grep -rn "new ActivatePayload()"` → **0 处**（旧无参调用已清干净）。

### 3.2 `CyberwareNetwork.handleActivate` 两条分支齐备

`src/main/java/com/dsh/cyberware/network/CyberwareNetwork.java`：

```java
58:  if (!(context.player() instanceof ServerPlayer serverPlayer)) { … return; }   // 客户端实体副本永不作执行依据
62:  if (payload.defId().isEmpty()) {
64:      CyberwareAbilities.activateHeld(serverPlayer);      // ← 空串 = 手持激活（V 键）
65:      return;
66:  }
68:  CyberwareAbilities.activateInstalled(serverPlayer, payload.defId());  // ← 非空 = 已安装激活（R 键轮盘）
```

产物级交叉印证（`javap -c`）：

```
48: invokestatic  // Method com/dsh/cyberware/core/CyberwareAbilities.activateHeld:(…Player;)Z
58: invokestatic  // Method com/dsh/cyberware/core/CyberwareAbilities.activateInstalled:(…Player;Ljava/lang/String;)Z
```

协议版本：`PROTOCOL_VERSION = "3"`（L22），产物常量池 `ConstantValue: String 3`。

### 3.3 `CyberwareAbilities.activateInstalled` 以**服务端**已安装表校验

```java
 99: public static boolean activateInstalled(Player player, String defId) {
100:     if (player == null || defId == null || defId.isBlank()) return false;
103:     CyberwareData installed = CyberwareInstallation.dataOf(player, defId);   // ← 服务端附件（权威）
104:     if (installed == null) {                                                // 没装 → 直接失败
106:         LOGGER.debug("[cyberware] 拒绝激活 {}：{} 身上并没有安装该义体", …);
108:         return false;                                                        // 不抛异常、不给效果
109:     }
110:     CyberwareDefinition def = CyberwareDefinitions.byId(defId);
111:     if (def == null) { LOGGER.warn(…); return false; }                       // 旧存档被删型号
116:     return activate(player, def, installed);                                 // 复用同一条效果链
117: }
```

**零复制分支逻辑**：`activateHeld`（L79）与 `activateInstalled`（L116）都调用同一个
`activate(Player, CyberwareDefinition, CyberwareData)`（L24）。也就是说「客户端发来的 defId 只是意图」，
数值/稀有度全部取服务端 Attachment 里那一份。

### 3.4 `CyberwareKeys.RADIAL` 已注册（默认 R）

```java
35: public static final KeyMapping RADIAL = new KeyMapping(
36:         "key.cyberware.radial",
37:         InputConstants.Type.KEYSYM,
38:         InputConstants.KEY_R,        // ← 默认 R
39:         CATEGORY);
```

注册处 `CyberwareClient.onRegisterKeys`：`event.register(CyberwareKeys.RADIAL);`（diff 新增行），
lang 键 `key.cyberware.radial = 义体轮盘` 已就位（§2.2）——t14 提示的「字符串必须逐字一致」已满足。

### 3.5 `CyberwareRadialScreen` 存在且已接线

- 文件存在：`src/main/java/com/dsh/cyberware/client/CyberwareRadialScreen.java`（388 行），已进 jar（§1）。
- 触发（`CyberwareClient.onClientTick` diff）：

```java
while (CyberwareKeys.RADIAL.consumeClick()) {
    CyberwareRadialScreen.openOrHint(Minecraft.getInstance());
}
```

- 施放入口（`CyberwareRadialScreen.commit()`）：只有选中了才发包，未选中一个包都不发：

```java
360: String defId = this.selected >= 0 ? this.entries.get(this.selected).id() : null;
368: if (defId != null) ClientPacketDistributor.sendToServer(ActivatePayload.of(defId));
```

### 3.6 ⭐ 客户端与服务端「有主动效果」判定口径交叉核对

| 判定点 | 服务端 `core/CyberwareAbilities.java` | 客户端 `client/CyberwareRadialScreen.java` | 一致 |
|---|---|---|---|
| 稀有度取档 | **L28** `Variant variant = def.variantFor(data.rarity());` | **L153** 同 | ✅ |
| 取不到就回退 | **L31** `variant = def.baseVariant();` | **L155** 同 | ✅ |
| 回退后仍 null | **L33-35** `if (variant == null) return false;` | **L157-159** 同 | ✅ |
| 时间减缓分支 | **L37** `double ratio = variant.stat(Stats.TIME_SLOW, 0.0D);`<br>**L38** `if (ratio > 0.0D) {` | **L160** `if (variant.stat(Stats.TIME_SLOW, 0.0D) > 0.0D) {` | ✅ |
| 狂暴分支 | **L50** `if (def.id().startsWith("berserk_")) {` | **L163** `return def.id().startsWith("berserk_");` | ✅ |
| 数据来源 | **L103** `CyberwareInstallation.dataOf(player, defId)` | **L120-121** `CyberwareInstallation.of(player).orderedIds()` + **L127** `installed.dataOf(id)` | ✅ 同源 |
| 其它类型 | **L64-67** 只发「暂未实现主动效果」+ `return false` | **L132-134** 直接不进轮盘 | ✅ 等效（都不会给出效果） |

即：**服务端不给效果的，客户端也不会列出来；客户端列出来的，服务端一定按同一条判定给效果** —— 不存在
「轮盘里能选、点了没反应」的口径错位。

### 3.7 未引入第三方库

```
$ grep -n "dependencies\|implementation\|compileOnly\|runtimeOnly\|maven" build.gradle
无 dependencies/依赖块
$ git diff HEAD -- build.gradle settings.gradle
（空 = 未改动）
```

jar 内顶层条目只有 `com`(自研 87) / `assets`(458) / `META-INF`(3) / `NOTICE.txt` / `cyberware.mixins.json`，
**没有打包任何第三方依赖**。

---

## 4. commit（本地，未 push）

```
$ git status --porcelain        →  3 ?? / 7 M（= 9 项本轮改动 + 1 项 LICENSE，见 §7 F1）
```

**显式清单**（写在 `/tmp/t16-add.txt`，`git add --pathspec-from-file`，**未用 `git add -A`**）：

```
ACTIVATE-WIRING.md                                        RADIAL-MENU.md
src/main/java/com/dsh/cyberware/client/CyberwareClient.java
src/main/java/com/dsh/cyberware/client/CyberwareKeys.java
src/main/java/com/dsh/cyberware/client/CyberwareRadialScreen.java
src/main/java/com/dsh/cyberware/core/CyberwareAbilities.java
src/main/java/com/dsh/cyberware/network/ActivatePayload.java
src/main/java/com/dsh/cyberware/network/CyberwareNetwork.java
src/main/resources/assets/cyberware/lang/zh_cn.json
```

清单混入检查：`.jar` / `build/` / `.gradle/` / `run/` / `LICENSE` **全部无命中**。

```
commit 75126311400009cd4a6a21e6d375f1ffc20adebf
Message: 0.3.12-Beta：R 键义体轮盘 + 激活链路改走已安装表

 ACTIVATE-WIRING.md                                 | 149 ++++++++
 RADIAL-MENU.md                                     | 326 +++++++++++++++++
 .../com/dsh/cyberware/client/CyberwareClient.java  |  10 +-
 .../com/dsh/cyberware/client/CyberwareKeys.java    |  14 +
 .../cyberware/client/CyberwareRadialScreen.java    | 388 +++++++++++++++++++++
 .../com/dsh/cyberware/core/CyberwareAbilities.java |  35 ++
 .../com/dsh/cyberware/network/ActivatePayload.java |  45 ++-
 .../dsh/cyberware/network/CyberwareNetwork.java    |  34 +-
 .../resources/assets/cyberware/lang/zh_cn.json     |   1 +
 9 files changed, 992 insertions(+), 10 deletions(-)
```

提交后 `git status --porcelain` 只剩 ` M LICENSE`（故意未提交，见 §7 F1）。**未执行 `git push`。**

---

## 5. 部署到 FCL

**部署前**：

```
<a:…/26.1.2-NeoForge/mods>
  -rw-rw---- 1 10000 1023  655477 Sep 29 06:37 cyberware-0.3.12-Beta.jar   ← 第一轮的 jar
```

**执行**：

```bash
mv <mods>/cyberware-0.3.12-Beta.jar  /sdcard/DSH/trash/cyberware/cyberware-0.3.12-Beta-round1.jar   # 同名，加 -round1 后缀区分
cp /root/mod26/cyberware/build/libs/cyberware-0.3.12-Beta.jar  <mods>/
sync
```

实测输出：

```
renamed '…/mods/cyberware-0.3.12-Beta.jar' -> '/sdcard/DSH/trash/cyberware/cyberware-0.3.12-Beta-round1.jar'
'…/build/libs/cyberware-0.3.12-Beta.jar' -> '…/mods/cyberware-0.3.12-Beta.jar'
```

**部署后核对**：

```
=== mods 里 cyberware 条目 ===
cyberware-0.3.12-Beta.jar   （663507 字节，Sep 29 13:03）   ← 只剩这一个

=== md5 两侧一致 ===
60401290cb49cc3a668cba3baf44f368  <mods>/cyberware-0.3.12-Beta.jar
60401290cb49cc3a668cba3baf44f368  /root/mod26/cyberware/build/libs/cyberware-0.3.12-Beta.jar

=== trash 里的旧 jar ===
cyberware-0.3.12-Beta-round1.jar   （655477 字节，第一轮那版）
cyberware-0.3.11-Beta-Hotfix-10.jar（296090 字节）
```

mods 目录其余 14 个第三方 jar 未动（目录共 15 项）。

---

## 6. 真机验证清单（第二轮更新版）

### 6.1 新增：R 键义体轮盘
1. 先确认身上装过**至少一件主动义体**：斯安威斯坦（`sandevistan_c1/c2/c3/c4/apogee`）或狂暴（`berserk_c1..c4`）。
   没装的话按 R 只会看到一句提示，轮盘**不出现**（这是设计，不是 bug）。
2. **按住 R** → 屏幕中央弹出轮盘；鼠标指向某扇区 → 该扇区高亮。
3. **松开 R** → 关闭并施放选中的那件（时间减缓会看到画面减速 + 动作栏提示；狂暴会看到红色提示）。
4. 松手时若鼠标在中心死区内（半径 < 24 px）→ **不施放**，只关闭。
5. **ESC** → 只关闭、不施放。
6. 窗口失焦导致的「按住 R 收不到松手事件」→ 回到窗口应自动关闭（`tick()` 里的物理键位兜底）。

### 6.2 保留（上一轮清单）
- **V 键**手持激活仍可用（主手拿着义体按 V，行为与 0.3.11 一致）。
- **操作台装/卸**：快捷栏拿义体 → 开操作台 → 放底部义体槽 → 点「安装」/「卸载」；标题栏显示 `容量 已用/上限`。
- **容量上限 100**：`config/cyberware-common.toml` 的 `[capacity] defaultCapacity = 100`、`enableCapacityLimit = true`。
- **义眼描边**（只能验当前生效的 6 件）：`基础歧路司义眼 / 歧路司义眼1型 / 歧路司义眼神舆 / 歧路司义眼祸兆 /
  歧路司义眼千里目 / 歧路司义眼石化鸡蛇` → 同队/宠物绿、中立黄、敌对红，**卸下义眼后描边消失**。
  ⚠ `kiroshi_optics_piercing` / `kiroshi_optics_sensor` 无素材、未注册，**游戏内不存在，找不到属正常**。
- ⚠ 12 件新义体（代谢编辑器等）槽位/稀有度/容量是 TODO 初值，分类可能不准，**不代表 bug**。

### 6.3 日志（判定注入是否成功）

```
文件：/sdcard/FCL/.minecraft/versions/26.1.2-NeoForge/logs/latest.log
```

搜这些关键字，命中任意一个都要把原文发回来：

| 关键字 | 含义 |
|---|---|
| `Mixin apply` + `failed` | 某 mixin 应用失败 |
| `InvalidInjectionException` | 注入点匹配/描述符出错 |
| `Invalid name:` / `InvalidMemberDescriptorException` | 描述符写法错（冒号陷阱） |
| `Scanned 0 target(s)` / `Cannot find method` | 目标没匹配到（`require=0` 下静默跳过 → 功能隐形失效） |

确认加载的是新版本：

```bash
grep -n "cyberware-" …/latest.log                 # 应出现 cyberware-0.3.12-Beta.jar
grep -n "Cyberware 义体系统" …/latest.log         # 版本号应为 0.3.12-Beta
```

### 6.4 回滚
把 `/sdcard/DSH/trash/cyberware/cyberware-0.3.12-Beta-round1.jar`（或 `cyberware-0.3.11-Beta-Hotfix-10.jar`）
拷回 mods 目录即可；**先把当前 jar 移走，不要两个 cyberware jar 并存**。

---

## 7. 发现 / 遗留

| # | 级别 | 内容 | 处理 |
|---|---|---|---|
| **F1** | ⚠ 需裁决 | **工作树里有一处非本轮、无任何任务/报告申报的改动**：`LICENSE` 由 **GPL-2 文本（281 行）** 换成 **GPL-3 文本（622 行）**，mtime `Sep 29 12:26`，落在 t13/t15 改动的时间窗内，但三份任务回报都没提。方向上看像是修正 —— `META-INF/neoforge.mods.toml:3` 写 `license = "GPL-3.0-only"`、`NOTICE.md` 写「代码采用 GPL-3.0」、`README.md:47` 写「GPL-3.0」，而 HEAD 里的 LICENSE 文件却是 GPL-2 正文，属于「声明与文件不一致」。 | **我未把它纳入本轮 commit**（不属本轮成果、无授权申报），现在仍是 ` M LICENSE`。请 captain 裁决：确认为修正 → 我或他人单独提交一条 `LICENSE：GPL-2 文本 → GPL-3（与 mods.toml/NOTICE/README 的 GPL-3.0 声明对齐）`；若为本意之外的误改 → 从 trash/HEAD 恢复。 |
| F2 | low | `mixin/client/ProjectileMixin.java` 仍未登记进 `cyberware.mixins.json`（自 0.3.6 起的死代码；功能已由 `ClientProjectileDilation` 取代）。本轮未动。 | 等主人裁决去留（t11/t12 均建议删除）。 |
| F3 | low | `en_us.json` 未补新定义英文名（主人裁决只补中文），英文环境会显示未翻译键。 | 等主人裁决。 |
| F4 | low | 12 件新义体槽位/稀有度/容量仍是 TODO 占位；19 条无素材定义仍注释留档。 | 等主人确认。 |
| F5 | info | 轮盘条目顺序 = `CyberwareInstallation.orderedIds()`（字典序），与操作台卸载按钮同一套确定性下标，两端一致。 | 无需动作。 |

---

## 8. 一句话总结

第二轮改动（协议带 `defId` + 服务端查已安装表 + R 键轮盘 + 中文键名）**静态验证全部通过、构建 exit 0、已提交并部署**；
唯一需要 captain 拍板的是 §7 F1 那个没人认领的 `LICENSE` GPL-2 → GPL-3 改动。
**再次强调：编译通过不等于注入成功，真机日志才算数** —— 请主人进游戏按住 R 试轮盘，并回读 `latest.log`。
