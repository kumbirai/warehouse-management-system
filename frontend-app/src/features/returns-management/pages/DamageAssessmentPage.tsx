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
import {
  Delete as DeleteIcon,
  Add as AddIcon,
  CloudUpload as CloudUploadIcon,
} from '@mui/icons-material';
import { FormPageLayout } from '../../../components/layouts';
import { FormActions, BarcodeInput } from '../../../components/common';
import { useRecordDamageAssessment } from '../hooks/useRecordDamageAssessment';
import { DamageType, DamageSeverity, DamageSource } from '../types/damageAssessment';
import { Routes, getBreadcrumbs } from '../../../utils/navigationUtils';

const damagedProductItemSchema = z.object({
  productId: z.string().min(1, 'Product ID is required'),
  damagedQuantity: z.number().min(0.01, 'Damaged quantity must be greater than 0'),
  damageType: z.nativeEnum(DamageType),
  damageSeverity: z.nativeEnum(DamageSeverity),
  damageSource: z.nativeEnum(DamageSource),
  photoUrl: z.string().optional(),
  notes: z.string().optional(),
});

const insuranceClaimInfoSchema = z.object({
  claimNumber: z.string().min(1, 'Claim number is required'),
  insuranceCompany: z.string().min(1, 'Insurance company is required'),
  claimStatus: z.string().min(1, 'Claim status is required'),
  claimAmount: z.number().min(0, 'Claim amount must be non-negative'),
});

const damageAssessmentSchema = z
  .object({
    orderNumber: z.string().min(1, 'Order number is required'),
    damageType: z.nativeEnum(DamageType),
    damageSeverity: z.nativeEnum(DamageSeverity),
    damageSource: z.nativeEnum(DamageSource),
    damagedProductItems: z
      .array(damagedProductItemSchema)
      .min(1, 'At least one damaged product item is required'),
    insuranceClaimInfo: insuranceClaimInfoSchema.optional(),
    damageNotes: z.string().optional(),
  })
  .refine(
    data => {
      if (
        data.damageSeverity === DamageSeverity.SEVERE ||
        data.damageSeverity === DamageSeverity.TOTAL
      ) {
        return !!data.insuranceClaimInfo;
      }
      return true;
    },
    {
      message: 'Insurance claim information is required for severe or total damage',
      path: ['insuranceClaimInfo'],
    }
  );

type DamageAssessmentFormValues = z.infer<typeof damageAssessmentSchema>;

export const DamageAssessmentPage = () => {
  const navigate = useNavigate();
  const { mutate: recordDamageAssessment, isPending, error } = useRecordDamageAssessment();

  const {
    register,
    control,
    handleSubmit,
    formState: { errors },
    watch,
    setValue,
  } = useForm<DamageAssessmentFormValues>({
    resolver: zodResolver(damageAssessmentSchema),
    defaultValues: {
      orderNumber: '',
      damageType: DamageType.OTHER,
      damageSeverity: DamageSeverity.MINOR,
      damageSource: DamageSource.OTHER,
      damagedProductItems: [],
      damageNotes: '',
    },
  });

  const { fields, append, remove } = useFieldArray({
    control,
    name: 'damagedProductItems',
  });

  const damageSeverity = watch('damageSeverity');
  const requiresInsuranceClaim =
    damageSeverity === DamageSeverity.SEVERE || damageSeverity === DamageSeverity.TOTAL;

  const handleAddDamagedProduct = () => {
    append({
      productId: '',
      damagedQuantity: 0,
      damageType: DamageType.OTHER,
      damageSeverity: DamageSeverity.MINOR,
      damageSource: DamageSource.OTHER,
      photoUrl: '',
      notes: '',
    });
  };

  const handlePhotoUpload = (index: number, file: File) => {
    const reader = new FileReader();
    reader.onloadend = () => {
      const base64String = reader.result as string;
      setValue(`damagedProductItems.${index}.photoUrl`, base64String);
    };
    reader.readAsDataURL(file);
  };

  const handleOrderNumberScan = async (scannedBarcode: string) => {
    setValue('orderNumber', scannedBarcode);
  };

  const onSubmit = (data: DamageAssessmentFormValues) => {
    recordDamageAssessment(
      {
        orderNumber: data.orderNumber,
        damageType: data.damageType,
        damageSeverity: data.damageSeverity,
        damageSource: data.damageSource,
        damagedProductItems: data.damagedProductItems,
        insuranceClaimInfo: data.insuranceClaimInfo,
        damageNotes: data.damageNotes,
      },
      {
        onSuccess: response => {
          navigate(`/returns/damage-assessments/${response.assessmentId}`);
        },
      }
    );
  };

  return (
    <FormPageLayout
      breadcrumbs={getBreadcrumbs.damageAssessment()}
      title="Damage Assessment"
      description="Record damage assessment with product details and insurance information"
      error={error?.message || null}
      maxWidth="lg"
    >
      <form onSubmit={handleSubmit(onSubmit)}>
        <Grid container spacing={3}>
          <Grid item xs={12}>
            <Paper elevation={1} sx={{ p: 3 }}>
              <Typography variant="h6" gutterBottom>
                Damage Information
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
              <Grid container spacing={2}>
                <Grid item xs={12} md={4}>
                  <TextField
                    {...register('damageType')}
                    select
                    label="Damage Type"
                    fullWidth
                    required
                    error={!!errors.damageType}
                    helperText={errors.damageType?.message}
                  >
                    {Object.values(DamageType).map(type => (
                      <MenuItem key={type} value={type}>
                        {type.replace(/_/g, ' ')}
                      </MenuItem>
                    ))}
                  </TextField>
                </Grid>
                <Grid item xs={12} md={4}>
                  <TextField
                    {...register('damageSeverity')}
                    select
                    label="Damage Severity"
                    fullWidth
                    required
                    error={!!errors.damageSeverity}
                    helperText={errors.damageSeverity?.message}
                  >
                    {Object.values(DamageSeverity).map(severity => (
                      <MenuItem key={severity} value={severity}>
                        {severity.replace(/_/g, ' ')}
                      </MenuItem>
                    ))}
                  </TextField>
                </Grid>
                <Grid item xs={12} md={4}>
                  <TextField
                    {...register('damageSource')}
                    select
                    label="Damage Source"
                    fullWidth
                    required
                    error={!!errors.damageSource}
                    helperText={errors.damageSource?.message}
                  >
                    {Object.values(DamageSource).map(source => (
                      <MenuItem key={source} value={source}>
                        {source.replace(/_/g, ' ')}
                      </MenuItem>
                    ))}
                  </TextField>
                </Grid>
              </Grid>
            </Paper>
          </Grid>

          <Grid item xs={12}>
            <Paper elevation={1} sx={{ p: 3 }}>
              <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
                <Typography variant="h6">Damaged Product Items</Typography>
                <Button
                  variant="outlined"
                  size="small"
                  startIcon={<AddIcon />}
                  onClick={handleAddDamagedProduct}
                >
                  Add Product
                </Button>
              </Box>
              {errors.damagedProductItems && (
                <Alert severity="error" sx={{ mb: 2 }}>
                  {errors.damagedProductItems.message}
                </Alert>
              )}
              {fields.length === 0 && (
                <Alert severity="info" sx={{ mb: 2 }}>
                  Click "Add Product" to add damaged products to this assessment
                </Alert>
              )}
              {fields.map((field, index) => (
                <Card key={field.id} sx={{ mb: 2, p: 2 }}>
                  <Grid container spacing={2}>
                    <Grid item xs={12} md={3}>
                      <Controller
                        name={`damagedProductItems.${index}.productId`}
                        control={control}
                        render={({ field }) => (
                          <BarcodeInput
                            {...field}
                            label="Product ID"
                            fullWidth
                            required
                            error={!!errors.damagedProductItems?.[index]?.productId}
                            helperText={
                              errors.damagedProductItems?.[index]?.productId?.message ||
                              'Scan barcode first, or enter manually'
                            }
                            onScan={field.onChange}
                          />
                        )}
                      />
                    </Grid>
                    <Grid item xs={12} md={2}>
                      <TextField
                        {...register(`damagedProductItems.${index}.damagedQuantity`, {
                          valueAsNumber: true,
                        })}
                        label="Damaged Quantity"
                        type="number"
                        fullWidth
                        required
                        inputProps={{ min: 0.01, step: 0.01 }}
                        error={!!errors.damagedProductItems?.[index]?.damagedQuantity}
                        helperText={errors.damagedProductItems?.[index]?.damagedQuantity?.message}
                      />
                    </Grid>
                    <Grid item xs={12} md={2}>
                      <TextField
                        {...register(`damagedProductItems.${index}.damageType`)}
                        select
                        label="Damage Type"
                        fullWidth
                        required
                        error={!!errors.damagedProductItems?.[index]?.damageType}
                        helperText={errors.damagedProductItems?.[index]?.damageType?.message}
                      >
                        {Object.values(DamageType).map(type => (
                          <MenuItem key={type} value={type}>
                            {type.replace(/_/g, ' ')}
                          </MenuItem>
                        ))}
                      </TextField>
                    </Grid>
                    <Grid item xs={12} md={2}>
                      <TextField
                        {...register(`damagedProductItems.${index}.damageSeverity`)}
                        select
                        label="Severity"
                        fullWidth
                        required
                        error={!!errors.damagedProductItems?.[index]?.damageSeverity}
                        helperText={errors.damagedProductItems?.[index]?.damageSeverity?.message}
                      >
                        {Object.values(DamageSeverity).map(severity => (
                          <MenuItem key={severity} value={severity}>
                            {severity.replace(/_/g, ' ')}
                          </MenuItem>
                        ))}
                      </TextField>
                    </Grid>
                    <Grid item xs={12} md={2}>
                      <TextField
                        {...register(`damagedProductItems.${index}.damageSource`)}
                        select
                        label="Source"
                        fullWidth
                        required
                        error={!!errors.damagedProductItems?.[index]?.damageSource}
                        helperText={errors.damagedProductItems?.[index]?.damageSource?.message}
                      >
                        {Object.values(DamageSource).map(source => (
                          <MenuItem key={source} value={source}>
                            {source.replace(/_/g, ' ')}
                          </MenuItem>
                        ))}
                      </TextField>
                    </Grid>
                    <Grid item xs={12} md={1}>
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
                    <Grid item xs={12} md={6}>
                      <Button
                        variant="outlined"
                        component="label"
                        fullWidth
                        startIcon={<CloudUploadIcon />}
                        disabled={isPending}
                      >
                        Upload Photo
                        <input
                          type="file"
                          hidden
                          accept="image/*"
                          onChange={e => {
                            const file = e.target.files?.[0];
                            if (file) handlePhotoUpload(index, file);
                          }}
                        />
                      </Button>
                      {watch(`damagedProductItems.${index}.photoUrl`) && (
                        <Box sx={{ mt: 1 }}>
                          <img
                            src={watch(`damagedProductItems.${index}.photoUrl`)}
                            alt="Damage photo"
                            style={{ maxWidth: '100%', maxHeight: '100px' }}
                          />
                        </Box>
                      )}
                    </Grid>
                    <Grid item xs={12} md={6}>
                      <TextField
                        {...register(`damagedProductItems.${index}.notes`)}
                        label="Notes (Optional)"
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

          {requiresInsuranceClaim && (
            <Grid item xs={12}>
              <Paper elevation={1} sx={{ p: 3 }}>
                <Typography variant="h6" gutterBottom>
                  Insurance Claim Information
                </Typography>
                {errors.insuranceClaimInfo && (
                  <Alert severity="error" sx={{ mb: 2 }}>
                    {errors.insuranceClaimInfo.message}
                  </Alert>
                )}
                <Grid container spacing={2}>
                  <Grid item xs={12} md={6}>
                    <TextField
                      {...register('insuranceClaimInfo.claimNumber')}
                      label="Claim Number"
                      fullWidth
                      required={requiresInsuranceClaim}
                      error={!!errors.insuranceClaimInfo?.claimNumber}
                      helperText={errors.insuranceClaimInfo?.claimNumber?.message}
                    />
                  </Grid>
                  <Grid item xs={12} md={6}>
                    <TextField
                      {...register('insuranceClaimInfo.insuranceCompany')}
                      label="Insurance Company"
                      fullWidth
                      required={requiresInsuranceClaim}
                      error={!!errors.insuranceClaimInfo?.insuranceCompany}
                      helperText={errors.insuranceClaimInfo?.insuranceCompany?.message}
                    />
                  </Grid>
                  <Grid item xs={12} md={6}>
                    <TextField
                      {...register('insuranceClaimInfo.claimStatus')}
                      label="Claim Status"
                      fullWidth
                      required={requiresInsuranceClaim}
                      error={!!errors.insuranceClaimInfo?.claimStatus}
                      helperText={errors.insuranceClaimInfo?.claimStatus?.message}
                    />
                  </Grid>
                  <Grid item xs={12} md={6}>
                    <TextField
                      {...register('insuranceClaimInfo.claimAmount', { valueAsNumber: true })}
                      label="Claim Amount"
                      type="number"
                      fullWidth
                      required={requiresInsuranceClaim}
                      inputProps={{ min: 0, step: 0.01 }}
                      error={!!errors.insuranceClaimInfo?.claimAmount}
                      helperText={errors.insuranceClaimInfo?.claimAmount?.message}
                    />
                  </Grid>
                </Grid>
              </Paper>
            </Grid>
          )}

          <Grid item xs={12}>
            <Paper elevation={1} sx={{ p: 3 }}>
              <TextField
                {...register('damageNotes')}
                label="Damage Notes (Optional)"
                fullWidth
                multiline
                rows={4}
                helperText="Additional notes about the damage assessment"
              />
            </Paper>
          </Grid>

          <Grid item xs={12}>
            <FormActions
              onCancel={() => navigate(Routes.returns)}
              isSubmitting={isPending}
              submitLabel="Submit Damage Assessment"
              cancelLabel="Cancel"
            />
          </Grid>
        </Grid>
      </form>
    </FormPageLayout>
  );
};
