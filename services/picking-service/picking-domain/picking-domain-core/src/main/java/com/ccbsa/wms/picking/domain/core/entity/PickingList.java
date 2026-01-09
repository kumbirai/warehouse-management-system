package com.ccbsa.wms.picking.domain.core.entity;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.ccbsa.common.domain.TenantAwareAggregateRoot;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.picking.domain.core.event.PickingCompletedEvent;
import com.ccbsa.wms.picking.domain.core.event.PickingListReceivedEvent;
import com.ccbsa.wms.picking.domain.core.valueobject.Notes;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListId;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListReference;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListStatus;

/**
 * Aggregate Root: PickingList
 * <p>
 * Represents a picking list containing multiple loads. A picking list is created from CSV upload or manual entry.
 * <p>
 * Business Rules:
 * - Picking list must contain at least one load
 * - Status transitions: RECEIVED -> PROCESSING -> PLANNED -> COMPLETED
 * - Cannot process a picking list that is already completed
 */
public class PickingList extends TenantAwareAggregateRoot<PickingListId> {
    private List<Load> loads;
    private PickingListStatus status;
    private ZonedDateTime receivedAt;
    private ZonedDateTime processedAt;
    private ZonedDateTime completedAt;
    private UserId completedByUserId;
    private Notes notes;
    private PickingListReference pickingListReference;

    /**
     * Private constructor for builder pattern. Prevents direct instantiation.
     */
    private PickingList() {
        this.loads = new ArrayList<>();
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
     * Business logic method: Creates a new picking list.
     * <p>
     * Business Rules:
     * - Sets status to RECEIVED
     * - Records received timestamp
     * - Publishes PickingListReceivedEvent
     * <p>
     * This method is idempotent - if the picking list is already in RECEIVED status, it returns without error.
     *
     * @throws IllegalStateException if picking list is in a status other than RECEIVED or null, or if loads are empty
     */
    public void create() {
        if (loads == null || loads.isEmpty()) {
            throw new IllegalStateException("Picking list must contain at least one load");
        }

        if (this.status == PickingListStatus.RECEIVED) {
            // Already created - idempotent operation
            if (this.receivedAt == null) {
                this.receivedAt = ZonedDateTime.now();
            }
            // Only publish event if not already published (check if domain events are empty)
            if (getDomainEvents().isEmpty()) {
                List<String> loadIds = loads.stream().map(load -> load.getId().getValueAsString()).toList();
                addDomainEvent(new PickingListReceivedEvent(this.getId().getValueAsString(), this.getTenantId(), loadIds));
            }
            return;
        }

        if (this.status != null) {
            throw new IllegalStateException(String.format("Picking list has already been created and is in status: %s", this.status));
        }

        this.status = PickingListStatus.RECEIVED;
        if (this.receivedAt == null) {
            this.receivedAt = ZonedDateTime.now();
        }

        // Publish domain event
        List<String> loadIds = loads.stream().map(load -> load.getId().getValueAsString()).toList();
        addDomainEvent(new PickingListReceivedEvent(this.getId().getValueAsString(), this.getTenantId(), loadIds));
    }

    /**
     * Business logic method: Marks the picking list as processed.
     * <p>
     * Business Rules:
     * - Can only process RECEIVED picking lists
     * - Sets status to PROCESSING
     * - Records processed timestamp
     *
     * @throws IllegalStateException if picking list is not in RECEIVED status
     */
    public void markAsProcessed() {
        if (this.status != PickingListStatus.RECEIVED) {
            throw new IllegalStateException(String.format("Cannot process picking list in status: %s. Only RECEIVED picking lists can be processed.", this.status));
        }

        this.status = PickingListStatus.PROCESSING;
        this.processedAt = ZonedDateTime.now();
    }

    /**
     * Business logic method: Marks the picking list as planned.
     * <p>
     * Business Rules:
     * - Can only mark as planned if in PROCESSING status
     * - Sets status to PLANNED
     *
     * @throws IllegalStateException if picking list is not in PROCESSING status
     */
    public void markAsPlanned() {
        if (this.status != PickingListStatus.PROCESSING) {
            throw new IllegalStateException(
                    String.format("Cannot mark picking list as planned in status: %s. Only PROCESSING picking lists can be marked as planned.", this.status));
        }

        this.status = PickingListStatus.PLANNED;
    }

    /**
     * Business logic method: Completes the picking list.
     * <p>
     * Business Rules:
     * - Can only complete PLANNED picking lists
     * - All picking tasks must be completed or partially completed (validated at application service layer)
     * - Sets status to COMPLETED
     * - Publishes PickingCompletedEvent
     *
     * @param completedByUserId User ID who completed the picking list
     * @throws IllegalStateException if picking list is not in PLANNED status
     */
    public void complete(UserId completedByUserId) {
        if (this.status != PickingListStatus.PLANNED) {
            throw new IllegalStateException(String.format("Cannot complete picking list in status: %s. Only PLANNED picking lists can be completed.", this.status));
        }

        this.status = PickingListStatus.COMPLETED;
        this.completedAt = ZonedDateTime.now();
        this.completedByUserId = completedByUserId;

        // Publish domain event
        List<String> loadIds = loads.stream().map(load -> load.getId().getValueAsString()).toList();
        addDomainEvent(new PickingCompletedEvent(this.getId().getValueAsString(), this.getTenantId(), loadIds, completedByUserId.getValue()));
    }

    /**
     * Query method: Gets the number of loads in the picking list.
     *
     * @return Load count
     */
    public int getLoadCount() {
        return loads.size();
    }

    /**
     * Query method: Gets the total number of orders across all loads.
     *
     * @return Total order count
     */
    public int getTotalOrderCount() {
        return loads.stream().mapToInt(Load::getOrderCount).sum();
    }

    // Getters

    public List<Load> getLoads() {
        return Collections.unmodifiableList(loads);
    }

    public PickingListStatus getStatus() {
        return status;
    }

    public ZonedDateTime getReceivedAt() {
        return receivedAt;
    }

    public ZonedDateTime getProcessedAt() {
        return processedAt;
    }

    public Notes getNotes() {
        return notes;
    }

    public PickingListReference getPickingListReference() {
        return pickingListReference;
    }

    public ZonedDateTime getCompletedAt() {
        return completedAt;
    }

    public UserId getCompletedByUserId() {
        return completedByUserId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        PickingList that = (PickingList) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getId());
    }

    @Override
    public String toString() {
        return String.format("PickingList{id=%s, reference=%s, status=%s, loadCount=%d}", getId(), pickingListReference != null ? pickingListReference.getValue() : "null", status,
                loads.size());
    }

    /**
     * Builder class for constructing PickingList instances.
     */
    public static class Builder {
        private PickingList pickingList = new PickingList();

        public Builder id(PickingListId id) {
            pickingList.setId(id);
            return this;
        }

        public Builder tenantId(TenantId tenantId) {
            pickingList.setTenantId(tenantId);
            return this;
        }

        public Builder loads(List<Load> loads) {
            if (loads != null) {
                pickingList.loads = new ArrayList<>(loads);
            }
            return this;
        }

        public Builder load(Load load) {
            if (load != null) {
                pickingList.loads.add(load);
            }
            return this;
        }

        public Builder status(PickingListStatus status) {
            pickingList.status = status;
            return this;
        }

        public Builder receivedAt(ZonedDateTime receivedAt) {
            pickingList.receivedAt = receivedAt;
            return this;
        }

        public Builder processedAt(ZonedDateTime processedAt) {
            pickingList.processedAt = processedAt;
            return this;
        }

        public Builder notes(Notes notes) {
            pickingList.notes = notes;
            return this;
        }

        public Builder notes(String notes) {
            pickingList.notes = Notes.ofNullable(notes);
            return this;
        }

        public Builder pickingListReference(PickingListReference pickingListReference) {
            pickingList.pickingListReference = pickingListReference;
            return this;
        }

        public Builder completedAt(ZonedDateTime completedAt) {
            pickingList.completedAt = completedAt;
            return this;
        }

        public Builder completedByUserId(UserId completedByUserId) {
            pickingList.completedByUserId = completedByUserId;
            return this;
        }

        public Builder version(int version) {
            pickingList.setVersion(version);
            return this;
        }

        /**
         * Builds and validates the PickingList instance.
         *
         * @return Validated PickingList instance
         * @throws IllegalArgumentException if validation fails
         */
        public PickingList build() {
            validate();
            initializeDefaults();

            if (pickingList.receivedAt == null) {
                pickingList.receivedAt = ZonedDateTime.now();
            }

            // Publish creation event only if this is a new picking list (no version set)
            if (pickingList.getVersion() == 0) {
                pickingList.create();
            }

            return consumePickingList();
        }

        /**
         * Validates all required fields are set.
         *
         * @throws IllegalArgumentException if validation fails
         */
        private void validate() {
            if (pickingList.getId() == null) {
                throw new IllegalArgumentException("PickingListId is required");
            }
            if (pickingList.getTenantId() == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            if (pickingList.loads == null || pickingList.loads.isEmpty()) {
                throw new IllegalArgumentException("At least one load is required");
            }
        }

        /**
         * Initializes default values for optional fields.
         */
        private void initializeDefaults() {
            if (pickingList.status == null) {
                pickingList.status = PickingListStatus.RECEIVED;
            }
        }

        /**
         * Consumes the picking list from the builder and returns it. Creates a new picking list instance for the next build.
         *
         * @return Built picking list
         */
        private PickingList consumePickingList() {
            PickingList builtPickingList = pickingList;
            pickingList = new PickingList();
            return builtPickingList;
        }

        /**
         * Builds PickingList without publishing creation event. Used when reconstructing from persistence.
         *
         * @return Validated PickingList instance
         * @throws IllegalArgumentException if validation fails
         */
        public PickingList buildWithoutEvents() {
            validate();
            initializeDefaults();

            if (pickingList.receivedAt == null) {
                pickingList.receivedAt = ZonedDateTime.now();
            }

            if (pickingList.status == null) {
                pickingList.status = PickingListStatus.RECEIVED;
            }

            return consumePickingList();
        }
    }
}
