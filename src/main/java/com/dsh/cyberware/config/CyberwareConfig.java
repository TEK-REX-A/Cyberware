package com.dsh.cyberware.config;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

/**
 * 模组配置（零依赖版，2026-10-22 由 ModConfigSpec 迁移而来）。
 *
 * <p>原实现用 NeoForge 的 {@code ModConfigSpec}，迁移到 Fabric 后那个包不存在，
 * 构建因此报 "package ModConfig does not exist"。现在改成
 * {@link java.util.Properties}：
 *
 * <ul>
 *   <li>首次启动在 {@code config/cyberware.properties} 生成<b>带分节注释的全量默认值</b></li>
 *   <li>之后每次启动读取；文件里缺的键会补写回去（已存在的用户值不覆盖）</li>
 *   <li>字段名与旧版**一一对应**，调用方只需把 {@code .get()} 去掉</li>
 * </ul>
 *
 * <p>⚠ 字段是 {@code static}（非 final）：{@link #reload()} 会整体重读，
 * 旧代码的 {@code .get()} 是"每次取当前值"，这里用静态字段保持同样的语义。
 *
 * <p>文件按 UTF-8 读写（注释是中文），解析用 {@link Properties#load(Reader)}。
 */
public final class CyberwareConfig {

    private static final String FILE_NAME = "cyberware.properties";

    // ---- 容量系统 ----
    public static boolean ENABLE_CAPACITY_LIMIT = true;
    public static int DEFAULT_CAPACITY = 100;

    // ---- 升级系统 ----
    public static double UPGRADE_SUCCESS_RATE = 0.6D;
    public static int UPGRADE_COST_EMERALD = 4;
    public static int UPGRADE_COST_SCRAP = 8;
    public static int UPGRADE_COST_NETHERITE_SCRAP = 1;

    // ---- 赛博精神病 ----
    public static boolean CYBERPSYCHO_ENABLED = true;
    public static int CYBERPSYCHO_LIGHT_THRESHOLD = 7;
    public static double CYBERPSYCHO_SPAWN_CHANCE = 0.02D;
    public static int VILLAGE_KILL_THRESHOLD = 3;

    // ---- 护盾（2026-10-22 主人 + Friend 定） ----
    /** 是否启用护盾系统。关掉 = 伤害完全按原版走（排查用）。 */
    public static boolean SHIELD_ENABLED = true;
    /**
     * 护盾的<b>基础</b>恢复速度（点/秒）。
     *
     * <p>实际速度 = 这个值 + Σ 已安装义体的 {@code shield_regen}。
     * 主人定的基准是 3/s；**故意做成配置项**，改数值不用改代码。
     */
    public static double SHIELD_BASE_REGEN_PER_SECOND = 3.0D;
    /** 脱战多少刻后开始回盾（60 刻 = 3 秒）。 */
    public static int SHIELD_COMBAT_DELAY_TICKS = 60;
    /**
     * 吸收伤害时往动作栏打一行「[护盾] -12 剩余 28/40」（真机验证用）。
     *
     * <p>右下角护盾条做好之后应关掉这项。
     */
    public static boolean SHIELD_DEBUG_MESSAGES = true;

    // ---- 主动技能 ----
    public static boolean TIME_DILATION_ALLOW_STACK = false;
    public static boolean TIME_DILATION_KILL_EXTEND_ENABLED = true;
    public static int TIME_DILATION_KILL_EXTEND_TICKS = 40;
    public static int TIME_DILATION_KILL_EXTEND_LIMIT = 20 * 60;
    public static boolean POST_EFFECT_ENABLED = true;
    public static boolean TIME_DILATION_PROJECTILE_IMMUNE = true;

    // ---- 狂暴 ----
    public static boolean BERSERK_INVULNERABLE = true;
    public static double BERSERK_DAMAGE_MULTIPLIER = 2.0D;
    public static double BERSERK_ATTACK_SPEED_BONUS = 0.5D;
    public static double BERSERK_MOVEMENT_SPEED_BONUS = 0.25D;
    public static int BERSERK_DURATION_SECONDS = 12;
    public static int BERSERK_KILL_EXTEND_TICKS = 30;
    public static int BERSERK_KILL_EXTEND_LIMIT = 600;
    public static double BERSERK_KILL_HEAL_HEARTS = 2.0D;

    /** 全部已知键，用于判断配置文件是否缺项（缺了就补写）。 */
    private static final String[] ALL_KEYS = {
            "capacity.enableCapacityLimit",
            "capacity.defaultCapacity",
            "upgrade.successRate",
            "upgrade.costEmerald",
            "upgrade.costScrap",
            "upgrade.costNetheriteScrap",
            "cyberpsycho.enabled",
            "cyberpsycho.lightThreshold",
            "cyberpsycho.spawnChance",
            "cyberpsycho.villageKillThreshold",
            "shield.enabled",
            "shield.baseRegenPerSecond",
            "shield.combatDelayTicks",
            "shield.debugMessages",
            "skills.allowTimeDilationStack",
            "skills.killExtendEnabled",
            "skills.killExtendTicks",
            "skills.killExtendLimit",
            "skills.postEffectEnabled",
            "skills.projectileImmune",
            "berserk.invulnerable",
            "berserk.damageMultiplier",
            "berserk.attackSpeedBonus",
            "berserk.movementSpeedBonus",
            "berserk.durationSeconds",
            "berserk.killExtendTicks",
            "berserk.killExtendLimit",
            "berserk.killHealHearts"
    };

    private CyberwareConfig() {
    }

    /** mod 初始化时调用一次：读配置，缺文件就写默认值。任何异常都不让 mod 挂掉。 */
    public static void load() {
        try {
            Path file = path();
            if (!Files.exists(file)) {
                save(file);
                return;
            }
            reload();
        } catch (Throwable t) {
            // 配置坏了就用默认值跑，不要拦启动
            com.dsh.cyberware.Cyberware.LOGGER.warn("[cyberware] 读取配置失败，使用默认值", t);
        }
    }

    /** 重新从磁盘读一遍（改完配置可在游戏内重载）。缺项会补写回文件。 */
    public static void reload() throws Exception {
        Path file = path();
        if (!Files.exists(file)) {
            save(file);
            return;
        }
        Properties p = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            p.load(reader);
        }

        ENABLE_CAPACITY_LIMIT = bool(p, "capacity.enableCapacityLimit", ENABLE_CAPACITY_LIMIT);
        DEFAULT_CAPACITY = integer(p, "capacity.defaultCapacity", DEFAULT_CAPACITY);

        UPGRADE_SUCCESS_RATE = dbl(p, "upgrade.successRate", UPGRADE_SUCCESS_RATE);
        UPGRADE_COST_EMERALD = integer(p, "upgrade.costEmerald", UPGRADE_COST_EMERALD);
        UPGRADE_COST_SCRAP = integer(p, "upgrade.costScrap", UPGRADE_COST_SCRAP);
        UPGRADE_COST_NETHERITE_SCRAP = integer(p, "upgrade.costNetheriteScrap", UPGRADE_COST_NETHERITE_SCRAP);

        CYBERPSYCHO_ENABLED = bool(p, "cyberpsycho.enabled", CYBERPSYCHO_ENABLED);
        CYBERPSYCHO_LIGHT_THRESHOLD = integer(p, "cyberpsycho.lightThreshold", CYBERPSYCHO_LIGHT_THRESHOLD);
        CYBERPSYCHO_SPAWN_CHANCE = dbl(p, "cyberpsycho.spawnChance", CYBERPSYCHO_SPAWN_CHANCE);
        VILLAGE_KILL_THRESHOLD = integer(p, "cyberpsycho.villageKillThreshold", VILLAGE_KILL_THRESHOLD);

        SHIELD_ENABLED = bool(p, "shield.enabled", SHIELD_ENABLED);
        SHIELD_BASE_REGEN_PER_SECOND = dbl(p, "shield.baseRegenPerSecond", SHIELD_BASE_REGEN_PER_SECOND);
        SHIELD_COMBAT_DELAY_TICKS = integer(p, "shield.combatDelayTicks", SHIELD_COMBAT_DELAY_TICKS);
        SHIELD_DEBUG_MESSAGES = bool(p, "shield.debugMessages", SHIELD_DEBUG_MESSAGES);

        TIME_DILATION_ALLOW_STACK = bool(p, "skills.allowTimeDilationStack", TIME_DILATION_ALLOW_STACK);
        TIME_DILATION_KILL_EXTEND_ENABLED = bool(p, "skills.killExtendEnabled", TIME_DILATION_KILL_EXTEND_ENABLED);
        TIME_DILATION_KILL_EXTEND_TICKS = integer(p, "skills.killExtendTicks", TIME_DILATION_KILL_EXTEND_TICKS);
        TIME_DILATION_KILL_EXTEND_LIMIT = integer(p, "skills.killExtendLimit", TIME_DILATION_KILL_EXTEND_LIMIT);
        POST_EFFECT_ENABLED = bool(p, "skills.postEffectEnabled", POST_EFFECT_ENABLED);
        TIME_DILATION_PROJECTILE_IMMUNE = bool(p, "skills.projectileImmune", TIME_DILATION_PROJECTILE_IMMUNE);

        BERSERK_INVULNERABLE = bool(p, "berserk.invulnerable", BERSERK_INVULNERABLE);
        BERSERK_DAMAGE_MULTIPLIER = dbl(p, "berserk.damageMultiplier", BERSERK_DAMAGE_MULTIPLIER);
        BERSERK_ATTACK_SPEED_BONUS = dbl(p, "berserk.attackSpeedBonus", BERSERK_ATTACK_SPEED_BONUS);
        BERSERK_MOVEMENT_SPEED_BONUS = dbl(p, "berserk.movementSpeedBonus", BERSERK_MOVEMENT_SPEED_BONUS);
        BERSERK_DURATION_SECONDS = integer(p, "berserk.durationSeconds", BERSERK_DURATION_SECONDS);
        BERSERK_KILL_EXTEND_TICKS = integer(p, "berserk.killExtendTicks", BERSERK_KILL_EXTEND_TICKS);
        BERSERK_KILL_EXTEND_LIMIT = integer(p, "berserk.killExtendLimit", BERSERK_KILL_EXTEND_LIMIT);
        BERSERK_KILL_HEAL_HEARTS = dbl(p, "berserk.killHealHearts", BERSERK_KILL_HEAL_HEARTS);

        // 老文件（或手改漏项）补齐：写回当前生效值 + 全部注释，用户已有值不变
        for (String key : ALL_KEYS) {
            if (!p.containsKey(key)) {
                save(file);
                return;
            }
        }
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    /** 把当前生效值连注释一起写盘（首次生成 / 补缺项时用）。 */
    public static void save() throws Exception {
        save(path());
    }

    private static void save(Path file) throws Exception {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        StringBuilder sb = new StringBuilder(4096);

        sb.append("# Cyberware 配置文件（Fabric 版，纯 java.util.Properties，无第三方依赖）\n");
        sb.append("# 改完保存后重启游戏生效；删掉本文件会按默认值重建。\n");
        sb.append("# 布尔值写 true / false；小数用点号（0.5 而不是 0,5）。\n");

        sb.append('\n');
        sb.append("# ========== 容量系统 ==========\n");
        comment(sb, "是否启用容量上限限制。关闭后可以无限安装（调试用）");
        kv(sb, "capacity.enableCapacityLimit", ENABLE_CAPACITY_LIMIT);
        comment(sb, "玩家默认植入体容量上限");
        kv(sb, "capacity.defaultCapacity", DEFAULT_CAPACITY);

        sb.append('\n');
        sb.append("# ========== 升级系统 ==========\n");
        comment(sb, "升级成功率（0.0 ~ 1.0）。TODO(主人填写): 默认 0.6 为占位值");
        kv(sb, "upgrade.successRate", UPGRADE_SUCCESS_RATE);
        comment(sb, "升级消耗绿宝石数量");
        kv(sb, "upgrade.costEmerald", UPGRADE_COST_EMERALD);
        comment(sb, "升级消耗废料数量");
        kv(sb, "upgrade.costScrap", UPGRADE_COST_SCRAP);
        comment(sb, "升级消耗下界合金碎片数量");
        kv(sb, "upgrade.costNetheriteScrap", UPGRADE_COST_NETHERITE_SCRAP);

        sb.append('\n');
        sb.append("# ========== 赛博精神病（危险敌人） ==========\n");
        comment(sb, "是否允许赛博精神病自然生成");
        kv(sb, "cyberpsycho.enabled", CYBERPSYCHO_ENABLED);
        comment(sb, "生成所需的最高亮度（低于该值且在夜晚才有概率生成）");
        kv(sb, "cyberpsycho.lightThreshold", CYBERPSYCHO_LIGHT_THRESHOLD);
        comment(sb, "每次生成判定的概率。TODO(主人填写): 默认 0.02 为占位值");
        kv(sb, "cyberpsycho.spawnChance", CYBERPSYCHO_SPAWN_CHANCE);
        comment(sb, "在村庄击杀多少村民后触发暴动事件");
        kv(sb, "cyberpsycho.villageKillThreshold", VILLAGE_KILL_THRESHOLD);

        sb.append('\n');
        sb.append("# ========== 护盾 ==========\n");
        comment(sb, "是否启用护盾系统（关闭后伤害完全按原版走，排查用）");
        kv(sb, "shield.enabled", SHIELD_ENABLED);
        comment(sb, "护盾基础恢复速度（点/秒）。实际速度 = 这个值 + 已装义体的 shield_regen 之和");
        comment(sb, "主人定的基准是 3.0；改这里不用改代码");
        kv(sb, "shield.baseRegenPerSecond", SHIELD_BASE_REGEN_PER_SECOND);
        comment(sb, "脱战多少刻后开始回盾（20 刻 = 1 秒，60 = 3 秒）");
        kv(sb, "shield.combatDelayTicks", SHIELD_COMBAT_DELAY_TICKS);
        comment(sb, "吸收伤害时在动作栏打一行调试文字（护盾条做好后关掉）");
        kv(sb, "shield.debugMessages", SHIELD_DEBUG_MESSAGES);

        sb.append('\n');
        sb.append("# ========== 主动技能 ==========\n");
        comment(sb, "斯安威斯坦与克伦齐科夫同时触发时是否叠加减速");
        comment(sb, "false = 取最强减速比例，防止实体 tick 彻底停滞");
        kv(sb, "skills.allowTimeDilationStack", TIME_DILATION_ALLOW_STACK);
        comment(sb, "击杀是否延长斯安威斯坦的持续时间");
        kv(sb, "skills.killExtendEnabled", TIME_DILATION_KILL_EXTEND_ENABLED);
        comment(sb, "每击杀一个敌人延长的 tick 数（20 tick = 1 秒）");
        kv(sb, "skills.killExtendTicks", TIME_DILATION_KILL_EXTEND_TICKS);
        comment(sb, "单次激活期间由击杀累计延长的上限（tick）");
        kv(sb, "skills.killExtendLimit", TIME_DILATION_KILL_EXTEND_LIMIT);
        comment(sb, "斯安威斯坦激活时的屏幕后处理（边缘径向模糊 + 色散）");
        comment(sb, "关掉可排除显卡/驱动相关的显示问题");
        kv(sb, "skills.postEffectEnabled", POST_EFFECT_ENABLED);
        comment(sb, "斯安威斯坦激活期间免疫弹射物伤害（箭矢、雪球、火球等）");
        kv(sb, "skills.projectileImmune", TIME_DILATION_PROJECTILE_IMMUNE);

        sb.append('\n');
        sb.append("# ========== 狂暴（Berserk）—— 把痛觉和疲劳一起关掉 ==========\n");
        comment(sb, "狂暴期间是否完全免疫伤害");
        kv(sb, "berserk.invulnerable", BERSERK_INVULNERABLE);
        comment(sb, "狂暴期间造成的伤害倍率");
        kv(sb, "berserk.damageMultiplier", BERSERK_DAMAGE_MULTIPLIER);
        comment(sb, "狂暴期间攻击速度加成（0.5 = 挥砍频率 +50%）");
        kv(sb, "berserk.attackSpeedBonus", BERSERK_ATTACK_SPEED_BONUS);
        comment(sb, "狂暴期间移动速度加成（0.25 = 奔跑速度 +25%）");
        kv(sb, "berserk.movementSpeedBonus", BERSERK_MOVEMENT_SPEED_BONUS);
        comment(sb, "默认持续时间（秒）。型号自带数值时会覆盖这项");
        kv(sb, "berserk.durationSeconds", BERSERK_DURATION_SECONDS);
        comment(sb, "每击杀一个敌人延长的 tick 数（20 tick = 1 秒）");
        kv(sb, "berserk.killExtendTicks", BERSERK_KILL_EXTEND_TICKS);
        comment(sb, "单次狂暴由击杀累计延长的上限（tick）");
        kv(sb, "berserk.killExtendLimit", BERSERK_KILL_EXTEND_LIMIT);
        comment(sb, "每击杀一个敌人恢复的心数（1 颗心 = 2 点生命）");
        kv(sb, "berserk.killHealHearts", BERSERK_KILL_HEAL_HEARTS);

        Files.write(file, sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void comment(StringBuilder sb, String text) {
        sb.append("# ").append(text).append('\n');
    }

    private static void kv(StringBuilder sb, String key, Object value) {
        sb.append(key).append('=').append(value).append('\n');
    }

    private static boolean bool(Properties p, String key, boolean def) {
        String v = p.getProperty(key);
        return v == null ? def : Boolean.parseBoolean(v.trim());
    }

    private static int integer(Properties p, String key, int def) {
        String v = p.getProperty(key);
        if (v == null) {
            return def;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static double dbl(Properties p, String key, double def) {
        String v = p.getProperty(key);
        if (v == null) {
            return def;
        }
        try {
            return Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
