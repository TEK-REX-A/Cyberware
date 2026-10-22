package com.dsh.cyberware.menu;

import com.dsh.cyberware.core.Attachments;
import com.dsh.cyberware.Cyberware;
import com.dsh.cyberware.core.CyberwareDefinition;
import com.dsh.cyberware.core.CyberwareDefinitions;
import com.dsh.cyberware.core.CyberwareInstallation;
import com.dsh.cyberware.data.CyberwareData;
import com.dsh.cyberware.item.CyberwareItem;
import com.dsh.cyberware.network.CyberwareNetwork;
import com.dsh.cyberware.registry.ModAttachments;
import com.dsh.cyberware.registry.ModComponents;
import com.dsh.cyberware.registry.ModItems;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.dsh.cyberware.registry.RegistryHolder;

/**
 * 义体装卸的<b>服务端权威</b>实现 —— 操作台按钮与网络包都只是「请求」，
 * 真正的校验与数据变更全部发生在这里，而且只在服务端发生。
 *
 * <p>为什么单独一个类：{@link CyberwareStationMenu#clickMenuButton} 与
 * {@code CyberwareNetwork} 是两条不同的入口（原版按钮包 / 自定义 payload），
 * 两者的规则必须一模一样 —— 复制粘贴两份规则迟早会分叉。
 *
 * <h3>数据流（安装，v0.5.4 起没有送料槽）</h3>
 * <ol>
 *   <li>客户端：操作台界面点「安装」→
 *       {@code CyberwareActionPayload.install(containerId, defId)}（带型号 id）。</li>
 *   <li>服务端：{@code CyberwareNetwork.handleAction} → 本类；原版按钮路径走
 *       {@link CyberwareStationMenu#clickMenuButton} 后同样进本类。</li>
 *   <li>本类：校验 → <b>从 {@code player.getInventory()} 里找到并扣掉 1 件</b>
 *       → 写 {@code ModAttachments.INSTALLATION} → {@link CyberwareNetwork#syncInstallation}
 *       把新表推给该玩家的客户端 → 回执。</li>
 * </ol>
 *
 * <p>⚠ <b>「吞义体」的教训（0.5.3 真机）</b>：安装成功但界面/轮盘读不到，
 * 根因不是写入失败，而是 <b>附件没有同步到客户端</b>
 * （{@code ModAttachments.INSTALLATION} 故意没有 {@code .syncWith(...)}，
 * 见该类注释：Fabric 的附件同步在 Android 上会炸）。所以每次改动权威数据之后
 * <b>必须</b>显式 {@link CyberwareNetwork#syncInstallation}，否则客户端
 * {@code CyberwareInstallation.of(player)} 永远是空表。
 */
public final class CyberwareStationService {

    private CyberwareStationService() {
    }

    // ------------------------------------------------------------------
    // 安装
    // ------------------------------------------------------------------

    /**
     * 从玩家背包里取一件义体装上（v0.5.4 起取消了操作台送料槽）。
     *
     * @param containerId 服务端当前打开的菜单 id；{@code < 0} 表示不校验
     * @param defId       要安装的型号 id（界面点「安装」时选中的那件）
     * @return 是否真的装上了
     */
    public static boolean install(ServerPlayer player, int containerId, String defId) {
        CyberwareStationMenu menu = openStation(player, containerId);
        if (menu == null) {
            return fail(player, "操作台没有打开，无法安装");
        }
        if (defId == null || defId.isBlank()) {
            return fail(player, "没有指定要安装的义体");
        }

        int found = findInventorySlot(player, defId);
        if (found < 0) {
            return fail(player, "背包里没有「" + displayNameOf(defId) + "」");
        }
        ItemStack stack = player.getInventory().getItem(found);
        if (!(stack.getItem() instanceof CyberwareItem item)) {
            return fail(player, "这个物品不是义体，装不了");
        }

        CyberwareDefinition def = item.definition();
        if (def == null) {
            return fail(player, "未知型号：定义表里找不到这件义体（可能是旧存档里已删除的型号）");
        }
        CyberwareData data = CyberwareItem.dataOf(stack);
        CyberwareInstallation current = CyberwareInstallation.of(player);

        String problem = validateInstall(def, data, current);
        if (problem != null) {
            return fail(player, problem);
        }

        CyberwareDefinition.Variant variant = def.variantFor(data.rarity());

        // ---- 真正落地：先写附件（权威数据），再扣物品，最后立刻同步给客户端 ----
        Attachments.set(Attachments.of(player), ModAttachments.INSTALLATION.get(), current.with(def.id(), data));
        player.getInventory().removeItem(found, 1);
        player.getInventory().setChanged();
        menu.broadcastChanges();
        // ★ 关键一步：附件不会自动同步（见类注释），少了这一行就是「吞义体」
        CyberwareNetwork.syncInstallation(player);

        Cyberware.LOGGER.debug("[cyberware] {} 植入 {}（{} / 容量 {}，取自背包第 {} 格）",
                player.getName().getString(), def.id(), data.rarity(),
                variant == null ? "?" : variant.capacity(), found);
        return ok(player, "已植入 " + def.displayName() + "（" + data.rarity().displayName() + "）· 占用容量 "
                + (variant == null ? "?" : variant.capacity()) + " · 已用 "
                + CyberwareInstallation.usedCapacity(player) + "/" + limitText());
    }

    /**
     * 在玩家背包里找「型号 id 匹配」的义体，返回背包下标；没有返回 {@code -1}。
     *
     * <p><b>两端共用这一条规则</b>（界面预检也调它），否则会出现
     * 「界面说能装、服务端却扣了另一件」。
     *
     * <p>同型号有多件时<b>挑稀有度最低的那件</b>（最不吃亏）：玩家同时有普通与传说
     * 时不该默默把传说吃掉。
     */
    public static int findInventorySlot(Player player, String defId) {
        if (player == null || defId == null || defId.isBlank()) {
            return -1;
        }
        Inventory inventory = player.getInventory();
        int best = -1;
        int bestRarity = Integer.MAX_VALUE;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof CyberwareItem item)) {
                continue;
            }
            if (!defId.equals(item.cyberwareId())) {
                continue;
            }
            int rarity = CyberwareItem.dataOf(stack).rarityIndex();
            if (rarity < bestRarity) {
                bestRarity = rarity;
                best = i;
            }
        }
        return best;
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
        Attachments.set(Attachments.of(player), ModAttachments.INSTALLATION.get(), current.without(targetId));
        // ★ 与安装同理：权威数据变了就必须同步，否则客户端还显示「已装」
        CyberwareNetwork.syncInstallation(player);

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
     * <p>这里同时是「所有数据变更只在服务端」的一道闸：客户端本地菜单（Simplified 版）
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
        RegistryHolder<CyberwareItem> item = ModItems.CYBERWARE.get(defId);
        if (item == null) {
            Cyberware.LOGGER.warn("[cyberware] 卸载 {}：物品注册表里没有这个型号，物品无法归还", defId);
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item.get(), 1);
        stack.set(ModComponents.CYBERWARE_DATA.get(), data.safe());
        return stack;
    }

    /**
     * 把归还的物品放回玩家背包；背包满了就掉在脚边。
     *
     * <p>v0.5.4 起操作台没有送料槽了，所以这里只剩这一条路 —— 不再有
     * 「先塞回操作台槽位」的分支（那个分支会让物品卡在方块里）。
     */
    private static String give(CyberwareStationMenu menu, ServerPlayer player, ItemStack stack) {
        Inventory inventory = player.getInventory();
        if (inventory.add(stack)) {
            inventory.setChanged();
            menu.broadcastChanges();
            return "背包";
        }
        player.drop(stack, false);
        return "脚边（背包满了）";
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

