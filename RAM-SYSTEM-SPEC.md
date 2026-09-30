# RAM System + 脑机超频 + 快速破解 —— 实施契约

来源：`/sdcard/DSH/Neko专属邮箱📮/邮件IX.txt`（标题 CBAD，发件人：狗修金sama）
目标版本：0.4.0（继续 0.3.12 之上）

---

## 0. 主人已拍板的决定（不可自行更改）

| 项 | 决定 |
|---|---|
| 歧路司扫描键 | **另开一个键**，默认 **X**（`InputConstants.KEY_X`）；V 仍为「手持义体激活」，R 不变 |
| 破解轮盘键 | **R 直接呼出轮盘**；无锁定目标时 R 回落为「已安装义体轮盘」（上下文切换，保留 t14 的 `CyberwareRadialScreen`） |
| 操作系统槽位上限 | `CyberwareSlot.OPERATING_SYSTEM` 的 `maxCount` **1 → 2** |
| 分步 | 按邮件三步：① RAM 核心+服务端+同步 ② HUD+超频视觉 ③ 锁定框+上传条+破解库 |

---

## 1. 数据模型（服务端为唯一真相）

* **RAM**：新 `AttachmentType`（`registry/ModAttachments.java` 里加 `cyberware:ram`）。
  沿用 `CyberwareInstallation` 的 `AttachmentType` 模式 —— 存档持久化、死亡拷贝、跨维度、自动同步白送。
  - 字段：`current`（double，保留小数），`max`、`regenPerMinute` 为**派生值**（不落盘，按义体实时算）。
  - **上限/恢复算法（本契约定死）**：遍历已安装义体，把各自 `variant.stats()` 里的
    `Stats.RAM` / `Stats.RAM_REGEN` **求和**（含 RAM配平/RAM升级/智能连接/网络接入仓等所有来源）。
  - ⚠️ **现状提醒**：仓库里目前**没有任何统计聚合器**（`CyberwareInstallation` 只有容量与顺序，
    统计数字只用于 UI 文案）。聚合器是本轮新增件，不许假设它已存在。
* **超频状态**：`active` / `expiresAt` / `cooldownUntil`，时长与冷却由**已安装的网络接入仓**品质（`variant.rarity`）决定。
* **破解状态**：`目标 UUID → 剩余上传刻数 / 已生效效果及到期刻`。
* **协议**：新增 `RamPayload` / `OverclockPayload` / `HackPayload`，沿用 `network/CyberwareNetwork.java`
  的 `PayloadRegistrar` 模式（`ActivatePayload` 是现成范例），`PROTOCOL_VERSION` 升到 `"4"`。
  客户端只渲染，不做任何扣费判定。

## 2. 玩法规则（邮件 §三）

1. 正常：超频开启 + RAM 充足 → 扣 RAM，HUD 平滑缩短。
2. **濒死超频**（RAM 耗尽且超频开启）：RAM 条转血红流动；释放破解**不扣 RAM，改扣 2 点生命**，
   **不会致死（最低保留 1 颗心 = 2 HP）**；屏幕边缘红光 + 玻璃碎裂粒子 + 心跳音。
3. **彻底瘫痪**（RAM 耗尽且超频未开启）：拒绝释放，屏幕中央闪 `RAM ACCESS FAILED` 红字。

## 3. 破解库（5 条，消耗为占位值，等主人调）

| 破解 | 效果 | 上传 | 占用 RAM（占位） |
|---|---|---|---|
| 过热 | 点燃目标 3 秒 + 降低护甲 | 1.0s | 4 |
| 短路 | 高额瞬间伤害 + 雷击粒子 | 1.5s | 5 |
| 突触熔断 | 虚弱 + 缓慢 5 秒 + 烧脑粒子 | 2.0s | 3 |
| 武器故障 | 禁用远程攻击 AI 8 秒 | 1.2s | 6 |
| 系统重置 | 无法移动与攻击 3 秒 | 2.5s | 8 |

## 4. 技术铁律（本项目血泪，逐条照做）

1. Mixin **单点注入一律写 `require = 0`**（`cyberware.mixins.json` 的 `injectors.defaultRequire = 1` 会咬人）。
2. 写 Mixin 前先 **`javap` 核实目标方法名与签名**（冒号陷阱：Java 内部描述符 `name:(args)` 要写成 `name(args)`）。
3. 渲染回调**整个方法体包 try/catch**，异常不许冒到渲染线程。
4. 判定「本地玩家」用渲染状态类型（`state instanceof AvatarRenderState`），**绝不用坐标/距离比较**。
5. 贴图只用 **32×32**；**没有素材的部分先不做**，不许拿高清图。
6. 音效先用**原版音效事件 + pitch 处理**（邮件要的 +20% 音调、风扇嗡鸣、键盘声），不自造 ogg。
7. 跨世界/状态残留是 0.3.12 的 P0 教训：任何静态缓存必须绑 `Level`/`player` 身份，并提供 `clear()`。
8. 静态验证不等于玩得对：编译通过 ≠ 注入成功。**产物级核验**（javap 看编译进 jar 的类）＋主人真机复验。

## 5. 文件归属（写作用域，互不重叠）

| 成员（沿用 cyberware-0312 名册） | 可写 |
|---|---|
| coredev | `core/**`、`network/**`、`event/**`、`registry/**`、`mixin/**`（非 client）、`src/main/resources/cyberware.mixins.json` |
| opticsdev（客户端） | `client/**`、`mixin/client/**`、`src/main/resources/assets/cyberware/**`（lang/sounds） |
| inspector（只读验收） | `VERIFY-RAM.md` |

冲突点提前钉死：**键位注册（`CyberwareKeys`）与按键分发（`CyberwareClient`）归 opticsdev**，
coredev 不碰 `client/**`；`CyberwareSlot.OPERATING_SYSTEM` 上限改动归 coredev。
`cyberware.mixins.json` 是**共享文件**：归 coredev 主写，opticsdev 需要追加时先 `send_message` 交接。

## 6. 验收

* `gradle build --no-daemon` 绿（`JAVA_HOME=/usr/lib/jvm/java-21-openjdk-arm64`，`/opt/gradle-8.10.2/bin/gradle`）。
* 产物核验：新类真的在 jar 里（`javap -cp build/libs/*.jar ...`），Mixin 注册项在 `cyberware.mixins.json` 里。
* 真机清单交给主人：超频视觉是否掉帧、线框怪帧率、HUD 观感、破解手感 —— **这些代码里验不出来，必须主人跑**。

---

## 7. 26.1 现成 API（captain 用 javap 提前核实，成员不必再摸索）

字节码：`javap -p -classpath build/moddev/artifacts/minecraft-patched-26.1.2.109-merged.jar <类名>`
反编译源码：`build/moddev/artifacts/minecraft-patched-26.1.2.109-sources.jar`
⚠️ **本机没有 `unzip`**（`unzip -l` 会静默失败、看起来像"找不到类"）：读源码用
`python3 -c "import zipfile;print(zipfile.ZipFile('...sources.jar').read('net/minecraft/....java').decode('utf-8','replace'))"` 或 `$JAVA_HOME/bin/jar`。

### 7.1 AI 禁用（t24）
```
protected final void Mob.serverAiStep()                       // 无参，final 只挡继承不挡 @Inject
protected void       Mob.customServerAiStep(ServerLevel)      // 单生物 AI 步骤
public void          Mob.aiStep()                             // 客户端+服务端都会跑，慎用
public void          Mob.setNoAi(boolean) / boolean isNoAi()  // 原版开关，存在
public void          Mob.removeAllGoals(Predicate<Goal>)      // 删了回不来，别做临时状态
public PathNavigation getNavigation() / getLookControl() / getMoveControl()
```
**建议**：`系统重置` = 原版 `setNoAi(true)` + `getNavigation().stop()`，到期 `setNoAi(false)`（零 Mixin、天然平滑恢复）；
`武器故障` = 优先服务端事件拦来源实体射出的投掷物（不改 AI 状态），确需 Mixin 时注入无参 `serverAiStep()`。
`Mob` 上**没有** `performRangedAttack`（远程逻辑在各 Goal 内），别按旧版记忆写。

### 7.2 世界坐标 → 屏幕坐标（t26）
```
public Vec3 GameRenderer.projectPointToScreen(Vec3)   // mainCamera.getViewRotationProjectionMatrix() + transformProject
public double GameRenderer.projectHorizonToScreen()
```
`transformProject` 自带透视除法 → 返回 **NDC**：`x,y ∈ [-1,1]`；屏幕像素 =
`((x+1)/2*width, (1-y)/2*height)`。**背后/超距必须自己判**（看 NDC 的 z 或与相机前向点乘），
否则目标在身后时锁定框会画到屏幕正中。
