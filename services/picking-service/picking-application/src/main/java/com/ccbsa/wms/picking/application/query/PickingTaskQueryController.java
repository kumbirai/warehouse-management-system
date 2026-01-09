package com.ccbsa.wms.picking.application.query;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ccbsa.common.application.api.ApiMeta;
import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.application.api.ApiResponseBuilder;
import com.ccbsa.wms.picking.application.dto.mapper.PickingTaskDTOMapper;
import com.ccbsa.wms.picking.application.dto.query.ListPickingTasksQueryResultDTO;
import com.ccbsa.wms.picking.application.service.query.ListPickingTasksQueryHandler;
import com.ccbsa.wms.picking.application.service.query.dto.ListPickingTasksQuery;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller: PickingTaskQueryController
 * <p>
 * Handles picking task query operations (read operations).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/picking/tasks")
@Tag(name = "Picking Task Queries", description = "Picking task query operations")
@RequiredArgsConstructor
public class PickingTaskQueryController {
    private final ListPickingTasksQueryHandler listQueryHandler;
    private final PickingTaskDTOMapper mapper;

    @GetMapping
    @Operation(summary = "List Picking Tasks", description = "Lists picking tasks with filtering and pagination")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'PICKING_MANAGER', 'OPERATOR', 'PICKING_CLERK', 'VIEWER', 'SERVICE')")
    public ResponseEntity<ApiResponse<ListPickingTasksQueryResultDTO>> listPickingTasks(@RequestHeader("X-Tenant-Id") String tenantId,
                                                                                        @RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page,
                                                                                        @RequestParam(defaultValue = "10") int size) {
        ListPickingTasksQuery query = mapper.toListQuery(tenantId, status, page, size);
        var result = listQueryHandler.handle(query);
        ListPickingTasksQueryResultDTO resultDTO = mapper.toListQueryResultDTO(result);

        // Add pagination metadata
        ApiMeta.Pagination pagination = ApiMeta.Pagination.of(result.getPage(), result.getSize(), result.getTotalElements());
        ApiMeta meta = ApiMeta.builder().pagination(pagination).build();

        return ApiResponseBuilder.ok(resultDTO, null, meta);
    }
}
