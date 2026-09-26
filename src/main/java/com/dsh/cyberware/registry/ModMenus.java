package com.dsh.cyberware.registry;

import com.dsh.cyberware.Cyberware;
import com.dsh.cyberware.menu.CyberwareStationMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 菜单注册表。 */
public final class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Cyberware.MODID);

    /** MenuType 的构造同样由 NeoForge 的 access transformer 开放。 */
    public static final DeferredHolder<MenuType<?>, MenuType<CyberwareStationMenu>> CYBERWARE_STATION =
            MENUS.register("cyberware_station", () -> new MenuType<>(
                    CyberwareStationMenu::new,
                    FeatureFlags.VANILLA_SET));

    private ModMenus() {
    }
}
