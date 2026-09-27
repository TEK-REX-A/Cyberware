package com.dsh.cyberware.mixin.client;

import com.dsh.cyberware.client.AfterimageRenderer;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 让**护甲**跟着残影一起半透明。
 *
 * <p>问题出在渲染类型上：主模型用的是我们指定的半透明类型（吃 alpha），
 * 但护甲走的是 {@code armorCutoutNoCull} —— <b>镂空</b>类型。
 * 这种类型的 alpha 只有 0/1 两档，给它多少透明度都当不透明处理，
 * 所以主人看到的是「身体半透明、盔甲实心」，整体就像半透明没生效。
 *
 * <p>两手：
 * <ol>
 *   <li>把护甲的渲染类型换成游戏里现成的 {@code armorTranslucent}（真半透明）</li>
 *   <li>把它的染色也乘上残影的 alpha</li>
 * </ol>
 * 只在残影渲染的上下文里生效 —— {@code AfterimageRenderer.ghostAlpha()} 为 0 时一切照旧。
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

    /** 护甲的染色也乘上残影透明度（第 7 个参数 = color）。 */
    @ModifyArg(
        method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"),
        index = 6
    )
    private int cyberware$ghostArmorColor(int color) {
        float alpha = AfterimageRenderer.ghostAlpha();
        if (alpha <= 0.0F) {
            return color;
        }
        return ARGB.multiplyAlpha(color, alpha);
    }
}
