package com.ccbsa.wms.user.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

/**
 * Application info contributor. Adds custom information to the /actuator/info endpoint.
 */
@Component
public class ApplicationInfoContributor implements InfoContributor {
    @Override
    public void contribute(Info.Builder builder) {
        Map<String, Object> details = new HashMap<>();
        details.put("service", "user-service");
        details.put("version", "1.0.0-SNAPSHOT");
        details.put("description", "User Management Service with BFF authentication endpoints");
        details.put("features", new String[] {"User Management", "IAM Integration (Keycloak)", "BFF Authentication", "JWT Token Management"});

        builder.withDetails(details);
    }
}

