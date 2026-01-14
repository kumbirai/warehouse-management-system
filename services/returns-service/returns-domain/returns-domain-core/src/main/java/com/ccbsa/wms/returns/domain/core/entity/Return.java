package com.ccbsa.wms.returns.domain.core.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ccbsa.common.domain.TenantAwareAggregateRoot;
import com.ccbsa.common.domain.valueobject.CustomerInfo;
import com.ccbsa.common.domain.valueobject.Notes;
import com.ccbsa.common.domain.valueobject.OrderNumber;
import com.ccbsa.common.domain.valueobject.ReturnReason;
import com.ccbsa.common.domain.valueobject.ReturnStatus;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.returns.domain.core.event.ReturnInitiatedEvent;
import com.ccbsa.wms.returns.domain.core.event.ReturnProcessedEvent;
import com.ccbsa.wms.returns.domain.core.valueobject.CustomerSignature;
import com.ccbsa.common.domain.valueobject.ReturnId;
import com.ccbsa.wms.returns.domain.core.valueobject.ReturnType;

/**
 * Aggregate Root: Return
 * <p>
 * Represents a return order (partial or full).
 * <p>
 * Business Rules:
 * - Return must have at least one line item
 * - Accepted quantity ≤ ordered quantity for partial returns
 * - Return status transitions: INITIATED → PROCESSED → LOCATION_ASSIGNED → RECONCILED
 * - Customer signature required for partial returns
 * - Product condition required for full returns
 */
public class Return extends TenantAwareAggregateRoot<ReturnId> {
    private OrderNumber orderNumber;
    private ReturnType returnType;
    private ReturnStatus status;
    private List<ReturnLineItem> lineItems;
    private CustomerSignature customerSignature;
    private ReturnReason primaryReturnReason;
    private Notes returnNotes;
    private LocalDateTime returnedAt;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;

    /**
     * Private constructor for builder pattern. Prevents direct instantiation.
     */
    private Return() {
        this.lineItems = new ArrayList<>();
    }

    /**
     * Factory method to initiate a partial return.
     *
     * @param returnId          Return ID
     * @param orderNumber       Order number
     * @param tenantId          Tenant ID
     * @param lineItems         List of return line items
     * @param customerSignature Customer signature (required for partial returns)
     * @return Return aggregate instance
     * @throws IllegalArgumentException if validation fails
     */
    public static Return initiatePartialReturn(ReturnId returnId, OrderNumber orderNumber, TenantId tenantId, List<ReturnLineItem> lineItems, CustomerSignature customerSignature) {
        if (returnId == null) {
            throw new IllegalArgumentException("Return ID cannot be null");
        }
        if (orderNumber == null) {
            throw new IllegalArgumentException("Order number cannot be null");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }
        if (lineItems == null || lineItems.isEmpty()) {
            throw new IllegalArgumentException("Return must have at least one line item");
        }
        if (customerSignature == null) {
            throw new IllegalArgumentException("Customer signature is required for partial returns");
        }

        // Validate that it's truly a partial return (has both accepted and returned items)
        boolean hasAccepted = lineItems.stream().anyMatch(line -> line.getAcceptedQuantity().isPositive());
        boolean hasReturned = lineItems.stream().anyMatch(line -> line.getReturnedQuantity().isPositive());

        if (!hasAccepted || !hasReturned) {
            throw new IllegalArgumentException("Partial return must have both accepted and returned items");
        }

        LocalDateTime now = LocalDateTime.now();
        Return returnAggregate = Return.builder().returnId(returnId).orderNumber(orderNumber).tenantId(tenantId).returnType(ReturnType.PARTIAL).status(ReturnStatus.INITIATED)
                .lineItems(new ArrayList<>(lineItems)).customerSignature(customerSignature).returnedAt(now).createdAt(now).lastModifiedAt(now).build();

        // Publish domain event
        returnAggregate.addDomainEvent(new ReturnInitiatedEvent(returnAggregate.getId().getValueAsString(), orderNumber, tenantId, ReturnType.PARTIAL, returnAggregate.lineItems));

        return returnAggregate;
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
     * Factory method to process a full return.
     *
     * @param returnId            Return ID
     * @param orderNumber         Order number
     * @param tenantId            Tenant ID
     * @param lineItems           List of return line items (all must have product condition)
     * @param primaryReturnReason Primary return reason
     * @param returnNotes         Optional return notes
     * @return Return aggregate instance
     * @throws IllegalArgumentException if validation fails
     */
    public static Return processFullReturn(ReturnId returnId, OrderNumber orderNumber, TenantId tenantId, List<ReturnLineItem> lineItems, ReturnReason primaryReturnReason,
                                           Notes returnNotes) {
        if (returnId == null) {
            throw new IllegalArgumentException("Return ID cannot be null");
        }
        if (orderNumber == null) {
            throw new IllegalArgumentException("Order number cannot be null");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }
        if (lineItems == null || lineItems.isEmpty()) {
            throw new IllegalArgumentException("Return must have at least one line item");
        }
        if (primaryReturnReason == null) {
            throw new IllegalArgumentException("Primary return reason is required for full returns");
        }

        // Validate all items are being returned (no accepted items)
        boolean hasAccepted = lineItems.stream().anyMatch(line -> line.getAcceptedQuantity().isPositive());
        if (hasAccepted) {
            throw new IllegalArgumentException("Full return cannot have accepted items");
        }

        // Validate all items have product condition
        boolean allHaveCondition = lineItems.stream().allMatch(line -> line.getProductCondition() != null);
        if (!allHaveCondition) {
            throw new IllegalArgumentException("All line items must have product condition for full returns");
        }

        LocalDateTime now = LocalDateTime.now();
        Notes notes = returnNotes != null ? returnNotes : Notes.of(null);
        Return returnAggregate = Return.builder().returnId(returnId).orderNumber(orderNumber).tenantId(tenantId).returnType(ReturnType.FULL).status(ReturnStatus.PROCESSED)
                .lineItems(new ArrayList<>(lineItems)).primaryReturnReason(primaryReturnReason).returnNotes(notes).returnedAt(now).createdAt(now).lastModifiedAt(now).build();

        // Publish domain event
        returnAggregate.addDomainEvent(new ReturnProcessedEvent(returnAggregate.getId().getValueAsString(), orderNumber, tenantId));

        return returnAggregate;
    }

    /**
     * Business logic method: Marks return as processed.
     * <p>
     * Business Rules:
     * - Can only process returns in INITIATED status
     * - Sets status to PROCESSED
     *
     * @throws IllegalStateException if return is not in INITIATED status
     */
    public void markAsProcessed() {
        if (this.status != ReturnStatus.INITIATED) {
            throw new IllegalStateException(String.format("Cannot process return in status: %s. Only INITIATED returns can be processed.", this.status));
        }

        this.status = ReturnStatus.PROCESSED;
        this.lastModifiedAt = LocalDateTime.now();
        incrementVersion();

        // Publish domain event
        addDomainEvent(new ReturnProcessedEvent(this.getId().getValueAsString(), this.orderNumber, this.getTenantId()));
    }

    /**
     * Business logic method: Adds a line item to the return.
     * <p>
     * Business Rules:
     * - Can only add line items to INITIATED returns
     * - Line item cannot be null
     *
     * @param lineItem Line item to add
     * @throws IllegalStateException    if return is not in INITIATED status
     * @throws IllegalArgumentException if lineItem is null
     */
    public void addLineItem(ReturnLineItem lineItem) {
        if (this.status != ReturnStatus.INITIATED) {
            throw new IllegalStateException(String.format("Cannot add line item to return in status: %s. Only INITIATED returns can be modified.", this.status));
        }
        if (lineItem == null) {
            throw new IllegalArgumentException("LineItem cannot be null");
        }

        this.lineItems.add(lineItem);
        this.lastModifiedAt = LocalDateTime.now();
        incrementVersion();
    }

    // Getters (read-only access)

    public OrderNumber getOrderNumber() {
        return orderNumber;
    }

    public ReturnType getReturnType() {
        return returnType;
    }

    public ReturnStatus getStatus() {
        return status;
    }

    public List<ReturnLineItem> getLineItems() {
        return Collections.unmodifiableList(lineItems);
    }

    public CustomerSignature getCustomerSignature() {
        return customerSignature;
    }

    public ReturnReason getPrimaryReturnReason() {
        return primaryReturnReason;
    }

    public Notes getReturnNotes() {
        return returnNotes;
    }

    public LocalDateTime getReturnedAt() {
        return returnedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    /**
     * Builder class for constructing Return instances. Ensures all required fields are set and validated.
     */
    public static class Builder {
        private Return returnAggregate = new Return();

        public Builder returnId(ReturnId id) {
            returnAggregate.setId(id);
            return this;
        }

        public Builder tenantId(TenantId tenantId) {
            returnAggregate.setTenantId(tenantId);
            return this;
        }

        public Builder orderNumber(OrderNumber orderNumber) {
            returnAggregate.orderNumber = orderNumber;
            return this;
        }

        public Builder returnType(ReturnType returnType) {
            returnAggregate.returnType = returnType;
            return this;
        }

        public Builder status(ReturnStatus status) {
            returnAggregate.status = status;
            return this;
        }

        public Builder lineItems(List<ReturnLineItem> lineItems) {
            if (lineItems != null) {
                returnAggregate.lineItems = new ArrayList<>(lineItems);
            }
            return this;
        }

        public Builder customerSignature(CustomerSignature customerSignature) {
            returnAggregate.customerSignature = customerSignature;
            return this;
        }

        public Builder primaryReturnReason(ReturnReason primaryReturnReason) {
            returnAggregate.primaryReturnReason = primaryReturnReason;
            return this;
        }

        public Builder returnNotes(Notes returnNotes) {
            returnAggregate.returnNotes = returnNotes;
            return this;
        }

        public Builder returnedAt(LocalDateTime returnedAt) {
            returnAggregate.returnedAt = returnedAt;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            returnAggregate.createdAt = createdAt;
            return this;
        }

        public Builder lastModifiedAt(LocalDateTime lastModifiedAt) {
            returnAggregate.lastModifiedAt = lastModifiedAt;
            return this;
        }

        public Builder version(int version) {
            returnAggregate.setVersion(version);
            return this;
        }

        /**
         * Builds and validates the Return instance.
         *
         * @return Validated Return instance
         * @throws IllegalArgumentException if validation fails
         */
        public Return build() {
            validate();
            initializeDefaults();
            return consumeReturn();
        }

        /**
         * Validates all required fields are set.
         *
         * @throws IllegalArgumentException if validation fails
         */
        private void validate() {
            if (returnAggregate.getId() == null) {
                throw new IllegalArgumentException("ReturnId is required");
            }
            if (returnAggregate.getTenantId() == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            if (returnAggregate.orderNumber == null) {
                throw new IllegalArgumentException("OrderNumber is required");
            }
            if (returnAggregate.returnType == null) {
                throw new IllegalArgumentException("ReturnType is required");
            }
            if (returnAggregate.status == null) {
                throw new IllegalArgumentException("ReturnStatus is required");
            }
            if (returnAggregate.lineItems == null || returnAggregate.lineItems.isEmpty()) {
                throw new IllegalArgumentException("At least one line item is required");
            }
        }

        /**
         * Initializes default values for optional fields.
         */
        private void initializeDefaults() {
            if (returnAggregate.createdAt == null) {
                returnAggregate.createdAt = LocalDateTime.now();
            }
            if (returnAggregate.lastModifiedAt == null) {
                returnAggregate.lastModifiedAt = LocalDateTime.now();
            }
            if (returnAggregate.returnedAt == null) {
                returnAggregate.returnedAt = LocalDateTime.now();
            }
        }

        /**
         * Consumes the return from the builder and returns it. Creates a new return instance for the next build.
         *
         * @return Built return
         */
        private Return consumeReturn() {
            Return builtReturn = returnAggregate;
            returnAggregate = new Return();
            return builtReturn;
        }

        /**
         * Builds Return without publishing creation event. Used when reconstructing from persistence.
         *
         * @return Validated Return instance
         * @throws IllegalArgumentException if validation fails
         */
        public Return buildWithoutEvents() {
            validate();
            initializeDefaults();
            return consumeReturn();
        }
    }
}
