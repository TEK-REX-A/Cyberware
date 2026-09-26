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
 * <p>拿 {@link AfterimageTracker} 里的历史快照，在当前实体的位置坐标系里再画几遍模型。
 *
 * <p><b>关键：不能只 submitModel。</b>原版 {@code LivingEntityRenderer.submit} 在提交模型前后
 * 还做了三件事，少一件就会出洋相：
 * <ol>
 *   <li>{@code setupRotations} —— 按身体朝向转，不做的话残影朝向乱</li>
 *   <li>{@code poseStack.scale(-1, -1, 1)} —— Minecraft 模型是上下反的；
 *       前一版漏了这步，主人看到的残影就是<b>倒立</b>的</li>
 *   <li>遍历 {@code layers} —— 护甲、手持物都是独立的渲染层；
 *       前一版只提交了基础模型，所以主人<b>「变成裸屌」</b>了</li>
 * </ol>
 * 这一版一比一复刻原版顺序，护甲和手持物都会跟着影子一起拖。
 *
 * <p>只对**本地玩家**生效 —— 第一人称下看不到自己，这个效果在第三人称（F5）最明显。
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public final class AfterimageRenderer {

    /** 残影最高不透明度 */
    private static final float MAX_ALPHA = 0.34F;
    /** 暖色渐变（照主人给的参考图）：近处黄 → 远处橙 → 尾端红 */
    private static final float HUE_NEAR = 0.14F;
    private static final float HUE_FAR = 0.0F;
    /** 最多画几个残影（每多一个就是一次模型提交 + 一层护甲，性能敏感） */
    private static final int MAX_GHOSTS = 12;
    /** 原版渲染器在模型前的下移量（照抄 LivingEntityRenderer.submit） */
    private static final float MODEL_Y_OFFSET = -1.501F;

    private AfterimageRenderer() {
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

        List<AfterimageTracker.Snapshot> snapshots = AfterimageTracker.snapshots();
        if (snapshots.size() < 4) {
            return;
        }

        LivingEntityRenderer renderer = (LivingEntityRenderer) event.getRenderer();
        LivingEntityRendererAccessor accessor = (LivingEntityRendererAccessor) renderer;
        PoseStack poseStack = event.getPoseStack();
        OrderedSubmitNodeCollector collector = event.getSubmitNodeCollector();
        // 层（护甲/手持物）要的是更具体的 SubmitNodeCollector；事件暴露的是它的父接口。
        // 运行时实际对象一定是 SubmitNodeCollector 的实例，这里稳妥地探测一次，拿不到就只画主体。
        SubmitNodeCollector layerCollector = collector instanceof SubmitNodeCollector specific
                ? specific : null;

        Identifier texture = renderer.getTextureLocation(state);
        RenderType renderType = RenderTypes.entityTranslucentCullItemTarget(texture);
        EntityModel<LivingEntityRenderState> model = renderer.getModel();
        List<RenderLayer<LivingEntityRenderState, ?>> layers = accessor.cyberware$layers();

        // 用步长把整段历史铺满：决定拖影长度的是这里，不是快照总数
        int available = snapshots.size() - 2;          // 跳过最近两份：离本体太近，画了像重影
        int wanted = Math.min(MAX_GHOSTS, available);
        if (wanted <= 0) {
            return;
        }
        int step = Math.max(1, available / wanted);

        // 备份要改的字段 —— 事件返回后原版还要用同一份 state 画本体，必须原样还回去
        float oldBody = state.bodyRot;
        float oldY = state.yRot;
        float oldX = state.xRot;
        float oldWalkPos = state.walkAnimationPos;
        float oldWalkSpeed = state.walkAnimationSpeed;
        float oldAge = state.ageInTicks;

        for (int g = 0; g < wanted; g++) {
            int index = 2 + g * step;
            if (index >= snapshots.size()) {
                break;
            }
            AfterimageTracker.Snapshot snapshot = snapshots.get(index);
            float t = wanted <= 1 ? 1.0F : (float) g / (wanted - 1);   // 0 = 最近，1 = 最老
            float alpha = MAX_ALPHA * (1.0F - 0.55F * t) * intensity;
            if (alpha <= 0.02F) {
                continue;
            }
            int rgb = Mth.hsvToRgb(Mth.lerp(t, HUE_NEAR, HUE_FAR), 0.85F, 1.0F);
            int color = ARGB.color(Math.round(alpha * 255.0F),
                    (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);

            poseStack.pushPose();
            poseStack.translate(snapshot.x() - state.x,
                    snapshot.y() - state.y,
                    snapshot.z() - state.z);

            // 姿态回退到那一刻 —— 否则残影会用当前的四肢动作，看着像整条影子跟着一起动
            state.bodyRot = snapshot.bodyRot();
            state.yRot = snapshot.yRot();
            state.xRot = snapshot.xRot();
            state.walkAnimationPos = snapshot.walkPos();
            state.walkAnimationSpeed = snapshot.walkSpeed();
            state.ageInTicks = snapshot.age();

            // ---- 一比一复刻原版 LivingEntityRenderer.submit 的变换 ----
            float scale = state.scale;
            poseStack.scale(scale, scale, scale);
            accessor.cyberware$setupRotations(state, poseStack, state.bodyRot, scale);
            poseStack.scale(-1.0F, -1.0F, 1.0F);   // ← 漏了这行就会「倒立」
            accessor.cyberware$scale(state, poseStack);
            poseStack.translate(0.0F, MODEL_Y_OFFSET, 0.0F);

            // 主体（半透明 + 染色）
            collector.submitModel(model, state, poseStack, renderType,
                    state.lightCoords, OverlayTexture.NO_OVERLAY, color, null, 0, null);

            // 全部渲染层：护甲、手持物、披风……（A.txt 第 2 条点名要「保留全部渲染层」）
            if (layerCollector != null) {
                model.setupAnim(state);
                for (RenderLayer<LivingEntityRenderState, ?> layer : layers) {
                    ((RenderLayer<LivingEntityRenderState, EntityModel<LivingEntityRenderState>>) layer)
                            .submit(poseStack, layerCollector, state.lightCoords, state, state.yRot, state.xRot);
                }
            }

            poseStack.popPose();
        }

        // 原样还回去
        state.bodyRot = oldBody;
        state.yRot = oldY;
        state.xRot = oldX;
        state.walkAnimationPos = oldWalkPos;
        state.walkAnimationSpeed = oldWalkSpeed;
        state.ageInTicks = oldAge;
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
