import { Box, Card, CardContent, Skeleton, Stack } from '@mui/material';

interface SkeletonCardProps {
  lines?: number;
}

export const SkeletonCard: React.FC<SkeletonCardProps> = ({ lines = 4 }) => {
  return (
    <Card>
      <CardContent>
        <Stack spacing={2}>
          <Skeleton variant="text" width="40%" height={32} />
          {Array.from({ length: lines }).map((_, index) => (
            <Box key={index}>
              <Skeleton variant="text" width="20%" height={16} />
              <Skeleton variant="text" width="60%" height={24} />
            </Box>
          ))}
        </Stack>
      </CardContent>
    </Card>
  );
};
