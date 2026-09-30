# RAM System 数据核心与协议契约（t23 交付物）

> 依据：`RAM-SYSTEM-SPEC.md` + `/sdcard/DSH/Neko专属邮箱📮/邮件IX.txt`
> 范围：`core/**`、`network/**`、`registry/**`（**未碰** `client/**`、`mixin/client/**`、`resources/**`）
> 验证：`gradle compileJava --no-daemon` → BUILD SUCCESSFUL + 产物级 `javap` 核对。**未跑完整 build、未 commit。**

---

## 0. 一句话

服务端侧的 RAM 地基（玩家挂件 + 义体数值聚合器 + 每 20 刻恢复推进）与三个协议包（含字段清单，
huddev 照此实现）都已落地；操作系统槽上限改为 2；超频状态机只有状态与同步、没有任何效果。
**破解效果与全部客户端渲染仍是 t25/t26 与 huddev 的活。**

---

## 1. 玩家 RAM 挂件（验收 ①）

**注册位置**：`registry/ModAttachments.java:47-56`（`RAM` 常量，注册名 `cyberware:ram`）。

```java
public static final DeferredHolder<AttachmentType<?>, AttachmentType<RamState>> RAM =
        ATTACHMENTS.register("ram",
                () -> AttachmentType.builder(() -> RamState.EMPTY)     // 默认值
                        .serialize(RamState.CODEC)                     // 存档持久化（MapCodec）
                        .copyOnDeath()                                 // 死亡拷贝
                        .sync(RamState.STREAM_CODEC)                   // 自动同步（跨维度跟随实体）
                        .build());
```

- **MapCodec 出处**：`core/RamState.java:32`
  `Codec.DOUBLE.fieldOf("current").xmap(RamState::new, RamState::current)`
  —— 与 `CyberwareInstallation.CODEC`（`CyberwareInstallation.java:35`，`Codec.unboundedMap(...).fieldOf("installed").xmap(...)`）同款写法。
- **StreamCodec 出处**：`core/RamState.java:36`
  `StreamCodec.composite(ByteBufCodecs.DOUBLE, RamState::current, RamState::new)`。
- **落盘字段只有 `current`**（契约 §1「max/regenPerMinute 为派生值，不落盘」）。
  `max` = `CyberwareStats.maxRam(player)`，`regenPerMinute` = `CyberwareStats.regenPerMinute(player)`，每次用时实时算
  —— 装卸义体后上限立刻变，不需要迁移旧存档数字。
- **「0」与「没初始化」必须分开**：`current = 0` 是合法的「RAM 耗尽」，所以初始化判据不是数值而是
  **附件是否存在**（`RamState.has(player)` → `AttachmentHolder.hasData`）。第一次结算时灌满，见 §4。
- 读接口：`RamState.of(Player)`（没有附件返回 `EMPTY`，**不创建附件**）、`RamState.set(Player,double)`。

**另有第二件挂件**（超频状态，见 §6）：`cyberware:overclock`（`registry/ModAttachments.java:64-70`），
`serialize(OverclockState.CODEC)` + `sync(OverclockState.STREAM_CODEC)`，**不** `copyOnDeath`。

---

## 2. 义体数值聚合器（验收 ②）

**新文件**：`core/CyberwareStats.java`（仓库此前**没有任何聚合器** —— 这条契约提醒是对的，
`CyberwareInstallation` 只负责容量与顺序）。

| API | 语义 |
| --- | --- |
| `installed(Player)` | 已安装义体列表（`Installed(def, data, variant)`）；跳过旧存档里被删的型号与没有该稀有度变体的型号 |
| `sum(Player, statKey)` | 某个数值键在所有已安装义体上**求和**（缺键按 0） |
| `maxRam(Player)` | `Σ Stats.RAM`，**求和后夹到 ≥ 0** |
| `regenPerMinute(Player)` | `Σ Stats.RAM_REGEN`（单位口径见下方「已知问题」） |
| `isCyberdeck(Variant)` | 网络接入仓判定：该档数值带 `Stats.BUFFER` |
| `bestCyberdeck(Player)` | 稀有度最高的网络接入仓（同稀有度取 RAM 高者）；没装返回 `null` |
| `overclockDurationTicks(Player)` / `overclockCooldownTicks(Player)` | 超频时长/冷却（tick），由网络接入仓稀有度决定（占位表） |
| `maxOverclockTicks()` | 占位表里最大的时长/冷却（给超频状态做「时间基准变化」自愈用） |

**取值口径**：每件义体按**它自己那一档稀有度**取 `variant` 再取值 —— 同一个型号的普通档与神话档算出来不同，
这正是需求书「每个稀有度独立容量与数值」的延续。

### 2.1 求值依据（真实定义 id，验收要求的 ≥2 条）

| 定义 id | 定义表位置 | 数值 | 聚合结果 |
| --- | --- | --- | --- |
| `cyberdeck_technica_4`（泰克重工技术4型） | `CyberwareDefinitions.java:122-126`，LEGENDARY | `RAM 12, BUFFER 8, SLOTS 6, HACK_COOLDOWN 45, COMBAT_HACK_DURATION 50, UPLOAD_TIME -25` | `maxRam` += **12**，且被 `isCyberdeck` 判为网络接入仓 |
| `ram_upgrade`（RAM升级） | `CyberwareDefinitions.java:1072-1075`，LEGENDARY | `RAM 2, RAM_REGEN 0.2` | `maxRam` += **2**，`regenPerMinute` += **0.2** |
| `cyberdeck_tetratronic_1`（四相传电1型） | `CyberwareDefinitions.java:91`，LEGENDARY | `RAM 5, RAM_REGEN 6` | `regenPerMinute` += **6**（对得上邮件「+6/分钟」） |
| `iconic_bio_conductors`（COX-2赛博生体优化） | `CyberwareDefinitions.java:349-353`，MYTHIC | `RAM -4` | `maxRam` **-4**（因此聚合后夹 ≥0） |
| `smart_link`（智能连接） | `CyberwareDefinitions.java:624-629`，LEGENDARY | `RAM 2, CRIT_DAMAGE 15` | `maxRam` += 2 —— 说明 RAM 不只来自网络接入仓 |

举例：一个玩家装着 `cyberdeck_technica_4` + `ram_upgrade` + `smart_link` →
`maxRam = 12 + 2 + 2 = 16`，`regenPerMinute = 0.2`。

### 2.2 「网络接入仓」判定为什么用 BUFFER（不是 id 前缀）

官方 123 条里网络接入仓命名不统一：`cyberdeck_*`、`haunted_cyberdeck`（军用科技篇章6型）、
`netwatch_netdriver_mk`、`militech_paraline_mkv`、`tetratronic_rippler_mkv`、`biotech_sigma_mkiv`、
`raven_microcyber_mkiii`、`arasaka_shadow_mkv` —— 前缀清单会漏。

证据：`grep -n "Stats.BUFFER" CyberwareDefinitions.java` 的命中行号是
`41 49 58 66 75 83 91 99 108 117 126 135 143 151 159 168 176 184 208 215 487 494 502 511 519`
—— **全部落在网络接入仓定义上**，斯安威斯坦/狂暴/额皮质等没有一个带 BUFFER。
⚠️ 若将来有人给非网络接入仓加 BUFFER，改 `CyberwareStats.isCyberdeck` 一处即可。

---

## 3. 三个协议包（验收 ③）—— huddev 照此实现

**注册位置**：`network/CyberwareNetwork.java:44-59`；**`PROTOCOL_VERSION` `"3"` → `"4"`**（`CyberwareNetwork.java:26`，
产物核对：`javap -constants` 显示 `PROTOCOL_VERSION = "4"`）。

| 包 | 注册方式 | 方向 |
| --- | --- | --- |
| `RamPayload`（`cyberware:ram`） | `playToClient(TYPE, CODEC)` | 仅 S2C |
| `OverclockPayload`（`cyberware:overclock`） | `playBidirectional(TYPE, CODEC, handler)` | C2S `TOGGLE` / S2C `STATE` |
| `HackPayload`（`cyberware:hack`） | `playBidirectional(TYPE, CODEC, handler)` | C2S `CAST` / S2C 其余 action |

### 3.1 `RamPayload`（S2C，HUD 的展示快照）

| # | 字段 | 类型 | 说明 |
| --- | --- | --- | --- |
| 1 | `current` | double | 当前 RAM（保留小数，客户端插值） |
| 2 | `max` | double | 上限（= Σ `Stats.RAM`，夹 ≥0） |
| 3 | `regenPerMinute` | double | 每分钟恢复量（= Σ `Stats.RAM_REGEN`） |
| 4 | `overclockActive` | boolean | 超频是否进行中 |
| 5 | `depleted` | boolean | `current <= 0` |

发送时机（`core/RamSystem`）：数值真的变化时（首次灌满、每秒恢复、装卸义体导致上限变化被夹住），
以及超频进行中每 20 刻补一次。**最多 1~2 个包/秒/玩家**。

⚠️ **`max <= 0` 的玩家**（没装任何给 RAM 的义体）也满足 `depleted = true`。
HUD 必须先判 `max <= 0` = 「没有 RAM 能力」（不显示 RAM 条或只显灰），再判状态（见 §5 判定表）。

### 3.2 `OverclockPayload`

| # | 字段 | 类型 | 说明 |
| --- | --- | --- | --- |
| 1 | `action` | enum `TOGGLE` / `STATE`（线格式 VAR_INT ordinal） | `TOGGLE` = C2S 请求切换；`STATE` = S2C 状态快照 |
| 2 | `active` | boolean | 是否超频中 |
| 3 | `remainingTicks` | int | 剩余超频刻数（0 = 不在超频） |
| 4 | `totalTicks` | int | 本次超频总时长（按当前网络接入仓稀有度算；进度条分母） |
| 5 | `cooldownRemainingTicks` | int | 剩余冷却刻数（0 = 不在冷却） |
| 6 | `cooldownTotalTicks` | int | 冷却总时长（冷却环分母） |

上行便利构造：`OverclockPayload.toggleRequest()`（其余字段占位，**服务端不看客户端的字段**）。

### 3.3 `HackPayload`

| # | 字段 | 类型 | 说明 |
| --- | --- | --- | --- |
| 1 | `action` | enum（VAR_INT ordinal） | `CAST`(C2S) / `LOCKED` / `UPLOAD_START` / `UPLOAD_PROGRESS` / `APPLIED` / `CANCELLED` / `REJECTED` |
| 2 | `targetEntityId` | int | 目标实体 id（`Entity#getId()`；无目标 0） |
| 3 | `hackId` | String | `overheat` / `short_circuit` / `synapse_burnout` / `weapon_glitch` / `system_reset`；无则空串 |
| 4 | `uploadRemainingTicks` | int | 上传剩余刻（`UPLOAD_PROGRESS` 用；其余 0） |
| 5 | `uploadTotalTicks` | int | 上传总刻（进度条分母；其余 0） |
| 6 | `ramCost` | int | 本次实际扣除的 RAM（0 = 没扣或扣的是血，见 `note`） |
| 7 | `note` | String | 短说明/拒绝原因（如 `RAM ACCESS FAILED`；空串合法，**不许塞长文本**） |

上行便利构造：`HackPayload.cast(targetEntityId, hackId)`。

### 3.4 客户端 handler 必须由 huddev 注册（⚠️ 交接点）

`RamPayload` 用的是 `playToClient(TYPE, CODEC)`（**无 handler 重载**），两个双向包只注册了**服务端** handler。
NeoForge 要求在客户端用 `RegisterClientPayloadHandlersEvent` 注册对应 handler ——
**在这件事做完之前，客户端收到这三种包会报「没有 handler」**。这是契约交接点，不是可以跳过的选项。
（`client/**` 不在 t23 范围，我没有代 huddev 注册，也不该替他决定 HUD 的处理方式。）

---

## 4. RAM 恢复：服务端推进（验收 ④）

**实现**：`core/RamSystem.java`（服务端每刻入口 `onPlayerTick(PlayerTickEvent.Post)`）。

1. **初始化**：第一次见到该玩家（`RamState.has` 为假）→ 灌满到 `maxRam`；
2. **夹上限**：`current > max`（卸了加 RAM 的义体）→ 立刻夹到 `max`，不凭空多出来；
3. **恢复**：每 **20 刻（= 1 秒）**结算一次，`current += regenPerMinute / 60`，夹在 `[0, max]`；
4. **同步**：只有数值真的变了才发包（避免每刻写附件 = 每刻一个同步包）。

**为什么是「每 20 刻结算」而不是每刻累加**：RAM 存在玩家附件里，而 `setData` 每次都会触发一次
附件同步包 —— 每刻写一次就是每秒 20 个包。任务书允许「每刻 / 每 20 刻」二选一，这里选 20 刻，
HUD 的平滑由客户端插值（契约 §3.1 的字段就是给插值用的）。

**客户端不参与扣费判定**：
- 唯一的写入口是服务端（`RamState.set` 只在 `RamSystem` / 将来的破解逻辑里调用）；
- 客户端的 RAM 只是 `AttachmentType.sync` 同步下来的镜像（`StreamCodec` 单向下发）；
- 上行的三个包都只表达「请求」（切换超频 / 释放破解），服务端一律自己校验。

**注册方式（为什么不用注解）**：`registry/ModAttachments.java:73-78` 的静态块里显式
`NeoForge.EVENT_BUS.addListener(RamSystem::onPlayerTick)`。用 `@EventBusSubscriber` 注解也可以，
但**注解漏扫会静默失效** —— 这个项目已经吃过「静默 = 等主人真机才发现」的亏，所以这里选显式一行：
产物核对 `javap -c ModAttachments` 能看到 `NeoForge.EVENT_BUS.addListener(...)`。

---

## 5. 状态判定表（huddev 直接用）

| 客户端状态 | 判据（全部来自服务端字段） | 视觉（邮件 §二/§三） |
| --- | --- | --- |
| 没有 RAM 能力 | `max <= 0` | 不画 RAM 条（或画灰） |
| 正常 | `max > 0 && current > 0` | 青蓝细线 + `current / max` 数字 |
| 濒死超频 | `current <= 0 && overclockActive` | RAM 条血红流动；释放破解改扣 2 点生命（**扣血逻辑是 t25**） |
| 彻底瘫痪 | `current <= 0 && !overclockActive` | 屏幕中央闪 `RAM ACCESS FAILED`（**判定与回包是 t25**） |
| 冷却中 | `cooldownRemainingTicks > 0` | 超频冷却环 |

---

## 6. 超频状态机（验收 ⑥）

**状态**：`core/OverclockState.java` —— `record OverclockState(boolean active, long expiresAt, long cooldownUntil)`，
挂在 `cyberware:overclock`（按玩家存，**没有静态表**，所以天生没有 0.3.12 那个 P0 的跨世界残留问题）。

**时长与冷却的来源**：`CyberwareStats.overclockDurationTicks` / `overclockCooldownTicks`
= 玩家**最好的网络接入仓**（`bestCyberdeck`，稀有度最高）的稀有度 → 查占位表（`core/CyberwareStats.java:32-33`）：

| 稀有度 | COMMON | UNCOMMON | RARE | EPIC | LEGENDARY | MYTHIC |
| --- | --- | --- | --- | --- | --- | --- |
| 超频时长（秒） | 10 | 12 | 14 | 16 | 18 | 20 |
| 冷却（秒） | 60 | 55 | 50 | 45 | 40 | 35 |

TODO(主人填写)：邮件只说「由网络接入仓的品质决定」，**没给数字** —— 上表是占位值，
主人调完改这一张表即可（改完手感立刻变，不用动别处）。

**时刻用哪个时钟**：`MinecraftServer.getTickCount()`（服务端全局单调 tick 计数），
**不是** `Level.getGameTime()` —— 后者每个世界各自从 0 算，跨维度/跨世界会串味（0.3.12 P0 的根因）。

**服务器重启后的自愈**：tick 计数随服务器实例归零，旧存档里可能留着「未来几小时」的时刻。
`OverclockSystem.read(...)` 用两条规则作废这种记录（并写回修复）：
① `active` 且剩余时长 > 占位表最大时长 → 视为已结束（进冷却）；
② `cooldownUntil` 比「现在 + 最大冷却」更远 → 视为冷却已结束。
判据只依赖占位表上限（35~60 秒级），不会误伤正常状态。

**转换规则**（`OverclockSystem.toggle` / `tick`，全部服务端裁决，客户端字段不作数）：

| 当前 | 请求/时点 | 结果 |
| --- | --- | --- |
| 不在超频、不在冷却、装了网络接入仓 | `TOGGLE` | 开启，时长按稀有度表 |
| 不在超频、不在冷却、**没装**网络接入仓 | `TOGGLE` | **拒绝**（debug 日志，不改状态） |
| 冷却中 | `TOGGLE` | **拒绝** |
| 超频中 | `TOGGLE` | 立刻结束 → 进入冷却（时长为该件网络接入仓的冷却） |
| 超频中 | 每刻 tick 到点 | 自动结束 → 进入冷却 + 一次 `OverclockPayload.STATE` 同步 |

**本任务不做效果**：扫描线/线框/音调/撕裂伪影全是客户端渲染（huddev），
破解的扣 RAM/扣血、AI 禁用、上传计时是 t25/t26。

---

## 7. 操作系统槽上限 1 → 2（验收 ⑤）

`core/CyberwareSlot.java:11-12`：

```java
/** 操作系统槽上限 2（主人 0.4.0 拍板）：网络接入仓与斯安威斯坦/狂暴同属这一槽，只装 1 件就挤掉了。 */
OPERATING_SYSTEM("操作系统", 1, 2),
```

**只有这一处**改动：`git diff --stat core/CyberwareSlot.java` = `1 file changed, 2 insertions(+), 1 deletion(-)`
（新增 1 行注释 + 改 1 行）；产物核对 `javap -c CyberwareSlot` 的静态初始化里
`OPERATING_SYSTEM` 的构造参数是 `(order=1, maxCount=2)`。其余 9 个槽位一个没动。
装机校验读的正是 `CyberwareSlot#maxCount()`（`menu/CyberwareStationService.validateInstall`），
所以这条改动会**直接生效**于操作台的安装校验。

---

## 8. 已知问题 / 未做的事（明确列出）

1. **客户端 handler 未注册**（见 §3.4）：huddev 必须在 `client/**` 里用
   `RegisterClientPayloadHandlersEvent` 注册 `RamPayload` / `OverclockPayload` / `HackPayload` 的客户端处理。
   在注册之前，客户端收到这三种包会报未注册 handler。
2. **`Stats.RAM_REGEN` 的单位在数据里不一致**（影响 HUD 数字，请主人/numbersmith 裁决）：
   - 键的声明口径是**每分钟**（`CyberwareDefinition.java:102` 的注释），
     邮件例子「四相传电1型 +6/分钟」与 `cyberdeck_tetratronic_1` 的 `RAM_REGEN = 6` 吻合；
   - 但 `ram_upgrade` 的**描述文本**写「RAM恢复速率每秒增加0.05~0.2个单位」，而它的 `RAM_REGEN = 0.2`
     —— 按每分钟读是 +0.2/分，按每秒读是 +12/分，差 60 倍。
   本聚合器**只做求和、不做单位换算**（换算会静默改掉数值）。裁决后要么改描述，要么改数值。
3. **超频时长/冷却是占位表**（§6），等主人给数字。
4. **破解效果一行没做**（t25/t26）：`HackPayload.CAST` 的服务端处理只记一条 debug 日志，
   **不产生效果、不回包**。
5. **没有 `spend/canPay`（扣 RAM）API**：t23 没做扣费入口 —— 扣费属于破解逻辑（t25），
   提前加一个没人调用的 API 只会变成死代码。t25 在实现破解时加 `RamSystem.spend(...)`。
6. **未真机验证**（见 §9）。
7. 未跑完整 `gradle build`（inspector 统一全量构建）、未 commit（收尾任务统一提交）。

---

## 9. 未真机验证

本环境跑不起 Minecraft（无客户端/服务端运行时），验证到 `compileJava` + 产物级 `javap` 为止。
进游戏后需要确认（**这些代码里验不出来**）：

1. 装上 `cyberdeck_technica_4` + `ram_upgrade` 后，客户端收到的 `RamPayload.max` 是不是 14、`regenPerMinute` 是不是 0.2；
2. RAM 从 0 恢复到上限的速率（每 20 刻 +regen/60）与 HUD 观感；
3. 装上网络接入仓后按超频键：时长/冷却是否按稀有度表生效；没装时是否被拒；
   冷却中是否被拒；超频中再按是否立刻结束并进冷却；
4. 服务器**重启**后旧存档里的超频状态是否被自愈规则清掉（不再出现「还差几小时」）；
5. 换维度（主世界↔下界）后 RAM 与超频状态是否正常（这条是 0.3.12 P0 的复检项）；
6. huddev 注册客户端 handler 之后，三种包是否都能正确到达与渲染。

---

## 10. 验证命令（可复核）

```bash
# 1) 编译（只跑 compileJava）
cd /root/mod26/cyberware && JAVA_HOME=/usr/lib/jvm/java-21-openjdk-arm64 \
  /opt/gradle-8.10.2/bin/gradle compileJava --no-daemon      # → BUILD SUCCESSFUL

# 2) 产物级核对（新类真的编译出来了）
ls build/classes/java/main/com/dsh/cyberware/core/{CyberwareStats,RamState,OverclockState,RamSystem,OverclockSystem}.class
ls build/classes/java/main/com/dsh/cyberware/network/{RamPayload,OverclockPayload,HackPayload}.class

# 3) 注册与常量
javap -constants -p build/classes/java/main/com/dsh/cyberware/network/CyberwareNetwork.class | grep PROTOCOL
#   → public static final java.lang.String PROTOCOL_VERSION = "4";
javap -p build/classes/java/main/com/dsh/cyberware/registry/ModAttachments.class | grep -E "RAM|OVERCLOCK"
javap -c -p build/classes/java/main/com/dsh/cyberware/registry/ModAttachments.class | grep addListener
#   → NeoForge.EVENT_BUS.addListener(...)  （显式注册，不依赖注解扫描）

# 4) 槽位上限
javap -c -p build/classes/java/main/com/dsh/cyberware/core/CyberwareSlot.class | grep -A8 "static {}"
#   → OPERATING_SYSTEM 的构造参数 iconst_1(order) iconst_2(maxCount)
git diff --stat src/main/java/com/dsh/cyberware/core/CyberwareSlot.java   # 1 file changed, 2 insertions(+), 1 deletion(-)

# 5) 范围核对（client / mixin-client / assets 未动）
git status --porcelain -- src/main/java/com/dsh/cyberware/client src/main/java/com/dsh/cyberware/mixin/client src/main/resources/assets
```
