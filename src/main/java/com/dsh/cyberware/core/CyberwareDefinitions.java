package com.dsh.cyberware.core;

import com.dsh.cyberware.core.CyberwareDefinition.Stats;
import com.dsh.cyberware.core.CyberwareDefinition.Variant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 全部义体型号的定义表。
 *
 * <p>数值严格照《义体拓展》文档录入；文档没给数值的条目标了
 * {@code TODO(主人填写)}，不影响编译与显示。
 */
public final class CyberwareDefinitions {

    private static final Map<String, CyberwareDefinition> BY_ID = new LinkedHashMap<>();

    // ═══════════════════ 操作系统 · 斯安威斯坦 ═══════════════════

    /** 1. 泽塔科技·斯安威斯坦 —— 普通/精良/史诗 */
    public static final CyberwareDefinition SANDEVISTAN_ZETATECH = register(new CyberwareDefinition(
            "sandevistan_zetatech", "泽塔科技·斯安威斯坦", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/sandevistan_zetatech",
            "泽塔科技的入门级斯安威斯坦，改写神经系统对时间的感知。",
            List.of(
                    variant(CyberwareRarity.COMMON, 12, stats(
                            Stats.TIME_SLOW, 0.25, Stats.DURATION, 8, Stats.COOLDOWN, 30, Stats.CRIT_CHANCE, 10)),
                    variant(CyberwareRarity.UNCOMMON, 18, stats(
                            Stats.TIME_SLOW, 0.50, Stats.DURATION, 12, Stats.COOLDOWN, 30, Stats.CRIT_CHANCE, 15)),
                    variant(CyberwareRarity.EPIC, 22, stats(
                            Stats.TIME_SLOW, 0.50, Stats.DURATION, 16, Stats.COOLDOWN, 30, Stats.CRIT_CHANCE, 20)))));

    /** 2. 迪纳拉·斯安威斯坦 —— 普通/精良/史诗/传说 */
    public static final CyberwareDefinition SANDEVISTAN_DYNALAR = register(new CyberwareDefinition(
            "sandevistan_dynalar", "迪纳拉·斯安威斯坦", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/sandevistan_dynalar",
            "以稳定性著称的斯安威斯坦，改造越深，出血量越大。",
            List.of(
                    variant(CyberwareRarity.COMMON, 12, stats(
                            Stats.TIME_SLOW, 0.50, Stats.DURATION, 8, Stats.COOLDOWN, 30, Stats.ALL_DAMAGE, 5)),
                    variant(CyberwareRarity.UNCOMMON, 18, stats(
                            Stats.TIME_SLOW, 0.75, Stats.DURATION, 12, Stats.COOLDOWN, 30, Stats.ALL_DAMAGE, 10)),
                    variant(CyberwareRarity.EPIC, 24, stats(
                            Stats.TIME_SLOW, 0.50, Stats.DURATION, 16, Stats.COOLDOWN, 30, Stats.ALL_DAMAGE, 15)),
                    variant(CyberwareRarity.LEGENDARY, 30, stats(
                            Stats.TIME_SLOW, 0.75, Stats.DURATION, 16, Stats.COOLDOWN, 30, Stats.ALL_DAMAGE, 15)))));

    /** 3. 千替“实境扭曲”·斯安威斯坦 —— 传说/神话 */
    public static final CyberwareDefinition SANDEVISTAN_QIANTAI = register(new CyberwareDefinition(
            "sandevistan_qiantai", "千替“实境扭曲”·斯安威斯坦", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/sandevistan_qiantai",
            "千替的高端型号：压缩得更狠，代价是更短的窗口。",
            List.of(
                    variant(CyberwareRarity.LEGENDARY, 30, stats(
                            Stats.TIME_SLOW, 0.75, Stats.DURATION, 12, Stats.COOLDOWN, 15,
                            Stats.CRIT_CHANCE, 15, Stats.CRIT_DAMAGE, 15)),
                    variant(CyberwareRarity.MYTHIC, 35, stats(
                            Stats.TIME_SLOW, 0.90, Stats.DURATION, 8, Stats.COOLDOWN, 30,
                            Stats.CRIT_CHANCE, 10, Stats.CRIT_DAMAGE, 50)))));

    /** 4. 军用科技“游隼”·斯安威斯坦 —— 神话 */
    public static final CyberwareDefinition SANDEVISTAN_MILITECH_FALCON = register(new CyberwareDefinition(
            "sandevistan_militech_falcon", "军用科技“游隼”·斯安威斯坦", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/sandevistan_militech_falcon",
            "军用科技“游隼”，为长窗口压制而生。",
            List.of(
                    variant(CyberwareRarity.MYTHIC, 39, stats(
                            Stats.TIME_SLOW, 0.70, Stats.DURATION, 20, Stats.COOLDOWN, 30,
                            Stats.ALL_DAMAGE, 15, Stats.CRIT_CHANCE, 20, Stats.CRIT_DAMAGE, 35)))));

    /** 5. 军用科技“远地点”·斯安威斯坦 —— 史诗/传说/神话 */
    public static final CyberwareDefinition SANDEVISTAN_MILITECH_APOGEE = register(new CyberwareDefinition(
            "sandevistan_militech_apogee", "军用科技“远地点”·斯安威斯坦", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/sandevistan_militech_apogee",
            "军用科技“远地点”：时间被压到几乎静止，代价是极短的窗口与极高的负荷。",
            List.of(
                    variant(CyberwareRarity.EPIC, 44, stats(
                            Stats.TIME_SLOW, 0.85, Stats.DURATION, 6, Stats.COOLDOWN, 30,
                            Stats.HEADSHOT_DAMAGE, 15, Stats.CRIT_CHANCE, 17, Stats.CRIT_DAMAGE, 20)),
                    variant(CyberwareRarity.LEGENDARY, 44, stats(
                            Stats.TIME_SLOW, 0.85, Stats.DURATION, 6, Stats.COOLDOWN, 25,
                            Stats.HEADSHOT_DAMAGE, 15, Stats.CRIT_CHANCE, 17, Stats.CRIT_DAMAGE, 20)),
                    variant(CyberwareRarity.MYTHIC, 44, stats(
                            Stats.TIME_SLOW, 0.85, Stats.DURATION, 6, Stats.COOLDOWN, 25,
                            Stats.HEADSHOT_DAMAGE, 15, Stats.CRIT_CHANCE, 17, Stats.CRIT_DAMAGE, 20)))));

    // ═══════════════════ 操作系统 · 网络接入仓 ═══════════════════
    // 黑客义体：提供 RAM / 缓冲 / 栏位，不提供时间减缓。

    /** 6. 军用科技平行线 —— 普通 */
    public static final CyberwareDefinition CYBERDECK_MILITECH_PARALLEL = register(new CyberwareDefinition(
            "cyberdeck_militech_parallel", "军用科技平行线", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/cyberdeck_militech_parallel",
            "军用科技的基础网络接入仓：够用，仅此而已。",
            List.of(variant(CyberwareRarity.COMMON, 6, stats(
                    Stats.RAM, 2, Stats.BUFFER, 4, Stats.SLOTS, 2)))));

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

    /** 10c. 生物技术3型 —— 史诗 */
    public static final CyberwareDefinition CYBERDECK_BIOTECH_3 = register(new CyberwareDefinition(
            "cyberdeck_biotech_3", "生物技术3型", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/cyberdeck_biotech_3",
            "生物技术3型：更高的 RAM 与破解伤害。",
            List.of(variant(CyberwareRarity.EPIC, 20, stats(
                    Stats.RAM, 10, Stats.BUFFER, 7, Stats.SLOTS, 5,
                    Stats.RAM_REGEN, 9, Stats.HACK_DAMAGE, 20)))));

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

    /** 14. 军用科技篇章6型 —— 史诗/传说/神话（解锁「黑墙网关」） */
    public static final CyberwareDefinition CYBERDECK_MILITECH_CANTO_6 = register(new CyberwareDefinition(
            "cyberdeck_militech_canto_6", "军用科技篇章6型", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/cyberdeck_militech_canto_6",
            "军用科技篇章6型：可解锁「黑墙网关」。",
            List.of(
                    variant(CyberwareRarity.EPIC, 28, stats(
                            Stats.RAM, 8, Stats.SLOTS, 5, Stats.TIER, 3)),
                    variant(CyberwareRarity.LEGENDARY, 33, stats(
                            Stats.RAM, 9, Stats.SLOTS, 5, Stats.TIER, 3)),
                    variant(CyberwareRarity.MYTHIC, 38, stats(
                            Stats.RAM, 10, Stats.SLOTS, 5, Stats.TIER, 4)))));

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

    /** 17. 摩尔科技狂暴 —— 普通/精良/史诗 */
    public static final CyberwareDefinition BERSERK_MOORE = register(new CyberwareDefinition(
            "berserk_moore", "摩尔科技狂暴", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/berserk_moore",
            "摩尔科技狂暴：牺牲精准，换取爆发与耐打。",
            List.of(
                    variant(CyberwareRarity.COMMON, 14, stats(
                            Stats.RECOIL, -10, Stats.MELEE_DAMAGE, 10, Stats.ARMOR, 5)),
                    variant(CyberwareRarity.UNCOMMON, 20, stats(
                            Stats.RECOIL, -10, Stats.MELEE_DAMAGE, 15, Stats.ARMOR, 5, Stats.MAX_HEALTH, 10)),
                    variant(CyberwareRarity.EPIC, 26, stats(
                            Stats.RECOIL, -10, Stats.MELEE_DAMAGE, 15, Stats.ARMOR, 10,
                            Stats.MAX_HEALTH, 20, Stats.KILL_HEAL, 2)))));

    /** 18. 生物动力狂暴 —— 普通/精良/史诗/传说（文档只给了容量） */
    public static final CyberwareDefinition BERSERK_BIODYNE = register(new CyberwareDefinition(
            "berserk_biodyne", "生物动力狂暴", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/berserk_biodyne",
            "生物动力狂暴。TODO(主人填写): 文档未给效果数值，当前仅录入容量。",
            List.of(
                    variant(CyberwareRarity.COMMON, 14, stats(Stats.RECOIL, -10, Stats.MELEE_DAMAGE, 10)),
                    variant(CyberwareRarity.UNCOMMON, 20, stats(Stats.RECOIL, -10, Stats.MELEE_DAMAGE, 15)),
                    variant(CyberwareRarity.EPIC, 26, stats(Stats.RECOIL, -10, Stats.MELEE_DAMAGE, 20)),
                    variant(CyberwareRarity.LEGENDARY, 32, stats(Stats.RECOIL, -10, Stats.MELEE_DAMAGE, 25)))));

    /** 19. 军用科技狂暴 —— 史诗/传说/神话（文档只给了容量） */
    public static final CyberwareDefinition BERSERK_MILITECH = register(new CyberwareDefinition(
            "berserk_militech", "军用科技狂暴", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/berserk_militech",
            "军用科技狂暴。TODO(主人填写): 文档未给效果数值，当前仅录入容量。",
            List.of(
                    variant(CyberwareRarity.EPIC, 26, stats(Stats.MELEE_DAMAGE, 20, Stats.ARMOR, 10)),
                    variant(CyberwareRarity.LEGENDARY, 32, stats(Stats.MELEE_DAMAGE, 25, Stats.ARMOR, 15)),
                    variant(CyberwareRarity.MYTHIC, 38, stats(Stats.MELEE_DAMAGE, 30, Stats.ARMOR, 20)))));

    /** 20. 泽塔科技狂暴 —— 史诗/传说/神话（文档只给了容量） */
    public static final CyberwareDefinition BERSERK_ZETATECH = register(new CyberwareDefinition(
            "berserk_zetatech", "泽塔科技狂暴", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/berserk_zetatech",
            "泽塔科技狂暴。TODO(主人填写): 文档未给效果数值，当前仅录入容量。",
            List.of(
                    variant(CyberwareRarity.EPIC, 26, stats(Stats.MELEE_DAMAGE, 20, Stats.ARMOR, 10)),
                    variant(CyberwareRarity.LEGENDARY, 32, stats(Stats.MELEE_DAMAGE, 25, Stats.ARMOR, 15)),
                    variant(CyberwareRarity.MYTHIC, 38, stats(Stats.MELEE_DAMAGE, 30, Stats.ARMOR, 20)))));

    /** 装殖缩减 · 操作系统 */
    public static final CyberwareDefinition CAPACITY_BOOSTER = register(new CyberwareDefinition(
            "capacity_booster", "装殖缩减", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/capacity_booster",
            "装殖缩减。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 军用科技篇章6型 · 操作系统 */
    public static final CyberwareDefinition HAUNTED_CYBERDECK = register(new CyberwareDefinition(
            "haunted_cyberdeck", "军用科技篇章6型", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/haunted_cyberdeck",
            "军用科技篇章6型。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 网络监察网驱1型 · 操作系统 */
    public static final CyberwareDefinition NETWATCH_NETDRIVER_MK = register(new CyberwareDefinition(
            "netwatch_netdriver_mk", "网络监察网驱1型", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/netwatch_netdriver_mk",
            "网络监察网驱1型。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 军用科技斯安威斯坦”远地点“ · 操作系统 */
    public static final CyberwareDefinition SANDEVISTAN_APOGEE = register(new CyberwareDefinition(
            "sandevistan_apogee", "军用科技斯安威斯坦”远地点“", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/sandevistan_apogee",
            "军用科技斯安威斯坦”远地点“。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 军用科技斯安威斯坦”游隼“ · 操作系统 */
    public static final CyberwareDefinition SANDEVISTAN_C4 = register(new CyberwareDefinition(
            "sandevistan_c4", "军用科技斯安威斯坦”游隼“", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/sandevistan_c4",
            "军用科技斯安威斯坦”游隼“。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 军用科技狂暴 · 操作系统 */
    public static final CyberwareDefinition BERSERK_C4 = register(new CyberwareDefinition(
            "berserk_c4", "军用科技狂暴", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/berserk_c4",
            "军用科技狂暴。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 歧路司义眼石化鸡蛇 · 面部 */
    public static final CyberwareDefinition ICONIC_ADVANCED_KIROSHI_OPTICS_BARE = register(new CyberwareDefinition(
            "iconic_advanced_kiroshi_optics_bare", "歧路司义眼石化鸡蛇", CyberwareSlot.FACE, true,
            "cyberware:item/iconic_advanced_kiroshi_optics_bare",
            "歧路司义眼石化鸡蛇。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.MYTHIC, 8, stats()))));

    /** 行为特征脸板 · 面部 */
    public static final CyberwareDefinition MASK_CW_PLUS_PLUS = register(new CyberwareDefinition(
            "mask_cw_plus_plus", "行为特征脸板", CyberwareSlot.FACE, true,
            "cyberware:item/mask_cw_plus_plus",
            "行为特征脸板。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 坚矛利盾 · 手掌 */
    public static final CyberwareDefinition ICONIC_GUN_STABILIZER = register(new CyberwareDefinition(
            "iconic_gun_stabilizer", "坚矛利盾", CyberwareSlot.ARMS, true,
            "cyberware:item/iconic_gun_stabilizer",
            "坚矛利盾。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.MYTHIC, 8, stats()))));

    /** 电磁回收 · 循环系统 */
    public static final CyberwareDefinition ICONIC_DISCHARGE_CONNECTOR = register(new CyberwareDefinition(
            "iconic_discharge_connector", "电磁回收", CyberwareSlot.CIRCULATORY, true,
            "cyberware:item/iconic_discharge_connector",
            "电磁回收。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.MYTHIC, 8, stats()))));

    /** 等距稳定 · 循环系统 */
    public static final CyberwareDefinition ICONIC_SHOCK_ABSORBER = register(new CyberwareDefinition(
            "iconic_shock_absorber", "等距稳定", CyberwareSlot.CIRCULATORY, true,
            "cyberware:item/iconic_shock_absorber",
            "等距稳定。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.MYTHIC, 8, stats()))));

    /** 团灭韧带 · 腿部 */
    public static final CyberwareDefinition ICONIC_JENKINS_TENDONS = register(new CyberwareDefinition(
            "iconic_jenkins_tendons", "团灭韧带", CyberwareSlot.LEGS, true,
            "cyberware:item/iconic_jenkins_tendons",
            "团灭韧带。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.MYTHIC, 8, stats()))));

    /** 几质丁壳 · 表皮系统 */
    public static final CyberwareDefinition ICONIC_CHITON = register(new CyberwareDefinition(
            "iconic_chiton", "几质丁壳", CyberwareSlot.INTEGUMENTARY, true,
            "cyberware:item/iconic_chiton",
            "几质丁壳。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.MYTHIC, 8, stats()))));

    /** 外周逆反 · 表皮系统 */
    public static final CyberwareDefinition ICONIC_PROXIMITY_REDUCER = register(new CyberwareDefinition(
            "iconic_proximity_reducer", "外周逆反", CyberwareSlot.INTEGUMENTARY, true,
            "cyberware:item/iconic_proximity_reducer",
            "外周逆反。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.MYTHIC, 8, stats()))));

    /** 长焦可视界面 · 神经系统 */
    public static final CyberwareDefinition ICONIC_VISUAL_CORTEX_SUPPORT = register(new CyberwareDefinition(
            "iconic_visual_cortex_support", "长焦可视界面", CyberwareSlot.NERVOUS_SYSTEM, true,
            "cyberware:item/iconic_visual_cortex_support",
            "长焦可视界面。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.MYTHIC, 8, stats()))));

    /** 肾上腺导引 · 神经系统 */
    public static final CyberwareDefinition ICONIC_DETECTOR_RUSH = register(new CyberwareDefinition(
            "iconic_detector_rush", "肾上腺导引", CyberwareSlot.NERVOUS_SYSTEM, true,
            "cyberware:item/iconic_detector_rush",
            "肾上腺导引。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.MYTHIC, 8, stats()))));

    /** 乖离排异 · 神经系统 */
    public static final CyberwareDefinition ICONIC_REFLEX_RECORDER = register(new CyberwareDefinition(
            "iconic_reflex_recorder", "乖离排异", CyberwareSlot.NERVOUS_SYSTEM, true,
            "cyberware:item/iconic_reflex_recorder",
            "乖离排异。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.MYTHIC, 8, stats()))));

    /** 一拳开 · 骨骼 */
    public static final CyberwareDefinition ICONIC_T1000 = register(new CyberwareDefinition(
            "iconic_t1000", "一拳开", CyberwareSlot.SKELETON, true,
            "cyberware:item/iconic_t1000",
            "一拳开。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.MYTHIC, 8, stats()))));

    /** 量子调谐 · 额皮质 */
    public static final CyberwareDefinition TIME_BANK = register(new CyberwareDefinition(
            "time_bank", "量子调谐", CyberwareSlot.FRONTAL_CORTEX, true,
            "cyberware:item/time_bank",
            "量子调谐。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 皮下变色 · 额皮质 */
    public static final CyberwareDefinition ICONIC_SUBDERMAL_CO_PROCESSOR = register(new CyberwareDefinition(
            "iconic_subdermal_co_processor", "皮下变色", CyberwareSlot.FRONTAL_CORTEX, true,
            "cyberware:item/iconic_subdermal_co_processor",
            "皮下变色。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.MYTHIC, 8, stats()))));

    /** COX-2赛博生体优化 · 额皮质 */
    public static final CyberwareDefinition ICONIC_BIO_CONDUCTORS = register(new CyberwareDefinition(
            "iconic_bio_conductors", "COX-2赛博生体优化", CyberwareSlot.FRONTAL_CORTEX, true,
            "cyberware:item/iconic_bio_conductors",
            "COX-2赛博生体优化。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.MYTHIC, 8, stats()))));

    /** RAM配平 · 额皮质 */
    public static final CyberwareDefinition ICONIC_CAMILLO_RAM_MANAGER = register(new CyberwareDefinition(
            "iconic_camillo_ram_manager", "RAM配平", CyberwareSlot.FRONTAL_CORTEX, true,
            "cyberware:item/iconic_camillo_ram_manager",
            "RAM配平。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.MYTHIC, 8, stats()))));

    /** 暴恐机动队螳螂刀 · 胳臂 */
    public static final CyberwareDefinition MAX_TAC_MANTIS_BLADES = register(new CyberwareDefinition(
            "max_tac_mantis_blades", "暴恐机动队螳螂刀", CyberwareSlot.ARMS, true,
            "cyberware:item/max_tac_mantis_blades",
            "暴恐机动队螳螂刀。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 剧毒螳螂刀 · 胳臂 */
    public static final CyberwareDefinition MANTIS_BLADES_CHEMICAL = register(new CyberwareDefinition(
            "mantis_blades_chemical", "剧毒螳螂刀", CyberwareSlot.ARMS, true,
            "cyberware:item/mantis_blades_chemical",
            "剧毒螳螂刀。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 放电螳螂刀 · 胳臂 */
    public static final CyberwareDefinition MANTIS_BLADES_ELECTRIC = register(new CyberwareDefinition(
            "mantis_blades_electric", "放电螳螂刀", CyberwareSlot.ARMS, true,
            "cyberware:item/mantis_blades_electric",
            "放电螳螂刀。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 螳螂刀 · 胳臂 */
    public static final CyberwareDefinition MANTIS_BLADES = register(new CyberwareDefinition(
            "mantis_blades", "螳螂刀", CyberwareSlot.ARMS, true,
            "cyberware:item/mantis_blades",
            "螳螂刀。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 热能螳螂刀 · 胳臂 */
    public static final CyberwareDefinition MANTIS_BLADES_THERMAL = register(new CyberwareDefinition(
            "mantis_blades_thermal", "热能螳螂刀", CyberwareSlot.ARMS, true,
            "cyberware:item/mantis_blades_thermal",
            "热能螳螂刀。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 剧毒单分子线 · 胳臂 */
    public static final CyberwareDefinition NANO_WIRES_CHEMICAL = register(new CyberwareDefinition(
            "nano_wires_chemical", "剧毒单分子线", CyberwareSlot.ARMS, true,
            "cyberware:item/nano_wires_chemical",
            "剧毒单分子线。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 放电单分子线 · 胳臂 */
    public static final CyberwareDefinition NANO_WIRES_ELECTRIC = register(new CyberwareDefinition(
            "nano_wires_electric", "放电单分子线", CyberwareSlot.ARMS, true,
            "cyberware:item/nano_wires_electric",
            "放电单分子线。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 单分子线 · 胳臂 */
    public static final CyberwareDefinition NANO_WIRES = register(new CyberwareDefinition(
            "nano_wires", "单分子线", CyberwareSlot.ARMS, true,
            "cyberware:item/nano_wires",
            "单分子线。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 热能单分子线 · 胳臂 */
    public static final CyberwareDefinition NANO_WIRES_THERMAL = register(new CyberwareDefinition(
            "nano_wires_thermal", "热能单分子线", CyberwareSlot.ARMS, true,
            "cyberware:item/nano_wires_thermal",
            "热能单分子线。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 剧毒弹射发射系统 · 胳臂 */
    public static final CyberwareDefinition PROJECTILE_LAUNCHER_CHEMICAL = register(new CyberwareDefinition(
            "projectile_launcher_chemical", "剧毒弹射发射系统", CyberwareSlot.ARMS, true,
            "cyberware:item/projectile_launcher_chemical",
            "剧毒弹射发射系统。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 电子弹射发射系统 · 胳臂 */
    public static final CyberwareDefinition PROJECTILE_LAUNCHER_ELECTRIC = register(new CyberwareDefinition(
            "projectile_launcher_electric", "电子弹射发射系统", CyberwareSlot.ARMS, true,
            "cyberware:item/projectile_launcher_electric",
            "电子弹射发射系统。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 弹射发射系统 · 胳臂 */
    public static final CyberwareDefinition PROJECTILE_LAUNCHER = register(new CyberwareDefinition(
            "projectile_launcher", "弹射发射系统", CyberwareSlot.ARMS, true,
            "cyberware:item/projectile_launcher",
            "弹射发射系统。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 热能弹射发射系统 · 胳臂 */
    public static final CyberwareDefinition PROJECTILE_LAUNCHER_THERMAL = register(new CyberwareDefinition(
            "projectile_launcher_thermal", "热能弹射发射系统", CyberwareSlot.ARMS, true,
            "cyberware:item/projectile_launcher_thermal",
            "热能弹射发射系统。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 剧毒大猩猩手臂 · 胳臂 */
    public static final CyberwareDefinition STRONG_ARMS_CHEMICAL = register(new CyberwareDefinition(
            "strong_arms_chemical", "剧毒大猩猩手臂", CyberwareSlot.ARMS, true,
            "cyberware:item/strong_arms_chemical",
            "剧毒大猩猩手臂。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 放电大猩猩手臂 · 胳臂 */
    public static final CyberwareDefinition STRONG_ARMS_ELECTRIC = register(new CyberwareDefinition(
            "strong_arms_electric", "放电大猩猩手臂", CyberwareSlot.ARMS, true,
            "cyberware:item/strong_arms_electric",
            "放电大猩猩手臂。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 大猩猩手臂 · 胳臂 */
    public static final CyberwareDefinition STRONG_ARMS = register(new CyberwareDefinition(
            "strong_arms", "大猩猩手臂", CyberwareSlot.ARMS, true,
            "cyberware:item/strong_arms",
            "大猩猩手臂。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 热能大猩猩手臂 · 胳臂 */
    public static final CyberwareDefinition STRONG_ARMS_THERMAL = register(new CyberwareDefinition(
            "strong_arms_thermal", "热能大猩猩手臂", CyberwareSlot.ARMS, true,
            "cyberware:item/strong_arms_thermal",
            "热能大猩猩手臂。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 军用科技平行线 · 操作系统 */
    public static final CyberwareDefinition MILITECH_PARALINE_MKV = register(new CyberwareDefinition(
            "militech_paraline_mkv", "军用科技平行线", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/militech_paraline_mkv",
            "军用科技平行线。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 四相传电涟漪5型 · 操作系统 */
    public static final CyberwareDefinition TETRATRONIC_RIPPLER_MKV = register(new CyberwareDefinition(
            "tetratronic_rippler_mkv", "四相传电涟漪5型", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/tetratronic_rippler_mkv",
            "四相传电涟漪5型。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 生物技术3型 · 操作系统 */
    public static final CyberwareDefinition BIOTECH_SIGMA_MKIV = register(new CyberwareDefinition(
            "biotech_sigma_mkiv", "生物技术3型", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/biotech_sigma_mkiv",
            "生物技术3型。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 乌鸦微控3型 · 操作系统 */
    public static final CyberwareDefinition RAVEN_MICROCYBER_MKIII = register(new CyberwareDefinition(
            "raven_microcyber_mkiii", "乌鸦微控3型", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/raven_microcyber_mkiii",
            "乌鸦微控3型。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 荒板5型 · 操作系统 */
    public static final CyberwareDefinition ARASAKA_SHADOW_MKV = register(new CyberwareDefinition(
            "arasaka_shadow_mkv", "荒板5型", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/arasaka_shadow_mkv",
            "荒板5型。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 泽塔科技斯安威斯坦 · 操作系统 */
    public static final CyberwareDefinition SANDEVISTAN_C1 = register(new CyberwareDefinition(
            "sandevistan_c1", "泽塔科技斯安威斯坦", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/sandevistan_c1",
            "泽塔科技斯安威斯坦。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 迪娜拉斯安威斯坦 · 操作系统 */
    public static final CyberwareDefinition SANDEVISTAN_C2 = register(new CyberwareDefinition(
            "sandevistan_c2", "迪娜拉斯安威斯坦", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/sandevistan_c2",
            "迪娜拉斯安威斯坦。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 千替斯安威斯坦 · 操作系统 */
    public static final CyberwareDefinition SANDEVISTAN_C3 = register(new CyberwareDefinition(
            "sandevistan_c3", "千替斯安威斯坦", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/sandevistan_c3",
            "千替斯安威斯坦。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 摩尔科技狂暴 · 操作系统 */
    public static final CyberwareDefinition BERSERK_C1 = register(new CyberwareDefinition(
            "berserk_c1", "摩尔科技狂暴", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/berserk_c1",
            "摩尔科技狂暴。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 生物动力狂暴 · 操作系统 */
    public static final CyberwareDefinition BERSERK_C2 = register(new CyberwareDefinition(
            "berserk_c2", "生物动力狂暴", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/berserk_c2",
            "生物动力狂暴。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 泽塔科技狂暴 · 操作系统 */
    public static final CyberwareDefinition BERSERK_C3 = register(new CyberwareDefinition(
            "berserk_c3", "泽塔科技狂暴", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/berserk_c3",
            "泽塔科技狂暴。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 基础歧路司义眼 · 面部 */
    public static final CyberwareDefinition KIROSHI_OPTICS_BARE = register(new CyberwareDefinition(
            "kiroshi_optics_bare", "基础歧路司义眼", CyberwareSlot.FACE, true,
            "cyberware:item/kiroshi_optics_bare",
            "基础歧路司义眼。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 歧路司义眼1型 · 面部 */
    public static final CyberwareDefinition KIROSHI_OPTICS = register(new CyberwareDefinition(
            "kiroshi_optics", "歧路司义眼1型", CyberwareSlot.FACE, true,
            "cyberware:item/kiroshi_optics",
            "歧路司义眼1型。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 歧路司义眼神舆 · 面部 */
    public static final CyberwareDefinition KIROSHI_OPTICS_COMBINED = register(new CyberwareDefinition(
            "kiroshi_optics_combined", "歧路司义眼神舆", CyberwareSlot.FACE, true,
            "cyberware:item/kiroshi_optics_combined",
            "歧路司义眼神舆。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 歧路司义眼祸兆 · 面部 */
    public static final CyberwareDefinition KIROSHI_OPTICS_HUNTER = register(new CyberwareDefinition(
            "kiroshi_optics_hunter", "歧路司义眼祸兆", CyberwareSlot.FACE, true,
            "cyberware:item/kiroshi_optics_hunter",
            "歧路司义眼祸兆。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 歧路司义眼追猎 · 面部 */
    public static final CyberwareDefinition KIROSHI_OPTICS_PIERCING = register(new CyberwareDefinition(
            "kiroshi_optics_piercing", "歧路司义眼追猎", CyberwareSlot.FACE, true,
            "cyberware:item/kiroshi_optics_piercing",
            "歧路司义眼追猎。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 歧路司义眼警戒 · 面部 */
    public static final CyberwareDefinition KIROSHI_OPTICS_SENSOR = register(new CyberwareDefinition(
            "kiroshi_optics_sensor", "歧路司义眼警戒", CyberwareSlot.FACE, true,
            "cyberware:item/kiroshi_optics_sensor",
            "歧路司义眼警戒。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 歧路司义眼千里目 · 面部 */
    public static final CyberwareDefinition KIROSHI_OPTICS_WALLHACK = register(new CyberwareDefinition(
            "kiroshi_optics_wallhack", "歧路司义眼千里目", CyberwareSlot.FACE, true,
            "cyberware:item/kiroshi_optics_wallhack",
            "歧路司义眼千里目。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 智能连接 · 手掌 */
    public static final CyberwareDefinition SMART_LINK = register(new CyberwareDefinition(
            "smart_link", "智能连接", CyberwareSlot.ARMS, true,
            "cyberware:item/smart_link",
            "智能连接。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 弹道协同处理器 · 手掌 */
    public static final CyberwareDefinition POWER_GRIP = register(new CyberwareDefinition(
            "power_grip", "弹道协同处理器", CyberwareSlot.ARMS, true,
            "cyberware:item/power_grip",
            "弹道协同处理器。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 握柄固定套 · 手掌 */
    public static final CyberwareDefinition KNIFE_SHARPENER = register(new CyberwareDefinition(
            "knife_sharpener", "握柄固定套", CyberwareSlot.ARMS, true,
            "cyberware:item/knife_sharpener",
            "握柄固定套。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 微发电机 · 手掌 */
    public static final CyberwareDefinition MICRO_GENERATOR = register(new CyberwareDefinition(
            "micro_generator", "微发电机", CyberwareSlot.ARMS, true,
            "cyberware:item/micro_generator",
            "微发电机。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 省力减震 · 手掌 */
    public static final CyberwareDefinition JOINT_LOCK = register(new CyberwareDefinition(
            "joint_lock", "省力减震", CyberwareSlot.ARMS, true,
            "cyberware:item/joint_lock",
            "省力减震。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 纹身虎爪帮 · 手掌 */
    public static final CyberwareDefinition YAKUZA_TATTOO = register(new CyberwareDefinition(
            "yakuza_tattoo", "纹身虎爪帮", CyberwareSlot.ARMS, true,
            "cyberware:item/yakuza_tattoo",
            "纹身虎爪帮。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 纹身永远在一起 · 手掌 */
    public static final CyberwareDefinition SILVERHAND_TATTOO = register(new CyberwareDefinition(
            "silverhand_tattoo", "纹身永远在一起", CyberwareSlot.ARMS, true,
            "cyberware:item/silverhand_tattoo",
            "纹身永远在一起。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 纹身强尼特制 · 手掌 */
    public static final CyberwareDefinition CASIUS_TATTOO = register(new CyberwareDefinition(
            "casius_tattoo", "纹身强尼特制", CyberwareSlot.ARMS, true,
            "cyberware:item/casius_tattoo",
            "纹身强尼特制。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 生物监测 · 循环系统 */
    public static final CyberwareDefinition BIOMONITOR = register(new CyberwareDefinition(
            "biomonitor", "生物监测", CyberwareSlot.CIRCULATORY, true,
            "cyberware:item/biomonitor",
            "生物监测。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 副心脏 · 循环系统 */
    public static final CyberwareDefinition SECOND_HEART = register(new CyberwareDefinition(
            "second_heart", "副心脏", CyberwareSlot.CIRCULATORY, true,
            "cyberware:item/second_heart",
            "副心脏。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 击杀治疗 · 循环系统 */
    public static final CyberwareDefinition HEAL_ON_KILL = register(new CyberwareDefinition(
            "heal_on_kill", "击杀治疗", CyberwareSlot.CIRCULATORY, true,
            "cyberware:item/heal_on_kill",
            "击杀治疗。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 黑曼巴 · 循环系统 */
    public static final CyberwareDefinition VIRAL_VENOM = register(new CyberwareDefinition(
            "viral_venom", "黑曼巴", CyberwareSlot.CIRCULATORY, true,
            "cyberware:item/viral_venom",
            "黑曼巴。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 风险归避 · 循环系统 */
    public static final CyberwareDefinition CATCH_ME_IF_YOU_CAN = register(new CyberwareDefinition(
            "catch_me_if_you_can", "风险归避", CyberwareSlot.CIRCULATORY, true,
            "cyberware:item/catch_me_if_you_can",
            "风险归避。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 活血泵 · 循环系统 */
    public static final CyberwareDefinition BLOOD_PUMP = register(new CyberwareDefinition(
            "blood_pump", "活血泵", CyberwareSlot.CIRCULATORY, true,
            "cyberware:item/blood_pump",
            "活血泵。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 持握衬垫 · 循环系统 */
    public static final CyberwareDefinition SHOCK_ABSORBER = register(new CyberwareDefinition(
            "shock_absorber", "持握衬垫", CyberwareSlot.CIRCULATORY, true,
            "cyberware:item/shock_absorber",
            "持握衬垫。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 反馈电路 · 循环系统 */
    public static final CyberwareDefinition DISCHARGE_CONNECTOR = register(new CyberwareDefinition(
            "discharge_connector", "反馈电路", CyberwareSlot.CIRCULATORY, true,
            "cyberware:item/discharge_connector",
            "反馈电路。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 肾上腺素增强件 · 循环系统 */
    public static final CyberwareDefinition STAMINA_REGEN_BOOSTER = register(new CyberwareDefinition(
            "stamina_regen_booster", "肾上腺素增强件", CyberwareSlot.CIRCULATORY, true,
            "cyberware:item/stamina_regen_booster",
            "肾上腺素增强件。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 微型转子 · 循环系统 */
    public static final CyberwareDefinition CYBER_ROTORS = register(new CyberwareDefinition(
            "cyber_rotors", "微型转子", CyberwareSlot.CIRCULATORY, true,
            "cyberware:item/cyber_rotors",
            "微型转子。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 强化肌健 · 腿部 */
    public static final CyberwareDefinition BOOSTED_TENDONS = register(new CyberwareDefinition(
            "boosted_tendons", "强化肌健", CyberwareSlot.LEGS, true,
            "cyberware:item/boosted_tendons",
            "强化肌健。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 猞猁爪 · 腿部 */
    public static final CyberwareDefinition CAT_PAWS = register(new CyberwareDefinition(
            "cat_paws", "猞猁爪", CyberwareSlot.LEGS, true,
            "cyberware:item/cat_paws",
            "猞猁爪。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 火车王肌建 · 腿部 */
    public static final CyberwareDefinition JENKINS_TENDONS = register(new CyberwareDefinition(
            "jenkins_tendons", "火车王肌建", CyberwareSlot.LEGS, true,
            "cyberware:item/jenkins_tendons",
            "火车王肌建。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 踝部加固 · 腿部 */
    public static final CyberwareDefinition REINFORCED_MUSCLES = register(new CyberwareDefinition(
            "reinforced_muscles", "踝部加固", CyberwareSlot.LEGS, true,
            "cyberware:item/reinforced_muscles",
            "踝部加固。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 思想防线 · 表皮系统 */
    public static final CyberwareDefinition COGITO_FRAME = register(new CyberwareDefinition(
            "cogito_frame", "思想防线", CyberwareSlot.INTEGUMENTARY, true,
            "cyberware:item/cogito_frame",
            "思想防线。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 细胞适配 · 表皮系统 */
    public static final CyberwareDefinition ADAPTIVE_STEM_CELLS = register(new CyberwareDefinition(
            "adaptive_stem_cells", "细胞适配", CyberwareSlot.INTEGUMENTARY, true,
            "cyberware:item/adaptive_stem_cells",
            "细胞适配。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 皮下护甲 · 表皮系统 */
    public static final CyberwareDefinition BORING_PLATING = register(new CyberwareDefinition(
            "boring_plating", "皮下护甲", CyberwareSlot.INTEGUMENTARY, true,
            "cyberware:item/boring_plating",
            "皮下护甲。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 近接防盾 · 表皮系统 */
    public static final CyberwareDefinition PROXIMITY_REDUCER = register(new CyberwareDefinition(
            "proximity_reducer", "近接防盾", CyberwareSlot.INTEGUMENTARY, true,
            "cyberware:item/proximity_reducer",
            "近接防盾。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 疼痛编辑器 · 表皮系统 */
    public static final CyberwareDefinition PAIN_REDUCTOR = register(new CyberwareDefinition(
            "pain_reductor", "疼痛编辑器", CyberwareSlot.INTEGUMENTARY, true,
            "cyberware:item/pain_reductor",
            "疼痛编辑器。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 疼痛置换 · 表皮系统 */
    public static final CyberwareDefinition BLOOD_DEPLETER = register(new CyberwareDefinition(
            "blood_depleter", "疼痛置换", CyberwareSlot.INTEGUMENTARY, true,
            "cyberware:item/blood_depleter",
            "疼痛置换。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 拒敌防护 · 表皮系统 */
    public static final CyberwareDefinition CHARGE_SYSTEM = register(new CyberwareDefinition(
            "charge_system", "拒敌防护", CyberwareSlot.INTEGUMENTARY, true,
            "cyberware:item/charge_system",
            "拒敌防护。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 反制壳层 · 表皮系统 */
    public static final CyberwareDefinition SUDDEN_AID = register(new CyberwareDefinition(
            "sudden_aid", "反制壳层", CyberwareSlot.INTEGUMENTARY, true,
            "cyberware:item/sudden_aid",
            "反制壳层。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 震慑通电 · 表皮系统 */
    public static final CyberwareDefinition ELECTROSHOCK_MECHANISM = register(new CyberwareDefinition(
            "electroshock_mechanism", "震慑通电", CyberwareSlot.INTEGUMENTARY, true,
            "cyberware:item/electroshock_mechanism",
            "震慑通电。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 光学迷彩 · 表皮系统 */
    public static final CyberwareDefinition OPTICAL_CAMO = register(new CyberwareDefinition(
            "optical_camo", "光学迷彩", CyberwareSlot.INTEGUMENTARY, true,
            "cyberware:item/optical_camo",
            "光学迷彩。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 纳米镀层 · 表皮系统 */
    public static final CyberwareDefinition NANO_TECH_PLATES = register(new CyberwareDefinition(
            "nano_tech_plates", "纳米镀层", CyberwareSlot.INTEGUMENTARY, true,
            "cyberware:item/nano_tech_plates",
            "纳米镀层。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 全域覆盖 · 表皮系统 */
    public static final CyberwareDefinition WEIRD_TANKY_PLATING = register(new CyberwareDefinition(
            "weird_tanky_plating", "全域覆盖", CyberwareSlot.INTEGUMENTARY, true,
            "cyberware:item/weird_tanky_plating",
            "全域覆盖。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 克伦齐科夫回护 · 表皮系统 */
    public static final CyberwareDefinition PLATING_GLITCH = register(new CyberwareDefinition(
            "plating_glitch", "克伦齐科夫回护", CyberwareSlot.INTEGUMENTARY, true,
            "cyberware:item/plating_glitch",
            "克伦齐科夫回护。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 锥刺子 · 神经系统 */
    public static final CyberwareDefinition OIL_DISPENSER = register(new CyberwareDefinition(
            "oil_dispenser", "锥刺子", CyberwareSlot.NERVOUS_SYSTEM, true,
            "cyberware:item/oil_dispenser",
            "锥刺子。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 克伦齐科夫 · 神经系统 */
    public static final CyberwareDefinition KERENZIKOV = register(new CyberwareDefinition(
            "kerenzikov", "克伦齐科夫", CyberwareSlot.NERVOUS_SYSTEM, true,
            "cyberware:item/kerenzikov",
            "克伦齐科夫。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 反应协调器 · 神经系统 */
    public static final CyberwareDefinition REFLEX_RECORDER = register(new CyberwareDefinition(
            "reflex_recorder", "反应协调器", CyberwareSlot.NERVOUS_SYSTEM, true,
            "cyberware:item/reflex_recorder",
            "反应协调器。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 突触加速器 · 神经系统 */
    public static final CyberwareDefinition SYNAPTIC_ACCELERATOR = register(new CyberwareDefinition(
            "synaptic_accelerator", "突触加速器", CyberwareSlot.NERVOUS_SYSTEM, true,
            "cyberware:item/synaptic_accelerator",
            "突触加速器。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 纳米纤维 · 神经系统 */
    public static final CyberwareDefinition NEO_FIBER = register(new CyberwareDefinition(
            "neo_fiber", "纳米纤维", CyberwareSlot.NERVOUS_SYSTEM, true,
            "cyberware:item/neo_fiber",
            "纳米纤维。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 视觉皮质支持 · 神经系统 */
    public static final CyberwareDefinition VISUAL_CORTEX_SUPPORT = register(new CyberwareDefinition(
            "visual_cortex_support", "视觉皮质支持", CyberwareSlot.NERVOUS_SYSTEM, true,
            "cyberware:item/visual_cortex_support",
            "视觉皮质支持。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 肾上腺素整流 · 神经系统 */
    public static final CyberwareDefinition DETECTOR_RUSH = register(new CyberwareDefinition(
            "detector_rush", "肾上腺素整流", CyberwareSlot.NERVOUS_SYSTEM, true,
            "cyberware:item/detector_rush",
            "肾上腺素整流。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 瞬时感知 · 神经系统 */
    public static final CyberwareDefinition TROUBLE_FINDER = register(new CyberwareDefinition(
            "trouble_finder", "瞬时感知", CyberwareSlot.NERVOUS_SYSTEM, true,
            "cyberware:item/trouble_finder",
            "瞬时感知。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 酪氨酸注射器 · 神经系统 */
    public static final CyberwareDefinition TYROSINE_INJECTOR = register(new CyberwareDefinition(
            "tyrosine_injector", "酪氨酸注射器", CyberwareSlot.NERVOUS_SYSTEM, true,
            "cyberware:item/tyrosine_injector",
            "酪氨酸注射器。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 死不鸟 · 骨骼 */
    public static final CyberwareDefinition NEURO_MATRIX = register(new CyberwareDefinition(
            "neuro_matrix", "死不鸟", CyberwareSlot.SKELETON, true,
            "cyberware:item/neuro_matrix",
            "死不鸟。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 弹性关节 · 骨骼 */
    public static final CyberwareDefinition AGILE_JOINTS = register(new CyberwareDefinition(
            "agile_joints", "弹性关节", CyberwareSlot.SKELETON, true,
            "cyberware:item/agile_joints",
            "弹性关节。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 致密骨骼 · 骨骼 */
    public static final CyberwareDefinition DENSE_MARROW = register(new CyberwareDefinition(
            "dense_marrow", "致密骨骼", CyberwareSlot.SKELETON, true,
            "cyberware:item/dense_marrow",
            "致密骨骼。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 再造骨骼 · 骨骼 */
    public static final CyberwareDefinition ENDOSKELETON = register(new CyberwareDefinition(
            "endoskeleton", "再造骨骼", CyberwareSlot.SKELETON, true,
            "cyberware:item/endoskeleton",
            "再造骨骼。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** RAM补偿 · 骨骼 */
    public static final CyberwareDefinition COMPILING_SKELETON = register(new CyberwareDefinition(
            "compiling_skeleton", "RAM补偿", CyberwareSlot.SKELETON, true,
            "cyberware:item/compiling_skeleton",
            "RAM补偿。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 清创凝合 · 骨骼 */
    public static final CyberwareDefinition NO_PAIN_NO_GAIN = register(new CyberwareDefinition(
            "no_pain_no_gain", "清创凝合", CyberwareSlot.SKELETON, true,
            "cyberware:item/no_pain_no_gain",
            "清创凝合。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 通用增强件 · 骨骼 */
    public static final CyberwareDefinition PAIN_DISTRIBUTOR = register(new CyberwareDefinition(
            "pain_distributor", "通用增强件", CyberwareSlot.SKELETON, true,
            "cyberware:item/pain_distributor",
            "通用增强件。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 动能骨架 · 骨骼 */
    public static final CyberwareDefinition BONE_MARROW_CELLS = register(new CyberwareDefinition(
            "bone_marrow_cells", "动能骨架", CyberwareSlot.SKELETON, true,
            "cyberware:item/bone_marrow_cells",
            "动能骨架。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 仿生关节 · 骨骼 */
    public static final CyberwareDefinition BIONIC_JOINTS = register(new CyberwareDefinition(
            "bionic_joints", "仿生关节", CyberwareSlot.SKELETON, true,
            "cyberware:item/bionic_joints",
            "仿生关节。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 圣甲虫 · 骨骼 */
    public static final CyberwareDefinition RAPID_MUSCLE_NURISH = register(new CyberwareDefinition(
            "rapid_muscle_nurish", "圣甲虫", CyberwareSlot.SKELETON, true,
            "cyberware:item/rapid_muscle_nurish",
            "圣甲虫。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 百拳开 · 骨骼 */
    public static final CyberwareDefinition T1000 = register(new CyberwareDefinition(
            "t1000", "百拳开", CyberwareSlot.SKELETON, true,
            "cyberware:item/t1000",
            "百拳开。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 钛金骨骼 · 骨骼 */
    public static final CyberwareDefinition TITANIUM_INFUSED_BONES = register(new CyberwareDefinition(
            "titanium_infused_bones", "钛金骨骼", CyberwareSlot.SKELETON, true,
            "cyberware:item/titanium_infused_bones",
            "钛金骨骼。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 生物导体 · 额皮质 */
    public static final CyberwareDefinition BIO_CONDUCTORS = register(new CyberwareDefinition(
            "bio_conductors", "生物导体", CyberwareSlot.FRONTAL_CORTEX, true,
            "cyberware:item/bio_conductors",
            "生物导体。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 卡米略RAM管理器 · 额皮质 */
    public static final CyberwareDefinition CAMILLO_RAM_MANAGER = register(new CyberwareDefinition(
            "camillo_ram_manager", "卡米略RAM管理器", CyberwareSlot.FRONTAL_CORTEX, true,
            "cyberware:item/camillo_ram_manager",
            "卡米略RAM管理器。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 外接盘 · 额皮质 */
    public static final CyberwareDefinition EX_DISK = register(new CyberwareDefinition(
            "ex_disk", "外接盘", CyberwareSlot.FRONTAL_CORTEX, true,
            "cyberware:item/ex_disk",
            "外接盘。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 克伦齐科夫增幅 · 额皮质 */
    public static final CyberwareDefinition KERENZIOV_BOOST_SYSTEM = register(new CyberwareDefinition(
            "kerenziov_boost_system", "克伦齐科夫增幅", CyberwareSlot.FRONTAL_CORTEX, true,
            "cyberware:item/kerenziov_boost_system",
            "克伦齐科夫增幅。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 机电核心 · 额皮质 */
    public static final CyberwareDefinition MECHATRONIC_CORE = register(new CyberwareDefinition(
            "mechatronic_core", "机电核心", CyberwareSlot.FRONTAL_CORTEX, true,
            "cyberware:item/mechatronic_core",
            "机电核心。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 内存加强 · 额皮质 */
    public static final CyberwareDefinition MEMORY_BOOST = register(new CyberwareDefinition(
            "memory_boost", "内存加强", CyberwareSlot.FRONTAL_CORTEX, true,
            "cyberware:item/memory_boost",
            "内存加强。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** RAM升级 · 额皮质 */
    public static final CyberwareDefinition RAM_UPGRADE = register(new CyberwareDefinition(
            "ram_upgrade", "RAM升级", CyberwareSlot.FRONTAL_CORTEX, true,
            "cyberware:item/ram_upgrade",
            "RAM升级。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 自我ICE · 额皮质 */
    public static final CyberwareDefinition SELF_ICE = register(new CyberwareDefinition(
            "self_ice", "自我ICE", CyberwareSlot.FRONTAL_CORTEX, true,
            "cyberware:item/self_ice",
            "自我ICE。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 蓄电缓冲 · 额皮质 */
    public static final CyberwareDefinition SMART_STORAGE = register(new CyberwareDefinition(
            "smart_storage", "蓄电缓冲", CyberwareSlot.FRONTAL_CORTEX, true,
            "cyberware:item/smart_storage",
            "蓄电缓冲。TODO(数值与描述待补)",
            List.of(variant(CyberwareRarity.LEGENDARY, 8, stats()))));

    /** 牛顿模块 · 额皮质 */
    public static final CyberwareDefinition SUBDERMAL_CO_PROCESSOR = register(new CyberwareDefinition(
            "subdermal_co_processor", "牛顿模块", CyberwareSlot.FRONTAL_CORTEX, true,
            "cyberware:item/subdermal_co_processor",
            "牛顿模块。TODO(数值与描述待补)",
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
