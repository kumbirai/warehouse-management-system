package com.ccbsa.wms.gateway.api.fixture;

import com.ccbsa.wms.gateway.api.dto.CreateTenantRequest;

/**
 * Builder for creating tenant test data.
 */
public class TenantTestDataBuilder {

    public static CreateTenantRequest buildCreateTenantRequest() {
        return CreateTenantRequest.builder().tenantId(TestData.tenantId()).name(TestData.tenantName()).emailAddress(TestData.tenantEmail()).build();
    }

    public static CreateTenantRequest buildCreateTenantRequestWithName(String name) {
        return CreateTenantRequest.builder().tenantId(TestData.tenantId()).name(name).emailAddress(TestData.tenantEmail()).build();
    }
}

