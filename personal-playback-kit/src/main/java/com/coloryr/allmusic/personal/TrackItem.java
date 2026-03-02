package com.coloryr.allmusic.personal;

import java.util.Objects;

public record TrackItem(String trackId, String source, String title, long durationMs, String requestBy) {
    public TrackItem {
        Objects.requireNonNull(trackId, "trackId cannot be null");
        Objects.requireNonNull(source, "source cannot be null");
        Objects.requireNonNull(title, "title cannot be null");
        if (durationMs <= 0) {
            throw new IllegalArgumentException("durationMs must be positive");
        }
    }
}
