package com.dsh.cyberware.menu;

import com.dsh.cyberware.Cyberware;
import com.dsh.cyberware.core.CyberwareDefinition;
import com.dsh.cyberware.core.CyberwareDefinitions;
import com.dsh.cyberware.core.CyberwareInstallation;
import com.dsh.cyberware.data.CyberwareData;
import com.dsh.cyberware.item.CyberwareItem;
import com.dsh.cyberware.registry.ModAttachments;
import com.dsh.cyberware.registry.ModComponents;
import com.dsh.cyberware.registry.ModItems;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * 义体装卸的<b>服务端权威</b>实现 —— 操作台按钮与网络包都只是「请求」，
 * 真正的校验与数据变更全部发生在这里，而且只在服务端发生。
 *
 * <p>为什么单独一个类：{@link CyberwareStationMenu#clickMenuButton} 与
 * {@code CyberwareNetwork} 是两条不同的入口（原版按钮包 / 自定义 payload），
 * 两者的规则必须一模一样 —— 复制粘贴两份规则迟早会分叉。
 *
 * <h3>数据流（安装）</h3>
 * <ol>
 *   <li>客户端：操作台界面 → {@code Minecraft.getInstance().gameMode
 *       .handleInventoryButtonClick(containerId, BUTTON_INSTALL_BASE + slot)}
 *       或者发 {@code CyberwareActionPayload.install(containerId, slot)}。</li>
 *   <li>服务端：{@link CyberwareStationMenu#clickMenuButton}（按钮路径）
 *       或 {@code CyberwareNetwork.handleAction}（payload 路径）→ 本类。</li>
 *   <li>本类：校验 → 扣物品 → 写 {@code ModAttachments.INSTALLATION} → 回执。</li>
 * </ol>
 *
 * <p>{@code AttachmentType.sync} 已配置，所以 {@code setData} 会由 NeoForge
 * 自动把新表同步给该玩家（以及注视他的客户端）—— 不需要额外补 S2C 包。
 */
public final class CyberwareStationService {

    private CyberwareStationService() {
    }

    // ------------------------------------------------------------------
    // 安装
    // ------------------------------------------------------------------

    /**
     * 从操作台的义体槽里取一件装上。
     *
     * @param containerId 服务端当前打开的菜单 id；{@code < 0} 表示不校验
     * @param slotIndex   操作台义体槽下标（0..{@link CyberwareStationMenu#IMPLANT_SLOT_COUNT}-1）
     * @return 是否真的装上了
     */
    public static boolean install(ServerPlayer player, int containerId, int slotIndex) {
        CyberwareStationMenu menu = openStation(player, containerId);
        if (menu == null) {
            return fail(player, "操作台没有打开，无法安装");
        }
        if (slotIndex < 0 || slotIndex >= CyberwareStationMenu.IMPLANT_SLOT_COUNT) {
            return fail(player, "义体槽下标越界：" + slotIndex);
        }

        Container container = menu.getImplantContainer();
        if (slotIndex >= container.getContainerSize()) {
            return fail(player, "义体槽不存在：" + slotIndex);
        }
        ItemStack stack = container.getItem(slotIndex);
        if (stack.isEmpty()) {
            return fail(player, "第 " + (slotIndex + 1) + " 个义体槽是空的");
        }
        if (!(stack.getItem() instanceof CyberwareItem item)) {
            return fail(player, "这个物品不是义体，装不了");
        }

        CyberwareDefinition def = item.definition();
        CyberwareData data = CyberwareItem.dataOf(stack);
        CyberwareInstallation current = CyberwareInstallation.of(player);

        String problem = validateInstall(def, data, current);
        if (problem != null) {
            return fail(player, problem);
        }

        CyberwareDefinition.Variant variant = def.variantFor(data.rarity());

        // ---- 真正落地：先写附件（触发同步），再扣物品 ----
        player.setData(ModAttachments.INSTALLATION.get(), current.with(def.id(), data));
        container.removeItem(slotIndex, 1);
        container.setChanged();
        menu.broadcastChanges();

        Cyberware.LOGGER.debug("[cyberware] {} 植入 {}（{} / 容量 {}）", player.getName().getString(),
                def.id(), data.rarity(), variant == null ? "?" : variant.capacity());
        return ok(player, "已植入 " + def.displayName() + "（" + data.rarity().displayName() + "）· 占用容量 "
                + (variant == null ? "?" : variant.capacity()) + " · 已用 "
                + CyberwareInstallation.usedCapacity(player) + "/" + limitText());
    }

    /**
     * 纯函数版校验 —— 不改任何状态，UI 想提前把按钮点灰 / 提示原因也能直接调它。
     *
     * @return {@code null} 表示可以安装；否则是给玩家看的中文原因
     */
    public static String validateInstall(CyberwareDefinition def, CyberwareData data, CyberwareInstallation current) {
        if (def == null) {
            return "未知型号：定义表里找不到这件义体（可能是旧存档里已删除的型号）";
        }
        CyberwareData safe = data == null ? CyberwareData.DEFAULT : data.safe();
        CyberwareDefinition.Variant variant = def.variantFor(safe.rarity());
        if (variant == null) {
            return "「" + def.displayName() + "」没有「" + safe.rarity().displayName() + "」这一档，装不了";
        }
        if (current.has(def.id())) {
            return "身上已经装了「" + def.displayName() + "」，同型号不能重复";
        }

        // 冲突规则一：同一分类槽位的件数上限（CyberwareSlot.maxCount）
        int slotMax = Math.max(1, def.slot().maxCount());
        int slotUsed = 0;
        for (String id : current.installed().keySet()) {
            CyberwareDefinition other = CyberwareDefinitions.byId(id);
            if (other != null && other.slot() == def.slot()) {
                slotUsed++;
            }
        }
        if (slotUsed >= slotMax) {
            return def.slot().displayName() + " 槽位已满（" + slotUsed + "/" + slotMax + "）";
        }

        // 冲突规则二：玩家总容量上限
        int limit = CyberwareInstallation.capacityLimit();
        int used = current.usedCapacity();
        if (used + variant.capacity() > limit) {
            return "植入容量不足：需要 " + variant.capacity() + "，只剩 " + Math.max(0, limit - used)
                    + "（上限 " + limit + "）";
        }
        return null;
    }

    // ------------------------------------------------------------------
    // 卸载
    // ------------------------------------------------------------------

    /**
     * 卸下一件义体并把物品还给玩家。
     *
     * @param containerId 服务端当前打开的菜单 id；{@code < 0} 表示不校验
     * @param defId       要卸下的型号 id；留空则用 {@code fallbackIndex}
     * @param fallbackIndex 当 {@code defId} 为空时，按 {@link CyberwareInstallation#orderedIds()}
     *                      的确定性顺序取第 N 件（为了让「按钮只有 int id」那条路径也能卸载）
     */
    public static boolean uninstall(ServerPlayer player, int containerId, String defId, int fallbackIndex) {
        CyberwareStationMenu menu = openStation(player, containerId);
        if (menu == null) {
            return fail(player, "操作台没有打开，无法卸载");
        }

        CyberwareInstallation current = CyberwareInstallation.of(player);
        String targetId = defId;
        if (targetId == null || targetId.isBlank()) {
            List<String> ids = current.orderedIds();
            if (fallbackIndex < 0 || fallbackIndex >= ids.size()) {
                return fail(player, "没有可卸载的义体（或下标越界：" + fallbackIndex + "）");
            }
            targetId = ids.get(fallbackIndex);
        }

        CyberwareData data = current.dataOf(targetId);
        if (data == null) {
            return fail(player, "身上没有装「" + displayNameOf(targetId) + "」");
        }

        // ---- 先清表，再归还物品（清表失败等于没卸，物品必须跟着表走）----
        player.setData(ModAttachments.INSTALLATION.get(), current.without(targetId));

        CyberwareDefinition def = CyberwareDefinitions.byId(targetId);
        String name = def == null ? targetId : def.displayName();
        ItemStack returned = buildReturnStack(targetId, data);
        String where;
        if (returned.isEmpty()) {
            where = "；该型号已从定义表移除，只清掉了记录";
        } else {
            where = "，物品已放入" + give(menu, player, returned);
        }

        Cyberware.LOGGER.debug("[cyberware] {} 卸下 {}（剩余容量 {}/{}）", player.getName().getString(),
                targetId, CyberwareInstallation.usedCapacity(player), limitText());
        return ok(player, "已卸下 " + name + " · 已用 " + CyberwareInstallation.usedCapacity(player)
                + "/" + limitText() + where);
    }

    // ------------------------------------------------------------------
    // 升级（框架占位）
    // ------------------------------------------------------------------

    /**
     * 升级按钮。
     *
     * <p>需求书【六.6】要求「校验材料 → 按配置成功率判定 → 提升稀有度」，
     * 材料扣除与成功率属于第六步；这里只把入口接上并明确回执，
     * 不再像以前那样静默返回 {@code true}（静默成功会让玩家以为升级生效了）。
     */
    public static boolean upgrade(ServerPlayer player, int containerId, int slotIndex) {
        if (openStation(player, containerId) == null) {
            return fail(player, "操作台没有打开，无法升级");
        }
        return fail(player, "升级尚未接线：材料扣除与成功率判定还没实现（见 INSTALL-WIRING.md）");
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    /**
     * 取服务端当前打开的操作台菜单。
     *
     * <p>这里同时是「所有数据变更只在服务端」的一道闸：客户端本地菜单（SimpleContainer 版）
     * 永远不会通过 {@code stillValid} + {@link ServerPlayer} 这两道检查。
     */
    private static CyberwareStationMenu openStation(ServerPlayer player, int containerId) {
        if (player.containerMenu instanceof CyberwareStationMenu menu
                && (containerId < 0 || menu.containerId == containerId)
                && menu.stillValid(player)) {
            return menu;
        }
        return null;
    }

    /** 按型号 id 与数据重造一件义体物品；定义表里已经没有该型号时返回空栈。 */
    private static ItemStack buildReturnStack(String defId, CyberwareData data) {
        DeferredItem<CyberwareItem> item = ModItems.CYBERWARE.get(defId);
        if (item == null) {
            Cyberware.LOGGER.warn("[cyberware] 卸载 {}：物品注册表里没有这个型号，物品无法归还", defId);
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item.get(), 1);
        stack.set(ModComponents.CYBERWARE_DATA.get(), data.safe());
        return stack;
    }

    /**
     * 把归还的物品放进玩家能看到/拿到的地方：操作台空槽 → 背包 → 脚边。
     *
     * <p>操作台优先是因为那个界面里只画了 10 个义体槽（背包格被移出可视区域，
     * 见 {@code CyberwareStationMenu.INVENTORY_X}），放进背包玩家会以为物品丢了。
     */
    private static String give(CyberwareStationMenu menu, ServerPlayer player, ItemStack stack) {
        Container container = menu.getImplantContainer();
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (container.getItem(i).isEmpty()) {
                container.setItem(i, stack);
                container.setChanged();
                menu.broadcastChanges();
                return "操作台第 " + (i + 1) + " 个槽";
            }
        }
        if (player.getInventory().add(stack)) {
            return "背包";
        }
        player.drop(stack, false);
        return "脚边（操作台与背包都满了）";
    }

    private static String displayNameOf(String defId) {
        CyberwareDefinition def = CyberwareDefinitions.byId(defId);
        return def == null ? defId : def.displayName();
    }

    private static String limitText() {
        int limit = CyberwareInstallation.capacityLimit();
        return limit == Integer.MAX_VALUE ? "∞" : String.valueOf(limit);
    }

    private static boolean fail(ServerPlayer player, String reason) {
        Cyberware.LOGGER.debug("[cyberware] 操作被拒绝（{}）：{}", player.getName().getString(), reason);
        player.sendOverlayMessage(Component.literal("§c[义体] " + reason));
        return false;
    }

    private static boolean ok(ServerPlayer player, String message) {
        player.sendOverlayMessage(Component.literal("§b[义体] " + message));
        return true;
    }
}
