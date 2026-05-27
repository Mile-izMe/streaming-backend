package com.melody.melody_stream.entity;

import com.melody.melody_stream.core.entity.AuditableEntity;
import com.melody.melody_stream.modules.song.entity.Song;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.SQLDelete;

@Entity
@Table(name = "playlist_songs")
@SQLDelete(sql = "UPDATE playlist_songs SET deleted_at = NOW() WHERE playlist_id = ? AND song_id = ?")
@FilterDef(name = "deletedFilter", parameters = @ParamDef(name = "isDeleted", type = Boolean.class))
@Filter(name = "deletedFilter", condition = "deleted_at IS NULL")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistSong extends AuditableEntity {

    @EmbeddedId
    private PlayListSongId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("playlistId")
    @JoinColumn(name = "playlist_id")
    private Playlist playlist;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("songId")
    @JoinColumn(name = "song_id")
    private Song song;
}