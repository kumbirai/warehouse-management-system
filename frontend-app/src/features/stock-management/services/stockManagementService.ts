import apiClient from '../../../services/apiClient';
import { ApiResponse } from '../../../types/api';
import {
  AssignLocationToStockRequest,
  ConsignmentListApiResponse,
  ConsignmentListFilters,
  CreateConsignmentApiResponse,
  CreateConsignmentRequest,
  CreateStockAdjustmentRequest,
  CreateStockAdjustmentResponse,
  CreateStockAllocationRequest,
  CreateStockAllocationResponse,
  CreateStockMovementRequest,
  CreateStockMovementResponse,
  GetConsignmentApiResponse,
  GetStockItemApiResponse,
  GetStockItemsByClassificationApiResponse,
  StockClassification,
  StockLevelFilters,
  StockLevelResponse,
  UploadConsignmentCsvApiResponse,
  UploadConsignmentCsvRequest,
  ValidateConsignmentApiResponse,
  ValidateConsignmentRequest,
} from '../types/stockManagement';

const STOCK_MANAGEMENT_BASE_PATH = '/api/v1/stock-management';

export const stockManagementService = {
  /**
   * Creates a new stock consignment.
   */
  /**
   * Creates a new stock consignment.
   */
  async createConsignment(
    request: CreateConsignmentRequest,
    tenantId: string
  ): Promise<CreateConsignmentApiResponse> {
    const response = await apiClient.post<CreateConsignmentApiResponse>(
      `${STOCK_MANAGEMENT_BASE_PATH}/consignments`,
      request,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },

  /**
   * Gets a consignment by ID.
   */
  async getConsignment(
    consignmentId: string,
    tenantId: string
  ): Promise<GetConsignmentApiResponse> {
    const response = await apiClient.get<GetConsignmentApiResponse>(
      `${STOCK_MANAGEMENT_BASE_PATH}/consignments/${consignmentId}`,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },

  /**
   * Lists consignments with filters and pagination.
   */
  async listConsignments(
    filters: ConsignmentListFilters,
    tenantId: string
  ): Promise<ConsignmentListApiResponse> {
    const params = new URLSearchParams();
    if (filters.page) params.append('page', filters.page.toString());
    if (filters.size) params.append('size', filters.size.toString());
    if (filters.status) params.append('status', filters.status);
    if (filters.search) params.append('search', filters.search);
    if (filters.expiringWithinDays)
      params.append('expiringWithinDays', filters.expiringWithinDays.toString());

    const response = await apiClient.get<ConsignmentListApiResponse>(
      `${STOCK_MANAGEMENT_BASE_PATH}/consignments?${params.toString()}`,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },

  /**
   * Validates (confirms) a consignment.
   */
  async validateConsignment(
    request: ValidateConsignmentRequest,
    tenantId: string
  ): Promise<ValidateConsignmentApiResponse> {
    const response = await apiClient.post<ValidateConsignmentApiResponse>(
      `${STOCK_MANAGEMENT_BASE_PATH}/consignments/${request.consignmentId}/validate`,
      {},
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },

  /**
   * Uploads a CSV file containing consignment data.
   */
  async uploadConsignmentCsv(
    request: UploadConsignmentCsvRequest,
    tenantId: string
  ): Promise<UploadConsignmentCsvApiResponse> {
    const formData = new FormData();
    formData.append('file', request.file);

    const response = await apiClient.post<UploadConsignmentCsvApiResponse>(
      `${STOCK_MANAGEMENT_BASE_PATH}/consignments/upload-csv`,
      formData,
      {
        headers: {
          'X-Tenant-Id': tenantId,
          'Content-Type': 'multipart/form-data',
        },
      }
    );
    return response.data;
  },

  /**
   * Creates a stock allocation.
   */
  async createStockAllocation(
    request: CreateStockAllocationRequest,
    tenantId: string
  ): Promise<ApiResponse<CreateStockAllocationResponse>> {
    const response = await apiClient.post<ApiResponse<CreateStockAllocationResponse>>(
      `${STOCK_MANAGEMENT_BASE_PATH}/allocations`,
      request,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },

  /**
   * Creates a stock movement.
   */
  async createStockMovement(
    request: CreateStockMovementRequest,
    tenantId: string
  ): Promise<ApiResponse<CreateStockMovementResponse>> {
    const response = await apiClient.post<ApiResponse<CreateStockMovementResponse>>(
      `${STOCK_MANAGEMENT_BASE_PATH}/movements`,
      request,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },

  /**
   * Creates a stock adjustment.
   */
  async createStockAdjustment(
    request: CreateStockAdjustmentRequest,
    tenantId: string
  ): Promise<ApiResponse<CreateStockAdjustmentResponse>> {
    const response = await apiClient.post<ApiResponse<CreateStockAdjustmentResponse>>(
      `${STOCK_MANAGEMENT_BASE_PATH}/adjustments`,
      request,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },

  /**
   * Gets stock levels by product and location.
   */
  async getStockLevels(
    filters: StockLevelFilters,
    tenantId: string
  ): Promise<ApiResponse<StockLevelResponse[]>> {
    const params = new URLSearchParams();
    if (filters.productId) params.append('productId', filters.productId);
    if (filters.locationId) params.append('locationId', filters.locationId);

    const response = await apiClient.get<ApiResponse<StockLevelResponse[]>>(
      `${STOCK_MANAGEMENT_BASE_PATH}/stock-levels?${params.toString()}`,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },

  /**
   * Gets a stock item by ID (Sprint 3).
   */
  async getStockItem(stockItemId: string, tenantId: string): Promise<GetStockItemApiResponse> {
    const response = await apiClient.get<GetStockItemApiResponse>(
      `${STOCK_MANAGEMENT_BASE_PATH}/stock-items/${stockItemId}`,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },

  /**
   * Gets stock items by classification (Sprint 3).
   */
  async getStockItemsByClassification(
    classification: StockClassification,
    tenantId: string
  ): Promise<GetStockItemsByClassificationApiResponse> {
    const response = await apiClient.get<GetStockItemsByClassificationApiResponse>(
      `${STOCK_MANAGEMENT_BASE_PATH}/stock-items/by-classification?classification=${classification}`,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },

  /**
   * Assigns a location to a stock item (Sprint 3).
   */
  async assignLocationToStock(
    stockItemId: string,
    request: AssignLocationToStockRequest,
    tenantId: string
  ): Promise<ApiResponse<void>> {
    const response = await apiClient.post<ApiResponse<void>>(
      `${STOCK_MANAGEMENT_BASE_PATH}/stock-items/${stockItemId}/assign-location`,
      request,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },

  /**
   * Confirms a consignment receipt (Sprint 3).
   */
  async confirmConsignment(consignmentId: string, tenantId: string): Promise<ApiResponse<void>> {
    const response = await apiClient.put<ApiResponse<void>>(
      `${STOCK_MANAGEMENT_BASE_PATH}/consignments/${consignmentId}/confirm`,
      {},
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },
};
