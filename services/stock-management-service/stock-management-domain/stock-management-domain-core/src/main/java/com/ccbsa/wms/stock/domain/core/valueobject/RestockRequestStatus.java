package com.ccbsa.wms.stock.domain.core.valueobject;

/**
 * Enum: RestockRequestStatus
 * <p>
 * Represents the status of a restock request.
 * <p>
 * Status values:
 * - PENDING: Restock request is pending processing
 * - SENT_TO_D365: Restock request has been sent to Microsoft Dynamics 365
 * - FULFILLED: Restock request has been fulfilled
 * - CANCELLED: Restock request has been cancelled
 */
public enum RestockRequestStatus {
    /**
     * Restock request is pending processing
     */
    PENDING,

    /**
     * Restock request has been sent to Microsoft Dynamics 365
     */
    SENT_TO_D365,

    /**
     * Restock request has been fulfilled
     */
    FULFILLED,

    /**
     * Restock request has been cancelled
     */
    CANCELLED
}
