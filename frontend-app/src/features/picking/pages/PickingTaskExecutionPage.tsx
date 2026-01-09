import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Alert,
  Box,
  Checkbox,
  Chip,
  Divider,
  FormControlLabel,
  Grid,
  Paper,
  TextField,
  Typography,
} from '@mui/material';
import { z } from 'zod';
import { Controller, useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { DetailPageLayout } from '../../../components/layouts';
import { getBreadcrumbs, Routes } from '../../../utils/navigationUtils';
import { usePickingTasks } from '../hooks/usePickingTasks';
import { useExecutePickingTask } from '../hooks/useExecutePickingTask';
import { useCheckStockExpiration } from '../../stock-management/hooks/useCheckStockExpiration';
import { ExpiredStockIndicator } from '../components/ExpiredStockIndicator';
import { StockExpirationCheckResponse } from '../../stock-management/types/stockManagement';
import { FormActions } from '../../../components/common';
import { logger } from '../../../utils/logger';

const executeTaskSchema = z
  .object({
    pickedQuantity: z.number().min(1, 'Picked quantity must be at least 1'),
    isPartialPicking: z.boolean().optional(),
    partialReason: z.string().optional(),
  })
  .refine(
    data => {
      if (data.isPartialPicking && !data.partialReason?.trim()) {
        return false;
      }
      return true;
    },
    {
      message: 'Partial reason is required when marking as partial picking',
      path: ['partialReason'],
    }
  );

type ExecuteTaskFormValues = z.infer<typeof executeTaskSchema>;

export const PickingTaskExecutionPage = () => {
  const { taskId } = useParams<{ taskId: string }>();
  const navigate = useNavigate();
  const { pickingTasks, isLoading: isLoadingTasks, refetch: refetchTasks } = usePickingTasks({});
  const { executeTask, isLoading: isExecuting, error: executeError } = useExecutePickingTask();
  const { checkExpiration } = useCheckStockExpiration();

  const [expirationStatus, setExpirationStatus] = useState<StockExpirationCheckResponse | null>(
    null
  );

  const task = pickingTasks?.pickingTasks.find(t => t.taskId === taskId);

  const {
    control,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<ExecuteTaskFormValues>({
    resolver: zodResolver(executeTaskSchema),
    defaultValues: {
      pickedQuantity: task?.quantity || 0,
      isPartialPicking: false,
      partialReason: '',
    },
  });

  const isPartialPicking = watch('isPartialPicking');
  const pickedQuantity = watch('pickedQuantity');

  // Check expiration when task or quantity changes
  useEffect(() => {
    if (task?.productCode && task?.locationId && pickedQuantity > 0) {
      checkExpiration(task.productCode, task.locationId)
        .then(result => {
          if (result) {
            setExpirationStatus(result);
          }
        })
        .catch(err => {
          // Log error but don't block execution - expiration check is a warning
          logger.warn('Failed to check stock expiration', {
            error: err instanceof Error ? err.message : String(err),
            taskId: task.taskId,
            productCode: task.productCode,
            locationId: task.locationId,
          });
        });
    } else {
      // Reset expiration status when task or quantity changes to zero
      setExpirationStatus(null);
    }
  }, [task, pickedQuantity, checkExpiration]);

  const onSubmit = async (values: ExecuteTaskFormValues) => {
    if (!taskId) {
      return;
    }

    const result = await executeTask(taskId, {
      pickedQuantity: values.pickedQuantity,
      isPartialPicking: values.isPartialPicking,
      partialReason: values.partialReason,
    });

    if (result) {
      // Refresh tasks and navigate back
      refetchTasks();
      navigate(Routes.pickingLists);
    }
  };

  const handleCancel = () => {
    navigate(Routes.pickingLists);
  };

  if (isLoadingTasks) {
    return (
      <DetailPageLayout
        breadcrumbs={getBreadcrumbs.pickingTaskExecute(taskId || '')}
        title="Execute Picking Task"
        isLoading={true}
        error={null}
      >
        <div />
      </DetailPageLayout>
    );
  }

  if (!task) {
    return (
      <DetailPageLayout
        breadcrumbs={getBreadcrumbs.pickingTaskExecute(taskId || '')}
        title="Execute Picking Task"
        isLoading={false}
        error="Picking task not found"
      >
        <div />
      </DetailPageLayout>
    );
  }

  const isExpired = expirationStatus?.expired || expirationStatus?.classification === 'EXPIRED';
  const canExecute = !isExpired && task.status === 'PENDING';

  return (
    <DetailPageLayout
      breadcrumbs={getBreadcrumbs.pickingTaskExecute(taskId || '')}
      title={`Execute Picking Task: ${task.productCode}`}
      isLoading={false}
      error={executeError?.message || null}
    >
      <Grid container spacing={3}>
        {/* Task Information */}
        <Grid item xs={12} md={6}>
          <Paper elevation={1} sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Task Information
            </Typography>
            <Divider sx={{ mb: 2 }} />
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Product Code
                </Typography>
                <Typography variant="body1" sx={{ fontFamily: 'monospace', fontWeight: 'medium' }}>
                  {task.productCode}
                </Typography>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Location ID
                </Typography>
                <Typography variant="body1" sx={{ fontFamily: 'monospace' }}>
                  {task.locationId}
                </Typography>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Required Quantity
                </Typography>
                <Typography variant="body1">{task.quantity}</Typography>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Status
                </Typography>
                <Box mt={0.5}>
                  <Chip
                    label={task.status}
                    color={task.status === 'COMPLETED' ? 'success' : 'default'}
                    size="small"
                  />
                </Box>
              </Box>
            </Box>
          </Paper>
        </Grid>

        {/* Expiration Status */}
        {expirationStatus && (
          <Grid item xs={12}>
            <ExpiredStockIndicator
              classification={expirationStatus.classification}
              message={expirationStatus.message}
            />
          </Grid>
        )}

        {/* Execution Form */}
        <Grid item xs={12}>
          <Paper elevation={1} sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Execute Picking Task
            </Typography>
            <Divider sx={{ mb: 3 }} />

            {!canExecute && (
              <Alert severity="error" sx={{ mb: 2 }}>
                {isExpired
                  ? 'This stock has expired and cannot be picked.'
                  : `Task is in ${task.status} status and cannot be executed.`}
              </Alert>
            )}

            <form onSubmit={handleSubmit(onSubmit)}>
              <Grid container spacing={2}>
                <Grid item xs={12} md={6}>
                  <Controller
                    name="pickedQuantity"
                    control={control}
                    render={({ field }) => (
                      <TextField
                        {...field}
                        label="Picked Quantity"
                        type="number"
                        fullWidth
                        required
                        disabled={!canExecute || isExecuting}
                        error={!!errors.pickedQuantity}
                        helperText={errors.pickedQuantity?.message}
                        inputProps={{ min: 1, max: task.quantity }}
                        onChange={e => field.onChange(parseInt(e.target.value) || 0)}
                      />
                    )}
                  />
                </Grid>

                <Grid item xs={12}>
                  <Controller
                    name="isPartialPicking"
                    control={control}
                    render={({ field }) => (
                      <FormControlLabel
                        control={
                          <Checkbox
                            checked={field.value || false}
                            onChange={field.onChange}
                            disabled={!canExecute || isExecuting}
                          />
                        }
                        label="This is a partial picking (picked quantity is less than required)"
                      />
                    )}
                  />
                </Grid>

                {isPartialPicking && (
                  <Grid item xs={12}>
                    <Controller
                      name="partialReason"
                      control={control}
                      render={({ field }) => (
                        <TextField
                          {...field}
                          label="Partial Picking Reason"
                          fullWidth
                          required
                          multiline
                          rows={3}
                          disabled={!canExecute || isExecuting}
                          error={!!errors.partialReason}
                          helperText={
                            errors.partialReason?.message ||
                            'Required when marking as partial picking'
                          }
                        />
                      )}
                    />
                  </Grid>
                )}
              </Grid>

              <FormActions
                onCancel={handleCancel}
                onSubmit={handleSubmit(onSubmit)}
                isSubmitting={isExecuting}
                submitLabel="Execute Task"
                cancelLabel="Cancel"
                submitDisabled={!canExecute}
              />
            </form>
          </Paper>
        </Grid>
      </Grid>
    </DetailPageLayout>
  );
};
