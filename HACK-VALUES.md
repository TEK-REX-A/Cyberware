# HACK-VALUES.md —— 五条快速破解 + 脑机超频的数值调研（t30 / numbersmith）

工作区：`/root/mod26/cyberware`　**本任务只写这一份文档，未改任何 `.java`**（替换由 t31 的 coredev 做）。

版本背景：0.5.0-Beta（`STEP3-CONTRACT.md` §2：占位值一律换成本文档的数字）。
代码里的占位点：`core/HackLibrary.java`（5 条破解的 RAM/上传/效果）+ `core/CyberwareStats.java:33-34`（超频时长/冷却两张表）。

---

## 0. 结论摘要（先读这一节）

| 项 | 有可靠 2077 出处？ | 结论 |
|---|---|---|
| 过热 RAM / 上传 / 点燃时长 / 破甲 | **是**（gamestegy，实测抓到全文） | 见 §1 表 |
| 短路 RAM / 上传 / 伤害 | **是**（gamestegy，实测抓到全文） | 见 §1 表 |
| 突触熔断 RAM / 上传 / 效果时长 | **否**（3 个来源站点本次抓取全部失败） | §4 需主人裁决 |
| 武器故障 RAM / 上传 / 时长 | **否**（同上） | §4 需主人裁决 |
| 系统重置 RAM / 上传 / 时长 | **否**（同上）。但**邮件IX 明写「消耗极高，如 8 点 RAM」** | RAM 用 house 锚点 8；其余 §4 |
| 脑机超频 时长/冷却 | **否**。2077 没有「按接入仓稀有度分档」的机制，也没有可查到的具体秒数 | §4 需主人裁决 |

**最重要的一条判断**：2077 的 RAM 成本是**满档（T5++）**数值 —— 过热 9、短路 10；而本仓库 `邮件IX` 定的 RAM 上限**默认只有 8**（接入仓才加）。**照抄 2077 会让这两条破解在默认配置下放不出来**。所以本文档给两列值：`2077 可查值`（考据用）与 `本仓库建议值`（t31 抄这一列），并在 §3 把冲突写清。

---

## 1. 可直接抄进代码的表（t31 抄「本仓库建议值」列）

### A. 五条快速破解 —— `core/HackLibrary.java`

| 破解 id | 字段（代码位置） | 现值 | 2077 可查值 | **本仓库建议值** | 出处 | 置信度 |
|---|---|---|---|---|---|---|
| `overheat` | `uploadTicks`（:19 第 3 参） | 20（1.0s） | **2.0s** | 20（1.0s，不变） | gamestegy Overheat：Upload time 2s | 高（2077 值）／中（本仓库取舍） |
| `overheat` | `ramCost`（:19 第 4 参） | 4 | **9** | 4（不变） | gamestegy Overheat：RAM cost 9 | 高／中（见 §3 量纲冲突） |
| `overheat` | `setRemainingFireTicks`（:23） | 60（3s） | Duration **2s** | 60（3s，不变） | gamestegy：Duration 2s；邮件IX：「点燃目标 3 秒」 | 高（2077）／高（邮件IX 锚点优先，见 §3.4） |
| `overheat` | `armorDebuff(…, 60, -4.0D)`（:25） | −4 平值 | T4 效果「Melts enemy armor over time (**max. -40%**)」 | **−5 平值**（若要真 −40% 需改实现，见 §3.5） | gamestegy Overheat：Effects T4 | 中（换算是建议值） |
| `short_circuit` | `uploadTicks`（:31） | 30（1.5s） | **0.5s** | **20（1.0s）** ← 唯一建议改的上传值 | gamestegy Short Circuit：Upload time 0.5s | 高／中（见 §3.3 下限） |
| `short_circuit` | `ramCost`（:31） | 5 | **10** | 5（不变） | gamestegy Short Circuit：RAM cost 10 | 高／中 |
| `short_circuit` | `hurtServer(…, 8.0F)`（:38） | 8.0 | **260**（2077 伤害量纲） | **10.0F** | gamestegy：Damage 260 | 高（2077 数）／低（换算无官方依据，见 §4-A3） |
| `synapse_burnout` | `uploadTicks`（:45） | 40（2.0s） | 未能取得 | 40（不变） | — | **低（无出处）** |
| `synapse_burnout` | `ramCost`（:45） | 3 | 未能取得（2077 里它是**终极**破解，属最贵一档） | **6**（可选方案，见 §4-A4） | 2077 定档常识 + 本仓库量纲 | **低（无出处）** |
| `synapse_burnout` | SLOWNESS/WEAKNESS（:49、:50） | 100 刻（5s）／amp 1 | 未能取得 | 100 刻（5s，不变） | 邮件IX：「虚弱+缓慢 5 秒」 | 高（邮件IX 锚点） |
| `synapse_burnout` | 效果等级 amp（:49、:50） | 1（= MC 的 II 级） | 未能取得 | **0（I 级）**（可选方案，见 §4-A5） | — | **低（无出处）** |
| `weapon_glitch` | `uploadTicks`（:56） | 24（1.2s） | 未能取得 | 24（不变） | — | **低（无出处）** |
| `weapon_glitch` | `ramCost`（:56） | 6 | 未能取得 | 6（不变） | — | **低（无出处）** |
| `weapon_glitch` | `weaponGlitch(target, 160)`（:60） | 160 刻（8s） | 未能取得 | 160（8s，不变） | 邮件IX：「禁用目标远程攻击AI逻辑8秒」 | 高（邮件IX 锚点） |
| `system_reset` | `uploadTicks`（:65） | 50（2.5s） | 未能取得 | 50（不变） | — | **低（无出处）** |
| `system_reset` | `ramCost`（:65） | 8 | 未能取得 | **8（不变，house 锚点）** | 邮件IX：「消耗极高，如8点RAM」 | 高（邮件IX 锚点） |
| `system_reset` | `disableAi(target, 60)`（:69） | 60 刻（3s） | 未能取得 | 60（3s，不变） | 邮件IX：「无法移动与攻击3秒」 | 高（邮件IX 锚点） |

⚠️ **构造参数顺序**（t31 容易搞反）：`HackLibrary(String id, String displayName, int uploadTicks, int ramCost)`（`HackLibrary.java:80`）—— 第 3 参是**上传刻数**，第 4 参才是 **RAM 占用**。

### B. 脑机超频 —— `core/CyberwareStats.java:33-34`

| 字段 | 现值（COMMON→MYTHIC） | 2077 可查值 | **本仓库建议值** | 出处 | 置信度 |
|---|---|---|---|---|---|
| `OVERCLOCK_DURATION_SECONDS` | 10 / 12 / 14 / 16 / 18 / 20 秒 | 未能取得（2077 的 Overclock 是智力系技能，**没有**按接入仓分档的机制） | 方案①**保持现表**（默认）；方案②12/14/16/18/20/22（见 §4-B1） | 邮件IX：「有持续时间和冷却时间（由网络接入仓的品质决定）」；[namu.wiki 특전 检索摘要](https://namu.wiki/w/%EC%82%AC%EC%9D%B4%EB%B2%84%ED%8E%91%ED%81%AC%202077/%ED%8A%B9%EC%A0%84)：2077 的 Overclock「冷却与使用时长相**不成比例**」 | 高（house 机制）／**低（秒数无出处）** |
| `OVERCLOCK_COOLDOWN_SECONDS` | 60 / 55 / 50 / 45 / 40 / 35 秒 | 未能取得 | 方案①保持现表；方案②75/70/65/60/55/50（见 §4-B1） | 同上 | 高／**低** |

**t31 注意（已核实，不需要额外改代码）**：`CyberwareStats.maxOverclockTicks()`（`CyberwareStats.java:152` 起）是**从这两张表实时算**出来的上限（用于服务器重启后的自愈判定），所以改表不用同步改别处；客户端 HUD 的「结束前 3 秒」用的是 `remainingTicks <= 60`（`RAM-HUD-OVERCLOCK.md` §3），也与这两张表无关。

---

## 2. 2077 实测抓取到的原始数据（出处原文）

### 2.1 Overheat（过热）—— [gamestegy.com/cyberpunk-2077/wiki/1048/overheat](https://gamestegy.com/cyberpunk-2077/wiki/1048/overheat)
抓取时间：本次会话；HTTP 200，页面注明 "Last updated: 15 July 2025"。页面显示的是**满档**（Tier `123455++Legendary++`）数值：

```
Overheat            Damage Quickhack — Thermal damage
RAM cost     9
Damage       29.88
Duration     2s
Upload time  2s
Max stacks   2
Spread range 6m
Traceability 30
Effects: T1 点燃持续伤害 / T2 重复上传延长时长 / T3 攻击延长时长 /
         T4 Melts enemy armor over time (max. -40%) /
         T5 攻击附带 10% 物理伤害的热能伤害 / T5++ 该比例 20%
```

### 2.2 Short Circuit（短路）—— [gamestegy.com/cyberpunk-2077/wiki/1028/short-circuit](https://gamestegy.com/cyberpunk-2077/wiki/1028/short-circuit)
同样为满档数值：

```
Short Circuit       Damage Quickhack — Electric damage
RAM cost     10
Damage       260
Duration     3s
Upload time  0.5s
Spread range 6m
Traceability 30
Effects: T1 造成伤害，机甲/机器人/无人机/炮塔受额外伤害 /
         T2 目标身上所有 Control 类破解 +3 秒 /
         T3 每层 Cyberware Malfunction +10% 伤害 / T4 附加 EMP 持续伤害 /
         T5 对低于 High 威胁等级的敌人 +20% 伤害 / T5++ 每层 +25%
```

### 2.3 关于「武器故障」的命名对照（重要）
2077 里禁用武器/义体的那条破解英文是 **Cyberware Malfunction**（中文常译「义体故障」），本仓库叫「武器故障」。
本次**未能**抓到它的数值页（见 §5）。t31 替换时不要以为 2077 有个叫 "Weapon Glitch" 的对应条目 —— 名字对不上，数值也就没有可比对对象。

### 2.4 量级参照（帮助判断本仓库 RAM 阶梯合不合理）
[九游《赛博朋克2077》2.0黑客流插件推荐](https://www.9game.cn/sbpk2077/9002856.html)（HTTP 200，抓取到全文）在 2.0 配装里写：**「重启义眼…只要2RAM」**。
→ 说明 2077 里**廉价的控制类破解**在 2 RAM 量级；本仓库把「突触熔断」放在 3 RAM 属于「廉价档」，而 2077 里它是终极档（见 §4-A4）。

---

## 3. 本仓库口径与 2077 的冲突（必须写出来）

### 3.1 RAM 预算量级差（最主要冲突）
| | 2077（满档） | 本仓库 |
|---|---|---|
| 过热 | 9 RAM | 占位 4 |
| 短路 | 10 RAM | 占位 5 |
| RAM 上限 | 20~30 级（玩家可堆） | **默认 8**（邮件IX §二.1），靠接入仓往上加 |
| RAM 恢复 | 每秒级 | **每分钟**：接入仓 0~9/分钟（`spec/义体拓展.txt` 第 18~23、27 行：+9 / +6~9 / +0~3 / +6） |

**结论**：直接照抄 2077 的 9 / 10，会让「默认上限 8」的玩家**一条都放不出来**（除非开超频走濒死扣血）。建议保留本仓库阶梯（4/5/…/8），只把 2077 的**相对顺序**对齐（见 3.2）。

### 3.2 相对顺序：本仓库有一处与 2077 相反
- 2077（已实测）：短路上传 **0.5s** ＜ 过热上传 **2.0s** → **短路更快**。
- 本仓库占位：过热 1.0s ＜ 短路 1.5s → **过热更快**（与 2077 相反）。
→ 因此本文档**只建议改一个上传值**：`short_circuit` 30 → 20 刻（1.0s），让短路成为最快的一条。

### 3.3 上传时长下限（本仓库可玩性）
2077 短路上传 0.5s，本仓库要做一条**看得见的上传进度条 + 全息倒计时**（邮件IX §四.3）。
0.5s（10 刻）在 MC 里几乎看不见 → **建议上传时长下限 1.0s**，所以本文档没有把短路压到 0.5s，而是压在 1.0s。这是「官方值 vs 本仓库可玩性」的显式取舍。

### 3.4 点燃时长：邮件IX 覆盖 2077
2077 Overheat Duration = **2s**（实测）；邮件IX 明写「点燃目标 **3 秒**」。
→ 按「主人邮件 > 外部考据」，**保留 3s**，把冲突记在这里。

### 3.5 破甲：2077 是百分比，本仓库代码是平值
2077 T4 效果是「**max. −40%** 护甲」；本仓库 `CombatEffects.armorDebuff(target, ticks, amount)`（`:60-69`）用的是 `Attributes.ARMOR` + `AttributeModifier.Operation.ADD_VALUE` = **平值加减**，不是百分比。
→ 直接填 −40 会变成「减 40 点护甲」（比下界合金全套还多），**不要那么填**。建议 −5 平值，并在 §4-A2 交主人裁决；真要做 −40% 需要改成百分比实现（属于代码改动，超出 t31 的「换数字」范围）。

### 3.6 其它沿用先例口径（`spec/义体拓展.txt`）的地方
已通读该文档全文（36 行）确认：它只覆盖**操作系统/网络接入仓/狂暴**，给的是**接入仓级**数值（RAM 数量 5~13、缓冲、栏位、RAM 恢复 0~9/分钟、上传时间/终极破解占用的**修正百分比**如 −25%/−75%），
**没有**任何「某一条快速破解本身要多少 RAM / 上传多久 / 效果持续多久」的基数，也没有超频时长与冷却。
所以本次要填的这两块（`HackLibrary` 的 5 条 + `CyberwareStats` 的两张表）**没有先例口径可冲突**，一律以邮件IX + 本仓库量纲为准；接入仓级的 RAM 恢复口径仍沿用该文档（`Stats.RAM_REGEN`）。

---

## 4. 需主人裁决（无可靠出处的，一律写「建议值 + 理由 + 不确定」）

### A 组 · 五条破解

**A1. 突触熔断 / 武器故障 / 系统重置 的 RAM 占用与上传时长（无出处）**
- 现状：3 与 2.0s / 6 与 1.2s / 8 与 2.5s。
- 建议：`system_reset` RAM 保持 **8**（邮件IX 明文锚点）；另外两条**保持现值**，因为唯一能查到的两条（过热 9、短路 10）无法反推出其余三条的官方数字。
- 理由：改成任何数字都没有出处，等于用「我觉得」替换「主人的占位」，违反本项目禁编造。
- **不确定**：2077 满档下三条的实际 RAM 成本未知（抓取失败，见 §5）。

**A2. 过热的破甲数值（换算是建议值）**
- 建议：`armorDebuff(target, 60, -4.0D)` → **`-5.0D`**（仍为 3 秒）。
- 理由：2077 的「−40%」在 MC 平值口径下不可直接用；MC 里一件铁甲 5~6 点、下界合金 8 点，−5 相当于「打掉一件铁甲」，与「点燃 + 破甲」的体感相称。
- **不确定**：−4 → −5 纯手感，无出处。

**A3. 短路的瞬间伤害（换算无官方依据）**
- 建议：`8.0F` → **`10.0F`**。
- 理由：2077 短路是「高额瞬间伤害」，满档 260（远高于过热的 29.88 直接伤害，靠 DoT 补）；MC 里 8 = 4 颗心、10 = 5 颗心，10 更接近「高额但不秒杀玩家」。
- **不确定**：260 → 10 的换算是本文档自定的，**没有任何官方换算表**，主人可任意改。

**A4. 突触熔断的 RAM 是否要从 3 提到 6（**建议但不确定**）**
- 建议：`ramCost` 3 → **6**。
- 理由：2077 里突触熔断是**终极**破解（与自杀/系统重置同档，最贵一档），而本仓库把它放在**最便宜**的 3，与 2077 的「越强越贵」方向相反；6 会让阶梯变成 4/5/6/6/8（过热/短路/突触/武器故障/系统重置），单调且不与系统重置撞 8。
- **不确定**：本仓库里它的效果只有「虚弱 + 缓慢 5 秒」（远弱于 2077 的巨额伤害 + 延长超频），所以**保持 3 也有充分理由**（效果弱就该便宜）。这是设计取向，请主人拍板。

**A5. 突触熔断的效果等级 amp=1（无出处）**
- 现状：`MobEffectInstance(MobEffects.SLOWNESS, 100, 1)` 与 `WEAKNESS, 100, 1`。MC 的 `amp=1` 是**II 级**（`amp=0` 才是 I 级）。
- 建议：**保持 amp 1**（主人若觉得太强，改 0 即可）。
- **不确定**：2077 的 Cripple/Synapse 效果没有可对应的等级刻度，amp 无出处。

### B 组 · 脑机超频（无出处，两个方案二选一）

**B1. 时长/冷却表的秒数**
- 方案①（默认，**推荐**）：**保持现表** 时长 10/12/14/16/18/20、冷却 60/55/50/45/40/35。
  理由：邮件IX 只定了「由接入仓品质决定」，没给数字；现表单调、上下限合理，且 0.4.0 已经按它跑过一版。
- 方案②（可选的收紧方案）：时长 **12/14/16/18/20/22**、冷却 **75/70/65/60/55/50**（整体 +15s）。
  理由：现表最顶档的占空比 = 20 / (20+35) ≈ 36%，配合「濒死超频用生命放破解」会让超频接近常驻，削弱「资源稀缺」的设计意图；整体上移 15s 可把最顶档压到 ≈29%。
  **不确定**：这是手感推理，**没有 2077 出处**（2077 的 Overclock 冷却与时长不成比例，也无法照抄）。
- 主人也可以直接给 6 个数，t31 只改 `CyberwareStats.java:33-34` 两行数组。

---

## 5. 取数过程与「未能取得的来源」（诚实记录，供主人复核）

已成功抓取（HTTP 200 + 正文）：
- [gamestegy Overheat](https://gamestegy.com/cyberpunk-2077/wiki/1048/overheat)、[gamestegy Short Circuit](https://gamestegy.com/cyberpunk-2077/wiki/1028/short-circuit)、[gamestegy Quickhacks 索引](https://gamestegy.com/cyberpunk-2077/wiki/quickhacks)（该索引**只收录 8 条**破解，不含突触熔断/Cyberware Malfunction/系统重置，所以这三条在 gamestegy 上根本查不到）。
- [九游 2.0 黑客流插件推荐](https://www.9game.cn/sbpk2077/9002856.html)（给出「重启义眼 2 RAM」量级参照）。

**尝试但抓取失败**（每一条都记录失败原因，置信度因此判为「低」）：
| 来源 | 结果 |
|---|---|
| [Game8 · Synapse Burnout](https://game8.co/games/Cyberpunk-2077/archives/Quickhacks-Synapse-Burnout) | HTTP **403**（CloudFront 拦截，非浏览器 UA 一律拒） |
| [Game8 · Weapon Glitch](https://game8.co/games/Cyberpunk-2077/archives/Quickhacks-Weapon-Glitch) | HTTP **403** |
| [Game8 · Overclock 技能](https://game8.co/games/Cyberpunk-2077/archives/Perks-Overclock) | HTTP **403** |
| `cyberpunk.fandom.com`（Synapse Burnout / Quickhack） | 域名解析到**非公网 IP**，抓取被拒 |
| `cyberpunk2077.wiki.fextralife.com`（Synapse Burnout / Weapon Glitch） | 请求**超时**（30s），多次重试均失败 |
| [namu.wiki 퀵핵](https://en.namu.wiki/w/%ED%80%B5%ED%95%B5)、[별자리 오버클록](https://stellar-gamer.com/cyberpunk2077/terms/3217) | 请求**超时** |
| [bilibili · 快速破解消耗基础值](https://www.bilibili.com/opus/1002906246960906294) | 返回「验证码」页，正文取不到 |

**从搜索索引摘要里拿到的两条 2077 事实**（页面本身打不开，故只作旁证，置信度中）：
1. [namu.wiki 특전](https://namu.wiki/w/%EC%82%AC%EC%9D%B4%EB%B2%84%ED%8E%91%ED%81%AC%202077/%ED%8A%B9%EC%A0%84)：「Sandevistan/Berserk 的冷却与使用时长相**关**，而 Overclock 的冷却与使用时长**不成比例**」→ 佐证「不能拿 2077 的超频去套本仓库的稀有度分档表」。
2. 同页：「突触熔断的 5 阶效果是**用它击杀敌人时延长 Overclock 持续时间**」→ 佐证突触熔断在 2077 属高位核心破解（支持 §4-A4 的「应该更贵」方向）。

**给主人的话**：如果希望我把突触熔断/武器故障/系统重置也换成有出处的真数字，请给一个能联网的取数窗口（或者直接把 Game8/fandom 的数值页贴给我），我可以在几分钟内补齐并把置信度从「低」提到「高」。现在的做法是**宁可留空并标注，也不填一个看起来合理却没有出处的数字**。

---

## 6. t31 替换清单（精确到 文件:行）

| # | 文件:行 | 现在 | 建议改成 | 备注 |
|---|---|---|---|---|
| 1 | `core/HackLibrary.java:19` | `overheat", "过热", 20, 4` | **不变** | RAM 4 / 上传 20 刻 |
| 2 | `core/HackLibrary.java:23` | `setRemainingFireTicks(60)` | **不变**（3s） | 邮件IX 锚点，覆盖 2077 的 2s |
| 3 | `core/HackLibrary.java:25` | `armorDebuff(target, 60, -4.0D)` | `-5.0D`（可选） | §4-A2 |
| 4 | `core/HackLibrary.java:31` | `short_circuit", "短路", 30, 5` | **`..., 20, 5`** | 唯一建议改的上传值（2077 短路更快） |
| 5 | `core/HackLibrary.java:38` | `hurtServer(…, 8.0F)` | `10.0F`（可选） | §4-A3 |
| 6 | `core/HackLibrary.java:45` | `synapse_burnout", "突触熔断", 40, 3` | `..., 40, 6`（建议但不确定） | §4-A4，请主人拍板 |
| 7 | `core/HackLibrary.java:49-50` | `SLOWNESS/WEAKNESS, 100, 1` | **不变** | amp 1 = II 级，见 §4-A5 |
| 8 | `core/HackLibrary.java:56` | `weapon_glitch", "武器故障", 24, 6` | **不变** | 无出处，保持占位 |
| 9 | `core/HackLibrary.java:60` | `weaponGlitch(target, 160)` | **不变**（8s） | 邮件IX 锚点 |
| 10 | `core/HackLibrary.java:65` | `system_reset", "系统重置", 50, 8` | **不变** | RAM 8 = 邮件IX 锚点 |
| 11 | `core/HackLibrary.java:69` | `disableAi(target, 60)` | **不变**（3s） | 邮件IX 锚点 |
| 12 | `core/CyberwareStats.java:33` | `{10, 12, 14, 16, 18, 20}` | **不变**（方案①）或 `{12,14,16,18,20,22}`（方案②） | §4-B1 |
| 13 | `core/CyberwareStats.java:34` | `{60, 55, 50, 45, 40, 35}` | **不变**（方案①）或 `{75,70,65,60,55,50}`（方案②） | §4-B1 |

**不在本次调研范围但顺带确认**：`core/HackSystem.java` 里的 `DYING_HEALTH_COST = 2.0F` / `DYING_HEALTH_FLOOR = 2.0F`（行号会随 t28 并行改动漂移，按常量名 grep）（濒死超频扣 2 点生命、最低保留 1 颗心 = 2 HP）**已有出处**（邮件IX §三.2），是可直接去掉 `TODO(主人填写)` 的真数字，不需要本次调研补值。

**替换时的注释要求**（`STEP3-CONTRACT.md` §2）：替换后在同一行注释里写出来源，例如
`// 2077: Short Circuit upload 0.5s（gamestegy）→ 本仓库压到 1.0s，理由见 HACK-VALUES.md §3.3`。

---

## 7. 本任务边界声明

- 只写了本文件 `HACK-VALUES.md`；**未改任何 `.java`**（`git status` 可核）。
- 未跑 gradle（本任务无编译需求）。
- 所有「有出处」的数字都指到 §2/§5 的具体 URL 与字段名；所有「无出处」的数字都在 §4 逐条标注 **建议值 / 理由 / 不确定**。
