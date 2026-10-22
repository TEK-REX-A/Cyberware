package com.dsh.cyberware.network;

import com.dsh.cyberware.Cyberware;
import com.dsh.cyberware.core.CyberwareInstallation;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端 → 客户端：把<b>权威的</b>「玩家已装义体表」整份推给该玩家。
 *
 * <p><b>为什么必须有这个包（0.5.3 真机 P0「吞义体」）</b>：
 * {@code ModAttachments.INSTALLATION} 是用 {@code AttachmentRegistry.create(...)} 注册的，
 * <b>没有</b> {@code .syncWith(...)}（Fabric 的附件同步在 Android 上会因 netty 反射失败而炸，
 * 见 {@code ModAttachments} 类注释）。所以：
 * <ul>
 *   <li>服务端 {@code Attachments.set(...)} 写进去的数据<b>只有服务端能看到</b>；</li>
 *   <li>客户端 {@code CyberwareInstallation.of(player)} 拿到的永远是 {@code EMPTY}；</li>
 *   <li>于是「点安装 → 物品没了 → 已安装列表空 → 轮盘说没装」，
 *       而服务端那边校验却报「不能重复安装」（它确实装上了）。</li>
 * </ul>
 * 修复方式就是这条自建 S2C 同步链（同样因为上面那条 netty 限制，不能用官方 syncWith）：
 * 服务端每次改表后 {@link CyberwareNetwork#syncInstallation} 发一份，
 * 客户端把收到的表写回<b>同一个</b>附件，于是三处读取方
 * （操作台界面 / 轮盘 / 被动效果）自然读到同一份数据。
 *
 * @param installation 该玩家身上的完整义体表（服务端权威快照，整份覆盖）
 */
public record InstallationPayload(CyberwareInstallation installation) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<InstallationPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Cyberware.MODID, "installation"));

    public static final StreamCodec<RegistryFriendlyByteBuf, InstallationPayload> STREAM_CODEC =
            StreamCodec.composite(
                    CyberwareInstallation.STREAM_CODEC, InstallationPayload::installation,
                    InstallationPayload::new);

    /** 防御：表永远不该是 null（服务端 {@code of()} 保证返回 EMPTY）。 */
    public InstallationPayload {
        installation = installation == null ? CyberwareInstallation.EMPTY : installation;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

