package com.ccbsa.wms.product.application.service.command.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;

/**
 * Result DTO: UploadProductCsvResult
 * <p>
 * Result object returned after uploading a CSV file. Contains summary statistics and error details.
 */
@Getter
@Builder
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Lombok builder stores list directly. Defensive copy made in constructor and getter returns immutable view.")
public final class UploadProductCsvResult {
    private final int totalRows;
    private final int createdCount;
    private final int updatedCount;
    private final int errorCount;
    private final List<ProductCsvError> errors;

    public UploadProductCsvResult(int totalRows, int createdCount, int updatedCount, int errorCount, List<ProductCsvError> errors) {
        this.totalRows = totalRows;
        this.createdCount = createdCount;
        this.updatedCount = updatedCount;
        this.errorCount = errors != null ? errors.size() : 0;
        // Defensive copy to prevent external modification
        this.errors = errors != null ? List.copyOf(errors) : List.of();
    }

    /**
     * Returns an unmodifiable view of the errors list.
     *
     * @return Unmodifiable list of errors
     */
    public List<ProductCsvError> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public static class Builder {
        private List<ProductCsvError> errors;

        public Builder addError(ProductCsvError error) {
            if (this.errors == null) {
                this.errors = new ArrayList<>();
            }
            this.errors.add(error);
            return this;
        }
    }
}

