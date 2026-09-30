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

    /** 过热：点燃目标 + 降护甲。上传 1.0s。 */
    OVERHEAT("overheat", "过热", 20, 4) {
        @Override
        public void apply(ServerPlayer caster, LivingEntity target) {
            // TODO(主人填写): 3 秒 = 60 刻是占位值（邮件写「点燃目标 3 秒」）
            target.setRemainingFireTicks(60);
            // TODO(主人填写): 护甲 -4 是占位值（邮件只说「降低护甲」）
            CombatEffects.armorDebuff(target, 60, -4.0D);
            particles(target, ParticleTypes.FLAME, 12);
        }
    },

    /** 短路：高额瞬间伤害 + 电火花。上传 1.5s。 */
    SHORT_CIRCUIT("short_circuit", "短路", 30, 5) {
        @Override
        public void apply(ServerPlayer caster, LivingEntity target) {
            if (!(target.level() instanceof ServerLevel level)) {
                return;
            }
            // TODO(主人填写): 8 点伤害是占位值（邮件写「高额瞬间伤害」，没给数字）
            target.hurtServer(level, level.damageSources().indirectMagic(caster, caster), 8.0F);
            // 雷击粒子（原版粒子，不自造贴图）
            particles(target, ParticleTypes.CRIT, 20);
        }
    },

    /** 突触熔断：虚弱 + 缓慢 5 秒。上传 2.0s。 */
    SYNAPSE_BURNOUT("synapse_burnout", "突触熔断", 40, 3) {
        @Override
        public void apply(ServerPlayer caster, LivingEntity target) {
            // TODO(主人填写): 5 秒 = 100 刻、等级 1 是占位值
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
            particles(target, ParticleTypes.CRIT, 10);
        }
    },

    /** 武器故障：禁用远程攻击 8 秒（拦来源实体射出的投掷物，不改 AI 状态）。上传 1.2s。 */
    WEAPON_GLITCH("weapon_glitch", "武器故障", 24, 6) {
        @Override
        public void apply(ServerPlayer caster, LivingEntity target) {
            // TODO(主人填写): 8 秒 = 160 刻是占位值
            CombatEffects.weaponGlitch(target, 160);
        }
    },

    /** 系统重置：无法移动与攻击 3 秒（原版 setNoAi + 停导航，到期恢复）。上传 2.5s。 */
    SYSTEM_RESET("system_reset", "系统重置", 50, 8) {
        @Override
        public void apply(ServerPlayer caster, LivingEntity target) {
            // TODO(主人填写): 3 秒 = 60 刻是占位值
            CombatEffects.disableAi(target, 60);
        }
    };

    private final String id;
    private final String displayName;
    /** 上传刻数（20 刻 = 1 秒）。TODO(主人填写): 全部为占位值。 */
    private final int uploadTicks;
    /** RAM 占用。TODO(主人填写): 取自契约 §3 的占位表格。 */
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
