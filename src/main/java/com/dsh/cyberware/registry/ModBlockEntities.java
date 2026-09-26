package com.dsh.cyberware.registry;

import com.dsh.cyberware.Cyberware;
import com.dsh.cyberware.block.CyberwareStationBlockEntity;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 方块实体注册表。
 *
 * <p>注意：26.x 的 NeoForge 已移除 {@code DeferredRegister$BlockEntities}，这里用通用
 * {@code DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ...)}。
 * {@code BlockEntityType} 的构造在 NeoForge 里由 access transformer 开放为 public，
 * 所以可以直接 {@code new}。
 */
public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Cyberware.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CyberwareStationBlockEntity>> CYBERWARE_STATION =
            BLOCK_ENTITIES.register("cyberware_station", () -> new BlockEntityType<>(
                    CyberwareStationBlockEntity::new,
                    Set.of(ModBlocks.CYBERWARE_STATION.get())));

    private ModBlockEntities() {
    }
}
