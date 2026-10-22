package com.dsh.cyberware.registry;

import com.dsh.cyberware.Cyberware;
import com.dsh.cyberware.data.CyberwareData;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/** 数据组件注册（组件本体用的是 26.1.2 原版 DataComponentType，Fabric 侧无需重写）。 */
public final class ModComponents {

    public static RegistryHolder<DataComponentType<CyberwareData>> CYBERWARE_DATA;

    private ModComponents() {
    }

    public static void register() {
        CYBERWARE_DATA = new RegistryHolder<>(Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                Identifier.fromNamespaceAndPath(Cyberware.MODID, "cyberware_data"),
                DataComponentType.<CyberwareData>builder()
                        .persistent(CyberwareData.CODEC)
                        .networkSynchronized(CyberwareData.STREAM_CODEC)
                        .build()));
    }
}
