package com.ccbsa.wms.tenant.application.api.query;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ccbsa.common.application.api.ApiMeta;
import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.application.api.ApiResponseBuilder;
import com.ccbsa.common.domain.exception.EntityNotFoundException;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.tenant.application.api.dto.TenantRealmResponse;
import com.ccbsa.wms.tenant.application.api.dto.TenantResponse;
import com.ccbsa.wms.tenant.application.api.dto.TenantSummaryResponse;
import com.ccbsa.wms.tenant.application.api.mapper.TenantMapper;
import com.ccbsa.wms.tenant.application.service.query.GetTenantQueryHandler;
import com.ccbsa.wms.tenant.application.service.query.GetTenantRealmQueryHandler;
import com.ccbsa.wms.tenant.application.service.query.ListTenantsQueryHandler;
import com.ccbsa.wms.tenant.application.service.query.dto.GetTenantQuery;
import com.ccbsa.wms.tenant.application.service.query.dto.TenantListQuery;
import com.ccbsa.wms.tenant.application.service.query.dto.TenantListResult;
import com.ccbsa.wms.tenant.application.service.query.dto.TenantView;
import com.ccbsa.wms.tenant.domain.core.valueobject.TenantStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST Controller: TenantQueryController
 * <p>
 * Handles query operations (read operations) for tenants. Separated from command operations following CQRS principles.
 */
@RestController
@RequestMapping("/api/v1/tenants")
@Tag(name = "Tenant Queries",
        description = "Tenant query operations")
public class TenantQueryController {
    private static final Logger logger = LoggerFactory.getLogger(TenantQueryController.class);
    private final GetTenantQueryHandler getTenantHandler;
    private final GetTenantRealmQueryHandler getTenantRealmHandler;
    private final ListTenantsQueryHandler listTenantsHandler;
    private final TenantMapper mapper;

    public TenantQueryController(GetTenantQueryHandler getTenantHandler, GetTenantRealmQueryHandler getTenantRealmHandler, ListTenantsQueryHandler listTenantsQueryHandler,
                                 TenantMapper mapper) {
        this.getTenantHandler = getTenantHandler;
        this.getTenantRealmHandler = getTenantRealmHandler;
        this.listTenantsHandler = listTenantsQueryHandler;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "List tenants",
            description = "Lists tenants with pagination, filtering, and search support")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<TenantSummaryResponse>>> listTenants(
            @RequestParam(value = "page",
                    required = false) Integer page,
            @RequestParam(value = "size",
                    required = false) Integer size,
            @RequestParam(value = "status",
                    required = false) String status,
            @RequestParam(value = "search",
                    required = false) String search) {
        logger.debug("Listing tenants - page: {}, size: {}, status: {}, search: {}", page, size, status, search);

        TenantStatus tenantStatus = parseStatus(status);
        TenantListQuery query = TenantListQuery.of(page, size, tenantStatus, search);
        TenantListResult result = listTenantsHandler.handle(query);

        logger.debug("Query result - totalElements: {}, totalPages: {}, tenants count: {}", result.getTotalElements(), result.getTotalPages(), result.getTenants()
                .size());

        List<TenantSummaryResponse> responses = result.getTenants()
                .stream()
                .map(mapper::toTenantSummaryResponse)
                .collect(Collectors.toList());

        logger.debug("Mapped {} tenant responses", responses.size());

        ApiMeta meta = ApiMeta.builder()
                .pagination(new ApiMeta.Pagination(result.getPage(), result.getSize(), result.getTotalElements(), result.getTotalPages(), result.hasNext(), result.hasPrevious()))
                .build();

        return ApiResponseBuilder.ok(responses, Collections.emptyMap(), meta);
    }

    private TenantStatus parseStatus(String status) {
        if (status == null || status.trim()
                .isEmpty()) {
            return null;
        }
        try {
            return TenantStatus.valueOf(status.trim()
                    .toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(String.format("Unsupported status filter: %s", status));
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get tenant",
            description = "Gets a tenant by ID")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasRole('SERVICE') or (hasRole('USER') and @tenantSecurityService.isUserTenant(#id))")
    public ResponseEntity<ApiResponse<TenantResponse>> getTenant(
            @PathVariable String id) {
        TenantId tenantId = TenantId.of(id);
        GetTenantQuery query = new GetTenantQuery(tenantId);

        Optional<TenantView> view = getTenantHandler.handle(query);
        if (view.isEmpty()) {
            throw new EntityNotFoundException(String.format("Tenant not found: %s", id));
        }

        TenantResponse response = mapper.toTenantResponse(view.get());
        return ApiResponseBuilder.ok(response);
    }

    @GetMapping("/{id}/realm")
    @Operation(summary = "Get tenant realm",
            description = "Gets the Keycloak realm name for a tenant. Used by user-service.")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM_ADMIN') or hasRole('SERVICE')")
    public ResponseEntity<ApiResponse<TenantRealmResponse>> getTenantRealm(
            @PathVariable String id) {
        TenantId tenantId = TenantId.of(id);
        Optional<String> realmName = getTenantRealmHandler.handle(tenantId);

        TenantRealmResponse response = new TenantRealmResponse(tenantId.getValue(), realmName.orElse(null));

        return ApiResponseBuilder.ok(response);
    }

    @GetMapping("/{id}/status")
    @Operation(summary = "Get tenant status",
            description = "Gets the status of a tenant. Used by user-service for tenant validation.")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasRole('SERVICE') or (hasRole('USER') and @tenantSecurityService.isUserTenant(#id))")
    public ResponseEntity<ApiResponse<String>> getTenantStatus(
            @PathVariable String id) {
        TenantId tenantId = TenantId.of(id);
        GetTenantQuery query = new GetTenantQuery(tenantId);

        Optional<TenantView> view = getTenantHandler.handle(query);
        if (view.isEmpty()) {
            throw new EntityNotFoundException(String.format("Tenant not found: %s", id));
        }

        return ApiResponseBuilder.ok(view.get()
                .getStatus()
                .name());
    }
}

