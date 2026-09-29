package com.dsh.cyberware.core;

import com.dsh.cyberware.core.CyberwareDefinition.Stats;
import com.dsh.cyberware.core.CyberwareDefinition.Variant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 全部义体型号的定义表。
 *
 * <p>数值来源分两段：
 * <ul>
 *   <li>官方 123 条（{@code capacity_booster} 起）—— 容量与效果数值逐条取自
 *       {@code /sdcard/DSH/义体/描述.txt}（官方义体数据表），逐条对照见仓库根 {@code DEFS-VALUES.md}。
 *       描述里写成区间 {@code a~b} 的数值统一取 <b>b</b>（最高档），因为本表每个型号只保留一个
 *       最高稀有度变体；{@code 时间减慢X%} 以小数存（0.85 = 85%），其余百分比直接存数字。</li>
 *   <li>早期 19 条网络接入仓 —— 数值照《义体拓展》文档（{@code /root/mod26/spec/义体拓展.txt}）。</li>
 * </ul>
 *
 * <p>三类特殊条目：{@code TODO(数值与描述待补)} = 描述.txt 里查不到该物品；以
 * {@code // 待素材：} 开头的整块注释 = 有定义但无贴图素材，暂不注册（见 {@code ASSET-MISSING.txt}）；
 * 描述.txt 里有、但 {@link Stats} 没有对应键的效果（半伤、移动/攻击速度、伤害减免、光学变焦等）
 * 只写进描述串，未映射成数值键。
 */
public final class CyberwareDefinitions {

    private static final Map<String, CyberwareDefinition> BY_ID = new LinkedHashMap<>();

    // ═══════════════════ 操作系统 · 斯安威斯坦 ═══════════════════

    // ═══════════════════ 操作系统 · 网络接入仓 ═══════════════════
    // 黑客义体：提供 RAM / 缓冲 / 栏位，不提供时间减缓。

    /** 7a. 冬月电子1型 —— 普通 */
    public static final CyberwareDefinition CYBERDECK_DONGYUE_1 = register(new CyberwareDefinition(
            "cyberdeck_dongyue_1", "冬月电子1型", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/cyberdeck_dongyue_1",
            "冬月电子的入门网络接入仓。",
            List.of(variant(CyberwareRarity.COMMON, 6, stats(
                    Stats.RAM, 3, Stats.BUFFER, 5, Stats.SLOTS, 2)))));

    /** 7b. 修补匠3型 —— 传说 */
    public static final CyberwareDefinition CYBERDECK_TINKERER_3 = register(new CyberwareDefinition(
            "cyberdeck_tinkerer_3", "修补匠3型", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/cyberdeck_tinkerer_3",
            "修补匠3型：破解持续时间与散布距离都异常出色的改装货。",
            List.of(variant(CyberwareRarity.LEGENDARY, 20, stats(
                    Stats.RAM, 8, Stats.BUFFER, 7, Stats.SLOTS, 6,
                    Stats.RAM_REGEN, 9, Stats.COMBAT_HACK_DURATION, 50, Stats.SPREAD_DISTANCE, 40)))));

    /** 8a. 瑞草电子1型 —— 精良 */
    public static final CyberwareDefinition CYBERDECK_RUICao_1 = register(new CyberwareDefinition(
            "cyberdeck_ruicao_1", "瑞草电子1型", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/cyberdeck_ruicao_1",
            "瑞草电子1型：隐蔽破解占用更小。",
            List.of(variant(CyberwareRarity.UNCOMMON, 8, stats(
                    Stats.RAM, 4, Stats.BUFFER, 5, Stats.SLOTS, 3, Stats.STEALTH_COST, -1)))));

    /** 8b. 瑞草电子2型 —— 稀有 */
    public static final CyberwareDefinition CYBERDECK_RUICao_2 = register(new CyberwareDefinition(
            "cyberdeck_ruicao_2", "瑞草电子2型", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/cyberdeck_ruicao_2",
            "瑞草电子2型：上传更快。",
            List.of(variant(CyberwareRarity.RARE, 14, stats(
                    Stats.RAM, 6, Stats.BUFFER, 6, Stats.SLOTS, 4,
                    Stats.STEALTH_COST, -1, Stats.UPLOAD_TIME, -25)))));

    /** 9a. 生物动力1型 —— 精良 */
    public static final CyberwareDefinition CYBERDECK_BIODYNE_1 = register(new CyberwareDefinition(
            "cyberdeck_biodyne_1", "生物动力1型", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/cyberdeck_biodyne_1",
            "生物动力1型：RAM 池较大。",
            List.of(variant(CyberwareRarity.UNCOMMON, 10, stats(
                    Stats.RAM, 6, Stats.BUFFER, 5, Stats.SLOTS, 3)))));

    /** 9b. 生物动力2型 —— 稀有 */
    public static final CyberwareDefinition CYBERDECK_BIODYNE_2 = register(new CyberwareDefinition(
            "cyberdeck_biodyne_2", "生物动力2型", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/cyberdeck_biodyne_2",
            "生物动力2型：开始带 RAM 自动恢复。",
            List.of(variant(CyberwareRarity.RARE, 16, stats(
                    Stats.RAM, 9, Stats.BUFFER, 6, Stats.SLOTS, 4, Stats.RAM_REGEN, 3)))));

    /** 10a. 生物技术1型 —— 精良 */
    public static final CyberwareDefinition CYBERDECK_BIOTECH_1 = register(new CyberwareDefinition(
            "cyberdeck_biotech_1", "生物技术1型", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/cyberdeck_biotech_1",
            "生物技术1型。",
            List.of(variant(CyberwareRarity.UNCOMMON, 8, stats(
                    Stats.RAM, 5, Stats.BUFFER, 5, Stats.SLOTS, 3, Stats.RAM_REGEN, 6)))));

    /** 10b. 生物技术2型 —— 稀有 */
    public static final CyberwareDefinition CYBERDECK_BIOTECH_2 = register(new CyberwareDefinition(
            "cyberdeck_biotech_2", "生物技术2型", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/cyberdeck_biotech_2",
            "生物技术2型：快速破解伤害提升。",
            List.of(variant(CyberwareRarity.RARE, 14, stats(
                    Stats.RAM, 7, Stats.BUFFER, 6, Stats.SLOTS, 4,
                    Stats.RAM_REGEN, 9, Stats.HACK_DAMAGE, 10)))));

    /** 11a. 泰克重工技术2型 —— 稀有 */
    public static final CyberwareDefinition CYBERDECK_TECHNICA_2 = register(new CyberwareDefinition(
            "cyberdeck_technica_2", "泰克重工技术2型", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/cyberdeck_technica_2",
            "泰克重工技术2型：破解冷却显著缩短。",
            List.of(variant(CyberwareRarity.RARE, 12, stats(
                    Stats.RAM, 8, Stats.BUFFER, 7, Stats.SLOTS, 4,
                    Stats.HACK_COOLDOWN, 30, Stats.COMBAT_HACK_DURATION, 30, Stats.UPLOAD_TIME, -25)))));

    /** 11b. 泰克重工技术3型 —— 史诗 */
    public static final CyberwareDefinition CYBERDECK_TECHNICA_3 = register(new CyberwareDefinition(
            "cyberdeck_technica_3", "泰克重工技术3型", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/cyberdeck_technica_3",
            "泰克重工技术3型。",
            List.of(variant(CyberwareRarity.EPIC, 18, stats(
                    Stats.RAM, 10, Stats.BUFFER, 7, Stats.SLOTS, 5,
                    Stats.HACK_COOLDOWN, 45, Stats.COMBAT_HACK_DURATION, 40, Stats.UPLOAD_TIME, -25)))));

    /** 11c. 泰克重工技术4型 —— 传说 */
    public static final CyberwareDefinition CYBERDECK_TECHNICA_4 = register(new CyberwareDefinition(
            "cyberdeck_technica_4", "泰克重工技术4型", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/cyberdeck_technica_4",
            "泰克重工技术4型：战斗破解的顶点。",
            List.of(variant(CyberwareRarity.LEGENDARY, 24, stats(
                    Stats.RAM, 12, Stats.BUFFER, 8, Stats.SLOTS, 6,
                    Stats.HACK_COOLDOWN, 45, Stats.COMBAT_HACK_DURATION, 50, Stats.UPLOAD_TIME, -25)))));

    /** 12a. 四相传电1型 —— 精良 */
    public static final CyberwareDefinition CYBERDECK_TETRATRONIC_1 = register(new CyberwareDefinition(
            "cyberdeck_tetratronic_1", "四相传电1型", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/cyberdeck_tetratronic_1",
            "四相传电系列：专精终极破解。",
            List.of(variant(CyberwareRarity.UNCOMMON, 8, stats(
                    Stats.RAM, 4, Stats.BUFFER, 5, Stats.SLOTS, 3)))));

    /** 12b. 四相传电2型 —— 稀有 */
    public static final CyberwareDefinition CYBERDECK_TETRATRONIC_2 = register(new CyberwareDefinition(
            "cyberdeck_tetratronic_2", "四相传电2型", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/cyberdeck_tetratronic_2",
            "四相传电2型：终极破解占用 -1。",
            List.of(variant(CyberwareRarity.RARE, 14, stats(
                    Stats.RAM, 6, Stats.BUFFER, 6, Stats.SLOTS, 4, Stats.ULTIMATE_COST, -1)))));

    /** 12c. 四相传电3型 —— 史诗 */
    public static final CyberwareDefinition CYBERDECK_TETRATRONIC_3 = register(new CyberwareDefinition(
            "cyberdeck_tetratronic_3", "四相传电3型", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/cyberdeck_tetratronic_3",
            "四相传电3型：终极破解占用 -2。",
            List.of(variant(CyberwareRarity.EPIC, 20, stats(
                    Stats.RAM, 8, Stats.BUFFER, 7, Stats.SLOTS, 5, Stats.ULTIMATE_COST, -2)))));

    /** 12d. 涟漪4型 —— 传说 */
    public static final CyberwareDefinition CYBERDECK_RIPPLE_4 = register(new CyberwareDefinition(
            "cyberdeck_ripple_4", "涟漪4型", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/cyberdeck_ripple_4",
            "涟漪4型：终极破解可散布一次，上传时间与冷却都被压到极限。",
            List.of(variant(CyberwareRarity.LEGENDARY, 26, stats(
                    Stats.RAM, 10, Stats.BUFFER, 8, Stats.SLOTS, 6, Stats.ULTIMATE_COST, -3,
                    Stats.UPLOAD_TIME, -75, Stats.HACK_COOLDOWN, 45)))));

    /** 13a. 荒坂3型 —— 史诗 */
    public static final CyberwareDefinition CYBERDECK_ARASAKA_3 = register(new CyberwareDefinition(
            "cyberdeck_arasaka_3", "荒坂3型", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/cyberdeck_arasaka_3",
            "荒坂3型。",
            List.of(variant(CyberwareRarity.EPIC, 18, stats(
                    Stats.RAM, 8, Stats.BUFFER, 7, Stats.SLOTS, 5)))));

    /** 13b. 荒坂4型 —— 传说 */
    public static final CyberwareDefinition CYBERDECK_ARASAKA_4 = register(new CyberwareDefinition(
            "cyberdeck_arasaka_4", "荒坂4型", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/cyberdeck_arasaka_4",
            "荒坂4型：企业级的稳定与容量。",
            List.of(variant(CyberwareRarity.LEGENDARY, 24, stats(
                    Stats.RAM, 10, Stats.BUFFER, 8, Stats.SLOTS, 6)))));

    /** 15. 网络监察网驱5型 —— 传说 */
    public static final CyberwareDefinition CYBERDECK_NETWATCH_5 = register(new CyberwareDefinition(
            "cyberdeck_netwatch_5", "网络监察网驱5型", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/cyberdeck_netwatch_5",
            "网络监察专用：最大的缓冲与栏位。",
            List.of(variant(CyberwareRarity.LEGENDARY, 30, stats(
                    Stats.RAM, 11, Stats.BUFFER, 8, Stats.SLOTS, 6)))));

    /** 16. 乌鸦微控4型 —— 传说 */
    public static final CyberwareDefinition CYBERDECK_RAVEN_4 = register(new CyberwareDefinition(
            "cyberdeck_raven_4", "乌鸦微控4型", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/cyberdeck_raven_4",
            "乌鸦微控4型：拉长敌人的破解时间，并加大破解距离。",
            List.of(variant(CyberwareRarity.LEGENDARY, 24, stats(
                    Stats.ENEMY_HACK_TIME, 100, Stats.HACK_DISTANCE, 60, Stats.RAM_REGEN, 6)))));

    // ═══════════════════ 操作系统 · 狂暴 ═══════════════════

    /** 装殖缩减 · 操作系统 */
    public static final CyberwareDefinition CAPACITY_BOOSTER = register(new CyberwareDefinition(
            "capacity_booster", "装殖缩减", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/capacity_booster",
            "+40~70义体容量",
            List.of(variant(CyberwareRarity.LEGENDARY, 0, stats()))));

    /** 军用科技篇章6型 · 操作系统 */
    public static final CyberwareDefinition HAUNTED_CYBERDECK = register(new CyberwareDefinition(
            "haunted_cyberdeck", "军用科技篇章6型", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/haunted_cyberdeck",
            "RAM数量：11 最大快速破解数量：4 缓冲区数量：4 解锁快速破解“黑墙网关”：这个快速破解会散布到25米内的5名敌人身上，对义体和神经系统造成致命伤害，并关闭机甲、机器人、无人机和炮塔。 每次散布时自动占用RAM，每次散布的上传时间会越来越短，占用的RAM越来越多。但只要你愿意付出血的代价，占用也可以减半…",
            List.of(variant(CyberwareRarity.LEGENDARY, 33, stats(Stats.RAM, 11, Stats.SLOTS, 4, Stats.BUFFER, 4)))));

    /** 网络监察网驱1型 · 操作系统 */
    public static final CyberwareDefinition NETWATCH_NETDRIVER_MK = register(new CyberwareDefinition(
            "netwatch_netdriver_mk", "网络监察网驱1型", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/netwatch_netdriver_mk",
            "RAM数量：13 最大快速破解数量：8 缓冲区数量：4 通过摄像头上传的快速破解-20%可追踪性。 设备和载具类型快速破解-50%RAM占用。 通过设备上传的战斗类型快速破解提供：+15%伤害，+20%效果持续时间。",
            List.of(variant(CyberwareRarity.LEGENDARY, 25, stats(Stats.RAM, 13, Stats.SLOTS, 8, Stats.BUFFER, 4)))));

    /** 军用科技斯安威斯坦”远地点“ · 操作系统 */
    public static final CyberwareDefinition SANDEVISTAN_APOGEE = register(new CyberwareDefinition(
            "sandevistan_apogee", "军用科技斯安威斯坦”远地点“", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/sandevistan_apogee",
            "按E键激活，无论其充能程度如何。 激活时：时间减慢85%（自身速度不变），+15%~20%爆头伤害，+15%~20%暴击率，+15%~20%暴击伤害，激活时消灭一名敌人提供+10%持续时间和22%耐力。 持续：6秒。冷却：30~25秒。 反应调幅：每一点属性使该义体+0.1秒持续时间。",
            List.of(variant(CyberwareRarity.LEGENDARY, 44, stats(
                    Stats.TIME_SLOW, 0.85, Stats.DURATION, 6, Stats.COOLDOWN, 25,
                    Stats.HEADSHOT_DAMAGE, 20, Stats.CRIT_CHANCE, 20, Stats.CRIT_DAMAGE, 20)))));

    /** 军用科技斯安威斯坦”游隼“ · 操作系统 */
    public static final CyberwareDefinition SANDEVISTAN_C4 = register(new CyberwareDefinition(
            "sandevistan_c4", "军用科技斯安威斯坦”游隼“", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/sandevistan_c4",
            "按E键激活，无论其充能程度如何。 激活时：时间减慢70%（自身速度不变），+5%~10%伤害，+5%~15%暴击率，+5%~10%暴击伤害，激活时消灭一名敌人提供+5%持续时间和10%~12%生命值。 持续：9~10秒。冷却：40~30秒。 反应调幅：每一点属性使该义体+0.1秒持续时间。",
            List.of(variant(CyberwareRarity.LEGENDARY, 39, stats(
                    Stats.TIME_SLOW, 0.7, Stats.DURATION, 10, Stats.COOLDOWN, 30, Stats.ALL_DAMAGE, 10,
                    Stats.CRIT_CHANCE, 15, Stats.CRIT_DAMAGE, 10)))));

    /** 军用科技狂暴 · 操作系统 */
    public static final CyberwareDefinition BERSERK_C4 = register(new CyberwareDefinition(
            "berserk_c4", "军用科技狂暴", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/berserk_c4",
            "按E键激活。 激活时：免疫伤害，无法使用物品，只能使用近战武器，近战攻击耐力消耗-100%，攻击速度+25%~30%，移动速度+15%~20%，随生命值降低而提高伤害（生命值低于20%时达到最大值50%）。 结束时：每消灭一名敌人+25%生命值。 持续：11~12秒。冷却：40~25秒。 肉体调幅：每一点属性+0.5%狂暴期间伤害。",
            List.of(variant(CyberwareRarity.LEGENDARY, 35, stats(
                    Stats.DURATION, 12, Stats.COOLDOWN, 25, Stats.KILL_HEAL, 25, Stats.ALL_DAMAGE, 50)))));

    /** 歧路司义眼石化鸡蛇 · 面部 */
    public static final CyberwareDefinition ICONIC_ADVANCED_KIROSHI_OPTICS_BARE = register(new CyberwareDefinition(
            "iconic_advanced_kiroshi_optics_bare", "歧路司义眼石化鸡蛇", CyberwareSlot.FACE, true,
            "cyberware:item/iconic_advanced_kiroshi_optics_bare",
            "+25%~35%暴击率。 扫描或瞄准时10倍光学变焦。",
            List.of(variant(CyberwareRarity.MYTHIC, 30, stats(Stats.CRIT_CHANCE, 35)))));

    // 待素材：暂无贴图素材，暂不注册（见 ASSET-MISSING.txt）
    // /** 行为特征脸板 · 面部 */
    // public static final CyberwareDefinition MASK_CW_PLUS_PLUS = register(new CyberwareDefinition(
    //         "mask_cw_plus_plus", "行为特征脸板", CyberwareSlot.FACE, true,
    //         "cyberware:item/mask_cw_plus_plus",
    //         "激活脸板可以改变使用者的现实身份和数字身份，让别人完全认不出来。脱离战斗时，能够轻松摆脱执法部门（最高4星通缉）。 冷却时间：1500~900秒 可以分配到“手雷”快捷栏位。 往日之影资料片内容 智力调幅：每一点属性值+0.05%造成的所有伤害。",
    //         List.of(variant(CyberwareRarity.LEGENDARY, 0, stats(Stats.COOLDOWN, 900)))));

    /** 坚矛利盾 · 胳臂 */
    public static final CyberwareDefinition ICONIC_GUN_STABILIZER = register(new CyberwareDefinition(
            "iconic_gun_stabilizer", "坚矛利盾", CyberwareSlot.ARMS, true,
            "cyberware:item/iconic_gun_stabilizer",
            "-18%~35%后坐力 -25%子弹散射 自动激活只有处在掩体后才会生效的远程武器效果。",
            List.of(variant(CyberwareRarity.MYTHIC, 35, stats(Stats.RECOIL, -35)))));

    // 待素材：暂无贴图素材，暂不注册（见 ASSET-MISSING.txt）
    // /** 电磁回收 · 循环系统 */
    // public static final CyberwareDefinition ICONIC_DISCHARGE_CONNECTOR = register(new CyberwareDefinition(
    //         "iconic_discharge_connector", "电磁回收", CyberwareSlot.CIRCULATORY, true,
    //         "cyberware:item/iconic_discharge_connector",
    //         "使用完全充能的技术武器射击命中敌人时，+2.5%~5%生命值和耐力。 技术调幅：每一点属性值+1%生命值物品效率。",
    //         List.of(variant(CyberwareRarity.MYTHIC, 40, stats()))));

    /** 等距稳定 · 循环系统 */
    public static final CyberwareDefinition ICONIC_SHOCK_ABSORBER = register(new CyberwareDefinition(
            "iconic_shock_absorber", "等距稳定", CyberwareSlot.CIRCULATORY, true,
            "cyberware:item/iconic_shock_absorber",
            "所有攻击-13%~20%耐力消耗。 反应调幅：每一点属性值-0.5%所有耐力消耗。",
            List.of(variant(CyberwareRarity.MYTHIC, 40, stats()))));

    /** 团灭韧带 · 腿部 */
    public static final CyberwareDefinition ICONIC_JENKINS_TENDONS = register(new CyberwareDefinition(
            "iconic_jenkins_tendons", "团灭韧带", CyberwareSlot.LEGS, true,
            "cyberware:item/iconic_jenkins_tendons",
            "提供护甲：10~26 +12%+20%移动速度",
            List.of(variant(CyberwareRarity.MYTHIC, 6, stats(Stats.ARMOR, 26)))));

    /** 几质丁壳 · 表皮系统 */
    public static final CyberwareDefinition ICONIC_CHITON = register(new CyberwareDefinition(
            "iconic_chiton", "几质丁壳", CyberwareSlot.INTEGUMENTARY, true,
            "cyberware:item/iconic_chiton",
            "提供护甲：140~200 转基因几丁质制作的皮下壳层，极其坚固。提供额外的生命值恢复。 肉体调幅：每一点属性值+10%生命值恢复速率。",
            List.of(variant(CyberwareRarity.MYTHIC, 40, stats(Stats.ARMOR, 200)))));

    /** 外周逆反 · 表皮系统 */
    public static final CyberwareDefinition ICONIC_PROXIMITY_REDUCER = register(new CyberwareDefinition(
            "iconic_proximity_reducer", "外周逆反", CyberwareSlot.INTEGUMENTARY, true,
            "cyberware:item/iconic_proximity_reducer",
            "提供护甲：8~36 攻击你的敌人越近，他们造成的伤害就越低。 在3米处-34%~45%受到的伤害。 伤害减免在6米处减少到0。 肉体调幅：每一点属性值+0.5生命值。",
            List.of(variant(CyberwareRarity.MYTHIC, 24, stats(Stats.ARMOR, 36)))));

    // 待素材：暂无贴图素材，暂不注册（见 ASSET-MISSING.txt）
    // /** 长焦可视界面 · 神经系统 */
    // public static final CyberwareDefinition ICONIC_VISUAL_CORTEX_SUPPORT = register(new CyberwareDefinition(
    //         "iconic_visual_cortex_support", "长焦可视界面", CyberwareSlot.NERVOUS_SYSTEM, true,
    //         "cyberware:item/iconic_visual_cortex_support",
    //         "距离敌人越远，暴击率越高（最多在85~100米处+70%~100%） 镇定调幅：每一点属性值+1%暴击伤害。",
    //         List.of(variant(CyberwareRarity.MYTHIC, 40, stats(Stats.CRIT_CHANCE, 100)))));

    // 待素材：暂无贴图素材，暂不注册（见 ASSET-MISSING.txt）
    // /** 肾上腺导引 · 神经系统 */
    // public static final CyberwareDefinition ICONIC_DETECTOR_RUSH = register(new CyberwareDefinition(
    //         "iconic_detector_rush", "肾上腺导引", CyberwareSlot.NERVOUS_SYSTEM, true,
    //         "cyberware:item/iconic_detector_rush",
    //         "进入战斗时+20%~30%移动速度，持续10~35秒。 反应调幅：每一点属性值使得该义体+3秒持续时间。",
    //         List.of(variant(CyberwareRarity.MYTHIC, 20, stats(Stats.DURATION, 35)))));

    /** 乖离排异 · 神经系统 */
    public static final CyberwareDefinition ICONIC_REFLEX_RECORDER = register(new CyberwareDefinition(
            "iconic_reflex_recorder", "乖离排异", CyberwareSlot.NERVOUS_SYSTEM, true,
            "cyberware:item/iconic_reflex_recorder",
            "生命值降到25%以下时，时间减慢40%~60%，持续3.5~4.5秒。 你的移动速度不会减慢。 冷却时间：50~35秒 镇定调幅：每一点属性值使得该义体-0.25秒冷却时间。",
            List.of(variant(CyberwareRarity.MYTHIC, 35, stats(
                    Stats.TIME_SLOW, 0.6, Stats.DURATION, 4.5, Stats.COOLDOWN, 35)))));

    /** 一拳开 · 骨骼 */
    public static final CyberwareDefinition ICONIC_T1000 = register(new CyberwareDefinition(
            "iconic_t1000", "一拳开", CyberwareSlot.SKELETON, true,
            "cyberware:item/iconic_t1000",
            "+30%~42%护甲。 肉体调幅：每一点属性值+2生命值。",
            List.of(variant(CyberwareRarity.MYTHIC, 36, stats(Stats.ARMOR, 42)))));

    // 待素材：暂无贴图素材，暂不注册（见 ASSET-MISSING.txt）
    // /** 量子调谐 · 额皮质 */
    // public static final CyberwareDefinition TIME_BANK = register(new CyberwareDefinition(
    //         "time_bank", "量子调谐", CyberwareSlot.FRONTAL_CORTEX, true,
    //         "cyberware:item/time_bank",
    //         "所有其它义体冷却时间减少10%~15%。每当其它义体进入冷却时，立即重置其冷却时间（最多30~50秒）。 冷却时间：60秒，仅脱战时生效。 智力调幅：每一点属性+0.05%造成的所有伤害。",
    //         List.of(variant(CyberwareRarity.LEGENDARY, 45, stats(Stats.COOLDOWN, 60)))));

    // 待素材：暂无贴图素材，暂不注册（见 ASSET-MISSING.txt）
    // /** 皮下变色 · 额皮质 */
    // public static final CyberwareDefinition ICONIC_SUBDERMAL_CO_PROCESSOR = register(new CyberwareDefinition(
    //         "iconic_subdermal_co_processor", "皮下变色", CyberwareSlot.FRONTAL_CORTEX, true,
    //         "cyberware:item/iconic_subdermal_co_processor",
    //         "消灭一名敌人后，所有义体立即减少5%~7.5%冷却时间。",
    //         List.of(variant(CyberwareRarity.MYTHIC, 48, stats()))));

    /** COX-2赛博生体优化 · 额皮质 */
    public static final CyberwareDefinition ICONIC_BIO_CONDUCTORS = register(new CyberwareDefinition(
            "iconic_bio_conductors", "COX-2赛博生体优化", CyberwareSlot.FRONTAL_CORTEX, true,
            "cyberware:item/iconic_bio_conductors",
            "允许你的快速破解造成暴击。快速破解有80%~100%的几率暴击。 最大RAM减少8~4。 智力调幅：每一点属性+0.5%快速破解伤害。",
            List.of(variant(CyberwareRarity.MYTHIC, 50, stats(Stats.RAM, -4)))));

    /** RAM配平 · 额皮质 */
    public static final CyberwareDefinition ICONIC_CAMILLO_RAM_MANAGER = register(new CyberwareDefinition(
            "iconic_camillo_ram_manager", "RAM配平", CyberwareSlot.FRONTAL_CORTEX, true,
            "cyberware:item/iconic_camillo_ram_manager",
            "最大RAM增加2。 可用RAM降到15%~20%时，立即恢复20%~40%的最大RAM。冷却时间：95秒~85秒。 智力调幅：每一点属性使该义体-3秒冷却时间。",
            List.of(variant(CyberwareRarity.MYTHIC, 40, stats(Stats.RAM, 2, Stats.COOLDOWN, 85)))));

    /** 暴恐机动队螳螂刀 · 胳臂 */
    public static final CyberwareDefinition MAX_TAC_MANTIS_BLADES = register(new CyberwareDefinition(
            "max_tac_mantis_blades", "暴恐机动队螳螂刀", CyberwareSlot.ARMS, true,
            "cyberware:item/max_tac_mantis_blades",
            "可以长按攻击键向目标飞跃，造成物理伤害。 15%~20%流血几率。 反应调幅：每一点属性值使该义体+0.5%伤害 获取方式1：在支线【砧板上的肉】结尾，离开神宫寺前消灭暴恐机动队成员梅利莎·罗里后从她身上拾取。",
            List.of(variant(CyberwareRarity.LEGENDARY, 0, stats()))));

    /** 剧毒螳螂刀 · 胳臂 */
    public static final CyberwareDefinition MANTIS_BLADES_CHEMICAL = register(new CyberwareDefinition(
            "mantis_blades_chemical", "剧毒螳螂刀", CyberwareSlot.ARMS, true,
            "cyberware:item/mantis_blades_chemical",
            "可以长按攻击键向目标飞跃，造成物理/热能/电子/化学伤害。 7%~20%流血/燃烧/电击/中毒几率。 剧毒：镇定调幅：每一点属性值使该义体+0.5%伤害",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 放电螳螂刀 · 胳臂 */
    public static final CyberwareDefinition MANTIS_BLADES_ELECTRIC = register(new CyberwareDefinition(
            "mantis_blades_electric", "放电螳螂刀", CyberwareSlot.ARMS, true,
            "cyberware:item/mantis_blades_electric",
            "可以长按攻击键向目标飞跃，造成物理/热能/电子/化学伤害。 7%~20%流血/燃烧/电击/中毒几率。 放电：智力调幅：每一点属性值使该义体+0.5%伤害",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 螳螂刀 · 胳臂 */
    public static final CyberwareDefinition MANTIS_BLADES = register(new CyberwareDefinition(
            "mantis_blades", "螳螂刀", CyberwareSlot.ARMS, true,
            "cyberware:item/mantis_blades",
            "可以长按攻击键向目标飞跃，造成物理/热能/电子/化学伤害。 7%~20%流血/燃烧/电击/中毒几率。 物理：反应调幅：每一点属性值使该义体+0.5%伤害",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 热能螳螂刀 · 胳臂 */
    public static final CyberwareDefinition MANTIS_BLADES_THERMAL = register(new CyberwareDefinition(
            "mantis_blades_thermal", "热能螳螂刀", CyberwareSlot.ARMS, true,
            "cyberware:item/mantis_blades_thermal",
            "可以长按攻击键向目标飞跃，造成物理/热能/电子/化学伤害。 7%~20%流血/燃烧/电击/中毒几率。 热能：肉体调幅：每一点属性值使该义体+0.5%伤害",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 剧毒单分子线 · 胳臂 */
    public static final CyberwareDefinition NANO_WIRES_CHEMICAL = register(new CyberwareDefinition(
            "nano_wires_chemical", "剧毒单分子线", CyberwareSlot.ARMS, true,
            "cyberware:item/nano_wires_chemical",
            "可以从远距离同时鞭打多名敌人。 造成物理/热能/电子/化学伤害。 7%~20%流血/燃烧/电击/中毒几率。 剧毒：技术调幅：每一点属性值使该义体+0.5%伤害",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 放电单分子线 · 胳臂 */
    public static final CyberwareDefinition NANO_WIRES_ELECTRIC = register(new CyberwareDefinition(
            "nano_wires_electric", "放电单分子线", CyberwareSlot.ARMS, true,
            "cyberware:item/nano_wires_electric",
            "可以从远距离同时鞭打多名敌人。 造成物理/热能/电子/化学伤害。 7%~20%流血/燃烧/电击/中毒几率。 放电：反应调幅：每一点属性值使该义体+0.5%伤害",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 单分子线 · 胳臂 */
    public static final CyberwareDefinition NANO_WIRES = register(new CyberwareDefinition(
            "nano_wires", "单分子线", CyberwareSlot.ARMS, true,
            "cyberware:item/nano_wires",
            "可以从远距离同时鞭打多名敌人。 造成物理/热能/电子/化学伤害。 7%~20%流血/燃烧/电击/中毒几率。 物理：智力调幅：每一点属性值使该义体+0.5%伤害",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 热能单分子线 · 胳臂 */
    public static final CyberwareDefinition NANO_WIRES_THERMAL = register(new CyberwareDefinition(
            "nano_wires_thermal", "热能单分子线", CyberwareSlot.ARMS, true,
            "cyberware:item/nano_wires_thermal",
            "可以从远距离同时鞭打多名敌人。 造成物理/热能/电子/化学伤害。 7%~20%流血/燃烧/电击/中毒几率。 热能：镇定调幅：每一点属性值使该义体+0.5%伤害",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 剧毒弹射发射系统 · 胳臂 */
    public static final CyberwareDefinition PROJECTILE_LAUNCHER_CHEMICAL = register(new CyberwareDefinition(
            "projectile_launcher_chemical", "剧毒弹射发射系统", CyberwareSlot.ARMS, true,
            "cyberware:item/projectile_launcher_chemical",
            "发射高爆射弹，造成物理/热能/电子/化学伤害。 充能射击提供：+30%伤害，+25%爆炸范围，+40%肢解几率/+30%燃烧几率/+100%击晕几率/+30%中毒几率。 可以分配到“手雷”快捷栏位。 剧毒：肉体调幅：每一点属性值使该义体+0.5%伤害",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats(Stats.ALL_DAMAGE, 30)))));

    /** 电子弹射发射系统 · 胳臂 */
    public static final CyberwareDefinition PROJECTILE_LAUNCHER_ELECTRIC = register(new CyberwareDefinition(
            "projectile_launcher_electric", "电子弹射发射系统", CyberwareSlot.ARMS, true,
            "cyberware:item/projectile_launcher_electric",
            "发射高爆射弹，造成物理/热能/电子/化学伤害。 充能射击提供：+30%伤害，+25%爆炸范围，+40%肢解几率/+30%燃烧几率/+100%击晕几率/+30%中毒几率。 可以分配到“手雷”快捷栏位。 放电：镇定调幅：每一点属性值使该义体+0.5%伤害",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats(Stats.ALL_DAMAGE, 30)))));

    /** 弹射发射系统 · 胳臂 */
    public static final CyberwareDefinition PROJECTILE_LAUNCHER = register(new CyberwareDefinition(
            "projectile_launcher", "弹射发射系统", CyberwareSlot.ARMS, true,
            "cyberware:item/projectile_launcher",
            "发射高爆射弹，造成物理/热能/电子/化学伤害。 充能射击提供：+30%伤害，+25%爆炸范围，+40%肢解几率/+30%燃烧几率/+100%击晕几率/+30%中毒几率。 可以分配到“手雷”快捷栏位。 物理：技术调幅：每一点属性值使该义体+0.5%伤害",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats(Stats.ALL_DAMAGE, 30)))));

    /** 热能弹射发射系统 · 胳臂 */
    public static final CyberwareDefinition PROJECTILE_LAUNCHER_THERMAL = register(new CyberwareDefinition(
            "projectile_launcher_thermal", "热能弹射发射系统", CyberwareSlot.ARMS, true,
            "cyberware:item/projectile_launcher_thermal",
            "发射高爆射弹，造成物理/热能/电子/化学伤害。 充能射击提供：+30%伤害，+25%爆炸范围，+40%肢解几率/+30%燃烧几率/+100%击晕几率/+30%中毒几率。 可以分配到“手雷”快捷栏位。 热能：智力调幅：每一点属性值使该义体+0.5%伤害",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats(Stats.ALL_DAMAGE, 30)))));

    /** 剧毒大猩猩手臂 · 胳臂 */
    public static final CyberwareDefinition STRONG_ARMS_CHEMICAL = register(new CyberwareDefinition(
            "strong_arms_chemical", "剧毒大猩猩手臂", CyberwareSlot.ARMS, true,
            "cyberware:item/strong_arms_chemical",
            "+1~6肉体属性检定。 造成物理/热能/电子/化学伤害。 7%~20%流血/燃烧/电击/中毒几率。 剧毒：镇定调幅：每一点属性值使该义体+0.5%伤害",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 放电大猩猩手臂 · 胳臂 */
    public static final CyberwareDefinition STRONG_ARMS_ELECTRIC = register(new CyberwareDefinition(
            "strong_arms_electric", "放电大猩猩手臂", CyberwareSlot.ARMS, true,
            "cyberware:item/strong_arms_electric",
            "+1~6肉体属性检定。 造成物理/热能/电子/化学伤害。 7%~20%流血/燃烧/电击/中毒几率。 放电：技术调幅：每一点属性值使该义体+0.5%伤害",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 大猩猩手臂 · 胳臂 */
    public static final CyberwareDefinition STRONG_ARMS = register(new CyberwareDefinition(
            "strong_arms", "大猩猩手臂", CyberwareSlot.ARMS, true,
            "cyberware:item/strong_arms",
            "+1~6肉体属性检定。 造成物理/热能/电子/化学伤害。 7%~20%流血/燃烧/电击/中毒几率。 物理：肉体调幅：每一点属性值使该义体+0.5%伤害",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 热能大猩猩手臂 · 胳臂 */
    public static final CyberwareDefinition STRONG_ARMS_THERMAL = register(new CyberwareDefinition(
            "strong_arms_thermal", "热能大猩猩手臂", CyberwareSlot.ARMS, true,
            "cyberware:item/strong_arms_thermal",
            "+1~6肉体属性检定。 造成物理/热能/电子/化学伤害。 7%~20%流血/燃烧/电击/中毒几率。 热能：反应调幅：每一点属性值使该义体+0.5%伤害",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 军用科技平行线 · 操作系统 */
    public static final CyberwareDefinition MILITECH_PARALINE_MKV = register(new CyberwareDefinition(
            "militech_paraline_mkv", "军用科技平行线", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/militech_paraline_mkv",
            "RAM数量：9 最大快速破解数量：8 缓冲区数量：4 +10%快速破解伤害。 每占用一个单位RAM，+2%单分子线伤害（最多+30%）。 对一名敌人上传快速破解时，使用智能武器向其射击会加速上传。 “脑机超频”激活时，智能武器和单分子线造成等同于正常攻击伤害40%的额外电子伤害。",
            List.of(variant(CyberwareRarity.LEGENDARY, 14, stats(
                    Stats.RAM, 9, Stats.SLOTS, 8, Stats.BUFFER, 4, Stats.HACK_DAMAGE, 10)))));

    /** 四相传电涟漪5型 · 操作系统 */
    public static final CyberwareDefinition TETRATRONIC_RIPPLER_MKV = register(new CyberwareDefinition(
            "tetratronic_rippler_mkv", "四相传电涟漪5型", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/tetratronic_rippler_mkv",
            "RAM数量：6 最大快速破解数量：8 缓冲区数量：4 被非战斗类型快速破解影响的敌人受到+15%武器伤害。 在战斗类型快速破解的后面立即堆栈一个非战斗类型快速破解，该战斗类型快速破解+40%伤害。 +8最大RAM单位。 “脑机超频”激活时，对8米内的所有敌人自动上传重启义眼和武器故障。",
            List.of(variant(CyberwareRarity.LEGENDARY, 16, stats(Stats.RAM, 6, Stats.SLOTS, 8, Stats.BUFFER, 4)))));

    /** 生物技术3型 · 操作系统 */
    public static final CyberwareDefinition BIOTECH_SIGMA_MKIV = register(new CyberwareDefinition(
            "biotech_sigma_mkiv", "生物技术3型", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/biotech_sigma_mkiv",
            "RAM数量：9 最大快速破解数量：8 缓冲区数量：4 战斗类快速破解+30%持续时间。 快速破解+10%持续性伤害。 对受到持续性伤害效果影响的敌人+25%单分子线伤害。 “脑机超频”激活时，重置快速破解持续性伤害效果的持续时间。",
            List.of(variant(CyberwareRarity.LEGENDARY, 16, stats(
                    Stats.RAM, 9, Stats.SLOTS, 8, Stats.BUFFER, 4, Stats.COMBAT_HACK_DURATION, 30,
                    Stats.HACK_DAMAGE, 10)))));

    /** 乌鸦微控3型 · 操作系统 */
    public static final CyberwareDefinition RAVEN_MICROCYBER_MKIII = register(new CyberwareDefinition(
            "raven_microcyber_mkiii", "乌鸦微控3型", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/raven_microcyber_mkiii",
            "RAM数量：9 最大快速破解数量：8 缓冲区数量：4 快速破解+40%散布距离。 快速破解无需等待初始的上传完成，立即散布到所有符合条件的敌人身上。 “脑机超频”激活时，任意快速破解散布到附近2名敌人身上的几率+15%。",
            List.of(variant(CyberwareRarity.LEGENDARY, 20, stats(
                    Stats.RAM, 9, Stats.SLOTS, 8, Stats.BUFFER, 4, Stats.SPREAD_DISTANCE, 40)))));

    /** 荒板5型 · 操作系统 */
    public static final CyberwareDefinition ARASAKA_SHADOW_MKV = register(new CyberwareDefinition(
            "arasaka_shadow_mkv", "荒板5型", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/arasaka_shadow_mkv",
            "RAM数量：5~9 最大快速破解数量：4~8 缓冲区数量：4 敌人追踪你的位置所需时间增加40%。 隐蔽类快速破解-2 RAM占用。 施展击倒后+5 RAM。 “脑机超频”激活后，快速破解不会增加追踪进度（但依然会触发）。",
            List.of(variant(CyberwareRarity.LEGENDARY, 14, stats(
                    Stats.RAM, 9, Stats.SLOTS, 8, Stats.BUFFER, 4, Stats.ENEMY_HACK_TIME, 40,
                    Stats.STEALTH_COST, -2)))));

    /** 泽塔科技斯安威斯坦 · 操作系统 */
    public static final CyberwareDefinition SANDEVISTAN_C1 = register(new CyberwareDefinition(
            "sandevistan_c1", "泽塔科技斯安威斯坦", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/sandevistan_c1",
            "按E键激活或解除。 激活时处于地面：时间减慢30%，+3%~12%伤害。 激活时处于空中：时间减慢60%，+6%~24%伤害，+25%~40%爆头和弱点伤害，受到的摔落伤害-30%。 持续：10秒。冷却：30秒。 反应调幅：每一点属性使该义体+0.1秒持续时间。",
            List.of(variant(CyberwareRarity.LEGENDARY, 20, stats(
                    Stats.TIME_SLOW, 0.6, Stats.DURATION, 10, Stats.COOLDOWN, 30, Stats.ALL_DAMAGE, 24,
                    Stats.HEADSHOT_DAMAGE, 40)))));

    /** 迪娜拉斯安威斯坦 · 操作系统 */
    public static final CyberwareDefinition SANDEVISTAN_C2 = register(new CyberwareDefinition(
            "sandevistan_c2", "迪娜拉斯安威斯坦", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/sandevistan_c2",
            "按E键激活或解除。 激活时：时间减慢50%（自身速度不变），+5%~15%暴击率，+5%~15%暴击伤害。 持续：9秒。冷却：40秒。 反应调幅：每一点属性使该义体+0.1秒持续时间。",
            List.of(variant(CyberwareRarity.LEGENDARY, 18, stats(
                    Stats.TIME_SLOW, 0.5, Stats.DURATION, 9, Stats.COOLDOWN, 40, Stats.CRIT_CHANCE, 15,
                    Stats.CRIT_DAMAGE, 15)))));

    /** 千替斯安威斯坦 · 操作系统 */
    public static final CyberwareDefinition SANDEVISTAN_C3 = register(new CyberwareDefinition(
            "sandevistan_c3", "千替斯安威斯坦", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/sandevistan_c3",
            "按E键激活或解除。 激活时：时间减慢20%（自身速度不变），+14%~24%半伤几率，+7%~12%半伤强度，+30%~50%热能、化学和电子伤害抗性。 持续： 10秒。冷却：60秒。 反应调幅：每一点属性使该义体+0.1秒持续时间。",
            List.of(variant(CyberwareRarity.LEGENDARY, 14, stats(
                    Stats.TIME_SLOW, 0.2, Stats.DURATION, 10, Stats.COOLDOWN, 60)))));

    /** 摩尔科技狂暴 · 操作系统 */
    public static final CyberwareDefinition BERSERK_C1 = register(new CyberwareDefinition(
            "berserk_c1", "摩尔科技狂暴", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/berserk_c1",
            "按E键激活。 激活时：+ 35%~ 50%伤害减免，生命值不会降低至25%以下，无法使用物品，只能使用近战武器，近战攻击耐力消耗-100%。 结束时：每消灭一名敌人+25%生命值。 持续：8~11秒。冷却：60~35秒。 肉体调幅：每一点属性+0.5%狂暴期间伤害。",
            List.of(variant(CyberwareRarity.LEGENDARY, 12, stats(
                    Stats.DURATION, 11, Stats.COOLDOWN, 35, Stats.KILL_HEAL, 25)))));

    /** 生物动力狂暴 · 操作系统 */
    public static final CyberwareDefinition BERSERK_C2 = register(new CyberwareDefinition(
            "berserk_c2", "生物动力狂暴", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/berserk_c2",
            "按E键激活。 激活时：+ 30%~50%伤害减免，生命值不会降低至25%以下，无法使用物品，只能使用近战武器，近战攻击耐力消耗-100%，攻击速度+ 15%~30%，暴击率+ 5%~20%，暴击伤害+100%。 结束时：每消灭一名敌人+25%生命值。 持续：8~11秒。冷却：60~35秒。 反应调幅：每一点属性+1%狂暴期间的暴击率。",
            List.of(variant(CyberwareRarity.LEGENDARY, 20, stats(
                    Stats.CRIT_CHANCE, 20, Stats.CRIT_DAMAGE, 100, Stats.DURATION, 11,
                    Stats.COOLDOWN, 35, Stats.KILL_HEAL, 25)))));

    /** 泽塔科技狂暴 · 操作系统 */
    public static final CyberwareDefinition BERSERK_C3 = register(new CyberwareDefinition(
            "berserk_c3", "泽塔科技狂暴", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/berserk_c3",
            "按E键激活。 激活时：+ 35%~ 50%伤害减免，生命值不会降低至25%以下，无法使用物品，只能使用近战武器，近战攻击耐力消耗-100%，攻击速度+ 10%~20%，受到的摔落伤害- 20%~30%，在空中时按Q键施展“英雄降临”猛击地面。 结束时：每消灭一名敌人+25%生命值。 持续：9~11秒。冷却：45~25秒。 肉体调幅：每一点属性+0.5%狂暴期间伤害。",
            List.of(variant(CyberwareRarity.LEGENDARY, 16, stats(
                    Stats.DURATION, 11, Stats.COOLDOWN, 25, Stats.KILL_HEAL, 25)))));

    /** 基础歧路司义眼 · 面部 */
    public static final CyberwareDefinition KIROSHI_OPTICS_BARE = register(new CyberwareDefinition(
            "kiroshi_optics_bare", "基础歧路司义眼", CyberwareSlot.FACE, true,
            "cyberware:item/kiroshi_optics_bare",
            "-20%~44%摄像头发现速度。 扫描或瞄准时4~10倍光学变焦。 镇定调幅：每一点属性值+0.2%爆头和弱点伤害。",
            List.of(variant(CyberwareRarity.LEGENDARY, 1, stats()))));

    /** 歧路司义眼1型 · 面部 */
    public static final CyberwareDefinition KIROSHI_OPTICS = register(new CyberwareDefinition(
            "kiroshi_optics", "歧路司义眼1型", CyberwareSlot.FACE, true,
            "cyberware:item/kiroshi_optics",
            "-20%~44%摄像头发现速度。 扫描或瞄准时4~10倍光学变焦。 镇定调幅：每一点属性值+0.2%爆头和弱点伤害。",
            List.of(variant(CyberwareRarity.LEGENDARY, 1, stats()))));

    /** 歧路司义眼神舆 · 面部 */
    public static final CyberwareDefinition KIROSHI_OPTICS_COMBINED = register(new CyberwareDefinition(
            "kiroshi_optics_combined", "歧路司义眼神舆", CyberwareSlot.FACE, true,
            "cyberware:item/kiroshi_optics_combined",
            "扫描时：高亮15~19.5米内的所有敌人； 高亮30~39米内的摄像头和炮塔； 高亮准星附近22~29米内的爆炸装置和陷阱；以上所有效果持续60秒。 扫描或瞄准时8~10倍光学变焦。 肉体调幅：每一点属性值+0.5%生命值。",
            List.of(variant(CyberwareRarity.LEGENDARY, 10, stats()))));

    /** 歧路司义眼祸兆 · 面部 */
    public static final CyberwareDefinition KIROSHI_OPTICS_HUNTER = register(new CyberwareDefinition(
            "kiroshi_optics_hunter", "歧路司义眼祸兆", CyberwareSlot.FACE, true,
            "cyberware:item/kiroshi_optics_hunter",
            "扫描时高亮准星附近10~29米内的爆炸装置和陷阱，效果持续60秒。 扫描或瞄准时4~10倍光学变焦。 反应调幅：每一点属性值+0.1%暴击率。",
            List.of(variant(CyberwareRarity.LEGENDARY, 2, stats()))));

    // 待素材：暂无贴图素材，暂不注册（见 ASSET-MISSING.txt）
    // /** 歧路司义眼追猎 · 面部 */
    // public static final CyberwareDefinition KIROSHI_OPTICS_PIERCING = register(new CyberwareDefinition(
    //         "kiroshi_optics_piercing", "歧路司义眼追猎", CyberwareSlot.FACE, true,
    //         "cyberware:item/kiroshi_optics_piercing",
    //         "自动连接你装备的技术武器。 瞄准时高亮视野范围内10%~24%位于掩体后的敌人，最大生效距离15~53米。 扫描或瞄准时4~10倍光学变焦。 技术调幅：每一点属性值+0.5护甲。",
    //         List.of(variant(CyberwareRarity.LEGENDARY, 2, stats()))));

    // 待素材：暂无贴图素材，暂不注册（见 ASSET-MISSING.txt）
    // /** 歧路司义眼警戒 · 面部 */
    // public static final CyberwareDefinition KIROSHI_OPTICS_SENSOR = register(new CyberwareDefinition(
    //         "kiroshi_optics_sensor", "歧路司义眼警戒", CyberwareSlot.FACE, true,
    //         "cyberware:item/kiroshi_optics_sensor",
    //         "扫描时高亮附近15~39米内的摄像头和炮塔，效果持续60秒。 扫描或瞄准时4~10倍光学变焦。 智力调幅：每一点属性值+0.05%造成的所有伤害。",
    //         List.of(variant(CyberwareRarity.LEGENDARY, 2, stats()))));

    /** 歧路司义眼千里目 · 面部 */
    public static final CyberwareDefinition KIROSHI_OPTICS_WALLHACK = register(new CyberwareDefinition(
            "kiroshi_optics_wallhack", "歧路司义眼千里目", CyberwareSlot.FACE, true,
            "cyberware:item/kiroshi_optics_wallhack",
            "扫描时高亮12.5~19.5米内的敌人，效果持续60秒。 扫描或瞄准时6~10倍光学变焦。 镇定调幅：每一点属性值+0.2%爆头和弱点伤害。",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 智能连接 · 胳臂 */
    public static final CyberwareDefinition SMART_LINK = register(new CyberwareDefinition(
            "smart_link", "智能连接", CyberwareSlot.ARMS, true,
            "cyberware:item/smart_link",
            "可以在智能武器上使用智能瞄准。 +10%~20%目标锁定持续时间 智能武器+5%~15%暴击伤害 将使用者的光学植入体和武器系统直接连接，提供实时武器信息。 +1~2最大RAM 智力调幅：每一点属性值+0.1%智能武器伤害。",
            List.of(variant(CyberwareRarity.LEGENDARY, 4, stats(Stats.RAM, 2, Stats.CRIT_DAMAGE, 15)))));

    /** 弹道协同处理器 · 胳臂 */
    public static final CyberwareDefinition POWER_GRIP = register(new CyberwareDefinition(
            "power_grip", "弹道协同处理器", CyberwareSlot.ARMS, true,
            "cyberware:item/power_grip",
            "动能武器增加跳弹几率并+8%~30%跳弹伤害。 连接使用者的武器和义眼，对武器状态进行实时数据追踪，预知跳弹轨迹。 技术调幅：每一点属性值+0.1%跳弹伤害。",
            List.of(variant(CyberwareRarity.LEGENDARY, 2, stats()))));

    /** 握柄固定套 · 胳臂 */
    public static final CyberwareDefinition KNIFE_SHARPENER = register(new CyberwareDefinition(
            "knife_sharpener", "握柄固定套", CyberwareSlot.ARMS, true,
            "cyberware:item/knife_sharpener",
            "装备或投掷一把投掷武器时，投掷武器+8%~27%暴击率，持续6秒。 镇定调幅：每一点属性值+0.2%爆头和弱点伤害。",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats(Stats.CRIT_CHANCE, 27, Stats.DURATION, 6)))));

    /** 微发电机 · 胳臂 */
    public static final CyberwareDefinition MICRO_GENERATOR = register(new CyberwareDefinition(
            "micro_generator", "微发电机", CyberwareSlot.ARMS, true,
            "cyberware:item/micro_generator",
            "为空仓武器装弹会使得下一次射击释放一道电流，对着弹点附近的敌人造成最多75~250电子伤害。 智力调幅：每一点属性值+0.05%造成的所有伤害。",
            List.of(variant(CyberwareRarity.LEGENDARY, 9, stats()))));

    /** 省力减震 · 胳臂 */
    public static final CyberwareDefinition JOINT_LOCK = register(new CyberwareDefinition(
            "joint_lock", "省力减震", CyberwareSlot.ARMS, true,
            "cyberware:item/joint_lock",
            "-8%~24%后坐力 镇定调幅：每一点属性值+0.2%爆头和弱点伤害。",
            List.of(variant(CyberwareRarity.LEGENDARY, 12, stats(Stats.RECOIL, -24)))));

    // 待素材：暂无贴图素材，暂不注册（见 ASSET-MISSING.txt）
    // /** 纹身虎爪帮 · 胳臂 */
    // public static final CyberwareDefinition YAKUZA_TATTOO = register(new CyberwareDefinition(
    //         "yakuza_tattoo", "纹身虎爪帮", CyberwareSlot.ARMS, true,
    //         "cyberware:item/yakuza_tattoo",
    //         "可以在智能武器上使用智能瞄准。 +10%~20%目标锁定持续时间 将使用者的光学植入体和武器系统直接连接，提供实时武器信息。",
    //         List.of(variant(CyberwareRarity.LEGENDARY, 0, stats()))));

    // 待素材：暂无贴图素材，暂不注册（见 ASSET-MISSING.txt）
    // /** 纹身永远在一起 · 胳臂 */
    // public static final CyberwareDefinition SILVERHAND_TATTOO = register(new CyberwareDefinition(
    //         "silverhand_tattoo", "纹身永远在一起", CyberwareSlot.ARMS, true,
    //         "cyberware:item/silverhand_tattoo",
    //         "可以在智能武器上使用智能瞄准。 将使用者的光学植入体和武器系统直接连接，提供实时武器信息。",
    //         List.of(variant(CyberwareRarity.LEGENDARY, 0, stats()))));

    // 待素材：暂无贴图素材，暂不注册（见 ASSET-MISSING.txt）
    // /** 纹身强尼特制 · 胳臂 */
    // public static final CyberwareDefinition CASIUS_TATTOO = register(new CyberwareDefinition(
    //         "casius_tattoo", "纹身强尼特制", CyberwareSlot.ARMS, true,
    //         "cyberware:item/casius_tattoo",
    //         "可以在智能武器上使用智能瞄准。 将使用者的光学植入体和武器系统直接连接，提供实时武器信息。",
    //         List.of(variant(CyberwareRarity.LEGENDARY, 0, stats()))));

    /** 生物监测 · 循环系统 */
    public static final CyberwareDefinition BIOMONITOR = register(new CyberwareDefinition(
            "biomonitor", "生物监测", CyberwareSlot.CIRCULATORY, true,
            "cyberware:item/biomonitor",
            "生命值降至50%以下时，使用装备的生命值物品自动进行治疗。 +2%~16%生命值物品效率 技术调幅：每一点属性值+0.1%生命值物品效率。",
            List.of(variant(CyberwareRarity.LEGENDARY, 14, stats()))));

    /** 副心脏 · 循环系统 */
    public static final CyberwareDefinition SECOND_HEART = register(new CyberwareDefinition(
            "second_heart", "副心脏", CyberwareSlot.CIRCULATORY, true,
            "cyberware:item/second_heart",
            "生命值为0时，+100%生命值。（某些情况下无法激活，例如从极高的高处坠落） 冷却时间：300~200秒。 肉体调幅：每一点属性值+0.5生命值。",
            List.of(variant(CyberwareRarity.LEGENDARY, 30, stats(Stats.COOLDOWN, 200)))));

    /** 击杀治疗 · 循环系统 */
    public static final CyberwareDefinition HEAL_ON_KILL = register(new CyberwareDefinition(
            "heal_on_kill", "击杀治疗", CyberwareSlot.CIRCULATORY, true,
            "cyberware:item/heal_on_kill",
            "消灭一名敌人时+3%~7.5%生命值。 智力调幅：每一点属性值+0.05%造成的所有伤害。",
            List.of(variant(CyberwareRarity.LEGENDARY, 10, stats(Stats.KILL_HEAL, 7.5)))));

    /** 黑曼巴 · 循环系统 */
    public static final CyberwareDefinition VIRAL_VENOM = register(new CyberwareDefinition(
            "viral_venom", "黑曼巴", CyberwareSlot.CIRCULATORY, true,
            "cyberware:item/viral_venom",
            "造成的中毒伤害-90%，但对于受到中毒效果影响的敌人+10%~22%伤害。 反应调幅：每一点属性值+0.1%暴击率。",
            List.of(variant(CyberwareRarity.LEGENDARY, 16, stats(Stats.ALL_DAMAGE, 22)))));

    /** 风险归避 · 循环系统 */
    public static final CyberwareDefinition CATCH_ME_IF_YOU_CAN = register(new CyberwareDefinition(
            "catch_me_if_you_can", "风险归避", CyberwareSlot.CIRCULATORY, true,
            "cyberware:item/catch_me_if_you_can",
            "生命值降至25%时，+5%~29%移动速度。 生命值越低，增加的移动速度越多（最高+15%~39%）。 智力调幅：每一点属性值+0.05%造成的所有伤害。",
            List.of(variant(CyberwareRarity.LEGENDARY, 5, stats()))));

    /** 活血泵 · 循环系统 */
    public static final CyberwareDefinition BLOOD_PUMP = register(new CyberwareDefinition(
            "blood_pump", "活血泵", CyberwareSlot.CIRCULATORY, true,
            "cyberware:item/blood_pump",
            "相当于效果强大的生命值物品。 立即恢复45~110生命值，然后每秒恢复9~23生命值，持续6秒。 技术调幅：每一点属性值+0.1%生命值物品效率。",
            List.of(variant(CyberwareRarity.LEGENDARY, 15, stats(Stats.DURATION, 6)))));

    // 待素材：暂无贴图素材，暂不注册（见 ASSET-MISSING.txt）
    // /** 持握衬垫 · 循环系统 */
    // public static final CyberwareDefinition SHOCK_ABSORBER = register(new CyberwareDefinition(
    //         "shock_absorber", "持握衬垫", CyberwareSlot.CIRCULATORY, true,
    //         "cyberware:item/shock_absorber",
    //         "射击-7%~20%耐力消耗。 肉体调幅：每一点属性值+0.5生命值。",
    //         List.of(variant(CyberwareRarity.LEGENDARY, 12, stats()))));

    /** 反馈电路 · 循环系统 */
    public static final CyberwareDefinition DISCHARGE_CONNECTOR = register(new CyberwareDefinition(
            "discharge_connector", "反馈电路", CyberwareSlot.CIRCULATORY, true,
            "cyberware:item/discharge_connector",
            "使用完全充能的技术武器射击命中敌人时，+1.5%~3.5%生命值。 技术调幅：每一点属性值+0.1%技术武器伤害。",
            List.of(variant(CyberwareRarity.LEGENDARY, 14, stats()))));

    /** 肾上腺素增强件 · 循环系统 */
    public static final CyberwareDefinition STAMINA_REGEN_BOOSTER = register(new CyberwareDefinition(
            "stamina_regen_booster", "肾上腺素增强件", CyberwareSlot.CIRCULATORY, true,
            "cyberware:item/stamina_regen_booster",
            "使用近战武器消灭一名敌人时，+10%~25%耐力。 反应调幅：每一点属性值+0.1%暴击率。",
            List.of(variant(CyberwareRarity.LEGENDARY, 14, stats()))));

    /** 微型转子 · 循环系统 */
    public static final CyberwareDefinition CYBER_ROTORS = register(new CyberwareDefinition(
            "cyber_rotors", "微型转子", CyberwareSlot.CIRCULATORY, true,
            "cyberware:item/cyber_rotors",
            "+10%~25%近战攻击速度 反应调幅：每一点属性值+0.1%暴击率。",
            List.of(variant(CyberwareRarity.LEGENDARY, 12, stats()))));

    /** 强化肌健 · 腿部 */
    public static final CyberwareDefinition BOOSTED_TENDONS = register(new CyberwareDefinition(
            "boosted_tendons", "强化肌健", CyberwareSlot.LEGS, true,
            "cyberware:item/boosted_tendons",
            "提供护甲：10 在空中按下跳跃键进行二段跳。 反应调幅：每一点属性值+0.1%暴击率。",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats(Stats.ARMOR, 10)))));

    /** 猞猁爪 · 腿部 */
    public static final CyberwareDefinition CAT_PAWS = register(new CyberwareDefinition(
            "cat_paws", "猞猁爪", CyberwareSlot.LEGS, true,
            "cyberware:item/cat_paws",
            "提供护甲：16~34 -50%移动时声音大小 +6%~12%蹲伏移动速度 -20%坠落伤害 镇定调幅：每一点属性值+0.2%爆头和弱点伤害。",
            List.of(variant(CyberwareRarity.LEGENDARY, 5, stats(Stats.ARMOR, 34)))));

    /** 火车王肌建 · 腿部 */
    public static final CyberwareDefinition JENKINS_TENDONS = register(new CyberwareDefinition(
            "jenkins_tendons", "火车王肌建", CyberwareSlot.LEGS, true,
            "cyberware:item/jenkins_tendons",
            "提供护甲：18~54 增加冲刺速度，初始为60%，在继续冲刺的5秒后逐渐减少到+10%。 不处于冲刺状态时，效果会以同样的速率恢复。 肉体调幅：每一点属性值+0.5生命值。",
            List.of(variant(CyberwareRarity.LEGENDARY, 6, stats(Stats.ARMOR, 54)))));

    /** 踝部加固 · 腿部 */
    public static final CyberwareDefinition REINFORCED_MUSCLES = register(new CyberwareDefinition(
            "reinforced_muscles", "踝部加固", CyberwareSlot.LEGS, true,
            "cyberware:item/reinforced_muscles",
            "提供护甲：35~103 允许你使用蓄力跳——跳跃距离更远。 长按跳跃键蓄力，松开后跳跃。 肉体调幅：每一点属性值+0.5生命值。",
            List.of(variant(CyberwareRarity.LEGENDARY, 6, stats(Stats.ARMOR, 103)))));

    /** 思想防线 · 表皮系统 */
    public static final CyberwareDefinition COGITO_FRAME = register(new CyberwareDefinition(
            "cogito_frame", "思想防线", CyberwareSlot.INTEGUMENTARY, true,
            "cyberware:item/cogito_frame",
            "提供护甲：18~54 可用RAM低于2~10时，+200%~240%护甲（需要安装网络接入仓）。 智力调幅：每一点属性值+2护甲。",
            List.of(variant(CyberwareRarity.LEGENDARY, 12, stats(Stats.ARMOR, 54)))));

    /** 细胞适配 · 表皮系统 */
    public static final CyberwareDefinition ADAPTIVE_STEM_CELLS = register(new CyberwareDefinition(
            "adaptive_stem_cells", "细胞适配", CyberwareSlot.INTEGUMENTARY, true,
            "cyberware:item/adaptive_stem_cells",
            "提供护甲：74~162 技术调幅：每一点属性值提供：+1%爆炸抗性、+0.5%技术武器伤害、+0.5%生命值物品补充速度、+0.5%手雷补充速度。",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats(Stats.ARMOR, 162)))));

    /** 皮下护甲 · 表皮系统 */
    public static final CyberwareDefinition BORING_PLATING = register(new CyberwareDefinition(
            "boring_plating", "皮下护甲", CyberwareSlot.INTEGUMENTARY, true,
            "cyberware:item/boring_plating",
            "提供护甲：17~49 就是护甲。没那么多花样，但是管用。 技术调幅：每一点属性值+0.5护甲。",
            List.of(variant(CyberwareRarity.LEGENDARY, 4, stats(Stats.ARMOR, 49)))));

    /** 近接防盾 · 表皮系统 */
    public static final CyberwareDefinition PROXIMITY_REDUCER = register(new CyberwareDefinition(
            "proximity_reducer", "近接防盾", CyberwareSlot.INTEGUMENTARY, true,
            "cyberware:item/proximity_reducer",
            "提供护甲：8~36 攻击你的敌人越近，他们造成的伤害就越低。 在3米处-8%~20%受到的伤害。 伤害减免在6米处减少到0。 肉体调幅：每一点属性值+0.5生命值。",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats(Stats.ARMOR, 36)))));

    /** 疼痛编辑器 · 表皮系统 */
    public static final CyberwareDefinition PAIN_REDUCTOR = register(new CyberwareDefinition(
            "pain_reductor", "疼痛编辑器", CyberwareSlot.INTEGUMENTARY, true,
            "cyberware:item/pain_reductor",
            "提供护甲：88~108 -6%~7%受到的所有伤害。 镇定调幅：每一点属性值+0.1%伤害减免。",
            List.of(variant(CyberwareRarity.LEGENDARY, 28, stats(Stats.ARMOR, 108)))));

    /** 疼痛置换 · 表皮系统 */
    public static final CyberwareDefinition BLOOD_DEPLETER = register(new CyberwareDefinition(
            "blood_depleter", "疼痛置换", CyberwareSlot.INTEGUMENTARY, true,
            "cyberware:item/blood_depleter",
            "提供护甲：114~138 将受到伤害的25%~30%转换为持续性伤害。 肉体调幅：每一点属性值+0.5%持续伤害抗性。",
            List.of(variant(CyberwareRarity.LEGENDARY, 24, stats(Stats.ARMOR, 138)))));

    /** 拒敌防护 · 表皮系统 */
    public static final CyberwareDefinition CHARGE_SYSTEM = register(new CyberwareDefinition(
            "charge_system", "拒敌防护", CyberwareSlot.INTEGUMENTARY, true,
            "cyberware:item/charge_system",
            "提供护甲：10~34 6米内没有敌人时，+30~90护甲。 镇定调幅：每一点属性值+0.2%爆头和弱点伤害。",
            List.of(variant(CyberwareRarity.LEGENDARY, 14, stats(Stats.ARMOR, 34)))));

    /** 反制壳层 · 表皮系统 */
    public static final CyberwareDefinition SUDDEN_AID = register(new CyberwareDefinition(
            "sudden_aid", "反制壳层", CyberwareSlot.INTEGUMENTARY, true,
            "cyberware:item/sudden_aid",
            "提供护甲：17~57 如果在3秒内失去35%生命值，+30~50%半伤几率，持续4秒。 冷却时间：6秒 反应调幅：每一点属性值+0.1%暴击率。",
            List.of(variant(CyberwareRarity.LEGENDARY, 10, stats(Stats.ARMOR, 57)))));

    /** 震慑通电 · 表皮系统 */
    public static final CyberwareDefinition ELECTROSHOCK_MECHANISM = register(new CyberwareDefinition(
            "electroshock_mechanism", "震慑通电", CyberwareSlot.INTEGUMENTARY, true,
            "cyberware:item/electroshock_mechanism",
            "提供护甲：44~92 受到伤害时，有10%几率释放大范围电流，对附近的敌人造成130~500伤害。 技术调幅：每一点属性值+0.1%技术武器伤害。",
            List.of(variant(CyberwareRarity.LEGENDARY, 20, stats(Stats.ARMOR, 92)))));

    /** 光学迷彩 · 表皮系统 */
    public static final CyberwareDefinition OPTICAL_CAMO = register(new CyberwareDefinition(
            "optical_camo", "光学迷彩", CyberwareSlot.INTEGUMENTARY, true,
            "cyberware:item/optical_camo",
            "提供护甲：15~40 主动激活：-30%~90%识别度，持续4~7秒，脱离战斗时敌人更难发现你，战斗期间敌人更难命中你。 冷却时间：70~50秒 可以分配到“手雷”快捷栏位。 镇定调幅：每一点属性值使得该义体+0.1秒持续时间。",
            List.of(variant(CyberwareRarity.LEGENDARY, 16, stats(Stats.ARMOR, 40)))));

    /** 纳米镀层 · 表皮系统 */
    public static final CyberwareDefinition NANO_TECH_PLATES = register(new CyberwareDefinition(
            "nano_tech_plates", "纳米镀层", CyberwareSlot.INTEGUMENTARY, true,
            "cyberware:item/nano_tech_plates",
            "提供护甲：38~78 有4%~7%几率格挡飞来的射弹。 进行闪躲或突进后，+100%几率加成，持续1.4~1.7秒或直到下一次射弹被格挡为止。 在6.5~5秒内无法格挡超过3颗射弹。 反应调幅：每一点属性值+0.1%暴击率。",
            List.of(variant(CyberwareRarity.LEGENDARY, 16, stats(Stats.ARMOR, 78)))));

    /** 全域覆盖 · 表皮系统 */
    public static final CyberwareDefinition WEIRD_TANKY_PLATING = register(new CyberwareDefinition(
            "weird_tanky_plating", "全域覆盖", CyberwareSlot.INTEGUMENTARY, true,
            "cyberware:item/weird_tanky_plating",
            "提供护甲：38~94 侧面或背面被攻击时+ 20%~32%护甲效率。 技术调幅：每一点属性值+0.5护甲。",
            List.of(variant(CyberwareRarity.LEGENDARY, 13, stats(Stats.ARMOR, 94)))));

    /** 克伦齐科夫回护 · 表皮系统 */
    public static final CyberwareDefinition PLATING_GLITCH = register(new CyberwareDefinition(
            "plating_glitch", "克伦齐科夫回护", CyberwareSlot.INTEGUMENTARY, true,
            "cyberware:item/plating_glitch",
            "提供护甲：24~80 克伦齐科夫结束时，+50%~90%半伤几率，持续3~4秒。 反应调幅：每一点属性值+0.1%暴击率。",
            List.of(variant(CyberwareRarity.LEGENDARY, 16, stats(Stats.ARMOR, 80)))));

    /** 锥刺子 · 神经系统 */
    public static final CyberwareDefinition OIL_DISPENSER = register(new CyberwareDefinition(
            "oil_dispenser", "锥刺子", CyberwareSlot.NERVOUS_SYSTEM, true,
            "cyberware:item/oil_dispenser",
            "刀剑和投掷武器+10%~20%暴击率。 反应调幅：每一点属性值+0.1%暴击率。",
            List.of(variant(CyberwareRarity.LEGENDARY, 12, stats(Stats.CRIT_CHANCE, 20)))));

    /** 克伦齐科夫 · 神经系统 */
    public static final CyberwareDefinition KERENZIKOV = register(new CyberwareDefinition(
            "kerenzikov", "克伦齐科夫", CyberwareSlot.NERVOUS_SYSTEM, true,
            "cyberware:item/kerenzikov",
            "允许你在滑铲、闪躲或突进时瞄准和进行远程攻击。 如果在滑铲、闪躲或突进时进行远程瞄准，时间减慢60%，持续2~3.75秒。 冷却时间：8~6秒。 反应调幅：每一点属性值+0.1%暴击率。",
            List.of(variant(CyberwareRarity.LEGENDARY, 12, stats(
                    Stats.TIME_SLOW, 0.6, Stats.DURATION, 3.75, Stats.COOLDOWN, 6)))));

    // 待素材：暂无贴图素材，暂不注册（见 ASSET-MISSING.txt）
    // /** 反应协调器 · 神经系统 */
    // public static final CyberwareDefinition REFLEX_RECORDER = register(new CyberwareDefinition(
    //         "reflex_recorder", "反应协调器", CyberwareSlot.NERVOUS_SYSTEM, true,
    //         "cyberware:item/reflex_recorder",
    //         "生命值降到25%以下时，时间减慢20%~60%，持续2~4.5秒。 冷却时间：60~35秒 镇定调幅：每一点属性值+0.2%爆头和弱点伤害。",
    //         List.of(variant(CyberwareRarity.LEGENDARY, 5, stats(
    //                 Stats.TIME_SLOW, 0.6, Stats.DURATION, 4.5, Stats.COOLDOWN, 35)))));

    /** 突触加速器 · 神经系统 */
    public static final CyberwareDefinition SYNAPTIC_ACCELERATOR = register(new CyberwareDefinition(
            "synaptic_accelerator", "突触加速器", CyberwareSlot.NERVOUS_SYSTEM, true,
            "cyberware:item/synaptic_accelerator",
            "敌人的发现程度达到50%时，时间减慢20%~50%，持续2~4.5秒。 冷却时间：60秒。 镇定调幅：每一点属性值+0.2%爆头和弱点伤害。",
            List.of(variant(CyberwareRarity.LEGENDARY, 5, stats(
                    Stats.TIME_SLOW, 0.5, Stats.DURATION, 4.5, Stats.COOLDOWN, 60)))));

    /** 纳米纤维 · 神经系统 */
    public static final CyberwareDefinition NEO_FIBER = register(new CyberwareDefinition(
            "neo_fiber", "纳米纤维", CyberwareSlot.NERVOUS_SYSTEM, true,
            "cyberware:item/neo_fiber",
            "+8%~11%半伤几率 +5%~11%半伤强度 反应调幅：每一点属性值+0.1%暴击率。",
            List.of(variant(CyberwareRarity.LEGENDARY, 14, stats()))));

    /** 视觉皮质支持 · 神经系统 */
    public static final CyberwareDefinition VISUAL_CORTEX_SUPPORT = register(new CyberwareDefinition(
            "visual_cortex_support", "视觉皮质支持", CyberwareSlot.NERVOUS_SYSTEM, true,
            "cyberware:item/visual_cortex_support",
            "距离敌人越远，暴击率越高（最多在30米处+16%~30%） 镇定调幅：每一点属性值+0.2%爆头和弱点伤害。",
            List.of(variant(CyberwareRarity.LEGENDARY, 20, stats(Stats.CRIT_CHANCE, 30)))));

    /** 肾上腺素整流 · 神经系统 */
    public static final CyberwareDefinition DETECTOR_RUSH = register(new CyberwareDefinition(
            "detector_rush", "肾上腺素整流", CyberwareSlot.NERVOUS_SYSTEM, true,
            "cyberware:item/detector_rush",
            "进入战斗时+20%~40%移动速度，持续3~9秒。 镇定调幅：每一点属性值+0.2%爆头和弱点伤害。",
            List.of(variant(CyberwareRarity.LEGENDARY, 6, stats(Stats.DURATION, 9)))));

    /** 瞬时感知 · 神经系统 */
    public static final CyberwareDefinition TROUBLE_FINDER = register(new CyberwareDefinition(
            "trouble_finder", "瞬时感知", CyberwareSlot.NERVOUS_SYSTEM, true,
            "cyberware:item/trouble_finder",
            "根据脱离战斗时敌人的发现程度正比增加移动速度（最多为发现程度80%时+20%~64%移动速度） 进入战斗时效果结束。 镇定调幅：每一点属性值+0.2%爆头和弱点伤害。",
            List.of(variant(CyberwareRarity.LEGENDARY, 5, stats()))));

    /** 酪氨酸注射器 · 神经系统 */
    public static final CyberwareDefinition TYROSINE_INJECTOR = register(new CyberwareDefinition(
            "tyrosine_injector", "酪氨酸注射器", CyberwareSlot.NERVOUS_SYSTEM, true,
            "cyberware:item/tyrosine_injector",
            "成功击倒提供：+10%~21%爆头伤害 +5%~11%移动速度 持续15秒。 镇定调幅：每一点属性值+0.2%爆头和弱点伤害。",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats(Stats.HEADSHOT_DAMAGE, 21, Stats.DURATION, 15)))));

    /** 死不鸟 · 骨骼 */
    public static final CyberwareDefinition NEURO_MATRIX = register(new CyberwareDefinition(
            "neuro_matrix", "死不鸟", CyberwareSlot.SKELETON, true,
            "cyberware:item/neuro_matrix",
            "提供护甲：8~32 可用RAM低3~7时，+250%RAM恢复速率。 智力调幅：每一点属性值+0.1%伤害减免。",
            List.of(variant(CyberwareRarity.LEGENDARY, 13, stats(Stats.ARMOR, 32)))));

    /** 弹性关节 · 骨骼 */
    public static final CyberwareDefinition AGILE_JOINTS = register(new CyberwareDefinition(
            "agile_joints", "弹性关节", CyberwareSlot.SKELETON, true,
            "cyberware:item/agile_joints",
            "提供护甲：16~34 +10%~16%半伤强度（半伤总强度最高不超过90%）。 肉体调幅：每一点属性值+0.5生命值。",
            List.of(variant(CyberwareRarity.LEGENDARY, 13, stats(Stats.ARMOR, 34)))));

    /** 致密骨骼 · 骨骼 */
    public static final CyberwareDefinition DENSE_MARROW = register(new CyberwareDefinition(
            "dense_marrow", "致密骨骼", CyberwareSlot.SKELETON, true,
            "cyberware:item/dense_marrow",
            "提供护甲：18~54 +15%~27%近战伤害。 +20%近战耐力消耗。 反应调幅：每一点属性值+0.1%暴击率。",
            List.of(variant(CyberwareRarity.LEGENDARY, 13, stats(Stats.ARMOR, 54, Stats.MELEE_DAMAGE, 27)))));

    /** 再造骨骼 · 骨骼 */
    public static final CyberwareDefinition ENDOSKELETON = register(new CyberwareDefinition(
            "endoskeleton", "再造骨骼", CyberwareSlot.SKELETON, true,
            "cyberware:item/endoskeleton",
            "提供护甲：96~186 +10%+15%最大生命值 肉体调幅：每一点属性值+0.5生命值。",
            List.of(variant(CyberwareRarity.LEGENDARY, 32, stats(Stats.ARMOR, 186, Stats.MAX_HEALTH, 15)))));

    /** RAM补偿 · 骨骼 */
    public static final CyberwareDefinition COMPILING_SKELETON = register(new CyberwareDefinition(
            "compiling_skeleton", "RAM补偿", CyberwareSlot.SKELETON, true,
            "cyberware:item/compiling_skeleton",
            "提供护甲：10~46 受到伤害时恢复RAM，恢复量相当于所受伤害的2%~3%。 最大RAM增加1~2。 智力调幅：每一点属性值+0.05%造成的所有伤害。",
            List.of(variant(CyberwareRarity.LEGENDARY, 11, stats(Stats.ARMOR, 46, Stats.RAM, 2)))));

    /** 清创凝合 · 骨骼 */
    public static final CyberwareDefinition NO_PAIN_NO_GAIN = register(new CyberwareDefinition(
            "no_pain_no_gain", "清创凝合", CyberwareSlot.SKELETON, true,
            "cyberware:item/no_pain_no_gain",
            "提供护甲：16~34 生命值低于50%时，+10%~22%护甲。 肉体调幅：每一点属性值+0.5%生命值。",
            List.of(variant(CyberwareRarity.LEGENDARY, 16, stats(Stats.ARMOR, 34)))));

    // 待素材：暂无贴图素材，暂不注册（见 ASSET-MISSING.txt）
    // /** 通用增强件 · 骨骼 */
    // public static final CyberwareDefinition PAIN_DISTRIBUTOR = register(new CyberwareDefinition(
    //         "pain_distributor", "通用增强件", CyberwareSlot.SKELETON, true,
    //         "cyberware:item/pain_distributor",
    //         "提供护甲：42~138 生命值物品提供+5%~9%护甲和-15%~27%所有耐力消耗，持续5秒，可以叠加。 技术调幅：每一点属性值+0.5护甲。",
    //         List.of(variant(CyberwareRarity.LEGENDARY, 20, stats(Stats.ARMOR, 138)))));

    /** 动能骨架 · 骨骼 */
    public static final CyberwareDefinition BONE_MARROW_CELLS = register(new CyberwareDefinition(
            "bone_marrow_cells", "动能骨架", CyberwareSlot.SKELETON, true,
            "cyberware:item/bone_marrow_cells",
            "提供护甲：15~117 耐力超过85%时，+8%~18%半伤几率。 反应调幅：每一点属性值+0.1%暴击率。",
            List.of(variant(CyberwareRarity.LEGENDARY, 13, stats(Stats.ARMOR, 117)))));

    // 待素材：暂无贴图素材，暂不注册（见 ASSET-MISSING.txt）
    // /** 仿生关节 · 骨骼 */
    // public static final CyberwareDefinition BIONIC_JOINTS = register(new CyberwareDefinition(
    //         "bionic_joints", "仿生关节", CyberwareSlot.SKELETON, true,
    //         "cyberware:item/bionic_joints",
    //         "提供护甲：25~85 就是护甲。没那么多花样，但是管用。 技术调幅：每一点属性值+0.5护甲。",
    //         List.of(variant(CyberwareRarity.LEGENDARY, 6, stats(Stats.ARMOR, 85)))));

    /** 圣甲虫 · 骨骼 */
    public static final CyberwareDefinition RAPID_MUSCLE_NURISH = register(new CyberwareDefinition(
            "rapid_muscle_nurish", "圣甲虫", CyberwareSlot.SKELETON, true,
            "cyberware:item/rapid_muscle_nurish",
            "提供护甲：10~34 蹲伏时，+20~110护甲，-20%移动速度。 技术调幅：每一点属性值+0.5%护甲。",
            List.of(variant(CyberwareRarity.LEGENDARY, 11, stats(Stats.ARMOR, 34)))));

    /** 百拳开 · 骨骼 */
    public static final CyberwareDefinition T1000 = register(new CyberwareDefinition(
            "t1000", "百拳开", CyberwareSlot.SKELETON, true,
            "cyberware:item/t1000",
            "提供护甲：30~150 +8%~13%护甲。 技术调幅：每一点属性值+0.5护甲。",
            List.of(variant(CyberwareRarity.LEGENDARY, 20, stats(Stats.ARMOR, 150)))));

    /** 钛金骨骼 · 骨骼 */
    public static final CyberwareDefinition TITANIUM_INFUSED_BONES = register(new CyberwareDefinition(
            "titanium_infused_bones", "钛金骨骼", CyberwareSlot.SKELETON, true,
            "cyberware:item/titanium_infused_bones",
            "提供护甲：10~34 +30%~62%负重。 肉体调幅：每一点属性值+0.5生命值。",
            List.of(variant(CyberwareRarity.LEGENDARY, 5, stats()))));

    // 待素材：暂无贴图素材，暂不注册（见 ASSET-MISSING.txt）
    // /** 生物导体 · 额皮质 */
    // public static final CyberwareDefinition BIO_CONDUCTORS = register(new CyberwareDefinition(
    //         "bio_conductors", "生物导体", CyberwareSlot.FRONTAL_CORTEX, true,
    //         "cyberware:item/bio_conductors",
    //         "允许你的快速破解造成暴击。快速破解有15%~35%的几率暴击。 最大RAM减少4。 智力调幅：每一点属性+0.05%造成的所有伤害。",
    //         List.of(variant(CyberwareRarity.LEGENDARY, 20, stats(Stats.RAM, -4)))));

    /** 卡米略RAM管理器 · 额皮质 */
    public static final CyberwareDefinition CAMILLO_RAM_MANAGER = register(new CyberwareDefinition(
            "camillo_ram_manager", "卡米略RAM管理器", CyberwareSlot.FRONTAL_CORTEX, true,
            "cyberware:item/camillo_ram_manager",
            "最大RAM增加2。 可用RAM降到15%~20%时，立即恢复20%~23%的最大RAM。 冷却时间：80秒 智力调幅：每一点属性使该义体-2秒冷却时间。",
            List.of(variant(CyberwareRarity.LEGENDARY, 10, stats(Stats.RAM, 2, Stats.COOLDOWN, 80)))));

    /** 外接盘 · 额皮质 */
    public static final CyberwareDefinition EX_DISK = register(new CyberwareDefinition(
            "ex_disk", "外接盘", CyberwareSlot.FRONTAL_CORTEX, true,
            "cyberware:item/ex_disk",
            "最大RAM增加4~6。快速破解上传速度提高20%~35%。 智力调幅：每一点属性+0.05%造成的所有伤害。",
            List.of(variant(CyberwareRarity.LEGENDARY, 10, stats(Stats.RAM, 6)))));

    /** 克伦齐科夫增幅 · 额皮质 */
    public static final CyberwareDefinition KERENZIOV_BOOST_SYSTEM = register(new CyberwareDefinition(
            "kerenziov_boost_system", "克伦齐科夫增幅", CyberwareSlot.FRONTAL_CORTEX, true,
            "cyberware:item/kerenziov_boost_system",
            "克伦齐科夫激活时：射击的耐力消耗降低100%，相对于敌人时间减慢5%~15%。 反应调幅：每一点属性+0.1%暴击率。",
            List.of(variant(CyberwareRarity.LEGENDARY, 3, stats(Stats.TIME_SLOW, 0.15)))));

    // 待素材：暂无贴图素材，暂不注册（见 ASSET-MISSING.txt）
    // /** 机电核心 · 额皮质 */
    // public static final CyberwareDefinition MECHATRONIC_CORE = register(new CyberwareDefinition(
    //         "mechatronic_core", "机电核心", CyberwareSlot.FRONTAL_CORTEX, true,
    //         "cyberware:item/mechatronic_core",
    //         "对无人机、机器人、机甲和炮塔造成的伤害提高15%~40%。最大RAM增加1~2。 技术能力调幅：每一点属性+0.5护甲。",
    //         List.of(variant(CyberwareRarity.LEGENDARY, 5, stats(Stats.RAM, 2, Stats.ALL_DAMAGE, 40)))));

    /** 内存加强 · 额皮质 */
    public static final CyberwareDefinition MEMORY_BOOST = register(new CyberwareDefinition(
            "memory_boost", "内存加强", CyberwareSlot.FRONTAL_CORTEX, true,
            "cyberware:item/memory_boost",
            "消灭一名敌人时恢复0.4~1.25RAM。最大RAM增加1。 智力调幅：每一点属性+0.05%造成的所有伤害。",
            List.of(variant(CyberwareRarity.LEGENDARY, 18, stats(Stats.RAM, 1)))));

    /** RAM升级 · 额皮质 */
    public static final CyberwareDefinition RAM_UPGRADE = register(new CyberwareDefinition(
            "ram_upgrade", "RAM升级", CyberwareSlot.FRONTAL_CORTEX, true,
            "cyberware:item/ram_upgrade",
            "RAM恢复速率每秒增加0.05~0.2个单位。最大RAM增加1~2。",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats(Stats.RAM, 2, Stats.RAM_REGEN, 0.2)))));

    /** 自我ICE · 额皮质 */
    public static final CyberwareDefinition SELF_ICE = register(new CyberwareDefinition(
            "self_ice", "自我ICE", CyberwareSlot.FRONTAL_CORTEX, true,
            "cyberware:item/self_ice",
            "自动抵消一个敌方快速破解。冷却时间：45秒~20秒。最大RAM增加1~2。",
            List.of(variant(CyberwareRarity.LEGENDARY, 5, stats(Stats.RAM, 2, Stats.COOLDOWN, 20)))));

    // 待素材：暂无贴图素材，暂不注册（见 ASSET-MISSING.txt）
    // /** 蓄电缓冲 · 额皮质 */
    // public static final CyberwareDefinition SMART_STORAGE = register(new CyberwareDefinition(
    //         "smart_storage", "蓄电缓冲", CyberwareSlot.FRONTAL_CORTEX, true,
    //         "cyberware:item/smart_storage",
    //         "蓄电缓冲。TODO(数值与描述待补)",
    //         List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    // 待素材：暂无贴图素材，暂不注册（见 ASSET-MISSING.txt）
    // /** 牛顿模块 · 额皮质 */
    // public static final CyberwareDefinition SUBDERMAL_CO_PROCESSOR = register(new CyberwareDefinition(
    //         "subdermal_co_processor", "牛顿模块", CyberwareSlot.FRONTAL_CORTEX, true,
    //         "cyberware:item/subdermal_co_processor",
    //         "消灭一名敌人后，所有义体立即减少0.3%~1.35%冷却时间。 技术能力调幅：每一点属性+0.5护甲。",
    //         List.of(variant(CyberwareRarity.LEGENDARY, 14, stats()))));
    // ═══════════════════ 新增：无上游出处的 12 件（主人裁决「这 12 张素材做成义体」） ═══════════════════
    // 三份上游文档（ID对照.txt / 待插入-123条定义.txt / 描述.txt）都没有这 12 个中文名：
    // 除 id 与中文名外，槽位/稀有度/容量/数值/描述全部无来源，均为待确认初值。

    // TODO(数据待确认)：槽位/稀有度/容量无上游来源，暂按素材名语义取 循环系统 + LEGENDARY + 占位容量 8，待主人确认
    /** 代谢编辑器 · 循环系统 */
    public static final CyberwareDefinition METABOLIC_EDITOR = register(new CyberwareDefinition(
            "metabolic_editor", "代谢编辑器", CyberwareSlot.CIRCULATORY, true,
            "cyberware:item/metabolic_editor",
            "代谢编辑器。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    // TODO(数据待确认)：槽位/稀有度/容量无上游来源，暂按素材名语义取 表皮系统 + LEGENDARY + 占位容量 8，待主人确认
    /** 全幅抵抗 · 表皮系统 */
    public static final CyberwareDefinition FULL_RESISTANCE = register(new CyberwareDefinition(
            "full_resistance", "全幅抵抗", CyberwareSlot.INTEGUMENTARY, true,
            "cyberware:item/full_resistance",
            "全幅抵抗。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    // TODO(数据待确认)：槽位/稀有度/容量无上游来源，暂按素材名语义取 循环系统 + LEGENDARY + 占位容量 8，待主人确认
    /** 反向电感 · 循环系统 */
    public static final CyberwareDefinition REVERSE_INDUCTOR = register(new CyberwareDefinition(
            "reverse_inductor", "反向电感", CyberwareSlot.CIRCULATORY, true,
            "cyberware:item/reverse_inductor",
            "反向电感。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    // TODO(数据待确认)：槽位/稀有度/容量无上游来源，暂按素材名语义取 循环系统 + LEGENDARY + 占位容量 8，待主人确认
    /** 合成肺叶 · 循环系统 */
    public static final CyberwareDefinition SYNTHETIC_LUNG = register(new CyberwareDefinition(
            "synthetic_lung", "合成肺叶", CyberwareSlot.CIRCULATORY, true,
            "cyberware:item/synthetic_lung",
            "合成肺叶。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    // TODO(数据待确认)：槽位/稀有度/容量无上游来源，暂按素材名语义取 表皮系统 + LEGENDARY + 占位容量 8，待主人确认
    /** 接地镀层 · 表皮系统 */
    public static final CyberwareDefinition GROUNDING_PLATING = register(new CyberwareDefinition(
            "grounding_plating", "接地镀层", CyberwareSlot.INTEGUMENTARY, true,
            "cyberware:item/grounding_plating",
            "接地镀层。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    // TODO(数据待确认)：槽位/稀有度/容量无上游来源，暂按素材名语义取 循环系统 + LEGENDARY + 占位容量 8，待主人确认
    /** 热能转化器 · 循环系统 */
    public static final CyberwareDefinition THERMAL_CONVERTER = register(new CyberwareDefinition(
            "thermal_converter", "热能转化器", CyberwareSlot.CIRCULATORY, true,
            "cyberware:item/thermal_converter",
            "热能转化器。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    // TODO(数据待确认)：槽位/稀有度/容量无上游来源，暂按素材名语义取 循环系统 + LEGENDARY + 占位容量 8，待主人确认
    /** 生物塑料血管 · 循环系统 */
    public static final CyberwareDefinition BIOPLASTIC_VESSELS = register(new CyberwareDefinition(
            "bioplastic_vessels", "生物塑料血管", CyberwareSlot.CIRCULATORY, true,
            "cyberware:item/bioplastic_vessels",
            "生物塑料血管。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    // TODO(数据待确认)：槽位/稀有度/容量无上游来源，暂按素材名语义取 表皮系统 + LEGENDARY + 占位容量 8，待主人确认
    /** 真皮上编束 · 表皮系统 */
    public static final CyberwareDefinition DERMAL_WEAVE = register(new CyberwareDefinition(
            "dermal_weave", "真皮上编束", CyberwareSlot.INTEGUMENTARY, true,
            "cyberware:item/dermal_weave",
            "真皮上编束。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    // TODO(数据待确认)：槽位/稀有度/容量无上游来源，暂按素材名语义取 神经系统 + LEGENDARY + 占位容量 8，待主人确认
    /** 纳米继电器 · 神经系统 */
    public static final CyberwareDefinition NANO_RELAY = register(new CyberwareDefinition(
            "nano_relay", "纳米继电器", CyberwareSlot.NERVOUS_SYSTEM, true,
            "cyberware:item/nano_relay",
            "纳米继电器。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    // TODO(数据待确认)：槽位/稀有度/容量无上游来源，暂按素材名语义取 循环系统 + LEGENDARY + 占位容量 8，待主人确认
    /** 解毒器 · 循环系统 */
    public static final CyberwareDefinition DETOXIFIER = register(new CyberwareDefinition(
            "detoxifier", "解毒器", CyberwareSlot.CIRCULATORY, true,
            "cyberware:item/detoxifier",
            "解毒器。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    // TODO(数据待确认)：槽位/稀有度/容量无上游来源，暂按素材名语义取 神经系统 + LEGENDARY + 占位容量 8，待主人确认
    /** 边缘增强系统 · 神经系统 */
    public static final CyberwareDefinition EDGE_ENHANCEMENT_SYSTEM = register(new CyberwareDefinition(
            "edge_enhancement_system", "边缘增强系统", CyberwareSlot.NERVOUS_SYSTEM, true,
            "cyberware:item/edge_enhancement_system",
            "边缘增强系统。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    // TODO(数据待确认)：槽位/稀有度/容量无上游来源，暂按素材名语义取 表皮系统 + LEGENDARY + 占位容量 8，待主人确认
    /** 防火涂层 · 表皮系统 */
    public static final CyberwareDefinition FIREPROOF_COATING = register(new CyberwareDefinition(
            "fireproof_coating", "防火涂层", CyberwareSlot.INTEGUMENTARY, true,
            "cyberware:item/fireproof_coating",
            "防火涂层。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));
    // ═══════════════════ 工具方法 ═══════════════════

    private static CyberwareDefinition register(CyberwareDefinition def) {
        BY_ID.put(def.id(), def);
        return def;
    }

    private static Variant variant(CyberwareRarity rarity, int capacity, Map<String, Double> stats) {
        return new Variant(rarity, capacity, stats);
    }

    /** 交替传入 key, value。 */
    private static Map<String, Double> stats(Object... pairs) {
        Map<String, Double> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put((String) pairs[i], ((Number) pairs[i + 1]).doubleValue());
        }
        return map;
    }

    public static Map<String, CyberwareDefinition> all() {
        return java.util.Collections.unmodifiableMap(BY_ID);
    }

    public static CyberwareDefinition byId(String id) {
        return BY_ID.get(id);
    }

    private CyberwareDefinitions() {
    }
}
