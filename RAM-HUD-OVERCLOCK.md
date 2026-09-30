# RAM-HUD-OVERCLOCK.md —— t25 客户端：RAM HUD + 脑机超频视觉特效

> opticsdev，attempt `b1353fa8-61a3-43c5-8a25-51b3f20c0d13`
> **P0 全部完成**；P1 完成 4/5（见文末）。**未真机验证**（见 §7）。

---

## 1. P0-1 客户端 handler 注册（契约 §3.4 交接点）✅

`CyberwareClient.onRegisterClientPayloads`（`RegisterClientPayloadHandlersEvent`）：

```java
event.register(RamPayload.TYPE,      (p, ctx) -> ctx.enqueueWork(() -> RamClientState.onRamPayload(p)));
event.register(OverclockPayload.TYPE,(p, ctx) -> ctx.enqueueWork(() -> RamClientState.onOverclockPayload(p)));
event.register(HackPayload.TYPE,     (p, ctx) -> ctx.enqueueWork(() -> RamClientState.onHackPayload(p)));
```

已挂上 `modEventBus.addListener(CyberwareClient::onRegisterClientPayloads)`。
三个 handler 全部只更新**显示状态**，不改任何玩法数据（客户端不做扣费判定）。
`OverclockPayload` 只认 `Action.STATE`（收到 TOGGLE 回环也不当状态用）。

## 2. P0-2 现代化 RAM 条 ✅

`RamHud.drawRamBar`：左侧偏下 `x=12, y=guiHeight()-22`、`110×3 px` 极细条
（竖着的那两根斯安威斯坦/狂暴条底边在 -40，**不叠**）：

- **底层光晕**三层由外到内收窄（`0x12/0x2E/0x55` + 青蓝 `35E0FF`），**顶层实线**是 3px 实心核心色 `7BF7FF`，顶端 2px 纯白「充能到哪亮到哪」；
- **全息数字**右侧 `"6 / 8"`，`g.text(..., dropShadow=false)` = **无阴影**，半透明 `0xD09BE9FF`；
- **消耗 lerp 回退**：`RamClientState.updateForFrame()` 每帧按时间常数推进，
  `lerpSpeed()` = 消耗 `9.0`、恢复 `2.2`（快下慢上就是手感）；
- **恢复流动感灌满**：`current < max` 时条内有一段亮色按 900ms 周期来回跑；
- **数字跳动**：恢复中数字 alpha 按 160ms 周期脉动；
- 没有 RAM 能力（`max<=0`）→ 整条不画（契约 §5）。
- 冷却中：橙色暗罩 + `CD Ns`。

## 3. P0-3 脑机超频全屏视觉 ✅

`RamHud.drawFullScreen`：

| 效果 | 实现 |
|---|---|
| 荧光绿半透明扫描线覆盖 | 每 4px 一条，16 行一个明暗带，相位随时间滚动；结束前 3 秒周期 `1100ms → 260ms`（**加速**）+ 一条亮扫描带 |
| 屏幕边缘二进制流 | 左右各 2 列 `0/1`（`font` 画字，无阴影），按 `120ms/40ms` 向下滚动 |
| 结束前 3 秒 RGB 撕裂 | `finishStartMs`（`remainingTicks<=60` 时置位）→ 5 条错位色带（红/绿/蓝各一层）+ 洋红边框脉冲 |
| 濒死超频 | 血红半透明流动（220ms 周期脉动）+ 边缘红光 |
| 彻底瘫痪 | 中央闪 `RAM ACCESS FAILED`（红字、180ms 脉动、带底板） |

**生物荧光轮廓化 + 头顶全息面板**（`OverclockWireframe` / `RamHud.drawHolo`）：

- 轮廓：`RenderLivingEvent.Post` 里把生物自己的模型用**纯色半透明渲染类型再提交一次**
  （荧光绿 `39FF6A` / 玩家用洋红 `FF37C8`，alpha `0x7A`）。
  **诚实说明：这不是逐边线的真线框** —— 原版 `RenderTypes.LINES` 是 LINE 图元，实体模型是 QUADS，
  顶点格式不兼容；这里做的是「荧光色轮廓覆盖」，视觉目标（怪物变成荧光色轮廓）达成。
- 头顶面板：`gameRenderer.projectPointToScreen(...)` → NDC → 屏幕像素，画类型名 + 血量 + 血条。

## 4. P0-4 G 键接线 ✅

- `CyberwareKeys.OVERCLOCK`（`key.cyberware.overclock` / `KEY_G` / 同一 CATEGORY），
  已在 `onRegisterKeys` 注册；
- `onClientTick`：`while (OVERCLOCK.consumeClick()) sendToServer(OverclockPayload.toggleRequest())`；
- **V（手持激活）/ R（轮盘）的既有代码一行未改**（新增的是并列的第三个 while 块）；
- lang 已补：`zh_cn` 加 `key.cyberware.overclock`；`en_us` 补 `key.cyberware.radial` + `key.cyberware.overclock`。

## 5. 帧率自保措施（供主人真机判断）

| 措施 | 数值 | 位置 |
|---|---|---|
| 轮廓只处理相机附近 | **24 格**内 | `OverclockWireframe.RANGE` |
| 轮廓单帧上限 | **16 只** | `OverclockWireframe.MAX_ENTITIES` |
| 轮廓只提交**主模型** | 不遍历图层（最大的那块开销省掉） | `OverclockWireframe`（对比残影会跑图层） |
| 头顶面板距离/数量 | 20 格 / 12 个 | `RamHud.HOLO_RANGE` / `HOLO_MAX` |
| 头顶面板投影 | `try/catch`，API 不可用则整块不画 | `RamHud.drawHolo` |
| 扫描线步长 | 4px（不是逐像素） | `drawScanlines` |
| 二进制流 | 2 列 × 屏幕行数（不是整屏铺满） | `drawBinaryEdges` |
| 音效限频 | 嗡鸣 1000ms / 键盘 250ms / 心跳 700ms / 故障 500ms / 粒子 250ms | `RamHud.tickAudioAndParticles` |
| 无同步快照时不画 | `fresh()`：3 秒没收到包就整块不画 | `RamHud.render` |

## 6. 铁律遵守

- **渲染回调整体 try/catch**：`RamHud.onRenderGui`、`OverclockWireframe.onRenderLiving` 各自
  整段包 `try/catch(Throwable)`，异常只记一次日志（`Cyberware.LOGGER.warn`）。
- **本地玩家判定用渲染状态类型**：`state instanceof AvatarRenderState avatar && avatar.id == self.getId()`
  —— 没有任何坐标比身份。距离剔除用的是坐标，那是「远近」不是「身份」，且是帧率自保要求。
- **单点注入 require = 0**：本次**没有新增任何 Mixin**
  （`git status src/main/java/com/dsh/cyberware/mixin/client/` 为空），
  所以 `cyberware.mixins.json` **不需要登记**，也没有违反「归 coredev」的约定。
  既有客户端 mixin 的 `require = 0` 计数 = **16**（verify 命令输出）。
- **客户端不做扣费判定**：`RamClientState` 只存服务端同步下来的镜像。

## 7. ⚠ 未真机验证

**没有启动过游戏，所有视觉/音效/手感都没有在真机上验证过。** `compileJava` 通过只说明能编译。

需要主人真机确认：

1. RAM 条的位置/粗细/光晕强度（3px 在实际 GUI 缩放下是否太细）、数字可读性；
2. lerp 手感（消耗 9.0 / 恢复 2.2 这两个时间常数）；
3. 扫描线密度（4px）与二进制流的可读性/干扰程度；
4. **头顶全息面板的位置是否准确** —— 投影用 `GameRenderer.projectPointToScreen`，
   按 `RAM-SYSTEM-SPEC.md §7.2` 是 NDC，我按 `((x+1)/2*w, (1-y)/2*h)` 换算并用 `z ∈ [-1,1]` 剔除背面；
   若实机发现面板偏移或出现在背后，改的就是 `RamHud.drawHolo` 这一处；
5. 荧光轮廓的观感（是否足够像「线框化」）与帧率影响；
6. 5 个音效的音量与选择（`BEACON_ACTIVATE`/`UI_BUTTON_CLICK`/`NOTE_BLOCK_BASS`/`NOTE_BLOCK_BIT`/`BEACON_AMBIENT`/`NOTE_BLOCK_HAT`/`WARDEN_HEARTBEAT`，全部原版 + pitch）；
7. G 键与既有键位是否冲突。

## 8. P1 完成情况

| P1 项 | 状态 |
|---|---|
| RAM 不足警告态（转红闪 3 次 + 1 帧 RGB 错位 + 故障音） | ✅ 已做 |
| 濒死超频（血红流动 + 边缘红光 + 碎裂粒子 + 心跳音） | ✅ 已做 |
| 瘫痪（中央闪 RAM ACCESS FAILED） | ✅ 已做 |
| 原版音效 + pitch（激活 1.2 = 提升 20%） | ✅ 已做 |
| 帧率自保措施 | ✅ 已做并写入 §5 |

**未做**：t26 的破解 UI（锁定框 / 上传条 / 破解轮盘）—— captain 已明确取消本轮。
`HackPayload` 客户端 handler 只取了 `REJECTED` 的 note 做红字提示，其余 action 忽略。
