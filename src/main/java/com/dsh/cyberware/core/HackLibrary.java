package com.dsh.cyberware.core;

import com.dsh.cyberware.Cyberware;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * 快速破解库（服务端效果）。5 条，数值全部是**占位值**，等主人调 —— 见 {@link Hack#apply}。
 *
 * <p>数值口径（RAM 占用）取自实施契约 `RAM-SYSTEM-SPEC.md` §3 的表格；上传时长取邮件给的值。
 */
public enum HackLibrary {

    /** 过热：点燃目标 + 降护甲。上传 1.0s（2077 官方 2.0s，本仓库取可玩下限 1.0s，见 HACK-VALUES §3.3）。 */
    OVERHEAT("overheat", "过热", 20, 4) {
        @Override
        public void apply(ServerPlayer caster, LivingEntity target) {
            // 邮件IX §四.4：「点燃目标3秒」→ 3 秒 = 60 刻（2077 官方 Duration 2s，按邮件口径优先）
            target.setRemainingFireTicks(60);
            // 2077 gamestegy Overheat T4「Melts enemy armor over time (max. -40%)」；
            // 本仓库 armorDebuff 是 ADD_VALUE 平值，-40% 无法直填 → 主人裁决 A2 定为 -5 平值
            CombatEffects.armorDebuff(target, 60, -5.0D);
            particles(target, ParticleTypes.FLAME, 12);
        }
    },

    /** 短路：高额瞬间伤害 + 电火花。上传 1.0s（2077 官方 0.5s；本仓库 1.0s 下限，见 HACK-VALUES §3.2/§3.3）。 */
    SHORT_CIRCUIT("short_circuit", "短路", 20, 5) {
        @Override
        public void apply(ServerPlayer caster, LivingEntity target) {
            if (!(target.level() instanceof ServerLevel level)) {
                return;
            }
            // 2077 gamestegy Short Circuit：Damage 260（2077 量纲）→ 主人裁决 A3 定为 MC 量纲 10.0F
            target.hurtServer(level, level.damageSources().indirectMagic(caster, caster), 10.0F);
            // 雷击粒子（原版粒子，不自造贴图）
            particles(target, ParticleTypes.CRIT, 20);
        }
    },

    /** 突触熔断：−24 HP + 凋零 IV 6 秒（0.5.1 主人拍板**替换**原「缓慢+虚弱」）。上传 2.0s。 */
    SYNAPSE_BURNOUT("synapse_burnout", "突触熔断", 40, 3) {
        @Override
        public void apply(ServerPlayer caster, LivingEntity target) {
            if (!(target.level() instanceof ServerLevel level)) {
                return;
            }
            // 0.5.1 主人拍板：−24 HP（indirectMagic 归因给施法者）+ 凋零 120 刻 等级 3（= 凋零 IV）
            target.hurtServer(level, level.damageSources().indirectMagic(caster, caster), 24.0F);
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 120, 3));
            particles(target, ParticleTypes.CRIT, 10);
        }
    },

    /** 武器故障：禁用远程攻击 8 秒（拦来源实体射出的投掷物，不改 AI 状态）。上传 1.2s。 */
    WEAPON_GLITCH("weapon_glitch", "武器故障", 24, 6) {
        @Override
        public void apply(ServerPlayer caster, LivingEntity target) {
            // 邮件IX §四.4：「禁用目标远程攻击AI逻辑8秒」→ 160 刻
            CombatEffects.weaponGlitch(target, 160);
        }
    },

    /** 系统重置：无法移动与攻击 3 秒（原版 setNoAi + 停导航，到期恢复）。上传 2.5s。 */
    SYSTEM_RESET("system_reset", "系统重置", 50, 8) {
        @Override
        public void apply(ServerPlayer caster, LivingEntity target) {
            // 邮件IX §四.4：「目标无法移动与攻击3秒」→ 60 刻；RAM 8 = 邮件IX「消耗极高，如8点RAM」
            CombatEffects.disableAi(target, 60);
        }
    };

    private final String id;
    private final String displayName;
    /**
     * 上传刻数（20 刻 = 1 秒）。
     *
     * <p>出处：过热/短路 = 2077 gamestegy 官方值（2.0s / 0.5s）取本仓库 1.0s 下限；
     * 突触熔断 / 武器故障 / 系统重置 = {@code TODO(待主人裁决: A1 2077 数值未能取得，见 HACK-VALUES.md §4)}。
     */
    private final int uploadTicks;
    /**
     * RAM 占用。
     *
     * <p>出处：系统重置 8 = 邮件IX「消耗极高，如8点RAM」；其余为契约 §3 占位表
     * —— 2077 满档值（过热 9 / 短路 10）在「默认上限 8」下会让破解放不出来，故不照抄（HACK-VALUES §3.1）；
     * {@code TODO(待主人裁决: A1/A4 突触熔断 3、武器故障 6 的 2077 值未能取得)}。
     */
    private final int ramCost;

    HackLibrary(String id, String displayName, int uploadTicks, int ramCost) {
        this.id = id;
        this.displayName = displayName;
        this.uploadTicks = uploadTicks;
        this.ramCost = ramCost;
    }

    /** 上传完成、目标校验通过后才会调用这里（服务端唯一的效果入口）。 */
    public abstract void apply(ServerPlayer caster, LivingEntity target);

    public String id() {
        return this.id;
    }

    public String displayName() {
        return this.displayName;
    }

    public int uploadTicks() {
        return this.uploadTicks;
    }

    public int ramCost() {
        return this.ramCost;
    }

    /** 按 id 查；未知返回 null（客户端传来的 id 一律当不可信）。 */
    public static HackLibrary byId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        for (HackLibrary hack : values()) {
            if (hack.id.equals(id)) {
                return hack;
            }
        }
        return null;
    }

    /** 服务端在原版粒子上加个特效（不新增贴图）。 */
    private static void particles(LivingEntity target, net.minecraft.core.particles.SimpleParticleType type, int count) {
        if (target.level() instanceof ServerLevel level) {
            level.sendParticles(type, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                    count, 0.4D, 0.5D, 0.4D, 0.02D);
        }
    }
}
