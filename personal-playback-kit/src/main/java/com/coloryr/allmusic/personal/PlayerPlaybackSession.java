package com.coloryr.allmusic.personal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class PlayerPlaybackSession {
    private final UUID playerId;
    private final Deque<TrackItem> queue = new ArrayDeque<>();
    private TrackItem current;
    private PlaybackState state = PlaybackState.IDLE;
    private PlaybackMode mode = PlaybackMode.SEQUENCE;
    private float volume;
    private long positionMs;
    private long lastActiveAt;
    private final int maxQueueSize;

    public PlayerPlaybackSession(UUID playerId, float defaultVolume, int maxQueueSize, long nowMs) {
        this.playerId = playerId;
        this.volume = clampVolume(defaultVolume);
        this.maxQueueSize = maxQueueSize;
        this.lastActiveAt = nowMs;
    }

    public synchronized UUID playerId() {
        return playerId;
    }

    public synchronized PlaybackState state() {
        return state;
    }

    public synchronized PlaybackMode mode() {
        return mode;
    }

    public synchronized TrackItem current() {
        return current;
    }

    public synchronized float volume() {
        return volume;
    }

    public synchronized long positionMs() {
        return positionMs;
    }

    public synchronized long lastActiveAt() {
        return lastActiveAt;
    }

    public synchronized List<TrackItem> queueSnapshot() {
        return Collections.unmodifiableList(new ArrayList<>(queue));
    }

    public synchronized void setMode(PlaybackMode mode, long nowMs) {
        this.mode = mode;
        touch(nowMs);
    }

    public synchronized void setVolume(float volume, long nowMs) {
        this.volume = clampVolume(volume);
        touch(nowMs);
    }

    public synchronized void enqueue(TrackItem item, long nowMs) {
        if (queue.size() >= maxQueueSize) {
            throw new IllegalStateException("queue limit reached: " + maxQueueSize);
        }
        queue.addLast(item);
        if (current == null) {
            current = queue.pollFirst();
            state = PlaybackState.PLAYING;
            positionMs = 0;
        }
        touch(nowMs);
    }

    public synchronized void pause(long nowMs) {
        if (state == PlaybackState.PLAYING) {
            state = PlaybackState.PAUSED;
        }
        touch(nowMs);
    }

    public synchronized void resume(long nowMs) {
        if (state == PlaybackState.PAUSED || state == PlaybackState.IDLE) {
            if (current != null || !queue.isEmpty()) {
                if (current == null) {
                    current = queue.pollFirst();
                    positionMs = 0;
                }
                state = PlaybackState.PLAYING;
            }
        }
        touch(nowMs);
    }

    public synchronized void stop(long nowMs) {
        state = PlaybackState.STOPPED;
        current = null;
        queue.clear();
        positionMs = 0;
        touch(nowMs);
    }

    public synchronized TrackItem skip(long nowMs) {
        TrackItem skipped = current;
        advanceToNext();
        touch(nowMs);
        return skipped;
    }

    public synchronized void tick(long deltaMs, long nowMs) {
        if (state != PlaybackState.PLAYING || current == null) {
            touch(nowMs);
            return;
        }

        positionMs += deltaMs;
        while (current != null && positionMs >= current.durationMs()) {
            positionMs -= current.durationMs();
            advanceToNext();
            if (state != PlaybackState.PLAYING) {
                positionMs = 0;
                break;
            }
        }
        touch(nowMs);
    }

    private void advanceToNext() {
        if (current == null) {
            current = queue.pollFirst();
            if (current == null) {
                state = PlaybackState.IDLE;
            } else {
                state = PlaybackState.PLAYING;
            }
            positionMs = 0;
            return;
        }

        TrackItem previous = current;
        switch (mode) {
            case LOOP_ONE -> {
                state = PlaybackState.PLAYING;
                positionMs = 0;
                return;
            }
            case LOOP_ALL -> queue.addLast(previous);
            case RANDOM -> {
                if (!queue.isEmpty()) {
                    List<TrackItem> list = new ArrayList<>(queue);
                    int index = ThreadLocalRandom.current().nextInt(list.size());
                    TrackItem selected = list.get(index);
                    queue.remove(selected);
                    current = selected;
                    state = PlaybackState.PLAYING;
                    positionMs = 0;
                    return;
                }
            }
            case SEQUENCE -> {
                // continue
            }
        }

        current = queue.pollFirst();
        positionMs = 0;
        state = current == null ? PlaybackState.IDLE : PlaybackState.PLAYING;
    }

    private static float clampVolume(float volume) {
        if (volume < 0F) {
            return 0F;
        }
        if (volume > 1F) {
            return 1F;
        }
        return volume;
    }

    private void touch(long nowMs) {
        this.lastActiveAt = nowMs;
    }
}
