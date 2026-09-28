package com.dsh.cyberware.client;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * 残影历史 —— 每 tick 存一份**完整的渲染状态副本**。
 *
 * <p><b>为什么不再「挑字段记录」：</b>早先的快照只存了位置、朝向、四肢摆动、披风这几个值，
 * 其它动画状态（游泳、蹲下、挥手、鞘翅……）没记 —— 渲染残影时读到的是「当前」的值，
 * 于是主人看到「我跑步，残影也在跑步」。
 *
 * <p>现在直接存整份 state：只要它在那一刻存在过，残影就一定会照原样画出来。
 * 代价是每 tick 复制一次（约六十个字段），所以复制走 {@link VarHandle}，
 * 存的对象从环形缓冲里复用，不做任何分配。
 */
public final class AfterimageHistory {

    /** 保留多少份（≈ 多少 tick 的历史） */
    public static final int CAPACITY = 48;

    private static final LivingEntityRenderState[] RING = new LivingEntityRenderState[CAPACITY];
    private static final Map<Class<?>, List<VarHandle>> HANDLES = new HashMap<>();
    private static final Map<Class<?>, Constructor<?>> CTORS = new HashMap<>();

    /** 最新一份的下标 */
    private static int head = 0;
    /** 已填充数量（最多 CAPACITY） */
    private static int count = 0;
    /** 诊断用：只打一次 */
    private static final java.util.concurrent.atomic.AtomicBoolean LOGGED =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    /** 上次采样的 tick —— 一 tick 可能渲染多帧，别重复存 */
    private static int lastSampleTick = Integer.MIN_VALUE;

    private AfterimageHistory() {
    }

    /** 每帧调一次；同一个 tick 内只有第一次会真正落盘。 */
    public static void sample(LivingEntityRenderState current, int gameTime) {
        if (gameTime == lastSampleTick) {
            return;
        }
        lastSampleTick = gameTime;

        Class<?> type = current.getClass();
        int slot = head;
        head = (head + 1) % CAPACITY;
        if (count < CAPACITY) {
            count++;
        }

        LivingEntityRenderState target = RING[slot];
        if (target == null || target.getClass() != type) {
            target = create(type);
            RING[slot] = target;
        }
        java.util.List<VarHandle> handles = handlesOf(type);
        copyInto(current, target, handles);
        if (LOGGED.compareAndSet(false, true)) {
            System.out.println("[cyberware] 残影采样: handles=" + handles.size()
                    + " | src walk=" + current.walkAnimationPos + " x=" + current.x
                    + " | dst walk=" + target.walkAnimationPos + " x=" + target.x);
        }
    }

    /** index = 0 表示最近的一份；越界返回 null。 */
    public static LivingEntityRenderState get(int index) {
        if (index < 0 || index >= count) {
            return null;
        }
        // head 指向「下一份要写的位置」，所以最新一份在 head-1
        int pos = ((head - 1 - index) % CAPACITY + CAPACITY) % CAPACITY;
        return RING[pos];
    }

    public static int size() {
        return count;
    }

    public static void clear() {
        count = 0;
        head = 0;
        lastSampleTick = Integer.MIN_VALUE;
    }

    private static List<VarHandle> handlesOf(Class<?> type) {
        return HANDLES.computeIfAbsent(type, key -> {
            List<VarHandle> handles = new ArrayList<>();
            MethodHandles.Lookup base = MethodHandles.lookup();
            for (Class<?> cursor = key; cursor != null && cursor != Object.class; cursor = cursor.getSuperclass()) {
                MethodHandles.Lookup lookup;
                try {
                    lookup = base;
                } catch (Throwable t) {
                    continue;
                }
                for (Field field : cursor.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }
                    try {
                        handles.add(lookup.unreflectVarHandle(field));
                    } catch (Throwable ignored) {
                        // 个别字段拿不到就算了
                    }
                }
            }
            return handles;
        });
    }

    private static void copyInto(LivingEntityRenderState src, LivingEntityRenderState dst,
                                 List<VarHandle> handles) {
        for (int i = 0; i < handles.size(); i++) {
            VarHandle handle = handles.get(i);
            try {
                handle.set(dst, handle.get(src));
            } catch (Throwable ignored) {
                // final 字段可能拒绝写入
            }
        }
    }

    private static LivingEntityRenderState create(Class<?> type) {
        try {
            Constructor<?> ctor = CTORS.computeIfAbsent(type, key -> {
                try {
                    Constructor<?> found = key.getDeclaredConstructor();
                    found.setAccessible(true);
                    return found;
                } catch (Throwable t) {
                    return null;
                }
            });
            return ctor == null ? new LivingEntityRenderState()
                    : (LivingEntityRenderState) ctor.newInstance();
        } catch (Throwable t) {
            return new LivingEntityRenderState();
        }
    }
}
