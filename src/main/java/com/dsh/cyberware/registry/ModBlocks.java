package com.dsh.cyberware.registry;

import com.dsh.cyberware.Cyberware;
import com.dsh.cyberware.block.CyberwareStationBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/** 方块注册表（Fabric：Registry.register 直注册，无延迟注册）。 */
public final class ModBlocks {

    /**
     * 义体操作台。
     * 需求书【一】要求：不可被活塞推动 → {@link PushReaction#BLOCK}。
     */
    public static RegistryHolder<CyberwareStationBlock> CYBERWARE_STATION;

    private ModBlocks() {
    }

    public static void register() {
        Identifier id = Identifier.fromNamespaceAndPath(Cyberware.MODID, "cyberware_station");
        // 26.1.2 起 BlockBehaviour.Properties 必须显式带 id（原版靠自己的注册表填，
        // Forge 系靠 ModBlocksBuilder 补；Fabric 手工注册要自己 setId，否则
        // effectiveDrops 里 Objects.requireNonNull(id) 直接 NPE）。
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);
        CYBERWARE_STATION = new RegistryHolder<>(Registry.register(
                BuiltInRegistries.BLOCK,
                id,
                new CyberwareStationBlock(BlockBehaviour.Properties.of()
                        .setId(key)
                        .mapColor(MapColor.COLOR_CYAN)
                        .strength(3.5F, 6.0F)
                        .sound(SoundType.METAL)
                        .noOcclusion()
                        .pushReaction(PushReaction.BLOCK))));
    }
}
