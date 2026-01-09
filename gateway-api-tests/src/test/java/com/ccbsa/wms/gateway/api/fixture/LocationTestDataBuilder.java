package com.ccbsa.wms.gateway.api.fixture;

import com.ccbsa.wms.gateway.api.dto.CreateLocationRequest;
import com.ccbsa.wms.gateway.api.dto.LocationDimensions;

/**
 * Builder for creating location test data.
 */
public class LocationTestDataBuilder {

    public static CreateLocationRequest buildWarehouseRequest() {
        return CreateLocationRequest.builder().code(TestData.warehouseCode()).name(TestData.faker().company().name() + " Warehouse").type("WAREHOUSE").parentLocationId(null)
                .capacity(TestData.locationCapacity("WAREHOUSE")).dimensions(warehouseDimensions()).build();
    }

    private static LocationDimensions warehouseDimensions() {
        return LocationDimensions.builder().length(100.0).width(80.0).height(10.0).unit("M").build();
    }

    public static CreateLocationRequest buildZoneRequest(String parentLocationId) {
        return CreateLocationRequest.builder().code(TestData.zoneCode()).name("Zone " + TestData.zoneCode()).type("ZONE").parentLocationId(parentLocationId)
                .capacity(TestData.locationCapacity("ZONE")).build();
    }

    public static CreateLocationRequest buildAisleRequest(String parentLocationId) {
        return CreateLocationRequest.builder().code(TestData.aisleCode()).name("Aisle " + TestData.aisleCode()).type("AISLE").parentLocationId(parentLocationId)
                .capacity(TestData.locationCapacity("AISLE")).build();
    }

    public static CreateLocationRequest buildRackRequest(String parentLocationId) {
        return CreateLocationRequest.builder().code(TestData.rackCode()).name("Rack " + TestData.rackCode()).type("RACK").parentLocationId(parentLocationId)
                .capacity(TestData.locationCapacity("RACK")).build();
    }

    public static CreateLocationRequest buildBinRequest(String parentLocationId) {
        return CreateLocationRequest.builder().code(TestData.binCode()).name("Bin " + TestData.binCode()).type("BIN").parentLocationId(parentLocationId)
                .capacity(TestData.locationCapacity("BIN")).build();
    }
}

