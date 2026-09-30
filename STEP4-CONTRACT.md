# 0.5.1-Beta 修复与调型契约（主人 2026-10-01 反馈）

上游：主人真机反馈（**脑机超频 BUG / 冷却 BUG / 生命抵不了 RAM / RAM 回太慢**）
+ 四条改动指令 + 四个裁决答案。

---

## 1. 两个已定位的 BUG（必须修，根因已写在这里，别再自己猜）

### 1.1 超频"一闪而过" → 连带产生"冷却 BUG"
`core/OverclockSystem.java:83-96` 的 `tick()`：
```java
OverclockState state = read(player, now);
if (!state.isActive(now)) return false;          // ← 只在"没在超频"时返回
OverclockState.set(player, state.endAt(...));    // ← 只要在超频中就立刻结束
```
`isActive(now)` 的含义是 `active && now < expiresAt` —— 所以**开启后的第一个 tick 就自己结束**，
紧接着 `endAt()` 又把它推进冷却 ⇒ 玩家看到"超频一闪"+"按了没反应（其实在冷却）"。

**正确逻辑**：只有**到点**才结束：
```java
if (!state.active() || state.remainingTicks(now) > 0) {
    return false;      // 没开或还没到点 → 什么都不做
}
// 到这里才是"真到期"
OverclockState.set(player, state.endAt(now, CyberwareStats.overclockCooldownTicks(player)));
```
修完请**顺带核对 `read()` 的两条自愈规则**是否会误伤正常状态（它们只能在本趟服务器运行内成立）。

### 1.2 「无法消耗生命值抵 RAM」
`client/HackRadialScreen.java` 的 commit 路径：
```java
if (HackClientState.affordable(entry)) hackId = entry.id();   // RAM 不够 → 连包都不发
```
⇒ 服务端 `HackSystem.pay()` 里那条"濒死超频 → 扣 2 点生命"的分支**永远收不到请求**。

**修法**：客户端**不再**以 RAM 作为"能不能发"的判据。改成：
* 超频中 → 一律允许发（提示语「超频中：以生命代偿 2 HP」），服务端裁决；
* 未超频且 RAM 不足 → 允许发但明确提示「RAM 不足」，由服务端回 `REJECTED`（**展示与裁决分离**，
  客户端只负责把选择告诉服务端）。
* `affordable()` 可以保留为**纯展示**（图标变灰），但**不许**再拦发包。

## 2. 数值与玩法改动（主人拍板）

| 项 | 改成 | 说明 |
|---|---|---|
| 基础 RAM 上限 | **12** | `maxRam = Math.max(12.0D, ΣRAM)`（接入仓高于 12 才继续涨上限） |
| 基础恢复速率 | **4.0 / 分钟** | `BASE_RAM_REGEN = 4.0D`（原 1.0，主人说太慢） |
| 击杀回 RAM | **常态 +2 / 超频中 +4** | 见下方判定 |
| 突触熔断 | **−24 HP + 凋零 IV 6 秒** | **替换**原效果（不再给缓慢/虚弱）：`hurtServer(indirectMagic, 24.0F)` + `MobEffects.WITHER, 120 刻, amp 3` |
| 超频时长/冷却表 | **不变** | 时长 10/12/14/16/18/20 秒、冷却 60/55/50/45/40/35 秒 |

**击杀回 RAM 判定（已 javap 核实，`Enemy`/`NeutralMob`/`Animal` 都是接口）**：
* 击杀者必须是 `ServerPlayer`（`LivingDeathEvent` 里取 `source.getEntity()`）；
* 受害者排除玩家本人、排除 `Animal`（狼/蜂/北极熊等），
* 受害者必须是 `Enemy`（僵尸/骷髅/掠夺者/猪灵…）**或** `NeutralMob`（末影人…）→
  等价写法：`(victim instanceof Enemy || victim instanceof NeutralMob) && !(victim instanceof Animal)`
  （村民、铁傀儡既不是 Enemy 也不是 NeutralMob，天然被排除，无需额外判断）；
* 超频中给 4、否则给 2，**夹在 [0, maxRam]**（满了不溢出）。

## 3. 键位最终形态（主人拍板：取消 G 键）

| 键 | 行为 |
|---|---|
| **V** | 手持义体激活（不变） |
| **R** | 义体轮盘 **+ 「脑机超频」一项**（选中松手 → `OverclockPayload.toggleRequest()`；冷却中显示剩余秒数并置灰） |
| **X 短按** | 歧路司扫描（松开时按住 < 300ms） |
| **X 长按** | 呼出**快速破解轮盘**（**按住满 300ms 即刻打开**，松手选择/取消） |
| ~~G~~ | **取消**：键位注册与分发都删掉（lang 条目可留可删，但不能再注册键位） |

长按阈值定为 **300ms**，实现放 `client/**`（`onClientTick` 里记按下时刻；打开过轮盘就不在松手时又发扫描）。

## 4. 写作用域

| 任务 | 成员 | 可写 |
|---|---|---|
| t35 修复 + 数值 | coredev | `core/**`、`network/**`、`event/**` |
| t36 客户端键位/轮盘/提示 | opticsdev | `client/**`、`assets/cyberware/lang/**` |
| t37 验收 | inspector | 只写 `VERIFY-051.md` |

## 5. 铁律与边界

1. 渲染/tick 回调整段 try/catch、异常只记一次（t34 刚补过，别退化）。
2. 单点注入 `require = 0`；新写 Mixin 先 `javap` 核实签名。
3. 客户端不做扣费/效果裁决 —— 但**也不要替服务端做"能不能发"的决定**（这正是 1.2 的教训）。
4. 静态缓存必须 `clear()` 且绑 `Level`/`player` 身份。
5. 本机**没有 `unzip`**，读 jar 用 `python3 -c "import zipfile..."`。
6. **未真机验证必须写明**；编译过 ≠ 玩得对。

## 6. 收尾

`gradle build --no-daemon` → inspector 独立验收（产物级）→ captain 提交 + 部署 FCL + 推 GitHub + 打 tag。
版本号：**0.5.1-Beta**。
