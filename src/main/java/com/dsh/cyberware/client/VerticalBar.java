package com.dsh.cyberware.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * 竖向细进度条 —— 斯安威斯坦与狂暴 HUD 共用的画法。
 *
 * <p>按主人手绘的那套来：条子是<b>竖向</b>的，底部对齐、向上生长；配色走「现代化」
 * 的灰阶（他画图时涂白色只是为了显眼，游戏里不要那么刺眼）；再顺手加一条上下扫动的
 * 扫描线，让静止的条子也能看出「还在跑」。
 *
 * <p>两根条并排时会自然形成一组，跟主人画的那几根竖条是同一个意思。
 */
public final class VerticalBar {

    /** 轨道：深灰半透明，透出后面的画面（主人要求整体走半透明） */
    private static final int TRACK = 0x6E12161B;
    /** 外描边：比轨道亮一档的灰，同样半透明 */
    private static final int OUTLINE = 0x8C3A424C;
    /** 扫描线：白色半透明 */
    private static final int SCAN = 0x8AFFFFFF;
    /** 顶端正的高光：充能到哪亮到哪 */
    private static final int TIP = 0xAAFFFFFF;
    /** 扫描一个来回的周期（毫秒） */
    private static final long SCAN_PERIOD_MS = 1400L;
    /** 竖向渐变的分段数：够顺滑，又不至于每帧几百次 fill */
    private static final int SEGMENTS = 8;

    private VerticalBar() {
    }

    /**
     * 画一根竖条。
     *
     * @param x       左边界
     * @param yBottom 底边（条子从这里往上长）
     * @param w       宽度
     * @param h       满进度时的高度
     * @param progress 0..1
     * @param fillLow  底部的填充色
     * @param fillHigh 顶端的填充色
     */
    public static void draw(GuiGraphicsExtractor g, int x, int yBottom, int w, int h,
                            float progress, int fillLow, int fillHigh) {
        float p = Math.max(0.0F, Math.min(1.0F, progress));
        int filled = Math.round(h * p);
        int top = yBottom - h;

        // 轨道
        g.fill(x, top, x + w, yBottom, TRACK);

        if (filled > 0) {
            int fillTop = yBottom - filled;
            int segments = Math.min(SEGMENTS, filled);
            for (int s = 0; s < segments; s++) {
                int y0 = fillTop + filled * s / segments;
                int y1 = fillTop + filled * (s + 1) / segments;
                float t = segments <= 1 ? 1.0F : (float) s / (segments - 1);
                g.fill(x + 1, y0, x + w - 1, y1, lerp(fillLow, fillHigh, t));
            }
            // 顶面高光
            g.fill(x + 1, fillTop, x + w - 1, fillTop + 1, TIP);
        }

        // 扫描线：只在有填充的高度里来回扫，条子空了就停
        if (filled > 3) {
            float phase = (float) (System.currentTimeMillis() % SCAN_PERIOD_MS) / SCAN_PERIOD_MS;
            // 三角波：下 → 上 → 下，比锯齿波少一次突兀的瞬跳
            float tri = phase < 0.5F ? phase * 2.0F : (1.0F - phase) * 2.0F;
            int scanY = yBottom - 2 - Math.round((filled - 2) * tri);
            g.fill(x + 1, scanY, x + w - 1, scanY + 1, SCAN);
        }

        // 描边
        g.fill(x, top, x + w, top + 1, OUTLINE);
        g.fill(x, yBottom - 1, x + w, yBottom, OUTLINE);
        g.fill(x, top, x + 1, yBottom, OUTLINE);
        g.fill(x + w - 1, top, x + w, yBottom, OUTLINE);
    }

    static int lerp(int from, int to, float t) {
        t = Math.max(0.0F, Math.min(1.0F, t));
        int a = (from >> 24) & 0xFF;
        int r = (from >> 16) & 0xFF;
        int gg = (from >> 8) & 0xFF;
        int b = from & 0xFF;
        int ta = (to >> 24) & 0xFF;
        int tr = (to >> 16) & 0xFF;
        int tg = (to >> 8) & 0xFF;
        int tb = to & 0xFF;
        return ((int) (a + (ta - a) * t) << 24)
                | ((int) (r + (tr - r) * t) << 16)
                | ((int) (gg + (tg - gg) * t) << 8)
                | (int) (b + (tb - b) * t);
    }
}
