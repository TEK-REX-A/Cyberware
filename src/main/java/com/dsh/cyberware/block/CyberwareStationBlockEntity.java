package com.dsh.cyberware.block;

import com.dsh.cyberware.menu.CyberwareStationMenu;
import com.dsh.cyberware.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 义体操作台的方块实体。
 *
 * <p><b>v0.5.4 起它不再持有任何容器</b>：安装材料直接从玩家背包里找并扣掉
 * （见 {@code CyberwareStationService#install}），所以「送料槽」那 10 个格子
 * 连同它的 {@code SimpleContainer} 一起删掉了。
 *
 * <p>它现在只做两件事：当 {@link MenuProvider}（右键开界面）、
 * 提供坐标给菜单的 {@code stillValid}。方块被破坏时菜单的 {@code stillValid}
 * 随即失败，界面自动关闭。
 */
public class CyberwareStationBlockEntity extends BlockEntity implements MenuProvider {

    public CyberwareStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CYBERWARE_STATION.get(), pos, state);
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

