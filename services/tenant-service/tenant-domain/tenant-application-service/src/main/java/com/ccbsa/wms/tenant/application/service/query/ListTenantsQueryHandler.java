package com.ccbsa.wms.tenant.application.service.query;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.tenant.application.service.port.data.TenantViewRepository;
import com.ccbsa.wms.tenant.application.service.query.dto.TenantListQuery;
import com.ccbsa.wms.tenant.application.service.query.dto.TenantListResult;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Query handler for listing tenants with pagination support.
 * <p>
 * Uses data port (TenantViewRepository) instead of repository port for CQRS compliance.
 */
@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Ports are injected singletons and immutable")
public class ListTenantsQueryHandler {
    private final TenantViewRepository viewRepository;

    public ListTenantsQueryHandler(TenantViewRepository viewRepository) {
        this.viewRepository = viewRepository;
    }

    @Transactional(readOnly = true)
    public TenantListResult handle(TenantListQuery query) {
        return viewRepository.listTenants(query);
    }
}

