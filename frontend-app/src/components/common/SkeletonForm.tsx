import { Grid, Paper, Skeleton, Stack } from '@mui/material';

interface SkeletonFormProps {
  fields?: number;
}

export const SkeletonForm: React.FC<SkeletonFormProps> = ({ fields = 6 }) => {
  return (
    <Paper sx={{ p: 3 }}>
      <Stack spacing={3}>
        <Skeleton variant="text" width="30%" height={32} />
        <Grid container spacing={2}>
          {Array.from({ length: fields }).map((_, index) => (
            <Grid item xs={12} sm={6} key={index}>
              <Skeleton variant="rectangular" height={56} />
            </Grid>
          ))}
        </Grid>
        <Stack direction="row" spacing={2} justifyContent="flex-end" sx={{ mt: 2 }}>
          <Skeleton variant="rectangular" width={100} height={36} />
          <Skeleton variant="rectangular" width={100} height={36} />
        </Stack>
      </Stack>
    </Paper>
  );
};
