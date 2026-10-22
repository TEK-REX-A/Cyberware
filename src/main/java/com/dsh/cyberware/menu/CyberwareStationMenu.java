package com.dsh.cyberware.menu;

import com.dsh.cyberware.block.CyberwareStationBlockEntity;
import com.dsh.cyberware.item.CyberwareItem;
import com.dsh.cyberware.registry.ModMenus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

/**
 * 义体操作台的容器菜单。
 *
 * <p><b>v0.5.4 起取消「送料槽」</b>：安装材料直接从 {@code player.getInventory()} 里找并扣掉，
 * 不再需要「背包 → 拖进送料槽 → 点安装」这一圈（见 {@link CyberwareStationService#install}）。
 * 菜单里因此<b>没有义体容器槽</b>，只剩 36 个玩家背包格（坐标被移出可视区）。
 *
 * <p>聊天栏里保留的旧需求：离开 3 格自动关闭（{@link #stillValid}）、
 * 升级按钮走 {@link #clickMenuButton}。
 */
public class CyberwareStationMenu extends AbstractContainerMenu {

    /**
     * 快捷栏格数。
     *
     * <p>只用于「原版按钮包」那条退化路径：{@code clickMenuButton} 只能带一个 int，
     * 所以 {@code BUTTON_INSTALL_BASE + n}（n = 快捷栏 0..8）表示
     * 「安装快捷栏第 n 格里的义体」。模组自己的界面走
     * {@link com.dsh.cyberware.network.CyberwareActionPayload#install(int, String)}（带型号 id），
     * 不需要这条限制。
     */
    public static final int INSTALL_HOTBAR_COUNT = 9;

    /** 离开操作台的最大距离平方（3 格 = 9.0） */
    public static final double MAX_DISTANCE_SQR = 9.0D;

    /** 升级按钮的 id（需求书【六.6】要求在 clickMenuButton 里处理） */
    public static final int BUTTON_UPGRADE = 0;

    /** 安装按钮 id：{@code BUTTON_INSTALL_BASE + 快捷栏下标 0..8}。 */
    public static final int BUTTON_INSTALL_BASE = BUTTON_UPGRADE + 1;

    /**
     * 卸载按钮 id：{@code BUTTON_UNINSTALL_BASE + 已装义体下标}。
     *
     * <p>下标走 {@link com.dsh.cyberware.core.CyberwareInstallation#orderedIds()} 的确定性顺序
     * （按 id 字典序），这样两端算出来的「第 N 件」一定是同一件。
     */
    public static final int BUTTON_UNINSTALL_BASE = BUTTON_INSTALL_BASE + INSTALL_HOTBAR_COUNT;

    /** 卸载按钮最多支持这么多件（与已装上限一致，够用又不会无限膨胀）。 */
    public static final int MAX_BUTTON_UNINSTALL = 10;

    /**
     * 玩家背包整体移出可视区域。
     *
     * <p>操作台不是箱子 —— 把 27+9 个背包格塞进面板只会把义体列表挤没。
     * 槽位仍在菜单里（shift 点击、快速移动照常工作），只是不画出来；
     * 快捷栏那 9 格由界面挪到底部（见 {@code CyberwareStationScreen#revealHotbar}）。
     */
    private static final int INVENTORY_X = -3000;
    private static final int INVENTORY_Y = -3000;

    /** 背包格数：27 主背包 + 9 快捷栏。 */
    private static final int MAIN_SLOTS = 27;
    private static final int TOTAL_SLOTS = 36;

    private final ContainerLevelAccess access;

    /** 客户端构造：MenuType 的 MenuSupplier 会走这个。 */
    public CyberwareStationMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    /** 服务端构造：方块实体打开界面时走这个。 */
    public CyberwareStationMenu(int containerId, Inventory playerInventory, CyberwareStationBlockEntity station) {
        this(containerId, playerInventory,
                ContainerLevelAccess.create(station.getLevel(), station.getBlockPos()));
    }

    private CyberwareStationMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(ModMenus.CYBERWARE_STATION.get(), containerId);
        this.access = access;

        // 只有玩家背包（主背包 0..26 / 快捷栏 27..35），没有任何自有容器槽 —— 见类注释
        this.addStandardInventorySlots(playerInventory, INVENTORY_X, INVENTORY_Y);
    }

    /**
     * Shift 点击：只在 27 主背包与 9 快捷栏之间搬。
     *
     * <p>没有自有容器了，所以这里就是原版 {@code InventoryMenu} 的同一套规则。
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= this.slots.size()) {
            return ItemStack.EMPTY;
        }
        var slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved = index < MAIN_SLOTS
                ? this.moveItemStackTo(stack, MAIN_SLOTS, TOTAL_SLOTS, false)
                : this.moveItemStackTo(stack, 0, MAIN_SLOTS, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    /**
     * 需求书【一】：玩家离开操作台超过 3 格，UI 自动关闭。
     * 原版默认是 8 格，这里收紧到 3 格。
     */
    @Override
    public boolean stillValid(Player player) {
        return this.access.evaluate((level, pos) -> player.distanceToSqr(
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= MAX_DISTANCE_SQR, true);
    }

    /**
     * 原版按钮路径：客户端 {@code gameMode.handleInventoryButtonClick(containerId, id)}
     * → 服务端本方法。安装/卸载都只是「请求」，真正的校验与数据变更在
     * {@link CyberwareStationService} 里。
     *
     * <p>26.x 里这个方法<b>只在服务端被调用</b>（{@code ServerGamePacketListenerImpl
     * #handleContainerButtonClick}）；这里再挡一道 {@code ServerPlayer}，
     * 保证客户端本地菜单永远不可能改到玩家数据。
     */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        if (id == BUTTON_UPGRADE) {
            return CyberwareStationService.upgrade(serverPlayer, this.containerId, -1);
        }
        if (id >= BUTTON_INSTALL_BASE && id < BUTTON_INSTALL_BASE + INSTALL_HOTBAR_COUNT) {
            // 按钮只有 int：这条路径退化成「安装快捷栏第 N 格的义体」
            String defId = hotbarCyberwareId(player, id - BUTTON_INSTALL_BASE);
            return CyberwareStationService.install(serverPlayer, this.containerId, defId);
        }
        if (id >= BUTTON_UNINSTALL_BASE && id < BUTTON_UNINSTALL_BASE + MAX_BUTTON_UNINSTALL) {
            // 按钮只有 int：卸载走 orderedIds() 的确定性下标
            return CyberwareStationService.uninstall(serverPlayer, this.containerId, "",
                    id - BUTTON_UNINSTALL_BASE);
        }
        return false;
    }

    /** 快捷栏第 {@code hotbarIndex} 格里的义体型号 id；不是义体则返回 null。 */
    private static String hotbarCyberwareId(Player player, int hotbarIndex) {
        if (hotbarIndex < 0 || hotbarIndex >= INSTALL_HOTBAR_COUNT) {
            return null;
        }
        return player.getInventory().getItem(hotbarIndex).getItem() instanceof CyberwareItem item
                ? item.cyberwareId()
                : null;
    }
}

