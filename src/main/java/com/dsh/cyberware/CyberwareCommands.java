package com.dsh.cyberware;

import com.dsh.cyberware.core.CyberwareDefinition;
import com.dsh.cyberware.core.CyberwareDefinitions;
import com.dsh.cyberware.core.TimeDilationManager;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * 调试命令（第二步临时用，等按键绑定做好后再决定是否保留）。
 *
 * <pre>
 * /cyberware sandevistan              用「泽塔科技·斯安威斯坦·普通」的数值触发一次减速
 * /cyberware dilate &lt;比例&gt; &lt;秒&gt;    自定义比例与时长，例如 /cyberware dilate 0.5 6
 * /cyberware stop                     立刻结束减速
 * </pre>
 */
public final class CyberwareCommands {

    private CyberwareCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("cyberware")
                // 26.x：hasPermission(int) 已废弃，改用 PermissionSet + Permissions 常量
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("sandevistan").executes(ctx -> {
                    net.minecraft.server.level.ServerPlayer player = ctx.getSource().getPlayer();
                    if (player == null) {
                        return 0;
                    }
                    // 0.3.12：旧占位定义 SANDEVISTAN_ZETATECH 已删除，用后继型号 C4（captain 授权的范围外修正）
                    CyberwareDefinition def = CyberwareDefinitions.SANDEVISTAN_C4;
                    CyberwareDefinition.Variant v = def.baseVariant();
                    if (v == null) {
                        return 0;
                    }
                    double ratio = v.stat(CyberwareDefinition.Stats.TIME_SLOW, 0.25);
                    int seconds = (int) v.stat(CyberwareDefinition.Stats.DURATION, 8);
                    TimeDilationManager.activate(player, def.id(), ratio, seconds * 20,
                            TimeDilationManager.DEFAULT_RADIUS);
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "[cyberware] " + def.displayName() + " 触发：时间减缓 "
                                    + Math.round(ratio * 100) + "%，持续 " + seconds + " 秒"), false);
                    return 1;
                }))
                .then(Commands.literal("dilate")
                        .then(Commands.argument("ratio", DoubleArgumentType.doubleArg(0.01D, 0.99D))
                                .then(Commands.argument("seconds", DoubleArgumentType.doubleArg(0.5D, 120.0D))
                                        .executes(ctx -> {
                                            net.minecraft.server.level.ServerPlayer player = ctx.getSource().getPlayer();
                                            if (player == null) {
                                                return 0;
                                            }
                                            double ratio = DoubleArgumentType.getDouble(ctx, "ratio");
                                            double seconds = DoubleArgumentType.getDouble(ctx, "seconds");
                                            TimeDilationManager.activate(player,
                                                    "debug", ratio, (int) (seconds * 20),
                                                    TimeDilationManager.DEFAULT_RADIUS);
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "[cyberware] 时间减缓 " + Math.round(ratio * 100)
                                                            + "%，持续 " + seconds + " 秒"), false);
                                            return 1;
                                        }))))
                .then(Commands.literal("stop").executes(ctx -> {
                    TimeDilationManager.clear();
                    ctx.getSource().sendSuccess(() -> Component.literal("[cyberware] 时间减缓已清除"), false);
                    return 1;
                })));
    }
}
