import { Box, Grid, Paper, TextField, Typography } from '@mui/material';
import { z } from 'zod';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMemo } from 'react';
import { UpdateUserProfileRequest, User } from '../types/user';
import { FormActions } from '../../../components/common';

const profileSchema = z.object({
  emailAddress: z
    .string()
    .min(1, 'Email is required')
    .email('Invalid email format')
    .max(255, 'Email cannot exceed 255 characters'),
  firstName: z.string().max(50, 'First name cannot exceed 50 characters').optional(),
  lastName: z.string().max(50, 'Last name cannot exceed 50 characters').optional(),
});

export type ProfileFormValues = z.infer<typeof profileSchema>;

interface UserProfileEditorProps {
  user: User;
  onSubmit: (values: UpdateUserProfileRequest) => Promise<void> | void;
  onCancel: () => void;
  isLoading?: boolean;
}

export const UserProfileEditor = ({
  user,
  onSubmit,
  onCancel,
  isLoading = false,
}: UserProfileEditorProps) => {
  // Memoize default values to prevent unnecessary re-renders
  const defaultValues = useMemo(
    () => ({
      emailAddress: user.emailAddress,
      firstName: user.firstName || '',
      lastName: user.lastName || '',
    }),
    [user.emailAddress, user.firstName, user.lastName]
  );

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ProfileFormValues>({
    resolver: zodResolver(profileSchema),
    defaultValues,
  });

  return (
    <Paper elevation={2} sx={{ p: 3 }}>
      <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
        <Typography variant="h6" gutterBottom>
          Edit Profile
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Note: Username and tenant cannot be changed.
        </Typography>

        <Grid container spacing={2}>
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
        </Grid>

        <FormActions
          onCancel={onCancel}
          isSubmitting={isLoading}
          submitLabel="Save Changes"
          cancelLabel="Cancel"
        />
      </Box>
    </Paper>
  );
};
