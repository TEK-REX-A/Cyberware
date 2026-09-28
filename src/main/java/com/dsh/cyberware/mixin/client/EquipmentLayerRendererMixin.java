package com.dsh.cyberware.mixin.client;

import com.dsh.cyberware.client.AfterimageRenderer;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 让**护甲**跟着残影一起半透明。
 *
 * <p>问题出在渲染类型：主模型走我们指定的半透明类型（吃 alpha），
 * 护甲走的却是 {@code armorCutoutNoCull} —— <b>镂空</b>类型，alpha 只有 0/1 两档，
 * 给多少透明度都当不透明处理。所以「身体透、盔甲实」，整体看着像没生效。
 *
 * <p>两手：
 * <ol>
 *   <li>把护甲的镂空类型换成游戏现成的 {@code armorTranslucent}</li>
 *   <li>把染色值乘上残影 alpha —— 拦的是扩展点 {@code getArmorLayerTintColor}，
 *       它在这个方法里只有一处调用，签名也没有泛型</li>
 * </ol>
 *
 * <p><b>为什么不去拦 {@code submitModel}：</b>那个调用在同一方法里有三处（护甲材质 + 附魔光效），
 * {@code @Redirect} 匹配不上，日志里是 {@code Scanned 0 target(s)}，
 * 而 {@code injectors.defaultRequire = 1} 会让整个 mixin 失败 → 游戏黑屏。
 * 这里给两处注入都加上 {@code require = 0}：匹配不到就安静跳过，
 * 最坏情况只是「护甲没半透明」，绝不会再把游戏拖黑。

 * 只在残影渲染上下文生效（{@code ghostAlpha > 0}），本体一切照旧。
 */
@Mixin(EquipmentLayerRenderer.class)
public class EquipmentLayerRendererMixin {

    /** 诊断日志去重：每种护甲层只打印一次，免得每帧 4 条刷爆日志 */
    private static final java.util.Set<String> LOGGED = new java.util.HashSet<>();

    /** 护甲的镂空渲染类型 → 残影上下文换成半透明版本。 */
    @Redirect(
        method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
        require = 0,
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorCutoutNoCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;")
    )
    private RenderType cyberware$ghostArmorRenderType(Identifier texture) {
        if (AfterimageRenderer.ghostAlpha() > 0.0F) {
            // 用主模型同一个渲染类型 —— 主人已经验证过它确实吃 alpha。
            // 早先换的是游戏自带的 armorTranslucent，它是给「皮革护甲」准备的特殊类型，
            // 对下界合金甲这种三层不透明材质并不生效（实测只有头盔看着变了）。
            return RenderTypes.entityTranslucentCullItemTarget(texture);
        }
        return RenderTypes.armorCutoutNoCull(texture);
    }

    /** 染色值乘上残影 alpha —— 拦扩展点，全方法只此一处。 */
    @Redirect(
        method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
        require = 0,
        at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/client/extensions/common/IClientItemExtensions;getArmorLayerTintColor(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/resources/model/EquipmentClientInfo$Layer;II)I")
    )
    private int cyberware$ghostArmorColor(IClientItemExtensions extensions, ItemStack stack,
                                          EquipmentClientInfo.Layer layer, int index, int dyeColor) {
        int color = extensions.getArmorLayerTintColor(stack, layer, index, dyeColor);
        float alpha = AfterimageRenderer.ghostAlpha();
        if (alpha <= 0.0F) {
            return color;
        }
        if (LOGGED.add(String.valueOf(layer))) {
            // 只打印一次：每帧 4 个部位都打会把日志刷爆
            System.out.println("[cyberware] 护甲残影染色生效: alpha=" + alpha
                    + " color=" + Integer.toHexString(color));
        }
        return ARGB.multiplyAlpha(color, alpha);
    }
}
