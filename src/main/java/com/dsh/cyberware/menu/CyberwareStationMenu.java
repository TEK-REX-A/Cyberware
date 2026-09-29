package com.dsh.cyberware.menu;

import com.dsh.cyberware.block.CyberwareStationBlockEntity;
import com.dsh.cyberware.registry.ModComponents;
import com.dsh.cyberware.registry.ModMenus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 义体操作台的容器菜单。
 *
 * <p>需求书【一】的界面要求在这里落地：左侧义体槽（详情面板在 Screen 里画）、
 * 离开 3 格自动关闭（{@link #stillValid}）、升级按钮走 {@link #clickMenuButton}。
 */
public class CyberwareStationMenu extends AbstractContainerMenu {

    /** 义体槽位数量。TODO(主人填写): 与界面排布一起确认 */
    public static final int IMPLANT_SLOT_COUNT = 10;

    /** 离开操作台的最大距离平方（3 格 = 9.0） */
    public static final double MAX_DISTANCE_SQR = 9.0D;

    /** 升级按钮的 id（需求书【六.6】要求在 clickMenuButton 里处理） */
    public static final int BUTTON_UPGRADE = 0;

    /**
     * 安装按钮 id：{@code BUTTON_INSTALL_BASE + 操作台义体槽下标}。
     *
     * <p>原版按钮包（{@code ServerboundContainerButtonClickPacket}）只能带一个 int，
     * 所以「装第几个槽」直接编进 id 里。
     */
    public static final int BUTTON_INSTALL_BASE = BUTTON_UPGRADE + 1;

    /**
     * 卸载按钮 id：{@code BUTTON_UNINSTALL_BASE + 已装义体下标}。
     *
     * <p>下标走 {@link com.dsh.cyberware.core.CyberwareInstallation#orderedIds()} 的确定性顺序
     * （按 id 字典序），这样两端算出来的「第 N 件」一定是同一件。
     */
    public static final int BUTTON_UNINSTALL_BASE = BUTTON_INSTALL_BASE + IMPLANT_SLOT_COUNT;

    /** 卸载按钮最多支持这么多件（与义体槽数量一致，够用又不会无限膨胀）。 */
    public static final int MAX_BUTTON_UNINSTALL = IMPLANT_SLOT_COUNT;

    // 布局对齐 CyberwareStationScreen：义体槽排在面板底部的一条里
    private static final int IMPLANT_COLS = 10;
    private static final int IMPLANT_X = 70;
    private static final int IMPLANT_Y = 184;
    /**
     * 玩家背包整体移出可视区域。
     *
     * <p>操作台不是箱子 —— 把 27+9 个背包格塞进面板只会把义体列表挤没。
     * 槽位仍在菜单里（shift 点击、快速移动照常工作），只是不画出来。
     */
    private static final int INVENTORY_X = -3000;
    private static final int INVENTORY_Y = -3000;

    private final Container implantContainer;
    private final ContainerLevelAccess access;

    /** 客户端构造：MenuType 的 MenuSupplier 会走这个。 */
    public CyberwareStationMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(IMPLANT_SLOT_COUNT), ContainerLevelAccess.NULL);
    }

    /** 服务端构造：方块实体打开界面时走这个。 */
    public CyberwareStationMenu(int containerId, Inventory playerInventory, CyberwareStationBlockEntity station) {
        this(containerId, playerInventory, station.getImplantSlots(),
                ContainerLevelAccess.create(station.getLevel(), station.getBlockPos()));
    }

    private CyberwareStationMenu(int containerId, Inventory playerInventory, Container implantContainer,
                                 ContainerLevelAccess access) {
        super(ModMenus.CYBERWARE_STATION.get(), containerId);
        checkContainerSize(implantContainer, IMPLANT_SLOT_COUNT);
        this.implantContainer = implantContainer;
        this.access = access;

        // 义体槽：只允许放带 cyberware_data 组件的物品
        for (int i = 0; i < IMPLANT_SLOT_COUNT; i++) {
            int x = IMPLANT_X + (i % IMPLANT_COLS) * 18;
            int y = IMPLANT_Y + (i / IMPLANT_COLS) * 18;
            this.addSlot(new Slot(implantContainer, i, x, y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    // 26.x 已移除 ItemStack#has(DataComponentType)，改用 DataComponentGetter#get
                    return stack.get(ModComponents.CYBERWARE_DATA.get()) != null;
                }
            });
        }

        // 玩家背包 + 快捷栏
        this.addStandardInventorySlots(playerInventory, INVENTORY_X, INVENTORY_Y);
    }

    public Container getImplantContainer() {
        return this.implantContainer;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < IMPLANT_SLOT_COUNT) {
            if (!this.moveItemStackTo(stack, IMPLANT_SLOT_COUNT, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, 0, IMPLANT_SLOT_COUNT, false)) {
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
        if (id >= BUTTON_INSTALL_BASE && id < BUTTON_INSTALL_BASE + IMPLANT_SLOT_COUNT) {
            return CyberwareStationService.install(serverPlayer, this.containerId, id - BUTTON_INSTALL_BASE);
        }
        if (id >= BUTTON_UNINSTALL_BASE && id < BUTTON_UNINSTALL_BASE + MAX_BUTTON_UNINSTALL) {
            // 按钮只有 int：卸载走 orderedIds() 的确定性下标
            return CyberwareStationService.uninstall(serverPlayer, this.containerId, "",
                    id - BUTTON_UNINSTALL_BASE);
        }
        return false;
    }
}
