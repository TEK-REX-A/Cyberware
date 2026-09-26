package com.dsh.cyberware.core;

import net.minecraft.world.entity.Entity;

/**
 * 跳 tick 闸门 —— 用「游戏刻 + 实体 id」算相位，保证<b>客户端与服务端跳过的是同一批刻</b>。
 *
 * <p>为什么不用「每实体一个累加器」：累加器的相位取决于它第一次被调用是哪一刻，
 * 客户端和服务端各自独立累加，跑一会儿就错开了 —— 于是服务端不动的刻客户端在动，
 * 位置包一来又被拽回，观感就是抖。相位由 gameTime 和实体 id 直接算出来，两端永远一致。
 *
 * <p>{@code timeScale = 0.2} → {@code period = 5} → 每 5 刻放行 1 刻，其余 4 刻整个 tick 被跳过。
 */
public final class DilationTickGate {

    /** 闸门下限：再慢也不会慢过 5% */
    public static final double MIN_TIME_SCALE = 0.05D;

    private DilationTickGate() {
    }

    /**
     * 这一 tick 该不该跳过。
     *
     * @param entity    目标实体（要有稳定的 id 与所在 level）
     * @param timeScale 1.0 = 正常，0.2 = 五分之一速
     * @return true 表示这一 tick 应当被跳过
     */
    public static boolean shouldSkip(Entity entity, double timeScale) {
        if (entity == null || timeScale >= 1.0D) {
            return false;
        }
        double clamped = Math.max(MIN_TIME_SCALE, Math.min(1.0D, timeScale));
        int period = (int) Math.round(1.0D / clamped);
        if (period <= 1) {
            return false;
        }
        long gameTime = entity.level().getGameTime();
        // 实体 id 让不同实体错开相位，不会所有箭在同一刻一起跳
        return Math.floorMod(gameTime + entity.getId(), period) != 0;
    }
}
