package com.dsh.cyberware.client;

import com.dsh.cyberware.core.CyberwareInstallation;
import com.dsh.cyberware.network.HackPayload;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 快速破解的<b>客户端显示状态</b>（t29）：锁定目标 + 上传进度 + 只读展示表。
 *
 * <p><b>客户端没有任何权威</b>：锁定目标是「我想打谁」的意图（服务端在 {@code CAST} 时会自己
 * 再校验存活/距离/视线）；上传进度、被拒原因全部来自服务端下行包。这里不做任何扣费或效果判定。
 *
 * <p>锁定口径按契约 §1.2：<b>屏幕中心 10% 半径内、距离 ≤ 20 格、最近的活体</b>。
 */
public final class HackClientState {

    /** 最大锁定距离（契约 §1.2），单位：格 */
    public static final double LOCK_RANGE = 20.0D;
    /** 「屏幕中心 10%」：横向/纵向各占屏幕的 10% */
    private static final double LOCK_BOX_FRACTION = 0.10D;
    /** 每 tick 最多投影几个候选（帧率自保；投影 API 每次都会 new 一个 Vec3） */
    private static final int MAX_CANDIDATES = 12;

    /**
     * 五条破解的<b>只读展示表</b>（契约 §1.3）。
     *
     * <p>⚠ RAM 消耗目前是**占位值**，待 t30 的 {@code HACK-VALUES.md} 落地后替换，
     * 替换时请在同一行注释里写出来源（契约 §2）。
     */
    public record HackEntry(String id, String name, int ramCost) {
    }

    private static final List<HackEntry> HACKS = List.of(
            new HackEntry("overheat", "过热", 4),          // TODO(主人填写) 占位值，待 HACK-VALUES.md
            new HackEntry("short_circuit", "短路", 5),      // TODO(主人填写) 占位值，待 HACK-VALUES.md
            new HackEntry("synapse_burnout", "突触熔断", 7), // TODO(主人填写) 占位值，待 HACK-VALUES.md
            new HackEntry("weapon_glitch", "武器故障", 3),   // TODO(主人填写) 占位值，待 HACK-VALUES.md
            new HackEntry("system_reset", "系统重置", 9));   // TODO(主人填写) 占位值，待 HACK-VALUES.md

    // ── 锁定目标（客户端算） ──
    private static int lockEntityId = -1;
    private static boolean locked;
    /** 扫描被拒的提示（NO_KIROSHI 等），由 HackHud 画一小段时间 */
    private static String lastNote = "";
    private static long noteAtMs;

    // ── 上传进度（服务端下行） ──
    private static boolean uploading;
    private static int uploadRemainingTicks;
    private static int uploadTotalTicks;
    private static long uploadStartedMs;
    private static String uploadHackId = "";
    /** APPLIED / CANCELLED / REJECTED 之后的收缩消失动画起点（0 = 没在收缩） */
    private static long collapseAtMs;
    /** 上一次显示过的进度（收缩动画用它作为起点宽度） */
    private static float lastProgress;
    /** 保命日志去重（t34 / inspector F1）：每 tick 的回调抛异常只记一次，绝不刷屏 */
    private static final java.util.concurrent.atomic.AtomicBoolean LOCK_FAIL_LOGGED =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    private HackClientState() {
    }

    /** 只读展示表（纯展示，服务端仍是唯一权威）。 */
    public static List<HackEntry> hacks() {
        return HACKS;
    }

    public static boolean hasLock() {
        return locked && lockEntityId >= 0;
    }

    public static int lockEntityId() {
        return lockEntityId;
    }

    /** 被锁定的实体；没有/已失效返回 null（调用方必须处理 null）。 */
    public static LivingEntity lockedEntity() {
        if (!hasLock()) {
            return null;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }
        Entity entity = minecraft.level.getEntity(lockEntityId);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    public static boolean uploading() {
        return uploading;
    }

    public static int uploadRemainingTicks() {
        return uploadRemainingTicks;
    }

    public static float uploadProgress() {
        if (uploadTotalTicks <= 0) {
            return 0.0F;
        }
        float p = 1.0F - (float) uploadRemainingTicks / uploadTotalTicks;
        return Math.max(0.0F, Math.min(1.0F, p));
    }

    /** 收缩动画起点（0 = 没有在收缩）。 */
    public static long collapseAtMs() {
        return collapseAtMs;
    }

    public static float lastProgress() {
        return lastProgress;
    }

    public static String uploadHackId() {
        return uploadHackId;
    }

    public static String lastNote() {
        return lastNote;
    }

    public static long noteAtMs() {
        return noteAtMs;
    }

    // ═══════════════════ 义眼前置（锁定框的唯一开关） ═══════════════════

    /**
     * 义眼型号的前缀。与服务端 {@code core/HackSystem.hasKiroshiOptics} 的口径<b>逐字一致</b>
     * （前缀匹配，不写死某几个 id —— 官方命名里有 {@code kiroshi_optics_bare/_combined/_hunter/_wallhack} 等变体）。
     * 客户端必须用服务端同一套「什么算义眼」的定义，否则会出现「服务端允许扫描、客户端不画框」或反过来。
     */
    private static final String[] OPTICS_PREFIXES = {
            "kiroshi_",
            "iconic_advanced_kiroshi"
    };

    /** 义眼判定读数据异常只报一次（不刷屏） */
    private static final java.util.concurrent.atomic.AtomicBoolean OPTICS_FAIL_LOGGED =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    /**
     * 本地玩家装没装歧路司义眼 —— <b>屏幕锁定框的唯一开关</b>。
     *
     * <p><b>为什么要在这里判一次</b>：服务端（{@code HackSystem}）只在 SCAN/CAST 时才检查义眼，
     * 而锁定框是<b>客户端每 tick 自己算</b>、<b>每帧自己画</b>的；不在这里挡一道，
     * 没装义眼的玩家世界里照样有框。
     *
     * <p><b>数据源</b>：{@code CyberwareInstallation.of(LocalPlayer)} —— 与服务端写入的
     * {@code ModAttachments.INSTALLATION} 是<b>同一张附件表</b>（走 core 侧查询 API，本类不碰存储层）。
     * 客户端能不能真的读到已装义体取决于存储层的同步（另一条线在修）；本处<b>只读</b>。
     * 读不到时返回 false —— 宁可不画框，也不能给没装义眼的玩家画框。
     */
    public static boolean hasOptics() {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            LocalPlayer self = minecraft == null ? null : minecraft.player;
            if (self == null) {
                return false;
            }
            for (String id : CyberwareInstallation.of(self).installed().keySet()) {
                for (String prefix : OPTICS_PREFIXES) {
                    if (id != null && id.startsWith(prefix)) {
                        return true;
                    }
                }
            }
        } catch (Throwable t) {
            // 数据层尚未就绪（例如附件表未注册）不能让每 tick / 每帧回调炸掉
            if (OPTICS_FAIL_LOGGED.compareAndSet(false, true)) {
                com.dsh.cyberware.Cyberware.LOGGER.warn(
                        "[cyberware] 义眼判定读数据失败，锁定框按「未装义眼」处理", t);
            }
        }
        return false;
    }

    // ═══════════════════ 锁定：每 tick 选一次（不在每帧做，帧率自保） ═══════════════════

    /**
     * 每 tick 重新选锁定目标：屏幕中心 10% 框内、≤20 格、最近的活体。
     *
     * <p><b>无候选时立刻清空</b>（目标死亡/走远/转出视野 / 玩家自己死亡都一样），不留残影。
     *
     * <p><b>保命壳（t34 / inspector F1）</b>：这是每 tick 回调，抛异常会当场崩客户端 ——
     * 整段包 try/catch，异常只记一次日志。逻辑体一字未动，搬进了
     * {@link #tickLockInternal()}。
     */
    public static void tickLock() {
        try {
            tickLockInternal();
        } catch (Throwable t) {
            if (LOCK_FAIL_LOGGED.compareAndSet(false, true)) {
                com.dsh.cyberware.Cyberware.LOGGER.warn(
                        "[cyberware] 锁定目标计算异常（已忽略，不影响游戏）", t);
            }
        }
    }

    /** {@link #tickLock()} 的逻辑体 —— 与 t29 完全一致，只是外包了一层保命壳。 */
    private static void tickLockInternal() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer self = minecraft.player;
        if (self == null || minecraft.level == null || !self.isAlive()) {
            clearLock();
            return;
        }
        // 【义眼前置】没装歧路司义眼就不选锁定目标 ——
        // 原实现在这里没有任何前置判断：只要屏幕中心 10%、≤20 格内有活体就锁定成功，
        // 于是 HackHud.drawLockFrame 每帧照画 —— 这就是「没装义眼也显示目标选定框」。
        if (!hasOptics()) {
            clearLock();
            return;
        }
        var window = minecraft.getWindow();
        if (window == null) {
            clearLock();
            return;
        }
        double w = window.getGuiScaledWidth();
        double h = window.getGuiScaledHeight();
        double halfW = w * 0.5D;
        double halfH = h * 0.5D;
        double boxX = w * LOCK_BOX_FRACTION;
        double boxY = h * LOCK_BOX_FRACTION;

        int bestId = -1;
        double bestDistSq = Double.MAX_VALUE;
        int projected = 0;
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (projected >= MAX_CANDIDATES) {
                break;                                  // 帧率自保
            }
            if (!(entity instanceof LivingEntity living) || entity == self || !living.isAlive()) {
                continue;
            }
            double distSq = entity.distanceToSqr(self);
            if (distSq > LOCK_RANGE * LOCK_RANGE) {
                continue;
            }
            // 身份判定：渲染状态类型/实体 id 那条铁律管的是「屏幕上的东西是谁」；
            // 这里是世界实体遍历，用实体引用与 id —— 不涉及坐标比身份。
            projected++;
            if (!ScreenProjection.project(entity.getX(),
                    entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ())) {
                continue;                               // 背后 / 不在视锥
            }
            if (Math.abs(ScreenProjection.x() - halfW) > boxX
                    || Math.abs(ScreenProjection.y() - halfH) > boxY) {
                continue;                               // 不在屏幕中心 10% 框内
            }
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                bestId = entity.getId();
            }
        }
        if (bestId < 0) {
            clearLock();
        } else {
            lockEntityId = bestId;
            locked = true;
        }
    }

    private static void clearLock() {
        lockEntityId = -1;
        locked = false;
    }

    // ═══════════════════ 服务端下行 ═══════════════════

    public static void onHackPayload(HackPayload payload) {
        if (payload == null) {
            return;
        }
        long now = System.currentTimeMillis();
        switch (payload.action()) {
            case LOCKED -> {
                // 服务端也认为锁上了：以服务端给的 id 为准（客户端算的那个只是意图）
                if (payload.targetEntityId() > 0) {
                    lockEntityId = payload.targetEntityId();
                    locked = true;
                }
            }
            case UPLOAD_START -> {
                uploading = true;
                uploadHackId = payload.hackId();
                uploadTotalTicks = Math.max(1, payload.uploadTotalTicks());
                uploadRemainingTicks = payload.uploadRemainingTicks() > 0
                        ? payload.uploadRemainingTicks() : uploadTotalTicks;
                uploadStartedMs = now;
                collapseAtMs = 0L;
            }
            case UPLOAD_PROGRESS -> {
                if (uploading) {
                    uploadRemainingTicks = Math.max(0, payload.uploadRemainingTicks());
                    lastProgress = uploadProgress();
                }
            }
            case APPLIED, CANCELLED -> finishUpload(now);
            case REJECTED -> {
                finishUpload(now);
                String note = payload.note() == null ? "" : payload.note();
                if (!note.isBlank()) {
                    lastNote = note;
                    noteAtMs = now;
                }
                if ("NO_TARGET".equals(note)) {
                    clearLock();                        // 服务端说没目标：清掉本地锁定
                }
            }
            default -> {
                // SCAN / CAST 是上行语义，下行收到也不该改状态
            }
        }
    }

    private static void finishUpload(long now) {
        if (uploading) {
            lastProgress = uploadProgress();
            uploading = false;
            collapseAtMs = now;                          // 收缩消失动画
        }
    }

    /** 让别的客户端逻辑也能弹一条提示（t36：X 长按但无锁定目标时用）。 */
    public static void showNote(String note) {
        if (note == null || note.isBlank()) {
            return;
        }
        lastNote = note;
        noteAtMs = System.currentTimeMillis();
    }

    /** 收缩动画结束：HUD 调它把时间戳归零，之后不再画任何东西（不留残影）。 */
    public static void endCollapse() {
        collapseAtMs = 0L;
        lastProgress = 0.0F;
    }

    /** 换世界 / 退出：静态状态必须清（契约 §4 第 4 条）。 */
    public static void clear() {
        lockEntityId = -1;
        locked = false;
        uploading = false;
        uploadRemainingTicks = 0;
        uploadTotalTicks = 0;
        uploadStartedMs = 0L;
        uploadHackId = "";
        collapseAtMs = 0L;
        lastProgress = 0.0F;
        lastNote = "";
        noteAtMs = 0L;
    }

    /** RAM 是否够用这条破解（**纯展示判定**，用于置灰；服务端仍会自己扣费与裁决）。 */
    public static boolean affordable(HackEntry entry) {
        return entry != null && RamClientState.current() >= entry.ramCost();
    }

    /** 给 HUD 用：这个玩家是不是本地玩家（避免把玩家自己当破解目标画框）。 */
    public static boolean isSelf(Entity entity) {
        return entity instanceof Player && entity == Minecraft.getInstance().player;
    }
}
