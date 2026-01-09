package com.ccbsa.wms.stock.domain.core.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.ccbsa.common.domain.TenantAwareAggregateRoot;
import com.ccbsa.common.domain.valueobject.BigDecimalQuantity;
import com.ccbsa.common.domain.valueobject.MaximumQuantity;
import com.ccbsa.common.domain.valueobject.MinimumQuantity;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.RestockPriority;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.domain.core.event.RestockRequestGeneratedEvent;
import com.ccbsa.wms.stock.domain.core.valueobject.RestockRequestId;
import com.ccbsa.wms.stock.domain.core.valueobject.RestockRequestStatus;

/**
 * Aggregate Root: RestockRequest
 * <p>
 * Represents a request to restock inventory when stock levels fall below minimum threshold.
 * <p>
 * Business Rules:
 * - Restock request is created when stock falls below minimum
 * - Priority is calculated based on current quantity vs minimum/maximum
 * - Only one active restock request per product (deduplication)
 * - Can be sent to Microsoft Dynamics 365 for fulfillment
 * - Requested quantity is calculated as (maximum - current) or a standard restock amount
 */
public class RestockRequest extends TenantAwareAggregateRoot<RestockRequestId> {

    private ProductId productId;
    private LocationId locationId; // Optional - may be null for product-level restock
    private BigDecimalQuantity currentQuantity;
    private MinimumQuantity minimumQuantity;
    private MaximumQuantity maximumQuantity;
    private BigDecimalQuantity requestedQuantity;
    private RestockPriority priority;
    private RestockRequestStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime sentToD365At;
    private String d365OrderReference;

    /**
     * Private constructor for builder pattern. Prevents direct instantiation.
     */
    private RestockRequest() {
    }

    /**
     * Factory method to create builder instance.
     *
     * @return Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Business logic method: Calculates restock priority based on current quantity.
     * <p>
     * Priority Rules:
     * - HIGH: Current quantity is less than 50% of minimum
     * - MEDIUM: Current quantity is between 50% and 100% of minimum
     * - LOW: Current quantity is at or above minimum but below maximum
     *
     * @param currentQuantity Current stock quantity
     * @param minimumQuantity Minimum threshold quantity
     * @return Calculated priority
     */
    public static RestockPriority calculatePriority(BigDecimalQuantity currentQuantity, MinimumQuantity minimumQuantity) {
        if (currentQuantity == null || minimumQuantity == null) {
            return RestockPriority.MEDIUM; // Default priority
        }

        BigDecimal current = currentQuantity.getValue();
        BigDecimal minimum = minimumQuantity.getValue();

        // Calculate percentage of minimum
        BigDecimal percentage = current.divide(minimum, 2, java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

        if (percentage.compareTo(BigDecimal.valueOf(50)) < 0) {
            return RestockPriority.HIGH;
        } else if (percentage.compareTo(BigDecimal.valueOf(100)) < 0) {
            return RestockPriority.MEDIUM;
        } else {
            return RestockPriority.LOW;
        }
    }

    /**
     * Business logic method: Calculates requested quantity for restock.
     * <p>
     * Calculation: requestedQuantity = maximumQuantity - currentQuantity
     * If maximum is not set, uses a standard restock amount (2x minimum)
     *
     * @param currentQuantity Current stock quantity
     * @param minimumQuantity Minimum threshold quantity
     * @param maximumQuantity Maximum threshold quantity (optional)
     * @return Calculated requested quantity
     */
    public static BigDecimalQuantity calculateRequestedQuantity(BigDecimalQuantity currentQuantity, MinimumQuantity minimumQuantity, MaximumQuantity maximumQuantity) {
        if (maximumQuantity != null) {
            BigDecimal requested = maximumQuantity.getValue().subtract(currentQuantity.getValue());
            return BigDecimalQuantity.of(requested.max(BigDecimal.ZERO));
        } else {
            // Default: restock to 2x minimum
            BigDecimal requested = minimumQuantity.getValue().multiply(BigDecimal.valueOf(2)).subtract(currentQuantity.getValue());
            return BigDecimalQuantity.of(requested.max(BigDecimal.ZERO));
        }
    }

    /**
     * Business logic method: Marks the restock request as sent to D365.
     *
     * @param d365OrderReference D365 order reference
     */
    public void markAsSentToD365(String d365OrderReference) {
        if (this.status != RestockRequestStatus.PENDING) {
            throw new IllegalStateException("Cannot mark restock request as sent to D365 in status: " + this.status);
        }
        this.status = RestockRequestStatus.SENT_TO_D365;
        this.sentToD365At = LocalDateTime.now();
        this.d365OrderReference = d365OrderReference;
    }

    /**
     * Business logic method: Marks the restock request as fulfilled.
     */
    public void markAsFulfilled() {
        if (this.status == RestockRequestStatus.FULFILLED) {
            return; // Idempotent
        }
        if (this.status == RestockRequestStatus.CANCELLED) {
            throw new IllegalStateException("Cannot fulfill a cancelled restock request");
        }
        this.status = RestockRequestStatus.FULFILLED;
    }

    /**
     * Business logic method: Cancels the restock request.
     */
    public void cancel() {
        if (this.status == RestockRequestStatus.FULFILLED) {
            throw new IllegalStateException("Cannot cancel a fulfilled restock request");
        }
        this.status = RestockRequestStatus.CANCELLED;
    }

    /**
     * Query method: Checks if restock request is active.
     *
     * @return true if status is PENDING or SENT_TO_D365
     */
    public boolean isActive() {
        return this.status == RestockRequestStatus.PENDING || this.status == RestockRequestStatus.SENT_TO_D365;
    }

    // Getters

    public ProductId getProductId() {
        return productId;
    }

    public LocationId getLocationId() {
        return locationId;
    }

    public BigDecimalQuantity getCurrentQuantity() {
        return currentQuantity;
    }

    public MinimumQuantity getMinimumQuantity() {
        return minimumQuantity;
    }

    public MaximumQuantity getMaximumQuantity() {
        return maximumQuantity;
    }

    public BigDecimalQuantity getRequestedQuantity() {
        return requestedQuantity;
    }

    public RestockPriority getPriority() {
        return priority;
    }

    public RestockRequestStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getSentToD365At() {
        return sentToD365At;
    }

    public String getD365OrderReference() {
        return d365OrderReference;
    }

    /**
     * Builder class for constructing RestockRequest instances.
     */
    public static class Builder {
        private RestockRequest restockRequest = new RestockRequest();

        public Builder restockRequestId(RestockRequestId id) {
            restockRequest.setId(id);
            return this;
        }

        public Builder tenantId(TenantId tenantId) {
            restockRequest.setTenantId(tenantId);
            return this;
        }

        public Builder productId(ProductId productId) {
            restockRequest.productId = productId;
            return this;
        }

        public Builder locationId(LocationId locationId) {
            restockRequest.locationId = locationId;
            return this;
        }

        public Builder currentQuantity(BigDecimalQuantity currentQuantity) {
            restockRequest.currentQuantity = currentQuantity;
            return this;
        }

        public Builder minimumQuantity(MinimumQuantity minimumQuantity) {
            restockRequest.minimumQuantity = minimumQuantity;
            return this;
        }

        public Builder maximumQuantity(MaximumQuantity maximumQuantity) {
            restockRequest.maximumQuantity = maximumQuantity;
            return this;
        }

        public Builder requestedQuantity(BigDecimalQuantity requestedQuantity) {
            restockRequest.requestedQuantity = requestedQuantity;
            return this;
        }

        public Builder priority(RestockPriority priority) {
            restockRequest.priority = priority;
            return this;
        }

        public Builder status(RestockRequestStatus status) {
            restockRequest.status = status;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            restockRequest.createdAt = createdAt;
            return this;
        }

        public Builder sentToD365At(LocalDateTime sentToD365At) {
            restockRequest.sentToD365At = sentToD365At;
            return this;
        }

        public Builder d365OrderReference(String d365OrderReference) {
            restockRequest.d365OrderReference = d365OrderReference;
            return this;
        }

        public Builder version(int version) {
            restockRequest.setVersion(version);
            return this;
        }

        /**
         * Builds and validates the RestockRequest instance.
         *
         * @return Validated RestockRequest instance
         * @throws IllegalArgumentException if validation fails
         */
        public RestockRequest build() {
            validate();
            initializeDefaults();

            if (restockRequest.createdAt == null) {
                restockRequest.createdAt = LocalDateTime.now();
            }

            // Publish creation event only if this is a new restock request (no version set)
            if (restockRequest.getVersion() == 0) {
                restockRequest.addDomainEvent(new RestockRequestGeneratedEvent(restockRequest.getId().getValueAsString(), restockRequest.getTenantId(), restockRequest.productId,
                        restockRequest.locationId, restockRequest.currentQuantity, restockRequest.minimumQuantity, restockRequest.maximumQuantity, restockRequest.requestedQuantity,
                        restockRequest.priority));
            }

            return consumeRestockRequest();
        }

        /**
         * Validates all required fields are set.
         *
         * @throws IllegalArgumentException if validation fails
         */
        private void validate() {
            if (restockRequest.getId() == null) {
                throw new IllegalArgumentException("RestockRequestId is required");
            }
            if (restockRequest.getTenantId() == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            if (restockRequest.productId == null) {
                throw new IllegalArgumentException("ProductId is required");
            }
            if (restockRequest.currentQuantity == null) {
                throw new IllegalArgumentException("CurrentQuantity is required");
            }
            if (restockRequest.minimumQuantity == null) {
                throw new IllegalArgumentException("MinimumQuantity is required");
            }
            if (restockRequest.requestedQuantity == null) {
                throw new IllegalArgumentException("RequestedQuantity is required");
            }
            if (restockRequest.priority == null) {
                throw new IllegalArgumentException("Priority is required");
            }
        }

        /**
         * Initializes default values for optional fields.
         */
        private void initializeDefaults() {
            if (restockRequest.status == null) {
                restockRequest.status = RestockRequestStatus.PENDING;
            }
        }

        /**
         * Consumes the restock request from the builder and returns it. Creates a new restock request instance for the next build.
         *
         * @return Built restock request
         */
        private RestockRequest consumeRestockRequest() {
            RestockRequest builtRequest = restockRequest;
            restockRequest = new RestockRequest();
            return builtRequest;
        }

        /**
         * Builds RestockRequest without publishing creation event. Used when reconstructing from persistence.
         *
         * @return Validated RestockRequest instance
         * @throws IllegalArgumentException if validation fails
         */
        public RestockRequest buildWithoutEvents() {
            validate();
            initializeDefaults();

            if (restockRequest.createdAt == null) {
                restockRequest.createdAt = LocalDateTime.now();
            }

            restockRequest.clearDomainEvents();

            return consumeRestockRequest();
        }
    }
}
