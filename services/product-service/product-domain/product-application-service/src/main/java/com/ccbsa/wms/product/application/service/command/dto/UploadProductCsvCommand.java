package com.ccbsa.wms.product.application.service.command.dto;

import com.ccbsa.common.domain.valueobject.TenantId;

/**
 * Command DTO: UploadProductCsvCommand
 * <p>
 * Command object for uploading product master data via CSV file.
 */
public final class UploadProductCsvCommand {
    private final TenantId tenantId;
    private final String csvContent;
    private final String fileName;

    private UploadProductCsvCommand(Builder builder) {
        this.tenantId = builder.tenantId;
        this.csvContent = builder.csvContent;
        this.fileName = builder.fileName;
    }

    public static Builder builder() {
        return new Builder();
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public String getCsvContent() {
        return csvContent;
    }

    public String getFileName() {
        return fileName;
    }

    public static class Builder {
        private TenantId tenantId;
        private String csvContent;
        private String fileName;

        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder csvContent(String csvContent) {
            this.csvContent = csvContent;
            return this;
        }

        public Builder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public UploadProductCsvCommand build() {
            if (tenantId == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            if (csvContent == null || csvContent.trim().isEmpty()) {
                throw new IllegalArgumentException("CSV content is required");
            }
            return new UploadProductCsvCommand(this);
        }
    }
}

