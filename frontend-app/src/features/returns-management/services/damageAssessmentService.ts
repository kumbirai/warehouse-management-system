import apiClient from '../../../services/apiClient';
import { ApiResponse } from '../../../types/api';
import {
  DamageAssessment,
  RecordDamageAssessmentRequest,
  RecordDamageAssessmentResponse,
} from '../types/damageAssessment';
import { logger } from '../../../utils/logger';

const RETURNS_BASE_PATH = '/returns';

export const damageAssessmentService = {
  async recordDamageAssessment(
    request: RecordDamageAssessmentRequest,
    tenantId: string
  ): Promise<ApiResponse<RecordDamageAssessmentResponse>> {
    if (import.meta.env.DEV) {
      logger.debug('Calling recordDamageAssessment API:', {
        path: `${RETURNS_BASE_PATH}/damage-assessment`,
        request: {
          ...request,
          // Don't log photo data in full
          damagedProductItems: request.damagedProductItems.map(item => ({
            ...item,
            photoUrl: item.photoUrl ? '[REDACTED]' : undefined,
          })),
        },
        tenantId,
      });
    }

    const response = await apiClient.post<ApiResponse<RecordDamageAssessmentResponse>>(
      `${RETURNS_BASE_PATH}/damage-assessment`,
      request,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );

    if (import.meta.env.DEV) {
      logger.debug('Damage assessment response:', {
        status: response.status,
        data: response.data,
      });
    }

    return response.data;
  },

  async getDamageAssessment(
    assessmentId: string,
    tenantId: string
  ): Promise<ApiResponse<DamageAssessment>> {
    const response = await apiClient.get<ApiResponse<DamageAssessment>>(
      `${RETURNS_BASE_PATH}/damage-assessments/${assessmentId}`,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );

    return response.data;
  },
};
