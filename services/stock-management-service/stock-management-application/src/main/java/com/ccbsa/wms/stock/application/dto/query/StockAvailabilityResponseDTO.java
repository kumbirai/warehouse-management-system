package com.ccbsa.wms.stock.application.dto.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO: StockAvailabilityResponseDTO
 * <p>
 * Response DTO for stock availability query for multiple products.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in callers before passing to builder, and getter returns defensive copy")
public class StockAvailabilityResponseDTO {
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in callers before passing to builder")
    private Map<String, List<StockAvailabilityItemDTO>> stockByProduct;

    /**
     * Returns a defensive copy of the stock by product map to prevent external modification.
     *
     * @return unmodifiable copy of the stock by product map
     */
    public Map<String, List<StockAvailabilityItemDTO>> getStockByProduct() {
        if (stockByProduct == null) {
            return Collections.emptyMap();
        }
        Map<String, List<StockAvailabilityItemDTO>> defensiveCopy = new HashMap<>();
        stockByProduct.forEach((key, value) -> defensiveCopy.put(key, Collections.unmodifiableList(new ArrayList<>(value))));
        return Collections.unmodifiableMap(defensiveCopy);
    }
}
