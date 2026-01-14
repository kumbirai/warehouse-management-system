import apiClient from '../../../services/apiClient';
import { ApiResponse } from '../../../types/api';
import {
  HandlePartialOrderAcceptanceRequest,
  HandlePartialOrderAcceptanceResponse,
} from '../types/returns';
import { logger } from '../../../utils/logger';

const RETURNS_BASE_PATH = '/returns';

export const partialOrderAcceptanceService = {
  async handlePartialOrderAcceptance(
    request: HandlePartialOrderAcceptanceRequest,
    tenantId: string
  ): Promise<ApiResponse<HandlePartialOrderAcceptanceResponse>> {
    if (import.meta.env.DEV) {
      logger.debug('Calling handlePartialOrderAcceptance API:', {
        path: `${RETURNS_BASE_PATH}/partial-acceptance`,
        request,
        tenantId,
      });
    }

    const response = await apiClient.post<ApiResponse<HandlePartialOrderAcceptanceResponse>>(
      `${RETURNS_BASE_PATH}/partial-acceptance`,
      request,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );

    if (import.meta.env.DEV) {
      logger.debug('Partial order acceptance response:', {
        status: response.status,
        data: response.data,
      });
    }

    return response.data;
  },
};
