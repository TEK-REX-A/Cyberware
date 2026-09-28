package com.dsh.cyberware.mixin.client;

import com.dsh.cyberware.client.AfterimageRenderer;
import net.minecraft.client.renderer.Sheets;
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
import org.spongepowered.asm.mixin.injection.ModifyArg;
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
 *   <li>把护甲的镂空类型换成游戏现成的 {@code armorTranslucent}（护甲专用：保留
 *       {@code VIEW_OFFSET_Z_LAYERING} 分层与 {@code PER_FACE_LIGHTING}，且有真混合）</li>
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
    /** 诊断日志去重：渲染类型替换只打印一次 */
    private static final java.util.Set<String> RT_LOGGED = new java.util.HashSet<>();
    /** 诊断日志去重：纹饰顶点色只打印一次 */
    private static final java.util.concurrent.atomic.AtomicBoolean TRIM_LOGGED =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    /** 护甲的镂空渲染类型 → 残影上下文换成半透明版本。 */
    @Redirect(
        method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
        require = 0,
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorCutoutNoCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;")
    )
    private RenderType cyberware$ghostArmorRenderType(Identifier texture) {
        if (AfterimageRenderer.ghostAlpha() > 0.0F) {
            // ★ 必须用**护甲专用**的半透明类型，不能拿主模型那个去套：
            //   1) armorTranslucent 保留了 VIEW_OFFSET_Z_LAYERING —— 护甲就靠这个视图空间
            //      Z 偏移浮在皮肤上面。换成实体类型就丢了分层，护甲会陷进皮肤里（z-fighting）。
            //   2) 它是 PER_FACE_LIGHTING（护甲的逐面光照），实体类型不是，光照会突变。
            //   3) entityTranslucentCullItemTarget 还带 OutputTarget.ITEM_ENTITY_TARGET
            //      （物品实体的渲染目标），用在护甲上完全是别的去处。
            //   两者都有 BlendFunction.TRANSLUCENT，所以都能真混合。
            RenderType replaced = RenderTypes.armorTranslucent(texture);
            if (RT_LOGGED.add(texture.toString())) {
                System.out.println("[cyberware] 护甲渲染类型已替换: " + texture
                        + " | alpha=" + AfterimageRenderer.ghostAlpha()
                        + " | blending=" + replaced.hasBlending()
                        + " | 原类型blending=" + RenderTypes.armorCutoutNoCull(texture).hasBlending());
            }
            return replaced;
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

    /**
     * 盔甲纹饰（armor trim）的图集类型 → 残影上下文换成半透明的那个。
     *
     * <p>纹饰走的是 {@code Sheets.armorTrimsSheet(...)}，它返回的是两个<b>静态常量</b>
     * （{@code ARMOR_TRIMS_SHEET_TYPE = armorCutoutNoCull(ARMOR_TRIMS_SHEET)}）——
     * 也就是说它虽然也叫 armorCutoutNoCull，却是在 {@code Sheets} 的静态初始化里造好的，
     * 跟 {@code renderLayers} 里那个调用点不是同一处，所以上面那个 @Redirect 够不着它。
     * 这里直接拦 {@code armorTrimsSheet} 的返回值。
     */
    @Redirect(
        method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
        require = 0,
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/Sheets;armorTrimsSheet(Z)Lnet/minecraft/client/renderer/rendertype/RenderType;")
    )
    private RenderType cyberware$ghostTrimRenderType(boolean decal) {
        if (AfterimageRenderer.ghostAlpha() > 0.0F) {
            // armorTranslucent 同样吃图集纹理，等于「半透明版的 trim 类型」
            return RenderTypes.armorTranslucent(Sheets.ARMOR_TRIMS_SHEET);
        }
        return Sheets.armorTrimsSheet(decal);
    }

    /**
     * 纹饰的顶点色原本是<b>硬编码的 -1</b>（纯不透明白）—— 光换渲染类型没用，
     * 顶点 alpha 还是 1.0。这里换成带残影 alpha 的白。
     *
     * <p>该调用点在这个方法里是第 3 个（护甲主体 → 附魔光效 → 纹饰），所以用 ordinal = 2 锁定。
     * 就算序号算偏了也不至于出错：返回的「带 alpha 的白」跟护甲主体那手算出来的颜色本就一致。
     */
    @ModifyArg(
        method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
        require = 0,
        index = 6,
        at = @At(value = "INVOKE",
            ordinal = 2,
            target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V")
    )
    private int cyberware$ghostTrimColor(int color) {
        float alpha = AfterimageRenderer.ghostAlpha();
        if (alpha <= 0.0F) {
            return color;
        }
        if (TRIM_LOGGED.compareAndSet(false, true)) {
            System.out.println("[cyberware] 纹饰顶点色已替换: 原色=" + Integer.toHexString(color)
                    + " alpha=" + alpha);
        }
        return ARGB.color(Math.round(Math.min(1.0F, alpha) * 255.0F), 255, 255, 255);
    }
}
