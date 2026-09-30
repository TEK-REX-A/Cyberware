package com.dsh.cyberware.registry;

import com.dsh.cyberware.Cyberware;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 音效注册表（t38）。
 *
 * <p>目前只有一条：斯安威斯坦的<b>开启音效</b>
 * （{@code assets/cyberware/sounds/sandevistan_activate.ogg}，由主人提供、captain 切好的前 3 秒）。
 *
 * <p>注册对象必须挂在 <b>mod 事件总线</b>上（{@code modEventBus}），
 * 这一点在 {@link Cyberware} 的构造函数里做 —— 挂错总线会静默不注册。
 *
 * <p><b>版权</b>：这段音频是主人提供的第三方素材，<b>不在模组代码的 AGPL-3.0 范围内</b>；
 * 将来发帖时要在 NOTICE 里单列（与 32×32 贴图同一类处理）。详见 {@code SOUND-NOTICE.md}。
 */
public final class ModSounds {

    /** 音效注册表（id 前缀 = cyberware） */
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, Cyberware.MODID);

    /**
     * 斯安威斯坦开启音效 —— id 是 {@code cyberware:sandevistan_activate}，
     * 与 {@code sounds.json} 里的键、以及 {@code sounds/sandevistan_activate.ogg} 三者同名。
     *
     * <p>用可变距离事件（{@code createVariableRangeEvent}）而不是固定距离：
     * 播放侧走的是「本地玩家自己的技能音效」，可变距离由音量/衰减自行决定，
     * 不会因为距离判定而被丢掉。
     */
    public static final DeferredHolder<SoundEvent, SoundEvent> SANDEVISTAN_ACTIVATE =
            SOUNDS.register("sandevistan_activate",
                    () -> SoundEvent.createVariableRangeEvent(
                            Identifier.fromNamespaceAndPath(Cyberware.MODID, "sandevistan_activate")));

    private ModSounds() {
    }
}
