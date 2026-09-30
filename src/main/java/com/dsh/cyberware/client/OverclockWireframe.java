package com.dsh.cyberware.client;

import com.dsh.cyberware.Cyberware;
import com.dsh.cyberware.mixin.client.LivingEntityRendererAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

/**
 * 脑机超频持续期：<b>生物荧光轮廓化</b>（t25，P0）。
 *
 * <p><b>实现方式（诚实说明）</b>：把生物自己的模型用<b>纯色半透明渲染类型</b>在
 * {@code RenderLivingEvent.Post} 再提交一次 —— 结果是「荧光绿/洋红的发光轮廓覆盖在生物身上」。
 * <b>不是</b>逐边线的真线框：原版 {@code RenderTypes.LINES} 是 LINE 图元，而实体模型是 QUADS，
 * 两者顶点格式不兼容（拿模型去走 lines 管线画不出东西）。
 * 视觉目标是「怪物在超频里变成荧光色的轮廓」，本实现达到该效果。
 *
 * <p><b>帧率自保</b>（写进交付文档供主人真机判断）：
 * <ul>
 *   <li>距离：只处理相机 24 格内的生物（{@link #RANGE}）；</li>
 *   <li>数量：单帧最多重绘 {@link #MAX_ENTITIES} 只，超了直接不画；</li>
 *   <li>只提交<b>主模型</b>，不遍历图层（护甲/披风等一律不参与，省下最大的一块）；</li>
 *   <li>本地玩家不画（用 {@link AvatarRenderState} + 实体 id 判定，不用坐标比身份）。</li>
 * </ul>
 *
 * <p>渲染回调整体 try/catch —— 这里抛异常会当场崩客户端。
 */
public final class OverclockWireframe {

    /** 只看这么远的生物（帧率自保） */
    private static final double RANGE = 24.0D;
    /** 单帧最多重绘几只（帧率自保） */
    private static final int MAX_ENTITIES = 16;
    /** 轮廓透明度 */
    private static final int ALPHA = 0x7A;
    /** 荧光绿（普通生物） */
    private static final int GREEN = 0x39FF6A;
    /** 洋红（玩家） */
    private static final int MAGENTA = 0xFF37C8;
    /** 原版渲染器在模型前的下移量（照抄 LivingEntityRenderer.submit） */
    private static final float MODEL_Y_OFFSET = -1.501F;

    private static final AtomicBoolean FAIL_LOGGED = new AtomicBoolean(false);
    /** 单帧计数：用毫秒时间戳分辨帧（渲染事件里没有帧号） */
    private static long frameStamp;
    private static int drawnThisFrame;

    private OverclockWireframe() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void onRenderLiving(RenderLivingEvent.Post<?, ?, ?> event) {
        try {
            render(event);
        } catch (Throwable t) {
            if (FAIL_LOGGED.compareAndSet(false, true)) {
                Cyberware.LOGGER.warn("[cyberware] 超频轮廓渲染异常（已忽略，不影响游戏）", t);
            }
        }
    }

    private static void render(RenderLivingEvent.Post<?, ?, ?> event) {
        if (!RamClientState.overclockActive()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer self = minecraft.player;
        if (self == null) {
            return;
        }
        LivingEntityRenderState state = event.getRenderState();
        if (state == null || state.isInvisible) {
            return;
        }
        // 身份判定走渲染状态类型 + 实体 id（铁律：不用坐标比身份）
        if (state instanceof AvatarRenderState avatar && avatar.id == self.getId()) {
            return;
        }
        // 距离剔除（帧率自保）：这是「远近」不是「身份」，用坐标没问题
        double dx = state.x - self.getX();
        double dy = state.y - self.getY();
        double dz = state.z - self.getZ();
        if (dx * dx + dy * dy + dz * dz > RANGE * RANGE) {
            return;
        }
        // 单帧上限（帧率自保）
        long now = System.currentTimeMillis();
        if (now != frameStamp) {
            frameStamp = now;
            drawnThisFrame = 0;
        }
        if (drawnThisFrame >= MAX_ENTITIES) {
            return;
        }

        boolean isPlayer = state instanceof AvatarRenderState;
        int rgb = isPlayer ? MAGENTA : GREEN;
        int color = ARGB.color(ALPHA, (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);

        LivingEntityRenderer renderer = (LivingEntityRenderer) event.getRenderer();
        LivingEntityRendererAccessor accessor = (LivingEntityRendererAccessor) renderer;
        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        if (poseStack == null || collector == null) {
            return;
        }
        Identifier texture = renderer.getTextureLocation(state);
        RenderType renderType = RenderTypes.entityTranslucentCullItemTarget(texture);
        EntityModel<LivingEntityRenderState> model = renderer.getModel();

        poseStack.pushPose();
        try {
            // —— 一比一复刻原版 LivingEntityRenderer.submit 的变换（同 AfterimageRenderer）——
            float scale = state.scale;
            poseStack.scale(scale, scale, scale);
            accessor.cyberware$setupRotations(state, poseStack, state.bodyRot, scale);
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            accessor.cyberware$scale(state, poseStack);
            poseStack.translate(0.0F, MODEL_Y_OFFSET, 0.0F);
            // 只提交主模型：不遍历图层（最大的一块开销省掉）
            collector.submitModel(model, state, poseStack, renderType,
                    state.lightCoords, OverlayTexture.NO_OVERLAY, color, null, 0, null);
            drawnThisFrame++;
        } finally {
            poseStack.popPose();
        }
    }
}
