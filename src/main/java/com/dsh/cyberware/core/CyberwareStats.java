package com.dsh.cyberware.core;

import com.dsh.cyberware.data.CyberwareData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.player.Player;

/**
 * 义体数值聚合器 —— 把「玩家已安装义体」的 {@link CyberwareDefinition.Variant#stats()}
 * 求和成**玩家级数值**（RAM 上限、RAM 恢复速率，以及超频的时长/冷却）。
 *
 * <p><b>为什么需要它</b>：0.3.12 之前仓库里只有 {@link CyberwareInstallation}（容量 + 顺序），
 * 义体的数值只被 UI 当文案显示，从来没有被「求和」过。RAM System 要的就是这个求和结果。
 *
 * <p><b>取值口径</b>：每件义体按**它自己那一档稀有度**取 {@code variant} 再取值，所以
 * 同一型号的普通档与神话档算出来的 RAM 不同（数值表本来就按稀有度给了不同数字）。
 * 旧存档里被删掉的型号、以及当前稀有度没有变体的型号一律跳过，不抛异常。
 */
public final class CyberwareStats {

    /** 一件已安装义体：定义 + 该件的数据（稀有度/等级）+ 命中稀有度的数值变体。 */
    public record Installed(CyberwareDefinition def, CyberwareData data,
                            CyberwareDefinition.Variant variant) {
    }

    /**
     * 超频时长（秒）与冷却（秒）表，下标 = {@link CyberwareRarity#ordinal()}。
     *
     * <p>出处：邮件IX「有持续时间和冷却时间（由网络接入仓的品质决定）」—— **秒数本身没有可查出处**。
     * 主人裁决 B1 定为「保持现表」（方案①）；方案②（整体 +15s）见 {@code HACK-VALUES.md} §4-B1，
     * 若改按方案②：时长 12/14/16/18/20/22、冷却 75/70/65/60/55/50。
     *
     * <p>{@code TODO(待主人裁决: B1 超频时长/冷却秒数无出处，现按方案①保留)}
     */
    private static final int[] OVERCLOCK_DURATION_SECONDS = {10, 12, 14, 16, 18, 20};
    private static final int[] OVERCLOCK_COOLDOWN_SECONDS = {60, 55, 50, 45, 40, 35};

    /**
     * 裸机（未装网络接入仓）的基础 RAM 恢复速率，单位：每分钟。
     *
     * <p>邮件IX 只说「默认上限 8、由接入仓决定恢复速率」，没说裸机速率；
     * 取 1.0 的理由：接入仓的 {@code RAM_REGEN} 是 3~9/分钟量级，1.0 只是「涓流」，
     * 保证「花光 8 点后不会永久卡死在 0/8」，同时把「想快就得装接入仓」留给玩家。
     *
     * <p>{@code TODO(主人裁决): 数值可调，0 也合法（= 与 0.4.0 同语义）。}
     */
    private static final double BASE_RAM_REGEN = 1.0D;

    private CyberwareStats() {
    }

    /** 已安装义体列表（按安装表的迭代顺序；跳过查不到的型号与变体）。 */
    public static List<Installed> installed(Player player) {
        List<Installed> list = new ArrayList<>();
        for (Map.Entry<String, CyberwareData> entry : CyberwareInstallation.of(player).installed().entrySet()) {
            CyberwareDefinition def = CyberwareDefinitions.byId(entry.getKey());
            if (def == null) {
                continue;   // 旧存档里被删掉的型号
            }
            CyberwareData data = entry.getValue().safe();
            CyberwareDefinition.Variant variant = def.variantFor(data.rarity());
            if (variant == null) {
                continue;   // 这个型号没有该稀有度（理论上装不上，防御）
            }
            list.add(new Installed(def, data, variant));
        }
        return list;
    }

    /** 把某个数值键在**所有**已安装义体上求和；缺这个键的按 0 计。 */
    public static double sum(Player player, String statKey) {
        double total = 0.0D;
        for (Installed it : installed(player)) {
            total += it.variant().stat(statKey, 0.0D);
        }
        return total;
    }

    /**
     * RAM 上限 = {@code max(8, Σ Stats.RAM)}。
     *
     * <p><b>那 8 点的来历</b>：邮件IX §二明文「玩家拥有 RAM 值（**默认上限 8**，由网络接入仓决定
     * 最大上限和恢复速率）」→ 裸机也有 8 点基线。取 {@code max} 而不是「8 + Σ」的理由：
     * ① 邮件口径就是「默认上限 8」；② 小接入仓（冬月电子1型 3 / 瑞草电子1型 4）不会把玩家削到比裸机更低；
     * ③ 大接入仓（technica_4 = 12）+ RAM 配平/升级才是真正涨上限的路径；④ 破解成本 4~8 在裸机 8 点下
     * 「一条一放」，装了接入仓才宽裕 —— 资源稀缺感保留。
     *
     * <p>数值表里这个键可以为负（例：{@code iconic_bio_conductors} RAM = -4），
     * 所以是**求和后夹到基线以上** —— 负数上限没有意义，会让「灌满」逻辑失去意义。
     */
    public static double maxRam(Player player) {
        return Math.max(8.0D, sum(player, CyberwareDefinition.Stats.RAM));
    }

    /**
     * 每分钟 RAM 恢复量 = {@link #BASE_RAM_REGEN} + Σ {@link CyberwareDefinition.Stats#RAM_REGEN}。
     *
     * <p><b>叠加，不是取最大</b>：接入仓与 RAM 升级/配平的恢复速率是相加关系
     * （邮件IX 的算法口径是「把所有来源求和」），所以这里用 {@code 基础值 + Σ}。
     *
     * <p>单位按 {@code Stats.RAM_REGEN} 的声明口径取「每分钟」：邮件给的例子
     * 「四相传电1型 +6/分钟」与定义表里 {@code cyberdeck_tetratronic_1} 的 {@code ram_regen = 6}
     * 对得上。⚠️ 但 {@code ram_upgrade} 的**描述文本**写的是「每秒增加 0.05~0.2」，
     * 与「每分钟」口径冲突 —— 这属于定义表的数据问题，见交付文档「已知问题」，本聚合器只做求和、
     * 不做单位换算（换算会静默改掉数值）。
     *
     * <p><b>旧存档不需要迁移代码</b>：0.4.0 期间已经建成 {@code current = 0} 的存档
     * （那时基线是 0）会被这条涓流按 {@link #BASE_RAM_REGEN}/分钟 慢慢救回来 ——
     * {@code RamSystem#settleRam} 每 20 刻给 current 加 {@code regenPerMinute / 60}，与存档新旧无关。
     *
     * <p>不夹负数：若将来有人给某个义体负的恢复速率（掉 RAM 的副作用），
     * 「当前值」由 {@link RamState} 夹在 0 以上，这里如实返回。
     */
    public static double regenPerMinute(Player player) {
        return BASE_RAM_REGEN + sum(player, CyberwareDefinition.Stats.RAM_REGEN);
    }

    /**
     * 「网络接入仓（cyberdeck）」判定：该件的数值里带 {@link CyberwareDefinition.Stats#BUFFER}。
     *
     * <p>为什么不用 id 前缀：官方 123 条里网络接入仓的命名并不统一
     * （{@code cyberdeck_*}、{@code haunted_cyberdeck}、{@code netwatch_netdriver_mk}、
     * {@code tetratronic_rippler_mkv}、{@code arasaka_shadow_mkv} …），前缀清单会漏。
     * 而**全表只有网络接入仓给 BUFFER**（{@code grep "Stats.BUFFER" CyberwareDefinitions.java}
     * 的命中全部落在网络接入仓定义上，见交付文档 §3 证据）。
     *
     * <p>⚠️ 若哪天有人给非网络接入仓加了 BUFFER，这条判定要改 —— 改这一个方法即可。
     */
    public static boolean isCyberdeck(CyberwareDefinition.Variant variant) {
        return variant != null && variant.has(CyberwareDefinition.Stats.BUFFER);
    }

    /**
     * 玩家装着的**最好的**网络接入仓：稀有度最高的那件；同稀有度取 RAM 更高的那件。
     *
     * @return 没有装网络接入仓时返回 {@code null}
     */
    public static Installed bestCyberdeck(Player player) {
        Installed best = null;
        for (Installed it : installed(player)) {
            if (!isCyberdeck(it.variant())) {
                continue;
            }
            if (best == null) {
                best = it;
                continue;
            }
            int byRarity = Integer.compare(it.data().rarity().ordinal(), best.data().rarity().ordinal());
            if (byRarity > 0) {
                best = it;
            } else if (byRarity == 0
                    && it.variant().stat(CyberwareDefinition.Stats.RAM, 0.0D)
                    > best.variant().stat(CyberwareDefinition.Stats.RAM, 0.0D)) {
                best = it;
            }
        }
        return best;
    }

    /** 超频持续 tick（20 tick = 1 秒）；没装网络接入仓返回 0。 */
    public static int overclockDurationTicks(Player player) {
        Installed deck = bestCyberdeck(player);
        return deck == null ? 0 : seconds(OVERCLOCK_DURATION_SECONDS, deck);
    }

    /** 超频冷却 tick；没装网络接入仓返回 0。 */
    public static int overclockCooldownTicks(Player player) {
        Installed deck = bestCyberdeck(player);
        return deck == null ? 0 : seconds(OVERCLOCK_COOLDOWN_SECONDS, deck);
    }

    /**
     * 占位表里**最大**的时长 / 冷却（tick）。
     *
     * <p>用途：{@link OverclockSystem} 用绝对 tick 记账，服务器重启后 tick 计数归零，
     * 旧的「未来时刻」会显得还差几小时 —— 超过这个上限的一律判定为「基准变了」，直接作废自愈。
     */
    public static int maxOverclockTicks() {
        int max = 0;
        for (int i = 0; i < OVERCLOCK_DURATION_SECONDS.length; i++) {
            max = Math.max(max, Math.max(OVERCLOCK_DURATION_SECONDS[i], OVERCLOCK_COOLDOWN_SECONDS[i]) * 20);
        }
        return max;
    }

    private static int seconds(int[] table, Installed deck) {
        int index = deck.data().rarity().ordinal();
        return table[Math.max(0, Math.min(table.length - 1, index))] * 20;
    }
}
