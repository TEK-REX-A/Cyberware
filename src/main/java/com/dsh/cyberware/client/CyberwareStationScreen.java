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
import java.util.List;
import java.util.Locale;
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
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * 义体操作台界面。
 *
 * <p>左侧分类树（可展开、可滚动），右侧型号详情。型号行用**自己的物品图标**，
 * 稀有度直接体现在名称颜色上；分类行用 10 个槽位的分类图标。
 *
 * <p>26.x 绘制入口是 {@link GuiGraphicsExtractor}（{@code GuiGraphics} 已移除），
 * 鼠标回调签名是 {@link MouseButtonEvent}。
 *
 * <h3>安装 / 卸载交互（0.3.12 补上）</h3>
 * <p>详情面板底部有「安装 / 卸载」两个按钮，它们只做两件事：<b>预检 + 发请求</b>。
 * 请求走既有通道 {@link CyberwareActionPayload}（{@code ClientPacketDistributor.sendToServer}），
 * 服务端 {@link CyberwareStationService} 才是唯一有权改数据的一方 —— 客户端永远不自己写
 * 玩家数据，失败原因也由服务端回执兜底（本地预检用的是**服务端同一份** {@code validateInstall}）。
 */
public class CyberwareStationScreen extends AbstractContainerScreen<CyberwareStationMenu> {

    private static final int PANEL_W = 320;
    private static final int PANEL_H = 226;

    // ---- 配色：现代深色主题（中性灰阶 + 单一柔和蓝强调色，去掉霓虹） ----
    private static final int BG = 0xF00E1014;
    private static final int CARD = 0xFF171A20;
    private static final int CARD_DEEP = 0xFF12151A;
    private static final int BORDER = 0xFF2A2F38;
    private static final int ACCENT = 0xFF6E9BFF;
    private static final int TEXT = 0xFFE8EBF0;
    private static final int DIM = 0xFF98A0AC;
    private static final int HOVER_BG = 0x14FFFFFF;
    private static final int SEL_BG = 0xFF243044;
    private static final int WARN = 0xFFFF8A80;
    // ---- 布局 ----
    private static final int ROW_H = 14;
    private static final int TREE_X = 4;
    private static final int TREE_W = 150;
    private static final int TREE_TOP = 20;
    private static final int TREE_BOTTOM = PANEL_H - 52;
    private static final int DETAIL_X = 160;
    /** 底部义体槽条的位置（与 CyberwareStationMenu 的槽位坐标一致）。 */
    private static final int SLOT_STRIP_TOP = PANEL_H - 46;

    // ---- 详情面板底部的操作按钮 ----
    private static final int BTN_Y = TREE_BOTTOM - 16;
    private static final int BTN_W = 74;
    private static final int BTN_H = 14;
    private static final int BTN_INSTALL_X = DETAIL_X;
    private static final int BTN_UNINSTALL_X = DETAIL_X + 80;
    /** 状态提示在标题栏停留的时长（毫秒）。 */
    private static final long STATUS_MILLIS = 4000L;

    /**
     * 底部第二行：玩家快捷栏（9 格）。
     *
     * <p>{@link CyberwareStationMenu} 把玩家背包整体放在 {@code (-3000, -3000)}（界面里不画），
     * 但那样玩家**根本没法把义体放进操作台槽位** —— 操作台槽是唯一能装东西的地方，
     * 见 {@code CyberwareStationService#install}。槽位坐标纯客户端用途（服务端只认槽位下标、
     * 不认坐标），所以这里把最后 9 个槽（快捷栏）挪到面板底部，玩家就能用原版点击
     * 把快捷栏里的义体放进义体槽，再点「安装」。
     */
    private static final int HOTBAR_X = 70;
    private static final int HOTBAR_Y = 204;
    private static final int HOTBAR_SLOTS = 9;

    /** 当前展开的分类（同时只展开一个）。 */
    private CyberwareSlot expandedSlot = CyberwareSlot.OPERATING_SYSTEM;
    /**
     * 右侧正在展示的型号。
     *
     * <p>0.3.12：旧占位定义 {@code SANDEVISTAN_ZETATECH} 已随官方 123 条定义表重写删除，
     * 默认展示它的后继型号 C4（captain 授权的范围外修正）。
     */
    private CyberwareDefinition selectedDef = CyberwareDefinitions.SANDEVISTAN_C4;
    /** 分类树命中区，每帧重建，供点击检测。 */
    private final List<HitRow> treeRows = new ArrayList<>();

    /** 分类树滚动偏移（像素）。 */
    private int scroll = 0;
    /** 是否正在拖动列表（触屏用） */
    private boolean draggingTree;
    private double lastDragY;
    /** 每帧算出的内容总高度，用于夹紧滚动。 */
    private int contentHeight = 0;

    /** 标题栏的短提示（本地预检结果 / 已发请求），过 {@link #STATUS_MILLIS} 后自动隐去。 */
    private String status = "";
    private boolean statusError;
    private long statusAt;

    private record HitRow(int x, int y, int w, int h, CyberwareSlot slot, CyberwareDefinition def) {
    }

    public CyberwareStationScreen(CyberwareStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, PANEL_W, PANEL_H);
        this.inventoryLabelY = -1000; // 不显示背包标签
        this.revealHotbar();
    }

    /**
     * 把玩家快捷栏那 9 格移到面板底部（只动客户端这份菜单）。
     *
     * <p>26.x 的 {@code Slot.x / Slot.y} 是 {@code final}，改不了坐标，所以这里是**换一个
     * 同 container、同 container 内下标、只是坐标不同**的新 {@link Slot} 顶替它：
     * <ul>
     *   <li>槽位在 {@code menu.slots} 里的**位置不变**（{@code index} 手动照抄），
     *       所以网络包里的槽位下标与两端完全一致；</li>
     *   <li>{@code container} 与 {@code getContainerSlot()} 不变 → 服务端与本地的读写
     *       还是同一格（服务端根本不知道坐标，它只认下标）。</li>
     * </ul>
     * 目的只有一个：让玩家能在界面里把快捷栏的义体点进义体槽 —— 否则义体槽永远空着，
     * 「安装」按钮永远点不动。
     */
    private void revealHotbar() {
        int first = this.menu.slots.size() - HOTBAR_SLOTS;
        if (first < CyberwareStationMenu.IMPLANT_SLOT_COUNT) {
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

    // ------------------------------------------------------------------
    // 背景
    // ------------------------------------------------------------------

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = this.leftPos;
        int y = this.topPos;

        // 面板底 + 1px 柔和边框
        g.fill(x, y, x + PANEL_W, y + PANEL_H, BG);
        g.fill(x, y, x + PANEL_W, y + 1, BORDER);
        g.fill(x, y + PANEL_H - 1, x + PANEL_W, y + PANEL_H, BORDER);
        g.fill(x, y, x + 1, y + PANEL_H, BORDER);
        g.fill(x + PANEL_W - 1, y, x + PANEL_W, y + PANEL_H, BORDER);
        // 顶部 2px 强调条（现代 UI 的常见做法，代替发光边框）
        g.fill(x + 1, y + 1, x + PANEL_W - 1, y + 3, ACCENT);
        // 标题栏卡片 + 分隔线
        g.fill(x + 1, y + 3, x + PANEL_W - 1, y + 18, CARD);
        g.fill(x + 1, y + 18, x + PANEL_W - 1, y + 19, BORDER);
        // 两栏卡片
        g.fill(x + TREE_X, y + TREE_TOP - 3, x + TREE_X + TREE_W, y + TREE_BOTTOM, CARD_DEEP);
        g.fill(x + DETAIL_X - 8, y + TREE_TOP - 3, x + PANEL_W - 6, y + TREE_BOTTOM, CARD_DEEP);
        // 底部义体槽条
        g.fill(x + TREE_X, y + SLOT_STRIP_TOP - 5, x + PANEL_W - 6, y + SLOT_STRIP_TOP - 4, BORDER);
        // 十个体位槽画上底图（原版只在悬停时才画，这里做成常驻，界面才不空）
        int slotCount = Math.min(CyberwareStationMenu.IMPLANT_SLOT_COUNT, this.menu.slots.size());
        for (int i = 0; i < slotCount; i++) {
            drawSlotFrame(g, this.menu.slots.get(i), x, y);
        }
        // 底部第二行：玩家快捷栏（坐标在 revealHotbar() 里挪进来的）
        g.fill(x + TREE_X, y + HOTBAR_Y - 2, x + PANEL_W - 6, y + HOTBAR_Y - 1, BORDER);
        int firstHotbar = this.menu.slots.size() - HOTBAR_SLOTS;
        for (int i = 0; i < HOTBAR_SLOTS && firstHotbar + i >= 0; i++) {
            drawSlotFrame(g, this.menu.slots.get(firstHotbar + i), x, y);
        }
    }

    /** 给一个菜单槽画常驻底框（原版只在悬停时画高亮，这里补上静态边框）。 */
    private static void drawSlotFrame(GuiGraphicsExtractor g, Slot slot, int x, int y) {
        int sx = x + slot.x;
        int sy = y + slot.y;
        g.fill(sx - 1, sy - 1, sx + 17, sy + 17, CARD_DEEP);
        g.fill(sx - 1, sy - 1, sx + 17, sy, BORDER);
        g.fill(sx - 1, sy + 16, sx + 17, sy + 17, BORDER);
        g.fill(sx - 1, sy - 1, sx, sy + 17, BORDER);
        g.fill(sx + 16, sy - 1, sx + 17, sy + 17, BORDER);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        // ⚠ 这里已经是「相对面板」坐标系：AbstractContainerScreen 在调用本方法前
        //   做过 pose().translate(leftPos, topPos)。再手动加一次偏移，整个界面就会
        //   右下方向错位 (leftPos, topPos) —— 之前的「错位 BUG」就是这个。
        //
        //   ⚠⚠ 但**鼠标坐标没有跟着平移**：extractContents 传进来的 mouseX/mouseY 是屏幕绝对坐标
        //   （26.x 源码：translate 之后直接 extractLabels(graphics, mouseX, mouseY)）。
        //   所以所有 hover 判定必须用 this.isHovering(...)（父类会自己减 leftPos/topPos），
        //   不能用自制的 isInside(mouseX, mouseY, 面板坐标...)。
        final int x = 0;
        final int y = 0;

        // 标题
        g.text(this.font, Component.literal("植入体"), x + 8, y + 7, ACCENT);
        g.text(this.font, Component.literal("CYBERWARE"), x + 52, y + 8, DIM);
        drawHeaderRight(g, x, y);

        this.treeRows.clear();
        drawTree(g, x + TREE_X, y + TREE_TOP, mouseX, mouseY);
        drawDetail(g, x + DETAIL_X, y + TREE_TOP + 2);
        drawSelectedImplantFrame(g);
        // 按钮最后画 —— 详情内容再长也不会盖住它们
        drawActionButtons(g, mouseX, mouseY);
    }

    /**
     * 标题栏右侧：常驻「容量 已用/上限」；有提示时提示优先显示在容量左边。
     *
     * <p>容量读的是同步到客户端的义体表（与效果系统同一份 {@link CyberwareInstallation} API）。
     */
    private void drawHeaderRight(GuiGraphicsExtractor g, int x, int y) {
        int limit = CyberwareInstallation.capacityLimit();
        String cap = "容量 " + CyberwareInstallation.usedCapacity(this.minecraft.player) + "/"
                + (limit == Integer.MAX_VALUE ? "∞" : String.valueOf(limit));
        int capW = this.font.width(cap);
        g.text(this.font, Component.literal(cap), x + PANEL_W - 8 - capW, y + 5, TEXT);

        int right = x + PANEL_W - 12 - capW;
        if (this.statusActive()) {
            String text = this.font.plainSubstrByWidth(this.status, Math.max(40, right - 112));
            g.text(this.font, Component.literal(text), right - this.font.width(text), y + 5,
                    this.statusError ? WARN : ACCENT);
            return;
        }
        // 没有提示时维持原样：显示当前展开的分类名
        String cat = this.expandedSlot == null ? "" : this.expandedSlot.displayName();
        g.text(this.font, Component.literal(cat), x + PANEL_W - 8 - this.font.width(cat), y + 5, TEXT);
    }

    // ------------------------------------------------------------------
    // 安装 / 卸载（客户端只发请求，服务端裁决）
    // ------------------------------------------------------------------

    /** 画详情面板底部的两个按钮（可用性由本地同步数据预判，灰掉时不可点）。 */
    private void drawActionButtons(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        drawButton(g, BTN_INSTALL_X, "安装", installSourceSlot() >= 0, mouseX, mouseY);
        drawButton(g, BTN_UNINSTALL_X, "卸载", isSelectedInstalled(), mouseX, mouseY);
    }

    private void drawButton(GuiGraphicsExtractor g, int bx, String label, boolean enabled,
                            int mouseX, int mouseY) {
        boolean hovered = enabled && this.isHovering(bx, BTN_Y, BTN_W, BTN_H, mouseX, mouseY);
        int bg = !enabled ? CARD_DEEP : (hovered ? SEL_BG : CARD);
        int border = enabled ? ACCENT : BORDER;
        int fg = !enabled ? DIM : (hovered ? ACCENT : TEXT);
        g.fill(bx, BTN_Y, bx + BTN_W, BTN_Y + BTN_H, bg);
        g.fill(bx, BTN_Y, bx + BTN_W, BTN_Y + 1, border);
        g.fill(bx, BTN_Y + BTN_H - 1, bx + BTN_W, BTN_Y + BTN_H, border);
        g.fill(bx, BTN_Y, bx + 1, BTN_Y + BTN_H, border);
        g.fill(bx + BTN_W - 1, BTN_Y, bx + BTN_W, BTN_Y + BTN_H, border);
        g.text(this.font, Component.literal(label), bx + (BTN_W - this.font.width(label)) / 2,
                BTN_Y + 3, fg);
    }

    /**
     * 找到「装着当前选中型号」的操作台槽位下标；没有返回 -1。
     *
     * <p>读的是客户端这份菜单的容器 —— 内容由服务端同步下来，所以和服务端看到的一致。
     */
    private int installSourceSlot() {
        CyberwareDefinition def = this.selectedDef;
        if (def == null) {
            return -1;
        }
        var container = this.menu.getImplantContainer();
        for (int i = 0; i < CyberwareStationMenu.IMPLANT_SLOT_COUNT && i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof CyberwareItem item
                    && def.id().equals(item.cyberwareId())) {
                return i;
            }
        }
        return -1;
    }

    /** 当前选中的型号是否已经装在身上（读同步下来的义体表）。 */
    private boolean isSelectedInstalled() {
        CyberwareDefinition def = this.selectedDef;
        return def != null && CyberwareInstallation.has(this.minecraft.player, def.id());
    }

    /** 点「安装」：本地预检 → 发请求。预检用的是服务端同一份 {@code validateInstall}。 */
    private void requestInstall() {
        CyberwareDefinition def = this.selectedDef;
        if (def == null) {
            setStatus("先在左侧选一件义体", true);
            return;
        }
        int slot = installSourceSlot();
        if (slot < 0) {
            setStatus("操作台槽里没有「" + def.displayName() + "」", true);
            return;
        }
        Player player = this.minecraft.player;
        CyberwareData data = CyberwareItem.dataOf(this.menu.getImplantContainer().getItem(slot));
        String problem = CyberwareStationService.validateInstall(def, data, CyberwareInstallation.of(player));
        if (problem != null) {
            setStatus(problem, true);
            return;
        }
        ClientPacketDistributor.sendToServer(
                CyberwareActionPayload.install(this.menu.containerId, slot));
        setStatus("已发送安装请求：" + def.displayName(), false);
    }

    /** 点「卸载」：本地预检 → 发请求（带 defId，服务端据此定位要卸哪一件）。 */
    private void requestUninstall() {
        CyberwareDefinition def = this.selectedDef;
        if (def == null) {
            setStatus("先在左侧选一件义体", true);
            return;
        }
        if (!isSelectedInstalled()) {
            setStatus("身上没有装「" + def.displayName() + "」", true);
            return;
        }
        ClientPacketDistributor.sendToServer(
                CyberwareActionPayload.uninstall(this.menu.containerId, def.id()));
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

    // ------------------------------------------------------------------
    // 分类树
    // ------------------------------------------------------------------

    private static Identifier slotIcon(CyberwareSlot slot) {
        return Identifier.fromNamespaceAndPath(Cyberware.MODID,
                "textures/gui/slot/slot_" + slot.name().toLowerCase(Locale.ROOT) + ".png");
    }

    private static Identifier itemIcon(CyberwareDefinition def) {
        return Identifier.fromNamespaceAndPath(Cyberware.MODID, "textures/item/" + def.id() + ".png");
    }

    private static void drawIcon(GuiGraphicsExtractor g, Identifier icon, int x, int y, int size) {
        g.blit(RenderPipelines.GUI_TEXTURED, icon, x, y, 0.0F, 0.0F, size, size, 32, 32);
    }

    /**
     * 绘制分类树。
     *
     * <p>内容先整体排版（算出总高度），再按滚动偏移取可见区间绘制 —— 超出面板的行直接跳过，
     * 因为 {@link GuiGraphicsExtractor} 没有公开的裁剪入口。
     */
    private void drawTree(GuiGraphicsExtractor g, int tx, int ty, int mouseX, int mouseY) {
        int viewTop = ty;
        int viewBottom = TREE_BOTTOM;

        // ---- 第一遍：算出内容总高度 ----
        int total = 0;
        for (CyberwareSlot slot : CyberwareSlot.values()) {
            total += ROW_H;
            if (slot == this.expandedSlot) {
                total += countOf(slot) * ROW_H;
            }
        }
        this.contentHeight = total;
        int viewH = viewBottom - viewTop;
        int maxScroll = Math.max(0, total - viewH);
        if (this.scroll > maxScroll) {
            this.scroll = maxScroll;
        }
        if (this.scroll < 0) {
            this.scroll = 0;
        }

        // ---- 第二遍：只画可见行 ----
        int cursor = 0;
        for (CyberwareSlot slot : CyberwareSlot.values()) {
            int rowY = viewTop + cursor - this.scroll;
            if (rowY + ROW_H > viewTop && rowY < viewBottom) {
                boolean expanded = slot == this.expandedSlot;
                // 用父类的 isHovering(int,int,int,int,double,double)：它会自己减 leftPos/topPos。
                // 之前这里用 isInside(mouseX, mouseY, 面板坐标...) 比较的是两个不同坐标系，
                // hover 高亮实际偏了 (leftPos, topPos)（点击不受影响，因为那用的是绝对坐标）。
                boolean hovered = this.isHovering(tx, rowY, TREE_W, ROW_H, mouseX, mouseY);
                if (hovered) {
                    g.fill(tx, rowY, tx + TREE_W, rowY + ROW_H, HOVER_BG);
                }
                drawIcon(g, slotIcon(slot), tx + 2, rowY + 1, 12);
                int nameColor = hovered || expanded ? ACCENT : TEXT;
                g.text(this.font, Component.literal(slot.displayName()), tx + 18, rowY + 3, nameColor);
                // 右侧箭头
                String arrow = expanded ? "v" : ">";
                g.text(this.font, Component.literal(arrow), tx + TREE_W - 10, rowY + 3, expanded ? ACCENT : DIM);
                // 命中区存的是**屏幕绝对坐标**，鼠标事件用的就是这个坐标系
                this.treeRows.add(new HitRow(tx + this.leftPos, rowY + this.topPos,
                        TREE_W, ROW_H, slot, null));
            }
            cursor += ROW_H;

            if (slot != this.expandedSlot) {
                continue;
            }
            for (CyberwareDefinition def : CyberwareDefinitions.all().values()) {
                if (def.slot() != slot) {
                    continue;
                }
                rowY = viewTop + cursor - this.scroll;
                if (rowY + ROW_H > viewTop && rowY < viewBottom) {
                    boolean sel = def == this.selectedDef;
                    boolean hovered = this.isHovering(tx + 8, rowY, TREE_W - 8, ROW_H, mouseX, mouseY);
                    if (sel || hovered) {
                        g.fill(tx + 8, rowY, tx + TREE_W, rowY + ROW_H, sel ? SEL_BG : HOVER_BG);
                    }
                    if (sel) {
                        g.fill(tx + 8, rowY, tx + 10, rowY + ROW_H, ACCENT);
                    }
                    drawIcon(g, itemIcon(def), tx + 13, rowY + 1, 12);
                    CyberwareDefinition.Variant base = def.baseVariant();
                    int color = base == null ? DIM : base.rarity().color();
                    String name = this.font.plainSubstrByWidth(def.displayName(), TREE_W - 44);
                    g.text(this.font, Component.literal(name), tx + 28, rowY + 3,
                            sel ? ACCENT : (hovered ? ACCENT : color));
                    // 稀有度简写
                    if (base != null) {
                        String tag = base.rarity().displayName();
                        tag = tag.substring(0, Math.min(1, tag.length()));
                        g.text(this.font, Component.literal(tag), tx + TREE_W - 12, rowY + 3, color);
                    }
                    this.treeRows.add(new HitRow(tx + 8 + this.leftPos, rowY + this.topPos,
                            TREE_W - 8, ROW_H, slot, def));
                }
                cursor += ROW_H;
            }
        }

        // ---- 滚动条 ----
        if (total > viewH && total > 0) {
            int barX = tx + TREE_W - 2;
            int barH = Math.max(12, viewH * viewH / total);
            int barY = viewTop + (viewH - barH) * this.scroll / Math.max(1, maxScroll);
            g.fill(barX, viewTop, barX + 2, viewBottom, 0x33FFFFFF);
            g.fill(barX, barY, barX + 2, barY + barH, ACCENT);
        }
    }

    /** 高亮当前分类对应的那个义体槽，让「列表 ↔ 槽位」的对应关系一眼可见。 */
    private void drawSelectedImplantFrame(GuiGraphicsExtractor g) {
        if (this.expandedSlot == null) {
            return;
        }
        int index = this.expandedSlot.ordinal();
        if (index >= CyberwareStationMenu.IMPLANT_SLOT_COUNT || index >= this.menu.slots.size()) {
            return;
        }
        Slot slot = this.menu.slots.get(index);
        // 同样在相对面板的坐标系里
        int sx = slot.x;
        int sy = slot.y;
        g.fill(sx - 1, sy - 1, sx + 17, sy, ACCENT);
        g.fill(sx - 1, sy + 16, sx + 17, sy + 17, ACCENT);
        g.fill(sx - 1, sy - 1, sx, sy + 17, ACCENT);
        g.fill(sx + 16, sy - 1, sx + 17, sy + 17, ACCENT);
    }

    private static int countOf(CyberwareSlot slot) {
        int n = 0;
        for (CyberwareDefinition def : CyberwareDefinitions.all().values()) {
            if (def.slot() == slot) {
                n++;
            }
        }
        return n;
    }

    // ------------------------------------------------------------------
    // 右侧详情
    // ------------------------------------------------------------------

    private void drawDetail(GuiGraphicsExtractor g, int dx, int dy) {
        CyberwareDefinition def = this.selectedDef;
        if (def == null) {
            g.text(this.font, Component.literal("（未选择义体）"), dx, dy, DIM);
            return;
        }

        CyberwareDefinition.Variant base = def.baseVariant();
        int accent = base == null ? ACCENT : base.rarity().color();
        boolean installed = CyberwareInstallation.has(this.minecraft.player, def.id());

        // 图标 + 名称
        drawIcon(g, itemIcon(def), dx, dy, 28);
        int textX = dx + 34;
        g.text(this.font, Component.literal(def.displayName()), textX, dy + 2, accent);
        g.text(this.font, Component.literal(def.slot().displayName()), textX, dy + 13, DIM);
        if (base != null) {
            g.text(this.font, Component.literal(base.rarity().displayName()), textX, dy + 24, accent);
        }
        if (installed) {
            // 已装标记：告诉玩家「卸载」按钮现在有意义
            String tag = "已装";
            g.text(this.font, Component.literal(tag), dx + PANEL_W - 22 - this.font.width(tag),
                    dy + 2, ACCENT);
        }
        dy += 34;

        if (base == null) {
            g.text(this.font, Component.literal("（该型号无数值）"), dx, dy, DIM);
            return;
        }

        dy = section(g, dx, dy, "容量 " + base.capacity());

        // 按钮占着最下面一行，内容超过就截断（原来会直接画到按钮/面板外）
        int contentBottom = BTN_Y - 4;
        for (var entry : base.stats().entrySet()) {
            if (dy + 9 > contentBottom) {
                g.text(this.font, Component.literal("…"), dx, dy, DIM);
                return;
            }
            g.text(this.font, Component.literal(statLabel(entry.getKey())), dx, dy, DIM);
            String v = formatStat(entry.getKey(), entry.getValue());
            g.text(this.font, Component.literal(v), dx + 96, dy, TEXT);
            dy += 10;
        }

        // 全部变体（稀有度 + 容量）
        if (def.variants() != null && def.variants().size() > 1 && dy + 15 <= contentBottom) {
            dy += 4;
            g.text(this.font, Component.literal("型号 / 稀有度"), dx, dy, ACCENT);
            dy += 11;
            for (CyberwareDefinition.Variant v : def.variants()) {
                if (dy + 9 > contentBottom) {
                    g.text(this.font, Component.literal("…"), dx, dy, DIM);
                    return;
                }
                g.text(this.font, Component.literal(v.rarity().displayName()), dx, dy, v.rarity().color());
                g.text(this.font, Component.literal("容量 " + v.capacity()), dx + 60, dy, DIM);
                dy += 10;
            }
        }
    }

    private int section(GuiGraphicsExtractor g, int dx, int dy, String label) {
        g.text(this.font, Component.literal(label), dx, dy, TEXT);
        return dy + 13;
    }

    // ------------------------------------------------------------------
    // 交互
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            // ① 先判两个操作按钮：命中就发请求，并且**不再交给原版槽位处理**
            //    （否则同一次点击会既发安装请求、又去抓起操作台槽里的那件义体）
            if (this.isHovering(BTN_INSTALL_X, BTN_Y, BTN_W, BTN_H, event.x(), event.y())) {
                requestInstall();
                return true;
            }
            if (this.isHovering(BTN_UNINSTALL_X, BTN_Y, BTN_W, BTN_H, event.x(), event.y())) {
                requestUninstall();
                return true;
            }
            // ② 落在列表区域内 → 记下起点，接下来可能是拖动
            if (isInside(event.x(), event.y(), this.leftPos + TREE_X, this.topPos + TREE_TOP,
                    TREE_W, TREE_BOTTOM - TREE_TOP)) {
                this.draggingTree = true;
                this.lastDragY = event.y();
            }
            // ③ 点在底部义体槽上 → 树切到该槽位对应的分类，只列出能装进去的义体
            //    （槽位本身保持原版行为：可以点起来/放下义体，用来把快捷栏的物品放进义体槽）
            int implantIndex = implantSlotIndexAt(event.x(), event.y());
            if (implantIndex >= 0) {
                selectImplantSlot(implantIndex);
                // 槽里正好有义体 → 顺手把详情面板切到它，玩家接着就能点「安装」
                ItemStack inSlot = this.menu.getImplantContainer().getItem(implantIndex);
                if (!inSlot.isEmpty() && inSlot.getItem() instanceof CyberwareItem item) {
                    CyberwareDefinition def = item.definition();
                    if (def != null) {
                        this.selectedDef = def;
                    }
                }
                return super.mouseClicked(event, doubleClick);
            }
            for (HitRow row : this.treeRows) {
                if (!isInside(event.x(), event.y(), row.x(), row.y(), row.w(), row.h())) {
                    continue;
                }
                if (row.def() != null) {
                    this.selectedDef = row.def();
                } else {
                    this.expandedSlot = (this.expandedSlot == row.slot()) ? null : row.slot();
                    this.scroll = 0;
                    CyberwareDefinition first = firstOf(row.slot());
                    if (first != null) {
                        this.selectedDef = first;
                    }
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    /**
     * 手机上**没有滚轮** —— 触摸屏不会触发 {@code mouseScrolled}，
     * 所以列表必须支持「按住拖动」。拖动期间按纵向位移直接推 scroll。
     */
    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.draggingTree) {
            this.scroll -= (int) Math.round(event.y() - this.lastDragY);
            this.lastDragY = event.y();
            clampScroll();
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.draggingTree = false;
        return super.mouseReleased(event);
    }

    private void clampScroll() {
        int viewH = TREE_BOTTOM - TREE_TOP;
        int maxScroll = Math.max(0, this.contentHeight - viewH);
        this.scroll = Math.max(0, Math.min(this.scroll, maxScroll));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isInside(mouseX, mouseY, this.leftPos + TREE_X, this.topPos + TREE_TOP,
                TREE_W, TREE_BOTTOM - TREE_TOP)) {
            this.scroll -= (int) (scrollY * ROW_H * 2);
            int maxScroll = Math.max(0, this.contentHeight - (TREE_BOTTOM - TREE_TOP));
            this.scroll = Math.max(0, Math.min(this.scroll, maxScroll));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /**
     * 鼠标落在第几个义体槽上（-1 = 没有）。
     *
     * <p>{@code getHoveredSlot} 在 26.x 是私有的，所以这里按槽位坐标自己算。
     */
    private int implantSlotIndexAt(double mouseX, double mouseY) {
        int count = Math.min(CyberwareStationMenu.IMPLANT_SLOT_COUNT, this.menu.slots.size());
        for (int i = 0; i < count; i++) {
            Slot slot = this.menu.slots.get(i);
            int sx = this.leftPos + slot.x;
            int sy = this.topPos + slot.y;
            if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
                return i;
            }
        }
        return -1;
    }

    /** 选中某个义体槽：展开它对应的分类，并挑出第一个型号。 */
    private void selectImplantSlot(int index) {
        CyberwareSlot slot = CyberwareSlot.byIndexSafe(index);
        this.expandedSlot = slot;
        this.scroll = 0;
        CyberwareDefinition first = firstOf(slot);
        if (first != null) {
            this.selectedDef = first;
        }
    }

    private static boolean isInside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static CyberwareDefinition firstOf(CyberwareSlot slot) {
        for (CyberwareDefinition def : CyberwareDefinitions.all().values()) {
            if (def.slot() == slot) {
                return def;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // 文案
    // ------------------------------------------------------------------

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
