package com.dsh.cyberware.mixin.client;

import com.dsh.cyberware.client.ClientTimeDilation;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 时间减缓时把世界的声音一起降调 —— 「连声音都变慢了」。
 *
 * <p>注入点是 {@code SoundEngine.calculatePitch(SoundInstance)}：它原本只是把
 * {@code instance.getPitch()} 夹到 [0.5, 2.0]。这里在**夹取之后**再乘一个系数，
 * 否则降调会被下界吃回去。
 *
 * <p>只在斯安威斯坦（时间减缓）生效，狂暴不动音调 —— 狂暴关掉的是痛觉，不是时间。
 */
@Mixin(SoundEngine.class)
public class SoundEngineMixin {

    /** 满强度时音调降到原来的多少（0.70 = 降 30%，明显低沉但不失真）。 */
    private static final float MAX_PITCH_DROP = 0.30F;
    /** 再低就会变成怪声，兜住下限。 */
    private static final float PITCH_FLOOR = 0.25F;

    @Inject(method = "calculatePitch", at = @At("RETURN"), cancellable = true)
    private void cyberware$lowerPitchWhileSlowed(SoundInstance instance, CallbackInfoReturnable<Float> cir) {
        float ratio = ClientTimeDilation.ratioNow();
        if (ratio <= 0.01F) {
            return;
        }
        float scale = 1.0F - MAX_PITCH_DROP * Math.min(1.0F, ratio);
        cir.setReturnValue(Math.max(PITCH_FLOOR, cir.getReturnValue() * scale));
    }
}
