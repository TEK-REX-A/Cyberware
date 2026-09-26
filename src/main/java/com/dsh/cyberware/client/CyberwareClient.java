package com.dsh.cyberware.client;

import com.dsh.cyberware.client.post.SandevistanPostProcessor;
import net.minecraft.client.Minecraft;
import com.dsh.cyberware.network.ActivatePayload;
import com.dsh.cyberware.network.BerserkPayload;
import com.dsh.cyberware.network.TimeDilationPayload;
import com.dsh.cyberware.registry.ModMenus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
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
        modEventBus.addListener(CyberwareClient::onRegisterKeys);

        // 注意：这里**不再**缩放生物动画。
        // 服务端已经把每 tick 的位移按倍率缩回去了，客户端的移动本身就是慢的，
        // 动画会自然跟上；再乘一次只会变成「腿不动却在平移」。
        // 屏幕边缘效果 + 科幻进度条
        NeoForge.EVENT_BUS.addListener(SandevistanOverlay::onRenderGui);
        NeoForge.EVENT_BUS.addListener(SandevistanHud::onRenderGui);
        NeoForge.EVENT_BUS.addListener(BerserkHud::onRenderGui);
        // 斯安威斯坦拖影（只对本地玩家）
        NeoForge.EVENT_BUS.addListener(AfterimageRenderer::onRenderLiving);
        // 投射物时间减缓：客户端这一半 —— 按住本地物理推进，位置只认服务端（见类注释）
        NeoForge.EVENT_BUS.addListener(ClientProjectileDilation::onEntityTickPre);
        NeoForge.EVENT_BUS.addListener(ClientProjectileDilation::onEntityTickPost);
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

    /** 按键触发 → 发一句请求给服务端（服务端才是有权改数据的一方）。 */
    private static void onClientTick(ClientTickEvent.Post event) {
        while (CyberwareKeys.ACTIVATE.consumeClick()) {
            ClientPacketDistributor.sendToServer(new ActivatePayload());
        }
        SandevistanPostProcessor.tick();
        AfterimageTracker.tick();

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            // 换维度/退出世界：这些时钟绑定的都是上一批对象
            ParticleTickClock.clear();
            WeatherTickClock.reset();
            AfterimageTracker.clear();
        } else {
            double timeScale = ClientTimeDilation.timeScaleAt(
                    minecraft.player == null ? 0.0D : minecraft.player.getX(),
                    minecraft.player == null ? 0.0D : minecraft.player.getY(),
                    minecraft.player == null ? 0.0D : minecraft.player.getZ());
            WeatherTickClock.tick((int) minecraft.level.getGameTime(), timeScale);
        }
    }
}
