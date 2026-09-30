# HACK-CLIENT.md —— t29 客户端：X 键扫描 + 锁定框 + 上传进度条 + R 键破解轮盘

> opticsdev，attempt `c7adee13-ad19-41d2-86d4-94aa-9f6a5f0a4c9e`（详见任务记录）
> 接口全部按 `STEP3-CONTRACT.md` §1.1–§1.5（冻结）。**未真机验证**（见文末）。
>
> ⚠️ **键位部分已被 0.5.1-Beta 取代**（现行口径见 `STEP4-CONTRACT.md` §3）：
> **V** 手持激活 ｜ **R** 义体轮盘 **+「脑机超频」项** ｜ **X 短按（按住 < 300ms）** 扫描 ｜
> **X 长按（≥ 300ms）** 呼出快速破解轮盘 ｜ **G 键已取消**。
> §1.3 的「R 键上下文（有目标 → 破解轮盘）」已在 0.5.1 取消 —— 破解轮盘现在只由 X 长按呼出。
> 其余内容（锁定框口径、上传条、帧率自保、未真机验证项）**仍然有效**。

---

## 1. 完成项

### 1.1 X 键歧路司扫描（契约 §1.1）
- `CyberwareKeys.SCAN`（`key.cyberware.scan` / `KEY_X` / 同一 CATEGORY），已在 `onRegisterKeys` 里
  `event.register`；lang 已补 **zh_cn**`歧路司扫描` 与 en_us`Kiroshi Scan`。
- 分发：`onClientTick` 里新增 `while (SCAN.consumeClick()) sendToServer(HackPayload.scan())`
  —— 字段由 coredev 的工厂收口（`action=SCAN / hackId="scan" / targetEntityId=-1`，契约 §1.1）。
- **不自己加效果**：客户端只发请求；装没装义眼、扫谁、1200 刻发光全在服务端。
- 被拒提示：`HackHud.drawNote` 把 `NO_KIROSHI` 翻成「未安装歧路司义眼 —— 无法扫描」，
  `NO_TARGET` → 「无锁定目标」，`RAM ACCESS FAILED` → 「RAM 不足 —— 破解失败」（**大写**，与 t28 统一后的字符串一致）。
- **V / G / R 行为**：V、G 两个 while 块一行未改；R 的改动见 §1.3。

### 1.2 屏幕空间折角锁定框（契约 §1.2）
- 口径：**屏幕中心 10% 框内 + 距离 ≤ 20 格 + 最近的活体**（`HackClientState.tickLock()`，**每 tick 一次**）。
- 投影：`ScreenProjection`（与 `RamHud.drawHolo` 同一套换算：`projectPointToScreen` → NDC →
  `((x+1)/2*w, (1-y)/2*h)`，`z ∈ [-1,1]` 之外视为背后/超距）。结果放在**静态字段**里，不产生对象。
- 绘制：**三条细线折角框**（左上/右上/左下，各两条 1px 线）；框内底部**极细血条**、
  框上方**名称**、框下方**5 个破解可用性小方块**（本模组暂无破解图标素材，用几何图形占位）。
- 清除：目标死亡 / 超距 / 转出视野 / 本地玩家死亡 → `tickLock` 立刻 `clearLock()`；
  渲染侧 `lockedEntity()` 为 null 就**直接 return**，**不留残影**。

### 1.3 R 键上下文（契约 §1.3）
```java
while (CyberwareKeys.RADIAL.consumeClick()) {
    if (HackClientState.hasLock()) HackRadialScreen.openIfLocked(mc);
    else                            CyberwareRadialScreen.openOrHint(mc);   // 原样，一字未改
}
```
- `HackRadialScreen`：5 条破解（过热/短路/突触熔断/武器故障/系统重置 + RAM 消耗），
  **RAM 不足的条目置灰**（灰底板 + 灰字 + 红字提示「RAM 不足 —— 置灰不可发」），
  **选中但 RAM 不足时不发包**（`commit()` 里只有 `affordable` 才取 `hackId`）。
- 扇区算法与 `CyberwareRadialScreen` **同一套公式**（第 0 项正上方、顺时针均分、半径只当死区门槛、
  `((k%n)+n)%n`）。**没有改 `CyberwareRadialScreen` 任何一个字符** ——
  「无目标时行为与 0.4.0 完全一致」是硬要求，靠「不动它」来保证。
- 施放：`HackPayload.cast(lockEntityId, hackId)`；ESC 只关不发；`finished` 保证只发一次。

### 1.4 上传进度条（契约 §1.4）
- 数据全部来自 `HackPayload` 下行：`UPLOAD_START`（起）→ `UPLOAD_PROGRESS`（推进）→
  `APPLIED`（成功）/ `CANCELLED`（打断）/ `REJECTED`（被拒）→ **收缩消失**。
- 屏幕中央偏下（`y = guiHeight()*0.72`）：**极细红线（2px）** + 流动光效（亮色沿进度来回跑）+
  上方全息倒计时 `UPLOADING... 1.2s` + 右侧 **8 个字符**的滚动二进制乱码。
- 结束时按 `lastProgress` 起算，250ms 内**宽度收缩 + 淡出**，动画结束调 `endCollapse()` 归零 —— 之后不再画任何东西（**无残影**）。

### 1.5 只读展示表（契约 §1.3 末）
`HackClientState.HACKS` 是 `record HackEntry(id, name, ramCost)` 的**不可变列表**，
只用于轮盘显示与「置灰」判断。**客户端不做任何扣费或效果判定** ——
`CAST` 后是否扣、扣多少、给不给效果全由服务端裁决（t28 已在扣费前校验目标）。

---

## 2. 未做项

1. **扫描成功的本地脉冲**：coredev 说服务端**成功路径不回包**（发光世界里看得见）。
   我按契约只发了上行请求，**没有**额外做「按 X 时的本地反馈脉冲」——
   视觉反馈目前完全依赖服务端的发光效果。若主人想在按 X 瞬间就有反馈，说一句，我加一个本地脉冲（不依赖网络）。
2. **锁定框没有做「超距渐隐」**：超距直接清除（契约要求「超距时清除」，没要求渐隐）。
3. **破解图标用的是几何方块**：本模组没有破解图标素材，契约也只要求「可用破解图标」，
   这里用 5 个小方块表示可用性（亮 = RAM 够，暗 = 不够）。
4. **RAM 消耗是占位值**：`HACK-VALUES.md`（t30）还没落地，5 条的成本写了
   `TODO(主人填写)` 注释的占位值（4/5/7/3/9）。替换时按契约 §2 在同行注明来源。
5. **没有改 `cyberware.mixins.json`，也没有新增 Mixin** —— 全部走事件（`RenderGuiEvent.Post`），
   所以不需要向 coredev 登记。

---

## 3. 帧率自保措施（契约要求写入文档）

| 措施 | 数值 | 位置 |
|---|---|---|
| 无锁定目标 → 立刻 return | 0 次投影 | `HackHud.drawLockFrame` |
| 无上传且不在收缩 → 立刻 return | 0 次绘制 | `HackHud.drawUploadBar` |
| 每帧投影次数 | **≤ 1**（只给已锁定的目标） | `drawLockFrame` |
| 锁定筛选频率 | **每 tick 一次**（不是每帧） | `HackClientState.tickLock` |
| 每 tick 候选投影上限 | **12 个** | `HackClientState.MAX_CANDIDATES` |
| 投影结果 | 静态字段，**不 new 对象** | `ScreenProjection` |
| 二进制乱码 | **8 个字符**（不铺满） | `drawUploadBar` |
| 上传条 | 2px 高、160px 宽 | `UP_BAR_H/UP_BAR_W` |
| 提示条 | 2.5 秒后停画 | `drawNote` |
| 锁定框线段 | 6 条 1px 线（3 个折角） | `drawCorner` |

**已知一处妥协（如实记录）**：`projectPointToScreen` 每次调用都会 `new Vec3`（原版 API 签名如此），
所以「零分配」做不到绝对 —— 但调用被压到 **每帧 ≤1 次 + 每 tick ≤12 次**，
且候选集合先按 20 格距离过滤，实际远小于 12。这是本实现能做到的最小分配。

---

## 4. ⚠ 未真机验证

**没有启动过游戏。** `compileJava` 通过（BUILD SUCCESSFUL in 1m 1s，exit 0）只说明能编译。

需要主人真机确认：

1. **锁定框的 10% 口径是否跟手**：屏幕中心 10% 框 + 最近活体，可能「稍微偏一点就丢锁」；
2. 折角框的大小随距离变化（30~58px）是否合适；框内血条/名称/5 个方块的位置是否挤；
3. 上传条位置（`0.72*h`）是否被热键栏挡住；2px 红线在实际 GUI 缩放下是否看得清；
4. **R 键的上下文切换**是否符合直觉：正瞄着一个生物时按 R 会进破解轮盘而不是义体轮盘；
5. RAM 置灰判断用的是客户端同步下来的 RAM（可能有 1 秒内的延迟）；
6. X 键与既有键位冲突；
7. 只读展示表的 5 个 RAM 占位值（等 t30 的真数字）。

**最容易出问题的一处**：`ScreenProjection` 的 NDC→屏幕换算与 `z∈[-1,1]` 剔背面判据 ——
若真机发现锁定框位置偏移或锁到身后的目标，改点只在 `ScreenProjection.project` 与
`HackClientState.tickLock` 这两处。
