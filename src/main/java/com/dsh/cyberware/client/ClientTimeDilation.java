package com.dsh.cyberware.client;

import com.dsh.cyberware.core.TimeDilationManager;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

/**
 * 时间减缓 · 客户端状态与渲染层。
 *
 * <p>服务端广播「哪个区域、多强、多久、谁开的」，这里按**区域**记账：
 * 渲染某个生物前先看它有没有落在区域内，落在里面才把动画参数按倍率缩小。
 *
 * <p>两条必须排除的：
 * <ul>
 *   <li><b>施法者本人</b>（按 UUID）—— 否则自己挥刀的动作也被拖慢</li>
 *   <li><b>本地玩家</b>（按渲染坐标比对）—— 别人开减速时，站在旁边的我也不该被拖慢</li>
 * </ul>
 */
public final class ClientTimeDilation {

    /** 一条客户端减速源，与服务端的 Activation 一一对应。 */
    private static final class Source {
        private double x;
        private double y;
        private double z;
        private float radius;
        private final float ratio;
        private final long startTick;
        private long endTick;
        private final long totalTicks;
        private final UUID owner;

        private Source(double x, double y, double z, float radius, float ratio,
                       long startTick, long endTick, long totalTicks, UUID owner) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.ratio = ratio;
            this.startTick = startTick;
            this.endTick = endTick;
            this.totalTicks = totalTicks;
            this.owner = owner;
        }
    }

    private static final List<Source> SOURCES = new ArrayList<>();

    /**
     * 清空所有减速源（换世界时调用）。
     *
     * <p>源里记的是**绝对游戏刻**（{@code endTick}），而 {@code level.getGameTime()}
     * 是每个世界各自从 0 开始的。换世界后旧的 endTick 会变成「几万秒后才结束」：
     * HUD 直接显示 {@code 16181.0s}、进度被 clamp 到 100%（真机 P0 截图）。
     *
     * <p>注意：**不能指望"自然过期"**—— 新世界的 now 远小于旧 endTick，
     * {@code now >= endTick} 这个条件要等好几个小时才成立。所以必须显式清。
     */
    public static void clear() {
        SOURCES.clear();
    }

    /** 本地玩家自身的位置匹配容差（格）。用于把「我自己」从动画减速里摘出去。 */
    private static final double SELF_EPSILON = 0.08D;

    private ClientTimeDilation() {
    }

    /** 收到服务端广播：同一个施法者的旧记录直接替换，其余按到期时间清理。 */
    public static void applyOnClient(int durationTicks, float ratio, double x, double y, double z,
                                     float radius, UUID owner) {
        long now = now();

        // 同一个施法者的**刷新广播**：只把结束时间往后推、把区域挪到新位置。
        //
        // 这里绝不能用这条广播重新初始化 startTick/totalTicks —— 服务端每 20 刻刷新一次，
        // 那样进度条会每隔一秒被重置回 100%，看起来就是 100% → 96% → 100% 地反复横跳。
        if (owner != null) {
            for (Source s : SOURCES) {
                if (owner.equals(s.owner) && now < s.endTick) {
                    s.endTick = Math.max(s.endTick, now + Math.max(1, durationTicks));
                    s.x = x;
                    s.y = y;
                    s.z = z;
                    s.radius = Math.max(1.0F, radius);
                    return;
                }
            }
            SOURCES.removeIf(s -> owner.equals(s.owner));
        }
        SOURCES.removeIf(s -> now >= s.endTick);
        SOURCES.add(new Source(x, y, z, Math.max(1.0F, radius), ratio,
                now, now + Math.max(1, durationTicks), Math.max(1, durationTicks), owner));

        // 开启音效（t38）：只在**新建一条源**时响一次 —— 也就是「这次减速真的开始了」。
        // 上面那条刷新广播的分支已经 return，所以服务端每 20 刻的刷新不会重复触发；
        // 减速结束、换维度、死亡都不经过这里。
        // 只有**本地玩家自己**开的才播（别人开的减速我们只是旁观者）。
        if (owner != null) {
            LocalPlayer self = Minecraft.getInstance().player;
            if (self != null && owner.equals(self.getUUID())) {
                SandevistanSounds.playActivate();
            }
        }
    }

    /** 世界坐标 (x,y,z) 处的减速比例，0 = 不受影响。多条源重叠时取最强的一条。 */
    public static float ratioAt(double x, double y, double z) {
        // 快速路径：没人开减速时连时钟都不碰。粒子每刻都会走这条路，值钱。
        if (SOURCES.isEmpty()) {
            return 0.0F;
        }
        long now = now();
        float best = 0.0F;
        for (Iterator<Source> it = SOURCES.iterator(); it.hasNext(); ) {
            Source s = it.next();
            if (now >= s.endTick) {
                it.remove();
                continue;
            }
            double dx = x - s.x;
            double dy = y - s.y;
            double dz = z - s.z;
            if (dx * dx + dy * dy + dz * dz > (double) s.radius * s.radius) {
                continue;
            }
            best = Math.max(best, faded(s, now));
        }
        return best;
    }

    /** 某坐标处的时间倍率（1.0 = 正常，0.25 = 只剩两成半）。粒子减速用这个。 */
    public static double timeScaleAt(double x, double y, double z) {
        return 1.0D - ratioAt(x, y, z);
    }

    /** 当前影响**本地玩家**的那条源（HUD / 屏幕特效用）。 */
    private static Source selfSource(long now) {
        LocalPlayer self = Minecraft.getInstance().player;
        if (self == null) {
            return null;
        }
        // 优先：自己开的源 —— 区域会跟着自己走，不该因为「跑远了」就找不到
        for (Source s : SOURCES) {
            if (now < s.endTick && self.getUUID().equals(s.owner)) {
                return s;
            }
        }

        Source best = null;
        float bestRatio = 0.0F;
        for (Source s : SOURCES) {
            if (now >= s.endTick) {
                continue;
            }
            double dx = self.getX() - s.x;
            double dy = self.getY() - s.y;
            double dz = self.getZ() - s.z;
            if (dx * dx + dy * dy + dz * dz > (double) s.radius * s.radius) {
                continue;
            }
            float r = faded(s, now);
            if (r > bestRatio) {
                bestRatio = r;
                best = s;
            }
        }
        return best;
    }

    /** 当前减速比例（含收尾衰减），0 表示没在生效。 */
    public static float ratioNow() {
        long now = now();
        Source s = selfSource(now);
        return s == null ? 0.0F : faded(s, now);
    }

    /** 进度：1.0 = 刚激活，0.0 = 结束（进度条用）。 */
    public static float progress() {
        long now = now();
        Source s = selfSource(now);
        if (s == null || s.totalTicks <= 0L) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, (s.endTick - now) / (float) s.totalTicks));
    }

    /** 剩余秒数（HUD 显示用）。 */
    public static float remainingSeconds() {
        long now = now();
        Source s = selfSource(now);
        return s == null ? 0.0F : (s.endTick - now) / 20.0F;
    }

    /** 是否正处于减速中。 */
    public static boolean active() {
        return selfSource(now()) != null;
    }

    private static float faded(Source s, long now) {
        long remaining = s.endTick - now;
        if (remaining <= 0L) {
            return 0.0F;
        }
        if (remaining < TimeDilationManager.FADE_TICKS) {
            return s.ratio * (remaining / (float) TimeDilationManager.FADE_TICKS);
        }
        return s.ratio;
    }

    private static long now() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 0L : mc.level.getGameTime();
    }

    /** 这个渲染状态是不是本地玩家自己。 */
    private static boolean isSelf(LivingEntityRenderState state, float partialTick) {
        LocalPlayer self = Minecraft.getInstance().player;
        if (self == null) {
            return false;
        }
        Vec3 p = self.getPosition(partialTick);
        return Math.abs(state.x - p.x) < SELF_EPSILON
                && Math.abs(state.y - p.y) < SELF_EPSILON
                && Math.abs(state.z - p.z) < SELF_EPSILON;
    }

    /** 渲染生物前：只对落在减速区域内的目标缩放动画，且不碰施法者与本地玩家。 */
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?, ?> event) {
        LivingEntityRenderState state = event.getRenderState();
        float r = ratioAt(state.x, state.y, state.z);
        if (r <= 0.01F) {
            return;
        }
        if (isSelf(state, event.getPartialTick())) {
            return;
        }
        float factor = 1.0F - r;
        // 走路动画
        state.walkAnimationPos *= factor;
        state.walkAnimationSpeed *= factor;
        // 攻击挥砍动画：attackTime 定义在 ArmedEntityRenderState 上（HumanoidRenderState 继承它）
        if (state instanceof ArmedEntityRenderState armed) {
            armed.attackTime *= factor;
        }
    }
}
