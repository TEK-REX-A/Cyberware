package com.dsh.cyberware.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 模组配置。
 *
 * <p>需求书【四】要求「升级有概率失败，成功率由配置文件控制」，这里给出全部可调项。
 * 标了 TODO 的默认值是占位，等主人测试手感后再定。
 */
public final class CyberwareConfig {

    public static final ModConfigSpec SPEC;

    // ---- 容量系统 ----
    public static final ModConfigSpec.IntValue DEFAULT_CAPACITY;
    public static final ModConfigSpec.BooleanValue ENABLE_CAPACITY_LIMIT;

    // ---- 升级系统 ----
    public static final ModConfigSpec.DoubleValue UPGRADE_SUCCESS_RATE;
    public static final ModConfigSpec.IntValue UPGRADE_COST_EMERALD;
    public static final ModConfigSpec.IntValue UPGRADE_COST_SCRAP;
    public static final ModConfigSpec.IntValue UPGRADE_COST_NETHERITE_SCRAP;

    // ---- 赛博精神病 ----
    public static final ModConfigSpec.BooleanValue CYBERPSYCHO_ENABLED;
    public static final ModConfigSpec.IntValue CYBERPSYCHO_LIGHT_THRESHOLD;
    public static final ModConfigSpec.DoubleValue CYBERPSYCHO_SPAWN_CHANCE;
    public static final ModConfigSpec.IntValue VILLAGE_KILL_THRESHOLD;

    // ---- 主动技能 ----
    public static final ModConfigSpec.BooleanValue TIME_DILATION_ALLOW_STACK;
    public static final ModConfigSpec.BooleanValue TIME_DILATION_KILL_EXTEND_ENABLED;
    public static final ModConfigSpec.IntValue TIME_DILATION_KILL_EXTEND_TICKS;
    public static final ModConfigSpec.IntValue TIME_DILATION_KILL_EXTEND_LIMIT;
    public static final ModConfigSpec.BooleanValue POST_EFFECT_ENABLED;
    public static final ModConfigSpec.BooleanValue TIME_DILATION_PROJECTILE_IMMUNE;

    // ---- 狂暴 ----
    public static final ModConfigSpec.BooleanValue BERSERK_INVULNERABLE;
    public static final ModConfigSpec.DoubleValue BERSERK_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue BERSERK_ATTACK_SPEED_BONUS;
    public static final ModConfigSpec.DoubleValue BERSERK_MOVEMENT_SPEED_BONUS;
    public static final ModConfigSpec.IntValue BERSERK_DURATION_SECONDS;
    public static final ModConfigSpec.IntValue BERSERK_KILL_EXTEND_TICKS;
    public static final ModConfigSpec.IntValue BERSERK_KILL_EXTEND_LIMIT;
    public static final ModConfigSpec.DoubleValue BERSERK_KILL_HEAL_HEARTS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("义体容量系统").push("capacity");
        ENABLE_CAPACITY_LIMIT = builder
                .comment("是否启用容量上限限制。关闭后可以无限安装（调试用）")
                .define("enableCapacityLimit", true);
        DEFAULT_CAPACITY = builder
                .comment("玩家默认植入体容量上限")
                .defineInRange("defaultCapacity", 100, 0, 10_000);
        builder.pop();

        builder.comment("升级系统").push("upgrade");
        UPGRADE_SUCCESS_RATE = builder
                .comment("升级成功率（0.0 ~ 1.0）。TODO(主人填写): 当前 0.6 为占位值")
                .defineInRange("successRate", 0.6D, 0.0D, 1.0D);
        UPGRADE_COST_EMERALD = builder
                .comment("升级消耗绿宝石数量")
                .defineInRange("costEmerald", 4, 0, 64);
        UPGRADE_COST_SCRAP = builder
                .comment("升级消耗废料数量")
                .defineInRange("costScrap", 8, 0, 64);
        UPGRADE_COST_NETHERITE_SCRAP = builder
                .comment("升级消耗下界合金碎片数量")
                .defineInRange("costNetheriteScrap", 1, 0, 64);
        builder.pop();

        builder.comment("赛博精神病（危险敌人）").push("cyberpsycho");
        CYBERPSYCHO_ENABLED = builder
                .comment("是否允许赛博精神病自然生成")
                .define("enabled", true);
        CYBERPSYCHO_LIGHT_THRESHOLD = builder
                .comment("生成所需的最高亮度（低于该值且在夜晚才有概率生成）")
                .defineInRange("lightThreshold", 7, 0, 15);
        CYBERPSYCHO_SPAWN_CHANCE = builder
                .comment("每次生成判定的概率。TODO(主人填写): 当前 0.02 为占位值")
                .defineInRange("spawnChance", 0.02D, 0.0D, 1.0D);
        VILLAGE_KILL_THRESHOLD = builder
                .comment("在村庄击杀多少村民后触发暴动事件")
                .defineInRange("villageKillThreshold", 3, 1, 100);
        builder.pop();

        builder.comment("主动技能").push("skills");
        TIME_DILATION_ALLOW_STACK = builder
                .comment("斯安威斯坦与克伦齐科夫同时触发时是否叠加减速（false = 取最强减速比例，防止实体 tick 彻底停滞）")
                .define("allowTimeDilationStack", false);
        TIME_DILATION_KILL_EXTEND_ENABLED = builder
                .comment("击杀是否延长斯安威斯坦的持续时间")
                .define("killExtendEnabled", true);
        TIME_DILATION_KILL_EXTEND_TICKS = builder
                .comment("每击杀一个敌人延长的 tick 数（20 tick = 1 秒）")
                .defineInRange("killExtendTicks", 40, 0, 20 * 60);
        TIME_DILATION_KILL_EXTEND_LIMIT = builder
                .comment("单次激活期间由击杀累计延长的上限（tick）")
                .defineInRange("killExtendLimit", 20 * 60, 0, 20 * 60 * 30);
        POST_EFFECT_ENABLED = builder
                .comment("斯安威斯坦激活时的屏幕后处理（边缘径向模糊 + 色散）。关掉可排除显卡/驱动相关的显示问题")
                .define("postEffectEnabled", true);
        TIME_DILATION_PROJECTILE_IMMUNE = builder
                .comment("斯安威斯坦激活期间免疫弹射物伤害（箭矢、雪球、火球等）")
                .define("projectileImmune", true);
        builder.pop();

        builder.comment("狂暴（Berserk）——把痛觉和疲劳一起关掉").push("berserk");
        BERSERK_INVULNERABLE = builder
                .comment("狂暴期间是否完全免疫伤害")
                .define("invulnerable", true);
        BERSERK_DAMAGE_MULTIPLIER = builder
                .comment("狂暴期间造成的伤害倍率")
                .defineInRange("damageMultiplier", 2.0D, 1.0D, 20.0D);
        BERSERK_ATTACK_SPEED_BONUS = builder
                .comment("狂暴期间攻击速度加成（0.5 = 挥砍频率 +50%）")
                .defineInRange("attackSpeedBonus", 0.5D, 0.0D, 5.0D);
        BERSERK_MOVEMENT_SPEED_BONUS = builder
                .comment("狂暴期间移动速度加成（0.25 = 奔跑速度 +25%）")
                .defineInRange("movementSpeedBonus", 0.25D, 0.0D, 5.0D);
        BERSERK_DURATION_SECONDS = builder
                .comment("默认持续时间（秒）。型号自带数值时会覆盖这项")
                .defineInRange("durationSeconds", 12, 1, 600);
        BERSERK_KILL_EXTEND_TICKS = builder
                .comment("每击杀一个敌人延长的 tick 数（20 tick = 1 秒）")
                .defineInRange("killExtendTicks", 30, 0, 1200);
        BERSERK_KILL_EXTEND_LIMIT = builder
                .comment("单次狂暴由击杀累计延长的上限（tick）")
                .defineInRange("killExtendLimit", 600, 0, 20 * 60 * 10);
        BERSERK_KILL_HEAL_HEARTS = builder
                .comment("每击杀一个敌人恢复的心数（1 颗心 = 2 点生命）")
                .defineInRange("killHealHearts", 2.0D, 0.0D, 20.0D);
        builder.pop();

        SPEC = builder.build();
    }

    private CyberwareConfig() {
    }
}
