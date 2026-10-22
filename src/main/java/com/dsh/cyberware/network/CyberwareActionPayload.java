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
 * @param slotIndex   {@link Action#UPGRADE} 用的目标下标；{@code -1} 表示不针对具体槽位
 * @param actionId    {@link Action#ordinal()}
 * @param defId       目标型号 id。<b>安装与卸载都靠它定位</b>：
 *                    安装 = 「从玩家背包里找这个型号并扣 1 件」（v0.5.4 起没有送料槽），
 *                    卸载 = 「卸下这个型号」。{@code CyberwareInstallation.installed()} 的迭代顺序
 *                    不保证，所以不能用「第 N 件」指代某件义体。
 */
public record CyberwareActionPayload(int containerId, int slotIndex, int actionId, String defId)
        implements CustomPacketPayload {

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
                    ByteBufCodecs.STRING_UTF8, CyberwareActionPayload::defId,
                    CyberwareActionPayload::new);

    /** 防御：{@code defId} 允许外部传 null，线上格式统一成空串。 */
    public CyberwareActionPayload {
        defId = defId == null ? "" : defId;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public Action action() {
        return Action.byId(this.actionId);
    }

    public static CyberwareActionPayload of(int containerId, int slotIndex, Action action) {
        return new CyberwareActionPayload(containerId, slotIndex, action.ordinal(), "");
    }

    /**
     * 安装：让服务端从玩家背包里找 {@code defId} 这个型号并扣掉 1 件装上。
     *
     * <p>v0.5.4 起没有送料槽，客户端<b>不再上传槽位下标</b>（{@code slotIndex} 恒为 -1）——
     * 只报「我要装哪个型号」，具体动哪一格由服务端 {@code findInventorySlot} 裁决。
     */
    public static CyberwareActionPayload install(int containerId, String defId) {
        return new CyberwareActionPayload(containerId, -1, Action.INSTALL.ordinal(), defId);
    }

    /** 卸载：卸下指定型号并把物品还给玩家。 */
    public static CyberwareActionPayload uninstall(int containerId, String defId) {
        return new CyberwareActionPayload(containerId, -1, Action.UNINSTALL.ordinal(), defId);
    }

    /** 升级：目标为操作台第 {@code slotIndex} 个义体槽。 */
    public static CyberwareActionPayload upgrade(int containerId, int slotIndex) {
        return of(containerId, slotIndex, Action.UPGRADE);
    }
}
