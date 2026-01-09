package com.ccbsa.wms.picking.application.command;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.application.api.ApiResponseBuilder;
import com.ccbsa.wms.picking.application.dto.command.CreatePickingTaskCommandDTO;
import com.ccbsa.wms.picking.application.dto.command.CreatePickingTaskResultDTO;
import com.ccbsa.wms.picking.application.dto.mapper.PickingTaskDTOMapper;
import com.ccbsa.wms.picking.application.service.command.CreatePickingTaskCommandHandler;
import com.ccbsa.wms.picking.application.service.command.dto.CreatePickingTaskCommand;
import com.ccbsa.wms.picking.application.service.command.dto.CreatePickingTaskResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller: PickingTaskCommandController
 * <p>
 * Handles picking task command operations (write operations).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/picking/tasks")
@Tag(name = "Picking Task Commands", description = "Picking task command operations")
@RequiredArgsConstructor
public class PickingTaskCommandController {
    private final CreatePickingTaskCommandHandler createCommandHandler;
    private final PickingTaskDTOMapper mapper;

    @PostMapping
    @Operation(summary = "Create Picking Task", description = "Creates a new picking task")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'PICKING_MANAGER', 'OPERATOR', 'PICKING_CLERK', 'SERVICE')")
    public ResponseEntity<ApiResponse<CreatePickingTaskResultDTO>> createPickingTask(@RequestHeader("X-Tenant-Id") String tenantId,
                                                                                     @Valid @org.springframework.web.bind.annotation.RequestBody
                                                                                     CreatePickingTaskCommandDTO commandDTO) {
        CreatePickingTaskCommand command = mapper.toCreateCommand(commandDTO, tenantId);
        CreatePickingTaskResult result = createCommandHandler.handle(command);
        CreatePickingTaskResultDTO resultDTO = mapper.toCreateResultDTO(result);
        return ApiResponseBuilder.created(resultDTO);
    }
}
