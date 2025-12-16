/**
 * Product Management Types
 *
 * Type definitions for Product Management feature.
 * These types match the backend DTOs for type safety.
 */

export type UnitOfMeasure = 'EA' | 'CS' | 'PK' | 'BOX' | 'PAL';

export interface CreateProductRequest {
  productCode: string;
  description: string;
  primaryBarcode: string;
  unitOfMeasure: UnitOfMeasure;
  secondaryBarcodes?: string[];
  category?: string;
  brand?: string;
}

export interface UpdateProductRequest {
  description: string;
  primaryBarcode: string;
  unitOfMeasure: UnitOfMeasure;
  secondaryBarcodes?: string[];
  category?: string;
  brand?: string;
}

export interface CreateProductResponse {
  productId: string;
  productCode: string;
  description: string;
  primaryBarcode: string;
  createdAt: string;
}

export interface UpdateProductResponse {
  productId: string;
  lastModifiedAt: string;
}

export interface Product {
  productId: string;
  productCode: string;
  description: string;
  primaryBarcode: string;
  secondaryBarcodes: string[];
  unitOfMeasure: UnitOfMeasure;
  category?: string;
  brand?: string;
  createdAt: string;
  lastModifiedAt?: string;
}

export interface ProductListFilters {
  tenantId?: string;
  page?: number;
  size?: number;
  category?: string;
  brand?: string;
  search?: string;
}

export interface UploadProductCsvResponse {
  totalRows: number;
  createdCount: number;
  updatedCount: number;
  errorCount: number;
  errors: ProductCsvError[];
}

export interface ProductCsvError {
  rowNumber: number;
  productCode: string;
  errorMessage: string;
}

export interface ProductCodeUniquenessResponse {
  productCode: string;
  isUnique: boolean;
}
