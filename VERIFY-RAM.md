# VERIFY-RAM.md —— 0.4.0 RAM System 全链路独立验收（t27 / inspector）

工作区：`/root/mod26/cyberware`　本任务 attempt_id：`8c4fb414-e988-4f70-9fb7-c877c3fd078b`
性质：**只读验收**。本任务**未修改任何源码或资源**，只新增本报告。
被验对象：`a162fc0`（0.4.0-Beta：RAM System 数据核心与服务端玩法 + 现代化 RAM HUD 与脑机超频视觉，27 files, +3123/−9）。

> ## ⚠ **我没有启动过游戏。**
> 本报告全部结论来自：源码/行号、`javap` 字节码、`git` 历史、静态 grep。
> 帧率收益、HUD 观感、破解手感、超频视觉 —— 这些**只有主人在真机上判断**（§6 给清单）。
> 本轮也**没有跑第二次 build**（见 §1 口径说明）。

---

## 0. 验收结论

| # | 验收项 | 结果 |
|---|---|---|
| 1 | `gradle build --no-daemon` 全量构建 exit 0 | ✅ **exit 0**，`BUILD SUCCESSFUL in 1m 37s`；产物 **720854 字节 / md5 `02e3dce0d71e1adb4def7804003b9a31`** |
| 2 | 产物级核验：本轮新增类在 jar 内 | ✅ **15/15 全在**（core 8 + client 3 + event 1 + network 3）；jar 内 577 条目 / 98 class |
| 3 | Mixin 合规：新增注入器显式 `require = 0`；`@Inject` 目标经 javap 核实 | ✅ 本轮**没有新增 mixin**（json 与 mixin/** 零改动）；既有 **13/13 `require=0`、无冒号**；8 个目标方法逐个 javap 命中 |
| 4 | 协议契约：`PROTOCOL_VERSION == "4"`、三包已注册、客户端 handler 已注册、客户端无 RAM 扣减 | ✅ 全部通过（`ConstantValue: String 4`；`playToClient` + `playBidirectional`×2；客户端 3 个 handler 走 `RegisterClientPayloadHandlersEvent`；客户端扣减/裁决 grep **0 命中**） |
| 5 | 主人拍板项：OS 槽位 `maxCount == 2`；键位 V/R/G/X 唯一 | ✅ `OPERATING_SYSTEM("操作系统", 1, 2)`（字节码 `iconst_2`）；**V/R/G 三个键码互不相同，X 键尚未注册**（扫描功能延到下一轮，故无冲突） |
| 6 | 0.3.12 P0 教训：客户端静态缓存均有 `clear()` 且绑 Level/player 身份 | ✅ `RamClientState.clear()` 存在**且已在换世界分支调用**；`OverclockWireframe` / `RamHud` 经逐字段检查**无世界绑定的绝对刻状态**（理由见 §5） |
| 7 | 交付 VERIFY-RAM.md（逐项证据 + 非阻断发现 + 真机清单 + 「没有启动过游戏」声明） | ✅ 本文件 |

**总裁决：PASS**（0 个阻断项；§7 列 5 条非阻断发现/待办）。

---

## 1. 全量构建（口径说明 + 结果）

**口径说明（如实记录）**：任务书原文要求我跑 `gradle build --no-daemon`；我在收到
captain 的「不要跑 gradle build（13:38 起他在同一台机上构建并部署）」之前**已经跑过一次**，
该次构建于工作树提交前的同一个源集上完成。收到改口径后**我没有再构建**，改为对**已部署的 jar** 做只读核验。

```
$ gradle build --no-daemon          （JAVA_HOME=java-21-openjdk-arm64, gradle 8.10.2）
> Task :createMinecraftArtifacts UP-TO-DATE
> Task :compileJava UP-TO-DATE
> Task :processResources / :classes / :jarJar NO-SOURCE / :jar / :assemble
> Task :compileTestJava NO-SOURCE / :test NO-SOURCE / :check UP-TO-DATE / :build
BUILD SUCCESSFUL in 1m 37s
4 actionable tasks: 2 executed, 2 up-to-date
EXIT=0
```

产物与部署一致性（关键）：

```
build/libs/cyberware-0.4.0-Beta.jar                              720854 字节
02e3dce0d71e1adb4def7804003b9a31  build/libs/cyberware-0.4.0-Beta.jar
02e3dce0d71e1adb4def7804003b9a31  /sdcard/FCL/.minecraft/versions/26.1.2-NeoForge/mods/cyberware-0.4.0-Beta.jar
```

→ **我核验的产物与 captain 部署的产物 md5 完全相同**（`02e3dce0…`），所以我下面所有
`javap -cp …` 的产物级证据对**真机将要加载的那个 jar** 成立。日志：`/tmp/t27-build.log`。

> ⚠ 验收文本里写的产物名是 `cyberware-0.3.12-Beta.jar`，但本轮的 `build.gradle` 已把
> `version` 升到 **`0.4.0-Beta`**（见 §7 F1），实际产物名是 `cyberware-0.4.0-Beta.jar`。

---

## 2. 产物级核验：本轮新增类确实在 jar 内

核验方式：`python3 zipfile`（本机**没有 unzip**，任务书给的 `unzip -l` 无法执行，已用等价手段替代）。

```
要求的 15 个新类：缺失 = 无（15/15 在包内）
总条目 577 class 98
```

逐个列出（`OK` = 该 `.class` 在 jar 内）：

| 类别 | 类 | 在包内 |
|---|---|---|
| core | `RamState` / `RamSystem` / `CyberwareStats` / `OverclockState` / `OverclockSystem` / `HackSystem` / `HackLibrary` / `CombatEffects` | OK ×8 |
| client | `RamHud` / `RamClientState` / `OverclockWireframe` | OK ×3 |
| event | `CombatEffectsHandler` | OK ×1 |
| network | `RamPayload` / `OverclockPayload` / `HackPayload` | OK ×3 |

附带可见的内部类（说明枚举/记录已编进包）：`HackLibrary$1..$5`、`HackSystem$Payment`、
`HackSystem$Upload`、`HackPayload$Action`、`OverclockPayload$Action`。

---

## 3. Mixin 合规

**本轮没有新增 mixin**（这是首要事实）：

```
$ git status --porcelain -- src/main/java/com/dsh/cyberware/mixin src/main/resources/cyberware.mixins.json
（空）
$ git show --stat a162fc0 | grep -i mixin
（无输出）
```

`cyberware.mixins.json` 仍是 11 个 client 条目（`…EquipmentLayerRendererMixin, client.CapeLayerMixin,
client.EntityRendererOutlineMixin`），`injectors.defaultRequire = 1` 未变。

对**已部署 jar** 复跑注入器合规（产物级，不只看源码）：

```
mixin 类 12   require=0 合计 13   非0值 无
method=[...] 去重 8 —— 全部 ok（方法名与 '(' 之间无冒号）
```

`@Inject` / `@ModifyVariable` 的**目标方法逐个 javap 核实**（`javap -p -s -cp
minecraft-patched-26.1.2.109-merged.jar`，命令与输出节选）：

| mixin | 目标 | javap 实测 | 一致 |
|---|---|---|---|
| GuiMixin | `extractPlayerHealth` | `private void extractPlayerHealth(GuiGraphicsExtractor); desc: (Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V` | ✅ |
| SoundEngineMixin | `calculatePitch` | `private float calculatePitch(SoundInstance); desc: (Lnet/minecraft/client/resources/sounds/SoundInstance;)F` | ✅ |
| ParticleMixin | `tick` | `public void tick(); desc: ()V` | ✅ |
| ProjectileMixin | `tick` | `public void tick(); desc: ()V` | ✅ |
| GameRendererMixin | `render` + `@At target LevelRenderer.doEntityOutline()V` | `public void render(DeltaTracker, boolean); desc: (Lnet/minecraft/client/DeltaTracker;Z)V` / `public void doEntityOutline(); desc: ()V` | ✅ |
| WeatherEffectRendererMixin | `extractRenderState` ×2（`index=2/3`） | `public void extractRenderState(Level,int,float,Vec3,WeatherRenderState); desc: (Lnet/minecraft/world/level/Level;IFLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/state/level/WeatherRenderState;)V` | ✅ |
| EntityRendererOutlineMixin | `extractRenderState(...)V` 全描述符 | `public void extractRenderState(T,S,float); desc: (Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V` | ✅ 逐字符 |

（`@Redirect`/`@ModifyArg` 的 4 个 Equipment 目标与 CapeLayer 目标在 t12/t16 已同为 `require=0`，
本轮 json 未动，结论沿用。）

---

## 4. 协议契约

### 4.1 `PROTOCOL_VERSION == "4"`（产物级）

```
$ javap -v -p -cp build/libs/cyberware-0.4.0-Beta.jar com.dsh.cyberware.network.CyberwareNetwork
  #233 = Utf8  PROTOCOL_VERSION
  …
  public static final java.lang.String PROTOCOL_VERSION;
    ConstantValue: String 4
```

源码佐证：`network/CyberwareNetwork.java:28` `public static final String PROTOCOL_VERSION = "4";`

### 4.2 三个新包都已注册（源码 + 字节码）

`CyberwareNetwork.register(...)`（源码）：

| 包 | 注册方式 | 说明 |
|---|---|---|
| `RamPayload` | `registrar.playToClient(TYPE, CODEC)` | 单向 S2C，**无 handler** → 客户端必须另注册 |
| `OverclockPayload` | `registrar.playBidirectional(TYPE, CODEC, handleOverclockToggle)` | 双向，服务端 handler 已有 |
| `HackPayload` | `registrar.playBidirectional(TYPE, CODEC, handleHackCast)` | 双向，服务端 handler 已有 |

字节码侧同样见到 `playToServer`×2（action/activate）+ `playToClient`×1 + `playBidirectional`×2。

**关键交叉核实（防双注册/漏注册）**：读 NeoForge 26.1.2.109 源码
`PayloadRegistrar.java:75-82`：

```java
/**
 * Registers a bidirectional payload for the play phase.
 * <p>
 * The provided handler is registered for serverbound payloads and the client-side handler
 * must be registered via {@code RegisterClientPayloadHandlersEvent}
 */
public <T extends CustomPacketPayload> PayloadRegistrar playBidirectional(Type<T> type, StreamCodec<…> codec, IPayloadHandler<T> serverHandler) {
    return this.playBidirectional(type, codec, serverHandler, null);
}
```

即：**三参 `playBidirectional` 只注册服务端 handler，客户端 handler 必须另行注册** ——
所以 t25 在客户端补 handler 是**必需且正确**的，不存在「重复注册」冲突。

### 4.3 客户端 handler 已注册

`client/CyberwareClient.java:81-89`（并且在 `modEventBus` 上显式挂载）：

```java
private static void onRegisterClientPayloads(RegisterClientPayloadHandlersEvent event) {
    event.register(RamPayload.TYPE,       (payload, ctx) -> ctx.enqueueWork(() -> RamClientState.onRamPayload(payload)));
    event.register(OverclockPayload.TYPE, (payload, ctx) -> ctx.enqueueWork(() -> RamClientState.onOverclockPayload(payload)));
    event.register(HackPayload.TYPE,      (payload, ctx) -> ctx.enqueueWork(() -> RamClientState.onHackPayload(payload)));
}
```

挂载点：`modEventBus.addListener(CyberwareClient::onRegisterClientPayloads);`（源码已核）。
三个 handler 都只调用 `RamClientState` 的**显示态更新**方法。

### 4.4 客户端不存在任何 RAM 扣减 / 效果判定（grep 证据）

```
$ grep -rnE "spend|canPay|deduct|pay\(|setData|RamSystem|OverclockSystem|HackSystem|attachment" src/main/java/com/dsh/cyberware/client/
（0 命中）
```

反向对照（权威全在服务端）：

```
core/RamSystem.java:44  public static void onPlayerTick(PlayerTickEvent.Post event) {
core/RamSystem.java:45      if (!(event.getEntity() instanceof ServerPlayer player)) { … }
core/RamSystem.java:57  public static boolean canPay(ServerPlayer player, double cost)
core/RamSystem.java:68  public static boolean spend(ServerPlayer player, double cost)
registry/ModAttachments.java:73  NeoForge.EVENT_BUS.addListener(RamSystem::onPlayerTick);   ← 显式注册，不依赖注解扫描
```

客户端侧 `RamClientState` 的类注释也明确写着「客户端没有任何权威 …… 扣费、冷却、能否超频一律由服务端裁决」。

---

## 5. 主人拍板项 + P0 教训

### 5.1 操作系统槽位 `maxCount == 2`

源码 `core/CyberwareSlot.java:12`：

```java
/** 操作系统槽上限 2（主人 0.4.0 拍板）：网络接入仓与斯安威斯坦/狂暴同属这一槽，只装 1 件就挤掉了。 */
OPERATING_SYSTEM("操作系统", 1, 2),
```

构造器签名 `CyberwareSlot(String displayName, int order, int maxCount)` → 第 3 个参数即 `maxCount = 2`。

**产物级独立确认**（`javap -c` 的 `static {}`）：

```
 4: ldc      #68   // String OPERATING_SYSTEM
 7: ldc      #69   // String 操作系统
 9: iconst_1              ← order = 1
10: iconst_2              ← maxCount = 2   ✅
11: invokespecial … "<init>":(Ljava/lang/String;ILjava/lang/String;II)V
```

对照：`FRONTAL_CORTEX`/`FACE` 仍是 `iconst_…, iconst_1`（maxCount=1），说明**只改了 OS 这一处**。

### 5.2 键位 V/R/G/X 唯一性

`client/CyberwareKeys.java` 里**只有 3 个 KeyMapping**，全部挂在同一 `CATEGORY`：

| 常量 | key 字符串 | 默认键 | 已注册？ |
|---|---|---|---|
| `ACTIVATE` | `key.cyberware.activate` | `InputConstants.KEY_V` | ✅ `event.register(ACTIVATE)` |
| `RADIAL` | `key.cyberware.radial` | `InputConstants.KEY_R` | ✅ `event.register(RADIAL)` |
| `OVERCLOCK` | `key.cyberware.overclock` | `InputConstants.KEY_G` | ✅ `event.register(OVERCLOCK)` |
| **X（扫描）** | —— | —— | ❌ **未实现**：扫描/锁定框功能本轮取消、延到下一轮（t26 取消），`X` 键位尚不存在 |

**结论**：三个已存在的键位各自用**不同的 `InputConstants.KEY_*` 常量**（V/R/G），键码互不相同、
无冲突；`X` 尚未注册，因此也谈不上冲突。**没有发现任何键位重复。**

### 5.3 0.3.12 P0 教训（换世界状态残留）本轮复核

| 客户端静态状态 | 有 `clear()`/`reset`？ | 换世界时被调用？ | 判定 |
|---|---|---|---|
| `RamClientState`（本轮新增，19 个静态字段全是绝对毫秒/刻） | ✅ `clear()` @ `:141`，逐字段重置 19 项 | ✅ 在 `CyberwareClient` 的 `minecraft.level != lastLevel` 分支里调用 | ✅ 合规 |
| `ClientTimeDilation` / `BerserkClientState` / `SandevistanPostProcessor` / `ParticleTickClock` / `WeatherTickClock` / `AfterimageHistory` | ✅ | ✅ 同一分支 | ✅ 沿用 |
| `OverclockWireframe` | ❌ 无 `clear()` | — | ⚠ **判定为不需要，理由见下** |
| `RamHud` | ❌ 无 `clear()` | — | ⚠ **判定为不需要，理由见下** |

**为什么 `OverclockWireframe` / `RamHud` 不需要 clear()（我逐字段看了）**：

- `OverclockWireframe` 的非 final 静态只有两个：`frameStamp`（`System.currentTimeMillis()`）与
  `drawnThisFrame`（单帧提交计数）。用法是 `if (now != frameStamp) { frameStamp = now; drawnThisFrame = 0; }`
  —— **墙钟毫秒的帧内计数**，不绑定任何世界的 `gameTime`，换世界最坏只是重开一次帧计数（无残留、无跳变）。
  其余全是 `static final` 常量与一个日志去重 `AtomicBoolean`。
- `RamHud` 的静态全是 `static final` 常量 + 一个 `AtomicBoolean FAIL_LOGGED`；**没有跨帧缓存**，
  它每帧从 `RamClientState` 读（而后者已被 clear）。

换世界分支实测调用清单（`CyberwareClient`）：

```java
if (minecraft.level != lastLevel) {
    lastLevel = minecraft.level;
    ClientTimeDilation.clear(); BerserkClientState.clear(); SandevistanPostProcessor.resetWorldState();
    ParticleTickClock.clear(); WeatherTickClock.reset(); AfterimageHistory.clear();
    RamClientState.clear();        // ← 本轮新增的这份也接上了
}
```

→ **P0 教训在本轮新代码上被遵守**，没有新增「换世界残留」面。

---

## 6. 主人真机复验清单（我没有启动过游戏，以下全部待实机）

> 验收边界：**这个系统的判定全在服务端，静态只能证明「代码接对了」**；
> 数值手感、观感、帧率只有真机能判断。

### 6.1 帧率 / 性能
1. 装上网络接入仓 + RAM 相关义体，打开 HUD 与超频视觉，观察 F3 帧率与 0.3.12（trash 里
   `cyberware-0.3.12-Beta-round3.jar`）对照。重点看：`RamHud` 每帧重绘、`OverclockWireframe`
   的 16 实体上限是否够用（人数多时是否会掉帧）。

### 6.2 观感
2. RAM 条：颜色/宽度/位置是否顺眼；**恢复时是否平滑**（客户端插值是「消耗快、恢复慢」）。
3. 超频视觉：全屏特效、头顶全息面板、生物荧光轮廓颜色；超频结束是否有突兀跳变。
4. 濒死超频/瘫痪时的红字与警告闪烁是否看得清、是否挡视线。

### 6.3 玩法 / 破解手感
5. **G 键**切换超频：装了接入仓能开、没装应被拒（且**被拒也有提示**）；冷却期间再按应被拒。
6. **RAM 消耗**：连按破解，RAM 应下降并在恢复；高于上限的消耗应失败（瘫痪提示）。
7. **濒死超频**：RAM 不够但超频开着时，应扣血（**不会致死**）而不是拒绝。
8. **五条快速破解**（过热/短路/突触熔断/武器故障/系统重置）手感与伤害是否符合预期
   —— 注意**数值全是 TODO 占位**。
9. **AI 禁用**：系统重置后目标应停止行动、到期恢复；武器故障应拦住来源实体投掷物。
10. **换世界**：退到标题 → 新建世界，RAM 条与超频 HUD 应从 0/关闭开始（P0 教训的实机验证）。

### 6.4 日志关键字（加载/注入是否正常）
`/sdcard/FCL/.minecraft/versions/26.1.2-NeoForge/logs/latest.log` 搜
`Mixin apply ... failed` / `InvalidInjectionException` / `Invalid name:` / `Scanned 0 target(s)`
/ `Attempted to register a payload handler twice`（最后一条专门盯 §4.2 那个双注册风险）。
另应能看到加载的是 `cyberware-0.4.0-Beta.jar`。

---

## 7. 非阻断发现 / 待办

| # | 级别 | 内容 | 建议 |
|---|---|---|---|
| **F1** | low（易混） | 版本号从 `0.3.12-Beta` 升到 **`0.4.0-Beta`**（`build.gradle` +1/−1），但**验收文本与本仓 `build/libs/` 里仍留着旧的 `cyberware-0.3.12-Beta.jar`**（664851 字节，01:29 的旧产物）。两个 jar 并存容易让人拿错包部署 | 部署时只认 `0.4.0-Beta.jar`；建议下轮清理 `build/libs` 旧产物 |
| **F2** | low（口径） | `en_us.json` 本轮 +2 条：`key.cyberware.radial` / `key.cyberware.overclock`（`zh_cn` +1 条 `key.cyberware.overclock`）。0.3.12 的裁决是「只补中文」，但那次针对的是**物品名 `item.cyberware.*`**；本次是**键位显示名**，属另一类。是否允许请主人确认 | 若「只补中文」适用于全部 lang，则这 2 条英文需回退；否则无需动作 |
| **F3** | info | **X 键（扫描）尚未实现**：t26（扫描/锁定框/上传条/破解轮盘）本轮取消、延到下一轮，因此 `CyberwareKeys` 里没有第 4 个键位 | 下轮补 X 键时注意与 V/R/G 的冲突检查 |
| **F4** | info（沿用 t23 已知） | `RAM_REGEN` 单位不一致（差 60 倍，未换算）；超频时长/冷却与五条破解数值**全是占位** | 等主人给数值 |
| **F5** | info | 我验收时**未能执行任务书里的 `unzip -l`**（本机无 unzip），已用 `python3 zipfile` 等价核验（§2） | 无需动作，仅记录口径差异 |

**没有发现阻断项**：构建 exit 0、新类全在已部署 jar 内、协议/客户端 handler 契约自洽且无双注册、
客户端无任何权威判定、OS 槽位=2、键位无冲突、P0 教训在本轮新代码上被遵守。

---

## 8. 一句话总结

0.4.0 RAM System 的**静态契约是自洽的**：三个包在服务端按 NeoForge 要求注册（`playToClient` 无 handler /
`playBidirectional` 只给服务端 handler），客户端在 `RegisterClientPayloadHandlersEvent` 补齐三个 handler
且只做显示，`PROTOCOL_VERSION="4"`、`OPERATING_SYSTEM.maxCount=2`、13/13 注入器 `require=0`、
`RamClientState.clear()` 已接入换世界分支 —— 我核验的产物与真机部署的 jar **md5 完全相同**（`02e3dce0…`）。

**但我没有启动过游戏**：帧率收益、HUD 观感、破解手感、超频视觉与「数值是否好玩」必须由主人按 §6 真机判断。
