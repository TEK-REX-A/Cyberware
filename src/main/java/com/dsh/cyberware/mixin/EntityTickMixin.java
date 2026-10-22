package com.dsh.cyberware.mixin;

import com.dsh.cyberware.core.DilationTickGate;
import com.dsh.cyberware.core.TimeDilationManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 时间减缓 · 投射物跳 tick（服务端这一半）—— Fabric 侧替代 NeoForge 的 {@code EntityTickEvent.Pre}。
 *
 * <p><b>为什么注入 {@code Entity.tick()} 而不是 {@code Projectile.tick()}</b>：
 * NeoForge 的 {@code EntityTickEvent.Pre} 是打在 {@code Entity.tick()} 开头的，
 * 所以注入点选 {@code Entity.tick()} 能拿到<b>与移植前逐字等价</b>的语义。26.1.2 里
 * {@code Projectile} 及其 15 个子类（Arrow / SpectralArrow / ThrownTrident / WindCharge /
 * ThrowableProjectile / AbstractHurtingProjectile / FireworkRocketEntity / ShulkerBullet /
 * LlamaSpit / EyeOfEnder / EvokerFangs / FishingHook …）都各自 override 了 {@code tick()V}，
 * 但它们最终都会走到 {@code super.tick()} 链上的 {@code Entity.tick()} —— 与 NeoForge 一致。
 *
 * <p>判定逻辑逐条照抄 {@code event/TimeDilationHandler.onEntityTickPre}（:83-99）：
 * 只处理投射物、玩家自己射出的豁免、相位由 {@link DilationTickGate} 用「游戏刻 + 实体 id」
 * 算出来，保证两端跳的是同一批刻。
 *
 * <p><b>require = 0</b>：铁律，任一注入失败不能让整包 mixin 崩掉。
 */
@Mixin(Entity.class)
public abstract class EntityTickMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0)
    private void cyberware$skipTickWhileDilated(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self.level() == null || self.level().isClientSide()) {
            return;   // 客户端那一半在 mixin/client/ClientEntityTickMixin
        }
        if (!(self instanceof Projectile projectile)) {
            return;   // 只有投射物跳 tick；生物走「位置回拉」
        }
        if (projectile.getOwner() instanceof Player) {
            return;   // 玩家自己射的箭豁免
        }
        double timeScale = TimeDilationManager.timeScaleFor(self);
        if (DilationTickGate.shouldSkip(self, timeScale)) {
            ci.cancel();
        }
    }
}
