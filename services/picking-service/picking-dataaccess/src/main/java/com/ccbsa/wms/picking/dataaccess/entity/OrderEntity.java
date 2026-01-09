package com.ccbsa.wms.picking.dataaccess.entity;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.ccbsa.common.domain.valueobject.Priority;
import com.ccbsa.wms.picking.domain.core.valueobject.OrderStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity: OrderEntity
 * <p>
 * JPA representation of Order entity.
 */
@Entity
@Table(name = "orders", schema = "tenant_schema")
@Getter
@Setter
@NoArgsConstructor
public class OrderEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "order_number", length = 50, nullable = false)
    private String orderNumber;

    @Column(name = "customer_code", length = 50, nullable = false)
    private String customerCode;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 20, nullable = false)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    @Column(name = "completed_at")
    private ZonedDateTime completedAt;

    @ManyToOne
    @JoinColumn(name = "load_id", nullable = false)
    private LoadEntity load;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLineItemEntity> lineItems = new ArrayList<>();
}
