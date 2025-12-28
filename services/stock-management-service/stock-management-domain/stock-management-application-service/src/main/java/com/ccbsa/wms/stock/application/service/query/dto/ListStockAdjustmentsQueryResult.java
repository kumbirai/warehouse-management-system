package com.ccbsa.wms.stock.application.service.query.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;

/**
 * Query Result DTO: ListStockAdjustmentsQueryResult
 * <p>
 * Query result object for listing stock adjustments.
 */
@Getter
@Builder
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Lombok builder stores list directly. Defensive copy made in constructor and getter returns immutable view.")
public final class ListStockAdjustmentsQueryResult {
    private final List<GetStockAdjustmentQueryResult> adjustments;
    private final int totalCount;

    public ListStockAdjustmentsQueryResult(List<GetStockAdjustmentQueryResult> adjustments, int totalCount) {
        if (adjustments == null) {
            throw new IllegalArgumentException("Adjustments list is required");
        }
        // Defensive copy to prevent external modification
        this.adjustments = new ArrayList<>(adjustments);
        this.totalCount = totalCount;
    }

    /**
     * Returns an unmodifiable view of the adjustments list.
     *
     * @return Unmodifiable list of adjustments
     */
    public List<GetStockAdjustmentQueryResult> getAdjustments() {
        return Collections.unmodifiableList(adjustments);
    }
}

