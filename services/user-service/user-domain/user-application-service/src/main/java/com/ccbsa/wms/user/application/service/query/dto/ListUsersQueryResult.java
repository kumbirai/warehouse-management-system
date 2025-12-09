package com.ccbsa.wms.user.application.service.query.dto;

import java.util.List;

/**
 * Query Result DTO for listing users.
 */
public class ListUsersQueryResult {
    private final List<GetUserQueryResult> items;
    private final long totalCount;
    private final int page;
    private final int size;

    public ListUsersQueryResult(List<GetUserQueryResult> items, long totalCount, int page, int size) {
        this.items = items != null ? List.copyOf(items) : List.of();
        this.totalCount = totalCount;
        this.page = page;
        this.size = size;
    }

    public List<GetUserQueryResult> getItems() {
        return items;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }
}

