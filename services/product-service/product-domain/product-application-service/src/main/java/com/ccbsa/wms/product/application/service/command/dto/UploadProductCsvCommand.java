package com.ccbsa.wms.product.application.service.command.dto;

import com.ccbsa.common.domain.valueobject.TenantId;

import lombok.Builder;
import lombok.Getter;

/**
 * Command DTO: UploadProductCsvCommand
 * <p>
 * Command object for uploading product master data via CSV file.
 */
@Getter
@Builder
public final class UploadProductCsvCommand {
    private final TenantId tenantId;
    private final String csvContent;
    private final String fileName;

    public UploadProductCsvCommand(TenantId tenantId, String csvContent, String fileName) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (csvContent == null || csvContent.trim().isEmpty()) {
            throw new IllegalArgumentException("CSV content is required");
        }
        this.tenantId = tenantId;
        this.csvContent = csvContent;
        this.fileName = fileName;
    }
}

