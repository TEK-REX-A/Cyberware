package com.dsh.cyberware.client;

import com.dsh.cyberware.Cyberware;
import com.dsh.cyberware.registry.ModSounds;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;

/**
 * 斯安威斯坦的<b>开启音效</b>播放（t38）。
 *
 * <p>只在客户端播：由 {@link ClientTimeDilation#applyOnClient} 在**新建一条减速源**时调用，
 * 也就是「本地玩家自己按下技能、服务端广播回来」的那一刻 ——
 * 服务端每 20 刻的**刷新广播走的是另一个分支并提前 return**，所以这里天然不会每 tick / 每秒重复。
 * 减速结束、换维度、死亡都不经过这个方法。
 *
 * <p>音频素材是主人提供的第三方文件（{@code sounds/sandevistan_activate.ogg}，前 3 秒），
 * 不在模组代码的 AGPL-3.0 范围内 —— 见 {@code SOUND-NOTICE.md}。
 */
public final class SandevistanSounds {

    /** 保命日志去重：音效出问题只记一次，绝不刷屏 */
    private static final AtomicBoolean FAIL_LOGGED = new AtomicBoolean(false);

    private SandevistanSounds() {
    }

    /**
     * 播一次开启音效。
     *
     * <p>整段 try/catch：音效只是装饰，任何异常都不该影响游戏（与既有渲染回调同一套铁律）。
     */
    public static void playActivate() {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft == null) {
                return;
            }
            minecraft.getSoundManager().play(
                    SimpleSoundInstance.forUI(ModSounds.SANDEVISTAN_ACTIVATE.get(), 1.0F));
        } catch (Throwable t) {
            if (FAIL_LOGGED.compareAndSet(false, true)) {
                Cyberware.LOGGER.warn("[cyberware] 斯安威斯坦开启音效播放失败（已忽略）", t);
            }
        }
    }
}
