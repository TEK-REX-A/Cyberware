# M2-FIX.md —— 7 处历史遗留注入补 `require = 0`（消除黑屏风险）

> 任务：t11（opticsdev，attempt `5d9155b0-ffd2-4896-80dc-b995f7d7b012`）
> 来源：inspector 的 `VERIFY-REPORT.md` §11 **M2**（§2.3 给出逐注入点统计）
> 结论：**7 处全部补齐**；全目录 13 处注入**现在 13/13 都显式带 `require = 0`**。
> ⚠ **未真机验证**（见 §8）。

---

## 1. 为什么补这一项：`defaultRequire = 1` 的杀伤力

`src/main/resources/cyberware.mixins.json` 明文：

```json
  "injectors": {
    "defaultRequire": 1
  }
```

`defaultRequire = 1` 的含义是「没写 `require` 的注入，默认要求**至少命中 1 个**目标」。
一旦某个注入点因为版本更新、方法改名、描述符对不上而匹配失败，Mixin 会直接
**`Mixin apply failed` → 整包 mixin 应用失败 → 客户端黑屏**。

显式写 `require = 0` 把这个「失败」降级为「安静跳过」：最坏结果只是**那一个**装饰性/辅助功能
不生效，游戏本体照常启动。交接文档的铁律就是这么定的。

> 补充（不改变结论）：0.3.11 的真机 `latest.log` 里**没有**任何
> `InvalidInjectionException` / `Mixin apply failed`（`VERIFY-REPORT.md` §12），
> 且全部目标方法都已 javap 确认存在。也就是说**这 7 处今天都能匹配上，行为不会因为本次改动而改变**。
> 本次改动是**缩小爆炸半径**，不是修一个正在发生的故障。

---

## 2. 7 处逐条（改前 → 改后）

行号均为**当前工作区**的行号（`grep -n` 实测，未照抄报告）。

| # | 类 | 方法 | 注入点 | 注入器 | 行号 | 改前 | 改后 |
|---|---|---|---|---|---|---|---|
| 1 | `GuiMixin` | `extractPlayerHealth` | `HEAD` | `@Inject` (cancellable) | 21 | `@Inject(method = "extractPlayerHealth", at = @At("HEAD"), cancellable = true)` | 末尾加 `, require = 0` |
| 2 | `SoundEngineMixin` | `calculatePitch` | `RETURN` | `@Inject` (cancellable) | 28 | `@Inject(method = "calculatePitch", at = @At("RETURN"), cancellable = true)` | 末尾加 `, require = 0` |
| 3 | `ParticleMixin` | `tick` | `RETURN` | `@Inject` | 46 | `@Inject(method = "tick", at = @At("RETURN"))` | 末尾加 `, require = 0` |
| 4 | `ProjectileMixin` | `tick` | `RETURN` | `@Inject` | 27 | `@Inject(method = "tick", at = @At("RETURN"))` | 末尾加 `, require = 0` |
| 5 | `GameRendererMixin` | `render` | `INVOKE`（`LevelRenderer.doEntityOutline()V`, `shift = AFTER`） | `@Inject` | 40–48（`require = 0` 在 **47** 行） | 注解以 `)` 收尾，无 `require` | `at = @At(...)` 之后加一行 `require = 0` |
| 6 | `WeatherEffectRendererMixin` | `extractRenderState` | `HEAD`（`argsOnly, index = 2`） | `@ModifyVariable` | 19 | `@ModifyVariable(..., index = 2)` | 末尾加 `, require = 0` |
| 7 | `WeatherEffectRendererMixin` | `extractRenderState` | `HEAD`（`argsOnly, index = 3`） | `@ModifyVariable` | 25 | `@ModifyVariable(..., index = 3)` | 末尾加 `, require = 0` |

改后的原文行（`grep -n "require = 0"`，可复现）：

```
GuiMixin.java:21:    @Inject(method = "extractPlayerHealth", at = @At("HEAD"), cancellable = true, require = 0)
SoundEngineMixin.java:28:    @Inject(method = "calculatePitch", at = @At("RETURN"), cancellable = true, require = 0)
ParticleMixin.java:46:    @Inject(method = "tick", at = @At("RETURN"), require = 0)
ProjectileMixin.java:27:    @Inject(method = "tick", at = @At("RETURN"), require = 0)
GameRendererMixin.java:47:            require = 0
WeatherEffectRendererMixin.java:19:    @ModifyVariable(method = "extractRenderState", at = @At("HEAD"), argsOnly = true, index = 2, require = 0)
WeatherEffectRendererMixin.java:25:    @ModifyVariable(method = "extractRenderState", at = @At("HEAD"), argsOnly = true, index = 3, require = 0)
```

未改动：`EntityRendererOutlineMixin`（t5 新增时本来就是 `require = 0`）。

### 2.1 「只加了 require」的机器证明

```
git diff --stat src/main/java/com/dsh/cyberware/mixin/client/
 GameRendererMixin.java          | 3 ++-
 GuiMixin.java                   | 2 +-
 ParticleMixin.java              | 2 +-
 ProjectileMixin.java            | 2 +-
 SoundEngineMixin.java           | 2 +-
 WeatherEffectRendererMixin.java | 4 ++--
 6 files changed, 8 insertions(+), 7 deletions(-)
```

全部增删行（`git diff -U0`，逐行列出，没有任何一行是别的改动）：

```
-            )
+            ),
+            require = 0
-    @Inject(method = "extractPlayerHealth", at = @At("HEAD"), cancellable = true)
+    @Inject(method = "extractPlayerHealth", at = @At("HEAD"), cancellable = true, require = 0)
-    @Inject(method = "tick", at = @At("RETURN"))
+    @Inject(method = "tick", at = @At("RETURN"), require = 0)
-    @Inject(method = "tick", at = @At("RETURN"))
+    @Inject(method = "tick", at = @At("RETURN"), require = 0)
-    @Inject(method = "calculatePitch", at = @At("RETURN"), cancellable = true)
+    @Inject(method = "calculatePitch", at = @At("RETURN"), cancellable = true, require = 0)
-    @ModifyVariable(method = "extractRenderState", at = @At("HEAD"), argsOnly = true, index = 2)
+    @ModifyVariable(method = "extractRenderState", at = @At("HEAD"), argsOnly = true, index = 2, require = 0)
-    @ModifyVariable(method = "extractRenderState", at = @At("HEAD"), argsOnly = true, index = 3)
+    @ModifyVariable(method = "extractRenderState", at = @At("HEAD"), argsOnly = true, index = 3, require = 0)
```

**没有改注入点、没有改方法体、没有重构、没有任何 target 描述符被碰过**
（`method` / `at` / `target` / `index` / `cancellable` 的值逐字符与改前一致）。

---

## 3. 全目录收口统计（13/13）

grep 整个 `mixin/client/**` 后逐条解析注解（脚本按括号配平取整段注解，不是只看单行）：

| 类 | 行 | 注入器 | 注入点 | `require` | method |
|---|---|---|---|---|---|
| `CapeLayerMixin` | 38 | `@Redirect` | `INVOKE` | **0** | `submit(...AvatarRenderState;FF)V` |
| `EntityRendererOutlineMixin` | 111 | `@Inject` | `TAIL` | **0** | `extractRenderState(...EntityRenderState;F)V` |
| `EquipmentLayerRendererMixin` | 53 | `@Redirect` | `INVOKE` | **0** | `renderLayers(...)V` |
| `EquipmentLayerRendererMixin` | 81 | `@Redirect` | `INVOKE` | **0** | `renderLayers(...)V` |
| `EquipmentLayerRendererMixin` | 111 | `@Redirect` | `INVOKE` | **0** | `renderLayers(...)V` |
| `EquipmentLayerRendererMixin` | 132 | `@ModifyArg` | `INVOKE` | **0** | `renderLayers(...)V` |
| `GameRendererMixin` | 40 | `@Inject` | `INVOKE` | **0** | `render` |
| `GuiMixin` | 21 | `@Inject` | `HEAD` | **0** | `extractPlayerHealth` |
| `ParticleMixin` | 46 | `@Inject` | `RETURN` | **0** | `tick` |
| `ProjectileMixin` | 27 | `@Inject` | `RETURN` | **0** | `tick` |
| `SoundEngineMixin` | 28 | `@Inject` | `RETURN` | **0** | `calculatePitch` |
| `WeatherEffectRendererMixin` | 19 | `@ModifyVariable` | `HEAD` | **0** | `extractRenderState` |
| `WeatherEffectRendererMixin` | 25 | `@ModifyVariable` | `HEAD` | **0** | `extractRenderState` |

```
注入器总数 = 13   带 require 的 = 13   缺失 = 0
```

**不适用 `require` 的成员**（accessor 类，`@Accessor`/`@Invoker` 没有 `require` 参数）：

```
LivingEntityRendererAccessor.java   3 处
PostChainAccessor.java              1 处
PostPassAccessor.java               1 处
```

（这 5 处不是注入器，不参与 `defaultRequire` 的失败判定。）

按注入器种类汇总：`@Inject` 6、`@Redirect` 4、`@ModifyArg` 1、`@ModifyVariable` 2，合计 **13**。

---

## 4. ProjectileMixin 现状与建议

### 4.1 现状（三件事，全部已核实）

1. **它当前没有登记进 `cyberware.mixins.json`**，因此**根本不会被应用**：
   `grep -c ProjectileMixin src/main/resources/cyberware.mixins.json` → **0**；
   json 的 `client` 数组 11 项里没有它，而 `mixin/client/` 目录下有 12 个类
   （唯一没登记的就是它）。`VERIFY-REPORT.md` §11 的 **M1** 记录的正是这件事。
2. 本次仍然给它补了 `require = 0` —— 无害且风格一致（万一将来登记启用，不至于是个例外）。
3. 它的注入目标**是真实存在的**：`javap` 确认
   `net.minecraft.world.entity.projectile.Projectile` 有 `public void tick();`。
   也就是说「登记它就会生效」，不会因为目标不存在而自动作废。

### 4.2 建议：**删除**（而不是登记启用）

理由 —— 它不是「忘了登记的可用功能」，而是**已被取代的旧方案**：

| | `ProjectileMixin`（旧方案，未登记） | `ClientProjectileDilation`（当前方案，已生效） |
|---|---|---|
| 机制 | `@Inject` 到 `Projectile.tick` 的 `RETURN`，**tick 跑完后把这一 tick 的位移按倍率缩回去** | `EntityTickEvent.Pre` 监听器，用 `DilationTickGate.shouldSkip(entity, timeScale)` **按「游戏刻 + 实体 id」整刻跳过**，与服务端同相位 |
| 接线 | 无（未登记） | `CyberwareClient.java:43` → `NeoForge.EVENT_BUS.addListener(ClientProjectileDilation::onEntityTickPre)` |
| 设计意图 | 位移插值式减速 | 「时间被切碎的本来面目」（`ClientProjectileDilation` 类注释：主人明确选的方案） |

**关键风险**：如果把它登记启用，两个机制会**叠加**——被 `DilationTickGate` 跳过的刻本来就不动，
而「没被跳过的刻」还会被 mixin 再把位移乘一次 `timeScale`。后果是客户端投射物比服务端模拟
**更慢**，位置持续对不上，出现抖动/回拉。这与 `CyberwareClient` 里那段注释
（「这里**不再**缩放生物动画……再乘一次只会变成『腿不动却在平移』」）是同一类错误。

所以：
- **建议删除 `ProjectileMixin.java`**（真要留作历史记录，也应留在提交历史里，而不是留在源码树里当死代码）。
- **不建议登记启用**。
- 按任务要求，**我没有删也没有登记** —— 这需要主人定。

> 顺带一提：这个文件的死代码状态与本次 `require = 0` 完全无关；补 `require` 只是让它「将来无论启用与否都不构成黑屏风险」。

---

## 5. 构建验证（全量 build，exit 0）

按任务要求本次跑的是**全量 build**（t6 已结束，不存在抢锁）：

```
cd /root/mod26/cyberware
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-arm64 \
PATH=$JAVA_HOME/bin:/opt/gradle-8.10.2/bin:$PATH \
gradle build --no-daemon
```

原始尾部输出：

```
> Task :createMinecraftArtifacts UP-TO-DATE
> Task :compileJava
> Task :processResources UP-TO-DATE
> Task :classes
> Task :jarJar NO-SOURCE
> Task :jar
> Task :assemble
> Task :compileTestJava NO-SOURCE
> Task :processTestResources NO-SOURCE
> Task :testClasses UP-TO-DATE
> Task :test NO-SOURCE
> Task :check UP-TO-DATE
> Task :build

BUILD SUCCESSFUL in 2m 21s
4 actionable tasks: 2 executed, 2 up-to-date
```

**退出码 = 0**（`PIPE_EXIT=0`）。未跑 `clean`（按硬约束）。未 commit。

产物：

```
build/libs/cyberware-0.3.12-Beta.jar   655477 bytes
```

—— 文件名仍是 **0.3.12-Beta**。jar 内的 `cyberware.mixins.json` 与源码一致，
`injectors.defaultRequire` 仍为 `1`（本次只改注入的 `require`，没有动全局默认值）。

---

## 6. 产物级验证：javap -v 从 jar 里读注解

从 **jar 内**取出的 class（不是 `build/classes` 的中间产物）：

```
jar xf build/libs/cyberware-0.3.12-Beta.jar com/dsh/cyberware/mixin/
javap -v -p com/dsh/cyberware/mixin/client/<类>.class
```

7 处**全部**核验（常量池 `RuntimeVisibleAnnotations` 片段）：

```
── GuiMixin ──                          ── SoundEngineMixin ──
  org.spongepowered.asm.mixin.injection.Inject(      org.spongepowered.asm.mixin.injection.Inject(
    method=["extractPlayerHealth"]                      method=["calculatePitch"]
    value="HEAD"                                        value="RETURN"
    cancellable=true                                    cancellable=true
    require=0                                           require=0

── ParticleMixin ──                     ── ProjectileMixin ──
  org.spongepowered.asm.mixin.injection.Inject(      org.spongepowered.asm.mixin.injection.Inject(
    method=["tick"]                                     method=["tick"]
    value="RETURN"                                      value="RETURN"
    require=0                                           require=0

── GameRendererMixin ──                 ── WeatherEffectRendererMixin（2 处）──
  org.spongepowered.asm.mixin.injection.Inject(      org.spongepowered.asm.mixin.injection.ModifyVariable(
    method=["render"]                                   method=["extractRenderState"]
    value="INVOKE"                                      value="HEAD"
    require=0                                           index=2
                                                        require=0
                                                      org.spongepowered.asm.mixin.injection.ModifyVariable(
                                                        method=["extractRenderState"]
                                                        value="HEAD"
                                                        index=3
                                                        require=0
```

对 jar 内**全部 12 个 mixin 类**做汇总统计：

```
require=0          出现 13 次   ← 13 个注入器全覆盖，没有别的取值
@Inject            6
@Redirect          4
@ModifyArg         1
@ModifyVariable    2
```

### 6.1 注入点本身没被改动的产物级佐证

同一份 javap 输出里，`value` / `index` / `cancellable` 与 §2 的「改前」一一对应：

```
value="HEAD"(×3: GuiMixin, WeatherEffect×2) · value="RETURN"(×3: SoundEngine, Particle, Projectile)
value="INVOKE"(GameRenderer) · index=2 / index=3(WeatherEffect)
cancellable=true(GuiMixin, SoundEngineMixin)
```

---

## 7. 冒号陷阱：t6 的「0 命中」结论继续有效

本次**只新增了 `require = 0` 这一个注解参数**，没有触碰任何 `method` / `target` 字符串，
所以不可能引入新的描述符语法问题。两个独立复检：

**源码级**（整个 mixin 目录）：

```
grep -rnE '"[A-Za-z0-9_$]+:' src/main/java/com/dsh/cyberware/mixin/
→ 无命中（exit=1）
```

**产物级**（jar 内全部 mixin class 的 `method=[...]` 字符串，去重后）：

```
method=["calculatePitch"]
method=["extractPlayerHealth"]
method=["extractRenderState"]
method=["extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V"]
method=["render"]
method=["renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V"]
method=["submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V"]
method=["tick"]
```

逐条自查：**没有任何一个方法名与左括号之间存在冒号**。
（`javap` 显示用的那个 `owner.name:(...)` 冒号只是它的排版分隔符，从没被抄进代码。）

---

## 8. ⚠ 未真机验证 —— 明确声明

**本次没有启动过游戏，`require = 0` 的实际效果没有在真机上验证过。**

改动本身是「注解参数」级的，编译期**不校验**任何 Mixin 注解字符串，因此
`gradle build` 通过 **只证明**「能编译、jar 能打出来」，
**不证明**注入真的命中、也不证明黑屏风险真的被消除。

真机（或 `runClient`）启动后必须在日志里确认：

| 关键字 | 期望 | 含义 |
|---|---|---|
| `Mixin apply for mod cyberware failed` | **不出现** | 整包 mixin 应用失败 → 黑屏 |
| `Mixin apply ... failed` | **不出现** | 单个 mixin 应用失败 |
| `InvalidInjectionException` | **不出现** | 注入点找不到 / 描述符对不上 |
| `Invalid name:` | **不出现** | target 描述符非法（冒号、拼错） |
| `Scanned 0 target(s)` / `require` 相关警告 | 允许出现 | 说明某个注入没匹配上，但**已被 `require = 0` 降级为跳过**，游戏不崩 |

同时确认功能面：雨雪随斯安威斯坦变慢（`WeatherEffectRendererMixin`）、世界音调降低
（`SoundEngineMixin`）、粒子变慢（`ParticleMixin`）、狂暴时血条隐藏（`GuiMixin`）、
斯安威斯坦后处理出现在世界画完/GUI 之前（`GameRendererMixin`）、
义眼描边（`EntityRendererOutlineMixin`）。

> 说明：`ProjectileMixin` 无法做功能验证——它没被登记，不会应用（§4）。

---

## 9. 改动文件清单（仅 6 个，均在独占范围内）

1. `src/main/java/com/dsh/cyberware/mixin/client/GuiMixin.java`
2. `src/main/java/com/dsh/cyberware/mixin/client/SoundEngineMixin.java`
3. `src/main/java/com/dsh/cyberware/mixin/client/ParticleMixin.java`
4. `src/main/java/com/dsh/cyberware/mixin/client/ProjectileMixin.java`
5. `src/main/java/com/dsh/cyberware/mixin/client/GameRendererMixin.java`
6. `src/main/java/com/dsh/cyberware/mixin/client/WeatherEffectRendererMixin.java`

`cyberware.mixins.json` **未改动**（无需要），`EntityRendererOutlineMixin.java` **未改动**，
`mixin/client/**` 之外的任何文件 **未改动**，未 commit。
