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

    private CyberwareKeys() {
    }
}
