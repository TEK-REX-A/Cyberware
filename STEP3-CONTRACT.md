# 邮件IX 第三步 · 冻结契约（0.5.0-Beta）

上游：`/sdcard/DSH/Neko专属邮箱📮/邮件IX.txt`（CBAD）
沿用：`RAM-SYSTEM-SPEC.md`（铁律、写作用域原则）
本轮目标：补完**邮件IX 第三步**（X 键扫描 / 屏幕锁定框 / 上传进度条 / R 键破解轮盘）+ 把 0.4.0 的占位数值换成真数字。
版本号：**0.5.0-Beta**（captain 已改 `build.gradle`）

---

## 1. 冻结接口（两个成员必须按这个写，不许自行改名）

### 1.1 扫描（X 键）
* **复用 `HackPayload`，新增枚举值 `Action.SCAN`**（不新增包，避免协议版本再跳）。
* 上行：`action = SCAN`，`hackId = "scan"`，`targetEntityId = -1`，其余数值字段 0。
* 服务端收到 SCAN：
  1. 校验施法者已安装**歧路司义眼**（`kiroshi_*` / `iconic_advanced_kiroshi*` 任一在 `CyberwareInstallation` 里）；
     未装 → 回 `REJECTED`，`note = "NO_KIROSHI"`。
  2. 对施法者 **20 格内**的 `LivingEntity`（不含施法者本人）加 `MobEffects.GLOWING`，**1200 刻 = 60 秒**。
* 客户端只发请求，不自行加效果。

### 1.2 锁定目标（客户端算，服务端独立校验）
* 客户端锁定口径：**屏幕中心 10% 半径内、距离 ≤ 20 格、最近的活体**。
* 服务端在 `CAST` 时**自己再校验一遍**（不信任客户端）：目标存活、距离 ≤ 20、`hasLineOfSight`；
  不通过 → `REJECTED`，`note = "NO_TARGET"`。

### 1.3 R 键上下文切换
* **有锁定目标** → 破解轮盘：5 条（过热 / 短路 / 突触熔断 / 武器故障 / 系统重置），显示 RAM 消耗，
  RAM 不足的条目**置灰且不可发**。
* **无锁定目标** → 回落到现有 `CyberwareRadialScreen` 义体轮盘，**行为一字不改**（t14/t15 成果）。
* 客户端展示表：`client/**` 里维护一份只读的 id / 显示名 / RAM 消耗表（纯展示用）。
  **服务端仍是唯一权威**，客户端不做任何裁决。

### 1.4 上传进度条
* 数据全部来自 `HackPayload` 已有的 `UPLOAD_START` / `UPLOAD_PROGRESS` / `APPLIED` / `CANCELLED` / `REJECTED`（t24 已实现）。
* 位置：屏幕中央偏下；极细红线 + 流动光效；上方全息倒计时 `UPLOADING... 1.2s`；右侧滚动随机二进制；
  完成瞬间收缩消失。
* 目标失效/施法者死亡 → 收到 `CANCELLED`，进度条立刻消失，**不许留残影**。

### 1.5 键位
| 键 | 用途 |
|---|---|
| V | 手持义体激活（**不变**） |
| R | 义体轮盘 / 破解轮盘（上下文） |
| G | 脑机超频（0.4.0 已有） |
| **X** | **歧路司扫描（本轮新增）**，lang 键 `key.cyberware.scan` |

## 2. 数值

占位值一律换成 `HACK-VALUES.md`（t30 交付）里的数字。
替换完成前，代码里保持 `TODO(主人填写)` 标记；替换后**在同一行注释里写出来源**（如 `// 2077: Overheat 4 RAM`）。

## 3. 写作用域

| 任务 | 成员 | 可写 |
|---|---|---|
| t28 扫描服务端与协议 | coredev | `core/**`、`network/**`、`event/**` |
| t29 第三步客户端 | opticsdev | `client/**`、`assets/cyberware/lang/**` |
| t30 数值调研 | numbersmith | 只写 `HACK-VALUES.md` |
| t31 数值替换 | coredev | `core/**`（与 t28 同一成员，串行执行，见任务描述） |
| t32 验收 | inspector | 只写 `VERIFY-050.md` |

## 4. 铁律（沿用，不许省）

1. 渲染回调整体 try/catch；本地玩家用渲染状态类型判定，禁止坐标比较。
2. 单点注入 `require = 0`；写 Mixin 前先 `javap` 核实签名。
3. 客户端不做扣费/效果判定。
4. 静态缓存必须提供 `clear()` 且绑 `Level`/`player` 身份（0.3.12 P0 教训）。
5. 本机**没有 `unzip`**；读 jar 用 `python3 -c "import zipfile..."`。
6. **未真机验证必须写明**，不许把"编译过了"写成"能玩"。

## 5. 收尾

* 全量 `gradle build --no-daemon` exit 0 → inspector 独立验收（产物级 javap + 静态 grep）。
* captain 统一 commit + 部署 FCL + 推送 GitHub。
* 数值表里凡有不确定的，**在交付文档里单独列一节**，交给主人裁决，不许含糊过去。
