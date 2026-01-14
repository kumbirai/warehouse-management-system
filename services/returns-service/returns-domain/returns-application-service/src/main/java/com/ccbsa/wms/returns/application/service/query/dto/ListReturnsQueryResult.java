package com.ccbsa.wms.returns.application.service.query.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * Query Result DTO: ListReturnsQueryResult
 * <p>
 * Query result object for listing returns with pagination.
 */
@Getter
@Builder
public final class ListReturnsQueryResult {
    private final List<GetReturnQueryResult> returns;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    public ListReturnsQueryResult(List<GetReturnQueryResult> returns, int page, int size, long totalElements, int totalPages) {
        if (returns == null) {
            throw new IllegalArgumentException("Returns list cannot be null");
        }
        if (page < 0) {
            throw new IllegalArgumentException("Page must be >= 0");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be > 0");
        }
        if (totalElements < 0) {
            throw new IllegalArgumentException("Total elements must be >= 0");
        }
        if (totalPages < 0) {
            throw new IllegalArgumentException("Total pages must be >= 0");
        }
        this.returns = List.copyOf(returns);
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }
}
