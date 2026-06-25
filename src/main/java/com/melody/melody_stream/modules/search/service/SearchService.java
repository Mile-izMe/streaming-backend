package com.melody.melody_stream.modules.search.service;

import com.melody.melody_stream.infrastructure.minio.service.MinioBuildService;
import com.melody.melody_stream.modules.search.document.SongDocument;
import com.melody.melody_stream.modules.search.dto.SearchResponse;
import com.melody.melody_stream.modules.search.dto.SearchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final MinioBuildService minioBuildService;

    public SearchResponse search(String keyword, int size) {

        String searchKeyword = "*" + keyword.toLowerCase().trim() + "*";

        // ── Search Songs ──────────────────────────────────────
        Query songQuery = NativeQuery.builder()
                .withQuery(q -> q
                        .bool(b -> b
                                .must(m -> m
                                        .queryString(qs -> qs
                                                .fields("title", "artist")
                                                .query(searchKeyword)
                                        )
                                )
                                .filter(f -> f.match(t -> t.field("status").query("COMPLETED")))
                        )
                )
                .withMaxResults(size)
                .build();

        List<SearchResult> songs = elasticsearchOperations
                .search(songQuery, SongDocument.class)
                .stream()
                .map(hit -> SearchResult.builder()
                        .type("SONG")
                        .id(hit.getContent().getId())
                        .title(hit.getContent().getTitle())
                        .subtitle(hit.getContent().getArtist())
                        .thumbnailUrl(minioBuildService.buildSignedGetUrl(hit.getContent().getThumbnailUrl(), 3600))
                        .data(hit.getContent())
                        .build()
                )
                .toList();

        // ── Search Artists ────────────────────────────────────
        // Aggregate unique artists from song index
        List<SearchResult> artists = songs.stream()
                .map(s -> ((SongDocument) s.getData()).getArtist())
                .distinct()
                .filter(artist -> artist.toLowerCase().contains(keyword.toLowerCase()))
                .map(artist -> SearchResult.builder()
                        .type("ARTIST")
                        .id(artist)
                        .title(artist)
                        .subtitle("Nghệ sĩ")
                        .build()
                )
                .toList();

        return SearchResponse.builder()
                .songs(songs)
                .artists(artists)
                .total(songs.size() + artists.size())
                .build();
    }
}
