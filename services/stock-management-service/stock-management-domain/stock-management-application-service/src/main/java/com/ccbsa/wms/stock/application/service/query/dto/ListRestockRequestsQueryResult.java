package com.ccbsa.wms.stock.application.service.query.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;

/**
 * Query Result DTO: ListRestockRequestsQueryResult
 * <p>
 * Result object for listing restock requests.
 */
@Getter
@Builder
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in callers before passing to builder, and getter returns defensive copy")
public final class ListRestockRequestsQueryResult {
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in callers before passing to builder")
    private final List<RestockRequestQueryResult> requests;
    private final int totalCount;

    /**
     * Returns a defensive copy of the requests list to prevent external modification.
     *
     * @return unmodifiable copy of the requests list
     */
    public List<RestockRequestQueryResult> getRequests() {
        if (requests == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(requests));
    }
}
