package com.ccbsa.wms.location.domain.core.valueobject;

/**
 * Enum: MovementType
 * <p>
 * Represents the type of stock movement.
 * <p>
 * Movement Types:
 * - RECEIVING_TO_STORAGE: Movement from receiving area to storage location
 * - STORAGE_TO_PICKING: Movement from storage location to picking location
 * - INTER_STORAGE: Movement between storage locations
 * - PICKING_TO_SHIPPING: Movement from picking location to shipping area
 */
public enum MovementType {
    RECEIVING_TO_STORAGE, STORAGE_TO_PICKING, INTER_STORAGE, PICKING_TO_SHIPPING
}

