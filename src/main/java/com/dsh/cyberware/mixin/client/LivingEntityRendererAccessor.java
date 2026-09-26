package com.dsh.cyberware.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 把 {@link LivingEntityRenderer} 里那几个 protected 成员借出来，给残影渲染用。
 *
 * <p>为什么要它们：原版渲染一个生物时，除了 {@code submitModel} 之外还有三件事 ——
 * <ol>
 *   <li>{@code setupRotations}（按身体朝向旋转）</li>
 *   <li>{@code poseStack.scale(-1, -1, 1)}（Minecraft 模型是上下反的，漏了就会<b>倒立</b>）</li>
 *   <li>遍历 {@code layers} 提交护甲、手持物（漏了就会<b>裸体</b>）</li>
 * </ol>
 * 这三样都要按原版的顺序复刻，残影才和本体长得一模一样。
 */
@Mixin(LivingEntityRenderer.class)
public interface LivingEntityRendererAccessor {

    @Invoker("setupRotations")
    void cyberware$setupRotations(LivingEntityRenderState state, PoseStack poseStack, float bodyRot, float scale);

    @Invoker("scale")
    void cyberware$scale(LivingEntityRenderState state, PoseStack poseStack);

    @Accessor("layers")
    List<RenderLayer<LivingEntityRenderState, ?>> cyberware$layers();
}
