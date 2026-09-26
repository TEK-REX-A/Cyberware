package com.dsh.cyberware.registry;

import com.dsh.cyberware.Cyberware;
import com.dsh.cyberware.data.CyberwareData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 数据组件注册。义体的稀有度/等级通过这个组件挂在物品栈上。 */
public final class ModComponents {

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Cyberware.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CyberwareData>> CYBERWARE_DATA =
            COMPONENTS.registerComponentType("cyberware_data", builder -> builder
                    .persistent(CyberwareData.CODEC)
                    .networkSynchronized(CyberwareData.STREAM_CODEC));

    private ModComponents() {
    }
}
