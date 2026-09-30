package com.dsh.cyberware.client;

import com.dsh.cyberware.Cyberware;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/**
 * 按键绑定。
 *
 * <p>26.x 的 {@link KeyMapping} 多了一个 {@link KeyMapping.Category} 参数（原来只是字符串），
 * 所以要先建自己的分类再注册。
 */
public final class CyberwareKeys {

    /** 本模组的按键分类（显示在「选项 → 按键」里） */
    public static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(Cyberware.MODID, "main"));

    /** 激活持有的义体（需求书里斯安威斯坦等主动技能的触发键，默认 V） */
    public static final KeyMapping ACTIVATE = new KeyMapping(
            "key.cyberware.activate",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_V,
            CATEGORY);

    /**
     * 打开「已安装义体」轮盘（默认 R）。
     *
     * <p>V 键只能激活**手上**那件；装上身的义体要靠这个轮盘选。
     * 0.3.12 只注册键位：**触发逻辑（{@code consumeClick} → 打开轮盘界面）在 t14 接**，
     * 见 {@code cyberware-0312} 的任务 t14 / 界面 {@code client/CyberwareRadialScreen.java}。
     * 键位显示名需要 lang：{@code key.cyberware.radial}。
     */
    public static final KeyMapping RADIAL = new KeyMapping(
            "key.cyberware.radial",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_R,
            CATEGORY);

    /**
     * 脑机超频开关（默认 G，t25）。
     *
     * <p>客户端只发「请求切换」的 {@code OverclockPayload.TOGGLE}；能不能开
     * （装了网络接入仓吗？在冷却吗？）全部由服务端裁决，客户端不做任何判定。
     * 键位显示名需要 lang：{@code key.cyberware.overclock}。
     */
    public static final KeyMapping OVERCLOCK = new KeyMapping(
            "key.cyberware.overclock",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_G,
            CATEGORY);

    /**
     * 歧路司扫描（默认 X，t29，契约 §1.5）。
     *
     * <p>客户端只发 {@code HackPayload.scan()}（{@code action=SCAN / hackId="scan" /
     * targetEntityId=-1}）；装没装歧路司义眼、扫谁、持续多久全在服务端裁决
     * （未装会回 {@code REJECTED / NO_KIROSHI}）。键位显示名需要 lang：{@code key.cyberware.scan}。
     */
    public static final KeyMapping SCAN = new KeyMapping(
            "key.cyberware.scan",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_X,
            CATEGORY);

    private CyberwareKeys() {
    }
}
