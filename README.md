# Cyberware 义体系统

一个还原《赛博朋克 2077》义体系统的 Minecraft 模组 —— 义体植入、斯安威斯坦的时间减缓、
狂暴状态、护盾，以及配套的视觉表现（屏幕后处理、HUD、洋葱皮残影）。

> **Q：为什么做这个 Mod？**
> **A：闲的没事干。**

## 环境要求

| 项目 | 版本 |
|---|---|
| Minecraft | 26.1.2 |
| 加载器 | **Fabric**（Loader ≥ 0.19.3） |
| Java | 25（26.1.2 强制要求） |
| 前置 | **Fabric API** |

除 Fabric API 外没有其它前置，单个 jar 丢进 `mods/` 即可。

> 注：项目已从 NeoForge 迁移到 Fabric。迁移过程中的语义损失、已知限制见
> `Cyberware-Fabric-迁移报告.md` / `Cyberware-Fabric-未完成功能报备.txt`。

## 当前进度

- [x] 义体容量 / 义体操作台 / 植入界面
- [x] 操作系统义体：斯安威斯坦（时间减缓）
- [x] 狂暴（Berserk）状态
- [x] 义体稀有度与型号差异
- [x] 屏幕后处理（边缘径向模糊 + 色散 + 色调）
- [x] 洋葱皮残影（第三人称可见）
- [x] 义体安装界面：三栏布局、人体部位图、容量条、已安装槽位
- [x] 安装数据跨端同步（S2C 快照，绕开 Fabric 附件在 Android 上的同步限制）
- [ ] 护盾系统（义体提供上限与恢复速率，先扣盾后扣血）—— 开发中
- [ ] 面部 / 前额皮质 / 神经系统义体
- [ ] 骨骼 / 手臂 / 腿部 / 循环 / 表皮 / 手掌义体
- [ ] 赛博精神病
- [ ] 义体升级系统

## 效果简述

- **时间减缓（斯安威斯坦）**：范围内的实体每刻位移按倍率缩回 → 连续慢动作；
  投射物则整刻跳过 → 一顿一顿地推进（时间被切碎的本来面目）
- **狂暴**：无敌 + 伤害翻倍 + 击杀延长 + 击杀治疗 + 移速 / 攻速加成
- **护盾**：伤害优先扣护盾，溢出部分才扣血；脱战后按义体提供的速率回复
- **视觉**：FOV 拉伸、边缘径向模糊、冷暖色调、竖条 HUD、半透明暖色渐变残影

## 义体操作台

- 左侧人体图十个部位，点选切换（再点取消）
- 中栏：可选安装列表 / 已安装槽位（图标 + 简称 + 容量占用）
- 右侧：选中义体的名称、英文名、稀有度、加成及效果
- 底部容量条：已用 / 总量，选中时预览本次需要多少
- **不需要送料槽**：点安装直接从背包取件

## 构建

需要 Gradle 9.x + JDK 25。26.1.2 官方不发布 mappings（客户端 jar 本身不混淆），
本项目用「恒等映射」构建，生成方法见 `Cyberware-Fabric-构建阻塞.md` 第二节。

```bash
gradle --no-daemon build
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
- Minecraft / Fabric 社区
- 以及那只一直在旁边念经的凑猫娘（它知道我在说谁）
