package com.ccbsa.wms.picking.application.service.query.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * Query Result DTO: ListPickingListsQueryResult
 * <p>
 * Result object returned from listing picking lists query.
 */
@Getter
@Builder
public final class ListPickingListsQueryResult {
    private final List<PickingListView> pickingLists;
    private final int totalElements;
    private final int page;
    private final int size;
    private final int totalPages;

    public ListPickingListsQueryResult(List<PickingListView> pickingLists, int totalElements, int page, int size, int totalPages) {
        this.pickingLists = pickingLists != null ? List.copyOf(pickingLists) : List.of();
        this.totalElements = totalElements;
        this.page = page;
        this.size = size;
        this.totalPages = totalPages;
    }
}
