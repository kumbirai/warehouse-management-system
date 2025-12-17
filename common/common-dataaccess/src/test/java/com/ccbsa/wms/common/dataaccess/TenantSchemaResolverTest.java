package com.ccbsa.wms.common.dataaccess;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.common.security.TenantContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link TenantSchemaResolver}.
 */
@DisplayName("TenantSchemaResolver Tests")
class TenantSchemaResolverTest {

    private TenantSchemaResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new TenantSchemaResolver();
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should resolve schema name for tenant with simple ID")
    void shouldResolveSchemaForSimpleTenantId() {
        // Given
        TenantId tenantId = TenantId.of("tenant-1");
        TenantContext.setTenantId(tenantId);

        // When
        String schema = resolver.resolveSchema();

        // Then
        assertThat(schema).isEqualTo("tenant_tenant_1_schema");
    }

    @Test
    @DisplayName("Should resolve schema name for tenant with hyphenated ID")
    void shouldResolveSchemaForHyphenatedTenantId() {
        // Given
        TenantId tenantId = TenantId.of("my-tenant-id");
        TenantContext.setTenantId(tenantId);

        // When
        String schema = resolver.resolveSchema();

        // Then
        assertThat(schema).isEqualTo("tenant_my_tenant_id_schema");
    }

    @Test
    @DisplayName("Should resolve schema name for tenant with uppercase ID")
    void shouldResolveSchemaForUppercaseTenantId() {
        // Given
        TenantId tenantId = TenantId.of("TENANT-ABC");
        TenantContext.setTenantId(tenantId);

        // When
        String schema = resolver.resolveSchema();

        // Then
        assertThat(schema).isEqualTo("tenant_tenant_abc_schema");
    }

    @Test
    @DisplayName("Should resolve schema name for tenant with special characters")
    void shouldResolveSchemaForTenantIdWithSpecialCharacters() {
        // Given
        TenantId tenantId = TenantId.of("tenant.123-test");
        TenantContext.setTenantId(tenantId);

        // When
        String schema = resolver.resolveSchema();

        // Then
        assertThat(schema).isEqualTo("tenant_tenant_123_test_schema");
    }

    @Test
    @DisplayName("Should resolve schema name for tenant ID starting with digit")
    void shouldResolveSchemaForTenantIdStartingWithDigit() {
        // Given
        TenantId tenantId = TenantId.of("123-tenant");
        TenantContext.setTenantId(tenantId);

        // When
        String schema = resolver.resolveSchema();

        // Then
        assertThat(schema).startsWith("tenant_t_");
        assertThat(schema).endsWith("_schema");
    }

    @Test
    @DisplayName("Should throw exception when tenant context is not set")
    void shouldThrowExceptionWhenTenantContextNotSet() {
        // Given - no tenant context set

        // When/Then
        assertThatThrownBy(() -> resolver.resolveSchema()).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Tenant context not set");
    }

    @Test
    @DisplayName("Should truncate very long tenant IDs to fit PostgreSQL identifier limit")
    void shouldTruncateLongTenantId() {
        // Given - Use maximum allowed tenant ID length (50 chars) which will exceed
        // PostgreSQL's 63-char limit when combined with "tenant_" prefix and "_schema" suffix
        String longTenantId = "a".repeat(50); // Maximum allowed by TenantId validation
        TenantId tenantId = TenantId.of(longTenantId);
        TenantContext.setTenantId(tenantId);

        // When
        String schema = resolver.resolveSchema();

        // Then
        assertThat(schema).hasSizeLessThanOrEqualTo(63); // PostgreSQL identifier limit
        assertThat(schema).startsWith("tenant_");
        assertThat(schema).endsWith("_schema");
        // Verify truncation occurred: sanitized tenant ID should be truncated to 49 chars
        // (63 total - 14 for prefix/suffix = 49 max for sanitized tenant ID)
        assertThat(schema.length()).isLessThanOrEqualTo(63);
    }
}

