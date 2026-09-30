package com.dsh.cyberware.core;

import com.dsh.cyberware.Cyberware;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 破解的「会到期的实体状态」（服务端）。
 *
 * <p>为什么需要它：三条破解要的是「临时状态 + 到期恢复」——
 * ① 过热降护甲（护甲是属性修饰符，原版不会自己到期）；
 * ② 武器故障（标记，供 `EntityJoinLevelEvent` 拦投掷物）；
 * ③ 系统重置（原版 {@code setNoAi(true)} + 停导航，到期必须还回去）。
 * 弱点/缓慢这类**原版效果自带到期**（{@code MobEffectInstance}），不走这里。
 *
 * <h3>时钟与残留（0.3.12 规则 7）</h3>
 * 到期时刻用 {@link MinecraftServer#getTickCount()}（服务端全局单调计数，不用 {@code Level.getGameTime()}）。
 * 表是静态的，所以：① 每条记录**强引用**它作用的实体（生命周期 ≤ 8 秒，有界，不是泄漏）；
 * ② 实体被移除/死亡时当刻就恢复；③ 提供 {@link #clear()}；
 * ④ 服务器停止时 {@link #restoreAll()} 把 {@code noAi} / 负护甲**全部还原**，
 * 不留「永久卡死」的存档状态（清理注册见 {@code event/CombatEffectsHandler}）。
 */
public final class CombatEffects {

    /** 会到期的状态种类。 */
    private enum Kind {
        /** 护甲属性修饰符（到期摘掉） */
        ARMOR_DEBUFF,
        /** 武器故障标记（只做标记，投掷物拦截在事件层） */
        WEAPON_GLITCH,
        /** 原版 NoAI（到期还回去） */
        NO_AI
    }

    private record Entry(Kind kind, long expireAt, Identifier modifierId, Entity entity) {
    }

    private static final Map<UUID, List<Entry>> PENDING = new HashMap<>();

    /** 护甲减益用的修饰符 id（一条，按实体实例挂）。 */
    private static final Identifier ARMOR_MODIFIER_ID =
            Identifier.fromNamespaceAndPath(Cyberware.MODID, "hack_armor_debuff");

    private CombatEffects() {
    }

    /** 降护甲：挂一条负的 ADD_VALUE 修饰符，{@code ticks} 刻后摘掉。 */
    public static void armorDebuff(LivingEntity target, int ticks, double amount) {
        AttributeInstance armor = target.getAttribute(Attributes.ARMOR);
        if (armor == null) {
            return;
        }
        armor.removeModifier(ARMOR_MODIFIER_ID);   // 先摘旧的：重复破解不叠加
        armor.addTransientModifier(new AttributeModifier(
                ARMOR_MODIFIER_ID, amount, AttributeModifier.Operation.ADD_VALUE));
        add(target, new Entry(Kind.ARMOR_DEBUFF, expireAt(target, ticks), ARMOR_MODIFIER_ID, target));
    }

    /** 武器故障：只做标记，{@code ticks} 刻内该实体射出的投掷物会被拦掉（见事件层）。 */
    public static void weaponGlitch(LivingEntity target, int ticks) {
        add(target, new Entry(Kind.WEAPON_GLITCH, expireAt(target, ticks), null, target));
    }

    /** 该实体现在是否处于「武器故障」。 */
    public static boolean isWeaponGlitched(Entity entity) {
        List<Entry> list = entity == null ? null : PENDING.get(entity.getUUID());
        if (list == null) {
            return false;
        }
        for (Entry entry : list) {
            if (entry.kind() == Kind.WEAPON_GLITCH && !entry.entity().isRemoved()) {
                return true;
            }
        }
        return false;
    }

    /** 系统重置：原版 NoAI + 停导航（不动 AI 目标表，恢复天然平滑）。 */
    public static void disableAi(LivingEntity target, int ticks) {
        if (!(target instanceof Mob mob)) {
            return;   // 非 Mob（例如玩家/盔甲架）没有 AI 可禁 —— 不报错，只记条目（护甲类仍可用）
        }
        mob.setNoAi(true);
        mob.getNavigation().stop();
        add(mob, new Entry(Kind.NO_AI, expireAt(mob, ticks), null, mob));
    }

    /**
     * 每刻调用（由 {@code event/CombatEffectsHandler} 遍历所有非客户端实体驱动）：
     * 到期或实体已消失就恢复。
     */
    public static void tick(Entity entity) {
        List<Entry> list = PENDING.get(entity.getUUID());
        if (list == null) {
            return;
        }
        long now = serverTicks(entity);
        Iterator<Entry> it = list.iterator();
        while (it.hasNext()) {
            Entry entry = it.next();
            if (!entry.entity().isRemoved() && now < entry.expireAt()) {
                continue;
            }
            restore(entry);
            it.remove();
        }
        if (list.isEmpty()) {
            PENDING.remove(entity.getUUID());
        }
    }

    /** 服务器停止 / 换世界前：把所有状态还原（防「永久 noAi / 永久负护甲」）。 */
    public static void restoreAll() {
        for (List<Entry> list : PENDING.values()) {
            for (Entry entry : list) {
                restore(entry);
            }
        }
        PENDING.clear();
    }

    /** 只清表、不还原（给「实体已经没了」的场合兜底用）。 */
    public static void clear() {
        PENDING.clear();
    }

    private static void restore(Entry entry) {
        switch (entry.kind()) {
            case ARMOR_DEBUFF -> {
                if (entry.entity() instanceof LivingEntity living && entry.modifierId() != null) {
                    AttributeInstance armor = living.getAttribute(Attributes.ARMOR);
                    if (armor != null) {
                        armor.removeModifier(entry.modifierId());
                    }
                }
            }
            case NO_AI -> {
                if (entry.entity() instanceof Mob mob) {
                    mob.setNoAi(false);
                }
            }
            case WEAPON_GLITCH -> {
                // 只是标记；条目被移除即失效，没有要还原的东西
            }
        }
    }

    private static void add(Entity target, Entry entry) {
        PENDING.computeIfAbsent(target.getUUID(), key -> new ArrayList<>()).add(entry);
    }

    private static long expireAt(Entity entity, int ticks) {
        return serverTicks(entity) + Math.max(1, ticks);
    }

    private static long serverTicks(Entity entity) {
        MinecraftServer server = entity.level().getServer();
        return server == null ? 0L : server.getTickCount();
    }
}
