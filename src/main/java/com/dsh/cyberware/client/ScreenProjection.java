package com.dsh.cyberware.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/**
 * 世界坐标 → 屏幕像素 的投影助手（t29）。
 *
 * <p>与 {@code RamHud.drawHolo} 用的是同一套换算（那份写在方法里，这里抽出来给锁定框复用）：
 * {@code GameRenderer.projectPointToScreen} 自带透视除法 → 返回 NDC，
 * 屏幕像素 = {@code ((x+1)/2*w, (1-y)/2*h)}；{@code z ∈ [-1,1]} 之外视为背后/超距。
 *
 * <p><b>为什么是静态字段而不是返回值</b>：调用点在每帧/每 tick 的热路径上，
 * 「无新增每帧对象分配」是硬要求 —— 这里把结果放在静态字段里，不产生任何对象。
 * 只允许在<b>客户端主线程/渲染线程</b>使用（本来也只有这两处调用）。
 */
public final class ScreenProjection {

    /** 上一次 {@link #project} 的结果：是否可见 */
    private static boolean visible;
    /** 上一次 {@link #project} 的结果：屏幕像素坐标 */
    private static double screenX;
    private static double screenY;

    private ScreenProjection() {
    }

    /**
     * 投影一个世界坐标点。
     *
     * <p><b>保命壳（t34）</b>：这个方法同时被<b>每 tick</b>（{@code HackClientState.tickLock}）与
     * <b>每帧渲染</b>（{@code HackHud.drawLockFrame}）调用，抛异常会当场崩客户端 ——
     * 整段包 try/catch（这里不必打日志：失败就是「不可见」，上层那两处各自的保命壳负责记录）。
     *
     * @return true = 结果有效（在视锥内且不在背后），结果见 {@link #x()} / {@link #y()}
     */
    public static boolean project(double worldX, double worldY, double worldZ) {
        try {
            return projectInternal(worldX, worldY, worldZ);
        } catch (Throwable t) {
            visible = false;                    // 抛出 = 当作不可见，绝不让渲染线程崩
            return false;
        }
    }

    /** {@link #project} 的逻辑体（与 t29 一字不差），由上面的保命壳调用。 */
    private static boolean projectInternal(double worldX, double worldY, double worldZ) {
        visible = false;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.gameRenderer == null) {
            return false;
        }
        Vec3 ndc = minecraft.gameRenderer.projectPointToScreen(new Vec3(worldX, worldY, worldZ));
        if (ndc == null || ndc.z < -1.0D || ndc.z > 1.0D) {
            return false;                       // 背后 / 超出近远平面
        }
        var window = minecraft.getWindow();
        if (window == null) {
            return false;
        }
        double w = window.getGuiScaledWidth();
        double h = window.getGuiScaledHeight();
        screenX = (ndc.x + 1.0D) * 0.5D * w;
        screenY = (1.0D - ndc.y) * 0.5D * h;
        visible = true;
        return true;
    }

    /** 上一次投影的屏幕 X（像素）。 */
    public static double x() {
        return screenX;
    }

    /** 上一次投影的屏幕 Y（像素）。 */
    public static double y() {
        return screenY;
    }

    /** 上一次投影是否有效。 */
    public static boolean visible() {
        return visible;
    }
}
