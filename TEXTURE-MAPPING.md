# TEXTURE-MAPPING.md —— t2 素材归档映射表

任务：t2 [assets] 贴图归档。全部贴图入库前统一 `Image.LANCZOS` 缩放为 **32x32 RGBA**（主人硬要求）。

行号基准：`定义表L<n>` 取自 t1 baseline 提交 `b0aabeeabbc1f2dcb46e649a72c9736678b1bb9d` 的 `CyberwareDefinitions.java`（154 条，含主人已裁决删除的 12 条）。numbersmith 并行删除那 12 条后，当前文件为 142 条 = 本表白名单 123 条 + 无素材 19 条，行号已整体偏移，引用请按 id 对齐而非行号。`ID对照L<n>` / `待插入L<n>` / `描述L<n>` / `完整名称L<n>` 均为 /sdcard 上游文件的当前行号（未改动）。

出处缩写：`定义表L<n>` = `src/main/java/com/dsh/cyberware/core/CyberwareDefinitions.java` 行号；`待插入L<n>` = `/sdcard/DSH/义体/待插入-123条定义.txt` 行号（123 条定义来源文档）；`ID对照L<n>` = `/sdcard/DSH/义体/ID对照.txt`（CET 物品 ID → 中文名）；`描述L<n>` = `/sdcard/DSH/义体/描述.txt`；`lang` = `assets/cyberware/lang/en_us.json` 官方英文名；`完整名称L<n>` = `/sdcard/DSH/斯安威斯坦/完整名称.txt`。

素材总量（实测，非任务书估计值）：义体/ 84 个图形文件（83 png + 1 webp）；斯安威斯坦/ 9 png；网络接入舱/ 11 png —— 合计 **104** 个图形文件。任务书写的 81+9+11=102 与实测差 2（义体/ 实际 83 png），以实测为准。

## ① 义体/ 素材映射

| 素材文件 | 原图尺寸 | 目标定义 id（一图多用时会列出多条） | 槽位 | 出处 |
|---|---|---|---|---|
| `COX-2_赛博生体优化.png` | 170x170 | `iconic_bio_conductors` | FRONTAL_CORTEX | 定义表L468 ID对照L22 名称逐字相同 |
| `RAM加强.png` | 160x170 | `memory_boost` | FRONTAL_CORTEX | 定义表L1154 ID对照L120 主人已确认异写 RAM加强=内存加强 |
| `RAM升级.png` | 200x200 | `ram_upgrade` | FRONTAL_CORTEX | 定义表L1161 ID对照L121 名称逐字相同 |
| `RAM外接盘.png` | 160x170 | `ex_disk` | FRONTAL_CORTEX | 定义表L1133 ID对照L117 指派：RAM外接盘→外接盘 |
| `RAM补偿.png` | 200x200 | `compiling_skeleton` | SKELETON | 定义表L1063 ID对照L107 名称逐字相同 |
| `一拳开.png` | 200x200 | `iconic_t1000` / `t1000` | SKELETON | iconic_t1000=定义表L447/ID对照L19 逐字相同；t1000=定义表L1105/ID对照L113 主人已确认异写 一拳开=百拳开；一图多用 |
| `不死鸟.png` | 200x200 | `neuro_matrix` | SKELETON | 定义表L1035 ID对照L103 主人已确认异写 不死鸟=死不鸟 |
| `乖离排异.png` | 200x200 | `iconic_reflex_recorder` | NERVOUS_SYSTEM | 定义表L440 ID对照L18 名称逐字相同 |
| `光学迷彩.png` | 200x200 | `optical_camo` | INTEGUMENTARY | 定义表L944 ID对照L90 名称逐字相同 |
| `克伦齐科夫.png` | 160x170 | `kerenzikov` | NERVOUS_SYSTEM | 定义表L979 ID对照L95 名称逐字相同 |
| `克伦齐科夫回护.png` | 200x200 | `plating_glitch` | INTEGUMENTARY | 定义表L965 ID对照L93 名称逐字相同 |
| `克伦齐科夫增幅.png` | 200x200 | `kerenziov_boost_system` | FRONTAL_CORTEX | 定义表L1140 ID对照L118 名称逐字相同 |
| `全域覆盖.png` | 200x200 | `weird_tanky_plating` | INTEGUMENTARY | 定义表L958 ID对照L92 名称逐字相同 |
| `再造骨骼.png` | 200x200 | `endoskeleton` | SKELETON | 定义表L1056 ID对照L106 名称逐字相同 |
| `几丁质壳.png` | 200x200 | `iconic_chiton` | INTEGUMENTARY | 定义表L412 ID对照L14 主人已确认异写 几丁质壳=几质丁壳 |
| `击杀治疗.png` | 160x170 | `heal_on_kill` | CIRCULATORY | 定义表L797 ID对照L69 名称逐字相同 |
| `副心脏.png` | 168x168 | `second_heart` | CIRCULATORY | 定义表L790 ID对照L68 名称逐字相同 |
| `加固脚踝.png` | 168x168 | `reinforced_muscles` | LEGS | 定义表L874 ID对照L80 主人已确认字序异写 加固脚踝=踝部加固 |
| `动能骨架.png` | 163x170 | `bone_marrow_cells` | SKELETON | 定义表L1084 ID对照L110 名称逐字相同 |
| `单分子线.png` | 160x170 | `nano_wires` / `nano_wires_chemical` / `nano_wires_electric` / `nano_wires_thermal` | ARMS | nano_wires=定义表L531/ID对照L31 逐字相同；三元素变体=定义表L517/ID对照L29、定义表L524/ID对照L30、定义表L538/ID对照L32；一图多用 |
| `卡米略RAM管理器.png` | 177x170 | `camillo_ram_manager` / `iconic_camillo_ram_manager` | FRONTAL_CORTEX | camillo_ram_manager=定义表L1126/ID对照L116 逐字相同；iconic_camillo_ram_manager=定义表L475/ID对照L23 主人已确认异写 卡米略RAM管理器=RAM配平；一图多用 |
| `反制壳层.png` | 200x200 | `sudden_aid` | INTEGUMENTARY | 定义表L930 ID对照L88 名称逐字相同 |
| `反馈电路.png` | 200x200 | `discharge_connector` | CIRCULATORY | 定义表L832 ID对照L74 名称逐字相同 |
| `圣甲虫.png` | 200x200 | `rapid_muscle_nurish` | SKELETON | 定义表L1098 ID对照L112 名称逐字相同 |
| `坚矛利盾.png` | 200x200 | `iconic_gun_stabilizer` | ARMS | 定义表L384 ID对照L10 名称逐字相同 |
| `基础歧路司义眼.png` | 160x170 | `kiroshi_optics_bare` / `kiroshi_optics` | FACE | kiroshi_optics_bare=定义表L678/ID对照L52 逐字相同；kiroshi_optics=定义表L685/ID对照L53 主人已确认异写 基础歧路司义眼=歧路司义眼1型；一图多用 |
| `外周逆反.png` | 200x200 | `iconic_proximity_reducer` | INTEGUMENTARY | 定义表L419 ID对照L15 名称逐字相同 |
| `大猩猩手臂.png` | 160x170 | `strong_arms` / `strong_arms_chemical` / `strong_arms_electric` / `strong_arms_thermal` | ARMS | strong_arms=定义表L587/ID对照L39 逐字相同；三元素变体=定义表L573/ID对照L37、定义表L580/ID对照L38、定义表L594/ID对照L40；一图多用 |
| `射弹发射系统.png` | 160x170 | `projectile_launcher` / `projectile_launcher_chemical` / `projectile_launcher_electric` / `projectile_launcher_thermal` | ARMS | projectile_launcher=定义表L559/ID对照L35 主人已确认异写 射弹=弹射；三元素变体=定义表L545/ID对照L33、定义表L552/ID对照L34、定义表L566/ID对照L36；一图多用 |
| `弹性关节.png` | 160x170 | `agile_joints` | SKELETON | 定义表L1042 ID对照L104 名称逐字相同 |
| `弹道协同处理器.png` | 166x170 | `power_grip` | ARMS | 定义表L734 ID对照L60 名称逐字相同 |
| `省力减震.png` | 200x200 | `joint_lock` | ARMS | 定义表L755 ID对照L63 名称逐字相同 |
| `强化肌腱.png` | 163x170 | `boosted_tendons` | LEGS | 定义表L853 ID对照L77 主人已确认异写 强化肌腱=强化肌健 |
| `微发电机.png` | 160x170 | `micro_generator` | ARMS | 定义表L748 ID对照L62 名称逐字相同 |
| `微型转子.png` | 200x200 | `cyber_rotors` | CIRCULATORY | 定义表L846 ID对照L76 名称逐字相同 |
| `思想防线.png` | 200x200 | `cogito_frame` | INTEGUMENTARY | 定义表L881 ID对照L81 名称逐字相同 |
| `拒敌防护.png` | 200x200 | `charge_system` | INTEGUMENTARY | 定义表L923 ID对照L87 名称逐字相同 |
| `握柄固定套.png` | 200x200 | `knife_sharpener` | ARMS | 定义表L741 ID对照L61 逐字相同；排除 shock_absorber(持握衬垫,定义表L825/ID对照L73)：描述L237 握柄固定套=投掷武器暴击，描述L296 持握衬垫=射击耐力消耗，描述表列为两件不同物品 |
| `智能链接.png` | 160x170 | `smart_link` | ARMS | 定义表L727 ID对照L59 主人已确认异写 智能链接=智能连接 |
| `歧路司义眼“千里目”.png` | 200x200 | `kiroshi_optics_wallhack` | FACE | 定义表L720 ID对照L58 名称逐字相同 |
| `歧路司义眼“石化鸡蛇”.png` | 200x200 | `iconic_advanced_kiroshi_optics_bare` | FACE | 定义表L370 ID对照L8 名称逐字相同 |
| `歧路司义眼“神谕”.png` | 200x200 | `kiroshi_optics_combined` | FACE | 定义表L692 ID对照L54 定义名作「神舆」；描述L193 与 待插入L366 上游文档同一物品写作「神谕」，为同音异写；候选 kiroshi_optics_piercing(追猎)/_sensor(警戒) 语义不符已排除 |
| `歧路司义眼“祸兆”.png` | 200x200 | `kiroshi_optics_hunter` | FACE | 定义表L699 ID对照L55 名称逐字相同 |
| `活血泵.png` | 200x200 | `blood_pump` | CIRCULATORY | 定义表L818 ID对照L72 名称逐字相同 |
| `清创凝合.png` | 200x200 | `no_pain_no_gain` | SKELETON | 定义表L1070 ID对照L108 名称逐字相同 |
| `火车王肌腱.png` | 200x200 | `jenkins_tendons` | LEGS | 定义表L867 ID对照L79 主人已确认异写 火车王肌腱=火车王肌建 |
| `灭团韧带.png` | 200x200 | `iconic_jenkins_tendons` | LEGS | 定义表L405 ID对照L13 主人已确认异写 灭团韧带=团灭韧带 |
| `猞猁爪.png` | 166x170 | `cat_paws` | LEGS | 定义表L860 ID对照L78 名称逐字相同 |
| `生物监测.png` | 200x200 | `biomonitor` | CIRCULATORY | 定义表L783 ID对照L67 名称逐字相同 |
| `疼痛编辑器.png` | 160x170 | `pain_reductor` | INTEGUMENTARY | 定义表L909 ID对照L85 名称逐字相同 |
| `疼痛置换.png` | 200x200 | `blood_depleter` | INTEGUMENTARY | 定义表L916 ID对照L86 名称逐字相同 |
| `皮下护甲.png` | 160x170 | `boring_plating` | INTEGUMENTARY | 定义表L895 ID对照L83 名称逐字相同 |
| `瞬时感知.png` | 200x200 | `trouble_finder` | NERVOUS_SYSTEM | 定义表L1021 ID对照L101 逐字相同；排除 reflex_recorder(反应协调器,定义表L986/ID对照L96)：描述L262 瞬时感知有独立描述条目，反应协调器在描述表无条目且非同一物品名 |
| `突触加速器.png` | 160x170 | `synaptic_accelerator` | NERVOUS_SYSTEM | 定义表L993 ID对照L97 名称逐字相同 |
| `等距稳定.png` | 200x200 | `iconic_shock_absorber` | CIRCULATORY | 定义表L398 ID对照L12 逐字相同；shock_absorber 的正确中文名是「持握衬垫」(定义表L825/ID对照L73)，与 iconic_shock_absorber「等距稳定」是两条独立定义，本图只对应等距稳定 |
| `纳米纤维.png` | 160x170 | `neo_fiber` | NERVOUS_SYSTEM | 定义表L1000 ID对照L98 名称逐字相同 |
| `纳米镀层.png` | 200x200 | `nano_tech_plates` | INTEGUMENTARY | 定义表L951 ID对照L91 名称逐字相同 |
| `细胞适配.png` | 93x88 | `adaptive_stem_cells` | INTEGUMENTARY | 定义表L888 ID对照L82 名称逐字相同 |
| `肾上腺素增强件.png` | 168x168 | `stamina_regen_booster` | CIRCULATORY | 定义表L839 ID对照L75 名称逐字相同 |
| `肾上腺素整流.png` | 200x200 | `detector_rush` | NERVOUS_SYSTEM | 定义表L1014 ID对照L100 名称逐字相同 |
| `自我ICE.png` | 200x200 | `self_ice` | FRONTAL_CORTEX | 定义表L1168 ID对照L122 名称逐字相同 |
| `致密骨骼.webp` | 166x170 | `dense_marrow` | SKELETON | 定义表L1049 ID对照L105 名称逐字相同；webp 经 Pillow 转 32x32 png |
| `螳螂刀.png` | 160x150 | `mantis_blades` / `max_tac_mantis_blades` / `mantis_blades_chemical` / `mantis_blades_electric` / `mantis_blades_thermal` | ARMS | mantis_blades=定义表L503/ID对照L27 逐字相同；暴恐机动队螳螂刀=定义表L482/ID对照L24、三元素变体=定义表L489/ID对照L25、定义表L496/ID对照L26、定义表L510/ID对照L28；一图多用 |
| `装植缩减.png` | 200x200 | `capacity_booster` | OPERATING_SYSTEM | 定义表L328 ID对照L2 主人已确认异写 装植缩减=装殖缩减；描述L130 上游文档另作「装植减缩」，同一物品三处书写不同 |
| `视觉皮层支持.png` | 160x170 | `visual_cortex_support` | NERVOUS_SYSTEM | 定义表L1007 ID对照L99 定义名「视觉皮质支持」，皮层/皮质同义；排除 iconic_visual_cortex_support(长焦可视界面,定义表L426/ID对照L16)：描述L265 视觉皮质支持 vs 描述L266 长焦可视界面 为两条独立条目，本图名与前者对应 |
| `近接防盾.png` | 200x200 | `proximity_reducer` | INTEGUMENTARY | 定义表L902 ID对照L84 名称逐字相同 |
| `酪氨酸注射器.png` | 200x200 | `tyrosine_injector` | NERVOUS_SYSTEM | 定义表L1028 ID对照L102 名称逐字相同 |
| `钛金骨骼.png` | 200x200 | `titanium_infused_bones` | SKELETON | 定义表L1112 ID对照L114 名称逐字相同 |
| `锥刺子.png` | 200x200 | `oil_dispenser` | NERVOUS_SYSTEM | 定义表L972 ID对照L94 名称逐字相同 |
| `震慑通电.png` | 162x170 | `electroshock_mechanism` | INTEGUMENTARY | 定义表L937 ID对照L89 名称逐字相同 |
| `风险规避.png` | 200x200 | `catch_me_if_you_can` | CIRCULATORY | 定义表L811 ID对照L71 主人已确认异写 风险规避=风险归避 |
| `黑曼巴.png` | 200x200 | `viral_venom` | CIRCULATORY | 定义表L804 ID对照L70 名称逐字相同 |

## ② 斯安威斯坦/ 与 网络接入舱/ 素材映射（含原图尺寸）

### 斯安威斯坦/（9 张，全部映射到「新增官方命名」id；旧 id 已按主人裁决删除）

| 素材文件 | 原图尺寸 | 新 id | 原旧 id（已删）与改名依据 | 出处 |
|---|---|---|---|---|
| `泽塔科技斯安威斯坦.png` | 165x170 | `sandevistan_c1` | 定义表L22 旧 id sandevistan_zetatech 改为 sandevistan_c1（主人裁决删旧定义、保留新增官方命名） | 定义表L636 待插入L310 ID对照L46；描述L63 泽塔科技斯安威斯坦；完整名称.txt L1 |
| `迪纳拉斯安威斯坦.png` | 160x170 | `sandevistan_c2` | 定义表L35 旧 id sandevistan_dynalar 改名为 sandevistan_c2；主人已确认异写 迪纳拉斯=迪纳拉 | 定义表L643 待插入L317 ID对照L47；描述L58 迪纳拉斯安威斯坦；完整名称.txt L2 |
| `千替“扭曲实境”斯安威斯坦.png` | 200x200 | `sandevistan_c3` | 定义表L50 旧 id sandevistan_qiantai 改名为 sandevistan_c3 | 定义表L650 待插入L324 ID对照L48；描述L54 千替“实境扭曲”斯安威斯坦（扭曲/实境 字序异写，同一型号）；完整名称.txt L3 |
| `军用科技“游隼”.png` | 400x400 | `sandevistan_c4` | 定义表L63 旧 id sandevistan_militech_falcon 改名为 sandevistan_c4 | 定义表L356 待插入L30 ID对照L6；描述L46 军用科技“游隼”斯安威斯坦；完整名称.txt L4（后缀 x=神话级） |
| `400px-军用科技斯安威斯坦“顶点”.png` | 400x400 | `sandevistan_apogee` | 定义表L73 旧 id sandevistan_militech_apogee 改名；主人已确认异写 顶点=远地点 | 定义表L349 待插入L23 ID对照L5；描述L42 军用科技“远地点”斯安威斯坦；完整名称.txt L5 |
| `摩尔科技狂暴.png` | 200x200 | `berserk_c1` | 定义表L283 旧 id berserk_moore 改名为 berserk_c1 | 定义表L657 待插入L331 ID对照L49；描述L49 摩尔科技狂暴；完整名称.txt L8 |
| `生物动力狂暴.png` | 200x200 | `berserk_c2` | 定义表L297 旧 id berserk_biodyne 改名为 berserk_c2 | 定义表L664 待插入L338 ID对照L50；描述L40 生物动力狂暴；完整名称.txt L7 |
| `泽塔科技狂暴.png` | 200x200 | `berserk_c3` | 定义表L318 旧 id berserk_zetatech 改名为 berserk_c3 | 定义表L671 待插入L345 ID对照L51；描述L51 泽塔科技狂暴；完整名称.txt L9 |
| `军用科技狂暴.png` | 200x200 | `berserk_c4` | 定义表L308 旧 id berserk_militech 改名为 berserk_c4；client/BerserkHud.java 引用由 coredev 同步 | 定义表L363 待插入L37 ID对照L7；描述L36 军用科技狂暴；完整名称.txt L10（后缀 x=神话级） |

### 网络接入舱/（11 张）

| 素材文件 | 原图尺寸 | 目标定义 id | 判定理由（含 3 张新 id + 4 张多义素材） | 出处 |
|---|---|---|---|---|
| `军用科技平行线.png` | 162x164 | `militech_paraline_mkv` | 定义表L92 旧 id cyberdeck_militech_parallel 已裁决删除，资源同步清理 | 定义表L601 待插入L275 ID对照L41；描述L107 军用科技平行线1~5型；lang en_us 旧 id cyberdeck_militech_parallel="Militech Paraline" |
| `生物技术Σ.png` | 200x200 | `biotech_sigma_mkiv` / `cyberdeck_biotech_1` / `cyberdeck_biotech_2` | 同线共用：cyberdeck_biotech_1=定义表L150/lang"Biotech Sigma Mk.1"、cyberdeck_biotech_2=定义表L158/lang"Biotech Sigma Mk.2"，仓库内二者贴图 md5 相同(c2968e8c…)，故同图覆盖；cyberdeck_biotech_3 已裁决删除 | biotech_sigma_mkiv=定义表L615/待插入L289/ID对照L43；描述L92 生物技术Σ 1~4型；lang en_us cyberdeck_biotech_1/2="Biotech Sigma Mk.1/2" 同一产品线 |
| `军用科技“篇章”.png` | 200x200 | `haunted_cyberdeck` | 定义表L252 旧 id cyberdeck_militech_canto_6 中文名同为「军用科技篇章6型」，已裁决删除；新素材只建 haunted_cyberdeck | 定义表L335 待插入L9 ID对照L3；描述L98 军用科技篇章6型；lang en_us 旧 id cyberdeck_militech_canto_6="Militech Canto Mk.6"（Canto=篇章） |
| `乌鸦微控.png` | 200x200 | `cyberdeck_raven_4` / `raven_microcyber_mkiii` | 同线共用：两条定义同为乌鸦微控(Raven Microcyber)产品线，仓库 cyberdeck_raven_4.png 为唯一既有贴图(md5 ea1d8a5e…) | cyberdeck_raven_4=定义表L273/lang"Raven Microcyber Mk.4"（任务指派目标）；raven_microcyber_mkiii=定义表L622/待插入L296/ID对照L44；描述L121 乌鸦微控1~3型 同一产品线 |
| `四相传电.png` | 160x170 | `cyberdeck_ripple_4` / `cyberdeck_tetratronic_1` / `cyberdeck_tetratronic_2` / `cyberdeck_tetratronic_3` / `tetratronic_rippler_mkv` | 同线共用：cyberdeck_ripple_4 与 cyberdeck_tetratronic_1/2/3 仓库贴图 md5 全部相同(85da6f6f…)，同一品牌图；tetratronic_rippler_mkv 为新定义，同线覆盖 | cyberdeck_ripple_4=定义表L227/lang"Tetratronic Rippler Mk.4"（任务指派目标）；tetratronic_rippler_mkv=定义表L608/待插入L282/ID对照L42；描述L126 四相传电涟漪1~5型 同一产品线；lang en_us tetratronic_1/2/3="Tetratronic Cyberdeck Mk.1/2/3" |
| `网络监察网驱.png` | 200x200 | `cyberdeck_netwatch_5` / `netwatch_netdriver_mk` | 同线共用：两条定义同为 Netwatch Netdriver 产品线 | cyberdeck_netwatch_5=定义表L265/lang"Netwatch Netdriver Mk.5"（任务指派目标）；netwatch_netdriver_mk=定义表L342/待插入L16/ID对照L4；描述L114 网络监察网驱1型 同一产品线 |
| `荒板.png` | 192x192 | `cyberdeck_arasaka_4` / `cyberdeck_arasaka_3` / `arasaka_shadow_mkv` | 同线共用：cyberdeck_arasaka_3=定义表L236 与 _4 仓库贴图 md5 相同(f1a1bbbb…)；arasaka_shadow_mkv 为新定义，同线覆盖 | cyberdeck_arasaka_4=定义表L244/lang"Arasaka Cyberdeck Mk.4"（任务指派目标）；arasaka_shadow_mkv=定义表L629/待插入L303/ID对照L45；描述L84 荒坂1~5型 同一产品线 |
| `冬月修补匠.png` | 110x106 | `cyberdeck_dongyue_1` / `cyberdeck_tinkerer_3` | 同线共用：素材名「冬月修补匠」同时含 冬月(Dongyue) 与 修补匠(Tinkerer)，两条定义仓库贴图 md5 相同(f57fa4f0…)，无法也不应二选一，同图覆盖二者 | cyberdeck_dongyue_1=定义表L100/lang"Dongyue Cyberdeck Mk.1"；cyberdeck_tinkerer_3=定义表L108/lang"Tinkerer Cyberdeck Mk.3"；描述L120「冬月电子修补匠被移除后唯二的不朽网络接入仓」——「冬月电子修补匠」= Dongyue + Tinkerer 同一产品线的合成叫法 |
| `斯蒂文森技术.png` | 165x170 | `cyberdeck_technica_2` / `cyberdeck_technica_3` / `cyberdeck_technica_4` | 同线共用：2/3/4 型为同一产品线的三个等级，官方英文名均为 Stephenson Tech，素材无法区分等级，同图覆盖三者 | cyberdeck_technica_2/3/4=定义表L176/定义表L185/定义表L194，lang en_us = "Stephenson Tech Mk.2/3/4"（斯蒂文森技术=Stephenson Tech，官方英文名直接对上）；仓库内三者贴图 md5 相同(511d8421…) |
| `瑞草电子.png` | 160x170 | `cyberdeck_ruicao_1` / `cyberdeck_ruicao_2` | 同线共用：素材名「瑞草电子」不含 1型/2型 等级信息，两条定义仓库贴图 md5 相同(46064687…)，同图覆盖二者 | cyberdeck_ruicao_1=定义表L117/lang"Ruicao Cyberdeck Mk.1"；cyberdeck_ruicao_2=定义表L125/lang"Ruicao Cyberdeck Mk.2"；素材名只给品牌不含等级 |
| `生物动力.png` | 160x170 | `cyberdeck_biodyne_1` / `cyberdeck_biodyne_2` | 同线共用：素材名「生物动力」不含 1型/2型 等级信息，两条定义仓库贴图 md5 相同(3231da90…)，同图覆盖二者 | cyberdeck_biodyne_1=定义表L134/lang"Biodyne Cyberdeck Mk.1"；cyberdeck_biodyne_2=定义表L142/lang"Biodyne Cyberdeck Mk.2"；素材名只给品牌不含等级 |

### 同线共用（一图多用）的客观依据：仓库既有贴图 md5

| 产品线 | 仓库既有贴图 md5 | 覆盖的 id |
|---|---|---|
| 冬月电子/修补匠 | `f57fa4f0ff84783cf06b2fb5377c3955` | cyberdeck_dongyue_1、cyberdeck_tinkerer_3 |
| 斯蒂文森技术 | `511d8421d47f43a3f22e8fd86dc6c285` | cyberdeck_technica_2、_3、_4 |
| 瑞草电子 | `46064687bcd37395c82cdb24bf64743a` | cyberdeck_ruicao_1、_2 |
| 生物动力 | `3231da903e2f3fd29034bbc9b97ab983` | cyberdeck_biodyne_1、_2 |
| 四相传电/涟漪 | `85da6f6f45eb681ae537f0a7496d6446` | cyberdeck_ripple_4、cyberdeck_tetratronic_1、_2、_3 |
| 荒坂 | `f1a1bbbb0976cf1a0ca9776685ebb532` | cyberdeck_arasaka_3、_4 |
| 生物技术Σ | `c2968e8ce1e43a1ea03a9914ed4641b1` | cyberdeck_biotech_1、_2、_3（_3 已删） |

同线各 id 的既有贴图逐字节相同，说明仓库本来就按「一条产品线一张图」渲染；素材名不含等级信息（瑞草电子/生物动力/斯蒂文森技术/冬月修补匠），无法从图像区分 `_1/_2/_3/_4`，因此同图覆盖整条产品线的全部保留 id，既满足「每个 id 都要有贴图」，也不改变既有视觉约定。

## ③ 未映射清单

### ③-1 有素材但定义表中无对应条目（未入库，原图保留在 /sdcard/DSH/义体/）

这 12 个文件在 `定义表 CyberwareDefinitions.java`（154 条）、`ID对照.txt`（126 条）、`待插入-123条定义.txt`（123 条）、`描述.txt` 中均无对应条目；没有定义就没有物品，入库会造成孤儿资源，故不入库，等待主人裁决是否新增定义。

| 素材文件 | 排除理由 |
|---|---|
| `代谢编辑器.png` | 循环系统(CET Metabolic Editor，属主人素材集但 123 条定义表中无此条) |
| `全幅抵抗.png` | 定义表中无同名/近义条目 |
| `反向电感.png` | 定义表中无同名条目；最接近候选「反馈电路」已有来自 反馈电路.png 的独立素材，且非同一名称 |
| `合成肺叶.png` | 定义表中无同名条目 |
| `接地镀层.png` | 定义表中无同名条目；候选「纳米镀层」已有来自 纳米镀层.png 的独立素材 |
| `热能转化器.png` | 定义表中无同名条目 |
| `生物塑料血管.png` | 定义表中无同名条目；候选「生物导体」语义/字形均不同 |
| `真皮上编束.png` | 定义表中无同名条目 |
| `纳米继电器.png` | 定义表中无同名条目；候选「纳米镀层」「纳米纤维」均已有各自素材 |
| `解毒器.png` | 定义表中无同名条目 |
| `边缘增强系统.png` | 定义表中无同名条目 |
| `防火涂层.png` | 定义表中无同名条目 |

### ③-2 无素材的定义

共 19 条，逐条排除过程见 `ASSET-MISSING.txt`：

- `mask_cw_plus_plus`（行为特征脸板，FACE，ID对照L9；描述L194作「行为特征同步脸板」）
- `iconic_discharge_connector`（电磁回收，CIRCULATORY，ID对照L11；描述L301）
- `iconic_detector_rush`（肾上腺导引，NERVOUS_SYSTEM，ID对照L17；描述L260）
- `time_bank`（量子调谐，FRONTAL_CORTEX，ID对照L20；描述L24）
- `iconic_subdermal_co_processor`（皮下变色，FRONTAL_CORTEX，ID对照L21；描述L5）
- `kiroshi_optics_piercing`（歧路司义眼追猎，FACE，ID对照L56；描述L188）
- `kiroshi_optics_sensor`（歧路司义眼警戒，FACE，ID对照L57；描述L187）
- `yakuza_tattoo`（纹身虎爪帮，ARMS，ID对照L64；描述L251）
- `silverhand_tattoo`（纹身永远在一起，ARMS，ID对照L65；描述L254）
- `casius_tattoo`（纹身强尼特制，ARMS，ID对照L66；描述L254）
- `shock_absorber`（持握衬垫，CIRCULATORY，ID对照L73；描述L296）
- `iconic_visual_cortex_support`（长焦可视界面，NERVOUS_SYSTEM，ID对照L16；描述L266）
- `reflex_recorder`（反应协调器，NERVOUS_SYSTEM，ID对照L96；描述表无该条目）
- `pain_distributor`（通用增强件，SKELETON，ID对照L109；描述L231）
- `bionic_joints`（仿生关节，SKELETON，ID对照L111；描述L202）
- `mechatronic_core`（机电核心，FRONTAL_CORTEX，ID对照L119；描述L22）
- `smart_storage`（蓄电缓冲，FRONTAL_CORTEX，ID对照L123）
- `subdermal_co_processor`（牛顿模块，FRONTAL_CORTEX，ID对照L124；描述L7）
- `bio_conductors`（生物导体，FRONTAL_CORTEX，ID对照L115；描述L11）

### ③-3 主人裁决删除的 12 条旧定义（资源已清理；定义已由 numbersmith 同步删除）

- `sandevistan_zetatech`（泽塔科技·斯安威斯坦）
- `sandevistan_dynalar`（迪纳拉·斯安威斯坦）
- `sandevistan_qiantai`（千替“实境扭曲”·斯安威斯坦）
- `sandevistan_militech_falcon`（军用科技“游隼”·斯安威斯坦）
- `sandevistan_militech_apogee`（军用科技“远地点”·斯安威斯坦）
- `berserk_moore`（摩尔科技狂暴）
- `berserk_biodyne`（生物动力狂暴）
- `berserk_zetatech`（泽塔科技狂暴）
- `berserk_militech`（军用科技狂暴）
- `cyberdeck_militech_parallel`（军用科技平行线）
- `cyberdeck_biotech_3`（生物技术3型）
- `cyberdeck_militech_canto_6`（军用科技篇章6型）

复核：numbersmith 已在其并行任务中删除这 12 条定义，当前 `CyberwareDefinitions.java` 为 142 条 = 白名单 123 + 无素材 19，白名单 123 条 id 在定义表中全部存在、12 条已删 id 在定义表中全部消失（核验命令见本次 t2 上报的 commandsRun）。

## 附录 A · 本次归档的实测数字

| 项目 | 实测值 | 说明 |
|---|---|---|
| 素材图形文件总数 | 104 | 义体/ 84（83 png + 1 webp）、斯安威斯坦/ 9、网络接入舱/ 11；任务书写 102，差 2 |
| 入库贴图（32x32） | 123 | `textures/item/<定义id>.png`，其中 104 张属新建 id、19 张覆盖既有 cyberdeck 贴图 |
| 新建 `models/item/*.json` | 104 | 结构与 `berserk_militech.json` 一致，仅改 texture 层 |
| 新建 `items/*.json` | 104 | 结构与 `berserk_militech.json` 一致 |
| 未入库素材 | 12 | 见 ③-1，原图保留在 /sdcard/DSH/义体/ 未改动 |
| 清理的旧资源 | 36 个文件（12 id × 3） | 备份在 `/sdcard/DSH/trash/cyberware-removed/{textures-item,models-item,items}/`，与 git HEAD 逐字节一致 |
| 删除的 lang 条目 | 24（2 文件 × 12） | `item.cyberware.<已删 id>`，用 python3 json 读写 |

## 附录 B · 遗留问题（交给 captain / inspector 裁决）

1. **123 条新定义缺 lang 条目**：`lang/zh_cn.json` 与 `lang/en_us.json` 各只剩 19 条 `item.cyberware.*`（既有 cyberdeck id）。`CyberwareItem` 未覆写 `getName()`，物品名走 `item.cyberware.<id>` 翻译键，因此这 123 个物品在游戏内会显示为未翻译键。zh_cn 的名称可从定义表逐字转录（零编造）；**en_us 无官方英文译名来源**（ID对照.txt 只有 CET 物品 ID，不是可直出的英文名），禁止编造，故本次未擅自新增，等主人/队长裁决后补。
2. **12 个素材无对应定义**（③-1）：代谢编辑器、全幅抵抗、反向电感、合成肺叶、接地镀层、热能转化器、生物塑料血管、真皮上编束、纳米继电器、解毒器、边缘增强系统、防火涂层。主人素材集里有图、三份上游文档（ID对照/待插入/描述）里都没有条目。是否新增 12 条定义由主人裁决；未入库前原文件安全留在 /sdcard/DSH/义体/。
3. **19 条定义无素材**（③-2）：没有贴图会渲染成紫黑缺失贴图，请 numbersmith 决定去留。

