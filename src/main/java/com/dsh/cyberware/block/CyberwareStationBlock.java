package com.dsh.cyberware.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
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
        if (!level.isClientSide()) {
            MenuProvider provider = this.getMenuProvider(state, level, pos);
            if (provider != null) {
                player.openMenu(provider);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
