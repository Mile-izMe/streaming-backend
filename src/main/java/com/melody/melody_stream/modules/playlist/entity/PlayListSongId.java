package com.melody.melody_stream.modules.playlist.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PlayListSongId implements Serializable {

    @Column(name = "playlist_id")
    private String playlistId;

    @Column(name = "song_id")
    private String songId;
}
