package com.dsh.cyberware.client;

import net.minecraft.client.Minecraft;

/**
 * 狂暴 · 客户端状态。
 *
 * <p>{@code totalTicks} 由服务端在激活时给出并在刷新时保持不变 —— 进度条才不会
 * 每隔一秒被重置回满格（斯安威斯坦那边踩过一次这个坑）。
 */
public final class BerserkClientState {

    private static long endTick;
    private static long totalTicks;
    private static float damageMultiplier = 1.0F;
    private static boolean active;

    private BerserkClientState() {
    }

    /**
     * 清空全部状态（换世界时调用）。
     *
     * <p>这里同样按**绝对游戏刻**记账（{@code endTick}），新世界的 gameTime 从 0 起，
     * 于是 {@code endTick - now} 变成几万秒、{@code progress()} 被 clamp 到 100%
     * （真机 P0 截图 {@code Berserk 100% 16059.7s}）。
     *
     * <p>而且服务端发的 {@code active=false} 也救不了：下面的防抖判断
     * {@code now >= endTick} 在新世界里同样不成立 —— 所以必须从这个方法硬清。
     */
    public static void clear() {
        endTick = 0L;
        totalTicks = 0L;
        damageMultiplier = 1.0F;
        active = false;
    }

    public static void onPayload(int remainingTicks, int totalTicks, float multiplier, boolean isActive) {
        long now = now();
        if (!isActive) {
            // 只有**确实已经过期**才收掉。刷新广播偶尔乱序到达时，
            // 一个迟到的 active=false 曾经把进度条直接抹掉 —— 这里加个防抖。
            if (now >= endTick) {
                active = false;
                endTick = 0L;
            }
            return;
        }
        active = true;
        damageMultiplier = multiplier;
        // 只往前推，绝不往回缩
        endTick = Math.max(endTick, now + Math.max(1, remainingTicks));
        if (totalTicks > 0) {
            BerserkClientState.totalTicks = totalTicks;
        }
    }

    public static boolean active() {
        return active && now() < endTick;
    }

    public static float damageMultiplier() {
        return damageMultiplier;
    }

    /** 进度：1.0 = 刚激活。 */
    public static float progress() {
        long now = now();
        if (!active() || totalTicks <= 0L) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, (endTick - now) / (float) totalTicks));
    }

    public static float remainingSeconds() {
        long now = now();
        return now >= endTick ? 0.0F : (endTick - now) / 20.0F;
    }

    /** 结束前半秒的淡出强度（0..1）。 */
    public static float intensity() {
        long now = now();
        if (!active() || now >= endTick) {
            return 0.0F;
        }
        long remaining = endTick - now;
        return remaining < 10L ? remaining / 10.0F : 1.0F;
    }

    private static long now() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 0L : mc.level.getGameTime();
    }
}
