import apiClient from '../../../services/apiClient';
import { ApiResponse } from '../../../types/api';

/**
 * Stock Movement Service
 *
 * Handles API calls for stock movement operations.
 */

export interface CreateStockMovementRequest {
  stockItemId: string;
  productId: string;
  sourceLocationId: string;
  destinationLocationId: string;
  quantity: number;
  movementType:
    | 'RECEIVING_TO_STORAGE'
    | 'STORAGE_TO_PICKING'
    | 'INTER_STORAGE'
    | 'PICKING_TO_SHIPPING'
    | 'OTHER';
  reason: 'PICKING' | 'RESTOCKING' | 'REORGANIZATION' | 'DAMAGE' | 'CORRECTION' | 'OTHER';
}

export interface CreateStockMovementResponse {
  stockMovementId: string;
  status: string;
  initiatedAt: string;
}

export interface CompleteStockMovementResponse {
  stockMovementId: string;
  status: string;
  completedAt: string;
}

export interface CancelStockMovementRequest {
  cancellationReason: string;
}

export interface CancelStockMovementResponse {
  stockMovementId: string;
  status: string;
  cancelledAt: string;
  cancellationReason: string;
}

export interface StockMovement {
  stockMovementId: string;
  stockItemId: string;
  productId: string;
  sourceLocationId: string;
  destinationLocationId: string;
  quantity: number;
  movementType: string;
  reason: string;
  status: string;
  initiatedBy?: string;
  initiatedAt: string;
  completedBy?: string;
  completedAt?: string;
  cancelledBy?: string;
  cancelledAt?: string;
  cancellationReason?: string;
}

export interface StockMovementListFilters {
  stockItemId?: string;
  sourceLocationId?: string;
  destinationLocationId?: string;
  status?: string;
  page?: number;
  size?: number;
}

export interface StockMovementListQueryResult {
  movements: StockMovement[];
  totalCount: number;
}

const BASE_PATH = '/location-management/stock-movements';

export const stockMovementService = {
  /**
   * Creates a new stock movement.
   */
  async createStockMovement(
    request: CreateStockMovementRequest,
    tenantId: string
  ): Promise<ApiResponse<CreateStockMovementResponse>> {
    const response = await apiClient.post<ApiResponse<CreateStockMovementResponse>>(
      BASE_PATH,
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
   * Completes a stock movement.
   */
  async completeStockMovement(
    movementId: string,
    tenantId: string
  ): Promise<ApiResponse<CompleteStockMovementResponse>> {
    const response = await apiClient.put<ApiResponse<CompleteStockMovementResponse>>(
      `${BASE_PATH}/${movementId}/complete`,
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
   * Cancels a stock movement.
   */
  async cancelStockMovement(
    movementId: string,
    request: CancelStockMovementRequest,
    tenantId: string
  ): Promise<ApiResponse<CancelStockMovementResponse>> {
    const response = await apiClient.put<ApiResponse<CancelStockMovementResponse>>(
      `${BASE_PATH}/${movementId}/cancel`,
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
   * Gets a stock movement by ID.
   */
  async getStockMovement(
    movementId: string,
    tenantId: string
  ): Promise<ApiResponse<StockMovement>> {
    const response = await apiClient.get<ApiResponse<StockMovement>>(`${BASE_PATH}/${movementId}`, {
      headers: {
        'X-Tenant-Id': tenantId,
      },
    });
    return response.data;
  },

  /**
   * Lists stock movements with filters.
   */
  async listStockMovements(
    filters: StockMovementListFilters,
    tenantId: string
  ): Promise<ApiResponse<StockMovementListQueryResult>> {
    const params = new URLSearchParams();
    if (filters.stockItemId) params.append('stockItemId', filters.stockItemId);
    if (filters.sourceLocationId) params.append('sourceLocationId', filters.sourceLocationId);
    if (filters.destinationLocationId)
      params.append('destinationLocationId', filters.destinationLocationId);
    if (filters.status) params.append('status', filters.status);
    if (filters.page !== undefined) params.append('page', filters.page.toString());
    if (filters.size !== undefined) params.append('size', filters.size.toString());

    const response = await apiClient.get<ApiResponse<StockMovementListQueryResult>>(
      `${BASE_PATH}?${params.toString()}`,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  },
};
