package com.dsh.cyberware.core;

import com.dsh.cyberware.data.CyberwareData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 义体主动能力的统一入口。
 *
 * <p>第二步先支持「时间减缓」这一条链路（斯安威斯坦）。
 * 网络接入仓的扫描、狂暴的增益等，按同一模式在此扩展。
 */
public final class CyberwareAbilities {

    private CyberwareAbilities() {
    }

    /**
     * 激活一件义体（以服务端权威数据为准）。
     *
     * @return 是否真的触发了什么
     */
    public static boolean activate(Player player, CyberwareDefinition def, CyberwareData data) {
        if (def == null) {
            return false;
        }
        CyberwareDefinition.Variant variant = def.variantFor(data.rarity());
        if (variant == null) {
            // 该型号没有这个稀有度，回落到它最低的一档，避免"拿了神话版却没效果"
            variant = def.baseVariant();
        }
        if (variant == null) {
            return false;
        }

        double ratio = variant.stat(CyberwareDefinition.Stats.TIME_SLOW, 0.0D);
        if (ratio > 0.0D) {
            int seconds = (int) Math.round(variant.stat(CyberwareDefinition.Stats.DURATION, 8.0D));
            double radius = variant.stat("radius", TimeDilationManager.DEFAULT_RADIUS);
            TimeDilationManager.activate(player, def.id(), ratio, seconds * 20, radius);
            // 26.x：displayClientMessage 已改为 sendOverlayMessage（动作栏）
            player.sendOverlayMessage(Component.literal(
                    "§b[义体] " + def.displayName() + " §7— 时间减缓 §f" + Math.round(ratio * 100)
                            + "%§7，持续 §f" + seconds + "§7 秒"));
            return true;
        }

        // —— 狂暴：把痛觉和疲劳一起关掉 ——
        if (def.id().startsWith("berserk_")) {
            int seconds = (int) Math.round(variant.stat(CyberwareDefinition.Stats.DURATION,
                    com.dsh.cyberware.config.CyberwareConfig.BERSERK_DURATION_SECONDS.get()));
            double multiplier = com.dsh.cyberware.config.CyberwareConfig.BERSERK_DAMAGE_MULTIPLIER.get();
            BerserkManager.activate(player, multiplier, Math.max(1, seconds) * 20);
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                com.dsh.cyberware.event.BerserkHandler.broadcast(serverPlayer);
            }
            player.sendOverlayMessage(Component.literal(
                    "§c[义体] " + def.displayName() + " §7— 狂暴 §f" + seconds
                            + "§7 秒，伤害 §f×" + trim(multiplier) + "§7，期间无敌"));
            return true;
        }

        // 其他类型（网络接入仓）在此扩展
        player.sendOverlayMessage(Component.literal(
                "§e[义体] " + def.displayName() + " §7暂未实现主动效果"));
        return false;
    }

    private static String trim(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value)
                : String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    /** 从玩家手持物品取义体并激活。 */
    public static boolean activateHeld(Player player) {
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof com.dsh.cyberware.item.CyberwareItem item) {
            return activate(player, item.definition(), com.dsh.cyberware.item.CyberwareItem.dataOf(held));
        }
        return false;
    }
}
