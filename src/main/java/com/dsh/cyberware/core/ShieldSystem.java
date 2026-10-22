package com.dsh.cyberware.core;

import com.dsh.cyberware.Cyberware;
import com.dsh.cyberware.config.CyberwareConfig;
import com.dsh.cyberware.network.ShieldPayload;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 护盾系统 · <b>服务端权威</b>核心（伤害拦截 / 脱战恢复 / 同步）。
 *
 * <h3>规则（主人 + Friend 定，2026-10-22）</h3>
 * <ol>
 *   <li><b>上限由义体提供</b>：{@code Σ Stats.SHIELD}（见 {@link CyberwareStats#maxShield}）。</li>
 *   <li><b>伤害先扣护盾，溢出扣血</b>：由 {@code mixin/ShieldDamageMixin} 在
 *       {@code LivingEntity.actuallyHurt(ServerLevel, DamageSource, float)} 的入参上改数值实现——
 *       护盾吃掉的部分直接从这一次伤害里减掉，剩下的照原版逻辑扣血。</li>
 *   <li><b>脱战 3 秒后恢复</b>：{@code now - lastHurtGameTime >= shield.combatDelayTicks}（默认 60 刻）
 *       才开始回，速度 = <b>基础值（配置，默认 3/s）+ Σ Stats.SHIELD_REGEN</b>，上限封顶。</li>
 *   <li><b>回满就停</b>；换下义体导致上限变小 → 立刻夹住。</li>
 * </ol>
 *
 * <h3>为什么伤害拦截要 mixin</h3>
 * Fabric 的 {@code ServerLivingEntityEvents.ALLOW_DAMAGE} 只能「放行 / 取消」，
 * <b>给不了修改后数值的能力</b>（签名是 {@code boolean allowDamage(LivingEntity, DamageSource, float)}），
 * {@code AFTER_DAMAGE} 是事后回执。所以只能 mixin {@code actuallyHurt}：
 * 选它而不是 {@code hurtServer} 的理由是它<b>只被真正要落地的伤害调用</b>
 * （invulnerableTime 的「只补差额」分支与正常分支都会调它），
 * 而且 {@code amount == 0} 时原版方法自己在第 111 行就 return 了 —— 全吸收 = 零副作用。
 *
 * <h3>线程 / 生命周期</h3>
 * 所有写操作都只在服务端主线程发生（{@code END_SERVER_TICK} + 伤害回调都在主线程）。
 */
public final class ShieldSystem {

    /** 20 刻 = 1 秒。 */
    private static final int TICKS_PER_SECOND = 20;

    /** 恢复期的同步节流：每 5 刻发一个包（4 包/秒），避免每刻一个包。 */
    private static final int REGEN_SYNC_INTERVAL_TICKS = 5;

    /** 伤害拦截是否已经生效过一次（真机验证用：第一次吸收会打一条 INFO）。 */
    private static final AtomicBoolean HOOK_ANNOUNCED = new AtomicBoolean(false);

    /** tick 异常只记一次日志（每刻回调，刷屏会拖垮日志）。 */
    private static final AtomicBoolean TICK_FAIL_LOGGED = new AtomicBoolean(false);

    /** mixin 注入方法里抛异常也只记一次（伤害路径每秒可能走几十次）。 */
    private static final AtomicBoolean HOOK_FAIL_LOGGED = new AtomicBoolean(false);

    private ShieldSystem() {
    }

    // ═══════════════════ 派生值 ═══════════════════

    /** 护盾上限（Σ 义体给的 {@code shield}）。 */
    public static double maxShield(Player player) {
        return CyberwareStats.maxShield(player);
    }

    /** 每秒恢复量（配置基础值 + Σ 义体给的 {@code shield_regen}）。 */
    public static double regenPerSecond(Player player) {
        return CyberwareStats.shieldRegenPerSecond(player);
    }

    // ═══════════════════ 伤害拦截（由 mixin 调用） ═══════════════════

    /**
     * 让护盾吸收这次伤害，返回<b>应继续交给原版逻辑的伤害量</b>。
     *
     * <p>调用点：{@code ShieldDamageMixin} 在
     * {@code LivingEntity.actuallyHurt} 的 {@code float amount} 入参上（{@code @ModifyVariable}）。
     *
     * <p><b>吸收的是护甲减免之前的伤害</b>（注入点在方法入口）：护盾是「外挂一层」，先吃原始伤害，
     * 剩下的溢出部分才进原版的护甲/抗性/吸收流程。
     * {@code TODO(主人裁决)} 若希望「护甲先生效、护盾吃减免后的伤害」，注入点要挪到
     * {@code getDamageAfterMagicAbsorb} 之后（注入点一改，数值手感会明显变化）。
     *
     * <p><b>已知不拦的</b>：创造 / 观察者模式。
     * <p><b>TODO(主人裁决)</b> 真伤 / 穿盾（{@code DamageTypeTags.BYPASSES_SHIELD}、
     * {@code BYPASSES_INVULNERABILITY}）目前<b>也会</b>被护盾吸收 —— 因为
     * {@code @ModifyVariable(argsOnly = true)} 拿不到 {@code DamageSource}。
     * 要按伤害类型放行得换注入方式（MixinExtras {@code @Local(argsOnly = true)} 或改成
     * {@code @Inject} + 手工改写），属于「先问主人要不要」的改动。
     *
     * @param player 挨打的服务端玩家
     * @param amount 本次要落地的伤害（护甲减免前）
     * @return 护盾吃完之后剩下的伤害；护盾没起作用时原样返回 {@code amount}
     */
    public static float absorb(ServerPlayer player, float amount) {
        if (player == null || amount <= 0.0F) {
            return amount;
        }
        if (!CyberwareConfig.SHIELD_ENABLED) {
            return amount;
        }
        if (player.isCreative() || player.isSpectator()) {
            return amount;   // 创造/观察者不拦（需求：创造不拦）
        }
        double max = maxShield(player);
        if (max <= 0.0D) {
            return amount;   // 没装任何给护盾的义体
        }

        long now = gameTime(player);
        double current = Math.min(ShieldState.of(player).current(), max);

        if (current <= 0.0D) {
            // 盾已破：这一次照样扣血，但要把「挨打时刻」记下来 —— 破盾后也得脱战 3 秒才回
            ShieldState.set(player, 0.0D, now);
            return amount;
        }

        double absorbed = Math.min(current, amount);
        double next = current - absorbed;
        double remain = amount - absorbed;
        if (remain < 1.0E-4D) {
            remain = 0.0D;   // 全吸收：浮点误差不能留一点点血线伤害
        }

        ShieldState.set(player, next, now);
        sync(player);
        announceOnce();
        debugMessage(player, absorbed, next, max, remain);

        Cyberware.LOGGER.debug("[cyberware] 护盾吸收 {} 点（{}/{}），溢出 {} 点",
                String.format("%.2f", absorbed), String.format("%.1f", next),
                String.format("%.1f", max), String.format("%.2f", remain));
        return (float) remain;
    }

    // ═══════════════════ 脱战恢复（每刻） ═══════════════════

    /** 服务端每刻入口（注册于 {@code CyberwareNetwork#register} 的 {@code END_SERVER_TICK}）。 */
    public static void onServerTick(MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            try {
                tick(player);
            } catch (Throwable t) {
                // 每刻回调，抛异常会带崩服务端主循环 —— 兜住，只记一次
                if (TICK_FAIL_LOGGED.compareAndSet(false, true)) {
                    Cyberware.LOGGER.warn("[cyberware] 护盾 tick 异常（已忽略）", t);
                }
            }
        }
    }

    /** 单个玩家的护盾推进：初始化 / 夹上限 / 脱战恢复。 */
    private static void tick(ServerPlayer player) {
        double max = maxShield(player);
        if (max <= 0.0D) {
            // 没装给护盾的义体：把残留值清零（卸掉义体后不该留一层白嫖的盾）
            if (ShieldState.has(player) && ShieldState.of(player).current() != 0.0D) {
                ShieldState.set(player, 0.0D, ShieldState.of(player).lastHurtGameTime());
                sync(player);
            }
            return;
        }

        if (!ShieldState.has(player)) {
            // 第一次见到该玩家（或刚装上第一件给护盾的义体）：灌满
            ShieldState.set(player, max, 0L);
            sync(player);
            if (CyberwareConfig.SHIELD_DEBUG_MESSAGES) {
                note(player, "§b[护盾] 上限 " + fmt(max) + "（Σ 义体提供的护盾量）");
            }
            return;
        }

        ShieldState state = ShieldState.of(player);
        double current = state.current();
        if (current > max) {
            // 卸了/降级了义体 → 上限变小，立刻夹住（不凭空多出护盾）
            ShieldState.set(player, max, state.lastHurtGameTime());
            sync(player);
            return;
        }
        if (current >= max) {
            return;   // 满盾
        }

        long now = gameTime(player);
        long since = state.lastHurtGameTime() <= 0L ? Long.MAX_VALUE : now - state.lastHurtGameTime();
        if (since < CyberwareConfig.SHIELD_COMBAT_DELAY_TICKS) {
            return;   // 还在战斗 / 脱战倒计时里
        }

        double perSecond = regenPerSecond(player);
        if (perSecond <= 0.0D) {
            return;
        }
        double next = Math.min(max, current + perSecond / TICKS_PER_SECOND);
        if (next - current <= 1.0E-6D) {
            return;
        }
        ShieldState.set(player, next, state.lastHurtGameTime());
        if (player.tickCount % REGEN_SYNC_INTERVAL_TICKS == 0) {
            sync(player);
        }

        // ---- 真机可观测性：三句话把「脱战 3 秒才开始回」讲清楚 ----
        // ① 刚好跨过脱战线的那一 tick；② 每秒报一次当前值；③ 回满的那一下
        if (CyberwareConfig.SHIELD_DEBUG_MESSAGES) {
            if (since == CyberwareConfig.SHIELD_COMBAT_DELAY_TICKS) {
                note(player, "§b[护盾] 脱战 " + (CyberwareConfig.SHIELD_COMBAT_DELAY_TICKS / TICKS_PER_SECOND)
                        + " 秒，开始恢复（" + fmt(perSecond) + "/s）");
            } else if (player.tickCount % TICKS_PER_SECOND == 0) {
                note(player, "§b[护盾] 恢复中 " + fmt(next) + "/" + fmt(max));
            }
            if (current < max && next >= max) {
                note(player, "§b[护盾] 已回满 " + fmt(max) + "/" + fmt(max));
            }
        }
    }

    // ═══════════════════ 生命周期 ═══════════════════

    /**
     * 死亡重置：护盾归零并同步。
     *
     * <p>{@code ModAttachments.SHIELD} <b>没有</b> {@code copyOnDeath}，
     * 所以重生后的新实体本来也拿不到旧盾 —— 这里显式清零是为了
     * 「假死（不死图腾）」与「死亡瞬间 HUD 还挂着半管盾」这两种情况。
     */
    public static void onDeath(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        ShieldState.set(player, 0.0D, gameTime(player));
        sync(player);
        Cyberware.LOGGER.debug("[cyberware] {} 死亡，护盾清零", player.getName().getString());
    }

    // ═══════════════════ 同步（与 InstallationPayload 同一套思路） ═══════════════════

    /**
     * 把护盾值推给该玩家的客户端（客户端写回<b>同一个</b>附件）。
     *
     * <p>附件不会自动同步（见 {@code ModAttachments} 注释），所以任何权威值变化之后都要调一次 ——
     * 将来右下角那条青蓝色 HUD 条读的就是客户端这份镜像。
     */
    public static void sync(ServerPlayer player) {
        if (player == null || player.connection == null) {
            return;
        }
        try {
            ShieldState state = ShieldState.of(player);
            ServerPlayNetworking.send(player, new ShieldPayload(
                    state.current(), maxShield(player), state.lastHurtGameTime()));
        } catch (Throwable t) {
            Cyberware.LOGGER.warn("[cyberware] 护盾同步失败（{}）", player.getName().getString(), t);
        }
    }

    // ═══════════════════ 内部工具 ═══════════════════

    /** 取当前游戏刻（玩家不在 ServerLevel 时退回 tickCount，保证单调递增）。 */
    private static long gameTime(Player player) {
        if (player == null) {
            return 0L;
        }
        Level level = player.level();
        return level instanceof ServerLevel serverLevel ? serverLevel.getGameTime() : player.tickCount;
    }

    /**
     * mixin 里的兜底：伤害回调抛异常绝不能带崩服务端主线程，只记一次日志。
     *
     * <p>会走到这里的典型情况：附件/配置在异常状态下被读到。
     */
    public static void reportHookFailure(Throwable t) {
        if (HOOK_FAIL_LOGGED.compareAndSet(false, true)) {
            Cyberware.LOGGER.warn("[cyberware] 护盾伤害拦截异常（已忽略，伤害按原版处理）", t);
        }
    }

    /** 第一次真正拦下伤害时打一条 INFO —— 真机验证「钩子挂上了没有」全靠它。 */
    private static void announceOnce() {
        if (HOOK_ANNOUNCED.compareAndSet(false, true)) {
            Cyberware.LOGGER.info("[cyberware] 护盾拦截已生效（LivingEntity.actuallyHurt @ModifyVariable）");
        }
    }

    /**
     * 真机可观测性：吸收时把当前值打到动作栏（默认开，配置 {@code shield.debugMessages} 可关）。
     *
     * <p>HUD 条做好之后应把这条关掉（主人：HUD 在另一个任务里画）。
     */
    private static void debugMessage(ServerPlayer player, double absorbed, double now,
                                     double max, double remain) {
        if (!CyberwareConfig.SHIELD_DEBUG_MESSAGES) {
            return;
        }
        String text = "§b[护盾] -" + fmt(absorbed) + "  剩余 " + fmt(now) + "/" + fmt(max)
                + (remain > 0.0D ? "  §c穿盾 " + fmt(remain) : "  §a全挡");
        player.sendOverlayMessage(Component.literal(text));
    }

    /** 动作栏一行（可观测性用；发不出去也不能影响逻辑）。 */
    private static void note(ServerPlayer player, String text) {
        try {
            player.sendOverlayMessage(Component.literal(text));
        } catch (Throwable ignored) {
            // 玩家正在断线等情况：忽略
        }
    }

    /** 一位小数，整数就不带小数点（给玩家看的数字）。 */
    private static String fmt(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.format("%.1f", value);
    }
}

