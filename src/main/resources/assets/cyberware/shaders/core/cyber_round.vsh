#version 330

// Cyberware 圆角 SDF 着色器 —— 顶点着色器
// 结构照抄原版 core/position_tex_color.vsh（GUI 只绑 Projection / DynamicTransforms 等 UBO，
// 没有 per-draw uniform，所以矩形尺寸靠 uv 的屏幕空间导数在 fragment 里推，见 .fsh）。
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
in vec2 UV0;
in vec4 Color;

out vec2 uv;
out vec4 vertexColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    uv = UV0;
    vertexColor = Color;
}
