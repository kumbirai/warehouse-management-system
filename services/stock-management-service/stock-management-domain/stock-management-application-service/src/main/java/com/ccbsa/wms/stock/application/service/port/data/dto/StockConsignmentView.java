package com.ccbsa.wms.stock.application.service.port.data.dto;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import com.ccbsa.common.domain.valueobject.WarehouseId;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentId;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentLineItem;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentReference;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentStatus;
import com.ccbsa.wms.stock.domain.core.valueobject.ReceivedBy;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;

/**
 * Read Model DTO: StockConsignmentView
 * <p>
 * Optimized read model representation of StockConsignment aggregate for query operations.
 * <p>
 * This is a denormalized view optimized for read queries, separate from the write model (StockConsignment aggregate).
 * <p>
 * Fields are flattened and optimized for query performance.
 */
@Getter
@Builder
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Lombok builder stores list directly. Defensive copy made in constructor and getter returns immutable view.")
public final class StockConsignmentView {
    private final ConsignmentId consignmentId;
    private final String tenantId;
    private final ConsignmentReference consignmentReference;
    private final WarehouseId warehouseId;
    private final ConsignmentStatus status;
    private final LocalDateTime receivedAt;
    private final LocalDateTime confirmedAt;
    private final ReceivedBy receivedBy;
    private final List<ConsignmentLineItem> lineItems;
    private final LocalDateTime createdAt;
    private final LocalDateTime lastModifiedAt;

    public StockConsignmentView(ConsignmentId consignmentId, String tenantId, ConsignmentReference consignmentReference, WarehouseId warehouseId, ConsignmentStatus status,
                                LocalDateTime receivedAt, LocalDateTime confirmedAt, ReceivedBy receivedBy, List<ConsignmentLineItem> lineItems, LocalDateTime createdAt,
                                LocalDateTime lastModifiedAt) {
        if (consignmentId == null) {
            throw new IllegalArgumentException("ConsignmentId is required");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (consignmentReference == null) {
            throw new IllegalArgumentException("ConsignmentReference is required");
        }
        if (warehouseId == null) {
            throw new IllegalArgumentException("WarehouseId is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status is required");
        }
        this.consignmentId = consignmentId;
        this.tenantId = tenantId;
        this.consignmentReference = consignmentReference;
        this.warehouseId = warehouseId;
        this.status = status;
        this.receivedAt = receivedAt;
        this.confirmedAt = confirmedAt;
        this.receivedBy = receivedBy;
        // Defensive copy to prevent external modification
        this.lineItems = lineItems != null ? List.copyOf(lineItems) : List.of();
        this.createdAt = createdAt;
        this.lastModifiedAt = lastModifiedAt;
    }

    /**
     * Returns an unmodifiable view of the line items list.
     *
     * @return Unmodifiable list of line items
     */
    public List<ConsignmentLineItem> getLineItems() {
        return Collections.unmodifiableList(lineItems);
    }
}

