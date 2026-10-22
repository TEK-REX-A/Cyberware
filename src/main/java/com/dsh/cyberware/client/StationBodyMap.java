package com.dsh.cyberware.client;

import com.dsh.cyberware.core.CyberwareSlot;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * 左栏「人体部位图」—— 十个固定部位 + 命中测试。
 *
 * <p>设计稿（{@code 无标题-2026-10-22-0001.png}）里的人体是**纯线条**，旁边那些引线和文字标注
 * 是给写代码的人看的注释，<b>不是游戏内要显示的东西</b>；游戏里只显示人体 + 当前部位高亮。
 *
 * <h3>部位 → 槽位的对应关系</h3>
 * <p>设计稿固定十个部位，与 {@link CyberwareSlot} 一一对应。
 * 2026-10-22 核心层新增了 {@code HAND}（手掌）槽，原先借 {@code SKIN} 的临时方案已撤销；
 * {@code HAND} 追加在枚举**末尾**，所以旧存档的槽位索引不会错位。
 *
 * <h3>重叠判定</h3>
 * <p>{@link #PARTS} 的顺序就是命中优先级：**小的在前、大的在后**。
 * 骨骼几乎覆盖整个躯干，所以一定排最后；颅内芯片、心脏、手掌这些小块排最前，
 * 否则会被大块抢走。{@link #partAt} 顺着列表走，第一个命中的就是结果。
 */
final class StationBodyMap {

    /** 一块矩形（人体局部坐标，不含量原点偏移）。 */
    record Box(int x, int y, int w, int h) {
        boolean contains(double mx, double my) {
            return mx >= this.x && mx < this.x + this.w && my >= this.y && my < this.y + this.h;
        }
    }

    /** 一个部位：显示名 + 对应槽位 + 若干块矩形（双手、双腿、双臂都是两块）。 */
    record Part(String label, CyberwareSlot slot, List<Box> boxes) {
    }

    /** 人体包围盒（局部坐标，原点在人体图左上角）。 */
    static final int BODY_W = 56;
    static final int BODY_H = 120;

    /**
     * 十个部位。<b>顺序 = 命中优先级（小 → 大）</b>，不要随手调换。
     */
    static final List<Part> PARTS = List.of(
            // ① 颅内芯片：最小的一块，压在额皮质/面部上面
            new Part("操作系统", CyberwareSlot.OPERATING_SYSTEM, List.of(new Box(24, 3, 8, 6))),
            // ② 心脏：压在躯干（骨骼）与脊柱上面
            new Part("循环系统", CyberwareSlot.CIRCULATORY, List.of(new Box(22, 29, 12, 12))),
            // ③ 双手：手掌（2026-10-22 起有独立 HAND 槽，不再借 SKIN）
            new Part("手掌", CyberwareSlot.HAND, List.of(new Box(6, 56, 8, 12), new Box(42, 56, 8, 12))),
            // ④ 脊柱：压在骨骼躯干上面
            new Part("神经系统", CyberwareSlot.NERVOUS_SYSTEM, List.of(new Box(25, 25, 6, 38))),
            // ⑤ 面部 / ⑥ 额皮质：头部切成上下两半，芯片优先
            new Part("面部", CyberwareSlot.FACE, List.of(new Box(18, 10, 20, 10))),
            new Part("额皮质", CyberwareSlot.FRONTAL_CORTEX, List.of(new Box(18, 0, 20, 9))),
            // ⑦ 表皮：手臂外侧那两条 3px 的皮肤带（比手臂窄，所以排在手臂前面）
            new Part("表皮", CyberwareSlot.INTEGUMENTARY, List.of(new Box(3, 22, 3, 36), new Box(50, 22, 3, 36))),
            // ⑧ 手臂
            new Part("手臂", CyberwareSlot.ARMS, List.of(new Box(6, 24, 8, 32), new Box(42, 24, 8, 32))),
            // ⑨ 腿部（两条大腿 + 小腿）
            new Part("腿部", CyberwareSlot.LEGS, List.of(new Box(15, 74, 12, 46), new Box(29, 74, 12, 46))),
            // ⑩ 骨骼：躯干 + 骨盆，最大的一块，永远排最后
            new Part("骨骼", CyberwareSlot.SKELETON,
                    List.of(new Box(14, 24, 28, 40), new Box(24, 20, 8, 4), new Box(15, 64, 26, 10)))
    );

    private StationBodyMap() {
    }

    /**
     * 命中测试。
     *
     * @param ox 人体图原点在<b>屏幕绝对坐标</b>下的 x（{@code leftPos + 栏内偏移}）
     * @param oy 同上 y
     * @param mx 鼠标屏幕绝对 x
     * @param my 鼠标屏幕绝对 y
     * @return 命中的部位；都没命中返回 null
     */
    static Part partAt(int ox, int oy, double mx, double my) {
        double lx = mx - ox;
        double ly = my - oy;
        for (Part part : PARTS) {
            for (Box box : part.boxes()) {
                if (box.contains(lx, ly)) {
                    return part;
                }
            }
        }
        return null;
    }

    /**
     * 画人体图（面板相对坐标）。
     *
     * @param selected  当前选中的部位槽位（用 accent-cyan 描边）
     * @param hovered   鼠标悬停的部位槽位（PC 端额外高亮；触屏没有悬停，通常是 null）
     * @param installed 身上装了义体的槽位（右上角画绿点 —— 状态不只靠颜色）
     */
    static void draw(GuiGraphicsExtractor g, int ox, int oy, CyberwareSlot selected,
                     CyberwareSlot hovered, Set<CyberwareSlot> installed) {
        // ① 剪影底：所有块铺同一色，重叠处看不出接缝
        for (Part part : PARTS) {
            for (Box b : part.boxes()) {
                StationPaint.roundRect(g, ox + b.x(), oy + b.y(), b.w(), b.h(), 2, StationPaint.BG_CARD);
            }
        }
        // ② 高亮填充（先填、再描边，否则描边会被盖掉）
        for (Part part : PARTS) {
            boolean isSel = part.slot() == selected;
            boolean isHov = !isSel && part.slot() == hovered;
            if (!isSel && !isHov) {
                continue;
            }
            int fill = isSel ? StationPaint.withAlpha(StationPaint.ACCENT_CYAN, 0.18F)
                    : StationPaint.BG_HOVER;
            for (Box b : part.boxes()) {
                StationPaint.roundRect(g, ox + b.x(), oy + b.y(), b.w(), b.h(), 2, fill);
            }
        }
        // ③ 描边
        for (Part part : PARTS) {
            boolean isSel = part.slot() == selected;
            boolean isHov = !isSel && part.slot() == hovered;
            int line = isSel ? StationPaint.ACCENT_CYAN
                    : (isHov ? StationPaint.withAlpha(StationPaint.ACCENT_CYAN, 0.5F)
                            : StationPaint.withAlpha(StationPaint.LINE, 0.75F));
            for (Box b : part.boxes()) {
                StationPaint.outline(g, ox + b.x(), oy + b.y(), b.w(), b.h(), line);
            }
        }
        // ④ 已装标记：该部位右上角一个 2px 绿点
        for (Part part : PARTS) {
            if (!installed.contains(part.slot())) {
                continue;
            }
            List<Box> boxes = part.boxes();
            Box b = boxes.get(boxes.size() - 1);
            g.fill(ox + b.x() + b.w() - 3, oy + b.y() + 1, ox + b.x() + b.w() - 1,
                    oy + b.y() + 3, StationPaint.OK_GREEN);
        }
    }
}
