package com.dsh.cyberware.client;

import com.dsh.cyberware.Cyberware;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * 三步客户端 HUD（t29）：<b>屏幕空间折角锁定框</b> + <b>上传进度条</b> + 提示条。
 *
 * <p>渲染回调整体 try/catch（渲染线程抛异常 = 当场崩客户端）。
 *
 * <p><b>帧率自保</b>：
 * <ul>
 *   <li><b>没有锁定目标就立刻 return</b>（不上传时也立刻 return）—— 绝大多数帧什么都不做；</li>
 *   <li>每帧最多 <b>1 次</b>投影（只给已锁定的那个目标）；候选筛选放在每 tick 一次，
 *       且每 tick 最多投影 {@link HackClientState} 里限定的候选数；</li>
 *   <li>二进制乱码只画 <b>8 个字符</b>（不是铺满）。</li>
 * </ul>
 */
public final class HackHud {

    /** 折角框：最小/最大边长（按距离插值，远了缩小） */
    private static final double BOX_MIN = 30.0D;
    private static final double BOX_MAX = 58.0D;
    /** 折角线的长度占边长的比例 */
    private static final double CORNER = 0.32D;
    /** 折角线粗细（像素） */
    private static final int LINE = 1;

    private static final int COL_FRAME = 0xE0FF3B30;
    private static final int COL_FRAME_DIM = 0x80FF3B30;
    private static final int COL_HP = 0xE0FF5555;
    private static final int COL_HP_BG = 0x80101010;
    private static final int COL_NAME = 0xE0FFD0D0;
    private static final int COL_PIP_ON = 0xE0FF37C8;
    private static final int COL_PIP_OFF = 0x50303030;

    /** 上传条 */
    private static final int UP_BAR_W = 160;
    private static final int UP_BAR_H = 2;              // 极细
    private static final int COL_UP = 0xE0FF2B2B;
    private static final int COL_UP_GLOW = 0x40FF2B2B;
    private static final int COL_UP_TEXT = 0xD0FF9A9A;
    private static final long COLLAPSE_MS = 250L;

    private static final AtomicBoolean FAIL_LOGGED = new AtomicBoolean(false);

    private HackHud() {
    }

    public static void onRenderGui(RenderGuiEvent.Post event) {
        try {
            GuiGraphicsExtractor g = event.getGuiGraphics();
            if (g == null) {
                return;
            }
            drawLockFrame(g);
            drawUploadBar(g);
            drawNote(g);
        } catch (Throwable t) {
            if (FAIL_LOGGED.compareAndSet(false, true)) {
                Cyberware.LOGGER.warn("[cyberware] 破解 HUD 渲染异常（已忽略，不影响游戏）", t);
            }
        }
    }

    // ═══════════════════ 折角锁定框 ═══════════════════

    private static void drawLockFrame(GuiGraphicsExtractor g) {
        LivingEntity target = HackClientState.lockedEntity();
        if (target == null) {
            // 没有目标/目标失效 → 立刻不做任何事（无残留、零分配）
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        double ex = target.getX();
        double ey = target.getY() + target.getBbHeight() * 0.5D;
        double ez = target.getZ();
        if (!ScreenProjection.project(ex, ey, ez)) {
            return;                                     // 身后/超距 → 本帧不画（状态由 tickLock 清）
        }
        double dist = Math.sqrt(target.distanceToSqr(minecraft.player));
        double size = Math.max(BOX_MIN, Math.min(BOX_MAX, BOX_MAX - dist * 1.2D));
        double half = size * 0.5D;
        int cx = (int) Math.round(ScreenProjection.x());
        int cy = (int) Math.round(ScreenProjection.y());
        int x0 = cx - (int) half;
        int x1 = cx + (int) half;
        int y0 = cy - (int) half;
        int y1 = cy + (int) half;

        Font font = minecraft.font;
        double corner = size * CORNER;

        // —— 三条细线折角框：左上 / 右上 / 左下（各两条 1px 线） ——
        drawCorner(g, x0, y0, corner, 1, 1);
        drawCorner(g, x1, y0, corner, -1, 1);
        drawCorner(g, x0, y1, corner, 1, -1);

        // —— 血条（框内底部，极细） ——
        float hp = Math.max(0.0F, target.getHealth());
        float maxHp = Math.max(1.0F, target.getMaxHealth());
        int barW = x1 - x0 - 4;
        int barY = y1 - 5;
        g.fill(x0 + 2, barY, x0 + 2 + barW, barY + 2, COL_HP_BG);
        g.fill(x0 + 2, barY, x0 + 2 + Math.round(barW * Math.min(1.0F, hp / maxHp)), barY + 2, COL_HP);

        // —— 名称（框上方） ——
        String name = target.getType().getDescription().getString();
        int nameW = font.width(name);
        g.text(font, Component.literal(name), cx - nameW / 2, y0 - 11, COL_NAME, false);

        // —— 可用破解图标：5 个小方块（本模组暂无破解图标素材，用几何图形占位） ——
        var hacks = HackClientState.hacks();
        int pip = 4;
        int gap = 3;
        int totalW = hacks.size() * pip + (hacks.size() - 1) * gap;
        int px = cx - totalW / 2;
        int py = y1 + 3;
        for (int i = 0; i < hacks.size(); i++) {
            boolean ok = HackClientState.affordable(hacks.get(i));
            g.fill(px, py, px + pip, py + pip, ok ? COL_PIP_ON : COL_PIP_OFF);
            px += pip + gap;
        }
    }

    private static void drawCorner(GuiGraphicsExtractor g, int x, int y, double len, int dx, int dy) {
        // 折角的横边与竖边：各画一条 LINE 像素粗的细线
        int ex = x + (int) Math.round(len * dx);
        int ey = y + (int) Math.round(len * dy);
        g.fill(Math.min(x, ex), y, Math.max(x, ex), y + LINE, COL_FRAME);
        g.fill(x, Math.min(y, ey), x + LINE, Math.max(y, ey), COL_FRAME);
    }

    // ═══════════════════ 上传进度条 ═══════════════════

    private static void drawUploadBar(GuiGraphicsExtractor g) {
        long collapseAt = HackClientState.collapseAtMs();
        boolean uploading = HackClientState.uploading();
        if (!uploading && collapseAt == 0L) {
            return;                                     // 没在上传 → 立刻 return
        }
        long now = System.currentTimeMillis();
        int w = g.guiWidth();
        int h = g.guiHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        int cx = w / 2;
        int y = (int) (h * 0.72D);

        float progress;
        float alpha = 1.0F;
        if (uploading) {
            progress = HackClientState.uploadProgress();
        } else {
            // 收缩消失：宽度从 lastProgress 缩到 0，同时淡出
            long elapsed = now - collapseAt;
            if (elapsed >= COLLAPSE_MS) {
                HackClientState.endCollapse();           // 动画结束 → 状态归零，之后不再画
                return;
            }
            float t = 1.0F - (float) elapsed / COLLAPSE_MS;
            progress = HackClientState.lastProgress() * t;
            alpha = t;
        }

        int barW = Math.round(UP_BAR_W * Math.max(0.0F, Math.min(1.0F, progress)));
        int x0 = cx - UP_BAR_W / 2;
        int a = Math.round(0xFF * alpha);

        // 底层光晕 + 极细红线
        g.fill(x0 - 1, y - 1, x0 + UP_BAR_W + 1, y + UP_BAR_H + 1, withAlpha(COL_UP_GLOW, a));
        if (barW > 0) {
            g.fill(x0, y, x0 + barW, y + UP_BAR_H, withAlpha(COL_UP, a));
            // 流动光效：一段亮色沿进度来回跑
            float phase = (now % 700L) / 700.0F;
            int fx = x0 + Math.round(barW * phase);
            g.fill(Math.max(x0, fx - 6), y, Math.min(x0 + barW, fx + 6), y + UP_BAR_H,
                    withAlpha(0xE0FFFFFF, a));
        } else {
            // 进度为 0 时也留一条极细的轨道，避免「什么都不显示」
            g.fill(x0, y, x0 + UP_BAR_W, y + UP_BAR_H, withAlpha(0x30FF2B2B, a));
        }

        if (uploading) {
            // 全息倒计时
            float seconds = HackClientState.uploadRemainingTicks() / 20.0F;
            String label = "UPLOADING... " + String.format(java.util.Locale.ROOT, "%.1fs", seconds);
            g.text(font, label, cx - font.width(label) / 2, y - 12, withAlpha(COL_UP_TEXT, a), false);
            // 右侧滚动二进制乱码（只 8 个字符）
            long seed = now / 90L;
            StringBuilder sb = new StringBuilder(8);
            for (int i = 0; i < 8; i++) {
                sb.append((((seed * 2654435761L + i * 40503L) >>> 3) & 1L) == 0L ? '0' : '1');
            }
            g.text(font, sb.toString(), x0 + UP_BAR_W + 6, y - 3, withAlpha(0xC0FF5555, a), false);
        }
    }

    private static int withAlpha(int argb, int alpha) {
        return ((argb >>> 24) * Math.max(0, Math.min(255, alpha)) / 255) << 24 | (argb & 0x00FFFFFF);
    }

    // ═══════════════════ 提示（NO_KIROSHI / NO_TARGET / 冷却 …） ═══════════════════

    private static void drawNote(GuiGraphicsExtractor g) {
        String note = HackClientState.lastNote();
        long at = HackClientState.noteAtMs();
        if (note.isEmpty() || at == 0L) {
            return;
        }
        long dt = System.currentTimeMillis() - at;
        if (dt > 2500L) {
            return;
        }
        int alpha = dt > 2000L ? (int) (0xFF * (2500L - dt) / 500L) : 0xFF;
        int w = g.guiWidth();
        int h = g.guiHeight();
        Font font = Minecraft.getInstance().font;
        Component text = Component.literal(explain(note));
        int tw = font.width(text);
        int x = w / 2 - tw / 2;
        int y = (int) (h * 0.60D);
        g.fill(x - 6, y - 4, x + tw + 6, y + 14, withAlpha(0x80000000, alpha));
        g.text(font, text, x, y, withAlpha(0xFFFF5555, alpha), false);
    }

    /** 把服务端 note 翻成人话（契约 §1.1 / §1.2 里冻结的那几个字符串）。 */
    private static String explain(String note) {
        return switch (note) {
            case "NO_KIROSHI" -> "未安装歧路司义眼 —— 无法扫描";
            case "NO_TARGET" -> "无锁定目标";
            case "RAM ACCESS FAILED" -> "RAM 不足 —— 破解失败";
            default -> note;
        };
    }
}
