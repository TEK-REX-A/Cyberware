#version 330

// ═══════════════════════════════════════════════════════════════════════════
// round_rect.fsh —— SDF 圆角矩形：填充 / 描边，一次搞定，smoothstep 抗锯齿
//
// 【版本头】#version 330 —— 与 26.1.2 原版一致（core/gui.fsh、core/position_color.fsh、
//   core/rendertype_outline.fsh、post/blit.fsh，以及本工程的 post/sandevistan_edge.fsh
//   全部是同一种 330 + in/out 写法）。**不是** 150，也**不是** GLSL ES。
//
// 【uniform 块为什么不像 sandevistan_edge.fsh 那样散着写 float】
//   sandevistan_edge.fsh 是 post 着色器，Java 侧自己 InSampler/CyberConfig 一把填；
//   这个是 core 着色器，走 RenderPipeline 的 std140 uniform group，所以参数打包进一个
//   块里并**显式对齐**（vec4 在前，float 在后，总 48 字节，无隐式 padding）。
//   Java 侧需要注册一个名为 RoundRectConfig 的 std140 uniform group，成员顺序/偏移见下。
//
// 【坐标】fragPosPx 来自 round_rect.vsh（GUI 像素，左上原点 y 向下）。
//   uRect 必须用**同一套**坐标给出。SDF 本身对称，所以 y 朝哪边不影响结果。
// ═══════════════════════════════════════════════════════════════════════════

// ── 参数块（std140：每个 vec4 占 16 字节，float 各占 4，块总长 48，16 字节对齐）──
layout(std140) uniform RoundRectConfig {
    vec4  uRect;      // 偏移  0 : x, y, w, h   —— 矩形左上角与宽高（GUI 像素）
    vec4  uColor;     // 偏移 16 : r, g, b, a   —— 线性 0..1（不是 0..255）
    float uRadius;    // 偏移 32 : 圆角半径（像素），0 = 直角
    float uStrokePx;  // 偏移 36 : 描边宽度（像素）；<=0 = **填充模式**，>0 = **描边模式**
    float uFillFlag;  // 偏移 40 : 描边模式下是否也填充内部：1 = 填，0 = 只描边（默认 0）
    float uAAPx;      // 偏移 44 : 抗锯齿过渡宽度（像素）；0 = 自动用 fwidth(d)
};

// 原版 GUI 管线会提供 DynamicTransforms；本 shader 默认**不乘** ColorModulator，
// 避免「管线没注册这个块 → 读出来是 0 → 整块透明」这种哑巴亏。
// 接管线时若确认提供了 DynamicTransforms，取消下面这行的注释即可恢复原版 GUI 的全局染色。
// #define USE_COLOR_MODULATOR

#ifdef USE_COLOR_MODULATOR
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};
#endif

in vec2 fragPosPx;

out vec4 fragColor;

void main() {
    float rw = uRect.z;
    float rh = uRect.w;

    // ── 0. 尺寸非法（宽度/高度 <= 0）：什么都不画
    if (rw <= 0.0 || rh <= 0.0) {
        discard;
    }

    // ── 1. 圆角半径夹取
    //    半径 > 短边一半 会让 (halfSize - r) 变负、SDF 中部反号（内部被判成外部），
    //    所以必须先夹住 —— 这是这类 SDF 最常见的「圆角一大就整块消失」来源。
    float r = clamp(uRadius, 0.0, min(rw, rh) * 0.5);

    vec2 center   = uRect.xy + vec2(rw, rh) * 0.5;
    vec2 halfSize = vec2(rw, rh) * 0.5;

    // ── 2. 有符号距离：负 = 在形状内部，正 = 外部，单位是像素
    //    p 是「把点折到第一象限后，相对『内缩 r 的矩形』的偏移」
    //    外面那段 length(max(p,0)) 管圆角，里面 min(max(p.x,p.y),0) 管直线边，最后减 r 得到真距离
    vec2  p = abs(fragPosPx - center) - (halfSize - vec2(r));
    float d = length(max(p, 0.0)) + min(max(p.x, p.y), 0.0) - r;

    // ── 3. 抗锯齿宽度（像素）
    //    fwidth(d) = d 在一个像素内的变化量 ≈ 1.0（d 就是像素单位），天然自适应缩放。
    //    原版 terrain.fsh 用了 dFdx/dFdy（同一族导数函数），说明 26.1.2 的 330 管线下
    //    导数可用；但为了「某些管线/驱动导数为 0 时 smoothstep 退化成硬边」不至于更丑，
    //    这里把结果夹在 [0.5, 2.0] 像素。uAAPx > 0 时直接用它，绕过所有导数问题。
    float aa = (uAAPx > 0.0) ? uAAPx : clamp(fwidth(d), 0.5, 2.0);

    // ── 4. 填充与描边
    //    填充：d < 0 的区域，边缘用 smoothstep 在 ±aa 内过渡（1px 级别抗锯齿）
    float fillAlpha = 1.0 - smoothstep(-aa, aa, d);

    //    描边：d ∈ (-uStrokePx, 0) 这条环带。
    //    前一项 = 「d 在 -uStrokePx 以右」，后一项 = 「d 在 0 以左」，相乘即环带。
    //    uStrokePx = 0 时两项重合，环带退化为边缘上一条极细的亮线 —— 所以下面按模式不用它。
    float S = max(uStrokePx, 0.0);
    float strokeAlpha = smoothstep(-S - aa, -S + aa, d) * (1.0 - smoothstep(-aa, aa, d));

    // ── 5. 模式选择：uStrokePx <= 0 → 纯填充；> 0 → 描边（可选同时填充）
    float a;
    if (S > 0.0) {
        a = strokeAlpha;
        if (uFillFlag > 0.5) {
            // 只有一种颜色时「填充+描边」视觉上等于实心圆角矩形；
            // 留着是为了将来拆出独立的描边色（那时把这里改成 mix 即可）
            a = max(a, fillAlpha);
        }
    } else {
        a = fillAlpha;
    }

    // ── 6. 输出 = uColor * alpha
    float alpha = clamp(uColor.a * a, 0.0, 1.0);
    if (alpha <= 0.001) {
        discard;                       // 形状以外直接扔掉，省混合带宽（原版 gui.fsh 同样做法）
    }

    vec4 color = vec4(uColor.rgb, alpha);
#ifdef USE_COLOR_MODULATOR
    fragColor = color * ColorModulator;
#else
    fragColor = color;
#endif
}
