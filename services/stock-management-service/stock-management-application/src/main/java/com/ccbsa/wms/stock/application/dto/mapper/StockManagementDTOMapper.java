package com.ccbsa.wms.stock.application.dto.mapper;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.StockItemId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.security.TenantContext;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.application.dto.command.AdjustStockCommandDTO;
import com.ccbsa.wms.stock.application.dto.command.AdjustStockResultDTO;
import com.ccbsa.wms.stock.application.dto.command.AllocateStockCommandDTO;
import com.ccbsa.wms.stock.application.dto.command.AllocateStockResultDTO;
import com.ccbsa.wms.stock.application.dto.command.ReleaseStockAllocationResultDTO;
import com.ccbsa.wms.stock.application.dto.query.ListStockAdjustmentsQueryResultDTO;
import com.ccbsa.wms.stock.application.dto.query.ListStockAllocationsQueryResultDTO;
import com.ccbsa.wms.stock.application.dto.query.StockAdjustmentQueryDTO;
import com.ccbsa.wms.stock.application.dto.query.StockAllocationQueryDTO;
import com.ccbsa.wms.stock.application.dto.query.StockLevelQueryDTO;
import com.ccbsa.wms.stock.application.service.command.dto.AdjustStockCommand;
import com.ccbsa.wms.stock.application.service.command.dto.AdjustStockResult;
import com.ccbsa.wms.stock.application.service.command.dto.AllocateStockCommand;
import com.ccbsa.wms.stock.application.service.command.dto.AllocateStockResult;
import com.ccbsa.wms.stock.application.service.command.dto.ReleaseStockAllocationCommand;
import com.ccbsa.wms.stock.application.service.command.dto.ReleaseStockAllocationResult;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockAdjustmentQuery;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockAdjustmentQueryResult;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockAllocationQuery;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockAllocationQueryResult;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockLevelsQuery;
import com.ccbsa.wms.stock.application.service.query.dto.GetStockLevelsQueryResult;
import com.ccbsa.wms.stock.application.service.query.dto.ListStockAdjustmentsQuery;
import com.ccbsa.wms.stock.application.service.query.dto.ListStockAdjustmentsQueryResult;
import com.ccbsa.wms.stock.application.service.query.dto.ListStockAllocationsQuery;
import com.ccbsa.wms.stock.application.service.query.dto.ListStockAllocationsQueryResult;
import com.ccbsa.wms.stock.domain.core.valueobject.AllocationStatus;
import com.ccbsa.wms.stock.domain.core.valueobject.StockAdjustmentId;
import com.ccbsa.wms.stock.domain.core.valueobject.StockAllocationId;

/**
 * DTO Mapper: StockManagementDTOMapper
 * <p>
 * Maps between API DTOs and application service commands/queries for StockAllocation, StockAdjustment, and StockLevelThreshold.
 * Acts as an anti-corruption layer protecting the domain from external API changes.
 */
@Component
public class StockManagementDTOMapper {

    // StockAllocation mapping methods

    /**
     * Converts AllocateStockCommandDTO to AllocateStockCommand.
     *
     * @param dto      Command DTO
     * @param tenantId Tenant identifier string
     * @return AllocateStockCommand
     */
    public AllocateStockCommand toAllocateStockCommand(AllocateStockCommandDTO dto, String tenantId) {
        com.ccbsa.common.domain.valueobject.UserId userId = TenantContext.getUserId();
        if (userId == null) {
            throw new IllegalStateException("User ID not found in TenantContext");
        }

        var builder = AllocateStockCommand.builder().tenantId(TenantId.of(tenantId)).productId(ProductId.of(dto.getProductId())).quantity(Quantity.of(dto.getQuantity()))
                .allocationType(dto.getAllocationType()).referenceId(dto.getReferenceId()).userId(userId).notes(dto.getNotes());

        if (dto.getLocationId() != null) {
            builder.locationId(LocationId.of(dto.getLocationId()));
        }

        return builder.build();
    }

    /**
     * Converts AllocateStockResult to AllocateStockResultDTO.
     *
     * @param result Command result
     * @return AllocateStockResultDTO
     */
    public AllocateStockResultDTO toAllocateStockResultDTO(AllocateStockResult result) {
        AllocateStockResultDTO dto = new AllocateStockResultDTO();
        dto.setAllocationId(result.getAllocationId().getValue());
        dto.setProductId(result.getProductId().getValue());
        if (result.getLocationId() != null) {
            dto.setLocationId(result.getLocationId().getValue());
        }
        dto.setStockItemId(result.getStockItemId().getValue());
        dto.setQuantity(result.getQuantity().getValue());
        dto.setAllocationType(result.getAllocationType());
        dto.setReferenceId(result.getReferenceId());
        dto.setStatus(result.getStatus());
        dto.setAllocatedAt(result.getAllocatedAt());
        return dto;
    }

    /**
     * Converts parameters to ReleaseStockAllocationCommand.
     *
     * @param allocationId Allocation ID string
     * @param tenantId     Tenant identifier string
     * @return ReleaseStockAllocationCommand
     */
    public ReleaseStockAllocationCommand toReleaseStockAllocationCommand(String allocationId, String tenantId) {
        return ReleaseStockAllocationCommand.builder().allocationId(StockAllocationId.of(UUID.fromString(allocationId))).tenantId(TenantId.of(tenantId)).build();
    }

    /**
     * Converts ReleaseStockAllocationResult to ReleaseStockAllocationResultDTO.
     *
     * @param result Command result
     * @return ReleaseStockAllocationResultDTO
     */
    public ReleaseStockAllocationResultDTO toReleaseStockAllocationResultDTO(ReleaseStockAllocationResult result) {
        ReleaseStockAllocationResultDTO dto = new ReleaseStockAllocationResultDTO();
        dto.setAllocationId(result.getAllocationId().getValue());
        dto.setStatus(result.getStatus());
        dto.setReleasedAt(result.getReleasedAt());
        return dto;
    }

    // StockAdjustment mapping methods

    /**
     * Converts AdjustStockCommandDTO to AdjustStockCommand.
     *
     * @param dto      Command DTO
     * @param tenantId Tenant identifier string
     * @return AdjustStockCommand
     */
    public AdjustStockCommand toAdjustStockCommand(AdjustStockCommandDTO dto, String tenantId) {
        com.ccbsa.common.domain.valueobject.UserId userId = TenantContext.getUserId();
        if (userId == null) {
            throw new IllegalStateException("User ID not found in TenantContext");
        }

        var builder = AdjustStockCommand.builder().tenantId(TenantId.of(tenantId)).productId(ProductId.of(dto.getProductId())).adjustmentType(dto.getAdjustmentType())
                .quantity(Quantity.of(dto.getQuantity())).reason(dto.getReason()).notes(dto.getNotes()).userId(userId).authorizationCode(dto.getAuthorizationCode());

        if (dto.getLocationId() != null) {
            builder.locationId(LocationId.of(dto.getLocationId()));
        }
        if (dto.getStockItemId() != null) {
            builder.stockItemId(StockItemId.of(dto.getStockItemId()));
        }

        return builder.build();
    }

    /**
     * Converts AdjustStockResult to AdjustStockResultDTO.
     *
     * @param result Command result
     * @return AdjustStockResultDTO
     */
    public AdjustStockResultDTO toAdjustStockResultDTO(AdjustStockResult result) {
        AdjustStockResultDTO dto = new AdjustStockResultDTO();
        dto.setAdjustmentId(result.getAdjustmentId().getValue());
        dto.setQuantityBefore(result.getQuantityBefore());
        dto.setQuantityAfter(result.getQuantityAfter());
        dto.setAdjustedAt(result.getAdjustedAt());
        return dto;
    }

    // Query mapping methods for StockAllocation

    /**
     * Converts parameters to GetStockAllocationQuery.
     *
     * @param allocationId Allocation ID string
     * @param tenantId     Tenant identifier string
     * @return GetStockAllocationQuery
     */
    public GetStockAllocationQuery toGetStockAllocationQuery(String allocationId, String tenantId) {
        return GetStockAllocationQuery.builder().allocationId(StockAllocationId.of(UUID.fromString(allocationId))).tenantId(TenantId.of(tenantId)).build();
    }

    /**
     * Converts parameters to ListStockAllocationsQuery.
     *
     * @param tenantId    Tenant identifier string
     * @param productId   Optional product ID string
     * @param locationId  Optional location ID string
     * @param referenceId Optional reference ID string
     * @param status      Optional status string
     * @param page        Page number
     * @param size        Page size
     * @return ListStockAllocationsQuery
     */
    public ListStockAllocationsQuery toListStockAllocationsQuery(String tenantId, String productId, String locationId, String referenceId, String status, Integer page,
                                                                 Integer size) {
        var builder = ListStockAllocationsQuery.builder().tenantId(TenantId.of(tenantId)).page(page).size(size);

        if (productId != null && !productId.isEmpty()) {
            builder.productId(ProductId.of(productId));
        }
        if (locationId != null && !locationId.isEmpty()) {
            builder.locationId(LocationId.of(locationId));
        }
        if (referenceId != null && !referenceId.isEmpty()) {
            builder.referenceId(referenceId);
        }
        if (status != null && !status.isEmpty()) {
            builder.status(AllocationStatus.valueOf(status));
        }

        return builder.build();
    }

    /**
     * Converts ListStockAllocationsQueryResult to ListStockAllocationsQueryResultDTO.
     *
     * @param result Query result
     * @return ListStockAllocationsQueryResultDTO
     */
    public ListStockAllocationsQueryResultDTO toListStockAllocationsQueryResultDTO(ListStockAllocationsQueryResult result) {
        ListStockAllocationsQueryResultDTO dto = new ListStockAllocationsQueryResultDTO();
        List<StockAllocationQueryDTO> allocationDTOs = result.getAllocations().stream().map(this::toStockAllocationQueryDTO).collect(Collectors.toList());
        dto.setAllocations(allocationDTOs);
        dto.setTotalCount(result.getTotalCount());
        return dto;
    }

    /**
     * Converts GetStockAllocationQueryResult to StockAllocationQueryDTO.
     *
     * @param result Query result
     * @return StockAllocationQueryDTO
     */
    public StockAllocationQueryDTO toStockAllocationQueryDTO(GetStockAllocationQueryResult result) {
        StockAllocationQueryDTO dto = new StockAllocationQueryDTO();
        dto.setAllocationId(result.getAllocationId().getValueAsString());
        dto.setProductId(result.getProductId().getValueAsString());
        if (result.getLocationId() != null) {
            dto.setLocationId(result.getLocationId().getValueAsString());
        }
        if (result.getStockItemId() != null) {
            dto.setStockItemId(result.getStockItemId().getValueAsString());
        }
        dto.setQuantity(result.getQuantity().getValue());
        dto.setAllocationType(result.getAllocationType().name());
        dto.setReferenceId(result.getReferenceId());
        dto.setStatus(result.getStatus().name());
        dto.setAllocatedAt(result.getAllocatedAt());
        dto.setReleasedAt(result.getReleasedAt());
        if (result.getAllocatedBy() != null) {
            dto.setAllocatedBy(result.getAllocatedBy().getValue());
        }
        dto.setNotes(result.getNotes());
        return dto;
    }

    // Query mapping methods for StockAdjustment

    /**
     * Converts parameters to GetStockAdjustmentQuery.
     *
     * @param adjustmentId Adjustment ID string
     * @param tenantId     Tenant identifier string
     * @return GetStockAdjustmentQuery
     */
    public GetStockAdjustmentQuery toGetStockAdjustmentQuery(String adjustmentId, String tenantId) {
        return GetStockAdjustmentQuery.builder().adjustmentId(StockAdjustmentId.of(UUID.fromString(adjustmentId))).tenantId(TenantId.of(tenantId)).build();
    }

    /**
     * Converts parameters to ListStockAdjustmentsQuery.
     *
     * @param tenantId    Tenant identifier string
     * @param productId   Optional product ID string
     * @param locationId  Optional location ID string
     * @param stockItemId Optional stock item ID string
     * @param page        Page number
     * @param size        Page size
     * @return ListStockAdjustmentsQuery
     */
    public ListStockAdjustmentsQuery toListStockAdjustmentsQuery(String tenantId, String productId, String locationId, String stockItemId, Integer page, Integer size) {
        var builder = ListStockAdjustmentsQuery.builder().tenantId(TenantId.of(tenantId)).page(page).size(size);

        if (productId != null && !productId.isEmpty()) {
            builder.productId(ProductId.of(productId));
        }
        if (locationId != null && !locationId.isEmpty()) {
            builder.locationId(LocationId.of(locationId));
        }
        if (stockItemId != null && !stockItemId.isEmpty()) {
            builder.stockItemId(StockItemId.of(UUID.fromString(stockItemId)));
        }

        return builder.build();
    }

    /**
     * Converts ListStockAdjustmentsQueryResult to ListStockAdjustmentsQueryResultDTO.
     *
     * @param result Query result
     * @return ListStockAdjustmentsQueryResultDTO
     */
    public ListStockAdjustmentsQueryResultDTO toListStockAdjustmentsQueryResultDTO(ListStockAdjustmentsQueryResult result) {
        ListStockAdjustmentsQueryResultDTO dto = new ListStockAdjustmentsQueryResultDTO();
        List<StockAdjustmentQueryDTO> adjustmentDTOs = result.getAdjustments().stream().map(this::toStockAdjustmentQueryDTO).collect(Collectors.toList());
        dto.setAdjustments(adjustmentDTOs);
        dto.setTotalCount(result.getTotalCount());
        return dto;
    }

    /**
     * Converts GetStockAdjustmentQueryResult to StockAdjustmentQueryDTO.
     *
     * @param result Query result
     * @return StockAdjustmentQueryDTO
     */
    public StockAdjustmentQueryDTO toStockAdjustmentQueryDTO(GetStockAdjustmentQueryResult result) {
        StockAdjustmentQueryDTO dto = new StockAdjustmentQueryDTO();
        dto.setAdjustmentId(result.getAdjustmentId().getValueAsString());
        dto.setProductId(result.getProductId().getValueAsString());
        if (result.getLocationId() != null) {
            dto.setLocationId(result.getLocationId().getValueAsString());
        }
        if (result.getStockItemId() != null) {
            dto.setStockItemId(result.getStockItemId().getValueAsString());
        }
        dto.setAdjustmentType(result.getAdjustmentType().name());
        dto.setQuantity(result.getQuantity().getValue());
        dto.setQuantityBefore(result.getQuantityBefore());
        dto.setQuantityAfter(result.getQuantityAfter());
        dto.setReason(result.getReason().name());
        dto.setNotes(result.getNotes());
        if (result.getAdjustedBy() != null) {
            dto.setAdjustedBy(result.getAdjustedBy().getValue());
        }
        dto.setAuthorizationCode(result.getAuthorizationCode());
        dto.setAdjustedAt(result.getAdjustedAt());
        return dto;
    }

    // Stock Level mapping methods

    /**
     * Converts query parameters to GetStockLevelsQuery.
     *
     * @param tenantId   Tenant identifier string
     * @param productId  Product identifier string
     * @param locationId Location identifier string (optional)
     * @return GetStockLevelsQuery
     */
    public GetStockLevelsQuery toGetStockLevelsQuery(String tenantId, String productId, String locationId) {
        var builder = GetStockLevelsQuery.builder().tenantId(TenantId.of(tenantId)).productId(ProductId.of(UUID.fromString(productId)));

        if (locationId != null && !locationId.trim().isEmpty()) {
            builder.locationId(LocationId.of(UUID.fromString(locationId)));
        }

        return builder.build();
    }

    /**
     * Converts GetStockLevelsQueryResult to list of StockLevelQueryDTO.
     *
     * @param result Query result
     * @return List of StockLevelQueryDTO
     */
    public List<StockLevelQueryDTO> toStockLevelQueryDTOList(GetStockLevelsQueryResult result) {
        return result.getStockLevels().stream().map(this::toStockLevelQueryDTO).collect(Collectors.toList());
    }

    /**
     * Converts StockLevelResult to StockLevelQueryDTO.
     *
     * @param stockLevel Stock level result
     * @return StockLevelQueryDTO
     */
    private StockLevelQueryDTO toStockLevelQueryDTO(GetStockLevelsQueryResult.StockLevelResult stockLevel) {
        // Generate a unique ID for this stock level snapshot
        String stockLevelId = UUID.randomUUID().toString();

        return StockLevelQueryDTO.builder().stockLevelId(stockLevelId).productId(stockLevel.getProductId()).locationId(stockLevel.getLocationId())
                .totalQuantity(stockLevel.getTotalQuantity()).allocatedQuantity(stockLevel.getAllocatedQuantity()).availableQuantity(stockLevel.getAvailableQuantity())
                .minimumQuantity(stockLevel.getMinimumQuantity()).maximumQuantity(stockLevel.getMaximumQuantity()).build();
    }
}

