package com.dsh.cyberware.network;

import com.dsh.cyberware.Cyberware;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端 → 客户端：RAM HUD 的展示快照。
 *
 * <p><b>方向</b>：单向（{@code playToClient}）。RAM 的权威值在服务端，客户端连扣费判定都不参与 ——
 * 这个包只是「给 HUD 画图用的数字」。
 *
 * <p>发送时机（服务端，见 {@code core/RamSystem}）：
 * ① 数值真的变化时（首次灌满、每秒恢复、装卸义体导致上限变化而被夹住）；
 * ② 超频进行中每 20 刻补一次。
 *
 * @param current         当前 RAM（保留小数，客户端插值用）
 * @param max             RAM 上限（= Σ 已安装义体的 {@code Stats.RAM}，最小 0）
 * @param regenPerMinute  每分钟恢复量（= Σ 已安装义体的 {@code Stats.RAM_REGEN}）
 * @param overclockActive 超频是否进行中（由 {@code OverclockPayload.Action.STATE} 也会带一次）
 * @param depleted        {@code current <= 0}。⚠️ {@code max <= 0} 的玩家（没装任何给 RAM 的义体）
 *                        同样会长这样 —— 客户端要用 {@code max <= 0} 先判「没有 RAM 能力」，
 *                        再判「濒死超频 / 彻底瘫痪」（见交付文档 §2 状态判定表）
 */
public record RamPayload(double current, double max, double regenPerMinute,
                         boolean overclockActive, boolean depleted) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RamPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Cyberware.MODID, "ram"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RamPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.DOUBLE, RamPayload::current,
                    ByteBufCodecs.DOUBLE, RamPayload::max,
                    ByteBufCodecs.DOUBLE, RamPayload::regenPerMinute,
                    ByteBufCodecs.BOOL, RamPayload::overclockActive,
                    ByteBufCodecs.BOOL, RamPayload::depleted,
                    RamPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
