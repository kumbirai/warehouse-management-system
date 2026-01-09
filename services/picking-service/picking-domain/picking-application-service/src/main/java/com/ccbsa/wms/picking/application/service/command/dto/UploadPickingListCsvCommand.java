package com.ccbsa.wms.picking.application.service.command.dto;

import com.ccbsa.common.domain.valueobject.TenantId;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;

/**
 * Command DTO: UploadPickingListCsvCommand
 * <p>
 * Command object for uploading a picking list via CSV file.
 */
@Getter
@Builder
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copy is created in constructor and getter returns defensive copy")
public final class UploadPickingListCsvCommand {
    private final TenantId tenantId;
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Defensive copy is created in constructor")
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
        // Create defensive copy of byte array
        this.csvContent = csvContent.clone();
        this.fileName = fileName;
    }

    /**
     * Returns a defensive copy of the CSV content byte array to prevent external modification.
     *
     * @return copy of the CSV content byte array (never null - validated in constructor)
     */
    public byte[] getCsvContent() {
        return csvContent.clone();
    }
}
