package com.ccbsa.wms.tenant.application.api.mapper;

import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.tenant.application.api.dto.CreateTenantRequest;
import com.ccbsa.wms.tenant.application.api.dto.CreateTenantResponse;
import com.ccbsa.wms.tenant.application.api.dto.TenantResponse;
import com.ccbsa.wms.tenant.application.api.dto.TenantSummaryResponse;
import com.ccbsa.wms.tenant.application.api.dto.UpdateTenantConfigurationRequest;
import com.ccbsa.wms.tenant.application.service.command.dto.CreateTenantCommand;
import com.ccbsa.wms.tenant.application.service.command.dto.CreateTenantResult;
import com.ccbsa.wms.tenant.application.service.command.dto.UpdateTenantConfigurationCommand;
import com.ccbsa.wms.tenant.application.service.query.dto.TenantView;
import com.ccbsa.wms.tenant.domain.core.valueobject.ContactInformation;
import com.ccbsa.wms.tenant.domain.core.valueobject.TenantConfiguration;
import com.ccbsa.wms.tenant.domain.core.valueobject.TenantName;

/**
 * Mapper: TenantMapper
 * <p>
 * Maps between DTOs and domain objects.
 * Anti-corruption layer between API and domain.
 */
@Component
public class TenantMapper {
    public CreateTenantCommand toCreateTenantCommand(CreateTenantRequest request) {
        TenantId tenantId = TenantId.of(request.getTenantId());
        TenantName name = TenantName.of(request.getName());

        ContactInformation contactInfo = null;
        // Check if any contact information is provided (not null and not empty)
        // Normalize email: trim and check if not empty after trimming
        String emailAddress = request.getEmailAddress() != null ? request.getEmailAddress().trim() : null;
        boolean hasEmail = emailAddress != null && !emailAddress.isEmpty();
        boolean hasPhone = request.getPhone() != null && !request.getPhone().trim().isEmpty();
        boolean hasAddress = request.getAddress() != null && !request.getAddress().trim().isEmpty();

        if (hasEmail || hasPhone || hasAddress) {
            // Always create ContactInformation if any field is provided
            // ContactInformation.of() will handle null/empty email gracefully via EmailAddress.ofNullable()
            contactInfo = ContactInformation.of(hasEmail ? emailAddress : null,
                    request.getPhone(),
                    request.getAddress());
        }

        TenantConfiguration configuration = TenantConfiguration.builder()
                .keycloakRealmName(request.getKeycloakRealmName())
                .usePerTenantRealm(request.getUsePerTenantRealm() != null && request.getUsePerTenantRealm())
                .build();

        return new CreateTenantCommand(tenantId,
                name,
                contactInfo,
                configuration);
    }

    public CreateTenantResponse toCreateTenantResponse(CreateTenantResult result) {
        return new CreateTenantResponse(result.getTenantId()
                .getValue(),
                result.isSuccess(),
                result.getMessage());
    }

    public TenantResponse toTenantResponse(TenantView view) {
        return new TenantResponse(view.getTenantId()
                .getValue(),
                view.getName()
                        .getValue(),
                view.getStatus()
                        .name(),
                view.getEmail()
                        .orElse(null),
                view.getPhone()
                        .orElse(null),
                view.getAddress()
                        .orElse(null),
                view.getKeycloakRealmName()
                        .orElse(null),
                view.isUsePerTenantRealm(),
                view.getCreatedAt(),
                view.getActivatedAt()
                        .orElse(null),
                view.getDeactivatedAt()
                        .orElse(null));
    }

    public TenantSummaryResponse toTenantSummaryResponse(TenantView view) {
        return new TenantSummaryResponse(view.getTenantId()
                .getValue(),
                view.getName()
                        .getValue(),
                view.getStatus()
                        .name(),
                view.getEmail()
                        .orElse(null),
                view.getPhone()
                        .orElse(null),
                view.getCreatedAt(),
                view.getActivatedAt()
                        .orElse(null),
                view.isUsePerTenantRealm());
    }

    public UpdateTenantConfigurationCommand toUpdateTenantConfigurationCommand(TenantId tenantId,
                                                                               UpdateTenantConfigurationRequest request) {
        TenantConfiguration configuration = TenantConfiguration.builder()
                .keycloakRealmName(request.getKeycloakRealmName())
                .usePerTenantRealm(request.getUsePerTenantRealm() != null && request.getUsePerTenantRealm())
                .build();

        return new UpdateTenantConfigurationCommand(tenantId,
                configuration);
    }
}

