/**
 * Returns Management Types
 *
 * TypeScript interfaces matching backend DTOs
 */

export enum ReturnReason {
  DEFECTIVE = 'DEFECTIVE',
  WRONG_ITEM = 'WRONG_ITEM',
  DAMAGED = 'DAMAGED',
  EXPIRED = 'EXPIRED',
  CUSTOMER_REQUEST = 'CUSTOMER_REQUEST',
  OTHER = 'OTHER',
}

export enum ProductCondition {
  GOOD = 'GOOD',
  DAMAGED = 'DAMAGED',
  EXPIRED = 'EXPIRED',
  QUARANTINE = 'QUARANTINE',
  WRITE_OFF = 'WRITE_OFF',
}

export enum ReturnStatus {
  INITIATED = 'INITIATED',
  PROCESSED = 'PROCESSED',
  LOCATION_ASSIGNED = 'LOCATION_ASSIGNED',
  RECONCILED = 'RECONCILED',
  CANCELLED = 'CANCELLED',
}

export enum ReturnType {
  PARTIAL = 'PARTIAL',
  FULL = 'FULL',
  DAMAGE_IN_TRANSIT = 'DAMAGE_IN_TRANSIT',
}

export interface ReturnLineItem {
  lineItemId: string;
  productId: string;
  orderedQuantity: number;
  pickedQuantity: number;
  acceptedQuantity: number;
  returnedQuantity: number;
  productCondition?: ProductCondition;
  returnReason: ReturnReason;
  lineNotes?: string;
}

export interface Return {
  returnId: string;
  orderNumber: string;
  returnType: ReturnType;
  status: ReturnStatus;
  lineItems: ReturnLineItem[];
  customerSignature?: CustomerSignature;
  primaryReturnReason?: ReturnReason;
  returnNotes?: string;
  returnedAt: string;
  createdAt: string;
  lastModifiedAt: string;
}

export interface CustomerSignature {
  signatureData: string; // base64 encoded image
  timestamp: string;
}

export interface HandlePartialOrderAcceptanceRequest {
  orderNumber: string;
  lineItems: PartialOrderLineItem[];
  customerSignature: CustomerSignature;
  returnNotes?: string;
}

export interface PartialOrderLineItem {
  productId: string;
  acceptedQuantity: number;
  returnReason: ReturnReason;
  lineNotes?: string;
}

export interface HandlePartialOrderAcceptanceResponse {
  returnId: string;
  orderNumber: string;
  status: ReturnStatus;
}

export interface ProcessFullOrderReturnRequest {
  returnId: string;
  primaryReturnReason: ReturnReason;
  lineItems: FullReturnLineItem[];
  returnNotes?: string;
}

export interface FullReturnLineItem {
  productId: string;
  productCondition: ProductCondition;
  returnReason: ReturnReason;
  lineNotes?: string;
}

export interface ProcessFullOrderReturnResponse {
  returnId: string;
  status: ReturnStatus;
}

export interface ReturnListFilters {
  tenantId?: string;
  status?: ReturnStatus;
  page?: number;
  size?: number;
  search?: string;
}
