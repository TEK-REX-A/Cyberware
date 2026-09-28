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
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
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
 * <p><b>动作定格（2026-09-28 修）：</b>光存整份 state 还不够 —— 若每 tick 都往环里写一份，
 * 环的 head 就每 tick 前进一格，{@code get(index)} 每 tick 换一份快照，残影的姿势依旧每 tick 在变
 * （观感 =「残影在滞后几 tick 地播放本体的动画」，也就是主人说的「位置定格了、四肢还在甩」）。
 * 真正让它定住的是 {@link AfterimageHistory#INTERVAL}：隔 2 tick 才落一次盘，
 * 两次采样之间残影读的是同一份快照，姿势完全冻结，每 0.1 秒跳一帧。
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
    /**
     * 残影之间隔几份快照 —— 配合 {@link AfterimageHistory#INTERVAL} 决定时间跨度。
     * step=1、INTERVAL=2 → 8 个残影分别是 2/4/…/16 tick 前（0.1~0.8 秒），挨得比 step=2 密一倍。
     */
    private static final int GHOST_STEP = 1;
    /** 原版渲染器在模型前的下移量（照抄 LivingEntityRenderer.submit） */
    private static final float MODEL_Y_OFFSET = -1.501F;

    /** 诊断用：只打一次 */
    private static final java.util.concurrent.atomic.AtomicBoolean LOGGED =
            new java.util.concurrent.atomic.AtomicBoolean(false);
    /** 诊断用：图层清单只打一次 */
    private static final java.util.concurrent.atomic.AtomicBoolean LAYER_LOGGED =
            new java.util.concurrent.atomic.AtomicBoolean(false);
    /** 保命用：异常日志只打一次 */
    private static final java.util.concurrent.atomic.AtomicBoolean FAIL_LOGGED =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    /** 渲染残影期间的透明度（0 = 不在渲染残影），护甲 mixin 靠它判断 */
    private static float ghostAlpha;

    private AfterimageRenderer() {
    }

    public static float ghostAlpha() {
        return ghostAlpha;
    }

    /**
     * 渲染事件的入口。
     *
     * <p><b>外面这层 try/catch 是保命的。</b>渲染事件里抛出任何异常都会直接崩客户端 ——
     * 0.3.11-Hotfix-6 就因为一个 {@code ClassCastException} 把主人踢出过游戏。
     * 残影只是装饰品，出任何意外都只打一条日志，绝不中断游戏。
     */
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?, ?> event) {
        try {
            renderAfterimages(event);
        } catch (Throwable t) {
            if (FAIL_LOGGED.compareAndSet(false, true)) {
                System.out.println("[cyberware] 残影渲染异常（已忽略，不影响游戏）: " + t);
                t.printStackTrace();
            }
        } finally {
            ghostAlpha = 0.0F;
        }
    }

    private static void renderAfterimages(RenderLivingEvent.Pre<?, ?, ?> event) {
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
        if (available <= 0) {
            return;
        }
        // 固定步长。早先是把 8 个残影摊到整条环上（step = available / wanted），
        // 那会把最老的一份拉到 46*INTERVAL ≈ 3.7 秒前 —— 位置远得没有意义。
        // 改成定步长：残影数量由「攒了多少份历史」决定，不够就少画几个。
        int wanted = Math.min(MAX_GHOSTS, available / GHOST_STEP);
        if (wanted <= 0) {
            return;
        }
        int step = GHOST_STEP;

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
                    int ageTicks = (2 + g * step) * AfterimageHistory.INTERVAL;
                    System.out.println("[cyberware] 残影渲染: index=" + (2 + g * step)
                            + " ageTicks=" + ageTicks
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

                if (g == 0 && LAYER_LOGGED.compareAndSet(false, true)) {
                    System.out.println("[cyberware] 残影图层: collector=" + (layerCollector != null)
                            + " count=" + layers.size() + " -> " + layers.stream()
                                    .map(l -> l.getClass().getSimpleName()).toList());
                }
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

    /**
     * 判断这份渲染状态是不是本地玩家的。
     *
     * <p><b>必须先认类型、再比坐标 —— 顺序不能反（Hotfix-7 修崩溃）。</b>
     * 早先只比坐标（差 &lt; 0.08 格就算本地玩家），于是监守者站到主人身上时，
     * 监守者的 {@code WardenRenderState} 也满足「离玩家很近」，被当成玩家放进了后面的流程 ——
     * 结果是拿 {@code WardenModel} 去 {@code setupAnim} 玩家的状态，
     * 一个 {@code ClassCastException} 当场把游戏崩掉。
     *
     * <p>坐标只能说明「离得近」，说明不了「他是谁」。身份得靠类型认。
     */
    private static boolean isLocalPlayer(LivingEntityRenderState state, float partialTick) {
        // 1) 身份：只有玩家才有 AvatarRenderState —— 监守者、宠物、一切其它生物挡在这里
        if (!(state instanceof AvatarRenderState)) {
            return false;
        }
        // 2) 位置：再排除掉别的玩家
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
