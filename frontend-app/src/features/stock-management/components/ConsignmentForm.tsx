import { Alert, Box, Button, Grid, IconButton, Paper, TextField, Typography } from '@mui/material';
import { useState } from 'react';
import { z } from 'zod';
import { useFieldArray, useForm } from 'react-hook-form';
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
      receivedAt: new Date().toISOString().slice(0, 16), // Default to now
      receivedBy: '',
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
    const request: CreateConsignmentRequest = {
      consignmentReference: values.consignmentReference,
      warehouseId: values.warehouseId,
      receivedAt: new Date(values.receivedAt).toISOString(),
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
              />
            </Grid>

            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Warehouse ID"
                {...register('warehouseId')}
                error={!!errors.warehouseId}
                helperText={errors.warehouseId?.message}
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
              />
            </Grid>

            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Received By"
                {...register('receivedBy')}
                error={!!errors.receivedBy}
                helperText={errors.receivedBy?.message}
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
                <Paper key={field.id} sx={{ p: 2, mb: 2 }}>
                  <Grid container spacing={2} alignItems="center">
                    <Grid item xs={12} sm={4}>
                      <Box display="flex" gap={1}>
                        <TextField
                          fullWidth
                          label="Product Code"
                          {...register(`lineItems.${index}.productCode`)}
                          error={!!errors.lineItems?.[index]?.productCode}
                          helperText={errors.lineItems?.[index]?.productCode?.message}
                        />
                        <IconButton
                          onClick={() => handleOpenScanner(index)}
                          color="primary"
                          title="Scan barcode"
                        >
                          <QrCodeIcon />
                        </IconButton>
                      </Box>
                    </Grid>

                    <Grid item xs={12} sm={2}>
                      <TextField
                        fullWidth
                        label="Quantity"
                        type="number"
                        {...register(`lineItems.${index}.quantity`, { valueAsNumber: true })}
                        error={!!errors.lineItems?.[index]?.quantity}
                        helperText={errors.lineItems?.[index]?.quantity?.message}
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
                      />
                    </Grid>

                    <Grid item xs={12} sm={3}>
                      <TextField
                        fullWidth
                        label="Batch Number"
                        {...register(`lineItems.${index}.batchNumber`)}
                        error={!!errors.lineItems?.[index]?.batchNumber}
                        helperText={errors.lineItems?.[index]?.batchNumber?.message}
                      />
                    </Grid>

                    <Grid item xs={12} sm={1}>
                      <IconButton
                        onClick={() => remove(index)}
                        color="error"
                        disabled={fields.length === 1}
                        title="Remove line item"
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

      <BarcodeScanner open={scannerOpen} onClose={handleCloseScanner} onScan={handleScan} />
    </>
  );
};
