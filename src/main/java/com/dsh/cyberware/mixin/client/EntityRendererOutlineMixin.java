package com.dsh.cyberware.mixin.client;

import com.dsh.cyberware.Cyberware;
import com.dsh.cyberware.core.CyberwareInstallation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * <b>义眼敌我识别描边</b> —— 装了歧路司义眼的玩家，看生物时按关系套一层描边色：
 * <ul>
 *   <li><b>绿</b> {@link #CYBERWARE_FRIENDLY} = 友好：同一队伍，或属于你的驯服生物</li>
 *   <li><b>黄</b> {@link #CYBERWARE_NEUTRAL}  = 中立：其余一切生物（动物、村民、未结盟玩家……）</li>
 *   <li><b>红</b> {@link #CYBERWARE_HOSTILE}  = 敌对：原版 {@link Enemy} 标记，或正把你当目标的中立怪</li>
 * </ul>
 *
 * <h2>注入点</h2>
 * 目标方法（描述符逐字符来自 {@code javap -p -s}，见 OPTICS-MIXIN.md）：
 * <pre>
 * net.minecraft.client.renderer.entity.EntityRenderer
 *   public void extractRenderState(T, S, float)
 *   descriptor: (Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V
 * </pre>
 * 注入 {@code TAIL}。原版在 {@code EntityRenderer.java:283} 把 {@code state.outlineColor}
 * 赋成「发光时=队伍色，否则 0」——全树只有这一处写实体描边色（GUI/粒子里的同名变量互不相干，
 * 已用 {@code grep -rn "outlineColor *="} 全量核对），所以 TAIL 就是「最后写一次」的位置，
 * 不会被子类 {@code extractRenderState} 覆盖掉。
 *
 * <h2>为什么改了颜色真的会画出来</h2>
 * {@code EntityRenderState.appearsGlowing()} 就是 {@code outlineColor != 0}；
 * {@code LevelRenderer.extractVisibleEntities} 会在每个实体抽取后检查它，为真则把
 * {@code levelRenderState.haveGlowingEntities} 置真；随后 {@code submitEntities} 只在
 * {@code haveGlowingEntities == false} 时才把所有 {@code outlineColor} 清零。
 * 我们的注入发生在抽取阶段（{@code extractEntity -> createRenderState}），所以这个标记来得及被看到。
 *
 * <h2>安全措施</h2>
 * <ul>
 *   <li>{@code require = 0}：匹配不到就安静跳过（{@code injectors.defaultRequire = 1} 的教训），
 *       最坏结果只是「没有描边」，绝不会因为注入失败黑屏。</li>
 *   <li>整段 {@code try/catch(Throwable)}：渲染线程抛异常 = 当场崩客户端，
 *       装饰性功能不该让玩家买单。异常只记一次日志。</li>
 *   <li>认类型判断「是不是本地玩家」（{@code state instanceof AvatarRenderState} + 实体引用比较），
 *       不靠坐标——监守者贴脸不会骗到我们，也就不会有 ClassCastException。</li>
 * </ul>
 *
 * <p><b>未真机验证</b>：本类只保证编译通过；注入是否真的命中必须以真机日志为准
 * （关键字 {@code Mixin apply ... failed} / {@code InvalidInjectionException} / {@code Invalid name:}）。
 */
@Mixin(EntityRenderer.class)
public class EntityRendererOutlineMixin {

    /** 友好 · 绿 */
    private static final int CYBERWARE_FRIENDLY = 0xFF34D058;
    /** 中立 · 黄 */
    private static final int CYBERWARE_NEUTRAL = 0xFFFFCC00;
    /** 敌对 · 红 */
    private static final int CYBERWARE_HOSTILE = 0xFFFF3B30;

    /**
     * 装了这些型号中的任意一件，就启用敌我识别描边（面部槽的歧路司义眼系列）。
     *
     * <p><b>每条 id 的状态（0.3.12 逐条核对，当前 135 条生效定义）</b>：
     * <ul>
     *   <li><b>已注册 · 生效（6 条）</b>：{@code kiroshi_optics_bare}、{@code kiroshi_optics}、
     *       {@code kiroshi_optics_combined}、{@code kiroshi_optics_hunter}、
     *       {@code kiroshi_optics_wallhack}、{@code iconic_advanced_kiroshi_optics_bare}</li>
     *   <li><b>暂未注册 · 故意保留（2 条）</b>：{@code kiroshi_optics_piercing}（歧路司义眼追猎）与
     *       {@code kiroshi_optics_sensor}（歧路司义眼警戒）。这两个型号因为<b>没有贴图素材</b>，
     *       定义在 {@code CyberwareDefinitions.java} 里被整块注释掉、不在当前 135 条生效定义中
     *       （见 {@code ASSET-MISSING.txt} 第 6、7 条），所以这两条在这里 {@code has()} 恒为 false、
     *       永远不会命中。</li>
     * </ul>
     *
     * <p><b>为什么不把这两行删掉（方案 A 的理由）</b>：它们不是 bug，是「等素材」。留着，将来素材补齐、
     * 定义恢复注册时白名单不需要再改一次——改定义表的人不一定会想到回来改这个渲染 mixin；
     * 代价仅仅是每帧多两次必然为 false 的 {@code has()} 查询。反过来，删掉省下的两行，
     * 换来的是「恢复注册后描边悄悄不生效」这种难查的问题，不划算。
     *
     * <p>{@code mask_cw_plus_plus}（行为特征脸板）虽然也是 {@code CyberwareSlot.FACE}，
     * 但它是脸板不是眼睛，<b>故意不在白名单里</b>。
     */
    private static final String[] CYBERWARE_OPTICS_IDS = {
        "kiroshi_optics_bare",
        "kiroshi_optics",
        "kiroshi_optics_combined",
        "kiroshi_optics_hunter",
        // 待素材：定义已被整块注释、暂不注册（ASSET-MISSING.txt #6；CyberwareDefinitions.java 0.3.12 时点约 601-607 行）
        // 保留此条：素材补齐、定义恢复注册后自动生效，本文件无需再改
        "kiroshi_optics_piercing",
        // 待素材：定义已被整块注释、暂不注册（ASSET-MISSING.txt #7；CyberwareDefinitions.java 0.3.12 时点约 609-615 行）
        // 保留此条：素材补齐、定义恢复注册后自动生效，本文件无需再改
        "kiroshi_optics_sensor",
        "kiroshi_optics_wallhack",
        "iconic_advanced_kiroshi_optics_bare"
    };

    /** 渲染线程异常只报一次，避免每帧刷屏把日志冲爆 */
    private static boolean cyberware$outlineErrorLogged;

    @Inject(
        // ⚠ 方法名与左括号之间绝对不能有冒号（javap 显示用的冒号不是描述符的一部分）
        method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V",
        at = @At("TAIL"),
        require = 0
    )
    private void cyberware$applyFriendFoeOutline(
            Entity entity,
            EntityRenderState state,
            float partialTicks,
            CallbackInfo ci) {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            LocalPlayer viewer = minecraft == null ? null : minecraft.player;
            if (viewer == null || entity == null || state == null) {
                return;
            }
            // 自己身上不描边（第一人称/第三人称都跳过）
            if (entity == viewer) {
                return;
            }
            // 认类型再认 id——绝不靠坐标猜「这是不是本地玩家」
            if (state instanceof AvatarRenderState avatar && avatar.id == viewer.getId()) {
                return;
            }
            // 只给生物描边：掉落物/箭矢/矿车/画框套一圈黄边纯属刷屏
            if (!(entity instanceof LivingEntity target)) {
                return;
            }
            if (!cyberware$hasOptics(viewer)) {
                return;
            }
            int color = cyberware$relationColor(target, viewer);
            if (color != 0) {
                state.outlineColor = color;
            }
        } catch (Throwable t) {
            if (!cyberware$outlineErrorLogged) {
                cyberware$outlineErrorLogged = true;
                Cyberware.LOGGER.warn("[cyberware] 义眼敌我识别描边失败，已跳过（不影响游戏本体渲染）", t);
            }
        }
    }

    /**
     * 关系 → 颜色。
     *
     * <p>顺序有意义：先判「同盟」（同队/宠物），再判「敌对」，剩下的都算中立。
     * 未结盟的玩家按<b>中立</b>处理而不是敌对——服务器上大多数人不在一队，
     * 一律标红等于没信息，还会误导玩家对队友开枪。
     */
    private static int cyberware$relationColor(LivingEntity target, Player viewer) {
        if (viewer.isAlliedTo(target)) {
            return CYBERWARE_FRIENDLY;
        }
        if (target instanceof Enemy) {
            return CYBERWARE_HOSTILE;
        }
        if (target instanceof Mob mob && mob.getTarget() == viewer) {
            // 僵尸骷髅之外的「中立怪」被激怒后会把玩家设成目标，这里一并标红
            return CYBERWARE_HOSTILE;
        }
        return CYBERWARE_NEUTRAL;
    }

    /**
     * 本地玩家身上有没有装义眼。
     *
     * <p>走 <b>core 侧查询 API</b>：{@code CyberwareInstallation.has(Player, String)}
     * （内部用 {@code getExistingDataOrNull}，不存在的玩家不会被动创建空表，且永不返回 null）。
     * 本类<b>不实现任何玩家存储层</b>——存储与查询全在 core，这里只是调用方。
     *
     * <p>注意：附件是 {@code .sync(...)} 到客户端的，但 NeoForge 只把玩家自己的 Attachment
     * 同步给该玩家的客户端，所以这里只能可靠地查询<b>本地玩家自己</b>装了什么——
     * 本功能恰好只需要这一点。
     */
    private static boolean cyberware$hasOptics(Player viewer) {
        for (String id : CYBERWARE_OPTICS_IDS) {
            if (CyberwareInstallation.has(viewer, id)) {
                return true;
            }
        }
        return false;
    }
}
