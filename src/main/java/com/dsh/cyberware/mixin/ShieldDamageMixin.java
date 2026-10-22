package com.dsh.cyberware.mixin;

import com.dsh.cyberware.core.ShieldSystem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 护盾 · 伤害拦截（服务端这一半）。
 *
 * <h3>为什么是 {@code actuallyHurt} 而不是 Fabric 事件</h3>
 * <ul>
 *   <li>{@code ServerLivingEntityEvents.ALLOW_DAMAGE} 的签名是
 *       {@code boolean allowDamage(LivingEntity, DamageSource, float)} —— <b>只能放行/取消</b>，
 *       没有任何「把 amount 改成小一点的数」的能力；{@code AFTER_DAMAGE} 是事后回执，
 *       那时血已经扣完、死亡判定也做过了。<b>所以「溢出扣血」必须 mixin。</b></li>
 *   <li>选 {@code actuallyHurt(ServerLevel, DamageSource, float)} 而不是 {@code hurtServer}：
 *       {@code actuallyHurt} <b>只被真正要落地的伤害调用</b>
 *       （{@code hurtServer} 里 invulnerableTime&gt;10 的「只补差额」分支与正常分支各调一次），
 *       且它第一步就 {@code if (isInvulnerableTo) return;} —— 无敌状态下根本不会走到这里；
 *       而 {@code hurtServer} 入口拿到的是护甲减免前的原始值，还会被冷却窗口整条丢弃。</li>
 *   <li>全吸收时把 amount 改成 0：原版 {@code actuallyHurt} 在 {@code if (amount == 0) return;}
 *       处直接返回（26.1.2 字节码偏移 111-117），<b>零副作用</b> —— 不掉血、不加战斗记录、
 *       不发实体伤害游戏事件。</li>
 * </ul>
 *
 * <h3>铁律自查（见 cyberware-mod skill）</h3>
 * <ul>
 *   <li>{@code require = 0}：注入失败（版本变动 / 描述符不符）只是<b>静默退化成「没有护盾」</b>，
 *       绝不让整个 mixin 包 FAILED（那会黑屏）。</li>
 *   <li>方法描述符逐字符取自 {@code javap -p -s}：
 *       {@code (Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V}
 *       （注意：方法名与左括号之间<b>没有</b>冒号）。</li>
 *   <li>方法体整体 try/catch：伤害路径跑在服务端主线程，抛异常会带崩服务器。</li>
 * </ul>
 *
 * <p>这是「公共」mixin（客户端也会应用，因为目标类是 {@code LivingEntity}），
 * 但客户端 {@code this instanceof ServerPlayer} 永远为 false，等于什么都不做。
 */
@Mixin(LivingEntity.class)
public abstract class ShieldDamageMixin {

    /**
     * 把这次伤害先喂给护盾，返回剩余需要落地的伤害。
     *
     * @param amount 原版要应用的伤害
     * @return 护盾吃完剩下的伤害；没有护盾时原样返回
     */
    @ModifyVariable(
            method = "actuallyHurt(Lnet/minecraft/server/level/ServerLevel;"
                    + "Lnet/minecraft/world/damagesource/DamageSource;F)V",
            at = @At("HEAD"),
            argsOnly = true,
            require = 0)
    private float cyberware$absorbWithShield(float amount) {
        try {
            if ((Object) this instanceof ServerPlayer player) {
                return ShieldSystem.absorb(player, amount);
            }
        } catch (Throwable t) {
            ShieldSystem.reportHookFailure(t);
        }
        return amount;
    }
}

