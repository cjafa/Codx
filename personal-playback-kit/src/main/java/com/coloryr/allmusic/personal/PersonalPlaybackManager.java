package com.coloryr.allmusic.personal;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PersonalPlaybackManager {
    private final Map<UUID, PlayerPlaybackSession> sessions = new ConcurrentHashMap<>();
    private final int maxSessions;
    private final int maxQueueSize;
    private final float defaultVolume;
    private final long timeoutMs;

    public PersonalPlaybackManager(int maxSessions, int maxQueueSize, float defaultVolume, long timeoutMs) {
        this.maxSessions = maxSessions;
        this.maxQueueSize = maxQueueSize;
        this.defaultVolume = defaultVolume;
        this.timeoutMs = timeoutMs;
    }

    public PlayerPlaybackSession getOrCreate(UUID playerId, long nowMs) {
        PlayerPlaybackSession existing = sessions.get(playerId);
        if (existing != null) {
            return existing;
        }
        if (sessions.size() >= maxSessions) {
            throw new IllegalStateException("session limit reached: " + maxSessions);
        }
        return sessions.computeIfAbsent(playerId,
                id -> new PlayerPlaybackSession(id, defaultVolume, maxQueueSize, nowMs));
    }

    public PlayerPlaybackSession get(UUID playerId) {
        return sessions.get(playerId);
    }

    public boolean remove(UUID playerId) {
        return sessions.remove(playerId) != null;
    }

    public int activeSessionCount() {
        return sessions.size();
    }

    public void tick(long deltaMs, long nowMs) {
        sessions.values().forEach(session -> session.tick(deltaMs, nowMs));
        sessions.entrySet().removeIf(entry -> nowMs - entry.getValue().lastActiveAt() > timeoutMs);
    }
}
