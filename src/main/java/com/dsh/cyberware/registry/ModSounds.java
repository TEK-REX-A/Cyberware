package com.dsh.cyberware.registry;

import com.dsh.cyberware.Cyberware;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * 音效注册表（t38）。
 *
 * <p>素材：{@code assets/cyberware/sounds/sandevistan_activate.ogg}（主人提供，不在 AGPL 范围）。
 * 用可变距离事件：播放侧走「本地玩家自己的技能音效」，不因距离判定被丢掉。
 */
public final class ModSounds {

    public static RegistryHolder<SoundEvent> SANDEVISTAN_ACTIVATE;

    /** 激活期击杀音效（0.5s，素材 kill_c 的副本）。 */
    public static RegistryHolder<SoundEvent> SANDEVISTAN_KILL;

    private ModSounds() {
    }

    public static void register() {
        Identifier id = Identifier.fromNamespaceAndPath(Cyberware.MODID, "sandevistan_activate");
        SANDEVISTAN_ACTIVATE = new RegistryHolder<>(Registry.register(
                BuiltInRegistries.SOUND_EVENT,
                id,
                SoundEvent.createVariableRangeEvent(id)));

        Identifier killId = Identifier.fromNamespaceAndPath(Cyberware.MODID, "sandevistan_kill");
        SANDEVISTAN_KILL = new RegistryHolder<>(Registry.register(
                BuiltInRegistries.SOUND_EVENT,
                killId,
                SoundEvent.createVariableRangeEvent(killId)));
    }
}
