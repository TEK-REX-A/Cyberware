package com.dsh.cyberware.core;

import com.dsh.cyberware.data.CyberwareData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * <b>玩家身上装了哪些义体</b> —— 义体系统的地基。
 *
 * <p>在此之前，「玩家装了什么」根本没有地方存：义体只能躺在操作台的 BlockEntity 里，
 * 技能全靠 {@code activateHeld}（手持物品按激活键）临时生效。
 * 结果就是任何被动义体都无从判断 —— 网络接入仓提不了 RAM 上限，
 * 义眼也不知道该不该给敌人描边。
 *
 * <p>现在每个玩家挂一份 {@code CyberwareInstallation}（见 {@code ModAttachments}）：
 * 槽位型号 id → 该件的 {@link CyberwareData}（稀有度 + 等级）。
 * 用 AttachmentType 的好处是存档持久化、跨维度跟随、死亡拷贝、自动同步全都白送。
 *
 * <p>做成不可变 record：每次装卸返回新实例，调用方拿返回值写回附件 ——
 * 避免半路被别的代码改掉，也顺便让「同步」有个明确的触发点。
 *
 * @param installed 型号 id → 该件的稀有度与等级
 */
public record CyberwareInstallation(Map<String, CyberwareData> installed) {

    /** 空身：什么都没装 */
    public static final CyberwareInstallation EMPTY = new CyberwareInstallation(Map.of());

    /** 存档 / 网络都用这个 MapCodec（AttachmentType.serialize 收 MapCodec） */
    public static final MapCodec<CyberwareInstallation> CODEC =
            Codec.unboundedMap(Codec.STRING, CyberwareData.CODEC)
                    .fieldOf("installed")
                    .xmap(CyberwareInstallation::new, CyberwareInstallation::installed);

    /** 同步到客户端用 */
    public static final StreamCodec<RegistryFriendlyByteBuf, CyberwareInstallation> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.map(LinkedHashMap::new, ByteBufCodecs.STRING_UTF8, CyberwareData.STREAM_CODEC),
                    CyberwareInstallation::installed,
                    CyberwareInstallation::new);

    public CyberwareInstallation {
        // 防御性拷贝：外部塞进来的可变 Map 不能留后门（也顺手挡掉 null）
        installed = Map.copyOf(installed);
    }

    /** 身上有没有这件义体 —— 被动效果全靠它判断。 */
    public boolean has(String defId) {
        return this.installed.containsKey(defId);
    }

    /** 取某件的稀有度与等级；没装返回 null。 */
    public CyberwareData dataOf(String defId) {
        return this.installed.get(defId);
    }

    /** 装上 / 覆盖一件。 */
    public CyberwareInstallation with(String defId, CyberwareData data) {
        Map<String, CyberwareData> next = new LinkedHashMap<>(this.installed);
        next.put(defId, data.safe());
        return new CyberwareInstallation(next);
    }

    /** 卸下一件。 */
    public CyberwareInstallation without(String defId) {
        if (!this.installed.containsKey(defId)) {
            return this;
        }
        Map<String, CyberwareData> next = new LinkedHashMap<>(this.installed);
        next.remove(defId);
        return new CyberwareInstallation(next);
    }

    /** 装了几件。 */
    public int count() {
        return this.installed.size();
    }

    /**
     * 已占用的植入容量。
     *
     * <p>按每件的稀有度取对应变体的 capacity 求和；查不到的型号（旧存档里被删掉的）
     * 直接跳过，不让它把界面撑崩。
     */
    public int usedCapacity() {
        int total = 0;
        for (Map.Entry<String, CyberwareData> entry : this.installed.entrySet()) {
            CyberwareDefinition def = CyberwareDefinitions.byId(entry.getKey());
            if (def == null) {
                continue;
            }
            CyberwareDefinition.Variant variant = def.variantFor(entry.getValue().safe().rarity());
            if (variant != null) {
                total += variant.capacity();
            }
        }
        return total;
    }
}
