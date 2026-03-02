# AllMusic_Server 个人播放功能设计方案

> 由于当前环境无法直接拉取上游仓库（访问 GitHub 返回 403），本方案先以 **可落地的服务端设计文档** 形式提交，便于你在现有 AllMusic_Server 代码上直接对照实现。

## 1. 目标

在现有“全服统一播放”能力基础上，新增“个人播放（Per-Player Playback）”：

- 每个玩家拥有独立播放队列、播放状态、音量与循环模式。
- 保留现有全局播放逻辑，二者可并行存在。
- 权限可控：管理员可禁用个人播放或限制并发人数。
- 兼容现有命令风格，减少迁移成本。

---

## 2. 核心模型

建议新增以下领域对象（按 Java 常见分层命名）：

### 2.1 PlayerPlaybackSession

用于表示一个玩家的独立播放会话。

关键字段：

- `UUID playerId`
- `Deque<TrackItem> queue`
- `TrackItem current`
- `PlaybackState state`（IDLE/PLAYING/PAUSED/STOPPED）
- `float volume`（0.0 ~ 1.0）
- `PlaybackMode mode`（SEQUENCE/LOOP_ONE/LOOP_ALL/RANDOM）
- `long positionMs`
- `long lastActiveAt`

### 2.2 TrackItem

- `String trackId`
- `String source`
- `String title`
- `long durationMs`
- `String requestBy`

### 2.3 PersonalPlaybackManager

负责管理所有会话：

- `ConcurrentHashMap<UUID, PlayerPlaybackSession> sessions`
- `getOrCreateSession(UUID playerId)`
- `removeSession(UUID playerId)`
- `tick()`（推进播放、清理超时会话）

---

## 3. 配置项

建议在配置中增加：

```yaml
personal-playback:
  enabled: true
  max-sessions: 200
  session-timeout-minutes: 30
  default-volume: 0.8
  max-queue-size: 100
  allow-global-and-personal-coexist: true
```

说明：

- `session-timeout-minutes`：玩家离线或长期无操作后回收。
- `max-sessions`：防止内存失控。
- `allow-global-and-personal-coexist`：是否允许全局与个人同时播放。

---

## 4. 命令/API 设计

若当前项目以命令驱动为主，可扩展命令：

- `/music p play <keyword|url>`：加入个人队列并开始播放
- `/music p pause`
- `/music p resume`
- `/music p stop`
- `/music p skip`
- `/music p list`
- `/music p volume <0-100>`
- `/music p mode <sequence|loop_one|loop_all|random>`

若已有 HTTP API，可新增前缀 `/api/personal/{playerId}`：

- `POST /play`
- `POST /pause`
- `POST /resume`
- `POST /stop`
- `POST /skip`
- `GET /queue`
- `PATCH /volume`

---

## 5. 状态机与调度

### 5.1 播放状态机

- `IDLE -> PLAYING`：首次播放/恢复播放
- `PLAYING -> PAUSED`：暂停
- `PAUSED -> PLAYING`：继续
- `PLAYING|PAUSED -> STOPPED`：手动停止
- `PLAYING -> IDLE`：队列结束

### 5.2 调度建议

复用当前服务端已有的音频 tick 线程（或调度器）：

1. 遍历活跃 `PlayerPlaybackSession`
2. 对 `PLAYING` 会话推进进度
3. 当前曲目结束时根据 `PlaybackMode` 选择下一首
4. 推送播放事件给对应玩家（仅单播）

---

## 6. 权限与隔离

- `allmusic.personal.use`：使用个人播放
- `allmusic.personal.manage.others`：管理他人会话（管理员）
- `allmusic.personal.unlimited`：绕过队列长度限制

隔离要求：

- 玩家 A 的命令不可影响玩家 B，除非具备管理权限。
- 全局播放命令默认不影响个人会话。

---

## 7. 兼容性策略

- 保持原有全局接口不变。
- 个人播放采用新命名空间（`p` 或 `personal`），避免命令冲突。
- 若旧客户端仅支持全局事件，个人播放事件需新增 channel/topic。

---

## 8. 实施步骤（建议）

1. 增加配置与启动期参数校验。
2. 引入 `PlayerPlaybackSession` 与 `PersonalPlaybackManager`。
3. 实现个人队列增删改查与状态机。
4. 接入音频调度与结束回调。
5. 增加命令/API 路由。
6. 增加权限校验。
7. 增加会话回收与监控指标（活跃会话数、平均队列长度）。
8. 回归测试（全局 + 个人并行场景）。

---

## 9. 测试用例清单

### 功能测试

- 单用户：播放/暂停/继续/停止/切歌。
- 多用户并发：A 与 B 的队列互不影响。
- 模式验证：`loop_one`、`loop_all`、`random` 行为正确。

### 边界测试

- 队列超限时拒绝入队。
- 超时会话自动清理。
- 离线重连后会话恢复策略（可配置：恢复/不恢复）。

### 回归测试

- 启用个人播放后，全局播放逻辑保持不变。
- 禁用个人播放配置时，命令/API 正确返回“功能关闭”。

---

## 10. 风险与优化

### 风险

- 每玩家一会话会增大内存与调度开销。
- 若音频解码实例按会话隔离，CPU 消耗显著上升。

### 优化建议

- 限制活跃会话与队列长度。
- 使用惰性创建会话。
- 增加缓存与对象池（如 Track 元数据缓存）。
- 暴露 Prometheus 指标，便于容量评估。

---

## 11. 最小可用版本（MVP）范围

首版建议只做：

- 独立队列
- 播放/暂停/继续/停止/切歌
- 音量控制
- 基础权限

将随机模式、复杂恢复策略、跨端同步放到第二阶段。
