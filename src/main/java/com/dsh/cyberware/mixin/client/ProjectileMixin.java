package com.dsh.cyberware.mixin.client;

import com.dsh.cyberware.client.ClientTimeDilation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 客户端也要把投射物慢下来 —— 否则敌人射来的箭看起来还是全速。
 *
 * <p>原因：{@code AbstractArrow} 这类投射物在**客户端也跑自己的物理**（本地预测），
 * 服务端每 tick 发来的位置只是"校正目标"。我们只在服务端做位置回拉时，
 * 客户端那套全速的本地模拟占了主导 —— 结果就是判定慢了、画面没慢。
 *
 * <p>做法和服务端/粒子完全一致：tick 结束后把这一 tick 的位移按倍率缩回去。
 * 用 {@code xo/yo/zo}（tick 起点）当基准，渲染插值依旧是连续的。
 *
 * <p>玩家自己射出去的仍然豁免 —— 自己没慢、箭却慢了就本末倒置。
 */
@Mixin(Projectile.class)
public abstract class ProjectileMixin {

    @Inject(method = "tick", at = @At("RETURN"))
    private void cyberware$slowProjectileOnClient(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!self.level().isClientSide()) {
            return;
        }
        if (self instanceof Projectile projectile && projectile.getOwner() instanceof Player) {
            return;
        }
        double timeScale = ClientTimeDilation.timeScaleAt(self.getX(), self.getY(), self.getZ());
        if (timeScale >= 1.0D) {
            return;
        }
        double dx = self.getX() - self.xo;
        double dy = self.getY() - self.yo;
        double dz = self.getZ() - self.zo;
        if (dx == 0.0D && dy == 0.0D && dz == 0.0D) {
            return;
        }
        self.setPos(self.xo + dx * timeScale,
                    self.yo + dy * timeScale,
                    self.zo + dz * timeScale);
    }
}
