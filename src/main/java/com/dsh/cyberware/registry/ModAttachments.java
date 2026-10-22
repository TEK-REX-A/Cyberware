package com.dsh.cyberware.registry;

import com.dsh.cyberware.Cyberware;
import com.dsh.cyberware.core.CyberwareInstallation;
import com.dsh.cyberware.core.OverclockState;
import com.dsh.cyberware.core.RamState;
import com.dsh.cyberware.core.ShieldState;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/**
 * 数据附件注册表 —— 把「玩家身上的义体表 / RAM / 超频状态」挂到每个玩家身上。
 *
 * <p>NeoForge 用 {@code AttachmentType}，Fabric 用 fabric-data-attachment-api 的
 * {@code AttachmentRegistry}：四件事（持久化 persistent / 死亡拷贝 copyOnDeath /
 * 默认值 initializer / 客户端同步 syncWith）一一对应。
 *
 * <p>⚠ 本文件的签名是<b>按 jar 方法表推断</b>的（persistent 收 {@code Codec}、
 * syncWith 收 {@code (StreamCodec, AttachmentSyncPredicate)}、targetOnly() 无参）——
 * 未编译验证。项目的 {@code CODEC} 是 {@code MapCodec}，所以这里要 {@code .codec()} 转换。
 */
public final class ModAttachments {

    /** 玩家身上的义体表 */
    public static RegistryHolder<AttachmentType<CyberwareInstallation>> INSTALLATION;

    /** 玩家 RAM（{@code cyberware:ram}） */
    public static RegistryHolder<AttachmentType<RamState>> RAM;

    /** 脑机超频状态（{@code cyberware:overclock}），不 copyOnDeath */
    public static RegistryHolder<AttachmentType<OverclockState>> OVERCLOCK;

    /**
     * 玩家护盾（{@code cyberware:shield}）。
     *
     * <p>持久化 + <b>不</b> copyOnDeath（死亡就该清零，见 {@code ShieldSystem#onDeath}）。
     * 同 RAM / 安装表一样走自建网络包同步（{@code ShieldPayload}），不用 {@code .syncWith(...)}。
     */
    public static RegistryHolder<AttachmentType<ShieldState>> SHIELD;

    private ModAttachments() {
    }

    public static void register() {
        // 注意：这里**不能**用 .syncWith(...)。
        // Fabric 的 attachment 同步实现（AttachmentSync）静态初始化时走 netty 的 ByteBufUtil，
        // 而 netty 在 Android 上 setAccessible(true) 被禁 → UnsupportedOperationException，
        // 整个 Entrypoint 直接炸（真机实测：debug.log 里 AttachmentRegistry.create -> byteBufUtil -> 反射失败）。
        // 数据同步改走自定义网络包（RamPayload / OverclockPayload 等），效果等价。
        INSTALLATION = new RegistryHolder<>(AttachmentRegistry.create(
                Identifier.fromNamespaceAndPath(Cyberware.MODID, "installation"),
                builder -> builder
                        .initializer(() -> CyberwareInstallation.EMPTY)
                        .persistent(CyberwareInstallation.CODEC.codec())
                        .copyOnDeath()));

        RAM = new RegistryHolder<>(AttachmentRegistry.create(
                Identifier.fromNamespaceAndPath(Cyberware.MODID, "ram"),
                builder -> builder
                        .initializer(() -> RamState.EMPTY)
                        .persistent(RamState.CODEC.codec())
                        .copyOnDeath()));

        OVERCLOCK = new RegistryHolder<>(AttachmentRegistry.create(
                Identifier.fromNamespaceAndPath(Cyberware.MODID, "overclock"),
                builder -> builder
                        .initializer(() -> OverclockState.IDLE)
                        .persistent(OverclockState.CODEC.codec())));

        // 护盾：持久化，但**不** copyOnDeath（死亡清零由 ShieldSystem.onDeath 负责）
        SHIELD = new RegistryHolder<>(AttachmentRegistry.create(
                Identifier.fromNamespaceAndPath(Cyberware.MODID, "shield"),
                builder -> builder
                        .initializer(() -> ShieldState.EMPTY)
                        .persistent(ShieldState.CODEC)));
    }
}
