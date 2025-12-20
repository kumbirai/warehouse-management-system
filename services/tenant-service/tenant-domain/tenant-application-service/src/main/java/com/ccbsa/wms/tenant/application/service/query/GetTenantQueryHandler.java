package com.ccbsa.wms.tenant.application.service.query;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.tenant.application.service.port.repository.TenantRepository;
import com.ccbsa.wms.tenant.application.service.query.dto.GetTenantQuery;
import com.ccbsa.wms.tenant.application.service.query.dto.TenantView;
import com.ccbsa.wms.tenant.domain.core.entity.Tenant;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Query Handler: GetTenantQueryHandler
 * <p>
 * Handles queries to get a tenant by ID.
 */
@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Ports are managed singletons injected by Spring and kept immutable")
public class GetTenantQueryHandler {
    private final TenantRepository tenantRepository;

    public GetTenantQueryHandler(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Transactional(readOnly = true)
    public Optional<TenantView> handle(GetTenantQuery query) {
        return tenantRepository.findById(query.getTenantId()).map(this::toView);
    }

    private TenantView toView(Tenant tenant) {
        return new TenantView(tenant.getId(), tenant.getName(), tenant.getStatus(),
                tenant.getContactInformation() != null ? tenant.getContactInformation().getEmailValue().orElse(null) : null,
                tenant.getContactInformation() != null ? tenant.getContactInformation().getPhone().orElse(null) : null,
                tenant.getContactInformation() != null ? tenant.getContactInformation().getAddress().orElse(null) : null,
                tenant.getConfiguration().getKeycloakRealmName().orElse(null), tenant.getConfiguration().isUsePerTenantRealm(), tenant.getCreatedAt(), tenant.getActivatedAt(),
                tenant.getDeactivatedAt());
    }
}

