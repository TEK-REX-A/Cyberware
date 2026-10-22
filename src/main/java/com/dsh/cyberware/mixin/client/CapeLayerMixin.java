package com.dsh.cyberware.mixin.client;

import com.dsh.cyberware.client.AfterimageRenderer;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
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
 * <p><b>★ 2026-10-22 改：{@code @Redirect} → MixinExtras {@code @WrapOperation}。</b>
 * 原因见 {@code /sdcard/DSH/MIXIN-CCE-ROOTCAUSE.md}：运行时的 MixinExtras <b>0.5.4</b>
 * 在 {@code FactoryRedirectWrapperMixinTransformer.transform:18} 把 {@code @Redirect} 的
 * {@code at} 当单值强转成 {@code AnnotationNode}，而 sponge-mixin 0.8.7 把
 * {@code Redirect.at()} 改成了 {@code At[]} —— javac 总会把数组型注解成员写成
 * {@code at=[@At(...)]}，于是<b>任何</b> {@code @Redirect} 都会让所属类 transform 时抛
 * {@code ClassCastException: ArrayList cannot be cast to AnnotationNode} → 渲染线程死亡 → 黑屏。
 * 那个 transformer 只扫 {@code @Redirect}，{@code @WrapOperation} 天然绕过。
 *
 * <p>⚠ 这颗雷原本是「哑的」：这天先被加载的是 {@code EquipmentLayerRenderer}，它替
 * {@code CapeLayer} 挡了枪；等玩家进世界真正渲染披风时才会炸 —— 所以必须一起改。
 *
 * <p>匹配不到就安静跳过（{@code require = 0}，配合 {@code defaultRequire = 1} 的教训），
 * 最坏只是「披风没半透明」，绝不会因为注入失败把游戏拖黑。
 * 非残影上下文（{@code ghostAlpha <= 0}）原样走原版调用，本体渲染一个像素都不动。
 *
 * <p>依赖：{@code build.gradle} 需要 MixinExtras（见 EquipmentLayerRendererMixin 的类注释，
 * 两处要同一个 {@code modImplementation include(...)} 配置）。
 */
@Mixin(CapeLayer.class)
public class CapeLayerMixin {

    /**
     * {@code SubmitNodeCollector.submitModel} 是 <b>接口实例方法</b>（INVOKEINTERFACE），
     * 所以处理函数第一个参数是接收者 {@code collector}，后面才是 8 个实参 + {@code Operation}。
     * （依据 MixinExtras 的 {@code getEffectiveArgTypes}：INVOKEINTERFACE/VIRTUAL 会把
     * owner 类型插到 args[0]，INVOKESTATIC 则不插。）
     */
    @WrapOperation(
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
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
            Operation<Void> original) {
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
        // 非残影上下文：原样走那个 8 参数重载（就是被包住的那次调用本身）
        original.call(collector, model, state, poseStack, renderType,
                lightCoords, overlayCoords, outlineColor, crumblingOverlay);
    }
}

