package com.ccbsa.wms.tenant.application.service.query;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.tenant.application.service.port.data.TenantViewRepository;
import com.ccbsa.wms.tenant.application.service.query.dto.GetTenantQuery;
import com.ccbsa.wms.tenant.application.service.query.dto.TenantView;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Query Handler: GetTenantQueryHandler
 * <p>
 * Handles queries to get a tenant read model by ID.
 * <p>
 * Uses data port (TenantViewRepository) instead of repository port for CQRS compliance.
 */
@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Ports are managed singletons injected by Spring and kept immutable")
public class GetTenantQueryHandler {
    private final TenantViewRepository viewRepository;

    public GetTenantQueryHandler(TenantViewRepository viewRepository) {
        this.viewRepository = viewRepository;
    }

    @Transactional(readOnly = true)
    public Optional<TenantView> handle(GetTenantQuery query) {
        return viewRepository.findById(query.getTenantId());
    }
}

