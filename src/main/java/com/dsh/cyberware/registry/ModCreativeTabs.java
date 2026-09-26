package com.dsh.cyberware.registry;

import com.dsh.cyberware.Cyberware;
import com.dsh.cyberware.core.CyberwareDefinition;
import com.dsh.cyberware.core.CyberwareDefinitions;
import com.dsh.cyberware.data.CyberwareData;
import com.dsh.cyberware.item.CyberwareItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 创造模式物品栏分类页。
 *
 * <p>光注册物品是不够的 —— 物品只有被某个 {@link CreativeModeTab}「显示」出来，
 * 才会出现在创造物品栏里。这里建一个属于本模组的「义体」分类页。
 */
public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Cyberware.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CYBERWARE =
            TABS.register("cyberware", () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                    .title(Component.translatable("itemGroup.cyberware"))
                    .icon(() -> new ItemStack(ModItems.CYBERWARE_STATION.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.CYBERWARE_STATION.get());
                        // 每个型号的每个稀有度变体各摆一份，方便你直接拿史诗/神话来试
                        for (CyberwareDefinition def : CyberwareDefinitions.all().values()) {
                            DeferredItem<CyberwareItem> item = ModItems.CYBERWARE.get(def.id());
                            if (item == null) {
                                continue;
                            }
                            for (CyberwareDefinition.Variant variant : def.variants()) {
                                ItemStack stack = new ItemStack(item.get());
                                stack.set(ModComponents.CYBERWARE_DATA.get(),
                                        new CyberwareData(variant.rarity().ordinal(), 1));
                                output.accept(stack);
                            }
                        }
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
