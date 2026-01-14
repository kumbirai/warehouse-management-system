import { useForm, Controller, useFieldArray } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useNavigate } from 'react-router-dom';
import {
  Alert,
  Box,
  Button,
  Card,
  Grid,
  MenuItem,
  Paper,
  TextField,
  Typography,
} from '@mui/material';
import { Delete as DeleteIcon, Add as AddIcon } from '@mui/icons-material';
import { FormPageLayout } from '../../../components/layouts';
import { FormActions, BarcodeInput } from '../../../components/common';
import { useHandlePartialOrderAcceptance } from '../hooks/useHandlePartialOrderAcceptance';
import { ReturnReason } from '../types/returns';
import { Routes, getBreadcrumbs } from '../../../utils/navigationUtils';

const partialOrderLineItemSchema = z.object({
  productId: z.string().min(1, 'Product ID is required'),
  acceptedQuantity: z.number().min(0.01, 'Accepted quantity must be greater than 0'),
  returnReason: z.nativeEnum(ReturnReason),
  lineNotes: z.string().optional(),
});

const partialOrderAcceptanceSchema = z.object({
  orderNumber: z.string().min(1, 'Order number is required'),
  lineItems: z.array(partialOrderLineItemSchema).min(1, 'At least one line item is required'),
  customerSignature: z
    .object({
      signatureData: z.string().min(1, 'Customer signature is required'),
      timestamp: z.string(),
    })
    .optional(),
  returnNotes: z.string().optional(),
});

type PartialOrderAcceptanceFormValues = z.infer<typeof partialOrderAcceptanceSchema>;

export const PartialOrderAcceptancePage = () => {
  const navigate = useNavigate();
  const {
    mutate: handlePartialOrderAcceptance,
    isPending,
    error,
  } = useHandlePartialOrderAcceptance();

  const {
    register,
    control,
    handleSubmit,
    formState: { errors },
    watch,
    setValue,
    setError,
  } = useForm<PartialOrderAcceptanceFormValues>({
    resolver: zodResolver(partialOrderAcceptanceSchema),
    defaultValues: {
      orderNumber: '',
      lineItems: [],
      returnNotes: '',
    },
  });

  const { fields, append, remove } = useFieldArray({
    control,
    name: 'lineItems',
  });

  const customerSignature = watch('customerSignature');

  const handleAddLineItem = () => {
    append({
      productId: '',
      acceptedQuantity: 0,
      returnReason: ReturnReason.OTHER,
      lineNotes: '',
    });
  };

  const handleSignatureCapture = (signatureData: string) => {
    setValue('customerSignature', {
      signatureData,
      timestamp: new Date().toISOString(),
    });
  };

  const handleOrderNumberScan = async (scannedBarcode: string) => {
    setValue('orderNumber', scannedBarcode);
  };

  const onSubmit = (data: PartialOrderAcceptanceFormValues) => {
    if (!data.customerSignature) {
      setError('customerSignature', {
        type: 'manual',
        message: 'Customer signature is required',
      });
      return;
    }

    handlePartialOrderAcceptance(
      {
        orderNumber: data.orderNumber,
        lineItems: data.lineItems,
        customerSignature: data.customerSignature,
        returnNotes: data.returnNotes,
      },
      {
        onSuccess: response => {
          navigate(Routes.returnDetail(response.returnId));
        },
      }
    );
  };

  return (
    <FormPageLayout
      breadcrumbs={getBreadcrumbs.partialOrderAcceptance()}
      title="Partial Order Acceptance"
      description="Record partial order acceptance with customer signature"
      error={error?.message || null}
      maxWidth="lg"
    >
      <form onSubmit={handleSubmit(onSubmit)}>
        <Grid container spacing={3}>
          <Grid item xs={12}>
            <Paper elevation={1} sx={{ p: 3 }}>
              <Typography variant="h6" gutterBottom>
                Order Information
              </Typography>
              <Controller
                name="orderNumber"
                control={control}
                render={({ field }) => (
                  <BarcodeInput
                    {...field}
                    label="Order Number"
                    fullWidth
                    required
                    error={!!errors.orderNumber}
                    helperText={
                      errors.orderNumber?.message || 'Scan barcode first, or enter manually'
                    }
                    onScan={handleOrderNumberScan}
                    autoFocus
                    sx={{ mb: 2 }}
                  />
                )}
              />
            </Paper>
          </Grid>

          <Grid item xs={12}>
            <Paper elevation={1} sx={{ p: 3 }}>
              <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
                <Typography variant="h6">Line Items</Typography>
                <Button
                  variant="outlined"
                  size="small"
                  startIcon={<AddIcon />}
                  onClick={handleAddLineItem}
                >
                  Add Line Item
                </Button>
              </Box>
              {errors.lineItems && (
                <Alert severity="error" sx={{ mb: 2 }}>
                  {errors.lineItems.message}
                </Alert>
              )}
              {fields.length === 0 && (
                <Alert severity="info" sx={{ mb: 2 }}>
                  Click "Add Line Item" to add products to this return
                </Alert>
              )}
              {fields.map((field, index) => (
                <Card key={field.id} sx={{ mb: 2, p: 2 }}>
                  <Grid container spacing={2}>
                    <Grid item xs={12} md={4}>
                      <Controller
                        name={`lineItems.${index}.productId`}
                        control={control}
                        render={({ field }) => (
                          <BarcodeInput
                            {...field}
                            label="Product ID"
                            fullWidth
                            required
                            error={!!errors.lineItems?.[index]?.productId}
                            helperText={
                              errors.lineItems?.[index]?.productId?.message ||
                              'Scan barcode first, or enter manually'
                            }
                            onScan={field.onChange}
                          />
                        )}
                      />
                    </Grid>
                    <Grid item xs={12} md={3}>
                      <TextField
                        {...register(`lineItems.${index}.acceptedQuantity`, {
                          valueAsNumber: true,
                        })}
                        label="Accepted Quantity"
                        type="number"
                        fullWidth
                        required
                        inputProps={{ min: 0, step: 0.01 }}
                        error={!!errors.lineItems?.[index]?.acceptedQuantity}
                        helperText={errors.lineItems?.[index]?.acceptedQuantity?.message}
                      />
                    </Grid>
                    <Grid item xs={12} md={3}>
                      <TextField
                        {...register(`lineItems.${index}.returnReason`)}
                        select
                        label="Return Reason"
                        fullWidth
                        required
                        error={!!errors.lineItems?.[index]?.returnReason}
                        helperText={errors.lineItems?.[index]?.returnReason?.message}
                      >
                        {Object.values(ReturnReason).map(reason => (
                          <MenuItem key={reason} value={reason}>
                            {reason.replace(/_/g, ' ')}
                          </MenuItem>
                        ))}
                      </TextField>
                    </Grid>
                    <Grid item xs={12} md={2}>
                      <Button
                        fullWidth
                        variant="outlined"
                        color="error"
                        startIcon={<DeleteIcon />}
                        onClick={() => remove(index)}
                        disabled={isPending}
                      >
                        Remove
                      </Button>
                    </Grid>
                    <Grid item xs={12}>
                      <TextField
                        {...register(`lineItems.${index}.lineNotes`)}
                        label="Line Notes (Optional)"
                        fullWidth
                        multiline
                        rows={2}
                      />
                    </Grid>
                  </Grid>
                </Card>
              ))}
            </Paper>
          </Grid>

          <Grid item xs={12}>
            <Paper elevation={1} sx={{ p: 3 }}>
              <Typography variant="h6" gutterBottom>
                Customer Signature
              </Typography>
              {errors.customerSignature && (
                <Alert severity="error" sx={{ mb: 2 }}>
                  {errors.customerSignature.message}
                </Alert>
              )}
              <Box
                sx={{
                  border: '2px dashed',
                  borderColor: customerSignature ? 'success.main' : 'divider',
                  borderRadius: 1,
                  p: 3,
                  textAlign: 'center',
                  minHeight: 200,
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'center',
                  alignItems: 'center',
                }}
              >
                {customerSignature ? (
                  <Box>
                    <img
                      src={`data:image/png;base64,${customerSignature.signatureData}`}
                      alt="Customer Signature"
                      style={{ maxWidth: '100%', maxHeight: '200px' }}
                    />
                    <Button
                      size="small"
                      variant="outlined"
                      onClick={() => setValue('customerSignature', undefined)}
                      sx={{ mt: 2 }}
                    >
                      Clear Signature
                    </Button>
                  </Box>
                ) : (
                  <Box>
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                      Customer signature is required for partial order acceptance
                    </Typography>
                    <Button
                      variant="outlined"
                      onClick={() => handleSignatureCapture('')}
                      disabled={isPending}
                    >
                      Capture Signature
                    </Button>
                  </Box>
                )}
              </Box>
            </Paper>
          </Grid>

          <Grid item xs={12}>
            <Paper elevation={1} sx={{ p: 3 }}>
              <TextField
                {...register('returnNotes')}
                label="Return Notes (Optional)"
                fullWidth
                multiline
                rows={4}
                helperText="Additional notes about this return"
              />
            </Paper>
          </Grid>

          <Grid item xs={12}>
            <FormActions
              onCancel={() => navigate(Routes.returns)}
              isSubmitting={isPending}
              submitLabel="Submit Partial Acceptance"
              cancelLabel="Cancel"
            />
          </Grid>
        </Grid>
      </form>
    </FormPageLayout>
  );
};
