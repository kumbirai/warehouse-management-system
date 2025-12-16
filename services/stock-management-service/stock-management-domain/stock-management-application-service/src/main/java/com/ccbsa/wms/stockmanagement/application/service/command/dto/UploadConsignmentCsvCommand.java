package com.ccbsa.wms.stockmanagement.application.service.command.dto;

import java.io.InputStream;

import com.ccbsa.common.domain.valueobject.TenantId;

/**
 * Command DTO: UploadConsignmentCsvCommand
 * <p>
 * Command object for uploading consignment data via CSV file.
 */
public final class UploadConsignmentCsvCommand {
    private final TenantId tenantId;
    private final InputStream csvInputStream;
    private final String receivedBy;

    private UploadConsignmentCsvCommand(Builder builder) {
        this.tenantId = builder.tenantId;
        this.csvInputStream = builder.csvInputStream;
        this.receivedBy = builder.receivedBy;
    }

    public static Builder builder() {
        return new Builder();
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public InputStream getCsvInputStream() {
        return csvInputStream;
    }

    public String getReceivedBy() {
        return receivedBy;
    }

    public static class Builder {
        private TenantId tenantId;
        private InputStream csvInputStream;
        private String receivedBy;

        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder csvInputStream(InputStream csvInputStream) {
            this.csvInputStream = csvInputStream;
            return this;
        }

        public Builder receivedBy(String receivedBy) {
            this.receivedBy = receivedBy;
            return this;
        }

        public UploadConsignmentCsvCommand build() {
            if (tenantId == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            if (csvInputStream == null) {
                throw new IllegalArgumentException("CSV input stream is required");
            }
            return new UploadConsignmentCsvCommand(this);
        }
    }
}

