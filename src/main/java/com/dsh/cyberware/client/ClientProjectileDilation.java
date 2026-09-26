package com.dsh.cyberware.client;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * 投射物时间减缓 · <b>客户端这一半</b>。
 *
 * <p>Minecraft 的投射物在客户端也会自己 tick —— 每刻按它自己的 {@code deltaMovement}
 * 往前推进，服务端位置包只是事后校正。所以「服务端慢、客户端快」的结局就是：
 * 玩家看到一支原速的箭，偶尔被包拽回原位，又抖又没减速。
 *
 * <p>这里做的不是把整个 tick 掐掉（那会连服务端位置包的插值一起冻住，箭会一格一格跳），
 * 而是**只按住本地物理推进**：tick 前把速度记下来、置零，tick 后再还回去。
 * 位置就完全由服务端位置包 + 客户端插值驱动 —— 服务端那边每刻只走一小步，
 * 插值出来就是连续的慢动作。
 */
public final class ClientProjectileDilation {

    /** tick 期间被临时借走的速度（tick 后原样还回去）。 */
    private static final Map<Entity, Vec3> FROZEN = new WeakHashMap<>();

    private ClientProjectileDilation() {
    }

    public static void onEntityTickPre(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Projectile projectile)) {
            return;
        }
        // 玩家自己射出的箭豁免：自己没慢、箭却慢了就本末倒置
        if (projectile.getOwner() instanceof Player) {
            return;
        }
        double timeScale = ClientTimeDilation.timeScaleAt(
                entity.getX(), entity.getY(), entity.getZ());
        if (timeScale >= 1.0D) {
            return;
        }
        FROZEN.put(projectile, projectile.getDeltaMovement());
        projectile.setDeltaMovement(Vec3.ZERO);
    }

    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Projectile projectile)) {
            return;
        }
        Vec3 saved = FROZEN.remove(projectile);
        if (saved != null) {
            projectile.setDeltaMovement(saved);
        }
    }
}
