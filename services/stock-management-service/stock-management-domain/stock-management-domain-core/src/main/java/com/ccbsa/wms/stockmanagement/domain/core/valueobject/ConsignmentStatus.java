package com.ccbsa.wms.stockmanagement.domain.core.valueobject;

/**
 * Enum: ConsignmentStatus
 * <p>
 * Represents the status of a stock consignment.
 * <p>
 * Status Values:
 * - RECEIVED: Consignment has been received but not yet confirmed
 * - CONFIRMED: Consignment has been confirmed and is ready for processing
 * - CANCELLED: Consignment has been cancelled
 */
public enum ConsignmentStatus {
    RECEIVED,
    CONFIRMED,
    CANCELLED
}

