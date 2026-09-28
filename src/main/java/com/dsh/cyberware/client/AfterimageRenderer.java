package com.dsh.cyberware.client;

import com.dsh.cyberware.mixin.client.LivingEntityRendererAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

/**
 * 斯安威斯坦 · 洋葱皮残影（A.txt 第 7 步）。
 *
 * <p><b>三个坑，都是主人实测出来的：</b>
 *
 * <p>1. 不能只 {@code submitModel} —— 原版还要 {@code setupRotations}、{@code scale(-1,-1,1)}
 *    （漏了会倒立）、遍历 {@code layers}（漏了会裸体）。全部照抄原版顺序。
 *
 * <p>2. 每个残影必须有**自己**的 state —— {@code submitModel} 是延迟提交的，共用一份引用
 *    会让所有残影在真正渲染时读到同一份数据。
 *
 * <p>3. 复制 state 要**快** —— 早先的反射实现让主人帧率掉了一半，现在走 {@link AfterimageHistory}，
 *    用 VarHandle + 环形缓冲复用对象，零分配。
 *
 * <p><b>动作定格：</b>历史里存的是**整份** render state，所以残影的姿势、四肢、披风、
 * 游泳/蹲下/挥手/鞘翅等一切动画都停在那一刻，不会再跟着本体动。
 *
 * <p>只对**本地玩家**生效 —— 第一人称看不到自己，第三人称（F5）最明显。
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public final class AfterimageRenderer {

    /** 最近处的残影透明度 */
    private static final float MAX_ALPHA = 0.45F;
    /** 透明度随「新旧」衰减的强度 */
    private static final float ALPHA_FALLOFF = 0.80F;
    /**
     * 冷色渐变（A.txt 第 3 条：青绿色 / 青蓝色滤镜）。
     * HSV 里 0.5 上下就是青，往 0.38 偏是青绿、往 0.55 偏是青蓝。
     */
    private static final float HUE_NEAR = 0.55F;
    private static final float HUE_FAR = 0.38F;
    /** 最多画几个残影 */
    private static final int MAX_GHOSTS = 8;
    /** 原版渲染器在模型前的下移量（照抄 LivingEntityRenderer.submit） */
    private static final float MODEL_Y_OFFSET = -1.501F;

    /** 诊断用：只打一次 */
    private static final java.util.concurrent.atomic.AtomicBoolean LOGGED =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    /** 渲染残影期间的透明度（0 = 不在渲染残影），护甲 mixin 靠它判断 */
    private static float ghostAlpha;

    private AfterimageRenderer() {
    }

    public static float ghostAlpha() {
        return ghostAlpha;
    }

    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?, ?> event) {
        // 只跟斯安威斯坦 —— 狂暴是力量型，不拖影
        float intensity = ClientTimeDilation.ratioNow();
        if (intensity <= 0.05F) {
            return;
        }

        LivingEntityRenderState state = event.getRenderState();
        if (!isLocalPlayer(state, event.getPartialTick())) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        int gameTime = minecraft.level == null ? 0 : (int) minecraft.level.getGameTime();
        // 每 tick 存一份完整状态（同一 tick 内只存一次）
        AfterimageHistory.sample(state, gameTime);

        int available = AfterimageHistory.size() - 2;
        int wanted = Math.min(MAX_GHOSTS, available);
        if (wanted <= 0) {
            return;
        }
        int step = Math.max(1, available / wanted);

        LivingEntityRenderer renderer = (LivingEntityRenderer) event.getRenderer();
        LivingEntityRendererAccessor accessor = (LivingEntityRendererAccessor) renderer;
        PoseStack poseStack = event.getPoseStack();
        OrderedSubmitNodeCollector collector = event.getSubmitNodeCollector();
        SubmitNodeCollector layerCollector = collector instanceof SubmitNodeCollector specific
                ? specific : null;

        Identifier texture = renderer.getTextureLocation(state);
        RenderType renderType = RenderTypes.entityTranslucentCullItemTarget(texture);
        EntityModel<LivingEntityRenderState> model = renderer.getModel();
        List<RenderLayer<LivingEntityRenderState, ?>> layers = accessor.cyberware$layers();

        try {
            for (int g = 0; g < wanted; g++) {
                LivingEntityRenderState ghost = AfterimageHistory.get(2 + g * step);
                if (ghost == null) {
                    break;
                }
                if (g == 0 && LOGGED.compareAndSet(false, true)) {
                    System.out.println("[cyberware] 残影渲染: index=" + (2 + g * step)
                            + " | ghost walk=" + ghost.walkAnimationPos + " x=" + ghost.x
                            + " | current walk=" + state.walkAnimationPos + " x=" + state.x);
                }
                float t = wanted <= 1 ? 1.0F : (float) g / (wanted - 1);
                float alpha = MAX_ALPHA * (1.0F - ALPHA_FALLOFF * t) * intensity;
                if (alpha <= 0.02F) {
                    continue;
                }
                int rgb = Mth.hsvToRgb(Mth.lerp(t, HUE_NEAR, HUE_FAR), 0.85F, 1.0F);
                int color = ARGB.color(Math.round(alpha * 255.0F),
                        (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);

                ghostAlpha = alpha;

                poseStack.pushPose();
                poseStack.translate(ghost.x - state.x, ghost.y - state.y, ghost.z - state.z);

                // ---- 一比一复刻原版 LivingEntityRenderer.submit 的变换 ----
                float scale = ghost.scale;
                poseStack.scale(scale, scale, scale);
                accessor.cyberware$setupRotations(ghost, poseStack, ghost.bodyRot, scale);
                poseStack.scale(-1.0F, -1.0F, 1.0F);
                accessor.cyberware$scale(ghost, poseStack);
                poseStack.translate(0.0F, MODEL_Y_OFFSET, 0.0F);

                // ghost 是那一刻的**完整**状态：姿势、四肢、披风、游泳/蹲下全都定格
                collector.submitModel(model, ghost, poseStack, renderType,
                        ghost.lightCoords, OverlayTexture.NO_OVERLAY, color, null, 0, null);

                if (layerCollector != null) {
                    model.setupAnim(ghost);
                    for (RenderLayer<LivingEntityRenderState, ?> layer : layers) {
                        ((RenderLayer<LivingEntityRenderState, EntityModel<LivingEntityRenderState>>) layer)
                                .submit(poseStack, layerCollector, ghost.lightCoords, ghost, ghost.yRot, ghost.xRot);
                    }
                }

                poseStack.popPose();
            }
        } finally {
            ghostAlpha = 0.0F;
        }
    }

    /** 用渲染坐标判断这是不是本地玩家（RenderLivingEvent 拿不到实体）。 */
    private static boolean isLocalPlayer(LivingEntityRenderState state, float partialTick) {
        LocalPlayer self = Minecraft.getInstance().player;
        if (self == null) {
            return false;
        }
        Vec3 pos = self.getPosition(partialTick);
        return Math.abs(state.x - pos.x) < 0.08D
                && Math.abs(state.y - pos.y) < 0.08D
                && Math.abs(state.z - pos.z) < 0.08D;
    }
}
