package com.dsh.cyberware.client;

import com.dsh.cyberware.Cyberware;
import com.dsh.cyberware.core.CyberwareDefinition;
import com.dsh.cyberware.core.CyberwareDefinitions;
import com.dsh.cyberware.core.CyberwareInstallation;
import com.dsh.cyberware.data.CyberwareData;
import com.dsh.cyberware.network.ActivatePayload;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * <b>R 键义体轮盘</b> —— 按住 R 弹出环形菜单，鼠标指向选择，松开施放。
 *
 * <p>这是一个<b>独立实现</b>：只借鉴「按住键弹环形菜单 / 鼠标指向 / 松开确认」这种交互形式
 * （交互形式属于思想层面的功能，不受版权保护）。<b>没有参考、阅读或反编译任何第三方模组的
 * 代码或资源</b>，也没有引入任何外部素材文件 —— 图标全部来自本模组自己的
 * {@code assets/cyberware/textures/item/<id>.png}（32×32）。
 *
 * <h2>生命周期（26.1 的 Screen 是「压层」的）</h2>
 * <ul>
 *   <li><b>打开</b>：由 {@link CyberwareClient} 在 {@code onClientTick} 里调
 *       {@link #openOrHint(Minecraft)} → {@code Minecraft.setScreen(this)}。
 *       选 {@code setScreen} 而不是 {@code pushGuiLayer}：前者会
 *       {@code mouseHandler.releaseMouse()}，鼠标才会被放出来给玩家指向（{@code pushGuiLayer}
 *       不会，光标仍被锁住）。</li>
 *   <li><b>关闭 + 施放</b>：见 {@link #commit()}。松手路径是 {@link #keyReleased(KeyEvent)}。</li>
 *   <li>绘制入口是 {@link #extractRenderState(GuiGraphicsExtractor, int, int, float)}
 *       —— 26.1 里 {@code Screen.render(...)}/{@code GuiGraphics} 已不存在，
 *       换成 {@code extractRenderState(GuiGraphicsExtractor, ...)}（同 {@code CyberwareStationScreen}）。</li>
 * </ul>
 *
 * <h2>为什么不会「打开就秒关」</h2>
 * {@link #tick()} 里有一条兜底：物理 R 键已松开就收尾（防「按住 R 时窗口失焦 → 收不到
 * {@code keyReleased} → 轮盘卡死」）。但它要求先<b>观测到过一次按下</b>（{@code sawKeyDown}），
 * 否则首帧若 GLFW 状态抖动就会把刚打开的轮盘立刻关掉。
 *
 * <p><b>未真机验证</b>：手感、图标位置、松手施放都需要真机确认（见 RADIAL-MENU.md §6）。
 */
public class CyberwareRadialScreen extends Screen {

    /** 死区半径（GUI 像素）：鼠标离屏幕中心不到这么远就不选任何一项 —— 防手抖误选。 */
    private static final double DEAD_ZONE = 24.0D;
    /** 图标中心到屏幕中心的距离。 */
    private static final double RING_RADIUS = 78.0D;
    /** 图标绘制边长（源贴图 32×32，这里缩小一点免得糊在一起）。 */
    private static final int ICON_SIZE = 28;
    /** 每项底板的半边长度。 */
    private static final int PLATE = 17;

    private static final int COL_PLATE = 0x88000000;
    private static final int COL_PLATE_SEL = 0xC000A8C8;
    private static final int COL_EDGE_SEL = 0xFF7BF7FF;
    private static final int COL_SPOKE = 0x66FFFFFF;
    private static final int COL_CENTER_BG = 0xB0000000;
    private static final int COL_TEXT = 0xFFE6E6E6;
    private static final int COL_TEXT_SEL = 0xFFFFFFFF;
    private static final int COL_HINT = 0xFF9AA0A6;

    private final List<Entry> entries;
    /** 当前选中项下标；-1 = 没指向任何一项（松开即取消）。 */
    private int selected = -1;
    /** 已经收尾过（防重复发包含）；ESC 关闭也走这里。 */
    private boolean finished;
    /** 兜底用：是否至少观测到过一次物理 R 按下。 */
    private boolean sawKeyDown;

    protected CyberwareRadialScreen(List<Entry> entries) {
        // 标题只用于旁白；界面文字全部自己画（不依赖 lang 键，避免缺翻译时显示生 key）
        super(Component.literal("义体轮盘"));
        this.entries = List.copyOf(entries);
    }

    // ═══════════════════ 打开入口 ═══════════════════

    /**
     * 打开轮盘；一件可用的都没有时<b>不开空盘</b>，只给一条动作栏提示。
     *
     * <p>{@code minecraft.screen != null} 时直接忽略：轮盘已经开着（或玩家正开别的界面），
     * 不叠第二个。
     */
    public static void openOrHint(Minecraft minecraft) {
        if (minecraft == null || minecraft.screen != null) {
            return;
        }
        List<Entry> entries = collectActive(minecraft.player);
        if (entries.isEmpty()) {
            if (minecraft.gui != null) {
                minecraft.gui.setOverlayMessage(
                        Component.literal("§7[义体] 没有可用的主动义体（先装上斯安威斯坦或狂暴）"), false);
            }
            return;
        }
        minecraft.setScreen(new CyberwareRadialScreen(entries));
    }

    /**
     * 收集「**已安装** 且 **有主动效果**」的义体，顺序就是 {@code orderedIds()}（字典序，稳定）。
     *
     * <p>判定口径<b>逐字对齐服务端</b> {@code CyberwareAbilities.activate(Player, CyberwareDefinition,
     * CyberwareData)} —— 见 {@link #hasActiveEffect}。客户端<b>不另创一套规则</b>，
     * 免得「轮盘里能选、服务端却不给效果」。
     */
    public static List<Entry> collectActive(LocalPlayer player) {
        List<Entry> out = new ArrayList<>();
        if (player == null) {
            return out;
        }
        CyberwareInstallation installed = CyberwareInstallation.of(player);
        for (String id : installed.orderedIds()) {
            CyberwareDefinition def = CyberwareDefinitions.byId(id);
            if (def == null) {
                // 已安装表里留着、定义表里已经没有的型号（旧存档 / 被删定义）→ 不进轮盘
                continue;
            }
            CyberwareData data = installed.dataOf(id);
            if (data == null) {
                // 服务端 activateInstalled 也是「查不到就失败」，这里保持一致
                continue;
            }
            if (!hasActiveEffect(def, data)) {
                continue;
            }
            out.add(new Entry(id, def.displayName(), itemIcon(def)));
        }
        return out;
    }

    /**
     * 「这件义体有没有主动效果」—— <b>与服务端 {@code CyberwareAbilities.activate(...)} 同一个口径</b>。
     *
     * <p>服务端那个方法里只有两个会真正给出效果的分支，本方法就是这两行：
     * <pre>
     *   double ratio = variant.stat(CyberwareDefinition.Stats.TIME_SLOW, 0.0D);
     *   if (ratio &gt; 0.0D) { ... }                 // CyberwareAbilities.java:37-38
     *   if (def.id().startsWith("berserk_")) { ... } // CyberwareAbilities.java:50
     * </pre>
     * 稀有度回退规则（{@code variantFor(rarity)} 为 null 时用 {@code baseVariant()}）
     * 也照抄服务端，否则会出现「服务端按高稀有度判有、客户端按别档判无」的错位。
     */
    private static boolean hasActiveEffect(CyberwareDefinition def, CyberwareData data) {
        CyberwareDefinition.Variant variant = def.variantFor(data.rarity());
        if (variant == null) {
            variant = def.baseVariant();
        }
        if (variant == null) {
            return false;
        }
        if (variant.stat(CyberwareDefinition.Stats.TIME_SLOW, 0.0D) > 0.0D) {
            return true;
        }
        return def.id().startsWith("berserk_");
    }

    /** 图标：和 {@code CyberwareStationScreen.itemIcon} 同一套路径规则（本模组自己的贴图）。 */
    private static Identifier itemIcon(CyberwareDefinition def) {
        return Identifier.fromNamespaceAndPath(Cyberware.MODID, "textures/item/" + def.id() + ".png");
    }

    // ═══════════════════ 绘制 ═══════════════════

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        updateSelection(mouseX, mouseY);

        double cx = this.width / 2.0D;
        double cy = this.height / 2.0D;
        int n = this.entries.size();
        if (n == 0) {
            return;
        }
        double step = (Math.PI * 2.0D) / n;

        // 选中项的辐条：先从中心连到图标（底板后画，正好盖住辐条末端）
        if (selected >= 0) {
            double a = angleOf(selected, step);
            int tx = (int) Math.round(cx + Math.cos(a) * RING_RADIUS);
            int ty = (int) Math.round(cy + Math.sin(a) * RING_RADIUS);
            drawSpoke(g, (int) cx, (int) cy, tx, ty);
        }

        for (int i = 0; i < n; i++) {
            Entry entry = this.entries.get(i);
            double a = angleOf(i, step);
            int x = (int) Math.round(cx + Math.cos(a) * RING_RADIUS);
            int y = (int) Math.round(cy + Math.sin(a) * RING_RADIUS);
            boolean isSelected = i == selected;

            g.fill(x - PLATE, y - PLATE, x + PLATE, y + PLATE, isSelected ? COL_PLATE_SEL : COL_PLATE);
            if (isSelected) {
                // 四边描一圈亮色，强化「当前扇区」
                g.fill(x - PLATE, y - PLATE, x + PLATE, y - PLATE + 1, COL_EDGE_SEL);
                g.fill(x - PLATE, y + PLATE - 1, x + PLATE, y + PLATE, COL_EDGE_SEL);
                g.fill(x - PLATE, y - PLATE, x - PLATE + 1, y + PLATE, COL_EDGE_SEL);
                g.fill(x + PLATE - 1, y - PLATE, x + PLATE, y + PLATE, COL_EDGE_SEL);
            }
            g.blit(RenderPipelines.GUI_TEXTURED, entry.icon(),
                    x - ICON_SIZE / 2, y - ICON_SIZE / 2, 0.0F, 0.0F,
                    ICON_SIZE, ICON_SIZE, 32, 32);
            g.centeredText(this.font, Component.literal(entry.name()),
                    x, y + PLATE + 3, isSelected ? COL_TEXT_SEL : COL_TEXT);
        }

        // 中心：当前选中项的名字（或「松开取消」）
        int cxi = (int) Math.round(cx);
        int cyi = (int) Math.round(cy);
        g.fill(cxi - 70, cyi - 16, cxi + 70, cyi + 16, COL_CENTER_BG);
        if (selected >= 0) {
            g.centeredText(this.font, Component.literal(this.entries.get(selected).name()),
                    cxi, cyi - 11, COL_TEXT_SEL);
            g.centeredText(this.font, Component.literal("松开 " + keyName() + " 施放"),
                    cxi, cyi + 1, COL_HINT);
        } else {
            g.centeredText(this.font, Component.literal("指到图标上选择"), cxi, cyi - 11, COL_HINT);
            g.centeredText(this.font, Component.literal("松开 " + keyName() + " 取消"),
                    cxi, cyi + 1, COL_HINT);
        }
    }

    /**
     * 第 i 项的圆心角。<b>从正上方开始、顺时针</b>：
     * {@code angle(0) = -π/2}（上）、{@code angle(1) = -π/2 + step}（右）……
     */
    private static double angleOf(int index, double step) {
        return -Math.PI / 2.0D + index * step;
    }

    /** 从中心到图标的实心射线（{@code fill} 没有画线原语，用一串小方块拼）。 */
    private static void drawSpoke(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1) {
        int steps = Math.max(2, (int) Math.round(Math.hypot(x1 - x0, y1 - y0)));
        for (int s = 0; s <= steps; s++) {
            double t = s / (double) steps;
            int px = (int) Math.round(x0 + (x1 - x0) * t);
            int py = (int) Math.round(y0 + (y1 - y0) * t);
            g.fill(px - 1, py - 1, px + 1, py + 1, COL_SPOKE);
        }
    }

    // ═══════════════════ 选中判定 ═══════════════════

    /**
     * 扇区算法（<b>只用角度，半径只做死区门槛</b>）：
     * <ol>
     *   <li>取鼠标相对屏幕中心的向量 {@code (dx, dy)}，长度 {@code dist}；</li>
     *   <li>{@code dist < DEAD_ZONE} → 不选任何项（{@code selected = -1}），中心显示「松开取消」；</li>
     *   <li>{@code a = atan2(dy, dx)} ∈ (-π, π]；</li>
     *   <li>{@code k = round((a + π/2) / step)}，{@code index = ((k % n) + n) % n}。</li>
     * </ol>
     * 第 4 步就是「离哪一项的圆心角最近就选哪一项」——{@code round} 天然把每个
     * {@code ±step/2} 的角度区间分给对应的项；{@code +π/2} 把坐标系挪到「正上方为 0」，
     * 与 {@link #angleOf} 对齐；取模两次是为了让负数角度（上半屏）落到正确的下标。
     */
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
        // 渲染那一路也会更新，这里让「刚移动就按松开」也不会有半帧延迟
        updateSelection(mouseX, mouseY);
        super.mouseMoved(mouseX, mouseY);
    }

    // ═══════════════════ 按键与收尾 ═══════════════════

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (isRadialKey(event.key())) {
            // 吃掉本次按下：否则 KeyboardHandler 会再塞一个 KeyMapping click，
            // 轮盘一关就立刻被那个残留 click 重新打开。
            return true;
        }
        // ESC 等仍走原版：Screen.keyPressed → onClose() → 本类 onClose() 只关闭、不施放
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
    public void tick() {
        // 兜底：按住 R 时窗口失焦/被系统吞掉 keyReleased 时，轮盘不会卡死。
        // 必须先见过一次「按下」，否则首帧 GLFW 状态抖动会把刚开的轮盘秒关。
        boolean down;
        try {
            var window = this.minecraft.getWindow();
            down = window != null && window.handle() != 0L
                    && InputConstants.isKeyDown(window, CyberwareKeys.RADIAL.getKey().getValue());
        } catch (Throwable t) {
            down = true; // 查询失败就当作还按着，交给 keyReleased 处理
        }
        if (down) {
            sawKeyDown = true;
        } else if (sawKeyDown) {
            commit();
        }
    }

    @Override
    public void onClose() {
        // ESC（以及任何「不是松手」的关闭路径）→ 只关，不施放
        this.finished = true;
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        // 单人游戏里按住 R 不该暂停世界
        return false;
    }

    @Override
    public boolean isInGameUi() {
        // 走原版「游戏内界面」那条背景：一层透明渐变压暗世界，而不是全景图/模糊
        return true;
    }

    /**
     * 收尾：关闭轮盘，并且<b>只有选中了某一项才发包</b>（没选中 = 取消，一个包都不发）。
     *
     * <p>发的是 {@link ActivatePayload#of(String)}（t13 的协议）：{@code defId} 只是
     * 「我想激活哪一个」的<b>意图声明</b>，服务端会自己查已安装表确认玩家真装了才给效果。
     */
    private void commit() {
        if (this.finished) {
            return;
        }
        this.finished = true;
        String defId = this.selected >= 0 ? this.entries.get(this.selected).id() : null;

        // 我们返回了 true，KeyboardHandler 就不会替我们清这个键的按下状态 —— 自己清掉，
        // 免得 RADIAL 卡在 isDown。静态 set 比 setDown 更直接（KeyMapping.set(Key, false)）。
        KeyMapping.set(CyberwareKeys.RADIAL.getKey(), false);

        this.minecraft.setScreen(null); // 恢复鼠标抓取 + 触发 removed()
        if (defId != null) {
            ClientPacketDistributor.sendToServer(ActivatePayload.of(defId));
        }
    }

    private static boolean isRadialKey(int key) {
        int bound = CyberwareKeys.RADIAL.getKey().getValue();
        return key >= 0 && key == bound;
    }

    private static String keyName() {
        try {
            return CyberwareKeys.RADIAL.getTranslatedKeyMessage().getString();
        } catch (Throwable t) {
            return "R";
        }
    }

    /** 轮盘里的一项：型号 id + 显示名 + 图标。 */
    public record Entry(String id, String name, Identifier icon) {
    }
}
