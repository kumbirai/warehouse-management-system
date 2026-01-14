package com.ccbsa.wms.returns.dataaccess.entity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.ccbsa.common.domain.valueobject.ReturnStatus;
import com.ccbsa.wms.returns.domain.core.valueobject.ReturnType;

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
 * JPA Entity: ReturnEntity
 * <p>
 * JPA representation of Return aggregate. Uses tenant schema resolver for multi-tenant isolation (schema-per-tenant strategy).
 * <p>
 * This entity maps to the Return domain aggregate and uses domain enums directly to maintain consistency between domain and persistence layers.
 * <p>
 * The schema "tenant_schema" is a placeholder that will be dynamically replaced with the actual tenant schema at runtime by TenantAwarePhysicalNamingStrategy.
 */
@Entity
@Table(name = "returns", schema = "tenant_schema")
@Getter
@Setter
@NoArgsConstructor
public class ReturnEntity {
    @Id
    @Column(name = "return_id", nullable = false)
    private UUID returnId;

    @Column(name = "tenant_id", length = 255, nullable = false)
    private String tenantId;

    @Column(name = "order_number", length = 50, nullable = false)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "return_type", length = 50, nullable = false)
    private ReturnType returnType;

    @Enumerated(EnumType.STRING)
    @Column(name = "return_status", length = 50, nullable = false)
    private ReturnStatus returnStatus;

    @Column(name = "customer_signature", columnDefinition = "TEXT")
    private String customerSignature;

    @Column(name = "signature_timestamp")
    private Instant signatureTimestamp;

    @Column(name = "primary_return_reason", length = 50)
    private String primaryReturnReason;

    @Column(name = "return_notes", length = 2000)
    private String returnNotes;

    @Column(name = "returned_at", nullable = false)
    private LocalDateTime returnedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_modified_at", nullable = false)
    private LocalDateTime lastModifiedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @OneToMany(mappedBy = "returnEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReturnLineItemEntity> lineItems = new ArrayList<>();
}
