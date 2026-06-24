package com.melody.melody_stream.modules.search.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SearchResponse {
    private List<SearchResult> songs;
    private List<SearchResult> artists;
    private int total;
}
