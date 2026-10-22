package com.dsh.cyberware.core;

/**
 * 义体安装槽位（对应植入体界面右侧的分类树）。
 *
 * <p>前 8 个是需求书的「核心分类」，最后 2 个来自扩充包。
 * {@code order} 决定分类树里的显示顺序，{@code maxCount} 是该槽位同时可装的义体数量上限
 * —— 需要在测试阶段由主人确认手感（见 TODO）。
 */
public enum CyberwareSlot {
    /** 操作系统槽上限 2（主人 0.4.0 拍板）：网络接入仓与斯安威斯坦/狂暴同属这一槽，只装 1 件就挤掉了。 */
    OPERATING_SYSTEM("操作系统", 1, 2),
    FRONTAL_CORTEX("前额皮质", 2, 1),
    FACE("面部", 3, 1),
    NERVOUS_SYSTEM("神经系统", 4, 1),
    SKELETON("骨骼", 5, 2),
    ARMS("手臂", 6, 2),
    LEGS("腿部", 7, 2),
    CIRCULATORY("循环系统", 8, 1),
    INTEGUMENTARY("表皮", 9, 1),
    SKIN("皮肤系统", 10, 1),
    /**
     * 手掌槽（2026-10-22 主人拍板新增）：智能连接一类原本挤在 ARMS 里，
     * 但界面上「手掌」是一个独立部位，没有自己的槽只能落到别处。
     *
     * ⚠ 必须放在枚举**最后**——{@link #byIndexSafe} 用 values()[index]，
     * 插在中间会让已有存档的槽位索引整体错位。
     */
    HAND("手掌", 11, 2);

    private final String displayName;
    private final int order;
    /** TODO(主人填写): 每个槽位允许同时安装的义体数量，当前给的是保守默认值 */
    private final int maxCount;

    CyberwareSlot(String displayName, int order, int maxCount) {
        this.displayName = displayName;
        this.order = order;
        this.maxCount = maxCount;
    }

    public String displayName() {
        return this.displayName;
    }

    public int order() {
        return this.order;
    }

    public int maxCount() {
        return this.maxCount;
    }

    /** 该槽位是否属于扩充包（UI 上可以打标记，或者按配置隐藏） */
    public boolean isExpansion() {
        return this == NERVOUS_SYSTEM || this == SKIN;
    }

    public static CyberwareSlot byIndexSafe(int index) {
        CyberwareSlot[] v = values();
        if (index < 0 || index >= v.length) {
            return OPERATING_SYSTEM;
        }
        return v[index];
    }
}
