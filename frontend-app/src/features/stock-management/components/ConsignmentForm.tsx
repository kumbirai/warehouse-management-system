import { Alert, Box, Button, Grid, IconButton, InputAdornment, Paper, TextField, Typography } from '@mui/material';
import { useState, useEffect } from 'react';
import { z } from 'zod';
import { useFieldArray, useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import {
  Add as AddIcon,
  Delete as DeleteIcon,
  QrCodeScanner as QrCodeIcon,
} from '@mui/icons-material';
import { CreateConsignmentRequest } from '../types/stockManagement';
import { BarcodeScanner } from './BarcodeScanner';
import { useValidateBarcode } from '../hooks/useValidateBarcode';
import { FormActions } from '../../../components/common';
import { WarehouseSelector } from './WarehouseSelector';
import { UserSelector } from './UserSelector';
import { useAppSelector } from '../../../store/hooks';
import { selectUser } from '../../../store/authSlice';

/**
 * Formats a Date object to a local datetime string in the format required by datetime-local input.
 * This ensures the time is displayed in the user's local timezone, not UTC.
 */
const formatLocalDateTime = (date: Date): string => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  return `${year}-${month}-${day}T${hours}:${minutes}`;
};

const consignmentSchema = z.object({
  consignmentReference: z
    .string()
    .min(1, 'Consignment reference is required')
    .max(50, 'Consignment reference cannot exceed 50 characters')
    .regex(
      /^[A-Za-z0-9_-]+$/,
      'Consignment reference must be alphanumeric with hyphens/underscores only'
    ),
  warehouseId: z
    .string()
    .min(1, 'Warehouse ID is required')
    .max(50, 'Warehouse ID cannot exceed 50 characters'),
  receivedAt: z.string().min(1, 'Received date is required'),
  receivedBy: z.string().optional(),
  lineItems: z
    .array(
      z.object({
        productCode: z.string().min(1, 'Product code is required'),
        quantity: z.number().min(1, 'Quantity must be at least 1'),
        expirationDate: z.string().optional(),
        batchNumber: z.string().optional(),
      })
    )
    .min(1, 'At least one line item is required'),
});

export type ConsignmentFormValues = z.infer<typeof consignmentSchema>;

interface ConsignmentFormProps {
  onSubmit: (values: CreateConsignmentRequest) => Promise<void> | void;
  onCancel: () => void;
  isSubmitting?: boolean;
}

export const ConsignmentForm = ({
  onSubmit,
  onCancel,
  isSubmitting = false,
}: ConsignmentFormProps) => {
  const [scannerOpen, setScannerOpen] = useState(false);
  const [scanningLineIndex, setScanningLineIndex] = useState<number | null>(null);
  const [barcodeError, setBarcodeError] = useState<string | null>(null);
  const validateBarcode = useValidateBarcode();
  const currentUser = useAppSelector(selectUser);

  const {
    register,
    handleSubmit,
    control,
    setValue,
    formState: { errors },
  } = useForm<ConsignmentFormValues>({
    resolver: zodResolver(consignmentSchema),
    defaultValues: {
      consignmentReference: '',
      warehouseId: '',
      receivedAt: formatLocalDateTime(new Date()), // Default to now in local time
      receivedBy: currentUser?.userId || '', // Default to logged-in user
      lineItems: [
        {
          productCode: '',
          quantity: 1,
          expirationDate: '',
          batchNumber: '',
        },
      ],
    },
  });

  // Update receivedBy when user context is available
  // Note: UserSelector will only accept the value if it exists in the options,
  // so this will only take effect once the users list has loaded
  useEffect(() => {
    if (currentUser?.userId) {
      setValue('receivedBy', currentUser.userId);
    }
  }, [currentUser?.userId, setValue]);

  const { fields, append, remove } = useFieldArray({
    control,
    name: 'lineItems',
  });

  const handleScan = async (barcode: string) => {
    if (scanningLineIndex === null) {
      return;
    }

    setBarcodeError(null);

    try {
      const result = await validateBarcode.validateBarcode(barcode);
      if (result.valid && result.productInfo) {
        setValue(`lineItems.${scanningLineIndex}.productCode`, result.productInfo.productCode);
        setScannerOpen(false);
        setScanningLineIndex(null);
      } else {
        setBarcodeError(`Barcode "${barcode}" is not valid or product not found`);
      }
    } catch (error) {
      setBarcodeError(error instanceof Error ? error.message : 'Failed to validate barcode');
    }
  };

  const handleOpenScanner = (index: number) => {
    setScanningLineIndex(index);
    setScannerOpen(true);
    setBarcodeError(null);
  };

  const handleCloseScanner = () => {
    setScannerOpen(false);
    setScanningLineIndex(null);
  };

  const onFormSubmit = (values: ConsignmentFormValues) => {
    // Convert datetime-local format (YYYY-MM-DDTHH:mm) to ISO local datetime format (YYYY-MM-DDTHH:mm:ss)
    // The datetime-local input gives us local time without timezone, which is what the backend expects
    // receivedAt is required by form validation, so it will always be present
    const receivedAtLocal = `${values.receivedAt}:00`;
    
    const request: CreateConsignmentRequest = {
      consignmentReference: values.consignmentReference,
      warehouseId: values.warehouseId,
      receivedAt: receivedAtLocal,
      receivedBy: values.receivedBy || undefined,
      lineItems: values.lineItems.map(item => ({
        productCode: item.productCode,
        quantity: item.quantity,
        expirationDate: item.expirationDate || undefined,
        batchNumber: item.batchNumber || undefined,
      })),
    };
    onSubmit(request);
  };

  return (
    <>
      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" gutterBottom>
          Create Stock Consignment
        </Typography>

        <form onSubmit={handleSubmit(onFormSubmit)}>
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Consignment Reference"
                {...register('consignmentReference')}
                error={!!errors.consignmentReference}
                helperText={errors.consignmentReference?.message}
                aria-label="Consignment reference input field"
                aria-required="true"
                aria-describedby="consignment-reference-helper"
                FormHelperTextProps={{ id: 'consignment-reference-helper' }}
                autoFocus
              />
            </Grid>

            <Grid item xs={12} md={6}>
              <Controller
                name="warehouseId"
                control={control}
                render={({ field }) => (
                  <WarehouseSelector
                    value={field.value}
                    onChange={field.onChange}
                    required
                    error={!!errors.warehouseId}
                    helperText={errors.warehouseId?.message}
                    aria-label="Select warehouse"
                    aria-required="true"
                  />
                )}
              />
            </Grid>

            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Received Date & Time"
                type="datetime-local"
                {...register('receivedAt')}
                error={!!errors.receivedAt}
                helperText={errors.receivedAt?.message}
                InputLabelProps={{
                  shrink: true,
                }}
                aria-label="Received date and time input field"
                aria-required="true"
                aria-describedby="received-at-helper"
                FormHelperTextProps={{ id: 'received-at-helper' }}
              />
            </Grid>

            <Grid item xs={12} md={6}>
              <Controller
                name="receivedBy"
                control={control}
                render={({ field }) => (
                  <UserSelector
                    value={field.value}
                    onChange={field.onChange}
                    error={!!errors.receivedBy}
                    helperText={errors.receivedBy?.message}
                  />
                )}
              />
            </Grid>

            <Grid item xs={12}>
              <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
                <Typography variant="subtitle1">Line Items</Typography>
                <Button
                  startIcon={<AddIcon />}
                  onClick={() =>
                    append({
                      productCode: '',
                      quantity: 1,
                      expirationDate: '',
                      batchNumber: '',
                    })
                  }
                  size="small"
                  aria-label="Add line item button"
                >
                  Add Line Item
                </Button>
              </Box>

              {errors.lineItems && (
                <Alert severity="error" sx={{ mb: 2 }}>
                  {errors.lineItems.message}
                </Alert>
              )}

              {fields.map((field, index) => (
                <Paper key={field.id} sx={{ p: 2, mb: 2, position: 'relative', zIndex: 1 }}>
                  <Grid container spacing={2} alignItems="center">
                    <Grid item xs={12} sm={4}>
                      <TextField
                        fullWidth
                        label="Product Code"
                        {...register(`lineItems.${index}.productCode`)}
                        error={!!errors.lineItems?.[index]?.productCode}
                        helperText={errors.lineItems?.[index]?.productCode?.message}
                        InputLabelProps={{
                          shrink: true,
                        }}
                        InputProps={{
                          endAdornment: (
                            <InputAdornment position="end">
                              <IconButton
                                onClick={() => handleOpenScanner(index)}
                                color="primary"
                                title="Scan barcode"
                                edge="end"
                                size="small"
                                aria-label={`Scan barcode for line item ${index + 1}`}
                              >
                                <QrCodeIcon />
                              </IconButton>
                            </InputAdornment>
                          ),
                        }}
                        aria-label={`Product code input field for line item ${index + 1}`}
                        aria-required="true"
                        aria-describedby={`line-item-${index}-product-code-helper`}
                        FormHelperTextProps={{ id: `line-item-${index}-product-code-helper` }}
                      />
                    </Grid>

                    <Grid item xs={12} sm={2}>
                      <TextField
                        fullWidth
                        label="Quantity"
                        type="number"
                        {...register(`lineItems.${index}.quantity`, { valueAsNumber: true })}
                        error={!!errors.lineItems?.[index]?.quantity}
                        helperText={errors.lineItems?.[index]?.quantity?.message}
                        aria-label={`Quantity input field for line item ${index + 1}`}
                        aria-required="true"
                        aria-describedby={`line-item-${index}-quantity-helper`}
                        FormHelperTextProps={{ id: `line-item-${index}-quantity-helper` }}
                      />
                    </Grid>

                    <Grid item xs={12} sm={2}>
                      <TextField
                        fullWidth
                        label="Expiration Date"
                        type="date"
                        {...register(`lineItems.${index}.expirationDate`)}
                        error={!!errors.lineItems?.[index]?.expirationDate}
                        helperText={errors.lineItems?.[index]?.expirationDate?.message}
                        InputLabelProps={{
                          shrink: true,
                        }}
                        aria-label={`Expiration date input field for line item ${index + 1} (optional)`}
                        aria-describedby={`line-item-${index}-expiration-date-helper`}
                        FormHelperTextProps={{ id: `line-item-${index}-expiration-date-helper` }}
                      />
                    </Grid>

                    <Grid item xs={12} sm={3}>
                      <TextField
                        fullWidth
                        label="Batch Number"
                        {...register(`lineItems.${index}.batchNumber`)}
                        error={!!errors.lineItems?.[index]?.batchNumber}
                        helperText={errors.lineItems?.[index]?.batchNumber?.message}
                        aria-label={`Batch number input field for line item ${index + 1} (optional)`}
                        aria-describedby={`line-item-${index}-batch-number-helper`}
                        FormHelperTextProps={{ id: `line-item-${index}-batch-number-helper` }}
                      />
                    </Grid>

                    <Grid item xs={12} sm={1}>
                      <IconButton
                        onClick={() => remove(index)}
                        color="error"
                        disabled={fields.length === 1}
                        title="Remove line item"
                        aria-label={`Remove line item ${index + 1}`}
                      >
                        <DeleteIcon />
                      </IconButton>
                    </Grid>
                  </Grid>
                </Paper>
              ))}
            </Grid>

            {barcodeError && (
              <Grid item xs={12}>
                <Alert severity="error">{barcodeError}</Alert>
              </Grid>
            )}

            <Grid item xs={12}>
              <FormActions
                onCancel={onCancel}
                isSubmitting={isSubmitting}
                submitLabel="Create Consignment"
                cancelLabel="Cancel"
              />
            </Grid>
          </Grid>
        </form>
      </Paper>

      <BarcodeScanner 
        open={scannerOpen} 
        onClose={handleCloseScanner} 
        onScan={handleScan}
      />
    </>
  );
};
