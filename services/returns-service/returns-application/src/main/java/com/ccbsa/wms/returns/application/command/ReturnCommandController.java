package com.ccbsa.wms.returns.application.command;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.application.api.ApiResponseBuilder;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.returns.application.dto.command.HandlePartialOrderAcceptanceRequestDTO;
import com.ccbsa.wms.returns.application.dto.command.HandlePartialOrderAcceptanceResponseDTO;
import com.ccbsa.wms.returns.application.dto.command.ProcessFullOrderReturnRequestDTO;
import com.ccbsa.wms.returns.application.dto.command.ProcessFullOrderReturnResponseDTO;
import com.ccbsa.wms.returns.application.dto.command.RecordDamageAssessmentRequestDTO;
import com.ccbsa.wms.returns.application.dto.command.RecordDamageAssessmentResponseDTO;
import com.ccbsa.wms.returns.application.dto.mapper.ReturnDTOMapper;
import com.ccbsa.wms.returns.application.service.command.HandlePartialOrderAcceptanceCommandHandler;
import com.ccbsa.wms.returns.application.service.command.ProcessFullOrderReturnCommandHandler;
import com.ccbsa.wms.returns.application.service.command.RecordDamageAssessmentCommandHandler;
import com.ccbsa.wms.returns.application.service.command.dto.HandlePartialOrderAcceptanceCommand;
import com.ccbsa.wms.returns.application.service.command.dto.HandlePartialOrderAcceptanceResult;
import com.ccbsa.wms.returns.application.service.command.dto.ProcessFullOrderReturnCommand;
import com.ccbsa.wms.returns.application.service.command.dto.ProcessFullOrderReturnResult;
import com.ccbsa.wms.returns.application.service.command.dto.RecordDamageAssessmentCommand;
import com.ccbsa.wms.returns.application.service.command.dto.RecordDamageAssessmentResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller: ReturnCommandController
 * <p>
 * Handles return command operations (write operations).
 * <p>
 * Responsibilities:
 * - Handle partial order acceptance endpoints
 * - Process full order return endpoints
 * - Record damage assessment endpoints
 * - Map DTOs to commands
 * - Return standardized API responses
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/returns")
@Tag(name = "Returns Management Commands", description = "Returns management command operations")
@RequiredArgsConstructor
public class ReturnCommandController {
    private final HandlePartialOrderAcceptanceCommandHandler handlePartialOrderAcceptanceCommandHandler;
    private final ProcessFullOrderReturnCommandHandler processFullOrderReturnCommandHandler;
    private final RecordDamageAssessmentCommandHandler recordDamageAssessmentCommandHandler;
    private final ReturnDTOMapper mapper;

    @PostMapping("/partial-acceptance")
    @Operation(summary = "Handle Partial Order Acceptance", description = "Process partial order acceptance when customer accepts only part of their order")
    @ApiResponses(value = {@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Partial acceptance processed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Order picking not completed")})
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'WAREHOUSE_OPERATOR', 'RETURNS_MANAGER', 'RETURNS_CLERK', 'SERVICE')")
    public ResponseEntity<ApiResponse<HandlePartialOrderAcceptanceResponseDTO>> handlePartialOrderAcceptance(@RequestHeader("X-Tenant-Id") String tenantId, @Valid @RequestBody
    HandlePartialOrderAcceptanceRequestDTO requestDTO) {
        log.info("Received partial order acceptance request for order: {}", requestDTO.getOrderNumber());

        // Map DTO to command
        HandlePartialOrderAcceptanceCommand command = mapper.toHandlePartialOrderAcceptanceCommand(requestDTO);

        // Execute command
        HandlePartialOrderAcceptanceResult result = handlePartialOrderAcceptanceCommandHandler.handle(command, TenantId.of(tenantId));

        // Map result to response DTO
        HandlePartialOrderAcceptanceResponseDTO responseDTO = mapper.toHandlePartialOrderAcceptanceResponseDTO(result);

        log.info("Partial order acceptance completed. Return ID: {}", result.getReturnId().getValueAsString());
        return ApiResponseBuilder.created(responseDTO);
    }

    @PostMapping("/full-return")
    @Operation(summary = "Process Full Order Return", description = "Process full order return when entire order is returned")
    @ApiResponses(value = {@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Full return processed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")})
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'WAREHOUSE_OPERATOR', 'RETURNS_MANAGER', 'RETURNS_CLERK', 'SERVICE')")
    public ResponseEntity<ApiResponse<ProcessFullOrderReturnResponseDTO>> processFullOrderReturn(@RequestHeader("X-Tenant-Id") String tenantId,
                                                                                                 @Valid @RequestBody ProcessFullOrderReturnRequestDTO requestDTO) {
        log.info("Received full order return request for order: {}", requestDTO.getOrderNumber());

        // Map DTO to command
        ProcessFullOrderReturnCommand command = mapper.toProcessFullOrderReturnCommand(requestDTO);

        // Execute command
        ProcessFullOrderReturnResult result = processFullOrderReturnCommandHandler.handle(command, TenantId.of(tenantId));

        // Map result to response DTO
        ProcessFullOrderReturnResponseDTO responseDTO = mapper.toProcessFullOrderReturnResponseDTO(result);

        log.info("Full order return completed. Return ID: {}", result.getReturnId().getValueAsString());
        return ApiResponseBuilder.created(responseDTO);
    }

    @PostMapping("/damage-assessment")
    @Operation(summary = "Record Damage Assessment", description = "Record damage assessment for products damaged in transit")
    @ApiResponses(value = {@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Damage assessment recorded successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data")})
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'WAREHOUSE_MANAGER', 'WAREHOUSE_OPERATOR', 'RETURNS_MANAGER', 'RETURNS_CLERK', 'SERVICE')")
    public ResponseEntity<ApiResponse<RecordDamageAssessmentResponseDTO>> recordDamageAssessment(@RequestHeader("X-Tenant-Id") String tenantId,
                                                                                                 @Valid @RequestBody RecordDamageAssessmentRequestDTO requestDTO) {
        log.info("Received damage assessment request for order: {}", requestDTO.getOrderNumber());

        // Map DTO to command
        RecordDamageAssessmentCommand command = mapper.toRecordDamageAssessmentCommand(requestDTO);

        // Execute command
        RecordDamageAssessmentResult result = recordDamageAssessmentCommandHandler.handle(command, TenantId.of(tenantId));

        // Map result to response DTO
        RecordDamageAssessmentResponseDTO responseDTO = mapper.toRecordDamageAssessmentResponseDTO(result);

        log.info("Damage assessment recorded. Assessment ID: {}", result.getDamageAssessmentId().getValueAsString());
        return ApiResponseBuilder.created(responseDTO);
    }
}
