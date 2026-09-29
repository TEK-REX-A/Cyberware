# LANG-AND-NEW-ASSETS.md —— zh_cn 物品名补录（123 条）+ 12 件新义体资源（t8 / archivist）

定义表基准：`src/main/java/com/dsh/cyberware/core/CyberwareDefinitions.java`，生成时 md5 `e4ad444c9b4413ed4c56148a6242c72b`，
当前 **register 调用 154 个 = 生效 135 个 + `//` 注释块 19 个**（那 19 个无素材、编号见 `ASSET-MISSING.txt`，不注册，未加 lang）。

本任务只动 `resources/assets/cyberware/{textures/item,models/item,items,lang}`，未改任何 `.java`。

## ① zh_cn 物品名：123 条生效定义（id → 中文名 → 定义表行号）

结果：`lang/zh_cn.json` 从 49 条目 / 19 个 `item.cyberware.*` 变为 **165 条目 / 135 个 `item.cyberware.*`**。
其中：原有 19 条（旧 cyberdeck，值未改）+ 本次新增 116 条（第一轮 104 条 + 第二轮 12 条）。
每条生效定义恰好一条 lang，且值逐字等于定义表中文名：核验 135/135 一致、0 条缺失、0 条悬空。
`lang/en_us.json` **未动**（49 条目 / 19 个 item 键，md5 `db559925b90bf73e22eda3b17053136b`）。

| # | id | zh_cn 值（=定义表中文名） | 槽位 | 定义表行号 | 本轮来源 |
|---|---|---|---|---|---|
| 1 | `cyberdeck_dongyue_1` | 冬月电子1型 | OPERATING_SYSTEM | L36 | 原有条目（值已核验与定义表一致） |
| 2 | `cyberdeck_tinkerer_3` | 修补匠3型 | OPERATING_SYSTEM | L44 | 原有条目（值已核验与定义表一致） |
| 3 | `cyberdeck_ruicao_1` | 瑞草电子1型 | OPERATING_SYSTEM | L53 | 原有条目（值已核验与定义表一致） |
| 4 | `cyberdeck_ruicao_2` | 瑞草电子2型 | OPERATING_SYSTEM | L61 | 原有条目（值已核验与定义表一致） |
| 5 | `cyberdeck_biodyne_1` | 生物动力1型 | OPERATING_SYSTEM | L70 | 原有条目（值已核验与定义表一致） |
| 6 | `cyberdeck_biodyne_2` | 生物动力2型 | OPERATING_SYSTEM | L78 | 原有条目（值已核验与定义表一致） |
| 7 | `cyberdeck_biotech_1` | 生物技术1型 | OPERATING_SYSTEM | L86 | 原有条目（值已核验与定义表一致） |
| 8 | `cyberdeck_biotech_2` | 生物技术2型 | OPERATING_SYSTEM | L94 | 原有条目（值已核验与定义表一致） |
| 9 | `cyberdeck_technica_2` | 泰克重工技术2型 | OPERATING_SYSTEM | L103 | 原有条目（值已核验与定义表一致） |
| 10 | `cyberdeck_technica_3` | 泰克重工技术3型 | OPERATING_SYSTEM | L112 | 原有条目（值已核验与定义表一致） |
| 11 | `cyberdeck_technica_4` | 泰克重工技术4型 | OPERATING_SYSTEM | L121 | 原有条目（值已核验与定义表一致） |
| 12 | `cyberdeck_tetratronic_1` | 四相传电1型 | OPERATING_SYSTEM | L130 | 原有条目（值已核验与定义表一致） |
| 13 | `cyberdeck_tetratronic_2` | 四相传电2型 | OPERATING_SYSTEM | L138 | 原有条目（值已核验与定义表一致） |
| 14 | `cyberdeck_tetratronic_3` | 四相传电3型 | OPERATING_SYSTEM | L146 | 原有条目（值已核验与定义表一致） |
| 15 | `cyberdeck_ripple_4` | 涟漪4型 | OPERATING_SYSTEM | L154 | 原有条目（值已核验与定义表一致） |
| 16 | `cyberdeck_arasaka_3` | 荒坂3型 | OPERATING_SYSTEM | L163 | 原有条目（值已核验与定义表一致） |
| 17 | `cyberdeck_arasaka_4` | 荒坂4型 | OPERATING_SYSTEM | L171 | 原有条目（值已核验与定义表一致） |
| 18 | `cyberdeck_netwatch_5` | 网络监察网驱5型 | OPERATING_SYSTEM | L179 | 原有条目（值已核验与定义表一致） |
| 19 | `cyberdeck_raven_4` | 乌鸦微控4型 | OPERATING_SYSTEM | L187 | 原有条目（值已核验与定义表一致） |
| 20 | `capacity_booster` | 装殖缩减 | OPERATING_SYSTEM | L197 | t8 新增 |
| 21 | `haunted_cyberdeck` | 军用科技篇章6型 | OPERATING_SYSTEM | L204 | t8 新增 |
| 22 | `netwatch_netdriver_mk` | 网络监察网驱1型 | OPERATING_SYSTEM | L211 | t8 新增 |
| 23 | `sandevistan_apogee` | 军用科技斯安威斯坦”远地点“ | OPERATING_SYSTEM | L218 | t8 新增 |
| 24 | `sandevistan_c4` | 军用科技斯安威斯坦”游隼“ | OPERATING_SYSTEM | L227 | t8 新增 |
| 25 | `berserk_c4` | 军用科技狂暴 | OPERATING_SYSTEM | L236 | t8 新增 |
| 26 | `iconic_advanced_kiroshi_optics_bare` | 歧路司义眼石化鸡蛇 | FACE | L244 | t8 新增 |
| 27 | `iconic_gun_stabilizer` | 坚矛利盾 | ARMS | L259 | t8 新增 |
| 28 | `iconic_shock_absorber` | 等距稳定 | CIRCULATORY | L274 | t8 新增 |
| 29 | `iconic_jenkins_tendons` | 团灭韧带 | LEGS | L281 | t8 新增 |
| 30 | `iconic_chiton` | 几质丁壳 | INTEGUMENTARY | L288 | t8 新增 |
| 31 | `iconic_proximity_reducer` | 外周逆反 | INTEGUMENTARY | L295 | t8 新增 |
| 32 | `iconic_reflex_recorder` | 乖离排异 | NERVOUS_SYSTEM | L318 | t8 新增 |
| 33 | `iconic_t1000` | 一拳开 | SKELETON | L326 | t8 新增 |
| 34 | `iconic_bio_conductors` | COX-2赛博生体优化 | FRONTAL_CORTEX | L349 | t8 新增 |
| 35 | `iconic_camillo_ram_manager` | RAM配平 | FRONTAL_CORTEX | L356 | t8 新增 |
| 36 | `max_tac_mantis_blades` | 暴恐机动队螳螂刀 | ARMS | L363 | t8 新增 |
| 37 | `mantis_blades_chemical` | 剧毒螳螂刀 | ARMS | L370 | t8 新增 |
| 38 | `mantis_blades_electric` | 放电螳螂刀 | ARMS | L377 | t8 新增 |
| 39 | `mantis_blades` | 螳螂刀 | ARMS | L384 | t8 新增 |
| 40 | `mantis_blades_thermal` | 热能螳螂刀 | ARMS | L391 | t8 新增 |
| 41 | `nano_wires_chemical` | 剧毒单分子线 | ARMS | L398 | t8 新增 |
| 42 | `nano_wires_electric` | 放电单分子线 | ARMS | L405 | t8 新增 |
| 43 | `nano_wires` | 单分子线 | ARMS | L412 | t8 新增 |
| 44 | `nano_wires_thermal` | 热能单分子线 | ARMS | L419 | t8 新增 |
| 45 | `projectile_launcher_chemical` | 剧毒弹射发射系统 | ARMS | L426 | t8 新增 |
| 46 | `projectile_launcher_electric` | 电子弹射发射系统 | ARMS | L433 | t8 新增 |
| 47 | `projectile_launcher` | 弹射发射系统 | ARMS | L440 | t8 新增 |
| 48 | `projectile_launcher_thermal` | 热能弹射发射系统 | ARMS | L447 | t8 新增 |
| 49 | `strong_arms_chemical` | 剧毒大猩猩手臂 | ARMS | L454 | t8 新增 |
| 50 | `strong_arms_electric` | 放电大猩猩手臂 | ARMS | L461 | t8 新增 |
| 51 | `strong_arms` | 大猩猩手臂 | ARMS | L468 | t8 新增 |
| 52 | `strong_arms_thermal` | 热能大猩猩手臂 | ARMS | L475 | t8 新增 |
| 53 | `militech_paraline_mkv` | 军用科技平行线 | OPERATING_SYSTEM | L482 | t8 新增 |
| 54 | `tetratronic_rippler_mkv` | 四相传电涟漪5型 | OPERATING_SYSTEM | L490 | t8 新增 |
| 55 | `biotech_sigma_mkiv` | 生物技术3型 | OPERATING_SYSTEM | L497 | t8 新增 |
| 56 | `raven_microcyber_mkiii` | 乌鸦微控3型 | OPERATING_SYSTEM | L506 | t8 新增 |
| 57 | `arasaka_shadow_mkv` | 荒板5型 | OPERATING_SYSTEM | L514 | t8 新增 |
| 58 | `sandevistan_c1` | 泽塔科技斯安威斯坦 | OPERATING_SYSTEM | L523 | t8 新增 |
| 59 | `sandevistan_c2` | 迪娜拉斯安威斯坦 | OPERATING_SYSTEM | L532 | t8 新增 |
| 60 | `sandevistan_c3` | 千替斯安威斯坦 | OPERATING_SYSTEM | L541 | t8 新增 |
| 61 | `berserk_c1` | 摩尔科技狂暴 | OPERATING_SYSTEM | L549 | t8 新增 |
| 62 | `berserk_c2` | 生物动力狂暴 | OPERATING_SYSTEM | L557 | t8 新增 |
| 63 | `berserk_c3` | 泽塔科技狂暴 | OPERATING_SYSTEM | L566 | t8 新增 |
| 64 | `kiroshi_optics_bare` | 基础歧路司义眼 | FACE | L574 | t8 新增 |
| 65 | `kiroshi_optics` | 歧路司义眼1型 | FACE | L581 | t8 新增 |
| 66 | `kiroshi_optics_combined` | 歧路司义眼神舆 | FACE | L588 | t8 新增 |
| 67 | `kiroshi_optics_hunter` | 歧路司义眼祸兆 | FACE | L595 | t8 新增 |
| 68 | `kiroshi_optics_wallhack` | 歧路司义眼千里目 | FACE | L618 | t8 新增 |
| 69 | `smart_link` | 智能连接 | ARMS | L625 | t8 新增 |
| 70 | `power_grip` | 弹道协同处理器 | ARMS | L632 | t8 新增 |
| 71 | `knife_sharpener` | 握柄固定套 | ARMS | L639 | t8 新增 |
| 72 | `micro_generator` | 微发电机 | ARMS | L646 | t8 新增 |
| 73 | `joint_lock` | 省力减震 | ARMS | L653 | t8 新增 |
| 74 | `biomonitor` | 生物监测 | CIRCULATORY | L684 | t8 新增 |
| 75 | `second_heart` | 副心脏 | CIRCULATORY | L691 | t8 新增 |
| 76 | `heal_on_kill` | 击杀治疗 | CIRCULATORY | L698 | t8 新增 |
| 77 | `viral_venom` | 黑曼巴 | CIRCULATORY | L705 | t8 新增 |
| 78 | `catch_me_if_you_can` | 风险归避 | CIRCULATORY | L712 | t8 新增 |
| 79 | `blood_pump` | 活血泵 | CIRCULATORY | L719 | t8 新增 |
| 80 | `discharge_connector` | 反馈电路 | CIRCULATORY | L734 | t8 新增 |
| 81 | `stamina_regen_booster` | 肾上腺素增强件 | CIRCULATORY | L741 | t8 新增 |
| 82 | `cyber_rotors` | 微型转子 | CIRCULATORY | L748 | t8 新增 |
| 83 | `boosted_tendons` | 强化肌健 | LEGS | L755 | t8 新增 |
| 84 | `cat_paws` | 猞猁爪 | LEGS | L762 | t8 新增 |
| 85 | `jenkins_tendons` | 火车王肌建 | LEGS | L769 | t8 新增 |
| 86 | `reinforced_muscles` | 踝部加固 | LEGS | L776 | t8 新增 |
| 87 | `cogito_frame` | 思想防线 | INTEGUMENTARY | L783 | t8 新增 |
| 88 | `adaptive_stem_cells` | 细胞适配 | INTEGUMENTARY | L790 | t8 新增 |
| 89 | `boring_plating` | 皮下护甲 | INTEGUMENTARY | L797 | t8 新增 |
| 90 | `proximity_reducer` | 近接防盾 | INTEGUMENTARY | L804 | t8 新增 |
| 91 | `pain_reductor` | 疼痛编辑器 | INTEGUMENTARY | L811 | t8 新增 |
| 92 | `blood_depleter` | 疼痛置换 | INTEGUMENTARY | L818 | t8 新增 |
| 93 | `charge_system` | 拒敌防护 | INTEGUMENTARY | L825 | t8 新增 |
| 94 | `sudden_aid` | 反制壳层 | INTEGUMENTARY | L832 | t8 新增 |
| 95 | `electroshock_mechanism` | 震慑通电 | INTEGUMENTARY | L839 | t8 新增 |
| 96 | `optical_camo` | 光学迷彩 | INTEGUMENTARY | L846 | t8 新增 |
| 97 | `nano_tech_plates` | 纳米镀层 | INTEGUMENTARY | L853 | t8 新增 |
| 98 | `weird_tanky_plating` | 全域覆盖 | INTEGUMENTARY | L860 | t8 新增 |
| 99 | `plating_glitch` | 克伦齐科夫回护 | INTEGUMENTARY | L867 | t8 新增 |
| 100 | `oil_dispenser` | 锥刺子 | NERVOUS_SYSTEM | L874 | t8 新增 |
| 101 | `kerenzikov` | 克伦齐科夫 | NERVOUS_SYSTEM | L881 | t8 新增 |
| 102 | `synaptic_accelerator` | 突触加速器 | NERVOUS_SYSTEM | L898 | t8 新增 |
| 103 | `neo_fiber` | 纳米纤维 | NERVOUS_SYSTEM | L906 | t8 新增 |
| 104 | `visual_cortex_support` | 视觉皮质支持 | NERVOUS_SYSTEM | L913 | t8 新增 |
| 105 | `detector_rush` | 肾上腺素整流 | NERVOUS_SYSTEM | L920 | t8 新增 |
| 106 | `trouble_finder` | 瞬时感知 | NERVOUS_SYSTEM | L927 | t8 新增 |
| 107 | `tyrosine_injector` | 酪氨酸注射器 | NERVOUS_SYSTEM | L934 | t8 新增 |
| 108 | `neuro_matrix` | 死不鸟 | SKELETON | L941 | t8 新增 |
| 109 | `agile_joints` | 弹性关节 | SKELETON | L948 | t8 新增 |
| 110 | `dense_marrow` | 致密骨骼 | SKELETON | L955 | t8 新增 |
| 111 | `endoskeleton` | 再造骨骼 | SKELETON | L962 | t8 新增 |
| 112 | `compiling_skeleton` | RAM补偿 | SKELETON | L969 | t8 新增 |
| 113 | `no_pain_no_gain` | 清创凝合 | SKELETON | L976 | t8 新增 |
| 114 | `bone_marrow_cells` | 动能骨架 | SKELETON | L991 | t8 新增 |
| 115 | `rapid_muscle_nurish` | 圣甲虫 | SKELETON | L1006 | t8 新增 |
| 116 | `t1000` | 百拳开 | SKELETON | L1013 | t8 新增 |
| 117 | `titanium_infused_bones` | 钛金骨骼 | SKELETON | L1020 | t8 新增 |
| 118 | `camillo_ram_manager` | 卡米略RAM管理器 | FRONTAL_CORTEX | L1035 | t8 新增 |
| 119 | `ex_disk` | 外接盘 | FRONTAL_CORTEX | L1042 | t8 新增 |
| 120 | `kerenziov_boost_system` | 克伦齐科夫增幅 | FRONTAL_CORTEX | L1049 | t8 新增 |
| 121 | `memory_boost` | 内存加强 | FRONTAL_CORTEX | L1064 | t8 新增 |
| 122 | `ram_upgrade` | RAM升级 | FRONTAL_CORTEX | L1071 | t8 新增 |
| 123 | `self_ice` | 自我ICE | FRONTAL_CORTEX | L1078 | t8 新增 |

> 本表 123 行；`item.cyberware.*` 键集合与 135 条生效定义集合完全相等（脚本断言）。

## ② 12 件新义体资源（素材 → 32x32 贴图 + models/item + items + lang）

id 来源：`/root/mod26/cyberware/12-NEW-DEFS.md`（numbersmith / t7）§2「id 定稿清单」，与定义表实际注册的 12 个 id **逐字 12/12 一致**；
素材中文名与定义表中文名逐字相等（12/12），映射无歧义。

| # | 素材（/sdcard/DSH/义体/） | 原图尺寸 | 目标 id | 中文名 | 槽位 | 定义表行号 | 生成文件 |
|---|---|---|---|---|---|---|---|
| 1 | `代谢编辑器.png` | 162x170 | `metabolic_editor` | 代谢编辑器 | CIRCULATORY | L1105 | `textures/item/metabolic_editor.png`(32x32) + `models/item/metabolic_editor.json` + `items/metabolic_editor.json` + `item.cyberware.metabolic_editor` |
| 2 | `全幅抵抗.png` | 163x170 | `full_resistance` | 全幅抵抗 | INTEGUMENTARY | L1113 | `textures/item/full_resistance.png`(32x32) + `models/item/full_resistance.json` + `items/full_resistance.json` + `item.cyberware.full_resistance` |
| 3 | `反向电感.png` | 162x170 | `reverse_inductor` | 反向电感 | CIRCULATORY | L1121 | `textures/item/reverse_inductor.png`(32x32) + `models/item/reverse_inductor.json` + `items/reverse_inductor.json` + `item.cyberware.reverse_inductor` |
| 4 | `合成肺叶.png` | 160x170 | `synthetic_lung` | 合成肺叶 | CIRCULATORY | L1129 | `textures/item/synthetic_lung.png`(32x32) + `models/item/synthetic_lung.json` + `items/synthetic_lung.json` + `item.cyberware.synthetic_lung` |
| 5 | `接地镀层.png` | 160x170 | `grounding_plating` | 接地镀层 | INTEGUMENTARY | L1137 | `textures/item/grounding_plating.png`(32x32) + `models/item/grounding_plating.json` + `items/grounding_plating.json` + `item.cyberware.grounding_plating` |
| 6 | `热能转化器.png` | 166x170 | `thermal_converter` | 热能转化器 | CIRCULATORY | L1145 | `textures/item/thermal_converter.png`(32x32) + `models/item/thermal_converter.json` + `items/thermal_converter.json` + `item.cyberware.thermal_converter` |
| 7 | `生物塑料血管.png` | 162x170 | `bioplastic_vessels` | 生物塑料血管 | CIRCULATORY | L1153 | `textures/item/bioplastic_vessels.png`(32x32) + `models/item/bioplastic_vessels.json` + `items/bioplastic_vessels.json` + `item.cyberware.bioplastic_vessels` |
| 8 | `真皮上编束.png` | 160x170 | `dermal_weave` | 真皮上编束 | INTEGUMENTARY | L1161 | `textures/item/dermal_weave.png`(32x32) + `models/item/dermal_weave.json` + `items/dermal_weave.json` + `item.cyberware.dermal_weave` |
| 9 | `纳米继电器.png` | 160x170 | `nano_relay` | 纳米继电器 | NERVOUS_SYSTEM | L1169 | `textures/item/nano_relay.png`(32x32) + `models/item/nano_relay.json` + `items/nano_relay.json` + `item.cyberware.nano_relay` |
| 10 | `解毒器.png` | 160x170 | `detoxifier` | 解毒器 | CIRCULATORY | L1177 | `textures/item/detoxifier.png`(32x32) + `models/item/detoxifier.json` + `items/detoxifier.json` + `item.cyberware.detoxifier` |
| 11 | `边缘增强系统.png` | 163x170 | `edge_enhancement_system` | 边缘增强系统 | NERVOUS_SYSTEM | L1185 | `textures/item/edge_enhancement_system.png`(32x32) + `models/item/edge_enhancement_system.json` + `items/edge_enhancement_system.json` + `item.cyberware.edge_enhancement_system` |
| 12 | `防火涂层.png` | 166x170 | `fireproof_coating` | 防火涂层 | INTEGUMENTARY | L1193 | `textures/item/fireproof_coating.png`(32x32) + `models/item/fireproof_coating.json` + `items/fireproof_coating.json` + `item.cyberware.fireproof_coating` |

三件套结构：`models/item/<id>.json` = `{"parent":"minecraft:item/generated","textures":{"layer0":"cyberware:item/<id>"}}`；
`items/<id>.json` = `{"model":{"type":"minecraft:model","model":"cyberware:item/<id>"}}`（与既有 `berserk_militech.json` 同构）。

## ③ 未完成项 / 残留风险

- **无挂起项**：①② 均已完成。任务书规定的「12-NEW-DEFS.md 缺失则挂起②」条件在实施时已不成立 —— 动手前该文件未出现，但 12 个 id 已由 numbersmith 写入定义表；随后 12-NEW-DEFS.md 出现，其 §2 清单与已落盘 id 12/12 一致，无需返工。
- zh_cn 的 12 条新值取自素材中文名，与定义表中文名逐字相同（12/12 断言通过），未新增任何文字。
- `lang/en_us.json` 仍只有 19 个 `item.cyberware.*`：那 12 件与 104 条新定义在英文环境下显示未翻译键。主人已裁决只补中文，故保持现状。
- 12 条新定义的槽位/稀有度/容量/数值在定义表里是 TODO 占位值（numbersmith 的 `12-NEW-DEFS.md` §8 列出待主人裁决），与本次资源归档无关，本任务未触碰 `.java`。
- 12 张素材原件在 `/sdcard/DSH/义体/` 未改名、未移动、未修改（只读读取）。

