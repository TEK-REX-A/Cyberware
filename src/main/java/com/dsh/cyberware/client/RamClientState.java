package com.dsh.cyberware.client;

import com.dsh.cyberware.network.HackPayload;
import com.dsh.cyberware.network.OverclockPayload;
import com.dsh.cyberware.network.RamPayload;

/**
 * 客户端 RAM / 脑机超频的<b>显示状态</b>（t25）。
 *
 * <p><b>客户端没有任何权威</b>：这里存的全是服务端同步下来的镜像（{@link RamPayload} /
 * {@link OverclockPayload}），只用于画 HUD。扣费、冷却、能否超频一律由服务端裁决 ——
 * 本类不会、也不该产生任何影响玩法的副作用。
 *
 * <p>三件事：
 * <ol>
 *   <li><b>插值</b>：服务端每 20 刻才结算一次 RAM，直接画会一跳一跳；
 *       这里按帧把显示值向目标值平滑推进（消耗快、恢复慢，见 {@link #lerpSpeed}）。</li>
 *   <li><b>事件脉冲</b>：警告闪 3 次、瘫痪红字、激活瞬间、结束前 3 秒 ——
 *       用「起始毫秒」记时间戳，渲染侧算相位，天然不依赖帧率。</li>
 *   <li><b>跨世界清理</b>：所有时间戳都是绝对毫秒，换世界必须清（0.3.12 真机 P0 的教训）。</li>
 * </ol>
 */
public final class RamClientState {

    // ── 服务端同步下来的原始值 ──
    private static double current;
    private static double max;
    private static double regenPerMinute;
    private static boolean overclockActive;
    private static boolean depleted;
    private static int remainingTicks;
    private static int totalTicks;
    private static int cooldownRemainingTicks;
    private static int cooldownTotalTicks;
    /** 收到过至少一份快照（没收到就什么都不画） */
    private static boolean synced;
    /** 最后一次收到快照的毫秒（用于判断同步是否还新鲜） */
    private static long lastPayloadMs;

    // ── 显示插值 ──
    /** 当前显示的比例（0..1），按帧向 target 推进 */
    private static float shownFraction;
    private static long lastFrameMs;

    // ── 事件脉冲（毫秒时间戳；0 = 没在放） ──
    private static long warningStartMs;
    private static long paralysisStartMs;
    private static long activateStartMs;
    private static long finishStartMs;
    /** 瘫痪/拒绝红字的文本（服务端 note） */
    private static String paralysisText = "RAM ACCESS FAILED";
    /** 上一次 tick 时是否处于警告态，用来只触发一次 */
    private static boolean wasLow;

    private RamClientState() {
    }

    // ═══════════════════ 收包 ═══════════════════

    public static void onRamPayload(RamPayload payload) {
        if (payload == null) {
            return;
        }
        current = payload.current();
        max = payload.max();
        regenPerMinute = payload.regenPerMinute();
        overclockActive = payload.overclockActive();
        depleted = payload.depleted();
        synced = true;
        lastPayloadMs = System.currentTimeMillis();
    }

    public static void onOverclockPayload(OverclockPayload payload) {
        if (payload == null || payload.action() != OverclockPayload.Action.STATE) {
            // TOGGLE 是客户端上行用的，收回来也不该改状态
            return;
        }
        boolean wasActive = overclockActive;
        overclockActive = payload.active();
        remainingTicks = payload.remainingTicks();
        totalTicks = payload.totalTicks();
        cooldownRemainingTicks = payload.cooldownRemainingTicks();
        cooldownTotalTicks = payload.cooldownTotalTicks();
        synced = true;
        long now = System.currentTimeMillis();
        lastPayloadMs = now;
        if (overclockActive && !wasActive) {
            activateStartMs = now;                       // 刚开启：扫描线闪入
        }
        if (!overclockActive && wasActive) {
            finishStartMs = 0L;                          // 关了：撕裂伪影立即停
        }
        // 结束前 3 秒 → 标记撕裂窗口
        if (overclockActive && remainingTicks > 0 && remainingTicks <= 60 && finishStartMs == 0L) {
            finishStartMs = now;
        }
    }

    /**
     * 破解包：只取「RAM 相关」的拒绝原因（t25 的瘫痪红字）。
     *
     * <p>t29 的锁定失败/无目标/未装义眼提示由 {@link HackClientState} + {@code HackHud} 负责 ——
     * 这里只认 RAM 那条，免得同一件事被两个 HUD 各画一次。
     */
    public static void onHackPayload(HackPayload payload) {
        if (payload == null || payload.action() != HackPayload.Action.REJECTED) {
            return;
        }
        String note = payload.note();
        if (note == null || !note.contains("RAM")) {
            return;                                  // NO_KIROSHI / NO_TARGET 等交给 HackHud
        }
        paralysisText = note.isBlank() ? "RAM ACCESS FAILED" : note;
        paralysisStartMs = System.currentTimeMillis();
    }

    // ═══════════════════ 每 tick / 每帧 ═══════════════════

    /** 每 tick 调用（{@code CyberwareClient.onClientTick}）：推进低 RAM 警告的边沿检测。 */
    public static void tick() {
        boolean low = isLowRam();
        if (low && !wasLow) {
            warningStartMs = System.currentTimeMillis();
        }
        wasLow = low;
    }

    /** 每帧调用一次（渲染入口）：推进插值。 */
    public static void updateForFrame() {
        long now = System.currentTimeMillis();
        float dt = lastFrameMs == 0L ? 0.016F
                : Math.min(0.25F, (now - lastFrameMs) / 1000.0F);
        lastFrameMs = now;
        float target = max > 0.0D ? (float) (current / max) : 0.0F;
        target = Math.max(0.0F, Math.min(1.0F, target));
        float k = Math.min(1.0F, lerpSpeed(target) * dt);
        shownFraction += (target - shownFraction) * k;
        if (Math.abs(target - shownFraction) < 0.001F) {
            shownFraction = target;
        }
    }

    /** 消耗要「唰」地下去（快），恢复要「流动灌满」（慢）——快慢差就是手感。 */
    private static float lerpSpeed(float target) {
        return target < shownFraction ? 9.0F : 2.2F;
    }

    /** 换世界/退出：所有绝对时间戳必须清，否则新世界第一帧会拿旧账渲染。 */
    public static void clear() {
        current = 0.0D;
        max = 0.0D;
        regenPerMinute = 0.0D;
        overclockActive = false;
        depleted = false;
        remainingTicks = 0;
        totalTicks = 0;
        cooldownRemainingTicks = 0;
        cooldownTotalTicks = 0;
        synced = false;
        lastPayloadMs = 0L;
        shownFraction = 0.0F;
        lastFrameMs = 0L;
        warningStartMs = 0L;
        paralysisStartMs = 0L;
        activateStartMs = 0L;
        finishStartMs = 0L;
        wasLow = false;
        paralysisText = "RAM ACCESS FAILED";
    }

    // ═══════════════════ 派生状态（契约 §5 判定表） ═══════════════════

    public static boolean synced() {
        return synced;
    }

    /** 没有 RAM 能力：{@code max <= 0}（契约 §5 第一行）。 */
    public static boolean hasNoRam() {
        return max <= 0.0D;
    }

    /** 濒死超频：{@code current <= 0 && overclockActive}。 */
    public static boolean dyingOverclock() {
        return depleted && overclockActive;
    }

    /** 彻底瘫痪：{@code current <= 0 && !overclockActive}。 */
    public static boolean paralysed() {
        return depleted && !overclockActive && max > 0.0D;
    }

    /** RAM 不足（警告态）：有 RAM 能力、没瘫痪，但剩余比例很低。 */
    public static boolean isLowRam() {
        return max > 0.0D && !depleted && (current / max) <= 0.25D;
    }

    public static boolean overclockActive() {
        return overclockActive;
    }

    public static double current() {
        return current;
    }

    public static double max() {
        return max;
    }

    public static double regenPerMinute() {
        return regenPerMinute;
    }

    public static float shownFraction() {
        return shownFraction;
    }

    public static int remainingTicks() {
        return remainingTicks;
    }

    public static int totalTicks() {
        return totalTicks;
    }

    public static int cooldownRemainingTicks() {
        return cooldownRemainingTicks;
    }

    /** 超频剩余秒数（向上取整，0 = 不在超频）。 */
    public static int remainingSeconds() {
        return (remainingTicks + 19) / 20;
    }

    /** 超频进度 0..1（1 = 刚开始）。 */
    public static float overclockProgress() {
        return totalTicks <= 0 ? 0.0F : Math.max(0.0F, Math.min(1.0F, (float) remainingTicks / totalTicks));
    }

    public static long warningStartMs() {
        return warningStartMs;
    }

    public static long paralysisStartMs() {
        return paralysisStartMs;
    }

    public static long activateStartMs() {
        return activateStartMs;
    }

    public static long finishStartMs() {
        return finishStartMs;
    }

    public static String paralysisText() {
        return paralysisText;
    }

    /** 同步是否还新鲜（超过 3 秒没收到包就认为状态不可信，别拿旧值画）。 */
    public static boolean fresh() {
        return synced && (System.currentTimeMillis() - lastPayloadMs) < 3000L;
    }
}
