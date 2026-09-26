package com.dsh.cyberware.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * 残影快照。
 *
 * <p>每 tick 抓一份**本地玩家**的状态，维护一个最近 N 份的队列，渲染时按
 * 「越老的越暗、颜色越靠后」画出去 —— 这就是 A.txt 里说的洋葱皮。
 *
 * <p>记录的东西照着 A.txt 第 1 条来：位置、身体 Yaw、头部 Pitch、
 * 四肢摆动（{@code walkAnimationPos}）与摆动幅度（{@code walkAnimationSpeed}）、
 * 以及存活刻数 {@code ageInTicks}。少一样，残影的动作就会「跟着本体一起动」。
 *
 * <p>只对自己 —— 给敌人做只是给敌人加特效，没意义。
 */
public final class AfterimageTracker {

    /** 保留多少份快照（越多拖得越长） */
    private static final int MAX_SNAPSHOTS = 48;
    /** 采集间隔（tick）：1 = 每刻一份 */
    private static final int SAMPLE_INTERVAL = 1;

    private static final Deque<Snapshot> SNAPSHOTS = new ArrayDeque<>();
    private static int lastGameTime = Integer.MIN_VALUE;

    private AfterimageTracker() {
    }

    /** 一份快照：只存画一个「影子」需要的东西。 */
    public record Snapshot(double x, double y, double z,
                           float bodyRot, float yRot, float xRot,
                           float walkPos, float walkSpeed, float age) {
    }

    /** 每客户端 tick 调一次。 */
    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            SNAPSHOTS.clear();
            lastGameTime = Integer.MIN_VALUE;
            return;
        }
        int now = (int) minecraft.level.getGameTime();
        if (now == lastGameTime) {
            return;
        }
        lastGameTime = now;
        if (now % SAMPLE_INTERVAL != 0) {
            return;
        }

        SNAPSHOTS.addFirst(new Snapshot(
                player.getX(), player.getY(), player.getZ(),
                player.yBodyRot, player.getYRot(), player.getXRot(),
                player.walkAnimation.position(), player.walkAnimation.speed(),
                player.tickCount));

        while (SNAPSHOTS.size() > MAX_SNAPSHOTS) {
            SNAPSHOTS.removeLast();
        }
    }

    /** 从新到旧的快照列表（第 0 份 ≈ 当前位置）。 */
    public static List<Snapshot> snapshots() {
        return new ArrayList<>(SNAPSHOTS);
    }

    public static void clear() {
        SNAPSHOTS.clear();
        lastGameTime = Integer.MIN_VALUE;
    }
}
