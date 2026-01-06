package com.ccbsa.wms.location.domain.core.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.ccbsa.common.domain.valueobject.BigDecimalQuantity;
import com.ccbsa.common.domain.valueobject.StockClassification;
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

        // Filter out expired stock items - they cannot be assigned locations
        List<StockItemAssignmentRequest> validStockItems = stockItems.stream().filter(item -> item.getClassification() != StockClassification.EXPIRED).collect(Collectors.toList());

        if (validStockItems.isEmpty()) {
            // All stock items are expired - return empty assignments
            return new HashMap<>();
        }

        // Sort stock items by expiration date (earliest first)
        // Strategy: Items with expiration dates are prioritized (FEFO principle)
        // Items without expiration dates (non-perishable) are processed last
        // This ensures perishable items get the best locations (closest to picking zones)
        // while non-perishable items can use remaining capacity
        List<StockItemAssignmentRequest> sortedStockItems = validStockItems.stream().sorted((a, b) -> {
            // Null expiration dates (non-perishable) go to end
            // They will still be assigned if locations have remaining capacity
            if (a.getExpirationDate() == null && b.getExpirationDate() == null) {
                return 0; // Equal priority for non-perishable items
            }
            if (a.getExpirationDate() == null) {
                return 1; // Non-perishable goes after perishable
            }
            if (b.getExpirationDate() == null) {
                return -1; // Perishable goes before non-perishable
            }

            // Both have expiration dates - sort by earliest expiration first (FEFO)
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
        // Strategy: Fill locations to capacity before moving to the next location
        // This optimizes warehouse space utilization
        Map<String, LocationId> assignments = new HashMap<>();
        // Track locations that are full and cannot accept more stock
        Set<LocationId> fullLocationIds = new java.util.HashSet<>();

        for (StockItemAssignmentRequest stockItem : sortedStockItems) {
            // Find next available location with sufficient capacity
            // Note: We don't exclude locations that already have assignments in this batch
            // because we want to fill locations to capacity before moving to the next
            BigDecimalQuantity requiredQuantity = BigDecimalQuantity.of(stockItem.getQuantity());
            Location assignedLocation = findAvailableLocation(sortedLocations, requiredQuantity, fullLocationIds);

            if (assignedLocation != null) {
                assignments.put(stockItem.getStockItemId(), assignedLocation.getId());

                // Check if this location is now full after this assignment
                // We need to calculate what the capacity will be after assignment
                BigDecimal currentQuantity = assignedLocation.getCapacity() != null ? assignedLocation.getCapacity().getCurrentQuantity() : BigDecimal.ZERO;
                BigDecimal maxQuantity = assignedLocation.getCapacity() != null ? assignedLocation.getCapacity().getMaximumQuantity() : null;

                if (maxQuantity != null) {
                    BigDecimal newQuantity = currentQuantity.add(stockItem.getQuantity());
                    if (newQuantity.compareTo(maxQuantity) >= 0) {
                        // Location will be full after this assignment
                        fullLocationIds.add(assignedLocation.getId());
                    }
                }
                // If unlimited capacity, location never becomes full
            } else {
                // No available location for this stock item - skip it
                // Stock item will remain unassigned and can be assigned later when locations become available
                // This is acceptable behavior - stock can be allocated from unassigned items
                // Logging is done at the application service layer
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
     * <p>
     * Prioritizes locations that can be filled to full capacity first:
     * 1. Partially-filled locations that can be completely filled with this stock item
     * 2. Partially-filled locations (to consolidate stock and fill them up)
     * 3. Empty locations that can accommodate the full quantity
     * 4. Empty locations (if unlimited capacity)
     * <p>
     * This ensures locations are filled to capacity before moving to the next location,
     * optimizing warehouse space utilization.
     * <p>
     * Note: Multiple stock items can be assigned to the same location in the same batch
     * until the location is full, which is why we only exclude full locations, not all assigned locations.
     *
     * @param locations        Sorted list of locations (by proximity)
     * @param requiredQuantity Required quantity
     * @param fullLocationIds  Set of location IDs that are full and cannot accept more stock
     * @return Available location with sufficient capacity, or null if none found
     */
    private Location findAvailableLocation(List<Location> locations, BigDecimalQuantity requiredQuantity, Set<LocationId> fullLocationIds) {
        Location bestLocation = null;
        BigDecimal bestRemainingCapacity = null;

        for (Location location : locations) {
            // Skip if location is full
            if (fullLocationIds.contains(location.getId())) {
                continue;
            }

            // Check if location is available
            if (!location.isAvailable() && location.getStatus() != LocationStatus.RESERVED) {
                continue;
            }

            // Check capacity
            if (!location.hasCapacity(requiredQuantity)) {
                continue;
            }

            // Get available capacity
            BigDecimal availableCapacity = location.getAvailableCapacity();

            // Check if this location can be filled completely with this stock item
            boolean canFillCompletely = false;
            if (availableCapacity != null) {
                // Location has a capacity limit - check if we can fill it completely
                canFillCompletely = availableCapacity.compareTo(requiredQuantity.getValue()) == 0;
            } else {
                // Unlimited capacity - cannot fill completely
                canFillCompletely = false;
            }

            // Prioritize locations that can be filled completely
            if (canFillCompletely) {
                // This location can be filled to capacity - prioritize it
                return location;
            }

            // For locations that can't be filled completely, prefer:
            // 1. Partially-filled locations (to consolidate stock)
            // 2. Locations with less remaining capacity (to fill them up)
            if (bestLocation == null) {
                bestLocation = location;
                bestRemainingCapacity = availableCapacity;
            } else {
                // Prefer partially-filled locations over empty ones
                boolean currentIsEmpty = location.isEmpty();
                boolean bestIsEmpty = bestLocation.isEmpty();

                if (!currentIsEmpty && bestIsEmpty) {
                    // Current is partially-filled, best is empty - prefer current
                    bestLocation = location;
                    bestRemainingCapacity = availableCapacity;
                } else if (currentIsEmpty && !bestIsEmpty) {
                    // Current is empty, best is partially-filled - keep best
                    // (no change)
                } else if (!currentIsEmpty && !bestIsEmpty) {
                    // Both are partially-filled - prefer the one with less remaining capacity
                    // (to fill locations more completely)
                    if (availableCapacity != null && bestRemainingCapacity != null) {
                        if (availableCapacity.compareTo(bestRemainingCapacity) < 0) {
                            bestLocation = location;
                            bestRemainingCapacity = availableCapacity;
                        }
                    }
                }
                // If both are empty, keep the first one (already sorted by proximity)
            }
        }

        return bestLocation;
    }
}

