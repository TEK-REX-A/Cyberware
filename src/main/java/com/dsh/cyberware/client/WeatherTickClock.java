package com.dsh.cyberware.client;

/**
 * 天气（雨/雪）的虚拟时钟。
 *
 * <p>雨雪不走粒子系统 —— 它们是 {@code WeatherEffectRenderer} 按「游戏刻 + 插值」算出来的
 * 柱体，所以 {@code ParticleMixin} 够不着。
 *
 * <p><b>第一版为什么卡顿</b>：只把 {@code ticks}（int）换成了虚拟刻数，而 {@code partialTick}
 * 还是原版那根 0→1 的帧插值。结果是——雨滴位置 = virtualTicks + partialTick：
 * virtualTicks 每 4 刻才 +1，partialTick 却每刻都从 0 走到 1，于是「不动 → 突然跳一格」。
 *
 * <p><b>现在的做法</b>：两边一起改。虚拟时间 = 累加的虚拟刻 + 帧内插值 × timeScale，
 * 再把它的整数部分喂给 ticks、小数部分喂给 partialTick。这样雨滴拿到的是一个**连续**
 * 且推进速度为 timeScale 的时间轴，平滑变慢。
 */
public final class WeatherTickClock {

    private static double virtualTicks;
    private static double lastTimeScale = 1.0D;
    private static int lastRealTick = Integer.MIN_VALUE;

    /** 同一帧内算出的一次虚拟时间（ticks 与 partialTick 必须取自同一个值） */
    private static double frameTime;
    private static boolean frameTimeValid;

    private WeatherTickClock() {
    }

    /** 每客户端 tick 调一次，按当前时间倍率推进虚拟刻数。 */
    public static void tick(int realTick, double timeScale) {
        lastTimeScale = Math.max(0.0D, Math.min(1.0D, timeScale));
        if (lastRealTick == Integer.MIN_VALUE) {
            lastRealTick = realTick;
            virtualTicks = realTick;
            return;
        }
        int delta = Math.max(0, realTick - lastRealTick);
        lastRealTick = realTick;
        virtualTicks += delta * lastTimeScale;
    }

    /** 供给这一帧的连续虚拟时间：整数部分给 ticks，小数部分给 partialTick。 */
    private static double frameTime(float realPartialTick) {
        frameTime = virtualTicks + realPartialTick * lastTimeScale;
        return frameTime;
    }

    public static int virtualTickPart(int fallback, float realPartialTick) {
        if (lastRealTick == Integer.MIN_VALUE) {
            return fallback;
        }
        return (int) Math.floor(frameTime(realPartialTick));
    }

    public static float virtualPartialPart(float fallback, float realPartialTick) {
        if (lastRealTick == Integer.MIN_VALUE) {
            return fallback;
        }
        double t = frameTime(realPartialTick);
        return (float) (t - Math.floor(t));
    }

    public static void reset() {
        lastRealTick = Integer.MIN_VALUE;
        virtualTicks = 0.0D;
        lastTimeScale = 1.0D;
    }
}
