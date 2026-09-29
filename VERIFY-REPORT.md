# VERIFY-REPORT.md —— t6 全量构建 + Mixin 静态自查 + 资源完整性 + 禁编造核验

执行人：inspector　任务：t6 [verify]　attempt_id：`2bbc20f2-15ce-492d-9e9d-28b3e923dee9`
工作区：`/root/mod26/cyberware`　基线 commit：`b0aabee`（工作树未提交，改动以 `git status` 为准）
校验方式：只读。本任务**未修改任何他人负责的源码/资源**，只新增本报告与 /tmp 证据文件。

---

## 0. 结论

| 项 | 结论 |
|---|---|
| 全量构建 | ✅ **PASS**（3 种口径全部 exit 0，含强制全量重编译） |
| Mixin 静态自查 | ✅ **PASS**（新增注入点描述符逐字符核对通过；发现 2 项**既有**加固项，见 §11，不阻断本次） |
| 定义表规模 | ✅ PASS（135 / 19 / 154 / 154 全中） |
| 资源完整性 + 32x32 | ✅ PASS（缺口 0；135/135 物品贴图 32x32） |
| lang 覆盖 | ✅ PASS（zh_cn 135/135；en_us 49 条、无新增） |
| 删除干净度 | ✅ PASS（12 id × 3 文件已删，lang 已清，零残留） |
| 19 条注释对齐 | ✅ PASS（集合与 ASSET-MISSING.txt 完全相等） |
| 12 件新增 TODO 合规 | ✅ PASS（12/12 有 TODO，无未标注来源数值） |
| 客户端接线（t9） | ✅ PASS（2 处 sendToServer 实存；按钮映射一致） |
| 禁编造 | ✅ PASS（抽检 2 条定义与 描述.txt 逐字一致；报告无模糊措辞；见 §11 的文档小错） |
| **总裁决** | **PASS** —— 0 个阻断项；§11 列出 5 条非阻断发现（2 中 3 低） |

> ### ⚠ **编译通过不等于注入成功。**
> 本报告的全部 Mixin 结论只证明「字节码层面描述符正确、注解正确、类已进 jar」。
> 注入是否真的命中、描边是否真的画出来，**必须由真机日志与实机画面确认**（见 §12）。

---

## 1. 全量构建

命令（三种口径，同一工作树、期间未改任何文件）：

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-arm64
export PATH=$JAVA_HOME/bin:/opt/gradle-8.10.2/bin:$PATH
gradle build --no-daemon                                     # 任务书指定口径
gradle clean build --no-daemon                               # 清 build 后再全量
gradle build --no-daemon --rerun-tasks --no-build-cache      # 强制真编译，排除缓存假绿
```

| # | 命令 | 耗时 | 退出码 | compileJava | 日志尾部 |
|---|---|---|---|---|---|
| 1 | `gradle build --no-daemon` | 1m 44s | **0** | `UP-TO-DATE` | `BUILD SUCCESSFUL in 1m 44s / 4 actionable tasks: 2 executed, 2 up-to-date` |
| 2 | `gradle clean build --no-daemon` | 2m 6s | **0** | `FROM-CACHE` | `BUILD SUCCESSFUL in 2m 6s / 5 actionable tasks: 4 executed, 1 from cache` |
| 3 | `gradle build --no-daemon --rerun-tasks --no-build-cache` | 3m 14s | **0** | **真实执行** | `BUILD SUCCESSFUL in 3m 14s / 4 actionable tasks: 4 executed` |

口径 1、2 分别命中 Gradle 的 up-to-date/build-cache（compileJava 没真跑），所以补跑口径 3：
`--rerun-tasks --no-build-cache` 下 `> Task :compileJava` 真执行（4 executed），**0 error**，
仅 2 条无害 `Note: ... deprecated API`。三种口径均 exit 0，日志里没有任何 `error:` / `FAILED`。
日志：`/tmp/t6-build.log`、`/tmp/t6-cleanbuild.log`、`/tmp/t6-forcebuild.log`。

产物核验（python zipfile 读，工作区无 unzip）：

```
build/libs/cyberware-0.3.12-Beta.jar   655393 字节
entries 548 / .class 69
com/dsh/cyberware/mixin/client/EntityRendererOutlineMixin.class   在包内
cyberware.mixins.json                                            在包内（client 数组 11 项）
META-INF/neoforge.mods.toml: version = "0.3.12-Beta"
包内 lang：zh_cn 165 条目（item 135） / en_us 49 条目
```

**结论：PASS。** jar 可交付真机（见 §12）。

---

## 2. Mixin 静态自查（黑屏的唯一防线）

### 2.1 冒号陷阱（javap 冒号写法 → InvalidMemberDescriptorException → 整个 mixin FAILED）

扫描口径：`grep -rn -E '"[A-Za-z_$][A-Za-z0-9_$]*:' src/main/java/com/dsh/cyberware/mixin/`

```
=== colon check: method name followed by colon ===
NO COLON-PATTERN MATCHES
```

mixin 目录全部 `method=` / `target=` 字面量共 19 处**逐条人工核对**：方法名与左括号之间**无冒号**。
新增的 `EntityRendererOutlineMixin.java:113`：

```
method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V",
```

`extractRenderState` 与 `(` **直接相邻**；`@At` 的 `target=` 形如
`Lnet/minecraft/...;方法名(参数)返回` —— 冒号只出现在描述符开头的 `L<类>;` 里，方法名后无冒号。

### 2.2 cyberware.mixins.json 逐类存在性（含 opticsdev 新增项）

json 的 `client` 数组 11 项，磁盘 `src/main/java/com/dsh/cyberware/mixin/client/` 有 12 个类：

| json 列出的类 | 磁盘存在 |
|---|---|
| client.PostChainAccessor / PostPassAccessor / GuiMixin / SoundEngineMixin / ParticleMixin / WeatherEffectRendererMixin / GameRendererMixin / LivingEntityRendererAccessor / EquipmentLayerRendererMixin / CapeLayerMixin | ✅ 全部存在 |
| **client.EntityRendererOutlineMixin**（t5 新增） | ✅ `EntityRendererOutlineMixin.java` 存在，且已进 jar |

**反向检查（json 未列、但磁盘存在的类）**：`client.ProjectileMixin` 存在于磁盘，**从未登记进
mixins.json**（`git log -p --all -- cyberware.mixins.json | grep Projectile` 无命中；`git show HEAD:...json`
也没有）→ 该项自 0.3.6 起就是**未生效的死代码**，功能已由 `CyberwareClient.java:43` 的
`ClientProjectileDilation` 事件监听取代（见 §11 M1）。

### 2.3 单点注入的 `require = 0`

`injectors.defaultRequire = 1`（json 明文）。逐注入点统计：

| 文件 | 注入 | 显式 require=0 |
|---|---|---|
| **EntityRendererOutlineMixin**（本次新增） | `@Inject` ×1 | ✅ `require = 0` |
| CapeLayerMixin（既有） | `@Redirect` ×1 | ✅ |
| EquipmentLayerRendererMixin（既有） | `@Redirect` ×3 + `@ModifyArg` ×1 | ✅ 4/4 |
| GuiMixin / SoundEngineMixin / ParticleMixin / ProjectileMixin / GameRendererMixin（既有） | `@Inject` 各 ×1 | ❌ 未写 |
| WeatherEffectRendererMixin（既有） | `@ModifyVariable` ×2 | ❌ 未写 |

即**本次新增注入点已按要求显式 `require = 0`**；6 个**既有**文件共 7 处注入未写 → §11 M2。

### 2.4 client 侧回调整体 try/catch

- `EntityRendererOutlineMixin.cyberware$applyFriendFoeOutline`：**整段 `try { … } catch (Throwable t)`**
  （L122–L152），异常只记一次日志（`cyberware$outlineErrorLogged`）→ ✅ 符合要求。
- 既有 client mixin 回调体内无 try/catch（部分把 try/catch 放在被调用的 client 类里，如
  `AfterimageHistory`/`AfterimageRenderer`）。属既有风格，本次未扩范围。

### 2.5 目标描述符 javap 逐个复核（merged jar `minecraft-patched-26.1.2.109-merged.jar`）

| mixin 里写的 target | javap -p -s 实测 | 一致 |
|---|---|---|
| `EntityRenderer.extractRenderState`<br>`(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V` | `public void extractRenderState(T,S,float);`<br>`descriptor: (L…Entity;L…EntityRenderState;F)V` | ✅ 逐字符 |
| `EntityRenderer.submit`（未注入，旁证） | `(L…EntityRenderState;L…PoseStack;L…SubmitNodeCollector;L…CameraRenderState;)V` | — |
| `EquipmentLayerRenderer.renderLayers`（带 Identifier 的重载） | `(…Model;Ljava/lang/Object;…ItemStack;…PoseStack;…SubmitNodeCollector;IL…Identifier;II)V` | ✅ |
| `CapeLayer.submit` | `(Lcom/mojang/blaze3d/vertex/PoseStack;L…SubmitNodeCollector;IL…AvatarRenderState;FF)V` | ✅ |
| `GameRenderer.render` | `(Lnet/minecraft/client/DeltaTracker;Z)V` | ✅ |
| `Gui.extractPlayerHealth` | `(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V` | ✅ |
| `Particle.tick` / `Projectile.tick` | `()V` | ✅ |
| `SoundEngine.calculatePitch` | `(Lnet/minecraft/client/resources/sounds/SoundInstance;)F` | ✅ |
| `WeatherEffectRenderer.extractRenderState` | `(Lnet/minecraft/world/level/Level;IFLnet/minecraft/world/phys/Vec3;…)V` | ✅ |
| `LivingEntityRenderer.setupRotations` / `scale` / `layers` | `(…LivingEntityRenderState;…PoseStack;FF)V` / `(…;…)V` / `List` 字段 | ✅ |
| `PostChain.passes` / `PostPass.customUniforms` | `List` / `Map` 字段 | ✅ |
| `@At target: LevelRenderer.doEntityOutline` | `public void doEntityOutline(); desc: ()V` | ✅ |
| `@At target: RenderTypes.armorCutoutNoCull(Identifier)RenderType` | `public static …RenderType armorCutoutNoCull(Identifier);` ✅ | ✅ |
| `@At target: Sheets.armorTrimsSheet(Z)RenderType` | `public static …RenderType armorTrimsSheet(boolean);` ✅ | ✅ |
| `@At target: OrderedSubmitNodeCollector.submitModel(…CrumblingOverlay;)V` | 抽象方法，描述符逐字符一致 ✅ | ✅ |
| `@At target: SubmitNodeCollector.submitModel(…)V`（CapeLayerMixin） | 接口**继承**自 `OrderedSubmitNodeCollector`；`javap -c CapeLayer` 确认调用点正是 `invokeinterface net/minecraft/client/renderer/SubmitNodeCollector.submitModel:(…)V` ✅ | ✅ |
| `@At target: IClientItemExtensions.getArmorLayerTintColor(…)I` | 类在 **NeoForge jar**（`neoforge-26.1.2.109-universal.jar`）不在 merged jar；0.3.9 起已发布且 0.3.11 真机日志无注入报错（§12 证据）| ✅（间接） |

**产物级复核**（读 **jar 里**的 class，不是源码）：

```
$ javap -v -p -cp build/libs/cyberware-0.3.12-Beta.jar com.dsh.cyberware.mixin.client.EntityRendererOutlineMixin
  #144 = Utf8  extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V
  #148 = Utf8  TAIL
  #149 = Utf8  require
  #150 = Integer 0
  RuntimeVisibleAnnotations:
      org.spongepowered.asm.mixin.injection.Inject(
        method=["extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V"]
        at=[@…At(value="TAIL")]
        require=0
      )
```

注入点字符串、`TAIL`、`require=0` 三者都在编译产物里 —— 与 merged jar javap 输出**逐字符相同**。

**结论：PASS（新增注入点静态证据完备）。** 再次强调：**编译通过不等于注入成功。**

---

## 3. 定义表规模核验（`core/CyberwareDefinitions.java`，1228 行）

判定口径：`^\s*public static final CyberwareDefinition X = register\(new CyberwareDefinition\($`（生效）+ 同式加 `//` 前缀（注释）。

| 指标 | 期望 | 实测 | 结论 |
|---|---|---|---|
| 生效 register | 135（123 白名单 + 12 新增） | **135** | ✅ |
| 注释的无素材定义 | 19 | **19** | ✅ |
| `new CyberwareDefinition(` 合计 | 154 | **154** | ✅ |
| `cyberware:item/` 出现次数 | 154（142 + 12） | **154** | ✅ |
| 生效 id 是否唯一 | — | 135 条，集合 == 135（无重复） | ✅ |

集合关系（脚本比对，见 `/tmp/t6_verify.py`）：

```
生效 == 白名单∪新增12 ?  True        （生效−白名单−新增12 = []；白名单+新增12−生效 = []）
注释 == ASSET-MISSING ?   True        （两向差集均为 []）
白名单∩注释（有素材却被误注释）= []   （0 条误注释）
白名单∩生效 = 123
12 件新增全部在生效集合内
```

---

## 4. 资源完整性

工作树 `assets/cyberware/`：

| 目录 | 实测 | 构成 |
|---|---|---|
| `textures/item/*.png` | **146** | 135 物品贴图 + 10 `slot_*.png` + 1 `cyberware_chip.png` |
| `models/item/*.json` | **137** | 135 物品 + `cyberware_chip.json` + `cyberware_station.json` |
| `items/*.json` | **136** | 135 物品 + `cyberware_station.json` |

> 任务书写的「11 张 slot_*.png」与实测差 1：实际是 **10 张 `slot_*.png`**，
> 第 11 个特殊贴图是 `cyberware_chip.png`（同为 16x16 合法）。按「除 chip 与 slot_* 外全部必须 32x32」判定。

**缺口清单：0 条。** 逐个生效 id 检查三件套：

```
生效定义缺贴图: []          生效定义缺 model json: []
生效定义缺 items json: []   贴图 id 集合 == 生效定义集合 ? True
贴图多余（不在生效定义）: []  非物品 model/items json 仅 cyberware_chip / cyberware_station（合法）
```

**json 可解析性**：`src/main/resources/**/*.json` 共 **279** 个，`json.load` 失败 **0** 个。

### 32x32 规格核验（主人硬要求）

PNG IHDR 逐字节读宽高：

```
[32x32] 物品贴图 32x32 数: 135 / 135      ← 全部合格
[32x32] 物品贴图非 32x32 (blocker 项): []
[32x32] 特殊贴图尺寸: cyberware_chip 16x16 + 10 张 slot_* 全部 16x16   ← 合法例外
```

**高清图漏网：0 张。无 blocker。**（t8 把原图 160~170 压成 32x32，实测 12 张新图与全部旧图都已是 32x32。）

---

## 5. lang 覆盖

| 文件 | 期望 | 实测 | 结论 |
|---|---|---|---|
| `zh_cn.json` | item 键覆盖 135 生效定义 | 总 165 条目，`item.cyberware.*` **135**，缺 0 / 多 0 | ✅ |
| `en_us.json` | 保持 49 条、无新增 | 总 **49** 条目 / item 19 条，md5 `db559925b90bf73e22eda3b17053136b` | ✅ |

`en_us` 是否「被改动」需按语义判：与基线 `HEAD` 相比它**确有 diff，但只有删除、没有任何新增** ——

```
HEAD en_us：61 条目 / 31 个 item 键；工作树：49 / 19
git diff HEAD -- lang/en_us.json  =  删除且仅删除这 12 个废弃 id 的 item 键
  sandevistan_zetatech / _dynalar / _qiantai / _militech_falcon / _militech_apogee
  cyberdeck_militech_parallel / cyberdeck_biotech_3 / cyberdeck_militech_canto_6
  berserk_moore / berserk_biodyne / berserk_militech / berserk_zetatech
```

即：**没有为任何新定义新增英文条目**（主人裁决「只补中文」被遵守），
49 条正是 t2 清理 12 条废弃键之后的应有状态，与 t8 报告声明的 md5 一致 → **不构成 blocker**。
`zh_cn` 的 diff 为 **+117 / −13**：13 删 = 12 废弃 item 键 + `cyberware.ui.unknown`（同键在同文件另一位置重新出现，**未丢键**），
117 增 = 116 item 键 + `cyberware.ui.unknown`；净增 104 个 item 键 = 31−12+116 = 135。✅

---

## 6. 删除干净度（12 个废弃 id）

`git status --short` 删除项共 **36** 个 = **12 id × 3 文件**（textures/item + models/item + items），无多删：

```
 D .../items/<12 id>.json          D .../models/item/<12 id>.json
 D .../textures/item/<12 id>.png
```

12 个 id 逐条残留扫描（定义表 / 贴图 / model json / items json / zh_cn / en_us 六处）：

```
sandevistan_zetatech / sandevistan_dynalar / sandevistan_qiantai /
sandevistan_militech_falcon / sandevistan_militech_apogee / berserk_moore /
berserk_biodyne / berserk_zetatech / berserk_militech / cyberdeck_militech_parallel /
cyberdeck_biotech_3 / cyberdeck_militech_canto_6   →  12/12 「干净」
```

- `lang` 两个文件的这 12 个键均已删除（zh_cn −12、en_us −12，见 §5）。
- `berserk_militech.png`：文件已不存在（`find /root/mod26 -name 'berserk_militech*'` 输出为空）；
  全仓字面量引用只剩 `INSTALL-WIRING.md:148` 的**历史说明文字**（「贴图 `berserk_militech.png` → `berserk_c4.png`」），
  **源码/资源 0 处引用**。
- `CyberwareDefinitions.SANDEVISTAN_ZETATECH`：源码 0 处代码引用，仅剩 2 处**注释**：
  `client/CyberwareStationScreen.java:97`（javadoc 说明已改名，实际用的是 `CyberwareDefinitions.SANDEVISTAN_C4`）与
  `CyberwareCommands.java:35`（同上，实际用的是 `…SANDEVISTAN_C4`）。
- 备份走 trash 未直接 rm：`/sdcard/DSH/trash/cyberware-removed/{textures-item,models-item,items}/` 存在 ✅

**结论：PASS。**

---

## 7. 「没素材的道具先不做」/ 19 条注释对齐

- `ASSET-MISSING.txt` 19 条（正则 `^\d+\.[ ]+<id>` 计数 = 19）。
- 定义表 19 个注释 register 的 id 集合与 MISSING 集合**完全相等**（两向差集皆空）。
- 反向：白名单 123 条**没有一条**被误注释（`白名单∩注释 = ∅`，`白名单∩生效 = 123`）。
- 义眼白名单状态复核（t10 的方案 A）：6 个 id 在生效集合，`kiroshi_optics_piercing` /
  `kiroshi_optics_sensor` 在注释集合（ASSET-MISSING #6、#7）—— 与代码 javadoc 声明一致。

**结论：PASS。**

---

## 8. 12 件新增义体专项核验（无官方来源 → 必须老实标 TODO）

`12-NEW-DEFS.md §2` 定稿清单 = 定义表实际注册 id = 仓库资源文件名，**12/12 三方逐字一致**
（脚本比对：`doc == java 12 ? True`，`doc == 资源名 ? True`）。

逐条查定义块（定义表 L1106–L1199，每条上方都有 `// TODO(数据待确认)：…`）：

| id | 行号 | TODO 注释行 | 描述串含 TODO | stats() | 槽位/稀有度/容量标注 |
|---|---|---|---|---|---|
| metabolic_editor | 1106 | ✅ | ✅ `代谢编辑器。TODO(数值与描述待补)` | 空 | ✅ 「槽位/稀有度/容量无上游来源…暂取…LEGENDARY + 占位容量 8」 |
| full_resistance | 1114 | ✅ | ✅ | 空 | ✅ |
| reverse_inductor | 1122 | ✅ | ✅ | 空 | ✅ |
| synthetic_lung | 1130 | ✅ | ✅ | 空 | ✅ |
| grounding_plating | 1138 | ✅ | ✅ | 空 | ✅ |
| thermal_converter | 1146 | ✅ | ✅ | 空 | ✅ |
| bioplastic_vessels | 1154 | ✅ | ✅ | 空 | ✅ |
| dermal_weave | 1162 | ✅ | ✅ | 空 | ✅ |
| nano_relay | 1170 | ✅ | ✅ | 空 | ✅ |
| detoxifier | 1178 | ✅ | ✅ | 空 | ✅ |
| edge_enhancement_system | 1186 | ✅ | ✅ | 空 | ✅ |
| fireproof_coating | 1194 | ✅ | ✅ | 空 | ✅ |

**未标注来源的数值：0 个。** 槽位/稀有度(LEGENDARY)/容量(8) 全部由 TODO 行覆盖并给出「占位、
待主人确认」口径；`stats()` 为空（没有编造任何效果数值）；中文名出处（素材文件名）与 id 出处
（§2 生成规则）在报告 §3 主表逐格标注。唯一小瑕疵：`active=true` 这一格未在 TODO 行里单独点名（§11 M5）。

**结论：PASS。**

---

## 9. 客户端接线核验（t9）

```
=== sendToServer ===
src/main/java/com/dsh/cyberware/client/CyberwareStationScreen.java:325  ClientPacketDistributor.sendToServer(
src/main/java/com/dsh/cyberware/client/CyberwareStationScreen.java:341  ClientPacketDistributor.sendToServer(
```

- 2 处均实打实构造 payload：`:325-326 CyberwareActionPayload.install(this.menu.containerId, slot)`、
  `:341-342 CyberwareActionPayload.uninstall(this.menu.containerId, def.id())`（t9 之前全仓 0 处，现已 ≥1）✅
- `INSTALL-WIRING.md:158`：`1. ~~客户端触发点仍然不存在~~ → **已完成（t9）**，见下。` —— 更正已落地 ✅
- 按钮 id 映射一致性：
  `CyberwareStationMenu`：`BUTTON_UPGRADE = 0`、`BUTTON_INSTALL_BASE = 1`、`IMPLANT_SLOT_COUNT = 10`、
  `BUTTON_UNINSTALL_BASE = 11`、`MAX_BUTTON_UNINSTALL = 10` → 实区间 **0=升级 / 1..10=装 0..9 / 11..20=卸 0..9** ✅
  `clickMenuButton` 用同两个区间判定并二次挡 `ServerPlayer`；卸载走 `orderedIds()` 确定性下标。
- 说明（不构成 blocker）：`CyberwareStationScreen` 装卸走 **payload 通道**（install(containerId,slot) /
  uninstall(containerId,defId)），不直接发 int 按钮 id；服务端两条入口并存（`uninstall(…, defId, fallbackIndex)`）。
  升级仍是「无按钮、回执未接线」，与 t9 报告一致，属既定范围外。

**结论：PASS。**

---

## 10. 禁编造核验

1. **模糊措辞扫描**：6 份报告全文 `grep "应该是|大概是|估计是|我觉得|可能是"` → **0 命中**。
2. **抽样反查上游原文**（不是看报告自证）：
   - `blood_pump`（活血泵）：`描述.txt:294` `活血泵  ≥2阶  15  相当于效果强大的生命值物品。` +
     续行 `立即恢复45~110生命值，然后每秒恢复9~23生命值，持续6秒。 技术调幅：…`；
     定义表 L719-723：容量 `15` ✅、`Stats.DURATION, 6` ✅、描述串=原文 ✅
   - `second_heart`（副心脏）：`描述.txt:305` `副心脏  ≥4阶  30  生命值为0时，+100%生命值。…` +
     续行 `冷却时间：300~200秒。 肉体调幅：…`；定义表 L691-695：容量 `30` ✅、
     `Stats.COOLDOWN, 200`（区间取最高档，规则见 DEFS-VALUES §0.2）✅、描述串=原文 ✅
3. **报告出处密度**：TEXTURE-MAPPING / DEFS-VALUES / 12-NEW-DEFS 的每一行都带
   `定义表L<n>` / `ID对照L<n>` / `描述L<n>` / `待完整名称L<n>` / `javap 输出` / `md5` 之一；
   INSTALL-WIRING / OPTICS-MIXIN 的字节码结论均附 `javap` 原始片段。
4. **上游文件真实存在**：`/sdcard/DSH/义体/{ID对照.txt,待插入-123条定义.txt,描述.txt}`、
   `/sdcard/DSH/斯安威斯坦/完整名称.txt` 均存在（`ls -la` 已核）。
5. **未找到无出处的数字** → 无 blocker。

**结论：PASS。**（3 条文档层面的小问题见 §11 M3/M4。）

---

## 11. 非阻断发现（不建议因此 FAIL，但应记录/排期）

| # | 级别 | 发现 | 证据 | 建议归属 |
|---|---|---|---|---|
| **M1** | medium | `mixin/client/ProjectileMixin.java` **从未登记进 `cyberware.mixins.json`**（自 0.3.6 起），是死代码；其功能已被 `CyberwareClient.java:43` 注册的 `ClientProjectileDilation` 事件监听取代 | json 11 项不含它；`git show HEAD:cyberware.mixins.json` 也没有；`grep ProjectileMixin` 除自身外 0 引用 | 清理类任务（谁碰 mixin 谁删） |
| **M2** | medium | **7 处既有注入未显式 `require = 0`**（GuiMixin、SoundEngineMixin、ParticleMixin、ProjectileMixin、GameRendererMixin 各 1，WeatherEffectRendererMixin 2），叠加 `defaultRequire = 1`：一旦匹配失败会 `Mixin apply failed` → 黑屏。本次**新增**注入点已合规 | §2.3；缓解证据：0.3.11 真机 `latest.log` 无任何 `InvalidInjectionException` / `Mixin apply failed`（§12），且全部目标方法已 javap 确认存在 | 下次发布前加固（低风险，不阻断） |
| **M3** | low | `TEXTURE-MAPPING.md:9` 与 `:200` 的算术错误：`81+9+11=102` 实为 **101**；实测 104，差值是 **3** 不是 2 | `ls` 实测 83 png + 1 webp + 9 + 11 = 104 | t2/文档维护 |
| **M4** | low | 快照数字未随 t7 更新：`ASSET-WHITELIST.txt` / `ASSET-MISSING.txt` 头部仍写「当前定义表 142 条」，`TEXTURE-MAPPING.md` 附录 A 仍写「入库贴图 123」——id 清单本身正确，仅计数陈述过期 | §3 实测 154 总 / 135 生效 | 文档维护 |
| **M5** | low | 12 条新增定义的 `active=true` 未在 TODO 行里单独点名（其余字段都点名了） | 定义表 L1106-1199 的 `// TODO(数据待确认)：槽位/稀有度/容量…` 未含 active | numbersmith（一行注释即可） |

---

## 12. 真机验证清单（交给主人；本任务到此为止，未进游戏）

> ⚠ 上表 M2 的缓解证据只能说明「0.3.11 的既有 mixin 在真机上应用成功」。
> **0.3.12 新增的 `EntityRendererOutlineMixin` 从未在真机启动过** ——
> **编译通过不等于注入成功**，下面每一步都要靠日志与画面对答案。

### 12.1 部署 jar（不需要 ADB，文件操作走设备 shell）

```bash
# 1) 先备份现有 mods 里的旧 jar 到 trash（不要直接 rm）
mkdir -p /sdcard/DSH/trash/cyberware
mv /sdcard/FCL/.minecraft/versions/26.1.2-NeoForge/mods/cyberware-0.3.11-Beta-Hotfix-10.jar \
   /sdcard/DSH/trash/cyberware/

# 2) 把新 jar 放进 mods（当前 mods 目录里只有上面这一个旧 cyberware jar）
cp /root/mod26/cyberware/build/libs/cyberware-0.3.12-Beta.jar \
   /sdcard/FCL/.minecraft/versions/26.1.2-NeoForge/mods/

# 3) 核对
ls -la /sdcard/FCL/.minecraft/versions/26.1.2-NeoForge/mods/cyberware-*.jar
#   期望只剩 cyberware-0.3.12-Beta.jar（655393 字节）
```

### 12.2 启动后读日志

```
文件：/sdcard/FCL/.minecraft/versions/26.1.2-NeoForge/logs/latest.log
```

按顺序搜这些关键字（**任意一个命中都是阻断级，需回报**）：

| 关键字 | 含义 |
|---|---|
| `Mixin apply` + `failed` | 某 mixin 整体应用失败 |
| `InvalidInjectionException` | 注入点匹配/描述符出错 |
| `Invalid name:` / `InvalidMemberDescriptorException` | 描述符写法错（冒号陷阱） |
| `Mixin apply for mod cyberware failed` | 本模组的 mixin 配置级失败 |
| `Scanned 0 target(s)` / `Cannot find method` | 目标方法没匹配到（`require=0` 时会静默跳过 → 功能隐形失效） |

另外确认加载的是新版本：

```
grep -n "cyberware-" latest.log        # 应出现 cyberware-0.3.12-Beta.jar
grep -n "Cyberware 义体系统" latest.log # 应出现 0.3.12-Beta
```

### 12.3 进游戏后要验的事

1. **安装/卸载是否生效**：拿一件义体放快捷栏 → 开操作台（面板底部快捷键那排）→ 点快捷栏格拿起 →
   点底部义体槽放下 → 选中它 → 点「安装」→ 标题栏 `容量 已用/上限` 应变化，义体进玩家义体表；
   点「卸载」应还原成物品回到操作台槽/背包/掉脚边。
2. **容量上限**：一直装到超上限，应被拒绝并给出中文原因（默认上限 100，见配置 `capacity.defaultCapacity`；
   `enableCapacityLimit=false` 时无限）。
3. **义眼描边颜色**：装上任一歧路司义眼（`kiroshi_optics_bare` / `kiroshi_optics` / `_combined` /
   `_hunter` / `_wallhack` / `iconic_advanced_kiroshi_optics_bare`）后看生物：
   - 同队玩家 / 自家宠物 → **绿** `0xFF34D058`
   - 村民、动物等中立 → **黄** `0xFFFFCC00`
   - 僵尸骷髅等 `Enemy`、或正把你当目标的中立怪 → **红** `0xFFFF3B30`
   - **卸下义眼后描边应消失**（否则说明 `has()` 查询或同步有问题）
4. **不要测这两个**：`kiroshi_optics_piercing` / `kiroshi_optics_sensor` 当前定义被注释、游戏内不存在，
   装了也没法测（等素材补齐恢复注册）。

真机结论回填：命中 → 记录 `latest.log` 里 cyberware 的注入行 + 一张描边截图；未命中 → 把
`Mixin apply … failed` 原文发给 captain。

---

## 13. 本任务证据文件

| 文件 | 内容 |
|---|---|
| `/tmp/t6-build.log` | `gradle build` 原始日志（exit 0） |
| `/tmp/t6-cleanbuild.log` | `gradle clean build` 原始日志（exit 0） |
| `/tmp/t6-forcebuild.log` | `gradle build --rerun-tasks --no-build-cache` 原始日志（exit 0，真编译） |
| `/tmp/t6_verify.py` / `/tmp/t6-verify-out.txt` | 定义表/资源/lang/删除干净度/32x32 全量核验脚本与输出 |
| `/tmp/t6_eff.txt` / `/tmp/t6_cmt.txt` | 135 条生效 / 19 条注释 id + 定义表行号 |
