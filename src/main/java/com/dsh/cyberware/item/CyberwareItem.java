package com.dsh.cyberware.item;

import com.dsh.cyberware.core.CyberwareDefinition;
import com.dsh.cyberware.core.CyberwareDefinitions;
import com.dsh.cyberware.data.CyberwareData;
import com.dsh.cyberware.registry.ModComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 义体物品。
 *
 * <p>同一个型号只注册**一个**物品；稀有度与等级存在 {@link CyberwareData} 数据组件里，
 * 所以「普通泽塔」和「史诗泽塔」是同一个物品的两个实例。
 */
public class CyberwareItem extends Item {

    private final String cyberwareId;

    public CyberwareItem(Properties properties, String cyberwareId) {
        super(properties);
        this.cyberwareId = cyberwareId;
    }

    public String cyberwareId() {
        return this.cyberwareId;
    }

    /** 对应的型号定义；找不到返回 null（防御性）。 */
    public CyberwareDefinition definition() {
        return CyberwareDefinitions.byId(this.cyberwareId);
    }

    /** 读取物品上的义体数据；组件缺失或非法时回落默认值（旧存档防御）。 */
    public static CyberwareData dataOf(ItemStack stack) {
        CyberwareData data = stack.get(ModComponents.CYBERWARE_DATA.get());
        return data == null ? CyberwareData.DEFAULT : data.safe();
    }
}
