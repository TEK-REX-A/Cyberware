package com.dsh.cyberware.core;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 统一时间减缓管理器。
 *
 * <p>以「激活源」为单位记账：每个施法者一条记录（位置 + 半径 + 比例 + 到期时间）。
 * 查询某个实体的时间倍率时，遍历所有激活源，**取最小的倍率**（即最重的减速），
 * 这样多个斯安威斯坦重叠时不会叠加成"时间静止"。
 *
 * <p>架构参考自内部测试版 <i>Epic ParCool: Momentum</i> 的同类管理器，代码自行实现。
 */
public final class TimeDilationManager {

    /** 低于这个减速比例视为没开 */
    public static final double MIN_RATIO = 0.01D;
    /** 结束前的平滑衰减（tick）：10 tick = 0.5 秒 */
    public static final int FADE_TICKS = 10;
    /** 默认作用半径（格）。TODO(主人填写): 可按型号配置 */
    public static final double DEFAULT_RADIUS = 16.0D;

    private static final Map<UUID, Activation> ACTIVE = new HashMap<>();

    private TimeDilationManager() {
    }

    /** 一条激活记录。 */
    private static final class Activation {
        private UUID owner;
        private double ratio;
        private double radius;
        private long expireAt;
        private String source;
        /** 本次激活已由击杀累计延长的 tick 数。 */
        private int extended;
        private double x;
        private double y;
        private double z;
        /**
         * 这条记录属于**哪个世界实例**。
         *
         * <p>0.3.11 记的是 {@code dimension()}（维度键），那**挡不住换世界**：
         * 新建出来的世界维度键还是 {@code minecraft:overworld}，可它的 {@code gameTime}
         * 从 0 重新算 —— 旧的 {@code expireAt} 于是变成「几小时后才过期」，
         * 减速/狂暴在新世界里继续**真生效**（真机 P0：HUD 显示 16181s）。
         * 只有 Level **实例**身份才能区分「同一个维度键的两个不同世界」。
         *
         * <p>用弱引用：这张表是静态的，不能让一个已卸载的世界被它钉在内存里；
         * 弱引用被回收也正好当作「这个世界已经没了」的判据（见 {@link #ownerLevel}）。
         */
        private WeakReference<Level> levelRef;
    }

    /**
     * 触发一次时间减缓。
     *
     * @param player    施法者（自己与坐骑豁免）
     * @param sourceId  来源型号 id
     * @param ratio     减速比例（0.25 = 目标只剩 75% 速度）
     * @param durationTicks 持续 tick
     * @param radius    作用半径（格）
     */
    public static void activate(Player player, String sourceId, double ratio, int durationTicks, double radius) {
        if (player == null || ratio <= 0.0D || durationTicks <= 0) {
            return;
        }
        Level level = player.level();
        long now = level.getGameTime();
        double clamped = Math.min(Math.max(ratio, MIN_RATIO), 0.99D);
        double r = radius > 0 ? radius : DEFAULT_RADIUS;

        Activation a = ACTIVE.get(player.getUUID());
        if (a != null && a.owner == null) {
            a.owner = player.getUUID();
        }
        // ⚠ 只有「同一个世界实例里还没过期」才允许续期。
        //   否则（换了世界）旧 expireAt 是**旧世界**的刻数，续期就等于把几小时的减速
        //   原封不动搬到新世界 —— 必须当成全新一条重开。
        if (a != null && belongsTo(a, level) && now < a.expireAt) {
            // 已在生效：取更强，时限取更晚
            if (clamped > a.ratio) {
                a.ratio = clamped;
                a.source = sourceId;
            }
            a.expireAt = Math.max(a.expireAt, now + durationTicks);
        } else {
            a = new Activation();
            a.owner = player.getUUID();
            a.ratio = clamped;
            a.radius = r;
            a.expireAt = now + durationTicks;
            a.source = sourceId;
            ACTIVE.put(player.getUUID(), a);
        }
        a.x = player.getX();
        a.y = player.getY();
        a.z = player.getZ();
        a.levelRef = new WeakReference<>(level);

        broadcast(level, a, durationTicks);
    }

    // ------------------------------------------------------------------
    // 世界归属判定 —— 所有查询都必须过这一关
    // ------------------------------------------------------------------

    /** 这条记录属于给定的世界**实例**吗（弱引用已被回收 = 世界没了 → 否）。 */
    private static boolean belongsTo(Activation a, Level level) {
        return a != null && level != null && a.levelRef != null && a.levelRef.get() == level;
    }

    /** 记录所属的世界；世界已卸载（或无记录）返回 null。 */
    private static Level ownerLevel(Activation a) {
        return a == null || a.levelRef == null ? null : a.levelRef.get();
    }

    /**
     * 这条记录在给定世界里**此刻**是否真的在生效。
     *
     * <p>两道判定缺一不可：
     * ① 世界实例必须一致 —— 换世界后旧记录一律视为不存在（不看事件、不看时机，天生失效）；
     * ② 在**它自己的**世界时间轴上还没过期 —— 不同维度/不同世界的 gameTime 是不同时间轴，
     * 拿 A 世界的 now 去比 B 世界的 expireAt 本身就是错的。
     */
    private static boolean liveIn(Activation a, Level level) {
        return belongsTo(a, level) && level.getGameTime() < a.expireAt;
    }

    /**
     * 某实体当前的时间倍率：1.0 = 正常，0.2 = 只剩两成速度。
     * 多个激活源重叠时取最小值。
     */
    public static double timeScaleFor(Entity entity) {
        // 快速路径：没人开减速时立刻返回，不碰任何计算
        if (entity == null || ACTIVE.isEmpty()) {
            return 1.0D;
        }
        Level level = entity.level();
        long now = level.getGameTime();
        double scale = 1.0D;

        for (Activation a : ACTIVE.values()) {
            // 换世界 / 维度不符 / 已过期 → 不算数（旧代码只查了 dimension()，挡不住换世界）
            if (!liveIn(a, level)) {
                continue;
            }
            // 自己与坐骑豁免
            if (entity instanceof Player p && a.owner != null && p.getUUID().equals(a.owner)) {
                continue;
            }
            // 区域中心跟随施法者当前位置（斯安威斯坦是「以我为中心」的范围效果，
            // 用激活瞬间的坐标会让玩家一跑开就脱离自己的领域）
            double cx = a.x;
            double cy = a.y;
            double cz = a.z;
            if (a.owner != null && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                Player ownerPlayer = serverLevel.getPlayerByUUID(a.owner);
                if (ownerPlayer != null) {
                    cx = ownerPlayer.getX();
                    cy = ownerPlayer.getY();
                    cz = ownerPlayer.getZ();
                }
            }
            double dx = entity.getX() - cx;
            double dy = entity.getY() - cy;
            double dz = entity.getZ() - cz;
            if (dx * dx + dy * dy + dz * dz > a.radius * a.radius) {
                continue;
            }
            scale = Math.min(scale, 1.0D - fadedRatio(a, now));
        }
        return scale;
    }

    /** 玩家自己是否正处在时间减缓中（击杀延长的前提）。 */
    public static boolean isOwnDilationActive(Player player) {
        if (player == null) {
            return false;
        }
        // 之前这里只比 gameTime —— 换世界后旧 expireAt 大于新世界的 now，
        // 「自己还在减速中」会一直成立，击杀延长也就一直给。现在按世界实例判。
        return liveIn(ACTIVE.get(player.getUUID()), player.level());
    }

    /**
     * 击杀延长：把施法者自己的减速时限往后推。
     *
     * <p>累计延长有上限（{@code killExtendLimit}），避免刷怪场里无限续杯。
     *
     * @return 实际延长的 tick 数，0 表示没在生效或已达上限
     */
    public static int extend(Player player, int extraTicks, int limitTicks) {
        if (player == null || extraTicks <= 0) {
            return 0;
        }
        Level level = player.level();
        Activation a = ACTIVE.get(player.getUUID());
        if (a == null || a.owner == null || !liveIn(a, level)) {
            return 0;
        }
        int room = limitTicks - a.extended;
        if (room <= 0) {
            return 0;
        }
        int added = Math.min(extraTicks, room);
        a.extended += added;
        a.expireAt += added;
        broadcast(level, a, (int) Math.min(Integer.MAX_VALUE, a.expireAt - level.getGameTime()));
        return added;
    }

    /** 单个激活源当前的比例（含收尾衰减）。 */
    private static double fadedRatio(Activation a, long now) {
        long remaining = a.expireAt - now;
        if (remaining <= 0) {
            return 0.0D;
        }
        if (remaining < FADE_TICKS) {
            return a.ratio * (remaining / (double) FADE_TICKS);
        }
        return a.ratio;
    }

    /** 是否还有任何激活源在生效（UI/HUD 用）。 */
    public static boolean active(Level level) {
        if (level == null) {
            return false;
        }
        for (Activation a : ACTIVE.values()) {
            if (liveIn(a, level)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 每隔一小段时间刷新：把区域中心挪到施法者当前位置并重新广播。
     *
     * <p>不做这一步，客户端手里的中心点还停在激活那一刻 —— 玩家一移动，
     * HUD 和边缘特效就会因为「自己已经不在区域内」而凭空消失。
     *
     * <p>顺手做两件清理（每 20 刻跑一次，开销可忽略）：
     * ① 所属世界已被卸载（弱引用被回收）的记录删掉；
     * ② 本世界已经过期的记录删掉。
     * 别的世界/维度的记录一律**不动** —— 它们要在自己的时间轴上继续生效。
     */
    public static void refresh(Level level) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel) || ACTIVE.isEmpty()) {
            return;
        }
        long now = serverLevel.getGameTime();
        Iterator<Map.Entry<UUID, Activation>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Activation a = it.next().getValue();
            if (ownerLevel(a) == null) {
                // 世界已经卸载：这条记录不可能再生效，顺手清掉
                it.remove();
                continue;
            }
            if (!belongsTo(a, serverLevel)) {
                // 别的世界 / 别的维度：它的时间轴与玩家列表都不在这一个 level 上
                continue;
            }
            if (now >= a.expireAt) {
                it.remove();
                continue;
            }
            if (a.owner == null) {
                continue;
            }
            Player ownerPlayer = serverLevel.getPlayerByUUID(a.owner);
            if (ownerPlayer == null) {
                continue;
            }
            a.x = ownerPlayer.getX();
            a.y = ownerPlayer.getY();
            a.z = ownerPlayer.getZ();
            broadcast(serverLevel, a, (int) Math.max(1L, a.expireAt - now));
        }
    }

    /**
     * 清理记录：所属世界已卸载的，以及在**各自**世界时间轴上已过期的。
     *
     * <p>0.3.11 的版本是「拿传进来的 level 的 gameTime 去比所有记录的 expireAt」，
     * 那在跨维度时本身就是错的（不同维度时间轴不同），删错/不删都可能发生。
     * 参数因此不再需要（记录自己知道属于哪个世界），调用方只有 0 处。
     */
    public static void prune() {
        Iterator<Map.Entry<UUID, Activation>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Activation a = it.next().getValue();
            Level owned = ownerLevel(a);
            if (owned == null || owned.getGameTime() >= a.expireAt) {
                it.remove();
            }
        }
    }

    /** 全清。注意：换世界**不再依赖**它（见 {@link #liveIn}），这里留给调试命令用。 */
    public static void clear() {
        ACTIVE.clear();
    }

    /** 广播给同维度所有玩家：带上区域中心、半径与施法者，客户端才能精确判断谁该慢。 */
    private static void broadcast(Level level, Activation a, int durationTicks) {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayersInDimension(
                    serverLevel,
                    new com.dsh.cyberware.network.TimeDilationPayload(
                            durationTicks, (float) a.ratio, a.x, a.y, a.z, (float) a.radius, a.owner));
        }
    }
}
