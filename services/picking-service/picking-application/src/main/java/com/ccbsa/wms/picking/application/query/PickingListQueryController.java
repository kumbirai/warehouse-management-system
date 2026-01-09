package com.ccbsa.wms.picking.application.query;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ccbsa.common.application.api.ApiMeta;
import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.application.api.ApiResponseBuilder;
import com.ccbsa.wms.picking.application.dto.mapper.PickingListDTOMapper;
import com.ccbsa.wms.picking.application.dto.query.ListPickingListsQueryResultDTO;
import com.ccbsa.wms.picking.application.dto.query.PickingListQueryResultDTO;
import com.ccbsa.wms.picking.application.service.query.GetPickingListQueryHandler;
import com.ccbsa.wms.picking.application.service.query.ListPickingListsQueryHandler;
import com.ccbsa.wms.picking.application.service.query.dto.GetPickingListQuery;
import com.ccbsa.wms.picking.application.service.query.dto.ListPickingListsQuery;
import com.ccbsa.wms.picking.application.service.query.dto.ListPickingListsQueryResult;
import com.ccbsa.wms.picking.application.service.query.dto.PickingListQueryResult;
import com.ccbsa.wms.picking.domain.core.exception.PickingListNotFoundException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller: PickingListQueryController
 * <p>
 * Handles picking list query operations (read operations).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/picking/picking-lists")
@Tag(name = "Picking List Queries", description = "Picking list query operations")
@RequiredArgsConstructor
public class PickingListQueryController {
    private final GetPickingListQueryHandler getQueryHandler;
    private final ListPickingListsQueryHandler listQueryHandler;
    private final PickingListDTOMapper mapper;

    @GetMapping("/{id}")
    @Operation(summary = "Get Picking List by ID", description = "Retrieves a picking list by ID")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'PICKING_MANAGER', 'OPERATOR', 'PICKING_CLERK', 'VIEWER', 'SERVICE')")
    public ResponseEntity<ApiResponse<PickingListQueryResultDTO>> getPickingList(@PathVariable String id, @RequestHeader("X-Tenant-Id") String tenantId) {
        try {
            GetPickingListQuery query = mapper.toGetQuery(id, tenantId);
            PickingListQueryResult result = getQueryHandler.handle(query);
            PickingListQueryResultDTO resultDTO = mapper.toQueryResultDTO(result);
            return ApiResponseBuilder.ok(resultDTO);
        } catch (IllegalArgumentException e) {
            // If the error is about invalid UUID format, treat it as resource not found (404)
            // This matches REST API best practices where invalid resource IDs return 404
            if (e.getMessage() != null && e.getMessage().contains("Invalid UUID format")) {
                log.debug("Invalid UUID format for picking list ID: {}, treating as not found", id);
                throw new PickingListNotFoundException(id);
            }
            // Re-throw other IllegalArgumentException to be handled by global exception handler
            throw e;
        }
    }

    @GetMapping
    @Operation(summary = "List Picking Lists", description = "Lists picking lists with filtering and pagination")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'PICKING_MANAGER', 'OPERATOR', 'PICKING_CLERK', 'VIEWER', 'SERVICE')")
    public ResponseEntity<ApiResponse<ListPickingListsQueryResultDTO>> listPickingLists(@RequestHeader("X-Tenant-Id") String tenantId,
                                                                                        @RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page,
                                                                                        @RequestParam(defaultValue = "20") int size) {
        // Validate input parameters
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("X-Tenant-Id header is required");
        }
        if (page < 0) {
            throw new IllegalArgumentException("Page number must be non-negative");
        }
        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }

        log.debug("Listing picking lists for tenant: {}, status: {}, page: {}, size: {}", tenantId, status, page, size);

        try {
            ListPickingListsQuery query = mapper.toListQuery(tenantId, status, page, size);
            ListPickingListsQueryResult result = listQueryHandler.handle(query);
            ListPickingListsQueryResultDTO resultDTO = mapper.toListQueryResultDTO(result);

            // Add pagination metadata
            ApiMeta.Pagination pagination = ApiMeta.Pagination.of(result.getPage(), result.getSize(), result.getTotalElements());
            ApiMeta meta = ApiMeta.builder().pagination(pagination).build();

            return ApiResponseBuilder.ok(resultDTO, null, meta);
        } catch (IllegalStateException e) {
            log.error("TenantContext error while listing picking lists for tenant: {}, status: {}, page: {}, size: {}", tenantId, status, page, size, e);
            throw new IllegalStateException("Tenant context error: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            log.error("Runtime error while listing picking lists for tenant: {}, status: {}, page: {}, size: {}. Error: {}", tenantId, status, page, size, e.getMessage(), e);
            // Re-throw to let global exception handler process it with full exception chain
            throw e;
        }
    }
}
