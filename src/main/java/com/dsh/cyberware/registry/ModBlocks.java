package com.dsh.cyberware.registry;

import com.dsh.cyberware.Cyberware;
import com.dsh.cyberware.block.CyberwareStationBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 方块注册表。 */
public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Cyberware.MODID);

    /**
     * 义体操作台。
     * 需求书【一】要求：不可被活塞推动 → {@link PushReaction#BLOCK}。
     */
    public static final DeferredBlock<CyberwareStationBlock> CYBERWARE_STATION = BLOCKS.registerBlock(
            "cyberware_station",
            CyberwareStationBlock::new,
            props -> props
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(3.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK));

    private ModBlocks() {
    }
}
