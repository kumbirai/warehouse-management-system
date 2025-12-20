import React, { ReactNode } from 'react';
import { Stack, Typography } from '@mui/material';

interface PageHeaderProps {
  title: string;
  description?: string;
  actions?: ReactNode;
}

export const PageHeader: React.FC<PageHeaderProps> = ({
  title,
  description,
  actions,
}) => {
  return (
    <Stack
      direction={{ xs: 'column', md: 'row' }}
      justifyContent="space-between"
      alignItems={{ xs: 'flex-start', md: 'center' }}
      spacing={2}
      sx={{ mb: 3 }}
    >
      <Stack spacing={1}>
        <Typography variant="h4" component="h1">
          {title}
        </Typography>
        {description && (
          <Typography variant="body1" color="text.secondary">
            {description}
          </Typography>
        )}
      </Stack>
      {actions && <Stack direction="row" spacing={2}>{actions}</Stack>}
    </Stack>
  );
};
