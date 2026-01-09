package com.ccbsa.wms.gateway.api.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadPickingListCsvResponse {
    private int totalRows;
    private int successfulRows;
    private int errorRows;
    private List<String> createdPickingListIds;
    private List<CsvValidationError> errors;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CsvValidationError {
        private int rowNumber;
        private String fieldName;
        private String errorMessage;
        private String invalidValue;
    }
}
