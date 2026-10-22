package com.dsh.cyberware.core;

import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/**
 * Attachment 访问的编译期适配层。
 *
 * <p>{@code Player} 在 <b>运行时</b> 由 Fabric Loader 注入实现
 * {@link AttachmentTarget}，但 <b>编译期</b> javac 看不到那些 default 方法
 * （原版类没有 implements 声明）。所以统一走这里做一次 cast。
 */
public final class Attachments {

    private Attachments() {
    }

    public static <A> A get(AttachmentTarget target, AttachmentType<A> type) {
        return target.getAttached(type);
    }

    public static <A> A set(AttachmentTarget target, AttachmentType<A> type, A value) {
        return target.setAttached(type, value);
    }

    public static boolean has(AttachmentTarget target, AttachmentType<?> type) {
        return target.hasAttached(type);
    }

    /** 原版类编译期不是 AttachmentTarget，这里是唯一的 cast 点。 */
    public static AttachmentTarget of(Object target) {
        return (AttachmentTarget) target;
    }
}
