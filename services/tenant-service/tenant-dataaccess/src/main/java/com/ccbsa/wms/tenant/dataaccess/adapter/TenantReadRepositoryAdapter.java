package com.ccbsa.wms.tenant.dataaccess.adapter;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.ccbsa.wms.tenant.application.service.port.repository.TenantReadRepository;
import com.ccbsa.wms.tenant.application.service.query.dto.TenantListQuery;
import com.ccbsa.wms.tenant.application.service.query.dto.TenantListResult;
import com.ccbsa.wms.tenant.application.service.query.dto.TenantView;
import com.ccbsa.wms.tenant.dataaccess.entity.TenantEntity;
import com.ccbsa.wms.tenant.dataaccess.jpa.TenantJpaRepository;
import com.ccbsa.wms.tenant.dataaccess.mapper.TenantViewMapper;

/**
 * Adapter implementing the read-only tenant repository port.
 */
@Repository
public class TenantReadRepositoryAdapter
        implements TenantReadRepository {
    private final TenantJpaRepository tenantJpaRepository;
    private final TenantViewMapper tenantViewMapper;

    public TenantReadRepositoryAdapter(TenantJpaRepository tenantJpaRepository, TenantViewMapper tenantViewMapper) {
        this.tenantJpaRepository = tenantJpaRepository;
        this.tenantViewMapper = tenantViewMapper;
    }

    @Override
    public TenantListResult listTenants(TenantListQuery query) {
        // Use unsorted Pageable for native query - sorting is handled in the SQL query itself
        Pageable pageable = PageRequest.of(query.getPage() - 1, query.getSize());

        // Convert TenantStatus enum to String for native query to avoid PostgreSQL type inference issues
        String statusString = query.getStatus()
                .map(Enum::name)
                .orElse(null);

        Page<TenantEntity> page = tenantJpaRepository.searchTenants(statusString, query.getSearch()
                .map(String::toLowerCase)
                .orElse(null), pageable);

        List<TenantView> tenants = page.getContent()
                .stream()
                .map(tenantViewMapper::toView)
                .collect(Collectors.toUnmodifiableList());

        return new TenantListResult(tenants, query.getPage(), query.getSize(), page.getTotalElements());
    }
}

