package com.dsh.cyberware.mixin.client;

import com.dsh.cyberware.client.AfterimageRenderer;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让**护甲**跟着残影一起半透明。
 *
 * <p>问题出在渲染类型：主模型走我们指定的半透明类型（吃 alpha），
 * 护甲走的却是 {@code armorCutoutNoCull} —— <b>镂空</b>类型，alpha 只有 0/1 两档，
 * 给多少透明度都当不透明处理。所以「身体透、盔甲实」，整体看着像没生效。
 *
 * <p>三手（都要）：
 * <ol>
 *   <li>把护甲的镂空类型换成游戏现成的 {@code armorTranslucent}（护甲专用：保留
 *       {@code VIEW_OFFSET_Z_LAYERING} 分层与 {@code PER_FACE_LIGHTING}，且有真混合）</li>
 *   <li>{@code getColorForLayer} 的返回值乘上残影 alpha（染色部位）</li>
 *   <li>盔甲纹饰（trim）的顶点色 —— 原版硬编码 {@code -1}，光换渲染类型没用，
 *       顶点 alpha 还是 1.0。改写第 3 个 {@code submitModel} 调用点的 tintedColor 参数</li>
 * </ol>
 *
 * <h2>★ 为什么整个文件里 @Redirect 一个都不留</h2>
 * 2026-10-22 黑屏事故的真正根因（task-1 定位，见 {@code /sdcard/DSH/MIXIN-CCE-ROOTCAUSE.md}）：
 * <pre>
 * ClassCastException: ArrayList cannot be cast to AnnotationNode
 *   at com.llamalad7.mixinextras.wrapper.factory.FactoryRedirectWrapperMixinTransformer.transform(:18)
 * </pre>
 * 运行时装的是 <b>MixinExtras 0.5.4</b>（Fabric API 用 JiJ 塞进来的），它那行代码是
 * {@code AnnotationNode at = Annotations.getValue(redirect, "at");}（单值强转），
 * 而 sponge-mixin 0.8.7 把 {@code @Redirect.at()} 从 {@code At} 改成了 {@code At[]} ——
 * javac 对数组型注解成员一律写成 {@code at=[@At(...)]}（只有一个元素也是 List）。
 * 于是<b>只要类里有任何一个 {@code @Redirect}，那个类 transform 时必炸</b>，
 * 跟写没写花括号、有几个 {@code @At} 无关。{@code @ModifyArg}、{@code @Inject} 都是无辜的
 * （那个 transformer 只扫 {@code @Redirect}）。0.5.5 已改成列表迭代修复。
 *
 * <p>所以这里把三处原来用 {@code @Redirect}/{@code @ModifyArg} 的注入<b>全换成
 * MixinExtras 的 {@code @WrapOperation}</b>（不走那个 transformer，0.5.4/0.5.5 都安全）。
 * 唯一的 {@code @Inject} 保留原样。
 *
 * <h2>★ 构建前提</h2>
 * {@code build.gradle} 目前<b>没有</b>声明 MixinExtras —— 必须补（编译期要用）：
 * <pre>{@code
 * modImplementation include("io.github.llamalad7:mixinextras-fabric:0.5.5")
 * }</pre>
 * 带上 {@code include}（jar-in-jar）不只是为了编译：运行时 Fabric API 自带的 0.5.4 会抢到
 * classpath，把我们这份 0.5.5 塞进 JiJ 才能靠版本仲裁压过它；保险起见再在
 * {@code fabric.mod.json} 的 depends 里写 {@code "mixinextras": ">=0.5.5"}，
 * 仲裁失败时直接报错，而不是又黑一次屏。该版本 artifact 已在本机 Gradle 缓存（jar+pom），离线可解析。
 *
 * <p>只在残影渲染上下文生效（{@code ghostAlpha > 0}），本体一切照旧。
 */
@Mixin(EquipmentLayerRenderer.class)
public class EquipmentLayerRendererMixin {

    /** 诊断日志去重：每种护甲层只打印一次，免得每帧 4 条刷爆日志 */
    private static final java.util.Set<String> LOGGED = new java.util.HashSet<>();
    /** 诊断日志去重：渲染类型替换只打印一次 */
    private static final java.util.Set<String> RT_LOGGED = new java.util.HashSet<>();
    /**
     * 护甲单独再压一档透明度。
     *
     * <p>主模型（{@code AfterimageRenderer} 的全局 alpha）要保持「人物看得见」，
     * 但护甲是 cutout 材质、alpha test 不混合，同样 alpha 下会显得比本体实 —— 主人反馈
     * 「盔甲不怎么透明，把人物盖住了一部分」。所以只在这里乘 0.5，不动全局 MAX_ALPHA。
     */
    private static final float ARMOR_ALPHA_SCALE = 0.8F;

    /** 诊断日志去重：纹饰顶点色只打印一次 */
    private static final java.util.concurrent.atomic.AtomicBoolean TRIM_LOGGED =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    /**
     * 护甲的镂空渲染类型 → 残影上下文换成半透明版本。
     *
     * <p>原来用 {@code @Redirect}（黑屏根因），改 {@code @WrapOperation}：
     * {@code RenderTypes.armorCutoutNoCull} 是 <b>INVOKESTATIC</b>，所以处理函数没有接收者参数，
     * 只有「实参 + Operation」。
     */
    @WrapOperation(
        method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
        require = 0,
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorCutoutNoCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;")
    )
    private RenderType cyberware$ghostArmorRenderType(Identifier texture, Operation<RenderType> original) {
        RenderType vanilla = original.call(texture);
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
                        + " | 原类型blending=" + vanilla.hasBlending());
            }
            return replaced;
        }
        return vanilla;
    }

    /**
     * 染色值乘上残影 alpha。
     *
     * <p><b>Fabric 移植改造点（原注入目标根本不存在）</b>：原来拦的是 NeoForge 专有扩展点
     * {@code IClientItemExtensions.getArmorLayerTintColor(ItemStack, Layer, int, int)} ——
     * 那不是原版方法，而是 NeoForge 打在 {@code EquipmentLayerRenderer} 里的补丁调用
     * （本机实测：NeoForge 补丁版把 {@code getColorForLayer} 从 {@code private static} 放宽成
     * {@code public static}，并在其中调用 {@code IClientItemExtensions.of(...)} 与该方法；
     * Fabric 的纯原版里这两个符号都不存在）。所以那处注入在 Fabric 上既编译不过、也无目标可拦。
     *
     * <p>等价替代：直接拦原版 {@code private static int getColorForLayer(EquipmentClientInfo$Layer,int)}
     * 的返回值（javap 实测：Fabric 版该类唯一产生护甲层颜色的地方，{@code NO_LAYER_COLOR} 之外
     * 全方法只有这一处计算）。语义比原来更直接：改动的是"最终着色值"，而不是某个扩展点的返回值。
     *
     * <p>{@code @Inject} 不受 MixinExtras 0.5.4 那个 CCE 影响（transformer 只扫 {@code @Redirect}），
     * 所以保持原样。
     */
    @Inject(
        method = "getColorForLayer(Lnet/minecraft/client/resources/model/EquipmentClientInfo$Layer;I)I",
        at = @At("RETURN"),
        cancellable = true,
        require = 0
    )
    private static void cyberware$ghostArmorColor(EquipmentClientInfo.Layer layer, int dyeColor,
                                                  CallbackInfoReturnable<Integer> cir) {
        float alpha = AfterimageRenderer.ghostAlpha();
        if (alpha <= 0.0F) {
            return;
        }
        int color = cir.getReturnValue();
        if (LOGGED.add(String.valueOf(layer))) {
            // 只打印一次：每帧 4 个部位都打会把日志刷爆
            System.out.println("[cyberware] 护甲残影染色生效: alpha=" + alpha
                    + " color=" + Integer.toHexString(color));
        }
        cir.setReturnValue(ARGB.multiplyAlpha(color, alpha * ARMOR_ALPHA_SCALE));
    }

    /**
     * 盔甲纹饰（armor trim）的图集类型 → 残影上下文换成半透明的那个。
     *
     * <p>纹饰走的是 {@code Sheets.armorTrimsSheet(...)}，它返回的是两个<b>静态常量</b>
     * （{@code ARMOR_TRIMS_SHEET_TYPE = armorCutoutNoCull(ARMOR_TRIMS_SHEET)}）——
     * 也就是说它虽然也叫 armorCutoutNoCull，却是在 {@code Sheets} 的静态初始化里造好的，
     * 跟 {@code renderLayers} 里那个调用点不是同一处，所以上面那手够不着它。
     * 这里直接换 {@code armorTrimsSheet} 的返回值。
     *
     * <p>{@code Sheets.armorTrimsSheet(boolean)} 也是 <b>INVOKESTATIC</b> → 处理函数无接收者。
     */
    @WrapOperation(
        method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
        require = 0,
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/Sheets;armorTrimsSheet(Z)Lnet/minecraft/client/renderer/rendertype/RenderType;")
    )
    private RenderType cyberware$ghostTrimRenderType(boolean decal, Operation<RenderType> original) {
        if (AfterimageRenderer.ghostAlpha() > 0.0F) {
            // armorTranslucent 同样吃图集纹理，等于「半透明版的 trim 类型」
            return RenderTypes.armorTranslucent(Sheets.ARMOR_TRIMS_SHEET);
        }
        return original.call(decal);
    }

    /**
     * 纹饰的顶点色原本是<b>硬编码的 -1</b>（纯不透明白）—— 光换渲染类型没用，
     * 顶点 alpha 还是 1.0。这里换成带残影 alpha 的白。
     *
     * <p>历史：这里原来是 {@code @ModifyArg(require = 0, index = 6, at = @At(value="INVOKE", ordinal=2, ...))}，
     * 被误判成黑屏元凶而注释掉（真凶是同类的 {@code @Redirect}，见类注释）。顺带一提它那个
     * {@code index = 6} 本身也可疑：按「接收者算 0 号参数」的约定，{@code submitModel} 的
     * tintedColor 是 index <b>7</b>，6 落在 overlay 那个 int 上。
     *
     * <p>现在用 {@code @WrapOperation} 把<b>整次调用</b>包起来：按形参顺序拿到全部实参，
     * 只把 {@code tintedColor} 换掉再 {@code original.call(...)} 转出去 —— 位置一目了然，
     * 也不依赖 Mixin 的 index 语义。
     *
     * <p>调用点定位：{@code renderLayers} 里 {@code submitModel} 依次是
     * 护甲主体(ordinal 0) → 附魔光效(ordinal 1) → 纹饰(ordinal 2)，故 {@code ordinal = 2}。
     * 三次调用都是 INVOKEINTERFACE，所以处理函数第一个参数是<b>接收者</b>
     * （{@code OrderedSubmitNodeCollector}）—— 这点由 MixinExtras 的
     * {@code getEffectiveArgTypes} 决定：INVOKEINTERFACE/VIRTUAL 会把 owner 类型插到 args[0]。
     *
     * <p>{@code alpha <= 0} 时原样透传（连参数都不动），保证本体渲染零影响。
     */
    @WrapOperation(
        method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
        require = 0,
        at = @At(value = "INVOKE",
            ordinal = 2,
            target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V")
    )
    private void cyberware$ghostTrimSubmitModel(OrderedSubmitNodeCollector collector,
                                                Model<?> model,
                                                Object state,
                                                PoseStack poseStack,
                                                RenderType renderType,
                                                int light,
                                                int overlayCoord,
                                                int tintedColor,
                                                TextureAtlasSprite sprite,
                                                int outlineColor,
                                                ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
                                                Operation<Void> original) {
        float alpha = AfterimageRenderer.ghostAlpha();
        int color = tintedColor;
        if (alpha > 0.0F) {
            if (TRIM_LOGGED.compareAndSet(false, true)) {
                System.out.println("[cyberware] 纹饰顶点色已替换: 原色=" + Integer.toHexString(tintedColor)
                        + " alpha=" + alpha);
            }
            color = ARGB.color(Math.round(Math.min(1.0F, alpha * ARMOR_ALPHA_SCALE) * 255.0F), 255, 255, 255);
        }
        // 其余实参一律原样传回，等于「只改了 tintedColor 这一个」
        original.call(collector, model, state, poseStack, renderType, light, overlayCoord,
                color, sprite, outlineColor, crumblingOverlay);
    }
}

