package com.ccbsa.wms.picking.application.service.port.service.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * DTO: ProductInfo
 * <p>
 * Represents product information returned from Product Service.
 */
@Getter
@Builder
public class ProductInfo {
    private final String productCode;
    private final String productName;
    private final String description;
}
