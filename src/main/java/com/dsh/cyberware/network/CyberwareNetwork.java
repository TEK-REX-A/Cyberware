package com.dsh.cyberware.network;

import com.dsh.cyberware.Cyberware;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** 网络包注册。 */
public final class CyberwareNetwork {

    /** 协议版本号：改了 payload 结构就往上加，避免旧客户端连新服务端出错。 */
    public static final String PROTOCOL_VERSION = "1";

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

    /** 激活手持义体：一切都以服务端手上那份数据为准，客户端只说「我按了键」。 */
    private static void handleActivate(ActivatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> com.dsh.cyberware.core.CyberwareAbilities.activateHeld(context.player()));
    }

    /**
     * 服务端处理。注意 26.x 的 {@code playToServer} 只有带 handler 的重载，
     * 且 handler 必须在主线程执行 —— 所以一律走 {@code enqueueWork}。
     */
    private static void handleAction(CyberwareActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // TODO(第二步): 按 payload.action() 分支处理安装/卸载/升级
            Cyberware.LOGGER.debug("[cyberware] station action: {} slot={} player={}",
                    payload.action(), payload.slotIndex(), context.player().getName().getString());
        });
    }
}
