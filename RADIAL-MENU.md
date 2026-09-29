# RADIAL-MENU.md —— R 键义体轮盘（t15）

> 交付：**新建** `src/main/java/com/dsh/cyberware/client/CyberwareRadialScreen.java`
> + `client/CyberwareClient.java` 里只加 **4 行**（2 行注释 + 2 行代码）
> + 本文件。
> 另：`RADIAL-MENU.md` 与 `CyberwareRadialScreen.java` 之外没有改任何文件（未动 `network/**`、
> `core/**`、`resources/**`），未 commit。
> ⚠ **未真机验证**（见 §7）。

---

## 0. 原创与 assets 声明（必读）

- **本实现未参考、未阅读、未反编译 Iron's Spells 'n Spellbooks 的任何代码**，
  也**没有**把它作为依赖、没有 fork 它的任何部分。双方代码库之间不存在复制关系。
- **未使用任何外部 assets**：轮盘的每一个图标都来自**本模组自己已入库的**
  `src/main/resources/assets/cyberware/textures/item/<id>.png`（32×32），
  路径规则与 `CyberwareStationScreen.itemIcon(...)` 完全相同
  （`Identifier.fromNamespaceAndPath(Cyberware.MODID, "textures/item/" + def.id() + ".png")`）。
  没有引入任何第三方贴图 / 模型 / 音效 / 字体 / 资源文件。
- 全程没有新增任何依赖（`build.gradle` 未改动），没有引入第三方库。
- 借鉴的**只有交互形式**：「按住某键弹出环形菜单 → 鼠标指向选择 → 松开确认」。
  这属于**功能与思想层面**，不受版权保护；具体实现（扇区算法、渲染、状态机、
  按键接管、协议对接）全部是按本模组自身的 API 与 26.1 的 `Screen`/`GuiGraphicsExtractor`
  现推现写的原创代码。
- 图标完整性已核验：判定会进轮盘的 **13 件**义体，其 `<id>.png` **全部存在**（不依赖任何外部素材）。

---

## 1. 交互设计

| 阶段 | 行为 |
|---|---|
| 按住 **R** | 打开轮盘（`Minecraft.setScreen` → 鼠标自动释放，玩家可以指向；世界不停） |
| 移动鼠标 | 鼠标所在扇区高亮，中心显示该项名称 |
| 鼠标靠近中心（< 死区） | 不高亮任何项，中心显示「指到图标上选择 / 松开 R 取消」 |
| **松开 R** | 关闭轮盘；**选中了就发包施放**，没选中就一个包都不发（= 取消） |
| **ESC** | 关闭且**不施放** |
| 一件主动义体都没有 | **不开空盘**，只给一条动作栏提示「没有可用的主动义体（先装上斯安威斯坦或狂暴）」 |
| 已经开着界面（`minecraft.screen != null`） | 忽略本次 R，不叠第二个界面 |
| 按住 R 时窗口失焦 | 兜底：下一个 tick 检测到物理键已松开 → 照常收尾（见 §3.3） |

交互细节：

- 轮盘是一层**变暗的悬浮界面**：`isInGameUi() = true` → 走原版「游戏内界面」背景
  （`extractTransparentBackground` 的透明渐变压暗），而不是全景图/模糊。
- `isPauseScreen() = false`：单人游戏里按住 R **不暂停世界**。
- 打开用 `Minecraft.setScreen`，**不是** `pushGuiLayer`：前者会 `mouseHandler.releaseMouse()`，
  光标才会被放出来给玩家指向；`pushGuiLayer` 不会释放鼠标，拿出来是没法指的。
- 中心有一块信息条：显示当前选中项名称 + 「松开 R 施放」；未选中时显示取消提示。
- 每项画「底板 + 图标 + 名称」；选中项底板换成亮青色并描一圈亮边，另有一条从中心连过去的辐条。

---

## 2. 扇区算法（角度 + 死区）

### 2.1 布局

- 屏幕中心 `(cx, cy) = (width/2, height/2)`；**死区半径 `DEAD_ZONE = 24`**（GUI 像素）；
  **图标环半径 `RING_RADIUS = 78`**；图标绘制边长 28（源贴图 32×32）；底板半边 17。
- `n` 项**均分**一圈，第 `i` 项的圆心角：

```
step  = 2π / n
angle(i) = -π/2 + i * step          // -π/2 = 正上方；i 增大 = 屏幕上的顺时针方向
iconX(i) = cx + cos(angle(i)) * RING_RADIUS
iconY(i) = cy + sin(angle(i)) * RING_RADIUS
```

即 **第 0 项永远在正上方**，之后顺时针排（右、下、左……）。

### 2.2 选中判定

```java
double dx = mouseX - width / 2.0, dy = mouseY - height / 2.0;
if (Math.hypot(dx, dy) < DEAD_ZONE) { selected = -1; return; }   // 死区：不选
double step = (Math.PI * 2.0) / n;
long k = Math.round((Math.atan2(dy, dx) + Math.PI / 2.0) / step);
selected = (int) (((k % n) + n) % n);
```

逐条解释：

1. **半径只当门槛**——超过死区后，选谁**完全由角度决定**（这样鼠标推到屏幕边缘也不会
   「选不中」，手感与扇形菜单一致）。死区的作用是防手抖：鼠标停在中心附近时不误选，
   此时松开 = 取消。
2. `atan2(dy, dx)` 得到鼠标方位角 `a ∈ (-π, π]`（`dy` 向下为正，与 MC 的屏幕坐标一致）。
3. `+π/2` 把坐标系挪成「正上方 = 0」，与 §2.1 的 `angle(i)` 对齐。
4. 除以 `step` 后 `round` —— 这一步就是「离哪一项的圆心角最近就选哪一项」：
   `round` 天然把每个 `±step/2` 的角度区间分给对应项，不需要写 if-else 判边界。
5. `((k % n) + n) % n`：上半屏的鼠标角度会让 `k` 为负（Java 的 `%` 保留负号），
   取模两次把它折回 `0..n-1`。

### 2.3 这一段的数值自测（不靠肉眼）

把**同样**的公式在脚本里跑了一遍：

```
各项圆心角处取点：91 个用例（n = 1..13 全部项），失败 0
分界角邻域（±step/2 两侧 1e-6）：全部落在 0..n-1 内，无越界
死区：半径 10 → -1；半径 23.9 → -1（应为 -1）；半径 24.1 且角度 0° → 1（右侧项，n=4）
正上方 n=4 → 0；正下方 → 2；正左方 → 3
```

> 唯一「反直觉」的点：n=4 时角度 **-135°** 取到的是 **0（上）** 而不是 3（左）。
> 这不是 bug —— -135° 正好是「上（-90°）」与「左（-180°）」的**精确中点**，
> 是平局；`Math.round` 对 -0.5 取 0（`floor(x+0.5)`），于是归给「上」。
> 平局必须有归属，两边的选择都合理，这里只是记录一下确定性行为。

---

## 3. 生命周期与状态机

### 3.1 打开

```java
// CyberwareClient.onClientTick
while (CyberwareKeys.RADIAL.consumeClick()) {
    CyberwareRadialScreen.openOrHint(Minecraft.getInstance());
}
```

`openOrHint` 内部两件事：**没有可选项就只提示不开盘**；`minecraft.screen != null` 就直接忽略。

### 3.2 关闭 + 施放

`keyReleased` 判断是本模组的 R 键 → `commit()`：

```java
String defId = selected >= 0 ? entries.get(selected).id() : null;
KeyMapping.set(CyberwareKeys.RADIAL.getKey(), false);   // 自己清按下状态（见下）
this.minecraft.setScreen(null);                          // 恢复鼠标抓取
if (defId != null) ClientPacketDistributor.sendToServer(ActivatePayload.of(defId));
```

几个刻意的细节：

- **`finished` 标志**：`commit()` 与 `onClose()` 都会置位，保证「只关一次、最多发一个包」。
  ESC 路径（`onClose`）只关不施放。
- **自己清 `KeyMapping` 的按下状态**：`KeyboardHandler` 的规则是
  「`screen.keyReleased(event)` 返回 true → 跳过 `KeyMapping.set(key,false)`」。
  我们返回 true（为了不让这次松手落进别的逻辑），所以要自己补这一句，避免 `RADIAL`
  卡在 `isDown`。打开时 `setScreen` 本来就会 `KeyMapping.releaseAll()`，这里是第二道保险。
- **`keyPressed` 里吃掉 R**：轮盘开着时再按 R，`KeyboardHandler` 会先把事件给屏幕；
  如果我们返回 false，它就会 `KeyMapping.click(...)` 塞一个 click，轮盘一关就被那个
  残留 click 立刻重新打开。返回 true 就没有这个隐患。
- 松手事件只在**本模组绑定的那个键**上生效（`event.key() == RADIAL.getKey().getValue()`），
  其它键原样交给 `super`，互不干扰（键位被玩家改绑也照样跟着走，因为读的是 `getKey()`）。

### 3.3 兜底：物理键松开检测（`tick()`）

按住 R 时如果窗口失焦（或被系统吞掉 `keyReleased`），光靠事件会把轮盘卡在屏幕上。
所以 `tick()` 里查一次 GLFW 的真实按键状态：

```java
down = window != null && window.handle() != 0L
       && InputConstants.isKeyDown(window, CyberwareKeys.RADIAL.getKey().getValue());
if (down) sawKeyDown = true; else if (sawKeyDown) commit();
```

- `InputConstants.isKeyDown(Window,int)` 的实现就是 `GLFW.glfwGetKey(handle, key) == 1`
  （已 javap/读源码确认），查的是**物理状态**，不受 `KeyMapping.releaseAll()` 影响。
- **必须先见过一次「按下」**（`sawKeyDown`）：否则万一首帧 GLFW 状态抖动，
  刚打开的轮盘会被立刻关掉。只见过按下之后再松开，才会走这条兜底。
- 查询本身包了 `try/catch`，查询失败就当作「还按着」，把收尾交回给 `keyReleased`。

---

## 4. 主动效果判定口径（与服务端一致）

轮盘只列「**已安装** 且 **有主动效果**」的义体。判定**完全照抄服务端**，
客户端**不另创一套规则**：

```java
// CyberwareRadialScreen.hasActiveEffect —— 对应 CyberwareAbilities.activate(...) 里仅有的两个给效果分支
CyberwareDefinition.Variant variant = def.variantFor(data.rarity());
if (variant == null) variant = def.baseVariant();                      // 稀有度回退，照抄
if (variant.stat(CyberwareDefinition.Stats.TIME_SLOW, 0.0D) > 0.0D) return true;  // ← CyberwareAbilities.java:37-38
return def.id().startsWith("berserk_");                                // ← CyberwareAbilities.java:50
```

出处（逐字对照，行号按当前工作区）：

- `CyberwareAbilities.java:37` `double ratio = variant.stat(CyberwareDefinition.Stats.TIME_SLOW, 0.0D);`
- `CyberwareAbilities.java:38` `if (ratio > 0.0D) {` → 时间减缓分支
- `CyberwareAbilities.java:50` `if (def.id().startsWith("berserk_")) {` → 狂暴分支
- `CyberwareAbilities.java:28-35` 的稀有度回退（`variantFor` 为 null 时用 `baseVariant()`）也一并照抄，
  否则会出现「服务端按高稀有度判有、客户端按别档判无」的错位。

「已安装」的数据来源（全在客户端可读，`AttachmentType` 带 sync）：

```java
CyberwareInstallation installed = CyberwareInstallation.of(player);   // 永不返回 null
for (String id : installed.orderedIds()) { ... installed.dataOf(id) ... }
```

两道跳过（与服务端 `activateInstalled` 的失败条件一致）：
`CyberwareDefinitions.byId(id) == null`（旧存档被删型号）、`installed.dataOf(id) == null`
（服务端也是「查不到就直接 false」）→ 都不进轮盘。

**当前实际会是哪些型号**（按上面的口径在 135 条生效定义里筛）：共 **13 件** ——
`sandevistan_apogee / c4 / c1 / c2 / c3`、`kerenzikov`、`kerenziov_boost_system`、
`synaptic_accelerator`、`iconic_reflex_recorder`、`berserk_c1..c4`。
这 13 件的 `<id>.png` **全部存在**（已逐个核验）。

> 顺带一个值得记下的现象：判定口径里 **没有** `CyberwareDefinition.active()`。
> 服务端 `activate()` 也不看它，所以为了「轮盘里能选 = 服务端会执行」严格一致，
> 这里同样不看。若将来服务端改成看 `active()`，这里要同步改。

---

## 5. 与 t13 协议的对接点

| 对接点 | 用的东西 | 说明 |
|---|---|---|
| 发施放请求 | `ClientPacketDistributor.sendToServer(ActivatePayload.of(defId))` | `defId` = 轮盘选中项的型号 id |
| 协议语义 | `ActivatePayload(String defId)`，`HELD = ""` | 轮盘只会发**非空** defId；空串那条是 V 键手持路径，不归轮盘 |
| 服务端权威 | `CyberwareAbilities.activateInstalled(player, defId)` | 服务端自己查已安装附件；伪造 defId 只得到一条 debug 日志 |
| 取消语义 | 未选中 → **不发包** | 不依赖服务端「空串 = 手持」的兼容逻辑，从源头避免误触发 |
| 键位 | `CyberwareKeys.RADIAL`（默认 R，已注册） | 由 t13 注册，t15 只消费 |

安全边界（沿用 t13 的结论）：`defId` 只是**意图声明**，不是权限凭证。轮盘即使在客户端被
篡改，也只能让服务端去查表；查不到就没有任何效果。

---

## 6. 接线 diff（`CyberwareClient.java`，属于我的只有 4 行）

> 注意：下面 `git diff` 是相对 HEAD 的，里面**还包含 t13（coredev）已做的两处改动**
> （`event.register(CyberwareKeys.RADIAL)` 与 `new ActivatePayload(ActivatePayload.HELD)`）。
> **t15 实际新增的只有标了 `← t15` 的 4 行**（2 行注释 + 2 行代码）。

```diff
@@ -76,7 +78,13 @@ public final class CyberwareClient {
     /** 按键触发 → 发一句请求给服务端（服务端才是有权改数据的一方）。 */
     private static void onClientTick(ClientTickEvent.Post event) {
         while (CyberwareKeys.ACTIVATE.consumeClick()) {
-            ClientPacketDistributor.sendToServer(new ActivatePayload());
+            // V 键 = 手持激活：defId 用 HELD（空串），服务端行为与以前完全一致     ← t13
+            ClientPacketDistributor.sendToServer(new ActivatePayload(ActivatePayload.HELD));  ← t13
+        }
+        // R 键 = 义体轮盘（按住弹出、松开施放）。轮盘自己管后续关闭/发包，这里只负责开；   ← t15
+        // openOrHint 内部会挡掉「界面已开着」和「一件主动义体都没有」两种情况。          ← t15
+        while (CyberwareKeys.RADIAL.consumeClick()) {                                    ← t15
+            CyberwareRadialScreen.openOrHint(Minecraft.getInstance());                   ← t15
         }
         SandevistanPostProcessor.tick();
```

`onClientTick` 里既有的其它逻辑（`SandevistanPostProcessor.tick()`、维度切换时的
时钟清理、`WeatherTickClock.tick`）**一行未动**。`network/**`、`core/**`、`resources/**`
均未改。

---

## 7. 验证状态

### 7.1 已做

```
cd /root/mod26/cyberware
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-arm64 \
PATH=$JAVA_HOME/bin:/opt/gradle-8.10.2/bin:$PATH \
gradle compileJava --no-daemon
```

```
> Task :compileJava
BUILD SUCCESSFUL in 1m 2s
PIPE_EXIT=0
```

（只跑 `compileJava`，**没有**跑完整 build；未 commit。）

产物级核对（`javap` 编译产物）：

```
public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor, int, int, float);
public boolean keyPressed(net.minecraft.client.input.KeyEvent);
public boolean keyReleased(net.minecraft.client.input.KeyEvent);
public void tick();
public void onClose();
public boolean isPauseScreen();
public boolean isInGameUi();
public void mouseMoved(double, double);
public static void openOrHint(net.minecraft.client.Minecraft);
public static java.util.List<...CyberwareRadialScreen$Entry> collectActive(net.minecraft.client.player.LocalPlayer);
```

覆盖签名与 26.1 的 `Screen` 逐字符一致（26.1 里 `Screen.render(...)` / `GuiGraphics`
已被 `extractRenderState(GuiGraphicsExtractor, ...)` 取代 —— 这点是 javap + 读 26.1 源码
确认的，不是凭记忆写的）。

另外两项静态核验（§2.3、§4）：扇区算法数值自测 91/91；13 件命中型号的贴图全部存在。

### 7.2 ⛔ 未真机验证 —— 明确声明

**没有在真机上启动过游戏，轮盘的任何视觉效果与手感都没有被验证过。**
`compileJava` 通过只说明**能编译**。以下全部需要主人在真机上确认：

1. 按住 R 是否真的弹出轮盘、松开是否真的关闭并施放（**松手施放这条路完全没跑过**）；
2. 图标位置是否是「正上方为第 0 项、顺时针排」，各图标是否落在期望的扇区上；
3. 死区 24 / 环半径 78 / 图标 28 这三个数在**实际 GUI 缩放**下是否合适
   （GUI 缩放 2~4 时，78 像素的环在不同分辨率下观感差别可能较大，可能要调）；
4. 中心信息条与 13 项的名称文字是否重叠、是否被屏幕边缘截断
   （n 较大时每项文字会挤在一起）；
5. 鼠标是否真的被释放出来（`setScreen` 的 `releaseMouse`）以及关闭后是否重新抓取；
6. 兜底路径（按住 R 时 Alt-Tab 失焦）是否按预期收尾；
7. 与 V 键、S 键等其它键是否有冲突；
8. 动作栏提示（一件主动义体都没有时）是否显示。

还有一条**依赖项**：轮盘要列出东西，前提是玩家身上**真的装了**义体
（服务端 `CyberwareInstallation` 且 sync 到客户端）。如果客户端拿到的一直是 `EMPTY`，
轮盘会一直显示「没有可用的主动义体」——那属于 t4/t13 的安装链路问题，不在本任务内。

---

## 8. 未做项（明确列出）

1. **没有做图标旋转/缩放动画**、没有选中音效（原版音效可用但未接）。
2. **没有做「按住时世界时间减缓」之类的联动**——只做菜单。
3. **没有做技能冷却显示**（`COOLDOWN` 数值已经在定义里，但轮盘没画冷却环）。
4. **没有做轮盘内翻页/分组**：超过约 8~10 项时扇区会变窄、文字会挤（当前最多 13 项）。
5. **没有做手柄/键盘方向键选择**：只支持鼠标指向（按需求书）。
6. **没有改任何非客户端代码**：服务端校验、协议、键位注册都在 t13，已就位。
7. **没有跑完整 build、没有 commit**（按硬约束）。
