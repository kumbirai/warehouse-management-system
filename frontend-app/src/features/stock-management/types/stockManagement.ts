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

export type ConsignmentStatus = 'RECEIVED' | 'CONFIRMED' | 'CANCELLED';

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

// Consignment List Types
export interface ConsignmentListFilters {
  page?: number;
  size?: number;
  status?: ConsignmentStatus;
  search?: string;
  expiringWithinDays?: number;
}

export interface ConsignmentListQueryResult {
  consignments: Consignment[];
  totalCount: number;
  page: number;
  size: number;
  totalPages: number;
}

// Stock Allocation Types
export interface CreateStockAllocationRequest {
  productId: string;
  sourceLocationId: string;
  quantity: number;
  allocationType: 'PICKING_ORDER' | 'RESERVATION' | 'OTHER';
  referenceId?: string;
}

export interface CreateStockAllocationResponse {
  allocationId: string;
  productId: string;
  sourceLocationId: string;
  quantity: number;
  allocationType: string;
}

// Stock Movement Types
export interface CreateStockMovementRequest {
  productId: string;
  sourceLocationId: string;
  targetLocationId: string;
  quantity: number;
  movementType: 'RELOCATION' | 'PICKING' | 'PUTAWAY' | 'OTHER';
  reason?: string;
}

export interface CreateStockMovementResponse {
  movementId: string;
  productId: string;
  sourceLocationId: string;
  targetLocationId: string;
  quantity: number;
  movementType: string;
}

// Stock Adjustment Types
export interface CreateStockAdjustmentRequest {
  consignmentId: string;
  adjustmentType: 'INCREASE' | 'DECREASE' | 'CORRECTION';
  quantity: number;
  reason: string;
}

export interface CreateStockAdjustmentResponse {
  adjustmentId: string;
  consignmentId: string;
  adjustmentType: string;
  quantity: number;
  newQuantity: number;
}

// Stock Level Types
export interface StockLevelFilters {
  productId?: string;
  locationId?: string;
}

export interface StockLevelResponse {
  stockLevelId: string;
  productId: string;
  locationId: string;
  availableQuantity: number;
  allocatedQuantity: number;
  totalQuantity: number;
  minimumQuantity?: number;
  maximumQuantity?: number;
}

// Stock Item Types (Sprint 3)
export type StockClassification =
  | 'EXPIRED'
  | 'CRITICAL'
  | 'NEAR_EXPIRY'
  | 'NORMAL'
  | 'EXTENDED_SHELF_LIFE';

export interface StockItem {
  stockItemId: string;
  productId: string;
  locationId?: string;
  quantity: number;
  expirationDate?: string; // ISO date string (YYYY-MM-DD)
  classification: StockClassification;
  createdAt: string; // ISO date-time string
  lastModifiedAt: string; // ISO date-time string
}

export interface GetStockItemResponse {
  stockItemId: string;
  productId: string;
  locationId?: string;
  quantity: number;
  expirationDate?: string;
  classification: StockClassification;
  createdAt: string;
  lastModifiedAt: string;
}

export interface GetStockItemsByClassificationResponse {
  stockItems: StockItem[];
}

export interface AssignLocationToStockRequest {
  locationId: string;
  quantity: number;
}

// API Response types
export type CreateConsignmentApiResponse = ApiResponse<CreateConsignmentResponse>;
export type GetConsignmentApiResponse = ApiResponse<Consignment>;
export type ConsignmentListApiResponse = ApiResponse<ConsignmentListQueryResult>;
export type ValidateConsignmentApiResponse = ApiResponse<ValidateConsignmentResponse>;
export type UploadConsignmentCsvApiResponse = ApiResponse<UploadConsignmentCsvResponse>;
export type ProductBarcodeValidationApiResponse = ApiResponse<ProductBarcodeValidationResponse>;
export type GetStockItemApiResponse = ApiResponse<GetStockItemResponse>;
export type GetStockItemsByClassificationApiResponse =
  ApiResponse<GetStockItemsByClassificationResponse>;
