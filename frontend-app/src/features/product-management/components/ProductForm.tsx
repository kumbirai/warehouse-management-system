import {
  Box,
  Button,
  FormControl,
  Grid,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  TextField,
  Typography,
} from '@mui/material';
import { z } from 'zod';
import { Controller, useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { CreateProductRequest, UpdateProductRequest } from '../types/product';
import { useCheckProductCodeUniqueness } from '../hooks/useCheckProductCodeUniqueness';
import { useAuth } from '../../../hooks/useAuth';
import { useEffect, useState } from 'react';
import { useDebounce } from '../../../hooks/useDebounce';
import { BarcodeInput, FormActions } from '../../../components/common';

const productSchema = z.object({
  productCode: z
    .string()
    .min(1, 'Product code is required')
    .max(50, 'Product code cannot exceed 50 characters')
    .regex(/^[A-Za-z0-9_-]+$/, 'Product code must be alphanumeric with hyphens/underscores only'),
  description: z
    .string()
    .min(1, 'Description is required')
    .max(500, 'Description cannot exceed 500 characters'),
  primaryBarcode: z.string().min(1, 'Primary barcode is required'),
  unitOfMeasure: z.enum(['EA', 'CS', 'PK', 'BOX', 'PAL']),
  secondaryBarcodes: z.array(z.string()).optional(),
  category: z.string().max(100, 'Category cannot exceed 100 characters').optional(),
  brand: z.string().max(100, 'Brand cannot exceed 100 characters').optional(),
});

export type ProductFormValues = z.infer<typeof productSchema>;

interface ProductFormProps {
  defaultValues?: Partial<ProductFormValues>;
  onSubmit: (values: CreateProductRequest | UpdateProductRequest) => Promise<void> | void;
  onCancel: () => void;
  isSubmitting?: boolean;
  isUpdate?: boolean;
}

export const ProductForm = ({
  defaultValues,
  onSubmit,
  onCancel,
  isSubmitting = false,
  isUpdate = false,
}: ProductFormProps) => {
  const { user } = useAuth();
  const { checkUniqueness } = useCheckProductCodeUniqueness();
  const [productCodeError, setProductCodeError] = useState<string | null>(null);
  const [isCheckingUniqueness, setIsCheckingUniqueness] = useState(false);

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    control,
    formState: { errors },
  } = useForm<ProductFormValues>({
    resolver: zodResolver(productSchema),
    defaultValues: {
      productCode: defaultValues?.productCode || '',
      description: defaultValues?.description || '',
      primaryBarcode: defaultValues?.primaryBarcode || '',
      unitOfMeasure: defaultValues?.unitOfMeasure || 'EA',
      secondaryBarcodes: defaultValues?.secondaryBarcodes || [],
      category: defaultValues?.category || '',
      brand: defaultValues?.brand || '',
    },
  });

  const productCode = watch('productCode');
  const debouncedProductCode = useDebounce(productCode, 500);

  // Check product code uniqueness (only for create, not update)
  useEffect(() => {
    if (!isUpdate && debouncedProductCode && user?.tenantId && debouncedProductCode.length > 0) {
      setIsCheckingUniqueness(true);
      checkUniqueness(debouncedProductCode, user.tenantId)
        .then(isUnique => {
          if (!isUnique) {
            setProductCodeError('Product code already exists');
          } else {
            setProductCodeError(null);
          }
        })
        .catch(() => {
          setProductCodeError('Failed to check product code uniqueness');
        })
        .finally(() => {
          setIsCheckingUniqueness(false);
        });
    }
  }, [debouncedProductCode, isUpdate, user?.tenantId, checkUniqueness]);

  const secondaryBarcodes = watch('secondaryBarcodes') || [];
  const [newSecondaryBarcode, setNewSecondaryBarcode] = useState('');

  const handleAddSecondaryBarcode = () => {
    if (newSecondaryBarcode.trim()) {
      setValue('secondaryBarcodes', [...secondaryBarcodes, newSecondaryBarcode.trim()]);
      setNewSecondaryBarcode('');
    }
  };

  const handleRemoveSecondaryBarcode = (index: number) => {
    setValue(
      'secondaryBarcodes',
      secondaryBarcodes.filter((_, i) => i !== index)
    );
  };

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h6" gutterBottom>
        {isUpdate ? 'Update Product' : 'Create Product'}
      </Typography>
      <form
        onSubmit={handleSubmit(values => {
          if (isUpdate) {
            // For updates, exclude productCode (it can't be changed)
            const { productCode, ...updateValues } = values;
            onSubmit(updateValues as UpdateProductRequest);
          } else {
            onSubmit(values as CreateProductRequest);
          }
        })}
      >
        <Grid container spacing={2}>
          <Grid item xs={12} sm={6}>
            <TextField
              {...register('productCode')}
              label="Product Code"
              fullWidth
              required
              disabled={isUpdate}
              error={!!errors.productCode || !!productCodeError}
              helperText={
                errors.productCode?.message ||
                productCodeError ||
                (isCheckingUniqueness ? 'Checking...' : '')
              }
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <FormControl fullWidth required error={!!errors.unitOfMeasure}>
              <InputLabel>Unit of Measure</InputLabel>
              <Controller
                name="unitOfMeasure"
                control={control}
                render={({ field }) => (
                  <Select {...field} label="Unit of Measure">
                    <MenuItem value="EA">Each (EA)</MenuItem>
                    <MenuItem value="CS">Case (CS)</MenuItem>
                    <MenuItem value="PK">Pack (PK)</MenuItem>
                    <MenuItem value="BOX">Box (BOX)</MenuItem>
                    <MenuItem value="PAL">Pallet (PAL)</MenuItem>
                  </Select>
                )}
              />
              {errors.unitOfMeasure && (
                <Typography variant="caption" color="error" sx={{ mt: 0.5, ml: 1.75 }}>
                  {errors.unitOfMeasure.message}
                </Typography>
              )}
            </FormControl>
          </Grid>
          <Grid item xs={12}>
            <TextField
              {...register('description')}
              label="Description"
              fullWidth
              required
              multiline
              rows={3}
              error={!!errors.description}
              helperText={errors.description?.message}
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <Controller
              name="primaryBarcode"
              control={control}
              render={({ field }) => (
                <BarcodeInput
                  {...field}
                  label="Primary Barcode"
                  fullWidth
                  required
                  error={!!errors.primaryBarcode}
                  helperText={errors.primaryBarcode?.message || 'Scan or enter barcode'}
                  value={field.value || ''}
                  onChange={value => field.onChange(value)}
                />
              )}
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              {...register('category')}
              label="Category (Optional)"
              fullWidth
              error={!!errors.category}
              helperText={errors.category?.message}
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              {...register('brand')}
              label="Brand (Optional)"
              fullWidth
              error={!!errors.brand}
              helperText={errors.brand?.message}
            />
          </Grid>
          <Grid item xs={12}>
            <Typography variant="subtitle2" gutterBottom>
              Secondary Barcodes (Optional)
            </Typography>
            <Box sx={{ display: 'flex', gap: 1, mb: 2 }}>
              <BarcodeInput
                value={newSecondaryBarcode}
                onChange={setNewSecondaryBarcode}
                label="Add Secondary Barcode"
                size="small"
                onScan={barcode => {
                  setNewSecondaryBarcode(barcode);
                  // Auto-add after a short delay to allow the value to be set
                  setTimeout(() => {
                    if (barcode.trim()) {
                      setValue('secondaryBarcodes', [...secondaryBarcodes, barcode.trim()]);
                      setNewSecondaryBarcode('');
                    }
                  }, 100);
                }}
                autoSubmitOnEnter={true}
                sx={{ flex: 1 }}
              />
              <Button variant="outlined" onClick={handleAddSecondaryBarcode}>
                Add
              </Button>
            </Box>
            {secondaryBarcodes.length > 0 && (
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                {secondaryBarcodes.map((barcode, index) => (
                  <Box key={index} sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <TextField value={barcode} size="small" disabled fullWidth />
                    <Button
                      variant="outlined"
                      color="error"
                      size="small"
                      onClick={() => handleRemoveSecondaryBarcode(index)}
                    >
                      Remove
                    </Button>
                  </Box>
                ))}
              </Box>
            )}
          </Grid>
          <Grid item xs={12}>
            <FormActions
              onCancel={onCancel}
              isSubmitting={isSubmitting}
              submitLabel={isUpdate ? 'Update Product' : 'Create Product'}
              cancelLabel="Cancel"
              submitDisabled={!!productCodeError || isCheckingUniqueness}
            />
          </Grid>
        </Grid>
      </form>
    </Paper>
  );
};
