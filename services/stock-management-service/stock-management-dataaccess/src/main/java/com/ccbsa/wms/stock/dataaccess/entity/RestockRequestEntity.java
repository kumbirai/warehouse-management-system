package com.ccbsa.wms.stock.dataaccess.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.ccbsa.common.domain.valueobject.RestockPriority;
import com.ccbsa.wms.stock.domain.core.valueobject.RestockRequestStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity: RestockRequestEntity
 * <p>
 * JPA representation of RestockRequest aggregate.
 */
@Entity
@Table(name = "restock_requests", schema = "tenant_schema")
@Getter
@Setter
@NoArgsConstructor
public class RestockRequestEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", length = 255, nullable = false)
    private String tenantId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "location_id")
    private UUID locationId;

    @Column(name = "current_quantity", nullable = false, precision = 18, scale = 2)
    private BigDecimal currentQuantity;

    @Column(name = "minimum_quantity", nullable = false, precision = 18, scale = 2)
    private BigDecimal minimumQuantity;

    @Column(name = "maximum_quantity", precision = 18, scale = 2)
    private BigDecimal maximumQuantity;

    @Column(name = "requested_quantity", nullable = false, precision = 18, scale = 2)
    private BigDecimal requestedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 50, nullable = false)
    private RestockPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    private RestockRequestStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_to_d365_at")
    private LocalDateTime sentToD365At;

    @Column(name = "d365_order_reference", length = 255)
    private String d365OrderReference;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
