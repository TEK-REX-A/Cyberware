#version 330

// ═══════════════════════════════════════════════════════════════════════════
// round_rect.vsh —— SDF 圆角矩形（GUI）的配套顶点着色器
//
// 与 round_rect.fsh 成对使用。写法**照抄原版 core/gui.vsh**：
//   · 版本头 #version 330（26.1.2 原版 core 与 post 着色器全用 330，见下方交付说明）
//   · uniform 块**内联声明**，不用 #moj_import（启动期资源包还没加载，import 会失败）
//   · 顶点属性 Position / Color，varying 命名跟原版 position_color.vsh 一致
//
// 额外输出 fragPosPx：片元所在的**像素坐标**（GUI 缩放后的像素，不是 framebuffer 像素）。
// 取的是 ModelViewMat 之后、ProjMat 之前的位置 —— 对 GUI 的正交投影来说这一步
// 就已经是「左上加原点、单位=GUI 像素」，而 ProjMat 只负责映射到 NDC 并翻转 y。
// 这样 uRect（同样以 GUI 像素给出）与片元坐标是**同一个坐标系**，不用碰 gl_FragCoord
// （gl_FragCoord 是 framebuffer 像素，手机 guiScale=2/3 时会差一个倍数）。
//
// ⚠ 若接的管线把平移/缩放烘进了 ModelViewMat，fragPosPx 会跟着变；
//   那种情况把下面两行换成 fragPosPx = Position.xy; 并把 uRect 换成同一坐标系即可。
// ═══════════════════════════════════════════════════════════════════════════

// 原版注释（照抄，说明为什么内联而不是 import）：
// Can't moj_import in things used during startup, when resource packs don't exist.
// This is a copy of dynamicimports.glsl and projection.glsl
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};
layout(std140) uniform Projection {
    mat4 ProjMat;
};

in vec3 Position;
in vec4 Color;

out vec4 vertexColor;
out vec2 fragPosPx;

void main() {
    vec4 viewPos = ModelViewMat * vec4(Position, 1.0);

    // GUI 像素坐标（片元着色器里算 SDF 用）
    fragPosPx = viewPos.xy;

    gl_Position = ProjMat * viewPos;

    // 原版 position_color.vsh 也这么传；本 shader 目前不读它，
    // 留给管线侧做整体染色（顶点格式里有没有 Color 都不影响链接，未用的输入会被优化掉）
    vertexColor = Color;
}
