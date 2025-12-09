package com.ccbsa.wms.tenant.application.service.query;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.tenant.application.service.port.repository.TenantReadRepository;
import com.ccbsa.wms.tenant.application.service.query.dto.TenantListQuery;
import com.ccbsa.wms.tenant.application.service.query.dto.TenantListResult;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Query handler for listing tenants with pagination support.
 */
@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Ports are injected singletons and immutable")
public class ListTenantsQueryHandler {
    private final TenantReadRepository tenantReadRepository;

    public ListTenantsQueryHandler(TenantReadRepository tenantReadRepository) {
        this.tenantReadRepository = tenantReadRepository;
    }

    @Transactional(readOnly = true)
    public TenantListResult handle(TenantListQuery query) {
        return tenantReadRepository.listTenants(query);
    }
}

