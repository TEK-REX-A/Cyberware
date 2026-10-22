package com.dsh.cyberware.client;

import java.lang.reflect.Field;
import net.minecraft.client.KeyMapping;

/**
 * 读 {@code KeyMapping} 当前绑定的物理键。
 *
 * <p>原版把 {@code key} 声明成 <b>protected</b>，NeoForge 靠 AT 放开、
 * 手工 javac 编不过，只能反射读。{@code getDefaultKey()} 不能用 —— 那是
 * 默认键位，玩家改键后它不跟着变。
 */
public final class KeyMappingKeys {

    private KeyMappingKeys() {
    }

    private static final Field KEY_FIELD;

    static {
        try {
            Field f = KeyMapping.class.getDeclaredField("key");
            f.setAccessible(true);
            KEY_FIELD = f;
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("KeyMapping.key not found", e);
        }
    }

    /** 拿到{@link com.mojang.blaze3d.platform.InputConstants.Key} 本身（KeyMapping.set 要它）。 */
    public static com.mojang.blaze3d.platform.InputConstants.Key get(KeyMapping mapping) {
        try {
            return (com.mojang.blaze3d.platform.InputConstants.Key) KEY_FIELD.get(mapping);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("read KeyMapping.key failed", e);
        }
    }

    public static int getValue(KeyMapping mapping) {
        try {
            var key = (com.mojang.blaze3d.platform.InputConstants.Key) KEY_FIELD.get(mapping);
            return key.getValue();
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("read KeyMapping.key failed", e);
        }
    }
}
