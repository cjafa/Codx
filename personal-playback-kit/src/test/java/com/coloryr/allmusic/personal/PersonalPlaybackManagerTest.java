package com.coloryr.allmusic.personal;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PersonalPlaybackManagerTest {

    @Test
    void shouldKeepSessionsIsolated() {
        PersonalPlaybackManager manager = new PersonalPlaybackManager(10, 10, 0.8F, 60_000);
        PersonalPlaybackService service = new PersonalPlaybackService(manager);

        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();

        service.play(a, new TrackItem("1", "yt", "song-a1", 1_000, "A"), 1);
        service.play(b, new TrackItem("2", "yt", "song-b1", 1_000, "B"), 1);
        service.play(a, new TrackItem("3", "yt", "song-a2", 1_000, "A"), 1);

        assertEquals(1, service.queue(b).size());
        assertEquals(1, service.queue(a).size());
        assertEquals("song-a1", manager.get(a).current().title());
        assertEquals("song-b1", manager.get(b).current().title());
    }

    @Test
    void shouldAdvanceTrackWhenTickReachesDuration() {
        PersonalPlaybackManager manager = new PersonalPlaybackManager(10, 10, 0.8F, 60_000);
        UUID player = UUID.randomUUID();
        PlayerPlaybackSession session = manager.getOrCreate(player, 0);
        session.enqueue(new TrackItem("1", "yt", "song1", 1_000, "U"), 0);
        session.enqueue(new TrackItem("2", "yt", "song2", 1_000, "U"), 0);

        manager.tick(1_000, 1_000);

        assertNotNull(session.current());
        assertEquals("song2", session.current().title());
        assertEquals(PlaybackState.PLAYING, session.state());
    }

    @Test
    void shouldRespectQueueLimit() {
        PersonalPlaybackManager manager = new PersonalPlaybackManager(10, 1, 0.8F, 60_000);
        UUID player = UUID.randomUUID();
        PlayerPlaybackSession session = manager.getOrCreate(player, 0);

        session.enqueue(new TrackItem("1", "yt", "song1", 1_000, "U"), 0);
        session.enqueue(new TrackItem("2", "yt", "song2", 1_000, "U"), 0);

        assertThrows(IllegalStateException.class,
                () -> session.enqueue(new TrackItem("3", "yt", "song3", 1_000, "U"), 0));
    }

    @Test
    void shouldCleanupExpiredSessionOnTick() {
        PersonalPlaybackManager manager = new PersonalPlaybackManager(10, 10, 0.8F, 1000);
        UUID player = UUID.randomUUID();

        manager.getOrCreate(player, 0);
        assertEquals(1, manager.activeSessionCount());

        manager.tick(100, 1_500);

        assertEquals(0, manager.activeSessionCount());
    }
}
