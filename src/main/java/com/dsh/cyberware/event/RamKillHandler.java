package com.dsh.cyberware.event;

import com.dsh.cyberware.core.OverclockState;
import com.dsh.cyberware.core.OverclockSystem;
import com.dsh.cyberware.core.RamSystem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * 击杀回 RAM（0.5.1 主人拍板）。
 *
 * <p>判定（全部在服务端，客户端不参与）：
 * <ol>
 *   <li>击杀者必须是 {@link ServerPlayer}（{@code source.getEntity()}）；</li>
 *   <li>受害者排除玩家、排除 {@link Animal}（狼/蜂/北极熊等）；</li>
 *   <li>受害者必须是 {@link Enemy}（僵尸/骷髅/掠夺者/猪灵…）**或** {@link NeutralMob}（末影人…）
 *       —— 村民、铁傀儡既不是 Enemy 也不是 NeutralMob，天然被排除，不需要额外判断；</li>
 *   <li>给多少：**超频中 +4、常态 +2**，由 {@link RamSystem#grant} 夹在 {@code [0, maxRam]}（满了不溢出）。</li>
 * </ol>
 *
 * <p>{@code Enemy} / {@link NeutralMob} / {@link Animal} 三个都是接口（javap 核实过），
 * 所以 {@code instanceof} 判断就是官方的「敌意 / 中立 / 动物」分类。
 */
public final class RamKillHandler {

    /** 常态击杀回 RAM。TODO(主人裁决): 数值可调。 */
    private static final double KILL_RAM = 2.0D;
    /** 超频中击杀回 RAM（翻倍）。TODO(主人裁决): 数值可调。 */
    private static final double KILL_RAM_OVERCLOCKED = 4.0D;

    private RamKillHandler() {
    }

    /** 注册见 {@code network/CyberwareNetwork#register}（显式注册，不用注解扫描）。 */
    public static void onLivingDeath(LivingDeathEvent event) {
        Entity victim = event.getEntity();
        if (victim.level().isClientSide()) {
            return;   // 只服务端算
        }
        if (victim instanceof Player) {
            return;   // 杀玩家不给（PVP 不刷 RAM）
        }
        if (victim instanceof Animal) {
            return;   // 动物排除
        }
        if (!(victim instanceof Enemy) && !(victim instanceof NeutralMob)) {
            return;   // 既不是敌意也不是中立 → 不给（村民/铁傀儡落在这里）
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) {
            return;   // 只认玩家击杀（环境伤害、生物互殴都不算）
        }
        if (killer == victim) {
            return;
        }
        boolean overclocked = OverclockState.of(killer).isActive(OverclockSystem.serverTicks(killer));
        RamSystem.grant(killer, overclocked ? KILL_RAM_OVERCLOCKED : KILL_RAM);
    }
}
