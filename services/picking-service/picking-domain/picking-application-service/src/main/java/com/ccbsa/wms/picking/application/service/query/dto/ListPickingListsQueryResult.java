package com.ccbsa.wms.picking.application.service.query.dto;

import java.util.Collections;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;

/**
 * Query Result DTO: ListPickingListsQueryResult
 * <p>
 * Result object returned from listing picking lists query.
 */
@Getter
@Builder
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in constructor and getter returns defensive copy")
public final class ListPickingListsQueryResult {
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in constructor")
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

    /**
     * Returns a defensive copy of the picking lists list to prevent external modification.
     *
     * @return unmodifiable copy of the picking lists list
     */
    public List<PickingListView> getPickingLists() {
        return Collections.unmodifiableList(pickingLists);
    }
}
