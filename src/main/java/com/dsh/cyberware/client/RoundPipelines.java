package com.dsh.cyberware.client;

import com.dsh.cyberware.Cyberware;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * 圆角 SDF 着色器管线（26.1.2）。
 *
 * <h3>为什么不用 per-draw uniform 传矩形尺寸</h3>
 * GUI 通道只绑 Projection / DynamicTransforms 等固定 UBO，<b>没有 per-draw uniform</b>；
 * 而 {@code fill(...)} 走 POSITION_COLOR（没有 UV）。所以这里改用
 * {@code blit(1×1 白贴图, 源区域 1×1, 目标 w×h)} —— uv 在整个 quad 上正好是 0..1，
 * 片元里 {@code fwidth(uv)} = 1/尺寸，尺寸就自己出来了（推导见 cyber_round.fsh）。
 *
 * <p>半径用 {@code withShaderDefine} 烘成编译期常量 → 4 条管线：半径 4/2 × 填充/描边。
 *
 * <p><b>任何一步失败都会永久退回 StationPaint 的贴图实现</b>，绝不白屏、绝不崩渲染线程。
 *
 * <h3>⚠ 静态初始化顺序（血泪）</h3>
 * 日志开关 {@link #LOGGED} 必须声明在 4 条管线<b>之前</b>：{@link #build} 的 catch 里要用它，
 * 字段按声明顺序初始化，放后面就会在 build() 抛异常时拿到 null → NPE 盖掉真正的异常
 * （2026-10-24 操作台打开即崩就是这么来的）。
 * 另外 catch 里必须 {@code printStackTrace()}，别再让异常消失。
 */
final class RoundPipelines {

    /** 日志开关：必须在所有 build(...) 之前初始化。 */
    private static final AtomicBoolean LOGGED = new AtomicBoolean(false);

    /** 只借 UV 用，不采样颜色（纯白 1×1）。 */
    private static final Identifier WHITE =
            Identifier.fromNamespaceAndPath(Cyberware.MODID, "textures/gui/white1.png");

    /** shader 路径：文件在 assets/cyberware/shaders/core/cyber_round.vsh|.fsh。 */
    private static final Identifier SHADER =
            Identifier.fromNamespaceAndPath(Cyberware.MODID, "core/cyber_round");

    private static final RenderPipeline FILL_R4 = build("round_fill_4", 4.0F, false);
    private static final RenderPipeline FILL_R2 = build("round_fill_2", 2.0F, false);
    private static final RenderPipeline STROKE_R4 = build("round_stroke_4", 4.0F, true);
    private static final RenderPipeline STROKE_R2 = build("round_stroke_2", 2.0F, true);
    private static final RenderPipeline FILL_R8 = build("round_fill_8", 8.0F, false);
    private static final RenderPipeline FILL_R16 = build("round_fill_16", 16.0F, false);
    private static final RenderPipeline FILL_R32 = build("round_fill_32", 32.0F, false);
    private static final RenderPipeline STROKE_R8 = build("round_stroke_8", 8.0F, true);
    private static final RenderPipeline STROKE_R16 = build("round_stroke_16", 16.0F, true);


    /** 着色器通道是否可用；任一条建不出来 / 任一次绘制抛异常就永久关闭。 */
    private static volatile boolean available =
            FILL_R4 != null && FILL_R2 != null && STROKE_R4 != null && STROKE_R2 != null;

    private RoundPipelines() {
    }

    /** 诊断用：拿到管线与可用性的快照。 */
    static String describe() {
        return "available=" + available
                + " FILL_R4=" + (FILL_R4 != null)
                + " FILL_R2=" + (FILL_R2 != null)
                + " STROKE_R4=" + (STROKE_R4 != null)
                + " STROKE_R2=" + (STROKE_R2 != null);
    }

    static boolean isAvailable() {
        return available;
    }

    /** 按半径挑最接近的填充管线（档位：2 / 4 / 8 / 16 / 32）。 */
    private static RenderPipeline fillPipeline(int radius) {
        if (radius <= 2) {
            return FILL_R2;
        }
        if (radius <= 4) {
            return FILL_R4;
        }
        if (radius <= 8) {
            return FILL_R8;
        }
        return radius <= 16 ? FILL_R16 : FILL_R32;
    }

    private static RenderPipeline strokePipeline(int radius) {
        if (radius <= 2) {
            return STROKE_R2;
        }
        if (radius <= 4) {
            return STROKE_R4;
        }
        return radius <= 8 ? STROKE_R8 : STROKE_R16;
    }


    /**
     * 建一条管线。
     *
     * <p>26.1.2 的管线不需要注册：GlDevice 首次 setPipeline 时按需编译并进 pipelineCache。
     *
     * <p>⚠ location / shader id 一律用 <b>Identifier 重载</b>：String 重载走
     * {@code Identifier.withDefaultNamespace}（javap 确认），mod 的命名空间会被吃掉甚至抛
     * InvalidIdentifierException。
     */
    private static RenderPipeline build(String tag, float radius, boolean stroke) {
        try {
            return RenderPipeline.builder()
                    .withLocation(Identifier.fromNamespaceAndPath(Cyberware.MODID, "pipeline/" + tag))
                    .withVertexShader(SHADER)
                    .withFragmentShader(SHADER)
                    .withShaderDefine("CY_RADIUS", radius)
                    .withShaderDefine("CY_STROKE", stroke ? 1 : 0)
                    .withSampler("Sampler0")
                    .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
                    .withUniform("Projection", UniformType.UNIFORM_BUFFER)
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
                    .withCull(false)
                    .withDepthStencilState(Optional.empty())
                    .build();
        } catch (Throwable t) {
            if (LOGGED.compareAndSet(false, true)) {
                System.out.println("[cyberware] 圆角 shader 管线创建失败（退回贴图实现）: " + t);
                t.printStackTrace();
            }
            return null;
        }
    }

    /**
     * 画一个圆角矩形。
     *
     * @return true = 已用 shader 画完；false = 不可用，调用方走贴图兜底
     */
    static boolean draw(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int color,
                        boolean stroke) {
        boolean fillMode = !stroke;
        RenderPipeline pipeline = stroke ? strokePipeline(radius) : fillPipeline(radius);
        if (!available || w <= 0 || h <= 0 || pipeline == null) {
            return false;
        }
        try {
            // 目标 w×h、源区域 1×1、贴图 1×1 → uv 在整个 quad 上 0..1
            g.blit(pipeline, WHITE, x, y, 0.0F, 0.0F, w, h, 1, 1, 1, 1, color);
            return true;
        } catch (Throwable t) {
            available = false;
            if (LOGGED.compareAndSet(false, true)) {
                System.out.println("[cyberware] 圆角 shader 绘制失败（永久退回贴图实现，fill=" + fillMode + "）: " + t);
                t.printStackTrace();
            }
            return false;
        }
    }
}

