package com.dsh.cyberware.client;

import com.dsh.cyberware.Cyberware;
import com.dsh.cyberware.client.post.SandevistanPostProcessor;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import com.dsh.cyberware.network.ActivatePayload;
import com.dsh.cyberware.network.BerserkPayload;
import com.dsh.cyberware.network.HackPayload;
import com.dsh.cyberware.network.OverclockPayload;
import com.dsh.cyberware.network.RamPayload;
import com.dsh.cyberware.network.TimeDilationPayload;
import com.dsh.cyberware.registry.ModMenus;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * 客户端专用初始化。
 *
 * <p>只在客户端类加载时引用（主类用 {@code FMLEnvironment.getDist() == Dist.CLIENT} 把关），
 * 否则专用服务器会因为找不到渲染类而崩。
 */
public final class CyberwareClient {

    private CyberwareClient() {
    }

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(CyberwareClient::onRegisterMenuScreens);
        modEventBus.addListener(CyberwareClient::onRegisterPayloads);
        modEventBus.addListener(CyberwareClient::onRegisterClientPayloads);
        modEventBus.addListener(CyberwareClient::onRegisterKeys);

        // 注意：这里**不再**缩放生物动画。
        // 服务端已经把每 tick 的位移按倍率缩回去了，客户端的移动本身就是慢的，
        // 动画会自然跟上；再乘一次只会变成「腿不动却在平移」。
        // 屏幕边缘效果 + 科幻进度条
        NeoForge.EVENT_BUS.addListener(SandevistanOverlay::onRenderGui);
        NeoForge.EVENT_BUS.addListener(SandevistanHud::onRenderGui);
        NeoForge.EVENT_BUS.addListener(BerserkHud::onRenderGui);
        // 脑机超频（t25）：RAM 条 + 全屏特效 + 头顶全息面板 + 生物荧光轮廓
        NeoForge.EVENT_BUS.addListener(RamHud::onRenderGui);
        NeoForge.EVENT_BUS.addListener(OverclockWireframe::onRenderLiving);
        // 快速破解（t29）：折角锁定框 + 上传进度条 + 提示条
        NeoForge.EVENT_BUS.addListener(HackHud::onRenderGui);
        // 斯安威斯坦拖影（只对本地玩家）
        NeoForge.EVENT_BUS.addListener(AfterimageRenderer::onRenderLiving);
        // 投射物时间减缓：客户端这一半 —— 与服务端同相位跳 tick（见类注释）
        NeoForge.EVENT_BUS.addListener(ClientProjectileDilation::onEntityTickPre);
        // 屏幕后处理：边缘径向模糊（强度随激活进度淡入淡出）
        // 后处理的执行点改由 GameRendererMixin 挂在原版那段代码后面
        // 激活时拉大 FOV（速度感）
        NeoForge.EVENT_BUS.addListener(SandevistanPostProcessor::onComputeFov);
        // 每 tick 检查激活键
        NeoForge.EVENT_BUS.addListener(CyberwareClient::onClientTick);
    }

    private static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.CYBERWARE_STATION.get(), CyberwareStationScreen::new);
    }

    private static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(CyberwareKeys.CATEGORY);
        event.register(CyberwareKeys.ACTIVATE);
        // R 键轮盘：本任务（t13）只注册键位；触发逻辑（consumeClick → 打开轮盘）留给 t14。
        event.register(CyberwareKeys.RADIAL);
        // X 键：短按扫描 / 长按快速破解轮盘（t29 新增，t36 改双行为）
        event.register(CyberwareKeys.SCAN);
        // t36：G 键已取消（契约 STEP4 §3 的键位最终形态）—— 脑机超频改成 R 轮盘里的一项，
        // 这里不再注册任何超频键位。
    }

    /**
     * <b>客户端包处理（t25 P0 第一件事）</b> —— 契约 §3.4 点名的交接点。
     *
     * <p>三个包在 {@code CyberwareNetwork} 里只有服务端 handler（{@code RamPayload} 用的是
     * 无 handler 的 {@code playToClient}）—— 不在这里补客户端 handler，客户端收到包会报
     * 「没有 handler」。所有处理都只是<b>更新显示状态</b>，绝不改玩法数据。
     */
    private static void onRegisterClientPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(RamPayload.TYPE,
                (payload, context) -> context.enqueueWork(() -> RamClientState.onRamPayload(payload)));
        event.register(OverclockPayload.TYPE,
                (payload, context) -> context.enqueueWork(() -> RamClientState.onOverclockPayload(payload)));
        event.register(HackPayload.TYPE,
                (payload, context) -> context.enqueueWork(() -> {
                    // t25 的瘫痪红字 + t29 的锁定/上传进度，各处管各处的显示状态
                    RamClientState.onHackPayload(payload);
                    HackClientState.onHackPayload(payload);
                }));
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(
                TimeDilationPayload.TYPE,
                TimeDilationPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientTimeDilation.applyOnClient(
                        payload.durationTicks(), payload.ratio(),
                        payload.x(), payload.y(), payload.z(), payload.radius(), payload.owner())));
        event.registrar("1").playToClient(
                BerserkPayload.TYPE,
                BerserkPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> BerserkClientState.onPayload(
                        payload.remainingTicks(), payload.totalTicks(),
                        payload.damageMultiplier(), payload.active())));
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
            int key = CyberwareKeys.SCAN.getKey().getValue();
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
                if (HackClientState.hasLock()) {
                    HackRadialScreen.openIfLocked(minecraft);
                } else {
                    HackClientState.showNote("NO_TARGET");
                }
            } else if (!down && scanHeld) {
                scanHeld = false;
                if (!scanWheelOpened) {
                    ClientPacketDistributor.sendToServer(HackPayload.scan());
                }
            }
        } catch (Throwable t) {
            if (SCAN_FAIL_LOGGED.compareAndSet(false, true)) {
                Cyberware.LOGGER.warn("[cyberware] X 键状态机异常（已忽略，不影响游戏）", t);
            }
        }
    }

    /** 按键触发 → 发一句请求给服务端（服务端才是有权改数据的一方）。 */
    private static void onClientTick(ClientTickEvent.Post event) {
        while (CyberwareKeys.ACTIVATE.consumeClick()) {
            // V 键 = 手持激活：defId 用 HELD（空串），服务端行为与以前完全一致
            ClientPacketDistributor.sendToServer(new ActivatePayload(ActivatePayload.HELD));
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
