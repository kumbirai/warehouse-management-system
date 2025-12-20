import React from 'react';
import { Box, CircularProgress } from '@mui/material';

interface LoadingSpinnerProps {
  size?: number;
  minHeight?: string | number;
}

export const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({
  size = 40,
  minHeight = '400px'
}) => {
  return (
    <Box
      display="flex"
      justifyContent="center"
      alignItems="center"
      minHeight={minHeight}
    >
      <CircularProgress size={size} />
    </Box>
  );
};
