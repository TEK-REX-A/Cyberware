package com.dsh.cyberware.client;

import com.dsh.cyberware.mixin.client.LivingEntityRendererAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
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
 *    （漏了会倒立）、以及遍历 {@code layers}（漏了会裸体）。全部照抄原版顺序。
 *
 * <p>2. 每个残影必须有**自己**的 state —— {@code submitModel} 是延迟提交的，共用一份引用
 *    会让所有残影在真正渲染时读到同一份数据（= 跟着本体做动作）。
 *
 * <p>3. 复制 state 要**快**。早先用反射逐字段 {@code Field.get/set}，每帧 12 个残影 × 几十个字段
 *    = 上百次反射调用，主人实测**帧率直接掉一半**。现在换成 {@link VarHandle}（JIT 能内联），
 *    再加一个**对象池**复用 state，不再每帧 new。
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
    /** 最多画几个残影 —— 从 12 降到 8：省三分之一开销，视觉上依旧够密 */
    private static final int MAX_GHOSTS = 8;
    /** 原版渲染器在模型前的下移量（照抄 LivingEntityRenderer.submit） */
    private static final float MODEL_Y_OFFSET = -1.501F;

    /** 每种 state 类型的 VarHandle 表（含父类）—— 比反射快得多 */
    private static final Map<Class<?>, List<VarHandle>> HANDLES_CACHE = new HashMap<>();
    /** 每种 state 类型的无参构造函数缓存 */
    private static final Map<Class<?>, Constructor<?>> CTOR_CACHE = new HashMap<>();
    /** 复用的 state 对象池 —— 每帧 new 十几个 state 是纯粹的浪费 */
    private static final List<LivingEntityRenderState> POOL = new ArrayList<>();

    /** 渲染残影期间的透明度（0 = 不在渲染残影），护甲 mixin 靠它判断 */
    private static float ghostAlpha;

    // 披风参数只在 render state 里，这里每帧缓存一次，供 AfterimageTracker 存进快照
    private static float capeFlap;
    private static float capeLean;
    private static float capeLean2;

    private AfterimageRenderer() {
    }

    public static float ghostAlpha() {
        return ghostAlpha;
    }

    public static float cachedCapeFlap() {
        return capeFlap;
    }

    public static float cachedCapeLean() {
        return capeLean;
    }

    public static float cachedCapeLean2() {
        return capeLean2;
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

        // 每帧把披风姿态缓存下来（它只存在于 render state 里）
        if (state instanceof AvatarRenderState avatar) {
            capeFlap = avatar.capeFlap;
            capeLean = avatar.capeLean;
            capeLean2 = avatar.capeLean2;
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

        int available = snapshots.size() - 2;
        int wanted = Math.min(MAX_GHOSTS, available);
        if (wanted <= 0) {
            return;
        }
        int step = Math.max(1, available / wanted);
        Class<?> stateType = state.getClass();
        List<VarHandle> handles = handlesOf(stateType);

        try {
            for (int g = 0; g < wanted; g++) {
                int index = 2 + g * step;
                if (index >= snapshots.size()) {
                    break;
                }
                AfterimageTracker.Snapshot snapshot = snapshots.get(index);
                float t = wanted <= 1 ? 1.0F : (float) g / (wanted - 1);
                float alpha = MAX_ALPHA * (1.0F - ALPHA_FALLOFF * t) * intensity;
                if (alpha <= 0.02F) {
                    continue;
                }
                int rgb = Mth.hsvToRgb(Mth.lerp(t, HUE_NEAR, HUE_FAR), 0.85F, 1.0F);
                int color = ARGB.color(Math.round(alpha * 255.0F),
                        (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);

                // 从池里借一个 state，把当前状态整体复制过来（VarHandle，很快）
                LivingEntityRenderState ghost = borrow(stateType, g);
                copyInto(state, ghost, handles);

                // 姿态/位置回退到快照那一刻 —— 这就是「定格」
                ghost.bodyRot = snapshot.bodyRot();
                ghost.yRot = snapshot.yRot();
                ghost.xRot = snapshot.xRot();
                ghost.walkAnimationPos = snapshot.walkPos();
                ghost.walkAnimationSpeed = snapshot.walkSpeed();
                ghost.ageInTicks = snapshot.age();
                if (ghost instanceof AvatarRenderState cape) {
                    cape.capeFlap = snapshot.capeFlap();
                    cape.capeLean = snapshot.capeLean();
                    cape.capeLean2 = snapshot.capeLean2();
                }

                ghostAlpha = alpha;

                poseStack.pushPose();
                poseStack.translate(snapshot.x() - state.x,
                        snapshot.y() - state.y,
                        snapshot.z() - state.z);

                // ---- 一比一复刻原版 LivingEntityRenderer.submit 的变换 ----
                float scale = ghost.scale;
                poseStack.scale(scale, scale, scale);
                accessor.cyberware$setupRotations(ghost, poseStack, ghost.bodyRot, scale);
                poseStack.scale(-1.0F, -1.0F, 1.0F);
                accessor.cyberware$scale(ghost, poseStack);
                poseStack.translate(0.0F, MODEL_Y_OFFSET, 0.0F);

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

    /** 收集某个类型（含父类）所有可写字段的 VarHandle。 */
    private static List<VarHandle> handlesOf(Class<?> type) {
        return HANDLES_CACHE.computeIfAbsent(type, key -> {
            List<VarHandle> handles = new ArrayList<>();
            MethodHandles.Lookup base = MethodHandles.lookup();
            for (Class<?> cursor = key; cursor != null && cursor != Object.class; cursor = cursor.getSuperclass()) {
                MethodHandles.Lookup lookup;
                try {
                    lookup = MethodHandles.privateLookupIn(cursor, base);
                } catch (Throwable t) {
                    continue;
                }
                for (Field field : cursor.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }
                    try {
                        handles.add(lookup.unreflectVarHandle(field));
                    } catch (Throwable ignored) {
                        // 个别字段拿不到就算，不影响整体观感
                    }
                }
            }
            return handles;
        });
    }

    /** 把 src 的全部字段复制进 dst（同一个对象反复复用，所以不需要重置）。 */
    private static void copyInto(LivingEntityRenderState src, LivingEntityRenderState dst,
                                 List<VarHandle> handles) {
        for (int i = 0; i < handles.size(); i++) {
            VarHandle handle = handles.get(i);
            try {
                handle.set(dst, handle.get(src));
            } catch (Throwable ignored) {
                // final 字段可能拒绝写入，跳过
            }
        }
    }

    /** 从对象池借一个 state（按 ghost 序号复用，避免每帧 new）。 */
    private static LivingEntityRenderState borrow(Class<?> type, int slot) {
        while (POOL.size() <= slot) {
            POOL.add(create(type));
        }
        LivingEntityRenderState state = POOL.get(slot);
        if (!type.isInstance(state)) {
            state = create(type);
            POOL.set(slot, state);
        }
        return state;
    }

    /** 按真实类型造一个 state（玩家的 state 是 AvatarRenderState，不能退化成基类）。 */
    private static LivingEntityRenderState create(Class<?> type) {
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
            return ctor == null ? new LivingEntityRenderState()
                    : (LivingEntityRenderState) ctor.newInstance();
        } catch (Throwable t) {
            return new LivingEntityRenderState();
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
