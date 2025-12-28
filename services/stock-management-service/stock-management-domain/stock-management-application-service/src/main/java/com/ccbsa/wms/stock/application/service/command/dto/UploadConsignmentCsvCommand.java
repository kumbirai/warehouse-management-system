package com.ccbsa.wms.stock.application.service.command.dto;

import java.io.InputStream;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.stock.domain.core.valueobject.ReceivedBy;

import lombok.Builder;
import lombok.Getter;

/**
 * Command DTO: UploadConsignmentCsvCommand
 * <p>
 * Command object for uploading consignment data via CSV file.
 */
@Getter
@Builder
public final class UploadConsignmentCsvCommand {
    private final TenantId tenantId;
    private final InputStream csvInputStream;
    private final ReceivedBy receivedBy;

    public UploadConsignmentCsvCommand(TenantId tenantId, InputStream csvInputStream, ReceivedBy receivedBy) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (csvInputStream == null) {
            throw new IllegalArgumentException("CSV input stream is required");
        }
        this.tenantId = tenantId;
        this.csvInputStream = csvInputStream;
        this.receivedBy = receivedBy;
    }
}

