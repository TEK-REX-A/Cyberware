package com.dsh.cyberware;

import com.dsh.cyberware.config.CyberwareConfig;
import com.dsh.cyberware.core.RamSystem;
import com.dsh.cyberware.event.BerserkHandler;
import com.dsh.cyberware.event.TimeDilationHandler;
import com.dsh.cyberware.network.CyberwareNetwork;
import com.dsh.cyberware.registry.ModAttachments;
import com.dsh.cyberware.registry.ModBlockEntities;
import com.dsh.cyberware.registry.ModBlocks;
import com.dsh.cyberware.registry.ModComponents;
import com.dsh.cyberware.registry.ModCreativeTabs;
import com.dsh.cyberware.registry.ModItems;
import com.dsh.cyberware.registry.ModMenus;
import com.dsh.cyberware.registry.ModSounds;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cyberware 主类（Fabric 入口）。
 *
 * <p>目标平台：Minecraft 26.1.2 + Fabric。由 {@code CyberwareFabric.onInitialize()} 调用
 * {@link #init()}；客户端专属初始化在 {@code CyberwareClient.initClient()}，
 * 由 {@code CyberwareClientFabric.onInitializeClient()} 调用。
 */
public final class Cyberware {

    public static final String MODID = "cyberware";
    public static final Logger LOGGER = LoggerFactory.getLogger("Cyberware");

    private Cyberware() {
    }

    public static void init() {
        // ---- 注册表（Fabric 是即时注册，顺序有意义：方块先于物品，物品先于创造页）----
        ModBlocks.register();
        ModItems.register();
        ModBlockEntities.register();
        ModMenus.register();
        ModComponents.register();
        ModAttachments.register();
        ModCreativeTabs.register();
        ModSounds.register();

        // ---- 网络包 ----
        CyberwareNetwork.register();

        // ---- 事件 ----
        registerCommonEvents();

        // ---- 配置（零第三方依赖：Properties + config/cyberware.properties）----
        CyberwareConfig.load();

        LOGGER.info("[cyberware] 义体系统已初始化（MC 26.1.2 / Fabric）");
    }

    /**
     * 通用（双端）事件注册。
     *
     * <p>⚠ 这里的方法引用<b>依赖 event/ 目录的新签名</b>（Fabric 回调没有 event 对象）：
     * <ul>
     *   <li>{@code TimeDilationHandler.onEntityTick} → {@code (ServerLevel) void}</li>
     *   <li>{@code TimeDilationHandler.onIncomingDamage} → {@code (LivingEntity, DamageSource, float) boolean}</li>
     *   <li>{@code TimeDilationHandler.onLivingDeath} → {@code (LivingEntity, DamageSource) void}</li>
     *   <li>{@code BerserkHandler.onIncomingDamage} → {@code (LivingEntity, DamageSource, float) boolean}</li>
     *   <li>{@code BerserkHandler.onEntityTick} → {@code (MinecraftServer) void}</li>
     *   <li>{@code BerserkHandler.onLivingDeath} → {@code (LivingEntity, DamageSource) void}</li>
     *   <li>{@code RamSystem.onPlayerTick} → {@code (MinecraftServer) void}</li>
     *   <li>{@code CyberwareCommands.register} → {@code (CommandDispatcher, CommandBuildContext, CommandSelection) void}</li>
     * </ul>
     * 这些签名由事件层负责人对齐；本文件只负责“注册点”的形式。
     *
     * <p>注：投射物跳 tick 不再走事件 —— 已由 {@code mixin/EntityTickMixin} 与
     * {@code mixin/client/ClientEntityTickMixin} 接管。
     */
    private static void registerCommonEvents() {
        ServerTickEvents.END_LEVEL_TICK.register(TimeDilationHandler::onLevelTickEnd);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> TimeDilationHandler.allowDamage(entity, source));
        ServerLivingEntityEvents.AFTER_DEATH.register(TimeDilationHandler::onLivingDeath);

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> BerserkHandler.allowDamage(entity, source));
        ServerTickEvents.END_LEVEL_TICK.register(BerserkHandler::onLevelTickEnd);
        ServerLivingEntityEvents.AFTER_DEATH.register(BerserkHandler::onLivingDeath);

        ServerTickEvents.END_SERVER_TICK.register(RamSystem::onPlayerTick);

        CommandRegistrationCallback.EVENT.register(CyberwareCommands::register);
    }
}
