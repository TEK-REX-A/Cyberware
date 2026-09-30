package com.dsh.cyberware;

import com.dsh.cyberware.core.BerserkManager;
import com.dsh.cyberware.core.CyberwareDefinition;
import com.dsh.cyberware.core.CyberwareDefinitions;
import com.dsh.cyberware.core.CyberwareStats;
import com.dsh.cyberware.core.RamState;
import com.dsh.cyberware.core.RamSystem;
import com.dsh.cyberware.core.TimeDilationManager;
import com.dsh.cyberware.event.BerserkHandler;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * 调试命令（第二步临时用，等按键绑定做好后再决定是否保留）。
 *
 * <pre>
 * /cyberware sandevistan              用后继型号 C4 的数值触发一次减速
 * /cyberware dilate &lt;比例&gt; &lt;秒&gt;    自定义比例与时长，例如 /cyberware dilate 0.5 6
 * /cyberware stop                     停止**一切**义体主动效果：时间减缓 + 狂暴
 * /cyberware ram &lt;数值&gt;             把执行者当前 RAM 直接设成该值（夹在 [0, 上限]）—— t33 调试用
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
                    // t22：一句"停"必须停干净 —— 原来只清减速，狂暴会留着（语义不一致）
                    TimeDilationManager.clear();
                    BerserkManager.clear();

                    // 狂暴有客户端表现（HUD / 屏幕滤镜 / BerserkClientState）。
                    // 服务端清表后，BerserkHandler.broadcast 会因为 isActive()==false 而发出
                    // active=false 的 BerserkPayload —— 这是现有的"已结束"信号，不需要新增协议。
                    // 广播给所有在线玩家（而不是只给刚才有状态的人）：这个包对没在狂暴的客户端
                    // 是幂等的空操作，同时能覆盖"客户端本地状态比服务端表更旧"的情形。
                    for (ServerPlayer online : ctx.getSource().getServer().getPlayerList().getPlayers()) {
                        BerserkHandler.broadcast(online);
                    }

                    // 注意：时间减缓这一侧**没有**等价的"停止"信号可用（TimeDilationPayload 只有
                    // duration/ratio/区域，客户端 applyOnClient 只会把结束时间往后推，从不缩短），
                    // 所以这里不发减速包。客户端 HUD 的即时消失需要 client/ 侧改动，见 DEBT-CLEANUP-B.md §4。
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "[cyberware] 已停止：时间减缓 + 狂暴"), false);
                    return 1;
                }))
                .then(Commands.literal("ram")
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0D, 10_000.0D))
                                .executes(ctx -> {
                                    // t33 调试命令：方便主人真机验证 RAM 条 / 警告态 / 濒死超频 / 瘫痪四种状态。
                                    // 改的是**执行者自己**的 RAM（服务端附件），夹在 [0, max]。
                                    ServerPlayer player = ctx.getSource().getPlayer();
                                    if (player == null) {
                                        ctx.getSource().sendFailure(Component.literal(
                                                "[cyberware] /cyberware ram 只能由玩家执行（改的是执行者自己的 RAM）"));
                                        return 0;
                                    }
                                    double requested = DoubleArgumentType.getDouble(ctx, "value");
                                    double max = CyberwareStats.maxRam(player);
                                    double before = RamState.of(player).current();
                                    double after = Math.max(0.0D, Math.min(max, requested));
                                    RamState.set(player, after);   // setData 顺带触发附件同步
                                    RamSystem.sync(player);        // 再补一个 RamPayload：HUD 立刻拿到 max/regen/状态
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "[cyberware] RAM " + trim(before) + " → " + trim(after)
                                                    + "（上限 " + trim(max) + "，恢复 "
                                                    + trim(CyberwareStats.regenPerMinute(player)) + "/分钟）"), false);
                                    return 1;
                                }))));
    }

    /** 数值显示：整数不带小数点，小数保留两位（RAM 恢复是小数，进度会一直变）。 */
    private static String trim(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value)
                : String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}
