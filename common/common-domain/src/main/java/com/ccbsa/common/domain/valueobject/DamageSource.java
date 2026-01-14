package com.ccbsa.common.domain.valueobject;

/**
 * Value Object: DamageSource
 * <p>
 * Represents the source or cause of damage to a product. Immutable enum.
 * <p>
 * This value object is shared across services (DRY principle).
 * <p>
 * Business Rules:
 * - DamageSource cannot be null
 * - Each source represents a specific origin of damage
 */
public enum DamageSource {
    /**
     * Damage occurred during transport
     */
    TRANSPORT,

    /**
     * Damage occurred during handling
     */
    HANDLING,

    /**
     * Damage occurred during storage
     */
    STORAGE,

    /**
     * Damage occurred during manufacturing
     */
    MANUFACTURING,

    /**
     * Other sources not covered by specific categories
     */
    OTHER
}
