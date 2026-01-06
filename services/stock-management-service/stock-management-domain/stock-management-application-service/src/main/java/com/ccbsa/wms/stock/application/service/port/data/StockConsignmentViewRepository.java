package com.ccbsa.wms.stock.application.service.port.data;

import java.util.List;
import java.util.Optional;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.stock.application.service.port.data.dto.StockConsignmentView;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentId;

/**
 * Data Port: StockConsignmentViewRepository
 * <p>
 * Read model repository for stock consignment queries. Provides optimized read access to consignment data.
 * <p>
 * This is a data port (read model) used by query handlers, not a repository port (write model).
 * <p>
 * Responsibilities:
 * - Provide optimized read model queries
 * - Support eventual consistency (read model may lag behind write model)
 * - Enable query performance optimization through denormalization
 */
public interface StockConsignmentViewRepository {

    /**
     * Finds a consignment view by tenant ID and consignment ID.
     *
     * @param tenantId      Tenant ID
     * @param consignmentId Consignment ID
     * @return Optional StockConsignmentView
     */
    Optional<StockConsignmentView> findByTenantIdAndId(TenantId tenantId, ConsignmentId consignmentId);

    /**
     * Finds all consignment views for a tenant with pagination.
     *
     * @param tenantId Tenant ID
     * @param page     Page number (0-based)
     * @param size     Page size
     * @return List of StockConsignmentView
     */
    List<StockConsignmentView> findByTenantId(TenantId tenantId, int page, int size);

    /**
     * Finds consignment views for a tenant with pagination and optional expiration filtering.
     *
     * @param tenantId           Tenant ID
     * @param page               Page number (0-based)
     * @param size               Page size
     * @param expiringWithinDays Optional number of days to filter consignments expiring within (null to disable filtering)
     * @return List of StockConsignmentView
     */
    List<StockConsignmentView> findByTenantId(TenantId tenantId, int page, int size, Integer expiringWithinDays);

    /**
     * Counts consignment views for a tenant.
     *
     * @param tenantId Tenant ID
     * @return Total count
     */
    long countByTenantId(TenantId tenantId);

    /**
     * Counts consignment views for a tenant with optional expiration filtering.
     *
     * @param tenantId           Tenant ID
     * @param expiringWithinDays Optional number of days to filter consignments expiring within (null to disable filtering)
     * @return Total count
     */
    long countByTenantId(TenantId tenantId, Integer expiringWithinDays);
}

