package com.ccbsa.wms.stock.application.service.query.dto;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.TenantId;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Query DTO: QueryStockAvailabilityForProductsQuery
 * <p>
 * Query for retrieving stock availability for multiple products.
 */
@Getter
@Builder
@EqualsAndHashCode
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in callers before passing to builder, and getter returns defensive copy")
public final class QueryStockAvailabilityForProductsQuery {
    private final TenantId tenantId;
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copies are created in callers before passing to builder")
    private final Map<ProductId, Integer> productQuantities; // ProductId -> required quantity

    /**
     * Returns a defensive copy of the product quantities map to prevent external modification.
     *
     * @return unmodifiable copy of the product quantities map
     */
    public Map<ProductId, Integer> getProductQuantities() {
        if (productQuantities == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new HashMap<>(productQuantities));
    }

    /**
     * Static factory method with validation.
     */
    public static QueryStockAvailabilityForProductsQuery of(TenantId tenantId, Map<ProductId, Integer> productQuantities) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (productQuantities == null || productQuantities.isEmpty()) {
            throw new IllegalArgumentException("Product quantities map is required and cannot be empty");
        }
        // Create defensive copy in builder
        Map<ProductId, Integer> defensiveCopy = new HashMap<>(productQuantities);
        return QueryStockAvailabilityForProductsQuery.builder()
                .tenantId(tenantId)
                .productQuantities(defensiveCopy)
                .build();
    }
}
