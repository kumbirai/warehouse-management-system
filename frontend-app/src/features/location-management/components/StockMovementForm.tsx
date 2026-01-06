import { Grid, MenuItem, Paper, TextField, Typography } from '@mui/material';
import { z } from 'zod';
import { Controller, useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMemo } from 'react';
import { CreateStockMovementRequest } from '../services/stockMovementService';
import { FormActions } from '../../../components/common';

const stockMovementSchema = z.object({
  stockItemId: z.string().min(1, 'Stock Item ID is required'),
  productId: z.string().min(1, 'Product ID is required'),
  sourceLocationId: z.string().min(1, 'Source location is required'),
  destinationLocationId: z.string().min(1, 'Destination location is required'),
  quantity: z.number().positive('Quantity must be positive').int('Quantity must be an integer'),
  movementType: z.enum(['RECEIVING_TO_STORAGE', 'STORAGE_TO_PICKING', 'INTER_STORAGE', 'PICKING_TO_SHIPPING', 'OTHER'], {
    required_error: 'Movement type is required',
  }),
  reason: z.enum(['PICKING', 'RESTOCKING', 'REORGANIZATION', 'DAMAGE', 'CORRECTION', 'OTHER'], {
    required_error: 'Reason is required',
  }),
});

export type StockMovementFormValues = z.infer<typeof stockMovementSchema>;

interface StockMovementFormProps {
  defaultValues?: Partial<StockMovementFormValues>;
  onSubmit: (values: CreateStockMovementRequest) => Promise<void> | void;
  onCancel: () => void;
  isSubmitting?: boolean;
}

export const StockMovementForm = ({
  defaultValues,
  onSubmit,
  onCancel,
  isSubmitting = false,
}: StockMovementFormProps) => {
  const formDefaultValues = useMemo(
    () => ({
      stockItemId: defaultValues?.stockItemId || '',
      productId: defaultValues?.productId || '',
      sourceLocationId: defaultValues?.sourceLocationId || '',
      destinationLocationId: defaultValues?.destinationLocationId || '',
      quantity: defaultValues?.quantity || 1,
      movementType: defaultValues?.movementType || 'INTER_STORAGE',
      reason: defaultValues?.reason || 'REORGANIZATION',
    }),
    [defaultValues]
  );

  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<StockMovementFormValues>({
    resolver: zodResolver(stockMovementSchema),
    defaultValues: formDefaultValues,
  });

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h6" gutterBottom>
        Create Stock Movement
      </Typography>
      <form onSubmit={handleSubmit(onSubmit)}>
        <Grid container spacing={2}>
          <Grid item xs={12} sm={6}>
            <TextField
              {...register('stockItemId')}
              label="Stock Item ID"
              fullWidth
              required
              error={!!errors.stockItemId}
              helperText={errors.stockItemId?.message}
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              {...register('productId')}
              label="Product ID"
              fullWidth
              required
              error={!!errors.productId}
              helperText={errors.productId?.message}
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              {...register('sourceLocationId')}
              label="Source Location ID"
              fullWidth
              required
              error={!!errors.sourceLocationId}
              helperText={errors.sourceLocationId?.message}
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              {...register('destinationLocationId')}
              label="Destination Location ID"
              fullWidth
              required
              error={!!errors.destinationLocationId}
              helperText={errors.destinationLocationId?.message}
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              {...register('quantity', { valueAsNumber: true })}
              label="Quantity"
              type="number"
              fullWidth
              required
              error={!!errors.quantity}
              helperText={errors.quantity?.message}
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <Controller
              name="movementType"
              control={control}
              render={({ field }) => (
                <TextField
                  {...field}
                  select
                  label="Movement Type"
                  fullWidth
                  required
                  error={!!errors.movementType}
                  helperText={errors.movementType?.message}
                >
                  <MenuItem value="RECEIVING_TO_STORAGE">Receiving to Storage</MenuItem>
                  <MenuItem value="STORAGE_TO_PICKING">Storage to Picking</MenuItem>
                  <MenuItem value="INTER_STORAGE">Inter Storage</MenuItem>
                  <MenuItem value="PICKING_TO_SHIPPING">Picking to Shipping</MenuItem>
                  <MenuItem value="OTHER">Other</MenuItem>
                </TextField>
              )}
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <Controller
              name="reason"
              control={control}
              render={({ field }) => (
                <TextField
                  {...field}
                  select
                  label="Reason"
                  fullWidth
                  required
                  error={!!errors.reason}
                  helperText={errors.reason?.message}
                >
                  <MenuItem value="PICKING">Picking</MenuItem>
                  <MenuItem value="RESTOCKING">Restocking</MenuItem>
                  <MenuItem value="REORGANIZATION">Reorganization</MenuItem>
                  <MenuItem value="DAMAGE">Damage</MenuItem>
                  <MenuItem value="CORRECTION">Correction</MenuItem>
                  <MenuItem value="OTHER">Other</MenuItem>
                </TextField>
              )}
            />
          </Grid>
        </Grid>

        <FormActions
          onCancel={onCancel}
          isSubmitting={isSubmitting}
          submitLabel="Create Movement"
          cancelLabel="Cancel"
        />
      </form>
    </Paper>
  );
};

