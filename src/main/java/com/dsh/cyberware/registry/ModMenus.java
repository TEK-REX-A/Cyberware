package com.dsh.cyberware.registry;

import com.dsh.cyberware.Cyberware;
import com.dsh.cyberware.menu.CyberwareStationMenu;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

/**
 * 菜单注册表。
 *
 * <p>⚠ {@code new MenuType<>(MenuSupplier, FeatureFlagSet)} 同样依赖 NeoForge 的 AT 开放；
 * Fabric 侧是否可直接 new <b>未验证</b>。
 */
public final class ModMenus {

    public static RegistryHolder<MenuType<CyberwareStationMenu>> CYBERWARE_STATION;

    private ModMenus() {
    }

    public static void register() {
        CYBERWARE_STATION = new RegistryHolder<>(Registry.register(
                BuiltInRegistries.MENU,
                Identifier.fromNamespaceAndPath(Cyberware.MODID, "cyberware_station"),
                MenuTypes.create(CyberwareStationMenu::new, FeatureFlags.VANILLA_SET)));
    }
}
