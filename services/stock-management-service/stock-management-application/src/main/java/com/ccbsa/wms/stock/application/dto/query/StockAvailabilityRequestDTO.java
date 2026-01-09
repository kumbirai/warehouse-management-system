package com.ccbsa.wms.stock.application.dto.query;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO: StockAvailabilityRequestDTO
 * <p>
 * Request DTO for stock availability query for multiple products.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in callers before passing to builder, and getter returns defensive copy")
public class StockAvailabilityRequestDTO {
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in callers before passing to builder")
    private Map<String, Integer> productQuantities;

    /**
     * Returns a defensive copy of the product quantities map to prevent external modification.
     *
     * @return unmodifiable copy of the product quantities map
     */
    public Map<String, Integer> getProductQuantities() {
        if (productQuantities == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new HashMap<>(productQuantities));
    }
}
