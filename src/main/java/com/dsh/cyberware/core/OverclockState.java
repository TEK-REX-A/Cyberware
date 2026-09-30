package com.dsh.cyberware.core;

import com.dsh.cyberware.registry.ModAttachments;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

/**
 * 脑机超频状态（服务端为唯一真相）。
 *
 * <p>挂在 {@code ModAttachments.OVERCLOCK}（{@code cyberware:overclock}）上 ——
 * **按玩家存**，不是静态表，所以天然没有 0.3.12 那个 P0（换世界后静态状态残留）的问题。
 *
 * <h3>时刻用哪个时钟</h3>
 * {@code expiresAt} / {@code cooldownUntil} 用的是
 * {@link net.minecraft.server.MinecraftServer#getTickCount()} —— **服务端全局单调 tick 计数**，
 * 与任何 {@code Level.getGameTime()} 无关。理由：
 * <ul>
 *   <li>{@code Level.getGameTime()} 是**每个世界各自**从 0 算的，跨维度/跨世界会串味（0.3.12 P0 的根因）；</li>
 *   <li>服务器 tick 计数是「这一趟服务器运行」的统一时间轴，换维度不影响；</li>
 *   <li>服务器重启后计数会归零 —— 这一种情形由 {@link OverclockSystem} 的
 *       「未来时刻超过占位表上限就作废」自愈规则兜住（见那里的注释）。</li>
 * </ul>
 * 本记录**不落 {@code totalTicks}**：进度条要的「总时长」按当前装着的网络接入仓实时算
 * （{@link CyberwareStats#overclockDurationTicks}），HUD 需要的剩余量由
 * {@link com.dsh.cyberware.network.OverclockPayload} 直接带过去。
 *
 * @param active        是否正在超频
 * @param expiresAt     超频结束时刻（服务端 tick）
 * @param cooldownUntil 冷却结束时刻（服务端 tick）；不在冷却中是 0
 */
public record OverclockState(boolean active, long expiresAt, long cooldownUntil) {

    /** 附件默认值：没超频、没冷却。 */
    public static final OverclockState IDLE = new OverclockState(false, 0L, 0L);

    /** 存档用（{@code AttachmentType.serialize} 收 MapCodec，所以用 {@code mapCodec} 而不是 {@code create}）。 */
    public static final MapCodec<OverclockState> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.BOOL.optionalFieldOf("active", false).forGetter(OverclockState::active),
            Codec.LONG.optionalFieldOf("expires_at", 0L).forGetter(OverclockState::expiresAt),
            Codec.LONG.optionalFieldOf("cooldown_until", 0L).forGetter(OverclockState::cooldownUntil)
    ).apply(inst, OverclockState::new));

    /** 同步用。 */
    public static final StreamCodec<RegistryFriendlyByteBuf, OverclockState> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, OverclockState::active,
                    ByteBufCodecs.VAR_LONG, OverclockState::expiresAt,
                    ByteBufCodecs.VAR_LONG, OverclockState::cooldownUntil,
                    OverclockState::new);

    /** 读玩家超频状态；没有附件返回 {@link #IDLE}（不创建附件）。 */
    public static OverclockState of(Player player) {
        if (player == null) {
            return IDLE;
        }
        OverclockState state = player.getExistingDataOrNull(ModAttachments.OVERCLOCK.get());
        return state == null ? IDLE : state;
    }

    /** 写回（服务端）；{@code setData} 顺带触发自动同步。 */
    public static void set(Player player, OverclockState state) {
        if (player != null) {
            player.setData(ModAttachments.OVERCLOCK.get(), state == null ? IDLE : state);
        }
    }

    /** 此刻是否在超频中。 */
    public boolean isActive(long now) {
        return this.active && now < this.expiresAt;
    }

    /** 此刻是否在冷却中（超频中不算冷却）。 */
    public boolean isCoolingDown(long now) {
        return !this.active && now < this.cooldownUntil;
    }

    /** 剩余超频 tick（不在超频中返回 0）。 */
    public int remainingTicks(long now) {
        if (!isActive(now)) {
            return 0;
        }
        long remaining = this.expiresAt - now;
        return remaining <= 0L ? 0 : (int) Math.min(Integer.MAX_VALUE, remaining);
    }

    /** 剩余冷却 tick（不在冷却中返回 0）。 */
    public int cooldownRemainingTicks(long now) {
        if (!isCoolingDown(now)) {
            return 0;
        }
        long remaining = this.cooldownUntil - now;
        return remaining <= 0L ? 0 : (int) Math.min(Integer.MAX_VALUE, remaining);
    }

    /**
     * 结束超频并进入冷却（手动关闭与到点自动结束都走这里）。
     *
     * @param now           当前服务端 tick
     * @param cooldownTicks 冷却时长（由网络接入仓稀有度决定）
     */
    public OverclockState endAt(long now, int cooldownTicks) {
        return new OverclockState(false, this.expiresAt, Math.max(this.cooldownUntil, now + Math.max(1, cooldownTicks)));
    }
}
