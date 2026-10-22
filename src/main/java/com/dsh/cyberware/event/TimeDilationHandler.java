package com.dsh.cyberware.event;

import com.dsh.cyberware.config.CyberwareConfig;
import com.dsh.cyberware.core.DilationTickGate;
import com.dsh.cyberware.core.TimeDilationManager;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;

/**
 * 时间减缓 · 服务端逻辑层（Fabric 版）。
 *
 * <p><b>移植说明</b>：NeoForge 有「每实体每刻」的 {@code EntityTickEvent.Pre/Post}，
 * Fabric 没有等价事件 —— 这里改成由 {@code ServerTickEvents.END_LEVEL_TICK} 每刻遍历一次
 * 该维度的实体（{@link ServerLevel#getAllEntities()}），语义等价、代价是每刻一次迭代。
 *
 * <p><b>已知语义损失（未验证项，见报告）</b>：原版用 {@code EntityTickEvent.Pre#setCanceled(true)}
 * 让投射物「整刻不 tick」（位置/碰撞/寿命一起慢）。Fabric 侧没有取消实体 tick 的公开钩子，
 * 这里退化成「tick 结束后把位移整体抹掉」——位置对了，但**该刻的碰撞与寿命判定已经发生**。
 * 要完全等价需要服务端 mixin（属渲染/mixin scope，本文件不涉及）。
 */
public final class TimeDilationHandler {

    private TimeDilationHandler() {
    }

    /** 上次刷新减速区域的游戏刻（按维度记，避免多维度互相顶掉）。 */
    private static final Map<ResourceKey<Level>, Long> LAST_REFRESH = new HashMap<>();

    /** 每刻末：每 20 刻刷新一次减速区域，然后逐实体处理。 */
    public static void onLevelTickEnd(ServerLevel level) {
        long gameTime = level.getGameTime();
        Long last = LAST_REFRESH.get(level.dimension());
        if (gameTime % 20L == 0L && (last == null || last.longValue() != gameTime)) {
            LAST_REFRESH.put(level.dimension(), gameTime);
            TimeDilationManager.refresh(level);
            BerserkHandler.refresh(level);
        }
        for (Entity entity : (Iterable<Entity>) level.getAllEntities()) {
            tickEntity(entity);
        }
    }

    private static void tickEntity(Entity entity) {
        // ---- 投射物：跳 tick 的降级实现（见类注释）----
        if (entity instanceof Projectile projectile && !(projectile.getOwner() instanceof Player)) {
            double projectileScale = TimeDilationManager.timeScaleFor(entity);
            if (DilationTickGate.shouldSkip(entity, projectileScale)) {
                entity.setPos(entity.xo, entity.yo, entity.zo);
                return;
            }
        }

        if (!shouldAffect(entity)) {
            return;
        }

        double timeScale = TimeDilationManager.timeScaleFor(entity);
        if (timeScale >= 1.0D) {
            return;
        }

        // 无敌帧清零（主人指定）
        if (entity instanceof LivingEntity living && living.invulnerableTime > 0) {
            living.invulnerableTime = 0;
        }

        // ---- 位置回拉：把这一 tick 的位移按倍率缩回去 ----
        double dx = entity.getX() - entity.xo;
        double dy = entity.getY() - entity.yo;
        double dz = entity.getZ() - entity.zo;
        if (dx == 0.0D && dy == 0.0D && dz == 0.0D) {
            return;
        }
        entity.setPos(entity.xo + dx * timeScale,
                      entity.yo + dy * timeScale,
                      entity.zo + dz * timeScale);
    }

    /** 参与减速的对象与豁免规则（与 NeoForge 版逐字一致）。 */
    private static boolean shouldAffect(Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return false;
        }
        if (entity instanceof Player) {
            return false;
        }
        return !(entity.getFirstPassenger() instanceof Player);
    }

    /**
     * 子弹时间：斯安威斯坦激活期间弹射物打不中你。
     *
     * <p>对应 NeoForge 的 {@code LivingIncomingDamageEvent#setCanceled} ——
     * Fabric 侧用 {@code ServerLivingEntityEvents.ALLOW_DAMAGE} 返回 false。
     */
    public static boolean allowDamage(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof ServerPlayer player)) {
            return true;
        }
        if (!CyberwareConfig.TIME_DILATION_PROJECTILE_IMMUNE) {
            return true;
        }
        if (!TimeDilationManager.isOwnDilationActive(player)) {
            return true;
        }
        boolean fromProjectile = source.getDirectEntity() instanceof Projectile
                || source.getEntity() instanceof Projectile;
        return !fromProjectile;
    }

    /** 击杀延长（对应 NeoForge 的 {@code LivingDeathEvent}）。 */
    public static void onLivingDeath(LivingEntity entity, DamageSource source) {
        if (!CyberwareConfig.TIME_DILATION_KILL_EXTEND_ENABLED) {
            return;
        }
        Entity killer = source.getEntity();
        if (!(killer instanceof ServerPlayer player) || entity == player) {
            return;
        }
        int added = TimeDilationManager.extend(player,
                CyberwareConfig.TIME_DILATION_KILL_EXTEND_TICKS,
                CyberwareConfig.TIME_DILATION_KILL_EXTEND_LIMIT);
        if (added > 0) {
            player.sendOverlayMessage(Component.literal("斯安威斯坦 +" + trimSeconds(added) + "s")
                    .withStyle(ChatFormatting.AQUA));
            // 击杀音效（新功能）：added > 0 就证明击杀发生时玩家处于减速中，
            // 在尸体位置广播，附近玩家都能听到。
            if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        com.dsh.cyberware.registry.ModSounds.SANDEVISTAN_KILL.get(),
                        net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
    }

    private static String trimSeconds(int ticks) {
        double seconds = ticks / 20.0D;
        return seconds == Math.floor(seconds)
                ? String.valueOf((long) seconds)
                : String.format(java.util.Locale.ROOT, "%.1f", seconds);
    }
}
