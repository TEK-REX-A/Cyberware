# 义体激活链路改造（t13 交付物）

> 版本：0.3.12-Beta · 范围：激活协议 + 服务端「已安装表激活」
> 本文档描述**当前代码的真实状态**：做了什么、怎么校验、哪些留给 t14，一句不含糊。

---

## 1. 为什么要改

0.3.12 之前只有一条激活路径：

```
client/CyberwareClient.onClientTick
  └─ CyberwareKeys.ACTIVATE.consumeClick()          （V 键）
        └─ sendToServer(new ActivatePayload())      （无参！）
              └─ CyberwareNetwork.handleActivate
                    └─ CyberwareAbilities.activateHeld(player)   ← 只看**主手物品**
```

后果：义体一旦装进操作台（进了玩家已安装表），**按 V 什么也不会发生** ——
因为手上没有那件物品了。而 `CyberwareInstallation` 那张表明明已经存着「你装了斯安威斯坦」。

主人拍板的方案：**V 键保持不变（手持激活）+ 新增 R 键轮盘**，轮盘里只列「有主动效果的已安装义体」。
t13（本任务）负责**协议 + 服务端**这条腿；轮盘 UI 在 t14（opticsdev）。

---

## 2. 协议：新旧对比

| | 旧（0.3.11 及以前） | 新（0.3.12 / t13） |
| --- | --- | --- |
| 类型 | `record ActivatePayload()` 无参 | `record ActivatePayload(String defId)` |
| 编码 | `StreamCodec.unit(new ActivatePayload())` | `StreamCodec.composite(ByteBufCodecs.STRING_UTF8, ActivatePayload::defId, ActivatePayload::new)` |
| 语义 | 只能是「激活手持」 | 空串 `ActivatePayload.HELD` = 手持；非空 = 指定型号 |
| 发送点 | `new ActivatePayload()` | `new ActivatePayload(ActivatePayload.HELD)`（V 键，`CyberwareClient`） |
| 协议版本 | `PROTOCOL_VERSION = "1"` | **`"3"`** |

关于版本号：`CyberwareNetwork.PROTOCOL_VERSION` 被 `station_action` 与 `activate` 两个 channel 共用，
t4 因 `CyberwareActionPayload` 加 `defId` 升到 `"2"`，本次 `ActivatePayload` 结构变化再升到 `"3"`。
NeoForge 的版本号是**按 channel 协商**的（`NetworkPayloadSetup` 里 `Map<Identifier, NetworkChannel>`），
所以 `CyberwareClient` 里那两个 S2C 包的 `"1"` 不受影响；两端用的是同一个常量，一致。

便利工厂（给 t14 用）：

```java
ActivatePayload.held()            // = HELD，手持激活
ActivatePayload.of("sandevistan_c4")   // 指定型号
```

`defId` 允许传 `null`：构造器里统一成空串（= 手持），不会退化成「激活任意义体」。

---

## 3. 服务端分支

```java
// CyberwareNetwork.handleActivate
context.enqueueWork(() -> {
    if (!(context.player() instanceof ServerPlayer serverPlayer)) {   // 再挡一道：客户端实体不能执行
        LOGGER.debug(...); return;
    }
    if (payload.defId().isEmpty()) {
        CyberwareAbilities.activateHeld(serverPlayer);            // V 键，行为与以前完全一致
        return;
    }
    CyberwareAbilities.activateInstalled(serverPlayer, payload.defId());   // R 键轮盘
});
```

`playToServer` 的 handler 本来就只在服务端跑；这里再显式判 `ServerPlayer`，
是为了让「客户端同步下来的附件副本」永远不可能成为执行依据。

---

## 4. `activateInstalled` 的校验路径

```java
public static boolean activateInstalled(Player player, String defId) {
    1. player == null || defId == null || defId.isBlank()  →  return false
    2. CyberwareData installed = CyberwareInstallation.dataOf(player, defId);
           installed == null  →  log.debug + return false        // 没装：不抛异常，也不给效果
    3. CyberwareDefinition def = CyberwareDefinitions.byId(defId);
           def == null        →  log.warn  + return false        // 旧存档里被删掉的型号
    4. return activate(player, def, installed);                  // ← 复用现有实现，分支逻辑零复制
}
```

- **复用而非复制**：字节码层面 `activateInstalled` 与 `activateHeld` 都调用同一个
  `CyberwareAbilities.activate(Player, CyberwareDefinition, CyberwareData)`
  （`javap -c` 可见两处 `invokestatic activate:(...)`），时间减缓 / 狂暴 / 待扩展的网络接入仓分支
  仍然只有一份。
- `dataOf` 读的是**服务端玩家身上那份 Attachment**（`ModAttachments.INSTALLATION`），
  即 `CyberwareInstallation.of(player).installed().get(defId)`；第一次安装之前表不存在时
  `of()` 回落 `EMPTY`（`getExistingDataOrNull` + null 防御），所以「从没装过」也不会炸。
- 稀有度与等级取的是**装上去那一刻存下的** `CyberwareData`（不是客户端报的），
  所以「拿普通档数据冒充神话档」这条路是不通的。

---

## 5. 「客户端不可信」到底意味着什么

| 客户端能做 | 客户端不能做 |
| --- | --- |
| 说「我想激活手持这件」（空串） | 决定用哪一档稀有度/等级 —— 服务端读自己存的那份 |
| 说「我想激活 `xxx`」（defId） | 让服务端相信自己**拥有** `xxx` —— 查不到就 `return false` |
| —— | 触发任何没有装在自己身上的效果 |

也就是说 `defId` 是**意图声明**，不是权限凭证：伪造一个没装的 defId，服务端只会记一条
debug 日志然后什么都不做。数值强度、是否拥有、能不能触发（冷却/状态）全部在服务端判定。

---

## 6. R 键（t13 只注册键位）

```java
// client/CyberwareKeys
public static final KeyMapping RADIAL = new KeyMapping(
        "key.cyberware.radial", InputConstants.Type.KEYSYM, InputConstants.KEY_R, CATEGORY);

// client/CyberwareClient.onRegisterKeys
event.register(CyberwareKeys.RADIAL);      // 只注册；**没有** consumeClick 触发逻辑
```

- `CyberwareClient.onClientTick` 里**只有 V 键**那一行（改成 `new ActivatePayload(ActivatePayload.HELD)`），
  R 键的触发逻辑一行都没写 —— 那是 t14 的活。
- 按键显示名需要 lang 条目 `key.cyberware.radial`（t15 / archivist 负责）。

---

## 7. 未完成 / 留给后续的部分（明确列出，不含糊）

1. **R 键的触发逻辑**（`CyberwareKeys.RADIAL.consumeClick()` → 打开轮盘）：**未做**，留给 t14（opticsdev）。
   现在按 R 不会有任何反应（键位已注册、可在「选项 → 按键」里看到并可改键）。
2. **轮盘 UI**（`client/CyberwareRadialScreen.java`、列出「有主动效果」的已安装义体、
   选中后发 `ActivatePayload.of(defId)`）：**未做**，t14。
   t13 只保证「发这条包，服务端能正确响应」。
3. **「有主动效果」的判定标准**：t14 需要自己定（例如只看 `CyberwareDefinition.active()`，
   或者按 `Stats` 里是否存在 `time_slow` / `berserk_` 前缀等）。
   t13 没有新增筛选 API —— `CyberwareInstallation.orderedIds()` + `CyberwareDefinitions.byId()`
   已经够用，若 t14 需要更顺手的入口可以再提。
4. **效果层面的未实现项照旧**（不是本任务范围）：网络接入仓等主动效果仍是
   「暂未实现主动效果」回执；升级（材料+成功率）未实现。
5. **未真机验证**：本环境跑不起 Minecraft（无客户端/服务端运行时），
   本次只做到**编译级 + 字节码级**验证。必须在游戏里确认的：
   - V 键手持激活行为与 0.3.11 完全一致；
   - 客户端与服务端在同一 jar 下不会因 `PROTOCOL_VERSION = "3"` 协商失败（连不上会直接报协议不匹配）；
   - 装上斯安威斯坦后，用 `ActivatePayload.of("sandevistan_c4")` 触发是否真的生效（t14 做完轮盘才能点）；
   - 伪造未安装的 defId 时确无效果（只能看服务端日志 `[cyberware] 拒绝激活 ...`）。
   **没有任何一项在真机上跑过**，报告里也不声称跑过。
