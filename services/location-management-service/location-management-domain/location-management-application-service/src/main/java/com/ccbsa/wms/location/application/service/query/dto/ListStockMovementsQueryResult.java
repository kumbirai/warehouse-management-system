package com.ccbsa.wms.location.application.service.query.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Query Result DTO: ListStockMovementsQueryResult
 * <p>
 * Result for listing stock movements.
 */
@Getter
@Builder
@EqualsAndHashCode
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Lombok builder stores list directly. Defensive copy made in constructor and getter returns immutable view.")
public final class ListStockMovementsQueryResult {
    private final List<StockMovementQueryResult> movements;
    private final int totalCount;

    public ListStockMovementsQueryResult(List<StockMovementQueryResult> movements, int totalCount) {
        if (movements == null) {
            throw new IllegalArgumentException("Movements list is required");
        }
        // Defensive copy to prevent external modification
        this.movements = new ArrayList<>(movements);
        this.totalCount = totalCount;
    }

    /**
     * Returns an unmodifiable view of the movements list.
     *
     * @return Unmodifiable list of movements
     */
    public List<StockMovementQueryResult> getMovements() {
        return Collections.unmodifiableList(movements);
    }
}

