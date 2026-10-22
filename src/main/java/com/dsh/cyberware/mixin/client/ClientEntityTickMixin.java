package com.dsh.cyberware.mixin.client;

import com.dsh.cyberware.client.ClientTimeDilation;
import com.dsh.cyberware.core.DilationTickGate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 时间减缓 · 投射物跳 tick（<b>客户端这一半</b>）——
 * Fabric 侧替代 NeoForge 的 {@code EntityTickEvent.Pre}（原实现见
 * {@code client/ClientProjectileDilation.onEntityTickPre}，:27-41）。
 *
 * <p>投射物在客户端也会自己 tick，服务端位置包只是事后校正；只让服务端跳，玩家看到的
 * 还是客户端那支原速的箭。所以客户端必须一起跳。
 *
 * <p>相位由 {@link DilationTickGate} 用「游戏刻 + 实体 id」算 —— 与
 * {@code EntityTickMixin} 完全同一套，两端跳的刻对齐。
 *
 * <p>本类只在客户端加载（{@code cyberware.mixins.json} 的 client 段），
 * 因此可以直接引用 {@link ClientTimeDilation}；服务端不会看到这个类。
 */
@Mixin(Entity.class)
public abstract class ClientEntityTickMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0)
    private void cyberware$skipTickWhileDilatedClient(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self.level() == null || !self.level().isClientSide()) {
            return;   // 服务端那一半在 mixin/EntityTickMixin
        }
        if (!(self instanceof Projectile projectile)) {
            return;
        }
        if (projectile.getOwner() instanceof Player) {
            return;
        }
        double timeScale = ClientTimeDilation.timeScaleAt(self.getX(), self.getY(), self.getZ());
        if (DilationTickGate.shouldSkip(self, timeScale)) {
            ci.cancel();
        }
    }
}
