package com.ccbsa.common.domain.valueobject;

/**
 * Value Object: DamageType
 * <p>
 * Represents the type of damage to a product. Immutable enum.
 * <p>
 * This value object is shared across services (DRY principle).
 * <p>
 * Business Rules:
 * - DamageType cannot be null
 * - Each type represents a specific category of damage
 */
public enum DamageType {
    /**
     * Physical damage (dents, cracks, breaks)
     */
    PHYSICAL,

    /**
     * Water damage
     */
    WATER,

    /**
     * Temperature damage (frozen, overheated)
     */
    TEMPERATURE,

    /**
     * Contamination damage
     */
    CONTAMINATION,

    /**
     * Other types of damage not covered by specific categories
     */
    OTHER
}
