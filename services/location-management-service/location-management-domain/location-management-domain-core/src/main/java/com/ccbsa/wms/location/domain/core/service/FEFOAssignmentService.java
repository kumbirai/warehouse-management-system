package com.ccbsa.wms.location.domain.core.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.ccbsa.wms.location.domain.core.entity.Location;
import com.ccbsa.wms.location.domain.core.valueobject.LocationCoordinates;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationStatus;
import com.ccbsa.wms.location.domain.core.valueobject.StockItemAssignmentRequest;

/**
 * Domain Service: FEFOAssignmentService
 * <p>
 * Business logic for assigning locations based on FEFO (First Expiring First Out) principles.
 * <p>
 * This is a domain service because it coordinates logic across multiple aggregates
 * within the Location Management bounded context.
 * <p>
 * Algorithm:
 * 1. Sort stock items by expiration date (earliest first)
 * 2. Sort available locations by proximity to picking zones (closest first)
 * 3. Match stock items to locations:
 * - Stock expiring soonest → Locations closest to picking zones
 * - Stock expiring later → Locations further from picking zones
 * 4. Consider location capacity and current stock
 */
public class FEFOAssignmentService {

    /**
     * Assigns locations to stock items based on FEFO principles.
     * <p>
     * Algorithm:
     * 1. Sort stock items by expiration date (earliest first)
     * 2. Sort available locations by proximity to picking zones (closest first)
     * 3. Match stock items to locations:
     * - Stock expiring soonest → Locations closest to picking zones
     * - Stock expiring later → Locations further from picking zones
     * 4. Consider location capacity and current stock
     *
     * @param stockItems         Stock items to assign (sorted by expiration date)
     * @param availableLocations Available locations (sorted by proximity to picking zone)
     * @return Map of StockItemId (String) to LocationId assignments
     * @throws IllegalArgumentException if inputs are invalid
     * @throws IllegalStateException    if no available location found for a stock item
     */
    public Map<String, LocationId> assignLocationsFEFO(List<StockItemAssignmentRequest> stockItems, List<Location> availableLocations) {

        // Validate inputs
        if (stockItems == null || stockItems.isEmpty()) {
            throw new IllegalArgumentException("Stock items list cannot be empty");
        }
        if (availableLocations == null || availableLocations.isEmpty()) {
            throw new IllegalArgumentException("Available locations list cannot be empty");
        }

        // Sort stock items by expiration date (earliest first)
        List<StockItemAssignmentRequest> sortedStockItems = stockItems.stream().sorted((a, b) -> {
            // Null expiration dates (non-perishable) go to end
            if (a.getExpirationDate() == null && b.getExpirationDate() == null) {
                return 0;
            }
            if (a.getExpirationDate() == null) {
                return 1;
            }
            if (b.getExpirationDate() == null) {
                return -1;
            }

            return a.getExpirationDate().getValue().compareTo(b.getExpirationDate().getValue());
        }).collect(Collectors.toList());

        // Sort locations by proximity to picking zones (closest first)
        List<Location> sortedLocations =
                availableLocations.stream().filter(location -> location.isAvailable() || location.getStatus() == LocationStatus.RESERVED).sorted((a, b) -> {
                    int proximityA = calculateProximityToPickingZone(a);
                    int proximityB = calculateProximityToPickingZone(b);
                    return Integer.compare(proximityA, proximityB);
                }).collect(Collectors.toList());

        // Match stock items to locations
        Map<String, LocationId> assignments = new HashMap<>();
        Set<LocationId> assignedLocationIds = new java.util.HashSet<>();

        for (StockItemAssignmentRequest stockItem : sortedStockItems) {
            // Find next available location with sufficient capacity
            Location assignedLocation = findAvailableLocation(sortedLocations, stockItem.getQuantity(), assignedLocationIds);

            if (assignedLocation != null) {
                assignments.put(stockItem.getStockItemId(), assignedLocation.getId());
                assignedLocationIds.add(assignedLocation.getId());
            } else {
                // No available location - log warning
                // In production, this would trigger an alert
                throw new IllegalStateException(String.format("No available location for stock item: %s", stockItem.getStockItemId()));
            }
        }

        return assignments;
    }

    /**
     * Calculates proximity to picking zones.
     * <p>
     * Lower number = closer to picking zones (higher priority for FEFO).
     * <p>
     * Algorithm:
     * - Zone "A" (picking zone) = 0
     * - Zone "B" = 1
     * - Zone "C" = 2
     * - etc.
     *
     * @param location Location to calculate proximity for
     * @return Proximity score (lower = closer to picking zones)
     */
    private int calculateProximityToPickingZone(Location location) {
        LocationCoordinates coordinates = location.getCoordinates();
        if (coordinates == null) {
            return Integer.MAX_VALUE; // Unknown location, lowest priority
        }

        String zone = coordinates.getZone();

        // Zone "A" is picking zone (closest)
        if (zone.equalsIgnoreCase("A")) {
            return 0;
        }

        // Calculate distance from picking zone
        // For now, simple alphabetical comparison
        // In production, this would use actual warehouse layout data
        if (zone.length() > 0) {
            char zoneChar = zone.charAt(0);
            char pickingZoneChar = 'A';

            int distance = Math.abs(zoneChar - pickingZoneChar);
            // Add penalty for zones beyond A (B=1, C=2, etc.)
            return distance;
        }

        return Integer.MAX_VALUE; // Unknown zone, lowest priority
    }

    /**
     * Finds available location with sufficient capacity.
     *
     * @param locations        Sorted list of locations (by proximity)
     * @param requiredQuantity Required quantity
     * @param alreadyAssigned  Set of location IDs already assigned in this batch
     * @return Available location with sufficient capacity, or null if none found
     */
    private Location findAvailableLocation(List<Location> locations, BigDecimal requiredQuantity, Set<LocationId> alreadyAssigned) {

        for (Location location : locations) {
            // Skip if already assigned in this batch
            if (alreadyAssigned.contains(location.getId())) {
                continue;
            }

            // Check if location is available
            if (!location.isAvailable() && location.getStatus() != LocationStatus.RESERVED) {
                continue;
            }

            // Check capacity
            if (location.hasCapacity(requiredQuantity)) {
                return location;
            }
        }

        return null;
    }
}

