package com.ccbsa.wms.tenant.application.api.command;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.application.api.ApiResponseBuilder;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.tenant.application.api.dto.CreateTenantRequest;
import com.ccbsa.wms.tenant.application.api.dto.CreateTenantResponse;
import com.ccbsa.wms.tenant.application.api.dto.UpdateTenantConfigurationRequest;
import com.ccbsa.wms.tenant.application.api.mapper.TenantMapper;
import com.ccbsa.wms.tenant.application.service.command.ActivateTenantCommandHandler;
import com.ccbsa.wms.tenant.application.service.command.CreateTenantCommandHandler;
import com.ccbsa.wms.tenant.application.service.command.DeactivateTenantCommandHandler;
import com.ccbsa.wms.tenant.application.service.command.SuspendTenantCommandHandler;
import com.ccbsa.wms.tenant.application.service.command.UpdateTenantConfigurationCommandHandler;
import com.ccbsa.wms.tenant.application.service.command.dto.ActivateTenantCommand;
import com.ccbsa.wms.tenant.application.service.command.dto.CreateTenantCommand;
import com.ccbsa.wms.tenant.application.service.command.dto.CreateTenantResult;
import com.ccbsa.wms.tenant.application.service.command.dto.DeactivateTenantCommand;
import com.ccbsa.wms.tenant.application.service.command.dto.SuspendTenantCommand;
import com.ccbsa.wms.tenant.application.service.command.dto.UpdateTenantConfigurationCommand;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST Controller: TenantCommandController
 * <p>
 * Handles command operations (write operations) for tenants. Separated from query operations following CQRS principles.
 */
@RestController
@RequestMapping("/api/v1/tenants")
@Tag(name = "Tenant Commands", description = "Tenant management command operations")
public class TenantCommandController {
    private final CreateTenantCommandHandler createTenantHandler;
    private final ActivateTenantCommandHandler activateTenantHandler;
    private final DeactivateTenantCommandHandler deactivateTenantHandler;
    private final SuspendTenantCommandHandler suspendTenantHandler;
    private final UpdateTenantConfigurationCommandHandler updateConfigurationHandler;
    private final TenantMapper mapper;

    public TenantCommandController(CreateTenantCommandHandler createTenantHandler, ActivateTenantCommandHandler activateTenantHandler,
                                   DeactivateTenantCommandHandler deactivateTenantHandler, SuspendTenantCommandHandler suspendTenantHandler,
                                   UpdateTenantConfigurationCommandHandler updateConfigurationHandler, TenantMapper mapper) {
        this.createTenantHandler = createTenantHandler;
        this.activateTenantHandler = activateTenantHandler;
        this.deactivateTenantHandler = deactivateTenantHandler;
        this.suspendTenantHandler = suspendTenantHandler;
        this.updateConfigurationHandler = updateConfigurationHandler;
        this.mapper = mapper;
    }

    @PostMapping
    @Operation(summary = "Create tenant", description = "Creates a new tenant (LDP)")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SERVICE')")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ApiResponse<CreateTenantResponse>> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        CreateTenantCommand command = mapper.toCreateTenantCommand(request);
        CreateTenantResult result = createTenantHandler.handle(command);
        CreateTenantResponse response = mapper.toCreateTenantResponse(result);

        return ApiResponseBuilder.created(response);
    }

    @PutMapping("/{id}/activate")
    @Operation(summary = "Activate tenant", description = "Activates a tenant and creates/enables Keycloak realm if configured")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SERVICE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<ApiResponse<Void>> activateTenant(@PathVariable String id) {
        TenantId tenantId = TenantId.of(id);
        ActivateTenantCommand command = new ActivateTenantCommand(tenantId);
        activateTenantHandler.handle(command);
        return ApiResponseBuilder.noContent();
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate tenant", description = "Deactivates a tenant and disables Keycloak realm if configured")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SERVICE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<ApiResponse<Void>> deactivateTenant(@PathVariable String id) {
        TenantId tenantId = TenantId.of(id);
        DeactivateTenantCommand command = new DeactivateTenantCommand(tenantId);
        deactivateTenantHandler.handle(command);
        return ApiResponseBuilder.noContent();
    }

    @PutMapping("/{id}/suspend")
    @Operation(summary = "Suspend tenant", description = "Suspends a tenant and disables Keycloak realm if configured")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SERVICE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<ApiResponse<Void>> suspendTenant(@PathVariable String id) {
        TenantId tenantId = TenantId.of(id);
        SuspendTenantCommand command = new SuspendTenantCommand(tenantId);
        suspendTenantHandler.handle(command);
        return ApiResponseBuilder.noContent();
    }

    @PutMapping("/{id}/configuration")
    @Operation(summary = "Update tenant configuration", description = "Updates tenant configuration settings")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SERVICE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<ApiResponse<Void>> updateTenantConfiguration(@PathVariable String id, @Valid @RequestBody UpdateTenantConfigurationRequest request) {
        TenantId tenantId = TenantId.of(id);
        UpdateTenantConfigurationCommand command = mapper.toUpdateTenantConfigurationCommand(tenantId, request);
        updateConfigurationHandler.handle(command);
        return ApiResponseBuilder.noContent();
    }
}

