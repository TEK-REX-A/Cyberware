package com.dsh.cyberware;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cyberware · Fabric 入口（26.1.2）。
 *
 * <p>这是移植的新入口。原来的 {@code Cyberware} 是 NeoForge 的 {@code @Mod}
 * 主类（依赖 IEventBus / ModContainer / DeferredRegister），Fabric 侧需要
 * 单独的 ModInitializer。
 *
 * <p>迁移进行中：注册表（DeferredRegister → Registry.register）与事件层
 * （NeoForge.EVENT_BUS → Fabric 事件）尚未接线，当前只做启动标记。
 */
public class CyberwareFabric implements ModInitializer {

    public static final String MODID = "cyberware";
    public static final Logger LOGGER = LoggerFactory.getLogger("Cyberware");

    @Override
    public void onInitialize() {
        LOGGER.info("[Cyberware] Fabric 入口已加载（迁移进行中）");
        Cyberware.init();
    }
}
