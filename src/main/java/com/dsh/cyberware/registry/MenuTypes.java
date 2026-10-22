package com.dsh.cyberware.registry;

import com.dsh.cyberware.Cyberware;
import java.lang.reflect.Proxy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/**
 * 手工编译（无 AT/accessWidener 编译期支持）下的 {@link MenuType} 工厂。
 *
 * <p>{@code MenuType} 的构造与 {@code MenuType$MenuSupplier} 都是 private，
 * javac 直接 {@code new MenuType<>(...)} 会报 "has private access"。
 * 这里用 {@link Proxy} 在运行时实现那个私有函数式接口，再用反射调构造。
 *
 * <p><b>运行时</b>：Fabric Loader 会按 accessWidener 开放这些成员，
 * 反射与直接调用等价；本类只是让 javac 能过。
 */
public final class MenuTypes {

    private MenuTypes() {
    }

    /** 造一个 {@code MenuType<T>}；{@code factory} 收 (syncId, inventory) 返回菜单实例。 */
    @SuppressWarnings("unchecked")
    public static <T extends AbstractContainerMenu> MenuType<T> create(java.util.function.BiFunction<Integer, Inventory, T> factory,
                                                                      FeatureFlagSet flags) {
        Cyberware.LOGGER.warn("[cyberware][probe] MenuTypes.create 开始");
        try {
            Class<?> supplierType = Class.forName("net.minecraft.world.inventory.MenuType$MenuSupplier");
            Object supplier = Proxy.newProxyInstance(
                    MenuType.class.getClassLoader(),
                    new Class<?>[]{supplierType},
                    (proxy, method, args) -> {
                        if ("create".equals(method.getName()) && args != null && args.length == 2) {
                            Cyberware.LOGGER.warn("[cyberware][probe] MenuSupplier.create 被调用 syncId={}",
                                    args[0]);
                            T made = factory.apply((Integer) args[0], (Inventory) args[1]);
                            if (made == null) {
                                Cyberware.LOGGER.warn("[cyberware][probe] factory.apply 返回 null！");
                            }
                            return made;
                        }
                        Cyberware.LOGGER.warn("[cyberware][probe] MenuSupplier 未知方法 name={} argc={}",
                                method.getName(), args == null ? 0 : args.length);
                        return null;
                    });
            Cyberware.LOGGER.warn("[cyberware][probe] 反射造 MenuType 之前");
            MenuType<T> made = (MenuType<T>) ReflectiveAccess.newInstanceByNames(
                    MenuType.class,
                    new String[]{
                            "net.minecraft.world.inventory.MenuType$MenuSupplier",
                            "net.minecraft.world.flag.FeatureFlagSet"
                    },
                    supplier, flags);
            Cyberware.LOGGER.warn("[cyberware][probe] 反射造 MenuType 成功: {}", made);
            return made;
        } catch (Throwable t) {
            Cyberware.LOGGER.warn("[cyberware][probe] MenuTypes.create 失败", t);
            throw new IllegalStateException("MenuTypes.create failed", t);
        }
    }
}
