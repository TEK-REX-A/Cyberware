package com.dsh.cyberware.client;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.particle.Particle;

/**
 * 粒子 tick 时钟 —— 累加器模式，与实体那边同一套思路。
 *
 * <p>{@code timeScale = 0.25} 的含义是「每 4 刻只真正 tick 一次」。余数会累积，
 * 所以跳过是**均匀摊开**的，而不是随机扎堆。
 *
 * <p>粒子是纯客户端的东西（爆炸、火花、方块碎屑），服务端管不着，
 * 所以这一步只能在客户端做 —— 参考实现里也是一个 mixin 挂在粒子引擎上。
 */
public final class ParticleTickClock {

    /** 下限：再慢也不会慢过 5% */
    public static final double MIN_TIME_SCALE = 0.05D;

    private static final Map<Particle, Clock> CLOCKS = new WeakHashMap<>();

    private ParticleTickClock() {
    }

    /**
     * 该粒子这一 tick 是否应该真正执行。
     *
     * @param timeScale 1.0 = 正常
     */
    public static boolean shouldTick(Particle particle, double timeScale) {
        if (particle == null) {
            return true;
        }
        if (timeScale >= 1.0D) {
            // 没被减速：顺手清掉时钟，避免 WeakHashMap 里堆积失效条目
            CLOCKS.remove(particle);
            return true;
        }
        Clock clock = CLOCKS.get(particle);
        if (clock == null) {
            clock = new Clock(timeScale);
            CLOCKS.put(particle, clock);
        } else {
            clock.update(timeScale);
        }
        clock.accumulator += clock.timeScale;
        if (clock.accumulator < 1.0D) {
            return false;
        }
        clock.accumulator -= 1.0D;
        return true;
    }

    public static void clear() {
        CLOCKS.clear();
    }

    private static double clamp(double value) {
        return Math.max(MIN_TIME_SCALE, Math.min(1.0D, value));
    }

    private static final class Clock {
        private double timeScale;
        private double accumulator;

        private Clock(double timeScale) {
            this.timeScale = clamp(timeScale);
            // 第一次立刻放行，避免新粒子先卡一下
            this.accumulator = 1.0D - this.timeScale;
        }

        private void update(double newScale) {
            this.timeScale = clamp(newScale);
        }
    }
}
