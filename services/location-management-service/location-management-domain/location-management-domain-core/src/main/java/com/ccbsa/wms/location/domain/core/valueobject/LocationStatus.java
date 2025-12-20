package com.ccbsa.wms.location.domain.core.valueobject;

/**
 * Enum: LocationStatus
 *
 * Represents the status of a warehouse location.
 *
 * Status Values: - AVAILABLE: Location is available for stock assignment - OCCUPIED: Location currently contains stock - RESERVED: Location is reserved for upcoming stock
 * assignment - BLOCKED: Location is blocked and cannot be used
 */
public enum LocationStatus {
    AVAILABLE, OCCUPIED, RESERVED, BLOCKED
}

