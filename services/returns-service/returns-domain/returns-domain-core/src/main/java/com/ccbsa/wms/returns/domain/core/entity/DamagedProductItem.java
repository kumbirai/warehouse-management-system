package com.ccbsa.wms.returns.domain.core.entity;

import java.util.Objects;

import com.ccbsa.common.domain.valueobject.DamageSeverity;
import com.ccbsa.common.domain.valueobject.DamageSource;
import com.ccbsa.common.domain.valueobject.DamageType;
import com.ccbsa.common.domain.valueobject.Notes;
import com.ccbsa.common.domain.valueobject.ProductCondition;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.wms.returns.domain.core.valueobject.DamagedProductItemId;

/**
 * Entity: DamagedProductItem
 * <p>
 * Represents a damaged product item within a damage assessment. Immutable entity within DamageAssessment aggregate.
 * <p>
 * Business Rules:
 * - Product ID must not be null
 * - Damaged quantity must be > 0
 * - Damaged quantity cannot exceed ordered quantity
 * - Damage type, severity, and source must not be null
 * - Product condition cannot be GOOD for damaged items
 * - Photo URL is optional but recommended
 */
public class DamagedProductItem {
    private final DamagedProductItemId id;
    private final ProductId productId;
    private final Quantity damagedQuantity;
    private final DamageType damageType;
    private final DamageSeverity damageSeverity;
    private final DamageSource damageSource;
    private final String photoUrl;
    private final Notes notes;

    private DamagedProductItem(DamagedProductItemId id, ProductId productId, Quantity damagedQuantity, DamageType damageType, DamageSeverity damageSeverity,
                               DamageSource damageSource, String photoUrl, Notes notes) {
        if (id == null) {
            throw new IllegalArgumentException("DamagedProductItemId cannot be null");
        }
        if (productId == null) {
            throw new IllegalArgumentException("ProductId cannot be null");
        }
        if (damagedQuantity == null || !damagedQuantity.isPositive()) {
            throw new IllegalArgumentException("Damaged quantity must be positive");
        }
        if (damageType == null) {
            throw new IllegalArgumentException("Damage type cannot be null");
        }
        if (damageSeverity == null) {
            throw new IllegalArgumentException("Damage severity cannot be null");
        }
        if (damageSource == null) {
            throw new IllegalArgumentException("Damage source cannot be null");
        }
        // Notes validation is handled by Notes value object
        if (photoUrl != null && photoUrl.length() > 500) {
            throw new IllegalArgumentException("Photo URL cannot exceed 500 characters");
        }

        this.id = id;
        this.productId = productId;
        this.damagedQuantity = damagedQuantity;
        this.damageType = damageType;
        this.damageSeverity = damageSeverity;
        this.damageSource = damageSource;
        this.photoUrl = photoUrl;
        this.notes = notes;
    }

    /**
     * Factory method to create DamagedProductItem.
     *
     * @param id              Damaged product item ID
     * @param productId       Product ID
     * @param damagedQuantity Damaged quantity
     * @param damageType      Damage type
     * @param damageSeverity  Damage severity
     * @param damageSource    Damage source
     * @param photoUrl        Optional photo URL
     * @param notes           Optional notes
     * @return DamagedProductItem instance
     */
    public static DamagedProductItem create(DamagedProductItemId id, ProductId productId, Quantity damagedQuantity, DamageType damageType, DamageSeverity damageSeverity,
                                            DamageSource damageSource, String photoUrl, Notes notes) {
        Notes itemNotes = notes != null ? notes : Notes.forLineItem(null);
        return new DamagedProductItem(id, productId, damagedQuantity, damageType, damageSeverity, damageSource, photoUrl, itemNotes);
    }

    public DamagedProductItemId getId() {
        return id;
    }

    public ProductId getProductId() {
        return productId;
    }

    public Quantity getDamagedQuantity() {
        return damagedQuantity;
    }

    public DamageType getDamageType() {
        return damageType;
    }

    public DamageSeverity getDamageSeverity() {
        return damageSeverity;
    }

    public DamageSource getDamageSource() {
        return damageSource;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public Notes getNotes() {
        return notes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DamagedProductItem that = (DamagedProductItem) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("DamagedProductItem{id=%s, productId=%s, damagedQuantity=%s, damageType=%s, damageSeverity=%s}", id, productId, damagedQuantity, damageType,
                damageSeverity);
    }
}
