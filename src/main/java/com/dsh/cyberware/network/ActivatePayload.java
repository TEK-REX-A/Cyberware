package com.dsh.cyberware.network;

import com.dsh.cyberware.Cyberware;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端 → 服务端：玩家要激活一件义体。
 *
 * <p>0.3.12 起带一个 {@code defId}，把「激活谁」这件事说清楚：
 * <ul>
 *   <li>{@link #HELD}（空串）= 「激活我手上这件」—— V 键的既有行为，
 *       服务端自己看主手物品（{@link com.dsh.cyberware.core.CyberwareAbilities#activateHeld}）；</li>
 *   <li>非空 = 「激活我已经装上的这件」—— R 键轮盘（t14）用，
 *       服务端会**自己查已安装表**确认玩家真的装了它，再决定给不给效果。</li>
 * </ul>
 *
 * <p>换句话说：{@code defId} 表达的是「意图」，不是「权限」。
 * 数值、稀有度、是否拥有，全以服务端为准。
 *
 * @param defId 目标型号 id；空串表示手持激活
 */
public record ActivatePayload(String defId) implements CustomPacketPayload {

    /** 空字符串 = 激活手持物品（V 键，保持既有行为）。 */
    public static final String HELD = "";

    public static final CustomPacketPayload.Type<ActivatePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Cyberware.MODID, "activate"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ActivatePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, ActivatePayload::defId,
                    ActivatePayload::new);

    /**
     * 防御：允许外部传 null，线上格式统一成空串。
     *
     * <p>空串 = 手持激活，所以 null 只会退化成「V 键行为」，不会变成「激活任意义体」。
     */
    public ActivatePayload {
        defId = defId == null ? HELD : defId;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** 手持激活（V 键）。 */
    public static ActivatePayload held() {
        return new ActivatePayload(HELD);
    }

    /** 指定型号激活（R 键轮盘）。 */
    public static ActivatePayload of(String defId) {
        return new ActivatePayload(defId);
    }
}
