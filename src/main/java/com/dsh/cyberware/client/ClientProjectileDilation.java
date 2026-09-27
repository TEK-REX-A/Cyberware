package com.dsh.cyberware.client;

import com.dsh.cyberware.core.DilationTickGate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * 投射物时间减缓 · <b>客户端这一半</b>。
 *
 * <p>Minecraft 的投射物在客户端也会自己 tick —— 每刻按它自己的 {@code deltaMovement}
 * 往前推进，服务端位置包只是事后校正。所以只让服务端跳 tick，玩家看到的还是客户端那支
 * 原速的箭。客户端必须一起跳。
 *
 * <p>相位由 {@link DilationTickGate} 用「游戏刻 + 实体 id」算，两端算出来是同一批刻，
 * 所以两边跳的刻完全对齐，不会互相打架。
 *
 * <p>主人明确选了这个方案：箭会**一顿一顿**地推着走（时间被切碎的本来面目），
 * 换来的是物理完全自洽 —— 不会「完全时停」也不会结束时瞬移到落点。
 */
public final class ClientProjectileDilation {

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
        if (DilationTickGate.shouldSkip(entity, timeScale)) {
            event.setCanceled(true);
        }
    }
}
