package com.ccbsa.wms.tenant.dataaccess.converter;

import com.ccbsa.wms.tenant.domain.core.valueobject.TenantStatus;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA Attribute Converter: TenantStatusConverter
 * <p>
 * Converts between TenantStatus domain enum and String for database storage.
 */
@Converter(autoApply = true)
public class TenantStatusConverter implements AttributeConverter<TenantStatus, String> {
    @Override
    public String convertToDatabaseColumn(TenantStatus tenantStatus) {
        if (tenantStatus == null) {
            return null;
        }
        return tenantStatus.name();
    }

    @Override
    public TenantStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return TenantStatus.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Invalid tenant status value: %s",
                    dbData),
                    e);
        }
    }
}

