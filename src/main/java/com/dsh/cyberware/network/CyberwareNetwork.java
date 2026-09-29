package com.dsh.cyberware.network;

import com.dsh.cyberware.Cyberware;
import com.dsh.cyberware.core.CyberwareAbilities;
import com.dsh.cyberware.menu.CyberwareStationService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** 网络包注册。 */
public final class CyberwareNetwork {

    /**
     * 协议版本号：改了 payload 结构就往上加，避免旧客户端连新服务端出错。
     *
     * <p>0.3.12 {@code CyberwareActionPayload} 多了 {@code defId} → {@code "2"}；
     * 随后 {@code ActivatePayload} 从无参 record 改成带 {@code defId} 的 record → 升到 {@code "3"}。
     * 注意 NeoForge 的版本号是**按 channel（payload）**协商的（见 {@code NetworkPayloadSetup}），
     * 所以客户端 {@code CyberwareClient} 里那两个 S2C 包的 {@code "1"} 不受影响。
     */
    public static final String PROTOCOL_VERSION = "3";

    private CyberwareNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        // 客户端 → 服务端：操作台动作（安装/卸载/升级）
        registrar.playToServer(
                CyberwareActionPayload.TYPE,
                CyberwareActionPayload.STREAM_CODEC,
                CyberwareNetwork::handleAction);

        // 客户端 → 服务端：按下义体激活键
        registrar.playToServer(
                ActivatePayload.TYPE,
                ActivatePayload.STREAM_CODEC,
                CyberwareNetwork::handleActivate);
    }

    /**
     * 激活请求：按 {@code defId} 分流。
     *
     * <ul>
     *   <li>空串（{@link ActivatePayload#HELD}）→ 手持激活，行为与 0.3.11 完全一致（V 键）；</li>
     *   <li>非空 → 已安装表激活（R 键轮盘，t14）。</li>
     * </ul>
     *
     * <p><b>客户端不可信</b>：{@code defId} 只说明「想激活哪一个」，
     * 服务端不会因为客户端说了就照办 —— 是否真的装了那件义体，
     * 由 {@link CyberwareAbilities#activateInstalled} 查**服务端的**已安装附件来裁决。
     * 这里再挡一道非 {@link ServerPlayer}，保证客户端实体上的同步副本永远不会成为执行依据。
     */
    private static void handleActivate(ActivatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                Cyberware.LOGGER.debug("[cyberware] 忽略非服务端玩家的激活请求：{}", payload.defId());
                return;
            }
            if (payload.defId().isEmpty()) {
                // 手持激活（V 键）：看主手物品，行为不变
                CyberwareAbilities.activateHeld(serverPlayer);
                return;
            }
            // 指定型号激活（R 键轮盘）：服务端自己查已安装表
            CyberwareAbilities.activateInstalled(serverPlayer, payload.defId());
        });
    }

    /**
     * 服务端处理操作台的安装 / 卸载 / 升级请求。
     *
     * <p>注意 26.x 的 {@code playToServer} 只有带 handler 的重载，
     * 且 handler 必须在主线程执行 —— 所以一律走 {@code enqueueWork}。
     *
     * <p>这里只负责派发：规则与数据变更都在 {@link CyberwareStationService} 里，
     * 和原版按钮路径（{@code CyberwareStationMenu#clickMenuButton}）共用同一份实现。
     * 非 {@link ServerPlayer} 一律直接返回 —— 客户端永远没有改数据的权限。
     */
    private static void handleAction(CyberwareActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }
            Cyberware.LOGGER.debug("[cyberware] station action: {} slot={} def={} player={}",
                    payload.action(), payload.slotIndex(), payload.defId(),
                    serverPlayer.getName().getString());
            switch (payload.action()) {
                case INSTALL -> CyberwareStationService.install(
                        serverPlayer, payload.containerId(), payload.slotIndex());
                case UNINSTALL -> CyberwareStationService.uninstall(
                        serverPlayer, payload.containerId(), payload.defId(), payload.slotIndex());
                case UPGRADE -> CyberwareStationService.upgrade(
                        serverPlayer, payload.containerId(), payload.slotIndex());
            }
        });
    }
}
