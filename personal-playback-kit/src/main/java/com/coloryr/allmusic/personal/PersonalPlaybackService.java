package com.coloryr.allmusic.personal;

import java.util.List;
import java.util.UUID;

public final class PersonalPlaybackService {
    private final PersonalPlaybackManager manager;

    public PersonalPlaybackService(PersonalPlaybackManager manager) {
        this.manager = manager;
    }

    public void play(UUID playerId, TrackItem track, long nowMs) {
        manager.getOrCreate(playerId, nowMs).enqueue(track, nowMs);
    }

    public void pause(UUID playerId, long nowMs) {
        PlayerPlaybackSession session = manager.get(playerId);
        if (session != null) {
            session.pause(nowMs);
        }
    }

    public void resume(UUID playerId, long nowMs) {
        PlayerPlaybackSession session = manager.get(playerId);
        if (session != null) {
            session.resume(nowMs);
        }
    }

    public void stop(UUID playerId, long nowMs) {
        PlayerPlaybackSession session = manager.get(playerId);
        if (session != null) {
            session.stop(nowMs);
        }
    }

    public TrackItem skip(UUID playerId, long nowMs) {
        PlayerPlaybackSession session = manager.get(playerId);
        return session == null ? null : session.skip(nowMs);
    }

    public List<TrackItem> queue(UUID playerId) {
        PlayerPlaybackSession session = manager.get(playerId);
        return session == null ? List.of() : session.queueSnapshot();
    }

    public void setVolume(UUID playerId, int volumePercent, long nowMs) {
        PlayerPlaybackSession session = manager.get(playerId);
        if (session != null) {
            session.setVolume(volumePercent / 100F, nowMs);
        }
    }

    public void setMode(UUID playerId, PlaybackMode mode, long nowMs) {
        PlayerPlaybackSession session = manager.get(playerId);
        if (session != null) {
            session.setMode(mode, nowMs);
        }
    }
}
