#version 330

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

// 由 Java 侧每帧改写（见 SandevistanPostProcessor）
layout(std140) uniform CyberConfig {
    float Intensity;     // 0..1 总强度（激活淡入淡出）
    float Strength;      // 径向模糊步长（UV）
    float BlurStart;     // 模糊起点（矩形边缘距离 0..1）
    float BlurFull;      // 完全模糊的矩形边缘距离
    float WarpAmount;    // 径向压缩强度
    float Chromatic;     // 色散强度
    float Samples;       // 采样次数
    float Pulse;         // 脉动相位（0..1）
    float TintR;         // 全屏色调（乘性）：斯安威斯坦=青绿，狂暴=红
    float TintG;
    float TintB;
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 center = vec2(0.5);
    vec2 dir = texCoord - center;
    float dist = length(dir);
    vec2 ndir = dist > 0.00001 ? dir / dist : vec2(0.0);

    // 到屏幕边缘的"矩形距离"：贴合屏幕形状，而不是圆形
    vec2 rect = abs(dir) * 2.0;
    float edge = max(rect.x, rect.y);

    // 脉动：让边缘的收缩幅度随时间呼吸
    float breathing = 1.0 + 0.25 * sin(Pulse * 6.2831853);
    float mask = smoothstep(BlurStart, max(BlurFull, BlurStart + 0.001), edge) * Intensity;
    mask = clamp(mask * breathing, 0.0, 1.0);

    // 径向压缩：越靠边，采样点越往中心收
    float warp = clamp(mask * WarpAmount, 0.0, 0.95);
    vec2 uv = clamp(center + dir * (1.0 - warp), vec2(0.001), vec2(0.999));

    // 沿径向向外做衰减加权模糊
    vec2 offset = ndir * Strength * mask;
    vec4 color = texture(InSampler, uv);
    float total = 1.0;
    int samples = int(Samples);
    for (int i = 1; i <= samples; i++) {
        float t = float(i) / float(samples);
        float weight = pow(1.0 - t, 2.0);
        color += texture(InSampler, clamp(uv - offset * t, vec2(0.001), vec2(0.999))) * weight;
        total += weight;
    }
    color /= total;

    // 色散：红蓝沿径向错开
    vec2 chroma = ndir * Chromatic * mask;
    color.r = texture(InSampler, clamp(uv + chroma, vec2(0.001), vec2(0.999))).r;
    color.b = texture(InSampler, clamp(uv - chroma, vec2(0.001), vec2(0.999))).b;

    // 全屏「义体视觉」色调：整体压向青绿（参考实现里那种强烈的夜视感），
    // 中间也有、边缘更重 —— 这是让效果「看得出来」的主要来源。
    // 后处理已经打通（挂在原版 GameRenderer 的执行点上），染色收回 shader ——
    // 这里是**乘法**混合，比 GUI 层那种 alpha 叠加自然得多，不会把画面对比度拉平。
    vec3 cyber = vec3(TintR, TintG, TintB);
    float globalTint = 0.72 * Intensity;
    color.rgb = mix(color.rgb, color.rgb * cyber, clamp(globalTint + mask * 0.28, 0.0, 1.0));
    // 轻微提亮，避免整体显脏
    color.rgb *= 1.0 + 0.10 * mask;
    color.a = 1.0;

    fragColor = color;
}
