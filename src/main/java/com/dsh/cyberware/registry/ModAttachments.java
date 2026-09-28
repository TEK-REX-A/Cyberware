package com.dsh.cyberware.registry;

import com.dsh.cyberware.Cyberware;
import com.dsh.cyberware.core.CyberwareInstallation;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * 数据附件注册表 —— 把「玩家身上的义体表」挂到每个玩家身上。
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

    private ModAttachments() {
    }
}
