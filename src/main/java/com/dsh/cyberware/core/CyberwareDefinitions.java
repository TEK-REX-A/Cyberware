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

    /** 1. 泽塔科技斯安威斯坦 —— 普通/精良/史诗 */
    public static final CyberwareDefinition SANDEVISTAN_ZETATECH = register(new CyberwareDefinition(
            "sandevistan_zetatech", "泽塔科技斯安威斯坦", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/sandevistan_zetatech",
            "泽塔科技的入门级斯安威斯坦，改写神经系统对时间的感知。",
            List.of(
                    variant(CyberwareRarity.COMMON, 12, stats(
                            Stats.TIME_SLOW, 0.25, Stats.DURATION, 8, Stats.COOLDOWN, 30, Stats.CRIT_CHANCE, 10)),
                    variant(CyberwareRarity.UNCOMMON, 18, stats(
                            Stats.TIME_SLOW, 0.50, Stats.DURATION, 12, Stats.COOLDOWN, 30, Stats.CRIT_CHANCE, 15)),
                    variant(CyberwareRarity.EPIC, 22, stats(
                            Stats.TIME_SLOW, 0.50, Stats.DURATION, 16, Stats.COOLDOWN, 30, Stats.CRIT_CHANCE, 20)))));

    /** 2. 迪纳拉斯安威斯坦 —— 普通/精良/史诗/传说 */
    public static final CyberwareDefinition SANDEVISTAN_DYNALAR = register(new CyberwareDefinition(
            "sandevistan_dynalar", "迪纳拉斯安威斯坦", CyberwareSlot.OPERATING_SYSTEM, true,
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

    /** 3. 千替斯安威斯坦 —— 传说/神话 */
    public static final CyberwareDefinition SANDEVISTAN_QIANTAI = register(new CyberwareDefinition(
            "sandevistan_qiantai", "千替斯安威斯坦", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/sandevistan_qiantai",
            "千替的高端型号：压缩得更狠，代价是更短的窗口。",
            List.of(
                    variant(CyberwareRarity.LEGENDARY, 30, stats(
                            Stats.TIME_SLOW, 0.75, Stats.DURATION, 12, Stats.COOLDOWN, 15,
                            Stats.CRIT_CHANCE, 15, Stats.CRIT_DAMAGE, 15)),
                    variant(CyberwareRarity.MYTHIC, 35, stats(
                            Stats.TIME_SLOW, 0.90, Stats.DURATION, 8, Stats.COOLDOWN, 30,
                            Stats.CRIT_CHANCE, 10, Stats.CRIT_DAMAGE, 50)))));

    /** 4. 军用科技「游隼」斯安威斯坦 —— 神话 */
    public static final CyberwareDefinition SANDEVISTAN_MILITECH_FALCON = register(new CyberwareDefinition(
            "sandevistan_militech_falcon", "军用科技「游隼」斯安威斯坦", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/sandevistan_militech_falcon",
            "军用科技「游隼」，为长窗口压制而生。",
            List.of(
                    variant(CyberwareRarity.MYTHIC, 39, stats(
                            Stats.TIME_SLOW, 0.70, Stats.DURATION, 20, Stats.COOLDOWN, 30,
                            Stats.ALL_DAMAGE, 15, Stats.CRIT_CHANCE, 20, Stats.CRIT_DAMAGE, 35)))));

    /** 5. 军用科技「远地点」斯安威斯坦 —— 史诗/传说/神话 */
    public static final CyberwareDefinition SANDEVISTAN_MILITECH_APOGEE = register(new CyberwareDefinition(
            "sandevistan_militech_apogee", "军用科技「远地点」斯安威斯坦", CyberwareSlot.OPERATING_SYSTEM, true,
            "cyberware:item/sandevistan_militech_apogee",
            "军用科技「远地点」：时间被压到几乎静止，代价是极短的窗口与极高的负荷。",
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
