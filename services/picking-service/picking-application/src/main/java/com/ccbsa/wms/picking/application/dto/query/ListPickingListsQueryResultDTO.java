package com.ccbsa.wms.picking.application.dto.query;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * Query Result DTO: ListPickingListsQueryResultDTO
 * <p>
 * DTO for listing picking lists query results.
 */
@Getter
@Builder
public class ListPickingListsQueryResultDTO {
    private final List<PickingListViewDTO> pickingLists;
    private final int totalElements;
    private final int page;
    private final int size;
    private final int totalPages;

    @Getter
    @Builder
    public static class PickingListViewDTO {
        private final String id;
        private final String pickingListReference;
        private final String status;
        private final int loadCount;
        private final int totalOrderCount;
    }
}
