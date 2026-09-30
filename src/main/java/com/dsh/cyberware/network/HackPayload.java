package com.dsh.cyberware.network;

import com.dsh.cyberware.Cyberware;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 快速破解：**双向**包（目标锁定 / 上传进度 / 结果，全走这一条）。
 *
 * <ul>
 *   <li>{@link Action#CAST}　客户端 → 服务端：「对实体 id = targetEntityId 释放 hackId」。
 *       客户端只表达意图；扣 RAM / 扣血 / 是否允许（濒死超频、彻底瘫痪三种状态）全部由服务端裁决。</li>
 *   <li>其余 action　服务端 → 客户端：锁定 / 上传 / 结果 / 被拒。客户端只渲染。</li>
 * </ul>
 *
 * <p>⚠️ 本任务（t23）只落**契约与注册**：服务端收到 {@code CAST} 只记一条 debug 日志，
 * 不产生任何效果与回包 —— 效果层（5 条破解、AI 禁用、上传计时）是 t25/t26 的活。
 *
 * @param action               见 {@link Action}
 * @param targetEntityId       目标实体 id（{@code Entity#getId()}；没有目标时 0）
 * @param hackId               破解 id（{@code overheat} / {@code short_circuit} / {@code synapse_burnout}
 *                             / {@code weapon_glitch} / {@code system_reset}；没有破解时是空串）
 * @param uploadRemainingTicks 上传剩余刻数（{@code UPLOAD_PROGRESS} 用；其余为 0）
 * @param uploadTotalTicks     本次上传总刻数（进度条分母；其余为 0）
 * @param ramCost              本次实际扣除的 RAM（0 = 没扣 / 扣的是血，见 {@code note}）
 * @param note                 给 HUD 的短说明或拒绝原因（可空串；不许拿它传长文本）
 */
public record HackPayload(Action action, int targetEntityId, String hackId,
                          int uploadRemainingTicks, int uploadTotalTicks, int ramCost, String note)
        implements CustomPacketPayload {

    /** 破解相关的事件类型。 */
    public enum Action {
        /** 客户端 → 服务端：请求释放破解 */
        CAST,
        /** 服务端 → 客户端：已经锁定目标（HUD 画折角框） */
        LOCKED,
        /** 服务端 → 客户端：上传开始（带总刻数） */
        UPLOAD_START,
        /** 服务端 → 客户端：上传进度（带剩余刻数） */
        UPLOAD_PROGRESS,
        /** 服务端 → 客户端：上传完成、效果生效 */
        APPLIED,
        /** 服务端 → 客户端：上传被打断 / 目标丢失 */
        CANCELLED,
        /** 服务端 → 客户端：请求被拒（原因在 note：如 {@code RAM ACCESS FAILED} / 冷却中 / 无目标） */
        REJECTED;

        public static Action byId(int id) {
            Action[] values = values();
            return (id < 0 || id >= values.length) ? REJECTED : values[id];
        }
    }

    public static final CustomPacketPayload.Type<HackPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Cyberware.MODID, "hack"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HackPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, p -> p.action().ordinal(),
                    ByteBufCodecs.VAR_INT, HackPayload::targetEntityId,
                    ByteBufCodecs.STRING_UTF8, HackPayload::hackId,
                    ByteBufCodecs.VAR_INT, HackPayload::uploadRemainingTicks,
                    ByteBufCodecs.VAR_INT, HackPayload::uploadTotalTicks,
                    ByteBufCodecs.VAR_INT, HackPayload::ramCost,
                    ByteBufCodecs.STRING_UTF8, HackPayload::note,
                    (actionId, targetId, hackId, remaining, total, ramCost, note) ->
                            new HackPayload(Action.byId(actionId), targetId, hackId, remaining, total,
                                    ramCost, note));

    /** 防御：两个字符串字段允许外部传 null，线上格式统一成空串。 */
    public HackPayload {
        hackId = hackId == null ? "" : hackId;
        note = note == null ? "" : note;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** 客户端上行用的「释放破解」包。 */
    public static HackPayload cast(int targetEntityId, String hackId) {
        return new HackPayload(Action.CAST, targetEntityId, hackId, 0, 0, 0, "");
    }
}
