package com.melody.melody_stream.modules.song.entity;

import com.melody.melody_stream.core.entity.AuditableEntity;
import com.melody.melody_stream.core.enums.SongStatus;
import com.melody.melody_stream.entity.PlaylistSong;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "songs")
@SQLDelete(sql = "UPDATE songs SET deleted_at = NOW() WHERE id = ?")
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Song extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "artist", nullable = false)
    private String artist;

    @Column(name = "audio_url", nullable = false)
    private String audioUrl;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    // Duration in milliseconds — same as Prisma schema comment
    @Column(name = "duration")
    private Integer duration;

    @Column(name = "lyrics", columnDefinition = "TEXT")
    private String lyrics;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SongStatus status = SongStatus.PENDING;

    @OneToMany(mappedBy = "song", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PlaylistSong> playlists = new ArrayList<>();
}