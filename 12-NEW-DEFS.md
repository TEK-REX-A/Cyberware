# 12-NEW-DEFS.md —— 12 件无出处义体的定义（t7 / numbersmith）

工作区：`/root/mod26/cyberware`　本任务改动：`src/main/java/com/dsh/cyberware/core/CyberwareDefinitions.java`（只此一个文件）

`register` 计数：**生效 135**（t3 后 123 → +12）、含 19 条注释块合计 154；`cyberware:item/` **154**（142 + 12）。

---

## 1. 核心声明（先读）

> **这 12 条没有官方数据来源。除「中文名」与「id」两项外，槽位、稀有度、容量、数值、描述全部是待确认初值，一律标 TODO，等主人确认或推翻。**

依据（可复核）：captain 通读 `/sdcard/DSH/义体/ID对照.txt`（127 行全文），这 12 个中文名都不在其中；`/sdcard/DSH/义体/待插入-123条定义.txt` 与 `/sdcard/DSH/义体/描述.txt`（374 行）里也没有对应条目。本报告未从任何外部来源补数据。

本文档里出现的**每一个数字/字段**都带 `（依据:…）` 或 `（TODO:…）` 标注；无标注的数值即视为不合格。

---

## 2. id 生成规则（依据）与 id 清单（给 archivist）

**规则（依据）**：英文小写下划线，与现存 142 个 id 同风格（例：现存 `metabolic_*` 无、`cyberdeck_*` 有；`描述.txt` 的官方条目亦然）。取自中文名的英文对应词，一个中文名一个 id，不做缩写以免与现存 id 撞名。

**id 定稿清单（archivist 按此放图，勿改）**：

```
metabolic_editor            <- 代谢编辑器.png
full_resistance             <- 全幅抵抗.png
reverse_inductor            <- 反向电感.png
synthetic_lung              <- 合成肺叶.png
grounding_plating           <- 接地镀层.png
thermal_converter           <- 热能转化器.png
bioplastic_vessels          <- 生物塑料血管.png
dermal_weave                <- 真皮上编束.png
nano_relay                  <- 纳米继电器.png
detoxifier                  <- 解毒器.png
edge_enhancement_system     <- 边缘增强系统.png
fireproof_coating           <- 防火涂层.png
```

**id 与 archivist 的产物已对上**（依据：只读核验，见 §6）：上面 12 个 id 在仓库里**各自已有 3/3 件**资源 —— `textures/item/<id>.png`（都是 32x32）、`models/item/<id>.json`、`items/<id>.json`，且两个 json 的 `layer0` / 模型引用都写着 `cyberware:item/<id>`，与本表「贴图路径」列逐字相同。即 id 定稿与本仓库现有资源一致，未出现第二种写法。

12 张素材均已核验存在于 `/sdcard/DSH/义体/`（依据：逐个 `stat` 过字节大小，见 §6）。每张需要的三件套路径（依据：本表 id 规则，与现存条目同构）：

- 贴图 `src/main/resources/assets/cyberware/textures/item/<id>.png`（32x32，archivist 负责）
- 模型 `src/main/resources/assets/cyberware/models/item/<id>.json`
- 物品 `src/main/resources/assets/cyberware/items/<id>.json`
- 语言键 `item.cyberware.<id>`

---

## 3. 主表（每格均带标注）

| 中文名 | id | 槽位 | 稀有度 | 容量 | 贴图路径 | 备注 |
|---|---|---|---|---|---|---|
| 代谢编辑器（依据:素材名） | `metabolic_editor`（依据:§2 规则） | `CIRCULATORY` 循环系统（TODO:无上游来源，推理见 §4.1） | `LEGENDARY`（TODO:占位，无来源） | `8`（TODO:占位，无来源） | `cyberware:item/metabolic_editor`（依据:id 规则） | TODO：待主人确认槽位/稀有度/容量（见 §4/§5） |
| 全幅抵抗（依据:素材名） | `full_resistance`（依据:§2 规则） | `INTEGUMENTARY` 表皮系统（TODO:无上游来源，推理见 §4.2） | `LEGENDARY`（TODO:占位，无来源） | `8`（TODO:占位，无来源） | `cyberware:item/full_resistance`（依据:id 规则） | TODO：待主人确认槽位/稀有度/容量（见 §4/§5） |
| 反向电感（依据:素材名） | `reverse_inductor`（依据:§2 规则） | `CIRCULATORY` 循环系统（TODO:无上游来源，推理见 §4.3） | `LEGENDARY`（TODO:占位，无来源） | `8`（TODO:占位，无来源） | `cyberware:item/reverse_inductor`（依据:id 规则） | TODO：待主人确认槽位/稀有度/容量（见 §4/§5） |
| 合成肺叶（依据:素材名） | `synthetic_lung`（依据:§2 规则） | `CIRCULATORY` 循环系统（TODO:无上游来源，推理见 §4.4） | `LEGENDARY`（TODO:占位，无来源） | `8`（TODO:占位，无来源） | `cyberware:item/synthetic_lung`（依据:id 规则） | TODO：待主人确认槽位/稀有度/容量（见 §4/§5） |
| 接地镀层（依据:素材名） | `grounding_plating`（依据:§2 规则） | `INTEGUMENTARY` 表皮系统（TODO:无上游来源，推理见 §4.5） | `LEGENDARY`（TODO:占位，无来源） | `8`（TODO:占位，无来源） | `cyberware:item/grounding_plating`（依据:id 规则） | TODO：待主人确认槽位/稀有度/容量（见 §4/§5） |
| 热能转化器（依据:素材名） | `thermal_converter`（依据:§2 规则） | `CIRCULATORY` 循环系统（TODO:无上游来源，推理见 §4.6） | `LEGENDARY`（TODO:占位，无来源） | `8`（TODO:占位，无来源） | `cyberware:item/thermal_converter`（依据:id 规则） | TODO：待主人确认槽位/稀有度/容量（见 §4/§5） |
| 生物塑料血管（依据:素材名） | `bioplastic_vessels`（依据:§2 规则） | `CIRCULATORY` 循环系统（TODO:无上游来源，推理见 §4.7） | `LEGENDARY`（TODO:占位，无来源） | `8`（TODO:占位，无来源） | `cyberware:item/bioplastic_vessels`（依据:id 规则） | TODO：待主人确认槽位/稀有度/容量（见 §4/§5） |
| 真皮上编束（依据:素材名） | `dermal_weave`（依据:§2 规则） | `INTEGUMENTARY` 表皮系统（TODO:无上游来源，推理见 §4.8） | `LEGENDARY`（TODO:占位，无来源） | `8`（TODO:占位，无来源） | `cyberware:item/dermal_weave`（依据:id 规则） | TODO：待主人确认槽位/稀有度/容量（见 §4/§5） |
| 纳米继电器（依据:素材名） | `nano_relay`（依据:§2 规则） | `NERVOUS_SYSTEM` 神经系统（TODO:无上游来源，推理见 §4.9） | `LEGENDARY`（TODO:占位，无来源） | `8`（TODO:占位，无来源） | `cyberware:item/nano_relay`（依据:id 规则） | TODO：待主人确认槽位/稀有度/容量（见 §4/§5） |
| 解毒器（依据:素材名） | `detoxifier`（依据:§2 规则） | `CIRCULATORY` 循环系统（TODO:无上游来源，推理见 §4.10） | `LEGENDARY`（TODO:占位，无来源） | `8`（TODO:占位，无来源） | `cyberware:item/detoxifier`（依据:id 规则） | TODO：待主人确认槽位/稀有度/容量（见 §4/§5） |
| 边缘增强系统（依据:素材名） | `edge_enhancement_system`（依据:§2 规则） | `NERVOUS_SYSTEM` 神经系统（TODO:无上游来源，推理见 §4.11） | `LEGENDARY`（TODO:占位，无来源） | `8`（TODO:占位，无来源） | `cyberware:item/edge_enhancement_system`（依据:id 规则） | TODO：待主人确认槽位/稀有度/容量（见 §4/§5） |
| 防火涂层（依据:素材名） | `fireproof_coating`（依据:§2 规则） | `INTEGUMENTARY` 表皮系统（TODO:无上游来源，推理见 §4.12） | `LEGENDARY`（TODO:占位，无来源） | `8`（TODO:占位，无来源） | `cyberware:item/fireproof_coating`（依据:id 规则） | TODO：待主人确认槽位/稀有度/容量（见 §4/§5） |

槽位分布（依据:上表）：`CIRCULATORY` 6 条、`INTEGUMENTARY` 4 条、`NERVOUS_SYSTEM` 2 条，合计 12。

---

## 4. 逐条槽位推理（主人可直接确认或推翻）

每条给出三项：**语义依据**（只看中文名的字面意思）、**本文件同族参照**（现存定义里的同类，槽位可在 `CyberwareDefinitions.java` 里逐条核对）、**可推翻方向**（另一种合理解读对应的槽位）。

### 4.1 代谢编辑器 → `CIRCULATORY`（循环系统）
- 语义依据：「代谢」是能量与物质的全身性交换，不专属某个局部器官。
- 同族参照：`biomonitor`「生物监测」与 `blood_pump`「活血泵」都在 `CyberwareSlot.CIRCULATORY`。
- 可推翻方向：若主人认为「编辑器」是额皮质类命名（同族 `memory_boost`「内存加强」、`smart_storage`「蓄电缓冲」都在 `FRONTAL_CORTEX`），改 `FRONTAL_CORTEX`。

### 4.2 全幅抵抗 → `INTEGUMENTARY`（表皮系统）
- 语义依据：「抵抗」= 抗性/减伤；「全幅」= 全身覆盖 → 体表防护。
- 同族参照：`charge_system`「拒敌防护」、`boring_plating`「皮下护甲」、`nano_tech_plates`「纳米镀层」都在 `CyberwareSlot.INTEGUMENTARY`。
- 可推翻方向：若「全幅抵抗」指状态抗性（中毒/流血一类），可改 `CIRCULATORY`（同族 `viral_venom`「黑曼巴」）。

### 4.3 反向电感 → `CIRCULATORY`（循环系统）
- 语义依据：「电感」是电路元件，「反向」指反向电流/反向感应 → 电力类植入体。
- 同族参照：`discharge_connector`「反馈电路」在 `CyberwareSlot.CIRCULATORY`。
- 可推翻方向：若主人认为它是手部电力件（同族 `micro_generator`「微发电机」在 `ARMS`），改 `ARMS`。

### 4.4 合成肺叶 → `CIRCULATORY`（循环系统）
- 语义依据：肺司呼吸与血液氧合，与循环系统直接相接。
- 同族参照：`biomonitor`「生物监测」、`blood_pump`「活血泵」都在 `CyberwareSlot.CIRCULATORY`。
- 可推翻方向：现存槽位里没有更贴近「呼吸器官」的槽位；`INTEGUMENTARY`（表皮）与肺无关，不建议。

### 4.5 接地镀层 → `INTEGUMENTARY`（表皮系统）
- 语义依据：「镀层」= plating，是体表/皮下镀层；「接地」= 泄放电流。
- 同族参照：本文件三个带「镀层/plating」的定义 `boring_plating`「皮下护甲」、`nano_tech_plates`「纳米镀层」、`weird_tanky_plating`「全域覆盖」都在 `CyberwareSlot.INTEGUMENTARY`。
- 可推翻方向：若「接地」取纯电路义（而非体表镀层），可改 `CIRCULATORY`（同族 `discharge_connector`「反馈电路」）。

### 4.6 热能转化器 → `CIRCULATORY`（循环系统）
- 语义依据：「转化器」是把热能转成可用能量（转换，不是单纯抗热），属能量/循环类。
- 同族参照：`cyber_rotors`「微型转子」、`discharge_connector`「反馈电路」都在 `CyberwareSlot.CIRCULATORY`。
- 可推翻方向：若「热能」侧重耐热/抗热，改 `INTEGUMENTARY`（同族 `nano_tech_plates`「纳米镀层」等防护类）。

### 4.7 生物塑料血管 → `CIRCULATORY`（循环系统）
- 语义依据：「血管」字面就是血液循环管路。
- 同族参照：`blood_pump`「活血泵」在 `CyberwareSlot.CIRCULATORY`。
  （同时记录一个反例以免误判：`blood_depleter`「疼痛置换」名字带 blood 却在 `INTEGUMENTARY`，所以本表不以「含 blood 字样」判定，而以「血管/泵=循环管路」判定。）
- 可推翻方向：无。

### 4.8 真皮上编束 → `INTEGUMENTARY`（表皮系统）
- 语义依据：「真皮」= dermis，属皮肤层。
- 同族参照：`adaptive_stem_cells`「细胞适配」、`iconic_chiton`「几质丁壳」、`boring_plating`「皮下护甲」都在 `CyberwareSlot.INTEGUMENTARY`。
- 可推翻方向：无。

### 4.9 纳米继电器 → `NERVOUS_SYSTEM`（神经系统）
- 语义依据：「继电器」是信号中继元件；植入体内即神经/信号中继。
- 同族参照：`neo_fiber`「纳米纤维」与 `synaptic_accelerator`「突触加速器」都在 `CyberwareSlot.NERVOUS_SYSTEM`。
- 可推翻方向：若「继电器」取纯电路义，可改 `CIRCULATORY`（同族 `discharge_connector`「反馈电路」）。

### 4.10 解毒器 → `CIRCULATORY`（循环系统）
- 语义依据：解毒要靠血液把毒素带走。
- 同族参照：`viral_venom`「黑曼巴」（毒素向）与 `biomonitor`「生物监测」都在 `CyberwareSlot.CIRCULATORY`。
- 可推翻方向：无。

### 4.11 边缘增强系统 → `NERVOUS_SYSTEM`（神经系统）
- 语义依据：「边缘增强」按图像/信号边缘强化理解 → 感知增强。
- 同族参照：`visual_cortex_support`「视觉皮质支持」与 `trouble_finder`「瞬时感知」都在 `CyberwareSlot.NERVOUS_SYSTEM`。
- 可推翻方向：若「边缘」取「边缘系统（limbic system）」之义，则属大脑 → `FRONTAL_CORTEX`（同族 `mechatronic_core`「机电核心」）。这两种读法请主人择一。

### 4.12 防火涂层 → `INTEGUMENTARY`（表皮系统）
- 语义依据：「涂层」= coating，是体表涂层，防火/耐热。
- 同族参照：`nano_tech_plates`「纳米镀层」、`boring_plating`「皮下护甲」都在 `CyberwareSlot.INTEGUMENTARY`。
- 可推翻方向：无。

---

## 5. 逐字段标注汇总（12 条同规则）

| 字段 | 值 | 标注 |
|---|---|---|
| 中文名 | 见 §3 | 依据：素材文件名逐字相同（`/sdcard/DSH/义体/<名>.png` 已核验存在） |
| id | 见 §2 清单 | 依据：§2 生成规则；已核验 12 个 id 与现存 142 个 id 无重复 |
| 贴图路径 | `cyberware:item/<id>` | 依据：与现存 142 条完全同构（`<id>` 即本表 id） |
| 槽位 | `CIRCULATORY` / `INTEGUMENTARY` / `NERVOUS_SYSTEM` | **TODO**：无上游来源；推理见 §4，暂按素材名语义 |
| 稀有度 | `LEGENDARY` | **TODO**：无上游来源；占位值。取 `LEGENDARY` 的依据仅是「占位同规格」——t7 生效的 135 条里 `LEGENDARY` 112 条，是唯一多数档（`MYTHIC` 10、`RARE` 5、`UNCOMMON` 4、`EPIC` 3、`COMMON` 1） |
| 容量 | `8` | **TODO**：无上游来源；占位值。取 `8` 的依据仅是「沿用占位」——t3 编辑前那 123 条新定义的占位容量全部是 `8`（基线 `/tmp/backup-defs-20260929-000424.java` 第 328 行起逐条可核） |
| stats（数值） | 空 `stats()` | **TODO**：无数值，等主人给 |
| 描述串 | `<中文名>。TODO(数值与描述待补)` | **TODO**：沿用 t3 时 123 条的占位写法 |
| `active` 标志 | `true` | 依据：与 t7 生效的全部 135 条一致（135/135 都是 `true`），未额外裁决 |

**唯一性的核验**（依据:脚本）：生效 135 + 注释 19 = 154 个 id 全部互不相同，且 12 个新 id 不在其中任何一个已用 id 里。

---

## 6. 验证记录（未跑 gradle）

```
$ grep -c '^    public static final CyberwareDefinition.*register(new CyberwareDefinition(' \
      src/main/java/com/dsh/cyberware/core/CyberwareDefinitions.java
135                     # 生效 register：t3 后 123 → +12 = 135
$ grep -c 'register(new CyberwareDefinition(' 同上文件
154                     # 含 19 条「待素材」注释块；123+19+12 = 154
$ grep -c 'cyberware:item/' 同上文件
154                     # 142 + 12
$ javac -d /tmp/ns/out -encoding UTF-8 <CyberwareDefinition+CyberwareRarity+CyberwareSlot+CyberwareDefinitions>
JAVAC OK                # 语法通过，Stats 键名全部解析成功（这是 javac，不是 gradle）
$ 12 个 id 的资源只读核验（PNG 头解析 + json 解析）
全部命中：12/12 张 textures/item/<id>.png 都是 32x32；12/12 个 models/item/<id>.json 与
12/12 个 items/<id>.json 可解析，且 layer0 引用与 <id> 逐字一致
$ 12 张素材逐个 stat
全部存在：代谢编辑器.png 29213B / 全幅抵抗.png 31683B / 反向电感.png 39027B / 合成肺叶.png 47498B /
接地镀层.png 34437B / 热能转化器.png 40580B / 生物塑料血管.png 38734B / 真皮上编束.png 43549B /
纳米继电器.png 23499B / 解毒器.png 25431B / 边缘增强系统.png 28827B / 防火涂层.png 40611B
```

`javac` 用 /usr/lib/jvm/java-21-openjdk-arm64 的 `javac 21.0.12`，只编译 4 个同包文件。**未跑 gradle**（按任务要求让给 inspector 统一全量构建）。

编辑前备份：`/tmp/backup-defs2-20260929-034555.java`（依据:实际 `cp` 过，84405B）。

---

## 7. 与 t3 的关系（本轮未动）

- t3 注释掉的 19 条「无素材」定义**保持注释状态**，一行未改（依据:注释块计数仍为 19）。
- t3 填好的 123 条数值**一字未改**（依据:本轮只在文件末尾的定义区之后、`工具方法` 注释之前**追加**了 12 块，以及 3 行区块说明注释）。
- 本任务未触碰 `resources/**`、`mixin/**`、`client/**`、`menu/**`、`block/**`（依据:`git status` 里这些路径本轮无新改动由本任务产生）。
- 阅读提示：`DEFS-VALUES.md`（t3 交付）记录的是 **t3 结束时**的状态（保留 123 条，生效 register 123）；加上本任务新增的 12 条后，文件当前生效 register 为 **135**、含 19 条注释块合计 154。两份文档不矛盾，是同一文件在两个时间点的快照。

---

## 8. 待主人裁决清单（一句话版）

1. 12 条**槽位**：见 §4 每条「可推翻方向」，其中 4.1 / 4.2 / 4.3 / 4.5 / 4.6 / 4.9 / 4.11 各有一个备选槽位。
2. 12 条**稀有度**（现占位 `LEGENDARY`）与**容量**（现占位 `8`）。
3. 12 条**数值与描述**：现全为 `TODO(数值与描述待补)`。
