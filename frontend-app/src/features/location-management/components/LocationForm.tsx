import {
  Box,
  Button,
  Grid,
  Paper,
  TextField,
  Typography,
} from '@mui/material';
import { z } from 'zod';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { CreateLocationRequest } from '../types/location';

const locationSchema = z.object({
  zone: z.string().min(1, 'Zone is required').max(10, 'Zone cannot exceed 10 characters'),
  aisle: z.string().min(1, 'Aisle is required').max(10, 'Aisle cannot exceed 10 characters'),
  rack: z.string().min(1, 'Rack is required').max(10, 'Rack cannot exceed 10 characters'),
  level: z.string().min(1, 'Level is required').max(10, 'Level cannot exceed 10 characters'),
  barcode: z.string().max(50, 'Barcode cannot exceed 50 characters').optional(),
  description: z.string().max(500, 'Description cannot exceed 500 characters').optional(),
});

export type LocationFormValues = z.infer<typeof locationSchema>;

interface LocationFormProps {
  defaultValues?: Partial<LocationFormValues>;
  onSubmit: (values: CreateLocationRequest) => Promise<void> | void;
  onCancel: () => void;
  isSubmitting?: boolean;
}

export const LocationForm = ({
  defaultValues,
  onSubmit,
  onCancel,
  isSubmitting = false,
}: LocationFormProps) => {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LocationFormValues>({
    resolver: zodResolver(locationSchema),
    defaultValues: {
      zone: '',
      aisle: '',
      rack: '',
      level: '',
      barcode: '',
      description: '',
      ...defaultValues,
    },
  });

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h6" gutterBottom>
        Create Location
      </Typography>
      <form onSubmit={handleSubmit(onSubmit)}>
        <Grid container spacing={2}>
          <Grid item xs={12} sm={6}>
            <TextField
              {...register('zone')}
              label="Zone"
              fullWidth
              required
              error={!!errors.zone}
              helperText={errors.zone?.message}
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              {...register('aisle')}
              label="Aisle"
              fullWidth
              required
              error={!!errors.aisle}
              helperText={errors.aisle?.message}
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              {...register('rack')}
              label="Rack"
              fullWidth
              required
              error={!!errors.rack}
              helperText={errors.rack?.message}
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              {...register('level')}
              label="Level"
              fullWidth
              required
              error={!!errors.level}
              helperText={errors.level?.message}
            />
          </Grid>
          <Grid item xs={12}>
            <TextField
              {...register('barcode')}
              label="Barcode (Optional - auto-generated if not provided)"
              fullWidth
              error={!!errors.barcode}
              helperText={errors.barcode?.message}
            />
          </Grid>
          <Grid item xs={12}>
            <TextField
              {...register('description')}
              label="Description (Optional)"
              fullWidth
              multiline
              rows={3}
              error={!!errors.description}
              helperText={errors.description?.message}
            />
          </Grid>
          <Grid item xs={12}>
            <Box sx={{ display: 'flex', gap: 2, justifyContent: 'flex-end' }}>
              <Button variant="outlined" onClick={onCancel} disabled={isSubmitting}>
                Cancel
              </Button>
              <Button type="submit" variant="contained" disabled={isSubmitting}>
                {isSubmitting ? 'Creating...' : 'Create Location'}
              </Button>
            </Box>
          </Grid>
        </Grid>
      </form>
    </Paper>
  );
};

