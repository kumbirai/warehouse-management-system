package com.ccbsa.wms.picking.application.dto.command;

import lombok.Getter;
import lombok.Setter;

/**
 * Command DTO: UploadPickingListCsvCommandDTO
 * <p>
 * DTO for CSV upload endpoint (file is handled via MultipartFile parameter).
 */
@Getter
@Setter
public class UploadPickingListCsvCommandDTO {
    // File is passed as MultipartFile parameter, not in DTO
}
