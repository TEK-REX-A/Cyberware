package com.dsh.cyberware.event;

import com.dsh.cyberware.core.CombatEffects;
import com.dsh.cyberware.core.HackSystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;

/**
 * 破解状态的生命周期与兜底（Fabric 版）。
 *
 * <p>四件事与 NeoForge 版的对应关系：
 * <ol>
 *   <li>{@code EntityTickEvent.Post} → 每刻遍历维度实体调 {@link CombatEffects#tick}；</li>
 *   <li>{@code EntityJoinLevelEvent}（可取消）→ {@code ServerEntityEvents.ALLOW_LOAD} 返回 false；</li>
 *   <li>{@code ServerStoppedEvent} → {@code ServerLifecycleEvents.SERVER_STOPPED}；</li>
 *   <li>{@code PlayerLoggedOutEvent} → {@code ServerPlayConnectionEvents.DISCONNECT}。</li>
 * </ol>
 */
public final class CombatEffectsHandler {

    private CombatEffectsHandler() {
    }

    /** 每刻：到期恢复（服务端，逐实体）。 */
    public static void onLevelTickEnd(ServerLevel level) {
        for (Entity entity : (Iterable<Entity>) level.getAllEntities()) {
            CombatEffects.tick(entity);
        }
    }

    /** 武器故障：来源实体处于故障状态的投掷物不许进入世界。 */
    public static boolean allowLoad(Entity entity) {
        if (!(entity instanceof Projectile projectile)) {
            return true;
        }
        Entity owner = projectile.getOwner();
        return owner == null || !CombatEffects.isWeaponGlitched(owner);
    }

    /** 服务器停止：还原所有临时状态 + 清上传。 */
    public static void onServerStopped() {
        CombatEffects.restoreAll();
        HackSystem.clear();
    }

    /** 玩家登出：丢掉他的上传。 */
    public static void onPlayerLoggedOut(ServerPlayer player) {
        HackSystem.cancelFor(player);
    }
}
