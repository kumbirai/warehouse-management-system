import { Grid, MenuItem, Paper, TextField, Typography } from '@mui/material';
import { z } from 'zod';
import { Controller, useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMemo } from 'react';
import { CreateStockAdjustmentRequest } from '../types/stockManagement';
import { FormActions } from '../../../components/common';

const stockAdjustmentSchema = z.object({
  productId: z.string().min(1, 'Product ID is required'),
  locationId: z.string().optional(),
  stockItemId: z.string().optional(),
  adjustmentType: z.enum(['INCREASE', 'DECREASE'], {
    required_error: 'Adjustment type is required',
  }),
  quantity: z.number().positive('Quantity must be positive').int('Quantity must be an integer'),
  reason: z.enum(['STOCK_COUNT', 'DAMAGE', 'CORRECTION', 'THEFT', 'EXPIRATION', 'OTHER'], {
    required_error: 'Reason is required',
  }),
  notes: z.string().max(500, 'Notes cannot exceed 500 characters').optional(),
  authorizationCode: z.string().optional(),
});

export type StockAdjustmentFormValues = z.infer<typeof stockAdjustmentSchema>;

interface StockAdjustmentFormProps {
  defaultValues?: Partial<StockAdjustmentFormValues>;
  onSubmit: (values: CreateStockAdjustmentRequest) => Promise<void> | void;
  onCancel: () => void;
  isSubmitting?: boolean;
}

export const StockAdjustmentForm = ({
  defaultValues,
  onSubmit,
  onCancel,
  isSubmitting = false,
}: StockAdjustmentFormProps) => {
  const formDefaultValues = useMemo(
    () => ({
      productId: defaultValues?.productId || '',
      locationId: defaultValues?.locationId || '',
      stockItemId: defaultValues?.stockItemId || '',
      adjustmentType: defaultValues?.adjustmentType || 'INCREASE',
      quantity: defaultValues?.quantity || 1,
      reason: defaultValues?.reason || 'STOCK_COUNT',
      notes: defaultValues?.notes || '',
      authorizationCode: defaultValues?.authorizationCode || '',
    }),
    [defaultValues]
  );

  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<StockAdjustmentFormValues>({
    resolver: zodResolver(stockAdjustmentSchema),
    defaultValues: formDefaultValues,
  });

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h6" gutterBottom>
        Adjust Stock Level
      </Typography>
      <form onSubmit={handleSubmit(onSubmit)}>
        <Grid container spacing={2}>
          <Grid item xs={12} sm={6}>
            <TextField
              {...register('productId')}
              label="Product ID"
              fullWidth
              required
              error={!!errors.productId}
              helperText={errors.productId?.message}
              aria-label="Product ID input field"
              aria-required="true"
              aria-describedby="product-id-helper"
              FormHelperTextProps={{ id: 'product-id-helper' }}
              autoFocus
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              {...register('locationId')}
              label="Location ID (Optional)"
              fullWidth
              error={!!errors.locationId}
              helperText={errors.locationId?.message}
              aria-label="Location ID input field (optional)"
              aria-describedby="location-id-helper"
              FormHelperTextProps={{ id: 'location-id-helper' }}
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              {...register('stockItemId')}
              label="Stock Item ID (Optional)"
              fullWidth
              error={!!errors.stockItemId}
              helperText={errors.stockItemId?.message}
              aria-label="Stock item ID input field (optional)"
              aria-describedby="stock-item-id-helper"
              FormHelperTextProps={{ id: 'stock-item-id-helper' }}
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <Controller
              name="adjustmentType"
              control={control}
              render={({ field }) => (
                <TextField
                  {...field}
                  select
                  label="Adjustment Type"
                  fullWidth
                  required
                  error={!!errors.adjustmentType}
                  helperText={errors.adjustmentType?.message}
                  aria-label="Select adjustment type"
                  aria-required="true"
                  aria-describedby="adjustment-type-helper"
                  FormHelperTextProps={{ id: 'adjustment-type-helper' }}
                >
                  <MenuItem value="INCREASE">Increase</MenuItem>
                  <MenuItem value="DECREASE">Decrease</MenuItem>
                </TextField>
              )}
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
              aria-label="Quantity input field"
              aria-required="true"
              aria-describedby="quantity-helper"
              FormHelperTextProps={{ id: 'quantity-helper' }}
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
                  aria-label="Select adjustment reason"
                  aria-required="true"
                  aria-describedby="reason-helper"
                  FormHelperTextProps={{ id: 'reason-helper' }}
                >
                  <MenuItem value="STOCK_COUNT">Stock Count</MenuItem>
                  <MenuItem value="DAMAGE">Damage</MenuItem>
                  <MenuItem value="CORRECTION">Correction</MenuItem>
                  <MenuItem value="THEFT">Theft</MenuItem>
                  <MenuItem value="EXPIRATION">Expiration</MenuItem>
                  <MenuItem value="OTHER">Other</MenuItem>
                </TextField>
              )}
            />
          </Grid>
          <Grid item xs={12}>
            <TextField
              {...register('notes')}
              label="Notes (Optional)"
              fullWidth
              multiline
              rows={3}
              error={!!errors.notes}
              helperText={errors.notes?.message}
              aria-label="Notes input field (optional)"
              aria-describedby="notes-helper"
              FormHelperTextProps={{ id: 'notes-helper' }}
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              {...register('authorizationCode')}
              label="Authorization Code (Optional)"
              fullWidth
              error={!!errors.authorizationCode}
              helperText={errors.authorizationCode?.message}
              aria-label="Authorization code input field (optional)"
              aria-describedby="authorization-code-helper"
              FormHelperTextProps={{ id: 'authorization-code-helper' }}
            />
          </Grid>
        </Grid>

        <FormActions
          onCancel={onCancel}
          isSubmitting={isSubmitting}
          submitLabel="Adjust Stock"
          cancelLabel="Cancel"
        />
      </form>
    </Paper>
  );
};

