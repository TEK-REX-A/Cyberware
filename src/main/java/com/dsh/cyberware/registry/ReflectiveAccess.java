package com.dsh.cyberware.registry;

import java.lang.reflect.Constructor;

/**
 * 手工编译（绕开 Loom/AT）时的反射小工具。
 *
 * <p>为什么需要：{@code BlockEntityType} / {@code MenuType} 的构造、
 * {@code MenuScreens.register} 在 26.1.2 里是 private/protected。
 * NeoForge 靠 AT 开放、Fabric 靠 accessWidener，但**手工 javac 编译期**
 * 这两者都不生效，只能走反射。
 *
 * <p>运行时：Fabric Loader 会按 {@code fabric.mod.json} 的 accessWidener 开放，
 * 反射调用与直接调用等价（这里只是编译期的绕路）。
 */
public final class ReflectiveAccess {

    private ReflectiveAccess() {
    }

    /** 调私有构造（参数类型按声明顺序给）。 */
    public static <T> T newInstance(Class<T> type, Class<?>[] paramTypes, Object... args) {
        try {
            Constructor<T> ctor = type.getDeclaredConstructor(paramTypes);
            ctor.setAccessible(true);
            return ctor.newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("reflective newInstance failed: " + type.getName(), e);
        }
    }

    /** 调私有静态方法。 */
    public static Object invokeStatic(Class<?> owner, String name, Class<?>[] paramTypes, Object... args) {
        try {
            var m = owner.getDeclaredMethod(name, paramTypes);
            m.setAccessible(true);
            return m.invoke(null, args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("reflective invokeStatic failed: " + owner.getName() + "#" + name, e);
        }
    }

    /** 按「类名字符串」调私有构造（用于 MenuSupplier 这类 private 内部接口）。 */
    public static <T> T newInstanceByNames(Class<T> type, String[] paramTypeNames, Object... args) {
        try {
            Class<?>[] types = new Class<?>[paramTypeNames.length];
            for (int i = 0; i < paramTypeNames.length; i++) {
                types[i] = Class.forName(paramTypeNames[i]);
            }
            Constructor<T> ctor = type.getDeclaredConstructor(types);
            ctor.setAccessible(true);
            return ctor.newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("reflective newInstanceByNames failed: " + type.getName(), e);
        }
    }

    /** 按「类名字符串」调私有静态方法。 */
    public static Object invokeStaticByNames(Class<?> owner, String name, String[] paramTypeNames, Object... args) {
        try {
            Class<?>[] types = new Class<?>[paramTypeNames.length];
            for (int i = 0; i < paramTypeNames.length; i++) {
                types[i] = Class.forName(paramTypeNames[i]);
            }
            var m = owner.getDeclaredMethod(name, types);
            m.setAccessible(true);
            return m.invoke(null, args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("reflective invokeStaticByNames failed: " + owner.getName() + "#" + name, e);
        }
    }
}
