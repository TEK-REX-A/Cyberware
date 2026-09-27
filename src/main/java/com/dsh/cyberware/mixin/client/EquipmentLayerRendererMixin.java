package com.dsh.cyberware.mixin.client;

import com.dsh.cyberware.client.AfterimageRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 让**护甲**跟着残影一起半透明。
 *
 * <p>问题出在渲染类型：主模型走我们指定的半透明类型（吃 alpha），
 * 护甲走的却是 {@code armorCutoutNoCull} —— <b>镂空</b>类型，alpha 只有 0/1 两档，
 * 给多少透明度都当不透明处理。所以「身体透、盔甲实」，整体就像没生效。
 *
 * <p>两手：
 * <ol>
 *   <li>把护甲的渲染类型换成游戏现成的 {@code armorTranslucent}（真半透明）</li>
 *   <li>把提交时的染色乘上残影 alpha</li>
 * </ol>
 * 只在残影渲染上下文生效（{@code AfterimageRenderer.ghostAlpha()} &gt; 0），本体一切照旧。
 *
 * <p>注：染色那步最早写成 {@code @ModifyArg}，但它在 {@code invokeinterface} 的重载方法上
 * 匹配失败（{@code Scanned 0 target(s)}），改用 {@code @Redirect} 自己转发调用。
 */
@Mixin(EquipmentLayerRenderer.class)
public class EquipmentLayerRendererMixin {

    /** 护甲的镂空渲染类型 → 残影上下文里换成半透明版本。 */
    @Redirect(
        method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorCutoutNoCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;")
    )
    private RenderType cyberware$ghostArmorRenderType(Identifier texture) {
        if (AfterimageRenderer.ghostAlpha() > 0.0F) {
            return RenderTypes.armorTranslucent(texture);
        }
        return RenderTypes.armorCutoutNoCull(texture);
    }

    /** 接住护甲的提交，把染色乘上残影透明度，再原样转发。 */
    @Redirect(
        method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V")
    )
    private <S> void cyberware$ghostArmorSubmit(
            OrderedSubmitNodeCollector collector,
            Model<? super S> model, S state, PoseStack poseStack,
            RenderType renderType, int lightCoords, int overlayCoords, int color,
            TextureAtlasSprite sprite, int outlineColor, ModelFeatureRenderer.CrumblingOverlay crumbling) {
        float alpha = AfterimageRenderer.ghostAlpha();
        if (alpha > 0.0F) {
            color = ARGB.multiplyAlpha(color, alpha);
        }
        collector.submitModel(model, state, poseStack, renderType, lightCoords, overlayCoords,
                color, sprite, outlineColor, crumbling);
    }
}
