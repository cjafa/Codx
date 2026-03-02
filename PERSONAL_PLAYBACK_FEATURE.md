# AllMusic_Server 个人播放功能（二次提交：含可运行模板）

这次不再只给设计说明，而是提供了一个 **可运行的 Java 模板工程**：`personal-playback-kit/`，可直接移植到 AllMusic_Server。

## 你现在可以直接用的内容

- `personal-playback-kit/src/main/java/com/coloryr/allmusic/personal/PlayerPlaybackSession.java`
- `personal-playback-kit/src/main/java/com/coloryr/allmusic/personal/PersonalPlaybackManager.java`
- `personal-playback-kit/src/main/java/com/coloryr/allmusic/personal/PersonalPlaybackService.java`
- `personal-playback-kit/src/main/java/com/coloryr/allmusic/personal/TrackItem.java`
- `personal-playback-kit/src/main/java/com/coloryr/allmusic/personal/PlaybackState.java`
- `personal-playback-kit/src/main/java/com/coloryr/allmusic/personal/PlaybackMode.java`
- `personal-playback-kit/src/test/java/com/coloryr/allmusic/personal/PersonalPlaybackManagerTest.java`

## 功能点（已在模板中实现）

1. 每玩家独立会话 + 独立队列。
2. 状态机：IDLE / PLAYING / PAUSED / STOPPED。
3. 模式：SEQUENCE / LOOP_ONE / LOOP_ALL / RANDOM。
4. 音量控制（0~1 自动 clamp）。
5. 会话上限、队列上限、会话超时回收。
6. 基础服务层接口：play/pause/resume/stop/skip/queue/volume/mode。

## 快速接入步骤（建议）

1. 在 AllMusic_Server 启动时创建 `PersonalPlaybackManager`。
2. 在现有音频调度 tick 中调用 `manager.tick(deltaMs, nowMs)`。
3. 新增命令前缀 `/music p ...`，映射到 `PersonalPlaybackService`。
4. 在命令层增加权限检查：
   - `allmusic.personal.use`
   - `allmusic.personal.manage.others`
5. 逐步扩展事件推送（仅发给对应玩家）与持久化（可选）。

## 配置建议

```yaml
personal-playback:
  enabled: true
  max-sessions: 200
  max-queue-size: 100
  default-volume: 0.8
  session-timeout-minutes: 30
```

## 本地验证

模板工程可直接运行：

```bash
cd personal-playback-kit
mvn test
```

> 当前容器无法直接访问 GitHub 上游仓库，因此本次以“可移植代码模板 + 测试”方式交付；你可以把 `personal-playback-kit/src/main/java/...` 代码直接合并进 AllMusic_Server 对应包结构。
