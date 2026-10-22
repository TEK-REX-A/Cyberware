package com.dsh.cyberware.client;

import com.dsh.cyberware.Cyberware;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Map;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/**
 * 手工编译下的菜单界面注册（{@code MenuScreens.register} 是 private）。
 *
 * <p>运行时 Fabric Loader 会按 accessWidener 开放；本类只是让 javac 编译通过。
 */
public final class ScreenRegistrar {

    private ScreenRegistrar() {
    }

    /** 等价于 {@code MenuScreens.register(type, ctor)}。 */
    @SuppressWarnings("unchecked")
    public static <M extends AbstractContainerMenu, U extends Screen & net.minecraft.client.gui.screens.inventory.MenuAccess<M>>
    void register(MenuType<M> type, ScreenConstructorLike<M, U> ctor) {
        try {
            Class<?> ctorType = Class.forName("net.minecraft.client.gui.screens.MenuScreens$ScreenConstructor");
            Object proxy = Proxy.newProxyInstance(
                    MenuScreens.class.getClassLoader(),
                    new Class<?>[]{ctorType},
                    (p, method, args) -> {
                        if ("create".equals(method.getName()) && args != null && args.length == 3) {
                            return ctor.create((M) args[0], (net.minecraft.world.entity.player.Inventory) args[1], (net.minecraft.network.chat.Component) args[2]);
                        }
                        // ★ 2026-10-22 修：26.1.2 的 MenuScreens.create 走的是 fromPacket(...)，
                        //   而 fromPacket 是接口上的 **default 方法**。Proxy 会拦掉 default 方法，
                        //   旧实现只处理 create，于是 fromPacket 返回 null、方法体一行没跑 →
                        //   没有 setScreen → 操作台永远打不开，且不报错。
                        //   这里手动复刻 fromPacket 的默认实现。
                        if ("fromPacket".equals(method.getName()) && args != null && args.length == 4) {
                            net.minecraft.network.chat.Component title =
                                    (net.minecraft.network.chat.Component) args[0];
                            @SuppressWarnings("unchecked")
                            MenuType<M> menuType = (MenuType<M>) args[1];
                            net.minecraft.client.Minecraft mc =
                                    (net.minecraft.client.Minecraft) args[2];
                            int containerId = (Integer) args[3];
                            net.minecraft.world.entity.player.Inventory inv = mc.player.getInventory();
                            M menu = menuType.create(containerId, inv);
                            U screen = ctor.create(menu, inv, title);
                            mc.player.containerMenu =
                                    ((net.minecraft.client.gui.screens.inventory.MenuAccess<M>) screen).getMenu();
                            mc.setScreen(screen);
                            return null;
                        }
                        return null;
                    });
            Field f = MenuScreens.class.getDeclaredField("SCREENS");
            f.setAccessible(true);
            Map<MenuType<?>, Object> map = (Map<MenuType<?>, Object>) f.get(null);
            int before = map == null ? -1 : map.size();
            Cyberware.LOGGER.warn("[cyberware][probe] ScreenRegistrar: 取得 SCREENS, size={}, 准备塞 type={}",
                    before, type);
            if (map != null) {
                map.put(type, proxy);
                Cyberware.LOGGER.warn("[cyberware][probe] ScreenRegistrar: 塞入完成, size={} (增量 {})",
                        map.size(), map.size() - before);
            }
        } catch (Throwable t) {
            // 原来这里抛异常；菜单界面注册失败不该崩客户端，改成只记日志（宁留痕、不吞崩溃）
            Cyberware.LOGGER.warn("[cyberware][probe] ScreenRegistrar.register 失败: type={}", type, t);
        }
    }

    /** 与 {@code MenuScreens$ScreenConstructor} 同形的函数式接口。 */
    @FunctionalInterface
    public interface ScreenConstructorLike<M extends AbstractContainerMenu, U extends Screen> {
        U create(M menu, net.minecraft.world.entity.player.Inventory inventory, net.minecraft.network.chat.Component title);
    }
}
