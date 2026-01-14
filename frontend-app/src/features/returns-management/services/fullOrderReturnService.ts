import apiClient from '../../../services/apiClient';
import { ApiResponse } from '../../../types/api';
import { ProcessFullOrderReturnRequest, ProcessFullOrderReturnResponse } from '../types/returns';
import { logger } from '../../../utils/logger';

const RETURNS_BASE_PATH = '/returns';

export const fullOrderReturnService = {
  async processFullOrderReturn(
    request: ProcessFullOrderReturnRequest,
    tenantId: string
  ): Promise<ApiResponse<ProcessFullOrderReturnResponse>> {
    if (import.meta.env.DEV) {
      logger.debug('Calling processFullOrderReturn API:', {
        path: `${RETURNS_BASE_PATH}/full-return`,
        request,
        tenantId,
      });
    }

    const response = await apiClient.post<ApiResponse<ProcessFullOrderReturnResponse>>(
      `${RETURNS_BASE_PATH}/full-return`,
      request,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );

    if (import.meta.env.DEV) {
      logger.debug('Full order return response:', {
        status: response.status,
        data: response.data,
      });
    }

    return response.data;
  },
};
