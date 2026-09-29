# 换世界后时间膨胀 / 狂暴状态残留 —— P0 修复报告（t17 交付物）

> 现场截图：`2026-09-29_22.04.24.png`（新建世界进游戏，HUD 直接显示 `Sandevistan 100% 16181.0s`
> 与 `Berserk 100% 16059.7s`）。16181s ≈ 323620 刻 ≈ 4.5 小时。
>
> 本文档里的**行号都是修复前（0.3.11 状态）**的位置；改动后行号会移动。

---

## 0. 结论速览

- 根因：**所有状态都按「绝对游戏刻」记账，而 `Level.getGameTime()` 是每个世界各自从 0 开始算的**，
  这些状态又全是**静态**的、跨世界从不重置。换世界后旧 `expireAt`/`endTick` 变成「几小时后才结束」，
  于是 ① 服务端减速/狂暴**真的继续生效**，② 客户端 HUD 显示几万秒。
- 修法：**方案 b（世界实例身份 + 每条记录用自己的世界时钟）**，客户端另加 `clear()` + 换世界检测。
- 这是**静默逻辑错误**：日志里什么都没有，编译、静态检查、单测全都抓不到，只有真机能发现（见 §8）。

---

## 1. 症状与数据流

| 现象 | 出处 |
| --- | --- |
| HUD `Sandevistan 100% 16181.0s` | `ClientTimeDilation.remainingSeconds()`（修复前 :172-176）用 `(endTick - now)/20` |
| HUD `Berserk 100% 16059.7s` | `BerserkClientState.remainingSeconds()`（修复前 :58-61）同款算法 |
| 进度条卡在 100% | `progress()` 里 `Math.min(1.0F, …)` 把巨大值夹成 1.0 |
| **实际效果仍在生效** | 服务端 `TimeDilationManager.ACTIVE` / `BerserkManager.ACTIVE` 里旧记录的 `expireAt` 远大于新世界的 now |

两个数字差 121 秒，正说明它们是**上一次会话里不同时刻**分别激活的 —— 与「旧账搬到新世界」完全吻合。

---

## 2. 根因复核（逐条，带行号）

### 2.1 共同机制

```
服务端/客户端都是：activate() 时 now = level.getGameTime();  expireAt = now + duration;
查询时：            level.getGameTime() < expireAt  → 还算生效
```

`gameTime` 是**每个世界各自**的计数器（新建世界从 0 开始），而这些表都是 `static`
（同一个 JVM 里的类静态字段，退出到标题再新建世界**不会**重置）。
于是「新世界的 0 刻 < 旧世界的 388000 刻」恒成立 —— 状态被无缝搬运到新世界。

### 2.2 服务端（**真的在生效**，这部分 captain 定位正确）

- `core/TimeDilationManager.java:29`　`private static final Map<UUID, Activation> ACTIVE`
- `core/TimeDilationManager.java:39`　`long expireAt`（绝对刻）
- `core/TimeDilationManager.java:71`　续期分支 `if (a != null && now < a.expireAt)` —— 换世界后这里**会命中**，
  等于把旧世界的「还剩下几小时」原样续到新世界（这一条 captain 概括里没单列，但是最致命的一环）
- `core/BerserkManager.java:24 / :30 / :44`　同款三件套（`ACTIVE` / `expireAt` / `state == null || now >= state.expireAt`）
- 查询侧：`TimeDilationManager.timeScaleFor`（:99）、`isOwnDilationActive`（:141）、`extend`（:160）、
  `active(Level)`（:196）、`refresh`（:212）、`prune`（:233）；
  `BerserkManager.isActive`（:55）、`damageMultiplier`（:64）、`extend`（:77）、`remainingRatio`（:97）、
  `intensity`（:113）、`remainingTicks`（:132）、`totalTicks`（:145）、`prune`（:154）

### 2.3 客户端（显示层）

- `client/ClientTimeDilation.java:55`　`private static final List<Source> SOURCES = new ArrayList<>();`
  （注意：**没有** `clear()`；`Source.endTick` 是绝对刻）
- `client/BerserkClientState.java:13-16`　`endTick / totalTicks / active` —— 同样**没有** `clear()`
- `client/CyberwareClient.java:92-96`（修复前）换世界分支只做了
  `ParticleTickClock.clear(); WeatherTickClock.reset(); AfterimageHistory.clear();`
  —— **压根没碰** 上面这两份状态（这就是为什么 HUD 只错了这两个数字）
- 额外坑：`BerserkClientState.onPayload`（:21-39）里有个防抖 `if (now >= endTick)`，
  换世界后同样不成立 —— 服务端主动发来的 `active=false` **也清不掉**客户端状态。
  所以客户端必须硬清。

### 2.4 为什么有些东西**没有**出症状

`level == null` 分支清掉的三个（粒子时钟、天气时钟、残影）恰好是「退出到标题」时会经过 null 的路径，
所以它们没事；而 dilation / berserk 两份状态**不在那个分支里**，于是原样活到了新世界。
这条对照反过来印证了根因：不是"某些状态特殊"，而是"清与不清的区别"。

### 2.5 与 captain 描述的两处出入（复核后修正）

1. **「`clear()` 全仓没有任何调用者」不完全准确**：
   `TimeDilationManager.clear()` **有 1 个调用者** —— `CyberwareCommands.java:69`（`/cyberware stop` 调试命令）；
   `BerserkManager.clear()` 才是 0 个调用者。
   结论方向不变（都**没有**在世界切换路径上被调用），但引用时要按这个来。
2. **「`dimension` 检查」并不能挡住换世界**：`TimeDilationManager` 其实**已经有** `dimension` 字段（:46）
   并在 `timeScaleFor` 里查了维度（:109）。但**新建世界的维度键还是 `minecraft:overworld`** ——
   维度键相同、时间轴不同，所以那一道检查形同虚设。这也正是我选方案 b 时**用 Level 实例身份而不是维度键**的原因。
   另外 5 个查询方法（`isOwnDilationActive` / `extend` / `active` / `refresh` / `prune`）连维度都没查。

---

## 3. 方案选择：**b（世界实例身份）**，不是 a（事件清理）

**做法**：每条 `Activation` / `State` 记住**所属 Level 实例**（弱引用），
所有查询都过同一个判据：**「世界实例一致」且「在它自己的世界时间轴上没过期」**，否则一律视为不存在。

**为什么 b 比 a（`PlayerLoggedOutEvent` / `ServerStoppedEvent` / `LevelEvent.Unload` → `clear()`）好**

1. **不依赖事件时机**：a 的正确性取决于「事件有没有在下一 tick 之前触发」「顺序对不对」。
   漏一个事件（比如某些整合包/自定义世界创建路径、`/reload`、单人退出到标题的时序），
   bug 就回来了。b 是**结构上不可能**：记录拿着旧世界的身份，新世界里永远匹配不上。
2. **失效是"天生"的，不是"被清掉"的**：即使一个事件都没触发，旧记录也**永远不会**生效 —— 失败方向安全。
3. **顺带修掉一个隐藏的兄弟 bug**：原来的 `prune(level)` 是「拿传进来的 level 的 gameTime 去比所有记录的 expireAt」。
   不同维度的时间轴根本不同，这在多人服务器上会**误删**别维度正在生效的记录（或反过来留着）。
   改成「每条记录用自己的世界时钟」后，这个问题一并消失。
4. 成本可控：一个弱引用字段 + 每个查询多一次 `==` 比较，没有新增事件订阅、没有新的生命周期假设。

**为什么弱引用（而不是强引用 Level）**：这张表是静态的。若强引用 Level，世界卸载后它会被这张表钉在内存里
（旧记录的 `expireAt` 还是「未来」，prune 也删不掉它 → 真正的内存泄漏：整个 ServerLevel 对象图）。
用弱引用后，世界一卸载引用自然被回收，而且「弱引用被回收」本身就是「这个世界已经没了 → 记录必删」的判据
（`refresh()` 里顺手删除）。

**a 还有什么用**：作为额外的「登出即释放」清理是可选的，但不是正确性所必需，本次没有加
（captain 明确要求二选一，我选 b 并给出上面的理由）。

---

## 4. 改动清单

| 文件 | 改动 |
| --- | --- |
| `core/TimeDilationManager.java` | `Activation.dimension`(ResourceKey) → `levelRef`(WeakReference&lt;Level&gt;)；新增 `belongsTo / ownerLevel / liveIn` 三个判据；`activate` 续期分支加世界一致性（**否则旧 expireAt 会被续到新世界**）；`timeScaleFor / isOwnDilationActive / extend / active` 全部改走 `liveIn`；`refresh` 跳过并清理非本世界记录、删掉世界已卸载/本世界已过期的记录；`prune(Level)` → `prune()`（每条记录用自己的世界时钟；调用方 0 处） |
| `core/BerserkManager.java` | `State` 加 `levelRef`；新增 `belongsTo / ownerLevel / validState`；`activate` 的"重新开一条"条件加上世界一致性；`isActive / damageMultiplier / extend / remainingRatio / intensity / remainingTicks / totalTicks` 全部改走 `validState`；`prune()` 同规则 |
| `event/BerserkHandler.java` | `BerserkManager.prune(level)` → `BerserkManager.prune()`（1 行） |
| `client/ClientTimeDilation.java` | 新增 `clear()`（清 `SOURCES`） |
| `client/BerserkClientState.java` | 新增 `clear()`（`endTick/totalTicks/damageMultiplier/active` 归零） |
| `client/post/SandevistanPostProcessor.java` | 新增 `resetWorldState()`（清 `intensity/previousIntensity/fovIntensity/pulseStartTick/wasActive/lastUploaded`；**故意不清** `disabled/diagnosed/chainMissingLogged` 三个一次性诊断标志，也不动 `RETIRED` GPU 缓冲） |
| `client/CyberwareClient.java` | 新增 `private static Level lastLevel`；`onClientTick` 里**先**判 `minecraft.level != lastLevel` → 六份状态全清并更新引用；`SandevistanPostProcessor.tick()` 移到清理**之后**；`level == null` 分支改为直接 return。**V 键 / R 键两段逻辑一行未动**（t13/t14 成果原样） |

`clear()`（服务端两个管理器）**保留**：`/cyberware stop` 还在用，且它现在只是「调试用全清」，
正确性不再依赖它。

---

## 5. 修复后各场景的期望行为

| 场景 | 期望 |
| --- | --- |
| 单人「退出到标题 → 新建世界」 | 客户端：六份状态在进入新世界的第一 tick 全清（`level == null` 时也会清一次）；服务端：旧记录因世界实例不同**永不生效**，且被 `refresh()/prune()` 回收 |
| 直接 A 世界 → B 世界（不经 null） | 由 `lastLevel` 引用比较捕获（这正是 captain 提醒的坑） |
| 同一世界的维度穿越（主世界 ↔ 下界） | 服务端本来按维度隔离；现在按实例隔离，语义不变；客户端 Level 也换了 → 状态清空 |
| 玩家登出再登录（同一世界） | 服务端记录仍在（这是原设计：kill-extend 之类的时限继续走），但**只对同一个世界实例生效**；世界没换就不会误清 |
| 服务器重启/新建世界 | 静态表里旧世界的记录 `levelRef` 会被回收 → `refresh()` 删除；即使没来得及删也不会生效 |
| 旧世界的减速/狂暴 | 新世界里**不再有任何效果**，HUD 归零 |

---

## 6. 同类隐患扫描（全仓「静态 + 绝对时间轴」清单）

扫描方式：列出全仓所有静态字段（含 `static final` 可变容器），逐个判断「是否按绝对时间记账 / 是否跨世界残留」。

| 位置 | 状态 | 结论 |
| --- | --- | --- |
| `core/TimeDilationManager.ACTIVE` | 绝对刻 `expireAt` | **P0 主角，已修** |
| `core/BerserkManager.ACTIVE` | 绝对刻 `expireAt` | **P0 主角，已修** |
| `client/ClientTimeDilation.SOURCES` | 绝对刻 `endTick` | **已修**（新增 `clear()` + 换世界调用） |
| `client/BerserkClientState` | 绝对刻 `endTick` | **已修** |
| `client/post/SandevistanPostProcessor.intensity / previousIntensity / fovIntensity / pulseStartTick / wasActive / lastUploaded` | 渐变值 + 绝对刻 `pulseStartTick` | **同类隐患，已修**（`resetWorldState()`）；`pulseStartTick` 顺带发现是**只写不读**的死字段 |
| `client/WeatherTickClock.virtualTicks / lastRealTick` | **绝对刻** `lastRealTick` + 累加虚拟时间轴 | **同类隐患，已纳入清理**。不清的后果：新世界 `delta = realTick - lastRealTick` 为负 → `Math.max(0,…)` → 虚拟时间**冻在旧世界的值**上（雨雪位置错乱）。原来只在 `level == null` 时 reset |
| `client/AfterimageHistory.RING / lastSampleTick` | 绝对刻 `lastSampleTick`（只做 `==` 比较，无大小假设） | 半同类：不会有"卡死"；但 `RING` 留着**旧世界的实体渲染快照** → 不清会在新世界里画出几秒的幽灵残影。已纳入清理 |
| `client/ParticleTickClock.CLOCKS` | `WeakHashMap<Particle, Clock>`，只存累加余量，**无绝对刻** | 非隐患（键是粒子对象，回收即消失）。已纳入清理只为省内存 |
| `client/AfterimageRenderer.ghostAlpha` | 渲染期的临时 alpha，`finally` 里归零 | 非隐患（不跨帧残留） |
| `mixin/client/ParticleMixin.CYBERWARE$AGE` | `WeakHashMap<Particle, double[]>` 相对累加 | 非隐患（无绝对刻；键随粒子回收） |
| `event/TimeDilationHandler.lastRefreshTick` | **绝对刻**，仅用于「同一刻不重复刷新」 | 非隐患但不严谨：换世界后最多多刷/漏刷一次 20 刻周期（≤1 秒的区域中心刷新延迟），不会造成状态残留。**未改**（避免扩大改动面），列在这里备查 |
| `core/DilationTickGate`（全静态方法，无字段） | 用 `gameTime + entityId` 算相位 | 非隐患：它是**相位函数**不是时长记账，换世界只是换了相位 |
| `mixin/client/EquipmentLayerRendererMixin.LOGGED / RT_LOGGED`、`EntityRendererOutlineMixin.cyberware$outlineErrorLogged`、`SandevistanPostProcessor.disabled / diagnosed / chainMissingLogged`、`AfterimageHistory.HANDLES / CTORS` | 日志去重 / 反射缓存 / 降级标志 | 非隐患，**故意不清**（清了会重复刷日志、重复尝试已坏掉的链） |
| `core/CyberwareDefinitions.BY_ID`、`registry/ModItems.CYBERWARE`、`CyberwareConfig.*`、各种注册表 | 静态常量式数据 | 非隐患（与时间轴无关，也不该清） |
| `client/CyberwareRadialScreen` | 只有常量 + Screen 实例状态 | 非隐患（实例随界面关闭丢弃），未动 |

**没发现其它遗漏**：全仓静态字段就这些，逐一给过结论。

**另一个顺手发现（未修，不在本 bug 范围）**：`/cyberware stop`（`CyberwareCommands.java:69`）
只清了 `TimeDilationManager.clear()`，**没有**清 `BerserkManager.clear()` —— 调试命令语义不一致。
本任务不动它（避免扩大改动面），留作后续。

---

## 7. 未做 / 留给后续

1. 没有采用方案 a 的任何事件订阅（理由见 §3）。
2. `TimeDilationHandler.lastRefreshTick` 未清（非隐患，见 §6）。
3. `/cyberware stop` 不清狂暴（见 §6 末尾）。
4. 维度穿越时「减速是否该跟着施法者走」是**设计问题**不是 bug（现语义：按世界/维度隔离，跨维度不生效）；
   本任务保持原语义，只把"实例身份"补齐。
5. **未真机验证**（见下）。

---

## 8. 未真机验证 —— 以及为什么这个 bug 只能靠真机发现

- **本次修复未真机验证**：本环境跑不起 Minecraft（无客户端/服务端运行时），
  只做到 `gradle compileJava` 通过 + 字节码级核对（§9）。以下必须在游戏里复验：
  - 退出到标题 → 新建世界：HUD 两条都要**从 0 开始**（不再出现 16181s / 16059.7s）；
  - 新世界里**没有任何**减速/狂暴效果（观察怪物是否恢复原速、狂暴的伤害翻倍/无敌是否消失）；
  - 同一世界内 `/cyberware dilate`、狂暴激活、击杀延长、HUD 进度条**仍然正常**（别把功能改坏）；
  - 维度穿越（主世界↔下界）后状态不串味；
  - 多人/集成服务器登出重进不复现。

- **这个 bug 日志里看不到**：它是**静默逻辑错误**，不是异常 ——
  没有 exception、没有 error/warn 日志（唯一相关日志是 `sendOverlayMessage` 之类的正常输出），
  代码路径全部"正常执行"，只是**算错了**。所以：
  - 编译（`compileJava`）通过 —— 抓不到；
  - 静态检查/字节码核对 —— 只能证明"我改成了什么样"，抓不到"换世界后 now 会重置"这个运行时事实；
  - 单测（本仓没有运行时测试框架）—— 需要真实 `Level` 与跨世界流程才能复现。
  **唯一抓到它的是真机**：主人新建世界进游戏，看见 HUD 上的 16181s/16059.7s 才发现。
  这条经验值得记住：**所有"按 gameTime 记账的静态状态"都必须能回答"换世界时怎么办"**。

---

## 9. 本次验证方式（可复核）

```bash
# 1) 编译（只跑 compileJava，按硬约束不跑完整 build、不 commit）
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-arm64 PATH=$JAVA_HOME/bin:/opt/gradle-8.10.2/bin:$PATH \
  gradle compileJava --no-daemon
# → BUILD SUCCESSFUL（> Task :compileJava 实际执行，0 error）

# 2) 字节码核对（证明改动真的落地，不只是写了注释）
javap -p build/classes/java/main/com/dsh/cyberware/core/BerserkManager.class
#   → belongsTo / ownerLevel / validState / prune() / clear()
javap -p 'build/classes/java/main/com/dsh/cyberware/core/BerserkManager$State.class'
#   → private java.lang.ref.WeakReference<net.minecraft.world.level.Level> levelRef
javap -p build/classes/java/main/com/dsh/cyberware/core/TimeDilationManager.class
#   → belongsTo / ownerLevel / liveIn / prune() / clear()
javap -p 'build/classes/java/main/com/dsh/cyberware/core/TimeDilationManager$Activation.class'
#   → levelRef（dimension 字段已消失）
javap -c -p build/classes/java/main/com/dsh/cyberware/client/CyberwareClient.class
#   → lastLevel 比较 + ClientTimeDilation.clear / BerserkClientState.clear /
#     SandevistanPostProcessor.resetWorldState / ParticleTickClock.clear /
#     WeatherTickClock.reset / AfterimageHistory.clear
```
