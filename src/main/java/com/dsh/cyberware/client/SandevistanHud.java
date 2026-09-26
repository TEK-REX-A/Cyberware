package com.dsh.cyberware.client;

import com.dsh.cyberware.Cyberware;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * 斯安威斯坦 HUD —— <b>竖向</b>细进度条（冷灰）。
 *
 * <p>主人手绘的样式：竖条、底部对齐、向上生长；配色用「现代化」的灰阶，而不是他画图时
 * 为了显眼涂的白；条内加一条上下扫动的扫描线。标签（Σ + 百分比 + 剩余秒数）挂在条子上方。
 *
 * <p>它和 {@link BerserkHud} 是并排的两根条，同时开也只是一组竖条，不会叠。
 */
public final class SandevistanHud {

    /** 条子左边界：整组竖条贴屏幕左侧 */
    private static final int BAR_X = 12;
    private static final int BAR_W = 10;
    private static final int BAR_H = 96;
    /** 底边离屏幕底部的距离（落在热键栏上方） */
    private static final int BAR_BOTTOM_GAP = 40;
    /** 标签离条顶的距离 */
    private static final int LABEL_GAP = 24;

    /** 冷灰：低 → 高（整体半透明，能看见后面的画面） */
    private static final int FILL_LOW = 0xBE74828F;
    private static final int FILL_HIGH = 0xBED8E2EC;
    private static final int DIM = 0xD8C6CED8;

    /** 标题旁的小图标：希腊字母 Σ（自带纹理，不依赖玩家字体是否含希腊字形） */
    private static final Identifier ICON = Identifier.fromNamespaceAndPath(
            Cyberware.MODID, "textures/gui/hud/sigma.png");
    private static final int ICON_SIZE = 11;

    private SandevistanHud() {
    }

    public static void onRenderGui(RenderGuiEvent.Post event) {
        float progress = ClientTimeDilation.progress();
        float ratio = ClientTimeDilation.ratioNow();
        if (ratio <= 0.01F && progress <= 0.0F) {
            return;
        }

        GuiGraphicsExtractor g = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;
        int yBottom = g.guiHeight() - BAR_BOTTOM_GAP;

        VerticalBar.draw(g, BAR_X, yBottom, BAR_W, BAR_H, progress, FILL_LOW, FILL_HIGH);

        // 标签：图标 + 百分比 + 剩余秒数
        int textY = yBottom - BAR_H - LABEL_GAP;
        g.blit(RenderPipelines.GUI_TEXTURED, ICON, BAR_X - 1, textY - 1,
                0.0F, 0.0F, ICON_SIZE, ICON_SIZE, 32, 32);
        String label = "Sandevistan  " + Math.round(progress * 100) + "%  "
                + String.format(java.util.Locale.ROOT, "%.1fs", ClientTimeDilation.remainingSeconds());
        g.text(font, Component.literal(label), BAR_X + ICON_SIZE + 2, textY + 1, DIM);
    }
}
