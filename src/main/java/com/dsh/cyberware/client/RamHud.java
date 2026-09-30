package com.dsh.cyberware.client;

import com.dsh.cyberware.Cyberware;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * 脑机超频客户端表现层（t25，P0）。
 *
 * <p>四块：<b>RAM 极细条</b>（左下，底层光晕 + 顶层实线 + 全息数字）、
 * <b>超频全屏覆盖</b>（荧光绿扫描线 + 边缘二进制流 + 结束前 3 秒撕裂）、
 * <b>头顶全息数据面板</b>（世界→屏幕投影）、以及 <b>音效调度</b>（只用原版 SoundEvent + pitch）。
 *
 * <p><b>渲染回调整体 try/catch</b>：渲染线程抛异常 = 当场崩客户端，装饰性功能不该让玩家买单。
 * <p><b>客户端不做任何扣费判定</b>：这里画的全是 {@link RamClientState} 同步下来的镜像。
 */
public final class RamHud {

    // ── RAM 条布局：屏幕左侧偏下（落在斯安威斯坦/狂暴两根竖条的下方，不叠） ──
    private static final int BAR_X = 12;
    private static final int BAR_W = 110;
    /** 极细：核心实线只有 3 像素高 */
    private static final int BAR_H = 3;
    /** 底边离屏幕底部（竖条底边在 40，这里压到 22，错开） */
    private static final int BAR_BOTTOM_GAP = 22;

    /** 青蓝（正常）：核心实线 + 光晕 */
    private static final int CORE_CYAN = 0xFF7BF7FF;
    private static final int GLOW_CYAN_IN = 0x5535E0FF;
    private static final int GLOW_CYAN_MID = 0x2E35E0FF;
    private static final int GLOW_CYAN_OUT = 0x1235E0FF;
    /** 警告（RAM 不足）转红 */
    private static final int CORE_RED = 0xFFFF3B30;
    private static final int GLOW_RED_IN = 0x55FF3B30;
    private static final int GLOW_RED_MID = 0x2EFF3B30;
    private static final int GLOW_RED_OUT = 0x12FF3B30;
    /** 濒死超频：血红 */
    private static final int CORE_BLOOD = 0xFFB3001B;
    private static final int GLOW_BLOOD_IN = 0x66B3001B;
    /** 轨道（未填充部分） */
    private static final int TRACK = 0x50101014;

    /** 超频主色：荧光绿 */
    private static final int NEON_GREEN = 0xFF39FF6A;
    /** 撕裂伪影的洋红 */
    private static final int NEON_MAGENTA = 0xFFFF37C8;

    /** 头顶全息面板：只给这么远的实体画（帧率自保） */
    private static final double HOLO_RANGE = 20.0D;
    /** 头顶全息面板：单帧最多几个（帧率自保） */
    private static final int HOLO_MAX = 12;

    private static final AtomicBoolean FAIL_LOGGED = new AtomicBoolean(false);
    /** 音效调度：上一次播放各音效的毫秒 */
    private static long lastHumMs;
    private static long lastKeyMs;
    private static long lastHeartMs;
    private static long lastGlitchMs;
    private static long lastParticleMs;

    private RamHud() {
    }

    // ═══════════════════ GUI 入口 ═══════════════════

    public static void onRenderGui(RenderGuiEvent.Post event) {
        try {
            render(event.getGuiGraphics());
        } catch (Throwable t) {
            if (FAIL_LOGGED.compareAndSet(false, true)) {
                Cyberware.LOGGER.warn("[cyberware] RAM HUD 渲染异常（已忽略，不影响游戏）", t);
            }
        }
    }

    private static void render(GuiGraphicsExtractor g) {
        if (g == null || !RamClientState.fresh()) {
            return;                       // 没同步到快照就什么都不画
        }
        RamClientState.updateForFrame();
        if (!RamClientState.hasNoRam()) {
            drawRamBar(g);
        }
        drawFullScreen(g);
        renderHoloIfActive(g);
    }

    // ═══════════════════ ① RAM 极细条 + 全息数字 ═══════════════════

    private static void drawRamBar(GuiGraphicsExtractor g) {
        Font font = Minecraft.getInstance().font;
        int x = BAR_X;
        int y = g.guiHeight() - BAR_BOTTOM_GAP;
        int w = BAR_W;
        int h = BAR_H;

        boolean dying = RamClientState.dyingOverclock();
        boolean low = RamClientState.isLowRam() || RamClientState.paralysed();
        int core = dying ? CORE_BLOOD : low ? CORE_RED : CORE_CYAN;
        int glowIn = dying ? GLOW_BLOOD_IN : low ? GLOW_RED_IN : GLOW_CYAN_IN;
        int glowMid = low ? GLOW_RED_MID : GLOW_CYAN_MID;
        int glowOut = low ? GLOW_RED_OUT : GLOW_CYAN_OUT;

        // 轨道（比核心线宽一点，做出「槽」的感觉）
        g.fill(x, y - 1, x + w, y + h + 1, TRACK);

        // 底层光晕：三层由外到内收窄
        g.fill(x - 6, y - 5, x + w + 6, y + h + 5, glowOut);
        g.fill(x - 3, y - 3, x + w + 3, y + h + 3, glowMid);
        g.fill(x - 1, y - 2, x + w + 1, y + h + 2, glowIn);

        // 已填充：顶层实线（极细、实心）
        int filled = Math.round(w * RamClientState.shownFraction());
        if (filled > 0) {
            g.fill(x, y, x + filled, y + h, core);
            // 顶端亮一点，像「充能到哪」
            g.fill(Math.max(x, x + filled - 2), y, x + filled, y + h, 0xFFFFFFFF);

            // 恢复时的流动感：一段亮色沿条子跑（只有真的在回填才画）
            boolean refilling = RamClientState.current() < RamClientState.max();
            if (refilling && !dying) {
                float phase = (System.currentTimeMillis() % 900L) / 900.0F;
                int fx = x + Math.round(filled * phase);
                g.fill(Math.max(x, fx - 3), y, Math.min(x + filled, fx + 4), y + h, 0xCCFFFFFF);
            }
        }

        // 全息数字 "6 / 8"：无阴影（dropShadow=false）、半透明
        String number = fmt(RamClientState.current()) + " / " + fmt(RamClientState.max());
        int numberColor = low ? 0xE0FF6B6B : 0xD09BE9FF;
        // 恢复中让数字「跳」一下：亮度随时间脉动，读得出还在涨
        boolean refilling = RamClientState.current() < RamClientState.max();
        if (refilling) {
            float pulse = 0.5F + 0.5F * (float) Math.sin(System.currentTimeMillis() / 160.0D);
            int a = 0xB0 + Math.round(0x40 * pulse);
            numberColor = (a << 24) | (numberColor & 0x00FFFFFF);
        }
        g.text(font, number, x + w + 5, y - 4, numberColor, false);

        // 冷却中：条子上叠一层暗罩 + 剩余秒数（避免和超频进度混淆）
        int cd = RamClientState.cooldownRemainingTicks();
        if (cd > 0 && !RamClientState.overclockActive()) {
            g.fill(x, y - 1, x + w, y + h + 1, 0x66FFAA00);
            g.text(font, "CD " + ((cd + 19) / 20) + "s", x + w + 5, y + 6, 0xA0FFAA00, false);
        }
    }

    private static String fmt(double v) {
        double r = Math.round(v * 10.0D) / 10.0D;
        if (r == Math.floor(r)) {
            return String.valueOf((long) r);
        }
        return String.format(Locale.ROOT, "%.1f", r);
    }

    // ═══════════════════ ② 全屏：扫描线 / 二进制流 / 撕裂 / 濒死 / 瘫痪 ═══════════════════

    private static void drawFullScreen(GuiGraphicsExtractor g) {
        int w = g.guiWidth();
        int h = g.guiHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        long now = System.currentTimeMillis();

        // —— 警告态：整体转红闪 3 次（1.2 秒内） ——
        long warn = RamClientState.warningStartMs();
        if (warn > 0L) {
            long dt = now - warn;
            if (dt < 1200L) {
                float flash = (float) Math.abs(Math.sin(dt / 1200.0D * Math.PI * 3.0D));  // 3 次
                int a = Math.round(0x50 * flash);
                g.fill(0, 0, w, h, (a << 24) | 0x00FF2020);
                drawEdgeGlow(g, w, h, Math.round(0x60 * flash), 0xFF2020);
                // 1 帧青红 RGB 错位（只在闪的峰值给一帧）
                if (flash > 0.96F) {
                    drawRgbSplit(g, w, h, 6);
                }
            }
        }

        // —— 濒死超频：血红流动 + 边缘红光 ——
        if (RamClientState.dyingOverclock()) {
            float flow = 0.5F + 0.5F * (float) Math.sin(now / 220.0D);
            int a = Math.round(0x38 + 0x20 * flow);
            g.fill(0, 0, w, h, (a << 24) | 0x00600000);
            drawEdgeGlow(g, w, h, 0x88, 0xB3001B);
        }

        // —— 彻底瘫痪：中央闪 "RAM ACCESS FAILED" ——
        if (RamClientState.paralysed()) {
            drawParalysis(g, w, h, now);
        }

        // —— 超频激活中：荧光绿扫描线 + 边缘二进制流 ——
        if (RamClientState.overclockActive() && !RamClientState.dyingOverclock()) {
            boolean finishing = RamClientState.finishStartMs() > 0L;
            drawScanlines(g, w, h, now, finishing);
            drawBinaryEdges(g, w, h, now, finishing);
            if (finishing) {
                drawTearing(g, w, h, now);
            }
        }
    }

    /** 边缘红光/绿光：四条边各画几层渐窄的色带。 */
    private static void drawEdgeGlow(GuiGraphicsExtractor g, int w, int h, int alpha, int rgb) {
        int a1 = Math.min(0xFF, alpha);
        int a2 = a1 / 3;
        int c1 = (a1 << 24) | rgb;
        int c2 = (a2 << 24) | rgb;
        for (int i = 0; i < 3; i++) {
            int c = i == 0 ? c1 : c2;
            int t = 2 + i * 3;
            g.fill(0, 0, w, t, c);
            g.fill(0, h - t, w, h, c);
            g.fill(0, 0, t, h, c);
            g.fill(w - t, 0, w, h, c);
        }
    }

    /** 1 帧青红 RGB 错位：把画面上下两条横带各偏移一点，用青/红叠出来。 */
    private static void drawRgbSplit(GuiGraphicsExtractor g, int w, int h, int offset) {
        int bands = 3;
        for (int i = 0; i < bands; i++) {
            int y0 = (h / bands) * i + 4;
            g.fill(offset, y0, w, y0 + 2, 0x66FF0000);          // 红往右
            g.fill(0, y0 + 2, w - offset, y0 + 4, 0x6600FFFF);  // 青往左
        }
    }

    private static void drawScanlines(GuiGraphicsExtractor g, int w, int h, long now, boolean finishing) {
        // 结束前 3 秒：扫描线加速（周期变短 -> 相位变快）
        long period = finishing ? 260L : 1100L;
        float phase = (float) (now % period) / period;
        int step = 4;
        int baseA = finishing ? 0x20 : 0x12;
        for (int y = 0; y < h; y += step) {
            float d = Math.abs(((y / (float) step) % 16) / 16.0F - phase);
            int a = Math.round(baseA * (1.0F - Math.min(1.0F, d * 3.0F)));
            if (a <= 2) {
                continue;
            }
            g.fill(0, y, w, y + 1, (a << 24) | (NEON_GREEN & 0x00FFFFFF));
        }
        // 一条亮扫描带自上而下
        int bandY = (int) (phase * h);
        g.fill(0, bandY, w, Math.min(h, bandY + 3), 0x2239FF6A);
    }

    /** 屏幕边缘的二进制流：左右两列 0/1，向下滚动；结束前 3 秒加速。 */
    private static void drawBinaryEdges(GuiGraphicsExtractor g, int w, int h, long now, boolean finishing) {
        Font font = Minecraft.getInstance().font;
        int rowH = 9;
        int rows = h / rowH;
        long speed = finishing ? 40L : 120L;
        int scroll = (int) ((now / speed) % rows);
        int cols = 2;
        int color = 0x9A39FF6A;
        for (int c = 0; c < cols; c++) {
            int xL = 2 + c * 8;
            int xR = w - 10 - c * 8;
            for (int r = 0; r < rows; r++) {
                int idx = (r + scroll) % rows;
                char bit = (((idx * 2654435761L + c * 40503L) >>> 3) & 1L) == 0L ? '0' : '1';
                String s = String.valueOf(bit);
                int y = r * rowH;
                g.text(font, s, xL, y, color, false);
                g.text(font, s, xR, y, color, false);
            }
        }
    }

    /** 结束前 3 秒的 RGB 三色错位撕裂伪影。 */
    private static void drawTearing(GuiGraphicsExtractor g, int w, int h, long now) {
        int slices = 5;
        for (int i = 0; i < slices; i++) {
            long seed = now / 90L + i * 7919L;
            int y = (int) Math.floorMod(seed * 2654435761L, h);
            int len = 2 + (int) Math.floorMod(seed * 40503L, 6);
            int dx = (int) Math.floorMod(seed * 2246822519L, 14) - 7;
            // 三条错位色带：红 / 绿 / 蓝
            g.fill(Math.max(0, dx), y, Math.min(w, w + dx), y + len, 0x55FF0000);
            g.fill(0, y + len, Math.min(w, w - dx), y + len * 2, 0x5500FF00);
            g.fill(Math.max(0, -dx), y + len * 2, Math.min(w, w - dx), y + len * 3, 0x550000FF);
        }
        // 洋红边框脉冲
        int a = 0x30 + (int) Math.round(0x20 * Math.abs(Math.sin(now / 120.0D)));
        drawEdgeGlow(g, w, h, a, NEON_MAGENTA & 0x00FFFFFF);
    }

    private static void drawParalysis(GuiGraphicsExtractor g, int w, int h, long now) {
        Font font = Minecraft.getInstance().font;
        float flash = 0.5F + 0.5F * (float) Math.sin(now / 180.0D);
        int a = Math.round(0x80 + 0x7F * flash);
        int color = (a << 24) | 0x00FF2B2B;
        Component text = Component.literal("RAM ACCESS FAILED");
        int tw = font.width(text);
        int x = (w - tw) / 2;
        int y = h / 2 - 6;
        g.fill(x - 10, y - 6, x + tw + 10, y + 14, 0x66000000);
        g.text(font, text, x, y, color, false);
        drawEdgeGlow(g, w, h, Math.round(0x50 * flash), 0xFF2B2B);
    }

    // ═══════════════════ ③ 头顶全息数据面板（世界→屏幕投影） ═══════════════════

    static void drawHolo(GuiGraphicsExtractor g) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        LocalPlayer self = minecraft.player;
        Font font = minecraft.font;
        int w = g.guiWidth();
        int h = g.guiHeight();
        int drawn = 0;
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (drawn >= HOLO_MAX) {
                break;                                    // 帧率自保：单帧上限
            }
            if (!(entity instanceof LivingEntity living) || entity == self || !entity.isAlive()) {
                continue;
            }
            if (entity.distanceToSqr(self) > HOLO_RANGE * HOLO_RANGE) {
                continue;                                 // 帧率自保：只看近处
            }
            Vec3 p;
            try {
                p = minecraft.gameRenderer.projectPointToScreen(new Vec3(
                        entity.getX(), entity.getY() + entity.getBbHeight() + 0.55D, entity.getZ()));
            } catch (Throwable t) {
                return;                                   // 投影 API 不可用就整块不画
            }
            if (p == null || p.z < -1.0D || p.z > 1.0D) {
                continue;                                 // 背后 / 超出近远平面
            }
            int sx = (int) Math.round((p.x + 1.0D) * 0.5D * w);
            int sy = (int) Math.round((1.0D - p.y) * 0.5D * h);
            if (sx < -40 || sx > w + 40 || sy < -20 || sy > h + 20) {
                continue;
            }
            drawPanel(g, font, sx, sy, living);
            drawn++;
        }
    }

    private static void drawPanel(GuiGraphicsExtractor g, Font font, int sx, int sy, LivingEntity living) {
        String name = living.getType().getDescription().getString();
        float hp = Math.max(0.0F, living.getHealth());
        float maxHp = Math.max(1.0F, living.getMaxHealth());
        String hpText = fmt(hp) + " / " + fmt(maxHp);
        int nameW = font.width(name);
        int hpW = font.width(hpText);
        int boxW = Math.max(nameW, hpW) + 12;
        int boxH = 24;
        int x = sx - boxW / 2;
        int y = sy - boxH;

        g.fill(x, y, x + boxW, y + boxH, 0x88101820);
        g.fill(x, y, x + boxW, y + 1, 0xCC39FF6A);
        g.fill(x, y + boxH - 1, x + boxW, y + boxH, 0xCC39FF6A);
        g.fill(x, y, x + 1, y + boxH, 0xCC39FF6A);
        g.fill(x + boxW - 1, y, x + boxW, y + boxH, 0xCC39FF6A);
        g.text(font, name, x + 6, y + 4, 0xD0B8FFD0, false);
        g.text(font, hpText, x + 6, y + 14, 0xC0FFD0D0, false);
        // 血条
        int barW = boxW - 12;
        int barY = y + boxH - 4;
        g.fill(x + 6, barY, x + 6 + barW, barY + 2, 0x66101010);
        int fillW = Math.round(barW * Math.min(1.0F, hp / maxHp));
        g.fill(x + 6, barY, x + 6 + fillW, barY + 2, 0xCC39FF6A);
    }

    // ═══════════════════ ④ 每 tick：音效 + 粒子（帧率/声音自保：全部限频） ═══════════════════

    public static void tick() {
        try {
            tickAudioAndParticles();
        } catch (Throwable t) {
            if (FAIL_LOGGED.compareAndSet(false, true)) {
                Cyberware.LOGGER.warn("[cyberware] 超频音效/粒子异常（已忽略）", t);
            }
        }
    }

    private static void tickAudioAndParticles() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        long now = System.currentTimeMillis();

        // 激活瞬间：原版音效 pitch 提升 20%
        long activate = RamClientState.activateStartMs();
        if (activate > 0L && now - activate < 60L) {
            play(SoundEvents.BEACON_ACTIVATE, 1.2F, 1.0F);
            play(SoundEvents.UI_BUTTON_CLICK, 1.2F, 0.7F);
        }
        // RAM 不足：短促电子故障音
        long warn = RamClientState.warningStartMs();
        if (warn > 0L && now - warn < 60L && now - lastGlitchMs > 500L) {
            lastGlitchMs = now;
            play(SoundEvents.NOTE_BLOCK_BASS, 0.6F, 0.8F);
            play(SoundEvents.NOTE_BLOCK_BIT, 1.6F, 0.5F);
        }

        if (RamClientState.overclockActive() && !RamClientState.dyingOverclock()) {
            // 高频嗡鸣：每 1 秒一次
            if (now - lastHumMs > 1000L) {
                lastHumMs = now;
                play(SoundEvents.BEACON_AMBIENT, 1.2F, 0.5F);
            }
            // 键盘声叠加：每 ~250ms 一声
            if (now - lastKeyMs > 250L) {
                lastKeyMs = now;
                play(SoundEvents.NOTE_BLOCK_HAT, 1.2F, 0.35F);
            }
        }
        // 濒死：心跳音 + 玻璃碎裂粒子
        if (RamClientState.dyingOverclock()) {
            if (now - lastHeartMs > 700L) {
                lastHeartMs = now;
                play(SoundEvents.WARDEN_HEARTBEAT, 0.9F, 0.8F);
            }
            if (now - lastParticleMs > 250L) {
                lastParticleMs = now;
                LocalPlayer player = minecraft.player;
                net.minecraft.util.RandomSource rnd = net.minecraft.util.RandomSource.create();
                for (int i = 0; i < 6; i++) {
                    double dx = (rnd.nextDouble() - 0.5D) * 1.6D;
                    double dy = rnd.nextDouble() * 1.8D;
                    double dz = (rnd.nextDouble() - 0.5D) * 1.6D;
                    minecraft.level.addParticle(ParticleTypes.CRIT,
                            player.getX() + dx, player.getY() + dy, player.getZ() + dz,
                            dx * 0.4D, 0.3D, dz * 0.4D);
                }
            }
        }
    }

    private static void play(SoundEvent sound, float pitch, float volume) {
        try {
            Minecraft.getInstance().getSoundManager()
                    .playDelayed(SimpleSoundInstance.forUI(sound, pitch, volume), 0);
        } catch (Throwable ignored) {
            // 音效不可用不影响游戏
        }
    }

    /** 26.1 里部分原版音效是 {@code Holder<SoundEvent>}（注册表引用），这里统一收口。 */
    private static void play(Holder<SoundEvent> sound, float pitch, float volume) {
        play(sound.value(), pitch, volume);
    }

    /** 超频激活时才画的头顶面板（由 {@link #drawFullScreen} 之后统一调用）。 */
    public static void renderHoloIfActive(GuiGraphicsExtractor g) {
        if (RamClientState.overclockActive()) {
            drawHolo(g);
        }
    }
}
