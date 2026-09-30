package com.dsh.cyberware.core;

import com.dsh.cyberware.registry.ModAttachments;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

/**
 * 玩家 RAM —— 脑机超频 / 快速破解的资源（服务端为唯一真相）。
 *
 * <p>挂在 {@code ModAttachments.RAM}（{@code cyberware:ram}）上：
 * 存档持久化、死亡拷贝、跨维度、自动同步全部由 {@code AttachmentType} 提供。
 *
 * <p><b>只有 {@code current} 落盘</b>：{@code max} 与 {@code regenPerMinute} 是**派生值**，
 * 每次用时按已安装义体实时求和（{@link CyberwareStats#maxRam} / {@link CyberwareStats#regenPerMinute}），
 * 这样装卸义体后上限立刻跟着变，不需要迁移旧存档里的数字。
 *
 * @param current 当前 RAM（保留小数；服务端夹在 0 以上）
 */
public record RamState(double current) {

    /** 附件默认值。第一次结算时由 {@link RamSystem} 灌满到上限（见那里的注释）。 */
    public static final RamState EMPTY = new RamState(0.0D);

    /** 存档用（{@code AttachmentType.serialize} 收 MapCodec）。 */
    public static final MapCodec<RamState> CODEC =
            Codec.DOUBLE.fieldOf("current").xmap(RamState::new, RamState::current);

    /** 同步用（{@code AttachmentType.sync} 收 StreamCodec）。 */
    public static final StreamCodec<RegistryFriendlyByteBuf, RamState> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.DOUBLE, RamState::current, RamState::new);

    public RamState {
        // 最后一道闸：负 RAM 没有意义（扣费与恢复都走服务端 API）
        current = Math.max(0.0D, current);
    }

    /**
     * 读玩家 RAM。
     *
     * <p>没装过（附件还不存在）时返回 {@link #EMPTY}，**不创建附件** ——
     * 「0」和「还没初始化」必须分得开：0 是合法的「RAM 耗尽」，
     * 而没初始化要在第一次结算时灌满（{@link RamSystem#onPlayerTick}）。
     */
    public static RamState of(Player player) {
        if (player == null) {
            return EMPTY;
        }
        RamState state = player.getExistingDataOrNull(ModAttachments.RAM.get());
        return state == null ? EMPTY : state;
    }

    /** 附件是否已经建立（= 是否已经初始化过）。 */
    public static boolean has(Player player) {
        return player != null && player.hasData(ModAttachments.RAM.get());
    }

    /** 写回 RAM（服务端）；{@code setData} 会顺带触发自动同步。 */
    public static void set(Player player, double current) {
        if (player != null) {
            player.setData(ModAttachments.RAM.get(), new RamState(current));
        }
    }
}
