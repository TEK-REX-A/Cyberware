package com.dsh.cyberware.core;

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
 */
public final class BerserkManager {

    /** 结束前的平滑衰减（tick） */
    public static final int FADE_TICKS = 10;

    private static final Map<UUID, State> ACTIVE = new HashMap<>();

    private BerserkManager() {
    }

    private static final class State {
        private long expireAt;
        private int totalTicks;
        private double damageMultiplier = 1.0D;
        private int extended;
    }

    /** 触发（或续期）一次狂暴。 */
    public static void activate(Player player, double damageMultiplier, int durationTicks) {
        if (player == null || durationTicks <= 0) {
            return;
        }
        Level level = player.level();
        long now = level.getGameTime();
        State state = ACTIVE.get(player.getUUID());
        if (state == null || now >= state.expireAt) {
            state = new State();
            state.totalTicks = durationTicks;
            state.extended = 0;
            ACTIVE.put(player.getUUID(), state);
        }
        state.expireAt = Math.max(state.expireAt, now + durationTicks);
        state.totalTicks = Math.max(state.totalTicks, (int) Math.min(Integer.MAX_VALUE, state.expireAt - now));
        state.damageMultiplier = Math.max(state.damageMultiplier, damageMultiplier);
    }

    public static boolean isActive(Player player) {
        if (player == null) {
            return false;
        }
        State state = ACTIVE.get(player.getUUID());
        return state != null && player.level().getGameTime() < state.expireAt;
    }

    /** 伤害倍率；不在狂暴中返回 1.0。 */
    public static double damageMultiplier(Player player) {
        if (!isActive(player)) {
            return 1.0D;
        }
        State state = ACTIVE.get(player.getUUID());
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
        State state = ACTIVE.get(player.getUUID());
        if (state == null || player.level().getGameTime() >= state.expireAt) {
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
        if (player == null) {
            return 0.0F;
        }
        State state = ACTIVE.get(player.getUUID());
        if (state == null || state.totalTicks <= 0) {
            return 0.0F;
        }
        long remaining = state.expireAt - player.level().getGameTime();
        if (remaining <= 0) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, remaining / (float) state.totalTicks));
    }

    /** 结束前的强度衰减（0..1），供滤镜淡出用。 */
    public static float intensity(Player player) {
        if (player == null) {
            return 0.0F;
        }
        State state = ACTIVE.get(player.getUUID());
        if (state == null) {
            return 0.0F;
        }
        long remaining = state.expireAt - player.level().getGameTime();
        if (remaining <= 0) {
            return 0.0F;
        }
        if (remaining < FADE_TICKS) {
            return remaining / (float) FADE_TICKS;
        }
        return 1.0F;
    }

    /** 剩余 tick（不在狂暴中返回 0）。 */
    public static int remainingTicks(Player player) {
        if (player == null) {
            return 0;
        }
        State state = ACTIVE.get(player.getUUID());
        if (state == null) {
            return 0;
        }
        long remaining = state.expireAt - player.level().getGameTime();
        return remaining <= 0L ? 0 : (int) Math.min(Integer.MAX_VALUE, remaining);
    }

    /** 本次狂暴的总时长（进度条用）。 */
    public static int totalTicks(Player player) {
        if (player == null) {
            return 0;
        }
        State state = ACTIVE.get(player.getUUID());
        return state == null ? 0 : state.totalTicks;
    }

    /** 清理过期记录。 */
    public static void prune(Level level) {
        long now = level == null ? 0L : level.getGameTime();
        Iterator<Map.Entry<UUID, State>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            if (now >= it.next().getValue().expireAt) {
                it.remove();
            }
        }
    }

    public static void clear() {
        ACTIVE.clear();
    }
}
