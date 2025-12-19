import {ApiResponse} from '../../../types/api';

/**
 * Stock Management Types
 * <p>
 * TypeScript types for Stock Management feature.
 */

export interface ConsignmentLineItem {
  productCode: string;
  quantity: number;
  expirationDate?: string; // ISO date string (YYYY-MM-DD)
  batchNumber?: string;
}

export interface CreateConsignmentRequest {
  consignmentReference: string;
  warehouseId: string;
  receivedAt: string; // ISO date-time string
  receivedBy?: string;
  lineItems: ConsignmentLineItem[];
}

export interface CreateConsignmentResponse {
  consignmentId: string;
  consignmentReference: string;
  status: ConsignmentStatus;
  createdAt: string; // ISO date-time string
}

export interface Consignment {
  consignmentId: string;
  consignmentReference: string;
  warehouseId: string;
  status: ConsignmentStatus;
  receivedAt: string; // ISO date-time string
  confirmedAt?: string; // ISO date-time string
  receivedBy?: string;
  lineItems: ConsignmentLineItem[];
  createdAt: string; // ISO date-time string
  lastModifiedAt: string; // ISO date-time string
}

export type ConsignmentStatus = 'RECEIVED' | 'CONFIRMED' | 'CANCELLED' | 'REJECTED';

export interface ValidateConsignmentRequest {
  consignmentId: string;
}

export interface ValidateConsignmentResponse {
  consignmentId: string;
  newStatus: ConsignmentStatus;
  confirmedAt: string; // ISO date-time string
}

export interface UploadConsignmentCsvRequest {
  file: File;
}

export interface ConsignmentCsvError {
  rowNumber: number;
  consignmentReference: string;
  productCode: string;
  errorMessage: string;
}

export interface UploadConsignmentCsvResponse {
  totalRows: number;
  createdCount: number;
  updatedCount: number;
  errorCount: number;
  errors: ConsignmentCsvError[];
}

export interface ProductBarcodeValidationRequest {
  barcode: string;
}

export interface ProductBarcodeValidationResponse {
  valid: boolean;
  productInfo?: {
    productId: string;
    productCode: string;
    description: string;
    primaryBarcode: string;
  };
  barcodeFormat: BarcodeType;
}

export type BarcodeType = 'EAN_13' | 'UPC_A' | 'CODE_128' | 'CODE_39' | 'ITF_14' | 'UNKNOWN';

// API Response types
export type CreateConsignmentApiResponse = ApiResponse<CreateConsignmentResponse>;
export type GetConsignmentApiResponse = ApiResponse<Consignment>;
export type ValidateConsignmentApiResponse = ApiResponse<ValidateConsignmentResponse>;
export type UploadConsignmentCsvApiResponse = ApiResponse<UploadConsignmentCsvResponse>;
export type ProductBarcodeValidationApiResponse = ApiResponse<ProductBarcodeValidationResponse>;
