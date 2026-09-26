package com.dsh.cyberware.block;

import com.dsh.cyberware.menu.CyberwareStationMenu;
import com.dsh.cyberware.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 义体操作台的方块实体。
 *
 * <p>持有一组「义体槽位」容器；菜单打开时把容器交给 {@link CyberwareStationMenu}。
 * 方块被破坏时方块实体一并消失，菜单里的 {@code stillValid} 随即失败，界面自动失效。
 */
public class CyberwareStationBlockEntity extends BlockEntity implements MenuProvider {

    /** 操作台自带的义体槽位。TODO(主人填写): 槽位数量与排布，当前 10 个（5×2） */
    private final SimpleContainer implantSlots = new SimpleContainer(CyberwareStationMenu.IMPLANT_SLOT_COUNT);

    public CyberwareStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CYBERWARE_STATION.get(), pos, state);
    }

    public SimpleContainer getImplantSlots() {
        return this.implantSlots;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.cyberware.cyberware_station");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CyberwareStationMenu(containerId, playerInventory, this);
    }
}
