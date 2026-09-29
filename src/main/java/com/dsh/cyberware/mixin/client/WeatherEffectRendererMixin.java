package com.dsh.cyberware.mixin.client;

import com.dsh.cyberware.client.WeatherTickClock;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 时间减缓时让雨雪也慢下来。
 *
 * <p>关键在**两个参数一起改**：{@code ticks} 取虚拟时间的整数部分、{@code partialTick}
 * 取小数部分。只改其中一个的话，另一个仍按原速走，雨滴就会「不动 → 跳一格」地一顿一顿。
 */
@Mixin(WeatherEffectRenderer.class)
public class WeatherEffectRendererMixin {

    /** 参数顺序：(Level level, int ticks, float partialTick, Vec3 cameraPos, WeatherRenderState state) */
    @ModifyVariable(method = "extractRenderState", at = @At("HEAD"), argsOnly = true, index = 2, require = 0)
    private int cyberware$slowWeatherTicks(int ticks) {
        // partialTick 还没轮到我们改，这里先用 0 近似取整；下一处会补齐小数
        return WeatherTickClock.virtualTickPart(ticks, 0.0F);
    }

    @ModifyVariable(method = "extractRenderState", at = @At("HEAD"), argsOnly = true, index = 3, require = 0)
    private float cyberware$slowWeatherPartial(float partialTick) {
        return WeatherTickClock.virtualPartialPart(partialTick, partialTick);
    }
}
