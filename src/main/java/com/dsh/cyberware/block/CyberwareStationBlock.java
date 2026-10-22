package com.dsh.cyberware.block;

import com.dsh.cyberware.Cyberware;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 义体操作台方块。
 *
 * <p>需求书【一】：右键才开界面（所以它实现 MenuProvider，由 BE 提供菜单）、
 * 离开 3 格自动关闭（在 {@code CyberwareStationMenu#stillValid} 里做）、
 * 不可被活塞推动（在方块属性里设 pushReaction）。
 */
public class CyberwareStationBlock extends BaseEntityBlock {

    public static final MapCodec<CyberwareStationBlock> CODEC = simpleCodec(CyberwareStationBlock::new);

    public CyberwareStationBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CyberwareStationBlockEntity(pos, state);
    }

    /**
     * 只有右击方块时才交出菜单 —— 这就是「取消快捷按键随地打开」的实现方式：
     * 菜单只能从这个方块拿到。
     */
    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof CyberwareStationBlockEntity station ? station : null;
    }

    /**
     * 右键打开界面。
     *
     * <p><b>关键</b>：26.x 里不能指望框架自动调用 {@code getMenuProvider} —— 原版方块
     * （工作台、熔炉等）都是在 {@code useWithoutItem} 里主动 {@code player.openMenu(...)}。
     * 少了这一段，右键表现就是「毫无反应」。
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        Cyberware.LOGGER.warn("[cyberware][probe] useWithoutItem 被调用 clientSide={}",
                level.isClientSide());
        if (!level.isClientSide()) {
            MenuProvider provider = this.getMenuProvider(state, level, pos);
            Cyberware.LOGGER.warn("[cyberware][probe] getMenuProvider -> {}", provider);
            if (provider != null) {
                Cyberware.LOGGER.warn("[cyberware][probe] openMenu 结果 = {}",
                        player.openMenu(provider));
            }
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * 右键入口探针。
     *
     * <p>26.1.2 里右键方块先走 {@code useItemOn}，再视手持物决定是否转 {@code useWithoutItem}
     * （两者都是 {@code BlockBehaviour} 上的 protected 默认实现，实测签名见方法注释）。
     * 这里只记一行、然后原样委托 —— 用来区分「右键根本没到方块」与「到了方块但没进 openMenu」。
     */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                         Player player, InteractionHand hand, BlockHitResult hitResult) {
        Cyberware.LOGGER.warn("[cyberware][probe] useItemOn 被调用 hand={} stack={} clientSide={}",
                hand, stack, level.isClientSide());
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }
}
