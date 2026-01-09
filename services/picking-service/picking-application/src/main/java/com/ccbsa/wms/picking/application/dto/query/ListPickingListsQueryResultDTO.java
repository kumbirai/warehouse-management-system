package com.ccbsa.wms.picking.application.dto.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;

/**
 * Query Result DTO: ListPickingListsQueryResultDTO
 * <p>
 * DTO for listing picking lists query results.
 */
@Getter
@Builder
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in callers before passing to builder, and getter returns defensive copy")
public class ListPickingListsQueryResultDTO {
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in callers before passing to builder")
    private final List<PickingListViewDTO> pickingLists;
    private final int totalElements;
    private final int page;
    private final int size;
    private final int totalPages;

    /**
     * Returns a defensive copy of the picking lists list to prevent external modification.
     *
     * @return unmodifiable copy of the picking lists list
     */
    public List<PickingListViewDTO> getPickingLists() {
        if (pickingLists == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(pickingLists));
    }

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
