# DEFS-VALUES.md —— CyberwareDefinitions.java 数值填充交付（t3 / numbersmith）

工作区：`/root/mod26/cyberware`　独占改动：`src/main/java/com/dsh/cyberware/core/CyberwareDefinitions.java`

```
# 本文件是交付报告；本任务实际改动的文件只有 CyberwareDefinitions.java（数值 + 删 12 条 + 注释 19 条）。
# 计数核验：
grep -c "^    public static final CyberwareDefinition.*register(new CyberwareDefinition(" \
    src/main/java/com/dsh/cyberware/core/CyberwareDefinitions.java   # -> 123（= 保留定义数）
grep -c "register(new CyberwareDefinition(" 同上文件                    # -> 142（含 19 条已注释）
grep -c "// 待素材：暂无贴图素材，暂不注册（见 ASSET-MISSING.txt）" 同上文件  # -> 19
```

## 0. 口径与规则（先读）

1. **容量**取 `描述.txt` 表格「所需义体容量」列原值（`描述.txt` L4/L33/L56/L83/L135/L139/L168/L201/L234/L258/L288/L310/L355 为表头，列序 = 义体名称 | 稀有度 | 所需义体容量 | 义体效果 | 备注）。
2. **区间 `a~b` 一律取 b（最高档）**。依据：`表头`说明每行按稀有度分档，本表每个型号只保留一个最高稀有度变体（LEGENDARY/MYTHIC），b 即该型号最高档读数；同一规则也是网络接入仓「1~5型」取其第 5 型的原因。
3. **只映射 `CyberwareDefinition.Stats` 中已存在的键**（该文件不在本任务独占范围，未新增键）。`描述.txt` 中有、但没有对应键的效果（半伤几率/强度、移动速度、攻击速度、伤害减免与抗性、属性检定、负重、光学变焦、高亮距离、流失/燃烧/电击/中毒几率、跳弹伤害、耐力消耗等）**不映射**，只保留在描述串里，清单见第 4.2 节。
4. **数值单位**：`时间减慢X%` 存小数（0.85 = 85%），与既有代码一致；其余百分比键直接存数字（20 = 20%）。`后坐力降低` 存负数（-35），与既有代码一致。
5. **`ARMOR` 键**：`描述.txt` 给的是「提供护甲：X~Y」的**平值**，而 `Stats.ARMOR` 的注释写「护甲加成（%）」。本表按 `描述.txt` 原文把平值存进 `ARMOR`（该键是现存唯一的护甲键），单位口径冲突见第 6 节待办。
6. **条件性/触发型数值**：`描述.txt` 原文给的是条件数值时（例如「生命值低于20%时达到最大值50%」「受到中毒效果影响的敌人+10%~22%伤害」「对无人机…造成的伤害提高15%~40%」），取文档最大值填入，并在第 3 节该条目下加 `＊条件值` 标注；不额外推导、不换算。
7. **第 6 个参数（描述串）**改写自 `描述.txt` 的「义体效果」列原文（多行合成一行），并追加该行的「XX调幅：」句；没有「调幅」句的不追加。未做任何文字创作。

---

## 1. 第 1 步：删除的 12 条旧定义（主人裁决）

| 常量名 | id | 原文件行号（基线） | 删除后保留的新 id |
|---|---|---|---|
| `SANDEVISTAN_ZETATECH` | `sandevistan_zetatech` | L22 | `sandevistan_c1` |
| `SANDEVISTAN_DYNALAR` | `sandevistan_dynalar` | L35 | `sandevistan_c2` |
| `SANDEVISTAN_QIANTAI` | `sandevistan_qiantai` | L50 | `sandevistan_c3` |
| `SANDEVISTAN_MILITECH_FALCON` | `sandevistan_militech_falcon` | L63 | `sandevistan_c4` |
| `SANDEVISTAN_MILITECH_APOGEE` | `sandevistan_militech_apogee` | L73 | `sandevistan_apogee` |
| `CYBERDECK_MILITECH_PARALLEL` | `cyberdeck_militech_parallel` | L92 | `militech_paraline_mkv` |
| `CYBERDECK_BIOTECH_3` | `cyberdeck_biotech_3` | L167 | `biotech_sigma_mkiv` |
| `CYBERDECK_MILITECH_CANTO_6` | `cyberdeck_militech_canto_6` | L252 | `haunted_cyberdeck` |
| `BERSERK_MOORE` | `berserk_moore` | L283 | `berserk_c1` |
| `BERSERK_BIODYNE` | `berserk_biodyne` | L297 | `berserk_c2` |
| `BERSERK_MILITECH` | `berserk_militech` | L308 | `berserk_c4` |
| `BERSERK_ZETATECH` | `berserk_zetatech` | L318 | `berserk_c3` |

合计 **12** 条，全部位于基线的旧定义区（L22–L318，共 31 条）；删除后旧定义区剩 19 条（均在第 2 步白名单内，数值沿用《义体拓展》文档，见 6.6）。

保留的新 id 复核（第 1 步要求）：`sandevistan_c1`、`sandevistan_c2`、`sandevistan_c3`、`sandevistan_c4`、`sandevistan_apogee`、`berserk_c1`、`berserk_c2`、`berserk_c3`、`berserk_c4`、`militech_paraline_mkv`、`biotech_sigma_mkiv`、`haunted_cyberdeck` —— 12 条在最终文件中全部存在。

---

## 2. 第 2 步：注释留档的无素材定义（与 ASSET-MISSING.txt 对齐）

依据 `/root/mod26/cyberware/ASSET-MISSING.txt`（19 条）与 `/root/mod26/cyberware/ASSET-WHITELIST.txt`（123 条）。两文件并集 = 142 = 基线 154 − 删除 12，且交集为空，已用脚本核验无遗漏、无多出。

处理方式：**不删代码**，整块（含 `/** */` 文档注释与整个 `register(...)` 表达式）逐行 `//` 注释，并在块首加：

```java
// 待素材：暂无贴图素材，暂不注册（见 ASSET-MISSING.txt）
```

| # | 常量名 | id | 中文名 | 描述.txt 行 | 描述.txt 是否有该条目 |
|---|---|---|---|---|---|
| 1 | `MASK_CW_PLUS_PLUS` | `mask_cw_plus_plus` | 行为特征脸板 | L194–L199 | 有 |
| 2 | `ICONIC_DISCHARGE_CONNECTOR` | `iconic_discharge_connector` | 电磁回收 | L301–L302 | 有 |
| 3 | `ICONIC_DETECTOR_RUSH` | `iconic_detector_rush` | 肾上腺导引 | L260–L261 | 有 |
| 4 | `TIME_BANK` | `time_bank` | 量子调谐 | L24–L28 | 有 |
| 5 | `ICONIC_SUBDERMAL_CO_PROCESSOR` | `iconic_subdermal_co_processor` | 皮下变色 | L5–L6 | 有 |
| 6 | `KIROSHI_OPTICS_PIERCING` | `kiroshi_optics_piercing` | 歧路司义眼追猎 | L185–L188 | 有 |
| 7 | `KIROSHI_OPTICS_SENSOR` | `kiroshi_optics_sensor` | 歧路司义眼警戒 | L182–L184 | 有 |
| 8 | `YAKUZA_TATTOO` | `yakuza_tattoo` | 纹身虎爪帮 | L251–L253 | 有 |
| 9 | `SILVERHAND_TATTOO` | `silverhand_tattoo` | 纹身永远在一起 | L254–L256 | 有 |
| 10 | `CASIUS_TATTOO` | `casius_tattoo` | 纹身强尼特制 | L254–L256 | 有 |
| 11 | `SHOCK_ABSORBER` | `shock_absorber` | 持握衬垫 | L296 | 有 |
| 12 | `ICONIC_VISUAL_CORTEX_SUPPORT` | `iconic_visual_cortex_support` | 长焦可视界面 | L266–L267 | 有 |
| 13 | `REFLEX_RECORDER` | `reflex_recorder` | 反应协调器 | L274–L275 | 有 |
| 14 | `PAIN_DISTRIBUTOR` | `pain_distributor` | 通用增强件 | L231–L232 | 有 |
| 15 | `BIONIC_JOINTS` | `bionic_joints` | 仿生关节 | L202–L204 | 有 |
| 16 | `MECHATRONIC_CORE` | `mechatronic_core` | 机电核心 | L22 | 有 |
| 17 | `SMART_STORAGE` | `smart_storage` | 蓄电缓冲 | — | 无（保留 TODO） |
| 18 | `SUBDERMAL_CO_PROCESSOR` | `subdermal_co_processor` | 牛顿模块 | L7 | 有 |
| 19 | `BIO_CONDUCTORS` | `bio_conductors` | 生物导体 | L11–L12 | 有 |

注：19 条全部属于官方 123 条（旧定义区的 19 条保留定义都在白名单里，没有一条进 MISSING）。
19 条注释块内的数值仍按第 3 步填好了（真实值，非占位）：素材到位后取消注释即可直接使用。

---

## 3. 第 3 步：保留的 123 条定义逐条数值

「容量出处行」= `描述.txt` 中该物品「所需义体容量」数值所在的那一行；「描述.txt 整条区间」= 该物品在 `描述.txt` 中的整行区间；「各键取值行」= 每个键单独能定位到某一行时给出该行号，定位不唯一时写 `同区间`（该键数值仍在所给区间内）。

| # | 中文名 | id | 容量 | 容量出处行 | 描述.txt 整条区间 | stats 键值 | 各键取值行 |
|---|---|---|---|---|---|---|---|
| 1 | 冬月电子1型 | `cyberdeck_dongyue_1` | 6 | — | — | `RAM`=3, `BUFFER`=5, `SLOTS`=2 | 沿用《义体拓展》文档（见 6.6） |
| 2 | 修补匠3型 | `cyberdeck_tinkerer_3` | 20 | — | — | `RAM`=8, `BUFFER`=7, `SLOTS`=6, `RAM_REGEN`=9, `COMBAT_HACK_DURATION`=50, `SPREAD_DISTANCE`=40 | 沿用《义体拓展》文档（见 6.6） |
| 3 | 瑞草电子1型 | `cyberdeck_ruicao_1` | 8 | — | — | `RAM`=4, `BUFFER`=5, `SLOTS`=3, `STEALTH_COST`=-1 | 沿用《义体拓展》文档（见 6.6） |
| 4 | 瑞草电子2型 | `cyberdeck_ruicao_2` | 14 | — | — | `RAM`=6, `BUFFER`=6, `SLOTS`=4, `STEALTH_COST`=-1, `UPLOAD_TIME`=-25 | 沿用《义体拓展》文档（见 6.6） |
| 5 | 生物动力1型 | `cyberdeck_biodyne_1` | 10 | — | — | `RAM`=6, `BUFFER`=5, `SLOTS`=3 | 沿用《义体拓展》文档（见 6.6） |
| 6 | 生物动力2型 | `cyberdeck_biodyne_2` | 16 | — | — | `RAM`=9, `BUFFER`=6, `SLOTS`=4, `RAM_REGEN`=3 | 沿用《义体拓展》文档（见 6.6） |
| 7 | 生物技术1型 | `cyberdeck_biotech_1` | 8 | — | — | `RAM`=5, `BUFFER`=5, `SLOTS`=3, `RAM_REGEN`=6 | 沿用《义体拓展》文档（见 6.6） |
| 8 | 生物技术2型 | `cyberdeck_biotech_2` | 14 | — | — | `RAM`=7, `BUFFER`=6, `SLOTS`=4, `RAM_REGEN`=9, `HACK_DAMAGE`=10 | 沿用《义体拓展》文档（见 6.6） |
| 9 | 泰克重工技术2型 | `cyberdeck_technica_2` | 12 | — | — | `RAM`=8, `BUFFER`=7, `SLOTS`=4, `HACK_COOLDOWN`=30, `COMBAT_HACK_DURATION`=30, `UPLOAD_TIME`=-25 | 沿用《义体拓展》文档（见 6.6） |
| 10 | 泰克重工技术3型 | `cyberdeck_technica_3` | 18 | — | — | `RAM`=10, `BUFFER`=7, `SLOTS`=5, `HACK_COOLDOWN`=45, `COMBAT_HACK_DURATION`=40, `UPLOAD_TIME`=-25 | 沿用《义体拓展》文档（见 6.6） |
| 11 | 泰克重工技术4型 | `cyberdeck_technica_4` | 24 | — | — | `RAM`=12, `BUFFER`=8, `SLOTS`=6, `HACK_COOLDOWN`=45, `COMBAT_HACK_DURATION`=50, `UPLOAD_TIME`=-25 | 沿用《义体拓展》文档（见 6.6） |
| 12 | 四相传电1型 | `cyberdeck_tetratronic_1` | 8 | — | — | `RAM`=4, `BUFFER`=5, `SLOTS`=3 | 沿用《义体拓展》文档（见 6.6） |
| 13 | 四相传电2型 | `cyberdeck_tetratronic_2` | 14 | — | — | `RAM`=6, `BUFFER`=6, `SLOTS`=4, `ULTIMATE_COST`=-1 | 沿用《义体拓展》文档（见 6.6） |
| 14 | 四相传电3型 | `cyberdeck_tetratronic_3` | 20 | — | — | `RAM`=8, `BUFFER`=7, `SLOTS`=5, `ULTIMATE_COST`=-2 | 沿用《义体拓展》文档（见 6.6） |
| 15 | 涟漪4型 | `cyberdeck_ripple_4` | 26 | — | — | `RAM`=10, `BUFFER`=8, `SLOTS`=6, `ULTIMATE_COST`=-3, `UPLOAD_TIME`=-75, `HACK_COOLDOWN`=45 | 沿用《义体拓展》文档（见 6.6） |
| 16 | 荒坂3型 | `cyberdeck_arasaka_3` | 18 | — | — | `RAM`=8, `BUFFER`=7, `SLOTS`=5 | 沿用《义体拓展》文档（见 6.6） |
| 17 | 荒坂4型 | `cyberdeck_arasaka_4` | 24 | — | — | `RAM`=10, `BUFFER`=8, `SLOTS`=6 | 沿用《义体拓展》文档（见 6.6） |
| 18 | 网络监察网驱5型 | `cyberdeck_netwatch_5` | 30 | — | — | `RAM`=11, `BUFFER`=8, `SLOTS`=6 | 沿用《义体拓展》文档（见 6.6） |
| 19 | 乌鸦微控4型 | `cyberdeck_raven_4` | 24 | — | — | `ENEMY_HACK_TIME`=100, `HACK_DISTANCE`=60, `RAM_REGEN`=6 | 沿用《义体拓展》文档（见 6.6） |
| 20 | 装殖缩减 | `capacity_booster` | 0 | L137 | L136–L137 | （空） | （无 stats） |
| 21 | 军用科技篇章6型 | `haunted_cyberdeck` | 33 | L99 | L98–L105 | `RAM`=11, `SLOTS`=4, `BUFFER`=4 | `RAM`=同区间，`SLOTS`=L100，`BUFFER`=L101 |
| 22 | 网络监察网驱1型 | `netwatch_netdriver_mk` | 25 | L114 | L113–L120 | `RAM`=13, `SLOTS`=8, `BUFFER`=4 | `RAM`=同区间，`SLOTS`=L115，`BUFFER`=L116 |
| 23 | 军用科技斯安威斯坦”远地点“ | `sandevistan_apogee` | 44 | L58 | L57–L61 | `TIME_SLOW`=0.85, `DURATION`=6, `COOLDOWN`=25, `HEADSHOT_DAMAGE`=20, `CRIT_CHANCE`=20, `CRIT_DAMAGE`=20 | `TIME_SLOW`=L59，`DURATION`=L60，`COOLDOWN`=L60，`HEADSHOT_DAMAGE`=L59，`CRIT_CHANCE`=L59，`CRIT_DAMAGE`=L59 |
| 24 | 军用科技斯安威斯坦”游隼“ | `sandevistan_c4` | 39 | L63 | L62–L66 | `TIME_SLOW`=0.7, `DURATION`=10, `COOLDOWN`=30, `ALL_DAMAGE`=10, `CRIT_CHANCE`=15, `CRIT_DAMAGE`=10 | `TIME_SLOW`=L64，`DURATION`=L65，`COOLDOWN`=L65，`ALL_DAMAGE`=L64，`CRIT_CHANCE`=L64，`CRIT_DAMAGE`=L64 |
| 25 | 军用科技狂暴 | `berserk_c4` | 35 | L35 | L34–L39 | `DURATION`=12, `COOLDOWN`=25, `KILL_HEAL`=25, `ALL_DAMAGE`=50 | `DURATION`=L38，`COOLDOWN`=L38，`KILL_HEAL`=L37，`ALL_DAMAGE`=同区间 |
| 26 | 歧路司义眼石化鸡蛇 | `iconic_advanced_kiroshi_optics_bare` | 30 | L176 | L175–L178 | `CRIT_CHANCE`=35 | `CRIT_CHANCE`=L176 |
| 27 | 坚矛利盾 | `iconic_gun_stabilizer` | 35 | L239 | L238–L241 | `RECOIL`=-35 | `RECOIL`=L239 |
| 28 | 等距稳定 | `iconic_shock_absorber` | 40 | L298 | L297–L298 | （空） | （无 stats） |
| 29 | 团灭韧带 | `iconic_jenkins_tendons` | 6 | L365 | L364–L367 | `ARMOR`=26 | `ARMOR`=L365 |
| 30 | 几质丁壳 | `iconic_chiton` | 40 | L316 | L315–L317 | `ARMOR`=200 | `ARMOR`=L316 |
| 31 | 外周逆反 | `iconic_proximity_reducer` | 24 | L344 | L343–L347 | `ARMOR`=36 | `ARMOR`=L344 |
| 32 | 乖离排异 | `iconic_reflex_recorder` | 35 | L277 | L276–L279 | `TIME_SLOW`=0.6, `DURATION`=4.5, `COOLDOWN`=35 | `TIME_SLOW`=L277，`DURATION`=L277，`COOLDOWN`=L279 |
| 33 | 一拳开 | `iconic_t1000` | 36 | L217 | L216–L217 | `ARMOR`=42 | `ARMOR`=L217 |
| 34 | COX-2赛博生体优化 | `iconic_bio_conductors` | 50 | L9 | L8–L10 | `RAM`=-4 | `RAM`=L10 |
| 35 | RAM配平 | `iconic_camillo_ram_manager` | 40 | L14 | L13–L15 | `RAM`=2, `COOLDOWN`=85 | `RAM`=同区间，`COOLDOWN`=L15 |
| 36 | 暴恐机动队螳螂刀 | `max_tac_mantis_blades` | 0 | L151 | L151–L154 | （空） | （无 stats） |
| 37 | 剧毒螳螂刀 | `mantis_blades_chemical` | 8 | L146 | L146–L150 | （空） | （无 stats） |
| 38 | 放电螳螂刀 | `mantis_blades_electric` | 8 | L146 | L146–L150 | （空） | （无 stats） |
| 39 | 螳螂刀 | `mantis_blades` | 8 | L146 | L146–L150 | （空） | （无 stats） |
| 40 | 热能螳螂刀 | `mantis_blades_thermal` | 8 | L146 | L146–L150 | （空） | （无 stats） |
| 41 | 剧毒单分子线 | `nano_wires_chemical` | 8 | L161 | L161–L166 | （空） | （无 stats） |
| 42 | 放电单分子线 | `nano_wires_electric` | 8 | L161 | L161–L166 | （空） | （无 stats） |
| 43 | 单分子线 | `nano_wires` | 8 | L161 | L161–L166 | （空） | （无 stats） |
| 44 | 热能单分子线 | `nano_wires_thermal` | 8 | L161 | L161–L166 | （空） | （无 stats） |
| 45 | 剧毒弹射发射系统 | `projectile_launcher_chemical` | 8 | L155 | L155–L160 | `ALL_DAMAGE`=30 | `ALL_DAMAGE`=同区间 |
| 46 | 电子弹射发射系统 | `projectile_launcher_electric` | 8 | L155 | L155–L160 | `ALL_DAMAGE`=30 | `ALL_DAMAGE`=同区间 |
| 47 | 弹射发射系统 | `projectile_launcher` | 8 | L155 | L155–L160 | `ALL_DAMAGE`=30 | `ALL_DAMAGE`=同区间 |
| 48 | 热能弹射发射系统 | `projectile_launcher_thermal` | 8 | L155 | L155–L160 | `ALL_DAMAGE`=30 | `ALL_DAMAGE`=同区间 |
| 49 | 剧毒大猩猩手臂 | `strong_arms_chemical` | 8 | L140 | L140–L145 | （空） | （无 stats） |
| 50 | 放电大猩猩手臂 | `strong_arms_electric` | 8 | L140 | L140–L145 | （空） | （无 stats） |
| 51 | 大猩猩手臂 | `strong_arms` | 8 | L140 | L140–L145 | （空） | （无 stats） |
| 52 | 热能大猩猩手臂 | `strong_arms_thermal` | 8 | L140 | L140–L145 | （空） | （无 stats） |
| 53 | 军用科技平行线 | `militech_paraline_mkv` | 14 | L106 | L106–L112 | `RAM`=9, `SLOTS`=8, `BUFFER`=4, `HACK_DAMAGE`=10 | `RAM`=同区间，`SLOTS`=L107，`BUFFER`=L108，`HACK_DAMAGE`=同区间 |
| 54 | 四相传电涟漪5型 | `tetratronic_rippler_mkv` | 16 | L127 | L127–L133 | `RAM`=6, `SLOTS`=8, `BUFFER`=4 | `RAM`=同区间，`SLOTS`=L128，`BUFFER`=L129 |
| 55 | 生物技术3型 | `biotech_sigma_mkiv` | 16 | L91 | L91–L97 | `RAM`=9, `SLOTS`=8, `BUFFER`=4, `COMBAT_HACK_DURATION`=30, `HACK_DAMAGE`=10 | `RAM`=L91，`SLOTS`=L92，`BUFFER`=L93，`COMBAT_HACK_DURATION`=L94，`HACK_DAMAGE`=同区间 |
| 56 | 乌鸦微控3型 | `raven_microcyber_mkiii` | 20 | L121 | L121–L126 | `RAM`=9, `SLOTS`=8, `BUFFER`=4, `SPREAD_DISTANCE`=40 | `RAM`=L121，`SLOTS`=L122，`BUFFER`=L123，`SPREAD_DISTANCE`=L124 |
| 57 | 荒板5型 | `arasaka_shadow_mkv` | 14 | L84 | L84–L90 | `RAM`=9, `SLOTS`=8, `BUFFER`=4, `ENEMY_HACK_TIME`=40, `STEALTH_COST`=-2 | `RAM`=同区间，`SLOTS`=L85，`BUFFER`=L86，`ENEMY_HACK_TIME`=L87，`STEALTH_COST`=L88 |
| 58 | 泽塔科技斯安威斯坦 | `sandevistan_c1` | 20 | L75 | L75–L79 | `TIME_SLOW`=0.6, `DURATION`=10, `COOLDOWN`=30, `ALL_DAMAGE`=24, `HEADSHOT_DAMAGE`=40 | `TIME_SLOW`=同区间，`DURATION`=L78，`COOLDOWN`=L78，`ALL_DAMAGE`=同区间，`HEADSHOT_DAMAGE`=L77 |
| 59 | 迪娜拉斯安威斯坦 | `sandevistan_c2` | 18 | L71 | L71–L74 | `TIME_SLOW`=0.5, `DURATION`=9, `COOLDOWN`=40, `CRIT_CHANCE`=15, `CRIT_DAMAGE`=15 | `TIME_SLOW`=L72，`DURATION`=L73，`COOLDOWN`=L73，`CRIT_CHANCE`=L72，`CRIT_DAMAGE`=L72 |
| 60 | 千替斯安威斯坦 | `sandevistan_c3` | 14 | L67 | L67–L70 | `TIME_SLOW`=0.2, `DURATION`=10, `COOLDOWN`=60 | `TIME_SLOW`=L68，`DURATION`=L69，`COOLDOWN`=L69 |
| 61 | 摩尔科技狂暴 | `berserk_c1` | 12 | L45 | L45–L49 | `DURATION`=11, `COOLDOWN`=35, `KILL_HEAL`=25 | `DURATION`=L48，`COOLDOWN`=L48，`KILL_HEAL`=L47 |
| 62 | 生物动力狂暴 | `berserk_c2` | 20 | L40 | L40–L44 | `CRIT_CHANCE`=20, `CRIT_DAMAGE`=100, `DURATION`=11, `COOLDOWN`=35, `KILL_HEAL`=25 | `CRIT_CHANCE`=同区间，`CRIT_DAMAGE`=L41，`DURATION`=L43，`COOLDOWN`=L43，`KILL_HEAL`=L42 |
| 63 | 泽塔科技狂暴 | `berserk_c3` | 16 | L50 | L50–L54 | `DURATION`=11, `COOLDOWN`=25, `KILL_HEAL`=25 | `DURATION`=L53，`COOLDOWN`=L53，`KILL_HEAL`=L52 |
| 64 | 基础歧路司义眼 | `kiroshi_optics_bare` | 1 | L169 | L169–L171 | （空） | （无 stats） |
| 65 | 歧路司义眼1型 | `kiroshi_optics` | 1 | L169 | L169–L171 | （空） | （无 stats） |
| 66 | 歧路司义眼神舆 | `kiroshi_optics_combined` | 10 | L189 | L189–L193 | （空） | （无 stats） |
| 67 | 歧路司义眼祸兆 | `kiroshi_optics_hunter` | 2 | L179 | L179–L181 | （空） | （无 stats） |
| 68 | 歧路司义眼千里目 | `kiroshi_optics_wallhack` | 8 | L172 | L172–L174 | （空） | （无 stats） |
| 69 | 智能连接 | `smart_link` | 4 | L246 | L246–L250 | `RAM`=2, `CRIT_DAMAGE`=15 | `RAM`=L250，`CRIT_DAMAGE`=L248 |
| 70 | 弹道协同处理器 | `power_grip` | 2 | L235 | L235–L236 | （空） | （无 stats） |
| 71 | 握柄固定套 | `knife_sharpener` | 8 | L237 | L237 | `CRIT_CHANCE`=27, `DURATION`=6 | `CRIT_CHANCE`=L237，`DURATION`=L237 |
| 72 | 微发电机 | `micro_generator` | 9 | L242 | L242–L243 | （空） | （无 stats） |
| 73 | 省力减震 | `joint_lock` | 12 | L244 | L244–L245 | `RECOIL`=-24 | `RECOIL`=L244 |
| 74 | 生物监测 | `biomonitor` | 14 | L291 | L291–L292 | （空） | （无 stats） |
| 75 | 副心脏 | `second_heart` | 30 | L305 | L305–L306 | `COOLDOWN`=200 | `COOLDOWN`=L306 |
| 76 | 击杀治疗 | `heal_on_kill` | 10 | L303 | L303 | `KILL_HEAL`=7.5 | `KILL_HEAL`=L303 |
| 77 | 黑曼巴 | `viral_venom` | 16 | L293 | L293 | `ALL_DAMAGE`=22 | `ALL_DAMAGE`=L293 |
| 78 | 风险归避 | `catch_me_if_you_can` | 5 | L307 | L307–L308 | （空） | （无 stats） |
| 79 | 活血泵 | `blood_pump` | 15 | L294 | L294–L295 | `DURATION`=6 | `DURATION`=L295 |
| 80 | 反馈电路 | `discharge_connector` | 14 | L299 | L299–L300 | （空） | （无 stats） |
| 81 | 肾上腺素增强件 | `stamina_regen_booster` | 14 | L289 | L289–L290 | （空） | （无 stats） |
| 82 | 微型转子 | `cyber_rotors` | 12 | L304 | L304 | （空） | （无 stats） |
| 83 | 强化肌健 | `boosted_tendons` | 8 | L373 | L373–L375 | `ARMOR`=10 | `ARMOR`=L373 |
| 84 | 猞猁爪 | `cat_paws` | 5 | L368 | L368–L372 | `ARMOR`=34 | `ARMOR`=L368 |
| 85 | 火车王肌建 | `jenkins_tendons` | 6 | L360 | L360–L363 | `ARMOR`=54 | `ARMOR`=L360 |
| 86 | 踝部加固 | `reinforced_muscles` | 6 | L356 | L356–L359 | `ARMOR`=103 | `ARMOR`=L356 |
| 87 | 思想防线 | `cogito_frame` | 12 | L318 | L318–L319 | `ARMOR`=54 | `ARMOR`=同区间 |
| 88 | 细胞适配 | `adaptive_stem_cells` | 8 | L313 | L313–L314 | `ARMOR`=162 | `ARMOR`=L313 |
| 89 | 皮下护甲 | `boring_plating` | 4 | L352 | L352–L353 | `ARMOR`=49 | `ARMOR`=同区间 |
| 90 | 近接防盾 | `proximity_reducer` | 8 | L339 | L339–L342 | `ARMOR`=36 | `ARMOR`=L339 |
| 91 | 疼痛编辑器 | `pain_reductor` | 28 | L335 | L335–L336 | `ARMOR`=108 | `ARMOR`=L335 |
| 92 | 疼痛置换 | `blood_depleter` | 24 | L337 | L337–L338 | `ARMOR`=138 | `ARMOR`=L337 |
| 93 | 拒敌防护 | `charge_system` | 14 | L348 | L348–L349 | `ARMOR`=34 | `ARMOR`=同区间 |
| 94 | 反制壳层 | `sudden_aid` | 10 | L320 | L320–L322 | `ARMOR`=57 | `ARMOR`=L320 |
| 95 | 震慑通电 | `electroshock_mechanism` | 20 | L350 | L350–L351 | `ARMOR`=92 | `ARMOR`=L350 |
| 96 | 光学迷彩 | `optical_camo` | 16 | L330 | L330–L334 | `ARMOR`=40 | `ARMOR`=L330 |
| 97 | 纳米镀层 | `nano_tech_plates` | 16 | L325 | L325–L329 | `ARMOR`=78 | `ARMOR`=L325 |
| 98 | 全域覆盖 | `weird_tanky_plating` | 13 | L311 | L311–L312 | `ARMOR`=94 | `ARMOR`=同区间 |
| 99 | 克伦齐科夫回护 | `plating_glitch` | 16 | L323 | L323–L324 | `ARMOR`=80 | `ARMOR`=L323 |
| 100 | 锥刺子 | `oil_dispenser` | 12 | L280 | L280–L281 | `CRIT_CHANCE`=20 | `CRIT_CHANCE`=L280 |
| 101 | 克伦齐科夫 | `kerenzikov` | 12 | L268 | L268–L270 | `TIME_SLOW`=0.6, `DURATION`=3.75, `COOLDOWN`=6 | `TIME_SLOW`=L269，`DURATION`=L269，`COOLDOWN`=L270 |
| 102 | 突触加速器 | `synaptic_accelerator` | 5 | L282 | L282–L283 | `TIME_SLOW`=0.5, `DURATION`=4.5, `COOLDOWN`=60 | `TIME_SLOW`=L282，`DURATION`=L282，`COOLDOWN`=L283 |
| 103 | 纳米纤维 | `neo_fiber` | 14 | L271 | L271–L273 | （空） | （无 stats） |
| 104 | 视觉皮质支持 | `visual_cortex_support` | 20 | L265 | L265 | `CRIT_CHANCE`=30 | `CRIT_CHANCE`=L265 |
| 105 | 肾上腺素整流 | `detector_rush` | 6 | L259 | L259 | `DURATION`=9 | `DURATION`=L259 |
| 106 | 瞬时感知 | `trouble_finder` | 5 | L262 | L262–L264 | （空） | （无 stats） |
| 107 | 酪氨酸注射器 | `tyrosine_injector` | 8 | L284 | L284–L286 | `HEADSHOT_DAMAGE`=21, `DURATION`=15 | `HEADSHOT_DAMAGE`=同区间，`DURATION`=L286 |
| 108 | 死不鸟 | `neuro_matrix` | 13 | L210 | L210–L211 | `ARMOR`=32 | `ARMOR`=L210 |
| 109 | 弹性关节 | `agile_joints` | 13 | L227 | L227–L228 | `ARMOR`=34 | `ARMOR`=L227 |
| 110 | 致密骨骼 | `dense_marrow` | 13 | L205 | L205–L207 | `ARMOR`=54, `MELEE_DAMAGE`=27 | `ARMOR`=L205，`MELEE_DAMAGE`=L206 |
| 111 | 再造骨骼 | `endoskeleton` | 32 | L208 | L208–L209 | `ARMOR`=186, `MAX_HEALTH`=15 | `ARMOR`=L208，`MAX_HEALTH`=L209 |
| 112 | RAM补偿 | `compiling_skeleton` | 11 | L218 | L218–L221 | `ARMOR`=46, `RAM`=2 | `ARMOR`=L218，`RAM`=同区间 |
| 113 | 清创凝合 | `no_pain_no_gain` | 16 | L222 | L222–L224 | `ARMOR`=34 | `ARMOR`=同区间 |
| 114 | 动能骨架 | `bone_marrow_cells` | 13 | L212 | L212–L213 | `ARMOR`=117 | `ARMOR`=L212 |
| 115 | 圣甲虫 | `rapid_muscle_nurish` | 11 | L225 | L225–L226 | `ARMOR`=34 | `ARMOR`=同区间 |
| 116 | 百拳开 | `t1000` | 20 | L214 | L214–L215 | `ARMOR`=150 | `ARMOR`=同区间 |
| 117 | 钛金骨骼 | `titanium_infused_bones` | 5 | L229 | L229–L230 | （空） | （无 stats） |
| 118 | 卡米略RAM管理器 | `camillo_ram_manager` | 10 | L16 | L16–L18 | `RAM`=2, `COOLDOWN`=80 | `RAM`=同区间，`COOLDOWN`=L18 |
| 119 | 外接盘 | `ex_disk` | 10 | L19 | L19 | `RAM`=6 | `RAM`=L19 |
| 120 | 克伦齐科夫增幅 | `kerenziov_boost_system` | 3 | L20 | L20–L21 | `TIME_SLOW`=0.15 | `TIME_SLOW`=L20 |
| 121 | 内存加强 | `memory_boost` | 18 | L23 | L23 | `RAM`=1 | `RAM`=L23 |
| 122 | RAM升级 | `ram_upgrade` | 8 | L29 | L29 | `RAM`=2, `RAM_REGEN`=0.2 | `RAM`=L29，`RAM_REGEN`=L29 |
| 123 | 自我ICE | `self_ice` | 5 | L30 | L30 | `RAM`=2, `COOLDOWN`=20 | `RAM`=L30，`COOLDOWN`=L30 |

合计 **123** 条保留定义（= `grep` 到的 register 数 123）。

### 3.1 条件性数值标注（第 0 节规则 6）

| id | 条件 | 所取数值 | 描述.txt 行 |
|---|---|---|---|
| `sandevistan_c1` | 「激活时处于空中」分支（文档另给地面分支：时间减慢30%、+3%~12%伤害） | time_slow 0.6 / all_damage 24 / headshot_damage 40 | L75–L79 |
| `berserk_c4` | 「生命值低于20%时达到最大值」 | all_damage 50 | L34–L39 |
| `berserk_c1` | 「+35%~50%伤害减免」为激活期条件减伤 | （无对应键，见 4.2） | L45–L49 |
| `projectile_launcher` | 「充能射击提供 +30%伤害」 | all_damage 30 | L155–L160 |
| `projectile_launcher_chemical` | 同上 | all_damage 30 | L155–L160 |
| `projectile_launcher_electric` | 同上 | all_damage 30 | L155–L160 |
| `projectile_launcher_thermal` | 同上 | all_damage 30 | L155–L160 |
| `viral_venom` | 「对受到中毒效果影响的敌人」 | all_damage 22 | L293 |
| `mechatronic_core` | 「对无人机、机器人、机甲和炮塔」 | all_damage 40 | L22 |
| `iconic_visual_cortex_support` | 「距离敌人越远，暴击率越高（最多在85~100米处）」 | crit_chance 100 | L266–L267 |
| `visual_cortex_support` | 「距离敌人越远…（最多在30米处）」 | crit_chance 30 | L265 |
| `knife_sharpener` | 「装备或投掷一把投掷武器时，持续6秒」 | crit_chance 27 / duration 6 | L237 |
| `smart_link` | 「智能武器+5%~15%暴击伤害」 | crit_damage 15 | L246–L250 |
| `oil_dispenser` | 「刀剑和投掷武器」 | crit_chance 20 | L280–L281 |
| `cogito_frame` | 「可用RAM低于2~10时+200%~240%护甲」为条件值，未映射；取「提供护甲」平值 | armor 54 | L318–L319 |
| `t1000` | 「+8%~13%护甲」为条件外另一条同键数值，取「提供护甲：30~150」 | armor 150 | L214–L215 |

---

## 4. 查不到数值 / 保留 TODO 的清单

### 4.1 描述.txt 里查不到该物品 —— 保留 `TODO(数值与描述待补)` 原文

| 常量名 | id | 中文名 | 现状 |
|---|---|---|---|
| `SMART_STORAGE` | `smart_storage` | 蓄电缓冲 | 容量 8、`stats()` 为空、描述串保留 `蓄电缓冲。TODO(数值与描述待补)`；`描述.txt` 全 374 行无「蓄电缓冲」，`ID对照.txt` L123 有该物品（AdvancedSmartStorageLegendary） |

合计 **1** 条。其余 122 条保留定义全部在 `描述.txt` 中定位到条目并填了真实数值。

（`ASSET-MISSING.txt` 第 17 条也是 `smart_storage`，即它既无素材、也无数值来源，两头都缺。）

### 4.2 保留定义里「描述.txt 有、但没有对应 Stats 键」的效果（未映射，只在描述串里）

这些不是查不到，是**没有键可放**；键定义在 `CyberwareDefinition.Stats`，该文件不在本任务范围，未新增。

| 效果类别 | 涉及定义（举例） | 描述.txt 行 |
|---|---|---|
| 义体容量加成（+40~70义体容量） | `capacity_booster` | L137 |
| 半伤几率 / 半伤强度 | `neo_fiber`、`iconic_proximity_reducer`、`iconic_gun_stabilizer` 等 | L271–L272、L346 |
| 移动速度 / 攻击速度 | `berserk_c1`~`c4`、`catch_me_if_you_can`、`detector_rush` 等 | L36、L41、L307、L261 |
| 伤害减免 / 元素抗性 | `berserk_c1`~`c3`、`iconic_proximity_reducer` | L41、L46、L51、L346 |
| 属性检定 / 负重 | `strong_arms_*`、`titanium_infused_bones` | L140、L229 |
| 流血/燃烧/电击/中毒几率 | `strong_arms_*`、`mantis_blades*`、`nano_wires*`、`max_tac_mantis_blades` | L142、L147、L163、L152 |
| 光学变焦 / 高亮距离（义眼） | `kiroshi_optics*`（7 条） | L169–L193 |
| 耐力消耗 | `iconic_shock_absorber`、`joint_lock`、`shock_absorber` | L298、L244、L296 |
| 跳弹伤害 | `power_grip` | L235 |
| 摄像头发现速度 / 识别度 | `kiroshi_optics_bare`、`optical_camo` | L169、L331 |

---

## 5. 容量分布（辅助核验，仅覆盖能对上 `描述.txt` 的 104 条；另 19 条旧定义沿用《义体拓展》，容量见第 3 节）

| 容量 | 条数 |
|---|---|
| 0 | 2 |
| 1 | 2 |
| 2 | 2 |
| 3 | 1 |
| 4 | 2 |
| 5 | 6 |
| 6 | 4 |
| 8 | 23 |
| 9 | 1 |
| 10 | 5 |
| 11 | 2 |
| 12 | 6 |
| 13 | 5 |
| 14 | 8 |
| 15 | 1 |
| 16 | 8 |
| 18 | 2 |
| 20 | 6 |
| 24 | 2 |
| 25 | 1 |
| 28 | 1 |
| 30 | 2 |
| 32 | 1 |
| 33 | 1 |
| 35 | 3 |
| 36 | 1 |
| 39 | 1 |
| 40 | 3 |
| 44 | 1 |
| 50 | 1 |

合计 104 条。


注：`描述.txt` 里「所需义体容量」原文为 0 的共 6 条 —— `capacity_booster`(L137)、`max_tac_mantis_blades`(L151)、`mask_cw_plus_plus`(L195)、`yakuza_tattoo`(L251)、`silverhand_tattoo`(L254)、`casius_tattoo`(L254)；其中前 2 条在白名单内（本表已生效），后 4 条无素材已注释。0 是原文值，不是缺失值。

---

## 6. 版本外决策与待办（需 captain / 其它通道处理，均不在本任务范围）

1. **`CyberwareDefinition.java` L17 的 Javadoc 仍拿 `sandevistan_zetatech` 当例子**，该 id 已被本任务删除。按范围约束未改该文件，请 captain 指派处理。
2. **`Stats` 缺键**：第 4.2 节列的 10 类效果在 `Stats` 里没有键（`CyberwareDefinition.java` 的 `Stats` 类）。要真正落地这些数值，需要在 `Stats` 中新增键（例如 `capacity_bonus`、`half_damage_chance`、`half_damage_strength`、`move_speed`、`attack_speed`、`damage_reduction`、`zoom`、`highlight_range`、`bleed_chance`、`stamina_cost`）。
3. **`Stats.ARMOR` 单位冲突**：`描述.txt` 给的是平值「提供护甲：X~Y」，`Stats.ARMOR` 注释写「护甲加成（%）」。本表按原文存平值。需要么改注释口径，要么为平值护甲另立键。
4. **`Stats.RAM_REGEN` 单位冲突**：`描述.txt` L29 原文「RAM恢复速率**每秒**增加0.05~0.2个单位」，而键注释写「每分钟」。`ram_upgrade` 按原文存了 0.2。
5. **`描述.txt` L209 的区间写法缺了 `~`**：原文逐字为「+10%+15%最大生命值」。本表按区间规则取第二个数字 15。请主人确认该写法是否等同 `+10%~15%`。
6. **早期 19 条网络接入仓保留原值**：它们在白名单内（有素材），但命名与 `描述.txt` 不是逐条对应（如 `cyberdeck_arasaka_4`「荒坂4型」vs `描述.txt` L84「荒坂1~5型」；`cyberdeck_netwatch_5` vs `描述.txt` L113「网络监察网驱1型」），故其容量与数值沿用《义体拓展》文档，本任务未改写。若主人要求这些也改用 `描述.txt` 口径，需另行裁决。
7. **`ASSET-WHITELIST.txt` / `ASSET-MISSING.txt` 里的「定义表Lxxx」行号是基线（改动前）行号**，与本任务改完后的行号不同，核验时请以 id 为准。

### 6.1 【阻塞构建 · 需 captain 指派】2 处调用点仍引用已删除常量 `SANDEVISTAN_ZETATECH`

全仓扫描（`grep -rn "\(CyberwareDefinitions\.\)\?<12 个已删常量>" --include=*.java src/main/java`，排除本表）：**只有下面 2 处**，且都是 `SANDEVISTAN_ZETATECH`。删掉的那 12 个常量里，其余 11 个全仓没有任何引用。

| 文件:行 | 现状 | 建议改法 |
|---|---|---|
| `src/main/java/com/dsh/cyberware/client/CyberwareStationScreen.java:57` | `private CyberwareDefinition selectedDef = CyberwareDefinitions.SANDEVISTAN_ZETATECH;` | 把 `SANDEVISTAN_ZETATECH` 换成 `CyberwareDefinitions.SANDEVISTAN_C4` |
| `src/main/java/com/dsh/cyberware/CyberwareCommands.java:35` | `CyberwareDefinition def = CyberwareDefinitions.SANDEVISTAN_ZETATECH;` | 同上换成 `CyberwareDefinitions.SANDEVISTAN_C4` |

这两个文件都不在本任务独占范围内（本人只许改 `core/CyberwareDefinitions.java`），故**未改动**，只在此记录。
coredev（t4）已在 /tmp/t4-verify 的逐字节副本上把这两行改成 `SANDEVISTAN_C4` 并跑通 `gradle compileJava`（BUILD SUCCESSFUL），即这两行是唯一的编译阻塞。

**不建议**在 `CyberwareDefinitions` 里补一个 `SANDEVISTAN_ZETATECH = SANDEVISTAN_C4` 的旧名别名：主人裁决是「删原有的 12 条、保留新增的官方命名」，别名会把已裁决废弃的旧名重新留在 API 上，等于把这次改名做成表面文章。

### 6.2 【运行时风险 · 不阻塞编译 · 需 captain 指派】义眼描边清单含 2 个已注释 id

`src/main/java/com/dsh/cyberware/mixin/client/EntityRendererOutlineMixin.java` L76–L77 的字符串清单里有 `"kiroshi_optics_piercing"` 与 `"kiroshi_optics_sensor"`，这两个 id 已被第 2 步整块注释（它们在 `ASSET-MISSING.txt` 第 6、7 条）。
影响：编译无碍（字符串常量），但这两条义体不会被注册，描边判断永远取不到它们 —— 功能上等于这两件义眼没有描边。
处理建议（二选一，由该文件责任人决定）：把 L76–L77 两行删掉；或保留（等价于「这两件眼暂无描边」，与「无素材暂不注册」一致）。
该文件也不在本任务独占范围内，未改动。

---

## 7. 验证记录（未跑 gradle）

```
$ grep -c "^    public static final CyberwareDefinition.*register(new CyberwareDefinition(" CyberwareDefinitions.java
123
$ grep -c "register(new CyberwareDefinition(" CyberwareDefinitions.java   # 含 19 条注释块
142
$ grep -c "// 待素材：暂无贴图素材，暂不注册（见 ASSET-MISSING.txt）" CyberwareDefinitions.java
19
$ javac -encoding UTF-8 <CyberwareDefinition+CyberwareRarity+CyberwareSlot+CyberwareDefinitions>
JAVAC OK — 语法通过，且所有 Stats.<KEY> 常量解析成功（键名写错会在此报错）
```

`javac` 用的是 /usr/lib/jvm/java-21-openjdk-arm64 的 `javac 21.0.12`，只编译这 4 个同包文件（**不是** gradle 全量构建），用于验证语法与 `Stats` 键名。

