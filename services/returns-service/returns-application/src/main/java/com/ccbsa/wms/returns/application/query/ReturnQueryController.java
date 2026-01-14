package com.ccbsa.wms.returns.application.query;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.application.api.ApiResponseBuilder;
import com.ccbsa.common.domain.valueobject.ReturnStatus;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.returns.application.dto.mapper.ReturnDTOMapper;
import com.ccbsa.wms.returns.application.dto.query.ReturnQueryDTO;
import com.ccbsa.wms.returns.application.service.query.GetReturnQueryHandler;
import com.ccbsa.wms.returns.application.service.query.ListReturnsQueryHandler;
import com.ccbsa.wms.returns.application.service.query.dto.GetReturnQuery;
import com.ccbsa.wms.returns.application.service.query.dto.ListReturnsQuery;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller: ReturnQueryController
 * <p>
 * Handles return query operations (read operations).
 * <p>
 * Responsibilities:
 * - Get return by ID endpoints
 * - List returns with filtering and pagination
 * - Map query results to DTOs
 * - Return standardized API responses
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/returns")
@Tag(name = "Returns Management Queries", description = "Returns management query operations")
@RequiredArgsConstructor
public class ReturnQueryController {
    private final GetReturnQueryHandler getReturnQueryHandler;
    private final ListReturnsQueryHandler listReturnsQueryHandler;
    private final ReturnDTOMapper mapper;

    @GetMapping("/{returnId}")
    @Operation(summary = "Get Return by ID", description = "Retrieves a return by ID")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'WAREHOUSE_OPERATOR', 'VIEWER', 'SERVICE')")
    public ResponseEntity<ApiResponse<ReturnQueryDTO>> getReturn(@PathVariable("returnId") String returnId, @RequestHeader("X-Tenant-Id") String tenantId) {
        log.debug("Received get return request for returnId: {}, tenant: {}", returnId, tenantId);

        // Map to query
        GetReturnQuery query = GetReturnQuery.builder()
                .returnId(com.ccbsa.common.domain.valueobject.ReturnId.of(returnId))
                .tenantId(TenantId.of(tenantId))
                .build();

        // Execute query
        var result = getReturnQueryHandler.handle(query);

        // Map result to DTO
        ReturnQueryDTO dto = mapper.toReturnQueryDTO(result);

        return ApiResponseBuilder.ok(dto);
    }

    @GetMapping
    @Operation(summary = "List Returns", description = "Lists returns with optional status filter and pagination")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'WAREHOUSE_OPERATOR', 'VIEWER', 'SERVICE')")
    public ResponseEntity<ApiResponse<Page<ReturnQueryDTO>>> listReturns(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("Received list returns request for tenant: {}, status: {}, page: {}, size: {}", tenantId, status, page, size);

        // Map status string to enum
        ReturnStatus returnStatus = status != null ? ReturnStatus.valueOf(status) : null;

        // Map to query
        ListReturnsQuery query = ListReturnsQuery.builder()
                .tenantId(TenantId.of(tenantId))
                .status(returnStatus)
                .page(page)
                .size(size)
                .build();

        // Execute query
        var result = listReturnsQueryHandler.handle(query);

        // Map result to DTOs
        List<ReturnQueryDTO> returnDTOs = result.getReturns().stream()
                .map(mapper::toReturnQueryDTO)
                .collect(Collectors.toList());

        // Create page response
        PageImpl<ReturnQueryDTO> pageResponse = new PageImpl<>(returnDTOs, 
                PageRequest.of(page, size), 
                result.getTotalElements());

        return ApiResponseBuilder.ok(pageResponse);
    }
}
