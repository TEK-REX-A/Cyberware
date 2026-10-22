package com.dsh.cyberware.client.post;

import com.dsh.cyberware.Cyberware;
import com.dsh.cyberware.client.BerserkClientState;
import com.dsh.cyberware.client.ClientTimeDilation;
import com.dsh.cyberware.config.CyberwareConfig;
import com.dsh.cyberware.mixin.client.PostChainAccessor;
import com.dsh.cyberware.mixin.client.PostPassAccessor;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * 斯安威斯坦 · 屏幕后处理（边缘径向模糊 + 内侧压缩 + 色散）。
 *
 * <p>效果本体是 {@code assets/cyberware/shaders/post/sandevistan_edge.fsh}，
 * 通过 {@code post_effect/sandevistan.json} 组成一条 PostChain：
 * 读 {@code minecraft:main} → 写 {@code swap} → 再原样拷回 {@code minecraft:main}。
 *
 * <p><b>为什么要 Mixin</b>：26.x 的 {@code PostPass} 在构造时就把 uniform 烤进了
 * {@code GpuBuffer}，JSON 里的值是死值、没有 setter。这里用 accessor 拿到那张缓冲表，
 * 在需要改变强度时换一块新 buffer 顶上去，于是强度可以随激活进度平滑变化。
 *
 * <p>旧 buffer 不立刻释放，而是排队等几帧 —— 上一帧的帧图可能还在引用它。
 */
public final class SandevistanPostProcessor {

    /** post_effect/sandevistan.json（id 不带目录前缀）。 */
    private static final Identifier CHAIN_ID = Identifier.fromNamespaceAndPath("cyberware", "sandevistan");
    /** 与 fsh 里的 UBO 块名一致。 */
    private static final String UNIFORM_BLOCK = "CyberConfig";

    // ---- 效果参数。TODO(主人填写): 这些是手感初值，按观感调 ----
    private static final float STRENGTH = 0.085F;
    private static final float BLUR_START = 0.06F;
    private static final float BLUR_FULL = 1.0F;
    private static final float WARP_AMOUNT = 0.28F;
    private static final float CHROMATIC = 0.0100F;
    /** 采样数别调太高 —— 全屏模糊在手机上很吃 GPU，10 次已经是性价比拐点 */
    private static final float SAMPLES = 10.0F;

    // ---- 淡入淡出（tick） ----
    private static final float FADE_IN_TICKS = 3.5F;
    private static final float FADE_OUT_TICKS = 12.0F;
    /** 强度变化小于这个值就不重传 uniform，省掉每帧的缓冲分配。 */
    private static final float UPLOAD_EPSILON = 0.02F;

    /** 激活时视野拉伸比例（0.28 = 视野 +28%）。速度感的主要来源。 */
    private static final float FOV_BOOST = 0.45F;
    /** FOV 收缩到位的时长（tick）。2.5 → 1.0：主人要「FOV 缩放加速」 */
    private static final float FOV_FADE_TICKS = 1.0F;

    /** 斯安威斯坦：冷青蓝 —— 压红、抬蓝，做出「高度冷静」的观感 */
    private static final float[] TINT_DILATION = {0.62F, 1.02F, 1.32F};
    /** 狂暴：红 */
    private static final float[] TINT_BERSERK = {1.38F, 0.52F, 0.50F};

    private static float intensity;
    private static float previousIntensity;
    private static float lastUploaded = -1.0F;
    private static long pulseStartTick;
    private static boolean wasActive;
    /** 一旦出错就永久停用，避免每帧刷屏。 */
    private static boolean disabled;

    private static float tintR = 0.62F;
    private static float tintG = 1.02F;
    private static float tintB = 1.32F;

    /** FOV 专用的平滑值（4 tick 到位），与滤镜强度解耦 */
    private static float fovIntensity;

    private static boolean diagnosed;
    private static boolean chainMissingLogged;

    /** 等待释放的旧 uniform buffer（延后几帧，避免帧图还在引用就被回收）。 */
    private static final List<GpuBuffer> RETIRED = new ArrayList<>();
    private static final int RETIRE_DELAY = 3;

    private SandevistanPostProcessor() {
    }

    /**
     * 激活时把 FOV 拉大：视野变宽 = 速度感。
     *
     * <p><b>Fabric 侧无等价事件</b>：fabric-api 0.155.3+26.1.2 里没有
     * {@code ComputeFovModifierEvent} 的对应物（本机实测：41 个模块、fabric-rendering-v1
     * 的 33 个 API 类里都没有 FOV 相关回调）。替代方案是 mixin 注入原版
     * {@code AbstractClientPlayer.getFieldOfViewModifier(boolean,float)}（描述符 {@code (ZF)F}，
     * javap 实测；{@code LocalPlayer}/{@code Player} 均未覆写，所以本地玩家必然走到），
     * 由 {@code AbstractClientPlayerMixin} 把返回值交给本方法。
     *
     * <p>乘法语义保持不变：乘在原版算出的修正值上，疾跑/瞄准/水下不会被覆盖。
     */
    public static float applyFovModifier(float original) {
        if (fovIntensity <= 0.01F) {
            return original;
        }
        return original * (1.0F + FOV_BOOST * fovIntensity);
    }

    /** 客户端每 tick 调一次：把强度朝目标值推进。 */
    public static void tick() {
        previousIntensity = intensity;
        Minecraft minecraft = Minecraft.getInstance();
        // 两个效果共用同一条后处理链：强度取更强的一方，色调由主导者在决定
        boolean dilation = minecraft.level != null && ClientTimeDilation.ratioNow() > 0.01F;
        float berserkIntensity = BerserkClientState.intensity();
        boolean berserk = berserkIntensity > 0.01F;
        boolean active = dilation || berserk;

        float step = active ? 1.0F / FADE_IN_TICKS : 1.0F / FADE_OUT_TICKS;
        intensity = Mth.clamp(intensity + (active ? step : -step), 0.0F, 1.0F);

        float[] want = (berserk && berserkIntensity >= (dilation ? 1.0F : 0.0F))
                ? TINT_BERSERK : TINT_DILATION;
        tintR = want[0];
        tintG = want[1];
        tintB = want[2];

        // FOV 单独一条曲线：8 tick 内拉到位，结束时同样 8 tick 收回
        float fovStep = 1.0F / FOV_FADE_TICKS;
        fovIntensity = Mth.clamp(fovIntensity + (active ? fovStep : -fovStep), 0.0F, 1.0F);

        if (active && !wasActive) {
            // 退出世界时 level 会是 null，这里必须判空（之前漏了，日志里一串 NPE）
            pulseStartTick = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        }
        wasActive = active;
        if (intensity <= 0.0F) {
            pumpRetired(true);
        }
    }

    /**
     * 换世界时清掉与本世界绑定的渲染状态（由 {@code CyberwareClient} 在检测到 Level 变化时调用）。
     *
     * <p>为什么要清：{@code pulseStartTick} 与上传给 shader 的 {@code gameTime} 都是**绝对游戏刻**，
     * 新世界又从 0 开始；而 {@code intensity / previousIntensity / fovIntensity} 是上一世界的
     * 残留值 —— 不清的话进新世界的第一帧会以旧强度闪一下屏幕特效。
     *
     * <p>**故意不清**这三个：
     * <ul>
     *   <li>{@code disabled}　—— 一次出错永久停用（清了会反复尝试已经坏掉的链）</li>
     *   <li>{@code diagnosed} / {@code chainMissingLogged}　—— 一次性日志去重标志（清了会刷屏）</li>
     * </ul>
     * {@code RETIRED}（等几帧释放的 GPU buffer）也不动：它跟世界无关，清了反而可能提前回收。
     */
    public static void resetWorldState() {
        intensity = 0.0F;
        previousIntensity = 0.0F;
        fovIntensity = 0.0F;
        pulseStartTick = 0L;
        wasActive = false;
        lastUploaded = -1.0F;
    }

    /**
     * 帧结束前自己把后处理链跑一遍。
     *
     * <p><b>为什么不用 {@code FrameGraphSetupEvent}</b>：那个事件确实触发、
     * PostChain 也确实加载了（诊断日志证实），但节点加进去之后画面毫无变化 ——
     * 本机由 Sodium 接管世界渲染管线，我们写回 {@code minecraft:main} 的内容
     * 被它后续的合成覆盖掉了。{@code FlipFrameEvent} 也试过，太晚（帧已提交）。
     *
     * <p>现在挂在 {@code RenderGuiEvent.Pre}：世界渲染已经完成、GUI 还没开始画，
     * 正好是「对世界画面做一次后处理」的窗口。
     *
     * <p>{@code PostChain.process(target, allocator)} 会**自己建帧图、自己执行**，
     * 不依赖任何外部帧图，配 {@code GraphicsResourceAllocator.UNPOOLED} 就能独立跑完。
     */
    public static void applyPostProcess(com.mojang.blaze3d.resource.GraphicsResourceAllocator allocator) {
        if (disabled || !CyberwareConfig.POST_EFFECT_ENABLED) {
            return;
        }
        try {
            if (intensity <= 0.001F) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            PostChain chain = minecraft.getShaderManager().getPostChain(CHAIN_ID, LevelTargetBundle.MAIN_TARGETS);
            if (chain == null) {
                if (!chainMissingLogged) {
                    chainMissingLogged = true;
                    Cyberware.LOGGER.warn("[cyberware] PostChain 加载失败，后处理不会生效");
                }
                return;
            }
            if (!diagnosed) {
                diagnosed = true;
                Cyberware.LOGGER.info("[cyberware] 后处理已在原版执行点运行, intensity={}, allocator={}",
                        intensity, allocator.getClass().getSimpleName());
            }
            upload(chain, intensity, minecraft.level == null ? 0L : minecraft.level.getGameTime());
            chain.process(minecraft.getMainRenderTarget(), allocator);
        } catch (Throwable t) {
            disabled = true;
            Cyberware.LOGGER.warn("[cyberware] 后处理已停用（{}）", t.toString());
        }
    }

    /*
     * 【Fabric 移植 · 降级说明】
     *
     * 原来的 onFrameGraph / frameGraphInternal 挂的是 NeoForge 的 FrameGraphSetupEvent，
     * 用 event.getFrameGrapBuilder() 往帧图里加节点。Fabric 侧两条路都没有：
     *   1) fabric-rendering-v1 的 LevelRenderEvents（12 个阶段）确实有 END_MAIN 这类时机，
     *      但 LevelRenderContext 只暴露 submitNodeCollector()/poseStack()/bufferSource()，
     *      **不提供 FrameGraphBuilder**（本机实测），接不上 addToFrame(...)；
     *   2) 就算接上，原注释已记录过：Sodium 接管世界渲染管线后，写回 minecraft:main
     *      的内容会被后续合成覆盖。
     *
     * 因此这里**整段删除**（不是注释保留）：可用的执行路径只剩下面 applyPostProcess，
     * 由 GameRendererMixin 在 LevelRenderer.doEntityOutline() 之后手动调用
     * chain.process(target, resourcePool) —— 那条路径不依赖任何帧图，本机实测所需字段与
     * 方法（CrossFrameResourcePool resourcePool / render(DeltaTracker,boolean) /
     * doEntityOutline()）在 Fabric 版 GameRenderer 与 LevelRenderer 里都在。
     *
     * 代价：这一版没有帧图内的后处理挂载点；若将来要接 Iris/Sodium 之外的管线，需重新设计。
     */

    /**
     * 把一组新的 uniform 值换进第一个 pass。
     *
     * <p>顺序必须与 {@code sandevistan_edge.fsh} 里 {@code CyberConfig} 的声明顺序一致。
     */
    private static void upload(PostChain chain, float value, long gameTime) {
        List<PostPass> passes = ((PostChainAccessor) chain).getPasses();
        if (passes.isEmpty()) {
            return;
        }
        Map<String, GpuBuffer> uniforms = ((PostPassAccessor) passes.get(0)).getCustomUniforms();
        GpuBuffer old = uniforms.get(UNIFORM_BLOCK);
        if (old == null) {
            return;
        }
        if (Math.abs(value - lastUploaded) < UPLOAD_EPSILON && lastUploaded >= 0.0F) {
            return;
        }
        lastUploaded = value;

        float pulse = (gameTime % 40L) / 40.0F;
        // Fabric 移植：原来用 lwjgl 的 MemoryUtil.memAlloc(64)（本机编译环境没有 lwjgl）。
        // 改用 java.nio 的堆外直连缓冲 —— 两者都是 direct buffer，语义等价；
        // ★ 但必须显式设 nativeOrder：memAlloc 返回的就是 nativeOrder，而 allocateDirect
        //   默认 BIG_ENDIAN，漏了这一句会让 Std140 的浮点字节序翻转（着色器收到垃圾值）。
        // 代价：不再手动 memFree，改由 GC 回收（每次强度变化才分配 64 字节，量可忽略）。
        ByteBuffer data = ByteBuffer.allocateDirect(64).order(java.nio.ByteOrder.nativeOrder());
        Std140Builder builder = Std140Builder.intoBuffer(data);
        builder.putFloat(value)
                .putFloat(STRENGTH)
                .putFloat(BLUR_START)
                .putFloat(BLUR_FULL)
                .putFloat(WARP_AMOUNT)
                .putFloat(CHROMATIC)
                .putFloat(SAMPLES)
                .putFloat(pulse)
                .putFloat(tintR)
                .putFloat(tintG)
                .putFloat(tintB);
        uniforms.put(UNIFORM_BLOCK,
                RenderSystem.getDevice().createBuffer(() -> "cyberware cyberconfig", GpuBuffer.USAGE_UNIFORM, builder.get()));
        RETIRED.add(old);
        pumpRetired(false);
    }

    /** 释放排队中的旧 buffer。{@code flushAll} 表示效果已结束，可以一次清空。 */
    private static void pumpRetired(boolean flushAll) {
        while (!RETIRED.isEmpty() && (flushAll || RETIRED.size() > RETIRE_DELAY)) {
            GpuBuffer buffer = RETIRED.remove(0);
            buffer.close();
        }
    }
}
