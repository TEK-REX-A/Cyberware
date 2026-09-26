package com.dsh.cyberware.network;

import com.dsh.cyberware.Cyberware;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端 → 客户端：广播「时间减缓」开始/刷新。
 *
 * <p>带上作用区域（中心坐标 + 半径）与施法者 UUID，客户端才能做到两件事：
 * <ul>
 *   <li>只对**真正落在区域内**的实体做动画减速 —— 否则地图另一头的生物也会跟着慢动作</li>
 *   <li>把施法者本人排除在外 —— 否则自己挥刀的动作也被拖慢</li>
 * </ul>
 */
public record TimeDilationPayload(int durationTicks, float ratio,
                                  double x, double y, double z, float radius,
                                  UUID owner) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TimeDilationPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Cyberware.MODID, "time_dilation"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TimeDilationPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, TimeDilationPayload::durationTicks,
                    ByteBufCodecs.FLOAT, TimeDilationPayload::ratio,
                    ByteBufCodecs.DOUBLE, TimeDilationPayload::x,
                    ByteBufCodecs.DOUBLE, TimeDilationPayload::y,
                    ByteBufCodecs.DOUBLE, TimeDilationPayload::z,
                    ByteBufCodecs.FLOAT, TimeDilationPayload::radius,
                    UUIDUtil.STREAM_CODEC, TimeDilationPayload::owner,
                    TimeDilationPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
