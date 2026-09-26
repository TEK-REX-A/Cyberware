package com.dsh.cyberware.mixin.client;

import com.dsh.cyberware.client.ClientTimeDilation;
import com.dsh.cyberware.client.ParticleTickClock;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 时间减缓时把**粒子**也一起放慢。
 *
 * <p>第一版用的是「按比例跳过 tick」（和早期实体方案一样），结果就是一卡一卡 ——
 * 攒够一次才动一下，位移是跳变的。
 *
 * <p>现在和实体统一：**照常 tick，tick 完把这一 tick 的位移按倍率缩回去**。
 * 粒子自己的 {@code xo/yo/zo} 还停在 tick 起点，渲染插值从起点走到回拉后的位置，
 * 于是每一刻都只走一小步，是连续的慢，不是顿。
 *
 * <p>{@code age} 也顺手补偿：位置慢了但寿命照常流逝的话，粒子会「没走几步就没了」。
 * 这里用累加器把 (1 - timeScale) 攒起来，攒够 1 就替粒子少老一岁。
 */
@Mixin(Particle.class)
public abstract class ParticleMixin {

    @Shadow
    protected double x;
    @Shadow
    protected double y;
    @Shadow
    protected double z;
    @Shadow
    protected double xo;
    @Shadow
    protected double yo;
    @Shadow
    protected double zo;
    @Shadow
    private int age;

    /** 每个粒子自己的寿命补偿累加器（WeakHashMap 太慢，用粒子字段装不下，这里走静态表）。 */
    private static final java.util.Map<Particle, double[]> CYBERWARE$AGE = new java.util.WeakHashMap<>();

    @Inject(method = "tick", at = @At("RETURN"))
    private void cyberware$slowDownParticle(CallbackInfo ci) {
        Particle self = (Particle) (Object) this;
        double timeScale = ClientTimeDilation.timeScaleAt(this.x, this.y, this.z);
        if (timeScale >= 1.0D) {
            CYBERWARE$AGE.remove(self);
            return;
        }

        // 位移回拉
        double dx = this.x - this.xo;
        double dy = this.y - this.yo;
        double dz = this.z - this.zo;
        if (dx != 0.0D || dy != 0.0D || dz != 0.0D) {
            self.setPos(this.xo + dx * timeScale, this.yo + dy * timeScale, this.zo + dz * timeScale);
        }

        // 寿命补偿：让 age 也走得慢
        double[] acc = CYBERWARE$AGE.get(self);
        if (acc == null) {
            acc = new double[]{0.0D};
            CYBERWARE$AGE.put(self, acc);
        }
        acc[0] += (1.0D - timeScale);
        if (acc[0] >= 1.0D && this.age > 0) {
            int step = (int) acc[0];
            acc[0] -= step;
            this.age = Math.max(0, this.age - step);
        }
    }
}
