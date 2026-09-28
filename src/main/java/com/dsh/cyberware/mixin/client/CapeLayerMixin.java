package com.dsh.cyberware.mixin.client;

import com.dsh.cyberware.client.AfterimageRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 让**披风**跟着残影一起半透明。
 *
 * <p>披风在原版走 {@code RenderTypes.entitySolid(...)} —— <b>实体不透明类型</b>，
 * 一点混合都没有。所以会出现「身体是透的、披风还是实心的」。
 *
 * <p>更麻烦的是它调的是 {@code submitModel} 的 8 参数重载，
 * 而那个重载把 {@code tintedColor} <b>硬编码成了 -1</b>（纯不透明白）——
 * 也就是说 <b>光换渲染类型没用</b>，顶点色里的 alpha 依然是 1.0。
 * 所以这里把整个调用拦下来，改调 10 参数版本，自己把 alpha 写进 {@code tintedColor}。
 *
 * <p>披风纹理直接从 {@code state.skin.cape().texturePath()} 取，不需要额外传参。
 *
 * <p>两点都加 {@code require = 0}：匹配不到就安静跳过，最坏只是「披风没半透明」，
 * 绝不会因为注入失败把游戏拖黑（{@code defaultRequire = 1} 的教训）。
 * 非残影上下文（{@code ghostAlpha <= 0}）原样走原版调用，本体渲染一个像素都不动。
 */
@Mixin(CapeLayer.class)
public class CapeLayerMixin {

    @Redirect(
        method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
        require = 0,
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V")
    )
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void cyberware$ghostCape(
            SubmitNodeCollector collector,
            Model<?> model,
            Object state,
            PoseStack poseStack,
            RenderType renderType,
            int lightCoords,
            int overlayCoords,
            int outlineColor,
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        float alpha = AfterimageRenderer.ghostAlpha();
        Identifier capeTexture = state instanceof AvatarRenderState avatar
                && avatar.skin != null
                && avatar.skin.cape() != null
                ? avatar.skin.cape().texturePath()
                : null;

        if (alpha > 0.0F && capeTexture != null) {
            // 半透明类型 + 顶点色带 alpha（原版这里是硬编码的 -1）
            int color = ARGB.color(Math.round(Math.min(1.0F, alpha) * 255.0F), 255, 255, 255);
            collector.submitModel((Model) model, state, poseStack,
                    RenderTypes.entityTranslucentCullItemTarget(capeTexture),
                    lightCoords, overlayCoords, color, null, outlineColor, crumblingOverlay);
            return;
        }
        // 非残影上下文：原样走那个 8 参数重载
        collector.submitModel((Model) model, state, poseStack, renderType,
                lightCoords, overlayCoords, outlineColor, crumblingOverlay);
    }
}
