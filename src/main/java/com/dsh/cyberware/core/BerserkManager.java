package com.dsh.cyberware.core;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 狂暴（Berserk）状态管理器。
 *
 * <p>狂暴的设定是「把身体的痛觉与疲劳一起关掉」：无敌、伤害翻倍、屏幕上连自己的
 * 生命值都看不见 —— 玩家只能靠那条进度条判断还剩多久。所以这里只需要记
 * 「谁在狂暴、还剩多久、伤害倍率多少」三件事。
 *
 * <p>与 {@link TimeDilationManager} 一样以施法者为单位记账，并周期性同步给客户端。
 *
 * <p><b>0.3.12 · 换世界残留修复（P0）</b>：所有状态都记在**绝对游戏刻**上，
 * 而 {@code Level.getGameTime()} 是**每个世界各自**从 0 开始算的。以前这条记录
 * 完全不知道自己属于哪个世界，换了世界之后「新世界的 0 刻 < 旧世界的 expireAt」
 * 恒成立 —— 狂暴于是真的在新世界里继续生效几小时（真机抓到 HUD 16059.7s）。
 * 现在每条记录都带**所属世界实例**（弱引用），查不到同一个实例就一律视为没有。
 */
public final class BerserkManager {

    /** 结束前的平滑衰减（tick） */
    public static final int FADE_TICKS = 10;

    private static final Map<UUID, State> ACTIVE = new HashMap<>();

    private BerserkManager() {
    }

    private static final class State {
        /** 到期刻 —— 记在**所属世界**的时间轴上。 */
        private long expireAt;
        private int totalTicks;
        private double damageMultiplier = 1.0D;
        private int extended;
        /**
         * 所属世界实例（弱引用）。
         *
         * <p>不用维度键：新建世界的维度键还是 {@code minecraft:overworld}，
         * 但它的 gameTime 从 0 重算 —— 只有实例身份能区分「同一个维度键的两个世界」。
         * 用弱引用是为了不让这张静态表把已卸载的世界钉在内存里；弱引用被回收
         * 同时就是「世界已经没了 → 记录必然失效」的判据。
         */
        private WeakReference<Level> levelRef;
    }

    /** 触发（或续期）一次狂暴。 */
    public static void activate(Player player, double damageMultiplier, int durationTicks) {
        if (player == null || durationTicks <= 0) {
            return;
        }
        Level level = player.level();
        long now = level.getGameTime();
        State state = ACTIVE.get(player.getUUID());
        // ⚠ 换了世界（Level 实例不同）必须**重开一条**，绝不能续用旧 expireAt：
        //   新世界 now 从 0 起，旧的巨大 expireAt 会让狂暴"续上几小时"。
        if (state == null || !belongsTo(state, level) || now >= state.expireAt) {
            state = new State();
            state.totalTicks = durationTicks;
            state.extended = 0;
            state.levelRef = new WeakReference<>(level);
            ACTIVE.put(player.getUUID(), state);
        }
        state.expireAt = Math.max(state.expireAt, now + durationTicks);
        state.totalTicks = Math.max(state.totalTicks, (int) Math.min(Integer.MAX_VALUE, state.expireAt - now));
        state.damageMultiplier = Math.max(state.damageMultiplier, damageMultiplier);
    }

    // ------------------------------------------------------------------
    // 世界归属判定 —— 所有查询都必须过这一关
    // ------------------------------------------------------------------

    /** 这条记录属于给定的世界**实例**吗（弱引用已被回收 = 世界没了 → 否）。 */
    private static boolean belongsTo(State state, Level level) {
        return state != null && level != null && state.levelRef != null && state.levelRef.get() == level;
    }

    /** 记录所属的世界；世界已卸载（或无记录）返回 null。 */
    private static Level ownerLevel(State state) {
        return state == null || state.levelRef == null ? null : state.levelRef.get();
    }

    /**
     * 取玩家**当前世界**里仍然生效的那条状态；没有 / 换了世界 / 已过期一律返回 null。
     *
     * <p>这是所有查询的唯一入口 —— 只要过了这一关，就不可能把旧世界的账算到新世界头上。
     */
    private static State validState(Player player) {
        if (player == null) {
            return null;
        }
        State state = ACTIVE.get(player.getUUID());
        if (state == null) {
            return null;
        }
        Level owned = ownerLevel(state);
        Level current = player.level();
        if (owned == null || current == null || owned != current) {
            return null;   // 换世界 / 世界已卸载 → 视为没有
        }
        return current.getGameTime() >= state.expireAt ? null : state;   // 自然结束
    }

    public static boolean isActive(Player player) {
        return validState(player) != null;
    }

    /** 伤害倍率；不在狂暴中返回 1.0。 */
    public static double damageMultiplier(Player player) {
        State state = validState(player);
        return state == null ? 1.0D : state.damageMultiplier;
    }

    /**
     * 击杀延长：把狂暴时限往后推（与斯安威斯坦同一套思路）。
     *
     * @return 实际延长的 tick 数
     */
    public static int extend(Player player, int extraTicks, int limitTicks) {
        if (player == null || extraTicks <= 0) {
            return 0;
        }
        State state = validState(player);
        if (state == null) {
            return 0;
        }
        int room = limitTicks - state.extended;
        if (room <= 0) {
            return 0;
        }
        int added = Math.min(extraTicks, room);
        state.extended += added;
        state.expireAt += added;
        state.totalTicks += added;
        return added;
    }

    /** 剩余比例（1.0 = 刚激活），进度条用。 */
    public static float remainingRatio(Player player) {
        State state = validState(player);
        if (state == null || state.totalTicks <= 0) {
            return 0.0F;
        }
        long remaining = state.expireAt - player.level().getGameTime();
        return Math.max(0.0F, Math.min(1.0F, remaining / (float) state.totalTicks));
    }

    /** 结束前的强度衰减（0..1），供滤镜淡出用。 */
    public static float intensity(Player player) {
        State state = validState(player);
        if (state == null) {
            return 0.0F;
        }
        long remaining = state.expireAt - player.level().getGameTime();
        if (remaining <= 0) {
            return 0.0F;
        }
        return remaining < FADE_TICKS ? remaining / (float) FADE_TICKS : 1.0F;
    }

    /** 剩余 tick（不在狂暴中返回 0）。 */
    public static int remainingTicks(Player player) {
        State state = validState(player);
        if (state == null) {
            return 0;
        }
        long remaining = state.expireAt - player.level().getGameTime();
        return remaining <= 0L ? 0 : (int) Math.min(Integer.MAX_VALUE, remaining);
    }

    /** 本次狂暴的总时长（进度条用）；不在狂暴中返回 0。 */
    public static int totalTicks(Player player) {
        State state = validState(player);
        return state == null ? 0 : state.totalTicks;
    }

    /**
     * 清理记录：所属世界已卸载的，以及在**各自**世界时间轴上已过期的。
     *
     * <p>0.3.11 的版本拿调用方那个 level 的 gameTime 去比所有记录 —— 跨维度时是错的。
     * 现在每条记录用自己的世界时钟判断，参数因此不再需要。
     */
    public static void prune() {
        Iterator<Map.Entry<UUID, State>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            State state = it.next().getValue();
            Level owned = ownerLevel(state);
            if (owned == null || owned.getGameTime() >= state.expireAt) {
                it.remove();
            }
        }
    }

    /** 全清。注意：换世界**不再依赖**它（见 {@link #validState}）。 */
    public static void clear() {
        ACTIVE.clear();
    }
}
