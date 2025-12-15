import apiClient from '../../../services/apiClient';
import { ApiResponse } from '../../../types/api';
import {
  CreateTenantRequest,
  CreateTenantResponse,
  Tenant,
  TenantListFilters,
} from '../types/tenant';
import { logger } from '../../../utils/logger';

const TENANT_BASE_PATH = '/tenants';

export interface TenantListApiResponse extends ApiResponse<Tenant[]> {}

export const tenantService = {
  async listTenants(filters: TenantListFilters): Promise<TenantListApiResponse> {
    const response = await apiClient.get<TenantListApiResponse>(TENANT_BASE_PATH, {
      params: {
        page: filters.page,
        size: filters.size,
        status: filters.status,
        search: filters.search,
      },
    });

    // Debug logging to see what we're receiving
    if (import.meta.env.DEV) {
      logger.debug('Tenant list raw axios response:', {
        status: response.status,
        statusText: response.statusText,
        headers: response.headers,
        hasData: !!response.data,
        dataType: typeof response.data,
        isArray: Array.isArray(response.data?.data),
        dataLength: Array.isArray(response.data?.data) ? response.data.data.length : 'N/A',
        dataKeys: response.data ? Object.keys(response.data) : [],
        fullResponse: JSON.stringify(response.data, null, 2),
      });

      // Log first tenant's createdAt field for debugging
      if (Array.isArray(response.data?.data) && response.data.data.length > 0) {
        const firstTenant = response.data.data[0];
        logger.debug('First tenant createdAt field:', {
          createdAt: firstTenant?.createdAt,
          createdAtType: typeof firstTenant?.createdAt,
          createdAtIsArray: Array.isArray(firstTenant?.createdAt),
          fullTenant: JSON.stringify(firstTenant, null, 2),
        });
      }
    }

    return response.data;
  },

  async getTenant(tenantId: string): Promise<ApiResponse<Tenant>> {
    const response = await apiClient.get<ApiResponse<Tenant>>(`${TENANT_BASE_PATH}/${tenantId}`);
    return response.data;
  },

  async createTenant(request: CreateTenantRequest): Promise<ApiResponse<CreateTenantResponse>> {
    const response = await apiClient.post<ApiResponse<CreateTenantResponse>>(
      TENANT_BASE_PATH,
      request
    );
    return response.data;
  },

  async activateTenant(tenantId: string): Promise<ApiResponse<void>> {
    const response = await apiClient.put<ApiResponse<void>>(
      `${TENANT_BASE_PATH}/${tenantId}/activate`
    );
    return response.data;
  },

  async deactivateTenant(tenantId: string): Promise<ApiResponse<void>> {
    const response = await apiClient.put<ApiResponse<void>>(
      `${TENANT_BASE_PATH}/${tenantId}/deactivate`
    );
    return response.data;
  },

  async suspendTenant(tenantId: string): Promise<ApiResponse<void>> {
    const response = await apiClient.put<ApiResponse<void>>(
      `${TENANT_BASE_PATH}/${tenantId}/suspend`
    );
    return response.data;
  },
};
