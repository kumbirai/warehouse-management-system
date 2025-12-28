package com.ccbsa.wms.location.dataaccess.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.ccbsa.wms.location.domain.core.valueobject.LocationStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity: LocationViewEntity
 * <p>
 * Read model entity for location queries. This is a denormalized view optimized for read operations.
 * <p>
 * Note: Currently maps to the locations table. In the future, this can be migrated to a separate
 * location_views table that is maintained via event listeners for better read/write separation.
 * <p>
 * The schema "tenant_schema" is a placeholder that will be dynamically replaced with
 * the actual tenant schema at runtime by TenantAwarePhysicalNamingStrategy.
 */
@Entity
@Table(name = "locations", schema = "tenant_schema")
@Getter
@Setter
@NoArgsConstructor
public class LocationViewEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", length = 255, nullable = false)
    private String tenantId;

    @Column(name = "barcode", length = 255, nullable = false)
    private String barcode;

    @Column(name = "code", length = 100)
    private String code;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "type", length = 50)
    private String type;

    @Column(name = "zone", length = 100, nullable = false)
    private String zone;

    @Column(name = "aisle", length = 100, nullable = false)
    private String aisle;

    @Column(name = "rack", length = 100, nullable = false)
    private String rack;

    @Column(name = "level", length = 100, nullable = false)
    private String level;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    private LocationStatus status;

    @Column(name = "current_quantity", precision = 18, scale = 2)
    private BigDecimal currentQuantity;

    @Column(name = "maximum_quantity", precision = 18, scale = 2)
    private BigDecimal maximumQuantity;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_modified_at", nullable = false)
    private LocalDateTime lastModifiedAt;

    @Column(name = "parent_location_id")
    private UUID parentLocationId;
}

