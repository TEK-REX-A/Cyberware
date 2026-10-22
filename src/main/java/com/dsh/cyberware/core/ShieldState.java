package com.dsh.cyberware.core;

import com.dsh.cyberware.registry.ModAttachments;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

/**
 * 玩家护盾的<b>当前值</b>（服务端为唯一真相），挂在 {@code ModAttachments.SHIELD} 上。
 *
 * <p><b>只存两个数</b>：{@code current}（当前护盾）与 {@code lastHurtGameTime}（最后一次挨打的
 * 游戏刻，用来算「脱战 3 秒」）。<b>上限与恢复速度都是派生值</b> ——
 * 每次用时按已安装义体实时求和（{@link CyberwareStats#maxShield} /
 * {@link CyberwareStats#shieldRegenPerSecond}），所以装卸义体后上限立刻跟着变，
 * 而且存档里永远不会存着「过期的上限」。
 *
 * <p>这和 {@link RamState} 是同一套口径（只有 current 落盘）。
 *
 * <p>⚠ <b>附件不会自动同步到客户端</b>（本工程 {@code ModAttachments} 里刻意没有
 * {@code .syncWith(...)}，原因见该类注释）。护盾值靠 {@code ShieldPayload} 手工同步，
 * 客户端收到后写回<b>同一个</b>附件 —— 与义体表（{@code InstallationPayload}）完全一样的做法。
 *
 * @param current          当前护盾（0..上限）
 * @param lastHurtGameTime 最后一次挨打的游戏刻（0 = 从没挨过打）
 */
public record ShieldState(double current, long lastHurtGameTime) {

    /** 空盾（没装过给护盾的义体 / 刚出生 / 刚死过）。 */
    public static final ShieldState EMPTY = new ShieldState(0.0D, 0L);

    /** 存档用（{@code AttachmentRegistry builder.persistent(...)} 收 Codec）。 */
    public static final Codec<ShieldState> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.DOUBLE.optionalFieldOf("current", 0.0D).forGetter(ShieldState::current),
            Codec.LONG.optionalFieldOf("last_hurt", 0L).forGetter(ShieldState::lastHurtGameTime)
    ).apply(inst, ShieldState::new));

    /** 网络同步用（{@code ShieldPayload} 复用这一个）。 */
    public static final StreamCodec<RegistryFriendlyByteBuf, ShieldState> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.DOUBLE, ShieldState::current,
                    ByteBufCodecs.VAR_LONG, ShieldState::lastHurtGameTime,
                    ShieldState::new);

    public ShieldState {
        // 最后一道闸：负护盾没有意义（扣减与恢复都走 ShieldSystem）
        current = Math.max(0.0D, current);
        lastHurtGameTime = Math.max(0L, lastHurtGameTime);
    }

    /** 读玩家护盾；附件不存在时返回 {@link #EMPTY}（<b>不创建</b>附件）。 */
    public static ShieldState of(Player player) {
        if (player == null) {
            return EMPTY;
        }
        ShieldState state = Attachments.get(Attachments.of(player), ModAttachments.SHIELD.get());
        return state == null ? EMPTY : state;
    }

    /** 附件是否已经建立（<b>区分「0 护盾」与「还没初始化」</b>，首次初始化要灌满）。 */
    public static boolean has(Player player) {
        return player != null && Attachments.has(Attachments.of(player), ModAttachments.SHIELD.get());
    }

    /** 写回护盾（服务端：权威值；客户端：服务端推下来的镜像）。 */
    public static void set(Player player, double current, long lastHurtGameTime) {
        if (player != null) {
            Attachments.set(Attachments.of(player), ModAttachments.SHIELD.get(),
                    new ShieldState(current, lastHurtGameTime));
        }
    }
}

