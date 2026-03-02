# Personal Playback Kit (AllMusic_Server integration template)

这是一个可直接移植到 `AllMusic_Server` 的最小实现模板，用于支持“个人播放（每个玩家独立队列）”。

## 包含内容

- `PlayerPlaybackSession`：单玩家会话（队列、状态、音量、模式）
- `PersonalPlaybackManager`：多玩家会话管理（并发 map、限流、超时回收）
- `PersonalPlaybackService`：对外动作层（play/pause/resume/stop/skip）
- `TrackItem` / `PlaybackState` / `PlaybackMode`：基础模型

## 建议接入方式

1. 在服务启动时创建 `PersonalPlaybackManager`：
   - `maxSessions` 对应配置 `personal-playback.max-sessions`
   - `maxQueueSize` 对应配置 `personal-playback.max-queue-size`
   - `defaultVolume` 对应配置 `personal-playback.default-volume`
   - `timeoutMs` 对应配置 `personal-playback.session-timeout-minutes`
2. 在已有 tick 线程里调用 `manager.tick(deltaMs, nowMs)`。
3. 把命令映射到 `PersonalPlaybackService`：
   - `/music p play ...` -> `play(...)`
   - `/music p pause` -> `pause(...)`
   - `/music p resume` -> `resume(...)`
   - `/music p stop` -> `stop(...)`
   - `/music p skip` -> `skip(...)`
   - `/music p volume <0-100>` -> `setVolume(...)`
   - `/music p mode ...` -> `setMode(...)`
4. 在命令层做权限校验：
   - `allmusic.personal.use`
   - `allmusic.personal.manage.others`

## 行为说明

- 新歌曲入队时，如果当前无播放曲目，则立即开始播放。
- `LOOP_ONE` 会重复当前曲目。
- `LOOP_ALL` 会把当前曲目在播放后放回队尾。
- `RANDOM` 会从当前队列随机选下一首。
- 长时间无活动会话会由 manager 在 `tick` 中自动回收。

## 本地验证

```bash
mvn test
```
