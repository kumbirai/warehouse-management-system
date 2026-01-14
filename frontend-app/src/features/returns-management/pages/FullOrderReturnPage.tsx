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
import { useProcessFullOrderReturn } from '../hooks/useProcessFullOrderReturn';
import { ReturnReason, ProductCondition } from '../types/returns';
import { Routes, getBreadcrumbs } from '../../../utils/navigationUtils';

const fullReturnLineItemSchema = z.object({
  productId: z.string().min(1, 'Product ID is required'),
  productCondition: z.nativeEnum(ProductCondition),
  returnReason: z.nativeEnum(ReturnReason),
  lineNotes: z.string().optional(),
});

const fullOrderReturnSchema = z.object({
  returnId: z.string().min(1, 'Return ID is required'),
  primaryReturnReason: z.nativeEnum(ReturnReason),
  lineItems: z.array(fullReturnLineItemSchema).min(1, 'At least one line item is required'),
  returnNotes: z.string().optional(),
});

type FullOrderReturnFormValues = z.infer<typeof fullOrderReturnSchema>;

export const FullOrderReturnPage = () => {
  const navigate = useNavigate();
  const { mutate: processFullOrderReturn, isPending, error } = useProcessFullOrderReturn();

  const {
    register,
    control,
    handleSubmit,
    formState: { errors },
    setValue,
  } = useForm<FullOrderReturnFormValues>({
    resolver: zodResolver(fullOrderReturnSchema),
    defaultValues: {
      returnId: '',
      primaryReturnReason: ReturnReason.OTHER,
      lineItems: [],
      returnNotes: '',
    },
  });

  const { fields, append, remove } = useFieldArray({
    control,
    name: 'lineItems',
  });

  const handleAddLineItem = () => {
    append({
      productId: '',
      productCondition: ProductCondition.GOOD,
      returnReason: ReturnReason.OTHER,
      lineNotes: '',
    });
  };

  const handleReturnIdScan = async (scannedBarcode: string) => {
    setValue('returnId', scannedBarcode);
  };

  const onSubmit = (data: FullOrderReturnFormValues) => {
    processFullOrderReturn(
      {
        returnId: data.returnId,
        primaryReturnReason: data.primaryReturnReason,
        lineItems: data.lineItems,
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
      breadcrumbs={getBreadcrumbs.fullOrderReturn()}
      title="Full Order Return"
      description="Process a full order return with product condition assessment"
      error={error?.message || null}
      maxWidth="lg"
    >
      <form onSubmit={handleSubmit(onSubmit)}>
        <Grid container spacing={3}>
          <Grid item xs={12}>
            <Paper elevation={1} sx={{ p: 3 }}>
              <Typography variant="h6" gutterBottom>
                Return Information
              </Typography>
              <Controller
                name="returnId"
                control={control}
                render={({ field }) => (
                  <BarcodeInput
                    {...field}
                    label="Return ID"
                    fullWidth
                    required
                    error={!!errors.returnId}
                    helperText={errors.returnId?.message || 'Scan barcode first, or enter manually'}
                    onScan={handleReturnIdScan}
                    autoFocus
                    sx={{ mb: 2 }}
                  />
                )}
              />
              <TextField
                {...register('primaryReturnReason')}
                select
                label="Primary Return Reason"
                fullWidth
                required
                error={!!errors.primaryReturnReason}
                helperText={errors.primaryReturnReason?.message}
              >
                {Object.values(ReturnReason).map(reason => (
                  <MenuItem key={reason} value={reason}>
                    {reason.replace(/_/g, ' ')}
                  </MenuItem>
                ))}
              </TextField>
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
                        {...register(`lineItems.${index}.productCondition`)}
                        select
                        label="Product Condition"
                        fullWidth
                        required
                        error={!!errors.lineItems?.[index]?.productCondition}
                        helperText={errors.lineItems?.[index]?.productCondition?.message}
                      >
                        {Object.values(ProductCondition).map(condition => (
                          <MenuItem key={condition} value={condition}>
                            {condition.replace(/_/g, ' ')}
                          </MenuItem>
                        ))}
                      </TextField>
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
              submitLabel="Process Full Return"
              cancelLabel="Cancel"
            />
          </Grid>
        </Grid>
      </form>
    </FormPageLayout>
  );
};
