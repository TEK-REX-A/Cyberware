package com.dsh.cyberware.event;

import com.dsh.cyberware.core.CombatEffects;
import com.dsh.cyberware.core.HackSystem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * 破解状态的生命周期与兜底（t24）。
 *
 * <p>本类只做四件事，注册方式见 {@code network/CyberwareNetwork#register}（显式注册，不用注解扫描）：
 * <ol>
 *   <li>{@code EntityTickEvent.Post} → 每刻让 {@link CombatEffects#tick} 检查到期/实体消失
 *       （护甲减益、武器故障标记、系统重置的 NoAI 都靠它恢复）；</li>
 *   <li>{@code EntityJoinLevelEvent} → **武器故障**：来源实体处于故障状态的投掷物直接取消
 *       （不改 AI 状态，最干净；这也是 captain 给的口径）；</li>
 *   <li>{@code ServerStoppedEvent} → 还原所有临时状态（防「永久 noAi / 永久负护甲」进存档）+ 清上传；</li>
 *   <li>{@code PlayerLoggedOutEvent} → 清掉该玩家的上传。</li>
 * </ol>
 */
public final class CombatEffectsHandler {

    private CombatEffectsHandler() {
    }

    /** 每刻：到期恢复（服务端）。 */
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        CombatEffects.tick(entity);
    }

    /** 武器故障：拦「来源实体正在故障」的投掷物（EntityJoinLevelEvent 可取消）。 */
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Projectile projectile)) {
            return;
        }
        Entity owner = projectile.getOwner();
        if (owner != null && CombatEffects.isWeaponGlitched(owner)) {
            event.setCanceled(true);
        }
    }

    /** 服务器停止：把临时状态全部还原，并清掉上传（不留跨会话残留）。 */
    public static void onServerStopped(ServerStoppedEvent event) {
        CombatEffects.restoreAll();
        HackSystem.clear();
    }

    /** 玩家登出：丢掉他的上传。 */
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        HackSystem.cancelFor(event.getEntity());
    }
}
