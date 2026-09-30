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
     * 歧路司扫描 / 快速破解轮盘（默认 X，t29 新增，t36 改双行为）。
     *
     * <p><b>短按（按住 &lt; {@code 300ms} 松开）</b>= 歧路司扫描：发
     * {@code HackPayload.scan()}（{@code action=SCAN / hackId="scan" / targetEntityId=-1}）；
     * <b>长按（按住满 300ms）</b>= 立刻呼出快速破解轮盘（松手选择/取消）。
     * 阈值与状态机在 {@code CyberwareClient} 里（契约 STEP4 §3）。
     *
     * <p><b>t36 起没有 G 键</b>：脑机超频改成 R 轮盘里的一项（契约 §3 的键位最终形态）。
     * 键位显示名需要 lang：{@code key.cyberware.scan}。
     */
    public static final KeyMapping SCAN = new KeyMapping(
            "key.cyberware.scan",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_X,
            CATEGORY);

    private CyberwareKeys() {
    }
}
