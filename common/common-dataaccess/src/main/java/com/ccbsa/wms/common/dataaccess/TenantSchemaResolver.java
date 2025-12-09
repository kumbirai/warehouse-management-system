package com.ccbsa.wms.common.dataaccess;

import java.util.Locale;

import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.security.TenantContext;

/**
 * Resolves the database schema name based on the current tenant context.
 * <p>
 * Implements schema-per-tenant strategy for multi-tenant isolation.
 * Each tenant has its own isolated PostgreSQL schema.
 * <p>
 * Schema naming convention: `tenant_{sanitized_tenant_id}_schema`
 * <p>
 * The tenant ID is sanitized to ensure it's a valid PostgreSQL identifier:
 * - Converted to lowercase
 * - Hyphens and other special characters replaced with underscores
 * - Invalid characters removed
 *
 * @see TenantContext
 */
@Component
public class TenantSchemaResolver {

    /**
     * Resolves the schema name for the current tenant.
     * <p>
     * Uses schema-per-tenant strategy where each tenant has its own isolated schema.
     * The schema name is derived from the tenant ID in the current tenant context.
     *
     * @return Schema name in format: `tenant_{sanitized_tenant_id}_schema`
     * @throws IllegalStateException if tenant context is not set
     */
    public String resolveSchema() {
        TenantId tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context not set. Cannot resolve schema without tenant ID.");
        }

        String sanitizedTenantId = sanitizeTenantId(tenantId.getValue());
        return String.format("tenant_%s_schema", sanitizedTenantId);
    }

    /**
     * Sanitizes the tenant ID to ensure it's a valid PostgreSQL identifier.
     * <p>
     * PostgreSQL identifiers:
     * - Must start with a letter or underscore
     * - Can contain letters, digits, and underscores
     * - Case-insensitive (converted to lowercase)
     * - Special characters are replaced with underscores
     *
     * @param tenantId The tenant ID value to sanitize
     * @return Sanitized tenant ID suitable for use in PostgreSQL schema name
     */
    private String sanitizeTenantId(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant ID cannot be null or empty");
        }

        // Convert to lowercase and replace hyphens and other special characters with underscores
        String sanitized = tenantId.toLowerCase(Locale.ROOT)
                .replace("-", "_")
                .replace(".", "_")
                .replace(" ", "_");

        // Remove any remaining invalid characters (keep only alphanumeric and underscores)
        sanitized = sanitized.replaceAll("[^a-z0-9_]", "");

        // Ensure it starts with a letter or underscore (PostgreSQL requirement)
        if (sanitized.isEmpty() || Character.isDigit(sanitized.charAt(0))) {
            sanitized = String.format("t_%s", sanitized);
        }

        // Ensure it doesn't exceed PostgreSQL identifier length limit (63 characters)
        // Account for "tenant_" prefix and "_schema" suffix (14 characters total)
        int maxLength = 63 - 14;
        if (sanitized.length() > maxLength) {
            sanitized = sanitized.substring(0, maxLength);
        }

        return sanitized;
    }
}

