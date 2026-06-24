package com.melody.melody_stream.core.event;

import com.melody.melody_stream.modules.song.entity.Song;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SongChangedEvent {
    private final Song song;
}
