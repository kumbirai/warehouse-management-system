package com.ccbsa.wms.picking.domain.core.entity;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.ccbsa.common.domain.TenantAwareAggregateRoot;
import com.ccbsa.common.domain.valueobject.LoadNumber;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.picking.domain.core.event.LoadPlannedEvent;
import com.ccbsa.wms.picking.domain.core.event.OrderMappedToLoadEvent;
import com.ccbsa.wms.picking.domain.core.valueobject.LoadId;
import com.ccbsa.wms.picking.domain.core.valueobject.LoadStatus;

/**
 * Aggregate Root: Load
 * <p>
 * Represents a load containing multiple orders. A load is a grouping of orders that will be picked together.
 * <p>
 * Business Rules:
 * - Load must have a load number
 * - Load can contain multiple orders
 * - Load can contain multiple orders per customer
 * - Load status transitions: CREATED -> PLANNED -> IN_PROGRESS -> COMPLETED
 * - Cannot plan a load that is already completed
 */
public class Load extends TenantAwareAggregateRoot<LoadId> {
    private LoadNumber loadNumber;
    private List<Order> orders;
    private LoadStatus status;
    private ZonedDateTime createdAt;
    private ZonedDateTime plannedAt;

    /**
     * Private constructor for builder pattern. Prevents direct instantiation.
     */
    private Load() {
        this.orders = new ArrayList<>();
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
     * Business logic method: Creates a new load.
     * <p>
     * Sets status to CREATED and records creation timestamp.
     * <p>
     * This method is idempotent - if the load is already in CREATED status, it returns without error.
     *
     * @throws IllegalStateException if load is in a status other than CREATED or null
     */
    public void create() {
        if (this.status == LoadStatus.CREATED) {
            // Already created - idempotent operation
            if (this.createdAt == null) {
                this.createdAt = ZonedDateTime.now();
            }
            return;
        }
        if (this.status != null) {
            throw new IllegalStateException(String.format("Load has already been created and is in status: %s", this.status));
        }
        this.status = LoadStatus.CREATED;
        if (this.createdAt == null) {
            this.createdAt = ZonedDateTime.now();
        }
    }

    /**
     * Business logic method: Plans picking locations for the load.
     * <p>
     * Business Rules:
     * - Can only plan loads in CREATED status
     * - Sets status to PLANNED
     * - Records planning timestamp
     * - Publishes LoadPlannedEvent
     *
     * @param pickingTaskIds List of picking task IDs created during planning
     * @throws IllegalStateException if load is not in CREATED status
     */
    public void plan(List<String> pickingTaskIds) {
        plan(pickingTaskIds, null);
    }

    /**
     * Business logic method: Plans picking locations for the load.
     * <p>
     * Business Rules:
     * - Can only plan loads in CREATED status
     * - Sets status to PLANNED
     * - Records planning timestamp
     * - Publishes LoadPlannedEvent
     *
     * @param pickingTaskIds List of picking task IDs created during planning
     * @param pickingListId  Optional picking list ID (as String) to include in event
     * @throws IllegalStateException if load is not in CREATED status
     */
    public void plan(List<String> pickingTaskIds, String pickingListId) {
        if (this.status != LoadStatus.CREATED) {
            throw new IllegalStateException(String.format("Cannot plan load in status: %s. Only CREATED loads can be planned.", this.status));
        }

        this.status = LoadStatus.PLANNED;
        this.plannedAt = ZonedDateTime.now();

        // Publish domain event
        addDomainEvent(new LoadPlannedEvent(this.getId().getValueAsString(), this.getTenantId(), pickingListId,
                pickingTaskIds != null ? pickingTaskIds : List.of()));
    }

    /**
     * Business logic method: Adds an order to the load.
     * <p>
     * Business Rules:
     * - Can only add orders to CREATED loads
     * - Order cannot be null
     * - Publishes OrderMappedToLoadEvent
     *
     * @param order Order to add
     * @throws IllegalStateException    if load is not in CREATED status
     * @throws IllegalArgumentException if order is null
     */
    public void addOrder(Order order) {
        if (this.status != LoadStatus.CREATED) {
            throw new IllegalStateException(String.format("Cannot add order to load in status: %s. Only CREATED loads can be modified.", this.status));
        }
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        this.orders.add(order);

        // Publish domain event
        addDomainEvent(new OrderMappedToLoadEvent(order.getId().getValueAsString(), this.getTenantId(), this.getId().getValueAsString(), order.getOrderNumber().getValue()));
    }

    /**
     * Query method: Gets the number of orders in the load.
     *
     * @return Order count
     */
    public int getOrderCount() {
        return orders.size();
    }

    /**
     * Query method: Checks if load can be planned.
     *
     * @return true if load can be planned
     */
    public boolean canPlan() {
        return this.status == LoadStatus.CREATED && !orders.isEmpty();
    }

    // Getters

    public LoadNumber getLoadNumber() {
        return loadNumber;
    }

    public List<Order> getOrders() {
        return Collections.unmodifiableList(orders);
    }

    public LoadStatus getStatus() {
        return status;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public ZonedDateTime getPlannedAt() {
        return plannedAt;
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
        Load load = (Load) o;
        return Objects.equals(getId(), load.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getId());
    }

    @Override
    public String toString() {
        return String.format("Load{id=%s, loadNumber=%s, status=%s, orderCount=%d}", getId(), loadNumber, status, orders.size());
    }

    /**
     * Builder class for constructing Load instances.
     */
    public static class Builder {
        private Load load = new Load();

        public Builder id(LoadId id) {
            load.setId(id);
            return this;
        }

        public Builder tenantId(TenantId tenantId) {
            load.setTenantId(tenantId);
            return this;
        }

        public Builder loadNumber(LoadNumber loadNumber) {
            load.loadNumber = loadNumber;
            return this;
        }

        public Builder orders(List<Order> orders) {
            if (orders != null) {
                load.orders = new ArrayList<>(orders);
            }
            return this;
        }

        public Builder order(Order order) {
            if (order != null) {
                load.orders.add(order);
            }
            return this;
        }

        public Builder status(LoadStatus status) {
            load.status = status;
            return this;
        }

        public Builder createdAt(ZonedDateTime createdAt) {
            load.createdAt = createdAt;
            return this;
        }

        public Builder plannedAt(ZonedDateTime plannedAt) {
            load.plannedAt = plannedAt;
            return this;
        }

        public Builder version(int version) {
            load.setVersion(version);
            return this;
        }

        /**
         * Builds and validates the Load instance.
         *
         * @return Validated Load instance
         * @throws IllegalArgumentException if validation fails
         */
        public Load build() {
            validate();
            initializeDefaults();

            if (load.createdAt == null) {
                load.createdAt = ZonedDateTime.now();
            }

            // Publish creation event only if this is a new load (no version set)
            if (load.getVersion() == 0) {
                load.create();
            }

            return consumeLoad();
        }

        /**
         * Validates all required fields are set.
         *
         * @throws IllegalArgumentException if validation fails
         */
        private void validate() {
            if (load.getId() == null) {
                throw new IllegalArgumentException("LoadId is required");
            }
            if (load.getTenantId() == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            if (load.loadNumber == null) {
                throw new IllegalArgumentException("LoadNumber is required");
            }
        }

        /**
         * Initializes default values for optional fields.
         */
        private void initializeDefaults() {
            if (load.status == null) {
                load.status = LoadStatus.CREATED;
            }
        }

        /**
         * Consumes the load from the builder and returns it. Creates a new load instance for the next build.
         *
         * @return Built load
         */
        private Load consumeLoad() {
            Load builtLoad = load;
            load = new Load();
            return builtLoad;
        }

        /**
         * Builds Load without publishing creation event. Used when reconstructing from persistence.
         *
         * @return Validated Load instance
         * @throws IllegalArgumentException if validation fails
         */
        public Load buildWithoutEvents() {
            validate();
            initializeDefaults();

            if (load.createdAt == null) {
                load.createdAt = ZonedDateTime.now();
            }

            if (load.status == null) {
                load.status = LoadStatus.CREATED;
            }

            return consumeLoad();
        }
    }
}
