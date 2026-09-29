package com.dsh.cyberware.core;

import com.dsh.cyberware.config.CyberwareConfig;
import com.dsh.cyberware.data.CyberwareData;
import com.dsh.cyberware.registry.ModAttachments;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

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
     * 已装型号 id 的**稳定顺序**列表（按 id 字典序）。
     *
     * <p>{@link Map#copyOf} 不保证迭代顺序，而 UI / 按钮 / 网络包都想用「第 N 件」指代某件义体，
     * 所以这里给出一个两端一致的确定性顺序 —— 服务端与客户端算出来的结果必须相同。
     */
    public List<String> orderedIds() {
        List<String> ids = new ArrayList<>(this.installed.keySet());
        ids.sort(java.util.Comparator.naturalOrder());
        return List.copyOf(ids);
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

    // ------------------------------------------------------------------
    // 静态查询 API —— 效果系统（义眼描边、被动加成）与操作台共用这一套入口
    // ------------------------------------------------------------------

    /** 需求书给玩家的默认总容量上限；配置项 {@code capacity.defaultCapacity} 的缺省值也是 100。 */
    public static final int MAX_TOTAL_CAPACITY = 100;

    /**
     * 取某玩家身上的义体表 —— <b>永远不返回 null</b>。
     *
     * <p>用 {@code getExistingDataOrNull} 而不是 {@code getData}：没装过义体的玩家
     * 不该被动多挂一份空表（原版实体附件是按需创建的）。
     */
    public static CyberwareInstallation of(Player player) {
        if (player == null) {
            return EMPTY;
        }
        CyberwareInstallation inst = player.getExistingDataOrNull(ModAttachments.INSTALLATION.get());
        return inst == null ? EMPTY : inst;
    }

    /** 玩家身上有没有这件义体（被动效果 / 义眼敌我识别都靠它判断）。 */
    public static boolean has(Player player, String defId) {
        return defId != null && of(player).installed().containsKey(defId);
    }

    /** 取某玩家身上某件义体的稀有度与等级；没装返回 null。 */
    public static CyberwareData dataOf(Player player, String defId) {
        return defId == null ? null : of(player).installed().get(defId);
    }

    /** 静态便捷版：某玩家已占用的植入容量。 */
    public static int usedCapacity(Player player) {
        return of(player).usedCapacity();
    }

    /**
     * 玩家总容量上限。
     *
     * <p>配置里关掉了容量限制就返回 {@link Integer#MAX_VALUE}（调试用「无限安装」）；
     * 配置值非法时回落到 {@link #MAX_TOTAL_CAPACITY}。
     */
    public static int capacityLimit() {
        if (!CyberwareConfig.ENABLE_CAPACITY_LIMIT.get()) {
            return Integer.MAX_VALUE;
        }
        int configured = CyberwareConfig.DEFAULT_CAPACITY.get();
        return configured > 0 ? configured : MAX_TOTAL_CAPACITY;
    }

    /** 剩余可用容量；上限未启用时为 {@link Integer#MAX_VALUE}。 */
    public static int remainingCapacity(Player player) {
        int limit = capacityLimit();
        return limit == Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.max(0, limit - usedCapacity(player));
    }
}
