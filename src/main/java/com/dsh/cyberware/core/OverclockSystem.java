package com.dsh.cyberware.core;

import com.dsh.cyberware.Cyberware;
import com.dsh.cyberware.network.OverclockPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 脑机超频 · 服务端状态机。
 *
 * <p>本类只做**状态**：谁能开（装了网络接入仓、不在冷却）、开多久（时长/冷却由网络接入仓稀有度决定）、
 * 什么时候结束、结束后进冷却，以及把状态同步给客户端。
 * <b>不做任何效果</b>：扫描线/线框/音调那些全是客户端渲染（huddev 的活），
 * 破解效果与消耗是 t25/t26 的活。
 *
 * <h3>时钟</h3>
 * 用 {@link MinecraftServer#getTickCount()}（服务端全局单调计数）而不是
 * {@code Level.getGameTime()} —— 后者每个世界各自从 0 算，跨维度/跨世界会串味（0.3.12 P0 教训）。
 *
 * <h3>服务器重启后的自愈</h3>
 * tick 计数随服务器实例重置为 0；旧存档里可能留着「未来几小时」的时刻。
 * {@link #read} 里的两条规则把这种记录直接作废：
 * ① {@code active} 但「剩余时长」超过占位表最大时长 → 视为已结束（并进入冷却）；
 * ② {@code cooldownUntil} 比「现在 + 最大冷却」还远 → 视为冷却已结束。
 * 判据只依赖占位表上限（10~60 秒级），不会把正常状态误判。
 */
public final class OverclockSystem {

    private OverclockSystem() {
    }

    /**
     * 客户端请求「切换超频」（C2S {@code OverclockPayload.Action.TOGGLE} 的服务端处理）。
     *
     * <p>服务端裁决，客户端说的不算：
     * <ul>
     *   <li>正在超频 → 立刻结束（手动关闭）并进入冷却；</li>
     *   <li>冷却中 → 拒绝（不发包、不改状态，日志留痕）；</li>
     *   <li>没装网络接入仓 → 拒绝；</li>
     *   <li>否则开始超频，时长为该网络接入仓稀有度对应的值。</li>
     * </ul>
     *
     * @return 状态是否真的切换了
     */
    public static boolean toggle(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        long now = serverTicks(player);
        OverclockState state = read(player, now);
        if (state.isActive(now)) {
            OverclockState.set(player, state.endAt(now, CyberwareStats.overclockCooldownTicks(player)));
            sync(player);
            Cyberware.LOGGER.debug("[cyberware] 超频手动关闭：{}", player.getName().getString());
            return true;
        }
        if (state.isCoolingDown(now)) {
            Cyberware.LOGGER.debug("[cyberware] 超频请求被拒（冷却中，还剩 {} 刻）：{}",
                    state.cooldownRemainingTicks(now), player.getName().getString());
            // 被拒也要广播状态：HUD 才能显示「冷却还剩多少」，而不是按了没反应
            sync(player);
            return false;
        }
        if (CyberwareStats.bestCyberdeck(player) == null) {
            Cyberware.LOGGER.debug("[cyberware] 超频请求被拒（没有安装网络接入仓）：{}",
                    player.getName().getString());
            sync(player);
            return false;
        }
        int duration = CyberwareStats.overclockDurationTicks(player);
        OverclockState.set(player, new OverclockState(true, now + duration, state.cooldownUntil()));
        sync(player);
        Cyberware.LOGGER.debug("[cyberware] 超频开启 {} 刻：{}", duration, player.getName().getString());
        return true;
    }

    /**
     * 每刻调用（由 {@link RamSystem#onPlayerTick} 驱动）：到点自动结束并进入冷却。
     *
     * @return 状态是否发生了变化（用于决定要不要同步）
     */
    public static boolean tick(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        long now = serverTicks(player);
        OverclockState state = read(player, now);
        if (!state.isActive(now)) {
            return false;
        }
        OverclockState.set(player, state.endAt(now, CyberwareStats.overclockCooldownTicks(player)));
        sync(player);
        Cyberware.LOGGER.debug("[cyberware] 超频到时结束：{}", player.getName().getString());
        return true;
    }

    /** 把当前状态发给本人（HUD 用）。 */
    public static void sync(ServerPlayer player) {
        if (player == null) {
            return;
        }
        long now = serverTicks(player);
        OverclockState state = read(player, now);
        PacketDistributor.sendToPlayer(player, new OverclockPayload(
                OverclockPayload.Action.STATE,
                state.isActive(now),
                state.remainingTicks(now),
                CyberwareStats.overclockDurationTicks(player),
                state.cooldownRemainingTicks(now),
                CyberwareStats.overclockCooldownTicks(player)));
    }

    /** 服务端全局单调 tick 计数（换维度不影响，重启归零）。 */
    public static long serverTicks(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        return server == null ? 0L : server.getTickCount();
    }

    /**
     * 读取状态并做「时间基准变了」的自愈（见类注释）。
     *
     * <p>自愈结果会写回附件（一次 {@code setData}），所以下次读到的就是干净状态。
     */
    private static OverclockState read(ServerPlayer player, long now) {
        OverclockState state = OverclockState.of(player);
        long maxFuture = now + CyberwareStats.maxOverclockTicks();
        if (state.active() && state.expiresAt() > maxFuture) {
            // 存档里的结束时刻超过「现在 + 表里最大时长」→ 只能是上一次服务器运行的 tick 基准
            Cyberware.LOGGER.debug("[cyberware] 超频状态基准失效，作废：{}（expiresAt={} now={}）",
                    player.getName().getString(), state.expiresAt(), now);
            OverclockState healed = state.endAt(now, CyberwareStats.overclockCooldownTicks(player));
            OverclockState.set(player, healed);
            return healed;
        }
        if (!state.active() && state.cooldownUntil() > maxFuture) {
            Cyberware.LOGGER.debug("[cyberware] 超频冷却基准失效，作废：{}（cooldownUntil={} now={}）",
                    player.getName().getString(), state.cooldownUntil(), now);
            OverclockState healed = new OverclockState(false, state.expiresAt(), 0L);
            OverclockState.set(player, healed);
            return healed;
        }
        return state;
    }
}
