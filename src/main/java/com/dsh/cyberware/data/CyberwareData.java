package com.dsh.cyberware.data;

import com.dsh.cyberware.core.CyberwareRarity;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 义体的数据组件载荷：稀有度 + 等级。
 *
 * <p>需求书要求义体数据必须存在 DataComponent 里。这里两个字段都用 {@code optionalFieldOf}
 * 加默认值，配合 {@link #safe()}，旧存档 / 组件缺失 / 字段被外部改坏都不会抛异常。
 *
 * @param rarityIndex 稀有度序号（对应 {@link CyberwareRarity#ordinal()}，越界一律回落普通）
 * @param level       升级等级（每次升级 +1）
 */
public record CyberwareData(int rarityIndex, int level) {

    /** 默认值：普通 + 1 级 */
    public static final CyberwareData DEFAULT = new CyberwareData(0, 1);

    public static final Codec<CyberwareData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.optionalFieldOf("rarity", 0).forGetter(CyberwareData::rarityIndex),
            Codec.INT.optionalFieldOf("level", 1).forGetter(CyberwareData::level)
    ).apply(inst, CyberwareData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CyberwareData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CyberwareData::rarityIndex,
            ByteBufCodecs.VAR_INT, CyberwareData::level,
            CyberwareData::new
    );

    /** 防御性读取入口：任何字段非法都回落到默认，绝不抛异常。 */
    public CyberwareData safe() {
        int safeRarity = CyberwareRarity.byIndexSafe(this.rarityIndex).ordinal();
        int safeLevel = Math.max(1, this.level);
        return (safeRarity == this.rarityIndex && safeLevel == this.level)
                ? this
                : new CyberwareData(safeRarity, safeLevel);
    }

    public CyberwareRarity rarity() {
        return CyberwareRarity.byIndexSafe(this.rarityIndex);
    }

    public CyberwareData withRarity(CyberwareRarity rarity) {
        return new CyberwareData(rarity.ordinal(), this.level);
    }

    public CyberwareData upgraded() {
        CyberwareRarity next = this.rarity().next();
        return next == null ? this : this.withRarity(next);
    }
}
