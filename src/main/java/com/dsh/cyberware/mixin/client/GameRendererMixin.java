package com.dsh.cyberware.mixin.client;

import com.dsh.cyberware.client.post.SandevistanPostProcessor;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 后处理的执行点 —— 直接贴在**原版自己**那一段后面。
 *
 * <p>原版 {@code GameRenderer.render()} 里本来就有这么一段：
 * <pre>
 *   this.renderLevel(deltaTracker);
 *   this.tryTakeScreenshotIfNeeded();
 *   this.minecraft.levelRenderer.doEntityOutline();
 *   if (this.postEffectId != null &amp;&amp; this.effectActive) {
 *       postChain.process(this.minecraft.getMainRenderTarget(), this.resourcePool);   // ← 原版后处理
 *   }
 *   ...
 *   profiler.push("gui");        // GUI 从这里才开始
 * </pre>
 *
 * <p>这正是「世界画完、GUI 未画」的窗口，而且用的是 {@code resourcePool}（跨帧资源池），
 * 不是我们之前硬凑的 UNPOOLED。既然原版的蜘蛛/苦力怕后处理在这台机器上是正常的，
 * 那就说明这条路径有效 —— 我们站到同一个位置上就行了。
 *
 * <p>注入点选在 {@code doEntityOutline()} 调用之后，紧跟原版那一小段。
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Shadow
    private CrossFrameResourcePool resourcePool;

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;doEntityOutline()V",
                    shift = At.Shift.AFTER
            ),
            require = 0
    )
    private void cyberware$applyPostEffect(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        SandevistanPostProcessor.applyPostProcess(this.resourcePool);
    }
}
