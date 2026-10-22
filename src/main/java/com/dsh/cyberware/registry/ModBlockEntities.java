package com.dsh.cyberware.registry;

import com.dsh.cyberware.Cyberware;
import com.dsh.cyberware.block.CyberwareStationBlockEntity;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

/**
 * 方块实体注册表。
 *
 * <p>⚠ {@code new BlockEntityType<>(...)} 的构造在 NeoForge 里由 access transformer 开放为
 * public；Fabric 没有 AT，这里能否直接 new <b>未验证</b> —— 若编译报不可见，需要改用
 * accessWidener（{@code fabric.mod.json} 的 {@code accessWidener} 字段）。
 */
public final class ModBlockEntities {

    public static RegistryHolder<BlockEntityType<CyberwareStationBlockEntity>> CYBERWARE_STATION;

    private ModBlockEntities() {
    }

    public static void register() {
        CYBERWARE_STATION = new RegistryHolder<>(Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(Cyberware.MODID, "cyberware_station"),
                FabricBlockEntityTypeBuilder.create(
                        CyberwareStationBlockEntity::new,
                        ModBlocks.CYBERWARE_STATION.get()).build()));
    }
}
