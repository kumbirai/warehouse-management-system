package com.ccbsa.wms.picking.dataaccess.entity;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.ccbsa.wms.picking.domain.core.valueobject.PickingListStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity: PickingListEntity
 * <p>
 * JPA representation of PickingList aggregate. Uses tenant schema resolver for multi-tenant isolation (schema-per-tenant strategy).
 */
@Entity
@Table(name = "picking_lists", schema = "tenant_schema")
@Getter
@Setter
@NoArgsConstructor
public class PickingListEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", length = 255, nullable = false)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    private PickingListStatus status;

    @Column(name = "received_at", nullable = false)
    private ZonedDateTime receivedAt;

    @Column(name = "processed_at")
    private ZonedDateTime processedAt;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "picking_list_reference", length = 50)
    private String pickingListReference;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @OneToMany(mappedBy = "pickingList", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LoadEntity> loads = new ArrayList<>();
}
