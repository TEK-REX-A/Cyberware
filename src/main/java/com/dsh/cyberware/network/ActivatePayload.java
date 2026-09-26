package com.dsh.cyberware.network;

import com.dsh.cyberware.Cyberware;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端 → 服务端：玩家按下了义体激活键。
 *
 * <p>不带参数 —— 服务端自己看玩家手上拿的是什么义体。
 * 数据以服务端为准，客户端说不上话（防作弊，也避免两端状态打架）。
 */
public record ActivatePayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ActivatePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Cyberware.MODID, "activate"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ActivatePayload> STREAM_CODEC =
            StreamCodec.unit(new ActivatePayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
