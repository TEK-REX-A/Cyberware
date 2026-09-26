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
 * 狂暴 HUD —— 与斯安威斯坦同款的<b>竖向</b>细进度条，暖灰。
 *
 * <p>狂暴期间原版生命值/饥饿/护甲会被隐藏（{@code GuiMixin}），
 * <b>这根条子就是玩家唯一还能看到的自身状态</b> —— 正好对应「身体感知被抑制」那个设定：
 * 数字还在，但你感觉不到疼。
 *
 * <p>它与 {@link SandevistanHud} 并排（右边一根），标签也往上挂，两根条不会打架。
 */
public final class BerserkHud {

    /** 与斯安威斯坦的条并排 */
    private static final int BAR_X = 30;
    private static final int BAR_W = 10;
    private static final int BAR_H = 96;
    private static final int BAR_BOTTOM_GAP = 40;
    /** 标签比斯安威斯坦那行低 12px，两行小字正好错开 */
    private static final int LABEL_GAP = 12;

    /** 暖灰：低 → 高（整体半透明，能看见后面的画面） */
    private static final int FILL_LOW = 0xBE927874;
    private static final int FILL_HIGH = 0xBEE6D4D0;
    private static final int DIM = 0xD8D4C4C1;

    private static final Identifier ICON = Identifier.fromNamespaceAndPath(
            Cyberware.MODID, "textures/item/berserk_militech.png");
    private static final int ICON_SIZE = 11;

    private BerserkHud() {
    }

    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!BerserkClientState.active()) {
            return;
        }
        float progress = BerserkClientState.progress();
        if (progress <= 0.0F) {
            return;
        }

        GuiGraphicsExtractor g = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;
        int yBottom = g.guiHeight() - BAR_BOTTOM_GAP;

        VerticalBar.draw(g, BAR_X, yBottom, BAR_W, BAR_H, progress, FILL_LOW, FILL_HIGH);

        int textY = yBottom - BAR_H - LABEL_GAP;
        g.blit(RenderPipelines.GUI_TEXTURED, ICON, BAR_X - 1, textY - 1,
                0.0F, 0.0F, ICON_SIZE, ICON_SIZE, 32, 32);
        String label = "Berserk  " + Math.round(progress * 100) + "%  "
                + String.format(java.util.Locale.ROOT, "%.1fs", BerserkClientState.remainingSeconds());
        g.text(font, Component.literal(label), BAR_X + ICON_SIZE + 2, textY + 1, DIM);
    }
}
