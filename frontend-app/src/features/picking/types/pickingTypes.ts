import {ApiResponse} from '../../../types/api';

/**
 * Picking Types
 * <p>
 * TypeScript types for Picking feature.
 */

export type PickingListStatus = 'RECEIVED' | 'PROCESSING' | 'PLANNED' | 'COMPLETED';
export type LoadStatus = 'CREATED' | 'PLANNED' | 'IN_PROGRESS' | 'COMPLETED';
export type OrderStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export type PickingTaskStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED';
export type Priority = 'HIGH' | 'NORMAL' | 'LOW';

// CSV Upload Types
export interface UploadPickingListCsvRequest {
  file: File;
}

export interface CsvValidationError {
  rowNumber: number;
  fieldName: string;
  errorMessage: string;
  invalidValue?: string;
}

export interface UploadPickingListCsvResponse {
  totalRows: number;
  successfulRows: number;
  errorRows: number;
  createdPickingListIds: string[];
  errors: CsvValidationError[];
}

export type UploadPickingListCsvApiResponse = ApiResponse<UploadPickingListCsvResponse>;

// Manual Entry Types
export interface OrderLineItem {
  productCode: string;
  quantity: number;
  notes?: string;
}

export interface Order {
  orderNumber: string;
  customerCode: string;
  customerName?: string;
  priority: Priority;
  lineItems: OrderLineItem[];
}

export interface Load {
  loadNumber: string;
  orders: Order[];
}

export interface CreatePickingListRequest {
  loads: Load[];
  notes?: string;
}

export interface CreatePickingListResponse {
  pickingListId: string;
  status: PickingListStatus;
  receivedAt: string;
  loadCount: number;
}

export type CreatePickingListApiResponse = ApiResponse<CreatePickingListResponse>;

// Query Types
export interface OrderLineItemQueryResult {
  lineItemId: string;
  productCode: string;
  productDescription?: string;
  quantity: number;
  notes?: string;
}

export interface OrderQueryResult {
  orderId: string;
  orderNumber: string;
  customerCode: string;
  customerName: string;
  priority: Priority;
  status: OrderStatus;
  lineItems: OrderLineItemQueryResult[];
}

export interface LoadQueryResult {
  loadId: string;
  loadNumber: string;
  status: LoadStatus;
  orderCount: number;
  orders: OrderQueryResult[];
}

export interface PickingListQueryResult {
  id: string;
  pickingListReference?: string;
  status: PickingListStatus;
  receivedAt: string;
  processedAt?: string;
  loadCount: number;
  totalOrderCount: number;
  notes?: string;
  loads: LoadQueryResult[];
}

export type GetPickingListApiResponse = ApiResponse<PickingListQueryResult>;

export interface PickingListView {
  id: string;
  pickingListReference?: string;
  status: PickingListStatus;
  loadCount: number;
  totalOrderCount: number;
}

export interface ListPickingListsQueryResult {
  pickingLists: PickingListView[];
  totalElements: number;
  page: number;
  size: number;
  totalPages: number;
}

export type ListPickingListsApiResponse = ApiResponse<ListPickingListsQueryResult>;

export interface ListPickingListsFilters {
  page?: number;
  size?: number;
  status?: PickingListStatus;
}

// Load Query Types
export interface LoadDetailQueryResult {
  id: string;
  loadNumber: string;
  status: LoadStatus;
  createdAt: string;
  plannedAt?: string;
  orders: OrderQueryResult[];
}

export type GetLoadApiResponse = ApiResponse<LoadDetailQueryResult>;

export interface ListOrdersByLoadQueryResult {
  loadId: string;
  orders: OrderQueryResult[];
}

export type ListOrdersByLoadApiResponse = ApiResponse<ListOrdersByLoadQueryResult>;
