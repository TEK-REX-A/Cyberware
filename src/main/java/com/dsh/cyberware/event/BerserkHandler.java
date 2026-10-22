package com.dsh.cyberware.event;

import com.dsh.cyberware.Cyberware;
import com.dsh.cyberware.config.CyberwareConfig;
import com.dsh.cyberware.core.BerserkManager;
import com.dsh.cyberware.network.BerserkPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * 狂暴 · 服务端效果（Fabric 版）。
 *
 * <p><b>与 NeoForge 版的三处结构差异</b>
 * <ol>
 *   <li>{@code EntityTickEvent.Post} → {@code ServerTickEvents.END_LEVEL_TICK} 遍历玩家；</li>
 *   <li>发包 {@code PacketDistributor.sendToPlayer} → {@link ServerPlayNetworking#send}；</li>
 *   <li><b>伤害翻倍不再是「改伤害值」</b>：Fabric 的 {@code ALLOW_DAMAGE} 只能允许/拒绝，
 *       给不出改动量的钩子。这里拆成两步 —— 无敌走 {@code ALLOW_DAMAGE} 返回 false，
 *       翻倍走 {@code AFTER_DAMAGE} 后补一次伤害（{@link #onAfterDamage}），并用
 *       {@link #APPLYING} 防递归。<b>语义差异：补的那一下是独立伤害事件</b>
 *       （会各自触发受击动画/无敌帧判定），与「一次伤害乘 2」不完全等价，标未验证。</li>
 * </ol>
 */
public final class BerserkHandler {

    private BerserkHandler() {
    }

    /** 补伤害时的递归护栏（AFTER_DAMAGE 里再打一次伤害会再次触发 AFTER_DAMAGE）。 */
    private static final ThreadLocal<Boolean> APPLYING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /** 把状态单播给玩家本人。 */
    public static void broadcast(ServerPlayer player) {
        boolean active = BerserkManager.isActive(player);
        ServerPlayNetworking.send(player, new BerserkPayload(
                active ? BerserkManager.remainingTicks(player) : 0,
                active ? BerserkManager.totalTicks(player) : 0,
                (float) BerserkManager.damageMultiplier(player),
                active));
    }

    /** 周期性同步（打给维度里所有玩家，只有真在狂暴的人会被通知到）。 */
    public static void refresh(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        boolean anyActive = false;
        for (ServerPlayer player : serverLevel.players()) {
            if (BerserkManager.isActive(player)) {
                anyActive = true;
                broadcast(player);
            }
        }
        if (!anyActive) {
            BerserkManager.prune();
        }
    }

    /** 攻击速度加成用的修饰符 id。 */
    private static final Identifier ATTACK_SPEED_ID =
            Identifier.fromNamespaceAndPath(Cyberware.MODID, "berserk_attack_speed");
    /** 移动速度加成用的修饰符 id。 */
    private static final Identifier MOVEMENT_SPEED_ID =
            Identifier.fromNamespaceAndPath(Cyberware.MODID, "berserk_movement_speed");

    /** 每刻末：逐玩家校核属性修饰符（幂等，死亡/换维度后自动补挂）。 */
    public static void onLevelTickEnd(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            boolean active = BerserkManager.isActive(player);
            applyModifier(player, Attributes.ATTACK_SPEED, ATTACK_SPEED_ID,
                    CyberwareConfig.BERSERK_ATTACK_SPEED_BONUS, active);
            applyModifier(player, Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED_ID,
                    CyberwareConfig.BERSERK_MOVEMENT_SPEED_BONUS, active);
        }
    }

    /** 挂上/摘掉一条属性修饰符（幂等）。 */
    private static void applyModifier(ServerPlayer player, Holder<Attribute> attribute,
                                      Identifier id, double bonus, boolean active) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        boolean applied = instance.hasModifier(id);
        if (active && bonus > 0.0D) {
            if (!applied) {
                instance.addTransientModifier(new AttributeModifier(
                        id, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        } else if (applied) {
            instance.removeModifier(id);
        }
    }

    /** 无敌：狂暴中的玩家不吃任何伤害。 */
    public static boolean allowDamage(LivingEntity victim, DamageSource source) {
        if (!(victim instanceof ServerPlayer player)) {
            return true;
        }
        if (!CyberwareConfig.BERSERK_INVULNERABLE) {
            return true;
        }
        return !BerserkManager.isActive(player);
    }

    /** 伤害翻倍：攻击者正在狂暴 → 事后补一次等量差额（见类注释的语义差异说明）。 */
    public static void onAfterDamage(LivingEntity victim, DamageSource source,
                                     float baseDamage, float damageTaken, boolean blocked) {
        if (APPLYING.get()) {
            return;
        }
        if (!(source.getEntity() instanceof ServerPlayer berserker) || berserker == victim) {
            return;
        }
        if (!BerserkManager.isActive(berserker)) {
            return;
        }
        double multiplier = BerserkManager.damageMultiplier(berserker);
        if (multiplier <= 1.0D) {
            return;
        }
        float extra = (float) (damageTaken * (multiplier - 1.0D));
        if (extra <= 0.0F) {
            return;
        }
        if (!(victim.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        APPLYING.set(Boolean.TRUE);
        try {
            victim.hurtServer(serverLevel, source, extra);
        } finally {
            APPLYING.set(Boolean.FALSE);
        }
    }

    /** 击杀：延长时间 + 回血。 */
    public static void onLivingDeath(LivingEntity entity, DamageSource source) {
        Entity killer = source.getEntity();
        if (!(killer instanceof ServerPlayer player) || entity == player) {
            return;
        }
        if (!BerserkManager.isActive(player)) {
            return;
        }

        int added = BerserkManager.extend(player,
                CyberwareConfig.BERSERK_KILL_EXTEND_TICKS,
                CyberwareConfig.BERSERK_KILL_EXTEND_LIMIT);

        double hearts = CyberwareConfig.BERSERK_KILL_HEAL_HEARTS;
        if (hearts > 0.0D && player.getHealth() < player.getMaxHealth()) {
            player.heal((float) (hearts * 2.0D));
        }

        if (added > 0) {
            player.sendOverlayMessage(Component.literal(
                    "狂暴 +" + trimSeconds(added) + "s  §c❤ +" + trimHearts(hearts))
                    .withStyle(ChatFormatting.RED));
        }
    }

    private static String trimSeconds(int ticks) {
        double seconds = ticks / 20.0D;
        return seconds == Math.floor(seconds)
                ? String.valueOf((long) seconds)
                : String.format(java.util.Locale.ROOT, "%.1f", seconds);
    }

    private static String trimHearts(double hearts) {
        return hearts == Math.floor(hearts) ? String.valueOf((long) hearts)
                : String.format(java.util.Locale.ROOT, "%.1f", hearts);
    }
}
