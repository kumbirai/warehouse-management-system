import {
  Box,
  Button,
  FormControlLabel,
  Grid,
  Paper,
  Switch,
  TextField,
  Typography,
} from '@mui/material';
import { z } from 'zod';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMemo } from 'react';

const tenantSchema = z.object({
  tenantId: z
    .string()
    .min(1, 'Tenant ID is required')
    .max(50, 'Tenant ID cannot exceed 50 characters')
    .regex(
      /^[a-zA-Z0-9-_]+$/,
      'Only alphanumeric characters, hyphens, and underscores are allowed'
    ),
  name: z
    .string()
    .min(1, 'Tenant name is required')
    .max(200, 'Tenant name cannot exceed 200 characters'),
  emailAddress: z
    .union([
      z.string().email('Invalid email format').max(255, 'Email cannot exceed 255 characters'),
      z.literal(''),
    ])
    .optional(),
  phone: z
    .union([z.string().max(50, 'Phone cannot exceed 50 characters'), z.literal('')])
    .optional(),
  address: z
    .union([z.string().max(500, 'Address cannot exceed 500 characters'), z.literal('')])
    .optional(),
  usePerTenantRealm: z.boolean().optional(),
  keycloakRealmName: z.string().max(100, 'Realm name cannot exceed 100 characters').optional(),
});

export type TenantFormValues = z.infer<typeof tenantSchema>;

interface TenantFormProps {
  defaultValues?: Partial<TenantFormValues>;
  onSubmit: (values: TenantFormValues) => Promise<void> | void;
  onCancel: () => void;
  isSubmitting?: boolean;
}

export const TenantForm = ({
  defaultValues,
  onSubmit,
  onCancel,
  isSubmitting = false,
}: TenantFormProps) => {
  // Memoize form default values to prevent unnecessary re-renders
  const formDefaultValues = useMemo(
    () => ({
      tenantId: defaultValues?.tenantId || '',
      name: defaultValues?.name || '',
      emailAddress: defaultValues?.emailAddress || '',
      phone: defaultValues?.phone || '',
      address: defaultValues?.address || '',
      usePerTenantRealm: defaultValues?.usePerTenantRealm || false,
      keycloakRealmName: defaultValues?.keycloakRealmName || '',
    }),
    [defaultValues]
  );

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<TenantFormValues>({
    resolver: zodResolver(tenantSchema),
    defaultValues: formDefaultValues,
  });

  return (
    <Paper elevation={2} sx={{ p: 3 }}>
      <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
        <Typography variant="h6" gutterBottom>
          Tenant Information
        </Typography>
        <Grid container spacing={2}>
          <Grid item xs={12} md={6}>
            <TextField
              label="Tenant ID"
              fullWidth
              required
              {...register('tenantId')}
              error={!!errors.tenantId}
              helperText={errors.tenantId?.message}
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              label="Tenant Name"
              fullWidth
              required
              {...register('name')}
              error={!!errors.name}
              helperText={errors.name?.message}
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              label="Email"
              fullWidth
              {...register('emailAddress')}
              error={!!errors.emailAddress}
              helperText={errors.emailAddress?.message}
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              label="Phone"
              fullWidth
              {...register('phone')}
              error={!!errors.phone}
              helperText={errors.phone?.message}
            />
          </Grid>
          <Grid item xs={12}>
            <TextField
              label="Address"
              fullWidth
              multiline
              rows={3}
              {...register('address')}
              error={!!errors.address}
              helperText={errors.address?.message}
            />
          </Grid>
          <Grid item xs={12}>
            <FormControlLabel
              control={<Switch color="primary" {...register('usePerTenantRealm')} />}
              label="Use dedicated Keycloak realm for this tenant"
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              label="Keycloak Realm Name"
              fullWidth
              {...register('keycloakRealmName')}
              error={!!errors.keycloakRealmName}
              helperText={errors.keycloakRealmName?.message}
            />
          </Grid>
        </Grid>

        <Box display="flex" justifyContent="flex-end" gap={2} mt={3}>
          <Button variant="outlined" onClick={onCancel}>
            Cancel
          </Button>
          <Button variant="contained" type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Submitting...' : 'Create Tenant'}
          </Button>
        </Box>
      </Box>
    </Paper>
  );
};
