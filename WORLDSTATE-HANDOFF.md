# WORLDSTATE-HANDOFF.md —— P0 收尾：换世界状态残留修复复核 + 交付（t18 / inspector）

工作区：`/root/mod26/cyberware`　本任务 attempt_id：`9fc333f6-c77f-42f9-8630-8bdd2fcca6e8`
范围：独立复核 t17（coredev）的 P0 修复 → 全量构建 → 静态自查 → commit → 部署 FCL → 真机复验清单。
本任务只新增本报告、执行授权范围内的 git 提交与 jar 部署；**未修改任何他人负责的源码逻辑**。

---

## 0. 结论

| 项 | 结果 |
|---|---|
| 判据是否为 **Level 实例身份**（不是维度键） | ✅ 两个 manager 都改成 `WeakReference<Level>` + `==` 引用比较；`dimension` 字段已彻底消失 |
| **最关键的一条**：`activate()` 续期分支的世界一致性 | ✅ `TimeDilationManager` L87 与 `BerserkManager` L63 都加了；换世界必重开新记录 |
| `WeakReference` 用法 | ✅ 无强引用钉住世界；被回收即判失效；`refresh/prune` 顺手回收记录 → 不会无界增长 |
| 客户端三份 `clear()` / `resetWorldState()` | ✅ 存在且清得干净；故意不清的 4 项**我逐条复核后同意**（理由见 §1.4） |
| `CyberwareClient` 用 Level 引用比较 + 清理时机 | ✅ 字节码 `if_acmpeq`（引用比较）；六份清理**全部在 `SandevistanPostProcessor.tick()` 之前**（§1.5） |
| 同类隐患扫描复核 | ✅ 抽查 3 条 non-hazard 结论 + 3 处已修项，**全部成立**（§1.6） |
| 全量构建 | ✅ `gradle build --no-daemon` exit 0；另跑 `--rerun-tasks --no-build-cache` **真编译 4 executed / 0 error** |
| 静态自查 | ✅ mixin 本轮零改动（jar 内复跑 13/13 `require=0`、无冒号）；定义表 135/19/154；贴图 135×32×32；zh_cn 166；en_us 49 md5 未变 |
| commit | ✅ `2d6abbbe9271633f51c83df51baded77c1bf006e`，8 files, **+481 / −71**（未 push） |
| 部署 | ✅ mods 里 cyberware 只剩新 jar（664396 B）；round2 jar 进 trash |
| **总裁决** | **PASS** —— coredev 的判据没有漏洞；我另列 4 条 low 观察（§1.7），均不阻断 |

> ### ⚠ **这个 bug 日志里看不到。**
> 它是**静默的逻辑错误**：没有异常、没有 WARN、没有报错栈 —— 服务端只是「以为还在生效」，
> 客户端只是「照着旧世界的绝对刻算出一个几万秒」。**静态验证（编译、javap、grep）也抓不到它**，
> 因为代码每一行都「合法」，错的只是**跨世界的时间轴语义**。
> **只有真机能发现**（真实地退到标题 → 新建世界 → 看 HUD 与怪物速度）。
>
> ### ⚠ **编译通过不等于注入成功，静态验证也不等于玩得对。**
> 本报告全部结论都是静态层面的；「新世界真的没有残留、同世界内不回归」必须由主人真机复验。

---

## 1. 独立复核（我自己动手验，不是复述 coredev）

复核方法：读源码 + `git diff` + **对最终 jar 做 `javap -p -c` 字节码核对**。

### 1.1 判据是 Level **实例**身份，不是维度键 ✅

| 文件 | 记录字段（产物级 javap） | 判据 |
|---|---|---|
| `TimeDilationManager$Activation` | `private java.lang.ref.WeakReference<net.minecraft.world.level.Level> levelRef;` | `belongsTo` L117：`a.levelRef.get() == level` |
| `BerserkManager$State` | `private java.lang.ref.WeakReference<net.minecraft.world.level.Level> levelRef;` | `belongsTo` L81：`state.levelRef.get() == level` |

- 两处 `dimension` **字段已完全消失**（`javap -p` 里查不到；源码里只剩两行解释性注释）。
- 关键理由成立：新建世界的维度键仍是 `minecraft:overworld`，但时间轴不同 →
  维度键判据挡不住换世界，**只有实例身份能区分**。
- 汇总式判定：`TimeDilationManager.liveIn`（L133-135）= `belongsTo && level.getGameTime() < a.expireAt`；
  `BerserkManager.validState`（L94-108）= `owned != current → null`，再用 `current.getGameTime()` 判过期。

### 1.2 ⭐ 最关键的一条：`activate()` 的续期分支加了世界一致性 ✅

**TimeDilationManager（源码 L84-102）**：

```java
 84: // ⚠ 只有「同一个世界实例里还没过期」才允许续期。
 87: if (a != null && belongsTo(a, level) && now < a.expireAt) {   // ← belongsTo 就是新增的那道闸
 89:     if (clamped > a.ratio) { a.ratio = clamped; a.source = sourceId; }
 93:     a.expireAt = Math.max(a.expireAt, now + durationTicks);
 94: } else {
 95:     a = new Activation();          // ← 换世界 → 当成全新一条重开
 ...
101:     ACTIVE.put(player.getUUID(), a);
102: }
```

**BerserkManager（源码 L61-69）**：

```java
 61: // ⚠ 换了世界（Level 实例不同）必须**重开一条**，绝不能续用旧 expireAt
 63: if (state == null || !belongsTo(state, level) || now >= state.expireAt) {
 64:     state = new State();
 67:     state.levelRef = new WeakReference<>(level);
 68:     ACTIVE.put(player.getUUID(), state);
 69: }
```

**我的判断**：这一条确实是最致命、也最容易漏掉的一环 —— 光在查询侧加判定还不够，
因为 `activate` 的续期分支会**先读到旧记录**并把「旧世界还剩几小时」原样续写到新记录上，
再往后所有查询都会「正确地」认为它是本世界的。现在两处都在**续期之前**就挡住了。
（t17 报告把这条单独标出来是对的；captain 最初的概括里没有它。）

### 1.3 `WeakReference<Level>` 用法正确 ✅

- 记录里**只有弱引用**，没有任何强引用字段（javap 已确认两个 `$Activation` / `$State` 只有 `levelRef` 一个 Level 相关字段）。
- 弱引用被回收 → `ownerLevel(a) == null` → `belongsTo` 恒 false → 记录**天然失效**；
  同时 `refresh()`（TDM L264-268）与 `prune()`（TDM L302-304 / BM L193-195）会把它从静态表里 **remove**。
- 因此不会出现「静态表把已卸载的 ServerLevel 钉住 → expireAt 还在未来 → 永远删不掉 → 真泄漏」。
- 反过来的时序也安全：只要还有谁能拿到那个 Level 实例并查询它，就说明那个世界**还没被卸载**，此时记录生效是正确行为。

### 1.4 客户端清理：清了什么、故意不清什么，以及我对取舍的判断 ✅

`CyberwareClient.onClientTick` 的换世界分支一次调用 **6 份**清理（源码 L104-110）：

| # | 调用 | 清掉的内容 | 我的判断 |
|---|---|---|---|
| 1 | `ClientTimeDilation.clear()` | `SOURCES`（每条含绝对 `endTick`） | 必要 —— 不清则 HUD 显示 `16181.0s` |
| 2 | `BerserkClientState.clear()` | `endTick/totalTicks/damageMultiplier/active` | 必要 —— 对应 `Berserk 100% 16059.7s` |
| 3 | `SandevistanPostProcessor.resetWorldState()` | `intensity / previousIntensity / fovIntensity / pulseStartTick / wasActive / lastUploaded` | 必要 —— 否则新世界第一帧按旧强度闪屏 |
| 4 | `ParticleTickClock.clear()` | 粒子时钟表 | 内存卫生 |
| 5 | `WeatherTickClock.reset()` | `lastRealTick=MIN_VALUE, virtualTicks=0, lastTimeScale=1` | 必要 —— 否则虚拟刻数从旧世界接着跳 |
| 6 | `AfterimageHistory.clear()` | `RING` + `lastSampleTick=MIN_VALUE` | 必要 —— 否则残影按旧刻取样 |

**故意不清的 4 项 —— 我逐条独立复核后同意 coredev 的取舍**：

| 不清的 | 它是什么 | 我同意保留的理由 |
|---|---|---|
| `SandevistanPostProcessor.disabled` | 后处理链一次出错后的**永久停用闸** | 清了会在每个新世界重新尝试一条**已经坏掉**的链 → 反复失败。保留 = 保守且正确 |
| `diagnosed` / `chainMissingLogged` | 一次性日志去重标志 | 清了最多多打一行日志，但会随「每次换世界/换维度」重复刷；保留更干净 |
| `RETIRED` | `List<GpuBuffer>`，等 `RETIRE_DELAY=3` 帧再 `close()` 的显存缓冲（源码 L89-91 / L296-306） | 它**与世界无关**：清空这个 list 会让正在等 fence 的 buffer 既不被释放也不被记录 → 真泄漏；保留是对的 |

结论：**取舍合理**。这 4 项都不是「按世界时间轴记账」的状态，清它们没有收益、只有坏处。

### 1.5 `CyberwareClient` 用 Level 引用比较，且清理发生在后处理 tick **之前** ✅

对最终 jar 的字节码（`javap -p -c …CyberwareClient`，`onClientTick` 片段）：

```
 51: getfield      Minecraft.level:Lnet/minecraft/…/ClientLevel;
 54: getstatic     lastLevel:Lnet/minecraft/world/level/Level;
 57: if_acmpeq     85        ← 引用比较（== / !=），不是 dimension 键比较
 64: putstatic     lastLevel:…
 67: invokestatic  ClientTimeDilation.clear:()V
 70: invokestatic  BerserkClientState.clear:()V
 73: invokestatic  SandevistanPostProcessor.resetWorldState:()V
 76: invokestatic  ParticleTickClock.clear:()V
 79: invokestatic  WeatherTickClock.reset:()V
 82: invokestatic  AfterimageHistory.clear:()V
 85: invokestatic  SandevistanPostProcessor.tick:()V      ← 后处理 tick 在 6 份清理之后
157: invokevirtual Level.getGameTime:()J
162: invokestatic  WeatherTickClock.tick:(ID)V
```

- 用 `if_acmpeq` 比较 `minecraft.level` 与 `lastLevel` → **引用身份**，能抓到「A 世界直接进 B 世界」（中间不经过 null）✅
- 6 份清理全部在 `SandevistanPostProcessor.tick()` **之前**，符合任务要求 ✅
- `level == null`（退出世界）时 `return` 在 `tick()` 之后 —— 与旧行为一致，且此时状态已被清过 ✅

**我额外核实的一点**：`lastLevel` 是**强引用**（与两个服务端 manager 用弱引用不同）。
我按生命周期推演过：世界→null、世界→新世界两种切换都会在**同一个 tick** 里把 `lastLevel` 覆盖成新值/null，
所以它不会长期钉住旧 `ClientLevel`（不是泄漏），只是与「弱引用」策略风格不一致。
**判断：可接受**，仅记录（§1.7-F4）。

### 1.6 同类隐患扫描复核：抽查 3 条已修 + 3 条「非隐患」✅

**已修的三处（我逐个确认不是只写了注释）**：

| 处 | 证据 |
|---|---|
| `SandevistanPostProcessor.resetWorldState()` | `resetWorldState` 方法存在（源码 L156-163），字节码 `invokestatic` 确认被调用（§1.5） |
| `WeatherTickClock.lastRealTick` | `reset()` L64-68 把 `lastRealTick = Integer.MIN_VALUE`、`virtualTicks = 0`、`lastTimeScale = 1`；被调用（§1.5） |
| `AfterimageHistory.RING` | `clear()` L109-112 清 `RING` 并 `lastSampleTick = Integer.MIN_VALUE`；被调用（§1.5） |

**「非隐患」结论抽查（我自己挑的 3 条，全部成立）**：

1. `mixin/client/ParticleMixin.CYBERWARE$AGE` —— 我读了源码 L44/L66-76：它是
   `WeakHashMap<Particle, double[]>`，里面累加的是 `(1.0 - timeScale)`，**没有绝对刻**；
   key 是粒子对象，粒子回收即消失。**结论：确实非隐患** ✅（coredev 说对了）
2. `core/DilationTickGate` —— 我通读了整个文件：只有 `MIN_TIME_SCALE` 常量，
   `shouldSkip` = `floorMod(gameTime + entity.getId(), period) != 0`，是**相位函数**而非时长记账。
   换世界只改变相位（最坏是这批实体在新世界的第一刻同时跳/不跳一次），**不残留状态**。
   **结论：确实非隐患** ✅
3. `event/TimeDilationHandler.lastRefreshTick` —— 源码 L49/L109-111：`gameTime % 20 == 0 && lastRefreshTick != gameTime`。
   换世界后新 gameTime 从 0 起，**最迟 20 刻（1 秒）内必定命中**，于是 `TimeDilationManager.refresh` /
   `BerserkHandler.refresh` 会在 ≤1 秒内跑一次 —— 这同时还保证了**已卸载世界的记录会被清掉**。
   **结论：确实非隐患**（影响只是换世界后 ≤1 秒的区域中心刷新延迟）✅

**我还独立做了一遍「谁在拿 A 世界的 now 比 B 世界的 expireAt」的普查**（两个 manager 内所有 `getGameTime()`）：

```
TimeDilationManager:76/134/147/216/260/303     BerserkManager:59/107/150/160/173/194
```

逐条看：全部要么是「查询方实体自己的 level（已过身份判定）」，要么是「记录自己的 ownerLevel」。
**没有任何一处还在跨世界比较时间** ✅ —— 这正是 §1.2 之外最容易漏的第二类漏洞，这里干净。

### 1.7 我的补充观察（4 条 low，均不阻断）

| # | 级别 | 观察 | 我的判断 |
|---|---|---|---|
| F1 | low | `TimeDilationManager.prune()` **0 个调用者**（`BerserkManager.prune()` 由 `BerserkHandler:61` 调用） | 不是 bug：清理职责已由每 20 刻的 `refresh(level)` 承担（它含同样的回收逻辑）。属**死代码**，可留可删，建议留（与 Berserk 对称，且将来手工排查有用） |
| F2 | low | `BerserkHandler.refresh` 把 `prune()` 写在 **per-player 循环里**（L57-61），而非循环后调用一次 | 功能正确（幂等），只是同一批记录可能被扫多次。表大小 ≈ 玩家数，开销可忽略。**不建议为此改动刚修好的 P0 代码** |
| F3 | low | `TimeDilationManager.activate()` 跨世界时会用新记录**覆盖**旧世界那条记录 | 无害：玩家同一时刻只可能在一个世界；旧记录的中心跟随 owner 位置，owner 已不在该世界，本来也不会再作用到任何人 |
| F4 | low | 客户端 `lastLevel` 是**强引用**，服务端两个 manager 用弱引用（策略不一致） | 生命周期推演后**不构成泄漏**（见 §1.5），仅记录风格差异；若将来要统一，改成弱引用即可 |

**F1/F2/F3/F4 都不影响本 P0 的正确性，因此总裁决仍是 PASS。**

---

## 2. 全量构建

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-arm64
export PATH=$JAVA_HOME/bin:/opt/gradle-8.10.2/bin:$PATH
gradle build --no-daemon                                   # 任务书指定口径
gradle build --no-daemon --rerun-tasks --no-build-cache     # 补一次真编译，排除缓存假绿
```

| # | 命令 | 耗时 | 退出码 | compileJava |
|---|---|---|---|---|
| 1 | `gradle build --no-daemon` | 45s | **0** | `UP-TO-DATE`（t17 已真编译） |
| 2 | `gradle build --no-daemon --rerun-tasks --no-build-cache` | 1m 43s | **0** | **真实执行（4 executed）** |

口径 2 的日志尾部：

```
> Task :compileJava … > Task :jar / :assemble / :check / :build
BUILD SUCCESSFUL in 1m 43s
4 actionable tasks: 4 executed
EXIT=0
```

**产物（以口径 2 的最终 jar 为准，下文全部验证与部署都用它）**：

```
build/libs/cyberware-0.3.12-Beta.jar
  664396 字节   md5 db16bd60396854aba9e77f377dbf2bbb
```

日志：`/tmp/t18-build.log`、`/tmp/t18-forcebuild.log`。

**产物级复核（javap，证明改动真的落地而不只是注释）**：

```
com.dsh.cyberware.core.TimeDilationManager$Activation
  private java.lang.ref.WeakReference<net.minecraft.world.level.Level> levelRef;     （无 dimension 字段）
com.dsh.cyberware.core.TimeDilationManager
  belongsTo(Activation,Level) / ownerLevel(Activation) / liveIn(Activation,Level) / prune() / clear() / refresh(Level)
com.dsh.cyberware.core.BerserkManager$State
  private java.lang.ref.WeakReference<net.minecraft.world.level.Level> levelRef;     （无 dimension 字段）
com.dsh.cyberware.core.BerserkManager
  belongsTo(State,Level) / ownerLevel(State) / validState(Player) / prune() / clear()
```

---

## 3. 静态自查（沿用前两轮口径）

| 项 | 结果 |
|---|---|
| Mixin 冒号陷阱 | ✅ **本轮 mixin 目录零改动**（`git status --porcelain -- …/mixin/` 空）；对最终 jar 复跑：12 个 mixin 类、`require=0` **13** 处、非 0 值 0、8 个 `method=[...]` 去重后**无冒号** |
| 定义表 | ✅ 135 生效 / 19 注释 / 154 合计（与上两轮一致） |
| 贴图 | ✅ 物品贴图 135 张**全 32×32**（非 32×32 列表为空）；11 张特殊图全 16×16 |
| lang | ✅ zh_cn 166 条（md5 `b73796f8…`）；**en_us 49 条、md5 `db559925…` 未变** |
| 构建脚本 | ✅ `build.gradle` / `settings.gradle` 未改动（无新依赖） |

---

## 4. commit（本地，未 push）

**显式清单**（`/tmp/t18-add.txt` → `git add --pathspec-from-file`，**未用 `git add -A`**）：

```
WORLDSTATE-FIX.md
src/main/java/com/dsh/cyberware/client/BerserkClientState.java
src/main/java/com/dsh/cyberware/client/ClientTimeDilation.java
src/main/java/com/dsh/cyberware/client/CyberwareClient.java
src/main/java/com/dsh/cyberware/client/post/SandevistanPostProcessor.java
src/main/java/com/dsh/cyberware/core/BerserkManager.java
src/main/java/com/dsh/cyberware/core/TimeDilationManager.java
src/main/java/com/dsh/cyberware/event/BerserkHandler.java
```

混入检查：`.jar` / `build/` / `.gradle/` / `run/` / `LICENSE` **零命中**。

```
commit 2d6abbbe9271633f51c83df51baded77c1bf006e
Message: 修 P0：换世界后时间膨胀/狂暴状态残留（服务端按 Level 身份失效 + 客户端六份静态状态清理）

 WORLDSTATE-FIX.md                                  | 228 +++++++++++++++++++++
 .../client/BerserkClientState.java                 |  17 ++
 .../client/ClientTimeDilation.java                 |  14 ++
 .../client/CyberwareClient.java                    |  41 ++-
 .../client/post/SandevistanPostProcessor.java      |  23 +++
 .../core/BerserkManager.java                       | 113 ++++++----
 .../core/TimeDilationManager.java                  | 113 +++++++---
 .../event/BerserkHandler.java                      |   3 +-
 8 files changed, 481 insertions(+), 71 deletions(-)
```

（提交前工作树：7 M + 1 ??；提交后 `git status --porcelain` = **0 行**，干净。）
**未执行 `git push`。**

---

## 5. 部署到 FCL

**部署前**：

```
<…/26.1.2-NeoForge/mods>
  -rw-rw---- 1 10000 1023 663507 Sep 29 13:03 cyberware-0.3.12-Beta.jar   md5 60401290cb49cc3a668cba3baf44f368  ← round2 那版
```

**执行**：

```bash
mv <mods>/cyberware-0.3.12-Beta.jar  /sdcard/DSH/trash/cyberware/cyberware-0.3.12-Beta-round2.jar
cp /root/mod26/cyberware/build/libs/cyberware-0.3.12-Beta.jar  <mods>/
sync
```

实测：

```
renamed '…/mods/cyberware-0.3.12-Beta.jar' -> '/sdcard/DSH/trash/cyberware/cyberware-0.3.12-Beta-round2.jar'
'…/build/libs/cyberware-0.3.12-Beta.jar' -> '…/mods/cyberware-0.3.12-Beta.jar'
```

**部署后核对**：

```
=== mods 里 cyberware 条目（只剩一个）===
cyberware-0.3.12-Beta.jar   664396 字节   Sep 29 14:57

=== md5 两侧一致 ===
db16bd60396854aba9e77f377dbf2bbb  <mods>/cyberware-0.3.12-Beta.jar
db16bd60396854aba9e77f377dbf2bbb  /root/mod26/cyberware/build/libs/cyberware-0.3.12-Beta.jar

=== trash 里 0.3.12 的三代 ===
cyberware-0.3.12-Beta-round1.jar   655477 B  （第一轮：义体轮盘之前）
cyberware-0.3.12-Beta-round2.jar   663507 B  （第二轮：R 键轮盘）
（本轮 664396 B 在 mods 里）
```

mods 目录其余 14 个第三方 jar 未动（目录共 15 项）。

---

## 6. 真机复验清单（主人操作；**这一步不能省**）

> 再说一次：§1 的全部结论都是静态的。**这个 bug 只有真机能证伪/证实。**

### 6.1 P0 复验（5 项，逐项看结果）
1. **① 新建世界 HUD 归零**：先在有义体的世界里开一次斯安威斯坦/狂暴（让状态非零），
   然后**退到标题** → **新建世界** → 看左下/左侧 HUD：
   应**完全没有** `Sandevistan …%` / `Berserk …%` 条；**不应再出现 16181.0s / 16059.7s**（旧版症状）。
2. **② 新世界无残留效果**：新世界里怪物/箭矢应是**原速**（不是慢动作）；狂暴的**无敌与伤害翻倍应消失**
   （打自己一下/看受击掉血即知）。
3. **③ 同世界内不回归**：在同一世界里用 `/cyberware dilate …`、开狂暴、
   击杀延长（打怪看时限是否被推后）、HUD 进度条都应与 0.3.12 之前**一致**，不能出现「时限越用越多」或「进度条不动」。
4. **④ 维度穿越不串味**：主世界 ↔ 下界来回走一趟，两边都应**各自正常** ——
   在下界开的减速不应影响主世界的人，回到主世界也不应看到下界的残留。
5. **⑤ 登出重进不复现**：完全退出游戏再进（甚至重启启动器），HUD 与效果都应是干净的。

### 6.2 保留项（上一轮清单，防止回归）
- **按住 R** → 义体轮盘弹出（需先装过斯安威斯坦或狂暴；没装则不出轮盘）→ 鼠标指向 → **松开施放**；
  中心死区不施放；ESC 只关不施放。
- **V 键**手持激活仍可用。
- **操作台装卸**：快捷栏义体 → 开操作台 → 放底部槽 → 安装/卸载；标题栏 `容量 已用/上限`。
- **容量上限 100**（`config/cyberware-common.toml`：`defaultCapacity = 100`、`enableCapacityLimit = true`）。
- **义眼描边**（只能验当前生效的 **6 件**：基础歧路司义眼 / 1型 / 神舆 / 祸兆 / 千里目 / 石化鸡蛇）：
  同队绿、中立黄、敌对红，**卸下义眼后描边消失**。
  ⚠ `kiroshi_optics_piercing` / `kiroshi_optics_sensor` 无素材未注册，**游戏内不存在属正常**。
- ⚠ 12 件新义体（代谢编辑器等）槽位/稀有度/容量是 TODO 初值，分类可能不准，**不代表 bug**。

### 6.3 日志（注意：P0 这个 bug **不会**在这里留痕）

```
文件：/sdcard/FCL/.minecraft/versions/26.1.2-NeoForge/logs/latest.log
```

搜索关键字（这些是**注入/加载失败**的信号，命中就要回报）：

| 关键字 | 含义 |
|---|---|
| `Mixin apply` + `failed` | 某 mixin 应用失败 |
| `InvalidInjectionException` | 注入点匹配/描述符出错 |
| `Invalid name:` / `InvalidMemberDescriptorException` | 描述符写法错（冒号陷阱） |
| `Scanned 0 target(s)` / `Cannot find method` | 目标没匹配到（`require=0` 下静默跳过 → 功能隐形失效） |

同时确认加载的是新版本：

```bash
grep -n "cyberware-" …/latest.log            # 应出现 cyberware-0.3.12-Beta.jar
grep -n "Cyberware 义体系统" …/latest.log    # 版本号应为 0.3.12-Beta
```

> ⚠ **反过来不成立**：`latest.log` 里「什么都没有」**不等于** P0 修好了 ——
> 这个 bug 本来就是静默的。**必须靠 §6.1 的 5 项实机行为判断。**

### 6.4 回滚
把 `/sdcard/DSH/trash/cyberware/` 里的 `cyberware-0.3.12-Beta-round2.jar`（第二轮）或
`cyberware-0.3.12-Beta-round1.jar`（第一轮）或 `cyberware-0.3.11-Beta-Hotfix-10.jar` 拷回 mods 目录即可。
**先把当前 jar 移走，不要两个 cyberware jar 并存。**

---

## 7. 一句话总结

coredev 的 P0 修复**判据正确、覆盖完整**：服务端两个 manager 都改用 **Level 实例身份 + 弱引用**，
**续期分支**（最容易漏的一环）也堵上了；客户端六份状态在**后处理 tick 之前**按 Level 引用比较清理，
故意不清的 4 项我复核后同意。我另做了一遍「跨世界比时间」的普查，**干净**。
静态层面全部通过、构建 exit 0、已提交（`2d6abbb`）并部署。

但**这个 bug 日志里看不到、静态验证也抓不到** —— 请主人按 §6.1 的五项真机复验：
**编译通过不等于注入成功，静态验证也不等于玩得对。**
