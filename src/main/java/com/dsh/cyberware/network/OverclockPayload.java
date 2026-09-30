package com.dsh.cyberware.network;

import com.dsh.cyberware.Cyberware;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 脑机超频：**双向**包（同一个 payload 两种用法）。
 *
 * <ul>
 *   <li>{@link Action#TOGGLE}　客户端 → 服务端：「我要开/关超频」。此时其余字段填 0，
 *       服务端一律自己裁决（装了网络接入仓吗？在冷却吗？）—— 客户端的字段不作数。</li>
 *   <li>{@link Action#STATE}　服务端 → 客户端：当前状态快照，HUD 按它渲染与倒计时。</li>
 * </ul>
 *
 * <p>客户端 handler 必须在 `client` 侧用 {@code RegisterClientPayloadHandlersEvent} 注册
 * （服务端 handler 在 {@code CyberwareNetwork} 里）—— 见交付文档 §4。
 *
 * @param action                  见上
 * @param active                  是否在超频中
 * @param remainingTicks          剩余超频刻数（0 = 不在超频中）
 * @param totalTicks              本次超频总时长（按当前网络接入仓稀有度算；进度条分母）
 * @param cooldownRemainingTicks  剩余冷却刻数（0 = 不在冷却中）
 * @param cooldownTotalTicks      冷却总时长（按当前网络接入仓稀有度算；冷却环分母）
 */
public record OverclockPayload(Action action, boolean active, int remainingTicks, int totalTicks,
                               int cooldownRemainingTicks, int cooldownTotalTicks)
        implements CustomPacketPayload {

    /** 用法区分。 */
    public enum Action {
        /** 客户端 → 服务端：请求切换超频 */
        TOGGLE,
        /** 服务端 → 客户端：状态快照 */
        STATE;

        public static Action byId(int id) {
            Action[] values = values();
            return (id < 0 || id >= values.length) ? STATE : values[id];
        }
    }

    public static final CustomPacketPayload.Type<OverclockPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Cyberware.MODID, "overclock"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OverclockPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, p -> p.action().ordinal(),
                    ByteBufCodecs.BOOL, OverclockPayload::active,
                    ByteBufCodecs.VAR_INT, OverclockPayload::remainingTicks,
                    ByteBufCodecs.VAR_INT, OverclockPayload::totalTicks,
                    ByteBufCodecs.VAR_INT, OverclockPayload::cooldownRemainingTicks,
                    ByteBufCodecs.VAR_INT, OverclockPayload::cooldownTotalTicks,
                    (actionId, active, remaining, total, cooldown, cooldownTotal) ->
                            new OverclockPayload(Action.byId(actionId), active, remaining, total,
                                    cooldown, cooldownTotal));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** 客户端上行用的「请求切换」包（其余字段占位，服务端不看）。 */
    public static OverclockPayload toggleRequest() {
        return new OverclockPayload(Action.TOGGLE, false, 0, 0, 0, 0);
    }
}
