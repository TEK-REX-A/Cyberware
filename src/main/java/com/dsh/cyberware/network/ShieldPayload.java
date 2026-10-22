package com.dsh.cyberware.network;

import com.dsh.cyberware.Cyberware;
import com.dsh.cyberware.core.ShieldState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端 → 客户端：玩家护盾快照（当前值 / 上限 / 最后挨打时刻）。
 *
 * <p><b>为什么需要它</b>：{@code ModAttachments.SHIELD} 和安装表一样**没有** {@code .syncWith(...)}
 * （Fabric 的附件同步在 Android 上会炸，见 {@code ModAttachments} 注释），
 * 所以服务端算出来的护盾客户端完全看不到。这里沿用
 * {@link InstallationPayload} 的同一套做法：服务端推、客户端写回**同一个**附件。
 *
 * <p>客户端拿到之后：
 * <ul>
 *   <li>右下角那条青蓝色护盾条（主人正在画，属后续任务）直接读
 *       {@code ShieldState.of(player)} 与 {@code CyberwareStats.maxShield(player)}；</li>
 *   <li>上限是派生值（Σ 已装义体的 {@code shield}），客户端已有安装表镜像，所以这里再带一份
 *       {@code max} 只是让 HUD 不必自己求和，也方便对照服务端算得对不对。</li>
 * </ul>
 *
 * @param current          当前护盾
 * @param max              护盾上限（服务端算好的派生值）
 * @param lastHurtGameTime 最后一次挨打的游戏刻（0 = 从没挨过打，HUD 可据此显示「战斗中 / 恢复中」）
 */
public record ShieldPayload(double current, double max, long lastHurtGameTime)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ShieldPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Cyberware.MODID, "shield"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShieldPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.DOUBLE, ShieldPayload::current,
                    ByteBufCodecs.DOUBLE, ShieldPayload::max,
                    ByteBufCodecs.VAR_LONG, ShieldPayload::lastHurtGameTime,
                    ShieldPayload::new);

    public ShieldPayload {
        current = Math.max(0.0D, current);
        max = Math.max(0.0D, max);
        lastHurtGameTime = Math.max(0L, lastHurtGameTime);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** 把快照还原成要写进客户端附件的状态（上限是派生值，不写进附件）。 */
    public ShieldState toState() {
        return new ShieldState(this.current, this.lastHurtGameTime);
    }
}

