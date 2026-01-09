package com.ccbsa.wms.picking.application.command;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.application.api.ApiResponseBuilder;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.picking.application.dto.command.CreatePickingTaskCommandDTO;
import com.ccbsa.wms.picking.application.dto.command.CreatePickingTaskResultDTO;
import com.ccbsa.wms.picking.application.dto.command.ExecutePickingTaskCommandDTO;
import com.ccbsa.wms.picking.application.dto.command.ExecutePickingTaskResultDTO;
import com.ccbsa.wms.picking.application.dto.mapper.PickingTaskDTOMapper;
import com.ccbsa.wms.picking.application.service.command.CreatePickingTaskCommandHandler;
import com.ccbsa.wms.picking.application.service.command.ExecutePickingTaskCommandHandler;
import com.ccbsa.wms.picking.application.service.command.dto.CreatePickingTaskCommand;
import com.ccbsa.wms.picking.application.service.command.dto.CreatePickingTaskResult;
import com.ccbsa.wms.picking.application.service.command.dto.ExecutePickingTaskCommand;
import com.ccbsa.wms.picking.domain.core.valueobject.PartialReason;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingTaskId;

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
@RequestMapping("/api/v1/picking/picking-tasks")
@Tag(name = "Picking Task Commands", description = "Picking task command operations")
@RequiredArgsConstructor
public class PickingTaskCommandController {
    private final CreatePickingTaskCommandHandler createCommandHandler;
    private final ExecutePickingTaskCommandHandler executePickingTaskCommandHandler;
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

    @PostMapping("/{pickingTaskId}/execute")
    @Operation(summary = "Execute Picking Task", description = "Executes a picking task with the specified quantity")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'PICKING_MANAGER', 'OPERATOR', 'PICKING_CLERK', 'SERVICE')")
    public ResponseEntity<ApiResponse<ExecutePickingTaskResultDTO>> executePickingTask(@PathVariable UUID pickingTaskId, @RequestHeader("X-Tenant-Id") String tenantId,
                                                                                       @Valid @RequestBody ExecutePickingTaskCommandDTO requestDTO,
                                                                                       @AuthenticationPrincipal Jwt jwt) {

        String username = jwt.getClaimAsString("preferred_username");
        if (username == null || username.isEmpty()) {
            username = jwt.getSubject();
        }
        log.info("Executing picking task: {} by user: {}", pickingTaskId, username);

        ExecutePickingTaskCommand command = ExecutePickingTaskCommand.builder().pickingTaskId(PickingTaskId.of(pickingTaskId)).tenantId(TenantId.of(tenantId))
                .pickedQuantity(Quantity.of(requestDTO.getPickedQuantity())).isPartialPicking(requestDTO.getIsPartialPicking() != null && requestDTO.getIsPartialPicking())
                .partialReason(requestDTO.getPartialReason() != null ? PartialReason.of(requestDTO.getPartialReason()) : null).pickedByUserId(UserId.of(username)).build();

        com.ccbsa.wms.picking.application.service.command.dto.ExecutePickingTaskResult result = executePickingTaskCommandHandler.handle(command);

        ExecutePickingTaskResultDTO resultDTO = ExecutePickingTaskResultDTO.builder().pickingTaskId(result.getPickingTaskId().getValueAsString()).status(result.getStatus())
                .pickedQuantity(result.getPickedQuantity()).isPartialPicking(result.isPartialPicking()).build();

        return ApiResponseBuilder.ok(resultDTO);
    }
}
