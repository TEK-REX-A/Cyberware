package com.dsh.cyberware.event;

import com.dsh.cyberware.Cyberware;
import com.dsh.cyberware.config.CyberwareConfig;
import com.dsh.cyberware.core.BerserkManager;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import com.dsh.cyberware.network.BerserkPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * 狂暴 · 服务端效果。
 *
 * <p>四条规则，对应「把身体感知关掉」这个设定：
 * <ul>
 *   <li><b>无敌</b> —— 狂暴期间免疫一切伤害（痛觉被抑制）</li>
 *   <li><b>伤害翻倍</b> —— 输出的伤害乘以倍率</li>
 *   <li><b>击杀延长</b> —— 每击杀一个敌人，持续时间 +1.5 秒（可配置）</li>
 *   <li><b>击杀治疗</b> —— 每击杀一个敌人回复 2 颗心</li>
 * </ul>
 */
public final class BerserkHandler {

    private BerserkHandler() {
    }

    /** 把状态单播给玩家本人（狂暴是自己的状态，不需要广播全维度）。 */
    public static void broadcast(ServerPlayer player) {
        boolean active = BerserkManager.isActive(player);
        PacketDistributor.sendToPlayer(player, new BerserkPayload(
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
        for (ServerPlayer player : serverLevel.players()) {
            if (BerserkManager.isActive(player)) {
                broadcast(player);
            } else {
                // prune() 不再需要 level 参数：过期判断一律用每条记录**自己的**世界时钟
                BerserkManager.prune();
            }
        }
    }

    /** 攻击速度加成用的修饰符 id（挂上/摘掉都靠它认）。 */
    private static final Identifier ATTACK_SPEED_ID =
            Identifier.fromNamespaceAndPath(Cyberware.MODID, "berserk_attack_speed");
    /** 移动速度加成用的修饰符 id。 */
    private static final Identifier MOVEMENT_SPEED_ID =
            Identifier.fromNamespaceAndPath(Cyberware.MODID, "berserk_movement_speed");

    /**
     * 狂暴期间攻击速度加快。
     *
     * <p>用属性修饰符而不是直接改攻击冷却：{@code ATTACK_SPEED} 决定攻击条的充能速度，
     * 挂 {@code ADD_MULTIPLIED_BASE} 就能让挥砍频率整体变快，收招时摘掉即可复原。
     * 每 tick 校核一次（纯属性查表，开销可忽略）：玩家死亡重生、换维度都会重置属性，
     * 靠这个循环自动补挂；狂暴一结束也立刻摘掉，不留尾巴。
     */
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        boolean active = BerserkManager.isActive(player);
        // 挥砍更快
        applyModifier(player, Attributes.ATTACK_SPEED, ATTACK_SPEED_ID,
                CyberwareConfig.BERSERK_ATTACK_SPEED_BONUS.get(), active);
        // 跑得更快
        applyModifier(player, Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED_ID,
                CyberwareConfig.BERSERK_MOVEMENT_SPEED_BONUS.get(), active);
    }

    /** 挂上/摘掉一条属性修饰符（幂等：状态已经对了就什么都不做）。 */
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

    /** 无敌 + 伤害翻倍。 */
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        // —— 无敌：狂暴中的玩家不吃任何伤害 ——
        if (event.getEntity() instanceof ServerPlayer victim
                && BerserkManager.isActive(victim)
                && CyberwareConfig.BERSERK_INVULNERABLE.get()) {
            event.setCanceled(true);
            return;
        }

        // —— 伤害翻倍：攻击者正在狂暴 ——
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof ServerPlayer berserker && BerserkManager.isActive(berserker)) {
            double multiplier = BerserkManager.damageMultiplier(berserker);
            if (multiplier > 1.0D) {
                event.setAmount((float) (event.getAmount() * multiplier));
            }
        }
    }

    /** 击杀：延长时间 + 回血。 */
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        Entity killer = event.getSource().getEntity();
        if (!(killer instanceof ServerPlayer player) || event.getEntity() == player) {
            return;
        }
        if (!BerserkManager.isActive(player)) {
            return;
        }

        // 延长
        int added = BerserkManager.extend(player,
                CyberwareConfig.BERSERK_KILL_EXTEND_TICKS.get(),
                CyberwareConfig.BERSERK_KILL_EXTEND_LIMIT.get());

        // 回血：击杀治疗（1 颗心 = 2 点生命）
        double hearts = CyberwareConfig.BERSERK_KILL_HEAL_HEARTS.get();
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
