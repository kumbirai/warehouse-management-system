import React, { ReactNode } from 'react';
import { Paper, Stack, Button } from '@mui/material';

interface FilterBarProps {
  children: ReactNode;
  onClearFilters?: () => void;
  hasActiveFilters?: boolean;
}

export const FilterBar: React.FC<FilterBarProps> = ({
  children,
  onClearFilters,
  hasActiveFilters = false,
}) => {
  return (
    <Paper sx={{ p: 2, mb: 3 }}>
      <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} alignItems="center">
        {children}
        {onClearFilters && (
          <Button
            variant="outlined"
            onClick={onClearFilters}
            disabled={!hasActiveFilters}
            sx={{ minWidth: 120 }}
          >
            Clear Filters
          </Button>
        )}
      </Stack>
    </Paper>
  );
};
