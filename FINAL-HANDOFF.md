# FINAL-HANDOFF.md —— 0.3.12-Beta 收尾交付（t12 / inspector）

工作区：`/root/mod26/cyberware`　本任务 attempt_id：`5ebf84b5-63fa-4786-a3cd-8dd1609d1244`
执行内容：M2 修复独立复核 → 最终构建 → commit 落盘 → 部署 jar 到 FCL → 真机验证清单。
本任务只新增/提交文件与执行授权范围内的 git 提交与部署，**未修改任何他人负责的源码逻辑**。

---

## 0. 结论

| 项 | 结果 |
|---|---|
| M2 复核（不盲信 opticsdev） | ✅ **通过**：源码 13/13 注入器带 `require`；jar 内 `require=0` × 13、`require=1` × 0；描述符与 t6 基线一致；`git diff` 只有 require 行 |
| 最终构建 | ✅ `gradle build --no-daemon` → **exit 0**，`BUILD SUCCESSFUL in 1m 26s` |
| commit | ✅ `30e9646cadd76444082b8cde1e273174e63ba98d`，**425 files changed, +5515 / −641**；工作树已干净 |
| 部署 FCL | ✅ mods 目录里 cyberware **只剩 `cyberware-0.3.12-Beta.jar`**；旧 `0.3.11-Beta-Hotfix-10.jar` 已 mv 到 `/sdcard/DSH/trash/cyberware/` |
| 真机验证 | ⏳ **待主人进游戏**（清单见 §5） |

> ### ⚠ **编译通过不等于注入成功，真机日志才算数。**
> 静态层面（描述符、注解、类已进 jar、`require=0`）已全部核实，但「注入命中 / 描边画出来 / 装卸真生效」
> 只有主人真机跑过并在 `latest.log` 里看不到 `Mixin apply ... failed` 之类关键字，才算通过。

---

## 1. M2 复核（独立复现 opticsdev 的 t11 结论）

### 1.1 源码级：注入器逐块检查（不是只看文件名）

脚本口径：从每个 `@Inject(` / `@Redirect(` / `@ModifyArg(` / `@ModifyVariable(` 一直读到配对的右括号，检查块内是否含 `require`。

```
注入器总数 13
缺 require 的: 无（0 处）
```

逐文件（`注入器数 / require 出现次数`，含文档注释里的提及）：

```
CapeLayerMixin 1/2   EntityRendererOutlineMixin 1/2   EquipmentLayerRendererMixin 4/5
GameRendererMixin 1/1  GuiMixin 1/1  ParticleMixin 1/1  ProjectileMixin 1/1
SoundEngineMixin 1/1   WeatherEffectRendererMixin 2/2
（LivingEntityRendererAccessor / PostChainAccessor / PostPassAccessor 无注入器，只有 @Accessor/@Invoker）
```

### 1.2 与 HEAD 的 diff：只加了 require，没动别的东西

```
$ git diff -U0 -- src/main/java/com/dsh/cyberware/mixin/
 GameRendererMixin.java            @@ -46 +46,2 @@   -)  +),  +require = 0
 GuiMixin.java                     @@ -21 +21 @@      …cancellable = true)  →  …cancellable = true, require = 0)
 ParticleMixin.java                @@ -46 +46 @@      …at = @At("RETURN"))  →  …, require = 0)
 ProjectileMixin.java              @@ -27 +27 @@      同上
 SoundEngineMixin.java             @@ -28 +28 @@      同上
 WeatherEffectRendererMixin.java   @@ -19 +19 @@ / @@ -25 +25 @@  两处 index=2 / index=3 各加 require = 0
 6 files changed, 8 insertions(+), 7 deletions(-)
```

`method=` / `target=` / `value=` / `index=` / `cancellable=` 的值**均未改动**。

### 1.3 产物级：从**最终 jar** 取 class 用 javap -v 核验

不是抽查 3 处 —— 把 jar 里 12 个 mixin 类全过了一遍：

```
CapeLayerMixin:                  require=0 x1   require=1 x0
EntityRendererOutlineMixin:      require=0 x1   require=1 x0
EquipmentLayerRendererMixin:     require=0 x4   require=1 x0
GameRendererMixin:               require=0 x1   require=1 x0
GuiMixin:                        require=0 x1   require=1 x0
LivingEntityRendererAccessor:    require=0 x0   require=1 x0  (@Accessor/@Invoker，无 require 参数)
ParticleMixin:                   require=0 x1   require=1 x0
PostChainAccessor:               require=0 x0   require=1 x0  (同上)
PostPassAccessor:                require=0 x0   require=1 x0  (同上)
ProjectileMixin:                 require=0 x1   require=1 x0
SoundEngineMixin:                require=0 x1   require=1 x0
WeatherEffectRendererMixin:      require=0 x2   require=1 x0
合计 require=0 = 13   非 0 值: 无
```

样例（GuiMixin，`javap -v -p -cp build/libs/cyberware-0.3.12-Beta.jar`）：

```
RuntimeVisibleAnnotations:
    org.spongepowered.asm.mixin.injection.Inject(
      method=["extractPlayerHealth"]
      at=[@…At(value="HEAD")]
      cancellable=true
      require=0
```

### 1.4 描述符未被改动（对照 t6 基线）

jar 内全部 `method=[...]` 字符串去重后 **8 个**，与 t6 复核时逐字符一致、**无「方法名+冒号」写法**：

```
[ok] calculatePitch
[ok] extractPlayerHealth
[ok] extractRenderState
[ok] extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V
[ok] render
[ok] renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V
[ok] submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V
[ok] tick
```

### 1.5 最终构建

```
$ JAVA_HOME=/usr/lib/jvm/java-21-openjdk-arm64 PATH=$JAVA_HOME/bin:/opt/gradle-8.10.2/bin:$PATH \
  gradle build --no-daemon
> Task :compileJava UP-TO-DATE … > Task :build UP-TO-DATE
BUILD SUCCESSFUL in 1m 26s
4 actionable tasks: 4 up-to-date
EXIT=0
```

全 UP-TO-DATE 本身即证据：源码自 t11 那次真编译之后**未再变动**，产物与源码一致。
产物 `build/libs/cyberware-0.3.12-Beta.jar`（655477 字节，md5 `3b704ae42705b5751f36791be41c8efc`）。

**结论：M2 已真实修复，13/13 合规，无残留黑屏风险项。**

---

## 2. commit 落盘

```
$ git status --porcelain | wc -l      →  433   （37 M + 36 D + 360 ??，与预期 400+ 一致）
```

暂存方式：**显式清单**（`git ls-files -m -d` + `git ls-files --others` + 逐份文档名 → `/tmp/t12-add.txt`），
用 `git add --pathspec-from-file=/tmp/t12-add.txt` 落索引，**没有用 `git add -A`**。
落库前确认清单里没有 `.jar` / `build/` / `.gradle/` / `run/`：`grep` 全部无命中（`.gitignore` 已忽略 `build/ .gradle/ run/ *.log`；jar 未被误加）。

```
commit 30e9646cadd76444082b8cde1e273174e63ba98d
Author: Neko <neko@cyberware.local>
Message: 0.3.12-Beta：123+12 条义体定义、贴图归档(32x32)、中文物品名、义体存储接线与操作台交互、义眼敌我识别描边

425 files changed, 5515 insertions(+), 641 deletions(-)
（提交后 git status --porcelain 为 0 行 —— 工作树干净）
```

分类统计（`git show --name-status`）：`352 A`（新增源码/资源/文档）、`28 D` + `8 R`（= 36 个旧资源删除，其中 8 个被 git 识别为重命名）、`37 M`。
**12 份交付文档已按 captain 意见入库**：`VERIFY-REPORT.md`、`DEFS-VALUES.md`、`TEXTURE-MAPPING.md`、
`ASSET-WHITELIST.txt`、`ASSET-MISSING.txt`、`INSTALL-WIRING.md`、`OPTICS-MIXIN.md`、`12-NEW-DEFS.md`、
`LANG-AND-NEW-ASSETS.md`、`M2-FIX.md`（+ 本文件 FINAL-HANDOFF.md，见下）。
理由：这些报告是本次的证据链，且报告里大量结论直接引用 `ASSET-WHITELIST.txt` / `ASSET-MISSING.txt` 与行号，
不入库会让后人无法复核。未入 `build/`、`.gradle/`、任何 jar。

本文件（FINAL-HANDOFF.md）本身在 `30e9646` 之后作为**独立收尾提交**入库（文件名已显式列出后 `git add`），
其 commit hash 记录在 t12 的任务回报里 —— 避免「文档里写自己的 hash」这种自指导致的失效。

未执行 `git push`（按硬约束）。

---

## 3. 部署到 FCL

### 3.1 部署前 mods 目录（cyberware 相关）

```
/sdcard/FCL/.minecraft/versions/26.1.2-NeoForge/mods/
  -rw-rw---- 1 10000 1023  296090 Sep 28 17:24 cyberware-0.3.11-Beta-Hotfix-10.jar
```

### 3.2 执行（旧 jar 走 trash，不 rm；二进制用 cp）

```bash
mkdir -p /sdcard/DSH/trash/cyberware
mv  <mods>/cyberware-0.3.11-Beta-Hotfix-10.jar  /sdcard/DSH/trash/cyberware/
cp  /root/mod26/cyberware/build/libs/cyberware-0.3.12-Beta.jar  <mods>/
sync
```

实测输出：

```
renamed '…/mods/cyberware-0.3.11-Beta-Hotfix-10.jar' -> '/sdcard/DSH/trash/cyberware/cyberware-0.3.11-Beta-Hotfix-10.jar'
'…/build/libs/cyberware-0.3.12-Beta.jar' -> '…/mods/cyberware-0.3.12-Beta.jar'
```

### 3.3 部署后核对

```
=== mods 目录里的 cyberware 条目 ===
cyberware-0.3.12-Beta.jar                       ← 只剩新 jar（计数 = 1）

=== 新 jar 落盘指纹核对 ===
3b704ae42705b5751f36791be41c8efc  <mods>/cyberware-0.3.12-Beta.jar
3b704ae42705b5751f36791be41c8efc  /root/mod26/cyberware/build/libs/cyberware-0.3.12-Beta.jar   ← md5 一致
大小 655477 字节

=== trash 里的旧 jar ===
-rw-rw---- 1 10000 1023 296090 Sep 28 17:24 cyberware-0.3.11-Beta-Hotfix-10.jar   ← 已进 trash
```

mods 目录其余 14 个 jar（Jade/ModernUI/sodium 等）未动。
新 jar 在实例内显示的时间戳为 `Sep 29 06:37`，大小与源文件一致、md5 相同 → 拷贝完整。

---

## 4. 本版包含什么（给主人对照）

- **义体定义 135 条**（123 条有素材 + 12 条无出处新定义），另有 19 条无素材定义被注释留档（物品在游戏内不存在）。
- **贴图 135 张，全部 32x32**（+ 10 张 16x16 槽位图标 `slot_*.png` 与 16x16 `cyberware_chip.png`）。
- **中文物品名 135/135**（`zh_cn`）；英文名未补（主人裁决只补中文）。
- **义体存储接线**：安装 / 卸载 / 容量校验 / 查询 API；操作台客户端安装卸载按钮已接上；
  容量上限取 `config/cyberware-common.toml` 的 `capacity.defaultCapacity`，**该实例当前值 = 100，`enableCapacityLimit = true`**。
- **义眼敌我识别描边**：装歧路司系列义眼后，生物按关系描边（绿=同队/宠物，黄=中立，红=敌对）。
- 删除的 12 条旧 id（斯安威斯坦/狂暴/网络接入仓旧型号）的定义、贴图、模型、语言条目全部清理。

---

## 5. 真机验证清单（主人操作）

### 第 0 步：启动
直接启动 FCL 的 `26.1.2-NeoForge` 实例即可（jar 已就位）。进游戏前**无需**改配置。

### 第 1 步：拿义体（创造模式）
创造模式物品栏里找 **「义体」** 标签页（`itemGroup.cyberware = 义体`），里面是本版全部义体物品。

### 第 2 步：装 / 卸（操作台）
1. 把 1 件义体放到**快捷栏**（面板底部会把快捷栏 9 格画出来）。
2. 放一个**义体操作台**并右键打开。
3. 点快捷栏那一格**拿起** → 点面板底部**义体槽**放下 → 点该槽（右侧详情切到它）。
4. 点 **「安装」**：标题栏 `容量 已用/上限` 应增加；义体进入玩家义体表。
5. 点 **「卸载」**：应还原成物品（回操作台槽 / 背包 / 掉脚边三级兜底）。
6. 失败时面板内会给出中文原因（容量不足 / 槽位满 / 同型号重复 / 槽里没有 / 身上没装）。

### 第 3 步：容量上限
一直装到超过 100（`cyberware-common.toml` 的 `capacity.defaultCapacity`），应被拒绝并提示容量不足。
想调试无限容量：把该文件 `[capacity] enableCapacityLimit` 改成 `false`（改完重启游戏生效）。

### 第 4 步：义眼描边
装上下面**当前生效的 6 件**里任意一件，然后看生物：

| id | 中文名 |
|---|---|
| `kiroshi_optics_bare` | 基础歧路司义眼 |
| `kiroshi_optics` | 歧路司义眼1型 |
| `kiroshi_optics_combined` | 歧路司义眼神舆 |
| `kiroshi_optics_hunter` | 歧路司义眼祸兆 |
| `kiroshi_optics_wallhack` | 歧路司义眼千里目 |
| `iconic_advanced_kiroshi_optics_bare` | 歧路司义眼石化鸡蛇 |

预期：
- 同队玩家 / 自家宠物 → **绿**（`0xFF34D058`）
- 村民、动物、未结盟玩家等中立 → **黄**（`0xFFFFCC00`）
- 僵尸骷髅等敌对、或正把你当目标的怪 → **红**（`0xFFFF3B30`）
- **卸下义眼后描边应消失**；不装义眼时任何生物都不描边。

> ⚠ **重要：义眼只能验这 6 件。**
> `kiroshi_optics_piercing`（歧路司义眼追猎）与 `kiroshi_optics_sensor`（歧路司义眼警戒）
> 因为**没有贴图素材，定义处于注释状态、未注册**，**游戏内根本不存在这两个物品** ——
> 在创造物品栏里找不到它们是**正常的**，不是 bug。等素材补齐恢复注册后，描边会自动生效（白名单已预留）。

### 第 5 步：12 件新义体的已知情况（不是 bug）
`代谢编辑器 / 全幅抵抗 / 反向电感 / 合成肺叶 / 接地镀层 / 热能转化器 / 生物塑料血管 /
真皮上编束 / 纳米继电器 / 解毒器 / 边缘增强系统 / 防火涂层` 这 12 件**没有官方数据来源**，
槽位、稀有度、容量是 **TODO 初值**（主人已决定先留着）。因此它们出现在哪个分类里可能与直觉不符、
描述里带 `TODO(数值与描述待补)` 字样，这些都是**已知占位**，不代表功能缺陷；数值待主人确认后补。

### 第 6 步：读日志（判定注入是否成功）

```
文件：/sdcard/FCL/.minecraft/versions/26.1.2-NeoForge/logs/latest.log
```

搜这些关键字（**命中任意一个都是问题，请把原文发回来**）：

| 关键字 | 含义 |
|---|---|
| `Mixin apply` + `failed` | 某 mixin 整体应用失败 |
| `InvalidInjectionException` | 注入点匹配/描述符出错 |
| `Invalid name:` / `InvalidMemberDescriptorException` | 描述符写法错（冒号陷阱） |
| `Scanned 0 target(s)` / `Cannot find method` | 目标方法没匹配到（`require=0` 下会静默跳过 → 功能隐形失效） |

同时确认加载的是新版本：

```bash
grep -n "cyberware-" /sdcard/FCL/.minecraft/versions/26.1.2-NeoForge/logs/latest.log
# 应出现 cyberware-0.3.12-Beta.jar
grep -n "Cyberware 义体系统" …/latest.log
# 应出现 0.3.12-Beta
```

### 第 7 步：回滚（万一）
把 `/sdcard/DSH/trash/cyberware/cyberware-0.3.11-Beta-Hotfix-10.jar` 拷回 mods 目录即可
（先把 0.3.12 的 jar 移走，不要两个 jar 同时存在）。

---

## 6. 未做 / 遗留（明确列出，不含糊）

1. **未 git push**（按硬约束）。
2. **真机验证未做** —— 需主人进游戏；本报告所有 Mixin 结论都只是静态证据。
3. `ProjectileMixin`（未登记进 mixins.json 的历史死代码）**未删也未启用**，等主人裁决：
   t11 的分析是建议删除（功能已被 `ClientProjectileDilation` 取代，同时启用会叠加减速导致不同步）。
4. `en_us.json` 未补新定义的英文名（主人裁决只补中文；英文环境会显示未翻译键）。
5. 12 件新定义的槽位/稀有度/容量/数值仍是 TODO 占位。
6. 19 条无素材定义仍注释留档（游戏内不存在），等素材。
7. 义眼白名单里预留的 `kiroshi_optics_piercing` / `kiroshi_optics_sensor` 两条 `has()` 恒为 false（无害）。

---

## 7. 提交记录

| # | commit | 内容 |
|---|---|---|
| 1 | `30e9646cadd76444082b8cde1e273174e63ba98d` | 0.3.12-Beta 全部源码/资源 + 10 份交付文档（425 files, +5515/−641） |
| 2 | `b131559ef462bc895fc13ff80e9e423faaf938c8` | FINAL-HANDOFF.md（本文件首次入库，311 行） |
| 3 | （本行所在提交） | 仅回填第 2 条的 hash，正文内容零改动 |

两次实质提交都在**本地**仓库；未 `git push`。
