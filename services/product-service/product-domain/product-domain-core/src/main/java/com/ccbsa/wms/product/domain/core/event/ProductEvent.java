package com.ccbsa.wms.product.domain.core.event;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.ProductId;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Base class for all Product domain events.
 * <p>
 * All product-specific events extend this class.
 */
public abstract class ProductEvent extends DomainEvent<ProductId> {
    /**
     * Constructor for Product events without metadata.
     *
     * @param aggregateId Aggregate identifier (ProductId)
     * @throws IllegalArgumentException if aggregateId is null
     */
    @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW", justification = "Product events must fail fast if aggregate identifiers are invalid")
    protected ProductEvent(ProductId aggregateId) {
        super(extractProductIdString(aggregateId), "Product");
    }

    /**
     * Extracts the ProductId string value from the aggregate identifier.
     *
     * @param productId Product identifier
     * @return String representation of product ID
     */
    private static String extractProductIdString(ProductId productId) {
        if (productId == null) {
            throw new IllegalArgumentException("ProductId cannot be null");
        }
        return productId.getValue().toString();
    }

    /**
     * Constructor for Product events with metadata.
     *
     * @param aggregateId Aggregate identifier (ProductId)
     * @param metadata    Event metadata for traceability
     * @throws IllegalArgumentException if aggregateId is null
     */
    @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW", justification = "Product events must fail fast if aggregate identifiers are invalid")
    protected ProductEvent(ProductId aggregateId, EventMetadata metadata) {
        super(extractProductIdString(aggregateId), "Product", metadata);
    }
}

