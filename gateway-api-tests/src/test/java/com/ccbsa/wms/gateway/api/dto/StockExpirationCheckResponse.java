package com.ccbsa.wms.gateway.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockExpirationCheckResponse {
    private boolean expired; // Matches service DTO field name
    private String classification; // CRITICAL, NEAR_EXPIRY, EXPIRED, etc.
    private String message;
    private java.time.LocalDate expirationDate;
    private Integer daysUntilExpiration;
    
    // Convenience getter for backward compatibility
    public Boolean getIsExpired() {
        return expired;
    }
}
