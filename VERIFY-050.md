# VERIFY-050.md —— 0.5.0-Beta 第三步独立验收（t32 / inspector）

工作区：`/root/mod26/cyberware`　本任务 attempt_id：`4864b08a-d395-4b44-a7c1-740cde3fbe6f`
性质：**只读验收**。本任务**未修改任何源码或资源**，只新增本报告。
被验对象：邮件IX 第三步（`t28` 服务端 SCAN/校验 + `t29` 客户端 X 键/锁定框/上传条/破解轮盘 + `t30`/`t31` 数值落地）。
按 captain 时序：**0.5.0 尚未构建时先做源码级**；captain 构建并部署（`0.5.0-Beta / md5 0e8c22bb…`）后已补完产物级 §3。

> ## ⚠ **我没有启动过游戏。**
> 本报告全部结论来自：源码/行号、`git diff`、静态 grep、`javap` 字节码、读 MC/NeoForge 源码。
> 锁定口径跟不跟手、上传条挡不挡热键栏、R 键上下文顺不顺手 —— **只有主人在真机上判断**（§4 给清单）。

---

## 0. 状态

| 部分 | 状态 |
|---|---|
| §1 源码级（不依赖产物） | ✅ 7 项逐条通过（§1.1–§1.7） |
| §2 非阻断发现 | 原 4 条：**F1 已由 t34 修复、F2 已由 t33/t34 修正注释**（两者都在**部署 jar / 当前源码**中独立核实）；F3 是 lang 策略待主人裁决；F4 随 F1 一并解决 |
| §3 产物级核验 | ✅ **已完成** —— 我对 `build/libs/cyberware-0.5.0-Beta.jar`（741555 B，md5 `0e8c22bbfeb4314f2304058e13632290`）逐项核验，且该 md5 与手机 mods 内已部署 jar **完全相同** |
| §4 主人真机复验清单 | 已给 |
| **总裁决** | **PASS**（0 个阻断项） |

---

## 1. 源码级核验（逐项证据）

### 1.1 协议与 `HackPayload.Action.SCAN`（枚举顺序 + 解码兼容）

`network/HackPayload.java`：

```java
36:  public enum Action {
37:      /** 客户端 → 服务端：请求释放破解 */
38:      CAST,
      …（LOCKED / UPLOAD_START / UPLOAD_PROGRESS / APPLIED / CANCELLED）…
50:      REJECTED,
      …
58:      SCAN;                       ← 新增，排在 REJECTED **之后**
60:      public static Action byId(int id) {
61:          Action[] values = values();
62:          return (id < 0 || id >= values.length) ? REJECTED : values[id];
```

`git diff` 对旧枚举**只做了一处改动**（`REJECTED;` → `REJECTED,` + 追加 `SCAN;`）：

```
-        REJECTED;
+        REJECTED,
+        /** … 必须加在 {@link #REJECTED} 之后 … */
+        SCAN;
```

**结论**：旧 7 个 action 的 `ordinal` 一个都没动（0…6 保持），`byId` 越界仍回落 `REJECTED` →
0.4.0 客户端/服务端的旧包解码兼容。工厂 `scan()` = `new HackPayload(Action.SCAN, -1, "scan", 0, 0, 0, "")`
（字段与 STEP3 §1.1 冻结契约一致），`cast()` 未变。`PROTOCOL_VERSION` 源码仍是 `"4"`
（`CyberwareNetwork.java:28`）—— 枚举值域扩展、字段结构未变，符合「不升版本」的理由。

### 1.2 服务端独立校验三件套，且**在扣费之前**

`core/HackSystem.java`（CAST 路径）：

```java
163:  // 服务端独立校验（STEP3 契约 §1.2，不信任客户端）：目标存活 / 距离 ≤ 20 / 有视线。
165:          reject(caster, targetEntityId, hackId, NO_TARGET);          // 目标找不到/类型不对
170:      if (!living.isAlive()
171:              || caster.distanceToSqr(living) > SCAN_RANGE_SQR
172:              || !caster.hasLineOfSight(living)) {
173:          reject(caster, targetEntityId, hackId, NO_TARGET);          // ← 拒绝在扣费之前
174:      }
179:      Payment payment = pay(caster, hack.ramCost());                   // ← 校验通过才谈钱
```

SCAN 路径：

```java
 65:  /** 扫描发光时长：1200 刻 = 60 秒（STEP3 契约 §1.1 冻结值）。 */
 92:      if (!hasKiroshiOptics(caster)) {
 93:          reject(caster, -1, "scan", NO_KIROSHI);                      // NO_KIROSHI 拒绝
100:              entity -> entity != caster && entity.isAlive()
101:                      && entity.distanceToSqr(caster) <= SCAN_RANGE_SQR);   // 20 格
103:      entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_TICKS, 0));
```

**结论**：目标存活 / 距离 ≤ 20 / 有视线 **三项服务端自己重算**，且判在 `pay()` **之前**（第 173 行拒绝、
第 179 行才可能进入扣费）。`hasKiroshiOptics` 决定 `NO_KIROSHI`，与客户端字段完全无关。

### 1.3 客户端零扣费 / 零效果判定（grep 证据）

```
$ grep -rnE "spend|canPay|deduct|pay\(|setData|MobEffects|addEffect|hurtServer|setHealth|attachment" \
      src/main/java/com/dsh/cyberware/client/ | wc -l
0
```

**0 命中**（含宽口径：连 `MobEffects`/`addEffect`/`hurtServer`/`setHealth` 这些「效果类」也一起搜了）。
反向对照：`HackSystem.java:33` 的类注释写着「**客户端字段一律不可信**：目标、破解 id、扣费结果全部服务端自己算」，
`HackSystem.java:24` 的调用链注释也标了 `客户端 CAST（HackPayload）` 只是「请求」。

### 1.4 键位 V/R/G/X：四个，各自唯一，且都已注册

`client/CyberwareKeys.java`：

| 常量 | key 字符串 | 键码 | 注册行 |
|---|---|---|---|
| `ACTIVATE` | `key.cyberware.activate` | `InputConstants.KEY_V` | `CyberwareClient:69` |
| `RADIAL` | `key.cyberware.radial` | `InputConstants.KEY_R` | `CyberwareClient:71` |
| `OVERCLOCK` | `key.cyberware.overclock` | `InputConstants.KEY_G` | `CyberwareClient:73` |
| **`SCAN`** | **`key.cyberware.scan`** | **`InputConstants.KEY_X`** | **`CyberwareClient:75`** |

四个常量分别用四个**不同**的 `InputConstants.KEY_*`，`event.register(...)` 四行齐备 → **无冲突**。
lang：`zh_cn.json:169 "key.cyberware.scan": "歧路司扫描"` ✅；`en_us.json:53 "key.cyberware.scan": "Kiroshi Scan"` ✅。

### 1.5 新增渲染回调的 try/catch（铁律）

| 回调 | 挂载 | 整段 try/catch？ |
|---|---|---|
| `HackHud.onRenderGui`（锁定框 + 上传条 + 提示条） | `CyberwareClient:50 NeoForge.EVENT_BUS.addListener(HackHud::onRenderGui)` | ✅ `:57 try { … } catch (Throwable t)` + 只记一次日志（`FAIL_LOGGED`） |
| `OverclockWireframe.onRenderLiving`（t25） | `CyberwareClient:52` | ✅（t25 已有，`try/catch(Throwable)`） |
| `AfterimageRenderer.onRenderLiving` | `CyberwareClient:54` | ✅ |
| **`HackClientState.tickLock()`** | `CyberwareClient:169`（`onClientTick` 内） | ❌ **无 try/catch，且其调用方 `onClientTick` 也没有**（见 §2 F1） |
| `HackRadialScreen`（Screen 子类） | 由原版屏幕调用 | 无（UI 屏幕，非每帧事件回调；风险面小于 tick/渲染回调） |

本轮**未新增 Mixin**：`git status --porcelain -- src/main/java/com/dsh/cyberware/mixin src/main/resources/cyberware.mixins.json` 为空；
`cyberware.mixins.json` 未改。源码里 `require = 0` 字面量计数 **16**（含 3 处 javadoc 提及；
产物级注解计数以 §3 的 javap 为准，上轮为 13/13）。

### 1.6 数值替换抽查 3 处 + 出处与 HACK-VALUES.md 对齐

| # | 源码位置 | 现值 | 源码行内的出处注释 | HACK-VALUES.md | 一致 |
|---|---|---|---|---|---|
| ① | `core/HackLibrary.java` OVERHEAT 的 `armorDebuff(target, 60, -5.0D)` | **−5.0D** | 「2077 gamestegy Overheat T4『max. -40%』；本仓库 armorDebuff 是 ADD_VALUE 平值，−40% 无法直填 → **主人裁决 A2** 定为 −5 平值」 | §4-A2 建议 −5 平值（§6 第 3 条） | ✅ |
| ② | 同文件 `SHORT_CIRCUIT("short_circuit", "短路", 20, 5)` | **uploadTicks = 20** | javadoc：「上传 1.0s（2077 官方 0.5s；本仓库 1.0s 下限，见 HACK-VALUES §3.2/§3.3）」 | §3.2/§3.3 + §6 第 4 条（30→20） | ✅ |
| ③ | 同文件 `hurtServer(…, 10.0F)` | **10.0F** | 「2077 gamestegy Short Circuit：Damage 260（2077 量纲）→ **主人裁决 A3** 定为 MC 量纲 10.0F」 | §1 表建议 10.0F（§6 第 5 条） | ✅ |

**待裁决项未被偷偷改掉**（逐条核对，全部保持原值且带 `TODO(待主人裁决: …)`）：

| 项 | 现值 | 标记位置 |
|---|---|---|
| A1 突触熔断 RAM / 武器故障 RAM | `SYNAPSE_BURNOUT(…, 40, **3**)`、`WEAPON_GLITCH(…, 24, **6**)` | `HackLibrary.java:80`、`:88` `TODO(待主人裁决: A1/A4 …)` |
| A4 突触熔断 RAM 3→6（建议但不确定） | 仍为 **3** | 同上 |
| A5 amp（效果等级）= 1 | `SLOWNESS/WEAKNESS, 100, 1` | `HackLibrary.java:49` 注释「效果等级 1 待主人裁决（A5）」 |
| B1 超频时长/冷却（方案①保留现表） | 未改 | `CyberwareStats.java:34` `TODO(待主人裁决: B1 …)` |
| 上传取消是否退还 RAM | 现为不退 | `HackSystem.java:218` `TODO(待主人裁决: …)` |
| 裸机基础 RAM 恢复速率 | `BASE_RAM_REGEN = 1.0D`（**t33 已落地**，见 §2 F2） | `CyberwareStats.java:48`、`:116`；`RamSystem.java:124` 的 TODO 尚在（陈旧） |

`CyberwareStats.maxRam`（captain 追加项）已落地：

```java
82:  public static double maxRam(Player player) {
83:      return Math.max(8.0D, sum(player, CyberwareDefinition.Stats.RAM));
84:  }
```

### 1.7 扫描/锁定：客户端只做「显示」，服务端有独立校验

- 客户端锁定：`HackClientState.tickLock()` 每 tick 选目标（`≤ LOCK_RANGE` 先过滤 → `ScreenProjection.project`
  → 屏幕中心 10% 框 → 取最近活体），只把 **entity id** 记在静态字段里供 HUD 画框；
  `HackHud` 每帧读 `HackClientState.lockedEntity()`，`null` 立刻 return。
- 服务端：§1.2 的三件套（`isAlive` / `distanceToSqr ≤ 400` / `hasLineOfSight`）在 `pay()` 之前重算，
  **不看客户端报的坐标/距离**。
- 客户端 RAM 消耗只用于「置灰 + commit 时不发包」（`HackClientState.HACKS` 是不可变展示表）——
  即客户端最多做到「不请求」，**能不能生效永远由服务端裁决**（§1.3 grep 0 命中佐证）。

---

## 2. 发现（含修复后的复核状态）

我第一轮静态核验时提出 4 条；captain 部署前 `t34` / `t33` 已处理其中 2 条。**我都在部署 jar / 当前源码里重新核实过**：

| # | 级别 | 内容 | 现状（复核证据） |
|---|---|---|---|
| **F1** | ~~medium~~ → **已修复** | 第一轮我发现：`HackClientState.tickLock()` 由 `CyberwareClient.onClientTick` 直接调用，**该 tick 回调与其调用方都没有 try/catch**，是全项目唯一不遵守「装饰性回调整段 try/catch + 只记一次日志」惯例的每 tick 回调 | ✅ **t34 已修，且已进部署 jar**。源码 `HackClientState.java:142-151`：`tickLock()` 现在只做 `try { tickLockInternal(); } catch (Throwable t) { LOCK_FAIL_LOGGED.compareAndSet → warn 一次 }`；`CyberwareClient` 调用处**再套一层** try/catch（双保险）。**产物级独立确认**（`javap -c` 部署 jar）：

```
public static void tickLock();
   0: invokestatic  tickLockInternal:()V
   3: goto 29
   6: astore_0                       ← catch
   7: getstatic     LOCK_FAIL_LOGGED
  12: invokevirtual AtomicBoolean.compareAndSet:(ZZ)Z
  24: invokeinterface Logger.warn
Exception table: from 0 to 3 target 6 Class java/lang/Throwable
```

（字段 `private static final AtomicBoolean LOCK_FAIL_LOGGED` 与方法 `tickLockInternal()` 同在包内。） |
| **F2** | ~~low~~ → **已修正** | 第一轮我发现：`RamSystem` 的注释/TODO 仍写「没装接入仓就是 0 —— 花光之后不会自己回」，但 `BASE_RAM_REGEN = 1.0D` 已落地，该分支对裸机已成死分支 | ✅ 当前源码 `RamSystem.java:122-126` 已改写为：「恢复速率为 0 的唯一途径：把 `BASE_RAM_REGEN` 调成 0 **且**没装任何给 RAM_REGEN 的接入仓/配平」「裸机默认是 1.0/分钟」，TODO 改为「`BASE_RAM_REGEN` 数值可调，0 也合法」——**与我指出的问题一致**；`:107` 另写明「上限 = max(8, ΣRAM)（邮件IX §二），所以裸机也会拿到 8 点，不是 0」 |
| **F3** | info | `en_us.json` 本轮新增 `key.cyberware.scan`（上一轮加了 radial/overclock）—— 与 0.3.12「只补中文」旧裁决（当时针对**物品名**）是否需要对齐 | 仍请主人**一次性**裁决 lang 策略；不影响功能 |
| **F4** | info | `HackRadialScreen` / `ScreenProjection` 自身无 try/catch（前者是 Screen；后者是被 `tickLockInternal` 与 `HackHud` 调用的纯数学工具） | 风险已随 F1 收敛：`HackHud` 侧有整段 try/catch，`tickLock` 侧现在有 F1 的双层壳；**可接受** |

**未发现阻断项**：协议兼容、服务端校验顺序、客户端零裁决、键位/lang、数值出处与待裁决项保持，全部符合契约。

---

## 3. 产物级核验（✅ 已完成）

**被验产物**：`build/libs/cyberware-0.5.0-Beta.jar`　**741555 字节**　md5 **`0e8c22bbfeb4314f2304058e13632290`**

```
$ md5sum build/libs/cyberware-0.5.0-Beta.jar  /sdcard/FCL/…/mods/cyberware-0.5.0-Beta.jar
0e8c22bbfeb4314f2304058e13632290  build/libs/cyberware-0.5.0-Beta.jar
0e8c22bbfeb4314f2304058e13632290  /sdcard/FCL/…/mods/cyberware-0.5.0-Beta.jar      ← 与部署产物同一个 jar

mods 目录里的 cyberware 条目：只剩 cyberware-0.5.0-Beta.jar（741555 B）
trash 里保留旧版：cyberware-0.4.0-Beta.jar.02e3dce0.20260930-2355（720854 B，未删）
```

### 3.1 jar 内容（zipfile，本机无 unzip）

```
总条目 583   class 104
缺失: 无（全部在包内）
```

逐类确认在包内：`client/HackClientState`（+ `$HackEntry`、`$1`）、`client/HackHud`、`client/HackRadialScreen`、
`client/ScreenProjection`、`network/HackPayload` + **`network/HackPayload$Action`**、`core/HackSystem`
（+ `$Payment`、`$Upload`）、`core/HackLibrary`（+ `$1..$5`）、`core/CyberwareStats`、`core/RamSystem`、
`client/RamClientState`、`client/OverclockWireframe`、`core/CombatEffects`、`event/CombatEffectsHandler`。

### 3.2 协议与枚举

```
$ javap -v -p -cp …jar com.dsh.cyberware.network.CyberwareNetwork
  public static final java.lang.String PROTOCOL_VERSION;
    ConstantValue: String 4                     ← 仍为 "4"（本轮未升版本）

$ javap -p -cp …jar 'com.dsh.cyberware.network.HackPayload$Action'
  … CAST, LOCKED, UPLOAD_START, UPLOAD_PROGRESS, APPLIED, CANCELLED, REJECTED, SCAN
```

**旧 7 个 action 的常量顺序一字未动，`SCAN` 排在最后** → 与 `byId(int)` 的 ordinal 解码兼容（0.4.0 旧包仍能解）。

### 3.3 数值常量（本轮替换项在字节码里确认）

`javap -c com.dsh.cyberware.core.HackLibrary`（`static {}` 的枚举构造参数 = `(id, 名称, uploadTicks, ramCost)`）：

```
overheat        : ldc "overheat"        bipush 20   iconst_4     ← upload 20 / RAM 4（A1 待裁决，未改）
short_circuit   : ldc "short_circuit"   bipush 20   iconst_5     ← upload **20**（本轮 30→20）
synapse_burnout : ldc "synapse_burnout" bipush 40   iconst_3     ← RAM 3（A4 待裁决，未改）
weapon_glitch   : ldc "weapon_glitch"   bipush 24   bipush 6     ← RAM 6（A1 待裁决，未改）
system_reset    : ldc "system_reset"    bipush 50   bipush 8     ← house 锚点（邮件IX）
```

```
$ javap -c '…HackLibrary$1'   （过热）
   9: ldc2_w  double -5.0d          → 12: invokestatic CombatEffects.armorDebuff:(…ID)V      ← 本轮 -4.0D→-5.0D（A2）
$ javap -c '…HackLibrary$2'   （短路）
  35: ldc     float  10.0f          → 37: invokevirtual LivingEntity.hurtServer:(…)Z         ← 本轮 8.0F→10.0F（A3）
```

`javap -c com.dsh.cyberware.core.CyberwareStats`：

```
maxRam(Player):
   0: ldc2_w  #101  // double 8.0d
   6: invokestatic sum:(…Ljava/lang/String;)D
   9: invokestatic Math.max:(DD)D          ← max(8, ΣRAM) ✔

regenPerMinute(Player):
   0: dconst_1                              ← 常量 1.0（BASE_RAM_REGEN）✔
   4: invokestatic sum:(…"ram_regen")D
   7: dadd                                  ← 1.0 + ΣRAM_REGEN ✔
```

### 3.4 调试命令（t33）也在 jar 内

`javap -c com.dsh.cyberware.CyberwareCommands`：

```
111: ldc "ram"                             ← /cyberware ram 子命令
119: ldc2_w double 10000.0d → 122: DoubleArgumentType.doubleArg:(DD)…
 69: invokestatic RamState.set:(Lnet/minecraft/world/entity/player/Player;D)V
 73: invokestatic RamSystem.sync:(Lnet/minecraft/server/level/ServerPlayer;)V
```

### 3.5 服务端扫描/校验（字节码）

`HackSystem` 内 `scan` / `hasKiroshiOptics` 存在（`javap -p` 可见），源码对应
`:92 NO_KIROSHI` / `:100-103 20 格 + GLOWING 1200 刻` / `:168-173` 三件套在 `:179 pay()` 之前（§1.2）。

### 3.6 Mixin 合规复跑（jar 级）

```
mixin 类 12   require=0 合计 13   非0值 无
method=[...] 去重 8 —— 全部 ok（方法名与 '(' 之间无冒号）
  calculatePitch / extractPlayerHealth / extractRenderState / render / tick
  extractRenderState(Lnet/minecraft/world/entity/Entity;…EntityRenderState;F)V
  renderLayers(…Identifier;II)V
  submit(…PoseStack;…SubmitNodeCollector;IL…AvatarRenderState;FF)V
```

与本轮源码 `git status`（mixin 目录与 `cyberware.mixins.json` 皆空）一致 → **本轮未新增 Mixin**。

> 口径说明：任务书给的 `grep -rn 'require = 0' …/mixin/ | wc -l` 在源码上得到 **16**，
> 是因为其中有 **3 处是 javadoc 里的 `{@code require = 0}` 说明文字**（`EquipmentLayerRendererMixin:36`、
> `CapeLayerMixin:31`、`EntityRendererOutlineMixin:49`）；**真实注解数是 13**（上面 javap 的注解计数为准），
> 与上一轮一致。

### 3.7 客户端契约（源码级复核，jar 同源）

- 客户端 handler 仍注册：`CyberwareClient:37 modEventBus.addListener(CyberwareClient::onRegisterClientPayloads)`，
  三包 handler 齐（`RamPayload` / `OverclockPayload` / `HackPayload`），其中 `HackPayload` 的 handler
  同时转给 `RamClientState.onHackPayload` 与 `HackClientState.onHackPayload`（覆盖 LOCKED / UPLOAD_START /
  UPLOAD_PROGRESS / APPLIED / CANCELLED / REJECTED 这些下行 action；`SCAN` 是上行，不需要客户端 handler）。
- 客户端零扣费/零效果：`grep -rnE "spend|canPay|deduct|pay\(|setData|MobEffects|addEffect|hurtServer|setHealth" src/main/java/com/dsh/cyberware/client/` → **0**。


---

## 4. 主人真机复验清单（不依赖产物，可先照此准备）

### 4.1 本轮新功能
1. **X 键扫描**：装歧路司义眼（6 件生效型号之一）按 X → 20 格内活体应发光 60 秒后自动消失；
   **不装**歧路司按 X → 应提示「未安装歧路司义眼，无法扫描」（`NO_KIROSHI`），且不扣 RAM。
2. **锁定框**：第三人称，把准星（屏幕中心 10% 框）对准生物 → 应出现折角框 + 血条 + 名称 + 5 个破解方块；
   转身/超 20 格/目标死亡 → 框应立刻消失（**不应有残影**）。重点判断：**10% 的锁定口径跟不跟手**、
   框大小（30~58px 随距离变化）是否合适、框内元素挤不挤。
3. **上传进度条**：对锁定目标释放破解 → 屏幕中央偏下 `0.72h` 出现 160×2px 红线 + `UPLOADING... X.Xs` + 二进制乱码；
   结束后 250ms 收缩淡出。重点判断：**是否被热键栏挡住**、2px 在实际 GUI 缩放下**是否看得清**。
4. **R 键上下文**：有锁定目标 → 弹破解轮盘（5 条 + RAM 消耗，RAM 不足置灰且点了不发包）；
   **无目标** → 仍应是原来的义体轮盘（行为不回退）。
5. **X 键是否与其他模组冲突**：`选项 → 按键` 里查 `歧路司扫描` 的绑定。

### 4.2 数值手感（占位项已知）
6. 裸机 8 点上限 + **t33 的 1 点/分钟基础恢复**：花光 RAM 后应能慢慢回（不再是永久 0）。
7. 短路上传 **1.0s** 是否「看得见又不拖沓」；过热破甲 −5 平值、短路伤害 10.0F 的手感。
8. 突触熔断 RAM 仍 **3**、武器故障仍 **6**、系统重置 **8** —— 这些是**待裁决占位**，不舒适属预期。

### 4.3 回归（0.4.0 已有）
9. V 键手持激活 / G 键超频（含被拒提示）/ 操作台装卸 / 容量上限 / 义眼描边（6 件）/
   P0 换世界残留（退到标题→新建世界，RAM 条与超频 HUD 应从 0/关闭开始）。

### 4.4 日志
`/sdcard/FCL/.minecraft/versions/26.1.2-NeoForge/logs/latest.log` 搜
`Mixin apply ... failed` / `InvalidInjectionException` / `Invalid name:` / `Scanned 0 target(s)`
/ `Attempted to register a payload handler twice`；并确认加载的是 `cyberware-0.5.0-Beta.jar`。

---

## 5. 结论

**总裁决：PASS（0 个阻断项）。**

- **源码级**：§1 七项全绿 —— 协议兼容（SCAN 追加在 REJECTED 之后、旧 7 个 ordinal 未动、版本仍 "4"）、
  服务端三件套校验在扣费之前、客户端零扣费/零效果（grep 0）、四键位唯一且已注册、lang 齐备、
  数值替换带出处且待裁决项未被偷改、锁定只做客户端显示。
- **产物级**：§3 已在**已部署的 jar**（`0.5.0-Beta`，md5 `0e8c22bbfeb4314f2304058e13632290`，
  与手机 mods 内同一个 md5）上逐一确认 —— 新类全在（583 条目 / 104 class）、`PROTOCOL_VERSION="4"`、
  枚举顺序正确、数值常量 `-5.0d` / `10.0f` / 短路 `bipush 20`、`maxRam=Math.max(8,ΣRAM)`、
  `regenPerMinute=1.0+Σ`、`/cyberware ram` 调试命令、mixin 13/13 `require=0` 且 8 个描述符无冒号。
- **我第一轮提的两条发现都已被处理并复核**：F1（tick 回调缺 try/catch）由 t34 修复且**已进部署 jar**
  （字节码异常表可证）；F2（RamSystem 陈旧注释）已按新基线改写。剩 F3 是 lang 策略待主人一次性裁决。

**再次声明：我没有启动过游戏。** 本报告的一切都是静态/产物级证据；
锁定手感、上传条可见性、2px 红线可辨识度、破解手感必须由主人按 §4 在真机上判断。

**再次声明：我没有启动过游戏。** 本报告的一切都是静态证据；锁定手感、上传条可见性、破解手感必须由主人在真机上判断。
