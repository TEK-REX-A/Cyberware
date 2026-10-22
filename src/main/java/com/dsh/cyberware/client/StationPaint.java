package com.dsh.cyberware.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

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
    static final int RADIUS_PANEL = 4;
    static final int RADIUS_CARD = 2;

    private StationPaint() {
    }

    /** 按比例改 alpha（覆盖原 alpha）。 */
    static int withAlpha(int color, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
        return (a << 24) | (color & 0x00FFFFFF);
    }

    /** 圆角矩形：四角按半圆逐行内缩（规范 4.2 的零依赖做法）。 */
    static void roundRect(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int rr = Math.max(0, Math.min(r, Math.min(w / 2, h / 2)));
        if (rr == 0) {
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
        outline(g, x - 1, y - 1, w + 2, h + 2, withAlpha(color, 0.10F));
        outline(g, x, y, w, h, withAlpha(color, 0.25F));
        outline(g, x + 1, y + 1, w - 2, h - 2, withAlpha(color, 0.45F));
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
            g.fill(x, y + 2, x + 2, y + h - 2, ACCENT_CYAN);
        } else if (hovered) {
            g.fill(x, y + 2, x + 2, y + h - 2, withAlpha(ACCENT_CYAN, 0.55F));
        }
    }

    /** 按钮底（规范 3.3）：禁用 = 深底 + 灰描边且不响应悬停。按下态未接线（见类注释）。 */
    static void button(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean enabled, boolean hovered) {
        int bg = !enabled ? BG_DEEP : (hovered ? BG_HOVER : BG_CARD);
        roundRect(g, x, y, w, h, RADIUS_CARD, bg);
        outline(g, x, y, w, h, enabled ? withAlpha(ACCENT_CYAN, hovered ? 0.55F : 0.30F)
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
        outline(g, x, y, w, h, withAlpha(color, 0.35F));
    }

    /** 图标位（设计稿卡片左边那个圆角小方块）。 */
    static void iconSlot(GuiGraphicsExtractor g, int x, int y, int size) {
        roundRect(g, x, y, size, size, RADIUS_CARD, BG_DEEP);
        outline(g, x, y, size, size, withAlpha(LINE, 0.7F));
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
