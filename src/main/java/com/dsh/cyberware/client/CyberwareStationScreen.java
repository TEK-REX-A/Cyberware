package com.dsh.cyberware.client;

import com.dsh.cyberware.Cyberware;
import com.dsh.cyberware.core.CyberwareDefinition;
import com.dsh.cyberware.core.CyberwareDefinitions;
import com.dsh.cyberware.core.CyberwareInstallation;
import com.dsh.cyberware.core.CyberwareSlot;
import com.dsh.cyberware.data.CyberwareData;
import com.dsh.cyberware.item.CyberwareItem;
import com.dsh.cyberware.menu.CyberwareStationMenu;
import com.dsh.cyberware.menu.CyberwareStationService;
import com.dsh.cyberware.network.CyberwareActionPayload;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 义体操作台（安装界面），按设计稿 {@code 无标题-2026-10-22-0001.png} 重做。
 *
 * <h3>三栏结构</h3>
 * <ol>
 *   <li><b>左栏 = 人体部位图</b>（{@link StationBodyMap}）：十个固定部位，点部位只切换右侧信息，
 *       <b>不直接执行安装</b>；</li>
 *   <li><b>中栏 = 可选安装列表</b>：每条「名称 / 描述 / 容量数字」，下面是按实际数量生成的
 *       「已安装」格，最下面一条容量条；</li>
 *   <li><b>右栏 = 已选中义体的详情</b>：名称、英文名、加成及效果。<b>底部是故意留白的占位结构</b>，
 *       只画一个极淡的框，别往里塞东西 —— 后面的功能要占这位。</li>
 * </ol>
 *
 * <h3>交互（主人定的规矩）</h3>
 * <ul>
 *   <li><b>点击选中，再点取消</b>；hover 只做 PC 端的额外高亮，不承担任何功能
 *       （触屏没有自然悬停）；</li>
 *   <li>点空白取消选中；</li>
 *   <li><b>安装按钮靠状态控制，不靠位置</b>：未选中 / 容量不足 / 背包里没有这件义体 → 置灰且点了也不发请求；</li>
 *   <li><b>退出用 ESC</b>（MC Screen 默认行为），右上角只放一个 11px 的小 × 和一行 ESC 提示，
 *       不摆大按钮跟安装抢镜；</li>
 *   <li>「需要 X 义体容量」这句话<b>全屏只出现一次</b> —— 在容量条下面写成 {@code +X / 剩余 Y}，
 *       列表里只留一个容量数字。</li>
 * </ul>
 *
 * <h3>26.x API 说明</h3>
 * <p>绘制入口是 {@link GuiGraphicsExtractor}（{@code GuiGraphics} 已移除），鼠标回调是
 * {@link MouseButtonEvent}。本文件用到的每个方法（{@code fill / text / blit}、父类
 * {@code isHovering}、{@code font.width / plainSubstrByWidth}）都能在本工程其它 client 文件里
 * 找到同款调用，没有凭记忆写的签名。
 * <p>{@code extractBackground} 收到的是<b>屏幕绝对坐标</b>；{@code extractLabels} 收到的是
 * <b>面板相对坐标</b>（父类已 translate）。鼠标坐标永远是绝对坐标，所以 hover 一律走
 * {@code this.isHovering(相对坐标...)}，点击判定一律用 {@code leftPos/topPos + 相对坐标}。
 */
public class CyberwareStationScreen extends AbstractContainerScreen<CyberwareStationMenu> {

    // ---- 面板 ----
    private static final int PANEL_W = 360;
    private static final int PANEL_H = 226;

    // ---- 左栏：人体部位图 ----
    private static final int LEFT_X = 4;
    private static final int LEFT_W = 76;
    private static final int BODY_X = 14;
    private static final int BODY_Y = 40;

    // ---- 中栏：可选安装 / 已安装 / 容量条 ----
    private static final int MID_X = 86;
    private static final int MID_W = 170;
    private static final int LIST_TOP = 38;
    private static final int LIST_BOTTOM = 90;
    private static final int CARD_H = 24;
    private static final int CARD_GAP = 4;
    private static final int INST_TOP = 108;
    private static final int INST_CELL_W = 30;
    private static final int INST_CELL_H = 20;
    private static final int INST_GAP = 4;
    private static final int INST_COLS = 5;
    private static final int INST_ROWS = 2;
    private static final int CAP_BAR_Y = 156;
    private static final int CAP_BAR_H = 8;
    private static final int CAP_TEXT_Y = 166;

    // ---- 右栏：详情 ----
    private static final int DETAIL_X = 262;
    private static final int DETAIL_W = 94;

    // ---- 底部：快捷栏 + 安装按钮 ----
    private static final int BTN_X = 284;
    private static final int BTN_Y = 196;
    private static final int BTN_W = 72;
    private static final int BTN_H = 16;
    /** 右上角退出提示（小 ×，11px，不跟安装按钮抢位置）。 */
    private static final int CLOSE_X = PANEL_W - 18;
    private static final int CLOSE_Y = 7;
    private static final int CLOSE_S = 11;

    /**
     * 底部的玩家快捷栏（纯显示方便确认手边有什么）。
     *
     * <p><b>v0.5.4 起它不再承担「送料」职责</b>：安装直接读 {@code player.getInventory()}，
     * 服务端 {@code CyberwareStationService#install(containerId, defId)} 自己去找并扣 1 件。
     * 但菜单把整个背包挪到 {@code (-3000,-3000)} 不显示 —— 快捷栏仍然是玩家唯一能看见、
     * 能拖放的格子，所以照样捞到底部。{@code Slot.x/y} 在 26.x 是 final，所以这里是换成
     * 同 container、同 index、只是坐标不同的新 Slot（对服务端完全透明）。
     */
    private static final int HOTBAR_X = 70;
    private static final int HOTBAR_Y = 204;
    private static final int HOTBAR_SLOTS = 9;

    private static final long STATUS_MILLIS = 4000L;

    /** 左下角版本号（真正取值见 {@link #modVersion()}）。 */
    private static final String MOD_VERSION = modVersion();

    /**
     * 从 <b>fabric.mod.json</b> 拿本 mod 的 version（构建时 processResources 把里面的版本占位符
     * 填成 0.5.3-Beta），拼成 {@code v0.5.3-Beta} 给左下角显示。
     *
     * <p>{@link FabricLoader} 在本工程 {@code CyberwareConfig} 里已经在用（取 config 目录），
     * 所以不是凭记忆编的 API；{@code getModContainer} 返回 {@code Optional<ModContainer>}，
     * {@code ModMetadata.getVersion()} 返回 {@code Version}，{@code getFriendlyString()} 给原始字符串。
     * 整段包 try/catch：版本号是装饰，绝不能因为读不到它让界面初始化失败。
     */
    private static String modVersion() {
        try {
            return "v" + FabricLoader.getInstance().getModContainer(Cyberware.MODID)
                    .map(container -> container.getMetadata().getVersion().getFriendlyString())
                    .orElse("?");
        } catch (Throwable ignored) {
            return "v?";
        }
    }

    // ------------------------------------------------------------------
    // 状态
    // ------------------------------------------------------------------

    /** 当前选中的部位（决定中栏列表与右栏）。 */
    private CyberwareSlot selectedPart = CyberwareSlot.OPERATING_SYSTEM;
    /** 当前选中的义体；null = 没选中。 */
    private CyberwareDefinition selectedDef;
    /** 选中的那件是不是「身上已装的」（是的话底部按钮变成卸载）。 */
    private boolean selectedInstalled;

    /** 中栏列表滚动偏移（像素）。 */
    private int listScroll;
    /** 列表按下起点，用于区分「点选」和「拖动滚动」（触屏没有滚轮）。 */
    private boolean draggingList;
    /** 上一次鼠标 y（算拖动增量用，会跟着更新）。 */
    private double pressY;
    /** 按下那一刻的 y（<b>不更新</b>，松开时用它判断这是点选还是拖动）。 */
    private double dragStartY;
    private CyberwareDefinition pressCard;
    /** 每帧重建的命中区（屏幕绝对坐标）。 */
    private final List<HitCard> listCards = new ArrayList<>();
    private final List<HitCell> installedCells = new ArrayList<>();

    /** 当前部位的可选列表缓存（切部位才重建，避免每帧遍历 123 条定义）。 */
    private CyberwareSlot cachedPart;
    private List<CyberwareDefinition> cachedList = List.of();

    /** 标题栏短提示（本地预检结果 / 已发请求）。 */
    private String status = "";
    private boolean statusError;
    private long statusAt;

    private record HitCard(int x, int y, int w, int h, CyberwareDefinition def) {
    }

    private record HitCell(int x, int y, int w, int h, String defId) {
    }

    public CyberwareStationScreen(CyberwareStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, PANEL_W, PANEL_H);
        this.inventoryLabelY = -1000; // 不显示背包标签
        this.revealHotbar();
    }

    /** 把玩家快捷栏那 9 格挪到面板底部（只动客户端这份菜单，见字段注释）。 */
    private void revealHotbar() {
        int first = this.menu.slots.size() - HOTBAR_SLOTS;
        if (first < 0) {
            return;
        }
        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            Slot original = this.menu.slots.get(first + i);
            Slot moved = new Slot(original.container, original.getContainerSlot(),
                    HOTBAR_X + i * 18, HOTBAR_Y);
            moved.index = original.index;
            this.menu.slots.set(first + i, moved);
        }
    }

    // ==================================================================
    // 背景（屏幕绝对坐标）
    // ==================================================================

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = this.leftPos;
        int y = this.topPos;

        StationPaint.panel(g, x, y, PANEL_W, PANEL_H);

        // 顶部 1px 亮线（立体感，代替粗边框）
        g.fill(x + 1, y + 1, x + PANEL_W - 1, y + 2, StationPaint.withAlpha(StationPaint.ACCENT_CYAN, 0.30F));
        // 标题栏底 + 1px 分隔（规范 3.1：用层次代替线条，所以只有这一条）
        g.fill(x + 1, y + 22, x + PANEL_W - 1, y + 23, StationPaint.withAlpha(StationPaint.LINE, 0.8F));

        // 三栏底板（卡片色，圆角 2）
        StationPaint.roundRect(g, x + LEFT_X, y + 24, LEFT_W, 152, StationPaint.RADIUS_CARD, StationPaint.BG_CARD);
        StationPaint.roundRect(g, x + MID_X, y + 24, MID_W, 152, StationPaint.RADIUS_CARD, StationPaint.BG_CARD);
        StationPaint.roundRect(g, x + DETAIL_X, y + 24, DETAIL_W, 152, StationPaint.RADIUS_CARD,
                StationPaint.BG_CARD);

        // 只剩快捷栏：常驻底框，否则界面下半部空得像坏掉（送料槽已删，见类注释）
        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            int index = hotbarSlotIndex(i);
            if (index >= 0) {
                drawSlotFrame(g, this.menu.slots.get(index), x, y);
            }
        }
    }

    /** 槽位常驻底框（原版只在悬停时才画高亮）。 */
    private static void drawSlotFrame(GuiGraphicsExtractor g, Slot slot, int x, int y) {
        int sx = x + slot.x;
        int sy = y + slot.y;
        StationPaint.roundRect(g, sx - 1, sy - 1, 18, 18, StationPaint.RADIUS_CARD, StationPaint.BG_DEEP);
        StationPaint.outline(g, sx - 1, sy - 1, 18, 18, StationPaint.withAlpha(StationPaint.LINE, 0.8F));
    }

    // ==================================================================
    // 内容（面板相对坐标）
    // ==================================================================

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        drawHeader(g, mouseX, mouseY);
        drawBodyColumn(g, mouseX, mouseY);
        drawListColumn(g, mouseX, mouseY);
        drawDetailColumn(g, mouseX, mouseY);
        drawBottom(g, mouseX, mouseY);
    }

    // ---- 标题栏 -------------------------------------------------------

    private void drawHeader(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        StationPaint.groupBar(g, 4, 4, 12);
        String title = "义体操作台";
        g.text(this.font, Component.literal(title), 10, 6, StationPaint.TEXT_PRIMARY);
        int subX = 10 + this.font.width(title) + StationPaint.GAP_S;
        g.text(this.font, Component.literal("CYBERWARE"), subX, 8, StationPaint.TEXT_SECONDARY);

        // 右上：ESC 提示 + 小 ×（不摆大按钮跟安装抢镜）
        g.text(this.font, Component.literal("ESC"), CLOSE_X - 24, 9, StationPaint.TEXT_DISABLED);
        boolean closeHover = this.isHovering(CLOSE_X, CLOSE_Y, CLOSE_S, CLOSE_S, mouseX, mouseY);
        if (closeHover) {
            StationPaint.roundRect(g, CLOSE_X, CLOSE_Y, CLOSE_S, CLOSE_S, StationPaint.RADIUS_CARD,
                    StationPaint.BG_HOVER);
        }
        int c = closeHover ? StationPaint.TEXT_PRIMARY : StationPaint.TEXT_SECONDARY;
        // 斜十字：两段阶梯 fill，够用且不需要新 API
        for (int i = 2; i < CLOSE_S - 2; i++) {
            g.fill(CLOSE_X + i, CLOSE_Y + i, CLOSE_X + i + 1, CLOSE_Y + i + 1, c);
            g.fill(CLOSE_X + CLOSE_S - 1 - i, CLOSE_Y + i, CLOSE_X + CLOSE_S - i, CLOSE_Y + i + 1, c);
        }

        // 状态提示：右对齐到 ESC 左边
        if (statusActive()) {
            int right = CLOSE_X - 30;
            String text = this.font.plainSubstrByWidth(this.status, Math.max(40, right - 120));
            g.text(this.font, Component.literal(text), right - this.font.width(text), 7,
                    this.statusError ? StationPaint.WARN_AMBER : StationPaint.ACCENT_CYAN);
        }
    }

    // ---- 左栏：人体部位图 ---------------------------------------------

    private void drawBodyColumn(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        StationPaint.groupBar(g, LEFT_X, 27, 10);
        g.text(this.font, Component.literal("人体部位"), LEFT_X + 6, 28, StationPaint.TEXT_SECONDARY);

        // hover 判定：鼠标是绝对坐标，人体图原点也是绝对坐标 → 直接比
        StationBodyMap.Part hovered = StationBodyMap.partAt(
                this.leftPos + BODY_X, this.topPos + BODY_Y, mouseX, mouseY);
        StationBodyMap.draw(g, BODY_X, BODY_Y, this.selectedPart,
                hovered == null ? null : hovered.slot(), installedSlots());

        // 底部一行：当前部位名（状态不只靠颜色，用文字再说一遍）
        String label = hovered != null ? hovered.label() : labelOf(this.selectedPart);
        g.text(this.font, Component.literal(label), LEFT_X + 6, 172 - 9,
                hovered != null ? StationPaint.ACCENT_CYAN : StationPaint.TEXT_SECONDARY);
    }

    /** 部位显示名：直接用枚举自己的名字（HAND 槽已有独立「手掌」，不再需要映射）。 */
    private static String labelOf(CyberwareSlot slot) {
        return slot == null ? "" : slot.displayName();
    }

    private Set<CyberwareSlot> installedSlots() {
        Set<CyberwareSlot> set = new HashSet<>();
        Player player = getPlayer();
        if (player == null) {
            return set;
        }
        for (String id : CyberwareInstallation.of(player).orderedIds()) {
            CyberwareDefinition def = CyberwareDefinitions.byId(id);
            if (def != null) {
                set.add(def.slot());
            }
        }
        return set;
    }

    // ---- 中栏：可选安装 / 已安装 / 容量条 ------------------------------

    private void drawListColumn(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        StationPaint.groupBar(g, MID_X, 27, 10);
        g.text(this.font, Component.literal("可选安装"), MID_X + 6, 28, StationPaint.TEXT_SECONDARY);

        List<CyberwareDefinition> list = listFor(this.selectedPart);
        int viewH = LIST_BOTTOM - LIST_TOP;
        int maxScroll = maxListScroll(list.size());
        this.listScroll = Math.max(0, Math.min(this.listScroll, maxScroll));

        this.listCards.clear();
        if (list.isEmpty()) {
            // 没选部位 ≠ 该部位没货：两种空态得说不同的话
            String empty = this.selectedPart == null ? "点左侧人体选择部位" : "该部位暂无可安装义体";
            g.text(this.font, Component.literal(empty), MID_X + 8, LIST_TOP + 8,
                    StationPaint.TEXT_DISABLED);
        }
        for (int i = 0; i < list.size(); i++) {
            int cy = LIST_TOP + i * (CARD_H + CARD_GAP) - this.listScroll;
            if (cy + CARD_H <= LIST_TOP || cy >= LIST_BOTTOM) {
                continue; // 超出可视区就不画（GuiGraphicsExtractor 没有公开裁剪入口）
            }
            CyberwareDefinition def = list.get(i);
            boolean sel = def == this.selectedDef && !this.selectedInstalled;
            boolean hovered = this.isHovering(MID_X, cy, MID_W, CARD_H, mouseX, mouseY);
            StationPaint.card(g, MID_X, cy, MID_W, CARD_H, sel, hovered);
            StationPaint.iconSlot(g, MID_X + 6, cy + 7, 10);
            drawIcon(g, itemIcon(def), MID_X + 6, cy + 7, 10);

            String name = this.font.plainSubstrByWidth(def.displayName(), MID_W - 60);
            g.text(this.font, Component.literal(name), MID_X + 20, cy + 4,
                    sel ? StationPaint.ACCENT_CYAN : StationPaint.TEXT_PRIMARY);
            String desc = def.description() == null ? "" : def.description();
            desc = this.font.plainSubstrByWidth(desc, MID_W - 52);
            g.text(this.font, Component.literal(desc), MID_X + 20, cy + 13, StationPaint.TEXT_SECONDARY);

            // 容量只给数字 ——「需要 X 义体容量」整句只在容量条那儿出现一次
            CyberwareDefinition.Variant base = def.baseVariant();
            if (base != null) {
                String cap = String.valueOf(base.capacity());
                g.text(this.font, Component.literal(cap),
                        MID_X + MID_W - 8 - this.font.width(cap), cy + 13, StationPaint.ACCENT_CYAN);
            }
            this.listCards.add(new HitCard(MID_X + this.leftPos, cy + this.topPos, MID_W, CARD_H, def));
        }

        // 滚动条（2px，只在内容超出时出现）
        if (maxScroll > 0) {
            int barX = MID_X + MID_W - 2;
            int barH = Math.max(10, viewH * viewH / (viewH + maxScroll));
            int barY = LIST_TOP + (viewH - barH) * this.listScroll / maxScroll;
            g.fill(barX, LIST_TOP, barX + 2, LIST_BOTTOM, StationPaint.withAlpha(StationPaint.BG_DEEP, 0.85F));
            g.fill(barX, barY, barX + 2, barY + barH, StationPaint.withAlpha(StationPaint.ACCENT_CYAN, 0.75F));
        }

        // 分隔（规范说能用留白就别用线条，所以只画一条极淡的）
        g.fill(MID_X, 94, MID_X + MID_W, 95, StationPaint.withAlpha(StationPaint.LINE, 0.55F));

        drawInstalled(g, mouseX, mouseY);
        drawCapacity(g);
    }

    private int maxListScroll(int rows) {
        int content = rows * (CARD_H + CARD_GAP) - CARD_GAP;
        return Math.max(0, content - (LIST_BOTTOM - LIST_TOP));
    }

    /** 已安装：按实际数量生成格子，绝不画固定 5 格 / 红色禁用块。 */
    private void drawInstalled(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        StationPaint.groupBar(g, MID_X, 98, 10);
        g.text(this.font, Component.literal("已安装"), MID_X + 6, 99, StationPaint.TEXT_SECONDARY);

        List<String> ids = installedIds();
        this.installedCells.clear();
        if (ids.isEmpty()) {
            g.text(this.font, Component.literal("还没有安装任何义体"), MID_X + 8, INST_TOP + 4,
                    StationPaint.TEXT_DISABLED);
            return;
        }
        int shown = Math.min(ids.size(), INST_COLS * INST_ROWS);
        for (int i = 0; i < shown; i++) {
            String id = ids.get(i);
            CyberwareDefinition def = CyberwareDefinitions.byId(id);
            int cx = MID_X + 2 + (i % INST_COLS) * (INST_CELL_W + INST_GAP);
            int cy = INST_TOP + (i / INST_COLS) * (INST_CELL_H + INST_GAP);
            boolean sel = this.selectedInstalled && id.equals(this.selectedDef == null ? null : this.selectedDef.id());
            boolean hovered = this.isHovering(cx, cy, INST_CELL_W, INST_CELL_H, mouseX, mouseY);
            StationPaint.roundRect(g, cx, cy, INST_CELL_W, INST_CELL_H, StationPaint.RADIUS_CARD,
                    hovered ? StationPaint.BG_HOVER : StationPaint.BG_DEEP);
            StationPaint.outline(g, cx, cy, INST_CELL_W, INST_CELL_H,
                    sel ? StationPaint.ACCENT_CYAN
                            : StationPaint.withAlpha(StationPaint.LINE, hovered ? 0.9F : 0.6F));
            if (def != null) {
                drawIcon(g, itemIcon(def), cx + 2, cy + 2, 8);
                String shortName = this.font.plainSubstrByWidth(def.displayName(), 18);
                g.text(this.font, Component.literal(shortName), cx + 11, cy + 2,
                        sel ? StationPaint.ACCENT_CYAN : StationPaint.TEXT_PRIMARY);
                String cap = String.valueOf(capacityOf(id));
                g.text(this.font, Component.literal(cap), cx + 11, cy + 11, StationPaint.ACCENT_CYAN);
            } else {
                g.text(this.font, Component.literal("?"), cx + 11, cy + 6, StationPaint.TEXT_DISABLED);
            }
            this.installedCells.add(new HitCell(cx + this.leftPos, cy + this.topPos,
                    INST_CELL_W, INST_CELL_H, id));
        }
        if (ids.size() > shown) {
            String more = "+" + (ids.size() - shown);
            g.text(this.font, Component.literal(more), MID_X + MID_W - 6 - this.font.width(more),
                    99, StationPaint.TEXT_DISABLED);
        }
    }

    /** 容量条 + 全屏唯一一句「+X / 剩余 Y」。 */
    private void drawCapacity(GuiGraphicsExtractor g) {
        Player player = getPlayer();
        int limit = CyberwareInstallation.capacityLimit();
        int used = player == null ? 0 : CyberwareInstallation.usedCapacity(player);
        boolean unlimited = limit == Integer.MAX_VALUE;
        StationPaint.capacityBar(g, MID_X, CAP_BAR_Y, MID_W, CAP_BAR_H, used, unlimited ? 0 : limit);

        String left = "已用 " + used + (unlimited ? "" : "/" + limit);
        g.text(this.font, Component.literal(left), MID_X, CAP_TEXT_Y, StationPaint.TEXT_SECONDARY);

        int need = selectedCost();
        String right = "+" + (need < 0 ? "—" : String.valueOf(need)) + " / 剩余 "
                + (unlimited ? "∞" : String.valueOf(Math.max(0, limit - used)));
        // 剩余不足时给警告色（不只靠颜色：文案本身也写了数字）
        boolean tight = !unlimited && need >= 0 && used + need > limit;
        g.text(this.font, Component.literal(right), MID_X + MID_W - this.font.width(right), CAP_TEXT_Y,
                tight ? StationPaint.WARN_AMBER : StationPaint.ACCENT_CYAN);
    }

    /** 当前选中件的容量占用；没选中返回 -1。 */
    private int selectedCost() {
        if (this.selectedDef == null) {
            return -1;
        }
        if (this.selectedInstalled) {
            return capacityOf(this.selectedDef.id());
        }
        CyberwareDefinition.Variant base = this.selectedDef.baseVariant();
        return base == null ? -1 : base.capacity();
    }

    private List<String> installedIds() {
        Player player = getPlayer();
        return player == null ? List.of() : CyberwareInstallation.of(player).orderedIds();
    }

    private int capacityOf(String defId) {
        CyberwareDefinition def = CyberwareDefinitions.byId(defId);
        Player player = getPlayer();
        if (def == null || player == null) {
            return 0;
        }
        CyberwareData data = CyberwareInstallation.dataOf(player, defId);
        CyberwareDefinition.Variant v = data == null ? null : def.variantFor(data.rarity());
        if (v == null) {
            v = def.baseVariant();
        }
        return v == null ? 0 : v.capacity();
    }

    private List<CyberwareDefinition> listFor(CyberwareSlot slot) {
        if (slot == this.cachedPart) {
            return this.cachedList;
        }
        List<CyberwareDefinition> list = new ArrayList<>();
        if (slot != null) {
            for (CyberwareDefinition def : CyberwareDefinitions.all().values()) {
                if (def.slot() == slot) {
                    list.add(def);
                }
            }
            list.sort(Comparator.comparing(CyberwareDefinition::displayName));
        }
        this.cachedPart = slot;
        this.cachedList = List.copyOf(list);
        return this.cachedList;
    }

    // ---- 右栏：详情 ---------------------------------------------------

    private void drawDetailColumn(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        StationPaint.groupBar(g, DETAIL_X, 27, 10);
        g.text(this.font, Component.literal("已选中的义体"), DETAIL_X + 6, 28, StationPaint.TEXT_SECONDARY);

        int dx = DETAIL_X + 4;
        int dw = DETAIL_W - 8;
        if (this.selectedDef == null) {
            drawPartInfo(g, dx, dw);
            return;
        }

        CyberwareDefinition def = this.selectedDef;
        CyberwareDefinition.Variant base = def.baseVariant();
        int accent = base == null ? StationPaint.ACCENT_CYAN : base.rarity().color();

        // 名称 / 英文名
        String name = this.font.plainSubstrByWidth(def.displayName(), dw);
        g.text(this.font, Component.literal(name), dx, 42, StationPaint.TEXT_PRIMARY);
        String en = englishName(def);
        g.text(this.font, Component.literal(this.font.plainSubstrByWidth(en, dw)), dx, 54,
                StationPaint.TEXT_SECONDARY);

        // 稀有度标签（12% alpha 底，规范 3.6）
        if (base != null) {
            String tag = base.rarity().displayName();
            int tw = this.font.width(tag) + 8;
            StationPaint.tag(g, dx, 66, tw, 11, accent);
            g.text(this.font, Component.literal(tag), dx + 4, 67, accent);
        }

        // 加成及效果
        StationPaint.groupBar(g, dx, 83, 9);
        g.text(this.font, Component.literal("加成及效果"), dx + 6, 84, StationPaint.TEXT_SECONDARY);

        int rowY = 96;
        int maxRows = 4;
        int drawn = 0;
        if (base != null) {
            for (var entry : base.stats().entrySet()) {
                if (drawn >= maxRows) {
                    break;
                }
                String label = this.font.plainSubstrByWidth(statLabel(entry.getKey()), dw - 32);
                g.text(this.font, Component.literal(label), dx, rowY, StationPaint.TEXT_SECONDARY);
                String value = formatStat(entry.getKey(), entry.getValue());
                g.text(this.font, Component.literal(value), dx + dw - this.font.width(value), rowY,
                        StationPaint.ACCENT_CYAN);
                rowY += 11;
                drawn++;
            }
        }
        if (drawn == 0) {
            g.text(this.font, Component.literal("（该型号无数值）"), dx, rowY, StationPaint.TEXT_DISABLED);
            rowY += 11;
        }
        if (this.selectedInstalled) {
            g.text(this.font, Component.literal("· 已装在身上"), dx, rowY, StationPaint.OK_GREEN);
            rowY += 11;
        }

        // 占位结构：设计稿这里是故意留白的，给后续功能占位，别塞满
        int phTop = Math.max(rowY + StationPaint.GAP_XS, 142);
        if (phTop < 170) {
            StationPaint.placeholder(g, dx, phTop, dw, 170 - phTop);
            String hint = "预留";
            g.text(this.font, Component.literal(hint), dx + (dw - this.font.width(hint)) / 2,
                    phTop + (170 - phTop) / 2 - 3, StationPaint.TEXT_DISABLED);
        }
    }

    /** 没选中义体时，右栏显示当前部位的信息（点部位只切这里，不执行安装）。 */
    private void drawPartInfo(GuiGraphicsExtractor g, int dx, int dw) {
        CyberwareSlot part = this.selectedPart;
        if (part == null) {
            g.text(this.font, Component.literal("点左侧人体选择部位"), dx, 42, StationPaint.TEXT_DISABLED);
            return;
        }
        String label = this.font.plainSubstrByWidth(labelOf(part), dw);
        g.text(this.font, Component.literal(label), dx, 42, StationPaint.TEXT_PRIMARY);
        g.text(this.font, Component.literal("安装区"), dx, 54, StationPaint.TEXT_SECONDARY);

        int usedInSlot = 0;
        for (String id : installedIds()) {
            CyberwareDefinition def = CyberwareDefinitions.byId(id);
            if (def != null && def.slot() == part) {
                usedInSlot++;
            }
        }
        StationPaint.groupBar(g, dx, 71, 9);
        g.text(this.font, Component.literal("本部位"), dx + 6, 72, StationPaint.TEXT_SECONDARY);
        g.text(this.font, Component.literal("已装 " + usedInSlot + "/" + Math.max(1, part.maxCount())),
                dx, 84, StationPaint.TEXT_PRIMARY);
        g.text(this.font, Component.literal("可安装 " + listFor(part).size() + " 件"), dx, 95,
                StationPaint.TEXT_SECONDARY);

        StationPaint.placeholder(g, dx, 142, dw, 28);
        String hint = "预留";
        g.text(this.font, Component.literal(hint), dx + (dw - this.font.width(hint)) / 2, 153,
                StationPaint.TEXT_DISABLED);
    }

    /** 英文名：定义表没有独立英文字段，用注册 id 推导（待核实是否有更正规的来源）。 */
    private static String englishName(CyberwareDefinition def) {
        return def.id().toUpperCase(Locale.ROOT).replace('_', ' ');
    }

    // ---- 底部：快捷栏标签 + 安装按钮 ----------------------------------

    private void drawBottom(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        // 左下角版本号（设计稿「Mod版本号」）：左栏卡片最后一行留给部位名，所以放卡片正下方
        g.text(this.font, Component.literal(MOD_VERSION), LEFT_X + 2, 181, StationPaint.TEXT_DISABLED);

        // v0.5.4：送料槽已删（安装直接从背包取），底部只剩快捷栏与安装按钮
        g.text(this.font, Component.literal("快捷栏"), 38, HOTBAR_Y + 4, StationPaint.TEXT_DISABLED);

        String reason = installBlockReason();
        boolean ready = reason == null;
        boolean hovered = ready && this.isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, mouseX, mouseY);
        StationPaint.button(g, BTN_X, BTN_Y, BTN_W, BTN_H, ready, hovered);
        String label = this.selectedInstalled ? "卸载" : "安装";
        int fg = !ready ? StationPaint.TEXT_DISABLED
                : (hovered ? StationPaint.ACCENT_CYAN : StationPaint.TEXT_PRIMARY);
        g.text(this.font, Component.literal(label), BTN_X + (BTN_W - this.font.width(label)) / 2,
                BTN_Y + 4, fg);

        // 按钮下方一行原因（不可用时才写，且不重复「需要 X 义体容量」那句）
        if (!ready) {
            String why = this.font.plainSubstrByWidth(reason, BTN_W + 16);
            g.text(this.font, Component.literal(why), BTN_X + (BTN_W - this.font.width(why)) / 2,
                    BTN_Y + BTN_H + 2, StationPaint.TEXT_DISABLED);
        }
    }

    /**
     * 底部按钮为什么不能点；{@code null} = 可以点。
     *
     * <p>预检用的是服务端同一份 {@link CyberwareStationService#validateInstall}（纯函数，不改状态），
     * 所以「置灰」和「服务端真正拒绝」的理由不会打架。
     */
    private String installBlockReason() {
        if (this.selectedDef == null) {
            return "先选中一件义体";
        }
        if (this.selectedInstalled) {
            return null; // 卸载不预判，交给服务端
        }
        Player player = getPlayer();
        if (player == null) {
            return "玩家数据未就绪";
        }
        // v0.5.4：安装材料从背包取（没有送料槽了）。定位规则与服务端**同一个方法**，
        // 否则会出现「界面说能装、服务端却扣了另一格」。
        int at = CyberwareStationService.findInventorySlot(player, this.selectedDef.id());
        if (at < 0) {
            return "背包里没有这件义体";
        }
        ItemStack stack = player.getInventory().getItem(at);
        return CyberwareStationService.validateInstall(this.selectedDef, CyberwareItem.dataOf(stack),
                CyberwareInstallation.of(player));
    }

    // ==================================================================
    // 安装 / 卸载（客户端只发请求，服务端裁决）
    // ==================================================================

    private void requestInstall() {
        CyberwareDefinition def = this.selectedDef;
        if (def == null) {
            setStatus("先选中一件义体", true);
            return;
        }
        Player player = getPlayer();
        if (player == null) {
            return;
        }
        int at = CyberwareStationService.findInventorySlot(player, def.id());
        if (at < 0) {
            setStatus("背包里没有「" + def.displayName() + "」", true);
            return;
        }
        CyberwareData data = CyberwareItem.dataOf(player.getInventory().getItem(at));
        String problem = CyberwareStationService.validateInstall(def, data, CyberwareInstallation.of(player));
        if (problem != null) {
            setStatus(problem, true);
            return;
        }
        // 只报型号，扣哪一格由服务端 findInventorySlot 裁决（两端同一条规则）
        ClientPlayNetworking.send(CyberwareActionPayload.install(this.menu.containerId, def.id()));
        setStatus("已发送安装请求：" + def.displayName(), false);
    }

    private void requestUninstall() {
        CyberwareDefinition def = this.selectedDef;
        if (def == null) {
            setStatus("先选中一件义体", true);
            return;
        }
        if (!this.selectedInstalled) {
            setStatus("身上没有装「" + def.displayName() + "」", true);
            return;
        }
        ClientPlayNetworking.send(CyberwareActionPayload.uninstall(this.menu.containerId, def.id()));
        setStatus("已发送卸载请求：" + def.displayName(), false);
    }

    private void setStatus(String message, boolean error) {
        this.status = message == null ? "" : message;
        this.statusError = error;
        this.statusAt = Util.getMillis();
    }

    private boolean statusActive() {
        return !this.status.isEmpty() && Util.getMillis() - this.statusAt < STATUS_MILLIS;
    }

    // ==================================================================
    // 交互
    // ==================================================================

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }
        double mx = event.x();
        double my = event.y();

        // ① 右上小 ×：退出（ESC 是默认行为，这里只是给触屏一个手感）
        if (this.isHovering(CLOSE_X, CLOSE_Y, CLOSE_S, CLOSE_S, mx, my)) {
            this.onClose();
            return true;
        }

        // ② 安装 / 卸载：靠状态判定，不靠位置；置灰时点了只出提示、不发请求
        if (this.isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, mx, my)) {
            String reason = installBlockReason();
            if (reason == null) {
                if (this.selectedInstalled) {
                    requestUninstall();
                } else {
                    requestInstall();
                }
            } else {
                setStatus(reason, true);
            }
            return true;
        }

        // ③ 快捷栏：交给原版处理抓放物品（安装不再需要把物品拖进任何槽里）
        if (hotbarSlotIndexAt(mx, my) >= 0) {
            return super.mouseClicked(event, doubleClick);
        }

        // ④ 已安装格：点一下切到该件详情（再点取消）
        for (HitCell cell : this.installedCells) {
            if (isInside(mx, my, cell.x(), cell.y(), cell.w(), cell.h())) {
                CyberwareDefinition def = CyberwareDefinitions.byId(cell.defId());
                if (def == this.selectedDef && this.selectedInstalled) {
                    this.selectedDef = null;
                    this.selectedInstalled = false;
                } else {
                    this.selectedDef = def;
                    this.selectedInstalled = true;
                }
                return true;
            }
        }

        // ⑤ 列表：记下按下点，松开时若没怎么动才算「点选」（触屏拖动不误选）
        if (isInside(mx, my, this.leftPos + MID_X, this.topPos + LIST_TOP, MID_W,
                LIST_BOTTOM - LIST_TOP)) {
            this.draggingList = true;
            this.pressY = my;
            this.dragStartY = my;
            this.pressCard = null;
            for (HitCard card : this.listCards) {
                if (isInside(mx, my, card.x(), card.y(), card.w(), card.h())) {
                    this.pressCard = card.def();
                    break;
                }
            }
            return true;
        }

        // ⑥ 人体部位：只切换右侧信息，绝不在这里执行安装
        StationBodyMap.Part part = StationBodyMap.partAt(
                this.leftPos + BODY_X, this.topPos + BODY_Y, mx, my);
        if (part != null) {
            selectPart(part.slot());
            return true;
        }

        // ⑦ 点空白：义体与部位一起取消（设计稿「点空白取消」）
        if (this.selectedDef != null || this.selectedInstalled || this.selectedPart != null) {
            clearPart();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.draggingList) {
            this.listScroll -= (int) Math.round(event.y() - this.pressY);
            this.pressY = event.y();
            int maxScroll = maxListScroll(listFor(this.selectedPart).size());
            this.listScroll = Math.max(0, Math.min(this.listScroll, maxScroll));
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.draggingList) {
            boolean tapped = Math.abs(event.y() - this.dragStartY) < 4.0D;
            this.draggingList = false;
            CyberwareDefinition card = this.pressCard;
            this.pressCard = null;
            if (tapped && card != null) {
                // 点击选中、再点取消
                if (card == this.selectedDef && !this.selectedInstalled) {
                    this.selectedDef = null;
                } else {
                    this.selectedDef = card;
                    this.selectedInstalled = false;
                    this.selectedPart = card.slot();
                }
            }
            clampListScroll();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isInside(mouseX, mouseY, this.leftPos + MID_X, this.topPos + LIST_TOP, MID_W,
                LIST_BOTTOM - LIST_TOP)) {
            this.listScroll -= (int) (scrollY * (CARD_H + CARD_GAP));
            clampListScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void clampListScroll() {
        int maxScroll = maxListScroll(listFor(this.selectedPart).size());
        this.listScroll = Math.max(0, Math.min(this.listScroll, maxScroll));
    }

    /**
     * 切换部位：中栏列表跟着换，右栏回到部位信息（不自动选中任何义体）。
     *
     * <p><b>再点同一部位 = 取消选中</b>（设计稿「点击选中 → 再点取消」）。
     * 取消后 {@code selectedPart == null}，全链路都吃 null：{@link #listFor} 返回空表、
     * 右栏 {@link #drawPartInfo} 已写「点左侧人体选择部位」、{@link #labelOf} 返回空串、
     * 人体图 draw 收到 null 就不画高亮。
     */
    private void selectPart(CyberwareSlot slot) {
        if (slot == this.selectedPart) {
            clearPart();
            return;
        }
        this.selectedPart = slot;
        this.selectedDef = null;
        this.selectedInstalled = false;
        this.listScroll = 0;
    }

    /** 取消部位选中：中栏回空态、右栏回到「点左侧人体选择部位」。 */
    private void clearPart() {
        this.selectedPart = null;
        this.selectedDef = null;
        this.selectedInstalled = false;
        this.listScroll = 0;
    }

    /** 快捷栏第 {@code i} 格在 {@code menu.slots} 里的下标；菜单结构异常时返回 -1。 */
    private int hotbarSlotIndex(int i) {
        int index = this.menu.slots.size() - HOTBAR_SLOTS + i;
        return index >= 0 && index < this.menu.slots.size() ? index : -1;
    }

    /** 鼠标落在第几个快捷栏格上（返回 menu.slots 下标，-1 = 没有）。{@code getHoveredSlot} 在 26.x 是私有的，自己算。 */
    private int hotbarSlotIndexAt(double mouseX, double mouseY) {
        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            int index = hotbarSlotIndex(i);
            if (index < 0) {
                continue;
            }
            Slot slot = this.menu.slots.get(index);
            int sx = this.leftPos + slot.x;
            int sy = this.topPos + slot.y;
            if (isInside(mouseX, mouseY, sx, sy, 16, 16)) {
                return index;
            }
        }
        return -1;
    }

    private Player getPlayer() {
        return this.minecraft == null ? null : this.minecraft.player;
    }

    private static boolean isInside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ==================================================================
    // 图标 / 文案
    // ==================================================================

    private static Identifier itemIcon(CyberwareDefinition def) {
        return Identifier.fromNamespaceAndPath(Cyberware.MODID, "textures/item/" + def.id() + ".png");
    }

    private static void drawIcon(GuiGraphicsExtractor g, Identifier icon, int x, int y, int size) {
        g.blit(RenderPipelines.GUI_TEXTURED, icon, x, y, 0.0F, 0.0F, size, size, 32, 32);
    }

    private static String statLabel(String key) {
        return switch (key) {
            case "time_slow" -> "时间减缓";
            case "duration" -> "持续时间";
            case "cooldown" -> "冷却";
            case "crit_chance" -> "暴击率";
            case "crit_damage" -> "暴击伤害";
            case "all_damage" -> "全伤害";
            case "headshot_damage" -> "爆头伤害";
            case "melee_damage" -> "近战伤害";
            case "armor" -> "护甲";
            case "max_health" -> "最大生命";
            case "recoil" -> "后坐力/摇摆";
            case "ram" -> "RAM";
            case "buffer" -> "缓冲";
            case "slots" -> "栏位";
            case "ram_regen" -> "RAM恢复";
            case "hack_damage" -> "快速破解伤害";
            case "hack_cooldown" -> "快速破解冷却";
            case "combat_hack_duration" -> "战斗破解持续";
            case "upload_time" -> "上传时间";
            case "ultimate_cost" -> "终极破解占用";
            case "stealth_cost" -> "隐蔽破解占用";
            case "enemy_hack_time" -> "敌方破解时间";
            case "hack_distance" -> "破解距离";
            case "spread_distance" -> "散布距离";
            case "tier" -> "位阶";
            case "kill_heal" -> "击败恢复";
            default -> key;
        };
    }

    private static String formatStat(String key, double value) {
        return switch (key) {
            case "time_slow" -> Math.round(value * 100) + "%";
            case "duration", "cooldown" -> trim(value) + "s";
            case "ram", "buffer", "slots", "tier", "ultimate_cost", "stealth_cost" -> trim(value);
            case "ram_regen" -> "+" + trim(value) + "/min";
            default -> (value >= 0 ? "+" : "") + trim(value) + "%";
        };
    }

    private static String trim(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.valueOf(value);
    }
}
