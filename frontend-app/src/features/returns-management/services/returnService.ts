import apiClient from '../../../services/apiClient';
import { ApiResponse } from '../../../types/api';
import { Return, ReturnListFilters } from '../types/returns';
import { logger } from '../../../utils/logger';

const RETURNS_BASE_PATH = '/returns';

export interface ReturnListApiResponse extends ApiResponse<Return[]> {}

export const returnService = {
  async getReturn(returnId: string, tenantId: string): Promise<ApiResponse<Return>> {
    const response = await apiClient.get<ApiResponse<Return>>(`${RETURNS_BASE_PATH}/${returnId}`, {
      headers: {
        'X-Tenant-Id': tenantId,
      },
    });

    return response.data;
  },

  async listReturns(filters: ReturnListFilters): Promise<ReturnListApiResponse> {
    const headers: Record<string, string> = {};
    if (filters.tenantId) {
      headers['X-Tenant-Id'] = filters.tenantId;
    }

    if (import.meta.env.DEV) {
      logger.debug('Calling listReturns API:', {
        path: RETURNS_BASE_PATH,
        params: {
          page: filters.page,
          size: filters.size,
          status: filters.status,
          search: filters.search,
        },
        headers,
      });
    }

    const response = await apiClient.get<ReturnListApiResponse>(RETURNS_BASE_PATH, {
      params: {
        page: filters.page,
        size: filters.size,
        status: filters.status,
        search: filters.search,
      },
      headers,
    });

    return response.data;
  },
};
