import { useMutation, useQueryClient } from '@tanstack/react-query';
import { damageAssessmentService } from '../services/damageAssessmentService';
import { RecordDamageAssessmentRequest } from '../types/damageAssessment';
import { useAuth } from '../../../hooks/useAuth';
import { useToast } from '../../../hooks/useToast';

export const useRecordDamageAssessment = () => {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const { success, error: showError } = useToast();

  return useMutation({
    mutationFn: async (request: RecordDamageAssessmentRequest) => {
      if (!user?.tenantId) {
        throw new Error('Tenant ID is required');
      }
      const response = await damageAssessmentService.recordDamageAssessment(request, user.tenantId);
      if (response.error) {
        throw new Error(response.error.message);
      }
      return response.data;
    },
    onSuccess: data => {
      queryClient.invalidateQueries({ queryKey: ['damage-assessments'] });
      queryClient.invalidateQueries({ queryKey: ['returns'] });
      success(`Damage assessment recorded successfully. ID: ${data.assessmentId}`);
    },
    onError: error => {
      const errorMessage =
        error instanceof Error ? error.message : 'Failed to record damage assessment';
      showError(errorMessage);
    },
  });
};
