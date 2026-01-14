package com.ccbsa.wms.returns.dataaccess.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ccbsa.common.domain.valueobject.ProductCondition;
import com.ccbsa.common.domain.valueobject.ReturnReason;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity: ReturnLineItemEntity
 * <p>
 * JPA representation of ReturnLineItem entity. Uses tenant schema resolver for multi-tenant isolation (schema-per-tenant strategy).
 * <p>
 * This entity maps to the ReturnLineItem domain entity and represents a line item within a return.
 * <p>
 * The schema "tenant_schema" is a placeholder that will be dynamically replaced with the actual tenant schema at runtime by TenantAwarePhysicalNamingStrategy.
 */
@Entity
@Table(name = "return_line_items", schema = "tenant_schema")
@Getter
@Setter
@NoArgsConstructor
public class ReturnLineItemEntity {
    @Id
    @Column(name = "line_item_id", nullable = false)
    private UUID lineItemId;

    @ManyToOne
    @JoinColumn(name = "return_id", nullable = false)
    private ReturnEntity returnEntity;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "ordered_quantity", nullable = false)
    private int orderedQuantity;

    @Column(name = "picked_quantity", nullable = false)
    private int pickedQuantity;

    @Column(name = "accepted_quantity", nullable = false)
    private int acceptedQuantity;

    @Column(name = "returned_quantity", nullable = false)
    private int returnedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_condition", length = 50)
    private ProductCondition productCondition;

    @Enumerated(EnumType.STRING)
    @Column(name = "return_reason", length = 50)
    private ReturnReason returnReason;

    @Column(name = "line_notes", length = 1000)
    private String lineNotes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
