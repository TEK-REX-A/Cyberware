package com.dsh.cyberware.client;

import com.dsh.cyberware.core.DilationTickGate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;

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

    /**
     * Fabric 移植：NeoForge 的 {@code EntityTickEvent.Pre} 没有等价事件
     * （本机实测：fabric-api 0.155.3 的模块清单里没有任何实体 tick 前事件），
     * 改为由 mixin {@code EntityTickSkipMixin} 注入 {@code Entity.tick()V} 的 HEAD 调用本方法。
     *
     * @return true = 这一 tick 该跳过（调用方执行 {@code ci.cancel()}）
     */
    public static boolean shouldCancelTick(Entity entity) {
        if (!(entity instanceof Projectile projectile)) {
            return false;
        }
        // 玩家自己射出的箭豁免：自己没慢、箭却慢了就本末倒置
        if (projectile.getOwner() instanceof Player) {
            return false;
        }
        double timeScale = ClientTimeDilation.timeScaleAt(
                entity.getX(), entity.getY(), entity.getZ());
        return DilationTickGate.shouldSkip(entity, timeScale);
    }
}
