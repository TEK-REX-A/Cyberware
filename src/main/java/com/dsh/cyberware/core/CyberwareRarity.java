package com.dsh.cyberware.core;

/**
 * 义体稀有度 —— 六级，对齐《赛博朋克2077》原作：
 * 普通 / 精良 / 稀有 / 史诗 / 传说 / 神话。
 *
 * <p><b>容量不再走倍率</b>：需求书里每件义体（每个型号的每个稀有度）都给了精确容量，
 * 例如斯安威斯坦 12/18/22 —— 这些数值由 {@link CyberwareDefinition} 逐条存储。
 * 所以本枚举只负责「显示名 + 颜色 + 数值强度」。
 *
 * <p>{@code powerMultiplier} 用于把义体的基准数值按稀有度放大，供 UI 展示与效果计算。
 */
public enum CyberwareRarity {
    /** 普通 —— 灰 */
    COMMON("普通", 0xFF9E9E9E, 1.00),
    /** 精良 —— 绿 */
    UNCOMMON("精良", 0xFF4CAF50, 1.15),
    /** 稀有 —— 蓝 */
    RARE("稀有", 0xFF3B82F6, 1.30),
    /** 史诗 —— 紫 */
    EPIC("史诗", 0xFFA855F7, 1.60),
    /** 传说 —— 橙 */
    LEGENDARY("传说", 0xFFF97316, 2.00),
    /** 神话 —— 红（文档里原写作「不朽」，主人定为「神话」） */
    MYTHIC("神话", 0xFFE53935, 2.40);

    private final String displayName;
    private final int color;
    private final double powerMultiplier;

    CyberwareRarity(String displayName, int color, double powerMultiplier) {
        this.displayName = displayName;
        this.color = color;
        this.powerMultiplier = powerMultiplier;
    }

    public String displayName() {
        return this.displayName;
    }

    /** ARGB 颜色，供 UI 边框与文字使用 */
    public int color() {
        return this.color;
    }

    public double powerMultiplier() {
        return this.powerMultiplier;
    }

    /** 下一级；已是最高级返回 null（升级按钮据此禁用）。 */
    public CyberwareRarity next() {
        CyberwareRarity[] values = values();
        return this.ordinal() + 1 < values.length ? values[this.ordinal() + 1] : null;
    }

    public boolean isMax() {
        return this == MYTHIC;
    }

    /**
     * 防御性读取：索引越界或非法时一律回落到「普通」。
     * 旧存档 / 组件为空 / 数据被外部改坏时不能抛异常。
     */
    public static CyberwareRarity byIndexSafe(int index) {
        CyberwareRarity[] values = values();
        if (index < 0 || index >= values.length) {
            return COMMON;
        }
        return values[index];
    }

    public static CyberwareRarity defaultRarity() {
        return COMMON;
    }
}
