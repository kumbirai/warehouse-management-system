package com.ccbsa.wms.picking.application.command;

import java.io.IOException;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.application.api.ApiResponseBuilder;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.picking.application.dto.command.CompletePickingListResultDTO;
import com.ccbsa.wms.picking.application.dto.command.CreatePickingListCommandDTO;
import com.ccbsa.wms.picking.application.dto.command.CreatePickingListResultDTO;
import com.ccbsa.wms.picking.application.dto.command.UploadPickingListCsvResultDTO;
import com.ccbsa.wms.picking.application.dto.mapper.PickingListDTOMapper;
import com.ccbsa.wms.picking.application.service.command.CompletePickingListCommandHandler;
import com.ccbsa.wms.picking.application.service.command.CreatePickingListCommandHandler;
import com.ccbsa.wms.picking.application.service.command.UploadPickingListCsvCommandHandler;
import com.ccbsa.wms.picking.application.service.command.dto.CompletePickingListCommand;
import com.ccbsa.wms.picking.application.service.command.dto.CompletePickingListResult;
import com.ccbsa.wms.picking.application.service.command.dto.CreatePickingListCommand;
import com.ccbsa.wms.picking.application.service.command.dto.CreatePickingListResult;
import com.ccbsa.wms.picking.application.service.command.dto.CsvUploadResult;
import com.ccbsa.wms.picking.application.service.command.dto.UploadPickingListCsvCommand;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingListId;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller: PickingListCommandController
 * <p>
 * Handles picking list command operations (write operations).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/picking/picking-lists")
@Tag(name = "Picking List Commands", description = "Picking list command operations")
@RequiredArgsConstructor
public class PickingListCommandController {
    private final UploadPickingListCsvCommandHandler uploadCsvCommandHandler;
    private final CreatePickingListCommandHandler createCommandHandler;
    private final CompletePickingListCommandHandler completePickingListCommandHandler;
    private final PickingListDTOMapper mapper;

    @PostMapping("/upload-csv")
    @Operation(summary = "Upload Picking List CSV", description = "Uploads picking list data via CSV file")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'PICKING_MANAGER', 'OPERATOR', 'PICKING_CLERK', 'SERVICE')")
    public ResponseEntity<ApiResponse<UploadPickingListCsvResultDTO>> uploadPickingListCsv(@RequestHeader("X-Tenant-Id") String tenantId,
                                                                                           @RequestParam("file") MultipartFile file) {
        try {
            UploadPickingListCsvCommand command = mapper.toUploadCsvCommand(file, tenantId);
            CsvUploadResult result = uploadCsvCommandHandler.handle(command);
            UploadPickingListCsvResultDTO resultDTO = mapper.toUploadCsvResultDTO(result);
            return ApiResponseBuilder.ok(resultDTO);
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Failed to read CSV file: %s", e.getMessage()), e);
        }
    }

    @PostMapping
    @Operation(summary = "Create Picking List", description = "Creates a new picking list manually")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'PICKING_MANAGER', 'OPERATOR', 'PICKING_CLERK', 'SERVICE')")
    public ResponseEntity<ApiResponse<CreatePickingListResultDTO>> createPickingList(@RequestHeader("X-Tenant-Id") String tenantId,
                                                                                     @Valid @org.springframework.web.bind.annotation.RequestBody
                                                                                     CreatePickingListCommandDTO commandDTO) {
        CreatePickingListCommand command = mapper.toCreateCommand(commandDTO, tenantId);
        CreatePickingListResult result = createCommandHandler.handle(command);
        CreatePickingListResultDTO resultDTO = mapper.toCreateResultDTO(result);
        return ApiResponseBuilder.created(resultDTO);
    }

    @PostMapping("/{pickingListId}/complete")
    @Operation(summary = "Complete Picking List", description = "Completes a picking list after all tasks are executed")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'PICKING_MANAGER', 'OPERATOR', 'PICKING_CLERK', 'SERVICE')")
    public ResponseEntity<ApiResponse<CompletePickingListResultDTO>> completePickingList(@PathVariable UUID pickingListId, @RequestHeader("X-Tenant-Id") String tenantId,
                                                                                         @AuthenticationPrincipal Jwt jwt) {

        String username = jwt.getClaimAsString("preferred_username");
        if (username == null || username.isEmpty()) {
            username = jwt.getSubject();
        }
        log.info("Completing picking list: {} by user: {}", pickingListId, username);

        CompletePickingListCommand command =
                CompletePickingListCommand.builder().pickingListId(PickingListId.of(pickingListId)).tenantId(TenantId.of(tenantId)).completedByUserId(UserId.of(username)).build();

        CompletePickingListResult result = completePickingListCommandHandler.handle(command);

        CompletePickingListResultDTO resultDTO =
                CompletePickingListResultDTO.builder().pickingListId(result.getPickingListId().getValueAsString()).status(result.getStatus()).build();

        return ApiResponseBuilder.ok(resultDTO);
    }
}
