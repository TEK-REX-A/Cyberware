package com.dsh.cyberware.core;

import com.dsh.cyberware.Cyberware;
import com.dsh.cyberware.network.HackPayload;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 快速破解 · 服务端玩法闭环（t24）。
 *
 * <p>一个请求的生命周期：
 * <pre>
 *   客户端 CAST（HackPayload）
 *     → 校验目标（存在/活着/不是自己；id 一律不可信）
 *     → 收费 pay()：① RAM 够 → 扣 RAM；② RAM 耗尽且**超频开着** → 濒死超频，扣 2 点生命（最低保留 2 HP）
 *                       ③ RAM 耗尽且超频没开 → 彻底瘫痪，拒绝 + 回包
 *     → 记一条上传（按刻倒计时），回 LOCKED + UPLOAD_START
 *     → 每刻 tick()：目标死亡/消失/跨维度/施法者死亡 → 兜底取消（回 CANCELLED）
 *     → 上传到点 → HackLibrary#apply（真正生效）+ 回 APPLIED
 * </pre>
 *
 * <p><b>客户端字段一律不可信</b>：目标、破解 id、扣费结果全部服务端自己算。
 *
 * <p>静态表（每个玩家一条上传）所以按 0.3.12 规则 7 处理：{@link #clear()} +
 * 服务器停止/玩家登出时清理（注册在 {@code network/CyberwareNetwork} 与
 * {@code event/CombatEffectsHandler}）。
 */
public final class HackSystem {

    /** 一次释放的付费结果。 */
    public enum Payment {
        /** 正常扣 RAM */
        RAM,
        /** 濒死超频：扣 2 点生命 */
        HEALTH,
        /** 彻底瘫痪：拒绝 */
        REJECTED
    }

    /** 进行中的上传（每个玩家最多一条）。 */
    private record Upload(int targetId, HackLibrary hack, int remaining, int total, ResourceKey<Level> dimension) {
    }

    private static final Map<UUID, Upload> UPLOADS = new HashMap<>();

    // 邮件IX §三.2 明文：濒死超频时「强制扣除玩家2点生命（不会致死，最低保留1颗心）」→ 2 点 / 1 颗心 = 2 HP
    private static final float DYING_HEALTH_COST = 2.0F;
    private static final float DYING_HEALTH_FLOOR = 2.0F;

    /** 扫描 / 目标校验的统一距离上限：20 格（STEP3 契约 §1.1/§1.2 冻结值）。 */
    private static final double SCAN_RANGE = 20.0D;
    private static final double SCAN_RANGE_SQR = SCAN_RANGE * SCAN_RANGE;

    /** 扫描发光时长：1200 刻 = 60 秒（STEP3 契约 §1.1 冻结值）。 */
    private static final int GLOW_TICKS = 1200;

    /** 拒绝原因（契约冻结字符串，客户端按它做提示）。 */
    private static final String NO_KIROSHI = "NO_KIROSHI";
    private static final String NO_TARGET = "NO_TARGET";

    private HackSystem() {
    }

    /**
     * 歧路司义眼扫描（C2S {@code HackPayload.Action.SCAN} 的服务端处理，STEP3 契约 §1.1）。
     *
     * <p>服务端自己算，客户端只发请求：
     * <ol>
     *   <li>没装歧路司义眼（{@code kiroshi_*} / {@code iconic_advanced_kiroshi*} 任一）→
     *       回 {@code REJECTED} + {@code note = "NO_KIROSHI"}；</li>
     *   <li>装了 → 给施法者 **20 格内**的活体（不含自己）加 {@code MobEffects.GLOWING} 1200 刻。</li>
     * </ol>
     * 成功路径不回包（发光本身在世界里看得见）；契约只规定失败要回 {@code REJECTED}。
     *
     * @return 是否真的扫描了
     */
    public static boolean scan(ServerPlayer caster) {
        if (caster == null) {
            return false;
        }
        if (!hasKiroshiOptics(caster)) {
            reject(caster, -1, "scan", NO_KIROSHI);
            Cyberware.LOGGER.debug("[cyberware] 扫描被拒（未装歧路司义眼）：{}",
                    caster.getName().getString());
            return false;
        }
        List<LivingEntity> targets = caster.level().getEntitiesOfClass(LivingEntity.class,
                caster.getBoundingBox().inflate(SCAN_RANGE),
                entity -> entity != caster && entity.isAlive()
                        && entity.distanceToSqr(caster) <= SCAN_RANGE_SQR);
        for (LivingEntity entity : targets) {
            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_TICKS, 0));
        }
        Cyberware.LOGGER.debug("[cyberware] 歧路司扫描：{} 高亮 {} 个目标",
                caster.getName().getString(), targets.size());
        return true;
    }

    /**
     * 歧路司义眼判定：安装表里有 {@code kiroshi_*} 或 {@code iconic_advanced_kiroshi*} 任一。
     *
     * <p>口径按 STEP3 契约 §1.1 冻结（前缀匹配，不是写死某几个 id —— 官方命名里有
     * {@code kiroshi_optics_bare}/{@code _combined}/{@code _hunter}/{@code _wallhack} 等变体）。
     */
    private static boolean hasKiroshiOptics(ServerPlayer caster) {
        for (String id : CyberwareInstallation.of(caster).installed().keySet()) {
            if (id.startsWith("kiroshi_") || id.startsWith("iconic_advanced_kiroshi")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 收费：RAM → 生命 → 拒绝（契约 §2 三种状态）。
     *
     * <p>可复现判定路径（验收第三条）：{@code canPay} 为假时看
     * {@link OverclockState#isActive(long)}（时钟 {@link OverclockSystem#serverTicks}）；
     * 开着就是濒死超频，扣 {@link #DYING_HEALTH_COST} 点生命并夹在 {@link #DYING_HEALTH_FLOOR} 以上
     * —— {@code setHealth} 的值恒 ≥ 2，所以**不会致死**。
     */
    public static Payment pay(ServerPlayer caster, int cost) {
        if (RamSystem.canPay(caster, cost)) {
            RamSystem.spend(caster, cost);
            return Payment.RAM;
        }
        if (OverclockState.of(caster).isActive(OverclockSystem.serverTicks(caster))) {
            // 濒死超频：不扣 RAM，改扣生命；生命本来就不足下限时不动（也不会把血加上去）
            if (caster.getHealth() > DYING_HEALTH_FLOOR) {
                caster.setHealth(Math.max(DYING_HEALTH_FLOOR, caster.getHealth() - DYING_HEALTH_COST));
            }
            return Payment.HEALTH;
        }
        return Payment.REJECTED;
    }

    /**
     * 客户端请求释放破解（C2S {@code HackPayload.Action.CAST} 的服务端处理）。
     *
     * @return 是否真的开始上传
     */
    public static boolean request(ServerPlayer caster, int targetEntityId, String hackId) {
        HackLibrary hack = HackLibrary.byId(hackId);
        if (caster == null || hack == null) {
            reject(caster, targetEntityId, hackId, "unknown_hack");
            return false;
        }
        if (UPLOADS.containsKey(caster.getUUID())) {
            reject(caster, targetEntityId, hackId, "uploading");
            return false;
        }
        Entity target = caster.level().getEntity(targetEntityId);
        if (!(target instanceof LivingEntity living) || target == caster) {
            reject(caster, targetEntityId, hackId, NO_TARGET);
            return false;
        }
        // 服务端独立校验（STEP3 契约 §1.2，**不信任客户端**）：目标存活 / 距离 ≤ 20 / 有视线。
        // 任一不过都在**扣费之前**拒绝 —— 不允许为无效目标扣 RAM。
        if (!living.isAlive()
                || caster.distanceToSqr(living) > SCAN_RANGE_SQR
                || !caster.hasLineOfSight(living)) {
            reject(caster, targetEntityId, hackId, NO_TARGET);
            Cyberware.LOGGER.debug("[cyberware] 破解被拒（目标校验不过）：{} → 实体 {}",
                    caster.getName().getString(), targetEntityId);
            return false;
        }

        Payment payment = pay(caster, hack.ramCost());
        if (payment == Payment.REJECTED) {
            // 彻底瘫痪：RAM 耗尽且超频未开 → 拒绝并回报失败状态
            reject(caster, targetEntityId, hackId, "RAM ACCESS FAILED");
            Cyberware.LOGGER.debug("[cyberware] 破解被拒（瘫痪）：{} → {}",
                    caster.getName().getString(), hack.id());
            return false;
        }

        int total = hack.uploadTicks();
        UPLOADS.put(caster.getUUID(),
                new Upload(targetEntityId, hack, total, total, caster.level().dimension()));
        String note = payment == Payment.HEALTH ? "dying_overclock" : "";
        send(caster, HackPayload.Action.LOCKED, targetEntityId, hack.id(), total, total,
                payment == Payment.RAM ? hack.ramCost() : 0, note);
        send(caster, HackPayload.Action.UPLOAD_START, targetEntityId, hack.id(), total, total, 0, "");
        return true;
    }

    /**
     * 每刻推进上传（由 {@link RamSystem#onPlayerTick} 调用）。
     *
     * <p>上传是**服务端按刻计时**：只有倒计时归零那一刻才调 {@link HackLibrary#apply}，
     * 中途任何一步不满足就兜底取消。
     */
    public static void tick(ServerPlayer caster) {
        Upload upload = UPLOADS.get(caster.getUUID());
        if (upload == null) {
            return;
        }
        if (caster.isDeadOrDying() || caster.level().dimension() != upload.dimension()) {
            UPLOADS.remove(caster.getUUID());
            send(caster, HackPayload.Action.CANCELLED, upload.targetId(), upload.hack().id(), 0,
                    upload.total(), 0, "caster_lost");
            return;
        }
        Entity target = caster.level().getEntity(upload.targetId());
        if (!(target instanceof LivingEntity living) || !living.isAlive()) {
            // 目标死亡/消失/换维度：兜底清理
            // TODO(待主人裁决: 上传被取消是否退还已扣的 RAM —— 现为不退，见 HACK-VALUES.md 待裁决清单)
            UPLOADS.remove(caster.getUUID());
            send(caster, HackPayload.Action.CANCELLED, upload.targetId(), upload.hack().id(), 0,
                    upload.total(), 0, "target_lost");
            return;
        }
        int remaining = upload.remaining() - 1;
        if (remaining > 0) {
            UPLOADS.put(caster.getUUID(), new Upload(upload.targetId(), upload.hack(), remaining,
                    upload.total(), upload.dimension()));
            send(caster, HackPayload.Action.UPLOAD_PROGRESS, upload.targetId(), upload.hack().id(),
                    remaining, upload.total(), 0, "");
            return;
        }
        // 到点：真正生效
        UPLOADS.remove(caster.getUUID());
        upload.hack().apply(caster, living);
        send(caster, HackPayload.Action.APPLIED, upload.targetId(), upload.hack().id(), 0,
                upload.total(), 0, "");
        Cyberware.LOGGER.debug("[cyberware] 破解生效：{} → 实体 {}", upload.hack().id(), upload.targetId());
    }

    /** 玩家登出：丢掉他的上传（服务端状态不留到下一个会话）。 */
    public static void cancelFor(Player player) {
        if (player != null) {
            UPLOADS.remove(player.getUUID());
        }
    }

    /** 服务器停止：清空所有上传。 */
    public static void clear() {
        UPLOADS.clear();
    }

    private static void reject(ServerPlayer caster, int targetEntityId, String hackId, String note) {
        if (caster != null) {
            send(caster, HackPayload.Action.REJECTED, targetEntityId, hackId == null ? "" : hackId,
                    0, 0, 0, note);
        }
    }

    private static void send(ServerPlayer player, HackPayload.Action action, int targetEntityId,
                             String hackId, int remaining, int total, int ramCost, String note) {
        PacketDistributor.sendToPlayer(player, new HackPayload(
                action, targetEntityId, hackId, remaining, total, ramCost, note));
    }
}
