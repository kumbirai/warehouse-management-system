package com.ccbsa.wms.picking.dataaccess.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ccbsa.wms.picking.domain.core.valueobject.PickingTaskStatus;

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
 * JPA Entity: PickingTaskEntity
 * <p>
 * JPA representation of PickingTask entity.
 */
@Entity
@Table(name = "picking_tasks", schema = "tenant_schema")
@Getter
@Setter
@NoArgsConstructor
public class PickingTaskEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "load_id", nullable = false)
    private UUID loadId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "product_code", length = 100, nullable = false)
    private String productCode;

    @Column(name = "location_id", nullable = false)
    private UUID locationId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    private PickingTaskStatus status;

    @Column(name = "sequence", nullable = false)
    private int sequence;

    @Column(name = "picked_quantity")
    private Integer pickedQuantity;

    @Column(name = "picked_by_user_id", length = 255)
    private String pickedByUserId;

    @Column(name = "picked_at")
    private LocalDateTime pickedAt;

    @Column(name = "is_partial_picking")
    private Boolean isPartialPicking;

    @Column(name = "partial_reason", length = 500)
    private String partialReason;
}
