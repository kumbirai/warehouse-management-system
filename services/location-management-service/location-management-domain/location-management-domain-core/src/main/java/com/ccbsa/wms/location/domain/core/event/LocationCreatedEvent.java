package com.ccbsa.wms.location.domain.core.event;

import com.ccbsa.common.domain.EventMetadata;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationBarcode;
import com.ccbsa.wms.location.domain.core.valueobject.LocationCoordinates;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationStatus;

/**
 * Domain Event: LocationCreatedEvent
 * <p>
 * Published when a new warehouse location is created.
 * <p>
 * Event Version: 1.0
 */
public final class LocationCreatedEvent
        extends LocationManagementEvent {
    private final LocationBarcode barcode;
    private final LocationCoordinates coordinates;
    private final LocationStatus status;
    private final TenantId tenantId;

    /**
     * Constructor for LocationCreatedEvent without metadata.
     *
     * @param locationId  Location identifier
     * @param tenantId    Tenant identifier
     * @param barcode     Location barcode
     * @param coordinates Location coordinates
     * @param status      Location status
     * @throws IllegalArgumentException if any parameter is null
     */
    public LocationCreatedEvent(LocationId locationId, TenantId tenantId, LocationBarcode barcode, LocationCoordinates coordinates, LocationStatus status) {
        super(locationId);
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId cannot be null");
        }
        if (barcode == null) {
            throw new IllegalArgumentException("LocationBarcode cannot be null");
        }
        if (coordinates == null) {
            throw new IllegalArgumentException("LocationCoordinates cannot be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("LocationStatus cannot be null");
        }
        this.tenantId = tenantId;
        this.barcode = barcode;
        this.coordinates = coordinates;
        this.status = status;
    }

    /**
     * Constructor for LocationCreatedEvent with metadata.
     *
     * @param locationId  Location identifier
     * @param tenantId    Tenant identifier
     * @param barcode     Location barcode
     * @param coordinates Location coordinates
     * @param status      Location status
     * @param metadata    Event metadata for traceability
     * @throws IllegalArgumentException if any parameter is null
     */
    public LocationCreatedEvent(LocationId locationId, TenantId tenantId, LocationBarcode barcode, LocationCoordinates coordinates, LocationStatus status, EventMetadata metadata) {
        super(locationId, metadata);
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId cannot be null");
        }
        if (barcode == null) {
            throw new IllegalArgumentException("LocationBarcode cannot be null");
        }
        if (coordinates == null) {
            throw new IllegalArgumentException("LocationCoordinates cannot be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("LocationStatus cannot be null");
        }
        this.tenantId = tenantId;
        this.barcode = barcode;
        this.coordinates = coordinates;
        this.status = status;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public LocationBarcode getBarcode() {
        return barcode;
    }

    public LocationCoordinates getCoordinates() {
        return coordinates;
    }

    public LocationStatus getStatus() {
        return status;
    }
}

