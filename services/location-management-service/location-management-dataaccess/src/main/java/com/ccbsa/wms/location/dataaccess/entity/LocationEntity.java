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
import jakarta.persistence.Version;

/**
 * JPA Entity: LocationEntity
 * <p>
 * JPA representation of Location aggregate. Uses tenant schema resolver for multi-tenant isolation (schema-per-tenant strategy).
 * <p>
 * This entity maps to the Location domain aggregate and uses domain enums directly to maintain consistency between domain and persistence layers.
 * <p>
 * Note: Location is tenant-aware. Uses schema-per-tenant strategy via TenantAwarePhysicalNamingStrategy
 * for multi-tenant isolation. Each tenant has its own isolated PostgreSQL schema.
 * <p>
 * The schema "tenant_schema" is a placeholder that will be dynamically replaced with
 * the actual tenant schema at runtime by TenantAwarePhysicalNamingStrategy.
 */
@Entity
@Table(name = "locations",
        schema = "tenant_schema")
public class LocationEntity {
    @Id
    @Column(name = "id",
            nullable = false)
    private UUID id;

    @Column(name = "tenant_id",
            length = 255,
            nullable = false)
    private String tenantId;

    @Column(name = "barcode",
            length = 255,
            nullable = false)
    private String barcode;

    @Column(name = "code",
            length = 100)
    private String code;

    @Column(name = "name",
            length = 255)
    private String name;

    @Column(name = "type",
            length = 50)
    private String type;

    @Column(name = "zone",
            length = 100,
            nullable = false)
    private String zone;

    @Column(name = "aisle",
            length = 100,
            nullable = false)
    private String aisle;

    @Column(name = "rack",
            length = 100,
            nullable = false)
    private String rack;

    @Column(name = "level",
            length = 100,
            nullable = false)
    private String level;

    @Enumerated(EnumType.STRING)
    @Column(name = "status",
            length = 50,
            nullable = false)
    private LocationStatus status;

    @Column(name = "current_quantity",
            precision = 18,
            scale = 2)
    private BigDecimal currentQuantity;

    @Column(name = "maximum_quantity",
            precision = 18,
            scale = 2)
    private BigDecimal maximumQuantity;

    @Column(name = "description",
            length = 500)
    private String description;

    @Column(name = "created_at",
            nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_modified_at",
            nullable = false)
    private LocalDateTime lastModifiedAt;

    @Version
    @Column(name = "version",
            nullable = false)
    private Long version;

    // JPA requires no-arg constructor
    public LocationEntity() {
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getAisle() {
        return aisle;
    }

    public void setAisle(String aisle) {
        this.aisle = aisle;
    }

    public String getRack() {
        return rack;
    }

    public void setRack(String rack) {
        this.rack = rack;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public LocationStatus getStatus() {
        return status;
    }

    public void setStatus(LocationStatus status) {
        this.status = status;
    }

    public BigDecimal getCurrentQuantity() {
        return currentQuantity;
    }

    public void setCurrentQuantity(BigDecimal currentQuantity) {
        this.currentQuantity = currentQuantity;
    }

    public BigDecimal getMaximumQuantity() {
        return maximumQuantity;
    }

    public void setMaximumQuantity(BigDecimal maximumQuantity) {
        this.maximumQuantity = maximumQuantity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    public void setLastModifiedAt(LocalDateTime lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}

