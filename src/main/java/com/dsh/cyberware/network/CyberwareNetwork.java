package com.dsh.cyberware.network;

import com.dsh.cyberware.core.CyberwareAbilities;
import com.dsh.cyberware.core.CyberwareInstallation;
import com.dsh.cyberware.core.HackSystem;
import com.dsh.cyberware.core.ShieldSystem;
import com.dsh.cyberware.core.OverclockSystem;
import com.dsh.cyberware.core.TimeDilationManager;
import com.dsh.cyberware.event.BerserkHandler;
import com.dsh.cyberware.event.CombatEffectsHandler;
import com.dsh.cyberware.event.RamKillHandler;
import com.dsh.cyberware.event.TimeDilationHandler;
import com.dsh.cyberware.menu.CyberwareStationService;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 网络包注册与事件接线（Fabric 26.1.2）。
 *
 * <p><b>与 NeoForge 版的差异（移植说明）</b>
 * <ol>
 *   <li>没有 {@code RegisterPayloadHandlersEvent}：payload 类型用
 *       {@link PayloadTypeRegistry#serverboundPlay()} / {@link PayloadTypeRegistry#clientboundPlay()} 注册，
 *       并把原来的 playBidirectional 拆成「两个方向各注册一次」；</li>
 *   <li>没有 {@code IPayloadContext#enqueueWork}：{@link ServerPlayNetworking.Context#server()} 拿 MinecraftServer
 *       自己 execute；{@code context.player()} 直接就是 {@link ServerPlayer}，原先的 instanceof 防御不再需要；</li>
 *   <li><b>协议版本协商机制已砍掉</b>（Fabric 侧没有 per-channel 版本协商）。
 *       {@link #PROTOCOL_VERSION} 仅作诊断常量保留，见报告；</li>
 *   <li>事件面从 NeoForge 总线换成 Fabric API 的注册接口（见 {@link #register()}）。</li>
 * </ol>
 */
public final class CyberwareNetwork {

    private static final Logger LOGGER = LoggerFactory.getLogger("cyberware");

    /** 协议版本号：仅用于诊断，不再参与协商（Fabric 无 per-channel 版本机制）。 */
    public static final String PROTOCOL_VERSION = "4";

    private CyberwareNetwork() {
    }

    /** 由主初始化入口调用一次（两端都会执行）。 */
    public static void register() {
        // ---- payload 类型注册：C2S ----
        PayloadTypeRegistry.serverboundPlay().register(CyberwareActionPayload.TYPE, CyberwareActionPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ActivatePayload.TYPE, ActivatePayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(OverclockPayload.TYPE, OverclockPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(HackPayload.TYPE, HackPayload.STREAM_CODEC);

        // ---- payload 类型注册：S2C（原 playBidirectional 的两个包两个方向都注册）----
        // 义体安装表：附件本身不同步（见 ModAttachments / InstallationPayload 注释），只能自建 S2C
        PayloadTypeRegistry.clientboundPlay().register(InstallationPayload.TYPE, InstallationPayload.STREAM_CODEC);
        // 护盾：同一个理由，服务端算完推给本人（HUD 后续任务读这份镜像）
        PayloadTypeRegistry.clientboundPlay().register(ShieldPayload.TYPE, ShieldPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(RamPayload.TYPE, RamPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TimeDilationPayload.TYPE, TimeDilationPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BerserkPayload.TYPE, BerserkPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OverclockPayload.TYPE, OverclockPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(HackPayload.TYPE, HackPayload.STREAM_CODEC);

        // ---- 服务端接收器 ----
        ServerPlayNetworking.registerGlobalReceiver(CyberwareActionPayload.TYPE,
                (CyberwareActionPayload payload, ServerPlayNetworking.Context ctx) ->
                        ctx.server().execute(() -> handleAction(payload, ctx.player())));
        ServerPlayNetworking.registerGlobalReceiver(ActivatePayload.TYPE,
                (ActivatePayload payload, ServerPlayNetworking.Context ctx) ->
                        ctx.server().execute(() -> handleActivate(payload, ctx.player())));
        ServerPlayNetworking.registerGlobalReceiver(OverclockPayload.TYPE,
                (OverclockPayload payload, ServerPlayNetworking.Context ctx) ->
                        ctx.server().execute(() -> handleOverclockToggle(payload, ctx.player())));
        ServerPlayNetworking.registerGlobalReceiver(HackPayload.TYPE,
                (HackPayload payload, ServerPlayNetworking.Context ctx) ->
                        ctx.server().execute(() -> handleHackCast(payload, ctx.player())));

        // ---- 事件接线（等价关系见各 Handler 的类注释）----
        ServerTickEvents.END_LEVEL_TICK.register(CyberwareNetwork::onLevelTickEnd);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
                TimeDilationHandler.allowDamage(entity, source) && BerserkHandler.allowDamage(entity, source));
        ServerLivingEntityEvents.AFTER_DAMAGE.register(BerserkHandler::onAfterDamage);
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            TimeDilationHandler.onLivingDeath(entity, source);
            BerserkHandler.onLivingDeath(entity, source);
            RamKillHandler.onLivingDeath(entity, source);
            // 护盾：死亡清零（附件不 copyOnDeath，这里再显式清一次，管住「不死图腾」与 HUD 残留）
            ShieldSystem.onDeath(entity);
        });
        ServerEntityEvents.ALLOW_LOAD.register((entity, world, reason, loaded) ->
                CombatEffectsHandler.allowLoad(entity));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> CombatEffectsHandler.onServerStopped());
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                CombatEffectsHandler.onPlayerLoggedOut(handler.player));

        // ---- 安装表同步：进服 / 重生 ----
        // 客户端那份「已装义体」是服务端推过去的一份镜像（见 InstallationPayload），
        // 客户端实体每次重建都会丢，所以进服与重生各补推一次；
        // 另有 onLevelTickEnd 里的低频兜底（每 40 刻），保证任何漏掉的时机最多 2 秒自愈。
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            syncInstallation(handler.player);
            ShieldSystem.sync(handler.player);
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            syncInstallation(newPlayer);
            ShieldSystem.sync(newPlayer);
        });

        // ---- 护盾：每刻推进（脱战恢复 + 夹上限 + 初始化灌满）----
        // 放在 END_SERVER_TICK 与 RamSystem 同层：都是「遍历在线玩家逐刻推进」的活。
        ServerTickEvents.END_SERVER_TICK.register(ShieldSystem::onServerTick);

        // ---- 维度广播钩子：替掉 core 里那段反射默认实现 ----
        TimeDilationManager.setSyncHook((level, payload) -> {
            if (!(level instanceof ServerLevel serverLevel)) {
                return;
            }
            for (ServerPlayer player : serverLevel.players()) {
                ServerPlayNetworking.send(player, payload);
            }
        });
    }

    /**
     * 每刻末（每个维度一次）：时缓刷新 + 逐实体位置回拉 + 破解效果到期检查 + 狂暴属性校核
     * + 安装表低频兜底同步。
     */
    private static void onLevelTickEnd(ServerLevel level) {
        TimeDilationHandler.onLevelTickEnd(level);
        CombatEffectsHandler.onLevelTickEnd(level);
        BerserkHandler.onLevelTickEnd(level);

        // 兜底：安装表每 2 秒重推一次（payload 只有几十字节，代价可以忽略）。
        // 存在的理由：附件不自带同步，客户端实体在重生 / 跨维度 / 重连等时机都会重建，
        // 漏一次就是「界面显示没装」。有了这条，任何漏发最多 2 秒自愈。
        if (level.getGameTime() % SYNC_INTERVAL_TICKS == 0L) {
            for (ServerPlayer player : level.players()) {
                syncInstallation(player);
                ShieldSystem.sync(player);   // 护盾镜像同样自愈（HUD 任务会读它）
            }
        }
    }

    /** 安装表兜底同步周期（刻）。 */
    private static final long SYNC_INTERVAL_TICKS = 40L;

    /**
     * 把玩家身上的<b>权威</b>义体表推给该玩家的客户端。
     *
     * <p>任何写入 {@code ModAttachments.INSTALLATION} 之后都必须调一次，否则客户端读到的
     * 还是旧表（甚至空表）—— 这就是 0.5.3 真机「吞义体」的直接原因。
     * 客户端收到后会把这份表写回<b>同一个</b>附件，因此操作台界面、轮盘、
     * 被动效果三处读到的一定是同一份数据。
     */
    public static void syncInstallation(ServerPlayer player) {
        if (player == null || player.connection == null) {
            return;
        }
        try {
            ServerPlayNetworking.send(player, new InstallationPayload(CyberwareInstallation.of(player)));
        } catch (Throwable t) {
            // 发包失败绝不能影响服务端主循环（玩家正在断线时会走到这里）
            LOGGER.warn("[cyberware] 安装表同步失败（{}）", player.getName().getString(), t);
        }
    }

    /** 超频切换请求（C2S）：客户端只表达意图，能不能开由服务端裁决。 */
    private static void handleOverclockToggle(OverclockPayload payload, ServerPlayer player) {
        if (payload.action() != OverclockPayload.Action.TOGGLE) {
            return;
        }
        OverclockSystem.toggle(player);
    }

    /** 破解上位请求（C2S）：按 action 分流，全部在服务端执行。 */
    private static void handleHackCast(HackPayload payload, ServerPlayer player) {
        switch (payload.action()) {
            case CAST -> HackSystem.request(player, payload.targetEntityId(), payload.hackId());
            case SCAN -> HackSystem.scan(player);
            default -> {
                // 下行 action 不该来自客户端
            }
        }
    }

    /** 激活请求：空串=手持激活，非空=按 defId 激活已安装义体（服务端自己查已安装表）。 */
    private static void handleActivate(ActivatePayload payload, ServerPlayer player) {
        if (payload.defId().isEmpty()) {
            CyberwareAbilities.activateHeld(player);
            return;
        }
        CyberwareAbilities.activateInstalled(player, payload.defId());
    }

    /** 操作台动作（安装 / 卸载 / 升级）。 */
    private static void handleAction(CyberwareActionPayload payload, ServerPlayer player) {
        LOGGER.debug("[cyberware] station action: {} slot={} def={} player={}",
                payload.action(), payload.slotIndex(), payload.defId(),
                player.getName().getString());
        switch (payload.action()) {
            case INSTALL -> CyberwareStationService.install(player, payload.containerId(), payload.defId());
            case UNINSTALL -> CyberwareStationService.uninstall(
                    player, payload.containerId(), payload.defId(), payload.slotIndex());
            case UPGRADE -> CyberwareStationService.upgrade(player, payload.containerId(), payload.slotIndex());
        }
    }
}
