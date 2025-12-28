package com.ccbsa.wms.stock.application.service.query.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;

/**
 * Query Result DTO: ListStockAllocationsQueryResult
 * <p>
 * Query result object for listing stock allocations.
 */
@Getter
@Builder
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Lombok builder stores list directly. Defensive copy made in constructor and getter returns immutable view.")
public final class ListStockAllocationsQueryResult {
    private final List<GetStockAllocationQueryResult> allocations;
    private final int totalCount;

    public ListStockAllocationsQueryResult(List<GetStockAllocationQueryResult> allocations, int totalCount) {
        if (allocations == null) {
            throw new IllegalArgumentException("Allocations list is required");
        }
        // Defensive copy to prevent external modification
        this.allocations = new ArrayList<>(allocations);
        this.totalCount = totalCount;
    }

    /**
     * Returns an unmodifiable view of the allocations list.
     *
     * @return Unmodifiable list of allocations
     */
    public List<GetStockAllocationQueryResult> getAllocations() {
        return Collections.unmodifiableList(allocations);
    }
}

