package com.ccbsa.wms.stock.domain.core.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ccbsa.common.domain.TenantAwareAggregateRoot;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.WarehouseId;
import com.ccbsa.wms.stock.domain.core.event.StockConsignmentConfirmedEvent;
import com.ccbsa.wms.stock.domain.core.event.StockConsignmentReceivedEvent;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentId;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentLineItem;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentReference;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentStatus;

/**
 * Aggregate Root: StockConsignment
 * <p>
 * Represents a stock consignment received at the warehouse.
 * <p>
 * Business Rules: - Consignment reference must be unique per tenant - At least one line item is required - Status transitions: RECEIVED -> CONFIRMED -> CANCELLED - Cannot confirm
 * a cancelled consignment - Cannot cancel a confirmed
 * consignment
 */
public class StockConsignment
        extends TenantAwareAggregateRoot<ConsignmentId> {
    private ConsignmentReference consignmentReference;
    private WarehouseId warehouseId;
    private ConsignmentStatus status;
    private LocalDateTime receivedAt;
    private LocalDateTime confirmedAt;
    private String receivedBy;
    private List<ConsignmentLineItem> lineItems;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;

    /**
     * Private constructor for builder pattern. Prevents direct instantiation.
     */
    private StockConsignment() {
        this.lineItems = new ArrayList<>();
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
     * Business logic method: Confirms the consignment.
     * <p>
     * Business Rules: - Can only confirm received consignments - Sets status to CONFIRMED - Records confirmation timestamp - Publishes StockConsignmentConfirmedEvent
     *
     * @throws IllegalStateException if consignment is not in RECEIVED status
     */
    public void confirm() {
        if (this.status != ConsignmentStatus.RECEIVED) {
            throw new IllegalStateException(String.format("Cannot confirm consignment in status: %s. Only RECEIVED consignments can be confirmed.", this.status));
        }

        this.status = ConsignmentStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
        this.lastModifiedAt = LocalDateTime.now();

        // Publish domain event
        addDomainEvent(new StockConsignmentConfirmedEvent(this.getId()
                .getValueAsString(), this.consignmentReference, this.getTenantId(), this.warehouseId));
    }

    /**
     * Business logic method: Cancels the consignment.
     * <p>
     * Business Rules: - Can only cancel received consignments - Sets status to CANCELLED
     *
     * @throws IllegalStateException if consignment is not in RECEIVED status
     */
    public void cancel() {
        if (this.status != ConsignmentStatus.RECEIVED) {
            throw new IllegalStateException(String.format("Cannot cancel consignment in status: %s. Only RECEIVED consignments can be cancelled.", this.status));
        }

        this.status = ConsignmentStatus.CANCELLED;
        this.lastModifiedAt = LocalDateTime.now();
    }

    /**
     * Business logic method: Adds a line item to the consignment.
     * <p>
     * Business Rules: - Can only add line items to received consignments - Line item cannot be null
     *
     * @param lineItem Line item to add
     * @throws IllegalStateException    if consignment is not in RECEIVED status
     * @throws IllegalArgumentException if lineItem is null
     */
    public void addLineItem(ConsignmentLineItem lineItem) {
        if (this.status != ConsignmentStatus.RECEIVED) {
            throw new IllegalStateException(String.format("Cannot add line item to consignment in status: %s. Only RECEIVED consignments can be modified.", this.status));
        }
        if (lineItem == null) {
            throw new IllegalArgumentException("LineItem cannot be null");
        }

        this.lineItems.add(lineItem);
        this.lastModifiedAt = LocalDateTime.now();
    }

    /**
     * Query method: Checks if consignment can be confirmed.
     *
     * @return true if consignment can be confirmed
     */
    public boolean canConfirm() {
        return this.status == ConsignmentStatus.RECEIVED;
    }

    /**
     * Query method: Checks if consignment is in received status.
     *
     * @return true if consignment is received
     */
    public boolean isReceived() {
        return this.status == ConsignmentStatus.RECEIVED;
    }

    /**
     * Query method: Checks if consignment is confirmed.
     *
     * @return true if consignment is confirmed
     */
    public boolean isConfirmed() {
        return this.status == ConsignmentStatus.CONFIRMED;
    }

    /**
     * Query method: Checks if consignment is cancelled.
     *
     * @return true if consignment is cancelled
     */
    public boolean isCancelled() {
        return this.status == ConsignmentStatus.CANCELLED;
    }

    // Getters (read-only access)

    public ConsignmentReference getConsignmentReference() {
        return consignmentReference;
    }

    public WarehouseId getWarehouseId() {
        return warehouseId;
    }

    public ConsignmentStatus getStatus() {
        return status;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public String getReceivedBy() {
        return receivedBy;
    }

    public List<ConsignmentLineItem> getLineItems() {
        return Collections.unmodifiableList(lineItems);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    /**
     * Builder class for constructing StockConsignment instances. Ensures all required fields are set and validated.
     */
    public static class Builder {
        private StockConsignment consignment = new StockConsignment();

        public Builder consignmentId(ConsignmentId id) {
            consignment.setId(id);
            return this;
        }

        public Builder tenantId(TenantId tenantId) {
            consignment.setTenantId(tenantId);
            return this;
        }

        public Builder consignmentReference(ConsignmentReference reference) {
            consignment.consignmentReference = reference;
            return this;
        }

        public Builder warehouseId(WarehouseId warehouseId) {
            consignment.warehouseId = warehouseId;
            return this;
        }

        public Builder receivedAt(LocalDateTime receivedAt) {
            consignment.receivedAt = receivedAt;
            return this;
        }

        public Builder receivedBy(String receivedBy) {
            consignment.receivedBy = receivedBy;
            return this;
        }

        public Builder lineItems(List<ConsignmentLineItem> lineItems) {
            if (lineItems != null) {
                consignment.lineItems = new ArrayList<>(lineItems);
            }
            return this;
        }

        public Builder lineItem(ConsignmentLineItem lineItem) {
            if (lineItem != null) {
                consignment.lineItems.add(lineItem);
            }
            return this;
        }

        public Builder status(ConsignmentStatus status) {
            consignment.status = status;
            return this;
        }

        public Builder confirmedAt(LocalDateTime confirmedAt) {
            consignment.confirmedAt = confirmedAt;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            consignment.createdAt = createdAt;
            return this;
        }

        public Builder lastModifiedAt(LocalDateTime lastModifiedAt) {
            consignment.lastModifiedAt = lastModifiedAt;
            return this;
        }

        public Builder version(int version) {
            consignment.setVersion(version);
            return this;
        }

        /**
         * Builds and validates the StockConsignment instance.
         *
         * @return Validated StockConsignment instance
         * @throws IllegalArgumentException if validation fails
         */
        public StockConsignment build() {
            validate();
            initializeDefaults();

            if (consignment.createdAt == null) {
                consignment.createdAt = LocalDateTime.now();
            }
            if (consignment.lastModifiedAt == null) {
                consignment.lastModifiedAt = LocalDateTime.now();
            }

            // Publish creation event only if this is a new consignment (no version set)
            if (consignment.getVersion() == 0) {
                consignment.addDomainEvent(new StockConsignmentReceivedEvent(consignment.getId()
                        .getValueAsString(), consignment.consignmentReference, consignment.getTenantId(), consignment.warehouseId, consignment.lineItems));
            }

            return consumeConsignment();
        }

        /**
         * Validates all required fields are set.
         *
         * @throws IllegalArgumentException if validation fails
         */
        private void validate() {
            if (consignment.getId() == null) {
                throw new IllegalArgumentException("ConsignmentId is required");
            }
            if (consignment.getTenantId() == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            if (consignment.consignmentReference == null) {
                throw new IllegalArgumentException("ConsignmentReference is required");
            }
            if (consignment.warehouseId == null) {
                throw new IllegalArgumentException("WarehouseId is required");
            }
            if (consignment.receivedAt == null) {
                throw new IllegalArgumentException("ReceivedAt is required");
            }
            if (consignment.lineItems == null || consignment.lineItems.isEmpty()) {
                throw new IllegalArgumentException("At least one line item is required");
            }
        }

        /**
         * Initializes default values for optional fields.
         */
        private void initializeDefaults() {
            if (consignment.status == null) {
                consignment.status = ConsignmentStatus.RECEIVED;
            }
            if (consignment.receivedAt == null) {
                consignment.receivedAt = LocalDateTime.now();
            }
        }

        /**
         * Consumes the consignment from the builder and returns it. Creates a new consignment instance for the next build.
         *
         * @return Built consignment
         */
        private StockConsignment consumeConsignment() {
            StockConsignment builtConsignment = consignment;
            consignment = new StockConsignment();
            return builtConsignment;
        }

        /**
         * Builds StockConsignment without publishing creation event. Used when reconstructing from persistence.
         *
         * @return Validated StockConsignment instance
         * @throws IllegalArgumentException if validation fails
         */
        public StockConsignment buildWithoutEvents() {
            validate();
            initializeDefaults();

            // Set timestamps if not already set
            if (consignment.createdAt == null) {
                consignment.createdAt = LocalDateTime.now();
            }
            if (consignment.lastModifiedAt == null) {
                consignment.lastModifiedAt = LocalDateTime.now();
            }

            // Do not publish events when loading from database
            return consumeConsignment();
        }
    }
}

