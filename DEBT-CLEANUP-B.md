# 技术债清理 B —— t22 交付物

> 范围：`core/**`、`CyberwareCommands.java`、`event/**`（未动 `client/**`、`mixin/client/**`、`resources/**`）
> 验证：`gradle compileJava --no-daemon` → BUILD SUCCESSFUL；未跑完整 build、未 commit。

---

## 0. 结论

| 项 | 决定 | 依据 |
| --- | --- | --- |
| ① `TimeDilationManager.prune()` | **删除** | 全仓 0 调用者；清理职责与每 20 刻一次的 `refresh(Level)` 完全重叠（见 §1） |
| ① `TimeDilationManager.active(Level)` | **保留**（复核时发现它也是 0 调用者） | 它是公开**查询** API、不是本次被指出的那条债；删它会把 diff 扩到任务范围之外。理由见 §1.4 |
| ② `/cyberware stop` | **改成 a：减速 + 狂暴都清**，并向客户端广播狂暴已结束 | 见 §2 |
| ④ 客户端 HUD 即时消失 | **本次做不到**（服务端信号已发；客户端两个 handler 的既有语义会忽略它） | 事实与证据见 §4 |

---

## 1. ① `prune()` 复核与决定

### 1.1 复核命令与命中（原始结果）

```bash
grep -rn "prune" src/main/java --include=*.java
```

命中只有 4 行：

```
core/TimeDilationManager.java:298:    public static void prune() {          ← 定义本身
core/BerserkManager.java:189:    public static void prune() {               ← 定义本身
event/BerserkHandler.java:60:   // prune() 不再需要 level 参数…
event/BerserkHandler.java:61:   BerserkManager.prune();                     ← 唯一调用
```

再加一道精确检索（含类名前缀、静态导入、全仓非 java 文件）：

```bash
grep -rn "TimeDilationManager\.prune\|TimeDilationManager\.active\|import static.*TimeDilationManager" \
     src/main/java --include=*.java      # → 零命中
grep -rn "TimeDilationManager" . | grep -v "^./build/" | grep -v "^./src/main/java"   # → 只命中文档 .md
```

**事实**：
- `TimeDilationManager.prune()`：**0 个调用者**（只有它自己的定义行）。
- `BerserkManager.prune()`：**1 个调用者**（`event/BerserkHandler.java:61`）→ **保留，删除动作不涉及它**。
- 没有 `src/test` 源码集（`ls src` 只有 `main`），没有反射/字符串调用，`cyberware.mixins.json` 也不引用这两个名字。

### 1.2 删除决定与理由

**删除 `TimeDilationManager.prune()`**。理由：

1. 0 调用者，且**职责与 `refresh(Level)` 完全重叠** —— `refresh` 每 20 刻由
   `TimeDilationHandler.onEntityTick`（`event/TimeDilationHandler.java:112`）调用一次，它内部做的是同一套清理：
   「所属世界弱引用被回收 → 删除」「本世界已过期 → 删除」「别的世界 → 跳过」。
   一个只做清理、又没人调用的方法，就是这条债本身（inspector t20 的 F1 指出的也是它）。
2. 与 inspector 移交文档（`WORLDSTATE-HANDOFF.md` F1）里「建议留」的关系：那条建议给的理由是
   「与 Berserk 对称、将来手工排查有用」。本任务的目标是清掉这条债，而
   **对称性由 `BerserkManager.prune()` 保留即已成立**（它有真实调用者、必须留）；
   TimeDilation 侧的清理职责在 t17 之后已经确定由 `refresh(Level)` 承担（t17 选了方案 b：靠世界实例判定，
   不引入事件钩子，也就不会出现「某个钩子需要调用 prune()」的将来）。captain 若裁决保留，恢复即可（见 §1.5）。

### 1.3 删除后是否产生新的死代码：**没有**

逐一核对 `TimeDilationManager` 的私有 helper 引用数（源码内出现次数）：

| helper | 出现次数 | 删除 `prune()` 后是否仍被使用 |
| --- | --- | --- |
| `belongsTo` | 4 | 是（`liveIn` 与 `refresh`） |
| `ownerLevel` | 4 | 是（`refresh` 里的「世界已卸载 → 删除」分支） |
| `liveIn` | 6 | 是（`timeScaleFor` / `isOwnDilationActive` / `extend` / `active`） |
| `fadedRatio` | 2 | 是（`timeScaleFor`） |

字节码核对（编译产物）：`ownerLevel` 仍被调用（`javap -c` 中 2 处引用：定义 + `refresh` 内调用）；
`Iterator` / `UUID` 两个 import 仍被 `refresh` 使用，未变成未用 import。

### 1.4 复核时发现的第二处死代码：`active(Level)` —— **保留**（决定与理由）

`TimeDilationManager.active(Level)`（源码第 233 行，javadoc 写「UI/HUD 用」）同样 **0 调用者**：
HUD 走的是客户端自己的 `ClientTimeDilation`（`client/**`），服务端这个方法没有任何调用点。

**决定：本次保留，不删。** 理由：

1. 它不是被指出的那条债：t20 F1 只点名 `prune()`；`active(Level)` 是**公开查询 API**，
   删除它属于扩大改动面，而并行 A 路正在改 `client/**` 邻域，减小 diff 可以降低交叉风险。
2. 它不是「重复的清理例程」：`prune()` 的职责已经被 `refresh()` 完全接手（真重复），
   而 `active(Level)` 是一个没有替代者的查询入口（服务端侧要判断「这个维度里现在有没有减速源」时直接可用）。
3. 决定有人负责、留有记录：本条即为记录；captain 要求一并删的话，是一处 10 行以内的删除，一轮可交付。

### 1.5 回滚成本

删除动作发生在**未提交的工作树**里：`git diff src/main/java/com/dsh/cyberware/core/TimeDilationManager.java`
可以直接看到被删掉的那 8 行原文；恢复时不需要重写逻辑 —— `BerserkManager.prune()`（保留在树上）
的判定结构与它完全同款（`ownerLevel == null || owned.getGameTime() >= expireAt` → 删除）。

---

## 2. ② `/cyberware stop` 选哪种修法

### 2.1 现状事实（改前）

- `CyberwareCommands.java`（改前第 68-72 行）：`stop` 只做两件事 ——
  `TimeDilationManager.clear()` 与一行回执 `"[cyberware] 时间减缓已清除"`。
- 同文件另有两个子命令：`sandevistan`、`dilate <比例> <秒>`，两者都只影响**执行命令的玩家自己**。
- 整个 `/cyberware` 需要 `Permissions.COMMANDS_GAMEMASTER` 权限。
- `stop` 清的是 `TimeDilationManager.ACTIVE` 这张**全局**静态表 → 它本来就是全服语义。
- **狂暴没有被清**：`BerserkManager.ACTIVE` 原样保留 → 同一个「停」字只停了减速。

### 2.2 选择：**a（两样都清）**

理由：

1. **语义对齐**：命令叫 `stop`，回执也写着「已停止」；只停一半是把不一致留在默认路径上。
2. **权限与定位**：它需要 gamemaster 权限、清的是全局表 —— 定位就是「调试用的全清」。
   改成两样都清与它现有的全局语义一致。
3. **不选 b（拆子命令）**：拆完 `stop` 仍然只停减速，等于把不一致保留成默认行为，
   而「只停减速」这个精确需求在调试场景里可以用 `/cyberware stop` 之后重新 `dilate` 覆盖，
   为它新增语法层级没有收益。
4. **不选 c（保持不动）**：现有行为是历史遗留 —— t17 之前 `BerserkManager` 甚至没有能被调用的清理路径
   （`clear()` 0 调用者），不是刻意设计。

### 2.3 实现

```java
TimeDilationManager.clear();
BerserkManager.clear();
for (ServerPlayer online : ctx.getSource().getServer().getPlayerList().getPlayers()) {
    BerserkHandler.broadcast(online);          // isActive()==false → 发出 active=false 的 BerserkPayload
}
ctx.getSource().sendSuccess(() -> Component.literal("[cyberware] 已停止：时间减缓 + 狂暴"), false);
```

- **狂暴的客户端通知**：`BerserkHandler.broadcast(player)`（`event/BerserkHandler.java:42-49`）内部
  `active = BerserkManager.isActive(player)`，清表之后恒为 `false` → 发出的就是现有的「已结束」信号
  `BerserkPayload(0, 0, 1.0, false)`。**不需要新增协议**。
- **广播为什么放在命令里，而不是 `BerserkManager.clear()` 里**：`clear()`（`core/**`）是纯数据操作，
  它手上没有 server / 玩家引用；把网络与 `ServerLifecycleHooks` 引入 core 会把 core 绑到网络层。
  命令层天然拿得到 `getServer().getPlayerList()`。
- **为什么广播给所有在线玩家**：这个包对没在狂暴的客户端是幂等的空操作；
  同时覆盖「客户端本地状态比服务端表更旧」的情形（例如客户端收到过激活包、服务端记录已被回收）。
- **减速侧不发包**：`TimeDilationPayload` 只有 `duration/ratio/区域/施法者`，
  客户端 `applyOnClient` 对入站广播只会 `Math.max` 延长（或过期后重建），没有缩短路径；
  发一个时长 0 的包既停不掉旧源，还会给「没见过原始广播的客户端」凭空造一条 1 刻的源。理由详见 §4。
- **属性修饰符**（攻击速度 / 移动速度）：`BerserkHandler.onEntityTick`（`event/BerserkHandler.java:80-91`）
  每 tick 按 `BerserkManager.isActive(player)` 校核，清表后 `isActive` 为 false → 当场摘掉修饰符，
  最长 1 tick（1/20 秒）内完成，无需额外处理。

---

## 3. ③ 改动清单

| 文件 | 改动 |
| --- | --- |
| `core/TimeDilationManager.java` | 删除 `public static void prune()`（8 行）与其 javadoc；javadoc 里补一句说明该职责由 `refresh(Level)` 承担。**其余一字未动**（`ownerLevel` / `belongsTo` / `liveIn` / `active` / `refresh` / `clear` / `broadcast` 全保留） |
| `CyberwareCommands.java` | `stop` 分支：新增 `BerserkManager.clear()` + 遍历在线玩家 `BerserkHandler.broadcast(...)` + 回执改为「已停止：时间减缓 + 狂暴」；类 javadoc 的 `stop` 说明同步改写；新增 3 个 import（`core.BerserkManager`、`event.BerserkHandler`、`server.level.ServerPlayer`）。`sandevistan` / `dilate` 两个子命令未动 |

字节码核对：
- `TimeDilationManager` 编译产物中 `prune` 已消失，`active(Level)`、`refresh(Level)`、`clear()` 均在；`ownerLevel` 仍被引用。
- `BerserkManager` 编译产物中 `prune()`、`clear()` 都在。
- `CyberwareCommands` 的 `stop` lambda 字节码顺序：
  `TimeDilationManager.clear()` → `BerserkManager.clear()` → `MinecraftServer.getPlayerList()` → `PlayerList.getPlayers()` → 循环 `BerserkHandler.broadcast(ServerPlayer)`。

---

## 4. ④ 客户端同步：服务端已发信号；**客户端当前会忽略它**（本次范围外）

**服务端侧（本任务已完成）**：狂暴的「已结束」广播已发出（见 §2.3）。

**客户端侧当前行为（读源码得到的事实，行号对应当前工作树）**：

1. `client/BerserkClientState.java`（`onPayload`）：
   ```java
   if (!isActive) {
       if (now >= endTick) { active = false; endTick = 0L; }   // ← 只在"确实已过期"时收状态
       return;
   }
   ```
   狂暴还在原时限内时 `now < endTick` → 这条 `active=false` **不改变**客户端状态 →
   狂暴条与屏幕表现继续显示到原到期时刻。该防抖是刻意加的（注释写明：迟到的 inactive 广播曾把进度条直接抹掉）。

2. `client/ClientTimeDilation.java`（`applyOnClient`）：
   入站广播只有两条路径 —— `s.endTick = Math.max(s.endTick, now + duration)`（延长）或
   「过期后重建」；**没有缩短路径** → 减速侧连可用的「停止」信号都没有。

**结论（事实）**：`/cyberware stop` 之后，服务端状态与效果全部停止；
客户端 HUD（狂暴条、减速条）**在这次任务后仍会走到原到期时刻**。这不是本次引入的回归
（本条改动之前，减速侧同样如此），而是两条客户端 handler 的既有语义决定的。

**需要的后续（`client/**` 范围，t22 明确排除）**：最小改法是给 `BerserkPayload` 增加一个显式
「停止」字段（例如 `stopped`），客户端收到即无条件 `clear()`；减速侧复用该信号清掉对应 `owner` 的源
（或整表 `clear()`）。**不要**把 `active=false` 直接改成无条件清 —— 会退回
「迟到广播把进度条抹掉」的老问题（客户端注释里记录了这次踩坑）。

---

## 5. ⑤ 未真机验证

本环境跑不起 Minecraft（无客户端/服务端运行时），本次验证到 `compileJava` + 字节码级为止。
进游戏后需要确认的 4 项：

1. `/cyberware dilate 0.5 10` 后执行 `/cyberware stop`：怪物立刻恢复原速，没有残留减速
   （检验 `TimeDilationManager.clear()` 生效）。
2. 用狂暴（`berserk_*` 主动激活）后执行 `/cyberware stop`：无敌、伤害翻倍、击杀延长、击杀回血
   全部停止；攻击/移动速度修饰符在 1 tick 内摘掉（检验 `BerserkManager.clear()` 与属性校核循环）。
3. HUD：狂暴条与减速条在 stop 之后**仍会显示到原到期时刻** —— 这是 §4 描述的已知客户端缺口，
   不是本任务的回归；客户端补丁落地后这一条才会变成「立刻消失」。
4. 多人：一个 gamemaster 执行 `/cyberware stop`，全服玩家的减速与狂暴都停止（全局语义，预期如此）。

---

## 6. 验证命令（可复核）

```bash
# 1) 编译（只跑 compileJava；未跑完整 build、未 commit）
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-arm64 PATH=$JAVA_HOME/bin:/opt/gradle-8.10.2/bin:$PATH \
  gradle compileJava --no-daemon
# → BUILD SUCCESSFUL（> Task :compileJava 实际执行，0 error）

# 2) prune 复核（删除前）
grep -rn "prune" src/main/java --include=*.java
# → TimeDilationManager 只有定义行；BerserkHandler:61 调的是 BerserkManager.prune()

# 3) 删除后字节码核对
javap -p build/classes/java/main/com/dsh/cyberware/core/TimeDilationManager.class | grep prune   # → 无输出
javap -p build/classes/java/main/com/dsh/cyberware/core/BerserkManager.class    | grep prune   # → public static void prune();
javap -c -p build/classes/java/main/com/dsh/cyberware/CyberwareCommands.class | grep -A2 clear
```
