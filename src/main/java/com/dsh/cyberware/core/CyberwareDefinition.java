package com.dsh.cyberware.core;

import java.util.List;
import java.util.Map;

/**
 * 一件义体「型号」的静态定义。
 *
 * <p>需求书的写法是「型号 + 多档稀有度」，每一档有独立的容量与数值，例如：
 * <pre>
 * 泽塔科技斯安威斯坦（普通/精良/史诗）
 *   时间减缓 25%/50%/50%，持续 8/12/16 秒，冷却 30 秒，暴击率 +10%/15%/20%
 *   占用容量 12/18/22
 * </pre>
 * 所以一个型号持有若干个 {@link Variant}，每个 Variant 对应一个稀有度。
 *
 * @param id          型号 id（注册名/数据组件键），例如 {@code sandevistan_zetatech}
 * @param displayName UI 显示名
 * @param slot        安装槽位
 * @param active      是否主动技能（主动技能需要按键绑定）
 * @param iconPath    UI 图标纹理路径
 * @param description UI 详情说明
 * @param variants    各稀有度变体，按稀有度从低到高
 */
public record CyberwareDefinition(
        String id,
        String displayName,
        CyberwareSlot slot,
        boolean active,
        String iconPath,
        String description,
        List<Variant> variants
) {
    /** 取出某个稀有度的变体；该型号不提供这个等级时返回 null。 */
    public Variant variantFor(CyberwareRarity rarity) {
        for (Variant v : this.variants) {
            if (v.rarity() == rarity) {
                return v;
            }
        }
        return null;
    }

    public boolean existsAt(CyberwareRarity rarity) {
        return variantFor(rarity) != null;
    }

    /** 该型号支持的最低稀有度（用于默认发放/展示）。 */
    public Variant baseVariant() {
        return this.variants.isEmpty() ? null : this.variants.get(0);
    }

    /**
     * 某一档稀有度的具体数值。
     *
     * @param rarity   稀有度
     * @param capacity 该档的容量占用（需求书给的是精确值）
     * @param stats    该档的数值表，键为 {@link Stats} 里的常量，便于 UI 逐行显示
     */
    public record Variant(CyberwareRarity rarity, int capacity, Map<String, Double> stats) {

        public double stat(String key, double fallback) {
            return this.stats.getOrDefault(key, fallback);
        }

        public boolean has(String key) {
            return this.stats.containsKey(key);
        }
    }

    /** 数值表的键名常量（UI 显示名见 lang 文件）。 */
    public static final class Stats {
        /** 时间减缓比例，0.25 = 25% */
        public static final String TIME_SLOW = "time_slow";
        /** 持续时间（秒） */
        public static final String DURATION = "duration";
        /** 冷却（秒） */
        public static final String COOLDOWN = "cooldown";
        /** 暴击率加成（%） */
        public static final String CRIT_CHANCE = "crit_chance";
        /** 暴击伤害加成（%） */
        public static final String CRIT_DAMAGE = "crit_damage";
        /** 全伤害加成（%） */
        public static final String ALL_DAMAGE = "all_damage";
        /** 爆头伤害加成（%） */
        public static final String HEADSHOT_DAMAGE = "headshot_damage";
        /** 近战伤害加成（%） */
        public static final String MELEE_DAMAGE = "melee_damage";
        /** 护甲加成（%） */
        public static final String ARMOR = "armor";
        /** 最大生命加成 */
        public static final String MAX_HEALTH = "max_health";
        /** 后坐力/摇摆降低（%） */
        public static final String RECOIL = "recoil";
        /** RAM 上限 */
        public static final String RAM = "ram";
        /** 缓冲 */
        public static final String BUFFER = "buffer";
        /** 栏位 */
        public static final String SLOTS = "slots";
        /** RAM 恢复速度（每分钟） */
        public static final String RAM_REGEN = "ram_regen";
        /** 快速破解伤害加成（%） */
        public static final String HACK_DAMAGE = "hack_damage";
        /** 快速破解冷却缩减（%） */
        public static final String HACK_COOLDOWN = "hack_cooldown";
        /** 战斗破解持续时间加成（%） */
        public static final String COMBAT_HACK_DURATION = "combat_hack_duration";
        /** 上传时间变化（%，负数为缩短） */
        public static final String UPLOAD_TIME = "upload_time";
        /** 终极破解占用变化 */
        public static final String ULTIMATE_COST = "ultimate_cost";
        /** 隐蔽破解占用变化 */
        public static final String STEALTH_COST = "stealth_cost";
        /** 敌方破解时间加成（%） */
        public static final String ENEMY_HACK_TIME = "enemy_hack_time";
        /** 破解距离加成（%） */
        public static final String HACK_DISTANCE = "hack_distance";
        /** 散布距离加成（%） */
        public static final String SPREAD_DISTANCE = "spread_distance";
        /** 位阶 */
        public static final String TIER = "tier";
        /** 击败敌人恢复生命（%） */
        public static final String KILL_HEAL = "kill_heal";

        private Stats() {
        }
    }
}
