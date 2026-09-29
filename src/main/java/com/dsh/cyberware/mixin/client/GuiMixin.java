package com.dsh.cyberware.mixin.client;

import com.dsh.cyberware.client.BerserkClientState;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 狂暴期间隐藏「身体状态」HUD。
 *
 * <p>{@code extractPlayerHealth} 一次画完生命、护甲、食物、氧气 —— 从这里整块掐掉，
 * 快捷栏、准星、聊天都还在。玩家看不到自己的血条，只能看狂暴进度条，
 * 这就是「身体感知被抑制」。
 */
@Mixin(Gui.class)
public class GuiMixin {

    @Inject(method = "extractPlayerHealth", at = @At("HEAD"), cancellable = true, require = 0)
    private void cyberware$hideBodyStatusWhileBerserk(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (BerserkClientState.active()) {
            ci.cancel();
        }
    }
}
