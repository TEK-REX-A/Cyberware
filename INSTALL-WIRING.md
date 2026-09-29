# 玩家义体存储接线（t4 交付物）

> 版本：0.3.12-Beta · 范围：安装 / 卸载 / 容量校验 / 查询 API
> 本文档描述**当前代码的真实状态**：哪些接线通了、哪些没通，一句不含糊。

---

## 1. 调用链

### 1.1 客户端 → 服务端有两条入口（服务端都是同一条实现）

```
【路径 A · 自定义 payload（UI 推荐）】
  CyberwareStationScreen（客户端）
      └─ ClientPacketDistributor.sendToServer(CyberwareActionPayload.install(menu.containerId, slot))
            └─ CyberwareNetwork.handleAction(payload, ctx)
                  └─ ctx.enqueueWork(...)  ← 主线程
                        └─ CyberwareStationService.install(serverPlayer, containerId, slot)

【路径 B · 原版按钮包（不用新包，按 id 编下标）】
  CyberwareStationScreen（客户端）
      └─ Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, buttonId)
            └─ ServerboundContainerButtonClickPacket
                  └─ ServerGamePacketListenerImpl#handleContainerButtonClick
                        └─ CyberwareStationMenu.clickMenuButton(player, buttonId)
                              └─ CyberwareStationService.install / uninstall / upgrade
```

两条路径最后都落到 **`com.dsh.cyberware.menu.CyberwareStationService`** ——
规则只有一份，不会分叉。

### 1.2 服务端安装的完整流程

```
CyberwareStationService.install(player, containerId, slotIndex)
  1. openStation(player, containerId)
       ├─ player.containerMenu instanceof CyberwareStationMenu ?
       ├─ menu.containerId == payload.containerId ?
       └─ menu.stillValid(player)（离开操作台 > 3 格即失败）
  2. 取下操作台第 slotIndex 个义体槽里的 ItemStack
  3. 解析：CyberwareItem#definition() → CyberwareDefinition
           CyberwareItem#dataOf(stack) → CyberwareData（缺组件回落默认）
  4. validateInstall(def, data, current)  ← 纯函数，不改状态
  5. player.setData(ModAttachments.INSTALLATION, current.with(def.id(), data))
  6. container.removeItem(slotIndex, 1) + setChanged() + menu.broadcastChanges()
  7. sendOverlayMessage 回执（成功/失败都有话）
```

**卸载**同理：先 `setData(... without(defId))` 清表，再用
`ModItems.CYBERWARE` + 数据组件重造物品，按
`操作台空槽 → 玩家背包 → 掉在脚边` 的顺序归还。

### 1.3 数据存储与同步

- 每个玩家一份 `CyberwareInstallation`（`Map<型号 id, CyberwareData>`），挂在
  `ModAttachments.INSTALLATION`（`AttachmentType`）上：`serialize` + `copyOnDeath` + `sync`。
- 同步**不需要额外补 S2C 包**，已从 NeoForge 26.1.2.109 源码确认：
  - `AttachmentHolder#setData` 内部调用 `syncData(type)`；
  - `Entity#syncData` → `AttachmentSync.syncEntityUpdate`，
    对 `ServerPlayer` 会**把自己也算进接收者**（玩家不 track 自己，源码里专门补了一个）；
  - 所以服务端 `setData` 之后，本人与被注视的客户端都会收到全量列表。
- 客户端读取用 `CyberwareInstallation.of(player)`（永不返回 null），
  没有附件的玩家走 `getExistingDataOrNull` 回落到 `EMPTY` —— 不会凭空给每个人挂空表。

---

## 2. 容量与冲突规则

| 规则 | 判定位置 | 说明 |
| --- | --- | --- |
| 玩家总容量上限 | `CyberwareInstallation.capacityLimit()` | 配置 `[capacity] defaultCapacity`，**缺省 100**；`enableCapacityLimit=false` 时返回 `Integer.MAX_VALUE`（调试用无限安装）；配置值非法时回落常量 `MAX_TOTAL_CAPACITY = 100` |
| 已用容量 | `CyberwareInstallation#usedCapacity()` | 逐件按**该件自己的稀有度**取 `variant.capacity()` 求和；查不到的型号（旧存档/已删型号）跳过，不炸界面 |
| 同型号重复 | `validateInstall` | 同一型号只能装一件（不提供「覆盖」语义） |
| 分类槽位件数 | `validateInstall` + `CyberwareSlot#maxCount()` | 操作系统 1 / 前额皮质 1 / 面部 1 / 神经系统 1 / 骨骼 2 / 手臂 2 / 腿部 2 / 循环系统 1 / 表皮 1 / 皮肤 1 |
| 稀有度必须存在 | `validateInstall` | `def.variantFor(data.rarity()) == null` 直接拒绝（不会拿低档数值顶替） |

容量计算示例：装了 `sandevistan_zetatech`（传说档 22）与 `kiroshi_optics`（普通档 3），
已用 = 25，上限 100 → 还能装 75。

---

## 3. 校验点清单（全部在服务端）

| # | 校验 | 失败回执 |
| --- | --- | --- |
| 1 | 调用者必须是 `ServerPlayer`（payload 路径） | 静默返回（客户端没有改数据的权限） |
| 2 | `clickMenuButton` 只处理 `ServerPlayer` | 返回 `false` |
| 3 | 服务端当前菜单是 `CyberwareStationMenu` 且 containerId 匹配 | 操作台没有打开 |
| 4 | `menu.stillValid(player)`（≤ 3 格） | 操作台没有打开 / 太远 |
| 5 | 槽位下标在 `0..IMPLANT_SLOT_COUNT-1` | 义体槽下标越界 |
| 6 | 槽里物品非空 | 第 N 个义体槽是空的 |
| 7 | 物品是 `CyberwareItem` | 这个物品不是义体 |
| 8 | 型号能在定义表里查到 | 未知型号（旧存档里被删掉的型号） |
| 9 | 该稀有度有变体 | 没有「X」这一档 |
| 10 | 没有装过同型号 | 同型号不能重复 |
| 11 | 分类槽位未满 | X 槽位已满（n/m） |
| 12 | 总容量够 | 植入容量不足：需要 X，只剩 Y（上限 100） |
| 13 | 卸载目标确实装在身上 | 身上没有装「X」 |
| 14 | 卸载时 `ModItems.CYBERWARE` 里还有该型号 | 只清记录，物品无法归还（打 warn 日志） |
| 15 | 首次访问附件用 `getExistingDataOrNull` | null 防御，返回 `EMPTY` |

所有失败都会 `sendOverlayMessage` 一句中文原因 —— 不再有「静默成功」。

---

## 4. 查询 API（给效果系统 / 义眼描边用）

`com.dsh.cyberware.core.CyberwareInstallation` 上的静态方法，**服务端、客户端都安全**：

```java
// 有没有装这件义体（被动效果判断的主力）
public static boolean has(Player player, String defId);

// 取某件的稀有度 + 等级；没装返回 null
public static CyberwareData dataOf(Player player, String defId);

// 整张表（永不返回 null；没装过义体就是 EMPTY）
public static CyberwareInstallation of(Player player);

// 容量读法
public static int usedCapacity(Player player);
public static int capacityLimit();          // 关掉限制时是 Integer.MAX_VALUE
public static int remainingCapacity(Player player);

// 实例方法
public List<String> orderedIds();           // 已装型号 id 的确定性顺序（字典序）
```

`orderedIds()` 是为「按钮只能带一个 int」那条路径准备的：两端算出来的「第 N 件」必须是同一件，
而 `Map.copyOf` 不保证迭代顺序，所以这里显式排序。

---

## 5. 改动清单

t4 的 7 个文件在原本声明范围内；`client/CyberwareStationScreen.java` 与 `CyberwareCommands.java`
是 **captain 正式授权的范围扩大**（当时唯一的编译阻塞，详见 §6.7）；
`client/CyberwareStationScreen.java` 的交互部分属于 **t9**（captain 授权该文件及 client/ 下确需改动）。

| 文件 | 改动 |
| --- | --- |
| `core/CyberwareInstallation.java` | 新增静态查询 API（`of/has/dataOf/usedCapacity/capacityLimit/remainingCapacity`）、`orderedIds()`、`MAX_TOTAL_CAPACITY` |
| `menu/CyberwareStationService.java` | **新增**：服务端权威的安装/卸载/升级实现 + 纯函数校验 `validateInstall` |
| `menu/CyberwareStationMenu.java` | 按钮 id 常量（`BUTTON_INSTALL_BASE=1`、`BUTTON_UNINSTALL_BASE=11`）+ `clickMenuButton` 接线（只认 `ServerPlayer`） |
| `network/CyberwareActionPayload.java` | 新增 `defId` 字段（卸载靠它定位）+ `install()/uninstall()/upgrade()` 工厂 |
| `network/CyberwareNetwork.java` | `handleAction` 真正派发到 service；`PROTOCOL_VERSION` 1 → 2 |
| `block/CyberwareStationBlockEntity.java` | 覆盖 `preRemoveSideEffects`，方块被破坏时把义体槽里的物品吐出来（否则卸载归还到槽里会随方块蒸发） |
| `client/BerserkHud.java` | 贴图 `berserk_militech.png` → `berserk_c4.png`（旧型号已删除） |
| `client/CyberwareStationScreen.java`（**t4 授权扩大**） | 默认展示型号 `SANDEVISTAN_ZETATECH` → `SANDEVISTAN_C4`（旧常量已被定义表重写删除） |
| `CyberwareCommands.java`（**t4 授权扩大**） | `/cyberware sandevistan` 同上改为 `SANDEVISTAN_C4` |
| `client/CyberwareStationScreen.java`（**t9**） | 安装/卸载按钮 + `sendToServer(CyberwareActionPayload)`；快捷栏挪进面板；标题栏容量/状态提示；hover 坐标系修正；详情内容截断不再压按钮 |


---

## 6. 仍未接线的地方（明确列出，不含糊）

1. ~~**客户端触发点仍然不存在**~~ → **已完成（t9）**，见下。

   **按钮映射（详情面板底部两个按钮，坐标面板相对）**

   | 按钮 | 位置 | 触发条件（灰掉 = 不可点） | 发出的请求 |
   | --- | --- | --- | --- |
   | 安装 | `x=160, y=158, 74x14` | 选中型号在操作台槽里有对应物品，且本地预检通过 | `CyberwareActionPayload.install(containerId, 槽位下标)` |
   | 卸载 | `x=240, y=158, 74x14` | 选中型号已装在身上 | `CyberwareActionPayload.uninstall(containerId, 型号 id)` |
   | —（未做） | — | 升级 | 没有按钮；`clickMenuButton(0)` / `Action.UPGRADE` 仍是「尚未接线」回执 |

   两条请求都走既有通道（`ClientPacketDistributor.sendToServer`），**没有新造协议**，
   服务端仍是唯一写数据的一方（`CyberwareStationService`）。全仓库现有 2 处
   `sendToServer(CyberwareActionPayload...)`（安装 / 卸载各一），此前是 0 处。

   **物品怎么进义体槽（t9 一起修的）**：`CyberwareStationMenu` 把玩家背包整体放在
   `(-3000, -3000)`，界面里根本点不到 —— 义体槽永远空着，安装按钮就永远点不动。
   t9 在 Screen 里把**快捷栏 9 格**挪到面板底部（`y=204`）：
   26.x 的 `Slot.x/y` 是 `final`，所以是换一个「同 container、同容器内下标、只改坐标」的新 `Slot`
   顶替，`index` 照抄 → 槽位在菜单里的位置不变、网络包下标不变、服务端完全无感（服务端只认下标）。
   流程：把义体放到快捷栏 → 开操作台 → 点快捷栏那格拿起 → 点底部义体槽放下 →
   点该义体槽（顺带把详情面板切到它）→ 点「安装」。

   **失败回执现状（如实说明）**

   - 服务端每一种拒绝都有 `sendOverlayMessage`（动作栏）中文回执，**这部分是有的**。
   - 但操作台界面打开时，动作栏被面板的半透明底盖住，玩家基本看不见 —— 所以 t9 在标题栏右侧
     加了状态提示位：点击按钮时先跑**服务端同一份** `validateInstall`（纯函数，两端可调用）做本地预检，
     容量不足 / 槽位冲突 / 同型号重复 / 槽里没这件义体 / 身上没装这件 —— 都能立刻在界面上看到原因；
     预检通过则显示「已发送安装请求」。标题栏常驻显示 `容量 已用/上限`。
   - **没有做**：把服务端的**最终**裁决（比如预检通过但服务端仍拒绝的竞态）镜像回界面 ——
     那需要一个新的 S2C 包（本任务被要求不新造协议），所以这种情况下玩家只能靠动作栏/聊天看到。
     这是已知缺口，不是「假装做了」。

2. **升级（UPGRADE）没有实现。** 只接了入口并明确回执「升级尚未接线」，
   材料扣除（`upgrade.costEmerald/costScrap/costNetheriteScrap`）与成功率
   （`upgrade.successRate`）属于需求书第六步，本任务没做。
3. **没有「同型号换档」**：已装同型号直接拒绝，不提供替换/覆盖。
4. **旧存档里被删除型号的处理**：容量计算时跳过、卸载时只清记录（物品不归还），
   不提供 id 迁移表。
5. **运行时未实测 —— 未真机验证（重要）**：本环境没有跑起 Minecraft（无客户端/服务端运行时），
   t4 与 t9 都只做到**编译级 + 源码/字节码级**验证。以下都必须进游戏点一次才能最终确认：
   - 安装/卸载的手感与延迟（一次请求 → 服务端 1 tick 后同步回来）；
   - 详情面板底部两个按钮的**坐标/命中区**是否与面板视觉对齐（`BTN_Y=158`，详情内容超出会截断）；
   - 快捷栏 9 格挪到 `y=204` 后的观感（与义体槽条相邻，底边留 3px）与点击是否顺畅；
   - 标题栏状态提示在手机上是否够宽（超长会按宽度截断）；
   - 客户端本地预检与服务端裁决在同一次点击里是否始终一致；
   - t9 顺带修的 hover 坐标系问题（见 §6.8）在真实分辨率下是否表现正确。
   **没有任何一项在真机上跑过**，报告里也不声称跑过。

6. **`CyberwareInstallation` 的同步是「全量列表」**：每次装卸都发整张表。
   义体数量是十位数量级，代价可以忽略；真要优化再加增量包。
7. **~~编译现状~~ → 已解决（captain 授权的范围扩大）**：
   numbersmith 把定义表重写为官方 123 条时删掉了旧占位常量 `SANDEVISTAN_ZETATECH`，
   而下面两处仍在引用它，导致工作区 `gradle compileJava` 一度红灯：
   - `src/main/java/com/dsh/cyberware/client/CyberwareStationScreen.java`（默认展示型号）
   - `src/main/java/com/dsh/cyberware/CyberwareCommands.java`（`/cyberware sandevistan`）

   这两处不在任何成员的声明范围内（captain 认定的协调缺口）。经 captain **正式授权扩大 t4 范围**后
   已改为后继型号 `CyberwareDefinitions.SANDEVISTAN_C4`（与 `berserk_militech` → `berserk_c4` 同思路）；
   captain 裁决**不采纳**「在定义表补旧常量别名」的方案（不把废弃旧名留在 API 上）。
   改后工作区 `JAVA_HOME=... gradle compileJava --no-daemon` → **BUILD SUCCESSFUL in 2m12s**。
   事前的隔离验证证据保留在 `/tmp/t4-verify`：与工作区源码逐字节一致、只改这两行即编译通过，
   证明这两行曾是**唯一**阻塞、且 t4 自身代码干净。

8. **t9 顺带修的两个既有小问题（都在同一个 Screen 文件里）**：
   - **hover 坐标系错位**：`extractLabels` 里绘制坐标是「面板相对」（父类已 translate），
     但传进来的 `mouseX/mouseY` 是**屏幕绝对坐标**（26.x `extractContents` 源码为证）。
     原来用 `isInside(mouseX, mouseY, 面板坐标...)` 判 hover，等于拿两个坐标系比 ——
     分类树/型号行的悬停高亮实际偏了 `(leftPos, topPos)`（点击不受影响，那里用的是绝对坐标）。
     已改用父类 `isHovering(int,int,int,int,double,double)`（它自己会减 `leftPos/topPos`）。
   - **详情内容压到按钮上**：原来数值/变体列表会一路画到面板外。现在以 `BTN_Y - 4` 为下限截断，
     超出画省略号。这两处都属**未真机验证**。

