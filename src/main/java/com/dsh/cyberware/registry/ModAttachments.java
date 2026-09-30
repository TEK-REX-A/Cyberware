package com.dsh.cyberware.registry;

import com.dsh.cyberware.Cyberware;
import com.dsh.cyberware.core.CyberwareInstallation;
import com.dsh.cyberware.core.OverclockState;
import com.dsh.cyberware.core.RamState;
import com.dsh.cyberware.core.RamSystem;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * 数据附件注册表 —— 把「玩家身上的义体表 / RAM / 超频状态」挂到每个玩家身上。
 *
 * <p>NeoForge 26.x 用 AttachmentType 取代了旧的 Capability。选它而不是自己存 NBT，
 * 是因为这几件事全都白送：
 * <ul>
 *   <li>{@code serialize} —— 存档持久化（MapCodec）</li>
 *   <li>{@code copyOnDeath} —— 死亡后不掉义体，复活仍是改造人</li>
 *   <li>{@code sync} —— 自动同步到客户端（义眼要在客户端判断该不该描边）</li>
 * </ul>
 */
public final class ModAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Cyberware.MODID);

    /** 玩家身上的义体表 */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<CyberwareInstallation>> INSTALLATION =
            ATTACHMENTS.register("installation",
                    () -> AttachmentType.builder(() -> CyberwareInstallation.EMPTY)
                            .serialize(CyberwareInstallation.CODEC)
                            .copyOnDeath()
                            .sync(CyberwareInstallation.STREAM_CODEC)
                            .build());

    /**
     * 玩家 RAM（{@code cyberware:ram}）—— 脑机超频 / 快速破解的资源。
     *
     * <p>与 {@link #INSTALLATION} 完全同款：MapCodec 落盘（{@link RamState#CODEC}）、
     * {@code copyOnDeath}（复活带着自己的 RAM 值）、StreamCodec 自动同步
     * （{@link RamState#STREAM_CODEC}）。{@code max} / {@code regenPerMinute} 是派生值，不落盘。
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<RamState>> RAM =
            ATTACHMENTS.register("ram",
                    () -> AttachmentType.builder(() -> RamState.EMPTY)
                            .serialize(RamState.CODEC)
                            .copyOnDeath()
                            .sync(RamState.STREAM_CODEC)
                            .build());

    /**
     * 脑机超频状态（{@code cyberware:overclock}）。
     *
     * <p>不 {@code copyOnDeath}：复活不该白带一次超频中状态（冷却时刻倒是跟着走，无害）。
     * 时刻用服务端全局 tick 计数，见 {@link OverclockState} 的类注释。
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<OverclockState>> OVERCLOCK =
            ATTACHMENTS.register("overclock",
                    () -> AttachmentType.builder(() -> OverclockState.IDLE)
                            .serialize(OverclockState.CODEC)
                            .sync(OverclockState.STREAM_CODEC)
                            .build());

    static {
        // RAM 恢复 / 超频倒计时的服务端每刻推进（core/RamSystem）。
        //
        // 用**显式注册**而不是类上的 @EventBusSubscriber 注解：注解漏扫会静默失效，
        // 而静默失效正是这个项目吃过亏的地方（要等主人真机才发现）。
        // 这个静态块随本类初始化执行，而本类在 Cyberware 构造器里被引用（ATTACHMENTS.register）。
        NeoForge.EVENT_BUS.addListener(RamSystem::onPlayerTick);
    }

    private ModAttachments() {
    }
}
