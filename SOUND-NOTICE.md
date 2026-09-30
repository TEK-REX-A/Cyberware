# SOUND-NOTICE.md —— 斯安威斯坦开启音效接线（t38）

> opticsdev，attempt `633d9ecc-4616-4120-a926-9f0449415584`
> ⚠ 文末有「未真机验证」与**版权声明**两节，发帖/打包前必读。

---

## 1. 音频素材（不含代码改动）

`src/main/resources/assets/cyberware/sounds/sandevistan_activate.ogg`

| 项 | 值 | 怎么核的 |
|---|---|---|
| 文件大小 | 26,607 字节 | `ls -la` |
| 容器 | Ogg（`OggS` 魔数） | python 读前 4 字节 |
| 编码 | Vorbis | 找到 `\x01vorbis` identification header |
| 声道数 | **1（单声道）** | 读 header 的 channels 字段 |
| 采样率 | **44100 Hz** | 读 header 的 rate 字段 |

来源：主人提供的 `/sdcard/DSH/audio.mp3`（151 秒）→ captain 截取**前 3 秒**、末尾 80ms 淡出。
**本任务没有重新编码它**，也没有改时长（只做了读 header 核验，写操作一次都没有）。

---

## 2. 代码接线（4 处）

| # | 文件 | 改了什么 |
|---|---|---|
| 1 | `assets/cyberware/sounds.json`（**新建**） | 注册 `sandevistan_activate`：`sounds` → `cyberware:sandevistan_activate`；带一个 subtitle 键 `cyberware.subtitle.sandevistan_activate`（可选，缺 lang 时只是不显示副标题，不影响播放） |
| 2 | `registry/ModSounds.java`（**新建**） | `DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MODID)`；`SANDEVISTAN_ACTIVATE` → id `cyberware:sandevistan_activate`（`SoundEvent.createVariableRangeEvent`） |
| 3 | `Cyberware.java`（+1 行 +1 import） | `ModSounds.SOUNDS.register(modEventBus)` —— **mod 事件总线**（不是 NeoForge 总线；挂错会静默不注册） |
| 4 | `client/SandevistanSounds.java`（**新建**）+ `client/ClientTimeDilation.java`（+7 行） | 播放封装 + 在「新建一条减速源」时调一次 |

`RamHud` 里那批超频音效（原版 `SoundEvent` + pitch）**一行未动** —— 两套音效互不干扰。

### 2.1 播放时机（为什么不会重复 / 不会误播）

`ClientTimeDilation.applyOnClient` 有两个出口：

```java
// ① 同一个施法者的「刷新广播」（服务端每 20 刻一次）→ 只把 endTick 往后推，然后 return
for (Source s : SOURCES) {
    if (owner.equals(s.owner) && now < s.endTick) { ...; return; }     // ← 这里不播
}
...
// ② 新建一条源 = 减速真的开始了 → 在这里播一次
SOURCES.add(new Source(...));
if (owner != null) {
    LocalPlayer self = Minecraft.getInstance().player;
    if (self != null && owner.equals(self.getUUID())) {
        SandevistanSounds.playActivate();                              // ← 只在这里播
    }
}
```

- **不会每 tick 刷**：播放点在「新建源」这一条路径上，而百分比/进度条那种每帧读的是 `ratioNow()/progress()`，跟这里无关；
- **不会每秒重复**：服务端的 20 刻刷新广播走 ① 并提前 `return`；
- **结束不播**：到期只是 `endTick` 到点被清理，不经过本方法；
- **换维度/死亡不播**：换世界走 `ClientTimeDilation.clear()`（清表），死亡不发这条广播；
- **只在本地玩家自己开时播**：比对 `owner` 与 `Minecraft.getInstance().player.getUUID()` —— 别人开的减速我们只是旁观者，不播。

### 2.2 只在客户端

`SandevistanSounds` / `ClientTimeDilation` 都在 `client/**`（只在 `Dist.CLIENT` 下加载）；
服务端不引用这两个类，也不会执行播放。`ModSounds` 是**注册**（两端都要注册），但**播放**只在客户端。

### 2.3 保命

`SandevistanSounds.playActivate()` 整段 `try/catch(Throwable)`，异常只记一次日志
（与既有渲染回调同一套铁律）。音效只是装饰，出问题不影响游戏。

---

## 3. 验证

```
cd /root/mod26/cyberware
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-arm64 /opt/gradle-8.10.2/bin/gradle compileJava --no-daemon
→ BUILD SUCCESSFUL in 1m 23s

python3 -c "import json;json.load(open('src/main/resources/assets/cyberware/sounds.json'));print('sounds.json OK')"
→ sounds.json OK
```

产物级：见任务报告里的 `gradle jar` + python zipfile 列目录结果
（本机**没有 unzip**，按契约 §5 用 python3 zipfile 读）。

---

## 4. ⚠ 未真机验证

**没有启动过游戏。** 以下全部由主人在真机上判断：

1. **音效实际听感与音量** —— 3 秒素材、`SimpleSoundInstance.forUI`（走 MASTER 音量），
   音量是否合适、会不会太响/太闷，只能听；
2. **触发时机是否对**：按技能那一刻响一次；反复激活（刷新广播）时不重复响；
3. **与超频音效是否打架**：同时开斯安威斯坦 + 脑机超频时的混音效果（两套逻辑在代码上互不影响，
   但听感只能主人判断）；
4. **副标题**：`cyberware.subtitle.sandevistan_activate` 这个 lang 键**我没有加**
   （本轮 inScope 不含 lang；且它只是可选字段）。若主人要显示副标题，需要补 lang 条目 —— 说一句我加。

---

## 5. ⚠ 版权声明（发帖/打包前必读）

- `sounds/sandevistan_activate.ogg` 是**主人提供的第三方音频素材**（来自其个人文件 `audio.mp3`）。
- 它**不在模组代码的 AGPL-3.0 范围内** —— 代码许可只覆盖模组自己的源码，不覆盖这段音频。
- 与 `assets/cyberware/textures/**` 那批 32×32 贴图**同一类处理**：
  **将来发帖 / 上传仓库 / 发布整合包时，必须在 `NOTICE` 里单列**，写明来源与授权情况，
  不要让它被 AGPL-3.0 的表述覆盖过去。
- 本任务只做了**接线**（sounds.json + 注册 + 播放），没有修改、没有重新编码这段音频。
