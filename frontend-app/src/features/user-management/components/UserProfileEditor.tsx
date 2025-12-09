import {Box, Button, Grid, Paper, TextField, Typography,} from '@mui/material';
import {z} from 'zod';
import {useForm} from 'react-hook-form';
import {zodResolver} from '@hookform/resolvers/zod';
import {UpdateUserProfileRequest, User} from '../types/user';

const profileSchema = z.object({
    emailAddress: z
            .string()
            .min(1, 'Email is required')
            .email('Invalid email format')
            .max(255, 'Email cannot exceed 255 characters'),
    firstName: z
            .string()
            .max(50, 'First name cannot exceed 50 characters')
            .optional(),
    lastName: z
            .string()
            .max(50, 'Last name cannot exceed 50 characters')
            .optional(),
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
    const {
        register,
        handleSubmit,
        formState: {errors},
    } = useForm<ProfileFormValues>({
        resolver: zodResolver(profileSchema),
        defaultValues: {
            emailAddress: user.emailAddress,
            firstName: user.firstName || '',
            lastName: user.lastName || '',
        },
    });

    return (
            <Paper elevation={2} sx={{p: 3}}>
                <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
                    <Typography variant="h6" gutterBottom>
                        Edit Profile
                    </Typography>
                    <Typography variant="body2" color="text.secondary" sx={{mb: 2}}>
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

                    <Box display="flex" justifyContent="flex-end" gap={2} mt={3}>
                        <Button variant="outlined" onClick={onCancel} disabled={isLoading}>
                            Cancel
                        </Button>
                        <Button variant="contained" type="submit" disabled={isLoading}>
                            {isLoading ? 'Saving...' : 'Save Changes'}
                        </Button>
                    </Box>
                </Box>
            </Paper>
    );
};

