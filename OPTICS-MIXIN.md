# OPTICS-MIXIN.md —— 义眼敌我识别描边（t5）

> 交付物：`src/main/java/com/dsh/cyberware/mixin/client/EntityRendererOutlineMixin.java`
> +
> `src/main/resources/cyberware.mixins.json`（client 数组新增一项）
>
> 本文件只记录**证据与规则**，不含实现细节的第二次描述。
> ⚠ **本功能未在真机验证过。编译通过 ≠ 注入成功。**（见文末「验证状态」）

---

## 1. javap 原始输出（逐字符来源，未凭记忆）

反编译对象：`build/moddev/artifacts/minecraft-patched-26.1.2.109-merged.jar`
（NeoForge `26.1.2.109` 的 patched + merged 产物，即编译期与运行期同一套 Mojang 官方映射名）。

命令：

```
/usr/lib/jvm/java-21-openjdk-arm64/bin/javap -p -s \
  -classpath build/moddev/artifacts/minecraft-patched-26.1.2.109-merged.jar \
  net.minecraft.client.renderer.entity.EntityRenderer
```

原始输出（节选，行号 71–72）：

```
  public void extractRenderState(T, S, float);
    descriptor: (Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V
```

补充证据 —— 目标类声明（同一次 javap 输出首行）：

```
public abstract class net.minecraft.client.renderer.entity.EntityRenderer<T extends net.minecraft.world.entity.Entity, S extends net.minecraft.client.renderer.entity.state.EntityRenderState> {
```

即：`extractRenderState` 是泛型方法，字节码里的参数类型是**擦除后的上界**
`Entity` / `EntityRenderState`（不是 `T`/`S`），`target` 描述符必须照抄上面
`descriptor:` 那一行的原样。

### 1.1 outlineColor 的唯一赋值点（字节码级）

```
/usr/lib/jvm/java-21-openjdk-arm64/bin/javap -p -c ... net.minecraft.client.renderer.entity.EntityRenderer
```

`extractRenderState` 尾部原始输出（节选）：

```
     929: invokestatic  #533   // Method net/minecraft/client/Minecraft.getInstance:()Lnet/minecraft/client/Minecraft;
     932: astore        4
     934: aload         4
     936: aload_1
     937: invokevirtual #539   // Method net/minecraft/client/Minecraft.shouldEntityAppearGlowing:(Lnet/minecraft/world/entity/Entity;)Z
     940: istore        5
     942: aload_2
     943: iload         5
     945: ifeq          958
     948: aload_1
     949: invokevirtual #542   // Method net/minecraft/world/entity/Entity.getTeamColor:()I
     952: invokestatic  #545   // Method net/minecraft/util/ARGB.opaque:(I)I
     955: goto          959
     958: iconst_0
     959: putfield      #551   // Field net/minecraft/client/renderer/entity/state/EntityRenderState.outlineColor:I
     962: aload_2
     963: aload_0
     964: aload_1
     965: fload_3
     966: invokevirtual #554   // Method getPackedLightCoords:(Lnet/minecraft/world/entity/Entity;F)I
     969: putfield      #240   // Field net/minecraft/client/renderer/entity/state/EntityRenderState.lightCoords:I
     972: return
```

`javap -p -l` 的 LineNumberTable 对应：

```
      line 283: 942
      line 284: 962
      line 285: 972
```

→ **`EntityRenderer.java:283` 就是那句 `state.outlineColor = ...`，bci 959 的 `putfield` 之后
紧接着只有 `lightCoords` 与 `return`，没有别的分支再写描边色。**

全量核对（不是只看这一个文件）——把 `minecraft-patched-26.1.2.109-sources.jar` 整包解开后：

```
grep -rn "outlineColor *=" --include=*.java net/minecraft/
```

命中的**实体渲染**相关赋值只有一条：

```
net/minecraft/client/renderer/entity/EntityRenderer.java:283:        state.outlineColor = appearsGlowing ? ARGB.opaque(entity.getTeamColor()) : 0;
```

其余命中全部无关（GUI 选择框局部变量 `AbstractSelectionList.java:353` / `ChatSelectionScreen.java:187`、
`InventoryScreen.java:146` 与 `ItemPickupParticle.java:27` 里的**物品**渲染状态、
`OutlineBufferSource.java:13/32` 的私有字段、`LevelRenderer.java:1082` 的调试线框），
`LevelRenderer.java:912` 是**读后清零**（见 §3）。

**结论**：`EntityRenderer.extractRenderState` 的 `TAIL` 就是「实体描边色最后一次被写入之后」，
注入到这里不会被任何子类 `extractRenderState` 覆盖（全树没有第二处赋值）。

---

## 2. 最终 target 字符串（逐字符，⛔ 无冒号）

注解里的最终字符串，**原样**（单行，无换行、无空格）：

```
extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V
```

逐段拆开对照：

| 片段 | 内容 | 说明 |
|---|---|---|
| 方法名 | `extractRenderState` | ✅ 名字与 `(` **直接相邻**，中间**没有** `:` |
| 参数 | `Lnet/minecraft/world/entity/Entity;` | `T` 的擦除上界 |
| 参数 | `Lnet/minecraft/client/renderer/entity/state/EntityRenderState;` | `S` 的擦除上界 |
| 参数 | `F` | `float partialTicks` |
| 返回 | `V` | `void` |

**冒号自查（已执行）**：

```
grep -rnE '(method|target) *= *"' src/main/java/com/dsh/cyberware/mixin/client/EntityRendererOutlineMixin.java
# 76:        method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V",

grep -rnE '"[A-Za-z0-9_$]+:' src/main/java/com/dsh/cyberware/mixin/client/EntityRendererOutlineMixin.java
# 无输出（exit=1）→ 没有任何「方法名 + 冒号 + 左括号」的写法
```

> 本类**只有一个** `method =` 字符串，也**没有任何** `target =`（`@Inject` 只用 `method`）。
> `EntityRendererOutlineMixin.java:76` 那一行就是全部。

### 2.1 编译产物交叉核对

```
javap -p -s build/classes/java/main/com/dsh/cyberware/mixin/client/EntityRendererOutlineMixin.class
```

```
  private void cyberware$applyFriendFoeOutline(net.minecraft.world.entity.Entity, net.minecraft.client.renderer.entity.state.EntityRenderState, float, org.spongepowered.asm.mixin.injection.callback.CallbackInfo);
    descriptor: (Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;FLorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V
```

处理器描述符的前 3 个参数与 §1 的目标描述符**逐字符相同**，末位是 `@Inject` 要求的
`CallbackInfo`。

---

## 3. 注入点与 require 值

| 项 | 值 |
|---|---|
| 目标类 | `net.minecraft.client.renderer.entity.EntityRenderer`（`@Mixin(EntityRenderer.class)`） |
| 目标方法 | `extractRenderState`（见 §2 的完整描述符） |
| 注入点 | `@At("TAIL")` —— 方法最后一个 `RETURN` 之前，即原版写完 `outlineColor`、写完 `lightCoords` 之后 |
| **`require`** | **`0`（显式写在注解上）** |
| 处理器 | `private void cyberware$applyFriendFoeOutline(Entity, EntityRenderState, float, CallbackInfo)` |

`require = 0` 的理由：`cyberware.mixins.json` 里 `injectors.defaultRequire = 1`，
意味着**任何一处注入匹配失败 = 整包 mixin 应用失败 = 黑屏**。
描边是装饰性功能，最坏结果只允许是「没有描边」。

> ⚠ `require = 0` 只兜「匹配到 0 个目标」。它**兜不住**描述符语法错误或成员不存在——
> 那种情况在 `prepareInjections` 阶段就抛异常，直接中断整个 mixin 应用。
> 所以 §1/§2 的逐字符照抄 + 冒号自查才是真保险。

### 3.1 改了颜色为什么真的会画出来（链路核对）

1. `EntityRenderState.appearsGlowing()` 的实现就是 `outlineColor != 0`
   （`net/minecraft/client/renderer/entity/state/EntityRenderState.java:41-42`）；
2. `LevelRenderer.extractVisibleEntities` 每抽取完一个实体就检查：
   ```java
   EntityRenderState state = this.extractEntity(entity, partialEntity);
   output.entityRenderStates.add(state);
   if (state.appearsGlowing() && shouldShowEntityOutlines) {
       output.haveGlowingEntities = true;
   }
   ```
   （`LevelRenderer.java:889-896`）——我们的注入发生在 `extractEntity → createRenderState →
   extractRenderState` 内部，所以**来得及**在这一步之前把颜色写进去；
3. `LevelRenderer.submitEntities` 只在「本帧没有任何发光实体」时才把颜色清掉：
   ```java
   if (!levelRenderState.haveGlowingEntities) {
       state.outlineColor = 0;
   }
   ```
   （`LevelRenderer.java:910-913`）；
4. `shouldShowEntityOutlines()` 的条件是
   `!cameraRenderState.isPanoramicMode && entityOutlineTarget != null && player != null`
   （`LevelRenderer.java:242-244`），而 `entityOutlineTarget` 在 `resize()` 里**无条件创建**
   （`LevelRenderer.java:227`），不受任何视频选项开关影响。

也就是说：**只要义眼判定通过，`outlineColor` 就会变成对应颜色并进入描边 pass。**

---

## 4. 颜色判定规则

颜色为 ARGB int（`0xAARRGGBB`，alpha 恒为 `FF`）。

| 关系 | 颜色 | ARGB | 规则 |
|---|---|---|---|
| 友好 | 绿 | `0xFF34D058` | `viewer.isAlliedTo(target)` —— 同一队伍（`PlayerTeam`），或属于该玩家的驯服生物（狼/猫等原版 `isAlliedTo` 覆盖的情形） |
| 敌对 | 红 | `0xFFFF3B30` | `target instanceof Enemy`（原版敌对生物标记接口），**或** `target instanceof Mob mob && mob.getTarget() == viewer`（被激怒的中立怪/正在锁定你的怪物） |
| 中立 | 黄 | `0xFFFFCC00` | 其余一切生物：动物、村民、铁傀儡、未结盟玩家…… |

判定顺序固定为：**同盟 → 敌对 → 中立**（先判同盟，避免「同队的僵尸」被判成红）。

不描边（保持原值）的情形：

1. `Minecraft.getInstance().player == null`；
2. 被渲染实体就是本地玩家自己（`entity == viewer`）；
3. 类型上确认是本地玩家自己：`state instanceof AvatarRenderState avatar && avatar.id == viewer.getId()`
   —— **认类型 + 认 id，绝不用坐标猜**（监守者贴脸也不会让我们误判，更不会 ClassCastException）；
4. 不是 `LivingEntity`（掉落物/箭矢/矿车/画框套黄边纯属刷屏，故意排除）；
5. 本地玩家身上没有义眼（见下）。

### 4.1 「装了义眼」的判定

只认**义眼**型号，即 `CyberwareSlot.FACE` 里的歧路司义眼系列（`CyberwareDefinitions.java`）。
白名单共 8 条，**每一条的状态都已在代码注释与本表里写死**（0.3.12 逐条核对，
当前生效定义 135 条）：

| # | id | 中文名 | 当前状态 | 依据 |
|---|---|---|---|---|
| 1 | `kiroshi_optics_bare` | 基础歧路司义眼 | ✅ 已注册 · 生效 | 135 条生效定义之一 |
| 2 | `kiroshi_optics` | 歧路司义眼1型 | ✅ 已注册 · 生效 | 同上 |
| 3 | `kiroshi_optics_combined` | 歧路司义眼神舆 | ✅ 已注册 · 生效 | 同上 |
| 4 | `kiroshi_optics_hunter` | 歧路司义眼祸兆 | ✅ 已注册 · 生效 | 同上 |
| 5 | `kiroshi_optics_piercing` | 歧路司义眼追猎 | ⚠️ **暂未注册**（故意保留） | 无贴图素材 → 定义被整块注释，见 `ASSET-MISSING.txt` #6 |
| 6 | `kiroshi_optics_sensor` | 歧路司义眼警戒 | ⚠️ **暂未注册**（故意保留） | 无贴图素材 → 定义被整块注释，见 `ASSET-MISSING.txt` #7 |
| 7 | `kiroshi_optics_wallhack` | 歧路司义眼千里目 | ✅ 已注册 · 生效 | 135 条生效定义之一 |
| 8 | `iconic_advanced_kiroshi_optics_bare` | 歧路司义眼石化鸡蛇 | ✅ 已注册 · 生效 | 同上 |

核对方法（可复现）：把 `CyberwareDefinitions.java` 的 `//` 行注释与 `/* */` 块注释剥掉后，
用 `register\(new CyberwareDefinition\(\s*"([a-z0-9_]+)"` 抽出**生效** id 集合 —— 得到 135 条，
与 `CyberwareDefinitions` 的 register 数一致；再拿白名单 8 条逐一比对。
同时反查：当前生效的 `CyberwareSlot.FACE` 定义共 6 条，**全部落在白名单内，无遗漏**。

`mask_cw_plus_plus`（行为特征脸板）虽然是 `CyberwareSlot.FACE`，但**不是眼睛，故意排除**。

> **死条目处理：选方案 A（保留，加注释说明）**，理由与 captain 的倾向一致：
> - 这两条**不是 bug**，而是「等素材」。它们永远不会命中（`has()` 恒 false），
>   但删掉之后，一旦素材补齐、`CyberwareDefinitions` 恢复注册这两个定义，
>   **描边会悄悄不生效**——而改定义表的人不一定会想到回来改这个渲染 mixin，这种问题极难查。
> - 保留的代价是每帧多两次必然为 false 的 `has()` 查询（HashMap 查找，可忽略）。
> - 代码里每个条目的状态都写在注释上（哪条生效、哪条待素材、去哪看原因），
>   所以不会留下「无解释的死条目」——这正是 t10 要解决的问题。
>
> 恢复注册时**不需要改本文件**（白名单已含这两个 id）；需要改的只有
> `CyberwareDefinitions.java` 与配套素材。

> **已切换到 core 侧查询 API（无占位实现）**：
> 本任务开工时 coredev 的 `CyberwareInstallation.has(Player, String)` / `dataOf(Player, String)`
> 尚未落地，中途落地后已把实现切成 core 侧查询：
>
> ```java
> for (String id : CYBERWARE_OPTICS_IDS) {
>     if (CyberwareInstallation.has(viewer, id)) {
>         return true;
>     }
> }
> ```
>
> 即 `CyberwareInstallation.has(Player, String)`（`CyberwareInstallation.java:144`，
> 内部走 `of(Player)` → `getExistingDataOrNull(ModAttachments.INSTALLATION.get())`，
> 永不返回 null、不为没装过义体的玩家创建空表）。
>
> **没有自己实现任何玩家存储层**——存储（`CyberwareInstallation` +
> `ModAttachments.INSTALLATION`，`.serialize().copyOnDeath().sync()`）完全在 core 侧，
> 本类只是调用方。本类**不 import `ModAttachments`**，也没有任何 `getData`/`setData` 调用。
>
> 已知边界：NeoForge 的附件同步只把玩家自己的 Attachment 发给该玩家的客户端，
> 所以这里只能可靠地查询**本地玩家自己**装了什么。本功能恰好只需要这一点。

### 4.2 覆盖语义

当义眼判定通过时**无条件覆盖** `state.outlineColor`，包括原版因「发光效果/光谱箭」而设的
队伍色描边（`EntityRenderer.java:283`）。这是**故意**的选择：
需求是「义眼看到的敌我识别颜色」，让原版发光色盖掉敌我色会让功能时灵时不灵。
副作用：中了发光效果的生物，装了义眼的玩家看到的是敌我色而不是队伍色。

### 4.3 异常与线程安全

渲染回调整体包在 `try/catch(Throwable)` 里：渲染线程抛异常 = 当场崩客户端，
装饰性功能不该让玩家买单。异常只记**一次** `Cyberware.LOGGER.warn`（不刷屏），
之后本帧及后续帧静默跳过描边。

---

## 5. 注册与改动范围

`src/main/resources/cyberware.mixins.json` 的 `client` 数组末尾新增（注意 JSON 逗号）：

```json
"client.CapeLayerMixin",
"client.EntityRendererOutlineMixin"
```

`python3 -c "import json; json.load(...)"` 校验通过，数组共 11 项。

改动文件（**仅这些**）：

1. `src/main/java/com/dsh/cyberware/mixin/client/EntityRendererOutlineMixin.java`（新增）
2. `src/main/resources/cyberware.mixins.json`（新增一行注册）
3. `OPTICS-MIXIN.md`（本文件）

没有碰渲染类型、没有碰护甲/披风相关代码、没有碰 `CyberwareInstallation.java` /
`ModAttachments.java` / `CyberwareDefinitions.java`。

---

## 6. 验证状态（⚠ 必读）

### 6.1 验收命令与真实结果（含一次失败，如实记录）

```
cd /root/mod26/cyberware
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-arm64 \
PATH=$JAVA_HOME/bin:/opt/gradle-8.10.2/bin:$PATH \
gradle compileJava --no-daemon
```

**第 1 次（00:41，本 mixin 已落地，当时工作树一致）—— 通过：**

```
> Task :compileJava
BUILD SUCCESSFUL in 2m 42s
2 actionable tasks: 1 executed, 1 up-to-date
```

**第 2 次（00:52，切到 core 查询 API 后复跑）—— 失败，但失败点不在本任务范围内：**

```
src/main/java/com/dsh/cyberware/client/CyberwareStationScreen.java:57: error: cannot find symbol
    private CyberwareDefinition selectedDef = CyberwareDefinitions.SANDEVISTAN_ZETATECH;
src/main/java/com/dsh/cyberware/CyberwareCommands.java:35: error: cannot find symbol
    CyberwareDefinition def = CyberwareDefinitions.SANDEVISTAN_ZETATECH;
2 errors
> Task :compileJava FAILED
```

**归因（有证据、非猜测）**：另一位成员的 `CyberwareDefinitions.java`
（00:48:32 写入）把常量 `SANDEVISTAN_ZETATECH` 改名为 `SANDEVISTAN_APOGEE` /
`SANDEVISTAN_C1..C4`，而上面两个**引用点**没人同步。
这两个文件**不在本任务的独占范围内**（本任务只拥有
`mixin/client/**` + `cyberware.mixins.json`），因此按硬约束**没有去改它们**，
只把该发现上报给了队长。它们修好之前，**任何人的**全量 `compileJava` 都不会通过。

**第 3 步：隔离证明「本文件本身可编译」**（用最小桩提供 `Cyberware.LOGGER`，
不依赖那两个坏文件，源码与交付版本逐字节相同）：

```
/opt/jdk25/bin/javac -proc:none -encoding UTF-8 -d /tmp/optics-verify \
  -cp "<所有依赖 jar>:build/moddev/artifacts/minecraft-patched-26.1.2.109-merged.jar:build/classes/java/main" \
  /tmp/stubsrc/com/dsh/cyberware/Cyberware.java \
  src/main/java/com/dsh/cyberware/core/CyberwareInstallation.java \
  src/main/java/com/dsh/cyberware/mixin/client/EntityRendererOutlineMixin.java
→ javac exit=0，产出 EntityRendererOutlineMixin.class
```

即：`EntityRendererOutlineMixin.java` 与它新用到的 core API
（`CyberwareInstallation.has(Player, String)`）**在源码层面确实成立**，
第 2 次的失败与本文件无关。

**第 4 步（那 2 处红灯被修复后，复跑原样验收命令）—— 通过：**

陈旧引用后来经队长授权改成 `CyberwareDefinitions.SANDEVISTAN_C4`
（`CyberwareStationScreen.java:62` / `CyberwareCommands.java:36`）。修复后复跑：

```
> Task :compileJava UP-TO-DATE
BUILD SUCCESSFUL in 1m 23s
2 actionable tasks: 2 up-to-date
```

（另在删掉本类的 `.class` 产物后复跑一次：`BUILD SUCCESSFUL` / `compileJava FROM-CACHE`，exit 0。）

**产物级佐证** —— 用 `javap -v` 读 `build/classes/.../EntityRendererOutlineMixin.class`
（该 class 编译于本文件源码之后）的常量池与注解：

```
#144 = Utf8               extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V
#148 = Utf8               TAIL
#149 = Utf8               require
    RuntimeVisibleAnnotations:
          method=["extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V"]
            value="TAIL"
          require=0
```

—— 真正落进字节码的 target 字符串与 §2 逐字符一致，**无冒号**。

（全程只跑 `compileJava`，**没有**跑完整 `build`。）

### 6.2 ⛔ 未做 —— 「未真机验证」明确声明

**本任务没有在真机上启动过游戏，因此「注入成功」这件事没有被验证过。**

`javac` **完全不校验** `@Mixin` 注解里的字符串：注解里的 target 描述符、方法名、
`method`/`at` 是纯运行期字符串，编译期一个字符都不会检查。所以
**`compileJava` 通过 ≠ 注入成功**，甚至不等于「Mixin 能应用」。

真机（或开发环境 `runClient`）启动后，必须在日志里确认：

| 关键字 | 含义 |
|---|---|
| `Mixin apply ... failed` | 本 mixin 应用失败 → 功能失效（`require = 0` 下通常只是没描边） |
| `InvalidInjectionException` | 注入点找不到/描述符对不上 |
| `Invalid name:` | target 描述符非法（冒号、拼错、成员不存在都在这里炸） |
| `Mixin apply for mod cyberware failed` | 整包 mixin 应用失败（会黑屏） |

以及一条**功能性**确认（编译期无论如何都测不出来）：

- 装上一件歧路司义眼后，看村民应见**黄**边、看同队玩家应见**绿**边、看僵尸应见**红**边；
- 卸下义眼后三种描边应全部消失（回归原版行为）。

在没有看到上述日志与实机画面之前，本交付只能算「**已编译、静态证据完备、待真机验证**」。
