package com.dsh.cyberware.network;

import com.dsh.cyberware.Cyberware;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端 → 服务端的操作请求：安装 / 卸载 / 升级。
 *
 * <p>需求书【六.2】要求主动技能用按键绑定 + CustomPayload 同步到服务端，
 * 操作台按钮同理走这条通道，服务端才是唯一有权改数据的一方。
 *
 * @param containerId 菜单的 containerId（服务端据此找到对应菜单）
 * @param slotIndex   目标义体槽位下标，{@code -1} 表示不针对具体槽位
 * @param actionId    {@link Action#ordinal()}
 */
public record CyberwareActionPayload(int containerId, int slotIndex, int actionId) implements CustomPacketPayload {

    /** 操作类型。 */
    public enum Action {
        /** 安装义体 */
        INSTALL,
        /** 卸载义体 */
        UNINSTALL,
        /** 升级稀有度 */
        UPGRADE;

        public static Action byId(int id) {
            Action[] values = values();
            return (id < 0 || id >= values.length) ? INSTALL : values[id];
        }
    }

    public static final CustomPacketPayload.Type<CyberwareActionPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Cyberware.MODID, "station_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CyberwareActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, CyberwareActionPayload::containerId,
                    ByteBufCodecs.VAR_INT, CyberwareActionPayload::slotIndex,
                    ByteBufCodecs.VAR_INT, CyberwareActionPayload::actionId,
                    CyberwareActionPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public Action action() {
        return Action.byId(this.actionId);
    }

    public static CyberwareActionPayload of(int containerId, int slotIndex, Action action) {
        return new CyberwareActionPayload(containerId, slotIndex, action.ordinal());
    }
}
