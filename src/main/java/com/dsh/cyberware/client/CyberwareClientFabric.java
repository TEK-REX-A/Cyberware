package com.dsh.cyberware.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cyberware · Fabric 客户端入口（26.1.2）。
 *
 * <p>迁移进行中：屏幕注册、HUD、渲染层尚未接线。
 */
public class CyberwareClientFabric implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("Cyberware/Client");

    @Override
    public void onInitializeClient() {
        LOGGER.info("[Cyberware] Fabric 客户端入口已加载（迁移进行中）");
        CyberwareClient.initClient();
    }
}
