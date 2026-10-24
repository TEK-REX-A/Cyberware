package com.dsh.cyberware.client;

import java.lang.reflect.Method;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Iris 软依赖适配层 —— <b>只回答一个问题</b>：「此刻是不是在 Iris 的阴影通道里？」
 *
 * <p>为什么要问：装了 Iris 时残影会被 shadow pass 再画一遍（主渲染一遍 + 阴影一遍），
 * 白白翻倍。这里只在阴影通道里跳过残影，主渲染照常显示。
 *
 * <h3>为什么用反射而不是编译期依赖</h3>
 * <ul>
 *   <li>玩家可能没装 Iris —— 硬依赖会让整个包起不来；</li>
 *   <li>Iris 可能在更新里挪走 API —— 编译期绑定会 {@code NoClassDefFoundError}，反射最多只是
 *       {@code catch} 到然后当作「不在阴影通道」（保留原行为）；</li>
 *   <li>工程约定<b>零第三方依赖</b>，所以 {@code build.gradle} 里不加 Iris。</li>
 * </ul>
 *
 * <p>类名/方法名来自 {@code javap iris-fabric-1.11.4+mc26.1.2.jar}：
 * {@code net.irisshaders.iris.api.v0.IrisApi#getInstance()}（静态）与
 * {@code #isRenderingShadowPass()}（实例）。
 *
 * <p>任何一步失败（没装 / 类不在 / getInstance 返回 null / 调用抛异常）都返回
 * {@code false}，也就是<b>行为与没装 Iris 时完全一致</b>。
 */
final class IrisCompat {

    /** Iris 的 API 入口（javap 实测的完整类名）。 */
    private static final String API_CLASS = "net.irisshaders.iris.api.v0.IrisApi";

    /** API 单例；null = 不可用（没装 / 加载失败）。 */
    private static final Object API;
    /** {@code isRenderingShadowPass()}；null = 不可用。 */
    private static final Method SHADOW_PASS;

    static {
        Object api = null;
        Method shadow = null;
        try {
            if (FabricLoader.getInstance().isModLoaded("iris")) {
                Class<?> apiClass = Class.forName(API_CLASS);
                // 接口上的静态方法：getInstance() -> IrisApi
                api = apiClass.getMethod("getInstance").invoke(null);
                if (api != null) {
                    shadow = apiClass.getMethod("isRenderingShadowPass");
                }
            }
        } catch (Throwable ignored) {
            // 老版本 Iris / 类被裁剪 / 模块访问受限 —— 一律当作「没有这个能力」
            api = null;
            shadow = null;
        }
        API = api;
        SHADOW_PASS = shadow;
    }

    /**
     * 当前是否在 Iris 的阴影通道。
     *
     * @return {@code true} 只有「Iris 在场 + API 拿得到 + 此刻确实在 shadow pass」三条同时成立；
     *         其余任何情况都返回 {@code false}（= 照原样渲染）
     */
    static boolean isShadowPass() {
        if (API == null || SHADOW_PASS == null) {
            return false;
        }
        try {
            Object result = SHADOW_PASS.invoke(API);
            return result instanceof Boolean value && value;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private IrisCompat() {
    }
}

