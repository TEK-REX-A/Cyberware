package com.dsh.cyberware.mixin.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import java.util.Map;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 取出 {@link PostPass} 的自定义 uniform 缓冲表。
 *
 * <p>26.x 把 pass 的 uniform 在构造时就烤进了 {@code GpuBuffer}，之后没有任何公开的
 * setter。拿到这张表之后，就能用一个新的 buffer 顶替旧的来更新数值 —— 这是
 * 「强度随激活进度平滑变化」在 26.x 唯一不重写渲染管线的做法。
 */
@Mixin(PostPass.class)
public interface PostPassAccessor {

    @Accessor("customUniforms")
    Map<String, GpuBuffer> getCustomUniforms();
}
