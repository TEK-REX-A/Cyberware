package com.dsh.cyberware.core;

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
        private net.minecraft.resources.ResourceKey<Level> dimension;
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
        if (a != null && now < a.expireAt) {
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
        a.dimension = level.dimension();

        broadcast(level, a, durationTicks);
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
            if (now >= a.expireAt || a.dimension == null || !a.dimension.equals(level.dimension())) {
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
        Activation a = ACTIVE.get(player.getUUID());
        if (a == null || a.owner == null) {
            return false;
        }
        Level level = player.level();
        return level != null && level.getGameTime() < a.expireAt;
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
        Activation a = ACTIVE.get(player.getUUID());
        if (a == null || a.owner == null) {
            return 0;
        }
        Level level = player.level();
        if (level == null || level.getGameTime() >= a.expireAt) {
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
        long now = level == null ? 0L : level.getGameTime();
        for (Activation a : ACTIVE.values()) {
            if (now < a.expireAt) {
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
     */
    public static void refresh(Level level) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel) || ACTIVE.isEmpty()) {
            return;
        }
        long now = serverLevel.getGameTime();
        for (Activation a : ACTIVE.values()) {
            if (now >= a.expireAt || a.owner == null) {
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

    /** 清理过期记录。 */
    public static void prune(Level level) {
        long now = level == null ? 0L : level.getGameTime();
        Iterator<Map.Entry<UUID, Activation>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            if (now >= it.next().getValue().expireAt) {
                it.remove();
            }
        }
    }

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
