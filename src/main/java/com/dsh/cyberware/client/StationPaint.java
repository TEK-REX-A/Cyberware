package com.dsh.cyberware.client;

import com.dsh.cyberware.Cyberware;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * 义体操作台专用绘制工具 + 设计 token。
 *
 * <p>全部只用 {@link GuiGraphicsExtractor#fill} 拼出来，<b>不引入任何第三方 UI 依赖</b>
 * （见 {@code Cyberware-UI-规范.md} 第三节与第六节验收项）。
 *
 * <p><b>字号分级说明（待核实）</b>：规范 2.2 要 4 级字号（16/12/9/7 px），但原版字体只有
 * 一档 9px 行高，缩放要靠 {@code pose().scale()} —— 本工程现有代码里<b>没有任何 pose 用法</b>
 * 可作签名参照，按「禁止编造 API」的约束这里先不写，用「颜色 + 留白」表达层级：
 * H1 = text-primary、H2 = text-secondary + 左侧 2px 竖条、body = text-primary、caption = 次级色。
 * 真要上真字号，得先核实 26.x 的 {@code GuiGraphicsExtractor} 是否暴露 pose 栈。
 */
final class StationPaint {

    // ---- 色板（规范 2.1，一个字节都不许改） ----
    static final int BG_DEEP = 0xFF0B0F14;
    static final int BG_PANEL = 0xEB131A22;
    static final int BG_CARD = 0xFF1A232D;
    static final int BG_HOVER = 0xFF223040;
    static final int LINE = 0xFF2C3A4A;
    static final int TEXT_PRIMARY = 0xFFE6EDF3;
    static final int TEXT_SECONDARY = 0xFF8A98A8;
    static final int TEXT_DISABLED = 0xFF55636F;
    static final int ACCENT_CYAN = 0xFF28E0D0;
    static final int ACCENT_MAGENTA = 0xFFFF2E88;
    static final int WARN_AMBER = 0xFFFFB020;
    static final int OK_GREEN = 0xFF4ADE80;

    // ---- 间距 / 圆角（规范 2.3：只用 4 / 8 / 16，圆角 0 / 2 / 4） ----
    static final int GAP_XS = 4;
    static final int GAP_S = 8;
    static final int GAP_L = 16;
    static final int RADIUS_PANEL = 8;
    static final int RADIUS_CARD = 4;

    private StationPaint() {
    }

    /** 按比例改 alpha（覆盖原 alpha）。 */
    static int withAlpha(int color, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
        return (a << 24) | (color & 0x00FFFFFF);
    }

    /** 圆角贴图：12×12 纯白，alpha 承担形状（圆角半径 4，8×8 超采样抗锯齿）。 */
    private static final Identifier ROUND_TEX =
            Identifier.fromNamespaceAndPath(Cyberware.MODID, "textures/gui/round32.png");
    /** 圆角描边贴图：同一套几何，1px 厚的环（九宫格用它画真圆角边框）。 */
    private static final Identifier ROUND_RING_TEX =
            Identifier.fromNamespaceAndPath(Cyberware.MODID, "textures/gui/round32_ring.png");
    /**
     * 贴图边长 = 10（角）+ 12（边）+ 10（角） = 32。
     *
     * <p>为什么从 12×12/半径 4 换成 32×32/半径 10：原先那个角弧只有 <b>4 个像素</b>，
     * 拉到面板尺寸就是 4 级台阶，抗锯齿只能柔化、消不掉锯齿（主人实机一眼看出来）。
     * 现在源里有 10px 的弧，缩到 4/2px 的角是**降采样**，弧就顺了。
     */
    private static final int ROUND_TEX_SIZE = 32;
    /** 角切片边长（源像素）。 */
    private static final int ROUND_SLICE = 10;
    /** 边/中心切片边长（源像素） = 32 - 10*2。 */
    private static final int ROUND_MID = 12;

    /**
     * <b>真圆角</b>：九宫格 blit 一张 12×12 的圆角贴图。
     *
     * <p>为什么不用「逐行内缩 fill」：那种做法放大看是一级级台阶（主人在实机上发现了）。
     * 26.1.2 的 {@code GuiGraphicsExtractor} 没有 {@code blitNineSliced}（javap 核实：只有
     * blit / blitSprite），所以九宫格自己拼：四角 4×4 不拉伸、四边单向拉伸、中心双向拉伸。
     *
     * <p>贴图是纯白的，{@code color} 的 RGB 乘上去正好是目标色（MC 的 blit 是乘算），
     * alpha 也乘算，所以贴图的边缘渐变保留成抗锯齿。
     */
    static void drawRound(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int r = Math.min(radius, Math.min(w / 2, h / 2));
        if (r <= 0) {
            g.fill(x, y, x + w, y + h, color);
            return;
        }
        // ① 首选：SDF 着色器（真圆角 + 解析抗锯齿）
        if (RoundPipelines.draw(g, x, y, w, h, r, color, false)) {
            diag(0, "shader-fill", x, y, w, h, r, color);
            return;
        }
        // ② 退回贴图九宫格
        if (!roundTextureOk()) {
            fallbackRound(g, x, y, w, h, r, color);
            diag(2, "inset-fallback", x, y, w, h, r, color);
            return;
        }
        diag(1, "texture9-fill", x, y, w, h, r, color);
        int midW = w - 2 * r;
        int midH = h - 2 * r;
        // 直边与中心：纯 fill（不拉伸贴图；拉伸会把 1px 的 alpha 渐变拉成周期性波浪）
        g.fill(x + r, y, x + w - r, y + h, color);
        if (midH > 0) {
            g.fill(x, y + r, x + r, y + h - r, color);
            g.fill(x + w - r, y + r, x + w, y + h - r, color);
        }
        // 四个角：只用角的 10×10 源切片缩放（这是唯一需要贴图的地方）
        blitTex(g, ROUND_TEX, x, y, 0, 0, r, r, color);
        blitTex(g, ROUND_TEX, x + w - r, y, ROUND_TEX_SIZE - ROUND_SLICE, 0, r, r, color);
        blitTex(g, ROUND_TEX, x, y + h - r, 0, ROUND_TEX_SIZE - ROUND_SLICE, r, r, color);
        blitTex(g, ROUND_TEX, x + w - r, y + h - r,
                ROUND_TEX_SIZE - ROUND_SLICE, ROUND_TEX_SIZE - ROUND_SLICE, r, r, color);
    }

    /** 真圆角描边（1px 环，同样九宫格）。 */
    static void drawRoundOutline(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int r = Math.min(radius, Math.min(w / 2, h / 2));
        if (r <= 0) {
            outline(g, x, y, w, h, color);
            return;
        }
        // ① 首选：SDF 描边（同一 shader 的 STROKE 模式）
        if (RoundPipelines.draw(g, x, y, w, h, r, color, true)) {
            diag(0, "shader-stroke", x, y, w, h, r, color);
            return;
        }
        // ② 退回「4 条 fill + 角环贴图」
        if (!roundTextureOk()) {
            outline(g, x, y, w, h, color);
            diag(2, "flat-outline", x, y, w, h, r, color);
            return;
        }
        diag(1, "fill+corner-stroke", x, y, w, h, r, color);
        // 四条直边用 1px fill：边缘永远是干净的直线（原来用环贴图横向拉伸，
        // 亚像素落点每格不同 → 主人的「小波浪/虚线」就是它）
        g.fill(x + r, y, x + w - r, y + 1, color);
        g.fill(x + r, y + h - 1, x + w - r, y + h, color);
        if (h - 2 * r > 0) {
            g.fill(x, y + r, x + 1, y + h - r, color);
            g.fill(x + w - 1, y + r, x + w, y + h - r, color);
        }
        // 四个角：环贴图的角切片（只缩角，不拉长）
        blitTex(g, ROUND_RING_TEX, x, y, 0, 0, r, r, color);
        blitTex(g, ROUND_RING_TEX, x + w - r, y, ROUND_TEX_SIZE - ROUND_SLICE, 0, r, r, color);
        blitTex(g, ROUND_RING_TEX, x, y + h - r, 0, ROUND_TEX_SIZE - ROUND_SLICE, r, r, color);
        blitTex(g, ROUND_RING_TEX, x + w - r, y + h - r,
                ROUND_TEX_SIZE - ROUND_SLICE, ROUND_TEX_SIZE - ROUND_SLICE, r, r, color);
    }

    /**
     * 贴图 12×12，u/v/w/h 都是贴图像素。
     *
     * <p><b>必须传 color</b>：贴图是纯白的，不着色就是一片白（v0.5.4 首个真机版本就这么翻车过）。
     * 用的是 {@code blit(pipeline, id, x, y, u, v, w, h, texW, texH, color)} 这个 11 参重载
     * —— javap 看过它的字节码：最后那个 int 一路传进 {@code innerBlit(..., int color)}。
     */
    private static void blitTex(GuiGraphicsExtractor g, Identifier tex, int x, int y, int u, int v, int w, int h,
                                int color) {
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, (float) u, (float) v, w, h,
                ROUND_TEX_SIZE, ROUND_TEX_SIZE, color);
    }

    /** 诊断：圆角第一笔走哪一级、参数是什么，只打一次。 */
    private static final AtomicInteger DIAG_TIER = new AtomicInteger(-1);

    private static void diag(int tier, String name, int x, int y, int w, int h, int r, int color) {
        if (!DIAG_TIER.compareAndSet(-1, tier)) {
            return;
        }
        System.out.println("[cyberware][diag] 圆角第一笔: 走了=" + name
                + " | " + RoundPipelines.describe()
                + " | 参数 x=" + x + " y=" + y + " w=" + w + " h=" + h
                + " r=" + r + " color=0x" + Integer.toHexString(color));
    }

    /** 圆角贴图是否可用：只查一次并缓存（排错日志也只打一次）。 */
    private static final AtomicBoolean TEX_CHECKED = new AtomicBoolean(false);
    private static boolean roundTexAvailable = true;

    /**
     * 贴图可用吗？
     *
     * <p>不可用时 {@link #drawRound} 会退回「逐行内缩」的老画法 —— 有台阶但界面能看。
     * 这是 P0 兜底：宁可圆角难看，也不能让整个操作台变成白底/棋盘格。
     */
    private static boolean roundTextureOk() {
        if (TEX_CHECKED.compareAndSet(false, true)) {
            try {
                Minecraft minecraft = Minecraft.getInstance();
                roundTexAvailable = minecraft != null && minecraft.getResourceManager() != null
                        && !minecraft.getResourceManager().getResourceStack(ROUND_TEX).isEmpty();
                System.out.println("[cyberware] 圆角贴图 " + ROUND_TEX
                        + " 资源存在=" + roundTexAvailable
                        + (roundTexAvailable ? "" : " —— 回退到逐行内缩画法"));
            } catch (Throwable ignored) {
                roundTexAvailable = false;
            }
        }
        return roundTexAvailable;
    }

    /** 兜底圆角：逐行内缩（老实现）。只在贴图不可用时使用。 */
    private static void fallbackRound(GuiGraphicsExtractor g, int x, int y, int w, int h, int rr, int color) {
        if (rr <= 0) {
            g.fill(x, y, x + w, y + h, color);
            return;
        }
        g.fill(x + rr, y, x + w - rr, y + h, color);
        g.fill(x, y + rr, x + rr, y + h - rr, color);
        g.fill(x + w - rr, y + rr, x + w, y + h - rr, color);
        for (int i = 0; i < rr; i++) {
            double dy = rr - i - 0.5D;
            int inset = rr - (int) Math.round(Math.sqrt(Math.max(0.0D, (double) rr * rr - dy * dy)));
            g.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            g.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, color);
        }
    }

    /**
     * 圆角矩形（历史入口，<b>已改为真圆角</b>，内部走 {@link #drawRound}）。
     *
     * @deprecated 直接用 {@link #drawRound}；保留这个名字只是为了不动一堆调用点。
     */
    @Deprecated
    static void roundRect(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int color) {
        drawRound(g, x, y, w, h, r, color);
    }

    /** 1px 矩形描边（四条 fill）。 */
    static void outline(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) {
            return;
        }
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    /** 三层发光描边（规范 4.3：alpha 0.10 / 0.25 / 0.45，向外各扩 1px）。 */
    static void glow(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        drawRoundOutline(g, x - 1, y - 1, w + 2, h + 2, RADIUS_PANEL + 1, withAlpha(color, 0.10F));
        drawRoundOutline(g, x, y, w, h, RADIUS_PANEL, withAlpha(color, 0.25F));
        drawRoundOutline(g, x + 1, y + 1, w - 2, h - 2, RADIUS_PANEL, withAlpha(color, 0.45F));
    }

    /** 立体感：顶部 1px 亮线 + 底部 1px 暗线（规范 3.1）。 */
    static void innerShade(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        if (w <= 2 || h <= 2) {
            return;
        }
        g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x0FFFFFFF);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0x26000000);
    }

    /** 面板：圆角 + 半透明底 + 内阴影 + 淡发光。 */
    static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        roundRect(g, x, y, w, h, RADIUS_PANEL, BG_PANEL);
        innerShade(g, x, y, w, h);
        glow(g, x, y, w, h, 0xFF7FB6C4);
    }

    /** 卡片：圆角 2、无边框；选中/悬停时加左侧 2px 强调竖条（规范 3.2）。 */
    static void card(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean selected, boolean hovered) {
        int bg = selected ? BG_HOVER : (hovered ? BG_HOVER : BG_CARD);
        roundRect(g, x, y, w, h, RADIUS_CARD, bg);
        if (selected) {
            g.fill(x, y + 3, x + 2, y + h - 3, ACCENT_CYAN);
        } else if (hovered) {
            g.fill(x, y + 3, x + 2, y + h - 3, withAlpha(ACCENT_CYAN, 0.55F));
        }
    }

    /** 按钮底（规范 3.3）：禁用 = 深底 + 灰描边且不响应悬停。按下态未接线（见类注释）。 */
    static void button(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean enabled, boolean hovered) {
        int bg = !enabled ? BG_DEEP : (hovered ? BG_HOVER : BG_CARD);
        roundRect(g, x, y, w, h, RADIUS_CARD, bg);
        drawRoundOutline(g, x, y, w, h, RADIUS_CARD, enabled ? withAlpha(ACCENT_CYAN, hovered ? 0.55F : 0.30F)
                : withAlpha(LINE, 0.55F));
    }

    /**
     * 容量条（规范 3.4）：轨道 bg-deep + 内阴影，填充强调色，高光 1px；
     * 剩余 &lt; 20% 换 warn-amber，装满换 accent-magenta。
     *
     * @param used  已占用容量
     * @param limit 总上限；{@code <= 0} 或无限时按「未启用容量限制」画满色条
     */
    static void capacityBar(GuiGraphicsExtractor g, int x, int y, int w, int h, int used, int limit) {
        roundRect(g, x, y, w, h, RADIUS_CARD, BG_DEEP);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, withAlpha(LINE, 0.45F));
        float p = limit <= 0 ? 0.0F : Math.min(1.0F, Math.max(0.0F, (float) used / (float) limit));
        int filled = Math.round((w - 2) * p);
        if (filled > 0) {
            boolean full = p >= 1.0F;
            int color = full ? ACCENT_MAGENTA : (p < 0.2F ? WARN_AMBER : ACCENT_CYAN);
            g.fill(x + 1, y + 1, x + 1 + filled, y + h - 1, withAlpha(color, 0.72F));
            g.fill(x + 1, y + 1, x + 1 + filled, y + 2, withAlpha(color, 1.0F));
            // 圆角只做右端
            int right = x + 1 + filled;
            if (filled > 1) {
                g.fill(right - 1, y + 1, right, y + 2, BG_DEEP);
                g.fill(right - 1, y + h - 2, right, y + h - 1, BG_DEEP);
            }
        }
        outline(g, x, y, w, h, withAlpha(LINE, 0.6F));
    }

    /** 标签底（规范 3.6：对应色 12% alpha，不用实心块）。 */
    static void tag(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        roundRect(g, x, y, w, h, RADIUS_CARD, withAlpha(color, 0.12F));
        drawRoundOutline(g, x, y, w, h, RADIUS_CARD, withAlpha(color, 0.35F));
    }

    /** 图标位（设计稿卡片左边那个圆角小方块）。 */
    static void iconSlot(GuiGraphicsExtractor g, int x, int y, int size) {
        roundRect(g, x, y, size, size, RADIUS_CARD, BG_DEEP);
        drawRoundOutline(g, x, y, size, size, RADIUS_CARD, withAlpha(LINE, 0.7F));
    }

    /** 占位结构框（右栏给后续功能预留的位置：只画极淡的框，不塞内容）。 */
    static void placeholder(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        if (w <= 2 || h <= 2) {
            return;
        }
        outline(g, x, y, w, h, withAlpha(LINE, 0.45F));
    }

    /** 2px 分组竖条（规范 3.1 标题左侧那条）。 */
    static void groupBar(GuiGraphicsExtractor g, int x, int y, int h) {
        g.fill(x, y, x + 2, y + h, ACCENT_CYAN);
    }
}
