package com.melody.melody_stream.modules.search.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SearchResult {
    private String type;
    private String id;
    private String title;
    private String subtitle;
    private String thumbnailUrl;
    private Object data;
}
