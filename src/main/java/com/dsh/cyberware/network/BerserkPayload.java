package com.dsh.cyberware.network;

import com.dsh.cyberware.Cyberware;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端 → 客户端：狂暴状态同步。
 *
 * <p>带上 {@code totalTicks}（本次激活的总时长），客户端才能算出**稳定**的进度 ——
 * 只给「剩余时间」的话，每次刷新都会把进度条重置回满格。
 */
public record BerserkPayload(int remainingTicks, int totalTicks, float damageMultiplier, boolean active)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BerserkPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Cyberware.MODID, "berserk"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BerserkPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, BerserkPayload::remainingTicks,
                    ByteBufCodecs.VAR_INT, BerserkPayload::totalTicks,
                    ByteBufCodecs.FLOAT, BerserkPayload::damageMultiplier,
                    ByteBufCodecs.BOOL, BerserkPayload::active,
                    BerserkPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
