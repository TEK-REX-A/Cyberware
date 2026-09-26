package com.dsh.cyberware.registry;

import com.dsh.cyberware.Cyberware;
import com.dsh.cyberware.core.CyberwareDefinition;
import com.dsh.cyberware.core.CyberwareDefinitions;
import com.dsh.cyberware.item.CyberwareItem;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 物品注册表。 */
public final class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Cyberware.MODID);

    /** 义体操作台 */
    public static final DeferredItem<BlockItem> CYBERWARE_STATION =
            ITEMS.registerSimpleBlockItem("cyberware_station", ModBlocks.CYBERWARE_STATION);

    /**
     * 全部义体物品，按型号 id 索引。
     *
     * <p>不手写 25 条注册 —— 直接从 {@link CyberwareDefinitions} 生成，
     * 以后往定义表里加型号，物品会自动出现。
     */
    public static final Map<String, DeferredItem<CyberwareItem>> CYBERWARE;

    static {
        Map<String, DeferredItem<CyberwareItem>> map = new LinkedHashMap<>();
        for (CyberwareDefinition def : CyberwareDefinitions.all().values()) {
            DeferredItem<CyberwareItem> item = ITEMS.registerItem(
                    def.id(),
                    props -> new CyberwareItem(props, def.id()),
                    props -> props.stacksTo(1));
            map.put(def.id(), item);
        }
        CYBERWARE = Collections.unmodifiableMap(map);
    }

    private ModItems() {
    }
}
