package com.dsh.cyberware.client;

import com.dsh.cyberware.Cyberware;
import com.dsh.cyberware.core.CyberwareDefinition;
import com.dsh.cyberware.core.CyberwareDefinitions;
import com.dsh.cyberware.core.CyberwareSlot;
import com.dsh.cyberware.menu.CyberwareStationMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * 义体操作台界面。
 *
 * <p>左侧分类树（可展开、可滚动），右侧型号详情。型号行用**自己的物品图标**，
 * 稀有度直接体现在名称颜色上；分类行用 10 个槽位的分类图标。
 *
 * <p>26.x 绘制入口是 {@link GuiGraphicsExtractor}（{@code GuiGraphics} 已移除），
 * 鼠标回调签名是 {@link MouseButtonEvent}。
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
    // ---- 布局 ----
    private static final int ROW_H = 14;
    private static final int TREE_X = 4;
    private static final int TREE_W = 150;
    private static final int TREE_TOP = 20;
    private static final int TREE_BOTTOM = PANEL_H - 52;
    private static final int DETAIL_X = 160;
    /** 底部义体槽条的位置（与 CyberwareStationMenu 的槽位坐标一致）。 */
    private static final int SLOT_STRIP_TOP = PANEL_H - 46;

    /** 当前展开的分类（同时只展开一个）。 */
    private CyberwareSlot expandedSlot = CyberwareSlot.OPERATING_SYSTEM;
    /** 右侧正在展示的型号。 */
    private CyberwareDefinition selectedDef = CyberwareDefinitions.SANDEVISTAN_ZETATECH;
    /** 分类树命中区，每帧重建，供点击检测。 */
    private final List<HitRow> treeRows = new ArrayList<>();

    /** 分类树滚动偏移（像素）。 */
    private int scroll = 0;
    /** 是否正在拖动列表（触屏用） */
    private boolean draggingTree;
    private double lastDragY;
    /** 每帧算出的内容总高度，用于夹紧滚动。 */
    private int contentHeight = 0;

    private record HitRow(int x, int y, int w, int h, CyberwareSlot slot, CyberwareDefinition def) {
    }

    public CyberwareStationScreen(CyberwareStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, PANEL_W, PANEL_H);
        this.inventoryLabelY = -1000; // 不显示背包标签
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
            Slot slot = this.menu.slots.get(i);
            int sx = x + slot.x;
            int sy = y + slot.y;
            g.fill(sx - 1, sy - 1, sx + 17, sy + 17, CARD_DEEP);
            g.fill(sx - 1, sy - 1, sx + 17, sy, BORDER);
            g.fill(sx - 1, sy + 16, sx + 17, sy + 17, BORDER);
            g.fill(sx - 1, sy - 1, sx, sy + 17, BORDER);
            g.fill(sx + 16, sy - 1, sx + 17, sy + 17, BORDER);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        // ⚠ 这里已经是「相对面板」坐标系：AbstractContainerScreen 在调用本方法前
        //   做过 pose().translate(leftPos, topPos)。再手动加一次偏移，整个界面就会
        //   右下方向错位 (leftPos, topPos) —— 之前的「错位 BUG」就是这个。
        final int x = 0;
        final int y = 0;

        // 标题
        g.text(this.font, Component.literal("植入体"), x + 8, y + 7, ACCENT);
        g.text(this.font, Component.literal("CYBERWARE"), x + 52, y + 8, DIM);
        // 右侧显示当前展开分类名
        String cat = this.expandedSlot == null ? "" : this.expandedSlot.displayName();
        g.text(this.font, Component.literal(cat), x + PANEL_W - 8 - this.font.width(cat), y + 5, TEXT);

        this.treeRows.clear();
        drawTree(g, x + TREE_X, y + TREE_TOP, mouseX, mouseY);
        drawDetail(g, x + DETAIL_X, y + TREE_TOP + 2);
        drawSelectedImplantFrame(g);
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
                boolean hovered = isInside(mouseX, mouseY, tx, rowY, TREE_W, ROW_H);
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
                    boolean hovered = isInside(mouseX, mouseY, tx + 8, rowY, TREE_W - 8, ROW_H);
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

        // 图标 + 名称
        drawIcon(g, itemIcon(def), dx, dy, 28);
        int textX = dx + 34;
        g.text(this.font, Component.literal(def.displayName()), textX, dy + 2, accent);
        g.text(this.font, Component.literal(def.slot().displayName()), textX, dy + 13, DIM);
        if (base != null) {
            g.text(this.font, Component.literal(base.rarity().displayName()), textX, dy + 24, accent);
        }
        dy += 34;

        if (base == null) {
            g.text(this.font, Component.literal("（该型号无数值）"), dx, dy, DIM);
            return;
        }

        dy = section(g, dx, dy, "容量 " + base.capacity());

        for (var entry : base.stats().entrySet()) {
            g.text(this.font, Component.literal(statLabel(entry.getKey())), dx, dy, DIM);
            String v = formatStat(entry.getKey(), entry.getValue());
            g.text(this.font, Component.literal(v), dx + 96, dy, TEXT);
            dy += 10;
        }

        // 全部变体（稀有度 + 容量）
        if (def.variants() != null && def.variants().size() > 1) {
            dy += 4;
            g.text(this.font, Component.literal("型号 / 稀有度"), dx, dy, ACCENT);
            dy += 11;
            for (CyberwareDefinition.Variant v : def.variants()) {
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
            // 落在列表区域内 → 记下起点，接下来可能是拖动
            if (isInside(event.x(), event.y(), this.leftPos + TREE_X, this.topPos + TREE_TOP,
                    TREE_W, TREE_BOTTOM - TREE_TOP)) {
                this.draggingTree = true;
                this.lastDragY = event.y();
            }
            // 点在底部义体槽上 → 树切到该槽位对应的分类，只列出能装进去的义体
            int implantIndex = implantSlotIndexAt(event.x(), event.y());
            if (implantIndex >= 0) {
                selectImplantSlot(implantIndex);
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
