package com.dsh.cyberware.mixin.client;

import com.dsh.cyberware.client.post.SandevistanPostProcessor;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 斯安威斯坦 · 激活期 FOV 拉伸的 <b>Fabric 注入点</b>。
 *
 * <p>NeoForge 用的是 {@code ComputeFovModifierEvent}；Fabric 没有等价事件
 * （本机实测：fabric-api 0.155.3+26.1.2 的 41 个模块、fabric-rendering-v1 的 33 个 API 类
 * 里都没有 FOV 回调）。所以改为拦原版方法的返回值。
 *
 * <p>目标（描述符来自 javap -s）：
 * <pre>
 * net.minecraft.client.player.AbstractClientPlayer
 *   public float getFieldOfViewModifier(boolean, float)
 *   descriptor: (ZF)F
 * </pre>
 * 选 {@code AbstractClientPlayer} 而不是 {@code LocalPlayer} 的理由（本机实测）：
 * {@code LocalPlayer} 与 {@code Player} <b>都没有</b>覆写这个方法，所以本地玩家必然走到这里。
 *
 * <p>{@code require = 0} + 整体 try/catch：FOV 只是观感，出任何问题都只该「没有拉伸」，
 * 不该崩客户端。
 */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {

    /** 乘上斯安威斯坦的 FOV 增益；未激活时原样返回，不动一个像素。 */
    @Inject(
        // ⚠ 方法名与左括号之间绝对不能有冒号（javap 显示用的冒号不是描述符的一部分）
        method = "getFieldOfViewModifier(ZF)F",
        at = @At("RETURN"),
        cancellable = true,
        require = 0
    )
    private void cyberware$applySandevistanFov(boolean firstPerson, float partialTick,
                                               CallbackInfoReturnable<Float> cir) {
        try {
            float original = cir.getReturnValue();
            float modified = SandevistanPostProcessor.applyFovModifier(original);
            if (modified != original) {
                cir.setReturnValue(modified);
            }
        } catch (Throwable ignored) {
            // 观感功能失败不该让玩家买单，也不刷日志（这个方法每帧会被调用多次）
        }
    }
}

