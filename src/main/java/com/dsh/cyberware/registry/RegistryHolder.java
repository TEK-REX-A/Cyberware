package com.dsh.cyberware.registry;

/**
 * 极简注册持有器 —— 替代 NeoForge 的 {@code DeferredHolder} / {@code DeferredItem} / {@code DeferredBlock}。
 *
 * <p>Fabric 没有延迟注册：注册发生在初始化时、当场拿到<b>实例</b>。但全项目几十处调用点都是
 * {@code ModXxx.YYY.get()}，为了不动别人 scope 里的调用点，这里保留一个只带 {@code get()} 的
 * 持有器 —— 语义与 {@code DeferredHolder.get()} 完全一致。
 *
 * @param <T> 被持有的注册对象类型
 */
public final class RegistryHolder<T> {

    private final T value;

    public RegistryHolder(T value) {
        this.value = value;
    }

    public T get() {
        return value;
    }
}
