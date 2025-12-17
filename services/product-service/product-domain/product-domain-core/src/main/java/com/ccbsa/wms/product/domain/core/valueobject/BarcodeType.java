package com.ccbsa.wms.product.domain.core.valueobject;

/**
 * Enum: BarcodeType
 *
 * Represents the type of barcode used for product identification.
 *
 * Barcode Types: - EAN_13: European Article Number 13 digits (most common for retail products) - CODE_128: Code 128 barcode (alphanumeric, used in logistics) - UPC_A: Universal
 * Product Code A (12 digits, North America) - ITF_14:
 * Interleaved 2 of 5 (14 digits, used for cartons) - CODE_39: Code 39 (alphanumeric, older standard)
 */
public enum BarcodeType {
    EAN_13,
    CODE_128,
    UPC_A,
    ITF_14,
    CODE_39
}

