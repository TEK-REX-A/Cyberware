package com.dsh.cyberware.mixin.client;

import java.util.List;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 取出 {@link PostChain} 内部的 pass 列表。
 *
 * <p>26.x 的 {@code PostChain} 没有公开访问器，而自定义后处理需要拿到 pass 才能改 UBO，
 * 所以这里开一个只读的口子。没有这个访问器，uniform 就只能在 JSON 里写死。
 */
@Mixin(PostChain.class)
public interface PostChainAccessor {

    @Accessor("passes")
    List<PostPass> getPasses();
}
