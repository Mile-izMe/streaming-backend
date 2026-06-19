package com.melody.melody_stream.core.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CursorPage<T> {
    private List<T> items;
    private String nextCursor;
    private boolean hasMore;
}
