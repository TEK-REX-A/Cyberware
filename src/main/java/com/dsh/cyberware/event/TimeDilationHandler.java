package com.dsh.cyberware.event;

import com.dsh.cyberware.config.CyberwareConfig;
import com.dsh.cyberware.core.DilationTickGate;
import com.dsh.cyberware.core.TimeDilationManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * 时间减缓 · 服务端逻辑层 —— <b>位置回拉</b>（生物与投射物都用这一套）。
 *
 * <p>实体照常 tick，tick 结束后把它这一 tick 的**位移**按倍率缩回去：
 * <pre>
 *   p0 = (xo, yo, zo)        // tick 起始位置（vanilla 自己记的）
 *   p1 = (getX(), ...)       // tick 结束时位置
 *   setPos(p0 + (p1 - p0) * timeScale)
 * </pre>
 * 于是「停 4 刻、窜 1 刻」变成「每刻一小步」—— 客户端插值出来是连续的慢动作。
 *
 * <p><b>跳 tick 那一套已经全部拆掉了。</b>它对生物是顿挫的来源（位移跳变，客户端插值
 * 一步一顿），对投射物更是坏在另一处：客户端会<b>自己 tick 投射物</b>（按它自己的
 * {@code deltaMovement} 推进），服务端位置包只是事后校正 —— 服务端跳得再准，玩家看到的
 * 还是客户端那支原速的箭。现在改成：服务端位置回拉（连续小位移，包每刻都发，客户端插值平滑），
 * 客户端那边由 {@code ClientProjectileDilation} 把本地物理推进按住，位置只认服务端。
 *
 * <p>副作用与对策：
 * <ul>
 *   <li>AI / 寻路仍是原速决策 —— 它们会一直「想走却走不快」，这正是慢动作该有的样子。</li>
 *   <li>投射物箭程补偿：位移被缩了但寿命没变，会飞一半掉下来。这里把 {@code (1 - timeScale)}
 *       攒起来替它少老一刻，射程就回来了。</li>
 *   <li>载着玩家的坐骑不动 —— 否则玩家的视点会被每 tick 拽回去。</li>
 * </ul>
 */
public final class TimeDilationHandler {

    private TimeDilationHandler() {
    }

    /** 上次刷新减速区域的游戏刻（每 20 刻刷一次）。 */
    private static long lastRefreshTick = Long.MIN_VALUE;

    /**
     * 参与减速的对象与豁免规则。
     *
     * <ul>
     *   <li>玩家本人不受影响（位置由客户端权威，也不能拽）。</li>
     *   <li>载着玩家的船 / 马 / 矿车豁免 —— 拉回去会把玩家的视点一起拽动。</li>
     *   <li>生物与投射物参与。</li>
     *   <li>玩家射出的投射物豁免：自己没慢、箭却慢了就本末倒置。</li>
     * </ul>
     */
    private static boolean shouldAffect(Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return false;
        }
        if (entity instanceof Player) {
            return false;
        }
        return !(entity.getFirstPassenger() instanceof Player);
    }

    /**
     * 投射物：跳 tick（时间真·不流逝）。这里是**唯一**还在跳 tick 的地方。
     *
     * <p>为什么投射物不能用位置回拉（主人实测的两个现象就是证据）：
     * 箭的物理是「每刻走完这一 tick 的位移 + 沿途做碰撞」。回拉只改了**位置**，
     * 改不掉 tick 内部已经走完的那 3 格 —— 于是箭在服务端该撞的照样撞、该插地的照样插地，
     * 而同步出去的位置却停在起点。主人看到的「完全时停」是这么来的；
     * 减速一结束，客户端追上服务端的真实落点，就是那下「瞬移」。
     *
     * <p>跳 tick 才是物理自洽的：整刻不执行 = 时间没流逝，位置、碰撞、寿命一起慢。
     * 客户端那边由 {@code ClientProjectileDilation} 按住本地物理推进，位置只认服务端包。
     */
    public static void onEntityTickPre(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        if (!(entity instanceof Projectile projectile)) {
            return;
        }
        // 玩家自己射出的箭豁免：自己没慢、箭却慢了就本末倒置
        if (projectile.getOwner() instanceof Player) {
            return;
        }
        double timeScale = TimeDilationManager.timeScaleFor(entity);
        if (DilationTickGate.shouldSkip(entity, timeScale)) {
            event.setCanceled(true);
        }
    }

    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();

        if (entity.level().isClientSide()) {
            return;
        }

        // 周期性把减速区域挪到施法者脚下，并同步给客户端
        long gameTime = entity.level().getGameTime();
        if (gameTime % 20L == 0L && lastRefreshTick != gameTime) {
            lastRefreshTick = gameTime;
            TimeDilationManager.refresh(entity.level());
            BerserkHandler.refresh(entity.level());
        }

        if (!shouldAffect(entity)) {
            return;
        }

        double timeScale = TimeDilationManager.timeScaleFor(entity);
        if (timeScale >= 1.0D) {
            return;
        }

        // 无敌帧清零（主人指定）
        if (entity instanceof LivingEntity living && living.invulnerableTime > 0) {
            living.invulnerableTime = 0;
        }

        // ---- 位置回拉：把这一 tick 的位移按倍率缩回去 ----
        double dx = entity.getX() - entity.xo;
        double dy = entity.getY() - entity.yo;
        double dz = entity.getZ() - entity.zo;
        if (dx == 0.0D && dy == 0.0D && dz == 0.0D) {
            return;
        }
        entity.setPos(entity.xo + dx * timeScale,
                      entity.yo + dy * timeScale,
                      entity.zo + dz * timeScale);
    }

    /**
     * 子弹时间：斯安威斯坦激活期间，弹射物打不中你。
     *
     * <p>判定看两个位置：{@code getDirectEntity()} 是命中你的那个实体（箭矢、火球），
     * {@code getEntity()} 是它的主人。两个都查，覆盖两种伤害源写法。
     * 不想要就把配置里的 {@code projectileImmune} 关掉。
     */
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!CyberwareConfig.TIME_DILATION_PROJECTILE_IMMUNE.get()) {
            return;
        }
        if (!TimeDilationManager.isOwnDilationActive(player)) {
            return;
        }
        DamageSource source = event.getSource();
        boolean fromProjectile = source.getDirectEntity() instanceof Projectile
                || source.getEntity() instanceof Projectile;
        if (!fromProjectile) {
            return;
        }
        event.setCanceled(true);
    }

    /**
     * 击杀延长：斯安威斯坦生效期间，玩家每击杀一个敌人就把持续时间往后推一段。
     *
     * <p>只有「开减速的那个人自己」击杀才算 —— 队友杀敌不该给自己的义体续杯。
     */
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!CyberwareConfig.TIME_DILATION_KILL_EXTEND_ENABLED.get()) {
            return;
        }
        Entity killer = event.getSource().getEntity();
        if (!(killer instanceof ServerPlayer player) || event.getEntity() == player) {
            return;
        }
        int added = TimeDilationManager.extend(player,
                CyberwareConfig.TIME_DILATION_KILL_EXTEND_TICKS.get(),
                CyberwareConfig.TIME_DILATION_KILL_EXTEND_LIMIT.get());
        if (added > 0) {
            player.sendOverlayMessage(Component.literal("斯安威斯坦 +" + trimSeconds(added) + "s")
                    .withStyle(ChatFormatting.AQUA));
        }
    }

    private static String trimSeconds(int ticks) {
        double seconds = ticks / 20.0D;
        return seconds == Math.floor(seconds)
                ? String.valueOf((long) seconds)
                : String.format(java.util.Locale.ROOT, "%.1f", seconds);
    }
}
