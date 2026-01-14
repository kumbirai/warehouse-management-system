package com.ccbsa.wms.gateway.api.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListPickingListsQueryResult {
    private List<PickingListView> pickingLists;
    private int totalElements;
    private int page;
    private int size;
    private int totalPages;

    @Getter
@Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PickingListView {
        private String id;
        private String status;
        private int loadCount;
        private int totalOrderCount;
    }
}
