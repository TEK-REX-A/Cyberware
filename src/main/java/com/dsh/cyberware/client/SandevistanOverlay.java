package com.dsh.cyberware.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * 屏幕色调 + 暗角。
 *
 * <p><b>为什么色调放在这里而不是 shader 里</b>：真正吃 GPU 的边缘模糊/色散走的是
 * NeoForge 帧图（{@code FrameGraphSetupEvent}），但那一步在本机（Sodium 接管渲染管线）
 * 会被后续的合成覆盖掉 —— 帧图事件确实触发了，画面却什么都没有。
 * 颜色这块用 GUI 层叠一层半透明色块就能做到，稳定、零兼容风险，
 * 于是「染色」交给这里，「模糊」继续留给 shader。
 */
public final class SandevistanOverlay {

    /** 暗角总厚度（像素） */
    private static final int BAND = 44;
    private static final int LAYERS = 12;
    private static final int MAX_ALPHA = 58;
    private static final int VIGNETTE_RGB = 0x070910;

    /** 全屏色调峰值透明度（主人反馈 64 太淡，加深到 105） */
    private static final int TINT_ALPHA = 105;

    /** 斯安威斯坦：冷青蓝 */
    private static final int TINT_DILATION = 0x3FA8FF;
    /** 狂暴：红 */
    private static final int TINT_BERSERK = 0xFF3B36;

    private SandevistanOverlay() {
    }

    public static void onRenderGui(RenderGuiEvent.Post event) {
        float dilation = ClientTimeDilation.ratioNow();
        float berserk = BerserkClientState.intensity();
        if (dilation <= 0.01F && berserk <= 0.01F) {
            return;
        }
        GuiGraphicsExtractor g = event.getGuiGraphics();
        int w = g.guiWidth();
        int h = g.guiHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        // 谁主导就用谁的色：狂暴优先
        boolean berserkMode = berserk > 0.01F && berserk >= (dilation > 0.01F ? 1.0F : 0.0F);
        int tint = berserkMode ? TINT_BERSERK : TINT_DILATION;
        float strength = Math.max(dilation, berserk);

        // 全屏染色已交回后处理（shader 里做乘法混合，观感好得多）。
        // 这里只留暗角 —— GUI 这一层的价值在于「框架感」，染色交给它做会显得发雾。
        if (false && TINT_ALPHA > 0) {
            int tintAlpha = (int) (TINT_ALPHA * strength);
            if (tintAlpha > 2) {
                g.fill(0, 0, w, h, (tintAlpha << 24) | tint);
            }
        }

        // 四边暗角（二次衰减）
        int step = Math.max(1, BAND / LAYERS);
        for (int i = 0; i < LAYERS; i++) {
            float t = i / (float) LAYERS;
            int alpha = (int) (MAX_ALPHA * strength * (1.0F - t) * (1.0F - t));
            if (alpha <= 2) {
                continue;
            }
            int color = (alpha << 24) | VIGNETTE_RGB;
            int off = i * step;
            g.fill(0, off, w, off + step, color);
            g.fill(0, h - off - step, w, h - off, color);
            g.fill(off, 0, off + step, h, color);
            g.fill(w - off - step, 0, w - off, h, color);
        }
    }
}
