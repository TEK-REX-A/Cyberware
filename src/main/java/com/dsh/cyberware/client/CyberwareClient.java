package com.dsh.cyberware.client;

import com.dsh.cyberware.Cyberware;
import com.dsh.cyberware.client.post.SandevistanPostProcessor;
import com.dsh.cyberware.core.Attachments;
import com.dsh.cyberware.core.ShieldState;
import com.dsh.cyberware.registry.ModAttachments;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import com.dsh.cyberware.network.ActivatePayload;
import com.dsh.cyberware.network.BerserkPayload;
import com.dsh.cyberware.network.HackPayload;
import com.dsh.cyberware.network.InstallationPayload;
import com.dsh.cyberware.network.OverclockPayload;
import com.dsh.cyberware.network.RamPayload;
import com.dsh.cyberware.network.ShieldPayload;
import com.dsh.cyberware.network.TimeDilationPayload;
import com.dsh.cyberware.registry.ModMenus;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

/**
 * 客户端专用初始化。
 *
 * <p>只在客户端类加载时引用（主类用 {@code FMLEnvironment.getDist() == Dist.CLIENT} 把关），
 * 否则专用服务器会因为找不到渲染类而崩。
 */
public final class CyberwareClient {

    private CyberwareClient() {
    }

    /**
     * 客户端初始化（由 {@code CyberwareClientFabric.onInitializeClient()} 调用）。
     *
     * <p>⚠ <b>并行迁移中间态</b>：本文件只保留“入口 + 能确定的注册”，
     * 渲染层与网络层的注册点已移出，交给各自负责人接线（见方法内 TODO）。
     */
    public static void initClient() {
        // ---- 每 tick 检查激活键 ----
        ClientTickEvents.END_CLIENT_TICK.register(CyberwareClient::onClientTick);

        // ---- 按键绑定 ----
        registerKeys();

        // ---- 菜单界面 ----
        ScreenRegistrar.register(ModMenus.CYBERWARE_STATION.get(), CyberwareStationScreen::new);

        // ---- 数据包接收（Fabric 版）----
        // payload 类型已在 CyberwareNetwork.register() 里用 clientboundPlay 注册过，
        // 这里只挂接收器；重复注册类型会抛 already registered，别再来一次。
        // 全部走 ctx.client().execute(...)：receive() 在网络线程上，客户端状态只能在主线程改。
        // ★ 义体安装表（0.5.3 真机「吞义体」的修复端）
        // 为什么需要它：ModAttachments.INSTALLATION 没有 .syncWith(...)（Fabric 附件同步在 Android
        // 上会因 netty 反射炸），所以服务端写进去的数据客户端根本拿不到。这里把服务端推来的权威表
        // 写回同一个附件 —— 之后操作台界面、轮盘、被动效果读到的都是这一份，天然对齐。
        ClientPlayNetworking.registerGlobalReceiver(InstallationPayload.TYPE,
                (InstallationPayload payload, ClientPlayNetworking.Context ctx) ->
                        ctx.client().execute(() -> applyInstallation(payload)));
        // ★ 护盾（同一套做法：服务端推、客户端写回同一个附件）
        // 后续的右下角护盾条直接读 ShieldState.of(minecraft.player)，不用再关心网络层。
        ClientPlayNetworking.registerGlobalReceiver(ShieldPayload.TYPE,
                (ShieldPayload payload, ClientPlayNetworking.Context ctx) ->
                        ctx.client().execute(() -> applyShield(payload)));
        ClientPlayNetworking.registerGlobalReceiver(RamPayload.TYPE,
                (RamPayload payload, ClientPlayNetworking.Context ctx) ->
                        ctx.client().execute(() -> RamClientState.onRamPayload(payload)));
        ClientPlayNetworking.registerGlobalReceiver(OverclockPayload.TYPE,
                (OverclockPayload payload, ClientPlayNetworking.Context ctx) ->
                        ctx.client().execute(() -> RamClientState.onOverclockPayload(payload)));
        ClientPlayNetworking.registerGlobalReceiver(HackPayload.TYPE,
                (HackPayload payload, ClientPlayNetworking.Context ctx) ->
                        ctx.client().execute(() -> {
                            // 瘫痪红字（t25）与锁定/上传进度（t29）各管各的显示状态
                            RamClientState.onHackPayload(payload);
                            HackClientState.onHackPayload(payload);
                        }));
        ClientPlayNetworking.registerGlobalReceiver(TimeDilationPayload.TYPE,
                (TimeDilationPayload payload, ClientPlayNetworking.Context ctx) ->
                        ctx.client().execute(() -> ClientTimeDilation.applyOnClient(
                                payload.durationTicks(), payload.ratio(),
                                payload.x(), payload.y(), payload.z(), payload.radius(), payload.owner())));
        ClientPlayNetworking.registerGlobalReceiver(BerserkPayload.TYPE,
                (BerserkPayload payload, ClientPlayNetworking.Context ctx) ->
                        ctx.client().execute(() -> BerserkClientState.onPayload(
                                payload.remainingTicks(), payload.totalTicks(),
                                payload.damageMultiplier(), payload.active())));

        // ---- HUD（fabric-rendering-v1 的 hud 层，替代原 5 处 RenderGuiEvent.Post）----
        // 每个 HUD 用独立 id，按叠加顺序注册：先底层特效、再状态条、最后提示框。
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(Cyberware.MODID, "sandevistan_overlay"),
                SandevistanOverlay::extractRenderState);
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(Cyberware.MODID, "sandevistan_hud"),
                SandevistanHud::extractRenderState);
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(Cyberware.MODID, "berserk_hud"),
                BerserkHud::extractRenderState);
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(Cyberware.MODID, "ram_hud"),
                RamHud::extractRenderState);
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(Cyberware.MODID, "hack_hud"),
                HackHud::extractRenderState);

        // 仍未接线（不属本次改动）：ComputeFovModifierEvent（SandevistanPostProcessor.onComputeFov）。
        // 投射物跳 tick 由 mixin/client/ClientEntityTickMixin 接管；
        // 生物轮廓/拖影由 mixin/client/LivingEntityRendererMixin 接管（该文件尚待接线）。
    }

    /**
     * 把服务端同步下来的权威安装表写回本机玩家实体上的附件。
     *
     * <p>整个方法包 try/catch：这里是网络回调，抛异常会直接把客户端带崩。
     * 写失败也不致命 —— 服务端每 2 秒兜底重推一次（见 {@code CyberwareNetwork#onLevelTickEnd}）。
     */
    private static void applyInstallation(InstallationPayload payload) {
        try {
            if (payload == null) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft == null || minecraft.player == null) {
                // 玩家实体还没建好（进服 / 重生的那一瞬）：等下一次同步
                return;
            }
            var type = ModAttachments.INSTALLATION.get();
            if (type == null) {
                return;
            }
            Attachments.set(Attachments.of(minecraft.player), type, payload.installation());
        } catch (Throwable t) {
            Cyberware.LOGGER.warn("[cyberware] 安装表同步写入失败（已忽略，等待服务端重推）", t);
        }
    }

    /**
     * 把服务端同步下来的护盾快照写回本机玩家实体上的附件。
     *
     * <p>同 {@link #applyInstallation}：包了 try/catch，写失败由服务端的低频重推兜底。
     */
    private static void applyShield(ShieldPayload payload) {
        try {
            if (payload == null) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft == null || minecraft.player == null) {
                return;
            }
            var type = ModAttachments.SHIELD.get();
            if (type == null) {
                return;
            }
            Attachments.set(Attachments.of(minecraft.player), type, payload.toState());
        } catch (Throwable t) {
            Cyberware.LOGGER.warn("[cyberware] 护盾同步写入失败（已忽略，等待服务端重推）", t);
        }
    }

    private static void registerKeys() {
        KeyMappingHelper.registerKeyMapping(CyberwareKeys.ACTIVATE);
        KeyMappingHelper.registerKeyMapping(CyberwareKeys.RADIAL);
        KeyMappingHelper.registerKeyMapping(CyberwareKeys.SCAN);
    }

    /**
     * 上一次 tick 时所在的世界实例。
     *
     * <p>用来发现「换世界」。**不能只看 {@code level == null}**：从 A 世界直接进 B 世界时
     * 引用换了、中间并没有经过 null（退出到标题 → 新建世界才会经过 null）。
     */
    private static Level lastLevel;

    /** 保命日志去重（t34 / inspector F1）：每 tick 的锁定计算抛异常只记一次，绝不刷屏 */
    private static final java.util.concurrent.atomic.AtomicBoolean LOCK_FAIL_LOGGED =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    /** X 键长按阈值（契约 STEP4 §3）：按住满这么久就立刻打开快速破解轮盘 */
    private static final long SCAN_HOLD_MS = 300L;
    /** X 键状态机的保命日志去重（同 LOCK_FAIL_LOGGED 的写法） */
    private static final java.util.concurrent.atomic.AtomicBoolean SCAN_FAIL_LOGGED =
            new java.util.concurrent.atomic.AtomicBoolean(false);
    /** X 键是否处于「按住」状态（看物理键，不看 KeyMapping） */
    private static boolean scanHeld;
    /** 本次按住的起始毫秒 */
    private static long scanPressMs;
    /** 本次按住是否已经开过破解轮盘 → 松手时不再发扫描（两种行为不会同时触发） */
    private static boolean scanWheelOpened;

    /**
     * X 键双行为（t36，契约 STEP4 §3）：<b>短按 = 扫描，长按 = 快速破解轮盘</b>。
     *
     * <p>状态机看的是<b>物理按键</b>（{@code InputConstants.isKeyDown}），所以：
     * <ul>
     *   <li>按下 → 记时刻；</li>
     *   <li>按住满 {@link #SCAN_HOLD_MS} → <b>立刻</b>打开破解轮盘，并置
     *       {@code scanWheelOpened}（松手时不再发扫描 —— 两条路互斥）；</li>
     *   <li>松开且没开过轮盘 → 发一次 {@code HackPayload.scan()}。</li>
     * </ul>
     * 长按时若没有锁定目标，<b>不开发空盘</b>，只给一条「无锁定目标」提示 ——
     * 且仍然算「已处理」，松手不会补发扫描（避免「长按没反应却突然扫描」）。
     */
    private static void pollScanKey() {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            var window = minecraft.getWindow();
            int key = KeyMappingKeys.getValue(CyberwareKeys.SCAN);
            // 清掉 KeyMapping 的 click 计数：这套状态机看物理键，click 留着只会堆积
            while (CyberwareKeys.SCAN.consumeClick()) {
                // 只为清计数
            }
            boolean down = window != null && window.handle() != 0L
                    && InputConstants.isKeyDown(window, key);
            long now = System.currentTimeMillis();
            if (down && !scanHeld) {
                scanHeld = true;
                scanPressMs = now;
                scanWheelOpened = false;
            } else if (down && scanHeld && !scanWheelOpened && now - scanPressMs >= SCAN_HOLD_MS) {
                scanWheelOpened = true;
                if (!HackClientState.hasOptics()) {
                    // 锁定框已被义眼前置挡住（hasLock() 恒 false），此时再报 NO_TARGET 会误导：
                    // 真正缺的是义眼，不是目标。
                    HackClientState.showNote("NO_KIROSHI");
                } else if (HackClientState.hasLock()) {
                    HackRadialScreen.openIfLocked(minecraft);
                } else {
                    HackClientState.showNote("NO_TARGET");
                }
            } else if (!down && scanHeld) {
                scanHeld = false;
                if (!scanWheelOpened) {
                    ClientPlayNetworking.send(HackPayload.scan());
                }
            }
        } catch (Throwable t) {
            if (SCAN_FAIL_LOGGED.compareAndSet(false, true)) {
                Cyberware.LOGGER.warn("[cyberware] X 键状态机异常（已忽略，不影响游戏）", t);
            }
        }
    }

    /** 按键触发 → 发一句请求给服务端（服务端才是有权改数据的一方）。 */
    private static void onClientTick(Minecraft client) {
        while (CyberwareKeys.ACTIVATE.consumeClick()) {
            // V 键 = 手持激活：defId 用 HELD（空串），服务端行为与以前完全一致
            ClientPlayNetworking.send(new ActivatePayload(ActivatePayload.HELD));
        }
        // R 键 = 义体轮盘（t36 起还有「脑机超频」一项，契约 STEP4 §3）。
        // 注意：t36 取消了「有锁定目标 → 破解轮盘」的上下文切换 ——
        // 破解轮盘现在归 X 长按（见下方），R 只做义体轮盘这一件事。
        while (CyberwareKeys.RADIAL.consumeClick()) {
            CyberwareRadialScreen.openOrHint(Minecraft.getInstance());
        }
        // X 键双行为（t36，契约 STEP4 §3）：短按(<300ms)发扫描；按住满 300ms 立刻开破解轮盘。
        pollScanKey();

        Minecraft minecraft = Minecraft.getInstance();

        // ⚠ 换世界检测必须在**任何按世界时间轴做事的调用之前**（包括下面的 post 后处理 tick）：
        //   客户端这几份静态状态全都记绝对游戏刻（endTick / pulseStartTick / lastRealTick），
        //   新世界的 gameTime 从 0 重算 —— 旧账于是变成「几万秒后才结束」。
        //   真机 P0 现场：新建世界后 HUD 直接显示 Sandevistan 16181.0s / Berserk 16059.7s。
        if (minecraft.level != lastLevel) {
            lastLevel = minecraft.level;
            ClientTimeDilation.clear();
            BerserkClientState.clear();
            SandevistanPostProcessor.resetWorldState();
            ParticleTickClock.clear();
            WeatherTickClock.reset();
            AfterimageHistory.clear();
            // 脑机超频：绝对时间戳（警告/瘫痪/激活/撕裂窗口）必须清，否则新世界拿旧账渲染
            RamClientState.clear();
            // 快速破解（t29）：锁定目标 id 与上传进度/提示时间戳同样必须清
            HackClientState.clear();
        }

        // 脑机超频：低 RAM 边沿检测 + 音效/粒子调度（全部限频，见 RamHud）
        RamClientState.tick();
        RamHud.tick();
        // 快速破解：每 tick 重算一次锁定目标（屏幕中心 10% / ≤20 格 / 最近活体）
        // 保命壳（t34 / inspector F1）：这是每 tick 回调，抛异常会当场崩客户端 ——
        // 整段 try/catch，异常只记一次日志（不刷屏）。行为与 t29 完全一致，只多了一层壳。
        try {
            HackClientState.tickLock();
        } catch (Throwable t) {
            if (LOCK_FAIL_LOGGED.compareAndSet(false, true)) {
                Cyberware.LOGGER.warn("[cyberware] 锁定目标计算异常（已忽略，不影响游戏）", t);
            }
        }

        SandevistanPostProcessor.tick();

        Level level = minecraft.level;
        if (level == null) {
            // 退出世界：没有世界可以推进（上面的换世界分支已经清过一轮）
            return;
        }
        double timeScale = ClientTimeDilation.timeScaleAt(
                minecraft.player == null ? 0.0D : minecraft.player.getX(),
                minecraft.player == null ? 0.0D : minecraft.player.getY(),
                minecraft.player == null ? 0.0D : minecraft.player.getZ());
        WeatherTickClock.tick((int) level.getGameTime(), timeScale);
    }
}
