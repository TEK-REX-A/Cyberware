package com.dsh.cyberware.client;

import com.dsh.cyberware.Cyberware;
import com.dsh.cyberware.core.CyberwareDefinition;
import com.dsh.cyberware.core.CyberwareDefinitions;
import com.dsh.cyberware.core.CyberwareInstallation;
import com.dsh.cyberware.core.CyberwareSlot;
import com.dsh.cyberware.core.CyberwareStats;
import com.dsh.cyberware.data.CyberwareData;
import com.dsh.cyberware.item.CyberwareItem;
import com.dsh.cyberware.menu.CyberwareStationMenu;
import com.dsh.cyberware.menu.CyberwareStationService;
import com.dsh.cyberware.network.CyberwareActionPayload;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
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

    // ---- 布局（全部按面板实际尺寸算，见 layout()；新稿要求 UI 随窗口加大）----
    /** 面板宽 = clamp(可用宽 × 0.8, 360, 540)。 */
    private static final int PANEL_MIN_W = 360;
    private static final int PANEL_MAX_W = 540;
    /** 面板高 = clamp(可用高 × 0.85, 226, 340)。 */
    private static final int PANEL_MIN_H = 226;
    private static final int PANEL_MAX_H = 340;

    private int panelW;
    private int panelH;
    /** 三栏卡片：左 18% / 中 48% / 右 34%（扣除间距后按比例分）。 */
    private int leftX;
    private int leftW;
    private int midX;
    private int midW;
    private int detailX;
    private int detailW;
    private int cardTop;
    private int cardH;
    private int cardGap = 4;
    private int bottomBandH;

    /** 左栏：人体图 + 部位名 + 版本号。 */
    private static final int BODY_W = 56;
    private static final int BODY_H = 120;
    private int bodyX;
    private int bodyY;

    /** 中栏：「已选中」chip + 两根竖条 + 列表 + 已安装。 */
    private int chipW;
    private int chipH;
    private int barX;
    private int barW;
    private int barGap;
    private int barTop;
    private int barH;
    private int capTextY;
    private int listX;
    private int listW;
    private int listTop;
    private int listBottom;
    private int instTop;
    private int instCellW;
    private int instCellH;
    private int instGap;
    private static final int INST_COLS = 5;
    private static final int INST_ROWS = 1;
    private static final int CARD_H = 24;
    private static final int CARD_GAP = 4;

    /** 缩略图（新稿「图片」位）。 */
    private static final int THUMB_LIST = 16;
    private static final int THUMB_INST = 12;
    private static final int THUMB_DETAIL = 32;

    /** 确认框（双击卸下）。 */
    private static final int CONFIRM_W = 200;
    private static final int CONFIRM_H = 74;
    private static final int CONFIRM_BTN_W = 76;
    private static final int CONFIRM_BTN_H = 18;
    private int confirmX;
    private int confirmY;

    /** 四色/双条颜色：红 = 生命值、蓝 = 护盾量、橙 = 义体容量（实=已用 / 虚=预览）。 */
    private static final int BAR_HP = 0xFFE04B5A;
    private static final int BAR_SHIELD = 0xFF3FA9F5;
    private static final int BAR_CAPACITY = 0xFFFF9E2C;

    /** 底部：安装按钮 + 右上角退出。 */
    private int btnX;
    private int btnY;
    private int btnW;
    private int btnH;
    private int closeX;
    private int closeY;
    private int closeS;

    private static final long STATUS_MILLIS = 4000L;

    // ---- 字号层级（新稿：标题 > 分组 > 正文 > 数值；原版只有一档 9px 字体，靠 pose 缩放做层级）----
    /** 面板标题放大倍数。 */
    private static final float TITLE_SCALE = 1.6F;
    /** 分组标题放大倍数。 */
    private static final float GROUP_SCALE = 1.15F;

    /**
     * 左下角版本号，新稿格式：{@code MC版本+Mod版本 加载器字母}，例如 {@code 26.1.2+0.5.4-Beta F}
     * （F = Fabric / N = NeoForge，见 {@link #loaderLetter()}）。
     */
    private static final String MOD_VERSION = modVersion();

    private static String modVersion() {
        return containerVersion("minecraft") + "+" + containerVersion(Cyberware.MODID)
                + " " + loaderLetter();
    }

    /** 取某个 mod 容器的 version 原始串（{@link FabricLoader} 在 CyberwareConfig 里已在用）。 */
    private static String containerVersion(String id) {
        try {
            return FabricLoader.getInstance().getModContainer(id)
                    .map(container -> container.getMetadata().getVersion().getFriendlyString())
                    .orElse("?");
        } catch (Throwable ignored) {
            return "?";
        }
    }

    /** 加载器字母：Fabric = F，其它（NeoForge）= N。 */
    private static String loaderLetter() {
        try {
            Class.forName("net.fabricmc.loader.api.FabricLoader");
            return "F";
        } catch (Throwable ignored) {
            return "N";
        }
    }

    // ------------------------------------------------------------------
    // 状态
    // ------------------------------------------------------------------

    /** 当前选中的部位（决定中栏列表与右栏）。 */
    private CyberwareSlot selectedPart = CyberwareSlot.OPERATING_SYSTEM;
    /** 当前选中的义体；null = 没选中。 */
    private CyberwareDefinition selectedDef;
    /** 待确认卸下的义体（双击已安装格弹出确认框）；null = 没有确认框。 */
    private CyberwareDefinition pendingUninstall;
    /** 缩略图解析缓存（defId -> texture）。换插画后清空即可。 */
    private static final Map<String, Identifier> THUMBS = new HashMap<>();
    /** 选中的那件是不是「身上已装的」（是的话底部按钮变成卸载）。 */
    private boolean selectedInstalled;

    /** 中栏列表滚动偏移（像素）。 */
    private int listScroll;
    /** 右栏「加成及效果」滚动偏移（行）。 */
    private int detailScroll;
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
        super(menu, playerInventory, title, panelWidth(), panelHeight());
        this.inventoryLabelY = -1000; // 不显示背包标签
        this.panelW = panelWidth();
        this.panelH = panelHeight();
        layout();
    }

    /** 新稿：面板宽 = clamp(可用宽 × 0.8, 360, 540)。 */
    private static int panelWidth() {
        return clamp(Math.round(guiWidth() * 0.8F), PANEL_MIN_W, PANEL_MAX_W);
    }

    /** 新稿：面板高 = clamp(可用高 × 0.85, 226, 340)。 */
    private static int panelHeight() {
        return clamp(Math.round(guiHeight() * 0.85F), PANEL_MIN_H, PANEL_MAX_H);
    }

    private static int guiWidth() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft == null ? PANEL_MIN_W : minecraft.getWindow().getGuiScaledWidth();
    }

    private static int guiHeight() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft == null ? PANEL_MIN_H : minecraft.getWindow().getGuiScaledHeight();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /** 按面板尺寸算三栏与各元素坐标（新稿比例 18% / 48% / 34%）。 */
    private void layout() {
        int usable = this.panelW - 8 - this.cardGap * 2;
        this.leftX = 4;
        this.leftW = Math.max(60, Math.round(usable * 0.18F));
        this.midX = this.leftX + this.leftW + this.cardGap;
        this.midW = Math.max(120, Math.round(usable * 0.48F));
        this.detailX = this.midX + this.midW + this.cardGap;
        this.detailW = this.panelW - 4 - this.detailX;

        this.cardTop = 24;
        this.bottomBandH = Math.max(34, Math.round(this.panelH * 0.15F));
        this.cardH = this.panelH - this.cardTop - this.bottomBandH - 4;

        // 左栏：人体图居中，部位名与版本号都压在卡片内部，不再出框
        this.bodyX = this.leftX + Math.max(2, (this.leftW - BODY_W) / 2);
        this.bodyY = this.cardTop + 22;

        // 中栏：chip + 两根竖条 + 列表 + 容量文字 + 已安装
        this.chipW = 64;
        this.chipH = 20;
        this.barX = this.midX + 2;
        this.barW = 9;
        this.barGap = 4;
        this.barTop = this.cardTop + 46;
        this.instTop = this.cardTop + this.cardH - 34;
        this.capTextY = this.instTop - 26;
        this.barH = this.capTextY - 4 - this.barTop;
        this.instCellH = 24;
        this.instGap = 2;
        this.instCellW = Math.max(26, (this.midW - 6 - this.instGap * (INST_COLS - 1)) / INST_COLS);
        this.listX = this.barX + this.barW * 2 + this.barGap + 4;
        this.listW = this.midX + this.midW - 2 - this.listX;
        this.listTop = this.cardTop + 44;
        this.listBottom = this.instTop - 16;

        // 底部：安装按钮靠右，左边一行「正在准备安装义体…」
        this.btnW = 84;
        this.btnH = 18;
        this.btnX = this.panelW - 6 - this.btnW;
        this.btnY = this.panelH - this.bottomBandH + 8;
        this.closeX = this.panelW - 18;
        this.closeY = 7;
        this.closeS = 11;

        // 确认框居中
        this.confirmX = (this.panelW - CONFIRM_W) / 2;
        this.confirmY = (this.panelH - CONFIRM_H) / 2;
    }

    /** 把玩家快捷栏那 9 格挪到面板底部（只动客户端这份菜单，见字段注释）。 */


    // ==================================================================
    // 背景（屏幕绝对坐标）
    // ==================================================================

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = this.leftPos;
        int y = this.topPos;

        StationPaint.panel(g, x, y, this.panelW, this.panelH);
        // 顶部 1px 亮线 + 标题栏分隔
        g.fill(x + 1, y + 1, x + this.panelW - 1, y + 2,
                StationPaint.withAlpha(StationPaint.ACCENT_CYAN, 0.30F));
        g.fill(x + 1, y + 22, x + this.panelW - 1, y + 23,
                StationPaint.withAlpha(StationPaint.LINE, 0.8F));

        // 三栏底板（按 layout() 的比例）
        StationPaint.roundRect(g, x + leftX, y + cardTop, leftW, cardH,
                StationPaint.RADIUS_CARD, StationPaint.BG_CARD);
        StationPaint.roundRect(g, x + midX, y + cardTop, midW, cardH,
                StationPaint.RADIUS_CARD, StationPaint.BG_CARD);
        StationPaint.roundRect(g, x + detailX, y + cardTop, detailW, cardH,
                StationPaint.RADIUS_CARD, StationPaint.BG_CARD);
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
        // 浮层最后画：中央提示 + 确认框，保证盖在三栏之上
        drawToast(g);
        drawConfirm(g);
    }

    // ---- 标题栏 -------------------------------------------------------

    private void drawHeader(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        StationPaint.groupBar(g, 4, 4, 12);
        String title = "义体操作台";
        textScaled(g, title, 10, 4, TITLE_SCALE, StationPaint.TEXT_PRIMARY);
        int subX = 10 + Math.round(this.font.width(title) * TITLE_SCALE) + StationPaint.GAP_S;
        g.text(this.font, Component.literal("CYBERWARE"), subX, 8, StationPaint.TEXT_SECONDARY);

        // 右上：ESC 提示 + 小 ×
        g.text(this.font, Component.literal("ESC"), closeX - 24, 9, StationPaint.TEXT_DISABLED);
        boolean closeHover = this.isHovering(closeX, closeY, closeS, closeS, mouseX, mouseY);
        if (closeHover) {
            StationPaint.roundRect(g, closeX, closeY, closeS, closeS, StationPaint.RADIUS_CARD,
                    StationPaint.BG_HOVER);
        }
        int c = closeHover ? StationPaint.TEXT_PRIMARY : StationPaint.TEXT_SECONDARY;
        for (int i = 2; i < closeS - 2; i++) {
            g.fill(closeX + i, closeY + i, closeX + i + 1, closeY + i + 1, c);
            g.fill(closeX + closeS - 1 - i, closeY + i, closeX + closeS - i, closeY + i + 1, c);
        }
    }

    // ---- 左栏：人体部位图 ---------------------------------------------

    private void drawBodyColumn(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        StationPaint.groupBar(g, leftX, cardTop + 3, 10);
        textScaled(g, "人体部位", leftX + 6, cardTop + 2, GROUP_SCALE, StationPaint.TEXT_SECONDARY);

        StationBodyMap.Part hovered = StationBodyMap.partAt(
                this.leftPos + bodyX, this.topPos + bodyY, mouseX, mouseY);
        StationBodyMap.draw(g, bodyX, bodyY, this.selectedPart,
                hovered == null ? null : hovered.slot(), installedSlots());

        // 部位名 + 版本号都压在卡片内部（原来贴到卡片外，被指「快超出边框」）
        String label = hovered != null ? hovered.label() : labelOf(this.selectedPart);
        g.text(this.font, Component.literal(label), leftX + 4, bodyY + BODY_H + 1,
                hovered != null ? StationPaint.ACCENT_CYAN : StationPaint.TEXT_SECONDARY);
        g.text(this.font, Component.literal(MOD_VERSION), leftX + 4, cardTop + cardH - 11,
                StationPaint.TEXT_DISABLED);
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
        Player who = getPlayer();

        // 左上「已选中 部位」chip（新稿那个小框）
        int chipY = cardTop + 2;
        StationPaint.roundRect(g, midX, chipY, chipW, chipH, StationPaint.RADIUS_CARD,
                StationPaint.BG_DEEP);
        StationPaint.drawRoundOutline(g, midX, chipY, chipW, chipH, StationPaint.RADIUS_CARD,
                StationPaint.withAlpha(StationPaint.ACCENT_CYAN, 0.45F));
        g.text(this.font, Component.literal("已选中"), midX + 4, chipY + 3, StationPaint.TEXT_DISABLED);
        String partName = this.font.plainSubstrByWidth(labelOf(this.selectedPart), chipW - 8);
        g.text(this.font, Component.literal(partName), midX + 4, chipY + 12, StationPaint.ACCENT_CYAN);

        // 「可选安装」标题放在 chip 右侧（原来被 chip 压住）
        StationPaint.groupBar(g, midX + chipW + 4, cardTop + 3, 10);
        textScaled(g, "可选安装", midX + chipW + 10, cardTop + 2, GROUP_SCALE, StationPaint.TEXT_SECONDARY);

        // 属性显示：生命值 / 护盾量（chip 正下方，不压竖条）
        g.text(this.font, Component.literal(Math.round(hpMax(who)) + "生命值"), midX + 2,
                chipY + chipH + 2, StationPaint.ACCENT_CYAN);
        g.text(this.font, Component.literal(Math.round(shieldMax(who)) + "护盾量"), midX + 2,
                chipY + chipH + 12, BAR_SHIELD);

        List<CyberwareDefinition> list = listFor(this.selectedPart);
        int viewH = listBottom - listTop;
        int maxScroll = maxListScroll(list.size());
        this.listScroll = Math.max(0, Math.min(this.listScroll, maxScroll));

        this.listCards.clear();
        if (list.isEmpty()) {
            String empty = this.selectedPart == null ? "点左侧人体选择部位" : "该部位暂无可安装义体";
            g.text(this.font, Component.literal(empty), listX + 4, listTop + 6,
                    StationPaint.TEXT_DISABLED);
        }
        for (int i = 0; i < list.size(); i++) {
            int cy = listTop + i * (CARD_H + CARD_GAP) - this.listScroll;
            if (cy + CARD_H <= listTop || cy >= listBottom) {
                continue;
            }
            CyberwareDefinition def = list.get(i);
            boolean sel = def == this.selectedDef && !this.selectedInstalled;
            boolean hovered = this.isHovering(listX, cy, listW, CARD_H, mouseX, mouseY);
            StationPaint.card(g, listX, cy, listW, CARD_H, sel, hovered);
            StationPaint.iconSlot(g, listX + 2, cy + 2, THUMB_LIST);
            drawThumb(g, def.id(), listX + 2, cy + 2, THUMB_LIST);

            int textX = listX + THUMB_LIST + 6;
            String name = this.font.plainSubstrByWidth(def.displayName(), listW - THUMB_LIST - 34);
            g.text(this.font, Component.literal(name), textX, cy + 4,
                    sel ? StationPaint.ACCENT_CYAN : StationPaint.TEXT_PRIMARY);
            String desc = def.description() == null ? "" : def.description();
            desc = this.font.plainSubstrByWidth(desc, listW - THUMB_LIST - 26);
            g.text(this.font, Component.literal(desc), textX, cy + 13, StationPaint.TEXT_SECONDARY);

            CyberwareDefinition.Variant base = def.baseVariant();
            if (base != null) {
                String cap = String.valueOf(base.capacity());
                g.text(this.font, Component.literal(cap),
                        listX + listW - 6 - this.font.width(cap), cy + 13, StationPaint.ACCENT_CYAN);
            }
            this.listCards.add(new HitCard(listX + this.leftPos, cy + this.topPos, listW, CARD_H, def));
        }

        if (maxScroll > 0) {
            int barX = listX + listW - 2;
            int barH = Math.max(10, viewH * viewH / (viewH + maxScroll));
            int barY = listTop + (viewH - barH) * this.listScroll / maxScroll;
            g.fill(barX, listTop, barX + 2, listBottom,
                    StationPaint.withAlpha(StationPaint.BG_DEEP, 0.85F));
            g.fill(barX, barY, barX + 2, barY + barH,
                    StationPaint.withAlpha(StationPaint.ACCENT_CYAN, 0.75F));
        }

        drawCapacity(g);
        drawInstalled(g, mouseX, mouseY);
    }

    private int maxListScroll(int rows) {
        int content = rows * (CARD_H + CARD_GAP) - CARD_GAP;
        return Math.max(0, content - (listBottom - listTop));
    }

    /** 已安装：按实际数量生成格子，绝不画固定 5 格 / 红色禁用块。 */
    private void drawInstalled(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        StationPaint.groupBar(g, midX, instTop - 12, 10);
        textScaled(g, "已安装", midX + 6, instTop - 13, GROUP_SCALE, StationPaint.TEXT_SECONDARY);

        List<String> ids = installedIds();
        this.installedCells.clear();
        if (ids.isEmpty()) {
            g.text(this.font, Component.literal("还没有安装任何义体"), midX + 2, instTop + 6,
                    StationPaint.TEXT_DISABLED);
            return;
        }
        int shown = Math.min(ids.size(), INST_COLS * INST_ROWS);
        for (int i = 0; i < shown; i++) {
            String id = ids.get(i);
            CyberwareDefinition def = CyberwareDefinitions.byId(id);
            int cx = midX + 2 + (i % INST_COLS) * (instCellW + instGap);
            int cy = instTop + (i / INST_COLS) * (instCellH + instGap);
            boolean sel = this.selectedInstalled
                    && id.equals(this.selectedDef == null ? null : this.selectedDef.id());
            boolean hovered = this.isHovering(cx, cy, instCellW, instCellH, mouseX, mouseY);
            StationPaint.roundRect(g, cx, cy, instCellW, instCellH, StationPaint.RADIUS_CARD,
                    hovered ? StationPaint.BG_HOVER : StationPaint.BG_DEEP);
            StationPaint.drawRoundOutline(g, cx, cy, instCellW, instCellH, StationPaint.RADIUS_CARD,
                    sel ? StationPaint.ACCENT_CYAN
                            : StationPaint.withAlpha(StationPaint.LINE, hovered ? 0.9F : 0.6F));
            if (def != null) {
                drawThumb(g, def.id(), cx + 2, cy + 4, THUMB_INST);
                String label = this.font.plainSubstrByWidth(shortName(def), instCellW - THUMB_INST - 4);
                g.text(this.font, Component.literal(label), cx + THUMB_INST + 3, cy + 4,
                        sel ? StationPaint.ACCENT_CYAN : StationPaint.TEXT_PRIMARY);
                String cap = String.valueOf(capacityOf(id));
                g.text(this.font, Component.literal(cap),
                        cx + instCellW - 3 - this.font.width(cap), cy + 13, StationPaint.ACCENT_CYAN);
            } else {
                g.text(this.font, Component.literal("?"), cx + THUMB_INST + 3, cy + 8,
                        StationPaint.TEXT_DISABLED);
            }
            this.installedCells.add(new HitCell(cx + this.leftPos, cy + this.topPos,
                    instCellW, instCellH, id));
        }
        if (ids.size() > shown) {
            String more = "+" + (ids.size() - shown);
            g.text(this.font, Component.literal(more), midX + midW - 4 - this.font.width(more),
                    instTop - 13, StationPaint.TEXT_DISABLED);
        }
    }

    /**
     * <b>两根并排的竖条</b>（新稿定义）：
     * <ul>
     *   <li>左条 = <b>此刻有多抗揍</b>：红 = 生命值、蓝 = 护盾量（这是「结果」）</li>
     *   <li>右条 = <b>装义体要花多少</b>：橙实 = 已用容量、橙虚 = 选中件的容量预览（这是「代价」）</li>
     * </ul>
     * 两根都做圆角；预览段是真虚线（3px 亮 / 3px 灭），不是描边冒充。
     * 数字放在两根条正下方，不再压在条上。
     */
    private void drawCapacity(GuiGraphicsExtractor g) {
        Player player = getPlayer();
        int hp = (int) Math.round(hpMax(player));
        int shield = (int) Math.round(shieldMax(player));
        int limit = CyberwareInstallation.capacityLimit();
        boolean unlimited = limit == Integer.MAX_VALUE;
        int used = player == null ? 0 : CyberwareInstallation.usedCapacity(player);
        int preview = Math.max(0, selectedCost());
        int innerH = this.barH - 2;

        // 左条：生命值（上）+ 护盾量（下）
        drawBarTrack(g, barX, barTop, barW, barH);
        int vitality = Math.max(1, hp + shield);
        int hpH = Math.round((float) innerH * hp / vitality);
        if (hpH > 0) {
            StationPaint.roundRect(g, barX + 1, barTop + 1, barW - 2, hpH, 2,
                    StationPaint.withAlpha(BAR_HP, 0.92F));
        }
        if (innerH - hpH > 0) {
            StationPaint.roundRect(g, barX + 1, barTop + 1 + hpH, barW - 2, innerH - hpH, 2,
                    StationPaint.withAlpha(BAR_SHIELD, 0.92F));
        }

        // 右条：已用容量（从底往上）+ 预览虚线
        int capBarX = barX + barW + barGap;
        drawBarTrack(g, capBarX, barTop, barW, barH);
        int scale = Math.max(1, unlimited ? Math.max(1, used + preview) : limit);
        int usedH = Math.min(innerH, Math.round((float) innerH * used / scale));
        int prevH = Math.min(innerH - usedH, Math.round((float) innerH * preview / scale));
        if (usedH > 0) {
            StationPaint.roundRect(g, capBarX + 1, barTop + 1 + innerH - usedH, barW - 2, usedH, 2,
                    StationPaint.withAlpha(BAR_CAPACITY, 0.95F));
        }
        if (prevH > 0) {
            int top = barTop + 1 + innerH - usedH - prevH;
            for (int i = 0; i < prevH; i += 6) {
                g.fill(capBarX + 1, top + i, capBarX + barW - 1, top + Math.min(prevH, i + 3),
                        StationPaint.withAlpha(BAR_CAPACITY, 0.85F));
            }
        }

        // 数字（两根条正下方）
        String text = used + "/" + (unlimited ? "∞" : String.valueOf(limit)) + "容量";
        g.text(this.font, Component.literal(text), midX + 2, capTextY, StationPaint.TEXT_SECONDARY);
        String right = "+" + (preview <= 0 ? "—" : String.valueOf(preview)) + " / 剩余 "
                + (unlimited ? "∞" : String.valueOf(Math.max(0, limit - used)));
        boolean tight = !unlimited && used + preview > limit;
        g.text(this.font, Component.literal(right), midX + midW - 2 - this.font.width(right),
                capTextY, tight ? StationPaint.WARN_AMBER : StationPaint.ACCENT_CYAN);
    }

    /** 竖条轨道：圆角描边 + 深底。 */
    private static void drawBarTrack(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        StationPaint.roundRect(g, x, y, w, h, 3, StationPaint.withAlpha(StationPaint.LINE, 0.9F));
        StationPaint.roundRect(g, x + 1, y + 1, w - 2, h - 2, 2, StationPaint.BG_DEEP);
    }

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
        StationPaint.groupBar(g, detailX, cardTop + 3, 10);
        textScaled(g, "已选中的义体", detailX + 6, cardTop + 2, GROUP_SCALE, StationPaint.TEXT_SECONDARY);

        int dx = detailX + 4;
        int dw = detailW - 12;
        if (this.selectedDef == null) {
            drawPartInfo(g, dx, dw);
            return;
        }

        CyberwareDefinition def = this.selectedDef;
        CyberwareDefinition.Variant base = selectedVariant();
        int accent = base == null ? StationPaint.ACCENT_CYAN : base.rarity().color();

        // 右上角大缩略图（新稿「图片」位）
        int thumbX = detailX + detailW - 6 - THUMB_DETAIL;
        int thumbY = cardTop + 16;
        StationPaint.iconSlot(g, thumbX, thumbY, THUMB_DETAIL);
        drawThumb(g, def.id(), thumbX, thumbY, THUMB_DETAIL);

        int nameW = Math.max(36, thumbX - dx - 4);
        String name = this.font.plainSubstrByWidth(def.displayName(), nameW);
        g.text(this.font, Component.literal(name), dx, cardTop + 18, StationPaint.TEXT_PRIMARY);
        String en = englishName(def);
        g.text(this.font, Component.literal(this.font.plainSubstrByWidth(en, nameW)), dx, cardTop + 30,
                StationPaint.TEXT_SECONDARY);

        if (base != null) {
            String tag = base.rarity().displayName();
            int tw = this.font.width(tag) + 8;
            int tagY = cardTop + 42;
            StationPaint.tag(g, dx, tagY, tw, 11, accent);
            g.text(this.font, Component.literal(tag), dx + 4, tagY + 1, accent);
            if (this.selectedInstalled) {
                String ins = "已装";
                int iw = this.font.width(ins) + 8;
                int ix = dx + tw + 4;
                if (ix + iw <= dx + dw) {
                    StationPaint.tag(g, ix, tagY, iw, 11, StationPaint.OK_GREEN);
                    g.text(this.font, Component.literal(ins), ix + 4, tagY + 1, StationPaint.OK_GREEN);
                }
            }
        }

        // 加成及效果（标签左 / 数值右；行数超出时右栏可滚动，滚动条不消失）
        int statsTop = cardTop + 60;
        int statsBottom = cardTop + cardH - 58;
        StationPaint.groupBar(g, dx, statsTop - 9, 9);
        g.text(this.font, Component.literal("加成及效果"), dx + 6, statsTop - 8,
                StationPaint.TEXT_SECONDARY);

        List<Map.Entry<String, Double>> entries = new ArrayList<>();
        if (base != null) {
            entries.addAll(base.stats().entrySet());
        }
        int rowH = 11;
        int rows = Math.max(1, (statsBottom - statsTop) / rowH);
        int maxScroll = Math.max(0, entries.size() - rows);
        this.detailScroll = Math.max(0, Math.min(this.detailScroll, maxScroll));

        if (entries.isEmpty()) {
            g.text(this.font, Component.literal("（该型号无数值）"), dx, statsTop,
                    StationPaint.TEXT_DISABLED);
        }
        for (int i = 0; i < rows && i + this.detailScroll < entries.size(); i++) {
            var entry = entries.get(i + this.detailScroll);
            int y = statsTop + i * rowH;
            String label = this.font.plainSubstrByWidth(statLabel(entry.getKey()), dw - 34);
            g.text(this.font, Component.literal(label), dx, y, StationPaint.TEXT_SECONDARY);
            String value = formatStat(entry.getKey(), entry.getValue());
            g.text(this.font, Component.literal(value), dx + dw - this.font.width(value), y,
                    StationPaint.ACCENT_CYAN);
        }
        drawScrollbar(g, detailX + detailW - 5, statsTop, statsBottom, rows, entries.size(),
                this.detailScroll);

        // 属性变化对比（固定在右栏下半）
        drawStatCompare(g, dx, dw, cardTop + cardH - 50);
    }

    /** 右栏滚动条（内容超出才画）。 */
    private void drawScrollbar(GuiGraphicsExtractor g, int x, int top, int bottom, int rows, int total,
                               int scroll) {
        int h = bottom - top;
        if (h <= 6 || total <= rows) {
            return;
        }
        g.fill(x, top, x + 2, bottom, StationPaint.withAlpha(StationPaint.BG_DEEP, 0.85F));
        int thumb = Math.max(10, h * rows / total);
        int y = top + (h - thumb) * scroll / Math.max(1, total - rows);
        g.fill(x, y, x + 2, y + thumb, StationPaint.withAlpha(StationPaint.ACCENT_CYAN, 0.75F));
    }

    /**
     * 属性变化对比（新稿：{@code 20生命值 → 20生命值}）。
     *
     * <p>左 = 当前值，右 = 装上（选中列表里的）或卸下（选中已安装格）之后的值；
     * 有变化的那一行右侧用 accent 高亮，没变化保持次级色。
     *
     * <p>标签缩成「生命 / 护盾 / 恢复」是因为右栏只有 86px —— 全称在「加成及效果」里已经写过一遍。
     */
    private void drawStatCompare(GuiGraphicsExtractor g, int x, int w, int top) {
        StationPaint.groupBar(g, x, top, 9);
        g.text(this.font, Component.literal("属性变化"), x + 6, top + 1, StationPaint.TEXT_SECONDARY);
        int y = top + 13;
        CyberwareDefinition.Variant v = selectedVariant();
        double sign = this.selectedInstalled ? -1.0D : 1.0D;
        Player who = getPlayer();
        y = compareRow(g, x, w, y, "生命", hpMax(who),
                statOf(v, CyberwareDefinition.Stats.MAX_HEALTH) * sign, false);
        y = compareRow(g, x, w, y, "护盾", shieldMax(who),
                statOf(v, CyberwareDefinition.Stats.SHIELD) * sign, false);
        compareRow(g, x, w, y, "恢复", shieldRegen(who),
                statOf(v, CyberwareDefinition.Stats.SHIELD_REGEN) * sign, true);
    }

    private int compareRow(GuiGraphicsExtractor g, int x, int w, int y, String label,
                           double now, double delta, boolean decimal) {
        boolean changed = Math.abs(delta) > 0.001D;
        String left = num(now, decimal) + label;
        String right = num(now + delta, decimal) + label;
        g.text(this.font, Component.literal(left), x, y, StationPaint.TEXT_SECONDARY);
        String arrow = "→";
        g.text(this.font, Component.literal(arrow), x + (w - this.font.width(arrow)) / 2, y,
                StationPaint.TEXT_DISABLED);
        int rw = this.font.width(right);
        g.text(this.font, Component.literal(right), x + w - rw, y,
                changed ? StationPaint.ACCENT_CYAN : StationPaint.TEXT_SECONDARY);
        return y + 11;
    }

    private static String num(double value, boolean decimal) {
        return decimal ? String.format(Locale.ROOT, "%.1f", value) : String.valueOf(Math.round(value));
    }

    /** 没选中义体时，右栏显示当前部位的信息（点部位只切这里，不执行安装）。 */
    private void drawPartInfo(GuiGraphicsExtractor g, int dx, int dw) {
        CyberwareSlot part = this.selectedPart;
        if (part == null) {
            g.text(this.font, Component.literal("点左侧人体选择部位"), dx, cardTop + 18,
                    StationPaint.TEXT_DISABLED);
            return;
        }
        String label = this.font.plainSubstrByWidth(labelOf(part), dw);
        g.text(this.font, Component.literal(label), dx, cardTop + 18, StationPaint.TEXT_PRIMARY);

        int usedInSlot = 0;
        for (String id : installedIds()) {
            CyberwareDefinition def = CyberwareDefinitions.byId(id);
            if (def != null && def.slot() == part) {
                usedInSlot++;
            }
        }
        StationPaint.groupBar(g, dx, cardTop + 34, 9);
        g.text(this.font, Component.literal("本部位"), dx + 6, cardTop + 35, StationPaint.TEXT_SECONDARY);
        g.text(this.font, Component.literal("已装 " + usedInSlot + "/" + Math.max(1, part.maxCount())),
                dx, cardTop + 48, StationPaint.TEXT_PRIMARY);
        g.text(this.font, Component.literal("可安装 " + listFor(part).size() + " 件"), dx, cardTop + 59,
                StationPaint.TEXT_SECONDARY);

        Player who = getPlayer();
        StationPaint.groupBar(g, dx, cardTop + 74, 9);
        g.text(this.font, Component.literal("当前属性"), dx + 6, cardTop + 75,
                StationPaint.TEXT_SECONDARY);
        g.text(this.font, Component.literal(Math.round(hpMax(who)) + "生命值"), dx, cardTop + 88,
                StationPaint.TEXT_PRIMARY);
        g.text(this.font, Component.literal(Math.round(shieldMax(who)) + "护盾量"), dx, cardTop + 99,
                StationPaint.TEXT_PRIMARY);
        g.text(this.font, Component.literal(num(shieldRegen(who), true) + "/s恢复"), dx, cardTop + 110,
                StationPaint.TEXT_SECONDARY);
    }

    /** 英文名：定义表没有独立英文字段，用注册 id 推导（待核实是否有更正规的来源）。 */
    private static String englishName(CyberwareDefinition def) {
        return def.id().toUpperCase(Locale.ROOT).replace('_', ' ');
    }

    // ---- 底部：快捷栏标签 + 安装按钮 ----------------------------------

    private void drawBottom(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        String reason = installBlockReason();
        boolean ready = reason == null;
        boolean hovered = ready && this.isHovering(btnX, btnY, btnW, btnH, mouseX, mouseY);

        // 新稿：按钮左边一行「正在准备安装义体…」（快捷栏已按主人要求删除）
        String prep = this.selectedDef == null ? "选一件义体开始安装"
                : (this.selectedInstalled ? "正在准备卸下义体…"
                        : "正在准备安装义体… " + this.font.plainSubstrByWidth(this.selectedDef.displayName(), 70));
        g.text(this.font, Component.literal(this.font.plainSubstrByWidth(prep, btnX - midX - 8)),
                midX, btnY + 5, StationPaint.TEXT_DISABLED);

        StationPaint.button(g, btnX, btnY, btnW, btnH, ready, hovered);
        String label = this.selectedInstalled ? "确认卸下" : "确认安装";
        int fg = !ready ? StationPaint.TEXT_DISABLED
                : (hovered ? StationPaint.ACCENT_CYAN : StationPaint.TEXT_PRIMARY);
        g.text(this.font, Component.literal(label), btnX + (btnW - this.font.width(label)) / 2,
                btnY + 5, fg);

        if (!ready) {
            String why = this.font.plainSubstrByWidth(reason, btnW + 40);
            g.text(this.font, Component.literal(why), btnX + (btnW - this.font.width(why)) / 2,
                    btnY - 11, StationPaint.WARN_AMBER);
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
        requestUninstallOf(this.selectedDef);
    }

    /** 卸下指定的那件（确认框点「确认」后走这里）。 */
    private void requestUninstallOf(CyberwareDefinition def) {
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

        // ⓪ 确认框打开时：模态，只认「确认 / 取消」
        if (this.pendingUninstall != null) {
            int okX = confirmX + 16;
            int btnY2 = confirmY + CONFIRM_H - 28;
            int cancelX = confirmX + CONFIRM_W - 16 - CONFIRM_BTN_W;
            if (this.isHovering(okX, btnY2, CONFIRM_BTN_W, CONFIRM_BTN_H, mx, my)) {
                CyberwareDefinition target = this.pendingUninstall;
                this.pendingUninstall = null;
                requestUninstallOf(target);
            } else if (this.isHovering(cancelX, btnY2, CONFIRM_BTN_W, CONFIRM_BTN_H, mx, my)) {
                this.pendingUninstall = null;
            }
            return true;
        }

        // ① 右上小 ×
        if (this.isHovering(closeX, closeY, closeS, closeS, mx, my)) {
            this.onClose();
            return true;
        }

        // ② 安装 / 卸载按钮
        if (this.isHovering(btnX, btnY, btnW, btnH, mx, my)) {
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

        // ③ 已安装格：单击看详情，双击卸下（确认框）
        for (HitCell cell : this.installedCells) {
            if (isInside(mx, my, cell.x(), cell.y(), cell.w(), cell.h())) {
                CyberwareDefinition def = CyberwareDefinitions.byId(cell.defId());
                if (doubleClick && def != null) {
                    this.selectedDef = def;
                    this.selectedInstalled = true;
                    this.pendingUninstall = def;
                    return true;
                }
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

        // ④ 列表（拖动与点选在 release 里区分）
        if (isInside(mx, my, this.leftPos + listX, this.topPos + listTop, listW,
                listBottom - listTop)) {
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

        // ⑤ 人体部位
        StationBodyMap.Part part = StationBodyMap.partAt(
                this.leftPos + bodyX, this.topPos + bodyY, mx, my);
        if (part != null) {
            selectPart(part.slot());
            return true;
        }

        // ⑥ 点空白
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
        if (isInside(mouseX, mouseY, this.leftPos + listX, this.topPos + listTop, listW,
                listBottom - listTop)) {
            this.listScroll -= (int) (scrollY * (CARD_H + CARD_GAP));
            clampListScroll();
            return true;
        }
        // 右栏「加成及效果」滚动
        if (isInside(mouseX, mouseY, this.leftPos + detailX, this.topPos + cardTop, detailW, cardH)) {
            this.detailScroll = Math.max(0, this.detailScroll - (int) Math.round(scrollY));
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


    /** 鼠标落在第几个快捷栏格上（返回 menu.slots 下标，-1 = 没有）。{@code getHoveredSlot} 在 26.x 是私有的，自己算。 */


    private Player getPlayer() {
        return this.minecraft == null ? null : this.minecraft.player;
    }

    private static boolean isInside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ==================================================================
    // 缩略图 / 简称 / 属性取值 / 浮层
    // ==================================================================

    /**
     * 缩略图入口（新稿的「图片」位）。<b>将来换真插画只改这一个方法</b>：
     * 先找 {@code textures/gui/thumb/<defId>.png}，没有就退回 32x32 的 item 图标。
     *
     * <p>尺寸都取 32 的整数分之一（16 / 32），避免非整数倍缩放把像素糊掉。
     */
    private Identifier thumbnail(String defId) {
        Identifier cached = THUMBS.get(defId);
        if (cached != null) {
            return cached;
        }
        Identifier art = Identifier.fromNamespaceAndPath(Cyberware.MODID,
                "textures/gui/thumb/" + defId + ".png");
        Identifier use = resourceExists(art) ? art
                : Identifier.fromNamespaceAndPath(Cyberware.MODID, "textures/item/" + defId + ".png");
        THUMBS.put(defId, use);
        return use;
    }

    private boolean resourceExists(Identifier id) {
        if (this.minecraft == null) {
            return false;
        }
        var manager = this.minecraft.getResourceManager();
        return manager != null && !manager.getResourceStack(id).isEmpty();
    }

    private void drawThumb(GuiGraphicsExtractor g, String defId, int x, int y, int size) {
        drawIcon(g, thumbnail(defId), x, y, size);
    }

    /**
     * 放大绘制文字（新稿的字号层级：标题 1.6x、分组 1.15x、正文 1.0）。
     *
     * <p>{@code GuiGraphicsExtractor.pose()} 返回 {@code Matrix3x2fStack}（javap 核实过），
     * pushMatrix / translate / scale / popMatrix 都是 joml 的标准方法，没有凭记忆写的签名。
     */
    private void textScaled(GuiGraphicsExtractor g, String text, int x, int y, float scale, int color) {
        g.pose().pushMatrix();
        g.pose().translate((float) x, (float) y);
        g.pose().scale(scale, scale);
        g.text(this.font, Component.literal(text), 0, 0, color);
        g.pose().popMatrix();
    }

    /**
     * 已安装格里的简称（新稿：{@code 军用科技·斯安威斯坦"远地点" -> 远地点}）。
     *
     * <p>规则按优先级：① 引号里的内容（中英文引号都认）② 最后一个 · 或空格之后那段 ③ 原名截断。
     * 要调规则只改这一处。
     */
    private static String shortName(CyberwareDefinition def) {
        String full = def == null ? "" : def.displayName();
        if (full.isEmpty()) {
            return "";
        }
        // ① 引号内：中英文/书名号都认（「远地点」/“远地点”/"远地点"）
        String[][] quotes = {{"\u300c", "\u300d"}, {"\u201c", "\u201d"}, {"\"", "\""}, {"\u300e", "\u300f"}};
        for (String[] pair : quotes) {
            int a = full.indexOf(pair[0]);
            int b = a < 0 ? -1 : full.indexOf(pair[1], a + pair[0].length());
            if (a >= 0 && b > a + pair[0].length()) {
                return full.substring(a + pair[0].length(), b);
            }
        }
        // ② 最后一个 · 之后（也认 ・ / • / ／ / ））
        int cut = -1;
        for (char sep : new char[] {'\u00b7', '\u30fb', '\u2022', '\u3001'}) {
            cut = Math.max(cut, full.lastIndexOf(sep));
        }
        if (cut >= 0 && cut + 1 < full.length()) {
            return full.substring(cut + 1);
        }
        // ③ 最后一个空格之后（英文名那种 "Military Tech Falcon"）
        int space = full.lastIndexOf(' ');
        if (space >= 0 && space + 1 < full.length()) {
            return full.substring(space + 1);
        }
        // ④ 实在没有分隔符就原样（格子那边按宽度截断）
        return full;
    }

    private static double hpMax(Player player) {
        return player == null ? 20.0D : player.getMaxHealth();
    }

    private static double shieldMax(Player player) {
        return player == null ? 0.0D : CyberwareStats.maxShield(player);
    }

    private static double shieldRegen(Player player) {
        return player == null ? 0.0D : CyberwareStats.shieldRegenPerSecond(player);
    }

    /** 选中件的实际档位：已装看身上的稀有度，未装看背包里那件的稀有度，最后退回基础档。 */
    private CyberwareDefinition.Variant selectedVariant() {
        CyberwareDefinition def = this.selectedDef;
        if (def == null) {
            return null;
        }
        Player player = getPlayer();
        if (player != null) {
            CyberwareData data = this.selectedInstalled
                    ? CyberwareInstallation.dataOf(player, def.id())
                    : dataInInventory(player, def.id());
            if (data != null) {
                CyberwareDefinition.Variant variant = def.variantFor(data.rarity());
                if (variant != null) {
                    return variant;
                }
            }
        }
        return def.baseVariant();
    }

    private CyberwareData dataInInventory(Player player, String defId) {
        int at = CyberwareStationService.findInventorySlot(player, defId);
        return at < 0 ? null : CyberwareItem.dataOf(player.getInventory().getItem(at));
    }

    private static double statOf(CyberwareDefinition.Variant variant, String key) {
        if (variant == null || variant.stats() == null) {
            return 0.0D;
        }
        Double value = variant.stats().get(key);
        return value == null ? 0.0D : value;
    }

    /** 中央提示（新稿：成功安装 xxx / 安装失败），最后 0.6 秒淡出。 */
    private void drawToast(GuiGraphicsExtractor g) {
        if (!statusActive()) {
            return;
        }
        long remain = STATUS_MILLIS - (Util.getMillis() - this.statusAt);
        float fade = remain < 600L ? Math.max(0.0F, remain / 600.0F) : 1.0F;
        int color = this.statusError ? StationPaint.WARN_AMBER : StationPaint.OK_GREEN;
        String text = this.font.plainSubstrByWidth(this.status, this.panelW - 64);
        int boxW = this.font.width(text) + 24;
        int boxH = 20;
        int bx = (this.panelW - boxW) / 2;
        int by = this.panelH / 2 - 34;
        StationPaint.roundRect(g, bx, by, boxW, boxH, StationPaint.RADIUS_CARD,
                StationPaint.withAlpha(StationPaint.BG_DEEP, 0.94F * fade));
        StationPaint.drawRoundOutline(g, bx, by, boxW, boxH, StationPaint.RADIUS_CARD,
                StationPaint.withAlpha(color, 0.85F * fade));
        g.text(this.font, Component.literal(text), bx + 12, by + 6,
                StationPaint.withAlpha(color, fade));
    }

    /** 双击卸下的确认框（模态）：遮罩 + 名字 + 「确认卸下 / 取消」。 */
    private void drawConfirm(GuiGraphicsExtractor g) {
        if (this.pendingUninstall == null) {
            return;
        }
        g.fill(0, 0, this.panelW, this.panelH, 0xB4000000);
        StationPaint.roundRect(g, confirmX, confirmY, CONFIRM_W, CONFIRM_H,
                StationPaint.RADIUS_PANEL, StationPaint.BG_PANEL);
        StationPaint.drawRoundOutline(g, confirmX, confirmY, CONFIRM_W, CONFIRM_H,
                StationPaint.RADIUS_PANEL, StationPaint.withAlpha(StationPaint.WARN_AMBER, 0.55F));
        g.text(this.font, Component.literal("确认卸下？"), confirmX + 12, confirmY + 12,
                StationPaint.TEXT_PRIMARY);
        String name = this.font.plainSubstrByWidth(this.pendingUninstall.displayName(), CONFIRM_W - 24);
        g.text(this.font, Component.literal(name), confirmX + 12, confirmY + 26,
                StationPaint.TEXT_SECONDARY);

        int btnY2 = confirmY + CONFIRM_H - 28;
        int okX = confirmX + 16;
        int cancelX = confirmX + CONFIRM_W - 16 - CONFIRM_BTN_W;
        StationPaint.button(g, okX, btnY2, CONFIRM_BTN_W, CONFIRM_BTN_H, true, false);
        String ok = "确认卸下";
        g.text(this.font, Component.literal(ok), okX + (CONFIRM_BTN_W - this.font.width(ok)) / 2,
                btnY2 + 4, StationPaint.WARN_AMBER);
        StationPaint.button(g, cancelX, btnY2, CONFIRM_BTN_W, CONFIRM_BTN_H, true, false);
        String cancel = "取消";
        g.text(this.font, Component.literal(cancel),
                cancelX + (CONFIRM_BTN_W - this.font.width(cancel)) / 2, btnY2 + 4,
                StationPaint.TEXT_PRIMARY);
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
            case "shield" -> "护盾量";
            case "shield_regen" -> "护盾恢复";
            default -> key;
        };
    }

    private static String formatStat(String key, double value) {
        return switch (key) {
            case "time_slow" -> Math.round(value * 100) + "%";
            case "duration", "cooldown" -> trim(value) + "s";
            case "ram", "buffer", "slots", "tier", "ultimate_cost", "stealth_cost" -> trim(value);
            case "ram_regen" -> "+" + trim(value) + "/min";
            case "shield" -> "+" + trim(value);
            case "shield_regen" -> "+" + trim(value) + "/s";
            case "max_health" -> "+" + trim(value);
            default -> (value >= 0 ? "+" : "") + trim(value) + "%";
        };
    }

    private static String trim(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.valueOf(value);
    }
}
