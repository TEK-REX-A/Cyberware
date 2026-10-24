#version 330

// Cyberware 圆角 SDF 着色器 —— 片元着色器
//
// 为什么不需要 uniform 传矩形尺寸：
//   绘制用 blit(1x1 白贴图, 源区域 1x1, 目标 w×h)，于是 uv 在整个 quad 上正好是 0..1。
//   屏幕空间导数 d(uv)/d(px) = 1 / 矩形像素尺寸，所以 fragment 里 尺寸 = 1 / fwidth(uv)。
//   有了尺寸 + uv 就能算 SDF；半径是编译期常量（CY_RADIUS，建管线时用 define 烘进去）。
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

uniform sampler2D Sampler0;

#ifndef CY_RADIUS
#define CY_RADIUS 4.0
#endif
#ifndef CY_STROKE
#define CY_STROKE 0
#endif


in vec2 uv;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec2 stepPx = max(fwidth(uv), vec2(1.0e-5));
    vec2 size = 1.0 / stepPx;
    vec2 halfSize = size * 0.5;
    vec2 p = abs(uv - 0.5) * size;              // 到中心的像素距离（各轴）

    float r = min(CY_RADIUS, min(halfSize.x, halfSize.y));
    vec2 q = p - (halfSize - r);
    float d = length(max(q, vec2(0.0))) + min(max(q.x, q.y), 0.0) - r;   // >0 = 矩形外

    vec4 color = texture(Sampler0, uv) * vertexColor;

#if CY_STROKE
    // 描边：贴在内侧 1px 的那一圈
    float inside = -d;
    float alpha = smoothstep(-0.5, 0.5, inside) * (1.0 - smoothstep(0.5, 1.5, inside));
#else
    // 填充：1px 抗锯齿
    float alpha = 1.0 - smoothstep(-0.5, 0.5, d);
#endif

    alpha *= color.a;
    if (alpha <= 0.002) {
        discard;
    }
    fragColor = vec4(color.rgb, alpha) * ColorModulator;
}
