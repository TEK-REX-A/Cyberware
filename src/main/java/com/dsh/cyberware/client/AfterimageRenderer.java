package com.dsh.cyberware.client;

import com.dsh.cyberware.mixin.client.LivingEntityRendererAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * <p><b>两个大坑，都是主人实测出来的：</b>
 *
 * <p>1. <b>不能只 submitModel。</b>原版 {@code LivingEntityRenderer.submit} 在提交模型前后
 *    还做三件事：{@code setupRotations}、{@code poseStack.scale(-1,-1,1)}（模型是上下反的，
 *    漏了会<b>倒立</b>）、遍历 {@code layers}（护甲与手持物，漏了会<b>裸体</b>）。
 *    全部照抄原版顺序。
 *
 * <p>2. <b>每个残影必须有自己的 state。</b>{@code submitModel} 是<b>延迟提交</b> ——
 *    它把「要画什么」塞进队列，渲染管线稍后才统一绘制。如果所有残影共用一份 state 引用，
 *    真正渲染时读到的全是最后那份数据，于是所有身影摆同一个姿势、跟着本体做动作。
 *    这里给每个残影复制一份独立的 state，姿态才真的被「定格」。
 *
 * <p>只对**本地玩家**生效 —— 第一人称看不到自己，这个效果在第三人称（F5）最明显。
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public final class AfterimageRenderer {

    /** 最近处的残影透明度 */
    private static final float MAX_ALPHA = 0.45F;
    /** 透明度随「新旧」衰减的强度：接近 1.0 = 最老的一批几乎全透明 */
    private static final float ALPHA_FALLOFF = 0.80F;
    /** 暖色渐变（照主人给的参考图）：近处黄 → 远处橙 → 尾端红 */
    private static final float HUE_NEAR = 0.14F;
    private static final float HUE_FAR = 0.0F;
    /** 最多画几个残影（每多一个就是一次模型提交 + 一层护甲，性能敏感） */
    private static final int MAX_GHOSTS = 12;
    /** 原版渲染器在模型前的下移量（照抄 LivingEntityRenderer.submit） */
    private static final float MODEL_Y_OFFSET = -1.501F;

    /** 每种 state 类型各自的字段表（含父类）—— 只建一次，别再重复查找。 */
    private static final Map<Class<?>, List<Field>> FIELDS_CACHE = new HashMap<>();
    /** 每种 state 类型的无参构造函数缓存。 */
    private static final Map<Class<?>, Constructor<?>> CTOR_CACHE = new HashMap<>();

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
        SubmitNodeCollector layerCollector = collector instanceof SubmitNodeCollector specific
                ? specific : null;

        Identifier texture = renderer.getTextureLocation(state);
        RenderType renderType = RenderTypes.entityTranslucentCullItemTarget(texture);
        EntityModel<LivingEntityRenderState> model = renderer.getModel();
        List<RenderLayer<LivingEntityRenderState, ?>> layers = accessor.cyberware$layers();

        // 用步长把整段历史铺满：决定拖影长度的是这里，不是快照总数
        int available = snapshots.size() - 2;
        int wanted = Math.min(MAX_GHOSTS, available);
        if (wanted <= 0) {
            return;
        }
        int step = Math.max(1, available / wanted);

        for (int g = 0; g < wanted; g++) {
            int index = 2 + g * step;
            if (index >= snapshots.size()) {
                break;
            }
            AfterimageTracker.Snapshot snapshot = snapshots.get(index);
            float t = wanted <= 1 ? 1.0F : (float) g / (wanted - 1);   // 0 = 最近，1 = 最老
            float alpha = MAX_ALPHA * (1.0F - ALPHA_FALLOFF * t) * intensity;
            if (alpha <= 0.02F) {
                continue;
            }
            int rgb = Mth.hsvToRgb(Mth.lerp(t, HUE_NEAR, HUE_FAR), 0.85F, 1.0F);
            int color = ARGB.color(Math.round(alpha * 255.0F),
                    (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);

            // 每个残影一份独立 state —— 见类注释第 2 点
            LivingEntityRenderState ghost = copyState(state);
            ghost.bodyRot = snapshot.bodyRot();
            ghost.yRot = snapshot.yRot();
            ghost.xRot = snapshot.xRot();
            ghost.walkAnimationPos = snapshot.walkPos();
            ghost.walkAnimationSpeed = snapshot.walkSpeed();
            ghost.ageInTicks = snapshot.age();

            poseStack.pushPose();
            poseStack.translate(snapshot.x() - state.x,
                    snapshot.y() - state.y,
                    snapshot.z() - state.z);

            // ---- 一比一复刻原版 LivingEntityRenderer.submit 的变换 ----
            float scale = ghost.scale;
            poseStack.scale(scale, scale, scale);
            accessor.cyberware$setupRotations(ghost, poseStack, ghost.bodyRot, scale);
            poseStack.scale(-1.0F, -1.0F, 1.0F);   // ← 漏了这行就会「倒立」
            accessor.cyberware$scale(ghost, poseStack);
            poseStack.translate(0.0F, MODEL_Y_OFFSET, 0.0F);

            // 主体（半透明 + 染色）
            collector.submitModel(model, ghost, poseStack, renderType,
                    ghost.lightCoords, OverlayTexture.NO_OVERLAY, color, null, 0, null);

            // 全部渲染层：护甲、手持物、披风……（A.txt 第 2 条要「保留全部渲染层」）
            if (layerCollector != null) {
                model.setupAnim(ghost);
                for (RenderLayer<LivingEntityRenderState, ?> layer : layers) {
                    ((RenderLayer<LivingEntityRenderState, EntityModel<LivingEntityRenderState>>) layer)
                            .submit(poseStack, layerCollector, ghost.lightCoords, ghost, ghost.yRot, ghost.xRot);
                }
            }

            poseStack.popPose();
        }
    }

    /** 收集某个类型（含全部父类）里所有可写的实例字段。 */
    private static List<Field> fieldsOf(Class<?> type) {
        return FIELDS_CACHE.computeIfAbsent(type, key -> {
            List<Field> fields = new ArrayList<>();
            for (Class<?> cursor = key; cursor != null && cursor != Object.class; cursor = cursor.getSuperclass()) {
                for (Field field : cursor.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }
                    try {
                        field.setAccessible(true);
                        fields.add(field);
                    } catch (Throwable ignored) {
                        // 拿不到就算，缺一个字段不影响整体观感
                    }
                }
            }
            return fields;
        });
    }

    /**
     * 复制一份渲染状态 —— 让每个残影拥有「那一刻」的完整数据。
     *
     * <p><b>必须按原对象的真实类型来造。</b>玩家的 state 是 {@code AvatarRenderState}
     * （{@code LivingEntityRenderState} 的子类），护甲层、披风层内部会把它强转回去 ——
     * 早先这里写死 {@code new LivingEntityRenderState()}，一开斯安威斯坦就是
     * {@code ClassCastException}，直接把渲染线程打崩。
     */
    private static LivingEntityRenderState copyState(LivingEntityRenderState src) {
        Class<?> type = src.getClass();
        LivingEntityRenderState dst;
        try {
            Constructor<?> ctor = CTOR_CACHE.computeIfAbsent(type, key -> {
                try {
                    Constructor<?> found = key.getDeclaredConstructor();
                    found.setAccessible(true);
                    return found;
                } catch (Throwable t) {
                    return null;
                }
            });
            dst = ctor == null
                    ? new LivingEntityRenderState()
                    : (LivingEntityRenderState) ctor.newInstance();
        } catch (Throwable t) {
            dst = new LivingEntityRenderState();
        }
        for (Field field : fieldsOf(type)) {
            try {
                field.set(dst, field.get(src));
            } catch (Throwable ignored) {
                // final 字段可能拒绝写入：那份数据保持默认值，不影响姿态定格
            }
        }
        return dst;
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
