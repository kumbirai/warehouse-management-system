import {
  Box,
  Button,
  Checkbox,
  FormControlLabel,
  Grid,
  Paper,
  TextField,
  Typography,
} from '@mui/material';
import { z } from 'zod';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { TenantSelector } from './TenantSelector';
import { useAuth } from '../../../hooks/useAuth';
import { ALL_ROLES, USER } from '../../../constants/roles';

const userSchema = z
  .object({
    tenantId: z.string().min(1, 'Tenant is required'),
    username: z
      .string()
      .min(1, 'Username is required')
      .max(50, 'Username cannot exceed 50 characters')
      .regex(
        /^[a-zA-Z0-9._-]+$/,
        'Username must be alphanumeric with periods, hyphens, or underscores only'
      ),
    emailAddress: z
      .string()
      .min(1, 'Email is required')
      .email('Invalid email format')
      .max(255, 'Email cannot exceed 255 characters'),
    firstName: z.string().max(50, 'First name cannot exceed 50 characters').optional(),
    lastName: z.string().max(50, 'Last name cannot exceed 50 characters').optional(),
    password: z
      .string()
      .min(8, 'Password must be at least 8 characters')
      .regex(
        /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]/,
        'Password must contain uppercase, lowercase, number, and special character'
      ),
    confirmPassword: z.string(),
    roles: z.array(z.string()).optional(),
  })
  .refine(data => data.password === data.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  });

export type UserFormValues = z.infer<typeof userSchema>;

interface UserFormProps {
  defaultValues?: Partial<UserFormValues>;
  onSubmit: (values: UserFormValues) => Promise<void> | void;
  onCancel: () => void;
  isSubmitting?: boolean;
}

export const UserForm = ({
  defaultValues,
  onSubmit,
  onCancel,
  isSubmitting = false,
}: UserFormProps) => {
  const { isSystemAdmin, user: currentUser } = useAuth();
  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors },
  } = useForm<UserFormValues>({
    resolver: zodResolver(userSchema),
    defaultValues: {
      tenantId: defaultValues?.tenantId || currentUser?.tenantId || '',
      username: '',
      emailAddress: '',
      firstName: '',
      lastName: '',
      password: '',
      confirmPassword: '',
      roles: defaultValues?.roles || [USER],
      ...defaultValues,
    },
  });

  const selectedRoles = watch('roles') || [];
  const selectedTenantId = watch('tenantId');

  const handleRoleToggle = (role: string) => {
    const currentRoles = selectedRoles;
    if (currentRoles.includes(role)) {
      setValue(
        'roles',
        currentRoles.filter(r => r !== role)
      );
    } else {
      setValue('roles', [...currentRoles, role]);
    }
  };

  const handleFormSubmit = (values: UserFormValues) => {
    onSubmit(values);
  };

  return (
    <Paper elevation={2} sx={{ p: 3 }}>
      <Box component="form" onSubmit={handleSubmit(handleFormSubmit)} noValidate>
        <Typography variant="h6" gutterBottom>
          User Information
        </Typography>

        {isSystemAdmin() && (
          <Grid container spacing={2} sx={{ mb: 2 }}>
            <Grid item xs={12} md={6}>
              <TenantSelector
                value={selectedTenantId}
                onChange={tenantId => setValue('tenantId', tenantId || '')}
                required
              />
              {errors.tenantId && (
                <Typography variant="caption" color="error" sx={{ mt: 0.5, display: 'block' }}>
                  {errors.tenantId.message}
                </Typography>
              )}
            </Grid>
          </Grid>
        )}

        <Grid container spacing={2}>
          <Grid item xs={12} md={6}>
            <TextField
              label="Username"
              fullWidth
              required
              {...register('username')}
              error={!!errors.username}
              helperText={errors.username?.message}
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              label="Email"
              fullWidth
              required
              type="email"
              {...register('emailAddress')}
              error={!!errors.emailAddress}
              helperText={errors.emailAddress?.message}
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              label="First Name"
              fullWidth
              {...register('firstName')}
              error={!!errors.firstName}
              helperText={errors.firstName?.message}
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              label="Last Name"
              fullWidth
              {...register('lastName')}
              error={!!errors.lastName}
              helperText={errors.lastName?.message}
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              label="Password"
              fullWidth
              required
              type="password"
              {...register('password')}
              error={!!errors.password}
              helperText={errors.password?.message}
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              label="Confirm Password"
              fullWidth
              required
              type="password"
              {...register('confirmPassword')}
              error={!!errors.confirmPassword}
              helperText={errors.confirmPassword?.message}
            />
          </Grid>
        </Grid>

        <Box sx={{ mt: 3, mb: 2 }}>
          <Typography variant="h6" gutterBottom>
            Role Assignment
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Select roles to assign to this user
          </Typography>
          <Grid container spacing={1}>
            {ALL_ROLES.map(role => (
              <Grid item xs={12} sm={6} md={4} key={role}>
                <FormControlLabel
                  control={
                    <Checkbox
                      checked={selectedRoles.includes(role)}
                      onChange={() => handleRoleToggle(role)}
                    />
                  }
                  label={role}
                />
              </Grid>
            ))}
          </Grid>
        </Box>

        <Box display="flex" justifyContent="flex-end" gap={2} mt={3}>
          <Button variant="outlined" onClick={onCancel} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button variant="contained" type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Creating...' : 'Create User'}
          </Button>
        </Box>
      </Box>
    </Paper>
  );
};
