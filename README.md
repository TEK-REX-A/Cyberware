# Cyberware 义体系统

一个还原《赛博朋克 2077》义体系统的 Minecraft 模组 —— 义体植入、斯安威斯坦的时间减缓、
狂暴状态，以及配套的视觉表现（屏幕后处理、HUD、洋葱皮残影）。

> 私人兴趣项目：做出来是因为自己想玩，顺便放出来让大家也玩玩。

## 环境要求

| 项目 | 版本 |
|---|---|
| Minecraft | 26.1.2 |
| NeoForge | 26.1.2.109+ |
| Java | 25 |

**无需任何前置模组** —— 除 NeoForge 外零第三方依赖，单个 jar 丢进 `mods/` 即可。

## 当前进度

- [x] 义体容量 / 义体操作台 / 植入界面
- [x] 操作系统义体：斯安威斯坦（时间减缓）
- [x] 狂暴（Berserk）状态
- [x] 义体稀有度与型号差异
- [x] 屏幕后处理（边缘径向模糊 + 色散 + 色调）
- [x] 洋葱皮残影（第三人称可见）
- [ ] 面部 / 前额皮质 / 神经系统义体
- [ ] 骨骼 / 手臂 / 腿部 / 循环 / 表皮义体
- [ ] 赛博精神病
- [ ] 义体升级系统

## 效果简述

- **时间减缓（斯安威斯坦）**：范围内的实体每刻位移按倍率缩回 → 连续慢动作；
  投射物则整刻跳过 → 一顿一顿地推进（时间被切碎的本来面目）
- **狂暴**：无敌 + 伤害翻倍 + 击杀延长 + 击杀治疗 + 移速 / 攻速加成
- **视觉**：FOV 拉伸、边缘径向模糊、冷暖色调、竖条 HUD、半透明暖色渐变残影

## 构建

```bash
./gradlew build
# 产物：build/libs/cyberware-<version>.jar
```

## 许可

**代码**：AGPL-3.0（见 [LICENSE](LICENSE)）

**素材**：不属于上述许可范围，详见下方声明。

## ⚠️ 素材声明 / Assets Notice

> 本项目为**非商业性的粉丝作品**，与 CD Projekt Red 无隶属关系，
> 也未获得其官方授权或背书。

项目中的**贴图与图标素材**（`src/main/resources/assets/cyberware/textures/**`）
提取自《赛博朋克 2077》(Cyberpunk 2077)，**版权归 CD Projekt Red 所有**。
这些素材**不在 AGPL-3.0 的授权范围内**，本项目对它们不主张任何权利。

---

The **code** of this project is licensed under **AGPL-3.0**.

However, the **texture and icon assets** are derived from *Cyberpunk 2077* and
remain the property of **CD Projekt Red**. They are **not covered by the
AGPL-3.0 license**, and this project claims no rights over them.

This is a **non-commercial fan project**, not affiliated with or endorsed by
CD Projekt Red.

## 致谢

- **CD Projekt Red** —— 义体设定与视觉风格源自《赛博朋克 2077》
- Minecraft / NeoForge 社区
- 以及那只一直在旁边念经的猫（它知道我在说谁）
