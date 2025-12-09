package com.ccbsa.wms.tenant.messaging.adapter;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.keycloak.admin.client.resource.GroupResource;
import org.keycloak.admin.client.resource.GroupsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.keycloak.config.KeycloakConfig;
import com.ccbsa.common.keycloak.port.KeycloakClientPort;
import com.ccbsa.wms.tenant.application.service.port.service.TenantGroupServicePort;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

/**
 * Adapter handling tenant-specific Keycloak group orchestration.
 */
@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Ports are framework-managed singletons and treated as immutable dependencies")
public class KeycloakGroupAdapter implements TenantGroupServicePort {
    private static final Logger logger = LoggerFactory.getLogger(KeycloakGroupAdapter.class);
    private static final String STATUS_ATTRIBUTE = "tenant_status";
    private static final String TENANT_ID_ATTRIBUTE = "tenant_id";

    private final KeycloakClientPort keycloakClientPort;
    private final KeycloakConfig keycloakConfig;

    public KeycloakGroupAdapter(KeycloakClientPort keycloakClientPort,
                                KeycloakConfig keycloakConfig) {
        this.keycloakClientPort = keycloakClientPort;
        this.keycloakConfig = keycloakConfig;
    }

    @Override
    public void ensureTenantGroupEnabled(TenantId tenantId) {
        String realmName = keycloakConfig.getDefaultRealm();
        String groupName = buildGroupName(tenantId);
        GroupRepresentation group = findGroup(realmName,
                groupName);
        if (group == null) {
            group = createGroup(realmName,
                    tenantId,
                    groupName);
        }
        updateStatusAttribute(realmName,
                group,
                "ACTIVE");
        logger.info("Tenant group {} ensured/enabled in realm {}",
                groupName,
                realmName);
    }

    private String buildGroupName(TenantId tenantId) {
        return String.format("tenant-%s",
                tenantId.getValue());
    }

    private GroupRepresentation findGroup(String realmName,
                                          String groupName) {
        RealmResource realmResource = realm(realmName);
        try {
            GroupRepresentation representation = realmResource.getGroupByPath(String.format("/%s",
                    groupName));
            if (representation != null) {
                return representation;
            }
        } catch (NotFoundException ex) {
            logger.debug("Group {} not found via path lookup in realm {}",
                    groupName,
                    realmName);
        }

        GroupsResource groupsResource = realmResource.groups();
        List<GroupRepresentation> groups = groupsResource.groups();
        if (groups == null || groups.isEmpty()) {
            return null;
        }

        return groups.stream()
                .filter(group -> groupName.equalsIgnoreCase(group.getName()))
                .findFirst()
                .orElse(null);
    }

    private GroupRepresentation createGroup(String realmName,
                                            TenantId tenantId,
                                            String groupName) {
        RealmResource realmResource = realm(realmName);
        GroupsResource groupsResource = realmResource.groups();

        GroupRepresentation group = new GroupRepresentation();
        group.setName(groupName);
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put(TENANT_ID_ATTRIBUTE,
                List.of(tenantId.getValue()));
        attributes.put(STATUS_ATTRIBUTE,
                List.of("ACTIVE"));
        group.setAttributes(attributes);

        try (Response response = groupsResource.add(group)) {
            int status = response.getStatus();
            if (status >= 400) {
                throw new IllegalStateException(String.format("Failed to create Keycloak group %s in realm %s. Status: %d",
                        groupName,
                        realmName,
                        status));
            }
        }

        return findGroup(realmName,
                groupName);
    }

    private void updateStatusAttribute(String realmName,
                                       GroupRepresentation group,
                                       String status) {
        Map<String, List<String>> attributes = group.getAttributes() != null
                ? new HashMap<>(group.getAttributes())
                : new HashMap<>();
        attributes.put(STATUS_ATTRIBUTE,
                List.of(status.toUpperCase(Locale.ROOT)));
        group.setAttributes(attributes);

        GroupResource groupResource = realm(realmName).groups()
                .group(group.getId());
        groupResource.update(group);
    }

    private RealmResource realm(String realmName) {
        return keycloakClientPort.getAdminClient()
                .realm(realmName);
    }

    @Override
    public void disableTenantGroup(TenantId tenantId) {
        String realmName = keycloakConfig.getDefaultRealm();
        String groupName = buildGroupName(tenantId);
        GroupRepresentation group = findGroup(realmName,
                groupName);
        if (group == null) {
            logger.info("Tenant group {} not found in realm {}; nothing to disable",
                    groupName,
                    realmName);
            return;
        }
        updateStatusAttribute(realmName,
                group,
                "DISABLED");
        logger.info("Tenant group {} disabled in realm {}",
                groupName,
                realmName);
    }
}

