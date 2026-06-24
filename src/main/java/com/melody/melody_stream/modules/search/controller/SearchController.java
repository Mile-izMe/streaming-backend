package com.melody.melody_stream.modules.search.controller;

import com.melody.melody_stream.modules.search.dto.SearchResponse;
import com.melody.melody_stream.modules.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<SearchResponse> search(
            @RequestParam String keyword,
            @RequestParam (defaultValue = "10") int size
    ) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.ok(SearchResponse.builder().build());
        }

        SearchResponse searchResponse = searchService.search(keyword.trim(), size);
        return ResponseEntity.ok(searchResponse);
    }
}
