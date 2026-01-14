package com.ccbsa.wms.location.domain.core.service;

import java.util.List;

import com.ccbsa.common.domain.valueobject.BigDecimalQuantity;
import com.ccbsa.common.domain.valueobject.ProductCondition;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.wms.location.domain.core.entity.Location;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

/**
 * Domain Service: ReturnLocationAssignmentStrategy
 * <p>
 * Business logic for assigning locations to returns based on product condition.
 * <p>
 * This is a domain service because it coordinates logic across multiple aggregates
 * within the Location Management bounded context.
 * <p>
 * Assignment Rules:
 * - GOOD condition → FEFO location (closest to picking zones)
 * - DAMAGED/QUARANTINE → Quarantine zone
 * - EXPIRED/WRITE_OFF → Disposal location
 */
public class ReturnLocationAssignmentStrategy {

    /**
     * Assigns a location to a return line item based on product condition.
     * <p>
     * Business Rules:
     * - GOOD condition: Assign to available stock location using FEFO
     * - DAMAGED/QUARANTINE: Assign to quarantine zone
     * - EXPIRED/WRITE_OFF: Assign to disposal location
     *
     * @param productId          Product identifier
     * @param productCondition   Product condition
     * @param quantity           Quantity to assign
     * @param availableLocations List of available locations (pre-filtered by application service)
     * @return Assigned LocationId
     * @throws IllegalStateException if no suitable location found
     */
    public LocationId assignLocationByCondition(ProductId productId, ProductCondition productCondition, int quantity, List<Location> availableLocations) {
        if (productId == null) {
            throw new IllegalArgumentException("ProductId cannot be null");
        }
        if (productCondition == null) {
            throw new IllegalArgumentException("ProductCondition cannot be null");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (availableLocations == null || availableLocations.isEmpty()) {
            throw new IllegalStateException("No available locations provided");
        }

        BigDecimalQuantity requiredQuantity = BigDecimalQuantity.of(quantity);

        switch (productCondition) {
            case GOOD:
                return assignToAvailableStock(productId, requiredQuantity, availableLocations);

            case DAMAGED:
            case QUARANTINE:
                return assignToQuarantineZone(productId, requiredQuantity, availableLocations);

            case EXPIRED:
            case WRITE_OFF:
                return assignToDisposalArea(productId, requiredQuantity, availableLocations);

            default:
                throw new IllegalArgumentException("Unknown product condition: " + productCondition);
        }
    }

    /**
     * Assigns good condition products to available stock locations.
     * Uses first available location with sufficient capacity.
     *
     * @param productId          Product identifier
     * @param quantity           Required quantity
     * @param availableLocations List of available locations
     * @return Assigned LocationId
     * @throws IllegalStateException if no suitable location found
     */
    private LocationId assignToAvailableStock(ProductId productId, BigDecimalQuantity quantity, List<Location> availableLocations) {
        // Filter to BIN type locations (stock must be at lowest hierarchy level)
        List<Location> binLocations =
                availableLocations.stream().filter(location -> location.getType() != null && "BIN".equalsIgnoreCase(location.getType().getValue())).filter(Location::isAvailable)
                        .toList();

        if (binLocations.isEmpty()) {
            throw new IllegalStateException("No available BIN locations for good condition product: " + productId.getValueAsString());
        }

        // Find first location with sufficient capacity
        for (Location location : binLocations) {
            if (location.hasCapacity(quantity)) {
                return location.getId();
            }
        }

        throw new IllegalStateException("No available location with sufficient capacity for product: " + productId.getValueAsString());
    }

    /**
     * Assigns damaged/quarantine products to quarantine zone.
     *
     * @param productId          Product identifier
     * @param quantity           Required quantity
     * @param availableLocations List of available locations (should include quarantine locations)
     * @return Assigned LocationId
     * @throws IllegalStateException if no quarantine location found
     */
    private LocationId assignToQuarantineZone(ProductId productId, BigDecimalQuantity quantity, List<Location> availableLocations) {
        // Filter to quarantine locations (could be identified by type, code pattern, or description)
        // For now, we'll use any available location - in production, you'd filter by location attributes
        List<Location> quarantineLocations = availableLocations.stream().filter(Location::isAvailable).toList();

        if (quarantineLocations.isEmpty()) {
            throw new IllegalStateException("No available quarantine location for damaged product: " + productId.getValueAsString());
        }

        // Find first location with sufficient capacity
        for (Location location : quarantineLocations) {
            if (location.hasCapacity(quantity)) {
                return location.getId();
            }
        }

        throw new IllegalStateException("No quarantine location with sufficient capacity for product: " + productId.getValueAsString());
    }

    /**
     * Assigns expired/write-off products to disposal area.
     *
     * @param productId          Product identifier
     * @param quantity           Required quantity
     * @param availableLocations List of available locations (should include disposal locations)
     * @return Assigned LocationId
     * @throws IllegalStateException if no disposal location found
     */
    private LocationId assignToDisposalArea(ProductId productId, BigDecimalQuantity quantity, List<Location> availableLocations) {
        // Filter to disposal locations
        // For now, we'll use any available location - in production, you'd filter by location attributes
        List<Location> disposalLocations = availableLocations.stream().filter(Location::isAvailable).toList();

        if (disposalLocations.isEmpty()) {
            throw new IllegalStateException("No available disposal location for expired/write-off product: " + productId.getValueAsString());
        }

        // Find first location with sufficient capacity
        for (Location location : disposalLocations) {
            if (location.hasCapacity(quantity)) {
                return location.getId();
            }
        }

        throw new IllegalStateException("No disposal location with sufficient capacity for product: " + productId.getValueAsString());
    }
}
