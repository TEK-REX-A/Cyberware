package com.dsh.cyberware.event;

import com.dsh.cyberware.core.OverclockState;
import com.dsh.cyberware.core.OverclockSystem;
import com.dsh.cyberware.core.RamSystem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * 击杀回 RAM（Fabric 版）。
 *
 * <p>判定逻辑与 NeoForge 版逐字一致，只是入口从 {@code LivingDeathEvent}
 * 换成 {@code ServerLivingEntityEvents.AFTER_DEATH}（实体 + 伤害源）。
 */
public final class RamKillHandler {

    /** 常态击杀回 RAM。TODO(主人裁决): 数值可调。 */
    private static final double KILL_RAM = 2.0D;
    /** 超频中击杀回 RAM（翻倍）。TODO(主人裁决): 数值可调。 */
    private static final double KILL_RAM_OVERCLOCKED = 4.0D;

    private RamKillHandler() {
    }

    public static void onLivingDeath(LivingEntity victim, DamageSource source) {
        if (victim instanceof Player) {
            return;   // 杀玩家不给（PVP 不刷 RAM）
        }
        if (victim instanceof Animal) {
            return;   // 动物排除
        }
        if (victim instanceof AbstractGolem) {
            return;   // 铁傀儡/雪傀儡排除
        }
        if (!(victim instanceof Enemy) && !(victim instanceof NeutralMob)) {
            return;   // 既不是敌意也不是中立 → 不给（村民落在这里）
        }
        if (!(source.getEntity() instanceof ServerPlayer killer)) {
            return;   // 只认玩家击杀
        }
        if (killer == victim) {
            return;
        }
        boolean overclocked = OverclockState.of(killer).isActive(OverclockSystem.serverTicks(killer));
        RamSystem.grant(killer, overclocked ? KILL_RAM_OVERCLOCKED : KILL_RAM);
    }
}
