# Cyberware 构建说明

目标平台：**Minecraft 26.1.2 + NeoForge 26.1.2.109 + Java 25**

## 在 PC 上构建（推荐，一条命令）

```bash
cd cyberware
./gradlew build          # 首次会自动生成 wrapper；或直接用系统 gradle
```

产物：`build/libs/cyberware-1.0.0.jar` → 丢进 `mods/` 即可。

要求：
- **JDK 25**（NeoForge 26.x 的编译目标；JDK 21 会被 Gradle 的 toolchain 拒绝）
- 首次构建会跑 NeoForge 的 `neoForm`（反编译整个 MC → 打补丁 → 重编译），
  PC 上通常 3~10 分钟，**需要 4GB 以上可用内存**

## 在手机上构建（内存要求高，容易失败）

本工程最初就是在 Android 容器里炮制的，结论是：

- Gradle 自身要跑在 **JDK 21**（Gradle 8.10.2 读不懂 Java 25 的 class 文件，报
  `Unsupported class file major version 69`），编译目标再用 toolchain 指到 JDK 25
- **反编译 MC 是子进程，不吃 Gradle 的 `-Xmx`**，它会一路吃到把系统撑爆 ——
  实测在 7.6GB 总内存 / 剩 1.3GB 的手机上被 OOM killer 干掉
- 想再试的话：先清后台腾内存，再设 `JAVA_TOOL_OPTIONS="-Xmx1500m"` 给所有 JVM 子进程上缰绳

## 第一步已实现的内容

- 主类 + 5 张注册表（方块 / 物品 / 方块实体 / 菜单 / 数据组件）
- `CyberwareData` 数据组件（稀有度 + 等级，带旧存档防御性回落）
- 配置类 `CyberwareConfig`（容量上限、升级成功率与材料、赛博精神病参数）
- 义体操作台方块 + BlockEntity + 菜单（离开 3 格自动关闭、只接受带组件的物品）
- 网络包 `CyberwareActionPayload`（安装 / 卸载 / 升级，客户端 → 服务端）
- 深色霓虹界面框架 `CyberwareStationScreen`
- 方块模型与自绘 16×16 霓虹纹理

## 26.x 的 API 变更备忘（踩过的坑）

| 旧写法 | 26.x 写法 |
|---|---|
| `ResourceLocation` | **`Identifier`** |
| `GuiGraphics` | **`GuiGraphicsExtractor`** |
| `render` / `renderBg` / `renderLabels` | `extractRenderState` / `extractBackground` / `extractLabels` |
| `drawString` / `renderItem` | `text(...)` / `item(...)` |
| `BlockEntityType.Builder` | 构造被 AT 开放，直接 `new BlockEntityType<>(supplier, Set.of(block))` |
| `DeferredRegister.BlockEntities/Menus` | 已移除，用 `DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE/MENU, ...)` |
| `ItemStack#has(DataComponentType)` | 改用 `stack.get(component) != null` |
| `CompoundTag` 存档 | `ValueInput` / `ValueOutput` |
