package com.ccbsa.wms.product.application.dto.command;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Result DTO: UpdateProductResultDTO
 * <p>
 * API response DTO for product update result.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class UpdateProductResultDTO {
    private String productId;
    private LocalDateTime lastModifiedAt;
}

