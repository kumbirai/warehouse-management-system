package com.ccbsa.wms.picking.application.service.command.dto;

import com.ccbsa.common.domain.valueobject.TenantId;

import lombok.Builder;
import lombok.Getter;

/**
 * Command DTO: UploadPickingListCsvCommand
 * <p>
 * Command object for uploading a picking list via CSV file.
 */
@Getter
@Builder
public final class UploadPickingListCsvCommand {
    private final TenantId tenantId;
    private final byte[] csvContent;
    private final String fileName;

    public UploadPickingListCsvCommand(TenantId tenantId, byte[] csvContent, String fileName) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (csvContent == null || csvContent.length == 0) {
            throw new IllegalArgumentException("CSV content is required");
        }
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("File name is required");
        }
        this.tenantId = tenantId;
        this.csvContent = csvContent;
        this.fileName = fileName;
    }
}
