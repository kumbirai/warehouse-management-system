package com.ccbsa.wms.stock.application.dto.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO: StockAvailabilityFefoResponseDTO
 * <p>
 * Response DTO for FEFO stock availability query.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in callers before passing to builder, and getter returns defensive copy")
public class StockAvailabilityFefoResponseDTO {
    private String productCode;
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in callers before passing to builder")
    private List<StockAvailabilityItemDTO> stockItems;

    /**
     * Returns a defensive copy of the stock items list to prevent external modification.
     *
     * @return unmodifiable copy of the stock items list
     */
    public List<StockAvailabilityItemDTO> getStockItems() {
        if (stockItems == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(stockItems));
    }
}
