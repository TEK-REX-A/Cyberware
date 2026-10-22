package com.dsh.cyberware.registry;

import com.dsh.cyberware.Cyberware;
import com.dsh.cyberware.core.CyberwareDefinition;
import com.dsh.cyberware.core.CyberwareDefinitions;
import com.dsh.cyberware.item.CyberwareItem;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

/** 物品注册表（Fabric：Registry.register 直注册）。 */
public final class ModItems {

    /** 义体操作台 */
    public static RegistryHolder<BlockItem> CYBERWARE_STATION;

    /**
     * 全部义体物品，按型号 id 索引。
     *
     * <p>不手写几十条注册 —— 直接从 {@link CyberwareDefinitions} 生成，
     * 以后往定义表里加型号，物品会自动出现。
     */
    public static Map<String, RegistryHolder<CyberwareItem>> CYBERWARE;

    private ModItems() {
    }

    public static void register() {
        // 26.1.2：Item.Properties 和 Block.Properties 一样强制要求 id，
        // Fabric 手工注册不会替你补，必须自己 setId（否则构造期 NPE）。
        Identifier stationId = Identifier.fromNamespaceAndPath(Cyberware.MODID, "cyberware_station");
        CYBERWARE_STATION = new RegistryHolder<>(Registry.register(
                BuiltInRegistries.ITEM,
                stationId,
                new BlockItem(ModBlocks.CYBERWARE_STATION.get(),
                        new Item.Properties().setId(ResourceKey.create(Registries.ITEM, stationId)))));

        Map<String, RegistryHolder<CyberwareItem>> map = new LinkedHashMap<>();
        for (CyberwareDefinition def : CyberwareDefinitions.all().values()) {
            Identifier itemId = Identifier.fromNamespaceAndPath(Cyberware.MODID, def.id());
            CyberwareItem item = Registry.register(
                    BuiltInRegistries.ITEM,
                    itemId,
                    new CyberwareItem(new Item.Properties()
                            .setId(ResourceKey.create(Registries.ITEM, itemId))
                            .stacksTo(1), def.id()));
            map.put(def.id(), new RegistryHolder<>(item));
        }
        CYBERWARE = Collections.unmodifiableMap(map);
    }
}
