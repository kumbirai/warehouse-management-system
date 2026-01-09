import { Grid, MenuItem, Paper, TextField, Typography } from '@mui/material';
import { z } from 'zod';
import { Controller, useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMemo } from 'react';
import { CreateStockAllocationRequest } from '../types/stockManagement';
import { FormActions } from '../../../components/common';

const stockAllocationSchema = z.object({
  productId: z.string().min(1, 'Product ID is required'),
  locationId: z.string().optional(),
  quantity: z.number().positive('Quantity must be positive').int('Quantity must be an integer'),
  allocationType: z.enum(['PICKING_ORDER', 'RESERVATION', 'OTHER'], {
    required_error: 'Allocation type is required',
  }),
  referenceId: z.string().min(1, 'Reference ID is required'),
  notes: z.string().max(500, 'Notes cannot exceed 500 characters').optional(),
});

export type StockAllocationFormValues = z.infer<typeof stockAllocationSchema>;

interface StockAllocationFormProps {
  defaultValues?: Partial<StockAllocationFormValues>;
  onSubmit: (values: CreateStockAllocationRequest) => Promise<void> | void;
  onCancel: () => void;
  isSubmitting?: boolean;
}

export const StockAllocationForm = ({
  defaultValues,
  onSubmit,
  onCancel,
  isSubmitting = false,
}: StockAllocationFormProps) => {
  const formDefaultValues = useMemo(
    () => ({
      productId: defaultValues?.productId || '',
      locationId: defaultValues?.locationId || '',
      quantity: defaultValues?.quantity || 1,
      allocationType: defaultValues?.allocationType || 'PICKING_ORDER',
      referenceId: defaultValues?.referenceId || '',
      notes: defaultValues?.notes || '',
    }),
    [defaultValues]
  );

  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<StockAllocationFormValues>({
    resolver: zodResolver(stockAllocationSchema),
    defaultValues: formDefaultValues,
  });

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h6" gutterBottom>
        Allocate Stock
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
              name="allocationType"
              control={control}
              render={({ field }) => (
                <TextField
                  {...field}
                  select
                  label="Allocation Type"
                  fullWidth
                  required
                  error={!!errors.allocationType}
                  helperText={errors.allocationType?.message}
                  aria-label="Select allocation type"
                  aria-required="true"
                  aria-describedby="allocation-type-helper"
                  FormHelperTextProps={{ id: 'allocation-type-helper' }}
                >
                  <MenuItem value="PICKING_ORDER">Picking Order</MenuItem>
                  <MenuItem value="RESERVATION">Reservation</MenuItem>
                  <MenuItem value="OTHER">Other</MenuItem>
                </TextField>
              )}
            />
          </Grid>
          <Grid item xs={12}>
            <TextField
              {...register('referenceId')}
              label="Reference ID (e.g., Order ID)"
              fullWidth
              required
              error={!!errors.referenceId}
              helperText={errors.referenceId?.message}
              aria-label="Reference ID input field"
              aria-required="true"
              aria-describedby="reference-id-helper"
              FormHelperTextProps={{ id: 'reference-id-helper' }}
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
        </Grid>

        <FormActions
          onCancel={onCancel}
          isSubmitting={isSubmitting}
          submitLabel="Allocate Stock"
          cancelLabel="Cancel"
        />
      </form>
    </Paper>
  );
};
