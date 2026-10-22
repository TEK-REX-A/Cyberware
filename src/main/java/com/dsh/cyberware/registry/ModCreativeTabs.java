package com.dsh.cyberware.registry;

import com.dsh.cyberware.Cyberware;
import com.dsh.cyberware.core.CyberwareDefinition;
import com.dsh.cyberware.core.CyberwareDefinitions;
import com.dsh.cyberware.data.CyberwareData;
import com.dsh.cyberware.item.CyberwareItem;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * 创造模式物品栏分类页。
 *
 * <p>Fabric 侧：{@code FabricCreativeModeTab.builder()}（返回原版 Builder）+ 直接注册到
 * {@code BuiltInRegistries.CREATIVE_MODE_TAB}。
 */
public final class ModCreativeTabs {

    public static RegistryHolder<CreativeModeTab> CYBERWARE;

    private ModCreativeTabs() {
    }

    public static void register() {
        ResourceKey<CreativeModeTab> key = ResourceKey.create(
                Registries.CREATIVE_MODE_TAB,
                Identifier.fromNamespaceAndPath(Cyberware.MODID, "cyberware"));

        CreativeModeTab tab = FabricCreativeModeTab.builder()
                .title(Component.translatable("itemGroup.cyberware"))
                .icon(() -> new ItemStack(ModItems.CYBERWARE_STATION.get()))
                .build();

        CYBERWARE = new RegistryHolder<>(Registry.register(
                BuiltInRegistries.CREATIVE_MODE_TAB, key, tab));

        // 26.1.2 的 displayItems 参数是 protected 的 CreativeModeTab.Output，
        // 跨包不可见；Fabric 的 modifyOutputEvent 给的是 public 的
        // FabricCreativeModeTabOutput（实现同一个接口），所以内容放这里填。
        CreativeModeTabEvents.modifyOutputEvent(key).register(output -> {
            output.accept(ModItems.CYBERWARE_STATION.get());
            for (CyberwareDefinition def : CyberwareDefinitions.all().values()) {
                RegistryHolder<CyberwareItem> holder = ModItems.CYBERWARE.get(def.id());
                if (holder == null) {
                    continue;
                }
                for (CyberwareDefinition.Variant variant : def.variants()) {
                    ItemStack stack = new ItemStack(holder.get());
                    stack.set(ModComponents.CYBERWARE_DATA.get(),
                            new CyberwareData(variant.rarity().ordinal(), 1));
                    output.accept(stack);
                }
            }
        });
    }
}
