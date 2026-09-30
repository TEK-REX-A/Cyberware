package com.dsh.cyberware.core;

import com.dsh.cyberware.network.RamPayload;
import com.dsh.cyberware.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * RAM System · 服务端推进器（每刻跑一次，只服务端）。
 *
 * <p>三件事：
 * <ol>
 *   <li><b>初始化</b>：第一次见到该玩家时把 RAM 灌满到上限（{@link CyberwareStats#maxRam}）；</li>
 *   <li><b>恢复</b>：每 {@value #SETTLE_INTERVAL_TICKS} 刻结算一次，
 *       加上 {@code regenPerMinute / 60}（20 刻 = 1 秒），夹在上限以内；</li>
 *   <li><b>同步</b>：数值真的变了才发包；超频期间每 20 刻补一次，防止客户端倒计时漂移。</li>
 * </ol>
 *
 * <p><b>为什么是「每 20 刻结算」而不是每刻累加</b>：RAM 存在玩家附件里，而
 * {@code setData} 每次都会触发一次附件同步包 —— 每刻写一次就是每秒 20 个包。每秒结算一次
 * 把包量压到 1 个/秒，HUD 的平滑由客户端自己插值（HUD 契约见 {@link RamPayload}）。
 *
 * <p><b>扣费判定不在客户端</b>：本类只在服务端跑（{@code PlayerTickEvent.Post} +
 * {@code ServerPlayer} 判定），客户端连 {@link RamState} 的权威副本都没有 —— 它只有同步下来的镜像。
 *
 * <p><b>注册方式</b>：由 {@code registry/ModAttachments} 的静态块显式
 * {@code NeoForge.EVENT_BUS.addListener(RamSystem::onPlayerTick)} ——
 * 不用 {@code @EventBusSubscriber} 注解，因为注解漏扫会**静默失效**（这个项目的血泪教训：
 * 静默 = 主人真机才发现）。显式一行在代码里能看见。
 */
public final class RamSystem {

    /** 结算间隔：20 刻 = 1 秒。 */
    public static final int SETTLE_INTERVAL_TICKS = 20;

    /** 超频期间的重同步间隔（防客户端倒计时漂移）。 */
    private static final int OVERCLOCK_RESYNC_TICKS = 20;

    private RamSystem() {
    }

    /** 服务端每刻入口（注册见类注释）。 */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        boolean changed = settleRam(player);
        changed |= OverclockSystem.tick(player);
        HackSystem.tick(player);   // 上传按刻计时（t24）
        if (changed || shouldResyncOverclock(player)) {
            sync(player);
        }
    }

    /** 够不够付（服务端判定；不足时调用方走「濒死超频扣血」或拒绝，见 {@link HackSystem#pay}）。 */
    public static boolean canPay(ServerPlayer player, double cost) {
        return player != null && cost > 0.0D && RamState.of(player).current() >= cost;
    }

    /**
     * 扣 RAM（t24 补上的入口 —— 契约 §8.5 点名的缺口）。
     *
     * <p>成功扣费会**立刻同步**：HUD 不用等下一次每秒结算就能看到 RAM 平滑回退。
     *
     * @return 是否扣成功（不够就返回 false 且不改任何状态 —— 调用方自己决定扣血还是拒绝）
     */
    public static boolean spend(ServerPlayer player, double cost) {
        if (player == null || cost <= 0.0D) {
            return false;
        }
        double current = RamState.of(player).current();
        if (current < cost) {
            return false;
        }
        RamState.set(player, current - cost);
        sync(player);
        return true;
    }

    /**
     * 加 RAM（击杀回 RAM 等来源；0.5.1 新增）。
     *
     * <p>夹在 {@code [0, maxRam]}：满了不溢出；加完立刻同步（HUD 要看到那一跳）。
     *
     * @return 实际加了多少（被上限截断时小于 {@code amount}）
     */
    public static double grant(ServerPlayer player, double amount) {
        if (player == null || amount <= 0.0D) {
            return 0.0D;
        }
        double max = CyberwareStats.maxRam(player);
        double current = RamState.of(player).current();
        double next = Math.min(max, Math.max(0.0D, current + amount));
        if (next == current) {
            return 0.0D;
        }
        RamState.set(player, next);
        sync(player);
        return next - current;
    }

    /** 把玩家当前的 RAM/超频状态发给本人（HUD 契约）。 */
    public static void sync(ServerPlayer player) {
        if (player == null) {
            return;
        }
        double max = CyberwareStats.maxRam(player);
        double current = RamState.of(player).current();
        boolean overclock = OverclockState.of(player).isActive(OverclockSystem.serverTicks(player));
        PacketDistributor.sendToPlayer(player, new RamPayload(
                current,
                max,
                CyberwareStats.regenPerMinute(player),
                overclock,
                current <= 0.0D));
    }

    /**
     * 结算 RAM：初始化 / 夹上限 / 每秒恢复。
     *
     * @return 是否写入了新的 RAM 值（= 需要同步）
     */
    private static boolean settleRam(ServerPlayer player) {
        double max = CyberwareStats.maxRam(player);

        if (!RamState.has(player)) {
            // 第一次见到该玩家：灌满到上限。
            // 上限 = max(8, Σ Stats.RAM)（邮件IX §二「默认上限 8」），所以裸机也会拿到 8 点，不是 0。
            RamState.set(player, max);
            return true;
        }

        double current = RamState.of(player).current();
        if (current > max) {
            // 上限变小了（卸了加 RAM 的义体）：立刻夹住，不凭空多出来
            RamState.set(player, max);
            return true;
        }
        if (current >= max || player.tickCount % SETTLE_INTERVAL_TICKS != 0) {
            return false;
        }
        double perSecond = CyberwareStats.regenPerMinute(player) / 60.0D;
        if (perSecond == 0.0D) {
            // 恢复速率为 0 的唯一途径：把 CyberwareStats.BASE_RAM_REGEN 调成 0（裸机涓流关掉）
            // **且**没装任何给 RAM_REGEN 的接入仓/配平 —— 两种情形叠加才会走到这里。
            // 裸机默认是 1.0/分钟（BASE_RAM_REGEN），所以「没接入仓」本身不再等于「不恢复」。
            // TODO(主人裁决: BASE_RAM_REGEN 的数值可调，0 也合法 —— 那时本分支就是「完全不恢复」的语义)
            return false;
        }
        double next = Math.min(max, Math.max(0.0D, current + perSecond));
        if (next == current) {
            return false;
        }
        RamState.set(player, next);
        return true;
    }

    /** 超频进行中时每 20 刻补一次同步。 */
    private static boolean shouldResyncOverclock(ServerPlayer player) {
        if (player.tickCount % OVERCLOCK_RESYNC_TICKS != 0) {
            return false;
        }
        return OverclockState.of(player).isActive(OverclockSystem.serverTicks(player));
    }
}
