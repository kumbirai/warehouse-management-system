package com.ccbsa.wms.user.dataaccess.mapper;

import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.user.application.service.port.data.dto.UserView;
import com.ccbsa.wms.user.dataaccess.entity.UserViewEntity;

/**
 * Entity Mapper: UserViewEntityMapper
 * <p>
 * Maps between UserViewEntity (JPA) and UserView (read model DTO).
 */
@Component
public class UserViewEntityMapper {

    /**
     * Converts UserViewEntity JPA entity to UserView read model DTO.
     *
     * @param entity UserViewEntity JPA entity
     * @return UserView read model DTO
     * @throws IllegalArgumentException if entity is null
     */
    public UserView toView(UserViewEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("UserViewEntity cannot be null");
        }

        // Build UserView
        return UserView.builder().userId(UserId.of(entity.getUserId())).tenantId(TenantId.of(entity.getTenantId())).username(entity.getUsername()).email(entity.getEmailAddress())
                .firstName(entity.getFirstName()).lastName(entity.getLastName()).status(entity.getStatus()).keycloakUserId(entity.getKeycloakUserId())
                .createdAt(entity.getCreatedAt()).lastModifiedAt(entity.getLastModifiedAt()).build();
    }
}

