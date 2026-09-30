package com.dsh.cyberware;

import com.dsh.cyberware.client.CyberwareClient;
import com.dsh.cyberware.event.BerserkHandler;
import com.dsh.cyberware.event.TimeDilationHandler;
import com.dsh.cyberware.config.CyberwareConfig;
import com.dsh.cyberware.network.CyberwareNetwork;
import com.dsh.cyberware.registry.ModAttachments;
import com.dsh.cyberware.registry.ModBlockEntities;
import com.dsh.cyberware.registry.ModBlocks;
import com.dsh.cyberware.registry.ModComponents;
import com.dsh.cyberware.registry.ModCreativeTabs;
import com.dsh.cyberware.registry.ModItems;
import com.dsh.cyberware.registry.ModMenus;
import com.dsh.cyberware.registry.ModSounds;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cyberware —— 赛博朋克风格义体植入系统。
 *
 * <p>目标平台：Minecraft 26.1.2 + NeoForge 26.1.2.109 + Java 25。
 */
@Mod(Cyberware.MODID)
public class Cyberware {

    public static final String MODID = "cyberware";
    public static final Logger LOGGER = LoggerFactory.getLogger("Cyberware");

    public Cyberware(IEventBus modEventBus, ModContainer modContainer) {
        // ---- 注册表 ----
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModComponents.COMPONENTS.register(modEventBus);
        ModAttachments.ATTACHMENTS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        // 音效（t38）：斯安威斯坦开启音效 —— 必须挂 mod 事件总线，挂错会静默不注册
        ModSounds.SOUNDS.register(modEventBus);

        // ---- 网络包 ----
        modEventBus.addListener(CyberwareNetwork::register);

        // 时间减缓 · 生物：位置回拉 —— 照常 tick、刻末把位移按倍率缩回
        NeoForge.EVENT_BUS.addListener(TimeDilationHandler::onEntityTick);
        // 时间减缓 · 投射物：跳 tick —— 时间真·不流逝（位置回拉对箭矢的碰撞/落点不成立）
        NeoForge.EVENT_BUS.addListener(TimeDilationHandler::onEntityTickPre);
        // 子弹时间：激活期间免疫弹射物伤害
        NeoForge.EVENT_BUS.addListener(TimeDilationHandler::onIncomingDamage);
        // 击杀延长：减速期间击杀敌人 → 持续时间往后推
        NeoForge.EVENT_BUS.addListener(TimeDilationHandler::onLivingDeath);
        // 狂暴：无敌 + 伤害翻倍 + 击杀延长 + 击杀治疗
        NeoForge.EVENT_BUS.addListener(BerserkHandler::onIncomingDamage);
        // 狂暴：攻击速度加快（每 tick 校核属性修饰符）
        NeoForge.EVENT_BUS.addListener(BerserkHandler::onEntityTick);
        NeoForge.EVENT_BUS.addListener(BerserkHandler::onLivingDeath);
        // 调试命令：/cyberware sandevistan —— 直接触发一次减速，方便验证
        NeoForge.EVENT_BUS.addListener(CyberwareCommands::register);

        // ---- 配置 ----
        modContainer.registerConfig(ModConfig.Type.COMMON, CyberwareConfig.SPEC);

        // ---- 客户端专属（专用服务器上绝不能碰客户端类）----
        // 26.x：dist 字段已改为 getDist() 方法
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            CyberwareClient.init(modEventBus);
        }

        LOGGER.info("[cyberware] 义体系统已初始化（MC 26.1.2 / NeoForge 26.1.2.109）");
    }
}
