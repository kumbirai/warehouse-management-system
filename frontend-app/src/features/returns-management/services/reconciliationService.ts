import apiClient from '../../../services/apiClient';
import { ApiResponse } from '../../../types/api';
import {
  ReconciliationRecord,
  ReconciliationSummary,
  ReconciliationListFilters,
} from '../types/reconciliation';
import { logger } from '../../../utils/logger';

const INTEGRATION_BASE_PATH = '/integration';

export interface ReconciliationListApiResponse extends ApiResponse<ReconciliationRecord[]> {}

export interface ReconciliationSummaryApiResponse extends ApiResponse<ReconciliationSummary> {}

export const reconciliationService = {
  async listReconciliationRecords(
    filters: ReconciliationListFilters
  ): Promise<ReconciliationListApiResponse> {
    const headers: Record<string, string> = {};
    if (filters.tenantId) {
      headers['X-Tenant-Id'] = filters.tenantId;
    }

    if (import.meta.env.DEV) {
      logger.debug('Calling listReconciliationRecords API:', {
        path: `${INTEGRATION_BASE_PATH}/reconciliation`,
        params: {
          page: filters.page,
          size: filters.size,
          status: filters.status,
          search: filters.search,
        },
        headers,
      });
    }

    const response = await apiClient.get<ReconciliationListApiResponse>(
      `${INTEGRATION_BASE_PATH}/reconciliation`,
      {
        params: {
          page: filters.page,
          size: filters.size,
          status: filters.status,
          search: filters.search,
        },
        headers,
      }
    );

    return response.data;
  },

  async getReconciliationRecord(
    returnId: string,
    tenantId: string
  ): Promise<ApiResponse<ReconciliationRecord>> {
    const response = await apiClient.get<ApiResponse<ReconciliationRecord>>(
      `${INTEGRATION_BASE_PATH}/reconciliation/${returnId}`,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );

    return response.data;
  },

  async getReconciliationSummary(tenantId: string): Promise<ReconciliationSummaryApiResponse> {
    const response = await apiClient.get<ReconciliationSummaryApiResponse>(
      `${INTEGRATION_BASE_PATH}/reconciliation/summary`,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );

    return response.data;
  },

  async retryD365Sync(returnId: string, tenantId: string): Promise<ApiResponse<void>> {
    if (import.meta.env.DEV) {
      logger.debug('Calling retryD365Sync API:', {
        path: `${INTEGRATION_BASE_PATH}/reconciliation/${returnId}/retry`,
        returnId,
        tenantId,
      });
    }

    const response = await apiClient.post<ApiResponse<void>>(
      `${INTEGRATION_BASE_PATH}/reconciliation/${returnId}/retry`,
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
