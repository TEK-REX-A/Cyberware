package com.dsh.cyberware.client;

import com.dsh.cyberware.network.HackPayload;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * <b>R 键 · 破解轮盘</b>（t29，契约 §1.3）：只在「有锁定目标」时打开。
 *
 * <p>扇区算法与 {@link CyberwareRadialScreen} <b>同一套公式</b>（契约要求不要推倒重来）：
 * 第 0 项在正上方、顺时针均分；半径只当死区门槛，之后完全由角度决定。
 * 这里<strong>没有</strong>改 {@code CyberwareRadialScreen} 的任何一行 ——
 * 「无目标时回落到义体轮盘，行为一字不改」是硬要求。
 *
 * <p><b>不裁决</b>：RAM 是否够只用于「置灰 + 不可发」的展示；真正的扣费与效果全在服务端。
 */
public class HackRadialScreen extends Screen {

    /** 与 CyberwareRadialScreen 相同：死区半径（防手抖） */
    private static final double DEAD_ZONE = 24.0D;
    /** 与 CyberwareRadialScreen 相同：图标环半径 */
    private static final double RING_RADIUS = 78.0D;
    private static final int PLATE = 30;

    private static final int COL_PLATE = 0x88101018;
    private static final int COL_PLATE_SEL = 0xC0C8102E;
    private static final int COL_PLATE_OFF = 0x60101010;
    private static final int COL_EDGE_SEL = 0xFFFF9AA0;
    private static final int COL_TEXT = 0xFFE6E6E6;
    private static final int COL_TEXT_SEL = 0xFFFFFFFF;
    private static final int COL_TEXT_OFF = 0xFF6A6A6A;
    private static final int COL_HINT = 0xFF9AA0A6;
    private static final int COL_RAM_OK = 0xFF7BF7FF;
    private static final int COL_RAM_BAD = 0xFFFF5555;
    private static final int COL_CENTER_BG = 0xB0000000;
    /** 保命日志去重（t34）：渲染回调抛异常只记一次，绝不刷屏 */
    private static final java.util.concurrent.atomic.AtomicBoolean RENDER_FAIL_LOGGED =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    private final List<HackClientState.HackEntry> entries;
    private final int targetEntityId;
    private int selected = -1;
    private boolean finished;

    protected HackRadialScreen(List<HackClientState.HackEntry> entries, int targetEntityId) {
        super(Component.literal("快速破解"));
        this.entries = List.copyOf(entries);
        this.targetEntityId = targetEntityId;
    }

    /**
     * 有锁定目标才开轮盘；没有目标就什么都不做（调用方会回落到义体轮盘）。
     *
     * <p>没有已锁定的实体 id 或列表为空时不打开 —— 避免出现「对空气发破解」。
     */
    public static void openIfLocked(Minecraft minecraft) {
        if (minecraft == null || minecraft.screen != null) {
            return;
        }
        if (!HackClientState.hasLock()) {
            return;
        }
        List<HackClientState.HackEntry> hacks = HackClientState.hacks();
        if (hacks.isEmpty()) {
            return;
        }
        minecraft.setScreen(new HackRadialScreen(hacks, HackClientState.lockEntityId()));
    }

    // ═══════════════════ 绘制（公式同 CyberwareRadialScreen） ═══════════════════

    /**
     * 渲染入口。
     *
     * <p><b>保命壳（t34）</b>：渲染回调抛异常 = 当场崩客户端，这里整段 try/catch，
     * 异常只记一次日志（不刷屏）。逻辑体在 {@link #renderRadial}，一字未改。
     */
    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        try {
            renderRadial(g, mouseX, mouseY, partialTick);
        } catch (Throwable t) {
            if (RENDER_FAIL_LOGGED.compareAndSet(false, true)) {
                com.dsh.cyberware.Cyberware.LOGGER.warn(
                        "[cyberware] 破解轮盘渲染异常（已忽略，不影响游戏）", t);
            }
        }
    }

    /** {@link #extractRenderState} 的逻辑体 —— 与 t29 一致，只是外包了一层保命壳。 */
    private void renderRadial(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        updateSelection(mouseX, mouseY);
        int n = this.entries.size();
        if (n == 0) {
            return;
        }
        double cx = this.width / 2.0D;
        double cy = this.height / 2.0D;
        double step = (Math.PI * 2.0D) / n;

        for (int i = 0; i < n; i++) {
            HackClientState.HackEntry entry = this.entries.get(i);
            double a = angleOf(i, step);
            int x = (int) Math.round(cx + Math.cos(a) * RING_RADIUS);
            int y = (int) Math.round(cy + Math.sin(a) * RING_RADIUS);
            boolean sel = i == selected;
            boolean ok = HackClientState.affordable(entry);

            int plate = !ok ? COL_PLATE_OFF : sel ? COL_PLATE_SEL : COL_PLATE;
            g.fill(x - PLATE, y - 14, x + PLATE, y + 14, plate);
            if (sel) {
                g.fill(x - PLATE, y - 14, x + PLATE, y - 13, COL_EDGE_SEL);
                g.fill(x - PLATE, y + 13, x + PLATE, y + 14, COL_EDGE_SEL);
                g.fill(x - PLATE, y - 14, x - PLATE + 1, y + 14, COL_EDGE_SEL);
                g.fill(x + PLATE - 1, y - 14, x + PLATE, y + 14, COL_EDGE_SEL);
            }
            g.centeredText(this.font, Component.literal(entry.name()), x, y - 9,
                    ok ? (sel ? COL_TEXT_SEL : COL_TEXT) : COL_TEXT_OFF);
            String cost = entry.ramCost() + " RAM";
            g.centeredText(this.font, Component.literal(cost), x, y + 2,
                    ok ? COL_RAM_OK : COL_RAM_BAD);
        }

        // 中心
        int cxi = (int) Math.round(cx);
        int cyi = (int) Math.round(cy);
        g.fill(cxi - 78, cyi - 16, cxi + 78, cyi + 16, COL_CENTER_BG);
        if (selected >= 0) {
            HackClientState.HackEntry entry = this.entries.get(selected);
            boolean ok = HackClientState.affordable(entry);
            g.centeredText(this.font, Component.literal(entry.name()), cxi, cyi - 11, COL_TEXT_SEL);
            if (ok) {
                g.centeredText(this.font,
                        Component.literal("松开 " + CyberwareKeys.RADIAL.getTranslatedKeyMessage().getString()
                                + " 上传 · " + entry.ramCost() + " RAM"), cxi, cyi + 1, COL_HINT);
            } else {
                g.centeredText(this.font, Component.literal("RAM 不足 —— 置灰不可发"), cxi, cyi + 1, COL_RAM_BAD);
            }
        } else {
            g.centeredText(this.font, Component.literal("指到破解上选择"), cxi, cyi - 11, COL_HINT);
            g.centeredText(this.font, Component.literal("松开取消"), cxi, cyi + 1, COL_HINT);
        }
    }

    private static double angleOf(int index, double step) {
        return -Math.PI / 2.0D + index * step;
    }

    private void updateSelection(double mouseX, double mouseY) {
        int n = this.entries.size();
        if (n == 0) {
            selected = -1;
            return;
        }
        double dx = mouseX - this.width / 2.0D;
        double dy = mouseY - this.height / 2.0D;
        if (Math.hypot(dx, dy) < DEAD_ZONE) {
            selected = -1;
            return;
        }
        double step = (Math.PI * 2.0D) / n;
        long k = Math.round((Math.atan2(dy, dx) + Math.PI / 2.0D) / step);
        selected = (int) (((k % n) + n) % n);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        updateSelection(mouseX, mouseY);
        super.mouseMoved(mouseX, mouseY);
    }

    // ═══════════════════ 输入（与义体轮盘同一套收尾规则） ═══════════════════

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (isRadialKey(event.key())) {
            return true;                        // 吃掉按下，免得残留 click 一关就重开
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (!isRadialKey(event.key())) {
            return super.keyReleased(event);
        }
        commit();
        return true;
    }

    @Override
    public void onClose() {
        this.finished = true;                   // ESC = 只关不施放
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    /** 收起并（仅当选中且 RAM 够时）发一次 CAST；其余情况一个包都不发。 */
    private void commit() {
        if (this.finished) {
            return;
        }
        this.finished = true;
        String hackId = null;
        if (selected >= 0) {
            HackClientState.HackEntry entry = this.entries.get(selected);
            if (HackClientState.affordable(entry)) {
                hackId = entry.id();            // RAM 不足 → 置灰不可发
            }
        }
        net.minecraft.client.KeyMapping.set(CyberwareKeys.RADIAL.getKey(), false);
        this.minecraft.setScreen(null);
        if (hackId != null && targetEntityId > 0) {
            ClientPacketDistributor.sendToServer(HackPayload.cast(targetEntityId, hackId));
        }
    }

    private static boolean isRadialKey(int key) {
        int bound = CyberwareKeys.RADIAL.getKey().getValue();
        return key >= 0 && key == bound;
    }
}
