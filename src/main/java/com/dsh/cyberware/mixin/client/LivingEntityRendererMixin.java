package com.dsh.cyberware.mixin.client;

import com.dsh.cyberware.Cyberware;
import com.dsh.cyberware.client.AfterimageRenderer;
import com.dsh.cyberware.client.ClientTimeDilation;
import com.dsh.cyberware.client.OverclockWireframe;
import com.dsh.cyberware.client.RamClientState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric 移植：RenderLivingEvent 的替代品（骨架，尚未接线）。
 *
 * 为什么要这个 mixin：
 * NeoForge 用 RenderLivingEvent.Pre/Post 把「实体渲染前/后」暴露给 mod；Fabric 的
 * fabric-rendering-v1 没有等价事件（本机实测：其 33 个 API 类里只有
 * LivingEntityFeatureRenderEvents（唯一事件 ALLOW_CAPE_RENDER）与
 * LivingEntityRenderLayerRegistrationCallback，无实体渲染前后事件）。所以只能注入原版方法。
 *
 * 注入点（描述符逐字符来自 javap -s）：
 *   net.minecraft.client.renderer.entity.LivingEntityRenderer
 *     public void submit(S, PoseStack, SubmitNodeCollector, CameraRenderState)
 *     descriptor: (Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V
 *
 * 选它的理由（本机实测）：
 *   - submit 是 public、非 final —— 可注入；
 *   - AvatarRenderer 没有覆写 submit（只覆写 submitNameDisplay），所以玩家
 *     （AvatarRenderState）也走这里 —— 残影只对本地玩家生效，这点是硬需求；
 *   - 参数正好覆盖 RenderLivingEvent.Pre 曾提供的四样东西：
 *     getRenderState()->state、getPoseStack()->poseStack、
 *     getSubmitNodeCollector()->collector、getRenderer()->(LivingEntityRenderer)(Object)this。
 *
 * getPartialTick() 的替代：
 * 新管线把 partialTick 烤进了 render state，submit 没有这个参数；这里用
 * Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false)
 * 取同一帧的值（这两个方法本机实测存在）。语义等价性未验证 —— 见交接说明。
 *
 * 两条保命措施（照抄本项目既有铁律）：
 *   - require = 0：匹配不到就安静跳过 —— 最坏是「残影/轮廓不出现」，绝不会因为注不进去
 *     把整个 mixin 应用拖失败（injectors.defaultRequire = 1 的教训）；
 *   - 两个注入整体 try/catch(Throwable)：渲染线程抛异常 = 当场崩客户端，装饰性功能
 *     不该让玩家买单；异常只记一次日志，不刷屏。
 *
 * 注意：当前是骨架，不能编译 —— 下面调用的三个入口
 * （AfterimageRenderer.onSubmit / ClientTimeDilation.onSubmit / OverclockWireframe.onSubmitPost）
 * 尚未添加。要改哪几行已列在交接清单里。
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    /** 渲染线程异常只报一次，避免每帧刷屏把日志冲爆 */
    private static boolean cyberware$failLogged;

    /**
     * 渲染前（对应 RenderLivingEvent.Pre）。
     * 服务对象：残影（AfterimageRenderer）与时间减缓的动画缩放（ClientTimeDilation）。
     */
    @Inject(
        // 方法名与左括号之间绝对不能有冒号（javap 显示用的冒号不是描述符的一部分）
        method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
        at = @At("HEAD"),
        require = 0
    )
    private void cyberware$beforeSubmit(
            LivingEntityRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState cameraState,
            CallbackInfo ci) {
        try {
            if (state == null || poseStack == null || collector == null) {
                return;
            }
            // 性能自保：没有任何时缓/超频在跑时直接返回，省掉每帧每实体的两次静态调用
            if (ClientTimeDilation.ratioNow() <= 0.01F && !RamClientState.overclockActive()) {
                return;
            }
            @SuppressWarnings("rawtypes")
            LivingEntityRenderer renderer = (LivingEntityRenderer) (Object) this;
            float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);

            AfterimageRenderer.onSubmit(state, poseStack, collector, renderer, partialTick);
            // TODO(接线时二选一)：ClientTimeDilation.onRenderLiving 目前在 CyberwareClient 里
            //   **从未注册**（全仓只有定义 ClientTimeDilation.java:248，没有 addListener），
            //   即那段「时缓期缩放走路/挥砍动画」是死代码。默认保持死代码，不悄悄复活它：
            // ClientTimeDilation.onSubmit(state, partialTick);
        } catch (Throwable t) {
            cyberware$logOnce(t);
        }
    }

    /**
     * 渲染后（对应 RenderLivingEvent.Post）。
     * 服务对象：脑机超频的生物荧光轮廓（OverclockWireframe）—— 它要在本体提交之后
     * 再用半透明渲染类型重提一次主模型。
     */
    @Inject(
        method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
        at = @At("RETURN"),
        require = 0
    )
    private void cyberware$afterSubmit(
            LivingEntityRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState cameraState,
            CallbackInfo ci) {
        try {
            if (state == null || poseStack == null || collector == null) {
                return;
            }
            if (!RamClientState.overclockActive()) {
                return;
            }
            @SuppressWarnings("rawtypes")
            LivingEntityRenderer renderer = (LivingEntityRenderer) (Object) this;
            OverclockWireframe.onSubmitPost(state, poseStack, collector, renderer);
        } catch (Throwable t) {
            cyberware$logOnce(t);
        }
    }

    /** 渲染线程异常只报一次：装饰性功能失败不该让玩家买单，也不该刷爆日志 */
    private static void cyberware$logOnce(Throwable t) {
        if (!cyberware$failLogged) {
            cyberware$failLogged = true;
            Cyberware.LOGGER.warn("[cyberware] 实体渲染注入失败，已跳过（不影响游戏本体渲染）", t);
        }
    }
}

