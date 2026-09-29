package com.dsh.cyberware.block;

import com.dsh.cyberware.menu.CyberwareStationMenu;
import com.dsh.cyberware.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
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

    /**
     * 方块被破坏时把义体槽里的东西吐出来。
     *
     * <p>原版 {@link BlockEntity#preRemoveSideEffects} 只会自动处理
     * 「方块实体自己就是 {@code Container}」的情况，本 BE 是**持有**一个容器，
     * 不覆盖这条路的话，玩家放在槽里的义体（以及卸载归还的物品）会随方块一起蒸发。
     * 卸载优先把物品还到这些槽位，所以这个覆盖是必要的配套。
     */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level != null && !this.level.isClientSide()) {
            Containers.dropContents(this.level, pos, this.implantSlots);
        }
    }
}
