package com.dsh.cyberware.mixin.client;

import com.dsh.cyberware.client.ClientProjectileDilation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 投射物时间减缓 · <b>Fabric 侧的每刻跳过点</b>。
 *
 * <p>NeoForge 用 {@code EntityTickEvent.Pre} + {@code event.setCanceled(true)} 实现
 * 「相位对齐地跳过这一 tick」；Fabric 没有等价事件（本机实测：模块清单里没有实体 tick 前事件）。
 * 因此注入原版 {@code Entity.tick()V} 的 HEAD 并取消。
 *
 * <p><b>覆盖面（本机 javap 实测）</b>：{@code Projectile.tick()} 内部以
 * {@code invokespecial Entity.tick} 调 super，而 {@code Arrow} / {@code ThrownTrident} /
 * {@code Snowball} 都<b>没有</b>覆写 {@code tick()}，所以它们全部经过这里。
 * 若将来某个投射物子类覆写 {@code tick()} 且不调 super，本注入不会触发 —— 这一条未逐一验证。
 *
 * <p>{@code require = 0} + try/catch：跳过失败的最坏结果是「箭不减速」，不该崩客户端。
 */
@Mixin(Entity.class)
public abstract class EntityTickSkipMixin {

    @Inject(method = "tick()V", at = @At("HEAD"), cancellable = true, require = 0)
    private void cyberware$skipDilatedProjectileTick(CallbackInfo ci) {
        try {
            if (ClientProjectileDilation.shouldCancelTick((Entity) (Object) this)) {
                ci.cancel();
            }
        } catch (Throwable ignored) {
            // 同 ProjectileMixin 的既有约定：装饰/节奏类功能失败不影响游戏
        }
    }
}

