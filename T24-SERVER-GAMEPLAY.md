# t24 服务端玩法交付（脑机超频 / RAM 消耗 / 五条快速破解 / AI 禁用）

> 依赖 t23 的契约（`RAM-DATA-CONTRACT.md`；该文档里写的 "t25" 就是本任务）。
> 范围：`core/**`、`network/**`、`event/**`（**未碰** `client/**`、`mixin/client/**`、`resources/**`）。
> 硬约束：未跑完整 build、未 commit、未写 Mixin（原因见 §2）。

---

## 0. 结论速览

| 项 | 状态 |
| --- | --- |
| P0-1 `RamSystem.canPay/spend` | ✅ 完成 |
| P0-2 超频接线（G 键上行 → 状态机裁决 → 下行 STATE，**被拒也广播**） | ✅ 完成 |
| P0-3 濒死超频（RAM 耗尽 + 超频开 → 扣 2 点生命，最低 2 HP，不致死） | ✅ 完成 |
| P0-4 瘫痪（RAM 耗尽 + 超频未开 → 拒绝 + 回包） | ✅ 完成 |
| P0-5 上传（服务端按刻计时，到时才生效，中途兜底取消） | ✅ 完成 |
| P0-6 五条破解效果（数值全占位） | ✅ 五条都在（过热 / 短路 / 突触熔断 / 武器故障 / 系统重置） |
| P1 AI 禁用 | ✅ **效果已实现**（无 Mixin：系统重置用原版 `setNoAi`，武器故障用事件拦投掷物） |
| P1 X 键扫描发光 | ❌ **未做**（原因见 §3） |
| 真机验证 | ❌ 未做（见 §4） |

---

## 1. P0 逐项（代码路径）

### P0-1 扣 RAM 入口 — `core/RamSystem.java`
```java
public static boolean canPay(ServerPlayer player, double cost)   // current >= cost
public static boolean spend(ServerPlayer player, double cost)    // 扣成功 → RamState.set + sync（HUD 不必等每秒结算）
```
任何 RAM 写入都只在服务端；客户端连扣费判定都不参与（沿用 t23 的口径）。

### P0-2 超频接线 — `network/CyberwareNetwork.java` + `core/OverclockSystem.java`
`G 键（huddev 的键位）→ OverclockPayload.toggleRequest() → CyberwareNetwork.handleOverclockToggle → OverclockSystem.toggle`
- 裁决：**没装网络接入仓** → 拒绝；**冷却中** → 拒绝；超频中再按 → 立刻结束进冷却；否则按稀有度表开超频。
- t24 补的一点：**被拒也 `sync(player)` 广播 STATE** —— 否则 HUD 按了没反应，看不到"冷却还剩多少"。
- 时长/冷却仍取 `CyberwareStats.overclockDurationTicks/CooldownTicks`（接入仓稀有度 → 占位表，TODO(主人填写)）。

### P0-3 濒死超频 — `core/HackSystem.pay`（可复现判定路径）
```java
public static Payment pay(ServerPlayer caster, int cost) {
    if (RamSystem.canPay(caster, cost)) { RamSystem.spend(caster, cost); return Payment.RAM; }   // ① 正常扣 RAM
    if (OverclockState.of(caster).isActive(OverclockSystem.serverTicks(caster))) {               // ② RAM 耗尽 + 超频开
        if (caster.getHealth() > DYING_HEALTH_FLOOR) {                                          //   只有血 > 2 才扣
            caster.setHealth(Math.max(DYING_HEALTH_FLOOR, caster.getHealth() - DYING_HEALTH_COST));
        }
        return Payment.HEALTH;                                                                  //   回包 note="dying_overclock"
    }
    return Payment.REJECTED;                                                                    // ③ RAM 耗尽 + 超频未开
}
```
- `DYING_HEALTH_COST = 2.0F`、`DYING_HEALTH_FLOOR = 2.0F`（= 1 颗心）都标了 TODO(主人填写)。
- **不会致死**：`setHealth` 的值恒 ≥ 2.0，且血 ≤ 2 时干脆不动（也不会把血加上去）。
- 三步都只改服务端数据，效果/回包各自独立（①③ 见 §1-P0-4）。

### P0-4 瘫痪 — `core/HackSystem.request`
`pay` 返回 `REJECTED` → `HackPayload.Action.REJECTED`（`note = "RAM ACCESS FAILED"`）+ 一条 debug 日志，**不改任何状态**。
其它拒绝原因同样是 REJECTED 包：`unknown_hack` / `uploading`（已有上传）/ `no_target`（目标无效）。

### P0-5 上传（服务端按刻计时）— `core/HackSystem.tick`（由 `RamSystem.onPlayerTick` 每刻驱动）
```
CAST → 校验目标 → 收费 → UPLOADS.put(每玩家最多一条) → 回 LOCKED + UPLOAD_START
每刻 tick：remaining-1 > 0 → 回 UPLOAD_PROGRESS（带剩余/总刻）
          remaining == 0 → 移除记录 → HackLibrary#apply（**此刻才生效**）→ 回 APPLIED
兜底：施法者死亡/换维度 → CANCELLED("caster_lost")；目标死亡/消失 → CANCELLED("target_lost")
```
- 上传刻数（占位，TODO(主人填写)）：过热 1.0s=20 刻 / 短路 1.5s=30 / 突触熔断 2.0s=40 / 武器故障 1.2s=24 / 系统重置 2.5s=50。
- 取消时**不退款**（TODO(主人填写)：要不要退由主人定，代码注释标了）。
- 静态表按 0.3.12 规则 7 处理：`clear()` + 服务器停止清空 + 玩家登出清理（见 §2）。

### P0-6 五条破解 — `core/HackLibrary.java`（枚举，`apply(caster, target)` 是唯一效果入口；数值全部 TODO 占位）
| 破解 | id | 服务端效果（占位数值） |
| --- | --- | --- |
| 过热 | `overheat` | `setRemainingFireTicks(60)` + 护甲 `-4`（属性修饰符 60 刻）+ FLAME 粒子 |
| 短路 | `short_circuit` | `hurtServer(indirectMagic(caster,caster), 8.0F)` + CRIT 粒子 |
| 突触熔断 | `synapse_burnout` | `SLOWNESS`/`WEAKNESS` 100 刻 等级 1 + CRIT 粒子 |
| 武器故障 | `weapon_glitch` | 标记 160 刻；期间该实体射出的投掷物被 `EntityJoinLevelEvent` 取消 |
| 系统重置 | `system_reset` | `setNoAi(true)` + `getNavigation().stop()` 60 刻，到期恢复 |
粒子用**原版** `ParticleTypes`（FLAME / CRIT），不自造贴图/音效（契约 §4.5/§4.6）。

---

## 2. AI 禁用：**没有写 Mixin**（含 javap 核实证据）

javap 核实（`build/moddev/artifacts/minecraft-patched-26.1.2.109-merged.jar`，与 captain §7.1 一致）：
```
public void      Mob.setNoAi(boolean)
public boolean   Mob.isNoAi()
public PathNavigation Mob.getNavigation()
public void      PathNavigation.stop()
protected final void Mob.serverAiStep()          ← 如需 Mixin 的目标方法（无参、final 只挡继承不挡 @Inject）
public void      Mob.aiStep()                    ← 两端都跑，**不建议**注入（会把客户端也停）
```
两条路线都不需要 Mixin，因此 **`mixin/**` 与 `cyberware.mixins.json` 本次未改**：
1. **系统重置** → 原版 `setNoAi(true)` + `getNavigation().stop()`，到期 `setNoAi(false)`（恢复天然平滑，满足邮件「防止 AI 突然卡死」）。
2. **武器故障** → `event/CombatEffectsHandler.onEntityJoinLevel` 拦「来源实体处于故障状态」的投掷物（`EntityJoinLevelEvent` 可取消）—— 不改 AI 状态，最干净。

**无永久卡死路径（四条防线）**：
1. `CombatEffects` 每条记录每刻检查（`EntityTickEvent.Post`，服务端）；
2. 实体被移除/死亡当刻就恢复；
3. **服务器停止**（`ServerStoppedEvent`）→ `restoreAll()` 把 `noAi` / 负护甲**全部还原**（不会把 `noAi=true` 写进存档）；
4. 提供 `clear()`；静态表只存活 ≤ 8 秒（每条记录强引用它作用的实体，生命周期有界，不是泄漏）。
⚠️ 已知残留风险：**硬崩溃**（不走 `ServerStoppedEvent`）时，若恰好有实体处于系统重置/过热状态，
存档里可能留下 `noAi=true` 或负护甲。彻底解法是给实体存一个持久标记（`AttachmentType`），
但那需要动 `registry/**`（本任务范围外）—— 留给后续任务（见 §3.3）。

---

## 3. 未做（P1 与已知缺口）

1. **X 键歧路司扫描发光：未做。** 原因：现有三个包里没有「扫描」语义，需要一个 C2S 扫描请求
   （新 action 或新包）+ huddev 的客户端按键，属联合改动，时间不够。
   实现路径（很便宜）：`target.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks))`
   —— `MobEffects.GLOWING` 已 javap 核实存在、原版自带到期，服务端三行即可；协议侧建议在
   `HackPayload.Action` 里加一个 `SCAN`（或由 huddev 用 `CAST` + `hackId="scan"` 约定），下一个任务定。
2. **没有 Mixin**（见 §2）：不是缺项，而是这条需求不需要（captain 给的口径也是优先不用）。
   若后续有人判断必须注入，目标是 `Mob.serverAiStep()`，写法 `require = 0`。
3. **硬崩溃残留**（见 §2 末）：需要 `registry/**` 的持久标记才能彻底解决，本任务范围外。
4. **上传取消是否退款**：当前不退，标 TODO 等主人定。
5. **全部玩法数值是占位值**（上传秒数、短路伤害、护甲减益、点燃时长、缓慢/虚弱时长、武器故障时长、系统重置时长）。

---

## 4. 未真机验证（**本节都是代码里验不出来的**）

本环境跑不起 Minecraft，验证只到编译级 + 字节码级（§5）。进游戏需要确认：

1. **G 键超频**：装了网络接入仓能开；没装/冷却中会被拒**且 HUD 能看到状态**（被拒也广播了 STATE）；
2. **扣 RAM 与 HUD**：释放破解后 RAM 立刻下降（`spend` 立即 sync），恢复速率符合 Σ`RAM_REGEN`；
3. **濒死超频**：RAM 耗尽 + 超频开时改扣 2 点生命，**血到 2 点就不再扣、不会死**；
4. **瘫痪**：RAM 耗尽 + 超频未开时拒绝，HUD 收到 `RAM ACCESS FAILED`；
5. **五条破解**：上传条按刻推进、到点才出效果；数值手感（全是占位值，主人要调）；
6. **系统重置到期 AI 恢复**（重点）：到期后怪物立刻恢复行动，**反复触发也不累积/不永久卡死**；
7. **武器故障**：期间该怪物不再射出投掷物（箭/火球），到期恢复；
8. **上传兜底**：上传途中打死目标 / 自己死亡 / 换维度 → 收到 CANCELLED，没有幽灵上传；
9. **服务器停止后无残留**：触发系统重置后立刻停服再进游戏，怪物 AI 正常（这条验 §2 第 3 条防线）。

---

## 5. 验证方式与当前编译状态

### 5.1 命令
```bash
# 工作区整体编译（契约里的 Verify 命令）
cd /root/mod26/cyberware && JAVA_HOME=/usr/lib/jvm/java-21-openjdk-arm64 \
  /opt/gradle-8.10.2/bin/gradle compileJava --no-daemon
# 范围核对（应为 0）
cd /root/mod26/cyberware && git status --short -- src/main/java/com/dsh/cyberware/client | wc -l
```

### 5.2 结果（含一个外部阻塞与最终状态）
- **隔离编译（我的代码）：BUILD SUCCESSFUL**（1m54s，副本 = 工作区整树 + `client/**`/`mixin/client/**` 回退到 HEAD）。
  产物级核对：`RamSystem.canPay/spend`、`HackSystem.pay/request/tick/cancelFor/clear`、
  `CombatEffects.armorDebuff/weaponGlitch/isWeaponGlitched/disableAi/restoreAll/clear` 都在；
  `CyberwareNetwork` 里有 4 处 `IEventBus.addListener`；`handleHackCast` 字节码调用 `HackSystem.request(ServerPlayer,int,String)`。
- 第一次工作区整体编译**失败**，8 个 error **全部**在 `client/RamHud.java`（huddev 并行开发中的新文件，
  范围外）—— 我未改该文件。
- 随后工作区 `compileJava --no-daemon` → **BUILD SUCCESSFUL**（huddev 修好后任务为 UP-TO-DATE，
  即当前输入集已有一次成功编译）。
- `git status --short -- src/main/java/com/dsh/cyberware/client | wc -l` → **5**：这 5 条**全部是 huddev 的
  并行改动**（`RamHud.java` / `RamClientState.java` / `OverclockWireframe.java` / `CyberwareClient.java` /
  `CyberwareKeys.java`），与 `RAM-SYSTEM-SPEC.md` §5 的归属表一致（`client/**` 归 huddev）。
  我的改动清单里**没有任何 `client/**` 路径**（见任务记录里的 changedPaths 与补充说明）。
- `mixin/**` 与 `cyberware.mixins.json` 未改（本任务不需要 Mixin，见 §2）；`resources/assets` 未改。
