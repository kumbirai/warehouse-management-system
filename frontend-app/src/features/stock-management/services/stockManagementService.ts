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
  GetConsignmentApiResponse,
  GetStockItemApiResponse,
  GetStockItemsByClassificationApiResponse,
  StockAdjustmentFilters,
  StockAdjustmentListQueryResult,
  StockAdjustmentResponse,
  StockAllocationFilters,
  StockAllocationListQueryResult,
  StockAllocationResponse,
  StockClassification,
  StockLevelFilters,
  StockLevelResponse,
  UploadConsignmentCsvApiResponse,
  UploadConsignmentCsvRequest,
  ValidateConsignmentApiResponse,
  ValidateConsignmentRequest,
} from '../types/stockManagement';

const STOCK_MANAGEMENT_BASE_PATH = '/stock-management';

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
   * Releases a stock allocation.
   */
  async releaseStockAllocation(
    allocationId: string,
    tenantId: string
  ): Promise<ApiResponse<{ allocationId: string; status: string; releasedAt: string }>> {
    const response = await apiClient.put<
      ApiResponse<{ allocationId: string; status: string; releasedAt: string }>
    >(
      `${STOCK_MANAGEMENT_BASE_PATH}/allocations/${allocationId}/release`,
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
   * Gets all stock items for a tenant (Sprint 3).
   */
  async getAllStockItems(tenantId: string): Promise<GetStockItemsByClassificationApiResponse> {
    const response = await apiClient.get<GetStockItemsByClassificationApiResponse>(
      `${STOCK_MANAGEMENT_BASE_PATH}/stock-items/all`,
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

  /**
   * Gets a stock allocation by ID.
   */
  async getStockAllocation(
    allocationId: string,
    tenantId: string
  ): Promise<ApiResponse<StockAllocationResponse>> {
    const response = await apiClient.get<ApiResponse<StockAllocationResponse>>(
      `${STOCK_MANAGEMENT_BASE_PATH}/allocations/${allocationId}`,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },

  /**
   * Lists stock allocations with filters and pagination.
   */
  async listStockAllocations(
    filters: StockAllocationFilters,
    tenantId: string
  ): Promise<ApiResponse<StockAllocationListQueryResult>> {
    const params = new URLSearchParams();
    if (filters.productId) params.append('productId', filters.productId);
    if (filters.locationId) params.append('locationId', filters.locationId);
    if (filters.referenceId) params.append('referenceId', filters.referenceId);
    if (filters.status) params.append('status', filters.status);
    if (filters.page) params.append('page', filters.page.toString());
    if (filters.size) params.append('size', filters.size.toString());

    const response = await apiClient.get<ApiResponse<StockAllocationListQueryResult>>(
      `${STOCK_MANAGEMENT_BASE_PATH}/allocations?${params.toString()}`,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },

  /**
   * Gets a stock adjustment by ID.
   */
  async getStockAdjustment(
    adjustmentId: string,
    tenantId: string
  ): Promise<ApiResponse<StockAdjustmentResponse>> {
    const response = await apiClient.get<ApiResponse<StockAdjustmentResponse>>(
      `${STOCK_MANAGEMENT_BASE_PATH}/adjustments/${adjustmentId}`,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },

  /**
   * Lists stock adjustments with filters and pagination.
   */
  async listStockAdjustments(
    filters: StockAdjustmentFilters,
    tenantId: string
  ): Promise<ApiResponse<StockAdjustmentListQueryResult>> {
    const params = new URLSearchParams();
    if (filters.productId) params.append('productId', filters.productId);
    if (filters.locationId) params.append('locationId', filters.locationId);
    if (filters.stockItemId) params.append('stockItemId', filters.stockItemId);
    if (filters.adjustmentType) params.append('adjustmentType', filters.adjustmentType);
    if (filters.reason) params.append('reason', filters.reason);
    if (filters.dateFrom) params.append('dateFrom', filters.dateFrom);
    if (filters.dateTo) params.append('dateTo', filters.dateTo);
    if (filters.page) params.append('page', filters.page.toString());
    if (filters.size) params.append('size', filters.size.toString());

    const response = await apiClient.get<ApiResponse<StockAdjustmentListQueryResult>>(
      `${STOCK_MANAGEMENT_BASE_PATH}/adjustments?${params.toString()}`,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },

  /**
   * Checks if stock is expired for a product at a location.
   * Uses productCode (not productId) as the backend converts it internally.
   */
  async checkStockExpiration(
    productCode: string,
    locationId: string,
    tenantId: string
  ): Promise<ApiResponse<{ expired: boolean; classification: string; message: string }>> {
    const params = new URLSearchParams();
    params.append('productCode', productCode);
    params.append('locationId', locationId);

    const response = await apiClient.get<
      ApiResponse<{ expired: boolean; classification: string; message: string }>
    >(`${STOCK_MANAGEMENT_BASE_PATH}/stock-items/check-expiration?${params.toString()}`, {
      headers: {
        'X-Tenant-Id': tenantId,
      },
    });
    return response.data;
  },

  /**
   * Gets expiring stock items within a date range.
   */
  async getExpiringStock(
    filters: {
      startDate?: string;
      endDate?: string;
      classification?: string;
    },
    tenantId: string
  ): Promise<
    ApiResponse<
      Array<{
        stockItemId: string;
        productId: string;
        productCode: string;
        locationId: string;
        quantity: number;
        expirationDate: string;
        classification: string;
        daysUntilExpiry?: number;
      }>
    >
  > {
    const params = new URLSearchParams();
    if (filters.startDate) params.append('startDate', filters.startDate);
    if (filters.endDate) params.append('endDate', filters.endDate);
    if (filters.classification) params.append('classification', filters.classification);

    const response = await apiClient.get<
      ApiResponse<
        Array<{
          stockItemId: string;
          productId: string;
          productCode: string;
          locationId: string;
          quantity: number;
          expirationDate: string;
          classification: string;
          daysUntilExpiry?: number;
        }>
      >
    >(`${STOCK_MANAGEMENT_BASE_PATH}/stock-items/expiring?${params.toString()}`, {
      headers: {
        'X-Tenant-Id': tenantId,
      },
    });
    return response.data;
  },

  /**
   * Lists restock requests with filters.
   */
  async listRestockRequests(
    filters: {
      status?: string;
      priority?: string;
      productId?: string;
      page?: number;
      size?: number;
    },
    tenantId: string
  ): Promise<
    ApiResponse<{
      requests: Array<{
        restockRequestId: string;
        productId: string;
        locationId?: string;
        currentQuantity: number;
        minimumQuantity: number;
        maximumQuantity?: number;
        requestedQuantity: number;
        priority: string;
        status: string;
        createdAt: string;
        sentToD365At?: string;
        d365OrderReference?: string;
      }>;
      totalCount: number;
    }>
  > {
    const params = new URLSearchParams();
    if (filters.status) params.append('status', filters.status);
    if (filters.priority) params.append('priority', filters.priority);
    if (filters.productId) params.append('productId', filters.productId);
    if (filters.page) params.append('page', filters.page.toString());
    if (filters.size) params.append('size', filters.size.toString());

    const response = await apiClient.get<
      ApiResponse<{
        requests: Array<{
          restockRequestId: string;
          productId: string;
          locationId?: string;
          currentQuantity: number;
          minimumQuantity: number;
          maximumQuantity?: number;
          requestedQuantity: number;
          priority: string;
          status: string;
          createdAt: string;
          sentToD365At?: string;
          d365OrderReference?: string;
        }>;
        totalCount: number;
      }>
    >(`${STOCK_MANAGEMENT_BASE_PATH}/restock-requests?${params.toString()}`, {
      headers: {
        'X-Tenant-Id': tenantId,
      },
    });
    return response.data;
  },
};
