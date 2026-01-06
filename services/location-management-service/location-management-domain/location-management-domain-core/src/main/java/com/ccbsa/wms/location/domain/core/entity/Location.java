package com.ccbsa.wms.location.domain.core.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.ccbsa.common.domain.TenantAwareAggregateRoot;
import com.ccbsa.common.domain.valueobject.BigDecimalQuantity;
import com.ccbsa.common.domain.valueobject.Description;
import com.ccbsa.common.domain.valueobject.StockItemId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.location.domain.core.event.LocationAssignedEvent;
import com.ccbsa.wms.location.domain.core.event.LocationBlockedEvent;
import com.ccbsa.wms.location.domain.core.event.LocationCreatedEvent;
import com.ccbsa.wms.location.domain.core.event.LocationStatusChangedEvent;
import com.ccbsa.wms.location.domain.core.event.LocationUnblockedEvent;
import com.ccbsa.wms.location.domain.core.valueobject.LocationBarcode;
import com.ccbsa.wms.location.domain.core.valueobject.LocationCapacity;
import com.ccbsa.wms.location.domain.core.valueobject.LocationCode;
import com.ccbsa.wms.location.domain.core.valueobject.LocationCoordinates;
import com.ccbsa.wms.location.domain.core.valueobject.LocationDescription;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationName;
import com.ccbsa.wms.location.domain.core.valueobject.LocationStatus;
import com.ccbsa.wms.location.domain.core.valueobject.LocationType;

/**
 * Aggregate Root: Location
 * <p>
 * Represents a warehouse location with barcode support.
 * <p>
 * Business Rules: - Locations are tenant-aware - Each location has a unique barcode per tenant - Locations have coordinates (zone, aisle, rack, level) - Locations have a status
 * (AVAILABLE, OCCUPIED, RESERVED, BLOCKED) - Locations can have
 * capacity constraints - Barcodes can be auto-generated from coordinates or manually provided
 */
public class Location extends TenantAwareAggregateRoot<LocationId> {

    // Value Objects
    private LocationBarcode barcode;
    private LocationCoordinates coordinates;
    private LocationStatus status;
    private LocationCapacity capacity;
    private LocationCode code; // Original location code (e.g., "WH-53")
    private LocationName name; // Location name
    private LocationType type; // Location type (WAREHOUSE, ZONE, AISLE, RACK, BIN)
    private LocationDescription description;
    private LocationId parentLocationId; // Parent location ID for hierarchical relationships (null for warehouses)
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;

    /**
     * Private constructor for builder pattern. Prevents direct instantiation.
     */
    private Location() {
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
     * Business logic method: Updates the location barcode.
     * <p>
     * Business Rules: - Barcode can be updated if location is AVAILABLE or OCCUPIED - New barcode must be unique per tenant (validated at application service layer)
     *
     * @param newBarcode New barcode value
     * @throws IllegalStateException    if location status prevents barcode update
     * @throws IllegalArgumentException if newBarcode is null
     */
    public void updateBarcode(LocationBarcode newBarcode) {
        if (newBarcode == null) {
            throw new IllegalArgumentException("LocationBarcode cannot be null");
        }
        if (status == LocationStatus.BLOCKED) {
            throw new IllegalStateException("Cannot update barcode for BLOCKED location");
        }

        this.barcode = newBarcode;
        this.lastModifiedAt = LocalDateTime.now();
    }

    /**
     * Business logic method: Updates the location status.
     * <p>
     * Business Rules: - Status transitions must be valid - BLOCKED status can only be set manually (not automatically)
     *
     * @param newStatus New status value
     * @throws IllegalArgumentException if newStatus is null
     */
    public void updateStatus(LocationStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("LocationStatus cannot be null");
        }

        LocationStatus oldStatus = this.status;
        this.status = newStatus;
        this.lastModifiedAt = LocalDateTime.now();

        // Publish status changed event if status actually changed
        if (oldStatus != newStatus) {
            addDomainEvent(new LocationStatusChangedEvent(this.getId(), this.getTenantId(), oldStatus, newStatus));
        }
    }

    /**
     * Business logic method: Updates the location capacity.
     * <p>
     * Business Rules: - New maximum capacity cannot be less than current quantity - Capacity update is allowed for any status
     *
     * @param newCapacity New capacity value
     * @throws IllegalArgumentException if newCapacity is null or invalid
     */
    public void updateCapacity(LocationCapacity newCapacity) {
        if (newCapacity == null) {
            throw new IllegalArgumentException("LocationCapacity cannot be null");
        }

        // Validate that current quantity doesn't exceed new maximum
        if (newCapacity.getMaximumQuantity() != null) {
            BigDecimal currentQty = this.capacity != null ? this.capacity.getCurrentQuantity() : BigDecimal.ZERO;
            if (currentQty.compareTo(newCapacity.getMaximumQuantity()) > 0) {
                throw new IllegalArgumentException(String.format("Current quantity (%s) exceeds new maximum capacity (%s)", currentQty, newCapacity.getMaximumQuantity()));
            }
        }

        this.capacity = newCapacity;
        this.lastModifiedAt = LocalDateTime.now();
    }

    /**
     * Business logic method: Updates the current quantity in location.
     * <p>
     * Business Rules: - Quantity cannot exceed maximum capacity if set - Quantity cannot be negative
     *
     * @param newQuantity New current quantity
     * @throws IllegalArgumentException if newQuantity is invalid
     */
    public void updateCurrentQuantity(BigDecimalQuantity newQuantity) {
        if (newQuantity == null) {
            throw new IllegalArgumentException("CurrentQuantity cannot be null");
        }

        BigDecimal maxQty = this.capacity != null ? this.capacity.getMaximumQuantity() : null;
        if (maxQty != null && newQuantity.isGreaterThan(maxQty)) {
            throw new IllegalArgumentException(String.format("CurrentQuantity (%s) cannot exceed MaximumQuantity (%s)", newQuantity.getValue(), maxQty));
        }

        LocationCapacity newCapacity = LocationCapacity.of(newQuantity.getValue(), maxQty);
        LocationStatus oldStatus = this.status;
        this.capacity = newCapacity;
        this.lastModifiedAt = LocalDateTime.now();

        // Update status based on capacity
        if (newQuantity.isZero()) {
            if (this.status != LocationStatus.BLOCKED) {
                this.status = LocationStatus.AVAILABLE;
            }
        } else if (this.status == LocationStatus.AVAILABLE) {
            this.status = LocationStatus.OCCUPIED;
        }

        // Publish status changed event if status changed
        if (oldStatus != this.status) {
            addDomainEvent(new LocationStatusChangedEvent(this.getId(), this.getTenantId(), oldStatus, this.status));
        }
    }

    /**
     * Business logic method: Updates location capacity (current and/or maximum).
     * <p>
     * Business Rules:
     * - New maximum capacity cannot be less than current quantity
     * - Capacity update is allowed for any status
     * - Updates status based on new capacity (AVAILABLE if empty, OCCUPIED if has stock)
     *
     * @param quantityChange Change in quantity (positive for increase, negative for decrease)
     * @throws IllegalArgumentException if quantityChange would result in negative quantity
     * @throws IllegalStateException    if capacity would be exceeded
     */
    public void updateCapacity(BigDecimalQuantity quantityChange) {
        if (quantityChange == null) {
            throw new IllegalArgumentException("QuantityChange cannot be null");
        }

        BigDecimal currentQty = this.capacity != null ? this.capacity.getCurrentQuantity() : BigDecimal.ZERO;
        BigDecimalQuantity currentQuantity = BigDecimalQuantity.of(currentQty);
        BigDecimalQuantity newQuantity;

        if (quantityChange.isPositive()) {
            newQuantity = currentQuantity.add(quantityChange);
        } else {
            // For negative changes, we need to allow subtraction that might go negative temporarily
            // but we'll validate the result
            BigDecimal result = currentQty.add(quantityChange.getValue());
            if (result.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Quantity cannot be negative after update");
            }
            newQuantity = BigDecimalQuantity.of(result);
        }

        BigDecimal maxQty = this.capacity != null ? this.capacity.getMaximumQuantity() : null;
        if (maxQty != null && newQuantity.isGreaterThan(maxQty)) {
            throw new IllegalStateException(String.format("Capacity would be exceeded. Current: %s, Change: %s, Max: %s", currentQty, quantityChange.getValue(), maxQty));
        }

        LocationCapacity newCapacity = LocationCapacity.of(newQuantity.getValue(), maxQty);
        LocationStatus oldStatus = this.status;
        this.capacity = newCapacity;
        this.lastModifiedAt = LocalDateTime.now();

        // Update status based on new capacity (only if not blocked)
        if (this.status != LocationStatus.BLOCKED) {
            if (newQuantity.isZero()) {
                this.status = LocationStatus.AVAILABLE;
            } else {
                this.status = LocationStatus.OCCUPIED;
            }
        }

        // Publish status changed event if status changed
        if (oldStatus != this.status) {
            addDomainEvent(new LocationStatusChangedEvent(this.getId(), this.getTenantId(), oldStatus, this.status));
        }
    }

    /**
     * Business logic method: Block location.
     * <p>
     * Business Rules:
     * - Location cannot be blocked if already blocked
     * - Block reason is required
     * - Blocked locations cannot be used for stock assignment
     * - Publishes LocationBlockedEvent
     *
     * @param blockedBy User who blocked the location
     * @param reason    Reason for blocking (required)
     * @throws IllegalStateException    if location is already blocked
     * @throws IllegalArgumentException if reason is null
     */
    public void block(UserId blockedBy, Description reason) {
        if (this.status == LocationStatus.BLOCKED) {
            throw new IllegalStateException("Location is already blocked");
        }

        if (blockedBy == null) {
            throw new IllegalArgumentException("BlockedBy cannot be null");
        }

        if (reason == null) {
            throw new IllegalArgumentException("Block reason is required");
        }

        LocationStatus oldStatus = this.status;
        this.status = LocationStatus.BLOCKED;
        this.lastModifiedAt = LocalDateTime.now();

        // Publish blocked event
        addDomainEvent(new LocationBlockedEvent(this.getId(), this.getTenantId(), blockedBy, reason, LocalDateTime.now()));

        // Publish status changed event
        if (oldStatus != this.status) {
            addDomainEvent(new LocationStatusChangedEvent(this.getId(), this.getTenantId(), oldStatus, this.status));
        }
    }

    /**
     * Business logic method: Unblock location.
     * <p>
     * Business Rules:
     * - Location cannot be unblocked if not blocked
     * - Status is set to AVAILABLE if empty, OCCUPIED if has stock
     * - Publishes LocationUnblockedEvent
     *
     * @param unblockedBy User who unblocked the location
     * @throws IllegalStateException    if location is not blocked
     * @throws IllegalArgumentException if unblockedBy is null
     */
    public void unblock(UserId unblockedBy) {
        if (this.status != LocationStatus.BLOCKED) {
            throw new IllegalStateException("Location is not blocked");
        }

        if (unblockedBy == null) {
            throw new IllegalArgumentException("UnblockedBy cannot be null");
        }

        LocationStatus oldStatus = this.status;

        // Determine new status based on current quantity
        BigDecimal currentQty = this.capacity != null ? this.capacity.getCurrentQuantity() : BigDecimal.ZERO;
        if (currentQty.compareTo(BigDecimal.ZERO) == 0) {
            this.status = LocationStatus.AVAILABLE;
        } else {
            this.status = LocationStatus.OCCUPIED;
        }

        this.lastModifiedAt = LocalDateTime.now();

        // Publish unblocked event
        addDomainEvent(new LocationUnblockedEvent(this.getId(), this.getTenantId(), unblockedBy, this.status, LocalDateTime.now()));

        // Publish status changed event
        if (oldStatus != this.status) {
            addDomainEvent(new LocationStatusChangedEvent(this.getId(), this.getTenantId(), oldStatus, this.status));
        }
    }

    /**
     * Business logic method: Updates the location coordinates.
     * <p>
     * Business Rules: - Coordinates can be updated for any status - Coordinates update will trigger barcode regeneration if barcode was auto-generated
     *
     * @param newCoordinates New coordinates value
     * @throws IllegalArgumentException if newCoordinates is null
     */
    public void updateCoordinates(LocationCoordinates newCoordinates) {
        if (newCoordinates == null) {
            throw new IllegalArgumentException("LocationCoordinates cannot be null");
        }

        this.coordinates = newCoordinates;
        this.lastModifiedAt = LocalDateTime.now();
    }

    /**
     * Business logic method: Updates the location description.
     * <p>
     * Business Rules: - Description can be updated for any status - Description can be set to null to clear it
     *
     * @param newDescription New description value (can be null)
     */
    public void updateDescription(LocationDescription newDescription) {
        this.description = newDescription;
        this.lastModifiedAt = LocalDateTime.now();
    }

    /**
     * Query method: Checks if location can accommodate additional quantity.
     *
     * @param quantity Quantity to check
     * @return true if location can accommodate the quantity
     */
    public boolean canAccommodate(BigDecimalQuantity quantity) {
        if (quantity == null || !quantity.isPositive()) {
            return false;
        }
        if (!isAvailable() && status != LocationStatus.RESERVED) {
            return false;
        }
        if (capacity == null) {
            return true; // No capacity constraints
        }
        return capacity.canAccommodate(quantity.getValue());
    }

    /**
     * Query method: Checks if location is available for stock assignment.
     *
     * @return true if location is AVAILABLE
     */
    public boolean isAvailable() {
        return status == LocationStatus.AVAILABLE;
    }

    /**
     * Query method: Checks if location is empty.
     *
     * @return true if location has no current quantity
     */
    public boolean isEmpty() {
        if (capacity == null) {
            return true; // No quantity tracked
        }
        return capacity.isEmpty();
    }

    /**
     * Business logic method: Assigns stock to location.
     * <p>
     * Business Rules:
     * - Location must be available
     * - Location must have sufficient capacity
     * - Updates location status and capacity
     * - Publishes LocationStatusChangedEvent if status changes
     *
     * @param stockItemId Stock item identifier
     * @param quantity    Quantity to assign
     * @throws IllegalStateException    if location is not available or cannot accommodate quantity
     * @throws IllegalArgumentException if stockItemId or quantity is null/invalid
     */
    public void assignStock(StockItemId stockItemId, BigDecimalQuantity quantity) {
        if (stockItemId == null) {
            throw new IllegalArgumentException("StockItemId cannot be null");
        }
        if (quantity == null || !quantity.isPositive()) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        // Validate location is available
        if (this.status != LocationStatus.AVAILABLE) {
            throw new IllegalStateException(String.format("Cannot assign stock to location in status: %s", this.status));
        }

        // Validate capacity
        if (!hasCapacity(quantity)) {
            BigDecimal available = getAvailableCapacity();
            throw new IllegalStateException(
                    String.format("Location does not have sufficient capacity. Required: %s, Available: %s", quantity.getValue(), available != null ? available : "unlimited"));
        }

        // Update capacity
        BigDecimalQuantity currentQuantity = BigDecimalQuantity.of(this.capacity.getCurrentQuantity());
        BigDecimalQuantity newCurrentQuantity = currentQuantity.add(quantity);
        LocationCapacity newCapacity = LocationCapacity.of(newCurrentQuantity.getValue(), this.capacity.getMaximumQuantity());
        this.capacity = newCapacity;

        // Update status if location is now full
        LocationStatus oldStatus = this.status;
        if (isFull()) {
            this.status = LocationStatus.OCCUPIED;
        }

        this.lastModifiedAt = LocalDateTime.now();

        // Publish domain events
        addDomainEvent(new LocationAssignedEvent(this.getId(), this.getTenantId(), stockItemId.getValueAsString(), quantity.getValue()));

        if (oldStatus != this.status) {
            addDomainEvent(new LocationStatusChangedEvent(this.getId(), this.getTenantId(), oldStatus, this.status));
        }
    }

    /**
     * Business logic method: Checks if location has sufficient capacity for the given quantity.
     *
     * @param quantity Required quantity
     * @return true if location has sufficient capacity
     * @throws IllegalArgumentException if quantity is null or non-positive
     */
    public boolean hasCapacity(BigDecimalQuantity quantity) {
        if (quantity == null || !quantity.isPositive()) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (this.capacity == null) {
            return true; // No capacity limit
        }
        return this.capacity.canAccommodate(quantity.getValue());
    }

    /**
     * Business logic method: Gets available capacity.
     *
     * @return Available capacity, or null if unlimited
     */
    public BigDecimal getAvailableCapacity() {
        if (this.capacity == null) {
            return null; // Unlimited capacity
        }
        return this.capacity.getAvailableCapacity();
    }

    /**
     * Query method: Checks if location is at full capacity.
     *
     * @return true if location is at full capacity
     */
    public boolean isFull() {
        if (capacity == null) {
            return false; // Unlimited capacity
        }
        return capacity.isFull();
    }

    // Getters (read-only access)

    public LocationBarcode getBarcode() {
        return barcode;
    }

    public LocationCoordinates getCoordinates() {
        return coordinates;
    }

    public LocationStatus getStatus() {
        return status;
    }

    public LocationCapacity getCapacity() {
        return capacity;
    }

    public LocationCode getCode() {
        return code;
    }

    public LocationName getName() {
        return name;
    }

    public LocationType getType() {
        return type;
    }

    public LocationDescription getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    public LocationId getParentLocationId() {
        return parentLocationId;
    }

    /**
     * Builder class for constructing Location instances. Ensures all required fields are set and validated.
     */
    public static class Builder {
        private Location location = new Location();

        public Builder locationId(LocationId id) {
            location.setId(id);
            return this;
        }

        public Builder tenantId(TenantId tenantId) {
            location.setTenantId(tenantId);
            return this;
        }

        public Builder barcode(LocationBarcode barcode) {
            location.barcode = barcode;
            return this;
        }

        public Builder coordinates(LocationCoordinates coordinates) {
            location.coordinates = coordinates;
            return this;
        }

        public Builder status(LocationStatus status) {
            location.status = status;
            return this;
        }

        public Builder capacity(LocationCapacity capacity) {
            location.capacity = capacity;
            return this;
        }

        public Builder code(LocationCode code) {
            location.code = code;
            return this;
        }

        public Builder code(String code) {
            location.code = LocationCode.ofNullable(code);
            return this;
        }

        public Builder name(LocationName name) {
            location.name = name;
            return this;
        }

        public Builder name(String name) {
            location.name = LocationName.ofNullable(name);
            return this;
        }

        public Builder type(LocationType type) {
            location.type = type;
            return this;
        }

        public Builder type(String type) {
            location.type = LocationType.ofNullable(type);
            return this;
        }

        public Builder description(LocationDescription description) {
            location.description = description;
            return this;
        }

        public Builder description(String description) {
            location.description = LocationDescription.ofNullable(description);
            return this;
        }

        public Builder parentLocationId(LocationId parentLocationId) {
            location.parentLocationId = parentLocationId;
            return this;
        }

        /**
         * Sets the creation timestamp (for loading from database).
         *
         * @param createdAt Creation timestamp
         * @return Builder instance
         */
        public Builder createdAt(LocalDateTime createdAt) {
            location.createdAt = createdAt;
            return this;
        }

        /**
         * Sets the last modified timestamp (for loading from database).
         *
         * @param lastModifiedAt Last modified timestamp
         * @return Builder instance
         */
        public Builder lastModifiedAt(LocalDateTime lastModifiedAt) {
            location.lastModifiedAt = lastModifiedAt;
            return this;
        }

        /**
         * Sets the version (for loading from database).
         *
         * @param version Version number
         * @return Builder instance
         */
        public Builder version(int version) {
            location.setVersion(version);
            return this;
        }

        /**
         * Sets the version as Long (for loading from database).
         *
         * @param version Version number
         * @return Builder instance
         */
        public Builder version(Long version) {
            location.setVersion(version != null ? version.intValue() : 0);
            return this;
        }

        /**
         * Builds and validates the Location instance.
         *
         * @return Validated Location instance
         * @throws IllegalArgumentException if validation fails
         */
        public Location build() {
            validate();
            initializeDefaults();

            // Set createdAt if not already set (for new locations)
            if (location.createdAt == null) {
                location.createdAt = LocalDateTime.now();
            }
            if (location.lastModifiedAt == null) {
                location.lastModifiedAt = LocalDateTime.now();
            }

            // Generate barcode from coordinates if not provided
            // Include location ID to ensure uniqueness (prevents collisions from sanitized codes)
            if (location.barcode == null && location.coordinates != null) {
                location.barcode = LocationBarcode.generate(location.coordinates, location.getId().getValue());
            }

            // Publish creation event only if this is a new location (no version set)
            if (location.getVersion() == 0) {
                location.addDomainEvent(new LocationCreatedEvent(location.getId(), location.getTenantId(), location.barcode, location.coordinates, location.status));
            }

            return consumeLocation();
        }

        /**
         * Validates all required fields are set.
         *
         * @throws IllegalArgumentException if validation fails
         */
        private void validate() {
            if (location.getId() == null) {
                throw new IllegalArgumentException("LocationId is required");
            }
            if (location.getTenantId() == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            if (location.coordinates == null) {
                throw new IllegalArgumentException("LocationCoordinates is required");
            }
            // Barcode can be null - will be generated from coordinates if not provided
        }

        /**
         * Initializes default values for optional fields.
         */
        private void initializeDefaults() {
            if (location.status == null) {
                location.status = LocationStatus.AVAILABLE;
            }
            if (location.capacity == null) {
                location.capacity = LocationCapacity.empty();
            }
        }

        /**
         * Consumes the location from the builder and returns it. Creates a new location instance for the next build.
         *
         * @return Built location
         */
        private Location consumeLocation() {
            Location builtLocation = location;
            location = new Location();
            return builtLocation;
        }

        /**
         * Builds Location without publishing creation event. Used when reconstructing from persistence.
         *
         * @return Validated Location instance
         * @throws IllegalArgumentException if validation fails
         */
        public Location buildWithoutEvents() {
            validate();
            initializeDefaults();

            // Set createdAt if not already set
            if (location.createdAt == null) {
                location.createdAt = LocalDateTime.now();
            }
            if (location.lastModifiedAt == null) {
                location.lastModifiedAt = LocalDateTime.now();
            }

            // Generate barcode from coordinates if not provided
            // Include location ID to ensure uniqueness (prevents collisions from sanitized codes)
            if (location.barcode == null && location.coordinates != null) {
                location.barcode = LocationBarcode.generate(location.coordinates, location.getId().getValue());
            }

            // Do not publish events when loading from database
            return consumeLocation();
        }
    }
}

